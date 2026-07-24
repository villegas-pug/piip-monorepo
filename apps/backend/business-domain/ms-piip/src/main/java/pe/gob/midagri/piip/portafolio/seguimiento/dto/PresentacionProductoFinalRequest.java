package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para presentar el producto final de un proyecto
 * (US4, reglas BR-018, BR-063, BR-064, BR-068, BR-069, BR-087).
 * La implementacion completa del servicio se entrega en T073;
 * T072 declara el DTO para estabilizar el contrato HTTP del
 * {@code SeguimientoController}. El tipo de producto final debe
 * pertenecer al catalogo canonico
 * ({@code PROTOTIPO_CONCEPTUALIZADO} o
 * {@code SOLUCION_FUNCIONAL}).
 */
public record PresentacionProductoFinalRequest(
        @NotBlank @Pattern(
                regexp = "PROTOTIPO_CONCEPTUALIZADO|SOLUCION_FUNCIONAL",
                message = "PRODUCT_FINAL_TYPE_REQUIRED") String tipoProductoFinal,
        @Size(max = 2000) String documentacionGestion,
        @Size(max = 2000) String resultadosClave,
        @Size(max = 1000) String nota,
         @NotNull Long idDocumentoSustenta,
         @NotNull List<@NotNull Long> evidenciaIds) {
}
