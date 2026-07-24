package pe.gob.midagri.piip.portafolio.cierre.service;

import pe.gob.midagri.piip.portafolio.cierre.dto.CierreProyectoRequest;
import pe.gob.midagri.piip.portafolio.cierre.dto.CierreProyectoResponse;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;

/** Caso de uso de cierre administrativo del proyecto. */
public interface CierreProyectoService {
    CierreProyectoResponse cerrar(long proyectoId, CierreProyectoRequest request, String ifMatch,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson);
}
