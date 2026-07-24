package pe.gob.midagri.piip.portafolio.cierre;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.portafolio.cierre.controller.DecisionProductoFinalController;
import pe.gob.midagri.piip.portafolio.cierre.dto.DecisionProductoFinalResponse;
import pe.gob.midagri.piip.portafolio.cierre.service.DecisionProductoFinalService;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.config.ProblemDetailsConfig;
import pe.gob.midagri.piip.portafolio.exception.PortafolioExceptionHandler;

/** Contrato MockMvc T078 migrado al controlador canónico de T080. */
@ExtendWith(MockitoExtension.class)
@DisplayName("US5 - Decisión de producto final")
class DecisionProductoFinalControllerTest {

    @Mock private DecisionProductoFinalService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new DecisionProductoFinalController(service, new ObjectMapper()))
                .setControllerAdvice(new PortafolioExceptionHandler(
                        new ProblemDetailsConfig.ProblemDetailsFactory()))
                .build();
    }

    @Test
    @DisplayName("La Autoridad aprueba con documento formal y tipo canónico")
    void aprobarProducto_devuelve200() throws Exception {
        when(service.decidir(anyLong(), any(), anyString(), any(), anyString(), anyString()))
                .thenReturn(new DecisionProductoFinalResponse(100L,
                        EstadoIniciativa.PRODUCTO_APROBADO, "SOLUCION_FUNCIONAL", 501L,
                        LocalDateTime.now(), "\"100-5\""));

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/producto-final/decisiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "decision-1")
                        .header("If-Match", "\"100-4\"")
                        .header("X-Asignacion-Efectiva-Id", "500")
                        .header("X-Perfil-Efectivo", "Autoridad")
                        .content("{\"decision\":\"APROBAR\",\"documentoId\":801,"
                                + "\"tipoProductoFinal\":\"SOLUCION_FUNCIONAL\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"100-5\""))
                .andExpect(jsonPath("$.estado").value("PRODUCTO_APROBADO"));
        verify(service).decidir(anyLong(), any(), anyString(), any(), anyString(), anyString());
    }

    @Test
    @DisplayName("La no aprobación devuelve la validación de evidencia apta")
    void noAprobar_sinEvidenciaApta_devuelve422() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "EVIDENCE_NOT_ELIGIBLE"))
                .when(service).decidir(anyLong(), any(), anyString(), any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/producto-final/decisiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "decision-2")
                        .header("If-Match", "\"100-4\"")
                        .header("X-Asignacion-Efectiva-Id", "500")
                        .header("X-Perfil-Efectivo", "Evaluador")
                        .content("{\"decision\":\"NO_APROBAR\",\"observacion\":\"No cumple\","
                                + "\"evidenciaId\":802,\"tipoProductoFinal\":\"SOLUCION_FUNCIONAL\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("La decisión sin If-Match se rechaza con 428")
    void decidir_sinIfMatch_rechazaPrecondicion() throws Exception {
        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/producto-final/decisiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "decision-3")
                        .header("X-Asignacion-Efectiva-Id", "500")
                        .header("X-Perfil-Efectivo", "Autoridad")
                        .content("{\"decision\":\"APROBAR\",\"documentoId\":801,\"tipoProductoFinal\":\"SOLUCION_FUNCIONAL\"}"))
                .andExpect(status().isPreconditionRequired());
    }

    @Test
    @DisplayName("La decisión con ETag obsoleto devuelve 412")
    void decidir_conIfMatchObsoleto_devuelve412() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "STATE_CHANGED"))
                .when(service).decidir(anyLong(), any(), anyString(), any(), anyString(), anyString());
        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/producto-final/decisiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "decision-4")
                        .header("If-Match", "\"100-3\"")
                        .header("X-Asignacion-Efectiva-Id", "500")
                        .header("X-Perfil-Efectivo", "Autoridad")
                        .content("{\"decision\":\"APROBAR\",\"documentoId\":801,"
                                + "\"tipoProductoFinal\":\"SOLUCION_FUNCIONAL\"}"))
                .andExpect(status().isPreconditionFailed());
    }
}
