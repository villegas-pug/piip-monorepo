package pe.gob.midagri.piip.documentos.service.impl;

import org.springframework.stereotype.Component;
import pe.gob.midagri.piip.documentos.entity.DocumentoVersionEntity;
import pe.gob.midagri.piip.documentos.repository.DocumentoVersionRepository;
import pe.gob.midagri.piip.documentos.service.DocumentStorage;

@Component
public class OracleBlobDocumentStorage implements DocumentStorage {
    private final DocumentoVersionRepository documentoVersionRepository;
    public OracleBlobDocumentStorage(DocumentoVersionRepository documentoVersionRepository) {
        this.documentoVersionRepository = documentoVersionRepository;
    }
    @Override public byte[] leerContenido(Long documentoId) {
        return documentoVersionRepository.findById(documentoId)
                .map(DocumentoVersionEntity::getContenido).orElseThrow();
    }
    @Override public void almacenarContenido(Long documentoId, byte[] contenido) {
        DocumentoVersionEntity documento = documentoVersionRepository.findById(documentoId).orElseThrow();
        documento.setContenido(contenido);
    }
}
