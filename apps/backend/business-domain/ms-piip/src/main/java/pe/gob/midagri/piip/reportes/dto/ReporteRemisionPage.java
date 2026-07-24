package pe.gob.midagri.piip.reportes.dto;

import java.util.List;

/**
 * Envoltorio que devuelve todas las remisiones
 * registradas de un reporte. Permite al cliente
 * reconstruir el historial y, en escenarios
 * recuperables, reejecutar el comando de remisión
 * con la misma clave de idempotencia sin
 * duplicar filas.
 */
public record ReporteRemisionPage(
        Long idReporte,
        Integer idVersion,
        List<ReporteRemisionDetail> remisiones) {
}
