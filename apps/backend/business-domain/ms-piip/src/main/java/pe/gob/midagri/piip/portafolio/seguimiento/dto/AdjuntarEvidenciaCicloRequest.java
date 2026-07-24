package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Solicitud HTTP para adjuntar un documento opcional como evidencia
 * de un ciclo (US4, Constitucion 5.0.0 y DDL
 * {@code 015_ciclos_resultados_cierre.sql}).
 *
 * <p>Los tipos documentales admitidos como evidencia del ciclo son
 * los definidos en el catalogo canonico:
 * {@code AutoevaluacionCicloTrabajo},
 * {@code SeguimientoAgilTableroKanban} y
 * {@code MatrizPlanificacionCiclos}. Su ausencia no bloquea el
 * ciclo; si el documento se adjunta, debe estar apto segun
 * {@link pe.gob.midagri.piip.portafolio.seguimiento.service.AptitudDocumentalService}.
 */
public record AdjuntarEvidenciaCicloRequest(
        @NotNull Long idDocumento,
        @NotBlank @Size(max = 200) String tipoDocumental) {
}
