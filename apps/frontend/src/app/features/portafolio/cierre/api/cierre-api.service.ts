import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import { withEntityTag } from '../../../../core/http/entity-tag';
import { REQUIRES_IDEMPOTENCY_KEY } from '../../../../core/http/idempotency-key.service';

/** Tipos derivados del snapshot OpenAPI de cierre administrativo. */
export interface CierreProyectoRequest {
  readonly informeFinal: string;
  readonly informeFinalDocumentoId: number;
  readonly aprendizajes: string;
  readonly conclusion: string;
  readonly observacion: string;
}

export interface CierreProyectoResponse {
  readonly idCierre: number;
  readonly idProyecto: number;
  readonly estado: 'FINALIZADO';
  readonly fechaCierre: string;
  readonly etag: string;
}

export interface CierreCommandOptions {
  readonly etag: string;
}

/** Cliente HTTP del cierre; el backend decide elegibilidad, transición y fecha de cierre. */
@Injectable({ providedIn: 'root' })
export class CierreApiService {
  private readonly http = inject(HttpClient);
  private static readonly base = '/api/v1/portafolio/proyectos';

  cerrar(
    proyectoId: number,
    body: CierreProyectoRequest,
    options: CierreCommandOptions
  ): Observable<CierreProyectoResponse> {
    return this.http.post<CierreProyectoResponse>(
      `${CierreApiService.base}/${proyectoId}/cierres`,
      body,
      { context: commandContext(options.etag) }
    );
  }
}

function commandContext(etag: string): HttpContext {
  if (!etag?.trim()) {
    throw new Error('El cierre requiere el ETag de una lectura previa.');
  }
  return withEntityTag(etag).set(REQUIRES_IDEMPOTENCY_KEY, true);
}
