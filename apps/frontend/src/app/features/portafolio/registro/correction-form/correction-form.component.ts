// Formulario de corrección append-only.
//
// Caso A — Iniciativa (subsanación): la matriz 013 permite modificar únicamente los campos
//   oficiales 5 a 12, 22 y 23 durante la única subsanación abierta por el Evaluador.
//   El snapshot OpenAPI codigo-first PIIP aún no expone los endpoints de subsanación de
//   iniciativa (`POST /portafolio/iniciativas/{id}/subsanaciones`,
//   `PATCH /portafolio/iniciativas/{id}/subsanacion`); quedan como `verification_pending`
//   en el handoff y este formulario ya modela la forma esperada.
//
// Caso B — Incorporación individual: el snapshot expone
//   `POST /portafolio/portafolio/incorporaciones/{id}/correcciones`, con `datosNuevos`
//   y `motivo` obligatorios. Cada corrección crea una nueva entrada append-only.
//
// WCAG 2.1 AA: foco visible, labels asociados, mensajes de error accesibles, navegación
// por teclado y roles ARIA correctos. La accesibilidad se delega al tema PIIP para
// el contraste mínimo 4.5:1.

import { ChangeDetectionStrategy, Component, Input, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';

import { parseProblemDetails, ProblemDetails } from '../../../../core/http/problem-details';
import { IncorporacionDetail } from '../api/types/incorporacion.types';
import { InitiativeDetail } from '../api/types/iniciativa.types';
import { RegistroApiService } from '../api/registro-api.service';
import { FuenteOrigen, TipoSolucion } from '../api/types/common.types';

interface CamposEditablesSubsanacion {
  readonly nombre: string;
  readonly tipoSolucion: TipoSolucion | null;
  readonly fuenteOrigen: FuenteOrigen | null;
  readonly detalleFuente: string;
  readonly problemaPublico: string;
  readonly solucionPropuesta: string;
  readonly objetivoPeiId: number | null;
  readonly actividadPoiId: number | null;
  readonly componenteDigital: boolean;
  readonly detalleComponenteDigital: string;
  readonly nota: string;
}

interface CorrectionFormGroup {
  /** Solo subsanación de iniciativa. */
  nombre: FormControl<string>;
  tipoSolucion: FormControl<TipoSolucion | null>;
  fuenteOrigen: FormControl<FuenteOrigen | null>;
  detalleFuente: FormControl<string>;
  problemaPublico: FormControl<string>;
  solucionPropuesta: FormControl<string>;
  objetivoPeiId: FormControl<number | null>;
  actividadPoiId: FormControl<number | null>;
  componenteDigital: FormControl<boolean>;
  detalleComponenteDigital: FormControl<string>;
  nota: FormControl<string>;
  /** Ambos casos. */
  motivo: FormControl<string>;
  /** Solo incorporación. */
  datosNuevos: FormControl<string>;
}

const MAX_MOTIVO = 2000;
const MAX_NOMBRE = 500;
const MAX_DESCRIPCION = 2000;
const MAX_DETALLE_FUENTE = 500;
const MAX_DETALLE_COMPONENTE = 500;
const MAX_NOTA = 1000;

export type CorrectionMode = 'INICIATIVA_SUBSANACION' | 'INCORPORACION';

@Component({
  selector: 'app-correction-form',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatRadioModule
  ],
  templateUrl: './correction-form.component.html',
  styleUrl: './correction-form.component.scss'
})
export class CorrectionFormComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly registro = inject(RegistroApiService);

  @Input({ required: true }) mode!: CorrectionMode;
  @Input() iniciativa?: InitiativeDetail;
  @Input() incorporacion?: IncorporacionDetail;
  /** ETag esperado para `If-Match`. Solo aplica cuando el backend lo exige. */
  @Input() etag?: string;
  /** Callback opcional cuando la corrección se persiste correctamente. */
  @Input() onPersisted?: (resultado: unknown) => void;

  protected readonly form: FormGroup<CorrectionFormGroup> = this.buildForm();

  protected readonly submitted = signal(false);
  protected readonly submitting = signal(false);
  protected readonly problem = signal<ProblemDetails | undefined>(undefined);
  protected readonly fuenteOrigenOpciones: readonly FuenteOrigen[] = [
    'FICHA_INICIATIVA',
    'CONCURSO_INTERNO',
    'INNOVACION_ABIERTA',
    'PROPUESTA_JEFATURA',
    'OTROS'
  ];
  protected readonly tipoSolucionOpciones: readonly TipoSolucion[] = ['POTENCIAL_ADAPTABLE', 'POR_DEFINIR'];

  ngOnInit(): void {
    this.prellenarDesdeContexto();
    this.escucharCambioFuente();
    this.escucharCambioComponenteDigital();
    this.ajugarValidacionPorModo();
  }

  private ajugarValidacionPorModo(): void {
    if (this.mode === 'INCORPORACION') {
      this.form.controls.datosNuevos.addValidators([Validators.required, Validators.maxLength(MAX_MOTIVO)]);
      this.form.controls.datosNuevos.updateValueAndValidity();
    }
  }

  protected enviar(): void {
    this.submitted.set(true);
    this.problem.set(undefined);

    if (this.mode === 'INICIATIVA_SUBSANACION') {
      this.enviarSubsanacionIniciativa();
      return;
    }

    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    if (this.mode === 'INCORPORACION') {
      this.enviarCorreccionIncorporacion();
      return;
    }
    this.enviarSubsanacionIniciativa();
  }

  protected mensajeError(campo: keyof CorrectionFormGroup): string | null {
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
    return null;
  }

  private buildForm(): FormGroup<CorrectionFormGroup> {
    return this.fb.group<CorrectionFormGroup>({
      nombre: this.fb.control<string>('', { nonNullable: true, validators: [Validators.maxLength(MAX_NOMBRE)] }),
      tipoSolucion: this.fb.control<TipoSolucion | null>(null),
      fuenteOrigen: this.fb.control<FuenteOrigen | null>(null),
      detalleFuente: this.fb.control<string>('', { nonNullable: true, validators: [Validators.maxLength(MAX_DETALLE_FUENTE)] }),
      problemaPublico: this.fb.control<string>('', { nonNullable: true, validators: [Validators.maxLength(MAX_DESCRIPCION)] }),
      solucionPropuesta: this.fb.control<string>('', { nonNullable: true, validators: [Validators.maxLength(MAX_DESCRIPCION)] }),
      objetivoPeiId: this.fb.control<number | null>(null, { validators: [Validators.min(1)] }),
      actividadPoiId: this.fb.control<number | null>(null, { validators: [Validators.min(1)] }),
      componenteDigital: this.fb.control<boolean>(false, { nonNullable: true }),
      detalleComponenteDigital: this.fb.control<string>('', { nonNullable: true, validators: [Validators.maxLength(MAX_DETALLE_COMPONENTE)] }),
      nota: this.fb.control<string>('', { nonNullable: true, validators: [Validators.maxLength(MAX_NOTA)] }),
      motivo: this.fb.control<string>('', { nonNullable: true, validators: [Validators.required, Validators.maxLength(MAX_MOTIVO)] }),
      datosNuevos: this.fb.control<string>('', { nonNullable: true, validators: [Validators.maxLength(MAX_MOTIVO)] })
    });
  }

  private prellenarDesdeContexto(): void {
    if (this.mode === 'INICIATIVA_SUBSANACION' && this.iniciativa) {
      const inicial: Partial<CamposEditablesSubsanacion> = {
        nombre: this.iniciativa.nombre ?? '',
        tipoSolucion: this.iniciativa.tipoSolucion ?? null,
        fuenteOrigen: this.iniciativa.fuenteOrigen ?? null,
        detalleFuente: this.iniciativa.detalleFuente ?? '',
        problemaPublico: this.iniciativa.problemaPublico ?? '',
        solucionPropuesta: this.iniciativa.solucionPropuesta ?? '',
        objetivoPeiId: this.iniciativa.objetivoPeiId ?? null,
        actividadPoiId: this.iniciativa.actividadPoiId ?? null,
        componenteDigital: this.iniciativa.componenteDigital ?? false,
        detalleComponenteDigital: this.iniciativa.detalleComponenteDigital ?? '',
        nota: this.iniciativa.nota ?? ''
      };
      this.form.patchValue(inicial);
      return;
    }
    if (this.mode === 'INCORPORACION' && this.incorporacion) {
      this.form.patchValue({ motivo: '', datosNuevos: '' });
    }
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

  private enviarSubsanacionIniciativa(): void {
    // El snapshot codigo-first aún no expone los endpoints de subsanación de iniciativa
    // (`POST /portafolio/iniciativas/{id}/subsanaciones` y
    // `PATCH /portafolio/iniciativas/{id}/subsanacion`). Esta función queda lista para
    // enchufarse cuando `RegistroApiService` exponga `subsanarIniciativa`. Mientras
    // tanto, se marca el formulario como pendiente contractual (`NEEDS CLARIFICATION`).
    this.problem.set({
      type: 'about:blank',
      title: 'Subsanación pendiente de contrato',
      status: 501,
      code: 'SUBSANACION_ENDPOINT_PENDING',
      detail:
        'El endpoint público de subsanación de iniciativa aún no está publicado en el snapshot OpenAPI. Registre la corrección mediante el canal de incorporación o espere la habilitación contractual.',
      correlationId: 'N/A',
      violations: []
    });
    this.submitting.set(false);
  }

  private enviarCorreccionIncorporacion(): void {
    const incorporacion = this.incorporacion;
    if (!incorporacion) {
      this.problem.set({
        type: 'about:blank',
        title: 'Contexto requerido',
        status: 400,
        code: 'INCORPORATION_CONTEXT_REQUIRED',
        detail: 'Se requiere el detalle de la incorporación para registrar la corrección.',
        correlationId: 'N/A',
        violations: []
      });
      this.submitting.set(false);
      return;
    }

    this.registro
      .corregirIncorporacion(
        incorporacion.id,
        {
          incorporacionId: incorporacion.id,
          datosNuevos: this.form.controls.datosNuevos.value,
          motivo: this.form.controls.motivo.value
        },
        this.etag ? { etag: this.etag } : {}
      )
      .subscribe({
        next: (detalle) => {
          this.submitting.set(false);
          this.submitted.set(false);
          this.onPersisted?.(detalle);
        },
        error: (error: unknown) => {
          this.problem.set(parseProblemDetails(error));
          this.submitting.set(false);
        }
      });
  }
}
