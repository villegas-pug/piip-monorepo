package pe.gob.midagri.piip.organizacion.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.gob.midagri.piip.config.ProblemDetailsConfig;

@RestControllerAdvice(basePackages = "pe.gob.midagri.piip.organizacion")
public class OrganizacionExceptionHandler extends ProblemDetailsConfig.ModuleExceptionHandler {
    public OrganizacionExceptionHandler(ProblemDetailsConfig.ProblemDetailsFactory factory) { super(factory); }
}
