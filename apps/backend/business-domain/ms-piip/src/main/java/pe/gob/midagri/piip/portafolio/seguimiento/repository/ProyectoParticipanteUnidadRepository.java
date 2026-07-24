package pe.gob.midagri.piip.portafolio.seguimiento.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.gob.midagri.piip.portafolio.seguimiento.entity.ProyectoParticipanteUnidadEntity;

/**
 * Repositorio JPA para PROYECTO_PARTICIPANTE_UNIDAD (DDL 012,
 * VIGENTE). Mantiene la vigencia de la participacion de una
 * unidad organizacional en un proyecto: mientras FIN sea null
 * la participacion esta vigente; al cerrarla se asigna FIN a la
 * fecha del servidor y nunca se elimina la fila (borrado logico
 * obligatorio).
 *
 * <p>El UK UK_PPU_PROY_UNI impide duplicar la misma unidad en
 * el mismo proyecto. El rol de una unidad participante es
 * siempre Participante segun el contrato del modulo portafolio.
 */
@Repository
public interface ProyectoParticipanteUnidadRepository
        extends JpaRepository<ProyectoParticipanteUnidadEntity, Long> {

    /**
     * Lista todas las participaciones (vigentes y finalizadas) de
     * unidades en un proyecto en orden cronologico ascendente.
     */
    List<ProyectoParticipanteUnidadEntity> findByIdProyectoOrderByInicioAsc(Long idProyecto);

    /**
     * Lista las participaciones vigentes (FIN IS NULL) de
     * unidades en un proyecto en orden cronologico ascendente.
     */
    List<ProyectoParticipanteUnidadEntity> findByIdProyectoAndFinIsNullOrderByInicioAsc(
            Long idProyecto);

    /**
     * Busca una participacion especifica de una unidad en un
     * proyecto para validar altas duplicadas o resolver la
     * participacion antes de una baja.
     */
    Optional<ProyectoParticipanteUnidadEntity> findByIdProyectoAndIdUnidad(
            Long idProyecto, Long idUnidad);
}
