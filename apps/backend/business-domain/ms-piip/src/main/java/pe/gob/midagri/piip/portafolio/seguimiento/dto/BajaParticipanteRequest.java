package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Solicitud HTTP para dar de baja logica un participante de un
 * proyecto (US4, BR-042). La implementacion completa del servicio
 * se entrega en T073; T072 declara el DTO para estabilizar el
 * contrato HTTP del {@code SeguimientoController}. La baja nunca
 * elimina la fila: cierra la vigencia.
 */
public record BajaParticipanteRequest(
        @NotNull LocalDate fechaBaja,
        @Size(max = 2000) String motivo) {
}
