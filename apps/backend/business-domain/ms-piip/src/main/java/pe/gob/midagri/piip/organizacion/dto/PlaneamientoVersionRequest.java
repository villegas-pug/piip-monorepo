package pe.gob.midagri.piip.organizacion.dto;
import java.time.LocalDate;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
public record PlaneamientoVersionRequest(@NotBlank @Size(max=30) String codigoVersion, @NotNull Long documentoAprobacionVersionId,
        @NotBlank @Size(max=200) String oficinaAprobadora, @NotNull LocalDate vigenteDesde, LocalDate vigenteHasta,
        @NotEmpty List<@Valid PlaneamientoItemRequest> items) {}
