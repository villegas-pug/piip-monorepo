package pe.gob.midagri.piip.auditoria.service;

/**
 * Ejecuta comandos idempotentes sin exponer la persistencia de auditoría a los módulos consumidores.
 */
public interface IdempotencyService {

    IdempotencyResult execute(IdempotencyRequest request, IdempotentOperation operation);

    @FunctionalInterface
    interface IdempotentOperation {
        IdempotencyResponse execute();
    }

    record IdempotencyRequest(
            String consumidor,
            String operacion,
            String clave,
            String payloadJson,
            String creadoPor) {
    }

    record IdempotencyResponse(
            String recursoTipo,
            Long recursoId,
            String respuestaJson) {
    }

    record IdempotencyResult(
            String recursoTipo,
            Long recursoId,
            String respuestaJson,
            boolean reutilizado) {
    }
}
