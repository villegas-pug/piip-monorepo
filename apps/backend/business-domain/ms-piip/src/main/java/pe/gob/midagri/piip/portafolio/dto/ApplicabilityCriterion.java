package pe.gob.midagri.piip.portafolio.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Criterio estructurado de aplicabilidad. Cada criterio se persiste en
 * APLICABILIDAD_CRITERIO y conserva la clave canonica (COMPETENCIA,
 * VALOR_PUBLICO, CARACTER_INNOVADOR, EXCLUSION, etc.) y su valoracion.
 */
public record ApplicabilityCriterion(
        @NotBlank @Size(max = 50) String clave,
        @NotBlank @Size(max = 500) String valor,
        @Min(1) @Max(999) int orden
) {}
