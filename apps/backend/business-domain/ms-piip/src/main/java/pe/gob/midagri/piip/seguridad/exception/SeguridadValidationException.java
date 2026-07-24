package pe.gob.midagri.piip.seguridad.exception;

/**
 * Excepción de validación de negocio del módulo seguridad.
 * Se traduce a 422 (Unprocessable Entity) con el código de negocio
 * definido en la propiedad {@code code}.
 */
public class SeguridadValidationException extends RuntimeException {

    private final String code;

    public SeguridadValidationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
