package pe.gob.midagri.piip.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

/** Configuración de autenticación JWT; la autorización PIIP se resuelve en Oracle. */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            ProblemDetailsConfig.ProblemDetailsFactory problemDetailsFactory) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/consulta/publica/**").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint((request, response, exception) ->
                                problemDetailsFactory.write(response, request, HttpStatus.UNAUTHORIZED,
                                        "AUTHENTICATION_REQUIRED", "Autenticación requerida."))
                        .accessDeniedHandler((request, response, exception) ->
                                problemDetailsFactory.write(response, request, HttpStatus.FORBIDDEN,
                                        "ACCESS_DENIED", "Acceso denegado."))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(this::toAuthenticatedPrincipal)))
                .build();
    }

    private AbstractAuthenticationToken toAuthenticatedPrincipal(Jwt jwt) {
        // Los claims Keycloak prueban identidad, no conceden permisos PIIP.
        return new JwtAuthenticationToken(jwt, List.of(), jwt.getSubject());
    }
}
