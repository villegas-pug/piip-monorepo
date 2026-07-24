package pe.gob.midagri.piip.portafolio.dto;

import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;
import pe.gob.midagri.piip.portafolio.entity.FuenteOrigen;
import pe.gob.midagri.piip.portafolio.entity.TipoRegistro;
import pe.gob.midagri.piip.portafolio.entity.TipoSolucion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Salida con los 23 campos oficiales de una iniciativa o proyecto.
 */
public record InitiativeDetail(
        Long id,
        TipoRegistro tipoRegistro,
        String codigo,
        String codigoOrigen,
        LocalDate fechaInicio,
        String nombre,
        TipoSolucion tipoSolucion,
        FuenteOrigen fuenteOrigen,
        String detalleFuente,
        Long responsableId,
        String problemaPublico,
        String solucionPropuesta,
        Long objetivoPeiId,
        Long actividadPoiId,
        List<UnidadResponsableDetail> unidades,
        EstadoIniciativa estado,
        Boolean componenteDigital,
        String detalleComponenteDigital,
        String nota,
        Long version,
        String etag,
        LocalDateTime fechaCreacion
) {
    public record UnidadResponsableDetail(
            Long id,
            Long unidadId,
            String descripcion,
            String abreviatura,
            Boolean principal
    ) {}
}
