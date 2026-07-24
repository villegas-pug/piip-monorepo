package pe.gob.midagri.piip.portafolio.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

/**
 * Comando para registrar una incorporación individual de información existente.
 */
public record CreateIncorporacionRequest(
        @NotBlank @Size(max = 200) String fuente,
        @NotNull LocalDate fechaFuente,
        @NotNull Long responsableId,
        @NotNull Long documentoFuenteId,
        @NotBlank @Size(max = 64) String hashOriginal,
        String datosOriginales,
        String codigoHeredado
) {}
