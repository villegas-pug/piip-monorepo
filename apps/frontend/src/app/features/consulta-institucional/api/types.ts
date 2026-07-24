// Tipos DTO del cliente institucional de consulta del portafolio (US7 - T101).
//
// Estos tipos son la proyección cliente del snapshot OpenAPI codigo-first
// aprobado en `specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml`
// para los endpoints:
//   * `GET /api/v1/consulta/institucional/portafolio`
//   * `GET /api/v1/consulta/institucional/portafolio/{id}`
//
// Son DTOs propios del módulo `consulta-institucional`. NO se reutilizan
// con el cliente público: el cliente público (`consulta-publica/api/types.ts`)
// expone únicamente la allowlist canónica (`tipoRegistro`, `codigo`, `nombre`,
// `estado` y metadatos públicos de publicaciones) y nunca Responsable,
// clasificación, contenido ni historial. Mezclar tipos o constantes entre
// ambos clientes expondría accidentalmente datos restringidos a la consulta
// anónima; por eso los DTOs viven en archivos separados y no comparten
// tipos, enums ni esquemas.
//
// Reglas de privacidad aplicadas en el cliente:
//   * `responsableId` solo se incluye si `puedeVerResponsable` es `true`;
//     el backend aplica la matriz de privacidad y el cliente nunca debe
//     sobreescribir esa decisión.
//   * `participantes` se proyectan únicamente cuando el actor es
//     Responsable del registro, Evaluador o administrador autorizado.
//   * `documentos` solo contiene metadatos; el contenido se obtiene por
//     `GET /api/v1/documentos/{id}/contenido` con revalidación de ámbito
//     y clasificación.
//   * `InstitutionalPortfolioDocument.etag` y `InstitutionalPortfolioDetail.etag`
//     se conservan para soportar `If-Match` e `If-None-Match` en comandos
//     y lecturas subsecuentes.

export type InstitutionalTipoRegistro = 'INICIATIVA' | 'PROYECTO';

export type InstitutionalTipoSolucion = 'POTENCIAL_ADAPTABLE' | 'POR_DEFINIR';

export type InstitutionalFuenteOrigen =
  | 'FICHA_INICIATIVA'
  | 'CONCURSO_INTERNO'
  | 'INNOVACION_ABIERTA'
  | 'PROPUESTA_JEFATURA'
  | 'OTROS';

export type InstitutionalEstadoIniciativa =
  | 'PRESENTADO'
  | 'NO_ADMISIBLE'
  | 'NO_APLICABLE'
  | 'INICIATIVA_APROBADA'
  | 'INICIATIVA_ARCHIVADA'
  | 'PROYECTO_EJECUCION'
  | 'SUSPENDIDO'
  | 'CANCELADO'
  | 'PRODUCTO_APROBADO'
  | 'PRODUCTO_NO_APROBADO'
  | 'FINALIZADO';

export type InstitutionalClasificacionDocumento = 'PUBLICO' | 'INTERNO' | 'RESTRINGIDO';

/** Unidad organizacional responsable del registro dentro del ámbito autorizado. */
export interface InstitutionalPortfolioUnidad {
  readonly id?: number;
  readonly unidadId: number;
  readonly descripcion?: string;
  readonly abreviatura?: string;
  readonly nroOrden?: number;
  readonly principal: boolean;
}

/**
 * Participante persona del registro institucional.
 *
 * El backend solo lo incluye cuando el actor es Responsable del registro,
 * Evaluador o administrador autorizado; el cliente lo refleja sin aplicar
 * ninguna decisión de privacidad.
 */
export interface InstitutionalPortfolioPersonaParticipante {
  readonly idParticipacion?: number;
  readonly participanteId?: number;
  readonly usuarioId?: number;
  readonly nombresCompletos: string;
  readonly institucion?: string;
  readonly funcion?: string;
  readonly clasificacion?: InstitutionalClasificacionDocumento;
  readonly fechaInicio?: string;
  readonly fechaFin?: string;
  readonly vigente: boolean;
}

/**
 * Metadatos documentales visibles para la consulta institucional autorizada.
 *
 * Nunca expone BLOB, clave física ni URL directa. El contenido se obtiene
 * por `GET /api/v1/documentos/{id}/contenido` con revalidación obligatoria
 * de ámbito y clasificación validada.
 */
export interface InstitutionalPortfolioDocument {
  readonly documentoId: number;
  readonly serieId?: number;
  readonly numeroVersion?: number;
  readonly titulo: string;
  readonly formato?: string;
  readonly mimeType?: string;
  readonly tamanoBytes?: number;
  readonly hashSha256?: string;
  readonly clasificacionPropuesta?: InstitutionalClasificacionDocumento;
  readonly clasificacionValidada?: InstitutionalClasificacionDocumento;
  readonly tipoDocumental?: string;
  readonly contextoDocumental?: string;
  readonly publicado: boolean;
  readonly fechaCarga?: string;
  readonly usuarioCargaId?: number;
  readonly puedeConsultarContenido: boolean;
  readonly etag?: string;
}

/** Entrada del historial append-only de transiciones de estado. */
export interface InstitutionalPortfolioHistoryEntry {
  readonly transicionId?: number;
  readonly estadoAnterior?: InstitutionalEstadoIniciativa;
  readonly estadoNuevo: InstitutionalEstadoIniciativa;
  readonly usuarioId?: number;
  readonly rolEfectivoId?: number;
  readonly unidadEfectivaId?: number;
  readonly fechaTransicion: string;
  readonly observaciones?: string;
  readonly documentoRefId?: number;
}

/** Vínculo inmutable entre la iniciativa y su proyecto derivado, cuando exista. */
export interface InstitutionalIniciativaProyectoRelacion {
  readonly iniciativaId?: number;
  readonly proyectoId?: number;
  readonly iniciativaActual?: boolean;
  readonly proyectoActual?: boolean;
}

/**
 * Resumen paginado de la consulta institucional.
 *
 * Refleja `InstitutionalPortfolioSummary` del snapshot OpenAPI. Incluye la
 * ETag agregada de la página para soportar concurrencia optimista con
 * `If-None-Match`.
 */
export interface InstitutionalPortfolioSummary {
  readonly id: number;
  readonly tipoRegistro: InstitutionalTipoRegistro;
  readonly codigo: string;
  readonly codigoOrigen?: string;
  readonly nombre: string;
  readonly estado: InstitutionalEstadoIniciativa;
  readonly fechaInicio: string;
  readonly unidadEjecutoraId?: number;
  readonly unidadEjecutoraDescripcion?: string;
  readonly unidadEjecutoraAbreviatura?: string;
  readonly responsableId?: number;
  readonly puedeVerResponsable: boolean;
  readonly version: number;
  readonly etag: string;
}

/**
 * Detalle institucional completo de un registro del portafolio.
 *
 * DTO propio del módulo `consulta`: NO expone entidades JPA, BLOB, clave
 * física ni URL directa. La colección de documentos solo contiene
 * metadatos; el contenido se obtiene por `/api/v1/documentos/{id}/contenido`.
 */
export interface InstitutionalPortfolioDetail {
  readonly id: number;
  readonly tipoRegistro: InstitutionalTipoRegistro;
  readonly codigo: string;
  readonly codigoOrigen?: string;
  readonly fechaInicio: string;
  readonly fechaCierre?: string;
  readonly nombre: string;
  readonly tipoSolucion?: InstitutionalTipoSolucion;
  readonly fuenteOrigen?: InstitutionalFuenteOrigen;
  readonly detalleFuente?: string;
  readonly responsableId?: number;
  readonly problemaPublico?: string;
  readonly solucionPropuesta?: string;
  readonly objetivoPeiId?: number;
  readonly actividadPoiId?: number;
  readonly unidadEjecutoraId?: number;
  readonly unidadEjecutoraDescripcion?: string;
  readonly unidadEjecutoraAbreviatura?: string;
  readonly estado: InstitutionalEstadoIniciativa;
  readonly componenteDigital?: boolean;
  readonly detalleComponenteDigital?: string;
  readonly nota?: string;
  readonly resultadosClave?: string;
  readonly unidades: readonly InstitutionalPortfolioUnidad[];
  readonly participantes: readonly InstitutionalPortfolioPersonaParticipante[];
  readonly documentos: readonly InstitutionalPortfolioDocument[];
  readonly historial: readonly InstitutionalPortfolioHistoryEntry[];
  readonly relacion?: InstitutionalIniciativaProyectoRelacion;
  readonly actorEsResponsable: boolean;
  readonly actorEsEvaluador: boolean;
  readonly actorEsAdministrador: boolean;
  readonly fechaCreacion?: string;
  readonly version: number;
  readonly etag: string;
}

/** Página institucional devuelta por `GET /consulta/institucional/portafolio`. */
export interface InstitutionalPortfolioPage {
  readonly items: readonly InstitutionalPortfolioSummary[];
  readonly pagina: number;
  readonly tamanio: number;
  readonly totalElementos: number;
  readonly totalPaginas: number;
  readonly etag: string;
}

/** Filtros admitidos por la búsqueda institucional. */
export interface InstitutionalPortfolioFilters {
  readonly tipo?: InstitutionalTipoRegistro;
  readonly codigo?: string;
  readonly nombre?: string;
  readonly estado?: InstitutionalEstadoIniciativa;
  readonly unidadId?: number;
  readonly responsableId?: number;
  readonly fechaDesde?: string;
  readonly fechaHasta?: string;
  readonly page?: number;
  readonly size?: number;
  readonly sort?: 'codigo' | 'nombre' | 'estado' | 'fechaInicio';
}

/** Opciones de la consulta institucional de detalle. */
export interface InstitutionalPortfolioDetailOptions {
  /**
   * ETag de la lectura previa. Si se envía, el backend aplica control de
   * concurrencia optimista: `If-Match` exige coincidencia exacta; un
   * `STATE_CHANGED` (412) obliga a refrescar.
   */
  readonly ifMatch?: string;
  /**
   * ETag de la última lectura. Si se envía, el backend responde 304 sin
   * cuerpo cuando el detalle no ha cambiado.
   */
  readonly ifNoneMatch?: string;
}

/**
 * Resultado crudo de la respuesta HTTP de detalle institucional, con la
 * ETag recibida en la cabecera. Permite al componente conservar la ETag
 * para `If-Match`/`If-None-Match` en operaciones posteriores.
 */
export interface InstitutionalPortfolioDetailResponse {
  readonly body: InstitutionalPortfolioDetail;
  readonly etag?: string;
  readonly correlationId?: string;
  readonly notModified: boolean;
}
