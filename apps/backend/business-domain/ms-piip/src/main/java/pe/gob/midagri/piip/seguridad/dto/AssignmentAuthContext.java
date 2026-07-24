package pe.gob.midagri.piip.seguridad.dto;

/** Contexto autenticado; los valores de perfil no proceden del cliente. */
public record AssignmentAuthContext(String actorSub, Long asignacionEfectivaId, Long unidadEfectivaId,
        String correlacionId) {}
