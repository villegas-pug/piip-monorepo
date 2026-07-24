package pe.gob.midagri.piip.documentos.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.documentos.entity.TipoDocumentoEntity;

public interface TipoDocumentoRepository extends JpaRepository<TipoDocumentoEntity, Integer> {
    Optional<TipoDocumentoEntity> findByIdAndActivo(Integer id, String activo);
}
