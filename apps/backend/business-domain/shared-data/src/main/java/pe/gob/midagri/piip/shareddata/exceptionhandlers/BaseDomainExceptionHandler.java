package pe.gob.midagri.piip.shareddata.exceptionhandlers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.gob.midagri.piip.shareddata.dtos.responses.ApiResponse;
import pe.gob.midagri.piip.shareddata.enums.ApiResponseStatus;
import pe.gob.midagri.piip.shareddata.exceptions.NotFoundByIdException;
import pe.gob.midagri.piip.shareddata.exceptions.NotFoundException;

@RestControllerAdvice
public class BaseDomainExceptionHandler {

	@ExceptionHandler(value = { NotFoundException.class })
	public ResponseEntity<?> handleNotFoundException(Exception e) {

		ApiResponseStatus apiStatus = ApiResponseStatus.NO_CONTENT;
		return ResponseEntity
				.status(apiStatus.getCode())
				.body(
						ApiResponse
								.builder()
								.message(apiStatus.getMessage())
								.code(apiStatus.getCode())
								.build());

	}

	@ExceptionHandler(value = { NotFoundByIdException.class })
	public ResponseEntity<?> handleNotFoundByIdException(Exception e) {
		ApiResponseStatus apiStatus = ApiResponseStatus.NO_CONTENT_BY_ID;
		apiStatus.setMessage(e.getMessage());
		return ResponseEntity
				.status(apiStatus.getCode())
				.body(ApiResponse.builder().code(apiStatus.getCode()).message(apiStatus.getMessage()).build());

	}

}
