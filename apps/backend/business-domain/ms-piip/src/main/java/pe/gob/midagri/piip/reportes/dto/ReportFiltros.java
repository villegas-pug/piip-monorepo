package pe.gob.midagri.piip.reportes.dto;

import java.util.List;

import jakarta.validation.constraints.Size;

/**
 * Filtros aprobados por BR-123 para un reporte
 * configurable. Todos los campos son opcionales: el
 * reporte se evalúa sobre la intersección de los
 * filtros presentes, sin exceder el ámbito
 * organizacional del generador.
 */
public record ReportFiltros(
        @Size(max = 30) String tipo,
        @Size(max = 30) String estado,
        Long unidadId,
        Long responsableId,
        @Size(max = 30) String fuente,
        @Size(max = 30) String tipoSolucion,
        @Size(max = 40) String producto,
        List<@Size(max = 30) String> unidadesAdicionales) {
}
