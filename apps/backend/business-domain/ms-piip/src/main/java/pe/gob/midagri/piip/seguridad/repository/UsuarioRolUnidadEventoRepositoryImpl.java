package pe.gob.midagri.piip.seguridad.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import pe.gob.midagri.piip.seguridad.entity.UsuarioRolUnidadEventoEntity;

@Repository
@RequiredArgsConstructor
class UsuarioRolUnidadEventoRepositoryImpl implements UsuarioRolUnidadEventoRepository {
    private final EntityManager entityManager;

    @Override
    public UsuarioRolUnidadEventoEntity append(UsuarioRolUnidadEventoEntity evento) {
        entityManager.persist(evento);
        return evento;
    }

    @Override
    public List<UsuarioRolUnidadEventoEntity> findByAsignacionId(Long asignacionId) {
        return entityManager.createQuery("""
                select e from UsuarioRolUnidadEventoEntity e
                where e.asignacionId = :asignacionId order by e.fechaEvento asc
                """, UsuarioRolUnidadEventoEntity.class)
                .setParameter("asignacionId", asignacionId)
                .getResultList();
    }
}
