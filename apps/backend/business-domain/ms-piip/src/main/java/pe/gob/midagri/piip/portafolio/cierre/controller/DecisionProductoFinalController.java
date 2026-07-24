package pe.gob.midagri.piip.portafolio.cierre.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.portafolio.cierre.dto.DecisionProductoFinalRequest;
import pe.gob.midagri.piip.portafolio.cierre.dto.DecisionProductoFinalResponse;
import pe.gob.midagri.piip.portafolio.cierre.service.DecisionProductoFinalService;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;

/** Endpoint específico de la decisión formal, delegado a la máquina canónica. */
@RestController
@RequestMapping("/api/v1/portafolio/proyectos")
public class DecisionProductoFinalController {

    private final DecisionProductoFinalService decisionService;
    private final ObjectMapper objectMapper;

    public DecisionProductoFinalController(DecisionProductoFinalService decisionService,
            ObjectMapper objectMapper) {
        this.decisionService = decisionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/{proyectoId}/producto-final/decisiones",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<DecisionProductoFinalResponse> decidir(
            @PathVariable long proyectoId,
            @Valid @RequestBody DecisionProductoFinalRequest request,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader("X-Perfil-Efectivo") String perfilEfectivo,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
                    "IF_MATCH_REQUIRED");
        }
        PortafolioAuthContext contexto = new PortafolioAuthContext(actorSub, actorUsuarioId,
                asignacionId, perfilEfectivo, 0L, 0L, correlationId);
        DecisionProductoFinalResponse response = decisionService.decidir(proyectoId, request,
                ifMatch, contexto, idempotencyKey, serializar(request));
        return ResponseEntity.ok().eTag(response.etag()).body(response);
    }

    private String serializar(DecisionProductoFinalRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException exception) {
            throw new PortafolioValidationException("REQUEST_NOT_READABLE",
                    "No se pudo serializar el cuerpo para la idempotencia.");
        }
    }
}
