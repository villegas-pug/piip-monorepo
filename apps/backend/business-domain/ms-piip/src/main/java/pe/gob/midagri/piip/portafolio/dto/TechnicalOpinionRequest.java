package pe.gob.midagri.piip.portafolio.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para corregir la opinion tecnica (campo 14). Cada
 * correccion crea una nueva version documental a traves de
 * {@code DocumentoService.crearVersion} y conserva la fila previa como
 * evidencia inmutable.
 */
public record TechnicalOpinionRequest(
        @NotNull Long documentoVersionId,
        @Size(max = 2000) String observaciones
) {}
