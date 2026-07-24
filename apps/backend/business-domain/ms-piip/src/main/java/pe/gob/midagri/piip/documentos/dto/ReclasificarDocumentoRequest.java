package pe.gob.midagri.piip.documentos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

/**
 * Solicitud HTTP para reclasificar un documento. La Autoridad
 * decisora siempre se resuelve en el servidor a partir del JWT y de
 * la asignación efectiva; el cliente solo aporta la nueva
 * clasificación, la referencia del documento formal de decisión y
 * el motivo. El campo {@code clasificacionAnterior} se ignora en la
 * autorización y se obtiene de la fila {@code DOCUMENTO} para
 * impedir que un cliente omita la transición restrictiva.
 */
public record ReclasificarDocumentoRequest(
        @NotNull ClasificacionDocumento clasificacionNueva,
        @NotNull Long documentoDecisionId,
        @NotNull Long autoridadDecisoraId,
        @Size(max = 2000) String motivo) {
}
