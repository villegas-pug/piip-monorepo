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

import java.time.LocalDateTime;
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
import pe.gob.midagri.piip.portafolio.dto.AdmissibilityRequest;
import pe.gob.midagri.piip.portafolio.dto.ApplicabilityCriterion;
import pe.gob.midagri.piip.portafolio.dto.ApplicabilityRequest;
import pe.gob.midagri.piip.portafolio.dto.EvaluacionDetail;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.TechnicalOpinionRequest;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;

/**
 * Pruebas unitarias para la evaluacion de iniciativas (admisibilidad y
 * aplicabilidad) conforme a la Constitucion 5.0.0, al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}
 * y al script DDL {@code 014_evaluacion_transiciones.sql}.
 *
 * <p>La evaluacion distingue dos operaciones independientes y NO
 * intercambiables:
 * <ul>
 *   <li><b>Admisibilidad</b>: verifica requisitos formales, ficha, asignacion
 *       del Responsable, cardinalidades y duplicados. Una iniciativa con
 *       incumplimientos formales debe pasar a subsanacion unica; agotada la
 *       subsanacion, la iniciativa pasa a {@code NO_ADMISIBLE} con
 *       observacion obligatoria. Decisor y registrador: {@code Evaluador}.</li>
 *   <li><b>Aplicabilidad</b>: aplica la lista estructurada de competencia
 *       institucional, valor publico y caracter innovador; exige motivo
 *       obligatorio cuando el resultado es {@code NO_APLICABLE}. Decisor y
 *       registrador: {@code Evaluador}.</li>
 * </ul>
 *
 * <p>Esta prueba modela la firma esperada del servicio a implementar en
 * T057; las firmas exactas se marcan con {@code // @NEEDS_CLARIFICATION}
 * cuando la especificacion pueda ajustar nombres, parametros o retornos.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("US2 - Evaluacion de iniciativa: EvaluacionIniciativaService")
class EvaluacionIniciativaServiceTest {

    private static final String IDEMPOTENCY_KEY = "key-evaluacion-1";
    private static final String PAYLOAD_JSON = "{}";

    @Mock private EvaluacionIniciativaRepository evaluacionRepository;
    @Mock private AplicabilidadIniciativaRepository aplicabilidadRepository;
    @Mock private SubsanacionIniciativaRepository subsanacionRepository;
    @Mock private RegistroPortafolioRepository registroRepository;
    @Mock private AuditService auditService;
    @Mock private IdempotencyService idempotencyService;

    private EvaluacionIniciativaServiceImpl service;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        service = new EvaluacionIniciativaServiceImpl(
                evaluacionRepository, aplicabilidadRepository, subsanacionRepository,
                registroRepository, auditService, idempotencyService);
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

    private PortafolioAuthContext contextoAutoridad() {
        return new PortafolioAuthContext("sub-aut", 50L, 500L, "Autoridad", 1L, 1L, "corr-aut");
    }

    private RegistroPortafolioEntity registroPresentado(long id, EstadoIniciativa estado) {
        RegistroPortafolioEntity entity = new RegistroPortafolioEntity();
        entity.setId(id);
        entity.setTipoRegistro(TipoRegistro.INICIATIVA);
        entity.setEstado(estado);
        entity.setVersion(0L);
        return entity;
    }

    private EvaluacionIniciativaEntity evaluacionExistente(long iniciativaId, long id) {
        EvaluacionIniciativaEntity entity = new EvaluacionIniciativaEntity();
        entity.setId(id);
        entity.setIniciativaId(iniciativaId);
        entity.setEvaluadorId(99L);
        entity.setRolEfectivoId(3L);
        entity.setUnidadEfectivaId(1L);
        entity.setFechaEvaluacion(LocalDateTime.now().minusDays(1));
        entity.setObservaciones("Observacion previa");
        entity.setDocumentoOpinionId(50L);
        return entity;
    }

    private AplicabilidadIniciativaEntity aplicabilidadExistente(long iniciativaId, long id) {
        AplicabilidadIniciativaEntity entity = new AplicabilidadIniciativaEntity();
        entity.setId(id);
        entity.setIniciativaId(iniciativaId);
        entity.setResultado("APLICABLE");
        entity.setMotivo(null);
        entity.setEvaluadorId(99L);
        entity.setFecha(LocalDateTime.now().minusDays(1));
        return entity;
    }

    @Test
    @DisplayName("Admisibilidad favorable con todos los requisitos formales se registra y NO abre subsanacion")
    void admisibilidad_favorable_seRegistraComoEval() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.PRESENTADO)));
        when(evaluacionRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());
        when(evaluacionRepository.save(any(EvaluacionIniciativaEntity.class)))
                .thenAnswer(invocation -> {
                    EvaluacionIniciativaEntity e = invocation.getArgument(0);
                    e.setId(101L);
                    return e;
                });

        // // @NEEDS_CLARIFICATION: el DTO puede llamarse AdmissibilityRequest
        // o EvaluacionAdmisibilidadRequest; los campos exactos dependen de T057.
        EvaluacionDetail detalle = service.registrarAdmisibilidad(1L,
                new AdmissibilityRequest("ADMITIDA", "Cumple todos los requisitos formales",
                        50L),
                contextoEvaluador(), IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.PRESENTADO, detalle.estadoIniciativa());
        verify(subsanacionRepository, never()).save(any());
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));
    }

    @Test
    @DisplayName("Admisibilidad NO_ADMISIBLE exige observacion obligatoria y se rechaza sin ella")
    void admisibilidad_noAdmisible_sinObservacion_lanzaValidacion() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.PRESENTADO)));
        when(evaluacionRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.registrarAdmisibilidad(1L,
                        new AdmissibilityRequest("NO_ADMISIBLE", "  ", null),
                        contextoEvaluador(), IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("ADMISSIBILITY_INCOMPLETE")
                    || error.getReason().contains("OFFICIAL_FIELD_REQUIRED")));
    }

    @Test
    @DisplayName("Admisibilidad NO_ADMISIBLE transita a NO_ADMISIBLE con observacion y auditoria de denegacion")
    void admisibilidad_noAdmisible_transitaYAudieta() {
        SubsanacionIniciativaEntity subsanacion = new SubsanacionIniciativaEntity();
        subsanacion.setId(11L);
        subsanacion.setIniciativaId(1L);
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.PRESENTADO)));
        when(evaluacionRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());
        when(subsanacionRepository.findByIniciativaId(1L)).thenReturn(Optional.of(subsanacion));
        when(evaluacionRepository.save(any(EvaluacionIniciativaEntity.class)))
                .thenAnswer(invocation -> {
                    EvaluacionIniciativaEntity e = invocation.getArgument(0);
                    e.setId(101L);
                    return e;
                });
        when(registroRepository.save(any(RegistroPortafolioEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EvaluacionDetail detalle = service.registrarAdmisibilidad(1L,
                new AdmissibilityRequest("NO_ADMISIBLE",
                        "Subsanacion vencida sin atender requisitos formales",
                        50L),
                contextoEvaluador(), IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.NO_ADMISIBLE, detalle.estadoIniciativa());
        verify(registroRepository, times(1)).save(any(RegistroPortafolioEntity.class));
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));
    }

    @Test
    @DisplayName("Aplicabilidad favorable con todos los criterios cumplidos se registra como APLICABLE")
    void aplicabilidad_favorable_seRegistraComoAplicable() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.PRESENTADO)));
        when(aplicabilidadRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());
        when(aplicabilidadRepository.save(any(AplicabilidadIniciativaEntity.class)))
                .thenAnswer(invocation -> {
                    AplicabilidadIniciativaEntity a = invocation.getArgument(0);
                    a.setId(201L);
                    return a;
                });

        // // @NEEDS_CLARIFICATION: el DTO puede llamarse ApplicabilityRequest
        // o EvaluacionAplicabilidadRequest; los criterios estructurados se
        // modelan como lista de pares clave/valor.
        EvaluacionDetail detalle = service.registrarAplicabilidad(1L,
                new ApplicabilityRequest("APLICABLE", null, List.of(
                        new ApplicabilityCriterion("COMPETENCIA", "Cumple", 1),
                        new ApplicabilityCriterion("VALOR_PUBLICO", "Cumple", 2),
                        new ApplicabilityCriterion("CARACTER_INNOVADOR", "Cumple", 3))),
                contextoEvaluador(), IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        verify(aplicabilidadRepository, times(1)).save(any(AplicabilidadIniciativaEntity.class));
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));
    }

    @Test
    @DisplayName("Aplicabilidad NO_APLICABLE sin motivo se rechaza con APPLICABILITY_INCOMPLETE")
    void aplicabilidad_noAplicable_sinMotivo_lanzaIncompleta() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.PRESENTADO)));
        when(aplicabilidadRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.registrarAplicabilidad(1L,
                        new ApplicabilityRequest("NO_APLICABLE", null, List.of()),
                        contextoEvaluador(), IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("APPLICABILITY_INCOMPLETE")
                    || error.getReason().contains("OFFICIAL_FIELD_REQUIRED")));
        verify(aplicabilidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Aplicabilidad NO_APLICABLE transita a NO_APLICABLE con motivo obligatorio")
    void aplicabilidad_noAplicable_transitaConMotivo() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.PRESENTADO)));
        when(aplicabilidadRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());
        when(aplicabilidadRepository.save(any(AplicabilidadIniciativaEntity.class)))
                .thenAnswer(invocation -> {
                    AplicabilidadIniciativaEntity a = invocation.getArgument(0);
                    a.setId(201L);
                    return a;
                });
        when(registroRepository.save(any(RegistroPortafolioEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        EvaluacionDetail detalle = service.registrarAplicabilidad(1L,
                new ApplicabilityRequest("NO_APLICABLE",
                        "No corresponde al ambito MIDAGRI ni sectorial: caso de compra",
                        List.of(
                                new ApplicabilityCriterion("COMPETENCIA", "No cumple", 1),
                                new ApplicabilityCriterion("EXCLUSION", "Compra", 2))),
                contextoEvaluador(), IDEMPOTENCY_KEY, PAYLOAD_JSON);

        assertNotNull(detalle);
        assertEquals(EstadoIniciativa.NO_APLICABLE, detalle.estadoIniciativa());
        verify(registroRepository, times(1)).save(any(RegistroPortafolioEntity.class));
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));
    }

    @Test
    @DisplayName("Admisibilidad y aplicabilidad son evaluaciones independientes y no intercambiables")
    void admisibilidadYAplicabilidadSonIndependientes() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.PRESENTADO)));
        when(evaluacionRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());
        when(aplicabilidadRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());
        when(evaluacionRepository.save(any(EvaluacionIniciativaEntity.class)))
                .thenAnswer(invocation -> {
                    EvaluacionIniciativaEntity e = invocation.getArgument(0);
                    e.setId(101L);
                    return e;
                });
        when(aplicabilidadRepository.save(any(AplicabilidadIniciativaEntity.class)))
                .thenAnswer(invocation -> {
                    AplicabilidadIniciativaEntity a = invocation.getArgument(0);
                    a.setId(201L);
                    return a;
                });

        // Admisibilidad favorable: no debe crear fila de aplicabilidad.
        service.registrarAdmisibilidad(1L,
                new AdmissibilityRequest("ADMITIDA", "Cumple requisitos", 50L),
                contextoEvaluador(), "k-1", PAYLOAD_JSON);
        verify(aplicabilidadRepository, never()).save(any());

        // Aplicabilidad favorable: no debe crear fila de evaluacion.
        service.registrarAplicabilidad(1L,
                new ApplicabilityRequest("APLICABLE", null, List.of(
                        new ApplicabilityCriterion("COMPETENCIA", "Cumple", 1))),
                contextoEvaluador(), "k-2", PAYLOAD_JSON);
        verify(evaluacionRepository, times(1)).save(any(EvaluacionIniciativaEntity.class));
    }

    @Test
    @DisplayName("El Evaluador decide y registra: el Responsable no puede registrar admisible ni aplicable")
    void separacionDeRoles_responsableNoDecide() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.PRESENTADO)));

        ResponseStatusException errorAdm = assertThrows(ResponseStatusException.class,
                () -> service.registrarAdmisibilidad(1L,
                        new AdmissibilityRequest("ADMITIDA", "ok", 50L),
                        contextoResponsable(), "k-1", PAYLOAD_JSON));
        assertTrue(errorAdm.getStatusCode() == HttpStatus.FORBIDDEN
                || errorAdm.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY);
        assertTrue(errorAdm.getReason() != null
                && (errorAdm.getReason().contains("ASSIGNMENT_SCOPE_DENIED")
                    || errorAdm.getReason().contains("FORBIDDEN_PROFILE")));

        ResponseStatusException errorApl = assertThrows(ResponseStatusException.class,
                () -> service.registrarAplicabilidad(1L,
                        new ApplicabilityRequest("APLICABLE", null, List.of(
                                new ApplicabilityCriterion("COMPETENCIA", "Cumple", 1))),
                        contextoResponsable(), "k-2", PAYLOAD_JSON));
        assertTrue(errorApl.getStatusCode() == HttpStatus.FORBIDDEN
                || errorApl.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY);
        assertTrue(errorApl.getReason() != null
                && (errorApl.getReason().contains("ASSIGNMENT_SCOPE_DENIED")
                    || errorApl.getReason().contains("FORBIDDEN_PROFILE")));
    }

    @Test
    @DisplayName("La Autoridad no puede registrar admisibilidad ni aplicabilidad (separacion de roles)")
    void autoridadNoRegistraEvaluacion() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.PRESENTADO)));

        ResponseStatusException errorAdm = assertThrows(ResponseStatusException.class,
                () -> service.registrarAdmisibilidad(1L,
                        new AdmissibilityRequest("ADMITIDA", "ok", 50L),
                        contextoAutoridad(), "k-1", PAYLOAD_JSON));
        assertTrue(errorAdm.getStatusCode() == HttpStatus.FORBIDDEN
                || errorAdm.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY);

        ResponseStatusException errorApl = assertThrows(ResponseStatusException.class,
                () -> service.registrarAplicabilidad(1L,
                        new ApplicabilityRequest("APLICABLE", null, List.of(
                                new ApplicabilityCriterion("COMPETENCIA", "Cumple", 1))),
                        contextoAutoridad(), "k-2", PAYLOAD_JSON));
        assertTrue(errorApl.getStatusCode() == HttpStatus.FORBIDDEN
                || errorApl.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName("La correccion de una opinion tecnica crea una nueva version: la fila previa permanece inmutable")
    void opinionTecnica_creaNuevaVersion() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.PRESENTADO)));
        when(evaluacionRepository.findByIniciativaId(1L))
                .thenReturn(Optional.of(evaluacionExistente(1L, 101L)));
        when(evaluacionRepository.save(any(EvaluacionIniciativaEntity.class)))
                .thenAnswer(invocation -> {
                    EvaluacionIniciativaEntity e = invocation.getArgument(0);
                    if (e.getId() == null) {
                        e.setId(102L);
                    }
                    return e;
                });

        // // @NEEDS_CLARIFICATION: el DTO de la opinion tecnica (campo 14) y
        // la version pueden ser TechnicalOpinionRequest. T057 confirmara.
        EvaluacionDetail detalle = service.corregirOpinionTecnica(1L,
                new TechnicalOpinionRequest(60L, "Observacion corregida y ampliada"),
                contextoEvaluador(), 0L, "\"101-0\"", "k-1", PAYLOAD_JSON);

        assertNotNull(detalle);
        // La fila original debe permanecer: la correccion crea una nueva
        // version; ninguna se borra.
        verify(evaluacionRepository, never()).delete(any());
        verify(evaluacionRepository, times(1)).save(any(EvaluacionIniciativaEntity.class));
    }

    @Test
    @DisplayName("La correccion de la opinion tecnica con ETag obsoleto se rechaza con 412 STATE_CHANGED")
    void opinionTecnica_etagObsoleto_412() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.PRESENTADO)));
        when(evaluacionRepository.findByIniciativaId(1L))
                .thenReturn(Optional.of(evaluacionExistente(1L, 101L)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.corregirOpinionTecnica(1L,
                        new TechnicalOpinionRequest(60L, "Observacion corregida"),
                        contextoEvaluador(), 0L, "\"101-99\"", "k-1", PAYLOAD_JSON));
        assertEquals(HttpStatus.PRECONDITION_FAILED, error.getStatusCode());
        assertTrue(error.getReason() != null && error.getReason().contains("STATE_CHANGED"));
    }

    @Test
    @DisplayName("El servicio expone contrato de interfaz y no entidades JPA en su API publica")
    void servicioExponeContratoSinEntidadesJPA() {
        assertTrue(EvaluacionIniciativaService.class.isInterface());
        for (var metodo : EvaluacionIniciativaService.class.getDeclaredMethods()) {
            String retorno = metodo.getReturnType().getName();
            assertFalse(retorno.contains("pe.gob.midagri.piip.portafolio.entity")
                            || retorno.contains("pe.gob.midagri.piip.portafolio.evaluacion"),
                    () -> "El metodo " + metodo.getName() + " no debe retornar entidades JPA");
        }
    }

    @Test
    @DisplayName("La respuesta de evaluacion expone DTO HTTP, no entidades JPA")
    void respuestaEsDtoHttp() {
        for (var c : EvaluacionDetail.class.getRecordComponents()) {
            Class<?> tipo = c.getType();
            assertFalse((tipo.getName().contains("pe.gob.midagri.piip.portafolio.entity") && !tipo.isEnum())
                            || tipo.getName().contains(".evaluacion.entity")
                            || tipo.getName().contains(".transicion.entity"),
                    () -> "El campo " + c.getName() + " no debe ser una entidad JPA");
        }
    }

    @Test
    @DisplayName("El Evaluador exige evidencia documental para admitir (campo 14 obligatorio)")
    void admitir_sinDocumentoOpinion_lanzaValidacion() {
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.PRESENTADO)));
        when(evaluacionRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.registrarAdmisibilidad(1L,
                        new AdmissibilityRequest("ADMITIDA", "ok", null),
                        contextoEvaluador(), IDEMPOTENCY_KEY, PAYLOAD_JSON));
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, error.getStatusCode());
        assertTrue(error.getReason() != null
                && (error.getReason().contains("ADMISSIBILITY_INCOMPLETE")
                    || error.getReason().contains("EVIDENCE_NOT_ELIGIBLE")
                    || error.getReason().contains("OFFICIAL_FIELD_REQUIRED")));
    }

    @Test
    @DisplayName("La auditoria se invoca tanto en exito como en denegacion de evaluacion")
    void auditoriaExitoYDenegacion() {
        // Caso 1: exito al registrar admisibilidad.
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.PRESENTADO)));
        when(evaluacionRepository.findByIniciativaId(1L)).thenReturn(Optional.empty());
        when(evaluacionRepository.save(any(EvaluacionIniciativaEntity.class)))
                .thenAnswer(invocation -> {
                    EvaluacionIniciativaEntity e = invocation.getArgument(0);
                    e.setId(101L);
                    return e;
                });
        service.registrarAdmisibilidad(1L,
                new AdmissibilityRequest("ADMITIDA", "ok", 50L),
                contextoEvaluador(), "k-1", PAYLOAD_JSON);
        verify(auditService, times(1)).registrarExito(any(AuditService.AuditCommand.class));

        // Caso 2: denegacion por estado no PRESENTADO.
        when(registroRepository.findById(1L)).thenReturn(Optional.of(registroPresentado(1L,
                EstadoIniciativa.NO_ADMISIBLE)));
        assertThrows(ResponseStatusException.class, () -> service.registrarAdmisibilidad(1L,
                new AdmissibilityRequest("ADMITIDA", "ok", 50L),
                contextoEvaluador(), "k-2", PAYLOAD_JSON));
        verify(auditService, times(1)).registrarDenegacion(any(AuditService.AuditCommand.class));
    }

}

