package pe.gob.midagri.piip.shareddata.dtos.querys;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnexoRespuestaQuery {
   private Integer idPregunta;
   private Integer numAnexo;
   private Integer numGrupo;
   private Integer numPregunta;
   private String pregunta;
   private Integer idFase;
   private String fase;
   private Integer idPersonal;
   private String personal;
   private String opciones;
   private String tipoControl;
   private Integer idRespuesta;
   private String respuesta;
   private String observacion;
   private LocalDate fechaRegistra;

}
