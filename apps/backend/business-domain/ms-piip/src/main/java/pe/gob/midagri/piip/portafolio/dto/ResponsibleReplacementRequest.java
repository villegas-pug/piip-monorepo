package pe.gob.midagri.piip.portafolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Comando para sustituir inmediatamente una titularidad vigente. */
public record ResponsibleReplacementRequest(
        @NotNull Long nuevoResponsableId,
        @NotBlank @Size(max = 2000) String motivo) {
}
