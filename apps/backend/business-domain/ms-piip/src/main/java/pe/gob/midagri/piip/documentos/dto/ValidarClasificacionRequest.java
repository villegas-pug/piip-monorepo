package pe.gob.midagri.piip.documentos.dto;

import jakarta.validation.constraints.NotNull;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

/**
 * Solicitud HTTP para validar la clasificación inicial propuesta
 * por el Responsable. El Evaluador confirma la clasificación y la
 * fecha del servidor fija la trazabilidad; el cliente nunca
 * contribuye con la fecha ni con la identidad del Evaluador.
 */
public record ValidarClasificacionRequest(
        @NotNull ClasificacionDocumento clasificacion) {
}
