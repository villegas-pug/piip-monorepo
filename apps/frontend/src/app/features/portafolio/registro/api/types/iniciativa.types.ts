// Tipos específicos de Iniciativa. Reflejan CreateInitiativeRequest, InitiativeDetail y
// sus componentes del snapshot codigo-first de PIIP.
// Referencia contractual: specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml
// Los campos 1 al 13, 22 y 23 de la matriz oficial se modelan aquí. El cliente nunca
// infiere ni decide: el backend es la autoridad efectiva de obligatoriedad y cardinalidad.

import { EstadoIniciativa, FuenteOrigen, TipoRegistro, TipoSolucion } from './common.types';

/** Unidad responsable presentada en `CreateInitiativeRequest.unidades`. */
export interface UnidadResponsableItem {
  readonly unidadId: number;
  readonly principal: boolean;
}

/** Participante persona. `personaId` se omite cuando el participante aún no tiene cuenta PIIP. */
export interface ParticipantePersonaItem {
  readonly personaId?: number;
  readonly nombresCompletos: string;
  readonly institucion?: string;
  readonly funcion?: string;
}

/** Participante unidad (vinculación de equipo, no persona). */
export interface ParticipanteUnidadItem {
  readonly unidadId: number;
}

/** Unidad responsable devuelta en `InitiativeDetail.unidades` (contexto descriptivo, no autorización). */
export interface UnidadResponsableDetail {
  readonly id?: number;
  readonly unidadId: number;
  readonly descripcion?: string;
  readonly abreviatura?: string;
  readonly principal: boolean;
}

/** Solicitud para presentar una iniciativa. Cubre los campos oficiales 5 a 12, 22 y 23. */
export interface CreateInitiativeRequest {
  readonly nombre: string;
  readonly tipoSolucion: TipoSolucion;
  readonly fuenteOrigen: FuenteOrigen;
  readonly detalleFuente?: string;
  readonly problemaPublico: string;
  readonly solucionPropuesta?: string;
  readonly responsableId: number;
  readonly objetivoPeiId: number;
  readonly actividadPoiId: number;
  readonly unidades: readonly UnidadResponsableItem[];
  readonly participantesPersona?: readonly ParticipantePersonaItem[];
  readonly participantesUnidad?: readonly ParticipanteUnidadItem[];
  readonly componenteDigital: boolean;
  readonly detalleComponenteDigital?: string;
  readonly nota?: string;
  readonly fichaDocumentoVersionId: number;
}

/** Detalle de iniciativa. El cliente no genera `codigo`, `fechaInicio`, `estado`, `version` ni `etag`. */
export interface InitiativeDetail {
  readonly id: number;
  readonly tipoRegistro: TipoRegistro;
  readonly codigo: string;
  readonly codigoOrigen?: string;
  readonly fechaInicio: string;
  readonly nombre?: string;
  readonly tipoSolucion?: TipoSolucion;
  readonly fuenteOrigen?: FuenteOrigen;
  readonly detalleFuente?: string;
  readonly responsableId?: number;
  readonly problemaPublico?: string;
  readonly solucionPropuesta?: string;
  readonly objetivoPeiId?: number;
  readonly actividadPoiId?: number;
  readonly unidades?: readonly UnidadResponsableDetail[];
  readonly estado: EstadoIniciativa;
  readonly componenteDigital?: boolean;
  readonly detalleComponenteDigital?: string;
  readonly nota?: string;
  readonly version: number;
  readonly etag: string;
  readonly fechaCreacion?: string;
}
