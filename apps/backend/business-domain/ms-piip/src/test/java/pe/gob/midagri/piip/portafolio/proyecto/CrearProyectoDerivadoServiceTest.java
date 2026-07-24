package pe.gob.midagri.piip.portafolio.proyecto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.RelacionIniciativaProyectoEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.repository.RelacionIniciativaProyectoRepository;
import pe.gob.midagri.piip.portafolio.service.CatalogoUnidadReader;
import pe.gob.midagri.piip.portafolio.service.CodigoProyectoService;
import pe.gob.midagri.piip.portafolio.service.CrearProyectoDerivadoService;
import pe.gob.midagri.piip.portafolio.service.impl.CrearProyectoDerivadoServiceImpl;
import pe.gob.midagri.piip.portafolio.dto.CreateDerivedProjectRequest;
import pe.gob.midagri.piip.portafolio.dto.ProjectDetail;

/**
 * Pruebas unitarias para la creacion del proyecto derivado conforme a la
 * Constitucion 5.0.0, al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}
 * y al script DDL {@code 010_iniciativa_proyecto_relacion.sql}.
 *
 * <p>Reglas cubiertas:
 * <ul>
 *   <li>Solo se permite crear un proyecto derivado cuando la iniciativa
 *       esta en {@code INICIATIVA_APROBADA}.</li>
 *   <li>Un segundo intento de derivado para la misma iniciativa falla
 *       con 409 {@code DERIVATION_ALREADY_EXISTS} (UK por
 *       {@code ID_INICIATIVA}).</li>
 *   <li>Dos solicitudes concurrentes: la primera gana, la segunda
 *       recibe 409.</li>
 *   <li>Autorizacion efectiva: solo el {@code Responsable} dentro de su
 *       ambito puede crear derivado. Evaluador y Autoridad quedan
 *       excluidos.</li>
 *   <li>El documento formal de aprobacion o autorizacion de inicio es
 *       obligatorio (campo 15).</li>
 *   <li>La iniciativa NO cambia de estado: sigue en
 *       {@code INICIATIVA_APROBADA}.</li>
 *   <li>El nuevo proyecto se crea en
 *       {@code PROYECTO_EJECUCION} con vinculo inmutable a la
 *       iniciativa via {@code INICIATIVA_PROYECTO}.</li>
 *   <li>Auditoria atomica de exito y denegacion.</li>
 * </ul>
 *
 * <p>Esta prueba modela la firma esperada que T065 implementara; las
 * firmas exactas se marcan con {@code // @NEEDS_CLARIFICATION} cuando
 * la especificacion pueda ajustar nombres o parametros.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("US3 - Crear proyecto derivado: estado INICIATIVA_APROBADA, segundo derivado 409, autorizacion Responsable")
class CrearProyectoDerivadoServiceTest {

    private static final String PREFIJO_UNIDAD = "MIDAGRI";
    private static final String IDEMPOTENCY_KEY = "key-derivado-1";
    private static final String PAYLOAD_JSON = "{}";
    private static final long INICIATIVA_ID = 1001L;
    private static final long PROYECTO_ID = 2001L;

    @Mock private RegistroPortafolioRepository registroRepository;
    @Mock private RelacionIniciativaProyectoRepository relacionRepository;
    @Mock private CodigoProyectoService codigoProyectoService;
    @Mock private CatalogoUnidadReader catalogoUnidadReader;
    @Mock private AuditService auditService;
    @Mock private IdempotencyService idempotencyService;

    private CrearProyectoDerivadoService service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // // @NEEDS_CLARIFICATION: la firma del constructor del servicio
        // podria incluir DocumentoService y AutorizacionEfectivaService
        // entre los colaboradores; T065 confirmara el orden y el conjunto
        // exacto. Aqui se modela el conjunto minimo necesario para
        // satisfacer las reglas de la Constitucion 5.0.0.
        service = new CrearProyectoDerivadoServiceImpl(
                registroRepository, relacionRepository,
                codigoProyectoService, catalogoUnidadReader,
                auditService, idempotencyService);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        ((CrearProyectoDerivadoServiceImpl) service).setObjectMapper(objectMapper);

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

    private PortafolioAuthContext contextoResponsable() {
        return new PortafolioAuthContext("sub-resp", 10L, 100L, "Responsable", 1L, 1L, "corr-resp");
    }

    private PortafolioAuthContext contextoEvaluador() {
        return new PortafolioAuthContext("sub-eval", 99L, 999L, "Evaluador", 1L, 1L, "corr-eval");
    }

    private PortafolioAuthContext contextoAutoridad() {
        return new PortafolioAuthContext("sub-aut", 50L, 500L, "Autoridad", 1L, 1L, "corr-aut");
    }

    private CreateDerivedProjectRequest buildRequest() {
        return new CreateDerivedProjectRequest(
                "Proyecto derivado de iniciativa aprobada",
                10L,
                20L,
                List.of(new CreateDerivedProjectRequest.UnidadDerivadaItem(1L, true)),
                10L,
                FuenteOrigen.FICHA_INICIATIVA,
                "Descripcion del proyecto derivado",
                Boolean.FALSE,
                null,
                "Nota opcional",
                500L);
    }

    private RegistroPortafolioEntity iniciativaAprobada(long id) {
        RegistroPortafolioEntity entity = new RegistroPortafolioEntity();
        entity.setId(id);
        entity.setTipoRegistro(TipoRegistro.INICIATIVA);
        entity.setEstado(EstadoIniciativa.INICIATIVA_APROBADA);
        entity.setVersion(0L);
        entity.setUnidadEjecutoraId(1L);
        entity.setResponsableId(10L);
        entity.setFechaInicio(LocalDate.now());
        return entity;
    }

    private void prepararAprobadaSinDerivado() {
        when(registroRepository.findById(INICIATIVA_ID))
                .thenReturn(Optional.of(iniciativaAprobada(INICIATIVA_ID)));
        when(relacionRepository.existsByIniciativaId(INICIATIVA_ID)).thenReturn(false);
        when(catalogoUnidadReader.prefijoUnidad(1L)).thenReturn(Optional.of(PREFIJO_UNIDAD));
        when(codigoProyectoService.generarCodigo(Year.now().getValue(), 1L, PREFIJO_UNIDAD))
                .thenReturn("2026-MIDAGRI-00042");
    }

    // ------------------------------------------------------------------
    // 1) Estado de origen: solo INICIATIVA_APROBADA
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Crear derivado exige iniciativa en INICIATIVA_APROBADA; PRESENTADO se rechaza")
    void crear_iniciativaPresentada_seRechaza() {
        RegistroPortafolioEntity entity = new RegistroPortafolioEntity();
        entity.setId(INICIATIVA_ID);
        entity.setTipoRegistro(TipoRegistro.INICIATIVA);
        entity.setEstado(EstadoIniciativa.PRESENTADO);
        entity.setVersion(0L);
        when(registroRepository.findById(INICIATIVA_ID)).thenReturn(Optional.of(entity));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(INICIATIVA_ID, buildRequest(), contextoResponsable(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("INITIATIVE_NOT_APPROVED")
                    || error.getReason().contains("STATE_TRANSITION_NOT_ALLOWED")),
                "El codigo debe ser INITIATIVE_NOT_APPROVED o STATE_TRANSITION_NOT_ALLOWED");
        verify(relacionRepository, never()).save(any(RelacionIniciativaProyectoEntity.class));
    }

    @Test
    @DisplayName("Crear derivado exige iniciativa en INICIATIVA_APROBADA; estado terminal se rechaza")
    void crear_iniciativaArchivada_terminal_seRechaza() {
        RegistroPortafolioEntity entity = new RegistroPortafolioEntity();
        entity.setId(INICIATIVA_ID);
        entity.setTipoRegistro(TipoRegistro.INICIATIVA);
        entity.setEstado(EstadoIniciativa.INICIATIVA_ARCHIVADA);
        when(registroRepository.findById(INICIATIVA_ID)).thenReturn(Optional.of(entity));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(INICIATIVA_ID, buildRequest(), contextoResponsable(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        verify(relacionRepository, never()).save(any(RelacionIniciativaProyectoEntity.class));
    }

    @Test
    @DisplayName("Crear derivado exitoso: codigo generado, estado PROYECTO_EJECUCION y vinculo inmutable")
    void crear_iniciativaAprobada_generaCodigoYVinculo() {
        prepararAprobadaSinDerivado();
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(PROYECTO_ID);
            e.setVersion(0L);
            return e;
        });
        when(relacionRepository.save(any(RelacionIniciativaProyectoEntity.class)))
                .thenAnswer(invocation -> {
                    RelacionIniciativaProyectoEntity r = invocation.getArgument(0);
                    if (r.getId() == null) {
                        r.setId(7001L);
                    }
                    return r;
                });

        ProjectDetail detalle = service.crear(INICIATIVA_ID, buildRequest(), contextoResponsable(),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(INICIATIVA_ID, detalle.iniciativaId(),
                "El ProjectDetail debe incluir el vinculo a la iniciativa origen");
        assertEquals(PROYECTO_ID, detalle.id(),
                "El proyecto debe recibir un ID propio");
        assertEquals(TipoRegistro.PROYECTO, detalle.tipoRegistro());
        assertEquals(EstadoIniciativa.PROYECTO_EJECUCION, detalle.estado());
        assertEquals("2026-MIDAGRI-00042", detalle.codigo(),
                "El codigo debe haber sido generado por CodigoProyectoService");
        assertEquals(INICIATIVA_ID, detalle.iniciativaId(),
                "El vinculo iniciativaId debe sobrevivir la serializacion");
        verify(relacionRepository, times(1)).save(any(RelacionIniciativaProyectoEntity.class));
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));
    }

    // ------------------------------------------------------------------
    // 2) Carrera del segundo derivado
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Un segundo intento de derivado para la misma iniciativa falla con 409 DERIVATION_ALREADY_EXISTS")
    void crear_segundoDerivado_409DerivationAlreadyExists() {
        prepararAprobadaSinDerivado();
        when(relacionRepository.existsByIniciativaId(INICIATIVA_ID)).thenReturn(true);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(INICIATIVA_ID, buildRequest(), contextoResponsable(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason() != null
                && error.getReason().contains("DERIVATION_ALREADY_EXISTS"),
                "El codigo canonico debe ser DERIVATION_ALREADY_EXISTS");
        verify(registroRepository, never()).save(any(RegistroPortafolioEntity.class));
        verify(relacionRepository, never()).save(any(RelacionIniciativaProyectoEntity.class));
        verify(auditService, times(1)).registrarDenegacion(any(AuditService.AuditCommand.class));
    }

    @Test
    @DisplayName("Dos solicitudes concurrentes de derivado: la primera gana, la segunda recibe 409")
    void crear_dosConcurrentes_primeraGana() throws Exception {
        AtomicReference<Boolean> derivadoPersistido = new AtomicReference<>(false);
        when(registroRepository.findById(INICIATIVA_ID))
                .thenReturn(Optional.of(iniciativaAprobada(INICIATIVA_ID)));
        when(catalogoUnidadReader.prefijoUnidad(1L)).thenReturn(Optional.of(PREFIJO_UNIDAD));
        when(codigoProyectoService.generarCodigo(Year.now().getValue(), 1L, PREFIJO_UNIDAD))
                .thenReturn("2026-MIDAGRI-00042");
        when(relacionRepository.existsByIniciativaId(INICIATIVA_ID)).thenAnswer(invocation -> {
            // La primera vez devuelve false; las subsiguientes, true.
            // El primer committer deja el flag en true.
            return derivadoPersistido.get();
        });
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(PROYECTO_ID);
            e.setVersion(0L);
            derivadoPersistido.set(true);
            return e;
        });
        java.util.concurrent.atomic.AtomicBoolean relacionPersistida = new java.util.concurrent.atomic.AtomicBoolean(false);
        when(relacionRepository.save(any(RelacionIniciativaProyectoEntity.class)))
                .thenAnswer(invocation -> {
                    RelacionIniciativaProyectoEntity r = invocation.getArgument(0);
                    if (!relacionPersistida.compareAndSet(false, true)) {
                        throw new org.springframework.dao.DataIntegrityViolationException(
                                "UK_INICIATIVA_DERIVADA");
                    }
                    if (r.getId() == null) {
                        r.setId(7001L);
                    }
                    return r;
                });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            Future<ProjectDetail> f1 = pool.submit(() -> {
                inicio.await();
                return service.crear(INICIATIVA_ID, buildRequest(), contextoResponsable(),
                        "k-1", PAYLOAD_JSON);
            });
            Future<ProjectDetail> f2 = pool.submit(() -> {
                inicio.await();
                return service.crear(INICIATIVA_ID, buildRequest(), contextoResponsable(),
                        "k-2", PAYLOAD_JSON);
            });
            inicio.countDown();

            boolean unoGano = false;
            boolean unoPerdio = false;
            for (Future<ProjectDetail> f : List.of(f1, f2)) {
                try {
                    ProjectDetail d = f.get(5, TimeUnit.SECONDS);
                    assertNotNull(d);
                    unoGano = true;
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    assertTrue(causa instanceof ResponseStatusException,
                            () -> "Se esperaba ResponseStatusException, fue: " + causa);
                    ResponseStatusException rse = (ResponseStatusException) causa;
                    assertEquals(HttpStatus.CONFLICT, rse.getStatusCode());
                    assertTrue(rse.getReason() != null
                            && rse.getReason().contains("DERIVATION_ALREADY_EXISTS"));
                    unoPerdio = true;
                }
            }
            assertTrue(unoGano, "Una de las dos solicitudes concurrentes debe triunfar");
            assertTrue(unoPerdio, "La otra debe recibir 409 DERIVATION_ALREADY_EXISTS");
        } finally {
            pool.shutdownNow();
        }
    }

    // ------------------------------------------------------------------
    // 3) Autorizacion efectiva: solo Responsable
    // ------------------------------------------------------------------

    @Test
    @DisplayName("El Responsable autorizado dentro de su ambito puede crear derivado")
    void crear_responsableAutorizado_permitido() {
        prepararAprobadaSinDerivado();
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(PROYECTO_ID);
            e.setVersion(0L);
            return e;
        });
        when(relacionRepository.save(any(RelacionIniciativaProyectoEntity.class)))
                .thenAnswer(invocation -> {
                    RelacionIniciativaProyectoEntity r = invocation.getArgument(0);
                    if (r.getId() == null) {
                        r.setId(7001L);
                    }
                    return r;
                });

        ProjectDetail detalle = service.crear(INICIATIVA_ID, buildRequest(), contextoResponsable(),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);
        assertNotNull(detalle);
    }

    @Test
    @DisplayName("El Evaluador no puede crear derivado: ASSIGNMENT_SCOPE_DENIED")
    void crear_evaluador_seRechaza() {
        prepararAprobadaSinDerivado();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(INICIATIVA_ID, buildRequest(), contextoEvaluador(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertTrue(error.getStatusCode() == HttpStatus.FORBIDDEN
                || error.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY,
                "El Evaluador debe ser rechazado con 403 o 422");
        assertTrue(error.getReason() != null
                && (error.getReason().contains("ASSIGNMENT_SCOPE_DENIED")
                    || error.getReason().contains("FORBIDDEN_PROFILE")));
        verify(relacionRepository, never()).save(any(RelacionIniciativaProyectoEntity.class));
    }

    @Test
    @DisplayName("La Autoridad no puede crear derivado: el derivado es responsabilidad del Responsable")
    void crear_autoridad_seRechaza() {
        prepararAprobadaSinDerivado();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(INICIATIVA_ID, buildRequest(), contextoAutoridad(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertTrue(error.getStatusCode() == HttpStatus.FORBIDDEN
                || error.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY);
        assertTrue(error.getReason() != null
                && (error.getReason().contains("ASSIGNMENT_SCOPE_DENIED")
                    || error.getReason().contains("FORBIDDEN_PROFILE")));
        verify(relacionRepository, never()).save(any(RelacionIniciativaProyectoEntity.class));
    }

    // ------------------------------------------------------------------
    // 4) Documento formal obligatorio
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Sin documento formal se rechaza con 422 FORMAL_DOCUMENT_REQUIRED")
    void crear_sinDocumentoFormal_seRechaza() {
        prepararAprobadaSinDerivado();
        CreateDerivedProjectRequest sinDocumento = new CreateDerivedProjectRequest(
                "Proyecto sin documento", 10L, 20L,
                List.of(new CreateDerivedProjectRequest.UnidadDerivadaItem(1L, true)),
                10L, FuenteOrigen.FICHA_INICIATIVA, "Descripcion",
                Boolean.FALSE, null, null,
                null);

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(INICIATIVA_ID, sinDocumento, contextoResponsable(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("FORMAL_DOCUMENT_REQUIRED")
                    || error.getReason().contains("EVIDENCE_NOT_ELIGIBLE")
                    || error.getReason().contains("FORMAL_DECISION_REQUIRED")));
        verify(relacionRepository, never()).save(any(RelacionIniciativaProyectoEntity.class));
    }

    // ------------------------------------------------------------------
    // 5) La iniciativa NO cambia de estado
    // ------------------------------------------------------------------

    @Test
    @DisplayName("La iniciativa permanece en INICIATIVA_APROBADA tras la creacion del derivado")
    void crear_iniciativaNoCambiaEstado() {
        prepararAprobadaSinDerivado();
        RegistroPortafolioEntity iniciativa = iniciativaAprobada(INICIATIVA_ID);
        when(registroRepository.findById(INICIATIVA_ID)).thenReturn(Optional.of(iniciativa));
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(PROYECTO_ID);
            e.setVersion(0L);
            return e;
        });
        when(relacionRepository.save(any(RelacionIniciativaProyectoEntity.class)))
                .thenAnswer(invocation -> {
                    RelacionIniciativaProyectoEntity r = invocation.getArgument(0);
                    if (r.getId() == null) {
                        r.setId(7001L);
                    }
                    return r;
                });

        service.crear(INICIATIVA_ID, buildRequest(), contextoResponsable(),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        // La iniciativa conserva su estado INICIATIVA_APROBADA: la
        // creacion del proyecto nunca muta el registro de origen.
        assertEquals(EstadoIniciativa.INICIATIVA_APROBADA, iniciativa.getEstado(),
                "La iniciativa debe permanecer en INICIATIVA_APROBADA");
    }

    // ------------------------------------------------------------------
    // 6) Validaciones de campos oficiales
    // ------------------------------------------------------------------

    @Test
    @DisplayName("El nombre del proyecto es obligatorio (campo 5)")
    void crear_sinNombre_seRechaza() {
        prepararAprobadaSinDerivado();
        CreateDerivedProjectRequest sinNombre = new CreateDerivedProjectRequest(
                "  ", 10L, 20L,
                List.of(new CreateDerivedProjectRequest.UnidadDerivadaItem(1L, true)),
                10L, FuenteOrigen.FICHA_INICIATIVA, "Descripcion",
                Boolean.FALSE, null, null, 500L);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(INICIATIVA_ID, sinNombre, contextoResponsable(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertTrue(error.getReason() != null
                && error.getReason().contains("OFFICIAL_FIELD_REQUIRED"));
    }

    @Test
    @DisplayName("La unidad principal es obligatoria (campo 12 + UNIT_MAIN_CARDINALITY)")
    void crear_sinUnidadPrincipal_seRechaza() {
        prepararAprobadaSinDerivado();
        CreateDerivedProjectRequest sinUnidad = new CreateDerivedProjectRequest(
                "Nombre valido", 10L, 20L,
                List.of(new CreateDerivedProjectRequest.UnidadDerivadaItem(1L, false),
                        new CreateDerivedProjectRequest.UnidadDerivadaItem(2L, false)),
                10L, FuenteOrigen.FICHA_INICIATIVA, "Descripcion",
                Boolean.FALSE, null, null, 500L);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(INICIATIVA_ID, sinUnidad, contextoResponsable(),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("UNIT_MAIN_CARDINALITY")
                    || error.getReason().contains("OFFICIAL_FIELD_REQUIRED")));
    }

    @Test
    @DisplayName("Idempotency-Key ausente se rechaza con IDEMPOTENCY_KEY_REQUIRED")
    void crear_sinIdempotencyKey_seRechaza() {
        prepararAprobadaSinDerivado();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.crear(INICIATIVA_ID, buildRequest(), contextoResponsable(),
                        null, PAYLOAD_JSON));
        assertTrue(error.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY
                || error.getStatusCode() == HttpStatus.BAD_REQUEST);
        assertTrue(error.getReason() != null
                && error.getReason().contains("IDEMPOTENCY_KEY_REQUIRED"));
    }

    // ------------------------------------------------------------------
    // 7) Vinculo inmutable
    // ------------------------------------------------------------------

    @Test
    @DisplayName("El vinculo se persiste una sola vez con ID_INICIATIVA y ID_PROYECTO distintos")
    void crear_vinculoUnicoIniciativaYProyecto() {
        prepararAprobadaSinDerivado();
        AtomicReference<RelacionIniciativaProyectoEntity> capturada = new AtomicReference<>();
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(PROYECTO_ID);
            e.setVersion(0L);
            return e;
        });
        when(relacionRepository.save(any(RelacionIniciativaProyectoEntity.class)))
                .thenAnswer(invocation -> {
                    RelacionIniciativaProyectoEntity r = invocation.getArgument(0);
                    capturada.set(r);
                    if (r.getId() == null) {
                        r.setId(7001L);
                    }
                    return r;
                });

        service.crear(INICIATIVA_ID, buildRequest(), contextoResponsable(),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        RelacionIniciativaProyectoEntity relacion = capturada.get();
        assertNotNull(relacion, "El servicio debe persistir la relacion");
        assertEquals(INICIATIVA_ID, relacion.getIniciativaId());
        assertEquals(PROYECTO_ID, relacion.getProyectoId());
        assertNotEquals(relacion.getIniciativaId(), relacion.getProyectoId(),
                "El CHECK CK_IP_DISTINTOS exige iniciativaId <> proyectoId");
    }

    // ------------------------------------------------------------------
    // 8) Contrato de interfaz
    // ------------------------------------------------------------------

    @Test
    @DisplayName("El servicio expone contrato de interfaz y no entidades JPA en su API publica")
    void servicioExponeContratoSinEntidadesJPA() {
        assertTrue(CrearProyectoDerivadoService.class.isInterface());
        for (var metodo : CrearProyectoDerivadoService.class.getDeclaredMethods()) {
            String retorno = metodo.getReturnType().getName();
            assertFalse(retorno.contains("pe.gob.midagri.piip.portafolio.entity")
                            || retorno.contains("pe.gob.midagri.piip.portafolio.repository"),
                    () -> "El metodo " + metodo.getName() + " no debe retornar entidades JPA");
        }
    }

    @Test
    @DisplayName("La respuesta ProjectDetail expone DTO HTTP, no entidades JPA")
    void respuestaEsDtoHttp() {
        for (var c : ProjectDetail.class.getRecordComponents()) {
            Class<?> tipo = c.getType();
            assertFalse((tipo.getName().contains("pe.gob.midagri.piip.portafolio.entity") && !tipo.isEnum())
                            || tipo.getName().contains("pe.gob.midagri.piip.portafolio.repository"),
                    () -> "El campo " + c.getName() + " no debe ser una entidad JPA");
        }
    }

    // ------------------------------------------------------------------
    // 9) Auditoria atomica
    // ------------------------------------------------------------------

    @Test
    @DisplayName("La auditoria se invoca tanto en exito como en denegacion del derivado")
    void auditoriaExitoYDenegacion() {
        // Caso 1: exito.
        prepararAprobadaSinDerivado();
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(PROYECTO_ID);
            e.setVersion(0L);
            return e;
        });
        when(relacionRepository.save(any(RelacionIniciativaProyectoEntity.class)))
                .thenAnswer(invocation -> {
                    RelacionIniciativaProyectoEntity r = invocation.getArgument(0);
                    if (r.getId() == null) {
                        r.setId(7001L);
                    }
                    return r;
                });
        service.crear(INICIATIVA_ID, buildRequest(), contextoResponsable(),
                "k-1", PAYLOAD_JSON);
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));

        // Caso 2: denegacion por perfil no autorizado.
        assertThrows(ResponseStatusException.class,
                () -> service.crear(INICIATIVA_ID, buildRequest(), contextoEvaluador(),
                        "k-2", PAYLOAD_JSON));
        verify(auditService, times(1)).registrarDenegacion(any(AuditService.AuditCommand.class));
    }
}
