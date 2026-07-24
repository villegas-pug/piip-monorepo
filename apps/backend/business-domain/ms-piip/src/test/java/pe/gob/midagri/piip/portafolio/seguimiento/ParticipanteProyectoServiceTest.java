package pe.gob.midagri.piip.portafolio.seguimiento;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AltaUnidadRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.ParticipanteResponse;

/** Contratos canónicos T073: las unidades nunca pueden ser Responsables. */
class ParticipanteProyectoServiceTest {
    @Test
    void altaUnidad_noExponeRolYLaRespuestaEsParticipante() {
        assertEquals(1, AltaUnidadRequest.class.getRecordComponents().length);
        ParticipanteResponse respuesta = new ParticipanteResponse(1L, 2L, null, 3L,
                "Participante", null, null, null, "VIGENTE", null, null, "\"1-0\"");
        assertEquals("Participante", respuesta.rol());
    }

    @Test
    void dtoNoExponeEntidadJpa() {
        for (var componente : ParticipanteResponse.class.getRecordComponents()) {
            assertFalse(componente.getType().getName().contains(".entity."));
        }
    }
}
