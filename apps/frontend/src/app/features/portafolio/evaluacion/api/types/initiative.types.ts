// Tipos del contexto de iniciativa que consume la página de evaluación.
//
// Proyectan el detalle de iniciativa devuelto por
// GET /api/v1/portafolio/iniciativas/{id} a una forma mínima y estable que la
// vista de evaluación necesita: código, estado, unidades responsables y ETag para
// control de concurrencia. NO replica la InitiativeDetail completa porque la UI
// solo consulta un subconjunto de campos y porque la máquina de estados
// canónica exige mantener una ETag explícita para If-Match.
//
// El cliente NO genera código, NO altera el estado y NO calcula versión:
// el backend es la autoridad efectiva y la ETag se devuelve siempre.

import { EstadoIniciativa, TipoSolucion } from './common.types';

/** Unidad responsable simplificada para vistas de evaluación. */
export interface UnidadResponsableResumen {
  readonly unidadId: number;
  readonly principal: boolean;
}

/**
 * Contexto de iniciativa para la página de evaluación.
 * El backend es la fuente del codigo, estado, version y etag.
 */
export interface InitiativeEvaluationContext {
  readonly id: number;
  readonly codigo: string;
  readonly estado: EstadoIniciativa;
  readonly nombre?: string;
  readonly tipoSolucion?: TipoSolucion;
  readonly unidades: readonly UnidadResponsableResumen[];
  readonly responsableId?: number;
  readonly version: number;
  readonly etag: string;
}
