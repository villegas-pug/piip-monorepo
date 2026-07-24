// Pagina de creacion de proyecto derivado (US3 - T067).
//
// Recorrido del Responsable: carga una iniciativa en `INICIATIVA_APROBADA`
// y registra el unico proyecto derivado. La ETag devuelta por la consulta
// de iniciativa se propaga al comando de creacion como `If-Match`; la
// cabecera `Idempotency-Key` la aplica automaticamente el
// `idempotencyKeyInterceptor` global.
//
// El componente NO genera codigo, NO decide la transicion a
// `PROYECTO_EJECUCION` y NO asigna responsables: el backend es la autoridad
// efectiva. Las reglas formales (cardinalidad de unidades, discriminador
// OTROS, documento formal obligatorio) se modelan en la capa de
// validacion de formularios; la autoridad definitiva reside en el
// backend.
//
// Accesibilidad WCAG 2.1 AA:
//   * Foco visible y navegacion por teclado en cada control.
//   * Labels asociados (`for`/`id`) y mensajes de error con `aria-describedby`.
//   * Regiones `aria-live` separadas para errores de servidor y resultados.
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
  signal
} from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
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
  CreateDerivedProjectCommand,
  FuenteOrigen,
  InitiativeDerivedContext,
  ProjectDetail,
  UnidadDerivadaItem
} from '../api/types/proyectos.types';

interface UnidadDerivadaFormGroup {
  unidadId: FormControl<number>;
  principal: FormControl<boolean>;
}

interface DerivedProjectFormGroup {
  nombre: FormControl<string>;
  objetivoPeiId: FormControl<number | null>;
  actividadPoiId: FormControl<number | null>;
  unidades: FormArray<FormGroup<UnidadDerivadaFormGroup>>;
  titularId: FormControl<number | null>;
  fuenteOrigen: FormControl<FuenteOrigen | null>;
  detalleFuente: FormControl<string>;
  descripcion: FormControl<string>;
  componenteDigital: FormControl<boolean>;
  detalleComponenteDigital: FormControl<string>;
  nota: FormControl<string>;
  documentoFormalId: FormControl<number | null>;
}

const MAX_NOMBRE = 500;
const MAX_DESCRIPCION = 2000;
const MAX_NOTA = 1000;
const MAX_DETALLE_FUENTE = 500;
const MAX_DETALLE_COMPONENTE = 500;
const TITULO_DOCUMENTO_FORMAL =
  'Documento Formal de Aprobacion o Autorizacion de Inicio';

const FORM_ID = 'derived-project-form';
const ID_NOMBRE = 'derived-nombre';
const ID_OBJETIVO_PEI = 'derived-objetivo-pei';
const ID_ACTIVIDAD_POI = 'derived-actividad-poi';
const ID_TITULAR = 'derived-titular';
const ID_FUENTE = 'derived-fuente';
const ID_DETALLE_FUENTE = 'derived-detalle-fuente';
const ID_DESCRIPCION = 'derived-descripcion';
const ID_COMPONENTE_DIGITAL = 'derived-componente-digital';
const ID_DETALLE_DIGITAL = 'derived-detalle-digital';
const ID_NOTA = 'derived-nota';
const ID_DOCUMENTO_FORMAL = 'derived-documento-formal';

const FUENTE_ORIGEN_OPCIONES: readonly FuenteOrigen[] = [
  'FICHA_INICIATIVA',
  'CONCURSO_INTERNO',
  'INNOVACION_ABIERTA',
  'PROPUESTA_JEFATURA',
  'OTROS'
];

/** Validador de cardinalidad: al menos una unidad y exactamente una principal. */
function unidadesCardinalidadValidator(): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const array = control as FormArray<FormGroup<UnidadDerivadaFormGroup>>;
    if (!array || array.length === 0) {
      return { unidadesInvalidas: true };
    }
    const principales = array.controls.filter((c) => c.controls.principal.value === true);
    if (principales.length !== 1) {
      return { unidadesInvalidas: true };
    }
    const todasConUnidad = array.controls.every((c) => {
      const id = c.controls.unidadId.value;
      return id !== null && Number.isFinite(id) && id >= 1;
    });
    if (!todasConUnidad) {
      return { unidadesInvalidas: true };
    }
    return null;
  };
}

@Component({
  selector: 'app-derived-project',
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
  templateUrl: './derived-project.component.html',
  styleUrl: './derived-project.component.scss'
})
export class DerivedProjectComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly proyectos = inject(ProyectosApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly host = inject(ElementRef<HTMLElement>);

  /** Identificador de la iniciativa. Si se omite, se lee de `ActivatedRoute.paramMap`. */
  readonly iniciativaId = input<number | null>(null);

  /** Notifica al shell cuando el Responsable cancela la operacion. */
  readonly cancelar = output<void>();

  protected readonly fuenteOrigenOpciones: readonly FuenteOrigen[] = FUENTE_ORIGEN_OPCIONES;
  protected readonly tituloDocumentoFormal = TITULO_DOCUMENTO_FORMAL;

  protected readonly form: FormGroup<DerivedProjectFormGroup> = this.buildForm();

  protected readonly submitted = signal(false);
  protected readonly submitting = signal(false);
  protected readonly iniciativa = signal<InitiativeDerivedContext | undefined>(undefined);
  protected readonly etagActual = signal<string | undefined>(undefined);
  protected readonly proyectoCreado = signal<ProjectDetail | undefined>(undefined);
  protected readonly problem = signal<ProblemDetails | undefined>(undefined);
  protected readonly atajoTecladoActivo = signal(true);

  protected readonly permiteCrearDerivado = computed(
    () => this.iniciativa()?.estado === 'INICIATIVA_APROBADA'
  );
  protected readonly estadoBloqueante = computed(() => {
    const iniciativa = this.iniciativa();
    return iniciativa && iniciativa.estado !== 'INICIATIVA_APROBADA'
      ? iniciativa.estado
      : undefined;
  });

  private readonly idResuelto = toSignal(this.route.paramMap.pipe(takeUntilDestroyed()), {
    initialValue: this.route.snapshot.paramMap
  });

  protected readonly formId = FORM_ID;
  protected readonly idNombre = ID_NOMBRE;
  protected readonly idObjetivoPei = ID_OBJETIVO_PEI;
  protected readonly idActividadPoi = ID_ACTIVIDAD_POI;
  protected readonly idTitular = ID_TITULAR;
  protected readonly idFuente = ID_FUENTE;
  protected readonly idDetalleFuente = ID_DETALLE_FUENTE;
  protected readonly idDescripcion = ID_DESCRIPCION;
  protected readonly idComponenteDigital = ID_COMPONENTE_DIGITAL;
  protected readonly idDetalleDigital = ID_DETALLE_DIGITAL;
  protected readonly idNota = ID_NOTA;
  protected readonly idDocumentoFormal = ID_DOCUMENTO_FORMAL;

  constructor() {
    // Recarga la iniciativa cada vez que cambia el `iniciativaId` resuelto.
    effect(() => {
      const id = this.resolverId();
      if (id !== null) {
        void this.cargarIniciativa(id);
      }
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
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const iniciativa = this.iniciativa();
    const etag = this.etagActual();
    if (!iniciativa || !etag) {
      this.problem.set({
        type: 'about:blank',
        title: 'Contexto requerido',
        status: 400,
        code: 'DERIVATION_CONTEXT_REQUIRED',
        detail: 'No se puede crear el proyecto derivado sin iniciativa ni ETag.',
        correlationId: 'N/A',
        violations: []
      });
      return;
    }

    const unidades: UnidadDerivadaItem[] = this.form.controls.unidades.controls.map((c) => ({
      unidadId: c.controls.unidadId.value as number,
      principal: c.controls.principal.value as boolean
    }));
    const fuenteOrigen = this.form.controls.fuenteOrigen.value as FuenteOrigen;
    const detalleFuenteValor = this.form.controls.detalleFuente.value.trim();
    const detalleDigitalValor = this.form.controls.detalleComponenteDigital.value.trim();

    const payload: CreateDerivedProjectCommand = {
      nombre: this.form.controls.nombre.value.trim(),
      objetivoPeiId: this.form.controls.objetivoPeiId.value as number,
      actividadPoiId: this.form.controls.actividadPoiId.value as number,
      unidades,
      titularId: this.form.controls.titularId.value as number,
      fuenteOrigen,
      detalleFuente:
        fuenteOrigen === 'OTROS' && detalleFuenteValor ? detalleFuenteValor : undefined,
      descripcion: this.form.controls.descripcion.value.trim(),
      componenteDigital: this.form.controls.componenteDigital.value,
      detalleComponenteDigital:
        this.form.controls.componenteDigital.value && detalleDigitalValor
          ? detalleDigitalValor
          : undefined,
      nota: this.form.controls.nota.value.trim() || undefined,
      documentoFormalId: this.form.controls.documentoFormalId.value as number
    };

    this.submitting.set(true);
    this.proyectos.crearProyectoDerivado(iniciativa.id, payload, { etag }).subscribe({
      next: (detalle) => {
        this.proyectoCreado.set(detalle);
        this.etagActual.set(detalle.etag);
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
  // Acciones de unidades (FormArray)
  // ---------------------------------------------------------------------------

  protected get unidades(): FormArray<FormGroup<UnidadDerivadaFormGroup>> {
    return this.form.controls.unidades;
  }

  protected agregarUnidad(): void {
    this.unidades.push(
      this.fb.group<UnidadDerivadaFormGroup>({
        unidadId: this.fb.nonNullable.control<number>(0, { validators: [Validators.min(1)] }),
        principal: this.fb.control<boolean>(false, { nonNullable: true })
      })
    );
    this.form.controls.unidades.updateValueAndValidity({ emitEvent: false });
  }

  protected quitarUnidad(indice: number): void {
    this.unidades.removeAt(indice);
    this.form.controls.unidades.updateValueAndValidity({ emitEvent: false });
  }

  // ---------------------------------------------------------------------------
  // Cancelacion y atajos de teclado
  // ---------------------------------------------------------------------------

  protected async revalidarETag(): Promise<void> {
    const id = this.resolverId();
    if (id === null) {
      return;
    }
    await this.cargarIniciativa(id, { silencioso: true });
  }

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
    );
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

  protected mensajeError(campo: keyof DerivedProjectFormGroup): string | null {
    const control = this.form.controls[campo];
    if (campo === 'unidades') {
      if (!control.errors || !(control.touched || this.submitted())) {
        return null;
      }
      if (control.hasError('unidadesInvalidas')) {
        return 'Debe registrar al menos una unidad y exactamente una principal.';
      }
      return null;
    }
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

  private async cargarIniciativa(
    id: number,
    opciones: { silencioso?: boolean } = {}
  ): Promise<void> {
    try {
      const respuesta = await firstValueFrom(
        this.proyectos.consultarIniciativaParaDerivar(id)
      );
      this.iniciativa.set(respuesta);
      this.etagActual.set(respuesta.etag);
      if (!opciones.silencioso) {
        this.problem.set(undefined);
      }
    } catch (error: unknown) {
      this.manejarError(error);
    }
  }

  private manejarError(error: unknown): void {
    const problem = parseProblemDetails(error);
    this.problem.set(problem);
    this.submitting.set(false);
  }

  private resolverId(): number | null {
    const desdeInput = this.iniciativaId();
    if (desdeInput !== null && desdeInput !== undefined && Number.isFinite(desdeInput)) {
      return desdeInput;
    }
    const paramMap = this.idResuelto();
    const crudo = paramMap?.get('id');
    if (!crudo) {
      return null;
    }
    const parsed = Number(crudo);
    return Number.isFinite(parsed) ? parsed : null;
  }

  private buildForm(): FormGroup<DerivedProjectFormGroup> {
    const unidades = this.fb.array<FormGroup<UnidadDerivadaFormGroup>>(
      [],
      { validators: [unidadesCardinalidadValidator()] }
    );
    return this.fb.group<DerivedProjectFormGroup>({
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
      unidades,
      titularId: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(1)]
      }),
      fuenteOrigen: this.fb.control<FuenteOrigen | null>(null, {
        validators: [Validators.required]
      }),
      detalleFuente: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.maxLength(MAX_DETALLE_FUENTE)]
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
      documentoFormalId: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(1)]
      })
    });
  }
}