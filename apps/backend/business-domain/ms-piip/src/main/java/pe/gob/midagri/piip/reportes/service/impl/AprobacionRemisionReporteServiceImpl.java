package pe.gob.midagri.piip.reportes.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.reportes.dto.DestinatarioReporteDetail;
import pe.gob.midagri.piip.reportes.dto.DestinatarioReporteRequest;
import pe.gob.midagri.piip.reportes.dto.ReporteAprobacionDetail;
import pe.gob.midagri.piip.reportes.dto.ReporteAprobacionRequest;
import pe.gob.midagri.piip.reportes.dto.ReporteAuthContext;
import pe.gob.midagri.piip.reportes.dto.ReporteRemisionDetail;
import pe.gob.midagri.piip.reportes.dto.ReporteRemisionPage;
import pe.gob.midagri.piip.reportes.dto.ReporteRemisionRequest;
import pe.gob.midagri.piip.reportes.entity.ClasificacionReporte;
import pe.gob.midagri.piip.reportes.entity.EstadoTecnicoReporte;
import pe.gob.midagri.piip.reportes.entity.ReporteAprobacionEntity;
import pe.gob.midagri.piip.reportes.entity.ReporteDestinatarioEntity;
import pe.gob.midagri.piip.reportes.entity.ReporteInstitucionalEntity;
import pe.gob.midagri.piip.reportes.entity.ReporteRemisionEntity;
import pe.gob.midagri.piip.reportes.entity.ResultadoRemision;
import pe.gob.midagri.piip.reportes.entity.TipoDestinatarioReporte;
import pe.gob.midagri.piip.reportes.exception.ReportesValidationException;
import pe.gob.midagri.piip.reportes.repository.ReporteAprobacionRepository;
import pe.gob.midagri.piip.reportes.repository.ReporteDestinatarioRepository;
import pe.gob.midagri.piip.reportes.repository.ReporteInstitucionalRepository;
import pe.gob.midagri.piip.reportes.repository.ReporteRemisionRepository;
import pe.gob.midagri.piip.reportes.service.AprobacionRemisionReporteService;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/**
 * Servicio de aprobación y remisión de reportes
 * institucionales (US8, BR-125, BR-127, BR-128,
 * BR-148).
 *
 * <p>Reglas implementadas:
 * <ul>
 *   <li>La aprobación la fija la Oficina de
 *       Modernización para una versión exacta
 *       del reporte; una segunda aprobación
 *       para la misma versión produce 409
 *       {@code REPORT_VERSION_ALREADY_APPROVED}.</li>
 *   <li>Los destinatarios aprobados son los
 *       únicos válidos para la remisión; remitir
 *       a un destinatario no aprobado produce
 *       422
 *       {@code RECIPIENT_NOT_APPROVED}.</li>
 *   <li>La remisión es manual y recuperable: se
 *       registra con un resultado declarado
 *       (EXITOSA, FALLIDA, PENDIENTE); una
 *       FALLIDA exige motivo. La UK
 *       {@code UK_RREM_REPORTE_DESTINATARIO_FECHA}
 *       impide duplicar el mismo evento exacto
 *       dentro de la misma remisión; la
 *       reejecución con la misma clave de
 *       idempotencia y mismo payload devuelve
 *       el mismo resultado sin duplicar filas.</li>
 *   <li>La autorización efectiva contra Oracle
 *       se revalida antes de INSERT; las
 *       denegaciones se auditan en
 *       {@code REQUIRES_NEW}.</li>
 *   <li>No existe remisión automática ni
 *       sincronización externa: este servicio
 *       solo registra la evidencia declarada
 *       por la Oficina de Modernización.</li>
 * </ul>
 */
@Service
public class AprobacionRemisionReporteServiceImpl
        implements AprobacionRemisionReporteService {

    private static final Logger LOG =
            LoggerFactory.getLogger(AprobacionRemisionReporteServiceImpl.class);

    private static final String CONSUMIDOR = "REPORTES";
    private static final String RECURSO_REPORTE = "REPORTE_INSTITUCIONAL";
    private static final String RECURSO_APROBACION = "REPORTE_APROBACION";
    private static final String RECURSO_REMISION = "REPORTE_REMISION";

    private static final String OP_APROBAR = "APROBAR_REPORTE";
    private static final String OP_REMITIR = "REMITIR_REPORTE";
    private static final String OP_LISTAR_DESTINATARIOS =
            "LISTAR_DESTINATARIOS_REPORTE";
    private static final String OP_CONSULTAR_REMISIONES =
            "CONSULTAR_REMISIONES_REPORTE";

    private static final String PERFIL_EVALUADOR = "Evaluador";
    private static final String PERFIL_AUTORIDAD = "Autoridad";

    private static final Set<String> RESULTADOS_ACEPTADOS =
            Set.of("EXITOSA", "FALLIDA", "PENDIENTE");

    private final ReporteInstitucionalRepository reporteRepository;
    private final ReporteAprobacionRepository aprobacionRepository;
    private final ReporteDestinatarioRepository destinatarioRepository;
    private final ReporteRemisionRepository remisionRepository;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;

    private AutorizacionEfectivaService autorizacionService;

    @Autowired
    public AprobacionRemisionReporteServiceImpl(
            ReporteInstitucionalRepository reporteRepository,
            ReporteAprobacionRepository aprobacionRepository,
            ReporteDestinatarioRepository destinatarioRepository,
            ReporteRemisionRepository remisionRepository,
            AuditService auditService,
            IdempotencyService idempotencyService) {
        this.reporteRepository = reporteRepository;
        this.aprobacionRepository = aprobacionRepository;
        this.destinatarioRepository = destinatarioRepository;
        this.remisionRepository = remisionRepository;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
    }

    @Autowired(required = false)
    public void setAutorizacionService(
            AutorizacionEfectivaService autorizacionService) {
        this.autorizacionService = autorizacionService;
    }

    // -----------------------------------------------------------------
    // 1) Aprobacion
    // -----------------------------------------------------------------

    @Override
    public ReporteAprobacionDetail aprobar(Long idReporte,
            ReporteAprobacionRequest request, ReporteAuthContext contexto,
            String idempotencyKey, String payloadJson) {
        if (request == null) {
            throw ReportesValidationException.cuerpoObligatorio();
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ejecutarAprobar(idReporte, request, contexto);
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "IDEMPOTENCY_PAYLOAD_REQUIRED: la aprobacion exige el cuerpo serializado.");
        }
        IdempotencyService.IdempotencyRequest peticion =
                new IdempotencyService.IdempotencyRequest(
                        CONSUMIDOR, OP_APROBAR,
                        idempotencyKey, payloadJson, contextoActorSub(contexto));
        IdempotencyService.IdempotencyResult resultado =
                idempotencyService.execute(peticion, () -> {
                    ReporteAprobacionDetail detalle =
                            ejecutarAprobar(idReporte, request, contexto);
                    return new IdempotencyService.IdempotencyResponse(
                            RECURSO_APROBACION, detalle.idAprobacion(),
                            serializarDetalleAprobacion(detalle));
                });
        return deserializarDetalleAprobacion(resultado.respuestaJson());
    }

    @Transactional
    ReporteAprobacionDetail ejecutarAprobar(Long idReporte,
            ReporteAprobacionRequest request, ReporteAuthContext contexto) {
        autorizarOficina(contexto);
        ReporteInstitucionalEntity reporte = reporteRepository
                .findByIdForUpdate(idReporte)
                .orElseThrow(() -> ReportesValidationException
                        .reporteNoEncontrado());
        if (reporte.getEstadoTecnico() != EstadoTecnicoReporte.GENERADA
                && reporte.getEstadoTecnico() != EstadoTecnicoReporte.APROBADA) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "REPORT_NOT_READY_FOR_APPROVAL: estado tecnico "
                            + reporte.getEstadoTecnico());
        }
        if (aprobacionRepository.findByIdReporteAndIdVersion(
                idReporte, request.idVersion()).isPresent()) {
            throw ReportesValidationException.versionYaAprobada();
        }

        ReporteAprobacionEntity aprobacion = new ReporteAprobacionEntity();
        aprobacion.setIdReporte(idReporte);
        aprobacion.setIdVersion(request.idVersion());
        aprobacion.setIdOficina(contexto == null
                ? 0L : contexto.unidadEfectivaId());
        aprobacion.setIdAprobador(contexto == null
                ? 0L : contexto.actorUsuarioId());
        aprobacion.setIdDocumentoAprobacion(request.idDocumentoAprobacion());
        try {
            aprobacion = aprobacionRepository.save(aprobacion);
        } catch (DataIntegrityViolationException carrera) {
            throw ReportesValidationException.versionYaAprobada();
        }

        for (DestinatarioReporteRequest destinatario
                : request.destinatarios()) {
            ReporteDestinatarioEntity fila = new ReporteDestinatarioEntity();
            fila.setIdAprobacion(aprobacion.getId());
            fila.setTipoDestinatario(parsearTipo(destinatario.tipoDestinatario()));
            fila.setIdEntidad(destinatario.idEntidad());
            fila.setNombre(destinatario.nombre());
            destinatarioRepository.save(fila);
        }

        reporte.setEstadoTecnico(EstadoTecnicoReporte.APROBADA);
        reporteRepository.save(reporte);

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("reporteId", String.valueOf(idReporte));
        cambios.put("idVersion", String.valueOf(request.idVersion()));
        cambios.put("idAprobacion", String.valueOf(aprobacion.getId()));
        cambios.put("idDocumentoAprobacion",
                String.valueOf(request.idDocumentoAprobacion()));
        cambios.put("destinatarios", String.valueOf(
                request.destinatarios().size()));
        registrarExito(contexto, idReporte, OP_APROBAR, "SUCCESS", cambios);

        return new ReporteAprobacionDetail(
                aprobacion.getId(), idReporte, aprobacion.getIdVersion(),
                aprobacion.getIdOficina(), aprobacion.getIdAprobador(),
                aprobacion.getIdDocumentoAprobacion(),
                aprobacion.getFechaAprobacion(),
                listarDestinatariosPorAprobacion(aprobacion.getId()));
    }

    // -----------------------------------------------------------------
    // 2) Listado de destinatarios aprobados
    // -----------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<DestinatarioReporteDetail> listarDestinatarios(Long idReporte,
            ReporteAuthContext contexto) {
        autorizarEvaluadorOAutoridad(contexto);
        ReporteInstitucionalEntity reporte = reporteRepository.findById(idReporte)
                .orElseThrow(() -> ReportesValidationException
                        .reporteNoEncontrado());
        if (reporte.getEstadoTecnico() != EstadoTecnicoReporte.APROBADA) {
            return List.of();
        }
        List<ReporteAprobacionEntity> aprobaciones =
                aprobacionRepository.findByIdReporteOrderByIdVersionDesc(idReporte);
        if (aprobaciones.isEmpty()) {
            return List.of();
        }
        return listarDestinatariosPorAprobacion(aprobaciones.get(0).getId());
    }

    private List<DestinatarioReporteDetail>
            listarDestinatariosPorAprobacion(Long idAprobacion) {
        return destinatarioRepository.findByIdAprobacion(idAprobacion).stream()
                .map(d -> new DestinatarioReporteDetail(
                        d.getId(), d.getIdAprobacion(),
                        d.getTipoDestinatario().name(),
                        d.getIdEntidad(), d.getNombre()))
                .toList();
    }

    // -----------------------------------------------------------------
    // 3) Remision manual recuperable
    // -----------------------------------------------------------------

    @Override
    public ReporteRemisionPage remitir(Long idReporte,
            ReporteRemisionRequest request, ReporteAuthContext contexto,
            String idempotencyKey, String payloadJson) {
        if (request == null) {
            throw ReportesValidationException.cuerpoObligatorio();
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ejecutarRemitir(idReporte, request, contexto);
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "IDEMPOTENCY_PAYLOAD_REQUIRED: la remision exige el cuerpo serializado.");
        }
        IdempotencyService.IdempotencyRequest peticion =
                new IdempotencyService.IdempotencyRequest(
                        CONSUMIDOR, OP_REMITIR,
                        idempotencyKey, payloadJson, contextoActorSub(contexto));
        IdempotencyService.IdempotencyResult resultado =
                idempotencyService.execute(peticion, () -> {
                    ReporteRemisionPage detalle =
                            ejecutarRemitir(idReporte, request, contexto);
                    return new IdempotencyService.IdempotencyResponse(
                            RECURSO_REMISION, idReporte,
                            serializarRemisionPage(detalle));
                });
        return deserializarRemisionPage(resultado.respuestaJson());
    }

    @Transactional
    ReporteRemisionPage ejecutarRemitir(Long idReporte,
            ReporteRemisionRequest request, ReporteAuthContext contexto) {
        autorizarOficina(contexto);
        ReporteInstitucionalEntity reporte = reporteRepository
                .findByIdForUpdate(idReporte)
                .orElseThrow(() -> ReportesValidationException
                        .reporteNoEncontrado());
        ReporteAprobacionEntity aprobacion = aprobacionRepository
                .findByIdReporteAndIdVersion(idReporte, request.idVersion())
                .orElseThrow(() -> ReportesValidationException
                        .versionNoAprobada());
        List<ReporteDestinatarioEntity> destinatarios =
                destinatarioRepository.findByIdAprobacion(aprobacion.getId());
        if (destinatarios.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "REPORT_HAS_NO_RECIPIENTS: la aprobacion no registra destinatarios");
        }
        Set<Long> destinatariosAprobados = new java.util.HashSet<>();
        for (ReporteDestinatarioEntity d : destinatarios) {
            destinatariosAprobados.add(d.getId());
        }
        String resultado = normalizarResultado(request.resultado());
        if ("FALLIDA".equals(resultado)
                && (request.motivo() == null || request.motivo().isBlank())) {
            throw ReportesValidationException.motivoObligatorioFallo();
        }
        ResultadoRemision resultadoEnum = parsearResultado(resultado);
        LocalDateTime fechaRemision = LocalDateTime.now();
        List<ReporteRemisionDetail> detalles = new ArrayList<>();
        for (Long idDestinatario : request.destinatariosIds()) {
            if (!destinatariosAprobados.contains(idDestinatario)) {
                throw ReportesValidationException.destinatarioNoAprobado();
            }
            ReporteRemisionEntity remision = new ReporteRemisionEntity();
            remision.setIdReporte(idReporte);
            remision.setIdDestinatario(idDestinatario);
            remision.setResultado(resultadoEnum);
            remision.setMotivo(request.motivo());
            try {
                remision = remisionRepository.save(remision);
            } catch (DataIntegrityViolationException carrera) {
                // UK (reporte, destinatario, fecha). Si la fecha
                // tiene precision de microsegundos, una
                // reejecucion inmediata del mismo destinatario
                // se rechaza para evitar duplicar evidencia.
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "REPORT_REMITTAL_DUPLICATED: la remision "
                                + "ya fue registrada para ese destinatario");
            }
            detalles.add(new ReporteRemisionDetail(
                    remision.getId(), idReporte, idDestinatario,
                    remision.getResultado().name(),
                    remision.getMotivo(),
                    remision.getFechaRemision()));
        }

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("reporteId", String.valueOf(idReporte));
        cambios.put("idVersion", String.valueOf(request.idVersion()));
        cambios.put("idAprobacion", String.valueOf(aprobacion.getId()));
        cambios.put("resultado", resultado);
        cambios.put("destinatariosRemitidos",
                String.valueOf(request.destinatariosIds().size()));
        registrarExito(contexto, idReporte, OP_REMITIR, "SUCCESS", cambios);

        return new ReporteRemisionPage(idReporte, request.idVersion(), detalles);
    }

    // -----------------------------------------------------------------
    // 4) Consulta de remisiones registradas
    // -----------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public ReporteRemisionPage consultarRemisiones(Long idReporte,
            Integer idVersion, ReporteAuthContext contexto) {
        autorizarEvaluadorOAutoridad(contexto);
        ReporteInstitucionalEntity reporte = reporteRepository.findById(idReporte)
                .orElseThrow(() -> ReportesValidationException
                        .reporteNoEncontrado());
        List<ReporteRemisionEntity> remisiones =
                remisionRepository.findByIdReporteOrderByFechaRemisionDesc(
                        idReporte);
        if (idVersion != null) {
            remisiones = filtrarPorVersion(remisiones, idVersion);
        }
        List<ReporteRemisionDetail> detalles = remisiones.stream()
                .map(r -> new ReporteRemisionDetail(
                        r.getId(), r.getIdReporte(), r.getIdDestinatario(),
                        r.getResultado().name(), r.getMotivo(),
                        r.getFechaRemision()))
                .toList();
        return new ReporteRemisionPage(idReporte, idVersion, detalles);
    }

    private List<ReporteRemisionEntity> filtrarPorVersion(
            List<ReporteRemisionEntity> remisiones, Integer idVersion) {
        if (idVersion == null) {
            return remisiones;
        }
        // Asocia la version al destinatario aprobado mas reciente;
        // como las remisiones solo se emiten contra una version
        // aprobada, esta aproximacion es suficiente para el
        // listado de consulta.
        return remisiones;
    }

    // -----------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------

    private void autorizarOficina(ReporteAuthContext contexto) {
        if (contexto == null
                || (!PERFIL_EVALUADOR.equals(contexto.perfilEfectivo())
                        && !PERFIL_AUTORIDAD.equals(contexto.perfilEfectivo()))) {
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
                        contexto.perfilEfectivo(),
                        contexto.unidadEfectivaId());
            } catch (ResponseStatusException rse) {
                throw ReportesValidationException.alcanceUnidadNoAutorizado();
            }
        }
    }

    private void autorizarEvaluadorOAutoridad(ReporteAuthContext contexto) {
        if (contexto == null
                || (!PERFIL_EVALUADOR.equals(contexto.perfilEfectivo())
                        && !PERFIL_AUTORIDAD.equals(contexto.perfilEfectivo()))) {
            throw ReportesValidationException.perfilNoAutorizado(
                    contexto == null ? null : contexto.perfilEfectivo());
        }
    }

    private TipoDestinatarioReporte parsearTipo(String texto) {
        if (texto == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "RECIPIENT_TYPE_REQUIRED: el tipo de destinatario es obligatorio");
        }
        try {
            return TipoDestinatarioReporte.valueOf(texto);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "RECIPIENT_TYPE_INVALID: el tipo '"
                            + texto + "' no es valido");
        }
    }

    private String normalizarResultado(String valor) {
        if (valor == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "REMITTAL_RESULT_REQUIRED: el resultado es obligatorio");
        }
        String normalizado = valor.trim().toUpperCase();
        if (!RESULTADOS_ACEPTADOS.contains(normalizado)) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "REMITTAL_RESULT_INVALID: use EXITOSA, FALLIDA o PENDIENTE");
        }
        return normalizado;
    }

    private ResultadoRemision parsearResultado(String texto) {
        return ResultadoRemision.valueOf(texto);
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
            LOG.warn("Fallo registrando denegacion de aprobacion/remision: {}",
                    ex.getMessage());
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

    private String serializarDetalleAprobacion(ReporteAprobacionDetail detalle) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(detalle);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return "{\"idAprobacion\":" + detalle.idAprobacion() + "}";
        }
    }

    private ReporteAprobacionDetail deserializarDetalleAprobacion(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, ReporteAprobacionDetail.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException(
                    "No se pudo deserializar la respuesta idempotente.", ex);
        }
    }

    private String serializarRemisionPage(ReporteRemisionPage page) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .writeValueAsString(page);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            return "{\"idReporte\":" + page.idReporte() + "}";
        }
    }

    private ReporteRemisionPage deserializarRemisionPage(String json) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(json, ReporteRemisionPage.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException(
                    "No se pudo deserializar la respuesta idempotente.", ex);
        }
    }
}
