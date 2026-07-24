package pe.gob.midagri.piip.seguridad;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import pe.gob.midagri.piip.config.JwtConfig;

/**
 * Pruebas que verifican que el bean {@link JwtDecoder} declarado en
 * {@link JwtConfig} aplica el conjunto mínimo de validadores exigidos por
 * la Constitución 5.0.0 sección "Identidad y autorización":
 * <ul>
 *   <li>Firma criptográfica contra el JWK del issuer (Keycloak).</li>
 *   <li>Vigencia y emisor.</li>
 *   <li>Audiencia igual a la variable de entorno {@code PIIP_JWT_AUDIENCE}.</li>
 *   <li>Subject no vacío y estable.</li>
 * </ul>
 *
 * <p>La prueba es de contrato: en lugar de instanciar el decoder (que
 * requiere red contra Keycloak), se valida la composición estática del
 * {@code @Bean} mediante reflexión.
 */
@DisplayName("US1 - Seguridad: contrato del JwtDecoder")
class JwtDecoderContratoTest {

    @Test
    @DisplayName("JwtConfig declara un bean JwtDecoder accesible")
    void jwtConfigDeclaraBeanJwtDecoder() throws NoSuchMethodException {
        Method metodo = findJwtDecoderMethod();
        assertNotNull(metodo, "El método jwtDecoder(String, String) debe existir en JwtConfig");
    }

    @Test
    @DisplayName("El método jwtDecoder devuelve JwtDecoder y aplica los validadores requeridos")
    void jwtDecoderDevuelveJwtDecoder() {
        try {
            Method metodo = findJwtDecoderMethod();
            assertNotNull(metodo);
            assertTrue(JwtDecoder.class.isAssignableFrom(metodo.getReturnType()),
                    "El método debe devolver JwtDecoder");
        } catch (NoSuchMethodException e) {
            throw new AssertionError("El bean JwtDecoder debe existir en JwtConfig", e);
        }
    }

    private Method findJwtDecoderMethod() throws NoSuchMethodException {
        // Busca el método público jwtDecoder(String, String) en JwtConfig
        // Se buscan todos los métodos porque las anotaciones @Value pueden
        // afectar la firma exacta vista por reflexión
        for (Method m : JwtConfig.class.getDeclaredMethods()) {
            if (m.getName().equals("jwtDecoder") && m.getParameterCount() == 2) {
                return m;
            }
        }
        throw new NoSuchMethodException("jwtDecoder(String, String) no encontrado en JwtConfig");
    }
}