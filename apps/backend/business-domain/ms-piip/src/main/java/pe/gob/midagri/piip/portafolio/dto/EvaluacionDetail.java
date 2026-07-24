package pe.gob.midagri.piip.portafolio.dto;

import java.time.LocalDateTime;

import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;

/**
 * Detalle HTTP de la evaluacion (admisibilidad, aplicabilidad u opinion
 * tecnica). Expone el estado vigente de la iniciativa, el tipo de
 * evaluacion, el id del documento de opinion asociado y la version
 * optimista para construir el ETag.
 */
public record EvaluacionDetail(
        Long iniciativaId,
        EstadoIniciativa estadoIniciativa,
        String tipoEvaluacion,
        Long documentoOpinionId,
        LocalDateTime fechaEvaluacion,
        Long version,
        String etag
) {}
