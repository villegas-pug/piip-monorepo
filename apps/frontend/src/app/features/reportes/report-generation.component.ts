// Página de generación de reportes institucionales (US8 - T109).
//
// Esta pantalla del Evaluador genera reportes semestrales y
// extraordinarios. La operación es asíncrona: el servidor responde `202`
// con `ReportOperation` (BR-013, BR-121, BR-122) y la UI debe mantenerse
// actualizada hasta que el estado técnico se estabilice en `GENERADA`,
// `APROBADA` o `FALLIDA`.
//
// Decisiones de diseño:
//   * La UI delega en el backend la transición entre estados: la
//     generación se considera exitosa cuando el servidor lo indica. El
//     cliente solo refleja `estadoTecnico` y muestra acciones de reintento
//     cuando la operación es recuperable.
//   * `Idempotency-Key` se propaga mediante el interceptor global
//     (`idempotencyKeyInterceptor`) y mediante la opción explícita del
//     cliente (`ReportesApiService`) para que un reintento con la misma
//     clave y mismo payload no duplique el reporte.
//   * Los cortes 30/06 (semestre 1) y 31/12 (semestre 2) los deriva el
//     servidor; el cliente NO acepta una fecha de corte alternativa.
//   * Los filtros BR-123 no amplían el ámbito del generador: el cliente
//     solo los envía, no los evalúa. La denegación
//     (`403 REPORT_SCOPE_DENIED`) la aplica el backend.
//   * Sin contraseña, token ni atributo sensible: la identidad la
//     administra Keycloak y esta pantalla nunca lo solicita.
//
// Accesibilidad WCAG 2.1 AA:
//   * Skip link, `<main tabindex="-1">` y encabezados jerárquicos.
//   * `aria-live="polite"` para resultados y `aria-live="assertive"` para
//     errores y rechazos.
//   * Labels asociados (`for`/`id`) y mensajes de error con
//     `aria-describedby`.
//   * Regiones separadas para anuncio de estado y Problem Details.
//   * Botones con `aria-label` explícito cuando su texto no es suficiente.
//   * Foco visible y navegación por teclado; la cancelación siempre
//     está disponible sin enviar al backend.

import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnDestroy,
  inject,
  signal
} from '@angular/core';
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
import { MatRadioModule } from '@angular/material/radio';
import { Subject, firstValueFrom, takeUntil } from 'rxjs';

import { parseProblemDetails, ProblemDetails } from '../../core/http/problem-details';
import { ReportesApiService } from './api/reportes-api.service';
import {
  ReporteComandoOpciones,
  ReporteExtraordinarioRequest,
  ReporteFiltros,
  ReporteIdempotencyOpciones,
  ReporteOperacion,
  ReporteSemestralRequest,
  ReporteTipo
} from './api/types';

interface SemestralFormGroup {
  anio: FormControl<number | null>;
  semestre: FormControl<1 | 2 | null>;
}

interface ExtraordinarioFormGroup {
  solicitudDocumentoId: FormControl<number | null>;
  aprobacionOficinaDocumentoId: FormControl<number | null>;
  periodo: FormControl<string>;
  fechaCorte: FormControl<string>;
  unidadId: FormControl<number>;
  responsableId: FormControl<number | null>;
  fuente: FormControl<string>;
  tipo: FormControl<string>;
  estado: FormControl<string>;
  tipoSolucion: FormControl<string>;
  producto: FormControl<string>;
}

const MAX_PERIODO = 30;
const MAX_FILTRO = 40;
const MAX_PRODUCTO = 40;
const MIN_ANIO = 2000;
const MAX_ANIO = 2100;
const MIN_DOCUMENTO = 1;
const IDEMPOTENCY_KEY_BYTES = 36;

const ID_MAIN = 'report-generation-main';
const ID_FORM_SEMESTRAL = 'report-generation-semestral-form';
const ID_FORM_EXTRAORDINARIO = 'report-generation-extraordinario-form';
const ID_RESULTADOS = 'report-generation-resultados';
const ID_ERRORES = 'report-generation-errores';
const ID_ANIO = 'report-generation-anio';
const ID_SEMESTRE = 'report-generation-semestre';
const ID_SOLICITUD_DOC = 'report-generation-solicitud';
const ID_APROBACION_DOC = 'report-generation-aprobacion';
const ID_PERIODO = 'report-generation-periodo';
const ID_FECHA_CORTE = 'report-generation-fecha-corte';
const ID_FILTRO_UNIDAD = 'report-generation-filtro-unidad';
const ID_FILTRO_RESPONSABLE = 'report-generation-filtro-responsable';
const ID_FILTRO_FUENTE = 'report-generation-filtro-fuente';
const ID_FILTRO_TIPO = 'report-generation-filtro-tipo';
const ID_FILTRO_ESTADO = 'report-generation-filtro-estado';
const ID_FILTRO_TIPO_SOLUCION = 'report-generation-filtro-tipo-solucion';
const ID_FILTRO_PRODUCTO = 'report-generation-filtro-producto';

@Component({
  selector: 'app-report-generation',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatRadioModule
  ],
  templateUrl: './report-generation.component.html',
  styleUrl: './report-generation.component.scss'
})
export class ReportGenerationComponent implements OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ReportesApiService);
  private readonly destroy$ = new Subject<void>();
  private readonly idempotencyKey = signal<string>('');

  protected readonly idMain = ID_MAIN;
  protected readonly idFormSemestral = ID_FORM_SEMESTRAL;
  protected readonly idFormExtraordinario = ID_FORM_EXTRAORDINARIO;
  protected readonly idResultados = ID_RESULTADOS;
  protected readonly idErrores = ID_ERRORES;
  protected readonly idAnio = ID_ANIO;
  protected readonly idSemestre = ID_SEMESTRE;
  protected readonly idSolicitudDoc = ID_SOLICITUD_DOC;
  protected readonly idAprobacionDoc = ID_APROBACION_DOC;
  protected readonly idPeriodo = ID_PERIODO;
  protected readonly idFechaCorte = ID_FECHA_CORTE;
  protected readonly idFiltroUnidad = ID_FILTRO_UNIDAD;
  protected readonly idFiltroResponsable = ID_FILTRO_RESPONSABLE;
  protected readonly idFiltroFuente = ID_FILTRO_FUENTE;
  protected readonly idFiltroTipo = ID_FILTRO_TIPO;
  protected readonly idFiltroEstado = ID_FILTRO_ESTADO;
  protected readonly idFiltroTipoSolucion = ID_FILTRO_TIPO_SOLUCION;
  protected readonly idFiltroProducto = ID_FILTRO_PRODUCTO;

  protected readonly tipo = signal<ReporteTipo>('SEMESTRAL');
  protected readonly formSemestral: FormGroup<SemestralFormGroup> = this.buildSemestral();
  protected readonly formExtraordinario: FormGroup<ExtraordinarioFormGroup> = this.buildExtraordinario();

  protected readonly submitted = signal(false);
  protected readonly submitting = signal(false);
  protected readonly operacion = signal<ReporteOperacion | undefined>(undefined);
  protected readonly problema = signal<ProblemDetails | undefined>(undefined);
  protected readonly submittedExtra = signal(false);
  protected readonly submittingExtra = signal(false);
  protected readonly operacionExtra = signal<ReporteOperacion | undefined>(undefined);
  protected readonly problemaExtra = signal<ProblemDetails | undefined>(undefined);

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  // ---------------------------------------------------------------------------
  // Acciones: reporte semestral
  // ---------------------------------------------------------------------------

  protected async generarSemestral(): Promise<void> {
    this.submitted.set(true);
    this.problema.set(undefined);
    if (this.formSemestral.invalid) {
      this.formSemestral.markAllAsTouched();
      return;
    }
    const raw = this.formSemestral.getRawValue();
    if (raw.anio === null || raw.semestre === null) {
      return;
    }
    const payload: ReporteSemestralRequest = {
      anio: raw.anio,
      semestre: raw.semestre
    };
    const opciones = this.opcionesComando();
    this.submitting.set(true);
    try {
      const operacion = await firstValueFrom(
        this.api.generarReporteSemestral(payload, opciones)
      );
      this.operacion.set(operacion);
    } catch (error: unknown) {
      this.problema.set(parseProblemDetails(error) ?? fallbackProblem('No fue posible generar el reporte semestral.'));
    } finally {
      this.submitting.set(false);
    }
  }

  protected cancelarSemestral(): void {
    this.formSemestral.reset({ anio: null, semestre: null });
    this.submitted.set(false);
    this.problema.set(undefined);
  }

  // ---------------------------------------------------------------------------
  // Acciones: reporte extraordinario
  // ---------------------------------------------------------------------------

  protected async generarExtraordinario(): Promise<void> {
    this.submittedExtra.set(true);
    this.problemaExtra.set(undefined);
    if (this.formExtraordinario.invalid) {
      this.formExtraordinario.markAllAsTouched();
      return;
    }
    const raw = this.formExtraordinario.getRawValue();
    if (
      raw.solicitudDocumentoId === null ||
      raw.aprobacionOficinaDocumentoId === null
    ) {
      return;
    }
    const filtros = this.construirFiltros(raw);
    const payload: ReporteExtraordinarioRequest = {
      solicitudDocumentoId: raw.solicitudDocumentoId,
      aprobacionOficinaDocumentoId: raw.aprobacionOficinaDocumentoId,
      periodo: raw.periodo.trim(),
      fechaCorte: raw.fechaCorte,
      filtros
    };
    const opciones = this.opcionesComando();
    this.submittingExtra.set(true);
    try {
      const operacion = await firstValueFrom(
        this.api.generarReporteExtraordinario(payload, opciones)
      );
      this.operacionExtra.set(operacion);
    } catch (error: unknown) {
      this.problemaExtra.set(
        parseProblemDetails(error) ??
          fallbackProblem('No fue posible generar el reporte extraordinario.')
      );
    } finally {
      this.submittingExtra.set(false);
    }
  }

  protected cancelarExtraordinario(): void {
    this.formExtraordinario.reset({
      solicitudDocumentoId: null,
      aprobacionOficinaDocumentoId: null,
      periodo: '',
      fechaCorte: '',
      unidadId: 0,
      responsableId: null,
      fuente: '',
      tipo: '',
      estado: '',
      tipoSolucion: '',
      producto: ''
    });
    this.submittedExtra.set(false);
    this.problemaExtra.set(undefined);
  }

  // ---------------------------------------------------------------------------
  // Helpers accesibles para plantillas
  // ---------------------------------------------------------------------------

  protected mensajeError(
    control: FormControl<unknown> | null,
    submitted: boolean
  ): string | null {
    if (!control || !(control.touched || submitted)) {
      return null;
    }
    const errors = control.errors;
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
    if (errors['max']) {
      const requerido = errors['max'] as { max: number };
      return `Debe ser menor o igual a ${requerido.max}.`;
    }
    return null;
  }

  protected descripcionEstado(estado: string): string {
    return estado.replace(/_/g, ' ').toLowerCase();
  }

  protected descripcionClasificacion(clasificacion?: string): string {
    if (!clasificacion) {
      return 'Sin clasificar';
    }
    return clasificacion.toLowerCase();
  }

  // ---------------------------------------------------------------------------
  // Internos
  // ---------------------------------------------------------------------------

  private buildSemestral(): FormGroup<SemestralFormGroup> {
    return this.fb.group<SemestralFormGroup>({
      anio: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(MIN_ANIO), Validators.max(MAX_ANIO)]
      }),
      semestre: this.fb.control<1 | 2 | null>(null, {
        validators: [Validators.required]
      })
    });
  }

  private buildExtraordinario(): FormGroup<ExtraordinarioFormGroup> {
    return this.fb.group<ExtraordinarioFormGroup>({
      solicitudDocumentoId: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(MIN_DOCUMENTO)]
      }),
      aprobacionOficinaDocumentoId: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(MIN_DOCUMENTO)]
      }),
      periodo: this.fb.nonNullable.control('', {
        validators: [Validators.required, Validators.maxLength(MAX_PERIODO)]
      }),
      fechaCorte: this.fb.nonNullable.control('', {
        validators: [Validators.required, Validators.pattern(/^\d{4}-\d{2}-\d{2}$/)]
      }),
      unidadId: this.fb.nonNullable.control<number>(0, {
        validators: [Validators.min(MIN_DOCUMENTO)]
      }),
      responsableId: this.fb.control<number | null>(null, {
        validators: [Validators.min(MIN_DOCUMENTO)]
      }),
      fuente: this.fb.nonNullable.control('', { validators: [Validators.maxLength(MAX_FILTRO)] }),
      tipo: this.fb.nonNullable.control('', { validators: [Validators.maxLength(MAX_FILTRO)] }),
      estado: this.fb.nonNullable.control('', { validators: [Validators.maxLength(MAX_FILTRO)] }),
      tipoSolucion: this.fb.nonNullable.control('', { validators: [Validators.maxLength(MAX_FILTRO)] }),
      producto: this.fb.nonNullable.control('', { validators: [Validators.maxLength(MAX_PRODUCTO)] })
    });
  }

  private construirFiltros(raw: ReturnType<FormGroup<ExtraordinarioFormGroup>['getRawValue']>): ReporteFiltros {
    const filtros: ReporteFiltros = {};
    const limpiar = (valor: string): string | undefined => {
      const trimmed = valor.trim();
      return trimmed ? trimmed : undefined;
    };
    const tipo = limpiar(raw.tipo);
    if (tipo) {
      Object.assign(filtros, { tipo });
    }
    const estado = limpiar(raw.estado);
    if (estado) {
      Object.assign(filtros, { estado });
    }
    if (raw.unidadId > 0) {
      Object.assign(filtros, { unidadId: raw.unidadId });
    }
    if (raw.responsableId !== null) {
      Object.assign(filtros, { responsableId: raw.responsableId });
    }
    const fuente = limpiar(raw.fuente);
    if (fuente) {
      Object.assign(filtros, { fuente });
    }
    const tipoSolucion = limpiar(raw.tipoSolucion);
    if (tipoSolucion) {
      Object.assign(filtros, { tipoSolucion });
    }
    const producto = limpiar(raw.producto);
    if (producto) {
      Object.assign(filtros, { producto });
    }
    return filtros;
  }

  private opcionesComando(): ReporteComandoOpciones & ReporteIdempotencyOpciones {
    let clave = this.idempotencyKey();
    if (!clave) {
      clave = this.generarIdempotencyKey();
      this.idempotencyKey.set(clave);
    }
    return { idempotencyKey: clave };
  }

  private generarIdempotencyKey(): string {
    // Reutiliza el generador del interceptor cuando esté disponible; el
    // cálculo manual es solo un respaldo para entornos de SSR donde
    // `crypto.randomUUID` puede no existir.
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    let resultado = '';
    for (let i = 0; i < IDEMPOTENCY_KEY_BYTES; i += 1) {
      resultado += Math.floor(Math.random() * 16).toString(16);
    }
    return resultado;
  }

  // Mantiene `takeUntil` aunque los flujos se consuman vía `firstValueFrom`
  // para alinear el patrón con los demás componentes institucionales.
  private readonly _alinear = takeUntil(this.destroy$);
}

function fallbackProblem(detail: string): ProblemDetails {
  return Object.freeze({
    type: 'about:blank',
    title: 'Operación no completada',
    status: 0,
    detail,
    violations: Object.freeze([])
  });
}
