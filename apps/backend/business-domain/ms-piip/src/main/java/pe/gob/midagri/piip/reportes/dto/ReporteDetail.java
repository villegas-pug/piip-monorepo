package pe.gob.midagri.piip.reportes.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalle completo de un reporte institucional
 * (US8, BR-128). Combina metadatos del reporte,
 * del snapshot y de los archivos. La versión de
 * datos corresponde al snapshot; el ETag se
 * calcula sobre la fila del reporte y sobre la
 * aprobación más reciente, siguiendo el patrón
 * de concurrencia optimista ya usado en
 * seguimiento y cierre.
 */
public record ReporteDetail(
        Long idReporte,
        String tipo,
        Integer anio,
        Integer semestre,
        String periodo,
        LocalDate fechaCorte,
        Integer versionDatos,
        String estadoTecnico,
        String clasificacion,
        String hashSnapshot,
        Long idSnapshot,
        Long idGenerador,
        LocalDateTime fechaGeneracion,
        ReportFiltros filtros,
        List<IndicadorReporte> indicadores,
        List<TotalDimension> totales,
        List<ReporteArchivoSummary> archivos,
        String etag) {
}
