package pe.gob.midagri.piip.documentos.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

/**
 * Metadatos del multipart de carga documental. El binario llega como parte
 * {@code file} en el mismo {@code multipart/form-data}.
 * PIIP no modela estados, resultados ni operaciones antimalware; OGTI los
 * administra fuera de la aplicacion sobre el BLOB Oracle.
 */
public record UploadDocumentRequest(
        @NotNull @Valid DocumentOwnerDto owner,
        @NotNull Integer tipoDocumentoId,
        @NotBlank @Size(max = 500) String titulo,
        @NotNull ClasificacionDocumento clasificacionPropuesta) {
}
