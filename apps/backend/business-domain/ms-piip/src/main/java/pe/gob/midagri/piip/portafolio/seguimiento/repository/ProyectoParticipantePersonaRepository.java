package pe.gob.midagri.piip.portafolio.seguimiento.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.gob.midagri.piip.portafolio.seguimiento.entity.ProyectoParticipantePersonaEntity;

/**
 * Repositorio JPA para PROYECTO_PARTICIPANTE_PERSONA (DDL 012,
 * VIGENTE). Mantiene la vigencia de la participacion de una
 * persona en un proyecto: mientras FIN sea null la participacion
 * esta vigente; al cerrarla se asigna FIN a la fecha del servidor
 * y nunca se elimina la fila (borrado logico obligatorio).
 *
 * <p>El UK UK_PPP_PROY_PART impide duplicar la misma persona en
 * el mismo proyecto, lo que permite detectar altas duplicadas de
 * forma temprana.
 */
@Repository
public interface ProyectoParticipantePersonaRepository
        extends JpaRepository<ProyectoParticipantePersonaEntity, Long> {

    /**
     * Lista todas las participaciones (vigentes y finalizadas) de
     * un proyecto en orden cronologico ascendente. El listado
     * historico se construye a partir de este metodo.
     */
    List<ProyectoParticipantePersonaEntity> findByIdProyectoOrderByInicioAsc(Long idProyecto);

    /**
     * Lista las participaciones vigentes (FIN IS NULL) de un
     * proyecto en orden cronologico ascendente.
     */
    List<ProyectoParticipantePersonaEntity> findByIdProyectoAndFinIsNullOrderByInicioAsc(
            Long idProyecto);

    /**
     * Busca una participacion especifica de una persona en un
     * proyecto. Se usa para resolver la relacion entre la fila
     * PROYECTO_PARTICIPANTE_PERSONA y el responsable titular
     * vigente (PROYECTO_RESPONSABLE) a fin de inferir el rol.
     */
    Optional<ProyectoParticipantePersonaEntity> findByIdProyectoAndIdParticipante(
            Long idProyecto, Long idParticipante);
}
