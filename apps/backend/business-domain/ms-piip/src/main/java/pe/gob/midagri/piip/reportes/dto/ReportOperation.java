package pe.gob.midagri.piip.reportes.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Respuesta 202 de las operaciones de generación
 * (US8, BR-128). El reporte se considera generado
 * cuando el snapshot CLOB JSON, los archivos PDF y
 * XLSX y el estado técnico
 * {@code GENERADA} quedan persistidos de forma
 * atómica. La aprobación posterior es independiente.
 */
public record ReportOperation(
        Long reporteId,
        String operacionId,
        LocalDate corte,
        Integer versionDatos,
        String estadoTecnico,
        String clasificacion,
        String hashSnapshot,
        LocalDateTime fechaGeneracion) {
}
