package pe.gob.midagri.piip.portafolio.seguimiento.controller;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AdjuntarEvidenciaCicloRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AltaPersonaRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AltaUnidadRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AnexarCicloVersionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.BajaParticipanteRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CancelacionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CorreccionCicloRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.EditarCamposEditablesRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.ParticipanteResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PlanificacionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PlanificacionResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PresentacionProductoFinalRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PresentacionProductoFinalResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.ResponsibleReplacementRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.SuspensionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.TransicionResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.service.CicloService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.ParticipanteProyectoService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.PresentacionProductoFinalService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.SeguimientoProyectoService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.TransicionProyectoService;


/**
 * Controlador REST de seguimiento del proyecto (US4, Constitucion
 * 5.0.0 y contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}).
 *
 * <p>Consolida los endpoints del modulo seguimiento del
 * portafolio: planificacion, ciclos, participantes, presentacion
 * del producto final, suspension y cancelacion. Delega toda la
 * logica a los servicios de aplicacion; nunca accede a
 * repositorios.
 *
 * <p>Las operaciones POST exigen la cabecera
 * {@code Idempotency-Key}; las operaciones PATCH y las
 * transiciones de estado (suspension, cancelacion) exigen
 * {@code If-Match} para control de concurrencia optimista. La
 * cabecera {@code X-Asignacion-Efectiva-Id} es obligatoria en
 * todos los endpoints sensibles. Los errores se devuelven con
 * {@code application/problem+json} y codigo canonico del
 * portafolio.
 *
 * <p>Alcance por tarea:
 * <ul>
 *   <li>T072 implementa planificacion, registro de ciclo,
 *       correccion de ciclo (append-only), cierre de ciclo y
 *       vinculacion de evidencia documental, incluidas sus
 *       consultas reales y el historial de versiones.</li>
 *   <li>T073 implementa los endpoints de participantes
 *       (alta persona, alta unidad, baja e histórico), la edición
 *       de los campos editables 17/19/23 y la presentación del
 *       producto final.</li>
 *   <li>T074 entregara suspension y cancelacion.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/portafolio/proyectos")
@Tag(name = "Portafolio - Seguimiento",
        description = "Planificacion, ciclos quincenales, participantes, "
                + "presentacion del producto final, suspension y cancelacion (US4).")
public class SeguimientoController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private static final String IF_MATCH_HEADER = "If-Match";

    private final SeguimientoProyectoService seguimientoService;
    private final CicloService cicloService;
    private final ParticipanteProyectoService participanteService;
    private final PresentacionProductoFinalService presentacionService;
    private final TransicionProyectoService transicionService;
    private final ObjectMapper objectMapper;

    public SeguimientoController(SeguimientoProyectoService seguimientoService,
            CicloService cicloService,
            ParticipanteProyectoService participanteService,
            PresentacionProductoFinalService presentacionService,
            TransicionProyectoService transicionService,
            ObjectMapper objectMapper) {
        this.seguimientoService = seguimientoService;
        this.cicloService = cicloService;
        this.participanteService = participanteService;
        this.presentacionService = presentacionService;
        this.transicionService = transicionService;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------------
    // Planificacion (T072)
    // ---------------------------------------------------------------------

    @Operation(
            summary = "Registrar planificacion del proyecto",
            description = "Registra la primera version de la planificacion "
                    + "del proyecto. La cabecera Idempotency-Key es "
                    + "obligatoria; X-Asignacion-Efectiva-Id identifica "
                    + "la asignacion efectiva del Responsable titular "
                    + "dentro de su ambito. La planificacion es "
                    + "append-only: las correcciones (T073) crean "
                    + "versiones adicionales conservando la fila "
                    + "anterior."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Planificacion registrada.",
                    headers = @Header(name = "ETag",
                            description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PlanificacionResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Solicitud malformada o falta Idempotency-Key o X-Asignacion-Efectiva-Id.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "403",
                    description = "ASSIGNMENT_SCOPE_DENIED o FORBIDDEN_PROFILE.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "PLANIFICACION_ALREADY_EXISTS o PROJECT_NOT_IN_EXECUTION.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422",
                    description = "VALIDATION_FAILED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/planificaciones",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlanificacionResponse> registrarPlanificacion(
            @PathVariable("id") Long id,
            @Valid @RequestBody PlanificacionRequest comando,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId);
        String payloadJson = serializarPayload(comando);
        PlanificacionResponse detalle = seguimientoService.registrarPlanificacion(
                id, comando, contexto, idempotencyKey, payloadJson);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(detalle.etag()).body(detalle);
    }

    @Operation(
            summary = "Consultar planificaciones del proyecto",
            description = "Devuelve el listado de planificaciones "
                    + "registradas para el proyecto en orden "
                    + "cronologico. La consulta es de solo lectura y "
                    + "no requiere cabecera Idempotency-Key."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Planificaciones del proyecto.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PlanificacionResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Solicitud malformada o falta X-Asignacion-Efectiva-Id.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @GetMapping(value = "/{id}/planificaciones", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<PlanificacionResponse>> listarPlanificaciones(
            @PathVariable("id") Long id,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ResponseEntity.ok(seguimientoService.listarPlanificaciones(id,
                construirContexto(asignacionId, actorSub, actorUsuarioId, correlationId)));
    }

    // ---------------------------------------------------------------------
    // Ciclos (T072)
    // ---------------------------------------------------------------------

    @Operation(
            summary = "Registrar primera version de un ciclo quincenal",
            description = "Registra un ciclo quincenal del proyecto. El "
                    + "periodo debe cumplir el formato canonico "
                    + "AAAA-Qn-Sn. Los campos objetivos, actividades "
                    + "y avance son obligatorios (regla "
                    + "CYCLE_INCOMPLETE). El avance debe estar en el "
                    + "rango [0, 100] validado por la CHECK "
                    + "CK_CP_AVANCE del DDL 015."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Ciclo registrado.",
                    headers = @Header(name = "ETag",
                            description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CicloResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Solicitud malformada o falta Idempotency-Key o X-Asignacion-Efectiva-Id.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "403",
                    description = "ASSIGNMENT_SCOPE_DENIED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "CYCLE_DUPLICATED o PROJECT_NOT_IN_EXECUTION.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422",
                    description = "INVALID_PERIOD_FORMAT, CYCLE_INCOMPLETE o CYCLE_AVANCE_OUT_OF_RANGE.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/ciclos",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CicloResponse> registrarCiclo(
            @PathVariable("id") Long id,
            @Valid @RequestBody CicloRequest comando,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId);
        String payloadJson = serializarPayload(comando);
        CicloResponse detalle = seguimientoService.registrarCiclo(
                id, comando, contexto, idempotencyKey, payloadJson);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(detalle.etag()).body(detalle);
    }

    @Operation(
            summary = "Listar ciclos del proyecto",
            description = "Devuelve los ciclos del proyecto en orden "
                    + "cronologico. La consulta es de solo lectura."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Ciclos del proyecto.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CicloResponse.class)))
    })
    @GetMapping(value = "/{id}/ciclos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CicloResponse>> listarCiclos(
            @PathVariable("id") Long id,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ResponseEntity.ok(seguimientoService.listarCiclos(id,
                construirContexto(asignacionId, actorSub, actorUsuarioId, correlationId)));
    }

    @Operation(
            summary = "Corregir ciclo creando nueva version append-only",
            description = "Corrige un ciclo del proyecto creando una "
                    + "nueva version. La fila original nunca se "
                    + "modifica; se inserta una nueva fila con "
                    + "NUMERO_VERSION incrementado y referencia a la "
                    + "anterior."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Nueva version registrada.",
                    headers = @Header(name = "ETag"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CicloResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Solicitud malformada o falta Idempotency-Key o X-Asignacion-Efectiva-Id.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "CYCLE_NOT_FOUND.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "CYCLE_PROJECT_MISMATCH.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/ciclos/{cicloId}/versiones",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CicloResponse> corregirCiclo(
            @PathVariable("id") Long id,
            @PathVariable("cicloId") Long cicloId,
            @Valid @RequestBody CorreccionCicloRequest comando,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId);
        String payloadJson = serializarPayload(comando);
        CicloResponse detalle = seguimientoService.corregirCiclo(
                id, cicloId, comando, contexto, idempotencyKey, payloadJson);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(detalle.etag()).body(detalle);
    }

    @Operation(
            summary = "Listar versiones de un ciclo del proyecto",
            description = "Devuelve el historial append-only de "
                    + "versiones de un ciclo en orden ascendente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Versiones del ciclo.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CicloResponse.class)))
    })
    @GetMapping(value = "/{id}/ciclos/{cicloId}/versiones",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CicloResponse>> listarVersionesCiclo(
            @PathVariable("id") Long id,
            @PathVariable("cicloId") Long cicloId,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ResponseEntity.ok(seguimientoService.listarVersionesCiclo(id, cicloId,
                construirContexto(asignacionId, actorSub, actorUsuarioId, correlationId)));
    }

    // ---------------------------------------------------------------------
    // Evidencia documental del ciclo (T072)
    // ---------------------------------------------------------------------

    @Operation(
            summary = "Adjuntar documento como evidencia del ciclo",
            description = "Vincula un documento del portafolio como "
                    + "evidencia opcional del ciclo. El tipo "
                    + "documental debe pertenecer al catalogo "
                    + "canonico (AutoevaluacionCicloTrabajo, "
                    + "SeguimientoAgilTableroKanban o "
                    + "MatrizPlanificacionCiclos) y el documento "
                    + "debe estar apto segun el servicio de "
                    + "aptitud documental."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Evidencia adjuntada."),
            @ApiResponse(responseCode = "400",
                    description = "Solicitud malformada o falta Idempotency-Key o X-Asignacion-Efectiva-Id.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "CYCLE_NOT_FOUND.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409", description = "CYCLE_PROJECT_MISMATCH o EVIDENCE_ALREADY_ATTACHED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422",
                    description = "EVIDENCE_TYPE_NOT_ALLOWED, EVIDENCE_NOT_ELIGIBLE o CYCLE_DOCUMENT_TYPE_INVALID.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/ciclos/{cicloId}/documentos",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> adjuntarEvidenciaCiclo(
            @PathVariable("id") Long id,
            @PathVariable("cicloId") Long cicloId,
            @Valid @RequestBody AdjuntarEvidenciaCicloRequest comando,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId);
        String payloadJson = serializarPayload(comando);
        cicloService.adjuntarEvidenciaDocumento(
                id, cicloId, comando.idDocumento(), comando.tipoDocumental(),
                contexto, idempotencyKey, payloadJson);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------------
    // Participantes (T073)
    // ---------------------------------------------------------------------

    @Operation(summary = "Listar histórico de participantes del proyecto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participaciones vigentes y dadas de baja.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ParticipanteResponse.class))),
            @ApiResponse(responseCode = "403", description = "ASSIGNMENT_SCOPE_DENIED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "PROJECT_NOT_FOUND.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @GetMapping(value = "/{id}/participantes", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<ParticipanteResponse>> listarParticipantesHistoricos(
            @PathVariable("id") Long id,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        return ResponseEntity.ok(participanteService.listarHistorico(id,
                construirContexto(asignacionId, actorSub, actorUsuarioId, correlationId),
                null, null));
    }

    @Operation(
            summary = "Alta de persona como participante del proyecto (T073)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Participante registrado.",
                    headers = @Header(name = "ETag"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ParticipanteResponse.class))),
            @ApiResponse(responseCode = "409",
                    description = "RESPONSIBLE_CARDINALITY.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/participantes/personas",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ParticipanteResponse> altaPersona(
            @PathVariable("id") Long id,
            @Valid @RequestBody AltaPersonaRequest comando,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId);
        String payloadJson = serializarPayload(comando);
        ParticipanteResponse detalle = participanteService.altaPersona(
                id, comando, contexto, idempotencyKey, payloadJson);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(detalle.etag()).body(detalle);
    }

    @Operation(summary = "Alta de unidad organizacional como participante (T073)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Unidad registrada.",
                    headers = @Header(name = "ETag"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ParticipanteResponse.class)))
    })
    @PostMapping(value = "/{id}/participantes/unidades",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ParticipanteResponse> altaUnidad(
            @PathVariable("id") Long id,
            @Valid @RequestBody AltaUnidadRequest comando,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId);
        String payloadJson = serializarPayload(comando);
        ParticipanteResponse detalle = participanteService.altaUnidad(
                id, comando, contexto, idempotencyKey, payloadJson);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(detalle.etag()).body(detalle);
    }

    @Operation(summary = "Baja logica de un participante (T073)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Baja registrada.")
    })
    @PostMapping(value = "/{id}/participaciones/{participacionId}/bajas",
            consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> bajaParticipante(
            @PathVariable("id") Long id,
            @PathVariable("participacionId") Long participacionId,
            @Valid @RequestBody BajaParticipanteRequest comando,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId);
        String payloadJson = serializarPayload(comando);
        participanteService.bajaParticipante(id, participacionId, comando, contexto,
                idempotencyKey, payloadJson);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/sustituciones-responsable", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ParticipanteResponse> sustituirResponsable(@PathVariable("id") Long id,
            @Valid @RequestBody ResponsibleReplacementRequest comando,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        ParticipanteResponse detalle = participanteService.sustituirResponsable(id, comando,
                construirContexto(asignacionId, actorSub, actorUsuarioId, correlationId, "UnidadAdmin"), idempotencyKey,
                serializarPayload(comando));
        return ResponseEntity.ok().eTag(detalle.etag()).body(detalle);
    }

    // ---------------------------------------------------------------------
    // Edicion de campos editables 17/19/23 (T073)
    // ---------------------------------------------------------------------

    @Operation(
            summary = "Editar campos editables 17, 19 y 23 (T073)",
            description = "Edita los campos oficiales editables 17 "
                    + "(Documentacion de la gestion), 19 "
                    + "(Resultados clave) y 23 (Nota) durante "
                    + "PROYECTO_EJECUCION. Exige cabecera If-Match."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Campos editados.",
                    headers = @Header(name = "ETag"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PlanificacionResponse.class))),
            @ApiResponse(responseCode = "400",
                    description = "Solicitud malformada o falta Idempotency-Key o X-Asignacion-Efectiva-Id.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "412",
                    description = "STATE_CHANGED por ETag obsoleto.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422",
                    description = "FIELD_NOT_EDITABLE.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "428",
                    description = "IF_MATCH_REQUIRED por cabecera ausente.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PatchMapping(value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PlanificacionResponse> editarCamposEditables(
            @PathVariable("id") Long id,
            @Valid @RequestBody EditarCamposEditablesRequest comando,
            @Parameter(description = "ETag esperado del proyecto.",
                    required = false)
            @RequestHeader(value = IF_MATCH_HEADER, required = false) String ifMatch,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader("X-Perfil-Efectivo") String perfilEfectivo,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
                    "IF_MATCH_REQUIRED: la edicion exige la cabecera If-Match");
        }
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId);
        String payloadJson = serializarPayload(comando);
        PlanificacionResponse detalle = seguimientoService.editarCamposEditables(
                id, comando, contexto, idempotencyKey, payloadJson);
        return ResponseEntity.ok().eTag(detalle.etag()).body(detalle);
    }

    // ---------------------------------------------------------------------
    // Presentacion del producto final (T073)
    // ---------------------------------------------------------------------

    @Operation(
            summary = "Presentar el producto final (T073)",
            description = "Registra la presentacion del producto "
                    + "final con tipo canonico, campos editables y "
                    + "documento formal de sustento y evidencias múltiples aptas. No realiza una "
                    + "transicion implicita: el estado del proyecto "
                    + "permanece en PROYECTO_EJECUCION."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Presentacion registrada.",
                    headers = @Header(name = "ETag"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PresentacionProductoFinalResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "ASSIGNMENT_SCOPE_DENIED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "PRESENTATION_NOT_ALLOWED_IN_STATE o STATE_TRANSITION_NOT_ALLOWED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422",
                    description = "PRODUCT_FINAL_TYPE_REQUIRED, EVIDENCE_NOT_ELIGIBLE u OFFICIAL_FIELD_REQUIRED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/producto-final/presentaciones",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PresentacionProductoFinalResponse> presentarProductoFinal(
            @PathVariable("id") Long id,
            @Valid @RequestBody PresentacionProductoFinalRequest comando,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId);
        String payloadJson = serializarPayload(comando);
        PresentacionProductoFinalResponse detalle = presentacionService.presentar(
                id, comando, contexto, idempotencyKey, payloadJson);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(detalle.etag()).body(detalle);
    }

    // ---------------------------------------------------------------------
    // Suspension y cancelacion (T074)
    // ---------------------------------------------------------------------

    @Operation(
            summary = "Suspender el proyecto (T074)",
            description = "UnidadAdmin suspende un proyecto en "
                    + "PROYECTO_EJECUCION con documento 'Evidencia "
                    + "de Suspension' y observacion obligatoria. "
                    + "Exige cabecera If-Match. SUSPENDIDO no "
                    + "admite transicion saliente (BR-012)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyecto suspendido.",
                    headers = @Header(name = "ETag"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransicionResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "ASSIGNMENT_SCOPE_DENIED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "STATE_TRANSITION_NOT_ALLOWED o SUSPENDED_NO_OUT_TRANSITION.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "412",
                    description = "STATE_CHANGED por ETag obsoleto.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422",
                    description = "EVIDENCE_NOT_ELIGIBLE u OBSERVATION_REQUIRED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "428",
                    description = "IF_MATCH_REQUIRED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/suspensiones",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransicionResponse> suspender(
            @PathVariable("id") Long id,
            @Valid @RequestBody SuspensionRequest comando,
            @Parameter(description = "ETag esperado del proyecto.",
                    required = false)
            @RequestHeader(value = IF_MATCH_HEADER, required = false) String ifMatch,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader("X-Perfil-Efectivo") String perfilEfectivo,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId, perfilEfectivo);
        SuspensionRequest comandoEfectivo = new SuspensionRequest(comando.idDocumento(),
                comando.observacion(), ifMatch);
        String payloadJson = serializarPayload(comandoEfectivo);
        TransicionResponse detalle = transicionService.suspender(
                id, comandoEfectivo, contexto, idempotencyKey, payloadJson);
        return ResponseEntity.ok().eTag(detalle.etag()).body(detalle);
    }

    @Operation(
            summary = "Cancelar el proyecto (T074)",
            description = "La Autoridad decide o el Evaluador "
                    + "registra con decision formal. Exige "
                    + "documento 'Informe de la Oficina de "
                    + "Modernizacion, Cancelacion' y observacion "
                    + "obligatoria. La fecha de cierre se genera "
                    + "automaticamente (BR-066)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Proyecto cancelado.",
                    headers = @Header(name = "ETag"),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = TransicionResponse.class))),
            @ApiResponse(responseCode = "403",
                    description = "ASSIGNMENT_SCOPE_DENIED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "STATE_TRANSITION_NOT_ALLOWED o CANCELLED_NO_OUT_TRANSITION.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "412",
                    description = "STATE_CHANGED por ETag obsoleto.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422",
                    description = "FORMAL_DOCUMENT_REQUIRED, EVIDENCE_NOT_ELIGIBLE u OBSERVATION_REQUIRED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "428",
                    description = "IF_MATCH_REQUIRED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/{id}/cancelaciones",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TransicionResponse> cancelar(
            @PathVariable("id") Long id,
            @Valid @RequestBody CancelacionRequest comando,
            @Parameter(description = "ETag esperado del proyecto.",
                    required = false)
            @RequestHeader(value = IF_MATCH_HEADER, required = false) String ifMatch,
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader("X-Perfil-Efectivo") String perfilEfectivo,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId, perfilEfectivo);
        CancelacionRequest comandoEfectivo = new CancelacionRequest(comando.idDocumento(),
                comando.observacion(), ifMatch);
        String payloadJson = serializarPayload(comandoEfectivo);
        TransicionResponse detalle = transicionService.cancelar(
                id, comandoEfectivo, contexto, idempotencyKey, payloadJson);
        return ResponseEntity.ok().eTag(detalle.etag()).body(detalle);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private PortafolioAuthContext construirContexto(Long asignacionId,
            String actorSub, Long actorUsuarioId, String correlationId) {
        return construirContexto(asignacionId, actorSub, actorUsuarioId, correlationId, "Responsable");
    }

    private PortafolioAuthContext construirContexto(Long asignacionId,
            String actorSub, Long actorUsuarioId, String correlationId, String perfilRequerido) {
        return new PortafolioAuthContext(
                actorSub,
                actorUsuarioId,
                asignacionId,
                perfilRequerido,
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
