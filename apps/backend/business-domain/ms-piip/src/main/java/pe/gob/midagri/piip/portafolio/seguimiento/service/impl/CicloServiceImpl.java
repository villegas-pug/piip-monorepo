package pe.gob.midagri.piip.portafolio.seguimiento.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AnexarCicloVersionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloVersionResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.CicloEvidenciaEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.CicloProyectoEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.CicloVersionEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.mapper.SeguimientoMapper;
import pe.gob.midagri.piip.portafolio.seguimiento.repository.CicloEvidenciaRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.repository.CicloProyectoRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.repository.CicloVersionRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.service.AptitudDocumentalService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.CicloService;


/**
 * Implementacion del servicio de ciclo del proyecto (US4,
 * Constitucion 5.0.0, DDL 015 y contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}).
 *
 * <p>Reglas canonicas aplicadas:
 * <ul>
 *   <li>El periodo del ciclo debe cumplir la CHECK
 *       {@code CK_CP_PERIODO} del DDL 015:
 *       {@code ^[0-9]{4}-Q[1-4]-S[1-2]$}.</li>
 *   <li>El proyecto debe estar en
 *       {@code PROYECTO_EJECUCION} (TIPO_REGISTRO=PROYECTO y
 *       ESTADO=PROYECTO_EJECUCION) para abrir un ciclo.</li>
 *   <li>El ciclo es append-only: la fila cerrada nunca se
 *       modifica; la correccion crea una nueva fila en
 *       {@code CICLO_PROYECTO_VERSION} con
 *       {@code NUMERO_VERSION} incrementado y referencia a la
 *       anterior.</li>
 *   <li>Las evidencias documentales se admiten solo para los
 *       tipos canonicos
 *       ({@code AutoevaluacionCicloTrabajo},
 *       {@code SeguimientoAgilTableroKanban},
 *       {@code MatrizPlanificacionCiclos}) y el documento debe
 *       estar apto segun
 *       {@link AptitudDocumentalService}.</li>
 *   <li>Auditoria atomica de exito en la misma transaccion de
 *       negocio y de denegacion en
 *       {@code REQUIRES_NEW}.</li>
 *   <li>Autorizacion efectiva Oracle mediante
 *       {@link AutorizacionEfectivaService} cuando el bean esta
 *       disponible; en pruebas unitarias se omite sin afectar el
 *       contrato.</li>
 *   <li>Solo el {@code Responsable} titular vigente del proyecto
 *       puede operar sobre los ciclos.</li>
 * </ul>
 */
@Service
public class CicloServiceImpl implements CicloService {

    private static final Logger LOG =
            LoggerFactory.getLogger(CicloServiceImpl.class);

    private static final String CONSUMIDOR = "PORTAFOLIO";
    private static final String RECURSO_CICLO = "CICLO_PROYECTO";
    private static final String RECURSO_CICLO_VERSION = "CICLO_PROYECTO_VERSION";
    private static final String RECURSO_EVIDENCIA = "CICLO_EVIDENCIA";

    private static final String OP_ABRIR_CICLO = "ABRIR_CICLO";
    private static final String OP_CERRAR_CICLO = "CERRAR_CICLO";
    private static final String OP_ANEXAR_VERSION = "ANEXAR_CICLO_VERSION";
    private static final String OP_ADJUNTAR_EVIDENCIA = "ADJUNTAR_EVIDENCIA_CICLO";

    private static final String PERFIL_RESPONSABLE = "Responsable";

    /** Tipos documentales canonicos para evidencia de un ciclo. */
    public static final Set<String> TIPOS_EVIDENCIA_VALIDOS = Set.of(
            "AutoevaluacionCicloTrabajo",
            "SeguimientoAgilTableroKanban",
            "MatrizPlanificacionCiclos");

    /** Regex canonica del periodo quincenal (alineada con CK_CP_PERIODO). */
    public static final Pattern PERIODO_REGEX =
            Pattern.compile("^[0-9]{4}-Q[1-4]-S[1-2]$");

    private final RegistroPortafolioRepository registroRepository;
    private final CicloProyectoRepository cicloRepository;
    private final CicloVersionRepository cicloVersionRepository;
    private final CicloEvidenciaRepository cicloEvidenciaRepository;
    private final AptitudDocumentalService aptitudDocumentalService;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final SeguimientoMapper mapper;

    private AutorizacionEfectivaService autorizacionService;
    private ObjectMapper objectMapper;

    public CicloServiceImpl(RegistroPortafolioRepository registroRepository,
            CicloProyectoRepository cicloRepository,
            CicloVersionRepository cicloVersionRepository,
            CicloEvidenciaRepository cicloEvidenciaRepository,
            AptitudDocumentalService aptitudDocumentalService,
            AuditService auditService,
            IdempotencyService idempotencyService,
            SeguimientoMapper mapper) {
        this.registroRepository = registroRepository;
        this.cicloRepository = cicloRepository;
        this.cicloVersionRepository = cicloVersionRepository;
        this.cicloEvidenciaRepository = cicloEvidenciaRepository;
        this.aptitudDocumentalService = aptitudDocumentalService;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
        this.mapper = mapper;
    }

    @Autowired(required = false)
    public void setAutorizacionService(
            AutorizacionEfectivaService autorizacionService) {
        this.autorizacionService = autorizacionService;
    }

    @Autowired(required = false)
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------------
    // 1) Abrir ciclo
    // ---------------------------------------------------------------------

    @Override
    public CicloResponse abrirCiclo(long proyectoId, String periodo,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson) {
        validarPeriodo(periodo);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ejecutarAbrirCiclo(proyectoId, periodo, ctx);
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "La apertura del ciclo exige el cuerpo serializado para calcular el hash canonico.");
        }
        IdempotencyService.IdempotencyRequest request =
                new IdempotencyService.IdempotencyRequest(
                        CONSUMIDOR, OP_ABRIR_CICLO, idempotencyKey, payloadJson,
                        contextoActorSub(ctx));
        IdempotencyService.IdempotencyResult resultado =
                idempotencyService.execute(request, () -> {
                    CicloResponse detalle = ejecutarAbrirCiclo(proyectoId, periodo, ctx);
                    return new IdempotencyService.IdempotencyResponse(
                            RECURSO_CICLO, detalle.idCiclo(),
                            serializarDetalleCiclo(detalle));
                });
        return deserializarCiclo(resultado.respuestaJson());
    }

    @Transactional
    CicloResponse ejecutarAbrirCiclo(long proyectoId, String periodo,
            PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = cargarProyectoEjecucion(proyectoId);

        cicloRepository.findByIdProyectoAndPeriodo(proyectoId, periodo)
                .ifPresent(existente -> {
                    String detalle = "El proyecto " + proyectoId
                            + " ya tiene un ciclo para el periodo " + periodo;
                    registrarDenegacion(ctx, proyectoId, OP_ABRIR_CICLO,
                            "CYCLE_DUPLICATED", detalle,
                            Map.of("proyectoId", String.valueOf(proyectoId),
                                    "periodo", periodo));
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "CYCLE_DUPLICATED: " + detalle);
                });

        autorizarResponsable(ctx, proyecto);

        CicloProyectoEntity ciclo = new CicloProyectoEntity();
        ciclo.setIdProyecto(proyectoId);
        ciclo.setPeriodo(periodo);
        ciclo.setNumeroVersion(1);
        ciclo.setIdVersionAnterior(null);
        ciclo.setCerrado("N");
        ciclo.setCreadoPor(ctx == null ? null : ctx.actorSub());
        try {
            ciclo = cicloRepository.save(ciclo);
        } catch (DataIntegrityViolationException carrera) {
            String detalle = "Carrera concurrente: el proyecto " + proyectoId
                    + " ya tiene un ciclo para el periodo " + periodo;
            registrarDenegacion(ctx, proyectoId, OP_ABRIR_CICLO,
                    "CYCLE_DUPLICATED", detalle,
                    Map.of("proyectoId", String.valueOf(proyectoId),
                            "periodo", periodo));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CYCLE_DUPLICATED: " + detalle);
        }

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("proyectoId", String.valueOf(proyectoId));
        cambios.put("periodo", periodo);
        cambios.put("numeroVersion", "1");
        cambios.put("cicloId", String.valueOf(ciclo.getId()));
        auditService.registrarExito(new AuditService.AuditCommand(
                ctx == null ? null : ctx.correlacionId(),
                ctx == null ? null : ctx.actorUsuarioId(),
                null,
                ctx == null ? null : ctx.asignacionEfectivaId(),
                ctx == null ? null : ctx.perfilEfectivo(),
                ctx == null ? null : ctx.unidadEfectivaId(),
                OP_ABRIR_CICLO,
                CONSUMIDOR,
                RECURSO_CICLO,
                ciclo.getId(),
                "SUCCESS",
                cambios,
                "INTERNO"));

        return mapper.toCicloResponse(ciclo);
    }

    // ---------------------------------------------------------------------
    // 2) Cerrar ciclo al final del periodo
    // ---------------------------------------------------------------------

    @Override
    public void cerrarCicloAlFinal(long proyectoId, long cicloId,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            ejecutarCerrarCicloAlFinal(proyectoId, cicloId, ctx);
            return;
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "El cierre del ciclo exige el cuerpo serializado para calcular el hash canonico.");
        }
        IdempotencyService.IdempotencyRequest request =
                new IdempotencyService.IdempotencyRequest(
                        CONSUMIDOR, OP_CERRAR_CICLO, idempotencyKey, payloadJson,
                        contextoActorSub(ctx));
        idempotencyService.execute(request, () -> {
            ejecutarCerrarCicloAlFinal(proyectoId, cicloId, ctx);
            return new IdempotencyService.IdempotencyResponse(
                    RECURSO_CICLO, cicloId, "{\"cicloId\":" + cicloId + "}");
        });
    }

    @Transactional
    void ejecutarCerrarCicloAlFinal(long proyectoId, long cicloId,
            PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = cargarProyectoEjecucion(proyectoId);
        CicloProyectoEntity ciclo = cicloRepository.findById(cicloId)
                .orElseThrow(() -> {
                    String detalle = "El ciclo " + cicloId + " no existe.";
                    registrarDenegacion(ctx, proyectoId, OP_CERRAR_CICLO,
                            "CYCLE_NOT_FOUND", detalle,
                            Map.of("cicloId", String.valueOf(cicloId)));
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "CYCLE_NOT_FOUND: " + detalle);
                });
        if (!ciclo.getIdProyecto().equals(proyectoId)) {
            String detalle = "El ciclo " + cicloId
                    + " no pertenece al proyecto " + proyectoId;
            registrarDenegacion(ctx, proyectoId, OP_CERRAR_CICLO,
                    "CYCLE_PROJECT_MISMATCH", detalle,
                    Map.of("cicloId", String.valueOf(cicloId),
                            "proyectoId", String.valueOf(proyectoId)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CYCLE_PROJECT_MISMATCH: " + detalle);
        }
        if ("S".equals(ciclo.getCerrado())) {
            String detalle = "El ciclo " + cicloId + " ya esta cerrado";
            registrarDenegacion(ctx, proyectoId, OP_CERRAR_CICLO,
                    "CYCLE_ALREADY_CLOSED", detalle,
                    Map.of("cicloId", String.valueOf(cicloId)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CYCLE_ALREADY_CLOSED: " + detalle);
        }
        autorizarResponsable(ctx, proyecto);

        ciclo.setCerrado("S");
        if (ciclo.getFechaCierre() == null) {
            ciclo.setFechaCierre(LocalDateTime.now());
        }
        cicloRepository.save(ciclo);

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("cicloId", String.valueOf(cicloId));
        cambios.put("proyectoId", String.valueOf(proyectoId));
        cambios.put("fechaCierre", String.valueOf(ciclo.getFechaCierre()));
        auditService.registrarExito(new AuditService.AuditCommand(
                ctx == null ? null : ctx.correlacionId(),
                ctx == null ? null : ctx.actorUsuarioId(),
                null,
                ctx == null ? null : ctx.asignacionEfectivaId(),
                ctx == null ? null : ctx.perfilEfectivo(),
                ctx == null ? null : ctx.unidadEfectivaId(),
                OP_CERRAR_CICLO,
                CONSUMIDOR,
                RECURSO_CICLO,
                cicloId,
                "SUCCESS",
                cambios,
                "INTERNO"));
    }

    // ---------------------------------------------------------------------
    // 3) Anexar version append-only
    // ---------------------------------------------------------------------

    @Override
    public CicloVersionResponse anexarVersion(long proyectoId, long cicloId,
            AnexarCicloVersionRequest request, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson) {
        if (request == null) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El cuerpo de la correccion es obligatorio.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ejecutarAnexarVersion(proyectoId, cicloId, request, ctx);
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "La anexion de version exige el cuerpo serializado para calcular el hash canonico.");
        }
        IdempotencyService.IdempotencyRequest peticion =
                new IdempotencyService.IdempotencyRequest(
                        CONSUMIDOR, OP_ANEXAR_VERSION, idempotencyKey, payloadJson,
                        contextoActorSub(ctx));
        IdempotencyService.IdempotencyResult resultado =
                idempotencyService.execute(peticion, () -> {
                    CicloVersionResponse detalle =
                            ejecutarAnexarVersion(proyectoId, cicloId, request, ctx);
                    return new IdempotencyService.IdempotencyResponse(
                            RECURSO_CICLO_VERSION, detalle.idVersion(),
                            serializarDetalleVersion(detalle));
                });
        return deserializarVersion(resultado.respuestaJson());
    }

    @Transactional
    CicloVersionResponse ejecutarAnexarVersion(long proyectoId, long cicloId,
            AnexarCicloVersionRequest request, PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = cargarProyectoEjecucion(proyectoId);
        CicloProyectoEntity ciclo = cicloRepository.findById(cicloId)
                .orElseThrow(() -> {
                    String detalle = "El ciclo " + cicloId + " no existe.";
                    registrarDenegacion(ctx, proyectoId, OP_ANEXAR_VERSION,
                            "CYCLE_NOT_FOUND", detalle,
                            Map.of("cicloId", String.valueOf(cicloId)));
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "CYCLE_NOT_FOUND: " + detalle);
                });
        if (!ciclo.getIdProyecto().equals(proyectoId)) {
            String detalle = "El ciclo " + cicloId
                    + " no pertenece al proyecto " + proyectoId;
            registrarDenegacion(ctx, proyectoId, OP_ANEXAR_VERSION,
                    "CYCLE_PROJECT_MISMATCH", detalle,
                    Map.of("cicloId", String.valueOf(cicloId),
                            "proyectoId", String.valueOf(proyectoId)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CYCLE_PROJECT_MISMATCH: " + detalle);
        }
        autorizarResponsable(ctx, proyecto);

        int nuevaVersion = ciclo.getNumeroVersion() + 1;
        CicloVersionEntity version = new CicloVersionEntity();
        version.setIdCiclo(cicloId);
        version.setNumeroVersion(nuevaVersion);
        version.setMotivo(request.motivo() == null ? null : request.motivo().trim());
        version.setObjetivos(request.objetivos() == null ? null : request.objetivos().trim());
        version.setActividades(request.actividades() == null ? null : request.actividades().trim());
        version.setAvance(request.avance());
        version.setDificultades(request.dificultades() == null ? null : request.dificultades().trim());
        version.setProximasAcciones(request.proximasAcciones() == null
                ? null : request.proximasAcciones().trim());
        version.setCreadoPor(ctx == null ? null : ctx.actorSub());
        try {
            version = cicloVersionRepository.save(version);
        } catch (DataIntegrityViolationException carrera) {
            String detalle = "Carrera concurrente al anexar version del ciclo "
                    + cicloId;
            registrarDenegacion(ctx, proyectoId, OP_ANEXAR_VERSION,
                    "CYCLE_VERSION_CONFLICT", detalle,
                    Map.of("cicloId", String.valueOf(cicloId)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CYCLE_VERSION_CONFLICT: " + detalle);
        }

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("cicloId", String.valueOf(cicloId));
        cambios.put("proyectoId", String.valueOf(proyectoId));
        cambios.put("numeroVersion", String.valueOf(nuevaVersion));
        cambios.put("idVersion", String.valueOf(version.getId()));
        auditService.registrarExito(new AuditService.AuditCommand(
                ctx == null ? null : ctx.correlacionId(),
                ctx == null ? null : ctx.actorUsuarioId(),
                null,
                ctx == null ? null : ctx.asignacionEfectivaId(),
                ctx == null ? null : ctx.perfilEfectivo(),
                ctx == null ? null : ctx.unidadEfectivaId(),
                OP_ANEXAR_VERSION,
                CONSUMIDOR,
                RECURSO_CICLO_VERSION,
                version.getId(),
                "SUCCESS",
                cambios,
                "INTERNO"));

        return mapper.toCicloVersionResponse(version);
    }

    // ---------------------------------------------------------------------
    // 4) Adjuntar evidencia documental
    // ---------------------------------------------------------------------

    @Override
    public void adjuntarEvidenciaDocumento(long proyectoId, long cicloId,
            long documentoId, String tipoDocumental,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson) {
        if (tipoDocumental == null
                || !TIPOS_EVIDENCIA_VALIDOS.contains(tipoDocumental)) {
            String detalle = "Tipo documental '" + tipoDocumental
                    + "' no es de ciclo";
            registrarDenegacion(ctx, proyectoId, OP_ADJUNTAR_EVIDENCIA,
                    "EVIDENCE_TYPE_NOT_ALLOWED", detalle,
                    Map.of("tipoDocumental",
                            tipoDocumental == null ? "" : tipoDocumental));
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "EVIDENCE_TYPE_NOT_ALLOWED: " + detalle);
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            ejecutarAdjuntarEvidencia(proyectoId, cicloId, documentoId,
                    tipoDocumental, ctx);
            return;
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "La vinculacion de evidencia exige el cuerpo serializado para calcular el hash canonico.");
        }
        IdempotencyService.IdempotencyRequest peticion =
                new IdempotencyService.IdempotencyRequest(
                        CONSUMIDOR, OP_ADJUNTAR_EVIDENCIA, idempotencyKey, payloadJson,
                        contextoActorSub(ctx));
        idempotencyService.execute(peticion, () -> {
            ejecutarAdjuntarEvidencia(proyectoId, cicloId, documentoId,
                    tipoDocumental, ctx);
            return new IdempotencyService.IdempotencyResponse(
                    RECURSO_EVIDENCIA, cicloId,
                    "{\"cicloId\":" + cicloId
                            + ",\"documentoId\":" + documentoId + "}");
        });
    }

    @Transactional
    void ejecutarAdjuntarEvidencia(long proyectoId, long cicloId,
            long documentoId, String tipoDocumental,
            PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = cargarProyectoEjecucion(proyectoId);
        CicloProyectoEntity ciclo = cicloRepository.findById(cicloId)
                .orElseThrow(() -> {
                    String detalle = "El ciclo " + cicloId + " no existe.";
                    registrarDenegacion(ctx, proyectoId, OP_ADJUNTAR_EVIDENCIA,
                            "CYCLE_NOT_FOUND", detalle,
                            Map.of("cicloId", String.valueOf(cicloId)));
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "CYCLE_NOT_FOUND: " + detalle);
                });
        if (!ciclo.getIdProyecto().equals(proyectoId)) {
            String detalle = "El ciclo " + cicloId
                    + " no pertenece al proyecto " + proyectoId;
            registrarDenegacion(ctx, proyectoId, OP_ADJUNTAR_EVIDENCIA,
                    "CYCLE_PROJECT_MISMATCH", detalle,
                    Map.of("cicloId", String.valueOf(cicloId),
                            "proyectoId", String.valueOf(proyectoId)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CYCLE_PROJECT_MISMATCH: " + detalle);
        }
        if (!aptitudDocumentalService.esApto(documentoId, tipoDocumental)) {
            String detalle = "El documento " + documentoId
                    + " no es apto como '" + tipoDocumental + "'";
            registrarDenegacion(ctx, proyectoId, OP_ADJUNTAR_EVIDENCIA,
                    "EVIDENCE_NOT_ELIGIBLE", detalle,
                    Map.of("documentoId", String.valueOf(documentoId),
                            "tipoDocumental", tipoDocumental));
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "EVIDENCE_NOT_ELIGIBLE: " + detalle);
        }
        if (cicloEvidenciaRepository.existsByIdCicloAndIdDocumento(
                cicloId, documentoId)) {
            String detalle = "El documento " + documentoId
                    + " ya esta adjunto al ciclo " + cicloId;
            registrarDenegacion(ctx, proyectoId, OP_ADJUNTAR_EVIDENCIA,
                    "EVIDENCE_ALREADY_ATTACHED", detalle,
                    Map.of("documentoId", String.valueOf(documentoId),
                            "cicloId", String.valueOf(cicloId)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "EVIDENCE_ALREADY_ATTACHED: " + detalle);
        }
        autorizarResponsable(ctx, proyecto);

        CicloEvidenciaEntity evidencia = new CicloEvidenciaEntity();
        evidencia.setIdCiclo(cicloId);
        evidencia.setIdDocumento(documentoId);
        evidencia.setCreadoPor(ctx == null ? null : ctx.actorSub());
        try {
            cicloEvidenciaRepository.save(evidencia);
        } catch (DataIntegrityViolationException carrera) {
            String detalle = "Carrera concurrente al adjuntar evidencia al ciclo "
                    + cicloId;
            registrarDenegacion(ctx, proyectoId, OP_ADJUNTAR_EVIDENCIA,
                    "EVIDENCE_ALREADY_ATTACHED", detalle,
                    Map.of("cicloId", String.valueOf(cicloId),
                            "documentoId", String.valueOf(documentoId)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "EVIDENCE_ALREADY_ATTACHED: " + detalle);
        }

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("cicloId", String.valueOf(cicloId));
        cambios.put("proyectoId", String.valueOf(proyectoId));
        cambios.put("documentoId", String.valueOf(documentoId));
        cambios.put("tipoDocumental", tipoDocumental);
        auditService.registrarExito(new AuditService.AuditCommand(
                ctx == null ? null : ctx.correlacionId(),
                ctx == null ? null : ctx.actorUsuarioId(),
                null,
                ctx == null ? null : ctx.asignacionEfectivaId(),
                ctx == null ? null : ctx.perfilEfectivo(),
                ctx == null ? null : ctx.unidadEfectivaId(),
                OP_ADJUNTAR_EVIDENCIA,
                CONSUMIDOR,
                RECURSO_EVIDENCIA,
                cicloId,
                "SUCCESS",
                cambios,
                "RESTRINGIDO"));
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private void validarPeriodo(String periodo) {
        if (periodo == null || !PERIODO_REGEX.matcher(periodo).matches()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "INVALID_PERIOD_FORMAT: el periodo debe tener formato AAAA-Qn-Sn");
        }
    }

    private RegistroPortafolioEntity cargarProyectoEjecucion(long proyectoId) {
        return registroRepository.findById(proyectoId)
                .filter(r -> r.getTipoRegistro() == TipoRegistro.PROYECTO)
                .filter(r -> r.getEstado() == EstadoIniciativa.PROYECTO_EJECUCION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "PROJECT_NOT_IN_EXECUTION: el proyecto " + proyectoId
                                + " no esta en PROYECTO_EJECUCION"));
    }

    private void autorizarResponsable(PortafolioAuthContext ctx,
            RegistroPortafolioEntity proyecto) {
        if (ctx == null || ctx.perfilEfectivo() == null
                || !PERFIL_RESPONSABLE.equals(ctx.perfilEfectivo())) {
            String detalle = "Solo el Responsable dentro de su ambito puede "
                    + "operar sobre los ciclos del proyecto; perfil efectivo: "
                    + (ctx == null ? "null" : ctx.perfilEfectivo());
            registrarDenegacion(ctx, proyecto.getId(), "CICLO_OPERACION",
                    "ASSIGNMENT_SCOPE_DENIED", detalle,
                    Map.of("perfilEfectivo",
                            ctx == null ? "" : ctx.perfilEfectivo()));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "ASSIGNMENT_SCOPE_DENIED: " + detalle);
        }
        if (autorizacionService != null
                && ctx.asignacionEfectivaId() != null
                && ctx.actorSub() != null
                && proyecto.getUnidadEjecutoraId() != null) {
            try {
                autorizacionService.revalidarParaOperacionSensible(
                        ctx.actorSub(),
                        ctx.asignacionEfectivaId(),
                        PERFIL_RESPONSABLE,
                        proyecto.getUnidadEjecutoraId());
            } catch (ResponseStatusException rse) {
                String detalle = "La revalidacion Oracle rechazo la operacion";
                registrarDenegacion(ctx, proyecto.getId(), "CICLO_OPERACION",
                        "ASSIGNMENT_SCOPE_DENIED", detalle,
                        Map.of("perfilEfectivo", ctx.perfilEfectivo()));
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "ASSIGNMENT_SCOPE_DENIED: " + detalle);
            }
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void registrarDenegacion(PortafolioAuthContext ctx, Long recursoId,
            String operacion, String codigo, String detalle,
            Map<String, String> cambios) {
        if (auditService == null) {
            return;
        }
        try {
            String correlacion = ctx == null || ctx.correlacionId() == null
                    ? "no-correlation" : ctx.correlacionId();
            Map<String, String> evidencia = new LinkedHashMap<>();
            if (cambios != null) {
                evidencia.putAll(cambios);
            }
            evidencia.put("detalle", truncar(detalle, 1000));
            auditService.registrarDenegacion(new AuditService.AuditCommand(
                    correlacion,
                    ctx == null ? null : ctx.actorUsuarioId(),
                    null,
                    ctx == null ? null : ctx.asignacionEfectivaId(),
                    ctx == null ? null : ctx.perfilEfectivo(),
                    ctx == null ? null : ctx.unidadEfectivaId(),
                    operacion,
                    CONSUMIDOR,
                    RECURSO_CICLO,
                    recursoId,
                    codigo,
                    evidencia,
                    "RESTRINGIDO"));
        } catch (RuntimeException ex) {
            LOG.warn("Fallo registrando denegacion de ciclo: {}", ex.getMessage());
        }
    }

    private static String contextoActorSub(PortafolioAuthContext ctx) {
        return ctx == null || ctx.actorSub() == null ? "unknown" : ctx.actorSub();
    }

    private static String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() > max ? valor.substring(0, max) : valor;
    }

    private String serializarDetalleCiclo(CicloResponse detalle) {
        if (objectMapper == null) {
            return "{\"idCiclo\":" + detalle.idCiclo()
                    + ",\"periodo\":\"" + detalle.periodo() + "\"}";
        }
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo serializar el detalle del ciclo para idempotencia.", e);
        }
    }

    private CicloResponse deserializarCiclo(String json) {
        if (objectMapper == null || json == null) {
            throw new IllegalStateException(
                    "No se puede deserializar la respuesta idempotente del ciclo.");
        }
        try {
            return objectMapper.readValue(json, CicloResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo deserializar la respuesta idempotente del ciclo.", e);
        }
    }

    private String serializarDetalleVersion(CicloVersionResponse detalle) {
        if (objectMapper == null) {
            return "{\"idVersion\":" + detalle.idVersion()
                    + ",\"numeroVersion\":" + detalle.numeroVersion() + "}";
        }
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo serializar el detalle de la version para idempotencia.", e);
        }
    }

    private CicloVersionResponse deserializarVersion(String json) {
        if (objectMapper == null || json == null) {
            throw new IllegalStateException(
                    "No se puede deserializar la respuesta idempotente de la version.");
        }
        try {
            return objectMapper.readValue(json, CicloVersionResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo deserializar la respuesta idempotente de la version.", e);
        }
    }
}
