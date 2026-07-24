package pe.gob.midagri.piip.auditoria.repository;

import pe.gob.midagri.piip.auditoria.entity.AuditoriaAccesoEntity;

public interface AuditoriaAccesoRepository {
    AuditoriaAccesoEntity append(AuditoriaAccesoEntity acceso);
}
