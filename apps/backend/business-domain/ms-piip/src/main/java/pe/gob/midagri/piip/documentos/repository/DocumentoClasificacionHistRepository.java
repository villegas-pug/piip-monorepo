package pe.gob.midagri.piip.documentos.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.gob.midagri.piip.documentos.entity.DocumentoClasificacionHistEntity;

/**
 * Acceso JPA al historial append-only de reclasificación
 * documental. Solo expone consultas ordenadas que conservan la
 * trazabilidad inmutable exigida por la constitución.
 */
@Repository
public interface DocumentoClasificacionHistRepository
        extends JpaRepository<DocumentoClasificacionHistEntity, Long> {

    List<DocumentoClasificacionHistEntity> findByDocumentoIdOrderByFechaCambioAsc(Long documentoId);
}
