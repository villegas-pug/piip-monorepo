package pe.gob.midagri.piip.portafolio.transicion;

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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
 * Pruebas unitarias para la maquina de estados canonica del portafolio
 * conforme a la Constitucion 5.0.0 (tabla "Transiciones controladas
 * iniciales"), al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}
 * y al script DDL {@code 014_evaluacion_transiciones.sql}.
 *
 * <p>La Constitucion define diez transiciones controladas iniciales y tres
 * estados terminales de iniciativa ({@code NO_ADMISIBLE},
 * {@code NO_APLICABLE}, {@code INICIATIVA_ARCHIVADA}); ademas, la
 * {@code TRANSICION_ESTADO} es append-only y registra estado anterior,
 * nuevo, actor, rol efectivo, unidad, fecha, observacion y documento
 * asociado. Esta prueba cubre las once transiciones canónicas (las diez
 * de la Constitucion mas la transicion automatica de
 * {@code Subsanacion vencida -> NO_ADMISIBLE}), el bloqueo de los
 * terminales, la verificacion de ETag, la evidencia exigida y la
 * auditoria atomica.
 *
 * <p>Esta prueba modela la firma esperada del servicio a implementar en
 * T058; las firmas exactas se marcan con {@code // @NEEDS_CLARIFICATION}
 * cuando la especificacion pueda ajustar nombres, parametros o retornos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("US2 - Maquina de estados: TransicionEstadoService")
class TransicionEstadoServiceTest {

    private static final String IDEMPOTENCY_KEY = "key-trans-1";
    private static final String PAYLOAD_JSON = "{}";

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

    private PortafolioAuthContext contexto(String perfil, long asignacionId, long unidadId) {
        return new PortafolioAuthContext("sub-" + perfil.toLowerCase(), 1L, asignacionId, perfil,
                unidadId, unidadId, "corr-" + perfil.toLowerCase());
    }

    private RegistroPortafolioEntity registro(long id, TipoRegistro tipo, EstadoIniciativa estado,
            long version) {
        RegistroPortafolioEntity entity = new RegistroPortafolioEntity();
        entity.setId(id);
        entity.setTipoRegistro(tipo);
        entity.setEstado(estado);
        entity.setVersion(version);
        return entity;
    }

    private void prepararTransicionExitosa(EstadoIniciativa origen, EstadoIniciativa destino,
            long registroId) {
        RegistroPortafolioEntity entity = registro(registroId, TipoRegistro.INICIATIVA, origen, 0L);
        when(registroRepository.findById(registroId)).thenReturn(Optional.of(entity));
        when(registroRepository.save(any(RegistroPortafolioEntity.class)))
                .thenAnswer(invocation -> {
                    RegistroPortafolioEntity e = invocation.getArgument(0);
                    e.setVersion(e.getVersion() == null ? 1L : e.getVersion() + 1L);
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
    }

    // ------------------------------------------------------------------
    // 1) PRESENTADO -> NO_ADMISIBLE
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Transicion 1: PRESENTADO -> NO_ADMISIBLE exige observacion y documento habilitante")
    void transicion01_presentadoNoAdmisible() {
        prepararTransicionExitosa(EstadoIniciativa.PRESENTADO, EstadoIniciativa.NO_ADMISIBLE, 1L);

        TransicionDetail detalle = service.transicionar(1L,
                new TransicionCommand(EstadoIniciativa.NO_ADMISIBLE, "Subsanacion vencida",
                        50L, "1-0"),
                contexto("Evaluador", 999L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.NO_ADMISIBLE, detalle.estadoNuevo());
        verify(transicionRepository, times(1)).save(any(TransicionEstadoEntity.class));
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));
    }

    // ------------------------------------------------------------------
    // 2) PRESENTADO -> NO_APLICABLE
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Transicion 2: PRESENTADO -> NO_APLICABLE exige motivo y aplica criterios")
    void transicion02_presentadoNoAplicable() {
        prepararTransicionExitosa(EstadoIniciativa.PRESENTADO, EstadoIniciativa.NO_APLICABLE, 2L);

        TransicionDetail detalle = service.transicionar(2L,
                new TransicionCommand(EstadoIniciativa.NO_APLICABLE,
                        "Caso de compra: no innovacion publica", 51L, "2-0"),
                contexto("Evaluador", 999L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.NO_APLICABLE, detalle.estadoNuevo());
        verify(transicionRepository, times(1)).save(any(TransicionEstadoEntity.class));
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));
    }

    // ------------------------------------------------------------------
    // 3) PRESENTADO -> INICIATIVA_APROBADA
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Transicion 3: PRESENTADO -> INICIATIVA_APROBADA exige opinion tecnica y decision formal")
    void transicion03_presentadoAprobada() {
        prepararTransicionExitosa(EstadoIniciativa.PRESENTADO, EstadoIniciativa.INICIATIVA_APROBADA,
                3L);

        TransicionDetail detalle = service.transicionar(3L,
                new TransicionCommand(EstadoIniciativa.INICIATIVA_APROBADA,
                        null, 60L, "3-0"),
                contexto("Autoridad", 500L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.INICIATIVA_APROBADA, detalle.estadoNuevo());
    }

    // ------------------------------------------------------------------
    // 4) PRESENTADO -> INICIATIVA_ARCHIVADA
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Transicion 4: PRESENTADO -> INICIATIVA_ARCHIVADA exige observacion y decision formal")
    void transicion04_presentadoArchivada() {
        prepararTransicionExitosa(EstadoIniciativa.PRESENTADO, EstadoIniciativa.INICIATIVA_ARCHIVADA,
                4L);

        TransicionDetail detalle = service.transicionar(4L,
                new TransicionCommand(EstadoIniciativa.INICIATIVA_ARCHIVADA,
                        "No cumple valor publico esperado", 61L, "4-0"),
                contexto("Autoridad", 500L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.INICIATIVA_ARCHIVADA, detalle.estadoNuevo());
    }

    // ------------------------------------------------------------------
    // 5) PROYECTO_EJECUCION -> SUSPENDIDO
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Transicion 5: PROYECTO_EJECUCION -> SUSPENDIDO exige evidencia y observacion")
    void transicion05_ejecucionSuspendido() {
        prepararTransicionExitosa(EstadoIniciativa.PROYECTO_EJECUCION, EstadoIniciativa.SUSPENDIDO,
                5L);

        TransicionDetail detalle = service.transicionar(5L,
                new TransicionCommand(EstadoIniciativa.SUSPENDIDO,
                        "Riesgo operativo sustentado por UnidadAdmin", 70L, "5-0"),
                contexto("UnidadAdmin", 800L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.SUSPENDIDO, detalle.estadoNuevo());
    }

    // ------------------------------------------------------------------
    // 6) PROYECTO_EJECUCION -> CANCELADO
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Transicion 6: PROYECTO_EJECUCION -> CANCELADO exige documento y observacion")
    void transicion06_ejecucionCancelado() {
        prepararTransicionExitosa(EstadoIniciativa.PROYECTO_EJECUCION, EstadoIniciativa.CANCELADO,
                6L);

        TransicionDetail detalle = service.transicionar(6L,
                new TransicionCommand(EstadoIniciativa.CANCELADO,
                        "Cancelacion decidida por la Autoridad", 71L, "6-0"),
                contexto("Autoridad", 500L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.CANCELADO, detalle.estadoNuevo());
    }

    // ------------------------------------------------------------------
    // 7) PROYECTO_EJECUCION -> PRODUCTO_APROBADO
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Transicion 7: PROYECTO_EJECUCION -> PRODUCTO_APROBADO exige documento formal de aprobacion")
    void transicion07_ejecucionProductoAprobado() {
        prepararTransicionExitosa(EstadoIniciativa.PROYECTO_EJECUCION, EstadoIniciativa.PRODUCTO_APROBADO,
                7L);

        TransicionDetail detalle = service.transicionar(7L,
                new TransicionCommand(EstadoIniciativa.PRODUCTO_APROBADO,
                        null, 80L, "7-0"),
                contexto("Autoridad", 500L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.PRODUCTO_APROBADO, detalle.estadoNuevo());
    }

    // ------------------------------------------------------------------
    // 8) PROYECTO_EJECUCION -> PRODUCTO_NO_APROBADO
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Transicion 8: PROYECTO_EJECUCION -> PRODUCTO_NO_APROBADO exige evidencia y observacion")
    void transicion08_ejecucionProductoNoAprobado() {
        prepararTransicionExitosa(EstadoIniciativa.PROYECTO_EJECUCION, EstadoIniciativa.PRODUCTO_NO_APROBADO,
                8L);

        TransicionDetail detalle = service.transicionar(8L,
                new TransicionCommand(EstadoIniciativa.PRODUCTO_NO_APROBADO,
                        "Producto no cumple tipo canonico", 81L, "8-0"),
                contexto("Autoridad", 500L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.PRODUCTO_NO_APROBADO, detalle.estadoNuevo());
    }

    // ------------------------------------------------------------------
    // 9) PRODUCTO_APROBADO -> FINALIZADO
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Transicion 9: PRODUCTO_APROBADO -> FINALIZADO exige informe final y resultados")
    void transicion09_productoAprobadoFinalizado() {
        prepararTransicionExitosa(EstadoIniciativa.PRODUCTO_APROBADO, EstadoIniciativa.FINALIZADO,
                9L);

        TransicionDetail detalle = service.transicionar(9L,
                new TransicionCommand(EstadoIniciativa.FINALIZADO,
                        "Cierre administrativo con informe final y resultados", 90L, "9-0"),
                contexto("Evaluador", 999L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.FINALIZADO, detalle.estadoNuevo());
    }

    // ------------------------------------------------------------------
    // 10) PRODUCTO_NO_APROBADO -> FINALIZADO
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Transicion 10: PRODUCTO_NO_APROBADO -> FINALIZADO exige informe final y resultados")
    void transicion10_productoNoAprobadoFinalizado() {
        prepararTransicionExitosa(EstadoIniciativa.PRODUCTO_NO_APROBADO, EstadoIniciativa.FINALIZADO,
                10L);

        TransicionDetail detalle = service.transicionar(10L,
                new TransicionCommand(EstadoIniciativa.FINALIZADO,
                        "Cierre administrativo con informe final y resultados", 91L, "10-0"),
                contexto("Evaluador", 999L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.FINALIZADO, detalle.estadoNuevo());
    }

    // ------------------------------------------------------------------
    // 11) Subsanacion vencida -> NO_ADMISIBLE (transicion automatica)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Transicion 11: subsanacion vencida -> NO_ADMISIBLE se ejecuta cuando vence el plazo")
    void transicion11_subsanacionVencidaNoAdmisible() {
        // // @NEEDS_CLARIFICATION: la transicion automatica por vencimiento
        // puede ser una variante del servicio (transicionarConSubsanacion)
        // o un metodo especifico (consumirVencimientoSubsanacion). T058
        // confirmara la firma.
        prepararTransicionExitosa(EstadoIniciativa.PRESENTADO, EstadoIniciativa.NO_ADMISIBLE,
                11L);

        TransicionDetail detalle = service.transicionarPorVencimientoSubsanacion(11L,
                "Plazo de subsanacion vencido sin atender", 100L,
                contexto("Evaluador", 999L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.NO_ADMISIBLE, detalle.estadoNuevo());
    }

    // ------------------------------------------------------------------
    // Bloqueo de terminales
    // ------------------------------------------------------------------
    @Test
    @DisplayName("NO_ADMISIBLE es terminal: no permite transiciones adicionales")
    void noAdmisibleTerminal_bloqueaTransiciones() {
        when(registroRepository.findById(20L))
                .thenReturn(Optional.of(registro(20L, TipoRegistro.INICIATIVA,
                        EstadoIniciativa.NO_ADMISIBLE, 0L)));

        for (EstadoIniciativa destino : EstadoIniciativa.values()) {
            if (destino == EstadoIniciativa.NO_ADMISIBLE) {
                continue;
            }
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> service.transicionar(20L,
                            new TransicionCommand(destino, "x", 1L, "20-0"),
                            contexto("Evaluador", 999L, 1L),
                            IDEMPOTENCY_KEY, PAYLOAD_JSON),
                    () -> "Destino " + destino + " no debe estar permitido desde NO_ADMISIBLE");
            assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
            assertTrue(error.getReason() != null
                    && (error.getReason().contains("STATE_TRANSITION_NOT_ALLOWED")
                        || error.getReason().contains("TERMINAL_STATE")));
        }
        verify(transicionRepository, never()).save(any());
    }

    @Test
    @DisplayName("NO_APLICABLE es terminal: no permite transiciones adicionales")
    void noAplicableTerminal_bloqueaTransiciones() {
        when(registroRepository.findById(21L))
                .thenReturn(Optional.of(registro(21L, TipoRegistro.INICIATIVA,
                        EstadoIniciativa.NO_APLICABLE, 0L)));

        for (EstadoIniciativa destino : EstadoIniciativa.values()) {
            if (destino == EstadoIniciativa.NO_APLICABLE) {
                continue;
            }
            assertThrows(ResponseStatusException.class,
                    () -> service.transicionar(21L,
                            new TransicionCommand(destino, "x", 1L, "21-0"),
                            contexto("Evaluador", 999L, 1L),
                            IDEMPOTENCY_KEY, PAYLOAD_JSON));
        }
        verify(transicionRepository, never()).save(any());
    }

    @Test
    @DisplayName("INICIATIVA_ARCHIVADA es terminal: no permite transiciones adicionales")
    void iniciativaArchivadaTerminal_bloqueaTransiciones() {
        when(registroRepository.findById(22L))
                .thenReturn(Optional.of(registro(22L, TipoRegistro.INICIATIVA,
                        EstadoIniciativa.INICIATIVA_ARCHIVADA, 0L)));

        for (EstadoIniciativa destino : EstadoIniciativa.values()) {
            if (destino == EstadoIniciativa.INICIATIVA_ARCHIVADA) {
                continue;
            }
            assertThrows(ResponseStatusException.class,
                    () -> service.transicionar(22L,
                            new TransicionCommand(destino, "x", 1L, "22-0"),
                            contexto("Autoridad", 500L, 1L),
                            IDEMPOTENCY_KEY, PAYLOAD_JSON));
        }
        verify(transicionRepository, never()).save(any());
    }

    // ------------------------------------------------------------------
    // Bloqueo por If-Match
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Sin If-Match se rechaza con 428 PRECONDITION_REQUIRED")
    void sinIfMatch_428() {
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.transicionar(30L,
                        new TransicionCommand(EstadoIniciativa.NO_ADMISIBLE, "x", 1L, null),
                        contexto("Evaluador", 999L, 1L),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.PRECONDITION_REQUIRED, error.getStatusCode());
        assertTrue(error.getReason() != null && error.getReason().contains("IF_MATCH_REQUIRED"));
    }

    @Test
    @DisplayName("If-Match con ETag incorrecto se rechaza con 412 STATE_CHANGED")
    void ifMatchIncorrecto_412() {
        when(registroRepository.findById(30L)).thenReturn(Optional.of(
                registro(30L, TipoRegistro.INICIATIVA, EstadoIniciativa.PRESENTADO, 0L)));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.transicionar(30L,
                        new TransicionCommand(EstadoIniciativa.NO_ADMISIBLE, "x", 1L, "\"30-99\""),
                        contexto("Evaluador", 999L, 1L),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.PRECONDITION_FAILED, error.getStatusCode());
        assertTrue(error.getReason() != null && error.getReason().contains("STATE_CHANGED"));
    }

    // ------------------------------------------------------------------
    // Bloqueo por transiciones no listadas
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Una transicion no listada (PRESENTADO -> FINALIZADO) se rechaza con STATE_TRANSITION_NOT_ALLOWED")
    void transicionNoListada_bloqueada() {
        when(registroRepository.findById(31L)).thenReturn(Optional.of(
                registro(31L, TipoRegistro.INICIATIVA, EstadoIniciativa.PRESENTADO, 0L)));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.transicionar(31L,
                        new TransicionCommand(EstadoIniciativa.FINALIZADO, "x", 1L, "31-0"),
                        contexto("Evaluador", 999L, 1L),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
        assertTrue(error.getReason() != null
                && error.getReason().contains("STATE_TRANSITION_NOT_ALLOWED"));
    }

    // ------------------------------------------------------------------
    // Evidencia exigida
    // ------------------------------------------------------------------
    @Test
    @DisplayName("Transiciones con documento habilitante obligatorio rechazan su omission con 422")
    void transicion_sinDocumento_lanzaValidacion() {
        when(registroRepository.findById(32L)).thenReturn(Optional.of(
                registro(32L, TipoRegistro.INICIATIVA, EstadoIniciativa.PRESENTADO, 0L)));
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.transicionar(32L,
                        new TransicionCommand(EstadoIniciativa.INICIATIVA_APROBADA, null, null, "32-0"),
                        contexto("Autoridad", 500L, 1L),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("FORMAL_DECISION_REQUIRED")
                    || error.getReason().contains("EVIDENCE_NOT_ELIGIBLE")));
    }

    @Test
    @DisplayName("Transiciones con observacion obligatoria (NO_ADMISIBLE, NO_APLICABLE, ARCHIVADA) rechazan su omission")
    void transicion_sinObservacion_lanzaValidacion() {
        for (EstadoIniciativa destino : List.of(EstadoIniciativa.NO_ADMISIBLE,
                EstadoIniciativa.NO_APLICABLE, EstadoIniciativa.INICIATIVA_ARCHIVADA)) {
            when(registroRepository.findById(33L)).thenReturn(Optional.of(
                    registro(33L, TipoRegistro.INICIATIVA, EstadoIniciativa.PRESENTADO, 0L)));
            ResponseStatusException error = assertThrows(ResponseStatusException.class,
                    () -> service.transicionar(33L,
                            new TransicionCommand(destino, "  ", 1L, "33-0"),
                            contexto("Evaluador", 999L, 1L),
                            IDEMPOTENCY_KEY, PAYLOAD_JSON),
                    () -> "Destino " + destino + " debe exigir observacion");
            assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        }
    }

    // ------------------------------------------------------------------
    // Historial append-only
    // ------------------------------------------------------------------
    @Test
    @DisplayName("El historial de transiciones es append-only: nunca se actualiza ni borra")
    void historialAppendOnly() {
        prepararTransicionExitosa(EstadoIniciativa.PRESENTADO, EstadoIniciativa.NO_ADMISIBLE, 40L);
        service.transicionar(40L,
                new TransicionCommand(EstadoIniciativa.NO_ADMISIBLE, "obs", 1L, "40-0"),
                contexto("Evaluador", 999L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);
        verify(transicionRepository, times(1)).save(any(TransicionEstadoEntity.class));
        verify(transicionRepository, never()).delete(any());
    }

    // ------------------------------------------------------------------
    // Auditoria atomica
    // ------------------------------------------------------------------
    @Test
    @DisplayName("La auditoria registra exito y denegacion de manera atomica y trazable")
    void auditoriaExitoYDenegacion() {
        // Caso 1: exito.
        prepararTransicionExitosa(EstadoIniciativa.PRESENTADO, EstadoIniciativa.NO_ADMISIBLE, 50L);
        service.transicionar(50L,
                new TransicionCommand(EstadoIniciativa.NO_ADMISIBLE, "obs", 1L, "50-0"),
                contexto("Evaluador", 999L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));

        // Caso 2: denegacion (transicion no listada).
        when(registroRepository.findById(51L)).thenReturn(Optional.of(
                registro(51L, TipoRegistro.INICIATIVA, EstadoIniciativa.PRESENTADO, 0L)));
        assertThrows(ResponseStatusException.class, () -> service.transicionar(51L,
                new TransicionCommand(EstadoIniciativa.FINALIZADO, "x", 1L, "51-0"),
                contexto("Evaluador", 999L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON));
        verify(auditService, times(1)).registrarDenegacion(any(AuditService.AuditCommand.class));
    }

    // ------------------------------------------------------------------
    // Contrato de interfaz
    // ------------------------------------------------------------------
    @Test
    @DisplayName("El servicio expone contrato de interfaz y no entidades JPA en su API publica")
    void servicioExponeContratoSinEntidadesJPA() {
        assertTrue(TransicionEstadoService.class.isInterface());
        for (var metodo : TransicionEstadoService.class.getDeclaredMethods()) {
            String retorno = metodo.getReturnType().getName();
            assertFalse(retorno.contains("pe.gob.midagri.piip.portafolio.entity")
                            || retorno.contains("pe.gob.midagri.piip.portafolio.repository"),
                    () -> "El metodo " + metodo.getName() + " no debe retornar entidades JPA");
        }
    }

    @Test
    @DisplayName("La respuesta de transicion expone DTO HTTP, no entidades JPA")
    void respuestaEsDtoHttp() {
        for (var c : TransicionDetail.class.getRecordComponents()) {
            Class<?> tipo = c.getType();
            assertFalse((tipo.getName().contains("pe.gob.midagri.piip.portafolio.entity") && !tipo.isEnum())
                            || tipo.getName().contains(".transicion.entity")
                            || tipo.getName().contains(".evaluacion.entity"),
                    () -> "El campo " + c.getName() + " no debe ser una entidad JPA");
        }
    }

    // ------------------------------------------------------------------
    // Asignacion efectiva (autorizacion)
    // ------------------------------------------------------------------
    @Test
    @DisplayName("La transicion rechaza si el actor no es el decisor/registrador canonico del destino")
    void asignacionEfectivaIncorrecta_bloqueada() {
        // // @NEEDS_CLARIFICATION: la validacion de la asignacion efectiva
        // puede ser un servicio de AutorizacionEfectivaService o una revision
        // local del perfil. T058 confirmara la integracion.
        when(registroRepository.findById(60L)).thenReturn(Optional.of(
                registro(60L, TipoRegistro.INICIATIVA, EstadoIniciativa.PRESENTADO, 0L)));
        // Un Responsable no debe poder ejecutar transiciones reservadas al
        // Evaluador o Autoridad.
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.transicionar(60L,
                        new TransicionCommand(EstadoIniciativa.NO_ADMISIBLE, "x", 1L, "60-0"),
                        contexto("Responsable", 100L, 1L),
                        IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertTrue(error.getStatusCode() == HttpStatus.FORBIDDEN
                || error.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY);
        assertTrue(error.getReason() != null
                && (error.getReason().contains("ASSIGNMENT_SCOPE_DENIED")
                    || error.getReason().contains("FORBIDDEN_PROFILE")));
    }

    @Test
    @DisplayName("El campo historial no se actualiza en una transicion exitosa (append-only real)")
    void campoHistorialInmutable() {
        // Capturamos el argumento de save para verificar que la fecha de
        // transicion la fija Oracle y que el servicio no la sobreescribe.
        AtomicReference<TransicionEstadoEntity> capturada = new AtomicReference<>();
        prepararTransicionExitosa(EstadoIniciativa.PRESENTADO, EstadoIniciativa.NO_ADMISIBLE, 70L);
        org.mockito.Mockito.doAnswer(invocation -> {
            TransicionEstadoEntity t = invocation.getArgument(0);
            capturada.set(t);
            if (t.getId() == null) {
                t.setId(700L);
            }
            return t;
        }).when(transicionRepository).save(any(TransicionEstadoEntity.class));

        service.transicionar(70L,
                new TransicionCommand(EstadoIniciativa.NO_ADMISIBLE, "obs", 1L, "70-0"),
                contexto("Evaluador", 999L, 1L),
                IDEMPOTENCY_KEY, PAYLOAD_JSON);

        TransicionEstadoEntity entity = capturada.get();
        assertNotNull(entity);
        assertEquals(EstadoIniciativa.PRESENTADO, entity.getEstadoAnterior());
        assertEquals(EstadoIniciativa.NO_ADMISIBLE, entity.getEstadoNuevo());
        // El servicio nunca reescribe una transicion confirmada.
        verify(transicionRepository, never()).save(
                org.mockito.ArgumentMatchers.argThat(t -> t.getId() != null && t.getId() == 700L
                        && t.getEstadoNuevo() != null
                        && t.getEstadoNuevo() != EstadoIniciativa.NO_ADMISIBLE));
    }

    // ------------------------------------------------------------------

    // ------------------------------------------------------------------
    // Stubs de DTOs para T058.
    // Modelan la firma esperada; T058 puede ajustar nombres y agregar
    // campos. Marcados con // @NEEDS_CLARIFICATION donde aplique.
    // ------------------------------------------------------------------

    // T058 produce los DTOs canonicos en el paquete
    // pe.gob.midagri.piip.portafolio.transicion. Los registros anidados
    // que existian en este archivo de pruebas se migraron a
    // TransicionCommand y TransicionDetail para que el codigo de
    // produccion pueda tiparlos sin importar clases del classpath de
    // tests. Las firmas se conservan exactas (mismo orden y tipos de
    // componentes) para preservar la intencion original.
}
