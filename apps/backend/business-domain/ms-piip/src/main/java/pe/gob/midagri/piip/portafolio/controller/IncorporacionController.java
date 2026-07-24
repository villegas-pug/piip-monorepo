package pe.gob.midagri.piip.portafolio.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import pe.gob.midagri.piip.portafolio.dto.CreateIncorporacionRequest;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionCorreccionRequest;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionDetail;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionResolucionConflictoRequest;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionValidacionRequest;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.service.IncorporacionRegistroService;
import pe.gob.midagri.piip.portafolio.service.impl.IncorporacionRegistroServiceImpl;

/**
 * Controlador REST para la incorporacion individual de informacion existente.
 * Delega a services; nunca accede a repositories.
 *
 * <p>Las respuestas exitosas incluyen {@code ETag} derivado de la version optimista
 * de la entidad. La cabecera {@code Idempotency-Key} es opcional; cuando se envia
 * y el bean {@code IdempotencyService} esta disponible, la misma clave con el mismo
 * payload retorna el resultado original; payload distinto produce 409 Conflict.
 * La cabecera {@code If-Match} permite control de concurrencia optimista.
 */
@RestController
@RequestMapping("/api/v1/portafolio/incorporaciones")
@Tag(name = "Portafolio - Incorporacion", description = "Incorporacion individual de informacion existente.")
public class IncorporacionController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final IncorporacionRegistroService incorporacionService;
    private final IncorporacionRegistroServiceImpl incorporacionServiceImpl;
    private final ObjectMapper objectMapper;

    public IncorporacionController(
            IncorporacionRegistroService incorporacionService,
            IncorporacionRegistroServiceImpl incorporacionServiceImpl,
            ObjectMapper objectMapper) {
        this.incorporacionService = incorporacionService;
        this.incorporacionServiceImpl = incorporacionServiceImpl;
        this.objectMapper = objectMapper;
    }

    @Operation(
            summary = "Registrar incorporacion individual",
            description = "Registra una nueva incorporacion individual en estado PENDIENTE. "
                    + "Devuelve 201 con ETag. Si el cliente envia Idempotency-Key y la "
                    + "operacion ya fue procesada, retorna el resultado original; payload "
                    + "distinto con la misma clave produce 409 Conflict.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Incorporacion registrada en estado PENDIENTE.",
                    headers = @Header(name = "ETag", description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = IncorporacionDetail.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud malformada o falta X-Asignacion-Efectiva-Id.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Hash duplicado o Idempotency-Key con payload distinto.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IncorporacionDetail> registrar(
            @Valid @RequestBody CreateIncorporacionRequest comando,
            @Parameter(description = "Clave de idempotencia opcional. Misma clave y mismo payload "
                    + "retornan el resultado original.", required = false)
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            HttpServletRequest request) {

        publicarContextoIdempotencia(idempotencyKey, serializarPayload(comando));
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub, actorUsuarioId, correlationId);

        IncorporacionDetail detalle = incorporacionService.registrar(comando, contexto);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(detalle.etag()).body(detalle);
    }

    @Operation(
            summary = "Corregir incorporacion pendiente",
            description = "Registra una correccion append-only. Solo permitido mientras la "
                    + "incorporacion siga en estado PENDIENTE.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Correccion registrada.",
                    headers = @Header(name = "ETag", description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = IncorporacionDetail.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud malformada o falta X-Asignacion-Efectiva-Id.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "La incorporacion no esta en estado PENDIENTE.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/correcciones",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IncorporacionDetail> corregir(
            @PathVariable("id") Long id,
            @Valid @RequestBody IncorporacionCorreccionRequest comando,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        validarIfMatch(ifMatch);
        publicarContextoIdempotencia(idempotencyKey, serializarPayload(comando));
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub, actorUsuarioId, correlationId);

        IncorporacionDetail detalle = incorporacionService.corregir(comando, contexto);
        return ResponseEntity.ok().eTag(detalle.etag()).body(detalle);
    }

    @Operation(
            summary = "Validar o rechazar incorporacion",
            description = "El Evaluador transita la incorporacion a VALIDADO o RECHAZADO. "
                    + "Bloqueada mientras existan conflictos sin resolver.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Incorporacion transicionada.",
                    headers = @Header(name = "ETag", description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = IncorporacionDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflictos sin resolver o estado no valido.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/validaciones",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IncorporacionDetail> validar(
            @PathVariable("id") Long id,
            @Valid @RequestBody IncorporacionValidacionRequest comando,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        validarIfMatch(ifMatch);
        publicarContextoIdempotencia(idempotencyKey, serializarPayload(comando));
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub, actorUsuarioId, correlationId);

        IncorporacionDetail detalle = incorporacionService.validar(comando, contexto);
        return ResponseEntity.ok().eTag(detalle.etag()).body(detalle);
    }

    @Operation(
            summary = "Resolver conflicto de incorporacion",
            description = "El Evaluador resuelve un conflicto abierto con evidencia documentada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conflicto resuelto.",
                    headers = @Header(name = "ETag", description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = IncorporacionDetail.class))),
            @ApiResponse(responseCode = "404", description = "Conflicto o incorporacion inexistente.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Conflicto ya resuelto.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/conflictos/{conflictoId}/resoluciones",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IncorporacionDetail> resolverConflicto(
            @PathVariable("id") Long id,
            @PathVariable("conflictoId") Long conflictoId,
            @Valid @RequestBody IncorporacionResolucionConflictoRequest comando,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        validarIfMatch(ifMatch);
        publicarContextoIdempotencia(idempotencyKey, serializarPayload(comando));
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub, actorUsuarioId, correlationId);

        IncorporacionDetail detalle = incorporacionService.resolverConflicto(comando, contexto);
        return ResponseEntity.ok().eTag(detalle.etag()).body(detalle);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void publicarContextoIdempotencia(String clave, String payloadJson) {
        if (clave == null || clave.isBlank()) {
            return;
        }
        IncorporacionRegistroServiceImpl.publicarContextoIdempotencia(clave, payloadJson);
    }

    private void validarIfMatch(String ifMatch) {
        if (ifMatch != null && ifMatch.isBlank()) {
            throw new PortafolioValidationException("IF_MATCH_INVALID",
                    "El valor de If-Match no puede estar vacio cuando se envia.");
        }
    }

    private PortafolioAuthContext construirContexto(
            Long asignacionId, String actorSub, Long actorUsuarioId, String correlationId) {
        return new PortafolioAuthContext(
                actorSub,
                actorUsuarioId,
                asignacionId,
                "Responsable",
                0L,
                0L,
                correlationId);
    }

    private String serializarPayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new PortafolioValidationException("REQUEST_NOT_READABLE",
                    "No se pudo serializar el cuerpo para calcular el hash canonico de idempotencia.");
        }
    }
}
