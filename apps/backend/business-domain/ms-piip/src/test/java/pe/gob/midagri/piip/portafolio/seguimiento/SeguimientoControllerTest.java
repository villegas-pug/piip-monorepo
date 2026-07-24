package pe.gob.midagri.piip.portafolio.seguimiento;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.exception.PortafolioExceptionHandler;
import pe.gob.midagri.piip.portafolio.seguimiento.controller.SeguimientoController;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AltaPersonaRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.AltaUnidadRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.BajaParticipanteRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CancelacionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.CorreccionCicloRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.EditarCamposEditablesRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PlanificacionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PlanificacionResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.ParticipanteResponse;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.PresentacionProductoFinalRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.dto.SuspensionRequest;
import pe.gob.midagri.piip.portafolio.seguimiento.service.CicloService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.ParticipanteProyectoService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.PresentacionProductoFinalService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.SeguimientoProyectoService;
import pe.gob.midagri.piip.portafolio.seguimiento.service.TransicionProyectoService;

/**
 * Pruebas MockMvc del controlador de seguimiento del proyecto
 * conforme al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md},
 * a la Constitucion 5.0.0 y al DDL
 * {@code 015_ciclos_resultados_cierre.sql}.
 *
 * <p>Cubre los endpoints REST de planificacion, ciclos,
 * participantes, campos editables 17/19/23, presentacion del
 * producto final, suspension y cancelacion. Verifica:
 * <ul>
 *   <li>{@code Idempotency-Key} obligatorio en operaciones POST;
 *       ausente produce 400.</li>
 *   <li>{@code If-Match} obligatorio en transiciones; ausente
 *       produce 428 y ETag incorrecto produce 412.</li>
 *   <li>Errores 4xx con {@code application/problem+json} y
 *       codigo canonico del portafolio.</li>
 *   <li>El controlador es delgado: solo delega en servicios y
 *       nunca accede a repositorios.</li>
 *   <li>Los DTOs son HTTP, no exponen entidades JPA.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("US4 - Seguimiento: MockMvc de planificacion, ciclos, participantes, presentacion, suspension y cancelacion")
class SeguimientoControllerTest {

    @Mock private SeguimientoProyectoService seguimientoService;
    @Mock private CicloService cicloService;
    @Mock private ParticipanteProyectoService participanteService;
    @Mock private PresentacionProductoFinalService presentacionService;
    @Mock private TransicionProyectoService transicionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        SeguimientoController controller = new SeguimientoController(
                seguimientoService, cicloService, participanteService, presentacionService,
                transicionService, objectMapper);
        PortafolioExceptionHandler advice = new PortafolioExceptionHandler(
                new ProblemDetailsConfig.ProblemDetailsFactory());
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(advice)
                .build();
    }

    // ------------------------------------------------------------------
    // 1) Planificacion: POST /proyectos/{id}/planificaciones
    // ------------------------------------------------------------------
    @Test
    @DisplayName("POST /proyectos/{id}/planificaciones devuelve 201 con ETag")
    void planificar_exitoso_devuelve201ConETag() throws Exception {
        when(seguimientoService.registrarPlanificacion(anyLong(), any(), any(), anyString(), anyString()))
                .thenReturn(new PlanificacionResponse(
                        9001L, 100L, "Alcance Q1-Q2", null, null, null,
                        1, 0L, "N", "\"9001-0\""));

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/planificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-001")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Actor-Sub", "sub-resp")
                        .header("X-Actor-Usuario-Id", "20")
                        .header("X-Correlation-Id", "corr-001")
                        .content(objectMapper.writeValueAsString(new PlanificacionRequest(
                                "Alcance Q1-Q2", "Objetivos", "Entregables",
                                "2026-Q1-S1;2026-Q1-S2"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.alcance").value("Alcance Q1-Q2"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("GET de seguimiento delega las lecturas reales a servicios con autorizacion efectiva")
    void listarSeguimiento_delegaListados() throws Exception {
        when(seguimientoService.listarPlanificaciones(anyLong(), any()))
                .thenReturn(List.of(new PlanificacionResponse(9001L, 100L, "Alcance", "Objetivos",
                        "Entregables", "Periodos", 1, 0L, "N", "\"9001-1\"")));
        when(seguimientoService.listarCiclos(anyLong(), any()))
                .thenReturn(List.of(new CicloResponse(7001L, 100L, "2026-Q1-S1", 1, 0L,
                        "Objetivos", "Actividades", 25, null, null, "N", null, "\"7001-1\"")));
        when(seguimientoService.listarVersionesCiclo(anyLong(), anyLong(), any()))
                .thenReturn(List.of(new CicloResponse(7001L, 100L, "2026-Q1-S1", 1, 0L,
                        "Objetivos", "Actividades", 25, null, null, "N", null, "\"7001-1\"")));
        when(participanteService.listarHistorico(anyLong(), any(), any(), any()))
                .thenReturn(List.of(new ParticipanteResponse(4001L, 100L, 3001L, null,
                        "Participante", "Persona", "MIDAGRI", "Funcion", "BAJA",
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), "\"4001-1\"")));

        String base = "/api/v1/portafolio/proyectos/100";
        mockMvc.perform(get(base + "/planificaciones").header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Actor-Sub", "sub-resp").header("X-Actor-Usuario-Id", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].idPlanificacion").value(9001));
        mockMvc.perform(get(base + "/ciclos").header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Actor-Sub", "sub-resp").header("X-Actor-Usuario-Id", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].idCiclo").value(7001));
        mockMvc.perform(get(base + "/ciclos/7001/versiones").header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Actor-Sub", "sub-resp").header("X-Actor-Usuario-Id", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].numeroVersion").value(1));
        mockMvc.perform(get(base + "/participantes").header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Actor-Sub", "sub-resp").header("X-Actor-Usuario-Id", "20"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].estado").value("BAJA"));
    }

    @Test
    @DisplayName("POST planificaciones sin X-Asignacion-Efectiva-Id se rechaza con 400")
    void planificar_sinAsignacionEfectiva_400() throws Exception {
        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/planificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-002")
                        .content(objectMapper.writeValueAsString(new PlanificacionRequest(
                                "Alcance", "Objetivos", "Entregables", "2026-Q1-S1"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST planificaciones sin Idempotency-Key se rechaza con 400")
    void planificar_sinIdempotencyKey_400() throws Exception {
        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/planificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .content(objectMapper.writeValueAsString(new PlanificacionRequest(
                                "Alcance", "Objetivos", "Entregables", "2026-Q1-S1"))))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // 2) Ciclo: POST /proyectos/{id}/ciclos
    // ------------------------------------------------------------------
    @Test
    @DisplayName("POST /proyectos/{id}/ciclos con periodo valido devuelve 201 con ETag")
    void registrarCiclo_periodoValido_devuelve201ConETag() throws Exception {
        when(seguimientoService.registrarCiclo(anyLong(), any(), any(), anyString(), anyString()))
                .thenReturn(new pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloResponse(
                        9002L, 100L, "2026-Q1-S1", 1, 0L, null, null, null, null, null, "N",
                        null, "\"9002-0\""));

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/ciclos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-010")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Actor-Sub", "sub-resp")
                        .header("X-Actor-Usuario-Id", "20")
                        .content(objectMapper.writeValueAsString(new CicloRequest(
                                "2026-Q1-S1", "Objetivos", "Actividades", 35,
                                "Sin dificultades", "Avanzar entregable"))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("ETag"))
                .andExpect(jsonPath("$.periodo").value("2026-Q1-S1"))
                .andExpect(jsonPath("$.numeroVersion").value(1));
    }

    @Test
    @DisplayName("POST ciclos con periodo invalido devuelve 422 INVALID_PERIOD_FORMAT")
    void registrarCiclo_periodoInvalido_422() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_PERIOD_FORMAT: el periodo debe tener formato AAAA-Qn-Sn"))
                .when(seguimientoService).registrarCiclo(anyLong(), any(), any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/ciclos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-011")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .content(objectMapper.writeValueAsString(new CicloRequest(
                                "2026-Q1-S1", "Objetivos validos", "Actividades validas", 50,
                                "Dif", "Prox"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD_FORMAT"));
    }

    @Test
    @DisplayName("POST ciclos con campos vacios devuelve 422 CYCLE_INCOMPLETE")
    void registrarCiclo_incompleto_422() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "CYCLE_INCOMPLETE: el ciclo debe incluir objetivos, actividades y avance"))
                .when(seguimientoService).registrarCiclo(anyLong(), any(), any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/ciclos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-012")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .content(objectMapper.writeValueAsString(new CicloRequest(
                                "2026-Q1-S1", "Objetivos validos", "Actividades validas", 50,
                                null, null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("CYCLE_INCOMPLETE"));
    }

    // ------------------------------------------------------------------
    // 3) Correccion de ciclo: POST /proyectos/{id}/ciclos/{cicloId}/versiones
    // ------------------------------------------------------------------
    @Test
    @DisplayName("POST /proyectos/{id}/ciclos/{cicloId}/versiones crea una nueva version")
    void corregirCiclo_exitoso_devuelve201() throws Exception {
        when(seguimientoService.corregirCiclo(anyLong(), anyLong(), any(), any(), anyString(), anyString()))
                .thenReturn(new pe.gob.midagri.piip.portafolio.seguimiento.dto.CicloResponse(
                        9100L, 100L, "2026-Q1-S1", 2, 200L, null, null, null, null, null, "N",
                        null, "\"9100-0\""));

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/ciclos/200/versiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-020")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Actor-Sub", "sub-resp")
                        .header("X-Actor-Usuario-Id", "20")
                        .content(objectMapper.writeValueAsString(new CorreccionCicloRequest(
                                "Motivo correccion", "Obj v2", "Act v2", 50, "Dif v2", "Prox v2"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroVersion").value(2));
    }

    // ------------------------------------------------------------------
    // 4) Evidencia de ciclo: POST /proyectos/{id}/ciclos/{cicloId}/documentos
    // ------------------------------------------------------------------
    @Test
    @DisplayName("POST /proyectos/{id}/ciclos/{cicloId}/documentos con tipo invalido devuelve 422")
    void adjuntarEvidenciaCiclo_tipoInvalido_422() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "EVIDENCE_TYPE_NOT_ALLOWED: el tipo documental no es de ciclo"))
                .when(cicloService).adjuntarEvidenciaDocumento(anyLong(), anyLong(), anyLong(),
                        anyString(), any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/ciclos/200/documentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-030")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .content(objectMapper.writeValueAsString(
                                new pe.gob.midagri.piip.portafolio.seguimiento.dto
                                        .AdjuntarEvidenciaCicloRequest(500L, "TipoInventado"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("EVIDENCE_TYPE_NOT_ALLOWED"));
    }

    // ------------------------------------------------------------------
    // 5) Participantes: alta persona, alta unidad, baja
    // ------------------------------------------------------------------
    @Test
    @DisplayName("POST /proyectos/{id}/participantes/personas devuelve 201 con ETag")
    void altaPersona_exitoso_devuelve201() throws Exception {
        when(participanteService.altaPersona(anyLong(), any(), any(), anyString(), anyString()))
                .thenReturn(new pe.gob.midagri.piip.portafolio.seguimiento.dto.ParticipanteResponse(
                        9003L, 100L, 501L, null, "Responsable", "Maria", "MIDAGRI", "Subdirector",
                        "VIGENTE", null, null, "\"9003-0\""));

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/participantes/personas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-040")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Actor-Sub", "sub-resp")
                        .header("X-Actor-Usuario-Id", "20")
                        .content(objectMapper.writeValueAsString(new AltaPersonaRequest(
                                501L, "Responsable", "Maria", "MIDAGRI", "Subdirector"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rol").value("Responsable"));
    }

    @Test
    @DisplayName("POST alta persona con segundo Responsable devuelve 409 RESPONSIBLE_CARDINALITY")
    void altaPersona_segundoResponsable_409() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "RESPONSIBLE_CARDINALITY: ya existe un Responsable titular"))
                .when(participanteService).altaPersona(anyLong(), any(), any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/participantes/personas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-041")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .content(objectMapper.writeValueAsString(new AltaPersonaRequest(
                                600L, "Responsable", "Otro", "MIDAGRI", "Cargo"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RESPONSIBLE_CARDINALITY"));
    }

    @Test
    @DisplayName("POST /proyectos/{id}/participantes/unidades devuelve 201")
    void altaUnidad_exitoso_devuelve201() throws Exception {
        when(participanteService.altaUnidad(anyLong(), any(), any(), anyString(), anyString()))
                .thenReturn(new pe.gob.midagri.piip.portafolio.seguimiento.dto.ParticipanteResponse(
                        9003L, 100L, null, 800L, "Participante", null, null, null,
                        "VIGENTE", null, null, "\"9003-0\""));

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/participantes/unidades")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-042")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Actor-Sub", "sub-resp")
                        .header("X-Actor-Usuario-Id", "20")
                        .content(objectMapper.writeValueAsString(new AltaUnidadRequest(800L))))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST /proyectos/{id}/participaciones/{participacionId}/bajas devuelve 204")
    void bajaParticipante_exitoso_devuelve204() throws Exception {
        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/participaciones/900/bajas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-043")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Actor-Sub", "sub-resp")
                        .header("X-Actor-Usuario-Id", "20")
                        .content(objectMapper.writeValueAsString(new BajaParticipanteRequest(
                                LocalDate.now(), "Fin de participacion"))))
                .andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------
    // 6) PATCH /proyectos/{id}: campos 17, 19, 23
    // ------------------------------------------------------------------
    @Test
    @DisplayName("PATCH /proyectos/{id} edita campos 17/19/23 con If-Match y devuelve 200")
    void patchProyecto_camposEditables_devuelve200() throws Exception {
        when(seguimientoService.editarCamposEditables(anyLong(), any(), any(), anyString(), anyString()))
                .thenReturn(new PlanificacionResponse(
                        100L, 100L, "x", null, null, null,
                        5, 0L, "N", "\"100-5\""));

        mockMvc.perform(patch("/api/v1/portafolio/proyectos/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-050")
                        .header("If-Match", "\"100-4\"")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Perfil-Efectivo", "Responsable")
                        .header("X-Actor-Sub", "sub-resp")
                        .header("X-Actor-Usuario-Id", "20")
                        .content(objectMapper.writeValueAsString(new EditarCamposEditablesRequest(
                                "Doc v2", "Resultados v2", "Nota v2"))))
                .andExpect(status().isOk())
                .andExpect(header().exists("ETag"));
    }

    @Test
    @DisplayName("PATCH /proyectos/{id} sin If-Match devuelve 428 IF_MATCH_REQUIRED")
    void patchProyecto_sinIfMatch_428() throws Exception {
        mockMvc.perform(patch("/api/v1/portafolio/proyectos/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-051")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Perfil-Efectivo", "Responsable")
                        .content(objectMapper.writeValueAsString(new EditarCamposEditablesRequest(
                                "Doc", "Resultados", "Nota"))))
                .andExpect(status().isPreconditionRequired())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("IF_MATCH_REQUIRED"));
    }

    @Test
    @DisplayName("PATCH /proyectos/{id} con campo no editable devuelve 422 FIELD_NOT_EDITABLE")
    void patchProyecto_campoNoEditable_422() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "FIELD_NOT_EDITABLE: el campo no es editable en esta etapa"))
                .when(seguimientoService).editarCamposEditables(anyLong(), any(), any(), anyString(), anyString());

        mockMvc.perform(patch("/api/v1/portafolio/proyectos/100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-052")
                        .header("If-Match", "\"100-4\"")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Perfil-Efectivo", "Responsable")
                        .content("{\"documentacionGestion\":\"cambiar nombre\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("FIELD_NOT_EDITABLE"));
    }

    // ------------------------------------------------------------------
    // 7) Presentacion del producto final
    // ------------------------------------------------------------------
    @Test
    @DisplayName("POST /proyectos/{id}/producto-final/presentaciones devuelve 201 sin cambiar estado")
    void presentarProductoFinal_exitoso_devuelve201() throws Exception {
        when(presentacionService.presentar(anyLong(), any(), any(), anyString(), anyString()))
                .thenReturn(new pe.gob.midagri.piip.portafolio.seguimiento.dto
                        .PresentacionProductoFinalResponse(
                        9004L, 100L, 1, 0L,
                        "PROTOTIPO_CONCEPTUALIZADO", "Doc", "Resultados", "Nota",
                        900L, java.util.List.of(901L), "\"9004-0\""));

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/producto-final/presentaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-060")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .header("X-Actor-Sub", "sub-resp")
                        .header("X-Actor-Usuario-Id", "20")
                        .content(objectMapper.writeValueAsString(new PresentacionProductoFinalRequest(
                                 "PROTOTIPO_CONCEPTUALIZADO", "Doc", "Resultados", "Nota", 900L,
                                 java.util.List.of(901L)))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipoProductoFinal").value("PROTOTIPO_CONCEPTUALIZADO"));
    }

    @Test
    @DisplayName("POST presentacion con tipo fuera de catalogo devuelve 422 PRODUCT_FINAL_TYPE_REQUIRED")
    void presentarProductoFinal_tipoInvalido_422() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "PRODUCT_FINAL_TYPE_REQUIRED: tipo de producto final fuera del catalogo canonico"))
                .when(presentacionService).presentar(anyLong(), any(), any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/producto-final/presentaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-061")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .content(objectMapper.writeValueAsString(new PresentacionProductoFinalRequest(
                                 "SOLUCION_FUNCIONAL", "Doc", "Resultados", "Nota", 900L,
                                 java.util.List.of(901L)))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("PRODUCT_FINAL_TYPE_REQUIRED"));
    }

    // ------------------------------------------------------------------
    // 8) Suspension: POST /proyectos/{id}/suspensiones
    // ------------------------------------------------------------------
    @Test
    @DisplayName("POST /proyectos/{id}/suspensiones con If-Match devuelve 200 con estado SUSPENDIDO")
    void suspender_exitoso_devuelve200() throws Exception {
        when(transicionService.suspender(anyLong(), any(), any(), anyString(), anyString()))
                .thenReturn(new pe.gob.midagri.piip.portafolio.seguimiento.dto.TransicionResponse(
                        9005L, 100L, EstadoIniciativa.PROYECTO_EJECUCION,
                        EstadoIniciativa.SUSPENDIDO, 0L, null, "Obs", 500L, "\"9005-0\""));

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/suspensiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-070")
                        .header("If-Match", "\"100-4\"")
                        .header("X-Asignacion-Efectiva-Id", "800")
                        .header("X-Perfil-Efectivo", "UnidadAdmin")
                        .header("X-Actor-Sub", "sub-ua")
                        .header("X-Actor-Usuario-Id", "80")
                        .content(objectMapper.writeValueAsString(new SuspensionRequest(
                                500L, "Riesgo operativo sustentado", "1-0"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoNuevo").value("SUSPENDIDO"));
    }

    @Test
    @DisplayName("POST suspension sin If-Match devuelve 428")
    void suspender_sinIfMatch_428() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
                "IF_MATCH_REQUIRED: la transicion exige If-Match"))
                .when(transicionService).suspender(anyLong(), any(), any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/suspensiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-071")
                        .header("X-Asignacion-Efectiva-Id", "800")
                        .header("X-Perfil-Efectivo", "UnidadAdmin")
                        .content(objectMapper.writeValueAsString(new SuspensionRequest(
                                500L, "Riesgo operativo", "1-0"))))
                .andExpect(status().isPreconditionRequired())
                .andExpect(jsonPath("$.code").value("IF_MATCH_REQUIRED"));
    }

    @Test
    @DisplayName("POST suspension desde PRESENTADO devuelve 409 STATE_TRANSITION_NOT_ALLOWED")
    void suspender_desdePresentado_409() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "STATE_TRANSITION_NOT_ALLOWED: PRESENTADO -> SUSPENDIDO no es canonica"))
                .when(transicionService).suspender(anyLong(), any(), any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/suspensiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-072")
                        .header("If-Match", "\"100-4\"")
                        .header("X-Asignacion-Efectiva-Id", "800")
                        .header("X-Perfil-Efectivo", "UnidadAdmin")
                        .content(objectMapper.writeValueAsString(new SuspensionRequest(
                                500L, "Riesgo operativo", "1-0"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_TRANSITION_NOT_ALLOWED"));
    }

    // ------------------------------------------------------------------
    // 9) Cancelacion: POST /proyectos/{id}/cancelaciones
    // ------------------------------------------------------------------
    @Test
    @DisplayName("POST /proyectos/{id}/cancelaciones con If-Match devuelve 200 con estado CANCELADO")
    void cancelar_exitoso_devuelve200() throws Exception {
        when(transicionService.cancelar(anyLong(), any(), any(), anyString(), anyString()))
                .thenReturn(new pe.gob.midagri.piip.portafolio.seguimiento.dto.TransicionResponse(
                        9005L, 100L, EstadoIniciativa.PROYECTO_EJECUCION,
                        EstadoIniciativa.CANCELADO, 0L, null, "Obs", 600L, "\"9005-0\""));

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/cancelaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-080")
                        .header("If-Match", "\"100-4\"")
                        .header("X-Asignacion-Efectiva-Id", "500")
                        .header("X-Perfil-Efectivo", "Autoridad")
                        .header("X-Actor-Sub", "sub-aut")
                        .header("X-Actor-Usuario-Id", "50")
                        .content(objectMapper.writeValueAsString(new CancelacionRequest(
                                600L, "Cancelacion decidida por la Autoridad", "1-0"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoNuevo").value("CANCELADO"));
    }

    @Test
    @DisplayName("POST cancelacion desde PRESENTADO devuelve 409 (transicion implicita prohibida)")
    void cancelar_desdePresentado_409() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "STATE_TRANSITION_NOT_ALLOWED: PRESENTADO -> CANCELADO no es canonica"))
                .when(transicionService).cancelar(anyLong(), any(), any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/cancelaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-081")
                        .header("If-Match", "\"100-4\"")
                        .header("X-Asignacion-Efectiva-Id", "500")
                        .header("X-Perfil-Efectivo", "Autoridad")
                        .content(objectMapper.writeValueAsString(new CancelacionRequest(
                                600L, "Cancelar iniciativa en presentacion", "1-0"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_TRANSITION_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("POST cancelacion sin documento formal devuelve 422 EVIDENCE_NOT_ELIGIBLE")
    void cancelar_sinDocumento_422() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "EVIDENCE_NOT_ELIGIBLE: la cancelacion exige documento formal"))
                .when(transicionService).cancelar(anyLong(), any(), any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/cancelaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-082")
                        .header("If-Match", "\"100-4\"")
                        .header("X-Asignacion-Efectiva-Id", "500")
                        .header("X-Perfil-Efectivo", "Autoridad")
                        .content(objectMapper.writeValueAsString(new CancelacionRequest(
                                600L, "Cancelar", null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("EVIDENCE_NOT_ELIGIBLE"));
    }

    // ------------------------------------------------------------------
    // 10) Forma del controlador (limites arquitectonicos)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("El controlador es delgado: no accede a repositorios directamente")
    void controladorEsDelgadoYNoAccedeARepositorios() {
        for (var c : SeguimientoController.class.getDeclaredFields()) {
            String tipo = c.getType().getName();
            assertFalse(tipo.contains(".repository."),
                    () -> "El controlador no debe inyectar repositorios: " + tipo);
        }
        assertNotNull(SeguimientoController.class.getDeclaredConstructors()[0]);
    }

    @Test
    @DisplayName("Los DTOs de seguimiento no exponen entidades JPA en su API publica")
    void dtosSeguimientoNoExponenEntidadesJPA() {
        Class<?>[] dtos = {
                PlanificacionRequest.class,
                CicloRequest.class,
                CorreccionCicloRequest.class,
                pe.gob.midagri.piip.portafolio.seguimiento.dto.AdjuntarEvidenciaCicloRequest.class,
                AltaPersonaRequest.class,
                AltaUnidadRequest.class,
                BajaParticipanteRequest.class,
                EditarCamposEditablesRequest.class,
                PresentacionProductoFinalRequest.class,
                SuspensionRequest.class,
                CancelacionRequest.class
        };
        for (Class<?> dto : dtos) {
            for (var c : dto.getRecordComponents()) {
                String tipo = c.getType().getName();
                assertFalse(tipo.contains("pe.gob.midagri.piip.portafolio.entity")
                                || tipo.contains("pe.gob.midagri.piip.portafolio.repository"),
                        () -> "El campo " + c.getName() + " en " + dto.getSimpleName()
                                + " no debe ser una entidad JPA");
            }
        }
    }

    @Test
    @DisplayName("El ProblemDetail incluye correlationId y code canonico del modulo portafolio")
    void problemDetailIncluyeCorrelationId() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,
                "STATE_TRANSITION_NOT_ALLOWED: PRESENTADO -> SUSPENDIDO no es canonica"))
                .when(transicionService).suspender(anyLong(), any(), any(), anyString(), anyString());

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/suspensiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-corr")
                        .header("If-Match", "\"100-4\"")
                        .header("X-Asignacion-Efectiva-Id", "800")
                        .header("X-Perfil-Efectivo", "UnidadAdmin")
                        .header("X-Correlation-Id", "corr-especial")
                        .content(objectMapper.writeValueAsString(new SuspensionRequest(
                                500L, "Riesgo operativo", "1-0"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STATE_TRANSITION_NOT_ALLOWED"))
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    @DisplayName("El controlador delega toda la logica al servicio (verifica la invocacion)")
    void controladorDelegaAlServicio() throws Exception {
        when(seguimientoService.registrarPlanificacion(anyLong(), any(), any(), anyString(), anyString()))
                .thenReturn(new PlanificacionResponse(
                        9001L, 100L, "Alcance", null, null, null,
                        1, 0L, "N", "\"9001-0\""));

        mockMvc.perform(post("/api/v1/portafolio/proyectos/100/planificaciones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "key-delega")
                        .header("X-Asignacion-Efectiva-Id", "200")
                        .content(objectMapper.writeValueAsString(new PlanificacionRequest(
                                "Alcance", "Objetivos", "Entregables", "2026-Q1-S1"))))
                .andExpect(status().isCreated());

        verify(seguimientoService).registrarPlanificacion(anyLong(), any(),
                any(PortafolioAuthContext.class), anyString(), anyString());
    }
}
