package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para dar de alta una persona como participante de
 * un proyecto (US4, reglas BR-035, BR-038, BR-039, BR-040, BR-041
 * y BR-042). La implementacion completa del servicio se entrega
 * en T073; T072 declara el DTO para estabilizar el contrato HTTP
 * del {@code SeguimientoController}.
 */
public record AltaPersonaRequest(
        Long personaId,
        @NotBlank @Pattern(regexp = "Responsable|Participante",
                message = "INVALID_ROLE") String rol,
        @Size(max = 300) String nombresCompletos,
        @Size(max = 200) String institucion,
        @Size(max = 200) String funcion) {
}
