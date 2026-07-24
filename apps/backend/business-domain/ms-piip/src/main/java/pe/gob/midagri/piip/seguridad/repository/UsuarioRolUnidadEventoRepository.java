package pe.gob.midagri.piip.seguridad.repository;

import java.util.List;
import pe.gob.midagri.piip.seguridad.entity.UsuarioRolUnidadEventoEntity;

public interface UsuarioRolUnidadEventoRepository {
    UsuarioRolUnidadEventoEntity append(UsuarioRolUnidadEventoEntity evento);

    List<UsuarioRolUnidadEventoEntity> findByAsignacionId(Long asignacionId);
}
