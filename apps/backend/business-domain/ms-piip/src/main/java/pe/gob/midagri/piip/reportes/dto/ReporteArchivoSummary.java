package pe.gob.midagri.piip.reportes.dto;

import java.time.LocalDateTime;

/**
 * Resumen de un archivo emitido (PDF/XLSX) de un
 * reporte. No expone el BLOB: el contenido se
 * descarga por el endpoint dedicado
 * {@code GET /reportes/generaciones/{id}/archivos/\{PDF|XLSX\}}.
 */
public record ReporteArchivoSummary(
        Long idArchivo,
        String formato,
        Integer version,
        String hashSha256,
        Long idDocumentoVersion,
        String creadoPor,
        LocalDateTime fechaCreacion) {
}
