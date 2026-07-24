// Tipos DTO del cliente institucional de reportes (US8 - T109).
//
// Estos tipos son la proyección cliente del snapshot OpenAPI codigo-first
// aprobado en `specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml`
// para los endpoints del módulo `reportes`:
//
//   * `POST /api/v1/reportes/semestrales/generaciones`
//   * `POST /api/v1/reportes/extraordinarios/generaciones`
//   * `GET  /api/v1/reportes/generaciones/{id}`
//   * `GET  /api/v1/reportes/generaciones/{id}/archivos/{formato}`
//   * `POST /api/v1/reportes/generaciones/{id}/aprobaciones-remision`
//   * `GET  /api/v1/reportes/generaciones/{id}/destinatarios`
//   * `GET  /api/v1/reportes/generaciones/{id}/remisiones`
//   * `POST /api/v1/reportes/generaciones/{id}/remisiones`
//
// Reglas aplicadas:
//   * DTOs inmutables (`readonly`) y proyectados en `Object.freeze` por el
//     cliente (`ReportesApiService`). El frontend nunca muta los datos que
//     recibió del backend.
//   * La clasificación (`INTERNO` por defecto, `RESTRINGIDO` cuando algún
//     dato del snapshot lo es) se respeta como dato de presentación: el
//     cliente NO decide si el reporte es público, intermedio o restringido.
//   * `indicadores` mantiene `porcentaje` nulo cuando `denominador === 0`
//     para representar la regla BR-122 (denominador cero) sin filtrar al
//     backend. La UI no divide; refleja el campo tal cual lo devolvió el
//     servidor.
//   * `archivos` solo enumera metadatos: `idArchivo`, `formato`, `version`,
//     `hashSha256` y puntero a la versión documental. La descarga se
//     solicita por `GET .../archivos/{PDF|XLSX}` con ETag del snapshot.
//   * Sin disposición ni eliminación: la Constitución veta la eliminación
//     automática mientras no exista tabla de retención aprobada. El cliente
//     no expone acciones de purga.
//
// Los componentes que consumen estos tipos NO deciden reglas de negocio.
// El backend es la autoridad efectiva: cortes, clasificación, destinatarios
// y resultado de remisión se calculan en el servidor y la UI solo los
// refleja.

// ---------------------------------------------------------------------------
// Tipos canónicos del reporte
// ---------------------------------------------------------------------------

/** Tipos de reporte institucional admitidos en la Fase 1. */
export type ReporteTipo = 'SEMESTRAL' | 'EXTRAORDINARIO';

/** Estados técnicos del reporte. La transición la controla el backend. */
export type ReporteEstadoTecnico = 'INICIADA' | 'GENERADA' | 'APROBADA' | 'FALLIDA';

/**
 * Clasificación del reporte (BR-121/BR-122). `INTERNO` por defecto; el
 * servidor promueve a `RESTRINGIDO` cuando el snapshot contiene un dato
 * restringido. La UI no reclasifica.
 */
export type ReporteClasificacion = 'INTERNO' | 'RESTRINGIDO';

/** Formatos oficiales emitidos desde un mismo snapshot. */
export type ReporteFormatoArchivo = 'PDF' | 'XLSX';

/** Tipos de destinatario admitidos por la aprobación y remisión. */
export type ReporteTipoDestinatario =
  | 'AUTORIDAD_MIDAGRI'
  | 'OFICINA_MODERNIZACION'
  | 'PCM_SGP';

/** Resultados posibles de la remisión manual. */
export type ReporteResultadoRemision = 'EXITOSA' | 'FALLIDA' | 'PENDIENTE';

// ---------------------------------------------------------------------------
// Indicador BR-122 (denominador cero es no aplicable)
// ---------------------------------------------------------------------------

/**
 * Indicador BR-122 devuelto por el backend. Cuando el denominador es cero
 * el servidor marca `aplicable: false` y deja `porcentaje` en `null`; la UI
 * representa el estado como "no aplicable" sin calcular divisiones.
 */
export interface ReporteIndicador {
  readonly nombre: string;
  readonly numerador: number;
  readonly denominador: number;
  readonly porcentaje: number | null;
  readonly aplicable: boolean;
  readonly detalle?: string;
}

// ---------------------------------------------------------------------------
// Totales BR-121
// ---------------------------------------------------------------------------

/** Ítem de un total agrupado por dimensión. */
export interface ReporteTotalDimensionItem {
  readonly clave: string;
  readonly etiqueta: string;
  readonly total: number;
}

/** Total BR-121 agrupado por una dimensión canónica. */
export interface ReporteTotalDimension {
  readonly dimension: string;
  readonly items: readonly ReporteTotalDimensionItem[];
}

// ---------------------------------------------------------------------------
// Archivos PDF / XLSX
// ---------------------------------------------------------------------------

/**
 * Resumen de un archivo emitido desde el snapshot. El cliente solo conoce
 * los metadatos: el contenido se descarga por la ruta
 * `GET /api/v1/reportes/generaciones/{id}/archivos/{PDF|XLSX}`.
 */
export interface ReporteArchivoSummary {
  readonly idArchivo: number;
  readonly formato: ReporteFormatoArchivo;
  readonly version: number;
  readonly hashSha256: string;
  readonly idDocumentoVersion?: number;
  readonly creadoPor?: string;
  readonly fechaCreacion?: string;
}

// ---------------------------------------------------------------------------
// Filtros BR-123
// ---------------------------------------------------------------------------

/**
 * Filtros canónicos del reporte (BR-123). Los filtros NO amplían el ámbito
 * del generador: el backend los aplica sobre la vista efectiva del actor
 * y rechaza los que exceden su unidad.
 */
export interface ReporteFiltros {
  readonly tipo?: string;
  readonly estado?: string;
  readonly unidadId?: number;
  readonly responsableId?: number;
  readonly fuente?: string;
  readonly tipoSolucion?: string;
  readonly producto?: string;
  readonly unidadesAdicionales?: readonly string[];
}

// ---------------------------------------------------------------------------
// Solicitudes
// ---------------------------------------------------------------------------

/** Solicitud del reporte semestral. El servidor deriva el corte. */
export interface ReporteSemestralRequest {
  readonly anio: number;
  readonly semestre: 1 | 2;
}

/** Solicitud del reporte extraordinario (BR-120). */
export interface ReporteExtraordinarioRequest {
  readonly solicitudDocumentoId: number;
  readonly aprobacionOficinaDocumentoId: number;
  readonly periodo: string;
  readonly fechaCorte: string;
  readonly filtros: ReporteFiltros;
}

/** Destinatario propuesto por la Oficina de Modernización (BR-125). */
export interface ReporteDestinatarioRequest {
  readonly tipoDestinatario: ReporteTipoDestinatario;
  readonly idEntidad: number;
  readonly nombre: string;
}

/** Aprobación formal (BR-127) de la versión exacta. */
export interface ReporteAprobacionRequest {
  readonly idVersion: number;
  readonly idDocumentoAprobacion: number;
  readonly destinatarios: readonly ReporteDestinatarioRequest[];
}

/** Remisión manual recuperable (BR-128). `motivo` obligatorio para `FALLIDA`. */
export interface ReporteRemisionRequest {
  readonly idVersion: number;
  readonly destinatariosIds: readonly number[];
  readonly resultado: ReporteResultadoRemision;
  readonly motivo?: string;
}

// ---------------------------------------------------------------------------
// Respuestas
// ---------------------------------------------------------------------------

/** Resultado `202` de una generación. La operación es asíncrona. */
export interface ReporteOperacion {
  readonly reporteId: number;
  readonly operacionId: string;
  readonly corte: string;
  readonly versionDatos: number;
  readonly estadoTecnico: ReporteEstadoTecnico;
  readonly clasificacion?: ReporteClasificacion;
  readonly hashSnapshot?: string;
  readonly fechaGeneracion?: string;
}

/** Destinatario registrado en una aprobación. */
export interface ReporteDestinatarioDetail {
  readonly idDestinatario: number;
  readonly idAprobacion: number;
  readonly tipoDestinatario: ReporteTipoDestinatario;
  readonly idEntidad: number;
  readonly nombre: string;
}

/** Detalle de aprobación registrada (respuesta 201). */
export interface ReporteAprobacionDetail {
  readonly idAprobacion: number;
  readonly idReporte: number;
  readonly idVersion: number;
  readonly idOficina: number;
  readonly idAprobador: number;
  readonly idDocumentoAprobacion: number;
  readonly fechaAprobacion: string;
  readonly destinatarios: readonly ReporteDestinatarioDetail[];
}

/** Detalle de una remisión individual. */
export interface ReporteRemisionDetail {
  readonly idRemision: number;
  readonly idReporte: number;
  readonly idDestinatario: number;
  readonly resultado: ReporteResultadoRemision;
  readonly motivo?: string;
  readonly fechaRemision: string;
}

/** Página de historial de remisiones (filtro opcional por versión). */
export interface ReporteRemisionPage {
  readonly idReporte: number;
  readonly idVersion?: number;
  readonly remisiones: readonly ReporteRemisionDetail[];
}

/**
 * Detalle completo del reporte institucional. Incluye la ETag agregada
 * para control de concurrencia (`If-Match`/`If-None-Match`) en operaciones
 * posteriores.
 */
export interface ReporteDetail {
  readonly idReporte: number;
  readonly tipo: ReporteTipo;
  readonly anio: number;
  readonly semestre: number | null;
  readonly periodo: string;
  readonly fechaCorte: string;
  readonly versionDatos: number;
  readonly estadoTecnico: ReporteEstadoTecnico;
  readonly clasificacion: ReporteClasificacion;
  readonly hashSnapshot?: string;
  readonly idSnapshot?: number;
  readonly idGenerador?: number;
  readonly fechaGeneracion?: string;
  readonly filtros?: ReporteFiltros;
  readonly indicadores: readonly ReporteIndicador[];
  readonly totales: readonly ReporteTotalDimension[];
  readonly archivos: readonly ReporteArchivoSummary[];
  readonly etag: string;
}

// ---------------------------------------------------------------------------
// Opciones de comandos y consultas
// ---------------------------------------------------------------------------

/** Opciones comunes a comandos mutables. */
export interface ReporteComandoOpciones {
  /**
   * ETag de la lectura previa. Se envía como `If-Match` para evitar
   * condiciones de carrera con aprobaciones o remisiones concurrentes.
   * La operación se rechaza con 412 si la ETag no coincide.
   */
  readonly etag?: string;
}

/** Opciones de la consulta de detalle. */
export interface ReporteDetalleOpciones {
  /**
   * ETag de la última lectura. Si se envía y el detalle no cambió, el
   * backend responde 304 sin cuerpo; el cliente conserva `notModified`.
   */
  readonly ifNoneMatch?: string;
}

/** Opciones para forzar la propagación explícita de la `Idempotency-Key`. */
export interface ReporteIdempotencyOpciones {
  /**
   * `Idempotency-Key` ya generada por la UI (por ejemplo, conservada
   * tras un reintento). Si no se envía, el interceptor global la crea.
   */
  readonly idempotencyKey?: string;
}

/** Forma de la respuesta `HttpResponse` para el detalle del reporte. */
export interface ReporteDetalleRespuesta {
  readonly body: ReporteDetail;
  readonly etag?: string;
  readonly correlationId?: string;
  readonly notModified: boolean;
}
