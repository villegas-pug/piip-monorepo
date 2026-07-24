// Página de evaluación de iniciativa (US2 - T059).
//
// Recorrido del Evaluador: carga una iniciativa en `PRESENTADO` y registra
// la decisión de admisibilidad y, si aplica, de aplicabilidad. La
// subsanación única se modela como acciones explícitas (abrir, editar,
// cerrar) sin decisión local: cada comando exige `If-Match` con la ETag
// devuelta por la última lectura.
//
// El componente NO decide transiciones, NO valida la admisibilidad ni
// calcula plazos de subsanación: el backend es la autoridad efectiva. La
// ETag se propaga al servicio en cada comando mutable y la respuesta se
// refleja en los `signal` internos.
//
// Accesibilidad WCAG 2.1 AA:
//   * Foco visible y navegación por teclado en cada control.
//   * Labels asociados (`for`/`id`) y mensajes de error con `aria-describedby`.
//   * Regiones `aria-live` separadas para errores de servidor y resultados.
//   * `aria-controls` en botones de cancelación para vincularlos al formulario.
//   * Contraste mínimo 4.5:1 delegado al tema PIIP (piip-theme.scss).
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
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';

import { parseProblemDetails, ProblemDetails, ProblemViolation } from '../../../core/http/problem-details';
import { EvaluacionApiService } from './api/evaluacion-api.service';
import {
  AdmissibilityRequest,
  AdmissibilityResultado,
  ApplicabilityCriterion,
  ApplicabilityRequest,
  ApplicabilityResultado,
  EvaluacionDetail,
  InitiativeEvaluationContext
} from './api/types';

interface AdmisibilidadFormGroup {
  resultado: FormControl<AdmissibilityResultado | null>;
  observacion: FormControl<string>;
  documentoOpinionId: FormControl<number | null>;
}

interface CriterioAplicabilidadFormGroup {
  codigo: FormControl<string>;
  cumple: FormControl<boolean>;
  observacion: FormControl<string>;
}

interface AplicabilidadFormGroup {
  resultado: FormControl<ApplicabilityResultado | null>;
  motivo: FormControl<string>;
  criterios: FormArray<FormGroup<CriterioAplicabilidadFormGroup>>;
}

const MAX_OBSERVACION = 2000;
const MAX_MOTIVO = 2000;
const MAX_CRITERIO_OBSERVACION = 500;
const CRITERIOS_CATALOGO: readonly { codigo: string; etiqueta: string }[] = [
  { codigo: 'COMPETENCIA_MIDAGRI', etiqueta: 'Competencia MIDAGRI' },
  { codigo: 'VALOR_PUBLICO', etiqueta: 'Valor público' },
  { codigo: 'CARACTER_INNOVADOR', etiqueta: 'Carácter innovador' }
];

const FORM_ADMISIBILIDAD_ID = 'evaluation-admisibilidad-form';
const FORM_APLICABILIDAD_ID = 'evaluation-aplicabilidad-form';
const ID_RESULTADO_ADMISIBILIDAD = 'evaluation-admisibilidad-resultado';
const ID_OBSERVACION_ADMISIBILIDAD = 'evaluation-admisibilidad-observacion';
const ID_DOCUMENTO_ADMISIBILIDAD = 'evaluation-admisibilidad-documento';
const ID_RESULTADO_APLICABILIDAD = 'evaluation-aplicabilidad-resultado';
const ID_MOTIVO_APLICABILIDAD = 'evaluation-aplicabilidad-motivo';

@Component({
  selector: 'app-evaluation-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatRadioModule,
    MatSelectModule
  ],
  templateUrl: './evaluation-page.component.html',
  styleUrl: './evaluation-page.component.scss'
})
export class EvaluationPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly evaluacion = inject(EvaluacionApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly host = inject(ElementRef<HTMLElement>);

  /** Identificador de la iniciativa. Si se omite, se lee de `ActivatedRoute.paramMap`. */
  readonly iniciativaId = input<number | null>(null);

  /** Notifica al shell cuando el Evaluador cancela la evaluación. */
  readonly cancelar = output<void>();

  protected readonly resultadosAdmisibilidad: readonly AdmissibilityResultado[] = [
    'ADMITIDA',
    'NO_ADMISIBLE'
  ];
  protected readonly resultadosAplicabilidad: readonly ApplicabilityResultado[] = [
    'APLICABLE',
    'NO_APLICABLE'
  ];
  protected readonly criteriosCatalogo: readonly { codigo: string; etiqueta: string }[] =
    CRITERIOS_CATALOGO;

  protected readonly formAdmisibilidad: FormGroup<AdmisibilidadFormGroup> = this.buildFormAdmisibilidad();
  protected readonly formAplicabilidad: FormGroup<AplicabilidadFormGroup> = this.buildFormAplicabilidad();

  protected readonly submitted = signal(false);
  protected readonly submitting = signal(false);
  protected readonly iniciativa = signal<InitiativeEvaluationContext | undefined>(undefined);
  protected readonly etagActual = signal<string | undefined>(undefined);
  protected readonly ultimaEvaluacion = signal<EvaluacionDetail | undefined>(undefined);
  protected readonly problem = signal<ProblemDetails | undefined>(undefined);
  protected readonly atajoTecladoActivo = signal(true);

  protected readonly permiteAdmisibilidad = computed(() => this.iniciativa()?.estado === 'PRESENTADO');
  protected readonly permiteAplicabilidad = computed(() => this.iniciativa()?.estado === 'PRESENTADO');
  protected readonly estadoBloqueante = computed(() => {
    const iniciativa = this.iniciativa();
    return iniciativa && iniciativa.estado !== 'PRESENTADO' ? iniciativa.estado : undefined;
  });

  /** Identificador resuelto a partir del `paramMap` de la ruta. */
  private readonly idResuelto = toSignal(this.route.paramMap.pipe(takeUntilDestroyed()), {
    initialValue: this.route.snapshot.paramMap
  });

  // IDs del DOM utilizados por los specs de T055 para WCAG 2.1 AA.
  protected readonly formAdmisibilidadId = FORM_ADMISIBILIDAD_ID;
  protected readonly formAplicabilidadId = FORM_APLICABILIDAD_ID;
  protected readonly idResultadoAdmisibilidad = ID_RESULTADO_ADMISIBILIDAD;
  protected readonly idObservacionAdmisibilidad = ID_OBSERVACION_ADMISIBILIDAD;
  protected readonly idDocumentoAdmisibilidad = ID_DOCUMENTO_ADMISIBILIDAD;
  protected readonly idResultadoAplicabilidad = ID_RESULTADO_APLICABILIDAD;
  protected readonly idMotivoAplicabilidad = ID_MOTIVO_APLICABILIDAD;

  constructor() {
    // Recarga la iniciativa cada vez que cambia el `iniciativaId` resuelto.
    effect(() => {
      const id = this.resolverId();
      if (id !== null) {
        void this.cargarIniciativa(id);
      }
    });

    // El motivo de aplicabilidad es obligatorio solo cuando el resultado es
    // `NO_APLICABLE`; se revalida sin emitir eventos.
    this.formAplicabilidad.controls.resultado.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((valor) => {
        const motivo = this.formAplicabilidad.controls.motivo;
        if (valor === 'NO_APLICABLE') {
          motivo.addValidators([Validators.required, Validators.maxLength(MAX_MOTIVO)]);
        } else {
          motivo.removeValidators([Validators.required]);
        }
        motivo.updateValueAndValidity({ emitEvent: false });
      });
  }

  ngOnInit(): void {
    // Mueve el foco al primer control del primer formulario para usuarios
    // que navegan con teclado. Se ejecuta tras la primera renderización.
    queueMicrotask(() => this.enfocarPrimerControl());
  }

  // ---------------------------------------------------------------------------
  // Acciones de admisibilidad
  // ---------------------------------------------------------------------------

  protected async registrarAdmisibilidad(): Promise<void> {
    this.submitted.set(true);
    this.problem.set(undefined);
    if (this.formAdmisibilidad.invalid) {
      this.formAdmisibilidad.markAllAsTouched();
      return;
    }
    const iniciativa = this.iniciativa();
    const etag = this.etagActual();
    if (!iniciativa || !etag) {
      this.problem.set({
        type: 'about:blank',
        title: 'Contexto requerido',
        status: 400,
        code: 'EVALUATION_CONTEXT_REQUIRED',
        detail: 'No se puede registrar la admisibilidad sin iniciativa ni ETag.',
        correlationId: 'N/A',
        violations: []
      });
      return;
    }

    const payload: AdmissibilityRequest = {
      resultado: this.formAdmisibilidad.controls.resultado.value as AdmissibilityResultado,
      observacion: this.formAdmisibilidad.controls.observacion.value.trim(),
      documentoOpinionId: this.formAdmisibilidad.controls.documentoOpinionId.value as number
    };

    this.submitting.set(true);
    this.evaluacion.registrarAdmisibilidad(iniciativa.id, payload, { etag }).subscribe({
      next: (detalle) => {
        this.ultimaEvaluacion.set(detalle);
        this.etagActual.set(detalle.etag);
        this.problem.set(undefined);
        this.submitting.set(false);
        this.submitted.set(false);
      },
      error: (error: unknown) => this.manejarError(error)
    });
  }

  protected cancelarAdmisibilidad(): void {
    this.formAdmisibilidad.reset();
    this.problem.set(undefined);
  }

  // ---------------------------------------------------------------------------
  // Acciones de aplicabilidad
  // ---------------------------------------------------------------------------

  protected async registrarAplicabilidad(): Promise<void> {
    this.submitted.set(true);
    this.problem.set(undefined);
    if (this.formAplicabilidad.invalid) {
      this.formAplicabilidad.markAllAsTouched();
      return;
    }
    const iniciativa = this.iniciativa();
    if (!iniciativa) {
      this.problem.set({
        type: 'about:blank',
        title: 'Contexto requerido',
        status: 400,
        code: 'EVALUATION_CONTEXT_REQUIRED',
        detail: 'No se puede registrar la aplicabilidad sin iniciativa.',
        correlationId: 'N/A',
        violations: []
      });
      return;
    }

    const gruposCriterios = this.formAplicabilidad.controls.criterios.value as readonly {
      codigo: string;
      cumple: boolean;
      observacion: string;
    }[];
    const criterios: ApplicabilityCriterion[] = gruposCriterios.map((c) => ({
      codigo: c.codigo,
      cumple: c.cumple,
      observacion: c.observacion.trim()
    }));

    const payload: ApplicabilityRequest = {
      resultado: this.formAplicabilidad.controls.resultado.value as ApplicabilityResultado,
      motivo: this.formAplicabilidad.controls.motivo.value.trim() || undefined,
      criterios
    };

    this.submitting.set(true);
    // El endpoint de aplicabilidad no exige If-Match: se omite para que el
    // entityTagInterceptor no incluya la cabecera.
    this.evaluacion.registrarAplicabilidad(iniciativa.id, payload).subscribe({
      next: (detalle) => {
        this.ultimaEvaluacion.set(detalle);
        this.etagActual.set(detalle.etag);
        this.problem.set(undefined);
        this.submitting.set(false);
        this.submitted.set(false);
      },
      error: (error: unknown) => this.manejarError(error)
    });
  }

  protected cancelarAplicabilidad(): void {
    this.formAplicabilidad.reset();
    this.problem.set(undefined);
  }

  // ---------------------------------------------------------------------------
  // Acciones de subsanación (UI mínima; el backend es la autoridad)
  // ---------------------------------------------------------------------------

  protected async revalidarETag(): Promise<void> {
    const id = this.resolverId();
    if (id === null) {
      return;
    }
    await this.cargarIniciativa(id, { silencioso: true });
  }

  // ---------------------------------------------------------------------------
  // Cancelación y atajos de teclado
  // ---------------------------------------------------------------------------

  protected cancelarEvaluacion(): void {
    this.formAdmisibilidad.reset();
    this.formAplicabilidad.reset();
    this.problem.set(undefined);
    this.cancelar.emit();
  }

  protected alPulsarEscape(): void {
    if (this.formAdmisibilidad.dirty) {
      this.cancelarAdmisibilidad();
    } else if (this.formAplicabilidad.dirty) {
      this.cancelarAplicabilidad();
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

  protected mensajeErrorAdmisibilidad(campo: keyof AdmisibilidadFormGroup): string | null {
    const control = this.formAdmisibilidad.controls[campo];
    if (!control.errors || !(control.touched || this.submitted())) {
      return null;
    }
    if (control.hasError('required')) {
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
    return null;
  }

  protected mensajeErrorAplicabilidad(campo: keyof AplicabilidadFormGroup): string | null {
    const control = this.formAplicabilidad.controls[campo];
    if (!control.errors || !(control.touched || this.submitted())) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Este campo es obligatorio.';
    }
    if (control.hasError('maxlength')) {
      const requerido = control.getError('maxlength') as { requiredLength: number };
      return `Máximo ${requerido.requiredLength} caracteres.`;
    }
    return null;
  }

  protected mensajeErrorCriterio(
    grupo: FormGroup<CriterioAplicabilidadFormGroup>,
    campo: keyof CriterioAplicabilidadFormGroup
  ): string | null {
    const control = grupo.controls[campo];
    if (!control.errors || !control.touched) {
      return null;
    }
    if (control.hasError('required')) {
      return 'Este campo es obligatorio.';
    }
    if (control.hasError('maxlength')) {
      const requerido = control.getError('maxlength') as { requiredLength: number };
      return `Máximo ${requerido.requiredLength} caracteres.`;
    }
    return null;
  }

  protected get criteriosAplicabilidad(): FormArray<FormGroup<CriterioAplicabilidadFormGroup>> {
    return this.formAplicabilidad.controls.criterios;
  }

  // ---------------------------------------------------------------------------
  // Carga inicial y manejo de errores
  // ---------------------------------------------------------------------------

  private async cargarIniciativa(id: number, opciones: { silencioso?: boolean } = {}): Promise<void> {
    try {
      const respuesta = await firstValueFrom(this.evaluacion.consultarIniciativa(id));
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

  private buildFormAdmisibilidad(): FormGroup<AdmisibilidadFormGroup> {
    return this.fb.group<AdmisibilidadFormGroup>({
      resultado: this.fb.control<AdmissibilityResultado | null>(null, {
        validators: [Validators.required]
      }),
      observacion: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(MAX_OBSERVACION)]
      }),
      documentoOpinionId: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(1)]
      })
    });
  }

  private buildFormAplicabilidad(): FormGroup<AplicabilidadFormGroup> {
    const criterios = this.fb.array<FormGroup<CriterioAplicabilidadFormGroup>>(
      CRITERIOS_CATALOGO.map((catalogo) =>
        this.fb.group<CriterioAplicabilidadFormGroup>({
          codigo: this.fb.control<string>(catalogo.codigo, { nonNullable: true }),
          cumple: this.fb.control<boolean>(false, { nonNullable: true }),
          observacion: this.fb.control<string>('', {
            nonNullable: true,
            validators: [Validators.maxLength(MAX_CRITERIO_OBSERVACION)]
          })
        })
      ),
      { validators: [Validators.minLength(1)] }
    );
    return this.fb.group<AplicabilidadFormGroup>({
      resultado: this.fb.control<ApplicabilityResultado | null>(null, {
        validators: [Validators.required]
      }),
      motivo: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.maxLength(MAX_MOTIVO)]
      }),
      criterios
    });
  }
}
