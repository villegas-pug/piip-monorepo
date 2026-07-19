package pe.gob.midagri.piip.tipodocumento.mapper;

import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;
import pe.gob.midagri.piip.tipodocumento.dto.TipoDocumentoResponse;
import pe.gob.midagri.piip.tipodocumento.entity.TipoDocumentoEntity;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-07-19T02:51:38-0500",
    comments = "version: 1.5.5.Final, compiler: Eclipse JDT (IDE) 3.46.100.v20260624-0231, environment: Java 21.0.11 (Eclipse Adoptium)"
)
@Component
public class TipoDocumentoMapperImpl implements TipoDocumentoMapper {

    @Override
    public TipoDocumentoResponse toResponse(TipoDocumentoEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Integer id = null;
        String nombre = null;
        String estadoAsociado = null;
        String descripcion = null;
        String anexoNormativo = null;

        if ( entity.getId() != null ) {
            id = entity.getId();
        }
        if ( entity.getNombre() != null ) {
            nombre = entity.getNombre();
        }
        if ( entity.getEstadoAsociado() != null ) {
            estadoAsociado = entity.getEstadoAsociado();
        }
        if ( entity.getDescripcion() != null ) {
            descripcion = entity.getDescripcion();
        }
        if ( entity.getAnexoNormativo() != null ) {
            anexoNormativo = entity.getAnexoNormativo();
        }

        boolean obligatorio = "S".equals(entity.getObligatorio());
        boolean activo = "S".equals(entity.getActivo());

        TipoDocumentoResponse tipoDocumentoResponse = new TipoDocumentoResponse( id, nombre, estadoAsociado, obligatorio, descripcion, anexoNormativo, activo );

        return tipoDocumentoResponse;
    }
}
