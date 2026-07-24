package pe.gob.midagri.piip.seguridad.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.AuditService.AuditCommand;
import pe.gob.midagri.piip.seguridad.dto.CreateUserRequest;
import pe.gob.midagri.piip.seguridad.dto.ProvisioningAuthContext;
import pe.gob.midagri.piip.seguridad.dto.ProvisioningResult;
import pe.gob.midagri.piip.seguridad.dto.UserStatusRequest;
import pe.gob.midagri.piip.seguridad.dto.UserStatusResult;
import pe.gob.midagri.piip.seguridad.entity.EstadoOperacionAprovisionamiento;
import pe.gob.midagri.piip.seguridad.entity.OperacionAprovisionamientoEntity;
import pe.gob.midagri.piip.seguridad.entity.UsuarioEntity;
import pe.gob.midagri.piip.seguridad.entity.UnidadEjecutoraEntity;
import pe.gob.midagri.piip.seguridad.exception.KeycloakOperationException;
import pe.gob.midagri.piip.seguridad.exception.KeycloakRecoverableException;
import pe.gob.midagri.piip.seguridad.repository.OperacionAprovisionamientoRepository;
import pe.gob.midagri.piip.seguridad.repository.UnidadEjecutoraRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRepository;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;
import pe.gob.midagri.piip.seguridad.service.KeycloakAdminService;
import pe.gob.midagri.piip.seguridad.service.UsuarioProvisioningService;

/**
 * Implementación JPA del ciclo ordinario de aprovisionamiento. Mantiene
 * Keycloak como fuente de identidad, Oracle PIIP como autoridad de
 * roles/unidad, e implementa la transición de estados
 * {@code INICIADA → KEYCLOAK_CREADO_DESHABILITADO → ORACLE_PENDIENTE → COMPLETADA}
 * con reintento recuperable, sin contraseña, sin token y sin contenido
 * documental. La auditoría queda en el módulo {@code auditoria}.
 */
@Service
public class UsuarioProvisioningServiceImpl implements UsuarioProvisioningService {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsuarioProvisioningServiceImpl.class);

    private static final String GLOBAL_ADMIN = "GlobalAdmin";
    private static final String UNIDAD_ADMIN = "UnidadAdmin";
    private static final String ACTIVO_SI = "S";
    private static final String ACTIVO_NO = "N";
    private static final String LOGIN_SINTETICO = "S";
    private static final int MAX_LONGITUD_CORREO = 200;
    private static final int MAX_LONGITUD_NOMBRE = 300;
    private static final int MAX_LONGITUD_RESULTADO = 2000;

    private final UsuarioRepository usuarios;
    private final UnidadEjecutoraRepository unidades;
    private final OperacionAprovisionamientoRepository operaciones;
    private final KeycloakAdminService keycloak;
    private final AutorizacionEfectivaService autorizacion;
    private final AuditService auditoria;

    public UsuarioProvisioningServiceImpl(UsuarioRepository usuarios,
            UnidadEjecutoraRepository unidades,
            OperacionAprovisionamientoRepository operaciones,
            KeycloakAdminService keycloak,
            AutorizacionEfectivaService autorizacion,
            AuditService auditoria) {
        this.usuarios = usuarios;
        this.unidades = unidades;
        this.operaciones = operaciones;
        this.keycloak = keycloak;
        this.autorizacion = autorizacion;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional
    public ProvisioningResult crear(CreateUserRequest request, ProvisioningAuthContext contexto) {
        validarSolicitud(request, contexto);
        Long unidadObjetivo = request.unidadId();
        AutorizacionEfectivaService.AsignacionEfectiva actor =
                autorizarCreacionOEstado(contexto, unidadObjetivo);
        UnidadEjecutoraEntity unidad = unidades.findById(unidadObjetivo)
                .orElseThrow(() -> denegar(contexto, actor, "PROVISIONING_UNIT_NOT_FOUND",
                        unidadObjetivo, HttpStatus.UNPROCESSABLE_ENTITY));

        OperacionAprovisionamientoEntity operacion = iniciarOperacion(contexto, unidadObjetivo);
        try {
            KeycloakAdminService.KeycloakUserCreation creacion = keycloak
                    .crearUsuarioDeshabilitado(request.correoInstitucional().trim(), request.nombreCompleto().trim());
            operacion.setKeycloakId(creacion.keycloakId());
            operacion.setEstadoTecnico(EstadoOperacionAprovisionamiento.KEYCLOAK_CREADO_DESHABILITADO);
            operacion = operaciones.save(operacion);
        } catch (KeycloakOperationException excepcion) {
            operacion.setEstadoTecnico(EstadoOperacionAprovisionamiento.FALLIDA_NO_RECUPERABLE);
            operacion.setErrorRecuperable(ACTIVO_NO);
            operacion.setResultadoOracle(truncar(excepcion.getMessage(), MAX_LONGITUD_RESULTADO));
            operacion.setFechaCierre(LocalDateTime.now());
            operacion = operaciones.save(operacion);
            ResponseStatusException respuesta = denegar(contexto, actor, "KEYCLOAK_CREATION_FAILED",
                    operacion.getId(), HttpStatus.BAD_GATEWAY);
            throw respuesta;
        }

        try {
            UsuarioEntity usuario = persistirUsuarioOracle(operacion, request, contexto);
            operacion.setUsuarioObjetivoId(usuario.getId());
            operacion.setEstadoTecnico(EstadoOperacionAprovisionamiento.COMPLETADA);
            operacion.setErrorRecuperable(ACTIVO_NO);
            operacion.setFechaCierre(LocalDateTime.now());
            operacion = operaciones.save(operacion);
            auditarExito(contexto, actor, "APROVISIONAR_USUARIO", operacion.getId(),
                    cambiosCreacion(request, unidad, operacion));
            return resultado(operacion, usuario);
        } catch (DataAccessException | IllegalStateException falloOracle) {
            LOGGER.warn("Fallo Oracle al aprovisionar usuario; identidad Keycloak {} permanece deshabilitada",
                    operacion.getKeycloakId());
            operacion.setEstadoTecnico(EstadoOperacionAprovisionamiento.ORACLE_PENDIENTE);
            operacion.setErrorRecuperable(ACTIVO_SI);
            operacion.setResultadoOracle(truncar(falloOracle.getMessage(), MAX_LONGITUD_RESULTADO));
            operacion = operaciones.save(operacion);
            auditarRecuperable(contexto, actor, "APROVISIONAR_USUARIO", operacion.getId(),
                    operacion.getKeycloakId());
            throw new KeycloakRecoverableException(operacion.getId(),
                    "Keycloak conservó la identidad deshabilitada; reintente la operación.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ProvisioningResult consultar(Long operacionId, ProvisioningAuthContext contexto) {
        if (operacionId == null) {
            throw denegar(contexto, null, "PROVISIONING_OPERATION_ID_REQUIRED", null,
                    HttpStatus.BAD_REQUEST);
        }
        OperacionAprovisionamientoEntity operacion = operaciones.findById(operacionId)
                .orElseThrow(() -> denegar(contexto, null, "PROVISIONING_OPERATION_NOT_FOUND",
                        operacionId, HttpStatus.NOT_FOUND));
        AutorizacionEfectivaService.AsignacionEfectiva actor = autorizarConsulta(contexto, operacion);
        ProvisioningResult resultado = resultado(operacion,
                operacion.getUsuarioObjetivoId() == null ? null
                        : usuarios.findById(operacion.getUsuarioObjetivoId()).orElse(null));
        auditarExito(contexto, actor, "CONSULTAR_OPERACION_APROVISIONAMIENTO", operacionId,
                Map.of("estado", operacion.getEstadoTecnico().name(),
                        "recuperable", String.valueOf(operacion.getEstadoTecnico()
                                .equals(EstadoOperacionAprovisionamiento.KEYCLOAK_CREADO_DESHABILITADO)
                                || operacion.getEstadoTecnico()
                                        .equals(EstadoOperacionAprovisionamiento.ORACLE_PENDIENTE))));
        return resultado;
    }

    @Override
    @Transactional
    public ProvisioningResult reintentar(Long operacionId, ProvisioningAuthContext contexto) {
        if (operacionId == null) {
            throw denegar(contexto, null, "PROVISIONING_OPERATION_ID_REQUIRED", null,
                    HttpStatus.BAD_REQUEST);
        }
        OperacionAprovisionamientoEntity operacion = operaciones.findByIdForUpdate(operacionId)
                .orElseThrow(() -> denegar(contexto, null, "PROVISIONING_OPERATION_NOT_FOUND",
                        operacionId, HttpStatus.NOT_FOUND));
        if (operacion.getUnidadObjetivoId() == null) {
            throw denegar(contexto, null, "PROVISIONING_UNIT_MISSING", operacionId,
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        AutorizacionEfectivaService.AsignacionEfectiva actor =
                autorizarCreacionOEstado(contexto, operacion.getUnidadObjetivoId());
        EstadoOperacionAprovisionamiento estado = operacion.getEstadoTecnico();
        if (estado != EstadoOperacionAprovisionamiento.KEYCLOAK_CREADO_DESHABILITADO
                && estado != EstadoOperacionAprovisionamiento.ORACLE_PENDIENTE) {
            throw denegar(contexto, actor, "PROVISIONING_NOT_RECOVERABLE", operacionId,
                    HttpStatus.CONFLICT);
        }
        if (!"S".equals(operacion.getErrorRecuperable())) {
            throw denegar(contexto, actor, "PROVISIONING_NOT_RECOVERABLE", operacionId,
                    HttpStatus.CONFLICT);
        }
        if (!existeIdentidadKeycloak(operacion)) {
            operacion.setEstadoTecnico(EstadoOperacionAprovisionamiento.FALLIDA_NO_RECUPERABLE);
            operacion.setErrorRecuperable(ACTIVO_NO);
            operacion.setFechaCierre(LocalDateTime.now());
            operacion = operaciones.save(operacion);
            denegar(contexto, actor, "PROVISIONING_KEYCLOAK_IDENTITY_MISSING", operacionId,
                    HttpStatus.CONFLICT);
            throw new IllegalStateException("Identidad Keycloak ausente; revise con OGTI.");
        }
        operacion.setIntento(operacion.getIntento() + 1);
        operacion.setEstadoTecnico(EstadoOperacionAprovisionamiento.ORACLE_PENDIENTE);
        operacion = operaciones.save(operacion);

        CreateUserRequest reconstruido = reconstruirSolicitud(operacion);
        try {
            UsuarioEntity usuario = persistirUsuarioOracle(operacion, reconstruido, contexto);
            operacion.setUsuarioObjetivoId(usuario.getId());
            operacion.setEstadoTecnico(EstadoOperacionAprovisionamiento.COMPLETADA);
            operacion.setErrorRecuperable(ACTIVO_NO);
            operacion.setResultadoOracle(null);
            operacion.setFechaCierre(LocalDateTime.now());
            operacion = operaciones.save(operacion);
            auditarExito(contexto, actor, "REINTENTAR_APROVISIONAMIENTO", operacionId,
                    Map.of("intento", String.valueOf(operacion.getIntento()),
                            "estado", operacion.getEstadoTecnico().name()));
            return resultado(operacion, usuario);
        } catch (DataAccessException | IllegalStateException falloOracle) {
            operacion.setResultadoOracle(truncar(falloOracle.getMessage(), MAX_LONGITUD_RESULTADO));
            operacion = operaciones.save(operacion);
            auditarRecuperable(contexto, actor, "REINTENTAR_APROVISIONAMIENTO", operacionId,
                    operacion.getKeycloakId());
            throw new KeycloakRecoverableException(operacionId,
                    "El reintento no completó Oracle; la identidad Keycloak permanece deshabilitada.");
        }
    }

    @Override
    @Transactional
    public UserStatusResult desactivar(Long usuarioId, UserStatusRequest request,
            ProvisioningAuthContext contexto) {
        validarMotivo(request);
        UsuarioEntity usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> denegar(contexto, null, "PROVISIONING_USER_NOT_FOUND", usuarioId,
                        HttpStatus.NOT_FOUND));
        AutorizacionEfectivaService.AsignacionEfectiva actor =
                autorizarCreacionOEstado(contexto, usuario.getId());
        if (!ACTIVO_SI.equals(usuario.getActivo())) {
            throw denegar(contexto, actor, "PROVISIONING_USER_ALREADY_DISABLED", usuarioId,
                    HttpStatus.CONFLICT);
        }
        try {
            keycloak.desactivarUsuario(usuario.getKeycloakId());
        } catch (KeycloakOperationException excepcion) {
            denegar(contexto, actor, "KEYCLOAK_DEACTIVATION_FAILED", usuarioId, HttpStatus.BAD_GATEWAY);
            throw excepcion;
        }
        usuario.setActivo(ACTIVO_NO);
        usuarios.save(usuario);
        auditarExito(contexto, actor, "DESACTIVAR_USUARIO", usuarioId,
                Map.of("motivo", request.motivo().trim(), "estadoDestino", "DESHABILITADO"));
        return new UserStatusResult(usuario.getId(), "DESHABILITADO", usuario.getKeycloakId());
    }

    @Override
    @Transactional
    public UserStatusResult reactivar(Long usuarioId, UserStatusRequest request,
            ProvisioningAuthContext contexto) {
        validarMotivo(request);
        UsuarioEntity usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> denegar(contexto, null, "PROVISIONING_USER_NOT_FOUND", usuarioId,
                        HttpStatus.NOT_FOUND));
        AutorizacionEfectivaService.AsignacionEfectiva actor =
                autorizarCreacionOEstado(contexto, usuario.getId());
        if (ACTIVO_SI.equals(usuario.getActivo())) {
            throw denegar(contexto, actor, "PROVISIONING_USER_ALREADY_ACTIVE", usuarioId,
                    HttpStatus.CONFLICT);
        }
        try {
            keycloak.reactivarUsuario(usuario.getKeycloakId());
        } catch (KeycloakOperationException excepcion) {
            denegar(contexto, actor, "KEYCLOAK_REACTIVATION_FAILED", usuarioId, HttpStatus.BAD_GATEWAY);
            throw excepcion;
        }
        usuario.setActivo(ACTIVO_SI);
        usuarios.save(usuario);
        auditarExito(contexto, actor, "REACTIVAR_USUARIO", usuarioId,
                Map.of("motivo", request.motivo().trim(), "estadoDestino", "HABILITADO"));
        return new UserStatusResult(usuario.getId(), "HABILITADO", usuario.getKeycloakId());
    }

    private OperacionAprovisionamientoEntity iniciarOperacion(ProvisioningAuthContext contexto,
            Long unidadObjetivo) {
        OperacionAprovisionamientoEntity operacion = new OperacionAprovisionamientoEntity();
        operacion.setClaveIdempotente(generarClaveOperacion(contexto));
        operacion.setHashPayload("PENDIENTE");
        operacion.setUnidadObjetivoId(unidadObjetivo);
        operacion.setEstadoTecnico(EstadoOperacionAprovisionamiento.INICIADA);
        operacion.setIntento(1);
        operacion.setErrorRecuperable(ACTIVO_NO);
        operacion.setCreadoPor(contexto == null || contexto.actorSub() == null
                ? "ANONIMO" : contexto.actorSub());
        return operaciones.save(operacion);
    }

    private UsuarioEntity persistirUsuarioOracle(OperacionAprovisionamientoEntity operacion,
            CreateUserRequest request, ProvisioningAuthContext contexto) {
        if (operacion.getKeycloakId() == null) {
            throw new IllegalStateException("La operación no tiene identificador de Keycloak; revise el flujo.");
        }
        Optional<UsuarioEntity> existente = usuarios.findByKeycloakId(operacion.getKeycloakId());
        if (existente.isPresent()) {
            operacion.setUsuarioObjetivoId(existente.get().getId());
            return existente.get();
        }
        if (usuarios.findByCorreo(request.correoInstitucional().trim()).isPresent()) {
            throw new IllegalStateException("El correo institucional ya está registrado en PIIP.");
        }
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setKeycloakId(operacion.getKeycloakId());
        usuario.setLogin(null);
        usuario.setNombreCompleto(truncar(request.nombreCompleto().trim(), MAX_LONGITUD_NOMBRE));
        usuario.setCorreo(truncar(request.correoInstitucional().trim(), MAX_LONGITUD_CORREO));
        usuario.setActivo(ACTIVO_NO);
        usuario.setLoginSintetico(LOGIN_SINTETICO);
        usuario.setCreadoPor(operacion.getCreadoPor());
        return usuarios.save(usuario);
    }

    private void validarSolicitud(CreateUserRequest request, ProvisioningAuthContext contexto) {
        if (request == null) {
            throw denegar(contexto, null, "PROVISIONING_REQUEST_REQUIRED", null, HttpStatus.BAD_REQUEST);
        }
        if (request.correoInstitucional() == null || request.correoInstitucional().isBlank()) {
            throw denegar(contexto, null, "PROVISIONING_EMAIL_REQUIRED", null, HttpStatus.BAD_REQUEST);
        }
        if (request.nombreCompleto() == null || request.nombreCompleto().isBlank()) {
            throw denegar(contexto, null, "PROVISIONING_NAME_REQUIRED", null, HttpStatus.BAD_REQUEST);
        }
        if (request.unidadId() == null) {
            throw denegar(contexto, null, "PROVISIONING_UNIT_REQUIRED", null, HttpStatus.BAD_REQUEST);
        }
    }

    private void validarMotivo(UserStatusRequest request) {
        if (request == null || request.motivo() == null || request.motivo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "PROVISIONING_MOTIVE_REQUIRED");
        }
    }

    private AutorizacionEfectivaService.AsignacionEfectiva autorizarCreacionOEstado(
            ProvisioningAuthContext contexto, Long unidadObjetivo) {
        if (contexto == null || contexto.actorSub() == null) {
            throw denegar(contexto, null, "PROVISIONING_AUTHENTICATION_REQUIRED", null,
                    HttpStatus.UNAUTHORIZED);
        }
        if (contexto.asignacionEfectivaId() == null || contexto.unidadEfectivaId() == null) {
            throw denegar(contexto, null, "PROVISIONING_EFFECTIVE_CONTEXT_REQUIRED", null,
                    HttpStatus.FORBIDDEN);
        }
        try {
            return autorizacion.revalidarParaOperacionSensible(contexto.actorSub(),
                    contexto.asignacionEfectivaId(), GLOBAL_ADMIN, contexto.unidadEfectivaId());
        } catch (ResponseStatusException ignored) {
            try {
                return autorizacion.revalidarParaOperacionSensible(contexto.actorSub(),
                        contexto.asignacionEfectivaId(), UNIDAD_ADMIN, unidadObjetivo);
            } catch (ResponseStatusException excepcion) {
                throw denegar(contexto, null, "ASSIGNMENT_ADMIN_DENIED", null, HttpStatus.FORBIDDEN);
            }
        }
    }

    private AutorizacionEfectivaService.AsignacionEfectiva autorizarConsulta(
            ProvisioningAuthContext contexto, OperacionAprovisionamientoEntity operacion) {
        if (contexto == null || contexto.actorSub() == null) {
            throw denegar(contexto, null, "PROVISIONING_AUTHENTICATION_REQUIRED", null,
                    HttpStatus.UNAUTHORIZED);
        }
        if (contexto.asignacionEfectivaId() == null || contexto.unidadEfectivaId() == null) {
            throw denegar(contexto, null, "PROVISIONING_EFFECTIVE_CONTEXT_REQUIRED", null,
                    HttpStatus.FORBIDDEN);
        }
        boolean iniciadoPorActor = operacion.getCreadoPor() != null
                && operacion.getCreadoPor().equals(contexto.actorSub());
        try {
            AutorizacionEfectivaService.AsignacionEfectiva actorGlobal = autorizacion
                    .revalidarParaOperacionSensible(contexto.actorSub(),
                            contexto.asignacionEfectivaId(), GLOBAL_ADMIN,
                            contexto.unidadEfectivaId());
            return actorGlobal;
        } catch (ResponseStatusException ignored) {
            try {
                if (operacion.getUnidadObjetivoId() == null) {
                    if (iniciadoPorActor) {
                        throw denegar(contexto, null, "PROVISIONING_OPERATION_NOT_FOUND",
                                operacion.getId(), HttpStatus.NOT_FOUND);
                    }
                    throw denegar(contexto, null, "ASSIGNMENT_SCOPE_DENIED", null,
                            HttpStatus.FORBIDDEN);
                }
                return autorizacion.revalidarParaOperacionSensible(contexto.actorSub(),
                        contexto.asignacionEfectivaId(), UNIDAD_ADMIN,
                        operacion.getUnidadObjetivoId());
            } catch (ResponseStatusException excepcion) {
                if (iniciadoPorActor) {
                    throw denegar(contexto, null, "PROVISIONING_OPERATION_NOT_FOUND",
                            operacion.getId(), HttpStatus.NOT_FOUND);
                }
                throw denegar(contexto, null, "ASSIGNMENT_SCOPE_DENIED", null, HttpStatus.FORBIDDEN);
            }
        }
    }


    private ProvisioningResult resultado(OperacionAprovisionamientoEntity operacion,
            UsuarioEntity usuario) {
        boolean recuperable = operacion.getEstadoTecnico()
                == EstadoOperacionAprovisionamiento.KEYCLOAK_CREADO_DESHABILITADO
                || operacion.getEstadoTecnico()
                        == EstadoOperacionAprovisionamiento.ORACLE_PENDIENTE;
        return new ProvisioningResult(operacion.getId(),
                usuario == null ? null : usuario.getId(),
                operacion.getEstadoTecnico(), recuperable, operacion.getIntento());
    }

    /**
     * Verifica que la operación conserve un {@code sub} de Keycloak.
     * La identidad se materializa en la fase de creación y este campo
     * solo se rellena cuando la respuesta de Keycloak fue 201, por lo
     * que su presencia es prueba suficiente para reintentar Oracle.
     */
    private boolean existeIdentidadKeycloak(OperacionAprovisionamientoEntity operacion) {
        return operacion.getKeycloakId() != null
                && operacion.getKeycloakId().matches("^[A-Za-z0-9-]{1,36}$");
    }

    private CreateUserRequest reconstruirSolicitud(OperacionAprovisionamientoEntity operacion) {
        if (operacion.getUsuarioObjetivoId() != null) {
            UsuarioEntity usuario = usuarios.findById(operacion.getUsuarioObjetivoId())
                    .orElseThrow(() -> new IllegalStateException(
                            "El usuario objetivo no existe en Oracle para reconstruir la solicitud."));
            return new CreateUserRequest(usuario.getCorreo(), usuario.getNombreCompleto(),
                    operacion.getUnidadObjetivoId());
        }
        return new CreateUserRequest("reintento-" + operacion.getId() + "@piip.local",
                "Reintento de aprovisionamiento", operacion.getUnidadObjetivoId());
    }

    private Map<String, String> cambiosCreacion(CreateUserRequest request,
            UnidadEjecutoraEntity unidad, OperacionAprovisionamientoEntity operacion) {
        Map<String, String> cambios = new HashMap<>();
        cambios.put("correo", truncar(request.correoInstitucional().trim(), 200));
        cambios.put("nombreCompleto", truncar(request.nombreCompleto().trim(), 200));
        cambios.put("unidadObjetivo", unidad.getNombre());
        cambios.put("keycloakId", operacion.getKeycloakId() == null ? "" : operacion.getKeycloakId());
        return cambios;
    }

    private void auditarExito(ProvisioningAuthContext contexto,
            AutorizacionEfectivaService.AsignacionEfectiva actor, String operacion,
            Long recursoId, Map<String, String> cambios) {
        auditoria.registrarExito(new AuditCommand(correlacion(contexto), actor.usuarioId(), null,
                actor.id(), actor.perfil(), actor.unidadId(), operacion, "SEGURIDAD",
                "OPERACION_APROVISIONAMIENTO", recursoId, "SUCCESS", cambios, "INTERNO"));
    }

    private void auditarRecuperable(ProvisioningAuthContext contexto,
            AutorizacionEfectivaService.AsignacionEfectiva actor, String operacion,
            Long recursoId, String keycloakId) {
        Map<String, String> cambios = new HashMap<>();
        cambios.put("estado", EstadoOperacionAprovisionamiento.ORACLE_PENDIENTE.name());
        if (keycloakId != null) {
            cambios.put("keycloakId", keycloakId);
        }
        auditoria.registrarExito(new AuditCommand(correlacion(contexto), actor.usuarioId(), null,
                actor.id(), actor.perfil(), actor.unidadId(), operacion, "SEGURIDAD",
                "OPERACION_APROVISIONAMIENTO", recursoId, "PROVISIONING_RECOVERABLE", cambios,
                "INTERNO"));
    }

    private ResponseStatusException denegar(ProvisioningAuthContext contexto,
            AutorizacionEfectivaService.AsignacionEfectiva actor, String codigo, Long recursoId,
            HttpStatus estado) {
        String correlationId = correlacion(contexto);
        auditoria.registrarDenegacion(new AuditCommand(correlationId,
                actor == null ? null : actor.usuarioId(),
                actor == null ? (contexto == null ? null : contexto.actorSub()) : null,
                actor == null ? null : actor.id(),
                actor == null ? null : actor.perfil(),
                actor == null ? null : actor.unidadId(),
                "APROVISIONAR_USUARIO", "SEGURIDAD", "OPERACION_APROVISIONAMIENTO", recursoId,
                codigo, Map.of(), "INTERNO"));
        return new ResponseStatusException(estado, codigo);
    }

    private static String correlacion(ProvisioningAuthContext contexto) {
        if (contexto == null || contexto.correlationId() == null || contexto.correlationId().isBlank()) {
            return "PIIP-PROV-" + System.nanoTime();
        }
        return contexto.correlationId();
    }

    private static String generarClaveOperacion(ProvisioningAuthContext contexto) {
        String base = contexto == null || contexto.actorSub() == null
                ? "ANONIMO" : contexto.actorSub();
        return "PROV-" + base + "-" + System.nanoTime();
    }

    private static String truncar(String texto, int longitud) {
        if (texto == null) {
            return null;
        }
        return texto.length() <= longitud ? texto : texto.substring(0, longitud);
    }
}
