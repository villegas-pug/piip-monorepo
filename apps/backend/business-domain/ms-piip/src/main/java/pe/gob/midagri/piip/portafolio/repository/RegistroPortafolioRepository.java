package pe.gob.midagri.piip.portafolio.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import pe.gob.midagri.piip.portafolio.entity.RegistroPortafolioEntity;

public interface RegistroPortafolioRepository extends JpaRepository<RegistroPortafolioEntity, Long> {

    Optional<RegistroPortafolioEntity> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    boolean existsByCodigoOrigen(String codigoOrigen);
    Optional<RegistroPortafolioEntity> findByCodigoOrigen(String codigoOrigen);

    /**
     * Recupera la fila de {@code PROYECTO} aplicando un bloqueo pesimista de
     * escritura ({@code SELECT ... FOR UPDATE}) para serializar las
     * transiciones de estado concurrentes. La Constitucion 5.0.0 exige
     * {@code LockModeType.PESSIMISTIC_WRITE} para resolver la carrera entre
     * confirmaciones; el bloqueo se libera al cerrar la transaccion de
     * negocio.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RegistroPortafolioEntity r WHERE r.id = :id")
    Optional<RegistroPortafolioEntity> findByIdForUpdate(@Param("id") Long id);

    /**
     * Indica si existe un proyecto activo (estado canonico) para la unidad
     * ejecutora y el anio indicados. La Constitucion 5.0.0 exige que solo
     * exista un proyecto directo activo por unidad ejecutora y anio; esta
     * consulta permite al servicio de aplicacion detectar la condicion
     * antes del INSERT y responder con 409
     * {@code DIRECT_PROJECT_NOT_AUTHORIZED} o
     * {@code ACTIVE_PROJECT_EXISTS} canonico.
     *
     * <p>El calculo del anio se realiza con la funcion {@code EXTRACT(YEAR
     * FROM fecha_inicio)} de Oracle sobre la columna
     * {@code FECHA_INICIO} del agregado central {@code PROYECTO}. El estado
     * se compara con el nombre canonico del enum
     * {@link pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa}.
     */
    @Query("SELECT COUNT(r) > 0 FROM RegistroPortafolioEntity r "
            + "WHERE r.unidadEjecutoraId = :unidadId "
            + "AND FUNCTION('YEAR', r.fechaInicio) = :anio "
            + "AND r.estado = :estado")
    boolean existsByUnidadEjecutoraIdAndAnioAndEstado(
            @Param("unidadId") Long unidadId,
            @Param("anio") Integer anio,
            @Param("estado") String estado);
}
