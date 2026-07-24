package pe.gob.midagri.piip.seguridad.dto;

import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MatrixVersionRequest(
        @NotBlank @Size(max = 30) String codigoVersion,
        Long versionAnteriorId,
        @NotNull LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        @NotNull Long documentoAprobacionVersionId,
        @NotEmpty List<@Valid MatrixFunctionRequest> funciones,
        @NotEmpty List<@Valid MatrixCombinationRequest> combinaciones) {
}
