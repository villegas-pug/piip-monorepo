package pe.gob.midagri.piip.seguridad.service.impl;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import pe.gob.midagri.piip.seguridad.config.KeycloakAdminProperties;

/**
 * Configuración del cliente administrativo de Keycloak limitado al ciclo
 * de identidad. El bean reside en {@code seguridad/service/impl/} para
 * cumplir la regla arquitectónica que restringe el acceso a
 * {@code org.keycloak.*} al adaptador de identidad. Los secretos llegan
 * exclusivamente de variables de entorno gestionadas por OGTI.
 */
@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class KeycloakConfig {

    /**
     * Construye el cliente administrativo de Keycloak. Falla al iniciar
     * si faltan parámetros críticos: la aplicación no puede operar sin
     * un cliente administrador válido para el ciclo de identidad.
     */
    @Bean(destroyMethod = "close")
    public Keycloak keycloakAdminClient(KeycloakAdminProperties propiedades) {
        KeycloakAdminProperties.Admin admin = propiedades.getAdmin();
        validarObligatorio(admin.getServerUrl(), "piip.keycloak.admin.server-url");
        validarObligatorio(admin.getClientId(), "piip.keycloak.admin.client-id");
        validarObligatorio(admin.getClientSecret(), "piip.keycloak.admin.client-secret");
        validarObligatorio(propiedades.getRealm(), "piip.keycloak.realm");
        validarObligatorio(admin.getAuthRealm(), "piip.keycloak.admin.auth-realm");
        return KeycloakBuilder.builder()
                .serverUrl(admin.getServerUrl())
                .realm(admin.getAuthRealm())
                .clientId(admin.getClientId())
                .clientSecret(admin.getClientSecret())
                .grantType(resolverGrantType(admin.getGrantType()))
                .build();
    }

    private static String resolverGrantType(String configurado) {
        if (!StringUtils.hasText(configurado)) {
            return OAuth2Constants.CLIENT_CREDENTIALS;
        }
        return configurado;
    }

    private static void validarObligatorio(String valor, String propiedad) {
        if (!StringUtils.hasText(valor)) {
            throw new IllegalStateException(
                    "La propiedad obligatoria '" + propiedad + "' no está configurada para el cliente de Keycloak.");
        }
    }
}
