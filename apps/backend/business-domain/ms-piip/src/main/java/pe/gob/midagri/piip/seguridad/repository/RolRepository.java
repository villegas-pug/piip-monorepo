package pe.gob.midagri.piip.seguridad.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.seguridad.entity.RolEntity;

public interface RolRepository extends JpaRepository<RolEntity, Integer> {
    Optional<RolEntity> findByNombre(String nombre);
}
