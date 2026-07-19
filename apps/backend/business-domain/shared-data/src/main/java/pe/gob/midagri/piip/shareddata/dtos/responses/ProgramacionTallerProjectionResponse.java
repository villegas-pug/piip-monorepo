package pe.gob.midagri.piip.shareddata.dtos.responses;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProgramacionTallerProjectionResponse {
   private Long idProgTaller;
   private String tema;
   private String actividad;
   private String nombreTaller;
   private String descripcionTaller;
   private LocalDateTime fecHoraInicio;
   private LocalDateTime fecHoraFin;
   private LocalDate fechaRegistra;
   private String dictadoPor;
   private Integer totalFamilias;
   private Integer estado;
   private Integer eliminado;
}
