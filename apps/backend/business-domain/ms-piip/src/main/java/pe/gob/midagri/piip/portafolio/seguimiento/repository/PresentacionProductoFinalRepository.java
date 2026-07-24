package pe.gob.midagri.piip.portafolio.seguimiento.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import pe.gob.midagri.piip.portafolio.seguimiento.entity.PresentacionProductoFinalEntity;

/**
 * Repositorio JPA para PRESENTACION_PRODUCTO_FINAL (DDL 015,
 * VIGENTE). Registra el evento append-only de presentacion del
 * producto final: cada nueva presentacion crea una fila con
 * VERSION incrementada y referencia a la fila anterior. La
 * presentacion NO realiza una transicion implicita: el estado
 * del proyecto permanece en PROYECTO_EJECUCION hasta que la
 * Autoridad registre la decision formal.
 */
@Repository
public interface PresentacionProductoFinalRepository
        extends JpaRepository<PresentacionProductoFinalEntity, Long> {

    /**
     * Devuelve la version mas reciente de la presentacion para
     * un proyecto. La fila devuelta tiene VERSION igual a la
     * maxima version registrada del proyecto o vacia si no
     * existe ninguna presentacion.
     */
    Optional<PresentacionProductoFinalEntity> findFirstByIdProyectoOrderByVersionDesc(
            Long idProyecto);

    /**
     * Lista todas las versiones de la presentacion del producto
     * final del proyecto en orden ascendente (append-only).
     */
    List<PresentacionProductoFinalEntity> findByIdProyectoOrderByVersionAsc(Long idProyecto);
}
