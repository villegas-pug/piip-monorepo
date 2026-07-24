package pe.gob.midagri.piip.seguridad.service.impl;

import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import pe.gob.midagri.piip.seguridad.config.KeycloakAdminProperties;
import pe.gob.midagri.piip.seguridad.exception.KeycloakOperationException;
import pe.gob.midagri.piip.seguridad.service.KeycloakAdminService;

/**
 * Adaptador único de Keycloak Admin API. Reside exclusivamente en este
 * paquete y es el único punto del monolito modular que importa
 * {@code org.keycloak.*}. La activación inicial exige la acción de correo
 * de Keycloak; nunca se asigna contraseña desde PIIP.
 */
@Service
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    private static final Pattern KEYCLOAK_ID_PATH = Pattern.compile(".*/([^/]+)$");

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakAdminServiceImpl(Keycloak keycloak, KeycloakAdminProperties propiedades) {
        this.keycloak = keycloak;
        this.realm = propiedades.getRealm();
        if (!StringUtils.hasText(this.realm)) {
            throw new IllegalStateException("El realm de usuarios de Keycloak no está configurado.");
        }
    }

    @Override
    public KeycloakUserCreation crearUsuarioDeshabilitado(String correo, String nombreCompleto) {
        UserRepresentation usuario = new UserRepresentation();
        usuario.setUsername(correo);
        usuario.setEmail(correo);
        usuario.setEnabled(false);
        usuario.setEmailVerified(false);
        usuario.setRequiredActions(Collections.singletonList("VERIFY_EMAIL"));
        if (StringUtils.hasText(nombreCompleto)) {
            usuario.setFirstName(nombreCompleto);
        }

        try (Response respuesta = keycloak.realm(realm).users().create(usuario)) {
            int estado = respuesta.getStatus();
            if (estado != Response.Status.CREATED.getStatusCode()) {
                throw new KeycloakOperationException(
                        "Keycloak rechazó la creación del usuario con estado " + estado + ".");
            }
            String keycloakId = extraerIdentificador(respuesta.getLocation());
            if (!StringUtils.hasText(keycloakId)) {
                throw new KeycloakOperationException(
                        "Keycloak no devolvió el identificador del usuario creado.");
            }
            return new KeycloakUserCreation(keycloakId, true);
        } catch (RuntimeException excepcion) {
            if (excepcion instanceof KeycloakOperationException) {
                throw excepcion;
            }
            throw new KeycloakOperationException(
                    "Fallo al invocar Keycloak Admin API durante la creación del usuario.", excepcion);
        }
    }

    @Override
    public void desactivarUsuario(String keycloakId) {
        actualizarEstado(keycloakId, false);
    }

    @Override
    public void reactivarUsuario(String keycloakId) {
        actualizarEstado(keycloakId, true);
    }

    private void actualizarEstado(String keycloakId, boolean habilitado) {
        if (!StringUtils.hasText(keycloakId)) {
            throw new KeycloakOperationException(
                    "No se puede modificar la identidad en Keycloak sin un identificador válido.");
        }
        try {
            UserRepresentation actual = keycloak.realm(realm).users().get(keycloakId).toRepresentation();
            actual.setEnabled(habilitado);
            keycloak.realm(realm).users().get(keycloakId).update(actual);
        } catch (RuntimeException excepcion) {
            throw new KeycloakOperationException(
                    "Fallo al modificar la identidad en Keycloak (habilitado=" + habilitado + ").",
                    excepcion);
        }
    }

    private static String extraerIdentificador(java.net.URI ubicacion) {
        if (ubicacion == null || !StringUtils.hasText(ubicacion.getPath())) {
            return null;
        }
        Matcher matcher = KEYCLOAK_ID_PATH.matcher(ubicacion.getPath());
        return matcher.matches() ? matcher.group(1) : null;
    }
}
