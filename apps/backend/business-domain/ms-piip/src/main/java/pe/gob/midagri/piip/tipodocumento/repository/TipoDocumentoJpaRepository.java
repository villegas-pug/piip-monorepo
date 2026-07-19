package pe.gob.midagri.piip.tipodocumento.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.gob.midagri.piip.tipodocumento.entity.TipoDocumentoEntity;

interface TipoDocumentoJpaRepository extends JpaRepository<TipoDocumentoEntity, Integer> {
}
