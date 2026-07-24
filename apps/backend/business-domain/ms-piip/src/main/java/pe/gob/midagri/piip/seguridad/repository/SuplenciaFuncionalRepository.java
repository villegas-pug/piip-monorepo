package pe.gob.midagri.piip.seguridad.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import pe.gob.midagri.piip.seguridad.entity.SuplenciaFuncionalEntity;

public interface SuplenciaFuncionalRepository extends JpaRepository<SuplenciaFuncionalEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SuplenciaFuncionalEntity s where s.id = :id")
    java.util.Optional<SuplenciaFuncionalEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select count(s) > 0 from SuplenciaFuncionalEntity s
            where s.asignacionTitularId = :asignacionTitularId and s.terminadaEn is null
              and s.inicio <= current_date and s.fin >= current_date
            """)
    boolean existsVigenteParaTitular(@Param("asignacionTitularId") Long asignacionTitularId);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from SuplenciaFuncionalEntity s
            where s.asignacionTitularId = :asignacionTitularId and s.terminadaEn is null
              and s.inicio <= :fin and s.fin >= :inicio
            """)
    List<SuplenciaFuncionalEntity> findSolapadasForUpdate(
            @Param("asignacionTitularId") Long asignacionTitularId,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);
}
