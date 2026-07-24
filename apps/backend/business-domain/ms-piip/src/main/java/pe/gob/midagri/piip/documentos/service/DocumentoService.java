package pe.gob.midagri.piip.documentos.service;

import pe.gob.midagri.piip.documentos.dto.AptitudDocumental;
import pe.gob.midagri.piip.documentos.dto.ContenidoDocumentoResponse;
import pe.gob.midagri.piip.documentos.dto.DocumentoEvidenciaApta;
import pe.gob.midagri.piip.documentos.dto.DocumentVersionDetail;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.DocumentoInstitucionalAprobatorio;
import pe.gob.midagri.piip.documentos.dto.UploadDocumentCommand;

public interface DocumentoService {
    AptitudDocumental obtenerAptitud(Integer tipoDocumentoId);
    DocumentoEvidenciaApta validarEvidencia(long documentoId, String tipoDocumental);
    DocumentoInstitucionalAprobatorio validarDocumentoInstitucionalAprobatorio(long documentoId);

    /**
     * Obtiene la unidad activa asociada a un expediente institucional.
     */
    Long obtenerUnidadExpediente(Long expedienteId);

    /**
     * Obtiene el expediente institucional asociado a una serie documental.
     */
    Long obtenerExpedienteDeSerie(Long serieId);

    /**
     * Recupera los metadatos y el BLOB de un documento para la
     * consulta institucional autorizada. La operación revalida el
     * ámbito y la clasificación validada del documento antes de
     * devolver el contenido; nunca se entrega contenido a la
     * consulta pública ni a usuarios sin asignación efectiva.
     */
    ContenidoInstitucional obtenerContenidoInstitucional(
            DocumentoAuthorizedContext contexto, Long documentoId);

    DocumentVersionDetail cargarEnExpediente(DocumentoAuthorizedContext contexto, String idempotencyKey,
            UploadDocumentCommand comando);
    DocumentVersionDetail crearVersion(DocumentoAuthorizedContext contexto, Long serieId, String idempotencyKey,
            String etagUltimaVersion, UploadDocumentCommand comando);

    /** DTO interno con el BLOB y la respuesta HTTP canónica. */
    record ContenidoInstitucional(ContenidoDocumentoResponse metadatos, byte[] binario) {
    }
}
