package pe.gob.midagri.piip.reportes.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Solicitud HTTP para generar el reporte institucional
 * semestral (US8, BR-013, BR-121). El servidor deriva
 * el periodo y la fecha de corte a partir de
 * {@code anio} y {@code semestre}: 30/06 o 31/12.
 * No se acepta una fecha de corte distinta.
 */
public record SemestralReportRequest(
        @NotNull @Min(2000) @Max(2100) Integer anio,
        @NotNull @Min(1) @Max(2) Integer semestre) {
}
