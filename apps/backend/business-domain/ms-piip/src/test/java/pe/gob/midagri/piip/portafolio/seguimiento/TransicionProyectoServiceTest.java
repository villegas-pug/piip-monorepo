package pe.gob.midagri.piip.portafolio.seguimiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CancelacionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.SuspensionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.service.AptitudDocumentalService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.TransicionProyectoService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.impl.TransicionProyectoServiceImpl;
import pe.gob.midagri.piip.portafolio.transicion.TransicionCommand;
import pe.gob.midagri.piip.portafolio.transicion.TransicionDetail;
import pe.gob.midagri.piip.portafolio.transicion.TransicionEstadoService;

@ExtendWith(MockitoExtension.class)
class TransicionProyectoServiceTest {

    @Mock private TransicionEstadoService transicionEstadoService;
    @Mock private AptitudDocumentalService aptitudDocumentalService;

    private TransicionProyectoService service;
    private final PortafolioAuthContext contexto = new PortafolioAuthContext(
            "sub-1", 10L, 20L, "UnidadAdmin", 30L, 30L, "corr-1");

    @BeforeEach
    void setUp() {
        service = new TransicionProyectoServiceImpl(transicionEstadoService,
                aptitudDocumentalService);
    }

    @Test
    void suspenderValidaEvidenciaTipadaYDelegaEnLaMaquinaCanonica() {
        when(aptitudDocumentalService.esAptoParaTransicion(40L,
                AptitudDocumentalService.TipoEvidenciaTransicion.SUSPENSION)).thenReturn(true);
        when(transicionEstadoService.transicionar(any(), any(), any(), any(), any()))
                .thenReturn(detalle(EstadoIniciativa.SUSPENDIDO));

        var respuesta = service.suspender(50L,
                new SuspensionRequest(40L, "Riesgo operativo", "\"50-0\""),
                contexto, "idem-1", "{}");

        ArgumentCaptor<TransicionCommand> comando = ArgumentCaptor.forClass(TransicionCommand.class);
        verify(transicionEstadoService).transicionar(
                org.mockito.ArgumentMatchers.eq(50L), comando.capture(),
                org.mockito.ArgumentMatchers.eq(contexto), org.mockito.ArgumentMatchers.eq("idem-1"), any());
        assertEquals(EstadoIniciativa.SUSPENDIDO, comando.getValue().destino());
        assertEquals(EstadoIniciativa.SUSPENDIDO, respuesta.estadoNuevo());
    }

    @Test
    void cancelarExigeInformeFormalApto() {
        when(aptitudDocumentalService.esAptoParaTransicion(41L,
                AptitudDocumentalService.TipoEvidenciaTransicion.CANCELACION)).thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.cancelar(50L,
                        new CancelacionRequest(41L, "Decisión formal", "\"50-0\""),
                        contexto, "idem-2", "{}"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        verify(transicionEstadoService, never()).transicionar(any(), any(), any(), any(), any());
    }

    @Test
    void transicionExigeIfMatchAntesDeEvaluarElDocumento() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.suspender(50L,
                        new SuspensionRequest(40L, "Riesgo operativo", null),
                        contexto, "idem-3", "{}"));

        assertEquals(HttpStatus.PRECONDITION_REQUIRED, error.getStatusCode());
        verify(aptitudDocumentalService, never()).esAptoParaTransicion(any(Long.class), any());
    }

    private TransicionDetail detalle(EstadoIniciativa destino) {
        return new TransicionDetail(50L, EstadoIniciativa.PROYECTO_EJECUCION,
                destino, 60L, LocalDateTime.now(), "sub-1", 1L, "\"50-1\"");
    }
}
