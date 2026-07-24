package pe.gob.midagri.piip.reportes.dto;

import java.util.List;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para registrar la remisión
 * manual de un reporte aprobado (US8, BR-128).
 * El servicio exige:
 * <ul>
 *   <li>{@code idVersion}: versión aprobada
 *       previamente. Una versión distinta de
 *       la aprobada produce 409
 *       {@code REPORT_VERSION_NOT_APPROVED}.</li>
 *   <li>{@code destinatariosIds}: destinatarios
 *       aprobados en esa versión; remitir a un
 *       destinatario no aprobado produce 422
 *       {@code RECIPIENT_NOT_APPROVED}.</li>
 *   <li>{@code resultado}: estado declarado por
 *       la Oficina de Modernización; la
 *       remisión automática no existe.</li>
 *   <li>{@code motivo}: obligatorio cuando el
 *       resultado es FALLIDA.</li>
 * </ul>
 */
public record ReporteRemisionRequest(
        @NotNull @Min(1) Integer idVersion,
        @NotNull @Size(min = 1, max = 50) List<Long> destinatariosIds,
        @NotBlank @Size(max = 20) String resultado,
        @Size(max = 2000) String motivo) {
}
