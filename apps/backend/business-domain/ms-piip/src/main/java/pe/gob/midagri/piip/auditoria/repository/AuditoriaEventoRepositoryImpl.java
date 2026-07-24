package pe.gob.midagri.piip.auditoria.repository;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import pe.gob.midagri.piip.auditoria.entity.AuditoriaEventoEntity;

@Repository
@RequiredArgsConstructor
class AuditoriaEventoRepositoryImpl implements AuditoriaEventoRepository {
    private final EntityManager entityManager;

    @Override
    public AuditoriaEventoEntity append(AuditoriaEventoEntity evento) {
        entityManager.persist(evento);
        return evento;
    }
}
