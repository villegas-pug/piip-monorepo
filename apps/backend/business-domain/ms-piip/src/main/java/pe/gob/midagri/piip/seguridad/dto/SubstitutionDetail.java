package pe.gob.midagri.piip.seguridad.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Vista de contrato de una suplencia; no expone entidades JPA. */
public record SubstitutionDetail(
        Long id,
        Long asignacionTitularId,
        Long asignacionSuplenteId,
        Long suplenteUsuarioId,
        LocalDate inicio,
        LocalDate fin,
        Long autoridadUsuarioId,
        Long documentoFormalVersionId,
        LocalDateTime terminadaEn) {
}
