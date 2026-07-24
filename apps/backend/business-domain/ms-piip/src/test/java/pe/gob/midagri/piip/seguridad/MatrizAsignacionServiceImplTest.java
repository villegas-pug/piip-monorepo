package pe.gob.midagri.piip.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.documentos.dto.DocumentoInstitucionalAprobatorio;
import pe.gob.midagri.piip.documentos.service.DocumentoService;
import pe.gob.midagri.piip.seguridad.dto.MatrixAuthContext;
import pe.gob.midagri.piip.seguridad.dto.MatrixCombinationRequest;
import pe.gob.midagri.piip.seguridad.dto.MatrixDeactivationRequest;
import pe.gob.midagri.piip.seguridad.dto.MatrixFunctionRequest;
import pe.gob.midagri.piip.seguridad.dto.MatrixVersionRequest;
import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionEntity;
import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionPerfilUnidadEntity;
import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionalVersionEntity;
import pe.gob.midagri.piip.seguridad.entity.RolEntity;
import pe.gob.midagri.piip.seguridad.entity.UsuarioEntity;
import pe.gob.midagri.piip.seguridad.repository.MatrizFuncionPerfilUnidadRepository;
import pe.gob.midagri.piip.seguridad.repository.MatrizFuncionRepository;
import pe.gob.midagri.piip.seguridad.repository.MatrizFuncionalVersionRepository;
import pe.gob.midagri.piip.seguridad.repository.RolRepository;
import pe.gob.midagri.piip.seguridad.repository.UnidadEjecutoraRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRolUnidadRepository;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;
import pe.gob.midagri.piip.seguridad.service.impl.MatrizAsignacionServiceImpl;

class MatrizAsignacionServiceImplTest {
    private final MatrizFuncionalVersionRepository versiones = Mockito.mock(MatrizFuncionalVersionRepository.class);
    private final MatrizFuncionRepository funciones = Mockito.mock(MatrizFuncionRepository.class);
    private final MatrizFuncionPerfilUnidadRepository combinaciones = Mockito.mock(MatrizFuncionPerfilUnidadRepository.class);
    private final RolRepository roles = Mockito.mock(RolRepository.class);
    private final UnidadEjecutoraRepository unidades = Mockito.mock(UnidadEjecutoraRepository.class);
    private final UsuarioRepository usuarios = Mockito.mock(UsuarioRepository.class);
    private final UsuarioRolUnidadRepository asignaciones = Mockito.mock(UsuarioRolUnidadRepository.class);
    private final DocumentoService documentos = Mockito.mock(DocumentoService.class);
    private final AutorizacionEfectivaService autorizacion = Mockito.mock(AutorizacionEfectivaService.class);
    private final AuditService auditoria = Mockito.mock(AuditService.class);
    private MatrizAsignacionServiceImpl service;

    @BeforeEach
    void preparar() {
        service = new MatrizAsignacionServiceImpl(versiones, funciones, combinaciones, roles, unidades, usuarios,
                asignaciones, documentos, autorizacion, auditoria);
        when(autorizacion.revalidarParaOperacionSensible("registrador", 10L, "GlobalAdmin", 1L))
                .thenReturn(new AutorizacionEfectivaService.AsignacionEfectiva(10L, 100L, 20L, "GlobalAdmin", 1L));
        when(versiones.findAllByOrderByIdDesc(any())).thenReturn(Page.empty());
        when(documentos.validarDocumentoInstitucionalAprobatorio(900L))
                .thenReturn(new DocumentoInstitucionalAprobatorio(900L, 70L, true));
        RolEntity responsable = new RolEntity(); responsable.setId(4); responsable.setNombre("Responsable");
        RolEntity autoridad = new RolEntity(); autoridad.setId(5); autoridad.setNombre("Autoridad");
        when(roles.findByNombre("Responsable")).thenReturn(Optional.of(responsable));
        when(roles.findByNombre("Autoridad")).thenReturn(Optional.of(autoridad));
        when(unidades.existsById(55L)).thenReturn(true);
        UsuarioEntity aprobador = new UsuarioEntity(); aprobador.setId(200L); aprobador.setActivo("S");
        when(usuarios.findById(200L)).thenReturn(Optional.of(aprobador));
        when(asignaciones.existsAsignacionVigente(200L, 5, 55L)).thenReturn(true);
        when(versiones.save(any())).thenAnswer(inv -> { var v = inv.getArgument(0, MatrizFuncionalVersionEntity.class); v.setId(300L); return v; });
        when(funciones.save(any())).thenAnswer(inv -> { var f = inv.getArgument(0, MatrizFuncionEntity.class); f.setId(400L); return f; });
        when(combinaciones.save(any())).thenAnswer(inv -> { var c = inv.getArgument(0, MatrizFuncionPerfilUnidadEntity.class); c.setId(500L); return c; });
    }

    @Test
    void registraMatrizSoloConAutoridadActivaDeLaUnidadConcreta() {
        var detalle = service.crearVersion(solicitud(), contexto());

        assertEquals(300L, detalle.id());
        assertEquals(1, detalle.combinaciones().size());
        assertEquals(200L, detalle.combinaciones().getFirst().aprobadorUsuarioId());
        verify(auditoria).registrarExito(any());
    }

    @Test
    void rechazaAprobadorSinAsignacionAutoridadVigenteEnLaUnidad() {
        when(asignaciones.existsAsignacionVigente(200L, 5, 55L)).thenReturn(false);

        var error = assertThrows(ResponseStatusException.class, () -> service.crearVersion(solicitud(), contexto()));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertEquals("MATRIX_APPROVER_INVALID", error.getReason());
        verify(auditoria).registrarDenegacion(any());
    }

    @Test
    void inactivacionCopiaLaMatrizYConservaLaAnteriorSinSobrescribirla() {
        MatrizFuncionalVersionEntity fuente = new MatrizFuncionalVersionEntity(); fuente.setId(1L);
        MatrizFuncionEntity funcion = new MatrizFuncionEntity(); funcion.setId(2L); funcion.setCodigo("GESTIONAR");
        funcion.setDescripcion("Gestionar matriz"); funcion.setActiva("S");
        MatrizFuncionPerfilUnidadEntity original = new MatrizFuncionPerfilUnidadEntity();
        original.setId(3L); original.setVersionId(1L); original.setFuncionId(2L); original.setRolId(4);
        original.setUnidadId(55L); original.setAprobadorId(200L); original.setRegistradorId(100L);
        original.setDocumentoAprobacionId(900L); original.setVigenteDesde(LocalDate.of(2026, 1, 1));
        original.setActiva("S");
        when(combinaciones.findById(3L)).thenReturn(Optional.of(original));
        when(versiones.findById(1L)).thenReturn(Optional.of(fuente));
        when(funciones.findByVersionIdOrderByCodigoAsc(1L)).thenReturn(List.of(funcion));
        when(combinaciones.findByVersionIdOrderByIdAsc(1L)).thenReturn(List.of(original));

        var detalle = service.inactivarCombinacion(3L,
                new MatrixDeactivationRequest("MFV-003", 900L, 200L, "Cambio formal"), contexto());

        assertEquals("MFV-003", detalle.codigoVersion());
        assertEquals(false, detalle.combinaciones().getFirst().activa());
        verify(auditoria).registrarExito(any());
    }

    private MatrixVersionRequest solicitud() {
        return new MatrixVersionRequest("MFV-002", null, LocalDate.of(2026, 7, 23), null, 900L,
                List.of(new MatrixFunctionRequest("GESTIONAR", "Gestionar matriz")),
                List.of(new MatrixCombinationRequest("GESTIONAR", "Responsable", 55L,
                        LocalDate.of(2026, 7, 23), null, 900L, 200L)));
    }

    private MatrixAuthContext contexto() { return new MatrixAuthContext("registrador", 10L, 1L, "corr-087"); }
}
