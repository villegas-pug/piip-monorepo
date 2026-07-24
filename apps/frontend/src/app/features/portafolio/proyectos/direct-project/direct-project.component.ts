// Pagina de creacion de proyecto directo (US3 - T067).
//
// Recorrido de la Autoridad o el Evaluador: registra un proyecto directo
// heredado o excepcional sin iniciativa origen. Antes de habilitar el
// envio, la pagina consulta si existe una iniciativa `PRESENTADO` en la
// misma unidad responsable; si existe, bloquea la creacion porque la
// Constitucion exige que la evaluacion no se omita. La verificacion se
// repite cada vez que el usuario cambia la unidad responsable.
//
// El componente NO genera codigo, NO decide transiciones y NO asigna
// responsables: el backend es la autoridad efectiva. La ETag devuelta por
// la consulta de iniciativa se propaga al comando de creacion como
// `If-Match`; la cabecera `Idempotency-Key` la aplica automaticamente el
// `idempotencyKeyInterceptor` global.
//
// Accesibilidad WCAG 2.1 AA:
//   * Foco visible y navegacion por teclado en cada control.
//   * Labels asociados (`for`/`id`) y mensajes de error con `aria-describedby`.
//   * Regiones `aria-live` separadas para errores de servidor, resultados
//     y bloqueo por iniciativa PRESENTADO.
//   * `aria-controls` en el boton de cancelacion para vincularlo al formulario.
//   * Contraste minimo 4.5:1 delegado al tema PIIP (piip-theme.scss).
//   * `Escape` revierte los cambios pendientes sin enviar al backend.

import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnInit,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  untracked
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';

import { parseProblemDetails, ProblemDetails, ProblemViolation } from '../../../../core/http/problem-details';
import { ProyectosApiService } from '../api/proyectos-api.service';
import {
  DirectProjectCommand,
  DirectProjectContext,
  FuenteOrigen,
  ProjectDetail,
  TipoOrigenDirecto
} from '../api/types/proyectos.types';

interface DirectProjectFormGroup {
  tipoOrigen: FormControl<TipoOrigenDirecto | null>;
  codigoOrigen: FormControl<string>;
  fechaInicio: FormControl<string>;
  nombre: FormControl<string>;
  objetivoPeiId: FormControl<number | null>;
  actividadPoiId: FormControl<number | null>;
  unidadResponsableId: FormControl<number | null>;
  responsableId: FormControl<number | null>;
  descripcion: FormControl<string>;
  componenteDigital: FormControl<boolean>;
  detalleComponenteDigital: FormControl<string>;
  nota: FormControl<string>;
  documentoAutorizacionId: FormControl<number | null>;
  evidenciaIds: FormControl<number[]>;
  fuenteOrigen: FormControl<FuenteOrigen | null>;
  detalleFuente: FormControl<string>;
}

const MAX_NOMBRE = 500;
const MAX_CODIGO_ORIGEN = 50;
const MAX_DESCRIPCION = 2000;
const MAX_NOTA = 1000;
const MAX_DETALLE_FUENTE = 500;
const MAX_DETALLE_COMPONENTE = 500;
const TITULO_DOCUMENTO_FORMAL =
  'Documento Formal de Aprobacion o Autorizacion de Inicio';

const FORM_ID = 'direct-project-form';
const ID_TIPO_ORIGEN = 'direct-tipo-origen';
const ID_CODIGO_ORIGEN = 'direct-codigo-origen';
const ID_FECHA_INICIO = 'direct-fecha-inicio';
const ID_NOMBRE = 'direct-nombre';
const ID_OBJETIVO_PEI = 'direct-objetivo-pei';
const ID_ACTIVIDAD_POI = 'direct-actividad-poi';
const ID_UNIDAD_RESPONSABLE = 'direct-unidad-responsable';
const ID_RESPONSABLE = 'direct-responsable';
const ID_DESCRIPCION = 'direct-descripcion';
const ID_COMPONENTE_DIGITAL = 'direct-componente-digital';
const ID_DETALLE_DIGITAL = 'direct-detalle-digital';
const ID_NOTA = 'direct-nota';
const ID_DOCUMENTO_AUTORIZACION = 'direct-documento-autorizacion';
const ID_EVIDENCIA_BASE = 'direct-evidencia';
const ID_FUENTE = 'direct-fuente';
const ID_DETALLE_FUENTE = 'direct-detalle-fuente';

const TIPO_ORIGEN_OPCIONES: readonly TipoOrigenDirecto[] = ['HEREDADO', 'EXCEPCION_FORMAL'];
const FUENTE_ORIGEN_OPCIONES: readonly FuenteOrigen[] = [
  'FICHA_INICIATIVA',
  'CONCURSO_INTERNO',
  'INNOVACION_ABIERTA',
  'PROPUESTA_JEFATURA',
  'OTROS'
];

@Component({
  selector: 'app-direct-project',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  templateUrl: './direct-project.component.html',
  styleUrl: './direct-project.component.scss'
})
export class DirectProjectComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly proyectos = inject(ProyectosApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly host = inject(ElementRef<HTMLElement>);

  /**
   * Identificador de la unidad responsable inicial. Si se omite, se lee de
   * `ActivatedRoute.queryParams.unidadId`. La unidad se reconsulta cada vez
   * que el usuario la cambia para revalidar la regla "no omite evaluacion".
   */
  readonly unidadResponsableInicial = input<number | null>(null);

  /** Notifica al shell cuando la Autoridad o el Evaluador cancela la operacion. */
  readonly cancelar = output<void>();

  protected readonly tipoOrigenOpciones: readonly TipoOrigenDirecto[] = TIPO_ORIGEN_OPCIONES;
  protected readonly fuenteOrigenOpciones: readonly FuenteOrigen[] = FUENTE_ORIGEN_OPCIONES;
  protected readonly tituloDocumentoFormal = TITULO_DOCUMENTO_FORMAL;

  protected readonly form: FormGroup<DirectProjectFormGroup> = this.buildForm();

  protected readonly submitted = signal(false);
  protected readonly submitting = signal(false);
  protected readonly unidadResponsable = signal<number | null>(null);
  protected readonly iniciativaPresentadaConflicto = signal<DirectProjectContext | undefined>(
    undefined
  );
  protected readonly verificandoUnidad = signal(false);
  protected readonly proyectoCreado = signal<ProjectDetail | undefined>(undefined);
  protected readonly etagActual = computed(() => this.proyectoCreado()?.etag);
  protected readonly problem = signal<ProblemDetails | undefined>(undefined);
  protected readonly atajoTecladoActivo = signal(true);

  protected readonly bloqueadoPorIniciativaPresentada = computed(
    () => this.iniciativaPresentadaConflicto()?.id !== undefined
  );
  protected readonly permiteEnvio = computed(
    () =>
      !this.bloqueadoPorIniciativaPresentada() &&
      this.unidadResponsable() !== null &&
      this.proyectoCreado() === undefined
  );

  private readonly queryResuelto = toSignal(this.route.queryParamMap.pipe(takeUntilDestroyed()), {
    initialValue: this.route.snapshot.queryParamMap
  });

  protected readonly formId = FORM_ID;
  protected readonly idTipoOrigen = ID_TIPO_ORIGEN;
  protected readonly idCodigoOrigen = ID_CODIGO_ORIGEN;
  protected readonly idFechaInicio = ID_FECHA_INICIO;
  protected readonly idNombre = ID_NOMBRE;
  protected readonly idObjetivoPei = ID_OBJETIVO_PEI;
  protected readonly idActividadPoi = ID_ACTIVIDAD_POI;
  protected readonly idUnidadResponsable = ID_UNIDAD_RESPONSABLE;
  protected readonly idResponsable = ID_RESPONSABLE;
  protected readonly idDescripcion = ID_DESCRIPCION;
  protected readonly idComponenteDigital = ID_COMPONENTE_DIGITAL;
  protected readonly idDetalleDigital = ID_DETALLE_DIGITAL;
  protected readonly idNota = ID_NOTA;
  protected readonly idDocumentoAutorizacion = ID_DOCUMENTO_AUTORIZACION;
  protected readonly idEvidenciaBase = ID_EVIDENCIA_BASE;
  protected readonly idFuente = ID_FUENTE;
  protected readonly idDetalleFuente = ID_DETALLE_FUENTE;

  constructor() {
    // Inicializa la unidad responsable a partir del input o queryParam.
    // No se trackea this.unidadResponsable para que cambios del usuario no re-activen el efecto.
    effect(() => {
      const desdeInput = this.unidadResponsableInicial();
      if (desdeInput !== null && desdeInput !== undefined && Number.isFinite(desdeInput)) {
        if (untracked(this.unidadResponsable) !== desdeInput) {
          this.unidadResponsable.set(desdeInput);
          this.form.controls.unidadResponsableId.setValue(desdeInput);
          void this.verificarIniciativaPresentada(desdeInput);
        }
        return;
      }
      const query = this.queryResuelto();
      const crudo = query?.get('unidadId');
      if (!crudo) {
        return;
      }
      const parsed = Number(crudo);
      if (Number.isFinite(parsed) && untracked(this.unidadResponsable) !== parsed) {
        this.unidadResponsable.set(parsed);
        this.form.controls.unidadResponsableId.setValue(parsed);
        void this.verificarIniciativaPresentada(parsed);
      }
    });

    // `codigoOrigen` es obligatorio solo cuando `tipoOrigen === 'EXCEPCION_FORMAL'`.
    this.form.controls.tipoOrigen.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((valor) => {
        const codigo = this.form.controls.codigoOrigen;
        if (valor === 'EXCEPCION_FORMAL') {
          codigo.addValidators([Validators.required, Validators.maxLength(MAX_CODIGO_ORIGEN)]);
        } else {
          codigo.removeValidators([Validators.required]);
        }
        codigo.updateValueAndValidity({ emitEvent: false });
      });

    // El detalle de la fuente es obligatorio solo cuando la fuente es OTROS.
    this.form.controls.fuenteOrigen.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((valor) => {
        const detalle = this.form.controls.detalleFuente;
        if (valor === 'OTROS') {
          detalle.addValidators([Validators.required, Validators.maxLength(MAX_DETALLE_FUENTE)]);
        } else {
          detalle.removeValidators([Validators.required]);
        }
        detalle.updateValueAndValidity({ emitEvent: false });
      });

    // El detalle del componente digital es obligatorio solo cuando se activa.
    this.form.controls.componenteDigital.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((valor) => {
        const detalle = this.form.controls.detalleComponenteDigital;
        if (valor) {
          detalle.addValidators([
            Validators.required,
            Validators.maxLength(MAX_DETALLE_COMPONENTE)
          ]);
        } else {
          detalle.removeValidators([Validators.required]);
        }
        detalle.updateValueAndValidity({ emitEvent: false });
      });
  }

  ngOnInit(): void {
    // Mueve el foco al primer control del formulario para usuarios que
    // navegan con teclado. Se ejecuta tras la primera renderizacion.
    queueMicrotask(() => this.enfocarPrimerControl());
  }

  // ---------------------------------------------------------------------------
  // Acciones principales
  // ---------------------------------------------------------------------------

  protected async enviar(): Promise<void> {
    this.submitted.set(true);
    this.problem.set(undefined);
    if (this.bloqueadoPorIniciativaPresentada()) {
      this.problem.set({
        type: 'about:blank',
        title: 'Bloqueo por iniciativa PRESENTADO',
        status: 409,
        code: 'INITIATIVE_PRESENT_FOR_SCOPE',
        detail:
          'Existe una iniciativa PRESENTADO en la unidad responsable; no se omite la evaluacion.',
        correlationId: 'N/A',
        violations: []
      });
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const unidad = this.unidadResponsable();
    if (unidad === null) {
      this.problem.set({
        type: 'about:blank',
        title: 'Contexto requerido',
        status: 400,
        code: 'DIRECT_PROJECT_CONTEXT_REQUIRED',
        detail: 'No se puede crear el proyecto directo sin unidad responsable.',
        correlationId: 'N/A',
        violations: []
      });
      return;
    }

    const tipoOrigen = this.form.controls.tipoOrigen.value as TipoOrigenDirecto;
    const fuenteOrigen = this.form.controls.fuenteOrigen.value as FuenteOrigen;
    const detalleFuenteValor = this.form.controls.detalleFuente.value.trim();
    const detalleDigitalValor = this.form.controls.detalleComponenteDigital.value.trim();
    const codigoOrigenValor = this.form.controls.codigoOrigen.value.trim();
    const evidenciaIds: number[] = (this.form.controls.evidenciaIds.value ?? [])
      .filter((id): id is number => id !== null && Number.isFinite(id));

    const payload: DirectProjectCommand = {
      tipoOrigen,
      codigoOrigen: tipoOrigen === 'EXCEPCION_FORMAL' ? codigoOrigenValor : undefined,
      fechaInicio: this.form.controls.fechaInicio.value,
      nombre: this.form.controls.nombre.value.trim(),
      objetivoPeiId: this.form.controls.objetivoPeiId.value as number,
      actividadPoiId: this.form.controls.actividadPoiId.value as number,
      unidadResponsableId: unidad,
      responsableId: this.form.controls.responsableId.value as number,
      descripcion: this.form.controls.descripcion.value.trim(),
      componenteDigital: this.form.controls.componenteDigital.value,
      detalleComponenteDigital:
        this.form.controls.componenteDigital.value && detalleDigitalValor
          ? detalleDigitalValor
          : undefined,
      nota: this.form.controls.nota.value.trim() || undefined,
      documentoAutorizacionId: this.form.controls.documentoAutorizacionId.value as number,
      evidenciaIds,
      fuenteOrigen,
      detalleFuente:
        fuenteOrigen === 'OTROS' && detalleFuenteValor ? detalleFuenteValor : undefined
    };

    this.submitting.set(true);
    this.proyectos.crearProyectoDirecto(payload).subscribe({
      next: (detalle) => {
        this.proyectoCreado.set(detalle);
        this.problem.set(undefined);
        this.submitting.set(false);
        this.submitted.set(false);
      },
      error: (error: unknown) => this.manejarError(error)
    });
  }

  protected cancelarEdicion(): void {
    this.form.reset();
    this.problem.set(undefined);
  }

  // ---------------------------------------------------------------------------
  // Acciones de unidad responsable
  // ---------------------------------------------------------------------------

  protected cambiarUnidadResponsable(unidadId: number): void {
    if (!Number.isFinite(unidadId) || unidadId < 1) {
      return;
    }
    if (this.unidadResponsable() === unidadId) {
      return;
    }
    this.unidadResponsable.set(unidadId);
    this.form.controls.unidadResponsableId.setValue(unidadId);
    void this.verificarIniciativaPresentada(unidadId);
  }

  // ---------------------------------------------------------------------------
  // Acciones de evidencias (FormControl<number[]>)
  // ---------------------------------------------------------------------------

  protected get evidencias(): FormControl<number[]> {
    return this.form.controls.evidenciaIds;
  }

  protected agregarEvidencia(): void {
    this.evidencias.setValue([...this.evidencias.value, null as unknown as number]);
  }

  protected quitarEvidencia(indice: number): void {
    const ids = [...this.evidencias.value];
    ids.splice(indice, 1);
    this.evidencias.setValue(ids);
  }

  protected actualizarEvidencia(indice: number, valor: number): number[] {
    const ids = [...this.evidencias.value];
    ids[indice] = Number.isFinite(valor) ? valor : (null as unknown as number);
    return ids;
  }

  // ---------------------------------------------------------------------------
  // Cancelacion y atajos de teclado
  // ---------------------------------------------------------------------------

  protected cancelarCreacion(): void {
    this.cancelarEdicion();
    this.cancelar.emit();
  }

  protected alPulsarEscape(): void {
    if (this.form.dirty) {
      this.cancelarEdicion();
    }
  }

  protected enfocarPrimerControl(): void {
    const root = this.host.nativeElement;
    const primerControl = root.querySelector(
      'input:not([type="hidden"]):not([disabled]), select:not([disabled]), textarea:not([disabled])'
    ) as HTMLElement | null;
    primerControl?.focus();
  }

  // ---------------------------------------------------------------------------
  // Mensajes de error accesibles
  // ---------------------------------------------------------------------------

  protected violacionesPara(campo: string): readonly ProblemViolation[] {
    const problema = this.problem();
    if (!problema) {
      return [];
    }
    return problema.violations.filter((violation) => violation.field === campo);
  }

  protected mensajeError(campo: keyof DirectProjectFormGroup): string | null {
    const control = this.form.controls[campo];
    if (!control.errors || !(control.touched || this.submitted())) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Este campo es obligatorio.';
    }
    if (control.hasError('maxlength')) {
      const requerido = control.getError('maxlength') as { requiredLength: number };
      return `Maximo ${requerido.requiredLength} caracteres.`;
    }
    if (control.hasError('min')) {
      const requerido = control.getError('min') as { min: number };
      return `Debe ser mayor o igual a ${requerido.min}.`;
    }
    return null;
  }

  // ---------------------------------------------------------------------------
  // Carga inicial y manejo de errores
  // ---------------------------------------------------------------------------

  private async verificarIniciativaPresentada(unidadId: number): Promise<void> {
    this.verificandoUnidad.set(true);
    try {
      const respuesta = await firstValueFrom(
        this.proyectos.consultarIniciativaPresentadaPorUnidad(unidadId)
      );
      this.iniciativaPresentadaConflicto.set(respuesta);
    } catch (error: unknown) {
      // Si la consulta falla, se asume que no existe bloqueo por iniciativa
      // PRESENTADO. El backend conserva la autoridad: reintentara confirmar
      // el bloqueo al recibir el POST.
      this.iniciativaPresentadaConflicto.set({ id: undefined });
      this.manejarError(error);
    } finally {
      this.verificandoUnidad.set(false);
    }
  }

  private manejarError(error: unknown): void {
    const problem = parseProblemDetails(error);
    this.problem.set(problem);
    this.submitting.set(false);
  }

  private buildForm(): FormGroup<DirectProjectFormGroup> {
    return this.fb.group<DirectProjectFormGroup>({
      tipoOrigen: this.fb.control<TipoOrigenDirecto | null>(null, {
        validators: [Validators.required]
      }),
      codigoOrigen: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.maxLength(MAX_CODIGO_ORIGEN)]
      }),
      fechaInicio: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.required]
      }),
      nombre: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(MAX_NOMBRE)]
      }),
      objetivoPeiId: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(1)]
      }),
      actividadPoiId: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(1)]
      }),
      unidadResponsableId: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(1)]
      }),
      responsableId: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(1)]
      }),
      descripcion: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(MAX_DESCRIPCION)]
      }),
      componenteDigital: this.fb.control<boolean>(false, { nonNullable: true }),
      detalleComponenteDigital: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.maxLength(MAX_DETALLE_COMPONENTE)]
      }),
      nota: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.maxLength(MAX_NOTA)]
      }),
      documentoAutorizacionId: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(1)]
      }),
      evidenciaIds: this.fb.control<number[]>([], {
        nonNullable: true,
        validators: [Validators.required, Validators.minLength(1)]
      }),
      fuenteOrigen: this.fb.control<FuenteOrigen | null>(null, {
        validators: [Validators.required]
      }),
      detalleFuente: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.maxLength(MAX_DETALLE_FUENTE)]
      })
    });
  }
}