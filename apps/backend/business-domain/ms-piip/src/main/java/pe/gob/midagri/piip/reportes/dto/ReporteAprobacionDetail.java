package pe.gob.midagri.piip.reportes.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalle de una aprobación registrada por la
 * Oficina de Modernización (US8, BR-127,
 * BR-128). Una aprobación siempre fija la
 * versión exacta y los destinatarios; la
 * remisión posterior se hace contra esta
 * aprobación.
 */
public record ReporteAprobacionDetail(
        Long idAprobacion,
        Long idReporte,
        Integer idVersion,
        Long idOficina,
        Long idAprobador,
        Long idDocumentoAprobacion,
        LocalDateTime fechaAprobacion,
        List<DestinatarioReporteDetail> destinatarios) {
}
