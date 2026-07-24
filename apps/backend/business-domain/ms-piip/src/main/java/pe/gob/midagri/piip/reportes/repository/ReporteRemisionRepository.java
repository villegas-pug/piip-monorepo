package pe.gob.midagri.piip.reportes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.gob.midagri.piip.reportes.entity.ReporteRemisionEntity;

/**
 * Repositorio de remisiones registradas
 * (DDL 017, tabla REPORTE_REMISION). La UK
 * {@code UK_RREM_REPORTE_DESTINATARIO_FECHA}
 * garantiza idempotencia natural: un mismo
 * (reporte, destinatario, fecha) no se duplica.
 * La remisión es manual y recuperable: una
 * reejecución con resultado FALLIDA se permite,
 * pero la concurrencia se bloquea por la UK
 * y por la CHECK
 * {@code CK_RREM_RESULTADO}.
 */
public interface ReporteRemisionRepository
        extends JpaRepository<ReporteRemisionEntity, Long> {

    /** Historial de remisiones de un reporte, ordenado desc por fecha. */
    List<ReporteRemisionEntity> findByIdReporteOrderByFechaRemisionDesc(
            Long idReporte);

    /** Historial de remisiones hacia un destinatario. */
    List<ReporteRemisionEntity> findByIdDestinatarioOrderByFechaRemisionDesc(
            Long idDestinatario);
}
