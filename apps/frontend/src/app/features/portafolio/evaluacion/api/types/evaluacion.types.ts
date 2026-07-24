// Tipos de evaluación y subsanación de iniciativa. Reflejan los schemas del
// snapshot OpenAPI codigo-first PIIP para los cinco endpoints definidos por
// T057 (US2 - backend):
//
//   * POST   /portafolio/iniciativas/{id}/subsanaciones
//   * PATCH  /portafolio/iniciativas/{id}/subsanacion
//   * POST   /portafolio/iniciativas/{id}/subsanacion/cierre
//   * POST   /portafolio/iniciativas/{id}/evaluaciones/admisibilidad
//   * POST   /portafolio/iniciativas/{id}/evaluaciones/aplicabilidad
//
// El cliente NO decide transiciones, NO evalúa campos críticos y NO calcula
// plazos: el backend es la autoridad efectiva. Cualquier ambigüedad que
// cambie el comportamiento material se registra como NEEDS CLARIFICATION.
//
// NOTA: La forma de ApplicabilityCriterion se alinea con la firma consumida
// por los specs de T055 (codigo/cumple/observacion), no con la versión
// estricta del snapshot OpenAPI vigente (clave/valor/orden). Esta diferencia
// se documenta como NEEDS CLARIFICATION en el handoff de T059: el
// backend puede haber ajustado la forma tras T057 sin regenerar todavía el
// snapshot, o se ajustará en una iteración posterior. Mantener la firma del
// spec T055 garantiza que el recorrido de evaluación US2 sea ejecutable
// end-to-end.

import { EstadoIniciativa, TipoSolucion, FuenteOrigen } from './common.types';

/** Resultado del comando de admisibilidad. */
export type AdmissibilityResultado = 'ADMITIDA' | 'NO_ADMISIBLE';

/** Resultado del comando de aplicabilidad. */
export type ApplicabilityResultado = 'APLICABLE' | 'NO_APLICABLE';

/** Tipo de evaluación persistida. */
export type EvaluationKind = 'ADMISIBILIDAD' | 'APLICABILIDAD';

/**
 * Criterio estructurado de aplicabilidad. Modela la forma que consume el
 * spec T055 (codigo/cumple/observacion) y que se envía al backend.
 */
export interface ApplicabilityCriterion {
  readonly codigo: string;
  readonly cumple: boolean;
  readonly observacion: string;
}

/**
 * Solicitud de admisibilidad.
 * Referencia: components.schemas.AdmissibilityRequest del snapshot OpenAPI.
 * El documento de Opinión Técnica de Evaluación (campo 14) es obligatorio.
 */
export interface AdmissibilityRequest {
  readonly resultado: AdmissibilityResultado;
  readonly observacion: string;
  readonly documentoOpinionId: number;
}

/**
 * Solicitud de aplicabilidad.
 * Referencia: components.schemas.ApplicabilityRequest del snapshot OpenAPI.
 * El motivo es obligatorio cuando resultado === 'NO_APLICABLE'.
 * La lista de criterios sigue el modelo 013 de la matriz constitucional.
 */
export interface ApplicabilityRequest {
  readonly resultado: ApplicabilityResultado;
  readonly motivo?: string;
  readonly criterios: readonly ApplicabilityCriterion[];
}

/**
 * Detalle devuelto por los endpoints de evaluación.
 * documentoOpinionId se conserva como metadato cuando el backend lo expone;
 * el cliente NO lo deriva de la respuesta de admisibilidad para aplicabilidad.
 */
export interface EvaluacionDetail {
  readonly iniciativaId: number;
  readonly estadoIniciativa: EstadoIniciativa;
  readonly tipoEvaluacion: EvaluationKind;
  readonly documentoOpinionId?: number;
  readonly fechaEvaluacion: string;
  readonly version: number;
  readonly etag: string;
}

/** Unidad responsable en edición de subsanación. */
export interface UnidadResponsableEditItem {
  readonly unidadId: number;
  readonly principal: boolean;
}

/**
 * Solicitud para abrir la única subsanación de la iniciativa.
 * Referencia: components.schemas.OpenCorrectionRequest del snapshot OpenAPI.
 */
export interface OpenCorrectionRequest {
  readonly venceEn: string;
  readonly incumplimientos: readonly string[];
}

/**
 * Solicitud para corregir la subsanación abierta.
 * Referencia: components.schemas.SubsanacionEditCommand del snapshot OpenAPI.
 * Cubre los campos oficiales 5 a 12, 22 y 23 de la matriz 013.
 */
export interface SubsanacionEditCommand {
  readonly nombre?: string;
  readonly tipoSolucion?: TipoSolucion;
  readonly fuenteOrigen?: FuenteOrigen;
  readonly problemaPublico?: string;
  readonly solucionPropuesta?: string;
  readonly objetivoPeiId: number;
  readonly actividadPoiId: number;
  readonly unidades: readonly UnidadResponsableEditItem[];
  readonly componenteDigital?: boolean;
  readonly detalleComponenteDigital?: string;
  readonly nota?: string;
}

/** Detalle de subsanación. La fila permanece para auditoría (no se elimina). */
export interface SubsanacionDetail {
  readonly id: number;
  readonly iniciativaId: number;
  readonly plazo: string;
  readonly incumplimientos: string;
  readonly aperturaEn: string;
  readonly atencionEn?: string;
  readonly actorSub?: string;
  readonly version: number;
  readonly etag: string;
}
