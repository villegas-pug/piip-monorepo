package pe.gob.midagri.piip.portafolio.dto;

import java.util.List;

import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;

/**
 * Resultado de una consulta institucional por ámbito, entregado
 * por el módulo {@code portafolio} al módulo {@code consulta} como
 * DTO simple. La página canónica respeta los límites del contrato
 * público PIIP (page, size, totalElements, totalPages) y la
 * colección se compone de {@link InstitutionalPortfolioSummary}.
 */
public record InstitutionalPortfolioPage(
        List<InstitutionalPortfolioSummary> items,
        int pagina,
        int tamanio,
        long totalElementos,
        int totalPaginas) {

    /**
     * Resumen canónico de un registro del portafolio para una
     * consulta institucional. No incluye datos sensibles; los
     * campos públicos se muestran siempre y los internos solo
     * cuando el actor y su asignación efectiva pertenecen al
     * ámbito autorizado.
     */
    public record InstitutionalPortfolioSummary(
            Long id,
            TipoRegistro tipoRegistro,
            String codigo,
            String codigoOrigen,
            String nombre,
            String estado,
            java.time.LocalDate fechaInicio,
            Long unidadEjecutoraId,
            String unidadEjecutoraDescripcion,
            String unidadEjecutoraAbreviatura,
            Long responsableId,
            Long version,
            String etag) { }
}
