package pe.gob.midagri.piip.reportes.dto;

import java.time.LocalDateTime;

/**
 * Detalle de una remisión registrada (US8,
 * BR-128). Se conserva con fines de auditoría;
 * no se trata de un mensaje enviado: el
 * contrato no implementa correo, PIDE ni
 * sincronización externa (BR-148).
 */
public record ReporteRemisionDetail(
        Long idRemision,
        Long idReporte,
        Long idDestinatario,
        String resultado,
        String motivo,
        LocalDateTime fechaRemision) {
}
