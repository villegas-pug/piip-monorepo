package pe.gob.midagri.piip.portafolio.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Detalle HTTP de la subsanacion unica de una iniciativa.
 *
 * <p>Expone el plazo, los incumplimientos declarados por el Evaluador, la
 * fecha de apertura, la fecha de atencion (cuando exista) y la version
 * optimista para construir el ETag. El actorSub se conserva como
 * referencia auditable minima; las entidades JPA no salen del modulo.
 */
public record SubsanacionDetail(
        Long id,
        Long iniciativaId,
        LocalDate plazo,
        String incumplimientos,
        LocalDateTime aperturaEn,
        LocalDateTime atencionEn,
        String actorSub,
        Long version,
        String etag
) {}
