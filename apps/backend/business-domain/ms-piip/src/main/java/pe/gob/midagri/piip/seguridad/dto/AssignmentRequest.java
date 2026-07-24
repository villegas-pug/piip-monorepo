package pe.gob.midagri.piip.seguridad.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

/** Comando de alta: función, perfil y unidad proceden exclusivamente de la matriz. */
public record AssignmentRequest(
        @NotNull Long usuarioId,
        @NotNull Long matrizCombinacionId,
        @NotNull LocalDate fechaInicio,
        LocalDate fechaFin,
        Long documentoFormalVersionId) {}
