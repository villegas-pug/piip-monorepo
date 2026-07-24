package pe.gob.midagri.piip.reportes.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para registrar la aprobación formal
 * de un reporte (US8, BR-127). La Oficina de
 * Modernización fija la versión exacta, el
 * documento de aprobación y los destinatarios
 * permitidos. Una segunda aprobación para la
 * misma versión produce 409
 * {@code REPORT_VERSION_ALREADY_APPROVED}; remitir
 * una versión no aprobada produce 409
 * {@code REPORT_VERSION_NOT_APPROVED}.
 */
public record ReporteAprobacionRequest(
        @NotNull @Min(1) Integer idVersion,
        @NotNull Long idDocumentoAprobacion,
        @NotNull @Size(min = 1, max = 50) @Valid
        List<DestinatarioReporteRequest> destinatarios) {
}
