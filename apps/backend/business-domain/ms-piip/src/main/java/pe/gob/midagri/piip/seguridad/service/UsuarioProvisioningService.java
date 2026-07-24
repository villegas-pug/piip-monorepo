package pe.gob.midagri.piip.seguridad.service;

import pe.gob.midagri.piip.seguridad.dto.CreateUserRequest;
import pe.gob.midagri.piip.seguridad.dto.ProvisioningAuthContext;
import pe.gob.midagri.piip.seguridad.dto.ProvisioningResult;
import pe.gob.midagri.piip.seguridad.dto.UserStatusRequest;
import pe.gob.midagri.piip.seguridad.dto.UserStatusResult;

/**
 * Servicio de aplicación del ciclo ordinario de aprovisionamiento y
 * ciclo de vida del usuario institucional. La implementación reside en
 * {@code seguridad/service/impl/} y aplica la autorización efectiva
 * Oracle, la auditoría inmutable, la idempotencia contractual y los
 * límites del adaptador de Keycloak.
 */
public interface UsuarioProvisioningService {

    /**
     * Aprovisiona Keycloak primero y registra el usuario en Oracle PIIP.
     * Si Keycloak tiene éxito y Oracle falla, conserva la identidad
     * deshabilitada y una operación recuperable auditada para reintento
     * sin duplicar la identidad.
     *
     * @param request  solicitud de creación
     * @param contexto contexto de autorización efectivo
     * @return resultado del aprovisionamiento con estado recuperable
     */
    ProvisioningResult crear(CreateUserRequest request, ProvisioningAuthContext contexto);

    /**
     * Consulta el estado actual de una operación de aprovisionamiento
     * revalidando la autorización efectiva contra Oracle.
     *
     * @param operacionId identificador de la operación
     * @param contexto    contexto de autorización efectivo
     * @return estado actual de la operación
     */
    ProvisioningResult consultar(Long operacionId, ProvisioningAuthContext contexto);

    /**
     * Reintenta una operación recuperable (estado
     * {@code KEYCLOAK_CREADO_DESHABILITADO} u {@code ORACLE_PENDIENTE})
     * revalidando la autorización efectiva y conservando el {@code sub}
     * original sin crear otra identidad.
     *
     * @param operacionId identificador de la operación
     * @param contexto    contexto de autorización efectivo
     * @return estado actualizado de la operación
     */
    ProvisioningResult reintentar(Long operacionId, ProvisioningAuthContext contexto);

    /**
     * Desactiva el usuario en Keycloak y PIIP de forma inmediata,
     * conservando los registros y asignaciones locales para auditoría.
     *
     * @param usuarioId identificador PIIP del usuario objetivo
     * @param request   motivo obligatorio
     * @param contexto  contexto de autorización efectivo
     * @return estado del usuario
     */
    UserStatusResult desactivar(Long usuarioId, UserStatusRequest request, ProvisioningAuthContext contexto);

    /**
     * Reactiva el usuario en Keycloak y PIIP sin restaurar asignaciones
     * revocadas o vencidas. La autoridad debe coincidir con la que autorizó
     * la desactivación.
     *
     * @param usuarioId identificador PIIP del usuario objetivo
     * @param request   motivo obligatorio
     * @param contexto  contexto de autorización efectivo
     * @return estado del usuario
     */
    UserStatusResult reactivar(Long usuarioId, UserStatusRequest request, ProvisioningAuthContext contexto);
}
