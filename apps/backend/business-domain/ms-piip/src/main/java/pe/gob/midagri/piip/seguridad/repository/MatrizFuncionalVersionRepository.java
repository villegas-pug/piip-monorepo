package pe.gob.midagri.piip.seguridad.repository;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionalVersionEntity;

public interface MatrizFuncionalVersionRepository extends JpaRepository<MatrizFuncionalVersionEntity, Long> {
    Optional<MatrizFuncionalVersionEntity> findByCodigoVersion(String codigoVersion);
    Page<MatrizFuncionalVersionEntity> findAllByOrderByIdDesc(Pageable pageable);
    boolean existsByVersionAnteriorId(Long versionAnteriorId);
}
