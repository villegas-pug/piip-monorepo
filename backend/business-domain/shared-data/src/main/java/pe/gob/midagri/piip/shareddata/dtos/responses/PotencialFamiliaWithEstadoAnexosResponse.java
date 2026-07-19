package pe.gob.midagri.piip.shareddata.dtos.responses;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PotencialFamiliaWithEstadoAnexosResponse {

   private Long idFamilia;
   private String codFamilia;
   private List<EstadoAnexo> estadoAnexos;

   @Data
   @Builder
   @NoArgsConstructor
   @AllArgsConstructor
   public static class EstadoAnexo {

      // private Integer idServicio;
      // private String resultadoFinal;
      private Integer numAnexo;
      private Integer estado;
      private Integer etapa;

   }

}
