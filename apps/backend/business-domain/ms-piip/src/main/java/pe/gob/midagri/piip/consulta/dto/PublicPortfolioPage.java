package pe.gob.midagri.piip.consulta.dto;

import java.util.List;

/**
 * DTO de respuesta paginada de la consulta pública anónima. El
 * módulo no expone totales, filtros, asignaciones ni datos
 * sensibles; solo se devuelven los elementos públicos filtrados y
 * la paginación es opcional y mínima para evitar filtraciones de
 * volumen.
 */
public record PublicPortfolioPage(
        List<PublicPortfolioSummary> items,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        String etag) { }
