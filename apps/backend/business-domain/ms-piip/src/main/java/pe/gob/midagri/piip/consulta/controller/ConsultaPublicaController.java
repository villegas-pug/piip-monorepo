package pe.gob.midagri.piip.consulta.controller;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import pe.gob.midagri.piip.config.ApiHeaders;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioDetail;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioPage;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioQuery;
import pe.gob.midagri.piip.consulta.dto.TipoRegistroConsulta;
import pe.gob.midagri.piip.consulta.service.ConsultaPublicaService;

/**
 * Controlador REST de la consulta pública anónima del portafolio.
 * No requiere autenticación ni asignación efectiva; la respuesta
 * aplica la minimización constitucional y nunca expone contenido
 * ni habilita descarga documental. La constitución prohíbe
 * absolutamente el endpoint de contenido público durante la Fase 1.
 */
@RestController
@RequestMapping("/api/v1/consulta/publica")
@Tag(name = "Consulta - Pública", description = "Consulta anónima minimizada sin descarga ni contenido.")
public class ConsultaPublicaController {

    private static final String PARAM_TIPO = "tipo";
    private static final String PARAM_CODIGO = "codigo";
    private static final String PARAM_NOMBRE = "nombre";
    private static final String PARAM_PAGE = "page";
    private static final String PARAM_SIZE = "size";

    private final ConsultaPublicaService service;

    public ConsultaPublicaController(ConsultaPublicaService service) {
        this.service = service;
    }

    @Operation(
            summary = "Buscar portafolio público",
            description = "Busca registros del portafolio aplicando filtros "
                    + "mínimos permitidos. La respuesta es anónima y solo "
                    + "incluye los cuatro campos públicos y los metadatos "
                    + "de las publicaciones elegibles. No expone "
                    + "responsable, notas, ni contenido.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página pública de resultados.",
                    headers = @Header(name = "ETag", description = "Versión de la página."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PublicPortfolioPage.class))),
            @ApiResponse(responseCode = "400", description = "Solicitud malformada.")
    })
    @GetMapping(value = "/portafolio", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PublicPortfolioPage> buscar(
            @RequestParam(value = PARAM_TIPO, required = false) TipoRegistroConsulta tipo,
            @RequestParam(value = PARAM_CODIGO, required = false) String codigo,
            @RequestParam(value = PARAM_NOMBRE, required = false) String nombre,
            @RequestParam(value = PARAM_PAGE, defaultValue = "0") int page,
            @RequestParam(value = PARAM_SIZE, defaultValue = "20") int size,
            @RequestHeader(value = ApiHeaders.CORRELATION_ID, required = false) String correlationId) {
        PublicPortfolioQuery consulta = new PublicPortfolioQuery(tipo, codigo, nombre, page, size);
        PublicPortfolioPage resultado = service.buscar(consulta);
        return ResponseEntity.ok()
                .eTag(resultado.etag())
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                .header(ApiHeaders.CORRELATION_ID, correlationId == null ? "" : correlationId)
                .body(resultado);
    }

    @Operation(
            summary = "Ver detalle público del registro",
            description = "Devuelve el detalle público de un registro del "
                    + "portafolio. La respuesta nunca expone contenido ni "
                    + "una URL de descarga. Si el registro no es elegible "
                    + "para consulta pública, se responde 404 sin confirmar "
                    + "su existencia.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Detalle público del registro.",
                    headers = @Header(name = "ETag", description = "Versión del detalle."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = PublicPortfolioDetail.class))),
            @ApiResponse(responseCode = "304", description = "El detalle no cambió."),
            @ApiResponse(responseCode = "404", description = "Registro no elegible o no visible.")
    })
    @GetMapping(value = "/portafolio/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PublicPortfolioDetail> detalle(
            @PathVariable("id") Long id,
            @RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch,
            @RequestHeader(value = ApiHeaders.CORRELATION_ID, required = false) String correlationId) {
        return service.obtenerDetalle(id)
                .map(detalle -> {
                    String etag = detalle.etag();
                    if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
                        return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                                .eTag(etag)
                                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                                .header(ApiHeaders.CORRELATION_ID,
                                        correlationId == null ? "" : correlationId)
                                .<PublicPortfolioDetail>build();
                    }
                    return ResponseEntity.ok()
                            .eTag(etag)
                            .header(HttpHeaders.CACHE_CONTROL, "public, max-age=60")
                            .header(ApiHeaders.CORRELATION_ID,
                                    correlationId == null ? "" : correlationId)
                            .<PublicPortfolioDetail>body(detalle);
                })
                .orElseGet(() -> ResponseEntity.<PublicPortfolioDetail>status(HttpStatus.NOT_FOUND)
                        .header(ApiHeaders.CORRELATION_ID,
                                correlationId == null ? "" : correlationId)
                        .build());
    }
}
