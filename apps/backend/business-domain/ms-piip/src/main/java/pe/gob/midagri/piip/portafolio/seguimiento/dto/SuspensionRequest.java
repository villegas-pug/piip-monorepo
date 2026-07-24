package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para suspender un proyecto
 * (US4, regla BR-012). La implementacion completa del servicio
 * se entrega en T074; T072 declara el DTO para estabilizar el
 * contrato HTTP del {@code SeguimientoController}. Solo
 * {@code UnidadAdmin} puede suspender; exige documento formal
 * "Evidencia de Suspension" y observacion obligatoria.
 */
public record SuspensionRequest(
        @NotNull Long idDocumento,
        @NotBlank @Size(max = 2000) String observacion,
        @Size(max = 200) String ifMatch) {
}
