package pe.gob.midagri.piip.seguridad.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.Map;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.seguridad.dto.AssignmentAuthContext;
import pe.gob.midagri.piip.seguridad.dto.AssignmentChangeRequest;
import pe.gob.midagri.piip.seguridad.dto.AssignmentDetail;
import pe.gob.midagri.piip.seguridad.dto.AssignmentRequest;
import pe.gob.midagri.piip.seguridad.dto.RevocationRequest;
import pe.gob.midagri.piip.seguridad.service.AsignacionFuncionalService;

/** Controlador delgado del ciclo ordinario; no existe endpoint de bootstrap. */
@RestController
@RequestMapping("/api/v1/seguridad/asignaciones")
public class AsignacionController {
    private final AsignacionFuncionalService service; private final IdempotencyService idempotencia; private final ObjectMapper json;
    public AsignacionController(AsignacionFuncionalService service, IdempotencyService idempotencia, ObjectMapper json) { this.service = service; this.idempotencia = idempotencia; this.json = json; }
    @PostMapping
    public ResponseEntity<AssignmentDetail> crear(@Valid @RequestBody AssignmentRequest request, @RequestHeader("Idempotency-Key") String clave, @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion, @RequestHeader("X-Unidad-Efectiva-Id") Long unidad, @RequestHeader(value = "X-Correlation-Id", required = false) String correlacion, Principal principal) { return ResponseEntity.status(HttpStatus.CREATED).body(idempotente("CREAR_ASIGNACION", clave, request, () -> service.crear(request, contexto(principal, asignacion, unidad, correlacion)), principal)); }
    @PatchMapping("/{id}")
    public ResponseEntity<AssignmentDetail> cambiar(@PathVariable Long id, @Valid @RequestBody AssignmentChangeRequest request, @RequestHeader("If-Match") String ifMatch, @RequestHeader("Idempotency-Key") String clave, @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion, @RequestHeader("X-Unidad-Efectiva-Id") Long unidad, @RequestHeader(value = "X-Correlation-Id", required = false) String correlacion, Principal principal) { return ResponseEntity.ok(idempotente("CAMBIAR_ASIGNACION", clave, Map.of("id", id, "version", ifMatch, "body", request), () -> service.cambiar(id, request, version(ifMatch), contexto(principal, asignacion, unidad, correlacion)), principal)); }
    @PostMapping("/{id}/revocaciones")
    public ResponseEntity<AssignmentDetail> revocar(@PathVariable Long id, @Valid @RequestBody RevocationRequest request, @RequestHeader("Idempotency-Key") String clave, @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion, @RequestHeader("X-Unidad-Efectiva-Id") Long unidad, @RequestHeader(value = "X-Correlation-Id", required = false) String correlacion, Principal principal) { return ResponseEntity.ok(idempotente("REVOCAR_ASIGNACION", clave, Map.of("id", id, "body", request), () -> service.revocar(id, request, contexto(principal, asignacion, unidad, correlacion)), principal)); }
    private AssignmentAuthContext contexto(Principal p, Long a, Long u, String c) { return new AssignmentAuthContext(p == null ? null : p.getName(), a, u, c); }
    private Long version(String value) { try { return Long.valueOf(value.replace("\"", "").trim()); } catch (Exception e) { throw new org.springframework.web.server.ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "STATE_CHANGED"); } }
    private AssignmentDetail idempotente(String op, String key, Object request, java.util.function.Supplier<AssignmentDetail> action, Principal p) { try { var result = idempotencia.execute(new IdempotencyService.IdempotencyRequest("SEGURIDAD", op, key, json.writeValueAsString(request), p == null ? "" : p.getName()), () -> { AssignmentDetail detail = action.get(); try { return new IdempotencyService.IdempotencyResponse("ASIGNACION_FUNCIONAL", detail.id(), json.writeValueAsString(detail)); } catch (JsonProcessingException e) { throw new IllegalStateException(e); } }); return json.readValue(result.respuestaJson(), AssignmentDetail.class); } catch (JsonProcessingException e) { throw new IllegalStateException("No fue posible procesar la idempotencia de asignación.", e); } }
}
