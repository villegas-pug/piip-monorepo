package pe.gob.midagri.piip.portafolio.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import pe.gob.midagri.piip.portafolio.dto.CreateDerivedProjectRequest;
import pe.gob.midagri.piip.portafolio.dto.DirectProjectRequest;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.ProjectDetail;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.service.CrearProyectoDerivadoService;
import pe.gob.midagri.piip.portafolio.service.CrearProyectoDirectoService;

/**
 * Controlador REST para la creacion de proyectos del portafolio (US3,
 * Constitucion 5.0.0). Delega a los servicios de aplicacion; nunca
 * accede a repositorios.
 *
 * <p>Dos endpoints publicos conforme al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}:
 * <ul>
 *   <li>{@code POST /api/v1/portafolio/iniciativas/{id}/proyecto-derivado}
 *       crea el unico proyecto derivado de una iniciativa
 *       {@code INICIATIVA_APROBADA}. Solo el {@code Responsable} puede
 *       invocarlo.</li>
 *   <li>{@code POST /api/v1/portafolio/proyectos-directos} registra un
 *       proyecto directo heredado o excepcional, sin iniciativa origen.
 *       Solo la {@code Autoridad} o el {@code Evaluador} con documento
 *       formal pueden invocarlo.</li>
 * </ul>
 *
 * <p>Las respuestas exitosas incluyen {@code ETag} derivado de la version
 * optimista y la representacion {@link ProjectDetail}. La cabecera
 * {@code Idempotency-Key} es obligatoria en ambos endpoints; misma clave
 * y mismo payload devuelven el resultado original. La cabecera
 * {@code X-Asignacion-Efectiva-Id} es obligatoria y la revalidacion
 * efectiva se ejecuta en el servicio de {@code seguridad}. Los errores se
 * devuelven con {@code application/problem+json} y codigo canonico del
 * portafolio.
 */
@RestController
@RequestMapping("/api/v1/portafolio")
@Tag(name = "Portafolio - Proyectos", description = "Creacion de proyectos derivados y directos (US3).")
public class ProyectoController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final CrearProyectoDerivadoService derivadoService;
    private final CrearProyectoDirectoService directoService;
    private final ObjectMapper objectMapper;

    public ProyectoController(CrearProyectoDerivadoService derivadoService,
            CrearProyectoDirectoService directoService, ObjectMapper objectMapper) {
        this.derivadoService = derivadoService;
        this.directoService = directoService;
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------------
    // Proyecto derivado
    // ---------------------------------------------------------------------

    @Operation(
            summary = "Crear proyecto derivado de iniciativa aprobada",
            description = "El Responsable autorizado crea el unico proyecto derivado de una iniciativa "
                + "INICIATIVA_APROBADA. El servidor copia el codigo de origen y el tipo de "
                + "solucion, genera el codigo propio bajo PESSIMISTIC_WRITE y fija el estado "
                + "PROYECTO_EJECUCION. El vinculo iniciativa-proyecto es inmutable. La "
                + "cabecera Idempotency-Key es obligatoria. Un segundo intento de derivado "
                + "para la misma iniciativa falla con 409 DERIVATION_ALREADY_EXISTS; dos "
                + "solicitudes concurrentes son serializadas por bloqueo pesimista y la "
                + "primera confirmacion gana."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proyecto derivado creado.",
                    headers = @Header(name = "ETag",
                            description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProjectDetail.class))),
            @ApiResponse(responseCode = "400",
                    description = "Solicitud malformada o falta Idempotency-Key o X-Asignacion-Efectiva-Id.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "403",
                    description = "ASSIGNMENT_SCOPE_DENIED o FORBIDDEN_PROFILE.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "DERIVATION_ALREADY_EXISTS o INITIATIVE_NOT_APPROVED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422",
                    description = "FORMAL_DOCUMENT_REQUIRED, UNIT_MAIN_CARDINALITY u OFFICIAL_FIELD_REQUIRED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/iniciativas/{id}/proyecto-derivado",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectDetail> crearDerivado(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateDerivedProjectRequest comando,
            @Parameter(description = "Clave de idempotencia; misma clave y mismo payload devuelven el "
                    + "resultado original, clave con payload distinto produce 409.", required = true)
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String payloadJson = serializarPayload(comando);
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId, "Responsable");

        ProjectDetail detalle = derivadoService.crear(id, comando, contexto,
                idempotencyKey, payloadJson);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(detalle.etag()).body(detalle);
    }

    // ---------------------------------------------------------------------
    // Proyecto directo
    // ---------------------------------------------------------------------

    @Operation(
            summary = "Crear proyecto directo heredado o excepcional",
            description = "La Autoridad o el Evaluador con documento formal registra un proyecto directo "
                + "heredado o excepcional sin iniciativa origen. El servidor fija el estado "
                + "PROYECTO_EJECUCION, exige codigo propio y se asegura de que solo exista un "
                + "proyecto directo activo por unidad ejecutora y anio. La cabecera "
                + "Idempotency-Key es obligatoria. Un segundo directo concurrente para la misma "
                + "unidad y anio falla con 409 DIRECT_PROJECT_NOT_AUTHORIZED. El Responsable "
                + "queda excluido por constitucion y recibe 403 ASSIGNMENT_SCOPE_DENIED."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Proyecto directo creado.",
                    headers = @Header(name = "ETag",
                            description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ProjectDetail.class))),
            @ApiResponse(responseCode = "400",
                    description = "Solicitud malformada o falta Idempotency-Key o X-Asignacion-Efectiva-Id.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "403",
                    description = "ASSIGNMENT_SCOPE_DENIED o FORBIDDEN_PROFILE.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "DIRECT_PROJECT_NOT_AUTHORIZED o DIRECT_PROJECT_DUPLICATE.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422",
                    description = "FORMAL_DOCUMENT_REQUIRED, EVIDENCE_NOT_ELIGIBLE u OFFICIAL_FIELD_REQUIRED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/proyectos-directos",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ProjectDetail> crearDirecto(
            @Valid @RequestBody DirectProjectRequest comando,
            @Parameter(description = "Clave de idempotencia; misma clave y mismo payload devuelven el "
                    + "resultado original, clave con payload distinto produce 409.", required = true)
            @RequestHeader(IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId) {

        String payloadJson = serializarPayload(comando);
        // El servicio valida que el perfil efectivo sea Autoridad o Evaluador.
        // El perfil por defecto "Autoridad" representa el caso comun de la
        // Constitucion; el Evaluador reusa el mismo endpoint con su
        // asignacion efectiva.
        PortafolioAuthContext contexto = construirContexto(asignacionId, actorSub,
                actorUsuarioId, correlationId, "Autoridad");

        ProjectDetail detalle = directoService.crear(comando, contexto,
                idempotencyKey, payloadJson);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(detalle.etag()).body(detalle);
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

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
