package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Solicitud HTTP para dar de alta una unidad organizacional como
 * participante de un proyecto (US4, BR-035, BR-038). La
 * implementacion completa del servicio se entrega en T073; T072
 * declara el DTO para estabilizar el contrato HTTP del
 * {@code SeguimientoController}.
 */
public record AltaUnidadRequest(
        @NotNull Long unidadId) {
}
