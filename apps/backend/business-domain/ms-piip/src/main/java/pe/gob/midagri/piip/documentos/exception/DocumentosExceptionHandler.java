package pe.gob.midagri.piip.documentos.exception;

import java.net.URI;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.config.CorrelationIdFilter;
import pe.gob.midagri.piip.config.ProblemDetailsConfig;

@RestControllerAdvice(basePackages = "pe.gob.midagri.piip.documentos")
public class DocumentosExceptionHandler extends ProblemDetailsConfig.ModuleExceptionHandler {
    public DocumentosExceptionHandler(ProblemDetailsConfig.ProblemDetailsFactory factory) { super(factory); }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> handleResponseStatus(
            ResponseStatusException exception, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(exception.getStatusCode().value());
        String code = extractCode(exception.getReason());
        String detail = extractDetail(exception.getReason(), status);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create("https://piip.midagri.gob.pe/problems/").resolve(code));
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
        if (reason.matches("[A-Z][A-Z0-9_]+")) {
            return status.getReasonPhrase();
        }
        return reason;
    }
}
