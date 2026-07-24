package pe.gob.midagri.piip.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.ArgumentCaptor;

import pe.gob.midagri.piip.seguridad.config.KeycloakAdminProperties;
import pe.gob.midagri.piip.seguridad.exception.KeycloakOperationException;
import pe.gob.midagri.piip.seguridad.service.impl.KeycloakAdminServiceImpl;

class KeycloakAdminServiceImplTest {

    private final Keycloak keycloak = mock(Keycloak.class);
    private final RealmResource realm = mock(RealmResource.class);
    private final UsersResource usuarios = mock(UsersResource.class);
    private final UserResource usuarioRecurso = mock(UserResource.class);
    private KeycloakAdminServiceImpl servicio;

    @BeforeEach
    void preparar() {
        when(keycloak.realm("piip")).thenReturn(realm);
        when(realm.users()).thenReturn(usuarios);
        when(usuarios.get("kc-1")).thenReturn(usuarioRecurso);
        KeycloakAdminProperties propiedades = new KeycloakAdminProperties();
        propiedades.setRealm("piip");
        servicio = new KeycloakAdminServiceImpl(keycloak, propiedades);
    }

    @Test
    void crearUsuarioDevuelveIdentificadorKeycloak() {
        Response respuesta = mock(Response.class);
        when(respuesta.getStatus()).thenReturn(201);
        when(respuesta.getLocation()).thenReturn(URI.create(
                "https://keycloak.example.com/admin/realms/piip/users/kc-1"));
        when(usuarios.create(any(UserRepresentation.class))).thenReturn(respuesta);

        KeycloakAdminServiceImpl.KeycloakUserCreation creacion = servicio
                .crearUsuarioDeshabilitado("ana@midagri.gob.pe", "Ana Pérez");

        assertEquals("kc-1", creacion.keycloakId());
        assertTrue(creacion.emailEnviado());
        ArgumentCaptor<UserRepresentation> captor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(usuarios).create(captor.capture());
        assertEquals(Boolean.FALSE, captor.getValue().isEnabled());
        assertEquals(Collections.singletonList("VERIFY_EMAIL"), captor.getValue().getRequiredActions());
    }

    @Test
    void crearUsuarioConEstadoDistintoA201LanzaExcepcionOperativa() {
        Response respuesta = mock(Response.class);
        when(respuesta.getStatus()).thenReturn(409);
        when(usuarios.create(any(UserRepresentation.class))).thenReturn(respuesta);

        KeycloakOperationException excepcion = assertThrows(KeycloakOperationException.class,
                () -> servicio.crearUsuarioDeshabilitado("ana@midagri.gob.pe", "Ana Pérez"));
        assertTrue(excepcion.getMessage().contains("409"));
    }

    @Test
    void crearUsuarioSinIdentificadorLanzaExcepcionOperativa() {
        Response respuesta = mock(Response.class);
        when(respuesta.getStatus()).thenReturn(201);
        when(respuesta.getLocation()).thenReturn(URI.create("https://keycloak.example.com/admin/realms/piip/users/"));
        when(usuarios.create(any(UserRepresentation.class))).thenReturn(respuesta);

        assertThrows(KeycloakOperationException.class,
                () -> servicio.crearUsuarioDeshabilitado("ana@midagri.gob.pe", "Ana Pérez"));
    }

    @Test
    void desactivarUsuarioActualizaEnabledFalse() {
        UserRepresentation actual = new UserRepresentation();
        actual.setEnabled(true);
        when(usuarioRecurso.toRepresentation()).thenReturn(actual);

        servicio.desactivarUsuario("kc-1");

        ArgumentCaptor<UserRepresentation> captor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(usuarioRecurso).update(captor.capture());
        assertEquals(Boolean.FALSE, captor.getValue().isEnabled());
    }

    @Test
    void reactivarUsuarioActualizaEnabledTrue() {
        UserRepresentation actual = new UserRepresentation();
        actual.setEnabled(false);
        when(usuarioRecurso.toRepresentation()).thenReturn(actual);

        servicio.reactivarUsuario("kc-1");

        ArgumentCaptor<UserRepresentation> captor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(usuarioRecurso).update(captor.capture());
        assertEquals(Boolean.TRUE, captor.getValue().isEnabled());
    }

    @Test
    void desactivarUsuarioSinIdentificadorLanzaExcepcion() {
        assertThrows(KeycloakOperationException.class, () -> servicio.desactivarUsuario(""));
        verify(usuarios, times(0)).get(any(String.class));
    }

    @Test
    void falloDeKeycloakAlActualizarLanzaExcepcionOperativa() {
        when(usuarioRecurso.toRepresentation()).thenThrow(new RuntimeException("Fallo HTTP"));
        assertThrows(KeycloakOperationException.class, () -> servicio.desactivarUsuario("kc-1"));
    }

    @Test
    void beanFallaSiRealmNoConfigurado() {
        KeycloakAdminProperties propiedades = new KeycloakAdminProperties();
        propiedades.setRealm("   ");
        IllegalStateException excepcion = assertThrows(IllegalStateException.class,
                () -> new KeycloakAdminServiceImpl(keycloak, propiedades));
        assertTrue(excepcion.getMessage().contains("realm"));
    }

    @Test
    void crearUsuarioConFallaDeRedLanzaExcepcionOperativa() {
        when(usuarios.create(any(UserRepresentation.class)))
                .thenThrow(new RuntimeException("I/O error"));
        assertThrows(KeycloakOperationException.class,
                () -> servicio.crearUsuarioDeshabilitado("ana@midagri.gob.pe", "Ana"));
    }
}
