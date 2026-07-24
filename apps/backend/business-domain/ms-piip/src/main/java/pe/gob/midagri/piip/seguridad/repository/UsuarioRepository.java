package pe.gob.midagri.piip.seguridad.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.seguridad.entity.UsuarioEntity;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Long> {
    Optional<UsuarioEntity> findByKeycloakId(String keycloakId);

    Optional<UsuarioEntity> findByCorreo(String correo);
}
