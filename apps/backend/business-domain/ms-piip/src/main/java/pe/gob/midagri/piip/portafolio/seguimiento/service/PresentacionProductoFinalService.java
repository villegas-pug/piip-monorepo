package pe.gob.midagri.piip.portafolio.seguimiento.service;

import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PresentacionProductoFinalRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PresentacionProductoFinalResponse;

/**
 * Servicio de presentacion del producto final del proyecto
 * (US4, BR-018, BR-063, BR-064, BR-068, BR-069, BR-087).
 * Declarado en T072 como contrato estabilizador del
 * {@code SeguimientoController}; la implementacion completa se
 * entrega en T073.
 */
public interface PresentacionProductoFinalService {

    /**
     * Registra una presentacion del producto final con tipo
     * canonico, campos 17/19/23 y documento formal de sustento.
     * 422 si el tipo no pertenece al catalogo canonico, si el
     * documento no es apto o si faltan campos obligatorios.
     */
    PresentacionProductoFinalResponse presentar(long proyectoId,
            PresentacionProductoFinalRequest request, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson);
}
