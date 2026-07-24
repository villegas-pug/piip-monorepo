// Página de administración de suplencias funcionales (US6 - T092).
//
// Recorrido del administrador: crea suplencias temporales sin solape
// sobre una asignación titular y termina anticipadamente suplencias
// vigentes por la misma autoridad que las autorizó. La página NO crea
// solapes, NO transfiere credenciales y NO entrega la contraseña del
// titular: el flujo de identidad pertenece exclusivamente a Keycloak y
// la suplencia es solo una delegación funcional registrada en Oracle.
//
// El componente NO valida fechas, NO calcula periodos, NO delega
// autorizaciones: el backend es la autoridad efectiva. Las respuestas
// `application/problem+json` se traducen con `parseProblemDetails` para
// preservar códigos canónicos (`SUBSTITUTION_OVERLAP`,
// `SUBSTITUTION_AUTHORITY_DIFFERENT`, `SUBSTITUTION_DOCUMENT_REQUIRED`,
// etc.).
//
// Discrepancia documentada (`NEEDS CLARIFICATION`): las rutas
// `POST /api/v1/seguridad/asignaciones/{titularId}/suplencias` y
// `POST /api/v1/seguridad/suplencias/{id}/terminaciones` están
// implementadas en `SuplenciaController` (T089) pero todavía no
// aparecen en el snapshot OpenAPI codigo-first (`piip-api.yaml`).
// Se conservan las rutas del backend para no divergir funcionalmente.
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
import { EarlyTerminationRequest, SubstitutionDetail, SubstitutionRequest } from '../api/types';

interface CrearSuplenciaFormGroup {
  titularAsignacionId: FormControl<number | null>;
  suplenteUsuarioId: FormControl<number | null>;
  inicio: FormControl<string>;
  fin: FormControl<string>;
  documentoFormalVersionId: FormControl<number | null>;
}

interface TerminarSuplenciaFormGroup {
  suplenciaId: FormControl<number | null>;
  motivo: FormControl<string>;
  documentoFormalVersionId: FormControl<number | null>;
}

const MAX_MOTIVO = 2000;
const ID_FORM_CREAR = 'substitution-create-form';
const ID_FORM_TERMINAR = 'substitution-terminate-form';
const ID_CREAR_TITULAR = 'substitution-create-titular';
const ID_CREAR_SUPLENTE = 'substitution-create-suplente';
const ID_CREAR_INICIO = 'substitution-create-inicio';
const ID_CREAR_FIN = 'substitution-create-fin';
const ID_CREAR_DOCUMENTO = 'substitution-create-documento';
const ID_TERMINAR_ID = 'substitution-terminate-id';
const ID_TERMINAR_MOTIVO = 'substitution-terminate-motivo';
const ID_TERMINAR_DOCUMENTO = 'substitution-terminate-documento';

@Component({
  selector: 'app-substitution-administration',
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
  templateUrl: './substitution-administration.component.html',
  styleUrl: './substitution-administration.component.scss'
})
export class SubstitutionAdministrationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(SeguridadApiService);
  private readonly assignments = inject(EffectiveAssignmentService);

  protected readonly formCrearId = ID_FORM_CREAR;
  protected readonly formTerminarId = ID_FORM_TERMINAR;
  protected readonly idCrearTitular = ID_CREAR_TITULAR;
  protected readonly idCrearSuplente = ID_CREAR_SUPLENTE;
  protected readonly idCrearInicio = ID_CREAR_INICIO;
  protected readonly idCrearFin = ID_CREAR_FIN;
  protected readonly idCrearDocumento = ID_CREAR_DOCUMENTO;
  protected readonly idTerminarId = ID_TERMINAR_ID;
  protected readonly idTerminarMotivo = ID_TERMINAR_MOTIVO;
  protected readonly idTerminarDocumento = ID_TERMINAR_DOCUMENTO;

  protected readonly formCrear: FormGroup<CrearSuplenciaFormGroup> = this.buildCrear();
  protected readonly formTerminar: FormGroup<TerminarSuplenciaFormGroup> = this.buildTerminar();

  protected readonly submittedCrear = signal(false);
  protected readonly submittedTerminar = signal(false);
  protected readonly submittingCrear = signal(false);
  protected readonly submittingTerminar = signal(false);

  protected readonly problema = signal<ProblemDetails | undefined>(undefined);
  protected readonly suplenciaActual = signal<SubstitutionDetail | undefined>(undefined);

  ngOnInit(): void {
    // El selector de asignación efectiva se inyecta en la plantilla.
  }

  // ---------------------------------------------------------------------------
  // Crear suplencia
  // ---------------------------------------------------------------------------

  protected async crearSuplencia(): Promise<void> {
    this.submittedCrear.set(true);
    this.problema.set(undefined);
    if (this.formCrear.invalid) {
      this.formCrear.markAllAsTouched();
      return;
    }
    const raw = this.formCrear.getRawValue();
    if (
      raw.titularAsignacionId === null ||
      raw.suplenteUsuarioId === null ||
      raw.documentoFormalVersionId === null
    ) {
      this.formCrear.markAllAsTouched();
      return;
    }
    const payload: SubstitutionRequest = {
      suplenteUsuarioId: raw.suplenteUsuarioId,
      inicio: raw.inicio,
      fin: raw.fin,
      documentoFormalVersionId: raw.documentoFormalVersionId
    };
    this.submittingCrear.set(true);
    try {
      const detalle = await firstValueFrom(this.api.crearSuplencia(raw.titularAsignacionId, payload));
      this.suplenciaActual.set(detalle);
      this.problema.set(undefined);
      this.submittedCrear.set(false);
      this.formCrear.reset({
        titularAsignacionId: null,
        suplenteUsuarioId: null,
        inicio: '',
        fin: '',
        documentoFormalVersionId: null
      });
    } catch (error: unknown) {
      this.manejarError(error);
    } finally {
      this.submittingCrear.set(false);
    }
  }

  protected cancelarCrear(): void {
    this.formCrear.reset({
      titularAsignacionId: null,
      suplenteUsuarioId: null,
      inicio: '',
      fin: '',
      documentoFormalVersionId: null
    });
    this.problema.set(undefined);
    this.submittedCrear.set(false);
  }

  // ---------------------------------------------------------------------------
  // Terminar suplencia
  // ---------------------------------------------------------------------------

  protected async terminarSuplencia(): Promise<void> {
    this.submittedTerminar.set(true);
    this.problema.set(undefined);
    if (this.formTerminar.invalid) {
      this.formTerminar.markAllAsTouched();
      return;
    }
    const raw = this.formTerminar.getRawValue();
    if (raw.suplenciaId === null) {
      this.formTerminar.controls.suplenciaId.markAsTouched();
      return;
    }
    const payload: EarlyTerminationRequest = {
      motivo: raw.motivo.trim(),
      documentoFormalVersionId: typeof raw.documentoFormalVersionId === 'number' ? raw.documentoFormalVersionId : undefined
    };
    this.submittingTerminar.set(true);
    try {
      const detalle = await firstValueFrom(this.api.terminarSuplenciaAnticipadamente(raw.suplenciaId, payload));
      this.suplenciaActual.set(detalle);
      this.problema.set(undefined);
      this.submittedTerminar.set(false);
      this.formTerminar.reset({ suplenciaId: null, motivo: '', documentoFormalVersionId: null });
      // La terminación anticipada confirmada por el backend invalida la
      // asignación efectiva local si coincide con la asignación titular
      // afectada (T093). La autorización efectiva la revalida el
      // servidor en cada llamada.
      this.assignments.invalidateAfterSubstitution(detalle.asignacionTitularId);
    } catch (error: unknown) {
      this.manejarError(error);
    } finally {
      this.submittingTerminar.set(false);
    }
  }

  protected cancelarTerminar(): void {
    this.formTerminar.reset({ suplenciaId: null, motivo: '', documentoFormalVersionId: null });
    this.problema.set(undefined);
    this.submittedTerminar.set(false);
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

  private buildCrear(): FormGroup<CrearSuplenciaFormGroup> {
    return this.fb.group<CrearSuplenciaFormGroup>({
      titularAsignacionId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      suplenteUsuarioId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      inicio: this.fb.nonNullable.control('', { validators: [Validators.required] }),
      fin: this.fb.nonNullable.control('', { validators: [Validators.required] }),
      documentoFormalVersionId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] })
    });
  }

  private buildTerminar(): FormGroup<TerminarSuplenciaFormGroup> {
    return this.fb.group<TerminarSuplenciaFormGroup>({
      suplenciaId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      motivo: this.fb.nonNullable.control('', { validators: [Validators.required, Validators.maxLength(MAX_MOTIVO)] }),
      documentoFormalVersionId: this.fb.control<number | null>(null, { validators: [Validators.min(1)] })
    });
  }

  private manejarError(error: unknown): void {
    this.problema.set(parseProblemDetails(error));
    this.submittingCrear.set(false);
    this.submittingTerminar.set(false);
  }
}
