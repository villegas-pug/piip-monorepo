package pe.gob.midagri.piip.reportes.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para generar un reporte institucional
 * extraordinario (US8, BR-120, BR-123, BR-124). La
 * generación exige:
 * <ul>
 *   <li>{@code solicitudDocumentoId}: documento formal
 *       que justifica la generación extraordinaria.</li>
 *   <li>{@code aprobacionOficinaDocumentoId}: aprobación
 *       de la Oficina de Modernización que la autoriza.
 *       Su ausencia produce
 *       {@code 422 REPORT_REQUEST_APPROVAL_REQUIRED}.</li>
 *   <li>{@code periodo}: descripción legible del periodo
 *       cubierto (no se infiere).</li>
 *   <li>{@code filtros}: filtros aprobados por BR-123. Se
 *       conservan tal como llegan; el servicio verifica
 *       que no excedan el ámbito del generador.</li>
 * </ul>
 */
public record ExtraordinarioReportRequest(
        @NotNull Long solicitudDocumentoId,
        @NotNull Long aprobacionOficinaDocumentoId,
        @NotBlank @Size(max = 30) String periodo,
        @NotNull @Size(max = 1) String fechaCorte,
        @NotNull @Valid ReportFiltros filtros) {
}
