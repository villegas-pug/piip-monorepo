package pe.gob.midagri.piip.consulta.dto;

import java.time.LocalDate;

/**
 * Unidad responsable visible en la consulta institucional.
 * Proyección controlada a partir de la entidad
 * {@code UnidadResponsableEntity} y del catálogo de unidades
 * ejecutoras del módulo {@code seguridad}; nunca expone
 * entidades JPA.
 */
public record InstitutionalPortfolioUnidad(
        Long id,
        Long unidadId,
        String descripcion,
        String abreviatura,
        Integer nroOrden,
        boolean principal) { }
