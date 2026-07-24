package pe.gob.midagri.piip.documentos.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.gob.midagri.piip.documentos.entity.DocumentoPublicacionEntity;

@Repository
public interface DocumentoPublicacionRepository
        extends JpaRepository<DocumentoPublicacionEntity, Long> {

    Optional<DocumentoPublicacionEntity> findByDocumentoId(Long documentoId);

    List<DocumentoPublicacionEntity> findByDocumentoIdIn(List<Long> documentoIds);
}
