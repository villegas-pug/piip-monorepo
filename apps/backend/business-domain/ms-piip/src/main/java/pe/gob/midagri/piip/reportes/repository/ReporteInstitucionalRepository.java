package pe.gob.midagri.piip.reportes.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import pe.gob.midagri.piip.reportes.entity.ReporteInstitucionalEntity;

/**
 * Repositorio del agregado raíz del módulo reportes
 * (DDL 017, tabla REPORTE_INSTITUCIONAL). La
 * constitución obliga a aplicar
 * {@code PESSIMISTIC_WRITE} cuando se generen
 * aprobaciones o remisiones para evitar carreras que
 * dupliquen la misma versión. La aprobación/remisión
 * es registrada, no automática, por lo que la fila
 * aprobada se selecciona exclusivamente con
 * {@code findByIdForUpdate}.
 */
public interface ReporteInstitucionalRepository
        extends JpaRepository<ReporteInstitucionalEntity, Long> {

    /**
     * Bloqueo pesimista del reporte para serializar la
     * aprobación o la remisión. Solo debe invocarse
     * desde una transacción activa.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM ReporteInstitucionalEntity r WHERE r.id = :id")
    Optional<ReporteInstitucionalEntity> findByIdForUpdate(@Param("id") Long id);

    /**
     * Búsqueda idempotente por la clave natural del reporte
     * semestral: tipo, año, semestre y periodo. Garantiza
     * que dos llamadas con los mismos filtros obtengan el
     * mismo {@code idReporte} sin generar duplicados.
     */
    @Query("SELECT r FROM ReporteInstitucionalEntity r "
            + "WHERE r.tipo = :tipo AND r.anio = :anio "
            + "AND ((:semestre IS NULL AND r.semestre IS NULL) "
            + "      OR r.semestre = :semestre) "
            + "AND r.periodo = :periodo")
    Optional<ReporteInstitucionalEntity> findByTipoAnioSemestrePeriodo(
            @Param("tipo") pe.gob.midagri.piip.reportes.entity.TipoReporte tipo,
            @Param("anio") Integer anio,
            @Param("semestre") Integer semestre,
            @Param("periodo") String periodo);
}
