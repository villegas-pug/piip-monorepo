package pe.gob.midagri.piip.seguridad.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record MatrixCombinationRequest(
        @NotBlank String funcionCodigo,
        @NotBlank String perfil,
        @NotNull Long unidadId,
        @NotNull LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        @NotNull Long documentoAprobacionVersionId,
        @NotNull Long aprobadorUsuarioId) {
}
