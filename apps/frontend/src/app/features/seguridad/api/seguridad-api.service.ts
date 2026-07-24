// Servicio de seguridad institucional (US6 - T092).
//
// Encapsula el ciclo ordinario de administración de acceso organizacional:
//   * Matriz función-perfil-unidad (versiones inmutables e inactivación por
//     nueva versión, implementadas en T087).
//   * Asignaciones funcionales (alta, cambio de vigencia, revocación
//     inmediata, implementadas en T088).
//   * Aprovisionamiento de usuarios institucionales con Keycloak primero
//     (consulta, reintento, desactivación y reactivación, sin contraseñas,
//     implementadas en T091).
//   * Suplencias funcionales sin solape y terminación por la misma
//     autoridad (implementadas en T089).
//
// El cliente NO decide transiciones, NO evalúa perfiles, NO valida
// combinaciones y NO combina permisos desde el JWT: el backend conserva la
// autorización efectiva. Las cabeceras `Authorization`, `X-Asignacion-Efectiva-Id`,
// `If-Match` e `Idempotency-Key` se aplican mediante los interceptores
// registrados en `app.config.ts`:
//
//   * `authInterceptor` añade el Bearer token en rutas `/api/v1/` que no
//     sean consulta pública.
//   * `effectiveAssignmentInterceptor` añade `X-Asignacion-Efectiva-Id` con
//     la asignación seleccionada por el usuario, salvo la ruta
//     `/api/v1/seguridad/me/asignaciones`.
//   * `idempotencyKeyInterceptor` añade `Idempotency-Key` a los verbos
//     `POST` institucionales, salvo cuando el consumidor la haya adjuntado.
//   * `entityTagInterceptor` traduce el `HttpContext.ENTITY_TAG` en la
//     cabecera `If-Match` cuando el consumidor la facilita tras una lectura.
//
// Las respuestas `application/problem+json` se traducen en la UI mediante
// `parseProblemDetails` desde `core/http/problem-details`; el cliente no
// inspecciona ni decide códigos canónicos.
//
// Discrepancia documentada (`NEEDS CLARIFICATION`): los endpoints de
// suplencias (`POST /api/v1/seguridad/asignaciones/{titularId}/suplencias`
// y `POST /api/v1/seguridad/suplencias/{id}/terminaciones`) están
// implementados en `SuplenciaController` (T089) pero aún no están
// declarados en `specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml`.
// Se conservan las rutas exactas del backend ya publicadas en
// `SuplenciaController` para evitar divergencias funcionales; la
// regeneración del snapshot OpenAPI corresponde al backend y se registrará
// en el handoff.

import { HttpClient, HttpContext, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { withEntityTag } from '../../../core/http/entity-tag';
import { REQUIRES_IDEMPOTENCY_KEY } from '../../../core/http/idempotency-key.service';
import {
  AssignmentChangeRequest,
  AssignmentDetail,
  AssignmentRequest,
  CreateUserRequest,
  EarlyTerminationRequest,
  MatrixCombination,
  MatrixDeactivationRequest,
  MatrixFunction,
  MatrixVersionDetail,
  MatrixVersionPage,
  MatrixVersionRequest,
  ProvisioningResult,
  RevocationRequest,
  SubstitutionDetail,
  SubstitutionRequest,
  UserStatusRequest,
  UserStatusResult
} from './types';

/** Opciones comunes: la ETag recibida en una lectura previa se propaga a comandos mutables. */
export interface ComandoConEtag {
  /** ETag actual para control de concurrencia optimista (`If-Match`). */
  readonly etag?: string;
}

@Injectable({ providedIn: 'root' })
export class SeguridadApiService {
  private readonly http = inject(HttpClient);
  private static readonly BASE = '/api/v1/seguridad';
  private static readonly MATRICES = '/api/v1/seguridad/matrices';
  private static readonly ASIGNACIONES = '/api/v1/seguridad/asignaciones';
  private static readonly USUARIOS = '/api/v1/seguridad/usuarios';

  // -------------------------------------------------------------------------
  // Matriz función-perfil-unidad
  // -------------------------------------------------------------------------

  /** Lista paginada del historial de versiones inmutables de la matriz. */
  listarVersionesMatriz(pagina = 0, tamanio = 20): Observable<MatrixVersionPage> {
    const params = new HttpParams().set('pagina', pagina).set('tamanio', tamanio);
    return this.http
      .get<MatrixVersionPage>(`${SeguridadApiService.MATRICES}/versiones`, { params })
      .pipe(map((page) => freezeMatrixPage(page)));
  }

  /** Crea una nueva versión inmutable de matriz con sus funciones y combinaciones. */
  crearVersionMatriz(payload: MatrixVersionRequest): Observable<MatrixVersionDetail> {
    return this.http
      .post<MatrixVersionDetail>(`${SeguridadApiService.MATRICES}/versiones`, payload, { context: context() })
      .pipe(map(freezeVersion));
  }

  /** Lista las funciones vigentes e históricas autorizadas por la matriz activa. */
  listarFunciones(): Observable<readonly MatrixFunction[]> {
    return this.http
      .get<readonly MatrixFunction[]>(`${SeguridadApiService.BASE}/funciones`)
      .pipe(map((funciones) => {
        const items = Array.isArray(funciones) ? funciones : [];
        return Object.freeze(items.map(freezeFunction));
      }));
  }

  /** Lista las combinaciones registradas para una versión concreta de la matriz. */
  listarCombinacionesMatriz(matrizVersionId: number): Observable<readonly MatrixCombination[]> {
    return this.http
      .get<readonly MatrixCombination[]>(`${SeguridadApiService.MATRICES}/versiones/${matrizVersionId}/combinaciones`)
      .pipe(map((combinaciones) => {
        const items = Array.isArray(combinaciones) ? combinaciones : [];
        return Object.freeze(items.map(freezeCombination));
      }));
  }

  /** Inactiva una combinación registrando una nueva versión de matriz que la omite. */
  inactivarCombinacionMatriz(
    combinacionId: number,
    payload: MatrixDeactivationRequest
  ): Observable<MatrixVersionDetail> {
    return this.http
      .post<MatrixVersionDetail>(
        `${SeguridadApiService.MATRICES}/combinaciones/${combinacionId}/inactivaciones`,
        payload,
        { context: context() }
      )
      .pipe(map(freezeVersion));
  }

  // -------------------------------------------------------------------------
  // Asignaciones funcionales
  // -------------------------------------------------------------------------

  /** Crea una asignación funcional desde una combinación vigente de la matriz. */
  crearAsignacion(payload: AssignmentRequest): Observable<AssignmentDetail> {
    return this.http
      .post<AssignmentDetail>(SeguridadApiService.ASIGNACIONES, payload, { context: context() })
      .pipe(map(freezeAssignment));
  }

  /**
   * Cambia únicamente la vigencia de una asignación. La ETag devuelta por
   * la lectura previa se envía como `If-Match` para evitar condiciones de
   * carrera con revocaciones concurrentes.
   */
  cambiarAsignacion(id: number, payload: AssignmentChangeRequest, opciones: ComandoConEtag): Observable<AssignmentDetail> {
    const etag = requireEtag(opciones.etag);
    return this.http
      .patch<AssignmentDetail>(`${SeguridadApiService.ASIGNACIONES}/${id}`, payload, { context: context(etag) })
      .pipe(map(freezeAssignment));
  }

  /** Revoca de forma inmediata una asignación con motivo obligatorio para auditoría. */
  revocarAsignacion(id: number, payload: RevocationRequest): Observable<AssignmentDetail> {
    return this.http
      .post<AssignmentDetail>(`${SeguridadApiService.ASIGNACIONES}/${id}/revocaciones`, payload, { context: context() })
      .pipe(map(freezeAssignment));
  }

  // -------------------------------------------------------------------------
  // Aprovisionamiento de usuarios (Keycloak primero, sin contraseñas)
  // -------------------------------------------------------------------------

  /**
   * Aprovisiona un usuario institucional. El backend crea primero la
   * identidad deshabilitada en Keycloak y luego persiste en Oracle; si
   * Oracle falla, la identidad permanece deshabilitada y la operación se
   * conserva como recuperable (HTTP 202). No se acepta contraseña, token
   * ni atributo sensible.
   */
  aprovisionarUsuario(payload: CreateUserRequest): Observable<ProvisioningResult> {
    return this.http
      .post<ProvisioningResult>(SeguridadApiService.USUARIOS, payload, { context: context() })
      .pipe(map(freezeProvisioning));
  }

  /** Consulta el estado actual de una operación de aprovisionamiento revalidando la autorización. */
  consultarOperacionAprovisionamiento(operacionId: number): Observable<ProvisioningResult> {
    return this.http
      .get<ProvisioningResult>(`${SeguridadApiService.USUARIOS}/operaciones/${operacionId}`)
      .pipe(map(freezeProvisioning));
  }

  /** Reintenta una operación de aprovisionamiento en estado recuperable. */
  reintentarAprovisionamiento(operacionId: number): Observable<ProvisioningResult> {
    return this.http
      .post<ProvisioningResult>(`${SeguridadApiService.USUARIOS}/operaciones/${operacionId}/reintentos`, null, {
        context: context()
      })
      .pipe(map(freezeProvisioning));
  }

  /** Desactiva un usuario bloqueándolo en Keycloak y PIIP con motivo obligatorio. */
  desactivarUsuario(id: number, payload: UserStatusRequest): Observable<UserStatusResult> {
    return this.http
      .post<UserStatusResult>(`${SeguridadApiService.USUARIOS}/${id}/desactivaciones`, payload, { context: context() })
      .pipe(map(freezeUserStatus));
  }

  /** Reactiva un usuario habilitado Keycloak y PIIP; no restaura asignaciones revocadas. */
  reactivarUsuario(id: number, payload: UserStatusRequest): Observable<UserStatusResult> {
    return this.http
      .post<UserStatusResult>(`${SeguridadApiService.USUARIOS}/${id}/reactivaciones`, payload, { context: context() })
      .pipe(map(freezeUserStatus));
  }

  // -------------------------------------------------------------------------
  // Suplencias funcionales
  // -------------------------------------------------------------------------

  /**
   * Crea una suplencia temporal sin solape sobre una asignación titular.
   * El backend rechaza solapes y exige documento formal de aprobación.
   * La autoridad es la misma que autorizó la asignación titular.
   *
   * `NEEDS CLARIFICATION`: la ruta no está publicada todavía en el
   * snapshot OpenAPI codigo-first (`piip-api.yaml`); se conserva la ruta
   * del backend (`SuplenciaController`).
   */
  crearSuplencia(titularAsignacionId: number, payload: SubstitutionRequest): Observable<SubstitutionDetail> {
    return this.http
      .post<SubstitutionDetail>(
        `${SeguridadApiService.ASIGNACIONES}/${titularAsignacionId}/suplencias`,
        payload,
        { context: context() }
      )
      .pipe(map(freezeSubstitution));
  }

  /**
   * Termina anticipadamente una suplencia por la misma autoridad que la
   * autorizó. La terminación queda registrada en el historial de
   * auditoría.
   *
   * `NEEDS CLARIFICATION`: la ruta no está publicada todavía en el
   * snapshot OpenAPI codigo-first; se conserva la ruta del backend.
   */
  terminarSuplenciaAnticipadamente(
    suplenciaId: number,
    payload: EarlyTerminationRequest
  ): Observable<SubstitutionDetail> {
    return this.http
      .post<SubstitutionDetail>(`${SeguridadApiService.BASE}/suplencias/${suplenciaId}/terminaciones`, payload, {
        context: context()
      })
      .pipe(map(freezeSubstitution));
  }
}

// ---------------------------------------------------------------------------
// Helpers de construcción de contexto y proyección inmutable
// ---------------------------------------------------------------------------

function context(etag?: string): HttpContext {
  const base = etag ? withEntityTag(etag) : new HttpContext();
  return base.set(REQUIRES_IDEMPOTENCY_KEY, true);
}

function requireEtag(etag: string | undefined): string {
  if (!etag) {
    throw new Error('La operación requiere el ETag de una lectura previa.');
  }
  return etag;
}

function freezeFunction(raw: MatrixFunction): MatrixFunction {
  return Object.freeze({ ...raw });
}

function freezeCombination(raw: MatrixCombination): MatrixCombination {
  return Object.freeze({ ...raw });
}

function freezeVersion(raw: MatrixVersionDetail): MatrixVersionDetail {
  return Object.freeze({
    ...raw,
    funciones: Object.freeze(raw.funciones ? [...raw.funciones] : []),
    combinaciones: Object.freeze(raw.combinaciones ? [...raw.combinaciones] : [])
  });
}

function freezeMatrixPage(raw: MatrixVersionPage): MatrixVersionPage {
  return Object.freeze({
    ...raw,
    content: Object.freeze((raw.content ?? []).map(freezeVersion))
  });
}

function freezeAssignment(raw: AssignmentDetail): AssignmentDetail {
  return Object.freeze({ ...raw });
}

function freezeProvisioning(raw: ProvisioningResult): ProvisioningResult {
  return Object.freeze({ ...raw });
}

function freezeUserStatus(raw: UserStatusResult): UserStatusResult {
  return Object.freeze({ ...raw });
}

function freezeSubstitution(raw: SubstitutionDetail): SubstitutionDetail {
  return Object.freeze({ ...raw });
}
