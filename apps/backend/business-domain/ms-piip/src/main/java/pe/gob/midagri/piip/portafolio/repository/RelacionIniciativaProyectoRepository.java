package pe.gob.midagri.piip.portafolio.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.portafolio.entity.RelacionIniciativaProyectoEntity;

public interface RelacionIniciativaProyectoRepository extends JpaRepository<RelacionIniciativaProyectoEntity, Long> {

    Optional<RelacionIniciativaProyectoEntity> findByIniciativaId(Long iniciativaId);

    Optional<RelacionIniciativaProyectoEntity> findByProyectoId(Long proyectoId);

    boolean existsByIniciativaId(Long iniciativaId);

    boolean existsByProyectoId(Long proyectoId);
}
