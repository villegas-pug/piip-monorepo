package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import java.time.LocalDate;

/**
 * Detalle canonico de una participacion (vigente o dada de baja)
 * en un proyecto del portafolio (US4, BR-035, BR-042). La
 * implementacion completa del servicio se entrega en T073; T072
 * declara el DTO para estabilizar el contrato HTTP del
 * {@code SeguimientoController}. No expone entidades JPA.
 */
public record ParticipanteResponse(
        long idParticipacion,
        long proyectoId,
        Long personaId,
        Long unidadId,
        String rol,
        String nombresCompletos,
        String institucion,
        String funcion,
        String estado,
        LocalDate fechaAlta,
        LocalDate fechaBaja,
        String etag) {
}
