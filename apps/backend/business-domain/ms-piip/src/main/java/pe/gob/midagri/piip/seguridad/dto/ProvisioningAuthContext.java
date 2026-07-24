package pe.gob.midagri.piip.seguridad.dto;

/**
 * Contexto de autorización efectivo para aprovisionamiento y revalidación
 * de alcance. El {@code actorSub} proviene del JWT validado por el resource
 * server; la asignación efectiva y la unidad son las que Oracle revalida.
 */
public record ProvisioningAuthContext(
        String actorSub,
        Long asignacionEfectivaId,
        Long unidadEfectivaId,
        String correlationId) {
}
