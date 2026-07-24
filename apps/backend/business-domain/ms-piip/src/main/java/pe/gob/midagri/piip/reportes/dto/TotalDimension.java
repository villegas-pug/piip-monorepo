package pe.gob.midagri.piip.reportes.dto;

import java.util.List;

/**
 * Resumen por dimensión exigido por BR-121 y
 * BR-123: totales por tipo, estado, unidad, fuente,
 * tipo de solución, producto y cierre. Se serializa
 * como un mapa dimensión/valor/etiqueta en el
 * snapshot JSON canónico.
 */
public record TotalDimension(
        String dimension,
        List<TotalDimensionItem> items) {
}
