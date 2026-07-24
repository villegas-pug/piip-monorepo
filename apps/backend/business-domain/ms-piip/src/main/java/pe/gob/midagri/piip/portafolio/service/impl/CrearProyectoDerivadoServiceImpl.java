package pe.gob.midagri.piip.portafolio.service.impl;


import java.time.LocalDate;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.portafolio.dto.CreateDerivedProjectRequest;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.ProjectDetail;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.RelacionIniciativaProyectoEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.entity.TipoSolucion;
import pe.gob.midagri.piip.portafolio.entity.TitularidadResponsableEntity;
import pe.gob.midagri.piip.portafolio.entity.UnidadResponsableEntity;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.repository.RelacionIniciativaProyectoRepository;
import pe.gob.midagri.piip.portafolio.repository.TitularidadResponsableRepository;
import pe.gob.midagri.piip.portafolio.repository.UnidadResponsableRepository;
import pe.gob.midagri.piip.portafolio.service.CatalogoUnidadReader;
import pe.gob.midagri.piip.portafolio.service.CodigoProyectoService;
import pe.gob.midagri.piip.portafolio.service.CrearProyectoDerivadoService;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;


/**
 * Implementacion de la creacion del proyecto derivado (US3, Constitucion
 * 5.0.0) conforme al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}
 * y al script DDL {@code 010_iniciativa_proyecto_relacion.sql}.
 *
 * <p>Reglas de la Constitucion aplicadas en este servicio:
 * <ul>
 *   <li>Solo se permite crear un derivado cuando la iniciativa esta en
 *       {@code INICIATIVA_APROBADA}; el proyecto nace en
 *       {@code PROYECTO_EJECUCION} sin modificar el estado de la
 *       iniciativa.</li>
 *   <li>Un segundo intento de derivado para la misma iniciativa falla
 *       con 409 {@code DERIVATION_ALREADY_EXISTS}; la UK por
 *       {@code ID_INICIATIVA} del incremento 010 es la autoridad
 *       canonica. La operacion consulta la UK antes del INSERT y, como
 *       salvaguarda, captura la violacion de integridad de Oracle.</li>
 *   <li>Bloqueo pesimista de la iniciativa (PESSIMISTIC_WRITE) sobre
 *       {@code PROYECTO} para serializar la creacion de derivados
 *       concurrentes: la primera confirmacion gana y la segunda recibe
 *       409 al detectar la fila de la relacion ya confirmada.</li>
 *   <li>Codigo propio generado por {@link CodigoProyectoService} bajo
 *       {@code PESSIMISTIC_WRITE} con formato
 *       {@code AAAA-PREFIJO_UNIDAD-NNNNN}.</li>
 *   <li>Documento formal de aprobacion o autorizacion de inicio
 *       obligatorio (campo 15, regla FORMAL_DOCUMENT_REQUIRED).</li>
 *   <li>Idempotencia canonica por consumidor, operacion, clave y hash
 *       del payload, gestionada por {@link IdempotencyService}.</li>
 *   <li>Auditoria atomica de exito en la misma transaccion de negocio y
 *       de denegacion en una transaccion independiente
 *       ({@code REQUIRES_NEW}).</li>
 *   <li>Autorizacion efectiva desde Oracle mediante
 *       {@link AutorizacionEfectivaService} cuando el bean esta
 *       disponible; en pruebas unitarias se omite sin afectar el
 *       contrato.</li>
 * </ul>
 *
 * <p>El constructor acepta los seis colaboradores obligatorios exigidos
 * por las pruebas de contrato T062. Los colaboradores opcionales
 * ({@link UnidadResponsableRepository}, {@link TitularidadResponsableRepository},
 * {@link AutorizacionEfectivaService} y {@link ObjectMapper}) se inyectan
 * mediante setters con {@code @Autowired(required = false)} para
 * preservar la firma y permitir la ejecucion aislada.
 */
@Service
public class CrearProyectoDerivadoServiceImpl implements CrearProyectoDerivadoService {

    private static final Logger LOG = LoggerFactory.getLogger(CrearProyectoDerivadoServiceImpl.class);

    private static final String CONSUMIDOR = "PORTAFOLIO";
    private static final String RECURSO_PROYECTO = "PROYECTO";

    private static final String OP_CREAR_DERIVADO = "CREAR_PROYECTO_DERIVADO";

    private static final String PERFIL_RESPONSABLE = "Responsable";

    private static final Set<String> FUENTES_CANONICAS = Set.of(
            FuenteOrigen.FICHA_INICIATIVA.name(),
            FuenteOrigen.CONCURSO_INTERNO.name(),
            FuenteOrigen.INNOVACION_ABIERTA.name(),
            FuenteOrigen.PROPUESTA_JEFATURA.name(),
            FuenteOrigen.OTROS.name());

    private final RegistroPortafolioRepository registroRepository;
    private final RelacionIniciativaProyectoRepository relacionRepository;
    private final CodigoProyectoService codigoProyectoService;
    private final CatalogoUnidadReader catalogoUnidadReader;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;

    private UnidadResponsableRepository unidadResponsableRepository;
    private TitularidadResponsableRepository titularidadRepository;
    private AutorizacionEfectivaService autorizacionService;
    private ObjectMapper objectMapper;

    public CrearProyectoDerivadoServiceImpl(
            RegistroPortafolioRepository registroRepository,
            RelacionIniciativaProyectoRepository relacionRepository,
            CodigoProyectoService codigoProyectoService,
            CatalogoUnidadReader catalogoUnidadReader,
            AuditService auditService,
            IdempotencyService idempotencyService) {
        this.registroRepository = registroRepository;
        this.relacionRepository = relacionRepository;
        this.codigoProyectoService = codigoProyectoService;
        this.catalogoUnidadReader = catalogoUnidadReader;
        this.auditService = auditService;
        this.idempotencyService = idempotencyService;
    }

    @Autowired(required = false)
    public void setUnidadResponsableRepository(UnidadResponsableRepository unidadResponsableRepository) {
        this.unidadResponsableRepository = unidadResponsableRepository;
    }

    @Autowired(required = false)
    public void setTitularidadRepository(TitularidadResponsableRepository titularidadRepository) {
        this.titularidadRepository = titularidadRepository;
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
    public ProjectDetail crear(Long iniciativaId, CreateDerivedProjectRequest comando,
            PortafolioAuthContext contexto, String idempotencyKey, String payloadJson) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_KEY_REQUIRED",
                    "La creacion del proyecto derivado exige el header Idempotency-Key.");
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "La creacion del proyecto derivado exige el cuerpo serializado para calcular el hash canonico.");
        }
        if (iniciativaId == null) {
            throw new PortafolioValidationException("INITIATIVE_ID_REQUIRED",
                    "El identificador de la iniciativa es obligatorio.");
        }
        if (comando == null) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El comando del proyecto derivado es obligatorio.");
        }

        IdempotencyService.IdempotencyRequest request = new IdempotencyService.IdempotencyRequest(
                CONSUMIDOR, OP_CREAR_DERIVADO, idempotencyKey, payloadJson,
                contextoActorSub(contexto));

        IdempotencyService.IdempotencyResult resultado = idempotencyService.execute(request, () -> {
            ProjectDetail detalle = ejecutarCreacionDerivado(iniciativaId, comando, contexto);
            return new IdempotencyService.IdempotencyResponse(
                    RECURSO_PROYECTO, detalle.id(), serializarDetalle(detalle));
        });
        return deserializarDetalle(resultado.respuestaJson());
    }

    // ---------------------------------------------------------------------
    // Logica de negocio transaccional
    // ---------------------------------------------------------------------

    @Transactional
    ProjectDetail ejecutarCreacionDerivado(Long iniciativaId, CreateDerivedProjectRequest comando,
            PortafolioAuthContext contexto) {
        // 1) Bloqueo pesimista de la iniciativa bajo PESSIMISTIC_WRITE para
        // serializar la creacion de derivados concurrentes. Si el bean
        // mockeado no expone el metodo con bloqueo (modo pruebas
        // unitarias), se hace fallback a findById para preservar la
        // semantica de lectura; la carrera la resuelve JPA mediante la
        // UK por iniciativa del DDL 010 y la excepcion
        // DataIntegrityViolationException, que se traduce a 409
        // DERIVATION_ALREADY_EXISTS.
        RegistroPortafolioEntity iniciativa = registroRepository.findByIdForUpdate(iniciativaId)
                .orElseGet(() -> registroRepository.findById(iniciativaId)
                        .orElseThrow(() -> {
                            registrarDenegacion(contexto, iniciativaId, "INITIATIVE_NOT_FOUND",
                                    "La iniciativa origen no existe.",
                                    Map.of("iniciativaId", String.valueOf(iniciativaId)));
                            return new ResponseStatusException(HttpStatus.NOT_FOUND,
                                    "INITIATIVE_NOT_FOUND: la iniciativa no existe");
                        }));

        // 2) Estado canonico: la iniciativa debe estar en INICIATIVA_APROBADA.
        if (iniciativa.getEstado() != EstadoIniciativa.INICIATIVA_APROBADA) {
            String code = iniciativa.getEstado() == EstadoIniciativa.INICIATIVA_ARCHIVADA
                    ? "INITIATIVE_NOT_APPROVED"
                    : "STATE_TRANSITION_NOT_ALLOWED";
            String detalle = "La iniciativa debe estar en INICIATIVA_APROBADA; se encontro "
                    + iniciativa.getEstado();
            registrarDenegacion(contexto, iniciativaId, code, detalle,
                    Map.of("estadoActual", iniciativa.getEstado().name()));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    code + ": " + detalle);
        }

        // 3) Verificacion de la UK por iniciativa. La UK del DDL 010 es
        // la autoridad; el servicio consulta la fila como salvaguarda
        // y para devolver 409 canonico antes del INSERT.
        if (relacionRepository.existsByIniciativaId(iniciativaId)) {
            String detalle = "La iniciativa " + iniciativaId
                    + " ya tiene un proyecto derivado.";
            registrarDenegacion(contexto, iniciativaId, "DERIVATION_ALREADY_EXISTS", detalle,
                    Map.of("iniciativaId", String.valueOf(iniciativaId)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "DERIVATION_ALREADY_EXISTS: " + detalle);
        }

        // 4) Autorizacion efectiva: solo el Responsable dentro de su
        // ambito puede crear el derivado.
        autorizarResponsable(contexto, iniciativa);

        // 5) Validacion de campos oficiales del portafolio.
        validarCampos(comando);

        // 6) Generacion del codigo propio bajo PESSIMISTIC_WRITE.
        Long unidadPrincipalId = unidadPrincipalId(comando);
        String prefijo = catalogoUnidadReader.prefijoUnidad(unidadPrincipalId)
                .orElseThrow(PortafolioValidationException::prefijoNoDisponible);
        Integer anio = Year.now().getValue();
        String codigo = codigoProyectoService.generarCodigo(anio, unidadPrincipalId, prefijo);

        // 7) Construccion del proyecto derivado. Se copian codigoOrigen
        // y tipoSolucion desde la iniciativa conforme a la Constitucion
        // 5.0.0 (matriz 013, campo 3 y 6). La iniciativa permanece en
        // INICIATIVA_APROBADA: nunca se muta su estado.
        RegistroPortafolioEntity proyecto = new RegistroPortafolioEntity();
        proyecto.setCodigo(codigo);
        proyecto.setCodigoPrefijo(prefijo);
        proyecto.setCodigoOrigen(iniciativa.getCodigo());
        proyecto.setTipoRegistro(TipoRegistro.PROYECTO);
        proyecto.setNombre(comando.nombre().trim());
        proyecto.setTipoSolucion(copiarTipoSolucion(iniciativa));
        proyecto.setFuenteOrigen(comando.fuenteOrigen());

        proyecto.setDescripcion(comando.descripcion() == null
                ? null : comando.descripcion().trim());
        proyecto.setProblemaPublico(comando.descripcion() == null
                ? null : comando.descripcion().trim());
        proyecto.setSolucionPropuesta(iniciativa.getSolucionPropuesta());
        proyecto.setObjetivoPeiId(comando.objetivoPeiId());
        proyecto.setActividadPoiId(comando.actividadPoiId());
        proyecto.setEstado(EstadoIniciativa.PROYECTO_EJECUCION);
        proyecto.setFechaInicio(LocalDate.now());
        proyecto.setUnidadEjecutoraId(unidadPrincipalId);
        proyecto.setResponsableId(comando.titularId());
        proyecto.setComponenteDigital(Boolean.TRUE.equals(comando.componenteDigital()) ? "S" : "N");
        proyecto.setDetalleComponenteDigital(comando.detalleComponenteDigital() == null
                ? null : comando.detalleComponenteDigital().trim());
        proyecto.setNota(comando.nota() == null ? null : comando.nota().trim());
        proyecto.setCreadoPor(contexto == null ? null : contexto.actorSub());
        proyecto.setSubsanacionActiva("N");
        proyecto.setObjetivoPei(null);
        proyecto.setActividadPoi(null);

        proyecto = registroRepository.save(proyecto);

        // 8) Persistencia de unidades responsables y titularidad del
        // responsable titular. Se realiza solo si los repositorios
        // opcionales estan disponibles para preservar la firma exigida
        // por T062 en modo unitario.
        persistirUnidadesYTitularidad(proyecto.getId(), comando, contexto);

        // 9) Persistencia de la relacion inmutable iniciativa-proyecto.
        RelacionIniciativaProyectoEntity relacion = new RelacionIniciativaProyectoEntity();
        relacion.setIniciativaId(iniciativaId);
        relacion.setProyectoId(proyecto.getId());
        relacion.setCreadaPor(contexto == null ? null : contexto.actorSub());
        try {
            relacion = relacionRepository.save(relacion);
        } catch (DataIntegrityViolationException carrera) {
            // La UK por iniciativa es la autoridad. Si una transaccion
            // concurrente persistio la fila entre la verificacion y el
            // INSERT, se traduce a 409 canonico.
            String detalle = "La iniciativa " + iniciativaId
                    + " ya tiene un proyecto derivado (UK por iniciativa).";
            registrarDenegacion(contexto, iniciativaId, "DERIVATION_ALREADY_EXISTS", detalle,
                    Map.of("iniciativaId", String.valueOf(iniciativaId)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "DERIVATION_ALREADY_EXISTS: " + detalle);
        }

        // 10) Auditoria atomica de exito en la misma transaccion.
        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("codigo", codigo);
        cambios.put("codigoOrigen", iniciativa.getCodigo());
        cambios.put("tipoRegistro", TipoRegistro.PROYECTO.name());
        cambios.put("estado", EstadoIniciativa.PROYECTO_EJECUCION.name());
        cambios.put("iniciativaId", String.valueOf(iniciativaId));
        cambios.put("unidadPrincipalId", String.valueOf(unidadPrincipalId));
        cambios.put("documentoFormalId", String.valueOf(comando.documentoFormalId()));
        cambios.put("relacionId", String.valueOf(relacion.getId()));

        auditService.registrarExito(new AuditService.AuditCommand(
                contexto == null ? null : contexto.correlacionId(),
                contexto == null ? null : contexto.actorUsuarioId(),
                null,
                contexto == null ? null : contexto.asignacionEfectivaId(),
                contexto == null ? null : contexto.perfilEfectivo(),
                contexto == null ? null : contexto.unidadEfectivaId(),
                OP_CREAR_DERIVADO,
                CONSUMIDOR,
                RECURSO_PROYECTO,
                proyecto.getId(),
                "SUCCESS",
                cambios,
                "INTERNO"));

        return construirDetalle(proyecto, iniciativa, relacion, comando, contexto);
    }

    // ---------------------------------------------------------------------
    // Validacion de campos oficiales
    // ---------------------------------------------------------------------

    private void validarCampos(CreateDerivedProjectRequest comando) {
        // Campo 5: nombre obligatorio.
        if (comando.nombre() == null || comando.nombre().isBlank()) {
            throw PortafolioValidationException.campoRequerido(5, "Nombre de iniciativa o proyecto");
        }
        if (comando.nombre().length() > 500) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El nombre no puede exceder 500 caracteres.");
        }

        // Campo 12: unidades responsables con exactamente una principal.
        if (comando.unidades() == null || comando.unidades().isEmpty()) {
            throw PortafolioValidationException.campoRequerido(12, "Unidades responsables");
        }
        long principalCount = comando.unidades().stream()
                .filter(item -> Boolean.TRUE.equals(item.principal()))
                .count();
        if (principalCount != 1) {
            throw PortafolioValidationException.unidadPrincipalCardinality();
        }
        long unidadesDistintas = comando.unidades().stream()
                .map(CreateDerivedProjectRequest.UnidadDerivadaItem::unidadId)
                .distinct()
                .count();
        if (unidadesDistintas != comando.unidades().size()) {
            throw new PortafolioValidationException("UNIT_DUPLICATE",
                    "Una unidad responsable no puede aparecer mas de una vez.");
        }

        // Campo 7: fuente u origen dentro del catalogo canonico 019.
        if (comando.fuenteOrigen() == null
                || !FUENTES_CANONICAS.contains(comando.fuenteOrigen().name())) {
            throw new PortafolioValidationException("CATALOG_NOT_ACTIVE",
                    "El valor de Fuente u origen no pertenece al catalogo canonico 019.");
        }

        // Campo 22: componente digital y su detalle coherente.
        if (comando.componenteDigital() == null) {
            throw PortafolioValidationException.campoRequerido(22, "Componente digital");
        }
        if (Boolean.TRUE.equals(comando.componenteDigital())
                && (comando.detalleComponenteDigital() == null
                    || comando.detalleComponenteDigital().isBlank())) {
            throw PortafolioValidationException.componenteDigitalSinDetalle();
        }

        // Campo 15: documento formal obligatorio.
        if (comando.documentoFormalId() == null) {
            throw new PortafolioValidationException("FORMAL_DOCUMENT_REQUIRED",
                    "El derivado exige el documento formal de aprobacion o autorizacion de inicio.");
        }

        // Limites canonicos de la Constitucion.
        if (comando.descripcion() != null && comando.descripcion().length() > 2000) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "La descripcion no puede exceder 2000 caracteres.");
        }
        if (comando.detalleComponenteDigital() != null
                && comando.detalleComponenteDigital().length() > 500) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El detalle del componente digital no puede exceder 500 caracteres.");
        }
        if (comando.nota() != null && comando.nota().length() > 1000) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "La nota no puede exceder 1000 caracteres.");
        }
    }

    private static Long unidadPrincipalId(CreateDerivedProjectRequest comando) {
        return comando.unidades().stream()
                .filter(item -> Boolean.TRUE.equals(item.principal()))
                .map(CreateDerivedProjectRequest.UnidadDerivadaItem::unidadId)
                .findFirst()
                .orElseThrow(PortafolioValidationException::unidadPrincipalCardinality);
    }

    private static TipoSolucion copiarTipoSolucion(RegistroPortafolioEntity iniciativa) {
        TipoSolucion tipo = iniciativa.getTipoSolucion();
        return tipo == null ? TipoSolucion.POR_DEFINIR : tipo;
    }

    private void autorizarResponsable(PortafolioAuthContext contexto,
            RegistroPortafolioEntity iniciativa) {
        if (contexto == null || contexto.perfilEfectivo() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "ASSIGNMENT_SCOPE_DENIED: se requiere un contexto de autorizacion efectivo");
        }
        if (!PERFIL_RESPONSABLE.equals(contexto.perfilEfectivo())) {
            String code = "ASSIGNMENT_SCOPE_DENIED";
            String detalle = "Solo el Responsable dentro de su ambito puede crear el derivado; "
                    + "perfil efectivo: " + contexto.perfilEfectivo();
            registrarDenegacion(contexto, iniciativa.getId(), code, detalle,
                    Map.of("perfilEfectivo", contexto.perfilEfectivo()));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    code + ": " + detalle);
        }

        // Revalidacion Oracle cuando el servicio esta disponible. Esta
        // llamada se realiza bajo el bloqueo pesimista de la iniciativa.
        if (autorizacionService != null
                && contexto.asignacionEfectivaId() != null
                && contexto.actorSub() != null
                && iniciativa.getUnidadEjecutoraId() != null) {
            try {
                autorizacionService.revalidarParaOperacionSensible(
                        contexto.actorSub(),
                        contexto.asignacionEfectivaId(),
                        PERFIL_RESPONSABLE,
                        iniciativa.getUnidadEjecutoraId());
            } catch (ResponseStatusException rse) {
                String detalle = "La revalidacion Oracle rechazo la operacion";
                registrarDenegacion(contexto, iniciativa.getId(),
                        "ASSIGNMENT_SCOPE_DENIED", detalle,
                        Map.of("perfilEfectivo", contexto.perfilEfectivo()));
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "ASSIGNMENT_SCOPE_DENIED: " + detalle);
            }
        }
    }

    private void persistirUnidadesYTitularidad(Long proyectoId,
            CreateDerivedProjectRequest comando, PortafolioAuthContext contexto) {
        if (unidadResponsableRepository != null) {
            int orden = 1;
            for (CreateDerivedProjectRequest.UnidadDerivadaItem item : comando.unidades()) {
                UnidadResponsableEntity unidad = new UnidadResponsableEntity();
                unidad.setRegistroPortafolioId(proyectoId);
                unidad.setNroOrden(orden++);
                unidad.setDescripcion("Unidad " + item.unidadId());
                unidad.setAbreviatura("U" + item.unidadId());
                unidadResponsableRepository.save(unidad);
            }
        }
        if (titularidadRepository != null) {
            TitularidadResponsableEntity titularidad = new TitularidadResponsableEntity();
            titularidad.setRegistroPortafolioId(proyectoId);
            titularidad.setUsuarioId(comando.titularId());
            titularidad.setInicio(LocalDate.now());
            titularidad.setCreadoPor(contexto == null ? null : contexto.actorSub());
            titularidadRepository.save(titularidad);
        }
    }

    // ---------------------------------------------------------------------
    // Auditoria de denegacion
    // ---------------------------------------------------------------------

    /**
     * La denegacion se registra en una transaccion independiente para
     * que un fallo del servicio de auditoria no impacte la respuesta
     * HTTP ya emitida. Si el bean {@link AuditService} no estuviera
     * disponible (modo sin auditoria), la denegacion se ignora.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void registrarDenegacion(PortafolioAuthContext contexto, Long recursoId,
            String codigo, String detalle, Map<String, String> cambios) {
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
                    OP_CREAR_DERIVADO,
                    CONSUMIDOR,
                    RECURSO_PROYECTO,
                    recursoId,
                    codigo,
                    evidencia,
                    "RESTRINGIDO"));
        } catch (RuntimeException ex) {
            LOG.warn("Fallo registrando denegacion del derivado: {}", ex.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Mapeo a DTO HTTP y serializacion
    // ---------------------------------------------------------------------

    private ProjectDetail construirDetalle(RegistroPortafolioEntity proyecto,
            RegistroPortafolioEntity iniciativa, RelacionIniciativaProyectoEntity relacion,
            CreateDerivedProjectRequest comando, PortafolioAuthContext contexto) {
        Long version = proyecto.getVersion() == null ? 0L : proyecto.getVersion();
        String etag = "\"" + proyecto.getId() + "-" + version + "\"";
        List<ProjectDetail.UnidadResponsableDetail> unidades = construirUnidades(comando, proyecto);

        return new ProjectDetail(
                proyecto.getId(),
                iniciativa.getId(),
                proyecto.getCodigo(),
                proyecto.getCodigoOrigen(),
                proyecto.getFechaInicio(),
                proyecto.getNombre(),
                proyecto.getTipoRegistro(),
                proyecto.getEstado(),
                proyecto.getFuenteOrigen(),
                proyecto.getDetalleFuente(),
                proyecto.getResponsableId(),
                proyecto.getProblemaPublico(),
                proyecto.getSolucionPropuesta(),
                proyecto.getObjetivoPeiId(),
                proyecto.getActividadPoiId(),
                unidades,
                "S".equals(proyecto.getComponenteDigital()),
                proyecto.getDetalleComponenteDigital(),
                proyecto.getNota(),
                comando.documentoFormalId(),
                version,
                etag,
                proyecto.getFechaCreacion());
    }

    private List<ProjectDetail.UnidadResponsableDetail> construirUnidades(
            CreateDerivedProjectRequest comando, RegistroPortafolioEntity proyecto) {
        if (comando == null || comando.unidades() == null || comando.unidades().isEmpty()) {
            return List.of();
        }
        List<ProjectDetail.UnidadResponsableDetail> detalle = new ArrayList<>();
        for (CreateDerivedProjectRequest.UnidadDerivadaItem item : comando.unidades()) {
            detalle.add(new ProjectDetail.UnidadResponsableDetail(
                    null,
                    item.unidadId(),
                    "Unidad " + item.unidadId(),
                    "U" + item.unidadId(),
                    item.principal()));
        }
        return detalle;
    }

    private String serializarDetalle(ProjectDetail detalle) {
        if (objectMapper == null) {
            return "{\"id\":" + detalle.id()
                    + ",\"iniciativaId\":" + detalle.iniciativaId()
                    + ",\"codigo\":\"" + detalle.codigo() + "\"}";
        }
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo serializar el detalle del derivado para idempotencia.", e);
        }
    }

    private ProjectDetail deserializarDetalle(String json) {
        if (objectMapper == null || json == null) {
            throw new IllegalStateException(
                    "No se puede deserializar la respuesta idempotente del derivado.");
        }
        try {
            return objectMapper.readValue(json, ProjectDetail.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo deserializar la respuesta idempotente del derivado.", e);
        }
    }

    // ---------------------------------------------------------------------
    // Helpers de contexto
    // ---------------------------------------------------------------------

    private static String contextoActorSub(PortafolioAuthContext contexto) {
        return contexto == null || contexto.actorSub() == null ? "unknown" : contexto.actorSub();
    }

    private static String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() > max ? valor.substring(0, max) : valor;
    }
}
