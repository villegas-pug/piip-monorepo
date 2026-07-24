package pe.gob.midagri.piip.consulta.dto;

import java.time.LocalDateTime;

/**
 * Metadatos públicos de un documento elegible para consulta
 * pública. Solo expone los cinco campos públicos aprobados:
 * tipo documental, título sin datos personales, versión, formato y
 * fecha de publicación del servidor. Nunca incluye el BLOB, la
 * clave física ni una URL de descarga.
 */
public record PublicPortfolioDocumento(
        String tipoDocumental,
        String tituloPublico,
        Integer version,
        String formato,
        LocalDateTime fechaPublicacion) { }
