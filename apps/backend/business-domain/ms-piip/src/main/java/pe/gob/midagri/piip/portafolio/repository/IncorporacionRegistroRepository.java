package pe.gob.midagri.piip.portafolio.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.portafolio.entity.IncorporacionRegistroEntity;

public interface IncorporacionRegistroRepository extends JpaRepository<IncorporacionRegistroEntity, Long> {

    Optional<IncorporacionRegistroEntity> findByHashOriginalAndResponsableIdAndFuente(String hashOriginal, Long responsableId, String fuente);
}
