package pe.gob.midagri.piip.portafolio.dto;

import jakarta.validation.constraints.*;
import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;
import pe.gob.midagri.piip.portafolio.entity.TipoSolucion;
import java.util.List;

/**
 * Comando de entrada para presentar una iniciativa nueva.
 * No acepta código, código de origen, fecha de inicio ni estado.
 */
public record CreateInitiativeRequest(
        @NotBlank @Size(max = 500) String nombre,
        @NotNull TipoSolucion tipoSolucion,
        @NotNull FuenteOrigen fuenteOrigen,
        @Size(max = 500) String detalleFuente,
        @NotBlank @Size(max = 2000) String problemaPublico,
        @Size(max = 2000) String solucionPropuesta,
        @NotNull Long responsableId,
        @NotNull Long objetivoPeiId,
        @NotNull Long actividadPoiId,
        @NotEmpty @Size(min = 1) List<UnidadResponsableItem> unidades,
        List<ParticipantePersonaItem> participantesPersona,
        List<ParticipanteUnidadItem> participantesUnidad,
        @NotNull Boolean componenteDigital,
        @Size(max = 500) String detalleComponenteDigital,
        @Size(max = 1000) String nota,
        @NotNull Long fichaDocumentoVersionId
) {
    public record UnidadResponsableItem(
            @NotNull Long unidadId,
            @NotNull Boolean principal
    ) {}

    public record ParticipantePersonaItem(
            Long personaId,
            @NotBlank String nombresCompletos,
            String institucion,
            String funcion
    ) {}

    public record ParticipanteUnidadItem(
            @NotNull Long unidadId
    ) {}
}
