// Tipos compartidos extraídos del snapshot OpenAPI codigo-first de PIIP.
// Referencia contractual: specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml
// No se infieren campos: cualquier ambigüedad se reporta como NEEDS CLARIFICATION.

/** Catálogo de tipo de registro (canónico constitucional). */
export type TipoRegistro = 'INICIATIVA' | 'PROYECTO';

/** Catálogo de tipo de solución. `POR_DEFINIR` puede conservarse sin plazo. */
export type TipoSolucion = 'POTENCIAL_ADAPTABLE' | 'POR_DEFINIR';

/** Catálogo de fuente u origen de la iniciativa. */
export type FuenteOrigen =
  | 'FICHA_INICIATIVA'
  | 'CONCURSO_INTERNO'
  | 'INNOVACION_ABIERTA'
  | 'PROPUESTA_JEFATURA'
  | 'OTROS';

/** Estados de la máquina de iniciativa (canónico constitucional). */
export type EstadoIniciativa =
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

/** Estados de incorporación individual (separados del estado de negocio). */
export type EstadoIncorporacion = 'PENDIENTE' | 'VALIDADO' | 'RECHAZADO';

/** Tipos de conflicto de incorporación individual. */
export type TipoConflicto = 'CODIGO' | 'DUPLICADO' | 'RELACION_INVALIDA';

/** Clasificación de documento (canónico constitucional). */
export type ClasificacionDocumento = 'PUBLICO' | 'INTERNO' | 'RESTRINGIDO';

/** Tipo de propietario de un documento. Excluyente: XOR entre `registroPortafolioId` y `expedienteInstitucionalId`. */
export type DocumentOwnerType = 'PORTAFOLIO' | 'EXPEDIENTE_INSTITUCIONAL';

/** Forma genérica de paginación del snapshot. */
export interface PageResponse<T> {
  readonly items: readonly T[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}
