package pe.gob.midagri.piip.consulta.dto;

import pe.gob.midagri.piip.consulta.dto.TipoRegistroConsulta;

/**
 * DTO de filtros admitidos por la consulta pública anónima. La
 * especificación reduce al mínimo la superficie de filtros para
 * evitar inferencias sobre datos personales o restricted; solo se
 * aceptan filtros por tipo de registro, código exacto y nombre
 * parcial. No se admite filtrar por responsable, unidad, fuente,
 * ni rango de fechas.
 */
public record PublicPortfolioQuery(
        TipoRegistroConsulta tipo,
        String codigo,
        String nombre,
        int page,
        int size) { }
