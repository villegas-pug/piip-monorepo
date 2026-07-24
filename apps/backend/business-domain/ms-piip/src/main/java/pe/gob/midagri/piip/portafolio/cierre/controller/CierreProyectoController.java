package pe.gob.midagri.piip.portafolio.cierre.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pe.gob.midagri.piip.portafolio.cierre.dto.CierreProyectoRequest;
import pe.gob.midagri.piip.portafolio.cierre.dto.CierreProyectoResponse;
import pe.gob.midagri.piip.portafolio.cierre.service.CierreProyectoService;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;

/** Adaptador HTTP del cierre; no contiene reglas de negocio. */
@RestController
@RequestMapping("/api/v1/portafolio/proyectos")
public class CierreProyectoController {
    private final CierreProyectoService cierreProyectoService;
    private final ObjectMapper objectMapper;

    public CierreProyectoController(CierreProyectoService cierreProyectoService, ObjectMapper objectMapper) {
        this.cierreProyectoService = cierreProyectoService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(value = "/{proyectoId}/cierres", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CierreProyectoResponse> cerrar(@PathVariable long proyectoId,
            @Valid @RequestBody CierreProyectoRequest request,
            @RequestHeader("If-Match") String ifMatch,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader("X-Perfil-Efectivo") String perfilEfectivo,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        PortafolioAuthContext contexto = new PortafolioAuthContext(actorSub, actorUsuarioId, asignacionId,
                perfilEfectivo, 0L, 0L, correlationId);
        CierreProyectoResponse response = cierreProyectoService.cerrar(proyectoId, request, ifMatch,
                contexto, idempotencyKey, serializar(request));
        return ResponseEntity.ok().eTag(response.etag()).body(response);
    }

    private String serializar(CierreProyectoRequest request) {
        try { return objectMapper.writeValueAsString(request); }
        catch (JsonProcessingException exception) {
            throw new PortafolioValidationException("REQUEST_NOT_READABLE",
                    "No se pudo serializar el cuerpo para la idempotencia.");
        }
    }
}
