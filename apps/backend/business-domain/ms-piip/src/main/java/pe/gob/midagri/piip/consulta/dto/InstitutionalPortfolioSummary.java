package pe.gob.midagri.piip.consulta.dto;

import java.time.LocalDate;

import pe.gob.midagri.piip.consulta.dto.TipoRegistroConsulta;

/**
 * Resumen canónico de un registro del portafolio para la consulta
 * institucional por ámbito y clasificación. DTO propio del módulo
 * {@code consulta}: nunca expone entidades JPA ni objetos
 * auxiliares de otros módulos. La privacidad de los campos se
 * aplica a nivel de servicio antes de emitir la respuesta.
 */
public record InstitutionalPortfolioSummary(
        Long id,
        TipoRegistroConsulta tipoRegistro,
        String codigo,
        String codigoOrigen,
        String nombre,
        String estado,
        LocalDate fechaInicio,
        Long unidadEjecutoraId,
        String unidadEjecutoraDescripcion,
        String unidadEjecutoraAbreviatura,
        Long responsableId,
        boolean puedeVerResponsable,
        Long version,
        String etag) { }
