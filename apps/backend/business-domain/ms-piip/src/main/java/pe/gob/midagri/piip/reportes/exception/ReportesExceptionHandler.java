package pe.gob.midagri.piip.reportes.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.gob.midagri.piip.config.ProblemDetailsConfig;

@RestControllerAdvice(basePackages = "pe.gob.midagri.piip.reportes")
public class ReportesExceptionHandler extends ProblemDetailsConfig.ModuleExceptionHandler {
    public ReportesExceptionHandler(ProblemDetailsConfig.ProblemDetailsFactory factory) { super(factory); }
}
