// Servicio de evaluación de iniciativa. Encapsula los seis endpoints
// institucionales definidos por T057 (US2 - backend) en el snapshot
// OpenAPI codigo-first PIIP:
//
//   * GET   /api/v1/portafolio/iniciativas/{id}
//   * POST  /api/v1/portafolio/iniciativas/{id}/subsanaciones
//   * PATCH /api/v1/portafolio/iniciativas/{id}/subsanacion
//   * POST  /api/v1/portafolio/iniciativas/{id}/subsanacion/cierre
//   * POST  /api/v1/portafolio/iniciativas/{id}/evaluaciones/admisibilidad
//   * POST  /api/v1/portafolio/iniciativas/{id}/evaluaciones/aplicabilidad
//
// El cliente NO decide transiciones, NO evalúa admisibilidad, NO calcula
// aplicabilidad: el backend es la autoridad efectiva. La ETag devuelta por
// `consultarIniciativa` se conserva en el cuerpo de la respuesta para que
// la página de evaluación la propague como `If-Match` en cada comando
// subsiguiente. La cabecera `Idempotency-Key` la aplica automáticamente
// el `idempotencyKeyInterceptor` global.
//
// NOTA: `consultarIniciativa` retorna el cuerpo directamente (no
// `HttpResponse`) para alinearse con la firma consumida por el spec de T055.
// El `Iniciativa` ya contiene la ETag en su cuerpo. Si el backend
// devolviera una ETag distinta en la cabecera, sería trivial extraerla con
// `observe: 'response'`; sin embargo, el snapshot actual la incluye en
// el cuerpo.

import { HttpClient, HttpContext } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { withEntityTag } from '../../../../core/http/entity-tag';
import { REQUIRES_IDEMPOTENCY_KEY } from '../../../../core/http/idempotency-key.service';
import {
  AdmissibilityRequest,
  ApplicabilityRequest,
  EvaluacionDetail,
  EstadoIniciativa,
  InitiativeEvaluationContext,
  OpenCorrectionRequest,
  SubsanacionDetail,
  SubsanacionEditCommand,
  TipoSolucion
} from './types';

/** Opciones comunes a los comandos de evaluación: ETag para control de concurrencia. */
export interface EvaluacionComandoOpciones {
  /** ETag devuelto por la última lectura del backend. Si se omite, no se envía `If-Match`. */
  readonly etag?: string;
}

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
}

interface RawEvaluacionDetail {
  readonly iniciativaId: number;
  readonly estadoIniciativa: EstadoIniciativa;
  readonly tipoEvaluacion: 'ADMISIBILIDAD' | 'APLICABILIDAD';
  readonly documentoOpinionId?: number;
  readonly fechaEvaluacion: string;
  readonly version: number;
  readonly etag: string;
}

interface RawSubsanacionDetail {
  readonly id: number;
  readonly iniciativaId: number;
  readonly plazo: string;
  readonly incumplimientos: string;
  readonly aperturaEn: string;
  readonly atencionEn?: string;
  readonly actorSub?: string;
  readonly version: number;
  readonly etag: string;
}

@Injectable({ providedIn: 'root' })
export class EvaluacionApiService {
  private readonly http = inject(HttpClient);
  private static readonly BASE = '/api/v1/portafolio/iniciativas';

  /**
   * Consulta el detalle de la iniciativa. La ETag devuelta se conserva en
   * el cuerpo para propagarla en cada comando subsiguiente con If-Match.
   */
  consultarIniciativa(id: number): Observable<InitiativeEvaluationContext> {
    return this.http
      .get<RawInitiativeContext>(`${EvaluacionApiService.BASE}/${id}`)
      .pipe(map((raw) => toContext(raw)));
  }

  /**
   * Abre la única subsanación de la iniciativa. El plazo debe ser estrictamente
   * posterior a la fecha de apertura. Una segunda apertura produce 409
   * `CORRECTION_ALREADY_USED`. El backend acepta `Idempotency-Key` opcional.
   */
  abrirSubsanacion(
    iniciativaId: number,
    payload: OpenCorrectionRequest,
    opciones: EvaluacionComandoOpciones = {}
  ): Observable<SubsanacionDetail> {
    return this.http
      .post<RawSubsanacionDetail>(`${EvaluacionApiService.BASE}/${iniciativaId}/subsanaciones`, payload, {
        context: buildContext(opciones)
      })
      .pipe(map((raw) => toSubsanacionDetail(raw)));
  }

  /**
   * Edita los campos oficiales 5-12, 22 y 23 de la iniciativa mientras la
   * subsanación siga abierta. La cabecera `If-Match` es obligatoria para
   * control de concurrencia.
   */
  editarSubsanacion(
    iniciativaId: number,
    payload: SubsanacionEditCommand,
    opciones: EvaluacionComandoOpciones = {}
  ): Observable<SubsanacionDetail> {
    if (!opciones.etag) {
      throw new Error('editarSubsanacion exige un ETag devuelto por una lectura previa.');
    }
    return this.http
      .patch<RawSubsanacionDetail>(`${EvaluacionApiService.BASE}/${iniciativaId}/subsanacion`, payload, {
        context: buildContext(opciones)
      })
      .pipe(map((raw) => toSubsanacionDetail(raw)));
  }

  /**
   * Registra la fecha de atención de la subsanación. La fila permanece para
   * auditoría. La cabecera `If-Match` es obligatoria.
   */
  cerrarSubsanacion(
    iniciativaId: number,
    opciones: EvaluacionComandoOpciones = {}
  ): Observable<SubsanacionDetail> {
    if (!opciones.etag) {
      throw new Error('cerrarSubsanacion exige un ETag devuelto por una lectura previa.');
    }
    return this.http
      .post<RawSubsanacionDetail>(`${EvaluacionApiService.BASE}/${iniciativaId}/subsanacion/cierre`, null, {
        context: buildContext(opciones)
      })
      .pipe(map((raw) => toSubsanacionDetail(raw)));
  }

  /**
   * Registra la decisión de admisibilidad. La iniciativa debe estar en
   * `PRESENTADO`. El backend acepta `Idempotency-Key` opcional y conserva la
   * ETag para If-Match cuando el cliente la propaga.
   */
  registrarAdmisibilidad(
    iniciativaId: number,
    payload: AdmissibilityRequest,
    opciones: EvaluacionComandoOpciones = {}
  ): Observable<EvaluacionDetail> {
    return this.http
      .post<RawEvaluacionDetail>(
        `${EvaluacionApiService.BASE}/${iniciativaId}/evaluaciones/admisibilidad`,
        payload,
        { context: buildContext(opciones) }
      )
      .pipe(map((raw) => toEvaluacionDetail(raw)));
  }

  /**
   * Registra la decisión de aplicabilidad con la lista estructurada de
   * criterios. El backend NO exige `If-Match` (concurrencia tolerante).
   */
  registrarAplicabilidad(
    iniciativaId: number,
    payload: ApplicabilityRequest,
    opciones: EvaluacionComandoOpciones = {}
  ): Observable<EvaluacionDetail> {
    return this.http
      .post<RawEvaluacionDetail>(
        `${EvaluacionApiService.BASE}/${iniciativaId}/evaluaciones/aplicabilidad`,
        payload,
        { context: buildContext(opciones) }
      )
      .pipe(map((raw) => toEvaluacionDetail(raw)));
  }
}

function buildContext(opciones: EvaluacionComandoOpciones = {}): HttpContext {
  const base = opciones.etag ? withEntityTag(opciones.etag) : new HttpContext();
  return base.set(REQUIRES_IDEMPOTENCY_KEY, true);
}

function toContext(raw: RawInitiativeContext): InitiativeEvaluationContext {
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

function toEvaluacionDetail(raw: RawEvaluacionDetail): EvaluacionDetail {
  return Object.freeze({
    iniciativaId: raw.iniciativaId,
    estadoIniciativa: raw.estadoIniciativa,
    tipoEvaluacion: raw.tipoEvaluacion,
    documentoOpinionId: raw.documentoOpinionId,
    fechaEvaluacion: raw.fechaEvaluacion,
    version: raw.version,
    etag: raw.etag
  });
}

function toSubsanacionDetail(raw: RawSubsanacionDetail): SubsanacionDetail {
  return Object.freeze({
    id: raw.id,
    iniciativaId: raw.iniciativaId,
    plazo: raw.plazo,
    incumplimientos: raw.incumplimientos,
    aperturaEn: raw.aperturaEn,
    atencionEn: raw.atencionEn,
    actorSub: raw.actorSub,
    version: raw.version,
    etag: raw.etag
  });
}
