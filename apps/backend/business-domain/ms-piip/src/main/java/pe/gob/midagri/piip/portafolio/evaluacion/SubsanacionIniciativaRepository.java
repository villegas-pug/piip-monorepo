package pe.gob.midagri.piip.portafolio.evaluacion;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SubsanacionIniciativaRepository extends JpaRepository<SubsanacionIniciativaEntity, Long> {

    /**
     * Recupera la subsanacion unica (UK_SI_INICIATIVA) para una iniciativa.
     * Si la iniciativa ya fue subsanada, retorna la fila persistida con su
     * fecha de atencion registrada.
     */
    Optional<SubsanacionIniciativaEntity> findByIniciativaId(Long iniciativaId);
}
