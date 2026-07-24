package pe.gob.midagri.piip.seguridad.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración del cliente de Keycloak Admin API. Se limita al ciclo de
 * identidad constitucional; nunca incluye credenciales en claro: se obtienen
 * de variables de entorno gestionadas por OGTI. El realm de autenticación
 * del cliente puede ser {@code master} (cliente con privilegios) mientras
 * que el realm de los usuarios se mantiene en {@code piip.keycloak.realm}.
 */
@ConfigurationProperties(prefix = "piip.keycloak")
public class KeycloakAdminProperties {

    /**
     * Realm donde se aprovisionan los usuarios institucionales.
     */
    private String realm = "piip";

    private final Admin admin = new Admin();

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public Admin getAdmin() {
        return admin;
    }

    public static class Admin {

        /**
         * URL base del servidor Keycloak (sin el sufijo {@code /auth}).
         */
        private String serverUrl;

        /**
         * Realm contra el que se autentica el cliente administrador.
         */
        private String authRealm = "master";

        /**
         * Identificador del cliente con permiso de administración.
         */
        private String clientId;

        /**
         * Secreto del cliente administrador (gestionado por OGTI).
         */
        private String clientSecret;

        /**
         * Tipo de concesión OAuth2; el cliente de servicio usa
         * {@code client_credentials} por defecto.
         */
        private String grantType = "client_credentials";

        public String getServerUrl() {
            return serverUrl;
        }

        public void setServerUrl(String serverUrl) {
            this.serverUrl = serverUrl;
        }

        public String getAuthRealm() {
            return authRealm;
        }

        public void setAuthRealm(String authRealm) {
            this.authRealm = authRealm;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getGrantType() {
            return grantType;
        }

        public void setGrantType(String grantType) {
            this.grantType = grantType;
        }
    }
}
