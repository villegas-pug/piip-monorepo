// Servicio de proyectos del portafolio (US3). Encapsula los dos endpoints
// institucionales definidos por T065 (derivado backend) y T066 (directo
// backend) en el snapshot OpenAPI codigo-first PIIP:
//
//   * POST /api/v1/portafolio/iniciativas/{id}/proyecto-derivado
//   * POST /api/v1/portafolio/proyectos-directos
//
// Ademas reusa el endpoint `GET /api/v1/portafolio/iniciativas/{id}` que
// `EvaluacionApiService` y `DecisionApiService` ya consumen desde T057 (US2)
// para cargar el contexto de iniciativa aprobado, y
// `GET /api/v1/portafolio/iniciativas?unidadId=...&estado=PRESENTADO` para
// verificar la regla "no omite la evaluacion" del recorrido de proyecto
// directo. Esta convencion de consulta es la misma que usan los servicios
// de T055 y T058; el snapshot OpenAPI consolidara estas formas en una
// iteracion posterior.
//
// El cliente NO genera codigo, NO decide transiciones, NO determina la
// pertenencia de un documento y NO asigna responsables: el backend es la
// autoridad efectiva. La ETag devuelta por la consulta de iniciativa se
// propaga al comando de creacion del derivado mediante `If-Match`; la
// cabecera `Idempotency-Key` la aplica automaticamente el
// `idempotencyKeyInterceptor` global.

import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { withEntityTag } from '../../../../core/http/entity-tag';
import { REQUIRES_IDEMPOTENCY_KEY } from '../../../../core/http/idempotency-key.service';
import {
  CreateDerivedProjectCommand,
  DirectProjectCommand,
  DirectProjectContext,
  InitiativeDerivedContext,
  ProjectDetail,
  UnidadDerivadaItem,
  UnidadResponsableDetalle
} from './types/proyectos.types';
import { EstadoIniciativa, TipoRegistro, TipoSolucion } from '../../registro/api/types/common.types';

/** Opciones del comando de creacion de proyecto derivado: ETag para `If-Match`. */
export interface CrearProyectoDerivadoOpciones {
  /** ETag devuelto por la ultima lectura de la iniciativa. Es obligatorio. */
  readonly etag: string;
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
  readonly tipoRegistro?: TipoRegistro;
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

interface RawProjectDetail {
  readonly id: number;
  readonly iniciativaId?: number;
  readonly codigo: string;
  readonly codigoOrigen?: string;
  readonly fechaInicio: string;
  readonly nombre?: string;
  readonly tipoRegistro: TipoRegistro;
  readonly estado: EstadoIniciativa;
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
  readonly componenteDigital?: boolean;
  readonly detalleComponenteDigital?: string;
  readonly nota?: string;
  readonly documentoFormalId?: number;
  readonly version: number;
  readonly etag: string;
  readonly fechaCreacion?: string;
}

interface RawInitiativeListItem {
  readonly id: number;
}

@Injectable({ providedIn: 'root' })
export class ProyectosApiService {
  private readonly http = inject(HttpClient);
  private static readonly BASE = '/api/v1/portafolio';

  /**
   * Consulta el detalle de la iniciativa aprobada para derivar. La ETag se
   * devuelve en el cuerpo para propagarla en el comando de creacion con
   * `If-Match`. Reusa el mismo endpoint que `EvaluacionApiService` y
   * `DecisionApiService` de US2; proyecta los campos minimos requeridos por
   * la pagina de proyecto derivado.
   */
  consultarIniciativaParaDerivar(id: number): Observable<InitiativeDerivedContext> {
    return this.http
      .get<RawInitiativeContext>(`${ProyectosApiService.BASE}/iniciativas/${id}`)
      .pipe(map((raw) => toDerivedContext(raw)));
  }

  /**
   * Crea el unico proyecto derivado de una iniciativa `INICIATIVA_APROBADA`.
   * La cabecera `If-Match` se aplica con la ETag devuelta por la consulta
   * previa. Un segundo intento para la misma iniciativa falla con 409
   * `DERIVATION_ALREADY_EXISTS`; dos solicitudes concurrentes son
   * serializadas por bloqueo pesimista y la primera confirmacion gana.
   */
  crearProyectoDerivado(
    iniciativaId: number,
    payload: CreateDerivedProjectCommand,
    opciones: CrearProyectoDerivadoOpciones
  ): Observable<ProjectDetail> {
    if (!opciones.etag) {
      throw new Error('crearProyectoDerivado exige un ETag devuelto por una lectura previa.');
    }
    const context: HttpContext = withEntityTag(opciones.etag).set(REQUIRES_IDEMPOTENCY_KEY, true);
    return this.http
      .post<RawProjectDetail>(
        `${ProyectosApiService.BASE}/iniciativas/${iniciativaId}/proyecto-derivado`,
        payload,
        { context }
      )
      .pipe(map((raw) => toProjectDetail(raw)));
  }

  /**
   * Verifica si existe una iniciativa en estado `PRESENTADO` para la misma
   * unidad responsable. Se reusa antes de registrar un proyecto directo
   * para aplicar la regla constitucional "no omite la evaluacion": si
   * existe, la pagina bloquea el envio. La forma de respuesta es minima
   * (solo `id`) y refleja la convencion del snapshot OpenAPI vigente.
   */
  consultarIniciativaPresentadaPorUnidad(unidadId: number): Observable<DirectProjectContext> {
    const params = new HttpParams()
      .set('unidadId', String(unidadId))
      .set('estado', 'PRESENTADO');
    return this.http
      .get<{ readonly items?: readonly RawInitiativeListItem[] }>(
        `${ProyectosApiService.BASE}/iniciativas`,
        { params }
      )
      .pipe(map((raw) => toDirectContext(raw)));
  }

  /**
   * Registra un proyecto directo (heredado o excepcional) sin iniciativa
   * origen. La Autoridad o el Evaluador con documento formal lo invocan.
   * Un segundo directo concurrente para la misma unidad y anio falla con
   * 409 `DIRECT_PROJECT_NOT_AUTHORIZED`; si ya existe un directo activo
   * el backend responde 409 `DIRECT_PROJECT_DUPLICATE`.
   */
  crearProyectoDirecto(payload: DirectProjectCommand): Observable<ProjectDetail> {
    const context = new HttpContext().set(REQUIRES_IDEMPOTENCY_KEY, true);
    return this.http
      .post<RawProjectDetail>(`${ProyectosApiService.BASE}/proyectos-directos`, payload, { context })
      .pipe(map((raw) => toProjectDetail(raw)));
  }
}

function toDerivedContext(raw: RawInitiativeContext): InitiativeDerivedContext {
  return Object.freeze({
    id: raw.id,
    codigo: raw.codigo,
    codigoOrigen: raw.codigoOrigen,
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

function toDirectContext(raw: { readonly items?: readonly RawInitiativeListItem[] }): DirectProjectContext {
  const primero = raw.items?.[0];
  return Object.freeze({ id: primero?.id });
}

function toProjectDetail(raw: RawProjectDetail): ProjectDetail {
  return Object.freeze({
    id: raw.id,
    iniciativaId: raw.iniciativaId,
    codigo: raw.codigo,
    codigoOrigen: raw.codigoOrigen,
    fechaInicio: raw.fechaInicio,
    nombre: raw.nombre,
    tipoRegistro: raw.tipoRegistro,
    estado: raw.estado,
    fuenteOrigen: raw.fuenteOrigen,
    detalleFuente: raw.detalleFuente,
    responsableId: raw.responsableId,
    problemaPublico: raw.problemaPublico,
    solucionPropuesta: raw.solucionPropuesta,
    objetivoPeiId: raw.objetivoPeiId,
    actividadPoiId: raw.actividadPoiId,
    unidades: (raw.unidades ?? []).map((unidad) =>
      Object.freeze({
        id: unidad.id,
        unidadId: unidad.unidadId,
        descripcion: unidad.descripcion,
        abreviatura: unidad.abreviatura,
        principal: unidad.principal
      }) satisfies UnidadResponsableDetalle
    ),
    componenteDigital: raw.componenteDigital,
    detalleComponenteDigital: raw.detalleComponenteDigital,
    nota: raw.nota,
    documentoFormalId: raw.documentoFormalId,
    version: raw.version,
    etag: raw.etag,
    fechaCreacion: raw.fechaCreacion
  });
}

// Reexporta el tipo de unidad derivada para los componentes que construyen
// el `FormArray` de unidades y necesitan proyectar la forma minima.
export type { UnidadDerivadaItem };