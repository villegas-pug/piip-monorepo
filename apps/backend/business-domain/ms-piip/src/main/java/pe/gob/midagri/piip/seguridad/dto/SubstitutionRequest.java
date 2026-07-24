package pe.gob.midagri.piip.seguridad.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

/** Comando para crear una suplencia temporal sobre una asignación titular. */
public record SubstitutionRequest(
        @NotNull Long suplenteUsuarioId,
        @NotNull LocalDate inicio,
        @NotNull LocalDate fin,
        @NotNull Long documentoFormalVersionId) {
}
