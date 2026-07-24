package pe.gob.midagri.piip.documentos.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.documentos.entity.DocumentoSerieEntity;

public interface DocumentoSerieRepository extends JpaRepository<DocumentoSerieEntity, Long> {
    List<DocumentoSerieEntity> findByExpedienteInstitucionalIdOrderByFechaCreacionDesc(Long expedienteId);
}
