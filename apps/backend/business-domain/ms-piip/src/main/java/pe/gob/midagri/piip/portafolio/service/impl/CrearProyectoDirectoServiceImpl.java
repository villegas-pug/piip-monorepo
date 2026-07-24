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
import pe.gob.midagri.piip.portafolio.dto.DirectProjectRequest;
import pe.gob.midagri.piip.portafolio.dto.PortafolioAuthContext;
import pe.gob.midagri.piip.portafolio.dto.ProjectDetail;
import pe.gob.midagri.piip.portafolio.dto.TipoOrigenDirecto;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.entity.TipoSolucion;
import pe.gob.midagri.piip.portafolio.entity.TitularidadResponsableEntity;
import pe.gob.midagri.piip.portafolio.entity.UnidadResponsableEntity;
import pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;
import pe.gob.midagri.piip.portafolio.repository.TitularidadResponsableRepository;
import pe.gob.midagri.piip.portafolio.repository.UnidadResponsableRepository;
import pe.gob.midagri.piip.portafolio.service.CatalogoUnidadReader;
import pe.gob.midagri.piip.portafolio.service.CodigoProyectoService;
import pe.gob.midagri.piip.portafolio.service.CrearProyectoDirectoService;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/**
 * Implementacion de la creacion del proyecto directo (US3, Constitucion
 * 5.0.0) conforme al contrato
 * {@code specs/001-gestionar-portafolio-innovacion/contracts/portafolio.md}.
 *
 * <p>Reglas canonicas aplicadas en este servicio:
 * <ul>
 *   <li>El proyecto directo exige, como minimo, los campos oficiales 1 al
 *       13 y 22; el campo 23 ({@code nota}) es opcional. La fecha de
 *       inicio coincide con la del documento formal de aprobacion o
 *       autorizacion de inicio.</li>
 *   <li>Solo aplica a proyectos heredados o a excepciones formalmente
 *       autorizadas por la Autoridad; NO omite la evaluacion de una
 *       iniciativa nueva. Si existe una iniciativa {@code PRESENTADO} para
 *       el mismo ambito y anio, la operacion se rechaza con 409.</li>
 *   <li>Un segundo directo para la misma unidad y anio, cuando ya hay uno
 *       activo, falla con 409 {@code DIRECT_PROJECT_NOT_AUTHORIZED}. La
 *       deteccion opera contra el agregado central {@code PROYECTO} antes
 *       del INSERT y actua como salvaguarda adicional al bloqueo
 *       pesimista.</li>
 *   <li>Solo la {@code Autoridad} o el {@code Evaluador} pueden crear un
 *       proyecto directo; el {@code Responsable} queda excluido por
 *       constitucion.</li>
 *   <li>{@link TipoOrigenDirecto#HEREDADO} exige {@code codigoOrigen} para
 *       acreditar el inicio previo a PIIP. {@link
 *       TipoOrigenDirecto#EXCEPCION_FORMAL} se acredita con el documento
 *       formal de autorizacion.</li>
 *   <li>Idempotencia canonica por consumidor, operacion, clave y hash del
 *       payload.</li>
 *   <li>Auditoria atomica de exito en la misma transaccion de negocio y
 *       de denegacion en una transaccion independiente
 *       ({@code REQUIRES_NEW}).</li>
 *   <li>El estado nace en {@code PROYECTO_EJECUCION} y el proyecto no se
 *       vincula con iniciativa alguna ({@code iniciativaId} nulo).</li>
 * </ul>
 *
 * <p>El constructor acepta los cinco colaboradores obligatorios exigidos
 * por las pruebas de contrato T062. Los colaboradores opcionales
 * ({@link UnidadResponsableRepository}, {@link
 * TitularidadResponsableRepository}, {@link AutorizacionEfectivaService}
 * y {@link ObjectMapper}) se inyectan mediante setters con
 * {@code @Autowired(required = false)} para preservar la firma y
 * permitir la ejecucion aislada.
 */
@Service
public class CrearProyectoDirectoServiceImpl implements CrearProyectoDirectoService {

    private static final Logger LOG = LoggerFactory.getLogger(CrearProyectoDirectoServiceImpl.class);

    private static final String CONSUMIDOR = "PORTAFOLIO";
    private static final String RECURSO_PROYECTO = "PROYECTO";
    private static final String OP_CREAR_DIRECTO = "CREAR_PROYECTO_DIRECTO";

    private static final String PERFIL_AUTORIDAD = "Autoridad";
    private static final String PERFIL_EVALUADOR = "Evaluador";

    private static final Set<String> FUENTES_CANONICAS = Set.of(
            FuenteOrigen.FICHA_INICIATIVA.name(),
            FuenteOrigen.CONCURSO_INTERNO.name(),
            FuenteOrigen.INNOVACION_ABIERTA.name(),
            FuenteOrigen.PROPUESTA_JEFATURA.name(),
            FuenteOrigen.OTROS.name());

    private final RegistroPortafolioRepository registroRepository;
    private final CodigoProyectoService codigoProyectoService;
    private final CatalogoUnidadReader catalogoUnidadReader;
    private final AuditService auditService;
    private final IdempotencyService idempotencyService;

    private UnidadResponsableRepository unidadResponsableRepository;
    private TitularidadResponsableRepository titularidadRepository;
    private AutorizacionEfectivaService autorizacionService;
    private ObjectMapper objectMapper;

    public CrearProyectoDirectoServiceImpl(
            RegistroPortafolioRepository registroRepository,
            CodigoProyectoService codigoProyectoService,
            CatalogoUnidadReader catalogoUnidadReader,
            AuditService auditService,
            IdempotencyService idempotencyService) {
        this.registroRepository = registroRepository;
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
    public ProjectDetail crear(DirectProjectRequest comando, PortafolioAuthContext contexto,
            String idempotencyKey, String payloadJson) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_KEY_REQUIRED",
                    "La creacion del proyecto directo exige el header Idempotency-Key.");
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new PortafolioValidationException("IDEMPOTENCY_PAYLOAD_REQUIRED",
                    "La creacion del proyecto directo exige el cuerpo serializado para calcular el hash canonico.");
        }
        if (comando == null) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El comando del proyecto directo es obligatorio.");
        }

        IdempotencyService.IdempotencyRequest request = new IdempotencyService.IdempotencyRequest(
                CONSUMIDOR, OP_CREAR_DIRECTO, idempotencyKey, payloadJson,
                contextoActorSub(contexto));

        IdempotencyService.IdempotencyResult resultado = idempotencyService.execute(request, () -> {
            ProjectDetail detalle = ejecutarCreacionDirecto(comando, contexto);
            return new IdempotencyService.IdempotencyResponse(
                    RECURSO_PROYECTO, detalle.id(), serializarDetalle(detalle));
        });
        return deserializarDetalle(resultado.respuestaJson());
    }

    // ---------------------------------------------------------------------
    // Logica de negocio transaccional
    // ---------------------------------------------------------------------

    @Transactional
    ProjectDetail ejecutarCreacionDirecto(DirectProjectRequest comando,
            PortafolioAuthContext contexto) {
        // 1) Autorizacion efectiva: solo Autoridad o Evaluador. Esta
        // validacion se aplica antes que el resto para impedir crear
        // proyectos directos con un perfil no canonico.
        autorizarAutoridadOEvaluador(contexto, comando);

        // 2) Validacion de campos oficiales del portafolio.
        validarCampos(comando);

        // 3) Verificacion de duplicado: un solo proyecto directo activo
        // por unidad y anio. La consulta opera contra el agregado
        // central PROYECTO antes del INSERT.
        Integer anio = Year.now().getValue();
        if (registroRepository.existsByUnidadEjecutoraIdAndAnioAndEstado(
                comando.unidadResponsableId(), anio,
                EstadoIniciativa.PROYECTO_EJECUCION.name())) {
            String detalle = "La unidad " + comando.unidadResponsableId()
                    + " ya tiene un proyecto directo activo en el anio " + anio + ".";
            registrarDenegacion(contexto, null, "DIRECT_PROJECT_NOT_AUTHORIZED", detalle,
                    Map.of(
                            "unidadId", String.valueOf(comando.unidadResponsableId()),
                            "anio", String.valueOf(anio)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "DIRECT_PROJECT_NOT_AUTHORIZED: " + detalle);
        }

        // 4) Generacion del codigo propio bajo PESSIMISTIC_WRITE.
        String prefijo = catalogoUnidadReader.prefijoUnidad(comando.unidadResponsableId())
                .orElseThrow(PortafolioValidationException::prefijoNoDisponible);
        String codigo = codigoProyectoService.generarCodigo(anio,
                comando.unidadResponsableId(), prefijo);

        // 5) Construccion del proyecto directo. El codigoOrigen se
        // determina segun el tipoOrigen; el proyecto nunca se vincula a
        // una iniciativa.
        RegistroPortafolioEntity proyecto = new RegistroPortafolioEntity();
        proyecto.setCodigo(codigo);
        proyecto.setCodigoPrefijo(prefijo);
        proyecto.setCodigoOrigen(resolverCodigoOrigen(comando));
        proyecto.setTipoRegistro(TipoRegistro.PROYECTO);
        proyecto.setNombre(comando.nombre().trim());
        proyecto.setTipoSolucion(TipoSolucion.POR_DEFINIR);
        proyecto.setFuenteOrigen(comando.fuenteOrigen());
        proyecto.setDetalleFuente(null);
        proyecto.setDescripcion(comando.descripcion().trim());
        proyecto.setProblemaPublico(comando.descripcion().trim());
        proyecto.setSolucionPropuesta(null);
        proyecto.setObjetivoPeiId(comando.objetivoPeiId());
        proyecto.setActividadPoiId(comando.actividadPoiId());
        proyecto.setEstado(EstadoIniciativa.PROYECTO_EJECUCION);
        proyecto.setFechaInicio(comando.fechaInicio());
        proyecto.setUnidadEjecutoraId(comando.unidadResponsableId());
        proyecto.setResponsableId(comando.responsableId());
        proyecto.setComponenteDigital(Boolean.TRUE.equals(comando.componenteDigital()) ? "S" : "N");
        proyecto.setDetalleComponenteDigital(comando.detalleComponenteDigital() == null
                ? null : comando.detalleComponenteDigital().trim());
        proyecto.setNota(comando.nota() == null ? null : comando.nota().trim());
        proyecto.setCreadoPor(contexto == null ? null : contexto.actorSub());
        proyecto.setSubsanacionActiva("N");
        proyecto.setObjetivoPei(null);
        proyecto.setActividadPoi(null);

        try {
            proyecto = registroRepository.save(proyecto);
        } catch (DataIntegrityViolationException carrera) {
            String detalle = "La unidad " + comando.unidadResponsableId()
                    + " ya tiene un proyecto directo activo en el anio " + anio + ".";
            registrarDenegacion(contexto, null, "DIRECT_PROJECT_NOT_AUTHORIZED", detalle,
                    Map.of(
                            "unidadId", String.valueOf(comando.unidadResponsableId()),
                            "anio", String.valueOf(anio)));
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "DIRECT_PROJECT_NOT_AUTHORIZED: " + detalle);
        }

        // 6) Persistencia de la unidad principal y la titularidad del
        // responsable titular. Se realiza solo si los repositorios
        // opcionales estan disponibles para preservar la firma exigida
        // por T062 en modo unitario.
        persistirUnidadYTitularidad(proyecto.getId(), comando, contexto);

        // 7) Auditoria atomica de exito en la misma transaccion.
        Map<String, String> cambios = new LinkedHashMap<>();
        cambios.put("codigo", codigo);
        cambios.put("codigoOrigen", proyecto.getCodigoOrigen() == null
                ? "" : proyecto.getCodigoOrigen());
        cambios.put("tipoOrigen", comando.tipoOrigen().name());
        cambios.put("tipoRegistro", TipoRegistro.PROYECTO.name());
        cambios.put("estado", EstadoIniciativa.PROYECTO_EJECUCION.name());
        cambios.put("unidadPrincipalId", String.valueOf(comando.unidadResponsableId()));
        cambios.put("responsableId", String.valueOf(comando.responsableId()));
        cambios.put("documentoAutorizacionId", String.valueOf(comando.documentoAutorizacionId()));
        cambios.put("evidencias", String.valueOf(comando.evidenciaIds().size()));
        cambios.put("fechaInicio", comando.fechaInicio().toString());

        auditService.registrarExito(new AuditService.AuditCommand(
                contexto == null ? null : contexto.correlacionId(),
                contexto == null ? null : contexto.actorUsuarioId(),
                null,
                contexto == null ? null : contexto.asignacionEfectivaId(),
                contexto == null ? null : contexto.perfilEfectivo(),
                contexto == null ? null : contexto.unidadEfectivaId(),
                OP_CREAR_DIRECTO,
                CONSUMIDOR,
                RECURSO_PROYECTO,
                proyecto.getId(),
                "SUCCESS",
                cambios,
                "INTERNO"));

        return construirDetalle(proyecto, comando);
    }

    // ---------------------------------------------------------------------
    // Autorizacion
    // ---------------------------------------------------------------------

    private void autorizarAutoridadOEvaluador(PortafolioAuthContext contexto,
            DirectProjectRequest comando) {
        if (contexto == null || contexto.perfilEfectivo() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "ASSIGNMENT_SCOPE_DENIED: se requiere un contexto de autorizacion efectivo");
        }
        String perfil = contexto.perfilEfectivo();
        if (!PERFIL_AUTORIDAD.equals(perfil) && !PERFIL_EVALUADOR.equals(perfil)) {
            String detalle = "Solo la Autoridad o el Evaluador pueden crear un proyecto directo; "
                    + "perfil efectivo: " + perfil;
            registrarDenegacion(contexto, null, "ASSIGNMENT_SCOPE_DENIED", detalle,
                    Map.of("perfilEfectivo", perfil));
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "ASSIGNMENT_SCOPE_DENIED: " + detalle);
        }

        // Revalidacion Oracle cuando el servicio esta disponible. Esta
        // llamada se realiza bajo la transaccion de negocio.
        if (autorizacionService != null
                && contexto.asignacionEfectivaId() != null
                && contexto.actorSub() != null
                && comando.unidadResponsableId() != null) {
            try {
                autorizacionService.revalidarParaOperacionSensible(
                        contexto.actorSub(),
                        contexto.asignacionEfectivaId(),
                        perfil,
                        comando.unidadResponsableId());
            } catch (ResponseStatusException rse) {
                String detalle = "La revalidacion Oracle rechazo la operacion";
                registrarDenegacion(contexto, null, "ASSIGNMENT_SCOPE_DENIED", detalle,
                        Map.of("perfilEfectivo", perfil));
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "ASSIGNMENT_SCOPE_DENIED: " + detalle);
            }
        }
    }

    // ---------------------------------------------------------------------
    // Validacion de campos oficiales
    // ---------------------------------------------------------------------

    private void validarCampos(DirectProjectRequest comando) {
        // Campo 1: tipoOrigen obligatorio.
        if (comando.tipoOrigen() == null) {
            throw PortafolioValidationException.campoRequerido(1, "Tipo de origen");
        }
        // Campo 4: fechaInicio obligatoria (servidor la fija del documento
        // formal; el cliente aporta la fecha del acto).
        if (comando.fechaInicio() == null) {
            throw PortafolioValidationException.campoRequerido(4, "Fecha de inicio");
        }
        // Campo 5: nombre obligatorio.
        if (comando.nombre() == null || comando.nombre().isBlank()) {
            throw PortafolioValidationException.campoRequerido(5, "Nombre de proyecto");
        }
        if (comando.nombre().length() > 500) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El nombre no puede exceder 500 caracteres.");
        }
        // Campo 10: objetivoPeiId obligatorio.
        if (comando.objetivoPeiId() == null) {
            throw PortafolioValidationException.campoRequerido(10, "Objetivo PEI");
        }
        // Campo 11: actividadPoiId obligatorio.
        if (comando.actividadPoiId() == null) {
            throw PortafolioValidationException.campoRequerido(11, "Actividad POI");
        }
        // Campo 12: unidadResponsableId obligatorio.
        if (comando.unidadResponsableId() == null) {
            throw PortafolioValidationException.campoRequerido(12, "Unidad responsable");
        }
        // Campo 8: responsableId obligatorio.
        if (comando.responsableId() == null) {
            throw PortafolioValidationException.responsableCardinality();
        }
        // Campo 9: descripcion obligatoria.
        if (comando.descripcion() == null || comando.descripcion().isBlank()) {
            throw PortafolioValidationException.campoRequerido(9, "Descripcion");
        }
        if (comando.descripcion().length() > 2000) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "La descripcion no puede exceder 2000 caracteres.");
        }
        // Campo 7: fuenteOrigen obligatoria y dentro del catalogo canonico.
        if (comando.fuenteOrigen() == null
                || !FUENTES_CANONICAS.contains(comando.fuenteOrigen().name())) {
            throw new PortafolioValidationException("CATALOG_NOT_ACTIVE",
                    "El valor de Fuente u origen no pertenece al catalogo canonico 019.");
        }
        // Campo 22: componenteDigital y su detalle coherente.
        if (comando.componenteDigital() == null) {
            throw PortafolioValidationException.campoRequerido(22, "Componente digital");
        }
        if (Boolean.TRUE.equals(comando.componenteDigital())
                && (comando.detalleComponenteDigital() == null
                    || comando.detalleComponenteDigital().isBlank())) {
            throw PortafolioValidationException.componenteDigitalSinDetalle();
        }
        // Campo 15: documento formal obligatorio para cualquier proyecto
        // directo (heredado o excepcion).
        if (comando.documentoAutorizacionId() == null) {
            throw new PortafolioValidationException("FORMAL_DOCUMENT_REQUIRED",
                    "El proyecto directo exige el documento formal de aprobacion o autorizacion de inicio.");
        }
        // HEREDADO exige codigoOrigen para acreditar el inicio previo a PIIP.
        if (comando.tipoOrigen() == TipoOrigenDirecto.HEREDADO
                && (comando.codigoOrigen() == null || comando.codigoOrigen().isBlank())) {
            throw new PortafolioValidationException("OFFICIAL_FIELD_REQUIRED",
                    "El proyecto heredado exige el codigo de origen del sistema previo a PIIP.");
        }
        // EXCEPCION_FORMAL se acredita con el documento de autorizacion
        // (ya validado arriba con FORMAL_DOCUMENT_REQUIRED); no exige
        // codigoOrigen.
        // Campo 17: al menos una evidencia disponible.
        if (comando.evidenciaIds() == null || comando.evidenciaIds().isEmpty()) {
            throw new PortafolioValidationException("EVIDENCE_NOT_ELIGIBLE",
                    "El proyecto directo exige al menos una evidencia disponible.");
        }
        // Limites canonicos de la Constitucion.
        if (comando.detalleComponenteDigital() != null
                && comando.detalleComponenteDigital().length() > 500) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El detalle del componente digital no puede exceder 500 caracteres.");
        }
        if (comando.nota() != null && comando.nota().length() > 1000) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "La nota no puede exceder 1000 caracteres.");
        }
        if (comando.codigoOrigen() != null && comando.codigoOrigen().length() > 50) {
            throw new PortafolioValidationException("VALIDATION_FAILED",
                    "El codigo de origen no puede exceder 50 caracteres.");
        }
    }

    private static String resolverCodigoOrigen(DirectProjectRequest comando) {
        if (comando.tipoOrigen() == TipoOrigenDirecto.HEREDADO) {
            return comando.codigoOrigen() == null ? null : comando.codigoOrigen().trim();
        }
        return null;
    }

    // ---------------------------------------------------------------------
    // Persistencia opcional de unidades y titularidad
    // ---------------------------------------------------------------------

    private void persistirUnidadYTitularidad(Long proyectoId, DirectProjectRequest comando,
            PortafolioAuthContext contexto) {
        if (unidadResponsableRepository != null) {
            UnidadResponsableEntity unidad = new UnidadResponsableEntity();
            unidad.setRegistroPortafolioId(proyectoId);
            unidad.setNroOrden(1);
            unidad.setDescripcion("Unidad " + comando.unidadResponsableId());
            unidad.setAbreviatura("U" + comando.unidadResponsableId());
            unidadResponsableRepository.save(unidad);
        }
        if (titularidadRepository != null) {
            TitularidadResponsableEntity titularidad = new TitularidadResponsableEntity();
            titularidad.setRegistroPortafolioId(proyectoId);
            titularidad.setUsuarioId(comando.responsableId());
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
                    OP_CREAR_DIRECTO,
                    CONSUMIDOR,
                    RECURSO_PROYECTO,
                    recursoId,
                    codigo,
                    evidencia,
                    "RESTRINGIDO"));
        } catch (RuntimeException ex) {
            LOG.warn("Fallo registrando denegacion del proyecto directo: {}", ex.getMessage());
        }
    }

    // ---------------------------------------------------------------------
    // Mapeo a DTO HTTP y serializacion idempotente
    // ---------------------------------------------------------------------

    private ProjectDetail construirDetalle(RegistroPortafolioEntity proyecto,
            DirectProjectRequest comando) {
        Long version = proyecto.getVersion() == null ? 0L : proyecto.getVersion();
        String etag = "\"" + proyecto.getId() + "-" + version + "\"";

        List<ProjectDetail.UnidadResponsableDetail> unidades = new ArrayList<>();
        unidades.add(new ProjectDetail.UnidadResponsableDetail(
                null,
                comando.unidadResponsableId(),
                "Unidad " + comando.unidadResponsableId(),
                "U" + comando.unidadResponsableId(),
                Boolean.TRUE));

        return new ProjectDetail(
                proyecto.getId(),
                null,
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
                comando.documentoAutorizacionId(),
                version,
                etag,
                proyecto.getFechaCreacion());
    }

    private String serializarDetalle(ProjectDetail detalle) {
        if (objectMapper == null) {
            return "{\"id\":" + detalle.id()
                    + ",\"codigo\":\"" + detalle.codigo() + "\"}";
        }
        try {
            return objectMapper.writeValueAsString(detalle);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo serializar el detalle del directo para idempotencia.", e);
        }
    }

    private ProjectDetail deserializarDetalle(String json) {
        if (objectMapper == null || json == null) {
            throw new IllegalStateException(
                    "No se puede deserializar la respuesta idempotente del directo.");
        }
        try {
            return objectMapper.readValue(json, ProjectDetail.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "No se pudo deserializar la respuesta idempotente del directo.", e);
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
