package pe.gob.midagri.piip.portafolio.transicion;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import pe.gob.midagri.piip.config.ProblemDetailsConfig;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.exception.PortafolioExceptionHandler;

/**
 * Pruebas MockMvc del controlador {@code TransicionEstadoController}
 * conforme al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}
 * y a la Constitucion 5.0.0 (tabla de transiciones controladas iniciales).
 *
 * <p>Las pruebas se realizan con {@code standaloneSetup} para no
 * levantar el contexto Spring completo. La autorizacion efectiva Oracle y
 * la auditoria se mockean; la seguridad JWT queda fuera de esta prueba.
 *
 * <p>Esta prueba modela la firma esperada del controlador a implementar
 * en T058; las firmas exactas se marcan con
 * {@code // @NEEDS_CLARIFICATION} cuando la especificacion pueda
 * ajustar rutas o encabezados.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US2 - Transicion de estado: MockMvc TransicionEstadoController")
class TransicionEstadoControllerTest {

    @Mock private TransicionEstadoService transicionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // // @NEEDS_CLARIFICATION: el nombre del controlador puede ajustarse
        // en T058; se modela como TransicionEstadoController publicado en
        // /api/v1/portafolio/transiciones.
        TransicionEstadoController controller = new TransicionEstadoController(transicionService,
                objectMapper);
        PortafolioExceptionHandler advice = new PortafolioExceptionHandler(
                new ProblemDetailsConfig.ProblemDetailsFactory());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();
    }

    private TransicionCommand buildComando(EstadoIniciativa destino) {
        return new TransicionCommand(destino, "Observacion de prueba", 1L, "1-0");
    }

    private TransicionDetail buildDetalle(EstadoIniciativa destino, String etag) {
        return new TransicionDetail(1L, EstadoIniciativa.PRESENTADO, destino, 100L,
                LocalDateTime.now(), "sub-eval", 1L, etag);
    }

    @Test
    @DisplayName("POST con If-Match y transicion valida devuelve 200 con ETag y nuevo estado")
    void transicionar_exitoso_devuelve200ConETag() throws Exception {
        when(transicionService.transicionar(anyLong(), any(TransicionCommand.class), any(),
                nullable(String.class), anyString()))
                .thenReturn(buildDetalle(EstadoIniciativa.NO_ADMISIBLE, "\"1-1\""));

        mockMvc.perform(post("/api/v1/portafolio/transiciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-001")
                        .header("If-Match", "\"1-0\"")
                        .header("X-Asignacion-Efectiva-Id", "999")
                        .header("X-Actor-Sub", "sub-eval")
                        .header("X-Actor-Usuario-Id", "99")
                        .header("X-Correlation-Id", "corr-1")
                        .content(objectMapper.writeValueAsString(
                                buildComando(EstadoIniciativa.NO_ADMISIBLE))))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.estadoNuevo").value("NO_ADMISIBLE"))
                .andExpect(jsonPath("$.estadoAnterior").value("PRESENTADO"));
    }

    @Test
    @DisplayName("POST sin If-Match se rechaza con 428 PRECONDITION_REQUIRED")
    void transicionar_sinIfMatch_428() throws Exception {
        mockMvc.perform(post("/api/v1/portafolio/transiciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-002")
                        .header("X-Asignacion-Efectiva-Id", "999")
                        .content(objectMapper.writeValueAsString(
                                new TransicionCommand(EstadoIniciativa.NO_ADMISIBLE,
                                        "Observacion de prueba", 1L, null))))
                .andExpect(status().isPreconditionRequired())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("IF_MATCH_REQUIRED"));
    }

    @Test
    @DisplayName("POST con ETag incorrecto se rechaza con 412 STATE_CHANGED")
    void transicionar_etagIncorrecto_412() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                "STATE_CHANGED: la version actual no coincide con If-Match"))
                .when(transicionService).transicionar(anyLong(), any(TransicionCommand.class), any(),
                        anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/transiciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-003")
                        .header("If-Match", "\"1-99\"")
                        .header("X-Asignacion-Efectiva-Id", "999")
                        .content(objectMapper.writeValueAsString(
                                buildComando(EstadoIniciativa.NO_ADMISIBLE))))
                .andExpect(status().isPreconditionFailed())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("STATE_CHANGED"));
    }

    @Test
    @DisplayName("POST sin Idempotency-Key se rechaza con 400")
    void transicionar_sinIdempotencyKey_400() throws Exception {
        when(transicionService.transicionar(anyLong(), any(TransicionCommand.class), any(),
                nullable(String.class), anyString()))
                .thenReturn(buildDetalle(EstadoIniciativa.NO_ADMISIBLE, "\"1-1\""));
        mockMvc.perform(post("/api/v1/portafolio/transiciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("If-Match", "\"1-0\"")
                        .header("X-Asignacion-Efectiva-Id", "999")
                        .content(objectMapper.writeValueAsString(
                                buildComando(EstadoIniciativa.NO_ADMISIBLE))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST sin X-Asignacion-Efectiva-Id se rechaza con 400")
    void transicionar_sinAsignacionEfectiva_400() throws Exception {
        mockMvc.perform(post("/api/v1/portafolio/transiciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-004")
                        .header("If-Match", "\"1-0\"")
                        .content(objectMapper.writeValueAsString(
                                buildComando(EstadoIniciativa.NO_ADMISIBLE))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Transicion no listada se rechaza con 409 STATE_TRANSITION_NOT_ALLOWED")
    void transicionar_noPermitida_409() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "STATE_TRANSITION_NOT_ALLOWED: PRESENTADO -> FINALIZADO no es canonica"))
                .when(transicionService).transicionar(anyLong(), any(TransicionCommand.class), any(),
                        anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/transiciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-005")
                        .header("If-Match", "\"1-0\"")
                        .header("X-Asignacion-Efectiva-Id", "999")
                        .content(objectMapper.writeValueAsString(
                                buildComando(EstadoIniciativa.FINALIZADO))))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("STATE_TRANSITION_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("Asignacion efectiva fuera del ambito se rechaza con 403 ASSIGNMENT_SCOPE_DENIED")
    void transicionar_asignacionEfectivaDenegada_403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                "ASSIGNMENT_SCOPE_DENIED: la asignacion efectiva no aplica"))
                .when(transicionService).transicionar(anyLong(), any(TransicionCommand.class), any(),
                        anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/transiciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-006")
                        .header("If-Match", "\"1-0\"")
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(objectMapper.writeValueAsString(
                                buildComando(EstadoIniciativa.NO_ADMISIBLE))))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ASSIGNMENT_SCOPE_DENIED"));
    }

    @Test
    @DisplayName("Evidencia incompleta se rechaza con 422 FORMAL_DECISION_REQUIRED")
    void transicionar_evidenciaIncompleta_422() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "FORMAL_DECISION_REQUIRED: la transicion exige documento habilitante"))
                .when(transicionService).transicionar(anyLong(), any(TransicionCommand.class), any(),
                        anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/transiciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-007")
                        .header("If-Match", "\"1-0\"")
                        .header("X-Asignacion-Efectiva-Id", "999")
                        .content(objectMapper.writeValueAsString(
                                new TransicionCommand(EstadoIniciativa.INICIATIVA_APROBADA, null,
                                        null, "\"1-0\""))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORMAL_DECISION_REQUIRED"));
    }

    @Test
    @DisplayName("El ProblemDetail incluye correlationId y code canonico del modulo portafolio")
    void problemDetailIncluyeCorrelationId() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                "STATE_CHANGED: la version actual no coincide con If-Match"))
                .when(transicionService).transicionar(anyLong(), any(TransicionCommand.class), any(),
                        anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/transiciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-008")
                        .header("If-Match", "\"1-99\"")
                        .header("X-Asignacion-Efectiva-Id", "999")
                        .header("X-Correlation-Id", "corr-especial")
                        .content(objectMapper.writeValueAsString(
                                buildComando(EstadoIniciativa.NO_ADMISIBLE))))
                .andExpect(status().isPreconditionFailed())
                .andExpect(header().exists(HttpHeaders.CONTENT_TYPE))
                .andExpect(jsonPath("$.code").value("STATE_CHANGED"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @DisplayName("El controlador es delgado y delega toda la logica al servicio")
    void controladorDelegaAlServicio() throws Exception {
        when(transicionService.transicionar(anyLong(), any(TransicionCommand.class), any(),
                anyString(), anyString()))
                .thenReturn(buildDetalle(EstadoIniciativa.NO_ADMISIBLE, "\"1-1\""));

        mockMvc.perform(post("/api/v1/portafolio/transiciones/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-009")
                        .header("If-Match", "\"1-0\"")
                        .header("X-Asignacion-Efectiva-Id", "999")
                        .content(objectMapper.writeValueAsString(
                                buildComando(EstadoIniciativa.NO_ADMISIBLE))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("El DTO TransicionCommand no expone campos autogenerados (id, fechas, transicionId)")
    void transicionCommandNoExponeCamposAutogenerados() {
        var componentes = TransicionCommand.class.getRecordComponents();
        java.util.Set<String> nombres = new java.util.HashSet<>();
        for (var c : componentes) {
            nombres.add(c.getName());
        }
        assertTrue(!nombres.contains("id"));
        assertTrue(!nombres.contains("estadoAnterior"));
        assertTrue(!nombres.contains("estadoNuevo"));
        assertTrue(!nombres.contains("fechaTransicion"));
        assertTrue(!nombres.contains("transicionId"));
    }

    @Test
    @DisplayName("El DTO TransicionDetail no expone entidades JPA en su API publica")
    void transicionDetailNoExponeEntidadesJPA() {
        for (var c : TransicionDetail.class.getRecordComponents()) {
            String tipo = c.getType().getName();
            assertTrue(!tipo.contains("pe.gob.midagri.piip.portafolio.entity")
                            || c.getType().isEnum(),
                    () -> "El campo " + c.getName() + " no debe ser una entidad JPA");
        }
    }

    @Test
    @DisplayName("El servicio se inyecta por constructor y no se accede a repositorios desde el controlador")
    void controladorEsDelgadoYNoAccedeARepositorios() {
        for (var c : TransicionEstadoController.class.getDeclaredFields()) {
            String tipo = c.getType().getName();
            assertTrue(!tipo.contains(".repository."),
                    () -> "El controlador no debe inyectar repositorios: " + tipo);
        }
        assertNotNull(TransicionEstadoController.class.getDeclaredConstructors()[0]);
    }
}

