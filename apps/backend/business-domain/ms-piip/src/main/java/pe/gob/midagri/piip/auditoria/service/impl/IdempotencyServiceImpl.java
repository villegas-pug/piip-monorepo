package pe.gob.midagri.piip.auditoria.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import pe.gob.midagri.piip.auditoria.entity.EstadoSolicitudIdempotente;
import pe.gob.midagri.piip.auditoria.entity.SolicitudIdempotenteEntity;
import pe.gob.midagri.piip.auditoria.repository.SolicitudIdempotenteRepository;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private static final int MAX_CONSUMIDOR_OPERACION_CLAVE = 100;
    private static final int MAX_RECURSO_TIPO = 50;

    private final SolicitudIdempotenteRepository repository;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    @Value("${piip.idempotency.retention:PT168H}")
    private Duration retention;

    @Override
    public IdempotencyResult execute(IdempotencyRequest request, IdempotentOperation operation) {
        validateRequest(request, operation);
        String hashPayload = hashCanonicalPayload(request.payloadJson());

        try {
            return inTransaction(() -> executeLocked(request, hashPayload, operation));
        } catch (ReservationRaceException exception) {
            // La UK Oracle es la autoridad entre nodos. Tras el rollback, el segundo intento
            // adquiere el bloqueo pesimista de la fila confirmada por el ganador.
            return inTransaction(() -> replayAfterReservationRace(request, hashPayload));
        }
    }

    private IdempotencyResult executeLocked(
            IdempotencyRequest request, String hashPayload, IdempotentOperation operation) {
        return repository.findByConsumidorOperacionClaveForUpdate(
                request.consumidor(), request.operacion(), request.clave())
                .map(existing -> resolveExisting(existing, hashPayload))
                .orElseGet(() -> createAndExecute(request, hashPayload, operation));
    }

    private IdempotencyResult replayAfterReservationRace(IdempotencyRequest request, String hashPayload) {
        SolicitudIdempotenteEntity existing = repository.findByConsumidorOperacionClaveForUpdate(
                request.consumidor(), request.operacion(), request.clave())
                .orElseThrow(() -> new IllegalStateException(
                        "La solicitud idempotente no quedó disponible después de la carrera."));
        return resolveExisting(existing, hashPayload);
    }

    private IdempotencyResult createAndExecute(
            IdempotencyRequest request, String hashPayload, IdempotentOperation operation) {
        SolicitudIdempotenteEntity solicitud = new SolicitudIdempotenteEntity();
        solicitud.setConsumidor(request.consumidor());
        solicitud.setOperacion(request.operacion());
        solicitud.setClave(request.clave());
        solicitud.setHashPayload(hashPayload);
        solicitud.setEstadoTecnico(EstadoSolicitudIdempotente.INICIADA);
        solicitud.setFechaExpiracion(LocalDateTime.now().plus(retention));
        solicitud.setCreadoPor(request.creadoPor());

        try {
            repository.createAndFlush(solicitud);
        } catch (RuntimeException exception) {
            if (isUniqueConstraintViolation(exception)) {
                throw new ReservationRaceException(exception);
            }
            throw exception;
        }

        IdempotencyResponse response = operation.execute();
        if (response == null) {
            throw new IllegalStateException("La operación idempotente debe devolver un resultado.");
        }
        validateResponse(response);

        solicitud.setRecursoTipo(response.recursoTipo());
        solicitud.setRecursoId(response.recursoId());
        solicitud.setRespuestaJson(response.respuestaJson());
        solicitud.setEstadoTecnico(EstadoSolicitudIdempotente.COMPLETADA);
        return new IdempotencyResult(
                response.recursoTipo(), response.recursoId(), response.respuestaJson(), false);
    }

    private IdempotencyResult resolveExisting(SolicitudIdempotenteEntity existing, String hashPayload) {
        if (!existing.getHashPayload().equalsIgnoreCase(hashPayload)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La clave de idempotencia ya fue usada con un payload diferente.");
        }
        if (existing.getEstadoTecnico() != EstadoSolicitudIdempotente.COMPLETADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La operación asociada a la clave de idempotencia no tiene un resultado reutilizable.");
        }
        return new IdempotencyResult(
                existing.getRecursoTipo(), existing.getRecursoId(), existing.getRespuestaJson(), true);
    }

    private IdempotencyResult inTransaction(TransactionCallback callback) {
        IdempotencyResult result = new TransactionTemplate(transactionManager).execute(
                status -> callback.execute());
        if (result == null) {
            throw new IllegalStateException("La transacción de idempotencia finalizó sin resultado.");
        }
        return result;
    }

    private String hashCanonicalPayload(String payloadJson) {
        try {
            JsonNode parsed = objectMapper.readTree(payloadJson);
            if (parsed == null) {
                throw new IllegalArgumentException("El payload idempotente debe contener JSON.");
            }
            String canonicalPayload = objectMapper.writeValueAsString(canonicalize(parsed));
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonicalPayload.getBytes(StandardCharsets.UTF_8)));
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("El payload idempotente debe contener JSON válido.", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 no está disponible en la JVM.", exception);
        }
    }

    private JsonNode canonicalize(JsonNode node) {
        if (node.isObject()) {
            ObjectNode canonical = JsonNodeFactory.instance.objectNode();
            Map<String, JsonNode> fields = new TreeMap<>();
            Iterator<Map.Entry<String, JsonNode>> iterator = node.fields();
            iterator.forEachRemaining(entry -> fields.put(entry.getKey(), entry.getValue()));
            fields.forEach((name, value) -> canonical.set(name, canonicalize(value)));
            return canonical;
        }
        if (node.isArray()) {
            ArrayNode canonical = JsonNodeFactory.instance.arrayNode();
            node.forEach(value -> canonical.add(canonicalize(value)));
            return canonical;
        }
        return node;
    }

    private void validateRequest(IdempotencyRequest request, IdempotentOperation operation) {
        if (request == null || operation == null) {
            throw new IllegalArgumentException("La solicitud y la operación idempotente son obligatorias.");
        }
        validateRequired(request.consumidor(), "consumidor", MAX_CONSUMIDOR_OPERACION_CLAVE);
        validateRequired(request.operacion(), "operación", MAX_CONSUMIDOR_OPERACION_CLAVE);
        validateRequired(request.clave(), "clave", MAX_CONSUMIDOR_OPERACION_CLAVE);
        validateRequired(request.payloadJson(), "payloadJson", Integer.MAX_VALUE);
        validateRequired(request.creadoPor(), "creadoPor", MAX_CONSUMIDOR_OPERACION_CLAVE);
    }

    private void validateResponse(IdempotencyResponse response) {
        if (response.recursoTipo() != null && response.recursoTipo().length() > MAX_RECURSO_TIPO) {
            throw new IllegalArgumentException("recursoTipo excede la longitud permitida.");
        }
    }

    private void validateRequired(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " es obligatorio.");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(field + " excede la longitud permitida.");
        }
    }

    private boolean isUniqueConstraintViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof SQLException sqlException && sqlException.getErrorCode() == 1) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    @FunctionalInterface
    private interface TransactionCallback {
        IdempotencyResult execute();
    }

    private static final class ReservationRaceException extends RuntimeException {
        private ReservationRaceException(Throwable cause) {
            super(cause);
        }
    }
}
