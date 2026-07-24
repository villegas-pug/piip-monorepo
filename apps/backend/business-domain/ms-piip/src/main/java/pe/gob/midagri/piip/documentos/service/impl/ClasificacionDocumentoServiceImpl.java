package pe.gob.midagri.piip.documentos.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.documentos.dto.ClasificacionHistDetalle;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.ReclasificarDocumentoCommand;
import pe.gob.midagri.piip.documentos.dto.ReclasificacionDocumentoResult;
import pe.gob.midagri.piip.documentos.dto.ValidacionClasificacionResult;
import pe.gob.midagri.piip.documentos.dto.ValidarClasificacionCommand;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.entity.DocumentoClasificacionHistEntity;
import pe.gob.midagri.piip.documentos.entity.DocumentoVersionEntity;
import pe.gob.midagri.piip.documentos.entity.ResultadoClasificacion;
import pe.gob.midagri.piip.documentos.repository.DocumentoClasificacionHistRepository;
import pe.gob.midagri.piip.documentos.repository.DocumentoVersionRepository;
import pe.gob.midagri.piip.documentos.service.ClasificacionDocumentoService;

/**
 * Implementación JPA de la autoridad de validación y
 * reclasificación documental. Conserva la regla restrictiva
 * constitucional: la reclasificación nunca puede ser menos
 * restrictiva que la clasificación validada actual. Las
 * reclasificaciones a {@code PUBLICO} exigen una decisión formal
 * registrada como documento previo. La validación inicial no
 * genera un evento de historial porque no existe una Autoridad
 * distinta que la haya decidido: la primera clasificación validada
 * del documento es responsabilidad exclusiva del Evaluador y se
 * conserva en los campos de la fila {@code DOCUMENTO}.
 */
@Service
public class ClasificacionDocumentoServiceImpl implements ClasificacionDocumentoService {

    private final DocumentoVersionRepository versionRepository;
    private final DocumentoClasificacionHistRepository histRepository;
    private final AuditService auditService;

    public ClasificacionDocumentoServiceImpl(DocumentoVersionRepository versionRepository,
            DocumentoClasificacionHistRepository histRepository,
            AuditService auditService) {
        this.versionRepository = versionRepository;
        this.histRepository = histRepository;
        this.auditService = auditService;
    }

    @Override
    @Transactional
    public ValidacionClasificacionResult validarClasificacion(
            DocumentoAuthorizedContext contexto, Long documentoId,
            String ifMatch, ValidarClasificacionCommand comando) {
        validarContexto(contexto);
        if (comando == null || comando.clasificacion() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLASIFICACION_REQUERIDA");
        }
        DocumentoVersionEntity documento = cargarVersion(documentoId);
        validarEtag(documento, ifMatch);
        ClasificacionDocumento anterior = documento.getClasificacionValidada();
        ClasificacionDocumento nueva = comando.clasificacion();
        // La validación inicial no exige decisión formal de la Autoridad. Cuando ya
        // existe una clasificación validada, el cambio exige el flujo de
        // reclasificación; por eso se rechaza una supuesta validación que solo
        // podría relajar la clasificación.
        if (anterior != null && nivelRestrictividad(nueva) < nivelRestrictividad(anterior)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "CLASIFICACION_RECLASIFICACION_REQUERIDA");
        }
        documento.setClasificacionValidada(nueva);
        documento.setUsuarioValidaId(contexto.actorUsuarioId());
        versionRepository.save(documento);
        DocumentoVersionEntity refrescado = versionRepository.findById(documentoId).orElse(documento);
        auditService.registrarExito(auditoria(contexto, "VALIDAR_CLASIFICACION", "DOCUMENTO",
                documentoId, nueva.name()));
        return new ValidacionClasificacionResult(documentoId, anterior, refrescado.getClasificacionValidada(),
                refrescado.getClasificacionFecha(), refrescado.getUsuarioValidaId(), etag(refrescado));
    }

    @Override
    @Transactional
    public ReclasificacionDocumentoResult reclasificar(
            DocumentoAuthorizedContext contexto, Long documentoId,
            String ifMatch, ReclasificarDocumentoCommand comando) {
        validarContexto(contexto);
        if (comando == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "RECLASIFICACION_REQUERIDA");
        }
        if (comando.clasificacionNueva() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "CLASIFICACION_REQUERIDA");
        }
        if (comando.autoridadDecisoraId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AUTORIDAD_DECISORA_REQUERIDA");
        }
        if (comando.documentoDecisionId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DOCUMENTO_DECISION_REQUERIDO");
        }
        if (Objects.equals(comando.autoridadDecisoraId(), contexto.actorUsuarioId())) {
            // La constitución exige decisor distinto del registrador (PSA-009 y BR-023).
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "AUTORIDAD_DISTINTA_REGISTRADOR");
        }
        DocumentoVersionEntity documento = cargarVersion(documentoId);
        validarEtag(documento, ifMatch);
        ClasificacionDocumento anterior = documento.getClasificacionValidada();
        // Antes de reclasificar, la clasificación validada debe existir; en otro
        // caso el flujo correcto es validar la clasificación inicial.
        if (anterior == null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "DOCUMENTO_SIN_CLASIFICACION_VALIDADA");
        }
        if (Objects.equals(anterior, comando.clasificacionNueva())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "RECLASIFICACION_SIN_CAMBIO");
        }
        if (nivelRestrictividad(comando.clasificacionNueva()) < nivelRestrictividad(anterior)) {
            // Regla restrictiva constitucional: una reclasificación nunca puede relajar
            // la clasificación. El CHECK de transición de la fila de historial aplica
            // la misma restricción; aquí se revalida para evitar carrera entre el
            // SELECT y la inserción del historial.
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "RECLASIFICACION_MENOS_RESTRICTIVA");
        }
        if (ClasificacionDocumento.PUBLICO == comando.clasificacionNueva()) {
            // Una reclasificación a PUBLICO exige decisión formal de la Autoridad y un
            // documento de decisión registrado (BR-023). El documento de decisión debe
            // ser una versión documental apta.
            DocumentoVersionEntity decision = cargarVersion(comando.documentoDecisionId());
            if (Objects.equals(decision.getId(), documento.getId())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "DOCUMENTO_DECISION_DISTINTO");
            }
        }
        documento.setClasificacionValidada(comando.clasificacionNueva());
        documento.setUsuarioValidaId(contexto.actorUsuarioId());
        versionRepository.save(documento);
        DocumentoVersionEntity refrescado = versionRepository.findById(documentoId).orElse(documento);
        ClasificacionHistDetalle detalle = registrarHistorial(documentoId, anterior,
                comando.clasificacionNueva(), comando.autoridadDecisoraId(),
                contexto.actorUsuarioId(), comando.documentoDecisionId(), comando.motivo(),
                ResultadoClasificacion.APLICADA);
        auditService.registrarExito(auditoria(contexto, "RECLASIFICAR_DOCUMENTO", "DOCUMENTO",
                documentoId, comando.clasificacionNueva().name()));
        return new ReclasificacionDocumentoResult(documentoId, anterior, refrescado.getClasificacionValidada(),
                etag(refrescado), detalle);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClasificacionHistDetalle> listarHistorial(
            DocumentoAuthorizedContext contexto, Long documentoId) {
        validarContexto(contexto);
        // La carga previa del documento garantiza que el actor tiene un contexto
        // autorizado sobre un recurso que existe. La autorización efectiva se aplicó
        // en la fase del contexto; aquí se omite un recheck redundante para evitar
        // dobles bloqueos pesimistas innecesarios.
        cargarVersion(documentoId);
        return histRepository.findByDocumentoIdOrderByFechaCambioAsc(documentoId).stream()
                .map(this::aDetalle)
                .toList();
    }

    private ClasificacionHistDetalle registrarHistorial(Long documentoId,
            ClasificacionDocumento anterior, ClasificacionDocumento nueva,
            Long autoridadDecisoraId, Long evaluadorRegistradorId,
            Long documentoDecisionId, String motivo, ResultadoClasificacion resultado) {
        if (Objects.equals(autoridadDecisoraId, evaluadorRegistradorId)) {
            // La constitución exige decisor distinto del registrador (PSA-009 y BR-023).
            // Esta salvaguarda Java refuerza el CHECK CK_DCH_AUTORIDAD_DISTINTA_EVALUADOR
            // del DDL para evitar carrera entre la lectura y la inserción.
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "AUTORIDAD_DISTINTA_REGISTRADOR");
        }
        DocumentoClasificacionHistEntity hist = new DocumentoClasificacionHistEntity();
        hist.setDocumentoId(documentoId);
        hist.setClasificacionAnterior(anterior);
        hist.setClasificacionNueva(nueva);
        hist.setAutoridadDecisoraId(autoridadDecisoraId);
        hist.setEvaluadorRegistradorId(evaluadorRegistradorId);
        hist.setDocumentoDecisionId(documentoDecisionId);
        hist.setMotivo(motivo);
        hist.setResultado(resultado);
        DocumentoClasificacionHistEntity persistido = histRepository.saveAndFlush(hist);
        return aDetalle(persistido);
    }

    private ClasificacionHistDetalle aDetalle(DocumentoClasificacionHistEntity entity) {
        return new ClasificacionHistDetalle(
                entity.getId(),
                entity.getDocumentoId(),
                entity.getClasificacionAnterior(),
                entity.getClasificacionNueva(),
                entity.getAutoridadDecisoraId(),
                entity.getEvaluadorRegistradorId(),
                entity.getDocumentoDecisionId(),
                entity.getMotivo(),
                entity.getFechaCambio(),
                entity.getResultado());
    }

    private DocumentoVersionEntity cargarVersion(Long documentoId) {
        if (documentoId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DOCUMENTO_REQUERIDO");
        }
        DocumentoVersionEntity documento = versionRepository.findById(documentoId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCUMENTO_NOT_FOUND"));
        if (!"S".equals(documento.getActivo()) || !"S".equals(documento.getInmutable())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCUMENTO_NOT_FOUND");
        }
        return documento;
    }

    private void validarEtag(DocumentoVersionEntity documento, String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED,
                    "DOCUMENT_VERSION_ETAG_REQUIRED");
        }
        String actual = etag(documento);
        if (!actual.equals(ifMatch)) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED,
                    "DOCUMENT_VERSION_ETAG_MISMATCH");
        }
    }

    private void validarContexto(DocumentoAuthorizedContext contexto) {
        if (contexto == null || contexto.actorUsuarioId() == null
                || contexto.asignacionEfectiva() == null) {
            throw new IllegalArgumentException("Contexto documental autorizado obligatorio.");
        }
    }

    private String etag(DocumentoVersionEntity documento) {
        return "\"" + documento.getId() + "-" + documento.getNumeroVersion() + "-"
                + (documento.getClasificacionValidada() == null ? "P" : documento.getClasificacionValidada().name())
                + "\"";
    }

    private AuditService.AuditCommand auditoria(DocumentoAuthorizedContext contexto,
            String operacion, String recursoTipo, Long recursoId, String clasificacion) {
        return new AuditService.AuditCommand(
                contexto.correlacionId(),
                contexto.actorUsuarioId(),
                null,
                contexto.asignacionEfectiva().id(),
                contexto.asignacionEfectiva().perfil(),
                contexto.unidadRecursoId(),
                operacion,
                "DOCUMENTOS",
                recursoTipo,
                recursoId,
                "SUCCESS",
                Map.of("documentoId", String.valueOf(recursoId)),
                clasificacion);
    }

    /**
     * Nivel de restrictividad creciente: PUBLICO &lt; INTERNO &lt; RESTRINGIDO.
     * Se usa para aplicar la regla restrictiva constitucional: una
     * reclasificación nunca puede tener un nivel menor que la
     * clasificación validada actual.
     */
    private int nivelRestrictividad(ClasificacionDocumento clasificacion) {
        if (clasificacion == null) {
            return 0;
        }
        return switch (clasificacion) {
            case PUBLICO -> 1;
            case INTERNO -> 2;
            case RESTRINGIDO -> 3;
        };
    }
}
