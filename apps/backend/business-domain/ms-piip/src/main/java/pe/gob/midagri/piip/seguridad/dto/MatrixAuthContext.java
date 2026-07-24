package pe.gob.midagri.piip.seguridad.dto;

public record MatrixAuthContext(String actorSub, Long asignacionEfectivaId, Long unidadEfectivaId,
        String correlacionId) {
}
