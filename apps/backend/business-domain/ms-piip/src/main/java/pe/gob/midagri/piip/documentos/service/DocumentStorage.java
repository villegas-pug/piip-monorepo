package pe.gob.midagri.piip.documentos.service;

/** Puerto interno para BLOB Oracle; no expone storage keys ni URLs. */
public interface DocumentStorage {
    byte[] leerContenido(Long documentoId);
    void almacenarContenido(Long documentoId, byte[] contenido);
}
