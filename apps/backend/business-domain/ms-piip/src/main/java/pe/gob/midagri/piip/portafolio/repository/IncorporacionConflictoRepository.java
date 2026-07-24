package pe.gob.midagri.piip.portafolio.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.portafolio.entity.IncorporacionConflictoEntity;

public interface IncorporacionConflictoRepository extends JpaRepository<IncorporacionConflictoEntity, Long> {

    List<IncorporacionConflictoEntity> findByIncorporacionIdAndResuelto(Long incorporacionId, String resuelto);
}
