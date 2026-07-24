package pe.gob.midagri.piip.seguridad.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de aprovisionamiento ordinario. La unidad objetivo se persiste
 * obligatoriamente para revalidar el alcance exacto de {@code UnidadAdmin}
 * en consulta y reintento; nunca se acepta contraseña, perfil ni rol.
 */
public record CreateUserRequest(
        @NotBlank @Email @Size(max = 200) String correoInstitucional,
        @NotBlank @Size(max = 300) String nombreCompleto,
        @NotNull Long unidadId) {
}
