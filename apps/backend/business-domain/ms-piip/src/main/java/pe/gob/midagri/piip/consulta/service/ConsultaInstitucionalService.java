package pe.gob.midagri.piip.consulta.service;

import java.util.Optional;

import pe.gob.midagri.piip.consulta.dto.ConsultaInstitucionalAuthContext;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioDetail;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioPage;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioQuery;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioSummary;

/**
 * Caso de uso del módulo {@code consulta} para la consulta
 * institucional del portafolio. El servicio valida la
 * autorización efectiva contra Oracle, aplica la matriz de
 * privacidad por ámbito y devuelve únicamente DTOs propios del
 * módulo. Nunca expone entidades JPA ni contenido/descarga
 * documental.
 */
public interface ConsultaInstitucionalService {

    /**
     * Busca registros del portafolio aplicando los filtros
     * declarados y la paginación canónica. La respuesta limita los
     * resultados al ámbito del actor y a las unidades
     * explícitamente autorizadas.
     */
    ResultadoConsulta buscar(InstitutionalPortfolioQuery consulta, ConsultaInstitucionalAuthContext contexto);

    /**
     * Recupera el detalle institucional de un registro. Si el
     * registro no pertenece al ámbito del actor, devuelve
     * {@link Optional#empty()} para responder como no visible sin
     * confirmar la existencia.
     */
    Optional<DetalleConsulta> obtenerDetalle(Long id, ConsultaInstitucionalAuthContext contexto);

    /**
     * Resultado de la búsqueda con la página canónica y el ETag
     * agregado para la cabecera HTTP.
     */
    record ResultadoConsulta(InstitutionalPortfolioPage page, java.util.List<InstitutionalPortfolioSummary> items) { }

    /**
     * Detalle de un registro con su ETag y la indicación de
     * visibilidad para la cabecera HTTP.
     */
    record DetalleConsulta(InstitutionalPortfolioDetail detalle, boolean visible) { }
}
