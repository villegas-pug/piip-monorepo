package pe.gob.midagri.piip.portafolio.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import pe.gob.midagri.piip.portafolio.dto.CreateInitiativeRequest;
import pe.gob.midagri.piip.portafolio.dto.InitiativeDetail;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.service.PresentarIniciativaService;

/**
 * Controlador REST para la gestión de iniciativas del portafolio.
 * Delega a services; nunca accede a repositories. La presentación exige
 * {@code Idempotency-Key} para reintentos seguros; la respuesta es {@code 201 Created} con
 * ETag y Problem Details en errores.
 */
@RestController
@RequestMapping("/api/v1/portafolio/iniciativas")
@Tag(name = "Portafolio - Iniciativas", description = "Presentación y consulta de iniciativas PIIP.")
public class IniciativaController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final PresentarIniciativaService presentarIniciativaService;
    private final ObjectMapper objectMapper;

    public IniciativaController(PresentarIniciativaService presentarIniciativaService,
            ObjectMapper objectMapper) {
        this.presentarIniciativaService = presentarIniciativaService;
        this.objectMapper = objectMapper;
    }

    @Operation(
            summary = "Presentar iniciativa",
            description = "Registra una iniciativa nueva. El servidor genera el código, fija el estado "
                    + "PRESENTADO y devuelve 201 con ETag. La operación exige Idempotency-Key para "
                    + "reintentos seguros; misma clave y mismo payload devuelven el resultado original, "
                    + "mientras que una clave con payload distinto produce 409 Conflict.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Iniciativa presentada con código generado.",
                    headers = @Header(name = "ETag", description = "Identificador de versión para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InitiativeDetail.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud malformada.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "Idempotency-Key reutilizada con payload distinto.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422", description = "Regla de negocio incumplida.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InitiativeDetail> presentar(
            @Valid @RequestBody CreateInitiativeRequest comando,
            @Parameter(description = "Clave de idempotencia; misma clave y mismo payload retornan el "
                    + "resultado original, clave con payload distinto produce 409.", required = true)
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            HttpServletRequest request) {

        String payloadJson = serializarPayload(comando);

        PortafolioAuthContext contexto = new PortafolioAuthContext(
                actorSub,
                actorUsuarioId,
                asignacionId,
                "Responsable",
                0L,
                0L,
                correlationId);

        InitiativeDetail detalle = presentarIniciativaService.presentar(comando, contexto,
                idempotencyKey, payloadJson);

        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag(detalle.etag())
                .body(detalle);
    }

    private String serializarPayload(CreateInitiativeRequest comando) {
        try {
            return objectMapper.writeValueAsString(comando);
        } catch (JsonProcessingException exception) {
            throw new PortafolioValidationException("REQUEST_NOT_READABLE",
                    "No se pudo serializar el cuerpo para calcular el hash canónico de idempotencia.");
        }
    }
}
