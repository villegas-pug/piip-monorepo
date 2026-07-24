package pe.gob.midagri.piip.portafolio.service;

import pe.gob.midagri.piip.portafolio.dto.CreateInitiativeRequest;
import pe.gob.midagri.piip.portafolio.dto.InitiativeDetail;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;

/**
 * Servicio de aplicación para presentar una iniciativa nueva.
 * Valida campos oficiales 1, 5-13 y 22, genera código, fecha de presentación y estado PRESENTADO.
 */
public interface PresentarIniciativaService {

    /**
     * Presenta una iniciativa completa y retorna el detalle con código generado.
     *
     * @param comando datos de entrada
     * @param contexto contexto de autorización efectiva
     * @param idempotencyKey clave de idempotencia aportada por el cliente; misma clave y mismo
     *                       payload devuelven el resultado original; clave con payload distinto
     *                       produce 409
     * @param payloadJson serialización canónica del cuerpo de la solicitud, requerida por
     *                    {@code IdempotencyService} para calcular el hash estable
     * @return detalle de la iniciativa con código, fecha y estado
     */
    InitiativeDetail presentar(CreateInitiativeRequest comando, PortafolioAuthContext contexto,
            String idempotencyKey, String payloadJson);
}
