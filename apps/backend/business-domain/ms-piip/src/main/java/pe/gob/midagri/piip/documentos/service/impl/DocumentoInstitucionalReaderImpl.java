package pe.gob.midagri.piip.documentos.service.impl;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.midagri.piip.documentos.dto.DocumentoInstitucionalMetadata;
import pe.gob.midagri.piip.documentos.entity.DocumentoPublicacionEntity;
import pe.gob.midagri.piip.documentos.entity.DocumentoSerieEntity;
import pe.gob.midagri.piip.documentos.entity.DocumentoVersionEntity;
import pe.gob.midagri.piip.documentos.entity.TipoDocumentoEntity;
import pe.gob.midagri.piip.documentos.repository.DocumentoPublicacionRepository;
import pe.gob.midagri.piip.documentos.repository.DocumentoSerieRepository;
import pe.gob.midagri.piip.documentos.repository.DocumentoVersionRepository;
import pe.gob.midagri.piip.documentos.repository.TipoDocumentoRepository;
import pe.gob.midagri.piip.documentos.service.DocumentoInstitucionalReader;

/**
 * Implementación JPA del lector de metadatos documentales para
 * la consulta institucional. Entrega únicamente DTOs
 * {@link DocumentoInstitucionalMetadata} que omiten el BLOB y la
 * clave física; el contenido nunca abandona el módulo
 * {@code documentos} sin pasar por el endpoint
 * {@code /api/v1/documentos/{id}/contenido} con su
 * revalidación obligatoria.
 */
@Service
public class DocumentoInstitucionalReaderImpl implements DocumentoInstitucionalReader {

    private final DocumentoVersionRepository versionRepository;
    private final DocumentoSerieRepository serieRepository;
    private final DocumentoPublicacionRepository publicacionRepository;
    private final TipoDocumentoRepository tipoRepository;

    public DocumentoInstitucionalReaderImpl(DocumentoVersionRepository versionRepository,
            DocumentoSerieRepository serieRepository,
            DocumentoPublicacionRepository publicacionRepository,
            TipoDocumentoRepository tipoRepository) {
        this.versionRepository = versionRepository;
        this.serieRepository = serieRepository;
        this.publicacionRepository = publicacionRepository;
        this.tipoRepository = tipoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoInstitucionalMetadata> listarPorRegistro(Long registroPortafolioId) {
        if (registroPortafolioId == null) {
            return List.of();
        }
        List<DocumentoVersionEntity> versiones = versionRepository
                .findByRegistroPortafolioIdAndActivoAndInmutable(
                        registroPortafolioId, "S", "S");
        if (versiones.isEmpty()) {
            return List.of();
        }
        Map<Long, DocumentoSerieEntity> series = cargarSeries(versiones);
        Map<Integer, TipoDocumentoEntity> tipos = cargarTipos(series);
        Map<Long, DocumentoPublicacionEntity> publicaciones = cargarPublicaciones(versiones);
        return versiones.stream()
                .map(version -> aMetadato(version, series, tipos, publicaciones))
                .toList();
    }

    private Map<Long, DocumentoSerieEntity> cargarSeries(List<DocumentoVersionEntity> versiones) {
        List<Long> ids = versiones.stream()
                .map(DocumentoVersionEntity::getSerieId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return serieRepository.findAllById(ids).stream()
                .filter(s -> "S".equals(s.getActiva()))
                .collect(Collectors.toMap(DocumentoSerieEntity::getId, s -> s));
    }

    private Map<Integer, TipoDocumentoEntity> cargarTipos(Map<Long, DocumentoSerieEntity> series) {
        List<Integer> ids = series.values().stream()
                .map(DocumentoSerieEntity::getTipoDocumentoId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return tipoRepository.findAllById(ids).stream()
                .filter(t -> "S".equals(t.getActivo()))
                .collect(Collectors.toMap(TipoDocumentoEntity::getId, t -> t));
    }

    private Map<Long, DocumentoPublicacionEntity> cargarPublicaciones(
            List<DocumentoVersionEntity> versiones) {
        List<Long> ids = versiones.stream()
                .map(DocumentoVersionEntity::getId)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return publicacionRepository.findByDocumentoIdIn(ids).stream()
                .collect(Collectors.toMap(DocumentoPublicacionEntity::getDocumentoId, p -> p));
    }

    private DocumentoInstitucionalMetadata aMetadato(DocumentoVersionEntity version,
            Map<Long, DocumentoSerieEntity> series,
            Map<Integer, TipoDocumentoEntity> tipos,
            Map<Long, DocumentoPublicacionEntity> publicaciones) {
        DocumentoSerieEntity serie = version.getSerieId() == null
                ? null
                : series.get(version.getSerieId());
        TipoDocumentoEntity tipo = serie == null
                ? null
                : tipos.get(serie.getTipoDocumentoId());
        DocumentoPublicacionEntity publicacion = publicaciones.get(version.getId());
        boolean publicado = publicacion != null
                && serie != null
                && serie.getClasificacionValidada() != null
                && "PUBLICO".equals(serie.getClasificacionValidada().name());
        return new DocumentoInstitucionalMetadata(
                version.getId(),
                version.getSerieId(),
                version.getNumeroVersion(),
                serie == null ? null : serie.getTitulo(),
                version.getNombreOriginal(),
                version.getFormato(),
                version.getMimeType(),
                version.getTamanoBytes(),
                version.getHashSha256(),
                serie == null ? null : serie.getClasificacionPropuesta(),
                serie == null ? null : serie.getClasificacionValidada(),
                tipo == null ? null : tipo.getNombre(),
                tipo == null ? null : tipo.getContexto() == null ? null : tipo.getContexto().name(),
                publicado,
                version.getFechaCarga(),
                version.getUsuarioCargaId(),
                "\"" + version.getId() + "-" + version.getNumeroVersion() + "\"");
    }
}
