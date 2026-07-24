package pe.gob.midagri.piip.portafolio.cierre.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.portafolio.cierre.entity.ValidacionResultadoEntity;

/** Persistencia JPA de la validación única de resultados por proyecto. */
public interface ValidacionResultadoRepository extends JpaRepository<ValidacionResultadoEntity, Long> {
}
