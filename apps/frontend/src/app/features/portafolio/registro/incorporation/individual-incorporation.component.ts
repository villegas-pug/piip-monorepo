// Componente de incorporación individual de información existente.
//
// Referencia contractual: specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md
//   (sección "Incorporación individual") y snapshot OpenAPI codigo-first PIIP.
//
// El cliente no transiciona estados, no resuelve conflictos por su cuenta: cuando la API
// responde 409 con código `INCORPORATION_CONFLICT_UNRESOLVED`, el componente presenta
// una UI explícita para que el Evaluador documente la resolución. Esa resolución vuelve
// al backend mediante `POST /portafolio/incorporaciones/{id}/conflictos/{conflictoId}/resoluciones`.
//
// Accesibilidad WCAG 2.1 AA: foco visible, navegación por teclado, labels asociados,
// mensajes de error accesibles, contraste mínimo 4.5:1 delegado al tema PIIP y
// regiones `aria-live` para errores y resultados.

import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { parseProblemDetails, ProblemDetails, ProblemViolation } from '../../../../core/http/problem-details';
import { CreateIncorporacionRequest, IncorporacionDetail, IncorporacionResolucionConflictoRequest } from '../api/types/incorporacion.types';
import { TipoConflicto } from '../api/types/common.types';
import { RegistroApiService } from '../api/registro-api.service';

interface IncorporationFormGroup {
  fuente: FormControl<string>;
  fechaFuente: FormControl<string>;
  responsableId: FormControl<number | null>;
  documentoFuenteId: FormControl<number | null>;
  hashOriginal: FormControl<string>;
  datosOriginales: FormControl<string>;
  codigoHeredado: FormControl<string>;
}

interface ConflictResolutionFormGroup {
  conflictoId: FormControl<number | null>;
  resolucion: FormControl<string>;
  documentoResolucionId: FormControl<number | null>;
}

const MAX_FUENTE = 200;
const MAX_HASH = 64;
const MAX_CODIGO_HEREDADO = 50;
const MAX_RESOLUCION = 2000;
const CONFLICT_CODES = new Set<string>(['INCORPORATION_CONFLICT_UNRESOLVED']);

@Component({
  selector: 'app-individual-incorporation',
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
  templateUrl: './individual-incorporation.component.html',
  styleUrl: './individual-incorporation.component.scss'
})
export class IndividualIncorporationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly registro = inject(RegistroApiService);

  protected readonly form: FormGroup<IncorporationFormGroup> = this.buildForm();
  protected readonly resolutionForm: FormGroup<ConflictResolutionFormGroup> = this.buildResolutionForm();

  protected readonly tipoConflictoOpciones: readonly TipoConflicto[] = ['CODIGO', 'DUPLICADO', 'RELACION_INVALIDA'];

  protected readonly submitted = signal(false);
  protected readonly submitting = signal(false);
  protected readonly problem = signal<ProblemDetails | undefined>(undefined);
  protected readonly incorporation = signal<IncorporacionDetail | undefined>(undefined);
  protected readonly conflictoDetectado = signal<ProblemDetails | undefined>(undefined);
  protected readonly tipoConflicto = signal<TipoConflicto | undefined>(undefined);

  ngOnInit(): void {
    this.resolutionForm.controls.conflictoId.valueChanges.subscribe(() => {
      this.resolutionForm.controls.conflictoId.updateValueAndValidity({ emitEvent: false });
    });
  }

  protected enviar(): void {
    this.submitted.set(true);
    this.problem.set(undefined);
    this.conflictoDetectado.set(undefined);
    this.tipoConflicto.set(undefined);
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const payload: CreateIncorporacionRequest = {
      fuente: this.form.controls.fuente.value.trim(),
      fechaFuente: this.form.controls.fechaFuente.value,
      responsableId: this.form.controls.responsableId.value as number,
      documentoFuenteId: this.form.controls.documentoFuenteId.value as number,
      hashOriginal: this.form.controls.hashOriginal.value.trim(),
      datosOriginales: this.form.controls.datosOriginales.value.trim() || undefined,
      codigoHeredado: this.form.controls.codigoHeredado.value.trim() || undefined
    };

    this.submitting.set(true);
    this.registro.registrarIncorporacion(payload).subscribe({
      next: (detalle) => {
        this.incorporation.set(detalle);
        this.submitting.set(false);
        this.form.reset();
        this.submitted.set(false);
      },
      error: (error: unknown) => this.manejarError(error)
    });
  }

  protected enviarResolucion(): void {
    this.resolutionForm.controls.conflictoId.markAsTouched();
    this.resolutionForm.controls.resolucion.markAsTouched();
    if (this.resolutionForm.invalid) {
      return;
    }
    const incorporacion = this.incorporation();
    if (!incorporacion) {
      this.problem.set({
        type: 'about:blank',
        title: 'Contexto requerido',
        status: 400,
        code: 'INCORPORATION_CONTEXT_REQUIRED',
        detail: 'No se puede resolver un conflicto sin una incorporación registrada.',
        correlationId: 'N/A',
        violations: []
      });
      return;
    }

    const payload: IncorporacionResolucionConflictoRequest = {
      conflictoId: this.resolutionForm.controls.conflictoId.value as number,
      incorporacionId: incorporacion.id,
      resolucion: this.resolutionForm.controls.resolucion.value.trim(),
      documentoResolucionId: this.resolutionForm.controls.documentoResolucionId.value ?? undefined
    };

    this.submitting.set(true);
    this.registro.resolverConflictoIncorporacion(incorporacion.id, payload).subscribe({
      next: (detalle) => {
        this.incorporation.set(detalle);
        this.problem.set(undefined);
        this.conflictoDetectado.set(undefined);
        this.resolutionForm.reset();
        this.submitting.set(false);
      },
      error: (error: unknown) => {
        this.problem.set(parseProblemDetails(error));
        this.submitting.set(false);
      }
    });
  }

  protected reintentar(): void {
    this.conflictoDetectado.set(undefined);
    this.problem.set(undefined);
    this.tipoConflicto.set(undefined);
    this.enviar();
  }

  protected mensajeError(campo: keyof IncorporationFormGroup): string | null {
    const control = this.form.controls[campo];
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
    if (control.hasError('pattern')) {
      return 'Formato no permitido.';
    }
    return null;
  }

  protected mensajeErrorResolucion(campo: keyof ConflictResolutionFormGroup): string | null {
    const control = this.resolutionForm.controls[campo];
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
    if (control.hasError('min')) {
      const requerido = control.getError('min') as { min: number };
      return `Debe ser mayor o igual a ${requerido.min}.`;
    }
    return null;
  }

  protected violacionesPara(campo: string): readonly ProblemViolation[] {
    const problem = this.conflictoDetectado() ?? this.problem();
    if (!problem) {
      return [];
    }
    return problem.violations.filter((violation) => violation.field === campo);
  }

  private buildForm(): FormGroup<IncorporationFormGroup> {
    return this.fb.group<IncorporationFormGroup>({
      fuente: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(MAX_FUENTE)]
      }),
      fechaFuente: this.fb.control<string>('', { nonNullable: true, validators: [Validators.required] }),
      responsableId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      documentoFuenteId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      hashOriginal: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(MAX_HASH), Validators.pattern(/^[a-fA-F0-9]*$/)]
      }),
      datosOriginales: this.fb.control<string>('', { nonNullable: true }),
      codigoHeredado: this.fb.control<string>('', { nonNullable: true, validators: [Validators.maxLength(MAX_CODIGO_HEREDADO)] })
    });
  }

  private buildResolutionForm(): FormGroup<ConflictResolutionFormGroup> {
    return this.fb.group<ConflictResolutionFormGroup>({
      conflictoId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      resolucion: this.fb.control<string>('', {
        nonNullable: true,
        validators: [Validators.required, Validators.maxLength(MAX_RESOLUCION)]
      }),
      documentoResolucionId: this.fb.control<number | null>(null, { validators: [Validators.min(1)] })
    });
  }

  private manejarError(error: unknown): void {
    const problem = parseProblemDetails(error);
    if (problem && problem.code && CONFLICT_CODES.has(problem.code)) {
      this.conflictoDetectado.set(problem);
      this.tipoConflicto.set(this.inferirTipoConflicto(problem.violations));
      this.problem.set(undefined);
    } else {
      this.problem.set(problem);
    }
    this.submitting.set(false);
  }

  private inferirTipoConflicto(violations: readonly ProblemViolation[]): TipoConflicto | undefined {
    for (const violation of violations) {
      const candidato = (violation.field ?? '').toUpperCase();
      if (candidato.includes('CODIGO')) {
        return 'CODIGO';
      }
      if (candidato.includes('DUPLICADO')) {
        return 'DUPLICADO';
      }
      if (candidato.includes('RELACION')) {
        return 'RELACION_INVALIDA';
      }
    }
    return undefined;
  }
}
