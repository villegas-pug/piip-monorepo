package pe.gob.midagri.piip.portafolio.service;

import pe.gob.midagri.piip.portafolio.dto.CreateDerivedProjectRequest;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.ProjectDetail;

/**
 * Servicio de aplicacion para crear un proyecto derivado a partir de una
 * iniciativa aprobada (US3, Constitucion 5.0.0).
 *
 * <p>Reglas canonicas implementadas por este contrato:
 * <ul>
 *   <li>Solo se permite crear un derivado cuando la iniciativa esta en
 *       {@code INICIATIVA_APROBADA}; el proyecto nace en
 *       {@code PROYECTO_EJECUCION} sin modificar el estado de la
 *       iniciativa.</li>
 *   <li>Un segundo intento de derivado para la misma iniciativa falla
 *       con 409 {@code DERIVATION_ALREADY_EXISTS}; la UK por
 *       {@code ID_INICIATIVA} del incremento 010 es la autoridad.</li>
 *   <li>Bloqueo pesimista de la iniciativa (PESSIMISTIC_WRITE) para
 *       serializar la creacion de derivados concurrentes.</li>
 *   <li>Documento formal de aprobacion o autorizacion de inicio
 *       obligatorio (campo 15).</li>
 *   <li>Idempotencia canonica por consumidor, operacion, clave y hash
 *       del payload.</li>
 *   <li>Auditoria atomica de exito y denegacion.</li>
 *   <li>Autorizacion efectiva: solo el {@code Responsable} dentro de su
 *       ambito puede crear el derivado.</li>
 * </ul>
 *
 * <p>El contrato nunca expone entidades JPA: las respuestas son
 * {@link ProjectDetail} y los parametros, DTOs HTTP.
 */
public interface CrearProyectoDerivadoService {

    /**
     * Crea un proyecto derivado a partir de una iniciativa aprobada.
     *
     * @param iniciativaId   identificador de la iniciativa origen.
     * @param comando        datos de entrada del derivado validados en
     *                       el DTO.
     * @param contexto       autorizacion efectiva calculada en la capa
     *                       HTTP.
     * @param idempotencyKey clave de idempotencia aportada por el cliente;
     *                       misma clave y mismo payload devuelven el
     *                       resultado original; clave con payload distinto
     *                       produce 409.
     * @param payloadJson    serializacion canonica del cuerpo de la
     *                       solicitud, requerida por
     *                       {@code IdempotencyService} para calcular el
     *                       hash estable.
     * @return detalle HTTP del proyecto derivado con codigo generado,
     *         estado, vinculo inmutable a la iniciativa y ETag.
     */
    ProjectDetail crear(Long iniciativaId, CreateDerivedProjectRequest comando,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson);
}
