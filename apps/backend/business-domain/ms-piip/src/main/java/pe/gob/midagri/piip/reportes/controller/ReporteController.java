package pe.gob.midagri.piip.reportes.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.Valid;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import pe.gob.midagri.piip.reportes.dto.DestinatarioReporteDetail;
import pe.gob.midagri.piip.reportes.dto.ExtraordinarioReportRequest;
import pe.gob.midagri.piip.reportes.dto.ReporteArchivoDescarga;
import pe.gob.midagri.piip.reportes.dto.ReporteAprobacionDetail;
import pe.gob.midagri.piip.reportes.dto.ReporteAprobacionRequest;
import pe.gob.midagri.piip.reportes.dto.ReporteAuthContext;
import pe.gob.midagri.piip.reportes.dto.ReporteDetail;
import pe.gob.midagri.piip.reportes.dto.ReporteRemisionPage;
import pe.gob.midagri.piip.reportes.dto.ReporteRemisionRequest;
import pe.gob.midagri.piip.reportes.dto.ReportOperation;
import pe.gob.midagri.piip.reportes.dto.SemestralReportRequest;
import pe.gob.midagri.piip.reportes.service.AprobacionRemisionReporteService;
import pe.gob.midagri.piip.reportes.service.GeneracionReporteService;

import java.util.List;

/**
 * Controlador REST de reportes institucionales (US8,
 * Constitución 5.0.0, contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/reportes.md}
 * y DDL 017). Delega toda la lógica a los servicios
 * de aplicación; nunca accede a repositorios ni
 * entidades. Las operaciones POST exigen
 * {@code Idempotency-Key}; las de generación
 * idempotente devuelven {@code 202} con la operación
 * registrada.
 *
 * <p>Alcance por tarea:
 * <ul>
 *   <li>T107 expone los endpoints de generación
 *       semestral, generación extraordinaria,
 *       consulta del detalle y descarga de PDF
 *       y XLSX a partir del mismo snapshot.</li>
 *   <li>T108 expone los endpoints de aprobación
 *       formal, listado de destinatarios
 *       aprobados y remisión manual
 *       recuperable. La remisión es registrada,
 *       no automática.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/reportes")
@Tag(name = "Reportes institucionales",
        description = "Generacion idempotente, aprobacion, remision manual y "
                + "descarga institucional de PDF y XLSX desde el snapshot (US8).")
public class ReporteController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";

    private final GeneracionReporteService generacionService;
    private final AprobacionRemisionReporteService aprobacionRemisionService;
    private final ObjectMapper objectMapper;

    public ReporteController(GeneracionReporteService generacionService,
            AprobacionRemisionReporteService aprobacionRemisionService,
            ObjectMapper objectMapper) {
        this.generacionService = generacionService;
        this.aprobacionRemisionService = aprobacionRemisionService;
        this.objectMapper = objectMapper;
    }

    // -----------------------------------------------------------------
    // 1) Generacion semestral
    // -----------------------------------------------------------------

    @Operation(
            summary = "Generar reporte institucional semestral",
            description = "Genera el reporte semestral oficial (BR-013, "
                    + "BR-121, BR-122) con cortes 30/06 y 31/12. "
                    + "Idempotencia por Idempotency-Key. Devuelve 202 "
                    + "con la operacion registrada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202",
                    description = "Reporte generado; pendiente de aprobacion.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReportOperation.class))),
            @ApiResponse(responseCode = "400",
                    description = "Solicitud malformada o falta Idempotency-Key.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "403",
                    description = "REPORT_SCOPE_DENIED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "409",
                    description = "Idempotency-Key reutilizada con payload distinto.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422",
                    description = "SEMESTER_INVALID o REPORT_CUTOFF_INVALID.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/semestrales/generaciones",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportOperation> generarSemestral(
            @Valid @RequestBody SemestralReportRequest request,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false)
                    String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader("X-Perfil-Efectivo") String perfilEfectivo,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false)
                    Long actorUsuarioId,
            @RequestHeader(value = "X-Unidad-Efectiva-Id", required = false)
                    Long unidadEfectivaId,
            @RequestHeader(value = "X-Correlation-Id", required = false)
                    String correlationId) {
        ReporteAuthContext contexto = construirContexto(asignacionId, perfilEfectivo,
                actorSub, actorUsuarioId, unidadEfectivaId, correlationId);
        ReportOperation operacion = generacionService.generarSemestral(request,
                contexto, idempotencyKey, serializar(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(operacion);
    }

    // -----------------------------------------------------------------
    // 2) Generacion extraordinaria
    // -----------------------------------------------------------------

    @Operation(
            summary = "Generar reporte institucional extraordinario",
            description = "Genera un reporte extraordinario (BR-120, "
                    + "BR-121, BR-122, BR-123) con filtros dentro del "
                    + "ambito del generador. La solicitud y la "
                    + "aprobacion documentadas son obligatorias."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "202",
                    description = "Reporte generado; pendiente de aprobacion.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReportOperation.class))),
            @ApiResponse(responseCode = "400",
                    description = "Solicitud malformada o falta Idempotency-Key.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "403",
                    description = "REPORT_SCOPE_DENIED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422",
                    description = "REPORT_REQUEST_APPROVAL_REQUIRED o REPORT_CUTOFF_INVALID.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/extraordinarios/generaciones",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReportOperation> generarExtraordinario(
            @Valid @RequestBody ExtraordinarioReportRequest request,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false)
                    String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader("X-Perfil-Efectivo") String perfilEfectivo,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false)
                    Long actorUsuarioId,
            @RequestHeader(value = "X-Unidad-Efectiva-Id", required = false)
                    Long unidadEfectivaId,
            @RequestHeader(value = "X-Correlation-Id", required = false)
                    String correlationId) {
        ReporteAuthContext contexto = construirContexto(asignacionId, perfilEfectivo,
                actorSub, actorUsuarioId, unidadEfectivaId, correlationId);
        ReportOperation operacion = generacionService.generarExtraordinario(
                request, contexto, idempotencyKey, serializar(request));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(operacion);
    }

    // -----------------------------------------------------------------
    // 3) Consulta del detalle
    // -----------------------------------------------------------------

    @Operation(
            summary = "Consultar reporte institucional",
            description = "Devuelve el detalle del reporte, sus "
                    + "indicadores BR-122, sus totales BR-121 y sus "
                    + "archivos PDF/XLSX."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Reporte encontrado.",
                    headers = @Header(name = "ETag",
                            description = "Identificador de version para concurrencia."),
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReporteDetail.class))),
            @ApiResponse(responseCode = "404",
                    description = "REPORT_NOT_FOUND.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @GetMapping(value = "/generaciones/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReporteDetail> consultar(
            @PathVariable("id") Long id,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader("X-Perfil-Efectivo") String perfilEfectivo,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false)
                    Long actorUsuarioId,
            @RequestHeader(value = "X-Unidad-Efectiva-Id", required = false)
                    Long unidadEfectivaId,
            @RequestHeader(value = "X-Correlation-Id", required = false)
                    String correlationId) {
        ReporteAuthContext contexto = construirContexto(asignacionId, perfilEfectivo,
                actorSub, actorUsuarioId, unidadEfectivaId, correlationId);
        ReporteDetail detalle = generacionService.consultar(id, contexto);
        return ResponseEntity.ok().eTag(detalle.etag()).body(detalle);
    }

    // -----------------------------------------------------------------
    // 4) Descarga de archivos institucionales
    // -----------------------------------------------------------------

    @GetMapping(value = "/generaciones/{id}/archivos/{formato}",
            produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<ByteArrayResource> descargarArchivo(
            @PathVariable("id") Long id,
            @PathVariable("formato") String formato,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader("X-Perfil-Efectivo") String perfilEfectivo,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false)
                    Long actorUsuarioId,
            @RequestHeader(value = "X-Unidad-Efectiva-Id", required = false)
                    Long unidadEfectivaId,
            @RequestHeader(value = "X-Correlation-Id", required = false)
                    String correlationId) {
        ReporteAuthContext contexto = construirContexto(asignacionId, perfilEfectivo,
                actorSub, actorUsuarioId, unidadEfectivaId, correlationId);
        ReporteArchivoDescarga descarga = generacionService.descargarArchivo(id,
                formato, contexto);
        ByteArrayResource recurso = new ByteArrayResource(descarga.contenido());
        HttpHeaders cabeceras = new HttpHeaders();
        cabeceras.setContentDisposition(
                org.springframework.http.ContentDisposition
                        .attachment()
                        .filename(descarga.nombreArchivo())
                        .build());
        cabeceras.add("ETag", descarga.etag());
        return ResponseEntity.ok()
                .headers(cabeceras)
                .contentLength(descarga.contenido().length)
                .contentType(MediaType.parseMediaType(descarga.contentType()))
                .body(recurso);
    }

    // -----------------------------------------------------------------
    // 5) Aprobacion y remision
    // -----------------------------------------------------------------

    @Operation(
            summary = "Aprobar version de reporte institucional",
            description = "La Oficina de Modernizacion aprueba la version "
                    + "exacta del reporte, fija el documento de "
                    + "aprobacion y los destinatarios permitidos (BR-125, "
                    + "BR-127). La aprobacion es idempotente por "
                    + "Idempotency-Key."
    )
    @PostMapping(value = "/generaciones/{id}/aprobaciones-remision",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReporteAprobacionDetail> aprobar(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReporteAprobacionRequest request,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false)
                    String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader("X-Perfil-Efectivo") String perfilEfectivo,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false)
                    Long actorUsuarioId,
            @RequestHeader(value = "X-Unidad-Efectiva-Id", required = false)
                    Long unidadEfectivaId,
            @RequestHeader(value = "X-Correlation-Id", required = false)
                    String correlationId) {
        ReporteAuthContext contexto = construirContexto(asignacionId, perfilEfectivo,
                actorSub, actorUsuarioId, unidadEfectivaId, correlationId);
        ReporteAprobacionDetail detalle = aprobacionRemisionService
                .aprobar(id, request, contexto, idempotencyKey, serializar(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(detalle);
    }

    @Operation(
            summary = "Listar destinatarios aprobados",
            description = "Devuelve los destinatarios aprobados en la "
                    + "ultima version aprobada del reporte (BR-125)."
    )
    @GetMapping(value = "/generaciones/{id}/destinatarios",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<DestinatarioReporteDetail>> listarDestinatarios(
            @PathVariable("id") Long id,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader("X-Perfil-Efectivo") String perfilEfectivo,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false)
                    Long actorUsuarioId,
            @RequestHeader(value = "X-Unidad-Efectiva-Id", required = false)
                    Long unidadEfectivaId,
            @RequestHeader(value = "X-Correlation-Id", required = false)
                    String correlationId) {
        ReporteAuthContext contexto = construirContexto(asignacionId, perfilEfectivo,
                actorSub, actorUsuarioId, unidadEfectivaId, correlationId);
        return ResponseEntity.ok(aprobacionRemisionService
                .listarDestinatarios(id, contexto));
    }

    @Operation(
            summary = "Registrar remision manual recuperable",
            description = "Registra la remision manual contra los "
                    + "destinatarios aprobados (BR-125, BR-128). El "
                    + "resultado es declarativo; FALLIDA exige motivo."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201",
                    description = "Remision registrada.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = ReporteRemisionPage.class))),
            @ApiResponse(responseCode = "409",
                    description = "REPORT_VERSION_NOT_APPROVED o "
                            + "REPORT_REMITTAL_DUPLICATED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE)),
            @ApiResponse(responseCode = "422",
                    description = "RECIPIENT_NOT_APPROVED o "
                            + "REMITTAL_MOTIVE_REQUIRED.",
                    content = @Content(mediaType = MediaType.APPLICATION_PROBLEM_JSON_VALUE))
    })
    @PostMapping(value = "/generaciones/{id}/remisiones",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReporteRemisionPage> remitir(
            @PathVariable("id") Long id,
            @Valid @RequestBody ReporteRemisionRequest request,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false)
                    String idempotencyKey,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader("X-Perfil-Efectivo") String perfilEfectivo,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false)
                    Long actorUsuarioId,
            @RequestHeader(value = "X-Unidad-Efectiva-Id", required = false)
                    Long unidadEfectivaId,
            @RequestHeader(value = "X-Correlation-Id", required = false)
                    String correlationId) {
        ReporteAuthContext contexto = construirContexto(asignacionId, perfilEfectivo,
                actorSub, actorUsuarioId, unidadEfectivaId, correlationId);
        ReporteRemisionPage detalle = aprobacionRemisionService
                .remitir(id, request, contexto, idempotencyKey, serializar(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(detalle);
    }

    @Operation(
            summary = "Consultar historial de remisiones",
            description = "Devuelve las remisiones registradas de un "
                    + "reporte. Permite reconstruir la trazabilidad "
                    + "sin perder evidencia."
    )
    @GetMapping(value = "/generaciones/{id}/remisiones",
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ReporteRemisionPage> consultarRemisiones(
            @PathVariable("id") Long id,
            @RequestParam(value = "idVersion", required = false) Integer idVersion,
            @RequestHeader("X-Asignacion-Efectiva-Id") Long asignacionId,
            @RequestHeader("X-Perfil-Efectivo") String perfilEfectivo,
            @RequestHeader(value = "X-Actor-Sub", required = false) String actorSub,
            @RequestHeader(value = "X-Actor-Usuario-Id", required = false)
                    Long actorUsuarioId,
            @RequestHeader(value = "X-Unidad-Efectiva-Id", required = false)
                    Long unidadEfectivaId,
            @RequestHeader(value = "X-Correlation-Id", required = false)
                    String correlationId) {
        ReporteAuthContext contexto = construirContexto(asignacionId, perfilEfectivo,
                actorSub, actorUsuarioId, unidadEfectivaId, correlationId);
        return ResponseEntity.ok(aprobacionRemisionService
                .consultarRemisiones(id, idVersion, contexto));
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private static ReporteAuthContext construirContexto(Long asignacionId,
            String perfilEfectivo, String actorSub, Long actorUsuarioId,
            Long unidadEfectivaId, String correlationId) {
        if (asignacionId == null || perfilEfectivo == null
                || perfilEfectivo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "X-Asignacion-Efectiva-Id y X-Perfil-Efectivo son obligatorios");
        }
        return new ReporteAuthContext(actorSub, actorUsuarioId, asignacionId,
                perfilEfectivo, unidadEfectivaId, correlationId);
    }

    private String serializar(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "REQUEST_NOT_READABLE: no se pudo serializar la solicitud");
        }
    }

}
