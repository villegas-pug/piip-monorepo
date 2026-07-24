package pe.gob.midagri.piip.portafolio.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

import pe.gob.midagri.piip.portafolio.dto.AdmissibilityRequest;
import pe.gob.midagri.piip.portafolio.dto.ApplicabilityRequest;
import pe.gob.midagri.piip.portafolio.dto.EvaluacionDetail;
import pe.gob.midagri.piip.portafolio.dto.OpenCorrectionRequest;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.SubsanacionDetail;
import pe.gob.midagri.piip.portafolio.dto.SubsanacionEditCommand;
import pe.gob.midagri.piip.portafolio.dto.TechnicalOpinionRequest;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.evaluacion.EvaluacionIniciativaService;
import pe.gob.midagri.piip.portafolio.evaluacion.EvaluacionIniciativaServiceImpl;
import pe.gob.midagri.piip.portafolio.evaluacion.SubsanacionIniciativaService;
import pe.gob.midagri.piip.portafolio.evaluacion.SubsanacionIniciativaServiceImpl;

/**
 * Controlador REST para subsanacion y evaluacion de iniciativa (US2).
 *
 * <p>Cinco endpoints conformes al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}:
 * <ul>
 *   <li>{@code POST /iniciativas/{id}/subsanaciones}: abre la subsanacion
 *       unica (Evaluador).</li>
 *   <li>{@code PATCH /iniciativas/{id}/subsanacion}: corrige campos 5-12,
 *       22 y 23 (Responsable, exige If-Match).</li>
 *   <li>{@code POST /iniciativas/{id}/subsanacion/cierre}: cierra la
 *       subsanacion registrando la atencion (Responsable/ Evaluador).</li>
 *   <li>{@code POST /iniciativas/{id}/evaluaciones/admisibilidad}:
 *       registra la decision de admision (Evaluador).</li>
 *   <li>{@code POST /iniciativas/{id}/evaluaciones/aplicabilidad}:
 *       registra la decision de aplicabilidad (Evaluador).</li>
 * </ul>
 * Las respuestas se devuelven con ETag y Problem Details en errores.
 */
@RestController
@RequestMapping("/api/v1/portafolio/iniciativas")
@Tag(name = "Portafolio - Evaluacion", description = "Subsanacion y evaluacion de iniciativa PIIP (US2).")
public class EvaluacionIniciativaController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final SubsanacionIniciativaService subsanacionService;
    private final SubsanacionIniciativaServiceImpl subsanacionServiceImpl;
    private final EvaluacionIniciativaService evaluacionService;
    private final EvaluacionIniciativaServiceImpl evaluacionServiceImpl;
    private final ObjectMapper objectMapper;

    public EvaluacionIniciativaController(
            SubsanacionIniciativaService subsanacionService,
            SubsanacionIniciativaServiceImpl subsanacionServiceImpl,
            EvaluacionIniciativaService evaluacionService,
            EvaluacionIniciativaServiceImpl evaluacionServiceImpl,
            ObjectMapper objectMapper) {
        this.subsanacionService = subsanacionService;
        this.subsanacionServiceImpl = subsanacionServiceImpl;
        this.evaluacionService = evaluacionService;
        this.evaluacionServiceImpl = evaluacionServiceImpl;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------------
    // Subsanacion
    // ---------------------------------------------------------------------

    @Operation(
            summary = "Abrir subsanacion unica de iniciativa",
            description = "El Evaluador abre la unica subsanacion de la iniciativa. El plazo debe "
                    + "ser estrictamente posterior a la apertura. La iniciativa debe seguir en "
                    + "PRESENTADO. Devuelve 201 con ETag y problem+json en errores.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Subsanacion abierta.",
                    headers = @Header(name = "ETag",
                            description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SubsanacionDetail.class))),
            @ApiResponse(responseCode = "409", description = "CORRECTION_ALREADY_USED o estado invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422", description = "SUBSANATION_PLAZO_INVALID.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/subsanaciones",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SubsanacionDetail> abrirSubsanacion(
            @PathVariable("id") Long id,
            @Valid @RequestBody OpenCorrectionRequest comando,
            @Parameter(description = "Clave de idempotencia. Misma clave y mismo payload devuelven el "
                    + "resultado original; payload distinto produce 409 Conflict.", required = false)
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        publicarContextoSubsanacion(idempotencyKey, serializarPayload(comando));
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId, "Evaluador");

        SubsanacionDetail detalle = subsanacionService.abrir(id, comando, contexto,
                idempotencyKey, serializarPayload(comando));
        return ResponseEntity.status(HttpStatus.CREATED).eTag(detalle.etag()).body(detalle);
    }

    @Operation(
            summary = "Editar subsanacion abierta",
            description = "El Responsable titular corrige los campos oficiales 5-12, 22 y 23 "
                    + "mientras la subsanacion este abierta y dentro de plazo. "
                    + "La cabecera If-Match es obligatoria para control de concurrencia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Correccion registrada.",
                    headers = @Header(name = "ETag",
                            description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SubsanacionDetail.class))),
            @ApiResponse(responseCode = "409", description = "CORRECTION_NOT_OPEN o estado invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "412", description = "STATE_CHANGED por ETag obsoleto.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PatchMapping(value = "/{id}/subsanacion",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SubsanacionDetail> editarSubsanacion(
            @PathVariable("id") Long id,
            @Valid @RequestBody SubsanacionEditCommand comando,
            @Parameter(description = "Version esperada del registro. Si no coincide, "
                    + "la operacion se rechaza con 412 STATE_CHANGED.", required = false)
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        validarIfMatch(ifMatch);
        publicarContextoSubsanacion(idempotencyKey, serializarPayload(comando));
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId, "Responsable");

        Long expectedVersion = parsearVersionDeIfMatch(ifMatch);
        SubsanacionDetail detalle = subsanacionService.editar(id, comando, contexto,
                expectedVersion, ifMatch, idempotencyKey, serializarPayload(comando));
        return ResponseEntity.ok().eTag(detalle.etag()).body(detalle);
    }

    @Operation(
            summary = "Cerrar subsanacion",
            description = "Registra la fecha de atencion de la subsanacion. La fila permanece "
                    + "para auditoria (no se elimina).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Subsanacion cerrada.",
                    headers = @Header(name = "ETag",
                            description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = SubsanacionDetail.class))),
            @ApiResponse(responseCode = "409", description = "CORRECTION_NOT_OPEN.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "412", description = "STATE_CHANGED por ETag obsoleto.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/subsanacion/cierre",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SubsanacionDetail> cerrarSubsanacion(
            @PathVariable("id") Long id,
            @Parameter(description = "Version esperada. Si no coincide, la operacion se rechaza "
                    + "con 412 STATE_CHANGED.", required = false)
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        validarIfMatch(ifMatch);
        publicarContextoSubsanacion(idempotencyKey, "{}");
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId, "Responsable");

        Long expectedVersion = parsearVersionDeIfMatch(ifMatch);
        SubsanacionDetail detalle = subsanacionService.cerrar(id, null, contexto,
                expectedVersion, ifMatch, idempotencyKey, "{}");
        return ResponseEntity.ok().eTag(detalle.etag()).body(detalle);
    }

    // ---------------------------------------------------------------------
    // Evaluacion
    // ---------------------------------------------------------------------

    @Operation(
            summary = "Registrar admisibilidad",
            description = "El Evaluador registra la decision de admision. El documento de "
                    + "Opinion Tecnica de Evaluacion (campo 14) es obligatorio. El resultado "
                    + "NO_ADMISIBLE transita la iniciativa solo si la subsanacion unica esta "
                    + "vencida o nunca se abrio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Admision registrada.",
                    headers = @Header(name = "ETag",
                            description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EvaluacionDetail.class))),
            @ApiResponse(responseCode = "403", description = "ASSIGNMENT_SCOPE_DENIED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "ADMISSIBILITY_ALREADY_RECORDED o estado invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422", description = "ADMISSIBILITY_INCOMPLETE o EVIDENCE_NOT_ELIGIBLE.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/evaluaciones/admisibilidad",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvaluacionDetail> registrarAdmisibilidad(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdmissibilityRequest comando,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        publicarContextoEvaluacion(idempotencyKey, serializarPayload(comando));
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId, "Evaluador");

        EvaluacionDetail detalle = evaluacionService.registrarAdmisibilidad(id, comando, contexto,
                idempotencyKey, serializarPayload(comando));
        return ResponseEntity.ok().eTag(detalle.etag()).body(detalle);
    }

    @Operation(
            summary = "Registrar aplicabilidad",
            description = "El Evaluador registra la decision de aplicabilidad. El motivo es "
                    + "obligatorio cuando el resultado es NO_APLICABLE. La lista estructurada "
                    + "de competencia, valor publico y caracter innovator se persiste como "
                    + "criterios de aplicabilidad.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aplicabilidad registrada.",
                    headers = @Header(name = "ETag",
                            description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = EvaluacionDetail.class))),
            @ApiResponse(responseCode = "403", description = "ASSIGNMENT_SCOPE_DENIED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "APPLICABILITY_ALREADY_RECORDED o estado invalido.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422", description = "APPLICABILITY_INCOMPLETE.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/evaluaciones/aplicabilidad",
            consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EvaluacionDetail> registrarAplicabilidad(
            @PathVariable("id") Long id,
            @Valid @RequestBody ApplicabilityRequest comando,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        publicarContextoEvaluacion(idempotencyKey, serializarPayload(comando));
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId, "Evaluador");

        EvaluacionDetail detalle = evaluacionService.registrarAplicabilidad(id, comando, contexto,
                idempotencyKey, serializarPayload(comando));
        return ResponseEntity.ok().eTag(detalle.etag()).body(detalle);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void publicarContextoSubsanacion(String clave, String payloadJson) {
        if (clave == null || clave.isBlank()) {
            return;
        }
        SubsanacionIniciativaServiceImpl.publicarContextoIdempotencia(clave, payloadJson);
    }

    private void publicarContextoEvaluacion(String clave, String payloadJson) {
        if (clave == null || clave.isBlank()) {
            return;
        }
        EvaluacionIniciativaServiceImpl.publicarContextoIdempotencia(clave, payloadJson);
    }

    private void validarIfMatch(String ifMatch) {
        if (ifMatch != null && ifMatch.isBlank()) {
            throw new PortafolioValidationException("IF_MATCH_INVALID",
                    "El valor de If-Match no puede estar vacio cuando se envia.");
        }
    }

    private Long parsearVersionDeIfMatch(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            return null;
        }
        String normalizado = ifMatch.trim();
        if (normalizado.startsWith("\"") && normalizado.endsWith("\"")) {
            normalizado = normalizado.substring(1, normalizado.length() - 1);
        }
        int guion = normalizado.lastIndexOf('-');
        if (guion < 0 || guion == normalizado.length() - 1) {
            return null;
        }
        String version = normalizado.substring(guion + 1);
        try {
            return Long.parseLong(version);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private PortafolioAuthContext construirContexto(Long asignacionId, String actorSub,
            Long actorUsuarioId, String correlationId, String perfilPorDefecto) {
        return new PortafolioAuthContext(
                actorSub,
                actorUsuarioId,
                asignacionId,
                perfilPorDefecto,
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
