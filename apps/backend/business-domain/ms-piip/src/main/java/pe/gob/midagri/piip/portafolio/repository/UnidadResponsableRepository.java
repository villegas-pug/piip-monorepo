package pe.gob.midagri.piip.portafolio.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.portafolio.entity.UnidadResponsableEntity;

public interface UnidadResponsableRepository extends JpaRepository<UnidadResponsableEntity, Long> {

    List<UnidadResponsableEntity> findByRegistroPortafolioId(Long registroPortafolioId);

    long countByRegistroPortafolioIdAndNroOrden(Long registroPortafolioId, Integer nroOrden);
}
