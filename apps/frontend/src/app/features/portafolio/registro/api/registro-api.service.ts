// Servicio de registro de portafolio. Encapsula las operaciones de presentación,
// corrección e incorporación individual contra el snapshot OpenAPI codigo-first PIIP.
// Referencia contractual: specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml
// El cliente NO decide transiciones, NO resuelve conflictos, NO determina la pertenencia de
// un documento y NO asigna código: el backend es la autoridad efectiva.

import { HttpClient, HttpContext, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { withEntityTag } from '../../../../core/http/entity-tag';
import { REQUIRES_IDEMPOTENCY_KEY } from '../../../../core/http/idempotency-key.service';
import { CreateInitiativeRequest, InitiativeDetail } from './types/iniciativa.types';
import {
  CreateIncorporacionRequest,
  IncorporacionCorreccionRequest,
  IncorporacionDetail,
  IncorporacionResolucionConflictoRequest,
  IncorporacionValidacionRequest
} from './types/incorporacion.types';

interface RawInitiativeDetail {
  readonly id: number;
  readonly tipoRegistro: 'INICIATIVA' | 'PROYECTO';
  readonly codigo: string;
  readonly codigoOrigen?: string;
  readonly fechaInicio: string;
  readonly nombre?: string;
  readonly tipoSolucion?: 'POTENCIAL_ADAPTABLE' | 'POR_DEFINIR';
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
  readonly unidades?: readonly RawUnidadResponsableDetail[];
  readonly estado:
    | 'PRESENTADO'
    | 'NO_ADMISIBLE'
    | 'NO_APLICABLE'
    | 'INICIATIVA_APROBADA'
    | 'INICIATIVA_ARCHIVADA'
    | 'PROYECTO_EJECUCION'
    | 'SUSPENDIDO'
    | 'CANCELADO'
    | 'PRODUCTO_APROBADO'
    | 'PRODUCTO_NO_APROBADO'
    | 'FINALIZADO';
  readonly componenteDigital?: boolean;
  readonly detalleComponenteDigital?: string;
  readonly nota?: string;
  readonly version: number;
  readonly etag: string;
  readonly fechaCreacion?: string;
}

interface RawUnidadResponsableDetail {
  readonly id?: number;
  readonly unidadId: number;
  readonly descripcion?: string;
  readonly abreviatura?: string;
  readonly principal: boolean;
}

interface RawIncorporacionDetail {
  readonly id: number;
  readonly fuente: string;
  readonly fechaFuente?: string;
  readonly responsableId?: number;
  readonly documentoFuenteId?: number;
  readonly hashOriginal: string;
  readonly estado: 'PENDIENTE' | 'VALIDADO' | 'RECHAZADO';
  readonly registroVinculadoId?: number;
  readonly observacion?: string;
  readonly creadoPor?: string;
  readonly fechaCreacion?: string;
  readonly version: number;
  readonly etag: string;
}

export interface ComandoIncorporacionOpcional {
  /** ETag actual para control de concurrencia optimista en operaciones con `If-Match`. */
  readonly etag?: string;
}

@Injectable({ providedIn: 'root' })
export class RegistroApiService {
  private readonly http = inject(HttpClient);
  private static readonly PORTAFOLIO = '/api/v1/portafolio';
  private static readonly INCORPORACIONES = '/api/v1/portafolio/incorporaciones';

  /** Presenta una iniciativa. Exige `Idempotency-Key` (autogenerado por el interceptor). */
  presentarIniciativa(payload: CreateInitiativeRequest): Observable<InitiativeDetail> {
    return this.http
      .post<RawInitiativeDetail>(`${RegistroApiService.PORTAFOLIO}/iniciativas`, payload, {
        context: buildContext()
      })
      .pipe(map((raw) => toInitiativeDetail(raw)));
  }

  /** Consulta el detalle de una iniciativa, devolviendo la respuesta HTTP completa para conservar el ETag. */
  consultarIniciativa(id: number): Observable<HttpResponse<InitiativeDetail>> {
    return this.http
      .get<RawInitiativeDetail>(`${RegistroApiService.PORTAFOLIO}/iniciativas/${id}`, { observe: 'response' })
      .pipe(
        map((response) =>
          new HttpResponse<InitiativeDetail>({
            body: response.body ? toInitiativeDetail(response.body) : undefined,
            headers: response.headers,
            status: response.status,
            statusText: response.statusText,
            url: response.url ?? undefined
          })
        )
      );
  }

  /**
   * Registra una incorporación individual. La deduplicación por hash y responsable es
   * primaria; la `Idempotency-Key` (autogenerada por el interceptor) activa el servicio
   * de idempotencia si está disponible. Si la API devuelve 409 con
   * `INCORPORATION_CONFLICT_UNRESOLVED`, el cliente NO resuelve: el Evaluador debe
   * hacerlo mediante `resolverConflictoIncorporacion`.
   */
  registrarIncorporacion(
    payload: CreateIncorporacionRequest,
    opciones: ComandoIncorporacionOpcional = {}
  ): Observable<IncorporacionDetail> {
    return this.http
      .post<RawIncorporacionDetail>(RegistroApiService.INCORPORACIONES, payload, {
        context: buildContext(opciones)
      })
      .pipe(map((raw) => toIncorporacionDetail(raw)));
  }

  /** Registra una corrección append-only de una incorporación pendiente. */
  corregirIncorporacion(
    incorporacionId: number,
    payload: IncorporacionCorreccionRequest,
    opciones: ComandoIncorporacionOpcional = {}
  ): Observable<IncorporacionDetail> {
    return this.http
      .post<RawIncorporacionDetail>(
        `${RegistroApiService.INCORPORACIONES}/${incorporacionId}/correcciones`,
        payload,
        { context: buildContext(opciones) }
      )
      .pipe(map((raw) => toIncorporacionDetail(raw)));
  }

  /** El Evaluador transiciona la incorporación a VALIDADO o RECHAZADO. */
  validarIncorporacion(
    incorporacionId: number,
    payload: IncorporacionValidacionRequest,
    opciones: ComandoIncorporacionOpcional = {}
  ): Observable<IncorporacionDetail> {
    return this.http
      .post<RawIncorporacionDetail>(
        `${RegistroApiService.INCORPORACIONES}/${incorporacionId}/validaciones`,
        payload,
        { context: buildContext(opciones) }
      )
      .pipe(map((raw) => toIncorporacionDetail(raw)));
  }

  /** El Evaluador documenta la resolución de un conflicto abierto. */
  resolverConflictoIncorporacion(
    incorporacionId: number,
    payload: IncorporacionResolucionConflictoRequest,
    opciones: ComandoIncorporacionOpcional = {}
  ): Observable<IncorporacionDetail> {
    return this.http
      .post<RawIncorporacionDetail>(
        `${RegistroApiService.INCORPORACIONES}/${incorporacionId}/conflictos/${payload.conflictoId}/resoluciones`,
        payload,
        { context: buildContext(opciones) }
      )
      .pipe(map((raw) => toIncorporacionDetail(raw)));
  }
}

function buildContext(opciones: ComandoIncorporacionOpcional = {}): HttpContext {
  const base = opciones.etag ? withEntityTag(opciones.etag) : new HttpContext();
  return base.set(REQUIRES_IDEMPOTENCY_KEY, true);
}

function toInitiativeDetail(raw: RawInitiativeDetail): InitiativeDetail {
  return Object.freeze({
    id: raw.id,
    tipoRegistro: raw.tipoRegistro,
    codigo: raw.codigo,
    codigoOrigen: raw.codigoOrigen,
    fechaInicio: raw.fechaInicio,
    nombre: raw.nombre,
    tipoSolucion: raw.tipoSolucion,
    fuenteOrigen: raw.fuenteOrigen,
    detalleFuente: raw.detalleFuente,
    responsableId: raw.responsableId,
    problemaPublico: raw.problemaPublico,
    solucionPropuesta: raw.solucionPropuesta,
    objetivoPeiId: raw.objetivoPeiId,
    actividadPoiId: raw.actividadPoiId,
    unidades: raw.unidades?.map((unidad) =>
      Object.freeze({
        id: unidad.id,
        unidadId: unidad.unidadId,
        descripcion: unidad.descripcion,
        abreviatura: unidad.abreviatura,
        principal: unidad.principal
      })
    ),
    estado: raw.estado,
    componenteDigital: raw.componenteDigital,
    detalleComponenteDigital: raw.detalleComponenteDigital,
    nota: raw.nota,
    version: raw.version,
    etag: raw.etag,
    fechaCreacion: raw.fechaCreacion
  });
}

function toIncorporacionDetail(raw: RawIncorporacionDetail): IncorporacionDetail {
  return Object.freeze({
    id: raw.id,
    fuente: raw.fuente,
    fechaFuente: raw.fechaFuente,
    responsableId: raw.responsableId,
    documentoFuenteId: raw.documentoFuenteId,
    hashOriginal: raw.hashOriginal,
    estado: raw.estado,
    registroVinculadoId: raw.registroVinculadoId,
    observacion: raw.observacion,
    creadoPor: raw.creadoPor,
    fechaCreacion: raw.fechaCreacion,
    version: raw.version,
    etag: raw.etag
  });
}
