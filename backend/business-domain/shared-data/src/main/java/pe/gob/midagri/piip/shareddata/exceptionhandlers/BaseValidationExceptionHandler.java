package pe.gob.midagri.piip.shareddata.exceptionhandlers;

import java.util.HashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import jakarta.validation.ConstraintViolationException;
import pe.gob.midagri.piip.shareddata.dtos.responses.ApiResponse;
import pe.gob.midagri.piip.shareddata.enums.ApiResponseStatus;

@RestControllerAdvice
@Validated
public class BaseValidationExceptionHandler {

      @ExceptionHandler(value = { MethodArgumentNotValidException.class })
      public ResponseEntity<?> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {

            Map<String, String> errors = new HashMap<>();
            e.getBindingResult().getFieldErrors().forEach(error -> {
                  String field = error.getField();
                  String message = error.getDefaultMessage();
                  errors.put(field, message);
            });

            return ResponseEntity
                        .status(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getCode())
                        .body(
                                    ApiResponse
                                                .builder()
                                                .message(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getMessage())
                                                .code(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getCode())
                                                .data(errors)
                                                .build());
      }

      @ExceptionHandler(value = { ConstraintViolationException.class })
      public ResponseEntity<?> handleConstraintViolationException(ConstraintViolationException e) {

            Map<String, String> errors = new HashMap<>();

            e.getConstraintViolations().forEach(violation -> {
                  String field = violation.getPropertyPath().toString().split("\\.")[2];
                  String message = violation.getMessage();
                  errors.put(field, message);
            });

            return ResponseEntity
                        .status(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getCode())
                        .body(
                                    ApiResponse
                                                .builder()
                                                .message(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getMessage())
                                                .code(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getCode())
                                                .data(errors)
                                                .build());
      }

      @ExceptionHandler(value = { MissingServletRequestParameterException.class })
      public ResponseEntity<?> handleMissingServletRequestParameterException(
                  MissingServletRequestParameterException e) {
            Map<String, String> errParam = Map.of(e.getParameterName(), "¡Parametro requerido!");
            return ResponseEntity
                        .status(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getCode())
                        .body(
                                    ApiResponse
                                                .builder()
                                                .message(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getMessage())
                                                .code(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getCode())
                                                .data(errParam)
                                                .build());
      }

      @ExceptionHandler(value = { MissingServletRequestPartException.class })
      public ResponseEntity<?> handleMissingServletRequestPartException(
                  MissingServletRequestPartException e) {
            Map<String, String> errParam = Map.of(e.getRequestPartName(), "¡Parametro requerido!");
            return ResponseEntity
                        .status(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getCode())
                        .body(
                                    ApiResponse
                                                .builder()
                                                .message(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getMessage())
                                                .code(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getCode())
                                                .data(errParam)
                                                .build());
      }

      @ExceptionHandler(MultipartException.class)
      public ResponseEntity<?> handleMultipartException(MultipartException ex) {
            Map<String, String> errParam = Map.of("Error", "¡La petición debe ser de tipo multipart/form-data!");
            return ResponseEntity
                        .status(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getCode())
                        .body(
                                    ApiResponse
                                                .builder()
                                                .message(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getMessage())
                                                .code(ApiResponseStatus.BAD_REQUEST_FAIL_VALIDATION.getCode())
                                                .data(errParam)
                                                .build());
      }
}
