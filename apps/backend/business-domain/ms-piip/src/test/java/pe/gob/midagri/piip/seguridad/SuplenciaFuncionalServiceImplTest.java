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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.documentos.dto.DocumentoInstitucionalAprobatorio;
import pe.gob.midagri.piip.documentos.service.DocumentoService;
import pe.gob.midagri.piip.seguridad.dto.*;
import pe.gob.midagri.piip.seguridad.entity.*;
import pe.gob.midagri.piip.seguridad.repository.*;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;
import pe.gob.midagri.piip.seguridad.service.impl.SuplenciaFuncionalServiceImpl;

class SuplenciaFuncionalServiceImplTest {
    private final UsuarioRolUnidadRepository asignaciones = mock(UsuarioRolUnidadRepository.class);
    private final SuplenciaFuncionalRepository suplencias = mock(SuplenciaFuncionalRepository.class);
    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final RolRepository roles = mock(RolRepository.class);
    private final DocumentoService documentos = mock(DocumentoService.class);
    private final AutorizacionEfectivaService autorizacion = mock(AutorizacionEfectivaService.class);
    private final UsuarioRolUnidadEventoRepository eventos = mock(UsuarioRolUnidadEventoRepository.class);
    private final AuditService auditoria = mock(AuditService.class);
    private SuplenciaFuncionalServiceImpl service;
    private final AssignmentAuthContext contexto = new AssignmentAuthContext("admin", 10L, 1L, "c-1");

    @BeforeEach void preparar() {
        service = new SuplenciaFuncionalServiceImpl(asignaciones, suplencias, usuarios, roles, documentos,
                autorizacion, eventos, auditoria);
        RolEntity rol = new RolEntity(); rol.setId(5); rol.setNombre("Responsable");
        when(roles.findById(5)).thenReturn(Optional.of(rol));
        when(autorizacion.revalidarParaOperacionSensible("admin", 10L, "GlobalAdmin", 1L))
                .thenReturn(new AutorizacionEfectivaService.AsignacionEfectiva(10L, 2L, 3L, "GlobalAdmin", 1L));
        UsuarioEntity usuario = new UsuarioEntity(); usuario.setActivo("S");
        when(usuarios.findById(20L)).thenReturn(Optional.of(usuario));
        when(documentos.validarDocumentoInstitucionalAprobatorio(40L))
                .thenReturn(new DocumentoInstitucionalAprobatorio(40L, 1L, true));
        when(asignaciones.save(any())).thenAnswer(invocation -> {
            UsuarioRolUnidadEntity asignacion = invocation.getArgument(0);
            if (asignacion.getId() == null) asignacion.setId(71L);
            return asignacion;
        });
        when(suplencias.save(any())).thenAnswer(invocation -> {
            SuplenciaFuncionalEntity suplencia = invocation.getArgument(0);
            if (suplencia.getId() == null) suplencia.setId(81L);
            return suplencia;
        });
    }

    @Test void creaAsignacionTemporalSinSolapeYAdita() {
        UsuarioRolUnidadEntity titular = titular();
        when(asignaciones.findByIdForUpdate(70L)).thenReturn(Optional.of(titular));
        when(suplencias.findSolapadasForUpdate(70L, LocalDate.now(), LocalDate.now().plusDays(2)))
                .thenReturn(List.of());

        SubstitutionDetail detalle = service.crear(70L,
                new SubstitutionRequest(20L, LocalDate.now(), LocalDate.now().plusDays(2), 40L), contexto);

        assertEquals(81L, detalle.id());
        assertEquals(71L, detalle.asignacionSuplenteId());
        verify(eventos, times(2)).append(any());
        verify(auditoria).registrarExito(any());
    }

    @Test void rechazaSolapeBajoBloqueoYAditaLaDenegacion() {
        when(asignaciones.findByIdForUpdate(70L)).thenReturn(Optional.of(titular()));
        when(suplencias.findSolapadasForUpdate(70L, LocalDate.now(), LocalDate.now().plusDays(2)))
                .thenReturn(List.of(new SuplenciaFuncionalEntity()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.crear(70L,
                new SubstitutionRequest(20L, LocalDate.now(), LocalDate.now().plusDays(2), 40L), contexto));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertEquals("SUBSTITUTION_OVERLAP", error.getReason());
        verify(auditoria).registrarDenegacion(any());
    }

    @Test void soloLaMismaAutoridadPuedeTerminarAnticipadamente() {
        SuplenciaFuncionalEntity suplencia = new SuplenciaFuncionalEntity();
        suplencia.setId(81L); suplencia.setAsignacionTitularId(70L); suplencia.setAsignacionSuplenteId(71L);
        suplencia.setAutoridadId(99L); suplencia.setInicio(LocalDate.now()); suplencia.setFin(LocalDate.now().plusDays(2));
        when(suplencias.findByIdForUpdate(81L)).thenReturn(Optional.of(suplencia));
        when(asignaciones.findByIdForUpdate(70L)).thenReturn(Optional.of(titular()));
        when(asignaciones.findByIdForUpdate(71L)).thenReturn(Optional.of(titular()));

        ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service.terminarAnticipadamente(
                81L, new EarlyTerminationRequest("Cese", null), contexto));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEquals("SUBSTITUTION_TERMINATION_DENIED", error.getReason());
        verify(auditoria).registrarDenegacion(any());
    }

    private UsuarioRolUnidadEntity titular() {
        UsuarioRolUnidadEntity titular = new UsuarioRolUnidadEntity();
        titular.setId(70L); titular.setRolId(5); titular.setUnidadId(99L); titular.setCombinacionMatrizId(30L);
        titular.setActivo("S"); titular.setFechaInicio(LocalDate.now().minusDays(1));
        return titular;
    }
}
