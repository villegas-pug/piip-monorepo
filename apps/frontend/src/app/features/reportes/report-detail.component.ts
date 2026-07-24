// Página de detalle y estado de reporte institucional (US8 - T109).
//
// Esta pantalla muestra el detalle del reporte (corte, parámetros,
// versión de datos, estado técnico, clasificación, indicadores, totales
// y archivos) y aplica la UX de operación asíncrona exigida por la
// Constitución y por el contrato OpenAPI:
//
//   * **Polling**: cuando el estado técnico es `INICIADA` o `FALLIDA`,
//     la página vuelve a consultar `GET /reportes/generaciones/{id}`
//     cada `INTERVALO_POLLING_MS` milisegundos y se detiene
//     automáticamente al alcanzar un estado terminal (`GENERADA`,
//     `APROBADA`). El cliente no decide cuándo termina: lo declara el
//     backend mediante `estadoTecnico`.
//   * **Reintento idempotente**: el botón de reintento reutiliza la
//     misma `Idempotency-Key` que el `idempotencyKeyInterceptor` ya
//     inyectó en la generación original. La UI NO la genera de nuevo:
//     pediría un nuevo identificador que el backend trataría como
//     operación distinta y duplicaría el expediente.
//   * **ETag / If-None-Match**: la ETag devuelta por el backend se
//     propaga a la siguiente lectura para que el servidor responda 304
//     cuando el detalle no cambió, evitando re-renderizados y reduciendo
//     la superficie de procesamiento.
//   * **Problem Details**: las respuestas `application/problem+json`
//     se traducen con `parseProblemDetails` y la UI nunca inspecciona
//     códigos canónicos (`REPORT_NOT_FOUND`, `REPORT_SCOPE_DENIED`,
//     `REPORT_VERSION_NOT_APPROVED`, etc.). El componente expone el
//     código, el detalle y el `correlationId` al actor para soporte.
//   * **Sin decisión de acceso**: la clasificación del reporte
//     (`INTERNO` por defecto, `RESTRINGIDO` cuando algún dato del
//     snapshot lo es) la fija el backend. La UI la refleja y, en
//     `RESTRINGIDO`, anuncia el aviso a lectores de pantalla.
//
// Accesibilidad WCAG 2.1 AA:
//   * Skip link, `<main tabindex="-1">` y encabezados jerárquicos.
//   * `aria-live="polite"` para resultados y estado de polling.
//   * `aria-live="assertive"` para errores y rechazos.
//   * Tablas con `<caption>`, `scope="col"` y resumen.
//   * Estados con `aria-busy` y mensajes de error anunciados.
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
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, firstValueFrom, interval, takeUntil } from 'rxjs';

import { parseProblemDetails, ProblemDetails } from '../../core/http/problem-details';
import { ReportesApiService } from './api/reportes-api.service';
import {
  ReporteArchivoSummary,
  ReporteClasificacion,
  ReporteDetail,
  ReporteDetalleRespuesta,
  ReporteEstadoTecnico,
  ReporteFormatoArchivo,
  ReporteIndicador,
  ReporteTotalDimension
} from './api/types';

const INTERVALO_POLLING_MS = 4000;
const ESTADOS_TERMINALES: ReadonlySet<ReporteEstadoTecnico> = new Set<ReporteEstadoTecnico>([
  'GENERADA',
  'APROBADA',
  'FALLIDA'
]);

const ID_MAIN = 'report-detail-main';
const ID_RESULTADOS = 'report-detail-resultados';
const ID_ERRORES = 'report-detail-errores';
const ID_INDICADORES = 'report-detail-indicadores';
const ID_TOTALES = 'report-detail-totales';
const ID_ARCHIVOS = 'report-detail-archivos';
const ID_ESTADO = 'report-detail-estado';

const EMPTY_DETAIL: ReporteDetail = Object.freeze({
  idReporte: 0,
  tipo: 'SEMESTRAL',
  anio: 0,
  semestre: null,
  periodo: '',
  fechaCorte: '',
  versionDatos: 0,
  estadoTecnico: 'INICIADA',
  clasificacion: 'INTERNO',
  indicadores: Object.freeze([]),
  totales: Object.freeze([]),
  archivos: Object.freeze([]),
  etag: ''
});

@Component({
  selector: 'app-report-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './report-detail.component.html',
  styleUrl: './report-detail.component.scss'
})
export class ReportDetailComponent implements OnInit, OnDestroy {
  private readonly api = inject(ReportesApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly stop$ = new Subject<void>();

  protected readonly idMain = ID_MAIN;
  protected readonly idResultados = ID_RESULTADOS;
  protected readonly idErrores = ID_ERRORES;
  protected readonly idIndicadores = ID_INDICADORES;
  protected readonly idTotales = ID_TOTALES;
  protected readonly idArchivos = ID_ARCHIVOS;
  protected readonly idEstado = ID_ESTADO;

  protected readonly reporteId = signal<number | null>(null);
  protected readonly detalle = signal<ReporteDetail>(EMPTY_DETAIL);
  protected readonly etag = signal<string | undefined>(undefined);
  protected readonly correlationId = signal<string | undefined>(undefined);
  protected readonly notModified = signal(false);
  protected readonly cargando = signal(false);
  protected readonly descargando = signal<ReporteFormatoArchivo | undefined>(undefined);
  protected readonly problema = signal<ProblemDetails | undefined>(undefined);
  protected readonly pollingActivo = signal(false);
  protected readonly ultimoIntento = signal<Date | undefined>(undefined);

  protected readonly estadoTerminal = computed(() => ESTADOS_TERMINALES.has(this.detalle().estadoTecnico));
  protected readonly puedeAprobar = computed(() => this.detalle().estadoTecnico === 'GENERADA');
  protected readonly puedeRemitir = computed(() => this.detalle().estadoTecnico === 'APROBADA');

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

  async ngOnInit(): Promise<void> {
    const desdeRuta = this.route.snapshot.paramMap.get('id');
    if (desdeRuta && this.reporteId() === null) {
      const id = Number(desdeRuta);
      if (Number.isInteger(id) && id > 0) {
        this.reporteId.set(id);
      }
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
    await this.refrescar();
    this.iniciarPollingSiProcede();
    this.destroyRef.onDestroy(() => this.detenerPolling());
  }

  ngOnDestroy(): void {
    this.detenerPolling();
    this.stop$.next();
    this.stop$.complete();
  }

  // ---------------------------------------------------------------------------
  // Acciones: refresco, reintento, descarga, navegación
  // ---------------------------------------------------------------------------

  protected async refrescar(): Promise<void> {
    const id = this.reporteId();
    if (id === null) {
      return;
    }
    this.cargando.set(true);
    this.problema.set(undefined);
    this.notModified.set(false);
    this.ultimoIntento.set(new Date());
    try {
      const respuesta = await firstValueFrom(
        this.api.consultarReporte(id, { ifNoneMatch: this.etag() })
      );
      this.aplicarRespuesta(respuesta);
    } catch (error: unknown) {
      this.problema.set(
        parseProblemDetails(error) ?? fallbackProblem('No se pudo cargar el detalle del reporte.')
      );
    } finally {
      this.cargando.set(false);
    }
  }

  /**
   * Reintento idempotente: reutiliza la última ETag conocida para
   * actualizar el detalle y, si la operación todavía no es terminal,
   * reanuda el polling. NO genera una nueva generación: el reporte ya
   * existe; la UI solo refresca la vista.
   */
  protected async reintentar(): Promise<void> {
    this.detenerPolling();
    await this.refrescar();
    this.iniciarPollingSiProcede();
  }

  /**
   * Polling asíncrono: cuando el estado técnico es `INICIADA` o
   * `FALLIDA`, se vuelve a consultar el detalle cada
   * `INTERVALO_POLLING_MS` milisegundos hasta alcanzar un estado
   * terminal. Se detiene explícitamente en `ngOnDestroy` y antes de
   * navegar fuera.
   */
  protected iniciarPollingSiProcede(): void {
    if (this.pollingActivo() || this.estadoTerminal()) {
      return;
    }
    this.pollingActivo.set(true);
    interval(INTERVALO_POLLING_MS)
      .pipe(takeUntil(this.stop$), takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: async () => {
          if (this.estadoTerminal() || this.cargando()) {
            return;
          }
          try {
            const respuesta = await firstValueFrom(
              this.api.consultarReporte(this.reporteId()!, { ifNoneMatch: this.etag() })
            );
            this.aplicarRespuesta(respuesta);
          } catch (error: unknown) {
            this.problema.set(
              parseProblemDetails(error) ??
                fallbackProblem('No se pudo actualizar el estado del reporte.')
            );
            this.detenerPolling();
          }
        }
      });
  }

  protected detenerPolling(): void {
    if (!this.pollingActivo()) {
      return;
    }
    this.pollingActivo.set(false);
    this.stop$.next();
  }

  protected async descargar(formato: ReporteFormatoArchivo): Promise<void> {
    const id = this.reporteId();
    if (id === null) {
      return;
    }
    this.descargando.set(formato);
    this.problema.set(undefined);
    try {
      const blob = await firstValueFrom(this.api.descargarArchivoReporte(id, formato));
      this.materializarDescarga(blob, formato);
    } catch (error: unknown) {
      this.problema.set(
        parseProblemDetails(error) ?? fallbackProblem(`No se pudo descargar el archivo ${formato}.`)
      );
    } finally {
      this.descargando.set(undefined);
    }
  }

  protected irAprobacion(): void {
    const id = this.reporteId();
    if (id === null) {
      return;
    }
    this.detenerPolling();
    void this.router.navigate(['/reportes', id, 'aprobacion'], {
      queryParams: { etag: this.etag() }
    });
  }

  protected irRemision(): void {
    const id = this.reporteId();
    if (id === null) {
      return;
    }
    this.detenerPolling();
    void this.router.navigate(['/reportes', id, 'remision'], {
      queryParams: { etag: this.etag() }
    });
  }

  protected volver(): void {
    this.detenerPolling();
    void this.router.navigate(['/reportes']);
  }

  // ---------------------------------------------------------------------------
  // Proyecciones accesibles (WCAG 2.1 AA)
  // ---------------------------------------------------------------------------

  protected descripcionEstado(estado: ReporteEstadoTecnico): string {
    return estado.replace(/_/g, ' ').toLowerCase();
  }

  protected descripcionClasificacion(clasificacion: ReporteClasificacion): string {
    return clasificacion === 'RESTRINGIDO'
      ? 'restringido'
      : clasificacion.toLowerCase();
  }

  protected esIndicadorNoAplicable(indicador: ReporteIndicador): boolean {
    return !indicador.aplicable || indicador.porcentaje === null;
  }

  protected descripcionIndicador(indicador: ReporteIndicador): string {
    if (this.esIndicadorNoAplicable(indicador)) {
      return 'No aplicable (denominador cero).';
    }
    const porcentaje = indicador.porcentaje ?? 0;
    return `${porcentaje.toFixed(2)} % (${indicador.numerador} de ${indicador.denominador})`;
  }

  protected trackIndicador(_: number, indicador: ReporteIndicador): string {
    return indicador.nombre;
  }

  protected trackTotal(_: number, total: ReporteTotalDimension): string {
    return total.dimension;
  }

  protected trackArchivo(_: number, archivo: ReporteArchivoSummary): number {
    return archivo.idArchivo;
  }

  protected formatoArchivo(archivo: ReporteArchivoSummary): ReporteFormatoArchivo {
    return archivo.formato;
  }

  protected descripcionTipo(tipo: 'SEMESTRAL' | 'EXTRAORDINARIO'): string {
    return tipo === 'SEMESTRAL' ? 'Semestral' : 'Extraordinario';
  }

  protected pistaIndicador(): string {
    return 'Cuando el denominador es cero, el indicador se presenta como no aplicable y el porcentaje se omite. La UI no calcula divisiones.';
  }

  // ---------------------------------------------------------------------------
  // Internos
  // ---------------------------------------------------------------------------

  private aplicarRespuesta(respuesta: ReporteDetalleRespuesta): void {
    if (respuesta.notModified) {
      this.notModified.set(true);
      if (respuesta.etag) {
        this.etag.set(respuesta.etag);
      }
      this.correlationId.set(respuesta.correlationId);
      return;
    }
    this.detalle.set(respuesta.body);
    this.etag.set(respuesta.etag ?? respuesta.body.etag);
    this.correlationId.set(respuesta.correlationId);
    this.notModified.set(false);
    if (this.estadoTerminal()) {
      this.detenerPolling();
    }
  }

  private materializarDescarga(blob: Blob, formato: ReporteFormatoArchivo): void {
    if (typeof document === 'undefined' || typeof URL === 'undefined' || typeof URL.createObjectURL !== 'function') {
      return;
    }
    const url = URL.createObjectURL(blob);
    const ancla = document.createElement('a');
    ancla.href = url;
    const id = this.reporteId() ?? 'reporte';
    ancla.download = `reporte-${id}.${formato.toLowerCase()}`;
    ancla.rel = 'noopener';
    document.body.appendChild(ancla);
    ancla.click();
    ancla.remove();
    setTimeout(() => URL.revokeObjectURL(url), 0);
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
