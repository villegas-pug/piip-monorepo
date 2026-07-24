import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { withEntityTag } from '../../../../core/http/entity-tag';
import { REQUIRES_IDEMPOTENCY_KEY } from '../../../../core/http/idempotency-key.service';
import { AdjuntarEvidenciaCicloRequest, AltaPersonaRequest, AltaUnidadRequest, BajaParticipanteRequest, CicloRequest, CicloResponse, CorreccionCicloRequest, EditarCamposEditablesRequest, ParticipanteResponse, PlanificacionRequest, PlanificacionResponse, PresentacionProductoFinalRequest, PresentacionProductoFinalResponse, ProyectoSeguimiento, SuspensionRequest, TransicionResponse } from './types';

export interface SeguimientoCommandOptions { readonly etag?: string; }

/** Cliente HTTP tipado: ProblemDetail se preserva para que la UI lo anuncie con parseProblemDetails. */
@Injectable({ providedIn: 'root' })
export class SeguimientoApiService {
  private readonly http = inject(HttpClient);
  private static readonly base = '/api/v1/portafolio/proyectos';

  consultarProyecto(id: number): Observable<ProyectoSeguimiento> { return this.http.get<ProyectoSeguimiento>(`${SeguimientoApiService.base}/${id}`); }
  listarPlanificaciones(id: number): Observable<readonly PlanificacionResponse[]> { return this.http.get<readonly PlanificacionResponse[]>(`${SeguimientoApiService.base}/${id}/planificaciones`); }
  registrarPlanificacion(id: number, body: PlanificacionRequest): Observable<PlanificacionResponse> { return this.http.post<PlanificacionResponse>(`${SeguimientoApiService.base}/${id}/planificaciones`, body, { context: context() }); }
  listarCiclos(id: number): Observable<readonly CicloResponse[]> { return this.http.get<readonly CicloResponse[]>(`${SeguimientoApiService.base}/${id}/ciclos`); }
  registrarCiclo(id: number, body: CicloRequest): Observable<CicloResponse> { return this.http.post<CicloResponse>(`${SeguimientoApiService.base}/${id}/ciclos`, body, { context: context() }); }
  corregirCiclo(id: number, cicloId: number, body: CorreccionCicloRequest): Observable<CicloResponse> { return this.http.post<CicloResponse>(`${SeguimientoApiService.base}/${id}/ciclos/${cicloId}/versiones`, body, { context: context() }); }
  listarVersionesCiclo(id: number, cicloId: number): Observable<readonly CicloResponse[]> { return this.http.get<readonly CicloResponse[]>(`${SeguimientoApiService.base}/${id}/ciclos/${cicloId}/versiones`); }
  adjuntarEvidenciaCiclo(id: number, cicloId: number, body: AdjuntarEvidenciaCicloRequest): Observable<void> { return this.http.post<void>(`${SeguimientoApiService.base}/${id}/ciclos/${cicloId}/documentos`, body, { context: context() }); }
  listarParticipantes(id: number): Observable<readonly ParticipanteResponse[]> { return this.http.get<readonly ParticipanteResponse[]>(`${SeguimientoApiService.base}/${id}/participantes`); }
  altaPersona(id: number, body: AltaPersonaRequest): Observable<ParticipanteResponse> { return this.http.post<ParticipanteResponse>(`${SeguimientoApiService.base}/${id}/participantes/personas`, body, { context: context() }); }
  altaUnidad(id: number, body: AltaUnidadRequest): Observable<ParticipanteResponse> { return this.http.post<ParticipanteResponse>(`${SeguimientoApiService.base}/${id}/participantes/unidades`, body, { context: context() }); }
  bajaParticipante(id: number, participacionId: number, body: BajaParticipanteRequest): Observable<void> { return this.http.post<void>(`${SeguimientoApiService.base}/${id}/participaciones/${participacionId}/bajas`, body, { context: context() }); }
  editarCamposEditables(id: number, body: EditarCamposEditablesRequest, options: SeguimientoCommandOptions): Observable<PlanificacionResponse> { return this.http.patch<PlanificacionResponse>(`${SeguimientoApiService.base}/${id}`, body, { context: context(options.etag, true) }); }
  presentarProductoFinal(id: number, body: PresentacionProductoFinalRequest): Observable<PresentacionProductoFinalResponse> { return this.http.post<PresentacionProductoFinalResponse>(`${SeguimientoApiService.base}/${id}/producto-final/presentaciones`, body, { context: context() }); }
  suspenderProyecto(id: number, body: SuspensionRequest, options: SeguimientoCommandOptions): Observable<TransicionResponse> { return this.http.post<TransicionResponse>(`${SeguimientoApiService.base}/${id}/suspensiones`, body, { context: context(options.etag, true) }); }
}
function context(etag?: string, required = false): HttpContext { if (required && !etag) throw new Error('La operación requiere el ETag de una lectura previa.'); return (etag ? withEntityTag(etag) : new HttpContext()).set(REQUIRES_IDEMPOTENCY_KEY, true); }
