package pe.gob.midagri.piip.seguridad.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.gob.midagri.piip.seguridad.entity.MatrizFuncionPerfilUnidadEntity;

public interface MatrizFuncionPerfilUnidadRepository extends JpaRepository<MatrizFuncionPerfilUnidadEntity, Long> {
    List<MatrizFuncionPerfilUnidadEntity> findByVersionIdOrderByIdAsc(Long versionId);

    @Query("""
            select c from MatrizFuncionPerfilUnidadEntity c, MatrizFuncionalVersionEntity v
            where c.id = :combinacionId and c.versionId = v.id
              and c.rolId = :rolId and c.unidadId = :unidadId
              and c.activa = 'S' and c.vigenteDesde <= :fecha
              and (c.vigenteHasta is null or c.vigenteHasta >= :fecha)
              and not exists (select siguiente from MatrizFuncionalVersionEntity siguiente
                              where siguiente.versionAnteriorId = v.id
                                and siguiente.vigenteDesde <= current_date)
            """)
    Optional<MatrizFuncionPerfilUnidadEntity> findVigenteParaAsignacion(
            @Param("combinacionId") Long combinacionId,
            @Param("rolId") Integer rolId,
            @Param("unidadId") Long unidadId,
            @Param("fecha") LocalDate fecha);
}
