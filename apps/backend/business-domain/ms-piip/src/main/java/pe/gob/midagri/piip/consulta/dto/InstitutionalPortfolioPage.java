package pe.gob.midagri.piip.consulta.dto;

import java.util.List;

/**
 * Página canónica de resultados para la consulta institucional.
 * DTO propio del módulo {@code consulta}; no expone entidades
 * JPA y respeta el contrato de paginación PIIP (page, size,
 * totalElements, totalPages). El ETag agregado del bloque se
 * calcula a partir de los ETag individuales y se transmite en
 * la cabecera HTTP.
 */
public record InstitutionalPortfolioPage(
        List<InstitutionalPortfolioSummary> items,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas,
        String etag) { }
