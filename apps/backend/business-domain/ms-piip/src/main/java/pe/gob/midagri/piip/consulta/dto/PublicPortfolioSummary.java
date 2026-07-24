package pe.gob.midagri.piip.consulta.dto;

import java.time.LocalDate;
import java.util.List;

import pe.gob.midagri.piip.consulta.dto.TipoRegistroConsulta;

/**
 * Resumen público canónico de un registro del portafolio para la
 * consulta anónima minimizada. Solo expone los cuatro campos
 * públicos aprobados por la constitución:
 * {@code Tipo de registro}, {@code Código}, {@code Nombre} y
 * {@code Estado}, además de metadatos de publicaciones elegibles
 * ({@code tipoDocumental}, {@code tituloPublico}, {@code version},
 * {@code formato} y {@code fechaPublicacion}).
 *
 * <p>El DTO no expone el responsable, la unidad ejecutora, el
 * contenido ni ningún dato personal. Las proyecciones públicas
 * excluyen la iniciativa archivada, la iniciativa no admisible y
 * los proyectos no publicables.
 */
public record PublicPortfolioSummary(
        Long id,
        TipoRegistroConsulta tipoRegistro,
        String codigo,
        String nombre,
        String estado,
        LocalDate fechaInicio,
        List<PublicPortfolioDocumento> publicaciones,
        String etag) { }
