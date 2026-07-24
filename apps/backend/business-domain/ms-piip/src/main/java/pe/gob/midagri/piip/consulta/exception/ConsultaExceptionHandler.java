package pe.gob.midagri.piip.consulta.exception;

import org.springframework.web.bind.annotation.RestControllerAdvice;

import pe.gob.midagri.piip.config.ProblemDetailsConfig;

@RestControllerAdvice(basePackages = "pe.gob.midagri.piip.consulta")
public class ConsultaExceptionHandler extends ProblemDetailsConfig.ModuleExceptionHandler {
    public ConsultaExceptionHandler(ProblemDetailsConfig.ProblemDetailsFactory factory) { super(factory); }
}
