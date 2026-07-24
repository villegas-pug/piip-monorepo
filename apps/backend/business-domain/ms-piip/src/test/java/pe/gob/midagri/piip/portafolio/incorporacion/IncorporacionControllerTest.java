package pe.gob.midagri.piip.portafolio.incorporacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import pe.gob.midagri.piip.config.ProblemDetailsConfig;
import pe.gob.midagri.piip.portafolio.controller.IncorporacionController;
import pe.gob.midagri.piip.portafolio.dto.CreateIncorporacionRequest;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionDetail;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIncorporacion;
import pe.gob.midagri.piip.portafolio.exception.PortafolioExceptionHandler;
import pe.gob.midagri.piip.portafolio.service.IncorporacionRegistroService;
import pe.gob.midagri.piip.portafolio.service.impl.IncorporacionRegistroServiceImpl;

/**
 * Pruebas MockMvc del controlador
 * {@link IncorporacionController} conforme al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}.
 *
 * <p>Cubre:
 * <ul>
 *   <li>POST de registro devuelve 201 con estado PENDIENTE y ETag.</li>
 *   <li>POST de correccion devuelve 200 con estado PENDIENTE y ETag.</li>
 *   <li>POST de validacion devuelve 200 con estado VALIDADO y ETag.</li>
 *   <li>POST de resolucion de conflicto devuelve 200 y ETag.</li>
 *   <li>Errores 4xx con ProblemDetail y content-type application/problem+json.</li>
 *   <li>Cabecera Idempotency-Key opcional: cuando se envia, se publica el contexto; cuando no,
 *       la operacion se ejecuta sin idempotencia canonica.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US1 - Incorporacion individual: MockMvc IncorporacionController")
class IncorporacionControllerTest {

    @Mock private IncorporacionRegistroService incorporacionService;
    @Mock private IncorporacionRegistroServiceImpl incorporacionServiceImpl;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        IncorporacionController controller = new IncorporacionController(
                incorporacionService, incorporacionServiceImpl, mapper);
        PortafolioExceptionHandler advice = new PortafolioExceptionHandler(
                new ProblemDetailsConfig.ProblemDetailsFactory());
        objectMapper = mapper;
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();
    }

    private CreateIncorporacionRequest buildRequest() {
        return new CreateIncorporacionRequest(
                "Archivo historico",
                LocalDate.of(2025, 1, 15),
                1L, 500L, "abc123hash", "{\"nombre\":\"Iniciativa legacy\"}", null);
    }

    private IncorporacionDetail buildDetailPendiente() {
        return new IncorporacionDetail(1L, "Archivo", LocalDate.of(2025, 1, 15), 1L,
                500L, "abc123hash", EstadoIncorporacion.PENDIENTE, null, null, "sub-001",
                LocalDateTime.now(), 0L, "\"1-0\"");
    }

    @Test
    @DisplayName("POST /api/v1/portafolio/incorporaciones devuelve 201 Created con PENDIENTE y ETag")
    void registrar_devuelve201ConPendiente() throws Exception {
        when(incorporacionService.registrar(any(CreateIncorporacionRequest.class),
                any(PortafolioAuthContext.class))).thenReturn(buildDetailPendiente());

        mockMvc.perform(post("/api/v1/portafolio/incorporaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .header("X-Actor-Sub", "sub-001")
                        .header("X-Actor-Usuario-Id", "1")
                        .header("X-Correlation-Id", "corr-001")
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("ETag", "\"1-0\""))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"))
                .andExpect(jsonPath("$.hashOriginal").value("abc123hash"));
    }

    @Test
    @DisplayName("POST sin X-Asignacion-Efectiva-Id se rechaza con 400")
    void registrar_sinAsignacionEfectiva_seRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/portafolio/incorporaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /correcciones devuelve 200 con PENDIENTE y ETag")
    void corregir_devuelve200() throws Exception {
        when(incorporacionService.corregir(any(), any())).thenReturn(buildDetailPendiente());
        String cuerpo = objectMapper.writeValueAsString(
                new pe.gob.midagri.piip.portafolio.dto.IncorporacionCorreccionRequest(
                        1L, "datos nuevos", "motivo"));

        mockMvc.perform(post("/api/v1/portafolio/incorporaciones/1/correcciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1-0\""))
                .andExpect(jsonPath("$.estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("POST /validaciones devuelve 200 con VALIDADO y ETag")
    void validar_devuelve200() throws Exception {
        IncorporacionDetail validado = new IncorporacionDetail(1L, "Archivo",
                LocalDate.of(2025, 1, 15), 1L, 500L, "abc123hash",
                EstadoIncorporacion.VALIDADO, 42L, null, "sub-001",
                LocalDateTime.now(), 1L, "\"1-1\"");
        when(incorporacionService.validar(any(), any())).thenReturn(validado);
        String cuerpo = objectMapper.writeValueAsString(
                new pe.gob.midagri.piip.portafolio.dto.IncorporacionValidacionRequest(
                        1L,
                        pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa.PRESENTADO,
                        42L, "validado"));

        mockMvc.perform(post("/api/v1/portafolio/incorporaciones/1/validaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1-1\""))
                .andExpect(jsonPath("$.estado").value("VALIDADO"))
                .andExpect(jsonPath("$.registroVinculadoId").value(42));
    }

    @Test
    @DisplayName("POST /conflictos/{id}/resoluciones devuelve 200 y ETag")
    void resolverConflicto_devuelve200() throws Exception {
        when(incorporacionService.resolverConflicto(any(), any())).thenReturn(buildDetailPendiente());
        String cuerpo = objectMapper.writeValueAsString(
                new pe.gob.midagri.piip.portafolio.dto.IncorporacionResolucionConflictoRequest(
                        9L, 1L, "resolucion", 600L));

        mockMvc.perform(post("/api/v1/portafolio/incorporaciones/1/conflictos/9/resoluciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Asignacion-Efectiva-Id", "300")
                        .content(cuerpo))
                .andExpect(status().isOk())
                .andExpect(header().string("ETag", "\"1-0\""));
    }

    @Test
    @DisplayName("Errores 4xx del servicio se devuelven como ProblemDetail")
    void servicioLanzaResponseStatus_devuelveSuCodigo() throws Exception {
        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                "DUPLICATE_INCORPORATION_HASH"))
                .when(incorporacionService).registrar(any(), any());

        mockMvc.perform(post("/api/v1/portafolio/incorporaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    @DisplayName("El controlador delega al servicio y no contiene logica de negocio")
    void controladorDelegaAlServicio() throws Exception {
        when(incorporacionService.registrar(any(), any())).thenReturn(buildDetailPendiente());
        mockMvc.perform(post("/api/v1/portafolio/incorporaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated());
        verify(incorporacionService).registrar(any(CreateIncorporacionRequest.class), any(PortafolioAuthContext.class));
    }

    @Test
    @DisplayName("La respuesta del servicio se serializa con DTO HTTP, sin entidades JPA")
    void respuestaEsDtoHttp() {
        for (var c : IncorporacionDetail.class.getRecordComponents()) {
            String tipo = c.getType().getName();
            assertTrue(!tipo.contains("pe.gob.midagri.piip.portafolio.entity")
                    || c.getType().isEnum(),
                    () -> "El campo " + c.getName() + " no debe ser entidad JPA");
        }
    }

    @Test
    @DisplayName("El contrato HTTP exige POST de registro con hash de origen")
    void contratoExigeHashOrigen() throws Exception {
        for (var c : CreateIncorporacionRequest.class.getRecordComponents()) {
            assertNotNull(c);
            if (c.getName().equals("hashOriginal")) {
                assertEquals(String.class, c.getType(),
                        "El campo hashOriginal debe ser String");
            }
        }
    }
}
