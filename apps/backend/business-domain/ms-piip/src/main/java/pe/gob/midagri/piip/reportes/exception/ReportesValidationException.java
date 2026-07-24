package pe.gob.midagri.piip.reportes.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Excepciones de dominio del módulo reportes.
 * Centraliza los códigos canónicos exigidos por
 * la constitución y el contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/reportes.md}.
 */
public final class ReportesValidationException extends ResponseStatusException {

    private ReportesValidationException(HttpStatus status, String code, String detail) {
        super(status, code + ": " + detail);
    }

    public static ReportesValidationException semesterInvalido() {
        return new ReportesValidationException(HttpStatus.UNPROCESSABLE_ENTITY,
                "SEMESTER_INVALID",
                "El semestre debe ser 1 (enero-junio) o 2 (julio-diciembre).");
    }

    public static ReportesValidationException fechaCorteInvalida() {
        return new ReportesValidationException(HttpStatus.UNPROCESSABLE_ENTITY,
                "REPORT_CUTOFF_INVALID",
                "La fecha de corte no coincide con el semestre o ano del reporte.");
    }

    public static ReportesValidationException aprobacionRequerida() {
        return new ReportesValidationException(HttpStatus.UNPROCESSABLE_ENTITY,
                "REPORT_REQUEST_APPROVAL_REQUIRED",
                "El reporte extraordinario exige solicitud y aprobacion documentadas.");
    }

    public static ReportesValidationException perfilNoAutorizado(String perfil) {
        return new ReportesValidationException(HttpStatus.FORBIDDEN,
                "REPORT_SCOPE_DENIED",
                "La operacion de reportes exige perfil Evaluador; perfil efectivo: "
                        + (perfil == null ? "null" : perfil));
    }

    public static ReportesValidationException alcanceUnidadNoAutorizado() {
        return new ReportesValidationException(HttpStatus.FORBIDDEN,
                "REPORT_SCOPE_DENIED",
                "La unidad efectiva no cubre el ambito institucional autorizado.");
    }

    public static ReportesValidationException reporteNoEncontrado() {
        return new ReportesValidationException(HttpStatus.NOT_FOUND,
                "REPORT_NOT_FOUND",
                "El reporte solicitado no existe.");
    }

    public static ReportesValidationException versionNoAprobada() {
        return new ReportesValidationException(HttpStatus.CONFLICT,
                "REPORT_VERSION_NOT_APPROVED",
                "La version remitida no coincide con una aprobacion vigente.");
    }

    public static ReportesValidationException versionYaAprobada() {
        return new ReportesValidationException(HttpStatus.CONFLICT,
                "REPORT_VERSION_ALREADY_APPROVED",
                "La version indicada ya tiene una aprobacion registrada.");
    }

    public static ReportesValidationException destinatarioNoAprobado() {
        return new ReportesValidationException(HttpStatus.UNPROCESSABLE_ENTITY,
                "RECIPIENT_NOT_APPROVED",
                "El destinatario no forma parte de la aprobacion de la version.");
    }

    public static ReportesValidationException motivoObligatorioFallo() {
        return new ReportesValidationException(HttpStatus.UNPROCESSABLE_ENTITY,
                "REMITTAL_MOTIVE_REQUIRED",
                "La remision con resultado FALLIDA exige un motivo.");
    }

    public static ReportesValidationException formatoInvalido(String formato) {
        return new ReportesValidationException(HttpStatus.UNPROCESSABLE_ENTITY,
                "REPORT_FORMAT_INVALID",
                "El formato '" + formato + "' no esta soportado. Use PDF o XLSX.");
    }

    public static ReportesValidationException archivoNoEncontrado() {
        return new ReportesValidationException(HttpStatus.NOT_FOUND,
                "REPORT_FILE_NOT_FOUND",
                "El archivo del reporte no esta disponible para el formato solicitado.");
    }

    public static ReportesValidationException cuerpoObligatorio() {
        return new ReportesValidationException(HttpStatus.UNPROCESSABLE_ENTITY,
                "VALIDATION_FAILED",
                "El cuerpo de la solicitud es obligatorio.");
    }
}
