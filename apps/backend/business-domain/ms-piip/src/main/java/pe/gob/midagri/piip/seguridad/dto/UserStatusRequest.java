package pe.gob.midagri.piip.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de cambio de estado de un usuario institucional
 * (desactivación o reactivación). El motivo es obligatorio para auditoría
 * y nunca contiene credenciales, tokens ni datos personales.
 */
public record UserStatusRequest(@NotBlank @Size(max = 1000) String motivo) {
}
