package pe.gob.midagri.piip.seguridad.dto;

import pe.gob.midagri.piip.seguridad.entity.EstadoOperacionAprovisionamiento;

/**
 * Resultado idempotente del aprovisionamiento Keycloak-first. {@code recuperable}
 * indica si la operación admite reintento (solo en estados
 * {@code KEYCLOAK_CREADO_DESHABILITADO} u {@code ORACLE_PENDIENTE}); no incluye
 * contraseñas, tokens ni atributos sensibles.
 */
public record ProvisioningResult(
        Long operacionId,
        Long usuarioId,
        EstadoOperacionAprovisionamiento estado,
        boolean recuperable,
        int intento) {
}
