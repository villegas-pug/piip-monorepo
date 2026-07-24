package pe.gob.midagri.piip.portafolio.cierre;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.portafolio.cierre.dto.DecisionProductoFinal;
import pe.gob.midagri.piip.portafolio.cierre.dto.DecisionProductoFinalRequest;
import pe.gob.midagri.piip.portafolio.cierre.dto.DecisionProductoFinalResponse;
import pe.gob.midagri.piip.portafolio.cierre.service.impl.DecisionProductoFinalServiceImpl;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.seguimiento.service.AptitudDocumentalService;
import pe.gob.midagri.piip.portafolio.transicion.TransicionCommand;
import pe.gob.midagri.piip.portafolio.transicion.TransicionDetail;
import pe.gob.midagri.piip.portafolio.transicion.TransicionEstadoService;

/** Reglas T080: requisitos documentales y delegación en la máquina canónica. */
@ExtendWith(MockitoExtension.class)
class DecisionProductoFinalServiceTest {

    @Mock private TransicionEstadoService transicionService;
    @Mock private AptitudDocumentalService aptitudDocumentalService;

    @Test
    void aprobar_validaDocumentoFormalYPersisteTipoDentroDeLaTransicion() {
        when(aptitudDocumentalService.esAptoParaTransicion(801L,
                AptitudDocumentalService.TipoEvidenciaTransicion.APROBACION_PRODUCTO_FINAL))
                .thenReturn(true);
        when(transicionService.transicionar(anyLong(), any(), any(), anyString(), anyString()))
                .thenReturn(detalle(EstadoIniciativa.PRODUCTO_APROBADO));

        DecisionProductoFinalResponse response = service().decidir(100L,
                new DecisionProductoFinalRequest(DecisionProductoFinal.APROBAR, 801L, null,
                        "SOLUCION_FUNCIONAL", null), "\"100-4\"", contexto(), "key-1", "{}");

        ArgumentCaptor<TransicionCommand> command = ArgumentCaptor.forClass(TransicionCommand.class);
        verify(transicionService).transicionar(anyLong(), command.capture(), any(), anyString(), anyString());
        assertEquals(EstadoIniciativa.PRODUCTO_APROBADO, command.getValue().destino());
        assertEquals("SOLUCION_FUNCIONAL", command.getValue().tipoProductoFinal());
        assertEquals("PRODUCTO_APROBADO", response.estado().name());
    }

    @Test
    void noAprobar_exigeObservacionYEvidenciaApta() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service().decidir(100L,
                        new DecisionProductoFinalRequest(DecisionProductoFinal.NO_APROBAR, null,
                                802L, "PROTOTIPO_CONCEPTUALIZADO", " "),
                        "\"100-4\"", contexto(), "key-2", "{}"));

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertEquals("OBSERVATION_REQUIRED", error.getReason());
    }

    @Test
    void noAprobar_rechazaEvidenciaNoApta() {
        when(aptitudDocumentalService.esAptoParaTransicion(802L,
                AptitudDocumentalService.TipoEvidenciaTransicion.NO_APROBACION_PRODUCTO_FINAL))
                .thenReturn(false);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service().decidir(100L,
                        new DecisionProductoFinalRequest(DecisionProductoFinal.NO_APROBAR, null,
                                802L, "PROTOTIPO_CONCEPTUALIZADO", "No cumple"),
                        "\"100-4\"", contexto(), "key-3", "{}"));

        assertEquals("EVIDENCE_NOT_ELIGIBLE", error.getReason());
    }

    private DecisionProductoFinalServiceImpl service() {
        return new DecisionProductoFinalServiceImpl(transicionService, aptitudDocumentalService);
    }

    private static PortafolioAuthContext contexto() {
        return new PortafolioAuthContext("sub-autoridad", 8L, 500L,
                "Autoridad", 10L, 10L, "corr-1");
    }

    private static TransicionDetail detalle(EstadoIniciativa estado) {
        return new TransicionDetail(100L, EstadoIniciativa.PROYECTO_EJECUCION, estado,
                501L, LocalDateTime.now(), "sub-autoridad", 5L, "\"100-5\"");
    }
}
