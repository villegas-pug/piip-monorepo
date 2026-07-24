package pe.gob.midagri.piip.portafolio.evaluacion;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EvaluacionIniciativaRepository extends JpaRepository<EvaluacionIniciativaEntity, Long> {

    /**
     * Recupera la unica evaluacion de admisibilidad (UK_EI_INICIATIVA)
     * asociada a la iniciativa. La fila es inmutable: cualquier correccion
     * de la opinion tecnica crea una nueva version, no se actualiza esta.
     */
    Optional<EvaluacionIniciativaEntity> findByIniciativaId(Long iniciativaId);
}
