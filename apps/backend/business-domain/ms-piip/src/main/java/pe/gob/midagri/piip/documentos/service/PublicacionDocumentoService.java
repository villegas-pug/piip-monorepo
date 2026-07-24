package pe.gob.midagri.piip.documentos.service;

import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.PublicacionDocumentoDetail;
import pe.gob.midagri.piip.documentos.dto.PublicarDocumentoRequest;

/**
 * Autoridad única para confirmar publicaciones documentales. Solo
 * el Evaluador de la Oficina de Modernización puede confirmar la
 * publicación de una versión con clasificación {@code PUBLICO}
 * validada. La fecha de publicación la fija exclusivamente el
 * servidor mediante el DEFAULT SYSTIMESTAMP del DDL 004; el cliente
 * nunca contribuye con la fecha.
 *
 * <p>La operación es idempotente: una segunda invocación con la
 * misma {@code Idempotency-Key} y el mismo payload devuelve el
 * resultado original; con la misma clave y un payload distinto
 * produce 409.
 */
public interface PublicacionDocumentoService {

    PublicacionDocumentoDetail confirmarPublicacion(
            DocumentoAuthorizedContext contexto, String idempotencyKey, PublicarDocumentoRequest solicitud);
}
