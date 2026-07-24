package pe.gob.midagri.piip.reportes.dto;

/**
 * Contexto de autorización efectiva para el módulo
 * de reportes. Conserva los mismos campos que el
 * contexto del portafolio: la constitución exige
 * revalidar la asignación efectiva contra Oracle
 * antes de cualquier operación sensible, y el
 * evaluador efectivo debe coincidir con
 * {@code "Evaluador"} para generar reportes.
 */
public record ReporteAuthContext(
        String actorSub,
        Long actorUsuarioId,
        Long asignacionEfectivaId,
        String perfilEfectivo,
        Long unidadEfectivaId,
        String correlacionId) {
}
