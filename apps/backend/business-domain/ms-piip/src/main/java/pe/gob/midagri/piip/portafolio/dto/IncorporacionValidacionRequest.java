package pe.gob.midagri.piip.portafolio.dto;

import jakarta.validation.constraints.*;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;

/**
 * Comando para que el Evaluador valide o rechace una incorporación pendiente.
 */
public record IncorporacionValidacionRequest(
        @NotNull Long incorporacionId,
        @NotNull EstadoIniciativa estadoCanonico,
        Long registroVinculadoId,
        String observacion
) {}
