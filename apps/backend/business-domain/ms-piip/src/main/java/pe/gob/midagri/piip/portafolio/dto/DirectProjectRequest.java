package pe.gob.midagri.piip.portafolio.dto;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;

/**
 * Comando HTTP para crear un proyecto directo heredado o excepcional (US3, Constitucion
 * 5.0.0, contrato {@code portafolio.md}).
 *
 * <p>El proyecto directo exige, como minimo, los campos oficiales 1 al 13 y 22 de la
 * matriz de campos, respetando los autogenerados (codigo, estado, fecha del servidor) y
 * haciendo opcional la nota. La fecha de inicio del proyecto directo coincide con la del
 * documento formal. El campo 23 ({@code nota}) es opcional.
 *
 * <p>Reglas validadas por {@code CrearProyectoDirectoServiceImpl}:
 * <ul>
 *   <li>{@code tipoOrigen} obligatorio ({@link TipoOrigenDirecto}); distingue entre
 *       proyecto heredado y excepcion formal.</li>
 *   <li>{@link TipoOrigenDirecto#HEREDADO} exige {@code codigoOrigen} no vacio.</li>
 *   <li>{@link TipoOrigenDirecto#EXCEPCION_FORMAL} exige {@code documentoAutorizacionId}
 *       como acto formal.</li>
 *   <li>{@code nombre}, {@code objetivoPeiId}, {@code actividadPoiId},
 *       {@code unidadResponsableId} y {@code responsableId} son obligatorios.</li>
 *   <li>{@code fechaInicio} obligatoria; la fija el servidor a partir del documento
 *       formal.</li>
 *   <li>{@code fuenteOrigen} obligatoria y dentro del catalogo canonico.</li>
 *   <li>{@code evidenciaIds} exige al menos una evidencia disponible.</li>
 *   <li>{@code componenteDigital} obligatorio; si es verdadero, exige
 *       {@code detalleComponenteDigital}.</li>
 * </ul>
 *
 * <p>La DTO no expone el identificador, codigo, estado, fecha de creacion, version ni
 * otros campos autogenerados por el servidor.
 */
public record DirectProjectRequest(

        @NotNull TipoOrigenDirecto tipoOrigen,

        @Size(max = 50) String codigoOrigen,

        @NotNull LocalDate fechaInicio,

        @NotBlank @Size(max = 500) String nombre,

        @NotNull Long objetivoPeiId,

        @NotNull Long actividadPoiId,

        @NotNull Long unidadResponsableId,

        @NotNull Long responsableId,

        @NotBlank @Size(max = 2000) String descripcion,

        @NotNull Boolean componenteDigital,

        @Size(max = 500) String detalleComponenteDigital,

        @Size(max = 1000) String nota,

        @NotNull Long documentoAutorizacionId,

        @NotEmpty List<Long> evidenciaIds,

        @NotNull FuenteOrigen fuenteOrigen
) {
}
