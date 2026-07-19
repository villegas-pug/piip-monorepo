package pe.gob.midagri.piip.tipodocumento.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;
import pe.gob.midagri.piip.tipodocumento.entity.TipoDocumentoEntity;

@Repository
@RequiredArgsConstructor
class TipoDocumentoRepositoryImpl implements TipoDocumentoRepository {

    private final TipoDocumentoJpaRepository jpaRepository;

    @Override
    public List<TipoDocumentoEntity> findAll() {
        return this.jpaRepository.findAll();
    }

    @Override
    public Optional<TipoDocumentoEntity> findById(Integer id) {
        return this.jpaRepository.findById(id);
    }
}
