package pe.gob.midagri.piip.portafolio.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.Principal;
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
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.ResponsibleReplacementDetail;
import pe.gob.midagri.piip.portafolio.dto.ResponsibleReplacementRequest;
import pe.gob.midagri.piip.portafolio.service.ResponsableTitularService;

/** Adaptador HTTP del comando de sustitución; la autoridad vive en el servicio. */
@RestController
@RequestMapping("/api/v1/portafolio/registros")
@Tag(name = "Portafolio - Responsable titular")
public class ResponsableTitularController {

    private final ResponsableTitularService service;
    private final IdempotencyService idempotencia;
    private final ObjectMapper json;

    public ResponsableTitularController(
            ResponsableTitularService service, IdempotencyService idempotencia, ObjectMapper json) {
        this.service = service;
        this.idempotencia = idempotencia;
        this.json = json;
    }

    @PostMapping(value = "/{registroId}/sustituciones-responsable")
    @Operation(summary = "Sustituir inmediatamente al Responsable titular")
    public ResponseEntity<ResponsibleReplacementDetail> sustituir(
            @PathVariable Long registroId,
            @Valid @RequestBody ResponsibleReplacementRequest comando,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlacionId,
            Principal principal) {
        try {
            String payload = json.writeValueAsString(java.util.Map.of("registroId", registroId, "body", comando));
            var resultado = idempotencia.execute(new IdempotencyService.IdempotencyRequest(
                    "PORTAFOLIO", "SUSTITUIR_RESPONSABLE_TITULAR", idempotencyKey, payload,
                    principal == null ? "" : principal.getName()), () -> {
                        ResponsibleReplacementDetail detalle = service.sustituir(registroId, comando,
                                new PortafolioAuthContext(principal == null ? null : principal.getName(), null,
                                        asignacionId, null, null, null, correlacionId));
                        try {
                            return new IdempotencyService.IdempotencyResponse("PROYECTO_RESPONSABLE",
                                    detalle.titularidadNuevaId(), json.writeValueAsString(detalle));
                        } catch (JsonProcessingException ex) {
                            throw new IllegalStateException("No se pudo serializar la sustitución.", ex);
                        }
                    });
            return ResponseEntity.status(HttpStatus.OK)
                    .body(json.readValue(resultado.respuestaJson(), ResponsibleReplacementDetail.class));
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No se pudo procesar la idempotencia de la sustitución.", ex);
        }
    }
}
