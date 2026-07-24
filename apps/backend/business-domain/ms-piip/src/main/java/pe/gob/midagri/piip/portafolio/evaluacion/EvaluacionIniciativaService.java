package pe.gob.midagri.piip.portafolio.evaluacion;

import pe.gob.midagri.piip.portafolio.dto.AdmissibilityRequest;
import pe.gob.midagri.piip.portafolio.dto.ApplicabilityRequest;
import pe.gob.midagri.piip.portafolio.dto.EvaluacionDetail;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.TechnicalOpinionRequest;

/**
 * Contrato del modulo de evaluacion de iniciativa (US2).
 *
 * <p>La evaluacion distingue dos operaciones independientes y NO
 * intercambiables:
 * <ul>
 *   <li>Admisibilidad: verifica requisitos formales y transita a
 *       {@code NO_ADMISIBLE} solo cuando la subsanacion ha vencido.</li>
 *   <li>Aplicabilidad: aplica la lista estructurada de competencia
 *       institucional, valor publico y caracter innovador; exige motivo
 *       obligatorio cuando el resultado es NO_APLICABLE.</li>
 * </ul>
 * El Evaluador decide y registra; el Responsable y la Autoridad no
 * pueden evaluar (separacion de roles). La correccion de la opinion
 * tecnica crea una nueva version documental (append-only).
 */
public interface EvaluacionIniciativaService {

    /**
     * Registra la decision de admisibilidad del Evaluador. Exige
     * documentoOpinionId no nulo (campo 14 obligatorio). Transita la
     * iniciativa a NO_ADMISIBLE cuando el resultado es desfavorable y la
     * subsanacion ya vencio o no fue abierta.
     */
    EvaluacionDetail registrarAdmisibilidad(Long iniciativaId, AdmissibilityRequest request,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson);

    /**
     * Registra la decision de aplicabilidad del Evaluador con la lista
     * estructurada de criterios. Exige motivo obligatorio cuando el
     * resultado es NO_APLICABLE.
     */
    EvaluacionDetail registrarAplicabilidad(Long iniciativaId, ApplicabilityRequest request,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson);

    /**
     * Corrige la opinion tecnica creando una nueva version documental.
     * La fila original de EVALUACION_INICIATIVA permanece inmutable
     * para conservar el historial de auditoria. La version del ETag
     * (If-Match) debe coincidir con la actual.
     */
    EvaluacionDetail corregirOpinionTecnica(Long iniciativaId, TechnicalOpinionRequest request,
            PortafolioAuthContext contexto, Long expectedVersion, String ifMatch,
            String idempotencyKey, String payloadJson);
}
