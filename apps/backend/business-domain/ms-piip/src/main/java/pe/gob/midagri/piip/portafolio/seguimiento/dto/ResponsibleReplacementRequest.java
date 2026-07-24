package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Sustitución inmediata y auditable del Responsable titular. */
public record ResponsibleReplacementRequest(
        @NotNull Long nuevoResponsableId,
        @NotBlank @Size(max = 2000) String motivo) {
}
