package pe.gob.midagri.piip.documentos.dto;

import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

/**
 * Metadatos documentales visibles para la consulta institucional
 * autorizada. No expone el BLOB, la clave física ni la URL
 * directa. El módulo {@code consulta} consume este DTO a través
 * del servicio de {@code documentos} sin necesidad de importar
 * ninguna entidad JPA.
 *
 * <p>La regla constitucional prohíbe la descarga pública del
 * contenido. La consulta institucional accede al contenido solo
 * por el endpoint {@code /api/v1/documentos/{id}/contenido} que
 * revalida el ámbito y la clasificación validada.
 */
public record DocumentoInstitucionalMetadata(
        Long documentoId,
        Long serieId,
        Integer numeroVersion,
        String titulo,
        String nombreOriginal,
        String formato,
        String mimeType,
        Long tamanoBytes,
        String hashSha256,
        ClasificacionDocumento clasificacionPropuesta,
        ClasificacionDocumento clasificacionValidada,
        String tipoDocumental,
        String contextoDocumental,
        boolean publicado,
        java.time.LocalDateTime fechaCarga,
        Long usuarioCargaId,
        String etag) { }
