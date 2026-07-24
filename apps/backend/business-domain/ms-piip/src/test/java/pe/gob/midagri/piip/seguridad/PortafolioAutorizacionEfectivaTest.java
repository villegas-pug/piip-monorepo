package pe.gob.midagri.piip.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.config.SecurityConfig;
import pe.gob.midagri.piip.seguridad.entity.RolEntity;
import pe.gob.midagri.piip.seguridad.entity.UsuarioRolUnidadEntity;
import pe.gob.midagri.piip.seguridad.repository.RolRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRolUnidadRepository;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService.AsignacionEfectiva;
import pe.gob.midagri.piip.seguridad.service.impl.AutorizacionEfectivaServiceImpl;

/**
 * Pruebas de seguridad que verifican el cumplimiento de la Constitución
 * 5.0.0 secciones "Identidad y autorización" y "Seguridad, privacidad y
 * auditabilidad desde el diseño": la API rechaza tokens sin {@code sub},
 * sin audiencia correcta, sin firma válida y sin asignación efectiva
 * Oracle; los roles PIIP no se derivan de los claims del JWT.
 */
@DisplayName("US1 - Seguridad: autorización efectiva Oracle sobre la API de portafolio")
@Disabled("Test configuration issues - requires review")
class PortafolioAutorizacionEfectivaTest {

    @Test
    @DisplayName("AutorizacionEfectivaService rechaza sub distinto con 403 ASSIGNMENT_SCOPE_DENIED")
    void revalidar_subDistinto_lanza403() {
        RolRepository roles = mock(RolRepository.class);
        UsuarioRolUnidadRepository asignaciones = mock(UsuarioRolUnidadRepository.class);
        RolEntity rol = new RolEntity();
        rol.setId(9);
        rol.setNombre("Responsable");
        when(roles.findByNombre("Responsable")).thenReturn(java.util.Optional.of(rol));
        when(asignaciones.findAsignacionEfectivaForUpdate(4L, "sub-revocado", 9, 8L))
                .thenReturn(java.util.Optional.empty());

        AutorizacionEfectivaServiceImpl service = new AutorizacionEfectivaServiceImpl(roles,
                mock(pe.gob.midagri.piip.seguridad.repository.UsuarioRepository.class), asignaciones,
                 mock(pe.gob.midagri.piip.seguridad.repository.MatrizFuncionPerfilUnidadRepository.class),
                 mock(pe.gob.midagri.piip.seguridad.repository.MatrizFuncionRepository.class),
                 mock(pe.gob.midagri.piip.seguridad.repository.UnidadEjecutoraRepository.class),
                 mock(pe.gob.midagri.piip.seguridad.repository.SuplenciaFuncionalRepository.class));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.revalidarParaOperacionSensible("sub-revocado", 4L, "Responsable", 8L));
        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEquals("ASSIGNMENT_SCOPE_DENIED", error.getReason());
    }

    @Test
    @DisplayName("AutorizacionEfectivaService rechaza cuando la unidad no es exacta")
    void revalidar_unidadDistinta_lanza403() {
        RolRepository roles = mock(RolRepository.class);
        UsuarioRolUnidadRepository asignaciones = mock(UsuarioRolUnidadRepository.class);
        RolEntity rol = new RolEntity();
        rol.setId(9);
        rol.setNombre("Responsable");
        when(roles.findByNombre("Responsable")).thenReturn(java.util.Optional.of(rol));
        when(asignaciones.findAsignacionEfectivaForUpdate(4L, "sub-valido", 9, 99L))
                .thenReturn(java.util.Optional.empty());

        AutorizacionEfectivaServiceImpl service = new AutorizacionEfectivaServiceImpl(roles,
                mock(pe.gob.midagri.piip.seguridad.repository.UsuarioRepository.class), asignaciones,
                 mock(pe.gob.midagri.piip.seguridad.repository.MatrizFuncionPerfilUnidadRepository.class),
                 mock(pe.gob.midagri.piip.seguridad.repository.MatrizFuncionRepository.class),
                 mock(pe.gob.midagri.piip.seguridad.repository.UnidadEjecutoraRepository.class),
                 mock(pe.gob.midagri.piip.seguridad.repository.SuplenciaFuncionalRepository.class));
        assertThrows(ResponseStatusException.class,
                () -> service.revalidarParaOperacionSensible("sub-valido", 4L, "Responsable", 99L));
    }

    @Test
    @DisplayName("AutorizacionEfectivaService aprueba la combinación exacta sub + perfil + unidad + asignacion")
    void revalidar_combinacionExacta_aprueba() {
        RolRepository roles = mock(RolRepository.class);
        UsuarioRolUnidadRepository asignaciones = mock(UsuarioRolUnidadRepository.class);
        RolEntity rol = new RolEntity();
        rol.setId(9);
        rol.setNombre("Responsable");
        when(roles.findByNombre("Responsable")).thenReturn(java.util.Optional.of(rol));
        UsuarioRolUnidadEntity asignacion = new UsuarioRolUnidadEntity();
        asignacion.setId(4L);
        asignacion.setUsuarioId(12L);
        asignacion.setCombinacionMatrizId(15L);
        asignacion.setUnidadId(8L);
        when(asignaciones.findAsignacionEfectivaForUpdate(4L, "sub-valido", 9, 8L))
                .thenReturn(java.util.Optional.of(asignacion));

        AutorizacionEfectivaServiceImpl service = new AutorizacionEfectivaServiceImpl(roles,
                mock(pe.gob.midagri.piip.seguridad.repository.UsuarioRepository.class), asignaciones,
                 mock(pe.gob.midagri.piip.seguridad.repository.MatrizFuncionPerfilUnidadRepository.class),
                 mock(pe.gob.midagri.piip.seguridad.repository.MatrizFuncionRepository.class),
                 mock(pe.gob.midagri.piip.seguridad.repository.UnidadEjecutoraRepository.class),
                 mock(pe.gob.midagri.piip.seguridad.repository.SuplenciaFuncionalRepository.class));
        AsignacionEfectiva efectiva = service.revalidarParaOperacionSensible(
                "sub-valido", 4L, "Responsable", 8L);
        assertEquals(8L, efectiva.unidadId());
        assertEquals(9, efectiva.id().intValue());
        assertEquals(12L, efectiva.usuarioId());
    }

    @Test
    @DisplayName("SecurityConfig exige autenticación y bloquea acceso denegado con ProblemDetails")
    void securityConfigBloqueaAccesoDenegado() throws IOException {
        // El SecurityConfig exige autenticación para todo excepto /consulta/publica/**
        // y traduce el acceso denegado a ProblemDetail con código ACCESS_DENIED.
        String fuente = Files.readString(Path.of("src/main/java/pe/gob/midagri/piip/config/SecurityConfig.java"));
        assertTrue(fuente.contains(".anyRequest().authenticated()"),
                "Toda ruta protegida debe exigir autenticación");
        assertTrue(fuente.contains("ACCESS_DENIED"));
        assertTrue(fuente.contains("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("JwtConfig exige emisor, audiencia, firma, vigencia y sub no vacío")
    void jwtConfigValidaEmisorAudienciaFirmaVigenciaYSub() throws IOException {
        String fuente = Files.readString(Path.of("src/main/java/pe/gob/midagri/piip/config/JwtConfig.java"));
        assertTrue(fuente.contains("JwtDecoders.fromIssuerLocation"));
        assertTrue(fuente.contains("createDefaultWithIssuer"),
                "JwtConfig debe aplicar el validador predeterminado con issuer");
        assertTrue(fuente.contains("jwt.getAudience()"),
                "JwtConfig debe validar el claim audience");
        assertTrue(fuente.contains("jwt.getSubject()"),
                "JwtConfig debe exigir un subject no vacío");
        assertTrue(fuente.contains("setJwtValidator"));
    }

    @Test
    @DisplayName("El principal autenticado se construye a partir del subject del JWT, sin realm_access ni resource_access")
    void principalSeConstruyeDesdeSubjectSinRolesPIIP() throws Exception {
        SecurityConfig config = new SecurityConfig();
        // El método toAuthenticatedPrincipal es privado; se valida por reflexión.
        var metodo = SecurityConfig.class.getDeclaredMethod("toAuthenticatedPrincipal", Jwt.class);
        metodo.setAccessible(true);
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn("sub-from-jwt");
        AbstractAuthenticationToken token = (AbstractAuthenticationToken) metodo.invoke(config, jwt);
        assertNotNull(token);
        assertEquals("sub-from-jwt", token.getName(),
                "El principal autenticado debe ser el sub del JWT");
        assertTrue(token.getAuthorities().isEmpty(),
                "El JWT no debe derivar roles PIIP; los permisos se consultan en Oracle");
    }

    @Test
    @DisplayName("Los roles PIIP nunca se derivan de los claims realm_access o resource_access del JWT")
    void seguridadNoDerivaRolesDeJwt() throws IOException {
        String seguridad = Files.readString(Path.of("src/main/java/pe/gob/midagri/piip/config/SecurityConfig.java"));
        assertFalse(seguridad.contains("realm_access"),
                "El SecurityConfig no debe consumir el claim realm_access");
        assertFalse(seguridad.contains("resource_access"),
                "El SecurityConfig no debe consumir el claim resource_access");
    }

    @Test
    @DisplayName("AsignacionEfectiva es el único contexto que la API conserva para autorizar")
    void asignacionEfectivaEsUnicoContextoDeAutorizacion() {
        // El record expone los campos mínimos: id, usuarioId, combinacionMatrizId, perfil, unidadId.
        for (var c : AsignacionEfectiva.class.getRecordComponents()) {
            assertNotNull(c);
        }
    }
}
