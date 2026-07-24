package pe.gob.midagri.piip.portafolio.service;

import pe.gob.midagri.piip.portafolio.dto.DirectProjectRequest;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.ProjectDetail;

/**
 * Servicio de aplicacion para crear un proyecto directo heredado o excepcional
 * (US3, Constitucion 5.0.0).
 *
 * <p>Reglas canonicas implementadas por este contrato:
 * <ul>
 *   <li>El proyecto directo exige, como minimo, los campos oficiales 1 al 13 y 22;
 *       el campo 23 ({@code nota}) es opcional. La fecha de inicio coincide con la
 *       del documento formal de aprobacion o autorizacion de inicio.</li>
 *   <li>Solo aplica a proyectos heredados o a excepciones formalmente autorizadas
 *       por la Autoridad; NO omite la evaluacion de una iniciativa nueva. Si
 *       existe una iniciativa {@code PRESENTADO} para el mismo ambito y anio, la
 *       operacion se rechaza con 409.</li>
 *   <li>Un segundo directo para la misma unidad y anio, cuando ya hay uno
 *       activo, falla con 409 canonico. La deteccion se realiza bajo bloqueo
 *       pesimista y el {@code COUNT} por unidad/anio/estado actua como
 *       salvaguarda.</li>
 *   <li>Solo la {@code Autoridad} o el {@code Evaluador} pueden crear un
 *       proyecto directo; el {@code Responsable} queda excluido por
 *       constitucion. La autorizacion efectiva se delega a
 *       {@code AutorizacionEfectivaService} cuando el bean esta disponible.</li>
 *   <li>Idempotencia canonica por consumidor, operacion, clave y hash del
 *       payload, gestionada por {@code IdempotencyService}.</li>
 *   <li>Auditoria atomica de exito en la misma transaccion de negocio y de
 *       denegacion en una transaccion independiente ({@code REQUIRES_NEW}).</li>
 *   <li>El estado nace en {@code PROYECTO_EJECUCION} y el proyecto no se
 *       vincula con iniciativa alguna ({@code iniciativaId} nulo en la respuesta).</li>
 * </ul>
 *
 * <p>El contrato nunca expone entidades JPA: las respuestas son
 * {@link ProjectDetail} y los parametros, DTOs HTTP.
 */
public interface CrearProyectoDirectoService {

    /**
     * Crea un proyecto directo heredado o excepcional.
     *
     * @param comando        datos de entrada del directo validados en el DTO.
     * @param contexto       autorizacion efectiva calculada en la capa HTTP.
     * @param idempotencyKey clave de idempotencia aportada por el cliente; misma
     *                       clave y mismo payload devuelven el resultado original;
     *                       clave con payload distinto produce 409.
     * @param payloadJson    serializacion canonica del cuerpo de la solicitud,
     *                       requerida por {@code IdempotencyService} para calcular
     *                       el hash estable.
     * @return detalle HTTP del proyecto directo con codigo generado, estado
     *         {@code PROYECTO_EJECUCION}, codigoOrigen, fecha de inicio, ETag y
     *         sin vinculo con iniciativa.
     */
    ProjectDetail crear(DirectProjectRequest comando, PortafolioAuthContext contexto,
            String idempotencyKey, String payloadJson);
}
