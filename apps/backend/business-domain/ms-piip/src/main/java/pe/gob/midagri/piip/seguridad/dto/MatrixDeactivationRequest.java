package pe.gob.midagri.piip.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MatrixDeactivationRequest(
        @NotBlank @Size(max = 30) String codigoNuevaVersion,
        @NotNull Long documentoAprobacionVersionId,
        @NotNull Long aprobadorUsuarioId,
        @NotBlank @Size(max = 2000) String motivo) {
}
