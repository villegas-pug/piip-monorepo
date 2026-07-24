package pe.gob.midagri.piip.seguridad.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
import java.util.Map;
import java.util.function.Supplier;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.seguridad.dto.AssignmentAuthContext;
import pe.gob.midagri.piip.seguridad.dto.EarlyTerminationRequest;
import pe.gob.midagri.piip.seguridad.dto.SubstitutionDetail;
import pe.gob.midagri.piip.seguridad.dto.SubstitutionRequest;
import pe.gob.midagri.piip.seguridad.service.SuplenciaFuncionalService;

/** Adaptador HTTP del caso de uso; autorización y reglas permanecen en el servicio. */
@RestController
@RequestMapping("/api/v1/seguridad")
@Tag(name = "Seguridad - Suplencias")
public class SuplenciaController {
    private final SuplenciaFuncionalService service;
    private final IdempotencyService idempotencia;
    private final ObjectMapper json;

    public SuplenciaController(SuplenciaFuncionalService service, IdempotencyService idempotencia, ObjectMapper json) {
        this.service = service; this.idempotencia = idempotencia; this.json = json;
    }

    @PostMapping("/asignaciones/{titularId}/suplencias")
    @Operation(summary = "Crear una suplencia temporal sin solape")
    public ResponseEntity<SubstitutionDetail> crear(@PathVariable Long titularId,
            @Valid @RequestBody SubstitutionRequest request, @RequestHeader("Idempotency-Key") String clave,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion,
            @RequestHeader("X-Unidad-Efectiva-Id") Long unidad,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlacion, Principal principal) {
        var resultado = idempotente("CREAR_SUPLENCIA", clave, Map.of("titularId", titularId, "body", request),
                () -> service.crear(titularId, request, contexto(principal, asignacion, unidad, correlacion)), principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(resultado);
    }

    @PostMapping("/suplencias/{id}/terminaciones")
    @Operation(summary = "Terminar anticipadamente una suplencia por su misma autoridad")
    public ResponseEntity<SubstitutionDetail> terminar(@PathVariable Long id,
            @Valid @RequestBody EarlyTerminationRequest request, @RequestHeader("Idempotency-Key") String clave,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacion,
            @RequestHeader("X-Unidad-Efectiva-Id") Long unidad,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlacion, Principal principal) {
        return ResponseEntity.ok(idempotente("TERMINAR_SUPLENCIA", clave, Map.of("id", id, "body", request),
                () -> service.terminarAnticipadamente(id, request, contexto(principal, asignacion, unidad, correlacion)), principal));
    }

    private AssignmentAuthContext contexto(Principal p, Long a, Long u, String c) {
        return new AssignmentAuthContext(p == null ? null : p.getName(), a, u, c);
    }

    private SubstitutionDetail idempotente(String operacion, String clave, Object request,
            Supplier<SubstitutionDetail> accion, Principal principal) {
        try {
            var resultado = idempotencia.execute(new IdempotencyService.IdempotencyRequest("SEGURIDAD", operacion,
                    clave, json.writeValueAsString(request), principal == null ? "" : principal.getName()), () -> {
                        SubstitutionDetail detalle = accion.get();
                        try {
                            return new IdempotencyService.IdempotencyResponse("SUPLENCIA_FUNCIONAL", detalle.id(),
                                    json.writeValueAsString(detalle));
                        } catch (JsonProcessingException e) { throw new IllegalStateException(e); }
                    });
            return json.readValue(resultado.respuestaJson(), SubstitutionDetail.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No fue posible procesar la idempotencia de suplencia.", e);
        }
    }
}
