package pe.gob.midagri.piip.organizacion.repository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.gob.midagri.piip.organizacion.entity.ObjetivoPeiEntity;
public interface ObjetivoPeiRepository extends JpaRepository<ObjetivoPeiEntity, Long> {
 @Query("select o from ObjetivoPeiEntity o where o.activo='S' and o.vigenteDesde <= :fecha and (o.vigenteHasta is null or o.vigenteHasta >= :fecha) and (lower(o.codigo) like lower(concat('%',:q,'%')) or lower(o.descripcion) like lower(concat('%',:q,'%'))) order by o.codigo")
 List<ObjetivoPeiEntity> buscarVigentes(@Param("q") String q, @Param("fecha") LocalDate fecha);
}
