// Tipos compartidos del feature de proyectos del portafolio (US3).
//
// Reflejan los schemas del snapshot OpenAPI codigo-first PIIP para los dos
// endpoints definidos por T065 (derivado backend) y T066 (directo backend):
//
//   * POST /api/v1/portafolio/iniciativas/{id}/proyecto-derivado
//   * POST /api/v1/portafolio/proyectos-directos
//
// La consulta de iniciativa por id (necesaria para cargar el contexto de
// derivacion) reusa el endpoint `GET /api/v1/portafolio/iniciativas/{id}` que
// `EvaluacionApiService` y `DecisionApiService` ya consumen desde T057 (US2).
// La consulta de iniciativa `PRESENTADO` por unidad reusa
// `GET /api/v1/portafolio/iniciativas` con `unidadId` y `estado` como
// discriminadores, alineado con la convencion de T059.
//
// El cliente NO genera codigo, NO decide transiciones, NO determina la
// pertenencia de un documento y NO asigna responsables: el backend es la
// autoridad efectiva. La obligatoriedad de campos se modela solo en la capa
// de validacion de formularios; la autoridad definitiva reside en el
// backend.

import { EstadoIniciativa, FuenteOrigen, TipoRegistro, TipoSolucion } from '../../../registro/api/types/common.types';
export type { EstadoIniciativa, FuenteOrigen, TipoRegistro, TipoSolucion };

/** Catalogo de origen de un proyecto directo. */
export type TipoOrigenDirecto = 'HEREDADO' | 'EXCEPCION_FORMAL';

/** Unidad responsable en la creacion de un proyecto derivado. */
export interface UnidadDerivadaItem {
  readonly unidadId: number;
  readonly principal: boolean;
}

/** Unidad responsable detallada devuelta por `ProjectDetail.unidades`. */
export interface UnidadResponsableDetalle {
  readonly id?: number;
  readonly unidadId: number;
  readonly descripcion?: string;
  readonly abreviatura?: string;
  readonly principal: boolean;
}

/**
 * Contexto de iniciativa para la pagina de proyecto derivado. Proyecta
 * `InitiativeDetail` a la forma minima que requiere la vista: id, codigo,
 * estado, unidades, responsable, version y ETag. NO replica el detalle
 * completo porque la UI solo consulta un subconjunto.
 */
export interface InitiativeDerivedContext {
  readonly id: number;
  readonly codigo: string;
  readonly codigoOrigen?: string;
  readonly estado: EstadoIniciativa;
  readonly nombre?: string;
  readonly tipoSolucion?: TipoSolucion;
  readonly unidades: readonly UnidadDerivadaItem[];
  readonly responsableId?: number;
  readonly version: number;
  readonly etag: string;
}

/**
 * Contexto de iniciativa `PRESENTADO` para la pagina de proyecto directo.
 * Se modela como `{ id?: number }` para distinguir entre "no existe
 * iniciativa PRESENTADO en la unidad" (`{ id: undefined }`) y "si existe y
 * bloquea la creacion" (`{ id: 999 }`). El backend conserva la autoridad
 * efectiva; este discriminador solo habilita la UX.
 */
export interface DirectProjectContext {
  readonly id?: number;
}

/**
 * Comando para `POST /api/v1/portafolio/iniciativas/{id}/proyecto-derivado`.
 * Refleja `components.schemas.CreateDerivedProjectRequest` del snapshot
 * OpenAPI. `detalleFuente` se omite cuando `fuenteOrigen` no es `OTROS`; la
 * omision la gestiona el componente para no enviar campos vacios.
 */
export interface CreateDerivedProjectCommand {
  readonly nombre: string;
  readonly objetivoPeiId: number;
  readonly actividadPoiId: number;
  readonly unidades: readonly UnidadDerivadaItem[];
  readonly titularId: number;
  readonly fuenteOrigen: FuenteOrigen;
  readonly detalleFuente?: string;
  readonly descripcion: string;
  readonly componenteDigital: boolean;
  readonly detalleComponenteDigital?: string;
  readonly nota?: string;
  readonly documentoFormalId: number;
}

/**
 * Comando para `POST /api/v1/portafolio/proyectos-directos`. Refleja
 * `components.schemas.DirectProjectRequest` del snapshot OpenAPI. El cliente
 * envia `codigoOrigen` solo cuando `tipoOrigen === 'EXCEPCION_FORMAL'`. La
 * fecha de inicio la fija el servidor en `PROYECTO_EJECUCION` pero el
 * cliente la exige como dato contractual del recorrido.
 */
export interface DirectProjectCommand {
  readonly tipoOrigen: TipoOrigenDirecto;
  readonly codigoOrigen?: string;
  readonly fechaInicio: string;
  readonly nombre: string;
  readonly objetivoPeiId: number;
  readonly actividadPoiId: number;
  readonly unidadResponsableId: number;
  readonly responsableId: number;
  readonly descripcion: string;
  readonly componenteDigital: boolean;
  readonly detalleComponenteDigital?: string;
  readonly nota?: string;
  readonly documentoAutorizacionId: number;
  readonly evidenciaIds: readonly number[];
  readonly fuenteOrigen: FuenteOrigen;
  readonly detalleFuente?: string;
}

/**
 * Detalle de proyecto devuelto por los dos endpoints de creacion. Refleja
 * `components.schemas.ProjectDetail` del snapshot OpenAPI. `iniciativaId` se
 * omite para proyectos directos (que no tienen iniciativa origen).
 */
export interface ProjectDetail {
  readonly id: number;
  readonly iniciativaId?: number;
  readonly codigo: string;
  readonly codigoOrigen?: string;
  readonly fechaInicio: string;
  readonly nombre?: string;
  readonly tipoRegistro: TipoRegistro;
  readonly estado: EstadoIniciativa;
  readonly fuenteOrigen?: FuenteOrigen;
  readonly detalleFuente?: string;
  readonly responsableId?: number;
  readonly problemaPublico?: string;
  readonly solucionPropuesta?: string;
  readonly objetivoPeiId?: number;
  readonly actividadPoiId?: number;
  readonly unidades?: readonly UnidadResponsableDetalle[];
  readonly componenteDigital?: boolean;
  readonly detalleComponenteDigital?: string;
  readonly nota?: string;
  readonly documentoFormalId?: number;
  readonly version: number;
  readonly etag: string;
  readonly fechaCreacion?: string;
}