package pe.gob.midagri.piip.portafolio.seguimiento.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.gob.midagri.piip.portafolio.seguimiento.entity.CicloProyectoEntity;

/**
 * Repositorio JPA para {@code CICLO_PROYECTO} (DDL 015, VIGENTE).
 * Expone la busqueda por proyecto y periodo (para detectar
 * duplicados) y la consulta de todas las versiones del ciclo de un
 * proyecto en orden cronologico.
 */
@Repository
public interface CicloProyectoRepository extends JpaRepository<CicloProyectoEntity, Long> {

    /**
     * Busca el ciclo del proyecto con el periodo indicado. La
     * version actual del ciclo se distingue en la capa de servicio
     * comparando con el campo {@code numeroVersion} o
     * {@code cerrado}.
     */
    Optional<CicloProyectoEntity> findByIdProyectoAndPeriodo(Long idProyecto, String periodo);

    /**
     * Lista todas las filas del proyecto (todas las versiones) en
     * orden cronologico ascendente por periodo y version. La
     * primera fila (version 1) representa la apertura original del
     * ciclo; las siguientes son correcciones append-only.
     */
    List<CicloProyectoEntity> findByIdProyectoOrderByPeriodoAscNumeroVersionAsc(Long idProyecto);
}
