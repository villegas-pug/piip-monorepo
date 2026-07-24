package pe.gob.midagri.piip.seguridad.dto;

import jakarta.validation.constraints.NotBlank;

public record RevocationRequest(@NotBlank String motivo, Long documentoFormalVersionId) {}
