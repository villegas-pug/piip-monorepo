package pe.gob.midagri.piip.portafolio.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;
import pe.gob.midagri.piip.portafolio.entity.SecuenciaCodigoEntity;

public interface SecuenciaCodigoRepository extends JpaRepository<SecuenciaCodigoEntity, Long> {

    @Query("SELECT s FROM SecuenciaCodigoEntity s WHERE s.anio = :anio AND s.unidadId = :unidadId")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SecuenciaCodigoEntity> findForUpdate(Integer anio, Long unidadId);

    Optional<SecuenciaCodigoEntity> findByAnioAndUnidadId(Integer anio, Long unidadId);
}
