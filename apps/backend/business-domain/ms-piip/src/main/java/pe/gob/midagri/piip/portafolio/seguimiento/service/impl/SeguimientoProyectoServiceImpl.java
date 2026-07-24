package pe.gob.midagri.piip.portafolio.seguimiento.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CorreccionCicloRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.EditarCamposEditablesRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PlanificacionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PlanificacionResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.CicloProyectoEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.entity.PlanificacionProyectoEntity;
import pe.gob.midagri.piip.portafolio.seguimiento.mapper.SeguimientoMapper;
import pe.gob.midagri.piip.portafolio.seguimiento.repository.CicloProyectoRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.repository.PlanificacionProyectoRepository;
import pe.gob.midagri.piip.portafolio.seguimiento.service.SeguimientoProyectoService;


/**
 * Implementacion del servicio de seguimiento del proyecto (US4,
 * Constitucion 5.0.0, DDL 015 y contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}).
 *
 * <p>Reglas canonicas aplicadas:
 * <ul>
 *   <li>Solo se permite registrar la planificacion o el primer
 *       ciclo cuando el proyecto esta en
 *       {@code PROYECTO_EJECUCION} (TIPO_REGISTRO=PROYECTO y
 *       ESTADO=PROYECTO_EJECUCION).</li>
 *   <li>La planificacion es append-only: la primera invocacion
 *       crea la version 1; las correcciones (entrega de T073)
 *       crean versiones adicionales conservando la fila
 *       anterior.</li>
 *   <li>El ciclo quincenal exige el periodo con formato
 *       {@code AAAA-Qn-Sn} (CHECK {@code CK_CP_PERIODO}) y los
 *       campos completos (objetivos, actividades, avance) para
 *       superar la regla CYCLE_INCOMPLETE.</li>
 *   <li>El avance debe estar en el rango [0, 100] validado por
 *       la CHECK {@code CK_CP_AVANCE} del DDL 015.</li>
 *   <li>La correccion de un ciclo cerrado nunca actualiza la
 *       fila original: inserta una nueva fila en
 *       {@code CICLO_PROYECTO} con {@code NUMERO_VERSION}
 *       incrementado y {@code ID_VERSION_ANTERIOR} apuntando a
 *       la fila cerrada.</li>
 *   <li>El cierre fija {@code FECHA_CIERRE} y bloquea ediciones
 *       directas; un segundo cierre se rechaza con 409
 *       {@code CYCLE_ALREADY_CLOSED}.</li>
 *   <li>Auditoria atomica de exito en la misma transaccion de
 *       negocio y de denegacion en
 *       {@code REQUIRES_NEW}.</li>
 *   <li>Autorizacion efectiva Oracle mediante
 *       {@link AutorizacionEfectivaService} cuando el bean esta
 *       disponible; en pruebas unitarias se omite sin afectar el
 *       contrato.</li>
 *   <li>Solo el {@code Responsable} titular vigente del proyecto
 *       puede operar.</li>
 * </ul>
 */
@Service
public class SeguimientoProyectoServiceImpl implements SeguimientoProyectoService {

    private static final Logger LOG =
            LoggerFactory.getLogger(SeguimientoProyectoServiceImpl.class);

    private static final String CONSUMIDOR = "PORTAFOLIO";
    private static final String RECURSO_PLANIFICACION = "PLANIFICACION_PROYECTO";
    private static final String RECURSO_CICLO = "CICLO_PROYECTO";

    private static final String OP_REGISTRAR_PLANIFICACION = "REGISTRAR_PLANIFICACION";
    private static final String OP_REGISTRAR_CICLO = "REGISTRAR_CICLO";
    private static final String OP_CORREGIR_CICLO = "CORREGIR_CICLO";
    private static final String OP_CERRAR_CICLO = "CERRAR_CICLO";
    private static final String OP_EDITAR_CAMPOS = "EDITAR_CAMPOS_EDITABLES";

    private static final String PERFIL_RESPONSABLE = "Responsable";

    private final RegistroPortafolioRepository registroRepository;
    private final PlanificacionProyectoRepository planificacionRepository;
    private final CicloProyectoRepository cicloRepository;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;
    private final SeguimientoMapper mapper;

    private AutorizacionEfectivaService autorizacionService;
    private ObjectMapper objectMapper;

    public SeguimientoProyectoServiceImpl(
            RegistroPortafolioRepository registroRepository,
            PlanificacionProyectoRepository planificacionRepository,
            CicloProyectoRepository cicloRepository,
            AuditService auditService,
            IdempotencyService idempotencyService,
            SeguimientoMapper mapper) {
        this.registroRepository = registroRepository;
        this.planificacionRepository = planificacionRepository;
        this.cicloRepository = cicloRepository;
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
    // 1) Planificacion
    // ---------------------------------------------------------------------

    @Override
    public PlanificacionResponse registrarPlanificacion(long proyectoId,
            PlanificacionRequest request, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson) {
        if (request == null) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El cuerpo de la planificacion es obligatorio.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ejecutarRegistrarPlanificacion(proyectoId, request, ctx);
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "La planificacion exige el cuerpo serializado para calcular el hash canonico.");
        }
        IdempotencyService.IdempotencyRequest peticion =
                new IdempotencyService.IdempotencyRequest(
                        CONSUMIDOR, OP_REGISTRAR_PLANIFICACION,
                        idempotencyKey, payloadJson, contextoActorSub(ctx));
        IdempotencyService.IdempotencyResult resultado =
                idempotencyService.execute(peticion, () -> {
                    PlanificacionResponse detalle =
                            ejecutarRegistrarPlanificacion(proyectoId, request, ctx);
                    return new IdempotencyService.IdempotencyResponse(
                            RECURSO_PLANIFICACION, detalle.idPlanificacion(),
                            serializarDetallePlanificacion(detalle));
                });
        return deserializarPlanificacion(resultado.respuestaJson());
    }

    @Transactional
    PlanificacionResponse ejecutarRegistrarPlanificacion(long proyectoId,
            PlanificacionRequest request, PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = cargarProyectoEjecucion(proyectoId);

        if (planificacionRepository.existsByIdProyecto(proyectoId)) {
            String detalle = "El proyecto " + proyectoId
                    + " ya tiene una planificacion registrada; use la correccion";
            registrarDenegacion(ctx, proyectoId, OP_REGISTRAR_PLANIFICACION,
                    "PLANIFICACION_ALREADY_EXISTS", detalle,
                    Map.of("proyectoId", String.valueOf(proyectoId)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "PLANIFICACION_ALREADY_EXISTS: " + detalle);
        }

        autorizarResponsable(ctx, proyecto);

        PlanificacionProyectoEntity planificacion = new PlanificacionProyectoEntity();
        planificacion.setIdProyecto(proyectoId);
        planificacion.setAlcance(request.alcance() == null ? null : request.alcance().trim());
        planificacion.setObjetivos(request.objetivos() == null ? null : request.objetivos().trim());
        planificacion.setEntregables(request.entregables());
        planificacion.setPeriodos(request.periodos());
        planificacion.setVersion(1);
        planificacion.setIdVersionAnterior(null);
        planificacion.setCerrada("N");
        planificacion.setCreadoPor(ctx == null ? null : ctx.actorSub());
        try {
            planificacion = planificacionRepository.save(planificacion);
        } catch (DataIntegrityViolationException carrera) {
            String detalle = "Carrera concurrente registrando planificacion del proyecto "
                    + proyectoId;
            registrarDenegacion(ctx, proyectoId, OP_REGISTRAR_PLANIFICACION,
                    "PLANIFICACION_ALREADY_EXISTS", detalle,
                    Map.of("proyectoId", String.valueOf(proyectoId)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "PLANIFICACION_ALREADY_EXISTS: " + detalle);
        }

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("proyectoId", String.valueOf(proyectoId));
        cambios.put("idPlanificacion", String.valueOf(planificacion.getId()));
        cambios.put("version", "1");
        auditService.registrarExito(new AuditService.AuditCommand(
                ctx == null ? null : ctx.correlacionId(),
                ctx == null ? null : ctx.actorUsuarioId(),
                null,
                ctx == null ? null : ctx.asignacionEfectivaId(),
                ctx == null ? null : ctx.perfilEfectivo(),
                ctx == null ? null : ctx.unidadEfectivaId(),
                OP_REGISTRAR_PLANIFICACION,
                CONSUMIDOR,
                RECURSO_PLANIFICACION,
                planificacion.getId(),
                "SUCCESS",
                cambios,
                "INTERNO"));

        return mapper.toPlanificacionResponse(planificacion);
    }

    // ---------------------------------------------------------------------
    // 2) Registrar ciclo
    // ---------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<PlanificacionResponse> listarPlanificaciones(long proyectoId,
            PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = cargarProyecto(proyectoId);
        autorizarResponsable(ctx, proyecto);
        return planificacionRepository.findByIdProyectoOrderByVersionAsc(proyectoId).stream()
                .map(mapper::toPlanificacionResponse)
                .toList();
    }

    @Override
    public CicloResponse registrarCiclo(long proyectoId, CicloRequest request,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson) {
        if (request == null) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El cuerpo del ciclo es obligatorio.");
        }
        if (request.objetivos() == null || request.objetivos().isBlank()
                || request.actividades() == null || request.actividades().isBlank()
                || request.avance() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "CYCLE_INCOMPLETE: el ciclo debe incluir objetivos, "
                            + "actividades y avance");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ejecutarRegistrarCiclo(proyectoId, request, ctx);
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "El registro del ciclo exige el cuerpo serializado para calcular el hash canonico.");
        }
        IdempotencyService.IdempotencyRequest peticion =
                new IdempotencyService.IdempotencyRequest(
                        CONSUMIDOR, OP_REGISTRAR_CICLO, idempotencyKey, payloadJson,
                        contextoActorSub(ctx));
        IdempotencyService.IdempotencyResult resultado =
                idempotencyService.execute(peticion, () -> {
                    CicloResponse detalle =
                            ejecutarRegistrarCiclo(proyectoId, request, ctx);
                    return new IdempotencyService.IdempotencyResponse(
                            RECURSO_CICLO, detalle.idCiclo(),
                            serializarDetalleCiclo(detalle));
                });
        return deserializarCiclo(resultado.respuestaJson());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CicloResponse> listarCiclos(long proyectoId, PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = cargarProyecto(proyectoId);
        autorizarResponsable(ctx, proyecto);

        Map<String, CicloProyectoEntity> versionActualPorPeriodo = new LinkedHashMap<>();
        for (CicloProyectoEntity ciclo : cicloRepository
                .findByIdProyectoOrderByPeriodoAscNumeroVersionAsc(proyectoId)) {
            versionActualPorPeriodo.put(ciclo.getPeriodo(), ciclo);
        }
        return versionActualPorPeriodo.values().stream()
                .map(mapper::toCicloResponse)
                .toList();
    }

    @Transactional
    CicloResponse ejecutarRegistrarCiclo(long proyectoId, CicloRequest request,
            PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = cargarProyectoEjecucion(proyectoId);
        autorizarResponsable(ctx, proyecto);

        CicloProyectoEntity ciclo = new CicloProyectoEntity();
        ciclo.setIdProyecto(proyectoId);
        ciclo.setPeriodo(request.periodo());
        ciclo.setNumeroVersion(1);
        ciclo.setIdVersionAnterior(null);
        ciclo.setObjetivos(request.objetivos() == null ? null : request.objetivos().trim());
        ciclo.setActividades(request.actividades() == null ? null : request.actividades().trim());
        ciclo.setAvance(request.avance());
        ciclo.setDificultades(request.dificultades() == null ? null : request.dificultades().trim());
        ciclo.setProximasAcciones(request.proximasAcciones() == null
                ? null : request.proximasAcciones().trim());
        ciclo.setCerrado("N");
        ciclo.setCreadoPor(ctx == null ? null : ctx.actorSub());
        try {
            ciclo = cicloRepository.save(ciclo);
        } catch (DataIntegrityViolationException carrera) {
            String detalle = "Carrera concurrente registrando ciclo del proyecto "
                    + proyectoId + " para el periodo " + request.periodo();
            registrarDenegacion(ctx, proyectoId, OP_REGISTRAR_CICLO,
                    "CYCLE_DUPLICATED", detalle,
                    Map.of("proyectoId", String.valueOf(proyectoId),
                            "periodo", request.periodo()));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CYCLE_DUPLICATED: " + detalle);
        }

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("proyectoId", String.valueOf(proyectoId));
        cambios.put("cicloId", String.valueOf(ciclo.getId()));
        cambios.put("periodo", request.periodo());
        cambios.put("numeroVersion", "1");
        auditService.registrarExito(new AuditService.AuditCommand(
                ctx == null ? null : ctx.correlacionId(),
                ctx == null ? null : ctx.actorUsuarioId(),
                null,
                ctx == null ? null : ctx.asignacionEfectivaId(),
                ctx == null ? null : ctx.perfilEfectivo(),
                ctx == null ? null : ctx.unidadEfectivaId(),
                OP_REGISTRAR_CICLO,
                CONSUMIDOR,
                RECURSO_CICLO,
                ciclo.getId(),
                "SUCCESS",
                cambios,
                "INTERNO"));

        return mapper.toCicloResponse(ciclo);
    }

    // ---------------------------------------------------------------------
    // 3) Corregir ciclo
    // ---------------------------------------------------------------------

    @Override
    public CicloResponse corregirCiclo(long proyectoId, long cicloId,
            CorreccionCicloRequest request, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson) {
        if (request == null) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El cuerpo de la correccion es obligatorio.");
        }
        if (request.objetivos() == null || request.objetivos().isBlank()
                || request.actividades() == null || request.actividades().isBlank()
                || request.avance() == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "CYCLE_INCOMPLETE: el ciclo debe incluir objetivos, "
                            + "actividades y avance");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ejecutarCorregirCiclo(proyectoId, cicloId, request, ctx);
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "La correccion del ciclo exige el cuerpo serializado para calcular el hash canonico.");
        }
        IdempotencyService.IdempotencyRequest peticion =
                new IdempotencyService.IdempotencyRequest(
                        CONSUMIDOR, OP_CORREGIR_CICLO, idempotencyKey, payloadJson,
                        contextoActorSub(ctx));
        IdempotencyService.IdempotencyResult resultado =
                idempotencyService.execute(peticion, () -> {
                    CicloResponse detalle =
                            ejecutarCorregirCiclo(proyectoId, cicloId, request, ctx);
                    return new IdempotencyService.IdempotencyResponse(
                            RECURSO_CICLO, detalle.idCiclo(),
                            serializarDetalleCiclo(detalle));
                });
        return deserializarCiclo(resultado.respuestaJson());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CicloResponse> listarVersionesCiclo(long proyectoId, long cicloId,
            PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = cargarProyecto(proyectoId);
        autorizarResponsable(ctx, proyecto);

        List<CicloProyectoEntity> ciclos = cicloRepository
                .findByIdProyectoOrderByPeriodoAscNumeroVersionAsc(proyectoId);
        Map<Long, CicloProyectoEntity> porId = new HashMap<>();
        for (CicloProyectoEntity ciclo : ciclos) {
            porId.put(ciclo.getId(), ciclo);
        }
        CicloProyectoEntity solicitado = porId.get(cicloId);
        if (solicitado == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "CYCLE_NOT_FOUND: el ciclo no pertenece al proyecto");
        }
        Long raizId = solicitado.getId();
        while (porId.get(raizId).getIdVersionAnterior() != null) {
            raizId = porId.get(raizId).getIdVersionAnterior();
        }
        List<CicloResponse> resultado = new ArrayList<>();
        Long versionId = raizId;
        while (versionId != null) {
            CicloProyectoEntity version = porId.get(versionId);
            if (version == null) {
                break;
            }
            resultado.add(mapper.toCicloResponse(version));
            versionId = ciclos.stream()
                    .filter(ciclo -> version.getId().equals(ciclo.getIdVersionAnterior()))
                    .map(CicloProyectoEntity::getId)
                    .findFirst()
                    .orElse(null);
        }
        return resultado;
    }

    @Transactional
    CicloResponse ejecutarCorregirCiclo(long proyectoId, long cicloId,
            CorreccionCicloRequest request, PortafolioAuthContext ctx) {
        RegistroPortafolioEntity proyecto = cargarProyectoEjecucion(proyectoId);
        CicloProyectoEntity cicloActual = cicloRepository.findById(cicloId)
                .orElseThrow(() -> {
                    String detalle = "El ciclo " + cicloId + " no existe.";
                    registrarDenegacion(ctx, proyectoId, OP_CORREGIR_CICLO,
                            "CYCLE_NOT_FOUND", detalle,
                            Map.of("cicloId", String.valueOf(cicloId)));
                    return new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "CYCLE_NOT_FOUND: " + detalle);
                });
        if (!cicloActual.getIdProyecto().equals(proyectoId)) {
            String detalle = "El ciclo " + cicloId
                    + " no pertenece al proyecto " + proyectoId;
            registrarDenegacion(ctx, proyectoId, OP_CORREGIR_CICLO,
                    "CYCLE_PROJECT_MISMATCH", detalle,
                    Map.of("cicloId", String.valueOf(cicloId),
                            "proyectoId", String.valueOf(proyectoId)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CYCLE_PROJECT_MISMATCH: " + detalle);
        }
        autorizarResponsable(ctx, proyecto);

        // La correccion es append-only: insertamos una nueva fila
        // en CICLO_PROYECTO con NUMERO_VERSION incrementado y
        // referencia a la fila original; nunca modificamos la fila
        // original.
        int nuevaVersion = cicloActual.getNumeroVersion() + 1;
        CicloProyectoEntity nuevaFila = new CicloProyectoEntity();
        nuevaFila.setIdProyecto(proyectoId);
        nuevaFila.setPeriodo(cicloActual.getPeriodo());
        nuevaFila.setNumeroVersion(nuevaVersion);
        nuevaFila.setIdVersionAnterior(cicloActual.getId());
        nuevaFila.setObjetivos(request.objetivos() == null ? null : request.objetivos().trim());
        nuevaFila.setActividades(request.actividades() == null ? null : request.actividades().trim());
        nuevaFila.setAvance(request.avance());
        nuevaFila.setDificultades(request.dificultades() == null ? null : request.dificultades().trim());
        nuevaFila.setProximasAcciones(request.proximasAcciones() == null
                ? null : request.proximasAcciones().trim());
        nuevaFila.setCerrado("N");
        nuevaFila.setCreadoPor(ctx == null ? null : ctx.actorSub());
        try {
            nuevaFila = cicloRepository.save(nuevaFila);
        } catch (DataIntegrityViolationException carrera) {
            String detalle = "Carrera concurrente al corregir el ciclo " + cicloId;
            registrarDenegacion(ctx, proyectoId, OP_CORREGIR_CICLO,
                    "CYCLE_VERSION_CONFLICT", detalle,
                    Map.of("cicloId", String.valueOf(cicloId)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CYCLE_VERSION_CONFLICT: " + detalle);
        }

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("proyectoId", String.valueOf(proyectoId));
        cambios.put("cicloIdOriginal", String.valueOf(cicloActual.getId()));
        cambios.put("cicloIdNuevaVersion", String.valueOf(nuevaFila.getId()));
        cambios.put("motivo", request.motivo());
        cambios.put("numeroVersion", String.valueOf(nuevaVersion));
        auditService.registrarExito(new AuditService.AuditCommand(
                ctx == null ? null : ctx.correlacionId(),
                ctx == null ? null : ctx.actorUsuarioId(),
                null,
                ctx == null ? null : ctx.asignacionEfectivaId(),
                ctx == null ? null : ctx.perfilEfectivo(),
                ctx == null ? null : ctx.unidadEfectivaId(),
                OP_CORREGIR_CICLO,
                CONSUMIDOR,
                RECURSO_CICLO,
                nuevaFila.getId(),
                "SUCCESS",
                cambios,
                "INTERNO"));

        return mapper.toCicloResponse(nuevaFila);
    }

    // ---------------------------------------------------------------------
    // 4) Cerrar ciclo
    // ---------------------------------------------------------------------

    @Override
    public void cerrarCiclo(long proyectoId, long cicloId,
            PortafolioAuthContext ctx, String idempotencyKey, String payloadJson) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            ejecutarCerrarCiclo(proyectoId, cicloId, ctx);
            return;
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "El cierre del ciclo exige el cuerpo serializado para calcular el hash canonico.");
        }
        IdempotencyService.IdempotencyRequest peticion =
                new IdempotencyService.IdempotencyRequest(
                        CONSUMIDOR, OP_CERRAR_CICLO, idempotencyKey, payloadJson,
                        contextoActorSub(ctx));
        idempotencyService.execute(peticion, () -> {
            ejecutarCerrarCiclo(proyectoId, cicloId, ctx);
            return new IdempotencyService.IdempotencyResponse(
                    RECURSO_CICLO, cicloId, "{\"cicloId\":" + cicloId + "}");
        });
    }

    @Transactional
    void ejecutarCerrarCiclo(long proyectoId, long cicloId,
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
        cambios.put("proyectoId", String.valueOf(proyectoId));
        cambios.put("cicloId", String.valueOf(cicloId));
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
    // 5) Editar campos editables (T073, contrato estabilizador)
    // ---------------------------------------------------------------------

    @Override
    public PlanificacionResponse editarCamposEditables(long proyectoId,
            EditarCamposEditablesRequest request, PortafolioAuthContext ctx,
            String idempotencyKey, String payloadJson) {
        // La implementacion completa se entrega en T073. Para
        // mantener la firma del servicio estable en el
        // SeguimientoController, este metodo documenta
        // explicitamente que aun no esta implementado.
        throw new UnsupportedOperationException(
                "editarCamposEditables pendiente de implementacion en T073");
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    private RegistroPortafolioEntity cargarProyectoEjecucion(long proyectoId) {
        return registroRepository.findById(proyectoId)
                .filter(r -> r.getTipoRegistro() == TipoRegistro.PROYECTO)
                .filter(r -> r.getEstado() == EstadoIniciativa.PROYECTO_EJECUCION)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT,
                        "PROJECT_NOT_IN_EXECUTION: el proyecto " + proyectoId
                                + " no esta en PROYECTO_EJECUCION"));
    }

    private RegistroPortafolioEntity cargarProyecto(long proyectoId) {
        return registroRepository.findById(proyectoId)
                .filter(r -> r.getTipoRegistro() == TipoRegistro.PROYECTO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "PROJECT_NOT_FOUND: el proyecto " + proyectoId + " no existe"));
    }

    private void autorizarResponsable(PortafolioAuthContext ctx,
            RegistroPortafolioEntity proyecto) {
        if (ctx == null || ctx.perfilEfectivo() == null
                || !PERFIL_RESPONSABLE.equals(ctx.perfilEfectivo())) {
            String detalle = "Solo el Responsable dentro de su ambito puede "
                    + "operar sobre el seguimiento del proyecto; perfil efectivo: "
                    + (ctx == null ? "null" : ctx.perfilEfectivo());
            registrarDenegacion(ctx, proyecto.getId(), "SEGUIMIENTO_OPERACION",
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
                registrarDenegacion(ctx, proyecto.getId(), "SEGUIMIENTO_OPERACION",
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
            LOG.warn("Fallo registrando denegacion de seguimiento: {}", ex.getMessage());
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

    private String serializarDetallePlanificacion(PlanificacionResponse detalle) {
        if (objectMapper == null) {
            return "{\"idPlanificacion\":" + detalle.idPlanificacion()
                    + ",\"version\":" + detalle.version() + "}";
        }
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo serializar el detalle de la planificacion para idempotencia.", e);
        }
    }

    private PlanificacionResponse deserializarPlanificacion(String json) {
        if (objectMapper == null || json == null) {
            throw new IllegalStateException(
                    "No se puede deserializar la respuesta idempotente de la planificacion.");
        }
        try {
            return objectMapper.readValue(json, PlanificacionResponse.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo deserializar la respuesta idempotente de la planificacion.", e);
        }
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
}
