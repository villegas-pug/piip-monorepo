package pe.gob.midagri.piip.reportes.dto;

import java.time.LocalDateTime;

/**
 * Destinatario aprobado dentro de un reporte
 * institucional. Se serializa como parte del
 * detalle de aprobación y como entrada de la
 * remisión (la remisión solo se permite contra
 * destinatarios previamente aprobados).
 */
public record DestinatarioReporteDetail(
        Long idDestinatario,
        Long idAprobacion,
        String tipoDestinatario,
        Long idEntidad,
        String nombre) {
}
