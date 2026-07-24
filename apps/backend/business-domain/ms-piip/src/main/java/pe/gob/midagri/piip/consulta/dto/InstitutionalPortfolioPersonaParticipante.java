package pe.gob.midagri.piip.consulta.dto;

import java.time.LocalDate;

/**
 * Persona participante del registro de portafolio. Solo se
 * incluye en la respuesta de detalle cuando el actor es
 * Responsable del registro, Evaluador o administrador autorizado,
 * conforme al contrato de privacidad del módulo
 * {@code consulta} y a la Constitución 5.0.0.
 */
public record InstitutionalPortfolioPersonaParticipante(
        Long idParticipacion,
        Long participanteId,
        Long usuarioId,
        String nombresCompletos,
        String institucion,
        String funcion,
        String clasificacion,
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean vigente) { }
