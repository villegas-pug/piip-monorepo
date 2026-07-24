package pe.gob.midagri.piip.seguridad.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Comando inmutablemente auditado para el cese anticipado de una suplencia. */
public record EarlyTerminationRequest(
        @NotBlank @Size(max = 2000) String motivo,
        Long documentoFormalVersionId) {
}
