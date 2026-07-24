package pe.gob.midagri.piip.documentos.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento;

/**
 * Solicitud HTTP para crear un expediente institucional.
 * El modulo de origen, la unidad y el registrador se resuelven en el servidor
 * a partir del contexto autorizado; el cliente nunca los declara ni los recibe.
 */
public record CreateInstitutionalFileRequest(
        @NotBlank @Size(max = 500) String asunto,
        @NotBlank @Size(max = 200) String referenciaCasoUso,
        @NotNull ClasificacionDocumento clasificacion) {
}
