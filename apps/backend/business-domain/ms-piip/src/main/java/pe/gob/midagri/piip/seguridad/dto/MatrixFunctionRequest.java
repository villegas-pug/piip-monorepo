package pe.gob.midagri.piip.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MatrixFunctionRequest(
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(max = 500) String descripcion) {
}
