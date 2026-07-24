package pe.gob.midagri.piip.consulta.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import pe.gob.midagri.piip.config.ApiHeaders;
import pe.gob.midagri.piip.config.CorrelationIdFilter;
import pe.gob.midagri.piip.consulta.dto.ConsultaInstitucionalAuthContext;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioDetail;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioPage;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioQuery;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioSummary;
import pe.gob.midagri.piip.consulta.dto.TipoRegistroConsulta;
import pe.gob.midagri.piip.consulta.service.ConsultaInstitucionalService;

/**
 * Controlador REST de la consulta institucional por ámbito y
 * clasificación. Su única responsabilidad es validar las
 * cabeceras canónicas, delegar en el servicio y proyectar la
 * respuesta HTTP. No accede a repositorios ni a entidades JPA.
 *
 * <p>El controlador no expone contenido/descarga documental:
 * entrega únicamente los metadatos documentales que el servicio
 * de {@code documentos} expone para la consulta autorizada. El
 * contenido se obtiene por el endpoint
 * {@code /api/v1/documentos/{id}/contenido} con la
 * revalidación obligatoria de ámbito y clasificación.
 */
@RestController
@RequestMapping("/api/v1/consulta/institucional")
@Tag(name = "Consulta - Institucional", description = "Consulta institucional autorizada por ámbito y clasificación.")
public class ConsultaInstitucionalController {

    private static final String PARAM_TIPO = "tipo";
    private static final String PARAM_ESTADO = "estado";
    private static final String PARAM_CODIGO = "codigo";
    private static final String PARAM_NOMBRE = "nombre";
    private static final String PARAM_UNIDAD_ID = "unidadId";
    private static final String PARAM_RESPONSABLE_ID = "responsableId";
    private static final String PARAM_FECHA_DESDE = "fechaDesde";
    private static final String PARAM_FECHA_HASTA = "fechaHasta";
    private static final String PARAM_PAGE = "page";
    private static final String PARAM_SIZE = "size";
    private static final String PARAM_SORT = "sort";

    private final ConsultaInstitucionalService service;

    public ConsultaInstitucionalController(ConsultaInstitucionalService service) {
        this.service = service;
    }

    @Operation(
            summary = "Buscar portafolio institucional",
            description = "Busca registros del portafolio aplicando los filtros "
                    + "canónicos y la paginación dentro del ámbito autorizado. "
                    + "La respuesta nunca incluye entidades JPA ni contenido "
                    + "documental. La cabecera If-None-Match permite la "
                    + "concurrencia optimista con la ETag agregada de la página.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de resultados del portafolio.",
                    headers = @Header(name = "ETag", description = "Identificador de versión de la página."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InstitutionalPortfolioPage.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud malformada.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Asignación efectiva ausente o autenticación requerida.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "ASSIGNMENT_SCOPE_DENIED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422", description = "Filtros inválidos.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @GetMapping(value = "/portafolio", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstitutionalPortfolioPage> buscar(
            @Parameter(description = "Tipo de registro (INICIATIVA, PROYECTO).")
            @RequestParam(value = PARAM_TIPO, required = false) TipoRegistroConsulta tipo,
            @RequestParam(value = PARAM_CODIGO, required = false) String codigo,
            @RequestParam(value = PARAM_NOMBRE, required = false) String nombre,
            @RequestParam(value = PARAM_ESTADO, required = false) String estado,
            @RequestParam(value = PARAM_UNIDAD_ID, required = false) Long unidadId,
            @RequestParam(value = PARAM_RESPONSABLE_ID, required = false) Long responsableId,
            @RequestParam(value = PARAM_FECHA_DESDE, required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate fechaDesde,
            @RequestParam(value = PARAM_FECHA_HASTA, required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate fechaHasta,
            @RequestParam(value = PARAM_PAGE, defaultValue = "0") int page,
            @RequestParam(value = PARAM_SIZE, defaultValue = "20") int size,
            @RequestParam(value = PARAM_SORT, required = false) String sort,
            @RequestHeader(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID) Long asignacionEfectivaId,
            @RequestHeader(value = ApiHeaders.CORRELATION_ID, required = false) String correlationId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Perfil-Efectivo", required = false) String perfilEfectivo,
            HttpServletRequest request,
            Principal principal) {
        ConsultaInstitucionalAuthContext contexto = construirContexto(asignacionEfectivaId,
                actorSub, actorUsuarioId, perfilEfectivo, correlationId(request, correlationId),
                principal);
        InstitutionalPortfolioQuery consulta = new InstitutionalPortfolioQuery(tipo, codigo, nombre,
                estado, unidadId, responsableId, fechaDesde, fechaHasta, sort, page, size);
        ConsultaInstitucionalService.ResultadoConsulta resultado = service.buscar(consulta, contexto);
        InstitutionalPortfolioPage pageResult = resultado.page();
        String correlation = correlationId(request, correlationId);
        return ResponseEntity.ok()
                .eTag(pageResult.etag())
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(ApiHeaders.CORRELATION_ID, correlation)
                .body(pageResult);
    }

    @Operation(
            summary = "Ver detalle institucional",
            description = "Devuelve el detalle institucional de un registro del "
                    + "portafolio. La respuesta nunca incluye BLOB, clave física "
                    + "ni URL directa; el contenido se obtiene exclusivamente por "
                    + "el endpoint /api/v1/documentos/{id}/contenido. Si el "
                    + "registro no pertenece al ámbito del actor, se responde 404 "
                    + "sin confirmar su existencia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle institucional del registro.",
                    headers = @Header(name = "ETag", description = "Identificador de versión del registro."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = InstitutionalPortfolioDetail.class))),
            @ApiResponse(responseCode = "304", description = "ETag actual; el recurso no cambió."),
            @ApiResponse(responseCode = "400", description = "Solicitud malformada.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "401", description = "Asignación efectiva ausente o autenticación requerida.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "403", description = "ASSIGNMENT_SCOPE_DENIED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "404", description = "Recurso fuera de ámbito o no visible.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @GetMapping(value = "/portafolio/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<InstitutionalPortfolioDetail> detalle(
            @PathVariable("id") Long id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestHeader(value = "If-Match", required = false) String ifMatch,
            @RequestHeader(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID) Long asignacionEfectivaId,
            @RequestHeader(value = ApiHeaders.CORRELATION_ID, required = false) String correlationId,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false) Long actorUsuarioId,
            @RequestHeader(value = "X-Perfil-Efectivo", required = false) String perfilEfectivo,
            HttpServletRequest request,
            Principal principal) {
        ConsultaInstitucionalAuthContext contexto = construirContexto(asignacionEfectivaId,
                actorSub, actorUsuarioId, perfilEfectivo, correlationId(request, correlationId),
                principal);
        Optional<ConsultaInstitucionalService.DetalleConsulta> detalle = service.obtenerDetalle(id,
                contexto);
        if (detalle.isEmpty() || !detalle.get().visible()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header(ApiHeaders.CORRELATION_ID, correlationId(request, correlationId))
                    .build();
        }
        InstitutionalPortfolioDetail valor = detalle.get().detalle();
        String etag = valor.etag();
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(etag)
                    .header(ApiHeaders.CORRELATION_ID, correlationId(request, correlationId))
                    .build();
        }
        if (ifMatch != null && !ifMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
                    .eTag(etag)
                    .header(ApiHeaders.CORRELATION_ID, correlationId(request, correlationId))
                    .build();
        }
        return ResponseEntity.ok()
                .eTag(etag)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(ApiHeaders.CORRELATION_ID, correlationId(request, correlationId))
                .body(valor);
    }

    private static ConsultaInstitucionalAuthContext construirContexto(Long asignacionEfectivaId,
            String actorSub, Long actorUsuarioId, String perfilEfectivo, String correlationId,
            Principal principal) {
        String sub = principal == null ? actorSub : principal.getName();
        return ConsultaInstitucionalAuthContext.desde(principal == null ? null : principal,
                asignacionEfectivaId, actorUsuarioId, perfilEfectivo, correlationId, List.of());
    }

    private static String correlationId(HttpServletRequest request, String supplied) {
        if (supplied != null && !supplied.isBlank()) {
            return supplied;
        }
        return CorrelationIdFilter.getCorrelationId(request);
    }
}
