// Página de administración de asignaciones funcionales (US6 - T092).
//
// Recorrido del administrador: da de alta, cambia la vigencia y revoca
// asignaciones de una persona a una combinación función-perfil-unidad
// vigente en la matriz. La página NO decide perfiles ni unidades, NO
// extiende vigencia fuera de la combinación matricial y NO revoca por
// su cuenta: el backend es la autoridad efectiva y aplica la protección
// del último `GlobalAdmin`. Las respuestas se conservan con ETag para
// propagar `If-Match` en el cambio de vigencia; la revocación exige un
// motivo obligatorio para auditoría.
//
// Accesibilidad WCAG 2.1 AA:
//   * Foco visible y navegación por teclado en cada control.
//   * Labels asociados (`for`/`id`) y mensajes de error con
//     `aria-describedby`.
//   * Regiones `aria-live="polite"` para resultados y
//     `aria-live="assertive"` para errores de servidor.
//   * Contraste mínimo 4.5:1 delegado al tema PIIP (`piip-theme.scss`).
//   * Atajo `Escape` revierte los cambios pendientes sin enviar al
//     backend.

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { firstValueFrom } from 'rxjs';

import { parseProblemDetails, ProblemDetails, ProblemViolation } from '../../../core/http/problem-details';
import { EffectiveAssignmentSelectorComponent } from '../../../core/effective-assignment/effective-assignment-selector.component';
import { EffectiveAssignmentService } from '../../../core/effective-assignment/effective-assignment.service';
import { SeguridadApiService } from '../api/seguridad-api.service';
import { AssignmentChangeRequest, AssignmentDetail, AssignmentRequest, RevocationRequest } from '../api/types';

interface AltaFormGroup {
  usuarioId: FormControl<number | null>;
  matrizCombinacionId: FormControl<number | null>;
  fechaInicio: FormControl<string>;
  fechaFin: FormControl<string>;
  documentoFormalVersionId: FormControl<number | null>;
}

interface CambioFormGroup {
  idAsignacion: FormControl<number | null>;
  fechaInicio: FormControl<string>;
  fechaFin: FormControl<string>;
}

interface RevocacionFormGroup {
  idAsignacion: FormControl<number | null>;
  motivo: FormControl<string>;
  documentoFormalVersionId: FormControl<number | null>;
}

const MAX_MOTIVO = 2000;
const ID_FORM_ALTA = 'assignment-alta-form';
const ID_FORM_CAMBIO = 'assignment-cambio-form';
const ID_FORM_REVOCACION = 'assignment-revocacion-form';
const ID_ALTA_USUARIO = 'assignment-alta-usuario';
const ID_ALTA_COMBINACION = 'assignment-alta-combinacion';
const ID_ALTA_FECHA_INICIO = 'assignment-alta-fecha-inicio';
const ID_ALTA_FECHA_FIN = 'assignment-alta-fecha-fin';
const ID_ALTA_DOCUMENTO = 'assignment-alta-documento';
const ID_CAMBIO_ID = 'assignment-cambio-id';
const ID_CAMBIO_FECHA_INICIO = 'assignment-cambio-fecha-inicio';
const ID_CAMBIO_FECHA_FIN = 'assignment-cambio-fecha-fin';
const ID_REVOCACION_ID = 'assignment-revocacion-id';
const ID_REVOCACION_MOTIVO = 'assignment-revocacion-motivo';
const ID_REVOCACION_DOCUMENTO = 'assignment-revocacion-documento';

@Component({
  selector: 'app-assignment-administration',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    EffectiveAssignmentSelectorComponent
  ],
  templateUrl: './assignment-administration.component.html',
  styleUrl: './assignment-administration.component.scss'
})
export class AssignmentAdministrationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(SeguridadApiService);
  private readonly assignments = inject(EffectiveAssignmentService);

  protected readonly formAltaId = ID_FORM_ALTA;
  protected readonly formCambioId = ID_FORM_CAMBIO;
  protected readonly formRevocacionId = ID_FORM_REVOCACION;
  protected readonly idAltaUsuario = ID_ALTA_USUARIO;
  protected readonly idAltaCombinacion = ID_ALTA_COMBINACION;
  protected readonly idAltaFechaInicio = ID_ALTA_FECHA_INICIO;
  protected readonly idAltaFechaFin = ID_ALTA_FECHA_FIN;
  protected readonly idAltaDocumento = ID_ALTA_DOCUMENTO;
  protected readonly idCambioId = ID_CAMBIO_ID;
  protected readonly idCambioFechaInicio = ID_CAMBIO_FECHA_INICIO;
  protected readonly idCambioFechaFin = ID_CAMBIO_FECHA_FIN;
  protected readonly idRevocacionId = ID_REVOCACION_ID;
  protected readonly idRevocacionMotivo = ID_REVOCACION_MOTIVO;
  protected readonly idRevocacionDocumento = ID_REVOCACION_DOCUMENTO;

  protected readonly formAlta: FormGroup<AltaFormGroup> = this.buildAlta();
  protected readonly formCambio: FormGroup<CambioFormGroup> = this.buildCambio();
  protected readonly formRevocacion: FormGroup<RevocacionFormGroup> = this.buildRevocacion();

  protected readonly submittedAlta = signal(false);
  protected readonly submittedCambio = signal(false);
  protected readonly submittedRevocacion = signal(false);
  protected readonly submittingAlta = signal(false);
  protected readonly submittingCambio = signal(false);
  protected readonly submittingRevocacion = signal(false);

  protected readonly problema = signal<ProblemDetails | undefined>(undefined);
  protected readonly asignacionActual = signal<AssignmentDetail | undefined>(undefined);
  protected readonly etagAsignacion = signal<string | undefined>(undefined);

  ngOnInit(): void {
    // La ETag devuelta por una respuesta se conserva para el siguiente cambio
    // de vigencia; el servicio la traduce en `If-Match` mediante
    // `withEntityTag` y `entityTagInterceptor`.
  }

  // ---------------------------------------------------------------------------
  // Alta
  // ---------------------------------------------------------------------------

  protected async crearAsignacion(): Promise<void> {
    this.submittedAlta.set(true);
    this.problema.set(undefined);
    if (this.formAlta.invalid) {
      this.formAlta.markAllAsTouched();
      return;
    }
    const raw = this.formAlta.getRawValue();
    if (raw.usuarioId === null || raw.matrizCombinacionId === null) {
      this.formAlta.markAllAsTouched();
      return;
    }
    const payload: AssignmentRequest = {
      usuarioId: raw.usuarioId,
      matrizCombinacionId: raw.matrizCombinacionId,
      fechaInicio: raw.fechaInicio,
      fechaFin: this.normalizeOptionalDate(raw.fechaFin),
      documentoFormalVersionId: this.normalizeOptionalId(raw.documentoFormalVersionId)
    };
    this.submittingAlta.set(true);
    try {
      const detalle = await firstValueFrom(this.api.crearAsignacion(payload));
      this.asignacionActual.set(detalle);
      this.etagAsignacion.set(detalle.etag);
      this.problema.set(undefined);
      this.formAlta.reset({ usuarioId: null, matrizCombinacionId: null, fechaInicio: '', fechaFin: '', documentoFormalVersionId: null });
      this.submittedAlta.set(false);
    } catch (error: unknown) {
      this.manejarError(error);
    } finally {
      this.submittingAlta.set(false);
    }
  }

  protected cancelarAlta(): void {
    this.formAlta.reset({ usuarioId: null, matrizCombinacionId: null, fechaInicio: '', fechaFin: '', documentoFormalVersionId: null });
    this.problema.set(undefined);
    this.submittedAlta.set(false);
  }

  // ---------------------------------------------------------------------------
  // Cambio de vigencia
  // ---------------------------------------------------------------------------

  protected async cambiarAsignacion(): Promise<void> {
    this.submittedCambio.set(true);
    this.problema.set(undefined);
    if (this.formCambio.invalid) {
      this.formCambio.markAllAsTouched();
      return;
    }
    const raw = this.formCambio.getRawValue();
    if (raw.idAsignacion === null) {
      this.formCambio.controls.idAsignacion.markAsTouched();
      return;
    }
    const payload: AssignmentChangeRequest = {
      fechaInicio: raw.fechaInicio,
      fechaFin: this.normalizeOptionalDate(raw.fechaFin)
    };
    this.submittingCambio.set(true);
    try {
      const detalle = await firstValueFrom(
        this.api.cambiarAsignacion(raw.idAsignacion, payload, { etag: this.etagAsignacion() })
      );
      this.asignacionActual.set(detalle);
      this.etagAsignacion.set(detalle.etag);
      this.problema.set(undefined);
      this.submittedCambio.set(false);
    } catch (error: unknown) {
      this.manejarError(error);
    } finally {
      this.submittingCambio.set(false);
    }
  }

  protected cancelarCambio(): void {
    this.formCambio.reset({ idAsignacion: null, fechaInicio: '', fechaFin: '' });
    this.problema.set(undefined);
    this.submittedCambio.set(false);
  }

  // ---------------------------------------------------------------------------
  // Revocación inmediata
  // ---------------------------------------------------------------------------

  protected async revocarAsignacion(): Promise<void> {
    this.submittedRevocacion.set(true);
    this.problema.set(undefined);
    if (this.formRevocacion.invalid) {
      this.formRevocacion.markAllAsTouched();
      return;
    }
    const raw = this.formRevocacion.getRawValue();
    if (raw.idAsignacion === null) {
      this.formRevocacion.controls.idAsignacion.markAsTouched();
      return;
    }
    const payload: RevocationRequest = {
      motivo: raw.motivo.trim(),
      documentoFormalVersionId: this.normalizeOptionalId(raw.documentoFormalVersionId)
    };
    this.submittingRevocacion.set(true);
    try {
      const detalle = await firstValueFrom(this.api.revocarAsignacion(raw.idAsignacion, payload));
      this.asignacionActual.set(detalle);
      this.etagAsignacion.set(detalle.etag);
      this.problema.set(undefined);
      this.formRevocacion.reset({ idAsignacion: null, motivo: '', documentoFormalVersionId: null });
      this.submittedRevocacion.set(false);
      // La revocación inmediata confirmada por el backend invalida la
      // asignación efectiva local si coincide con la afectada (T093). La
      // autorización efectiva la revalida el servidor en cada llamada.
      this.assignments.invalidateAfterRevocation(detalle.id ?? raw.idAsignacion);
    } catch (error: unknown) {
      this.manejarError(error);
    } finally {
      this.submittingRevocacion.set(false);
    }
  }

  protected cancelarRevocacion(): void {
    this.formRevocacion.reset({ idAsignacion: null, motivo: '', documentoFormalVersionId: null });
    this.problema.set(undefined);
    this.submittedRevocacion.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers accesibles
  // ---------------------------------------------------------------------------

  protected mensajeError(
    control: AbstractControl<unknown, unknown> | null,
    submitted: boolean
  ): string | null {
    if (!control || !(control.touched || submitted)) {
      return null;
    }
    const errors: ValidationErrors | null = control.errors;
    if (!errors) {
      return null;
    }
    if (errors['required']) {
      return 'Este campo es obligatorio.';
    }
    if (errors['maxlength']) {
      const requerido = errors['maxlength'] as { requiredLength: number };
      return `Máximo ${requerido.requiredLength} caracteres.`;
    }
    if (errors['min']) {
      const requerido = errors['min'] as { min: number };
      return `Debe ser mayor o igual a ${requerido.min}.`;
    }
    return null;
  }

  protected violacionesPara(campo: string): readonly ProblemViolation[] {
    const problema = this.problema();
    if (!problema) {
      return [];
    }
    return problema.violations.filter((violation: ProblemViolation) => violation.field === campo);
  }

  // ---------------------------------------------------------------------------
  // Internos
  // ---------------------------------------------------------------------------

  private buildAlta(): FormGroup<AltaFormGroup> {
    return this.fb.group<AltaFormGroup>({
      usuarioId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      matrizCombinacionId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      fechaInicio: this.fb.nonNullable.control('', { validators: [Validators.required] }),
      fechaFin: this.fb.nonNullable.control(''),
      documentoFormalVersionId: this.fb.control<number | null>(null, { validators: [Validators.min(1)] })
    });
  }

  private buildCambio(): FormGroup<CambioFormGroup> {
    return this.fb.group<CambioFormGroup>({
      idAsignacion: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      fechaInicio: this.fb.nonNullable.control('', { validators: [Validators.required] }),
      fechaFin: this.fb.nonNullable.control('')
    });
  }

  private buildRevocacion(): FormGroup<RevocacionFormGroup> {
    return this.fb.group<RevocacionFormGroup>({
      idAsignacion: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      motivo: this.fb.nonNullable.control('', { validators: [Validators.required, Validators.maxLength(MAX_MOTIVO)] }),
      documentoFormalVersionId: this.fb.control<number | null>(null, { validators: [Validators.min(1)] })
    });
  }

  private normalizeOptionalDate(value: string): string | undefined {
    const trimmed = value?.trim();
    return trimmed ? trimmed : undefined;
  }

  private normalizeOptionalId(value: number | null): number | undefined {
    return typeof value === 'number' && Number.isFinite(value) ? value : undefined;
  }

  private manejarError(error: unknown): void {
    this.problema.set(parseProblemDetails(error));
    this.submittingAlta.set(false);
    this.submittingCambio.set(false);
    this.submittingRevocacion.set(false);
  }
}
