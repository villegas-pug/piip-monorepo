package pe.gob.midagri.piip.portafolio.mapper;

import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pe.gob.midagri.piip.portafolio.dto.InitiativeDetail;
import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;
import pe.gob.midagri.piip.portafolio.entity.UnidadResponsableEntity;

/**
 * MapStruct mapper entre entidades de portafolio y DTOs de respuesta.
 */
@Mapper(componentModel = "spring")
public interface RegistroPortafolioMapper {

    @Mapping(target = "unidades", ignore = true)
    InitiativeDetail toInitiativeDetail(RegistroPortafolioEntity entity);

    InitiativeDetail.UnidadResponsableDetail toUnidadDetail(UnidadResponsableEntity entity);

    List<InitiativeDetail.UnidadResponsableDetail> toUnidadDetails(List<UnidadResponsableEntity> unidades);
}
