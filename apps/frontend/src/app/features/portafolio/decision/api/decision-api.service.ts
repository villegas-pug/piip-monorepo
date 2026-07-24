// Servicio de decisión formal de iniciativa y cancelación de proyecto.
// Consolida:
//
//   * GET  /api/v1/portafolio/iniciativas/{id}   (consulta reutilizada)
//   * POST /api/v1/portafolio/transiciones/{id}  (T058 - US2 backend)
//
// El cliente NO decide transiciones, NO evalúa documentos ni calcula plazos:
// el backend es la autoridad efectiva. La ETag devuelta por
// `consultarIniciativa` se conserva en el cuerpo de la respuesta para que
// las páginas de decisión y cancelación la propaguen como `If-Match` en cada
// comando subsiguiente. La cabecera `Idempotency-Key` la aplica
// automáticamente el `idempotencyKeyInterceptor` global.
//
// El servicio delega la transición a `TransicionApiService` para evitar
// duplicar la lógica HTTP y mantener un único punto de cambio si el
// backend modifica la forma del endpoint. La consulta de iniciativa se
// resuelve contra el mismo endpoint que `RegistroApiService` y
// `EvaluacionApiService` usan, devolviendo una vista mínima adaptada a la
// página de decisión y cancelación. La página de cancelación distingue
// un proyecto de una iniciativa por `estado === 'PROYECTO_EJECUCION'`.

import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import {
  DecisionTransitionCommand,
  EstadoIniciativa,
  InitiativeDecisionContext,
  ProjectCancellationCommand,
  TipoSolucion,
  TransicionCommand,
  TransicionDetail
} from './types';
import { TransicionApiService, TransicionComandoOpciones } from './transicion-api.service';

interface RawUnidadResponsable {
  readonly id?: number;
  readonly unidadId: number;
  readonly descripcion?: string;
  readonly abreviatura?: string;
  readonly principal: boolean;
}

interface RawInitiativeContext {
  readonly id: number;
  readonly tipoRegistro?: 'INICIATIVA' | 'PROYECTO';
  readonly codigo: string;
  readonly codigoOrigen?: string;
  readonly fechaInicio?: string;
  readonly nombre?: string;
  readonly tipoSolucion?: TipoSolucion;
  readonly fuenteOrigen?:
    | 'FICHA_INICIATIVA'
    | 'CONCURSO_INTERNO'
    | 'INNOVACION_ABIERTA'
    | 'PROPUESTA_JEFATURA'
    | 'OTROS';
  readonly detalleFuente?: string;
  readonly responsableId?: number;
  readonly problemaPublico?: string;
  readonly solucionPropuesta?: string;
  readonly objetivoPeiId?: number;
  readonly actividadPoiId?: number;
  readonly unidades?: readonly RawUnidadResponsable[];
  readonly estado: EstadoIniciativa;
  readonly componenteDigital?: boolean;
  readonly detalleComponenteDigital?: string;
  readonly nota?: string;
  readonly version: number;
  readonly etag: string;
  readonly fechaCreacion?: string;
  readonly iniciativaOrigenId?: number;
}

@Injectable({ providedIn: 'root' })
export class DecisionApiService {
  private readonly http = inject(HttpClient);
  private readonly transicion = inject(TransicionApiService);
  private static readonly BASE = '/api/v1/portafolio/iniciativas';

  /**
   * Consulta el detalle de la iniciativa o proyecto. La ETag se devuelve
   * en el cuerpo para propagarla en cada comando subsiguiente con `If-Match`.
   */
  consultarIniciativa(id: number): Observable<InitiativeDecisionContext> {
    return this.http
      .get<RawInitiativeContext>(`${DecisionApiService.BASE}/${id}`)
      .pipe(map((raw) => toContext(raw)));
  }

  /**
   * Confirma la decisión formal de iniciativa. Traduce el `destino` del
   * comando del cliente al `destino` del `TransicionCommand` que espera el
   * backend. El backend sigue siendo la autoridad del cambio de estado.
   */
  transicionar(
    iniciativaId: number,
    payload: DecisionTransitionCommand,
    opciones: TransicionComandoOpciones
  ): Observable<TransicionDetail> {
    const comando: TransicionCommand = {
      destino: payload.destino,
      documentoRefId: payload.documentoRefId,
      observaciones: payload.observaciones
    };
    return this.transicion.transicionar(iniciativaId, comando, opciones);
  }

  /**
   * Confirma la cancelación formal de un proyecto en ejecución
   * (`PROYECTO_EJECUCION` -> `CANCELADO`). El destino se mantiene como
   * discriminador explícito para evidenciar la regla constitucional y
   * permitir que el backend aplique la matriz 013 específica.
   */
  cancelarProyecto(
    proyectoId: number,
    payload: ProjectCancellationCommand,
    opciones: TransicionComandoOpciones
  ): Observable<TransicionDetail> {
    const comando: TransicionCommand = {
      destino: payload.destino,
      documentoRefId: payload.documentoRefId,
      observaciones: payload.observaciones
    };
    return this.transicion.transicionar(proyectoId, comando, opciones);
  }
}

function toContext(raw: RawInitiativeContext): InitiativeDecisionContext {
  return Object.freeze({
    id: raw.id,
    codigo: raw.codigo,
    estado: raw.estado,
    nombre: raw.nombre,
    tipoSolucion: raw.tipoSolucion,
    unidades: (raw.unidades ?? []).map((unidad) =>
      Object.freeze({ unidadId: unidad.unidadId, principal: unidad.principal })
    ),
    responsableId: raw.responsableId,
    version: raw.version,
    etag: raw.etag
  });
}
