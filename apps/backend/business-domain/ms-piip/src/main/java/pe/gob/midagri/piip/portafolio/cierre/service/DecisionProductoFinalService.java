package pe.gob.midagri.piip.portafolio.cierre.service;

import pe.gob.midagri.piip.portafolio.cierre.dto.DecisionProductoFinalRequest;
import pe.gob.midagri.piip.portafolio.cierre.dto.DecisionProductoFinalResponse;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;

/** Caso de uso de decisión formal del producto final. */
public interface DecisionProductoFinalService {

    DecisionProductoFinalResponse decidir(long proyectoId,
            DecisionProductoFinalRequest request,
            String ifMatch,
            PortafolioAuthContext contexto,
            String idempotencyKey,
            String payloadJson);
}
