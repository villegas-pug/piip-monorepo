package pe.gob.midagri.piip.auditoria.repository;

import pe.gob.midagri.piip.auditoria.entity.AuditoriaEventoEntity;

public interface AuditoriaEventoRepository {
    AuditoriaEventoEntity append(AuditoriaEventoEntity evento);
}
