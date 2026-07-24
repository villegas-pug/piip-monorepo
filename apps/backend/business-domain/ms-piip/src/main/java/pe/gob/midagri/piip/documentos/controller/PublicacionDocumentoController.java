package pe.gob.midagri.piip.documentos.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.PublicacionDocumentoDetail;
import pe.gob.midagri.piip.documentos.dto.PublicarDocumentoRequest;
import pe.gob.midagri.piip.documentos.service.DocumentoAuthorizedContextResolver;
import pe.gob.midagri.piip.documentos.service.PublicacionDocumentoService;

/**
 * Controlador REST de confirmación de publicación documental.
 * Reutiliza la autorización efectiva Oracle y exige
 * {@code Idempotency-Key} para soportar reintentos seguros. La
 * fecha de publicación la fija exclusivamente el servidor mediante
 * el DEFAULT SYSTIMESTAMP del DDL 004; el cliente nunca contribuye
 * con la fecha. La respuesta nunca expone el BLOB ni una URL
 * directa de descarga.
 */
@RestController
@RequestMapping("/api/v1/documentos")
@Tag(name = "Documentos - Publicación", description = "Confirmación idempotente de publicación de versiones con clasificación PUBLICO.")
public class PublicacionDocumentoController {

    private static final String PERFIL_EVALUADOR = "Evaluador";
    private static final String HEADER_ACTOR_SUB = "X-Actor-Sub";
    private static final String HEADER_UNIDAD_RECURSO = "X-Unidad-Recurso-Id";

    private final PublicacionDocumentoService service;
    private final DocumentoAuthorizedContextResolver contextoResolver;

    public PublicacionDocumentoController(PublicacionDocumentoService service,
            DocumentoAuthorizedContextResolver contextoResolver) {
        this.service = service;
        this.contextoResolver = contextoResolver;
    }

    @Operation(
            summary = "Confirmar publicación de una versión PUBLICO",
            description = "Confirma la publicación de una versión con "
                    + "clasificación PUBLICO validada. La fecha de "
                    + "publicación es exclusivamente del servidor y la "
                    + "operación es idempotente mediante Idempotency-Key.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Publicación confirmada con fecha del servidor.",
                    headers = {
                            @Header(name = "ETag", description = "Identificador de la versión."),
                            @Header(name = "X-Publicacion-Id", description = "Identificador append-only de la publicación.")
                    },
                    content = @Content(schema = @Schema(implementation = PublicacionDocumentoDetail.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud malformada."),
            @ApiResponse(responseCode = "403", description = "Asignación efectiva insuficiente."),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado."),
            @ApiResponse(responseCode = "409", description = "Publicación duplicada o clave de idempotencia con payload distinto."),
            @ApiResponse(responseCode = "422", description = "Clasificación validada no es PUBLICO o título público inválido.")
    })
    @PostMapping(value = "/publicaciones", produces = "application/json")
    public ResponseEntity<PublicacionDocumentoDetail> confirmar(
            @RequestBody @Valid PublicarDocumentoRequest solicitud,
            @RequestHeader(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID) Long asignacionEfectivaId,
            @RequestHeader(value = HEADER_ACTOR_SUB, required = false) String actorSub,
            @RequestHeader(value = ApiHeaders.CORRELATION_ID, required = false) String correlationId,
            @RequestHeader(value = HEADER_UNIDAD_RECURSO) Long unidadRecursoId,
            @RequestHeader(ApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey) {
        if (solicitud == null || solicitud.documentoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PUBLICACION_REQUERIDA");
        }
        DocumentoAuthorizedContext contexto = contextoResolver.resolver(
                actorSub, asignacionEfectivaId, PERFIL_EVALUADOR, unidadRecursoId,
                solicitud.documentoId(), correlationId);
        PublicacionDocumentoDetail detalle = service.confirmarPublicacion(
                contexto, idempotencyKey, solicitud);
        return ResponseEntity.status(HttpStatus.CREATED)
                .eTag("\"" + detalle.publicacionId() + "\"")
                .header("X-Publicacion-Id", String.valueOf(detalle.publicacionId()))
                .body(detalle);
    }
}
