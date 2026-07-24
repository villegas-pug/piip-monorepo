package pe.gob.midagri.piip.portafolio.cierre.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Comando para decidir el producto final de un proyecto en ejecución. */
public record DecisionProductoFinalRequest(
        @NotNull DecisionProductoFinal decision,
        Long documentoId,
        Long evidenciaId,
        @Size(max = 40) String tipoProductoFinal,
        @Size(max = 2000) String observacion) {
}
