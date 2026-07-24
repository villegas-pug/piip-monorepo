package pe.gob.midagri.piip.consulta.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Excepción de dominio para errores de validación de la consulta
 * institucional. Hereda de {@link ResponseStatusException} para
 * integrarse con el advice {@code ConsultaExceptionHandler} que
 * convierte el error en un {@code application/problem+json}
 * canónico sin filtrar datos sensibles.
 */
public class ConsultaInstitucionalValidationException extends ResponseStatusException {

    public ConsultaInstitucionalValidationException(String code, String detail) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code + ": " + detail);
    }

    public static ConsultaInstitucionalValidationException filtrosInvalidos(String detalle) {
        return new ConsultaInstitucionalValidationException("CONSULTA_FILTROS_INVALIDOS", detalle);
    }

    public static ConsultaInstitucionalValidationException sinAsignacionEfectiva() {
        return new ConsultaInstitucionalValidationException("CONSULTA_ASIGNACION_REQUERIDA",
                "La consulta institucional exige una asignación efectiva autorizada.");
    }
}
