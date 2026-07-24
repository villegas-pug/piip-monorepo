package pe.gob.midagri.piip.organizacion.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.organizacion.entity.ObjetivoPeiVersionEntity;
public interface ObjetivoPeiVersionRepository extends JpaRepository<ObjetivoPeiVersionEntity, Long> {
    boolean existsByCodigoVersion(String codigoVersion);
    Optional<ObjetivoPeiVersionEntity> findFirstByOrderByIdDesc();
}
