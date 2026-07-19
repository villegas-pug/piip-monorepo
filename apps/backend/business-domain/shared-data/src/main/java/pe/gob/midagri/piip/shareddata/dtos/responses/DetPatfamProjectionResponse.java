package pe.gob.midagri.piip.shareddata.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DetPatfamProjectionResponse {

   private Integer idObjetivo;
   private String nombreObjetivo;
   private Integer idModulo;
   private String nombreModulo;
   private Integer idUnidad;
   private String nombreUnidad;
   private Integer idSesion;
   private String nombreSesion;
   private Integer idTaller;
   private String nombreTaller;
}
