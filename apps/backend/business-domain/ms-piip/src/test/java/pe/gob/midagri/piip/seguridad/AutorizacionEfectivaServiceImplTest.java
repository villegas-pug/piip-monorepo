package pe.gob.midagri.piip.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.midagri.piip.seguridad.entity.RolEntity;
import pe.gob.midagri.piip.seguridad.entity.UsuarioRolUnidadEntity;
import pe.gob.midagri.piip.seguridad.repository.RolRepository;
import pe.gob.midagri.piip.seguridad.repository.UsuarioRolUnidadRepository;
import pe.gob.midagri.piip.seguridad.service.impl.AutorizacionEfectivaServiceImpl;

class AutorizacionEfectivaServiceImplTest {
    private final RolRepository roles = mock(RolRepository.class);
    private final UsuarioRolUnidadRepository asignaciones = mock(UsuarioRolUnidadRepository.class);
    private final AutorizacionEfectivaServiceImpl service = new AutorizacionEfectivaServiceImpl(
            roles,
            mock(pe.gob.midagri.piip.seguridad.repository.UsuarioRepository.class),
             asignaciones,
             mock(pe.gob.midagri.piip.seguridad.repository.MatrizFuncionPerfilUnidadRepository.class),
             mock(pe.gob.midagri.piip.seguridad.repository.MatrizFuncionRepository.class),
             mock(pe.gob.midagri.piip.seguridad.repository.UnidadEjecutoraRepository.class),
             mock(pe.gob.midagri.piip.seguridad.repository.SuplenciaFuncionalRepository.class));

    @Test
    void autorizaSoloElSubAsignacionPerfilYUnidadExactosVigentes() {
        RolEntity rol = new RolEntity(); rol.setId(9); rol.setNombre("Responsable");
        UsuarioRolUnidadEntity asignacion = new UsuarioRolUnidadEntity();
        asignacion.setId(4L); asignacion.setUsuarioId(12L); asignacion.setCombinacionMatrizId(15L); asignacion.setUnidadId(8L);
        when(roles.findByNombre("Responsable")).thenReturn(Optional.of(rol));
        when(asignaciones.findAsignacionEfectivaForUpdate(4L, "sub-valido", 9, 8L)).thenReturn(Optional.of(asignacion));

        var effective = service.revalidarParaOperacionSensible("sub-valido", 4L, "Responsable", 8L);

        assertEquals(8L, effective.unidadId());
        verify(asignaciones).findAsignacionEfectivaForUpdate(4L, "sub-valido", 9, 8L);
    }

    @Test
    void deniegaSubDistintoUnidadNoExactaAsignacionVencidaRevocadaOSuplida() {
        RolEntity rol = new RolEntity(); rol.setId(9); rol.setNombre("Responsable");
        when(roles.findByNombre("Responsable")).thenReturn(Optional.of(rol));
        when(asignaciones.findAsignacionEfectivaForUpdate(anyLong(), anyString(), anyInt(), anyLong())).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.revalidarParaOperacionSensible("sub-revocado", 4L, "Responsable", 99L));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        assertEquals("ASSIGNMENT_SCOPE_DENIED", error.getReason());
    }
}
