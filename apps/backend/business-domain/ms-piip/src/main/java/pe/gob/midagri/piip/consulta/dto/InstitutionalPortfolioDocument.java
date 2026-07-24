package pe.gob.midagri.piip.consulta.dto;

import java.time.LocalDateTime;

/**
 * Documento asociado a un registro del portafolio visible en la
 * consulta institucional. No expone BLOB, clave física ni URL
 * directa; el contenido se obtiene por el endpoint
 * {@code /api/v1/documentos/{id}/contenido} con la
 * revalidación obligatoria del ámbito y la clasificación
 * validada. La colección no incluye los expedientes
 * institucionales.
 */
public record InstitutionalPortfolioDocument(
        Long documentoId,
        Long serieId,
        Integer numeroVersion,
        String titulo,
        String formato,
        String mimeType,
        Long tamanoBytes,
        String hashSha256,
        ClasificacionDocumentoConsulta clasificacionPropuesta,
        ClasificacionDocumentoConsulta clasificacionValidada,
        String tipoDocumental,
        String contextoDocumental,
        boolean publicado,
        LocalDateTime fechaCarga,
        Long usuarioCargaId,
        boolean puedeConsultarContenido,
        String etag) { }
