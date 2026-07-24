package pe.gob.midagri.piip.portafolio.transicion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.entity.TransicionEstadoEntity;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.repository.TransicionEstadoRepository;

/**
 * Pruebas de concurrencia para la maquina de estados canonica.
 *
 * <p>Simula la carrera entre dos Evaluadores que confirman la misma
 * transicion sobre la misma iniciativa al mismo tiempo. La primera
 * confirmacion debe ganar; la segunda debe recibir {@code 412
 * STATE_CHANGED} porque la entidad JPA ya incremento su version
 * optimista. La prueba modela este escenario con dos hilos, un
 * {@code CountDownLatch} y un repositorio que mantiene una version
 * observable compartida.
 *
 * <p>Esta prueba sustituye a {@code @SpringBootTest} +
 * {@code OracleContainer} con mocks equivalentes; el escenario real con
 * Testcontainers y bloqueo pesimista Oracle sera ejecutado en
 * {@code MaquinaEstadosConcurrenciaIT} tras la implementacion del
 * servicio en T058.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("US2 - Concurrencia: primera confirmacion gana, segunda recibe 412 STATE_CHANGED")
class MaquinaEstadosConcurrenciaTest {

    @Mock private TransicionEstadoRepository transicionRepository;
    @Mock private RegistroPortafolioRepository registroRepository;
    @Mock private AuditService auditService;
    @Mock private IdempotencyService idempotencyService;

    private TransicionEstadoServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new TransicionEstadoServiceImpl(
                transicionRepository, registroRepository, auditService, idempotencyService);
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

    private PortafolioAuthContext contextoEvaluador() {
        return new PortafolioAuthContext("sub-eval", 99L, 999L, "Evaluador", 1L, 1L, "corr-eval");
    }

    /**
     * Version compartida entre los dos hilos: se incrementa cuando el
     * primero hace commit; el segundo hilo, que leyo la version previa,
     * recibe 412.
     */
    @Test
    @DisplayName("Dos Evaluadores confirman la misma transicion: el primero gana, el segundo recibe 412")
    void dosConcurrente_primeraGana412() throws Exception {
        AtomicLong versionCompartida = new AtomicLong(0L);
        AtomicReference<RegistroPortafolioEntity> entityCompartida = new AtomicReference<>();
        RegistroPortafolioEntity inicial = new RegistroPortafolioEntity();
        inicial.setId(1L);
        inicial.setTipoRegistro(TipoRegistro.INICIATIVA);
        inicial.setEstado(EstadoIniciativa.PRESENTADO);
        inicial.setVersion(0L);
        entityCompartida.set(inicial);

        when(registroRepository.findById(1L)).thenAnswer(invocation -> {
            // Cada lectura observa la version vigente: el ganador la
            // incrementa a 1; el perdedor la ve ya en 1.
            RegistroPortafolioEntity copia = new RegistroPortafolioEntity();
            copia.setId(1L);
            copia.setTipoRegistro(TipoRegistro.INICIATIVA);
            copia.setEstado(EstadoIniciativa.PRESENTADO);
            copia.setVersion(versionCompartida.get());
            return java.util.Optional.of(copia);
        });
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            // Simulamos la condicion de guarda: si la version esperada (0L)
            // no coincide con la real (1L despues del ganador), lanzamos 412.
            long versionActual = versionCompartida.get();
            if (e.getVersion() != null && e.getVersion() != versionActual) {
                throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                        "STATE_CHANGED: la version actual es " + versionActual
                                + " y se esperaba " + e.getVersion());
            }
            versionCompartida.incrementAndGet();
            e.setVersion(versionActual + 1L);
            return e;
        });
        when(transicionRepository.save(any(TransicionEstadoEntity.class)))
                .thenAnswer(invocation -> {
                    TransicionEstadoEntity t = invocation.getArgument(0);
                    if (t.getId() == null) {
                        t.setId(System.nanoTime());
                    }
                    return t;
                });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            TransicionCommand cmd =
                    new TransicionCommand(
                            EstadoIniciativa.NO_ADMISIBLE, "obs", 1L, "1-0");

            Future<TransicionDetail> f1 = pool.submit(() -> {
                inicio.await();
                return service.transicionar(1L, cmd, contextoEvaluador(), "k-1", "{}");
            });
            Future<TransicionDetail> f2 = pool.submit(() -> {
                inicio.await();
                return service.transicionar(1L, cmd, contextoEvaluador(), "k-2", "{}");
            });
            inicio.countDown();

            // Solo uno gana.
            boolean unaGano = false;
            boolean unaPerdio = false;
            for (Future<TransicionDetail> f : List.of(f1, f2)) {
                try {
                    TransicionDetail d = f.get(5, TimeUnit.SECONDS);
                    assertNotNull(d);
                    unaGano = true;
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    assertTrue(causa instanceof ResponseStatusException,
                            () -> "Se esperaba ResponseStatusException, fue: " + causa);
                    ResponseStatusException rse = (ResponseStatusException) causa;
                    assertEquals(HttpStatus.PRECONDITION_FAILED, rse.getStatusCode());
                    assertTrue(rse.getReason() != null
                            && rse.getReason().contains("STATE_CHANGED"));
                    unaPerdio = true;
                }
            }
            assertTrue(unaGano, "Una de las dos confirmaciones concurrentes debe triunfar");
            assertTrue(unaPerdio, "La otra confirmacion debe recibir 412 STATE_CHANGED");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("Confirmaciones secuenciales sobre la misma iniciativa avanzan la version sin colision")
    void secuencial_dosConfirmaciones_seguidas_avanzanVersion() {
        AtomicLong versionCompartida = new AtomicLong(0L);
        when(registroRepository.findById(2L)).thenAnswer(invocation -> {
            RegistroPortafolioEntity copia = new RegistroPortafolioEntity();
            copia.setId(2L);
            copia.setTipoRegistro(TipoRegistro.INICIATIVA);
            copia.setEstado(EstadoIniciativa.PRESENTADO);
            copia.setVersion(versionCompartida.get());
            return java.util.Optional.of(copia);
        });
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setVersion(versionCompartida.incrementAndGet());
            return e;
        });
        when(transicionRepository.save(any(TransicionEstadoEntity.class)))
                .thenAnswer(invocation -> {
                    TransicionEstadoEntity t = invocation.getArgument(0);
                    if (t.getId() == null) {
                        t.setId(System.nanoTime());
                    }
                    return t;
                });

        TransicionCommand cmd1 =
                new TransicionCommand(
                        EstadoIniciativa.NO_ADMISIBLE, "primera", 1L, "2-0");
        TransicionCommand cmd2 =
                new TransicionCommand(
                        EstadoIniciativa.NO_APLICABLE, "segunda", 2L, "2-1");

        TransicionDetail primera =
                service.transicionar(2L, cmd1, contextoEvaluador(), "k-1", "{}");
        TransicionDetail segunda =
                service.transicionar(2L, cmd2, contextoEvaluador(), "k-2", "{}");

        assertNotNull(primera);
        assertNotNull(segunda);
        // La version final debe haber avanzado: 0 -> 1 -> 2.
        assertTrue(versionCompartida.get() >= 2L);
    }

    @Test
    @DisplayName("Una confirmacion posterior con ETag obsoleto se rechaza con 412 STATE_CHANGED")
    void etagObsoletoEnConfirmacionPosterior_412() {
        AtomicLong versionCompartida = new AtomicLong(1L);
        when(registroRepository.findById(3L)).thenAnswer(invocation -> {
            RegistroPortafolioEntity copia = new RegistroPortafolioEntity();
            copia.setId(3L);
            copia.setTipoRegistro(TipoRegistro.INICIATIVA);
            copia.setEstado(EstadoIniciativa.PRESENTADO);
            copia.setVersion(versionCompartida.get());
            return java.util.Optional.of(copia);
        });

        TransicionCommand cmdObsoleto =
                new TransicionCommand(
                        EstadoIniciativa.NO_ADMISIBLE, "obs", 1L, "\"3-0\"");
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.transicionar(3L, cmdObsoleto, contextoEvaluador(), "k-1", "{}"));
        assertEquals(HttpStatus.PRECONDITION_FAILED, error.getStatusCode());
        assertTrue(error.getReason() != null && error.getReason().contains("STATE_CHANGED"));
    }

    @Test
    @DisplayName("La primera confirmacion que gana la carrera registra la transicion append-only")
    void primeraConfirmacionRegistraTransicion() {
        AtomicLong versionCompartida = new AtomicLong(0L);
        when(registroRepository.findById(4L)).thenAnswer(invocation -> {
            RegistroPortafolioEntity copia = new RegistroPortafolioEntity();
            copia.setId(4L);
            copia.setTipoRegistro(TipoRegistro.INICIATIVA);
            copia.setEstado(EstadoIniciativa.PRESENTADO);
            copia.setVersion(versionCompartida.get());
            return java.util.Optional.of(copia);
        });
        when(registroRepository.save(any(RegistroPortafolioEntity.class))).thenAnswer(invocation -> {
            RegistroPortafolioEntity e = invocation.getArgument(0);
            e.setVersion(versionCompartida.incrementAndGet());
            return e;
        });
        when(transicionRepository.save(any(TransicionEstadoEntity.class)))
                .thenAnswer(invocation -> {
                    TransicionEstadoEntity t = invocation.getArgument(0);
                    if (t.getId() == null) {
                        t.setId(1L);
                    }
                    return t;
                });

        TransicionCommand cmd =
                new TransicionCommand(
                        EstadoIniciativa.NO_ADMISIBLE, "obs", 1L, "4-0");
        TransicionDetail detalle =
                service.transicionar(4L, cmd, contextoEvaluador(), "k-1", "{}");

        assertNotNull(detalle);
        // Una sola transicion persistida: la transicion confirmada es
        // append-only y el registro previo no se borra.
        org.mockito.Mockito.verify(transicionRepository, org.mockito.Mockito.times(1))
                .save(any(TransicionEstadoEntity.class));
    }
}

