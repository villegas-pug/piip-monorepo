package pe.gob.midagri.piip.documentos.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pe.gob.midagri.piip.config.ApiHeaders;
import pe.gob.midagri.piip.documentos.dto.ClasificacionHistDetalle;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.ReclasificarDocumentoCommand;
import pe.gob.midagri.piip.documentos.dto.ReclasificarDocumentoRequest;
import pe.gob.midagri.piip.documentos.dto.ReclasificacionDocumentoResult;
import pe.gob.midagri.piip.documentos.dto.ValidacionClasificacionResult;
import pe.gob.midagri.piip.documentos.dto.ValidarClasificacionCommand;
import pe.gob.midagri.piip.documentos.dto.ValidarClasificacionRequest;
import pe.gob.midagri.piip.documentos.service.ClasificacionDocumentoService;
import pe.gob.midagri.piip.documentos.service.DocumentoAuthorizedContextResolver;

/**
 * Controlador REST de validación y reclasificación documental.
 * Reutiliza la autorización efectiva Oracle común al módulo y la
 * cabecera canónica de correlación. No expone entidades JPA ni
 * decisiones internas; los errores se devuelven como
 * {@code application/problem+json} mediante
 * {@code DocumentosExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1/documentos")
@Tag(name = "Documentos - Clasificación", description = "Validación, reclasificación e historial documental.")
public class ClasificacionDocumentoController {

    private static final String PERFIL_EVALUADOR = "Evaluador";
    private static final String HEADER_ACTOR_SUB = "X-Actor-Sub";
    private static final String HEADER_UNIDAD_RECURSO = "X-Unidad-Recurso-Id";

    private final ClasificacionDocumentoService service;
    private final DocumentoAuthorizedContextResolver contextoResolver;

    public ClasificacionDocumentoController(ClasificacionDocumentoService service,
            DocumentoAuthorizedContextResolver contextoResolver) {
        this.service = service;
        this.contextoResolver = contextoResolver;
    }

    @Operation(
            summary = "Validar clasificación inicial",
            description = "Confirma la clasificación inicial propuesta por el "
                    + "Responsable. La fecha del servidor la aplica el "
                    + "DEFAULT SYSTIMESTAMP del DDL 004. Solo un Evaluador "
                    + "puede validar; la operación exige If-Match.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Clasificación validada.",
                    headers = @Header(name = "ETag", description = "Versión optimista de la fila."),
                    content = @Content(schema = @Schema(implementation = ValidacionClasificacionResult.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud malformada."),
            @ApiResponse(responseCode = "403", description = "Asignación efectiva insuficiente."),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado."),
            @ApiResponse(responseCode = "409", description = "ETag desactualizado o reclasificación más restrictiva."),
            @ApiResponse(responseCode = "412", description = "ETag ausente o mismatch."),
            @ApiResponse(responseCode = "428", description = "If-Match obligatorio.")
    })
    @PostMapping(value = "/{documentoId}/clasificacion/validacion", produces = "application/json")
    public ResponseEntity<ValidacionClasificacionResult> validar(
            @PathVariable Long documentoId,
            @RequestBody @Valid ValidarClasificacionRequest solicitud,
            @RequestHeader(value = ApiHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID) Long asignacionEfectivaId,
            @RequestHeader(value = HEADER_ACTOR_SUB, required = false) String actorSub,
            @RequestHeader(value = ApiHeaders.CORRELATION_ID, required = false) String correlationId,
            @RequestHeader(value = HEADER_UNIDAD_RECURSO) Long unidadRecursoId) {
        if (solicitud == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLASIFICACION_REQUERIDA");
        }
        DocumentoAuthorizedContext contexto = contextoResolver.resolver(
                actorSub, asignacionEfectivaId, PERFIL_EVALUADOR, unidadRecursoId, documentoId, correlationId);
        ValidacionClasificacionResult resultado = service.validarClasificacion(
                contexto, documentoId, ifMatch, new ValidarClasificacionCommand(solicitud.clasificacion()));
        return ResponseEntity.ok().eTag(resultado.etag()).body(resultado);
    }

    @Operation(
            summary = "Reclasificar documento",
            description = "Aplica una reclasificación con la decisión formal de "
                    + "la Autoridad y la registra en el historial append-only. "
                    + "La reclasificación nunca puede ser menos restrictiva. "
                    + "Una reclasificación a PUBLICO exige además un documento "
                    + "formal de decisión registrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reclasificación aplicada.",
                    headers = @Header(name = "ETag", description = "Versión optimista de la fila."),
                    content = @Content(schema = @Schema(implementation = ReclasificacionDocumentoResult.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud malformada."),
            @ApiResponse(responseCode = "403", description = "Asignación efectiva insuficiente."),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado."),
            @ApiResponse(responseCode = "409", description = "Reclasificación no permitida o sin cambio."),
            @ApiResponse(responseCode = "412", description = "ETag ausente o mismatch."),
            @ApiResponse(responseCode = "428", description = "If-Match obligatorio.")
    })
    @PostMapping(value = "/{documentoId}/clasificacion/reclasificacion", produces = "application/json")
    public ResponseEntity<ReclasificacionDocumentoResult> reclasificar(
            @PathVariable Long documentoId,
            @RequestBody @Valid ReclasificarDocumentoRequest solicitud,
            @RequestHeader(value = ApiHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID) Long asignacionEfectivaId,
            @RequestHeader(value = HEADER_ACTOR_SUB, required = false) String actorSub,
            @RequestHeader(value = ApiHeaders.CORRELATION_ID, required = false) String correlationId,
            @RequestHeader(value = HEADER_UNIDAD_RECURSO) Long unidadRecursoId) {
        if (solicitud == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RECLASIFICACION_REQUERIDA");
        }
        DocumentoAuthorizedContext contexto = contextoResolver.resolver(
                actorSub, asignacionEfectivaId, PERFIL_EVALUADOR, unidadRecursoId, documentoId, correlationId);
        ReclasificarDocumentoCommand comando = new ReclasificarDocumentoCommand(
                solicitud.clasificacionNueva(),
                solicitud.documentoDecisionId(),
                solicitud.autoridadDecisoraId(),
                solicitud.motivo());
        ReclasificacionDocumentoResult resultado = service.reclasificar(
                contexto, documentoId, ifMatch, comando);
        return ResponseEntity.ok().eTag(resultado.etag()).body(resultado);
    }

    @Operation(
            summary = "Historial de reclasificación",
            description = "Devuelve el historial append-only de reclasificaciones "
                    + "registradas para el documento, en orden cronológico. "
                    + "No expone contenido documental ni datos personales "
                    + "innecesarios.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historial devuelto.",
                    content = @Content(schema = @Schema(implementation = ClasificacionHistDetalle.class))),
            @ApiResponse(responseCode = "403", description = "Asignación efectiva insuficiente."),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado.")
    })
    @GetMapping(value = "/{documentoId}/clasificacion/historial", produces = "application/json")
    public ResponseEntity<List<ClasificacionHistDetalle>> historial(
            @PathVariable Long documentoId,
            @RequestHeader(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID) Long asignacionEfectivaId,
            @RequestHeader(value = HEADER_ACTOR_SUB, required = false) String actorSub,
            @RequestHeader(value = ApiHeaders.CORRELATION_ID, required = false) String correlationId,
            @RequestHeader(value = HEADER_UNIDAD_RECURSO) Long unidadRecursoId) {
        DocumentoAuthorizedContext contexto = contextoResolver.resolver(
                actorSub, asignacionEfectivaId, PERFIL_EVALUADOR, unidadRecursoId, documentoId, correlationId);
        return ResponseEntity.ok(service.listarHistorial(contexto, documentoId));
    }
}
