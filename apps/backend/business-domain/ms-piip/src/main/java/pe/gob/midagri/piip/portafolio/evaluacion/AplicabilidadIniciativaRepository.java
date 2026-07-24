package pe.gob.midagri.piip.portafolio.evaluacion;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AplicabilidadIniciativaRepository extends JpaRepository<AplicabilidadIniciativaEntity, Long> {

    /**
     * Recupera la unica aplicabilidad (UK_AI_INICIATIVA) registrada para
     * la iniciativa. Es una operacion independiente de la admision y no
     * se mezcla con NO_ADMISIBLE.
     */
    Optional<AplicabilidadIniciativaEntity> findByIniciativaId(Long iniciativaId);
}
