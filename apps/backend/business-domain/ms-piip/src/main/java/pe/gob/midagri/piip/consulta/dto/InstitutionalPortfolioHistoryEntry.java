package pe.gob.midagri.piip.consulta.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import pe.gob.midagri.piip.consulta.dto.EstadoIniciativaConsulta;

/**
 * Entrada del historial de transiciones de estado de un registro
 * del portafolio. La consulta institucional expone el historial
 * completo (incluidos los campos 13, 14, 15, 16, 18, 19, 20 y 21
 * derivados) sin incluir la auditoría inmutable, que pertenece
 * al módulo {@code auditoria}.
 */
public record InstitutionalPortfolioHistoryEntry(
        Long transicionId,
        EstadoIniciativaConsulta estadoAnterior,
        EstadoIniciativaConsulta estadoNuevo,
        Long usuarioId,
        Integer rolEfectivoId,
        Long unidadEfectivaId,
        LocalDateTime fechaTransicion,
        String observaciones,
        Long documentoRefId) { }
