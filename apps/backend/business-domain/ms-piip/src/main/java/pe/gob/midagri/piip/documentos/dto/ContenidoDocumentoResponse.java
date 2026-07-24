package pe.gob.midagri.piip.documentos.dto;

import java.time.LocalDateTime;

import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

/**
 * Respuesta HTTP del endpoint de contenido institucional. Regresa
 * el BLOB en su forma binaria y expone metadatos para auditoría
 * del cliente. La cabecera de respuesta {@code Content-Type}
 * refleja el MIME original; el DTO nunca se serializa a JSON con
 * el contenido embebido. La respuesta se entrega con cabeceras
 * canónicas de correlación y caché.
 */
public record ContenidoDocumentoResponse(
        Long documentoId,
        Long serieId,
        Integer numeroVersion,
        String nombreOriginal,
        String mimeType,
        String formato,
        long tamanoBytes,
        String hashSha256,
        ClasificacionDocumento clasificacionValidada,
        LocalDateTime fechaCarga,
        String etag) {
}
