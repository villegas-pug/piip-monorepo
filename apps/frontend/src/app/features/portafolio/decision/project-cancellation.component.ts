// Componente de cancelación formal de proyecto (US2 - T059).
//
// Recorrido de la Autoridad: carga un proyecto en `PROYECTO_EJECUCION` y
// registra la decisión de cancelación. El documento "Informe de la Oficina
// de Modernización, Cancelación" es obligatorio, así como la observación.
// La transición se envía al endpoint canónico
// `POST /api/v1/portafolio/transiciones/{id}` mediante `DecisionApiService`
// con la ETag devuelta por la última lectura.
//
// El componente NO decide transiciones ni calcula plazos: el backend es la
// autoridad efectiva. La ETag se propaga al servicio en cada comando y la
// respuesta se refleja en los `signal` internos.
//
// Accesibilidad WCAG 2.1 AA:
//   * Foco visible y navegación por teclado en cada control.
//   * Labels asociados (`for`/`id`) y mensajes de error con `aria-describedby`.
//   * Regiones `aria-live` separadas para errores de servidor y resultados.
//   * Diálogo de confirmación accesible: `role="alertdialog"`, `aria-modal`,
//     `aria-labelledby`, `aria-describedby` y foco inicial al primer botón.
//   * `Escape` revierte los cambios pendientes o cierra el diálogo.
//   * Contraste mínimo 4.5:1 delegado al tema PIIP (piip-theme.scss).

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
  viewChild
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
import { firstValueFrom } from 'rxjs';

import { parseProblemDetails, ProblemDetails, ProblemViolation } from '../../../core/http/problem-details';
import { DecisionApiService } from './api/decision-api.service';
import {
  CancellationDestino,
  EstadoIniciativa,
  InitiativeDecisionContext,
  ProjectCancellationCommand,
  TransicionDetail
} from './api/types';

interface CancellationFormGroup {
  documentoRefId: FormControl<number | null>;
  observaciones: FormControl<string>;
}

const MAX_OBSERVACIONES = 2000;
const TITULO_DOCUMENTO_OBLIGATORIO = 'Informe de la Oficina de Modernización, Cancelación';
const FORM_ID = 'cancellation-form';
const DIALOGO_ID = 'cancellation-confirmation-dialog';
const ID_DOCUMENTO = 'cancellation-documento';
const ID_OBSERVACIONES = 'cancellation-observaciones';
const ID_BOTON_MANTENER = 'cancellation-mantener-edicion';
const ID_BOTON_DESCARTAR = 'cancellation-descartar-cambios';

@Component({
  selector: 'app-project-cancellation',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './project-cancellation.component.html',
  styleUrl: './project-cancellation.component.scss'
})
export class ProjectCancellationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly decision = inject(DecisionApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly host = inject(ElementRef<HTMLElement>);

  /**
   * Identificador del proyecto. Si se omite, se lee de `ActivatedRoute.paramMap`.
   * Por convención se reutiliza el mismo segmento `id` que la iniciativa.
   */
  readonly proyectoId = input<number | null>(null);

  /** Notifica al shell cuando la Autoridad cancela la operación. */
  readonly cancelar = output<void>();

  protected readonly destinos: readonly CancellationDestino[] = ['CANCELADO'];
  protected readonly tituloDocumento = TITULO_DOCUMENTO_OBLIGATORIO;

  protected readonly formCancelacion: FormGroup<CancellationFormGroup> = this.buildForm();

  protected readonly submitted = signal(false);
  protected readonly submitting = signal(false);
  protected readonly proyecto = signal<InitiativeDecisionContext | undefined>(undefined);
  protected readonly etagActual = signal<string | undefined>(undefined);
  protected readonly ultimaTransicion = signal<TransicionDetail | undefined>(undefined);
  protected readonly problem = signal<ProblemDetails | undefined>(undefined);
  protected readonly mostrarDialogoConfirmacion = signal(false);
  protected readonly atajoTecladoActivo = signal(true);

  protected readonly permiteCancelacion = computed(() => this.proyecto()?.estado === 'PROYECTO_EJECUCION');
  protected readonly estadoBloqueante = computed(() => {
    const proyecto = this.proyecto();
    return proyecto && proyecto.estado !== 'PROYECTO_EJECUCION' ? proyecto.estado : undefined;
  });
  protected readonly estadoTerminal = computed(() => {
    const proyecto = this.proyecto();
    return proyecto && proyecto.estado === 'CANCELADO' ? proyecto.estado : undefined;
  });

  private readonly idResuelto = toSignal(this.route.paramMap.pipe(takeUntilDestroyed()), {
    initialValue: this.route.snapshot.paramMap
  });

  protected readonly formId = FORM_ID;
  protected readonly dialogoId = DIALOGO_ID;
  protected readonly idDocumento = ID_DOCUMENTO;
  protected readonly idObservaciones = ID_OBSERVACIONES;
  protected readonly idBotonMantener = ID_BOTON_MANTENER;
  protected readonly idBotonDescartar = ID_BOTON_DESCARTAR;
  protected readonly tituloDialogoId = 'cancellation-confirmation-title';
  protected readonly mensajeDialogoId = 'cancellation-confirmation-message';

  /** Referencia al botón "Mantener edición" para restaurar el foco. */
  private readonly botonMantener = viewChild<ElementRef<HTMLButtonElement>>(ID_BOTON_MANTENER);
  private focoPrevio: HTMLElement | null = null;

  constructor() {
    // Recarga el proyecto cada vez que cambia el `proyectoId` resuelto.
    effect(() => {
      const id = this.resolverId();
      if (id !== null) {
        void this.cargarProyecto(id);
      }
    });
  }

  ngOnInit(): void {
    // Mueve el foco al primer control del formulario para usuarios que
    // navegan con teclado. Se ejecuta tras la primera renderización.
    queueMicrotask(() => this.enfocarPrimerControl());
  }

  // ---------------------------------------------------------------------------
  // Acciones principales
  // ---------------------------------------------------------------------------

  protected async confirmarCancelacion(): Promise<void> {
    this.submitted.set(true);
    this.problem.set(undefined);
    if (this.formCancelacion.invalid) {
      this.formCancelacion.markAllAsTouched();
      return;
    }
    const proyecto = this.proyecto();
    const etag = this.etagActual();
    if (!proyecto || !etag) {
      this.problem.set({
        type: 'about:blank',
        title: 'Contexto requerido',
        status: 400,
        code: 'CANCELLATION_CONTEXT_REQUIRED',
        detail: 'No se puede confirmar la cancelación sin proyecto ni ETag.',
        correlationId: 'N/A',
        violations: []
      });
      return;
    }

    const comando: ProjectCancellationCommand = {
      destino: 'CANCELADO',
      documentoRefId: this.formCancelacion.controls.documentoRefId.value as number,
      observaciones: this.formCancelacion.controls.observaciones.value.trim()
    };

    this.submitting.set(true);
    this.decision.cancelarProyecto(proyecto.id, comando, { etag }).subscribe({
      next: (detalle) => {
        this.ultimaTransicion.set(detalle);
        this.etagActual.set(detalle.etag);
        this.proyecto.set({ ...proyecto, estado: detalle.estadoNuevo, version: detalle.version });
        this.problem.set(undefined);
        this.submitting.set(false);
        this.submitted.set(false);
        this.mostrarDialogoConfirmacion.set(false);
      },
      error: (error: unknown) => this.manejarError(error)
    });
  }

  // ---------------------------------------------------------------------------
  // Diálogo accesible de cancelación
  // ---------------------------------------------------------------------------

  protected abrirDialogoCancelacion(): void {
    if (this.formCancelacion.pristine) {
      // Sin cambios pendientes, se descarta directamente.
      this.cancelarEdicion();
      return;
    }
    this.focoPrevio = document.activeElement as HTMLElement | null;
    this.mostrarDialogoConfirmacion.set(true);
    queueMicrotask(() => this.botonMantener()?.nativeElement.focus());
  }

  protected mantenerEdicion(): void {
    this.mostrarDialogoConfirmacion.set(false);
    this.restaurarFocoPrevio();
  }

  protected descartarCambios(): void {
    this.mostrarDialogoConfirmacion.set(false);
    this.formCancelacion.reset();
    this.problem.set(undefined);
    this.restaurarFocoPrevio();
    this.cancelar.emit();
  }

  private restaurarFocoPrevio(): void {
    if (this.focoPrevio) {
      this.focoPrevio.focus();
      this.focoPrevio = null;
    }
  }

  protected alPulsarEscape(): void {
    if (this.mostrarDialogoConfirmacion()) {
      this.mantenerEdicion();
      return;
    }
    if (this.formCancelacion.dirty) {
      this.abrirDialogoCancelacion();
    }
  }

  protected cancelarEdicion(): void {
    this.abrirDialogoCancelacion();
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

  protected mensajeError(campo: keyof CancellationFormGroup): string | null {
    const control = this.formCancelacion.controls[campo];
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

  private async cargarProyecto(id: number, opciones: { silencioso?: boolean } = {}): Promise<void> {
    try {
      const respuesta = await firstValueFrom(this.decision.consultarIniciativa(id));
      this.proyecto.set(respuesta);
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
    const desdeInput = this.proyectoId();
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

  private buildForm(): FormGroup<CancellationFormGroup> {
    return this.fb.group<CancellationFormGroup>({
      documentoRefId: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(1)]
      }),
      observaciones: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(MAX_OBSERVACIONES)]
      })
    });
  }
}
