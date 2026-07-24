// Página de administración de usuarios institucionales (US6 - T092).
//
// Recorrido del administrador: aprovisiona usuarios institucionales
// consultando y reintentando operaciones recuperables, desactivando o
// reactivando identidades. La página NO recopila, procesa ni almacena
// contraseñas, tokens ni atributos sensibles: el flujo de identidad
// pertenece exclusivamente a Keycloak. Cuando el backend Keycloak tiene
// éxito pero Oracle falla, la operación queda en estado recuperable y
// esta página expone el botón de reintento con la `Idempotency-Key` y
// `Authorization` aplicadas por los interceptores globales.
//
// El componente NO decide activación, NO calcula vigencia, NO altera
// asignaciones revocadas ni vencidas: el backend es la autoridad efectiva.
// Las respuestas `application/problem+json` se traducen con
// `parseProblemDetails` para preservar los códigos canónicos
// (`KEYCLOAK_OPERATION_RECOVERABLE`, `KEYCLOAK_CREATION_FAILED`,
// `PROVISIONING_USER_ALREADY_DISABLED`, etc.).
//
// Accesibilidad WCAG 2.1 AA:
//   * Foco visible y navegación por teclado en cada control.
//   * Labels asociados (`for`/`id`) y mensajes de error con
//     `aria-describedby`.
//   * Regiones `aria-live="polite"` para resultados y
//     `aria-live="assertive"` para errores de servidor.
//   * Anuncios accesibles al completar o reintentar operaciones.
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
import { MatRadioModule } from '@angular/material/radio';
import { firstValueFrom } from 'rxjs';

import { parseProblemDetails, ProblemDetails, ProblemViolation } from '../../../core/http/problem-details';
import { EffectiveAssignmentSelectorComponent } from '../../../core/effective-assignment/effective-assignment-selector.component';
import { SeguridadApiService } from '../api/seguridad-api.service';
import { CreateUserRequest, ProvisioningResult, UserStatusRequest, UserStatusResult } from '../api/types';

interface AprovisionamientoFormGroup {
  correoInstitucional: FormControl<string>;
  nombreCompleto: FormControl<string>;
  unidadId: FormControl<number>;
}

interface EstadoFormGroup {
  idUsuario: FormControl<number | null>;
  motivo: FormControl<string>;
}

const MAX_CORREO = 200;
const MAX_NOMBRE = 300;
const MAX_MOTIVO = 1000;

const ID_FORM_PROVISION = 'user-provisioning-form';
const ID_FORM_ESTADO = 'user-status-form';
const ID_CORREO = 'user-provisioning-correo';
const ID_NOMBRE = 'user-provisioning-nombre';
const ID_UNIDAD = 'user-provisioning-unidad';
const ID_ID_ESTADO = 'user-status-id';
const ID_MOTIVO = 'user-status-motivo';

@Component({
  selector: 'app-user-administration',
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
    EffectiveAssignmentSelectorComponent
  ],
  templateUrl: './user-administration.component.html',
  styleUrl: './user-administration.component.scss'
})
export class UserAdministrationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(SeguridadApiService);

  protected readonly formId = ID_FORM_PROVISION;
  protected readonly formEstadoId = ID_FORM_ESTADO;
  protected readonly idCorreo = ID_CORREO;
  protected readonly idNombre = ID_NOMBRE;
  protected readonly idUnidad = ID_UNIDAD;
  protected readonly idIdEstado = ID_ID_ESTADO;
  protected readonly idMotivo = ID_MOTIVO;

  protected readonly formAprovisionamiento: FormGroup<AprovisionamientoFormGroup> = this.buildAprovisionamiento();
  protected readonly formEstado: FormGroup<EstadoFormGroup> = this.buildEstado();
  protected readonly modoEstado = signal<'DESACTIVAR' | 'REACTIVAR'>('DESACTIVAR');

  protected readonly submittedAprovision = signal(false);
  protected readonly submittedEstado = signal(false);
  protected readonly submittingAprovision = signal(false);
  protected readonly submittingEstado = signal(false);
  protected readonly submittingConsulta = signal(false);
  protected readonly submittingReintento = signal(false);

  protected readonly problema = signal<ProblemDetails | undefined>(undefined);
  protected readonly problemaConsulta = signal<ProblemDetails | undefined>(undefined);
  protected readonly operacionActual = signal<ProvisioningResult | undefined>(undefined);
  protected readonly estadoActual = signal<UserStatusResult | undefined>(undefined);

  ngOnInit(): void {
    this.ajustarValidadoresModo();
    this.formEstado.controls.motivo.valueChanges.subscribe(() => this.ajustarValidadoresModo());
  }

  // ---------------------------------------------------------------------------
  // Acciones: aprovisionamiento
  // ---------------------------------------------------------------------------

  protected async aprovisionarUsuario(): Promise<void> {
    this.submittedAprovision.set(true);
    this.problema.set(undefined);
    if (this.formAprovisionamiento.invalid) {
      this.formAprovisionamiento.markAllAsTouched();
      return;
    }
    const payload = this.formAprovisionamiento.getRawValue() as CreateUserRequest;
    this.submittingAprovision.set(true);
    try {
      const operacion = await firstValueFrom(this.api.aprovisionarUsuario(payload));
      this.operacionActual.set(operacion);
      this.estadoActual.set(undefined);
      this.problema.set(undefined);
      this.formAprovisionamiento.reset({ correoInstitucional: '', nombreCompleto: '', unidadId: 0 });
      this.submittedAprovision.set(false);
    } catch (error: unknown) {
      this.manejarError(error);
    } finally {
      this.submittingAprovision.set(false);
    }
  }

  protected async consultarOperacion(): Promise<void> {
    const operacion = this.operacionActual();
    if (!operacion) {
      return;
    }
    this.submittingConsulta.set(true);
    this.problemaConsulta.set(undefined);
    try {
      const detalle = await firstValueFrom(this.api.consultarOperacionAprovisionamiento(operacion.operacionId));
      this.operacionActual.set(detalle);
    } catch (error: unknown) {
      this.problemaConsulta.set(parseProblemDetails(error));
    } finally {
      this.submittingConsulta.set(false);
    }
  }

  protected async reintentarOperacion(): Promise<void> {
    const operacion = this.operacionActual();
    if (!operacion) {
      return;
    }
    this.submittingReintento.set(true);
    this.problema.set(undefined);
    try {
      const detalle = await firstValueFrom(this.api.reintentarAprovisionamiento(operacion.operacionId));
      this.operacionActual.set(detalle);
    } catch (error: unknown) {
      this.manejarError(error);
    } finally {
      this.submittingReintento.set(false);
    }
  }

  protected cancelarAprovisionamiento(): void {
    this.formAprovisionamiento.reset({ correoInstitucional: '', nombreCompleto: '', unidadId: 0 });
    this.problema.set(undefined);
    this.submittedAprovision.set(false);
  }

  // ---------------------------------------------------------------------------
  // Acciones: desactivación / reactivación
  // ---------------------------------------------------------------------------

  protected seleccionarModo(modo: 'DESACTIVAR' | 'REACTIVAR'): void {
    this.modoEstado.set(modo);
    this.ajustarValidadoresModo();
  }

  protected async aplicarCambioEstado(): Promise<void> {
    this.submittedEstado.set(true);
    this.problema.set(undefined);
    if (this.formEstado.invalid) {
      this.formEstado.markAllAsTouched();
      return;
    }
    const { idUsuario, motivo } = this.formEstado.getRawValue();
    if (idUsuario === null) {
      this.formEstado.controls.idUsuario.markAsTouched();
      return;
    }
    const payload: UserStatusRequest = { motivo: motivo.trim() };
    this.submittingEstado.set(true);
    try {
      const llamada =
        this.modoEstado() === 'DESACTIVAR'
          ? this.api.desactivarUsuario(idUsuario, payload)
          : this.api.reactivarUsuario(idUsuario, payload);
      const resultado = await firstValueFrom(llamada);
      this.estadoActual.set(resultado);
      this.problema.set(undefined);
      this.formEstado.reset({ idUsuario: null, motivo: '' });
      this.submittedEstado.set(false);
    } catch (error: unknown) {
      this.manejarError(error);
    } finally {
      this.submittingEstado.set(false);
    }
  }

  protected cancelarCambioEstado(): void {
    this.formEstado.reset({ idUsuario: null, motivo: '' });
    this.problema.set(undefined);
    this.submittedEstado.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers accesibles para plantillas
  // ---------------------------------------------------------------------------

  protected violacionesPara(campo: string, form: FormGroup<Record<string, AbstractControl>>): readonly ProblemViolation[] {
    const problema = this.problema();
    if (!problema) {
      return [];
    }
    return problema.violations.filter((violation: ProblemViolation) => violation.field === campo);
  }

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
    if (errors['email']) {
      return 'Debe ser un correo institucional válido.';
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

  protected estadoOperacion(): string {
    return this.operacionActual()?.estado ?? '';
  }

  protected operacionRecuperable(): boolean {
    return this.operacionActual()?.recuperable === true;
  }

  protected intentoActual(): number {
    return this.operacionActual()?.intento ?? 0;
  }

  // ---------------------------------------------------------------------------
  // Internos
  // ---------------------------------------------------------------------------

  private buildAprovisionamiento(): FormGroup<AprovisionamientoFormGroup> {
    return this.fb.group<AprovisionamientoFormGroup>({
      correoInstitucional: this.fb.nonNullable.control('', {
        validators: [Validators.required, Validators.email, Validators.maxLength(MAX_CORREO)]
      }),
      nombreCompleto: this.fb.nonNullable.control('', {
        validators: [Validators.required, Validators.maxLength(MAX_NOMBRE)]
      }),
      unidadId: this.fb.nonNullable.control<number>(0, {
        validators: [Validators.required, Validators.min(1)]
      })
    });
  }

  private buildEstado(): FormGroup<EstadoFormGroup> {
    return this.fb.group<EstadoFormGroup>({
      idUsuario: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(1)]
      }),
      motivo: this.fb.nonNullable.control('', {
        validators: [Validators.required, Validators.maxLength(MAX_MOTIVO)]
      })
    });
  }

  private ajustarValidadoresModo(): void {
    const motivo = this.formEstado.controls.motivo;
    motivo.setValidators([Validators.required, Validators.maxLength(MAX_MOTIVO)]);
    motivo.updateValueAndValidity({ emitEvent: false });
  }

  private manejarError(error: unknown): void {
    this.problema.set(parseProblemDetails(error));
    this.submittingAprovision.set(false);
    this.submittingEstado.set(false);
  }
}
