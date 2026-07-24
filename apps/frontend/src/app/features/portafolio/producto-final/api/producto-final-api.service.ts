import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { withEntityTag } from '../../../../core/http/entity-tag';
import { REQUIRES_IDEMPOTENCY_KEY } from '../../../../core/http/idempotency-key.service';

export type DecisionProductoFinal = 'APROBAR' | 'NO_APROBAR';
export type TipoProductoFinal = 'PROTOTIPO_CONCEPTUALIZADO' | 'SOLUCION_FUNCIONAL';

/** Tipos derivados del snapshot OpenAPI de decisión de producto final. */
export interface DecisionProductoFinalRequest {
  readonly decision: DecisionProductoFinal;
  readonly tipoProductoFinal: TipoProductoFinal;
  readonly documentoId?: number;
  readonly evidenciaId?: number;
  readonly observacion?: string;
}

export interface DecisionProductoFinalResponse {
  readonly idProyecto: number;
  readonly estado: 'PRODUCTO_APROBADO' | 'PRODUCTO_NO_APROBADO';
  readonly tipoProductoFinal: TipoProductoFinal;
  readonly idTransicion: number;
  readonly fechaDecision: string;
  readonly etag: string;
}

export interface ProductoFinalCommandOptions {
  readonly etag: string;
}

/**
 * Cliente del comando canónico de decisión. Las cabeceras de asignación efectiva,
 * idempotencia y concurrencia se resuelven por la infraestructura HTTP compartida.
 */
@Injectable({ providedIn: 'root' })
export class ProductoFinalApiService {
  private readonly http = inject(HttpClient);
  private static readonly base = '/api/v1/portafolio/proyectos';

  decidir(
    proyectoId: number,
    body: DecisionProductoFinalRequest,
    options: ProductoFinalCommandOptions
  ): Observable<DecisionProductoFinalResponse> {
    return this.http.post<DecisionProductoFinalResponse>(
      `${ProductoFinalApiService.base}/${proyectoId}/producto-final/decisiones`,
      body,
      { context: commandContext(options.etag) }
    );
  }
}

function commandContext(etag: string): HttpContext {
  if (!etag?.trim()) {
    throw new Error('La decisión requiere el ETag de una lectura previa.');
  }
  return withEntityTag(etag).set(REQUIRES_IDEMPOTENCY_KEY, true);
}
