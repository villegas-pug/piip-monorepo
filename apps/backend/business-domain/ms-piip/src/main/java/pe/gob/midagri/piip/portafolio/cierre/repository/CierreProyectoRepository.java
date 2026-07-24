package pe.gob.midagri.piip.portafolio.cierre.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import pe.gob.midagri.piip.portafolio.cierre.entity.CierreProyectoEntity;

/** El índice único UK_CIERRE_PROY es la autoridad ante cierres duplicados. */
public interface CierreProyectoRepository extends JpaRepository<CierreProyectoEntity, Long> {
    boolean existsByIdProyecto(Long idProyecto);
    Optional<CierreProyectoEntity> findByIdProyecto(Long idProyecto);
}
