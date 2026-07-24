package pe.gob.midagri.piip.portafolio.dto;

import pe.gob.midagri.piip.portafolio.entity.EstadoIncorporacion;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Salida con el detalle de una incorporacion individual.
 *
 * <p>Expone unicamente DTO y tipos simples: las entidades JPA permanecen dentro del modulo.
 * Incluye la observacion del Evaluador al validar o rechazar y la version optimista para
 * construir el ETag de la respuesta.
 */
public record IncorporacionDetail(
        Long id,
        String fuente,
        LocalDate fechaFuente,
        Long responsableId,
        Long documentoFuenteId,
        String hashOriginal,
        EstadoIncorporacion estado,
        Long registroVinculadoId,
        String observacion,
        String creadoPor,
        LocalDateTime fechaCreacion,
        Long version,
        String etag
) {}
