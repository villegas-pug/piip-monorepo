package pe.gob.midagri.piip.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

@Configuration
@OpenAPIDefinition(info = @Info(title = "API PIIP", version = "v1", description = "API interna de la Plataforma de Innovación Pública"))
public class OpenApiConfig {
}
