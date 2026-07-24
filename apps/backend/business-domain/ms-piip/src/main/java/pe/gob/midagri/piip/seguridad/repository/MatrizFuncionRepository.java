package pe.gob.midagri.piip.seguridad.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionEntity;

public interface MatrizFuncionRepository extends JpaRepository<MatrizFuncionEntity, Long> {
    List<MatrizFuncionEntity> findByVersionIdOrderByCodigoAsc(Long versionId);
}
