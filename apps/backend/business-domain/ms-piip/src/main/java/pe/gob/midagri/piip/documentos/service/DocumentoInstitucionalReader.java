package pe.gob.midagri.piip.documentos.service;

import java.util.List;

import pe.gob.midagri.piip.documentos.dto.DocumentoInstitucionalMetadata;

/**
 * Puerto del módulo {@code documentos} que entrega los metadatos
 * documentales visibles a la consulta institucional autorizada.
 * No expone el BLOB, la clave física ni la URL directa; el
 * contenido se obtiene únicamente por el endpoint
 * {@code /api/v1/documentos/{id}/contenido} que revalida el
 * ámbito y la clasificación validada.
 *
 * <p>Los resultados respetan la clasificación validada: solo se
 * exponen al actor autorizado dentro de su ámbito. Las
 * publicaciones confirmadas se incluyen cuando el documento tiene
 * clasificación {@code PUBLICO} validada.
 */
public interface DocumentoInstitucionalReader {

    /**
     * Recupera los metadatos documentales de las series asociadas
     * a un registro de portafolio dentro del ámbito del actor. La
     * colección no incluye los expedientes institucionales.
     *
     * @param registroPortafolioId identificador del registro del portafolio
     * @return metadatos documentales filtrados por clasificación
     */
    List<DocumentoInstitucionalMetadata> listarPorRegistro(Long registroPortafolioId);
}
