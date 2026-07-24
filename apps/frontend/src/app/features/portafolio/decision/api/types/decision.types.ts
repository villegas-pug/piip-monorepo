// Tipos de decisión formal de iniciativa y cancelación de proyecto. Reflejan
// los schemas del snapshot OpenAPI codigo-first PIIP:
//
//   * POST /api/v1/portafolio/transiciones/{id} (T058 - US2 backend)
//   * GET  /api/v1/portafolio/iniciativas/{id} (consulta reutilizada)
//
// El cliente NO decide transiciones, NO evalúa documentos ni calcula plazos:
// el backend es la autoridad efectiva. Las reglas formales de obligatoriedad
// (documento formal, observación obligatoria para archivado, etc.) se
// modelan solo en el lado de validación de formularios; la autoridad
// definitiva reside en el backend.

import { EstadoIniciativa } from './common.types';

/** Destinos canónicos desde PRESENTADO (US2). */
export type DecisionDestino = 'INICIATIVA_APROBADA' | 'INICIATIVA_ARCHIVADA';

/** Destinos canónicos desde PROYECTO_EJECUCION (cancelación). */
export type CancellationDestino = 'CANCELADO';

/**
 * Comando para la transición formal de decisión de iniciativa.
 * Reutiliza el TransicionCommand del snapshot OpenAPI con un discriminador
 * semántico del lado del cliente. El backend sigue siendo la autoridad
 * efectiva del cambio de estado.
 */
export interface DecisionTransitionCommand {
  readonly destino: DecisionDestino;
  readonly documentoRefId: number;
  readonly observaciones?: string;
}

/** Comando para cancelación de proyecto en ejecución. */
export interface ProjectCancellationCommand {
  readonly destino: CancellationDestino;
  readonly documentoRefId: number;
  readonly observaciones: string;
}

/**
 * Comando genérico para POST /api/v1/portafolio/transiciones/{id}.
 * Refleja components.schemas.TransicionCommand del snapshot OpenAPI.
 * destino se modela como union del EstadoIniciativa y se reduce a
 * DecisionDestino o CancellationDestino en los formularios.
 */
export interface TransicionCommand {
  readonly destino: EstadoIniciativa;
  readonly observaciones?: string;
  readonly documentoRefId?: number;
  readonly ifMatch?: string;
}

/** Detalle devuelto por la transición. Refleja components.schemas.TransicionDetail. */
export interface TransicionDetail {
  readonly registroId: number;
  readonly estadoAnterior: EstadoIniciativa;
  readonly estadoNuevo: EstadoIniciativa;
  readonly transicionId: number;
  readonly fechaTransicion: string;
  readonly actorSub?: string;
  readonly version: number;
  readonly etag: string;
}
