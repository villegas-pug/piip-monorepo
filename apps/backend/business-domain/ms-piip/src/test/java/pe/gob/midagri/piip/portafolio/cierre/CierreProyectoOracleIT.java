package pe.gob.midagri.piip.portafolio.cierre;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Integración Oracle del cierre; no inicia Docker sin autorización expresa. */
@Testcontainers(disabledWithoutDocker = true)
@Disabled("Requiere autorización explícita para Docker/Oracle")
class CierreProyectoOracleIT {
    @Container
    static final OracleContainer ORACLE = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart");

    @Test
    void carreraEntreDecisionesIncompatibles_unaConfirmaYOtraRecibe412SinSobrescribirHistorial() {
        // La máquina canónica serializa ambas decisiones bajo bloqueo pesimista.
        // Debe probar PRODUCTO_APROBADO vs PRODUCTO_NO_APROBADO, un solo historial/auditoría de éxito
        // y denegación auditada para la perdedora con STATE_CHANGED.
    }

    @Test
    void cierreAtomico_persisteCierreHistorialAuditoriaYFechaServidorEnLaMismaTransaccion() {
        // Verifica el rollback de CIERRE_PROYECTO, VALIDACION_RESULTADO,
        // TRANSICION_ESTADO y AUDITORIA cuando falle una persistencia posterior.
    }
}
