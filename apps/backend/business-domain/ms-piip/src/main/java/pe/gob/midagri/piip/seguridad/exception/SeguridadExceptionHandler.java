package pe.gob.midagri.piip.seguridad.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.gob.midagri.piip.config.ProblemDetailsConfig;
import pe.gob.midagri.piip.config.CorrelationIdFilter;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice(basePackages = "pe.gob.midagri.piip.seguridad")
public class SeguridadExceptionHandler extends ProblemDetailsConfig.ModuleExceptionHandler {
    public SeguridadExceptionHandler(ProblemDetailsConfig.ProblemDetailsFactory factory) { super(factory); }

    /**
     * Traduce un fallo recuperable de Keycloak a 503 con
     * {@code KEYCLOAK_OPERATION_RECOVERABLE}, exponiendo únicamente el
     * {@code operacionId}. No incluye contraseñas, tokens ni el cuerpo
     * de la respuesta original de Keycloak.
     */
    @ExceptionHandler(KeycloakRecoverableException.class)
    public ResponseEntity<Map<String, Object>> keycloakRecuperable(
            KeycloakRecoverableException excepcion, HttpServletRequest request) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("type", "https://api.piip.gob.pe/errors/keycloak-recoverable");
        cuerpo.put("title", "Operación Keycloak recuperable");
        cuerpo.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        cuerpo.put("code", "KEYCLOAK_OPERATION_RECOVERABLE");
        cuerpo.put("detail", "La identidad en Keycloak permanece deshabilitada; reintente la operación.");
        cuerpo.put("operacionId", excepcion.getOperacionId());
        cuerpo.put("correlationId", CorrelationIdFilter.getCorrelationId(request));
        cuerpo.put("traceId", CorrelationIdFilter.getCorrelationId(request));
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(cuerpo);
    }

    /**
     * Traduce excepciones de validación de negocio del módulo seguridad
     * a 422 (Unprocessable Entity) con Problem Details según RFC 9457.
     */
    @ExceptionHandler(SeguridadValidationException.class)
    public ResponseEntity<Map<String, Object>> validacionNegocio(
            SeguridadValidationException excepcion, HttpServletRequest request) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("type", "https://api.piip.gob.pe/errors/validation");
        cuerpo.put("title", "Error de validación de negocio");
        cuerpo.put("status", HttpStatus.UNPROCESSABLE_ENTITY.value());
        cuerpo.put("code", excepcion.getCode());
        cuerpo.put("detail", excepcion.getMessage());
        cuerpo.put("correlationId", CorrelationIdFilter.getCorrelationId(request));
        cuerpo.put("traceId", CorrelationIdFilter.getCorrelationId(request));
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .body(cuerpo);
    }
}
