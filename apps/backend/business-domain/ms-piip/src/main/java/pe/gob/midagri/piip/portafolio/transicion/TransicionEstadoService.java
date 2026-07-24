package pe.gob.midagri.piip.portafolio.transicion;

import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;

/**
 * Contrato de la maquina de estados canonica del portafolio (US2) conforme a
 * la Constitucion 5.0.0 (tabla "Transiciones controladas iniciales") y al
 * contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}.
 *
 * <p>La interfaz nunca expone entidades JPA. La concurrencia se resuelve
 * mediante un bloqueo pesimista sobre la fila de
 * {@code RegistroPortafolioEntity} y el versionado optimista via
 * {@code @Version}. La cabecera {@code If-Match} es obligatoria en cada
 * transicion: sin ella se responde 428 PRECONDITION_REQUIRED; si el ETag
 * no coincide con la version actual de la fila se responde 412
 * STATE_CHANGED.
 *
 * <p>Estados terminales de iniciativa: {@code NO_ADMISIBLE},
 * {@code NO_APLICABLE} e {@code INICIATIVA_ARCHIVADA}. Cualquier intento de
 * transicion posterior desde un terminal se rechaza con 409
 * STATE_TRANSITION_NOT_ALLOWED.
 *
 * <p>La evidencia documental (campo 14) es exigida por la Constitucion para
 * todas las transiciones listadas. La omision se traduce en 422
 * EVIDENCE_NOT_ELIGIBLE o 422 FORMAL_DECISION_REQUIRED segun el caso.
 */
public interface TransicionEstadoService {

    /**
     * Ejecuta una transicion canonica: PRESENTADO hacia los cuatro destinos
     * posibles, PROYECTO_EJECUCION hacia los cuatro destinos posibles y los
     * dos cierres desde PRODUCTO_*.
     *
     * @param registroId    identificador del registro de portafolio.
     * @param comando       destino canonico, observacion opcional,
     *                      identificador del documento de evidencia y ETag.
     * @param contexto      autorizacion efectiva calculada en la capa HTTP.
     * @param idempotencyKey clave de idempotencia opcional aportada por el
     *                       cliente.
     * @param payloadJson   representacion serializada del comando para
     *                       calcular el hash canonico de idempotencia.
     * @return detalle HTTP con estado anterior, nuevo, transicion, fecha del
     *         servidor, actor y ETag actualizado.
     */
    TransicionDetail transicionar(Long registroId, TransicionCommand comando,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson);

    /**
     * Ejecuta la transicion automatica de subsanacion vencida: la iniciativa
     * cierra su ciclo pasando a {@code NO_ADMISIBLE} sin intervencion del
     * Responsable, registrando la observacion "Plazo de subsanacion vencido
     * sin atender" y el documento de evidencia que la maquina documental
     * produce al cierre del plazo.
     *
     * <p>Esta firma existe para desacoplar la senal de vencimiento
     * (provista por {@code SubsanacionIniciativaService}) del comando HTTP
     * del cliente. Solo el Evaluador puede ejecutar el cierre formal.
     */
    TransicionDetail transicionarPorVencimientoSubsanacion(Long registroId,
            String observacionVencimiento, Long documentoRefId,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson);
}
