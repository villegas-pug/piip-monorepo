package pe.gob.midagri.piip.portafolio.seguimiento.service;

import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AdjuntarEvidenciaCicloRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AnexarCicloVersionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloVersionResponse;

/**
 * Servicio de ciclo del proyecto (US4, Constitucion 5.0.0,
 * DDL {@code 015_ciclos_resultados_cierre.sql} y contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}).
 *
 * <p>El contrato cubre las cuatro operaciones canonicas del
 * ciclo:
 * <ul>
 *   <li>{@link #abrirCiclo(long, String, PortafolioAuthContext, String, String)}
 *       crea la primera version del ciclo para un periodo
 *       quincenal valido (formato {@code AAAA-Qn-Sn}) exigiendo
 *       que el proyecto este en {@code PROYECTO_EJECUCION}.</li>
 *   <li>{@link #cerrarCicloAlFinal(long, long, PortafolioAuthContext, String, String)}
 *       fija la fecha de cierre y bloquea ediciones directas
 *       sobre la fila cerrada; la correccion posterior crea una
 *       nueva version (append-only).</li>
 *   <li>{@link #anexarVersion(long, long, AnexarCicloVersionRequest, PortafolioAuthContext, String, String)}
 *       agrega una version al historial del ciclo, sin destruir
 *       las anteriores.</li>
 *   <li>{@link #adjuntarEvidenciaDocumento(long, long, long, String, PortafolioAuthContext, String, String)}
 *       vincula un documento del portafolio como evidencia
 *       opcional del ciclo, previa validacion de aptitud
 *       documental.</li>
 * </ul>
 *
 * <p>El contrato es una interfaz; el servicio nunca retorna
 * entidades JPA y exige autorizacion efectiva Oracle en cada
 * operacion.
 */
public interface CicloService {

    /**
     * Abre la primera version del ciclo para un periodo quincenal
     * del proyecto. Devuelve 409 {@code CYCLE_ALREADY_OPEN} o
     * {@code CYCLE_DUPLICATED} si el proyecto ya tiene un ciclo
     * abierto o una fila para ese periodo. Devuelve 409
     * {@code PROJECT_NOT_IN_EXECUTION} si el proyecto no esta
     * en ejecucion. Devuelve 422 {@code INVALID_PERIOD_FORMAT} si
     * el periodo no cumple la CHECK {@code CK_CP_PERIODO}.
     */
    CicloResponse abrirCiclo(long proyectoId, String periodo,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson);

    /**
     * Cierra el ciclo indicado al final del periodo, fija la
     * fecha de cierre y bloquea ediciones directas. Devuelve 409
     * {@code CYCLE_ALREADY_CLOSED} si el ciclo ya esta cerrado.
     */
    void cerrarCicloAlFinal(long proyectoId, long cicloId,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson);

    /**
     * Anexa una nueva version append-only al ciclo. Devuelve 404
     * {@code CYCLE_NOT_FOUND} si el ciclo no existe. La fila
     * original nunca se modifica.
     */
    CicloVersionResponse anexarVersion(long proyectoId, long cicloId,
            AnexarCicloVersionRequest request, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson);

    /**
     * Adjunta un documento del portafolio como evidencia del
     * ciclo. El tipo documental debe pertenecer al catalogo
     * canonico y el documento debe estar apto segun
     * {@link AptitudDocumentalService}. Devuelve 422
     * {@code EVIDENCE_TYPE_NOT_ALLOWED},
     * {@code EVIDENCE_NOT_ELIGIBLE} o
     * {@code CYCLE_DOCUMENT_TYPE_INVALID} segun el caso.
     */
    void adjuntarEvidenciaDocumento(long proyectoId, long cicloId,
            long documentoId, String tipoDocumental,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson);
}
