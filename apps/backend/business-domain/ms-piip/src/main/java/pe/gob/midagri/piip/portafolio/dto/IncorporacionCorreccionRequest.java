package pe.gob.midagri.piip.portafolio.dto;

import jakarta.validation.constraints.*;

/**
 * Comando para registrar una corrección a una incorporación pendiente.
 */
public record IncorporacionCorreccionRequest(
        @NotNull Long incorporacionId,
        @NotBlank String datosNuevos,
        @NotBlank String motivo
) {}
