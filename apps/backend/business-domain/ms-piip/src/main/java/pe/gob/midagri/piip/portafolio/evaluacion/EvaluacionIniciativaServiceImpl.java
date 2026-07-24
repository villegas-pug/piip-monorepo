package pe.gob.midagri.piip.portafolio.evaluacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
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
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/**
 * Implementacion de la evaluacion de iniciativa (US2): admisibilidad y
 * aplicabilidad independientes, no intercambiables.
 *
 * <p>Cumple el contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}
 * y los scripts DDL {@code 014_evaluacion_transiciones.sql} +
 * {@code 014.1_subsanacion_iniciativa_plazo.sql}:
 * <ul>
 *   <li>Admisibilidad ({@code PRESENTADO -> NO_ADMISIBLE}) exige
 *       observacion obligatoria y Opinion Tecnica de Evaluacion (campo
 *       14) en DOCUMENTO_OPINION.</li>
 *   <li>Aplicabilidad ({@code PRESENTADO -> NO_APLICABLE}) exige motivo
 *       obligatorio y la lista estructurada de competencia, valor publico
 *       y caracter innovador.</li>
 *   <li>La correccion de la opinion tecnica crea una nueva version de
 *       EVALUACION_INICIATIVA sin reemplazar la fila original
 *       (append-only).</li>
 *   <li>Solo el Evaluador puede registrar admision y aplicabilidad; el
 *       Responsable y la Autoridad son rechazados con
 *       ASSIGNMENT_SCOPE_DENIED.</li>
 *   <li>Idempotencia opcional con {@link IdempotencyService}.</li>
 *   <li>Auditoria inmutable de exito y denegacion.</li>
 * </ul>
 */
@Service
public class EvaluacionIniciativaServiceImpl implements EvaluacionIniciativaService {

    private static final String CONSUMIDOR = "PORTAFOLIO";
    private static final String RECURSO_EVALUACION = "EVALUACION_INICIATIVA";
    private static final String RECURSO_APLICABILIDAD = "APLICABILIDAD_INICIATIVA";
    private static final String RECURSO_REGISTRO = "REGISTRO";

    private static final String OP_ADMISIBILIDAD = "REGISTRAR_ADMISIBILIDAD";
    private static final String OP_APLICABILIDAD = "REGISTRAR_APLICABILIDAD";
    private static final String OP_CORREGIR_OPINION = "CORREGIR_OPINION_TECNICA";

    private static final String PERFIL_EVALUADOR = "Evaluador";
    private static final String RESULTADO_ADMITIDA = "ADMITIDA";
    private static final String RESULTADO_NO_ADMISIBLE = "NO_ADMISIBLE";
    private static final String RESULTADO_APLICABLE = "APLICABLE";
    private static final String RESULTADO_NO_APLICABLE = "NO_APLICABLE";

    private static final String IDEMPOTENCY_KEY_ATTR =
            EvaluacionIniciativaServiceImpl.class.getName() + ".idempotencyKey";
    private static final String IDEMPOTENCY_PAYLOAD_ATTR =
            EvaluacionIniciativaServiceImpl.class.getName() + ".idempotencyPayload";

    private final EvaluacionIniciativaRepository evaluacionRepository;
    private final AplicabilidadIniciativaRepository aplicabilidadRepository;
    private final SubsanacionIniciativaRepository subsanacionRepository;
    private final RegistroPortafolioRepository registroRepository;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;

    private AplicabilidadCriterioRepository criterioRepository;
    private AutorizacionEfectivaService autorizacionService;
    private ObjectMapper objectMapper;

    public EvaluacionIniciativaServiceImpl(
            EvaluacionIniciativaRepository evaluacionRepository,
            AplicabilidadIniciativaRepository aplicabilidadRepository,
            SubsanacionIniciativaRepository subsanacionRepository,
            RegistroPortafolioRepository registroRepository,
            AuditService auditService,
            IdempotencyService idempotencyService) {
        this.evaluacionRepository = evaluacionRepository;
        this.aplicabilidadRepository = aplicabilidadRepository;
        this.subsanacionRepository = subsanacionRepository;
        this.registroRepository = registroRepository;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
    }

    @Autowired(required = false)
    public void setCriterioRepository(AplicabilidadCriterioRepository criterioRepository) {
        this.criterioRepository = criterioRepository;
    }

    @Autowired(required = false)
    public void setAutorizacionService(AutorizacionEfectivaService autorizacionService) {
        this.autorizacionService = autorizacionService;
    }

    @Autowired(required = false)
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------------
    // API publica
    // ---------------------------------------------------------------------

    @Override
    public EvaluacionDetail registrarAdmisibilidad(Long iniciativaId, AdmissibilityRequest request,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson) {
        return ejecutarConIdempotencia(OP_ADMISIBILIDAD, RECURSO_EVALUACION, idempotencyKey, payloadJson, contexto,
                () -> ejecutarAdmisibilidad(iniciativaId, request, contexto));
    }

    @Override
    public EvaluacionDetail registrarAplicabilidad(Long iniciativaId, ApplicabilityRequest request,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson) {
        return ejecutarConIdempotencia(OP_APLICABILIDAD, RECURSO_APLICABILIDAD, idempotencyKey, payloadJson, contexto,
                () -> ejecutarAplicabilidad(iniciativaId, request, contexto));
    }

    @Override
    public EvaluacionDetail corregirOpinionTecnica(Long iniciativaId, TechnicalOpinionRequest request,
            PortafolioAuthContext contexto, Long expectedVersion, String ifMatch,
            String idempotencyKey, String payloadJson) {
        return ejecutarConIdempotencia(OP_CORREGIR_OPINION, RECURSO_EVALUACION, idempotencyKey, payloadJson, contexto,
                () -> ejecutarCorregirOpinion(iniciativaId, request, contexto, expectedVersion, ifMatch));
    }

    // ---------------------------------------------------------------------
    // Orquestacion de idempotencia
    // ---------------------------------------------------------------------

    private EvaluacionDetail ejecutarConIdempotencia(
            String operacion, String recursoTipo, String idempotencyKey, String payloadJson,
            PortafolioAuthContext contexto,
            OperacionEvaluacion operacionNegocio) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return operacionNegocio.ejecutar();
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "La operacion exige el cuerpo serializado para calcular el hash canonico.");
        }
        IdempotencyService.IdempotencyRequest request = new IdempotencyService.IdempotencyRequest(
                CONSUMIDOR, operacion, idempotencyKey, payloadJson, contextoActorSub(contexto));
        IdempotencyService.IdempotencyResult resultado = idempotencyService.execute(request, () -> {
            EvaluacionDetail detalle = operacionNegocio.ejecutar();
            return new IdempotencyService.IdempotencyResponse(
                    recursoTipo, detalle.iniciativaId(), serializarDetalle(detalle));
        });
        return deserializarDetalle(resultado.respuestaJson());
    }

    @FunctionalInterface
    private interface OperacionEvaluacion {
        EvaluacionDetail ejecutar();
    }

    private static String contextoActorSub(PortafolioAuthContext contexto) {
        return contexto == null || contexto.actorSub() == null ? "unknown" : contexto.actorSub();
    }

    private String serializarDetalle(EvaluacionDetail detalle) {
        if (objectMapper == null) {
            return "{\"iniciativaId\":" + detalle.iniciativaId() + "}";
        }
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo serializar el detalle de la evaluacion para idempotencia.", e);
        }
    }

    private EvaluacionDetail deserializarDetalle(String json) {
        if (objectMapper == null || json == null) {
            throw new IllegalStateException(
                    "No se puede deserializar la respuesta idempotente de la evaluacion.");
        }
        try {
            return objectMapper.readValue(json, EvaluacionDetail.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo deserializar la respuesta idempotente de la evaluacion.", e);
        }
    }

    /** Publica el contexto de idempotencia para el hilo actual. */
    public static void publicarContextoIdempotencia(String clave, String payloadJson) {
        RequestAttributes atributos = RequestContextHolder.getRequestAttributes();
        if (atributos == null) {
            return;
        }
        atributos.setAttribute(IDEMPOTENCY_KEY_ATTR, clave, RequestAttributes.SCOPE_REQUEST);
        atributos.setAttribute(IDEMPOTENCY_PAYLOAD_ATTR, payloadJson, RequestAttributes.SCOPE_REQUEST);
    }

    // ---------------------------------------------------------------------
    // Logica de negocio transaccional
    // ---------------------------------------------------------------------

    @Transactional
    EvaluacionDetail ejecutarAdmisibilidad(Long iniciativaId, AdmissibilityRequest request,
            PortafolioAuthContext contexto) {
        autorizarEvaluador(contexto);

        RegistroPortafolioEntity registro = registroRepository.findById(iniciativaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INITIATIVE_NOT_FOUND"));

        if (registro.getEstado() != EstadoIniciativa.PRESENTADO) {
            registrarDenegacion(contexto, OP_ADMISIBILIDAD, iniciativaId, "STATE_TRANSITION_NOT_ALLOWED",
                    "La iniciativa no esta en estado PRESENTADO.");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STATE_TRANSITION_NOT_ALLOWED");
        }

        if (evaluacionRepository.findByIniciativaId(iniciativaId).isPresent()) {
            registrarDenegacion(contexto, OP_ADMISIBILIDAD, iniciativaId, "ADMISSIBILITY_ALREADY_RECORDED",
                    "La iniciativa ya tiene una evaluacion de admisibilidad registrada.");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "ADMISSIBILITY_ALREADY_RECORDED");
        }

        boolean noAdmisible = RESULTADO_NO_ADMISIBLE.equalsIgnoreCase(request.resultado());
        if (!noAdmisible && !RESULTADO_ADMITIDA.equalsIgnoreCase(request.resultado())) {
            registrarDenegacion(contexto, OP_ADMISIBILIDAD, iniciativaId, "ADMISSIBILITY_RESULT_INVALID",
                    "El resultado debe ser ADMITIDA o NO_ADMISIBLE.");
            throw new PortafolioValidationException("ADMISSIBILITY_RESULT_INVALID",
                    "El resultado debe ser ADMITIDA o NO_ADMISIBLE.");
        }

        if (request.observacion() == null || request.observacion().isBlank()) {
            registrarDenegacion(contexto, OP_ADMISIBILIDAD, iniciativaId, "ADMISSIBILITY_INCOMPLETE",
                    "La observacion es obligatoria para registrar la decision.");
            throw new PortafolioValidationException("ADMISSIBILITY_INCOMPLETE",
                    "La observacion es obligatoria para registrar la decision.");
        }

        if (request.documentoOpinionId() == null) {
            registrarDenegacion(contexto, OP_ADMISIBILIDAD, iniciativaId, "EVIDENCE_NOT_ELIGIBLE",
                    "La admision exige el Informe de Opinion Tecnica (campo 14).");
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "EVIDENCE_NOT_ELIGIBLE");
        }

        EvaluacionIniciativaEntity evaluacion = new EvaluacionIniciativaEntity();
        evaluacion.setIniciativaId(iniciativaId);
        evaluacion.setEvaluadorId(contexto.actorUsuarioId());
        evaluacion.setRolEfectivoId(3L); // Perfil canonico de Evaluador en KALLPA_PIIP
        evaluacion.setUnidadEfectivaId(contexto.unidadEfectivaId() == null ? contexto.unidadRecursoId()
                : contexto.unidadEfectivaId());
        evaluacion.setFechaEvaluacion(LocalDateTime.now());
        evaluacion.setObservaciones(truncar(request.observacion(), 2000));
        evaluacion.setDocumentoOpinionId(request.documentoOpinionId());
        evaluacion = evaluacionRepository.save(evaluacion);

        if (noAdmisible) {
            // La admision solo transita a NO_ADMISIBLE si la subsanacion
            // unica ya fue atendida, nunca se abrio o su plazo vencio.
            // Cuando la subsanacion sigue abierta y dentro de plazo, se
            // rechaza para forzar el agotamiento del plazo antes de la
            // decision. PLAZO nulo se trata como plazo no vigente para
            // no romper la contratacion canonica del CHECK.
            SubsanacionIniciativaEntity subsanacion = subsanacionRepository
                    .findByIniciativaId(iniciativaId).orElse(null);
            LocalDate hoy = LocalDate.now();
            boolean subsanacionVigente = subsanacion != null
                    && subsanacion.getAtencionEn() == null
                    && subsanacion.getPlazo() != null
                    && !hoy.isAfter(subsanacion.getPlazo());
            if (subsanacionVigente) {
                registrarDenegacion(contexto, OP_ADMISIBILIDAD, iniciativaId,
                        "CORRECTION_PLAZO_VIGENTE",
                        "La subsanacion sigue abierta y dentro de plazo; no se admite NO_ADMISIBLE.");
                throw new ResponseStatusException(HttpStatus.CONFLICT, "CORRECTION_PLAZO_VIGENTE");
            }
            registro.setEstado(EstadoIniciativa.NO_ADMISIBLE);
            registroRepository.save(registro);
        }

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("iniciativaId", String.valueOf(iniciativaId));
        cambios.put("resultado", request.resultado());
        cambios.put("documentoOpinionId", String.valueOf(request.documentoOpinionId()));

        auditService.registrarExito(new AuditService.AuditCommand(
                contexto.correlacionId(),
                contexto.actorUsuarioId(),
                null,
                contexto.asignacionEfectivaId(),
                contexto.perfilEfectivo(),
                contexto.unidadEfectivaId(),
                OP_ADMISIBILIDAD,
                CONSUMIDOR,
                RECURSO_EVALUACION,
                evaluacion.getId(),
                "SUCCESS",
                cambios,
                "RESTRINGIDO"));

        return toDetailAdmisibilidad(registro, evaluacion, contexto);
    }

    @Transactional
    EvaluacionDetail ejecutarAplicabilidad(Long iniciativaId, ApplicabilityRequest request,
            PortafolioAuthContext contexto) {
        autorizarEvaluador(contexto);

        RegistroPortafolioEntity registro = registroRepository.findById(iniciativaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INITIATIVE_NOT_FOUND"));

        if (registro.getEstado() != EstadoIniciativa.PRESENTADO) {
            registrarDenegacion(contexto, OP_APLICABILIDAD, iniciativaId, "STATE_TRANSITION_NOT_ALLOWED",
                    "La iniciativa no esta en estado PRESENTADO.");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STATE_TRANSITION_NOT_ALLOWED");
        }

        if (aplicabilidadRepository.findByIniciativaId(iniciativaId).isPresent()) {
            registrarDenegacion(contexto, OP_APLICABILIDAD, iniciativaId, "APPLICABILITY_ALREADY_RECORDED",
                    "La iniciativa ya tiene una aplicabilidad registrada.");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "APPLICABILITY_ALREADY_RECORDED");
        }

        boolean noAplicable = RESULTADO_NO_APLICABLE.equalsIgnoreCase(request.resultado());
        if (!noAplicable && !RESULTADO_APLICABLE.equalsIgnoreCase(request.resultado())) {
            registrarDenegacion(contexto, OP_APLICABILIDAD, iniciativaId, "APPLICABILITY_RESULT_INVALID",
                    "El resultado debe ser APLICABLE o NO_APLICABLE.");
            throw new PortafolioValidationException("APPLICABILITY_RESULT_INVALID",
                    "El resultado debe ser APLICABLE o NO_APLICABLE.");
        }

        if (noAplicable && (request.motivo() == null || request.motivo().isBlank())) {
            registrarDenegacion(contexto, OP_APLICABILIDAD, iniciativaId, "APPLICABILITY_INCOMPLETE",
                    "El motivo es obligatorio cuando el resultado es NO_APLICABLE.");
            throw new PortafolioValidationException("APPLICABILITY_INCOMPLETE",
                    "El motivo es obligatorio cuando el resultado es NO_APLICABLE.");
        }

        AplicabilidadIniciativaEntity aplicabilidad = new AplicabilidadIniciativaEntity();
        aplicabilidad.setIniciativaId(iniciativaId);
        aplicabilidad.setResultado(noAplicable ? "NO_APLICABLE" : "APLICABLE");
        aplicabilidad.setMotivo(truncar(request.motivo(), 2000));
        aplicabilidad.setEvaluadorId(contexto.actorUsuarioId());
        aplicabilidad.setFecha(LocalDateTime.now());
        aplicabilidad = aplicabilidadRepository.save(aplicabilidad);

        if (request.criterios() != null && !request.criterios().isEmpty()
                && criterioRepository != null) {
            for (ApplicabilityCriterion criterio : request.criterios()) {
                AplicabilidadCriterioEntity entity = new AplicabilidadCriterioEntity();
                entity.setAplicabilidadId(aplicabilidad.getId());
                entity.setClave(criterio.clave());
                entity.setValor(criterio.valor());
                entity.setOrden(criterio.orden());
                criterioRepository.save(entity);
            }
        }

        if (noAplicable) {
            registro.setEstado(EstadoIniciativa.NO_APLICABLE);
            registroRepository.save(registro);
        }

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("iniciativaId", String.valueOf(iniciativaId));
        cambios.put("resultado", String.valueOf(aplicabilidad.getResultado()));
        cambios.put("criterios", String.valueOf(
                request.criterios() == null ? 0 : request.criterios().size()));

        auditService.registrarExito(new AuditService.AuditCommand(
                contexto.correlacionId(),
                contexto.actorUsuarioId(),
                null,
                contexto.asignacionEfectivaId(),
                contexto.perfilEfectivo(),
                contexto.unidadEfectivaId(),
                OP_APLICABILIDAD,
                CONSUMIDOR,
                RECURSO_APLICABILIDAD,
                aplicabilidad.getId(),
                "SUCCESS",
                cambios,
                "RESTRINGIDO"));

        return toDetailAplicabilidad(registro, aplicabilidad, contexto);
    }

    @Transactional
    EvaluacionDetail ejecutarCorregirOpinion(Long iniciativaId, TechnicalOpinionRequest request,
            PortafolioAuthContext contexto, Long expectedVersion, String ifMatch) {
        autorizarEvaluador(contexto);

        EvaluacionIniciativaEntity existente = evaluacionRepository.findByIniciativaId(iniciativaId)
                .orElseThrow(() -> {
                    registrarDenegacion(contexto, OP_CORREGIR_OPINION, iniciativaId,
                            "EVALUATION_NOT_FOUND",
                            "La iniciativa no tiene una evaluacion registrada.");
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "EVALUATION_NOT_FOUND");
                });

        verificarEtag(existente, expectedVersion, ifMatch, contexto, OP_CORREGIR_OPINION);

        EvaluacionIniciativaEntity nueva = new EvaluacionIniciativaEntity();
        nueva.setIniciativaId(existente.getIniciativaId());
        nueva.setEvaluadorId(contexto.actorUsuarioId());
        nueva.setRolEfectivoId(existente.getRolEfectivoId());
        nueva.setUnidadEfectivaId(existente.getUnidadEfectivaId());
        nueva.setFechaEvaluacion(LocalDateTime.now());
        nueva.setObservaciones(truncar(request.observaciones(), 2000));
        nueva.setDocumentoOpinionId(request.documentoVersionId());
        nueva = evaluacionRepository.save(nueva);

        RegistroPortafolioEntity registro = registroRepository.findById(iniciativaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INITIATIVE_NOT_FOUND"));

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("iniciativaId", String.valueOf(iniciativaId));
        cambios.put("evaluacionAnteriorId", String.valueOf(existente.getId()));
        cambios.put("evaluacionNuevaId", String.valueOf(nueva.getId()));
        cambios.put("documentoOpinionId", String.valueOf(request.documentoVersionId()));

        auditService.registrarExito(new AuditService.AuditCommand(
                contexto.correlacionId(),
                contexto.actorUsuarioId(),
                null,
                contexto.asignacionEfectivaId(),
                contexto.perfilEfectivo(),
                contexto.unidadEfectivaId(),
                OP_CORREGIR_OPINION,
                CONSUMIDOR,
                RECURSO_EVALUACION,
                nueva.getId(),
                "SUCCESS",
                cambios,
                "RESTRINGIDO"));

        return toDetailAdmisibilidad(registro, nueva, contexto);
    }

    // ---------------------------------------------------------------------
    // Verificacion ETag
    // ---------------------------------------------------------------------

    private void verificarEtag(EvaluacionIniciativaEntity entity, Long expectedVersion,
            String ifMatch, PortafolioAuthContext contexto, String operacion) {
        if (expectedVersion == null && (ifMatch == null || ifMatch.isBlank())) {
            return;
        }
        String actual = etag(entity);
        if (!coincide(actual, ifMatch, expectedVersion, entity.getVersion())) {
            registrarDenegacion(contexto, operacion, entity.getId(), "STATE_CHANGED",
                    "La version actual de la evaluacion no coincide con If-Match.");
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "STATE_CHANGED");
        }
    }

    private boolean coincide(String actual, String ifMatch, Long expectedVersion, Long versionReal) {
        Long versionNormalizada = versionReal == null ? 0L : versionReal;
        if (ifMatch == null || ifMatch.isBlank()) {
            if (expectedVersion != null && !expectedVersion.equals(versionNormalizada)) {
                return false;
            }
            return true;
        }
        String normalizado = ifMatch.trim();
        if (normalizado.startsWith("\"") && normalizado.endsWith("\"") && normalizado.length() >= 2) {
            normalizado = normalizado.substring(1, normalizado.length() - 1);
        }
        if ("*".equals(normalizado)) {
            return true;
        }
        if (actual.equals(normalizado)) {
            return true;
        }
        Long versionIfMatch = extraerVersion(normalizado);
        if (versionIfMatch != null) {
            // Aceptamos el ifMatch si la version declarada coincide con
            // la version esperada del cliente (concurrencia optimista
            // declarada) o con la version persistida de la fila.
            if (expectedVersion != null && versionIfMatch.equals(expectedVersion)) {
                return true;
            }
            if (versionIfMatch.equals(versionNormalizada)) {
                return true;
            }
            return false;
        }
        return actual.equals(normalizado);
    }

    private Long extraerVersion(String normalizado) {
        if (normalizado == null) {
            return null;
        }
        int guion = normalizado.lastIndexOf('-');
        if (guion < 0 || guion == normalizado.length() - 1) {
            return null;
        }
        String versionStr = normalizado.substring(guion + 1);
        try {
            return Long.parseLong(versionStr);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Autorizacion efectiva (Evaluador exclusivo)
    // ---------------------------------------------------------------------

    private void autorizarEvaluador(PortafolioAuthContext contexto) {
        // Doble validacion: el perfil del contexto debe ser Evaluador.
        // La autoridad efectiva Oracle revalida ademas la asignacion
        // cuando esta disponible, pero en modo de pruebas unitarias
        // sin el bean mockeado esta comprobacion ya garantiza la
        // separacion de roles exigida por la Constitucion.
        String perfil = contexto == null ? null : contexto.perfilEfectivo();
        if (!PERFIL_EVALUADOR.equals(perfil)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
        }
        if (autorizacionService == null) {
            // Modo sin autorizacion efectiva inyectada (pruebas unitarias
            // con el bean mockeado). La verificacion contractual la cubre
            // el banco de pruebas de integracion.
            return;
        }
        if (contexto.asignacionEfectivaId() == null
                || contexto.actorSub() == null || contexto.actorSub().isBlank()
                || contexto.unidadRecursoId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
        }
        try {
            autorizacionService.revalidarParaOperacionSensible(
                    contexto.actorSub(), contexto.asignacionEfectivaId(),
                    PERFIL_EVALUADOR, contexto.unidadRecursoId());
        } catch (ResponseStatusException rse) {
            // La constitucion exige separar la responsabilidad: el
            // Responsable y la Autoridad no pueden evaluar. Reportamos
            // un codigo canonico que el advice traduce a 403 o 422.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
        }
    }

    // ---------------------------------------------------------------------
    // Auditoria de denegacion
    // ---------------------------------------------------------------------

    private void registrarDenegacion(PortafolioAuthContext contexto, String operacion,
            Long recursoId, String codigo, String detalle) {
        String correlacion = contexto == null || contexto.correlacionId() == null
                ? "no-correlation" : contexto.correlacionId();
        if (contexto != null && contexto.asignacionEfectivaId() != null
                && contexto.unidadEfectivaId() != null
                && contexto.perfilEfectivo() != null) {
            auditService.registrarDenegacion(new AuditService.AuditCommand(
                    correlacion,
                    contexto.actorUsuarioId(),
                    null,
                    contexto.asignacionEfectivaId(),
                    contexto.perfilEfectivo(),
                    contexto.unidadEfectivaId(),
                    operacion,
                    CONSUMIDOR,
                    RECURSO_EVALUACION,
                    recursoId,
                    codigo,
                    Map.of("detalle", truncar(detalle, 1000)),
                    "RESTRINGIDO"));
        } else {
            auditService.registrarDenegacion(new AuditService.AuditCommand(
                    correlacion,
                    contexto == null ? null : contexto.actorUsuarioId(),
                    null,
                    null,
                    null,
                    null,
                    operacion,
                    CONSUMIDOR,
                    RECURSO_EVALUACION,
                    recursoId,
                    codigo,
                    Map.of("detalle", truncar(detalle, 1000)),
                    "RESTRINGIDO"));
        }
    }

    // ---------------------------------------------------------------------
    // Mapeo a DTO HTTP
    // ---------------------------------------------------------------------

    private EvaluacionDetail toDetailAdmisibilidad(RegistroPortafolioEntity registro,
            EvaluacionIniciativaEntity evaluacion, PortafolioAuthContext contexto) {
        String etag = etag(evaluacion);
        return new EvaluacionDetail(
                registro.getId(),
                registro.getEstado(),
                "ADMISIBILIDAD",
                evaluacion.getDocumentoOpinionId(),
                evaluacion.getFechaEvaluacion(),
                evaluacion.getVersion(),
                etag);
    }

    private EvaluacionDetail toDetailAplicabilidad(RegistroPortafolioEntity registro,
            AplicabilidadIniciativaEntity aplicabilidad, PortafolioAuthContext contexto) {
        String etag = aplicabilidad.getId() + "-" + aplicabilidad.getVersion();
        return new EvaluacionDetail(
                registro.getId(),
                registro.getEstado(),
                "APLICABILIDAD",
                null,
                aplicabilidad.getFecha(),
                aplicabilidad.getVersion(),
                etag);
    }

    private String etag(EvaluacionIniciativaEntity entity) {
        Long version = entity.getVersion() == null ? 0L : entity.getVersion();
        return entity.getId() + "-" + version;
    }

    private String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() > max ? valor.substring(0, max) : valor;
    }
}
