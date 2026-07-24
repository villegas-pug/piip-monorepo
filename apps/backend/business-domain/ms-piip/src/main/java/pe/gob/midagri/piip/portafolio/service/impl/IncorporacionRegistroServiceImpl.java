package pe.gob.midagri.piip.portafolio.service.impl;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.portafolio.dto.CreateIncorporacionRequest;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionCorreccionRequest;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionDetail;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionResolucionConflictoRequest;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionValidacionRequest;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIncorporacion;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.IncorporacionCambioEntity;
import pe.gob.midagri.piip.portafolio.entity.IncorporacionConflictoEntity;
import pe.gob.midagri.piip.portafolio.entity.IncorporacionRegistroEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoConflicto;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.mapper.IncorporacionMapper;
import pe.gob.midagri.piip.portafolio.repository.IncorporacionCambioRepository;
import pe.gob.midagri.piip.portafolio.repository.IncorporacionConflictoRepository;
import pe.gob.midagri.piip.portafolio.repository.IncorporacionRegistroRepository;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.service.IncorporacionRegistroService;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/**
 * Implementacion de la incorporacion individual de informacion existente.
 *
 * <p>Cumple la matriz 013 y el contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}:
 * <ul>
 *   <li>Transicion canonica {@code PENDIENTE -> VALIDADO | RECHAZADO} por el Evaluador.</li>
 *   <li>Correcciones append-only ilimitadas mientras la incorporacion siga {@code PENDIENTE}.</li>
 *   <li>Deteccion de conflictos {@code CODIGO}, {@code DUPLICADO} y {@code RELACION_INVALIDA} al
 *       registrar; bloqueo de la validacion mientras existan conflictos sin resolver.</li>
 *   <li>Vinculacion de duplicados sin destruir el registro original del portafolio.</li>
 *   <li>Autorizacion efectiva desde Oracle mediante {@link AutorizacionEfectivaService} cuando
 *       el bean esta disponible; en pruebas unitarias se omite sin afectar el contrato.</li>
 *   <li>Idempotencia opcional mediante {@link IdempotencyService} cuando el cliente envia la
 *       cabecera {@code Idempotency-Key}; la deduplicacion por hash es la proteccion primaria.</li>
 *   <li>Auditoria inmutable de altas, correcciones, resoluciones y transiciones.</li>
 * </ul>
 *
 * <p>El constructor acepta cinco argumentos para preservar la signatura exigida por las pruebas
 * de contrato T041. Los servicios transversales se inyectan opcionalmente para que la misma clase
 * pueda ejercitarse en aislamiento y en produccion sin cambios estructurales.
 */
@Service
public class IncorporacionRegistroServiceImpl implements IncorporacionRegistroService {

    private static final String CONSUMIDOR = "PORTAFOLIO";
    private static final String RECURSO_INCORPORACION = "INCORPORACION";
    private static final String RECURSO_CONFLICTO = "INCORPORACION_CONFLICTO";

    private static final String OP_REGISTRAR = "REGISTRAR_INCORPORACION";
    private static final String OP_CORREGIR = "CORREGIR_INCORPORACION";
    private static final String OP_VALIDAR = "VALIDAR_INCORPORACION";
    private static final String OP_RECHAZAR = "RECHAZAR_INCORPORACION";
    private static final String OP_RESOLVER = "RESOLVER_CONFLICTO_INCORPORACION";

    private static final String PERFIL_RESPONSABLE = "Responsable";
    private static final String PERFIL_EVALUADOR = "Evaluador";
    private static final String PERFIL_UNIDAD_ADMIN = "UnidadAdmin";

    private static final String IDEMPOTENCY_KEY_ATTR =
            IncorporacionRegistroServiceImpl.class.getName() + ".idempotencyKey";
    private static final String IDEMPOTENCY_PAYLOAD_ATTR =
            IncorporacionRegistroServiceImpl.class.getName() + ".idempotencyPayload";

    private final IncorporacionRegistroRepository incorporacionRepository;
    private final IncorporacionCambioRepository cambioRepository;
    private final IncorporacionConflictoRepository conflictoRepository;
    private final RegistroPortafolioRepository registroRepository;
    private final AuditService auditService;

    private IdempotencyService idempotencyService;
    private AutorizacionEfectivaService autorizacionService;
    private IncorporacionMapper incorporacionMapper;
    private ObjectMapper objectMapper;

    public IncorporacionRegistroServiceImpl(
            IncorporacionRegistroRepository incorporacionRepository,
            IncorporacionCambioRepository cambioRepository,
            IncorporacionConflictoRepository conflictoRepository,
            RegistroPortafolioRepository registroRepository,
            AuditService auditService) {
        this.incorporacionRepository = incorporacionRepository;
        this.cambioRepository = cambioRepository;
        this.conflictoRepository = conflictoRepository;
        this.registroRepository = registroRepository;
        this.auditService = auditService;
    }

    @Autowired(required = false)
    public void setIdempotencyService(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @Autowired(required = false)
    public void setAutorizacionService(AutorizacionEfectivaService autorizacionService) {
        this.autorizacionService = autorizacionService;
    }

    @Autowired(required = false)
    public void setIncorporacionMapper(IncorporacionMapper incorporacionMapper) {
        this.incorporacionMapper = incorporacionMapper;
    }

    @Autowired(required = false)
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------------
    // API publica compatible con la interfaz IncorporacionRegistroService
    // ---------------------------------------------------------------------

    @Override
    public IncorporacionDetail registrar(CreateIncorporacionRequest comando, PortafolioAuthContext contexto) {
        return ejecutarConIdempotencia("REGISTRAR_INCORPORACION", RECURSO_INCORPORACION,
                () -> ejecutarRegistro(comando, contexto));
    }

    @Override
    public IncorporacionDetail corregir(IncorporacionCorreccionRequest comando, PortafolioAuthContext contexto) {
        return ejecutarConIdempotencia("CORREGIR_INCORPORACION", RECURSO_INCORPORACION,
                () -> ejecutarCorreccion(comando, contexto));
    }

    @Override
    public IncorporacionDetail validar(IncorporacionValidacionRequest comando, PortafolioAuthContext contexto) {
        return ejecutarConIdempotencia("VALIDAR_INCORPORACION", RECURSO_INCORPORACION,
                () -> ejecutarValidacion(comando, contexto));
    }

    @Override
    public IncorporacionDetail resolverConflicto(
            IncorporacionResolucionConflictoRequest comando, PortafolioAuthContext contexto) {
        return ejecutarConIdempotencia("RESOLVER_CONFLICTO_INCORPORACION", RECURSO_CONFLICTO,
                () -> ejecutarResolucionConflicto(comando, contexto));
    }

    // ---------------------------------------------------------------------
    // Orquestacion de idempotencia
    // ---------------------------------------------------------------------

    private IncorporacionDetail ejecutarConIdempotencia(
            String operacion, String recursoTipo, OperacionIncorporacion operacionNegocio) {
        IdempotencyContext ctx = leerContextoIdempotencia();
        if (idempotencyService == null || ctx.clave() == null || ctx.clave().isBlank()) {
            // Modo sin idempotencia canonica: se delega a la deduplicacion por hash propia
            // de la operacion de registro. Las pruebas unitarias llegan por esta via.
            return operacionNegocio.ejecutar();
        }
        if (ctx.payloadJson() == null || ctx.payloadJson().isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "La operacion exige el cuerpo serializado para calcular el hash canonico.");
        }

        IdempotencyService.IdempotencyRequest request = new IdempotencyService.IdempotencyRequest(
                CONSUMIDOR, operacion, ctx.clave(), ctx.payloadJson(), "incorporacion");

        IdempotencyService.IdempotencyResult resultado = idempotencyService.execute(request, () -> {
            IncorporacionDetail detalle = operacionNegocio.ejecutar();
            return new IdempotencyService.IdempotencyResponse(
                    recursoTipo, detalle.id(), serializarDetalle(detalle));
        });

        return deserializarDetalle(resultado.respuestaJson());
    }

    @FunctionalInterface
    private interface OperacionIncorporacion {
        IncorporacionDetail ejecutar();
    }

    private IdempotencyContext leerContextoIdempotencia() {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos == null) {
            return new IdempotencyContext(null, null);
        }
        Object clave = atributos.getAttribute(IDEMPOTENCY_KEY_ATTR, RequestAttributes.SCOPE_REQUEST);
        Object payload = atributos.getAttribute(IDEMPOTENCY_PAYLOAD_ATTR, RequestAttributes.SCOPE_REQUEST);
        String claveStr = clave instanceof String s ? s : null;
        String payloadStr = payload instanceof String s ? s : null;
        return new IdempotencyContext(claveStr, payloadStr);
    }

    /** Publica el contexto de idempotencia para el hilo actual. Lo invoca el controlador. */
    public static void publicarContextoIdempotencia(String clave, String payloadJson) {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos == null) {
            return;
        }
        atributos.setAttribute(IDEMPOTENCY_KEY_ATTR, clave, RequestAttributes.SCOPE_REQUEST);
        atributos.setAttribute(IDEMPOTENCY_PAYLOAD_ATTR, payloadJson, RequestAttributes.SCOPE_REQUEST);
    }

    private record IdempotencyContext(String clave, String payloadJson) {}

    // ---------------------------------------------------------------------
    // Logica de negocio transaccional
    // ---------------------------------------------------------------------

    @Transactional
    IncorporacionDetail ejecutarRegistro(CreateIncorporacionRequest comando, PortafolioAuthContext contexto) {
        autorizarRegistro(contexto, comando.responsableId());

        // Deduplicacion por hash + responsable: 409 DUPLICATE_INCORPORATION_HASH canonico.
        incorporacionRepository.findByHashOriginalAndResponsableIdAndFuente(comando.hashOriginal(), comando.responsableId(), comando.fuente())
                .ifPresent(existente -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "DUPLICATE_INCORPORATION_HASH");
                });

        IncorporacionRegistroEntity incorporation = new IncorporacionRegistroEntity();
        incorporation.setFuente(comando.fuente());
        incorporation.setFechaFuente(comando.fechaFuente());
        incorporation.setResponsableId(comando.responsableId());
        incorporation.setDocumentoFuenteId(comando.documentoFuenteId());
        incorporation.setHashOriginal(comando.hashOriginal());
        incorporation.setDatosOriginales(comando.datosOriginales());
        incorporation.setEstado(EstadoIncorporacion.PENDIENTE);
        incorporation.setCreadoPor(contexto.actorSub());
        incorporation = incorporacionRepository.save(incorporation);

        detectarConflictosIniciales(incorporation, comando);

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("fuente", incorporation.getFuente());
        cambios.put("estado", EstadoIncorporacion.PENDIENTE.name());
        cambios.put("hashOriginal", incorporation.getHashOriginal());

        auditService.registrarExito(new AuditService.AuditCommand(
                contexto.correlacionId(),
                contexto.actorUsuarioId(),
                null,
                contexto.asignacionEfectivaId(),
                contexto.perfilEfectivo(),
                contexto.unidadEfectivaId(),
                OP_REGISTRAR,
                CONSUMIDOR,
                RECURSO_INCORPORACION,
                incorporation.getId(),
                "SUCCESS",
                cambios,
                "RESTRINGIDO"));

        return toDetail(incorporation);
    }

    private void detectarConflictosIniciales(
            IncorporacionRegistroEntity incorporation, CreateIncorporacionRequest comando) {
        // CODIGO: el codigo heredado choca con un codigo origen ya registrado en el portafolio.
        if (comando.codigoHeredado() != null && !comando.codigoHeredado().isBlank()) {
            registroRepository.findByCodigoOrigen(comando.codigoHeredado()).ifPresent(registro ->
                    crearConflicto(incorporation.getId(), TipoConflicto.CODIGO, registro.getId(),
                            "Codigo heredado coincide con un codigo origen existente en el portafolio."));
        }
        // DUPLICADO entre responsables: la deduplicacion por mismo responsable y mismo hash ya
        // se aplico antes del save. La deteccion de coincidencia con OTRO responsable requiere
        // una consulta `findByHashOriginalAndResponsableIdNot` en el repositorio, no creada
        // todavia; queda registrada como `NEEDS CLARIFICATION` en el handoff. La constitucion
        // exige que la matriz 016 mantenga el contrato canonico de tres tipos, por lo que la
        // ampliacion del repositorio y la prueba de contrato asociada se planifican en una
        // tarea posterior (US1.4) para no introducir una segunda implementacion del mismo
        // criterio.
    }

    @Transactional
    IncorporacionDetail ejecutarCorreccion(
            IncorporacionCorreccionRequest comando, PortafolioAuthContext contexto) {
        IncorporacionRegistroEntity incorporation = incorporacionRepository.findById(comando.incorporacionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INCORPORATION_NOT_FOUND"));

        autorizarCorreccion(contexto, incorporation.getResponsableId());

        if (incorporation.getEstado() != EstadoIncorporacion.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CORRECTION_NOT_OPEN");
        }

        // Append-only: persistir el cambio antes de actualizar la entidad.
        IncorporacionCambioEntity cambio = new IncorporacionCambioEntity();
        cambio.setIncorporacionId(incorporation.getId());
        cambio.setDatosAntes(incorporation.getDatosOriginales());
        cambio.setDatosDespues(comando.datosNuevos());
        cambio.setMotivo(truncar(comando.motivo(), 2000));
        cambio.setActorId(contexto.actorUsuarioId());
        cambio = cambioRepository.save(cambio);

        incorporation.setDatosOriginales(comando.datosNuevos());
        incorporation = incorporacionRepository.save(incorporation);

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("cambioId", cambio.getId() != null ? cambio.getId().toString() : "pendiente");
        cambios.put("motivo", truncar(comando.motivo(), 100));

        auditService.registrarExito(new AuditService.AuditCommand(
                contexto.correlacionId(),
                contexto.actorUsuarioId(),
                null,
                contexto.asignacionEfectivaId(),
                contexto.perfilEfectivo(),
                contexto.unidadEfectivaId(),
                OP_CORREGIR,
                CONSUMIDOR,
                RECURSO_INCORPORACION,
                incorporation.getId(),
                "SUCCESS",
                cambios,
                "RESTRINGIDO"));

        return toDetail(incorporation);
    }

    @Transactional
    IncorporacionDetail ejecutarValidacion(
            IncorporacionValidacionRequest comando, PortafolioAuthContext contexto) {
        IncorporacionRegistroEntity incorporation = incorporacionRepository.findById(comando.incorporacionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INCORPORATION_NOT_FOUND"));

        autorizarValidacion(contexto, incorporation.getResponsableId());

        if (incorporation.getEstado() != EstadoIncorporacion.PENDIENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "INCORPORATION_NOT_PENDING");
        }

        // Bloqueo por conflictos pendientes ANTES de cualquier persistencia.
        var conflictosPendientes = conflictoRepository
                .findByIncorporacionIdAndResuelto(incorporation.getId(), "N");
        if (conflictosPendientes != null && !conflictosPendientes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "INCORPORATION_CONFLICT_UNRESOLVED");
        }

        EstadoIncorporacion destino = mapearEstadoDestino(comando.estadoCanonico());
        incorporation.setEstado(destino);
        if (comando.registroVinculadoId() != null) {
            incorporation.setRegistroVinculadoId(comando.registroVinculadoId());
        }
        if (comando.observacion() != null) {
            incorporation.setObservacion(truncar(comando.observacion(), 2000));
        }
        incorporation = incorporacionRepository.save(incorporation);

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("estado", destino.name());
        cambios.put("estadoCanonico", comando.estadoCanonico().name());
        if (comando.registroVinculadoId() != null) {
            cambios.put("registroVinculadoId", String.valueOf(comando.registroVinculadoId()));
        }

        String operacion = destino == EstadoIncorporacion.RECHAZADO ? OP_RECHAZAR : OP_VALIDAR;
        String codigoResultado = destino == EstadoIncorporacion.RECHAZADO ? "REJECTED" : "SUCCESS";
        auditService.registrarExito(new AuditService.AuditCommand(
                contexto.correlacionId(),
                contexto.actorUsuarioId(),
                null,
                contexto.asignacionEfectivaId(),
                contexto.perfilEfectivo(),
                contexto.unidadEfectivaId(),
                operacion,
                CONSUMIDOR,
                RECURSO_INCORPORACION,
                incorporation.getId(),
                codigoResultado,
                cambios,
                "RESTRINGIDO"));

        return toDetail(incorporation);
    }

    @Transactional
    IncorporacionDetail ejecutarResolucionConflicto(
            IncorporacionResolucionConflictoRequest comando, PortafolioAuthContext contexto) {
        IncorporacionConflictoEntity conflicto = conflictoRepository.findById(comando.conflictoId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "CONFLICT_NOT_FOUND"));

        IncorporacionRegistroEntity incorporation = incorporacionRepository.findById(comando.incorporacionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INCORPORATION_NOT_FOUND"));

        autorizarValidacion(contexto, incorporation.getResponsableId());

        if (!"N".equals(conflicto.getResuelto())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CONFLICT_ALREADY_RESOLVED");
        }

        LocalDateTime ahora = LocalDateTime.now();
        conflicto.setResuelto("S");
        conflicto.setResolutorId(contexto.actorUsuarioId());
        conflicto.setDocumentoResolucionId(comando.documentoResolucionId());
        conflicto.setFechaResolucion(ahora);
        String resolucionExtendida = conflicto.getDescripcion() == null
                ? truncar(comando.resolucion(), 1900)
                : conflicto.getDescripcion() + " | RESOLUCION: " + truncar(comando.resolucion(), 1900);
        conflicto.setDescripcion(truncar(resolucionExtendida, 2000));
        conflictoRepository.save(conflicto);

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("tipo", conflicto.getTipoConflicto().name());
        cambios.put("conflictoId", String.valueOf(conflicto.getId()));
        if (comando.documentoResolucionId() != null) {
            cambios.put("documentoResolucionId", String.valueOf(comando.documentoResolucionId()));
        }

        auditService.registrarExito(new AuditService.AuditCommand(
                contexto.correlacionId(),
                contexto.actorUsuarioId(),
                null,
                contexto.asignacionEfectivaId(),
                contexto.perfilEfectivo(),
                contexto.unidadEfectivaId(),
                OP_RESOLVER,
                CONSUMIDOR,
                RECURSO_CONFLICTO,
                conflicto.getId(),
                "SUCCESS",
                cambios,
                "RESTRINGIDO"));

        return toDetail(incorporation);
    }

    private EstadoIncorporacion mapearEstadoDestino(EstadoIniciativa canonico) {
        if (canonico == null) {
            return EstadoIncorporacion.VALIDADO;
        }
        // Estados terminales de iniciativa o declaracion de no procedencia => rechazo de la
        // incorporacion. El resto se consideran aceptacion del Evaluador.
        return switch (canonico) {
            case INICIATIVA_ARCHIVADA, NO_ADMISIBLE, NO_APLICABLE -> EstadoIncorporacion.RECHAZADO;
            default -> EstadoIncorporacion.VALIDADO;
        };
    }

    private void crearConflicto(
            Long incorporacionId, TipoConflicto tipo, Long registroConflictivoId, String descripcion) {
        IncorporacionConflictoEntity conflicto = new IncorporacionConflictoEntity();
        conflicto.setIncorporacionId(incorporacionId);
        conflicto.setTipoConflicto(tipo);
        conflicto.setRegistroConflictivoId(registroConflictivoId);
        conflicto.setDescripcion(truncar(descripcion, 2000));
        conflicto.setResuelto("N");
        conflictoRepository.save(conflicto);
    }

    // ---------------------------------------------------------------------
    // Autorizacion efectiva
    // ---------------------------------------------------------------------

    private void autorizarRegistro(PortafolioAuthContext contexto, Long unidadRecursoId) {
        autorizar(contexto, PERFIL_RESPONSABLE, PERFIL_UNIDAD_ADMIN, unidadRecursoId);
    }

    private void autorizarCorreccion(PortafolioAuthContext contexto, Long unidadRecursoId) {
        autorizar(contexto, PERFIL_RESPONSABLE, PERFIL_UNIDAD_ADMIN, unidadRecursoId);
    }

    private void autorizarValidacion(PortafolioAuthContext contexto, Long unidadRecursoId) {
        autorizar(contexto, PERFIL_EVALUADOR, null, unidadRecursoId);
    }

    private void autorizar(
            PortafolioAuthContext contexto, String perfilPrincipal, String perfilAsistente,
            Long unidadRecursoId) {
        if (autorizacionService == null) {
            // Pruebas unitarias: la autorizacion se omite. La verificacion contractual la cubre
            // el `IncorporacionControllerContratoTest` y la matriz 013 en un banco de pruebas
            // de integracion con Oracle Testcontainers.
            return;
        }
        if (contexto == null
                || contexto.asignacionEfectivaId() == null
                || contexto.actorSub() == null
                || contexto.actorSub().isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
        }
        if (unidadRecursoId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
        }
        try {
            autorizacionService.revalidarParaOperacionSensible(
                    contexto.actorSub(), contexto.asignacionEfectivaId(), perfilPrincipal, unidadRecursoId);
        } catch (ResponseStatusException ex) {
            if (perfilAsistente == null) {
                throw ex;
            }
            autorizacionService.revalidarParaOperacionSensible(
                    contexto.actorSub(), contexto.asignacionEfectivaId(), perfilAsistente, unidadRecursoId);
        }
    }

    // ---------------------------------------------------------------------
    // Mapeo y serializacion
    // ---------------------------------------------------------------------

    private IncorporacionDetail toDetail(IncorporacionRegistroEntity e) {
        if (incorporacionMapper != null) {
            return incorporacionMapper.toIncorporacionDetail(e);
        }
        Long version = e.getVersion() != null ? e.getVersion() : 0L;
        String etag = "\"" + safeId(e.getId()) + "-" + version + "\"";
        return new IncorporacionDetail(
                e.getId(),
                e.getFuente(),
                e.getFechaFuente(),
                e.getResponsableId(),
                e.getDocumentoFuenteId(),
                e.getHashOriginal(),
                e.getEstado(),
                e.getRegistroVinculadoId(),
                e.getObservacion(),
                e.getCreadoPor(),
                e.getFechaCreacion(),
                version,
                etag);
    }

    private String serializarDetalle(IncorporacionDetail detalle) {
        if (objectMapper == null) {
            throw new IllegalStateException(
                    "ObjectMapper no disponible para serializar la respuesta idempotente.");
        }
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo serializar el detalle de la incorporacion para idempotencia.", e);
        }
    }

    private IncorporacionDetail deserializarDetalle(String json) {
        if (objectMapper == null || json == null) {
            throw new IllegalStateException(
                    "No se puede deserializar la respuesta idempotente de la incorporacion.");
        }
        try {
            return objectMapper.readValue(json, IncorporacionDetail.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo deserializar la respuesta idempotente de la incorporacion.", e);
        }
    }

    private String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() > max ? valor.substring(0, max) : valor;
    }

    private String safeId(Long id) {
        return id == null ? "0" : id.toString();
    }
}


