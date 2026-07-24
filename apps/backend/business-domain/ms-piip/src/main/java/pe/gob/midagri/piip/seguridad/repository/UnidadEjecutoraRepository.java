package pe.gob.midagri.piip.seguridad.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.seguridad.entity.UnidadEjecutoraEntity;

public interface UnidadEjecutoraRepository extends JpaRepository<UnidadEjecutoraEntity, Long> {
    Optional<UnidadEjecutoraEntity> findByCodigo(String codigo);
}
