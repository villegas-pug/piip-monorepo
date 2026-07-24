package pe.gob.midagri.piip.seguridad.exception;

/**
 * Falla operativa de Keycloak que no admite compensación automática.
 * Se traduce a 502 con el código {@code KEYCLOAK_OPERATION_FAILED} y
 * nunca expone contraseñas, tokens ni secretos de cliente.
 */
public class KeycloakOperationException extends RuntimeException {

    public KeycloakOperationException(String mensaje) {
        super(mensaje);
    }

    public KeycloakOperationException(String mensaje, Throwable cause) {
        super(mensaje, cause);
    }
}
