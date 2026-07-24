package pe.gob.midagri.piip.config;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/** Construye el contrato canónico de errores HTTP sin revelar detalles internos. */
@Configuration
public class ProblemDetailsConfig {

    @Bean
    ProblemDetailsFactory problemDetailsFactory() {
        return new ProblemDetailsFactory();
    }

    /** Factoría reutilizable para los límites de error de cada módulo constitucional. */
    public static final class ProblemDetailsFactory {
        private static final URI TYPE_BASE = URI.create("https://piip.midagri.gob.pe/problems/");

        public ResponseEntity<ProblemDetail> response(
                HttpServletRequest request, HttpStatus status, String code, String detail) {
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .body(problem(request, status, code, detail, List.of()));
        }

        public ResponseEntity<ProblemDetail> validationResponse(
                HttpServletRequest request, MethodArgumentNotValidException exception) {
            List<Map<String, String>> violations = exception.getBindingResult().getFieldErrors().stream()
                    .map(this::violation)
                    .toList();
            ProblemDetail problem = problem(request, HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                    "La solicitud contiene campos inválidos.", violations);
            return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(problem);
        }

        public void write(HttpServletResponse response, HttpServletRequest request,
                HttpStatus status, String code, String detail) throws IOException {
            response.setStatus(status.value());
            response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
            response.setHeader(ApiHeaders.CORRELATION_ID, CorrelationIdFilter.getCorrelationId(request));
            String body = "{\"type\":\"" + TYPE_BASE + code + "\",\"title\":\""
                    + status.getReasonPhrase() + "\",\"status\":" + status.value() + ",\"code\":\""
                    + code + "\",\"detail\":\"" + detail + "\",\"instance\":\""
                    + request.getRequestURI() + "\",\"correlationId\":\""
                    + CorrelationIdFilter.getCorrelationId(request) + "\",\"traceId\":\""
                    + CorrelationIdFilter.getCorrelationId(request) + "\",\"violations\":[]}";
            response.getWriter().write(body);
        }

        private ProblemDetail problem(HttpServletRequest request, HttpStatus status, String code,
                String detail, List<?> violations) {
            ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
            problem.setType(TYPE_BASE.resolve(code));
            problem.setTitle(status.getReasonPhrase());
            problem.setInstance(URI.create(request.getRequestURI()));
            problem.setProperty("code", code);
            problem.setProperty("correlationId", CorrelationIdFilter.getCorrelationId(request));
            problem.setProperty("traceId", CorrelationIdFilter.getCorrelationId(request));
            problem.setProperty("violations", violations);
            return problem;
        }

        private Map<String, String> violation(FieldError error) {
            return Map.of("field", error.getField(), "code", error.getCode() == null ? "INVALID" : error.getCode());
        }
    }

    /** Base sin anotación para conservar el mismo contrato en cada advice de módulo. */
    public abstract static class ModuleExceptionHandler {
        private final ProblemDetailsFactory problemDetailsFactory;

        protected ModuleExceptionHandler(ProblemDetailsFactory problemDetailsFactory) {
            this.problemDetailsFactory = problemDetailsFactory;
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ProblemDetail> handleValidation(
                MethodArgumentNotValidException exception, HttpServletRequest request) {
            return problemDetailsFactory.validationResponse(request, exception);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ProblemDetail> handleUnreadableBody(
                HttpMessageNotReadableException exception, HttpServletRequest request) {
            return problemDetailsFactory.response(request, HttpStatus.BAD_REQUEST, "REQUEST_NOT_READABLE",
                    "La solicitud no tiene un formato válido.");
        }

        @ExceptionHandler(MissingRequestHeaderException.class)
        public ResponseEntity<ProblemDetail> handleMissingRequestHeader(
                MissingRequestHeaderException exception, HttpServletRequest request) {
            String headerName = exception.getHeaderName();
            if ("If-Match".equalsIgnoreCase(headerName)) {
                return problemDetailsFactory.response(request, HttpStatus.PRECONDITION_REQUIRED,
                        "IF_MATCH_REQUIRED", "La solicitud requiere el header If-Match.");
            }
            return problemDetailsFactory.response(request, HttpStatus.BAD_REQUEST, "HEADER_REQUIRED",
                    "Falta el header obligatorio: " + headerName + ".");
        }

        @ExceptionHandler(ResponseStatusException.class)
        public ResponseEntity<ProblemDetail> handleResponseStatus(
                ResponseStatusException exception, HttpServletRequest request) {
            HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
            return problemDetailsFactory.response(request, status, "HTTP_" + status.value(),
                    "La solicitud no puede completarse.");
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ProblemDetail> handleUnexpected(Exception exception, HttpServletRequest request) {
            return problemDetailsFactory.response(request, HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                    "Ocurrió un error al procesar la solicitud.");
        }
    }
}
