package pe.gob.midagri.piip.portafolio.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.gob.midagri.piip.portafolio.entity.TransicionEstadoEntity;

public interface TransicionEstadoRepository extends JpaRepository<TransicionEstadoEntity, Long> {
}
