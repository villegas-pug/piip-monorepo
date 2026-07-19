package pe.gob.midagri.piip.tipodocumento.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.gob.midagri.piip.shareddata.dtos.responses.ApiResponse;

@RestControllerAdvice(basePackages = "pe.gob.midagri.piip.tipodocumento")
public class TipoDocumentoExceptionHandler {

    @ExceptionHandler(TipoDocumentoNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(TipoDocumentoNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.<Void>builder()
                        .code(HttpStatus.NOT_FOUND.value())
                        .message(exception.getMessage())
                        .build());
    }
}
