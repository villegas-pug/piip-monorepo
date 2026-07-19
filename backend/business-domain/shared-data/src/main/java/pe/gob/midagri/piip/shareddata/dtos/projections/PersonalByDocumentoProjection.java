package pe.gob.midagri.piip.shareddata.dtos.projections;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PersonalByDocumentoProjection {
   private Long idPersona;
   private Long idPersonal;
   private String nombres;
   private String primerApe;
   private String segundoApe;
   private String documento;
   private String numeroDoc;
   private String nacionalidad;
   private String estadoCivil;
   private String sexo;
   private LocalDate fecNacimiento;
   private String telefono;
   private String correo;
   private String direccion;
   private String referencia;
   private String carrera;
}
