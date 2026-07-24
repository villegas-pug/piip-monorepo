package pe.gob.midagri.piip.portafolio.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;

/**
 * Comando HTTP para crear un proyecto derivado a partir de una iniciativa
 * aprobada (US3, Constitucion 5.0.0).
 *
 * <p>El derivado conserva el codigo de origen, el tipo de solucion y el
 * problema/solucion copiados de la iniciativa; exige documento formal de
 * aprobacion o autorizacion de inicio (campo 15) y el nombre propio
 * obligatorio (campo 5). El estado del proyecto nace en
 * {@code PROYECTO_EJECUCION} sin modificar el estado de la iniciativa
 * origen, que permanece en {@code INICIATIVA_APROBADA}.
 *
 * <p>Reglas validadas por {@code CrearProyectoDerivadoServiceImpl}:
 * <ul>
 *   <li>{@code nombre} obligatorio y con longitud maxima 500 (campo 5).</li>
 *   <li>Al menos una unidad responsable y exactamente una principal
 *       (campo 12, regla UNIT_MAIN_CARDINALITY).</li>
 *   <li>{@code fuenteOrigen} obligatoria y dentro del catalogo canonico 019.</li>
 *   <li>{@code componenteDigital} y su detalle coherentes (campo 22).</li>
 *   <li>{@code documentoFormalId} obligatorio (campo 15, regla
 *       FORMAL_DOCUMENT_REQUIRED).</li>
 * </ul>
 */
public record CreateDerivedProjectRequest(
        @NotBlank @Size(max = 500) String nombre,
        @NotNull Long objetivoPeiId,
        @NotNull Long actividadPoiId,
        @NotNull List<UnidadDerivadaItem> unidades,
        @NotNull Long titularId,
        @NotNull FuenteOrigen fuenteOrigen,
        @NotBlank @Size(max = 2000) String descripcion,
        @NotNull Boolean componenteDigital,
        @Size(max = 500) String detalleComponenteDigital,
        @Size(max = 1000) String nota,
        @NotNull Long documentoFormalId
) {

    /**
     * Unidad responsable del proyecto derivado. El indicador
     * {@code principal} identifica a la unidad principal; el conjunto
     * debe contener exactamente una.
     */
    public record UnidadDerivadaItem(
            @NotNull Long unidadId,
            @NotNull Boolean principal
    ) {
    }
}
