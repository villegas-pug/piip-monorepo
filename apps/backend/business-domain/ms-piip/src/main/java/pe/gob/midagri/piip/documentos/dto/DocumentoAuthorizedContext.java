package pe.gob.midagri.piip.documentos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import pe.gob.midagri.piip.seguridad.service.AutorizacionEfectivaService.AsignacionEfectiva;

/** Contexto interno emitido solo tras la autorización Oracle del caso de uso propietario. */
@JsonIgnoreType
public record DocumentoAuthorizedContext(
        String actorSub,
        Long actorUsuarioId,
        AsignacionEfectiva asignacionEfectiva,
        Long unidadRecursoId,
        Long recursoId,
        String correlacionId) {
}
