package pe.gob.midagri.piip.documentos.dto;

import java.time.LocalDateTime;

import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

/**
 * Detalle append-only de una publicación confirmada. Conserva el
 * identificador de publicación, la fecha del servidor que la fija
 * y los metadatos públicos validados. Se entrega en respuestas
 * HTTP y en proyecciones institucionales; nunca expone el BLOB ni
 * claves físicas.
 */
public record PublicacionDocumentoDetail(
        Long publicacionId,
        Long documentoId,
        String tituloPublico,
        ClasificacionDocumento clasificacion,
        Long evaluadorConfirmadorId,
        Long asignacionEfectivaId,
        LocalDateTime fechaPublicacion) {
}
