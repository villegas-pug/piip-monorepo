package pe.gob.midagri.piip.shareddata.dtos.responses;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

   private @Builder.Default int code = HttpStatus.OK.value();
   private String message;
   private @Builder.Default T data = null;
   private Long totalRows;
   private Integer page;
   private Integer rowsPerPage;

}
