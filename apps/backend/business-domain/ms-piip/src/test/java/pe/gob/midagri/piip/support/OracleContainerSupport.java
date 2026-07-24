package pe.gob.midagri.piip.support;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.containers.startupcheck.MinimumDurationRunningStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Soporte Oracle Testcontainers para suites de integración PIIP.
 *
 * Ejecuta los DDLs incrementales (001-027) en orden de dependencia antes de
 * cada suite, validando que Docker esté disponible y evitando omisiones
 * silenciosas de infraestructura.
 *
 * Uso en clases de prueba:
 * <pre>
 * {@literal @}SpringBootTest
 * class MiIntegracionTest extends OracleContainerSupport {
 *     // Hereda container y properties dinámicos
 * }
 * </pre>
 *
 * Orden de ejecución DDL según dependencias:
 * <ol>
 *   <li>001_baseline_piip.sql (schema base, catálogos canonicos)</li>
 *   <li>002_auditoria_idempotencia.sql</li>
 *   <li>003_expediente_serie_version.sql</li>
 *   <li>003.1_tipo_documento_contexto_nullable.sql</li>
 *   <li>003.2_documento_propietario_institucional.sql</li>
 *   <li>004_documento_publicacion.sql</li>
 *   <li>005_objetivo_pei_versionado.sql</li>
 *   <li>005.1_objetivo_pei_versionado_indice.sql</li>
 *   <li>006_actividad_poi_versionada.sql</li>
 *   <li>007_matriz_funcional_versionada.sql</li>
 *   <li>008_usuario_rol_unidad_vigencia.sql</li>
 *   <li>008.1_secuencias_vigencia.sql</li>
 *   <li>009_proyecto_campos_oficiales.sql</li>
 *   <li>010_iniciativa_proyecto_relacion.sql</li>
 *   <li>011_proyecto_unidades_responsables.sql</li>
 *   <li>012_responsables_participantes.sql</li>
 *   <li>013_clasificacion_campos.sql</li>
 *   <li>014_evaluacion_transiciones.sql</li>
 *   <li>014.1_subsanacion_iniciativa_plazo.sql</li>
 *   <li>015_ciclos_resultados_cierre.sql</li>
 *   <li>016_incorporacion_individual.sql</li>
 *   <li>017_reporte_expediente_remision.sql</li>
 *   <li>025_ciclo_presentacion_evidencia_version.sql</li>
 *   <li>026_incorporacion_registro_observacion_version.sql</li>
 *   <li>027_operacion_aprovisionamiento_unidad_objetivo.sql</li>
 * </ol>
 */
public abstract class OracleContainerSupport {

    /** Imagen Oracle XE 21c compatible con Testcontainers. */
    private static final String ORACLE_XE_IMAGE = "gvenzl/oracle-xe:21-slim";

    /** Timeout para startup del contenedor. */
    private static final Duration ORACLE_STARTUP_TIMEOUT = Duration.ofMinutes(5);

    /** Usuario SYS del contenedor Oracle XE. */
    private static final String ORACLE_SYS_USER = "sys";

    /** Rol SYSDBA requerido para DDL. */
    private static final String ORACLE_SYS_ROLE = "AS SYSDBA";

    /** Usuario de aplicación (creado en el DDL 001). */
    private static final String ORACLE_APP_USER = "KALLPA_PIIP";

    /** Contraseña del usuario de aplicación. */
    private static final String ORACLE_APP_PASSWORD = "piip_test";

    /**
     * Indica si la sesión actual de DDL ya fue ejecutada para evitar
     * re-ejecución por suite (soporte para múltiples {@code @BeforeAll}
     * en herencia).
     */
    private static boolean schemaInitialized = false;

    /**
     * Contenedor Oracle XE compartido entre todos los tests de integración.
     * Se reutilizaAcross test classes que extienden esta clase.
     */
    private static OracleContainer oracleContainer;

    /**
     * Valida que Docker esté disponible en el sistema antes de iniciar
     * cualquier test de integración. Si Docker no está disponible,
     * la ejecución falla con un mensaje claro en lugar de ser omitida
     * silenciosamente.
     *
     * @throws IllegalStateException si Docker no está disponible
     */
    private static void validarDockerDisponible() {
        String dockerAvailable = System.getenv("DOCKER_AVAILABLE");
        if ("false".equalsIgnoreCase(dockerAvailable)) {
            throw new IllegalStateException(
                "Docker no está disponible en el sistema. " +
                "Establezca DOCKER_AVAILABLE=true para habilitar pruebas de integración con Oracle Testcontainers. " +
                "Verifique que Docker Desktop esté ejecutándose o que el demonio Docker esté accesible."
            );
        }

        // Testcontainers valida internamente Docker; la conexión se prueba
        // al iniciar el contenedor para evitar omisiones silenciosas.
        try {
            var factory = org.testcontainers.DockerClientFactory.instance();
            factory.client().infoCmd().exec();
        } catch (Exception ex) {
            throw new IllegalStateException(
                "No se pudo conectar al demonio Docker. Asegúrese de que Docker esté en ejecución. " +
                "Detalles: " + ex.getMessage(), ex
            );
        }
    }

    /**
     * Inicializa el contenedor Oracle XE una sola vez por JVM.
     * Registra las propiedades dinámicas de Spring DataSource.
     *
     * @param registry registro de propiedades dinámicas de Spring Test
     */
    @DynamicPropertySource
    static void registrarProperties(DynamicPropertyRegistry registry) {
        validarDockerDisponible();
        iniciarContenedorSiNecesario();
        registrarDataSourceProperties(registry);
    }

    /**
     * Asegura que el contenedor Oracle XE esté corriendo.
     * Si ya está corriendo, no hace nada (soporte a múltiples clases de test).
     */
    private static synchronized void iniciarContenedorSiNecesario() {
        if (oracleContainer != null && oracleContainer.isRunning()) {
            return;
        }

        oracleContainer = new OracleContainer(DockerImageName.parse(ORACLE_XE_IMAGE))
            .withStartupCheckStrategy(new MinimumDurationRunningStartupCheckStrategy(ORACLE_STARTUP_TIMEOUT))
            .withStartupTimeout(ORACLE_STARTUP_TIMEOUT)
            .withTmpFs(java.util.Map.of("/dev/shm", "2g"));

        oracleContainer.start();

        ejecutarSchemaInicial();
    }

    /**
     * Registra las propiedades de DataSource para Spring.
     *
     * @param registry registro de propiedades
     */
    private static void registrarDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", oracleContainer::getJdbcUrl);
        registry.add("spring.datasource.username", () -> ORACLE_APP_USER);
        registry.add("spring.datasource.password", () -> ORACLE_APP_PASSWORD);
        registry.add("spring.datasource.driver-class-name", () -> "oracle.jdbc.OracleDriver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "none");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.OracleDialect");
    }

    /**
     * Ejecuta los DDLs incrementales en orden de dependencia.
     * Se ejecuta una sola vez por JVM.
     */
    private static synchronized void ejecutarSchemaInicial() {
        if (schemaInitialized) {
            return;
        }

        String jdbcUrl = oracleContainer.getJdbcUrl();
        String sysUser = ORACLE_SYS_USER;
        String sysPassword = oracleContainer.getPassword();

        var properties = new java.util.Properties();
        properties.setProperty("user", sysUser);
        properties.setProperty("password", sysPassword);

        try (var connection = java.sql.DriverManager.getConnection(jdbcUrl, properties);
             var statement = connection.createStatement()) {

            connection.setAutoCommit(true);

            ejecutarInitSchema(statement, connection);
            ejecutarDDLsEnOrden(statement, connection);

            schemaInitialized = true;

        } catch (java.sql.SQLException ex) {
            throw new IllegalStateException(
                "Error al ejecutar DDLs de esquema en Oracle Testcontainers: " + ex.getMessage(), ex
            );
        }
    }

    /**
     * Ejecuta el DDL inicial 001_baseline_piip.sql conectado como KALLPA_PIIP.
     * Este script crea el usuario, las tablas base, secuencias, constraints,
     * índices y datos semilla canonicos.
     *
     * @param statement statement SQL
     * @param connection conexión activa
     * @throws java.sql.SQLException si falla la ejecución
     */
    private static void ejecutarInitSchema(java.sql.Statement statement,
                                           java.sql.Connection connection)
        throws java.sql.SQLException {

        ejecutarScript(statement, "database/ddl/init/001_baseline_piip.sql");
    }

    /**
     * Ejecuta los DDLs incrementales en orden de número de secuencia.
     * Cada script depende del anterior; se ejecutan en una transacción
     * separada por script para facilitar el diagnóstico de errores.
     *
     * @param statement statement SQL
     * @param connection conexión activa
     * @throws java.sql.SQLException si falla la ejecución
     */
    private static void ejecutarDDLsEnOrden(java.sql.Statement statement,
                                           java.sql.Connection connection)
        throws java.sql.SQLException {

        var ddlFiles = obtenerOrdenDDLs();

        for (String ddlFile : ddlFiles) {
            System.out.println("[OracleContainerSupport] Ejecutando DDL: " + ddlFile);

            connection.setAutoCommit(false);
            try {
                ejecutarScript(statement, ddlFile);
                connection.commit();
            } catch (java.sql.SQLException ex) {
                connection.rollback();
                throw new IllegalStateException(
                    "Error al ejecutar DDL incremental: " + ddlFile + ". " +
                    "Detalles: " + ex.getMessage(), ex
                );
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    /**
     * Retorna la lista ordenada de DDLs incrementales según dependencia.
     * El orden respeta las referencias cruzadas entre módulos:
     * <ul>
     *   <li>002 (auditoria) después de 001</li>
     *   <li>003, 003.1, 003.2, 004 (documentos) después de 001</li>
     *   <li>005, 005.1, 006 (organización) después de 001</li>
     *   <li>007 (seguridad) después de 001</li>
     *   <li>008, 008.1 (vigencia) después de 007</li>
     *   <li>009-016 (portafolio) después de 001, 005, 007</li>
     *   <li>017 (reportes) después de 001, 003, 009</li>
     *   <li>025, 026 (portafolio extendido) después de 001, 015</li>
     *   <li>027 (aprovisionamiento) después de 001, 005, 007, 008</li>
     * </ul>
     *
     * @return lista ordenada de rutas de DDLs
     */
    private static java.util.List<String> obtenerOrdenDDLs() {
        var ordered = new java.util.ArrayList<String>();

        // Auditoria
        ordered.add("database/ddl/auditoria/002_auditoria_idempotencia.sql");

        // Documentos (después de 001)
        ordered.add("database/ddl/documentos/003_expediente_serie_version.sql");
        ordered.add("database/ddl/documentos/003.1_tipo_documento_contexto_nullable.sql");
        ordered.add("database/ddl/documentos/003.2_documento_propietario_institucional.sql");
        ordered.add("database/ddl/documentos/004_documento_publicacion.sql");

        // Organización (después de 001)
        ordered.add("database/ddl/organizacion/005_objetivo_pei_versionado.sql");
        ordered.add("database/ddl/organizacion/005.1_objetivo_pei_versionado_indice.sql");
        ordered.add("database/ddl/organizacion/006_actividad_poi_versionada.sql");

        // Seguridad (después de 001)
        ordered.add("database/ddl/seguridad/007_matriz_funcional_versionada.sql");
        ordered.add("database/ddl/seguridad/008_usuario_rol_unidad_vigencia.sql");
        ordered.add("database/ddl/seguridad/008.1_secuencias_vigencia.sql");

        // Portafolio (después de 001, 005, 007, 008)
        ordered.add("database/ddl/portafolio/009_proyecto_campos_oficiales.sql");
        ordered.add("database/ddl/portafolio/010_iniciativa_proyecto_relacion.sql");
        ordered.add("database/ddl/portafolio/011_proyecto_unidades_responsables.sql");
        ordered.add("database/ddl/portafolio/012_responsables_participantes.sql");
        ordered.add("database/ddl/portafolio/013_clasificacion_campos.sql");
        ordered.add("database/ddl/portafolio/014_evaluacion_transiciones.sql");
        ordered.add("database/ddl/portafolio/014.1_subsanacion_iniciativa_plazo.sql");
        ordered.add("database/ddl/portafolio/015_ciclos_resultados_cierre.sql");
        ordered.add("database/ddl/portafolio/016_incorporacion_individual.sql");

        // Reportes (después de 001, 003, 009)
        ordered.add("database/ddl/reportes/017_reporte_expediente_remision.sql");

        // Portafolio extendido (después de 001, 015)
        ordered.add("database/ddl/portafolio/025_ciclo_presentacion_evidencia_version.sql");
        ordered.add("database/ddl/portafolio/026_incorporacion_registro_observacion_version.sql");

        // Seguridad avanzada / aprovisionamiento (después de 001, 005, 007, 008)
        ordered.add("database/ddl/seguridad/027_operacion_aprovisionamiento_unidad_objetivo.sql");

        return ordered;
    }

    /**
     * Ejecuta un script SQL desde el classpath.
     * Maneja bloques PL/SQL (delimitados por {@code /}) y comandos SQLPlus
     * como {@code PROMPT} y {@code WHENEVER}.
     *
     * @param statement statement SQL en ejecución
     * @param classpathResource ruta del recurso en classpath
     * @throws java.sql.SQLException si falla la ejecución
     */
    private static void ejecutarScript(java.sql.Statement statement, String classpathResource)
        throws java.sql.SQLException {

        try (InputStream is = OracleContainerSupport.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (is == null) {
                throw new IllegalStateException(
                    "Recurso DDL no encontrado en classpath: " + classpathResource
                );
            }

            String content = new BufferedReader(
                new InputStreamReader(is, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

            // Remover comentarios SQL Plus estilo PROMPT
            content = content.replaceAll("(?m)^PROMPT.*$", "");
            // RemoverWHENEVER SQLERROR que interfiere con JDBC
            content = content.replaceAll("(?i)^WHENEVER\\s+SQLERROR.*$", "");
            // Normalizar múltiples saltos de línea
            content = content.replaceAll("(?m)^\\s*$", "");

            // Dividir por bloques PL/SQL delimitados por /
            String[] blocks = content.split("\\n(?=/)");

            for (String block : blocks) {
                block = block.trim();
                if (block.isEmpty()) {
                    continue;
                }

                // Remover el / final de bloque si existe
                block = block.replaceAll("/\\s*$", "").trim();
                if (block.isEmpty()) {
                    continue;
                }

                // Ignorar bloques de declaración PL/SQL vacíos
                if (block.startsWith("DECLARE") && !block.contains("BEGIN")) {
                    continue;
                }

                try {
                    statement.execute(block);
                } catch (java.sql.SQLException ex) {
                    // Oracle XE puede devolver XEZ-0956 o errores de objetos existentes
                    // en scripts idempotentes; solo fallamos si es un error real de sintaxis
                    // o de restricción que impide la operación.
                    String sqlState = ex.getSQLState();
                    int errorCode = ex.getErrorCode();

                    if (isIdempotentError(ex)) {
                        System.out.println(
                            "[OracleContainerSupport] Ignorando error idempotente en bloque: " +
                            truncateForDisplay(block, 100)
                        );
                        continue;
                    }

                    throw ex;
                }
            }

        } catch (IOException ex) {
            throw new IllegalStateException(
                "Error al leer recurso DDL: " + classpathResource, ex
            );
        }
    }

    /**
     * Determina si un error SQL es esperado en contexto idempotente
     * (ej: objeto ya existe, constraint ya existe).
     *
     * @param ex excepción SQL
     * @return true si es error idempotente conocido
     */
    private static boolean isIdempotentError(java.sql.SQLException ex) {
        int code = ex.getErrorCode();
        String message = ex.getMessage() != null ? ex.getMessage().toUpperCase() : "";

        //ORA-00955: nombre de objeto ya utilizado (CREATE TABLE/INDEX idempotente)
        //ORA-01430: columna ya existe en la tabla (ALTER TABLE ADD COLUMN)
        //ORA-02275: constraint de referencia ya existe
        //ORA-01451: columna a modificar ya es NULL (ALTER COLUMN idempotente)
        //ORA-00942: tabla o vista no existe (para algunos DROP que queremos ignorar)
        return code == 955 || code == 1430 || code == 2275 ||
               code == 1451 || code == 942 ||
               message.contains("OBJECT ALREADY EXISTS") ||
               message.contains("ALREADY EXISTS") ||
               message.contains("DUPLICATE");
    }

    /**
     * Trunca una cadena para display en logs.
     *
     * @param s cadena a truncar
     * @param maxLen longitud máxima
     * @return cadena truncada
     */
    private static String truncateForDisplay(String s, int maxLen) {
        if (s == null) return "null";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...";
    }

    /**
     * Detiene el contenedor Oracle al final de la JVM.
     * No es necesario invocarlo manualmente; elshutdown hook lo hace.
     */
    public static synchronized void detenerContenedor() {
        if (oracleContainer != null) {
            try {
                if (oracleContainer.isRunning()) {
                    oracleContainer.stop();
                }
            } catch (Exception ex) {
                System.err.println(
                    "[OracleContainerSupport] Error al detener contenedor: " + ex.getMessage()
                );
            } finally {
                oracleContainer = null;
                schemaInitialized = false;
            }
        }
    }

    /**
     * Hook de shutdown para detener el contenedor al finalizar la JVM.
     */
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(OracleContainerSupport::detenerContenedor));
    }
}
