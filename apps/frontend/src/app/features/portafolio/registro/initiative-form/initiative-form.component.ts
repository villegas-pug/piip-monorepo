// Formulario de presentación de iniciativa. Cubre los campos oficiales 5 a 12, 22 y 23 de la
// matriz 013 más la ficha documental obligatoria (`fichaDocumentoVersionId` en `CreateInitiativeRequest`).
// Campos 1, 2, 3, 4 y 13 son generados por el backend; el cliente solo los refleja.
//
// El formulario no decide transiciones, no asigna código, no valida hashes: el backend
// es la autoridad efectiva. Cualquier error se traduce desde `ProblemDetail` para
// garantizar un único formato de mensaje.
//
// Accesibilidad WCAG 2.1 AA:
//   - Foco visible y navegación por teclado en cada control.
//   - Labels asociados (`for`/`id`) y mensajes de error con `aria-describedby`.
//   - Regiones `aria-live` para errores de servidor y feedback de envío.
//   - Contraste mínimo 4.5:1 delegado al tema PIIP (piip-theme.scss).
//   - Roles ARIA correctos en fieldsets de grupos de controles.

import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { Observable, debounceTime, distinctUntilChanged, of, switchMap } from 'rxjs';

import { DocumentosApiService } from '../../../documentos/api/documentos-api.service';
import { EffectiveAssignmentSelectorComponent } from '../../../../core/effective-assignment/effective-assignment-selector.component';
import { OrganizacionApiService } from '../../../organizacion/api/organizacion-api.service';
import { parseProblemDetails, ProblemDetails, ProblemViolation } from '../../../../core/http/problem-details';
import { CreateInitiativeRequest, InitiativeDetail, UnidadResponsableItem } from '../api/types/iniciativa.types';
import { FuenteOrigen, TipoSolucion } from '../api/types/common.types';
import { RegistroApiService } from '../api/registro-api.service';
import { DocumentVersionDetail, UploadDocumentRequest } from '../api/types/documentos.types';

interface UnidadResponsableFormGroup {
  unidadId: FormControl<number>;
  principal: FormControl<boolean>;
}

interface ParticipantePersonaFormGroup {
  nombresCompletos: FormControl<string>;
  institucion: FormControl<string>;
  funcion: FormControl<string>;
}

interface InitiativeFormGroup {
  nombre: FormControl<string>;
  tipoSolucion: FormControl<TipoSolucion | null>;
  fuenteOrigen: FormControl<FuenteOrigen | null>;
  detalleFuente: FormControl<string>;
  problemaPublico: FormControl<string>;
  solucionPropuesta: FormControl<string>;
  responsableId: FormControl<number | null>;
  objetivoPeiId: FormControl<number | null>;
  actividadPoiId: FormControl<number | null>;
  unidades: FormArray<FormGroup<UnidadResponsableFormGroup>>;
  participantesPersona: FormArray<FormGroup<ParticipantePersonaFormGroup>>;
  componenteDigital: FormControl<boolean>;
  detalleComponenteDigital: FormControl<string>;
  nota: FormControl<string>;
  fichaDocumentoVersionId: FormControl<number | null>;
}

const MAX_NOMBRE = 500;
const MAX_DETALLE_FUENTE = 500;
const MAX_DESCRIPCION = 2000;
const MAX_DETALLE_COMPONENTE = 500;
const MAX_NOTA = 1000;

@Component({
  selector: 'app-initiative-form',
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
    MatRadioModule,
    MatSelectModule,
    EffectiveAssignmentSelectorComponent
  ],
  templateUrl: './initiative-form.component.html',
  styleUrl: './initiative-form.component.scss'
})
export class InitiativeFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly organizacion = inject(OrganizacionApiService);
  private readonly documentos = inject(DocumentosApiService);
  private readonly registro = inject(RegistroApiService);

  protected readonly form: FormGroup<InitiativeFormGroup> = this.buildForm();
  protected readonly unidades = this.form.controls.unidades;
  protected readonly participantes = this.form.controls.participantesPersona;

  protected readonly fuenteOrigenOpciones: readonly FuenteOrigen[] = [
    'FICHA_INICIATIVA',
    'CONCURSO_INTERNO',
    'INNOVACION_ABIERTA',
    'PROPUESTA_JEFATURA',
    'OTROS'
  ];
  protected readonly tipoSolucionOpciones: readonly TipoSolucion[] = ['POTENCIAL_ADAPTABLE', 'POR_DEFINIR'];

  protected readonly submitted = signal(false);
  protected readonly submitting = signal(false);
  protected readonly problem = signal<ProblemDetails | undefined>(undefined);
  protected readonly submittedInitiative = signal<InitiativeDetail | undefined>(undefined);
  protected readonly fichaArchivo = signal<File | undefined>(undefined);
  protected readonly fichaError = signal<string | undefined>(undefined);

  protected readonly unidadesCatalogo = signal<readonly { id: number; codigo: string; nombre: string }[]>([]);
  protected readonly objetivosPei = signal<readonly { id: number; codigo: string; descripcion: string }[]>([]);
  protected readonly actividadesPoi = signal<readonly { id: number; codigo: string; descripcion: string }[]>([]);

  ngOnInit(): void {
    this.cargarCatalogos();
    this.escucharCambioFuente();
    this.escucharCambioComponenteDigital();
    this.escucharCambioUnidades();
    this.escucharCambioResponsable();
  }

  protected agregarUnidad(): void {
    this.unidades.push(
      this.fb.group<UnidadResponsableFormGroup>({
        unidadId: this.fb.nonNullable.control<number>(0, { validators: [Validators.required] }),
        principal: this.fb.control<boolean>(false, { nonNullable: true })
      })
    );
  }

  protected removerUnidad(indice: number): void {
    this.unidades.removeAt(indice);
  }

  protected marcarUnidadPrincipal(indice: number): void {
    this.unidades.controls.forEach((control, idx) => {
      control.controls.principal.setValue(idx === indice, { emitEvent: false });
    });
  }

  protected agregarParticipante(): void {
    this.participantes.push(
      this.fb.group<ParticipantePersonaFormGroup>({
        nombresCompletos: this.fb.control<string>('', {
          nonNullable: true,
          validators: [Validators.required, Validators.maxLength(200)]
        }),
        institucion: this.fb.control<string>('', { nonNullable: true, validators: [Validators.maxLength(200)] }),
        funcion: this.fb.control<string>('', { nonNullable: true, validators: [Validators.maxLength(200)] })
      })
    );
  }

  protected removerParticipante(indice: number): void {
    this.participantes.removeAt(indice);
  }

  protected async onFichaSeleccionada(evento: Event): Promise<void> {
    const input = evento.target as HTMLInputElement;
    const archivo = input.files?.[0];
    this.fichaError.set(undefined);
    this.fichaArchivo.set(undefined);
    this.form.controls.fichaDocumentoVersionId.setValue(null);

    if (!archivo) {
      return;
    }

    const validacionFormato = validarFormatoFicha(archivo);
    if (validacionFormato) {
      this.fichaError.set(validacionFormato);
      input.value = '';
      return;
    }

    this.fichaArchivo.set(archivo);
  }

  protected async enviar(): Promise<void> {
    this.submitted.set(true);
    this.problem.set(undefined);
    this.submittedInitiative.set(undefined);

    if (this.form.invalid || this.unidades.length === 0) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    try {
      const fichaId = await this.cargarFichaSiAplica();
      if (fichaId === undefined) {
        this.submitting.set(false);
        return;
      }

      const payload = this.construirPayload(fichaId as number);
      this.registro.presentarIniciativa(payload).subscribe({
        next: (detalle) => {
          this.submittedInitiative.set(detalle);
          this.submitting.set(false);
          this.form.reset();
          this.unidades.clear();
          this.participantes.clear();
          this.submitted.set(false);
        },
        error: (error: unknown) => {
          this.problem.set(parseProblemDetails(error));
          this.submitting.set(false);
        }
      });
    } catch (error) {
      this.fichaError.set(error instanceof Error ? error.message : 'No se pudo cargar la ficha.');
      this.submitting.set(false);
    }
  }

  protected violacionesPara(campo: string): readonly ProblemViolation[] {
    const problem = this.problem();
    if (!problem) {
      return [];
    }
    return problem.violations.filter((violation) => violation.field === campo);
  }

  protected mensajeError(campo: keyof InitiativeFormGroup): string | null {
    const control = this.form.controls[campo];
    if (!control.errors || !(control.touched || this.submitted())) {
      return null;
    }
    if (control.hasError('required')) {
      if (campo === 'detalleFuente' && this.form.controls.fuenteOrigen.value === 'OTROS') {
        return 'Cuando la fuente es OTROS debe describirla.';
      }
      if (campo === 'detalleComponenteDigital' && this.form.controls.componenteDigital.value) {
        return 'Cuando el componente digital es Sí debe describirlo.';
      }
      return 'Este campo es obligatorio.';
    }
    if (control.hasError('maxlength')) {
      const requerido = control.getError('maxlength') as { requiredLength: number };
      return `Máximo ${requerido.requiredLength} caracteres.`;
    }
    if (control.hasError('min')) {
      const requerido = control.getError('min') as { min: number };
      return `Debe ser mayor o igual a ${requerido.min}.`;
    }
    if (control.hasError('detalleFuenteRequerido')) {
      return 'Cuando la fuente es OTROS debe describirla.';
    }
    if (control.hasError('detalleComponenteRequerido')) {
      return 'Cuando el componente digital es Sí debe describirlo.';
    }
    if (control.hasError('unidadesInvalidas')) {
      return 'Debe registrar al menos una unidad y exactamente una principal.';
    }
    return null;
  }

  private buildForm(): FormGroup<InitiativeFormGroup> {
    return this.fb.group<InitiativeFormGroup>({
      nombre: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(MAX_NOMBRE)]
      }),
      tipoSolucion: this.fb.control<TipoSolucion | null>(null, { validators: [Validators.required] }),
      fuenteOrigen: this.fb.control<FuenteOrigen | null>(null, { validators: [Validators.required] }),
      detalleFuente: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.maxLength(MAX_DETALLE_FUENTE)]
      }),
      problemaPublico: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(MAX_DESCRIPCION)]
      }),
      solucionPropuesta: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.maxLength(MAX_DESCRIPCION)]
      }),
      responsableId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      objetivoPeiId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      actividadPoiId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      unidades: this.fb.array<FormGroup<UnidadResponsableFormGroup>>([], [unidadesValidator]),
      participantesPersona: this.fb.array<FormGroup<ParticipantePersonaFormGroup>>([]),
      componenteDigital: this.fb.control<boolean>(false, { nonNullable: true }),
      detalleComponenteDigital: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.maxLength(MAX_DETALLE_COMPONENTE)]
      }),
      nota: this.fb.control<string>('', { nonNullable: true, validators: [Validators.maxLength(MAX_NOTA)] }),
      fichaDocumentoVersionId: this.fb.control<number | null>(null, { validators: [Validators.min(1)] })
    });
  }

  private cargarCatalogos(): void {
    this.organizacion.consultarUnidades({ activa: true, size: 200 }).subscribe({
      next: (pagina) => this.unidadesCatalogo.set(pagina.items.map((u) => ({ id: u.id, codigo: u.codigo, nombre: u.nombre }))),
      error: () => this.unidadesCatalogo.set([])
    });
    this.organizacion.consultarObjetivosPei({ size: 200 }).subscribe({
      next: (pagina) =>
        this.objetivosPei.set(pagina.items.map((p) => ({ id: p.id, codigo: p.codigo, descripcion: p.descripcion }))),
      error: () => this.objetivosPei.set([])
    });
    this.organizacion.consultarActividadesPoi({ size: 200 }).subscribe({
      next: (pagina) =>
        this.actividadesPoi.set(pagina.items.map((p) => ({ id: p.id, codigo: p.codigo, descripcion: p.descripcion }))),
      error: () => this.actividadesPoi.set([])
    });
  }

  private escucharCambioFuente(): void {
    this.form.controls.fuenteOrigen.valueChanges.subscribe((valor) => {
      const detalle = this.form.controls.detalleFuente;
      if (valor === 'OTROS') {
        detalle.addValidators([Validators.required, Validators.maxLength(MAX_DETALLE_FUENTE)]);
      } else {
        detalle.removeValidators([Validators.required]);
      }
      detalle.updateValueAndValidity({ emitEvent: false });
    });
  }

  private escucharCambioComponenteDigital(): void {
    this.form.controls.componenteDigital.valueChanges.subscribe((valor) => {
      const detalle = this.form.controls.detalleComponenteDigital;
      if (valor) {
        detalle.addValidators([Validators.required, Validators.maxLength(MAX_DETALLE_COMPONENTE)]);
      } else {
        detalle.removeValidators([Validators.required]);
      }
      detalle.updateValueAndValidity({ emitEvent: false });
    });
  }

  private escucharCambioUnidades(): void {
    this.unidades.valueChanges.subscribe(() => {
      this.unidades.updateValueAndValidity({ emitEvent: false });
    });
  }

  private escucharCambioResponsable(): void {
    this.form.controls.responsableId.valueChanges
      .pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap((valor) => this.validarResponsableUnico(valor))
      )
      .subscribe((errores) => {
        const control = this.form.controls.responsableId;
        if (errores) {
          control.setErrors(errores);
        } else {
          const current = { ...control.errors } as Record<string, unknown>;
          delete current['responsableDuplicado'];
          control.setErrors(Object.keys(current).length ? current : null);
        }
      });
  }

  /** Hook asíncrono: el backend aún no expone endpoint de lookup único, pero la estructura queda lista. */
  private validarResponsableUnico(_valor: number | null): Observable<ValidationErrors | null> {
    return of(null);
  }

  private async cargarFichaSiAplica(): Promise<number | null | undefined> {
    const archivo = this.fichaArchivo();
    if (!archivo) {
      this.fichaError.set('La ficha documental es obligatoria al presentar una iniciativa.');
      return undefined;
    }

    const metadata: UploadDocumentRequest = {
      owner: { tipo: 'PORTAFOLIO' },
      tipoDocumentoId: 0, // El backend resuelve el tipo documental de ficha de iniciativa.
      titulo: `Ficha de iniciativa - ${archivo.name}`,
      clasificacionPropuesta: 'INTERNO',
      uso: 'FICHA_INICIATIVA'
    };

    return new Promise<number | null | undefined>((resolver) => {
      this.documentos.cargar(archivo, metadata).subscribe({
        next: (respuesta) => {
          const detalle: DocumentVersionDetail = respuesta.detail;
          this.form.controls.fichaDocumentoVersionId.setValue(detalle.documentoId);
          resolver(detalle.documentoId);
        },
        error: (error: unknown) => {
          const problem = parseProblemDetails(error as HttpErrorResponse);
          this.fichaError.set(problem?.detail ?? problem?.title ?? 'No se pudo cargar la ficha documental.');
          resolver(undefined);
        }
      });
    });
  }

  private construirPayload(fichaId: number): CreateInitiativeRequest {
    const unidades: UnidadResponsableItem[] = this.unidades.controls.map((control) => ({
      unidadId: control.controls.unidadId.value as number,
      principal: control.controls.principal.value
    }));
    const participantes = this.participantes.controls.map((control) => ({
      nombresCompletos: control.controls.nombresCompletos.value,
      institucion: control.controls.institucion.value || undefined,
      funcion: control.controls.funcion.value || undefined
    }));

    return {
      nombre: this.form.controls.nombre.value.trim(),
      tipoSolucion: this.form.controls.tipoSolucion.value as TipoSolucion,
      fuenteOrigen: this.form.controls.fuenteOrigen.value as FuenteOrigen,
      detalleFuente: this.form.controls.detalleFuente.value.trim() || undefined,
      problemaPublico: this.form.controls.problemaPublico.value.trim(),
      solucionPropuesta: this.form.controls.solucionPropuesta.value.trim() || undefined,
      responsableId: this.form.controls.responsableId.value as number,
      objetivoPeiId: this.form.controls.objetivoPeiId.value as number,
      actividadPoiId: this.form.controls.actividadPoiId.value as number,
      unidades,
      participantesPersona: participantes.length > 0 ? participantes : undefined,
      componenteDigital: this.form.controls.componenteDigital.value,
      detalleComponenteDigital: this.form.controls.detalleComponenteDigital.value.trim() || undefined,
      nota: this.form.controls.nota.value.trim() || undefined,
      fichaDocumentoVersionId: fichaId
    };
  }
}

function unidadesValidator(control: AbstractControl): ValidationErrors | null {
  const array = control as FormArray<FormGroup<UnidadResponsableFormGroup>>;
  if (array.length === 0) {
    return { unidadesInvalidas: true };
  }
  const principales = array.controls.filter((c) => c.controls.principal.value).length;
  if (principales !== 1) {
    return { unidadesInvalidas: true };
  }
  return null;
}

function validarFormatoFicha(archivo: File): string | null {
  const tamanoMaximo = 104_857_600; // 100 MB conforme al contrato de documentos.
  if (archivo.size < 1) {
    return 'La ficha documental no puede estar vacía.';
  }
  if (archivo.size > tamanoMaximo) {
    return 'La ficha documental supera el tamaño máximo permitido (100 MB).';
  }
  const extension = archivo.name.split('.').pop()?.toLowerCase() ?? '';
  const extensionesPermitidas = ['pdf', 'docx', 'xlsx', 'pptx', 'jpg', 'jpeg', 'png'];
  if (!extensionesPermitidas.includes(extension)) {
    return 'Formato no permitido. Use PDF, Office Open XML, JPEG o PNG.';
  }
  return null;
}
