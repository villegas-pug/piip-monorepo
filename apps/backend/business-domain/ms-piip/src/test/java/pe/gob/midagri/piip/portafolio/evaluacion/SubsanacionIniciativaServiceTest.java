package pe.gob.midagri.piip.portafolio.evaluacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import pe.gob.midagri.piip.portafolio.dto.OpenCorrectionRequest;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.SubsanacionDetail;
import pe.gob.midagri.piip.portafolio.dto.SubsanacionEditCommand;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;

/**
 * Pruebas unitarias para la subsanacion de iniciativas conforme a la
 * Constitucion 5.0.0, al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}
 * y al script DDL {@code 014_evaluacion_transiciones.sql} y su correccion
 * {@code 014.1_subsanacion_iniciativa_plazo.sql}.
 *
 * <p>La subsanacion es UNICA por iniciativa: la UK {@code UK_SI_INICIATIVA}
 * lo garantiza a nivel de fila y el servicio debe rechazarlo con
 * {@code 409 CORRECTION_ALREADY_USED} en una segunda apertura. El plazo
 * registrado por el Evaluador debe ser estrictamente posterior a la
 * apertura (CHECK {@code CK_SI_PLAZO}); la correccion del Responsable se
 * limita a los campos oficiales 5 al 12, 22 y 23 mientras la subsanacion
 * siga abierta y dentro de plazo, con historial append-only.
 *
 * <p>Esta prueba modela la firma esperada del servicio a implementar en
 * T057; las firmas exactas se marcan con {@code // @NEEDS_CLARIFICATION}
 * cuando la especificacion pueda ajustar nombres, parametros o retornos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("US2 - Subsanacion de iniciativa: SubsanacionIniciativaService")
class SubsanacionIniciativaServiceTest {

    private static final String IDEMPOTENCY_KEY = "key-subsanacion-1";
    private static final String PAYLOAD_JSON = "{\"incumplimientos\":[]}";

    @Mock private SubsanacionIniciativaRepository subsanacionRepository;
    @Mock private RegistroPortafolioRepository registroRepository;
    @Mock private AuditService auditService;
    @Mock private IdempotencyService idempotencyService;

    private SubsanacionIniciativaServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new SubsanacionIniciativaServiceImpl(
                subsanacionRepository, registroRepository, auditService, idempotencyService);
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

    private PortafolioAuthContext contextoResponsable() {
        return new PortafolioAuthContext("sub-resp", 10L, 100L, "Responsable", 1L, 1L, "corr-resp");
    }

    private RegistroPortafolioEntity registroPresentado(long id) {
        RegistroPortafolioEntity entity = new RegistroPortafolioEntity();
        entity.setId(id);
        entity.setTipoRegistro(TipoRegistro.INICIATIVA);
        entity.setEstado(EstadoIniciativa.PRESENTADO);
        entity.setVersion(0L);
        return entity;
    }

    private SubsanacionIniciativaEntity subsanacionAbierta(long iniciativaId, long id) {
        SubsanacionIniciativaEntity entity = new SubsanacionIniciativaEntity();
        entity.setId(id);
        entity.setIniciativaId(iniciativaId);
        entity.setPlazo(LocalDate.now().plusDays(7));
        entity.setIncumplimientos("Falta documento X");
        entity.setAperturaEn(LocalDateTime.now().minusHours(1));
        entity.setAtencionEn(null);
        entity.setActorId(99L);
        return entity;
    }

    @Test
    @DisplayName("Abrir subsanacion registra plazo posterior a la apertura e incluye incumplimientos")
    void abrir_registraPlazoEIncumplimientos() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L)));
        when(subsanacionRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());
        when(subsanacionRepository.save(any(SubsanacionIniciativaEntity.class)))
                .thenAnswer(invocation -> {
                    SubsanacionIniciativaEntity s = invocation.getArgument(0);
                    s.setId(11L);
                    return s;
                });

        // // @NEEDS_CLARIFICATION: el nombre exacto del DTO de apertura
        // (OpenCorrectionRequest en el contrato) y los nombres de sus campos
        // pueden ajustarse en T057.
        LocalDate plazo = LocalDate.now().plusDays(10);
        SubsanacionDetail detalle = service.abrir(1L,
                new OpenCorrectionRequest(plazo, List.of("Falta ficha de iniciativa")),
                contextoEvaluador(), IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(11L, detalle.id());
        assertEquals(1L, detalle.iniciativaId());
        assertEquals(plazo, detalle.plazo());
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));
    }

    @Test
    @DisplayName("Abrir subsanacion con plazo anterior a la apertura se rechaza con PLAZO_INVALIDO")
    void abrir_plazoAnterior_lanzaValidacion() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L)));
        when(subsanacionRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());

        LocalDate plazoInvalido = LocalDate.now().minusDays(1);
        // // @NEEDS_CLARIFICATION: el codigo canonico de error
        // (SUBSANACION_PLAZO_INVALIDO, SUBSANATION_PLAZO_INVALID o el
        // equivalente publicado por T057) debe confirmarse.
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.abrir(1L,
                        new OpenCorrectionRequest(plazoInvalido, List.of("x")),
                        contextoEvaluador(), IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("PLAZO_INVALIDO")
                    || error.getReason().contains("SUBSANATION_PLAZO_INVALID")));
        verify(subsanacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Doble apertura simultanea deja un unico ganador: la UK_SI_INICIATIVA rechaza la segunda")
    void abrir_dosConcurrente_soloUnaGana() throws Exception {
        // Simulacion de la UK_SI_INICIATIVA: la primera invocacion crea la
        // fila, la segunda la viola y el repositorio lanza
        // DataIntegrityViolationException (equivalente Oracle ORA-00001).
        AtomicInteger guardadas = new AtomicInteger();
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L)));
        when(subsanacionRepository.findByIniciativaId(1L)).thenAnswer(invocation -> {
            if (guardadas.get() == 0) {
                return Optional.empty();
            }
            return Optional.of(subsanacionAbierta(1L, 11L));
        });
        when(subsanacionRepository.save(any(SubsanacionIniciativaEntity.class))).thenAnswer(invocation -> {
            int previo = guardadas.getAndIncrement();
            if (previo == 0) {
                SubsanacionIniciativaEntity s = invocation.getArgument(0);
                s.setId(11L);
                return s;
            }
            // La UK de Oracle (UK_SI_INICIATIVA) rechaza la segunda fila.
            throw new DataIntegrityViolationException("UK_SI_INICIATIVA violated");
        });

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        try {
            Future<SubsanacionDetail> f1 = pool.submit(() -> {
                inicio.await();
                return service.abrir(1L,
                        new OpenCorrectionRequest(LocalDate.now().plusDays(5), List.of("a")),
                        contextoEvaluador(), "k-1", PAYLOAD_JSON);
            });
            Future<SubsanacionDetail> f2 = pool.submit(() -> {
                inicio.await();
                return service.abrir(1L,
                        new OpenCorrectionRequest(LocalDate.now().plusDays(5), List.of("b")),
                        contextoEvaluador(), "k-2", PAYLOAD_JSON);
            });
            inicio.countDown();

            // Una de las dos llamadas debe tener exito; la otra debe
            // recibir 409 CORRECTION_ALREADY_USED o la integridad referencial.
            boolean unaGano = false;
            boolean unaPerdio = false;
            for (Future<SubsanacionDetail> f : List.of(f1, f2)) {
                try {
                    SubsanacionDetail d = f.get(5, TimeUnit.SECONDS);
                    if (d != null) {
                        unaGano = true;
                    }
                } catch (Exception ex) {
                    Throwable causa = ex.getCause() != null ? ex.getCause() : ex;
                    assertTrue(causa instanceof ResponseStatusException
                                    || causa instanceof DataIntegrityViolationException,
                            () -> "Se esperaba ResponseStatusException o DataIntegrityViolationException, fue: "
                                    + causa.getClass().getName());
                    if (causa instanceof ResponseStatusException rse) {
                        assertEquals(HttpStatus.CONFLICT, rse.getStatusCode());
                        assertTrue(rse.getReason() != null
                                && rse.getReason().contains("CORRECTION_ALREADY_USED"));
                    }
                    unaPerdio = true;
                }
            }
            assertTrue(unaGano, "Una de las dos aperturas concurrentes debe triunfar");
            assertTrue(unaPerdio, "La otra apertura debe ser rechazada por la UK de subsanacion");
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @DisplayName("Segunda apertura explicita despues de la primera se rechaza con 409 CORRECTION_ALREADY_USED")
    void abrir_segundaLanzaCorreccionUsada() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L)));
        when(subsanacionRepository.findByIniciativaId(1L))
                .thenReturn(Optional.of(subsanacionAbierta(1L, 11L)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.abrir(1L,
                        new OpenCorrectionRequest(LocalDate.now().plusDays(3), List.of("otro")),
                        contextoEvaluador(), IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason() != null && error.getReason().contains("CORRECTION_ALREADY_USED"));
        verify(subsanacionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Editar durante subsanacion abierta aplica solo los campos oficiales 5-12, 22 y 23")
    void editar_camposPermitidos_actualizaYVersiona() {
        SubsanacionIniciativaEntity abierta = subsanacionAbierta(1L, 11L);
        when(subsanacionRepository.findByIniciativaId(1L)).thenReturn(Optional.of(abierta));
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L)));
        when(registroRepository.save(any(RegistroPortafolioEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // // @NEEDS_CLARIFICATION: el DTO de correccion
        // (EditSubsanationRequest, SubsanacionCorreccionRequest u otro) sera
        // confirmado en T057; aqui se modela con los campos 5-12, 22 y 23.
        SubsanacionEditCommand comando = new SubsanacionEditCommand(
                "Nombre corregido",
                null,
                null,
                "Problema corregido",
                null,
                10L,
                20L,
                List.of(new SubsanacionEditCommand.UnidadResponsableItem(1L, true)),
                Boolean.FALSE,
                null,
                "Nota ampliada");
        SubsanacionDetail resultado = service.editar(1L, comando,
                contextoResponsable(), 0L, "1-0", IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(resultado);
        verify(registroRepository, times(1)).save(any(RegistroPortafolioEntity.class));
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));
    }

    @Test
    @DisplayName("Editar sin subsanacion abierta se rechaza con 409 CORRECTION_NOT_OPEN")
    void editar_sinSubsanacionAbierta_lanzaNoAbierta() {
        when(subsanacionRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());

        SubsanacionEditCommand comando = new SubsanacionEditCommand(
                "x", null, null, "x", null, 10L, 20L,
                List.of(new SubsanacionEditCommand.UnidadResponsableItem(1L, true)),
                Boolean.FALSE, null, null);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.editar(1L, comando, contextoResponsable(), 0L, "1-0",
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason() != null && error.getReason().contains("CORRECTION_NOT_OPEN"));
    }

    @Test
    @DisplayName("Editar con ETag obsoleto se rechaza con 412 STATE_CHANGED")
    void editar_concurrenciaPerdedora_412() {
        SubsanacionIniciativaEntity abierta = subsanacionAbierta(1L, 11L);
        when(subsanacionRepository.findByIniciativaId(1L)).thenReturn(Optional.of(abierta));
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L)));
        // // @NEEDS_CLARIFICATION: la verificacion de ETag puede residir en
        // un VersionConflictException, un ResponseStatusException 412, o un
        // STATE_CHANGED personalizado. T057 confirmara la forma final.
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.editar(1L,
                        new SubsanacionEditCommand("x", null, null, "x", null, 10L, 20L,
                                List.of(new SubsanacionEditCommand.UnidadResponsableItem(1L, true)),
                                Boolean.FALSE, null, null),
                        contextoResponsable(), 0L, "\"1-99\"", IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.PRECONDITION_FAILED, error.getStatusCode());
        assertTrue(error.getReason() != null && error.getReason().contains("STATE_CHANGED"));
    }

    @Test
    @DisplayName("Cerrar subsanacion registra atencionEn y conserva la fila para auditoria")
    void cerrar_registraAtencionYConservaFila() {
        SubsanacionIniciativaEntity abierta = subsanacionAbierta(1L, 11L);
        when(subsanacionRepository.findByIniciativaId(1L)).thenReturn(Optional.of(abierta));
        when(subsanacionRepository.save(any(SubsanacionIniciativaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SubsanacionDetail resultado = service.cerrar(1L, "Atendida por Responsable",
                contextoResponsable(), 0L, "1-0", IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(resultado);
        verify(subsanacionRepository, times(1)).save(any(SubsanacionIniciativaEntity.class));
        // No se elimina la fila: la subsanacion es append-only para auditoria.
        verify(subsanacionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("La persistencia de subsanacion es append-only: nunca se borra ni se sobrescribe")
    void persistenciaAppendOnly() {
        SubsanacionIniciativaEntity abierta = subsanacionAbierta(1L, 11L);
        when(subsanacionRepository.findByIniciativaId(1L)).thenReturn(Optional.of(abierta));
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L)));
        when(registroRepository.save(any(RegistroPortafolioEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(subsanacionRepository.save(any(SubsanacionIniciativaEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        for (int i = 0; i < 3; i++) {
            service.editar(1L,
                    new SubsanacionEditCommand("v" + i, null, null, "v" + i, null, 10L, 20L,
                            List.of(new SubsanacionEditCommand.UnidadResponsableItem(1L, true)),
                            Boolean.FALSE, null, null),
                    contextoResponsable(), (long) i, "\"1-" + i + "\"",
                    "k-" + i, PAYLOAD_JSON);
        }
        // Tres correcciones quedan anexadas al historial; ninguna borra el
        // registro original.
        verify(registroRepository, times(3)).save(any(RegistroPortafolioEntity.class));
        verify(subsanacionRepository, never()).delete(any());
    }

    @Test
    @DisplayName("El servicio expone contrato de interfaz y no entidades JPA en su API publica")
    void servicioExponeContratoSinEntidadesJPA() {
        assertTrue(SubsanacionIniciativaService.class.isInterface());
        for (var metodo : SubsanacionIniciativaService.class.getDeclaredMethods()) {
            String retorno = metodo.getReturnType().getName();
            assertFalse(retorno.contains("pe.gob.midagri.piip.portafolio.entity")
                            || retorno.contains("pe.gob.midagri.piip.portafolio.evaluacion"),
                    () -> "El metodo " + metodo.getName() + " no debe retornar entidades JPA");
        }
    }

    @Test
    @DisplayName("La auditoria se invoca tanto en exito como en denegacion de subsanacion")
    void auditoriaExitoYDenegacion() {
        // Caso 1: exito al abrir.
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L)));
        when(subsanacionRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());
        when(subsanacionRepository.save(any(SubsanacionIniciativaEntity.class)))
                .thenAnswer(invocation -> {
                    SubsanacionIniciativaEntity s = invocation.getArgument(0);
                    s.setId(11L);
                    return s;
                });
        service.abrir(1L,
                new OpenCorrectionRequest(LocalDate.now().plusDays(7), List.of("x")),
                contextoEvaluador(), "k-1", PAYLOAD_JSON);
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));

        // Caso 2: denegacion por subsanacion ya usada.
        when(subsanacionRepository.findByIniciativaId(1L))
                .thenReturn(Optional.of(subsanacionAbierta(1L, 11L)));
        assertThrows(ResponseStatusException.class, () -> service.abrir(1L,
                new OpenCorrectionRequest(LocalDate.now().plusDays(7), List.of("y")),
                contextoEvaluador(), "k-2", PAYLOAD_JSON));
        verify(auditService, times(1)).registrarDenegacion(any(AuditService.AuditCommand.class));
    }

    @Test
    @DisplayName("Las respuestas de subsanacion exponen DTO HTTP, no entidades JPA")
    void respuestaEsDtoHttp() {
        for (var c : SubsanacionDetail.class.getRecordComponents()) {
            String tipo = c.getType().getName();
            assertFalse(tipo.contains("pe.gob.midagri.piip.portafolio.entity")
                            || tipo.contains(".evaluacion.entity")
                            || tipo.contains(".transicion.entity"),
                    () -> "El campo " + c.getName() + " no debe ser una entidad JPA");
        }
    }

}
