package pe.gob.midagri.piip.portafolio.proyecto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.dto.DirectProjectRequest;
import pe.gob.midagri.piip.portafolio.dto.ProjectDetail;
import pe.gob.midagri.piip.portafolio.dto.TipoOrigenDirecto;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.service.CatalogoUnidadReader;
import pe.gob.midagri.piip.portafolio.service.CodigoProyectoService;
import pe.gob.midagri.piip.portafolio.service.CrearProyectoDirectoService;
import pe.gob.midagri.piip.portafolio.service.impl.CrearProyectoDirectoServiceImpl;

/**
 * Pruebas unitarias para la creacion del proyecto directo conforme a la
 * Constitucion 5.0.0 y al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}.
 *
 * <p>Reglas cubiertas:
 * <ul>
 *   <li>El proyecto directo exige documento formal de aprobacion o
 *       autorizacion de inicio (campo 15), fecha de inicio, unidad
 *       responsable, responsable, estado actual y al menos una
 *       evidencia.</li>
 *   <li>Un segundo directo para la misma unidad y anio, cuando ya hay
 *       uno activo, se rechaza con 409.</li>
 *   <li>La ruta del proyecto directo NO omite la evaluacion de
 *       iniciativas nuevas: solo aplica a proyectos heredados o a
 *       excepciones formalmente autorizadas.</li>
 *   <li>Solo Autoridad o Evaluador con documento formal pueden crear
 *       un proyecto directo; el Responsable queda excluido.</li>
 *   <li>Tipos: {@code HEREDADO} (acredita inicio previo a PIIP, acto
 *       formal y ejecucion) y {@code EXCEPCION_FORMAL} (aprobada por
 *       Autoridad).</li>
 *   <li>Auditoria atomica de exito y denegacion.</li>
 * </ul>
 *
 * <p>Esta prueba modela la firma esperada que T066 implementara; las
 * firmas exactas se marcan con {@code // @NEEDS_CLARIFICATION} cuando
 * la especificacion pueda ajustar nombres o parametros.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("US3 - Crear proyecto directo: documento, fecha, unidad, responsable, estado, evidencia, 409 segundo activo")
class CrearProyectoDirectoServiceTest {

    private static final String PREFIJO_UNIDAD = "MIDAGRI";
    private static final String IDEMPOTENCY_KEY = "key-directo-1";
    private static final String PAYLOAD_JSON = "{}";
    private static final long UNIDAD_ID = 1L;
    private static final int ANIO_ACTUAL = 2026;

    @Mock private RegistroPortafolioRepository registroRepository;
    @Mock private CodigoProyectoService codigoProyectoService;
    @Mock private CatalogoUnidadReader catalogoUnidadReader;
    @Mock private AuditService auditService;
    @Mock private IdempotencyService idempotencyService;

    private CrearProyectoDirectoServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new CrearProyectoDirectoServiceImpl(
                registroRepository, codigoProyectoService, catalogoUnidadReader,
                auditService, idempotencyService);
        service.setObjectMapper(objectMapper);
        when(idempotencyService.execute(any(IdempotencyService.IdempotencyRequest.class),
                any(IdempotencyService.IdempotentOperation.class)))
                .thenAnswer(invocation -> {
                    IdempotencyService.IdempotentOperation operacion = invocation.getArgument(1);
                    IdempotencyService.IdempotencyResponse response = operacion.execute();
                    return new IdempotencyService.IdempotencyResult(
                            response.recursoTipo(), response.recursoId(),
                            response.respuestaJson(), false);
                });
    }

    private PortafolioAuthContext contextoAutoridad() {
        return new PortafolioAuthContext("sub-aut", 50L, 500L, "Autoridad", 1L, 1L, "corr-aut");
    }

    private PortafolioAuthContext contextoEvaluador() {
        return new PortafolioAuthContext("sub-eval", 99L, 999L, "Evaluador", 1L, 1L, "corr-eval");
    }

    private PortafolioAuthContext contextoResponsable() {
        return new PortafolioAuthContext("sub-resp", 10L, 100L, "Responsable", 1L, 1L, "corr-resp");
    }

    private DirectProjectRequest buildRequestHeredado() {
        return new DirectProjectRequest(
                TipoOrigenDirecto.HEREDADO,
                "LEGACY-2024-001",
                LocalDate.of(2024, 3, 15),
                "Proyecto heredado de sistema previo",
                10L,
                20L,
                UNIDAD_ID,
                10L,
                "Descripcion del proyecto heredado",
                Boolean.FALSE,
                null,
                "Nota opcional",
                800L,
                List.of(900L, 901L),
                FuenteOrigen.FICHA_INICIATIVA);
    }

    private DirectProjectRequest buildRequestExcepcion() {
        return new DirectProjectRequest(
                TipoOrigenDirecto.EXCEPCION_FORMAL,
                null,
                LocalDate.of(ANIO_ACTUAL, 1, 10),
                "Proyecto excepcional autorizado por la Autoridad",
                10L,
                20L,
                UNIDAD_ID,
                50L,
                "Descripcion del proyecto excepcional",
                Boolean.FALSE,
                null,
                null,
                850L,
                List.of(950L),
                FuenteOrigen.OTROS);
    }

    private void prepararUnidad() {
        when(catalogoUnidadReader.prefijoUnidad(UNIDAD_ID))
                .thenReturn(Optional.of(PREFIJO_UNIDAD));
        when(codigoProyectoService.generarCodigo(Year.now().getValue(), UNIDAD_ID, PREFIJO_UNIDAD))
                .thenReturn("2026-MIDAGRI-00010");
    }

    // ------------------------------------------------------------------
    // 1) Campos obligatorios: documento, fecha, unidad, responsable, evidencia
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Crear directo exige documento formal (campo 15)")
    void crear_sinDocumentoFormal_seRechaza() {
        DirectProjectRequest sinDocumento = new DirectProjectRequest(
                TipoOrigenDirecto.HEREDADO,
                "LEGACY-2024-001",
                LocalDate.of(2024, 3, 15),
                "Nombre valido",
                10L, 20L, UNIDAD_ID, 10L,
                "Descripcion valida", Boolean.FALSE, null, null,
                null,
                List.of(900L),
                FuenteOrigen.FICHA_INICIATIVA);
        prepararUnidad();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(sinDocumento, contextoAutoridad(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("FORMAL_DOCUMENT_REQUIRED")
                    || error.getReason().contains("EVIDENCE_NOT_ELIGIBLE")));
        verify(registroRepository, never()).save(any(RegistroPortafolioEntity.class));
    }

    @Test
    @DisplayName("Crear directo exige fecha de inicio (campo 4)")
    void crear_sinFechaInicio_seRechaza() {
        DirectProjectRequest sinFecha = new DirectProjectRequest(
                TipoOrigenDirecto.HEREDADO,
                "LEGACY-2024-001",
                null,
                "Nombre valido",
                10L, 20L, UNIDAD_ID, 10L,
                "Descripcion valida", Boolean.FALSE, null, null,
                800L, List.of(900L), FuenteOrigen.FICHA_INICIATIVA);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(sinFecha, contextoAutoridad(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertTrue(error.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY
                || error.getStatusCode() == HttpStatus.BAD_REQUEST);
        assertTrue(error.getReason() != null
                && (error.getReason().contains("OFFICIAL_FIELD_REQUIRED")
                    || error.getReason().contains("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("Crear directo exige unidad responsable (campo 12)")
    void crear_sinUnidadResponsable_seRechaza() {
        DirectProjectRequest sinUnidad = new DirectProjectRequest(
                TipoOrigenDirecto.HEREDADO,
                "LEGACY-2024-001",
                LocalDate.of(2024, 3, 15),
                "Nombre valido",
                10L, 20L, null, 10L,
                "Descripcion valida", Boolean.FALSE, null, null,
                800L, List.of(900L), FuenteOrigen.FICHA_INICIATIVA);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(sinUnidad, contextoAutoridad(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertTrue(error.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY
                || error.getStatusCode() == HttpStatus.BAD_REQUEST);
        assertTrue(error.getReason() != null
                && (error.getReason().contains("OFFICIAL_FIELD_REQUIRED")
                    || error.getReason().contains("UNIT_MAIN_CARDINALITY")));
    }

    @Test
    @DisplayName("Crear directo exige responsable (campo 8)")
    void crear_sinResponsable_seRechaza() {
        DirectProjectRequest sinResponsable = new DirectProjectRequest(
                TipoOrigenDirecto.HEREDADO,
                "LEGACY-2024-001",
                LocalDate.of(2024, 3, 15),
                "Nombre valido",
                10L, 20L, UNIDAD_ID, null,
                "Descripcion valida", Boolean.FALSE, null, null,
                800L, List.of(900L), FuenteOrigen.FICHA_INICIATIVA);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(sinResponsable, contextoAutoridad(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("RESPONSIBLE_CARDINALITY")
                    || error.getReason().contains("OFFICIAL_FIELD_REQUIRED")));
    }

    @Test
    @DisplayName("Crear directo exige al menos una evidencia (campo 17)")
    void crear_sinEvidencias_seRechaza() {
        DirectProjectRequest sinEvidencias = new DirectProjectRequest(
                TipoOrigenDirecto.HEREDADO,
                "LEGACY-2024-001",
                LocalDate.of(2024, 3, 15),
                "Nombre valido",
                10L, 20L, UNIDAD_ID, 10L,
                "Descripcion valida", Boolean.FALSE, null, null,
                800L, List.of(), FuenteOrigen.FICHA_INICIATIVA);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(sinEvidencias, contextoAutoridad(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("EVIDENCE_NOT_ELIGIBLE")
                    || error.getReason().contains("FORMAL_DOCUMENT_REQUIRED")
                    || error.getReason().contains("OFFICIAL_FIELD_REQUIRED")));
    }

    // ------------------------------------------------------------------
    // 2) Unicidad: un solo directo activo por unidad y anio
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un segundo directo para la misma unidad y anio, cuando ya hay uno activo, se rechaza con 409")
    void crear_segundoDirectoActivo_409() {
        prepararUnidad();
        when(registroRepository.existsByUnidadEjecutoraIdAndAnioAndEstado(UNIDAD_ID,
                Year.now().getValue(), EstadoIniciativa.PROYECTO_EJECUCION.name()))
                .thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(buildRequestHeredado(), contextoAutoridad(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("DIRECT_PROJECT_NOT_AUTHORIZED")
                    || error.getReason().contains("DIRECT_PROJECT_DUPLICATE")
                    || error.getReason().contains("ACTIVE_PROJECT_EXISTS")),
                "El codigo debe identificar el bloqueo de duplicado");
        verify(registroRepository, never()).save(any(RegistroPortafolioEntity.class));
    }

    // ------------------------------------------------------------------
    // 3) La ruta directa NO omite la evaluacion de iniciativas nuevas
    // ------------------------------------------------------------------

    @Test
    @DisplayName("La ruta directa exige tipoOrigen valido: HEREDADO o EXCEPCION_FORMAL")
    void crear_sinTipoOrigen_seRechaza() {
        DirectProjectRequest sinTipo = new DirectProjectRequest(
                null,
                "LEGACY-2024-001",
                LocalDate.of(2024, 3, 15),
                "Nombre valido",
                10L, 20L, UNIDAD_ID, 10L,
                "Descripcion valida", Boolean.FALSE, null, null,
                800L, List.of(900L), FuenteOrigen.FICHA_INICIATIVA);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(sinTipo, contextoAutoridad(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertTrue(error.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY
                || error.getStatusCode() == HttpStatus.BAD_REQUEST);
        assertTrue(error.getReason() != null
                && (error.getReason().contains("DIRECT_PROJECT_NOT_AUTHORIZED")
                    || error.getReason().contains("OFFICIAL_FIELD_REQUIRED")
                    || error.getReason().contains("VALIDATION_FAILED")));
    }

    @Test
    @DisplayName("El tipoOrigen HEREDADO exige codigoOrigen para acreditar el inicio previo a PIIP")
    void crear_heredado_sinCodigoOrigen_seRechaza() {
        prepararUnidad();
        DirectProjectRequest heredadoSinCodigo = new DirectProjectRequest(
                TipoOrigenDirecto.HEREDADO,
                null,
                LocalDate.of(2024, 3, 15),
                "Nombre valido",
                10L, 20L, UNIDAD_ID, 10L,
                "Descripcion valida", Boolean.FALSE, null, null,
                800L, List.of(900L), FuenteOrigen.FICHA_INICIATIVA);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(heredadoSinCodigo, contextoAutoridad(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("OFFICIAL_FIELD_REQUIRED")
                    || error.getReason().contains("DIRECT_PROJECT_NOT_AUTHORIZED")
                    || error.getReason().contains("CODIGO_ORIGEN_REQUIRED")));
    }

    @Test
    @DisplayName("El tipoOrigen EXCEPCION_FORMAL exige documentoAutorizacionId como acto formal")
    void crear_excepcion_sinDocumentoAutorizacion_seRechaza() {
        prepararUnidad();
        DirectProjectRequest excepcionSinDoc = new DirectProjectRequest(
                TipoOrigenDirecto.EXCEPCION_FORMAL,
                null,
                LocalDate.of(ANIO_ACTUAL, 1, 10),
                "Nombre valido",
                10L, 20L, UNIDAD_ID, 50L,
                "Descripcion valida", Boolean.FALSE, null, null,
                null, List.of(950L), FuenteOrigen.OTROS);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(excepcionSinDoc, contextoAutoridad(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("FORMAL_DOCUMENT_REQUIRED")
                    || error.getReason().contains("DIRECT_PROJECT_NOT_AUTHORIZED")
                    || error.getReason().contains("EVIDENCE_NOT_ELIGIBLE")));
    }

    // ------------------------------------------------------------------
    // 4) Autorizacion efectiva
    // ------------------------------------------------------------------

    @Test
    @DisplayName("La Autoridad puede crear un proyecto directo con documento formal")
    void crear_autoridad_permitido() {
        prepararUnidad();
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(9001L);
            e.setVersion(0L);
            return e;
        });
        when(registroRepository.existsByUnidadEjecutoraIdAndAnioAndEstado(UNIDAD_ID,
                Year.now().getValue(), EstadoIniciativa.PROYECTO_EJECUCION.name()))
                .thenReturn(false);

        ProjectDetail detalle = service.crear(buildRequestHeredado(), contextoAutoridad(),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);
        assertNotNull(detalle);
        assertEquals(TipoRegistro.PROYECTO, detalle.tipoRegistro());
        assertEquals(EstadoIniciativa.PROYECTO_EJECUCION, detalle.estado());
        verify(registroRepository, times(1)).save(any(RegistroPortafolioEntity.class));
    }

    @Test
    @DisplayName("El Evaluador con documento formal puede crear un proyecto directo")
    void crear_evaluador_permitido() {
        prepararUnidad();
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(9002L);
            e.setVersion(0L);
            return e;
        });
        when(registroRepository.existsByUnidadEjecutoraIdAndAnioAndEstado(UNIDAD_ID,
                Year.now().getValue(), EstadoIniciativa.PROYECTO_EJECUCION.name()))
                .thenReturn(false);

        ProjectDetail detalle = service.crear(buildRequestHeredado(), contextoEvaluador(),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);
        assertNotNull(detalle);
    }

    @Test
    @DisplayName("El Responsable NO puede usar la ruta de proyecto directo")
    void crear_responsable_seRechaza() {
        prepararUnidad();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(buildRequestHeredado(), contextoResponsable(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertTrue(error.getStatusCode() == HttpStatus.FORBIDDEN
                || error.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY);
        assertTrue(error.getReason() != null
                && (error.getReason().contains("ASSIGNMENT_SCOPE_DENIED")
                    || error.getReason().contains("FORBIDDEN_PROFILE")
                    || error.getReason().contains("DIRECT_PROJECT_NOT_AUTHORIZED")));
        verify(registroRepository, never()).save(any(RegistroPortafolioEntity.class));
    }

    // ------------------------------------------------------------------
    // 5) Creacion exitosa y sus garantias
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Crear directo exitoso genera codigo propio, fija PROYECTO_EJECUCION y registra auditoria")
    void crear_exitoso_generaCodigoYAudita() {
        prepararUnidad();
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(9001L);
            e.setVersion(0L);
            return e;
        });
        when(registroRepository.existsByUnidadEjecutoraIdAndAnioAndEstado(UNIDAD_ID,
                Year.now().getValue(), EstadoIniciativa.PROYECTO_EJECUCION.name()))
                .thenReturn(false);

        ProjectDetail detalle = service.crear(buildRequestExcepcion(), contextoAutoridad(),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals("2026-MIDAGRI-00010", detalle.codigo());
        assertEquals(EstadoIniciativa.PROYECTO_EJECUCION, detalle.estado());
        assertEquals(TipoRegistro.PROYECTO, detalle.tipoRegistro());
        // El proyecto directo no tiene iniciativa origen.
        assertEquals(null, detalle.iniciativaId(),
                "Un proyecto directo no debe tener iniciativa origen");
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));
    }

    @Test
    @DisplayName("El codigo de origen se persiste en PROYECTO.CODIGO_ORIGEN cuando es HEREDADO")
    void crear_heredado_persisteCodigoOrigen() {
        prepararUnidad();
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(9001L);
            e.setVersion(0L);
            return e;
        });
        when(registroRepository.existsByUnidadEjecutoraIdAndAnioAndEstado(UNIDAD_ID,
                Year.now().getValue(), EstadoIniciativa.PROYECTO_EJECUCION.name()))
                .thenReturn(false);

        ProjectDetail detalle = service.crear(buildRequestHeredado(), contextoAutoridad(),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);
        assertEquals("LEGACY-2024-001", detalle.codigoOrigen());
    }

    @Test
    @DisplayName("Idempotency-Key ausente se rechaza con IDEMPOTENCY_KEY_REQUIRED")
    void crear_sinIdempotencyKey_seRechaza() {
        prepararUnidad();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(buildRequestHeredado(), contextoAutoridad(),
                        null, PAYLOAD_JSON));
        assertTrue(error.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY
                || error.getStatusCode() == HttpStatus.BAD_REQUEST);
        assertTrue(error.getReason() != null
                && error.getReason().contains("IDEMPOTENCY_KEY_REQUIRED"));
    }

    // ------------------------------------------------------------------
    // 6) Contrato de interfaz
    // ------------------------------------------------------------------

    @Test
    @DisplayName("El servicio expone contrato de interfaz y no entidades JPA en su API publica")
    void servicioExponeContratoSinEntidadesJPA() {
        assertTrue(CrearProyectoDirectoService.class.isInterface());
        for (var metodo : CrearProyectoDirectoService.class.getDeclaredMethods()) {
            String retorno = metodo.getReturnType().getName();
            assertFalse(retorno.contains("pe.gob.midagri.piip.portafolio.entity")
                            || retorno.contains("pe.gob.midagri.piip.portafolio.repository"),
                    () -> "El metodo " + metodo.getName() + " no debe retornar entidades JPA");
        }
    }

    @Test
    @DisplayName("El DTO DirectProjectRequest no expone campos autogenerados")
    void directProjectRequestNoExponeCamposAutogenerados() {
        var componentes = DirectProjectRequest.class.getRecordComponents();
        java.util.Set<String> nombres = new java.util.HashSet<>();
        for (var c : componentes) {
            nombres.add(c.getName());
        }
        assertFalse(nombres.contains("id"));
        assertFalse(nombres.contains("codigo"));
        assertFalse(nombres.contains("estado"));
        assertFalse(nombres.contains("fechaCreacion"));
        assertFalse(nombres.contains("transicionId"));
    }

    // ------------------------------------------------------------------
    // 7) Auditoria atomica
    // ------------------------------------------------------------------

    @Test
    @DisplayName("La auditoria registra exito y denegacion de manera atomica y trazable")
    void auditoriaExitoYDenegacion() {
        // Caso 1: exito.
        prepararUnidad();
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(9001L);
            e.setVersion(0L);
            return e;
        });
        when(registroRepository.existsByUnidadEjecutoraIdAndAnioAndEstado(UNIDAD_ID,
                Year.now().getValue(), EstadoIniciativa.PROYECTO_EJECUCION.name()))
                .thenReturn(false);
        service.crear(buildRequestHeredado(), contextoAutoridad(), "k-1", PAYLOAD_JSON);
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));

        // Caso 2: denegacion por perfil Responsable.
        ResponseStatusException denegado = assertThrows(ResponseStatusException.class,
                () -> service.crear(buildRequestHeredado(), contextoResponsable(),
                        "k-2", PAYLOAD_JSON));
        assertTrue(denegado.getStatusCode() == HttpStatus.FORBIDDEN
                || denegado.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY);
        verify(auditService, times(1)).registrarDenegacion(any(AuditService.AuditCommand.class));
    }

    // ------------------------------------------------------------------
    // 8) El path del directo no es atajo para saltarse la evaluacion
    // ------------------------------------------------------------------

    @Test
    @DisplayName("El directo exige estado actual explicito: PROYECTO_EJECUCION desde la creacion")
    void crear_estadoInicialEsProyectoEjecucion() {
        prepararUnidad();
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(9001L);
            e.setVersion(0L);
            return e;
        });
        when(registroRepository.existsByUnidadEjecutoraIdAndAnioAndEstado(UNIDAD_ID,
                Year.now().getValue(), EstadoIniciativa.PROYECTO_EJECUCION.name()))
                .thenReturn(false);

        ProjectDetail detalle = service.crear(buildRequestHeredado(), contextoAutoridad(),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);
        assertEquals(EstadoIniciativa.PROYECTO_EJECUCION, detalle.estado(),
                "Un proyecto directo inicia en PROYECTO_EJECUCION sin pasar por INICIATIVA_APROBADA");
    }
}