package pe.gob.midagri.piip.portafolio;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import pe.gob.midagri.piip.config.ApiHeaders;
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
 * Pruebas de contrato MockMvc para el módulo {@code portafolio}.
 * Verifica headers canónicos, ETag, idempotencia y Problem Details
 * según los contratos definidos en T118.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("T118 - Portafolio: contrato HTTP con headers, ETag, idempotencia y Problem Details")
class PortafolioContratoMvcTest {

    @Mock
    private PresentarIniciativaService presentarIniciativaService;

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

    private CreateInitiativeRequest buildRequest() {
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

    private InitiativeDetail buildDetalle(String codigo, String etag) {
        return new InitiativeDetail(
                1L,
                TipoRegistro.INICIATIVA,
                codigo,
                null,
                LocalDate.now(),
                "Innovación en riego",
                TipoSolucion.POTENCIAL_ADAPTABLE,
                FuenteOrigen.FICHA_INICIATIVA,
                null,
                1L,
                "Problema de riego",
                "Solución IoT",
                10L,
                20L,
                List.of(),
                EstadoIniciativa.PRESENTADO,
                Boolean.FALSE,
                null,
                null,
                0L,
                etag,
                java.time.LocalDateTime.now());
    }

    @Nested
    @DisplayName("Headers canónicos")
    class HeadersCanónicos {

        @Test
        @DisplayName("POST exige X-Asignacion-Efectiva-Id y lo refleja en la respuesta")
        void post_exigeXAsignacionEfectivaId() throws Exception {
            InitiativeDetail detalle = buildDetalle("2026-MIDAGRI-00001", "\"1-0\"");
            when(presentarIniciativaService.presentar(any(), any(), anyString(), anyString()))
                    .thenReturn(detalle);

            mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "key-hdr-1")
                            .header("X-Asignacion-Efectiva-Id", "100")
                            .header("X-Actor-Sub", "sub-001")
                            .header("X-Actor-Usuario-Id", "1")
                            .header("X-Correlation-Id", "corr-001")
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("ETag"));
        }

        @Test
        @DisplayName("POST exige Idempotency-Key y lo refleja en la respuesta")
        void post_exigeIdempotencyKey() throws Exception {
            InitiativeDetail detalle = buildDetalle("2026-MIDAGRI-00002", "\"2-0\"");
            when(presentarIniciativaService.presentar(any(), any(), anyString(), anyString()))
                    .thenReturn(detalle);

            mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "key-hdr-2")
                            .header("X-Asignacion-Efectiva-Id", "100")
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("ETag"));
        }

        @Test
        @DisplayName("POST rechaza sin Idempotency-Key con 400")
        void post_sinIdempotencyKey_rechaza400() throws Exception {
            mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Asignacion-Efectiva-Id", "100")
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

        @Test
        @DisplayName("POST rechaza sin X-Asignacion-Efectiva-Id con 400")
        void post_sinXAsignacionEfectivaId_rechaza400() throws Exception {
            mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "key-hdr-3")
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }

    }

    @Nested
    @DisplayName("ETag")
    class ETagTests {

        @Test
        @DisplayName("POST devuelve 201 Created con ETag en la respuesta")
        void post_devuelve201ConETag() throws Exception {
            InitiativeDetail detalle = buildDetalle("2026-MIDAGRI-00004", "\"4-0\"");
            when(presentarIniciativaService.presentar(any(), any(), anyString(), anyString()))
                    .thenReturn(detalle);

            mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "key-etag-1")
                            .header("X-Asignacion-Efectiva-Id", "100")
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isCreated())
                    .andExpect(header().exists("ETag"))
                    .andExpect(header().string("ETag", "\"4-0\""));
        }

    }

    @Nested
    @DisplayName("Idempotencia")
    class IdempotenciaTests {

        @Test
        @DisplayName("POST con Idempotency-Key diferente se acepta")
        void post_conIdempotencyKeyDiferente_seAcepta() throws Exception {
            InitiativeDetail detalle = buildDetalle("2026-MIDAGRI-00007", "\"7-0\"");
            when(presentarIniciativaService.presentar(any(), any(), anyString(), anyString()))
                    .thenReturn(detalle);

            mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "key-idem-nuevo")
                            .header("X-Asignacion-Efectiva-Id", "100")
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("POST sin Idempotency-Key se rechaza con Problem Details")
        void post_sinIdempotencyKey_problemDetails() throws Exception {
            mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Asignacion-Efectiva-Id", "100")
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").exists())
                    .andExpect(jsonPath("$.title").exists());
        }
    }

    @Nested
    @DisplayName("Problem Details (RFC 9457)")
    class ProblemDetailsTests {

        @Test
        @DisplayName("PortafolioValidationException produce 422 con Problem Details")
        void validacionException_produce422ProblemDetails() throws Exception {
            doThrow(PortafolioValidationException.campoRequerido(5, "Nombre"))
                    .when(presentarIniciativaService).presentar(any(), any(), anyString(), anyString());

            mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "key-pd-1")
                            .header("X-Asignacion-Efectiva-Id", "100")
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.type").exists())
                    .andExpect(jsonPath("$.title").exists())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.code").value("OFFICIAL_FIELD_REQUIRED"))
                    .andExpect(jsonPath("$.detail").exists());
        }

        @Test
        @DisplayName("Error de dominio produce Problem Details con código canónico")
        void errorDominio_produceProblemDetails() throws Exception {
            doThrow(new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY,
                    "INITIATIVE_STATE_TRANSITION_INVALID: Transición de estado inválida"))
                    .when(presentarIniciativaService).presentar(any(), any(), anyString(), anyString());

            mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "key-pd-2")
                            .header("X-Asignacion-Efectiva-Id", "100")
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.code").value("INITIATIVE_STATE_TRANSITION_INVALID"));
        }

        @Test
        @DisplayName("El Problem Details incluye traceId para correlación")
        void problemDetails_incluyeTraceId() throws Exception {
            doThrow(PortafolioValidationException.campoRequerido(1, "Nombre"))
                    .when(presentarIniciativaService).presentar(any(), any(), anyString(), anyString());

            mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "key-pd-3")
                            .header("X-Asignacion-Efectiva-Id", "100")
                            .header("X-Correlation-Id", "corr-trace-1")
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                    .andExpect(jsonPath("$.traceId").exists());
        }
    }

    @Nested
    @DisplayName("Contratos de estado HTTP")
    class EstadoHttpTests {

        @Test
        @DisplayName("Recurso creado devuelve 201 Created")
        void recursoCreado_devuelve201() throws Exception {
            InitiativeDetail detalle = buildDetalle("2026-MIDAGRI-00008", "\"8-0\"");
            when(presentarIniciativaService.presentar(any(), any(), anyString(), anyString()))
                    .thenReturn(detalle);

            mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "key-st-1")
                            .header("X-Asignacion-Efectiva-Id", "100")
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("Alcance denegado devuelve 403 Forbidden")
        void alcanceDenegado_devuelve403() throws Exception {
            when(presentarIniciativaService.presentar(any(), any(), anyString(), anyString()))
                    .thenThrow(new org.springframework.web.server.ResponseStatusException(
                            org.springframework.http.HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED"));

            mockMvc.perform(post("/api/v1/portafolio/iniciativas")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Idempotency-Key", "key-st-2")
                            .header("X-Asignacion-Efectiva-Id", "999")
                            .content(objectMapper.writeValueAsString(buildRequest())))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }
}
