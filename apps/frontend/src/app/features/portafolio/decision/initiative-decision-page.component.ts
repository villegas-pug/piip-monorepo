// Página de decisión formal de iniciativa (US2 - T059).
//
// Recorrido de la Autoridad: carga una iniciativa en `PRESENTADO` y registra
// la decisión formal de aprobación o archivo. El documento formal (campo 15)
// es obligatorio; la observación es opcional en aprobación y obligatoria en
// archivo. La transición se envía al endpoint canónico
// `POST /api/v1/portafolio/transiciones/{id}` mediante `DecisionApiService`
// con la ETag devuelta por la última lectura.
//
// El componente NO decide transiciones, NO evalúa documentos y NO calcula
// plazos: el backend es la autoridad efectiva. La ETag se propaga al servicio
// en cada comando y la respuesta se refleja en los `signal` internos.
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
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';

import { parseProblemDetails, ProblemDetails, ProblemViolation } from '../../../core/http/problem-details';
import { DecisionApiService } from './api/decision-api.service';
import {
  DecisionDestino,
  DecisionTransitionCommand,
  EstadoIniciativa,
  InitiativeDecisionContext,
  TransicionDetail
} from './api/types';

interface DecisionFormGroup {
  destino: FormControl<DecisionDestino | null>;
  documentoRefId: FormControl<number | null>;
  observaciones: FormControl<string>;
}

const MAX_OBSERVACIONES = 2000;
const ESTADOS_TERMINALES: ReadonlySet<EstadoIniciativa> = new Set<EstadoIniciativa>([
  'NO_ADMISIBLE',
  'NO_APLICABLE',
  'INICIATIVA_ARCHIVADA',
  'CANCELADO',
  'PRODUCTO_NO_APROBADO',
  'FINALIZADO'
]);

const FORM_ID = 'decision-form';
const ID_DESTINO = 'decision-destino';
const ID_DOCUMENTO = 'decision-documento';
const ID_OBSERVACIONES = 'decision-observaciones';

@Component({
  selector: 'app-initiative-decision-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  templateUrl: './initiative-decision-page.component.html',
  styleUrl: './initiative-decision-page.component.scss'
})
export class InitiativeDecisionPageComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly decision = inject(DecisionApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly host = inject(ElementRef<HTMLElement>);

  /** Identificador de la iniciativa. Si se omite, se lee de `ActivatedRoute.paramMap`. */
  readonly iniciativaId = input<number | null>(null);

  /** Notifica al shell cuando la Autoridad cancela la decisión. */
  readonly cancelar = output<void>();

  protected readonly destinos: readonly DecisionDestino[] = ['INICIATIVA_APROBADA', 'INICIATIVA_ARCHIVADA'];

  protected readonly formDecision: FormGroup<DecisionFormGroup> = this.buildForm();

  protected readonly submitted = signal(false);
  protected readonly submitting = signal(false);
  protected readonly iniciativa = signal<InitiativeDecisionContext | undefined>(undefined);
  protected readonly etagActual = signal<string | undefined>(undefined);
  protected readonly ultimaTransicion = signal<TransicionDetail | undefined>(undefined);
  protected readonly problem = signal<ProblemDetails | undefined>(undefined);
  protected readonly atajoTecladoActivo = signal(true);

  protected readonly permiteDecision = computed(() => this.iniciativa()?.estado === 'PRESENTADO');
  protected readonly estadoBloqueante = computed(() => {
    const iniciativa = this.iniciativa();
    return iniciativa && iniciativa.estado !== 'PRESENTADO' ? iniciativa.estado : undefined;
  });
  protected readonly estadoTerminal = computed(() => {
    const iniciativa = this.iniciativa();
    return iniciativa && ESTADOS_TERMINALES.has(iniciativa.estado) ? iniciativa.estado : undefined;
  });

  private readonly idResuelto = toSignal(this.route.paramMap.pipe(takeUntilDestroyed()), {
    initialValue: this.route.snapshot.paramMap
  });

  protected readonly formId = FORM_ID;
  protected readonly idDestino = ID_DESTINO;
  protected readonly idDocumento = ID_DOCUMENTO;
  protected readonly idObservaciones = ID_OBSERVACIONES;

  constructor() {
    // Recarga la iniciativa cada vez que cambia el `iniciativaId` resuelto.
    effect(() => {
      const id = this.resolverId();
      if (id !== null) {
        void this.cargarIniciativa(id);
      }
    });

    // La observación es obligatoria cuando el destino es INICIATIVA_ARCHIVADA;
    // se revalida sin emitir eventos.
    this.formDecision.controls.destino.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe((valor) => {
        const obs = this.formDecision.controls.observaciones;
        if (valor === 'INICIATIVA_ARCHIVADA') {
          obs.addValidators([Validators.required, Validators.maxLength(MAX_OBSERVACIONES)]);
        } else {
          obs.removeValidators([Validators.required]);
        }
        obs.updateValueAndValidity({ emitEvent: false });
      });
  }

  ngOnInit(): void {
    // Mueve el foco al primer control del formulario para usuarios que
    // navegan con teclado. Se ejecuta tras la primera renderización.
    queueMicrotask(() => this.enfocarPrimerControl());
  }

  // ---------------------------------------------------------------------------
  // Acciones
  // ---------------------------------------------------------------------------

  protected async confirmarDecision(): Promise<void> {
    this.submitted.set(true);
    this.problem.set(undefined);
    if (this.formDecision.invalid) {
      this.formDecision.markAllAsTouched();
      return;
    }
    const iniciativa = this.iniciativa();
    const etag = this.etagActual();
    if (!iniciativa || !etag) {
      this.problem.set({
        type: 'about:blank',
        title: 'Contexto requerido',
        status: 400,
        code: 'DECISION_CONTEXT_REQUIRED',
        detail: 'No se puede confirmar la decisión sin iniciativa ni ETag.',
        correlationId: 'N/A',
        violations: []
      });
      return;
    }

    const destino = this.formDecision.controls.destino.value as DecisionDestino;
    const comando: DecisionTransitionCommand = {
      destino,
      documentoRefId: this.formDecision.controls.documentoRefId.value as number,
      observaciones: this.formDecision.controls.observaciones.value.trim() || undefined
    };

    this.submitting.set(true);
    this.decision.transicionar(iniciativa.id, comando, { etag }).subscribe({
      next: (detalle) => {
        this.ultimaTransicion.set(detalle);
        this.etagActual.set(detalle.etag);
        // Refleja el nuevo estado en la iniciativa local.
        this.iniciativa.set({ ...iniciativa, estado: detalle.estadoNuevo, version: detalle.version });
        this.problem.set(undefined);
        this.submitting.set(false);
        this.submitted.set(false);
      },
      error: (error: unknown) => this.manejarError(error)
    });
  }

  protected cancelarDecision(): void {
    this.formDecision.reset();
    this.problem.set(undefined);
  }

  // ---------------------------------------------------------------------------
  // Cancelación y atajos de teclado
  // ---------------------------------------------------------------------------

  protected cancelarVista(): void {
    this.formDecision.reset();
    this.problem.set(undefined);
    this.cancelar.emit();
  }

  protected alPulsarEscape(): void {
    if (this.formDecision.dirty) {
      this.cancelarDecision();
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

  protected mensajeError(campo: keyof DecisionFormGroup): string | null {
    const control = this.formDecision.controls[campo];
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

  // ---------------------------------------------------------------------------
  // Carga inicial y manejo de errores
  // ---------------------------------------------------------------------------

  private async cargarIniciativa(id: number, opciones: { silencioso?: boolean } = {}): Promise<void> {
    try {
      const respuesta = await firstValueFrom(this.decision.consultarIniciativa(id));
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

  private buildForm(): FormGroup<DecisionFormGroup> {
    return this.fb.group<DecisionFormGroup>({
      destino: this.fb.control<DecisionDestino | null>(null, { validators: [Validators.required] }),
      documentoRefId: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(1)]
      }),
      observaciones: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.maxLength(MAX_OBSERVACIONES)]
      })
    });
  }
}
