package pe.gob.midagri.piip.seguridad.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.NotNull;

/** El cambio no permite alterar la combinación matricial ni el ámbito. */
public record AssignmentChangeRequest(@NotNull LocalDate fechaInicio, LocalDate fechaFin) {}
