// Tipos compartidos del módulo de seguridad (US6 - T092).
//
// El cliente `SeguridadApiService` y los componentes standalone de
// `user-administration`, `assignment-administration`,
// `matrix-administration` y `substitution-administration` consumen estas
// definiciones. Los DTO reflejan el snapshot OpenAPI codigo-first aprobado
// en `specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml`.
// Ningún tipo expone credenciales, contraseñas, tokens ni atributos
// sensibles: el frontend delega en Keycloak la gestión de identidad y nunca
// recibe ni almacena secretos.
//
// Los componentes NO deciden reglas de negocio, NO determinan vigencia,
// NO evalúan perfiles ni unidades: el backend es la autoridad efectiva
// mediante los servicios de `seguridad`. Los tipos son inmutables
// (`readonly`) y se proyectan en `Object.freeze` desde el cliente.

// ---------------------------------------------------------------------------
// Matriz función-perfil-unidad
// ---------------------------------------------------------------------------

/** Función declarada dentro de una versión de matriz. */
export interface MatrixFunction {
  readonly id?: number;
  readonly matrizVersionId?: number;
  readonly codigo: string;
  readonly descripcion: string;
  readonly activa: boolean;
}

/** Solicitud de creación/actualización de una función dentro de una versión. */
export interface MatrixFunctionRequest {
  readonly codigo: string;
  readonly descripcion: string;
}

/** Combinación concreta función-perfil-unidad vigente en una versión. */
export interface MatrixCombination {
  readonly id?: number;
  readonly matrizVersionId?: number;
  readonly funcion: string;
  readonly perfil: string;
  readonly unidadId: number;
  readonly vigenteDesde: string;
  readonly vigenteHasta?: string;
  readonly activa: boolean;
  readonly documentoAprobacionVersionId?: number;
  readonly aprobadorUsuarioId?: number;
  readonly registradorUsuarioId?: number;
}

/** Solicitud de combinación al registrar una versión de matriz. */
export interface MatrixCombinationRequest {
  readonly funcionCodigo: string;
  readonly perfil: string;
  readonly unidadId: number;
  readonly vigenteDesde: string;
  readonly vigenteHasta?: string;
  readonly documentoAprobacionVersionId: number;
  readonly aprobadorUsuarioId: number;
}

/** Versión inmutable de matriz aprobada externamente. */
export interface MatrixVersionDetail {
  readonly id?: number;
  readonly codigoVersion: string;
  readonly versionAnteriorId?: number;
  readonly documentoAprobacionVersionId: number;
  readonly vigenteDesde: string;
  readonly vigenteHasta?: string;
  readonly activa: boolean;
  readonly funciones: readonly MatrixFunctionRequest[];
  readonly combinaciones: readonly MatrixCombination[];
}

/** Solicitud de creación de una nueva versión de matriz. */
export interface MatrixVersionRequest {
  readonly codigoVersion: string;
  readonly versionAnteriorId?: number;
  readonly vigenteDesde: string;
  readonly vigenteHasta?: string;
  readonly documentoAprobacionVersionId: number;
  readonly funciones: readonly MatrixFunctionRequest[];
  readonly combinaciones: readonly MatrixCombinationRequest[];
}

/** Solicitud para inactivar una combinación mediante una nueva versión. */
export interface MatrixDeactivationRequest {
  readonly codigoNuevaVersion: string;
  readonly documentoAprobacionVersionId: number;
  readonly aprobadorUsuarioId: number;
  readonly motivo: string;
}

/** Página genérica devuelta por `GET /seguridad/matrices/versiones`. */
export interface MatrixVersionPage {
  readonly content: readonly MatrixVersionDetail[];
  readonly totalElements: number;
  readonly totalPages: number;
  readonly size: number;
  readonly number: number;
}

// ---------------------------------------------------------------------------
// Asignaciones funcionales
// ---------------------------------------------------------------------------

/** Detalle de una asignación funcional. */
export interface AssignmentDetail {
  readonly id?: number;
  readonly usuarioId: number;
  readonly matrizCombinacionId: number;
  readonly perfil?: string;
  readonly unidadId?: number;
  readonly fechaInicio: string;
  readonly fechaFin?: string;
  readonly documentoFormalVersionId?: number;
  readonly revocadaEn?: string;
  readonly version?: number;
  readonly etag?: string;
}

/** Solicitud de alta de asignación: el perfil y la unidad proceden de la combinación. */
export interface AssignmentRequest {
  readonly usuarioId: number;
  readonly matrizCombinacionId: number;
  readonly fechaInicio: string;
  readonly fechaFin?: string;
  readonly documentoFormalVersionId?: number;
}

/** Solicitud de cambio: solo admite ajustar la vigencia. */
export interface AssignmentChangeRequest {
  readonly fechaInicio: string;
  readonly fechaFin?: string;
}

/** Solicitud de revocación inmediata con motivo obligatorio. */
export interface RevocationRequest {
  readonly motivo: string;
  readonly documentoFormalVersionId?: number;
}

// ---------------------------------------------------------------------------
// Aprovisionamiento de usuarios (Keycloak primero, sin contraseñas)
// ---------------------------------------------------------------------------

/** Estados canónicos de una operación de aprovisionamiento. */
export type EstadoOperacionAprovisionamiento =
  | 'INICIADA'
  | 'KEYCLOAK_CREADO_DESHABILITADO'
  | 'ORACLE_PENDIENTE'
  | 'COMPLETADA'
  | 'FALLIDA_NO_RECUPERABLE';

/** Resultado de una operación de aprovisionamiento. */
export interface ProvisioningResult {
  readonly operacionId: number;
  readonly usuarioId?: number;
  readonly estado: EstadoOperacionAprovisionamiento;
  readonly recuperable: boolean;
  readonly intento: number;
}

/** Solicitud de aprovisionamiento. Nunca incluye contraseña, token ni atributo sensible. */
export interface CreateUserRequest {
  readonly correoInstitucional: string;
  readonly nombreCompleto: string;
  readonly unidadId: number;
}

/** Solicitud de cambio de estado (desactivación o reactivación) con motivo obligatorio. */
export interface UserStatusRequest {
  readonly motivo: string;
}

/** Resultado del cambio de estado de un usuario. */
export interface UserStatusResult {
  readonly usuarioId: number;
  readonly estado: 'HABILITADO' | 'DESHABILITADO';
  readonly keycloakId?: string;
}

// ---------------------------------------------------------------------------
// Suplencias funcionales
// ---------------------------------------------------------------------------

/** Detalle de una suplencia temporal sin solape. */
export interface SubstitutionDetail {
  readonly id?: number;
  readonly asignacionTitularId: number;
  readonly asignacionSuplenteId?: number;
  readonly suplenteUsuarioId: number;
  readonly inicio: string;
  readonly fin: string;
  readonly autoridadUsuarioId?: number;
  readonly documentoFormalVersionId?: number;
  readonly terminadaEn?: string;
}

/** Solicitud de creación de suplencia. */
export interface SubstitutionRequest {
  readonly suplenteUsuarioId: number;
  readonly inicio: string;
  readonly fin: string;
  readonly documentoFormalVersionId: number;
}

/** Solicitud de terminación anticipada por la misma autoridad. */
export interface EarlyTerminationRequest {
  readonly motivo: string;
  readonly documentoFormalVersionId?: number;
}
