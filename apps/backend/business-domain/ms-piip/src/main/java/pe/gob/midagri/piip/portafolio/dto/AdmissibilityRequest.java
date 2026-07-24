package pe.gob.midagri.piip.portafolio.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP de admision (admisibilidad) registrada por el Evaluador.
 * El documentoOpinionId es obligatorio porque la admision exige el
 * Informe de Opinion Tecnica de Evaluacion (campo 14 de la matriz 013).
 */
public record AdmissibilityRequest(
        @NotBlank String resultado,
        @NotBlank @Size(max = 2000) String observacion,
        Long documentoOpinionId
) {}
