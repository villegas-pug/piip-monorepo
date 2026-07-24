package pe.gob.midagri.piip.seguridad.service;

/**
 * Contrato del adaptador de Keycloak Admin API limitado al ciclo de
 * identidad constitucional. La implementación reside exclusivamente en
 * {@code seguridad/service/impl/} y es el único punto del monolito
 * modular con acceso a {@code org.keycloak.*}. Ningún método acepta
 * contraseñas, tokens ni atributos sensibles fuera de este contrato.
 */
public interface KeycloakAdminService {

    /**
     * Crea la identidad deshabilitada en Keycloak con acción de verificación
     * de correo y devuelve el {@code sub} generado. La activación depende
     * exclusivamente de la aceptación de la acción de correo.
     *
     * @param correo              correo institucional válido
     * @param nombreCompleto      nombre completo de la persona
     * @return identificador Keycloak y confirmación de envío del correo
     * @throws KeycloakOperationException si Keycloak rechaza la operación
     */
    KeycloakUserCreation crearUsuarioDeshabilitado(String correo, String nombreCompleto);

    /**
     * Bloquea la identidad en Keycloak conservando los registros locales
     * para auditoría. La operación es idempotente si el usuario ya está
     * deshabilitado.
     *
     * @param keycloakId identificador {@code sub} devuelto por Keycloak
     * @throws KeycloakOperationException si Keycloak rechaza la operación
     */
    void desactivarUsuario(String keycloakId);

    /**
     * Reactiva la identidad en Keycloak. No restaura asignaciones
     * revocadas ni vencidas en PIIP.
     *
     * @param keycloakId identificador {@code sub} devuelto por Keycloak
     * @throws KeycloakOperationException si Keycloak rechaza la operación
     */
    void reactivarUsuario(String keycloakId);

    /**
     * Resultado inmutable de la creación en Keycloak.
     *
     * @param keycloakId   identificador {@code sub} devuelto por Keycloak
     * @param emailEnviado indica si la acción de verificación de correo
     *                     fue solicitada correctamente
     */
    record KeycloakUserCreation(String keycloakId, boolean emailEnviado) {
    }
}
