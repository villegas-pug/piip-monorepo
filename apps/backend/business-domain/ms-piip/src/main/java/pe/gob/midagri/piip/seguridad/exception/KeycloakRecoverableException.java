package pe.gob.midagri.piip.seguridad.exception;

/**
 * Excepción de aplicación que señala un fallo de Keycloak que mantiene
 * la identidad deshabilitada y permite reintento controlado. Se traduce
 * a 503 con el código de negocio {@code KEYCLOAK_OPERATION_RECOVERABLE}
 * y expone únicamente el {@code operacionId}; nunca incluye contraseñas,
 * tokens, secretos ni contenido del cuerpo de la respuesta de Keycloak.
 */
public class KeycloakRecoverableException extends RuntimeException {

    private final Long operacionId;

    public KeycloakRecoverableException(Long operacionId, String mensaje) {
        super(mensaje);
        this.operacionId = operacionId;
    }

    public Long getOperacionId() {
        return operacionId;
    }
}
