package pe.gob.midagri.piip.seguridad.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.seguridad.dto.CreateUserRequest;
import pe.gob.midagri.piip.seguridad.dto.ProvisioningAuthContext;
import pe.gob.midagri.piip.seguridad.dto.ProvisioningResult;
import pe.gob.midagri.piip.seguridad.dto.UserStatusRequest;
import pe.gob.midagri.piip.seguridad.dto.UserStatusResult;
import pe.gob.midagri.piip.seguridad.service.UsuarioProvisioningService;

/**
 * API institucional del ciclo ordinario de aprovisionamiento. Mantiene
 * Keycloak como fuente de identidad, Oracle como autoridad de roles y
 * unidad, e implementa consulta, reintento, desactivación, reactivación
 * y auditoría sin contraseñas. No expone un endpoint de bootstrap del
 * primer {@code GlobalAdmin}; esa inicialización pertenece a la semilla
 * SQL 021.
 */
@RestController
@RequestMapping("/api/v1/seguridad/usuarios")
@Tag(name = "Seguridad - Aprovisionamiento de usuarios")
public class UsuarioProvisioningController {

    private static final String IDEMPOTENCY_HEADER = "Idempotency-Key";
    private static final String EFFECTIVE_ASSIGNMENT_HEADER = "X-Asignacion-Efectiva-Id";
    private static final String EFFECTIVE_UNIT_HEADER = "X-Unidad-Efectiva-Id";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final UsuarioProvisioningService aprovisionamiento;
    private final IdempotencyService idempotencia;
    private final ObjectMapper json;

    public UsuarioProvisioningController(UsuarioProvisioningService aprovisionamiento,
            IdempotencyService idempotencia, ObjectMapper json) {
        this.aprovisionamiento = aprovisionamiento;
        this.idempotencia = idempotencia;
        this.json = json;
    }

    @Operation(summary = "Aprovisionar usuario institucional con Keycloak primero")
    @PostMapping
    public ResponseEntity<ProvisioningResult> crear(@Valid @RequestBody CreateUserRequest request,
            @RequestHeader(IDEMPOTENCY_HEADER) String clave,
            @RequestHeader(EFFECTIVE_ASSIGNMENT_HEADER) Long asignacion,
            @RequestHeader(EFFECTIVE_UNIT_HEADER) Long unidad,
            @RequestHeader(value = CORRELATION_HEADER, required = false) String correlacion,
            Principal principal) {
        ProvisioningResult resultado = idempotente("APROVISIONAR_USUARIO", clave, request,
                () -> aprovisionamiento.crear(request, contexto(principal, asignacion, unidad, correlacion)),
                principal, "OPERACION_APROVISIONAMIENTO", ProvisioningResult::operacionId,
                ProvisioningResult.class);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(resultado);
    }

    @Operation(summary = "Consultar una operación de aprovisionamiento")
    @GetMapping("/operaciones/{operacionId}")
    public ResponseEntity<ProvisioningResult> consultar(@PathVariable Long operacionId,
            @RequestHeader(EFFECTIVE_ASSIGNMENT_HEADER) Long asignacion,
            @RequestHeader(EFFECTIVE_UNIT_HEADER) Long unidad,
            @RequestHeader(value = CORRELATION_HEADER, required = false) String correlacion,
            Principal principal) {
        return ResponseEntity.ok(aprovisionamiento.consultar(operacionId,
                contexto(principal, asignacion, unidad, correlacion)));
    }

    @Operation(summary = "Reintentar una operación de aprovisionamiento recuperable")
    @PostMapping("/operaciones/{operacionId}/reintentos")
    public ResponseEntity<ProvisioningResult> reintentar(@PathVariable Long operacionId,
            @RequestHeader(IDEMPOTENCY_HEADER) String clave,
            @RequestHeader(EFFECTIVE_ASSIGNMENT_HEADER) Long asignacion,
            @RequestHeader(EFFECTIVE_UNIT_HEADER) Long unidad,
            @RequestHeader(value = CORRELATION_HEADER, required = false) String correlacion,
            Principal principal) {
        ProvisioningResult resultado = idempotente("REINTENTAR_APROVISIONAMIENTO", clave,
                operacionId,
                () -> aprovisionamiento.reintentar(operacionId,
                        contexto(principal, asignacion, unidad, correlacion)),
                principal, "OPERACION_APROVISIONAMIENTO", ProvisioningResult::operacionId,
                ProvisioningResult.class);
        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Desactivar un usuario institucional")
    @PostMapping("/{id}/desactivaciones")
    public ResponseEntity<UserStatusResult> desactivar(@PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request,
            @RequestHeader(IDEMPOTENCY_HEADER) String clave,
            @RequestHeader(EFFECTIVE_ASSIGNMENT_HEADER) Long asignacion,
            @RequestHeader(EFFECTIVE_UNIT_HEADER) Long unidad,
            @RequestHeader(value = CORRELATION_HEADER, required = false) String correlacion,
            Principal principal) {
        UserStatusResult resultado = idempotente("DESACTIVAR_USUARIO", clave,
                java.util.Map.of("id", id, "motivo", request.motivo()),
                () -> aprovisionamiento.desactivar(id, request,
                        contexto(principal, asignacion, unidad, correlacion)),
                principal, "USUARIO", UserStatusResult::usuarioId, UserStatusResult.class);
        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Reactivar un usuario institucional")
    @PostMapping("/{id}/reactivaciones")
    public ResponseEntity<UserStatusResult> reactivar(@PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request,
            @RequestHeader(IDEMPOTENCY_HEADER) String clave,
            @RequestHeader(EFFECTIVE_ASSIGNMENT_HEADER) Long asignacion,
            @RequestHeader(EFFECTIVE_UNIT_HEADER) Long unidad,
            @RequestHeader(value = CORRELATION_HEADER, required = false) String correlacion,
            Principal principal) {
        UserStatusResult resultado = idempotente("REACTIVAR_USUARIO", clave,
                java.util.Map.of("id", id, "motivo", request.motivo()),
                () -> aprovisionamiento.reactivar(id, request,
                        contexto(principal, asignacion, unidad, correlacion)),
                principal, "USUARIO", UserStatusResult::usuarioId, UserStatusResult.class);
        return ResponseEntity.ok(resultado);
    }

    private ProvisioningAuthContext contexto(Principal principal, Long asignacion, Long unidad,
            String correlacion) {
        return new ProvisioningAuthContext(principal == null ? null : principal.getName(),
                asignacion, unidad, correlacion);
    }

    private <T> T idempotente(String operacion, String clave, Object solicitud,
            java.util.function.Supplier<T> accion, Principal principal, String tipoRecurso,
            java.util.function.Function<T, Long> extractorId, Class<T> tipoRespuesta) {
        try {
            String payload = json.writeValueAsString(solicitud);
            var resultado = idempotencia.execute(
                    new IdempotencyService.IdempotencyRequest("SEGURIDAD", operacion, clave, payload,
                            principal == null ? "" : principal.getName()),
                    () -> {
                        T respuesta = accion.get();
                        try {
                            return new IdempotencyService.IdempotencyResponse(tipoRecurso,
                                    extractorId.apply(respuesta), json.writeValueAsString(respuesta));
                        } catch (JsonProcessingException excepcion) {
                            throw new IllegalStateException(
                                    "No fue posible serializar la respuesta de aprovisionamiento.", excepcion);
                        }
                    });
            return json.readValue(resultado.respuestaJson(), tipoRespuesta);
        } catch (JsonProcessingException excepcion) {
            throw new IllegalStateException("No fue posible procesar la idempotencia de aprovisionamiento.",
                    excepcion);
        }
    }
}
