package pe.gob.midagri.piip.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionEntity;
import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionPerfilUnidadEntity;
import pe.gob.midagri.piip.seguridad.entity.RolEntity;
import pe.gob.midagri.piip.seguridad.entity.UnidadEjecutoraEntity;
import pe.gob.midagri.piip.seguridad.entity.UsuarioEntity;
import pe.gob.midagri.piip.seguridad.entity.UsuarioRolUnidadEntity;
import pe.gob.midagri.piip.seguridad.repository.MatrizFuncionPerfilUnidadRepository;
import pe.gob.midagri.piip.seguridad.repository.MatrizFuncionRepository;
import pe.gob.midagri.piip.seguridad.repository.RolRepository;
import pe.gob.midagri.piip.seguridad.repository.UnidadEjecutoraRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRolUnidadRepository;
import pe.gob.midagri.piip.seguridad.service.impl.AutorizacionEfectivaServiceImpl;

/** Pruebas de la fuente Oracle de las opciones de contexto efectivo. */
class ContextoEfectivoServiceTest {

    private final RolRepository roles = org.mockito.Mockito.mock(RolRepository.class);
    private final UsuarioRepository usuarios = org.mockito.Mockito.mock(UsuarioRepository.class);
    private final UsuarioRolUnidadRepository asignaciones = org.mockito.Mockito.mock(UsuarioRolUnidadRepository.class);
    private final MatrizFuncionPerfilUnidadRepository combinaciones = org.mockito.Mockito.mock(MatrizFuncionPerfilUnidadRepository.class);
    private final MatrizFuncionRepository funciones = org.mockito.Mockito.mock(MatrizFuncionRepository.class);
    private final UnidadEjecutoraRepository unidades = org.mockito.Mockito.mock(UnidadEjecutoraRepository.class);
    private final pe.gob.midagri.piip.seguridad.repository.SuplenciaFuncionalRepository suplencias =
            org.mockito.Mockito.mock(pe.gob.midagri.piip.seguridad.repository.SuplenciaFuncionalRepository.class);
    private final AutorizacionEfectivaServiceImpl service = new AutorizacionEfectivaServiceImpl(
            roles, usuarios, asignaciones, combinaciones, funciones, unidades, suplencias);

    @Test
    void devuelveOpcionVigenteDesdeLasAsignacionesOracleDelSub() {
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setId(1L);
        usuario.setActivo("S");
        UsuarioRolUnidadEntity asignacion = new UsuarioRolUnidadEntity();
        asignacion.setId(2L);
        asignacion.setRolId(3);
        asignacion.setUnidadId(4L);
        asignacion.setCombinacionMatrizId(5L);
        asignacion.setActivo("S");
        asignacion.setInactivaTemporalmente("N");
        asignacion.setFechaInicio(LocalDate.of(2020, 1, 1));
        MatrizFuncionPerfilUnidadEntity combinacion = new MatrizFuncionPerfilUnidadEntity();
        combinacion.setFuncionId(6L);
        combinacion.setActiva("S");
        combinacion.setVigenteDesde(LocalDate.of(2020, 1, 1));
        MatrizFuncionEntity funcion = new MatrizFuncionEntity();
        funcion.setDescripcion("Gestionar portafolio");
        RolEntity rol = new RolEntity();
        rol.setNombre("Responsable");
        UnidadEjecutoraEntity unidad = new UnidadEjecutoraEntity();
        unidad.setNombre("Oficina de Modernización");

        when(usuarios.findByKeycloakId("sub-oracle")).thenReturn(Optional.of(usuario));
        when(asignaciones.findByUsuarioIdOrderByFechaInicioDesc(1L)).thenReturn(List.of(asignacion));
        when(combinaciones.findById(5L)).thenReturn(Optional.of(combinacion));
        when(funciones.findById(6L)).thenReturn(Optional.of(funcion));
        when(roles.findById(3)).thenReturn(Optional.of(rol));
        when(unidades.findById(4L)).thenReturn(Optional.of(unidad));

        var opciones = service.listarAsignacionesPropias("sub-oracle");

        assertEquals(1, opciones.size());
        assertEquals("VIGENTE", opciones.getFirst().estadoEfectivo());
        assertEquals("Gestionar portafolio", opciones.getFirst().funcion());
        assertEquals("Responsable", opciones.getFirst().perfil());
        assertEquals("Oficina de Modernización", opciones.getFirst().unidad());
    }
}
