package pe.gob.midagri.piip.portafolio.seguimiento.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.PresentacionProductoFinalEvidenciaEntity;

/** Acceso JPA a la relación append-only creada por el DDL 025. */
@Repository
public interface PresentacionProductoFinalEvidenciaRepository
        extends JpaRepository<PresentacionProductoFinalEvidenciaEntity, Long> {
    List<PresentacionProductoFinalEvidenciaEntity> findByIdPresentacionOrderByIdAsc(Long idPresentacion);
}
