package pe.gob.midagri.piip.portafolio.service;

import java.util.Optional;

import pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioPage;
import pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioProjection;
import pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioQuery;

/**
 * Puerto del módulo {@code portafolio} que entrega proyecciones
 * del agregado central a la consulta institucional. Las
 * implementaciones resuelven con JPA contra Oracle PIIP y devuelven
 * únicamente DTOs del propio módulo, sin exponer entidades JPA
 * hacia {@code consulta}.
 *
 * <p>El puerto es de solo lectura: la consulta institucional no
 * muta el agregado. La autorización efectiva (perfil y ámbito) se
 * evalúa en el módulo {@code seguridad} antes de invocar este
 * servicio; la proyección se filtra por las unidades visibles
 * declaradas en {@link InstitutionalPortfolioQuery#unidadesVisibles()}.
 */
public interface InstitutionalPortfolioReader {

    /**
     * Busca registros de portafolio aplicando los filtros neutros
     * y limitando los resultados a las unidades visibles. La
     * paginación se respeta estrictamente.
     *
     * @param consulta criterios de búsqueda y paginación; nunca nulo
     * @return página de resúmenes canónicos del portafolio
     */
    InstitutionalPortfolioPage buscar(InstitutionalPortfolioQuery consulta);

    /**
     * Recupera la proyección completa de un registro por
     * identificador. Si el registro no existe o no pertenece a
     * las unidades visibles declaradas, devuelve {@link Optional#empty()}
     * para que la consulta responda como no visible sin confirmar
     * la existencia.
     *
     * @param id identificador del registro
     * @param unidadesVisibles colección de unidades autorizadas; nunca nula
     * @return proyección opcional, vacía cuando el registro está fuera de ámbito
     */
    Optional<InstitutionalPortfolioProjection> obtener(Long id, java.util.List<Long> unidadesVisibles);
}
