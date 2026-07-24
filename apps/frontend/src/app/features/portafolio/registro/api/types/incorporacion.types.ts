// Tipos específicos de Incorporación individual. Reflejan los schemas del snapshot
// codigo-first de PIIP. El cliente no resuelve conflictos, no transiciona estados y
// no decide; el backend es la autoridad efectiva.

import { EstadoIncorporacion, EstadoIniciativa, TipoConflicto } from './common.types';

/** Solicitud para registrar una incorporación individual. */
export interface CreateIncorporacionRequest {
  readonly fuente: string;
  readonly fechaFuente: string;
  readonly responsableId: number;
  readonly documentoFuenteId: number;
  readonly hashOriginal: string;
  readonly datosOriginales?: string;
  readonly codigoHeredado?: string;
}

/** Detalle de incorporación devuelto por el backend. */
export interface IncorporacionDetail {
  readonly id: number;
  readonly fuente: string;
  readonly fechaFuente?: string;
  readonly responsableId?: number;
  readonly documentoFuenteId?: number;
  readonly hashOriginal: string;
  readonly estado: EstadoIncorporacion;
  readonly registroVinculadoId?: number;
  readonly observacion?: string;
  readonly creadoPor?: string;
  readonly fechaCreacion?: string;
  readonly version: number;
  readonly etag: string;
}

/** Solicitud de corrección append-only de una incorporación. */
export interface IncorporacionCorreccionRequest {
  readonly incorporacionId: number;
  readonly datosNuevos: string;
  readonly motivo: string;
}

/** Solicitud de validación o rechazo por el Evaluador. */
export interface IncorporacionValidacionRequest {
  readonly incorporacionId: number;
  readonly estadoCanonico: EstadoIniciativa;
  readonly registroVinculadoId?: number;
  readonly observacion?: string;
}

/** Solicitud de resolución de un conflicto de incorporación. */
export interface IncorporacionResolucionConflictoRequest {
  readonly conflictoId: number;
  readonly incorporacionId: number;
  readonly resolucion: string;
  readonly documentoResolucionId?: number;
}

/** Forma resumida de un conflicto abierto (derivado de ProblemDetail.violations). */
export interface IncorporacionConflictoResumen {
  readonly id: number;
  readonly tipo: TipoConflicto;
  readonly descripcion: string;
  readonly resolucionDocumentoId?: number;
}
