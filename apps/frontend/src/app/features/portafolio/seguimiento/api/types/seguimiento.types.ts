/** Tipos derivados exclusivamente del snapshot OpenAPI vigente de seguimiento. */
export type EstadoProyecto =
  | 'PROYECTO_EJECUCION' | 'SUSPENDIDO' | 'CANCELADO' | 'PRODUCTO_APROBADO'
  | 'PRODUCTO_NO_APROBADO' | 'FINALIZADO';
export type TipoProductoFinal = 'PROTOTIPO_CONCEPTUALIZADO' | 'SOLUCION_FUNCIONAL';
export type RolParticipante = 'Responsable' | 'Participante';

export interface PlanificacionRequest { readonly alcance: string; readonly objetivos: string; readonly entregables?: string; readonly periodos?: string; }
export interface PlanificacionResponse { readonly idPlanificacion: number; readonly idProyecto: number; readonly alcance?: string; readonly objetivos?: string; readonly entregables?: string; readonly periodos?: string; readonly version: number; readonly idVersionAnterior?: number; readonly cerrado?: 'S' | 'N'; readonly etag: string; }
export interface CicloRequest { readonly periodo: string; readonly objetivos: string; readonly actividades: string; readonly avance: number; readonly dificultades?: string; readonly proximasAcciones?: string; }
export interface CorreccionCicloRequest { readonly motivo: string; readonly objetivos: string; readonly actividades: string; readonly avance: number; readonly dificultades?: string; readonly proximasAcciones?: string; }
export interface CicloResponse { readonly idCiclo: number; readonly idProyecto: number; readonly periodo: string; readonly numeroVersion: number; readonly idVersionAnterior?: number; readonly objetivos?: string; readonly actividades?: string; readonly avance?: number; readonly dificultades?: string; readonly proximasAcciones?: string; readonly cerrado: 'S' | 'N'; readonly fechaCierre?: string; readonly etag: string; }
export interface AdjuntarEvidenciaCicloRequest { readonly idDocumento: number; readonly tipoDocumental: string; }
export interface EvidenciaSeleccionable extends AdjuntarEvidenciaCicloRequest { readonly aptaComoEvidencia: boolean; }
export interface AltaPersonaRequest { readonly personaId?: number; readonly rol: RolParticipante; readonly nombresCompletos?: string; readonly institucion?: string; readonly funcion?: string; }
export interface AltaUnidadRequest { readonly unidadId: number; readonly rol: RolParticipante; }
export interface BajaParticipanteRequest { readonly fechaBaja: string; readonly motivo?: string; }
export interface ParticipanteResponse { readonly idParticipacion: number; readonly proyectoId: number; readonly personaId?: number; readonly unidadId?: number; readonly rol: string; readonly nombresCompletos?: string; readonly institucion?: string; readonly funcion?: string; readonly estado: 'VIGENTE' | 'BAJA'; readonly fechaAlta?: string; readonly fechaBaja?: string; readonly etag: string; }
export interface EditarCamposEditablesRequest { readonly documentacionGestion?: string; readonly resultadosClave?: string; readonly nota?: string; }
export interface PresentacionProductoFinalRequest extends EditarCamposEditablesRequest { readonly tipoProductoFinal: TipoProductoFinal; readonly idDocumentoSustenta: number; readonly evidenciaIds: readonly number[]; }
export interface PresentacionProductoFinalResponse extends PresentacionProductoFinalRequest { readonly idPresentacion: number; readonly idProyecto: number; readonly version: number; readonly idVersionAnterior?: number; readonly etag: string; }
export interface SuspensionRequest { readonly idDocumento: number; readonly observacion: string; }
export interface TransicionResponse { readonly idTransicion: number; readonly idProyecto: number; readonly estadoAnterior?: EstadoProyecto; readonly estadoNuevo: EstadoProyecto; readonly fechaTransicion?: string; readonly observaciones?: string; readonly idDocumento?: number; readonly etag: string; }
export interface ProyectoSeguimiento { readonly id: number; readonly codigo?: string; readonly nombre?: string; readonly estado: EstadoProyecto; readonly version: number; readonly etag: string; readonly documentacionGestion?: string; readonly resultadosClave?: string; readonly nota?: string; }
