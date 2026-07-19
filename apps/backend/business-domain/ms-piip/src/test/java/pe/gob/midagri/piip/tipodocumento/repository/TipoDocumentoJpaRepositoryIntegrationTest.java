package pe.gob.midagri.piip.tipodocumento.repository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=none")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
@ActiveProfiles("test")
@Sql(statements = {
        "CREATE TABLE TIPO_DOCUMENTO (ID_TIPO_DOC NUMBER(5) PRIMARY KEY, NOMBRE VARCHAR2(200) NOT NULL, ESTADO_ASOCIADO VARCHAR2(30) NOT NULL, OBLIGATORIO CHAR(1) NOT NULL, DESCRIPCION VARCHAR2(500), ANEXO_NT VARCHAR2(20), ACTIVO CHAR(1) NOT NULL)",
        "INSERT INTO TIPO_DOCUMENTO (ID_TIPO_DOC, NOMBRE, ESTADO_ASOCIADO, OBLIGATORIO, ACTIVO) VALUES (1, 'Ficha de Iniciativa de Innovación Pública', 'PRESENTADO', 'S', 'S')"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = "DROP TABLE TIPO_DOCUMENTO", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class TipoDocumentoJpaRepositoryIntegrationTest {

    @Container
    static final OracleContainer ORACLE = new OracleContainer(
            DockerImageName.parse("gvenzl/oracle-xe:21-slim-faststart"));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", ORACLE::getJdbcUrl);
        registry.add("spring.datasource.username", ORACLE::getUsername);
        registry.add("spring.datasource.password", ORACLE::getPassword);
    }

    @Autowired
    private TipoDocumentoJpaRepository repository;

    @Test
    void findsTheCanonicalCatalogEntry() {
        assertThat(this.repository.findById(1))
                .isPresent()
                .get()
                .extracting("nombre")
                .isEqualTo("Ficha de Iniciativa de Innovación Pública");
    }
}
