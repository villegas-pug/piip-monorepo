package pe.gob.midagri.piip.portafolio.exception;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.config.ProblemDetailsConfig;
import pe.gob.midagri.piip.config.CorrelationIdFilter;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Advice exclusivo del modulo portafolio. Construye errores RFC 9457
 * sin filtrar datos internos y sin mezclar reglas de otros modulos.
 *
 * <p>El handler de {@link ResponseStatusException} extrae el codigo canonico del
 * {@code reason} cuando existe, de modo que las excepciones del servicio
 * ({@code INCORPORATION_CONFLICT_UNRESOLVED}, {@code DUPLICATE_INCORPORATION_HASH},
 * etc.) lleguen al cliente con el codigo funcional, no con un {@code HTTP_409}
 * opaco. La subclase tiene precedencia sobre el handler de la clase base.
 */
@RestControllerAdvice(basePackages = "pe.gob.midagri.piip.portafolio")
public class PortafolioExceptionHandler extends ProblemDetailsConfig.ModuleExceptionHandler {

    private static final URI TYPE_BASE = URI.create("https://piip.midagri.gob.pe/problems/");

    public PortafolioExceptionHandler(ProblemDetailsConfig.ProblemDetailsFactory factory) {
        super(factory);
    }

    @ExceptionHandler(PortafolioValidationException.class)
    public ResponseEntity<ProblemDetail> handlePortafolioValidation(
            PortafolioValidationException exception, HttpServletRequest request) {

        String raw = exception.getReason();
        String code = "VALIDATION_FAILED";
        String detail = raw;

        if (raw != null && raw.contains(":")) {
            code = raw.substring(0, raw.indexOf(':')).trim();
            detail = raw.substring(raw.indexOf(':') + 1).trim();
        }

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, detail);
        problem.setType(TYPE_BASE.resolve(code));
        problem.setTitle("Unprocessable Entity");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("correlationId", CorrelationIdFilter.getCorrelationId(request));
        problem.setProperty("traceId", CorrelationIdFilter.getCorrelationId(request));
        problem.setProperty("violations", List.of());

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(
            ResponseStatusException exception, HttpServletRequest request) {

        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String code = extractCode(exception.getReason());
        String detail = extractDetail(exception.getReason(), status);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(TYPE_BASE.resolve(code));
        problem.setTitle(status.getReasonPhrase());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("code", code);
        problem.setProperty("correlationId", CorrelationIdFilter.getCorrelationId(request));
        problem.setProperty("traceId", CorrelationIdFilter.getCorrelationId(request));
        problem.setProperty("violations", List.of());

        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(problem);
    }

    private String extractCode(String reason) {
        if (reason == null || reason.isBlank()) {
            return "HTTP_ERROR";
        }
        if (reason.contains(":")) {
            return reason.substring(0, reason.indexOf(':')).trim();
        }
        return reason.trim();
    }

    private String extractDetail(String reason, HttpStatus status) {
        if (reason == null || reason.isBlank()) {
            return status.getReasonPhrase();
        }
        int idx = reason.indexOf(':');
        if (idx >= 0) {
            String d = reason.substring(idx + 1).trim();
            return d.isEmpty() ? status.getReasonPhrase() : d;
        }
        // Si el reason parece un codigo canonico, usamos el status reason phrase como detail legible.
        if (reason.matches("[A-Z][A-Z0-9_]+")) {
            return status.getReasonPhrase();
        }
        return reason;
    }
}
