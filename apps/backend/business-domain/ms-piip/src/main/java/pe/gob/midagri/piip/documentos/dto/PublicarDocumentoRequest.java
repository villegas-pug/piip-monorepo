package pe.gob.midagri.piip.documentos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para confirmar la publicación de una versión
 * documental con clasificación {@code PUBLICO} validada. El
 * título público debe estar libre de datos personales detectables
 * y respetar el CHECK de formato del DDL 004.
 */
public record PublicarDocumentoRequest(
        @NotNull Long documentoId,
        @NotBlank @Size(min = 5, max = 500) String tituloPublico,
        @NotBlank @Size(max = 100) String autoridadPublica,
        @Pattern(regexp = "^[A-Za-z0-9 .,;:()\\-]{5,500}$") String resumenPublico) {
}
