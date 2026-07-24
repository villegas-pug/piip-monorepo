package pe.gob.midagri.piip.documentos.controller;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import pe.gob.midagri.piip.config.ApiHeaders;
import pe.gob.midagri.piip.documentos.dto.AptitudDocumental;
import pe.gob.midagri.piip.documentos.dto.ContenidoDocumentoResponse;
import pe.gob.midagri.piip.documentos.dto.DocumentVersionDetail;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.UploadDocumentCommand;
import pe.gob.midagri.piip.documentos.dto.UploadDocumentRequest;
import pe.gob.midagri.piip.documentos.service.DocumentoAuthorizedContextResolver;
import pe.gob.midagri.piip.documentos.service.DocumentoService;

/**
 * Controlador REST del modulo documental.
 *
 * <p>Implementa la carga inicial y el versionado trazable de documentos
 * formales sobre expedientes institucionales. El backend calcula el SHA-256,
 * formaliza la version y la persiste como BLOB Oracle; PIIP no modela
 * estados, contratos, gates ni informes antimalware, cuya administracion
 * corresponde a OGTI fuera de la aplicacion.
 *
 * <p>Los errores se devuelven como {@code application/problem+json} a
 * traves del {@code DocumentosExceptionHandler}, unico advice del modulo.
 * La autorizacion efectiva se resuelve en Oracle mediante
 * {@link DocumentoAuthorizedContextResolver} y la pertenencia de la serie
 * a su expediente es inmutable.
 */
@RestController
@RequestMapping("/api/v1/documentos")
@Tag(name = "Documentos - Carga", description = "Carga y versionado de documentos formales sobre expedientes institucionales.")
public class DocumentoController {

    private static final long MAX_BYTES = 104_857_600L;
    private static final String PERFIL_RESPONSABLE = "Responsable";
    private static final String HEADER_ACTOR_SUB = "X-Actor-Sub";
    private static final String HEADER_UNIDAD_RECURSO = "X-Unidad-Recurso-Id";

    private final DocumentoService documentoService;
    private final DocumentoAuthorizedContextResolver contextoResolver;

    public DocumentoController(DocumentoService documentoService,
            DocumentoAuthorizedContextResolver contextoResolver) {
        this.documentoService = documentoService;
        this.contextoResolver = contextoResolver;
    }

    /**
     * GET /api/v1/documentos/tipos/{tipoDocumentoId}/aptitud
     * Catalogo de tipos documentales: nombre, contexto, obligatoriedad y estado.
     */
    @GetMapping("/tipos/{tipoDocumentoId}/aptitud")
    public AptitudDocumental obtenerAptitud(@PathVariable Integer tipoDocumentoId) {
        return documentoService.obtenerAptitud(tipoDocumentoId);
    }

    /**
     * POST /api/v1/documentos
     * Carga inicial de un documento sobre un expediente institucional.
     * Multipart con parte {@code file} (binario) y parte {@code metadata}
     * ({@link UploadDocumentRequest}). Exige {@code Idempotency-Key}.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentVersionDetail> cargar(
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") @Valid UploadDocumentRequest metadata,
            @RequestHeader(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID) Long asignacionEfectivaId,
            @RequestHeader(value = HEADER_ACTOR_SUB, required = false) String actorSub,
            @RequestHeader(value = ApiHeaders.CORRELATION_ID, required = false) String correlationId,
            @RequestHeader(value = HEADER_UNIDAD_RECURSO) Long unidadRecursoId,
            @RequestHeader(ApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey) {

        if (!metadata.owner().esExpedienteInstitucional()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "DOCUMENT_OWNER_XOR_VIOLATION");
        }
        validarBinario(file);
        Long expedienteId = metadata.owner().expedienteInstitucionalId();
        Long unidadDelExpediente = documentoService.obtenerUnidadExpediente(expedienteId);
        if (unidadDelExpediente == null || !unidadDelExpediente.equals(unidadRecursoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "INSTITUTIONAL_FILE_SCOPE_DENIED");
        }
        DocumentoAuthorizedContext contexto = contextoResolver.resolver(
                actorSub, asignacionEfectivaId, PERFIL_RESPONSABLE,
                unidadRecursoId, expedienteId, correlationId);
        UploadDocumentCommand comando = new UploadDocumentCommand(
                metadata.tipoDocumentoId(),
                metadata.titulo(),
                file.getOriginalFilename(),
                file.getContentType(),
                metadata.clasificacionPropuesta(),
                leerBytes(file));
        DocumentVersionDetail detalle = documentoService.cargarEnExpediente(
                contexto, idempotencyKey, comando);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(detalle.etag()).body(detalle);
    }

    /**
     * POST /api/v1/documentos/{serieId}/versiones
     * Crea una nueva version trazable sobre la serie. Exige {@code If-Match}
     * con el ETag de la ultima version y {@code Idempotency-Key}.
     */
    @PostMapping(value = "/{serieId}/versiones", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentVersionDetail> crearVersion(
            @PathVariable Long serieId,
            @RequestPart("file") MultipartFile file,
            @RequestPart("metadata") @Valid UploadDocumentRequest metadata,
            @RequestHeader(value = ApiHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestHeader(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID) Long asignacionEfectivaId,
            @RequestHeader(value = HEADER_ACTOR_SUB, required = false) String actorSub,
            @RequestHeader(value = ApiHeaders.CORRELATION_ID, required = false) String correlationId,
            @RequestHeader(value = HEADER_UNIDAD_RECURSO) Long unidadRecursoId,
            @RequestHeader(ApiHeaders.IDEMPOTENCY_KEY) String idempotencyKey) {

        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
                    "DOCUMENT_VERSION_ETAG_REQUIRED");
        }
        validarBinario(file);
        Long expedienteId = documentoService.obtenerExpedienteDeSerie(serieId);
        if (expedienteId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "DOCUMENT_OWNER_IMMUTABLE");
        }
        Long unidadDelExpediente = documentoService.obtenerUnidadExpediente(expedienteId);
        if (unidadDelExpediente == null || !unidadDelExpediente.equals(unidadRecursoId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "INSTITUTIONAL_FILE_SCOPE_DENIED");
        }
        DocumentoAuthorizedContext contexto = contextoResolver.resolver(
                actorSub, asignacionEfectivaId, PERFIL_RESPONSABLE,
                unidadRecursoId, expedienteId, correlationId);
        UploadDocumentCommand comando = new UploadDocumentCommand(
                metadata.tipoDocumentoId(),
                metadata.titulo(),
                file.getOriginalFilename(),
                file.getContentType(),
                metadata.clasificacionPropuesta(),
                leerBytes(file));
        DocumentVersionDetail detalle = documentoService.crearVersion(
                contexto, serieId, idempotencyKey, ifMatch, comando);
        return ResponseEntity.status(HttpStatus.CREATED).eTag(detalle.etag()).body(detalle);
    }

    @Operation(
            summary = "Descargar contenido institucional",
            description = "Devuelve el BLOB y los metadatos del documento "
                    + "para un usuario institucional autorizado. La operación "
                    + "revalida la asignación efectiva, el ámbito y la "
                    + "clasificación validada antes de exponer el contenido; "
                    + "nunca se entrega a la consulta pública ni a usuarios "
                    + "sin asignación efectiva.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Contenido del documento.",
                    headers = {
                            @Header(name = "ETag", description = "Versión optimista de la fila."),
                            @Header(name = "X-Clasificacion-Validada",
                                    description = "Clasificación validada al momento de servir.")
                    }),
            @ApiResponse(responseCode = "400", description = "Solicitud malformada."),
            @ApiResponse(responseCode = "403", description = "Asignación efectiva insuficiente o clasificación pendiente."),
            @ApiResponse(responseCode = "404", description = "Documento no encontrado en el ámbito.")
    })
    @GetMapping(value = "/{documentoId}/contenido")
    public ResponseEntity<byte[]> contenido(
            @PathVariable Long documentoId,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestHeader(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID) Long asignacionEfectivaId,
            @RequestHeader(value = HEADER_ACTOR_SUB, required = false) String actorSub,
            @RequestHeader(value = ApiHeaders.CORRELATION_ID, required = false) String correlationId,
            @RequestHeader(value = HEADER_UNIDAD_RECURSO) Long unidadRecursoId) {
        // La autorización efectiva exige perfil Consulta o superior dentro del
        // ámbito; la pertenencia del documento al ámbito se valida mediante la
        // cabecera de unidad de recurso, que debe coincidir con la unidad de la
        // asignación efectiva revalidada por Oracle.
        DocumentoAuthorizedContext contexto = contextoResolver.resolver(
                actorSub, asignacionEfectivaId, "Consulta",
                unidadRecursoId, documentoId, correlationId);
        DocumentoService.ContenidoInstitucional contenido =
                documentoService.obtenerContenidoInstitucional(contexto, documentoId);
        ContenidoDocumentoResponse meta = contenido.metadatos();
        byte[] binario = contenido.binario();
        String etag = meta.etag();
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .header("X-Clasificacion-Validada", meta.clasificacionValidada().name())
                    .build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .header(HttpHeaders.CONTENT_TYPE,
                        meta.mimeType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : meta.mimeType())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + (meta.nombreOriginal() == null ? "documento" : meta.nombreOriginal()) + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header("X-Clasificacion-Validada", meta.clasificacionValidada().name())
                .body(binario);
    }

    private void validarBinario(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "DOCUMENT_CONTENT_REQUIRED");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "DOCUMENT_TOO_LARGE");
        }
    }

    private byte[] leerBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No fue posible leer el archivo enviado.", e);
        }
    }
}
