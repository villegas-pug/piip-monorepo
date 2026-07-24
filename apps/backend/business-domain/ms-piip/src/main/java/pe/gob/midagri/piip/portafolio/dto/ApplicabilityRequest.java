package pe.gob.midagri.piip.portafolio.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP de aplicabilidad registrada por el Evaluador.
 *
 * <p>El motivo es obligatorio cuando el resultado es NO_APLICABLE (regla
 * CK_AI_MOTIVO del incremento 014/014.1). Los criterios estructurados
 * representan la lista canonica de competencia, valor publico y caracter
 * innovador.
 */
public record ApplicabilityRequest(
        @NotBlank String resultado,
        @Size(max = 2000) String motivo,
        List<ApplicabilityCriterion> criterios
) {}
