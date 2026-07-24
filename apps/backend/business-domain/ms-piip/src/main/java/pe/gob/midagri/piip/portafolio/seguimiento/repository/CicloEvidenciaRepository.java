package pe.gob.midagri.piip.portafolio.seguimiento.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.gob.midagri.piip.portafolio.seguimiento.entity.CicloEvidenciaEntity;

/**
 * Repositorio JPA para {@code CICLO_EVIDENCIA} (DDL 015, VIGENTE).
 * La UK {@code UK_CE_CICLO_DOC} garantiza unicidad por ciclo y
 * documento; las altas duplicadas se traducen a
 * {@code EVIDENCE_ALREADY_ATTACHED} en la capa de servicio.
 */
@Repository
public interface CicloEvidenciaRepository
        extends JpaRepository<CicloEvidenciaEntity, Long> {

    /**
     * Lista las evidencias asociadas a un ciclo. El orden lo
     * define la base de datos (PK y FECHA_CREACION).
     */
    List<CicloEvidenciaEntity> findByIdCiclo(Long idCiclo);

    /**
     * Indica si ya existe una evidencia que vincule el documento
     * al ciclo. La consulta respeta la UK canonica.
     */
    boolean existsByIdCicloAndIdDocumento(Long idCiclo, Long idDocumento);
}
