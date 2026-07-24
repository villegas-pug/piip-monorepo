package pe.gob.midagri.piip.reportes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Destinatario propuesto por la Oficina de
 * Modernización para un reporte aprobado. El
 * {@code tipoDestinatario} se restringe a los
 * valores del DDL 017 (CHECK
 * {@code CK_RD_TIPO_DESTINATARIO}).
 */
public record DestinatarioReporteRequest(
        @NotBlank @Size(max = 30) String tipoDestinatario,
        @NotNull Long idEntidad,
        @NotBlank @Size(max = 200) String nombre) {
}
