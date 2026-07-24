package pe.gob.midagri.piip.documentos.dto;

import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

/** Comando interno. El propietario proviene del contexto autorizado y no del cliente. */
public record UploadDocumentCommand(
        Integer tipoDocumentoId, String titulo, String nombreOriginal, String mimeType,
        ClasificacionDocumento clasificacionPropuesta, byte[] contenido) { }
