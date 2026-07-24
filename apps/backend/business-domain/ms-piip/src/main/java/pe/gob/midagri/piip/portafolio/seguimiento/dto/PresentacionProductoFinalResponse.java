package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import java.util.List;

/**
 * Detalle canonico de la presentacion del producto final de un
 * proyecto (US4, BR-018, BR-063, BR-064, BR-068, BR-069, BR-087).
 * La implementacion completa del servicio se entrega en T073;
 * T072 declara el DTO para estabilizar el contrato HTTP del
 * {@code SeguimientoController}. No expone entidades JPA.
 */
public record PresentacionProductoFinalResponse(
        long idPresentacion,
        long idProyecto,
        int version,
        long idVersionAnterior,
        String tipoProductoFinal,
        String documentacionGestion,
        String resultadosClave,
        String nota,
        long idDocumentoSustenta,
        List<Long> evidenciaIds,
        String etag) {
}
