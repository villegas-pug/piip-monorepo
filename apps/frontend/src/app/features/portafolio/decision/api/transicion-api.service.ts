// Servicio de transición de estado de iniciativa/proyecto. Encapsula el
// endpoint único de la máquina de estados canónica definido por T058 (US2 -
// backend) en el snapshot OpenAPI codigo-first PIIP:
//
//   * POST /api/v1/portafolio/transiciones/{id}
//
// El cliente NO decide transiciones: la autoridad efectiva es el backend.
// La cabecera If-Match es obligatoria (428 IF_MATCH_REQUIRED si se omite;
// 412 STATE_CHANGED si la ETag no coincide). La cabecera Idempotency-Key
// la aplica automáticamente el idempotencyKeyInterceptor global.
//
// Este servicio es reutilizable desde la página de decisión de iniciativa
// (DecisionApiService), desde la página de cancelación de proyecto
// (DecisionApiService) y desde cualquier otra operación que requiera
// invocar el endpoint canónico de transiciones.

import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { withEntityTag } from '../../../../core/http/entity-tag';
import { REQUIRES_IDEMPOTENCY_KEY } from '../../../../core/http/idempotency-key.service';
import { EstadoIniciativa, TransicionCommand, TransicionDetail } from './types';

/** Opciones del comando de transición: ETag para control de concurrencia. */
export interface TransicionComandoOpciones {
  /** ETag devuelto por la última lectura del backend. Es obligatorio. */
  readonly etag: string;
}

interface RawTransicionDetail {
  readonly registroId: number;
  readonly estadoAnterior: EstadoIniciativa;
  readonly estadoNuevo: EstadoIniciativa;
  readonly transicionId: number;
  readonly fechaTransicion: string;
  readonly actorSub?: string;
  readonly version: number;
  readonly etag: string;
}

@Injectable({ providedIn: 'root' })
export class TransicionApiService {
  private readonly http = inject(HttpClient);
  private static readonly BASE = '/api/v1/portafolio/transiciones';

  /**
   * Confirma una transición controlada. La cabecera If-Match es obligatoria.
   * Sin ETag el backend responde 428; con ETag que no coincide responde 412.
   */
  transicionar(
    iniciativaId: number,
    payload: TransicionCommand,
    opciones: TransicionComandoOpciones
  ): Observable<TransicionDetail> {
    if (!opciones.etag) {
      throw new Error('transicionar exige un ETag devuelto por una lectura previa.');
    }
    const context: HttpContext = withEntityTag(opciones.etag).set(REQUIRES_IDEMPOTENCY_KEY, true);
    return this.http
      .post<RawTransicionDetail>(`${TransicionApiService.BASE}/`, payload, { context })
      .pipe(map((raw) => toTransicionDetail(raw)));
  }
}

function toTransicionDetail(raw: RawTransicionDetail): TransicionDetail {
  return Object.freeze({
    registroId: raw.registroId,
    estadoAnterior: raw.estadoAnterior,
    estadoNuevo: raw.estadoNuevo,
    transicionId: raw.transicionId,
    fechaTransicion: raw.fechaTransicion,
    actorSub: raw.actorSub,
    version: raw.version,
    etag: raw.etag
  });
}
