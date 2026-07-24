package pe.gob.midagri.piip.portafolio.evaluacion;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AplicabilidadCriterioRepository extends JpaRepository<AplicabilidadCriterioEntity, Long> {

    /**
     * Lista los criterios estructurados de una aplicabilidad ordenados
     * por su campo ORDEN ascendente para preservar el orden canonico
     * declarado por el Evaluador.
     */
    List<AplicabilidadCriterioEntity> findByAplicabilidadIdOrderByOrdenAsc(Long aplicabilidadId);
}
