package pe.gob.midagri.piip.portafolio.cierre.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Datos que completa el Evaluador para el cierre administrativo. */
public record CierreProyectoRequest(
        @NotBlank String informeFinal,
        @NotNull Long informeFinalDocumentoId,
        @NotBlank String aprendizajes,
        @NotBlank @Size(max = 2000) String conclusion,
        @NotBlank @Size(max = 2000) String observacion) {
}
