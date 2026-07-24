package pe.gob.midagri.piip.portafolio.proyecto;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import java.time.LocalDateTime;
import java.util.List;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import pe.gob.midagri.piip.config.ProblemDetailsConfig;
import pe.gob.midagri.piip.portafolio.controller.ProyectoController;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.exception.PortafolioExceptionHandler;
import pe.gob.midagri.piip.portafolio.dto.CreateDerivedProjectRequest;
import pe.gob.midagri.piip.portafolio.dto.DirectProjectRequest;
import pe.gob.midagri.piip.portafolio.dto.ProjectDetail;
import pe.gob.midagri.piip.portafolio.dto.TipoOrigenDirecto;
import pe.gob.midagri.piip.portafolio.service.CrearProyectoDerivadoService;
import pe.gob.midagri.piip.portafolio.service.CrearProyectoDirectoService;

/**
 * Pruebas MockMvc del controlador {@code ProyectoController} conforme al
 * contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}
 * y a la Constitucion 5.0.0 (US3).
 *
 * <p>Cubre:
 * <ul>
 *   <li>{@code POST /api/v1/portafolio/iniciativas/{id}/proyecto-derivado}
 *       devuelve 201 con ETag y codigo generado.</li>
 *   <li>{@code POST /api/v1/portafolio/proyectos-directos} devuelve 201
 *       con ETag, estado PROYECTO_EJECUCION y vinculo opcional a
 *       iniciativa.</li>
 *   <li>{@code Idempotency-Key} obligatorio en la creacion de
 *       derivado; ausente produce 400.</li>
 *   <li>{@code X-Asignacion-Efectiva-Id} obligatorio en ambos
 *       endpoints; ausente produce 400.</li>
 *   <li>Errores 4xx con {@code application/problem+json} y codigo
 *       canonico del portafolio.</li>
 *   <li>El controlador es delgado: solo delega en los servicios y
 *       nunca accede a repositorios.</li>
 *   <li>Los DTOs son HTTP, no exponen entidades JPA.</li>
 * </ul>
 *
 * <p>Esta prueba modela la firma esperada del controlador que T066
 * implementara; las firmas exactas se marcan con
 * {@code // @NEEDS_CLARIFICATION} cuando la especificacion pueda
 * ajustar rutas, headers o DTOs.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US3 - ProyectoController: MockMvc para derivado y directo")
class ProyectoControllerTest {

    // // @NEEDS_CLARIFICATION: el nombre del controlador puede ajustarse
    // en T066; se modela como ProyectoController publicado en
    // /api/v1/portafolio.

    @Mock private CrearProyectoDerivadoService derivadoService;
    @Mock private CrearProyectoDirectoService directoService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // // @NEEDS_CLARIFICATION: la firma del constructor del controlador
        // se ajustara en T066. Aqui se modela la inyeccion de los dos
        // servicios y el ObjectMapper para serializar el payload.
        ProyectoController controller = new ProyectoController(
                derivadoService, directoService, objectMapper);
        PortafolioExceptionHandler advice = new PortafolioExceptionHandler(
                new ProblemDetailsConfig.ProblemDetailsFactory());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();
    }

    private CreateDerivedProjectRequest buildDerivedRequest() {
        return new CreateDerivedProjectRequest(
                "Proyecto derivado",
                10L, 20L,
                List.of(new CreateDerivedProjectRequest.UnidadDerivadaItem(1L, true)),
                10L,
                FuenteOrigen.FICHA_INICIATIVA,
                "Descripcion del proyecto derivado",
                Boolean.FALSE,
                null,
                "Nota opcional",
                500L);
    }

    private DirectProjectRequest buildDirectRequest() {
        return new DirectProjectRequest(
                TipoOrigenDirecto.HEREDADO,
                "LEGACY-2024-001",
                LocalDate.of(2024, 3, 15),
                "Proyecto heredado",
                10L, 20L, 1L, 10L,
                "Descripcion del proyecto heredado",
                Boolean.FALSE, null, null,
                800L,
                List.of(900L),
                FuenteOrigen.FICHA_INICIATIVA);
    }

    private ProjectDetail buildDetailDerivado() {
        return new ProjectDetail(
                2001L, 1001L,
                "2026-MIDAGRI-00042", null,
                LocalDate.now(),
                "Proyecto derivado",
                TipoRegistro.PROYECTO,
                EstadoIniciativa.PROYECTO_EJECUCION,
                FuenteOrigen.FICHA_INICIATIVA,
                null,
                10L,
                "Problema heredado de la iniciativa",
                null,
                10L, 20L,
                List.of(new ProjectDetail.UnidadResponsableDetail(1L, 1L, "Unidad 1", "U1", true)),
                Boolean.FALSE, null, null,
                500L,
                0L, "\"2001-0\"", LocalDateTime.now());
    }

    private ProjectDetail buildDetailDirecto() {
        return new ProjectDetail(
                9001L, null,
                "2026-MIDAGRI-00010", "LEGACY-2024-001",
                LocalDate.of(2024, 3, 15),
                "Proyecto heredado",
                TipoRegistro.PROYECTO,
                EstadoIniciativa.PROYECTO_EJECUCION,
                FuenteOrigen.FICHA_INICIATIVA,
                null,
                10L,
                "Descripcion del proyecto heredado",
                null,
                10L, 20L,
                List.of(new ProjectDetail.UnidadResponsableDetail(1L, 1L, "Unidad 1", "U1", true)),
                Boolean.FALSE, null, null,
                800L,
                0L, "\"9001-0\"", LocalDateTime.now());
    }

    // ------------------------------------------------------------------
    // Proyecto derivado
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /iniciativas/{id}/proyecto-derivado devuelve 201 con ETag y codigo generado")
    void crearDerivado_devuelve201ConETag() throws Exception {
        when(derivadoService.crear(anyLong(), any(CreateDerivedProjectRequest.class),
                any(PortafolioAuthContext.class), anyString(), anyString()))
                .thenReturn(buildDetailDerivado());

        mockMvc.perform(post("/api/v1/portafolio/iniciativas/1001/proyecto-derivado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-001")
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .header("X-Actor-Sub", "sub-resp")
                        .header("X-Actor-Usuario-Id", "10")
                        .header("X-Correlation-Id", "corr-001")
                        .content(objectMapper.writeValueAsString(buildDerivedRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.codigo").value("2026-MIDAGRI-00042"))
                .andExpect(jsonPath("$.estado").value("PROYECTO_EJECUCION"))
                .andExpect(jsonPath("$.tipoRegistro").value("PROYECTO"))
                .andExpect(jsonPath("$.iniciativaId").value(1001));
    }

    @Test
    @DisplayName("POST derivado sin X-Asignacion-Efectiva-Id se rechaza con 400")
    void crearDerivado_sinAsignacionEfectiva_seRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/portafolio/iniciativas/1001/proyecto-derivado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-002")
                        .content(objectMapper.writeValueAsString(buildDerivedRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST derivado sin Idempotency-Key se rechaza con 400")
    void crearDerivado_sinIdempotencyKey_seRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/portafolio/iniciativas/1001/proyecto-derivado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(objectMapper.writeValueAsString(buildDerivedRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST derivado cuando ya existe uno: 409 DERIVATION_ALREADY_EXISTS")
    void crearDerivado_segundoIntento_409() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "DERIVATION_ALREADY_EXISTS: la iniciativa ya tiene un proyecto derivado"))
                .when(derivadoService).crear(anyLong(), any(CreateDerivedProjectRequest.class),
                        any(PortafolioAuthContext.class), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/iniciativas/1001/proyecto-derivado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-003")
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(objectMapper.writeValueAsString(buildDerivedRequest())))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DERIVATION_ALREADY_EXISTS"));
    }

    @Test
    @DisplayName("POST derivado con iniciativa en estado no aprobado: 409 INITIATIVE_NOT_APPROVED")
    void crearDerivado_iniciativaNoAprobada_409() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "INITIATIVE_NOT_APPROVED: la iniciativa debe estar en INICIATIVA_APROBADA"))
                .when(derivadoService).crear(anyLong(), any(CreateDerivedProjectRequest.class),
                        any(PortafolioAuthContext.class), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/iniciativas/1001/proyecto-derivado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-004")
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(objectMapper.writeValueAsString(buildDerivedRequest())))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INITIATIVE_NOT_APPROVED"));
    }

    @Test
    @DisplayName("POST derivado sin documento formal: 422 FORMAL_DOCUMENT_REQUIRED")
    void crearDerivado_sinDocumentoFormal_422() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "FORMAL_DOCUMENT_REQUIRED: el derivado exige documento formal de aprobacion"))
                .when(derivadoService).crear(anyLong(), any(CreateDerivedProjectRequest.class),
                        any(PortafolioAuthContext.class), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/iniciativas/1001/proyecto-derivado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-005")
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(objectMapper.writeValueAsString(buildDerivedRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORMAL_DOCUMENT_REQUIRED"));
    }

    @Test
    @DisplayName("POST derivado con perfil no autorizado: 403 ASSIGNMENT_SCOPE_DENIED")
    void crearDerivado_perfilNoAutorizado_403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                "ASSIGNMENT_SCOPE_DENIED: solo el Responsable puede crear derivado"))
                .when(derivadoService).crear(anyLong(), any(CreateDerivedProjectRequest.class),
                        any(PortafolioAuthContext.class), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/iniciativas/1001/proyecto-derivado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-006")
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(objectMapper.writeValueAsString(buildDerivedRequest())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ASSIGNMENT_SCOPE_DENIED"));
    }

    // ------------------------------------------------------------------
    // Proyecto directo
    // ------------------------------------------------------------------

    @Test
    @DisplayName("POST /proyectos-directos devuelve 201 con ETag, codigo y codigoOrigen")
    void crearDirecto_devuelve201ConETag() throws Exception {
        when(directoService.crear(any(DirectProjectRequest.class),
                any(PortafolioAuthContext.class), anyString(), anyString()))
                .thenReturn(buildDetailDirecto());

        mockMvc.perform(post("/api/v1/portafolio/proyectos-directos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-101")
                        .header("X-Asignacion-Efectiva-Id", "500")
                        .header("X-Actor-Sub", "sub-aut")
                        .header("X-Actor-Usuario-Id", "50")
                        .header("X-Correlation-Id", "corr-101")
                        .content(objectMapper.writeValueAsString(buildDirectRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.codigo").value("2026-MIDAGRI-00010"))
                .andExpect(jsonPath("$.codigoOrigen").value("LEGACY-2024-001"))
                .andExpect(jsonPath("$.estado").value("PROYECTO_EJECUCION"))
                .andExpect(jsonPath("$.tipoRegistro").value("PROYECTO"));
    }

    @Test
    @DisplayName("POST /proyectos-directos sin X-Asignacion-Efectiva-Id se rechaza con 400")
    void crearDirecto_sinAsignacionEfectiva_seRechaza() throws Exception {
        mockMvc.perform(post("/api/v1/portafolio/proyectos-directos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-102")
                        .content(objectMapper.writeValueAsString(buildDirectRequest())))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /proyectos-directos cuando ya hay uno activo: 409 DIRECT_PROJECT_NOT_AUTHORIZED")
    void crearDirecto_segundoActivo_409() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "DIRECT_PROJECT_NOT_AUTHORIZED: ya existe un proyecto directo activo en la unidad y anio"))
                .when(directoService).crear(any(DirectProjectRequest.class),
                        any(PortafolioAuthContext.class), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos-directos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-103")
                        .header("X-Asignacion-Efectiva-Id", "500")
                        .content(objectMapper.writeValueAsString(buildDirectRequest())))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("DIRECT_PROJECT_NOT_AUTHORIZED"));
    }

    @Test
    @DisplayName("POST /proyectos-directos sin documento formal: 422 FORMAL_DOCUMENT_REQUIRED")
    void crearDirecto_sinDocumentoFormal_422() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "FORMAL_DOCUMENT_REQUIRED: el directo exige documento de aprobacion"))
                .when(directoService).crear(any(DirectProjectRequest.class),
                        any(PortafolioAuthContext.class), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos-directos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-104")
                        .header("X-Asignacion-Efectiva-Id", "500")
                        .content(objectMapper.writeValueAsString(buildDirectRequest())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("FORMAL_DOCUMENT_REQUIRED"));
    }

    @Test
    @DisplayName("POST /proyectos-directos con perfil Responsable: 403 ASSIGNMENT_SCOPE_DENIED")
    void crearDirecto_responsable_403() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN,
                "ASSIGNMENT_SCOPE_DENIED: el directo solo lo crea Autoridad o Evaluador"))
                .when(directoService).crear(any(DirectProjectRequest.class),
                        any(PortafolioAuthContext.class), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos-directos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-105")
                        .header("X-Asignacion-Efectiva-Id", "500")
                        .content(objectMapper.writeValueAsString(buildDirectRequest())))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ASSIGNMENT_SCOPE_DENIED"));
    }

    // ------------------------------------------------------------------
    // Forma del controlador
    // ------------------------------------------------------------------

    @Test
    @DisplayName("El controlador es delgado: no accede a repositorios directamente")
    void controladorEsDelgadoYNoAccedeARepositorios() {
        for (var c : ProyectoController.class.getDeclaredFields()) {
            String tipo = c.getType().getName();
            assertFalse(tipo.contains(".repository."),
                    () -> "El controlador no debe inyectar repositorios: " + tipo);
        }
        assertNotNull(ProyectoController.class.getDeclaredConstructors()[0]);
    }

    @Test
    @DisplayName("El DTO ProjectDetail no expone entidades JPA en su API publica")
    void projectDetailNoExponeEntidadesJPA() {
        for (var c : ProjectDetail.class.getRecordComponents()) {
            Class<?> tipo = c.getType();
            assertFalse((tipo.getName().contains("pe.gob.midagri.piip.portafolio.entity") && !tipo.isEnum())
                            || tipo.getName().contains("pe.gob.midagri.piip.portafolio.repository"),
                    () -> "El campo " + c.getName() + " no debe ser una entidad JPA");
        }
    }

    @Test
    @DisplayName("El DTO CreateDerivedProjectRequest no expone campos autogenerados")
    void createDerivedProjectRequestNoExponeCamposAutogenerados() {
        var componentes = CreateDerivedProjectRequest.class.getRecordComponents();
        java.util.Set<String> nombres = new java.util.HashSet<>();
        for (var c : componentes) {
            nombres.add(c.getName());
        }
        assertFalse(nombres.contains("id"));
        assertFalse(nombres.contains("codigo"));
        assertFalse(nombres.contains("estado"));
        assertFalse(nombres.contains("fechaCreacion"));
        assertFalse(nombres.contains("relacionId"));
    }

    @Test
    @DisplayName("El servicio se inyecta por constructor y el controlador delega sin reglas de negocio")
    void controladorDelegaAlServicio() throws Exception {
        when(derivadoService.crear(anyLong(), any(CreateDerivedProjectRequest.class),
                any(PortafolioAuthContext.class), anyString(), anyString()))
                .thenReturn(buildDetailDerivado());

        mockMvc.perform(post("/api/v1/portafolio/iniciativas/1001/proyecto-derivado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-delega")
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(objectMapper.writeValueAsString(buildDerivedRequest())))
                .andExpect(status().isCreated());

        verify(derivadoService).crear(anyLong(), any(CreateDerivedProjectRequest.class),
                any(PortafolioAuthContext.class), anyString(), anyString());
    }

    @Test
    @DisplayName("El ProblemDetail incluye correlationId y code canonico del modulo portafolio")
    void problemDetailIncluyeCorrelationId() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "DERIVATION_ALREADY_EXISTS: la iniciativa ya tiene un derivado"))
                .when(derivadoService).crear(anyLong(), any(CreateDerivedProjectRequest.class),
                        any(PortafolioAuthContext.class), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/iniciativas/1001/proyecto-derivado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-corr")
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .header("X-Correlation-Id", "corr-especial")
                        .content(objectMapper.writeValueAsString(buildDerivedRequest())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DERIVATION_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @DisplayName("La respuesta del servicio se serializa con DTO HTTP y ETag canonico")
    void respuestaEsDtoHttpConETag() throws Exception {
        when(derivadoService.crear(anyLong(), any(CreateDerivedProjectRequest.class),
                any(PortafolioAuthContext.class), anyString(), anyString()))
                .thenReturn(buildDetailDerivado());

        mockMvc.perform(post("/api/v1/portafolio/iniciativas/1001/proyecto-derivado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-etag")
                        .header("X-Asignacion-Efectiva-Id", "100")
                        .content(objectMapper.writeValueAsString(buildDerivedRequest())))
                .andExpect(header().string("ETag", "\"2001-0\""));

        assertTrue(true, "La respuesta trae ETag derivado de la version optimista del proyecto");
    }
}
