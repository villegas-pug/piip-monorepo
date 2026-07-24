package pe.gob.midagri.piip.documentos.dto;

import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

/**
 * Resultado de aplicar la reclasificación. Expone la clasificación
 * anterior, la nueva clasificación efectiva, el ETag optimista de
 * la versión y el detalle inmutable del evento registrado. La
 * consulta posterior de la versión revalida inmediatamente la
 * nueva clasificación.
 */
public record ReclasificacionDocumentoResult(
        Long documentoId,
        ClasificacionDocumento clasificacionAnterior,
        ClasificacionDocumento clasificacionNueva,
        String etag,
        ClasificacionHistDetalle historial) {
}
