package pe.gob.midagri.piip.reportes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.gob.midagri.piip.reportes.entity.ReporteAprobacionEntity;

/**
 * Repositorio de aprobaciones formales de un
 * reporte (DDL 017, tabla REPORTE_APROBACION). La
 * UK {@code UK_RAP_REPORTE_VERSION} garantiza
 * una sola aprobación por versión; un reintento
 * devuelve 409 REPORT_VERSION_NOT_APPROVED si
 * la versión ya tiene aprobación registrada o si
 * se intenta remitir sin aprobación previa.
 */
public interface ReporteAprobacionRepository
        extends JpaRepository<ReporteAprobacionEntity, Long> {

    /**
     * Devuelve la aprobación de la versión exacta
     * solicitada. Se utiliza en la remisión para
     * validar que la versión remitida coincide
     * con la aprobada por la Oficina de
     * Modernización.
     */
    Optional<ReporteAprobacionEntity> findByIdReporteAndIdVersion(
            Long idReporte, Integer idVersion);

    /** Historial completo de aprobaciones de un reporte. */
    List<ReporteAprobacionEntity> findByIdReporteOrderByIdVersionDesc(
            Long idReporte);
}
