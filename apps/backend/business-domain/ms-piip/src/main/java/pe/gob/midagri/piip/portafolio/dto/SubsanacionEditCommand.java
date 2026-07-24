package pe.gob.midagri.piip.portafolio.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Comando HTTP para editar una subsanacion abierta por el Responsable titular.
 * Limita la correccion a los campos oficiales 5 al 12, 22 y 23; cualquier
 * mutacion fuera de este perimetro se rechaza con FIELD_NOT_EDITABLE.
 */
public record SubsanacionEditCommand(
        @Size(max = 500) String nombre,
        @Size(max = 30) String tipoSolucion,
        @Size(max = 50) String fuenteOrigen,
        @Size(max = 2000) String problemaPublico,
        @Size(max = 2000) String solucionPropuesta,
        @NotNull Long objetivoPeiId,
        @NotNull Long actividadPoiId,
        @NotNull List<UnidadResponsableItem> unidades,
        Boolean componenteDigital,
        @Size(max = 500) String detalleComponenteDigital,
        @Size(max = 1000) String nota
) {
    public record UnidadResponsableItem(
            @NotNull Long unidadId,
            boolean principal
    ) {}
}
