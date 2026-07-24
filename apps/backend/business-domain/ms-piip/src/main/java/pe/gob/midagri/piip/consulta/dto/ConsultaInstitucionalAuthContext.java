package pe.gob.midagri.piip.consulta.dto;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * Contexto de autorización efectiva para la consulta
 * institucional. Concentra los datos que el módulo {@code consulta}
 * necesita para invocar
 * {@code AutorizacionEfectivaService#revalidarAsignacionInstitucional}
 * sin filtrar entidades JPA. La construcción se realiza a partir
 * de las cabeceras HTTP canónicas y del {@link Principal}
 * resuelto por el Resource Server.
 */
public record ConsultaInstitucionalAuthContext(
        String actorSub,
        Long actorUsuarioId,
        Long asignacionEfectivaId,
        String perfilEfectivo,
        String correlationId,
        List<Long> unidadesVisibles) {

    public static ConsultaInstitucionalAuthContext desde(
            Principal principal, Long asignacionEfectiva, Long actorUsuarioId,
            String perfilEfectivo, String correlationId, List<Long> unidadesVisibles) {
        return new ConsultaInstitucionalAuthContext(
                principal == null ? null : principal.getName(),
                actorUsuarioId,
                asignacionEfectiva,
                perfilEfectivo,
                correlationId,
                unidadesVisibles);
    }
}
