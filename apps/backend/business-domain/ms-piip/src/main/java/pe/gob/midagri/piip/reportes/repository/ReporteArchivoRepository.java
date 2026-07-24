package pe.gob.midagri.piip.reportes.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.gob.midagri.piip.reportes.entity.ReporteArchivoEntity;

/**
 * Repositorio de los archivos PDF/XLSX emitidos
 * (DDL 017, tabla REPORTE_ARCHIVO). Cada
 * combinación (reporte, formato, version) es única
 * por la UK
 * {@code UK_RA_REPORTE_FORMATO_VERSION}.
 */
public interface ReporteArchivoRepository
        extends JpaRepository<ReporteArchivoEntity, Long> {

    /**
     * Devuelve la última versión registrada para un
     * reporte y formato concretos; se utiliza para
     * generar la siguiente versión sin duplicar la
     * UK.
     */
    Optional<ReporteArchivoEntity>
            findFirstByIdReporteAndFormatoOrderByVersionDesc(
                    Long idReporte,
                    ReporteArchivoEntity.FormatoArchivoReporte formato);

    /** Lista todos los archivos asociados a un reporte, sin importar formato. */
    List<ReporteArchivoEntity> findByIdReporte(Long idReporte);
}
