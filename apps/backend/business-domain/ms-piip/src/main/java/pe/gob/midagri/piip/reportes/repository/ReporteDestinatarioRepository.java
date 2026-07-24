package pe.gob.midagri.piip.reportes.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pe.gob.midagri.piip.reportes.entity.ReporteDestinatarioEntity;

/**
 * Repositorio de destinatarios aprobados de un
 * reporte (DDL 017, tabla REPORTE_DESTINATARIO).
 * La UK
 * {@code UK_RD_APROBACION_TIPO_ENTIDAD} impide
 * duplicados; la remisión solo se permite contra
 * destinatarios previamente aprobados.
 */
public interface ReporteDestinatarioRepository
        extends JpaRepository<ReporteDestinatarioEntity, Long> {

    /** Lista los destinatarios aprobados dentro de una aprobación. */
    List<ReporteDestinatarioEntity> findByIdAprobacion(Long idAprobacion);
}
