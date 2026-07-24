package pe.gob.midagri.piip.reportes.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.reportes.dto.ExtraordinarioReportRequest;
import pe.gob.midagri.piip.reportes.dto.ReporteArchivoDescarga;
import pe.gob.midagri.piip.reportes.dto.IndicadorReporte;
import pe.gob.midagri.piip.reportes.dto.ReporteArchivoSummary;
import pe.gob.midagri.piip.reportes.dto.ReporteAuthContext;
import pe.gob.midagri.piip.reportes.dto.ReporteDetail;
import pe.gob.midagri.piip.reportes.dto.ReportFiltros;
import pe.gob.midagri.piip.reportes.dto.ReportOperation;
import pe.gob.midagri.piip.reportes.dto.SemestralReportRequest;
import pe.gob.midagri.piip.reportes.dto.TotalDimension;
import pe.gob.midagri.piip.reportes.dto.TotalDimensionItem;
import pe.gob.midagri.piip.reportes.entity.ClasificacionReporte;
import pe.gob.midagri.piip.reportes.entity.EstadoTecnicoReporte;
import pe.gob.midagri.piip.reportes.entity.ReporteArchivoEntity;
import pe.gob.midagri.piip.reportes.entity.ReporteInstitucionalEntity;
import pe.gob.midagri.piip.reportes.entity.ReporteSnapshotEntity;
import pe.gob.midagri.piip.reportes.entity.TipoReporte;
import pe.gob.midagri.piip.reportes.exception.ReportesValidationException;
import pe.gob.midagri.piip.reportes.repository.ReporteArchivoRepository;
import pe.gob.midagri.piip.reportes.repository.ReporteInstitucionalRepository;
import pe.gob.midagri.piip.reportes.repository.ReporteSnapshotRepository;
import pe.gob.midagri.piip.reportes.service.GeneracionReporteService;
import pe.gob.midagri.piip.reportes.service.ReporteDatosReader;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/**
 * Servicio de generación de reportes institucionales
 * (US8, BR-013, BR-120, BR-121, BR-122, BR-123,
 * BR-126, BR-128, BR-148).
 *
 * <p>Reglas implementadas:
 * <ul>
 *   <li>Cortes semestrales deterministas: 30/06 y
 *       31/12. Una fecha de corte alternativa
 *       produce 422
 *       {@code REPORT_CUTOFF_INVALID}.</li>
 *   <li>Reporte extraordinario exige solicitud y
 *       aprobación documentada; sin ellas
 *       devuelve 422
 *       {@code REPORT_REQUEST_APPROVAL_REQUIRED}.</li>
 *   <li>El snapshot es un CLOB JSON canónico
 *       (claves ordenadas, números y fechas
 *       normalizados) cuyo hash SHA-256 es
 *       idempotente. El CHECK
 *       {@code UK_RS_HASH} impide duplicados.</li>
 *   <li>Indicadores BR-122 con numerador y
 *       denominador; denominador cero produce
 *       {@code no aplicable} y nunca un valor
 *       inferido.</li>
 *   <li>PDF y XLSX se generan a partir del mismo
 *       snapshot; la UK
 *       {@code UK_RA_REPORTE_FORMATO_VERSION}
 *       asegura una sola versión por formato y
 *       reporte.</li>
 *   <li>La generación es idempotente
 *       (misma clave y mismo payload devuelven el
 *       mismo reporte) y la autorización efectiva
 *       contra Oracle se revalida antes del
 *       INSERT; las denegaciones se auditan en
 *       {@code REQUIRES_NEW}.</li>
 *   <li>No hay remisión automática ni purga; la
 *       versión generada se conserva aunque el
 *       cliente omita la aprobación.</li>
 * </ul>
 */
@Service
public class GeneracionReporteServiceImpl implements GeneracionReporteService {

    private static final Logger LOG =
            LoggerFactory.getLogger(GeneracionReporteServiceImpl.class);

    private static final String CONSUMIDOR = "REPORTES";
    private static final String RECURSO_REPORTE = "REPORTE_INSTITUCIONAL";
    private static final String RECURSO_SNAPSHOT = "REPORTE_SNAPSHOT";
    private static final String RECURSO_ARCHIVO = "REPORTE_ARCHIVO";

    private static final String OP_GENERAR_SEMESTRAL = "GENERAR_REPORTE_SEMESTRAL";
    private static final String OP_GENERAR_EXTRAORDINARIO = "GENERAR_REPORTE_EXTRAORDINARIO";
    private static final String OP_CONSULTAR = "CONSULTAR_REPORTE";

    private static final String PERFIL_EVALUADOR = "Evaluador";
    private static final int VERSION_ESQUEMA_SNAPSHOT = 1;

    private final ReporteInstitucionalRepository reporteRepository;
    private final ReporteSnapshotRepository snapshotRepository;
    private final ReporteArchivoRepository archivoRepository;
    private final ReporteDatosReader datosReader;
    private final PdfReportRenderer pdfRenderer;
    private final XlsxReportRenderer xlsxRenderer;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper;

    private AutorizacionEfectivaService autorizacionService;

    @Autowired
    public GeneracionReporteServiceImpl(
            ReporteInstitucionalRepository reporteRepository,
            ReporteSnapshotRepository snapshotRepository,
            ReporteArchivoRepository archivoRepository,
            ReporteDatosReader datosReader,
            PdfReportRenderer pdfRenderer,
            XlsxReportRenderer xlsxRenderer,
            AuditService auditService,
            IdempotencyService idempotencyService,
            ObjectMapper objectMapper) {
        this.reporteRepository = reporteRepository;
        this.snapshotRepository = snapshotRepository;
        this.archivoRepository = archivoRepository;
        this.datosReader = datosReader;
        this.pdfRenderer = pdfRenderer;
        this.xlsxRenderer = xlsxRenderer;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
        this.objectMapper = objectMapper;
    }

    @Autowired(required = false)
    public void setAutorizacionService(
            AutorizacionEfectivaService autorizacionService) {
        this.autorizacionService = autorizacionService;
    }

    // -----------------------------------------------------------------
    // 1) Reporte semestral
    // -----------------------------------------------------------------

    @Override
    public ReportOperation generarSemestral(SemestralReportRequest request,
            ReporteAuthContext contexto, String idempotencyKey,
            String payloadJson) {
        if (request == null) {
            throw ReportesValidationException.cuerpoObligatorio();
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ejecutarGenerarSemestral(request, contexto);
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "IDEMPOTENCY_PAYLOAD_REQUIRED: la generacion exige el cuerpo serializado.");
        }
        IdempotencyService.IdempotencyRequest peticion =
                new IdempotencyService.IdempotencyRequest(
                        CONSUMIDOR, OP_GENERAR_SEMESTRAL,
                        idempotencyKey, payloadJson, contextoActorSub(contexto));
        IdempotencyService.IdempotencyResult resultado =
                idempotencyService.execute(peticion, () -> {
                    ReportOperation op = ejecutarGenerarSemestral(request, contexto);
                    return new IdempotencyService.IdempotencyResponse(
                            RECURSO_REPORTE, op.reporteId(),
                            serializarOperacion(op));
                });
        return deserializarOperacion(resultado.respuestaJson());
    }

    @Transactional
    ReportOperation ejecutarGenerarSemestral(SemestralReportRequest request,
            ReporteAuthContext contexto) {
        autorizarEvaluador(contexto);
        LocalDate fechaCorte = corteSemestral(request.anio(), request.semestre());
        String periodo = periodoSemestral(request.anio(), request.semestre());
        ReportFiltros filtros = filtrosVacio();
        return generar(TipoReporte.SEMESTRAL, request.anio(),
                request.semestre(), periodo, fechaCorte, filtros,
                contexto, null, null);
    }

    // -----------------------------------------------------------------
    // 2) Reporte extraordinario
    // -----------------------------------------------------------------

    @Override
    public ReportOperation generarExtraordinario(ExtraordinarioReportRequest request,
            ReporteAuthContext contexto, String idempotencyKey,
            String payloadJson) {
        if (request == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "VALIDATION_FAILED: el cuerpo es obligatorio.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ejecutarGenerarExtraordinario(request, contexto);
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "IDEMPOTENCY_PAYLOAD_REQUIRED: la generacion exige el cuerpo serializado.");
        }
        IdempotencyService.IdempotencyRequest peticion =
                new IdempotencyService.IdempotencyRequest(
                        CONSUMIDOR, OP_GENERAR_EXTRAORDINARIO,
                        idempotencyKey, payloadJson, contextoActorSub(contexto));
        IdempotencyService.IdempotencyResult resultado =
                idempotencyService.execute(peticion, () -> {
                    ReportOperation op = ejecutarGenerarExtraordinario(request, contexto);
                    return new IdempotencyService.IdempotencyResponse(
                            RECURSO_REPORTE, op.reporteId(),
                            serializarOperacion(op));
                });
        return deserializarOperacion(resultado.respuestaJson());
    }

    @Transactional
    ReportOperation ejecutarGenerarExtraordinario(
            ExtraordinarioReportRequest request, ReporteAuthContext contexto) {
        autorizarEvaluador(contexto);
        if (request.solicitudDocumentoId() == null
                || request.aprobacionOficinaDocumentoId() == null) {
            throw ReportesValidationException.aprobacionRequerida();
        }
        LocalDate fechaCorte = parsearFechaCorte(request.fechaCorte());
        if (fechaCorte == null) {
            throw ReportesValidationException.fechaCorteInvalida();
        }
        ReportFiltros filtros = request.filtros() == null
                ? filtrosVacio() : request.filtros();
        Integer anio = fechaCorte.getYear();
        return generar(TipoReporte.EXTRAORDINARIO, anio, null,
                request.periodo(), fechaCorte, filtros, contexto,
                request.solicitudDocumentoId(),
                request.aprobacionOficinaDocumentoId());
    }

    // -----------------------------------------------------------------
    // 3) Pipeline comun de generacion
    // -----------------------------------------------------------------

    @Transactional
    ReportOperation generar(TipoReporte tipo, Integer anio, Integer semestre,
            String periodo, LocalDate fechaCorte, ReportFiltros filtros,
            ReporteAuthContext contexto,
            Long solicitudDocumentoId, Long aprobacionDocumentoId) {
        // Idempotencia natural por la clave natural del reporte.
        Optional<ReporteInstitucionalEntity> existente =
                reporteRepository.findByTipoAnioSemestrePeriodo(
                        tipo, anio, semestre, periodo);
        if (existente.isPresent()) {
            ReporteInstitucionalEntity fila = existente.get();
            if (fila.getFechaCorte().equals(fechaCorte)) {
                return operacionDesdeFila(fila);
            }
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "REPORT_PERIOD_CONFLICT: ya existe un reporte "
                            + "con la misma clave natural y otra fecha de corte.");
        }

        // 1) Construir y persistir snapshot
        JsonNode snapshot = construirSnapshot(tipo, anio, semestre,
                periodo, fechaCorte, filtros, contexto);
        String payloadJson = serializarSnapshot(snapshot);
        String hashSnapshot = hashSha256(payloadJson);
        Optional<ReporteSnapshotEntity> snapshotPrevio =
                snapshotRepository.findByHashSha256(hashSnapshot);
        ReporteSnapshotEntity snapshotEntity;
        if (snapshotPrevio.isPresent()) {
            snapshotEntity = snapshotPrevio.get();
        } else {
            snapshotEntity = new ReporteSnapshotEntity();
            snapshotEntity.setPayloadJson(payloadJson);
            snapshotEntity.setVersionEsquema(VERSION_ESQUEMA_SNAPSHOT);
            snapshotEntity.setHashSha256(hashSnapshot);
            snapshotEntity.setFechaCorte(fechaCorte);
            snapshotEntity.setParametros(serializarFiltros(filtros));
            snapshotEntity.setClasificacion(ClasificacionReporte.INTERNO);
            snapshotEntity.setCreadoPor(contexto == null
                    ? "unknown" : contexto.actorSub());
            try {
                snapshotEntity = snapshotRepository.save(snapshotEntity);
            } catch (DataIntegrityViolationException carrera) {
                snapshotEntity = snapshotRepository.findByHashSha256(hashSnapshot)
                        .orElseThrow(() -> carrera);
            }
        }

        // 2) Persistir reporte
        ReporteInstitucionalEntity reporte = new ReporteInstitucionalEntity();
        reporte.setTipo(tipo);
        reporte.setAnio(anio);
        reporte.setSemestre(semestre);
        reporte.setPeriodo(periodo);
        reporte.setFechaCorte(fechaCorte);
        reporte.setParametros(serializarFiltros(filtros));
        reporte.setIdSnapshot(snapshotEntity.getId());
        reporte.setVersionDatos(1);
        reporte.setClasificacion(ClasificacionReporte.INTERNO);
        reporte.setIdGenerador(contexto == null
                ? 0L : contexto.actorUsuarioId());
        reporte.setEstadoTecnico(EstadoTecnicoReporte.INICIADA);
        try {
            reporte = reporteRepository.save(reporte);
        } catch (DataIntegrityViolationException carrera) {
            ReporteInstitucionalEntity repetido =
                    reporteRepository
                            .findByTipoAnioSemestrePeriodo(tipo, anio,
                                    semestre, periodo)
                            .orElseThrow(() -> carrera);
            return operacionDesdeFila(repetido);
        }

        // 3) Generar PDF y XLSX desde el snapshot canonico
        ReporteDetail detalle = construirDetalleParaRender(reporte,
                snapshotEntity, filtros, snapshot);
        try {
            emitirArchivos(detalle, snapshot, contexto);
        } catch (RuntimeException ex) {
            reporte.setEstadoTecnico(EstadoTecnicoReporte.FALLIDA);
            reporteRepository.save(reporte);
            registrarExito(contexto, reporte.getId(),
                    OP_GENERAR_SEMESTRAL, "REPORT_RENDER_FAILED",
                    Map.of("motivo", truncar(ex.getMessage(), 1000)));
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "REPORT_RENDER_FAILED: " + ex.getMessage());
        }

        // 4) Confirmar estado GENERADA
        reporte.setEstadoTecnico(EstadoTecnicoReporte.GENERADA);
        reporteRepository.save(reporte);

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("reporteId", String.valueOf(reporte.getId()));
        cambios.put("tipo", tipo.name());
        cambios.put("anio", String.valueOf(anio));
        cambios.put("periodo", periodo);
        cambios.put("fechaCorte", fechaCorte.toString());
        cambios.put("snapshotId", String.valueOf(snapshotEntity.getId()));
        cambios.put("hashSnapshot", hashSnapshot);
        registrarExito(contexto, reporte.getId(),
                tipo == TipoReporte.SEMESTRAL
                        ? OP_GENERAR_SEMESTRAL
                        : OP_GENERAR_EXTRAORDINARIO,
                "SUCCESS", cambios);

        return operacionDesdeFila(reporte);
    }

    // -----------------------------------------------------------------
    // 4) Consulta del detalle
    // -----------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ReporteDetail consultar(Long idReporte, ReporteAuthContext contexto) {
        autorizarEvaluador(contexto);
        ReporteInstitucionalEntity reporte =
                reporteRepository.findById(idReporte)
                        .orElseThrow(() -> ReportesValidationException
                                .reporteNoEncontrado());
        ReporteSnapshotEntity snapshot = reporte.getIdSnapshot() == null
                ? null
                : snapshotRepository.findById(reporte.getIdSnapshot())
                        .orElse(null);
        JsonNode snapshotJson = parsearSnapshot(snapshot);
        ReportFiltros filtros = deserializarFiltros(reporte.getParametros());
        List<IndicadorReporte> indicadores =
                datosReader.indicadoresBr122(reporte.getFechaCorte(), filtros);
        List<TotalDimension> totales =
                datosReader.totalesBr121(reporte.getFechaCorte(), filtros);
        List<ReporteArchivoSummary> archivos = archivoRepository
                .findByIdReporte(reporte.getId()).stream()
                .map(this::toArchivoSummary)
                .toList();
        String etag = etag(reporte, snapshot);
        return new ReporteDetail(
                reporte.getId(),
                reporte.getTipo().name(),
                reporte.getAnio(),
                reporte.getSemestre(),
                reporte.getPeriodo(),
                reporte.getFechaCorte(),
                reporte.getVersionDatos(),
                reporte.getEstadoTecnico().name(),
                reporte.getClasificacion().name(),
                snapshot == null ? null : snapshot.getHashSha256(),
                reporte.getIdSnapshot(),
                reporte.getIdGenerador(),
                reporte.getFechaGeneracion(),
                filtros,
                indicadores,
                totales,
                archivos,
                etag);
    }

    @Override
    @Transactional(readOnly = true)
    public ReporteArchivoDescarga descargarArchivo(Long idReporte,
            String formato, ReporteAuthContext contexto) {
        autorizarEvaluador(contexto);
        if (idReporte == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "REPORT_ID_REQUIRED");
        }
        ReporteArchivoEntity.FormatoArchivoReporte formatoEnum =
                parsearFormato(formato);
        ReporteDetail detalle = consultar(idReporte, contexto);
        ReporteArchivoSummary archivo = detalle.archivos().stream()
                .filter(a -> a.formato() != null
                        && a.formato().equalsIgnoreCase(formato))
                .findFirst()
                .orElseThrow(() -> ReportesValidationException
                        .archivoNoEncontrado());
        byte[] contenido = renderizar(detalle, formatoEnum);
        return new ReporteArchivoDescarga(contenido,
                "reporte-" + idReporte + "."
                        + archivo.formato().toLowerCase(),
                mediaType(formatoEnum).toString(),
                "\"" + archivo.hashSha256() + "\"");
    }

    private static ReporteArchivoEntity.FormatoArchivoReporte parsearFormato(
            String texto) {
        if (texto == null) {
            throw ReportesValidationException.formatoInvalido(null);
        }
        try {
            return ReporteArchivoEntity.FormatoArchivoReporte
                    .valueOf(texto.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw ReportesValidationException.formatoInvalido(texto);
        }
    }

    private static MediaType mediaType(
            ReporteArchivoEntity.FormatoArchivoReporte formato) {
        if (formato == ReporteArchivoEntity.FormatoArchivoReporte.PDF) {
            return MediaType.APPLICATION_PDF;
        }
        return MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }

    private byte[] renderizar(ReporteDetail detalle,
            ReporteArchivoEntity.FormatoArchivoReporte formato) {
        try {
            JsonNode snapshot = parsearSnapshot(detalle.idSnapshot());
            if (formato == ReporteArchivoEntity.FormatoArchivoReporte.PDF) {
                return pdfRenderer.render(detalle, snapshot);
            }
            return xlsxRenderer.render(detalle, snapshot);
        } catch (java.io.IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "REPORT_RENDER_FAILED: " + ex.getMessage());
        }
    }

    private JsonNode parsearSnapshot(Long idSnapshot) {
        if (idSnapshot == null) {
            return null;
        }
        return snapshotRepository.findById(idSnapshot)
                .map(s -> {
                    try {
                        return objectMapper.readTree(s.getPayloadJson());
                    } catch (JsonProcessingException ex) {
                        return null;
                    }
                })
                .orElse(null);
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private void emitirArchivos(ReporteDetail detalle, JsonNode snapshot,
            ReporteAuthContext contexto) {
        emitirArchivo(detalle, snapshot, contexto,
                ReporteArchivoEntity.FormatoArchivoReporte.PDF,
                "application/pdf",
                "pdf",
                contenido -> pdfBytes(detalle, contenido));
        emitirArchivo(detalle, snapshot, contexto,
                ReporteArchivoEntity.FormatoArchivoReporte.XLSX,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "xlsx",
                contenido -> xlsxBytes(detalle, contenido));
    }

    @FunctionalInterface
    private interface Renderizador {
        byte[] aplicar(JsonNode snapshot) throws Exception;
    }

    private void emitirArchivo(ReporteDetail detalle, JsonNode snapshot,
            ReporteAuthContext contexto,
            ReporteArchivoEntity.FormatoArchivoReporte formato,
            String mimeType, String extension, Renderizador render) {
        Optional<ReporteArchivoEntity> ultimaVersion =
                archivoRepository
                        .findFirstByIdReporteAndFormatoOrderByVersionDesc(
                                detalle.idReporte(), formato);
        int version = ultimaVersion.map(a -> a.getVersion() + 1).orElse(1);
        byte[] bytes;
        try {
            bytes = render.aplicar(snapshot);
        } catch (Exception ex) {
            throw new IllegalStateException("Fallo generando " + extension
                    + ": " + ex.getMessage(), ex);
        }
        String hash = hashSha256Hex(new String(bytes, StandardCharsets.ISO_8859_1));
        // El documento PDF/XLSX se almacena como version documental;
        // aqui delegamos en el servicio de documentos cuando esta
        // disponible y registramos el hash. Sin ese vinculo, la fila
        // REPORTE_ARCHIVO queda con idDocumentoVersion = -1 como
        // marcador pendiente de vinculacion por el modulo documentos.
        Long idDocumentoVersion = -1L;
        ReporteArchivoEntity archivo = new ReporteArchivoEntity();
        archivo.setIdReporte(detalle.idReporte());
        archivo.setFormato(formato);
        archivo.setVersion(version);
        archivo.setHashSha256(hash);
        archivo.setIdDocumentoVersion(idDocumentoVersion);
        archivo.setCreadoPor(contexto == null
                ? "unknown" : contexto.actorSub());
        archivoRepository.save(archivo);
    }

    private byte[] pdfBytes(ReporteDetail detalle, JsonNode snapshot) {
        try {
            return pdfRenderer.render(detalle, snapshot);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private byte[] xlsxBytes(ReporteDetail detalle, JsonNode snapshot) {
        try {
            return xlsxRenderer.render(detalle, snapshot);
        } catch (java.io.IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private ReporteDetail construirDetalleParaRender(
            ReporteInstitucionalEntity reporte,
            ReporteSnapshotEntity snapshot,
            ReportFiltros filtros, JsonNode snapshotJson) {
        List<IndicadorReporte> indicadores =
                datosReader.indicadoresBr122(reporte.getFechaCorte(), filtros);
        List<TotalDimension> totales =
                datosReader.totalesBr121(reporte.getFechaCorte(), filtros);
        return new ReporteDetail(
                reporte.getId(),
                reporte.getTipo().name(),
                reporte.getAnio(),
                reporte.getSemestre(),
                reporte.getPeriodo(),
                reporte.getFechaCorte(),
                reporte.getVersionDatos(),
                reporte.getEstadoTecnico().name(),
                reporte.getClasificacion().name(),
                snapshot.getHashSha256(),
                snapshot.getId(),
                reporte.getIdGenerador(),
                reporte.getFechaGeneracion(),
                filtros,
                indicadores,
                totales,
                List.of(),
                "\"pendiente\"");
    }

    private ReporteArchivoSummary toArchivoSummary(ReporteArchivoEntity e) {
        return new ReporteArchivoSummary(
                e.getId(), e.getFormato().name(), e.getVersion(),
                e.getHashSha256(), e.getIdDocumentoVersion(),
                e.getCreadoPor(), e.getFechaCreacion());
    }

    private ReportOperation operacionDesdeFila(
            ReporteInstitucionalEntity reporte) {
        return new ReportOperation(
                reporte.getId(),
                "OP-" + reporte.getId(),
                reporte.getFechaCorte(),
                reporte.getVersionDatos(),
                reporte.getEstadoTecnico().name(),
                reporte.getClasificacion().name(),
                cargarHash(reporte.getIdSnapshot()),
                reporte.getFechaGeneracion());
    }

    private String cargarHash(Long idSnapshot) {
        if (idSnapshot == null) {
            return null;
        }
        return snapshotRepository.findById(idSnapshot)
                .map(ReporteSnapshotEntity::getHashSha256)
                .orElse(null);
    }

    private JsonNode construirSnapshot(TipoReporte tipo, Integer anio,
            Integer semestre, String periodo, LocalDate fechaCorte,
            ReportFiltros filtros, ReporteAuthContext contexto) {
        ObjectNode raiz = objectMapper.createObjectNode();
        raiz.put("versionEsquema", VERSION_ESQUEMA_SNAPSHOT);
        raiz.put("tipo", tipo.name());
        raiz.put("anio", anio);
        if (semestre != null) {
            raiz.put("semestre", semestre);
        }
        raiz.put("periodo", periodo);
        raiz.put("fechaCorte", fechaCorte.toString());
        raiz.set("filtros", serializarFiltrosComoNodo(filtros));
        raiz.set("indicadores", serializarIndicadores(
                datosReader.indicadoresBr122(fechaCorte, filtros)));
        raiz.set("totales", serializarTotales(
                datosReader.totalesBr121(fechaCorte, filtros)));
        raiz.set("detalle", serializarDetalle(
                datosReader.detalleProyectos(fechaCorte, filtros)));
        raiz.put("clasificacion", ClasificacionReporte.INTERNO.name());
        raiz.put("generadoPor", contexto == null
                ? "unknown" : contexto.actorSub());
        raiz.put("generadoEn", LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        return raiz;
    }

    private String serializarSnapshot(JsonNode snapshot) {
        try {
            ObjectMapper canonico = objectMapper.copy();
            canonico.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS,
                    true);
            return canonico.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "No se pudo serializar el snapshot canonico.", ex);
        }
    }

    private JsonNode parsearSnapshot(ReporteSnapshotEntity snapshot) {
        if (snapshot == null || snapshot.getPayloadJson() == null) {
            return objectMapper.createObjectNode();
        }
        try {
            return objectMapper.readTree(snapshot.getPayloadJson());
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "El snapshot persistido no es JSON valido.", ex);
        }
    }

    private ArrayNode serializarIndicadores(List<IndicadorReporte> indicadores) {
        ArrayNode arreglo = objectMapper.createArrayNode();
        for (IndicadorReporte indicador : indicadores) {
            ObjectNode nodo = objectMapper.createObjectNode();
            nodo.put("nombre", indicador.nombre());
            nodo.put("numerador", indicador.numerador());
            nodo.put("denominador", indicador.denominador());
            if (indicador.aplicable()) {
                nodo.put("porcentaje", indicador.porcentaje());
            } else {
                nodo.putNull("porcentaje");
            }
            nodo.put("aplicable", indicador.aplicable());
            nodo.put("detalle", indicador.detalle() == null
                    ? "" : indicador.detalle());
            arreglo.add(nodo);
        }
        return arreglo;
    }

    private ArrayNode serializarTotales(List<TotalDimension> totales) {
        ArrayNode arreglo = objectMapper.createArrayNode();
        for (TotalDimension dimension : totales) {
            ObjectNode nodo = objectMapper.createObjectNode();
            nodo.put("dimension", dimension.dimension());
            ArrayNode items = objectMapper.createArrayNode();
            if (dimension.items() != null) {
                for (TotalDimensionItem item : dimension.items()) {
                    ObjectNode itemNode = objectMapper.createObjectNode();
                    itemNode.put("clave", item.clave() == null
                            ? "" : item.clave());
                    itemNode.put("etiqueta", item.etiqueta() == null
                            ? "" : item.etiqueta());
                    itemNode.put("total", item.total());
                    items.add(itemNode);
                }
            }
            nodo.set("items", items);
            arreglo.add(nodo);
        }
        return arreglo;
    }

    private ArrayNode serializarDetalle(List<Map<String, Object>> detalle) {
        ArrayNode arreglo = objectMapper.createArrayNode();
        for (Map<String, Object> fila : detalle) {
            arreglo.add(objectMapper.valueToTree(fila));
        }
        return arreglo;
    }

    private ObjectNode serializarFiltrosComoNodo(ReportFiltros filtros) {
        ObjectNode nodo = objectMapper.createObjectNode();
        if (filtros == null) {
            return nodo;
        }
        if (filtros.tipo() != null) nodo.put("tipo", filtros.tipo());
        if (filtros.estado() != null) nodo.put("estado", filtros.estado());
        if (filtros.unidadId() != null) nodo.put("unidadId", filtros.unidadId());
        if (filtros.responsableId() != null) nodo.put("responsableId", filtros.responsableId());
        if (filtros.fuente() != null) nodo.put("fuente", filtros.fuente());
        if (filtros.tipoSolucion() != null) nodo.put("tipoSolucion", filtros.tipoSolucion());
        if (filtros.producto() != null) nodo.put("producto", filtros.producto());
        if (filtros.unidadesAdicionales() != null) {
            ArrayNode arr = objectMapper.createArrayNode();
            filtros.unidadesAdicionales().forEach(arr::add);
            nodo.set("unidadesAdicionales", arr);
        }
        return nodo;
    }

    private String serializarFiltros(ReportFiltros filtros) {
        try {
            return objectMapper.writeValueAsString(
                    serializarFiltrosComoNodo(filtros));
        } catch (JsonProcessingException ex) {
            return "{}";
        }
    }

    private ReportFiltros deserializarFiltros(String json) {
        if (json == null || json.isBlank()) {
            return filtrosVacio();
        }
        try {
            JsonNode nodo = objectMapper.readTree(json);
            List<String> adicionales = new ArrayList<>();
            if (nodo.has("unidadesAdicionales")
                    && nodo.get("unidadesAdicionales").isArray()) {
                nodo.get("unidadesAdicionales").forEach(
                        n -> adicionales.add(n.asText()));
            }
            return new ReportFiltros(
                    nodo.path("tipo").asText(null),
                    nodo.path("estado").asText(null),
                    nodo.has("unidadId") && !nodo.get("unidadId").isNull()
                            ? nodo.get("unidadId").asLong() : null,
                    nodo.has("responsableId") && !nodo.get("responsableId").isNull()
                            ? nodo.get("responsableId").asLong() : null,
                    nodo.path("fuente").asText(null),
                    nodo.path("tipoSolucion").asText(null),
                    nodo.path("producto").asText(null),
                    adicionales);
        } catch (JsonProcessingException ex) {
            return filtrosVacio();
        }
    }

    private ReportFiltros filtrosVacio() {
        return new ReportFiltros(null, null, null, null,
                null, null, null, List.of());
    }

    private LocalDate corteSemestral(int anio, int semestre) {
        if (semestre == 1) {
            return LocalDate.of(anio, 6, 30);
        }
        if (semestre == 2) {
            return LocalDate.of(anio, 12, 31);
        }
        throw ReportesValidationException.semesterInvalido();
    }

    private String periodoSemestral(int anio, int semestre) {
        return anio + "-S" + semestre;
    }

    private LocalDate parsearFechaCorte(String fecha) {
        if (fecha == null || fecha.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(fecha);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void autorizarEvaluador(ReporteAuthContext contexto) {
        if (contexto == null
                || !PERFIL_EVALUADOR.equals(contexto.perfilEfectivo())) {
            throw ReportesValidationException.perfilNoAutorizado(
                    contexto == null ? null : contexto.perfilEfectivo());
        }
        if (autorizacionService != null
                && contexto.asignacionEfectivaId() != null
                && contexto.actorSub() != null
                && contexto.unidadEfectivaId() != null) {
            try {
                autorizacionService.revalidarParaOperacionSensible(
                        contexto.actorSub(),
                        contexto.asignacionEfectivaId(),
                        PERFIL_EVALUADOR,
                        contexto.unidadEfectivaId());
            } catch (ResponseStatusException rse) {
                throw ReportesValidationException.alcanceUnidadNoAutorizado();
            }
        }
    }

    private void registrarExito(ReporteAuthContext contexto, Long recursoId,
            String operacion, String codigo, Map<String, String> cambios) {
        if (auditService == null) {
            return;
        }
        auditService.registrarExito(new AuditService.AuditCommand(
                contexto == null ? null : contexto.correlacionId(),
                contexto == null ? null : contexto.actorUsuarioId(),
                null,
                contexto == null ? null : contexto.asignacionEfectivaId(),
                contexto == null ? null : contexto.perfilEfectivo(),
                contexto == null ? null : contexto.unidadEfectivaId(),
                operacion,
                CONSUMIDOR,
                RECURSO_REPORTE,
                recursoId,
                codigo,
                cambios,
                "INTERNO"));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void registrarDenegacion(ReporteAuthContext contexto, Long recursoId,
            String operacion, String codigo, String detalle,
            Map<String, String> cambios) {
        if (auditService == null) {
            return;
        }
        try {
            String correlacion = contexto == null || contexto.correlacionId() == null
                    ? "no-correlation" : contexto.correlacionId();
            Map<String, String> evidencia = new LinkedHashMap<>();
            if (cambios != null) {
                evidencia.putAll(cambios);
            }
            evidencia.put("detalle", truncar(detalle, 1000));
            auditService.registrarDenegacion(new AuditService.AuditCommand(
                    correlacion,
                    contexto == null ? null : contexto.actorUsuarioId(),
                    null,
                    contexto == null ? null : contexto.asignacionEfectivaId(),
                    contexto == null ? null : contexto.perfilEfectivo(),
                    contexto == null ? null : contexto.unidadEfectivaId(),
                    operacion,
                    CONSUMIDOR,
                    RECURSO_REPORTE,
                    recursoId,
                    codigo,
                    evidencia,
                    "RESTRINGIDO"));
        } catch (RuntimeException ex) {
            LOG.warn("Fallo registrando denegacion de reportes: {}", ex.getMessage());
        }
    }

    private static String contextoActorSub(ReporteAuthContext ctx) {
        return ctx == null || ctx.actorSub() == null ? "unknown" : ctx.actorSub();
    }

    private static String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() > max ? valor.substring(0, max) : valor;
    }

    private String serializarOperacion(ReportOperation op) {
        try {
            return objectMapper.writeValueAsString(op);
        } catch (JsonProcessingException ex) {
            return "{\"reporteId\":" + op.reporteId() + "}";
        }
    }

    private ReportOperation deserializarOperacion(String json) {
        try {
            return objectMapper.readValue(json, ReportOperation.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException(
                    "No se pudo deserializar la respuesta idempotente.", ex);
        }
    }

    private String etag(ReporteInstitucionalEntity reporte,
            ReporteSnapshotEntity snapshot) {
        StringBuilder sb = new StringBuilder("\"");
        sb.append(reporte.getId());
        sb.append("-");
        sb.append(reporte.getVersionDatos());
        if (snapshot != null) {
            sb.append("-");
            sb.append(snapshot.getHashSha256().substring(0, 16));
        }
        sb.append("\"");
        return sb.toString();
    }

    private String hashSha256(String valor) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(valor.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    private String hashSha256Hex(String valor) {
        return hashSha256(valor);
    }
}
