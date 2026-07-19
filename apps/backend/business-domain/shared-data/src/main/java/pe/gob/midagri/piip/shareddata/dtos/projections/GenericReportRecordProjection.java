package pe.gob.midagri.piip.shareddata.dtos.projections;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenericReportRecordProjection {

   private Long rowGroup;
   private String fieldName;
   private String fieldType;
   private String value;

}
