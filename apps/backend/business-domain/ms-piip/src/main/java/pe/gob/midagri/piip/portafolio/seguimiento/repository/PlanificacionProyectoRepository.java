package pe.gob.midagri.piip.portafolio.seguimiento.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.gob.midagri.piip.portafolio.seguimiento.entity.PlanificacionProyectoEntity;

/**
 * Repositorio JPA para {@code PLANIFICACION_PROYECTO} (DDL 015,
 * VIGENTE). La firma del repositorio se limita a las operaciones
 * que la capa de aplicacion necesita para el versionado append-only
 * (busqueda por id, ultima version de un proyecto y existencia de
 * planificacion vigente).
 */
@Repository
public interface PlanificacionProyectoRepository
        extends JpaRepository<PlanificacionProyectoEntity, Long> {

    /**
     * Recupera la ultima version de la planificacion del proyecto
     * indicada, ordenando por {@code VERSION} descendente.
     * Devuelve {@code Optional.empty()} si el proyecto aun no
     * tiene planificacion registrada.
     */
    java.util.Optional<PlanificacionProyectoEntity> findFirstByIdProyectoOrderByVersionDesc(
            Long idProyecto);

    /**
     * Indica si existe al menos una planificacion registrada para
     * el proyecto.
     */
    boolean existsByIdProyecto(Long idProyecto);

    /** Lista el historial append-only de planificaciones del proyecto. */
    List<PlanificacionProyectoEntity> findByIdProyectoOrderByVersionAsc(Long idProyecto);
}
