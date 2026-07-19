package pe.gob.midagri.piip.tipodocumento.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import pe.gob.midagri.piip.shareddata.mappers.BaseMapStructConfig;
import pe.gob.midagri.piip.tipodocumento.dto.TipoDocumentoResponse;
import pe.gob.midagri.piip.tipodocumento.entity.TipoDocumentoEntity;

@Mapper(config = BaseMapStructConfig.class)
public interface TipoDocumentoMapper {

    @Mapping(target = "obligatorio", expression = "java(\"S\".equals(entity.getObligatorio()))")
    @Mapping(target = "activo", expression = "java(\"S\".equals(entity.getActivo()))")
    TipoDocumentoResponse toResponse(TipoDocumentoEntity entity);
}
