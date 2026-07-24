package pe.gob.midagri.piip.seguridad.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import pe.gob.midagri.piip.seguridad.entity.OperacionAprovisionamientoEntity;

public interface OperacionAprovisionamientoRepository extends JpaRepository<OperacionAprovisionamientoEntity, Long> {
    Optional<OperacionAprovisionamientoEntity> findByClaveIdempotente(String claveIdempotente);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OperacionAprovisionamientoEntity o where o.claveIdempotente = :clave")
    Optional<OperacionAprovisionamientoEntity> findByClaveIdempotenteForUpdate(@Param("clave") String clave);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OperacionAprovisionamientoEntity o where o.id = :id")
    Optional<OperacionAprovisionamientoEntity> findByIdForUpdate(@Param("id") Long id);
}
