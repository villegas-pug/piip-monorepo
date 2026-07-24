// Cliente HTTP de la consulta institucional del portafolio (US7 - T101).
//
// Encapsula las dos operaciones de lectura de la consulta institucional
// contra el snapshot OpenAPI codigo-first aprobado en
// `specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml`:
//   * `GET /api/v1/consulta/institucional/portafolio` (búsqueda paginada)
//   * `GET /api/v1/consulta/institucional/portafolio/{id}` (detalle)
//
// El cliente NO decide reglas de negocio, NO filtra permisos, NO asume
// visibilidad más allá de la declarada por el backend y NO reutiliza
// DTOs con la consulta pública. La autorización efectiva la conserva el
// backend mediante la matriz de privacidad y la asignación efectiva.
//
// Cabeceras aplicadas:
//   * `Authorization: Bearer <token>` la añade el `authInterceptor` global
//     a todas las rutas `/api/v1/` que no sean `consulta/publica`.
//   * `X-Asignacion-Efectiva-Id` la añade el `effectiveAssignmentInterceptor`
//     cuando hay una asignación efectiva seleccionada; el backend la
//     revalida contra Oracle en cada llamada.
//   * `X-Correlation-Id` se devuelve por el backend y se propaga en
//     `InstitutionalPortfolioDetailResponse.correlationId` para auditoría.
//   * `If-Match` / `If-None-Match` se traducen desde la ETag recibida
//     en la lectura previa; el cliente las serializa a través de las
//     opciones `ifMatch` e `ifNoneMatch`.
//
// Las respuestas Problem Details (`application/problem+json`) NO se
// inspeccionan en el cliente: la UI las traduce mediante
// `parseProblemDetails` desde `core/http/problem-details`. El cliente no
// decide ni reemplaza los códigos canónicos (`ASSIGNMENT_SCOPE_DENIED`,
// `STATE_CHANGED`, `RECURSO_FUERA_DE_AMBITO`).

import { HttpClient, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import {
  InstitutionalPortfolioDetail,
  InstitutionalPortfolioDetailOptions,
  InstitutionalPortfolioDetailResponse,
  InstitutionalPortfolioFilters,
  InstitutionalPortfolioPage,
  InstitutionalPortfolioSummary
} from './types';

@Injectable({ providedIn: 'root' })
export class InstitutionalQueryApiService {
  private readonly http = inject(HttpClient);
  private static readonly BASE = '/api/v1/consulta/institucional/portafolio';

  /**
   * Búsqueda paginada del portafolio institucional dentro del ámbito del
   * actor. Los filtros se ajustan al contrato; la respuesta nunca
   * incluye contenido documental.
   */
  buscar(filtros: InstitutionalPortfolioFilters = {}): Observable<InstitutionalPortfolioPage> {
    return this.http
      .get<RawInstitutionalPortfolioPage>(InstitutionalQueryApiService.BASE, {
        params: buildParams(filtros),
        observe: 'response'
      })
      .pipe(map((response) => freezePage(response, filtros)));
  }

  /**
   * Detalle institucional de un registro. Conserva la ETag y el
   * `X-Correlation-Id` para soportar `If-Match` e `If-None-Match` en
   * operaciones posteriores y para registrar la correlación en
   * auditoría local.
   */
  detalle(
    id: number,
    opciones: InstitutionalPortfolioDetailOptions = {}
  ): Observable<InstitutionalPortfolioDetailResponse> {
    return this.http
      .get<RawInstitutionalPortfolioDetail>(`${InstitutionalQueryApiService.BASE}/${id}`, {
        observe: 'response',
        headers: buildDetailHeaders(opciones)
      })
      .pipe(map((response) => toDetailResponse(response)));
  }
}

// ---------------------------------------------------------------------------
// Tipos crudos del backend (forma del JSON entrante)
// ---------------------------------------------------------------------------

interface RawInstitutionalPortfolioSummary {
  readonly id: number;
  readonly tipoRegistro: 'INICIATIVA' | 'PROYECTO';
  readonly codigo: string;
  readonly codigoOrigen?: string;
  readonly nombre: string;
  readonly estado: string;
  readonly fechaInicio: string;
  readonly unidadEjecutoraId?: number;
  readonly unidadEjecutoraDescripcion?: string;
  readonly unidadEjecutoraAbreviatura?: string;
  readonly responsableId?: number;
  readonly puedeVerResponsable: boolean;
  readonly version: number;
  readonly etag: string;
}

interface RawInstitutionalPortfolioPage {
  readonly items: readonly RawInstitutionalPortfolioSummary[];
  readonly pagina: number;
  readonly tamanio: number;
  readonly totalElementos: number;
  readonly totalPaginas: number;
  readonly etag: string;
}

interface RawInstitutionalPortfolioUnidad {
  readonly id?: number;
  readonly unidadId: number;
  readonly descripcion?: string;
  readonly abreviatura?: string;
  readonly nroOrden?: number;
  readonly principal: boolean;
}

interface RawInstitutionalPortfolioPersona {
  readonly idParticipacion?: number;
  readonly participanteId?: number;
  readonly usuarioId?: number;
  readonly nombresCompletos: string;
  readonly institucion?: string;
  readonly funcion?: string;
  readonly clasificacion?: 'PUBLICO' | 'INTERNO' | 'RESTRINGIDO';
  readonly fechaInicio?: string;
  readonly fechaFin?: string;
  readonly vigente: boolean;
}

interface RawInstitutionalPortfolioDocument {
  readonly documentoId: number;
  readonly serieId?: number;
  readonly numeroVersion?: number;
  readonly titulo: string;
  readonly formato?: string;
  readonly mimeType?: string;
  readonly tamanoBytes?: number;
  readonly hashSha256?: string;
  readonly clasificacionPropuesta?: 'PUBLICO' | 'INTERNO' | 'RESTRINGIDO';
  readonly clasificacionValidada?: 'PUBLICO' | 'INTERNO' | 'RESTRINGIDO';
  readonly tipoDocumental?: string;
  readonly contextoDocumental?: string;
  readonly publicado: boolean;
  readonly fechaCarga?: string;
  readonly usuarioCargaId?: number;
  readonly puedeConsultarContenido: boolean;
  readonly etag?: string;
}

interface RawInstitutionalPortfolioHistory {
  readonly transicionId?: number;
  readonly estadoAnterior?: string;
  readonly estadoNuevo: string;
  readonly usuarioId?: number;
  readonly rolEfectivoId?: number;
  readonly unidadEfectivaId?: number;
  readonly fechaTransicion: string;
  readonly observaciones?: string;
  readonly documentoRefId?: number;
}

interface RawInstitutionalRelacion {
  readonly iniciativaId?: number;
  readonly proyectoId?: number;
  readonly iniciativaActual?: boolean;
  readonly proyectoActual?: boolean;
}

interface RawInstitutionalPortfolioDetail {
  readonly id: number;
  readonly tipoRegistro: 'INICIATIVA' | 'PROYECTO';
  readonly codigo: string;
  readonly codigoOrigen?: string;
  readonly fechaInicio: string;
  readonly fechaCierre?: string;
  readonly nombre: string;
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
  readonly unidadEjecutoraId?: number;
  readonly unidadEjecutoraDescripcion?: string;
  readonly unidadEjecutoraAbreviatura?: string;
  readonly estado: string;
  readonly componenteDigital?: boolean;
  readonly detalleComponenteDigital?: string;
  readonly nota?: string;
  readonly resultadosClave?: string;
  readonly unidades?: readonly RawInstitutionalPortfolioUnidad[];
  readonly participantes?: readonly RawInstitutionalPortfolioPersona[];
  readonly documentos?: readonly RawInstitutionalPortfolioDocument[];
  readonly historial?: readonly RawInstitutionalPortfolioHistory[];
  readonly relacion?: RawInstitutionalRelacion;
  readonly actorEsResponsable: boolean;
  readonly actorEsEvaluador: boolean;
  readonly actorEsAdministrador: boolean;
  readonly fechaCreacion?: string;
  readonly version: number;
  readonly etag: string;
}

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

function buildParams(filtros: InstitutionalPortfolioFilters): HttpParams {
  let params = new HttpParams();
  for (const clave of Object.keys(filtros) as ReadonlyArray<keyof InstitutionalPortfolioFilters>) {
    const valor = filtros[clave];
    if (valor === undefined || valor === null || valor === '') {
      continue;
    }
    params = params.set(clave, String(valor));
  }
  return params;
}

function buildDetailHeaders(opciones: InstitutionalPortfolioDetailOptions): HttpHeaders {
  let headers = new HttpHeaders();
  const ifMatch = opciones.ifMatch?.trim();
  if (ifMatch) {
    headers = headers.set('If-Match', ifMatch);
  }
  const ifNoneMatch = opciones.ifNoneMatch?.trim();
  if (ifNoneMatch) {
    headers = headers.set('If-None-Match', ifNoneMatch);
  }
  return headers;
}

function freezePage(
  response: HttpResponse<RawInstitutionalPortfolioPage>,
  filtros: InstitutionalPortfolioFilters
): InstitutionalPortfolioPage {
  const body = response.body;
  if (!body) {
    throw new Error('La respuesta institucional no contiene cuerpo.');
  }
  return Object.freeze({
    items: Object.freeze(body.items.map(freezeSummary)),
    pagina: body.pagina,
    tamanio: body.tamanio,
    totalElementos: body.totalElementos,
    totalPaginas: body.totalPaginas,
    etag: body.etag,
    // La página mantiene los filtros aplicados para que la UI pueda
    // reconstruirlos en la URL sin repreguntar al usuario.
    filtros
  });
}

function freezeSummary(raw: RawInstitutionalPortfolioSummary): InstitutionalPortfolioSummary {
  return Object.freeze({
    id: raw.id,
    tipoRegistro: raw.tipoRegistro,
    codigo: raw.codigo,
    codigoOrigen: raw.codigoOrigen,
    nombre: raw.nombre,
    estado: raw.estado as InstitutionalPortfolioSummary['estado'],
    fechaInicio: raw.fechaInicio,
    unidadEjecutoraId: raw.unidadEjecutoraId,
    unidadEjecutoraDescripcion: raw.unidadEjecutoraDescripcion,
    unidadEjecutoraAbreviatura: raw.unidadEjecutoraAbreviatura,
    responsableId: raw.responsableId,
    puedeVerResponsable: raw.puedeVerResponsable,
    version: raw.version,
    etag: raw.etag
  });
}

function toDetailResponse(
  response: HttpResponse<RawInstitutionalPortfolioDetail>
): InstitutionalPortfolioDetailResponse {
  const status = response.status;
  const headers = response.headers;
  const etag = headers.get('ETag') ?? response.body?.etag;
  const correlationId = headers.get('X-Correlation-Id') ?? undefined;

  // `If-None-Match` produce 304 sin cuerpo. El cliente conserva la
  // bandera para que la UI no muestre contenido obsoleto y refresque
  // cuando proceda.
  if (status === 304 || !response.body) {
    return Object.freeze({
      body: response.body
        ? freezeDetail(response.body)
        : emptyDetail(),
      etag,
      correlationId,
      notModified: true
    });
  }

  return Object.freeze({
    body: freezeDetail(response.body),
    etag,
    correlationId,
    notModified: false
  });
}

function emptyDetail(): InstitutionalPortfolioDetail {
  return Object.freeze({
    id: 0,
    tipoRegistro: 'INICIATIVA',
    codigo: '',
    fechaInicio: '',
    nombre: '',
    estado: 'PRESENTADO',
    unidades: Object.freeze([]),
    participantes: Object.freeze([]),
    documentos: Object.freeze([]),
    historial: Object.freeze([]),
    actorEsResponsable: false,
    actorEsEvaluador: false,
    actorEsAdministrador: false,
    version: 0,
    etag: ''
  });
}

function freezeDetail(raw: RawInstitutionalPortfolioDetail): InstitutionalPortfolioDetail {
  return Object.freeze({
    id: raw.id,
    tipoRegistro: raw.tipoRegistro,
    codigo: raw.codigo,
    codigoOrigen: raw.codigoOrigen,
    fechaInicio: raw.fechaInicio,
    fechaCierre: raw.fechaCierre,
    nombre: raw.nombre,
    tipoSolucion: raw.tipoSolucion,
    fuenteOrigen: raw.fuenteOrigen,
    detalleFuente: raw.detalleFuente,
    responsableId: raw.responsableId,
    problemaPublico: raw.problemaPublico,
    solucionPropuesta: raw.solucionPropuesta,
    objetivoPeiId: raw.objetivoPeiId,
    actividadPoiId: raw.actividadPoiId,
    unidadEjecutoraId: raw.unidadEjecutoraId,
    unidadEjecutoraDescripcion: raw.unidadEjecutoraDescripcion,
    unidadEjecutoraAbreviatura: raw.unidadEjecutoraAbreviatura,
    estado: raw.estado as InstitutionalPortfolioDetail['estado'],
    componenteDigital: raw.componenteDigital,
    detalleComponenteDigital: raw.detalleComponenteDigital,
    nota: raw.nota,
    resultadosClave: raw.resultadosClave,
    unidades: Object.freeze((raw.unidades ?? []).map(freezeUnidad)),
    participantes: Object.freeze((raw.participantes ?? []).map(freezePersona)),
    documentos: Object.freeze((raw.documentos ?? []).map(freezeDocumento)),
    historial: Object.freeze((raw.historial ?? []).map(freezeHistorial)),
    relacion: raw.relacion ? freezeRelacion(raw.relacion) : undefined,
    actorEsResponsable: raw.actorEsResponsable,
    actorEsEvaluador: raw.actorEsEvaluador,
    actorEsAdministrador: raw.actorEsAdministrador,
    fechaCreacion: raw.fechaCreacion,
    version: raw.version,
    etag: raw.etag
  });
}

function freezeUnidad(raw: RawInstitutionalPortfolioUnidad) {
  return Object.freeze({ ...raw });
}

function freezePersona(raw: RawInstitutionalPortfolioPersona) {
  return Object.freeze({ ...raw });
}

function freezeDocumento(raw: RawInstitutionalPortfolioDocument) {
  return Object.freeze({ ...raw });
}

function freezeHistorial(raw: RawInstitutionalPortfolioHistory) {
  return Object.freeze({
    ...raw,
    estadoAnterior: raw.estadoAnterior as InstitutionalPortfolioDetail['estado'] | undefined,
    estadoNuevo: raw.estadoNuevo as InstitutionalPortfolioDetail['estado']
  });
}

function freezeRelacion(raw: RawInstitutionalRelacion) {
  return Object.freeze({ ...raw });
}
