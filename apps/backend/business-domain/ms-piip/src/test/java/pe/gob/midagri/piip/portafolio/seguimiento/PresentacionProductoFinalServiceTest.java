package pe.gob.midagri.piip.portafolio.seguimiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PresentacionProductoFinalRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PresentacionProductoFinalResponse;

/** Contrato T073 para evidencias múltiples de la presentación final. */
class PresentacionProductoFinalServiceTest {
    @Test
    void requestYResponseConservanEvidenciasMultiples() {
        PresentacionProductoFinalRequest request = new PresentacionProductoFinalRequest(
                "SOLUCION_FUNCIONAL", null, "Resultados", null, 10L, List.of(11L, 12L));
        PresentacionProductoFinalResponse response = new PresentacionProductoFinalResponse(1L, 2L, 1,
                0L, request.tipoProductoFinal(), null, request.resultadosClave(), null, 10L,
                request.evidenciaIds(), "\"1-1\"");
        assertEquals(List.of(11L, 12L), response.evidenciaIds());
    }

    @Test
    void dtoNoExponeEntidadJpa() {
        for (var componente : PresentacionProductoFinalRequest.class.getRecordComponents()) {
            assertFalse(componente.getType().getName().contains(".entity."));
        }
    }
}
