package pe.gob.midagri.piip.documentos;

import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Base preparada para validar BLOB, SHA-256, XOR y expedientes en Oracle; se ejecuta solo con el perfil integration-tests autorizado. */
@Testcontainers(disabledWithoutDocker = true)
class DocumentosOracleIT {
    @Container
    static final OracleContainer ORACLE = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart");
}
