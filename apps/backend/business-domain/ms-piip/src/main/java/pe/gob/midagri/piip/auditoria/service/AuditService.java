package pe.gob.midagri.piip.auditoria.service;

import java.util.Map;

/** Contrato transversal append-only para la evidencia de auditoría PIIP. */
public interface AuditService {

    void registrarExito(AuditCommand command);

    void registrarDenegacion(AuditCommand command);

    void registrarAccesoExitoso(AuditAccessCommand command);

    void registrarAccesoDenegado(AuditAccessCommand command);

    record AuditCommand(
            String correlationId,
            Long actorId,
            String identidadAnonimaMinima,
            Long asignacionEfectivaId,
            String perfilEfectivo,
            Long unidadEfectivaId,
            String operacion,
            String modulo,
            String recursoTipo,
            Long recursoId,
            String codigoResultado,
            Map<String, String> cambiosMinimos,
            String clasificacion) {
    }

    record AuditAccessCommand(
            AuditCommand evento,
            String endpoint,
            String metodoHttp,
            Integer codigoRespuesta,
            String ipCliente,
            Integer duracionMs,
            Integer perfilEfectivoId) {
    }
}
