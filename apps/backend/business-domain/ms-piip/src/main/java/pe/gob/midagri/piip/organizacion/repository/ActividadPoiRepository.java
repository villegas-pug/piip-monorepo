package pe.gob.midagri.piip.organizacion.repository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.gob.midagri.piip.organizacion.entity.ActividadPoiEntity;
public interface ActividadPoiRepository extends JpaRepository<ActividadPoiEntity, Long> {
 @Query("select a from ActividadPoiEntity a where a.activo='S' and a.vigenteDesde <= :fecha and (a.vigenteHasta is null or a.vigenteHasta >= :fecha) and (lower(a.codigo) like lower(concat('%',:q,'%')) or lower(a.descripcion) like lower(concat('%',:q,'%'))) order by a.codigo")
 List<ActividadPoiEntity> buscarVigentes(@Param("q") String q, @Param("fecha") LocalDate fecha);
}
