package pe.gob.midagri.piip.consulta.dto;

import java.util.List;

/**
 * Detalle público de un registro del portafolio. Conserva los
 * cuatro campos públicos, los metadatos de las publicaciones
 * elegibles y la ETag optimista. No expone responsable, unidades,
 * notas, ni datos sensibles de cualquier tipo. La constitución
 * exige esta minimización para la consulta anónima.
 */
public record PublicPortfolioDetail(
        Long id,
        TipoRegistroConsulta tipoRegistro,
        String codigo,
        String nombre,
        String estado,
        List<PublicPortfolioDocumento> publicaciones,
        String etag) { }
