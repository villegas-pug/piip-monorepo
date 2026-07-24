package pe.gob.midagri.piip.auditoria.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;
import pe.gob.midagri.piip.auditoria.entity.SolicitudIdempotenteEntity;

@Repository
@RequiredArgsConstructor
class SolicitudIdempotenteRepositoryImpl implements SolicitudIdempotenteRepository {
    private final EntityManager entityManager;

    @Override
    public SolicitudIdempotenteEntity createAndFlush(SolicitudIdempotenteEntity solicitud) {
        entityManager.persist(solicitud);
        entityManager.flush();
        return solicitud;
    }

    @Override
    public Optional<SolicitudIdempotenteEntity> findByConsumidorOperacionClaveForUpdate(
            String consumidor, String operacion, String clave) {
        try {
            return Optional.of(entityManager.createQuery("""
                    select s from SolicitudIdempotenteEntity s
                    where s.consumidor = :consumidor and s.operacion = :operacion and s.clave = :clave
                    """, SolicitudIdempotenteEntity.class)
                    .setParameter("consumidor", consumidor)
                    .setParameter("operacion", operacion)
                    .setParameter("clave", clave)
                    .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                    .getSingleResult());
        } catch (NoResultException exception) {
            return Optional.empty();
        }
    }
}
