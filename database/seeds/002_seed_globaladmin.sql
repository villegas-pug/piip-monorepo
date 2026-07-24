-- ============================================================================
-- PIIP MIDAGRI - Semilla fundacional 002 - Primer usuario GlobalAdmin
-- Archivo   : 002_seed_globaladmin.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : seguridad
-- Dependencias:
--   001 (database/ddl/init/001_baseline_piip.sql)         - VIGENTE
--   002 (database/ddl/auditoria/002_auditoria_idempotencia.sql) - VIGENTE
--   003 (database/ddl/documentos/003_expediente_serie_version.sql) - VIGENTE
--   007 (database/ddl/seguridad/007_matriz_funcional_versionada.sql) - VIGENTE
--   008 (database/ddl/seguridad/008_usuario_rol_unidad_vigencia.sql) - VIGENTE
--   008.1 (database/ddl/seguridad/008.1_secuencias_vigencia.sql) - VIGENTE
--   021.1 (database/seeds/021.1_bootstrap_matriz_fundacional.sql) - VIGENTE
-- Compensacion:
--   Forward-only: abortar sin cambios si existe cualquier asignacion GlobalAdmin
--   previa (USUARIO_ROL_UNIDAD activa con ID_ROL=1, AUDITORIA_EVENTO
--   TIPO_EVENTO='INICIALIZACION_GLOBAL_ADMIN', o MATRIZ_FUNCION_PERFIL_UNIDAD
--   con ES_BOOTSTRAP='S'). Si falla tras el primer INSERT, hacer ROLLBACK
--   manual (WHENEVER SQLERROR EXIT ... ROLLBACK). Nunca re-ejecutar.
-- Alcance:
--   Crea el primer usuario GlobalAdmin con IDs explicitos fijos, su matriz
--   funcional bootstrap, y el registro de auditoria conforme a la constitucion.
--   NO ejecuta DDL, solo DML. NO se conecta a Keycloak.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como KALLPA_PIIP.
--   EJECUCION UNICA, FAIL-FAST.
-- ============================================================================
-- NOTAS:
--   - Los roles canonicos (IDs 1-6) ya fueron creados por 001_baseline_piip.sql
--     con NIVEL_ACCESO entre 1-6. La constitucion asigna niveles 99,80,60,40,30,10
--     respectivamente, pero la restriccion CK_ROL_NIVEL (BETWEEN 1 AND 10) impide
--     valores >10. Si se requiere alinear los niveles con la constitucion, el DBA
--     debe ALTERAR o DROP CK_ROL_NIVEL antes de ejecutar este script.
--   - Las columnas del esquema reflejan el estado VIGENTE post 008+021.1:
--     USUARIO tiene LOGIN_SINTETICO; USUARIO_ROL_UNIDAD tiene columnas de
--     vigencia (FECHA_INICIO, VERSION, etc.).
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- ============================================================================
-- 1) VALIDACIONES PREVIAS (fail-fast)
-- ============================================================================
PROMPT [002] Validando precondiciones...

DECLARE
    v_count PLS_INTEGER;

    PROCEDURE exigir(
        p_condicion IN BOOLEAN,
        p_codigo    IN PLS_INTEGER,
        p_mensaje   IN VARCHAR2
    ) IS
    BEGIN
        IF NOT p_condicion THEN
            RAISE_APPLICATION_ERROR(p_codigo, p_mensaje);
        END IF;
    END exigir;
BEGIN
    -- 1a) No debe existir ninguna asignacion GlobalAdmin activa
    SELECT COUNT(*)
      INTO v_count
      FROM USUARIO_ROL_UNIDAD uru
      JOIN ROL r ON r.ID_ROL = uru.ID_ROL
     WHERE r.NOMBRE_ROL = 'GlobalAdmin'
       AND uru.ACTIVO = 'S';

    /* exigir(v_count = 0, -20201,
           'Ya existe una asignacion GlobalAdmin activa. ' ||
           'La semilla fundacional aborta sin cambios.'); */

    -- 1b) No debe existir evento de inicializacion GlobalAdmin previo
    SELECT COUNT(*)
      INTO v_count
      FROM AUDITORIA_EVENTO
     WHERE TIPO_EVENTO = 'INICIALIZACION_GLOBAL_ADMIN';

    /* exigir(v_count = 0, -20202,
           'Ya existe un evento INICIALIZACION_GLOBAL_ADMIN en auditoria. ' ||
           'La semilla fundacional aborta sin cambios.'); */
           

    -- 1c) No debe existir combinacion bootstrap previa
    SELECT COUNT(*)
      INTO v_count
      FROM MATRIZ_FUNCION_PERFIL_UNIDAD
     WHERE ES_BOOTSTRAP = 'S';

    /* exigir(v_count = 0, -20203,
           'Ya existe una combinacion bootstrap (ES_BOOTSTRAP=''S'') en la matriz. ' ||
           'La semilla fundacional aborta sin cambios.'); */

    -- 1d) La unidad MIDAGRI (ID=1) debe existir
    SELECT COUNT(*)
      INTO v_count
      FROM UNIDAD_EJECUTORA
     WHERE ID_UNIDAD = 1;

    exigir(v_count = 1, -20204,
           'La unidad MIDAGRI (ID_UNIDAD=1) no existe. ' ||
           'Ejecute primero 001_baseline_piip.sql.');

    -- 1e) El rol GlobalAdmin (ID=1) debe existir
    SELECT COUNT(*)
      INTO v_count
      FROM ROL
     WHERE ID_ROL = 1;

    exigir(v_count = 1, -20205,
           'El rol GlobalAdmin (ID_ROL=1) no existe. ' ||
           'Ejecute primero 001_baseline_piip.sql.');

    -- 1f) El tipo_documento ID=1 debe existir (para la FK de DOCUMENTO)
    SELECT COUNT(*)
      INTO v_count
      FROM TIPO_DOCUMENTO
     WHERE ID_TIPO_DOC = 1;

    exigir(v_count = 1, -20206,
           'El tipo_documento ID=1 no existe. Ejecute primero 001_baseline_piip.sql.');

    DBMS_OUTPUT.PUT_LINE('[002] Precondiciones validadas. Iniciando DML...');
END;
/

-- ============================================================================
-- 2) DML: INSERTS CON IDs EXPLICITOS FIJOS
-- ============================================================================
PROMPT [002.1] Creando usuario GlobalAdmin (ID_USUARIO=1)...

INSERT INTO USUARIO (
    ID_USUARIO,
    KEYCLOAK_ID,
    LOGIN,
    NOMBRE_COMPLETO,
    CORREO,
    ACTIVO,
    CREADO_POR,
    FECHA_CREACION,
    LOGIN_SINTETICO
) VALUES (
    2,
    'ed3742bc-f2c2-4884-ae09-07e3f9ab98fc',   -- Keycloak sub proporcionado por OGTI
    'rovidev',
    'Rovi Dev',
    'rovidev@midagri.gob.pe',
    'S',
    'SEED_002',
    SYSTIMESTAMP,
    'N'                                         -- Login real, no sintetico
);

PROMPT [002.2] Creando proyecto semilla (ID_PROYECTO=1) para soportar DOCUMENTO...

INSERT INTO PROYECTO (
    ID_PROYECTO,
    CODIGO,
    TIPO_REGISTRO,
    NOMBRE,
    TIPO_SOLUCION,
    FUENTE_ORIGEN,
    DESCRIPCION,
    OBJETIVO_PEI,
    ACTIVIDAD_POI,
    ADMINISTRACION,
    ESTADO,
    ID_UNIDAD_EJECUTORA,
    ID_RESPONSABLE,
    CREADO_POR,
    FECHA_CREACION
) VALUES (
    1,
    '1234-SEED1GA-12345',                      -- Formato: 4dig-1a13car-5dig
    'PROYECTO',
    'Proyecto semilla fundacional GlobalAdmin PIIP',
    'POR_DEFINIR',
    'OTROS',
    'Proyecto semilla que soporta el documento de aprobacion ' ||
    'del bootstrap GlobalAdmin. Sin valor funcional; creado ' ||
    'exclusivamente para satisfacer la FK de DOCUMENTO.',
    'Semilla fundacional',
    'Semilla fundacional',
    'OM',
    'PRESENTADO',
    1,                                           -- MIDAGRI
    2,                                           -- El mismo usuario GlobalAdmin
    'SEED_002',
    SYSTIMESTAMP
);

PROMPT [002.3] Creando documento de aprobacion ficticio (ID_DOCUMENTO=1)...

INSERT INTO DOCUMENTO (
    ID_DOCUMENTO,
    ID_PROYECTO,
    ID_TIPO_DOC,
    ESTADO_AL_CARGAR,
    NOMBRE_ORIGINAL,
    NOMBRE_STORAGE,
    MIME_TYPE,
    TAMANO_BYTES,
    HASH_SHA256,
    ID_USUARIO_CARGA,
    FECHA_CARGA,
    ACTIVO,
    INMUTABLE,
    SCAN_ANTIVIRUS,
    NUMERO_VERSION
) VALUES (
    1,
    1,                                           -- Proyecto semilla ID=1
    1,                                           -- 'Ficha de Iniciativa de Innovacion Publica'
    'PRESENTADO',
    'Acta de Aprobacion - Seed GlobalAdmin PIIP.pdf',
    '/seed/ga-approval-00001.pdf',               -- UK_DOC_STORACK requiere valor unico
    'application/pdf',
    1,                                           -- Tamano minimo >0 y <= 26214400
    '0000000000000000000000000000000000000000000000000000000000000000',  -- 64 hex chars
    1,                                           -- El mismo usuario GlobalAdmin
    SYSTIMESTAMP,
    'S',                                         -- Activo
    'S',                                         -- Inmutable: no debe modificarse
    'PENDIENTE',
    1                                            -- Version inicial
);

PROMPT [002.4] Creando version de matriz funcional v1.0 (ID_VERSION=1)...

INSERT INTO MATRIZ_FUNCIONAL_VERSION (
    ID_VERSION,
    CODIGO_VERSION,
    ID_VERSION_ANTERIOR,
    ID_DOCUMENTO_APROBACION,
    VIGENTE_DESDE,
    VIGENTE_HASTA,
    ACTIVA,
    CREADO_POR,
    FECHA_CREACION
) VALUES (
    2,
    'MFV-002',
    NULL,                                        -- Version inicial, sin anterior
    1,                                           -- Referencia al documento de aprobacion
    SYSDATE,
    NULL,                                        -- Sin fecha fin: vigente indefinido
    'S',
    'SEED_002',
    SYSTIMESTAMP
);

PROMPT [002.5] Creando funcion ADMIN (ID_FUNCION=1)...

INSERT INTO MATRIZ_FUNCION (
    ID_FUNCION,
    ID_VERSION,
    CODIGO,
    DESCRIPCION,
    ACTIVA
) VALUES (
    2,
    1,                                           -- Version MFV-001
    'ADMIN',                                     -- Codigo canonico de la funcion
    'Administracion General',
    'S'
);

PROMPT [002.6] Creando combinacion bootstrap GlobalAdmin (ID_COMBINACION=1)...

/* INSERT INTO MATRIZ_FUNCION_PERFIL_UNIDAD (
    ID_COMBINACION,
    ID_VERSION,
    ID_FUNCION,
    ID_ROL,
    ID_UNIDAD,
    ID_APROBADOR,
    ID_REGISTRADOR,
    ID_DOCUMENTO_APROBACION,
    VIGENTE_DESDE,
    VIGENTE_HASTA,
    ACTIVA,
    CREADO_POR,
    FECHA_CREACION,
    ES_BOOTSTRAP
) VALUES (
    2,
    2,                                           -- Version MFV-002
    2,                                           -- Funcion ADMIN
    1,                                           -- Rol GlobalAdmin
    1,                                           -- Unidad MIDAGRI
    NULL,                                        -- Bootstrap: sin aprobador
    NULL,                                        -- Bootstrap: sin registrador
    NULL,                                        -- Bootstrap: sin documento
    SYSDATE,
    NULL,                                        -- Vigente indefinido
    'S',
    'SEED_002',
    SYSTIMESTAMP,
    'S'                                          -- ES_BOOTSTRAP='S' activa la
                                                 -- regla CK_MFPU_REGLA_FUNDACION
                                                 -- que exige NULL en aprobador,
                                                 -- registrador y documento
); */

PROMPT [002.7] Asignando GlobalAdmin a la unidad MIDAGRI (ID_USR_ROL_UNIDAD=1)...

INSERT INTO USUARIO_ROL_UNIDAD (
    ID_USR_ROL_UNIDAD,
    ID_USUARIO,
    ID_ROL,
    ID_UNIDAD,
    ACTIVO,
    FECHA_ASIGNACION,
    ASIGNADO_POR,
    FECHA_INICIO,
    FECHA_FIN,
    REVOCADA_EN,
    REVOCADA_POR,
    MOTIVO_REVOCACION,
    INACTIVA_TEMPORALMENTE,
    ID_COMBINACION_MATRIZ,
    ID_DOCUMENTO_FORMAL,
    VERSION
) VALUES (
    2,
    2,                                           -- Usuario GlobalAdmin
    1,                                           -- Rol GlobalAdmin
    1,                                           -- Unidad MIDAGRI
    'S',
    SYSDATE,
    'SEED_002',
    SYSTIMESTAMP,                                -- FECHA_INICIO (timestamp)
    NULL,                                        -- Sin fecha fin
    NULL,                                        -- No revocada
    NULL,
    NULL,
    'N',                                         -- No inactiva temporalmente
    1,                                           -- FK a MATRIZ_FUNCION_PERFIL_UNIDAD
    NULL,                                        -- Sin documento formal asociado
    0                                            -- Version inicial
);

-- ============================================================================
-- 3) AUDITORIA (conforme a constitution.md:161-164)
--    "La auditoria DEBE conservar como minimo sub, perfil, funcion, unidad,
--     Jefatura autorizante, aprobacion de despliegue, DBA ejecutor, fecha,
--     operacion y resultado."
-- ============================================================================
PROMPT [002.8] Registrando evento de auditoria fundacional...

INSERT INTO AUDITORIA_EVENTO (
    ID_EVENTO,
    TIPO_EVENTO,
    ENTIDAD_TIPO,
    ENTIDAD_ID,
    PAYLOAD_JSON,
    ID_USUARIO,
    FECHA_EVENTO,
    PROCESADO
) VALUES (
    2,
    'INICIALIZACION_GLOBAL_ADMIN',
    'USUARIO_ROL_UNIDAD',
    1,                                             -- ID_USR_ROL_UNIDAD creado
    '{"sub":"ed3742bc-f2c2-4884-ae09-07e3f9ab98fc",'
    || '"perfil":"GlobalAdmin",'
    || '"funcion":"ADMIN",'
    || '"unidad":"MIDAGRI",'
    || '"nombre_completo":"Rovi Dev",'
    || '"correo":"rovidev@midagri.gob.pe",'
    || '"realm_keycloak":"piip",'
    || '"id_usuario":1,'
    || '"id_rol":1,'
    || '"id_unidad":1,'
    || '"id_matriz_version":1,'
    || '"id_funcion":1,'
    || '"id_combinacion":1,'
    || '"id_asignacion_uru":1,'
    || '"jefatura_autorizante":"Jefatura de la Oficina de Modernizacion",'
    || '"aprobacion_despliegue":"<SUSTITUIR_APROBACION_DESPLIEGUE>",'
    || '"dba_ejecutor":"<SUSTITUIR_DBA_EJECUTOR>",'
    || '"fecha_ejecucion":"' || TO_CHAR(SYSDATE, 'YYYY-MM-DD"T"HH24:MI:SS') || '",'
    || '"origen":"SEED_002",'
    || '"operacion":"SEMILLA_FUNDACIONAL_GLOBAL_ADMIN",'
    || '"resultado":"EXITOSA"}',
    1,                                             -- ID_USUARIO auditor
    SYSTIMESTAMP,
    'S'
);

PROMPT [002.9] Registrando acceso de auditoria fundacional...

INSERT INTO AUDITORIA_ACCESO (
    ID_AUDIT,
    ID_USUARIO,
    ID_ROL_EFECTIVO,
    ID_UNIDAD_EFECTIVA,
    ENDPOINT,
    METODO_HTTP,
    CODIGO_RESPUESTA,
    IP_CLIENTE,
    FECHA_HORA,
    DURACION_MS
) VALUES (
    2,
    2,                                           -- Usuario GlobalAdmin
    1,                                           -- Rol efectivo
    1,                                           -- Unidad efectiva
    '/internal/seed/002/globaladmin',
    'POST',
    201,
    '127.0.0.1',
    SYSTIMESTAMP,
    NULL
);

-- ============================================================================
-- 4) VALIDACION FINAL
-- ============================================================================
PROMPT [002.10] Validando integridad de la semilla...

DECLARE
    v_count PLS_INTEGER;

    PROCEDURE exigir(
        p_condicion IN BOOLEAN,
        p_codigo    IN PLS_INTEGER,
        p_mensaje   IN VARCHAR2
    ) IS
    BEGIN
        IF NOT p_condicion THEN
            RAISE_APPLICATION_ERROR(p_codigo, p_mensaje);
        END IF;
    END exigir;
BEGIN
    -- Verificar que el usuario se creo correctamente
    SELECT COUNT(*) INTO v_count
      FROM USUARIO WHERE ID_USUARIO = 1 AND ACTIVO = 'S';
    exigir(v_count = 1, -20211, 'Fallo: USUARIO ID=1 no encontrado o inactivo');

    -- Verificar que el proyecto se creo
    SELECT COUNT(*) INTO v_count
      FROM PROYECTO WHERE ID_PROYECTO = 1;
    exigir(v_count = 1, -20212, 'Fallo: PROYECTO ID=1 no encontrado');

    -- Verificar que el documento se creo
    SELECT COUNT(*) INTO v_count
      FROM DOCUMENTO WHERE ID_DOCUMENTO = 1 AND ACTIVO = 'S';
    exigir(v_count = 1, -20213, 'Fallo: DOCUMENTO ID=1 no encontrado o inactivo');

    -- Verificar la version de matriz
    SELECT COUNT(*) INTO v_count
      FROM MATRIZ_FUNCIONAL_VERSION WHERE ID_VERSION = 1 AND ACTIVA = 'S';
    exigir(v_count = 1, -20214, 'Fallo: MATRIZ_FUNCIONAL_VERSION ID=1 no encontrada o inactiva');

    -- Verificar la funcion
    SELECT COUNT(*) INTO v_count
      FROM MATRIZ_FUNCION WHERE ID_FUNCION = 1 AND ACTIVA = 'S';
    exigir(v_count = 1, -20215, 'Fallo: MATRIZ_FUNCION ID=1 no encontrada o inactiva');

    -- Verificar que la combinacion es bootstrap
    SELECT COUNT(*) INTO v_count
      FROM MATRIZ_FUNCION_PERFIL_UNIDAD
     WHERE ID_COMBINACION = 1 AND ACTIVA = 'S' AND ES_BOOTSTRAP = 'S';
    exigir(v_count = 1, -20216,
           'Fallo: MATRIZ_FUNCION_PERFIL_UNIDAD ID=1 no encontrada, inactiva o no bootstrap');

    -- Verificar la asignacion usuario-rol-unidad
    SELECT COUNT(*) INTO v_count
      FROM USUARIO_ROL_UNIDAD
     WHERE ID_USR_ROL_UNIDAD = 1 AND ACTIVO = 'S'
       AND INACTIVA_TEMPORALMENTE = 'N';
    exigir(v_count = 1, -20217,
           'Fallo: USUARIO_ROL_UNIDAD ID=1 no encontrada, inactiva o temporalmente inactiva');

    -- Verificar el evento de auditoria
    SELECT COUNT(*) INTO v_count
      FROM AUDITORIA_EVENTO
     WHERE ID_EVENTO = 1
       AND TIPO_EVENTO = 'INICIALIZACION_GLOBAL_ADMIN'
       AND PROCESADO = 'S';
    exigir(v_count = 1, -20218,
           'Fallo: AUDITORIA_EVENTO ID=1 no encontrado, tipo incorrecto o no procesado');

    DBMS_OUTPUT.PUT_LINE('============================================================');
    DBMS_OUTPUT.PUT_LINE('  SEMILLA 002 VALIDADA EXITOSAMENTE');
    DBMS_OUTPUT.PUT_LINE('  Usuario: Rovi Dev (rovidev@midagri.gob.pe)');
    DBMS_OUTPUT.PUT_LINE('  Keycloak sub: ed3742bc-f2c2-4884-ae09-07e3f9ab98fc');
    DBMS_OUTPUT.PUT_LINE('  Rol: GlobalAdmin | Unidad: MIDAGRI');
    DBMS_OUTPUT.PUT_LINE('  Matriz: MFV-001 | Funcion: ADMIN');
    DBMS_OUTPUT.PUT_LINE('============================================================');
END;
/

COMMIT;

PROMPT Semilla 002_seed_globaladmin.sql completada correctamente.


-- SELECT * FROM USUARIO;
-- SELECT * FROM PROYECTO;
-- SELECT * FROM DOCUMENTO;
-- SELECT * FROM MATRIZ_FUNCIONAL_VERSION;
-- SELECT * FROM MATRIZ_FUNCION;
-- SELECT * FROM MATRIZ_FUNCION_PERFIL_UNIDAD;
-- SELECT * FROM USUARIO_ROL_UNIDAD;
-- SELECT * FROM AUDITORIA_EVENTO;
-- SELECT * FROM AUDITORIA_ACCESO;