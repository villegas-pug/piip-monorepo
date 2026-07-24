package pe.gob.midagri.piip.portafolio.responsable;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.portafolio.controller.ResponsableTitularController;
import pe.gob.midagri.piip.portafolio.dto.ResponsibleReplacementDetail;
import pe.gob.midagri.piip.portafolio.service.ResponsableTitularService;

@ExtendWith(MockitoExtension.class)
class ResponsableTitularControllerTest {

    @Mock private ResponsableTitularService service;
    @Mock private IdempotencyService idempotencia;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new ResponsableTitularController(service, idempotencia, new ObjectMapper())).build();
    }

    @Test
    void delegaElComandoIdempotenteAlServicioSinAutoridadEnElControlador() throws Exception {
        ResponsibleReplacementDetail detail = new ResponsibleReplacementDetail(7L, 101L, 8L, 102L, null);
        when(service.sustituir(any(), any(), any())).thenReturn(detail);
        when(idempotencia.execute(any(), any())).thenAnswer(invocation -> {
            IdempotencyService.IdempotencyResponse response = invocation
                    .<IdempotencyService.IdempotentOperation>getArgument(1).execute();
            return new IdempotencyService.IdempotencyResult(
                    response.recursoTipo(), response.recursoId(), response.respuestaJson(), false);
        });

        mockMvc.perform(post("/api/v1/portafolio/registros/10/sustituciones-responsable")
                        .header("Idempotency-Key", "key-1")
                        .header("X-Asignacion-Efectiva-Id", "9")
                        .contentType("application/json")
                        .content("{\"nuevoResponsableId\":102,\"motivo\":\"Cambio formal\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("titularAnteriorId")))
                .andExpect(content().string(containsString("nuevoResponsableId")));

        verify(service).sustituir(any(), any(), any());
    }
}
