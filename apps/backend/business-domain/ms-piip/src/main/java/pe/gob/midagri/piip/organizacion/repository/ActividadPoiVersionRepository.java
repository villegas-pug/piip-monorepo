package pe.gob.midagri.piip.organizacion.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.organizacion.entity.ActividadPoiVersionEntity;
public interface ActividadPoiVersionRepository extends JpaRepository<ActividadPoiVersionEntity, Long> {
 boolean existsByCodigoVersion(String codigoVersion);
 Optional<ActividadPoiVersionEntity> findFirstByOrderByIdDesc();
}
