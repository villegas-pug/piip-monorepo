package pe.gob.midagri.piip.documentos.dto;

import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

/**
 * Comando interno para aplicar la reclasificación. El Evaluador
 * registrador se obtiene del contexto autorizado en el servidor; la
 * clasificación anterior se lee de la fila {@code DOCUMENTO} y nunca
 * del cliente para garantizar la regla restrictiva.
 */
public record ReclasificarDocumentoCommand(
        ClasificacionDocumento clasificacionNueva,
        Long documentoDecisionId,
        Long autoridadDecisoraId,
        String motivo) {
}
