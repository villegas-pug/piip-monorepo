package pe.gob.midagri.piip.consulta.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pe.gob.midagri.piip.consulta.dto.PublicPortfolioDetail;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioDocumento;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioPage;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioQuery;
import pe.gob.midagri.piip.consulta.dto.PublicPortfolioSummary;
import pe.gob.midagri.piip.consulta.dto.TipoRegistroConsulta;
import pe.gob.midagri.piip.consulta.service.ConsultaPublicaService;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;
import pe.gob.midagri.piip.documentos.entity.DocumentoPublicacionEntity;
import pe.gob.midagri.piip.documentos.entity.DocumentoSerieEntity;
import pe.gob.midagri.piip.documentos.entity.DocumentoVersionEntity;
import pe.gob.midagri.piip.documentos.entity.TipoDocumentoEntity;
import pe.gob.midagri.piip.documentos.repository.DocumentoPublicacionRepository;
import pe.gob.midagri.piip.documentos.repository.DocumentoSerieRepository;
import pe.gob.midagri.piip.documentos.repository.DocumentoVersionRepository;
import pe.gob.midagri.piip.documentos.repository.TipoDocumentoRepository;
import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.repository.RegistroPortafolioRepository;

/**
 * Implementación JPA de la proyección pública anónima del
 * portafolio. Aplica la minimización constitucional: solo los
 * cuatro campos públicos y los metadatos de las publicaciones
 * elegibles. El módulo nunca consulta el BLOB ni genera URL de
 * descarga.
 *
 * <p>Estados excluidos por la constitución: {@code NO_ADMISIBLE},
 * {@code NO_APLICABLE}, {@code INICIATIVA_ARCHIVADA},
 * {@code SUSPENDIDO}, {@code CANCELADO} y {@code FINALIZADO} no
 * aparecen en la consulta pública. La reclasificación más
 * restrictiva posterior a una publicación confirmada excluye la
 * versión de futuras proyecciones públicas sin eliminar la fila
 * de {@code DOCUMENTO_PUBLICACION}.
 */
@Service
public class ConsultaPublicaServiceImpl implements ConsultaPublicaService {

    private static final Set<EstadoIniciativa> ESTADOS_ELEGIBLES = Set.of(
            EstadoIniciativa.INICIATIVA_APROBADA,
            EstadoIniciativa.PROYECTO_EJECUCION,
            EstadoIniciativa.PRODUCTO_APROBADO,
            EstadoIniciativa.PRODUCTO_NO_APROBADO);
    private static final int TAMANIO_MAXIMO = 100;
    private static final int TAMANIO_MINIMO = 1;

    private final RegistroPortafolioRepository registroRepository;
    private final DocumentoVersionRepository versionRepository;
    private final DocumentoSerieRepository serieRepository;
    private final DocumentoPublicacionRepository publicacionRepository;
    private final TipoDocumentoRepository tipoRepository;

    public ConsultaPublicaServiceImpl(RegistroPortafolioRepository registroRepository,
            DocumentoVersionRepository versionRepository,
            DocumentoSerieRepository serieRepository,
            DocumentoPublicacionRepository publicacionRepository,
            TipoDocumentoRepository tipoRepository) {
        this.registroRepository = registroRepository;
        this.versionRepository = versionRepository;
        this.serieRepository = serieRepository;
        this.publicacionRepository = publicacionRepository;
        this.tipoRepository = tipoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicPortfolioPage buscar(PublicPortfolioQuery consulta) {
        int pagina = Math.max(0, consulta == null ? 0 : consulta.page());
        int tamanio = clampSize(consulta == null ? 20 : consulta.size());
        if (consulta == null) {
            return new PublicPortfolioPage(List.of(), pagina, tamanio, 0L, 0, etagLista(List.of(), pagina, tamanio, 0L));
        }
        List<RegistroPortafolioEntity> registros = registroRepository.findAll().stream()
                .filter(r -> ESTADOS_ELEGIBLES.contains(r.getEstado()))
                .filter(r -> consulta.tipo() == null
                        || toTipoRegistroConsulta(r.getTipoRegistro()) == consulta.tipo())
                .filter(r -> consulta.codigo() == null || consulta.codigo().isBlank()
                        || consulta.codigo().equalsIgnoreCase(r.getCodigo()))
                .filter(r -> consulta.nombre() == null || consulta.nombre().isBlank()
                        || contieneIgnorandoAcentos(r.getNombre(), consulta.nombre()))
                .sorted((a, b) -> a.getId().compareTo(b.getId()))
                .toList();
        long total = registros.size();
        int totalPaginas = total == 0 ? 0 : (int) Math.ceil((double) total / (double) tamanio);
        int desde = Math.min(pagina * tamanio, registros.size());
        int hasta = Math.min(desde + tamanio, registros.size());
        List<RegistroPortafolioEntity> paginaFiltrada = registros.subList(desde, hasta);
        List<Long> ids = paginaFiltrada.stream().map(RegistroPortafolioEntity::getId).toList();
        Map<Long, List<PublicPortfolioDocumento>> publicacionesPorRegistro = cargarPublicaciones(ids);
        List<PublicPortfolioSummary> items = paginaFiltrada.stream()
                .map(r -> aResumen(r, publicacionesPorRegistro.getOrDefault(r.getId(), List.of())))
                .toList();
        return new PublicPortfolioPage(items, pagina, tamanio, total, totalPaginas,
                etagLista(items, pagina, tamanio, total));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PublicPortfolioDetail> obtenerDetalle(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        Optional<RegistroPortafolioEntity> registro = registroRepository.findById(id);
        if (registro.isEmpty() || !ESTADOS_ELEGIBLES.contains(registro.get().getEstado())) {
            return Optional.empty();
        }
        List<PublicPortfolioDocumento> publicaciones = cargarPublicaciones(List.of(id))
                .getOrDefault(id, List.of());
        return Optional.of(aDetalle(registro.get(), publicaciones));
    }

    private PublicPortfolioSummary aResumen(RegistroPortafolioEntity registro,
            List<PublicPortfolioDocumento> publicaciones) {
        return new PublicPortfolioSummary(
                registro.getId(),
                toTipoRegistroConsulta(registro.getTipoRegistro()),
                registro.getCodigo(),
                registro.getNombre(),
                registro.getEstado() == null ? null : registro.getEstado().name(),
                registro.getFechaInicio(),
                Collections.unmodifiableList(publicaciones),
                etagResumen(registro, publicaciones));
    }

    private PublicPortfolioDetail aDetalle(RegistroPortafolioEntity registro,
            List<PublicPortfolioDocumento> publicaciones) {
        return new PublicPortfolioDetail(
                registro.getId(),
                toTipoRegistroConsulta(registro.getTipoRegistro()),
                registro.getCodigo(),
                registro.getNombre(),
                registro.getEstado() == null ? null : registro.getEstado().name(),
                Collections.unmodifiableList(publicaciones),
                etagResumen(registro, publicaciones));
    }

    private Map<Long, List<PublicPortfolioDocumento>> cargarPublicaciones(List<Long> registroIds) {
        if (registroIds == null || registroIds.isEmpty()) {
            return Map.of();
        }
        // Carga las versiones documentales activas e inmutables de los registros.
        List<DocumentoVersionEntity> versiones = new ArrayList<>();
        for (Long id : registroIds) {
            versiones.addAll(versionRepository
                    .findByRegistroPortafolioIdAndActivoAndInmutable(id, "S", "S"));
        }
        if (versiones.isEmpty()) {
            return Map.of();
        }
        // Filtra a las versiones con clasificación validada PUBLICO. Una reclasificación
        // posterior más restrictiva las excluye automáticamente.
        List<DocumentoVersionEntity> publicables = versiones.stream()
                .filter(v -> v.getClasificacionValidada() == ClasificacionDocumento.PUBLICO)
                .toList();
        if (publicables.isEmpty()) {
            return Map.of();
        }
        List<Long> documentoIds = publicables.stream().map(DocumentoVersionEntity::getId).toList();
        Map<Long, DocumentoPublicacionEntity> publicaciones = publicacionRepository
                .findByDocumentoIdIn(documentoIds).stream()
                .collect(Collectors.toMap(DocumentoPublicacionEntity::getDocumentoId, p -> p));
        Map<Long, DocumentoSerieEntity> series = cargarSeries(publicables);
        Map<Integer, TipoDocumentoEntity> tipos = cargarTipos(series);
        // Agrupa las publicaciones por registro portafolio para producir el detalle.
        Map<Long, List<PublicPortfolioDocumento>> porRegistro = new java.util.HashMap<>();
        for (DocumentoVersionEntity version : publicables) {
            DocumentoPublicacionEntity publicacion = publicaciones.get(version.getId());
            if (publicacion == null) {
                // La publicación debe estar confirmada por el Evaluador. La ausencia
                // excluye la versión de la proyección pública.
                continue;
            }
            DocumentoSerieEntity serie = version.getSerieId() == null
                    ? null
                    : series.get(version.getSerieId());
            TipoDocumentoEntity tipo = serie == null
                    ? null
                    : tipos.get(serie.getTipoDocumentoId());
            PublicPortfolioDocumento documento = new PublicPortfolioDocumento(
                    tipo == null ? null : tipo.getNombre(),
                    publicacion.getTituloPublico(),
                    version.getNumeroVersion(),
                    version.getFormato(),
                    publicacion.getFechaPublicacion());
            porRegistro.computeIfAbsent(version.getRegistroPortafolioId(), k -> new ArrayList<>())
                    .add(documento);
        }
        return porRegistro;
    }

    private Map<Long, DocumentoSerieEntity> cargarSeries(List<DocumentoVersionEntity> versiones) {
        List<Long> ids = versiones.stream()
                .map(DocumentoVersionEntity::getSerieId)
                .filter(Objects::nonNull)
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
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        return tipoRepository.findAllById(ids).stream()
                .filter(t -> "S".equals(t.getActivo()))
                .collect(Collectors.toMap(TipoDocumentoEntity::getId, t -> t));
    }

    private TipoRegistroConsulta toTipoRegistroConsulta(TipoRegistro tipoRegistro) {
        if (tipoRegistro == null) {
            return null;
        }
        return switch (tipoRegistro) {
            case INICIATIVA -> TipoRegistroConsulta.INICIATIVA;
            case PROYECTO -> TipoRegistroConsulta.PROYECTO;
        };
    }

    private int clampSize(int size) {
        if (size < TAMANIO_MINIMO) {
            return TAMANIO_MINIMO;
        }
        return Math.min(size, TAMANIO_MAXIMO);
    }

    private boolean contieneIgnorandoAcentos(String texto, String filtro) {
        if (texto == null || filtro == null) {
            return false;
        }
        String normalizado = java.text.Normalizer.normalize(texto, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(java.util.Locale.ROOT);
        String filtroNormalizado = java.text.Normalizer.normalize(filtro, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(java.util.Locale.ROOT);
        return normalizado.contains(filtroNormalizado);
    }

    private String etagLista(List<PublicPortfolioSummary> items, int pagina, int tamanio, long total) {
        String contenido = items.stream()
                .map(PublicPortfolioSummary::etag)
                .reduce((a, b) -> a + "|" + b)
                .orElse("");
        return "\"" + Integer.toHexString(contenido.hashCode()) + "-"
                + pagina + "-" + tamanio + "-" + total + "\"";
    }

    private String etagResumen(RegistroPortafolioEntity registro, List<PublicPortfolioDocumento> publicaciones) {
        String publicacionesToken = publicaciones.stream()
                .map(p -> p.tipoDocumental() + "|" + p.version() + "|" + p.fechaPublicacion())
                .reduce((a, b) -> a + ";" + b)
                .orElse("");
        return "\"" + registro.getId() + "-" + registro.getVersion() + "-"
                + Integer.toHexString(publicacionesToken.hashCode()) + "\"";
    }
}
