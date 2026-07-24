package pe.gob.midagri.piip.portafolio.dto;

import jakarta.validation.constraints.*;

/**
 * Comando para que el Evaluador resuelva un conflicto de incorporación.
 */
public record IncorporacionResolucionConflictoRequest(
        @NotNull Long conflictoId,
        @NotNull Long incorporacionId,
        @NotBlank String resolucion,
        Long documentoResolucionId
) {}
