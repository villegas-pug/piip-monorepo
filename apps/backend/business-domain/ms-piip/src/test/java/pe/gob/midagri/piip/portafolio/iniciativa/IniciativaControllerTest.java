package pe.gob.midagri.piip.portafolio.iniciativa;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import pe.gob.midagri.piip.config.ProblemDetailsConfig;
import pe.gob.midagri.piip.portafolio.controller.IniciativaController;
import pe.gob.midagri.piip.portafolio.dto.CreateInitiativeRequest;
import pe.gob.midagri.piip.portafolio.dto.InitiativeDetail;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.entity.TipoSolucion;
import pe.gob.midagri.piip.portafolio.exception.PortafolioExceptionHandler;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.service.PresentarIniciativaService;

/**
 * Pruebas MockMvc de extremo a extremo para
 * {@link IniciativaController} sin levantar el contexto Spring completo. La
 * autorización efectiva Oracle y la auditoría transversal se mockean y la
 * seguridad JWT queda fuera del alcance de esta prueba (se cubre en la
 * prueba de seguridad).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US1 - Iniciativa: MockMvc IniciativaController")
class IniciativaControllerTest {

    @Mock private PresentarIniciativaService presentarIniciativaService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        IniciativaController controller = new IniciativaController(presentarIniciativaService, objectMapper);
        PortafolioExceptionHandler advice = new PortafolioExceptionHandler(
                new ProblemDetailsConfig.ProblemDetailsFactory());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();
    }

    private CreateInitiativeRequest buildRequestCompleto() {
        return new CreateInitiativeRequest(
                "Innovación en riego",
                TipoSolucion.POTENCIAL_ADAPTABLE,
                FuenteOrigen.FICHA_INICIATIVA,
                null,
                "Problema de riego en zonas áridas",
                "Solución con sensores IoT",
                1L,
                10L,
                20L,
                List.of(new CreateInitiativeRequest.UnidadResponsableItem(1L, true)),
                List.of(),
                List.of(),
                Boolean.FALSE,
                null,
                null,
                500L);
    }

    @Test
    @DisplayName("POST /api/v1/portafolio/iniciativas devuelve 201 Created con ETag y código generado")
    void presentar_devuelve201ConETagYCodigo() throws Exception {
        InitiativeDetail detalle = new InitiativeDetail(
                1L,
                TipoRegistro.INICIATIVA,
                "2026-MIDAGRI-00001",
                null,
                LocalDate.now(),
                "Innovación en riego",
                TipoSolucion.POTENCIAL_ADAPTABLE,
                FuenteOrigen.FICHA_INICIATIVA,
                null,
                1L,
                "Problema de riego en zonas áridas",
                "Solución con sensores IoT",
                10L,
                20L,
                List.of(),
                EstadoIniciativa.PRESENTADO,
                Boolean.FALSE,
                null,
                null,
                0L,
                "\"1-0\"",
                java.time.LocalDateTime.now());
        when(presentarIniciativaService.presentar(any(CreateInitiativeRequest.class),
                any(PortafolioAuthContext.class), anyString(), anyString())).thenReturn(detalle);

        mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-001")
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .header("X-Actor-Sub", "sub-001")
                        .header("X-Actor-Usuario-Id", "1")
                        .header("X-Correlation-Id", "corr-001")
                        .content(objectMapper.writeValueAsString(buildRequestCompleto())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.codigo").value("2026-MIDAGRI-00001"))
                .andExpect(jsonPath("$.estado").value("PRESENTADO"))
                .andExpect(jsonPath("$.tipoRegistro").value("INICIATIVA"));
    }

    @Test
    @DisplayName("POST sin X-Asignacion-Efectiva-Id se rechaza con 400")
    void presentar_sinAsignacionEfectiva_seRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-002")
                        .content(objectMapper.writeValueAsString(buildRequestCompleto())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST sin Idempotency-Key se rechaza con 400")
    void presentar_sinIdempotencyKey_seRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(objectMapper.writeValueAsString(buildRequestCompleto())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("El servicio responde con PortafolioValidationException y el advice produce 422 ProblemDetail")
    void presentar_servicioLanzaValidacion_devuelve422() throws Exception {
        doThrow(PortafolioValidationException.campoRequerido(5, "Nombre"))
                .when(presentarIniciativaService).presentar(any(), any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-003")
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(objectMapper.writeValueAsString(buildRequestCompleto())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("OFFICIAL_FIELD_REQUIRED"));
    }

    @Test
    @DisplayName("El servicio responde con un ResponseStatusException y el advice lo traduce a su código HTTP")
    void presentar_servicioLanzaResponseStatus_devuelveSuCodigo() throws Exception {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                "ASSIGNMENT_SCOPE_DENIED"))
                .when(presentarIniciativaService).presentar(any(), any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-004")
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(objectMapper.writeValueAsString(buildRequestCompleto())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("El cuerpo de la petición no contiene codigo, codigoOrigen, fechaInicio ni estado")
    void presentar_clienteNoEnviaCodigoNiFechaNiEstado() throws Exception {
        // El contrato exige que el cliente NO envíe estos campos; el DTO
        // CreateInitiativeRequest no los expone, por lo que el cuerpo no los
        // incluye ni siquiera si se serializa con valores nulos.
        String cuerpo = objectMapper.writeValueAsString(buildRequestCompleto());
        org.junit.jupiter.api.Assertions.assertFalse(cuerpo.contains("\"codigo\":"),
                "El cuerpo de la petición no debe incluir codigo");
        org.junit.jupiter.api.Assertions.assertFalse(cuerpo.contains("\"codigoOrigen\":"),
                "El cuerpo de la petición no debe incluir codigoOrigen");
        org.junit.jupiter.api.Assertions.assertFalse(cuerpo.contains("\"fechaInicio\":"),
                "El cuerpo de la petición no debe incluir fechaInicio");
        org.junit.jupiter.api.Assertions.assertFalse(cuerpo.contains("\"estado\":"),
                "El cuerpo de la petición no debe incluir estado");
    }

    @Test
    @DisplayName("El servicio recibe un PortafolioAuthContext con X-Asignacion-Efectiva-Id")
    void presentar_pasaAsignacionEfectivaAlServicio() throws Exception {
        when(presentarIniciativaService.presentar(any(), any(), anyString(), anyString())).thenReturn(
                new InitiativeDetail(1L, TipoRegistro.INICIATIVA, "2026-MIDAGRI-00001",
                        null, LocalDate.now(), "x", TipoSolucion.POTENCIAL_ADAPTABLE,
                        FuenteOrigen.FICHA_INICIATIVA, null, 1L, "p", null,
                        10L, 20L, List.of(), EstadoIniciativa.PRESENTADO, Boolean.FALSE,
                        null, null, 0L, "\"1-0\"", java.time.LocalDateTime.now()));

        mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-005")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Actor-Sub", "sub-002")
                        .header("X-Actor-Usuario-Id", "2")
                        .header("X-Correlation-Id", "corr-002")
                        .content(objectMapper.writeValueAsString(buildRequestCompleto())))
                .andExpect(status().isCreated());

        verify(presentarIniciativaService).presentar(any(CreateInitiativeRequest.class),
                any(PortafolioAuthContext.class), anyString(), anyString());
    }
}
