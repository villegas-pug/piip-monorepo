package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para cancelar un proyecto
 * (US4, reglas BR-012 y BR-066). La implementacion completa del
 * servicio se entrega en T074; T072 declara el DTO para
 * estabilizar el contrato HTTP del
 * {@code SeguimientoController}. La cancelacion exige documento
 * "Informe de la Oficina de Modernizacion, Cancelacion" y
 * observacion obligatoria. La Autoridad decide o el Evaluador
 * registra con decision formal.
 */
public record CancelacionRequest(
        @NotNull Long idDocumento,
        @NotBlank @Size(max = 2000) String observacion,
        @Size(max = 200) String ifMatch) {
}
