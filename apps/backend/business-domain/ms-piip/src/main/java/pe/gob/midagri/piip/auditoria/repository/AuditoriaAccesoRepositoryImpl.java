package pe.gob.midagri.piip.auditoria.repository;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import pe.gob.midagri.piip.auditoria.entity.AuditoriaAccesoEntity;

@Repository
@RequiredArgsConstructor
class AuditoriaAccesoRepositoryImpl implements AuditoriaAccesoRepository {
    private final EntityManager entityManager;

    @Override
    public AuditoriaAccesoEntity append(AuditoriaAccesoEntity acceso) {
        entityManager.persist(acceso);
        return acceso;
    }
}
