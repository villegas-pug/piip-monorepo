package pe.gob.midagri.piip.tipodocumento.repository;

import java.util.List;
import java.util.Optional;

import pe.gob.midagri.piip.tipodocumento.entity.TipoDocumentoEntity;

public interface TipoDocumentoRepository {

    List<TipoDocumentoEntity> findAll();

    Optional<TipoDocumentoEntity> findById(Integer id);
}
