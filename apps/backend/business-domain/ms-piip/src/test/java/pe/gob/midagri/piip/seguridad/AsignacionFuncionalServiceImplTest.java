package pe.gob.midagri.piip.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.documentos.dto.DocumentoInstitucionalAprobatorio;
import pe.gob.midagri.piip.documentos.service.DocumentoService;
import pe.gob.midagri.piip.seguridad.dto.*;
import pe.gob.midagri.piip.seguridad.entity.*;
import pe.gob.midagri.piip.seguridad.repository.*;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;
import pe.gob.midagri.piip.seguridad.service.impl.AsignacionFuncionalServiceImpl;

class AsignacionFuncionalServiceImplTest {
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class); private final RolRepository roles = mock(RolRepository.class);
    private final MatrizFuncionPerfilUnidadRepository matriz = mock(MatrizFuncionPerfilUnidadRepository.class);
    private final UsuarioRolUnidadRepository asignaciones = mock(UsuarioRolUnidadRepository.class);
    private final UsuarioRolUnidadEventoRepository eventos = mock(UsuarioRolUnidadEventoRepository.class);
    private final DocumentoService documentos = mock(DocumentoService.class); private final AutorizacionEfectivaService autorizacion = mock(AutorizacionEfectivaService.class);
    private final AuditService auditoria = mock(AuditService.class); private AsignacionFuncionalServiceImpl service;
    private final AssignmentAuthContext contexto = new AssignmentAuthContext("admin", 10L, 1L, "c-1");
    @BeforeEach void preparar() {
        service = new AsignacionFuncionalServiceImpl(usuarios, roles, matriz, asignaciones, eventos, documentos, autorizacion, auditoria);
        when(autorizacion.revalidarParaOperacionSensible("admin", 10L, "GlobalAdmin", 1L)).thenReturn(new AutorizacionEfectivaService.AsignacionEfectiva(10L, 2L, 3L, "GlobalAdmin", 1L));
        UsuarioEntity usuario = new UsuarioEntity(); usuario.setActivo("S"); when(usuarios.findById(20L)).thenReturn(Optional.of(usuario));
        RolEntity rol = new RolEntity(); rol.setId(5); rol.setNombre("GlobalAdmin"); when(roles.findById(5)).thenReturn(Optional.of(rol));
        MatrizFuncionPerfilUnidadEntity c = new MatrizFuncionPerfilUnidadEntity(); c.setId(30L); c.setRolId(5); c.setUnidadId(99L); when(matriz.findById(30L)).thenReturn(Optional.of(c)); when(matriz.findVigenteParaAsignacion(30L, 5, 99L, LocalDate.now())).thenReturn(Optional.of(c));
        when(documentos.validarDocumentoInstitucionalAprobatorio(40L)).thenReturn(new DocumentoInstitucionalAprobatorio(40L, 1L, true));
        when(asignaciones.existsGlobalAdminHistorico()).thenReturn(true);
        when(asignaciones.save(any())).thenAnswer(i -> { UsuarioRolUnidadEntity a = i.getArgument(0); if (a.getId() == null) a.setId(70L); a.setVersion(0L); return a; });
    }
    @Test void altaDerivaPerfilYUnidadSoloDeLaMatrizYAdita() {
        var d = service.crear(new AssignmentRequest(20L, 30L, LocalDate.now(), null, 40L), contexto);
        assertEquals("GlobalAdmin", d.perfil()); assertEquals(99L, d.unidadId()); verify(eventos).append(any()); verify(auditoria).registrarExito(any());
    }
    @Test void revocacionDelUltimoGlobalAdminSeBloqueaBajoBloqueo() {
        UsuarioRolUnidadEntity a = asignacionGlobal(); when(asignaciones.findByIdForUpdate(70L)).thenReturn(Optional.of(a)); when(asignaciones.findGlobalAdminsEfectivosForUpdate()).thenReturn(List.of(a));
        var error = assertThrows(ResponseStatusException.class, () -> service.revocar(70L, new RevocationRequest("Cese", 40L), contexto));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode()); assertEquals("LAST_GLOBAL_ADMIN", error.getReason()); verify(auditoria).registrarDenegacion(any());
    }
    @Test void cambioConEtagObsoletoSeRechazaAntesDePersistir() {
        UsuarioRolUnidadEntity a = asignacionGlobal(); when(asignaciones.findByIdForUpdate(70L)).thenReturn(Optional.of(a));
        var error = assertThrows(ResponseStatusException.class, () -> service.cambiar(70L, new AssignmentChangeRequest(LocalDate.now(), null), 8L, contexto));
        assertEquals(HttpStatus.PRECONDITION_FAILED, error.getStatusCode()); verify(asignaciones, never()).save(a);
    }
    private UsuarioRolUnidadEntity asignacionGlobal() { UsuarioRolUnidadEntity a = new UsuarioRolUnidadEntity(); a.setId(70L); a.setRolId(5); a.setUnidadId(99L); a.setDocumentoFormalId(40L); a.setFechaInicio(LocalDate.now().minusDays(1)); a.setVersion(1L); return a; }
}
