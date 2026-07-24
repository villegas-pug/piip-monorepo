package pe.gob.midagri.piip.documentos.dto;

/** Resultado mínimo de aptitud documental, sin exponer entidades ni binarios. */
public record DocumentoEvidenciaApta(long documentoId, String tipoDocumental, boolean apto) {
}
