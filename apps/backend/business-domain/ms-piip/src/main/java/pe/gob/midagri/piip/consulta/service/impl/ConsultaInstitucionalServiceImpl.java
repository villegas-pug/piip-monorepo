package pe.gob.midagri.piip.consulta.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.consulta.dto.ConsultaInstitucionalAuthContext;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioDetail;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioPage;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioQuery;
import pe.gob.midagri.piip.consulta.exception.ConsultaInstitucionalValidationException;
import pe.gob.midagri.piip.consulta.mapper.InstitutionalPortfolioMapper;
import pe.gob.midagri.piip.consulta.service.ConsultaInstitucionalService;
import pe.gob.midagri.piip.documentos.dto.DocumentoInstitucionalMetadata;
import pe.gob.midagri.piip.documentos.service.DocumentoInstitucionalReader;
import pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioProjection;
import pe.gob.midagri.piip.portafolio.service.InstitutionalPortfolioReader;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService;

/**
 * Implementación del caso de uso de consulta institucional por
 * ámbito y clasificación. La regla de privacidad se aplica
 * dentro de la misma transacción de lectura; las decisiones
 * tomadas para una respuesta (perfil, unidades visibles,
 * clasificación documental) se conservan en la evidencia de
 * auditoría inmutable registrada por {@code auditoria}.
 */
@Service
public class ConsultaInstitucionalServiceImpl implements ConsultaInstitucionalService {

    private static final String MODULO = "CONSULTA";
    private static final String OPERACION_BUSCAR = "CONSULTAR_PORTAFOLIO_INSTITUCIONAL";
    private static final String OPERACION_DETALLE = "CONSULTAR_DETALLE_PORTAFOLIO_INSTITUCIONAL";
    private static final String RECURSO_TIPO = "REGISTRO_PORTAFOLIO";
    private static final String CLASIFICACION_INTERNA = "INTERNO";
    private static final String CLASIFICACION_RESTRINGIDA = "RESTRINGIDO";

    private final InstitutionalPortfolioReader portafolioReader;
    private final DocumentoInstitucionalReader documentoReader;
    private final AutorizacionEfectivaService autorizacion;
    private final AuditService auditoria;

    public ConsultaInstitucionalServiceImpl(InstitutionalPortfolioReader portafolioReader,
            DocumentoInstitucionalReader documentoReader,
            AutorizacionEfectivaService autorizacion,
            AuditService auditoria) {
        this.portafolioReader = portafolioReader;
        this.documentoReader = documentoReader;
        this.autorizacion = autorizacion;
        this.auditoria = auditoria;
    }

    @Override
    @Transactional(readOnly = true)
    public ResultadoConsulta buscar(InstitutionalPortfolioQuery consulta,
            ConsultaInstitucionalAuthContext contexto) {
        validarContexto(contexto);
        InstitutionalPortfolioQuery normalizada = consulta == null ? null : consulta.normalizar();
        AutorizacionEfectivaService.AsignacionEfectiva asignacion = revalidarAsignacion(contexto, null);
        Set<Long> unidadesVisibles = unidadesVisibles(contexto, asignacion);
        pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioQuery portafolioQuery =
                new pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioQuery(
                        normalizada == null || normalizada.tipoRegistro() == null
                                ? null : normalizada.tipoRegistro().name(),
                        normalizada == null ? null : normalizada.codigo(),
                        normalizada == null ? null : normalizada.nombre(),
                        normalizada == null ? null : normalizada.estado(),
                        normalizada == null ? null : normalizada.unidadId(),
                        normalizada == null ? null : normalizada.responsableId(),
                        normalizada == null ? null : normalizada.fechaDesde(),
                        normalizada == null ? null : normalizada.fechaHasta(),
                        normalizada == null ? null : normalizada.orden(),
                        normalizada == null ? 0 : normalizada.paginaNormalizada(),
                        normalizada == null ? 20 : normalizada.tamanioNormalizado(),
                        List.copyOf(unidadesVisibles));
        pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioPage origen =
                portafolioReader.buscar(portafolioQuery);
        boolean actorEsResponsable = perfilEs(asignacion, "Responsable");
        boolean actorEsEvaluador = perfilEs(asignacion, "Evaluador");
        boolean actorEsAdministrador = perfilEs(asignacion, "GlobalAdmin")
                || perfilEs(asignacion, "UnidadAdmin");
        InstitutionalPortfolioPage page = InstitutionalPortfolioMapper.aPagina(origen,
                actorEsResponsable, actorEsEvaluador, actorEsAdministrador);
        auditarBusqueda(contexto, asignacion, normalizada, page, true);
        return new ResultadoConsulta(page, page.items());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DetalleConsulta> obtenerDetalle(Long id, ConsultaInstitucionalAuthContext contexto) {
        validarContexto(contexto);
        Objects.requireNonNull(id, "El identificador es obligatorio.");
        AutorizacionEfectivaService.AsignacionEfectiva asignacion = revalidarAsignacion(contexto, null);
        Set<Long> unidadesVisibles = unidadesVisibles(contexto, asignacion);
        Optional<InstitutionalPortfolioProjection> proyeccion = portafolioReader
                .obtener(id, List.copyOf(unidadesVisibles));
        if (proyeccion.isEmpty()) {
            auditarDetalle(contexto, asignacion, id, false);
            return Optional.of(new DetalleConsulta(null, false));
        }
        InstitutionalPortfolioProjection registro = proyeccion.get();
        List<DocumentoInstitucionalMetadata> documentos = documentoReader
                .listarPorRegistro(registro.id());
        boolean actorEsResponsable = actorEsResponsableDelRegistro(asignacion, registro);
        boolean actorEsEvaluador = perfilEs(asignacion, "Evaluador");
        boolean actorEsAdministrador = perfilEs(asignacion, "GlobalAdmin")
                || perfilEs(asignacion, "UnidadAdmin");
        InstitutionalPortfolioDetail detalle = InstitutionalPortfolioMapper.aDetalle(
                registro, documentos, actorEsResponsable, actorEsEvaluador, actorEsAdministrador,
                null, null);
        auditarDetalle(contexto, asignacion, registro.id(), true);
        return Optional.of(new DetalleConsulta(detalle, true));
    }

    private void validarContexto(ConsultaInstitucionalAuthContext contexto) {
        if (contexto == null) {
            throw ConsultaInstitucionalValidationException.sinAsignacionEfectiva();
        }
        if (contexto.asignacionEfectivaId() == null) {
            throw ConsultaInstitucionalValidationException.sinAsignacionEfectiva();
        }
        if (contexto.actorSub() == null || contexto.actorSub().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "AUTENTICACION_REQUERIDA: la consulta exige un sujeto autenticado.");
        }
    }

    private AutorizacionEfectivaService.AsignacionEfectiva revalidarAsignacion(
            ConsultaInstitucionalAuthContext contexto, Long unidadRecurso) {
        Long unidadParaRevalidar = unidadRecurso;
        if (unidadParaRevalidar == null && contexto.unidadesVisibles() != null
                && !contexto.unidadesVisibles().isEmpty()) {
            unidadParaRevalidar = contexto.unidadesVisibles().get(0);
        }
        try {
            return autorizacion.revalidarAsignacionInstitucional(
                    contexto.actorSub(), contexto.asignacionEfectivaId(), unidadParaRevalidar);
        } catch (ResponseStatusException ex) {
            auditarDenegacion(contexto, ex.getReason() == null ? "ASSIGNMENT_SCOPE_DENIED"
                    : ex.getReason());
            throw ex;
        }
    }

    private Set<Long> unidadesVisibles(ConsultaInstitucionalAuthContext contexto,
            AutorizacionEfectivaService.AsignacionEfectiva asignacion) {
        if (contexto.unidadesVisibles() != null && !contexto.unidadesVisibles().isEmpty()) {
            return contexto.unidadesVisibles().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toUnmodifiableSet());
        }
        if (asignacion == null) {
            return Set.of();
        }
        return Set.of(asignacion.unidadId());
    }

    private boolean perfilEs(AutorizacionEfectivaService.AsignacionEfectiva asignacion, String perfil) {
        if (asignacion == null) {
            return false;
        }
        return perfil != null && perfil.equalsIgnoreCase(asignacion.perfil());
    }

    private boolean actorEsResponsableDelRegistro(AutorizacionEfectivaService.AsignacionEfectiva asignacion,
            InstitutionalPortfolioProjection registro) {
        if (asignacion == null || registro == null) {
            return false;
        }
        if (!"Responsable".equalsIgnoreCase(asignacion.perfil())) {
            return false;
        }
        return Objects.equals(asignacion.usuarioId(), registro.responsableId());
    }

    private void auditarBusqueda(ConsultaInstitucionalAuthContext contexto,
            AutorizacionEfectivaService.AsignacionEfectiva asignacion,
            InstitutionalPortfolioQuery consulta, InstitutionalPortfolioPage page, boolean exito) {
        Map<String, String> cambios = new HashMap<>();
        cambios.put("operacion", OPERACION_BUSCAR);
        if (consulta != null) {
            cambios.put("filtroTipo", consulta.tipoRegistro() == null ? "" : consulta.tipoRegistro().name());
            cambios.put("filtroEstado", consulta.estado() == null ? "" : consulta.estado());
            cambios.put("filtroUnidadId", consulta.unidadId() == null ? "" : consulta.unidadId().toString());
            cambios.put("filtroResponsableId", consulta.responsableId() == null
                    ? "" : consulta.responsableId().toString());
            cambios.put("pagina", Integer.toString(consulta.paginaNormalizada()));
            cambios.put("tamanio", Integer.toString(consulta.tamanioNormalizado()));
        }
        if (page != null) {
            cambios.put("totalElementos", Long.toString(page.totalElementos()));
        }
        cambios.put("resultado", exito ? "SUCCESS" : "EMPTY");
        auditoria.registrarAccesoExitoso(new AuditService.AuditAccessCommand(
                new AuditService.AuditCommand(
                        contexto.correlationId(),
                        asignacion == null ? null : asignacion.usuarioId(),
                        null,
                        contexto.asignacionEfectivaId(),
                        asignacion == null ? null : asignacion.perfil(),
                        asignacion == null ? null : asignacion.unidadId(),
                        OPERACION_BUSCAR,
                        MODULO,
                        RECURSO_TIPO,
                        null,
                        exito ? "SUCCESS" : "EMPTY",
                        cambios,
                        CLASIFICACION_INTERNA),
                "/api/v1/consulta/institucional/portafolio", "GET", 200, null, 0,
                asignacion == null ? null : perfilId(asignacion)));
    }

    private void auditarDetalle(ConsultaInstitucionalAuthContext contexto,
            AutorizacionEfectivaService.AsignacionEfectiva asignacion, Long registroId, boolean exito) {
        Map<String, String> cambios = new HashMap<>();
        cambios.put("operacion", OPERACION_DETALLE);
        cambios.put("registroId", registroId == null ? "" : registroId.toString());
        cambios.put("resultado", exito ? "SUCCESS" : "NOT_FOUND");
        auditoria.registrarAccesoExitoso(new AuditService.AuditAccessCommand(
                new AuditService.AuditCommand(
                        contexto.correlationId(),
                        asignacion == null ? null : asignacion.usuarioId(),
                        null,
                        contexto.asignacionEfectivaId(),
                        asignacion == null ? null : asignacion.perfil(),
                        asignacion == null ? null : asignacion.unidadId(),
                        OPERACION_DETALLE,
                        MODULO,
                        RECURSO_TIPO,
                        registroId,
                        exito ? "SUCCESS" : "NOT_FOUND",
                        cambios,
                        CLASIFICACION_RESTRINGIDA),
                "/api/v1/consulta/institucional/portafolio/{id}", "GET", 200, null, 0,
                asignacion == null ? null : perfilId(asignacion)));
    }

    private void auditarDenegacion(ConsultaInstitucionalAuthContext contexto, String codigo) {
        Map<String, String> cambios = new HashMap<>();
        cambios.put("operacion", OPERACION_BUSCAR);
        cambios.put("motivo", codigo);
        auditoria.registrarAccesoDenegado(new AuditService.AuditAccessCommand(
                new AuditService.AuditCommand(
                        contexto.correlationId(),
                        null,
                        contexto.actorSub(),
                        contexto.asignacionEfectivaId(),
                        null,
                        null,
                        OPERACION_BUSCAR,
                        MODULO,
                        RECURSO_TIPO,
                        null,
                        codigo,
                        cambios,
                        CLASIFICACION_INTERNA),
                "/api/v1/consulta/institucional/portafolio", "GET", 403, null, 0, null));
    }

    private static Integer perfilId(AutorizacionEfectivaService.AsignacionEfectiva asignacion) {
        if (asignacion == null || asignacion.id() == null) {
            return null;
        }
        return asignacion.id().intValue();
    }
}
