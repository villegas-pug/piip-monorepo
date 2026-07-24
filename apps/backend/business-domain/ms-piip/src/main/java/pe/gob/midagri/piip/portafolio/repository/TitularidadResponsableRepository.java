package pe.gob.midagri.piip.portafolio.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.gob.midagri.piip.portafolio.entity.TitularidadResponsableEntity;

public interface TitularidadResponsableRepository extends JpaRepository<TitularidadResponsableEntity, Long> {

    Optional<TitularidadResponsableEntity> findByRegistroPortafolioIdAndFinIsNull(Long registroPortafolioId);

    List<TitularidadResponsableEntity> findByRegistroPortafolioIdOrderByInicioDesc(Long registroPortafolioId);
}
