package pe.gob.midagri.piip.documentos.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Solicitud HTTP para confirmar la publicación. Mantiene solo el
 * identificador del documento porque el título público y la
 * trazabilidad del Evaluador se conservan en {@link PublicarDocumentoRequest};
 * este record existe para que el servicio idempotente reciba un
 * payload canónico único.
 */
public record ConfirmarPublicacionCommand(
        @NotNull Long documentoId) {
}
