package pe.gob.midagri.piip.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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
import pe.gob.midagri.piip.seguridad.entity.UnidadEjecutoraEntity;
import pe.gob.midagri.piip.seguridad.entity.UsuarioEntity;
import pe.gob.midagri.piip.seguridad.exception.KeycloakOperationException;
import pe.gob.midagri.piip.seguridad.exception.KeycloakRecoverableException;
import pe.gob.midagri.piip.seguridad.repository.OperacionAprovisionamientoRepository;
import pe.gob.midagri.piip.seguridad.repository.UnidadEjecutoraRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRepository;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService.AsignacionEfectiva;
import pe.gob.midagri.piip.seguridad.service.KeycloakAdminService;
import pe.gob.midagri.piip.seguridad.service.KeycloakAdminService.KeycloakUserCreation;
import pe.gob.midagri.piip.seguridad.service.impl.UsuarioProvisioningServiceImpl;

class UsuarioProvisioningServiceImplTest {

    private final UsuarioRepository usuarios = org.mockito.Mockito.mock(UsuarioRepository.class);
    private final UnidadEjecutoraRepository unidades = org.mockito.Mockito.mock(UnidadEjecutoraRepository.class);
    private final OperacionAprovisionamientoRepository operaciones = org.mockito.Mockito.mock(
            OperacionAprovisionamientoRepository.class);
    private final KeycloakAdminService keycloak = org.mockito.Mockito.mock(KeycloakAdminService.class);
    private final AutorizacionEfectivaService autorizacion = org.mockito.Mockito.mock(
            AutorizacionEfectivaService.class);
    private final AuditService auditoria = org.mockito.Mockito.mock(AuditService.class);
    private UsuarioProvisioningServiceImpl servicio;

    private final ProvisioningAuthContext contexto = new ProvisioningAuthContext("admin", 100L, 1L,
            "corr-test");
    private final AsignacionEfectiva actor = new AsignacionEfectiva(100L, 50L, 3L, "GlobalAdmin", 1L);

    @BeforeEach
    void preparar() {
        servicio = new UsuarioProvisioningServiceImpl(usuarios, unidades, operaciones, keycloak,
                autorizacion, auditoria);
        when(autorizacion.revalidarParaOperacionSensible("admin", 100L, "GlobalAdmin", 1L))
                .thenReturn(actor);
        when(operaciones.save(any(OperacionAprovisionamientoEntity.class)))
                .thenAnswer(invocacion -> {
                    OperacionAprovisionamientoEntity entidad = invocacion.getArgument(0);
                    if (entidad.getId() == null) {
                        entidad.setId(System.nanoTime());
                    }
                    return entidad;
                });
    }

    @Test
    void crearConKeycloakExitosoYOracleExitosoCompletaOperacion() {
        when(keycloak.crearUsuarioDeshabilitado("ana@midagri.gob.pe", "Ana Pérez"))
                .thenReturn(new KeycloakUserCreation("kc-1", true));
        when(unidades.findById(7L)).thenReturn(Optional.of(unidad(7L, "MIDAGRI")));
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.empty());
        when(usuarios.findByCorreo("ana@midagri.gob.pe")).thenReturn(Optional.empty());
        when(usuarios.save(any(UsuarioEntity.class))).thenAnswer(inv -> {
            UsuarioEntity u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        ProvisioningResult resultado = servicio.crear(
                new CreateUserRequest("ana@midagri.gob.pe", "Ana Pérez", 7L), contexto);

        assertEquals(EstadoOperacionAprovisionamiento.COMPLETADA, resultado.estado());
        assertFalse(resultado.recuperable());
        assertEquals(99L, resultado.usuarioId());
        verify(usuarios).save(any(UsuarioEntity.class));
        verify(auditoria, times(1)).registrarExito(any(AuditCommand.class));
        verify(auditoria, never()).registrarDenegacion(any());
    }

    @Test
    void crearConFalloOracleConservaIdentidadDeshabilitadaYOperacionRecuperable() {
        when(keycloak.crearUsuarioDeshabilitado("ana@midagri.gob.pe", "Ana Pérez"))
                .thenReturn(new KeycloakUserCreation("kc-1", true));
        when(unidades.findById(7L)).thenReturn(Optional.of(unidad(7L, "MIDAGRI")));
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.empty());
        when(usuarios.findByCorreo("ana@midagri.gob.pe")).thenReturn(Optional.empty());
        when(usuarios.save(any(UsuarioEntity.class)))
                .thenThrow(new DataIntegrityViolationException("ORA-02291 integridad"));

        ArgumentCaptor<OperacionAprovisionamientoEntity> captor = ArgumentCaptor
                .forClass(OperacionAprovisionamientoEntity.class);
        KeycloakRecoverableException excepcion = assertThrows(KeycloakRecoverableException.class,
                () -> servicio.crear(
                        new CreateUserRequest("ana@midagri.gob.pe", "Ana Pérez", 7L), contexto));

        assertNotNull(excepcion.getOperacionId());
        verify(operaciones, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        OperacionAprovisionamientoEntity guardada = captor.getAllValues()
                .get(captor.getAllValues().size() - 1);
        assertEquals(EstadoOperacionAprovisionamiento.ORACLE_PENDIENTE, guardada.getEstadoTecnico());
        assertEquals("S", guardada.getErrorRecuperable());
        assertEquals("kc-1", guardada.getKeycloakId());
    }

    @Test
    void crearConFalloKeycloakNoCreaUsuarioOracleYMarcanoRecuperable() {
        when(keycloak.crearUsuarioDeshabilitado(anyString(), anyString()))
                .thenThrow(new KeycloakOperationException("Keycloak rechazó 409"));
        when(unidades.findById(7L)).thenReturn(Optional.of(unidad(7L, "MIDAGRI")));

        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(
                        new CreateUserRequest("ana@midagri.gob.pe", "Ana Pérez", 7L), contexto));
        assertEquals(HttpStatus.BAD_GATEWAY, excepcion.getStatusCode());
        assertEquals("KEYCLOAK_CREATION_FAILED", excepcion.getReason());
        verify(usuarios, never()).save(any(UsuarioEntity.class));
        verify(auditoria).registrarDenegacion(any(AuditCommand.class));
    }

    @Test
    void consultarOperacionRecuperableExitosaAuditaComoExitoso() {
        OperacionAprovisionamientoEntity operacion = operacionCompleta();
        operacion.setEstadoTecnico(EstadoOperacionAprovisionamiento.ORACLE_PENDIENTE);
        when(operaciones.findById(operacion.getId())).thenReturn(Optional.of(operacion));
        when(usuarios.findById(operacion.getUsuarioObjetivoId()))
                .thenReturn(Optional.of(usuarioOracle(operacion.getUsuarioObjetivoId())));

        ProvisioningResult resultado = servicio.consultar(operacion.getId(), contexto);

        assertEquals(EstadoOperacionAprovisionamiento.ORACLE_PENDIENTE, resultado.estado());
        assertTrue(resultado.recuperable());
        verify(auditoria).registrarExito(any(AuditCommand.class));
    }

    @Test
    void reintentarOperacionRecuperableCompletaYActualizaIntentos() {
        OperacionAprovisionamientoEntity operacion = operacionCompleta();
        operacion.setEstadoTecnico(EstadoOperacionAprovisionamiento.ORACLE_PENDIENTE);
        operacion.setErrorRecuperable("S");
        when(operaciones.findByIdForUpdate(operacion.getId())).thenReturn(Optional.of(operacion));
        when(usuarios.findById(operacion.getUsuarioObjetivoId()))
                .thenReturn(Optional.of(usuarioOracle(operacion.getUsuarioObjetivoId())));
        when(usuarios.save(any(UsuarioEntity.class))).thenAnswer(inv -> {
            UsuarioEntity u = inv.getArgument(0);
            u.setId(operacion.getUsuarioObjetivoId());
            return u;
        });

        ProvisioningResult resultado = servicio.reintentar(operacion.getId(), contexto);

        assertEquals(EstadoOperacionAprovisionamiento.COMPLETADA, resultado.estado());
        assertFalse(resultado.recuperable());
        verify(operaciones, org.mockito.Mockito.atLeast(2)).save(any(OperacionAprovisionamientoEntity.class));
    }

    @Test
    void reintentarOperacionNoRecuperableSeRechazaComoConflicto() {
        OperacionAprovisionamientoEntity operacion = operacionCompleta();
        operacion.setEstadoTecnico(EstadoOperacionAprovisionamiento.COMPLETADA);
        when(operaciones.findByIdForUpdate(operacion.getId())).thenReturn(Optional.of(operacion));

        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.reintentar(operacion.getId(), contexto));
        assertEquals(HttpStatus.CONFLICT, excepcion.getStatusCode());
        assertEquals("PROVISIONING_NOT_RECOVERABLE", excepcion.getReason());
    }

    @Test
    void desactivarUsuarioDeshabilitaYAuditaExitoso() {
        UsuarioEntity usuario = usuarioOracle(7L);
        usuario.setActivo("S");
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        doNothing().when(keycloak).desactivarUsuario(usuario.getKeycloakId());

        UserStatusResult resultado = servicio.desactivar(7L,
                new UserStatusRequest("Cese por renuncia"), contexto);

        assertEquals("DESHABILITADO", resultado.estado());
        assertEquals("N", usuario.getActivo());
        verify(keycloak).desactivarUsuario(usuario.getKeycloakId());
        verify(auditoria).registrarExito(any(AuditCommand.class));
    }

    @Test
    void desactivarUsuarioYaDeshabilitadoSeRechazaComoConflicto() {
        UsuarioEntity usuario = usuarioOracle(7L);
        usuario.setActivo("N");
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));

        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.desactivar(7L, new UserStatusRequest("Cese duplicado"), contexto));
        assertEquals(HttpStatus.CONFLICT, excepcion.getStatusCode());
        assertEquals("PROVISIONING_USER_ALREADY_DISABLED", excepcion.getReason());
        verify(keycloak, never()).desactivarUsuario(anyString());
    }

    @Test
    void reactivarUsuarioHabilitadoActivaYAuditaExitoso() {
        UsuarioEntity usuario = usuarioOracle(7L);
        usuario.setActivo("N");
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        doNothing().when(keycloak).reactivarUsuario(usuario.getKeycloakId());

        UserStatusResult resultado = servicio.reactivar(7L,
                new UserStatusRequest("Reingreso institucional"), contexto);

        assertEquals("HABILITADO", resultado.estado());
        assertEquals("S", usuario.getActivo());
        verify(keycloak).reactivarUsuario(usuario.getKeycloakId());
        verify(auditoria).registrarExito(any(AuditCommand.class));
    }

    @Test
    void activarPorReactivacionConFalloKeycloakAuditaDenegacion() {
        UsuarioEntity usuario = usuarioOracle(7L);
        usuario.setActivo("N");
        when(usuarios.findById(7L)).thenReturn(Optional.of(usuario));
        doThrow(new KeycloakOperationException("Keycloak no disponible"))
                .when(keycloak).reactivarUsuario(usuario.getKeycloakId());

        assertThrows(KeycloakOperationException.class,
                () -> servicio.reactivar(7L, new UserStatusRequest("Reingreso institucional"), contexto));
        assertEquals("N", usuario.getActivo());
        verify(auditoria).registrarDenegacion(any(AuditCommand.class));
    }

    @Test
    void crearConSolicitudInvalidaDenegada() {
        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(
                        new CreateUserRequest("   ", "Ana", 7L), contexto));
        assertEquals(HttpStatus.BAD_REQUEST, excepcion.getStatusCode());
        verify(keycloak, never()).crearUsuarioDeshabilitado(anyString(), anyString());
    }

    @Test
    void crearSinUnidadObjetivoDenegada() {
        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(
                        new CreateUserRequest("ana@midagri.gob.pe", "Ana", null), contexto));
        assertEquals(HttpStatus.BAD_REQUEST, excepcion.getStatusCode());
        assertEquals("PROVISIONING_UNIT_REQUIRED", excepcion.getReason());
    }

    @Test
    void crearSinAsignacionEfectivaDenegada() {
        ProvisioningAuthContext sinAsignacion = new ProvisioningAuthContext("admin", null, 1L,
                "corr");
        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.crear(
                        new CreateUserRequest("ana@midagri.gob.pe", "Ana", 7L), sinAsignacion));
        assertEquals(HttpStatus.FORBIDDEN, excepcion.getStatusCode());
        assertEquals("PROVISIONING_EFFECTIVE_CONTEXT_REQUIRED", excepcion.getReason());
    }

    @Test
    void consultarOperacionInexistenteDenegada() {
        when(operaciones.findById(404L)).thenReturn(Optional.empty());
        ResponseStatusException excepcion = assertThrows(ResponseStatusException.class,
                () -> servicio.consultar(404L, contexto));
        assertEquals(HttpStatus.NOT_FOUND, excepcion.getStatusCode());
        assertEquals("PROVISIONING_OPERATION_NOT_FOUND", excepcion.getReason());
    }

    @Test
    void consultarOperacionComoUnidadAdminDeUnidadCoincidentePermiteAcceso() {
        OperacionAprovisionamientoEntity operacion = operacionCompleta();
        operacion.setUnidadObjetivoId(7L);
        when(operaciones.findById(operacion.getId())).thenReturn(Optional.of(operacion));
        when(autorizacion.revalidarParaOperacionSensible("admin", 100L, "GlobalAdmin", 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "no es global"));
        when(autorizacion.revalidarParaOperacionSensible("admin", 100L, "UnidadAdmin", 7L))
                .thenReturn(new AsignacionEfectiva(200L, 50L, 4L, "UnidadAdmin", 7L));
        when(usuarios.findById(operacion.getUsuarioObjetivoId()))
                .thenReturn(Optional.of(usuarioOracle(operacion.getUsuarioObjetivoId())));

        ProvisioningResult resultado = servicio.consultar(operacion.getId(), contexto);

        assertNotNull(resultado);
        assertEquals(EstadoOperacionAprovisionamiento.COMPLETADA, resultado.estado());
        verify(autorizacion).revalidarParaOperacionSensible(eq("admin"), eq(100L), eq("UnidadAdmin"),
                eq(7L));
    }

    @Test
    void crearNoContieneContrasenaOTokenEnAuditoria() {
        when(keycloak.crearUsuarioDeshabilitado("ana@midagri.gob.pe", "Ana Pérez"))
                .thenReturn(new KeycloakUserCreation("kc-1", true));
        when(unidades.findById(7L)).thenReturn(Optional.of(unidad(7L, "MIDAGRI")));
        when(usuarios.findByKeycloakId("kc-1")).thenReturn(Optional.empty());
        when(usuarios.findByCorreo("ana@midagri.gob.pe")).thenReturn(Optional.empty());
        when(usuarios.save(any(UsuarioEntity.class))).thenAnswer(inv -> {
            UsuarioEntity u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });

        ArgumentCaptor<AuditCommand> captor = ArgumentCaptor.forClass(AuditCommand.class);
        servicio.crear(new CreateUserRequest("ana@midagri.gob.pe", "Ana Pérez", 7L), contexto);
        verify(auditoria).registrarExito(captor.capture());

        AuditCommand comando = captor.getValue();
        assertNull(comando.identidadAnonimaMinima());
        assertTrue(comando.cambiosMinimos().containsKey("correo"));
        assertTrue(comando.cambiosMinimos().containsKey("keycloakId"));
        assertFalse(comando.cambiosMinimos().containsKey("password"));
        assertFalse(comando.cambiosMinimos().containsKey("token"));
    }

    private UnidadEjecutoraEntity unidad(Long id, String nombre) {
        UnidadEjecutoraEntity unidad = new UnidadEjecutoraEntity();
        unidad.setId(id);
        unidad.setNombre(nombre);
        return unidad;
    }

    private UsuarioEntity usuarioOracle(Long id) {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(id);
        usuario.setKeycloakId("kc-" + id);
        usuario.setNombreCompleto("Ana Pérez");
        usuario.setCorreo("ana@midagri.gob.pe");
        usuario.setActivo("N");
        usuario.setLoginSintetico("S");
        usuario.setCreadoPor("admin");
        return usuario;
    }

    private OperacionAprovisionamientoEntity operacionCompleta() {
        OperacionAprovisionamientoEntity operacion = new OperacionAprovisionamientoEntity();
        operacion.setId(500L);
        operacion.setClaveIdempotente("PROV-admin-1");
        operacion.setHashPayload("hash");
        operacion.setKeycloakId("kc-99");
        operacion.setUsuarioObjetivoId(99L);
        operacion.setUnidadObjetivoId(7L);
        operacion.setEstadoTecnico(EstadoOperacionAprovisionamiento.COMPLETADA);
        operacion.setIntento(1);
        operacion.setErrorRecuperable("N");
        operacion.setCreadoPor("admin");
        return operacion;
    }
}
