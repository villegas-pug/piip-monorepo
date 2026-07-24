package pe.gob.midagri.piip.portafolio.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Excepción de dominio para errores de validación de campos oficiales del portafolio.
 */
public class PortafolioValidationException extends ResponseStatusException {

    public PortafolioValidationException(String code, String detail) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, code + ": " + detail);
    }

    public static PortafolioValidationException campoRequerido(int numeroCampo, String nombreCampo) {
        return new PortafolioValidationException("OFFICIAL_FIELD_REQUIRED",
                "El campo oficial " + numeroCampo + " (" + nombreCampo + ") es obligatorio.");
    }

    public static PortafolioValidationException campoNoEditable(String nombreCampo) {
        return new PortafolioValidationException("FIELD_NOT_EDITABLE",
                "El campo " + nombreCampo + " no es editable en esta etapa.");
    }

    public static PortafolioValidationException unidadPrincipalCardinality() {
        return new PortafolioValidationException("UNIT_MAIN_CARDINALITY",
                "Debe existir exactamente una unidad principal.");
    }

    public static PortafolioValidationException responsableCardinality() {
        return new PortafolioValidationException("RESPONSIBLE_CARDINALITY",
                "Debe existir exactamente un Responsable titular.");
    }

    public static PortafolioValidationException tipoRegistroInmutable() {
        return new PortafolioValidationException("FIELD_NOT_EDITABLE",
                "El tipo de registro es inmutable después de la presentación.");
    }

    public static PortafolioValidationException prefijoNoDisponible() {
        return new PortafolioValidationException("UNIT_PREFIX_NOT_AVAILABLE",
                "La unidad principal no tiene un prefijo de código formalmente aprobado.");
    }

    public static PortafolioValidationException fuenteOtrosSinDetalle() {
        return new PortafolioValidationException("OFFICIAL_FIELD_REQUIRED",
                "Cuando la fuente es OTROS, el detalle de la fuente es obligatorio.");
    }

    public static PortafolioValidationException componenteDigitalSinDetalle() {
        return new PortafolioValidationException("OFFICIAL_FIELD_REQUIRED",
                "Cuando el componente digital es Sí, la descripción es obligatoria.");
    }

    public static PortafolioValidationException textoVacio(String nombreCampo) {
        return new PortafolioValidationException("VALIDATION_FAILED",
                "El campo " + nombreCampo + " no puede contener solo espacios.");
    }
}
