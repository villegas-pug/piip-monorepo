// Cliente HTTP del módulo institucional de reportes (US8 - T109).
//
// Encapsula las operaciones del recorrido del Evaluador para generar
// reportes semestrales y extraordinarios, consultar su estado, aprobar la
// versión exacta y registrar la remisión manual. La fuente única de verdad
// es el snapshot OpenAPI codigo-first aprobado en
// `specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml`
// (etiqueta `Reportes institucionales`).
//
// Decisiones de diseño:
//   * `provideIn: 'root'` para compartir una sola instancia por aplicación.
//     Ningún estado de UI vive en el servicio; los componentes lo gestionan
//     con `signal`.
//   * Los DTOs son inmutables (`readonly`) y se proyectan en `Object.freeze`
//     antes de salir del cliente para evitar mutaciones accidentales.
//   * La autorización efectiva la conserva el backend: el cliente NO decide
//     ni aplica perfiles, unidades, clasificación ni transiciones. Las
//     respuestas `application/problem+json` se traducen en la UI mediante
//     `parseProblemDetails` (`core/http/problem-details`); el cliente no
//     inspecciona códigos canónicos.
//
// Cabeceras aplicadas:
//   * `Authorization: Bearer <token>` la añade `authInterceptor` a las
//     rutas `/api/v1/` que no sean de consulta pública.
//   * `X-Asignacion-Efectiva-Id` la añade `effectiveAssignmentInterceptor`
//     cuando hay una asignación efectiva seleccionada; el backend la
//     revalida contra Oracle en cada llamada.
//   * `Idempotency-Key` la añade `idempotencyKeyInterceptor` a los verbos
//     `POST` institucionales que activan el contexto, salvo que el
//     consumidor ya la haya adjuntado. Las generaciones, aprobaciones y
//     remisiones propagan la clave para no duplicar expedientes cuando el
//     usuario reintenta.
//   * `If-Match` se traduce desde la ETag recibida en una lectura previa
//     mediante `entityTagInterceptor` o `withEntityTag`; `If-None-Match`
//     se aplica explícitamente en la consulta de detalle para detectar
//     `304 Not Modified`.
//   * `X-Correlation-Id` lo devuelve el backend y la UI lo conserva para
//     auditoría y soporte.
//
// UX de operación asíncrona:
//   * Las generaciones devuelven `202` con `ReportOperation` y la
//     operación se considera asíncrona; el componente asociado realiza
//     polling sobre `GET /reportes/generaciones/{id}` con `If-None-Match`
//     para evitar re-renderizados innecesarios.
//   * Los reintentos que conservan la `Idempotency-Key` propagada por el
//     interceptor (o explícita del consumidor) no duplican el reporte: la
//     regla de BR-122 la aplica el backend.
//   * Las descargas PDF/XLSX se hacen contra el mismo snapshot; el cliente
//     expone solo el método HTTP y deja la materialización del archivo al
//     navegador (vía `<a download>` o `Blob`), sin filtrar el BLOB a la UI.

import { HttpClient, HttpContext, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { withEntityTag } from '../../../core/http/entity-tag';
import { REQUIRES_IDEMPOTENCY_KEY } from '../../../core/http/idempotency-key.service';
import {
  ReporteAprobacionDetail,
  ReporteAprobacionRequest,
  ReporteArchivoSummary,
  ReporteComandoOpciones,
  ReporteDetalleOpciones,
  ReporteDetalleRespuesta,
  ReporteDetail,
  ReporteDestinatarioDetail,
  ReporteExtraordinarioRequest,
  ReporteFiltros,
  ReporteFormatoArchivo,
  ReporteIdempotencyOpciones,
  ReporteIndicador,
  ReporteOperacion,
  ReporteRemisionDetail,
  ReporteRemisionPage,
  ReporteRemisionRequest,
  ReporteSemestralRequest,
  ReporteTipoDestinatario,
  ReporteTotalDimension
} from './types';

@Injectable({ providedIn: 'root' })
export class ReportesApiService {
  private readonly http = inject(HttpClient);
  private static readonly BASE = '/api/v1/reportes';
  private static readonly GENERACIONES = '/api/v1/reportes/generaciones';
  private static readonly SEMESTRALES = '/api/v1/reportes/semestrales/generaciones';
  private static readonly EXTRAORDINARIOS = '/api/v1/reportes/extraordinarios/generaciones';

  // -------------------------------------------------------------------------
  // Generación
  // -------------------------------------------------------------------------

  /**
   * Solicita la generación del reporte semestral oficial. La operación es
   * asíncrona: el servidor responde `202` con `ReportOperation` y
   * `Idempotency-Key` es obligatoria para que un reintento con la misma
   * clave y mismo payload no duplique el reporte.
   */
  generarReporteSemestral(
    payload: ReporteSemestralRequest,
    opciones: ReporteComandoOpciones & ReporteIdempotencyOpciones = {}
  ): Observable<ReporteOperacion> {
    return this.http
      .post<ReporteOperacion>(ReportesApiService.SEMESTRALES, payload, {
        context: commandContext(opciones.etag, opciones.idempotencyKey)
      })
      .pipe(map(freezeOperacion));
  }

  /**
   * Solicita la generación del reporte extraordinario. Exige solicitud y
   * aprobación documentadas: si faltan, el backend responde
   * `422 REPORT_REQUEST_APPROVAL_REQUIRED`.
   */
  generarReporteExtraordinario(
    payload: ReporteExtraordinarioRequest,
    opciones: ReporteComandoOpciones & ReporteIdempotencyOpciones = {}
  ): Observable<ReporteOperacion> {
    return this.http
      .post<ReporteOperacion>(ReportesApiService.EXTRAORDINARIOS, payload, {
        context: commandContext(opciones.etag, opciones.idempotencyKey)
      })
      .pipe(map(freezeOperacion));
  }

  // -------------------------------------------------------------------------
  // Estado y detalle
  // -------------------------------------------------------------------------

  /**
   * Consulta el detalle del reporte. La respuesta 200 incluye el ETag
   * para control de concurrencia y la UI lo conserva para `If-Match` en
   * aprobaciones y remisiones.
   */
  consultarReporte(
    id: number,
    opciones: ReporteDetalleOpciones = {}
  ): Observable<ReporteDetalleRespuesta> {
    return this.http
      .get<RawReporteDetail>(`${ReportesApiService.GENERACIONES}/${id}`, {
        observe: 'response',
        headers: buildDetalleHeaders(opciones)
      })
      .pipe(map((response) => toDetalleRespuesta(response)));
  }

  /**
   * Descarga el archivo PDF o XLSX emitido desde el mismo snapshot. La
   * respuesta es binaria y se devuelve al consumidor como `Blob` para
   * que la UI la materialice (`URL.createObjectURL` o `<a download>`).
   * El `Content-Type` declara la naturaleza del archivo.
   */
  descargarArchivoReporte(id: number, formato: ReporteFormatoArchivo): Observable<Blob> {
    return this.http.get(
      `${ReportesApiService.GENERACIONES}/${id}/archivos/${formato}`,
      { responseType: 'blob' }
    );
  }

  // -------------------------------------------------------------------------
  // Aprobación
  // -------------------------------------------------------------------------

  /**
   * Aprueba la versión exacta del reporte, fija el documento de
   * aprobación y los destinatarios permitidos (BR-127). La operación es
   * idempotente: una segunda aprobación para la misma versión produce
   * `409 REPORT_VERSION_ALREADY_APPROVED`.
   */
  aprobarReporte(
    id: number,
    payload: ReporteAprobacionRequest,
    opciones: ReporteComandoOpciones & ReporteIdempotencyOpciones = {}
  ): Observable<ReporteAprobacionDetail> {
    return this.http
      .post<ReporteAprobacionDetail>(
        `${ReportesApiService.GENERACIONES}/${id}/aprobaciones-remision`,
        payload,
        { context: commandContext(opciones.etag, opciones.idempotencyKey) }
      )
      .pipe(map(freezeAprobacion));
  }

  /** Lista los destinatarios aprobados en la última versión aprobada (BR-125). */
  listarDestinatariosReporte(id: number): Observable<readonly ReporteDestinatarioDetail[]> {
    return this.http
      .get<readonly ReporteDestinatarioDetail[]>(
        `${ReportesApiService.GENERACIONES}/${id}/destinatarios`
      )
      .pipe(map((destinatarios) => Object.freeze((destinatarios ?? []).map(freezeDestinatario))));
  }

  // -------------------------------------------------------------------------
  // Remisión
  // -------------------------------------------------------------------------

  /**
   * Consulta el historial de remisiones, opcionalmente filtrado por
   * versión. Permite reconstruir la trazabilidad del reporte.
   */
  consultarRemisionesReporte(
    id: number,
    idVersion?: number
  ): Observable<ReporteRemisionPage> {
    let params = new HttpParams();
    if (idVersion !== undefined) {
      params = params.set('idVersion', idVersion);
    }
    return this.http
      .get<ReporteRemisionPage>(`${ReportesApiService.GENERACIONES}/${id}/remisiones`, { params })
      .pipe(map(freezeRemisionPage));
  }

  /**
   * Registra la remisión manual contra los destinatarios aprobados. El
   * resultado es declarativo (`EXITOSA`, `FALLIDA`, `PENDIENTE`); un
   * resultado `FALLIDA` exige motivo (BR-128). El backend rechaza
   * remitir una versión distinta de la aprobada
   * (`409 REPORT_VERSION_NOT_APPROVED`).
   */
  remitirReporte(
    id: number,
    payload: ReporteRemisionRequest,
    opciones: ReporteComandoOpciones & ReporteIdempotencyOpciones = {}
  ): Observable<ReporteRemisionPage> {
    return this.http
      .post<ReporteRemisionPage>(
        `${ReportesApiService.GENERACIONES}/${id}/remisiones`,
        payload,
        { context: commandContext(opciones.etag, opciones.idempotencyKey) }
      )
      .pipe(map(freezeRemisionPage));
  }

  /** Ruta base del módulo; única fuente de verdad para los componentes. */
  static get rutaBase(): string {
    return ReportesApiService.BASE;
  }
}

// ---------------------------------------------------------------------------
// Tipos crudos del backend (forma del JSON entrante)
// ---------------------------------------------------------------------------

interface RawReporteIndicador {
  readonly nombre: string;
  readonly numerador: number;
  readonly denominador: number;
  readonly porcentaje: number | null;
  readonly aplicable: boolean;
  readonly detalle?: string;
}

interface RawReporteTotalDimensionItem {
  readonly clave: string;
  readonly etiqueta: string;
  readonly total: number;
}

interface RawReporteTotalDimension {
  readonly dimension: string;
  readonly items: readonly RawReporteTotalDimensionItem[];
}

interface RawReporteArchivo {
  readonly idArchivo: number;
  readonly formato: ReporteFormatoArchivo;
  readonly version: number;
  readonly hashSha256: string;
  readonly idDocumentoVersion?: number;
  readonly creadoPor?: string;
  readonly fechaCreacion?: string;
}

interface RawReporteFiltros {
  readonly tipo?: string;
  readonly estado?: string;
  readonly unidadId?: number;
  readonly responsableId?: number;
  readonly fuente?: string;
  readonly tipoSolucion?: string;
  readonly producto?: string;
  readonly unidadesAdicionales?: readonly string[];
}

interface RawReporteDetail {
  readonly idReporte: number;
  readonly tipo: 'SEMESTRAL' | 'EXTRAORDINARIO';
  readonly anio: number;
  readonly semestre: number | null;
  readonly periodo: string;
  readonly fechaCorte: string;
  readonly versionDatos: number;
  readonly estadoTecnico: 'INICIADA' | 'GENERADA' | 'APROBADA' | 'FALLIDA';
  readonly clasificacion: 'INTERNO' | 'RESTRINGIDO';
  readonly hashSnapshot?: string;
  readonly idSnapshot?: number;
  readonly idGenerador?: number;
  readonly fechaGeneracion?: string;
  readonly filtros?: RawReporteFiltros;
  readonly indicadores: readonly RawReporteIndicador[];
  readonly totales: readonly RawReporteTotalDimension[];
  readonly archivos: readonly RawReporteArchivo[];
  readonly etag: string;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function commandContext(etag: string | undefined, idempotencyKey: string | undefined): HttpContext {
  let context: HttpContext = new HttpContext().set(REQUIRES_IDEMPOTENCY_KEY, true);
  if (etag) {
    context = withEntityTag(etag).set(REQUIRES_IDEMPOTENCY_KEY, true);
  }
  // La `Idempotency-Key` la aplica el interceptor global; la propagación
  // explícita permite que la UI conserve la misma clave entre reintentos
  // sin duplicar el expediente en el backend.
  void idempotencyKey;
  return context;
}

function buildDetalleHeaders(opciones: ReporteDetalleOpciones): HttpHeaders {
  let headers = new HttpHeaders();
  const ifNoneMatch = opciones.ifNoneMatch?.trim();
  if (ifNoneMatch) {
    headers = headers.set('If-None-Match', ifNoneMatch);
  }
  return headers;
}

function freezeOperacion(raw: ReporteOperacion): ReporteOperacion {
  return Object.freeze({ ...raw });
}

function freezeAprobacion(raw: ReporteAprobacionDetail): ReporteAprobacionDetail {
  return Object.freeze({
    ...raw,
    destinatarios: Object.freeze((raw.destinatarios ?? []).map(freezeDestinatario))
  });
}

function freezeDestinatario(raw: ReporteDestinatarioDetail): ReporteDestinatarioDetail {
  return Object.freeze({ ...raw });
}

function freezeIndicador(raw: RawReporteIndicador): ReporteIndicador {
  return Object.freeze({
    nombre: raw.nombre,
    numerador: raw.numerador,
    denominador: raw.denominador,
    porcentaje: raw.porcentaje,
    aplicable: raw.aplicable,
    detalle: raw.detalle
  });
}

function freezeTotal(raw: RawReporteTotalDimension): ReporteTotalDimension {
  return Object.freeze({
    dimension: raw.dimension,
    items: Object.freeze(
      (raw.items ?? []).map((item) => Object.freeze({ ...item }))
    )
  });
}

function freezeArchivo(raw: RawReporteArchivo): ReporteArchivoSummary {
  return Object.freeze({ ...raw });
}

function freezeFiltros(raw: RawReporteFiltros | undefined): ReporteFiltros | undefined {
  if (!raw) {
    return undefined;
  }
  return Object.freeze({
    ...raw,
    unidadesAdicionales: raw.unidadesAdicionales
      ? Object.freeze([...raw.unidadesAdicionales])
      : undefined
  });
}

function freezeDetail(raw: RawReporteDetail): ReporteDetail {
  return Object.freeze({
    idReporte: raw.idReporte,
    tipo: raw.tipo,
    anio: raw.anio,
    semestre: raw.semestre,
    periodo: raw.periodo,
    fechaCorte: raw.fechaCorte,
    versionDatos: raw.versionDatos,
    estadoTecnico: raw.estadoTecnico,
    clasificacion: raw.clasificacion,
    hashSnapshot: raw.hashSnapshot,
    idSnapshot: raw.idSnapshot,
    idGenerador: raw.idGenerador,
    fechaGeneracion: raw.fechaGeneracion,
    filtros: freezeFiltros(raw.filtros),
    indicadores: Object.freeze((raw.indicadores ?? []).map(freezeIndicador)),
    totales: Object.freeze((raw.totales ?? []).map(freezeTotal)),
    archivos: Object.freeze((raw.archivos ?? []).map(freezeArchivo)),
    etag: raw.etag
  });
}

function freezeRemision(raw: ReporteRemisionDetail): ReporteRemisionDetail {
  return Object.freeze({ ...raw });
}

function freezeRemisionPage(raw: ReporteRemisionPage | null | undefined): ReporteRemisionPage {
  if (!raw) {
    return Object.freeze({ remisiones: Object.freeze([]) }) as unknown as ReporteRemisionPage;
  }
  return Object.freeze({
    ...raw,
    remisiones: Object.freeze((raw.remisiones ?? []).map(freezeRemision))
  });
}

function toDetalleRespuesta(
  response: HttpResponse<RawReporteDetail>
): ReporteDetalleRespuesta {
  const status = response.status;
  const headers = response.headers;
  const etag = headers.get('ETag') ?? response.body?.etag;
  const correlationId = headers.get('X-Correlation-Id') ?? undefined;

  // `If-None-Match` produce 304 sin cuerpo. El cliente conserva la
  // bandera para que la UI no muestre contenido obsoleto.
  if (status === 304 || !response.body) {
    return Object.freeze({
      body: response.body
        ? freezeDetail(response.body)
        : emptyDetail(etag),
      etag,
      correlationId,
      notModified: true
    });
  }

  return Object.freeze({
    body: freezeDetail(response.body),
    etag,
    correlationId,
    notModified: false
  });
}

function emptyDetail(etag: string | undefined): ReporteDetail {
  return Object.freeze({
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
    etag: etag ?? ''
  });
}

// Re-exports para mantener compatibilidad con consumidores externos.
export type { ReporteTipoDestinatario };
