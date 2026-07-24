package pe.gob.midagri.piip.seguridad.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import pe.gob.midagri.piip.seguridad.entity.UsuarioRolUnidadEntity;

public interface UsuarioRolUnidadRepository extends JpaRepository<UsuarioRolUnidadEntity, Long> {
    List<UsuarioRolUnidadEntity> findByUsuarioIdOrderByFechaInicioDesc(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from UsuarioRolUnidadEntity a, UsuarioEntity u, MatrizFuncionPerfilUnidadEntity c,
                 MatrizFuncionalVersionEntity v
            where a.id = :asignacionId and a.usuarioId = u.id and u.keycloakId = :keycloakId
              and a.rolId = :rolId and a.unidadId = :unidadId
              and a.combinacionMatrizId = c.id and c.versionId = v.id
              and c.rolId = :rolId and c.unidadId = :unidadId
              and c.activa = 'S' and c.vigenteDesde <= current_date
              and (c.vigenteHasta is null or c.vigenteHasta >= current_date)
              and not exists (select siguiente from MatrizFuncionalVersionEntity siguiente
                              where siguiente.versionAnteriorId = v.id
                                and siguiente.vigenteDesde <= current_date)
              and u.activo = 'S' and a.activo = 'S' and a.revocadaEn is null
               and a.fechaInicio <= current_date
               and (a.fechaFin is null or a.fechaFin >= current_date)
               and not exists (select s from SuplenciaFuncionalEntity s
                               where s.asignacionTitularId = a.id and s.terminadaEn is null
                                 and s.inicio <= current_date and s.fin >= current_date)
            """)
    Optional<UsuarioRolUnidadEntity> findAsignacionEfectivaForUpdate(
            @Param("asignacionId") Long asignacionId,
            @Param("keycloakId") String keycloakId,
            @Param("rolId") Integer rolId,
            @Param("unidadId") Long unidadId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from UsuarioRolUnidadEntity a, UsuarioEntity u, MatrizFuncionPerfilUnidadEntity c,
                 MatrizFuncionalVersionEntity v
            where a.id = :asignacionId and a.usuarioId = u.id and u.keycloakId = :keycloakId
              and a.unidadId = :unidadId and a.combinacionMatrizId = c.id and c.versionId = v.id
              and c.unidadId = :unidadId
              and c.activa = 'S' and c.vigenteDesde <= current_date
              and (c.vigenteHasta is null or c.vigenteHasta >= current_date)
              and not exists (select siguiente from MatrizFuncionalVersionEntity siguiente
                              where siguiente.versionAnteriorId = v.id
                                and siguiente.vigenteDesde <= current_date)
              and u.activo = 'S' and a.activo = 'S' and a.revocadaEn is null
               and a.fechaInicio <= current_date
               and (a.fechaFin is null or a.fechaFin >= current_date)
               and not exists (select s from SuplenciaFuncionalEntity s
                               where s.asignacionTitularId = a.id and s.terminadaEn is null
                                 and s.inicio <= current_date and s.fin >= current_date)
            """)
    Optional<UsuarioRolUnidadEntity> findAsignacionInstitucionalForUpdate(
            @Param("asignacionId") Long asignacionId, @Param("keycloakId") String keycloakId,
            @Param("unidadId") Long unidadId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from UsuarioRolUnidadEntity a where a.id = :id")
    Optional<UsuarioRolUnidadEntity> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select count(a) > 0 from UsuarioRolUnidadEntity a, MatrizFuncionPerfilUnidadEntity c,
                 MatrizFuncionalVersionEntity v
            where a.usuarioId = :usuarioId and a.rolId = :rolId and a.unidadId = :unidadId
              and a.combinacionMatrizId = c.id and c.versionId = v.id
              and c.rolId = :rolId and c.unidadId = :unidadId
              and c.activa = 'S' and c.vigenteDesde <= current_date
              and (c.vigenteHasta is null or c.vigenteHasta >= current_date)
              and not exists (select siguiente from MatrizFuncionalVersionEntity siguiente
                              where siguiente.versionAnteriorId = v.id
                                and siguiente.vigenteDesde <= current_date)
               and a.activo = 'S' and a.revocadaEn is null
               and a.fechaInicio <= current_date and (a.fechaFin is null or a.fechaFin >= current_date)
               and not exists (select s from SuplenciaFuncionalEntity s
                               where s.asignacionTitularId = a.id and s.terminadaEn is null
                                 and s.inicio <= current_date and s.fin >= current_date)
            """)
    boolean existsAsignacionVigente(@Param("usuarioId") Long usuarioId, @Param("rolId") Integer rolId,
            @Param("unidadId") Long unidadId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select a from UsuarioRolUnidadEntity a, RolEntity r
            where a.rolId = r.id and r.nombre = 'GlobalAdmin' and a.activo = 'S'
               and a.revocadaEn is null
               and a.fechaInicio <= current_date and (a.fechaFin is null or a.fechaFin >= current_date)
               and not exists (select s from SuplenciaFuncionalEntity s
                               where s.asignacionTitularId = a.id and s.terminadaEn is null
                                 and s.inicio <= current_date and s.fin >= current_date)
            """)
    List<UsuarioRolUnidadEntity> findGlobalAdminsEfectivosForUpdate();

    @Query("select count(a) > 0 from UsuarioRolUnidadEntity a, RolEntity r where a.rolId = r.id and r.nombre = 'GlobalAdmin'")
    boolean existsGlobalAdminHistorico();
}
