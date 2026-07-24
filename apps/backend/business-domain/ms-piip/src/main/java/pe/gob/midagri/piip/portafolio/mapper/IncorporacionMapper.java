package pe.gob.midagri.piip.portafolio.mapper;

import org.mapstruct.Mapper;
import pe.gob.midagri.piip.portafolio.dto.IncorporacionDetail;
import pe.gob.midagri.piip.portafolio.entity.IncorporacionRegistroEntity;

/**
 * MapStruct mapper entre entidades de incorporación y DTOs de respuesta.
 */
@Mapper(componentModel = "spring")
public interface IncorporacionMapper {

    IncorporacionDetail toIncorporacionDetail(IncorporacionRegistroEntity entity);
}
