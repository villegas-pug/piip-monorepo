package pe.gob.midagri.piip.seguridad.dto;

/**
 * Resultado de una transición de estado de usuario. Expone únicamente el
 * identificador PIIP, el estado canónico y el {@code keycloakId}; nunca
 * incluye contraseña, token ni atributos sensibles del Keycloak.
 */
public record UserStatusResult(Long usuarioId, String estado, String keycloakId) {
}
