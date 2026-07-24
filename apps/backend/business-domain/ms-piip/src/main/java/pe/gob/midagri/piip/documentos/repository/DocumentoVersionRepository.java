package pe.gob.midagri.piip.documentos.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.documentos.entity.DocumentoVersionEntity;

public interface DocumentoVersionRepository extends JpaRepository<DocumentoVersionEntity, Long> {
    List<DocumentoVersionEntity> findBySerieIdOrderByNumeroVersionDesc(Long serieId);

    List<DocumentoVersionEntity> findByRegistroPortafolioIdAndActivoAndInmutable(
            Long registroPortafolioId, String activo, String inmutable);
}
