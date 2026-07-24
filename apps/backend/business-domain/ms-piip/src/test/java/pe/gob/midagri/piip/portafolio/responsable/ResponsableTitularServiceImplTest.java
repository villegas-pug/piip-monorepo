package pe.gob.midagri.piip.portafolio.responsable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.ResponsibleReplacementRequest;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TitularidadResponsableEntity;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.repository.TitularidadResponsableRepository;
import pe.gob.midagri.piip.portafolio.service.impl.ResponsableTitularServiceImpl;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

@ExtendWith(MockitoExtension.class)
@DisplayName("US6 - Sustitución de Responsable titular")
class ResponsableTitularServiceImplTest {

    @Mock private RegistroPortafolioRepository registros;
    @Mock private TitularidadResponsableRepository titularidades;
    @Mock private AutorizacionEfectivaService autorizacion;
    @Mock private AuditService auditoria;
    private ResponsableTitularServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ResponsableTitularServiceImpl(registros, titularidades, autorizacion, auditoria);
    }

    @Test
    @DisplayName("bloquea el agregado, revalida UnidadAdmin y conserva exactamente un titular")
    void sustituyeBajoBloqueoYAutoridadEfectiva() {
        RegistroPortafolioEntity registro = registro(10L, 50L);
        TitularidadResponsableEntity anterior = titularidad(7L, 101L);
        when(registros.findByIdForUpdate(10L)).thenReturn(Optional.of(registro));
        when(autorizacion.revalidarParaOperacionSensible("sub-actor", 9L, "UnidadAdmin", 50L))
                .thenReturn(new AutorizacionEfectivaService.AsignacionEfectiva(9L, 201L, 1L, "UnidadAdmin", 50L));
        when(titularidades.findByRegistroPortafolioIdAndFinIsNull(10L)).thenReturn(Optional.of(anterior));
        when(titularidades.saveAndFlush(any())).thenAnswer(invocation -> {
            TitularidadResponsableEntity value = invocation.getArgument(0);
            if (value.getId() == null) value.setId(8L);
            return value;
        });

        var resultado = service.sustituir(10L, new ResponsibleReplacementRequest(102L, "Cambio formal"),
                new PortafolioAuthContext("sub-actor", null, 9L, null, null, null, "corr-1"));

        assertEquals(101L, resultado.titularAnteriorId());
        assertEquals(102L, resultado.nuevoResponsableId());
        assertEquals(8L, resultado.titularidadNuevaId());
        assertEquals(102L, anteriorCaptor().getValue().getUsuarioId());
        verify(registros).findByIdForUpdate(10L);
        verify(auditoria).registrarExito(any());
    }

    @Test
    @DisplayName("deniega y audita cuando seguridad no confirma UnidadAdmin en el ámbito del registro")
    void deniegaSiSeguridadNoAutorizaAmbito() {
        when(registros.findByIdForUpdate(10L)).thenReturn(Optional.of(registro(10L, 50L)));
        when(autorizacion.revalidarParaOperacionSensible("sub-actor", 9L, "UnidadAdmin", 50L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED"));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.sustituir(10L, new ResponsibleReplacementRequest(102L, "Cambio formal"),
                        new PortafolioAuthContext("sub-actor", null, 9L, null, null, null, "corr-1")));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verify(auditoria).registrarDenegacion(any());
        verify(titularidades, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("rechaza sustituir por el mismo titular sin alterar el historial")
    void rechazaMismoTitular() {
        when(registros.findByIdForUpdate(10L)).thenReturn(Optional.of(registro(10L, 50L)));
        when(autorizacion.revalidarParaOperacionSensible(eq("sub-actor"), eq(9L), eq("UnidadAdmin"), eq(50L)))
                .thenReturn(new AutorizacionEfectivaService.AsignacionEfectiva(9L, 201L, 1L, "UnidadAdmin", 50L));
        when(titularidades.findByRegistroPortafolioIdAndFinIsNull(10L)).thenReturn(Optional.of(titularidad(7L, 101L)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.sustituir(10L, new ResponsibleReplacementRequest(101L, "Cambio formal"),
                        new PortafolioAuthContext("sub-actor", null, 9L, null, null, null, "corr-1")));

        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(titularidades, never()).saveAndFlush(any());
        verify(auditoria).registrarDenegacion(any());
    }

    private ArgumentCaptor<TitularidadResponsableEntity> anteriorCaptor() {
        ArgumentCaptor<TitularidadResponsableEntity> captor = ArgumentCaptor.forClass(TitularidadResponsableEntity.class);
        verify(titularidades, org.mockito.Mockito.times(2)).saveAndFlush(captor.capture());
        return captor;
    }

    private static RegistroPortafolioEntity registro(Long id, Long unidadId) {
        RegistroPortafolioEntity value = new RegistroPortafolioEntity();
        value.setId(id); value.setUnidadEjecutoraId(unidadId);
        return value;
    }

    private static TitularidadResponsableEntity titularidad(Long id, Long usuarioId) {
        TitularidadResponsableEntity value = new TitularidadResponsableEntity();
        value.setId(id); value.setUsuarioId(usuarioId);
        return value;
    }
}
