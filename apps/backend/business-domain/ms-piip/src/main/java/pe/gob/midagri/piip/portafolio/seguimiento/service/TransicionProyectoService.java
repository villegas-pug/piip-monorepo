package pe.gob.midagri.piip.portafolio.seguimiento.service;

import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CancelacionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.SuspensionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.TransicionResponse;

/**
 * Servicio de transiciones de suspension y cancelacion del
 * proyecto (US4, BR-012, BR-066). Declarado en T072 como contrato
 * estabilizador del {@code SeguimientoController}; la
 * implementacion completa se entrega en T074.
 */
public interface TransicionProyectoService {

    /**
     * Suspende un proyecto en {@code PROYECTO_EJECUCION}. Solo
     * {@code UnidadAdmin} con documento "Evidencia de Suspension"
     * y observacion obligatoria. 428 si falta If-Match, 412 si el
     * ETag no coincide, 403 si el perfil no es UnidadAdmin, 409
     * si la transicion no es canonica, 422 si falta evidencia u
     * observacion.
     */
    TransicionResponse suspender(long proyectoId, SuspensionRequest request,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson);

    /**
     * Cancela un proyecto en {@code PROYECTO_EJECUCION}. La
     * Autoridad decide o el Evaluador registra con decision
     * formal. Exige documento "Informe de la Oficina de
     * Modernizacion, Cancelacion" y observacion obligatoria. La
     * fecha de cierre se genera automaticamente.
     */
    TransicionResponse cancelar(long proyectoId, CancelacionRequest request,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson);
}
