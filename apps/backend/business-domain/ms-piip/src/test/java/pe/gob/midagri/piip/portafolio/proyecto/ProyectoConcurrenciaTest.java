package pe.gob.midagri.piip.portafolio.proyecto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.RelacionIniciativaProyectoEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.dto.CreateDerivedProjectRequest;
import pe.gob.midagri.piip.portafolio.dto.DirectProjectRequest;
import pe.gob.midagri.piip.portafolio.dto.ProjectDetail;
import pe.gob.midagri.piip.portafolio.dto.TipoOrigenDirecto;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.repository.RelacionIniciativaProyectoRepository;
import pe.gob.midagri.piip.portafolio.service.CatalogoUnidadReader;
import pe.gob.midagri.piip.portafolio.service.CodigoProyectoService;
import pe.gob.midagri.piip.portafolio.service.CrearProyectoDerivadoService;
import pe.gob.midagri.piip.portafolio.service.CrearProyectoDirectoService;
import pe.gob.midagri.piip.portafolio.service.impl.CrearProyectoDerivadoServiceImpl;
import pe.gob.midagri.piip.portafolio.service.impl.CrearProyectoDirectoServiceImpl;

/**
 * Pruebas de concurrencia para la creacion de proyectos derivados y
 * directos conforme a la Constitucion 5.0.0 (US3) y al script DDL
 * {@code 010_iniciativa_proyecto_relacion.sql}.
 *
 * <p>Simula la carrera entre dos Responsables que intentan crear el
 * derivado de la misma iniciativa al mismo tiempo, o entre dos
 * Autoridades que intentan crear un proyecto directo en la misma
 * unidad y anio. La primera confirmacion debe ganar; la segunda debe
 * recibir 409 con un codigo canonico
 * ({@code DERIVATION_ALREADY_EXISTS} o
 * {@code DIRECT_PROJECT_NOT_AUTHORIZED}).
 *
 * <p>La prueba sustituye {@code @SpringBootTest} +
 * {@code OracleContainer} con mocks equivalentes que modelan la
 * semantica del bloqueo pesimista y de las UK de Oracle. El escenario
 * real con Testcontainers y bloqueo pesimista Oracle sera ejecutado en
 * {@code ProyectoConcurrenciaIT} tras la implementacion de los
 * servicios en T065/T066.
 *
 * <p>Marcado con {@code // @NEEDS_CLARIFICATION} en las firmas que
 * T065 y T066 ajustaran.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("US3 - Concurrencia: primera confirmacion gana, segunda recibe 409 DERIVATION o DIRECT")
class ProyectoConcurrenciaTest {

    private static final String PREFIJO_UNIDAD = "MIDAGRI";
    private static final String PAYLOAD_JSON = "{}";
    private static final long INICIATIVA_ID = 1001L;
    private static final long UNIDAD_ID = 1L;
    private static final int ANIO_ACTUAL = Year.now().getValue();

    @Mock private RegistroPortafolioRepository registroRepository;
    @Mock private RelacionIniciativaProyectoRepository relacionRepository;
    @Mock private CodigoProyectoService codigoProyectoService;
    @Mock private CatalogoUnidadReader catalogoUnidadReader;
    @Mock private AuditService auditService;
    @Mock private IdempotencyService idempotencyService;

    private CrearProyectoDerivadoServiceImpl derivadoService;
    private CrearProyectoDirectoServiceImpl directoService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        derivadoService = new CrearProyectoDerivadoServiceImpl(
                registroRepository, relacionRepository,
                codigoProyectoService, catalogoUnidadReader,
                auditService, idempotencyService);
        derivadoService.setObjectMapper(objectMapper);
        directoService = new CrearProyectoDirectoServiceImpl(
                registroRepository, codigoProyectoService, catalogoUnidadReader,
                auditService, idempotencyService);
        directoService.setObjectMapper(objectMapper);

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

    private PortafolioAuthContext contextoAutoridad() {
        return new PortafolioAuthContext("sub-aut", 50L, 500L, "Autoridad", 1L, 1L, "corr-aut");
    }

    private CreateDerivedProjectRequest buildDerivado() {
        return new CreateDerivedProjectRequest(
                "Proyecto derivado", 10L, 20L,
                List.of(new CreateDerivedProjectRequest.UnidadDerivadaItem(1L, true)),
                10L, FuenteOrigen.FICHA_INICIATIVA,
                "Descripcion valida", Boolean.FALSE, null, null, 500L);
    }

    private DirectProjectRequest buildDirecto() {
        return new DirectProjectRequest(
                TipoOrigenDirecto.HEREDADO, "LEGACY-2024-001",
                LocalDate.of(2024, 3, 15), "Proyecto heredado",
                10L, 20L, UNIDAD_ID, 10L,
                "Descripcion valida", Boolean.FALSE, null, null,
                800L, List.of(900L), FuenteOrigen.FICHA_INICIATIVA);
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

    // ------------------------------------------------------------------
    // 1) Carrera del derivado: dos solicitudes concurrentes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Dos Responsables crean derivado de la misma iniciativa: la primera gana, la segunda recibe 409")
    void carreraDerivado_dosConcurrente_primeraGana() throws Exception {
        AtomicReference<Boolean> derivadoPersistido = new AtomicReference<>(false);
        java.util.concurrent.atomic.AtomicInteger relacionSaveCount = new java.util.concurrent.atomic.AtomicInteger();
        when(registroRepository.findById(INICIATIVA_ID))
                .thenReturn(Optional.of(iniciativaAprobada(INICIATIVA_ID)));
        when(catalogoUnidadReader.prefijoUnidad(1L)).thenReturn(Optional.of(PREFIJO_UNIDAD));
        when(codigoProyectoService.generarCodigo(ANIO_ACTUAL, 1L, PREFIJO_UNIDAD))
                .thenReturn("2026-MIDAGRI-00042");
        when(relacionRepository.existsByIniciativaId(INICIATIVA_ID)).thenAnswer(invocation -> {
            // La primera vez devuelve false; las subsiguientes, true.
            return derivadoPersistido.get();
        });
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(2001L);
            e.setVersion(0L);
            derivadoPersistido.set(true);
            return e;
        });
        when(relacionRepository.save(any(RelacionIniciativaProyectoEntity.class)))
                .thenAnswer(invocation -> {
                    if (relacionSaveCount.getAndIncrement() > 0) {
                        throw new DataIntegrityViolationException("UK_INICIATIVA_PROYECTO");
                    }
                    RelacionIniciativaProyectoEntity r = invocation.getArgument(0);
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
                return derivadoService.crear(INICIATIVA_ID, buildDerivado(), contextoResponsable(),
                        "k-1", PAYLOAD_JSON);
            });
            Future<ProjectDetail> f2 = pool.submit(() -> {
                inicio.await();
                return derivadoService.crear(INICIATIVA_ID, buildDerivado(), contextoResponsable(),
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
                            && rse.getReason().contains("DERIVATION_ALREADY_EXISTS"),
                            () -> "El codigo canonico debe ser DERIVATION_ALREADY_EXISTS; fue: "
                                    + rse.getReason());
                    unoPerdio = true;
                }
            }
            assertTrue(unoGano, "Una de las dos confirmaciones concurrentes debe triunfar");
            assertTrue(unoPerdio, "La otra debe recibir 409 DERIVATION_ALREADY_EXISTS");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("La primera confirmacion que gana la carrera persiste el vinculo inmutable")
    void carreraDerivado_ganadorPersisteVinculo() throws Exception {
        AtomicReference<RelacionIniciativaProyectoEntity> capturada = new AtomicReference<>();
        when(registroRepository.findById(INICIATIVA_ID))
                .thenReturn(Optional.of(iniciativaAprobada(INICIATIVA_ID)));
        when(catalogoUnidadReader.prefijoUnidad(1L)).thenReturn(Optional.of(PREFIJO_UNIDAD));
        when(codigoProyectoService.generarCodigo(ANIO_ACTUAL, 1L, PREFIJO_UNIDAD))
                .thenReturn("2026-MIDAGRI-00042");
        when(relacionRepository.existsByIniciativaId(INICIATIVA_ID)).thenReturn(false);
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(2001L);
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

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        AtomicReference<Throwable> ganador = new AtomicReference<>();
        AtomicReference<Throwable> perdedor = new AtomicReference<>();
        try {
            Future<ProjectDetail> f1 = pool.submit(() -> {
                inicio.await();
                try {
                    return derivadoService.crear(INICIATIVA_ID, buildDerivado(),
                            contextoResponsable(), "k-1", PAYLOAD_JSON);
                } catch (Throwable t) {
                    ganador.set(t);
                    throw t;
                }
            });
            Future<ProjectDetail> f2 = pool.submit(() -> {
                inicio.await();
                try {
                    return derivadoService.crear(INICIATIVA_ID, buildDerivado(),
                            contextoResponsable(), "k-2", PAYLOAD_JSON);
                } catch (Throwable t) {
                    perdedor.set(t);
                    throw t;
                }
            });
            inicio.countDown();
            try {
                f1.get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // Puede que el ganador sea f1 o f2; ya capturamos la excepcion arriba.
            }
            try {
                f2.get(5, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                // Idem para el perdedor.
            }
        } finally {
            pool.shutdownNow();
        }

        RelacionIniciativaProyectoEntity relacion = capturada.get();
        assertNotNull(relacion,
                "La primera confirmacion que gana debe persistir la relacion inmutable");
        assertEquals(INICIATIVA_ID, relacion.getIniciativaId());
        assertEquals(2001L, relacion.getProyectoId());

        // Solo una transicion de exito: el ganador persiste una sola
        // fila; el perdedor queda bloqueado por la UK sin afectar el
        // estado.
        org.mockito.Mockito.verify(relacionRepository, org.mockito.Mockito.times(2))
                .save(any(RelacionIniciativaProyectoEntity.class));
    }

    // ------------------------------------------------------------------
    // 2) Carrera del directo: dos solicitudes concurrentes
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Dos Autoridades crean directo en la misma unidad y anio: la primera gana, la segunda recibe 409")
    void carreraDirecto_dosConcurrente_primeraGana() throws Exception {
        AtomicReference<Boolean> directoPersistido = new AtomicReference<>(false);
        java.util.concurrent.atomic.AtomicInteger saveCount = new java.util.concurrent.atomic.AtomicInteger();
        when(catalogoUnidadReader.prefijoUnidad(UNIDAD_ID)).thenReturn(Optional.of(PREFIJO_UNIDAD));
        when(codigoProyectoService.generarCodigo(ANIO_ACTUAL, UNIDAD_ID, PREFIJO_UNIDAD))
                .thenReturn("2026-MIDAGRI-00010");
        when(registroRepository.existsByUnidadEjecutoraIdAndAnioAndEstado(UNIDAD_ID,
                ANIO_ACTUAL, EstadoIniciativa.PROYECTO_EJECUCION.name()))
                .thenAnswer(invocation -> directoPersistido.get());
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            if (saveCount.getAndIncrement() > 0) {
                throw new DataIntegrityViolationException("UK_PROYECTO_ACTIVO_POR_UNIDAD_ANIO");
            }
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(9001L);
            e.setVersion(0L);
            directoPersistido.set(true);
            return e;
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            Future<ProjectDetail> f1 = pool.submit(() -> {
                inicio.await();
                return directoService.crear(buildDirecto(), contextoAutoridad(),
                        "k-1", PAYLOAD_JSON);
            });
            Future<ProjectDetail> f2 = pool.submit(() -> {
                inicio.await();
                return directoService.crear(buildDirecto(), contextoAutoridad(),
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
                            && (rse.getReason().contains("DIRECT_PROJECT_NOT_AUTHORIZED")
                                || rse.getReason().contains("DIRECT_PROJECT_DUPLICATE")
                                || rse.getReason().contains("ACTIVE_PROJECT_EXISTS")),
                            () -> "El codigo canonico debe identificar el bloqueo de duplicado; "
                                    + "fue: " + rse.getReason());
                    unoPerdio = true;
                }
            }
            assertTrue(unoGano, "Una de las dos confirmaciones concurrentes debe triunfar");
            assertTrue(unoPerdio, "La otra debe recibir 409 con codigo canonico");
        } finally {
            pool.shutdownNow();
        }
    }

    // ------------------------------------------------------------------
    // 3) Secuencial: dos confirmaciones sin colision avanzan el estado
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Confirmaciones secuenciales no colisionan: la primera derivada guarda, la segunda directa avanza en otra unidad")
    void secuencial_derivadoYDirecto_avanzan() {
        when(registroRepository.findById(INICIATIVA_ID))
                .thenReturn(Optional.of(iniciativaAprobada(INICIATIVA_ID)));
        when(catalogoUnidadReader.prefijoUnidad(1L)).thenReturn(Optional.of(PREFIJO_UNIDAD));
        when(codigoProyectoService.generarCodigo(ANIO_ACTUAL, 1L, PREFIJO_UNIDAD))
                .thenReturn("2026-MIDAGRI-00042");
        when(relacionRepository.existsByIniciativaId(INICIATIVA_ID)).thenReturn(false);
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(System.nanoTime());
            e.setVersion(0L);
            return e;
        });
        when(relacionRepository.save(any(RelacionIniciativaProyectoEntity.class)))
                .thenAnswer(invocation -> {
                    RelacionIniciativaProyectoEntity r = invocation.getArgument(0);
                    if (r.getId() == null) {
                        r.setId(System.nanoTime());
                    }
                    return r;
                });
        when(registroRepository.existsByUnidadEjecutoraIdAndAnioAndEstado(2L,
                ANIO_ACTUAL, EstadoIniciativa.PROYECTO_EJECUCION.name()))
                .thenReturn(false);

        // Confirmacion 1: derivado en la iniciativa 1001.
        ProjectDetail derivado = derivadoService.crear(INICIATIVA_ID, buildDerivado(),
                contextoResponsable(), "k-1", PAYLOAD_JSON);
        assertNotNull(derivado);

        // Confirmacion 2: directo en otra unidad (2L) sin colision.
        DirectProjectRequest directoOtraUnidad = new DirectProjectRequest(
                TipoOrigenDirecto.EXCEPCION_FORMAL, null,
                LocalDate.of(ANIO_ACTUAL, 6, 1),
                "Proyecto excepcional en otra unidad",
                10L, 20L, 2L, 50L,
                "Descripcion valida", Boolean.FALSE, null, null,
                850L, List.of(950L), FuenteOrigen.OTROS);
        when(catalogoUnidadReader.prefijoUnidad(2L)).thenReturn(Optional.of("OM"));
        when(codigoProyectoService.generarCodigo(ANIO_ACTUAL, 2L, "OM"))
                .thenReturn("2026-OM-00010");
        ProjectDetail directo = directoService.crear(directoOtraUnidad, contextoAutoridad(),
                "k-2", PAYLOAD_JSON);
        assertNotNull(directo);
    }

    // ------------------------------------------------------------------
    // 4) Confirma la semantica de la primera confirmacion
    // ------------------------------------------------------------------

    @Test
    @DisplayName("La primera confirmacion que gana ve el resultado correcto: estado PROYECTO_EJECUCION y codigo generado")
    void carrera_ganadorVeResultadoCorrecto() throws Exception {
        AtomicReference<Boolean> derivadoPersistido = new AtomicReference<>(false);
        when(registroRepository.findById(INICIATIVA_ID))
                .thenReturn(Optional.of(iniciativaAprobada(INICIATIVA_ID)));
        when(catalogoUnidadReader.prefijoUnidad(1L)).thenReturn(Optional.of(PREFIJO_UNIDAD));
        when(codigoProyectoService.generarCodigo(ANIO_ACTUAL, 1L, PREFIJO_UNIDAD))
                .thenReturn("2026-MIDAGRI-00042");
        when(relacionRepository.existsByIniciativaId(INICIATIVA_ID))
                .thenAnswer(invocation -> derivadoPersistido.get());
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setId(2001L);
            e.setVersion(0L);
            derivadoPersistido.set(true);
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

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            Future<ProjectDetail> f1 = pool.submit(() -> {
                inicio.await();
                return derivadoService.crear(INICIATIVA_ID, buildDerivado(),
                        contextoResponsable(), "k-1", PAYLOAD_JSON);
            });
            Future<ProjectDetail> f2 = pool.submit(() -> {
                inicio.await();
                return derivadoService.crear(INICIATIVA_ID, buildDerivado(),
                        contextoResponsable(), "k-2", PAYLOAD_JSON);
            });
            inicio.countDown();

            ProjectDetail ganador = null;
            for (Future<ProjectDetail> f : List.of(f1, f2)) {
                try {
                    ProjectDetail d = f.get(5, TimeUnit.SECONDS);
                    if (ganador == null) {
                        ganador = d;
                    }
                } catch (Exception ignored) {
                    // El perdedor cae aqui; se ignora.
                }
            }
            assertNotNull(ganador, "El ganador debe recibir un ProjectDetail valido");
            assertEquals("2026-MIDAGRI-00042", ganador.codigo());
            assertEquals(EstadoIniciativa.PROYECTO_EJECUCION, ganador.estado());
            assertEquals(TipoRegistro.PROYECTO, ganador.tipoRegistro());
            assertEquals(INICIATIVA_ID, ganador.iniciativaId());
        } finally {
            pool.shutdownNow();
        }
    }
}
