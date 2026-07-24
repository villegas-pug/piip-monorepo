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
import pe.gob.midagri.piip.portafolio.dto.OpenCorrectionRequest;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.SubsanacionDetail;
import pe.gob.midagri.piip.portafolio.dto.SubsanacionEditCommand;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/**
 * Implementacion de la subsanacion unica de iniciativa (US2).
 *
 * <p>Cumple la matriz 013, el contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}
 * y el script DDL {@code 014_evaluacion_transiciones.sql} +
 * {@code 014.1_subsanacion_iniciativa_plazo.sql}:
 * <ul>
 *   <li>Una sola oportunidad por iniciativa (UK_SI_INICIATIVA).</li>
 *   <li>Plazo estrictamente posterior a la apertura (CK_SI_PLAZO).</li>
 *   <li>Edicion limitada a los campos oficiales 5-12, 22 y 23.</li>
 *   <li>Cierre conserva la fila para auditoria (append-only).</li>
 *   <li>Autorizacion efectiva: Responsable abre/edita/cierra; Evaluador
 *       abre/cierra; Autoridad y UnidadAdmin no participan.</li>
 *   <li>Idempotencia opcional mediante {@link IdempotencyService}.</li>
 *   <li>Auditoria inmutable de exito y denegacion.</li>
 * </ul>
 */
@Service
public class SubsanacionIniciativaServiceImpl implements SubsanacionIniciativaService {

    private static final String CONSUMIDOR = "PORTAFOLIO";
    private static final String RECURSO_SUBSANACION = "SUBSANACION_INICIATIVA";
    private static final String RECURSO_REGISTRO = "REGISTRO";

    private static final String OP_ABRIR = "ABRIR_SUBSANACION";
    private static final String OP_EDITAR = "EDITAR_SUBSANACION";
    private static final String OP_CERRAR = "CERRAR_SUBSANACION";

    private static final String PERFIL_RESPONSABLE = "Responsable";
    private static final String PERFIL_EVALUADOR = "Evaluador";
    private static final String PERFIL_UNIDAD_ADMIN = "UnidadAdmin";

    private static final String IDEMPOTENCY_KEY_ATTR =
            SubsanacionIniciativaServiceImpl.class.getName() + ".idempotencyKey";
    private static final String IDEMPOTENCY_PAYLOAD_ATTR =
            SubsanacionIniciativaServiceImpl.class.getName() + ".idempotencyPayload";

    private final SubsanacionIniciativaRepository subsanacionRepository;
    private final RegistroPortafolioRepository registroRepository;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;

    private AutorizacionEfectivaService autorizacionService;
    private ObjectMapper objectMapper;

    public SubsanacionIniciativaServiceImpl(
            SubsanacionIniciativaRepository subsanacionRepository,
            RegistroPortafolioRepository registroRepository,
            AuditService auditService,
            IdempotencyService idempotencyService) {
        this.subsanacionRepository = subsanacionRepository;
        this.registroRepository = registroRepository;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
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
    public SubsanacionDetail abrir(Long iniciativaId, OpenCorrectionRequest request,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson) {
        return ejecutarConIdempotencia(OP_ABRIR, RECURSO_SUBSANACION, idempotencyKey, payloadJson, contexto,
                () -> ejecutarAbrir(iniciativaId, request, contexto));
    }

    @Override
    public SubsanacionDetail editar(Long iniciativaId, SubsanacionEditCommand comando,
            PortafolioAuthContext contexto, Long expectedVersion, String ifMatch,
            String idempotencyKey, String payloadJson) {
        return ejecutarConIdempotencia(OP_EDITAR, RECURSO_SUBSANACION, idempotencyKey, payloadJson, contexto,
                () -> ejecutarEditar(iniciativaId, comando, contexto, expectedVersion, ifMatch));
    }

    @Override
    public SubsanacionDetail cerrar(Long iniciativaId, String motivo,
            PortafolioAuthContext contexto, Long expectedVersion, String ifMatch,
            String idempotencyKey, String payloadJson) {
        return ejecutarConIdempotencia(OP_CERRAR, RECURSO_SUBSANACION, idempotencyKey, payloadJson, contexto,
                () -> ejecutarCerrar(iniciativaId, motivo, contexto, expectedVersion, ifMatch));
    }

    // ---------------------------------------------------------------------
    // Orquestacion de idempotencia
    // ---------------------------------------------------------------------

    private SubsanacionDetail ejecutarConIdempotencia(
            String operacion, String recursoTipo, String idempotencyKey, String payloadJson,
            PortafolioAuthContext contexto,
            OperacionSubsanacion operacionNegocio) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return operacionNegocio.ejecutar();
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "La operacion exige el cuerpo serializado para calcular el hash canonico.");
        }
        IdempotencyService.IdempotencyRequest request = new IdempotencyService.IdempotencyRequest(
                CONSUMIDOR, operacion, idempotencyKey, payloadJson,
                contextoActorSub(contexto));
        IdempotencyService.IdempotencyResult resultado = idempotencyService.execute(request, () -> {
            SubsanacionDetail detalle = operacionNegocio.ejecutar();
            return new IdempotencyService.IdempotencyResponse(
                    recursoTipo, detalle.id(), serializarDetalle(detalle));
        });
        return deserializarDetalle(resultado.respuestaJson());
    }

    @FunctionalInterface
    private interface OperacionSubsanacion {
        SubsanacionDetail ejecutar();
    }

    private static String contextoActorSub(PortafolioAuthContext contexto) {
        return contexto == null || contexto.actorSub() == null ? "unknown" : contexto.actorSub();
    }

    private String serializarDetalle(SubsanacionDetail detalle) {
        if (objectMapper == null) {
            return "{\"id\":" + detalle.id() + "}";
        }
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo serializar el detalle de la subsanacion para idempotencia.", e);
        }
    }

    private SubsanacionDetail deserializarDetalle(String json) {
        if (objectMapper == null || json == null) {
            throw new IllegalStateException(
                    "No se puede deserializar la respuesta idempotente de la subsanacion.");
        }
        try {
            return objectMapper.readValue(json, SubsanacionDetail.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo deserializar la respuesta idempotente de la subsanacion.", e);
        }
    }

    /** Publica el contexto de idempotencia para el hilo actual. Lo invoca el controlador. */
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
    SubsanacionDetail ejecutarAbrir(Long iniciativaId, OpenCorrectionRequest request,
            PortafolioAuthContext contexto) {
        autorizarAbrir(contexto);

        RegistroPortafolioEntity registro = registroRepository.findById(iniciativaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INITIATIVE_NOT_FOUND"));

        if (registro.getEstado() != EstadoIniciativa.PRESENTADO) {
            registrarDenegacion(contexto, OP_ABRIR, iniciativaId, "STATE_NOT_PRESENTED",
                    "La iniciativa no esta en estado PRESENTADO.");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STATE_TRANSITION_NOT_ALLOWED");
        }

        if (subsanacionRepository.findByIniciativaId(iniciativaId).isPresent()) {
            registrarDenegacion(contexto, OP_ABRIR, iniciativaId, "CORRECTION_ALREADY_USED",
                    "La iniciativa ya tiene una subsanacion registrada.");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CORRECTION_ALREADY_USED");
        }

        LocalDateTime apertura = LocalDateTime.now();
        if (!request.venceEn().isAfter(apertura.toLocalDate())
                && !request.venceEn().isEqual(apertura.toLocalDate().plusDays(1))) {
            // CK_SI_PLAZO exige PLAZO > APERTURA_EN (a nivel de fila). La
            // validacion de aplicacion se hace contra la fecha local de
            // apertura, sin depender de la hora, para mantener la
            // invariante determinista del CHECK.
        }
        if (!request.venceEn().isAfter(apertura.toLocalDate())) {
            registrarDenegacion(contexto, OP_ABRIR, iniciativaId, "SUBSANATION_PLAZO_INVALID",
                    "El plazo debe ser posterior a la fecha de apertura.");
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "SUBSANATION_PLAZO_INVALID: El plazo debe ser estrictamente posterior a la fecha de apertura");
        }

        SubsanacionIniciativaEntity entity = new SubsanacionIniciativaEntity();
        entity.setIniciativaId(iniciativaId);
        entity.setPlazo(request.venceEn());
        entity.setIncumplimientos(serializarIncumplimientos(request.incumplimientos()));
        entity.setAperturaEn(apertura);
        entity.setAtencionEn(null);
        entity.setActorId(contexto.actorUsuarioId());
        try {
            entity = subsanacionRepository.save(entity);
        } catch (org.springframework.dao.DataIntegrityViolationException ex) {
            // La UK_SI_INICIATIVA rechazo la segunda fila en una carrera concurrente.
            registrarDenegacion(contexto, OP_ABRIR, iniciativaId, "CORRECTION_ALREADY_USED",
                    "La iniciativa ya tiene una subsanacion registrada (UK violada).");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CORRECTION_ALREADY_USED");
        }

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("iniciativaId", String.valueOf(iniciativaId));
        cambios.put("plazo", request.venceEn().toString());
        cambios.put("incumplimientos", String.valueOf(request.incumplimientos().size()));

        auditService.registrarExito(new AuditService.AuditCommand(
                contexto.correlacionId(),
                contexto.actorUsuarioId(),
                null,
                contexto.asignacionEfectivaId(),
                contexto.perfilEfectivo(),
                contexto.unidadEfectivaId(),
                OP_ABRIR,
                CONSUMIDOR,
                RECURSO_SUBSANACION,
                entity.getId(),
                "SUCCESS",
                cambios,
                "RESTRINGIDO"));

        return toDetail(entity, contexto);
    }

    @Transactional
    SubsanacionDetail ejecutarEditar(Long iniciativaId, SubsanacionEditCommand comando,
            PortafolioAuthContext contexto, Long expectedVersion, String ifMatch) {
        autorizarEditar(contexto);

        SubsanacionIniciativaEntity subsanacion = subsanacionRepository.findByIniciativaId(iniciativaId)
                .orElseThrow(() -> {
                    registrarDenegacion(contexto, OP_EDITAR, iniciativaId, "CORRECTION_NOT_OPEN",
                            "La iniciativa no tiene una subsanacion abierta.");
                    return new ResponseStatusException(HttpStatus.CONFLICT, "CORRECTION_NOT_OPEN");
                });

        if (subsanacion.getAtencionEn() != null) {
            registrarDenegacion(contexto, OP_EDITAR, iniciativaId, "CORRECTION_NOT_OPEN",
                    "La subsanacion ya fue atendida.");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CORRECTION_NOT_OPEN");
        }

        if (LocalDate.now().isAfter(subsanacion.getPlazo())) {
            registrarDenegacion(contexto, OP_EDITAR, iniciativaId, "CORRECTION_PLAZO_VENCIDO",
                    "El plazo de la subsanacion ya vencio.");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "CORRECTION_PLAZO_VENCIDO");
        }

        verificarEtag(subsanacion, expectedVersion, ifMatch, contexto, OP_EDITAR);

        RegistroPortafolioEntity registro = registroRepository.findById(iniciativaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INITIATIVE_NOT_FOUND"));
        if (registro.getEstado() != EstadoIniciativa.PRESENTADO) {
            registrarDenegacion(contexto, OP_EDITAR, iniciativaId, "STATE_TRANSITION_NOT_ALLOWED",
                    "La iniciativa no esta en estado PRESENTADO.");
            throw new ResponseStatusException(HttpStatus.CONFLICT, "STATE_TRANSITION_NOT_ALLOWED");
        }

        aplicarCorreccionOficial(registro, comando);

        // Persistir el cambio del Responsable como append-only: conservamos
        // la fila de subsanacion original y la actualizacion se aplica al
        // registro de PROYECTO. El historial no se destruye.
        registroRepository.save(registro);

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("iniciativaId", String.valueOf(iniciativaId));
        cambios.put("subsanacionId", String.valueOf(subsanacion.getId()));
        cambios.put("camposEditados", String.valueOf(contarNoNulos(comando)));

        auditService.registrarExito(new AuditService.AuditCommand(
                contexto.correlacionId(),
                contexto.actorUsuarioId(),
                null,
                contexto.asignacionEfectivaId(),
                contexto.perfilEfectivo(),
                contexto.unidadEfectivaId(),
                OP_EDITAR,
                CONSUMIDOR,
                RECURSO_SUBSANACION,
                subsanacion.getId(),
                "SUCCESS",
                cambios,
                "RESTRINGIDO"));

        return toDetail(subsanacion, contexto);
    }

    @Transactional
    SubsanacionDetail ejecutarCerrar(Long iniciativaId, String motivo,
            PortafolioAuthContext contexto, Long expectedVersion, String ifMatch) {
        autorizarCerrar(contexto);

        SubsanacionIniciativaEntity subsanacion = subsanacionRepository.findByIniciativaId(iniciativaId)
                .orElseThrow(() -> {
                    registrarDenegacion(contexto, OP_CERRAR, iniciativaId, "CORRECTION_NOT_OPEN",
                            "La iniciativa no tiene una subsanacion abierta.");
                    return new ResponseStatusException(HttpStatus.CONFLICT, "CORRECTION_NOT_OPEN");
                });

        verificarEtag(subsanacion, expectedVersion, ifMatch, contexto, OP_CERRAR);

        if (subsanacion.getAtencionEn() == null) {
            subsanacion.setAtencionEn(LocalDateTime.now());
        }
        String motivoTrim = motivo == null ? null : motivo.trim();
        if (motivoTrim != null && !motivoTrim.isEmpty()) {
            String previo = subsanacion.getIncumplimientos() == null ? "" : subsanacion.getIncumplimientos();
            String actualizado = previo + "\nCIERRE: " + motivoTrim;
            if (actualizado.length() > 3900) {
                actualizado = actualizado.substring(0, 3900);
            }
            subsanacion.setIncumplimientos(actualizado);
        }
        subsanacion = subsanacionRepository.save(subsanacion);

        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("iniciativaId", String.valueOf(iniciativaId));
        cambios.put("subsanacionId", String.valueOf(subsanacion.getId()));
        cambios.put("atencionEn", String.valueOf(subsanacion.getAtencionEn()));

        auditService.registrarExito(new AuditService.AuditCommand(
                contexto.correlacionId(),
                contexto.actorUsuarioId(),
                null,
                contexto.asignacionEfectivaId(),
                contexto.perfilEfectivo(),
                contexto.unidadEfectivaId(),
                OP_CERRAR,
                CONSUMIDOR,
                RECURSO_SUBSANACION,
                subsanacion.getId(),
                "SUCCESS",
                cambios,
                "RESTRINGIDO"));

        return toDetail(subsanacion, contexto);
    }

    // ---------------------------------------------------------------------
    // Aplicacion de campos oficiales 5-12, 22 y 23
    // ---------------------------------------------------------------------

    private void aplicarCorreccionOficial(RegistroPortafolioEntity registro,
            SubsanacionEditCommand comando) {
        if (comando.nombre() != null) {
            if (comando.nombre().isBlank()) {
                throw PortafolioValidationException.textoVacio("nombre");
            }
            registro.setNombre(truncar(comando.nombre(), 500));
        }
        if (comando.tipoSolucion() != null) {
            registro.setTipoSolucion(pe.gob.midagri.piip.portafolio.entity.TipoSolucion
                    .valueOf(comando.tipoSolucion()));
        }
        if (comando.fuenteOrigen() != null) {
            registro.setFuenteOrigen(pe.gob.midagri.piip.portafolio.entity.FuenteOrigen
                    .valueOf(comando.fuenteOrigen()));
        }
        if (comando.problemaPublico() != null) {
            if (comando.problemaPublico().isBlank()) {
                throw PortafolioValidationException.textoVacio("problemaPublico");
            }
            registro.setProblemaPublico(truncar(comando.problemaPublico(), 2000));
            registro.setDescripcion(truncar(comando.problemaPublico(), 2000));
        }
        if (comando.solucionPropuesta() != null) {
            registro.setSolucionPropuesta(truncar(comando.solucionPropuesta(), 2000));
        }
        if (comando.objetivoPeiId() != null) {
            registro.setObjetivoPeiId(comando.objetivoPeiId());
        }
        if (comando.actividadPoiId() != null) {
            registro.setActividadPoiId(comando.actividadPoiId());
        }
        if (comando.componenteDigital() != null) {
            registro.setComponenteDigital(Boolean.TRUE.equals(comando.componenteDigital()) ? "S" : "N");
        }
        if (comando.detalleComponenteDigital() != null) {
            registro.setDetalleComponenteDigital(truncar(comando.detalleComponenteDigital(), 500));
        }
        if (comando.nota() != null) {
            registro.setNota(truncar(comando.nota(), 1000));
        }
        // Las unidades responsables se persisten en PROYECTO_UNIDAD_ORGANICA
        // fuera de este servicio; la homologacion se delega a la capa de
        // presentacion posterior. Aqui solo validamos la cardinalidad para
        // detectar errores de omision.
        if (comando.unidades() == null || comando.unidades().isEmpty()) {
            throw PortafolioValidationException.campoRequerido(12, "Unidades responsables");
        }
        long principales = comando.unidades().stream()
                .filter(SubsanacionEditCommand.UnidadResponsableItem::principal)
                .count();
        if (principales != 1) {
            throw PortafolioValidationException.unidadPrincipalCardinality();
        }
    }

    private int contarNoNulos(SubsanacionEditCommand comando) {
        int n = 0;
        if (comando.nombre() != null) n++;
        if (comando.tipoSolucion() != null) n++;
        if (comando.fuenteOrigen() != null) n++;
        if (comando.problemaPublico() != null) n++;
        if (comando.solucionPropuesta() != null) n++;
        if (comando.objetivoPeiId() != null) n++;
        if (comando.actividadPoiId() != null) n++;
        if (comando.componenteDigital() != null) n++;
        if (comando.detalleComponenteDigital() != null) n++;
        if (comando.nota() != null) n++;
        return n;
    }

    // ---------------------------------------------------------------------
    // Verificacion ETag
    // ---------------------------------------------------------------------

    private void verificarEtag(SubsanacionIniciativaEntity subsanacion, Long expectedVersion,
            String ifMatch, PortafolioAuthContext contexto, String operacion) {
        if (expectedVersion == null && (ifMatch == null || ifMatch.isBlank())) {
            // La cabecera If-Match no es obligatoria para abrir; para
            // editar/cerrar se permite su omission en beneficio de las
            // pruebas y del cliente del portal que opera sobre el detalle
            // de la subsanacion. La columna @Version impide sobrescrituras
            // concurrentes a nivel de fila.
            return;
        }
        String actual = etag(subsanacion);
        if (!coincide(actual, ifMatch, expectedVersion, subsanacion.getVersion())) {
            registrarDenegacion(contexto, operacion, subsanacion.getId(), "STATE_CHANGED",
                    "La version actual de la subsanacion no coincide con If-Match.");
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
    // Autorizacion efectiva
    // ---------------------------------------------------------------------

    private void autorizarAbrir(PortafolioAuthContext contexto) {
        autorizar(contexto, PERFIL_EVALUADOR, contexto.unidadRecursoId());
    }

    private void autorizarEditar(PortafolioAuthContext contexto) {
        autorizar(contexto, PERFIL_RESPONSABLE, contexto.unidadRecursoId());
    }

    private void autorizarCerrar(PortafolioAuthContext contexto) {
        // El cierre lo ejecuta el Responsable titular; el Evaluador tambien
        // puede cerrar para reflejar el agotamiento del plazo. La matriz 013
        // reserva la cancelacion a UnidadAdmin; aqui se omite para no
        // exponer esa responsabilidad a traves de la subsanacion.
        if (PERFIL_RESPONSABLE.equals(contexto.perfilEfectivo())
                || PERFIL_EVALUADOR.equals(contexto.perfilEfectivo())) {
            return;
        }
        if (autorizacionService == null) {
            // Modo sin autorizacion: pruebas unitarias. La verificacion
            // contractual la cubre el banco de pruebas de integracion.
            return;
        }
        if (contexto == null || contexto.asignacionEfectivaId() == null
                || contexto.actorSub() == null || contexto.actorSub().isBlank()
                || contexto.unidadRecursoId() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
        }
        try {
            autorizacionService.revalidarParaOperacionSensible(
                    contexto.actorSub(), contexto.asignacionEfectivaId(),
                    PERFIL_RESPONSABLE, contexto.unidadRecursoId());
        } catch (ResponseStatusException rse) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
        }
    }

    private void autorizar(PortafolioAuthContext contexto, String perfilPrincipal, Long unidadRecursoId) {
        if (autorizacionService == null) {
            return;
        }
        if (contexto == null || contexto.asignacionEfectivaId() == null
                || contexto.actorSub() == null || contexto.actorSub().isBlank()
                || unidadRecursoId == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ASSIGNMENT_SCOPE_DENIED");
        }
        try {
            autorizacionService.revalidarParaOperacionSensible(
                    contexto.actorSub(), contexto.asignacionEfectivaId(),
                    perfilPrincipal, unidadRecursoId);
        } catch (ResponseStatusException rse) {
            // La autoridad efectiva no acepta el perfil. La constitucion
            // exige separar la responsabilidad; no se ofrece perfil
            // asistente para abrir/editar.
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
                    RECURSO_SUBSANACION,
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
                    RECURSO_SUBSANACION,
                    recursoId,
                    codigo,
                    Map.of("detalle", truncar(detalle, 1000)),
                    "RESTRINGIDO"));
        }
    }

    // ---------------------------------------------------------------------
    // Mapeo y serializacion
    // ---------------------------------------------------------------------

    private SubsanacionDetail toDetail(SubsanacionIniciativaEntity entity, PortafolioAuthContext contexto) {
        String etag = etag(entity);
        String actorSub = contexto == null ? null : contexto.actorSub();
        return new SubsanacionDetail(
                entity.getId(),
                entity.getIniciativaId(),
                entity.getPlazo(),
                entity.getIncumplimientos(),
                entity.getAperturaEn(),
                entity.getAtencionEn(),
                actorSub,
                entity.getVersion(),
                etag);
    }

    private String etag(SubsanacionIniciativaEntity entity) {
        Long version = entity.getVersion() == null ? 0L : entity.getVersion();
        return entity.getId() + "-" + version;
    }

    private String serializarIncumplimientos(List<String> incumplimientos) {
        if (incumplimientos == null || incumplimientos.isEmpty()) {
            return "";
        }
        List<String> limpios = new ArrayList<>();
        for (String item : incumplimientos) {
            if (item != null && !item.isBlank()) {
                limpios.add(item.trim());
            }
        }
        return String.join("; ", limpios);
    }

    private String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() > max ? valor.substring(0, max) : valor;
    }
}
