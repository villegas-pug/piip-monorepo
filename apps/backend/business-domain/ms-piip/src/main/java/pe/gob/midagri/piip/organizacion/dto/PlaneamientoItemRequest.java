package pe.gob.midagri.piip.organizacion.dto;
import java.time.LocalDate;
import jakarta.validation.constraints.*;
public record PlaneamientoItemRequest(@NotBlank @Size(max=30) String codigo, @NotBlank @Size(max=500) String descripcion,
        @NotNull LocalDate vigenteDesde, LocalDate vigenteHasta) {}
