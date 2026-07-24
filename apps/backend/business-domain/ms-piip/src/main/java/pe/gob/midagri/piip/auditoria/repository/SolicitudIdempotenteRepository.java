package pe.gob.midagri.piip.auditoria.repository;

import java.util.Optional;

import pe.gob.midagri.piip.auditoria.entity.SolicitudIdempotenteEntity;

public interface SolicitudIdempotenteRepository {
    SolicitudIdempotenteEntity createAndFlush(SolicitudIdempotenteEntity solicitud);

    Optional<SolicitudIdempotenteEntity> findByConsumidorOperacionClaveForUpdate(
            String consumidor, String operacion, String clave);
}
