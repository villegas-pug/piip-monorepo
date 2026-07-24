package pe.gob.midagri.piip.portafolio.seguimiento.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.gob.midagri.piip.portafolio.seguimiento.entity.ParticipantePersonaEntity;

/**
 * Repositorio JPA para PARTICIPANTE_PERSONA (DDL 012, VIGENTE).
 * Catalogo transversal de personas participantes con o sin
 * cuenta PIIP, conservado de forma inmutable y reutilizable por
 * varios proyectos.
 */
@Repository
public interface ParticipantePersonaRepository
        extends JpaRepository<ParticipantePersonaEntity, Long> {

    /**
     * Busca la persona participante asociada a una cuenta PIIP.
     * La unicidad por usuario se mantiene en la capa de servicio
     * (UK implicita en CHECK CK_PP_DATOS_MINIMOS).
     */
    Optional<ParticipantePersonaEntity> findByUsuarioId(Long usuarioId);
}
