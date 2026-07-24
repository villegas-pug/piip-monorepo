package pe.gob.midagri.piip.documentos.dto;

import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

public record DocumentVersionDetail(
        Long documentoId, Long serieId, int version, String titulo, String formato, long tamanoBytes,
        String hashSha256, ClasificacionDocumento clasificacionPropuesta,
        ClasificacionDocumento clasificacionValidada, boolean aptaComoEvidencia, String etag) { }
