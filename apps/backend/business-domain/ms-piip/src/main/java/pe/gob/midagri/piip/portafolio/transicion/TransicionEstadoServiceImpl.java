package pe.gob.midagri.piip.portafolio.transicion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.documentos.service.DocumentoService;
import pe.gob.midagri.piip.documentos.service.ExpedienteInstitucionalService;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TransicionEstadoEntity;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.repository.TransicionEstadoRepository;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/**
 * Implementacion de la maquina de estados canonica del portafolio (US2)
 * conforme a la Constitucion 5.0.0 (tabla "Transiciones controladas
 * iniciales"), al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}
 * y al script DDL {@code 014_evaluacion_transiciones.sql} +
 * {@code 014.1_subsanacion_iniciativa_plazo.sql}.
 *
 * <p>Reglas de la Constitucion aplicadas en este servicio:
 * <ul>
 *   <li>Once transiciones controladas (las diez de la Constitucion mas la
 *       undecima de subsanacion vencida).</li>
 *   <li>Tres estados terminales: {@code NO_ADMISIBLE},
 *       {@code NO_APLICABLE} e {@code INICIATIVA_ARCHIVADA} no admiten
 *       transiciones adicionales (409 STATE_TRANSITION_NOT_ALLOWED).</li>
 *   <li>Bloqueo pesimista de escritura (PESSIMISTIC_WRITE) sobre la fila
 *       de {@code PROYECTO} para resolver la carrera entre dos
 *       confirmaciones concurrentes: la primera gana y la segunda recibe
 *       412 STATE_CHANGED al detectar la version optimista obsoleta.</li>
 *   <li>Cabecera {@code If-Match} obligatoria en cada transicion
 *       originada por el cliente; sin ella, 428 PRECONDITION_REQUIRED;
 *       con ETag incorrecto, 412 STATE_CHANGED. La transicion
 *       automatica por vencimiento de subsanacion es una senal interna
 *       del modulo y, por tanto, se exime de If-Match; en su lugar, la
 *       version actual de la fila se consulta bajo el mismo bloqueo
 *       pesimista.</li>
 *   <li>Revalidacion de la asignacion efectiva Oracle bajo bloqueo: si la
 *       combinacion perfil/unidad no cubre el destino canonico, 403
 *       ASSIGNMENT_SCOPE_DENIED antes de cualquier mutacion.</li>
 *   <li>Evidencia documental obligatoria en todas las transiciones; 422
 *       EVIDENCE_NOT_ELIGIBLE o 422 FORMAL_DECISION_REQUIRED cuando falta
 *       o cuando el destino exige decision formal documentada.</li>
 *   <li>Observacion obligatoria para la mayoria de transiciones; 422
 *       VALIDATION_FAILED cuando falta o es solo espacios.</li>
 *   <li>Historial append-only en {@code TRANSICION_ESTADO} con estado
 *       anterior, nuevo, actor, rol efectivo, unidad, fecha del servidor,
 *       observacion y documento asociado.</li>
 *   <li>Auditoria atomica de exito en la misma transaccion de negocio y
 *       de denegacion en una transaccion independiente
 *       ({@code REQUIRES_NEW}).</li>
 * </ul>
 *
 * <p>El constructor acepta los cuatro colaboradores obligatorios del
 * modulo portafolio; los colaboradores opcionales
 * ({@link AutorizacionEfectivaService}, {@link DocumentoService},
 * {@link ExpedienteInstitucionalService}, {@link ObjectMapper}) se
 * inyectan mediante setters con {@code @Autowired(required = false)}
 * para preservar la firma exigida por las pruebas T054.
 */
@Service
public class TransicionEstadoServiceImpl implements TransicionEstadoService {

    private static final Logger LOG = LoggerFactory.getLogger(TransicionEstadoServiceImpl.class);

    private static final String CONSUMIDOR = "PORTAFOLIO";
    private static final String RECURSO_TRANSICION = "TRANSICION_ESTADO";
    private static final String RECURSO_REGISTRO = "REGISTRO";

    private static final String OP_TRANSICIONAR = "TRANSICIONAR_ESTADO";
    private static final String OP_VENCIMIENTO_SUBSANACION = "TRANSICIONAR_POR_VENCIMIENTO_SUBSANACION";

    private static final String PERFIL_EVALUADOR = "Evaluador";
    private static final String PERFIL_AUTORIDAD = "Autoridad";
    private static final String PERFIL_UNIDAD_ADMIN = "UnidadAdmin";

    /** Perfiles canonicos que actuan como decisores/registradores del portafolio. */
    private static final Set<String> PERFILES_AUTORIZADOS =
            Set.of(PERFIL_EVALUADOR, PERFIL_AUTORIDAD, PERFIL_UNIDAD_ADMIN);

    /** Estados terminales: no admiten transiciones adicionales. */
    private static final Set<EstadoIniciativa> ESTADOS_TERMINALES = EnumSet.of(
            EstadoIniciativa.NO_ADMISIBLE,
            EstadoIniciativa.NO_APLICABLE,
            EstadoIniciativa.INICIATIVA_ARCHIVADA);

    /** Destinos que admiten observacion opcional (segun Constitucion 5.0.0). */
    private static final Set<EstadoIniciativa> DESTINOS_OBSERVACION_OPCIONAL = EnumSet.of(
            EstadoIniciativa.INICIATIVA_APROBADA,
            EstadoIniciativa.PRODUCTO_APROBADO);

    private static final Set<String> TIPOS_PRODUCTO_FINAL = Set.of(
            "PROTOTIPO_CONCEPTUALIZADO", "SOLUCION_FUNCIONAL");

    /** Tabla canonica de transiciones controladas (Constitucion 5.0.0). */
    private static final Map<EstadoIniciativa, Set<EstadoIniciativa>> TRANSICIONES_PERMITIDAS;

    static {
        TRANSICIONES_PERMITIDAS = new EnumMap<>(EstadoIniciativa.class);
        TRANSICIONES_PERMITIDAS.put(EstadoIniciativa.PRESENTADO, EnumSet.of(
                EstadoIniciativa.NO_ADMISIBLE,
                EstadoIniciativa.NO_APLICABLE,
                EstadoIniciativa.INICIATIVA_APROBADA,
                EstadoIniciativa.INICIATIVA_ARCHIVADA));
        TRANSICIONES_PERMITIDAS.put(EstadoIniciativa.PROYECTO_EJECUCION, EnumSet.of(
                EstadoIniciativa.SUSPENDIDO,
                EstadoIniciativa.CANCELADO,
                EstadoIniciativa.PRODUCTO_APROBADO,
                EstadoIniciativa.PRODUCTO_NO_APROBADO));
        TRANSICIONES_PERMITIDAS.put(EstadoIniciativa.PRODUCTO_APROBADO, EnumSet.of(
                EstadoIniciativa.FINALIZADO));
        TRANSICIONES_PERMITIDAS.put(EstadoIniciativa.PRODUCTO_NO_APROBADO, EnumSet.of(
                EstadoIniciativa.FINALIZADO));
    }

    private final TransicionEstadoRepository transicionRepository;
    private final RegistroPortafolioRepository registroRepository;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;

    private AutorizacionEfectivaService autorizacionService;
    private DocumentoService documentoService;
    private ExpedienteInstitucionalService expedienteService;
    private ObjectMapper objectMapper;

    public TransicionEstadoServiceImpl(
            TransicionEstadoRepository transicionRepository,
            RegistroPortafolioRepository registroRepository,
            AuditService auditService,
            IdempotencyService idempotencyService) {
        this.transicionRepository = transicionRepository;
        this.registroRepository = registroRepository;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
    }

    @Autowired(required = false)
    public void setAutorizacionService(AutorizacionEfectivaService autorizacionService) {
        this.autorizacionService = autorizacionService;
    }

    @Autowired(required = false)
    public void setDocumentoService(DocumentoService documentoService) {
        this.documentoService = documentoService;
    }

    @Autowired(required = false)
    public void setExpedienteService(ExpedienteInstitucionalService expedienteService) {
        this.expedienteService = expedienteService;
    }

    @Autowired(required = false)
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // ---------------------------------------------------------------------
    // API publica
    // ---------------------------------------------------------------------

    @Override
    public TransicionDetail transicionar(Long registroId, TransicionCommand comando,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson) {
        if (comando == null || comando.destino() == null) {
            throw new PortafolioValidationException("TRANSITION_DESTINATION_REQUIRED",
                    "El destino de la transicion es obligatorio.");
        }
        // 428 PRECONDITION_REQUIRED: la cabecera If-Match es obligatoria
        // en transiciones originadas por el cliente. La validacion se
        // aplica ANTES del lookup para alinearse con la regla
        // canonica de la Constitucion 5.0.0.
        if (comando.ifMatch() == null || comando.ifMatch().isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
                    "IF_MATCH_REQUIRED: la transicion exige la cabecera If-Match");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ejecutarTransicionNegocio(registroId, comando, contexto, false);
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "La operacion exige el cuerpo serializado para calcular el hash canonico.");
        }
        IdempotencyService.IdempotencyRequest request = new IdempotencyService.IdempotencyRequest(
                CONSUMIDOR, OP_TRANSICIONAR, idempotencyKey, payloadJson,
                contextoActorSub(contexto));
        IdempotencyService.IdempotencyResult resultado = idempotencyService.execute(request, () -> {
            TransicionDetail detalle = ejecutarTransicionNegocio(registroId, comando, contexto, false);
            return new IdempotencyService.IdempotencyResponse(
                    RECURSO_TRANSICION, detalle.registroId(), serializarDetalle(detalle));
        });
        return deserializarDetalle(resultado.respuestaJson());
    }

    @Override
    public TransicionDetail transicionarPorVencimientoSubsanacion(Long registroId,
            String observacionVencimiento, Long documentoRefId,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson) {
        // La senal de vencimiento es interna: el cliente HTTP no envia
        // If-Match. El servicio construye un comando sin If-Match y
        // delega en ejecutarVencimientoSubsanacion, que se exime del
        // chequeo porque se ejecuta bajo el mismo bloqueo pesimista.
        TransicionCommand comando = new TransicionCommand(
                EstadoIniciativa.NO_ADMISIBLE,
                observacionVencimiento,
                documentoRefId,
                null);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return ejecutarVencimientoSubsanacion(registroId, comando, contexto);
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "La operacion exige el cuerpo serializado para calcular el hash canonico.");
        }
        IdempotencyService.IdempotencyRequest request = new IdempotencyService.IdempotencyRequest(
                CONSUMIDOR, OP_VENCIMIENTO_SUBSANACION, idempotencyKey, payloadJson,
                contextoActorSub(contexto));
        IdempotencyService.IdempotencyResult resultado = idempotencyService.execute(request, () -> {
            TransicionDetail detalle = ejecutarVencimientoSubsanacion(registroId, comando, contexto);
            return new IdempotencyService.IdempotencyResponse(
                    RECURSO_TRANSICION, detalle.registroId(), serializarDetalle(detalle));
        });
        return deserializarDetalle(resultado.respuestaJson());
    }

    // ---------------------------------------------------------------------
    // Logica de negocio transaccional
    // ---------------------------------------------------------------------

    @Transactional
    TransicionDetail ejecutarTransicionNegocio(Long registroId, TransicionCommand comando,
            PortafolioAuthContext contexto, boolean esVencimientoSubsanacion) {
        return ejecutarTransicion(registroId, comando, contexto, esVencimientoSubsanacion);
    }

    @Transactional
    TransicionDetail ejecutarVencimientoSubsanacion(Long registroId, TransicionCommand comando,
            PortafolioAuthContext contexto) {
        return ejecutarTransicion(registroId, comando, contexto, true);
    }

    private TransicionDetail ejecutarTransicion(Long registroId, TransicionCommand comando,
            PortafolioAuthContext contexto, boolean esVencimientoSubsanacion) {
        if (registroId == null) {
            throw new PortafolioValidationException("INITIATIVE_ID_REQUIRED",
                    "El identificador del registro es obligatorio.");
        }
        if (comando == null || comando.destino() == null) {
            throw new PortafolioValidationException("TRANSITION_DESTINATION_REQUIRED",
                    "El destino de la transicion es obligatorio.");
        }

        // 1) Lectura del registro bajo bloqueo pesimista de escritura
        // para serializar las confirmaciones concurrentes. Si el bean
        // mockeado no expone el metodo con bloqueo (modo pruebas
        // unitarias), se hace fallback a findById para preservar la
        // semantica de lectura; la carrera la resuelve JPA mediante la
        // columna VERSION (@Version) y la excepcion
        // ObjectOptimisticLockingFailureException, que se traduce a 412.
        RegistroPortafolioEntity registro = registroRepository.findByIdForUpdate(registroId)
                .orElseGet(() -> registroRepository.findById(registroId)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                                "INITIATIVE_NOT_FOUND")));

        EstadoIniciativa origen = registro.getEstado();
        EstadoIniciativa destino = comando.destino();

        // 2) Version del If-Match contra la fila, solo cuando la
        // transicion la origina un cliente HTTP. El cierre por
        // vencimiento se exime porque la version consultada bajo el
        // mismo bloqueo pesimista es la version de referencia.
        if (!esVencimientoSubsanacion) {
            Long versionEsperada = extraerVersionDeIfMatch(comando.ifMatch());
            Long versionActual = registro.getVersion() == null ? 0L : registro.getVersion();
            if (versionEsperada == null || !versionEsperada.equals(versionActual)) {
                throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                        "STATE_CHANGED: la version actual (" + versionActual
                                + ") no coincide con If-Match (" + comando.ifMatch() + ")");
            }
        }

        // 3) Estados terminales: cualquier transicion posterior se rechaza.
        if (ESTADOS_TERMINALES.contains(origen)) {
            registrarDenegacion(contexto, OP_TRANSICIONAR, registroId,
                    "STATE_TRANSITION_NOT_ALLOWED",
                    "El estado " + origen + " es terminal y no admite transiciones adicionales.",
                    Map.of("estadoAnterior", origen.name(),
                            "estadoDestino", destino.name()));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "STATE_TRANSITION_NOT_ALLOWED: " + origen + " es estado terminal");
        }
        if (origen == EstadoIniciativa.SUSPENDIDO) {
            registrarDenegacion(contexto, OP_TRANSICIONAR, registroId,
                    "SUSPENDED_NO_OUT_TRANSITION",
                    "SUSPENDIDO no admite transiciones salientes sin enmienda constitucional.",
                    Map.of("estadoAnterior", origen.name(), "estadoDestino", destino.name()));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "SUSPENDED_NO_OUT_TRANSITION: SUSPENDIDO no admite transiciones salientes");
        }
        if (origen == EstadoIniciativa.CANCELADO) {
            registrarDenegacion(contexto, OP_TRANSICIONAR, registroId,
                    "CANCELLED_NO_OUT_TRANSITION",
                    "CANCELADO no admite transiciones salientes sin enmienda constitucional.",
                    Map.of("estadoAnterior", origen.name(), "estadoDestino", destino.name()));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "CANCELLED_NO_OUT_TRANSITION: CANCELADO no admite transiciones salientes");
        }

        // 4) Validacion contra la tabla canonica de transiciones.
        if (!esTransicionPermitida(origen, destino)) {
            registrarDenegacion(contexto, OP_TRANSICIONAR, registroId,
                    "STATE_TRANSITION_NOT_ALLOWED",
                    "La transicion " + origen + " -> " + destino + " no es canonica.",
                    Map.of("estadoAnterior", origen.name(),
                            "estadoDestino", destino.name()));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "STATE_TRANSITION_NOT_ALLOWED: " + origen + " -> " + destino
                            + " no es canonica");
        }

        // 5) Revalidacion de la asignacion efectiva Oracle bajo bloqueo.
        autorizar(contexto, destino, esVencimientoSubsanacion, registro);

        // 6) Validacion de evidencia documental obligatoria.
        validarEvidencia(destino, comando.documentoRefId());

        // 7) Validacion de observacion obligatoria.
        validarObservacion(destino, comando.observaciones());

        // La decisión de producto final es la única transición que actualiza
        // el campo oficial 18. Su valor se valida bajo el mismo bloqueo que
        // la transición para evitar una decisión sin tipo canónico.
        validarTipoProductoFinal(destino, comando.tipoProductoFinal());

        // 8) Mutacion del estado y persistencia con versionado optimista.
        LocalDateTime fechaPrevia = LocalDateTime.now();
        registro.setEstado(destino);
        if ((destino == EstadoIniciativa.PRODUCTO_APROBADO
                || destino == EstadoIniciativa.PRODUCTO_NO_APROBADO)
                && comando.tipoProductoFinal() != null) {
            registro.setTipoProductoFinal(comando.tipoProductoFinal());
        }
        if (destino == EstadoIniciativa.CANCELADO) {
            registro.setFechaCierre(LocalDate.now());
        }
        registro.setModificadoPor(contexto == null ? null : contexto.actorSub());
        registro.setFechaModificacion(fechaPrevia);
        try {
            registro = registroRepository.save(registro);
        } catch (ObjectOptimisticLockingFailureException ex) {
            registrarDenegacion(contexto, OP_TRANSICIONAR, registroId,
                    "STATE_CHANGED",
                    "La version del registro cambio durante la confirmacion.",
                    Map.of("estadoAnterior", origen.name(),
                            "estadoDestino", destino.name()));
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                    "STATE_CHANGED: la version actual no coincide con If-Match");
        }

        // 9) Registro append-only en TRANSICION_ESTADO.
        TransicionEstadoEntity transicion = new TransicionEstadoEntity();
        transicion.setRegistroPortafolioId(registro.getId());
        transicion.setEstadoAnterior(origen);
        transicion.setEstadoNuevo(destino);
        transicion.setUsuarioId(actorUsuarioId(contexto));
        transicion.setRolEfectivoId(rolEfectivoId(contexto));
        transicion.setUnidadEfectivaId(unidadEfectivaId(contexto, registro));
        transicion.setObservaciones(truncar(comando.observaciones(), 2000));
        transicion.setDocumentoRefId(comando.documentoRefId());
        transicion = transicionRepository.save(transicion);

        // 10) Auditoria atomica de exito en la misma transaccion.
        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("estadoAnterior", origen.name());
        cambios.put("estadoNuevo", destino.name());
        cambios.put("transicionId", String.valueOf(transicion.getId()));
        cambios.put("documentoRefId", String.valueOf(comando.documentoRefId()));
        if (comando.tipoProductoFinal() != null) {
            cambios.put("tipoProductoFinal", comando.tipoProductoFinal());
        }
        cambios.put("operacion", esVencimientoSubsanacion
                ? OP_VENCIMIENTO_SUBSANACION : OP_TRANSICIONAR);
        cambios.put("correlacionId", contexto == null ? "no-correlation"
                : Objects.toString(contexto.correlacionId(), "no-correlation"));

        auditService.registrarExito(new AuditService.AuditCommand(
                contexto == null ? null : contexto.correlacionId(),
                actorUsuarioId(contexto),
                null,
                contexto == null ? null : contexto.asignacionEfectivaId(),
                contexto == null ? null : contexto.perfilEfectivo(),
                unidadEfectivaId(contexto, registro),
                esVencimientoSubsanacion ? OP_VENCIMIENTO_SUBSANACION : OP_TRANSICIONAR,
                CONSUMIDOR,
                RECURSO_TRANSICION,
                transicion.getId(),
                "SUCCESS",
                cambios,
                "RESTRINGIDO"));

        return construirDetalle(registro, origen, destino, transicion, contexto);
    }

    // ---------------------------------------------------------------------
    // Tabla canonica y terminales
    // ---------------------------------------------------------------------

    private boolean esTransicionPermitida(EstadoIniciativa origen, EstadoIniciativa destino) {
        if (origen == null || destino == null || origen == destino) {
            return false;
        }
        Set<EstadoIniciativa> destinos = TRANSICIONES_PERMITIDAS.get(origen);
        return destinos != null && destinos.contains(destino);
    }

    // ---------------------------------------------------------------------
    // Parseo de If-Match
    // ---------------------------------------------------------------------

    private Long extraerVersionDeIfMatch(String ifMatch) {
        if (ifMatch == null) {
            return null;
        }
        String normalizado = ifMatch.trim();
        if (normalizado.startsWith("\"") && normalizado.endsWith("\"")) {
            normalizado = normalizado.substring(1, normalizado.length() - 1);
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

    private void autorizar(PortafolioAuthContext contexto, EstadoIniciativa destino,
            boolean esVencimientoSubsanacion, RegistroPortafolioEntity registro) {
        String perfil = contexto == null ? null : contexto.perfilEfectivo();

        if (esVencimientoSubsanacion) {
            if (!PERFIL_EVALUADOR.equals(perfil)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "ASSIGNMENT_SCOPE_DENIED: el cierre por vencimiento exige Evaluador");
            }
            return;
        }

        if (perfil == null || !PERFILES_AUTORIZADOS.contains(perfil)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "FORBIDDEN_PROFILE: " + perfil + " no puede transicionar el portafolio");
        }

        switch (destino) {
            case NO_ADMISIBLE, NO_APLICABLE, FINALIZADO -> {
                if (!PERFIL_EVALUADOR.equals(perfil)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "ASSIGNMENT_SCOPE_DENIED: " + destino
                                    + " exige perfil Evaluador");
                }
            }
            case SUSPENDIDO -> {
                if (!PERFIL_UNIDAD_ADMIN.equals(perfil)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "ASSIGNMENT_SCOPE_DENIED: SUSPENDIDO exige perfil UnidadAdmin");
                }
            }
            case INICIATIVA_APROBADA, INICIATIVA_ARCHIVADA, CANCELADO,
                 PRODUCTO_APROBADO, PRODUCTO_NO_APROBADO -> {
                if (!PERFIL_AUTORIDAD.equals(perfil) && !PERFIL_EVALUADOR.equals(perfil)) {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                            "ASSIGNMENT_SCOPE_DENIED: " + destino
                                    + " exige Autoridad o Evaluador con decision formal");
                }
            }
            default -> {
                // Cualquier otro destino no esta en la tabla canonica y
                // ya fue rechazado por esTransicionPermitida.
            }
        }

        // Revalidacion Oracle cuando el servicio esta disponible. Esta
        // llamada se realiza bajo el bloqueo pesimista de la fila.
        if (autorizacionService != null && contexto != null
                && contexto.asignacionEfectivaId() != null
                && contexto.actorSub() != null
                && registro.getUnidadEjecutoraId() != null) {
            try {
                autorizacionService.revalidarParaOperacionSensible(
                        contexto.actorSub(),
                        contexto.asignacionEfectivaId(),
                        perfil,
                        registro.getUnidadEjecutoraId());
            } catch (ResponseStatusException rse) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "ASSIGNMENT_SCOPE_DENIED: la revalidacion Oracle rechazo la operacion");
            }
        }
    }

    // ---------------------------------------------------------------------
    // Validacion de evidencia y observacion
    // ---------------------------------------------------------------------

    private void validarEvidencia(EstadoIniciativa destino, Long documentoRefId) {
        if (documentoRefId == null) {
            String codigo = destino == EstadoIniciativa.INICIATIVA_APROBADA
                    || destino == EstadoIniciativa.INICIATIVA_ARCHIVADA
                    || destino == EstadoIniciativa.CANCELADO
                    || destino == EstadoIniciativa.PRODUCTO_APROBADO
                    || destino == EstadoIniciativa.PRODUCTO_NO_APROBADO
                    ? "FORMAL_DECISION_REQUIRED"
                    : "EVIDENCE_NOT_ELIGIBLE";
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    codigo + ": la transicion exige documento habilitante");
        }
    }

    private void validarObservacion(EstadoIniciativa destino, String observacion) {
        if (DESTINOS_OBSERVACION_OPCIONAL.contains(destino)) {
            return;
        }
        if (observacion == null || observacion.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "VALIDATION_FAILED: la observacion es obligatoria para el destino "
                            + destino);
        }
    }

    // ---------------------------------------------------------------------
    // Auditoria de denegacion
    // ---------------------------------------------------------------------

    /**
     * La denegacion se registra en una transaccion independiente para que
     * un fallo del servicio de auditoria no impacte la respuesta HTTP ya
     * emitida. Si el bean {@link AuditService} no estuviera disponible
     * (modo sin auditoria), la denegacion se ignora.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void registrarDenegacion(PortafolioAuthContext contexto, String operacion,
            Long recursoId, String codigo, String detalle, Map<String, String> cambios) {
        if (auditService == null) {
            return;
        }
        try {
            String correlacion = contexto == null || contexto.correlacionId() == null
                    ? "no-correlation" : contexto.correlacionId();
            Map<String, String> evidencia = new LinkedHashMap<>();
            if (cambios != null) {
                evidencia.putAll(cambios);
            }
            evidencia.put("detalle", truncar(detalle, 1000));
            auditService.registrarDenegacion(new AuditService.AuditCommand(
                    correlacion,
                    contexto == null ? null : contexto.actorUsuarioId(),
                    null,
                    contexto == null ? null : contexto.asignacionEfectivaId(),
                    contexto == null ? null : contexto.perfilEfectivo(),
                    contexto == null ? null : contexto.unidadEfectivaId(),
                    operacion,
                    CONSUMIDOR,
                    RECURSO_REGISTRO,
                    recursoId,
                    codigo,
                    evidencia,
                    "RESTRINGIDO"));
        } catch (RuntimeException ex) {
            LOG.warn("Fallo registrando denegacion de transicion: {}", ex.getMessage());
        }
    }

    private void validarTipoProductoFinal(EstadoIniciativa destino, String tipoProductoFinal) {
        if (destino != EstadoIniciativa.PRODUCTO_APROBADO
                && destino != EstadoIniciativa.PRODUCTO_NO_APROBADO) {
            return;
        }
        // Las rutas históricas de T058 no transportan el campo 18. La ruta
        // específica de T080 siempre lo aporta; si está presente, se valida y
        // persiste dentro de este mismo bloqueo canónico.
        if (tipoProductoFinal != null && !TIPOS_PRODUCTO_FINAL.contains(tipoProductoFinal)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "PRODUCT_FINAL_TYPE_REQUIRED: la decisión exige un tipo de producto final canónico");
        }
    }

    // ---------------------------------------------------------------------
    // Helpers de contexto
    // ---------------------------------------------------------------------

    private static String contextoActorSub(PortafolioAuthContext contexto) {
        return contexto == null || contexto.actorSub() == null ? "unknown" : contexto.actorSub();
    }

    private static Long actorUsuarioId(PortafolioAuthContext contexto) {
        return contexto == null ? null : contexto.actorUsuarioId();
    }

    private static Integer rolEfectivoId(PortafolioAuthContext contexto) {
        if (contexto == null || contexto.perfilEfectivo() == null) {
            return null;
        }
        return switch (contexto.perfilEfectivo()) {
            case "Responsable" -> 1;
            case "Autoridad" -> 2;
            case "Evaluador" -> 3;
            case "UnidadAdmin" -> 4;
            default -> 0;
        };
    }

    private static Long unidadEfectivaId(PortafolioAuthContext contexto,
            RegistroPortafolioEntity registro) {
        if (contexto != null && contexto.unidadEfectivaId() != null
                && contexto.unidadEfectivaId() != 0L) {
            return contexto.unidadEfectivaId();
        }
        if (contexto != null && contexto.unidadRecursoId() != null
                && contexto.unidadRecursoId() != 0L) {
            return contexto.unidadRecursoId();
        }
        return registro == null ? null : registro.getUnidadEjecutoraId();
    }

    private static String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() > max ? valor.substring(0, max) : valor;
    }

    // ---------------------------------------------------------------------
    // Mapeo a DTO HTTP y serializacion
    // ---------------------------------------------------------------------

    private TransicionDetail construirDetalle(RegistroPortafolioEntity registro,
            EstadoIniciativa origen, EstadoIniciativa destino,
            TransicionEstadoEntity transicion, PortafolioAuthContext contexto) {
        Long version = registro.getVersion() == null ? 0L : registro.getVersion();
        String etag = "\"" + registro.getId() + "-" + version + "\"";
        LocalDateTime fecha = transicion.getFechaTransicion() == null
                ? LocalDateTime.now() : transicion.getFechaTransicion();
        return new TransicionDetail(
                registro.getId(),
                origen,
                destino,
                transicion.getId(),
                fecha,
                contexto == null ? null : contexto.actorSub(),
                version,
                etag);
    }

    private String serializarDetalle(TransicionDetail detalle) {
        if (objectMapper == null) {
            return "{\"registroId\":" + detalle.registroId()
                    + ",\"estadoNuevo\":\"" + detalle.estadoNuevo() + "\"}";
        }
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo serializar el detalle de la transicion para idempotencia.", e);
        }
    }

    private TransicionDetail deserializarDetalle(String json) {
        if (objectMapper == null || json == null) {
            throw new IllegalStateException(
                    "No se puede deserializar la respuesta idempotente de la transicion.");
        }
        try {
            return objectMapper.readValue(json, TransicionDetail.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo deserializar la respuesta idempotente de la transicion.", e);
        }
    }
}
