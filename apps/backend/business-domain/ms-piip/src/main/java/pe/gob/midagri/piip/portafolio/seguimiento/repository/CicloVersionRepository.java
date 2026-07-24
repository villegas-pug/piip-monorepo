package pe.gob.midagri.piip.portafolio.seguimiento.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.gob.midagri.piip.portafolio.seguimiento.entity.CicloVersionEntity;

/**
 * Repositorio JPA para la cadena append-only de versiones de un
 * ciclo ({@code CICLO_PROYECTO_VERSION}, contrato logico de US4
 * pendiente de DDL canonico). Permite guardar una nueva version
 * sin afectar a las anteriores, consultar la ultima version
 * persistida y listar el historial completo de un ciclo.
 */
@Repository
public interface CicloVersionRepository extends JpaRepository<CicloVersionEntity, Long> {

    /**
     * Recupera la ultima version persistida para el ciclo
     * indicado, ordenando por {@code NUMERO_VERSION} descendente.
     */
    Optional<CicloVersionEntity> findFirstByIdCicloOrderByNumeroVersionDesc(Long idCiclo);

    /**
     * Lista todas las versiones de un ciclo en orden ascendente
     * por {@code NUMERO_VERSION}. La fila con version 1 es la
     * apertura original del ciclo.
     */
    List<CicloVersionEntity> findByIdCicloOrderByNumeroVersionAsc(Long idCiclo);
}
