package pe.gob.midagri.piip.seguridad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Verifica el contrato de configuración sin descubrir JWK ni conectarse a Keycloak. */
class JwtContractTest {
    @Test
    void resourceServerExigeIssuerAudienceFirmaVigenciaYSubSinDerivarRolesPiipDelJwt() throws Exception {
        String source = Files.readString(Path.of("src/main/java/pe/gob/midagri/piip/config/JwtConfig.java"));
        String security = Files.readString(Path.of("src/main/java/pe/gob/midagri/piip/config/SecurityConfig.java"));

        assertTrue(source.contains("JwtDecoders.fromIssuerLocation"));
        assertTrue(source.contains("JwtValidators.createDefaultWithIssuer"));
        assertTrue(source.contains("jwt.getAudience()"));
        assertTrue(source.contains("jwt.getSubject()"));
        assertTrue(source.contains("decoder.setJwtValidator"));
        assertTrue(security.contains("new JwtAuthenticationToken(jwt, List.of(), jwt.getSubject())"));
        assertFalse(security.contains("realm_access"));
        assertFalse(security.contains("resource_access"));
    }
}
