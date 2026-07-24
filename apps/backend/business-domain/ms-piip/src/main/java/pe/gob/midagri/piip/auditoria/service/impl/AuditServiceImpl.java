package pe.gob.midagri.piip.auditoria.service.impl;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import pe.gob.midagri.piip.auditoria.entity.AuditoriaAccesoEntity;
import pe.gob.midagri.piip.auditoria.entity.AuditoriaEventoEntity;
import pe.gob.midagri.piip.auditoria.repository.AuditoriaAccesoRepository;
import pe.gob.midagri.piip.auditoria.repository.AuditoriaEventoRepository;
import pe.gob.midagri.piip.auditoria.service.AuditService;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final Pattern CORRELATION_ID_PATTERN = Pattern.compile("[A-Za-z0-9-]{1,64}");
    private static final Set<String> SENSITIVE_CHANGE_KEYS = Set.of(
            "password", "contrasena", "contraseña", "token", "secret", "secreto",
            "contenido", "content", "ruta", "path");

    private final AuditoriaEventoRepository eventoRepository;
    private final AuditoriaAccesoRepository accesoRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void registrarExito(AuditCommand command) {
        appendEvento(command, "EXITOSO");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarDenegacion(AuditCommand command) {
        appendEvento(command, "DENEGADO");
    }

    @Override
    @Transactional
    public void registrarAccesoExitoso(AuditAccessCommand command) {
        appendAcceso(command, "EXITOSO");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAccesoDenegado(AuditAccessCommand command) {
        appendAcceso(command, "DENEGADO");
    }

    private void appendAcceso(AuditAccessCommand command, String resultado) {
        if (command == null) {
            throw new IllegalArgumentException("El comando de auditoría de acceso es obligatorio");
        }
        validarAcceso(command);
        appendEvento(command.evento(), resultado);
        accesoRepository.append(toAccesoEntity(command));
    }

    private void appendEvento(AuditCommand command, String resultado) {
        validarCommand(command, resultado);
        try {
            eventoRepository.append(toEventoEntity(command, resultado));
        } catch (RuntimeException exception) {
            throw new IllegalStateException("No fue posible persistir la evidencia obligatoria de auditoría", exception);
        }
    }

    private AuditoriaEventoEntity toEventoEntity(AuditCommand command, String resultado) {
        AuditoriaEventoEntity entity = new AuditoriaEventoEntity();
        entity.setTipoEvento(command.operacion());
        entity.setEntidadTipo(command.recursoTipo());
        entity.setEntidadId(command.recursoId());
        entity.setUsuarioId(command.actorId());
        entity.setPayloadJson(serializePayload(command, resultado));
        return entity;
    }

    private AuditoriaAccesoEntity toAccesoEntity(AuditAccessCommand command) {
        AuditoriaAccesoEntity entity = new AuditoriaAccesoEntity();
        entity.setUsuarioId(command.evento().actorId());
        entity.setEndpoint(command.endpoint());
        entity.setMetodoHttp(command.metodoHttp().toUpperCase(Locale.ROOT));
        entity.setCodigoRespuesta(command.codigoRespuesta());
        entity.setIpCliente(command.ipCliente());
        entity.setDuracionMs(command.duracionMs());
        entity.setRolEfectivoId(command.perfilEfectivoId());
        entity.setUnidadEfectivaId(command.evento().unidadEfectivaId());
        entity.setAsignacionEfectivaId(command.evento().asignacionEfectivaId());
        return entity;
    }

    private String serializePayload(AuditCommand command, String resultado) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("correlationId", command.correlationId());
        payload.put("actorId", command.actorId());
        payload.put("identidadAnonimaMinima", command.identidadAnonimaMinima());
        payload.put("asignacionEfectivaId", command.asignacionEfectivaId());
        payload.put("perfilEfectivo", command.perfilEfectivo());
        payload.put("unidadEfectivaId", command.unidadEfectivaId());
        payload.put("operacion", command.operacion());
        payload.put("modulo", command.modulo());
        payload.put("recursoTipo", command.recursoTipo());
        payload.put("recursoId", command.recursoId());
        payload.put("instante", Instant.now().toString());
        payload.put("resultado", resultado);
        payload.put("codigoResultado", command.codigoResultado());
        payload.put("cambios", command.cambiosMinimos());
        payload.put("clasificacion", command.clasificacion());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible serializar la evidencia de auditoría", exception);
        }
    }

    private void validarCommand(AuditCommand command, String resultado) {
        if (command == null) {
            throw new IllegalArgumentException("El comando de auditoría es obligatorio");
        }
        validarTexto(command.correlationId(), "correlationId", 64);
        if (!CORRELATION_ID_PATTERN.matcher(command.correlationId()).matches()) {
            throw new IllegalArgumentException("correlationId no tiene un formato permitido");
        }
        if ((command.actorId() == null) == (isBlank(command.identidadAnonimaMinima()))) {
            throw new IllegalArgumentException("Debe registrar actorId o una identidad anónima mínima, pero no ambos");
        }
        validarIdOpcional(command.actorId(), "actorId");
        if (!isBlank(command.identidadAnonimaMinima())) {
            validarTexto(command.identidadAnonimaMinima(), "identidadAnonimaMinima", 50);
        }
        validarContextoEfectivo(command);
        validarTexto(command.operacion(), "operacion", 100);
        validarTexto(command.modulo(), "modulo", 50);
        validarTexto(command.recursoTipo(), "recursoTipo", 50);
        validarId(command.recursoId(), "recursoId");
        validarTexto(command.codigoResultado(), "codigoResultado", 100);
        validarTexto(command.clasificacion(), "clasificacion", 20);
        validarCambios(command.cambiosMinimos());
        if (!"EXITOSO".equals(resultado) && !"DENEGADO".equals(resultado)) {
            throw new IllegalArgumentException("El resultado de auditoría no es válido");
        }
    }

    private void validarContextoEfectivo(AuditCommand command) {
        boolean sinContexto = command.asignacionEfectivaId() == null
                && isBlank(command.perfilEfectivo()) && command.unidadEfectivaId() == null;
        boolean conContexto = command.asignacionEfectivaId() != null
                && !isBlank(command.perfilEfectivo()) && command.unidadEfectivaId() != null;
        if (!sinContexto && !conContexto) {
            throw new IllegalArgumentException("La asignación, perfil y unidad efectivos deben registrarse conjuntamente");
        }
        if (conContexto) {
            validarId(command.asignacionEfectivaId(), "asignacionEfectivaId");
            validarTexto(command.perfilEfectivo(), "perfilEfectivo", 50);
            validarId(command.unidadEfectivaId(), "unidadEfectivaId");
        }
    }

    private void validarAcceso(AuditAccessCommand command) {
        validarTexto(command.endpoint(), "endpoint", 300);
        validarTexto(command.metodoHttp(), "metodoHttp", 10);
        if (command.codigoRespuesta() == null || command.codigoRespuesta() < 100 || command.codigoRespuesta() > 599) {
            throw new IllegalArgumentException("codigoRespuesta no es válido");
        }
        validarTexto(command.ipCliente(), "ipCliente", 45);
        if (command.duracionMs() != null && command.duracionMs() < 0) {
            throw new IllegalArgumentException("duracionMs no puede ser negativa");
        }
        if (command.perfilEfectivoId() != null && command.perfilEfectivoId() <= 0) {
            throw new IllegalArgumentException("perfilEfectivoId debe ser positivo");
        }
    }

    private void validarCambios(Map<String, String> cambios) {
        if (cambios == null || cambios.size() > 30) {
            throw new IllegalArgumentException("Los cambios mínimos de auditoría no son válidos");
        }
        for (Map.Entry<String, String> cambio : cambios.entrySet()) {
            validarTexto(cambio.getKey(), "clave de cambio", 100);
            if (esClaveSensible(cambio.getKey())) {
                throw new IllegalArgumentException("Los cambios de auditoría contienen un campo sensible prohibido");
            }
            validarTexto(cambio.getValue(), "valor de cambio", 1000);
        }
    }

    private void validarId(Long value, String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " debe ser positivo");
        }
    }

    private void validarIdOpcional(Long value, String field) {
        if (value != null) {
            validarId(value, field);
        }
    }

    private void validarTexto(String value, String field, int maxLength) {
        if (isBlank(value) || value.length() > maxLength) {
            throw new IllegalArgumentException(field + " es obligatorio y excede el límite permitido");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean esClaveSensible(String key) {
        String normalizedKey = key.toLowerCase(Locale.ROOT);
        return SENSITIVE_CHANGE_KEYS.stream().anyMatch(normalizedKey::contains);
    }
}
