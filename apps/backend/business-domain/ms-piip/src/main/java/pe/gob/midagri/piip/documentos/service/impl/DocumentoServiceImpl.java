package pe.gob.midagri.piip.documentos.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.documentos.dto.*;
import pe.gob.midagri.piip.documentos.entity.*;
import pe.gob.midagri.piip.documentos.repository.*;
import pe.gob.midagri.piip.documentos.service.DocumentoService;
import pe.gob.midagri.piip.documentos.service.DocumentoService.ContenidoInstitucional;
import pe.gob.midagri.piip.documentos.service.DocumentStorage;

@Service
public class DocumentoServiceImpl implements DocumentoService {
    private static final long MAX_BYTES = 104857600L;
    private final TipoDocumentoRepository tipoDocumentoRepository; private final ExpedienteInstitucionalRepository expedienteRepository;
    private final DocumentoSerieRepository serieRepository; private final DocumentoVersionRepository versionRepository;
    private final IdempotencyService idempotencyService; private final AuditService auditService; private final ObjectMapper objectMapper;
    private final DocumentStorage documentStorage;
    public DocumentoServiceImpl(TipoDocumentoRepository tipoDocumentoRepository, ExpedienteInstitucionalRepository expedienteRepository, DocumentoSerieRepository serieRepository, DocumentoVersionRepository versionRepository, IdempotencyService idempotencyService, AuditService auditService, ObjectMapper objectMapper, DocumentStorage documentStorage) {
        this.tipoDocumentoRepository=tipoDocumentoRepository; this.expedienteRepository=expedienteRepository; this.serieRepository=serieRepository; this.versionRepository=versionRepository; this.idempotencyService=idempotencyService; this.auditService=auditService; this.objectMapper=objectMapper; this.documentStorage=documentStorage;
    }
    @Override @Transactional(readOnly = true)
    public AptitudDocumental obtenerAptitud(Integer id) {
        TipoDocumentoEntity tipo = tipoDocumentoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCUMENT_TYPE_NOT_FOUND"));
        return new AptitudDocumental(tipo.getId(), tipo.getNombre(), tipo.getContexto(), tipo.getClasificacionDefecto(), "S".equals(tipo.getObligatorio()), "S".equals(tipo.getActivo()));
    }
    @Override @Transactional(readOnly = true)
    public DocumentoEvidenciaApta validarEvidencia(long documentoId, String tipoDocumental) {
        DocumentoVersionEntity documento = versionRepository.findById(documentoId).orElse(null);
        if (documento == null || tipoDocumental == null || tipoDocumental.isBlank()
                || !"S".equals(documento.getActivo()) || !"S".equals(documento.getInmutable())
                || documento.getHashSha256() == null || documento.getHashSha256().isBlank()) {
            return new DocumentoEvidenciaApta(documentoId, tipoDocumental, false);
        }
        TipoDocumentoEntity tipo = tipoDocumentoRepository.findById(documento.getTipoDocumentoId()).orElse(null);
        DocumentoSerieEntity serie = documento.getSerieId() == null ? null
                : serieRepository.findById(documento.getSerieId()).orElse(null);
        boolean apto = tipo != null && "S".equals(tipo.getActivo())
                && tipoDocumental.equals(tipo.getNombre()) && serie != null
                && "S".equals(serie.getActiva()) && serie.getClasificacionValidada() != null;
        return new DocumentoEvidenciaApta(documentoId, tipoDocumental, apto);
    }
    @Override @Transactional(readOnly = true)
    public DocumentoInstitucionalAprobatorio validarDocumentoInstitucionalAprobatorio(long documentoId) {
        DocumentoVersionEntity documento = versionRepository.findById(documentoId).orElse(null);
        if (documento == null || documento.getSerieId() == null || !"S".equals(documento.getActivo())
                || !"S".equals(documento.getInmutable()) || documento.getHashSha256() == null) {
            return new DocumentoInstitucionalAprobatorio(documentoId, null, false);
        }
        DocumentoSerieEntity serie = serieRepository.findById(documento.getSerieId()).orElse(null);
        boolean valido = serie != null && serie.getExpedienteInstitucionalId() != null
                && serie.getRegistroPortafolioId() == null && "S".equals(serie.getActiva());
        return new DocumentoInstitucionalAprobatorio(documentoId,
                serie == null ? null : serie.getExpedienteInstitucionalId(), valido);
    }

    @Override @Transactional(readOnly = true)
    public Long obtenerUnidadExpediente(Long expedienteId) {
        if (expedienteId == null) {
            return null;
        }
        return expedienteRepository.findById(expedienteId)
                .filter(e -> "S".equals(e.getActivo()))
                .map(ExpedienteInstitucionalEntity::getUnidadId)
                .orElse(null);
    }

    @Override @Transactional(readOnly = true)
    public Long obtenerExpedienteDeSerie(Long serieId) {
        if (serieId == null) {
            return null;
        }
        return serieRepository.findById(serieId)
                .map(DocumentoSerieEntity::getExpedienteInstitucionalId)
                .orElse(null);
    }
    @Override @Transactional(readOnly = true)
    public ContenidoInstitucional obtenerContenidoInstitucional(DocumentoAuthorizedContext c, Long documentoId) {
        validarContexto(c);
        if (documentoId == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DOCUMENTO_REQUERIDO");
        DocumentoVersionEntity documento = versionRepository.findById(documentoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCUMENTO_NOT_FOUND"));
        if (!"S".equals(documento.getActivo()) || !"S".equals(documento.getInmutable())) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCUMENTO_NOT_FOUND");
        // La autorización efectiva ya validó perfil, asignación y unidad exactos;
        // se revalida que la unidad del recurso coincida con la unidad efectiva.
        if (c.recursoId() != null && !c.recursoId().equals(documentoId)) {
            // En este endpoint, el recurso es el propio documento: el contexto
            // se construyó con documentoId; aquí solo se exige coherencia.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DOCUMENT_OWNER_IMMUTABLE");
        }
        if (documento.getClasificacionValidada() == null) {
            // Documentos sin clasificación validada solo pueden consultarlos
            // el Responsable cargador o el Evaluador (BR-087 / FR-098). El
            // caso del Responsable cargador se gestiona en la consulta
            // específica del módulo portafolio; aquí exigimos clasificación
            // validada como filtro de seguridad institucional.
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DOCUMENTO_CLASIFICACION_PENDIENTE");
        }
        byte[] binario = documentStorage.leerContenido(documentoId);
        ContenidoDocumentoResponse metadatos = new ContenidoDocumentoResponse(
                documento.getId(), documento.getSerieId(), documento.getNumeroVersion(),
                documento.getNombreOriginal(), documento.getMimeType(), documento.getFormato(),
                documento.getTamanoBytes() == null ? 0L : documento.getTamanoBytes(),
                documento.getHashSha256(), documento.getClasificacionValidada(),
                documento.getFechaCarga(), etag(documento));
        return new ContenidoInstitucional(metadatos, binario);
    }
    @Override
    public DocumentVersionDetail cargarEnExpediente(DocumentoAuthorizedContext c, String key, UploadDocumentCommand cmd) {
        validarContexto(c);
        return ejecutarIdempotente(c, key, cmd, () -> crearInicial(c, cmd));
    }
    @Override
    public DocumentVersionDetail crearVersion(DocumentoAuthorizedContext c, Long serieId, String key, String etag, UploadDocumentCommand cmd) {
        validarContexto(c); return ejecutarIdempotente(c, key, cmd, () -> crearSiguiente(c, serieId, etag, cmd));
    }
    @Transactional
    protected DocumentVersionDetail crearInicial(DocumentoAuthorizedContext c, UploadDocumentCommand cmd) {
        expedienteRepository.findById(c.recursoId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "INSTITUTIONAL_FILE_NOT_FOUND"));
        TipoDocumentoEntity tipo = tipoValido(cmd.tipoDocumentoId(), ContextoTipoDocumento.INSTITUCIONAL);
        validarContenido(cmd);
        DocumentoSerieEntity serie = new DocumentoSerieEntity(); serie.setTipoDocumentoId(tipo.getId()); serie.setExpedienteInstitucionalId(c.recursoId()); serie.setTitulo(cmd.titulo()); serie.setClasificacionPropuesta(cmd.clasificacionPropuesta()); serie.setActiva("S"); serie.setCreadoPor(c.actorSub()); serieRepository.save(serie);
        return persistirVersion(c, serie, null, 1, cmd);
    }
    @Transactional
    protected DocumentVersionDetail crearSiguiente(DocumentoAuthorizedContext c, Long serieId, String etag, UploadDocumentCommand cmd) {
        DocumentoSerieEntity serie = serieRepository.findById(serieId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCUMENT_SERIES_NOT_FOUND"));
        if (!c.recursoId().equals(serie.getExpedienteInstitucionalId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "DOCUMENT_OWNER_IMMUTABLE");
        TipoDocumentoEntity tipo = tipoValido(cmd.tipoDocumentoId(), ContextoTipoDocumento.INSTITUCIONAL);
        if (!tipo.getId().equals(serie.getTipoDocumentoId())) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "DOCUMENT_OWNER_IMMUTABLE");
        var anteriores = versionRepository.findBySerieIdOrderByNumeroVersionDesc(serieId);
        DocumentoVersionEntity anterior = anteriores.isEmpty() ? null : anteriores.getFirst();
        if (anterior == null || !etag(anterior).equals(etag)) throw new ResponseStatusException(HttpStatus.PRECONDITION_FAILED, "DOCUMENT_VERSION_ETAG_MISMATCH");
        validarContenido(cmd); return persistirVersion(c, serie, anterior, anterior.getNumeroVersion() + 1, cmd);
    }
    private DocumentVersionDetail persistirVersion(DocumentoAuthorizedContext c, DocumentoSerieEntity serie, DocumentoVersionEntity anterior, int numero, UploadDocumentCommand cmd) {
        DocumentoVersionEntity v = new DocumentoVersionEntity(); v.setSerieId(serie.getId()); v.setTipoDocumentoId(serie.getTipoDocumentoId()); v.setNombreOriginal(cmd.nombreOriginal()); v.setMimeType(cmd.mimeType()); v.setFormato(formato(cmd.mimeType())); v.setTamanoBytes((long) cmd.contenido().length); v.setHashSha256(hash(cmd.contenido())); v.setUsuarioCargaId(c.actorUsuarioId()); v.setActivo("S"); v.setInmutable("S"); v.setNumeroVersion(numero); v.setDocumentoAnteriorId(anterior == null ? null : anterior.getId()); v.setClasificacion(cmd.clasificacionPropuesta()); v.setContenido(cmd.contenido()); versionRepository.save(v);
        auditService.registrarExito(new AuditService.AuditCommand(c.correlacionId(), c.actorUsuarioId(), null, c.asignacionEfectiva().id(), c.asignacionEfectiva().perfil(), c.unidadRecursoId(), anterior == null ? "CARGAR_DOCUMENTO" : "VERSIONAR_DOCUMENTO", "DOCUMENTOS", "DOCUMENTO", v.getId(), "SUCCESS", Map.of("serieId", serie.getId().toString()), cmd.clasificacionPropuesta().name()));
        return new DocumentVersionDetail(v.getId(), serie.getId(), numero, serie.getTitulo(), v.getFormato(), v.getTamanoBytes(), v.getHashSha256(), serie.getClasificacionPropuesta(), serie.getClasificacionValidada(), serie.getClasificacionValidada() != null, etag(v));
    }
    private DocumentVersionDetail ejecutarIdempotente(DocumentoAuthorizedContext c, String key, UploadDocumentCommand cmd, java.util.function.Supplier<DocumentVersionDetail> op) {
        if (key == null || key.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED");
        try { var r=idempotencyService.execute(new IdempotencyService.IdempotencyRequest("DOCUMENTOS", "CARGAR_DOCUMENTO", key, payloadIdempotente(cmd), c.actorSub()), () -> { DocumentVersionDetail d=op.get(); try { return new IdempotencyService.IdempotencyResponse("DOCUMENTO", d.documentoId(), objectMapper.writeValueAsString(d)); } catch(JsonProcessingException e){ throw new IllegalStateException(e); }}); return objectMapper.readValue(r.respuestaJson(), DocumentVersionDetail.class); } catch(JsonProcessingException e) { throw new IllegalStateException("No fue posible serializar la respuesta documental.", e); }
    }
    private String payloadIdempotente(UploadDocumentCommand c) throws JsonProcessingException {
        if (c == null || c.contenido() == null) return "{}";
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("tipoDocumentoId", c.tipoDocumentoId()); payload.put("titulo", c.titulo());
        payload.put("nombreOriginal", c.nombreOriginal()); payload.put("mimeType", c.mimeType());
        payload.put("clasificacion", c.clasificacionPropuesta()); payload.put("sha256CalculadoServidor", hash(c.contenido()));
        return objectMapper.writeValueAsString(payload);
    }
    private TipoDocumentoEntity tipoValido(Integer id, ContextoTipoDocumento contexto) { TipoDocumentoEntity t=tipoDocumentoRepository.findByIdAndActivo(id, "S").orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"DOCUMENT_TYPE_NOT_ALLOWED")); if(t.getContexto()!=contexto) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"DOCUMENT_TYPE_CONTEXT_MISMATCH"); return t; }
    private void validarContexto(DocumentoAuthorizedContext c) { if(c==null||c.actorUsuarioId()==null||c.asignacionEfectiva()==null||c.recursoId()==null) throw new IllegalArgumentException("Contexto documental autorizado obligatorio."); }
    private void validarContenido(UploadDocumentCommand c) { if(c==null||c.contenido()==null||c.contenido().length==0) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,"DOCUMENT_CONTENT_REQUIRED"); if(c.contenido().length>MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"DOCUMENT_TOO_LARGE"); if(!mimePermitido(c.mimeType(),c.contenido())) throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,"DOCUMENT_TYPE_NOT_ALLOWED"); }
    private boolean mimePermitido(String m, byte[] b) { return ("application/pdf".equals(m)&&b.length>=5&&b[0]=='%'&&b[1]=='P'&&b[2]=='D'&&b[3]=='F') || ("image/png".equals(m)&&b.length>=8&&b[0]==(byte)137&&b[1]==80&&b[2]==78&&b[3]==71) || ("image/jpeg".equals(m)&&b.length>=3&&b[0]==(byte)255&&b[1]==(byte)216&&b[2]==(byte)255) || ("application/vnd.openxmlformats-officedocument.wordprocessingml.document".equals(m)||"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet".equals(m)||"application/vnd.openxmlformats-officedocument.presentationml.presentation".equals(m))&&b.length>=4&&b[0]==80&&b[1]==75; }
    private String formato(String mime) { return mime.substring(mime.lastIndexOf('/')+1); }
    private String hash(byte[] bytes) { try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException("SHA-256 no disponible",e);} }
    private String etag(DocumentoVersionEntity v) { return "\""+v.getId()+"-"+v.getNumeroVersion()+"-"+v.getHashSha256()+"\""; }
}
