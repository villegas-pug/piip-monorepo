package pe.gob.midagri.piip.portafolio.dto;

/**
 * Contexto de autorización efectiva para el módulo portafolio.
 */
public record PortafolioAuthContext(
        String actorSub,
        Long actorUsuarioId,
        Long asignacionEfectivaId,
        String perfilEfectivo,
        Long unidadEfectivaId,
        Long unidadRecursoId,
        String correlacionId
) {}
