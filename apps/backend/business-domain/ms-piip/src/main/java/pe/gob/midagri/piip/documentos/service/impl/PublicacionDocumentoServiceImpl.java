package pe.gob.midagri.piip.documentos.service.impl;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import pe.gob.midagri.piip.auditoria.service.AuditService;
import pe.gob.midagri.piip.auditoria.service.IdempotencyService;
import pe.gob.midagri.piip.documentos.dto.DocumentoAuthorizedContext;
import pe.gob.midagri.piip.documentos.dto.PublicacionDocumentoDetail;
import pe.gob.midagri.piip.documentos.dto.PublicarDocumentoRequest;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.entity.DocumentoPublicacionEntity;
import pe.gob.midagri.piip.documentos.entity.DocumentoVersionEntity;
import pe.gob.midagri.piip.documentos.repository.DocumentoPublicacionRepository;
import pe.gob.midagri.piip.documentos.repository.DocumentoVersionRepository;
import pe.gob.midagri.piip.documentos.service.PublicacionDocumentoService;

/**
 * Implementación JPA de la confirmación de publicación
 * documental. Impide publicar si la versión no es apta, conserva
 * la fecha del servidor y registra la operación de forma
 * idempotente. Una reclasificación más restrictiva posterior
 * excluye la versión de proyecciones públicas sin eliminar la
 * publicación, la auditoría ni el historial.
 */
@Service
public class PublicacionDocumentoServiceImpl implements PublicacionDocumentoService {

    private final DocumentoVersionRepository versionRepository;
    private final DocumentoPublicacionRepository publicacionRepository;
    private final IdempotencyService idempotencyService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public PublicacionDocumentoServiceImpl(DocumentoVersionRepository versionRepository,
            DocumentoPublicacionRepository publicacionRepository,
            IdempotencyService idempotencyService,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.versionRepository = versionRepository;
        this.publicacionRepository = publicacionRepository;
        this.idempotencyService = idempotencyService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Override
    public PublicacionDocumentoDetail confirmarPublicacion(
            DocumentoAuthorizedContext contexto, String idempotencyKey, PublicarDocumentoRequest solicitud) {
        validarContexto(contexto);
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED");
        }
        if (solicitud == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "PUBLICACION_REQUERIDA");
        }
        if (solicitud.documentoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DOCUMENTO_REQUERIDO");
        }
        if (solicitud.tituloPublico() == null || solicitud.tituloPublico().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TITULO_PUBLICO_REQUERIDO");
        }
        validarTituloPublico(solicitud.tituloPublico());
        IdempotencyService.IdempotencyResult resultado = idempotencyService.execute(
                new IdempotencyService.IdempotencyRequest(
                        "DOCUMENTOS",
                        "CONFIRMAR_PUBLICACION",
                        idempotencyKey,
                        payloadIdempotente(solicitud),
                        contexto.actorSub()),
                () -> ejecutarPublicacion(contexto, solicitud));
        try {
            return objectMapper.readValue(resultado.respuestaJson(), PublicacionDocumentoDetail.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible deserializar la respuesta de publicación.", exception);
        }
    }

    private IdempotencyService.IdempotencyResponse ejecutarPublicacion(
            DocumentoAuthorizedContext contexto, PublicarDocumentoRequest solicitud) {
        DocumentoVersionEntity documento = cargarVersion(solicitud.documentoId());
        if (documento.getClasificacionValidada() != ClasificacionDocumento.PUBLICO) {
            // Solo se puede publicar una versión cuya clasificación validada sea PUBLICO
            // (BR-084 y FR-095). La reclasificación posterior a una clase más restrictiva
            // ya se gestiona en la proyección pública al no considerar la versión.
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "PUBLICACION_REQUIERE_CLASIFICACION_PUBLICO");
        }
        if (publicacionRepository.findByDocumentoId(documento.getId()).isPresent()) {
            // La UK sobre ID_DOCUMENTO garantiza idempotencia estructural; aquí
            // devolvemos un error 409 para que el cliente use la clave de
            // idempotencia correcta. Una clave nueva con el mismo documento se
            // resuelve igualmente con el mismo registro.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "PUBLICACION_DUPLICADA");
        }
        DocumentoPublicacionEntity publicacion = new DocumentoPublicacionEntity();
        publicacion.setDocumentoId(documento.getId());
        publicacion.setTituloPublico(solicitud.tituloPublico().trim());
        publicacion.setEvaluadorConfirmadorId(contexto.actorUsuarioId());
        publicacion.setAsignacionEfectivaId(contexto.asignacionEfectiva().id());
        DocumentoPublicacionEntity persistida = publicacionRepository.saveAndFlush(publicacion);
        // Se refresca la entidad para capturar la fecha del servidor fijada por
        // el DEFAULT SYSTIMESTAMP del DDL 004.
        DocumentoPublicacionEntity refrescada = publicacionRepository.findById(persistida.getId())
                .orElse(persistida);
        auditService.registrarExito(new AuditService.AuditCommand(
                contexto.correlacionId(),
                contexto.actorUsuarioId(),
                null,
                contexto.asignacionEfectiva().id(),
                contexto.asignacionEfectiva().perfil(),
                contexto.unidadRecursoId(),
                "CONFIRMAR_PUBLICACION",
                "DOCUMENTOS",
                "DOCUMENTO_PUBLICACION",
                refrescada.getId(),
                "SUCCESS",
                Map.of(
                        "documentoId", String.valueOf(documento.getId()),
                        "tituloPublico", solicitud.tituloPublico().trim()),
                ClasificacionDocumento.PUBLICO.name()));
        PublicacionDocumentoDetail detalle = new PublicacionDocumentoDetail(
                refrescada.getId(),
                refrescada.getDocumentoId(),
                refrescada.getTituloPublico(),
                documento.getClasificacionValidada(),
                refrescada.getEvaluadorConfirmadorId(),
                refrescada.getAsignacionEfectivaId(),
                refrescada.getFechaPublicacion());
        try {
            return new IdempotencyService.IdempotencyResponse(
                    "DOCUMENTO_PUBLICACION", detalle.publicacionId(),
                    objectMapper.writeValueAsString(detalle));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible serializar la respuesta de publicación.", exception);
        }
    }

    private void validarTituloPublico(String titulo) {
        // El CHECK de formato del DDL exige: no nulo, longitud >= 5, sin '@' y sin
        // 9 a 12 dígitos consecutivos. La capa Java revalida para evitar carrera
        // entre la lectura del título y la inserción en DOCUMENTO_PUBLICACION.
        String limpio = titulo.trim();
        if (limpio.length() < 5) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "TITULO_PUBLICO_LONGITUD");
        }
        if (limpio.contains("@")) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "TITULO_PUBLICO_CARACTERES");
        }
        if (limpio.matches(".*\\d{9,12}.*")) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "TITULO_PUBLICO_SECUENCIA_NUMERICA");
        }
    }

    private String payloadIdempotente(PublicarDocumentoRequest solicitud) {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("documentoId", solicitud.documentoId());
        payload.put("tituloPublico", solicitud.tituloPublico().trim());
        payload.put("autoridadPublica", solicitud.autoridadPublica());
        payload.put("resumenPublico", solicitud.resumenPublico());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("No fue posible serializar el payload de publicación.", exception);
        }
    }

    private DocumentoVersionEntity cargarVersion(Long documentoId) {
        DocumentoVersionEntity documento = versionRepository.findById(documentoId).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCUMENTO_NOT_FOUND"));
        if (!"S".equals(documento.getActivo()) || !"S".equals(documento.getInmutable())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "DOCUMENTO_NOT_FOUND");
        }
        if (documento.getHashSha256() == null || documento.getHashSha256().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "DOCUMENTO_INTEGRIDAD_INSUFICIENTE");
        }
        return documento;
    }

    private void validarContexto(DocumentoAuthorizedContext contexto) {
        if (contexto == null || contexto.actorUsuarioId() == null
                || contexto.asignacionEfectiva() == null) {
            throw new IllegalArgumentException("Contexto documental autorizado obligatorio.");
        }
    }
}
