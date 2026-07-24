// Cliente HTTP de la consulta pública del portafolio (US7 - T101).
//
// Encapsula las dos operaciones de lectura de la consulta pública
// contra el snapshot OpenAPI codigo-first aprobado en
// `specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml`:
//   * `GET /api/v1/consulta/publica/portafolio` (búsqueda paginada)
//   * `GET /api/v1/consulta/publica/portafolio/{id}` (detalle)
//
// La consulta pública es ANÓNIMA: el cliente NO envía `Authorization`
// ni `X-Asignacion-Efectiva-Id` (los interceptores globales excluyen
// explícitamente las rutas `/api/v1/consulta/publica`). Los DTOs son
// propios y NO se reutilizan con el cliente institucional: la
// Constitución prohíbe exponer Responsable, clasificación, contenido
// ni historial en la consulta pública.
//
// El cliente público NO llama a `/api/v1/documentos/{id}/contenido`:
// la descarga y la apertura de contenido no existen en el espacio
// público. La respuesta nunca incluye BLOB, URL de descarga, hash ni
// clave física; el componente que la consume NO debe renderizar ningún
// enlace o botón de descarga.
//
// Las respuestas Problem Details (`application/problem+json`) NO se
// inspeccionan en el cliente: la UI las traduce mediante
// `parseProblemDetails` desde `core/http/problem-details`. El cliente
// no decide ni reemplaza códigos canónicos.

import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import {
  PublicPortfolioDetail,
  PublicPortfolioDetailOptions,
  PublicPortfolioDetailResponse,
  PublicPortfolioFilters,
  PublicPortfolioPage,
  PublicPortfolioSummary
} from './types';

@Injectable({ providedIn: 'root' })
export class PublicQueryApiService {
  private readonly http = inject(HttpClient);
  private static readonly BASE = '/api/v1/consulta/publica/portafolio';

  /**
   * Búsqueda paginada anónima del portafolio público. La respuesta
   * contiene únicamente la allowlist canónica y los metadatos
   * descriptivos de publicaciones elegibles.
   */
  buscar(filtros: PublicPortfolioFilters = {}): Observable<PublicPortfolioPage> {
    return this.http
      .get<RawPublicPortfolioPage>(PublicQueryApiService.BASE, {
        params: buildParams(filtros),
        observe: 'response'
      })
      .pipe(map((response) => freezePage(response, filtros)));
  }

  /**
   * Detalle público de un registro. Conserva la ETag para soportar
   * `If-None-Match` en lecturas posteriores. La respuesta nunca
   * expone contenido ni una URL de descarga.
   */
  detalle(
    id: number,
    opciones: PublicPortfolioDetailOptions = {}
  ): Observable<PublicPortfolioDetailResponse> {
    return this.http
      .get<RawPublicPortfolioDetail>(`${PublicQueryApiService.BASE}/${id}`, {
        observe: 'response',
        headers: buildDetailHeaders(opciones)
      })
      .pipe(map((response) => toDetailResponse(response)));
  }
}

// ---------------------------------------------------------------------------
// Tipos crudos del backend (forma del JSON entrante)
// ---------------------------------------------------------------------------

interface RawPublicPortfolioDocumento {
  readonly tipoDocumental: string;
  readonly tituloPublico: string;
  readonly version: number;
  readonly formato: string;
  readonly fechaPublicacion: string;
}

interface RawPublicPortfolioSummary {
  readonly id: number;
  readonly tipoRegistro: 'INICIATIVA' | 'PROYECTO';
  readonly codigo: string;
  readonly nombre: string;
  readonly estado: string;
  readonly fechaInicio?: string;
  readonly publicaciones?: readonly RawPublicPortfolioDocumento[];
  readonly etag: string;
}

interface RawPublicPortfolioPage {
  readonly items: readonly RawPublicPortfolioSummary[];
  readonly pagina: number;
  readonly tamanio: number;
  readonly totalElementos: number;
  readonly totalPaginas: number;
  readonly etag: string;
}

interface RawPublicPortfolioDetail {
  readonly id: number;
  readonly tipoRegistro: 'INICIATIVA' | 'PROYECTO';
  readonly codigo: string;
  readonly nombre: string;
  readonly estado: string;
  readonly publicaciones?: readonly RawPublicPortfolioDocumento[];
  readonly etag: string;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function buildParams(filtros: PublicPortfolioFilters): HttpParams {
  let params = new HttpParams();
  for (const clave of Object.keys(filtros) as ReadonlyArray<keyof PublicPortfolioFilters>) {
    const valor = filtros[clave];
    if (valor === undefined || valor === null || valor === '') {
      continue;
    }
    params = params.set(clave, String(valor));
  }
  return params;
}

function buildDetailHeaders(opciones: PublicPortfolioDetailOptions): HttpHeaders {
  let headers = new HttpHeaders();
  const ifNoneMatch = opciones.ifNoneMatch?.trim();
  if (ifNoneMatch) {
    headers = headers.set('If-None-Match', ifNoneMatch);
  }
  return headers;
}

function freezePage(
  response: HttpResponse<RawPublicPortfolioPage>,
  filtros: PublicPortfolioFilters
): PublicPortfolioPage {
  const body = response.body;
  if (!body) {
    throw new Error('La respuesta pública no contiene cuerpo.');
  }
  return Object.freeze({
    items: Object.freeze(body.items.map(freezeSummary)),
    pagina: body.pagina,
    tamanio: body.tamanio,
    totalElementos: body.totalElementos,
    totalPaginas: body.totalPaginas,
    etag: body.etag,
    filtros
  });
}

function freezeSummary(raw: RawPublicPortfolioSummary): PublicPortfolioSummary {
  return Object.freeze({
    id: raw.id,
    tipoRegistro: raw.tipoRegistro,
    codigo: raw.codigo,
    nombre: raw.nombre,
    estado: raw.estado,
    fechaInicio: raw.fechaInicio,
    publicaciones: Object.freeze((raw.publicaciones ?? []).map(freezeDocumento)),
    etag: raw.etag
  });
}

function toDetailResponse(
  response: HttpResponse<RawPublicPortfolioDetail>
): PublicPortfolioDetailResponse {
  const status = response.status;
  const headers = response.headers;
  const etag = headers.get('ETag') ?? response.body?.etag;

  if (status === 304 || !response.body) {
    return Object.freeze({
      body: response.body
        ? freezeDetail(response.body)
        : emptyDetail(),
      etag,
      notModified: true
    });
  }

  return Object.freeze({
    body: freezeDetail(response.body),
    etag,
    notModified: false
  });
}

function emptyDetail(): PublicPortfolioDetail {
  return Object.freeze({
    id: 0,
    tipoRegistro: 'INICIATIVA',
    codigo: '',
    nombre: '',
    estado: '',
    publicaciones: Object.freeze([]),
    etag: ''
  });
}

function freezeDetail(raw: RawPublicPortfolioDetail): PublicPortfolioDetail {
  return Object.freeze({
    id: raw.id,
    tipoRegistro: raw.tipoRegistro,
    codigo: raw.codigo,
    nombre: raw.nombre,
    estado: raw.estado,
    publicaciones: Object.freeze((raw.publicaciones ?? []).map(freezeDocumento)),
    etag: raw.etag
  });
}

function freezeDocumento(raw: RawPublicPortfolioDocumento) {
  return Object.freeze({ ...raw });
}
