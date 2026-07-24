package pe.gob.midagri.piip.portafolio.transicion;

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
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;

/**
 * Controlador REST para la maquina de estados canonica del portafolio
 * (US2). Delega toda la logica a {@link TransicionEstadoService}; nunca
 * accede a repositorios.
 *
 * <p>Un solo endpoint publico:
 * {@code POST /api/v1/portafolio/transiciones/{id}} que confirma la
 * transicion del registro cuya id se recibe en la URL. La respuesta
 * exitosa incluye la cabecera {@code ETag} derivada de la version
 * optimista y la representacion {@link TransicionDetail}. La cabecera
 * {@code If-Match} es obligatoria y exige la version del registro.
 * La cabecera {@code Idempotency-Key} es opcional; cuando se envia y
 * el bean {@code IdempotencyService} esta disponible, misma clave y
 * mismo payload devuelven el resultado original; payload distinto
 * produce 409 Conflict.
 */
@RestController
@RequestMapping("/api/v1/portafolio/transiciones")
@Tag(name = "Portafolio - Transiciones",
        description = "Maquina de estados canonica del portafolio (US2).")
public class TransicionEstadoController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IF_MATCH_HEADER = "If-Match";

    private final TransicionEstadoService transicionService;
    private final ObjectMapper objectMapper;

    public TransicionEstadoController(TransicionEstadoService transicionService,
            ObjectMapper objectMapper) {
        this.transicionService = transicionService;
        this.objectMapper = objectMapper;
    }

    @Operation(
            summary = "Confirmar transicion de estado",
            description = "Confirma una transicion controlada de la maquina de "
                    + "estados canonica del portafolio. La cabecera If-Match es "
                    + "obligatoria y exige la version actual de la iniciativa; "
                    + "sin ella se responde 428 PRECONDITION_REQUIRED. Con un "
                    + "ETag que no coincide con la version persistida se "
                    + "responde 412 STATE_CHANGED. La transicion se ejecuta "
                    + "bajo bloqueo pesimista (PESSIMISTIC_WRITE) para "
                    + "resolver la carrera entre confirmaciones concurrentes; "
                    + "el primer committer gana y el resto recibe 412. La "
                    + "evidencia documental es obligatoria en todos los "
                    + "destinos canonicos. La auditoria de exito se emite en "
                    + "la misma transaccion de negocio; la de denegacion, en "
                    + "REQUIRES_NEW.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Transicion confirmada.",
                    headers = @Header(name = "ETag",
                            description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransicionDetail.class))),
            @ApiResponse(responseCode = "400",
                    description = "Solicitud malformada o falta Idempotency-Key o X-Asignacion-Efectiva-Id.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "403",
                    description = "ASSIGNMENT_SCOPE_DENIED o FORBIDDEN_PROFILE.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "STATE_TRANSITION_NOT_ALLOWED o estado terminal.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "412",
                    description = "STATE_CHANGED por ETag obsoleto.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422",
                    description = "FORMAL_DECISION_REQUIRED o EVIDENCE_NOT_ELIGIBLE.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "428",
                    description = "IF_MATCH_REQUIRED por cabecera ausente.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransicionDetail> transicionar(
            @PathVariable("id") Long id,
            @Valid @RequestBody TransicionCommand comando,
            @Parameter(description = "ETag esperado del registro. Si no coincide, "
                    + "la operacion se rechaza con 412 STATE_CHANGED. Si se omite, "
                    + "se rechaza con 428 IF_MATCH_REQUIRED.", required = false)
            @RequestHeader(value = IF_MATCH_HEADER, required = false) String ifMatch,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String ifMatchEfectivo = comando.ifMatch() != null ? comando.ifMatch() : ifMatch;
        if (ifMatchEfectivo == null || ifMatchEfectivo.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.PRECONDITION_REQUIRED,
                    "IF_MATCH_REQUIRED");
        }

        // El servicio recibe el ETag desde el cuerpo del comando para
        // poder manejar la transicion automatica por vencimiento de
        // subsanacion (sin If-Match del cliente). Aqui consolidamos
        // la cabecera HTTP y la enviamos al servicio.
        TransicionCommand comandoConIfMatch = new TransicionCommand(
                comando.destino(),
                comando.observaciones(),
                comando.documentoRefId(),
                ifMatchEfectivo,
                comando.tipoProductoFinal());

        String payloadJson = serializarPayload(comandoConIfMatch);
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId);

        TransicionDetail detalle = transicionService.transicionar(id, comandoConIfMatch,
                contexto, idempotencyKey, payloadJson);
        return ResponseEntity.ok().eTag(detalle.etag()).body(detalle);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private PortafolioAuthContext construirContexto(Long asignacionId, String actorSub,
            Long actorUsuarioId, String correlationId) {
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
