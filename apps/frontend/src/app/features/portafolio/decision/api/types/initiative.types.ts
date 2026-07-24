// Tipos del contexto de iniciativa o proyecto que consume la página de
// decisión formal y la página de cancelación de proyecto. Modelan la forma
// mínima devuelta por GET /api/v1/portafolio/iniciativas/{id} para que la
// vista de decisión pueda consultar y enviar la transición con If-Match.
//
// El cliente NO genera código, NO altera el estado y NO calcula versión:
// el backend es la autoridad efectiva y tag se devuelve siempre.

import { EstadoIniciativa, TipoSolucion } from './common.types';

/** Unidad responsable simplificada para vistas de decisión y cancelación. */
export interface UnidadResponsableResumen {
  readonly unidadId: number;
  readonly principal: boolean;
}

/**
 * Contexto de iniciativa para la página de decisión formal.
 * La iniciativa debe estar en PRESENTADO para permitir la decisión.
 */
export interface InitiativeDecisionContext {
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

/**
 * Contexto de proyecto para la página de cancelación.
 * El proyecto debe estar en PROYECTO_EJECUCION para permitir la cancelación.
 */
export interface ProjectCancellationContext {
  readonly id: number;
  readonly codigo: string;
  readonly estado: EstadoIniciativa;
  readonly nombre?: string;
  readonly tipoSolucion?: TipoSolucion;
  readonly unidades: readonly UnidadResponsableResumen[];
  readonly responsableId?: number;
  readonly version: number;
  readonly etag: string;
  readonly iniciativaOrigenId?: number;
}
