package pe.gob.midagri.piip.auditoria.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.gob.midagri.piip.config.ProblemDetailsConfig;

@RestControllerAdvice(basePackages = "pe.gob.midagri.piip.auditoria")
public class AuditoriaExceptionHandler extends ProblemDetailsConfig.ModuleExceptionHandler {
    public AuditoriaExceptionHandler(ProblemDetailsConfig.ProblemDetailsFactory factory) { super(factory); }
}
