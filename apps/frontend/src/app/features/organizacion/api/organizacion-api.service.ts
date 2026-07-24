// Servicio de organización. Consulta unidades y catálogos PEI/POI.
// Referencia contractual: specs/001-gestionar-portafolio-innovacion/contracts/organizacion.md
// El snapshot OpenAPI codigo-first (piip-api.yaml) aún no expone estos endpoints (placeholders
// hasta T048), por lo que este servicio se modela contra el contrato aprobado y se conecta
// a `/api/v1/organizacion/...` para que su activación sea trivial cuando el backend lo exponga.
// El cliente solo presenta opciones; el backend conserva la autorización efectiva.

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { PageResponse } from '../../portafolio/registro/api/types/common.types';
import { PlaneamientoOption, PlaneamientoQuery, UnidadOption, UnidadQuery } from '../../portafolio/registro/api/types/organizacion.types';

interface RawPlaneamientoOption {
  readonly id: number;
  readonly codigo: string;
  readonly descripcion: string;
  readonly vigenteDesde: string;
  readonly vigenteHasta?: string;
  readonly activo: boolean;
}

interface RawUnidadOption {
  readonly id: number;
  readonly codigo: string;
  readonly nombre: string;
  readonly activa: boolean;
}

@Injectable({ providedIn: 'root' })
export class OrganizacionApiService {
  private readonly http = inject(HttpClient);
  private static readonly BASE = '/api/v1/organizacion';

  /** Lista unidades responsables visibles para el actor, paginadas, con filtros opcionales. */
  consultarUnidades(filtros: UnidadQuery = {}): Observable<PageResponse<UnidadOption>> {
    return this.http
      .get<{ items?: readonly RawUnidadOption[]; page?: number; size?: number; totalElements?: number; totalPages?: number }>(
        `${OrganizacionApiService.BASE}/unidades`,
        { params: toUnidadParams(filtros) }
      )
      .pipe(map((response) => normalizePage(response, mapUnidad)));
  }

  /** Lista objetivos PEI vigentes visibles para el actor, paginados, con filtros opcionales. */
  consultarObjetivosPei(filtros: PlaneamientoQuery = {}): Observable<PageResponse<PlaneamientoOption>> {
    return this.http
      .get<{ items?: readonly RawPlaneamientoOption[]; page?: number; size?: number; totalElements?: number; totalPages?: number }>(
        `${OrganizacionApiService.BASE}/objetivos-pei`,
        { params: toPlaneamientoParams(filtros) }
      )
      .pipe(map((response) => normalizePage(response, mapPlaneamiento)));
  }

  /** Lista actividades POI vigentes visibles para el actor, paginadas, con filtros opcionales. */
  consultarActividadesPoi(filtros: PlaneamientoQuery = {}): Observable<PageResponse<PlaneamientoOption>> {
    return this.http
      .get<{ items?: readonly RawPlaneamientoOption[]; page?: number; size?: number; totalElements?: number; totalPages?: number }>(
        `${OrganizacionApiService.BASE}/actividades-poi`,
        { params: toPlaneamientoParams(filtros) }
      )
      .pipe(map((response) => normalizePage(response, mapPlaneamiento)));
  }
}

function toUnidadParams(filtros: UnidadQuery): HttpParams {
  let params = new HttpParams();
  if (filtros.q) params = params.set('q', filtros.q);
  if (typeof filtros.activa === 'boolean') params = params.set('activa', String(filtros.activa));
  if (typeof filtros.page === 'number') params = params.set('page', String(filtros.page));
  if (typeof filtros.size === 'number') params = params.set('size', String(filtros.size));
  if (filtros.sort) params = params.set('sort', filtros.sort);
  return params;
}

function toPlaneamientoParams(filtros: PlaneamientoQuery): HttpParams {
  let params = new HttpParams();
  if (filtros.q) params = params.set('q', filtros.q);
  if (filtros.vigenteEn) params = params.set('vigenteEn', filtros.vigenteEn);
  if (typeof filtros.page === 'number') params = params.set('page', String(filtros.page));
  if (typeof filtros.size === 'number') params = params.set('size', String(filtros.size));
  if (filtros.sort) params = params.set('sort', filtros.sort);
  return params;
}

function mapUnidad(raw: RawUnidadOption): UnidadOption {
  return Object.freeze({
    id: raw.id,
    codigo: raw.codigo,
    nombre: raw.nombre,
    activa: raw.activa
  });
}

function mapPlaneamiento(raw: RawPlaneamientoOption): PlaneamientoOption {
  return Object.freeze({
    id: raw.id,
    codigo: raw.codigo,
    descripcion: raw.descripcion,
    vigenteDesde: raw.vigenteDesde,
    vigenteHasta: raw.vigenteHasta,
    activo: raw.activo
  });
}

interface RawPage<T> {
  readonly items?: readonly T[];
  readonly page?: number;
  readonly size?: number;
  readonly totalElements?: number;
  readonly totalPages?: number;
}

function normalizePage<T, R>(response: RawPage<T>, mapper: (item: T) => R): PageResponse<R> {
  const items = (response.items ?? []).map(mapper);
  return Object.freeze({
    items,
    page: response.page ?? 0,
    size: response.size ?? items.length,
    totalElements: response.totalElements ?? items.length,
    totalPages: response.totalPages ?? (items.length ? 1 : 0)
  });
}
