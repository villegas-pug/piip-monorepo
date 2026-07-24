package pe.gob.midagri.piip.config;

import java.math.BigDecimal;
import java.util.List;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.HeaderParameter;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;

/** Metadatos OpenAPI código-first para la superficie REST versionada de PIIP. */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_AUTH = "bearerAuth";

    @Bean
    OpenAPI piipOpenApi() {
        Components components = new Components()
                .addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("JWT validado por el Resource Server de PIIP."))
                .addParameters("CorrelationId", correlationIdParameter())
                .addParameters("EffectiveAssignmentId", effectiveAssignmentParameter())
                .addSchemas("ProblemDetail", problemDetailSchema())
                .addSchemas("Page", pageSchema());
        return new OpenAPI()
                .info(new Info().title("API PIIP").version("v1")
                        .description("API REST de la Plataforma de Innovación Pública."))
                .servers(List.of(new Server().url("/api/v1").description("API versionada PIIP")))
                .components(components);
    }

    @Bean
    OpenApiCustomizer piipOperationCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, pathItem) -> pathItem.readOperations().forEach(operation ->
                        configureOperation(path, operation)));
            }
        };
    }

    private void configureOperation(String path, Operation operation) {
        operation.addParametersItem(new HeaderParameter().$ref("#/components/parameters/CorrelationId"));
        if (path.startsWith("/consulta/publica")) {
            return;
        }
        operation.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
        operation.addParametersItem(new HeaderParameter().$ref("#/components/parameters/EffectiveAssignmentId"));
    }

    private HeaderParameter correlationIdParameter() {
        return (HeaderParameter) new HeaderParameter().name(ApiHeaders.CORRELATION_ID).required(false)
                .description("Identificador de correlación propagado o generado por PIIP.")
                .schema(new StringSchema());
    }

    private HeaderParameter effectiveAssignmentParameter() {
        return (HeaderParameter) new HeaderParameter().name(ApiHeaders.EFFECTIVE_ASSIGNMENT_ID).required(true)
                .description("Asignación efectiva que PIIP revalida en Oracle.")
                .schema(new StringSchema());
    }

    private ObjectSchema problemDetailSchema() {
        return (ObjectSchema) new ObjectSchema()
                .addProperty("type", new StringSchema().format("uri"))
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("code", new StringSchema())
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema().format("uri"))
                .addProperty("correlationId", new StringSchema())
                .addProperty("violations", new ArraySchema().items(new ObjectSchema()));
    }

    private ObjectSchema pageSchema() {
        return (ObjectSchema) new ObjectSchema()
                .addProperty("items", new ArraySchema().items(new ObjectSchema()))
                .addProperty("page", new IntegerSchema()._default(0).minimum(BigDecimal.ZERO))
                .addProperty("size", new IntegerSchema()._default(20).minimum(BigDecimal.ONE)
                        .maximum(BigDecimal.valueOf(100)))
                .addProperty("totalElements", new IntegerSchema().minimum(BigDecimal.ZERO))
                .addProperty("totalPages", new IntegerSchema().minimum(BigDecimal.ZERO));
    }
}
