package pe.gob.midagri.piip.documentos.dto;

import java.time.LocalDateTime;

import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

/**
 * Resultado de validar la clasificación de un documento. Expone
 * la clasificación anterior, la nueva, la fecha del servidor, el
 * ETag optimista y el Evaluador que la registró. La fecha es
 * siempre del servidor para preservar la trazabilidad exigida por
 * la constitución.
 */
public record ValidacionClasificacionResult(
        Long documentoId,
        ClasificacionDocumento clasificacionAnterior,
        ClasificacionDocumento clasificacionValidada,
        LocalDateTime fechaValidacion,
        Long evaluadorId,
        String etag) {
}
