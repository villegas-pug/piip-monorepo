package pe.gob.midagri.piip.consulta.service;

import java.util.Optional;

import pe.gob.midagri.piip.consulta.dto.PublicPortfolioDetail;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioPage;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioQuery;

/**
 * Autoridad única para la proyección pública minimizada del
 * portafolio. La consulta es anónima y se limita a los cuatro
 * campos públicos y a los metadatos de publicaciones elegibles.
 * El módulo no consulta contenido documental ni genera URL de
 * descarga: la constitución prohíbe ambas acciones durante la
 * Fase 1.
 */
public interface ConsultaPublicaService {

    /**
     * Busca registros del portafolio aplicando los filtros
     * públicos admitidos y la paginación. La respuesta nunca
     * expone campos restringidos ni el responsable, aunque sea
     * público por confusión.
     */
    PublicPortfolioPage buscar(PublicPortfolioQuery consulta);

    /**
     * Devuelve el detalle público de un registro del portafolio.
     * Si el registro no es elegible para consulta pública, se
     * devuelve {@link Optional#empty()} y el controlador
     * responderá 404 sin confirmar su existencia.
     */
    Optional<PublicPortfolioDetail> obtenerDetalle(Long id);
}
