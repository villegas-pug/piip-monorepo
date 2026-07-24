package pe.gob.midagri.piip.portafolio.seguimiento.service;

import java.util.List;

import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AltaPersonaRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AltaUnidadRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.BajaParticipanteRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.ParticipanteResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.ResponsibleReplacementRequest;

/**
 * Servicio de participantes del proyecto (US4, BR-035, BR-038,
 * BR-039, BR-040, BR-041, BR-042). Declarado en T072 como
 * contrato estabilizador del {@code SeguimientoController}; la
 * implementacion completa se entrega en T073.
 */
public interface ParticipanteProyectoService {

    /**
     * Lista el historico completo de participaciones del proyecto
     * (vigentes y bajas). La baja nunca elimina la fila; cierra la
     * vigencia. 404 si el proyecto no existe.
     */
    List<ParticipanteResponse> listarHistorico(long proyectoId,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson);

    /**
     * Da de alta una persona como participante del proyecto
     * (Responsable o Participante). 409
     * {@code RESPONSIBLE_CARDINALITY} si ya existe un
     * Responsable titular.
     */
    ParticipanteResponse altaPersona(long proyectoId, AltaPersonaRequest request,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson);

    /**
     * Da de alta una unidad organizacional como participante del
     * proyecto.
     */
    ParticipanteResponse altaUnidad(long proyectoId, AltaUnidadRequest request,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson);

    /**
     * Da de baja logica un participante cerrando su vigencia sin
     * eliminar la fila. 409
     * {@code RESPONSIBLE_REPLACEMENT_REQUIRED} si se intenta
     * dar de baja al Responsable titular.
     */
    void bajaParticipante(long proyectoId, long participacionId,
            BajaParticipanteRequest request, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson);

    /** Sustituye al único Responsable vigente sin dejar una titularidad abierta ambigua. */
    ParticipanteResponse sustituirResponsable(long proyectoId,
            ResponsibleReplacementRequest request, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson);
}
