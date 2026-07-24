package pe.gob.midagri.piip.portafolio.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;

/**
 * Salida HTTP con el detalle de un proyecto (US3, Constitucion 5.0.0).
 *
 * <p>Para un proyecto derivado, {@code iniciativaId} mantiene el vinculo
 * inmutable con la iniciativa origen; para un proyecto directo, dicho
 * campo es nulo. El estado nace en {@code PROYECTO_EJECUCION} y nunca
 * queda {@code null}. La respuesta nunca expone entidades JPA: los
 * enums canonicos del portafolio se reutilizan como tipos simples.
 */
public record ProjectDetail(
        Long id,
        Long iniciativaId,
        String codigo,
        String codigoOrigen,
        LocalDate fechaInicio,
        String nombre,
        TipoRegistro tipoRegistro,
        EstadoIniciativa estado,
        FuenteOrigen fuenteOrigen,
        String detalleFuente,
        Long responsableId,
        String problemaPublico,
        String solucionPropuesta,
        Long objetivoPeiId,
        Long actividadPoiId,
        List<UnidadResponsableDetail> unidades,
        Boolean componenteDigital,
        String detalleComponenteDigital,
        String nota,
        Long documentoFormalId,
        Long version,
        String etag,
        LocalDateTime fechaCreacion
) {

    /**
     * Unidad responsable del proyecto. En la creacion del derivado, la
     * coleccion se copia de la iniciativa y puede ajustarse antes de
     * confirmar; la unidad principal es exactamente una.
     */
    public record UnidadResponsableDetail(
            Long id,
            Long unidadId,
            String descripcion,
            String abreviatura,
            Boolean principal
    ) {
    }
}
