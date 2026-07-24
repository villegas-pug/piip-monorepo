// Página de aprobación y remisión de reporte institucional (US8 - T109).
//
// Esta pantalla permite a la Oficina de Modernización aprobar la versión
// exacta del reporte, fijar el documento de aprobación y los
// destinatarios permitidos (BR-127, BR-125), y registrar la remisión
// manual contra los destinatarios aprobados (BR-128). El recorrido
// refleja el contrato OpenAPI:
//
//   * `POST /api/v1/reportes/generaciones/{id}/aprobaciones-remision`
//     exige `Idempotency-Key` y la ETag exacta del detalle
//     (`If-Match`). Una segunda aprobación para la misma versión
//     produce `409 REPORT_VERSION_ALREADY_APPROVED`.
//   * `GET  /api/v1/reportes/generaciones/{id}/destinatarios` lista los
//     destinatarios aprobados (BR-125) y se usa para presentar la
//     lista al Evaluador al registrar la remisión.
//   * `POST /api/v1/reportes/generaciones/{id}/remisiones` exige
//     `Idempotency-Key` y la ETag exacta; el resultado es declarativo
//     (`EXITOSA`, `FALLIDA`, `PENDIENTE`). Un resultado `FALLIDA` exige
//     motivo (BR-128). El backend rechaza remitir una versión distinta
//     de la aprobada (`409 REPORT_VERSION_NOT_APPROVED`).
//
// Decisiones de diseño:
//   * `Idempotency-Key` se propaga explícitamente desde la pantalla de
//     aprobación; un reintento con la misma clave y mismo payload no
//     duplica la aprobación.
//   * La ETag devuelta por `GET /reportes/generaciones/{id}` se envía
//     como `If-Match` para que la operación sensible rechace
//     aprobaciones o remisiones contra una versión modificada fuera
//     del flujo actual.
//   * La pantalla NO decide la transición entre aprobación y remisión:
//     el backend expone `estadoTecnico === 'APROBADA'` solo después de
//     aceptar la aprobación. La UI refleja la disponibilidad de
//     `remitir` en función de ese estado.
//   * Sin eliminación ni disposición: la Constitución veta la purga
//     mientras no exista tabla de retención aprobada.
//
// Accesibilidad WCAG 2.1 AA:
//   * Skip link, `<main tabindex="-1">` y encabezados jerárquicos.
//   * `aria-live="polite"` para resultados y `aria-live="assertive"`
//     para errores.
//   * Tablas con `<caption>` y `scope="col"`, listas con `aria-label`.
//   * Botones con `aria-label` explícito cuando el texto no basta.

import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  Input,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router } from '@angular/router';
import { firstValueFrom, takeUntil, Subject } from 'rxjs';

import { parseProblemDetails, ProblemDetails } from '../../core/http/problem-details';
import { ReportesApiService } from './api/reportes-api.service';
import {
  ReporteAprobacionDetail,
  ReporteAprobacionRequest,
  ReporteComandoOpciones,
  ReporteDestinatarioDetail,
  ReporteDestinatarioRequest,
  ReporteDetalleRespuesta,
  ReporteIdempotencyOpciones,
  ReporteRemisionDetail,
  ReporteRemisionPage,
  ReporteRemisionRequest,
  ReporteResultadoRemision,
  ReporteTipoDestinatario
} from './api/types';

const MIN_DOCUMENTO = 1;
const MIN_VERSION = 1;
const MIN_DESTINATARIO = 1;
const MAX_NOMBRE = 200;
const MAX_MOTIVO = 2000;
const IDEMPOTENCY_KEY_BYTES = 36;

const TIPOS_DESTINATARIO: readonly ReporteTipoDestinatario[] = [
  'AUTORIDAD_MIDAGRI',
  'OFICINA_MODERNIZACION',
  'PCM_SGP'
] as const;

const RESULTADOS_REMISION: readonly ReporteResultadoRemision[] = [
  'EXITOSA',
  'FALLIDA',
  'PENDIENTE'
] as const;

const ID_MAIN = 'report-approval-main';
const ID_FORM_APROBACION = 'report-approval-aprobacion-form';
const ID_FORM_REMISION = 'report-approval-remision-form';
const ID_RESULTADOS = 'report-approval-resultados';
const ID_ERRORES = 'report-approval-errores';
const ID_DESTINATARIOS = 'report-approval-destinatarios';
const ID_REMISIONES = 'report-approval-remisiones';
const ID_VERSION = 'report-approval-version';
const ID_DOCUMENTO_APROBACION = 'report-approval-documento';
const ID_RESULTADO = 'report-approval-resultado';
const ID_MOTIVO = 'report-approval-motivo';

interface DestinatarioFormGroup {
  tipoDestinatario: FormControl<ReporteTipoDestinatario | null>;
  idEntidad: FormControl<number | null>;
  nombre: FormControl<string>;
}

interface AprobacionFormGroup {
  idVersion: FormControl<number | null>;
  idDocumentoAprobacion: FormControl<number | null>;
  destinatarios: FormArray<FormGroup<DestinatarioFormGroup>>;
}

interface RemisionFormGroup {
  idVersion: FormControl<number | null>;
  destinatariosSeleccionados: FormControl<readonly number[]>;
  resultado: FormControl<ReporteResultadoRemision | null>;
  motivo: FormControl<string>;
}

@Component({
  selector: 'app-report-approval',
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
    MatSelectModule
  ],
  templateUrl: './report-approval.component.html',
  styleUrl: './report-approval.component.scss'
})
export class ReportApprovalComponent implements OnInit, OnDestroy {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ReportesApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly stop$ = new Subject<void>();

  protected readonly idMain = ID_MAIN;
  protected readonly idFormAprobacion = ID_FORM_APROBACION;
  protected readonly idFormRemision = ID_FORM_REMISION;
  protected readonly idResultados = ID_RESULTADOS;
  protected readonly idErrores = ID_ERRORES;
  protected readonly idDestinatarios = ID_DESTINATARIOS;
  protected readonly idRemisiones = ID_REMISIONES;
  protected readonly idVersion = ID_VERSION;
  protected readonly idDocumentoAprobacion = ID_DOCUMENTO_APROBACION;
  protected readonly idResultado = ID_RESULTADO;
  protected readonly idMotivo = ID_MOTIVO;

  protected readonly tiposDestinatario = TIPOS_DESTINATARIO;
  protected readonly resultadosRemision = RESULTADOS_REMISION;
  protected readonly maxMotivo = MAX_MOTIVO;

  protected readonly reporteId = signal<number | null>(null);
  protected readonly etag = signal<string | undefined>(undefined);
  protected readonly versionActual = signal<number>(1);
  protected readonly destinatarios = signal<readonly ReporteDestinatarioDetail[]>([]);
  protected readonly aprobacion = signal<ReporteAprobacionDetail | undefined>(undefined);
  protected readonly remision = signal<ReporteRemisionPage | undefined>(undefined);

  protected readonly cargando = signal(false);
  protected readonly submittingAprobacion = signal(false);
  protected readonly submittingRemision = signal(false);
  protected readonly submittedAprobacion = signal(false);
  protected readonly submittedRemision = signal(false);
  protected readonly problema = signal<ProblemDetails | undefined>(undefined);
  protected readonly problemaRemision = signal<ProblemDetails | undefined>(undefined);
  protected readonly idempotencyKey = signal<string>('');
  protected readonly idempotencyKeyRemision = signal<string>('');

  protected readonly puedeRemitir = computed(() => this.aprobacion() !== undefined);

  protected readonly formAprobacion: FormGroup<AprobacionFormGroup> = this.buildAprobacion();
  protected readonly formRemision: FormGroup<RemisionFormGroup> = this.buildRemision();

  @Input() set idReporte(value: string | number | null | undefined) {
    if (value === null || value === undefined || value === '') {
      this.reporteId.set(null);
      return;
    }
    const texto = String(value).trim();
    if (texto === '') {
      this.reporteId.set(null);
      return;
    }
    const id = Number(texto);
    if (Number.isInteger(id) && id > 0) {
      this.reporteId.set(id);
    } else {
      this.reporteId.set(null);
    }
  }

  @Input() set etagReporte(value: string | undefined) {
    if (value) {
      this.etag.set(value);
    }
  }

  async ngOnInit(): Promise<void> {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam && this.reporteId() === null) {
      const id = Number(idParam);
      if (Number.isInteger(id) && id > 0) {
        this.reporteId.set(id);
      }
    }
    const etagQuery = this.route.snapshot.queryParamMap.get('etag');
    if (etagQuery && !this.etag()) {
      this.etag.set(etagQuery);
    }
    if (this.reporteId() === null) {
      this.problema.set({
        type: 'about:blank',
        title: 'Identificador ausente',
        status: 400,
        detail: 'No se proporcionó un identificador de reporte.',
        violations: []
      });
      return;
    }
    await this.cargarDetalle();
    this.formAprobacion.controls.idVersion.valueChanges
      .pipe(takeUntil(this.stop$), takeUntilDestroyed(this.destroyRef))
      .subscribe((valor) => {
        if (typeof valor === 'number' && Number.isInteger(valor) && valor > 0) {
          this.versionActual.set(valor);
        }
      });
  }

  ngOnDestroy(): void {
    this.stop$.next();
    this.stop$.complete();
  }

  // ---------------------------------------------------------------------------
  // Acciones: aprobacion
  // ---------------------------------------------------------------------------

  protected agregarDestinatario(): void {
    this.formAprobacion.controls.destinatarios.push(
      this.fb.group<DestinatarioFormGroup>({
        tipoDestinatario: this.fb.control<ReporteTipoDestinatario | null>(null, {
          validators: [Validators.required]
        }),
        idEntidad: this.fb.control<number | null>(null, {
          validators: [Validators.required, Validators.min(MIN_DESTINATARIO)]
        }),
        nombre: this.fb.nonNullable.control('', {
          validators: [Validators.required, Validators.maxLength(MAX_NOMBRE)]
        })
      })
    );
  }

  protected eliminarDestinatario(indice: number): void {
    this.formAprobacion.controls.destinatarios.removeAt(indice);
  }

  protected async aprobar(): Promise<void> {
    this.submittedAprobacion.set(true);
    this.problema.set(undefined);
    if (this.formAprobacion.invalid) {
      this.formAprobacion.markAllAsTouched();
      return;
    }
    const id = this.reporteId();
    if (id === null) {
      return;
    }
    const raw = this.formAprobacion.getRawValue();
    if (raw.idVersion === null || raw.idDocumentoAprobacion === null) {
      return;
    }
    const destinatarios = this.coleccionDestinatarios(raw);
    if (destinatarios.length === 0) {
      this.formAprobacion.setErrors({ destinatariosRequeridos: true });
      this.formAprobacion.markAllAsTouched();
      return;
    }
    const payload: ReporteAprobacionRequest = {
      idVersion: raw.idVersion,
      idDocumentoAprobacion: raw.idDocumentoAprobacion,
      destinatarios
    };
    const opciones = this.opcionesComando();
    this.submittingAprobacion.set(true);
    try {
      const aprobacion = await firstValueFrom(
        this.api.aprobarReporte(id, payload, opciones)
      );
      this.aprobacion.set(aprobacion);
      this.destinatarios.set(aprobacion.destinatarios);
      this.versionActual.set(aprobacion.idVersion);
      this.etag.set(aprobacion.destinatarios[0]?.idAprobacion ? this.etag() : this.etag());
      await this.refrescarRemisiones();
    } catch (error: unknown) {
      this.problema.set(
        parseProblemDetails(error) ??
          fallbackProblem('No fue posible registrar la aprobación del reporte.')
      );
    } finally {
      this.submittingAprobacion.set(false);
    }
  }

  protected cancelarAprobacion(): void {
    this.formAprobacion.reset({
      idVersion: this.versionActual(),
      idDocumentoAprobacion: null
    });
    while (this.formAprobacion.controls.destinatarios.length > 0) {
      this.formAprobacion.controls.destinatarios.removeAt(0);
    }
    this.agregarDestinatario();
    this.submittedAprobacion.set(false);
    this.problema.set(undefined);
  }

  // ---------------------------------------------------------------------------
  // Acciones: remisión
  // ---------------------------------------------------------------------------

  protected async remitir(): Promise<void> {
    this.submittedRemision.set(true);
    this.problemaRemision.set(undefined);
    if (this.formRemision.invalid) {
      this.formRemision.markAllAsTouched();
      return;
    }
    const id = this.reporteId();
    if (id === null) {
      return;
    }
    const raw = this.formRemision.getRawValue();
    if (raw.idVersion === null || raw.resultado === null) {
      return;
    }
    if (raw.resultado === 'FALLIDA' && !raw.motivo.trim()) {
      this.formRemision.controls.motivo.setErrors({ required: true });
      this.formRemision.controls.motivo.markAsTouched();
      return;
    }
    if (raw.destinatariosSeleccionados.length === 0) {
      this.formRemision.setErrors({ destinatariosRequeridos: true });
      this.formRemision.markAllAsTouched();
      return;
    }
    const payload: ReporteRemisionRequest = {
      idVersion: raw.idVersion,
      destinatariosIds: raw.destinatariosSeleccionados,
      resultado: raw.resultado,
      motivo: raw.motivo.trim() || undefined
    };
    const opciones = this.opcionesComandoRemision();
    this.submittingRemision.set(true);
    try {
      const remision = await firstValueFrom(this.api.remitirReporte(id, payload, opciones));
      this.remision.set(remision);
      this.formRemision.patchValue({ destinatariosSeleccionados: [], motivo: '' });
      this.submittedRemision.set(false);
    } catch (error: unknown) {
      this.problemaRemision.set(
        parseProblemDetails(error) ??
          fallbackProblem('No fue posible registrar la remisión del reporte.')
      );
    } finally {
      this.submittingRemision.set(false);
    }
  }

  protected cancelarRemision(): void {
    this.formRemision.patchValue({
      idVersion: this.versionActual(),
      destinatariosSeleccionados: [],
      motivo: '',
      resultado: 'EXITOSA'
    });
    this.submittedRemision.set(false);
    this.problemaRemision.set(undefined);
  }

  protected volver(): void {
    const id = this.reporteId();
    if (id === null) {
      void this.router.navigate(['/reportes']);
      return;
    }
    void this.router.navigate(['/reportes', id]);
  }

  // ---------------------------------------------------------------------------
  // Proyecciones accesibles
  // ---------------------------------------------------------------------------

  protected descripcionTipo(tipo: ReporteTipoDestinatario): string {
    switch (tipo) {
      case 'AUTORIDAD_MIDAGRI':
        return 'Autoridad MIDAGRI';
      case 'OFICINA_MODERNIZACION':
        return 'Oficina de Modernización';
      case 'PCM_SGP':
        return 'PCM-SGP';
      default:
        return tipo;
    }
  }

  protected descripcionResultado(resultado: ReporteResultadoRemision): string {
    return resultado.toLowerCase();
  }

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

  protected trackDestinatarioAprobado(_: number, d: ReporteDestinatarioDetail): number {
    return d.idDestinatario;
  }

  protected trackRemision(_: number, r: ReporteRemisionDetail): number {
    return r.idRemision;
  }

  protected destinatarioEstaSeleccionado(idDestinatario: number): boolean {
    return (this.formRemision.controls.destinatariosSeleccionados.value ?? []).includes(
      idDestinatario
    );
  }

  protected alternarDestinatario(idDestinatario: number, seleccionado: boolean): void {
    const actual = new Set(this.formRemision.controls.destinatariosSeleccionados.value ?? []);
    if (seleccionado) {
      actual.add(idDestinatario);
    } else {
      actual.delete(idDestinatario);
    }
    this.formRemision.controls.destinatariosSeleccionados.setValue(
      Array.from(actual).sort((a, b) => a - b)
    );
  }

  protected esDestinatarioAprobable(d: ReporteDestinatarioDetail): boolean {
    return TIPOS_DESTINATARIO.includes(d.tipoDestinatario);
  }

  // ---------------------------------------------------------------------------
  // Internos
  // ---------------------------------------------------------------------------

  private async cargarDetalle(): Promise<void> {
    const id = this.reporteId();
    if (id === null) {
      return;
    }
    this.cargando.set(true);
    this.problema.set(undefined);
    try {
      const respuesta: ReporteDetalleRespuesta = await firstValueFrom(
        this.api.consultarReporte(id, { ifNoneMatch: this.etag() })
      );
      if (!respuesta.notModified) {
        this.etag.set(respuesta.etag ?? respuesta.body.etag);
        this.versionActual.set(respuesta.body.versionDatos);
        this.formAprobacion.patchValue({ idVersion: respuesta.body.versionDatos });
        this.formRemision.patchValue({ idVersion: respuesta.body.versionDatos });
      }
      try {
        const destinatarios = await firstValueFrom(this.api.listarDestinatariosReporte(id));
        if (destinatarios.length > 0) {
          this.destinatarios.set(destinatarios);
        }
      } catch (error: unknown) {
        const problema = parseProblemDetails(error);
        if (problema?.status !== 404) {
          this.problema.set(problema ?? fallbackProblem('No se pudieron listar los destinatarios.'));
        }
      }
      await this.refrescarRemisiones();
    } catch (error: unknown) {
      this.problema.set(
        parseProblemDetails(error) ?? fallbackProblem('No fue posible cargar el detalle del reporte.')
      );
    } finally {
      this.cargando.set(false);
    }
  }

  private async refrescarRemisiones(): Promise<void> {
    const id = this.reporteId();
    if (id === null) {
      return;
    }
    try {
      const remision = await firstValueFrom(
        this.api.consultarRemisionesReporte(id, this.versionActual())
      );
      this.remision.set(remision);
    } catch (error: unknown) {
      this.problema.set(
        parseProblemDetails(error) ?? fallbackProblem('No se pudo consultar el historial de remisiones.')
      );
    }
  }

  private buildAprobacion(): FormGroup<AprobacionFormGroup> {
    const grupo = this.fb.group<AprobacionFormGroup>({
      idVersion: this.fb.control<number | null>(1, {
        validators: [Validators.required, Validators.min(MIN_VERSION)]
      }),
      idDocumentoAprobacion: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(MIN_DOCUMENTO)]
      }),
      destinatarios: this.fb.array<FormGroup<DestinatarioFormGroup>>([])
    });
    grupo.controls.destinatarios.push(this.construirDestinatarioFormGroup());
    return grupo;
  }

  private buildRemision(): FormGroup<RemisionFormGroup> {
    return this.fb.group<RemisionFormGroup>({
      idVersion: this.fb.control<number | null>(1, {
        validators: [Validators.required, Validators.min(MIN_VERSION)]
      }),
      destinatariosSeleccionados: this.fb.nonNullable.control<readonly number[]>([]),
      resultado: this.fb.control<ReporteResultadoRemision | null>('EXITOSA', {
        validators: [Validators.required]
      }),
      motivo: this.fb.nonNullable.control('', {
        validators: [Validators.maxLength(MAX_MOTIVO)]
      })
    });
  }

  private construirDestinatarioFormGroup(): FormGroup<DestinatarioFormGroup> {
    return this.fb.group<DestinatarioFormGroup>({
      tipoDestinatario: this.fb.control<ReporteTipoDestinatario | null>(null, {
        validators: [Validators.required]
      }),
      idEntidad: this.fb.control<number | null>(null, {
        validators: [Validators.required, Validators.min(MIN_DESTINATARIO)]
      }),
      nombre: this.fb.nonNullable.control('', {
        validators: [Validators.required, Validators.maxLength(MAX_NOMBRE)]
      })
    });
  }

  private coleccionDestinatarios(
    raw: ReturnType<FormGroup<AprobacionFormGroup>['getRawValue']>
  ): readonly ReporteDestinatarioRequest[] {
    return raw.destinatarios
      .map((item) => ({
        tipoDestinatario: item.tipoDestinatario ?? undefined,
        idEntidad: item.idEntidad ?? undefined,
        nombre: (item.nombre ?? '').trim()
      }))
      .filter(
        (
          d
        ): d is {
          tipoDestinatario: ReporteTipoDestinatario;
          idEntidad: number;
          nombre: string;
        } =>
          d.tipoDestinatario !== undefined &&
          d.idEntidad !== undefined &&
          d.nombre.length > 0
      )
      .map((d) => ({
        tipoDestinatario: d.tipoDestinatario,
        idEntidad: d.idEntidad,
        nombre: d.nombre
      }));
  }

  private opcionesComando(): ReporteComandoOpciones & ReporteIdempotencyOpciones {
    let clave = this.idempotencyKey();
    if (!clave) {
      clave = this.generarIdempotencyKey();
      this.idempotencyKey.set(clave);
    }
    return { etag: this.etag(), idempotencyKey: clave };
  }

  private opcionesComandoRemision(): ReporteComandoOpciones & ReporteIdempotencyOpciones {
    let clave = this.idempotencyKeyRemision();
    if (!clave) {
      clave = this.generarIdempotencyKey();
      this.idempotencyKeyRemision.set(clave);
    }
    return { etag: this.etag(), idempotencyKey: clave };
  }

  private generarIdempotencyKey(): string {
    if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
      return crypto.randomUUID();
    }
    let resultado = '';
    for (let i = 0; i < IDEMPOTENCY_KEY_BYTES; i += 1) {
      resultado += Math.floor(Math.random() * 16).toString(16);
    }
    return resultado;
  }
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
