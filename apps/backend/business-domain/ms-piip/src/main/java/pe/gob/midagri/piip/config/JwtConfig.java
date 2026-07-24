package pe.gob.midagri.piip.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/** Validadores criptográficos y de claims mínimos requeridos para tokens de PIIP. */
@Configuration
public class JwtConfig {

    @Bean
    JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${piip.jwt.audience}") String audience) {
        String expectedIssuer = requiredConfiguration(issuerUri, "KEYCLOAK_ISSUER_URI");
        String expectedAudience = requiredConfiguration(audience, "PIIP_JWT_AUDIENCE");
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(expectedIssuer);

        OAuth2TokenValidator<Jwt> audienceValidator = jwt -> jwt.getAudience().stream()
                .anyMatch(expectedAudience::equals)
                ? OAuth2TokenValidatorResult.success()
                : invalidToken("La audiencia del token no es válida.");
        OAuth2TokenValidator<Jwt> subjectValidator = jwt -> {
            String subject = jwt.getSubject();
            return subject != null && !subject.isBlank()
                    ? OAuth2TokenValidatorResult.success()
                    : invalidToken("El token no contiene una identidad válida.");
        };

        // El validador predeterminado verifica vigencia; el decoder verifica firma contra el JWK del issuer.
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(expectedIssuer), audienceValidator, subjectValidator));
        return decoder;
    }

    private static OAuth2TokenValidatorResult invalidToken(String description) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", description, null));
    }

    private static String requiredConfiguration(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Debe configurarse la variable externa " + variableName + ".");
        }
        return value.trim();
    }
}
