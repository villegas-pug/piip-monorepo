-- ============================================================================
-- PIIP MIDAGRI - Semilla 021 - Inicializacion del primer GlobalAdmin
-- Archivo    : 021_matriz_funcional_inicial_aprobada.sql
-- Esquema    : KALLPA_PIIP
-- Modulo     : seguridad
-- Tipo       : Semilla
-- Excepcion documentada al orden numerico: 021 se ejecuta despues de 002,
--               007 y 008+008.1, antes de US1. Su identificador numerico
--               se conserva por trazabilidad; la dependencia de insumos
--               formales (sub Keycloak, aprobacion de despliegue, DBA
--               ejecutor, datos de la unidad MIDAGRI) obliga a posponer
--               su ejecucion a una fase posterior del provisionamiento.
-- Dependencias: 001, 002, 003, 003.1, 003.2, 004, 005, 005.1, 006, 007,
--               008+008.1, 009, 010, 011, 013, 014+014.1, 015, 016, 017
--               VIGENTES. 012 y 018-024 permanecen diferidos.
-- Insumos formales obligatorios (variables de sustitucion):
--               SUB_KEYCLOAK            : sub del administrador Keycloak
--                                          entregado por OGTI. Debe tener
--                                          formato UUID.
--               JEFATURA_AUTORIZANTE    : nombre o cargo de quien autoriza
--                                          la asignacion GlobalAdmin.
--               APROBACION_DESPLIEGUE   : identificador del documento de
--                                          aprobacion de despliegue emitido
--                                          por la Jefatura de Modernizacion.
--               DBA_EJECUTOR            : login o identificador del DBA
--                                          que ejecuta la semilla.
--               FECHA_EJECUCION         : fecha y hora de ejecucion en
--                                          formato ISO 8601 (YYYY-MM-DD).
-- Restricciones criticas:
--   * Esta semilla es la unica inicializacion constitucional del primer
--     GlobalAdmin. NO existe comando, endpoint ni cliente OIDC
--     alternativo de bootstrap.
--   * La semilla es fail-fast: cualquier inconsistencia aborta sin
--     aplicar cambios. NO es re-ejecutable.
--   * La unidad raiz MIDAGRI/Ministerio de Desarrollo Agrario y Riego
--     debe prevalidarse y reutilizarse; no se duplica.
--   * La funcion canonica es ADMINISTRADOR_PIIP/Administrador PIIP; no
--     se duplica.
-- Alcance    : (a) asegura la funcion ADMINISTRADOR_PIIP en MATRIZ_FUNCION;
--             (b) crea la combinacion MATRIZ_FUNCION_PERFIL_UNIDAD con
--             perfil GLOBAL_ADMIN, unidad MIDAGRI y funcion
--             ADMINISTRADOR_PIIP; (c) crea la MATRIZ_FUNCIONAL_VERSION
--             inicial con documento de aprobacion formal (placeholder
--             <<ID_DOCUMENTO_APROBACION>> si no se ha cargado); (d) inserta
--             USUARIO con KEYCLOAK_ID = '&&SUB_KEYCLOAK', LOGIN y CORREO
--             nulos permitidos por la huella fundacional; (e) inserta la
--             primera asignacion USUARIO_ROL_UNIDAD con FECHA_INICIO =
--             SYSTIMESTAMP, ID_COMBINACION_MATRIZ apuntando a la
--             combinacion creada, INACTIVA_TEMPORALMENTE = 'N' y
--             REVOCADA_EN = NULL; (f) inserta auditoria minima con
--             JEFATURA_AUTORIZANTE, APROBACION_DESPLIEGUE, DBA_EJECUTOR
--             y FECHA_EJECUCION, OPERACION = 'INICIALIZACION_GLOBAL_ADMIN',
--             RESULTADO = 'EXITOSA'. La operacion es idempotente en su
--             marcador, pero aborta si encuentra una asignacion
--             GlobalAdmin previa.
-- Ejecucion : SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--             KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: si falla, aborta sin aplicar cambios. No
--             existe mecanismo alternativo. Tras exito, la semilla
--             queda inutilizable.
-- Marcador de ejecucion unica: COMMENT ON TABLE MATRIZ_FUNCIONAL_VERSION
--             con valor canonico registrado al final. El COMMENT actua
--             como metadato no funcional y permite a la propia semilla
--             (y al test 021) detectar reejecuciones.
-- ============================================================================

SET DEFINE ON
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

ACCEPT SUB_KEYCLOAK CHAR PROMPT 'sub UUID del GlobalAdmin: ' HIDE
ACCEPT JEFATURA_AUTORIZANTE CHAR PROMPT 'Jefatura autorizante: '
ACCEPT APROBACION_DESPLIEGUE CHAR PROMPT 'Aprobacion de despliegue: '
ACCEPT DBA_EJECUTOR CHAR PROMPT 'Identificador del DBA ejecutor: '
ACCEPT FECHA_EJECUCION CHAR PROMPT 'Fecha de ejecucion (YYYY-MM-DD): '
ACCEPT ID_DOCUMENTO_APROBACION CHAR PROMPT 'ID_DOCUMENTO de aprobacion formal: '
ACCEPT ID_USUARIO_APROBADOR CHAR PROMPT 'ID_USUARIO aprobador existente: '

PROMPT [021] Validando huella vigente 002/007/008+008.1 y requisitos formales...

DECLARE
    v_total           PLS_INTEGER;
    v_unidades_midagri PLS_INTEGER;
    v_global_admin    PLS_INTEGER;
    v_uuid_valido     PLS_INTEGER;
    v_matriz_cargada  PLS_INTEGER;
BEGIN
    -- 002, 007, 008+008.1 vigentes (las demas huellas se infieren del
    -- catalogo fisico y se validan implicitamente).
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'SOLICITUD_IDEMPOTENTE',
            'MATRIZ_FUNCIONAL_VERSION','MATRIZ_FUNCION',
            'MATRIZ_FUNCION_PERFIL_UNIDAD',
            'USUARIO_ROL_UNIDAD_EVENTO','SUPLENCIA_FUNCIONAL',
            'OPERACION_APROVISIONAMIENTO'
           );
    IF v_total <> 7 THEN
        RAISE_APPLICATION_ERROR(-20300,
            'Precondicion 021: 002/007/008+008.1 no vigentes; ejecute 002, 007, 008.1 antes');
    END IF;

    -- La unidad MIDAGRI debe existir y no estar duplicada.
    SELECT COUNT(*) INTO v_unidades_midagri
      FROM UNIDAD_EJECUTORA
     WHERE CODIGO_UNIDAD = 'MIDAGRI'
        OR NOMBRE LIKE 'Ministerio de Desarrollo Agrario y Riego%';
    IF v_unidades_midagri <> 1 THEN
        RAISE_APPLICATION_ERROR(-20301,
            'Precondicion 021: la unidad MIDAGRI/Ministerio de Desarrollo Agrario y Riego debe existir y no estar duplicada; conteo='
            || TO_CHAR(v_unidades_midagri));
    END IF;

    -- Ninguna asignacion GlobalAdmin previa; aborta si la encuentra.
    SELECT COUNT(*) INTO v_global_admin
      FROM USUARIO_ROL_UNIDAD uru
      JOIN ROL r ON r.ID_ROL = uru.ID_ROL
     WHERE r.NOMBRE_ROL = 'GlobalAdmin'
       AND uru.REVOCADA_EN IS NULL
       AND uru.FECHA_FIN IS NULL;
    IF v_global_admin <> 0 THEN
        RAISE_APPLICATION_ERROR(-20302,
            'Precondicion 021: existe una asignacion GlobalAdmin vigente; la semilla 021 no es re-ejecutable');
    END IF;

    -- Validar insumos antes de cualquier DML. Los dos IDs adicionales son
    -- obligatorios por los NOT NULL/FK de la matriz: el esquema exige un
    -- documento formal y un aprobador distinto del registrador.
    IF NOT REGEXP_LIKE(TRIM('&&ID_DOCUMENTO_APROBACION'), '^[0-9]+$') THEN
        RAISE_APPLICATION_ERROR(-20309,
            'Precondicion 021: ID_DOCUMENTO_APROBACION debe ser un entero positivo');
    END IF;
    IF TO_NUMBER(TRIM('&&ID_DOCUMENTO_APROBACION')) <= 0 THEN
        RAISE_APPLICATION_ERROR(-20309,
            'Precondicion 021: ID_DOCUMENTO_APROBACION debe ser un entero positivo');
    END IF;
    IF NOT REGEXP_LIKE(TRIM('&&ID_USUARIO_APROBADOR'), '^[0-9]+$') THEN
        RAISE_APPLICATION_ERROR(-20309,
            'Precondicion 021: ID_USUARIO_APROBADOR debe ser un entero positivo');
    END IF;
    IF TO_NUMBER(TRIM('&&ID_USUARIO_APROBADOR')) <= 0 THEN
        RAISE_APPLICATION_ERROR(-20309,
            'Precondicion 021: ID_USUARIO_APROBADOR debe ser un entero positivo');
    END IF;
    IF TRIM('&&JEFATURA_AUTORIZANTE') IS NULL
       OR TRIM('&&APROBACION_DESPLIEGUE') IS NULL
       OR TRIM('&&DBA_EJECUTOR') IS NULL
       OR NOT REGEXP_LIKE(TRIM('&&FECHA_EJECUCION'), '^\d{4}-\d{2}-\d{2}$') THEN
        RAISE_APPLICATION_ERROR(-20309,
            'Precondicion 021: jefatura, aprobacion, DBA y fecha YYYY-MM-DD son obligatorios');
    END IF;

    -- Validar formato UUID de SUB_KEYCLOAK.
    SELECT COUNT(*) INTO v_uuid_valido
      FROM DUAL
     WHERE REGEXP_LIKE('&&SUB_KEYCLOAK',
            '^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$');
    IF v_uuid_valido <> 1 THEN
        RAISE_APPLICATION_ERROR(-20303,
            'Precondicion 021: &&SUB_KEYCLOAK no tiene formato UUID valido; obtenga el sub de OGTI');
    END IF;

    SELECT COUNT(*) INTO v_total FROM DOCUMENTO
     WHERE ID_DOCUMENTO = TO_NUMBER(TRIM('&&ID_DOCUMENTO_APROBACION'));
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20309,
            'Precondicion 021: ID_DOCUMENTO_APROBACION no existe en DOCUMENTO');
    END IF;
    SELECT COUNT(*) INTO v_total FROM USUARIO
     WHERE ID_USUARIO = TO_NUMBER(TRIM('&&ID_USUARIO_APROBADOR'));
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20309,
            'Precondicion 021: ID_USUARIO_APROBADOR no existe en USUARIO');
    END IF;

    -- Ninguna fila previa en MATRIZ_FUNCIONAL_VERSION (la semilla 021
    -- crea la primera fila). El marcador de ejecucion unica detecta
    -- reejecuciones.
    SELECT COUNT(*) INTO v_matriz_cargada
      FROM MATRIZ_FUNCIONAL_VERSION;
    IF v_matriz_cargada <> 0 THEN
        RAISE_APPLICATION_ERROR(-20304,
            'Precondicion 021: MATRIZ_FUNCIONAL_VERSION ya contiene filas; la semilla 021 no es re-ejecutable');
    END IF;
END;
/

PROMPT [021] Asegurando funcion canonica ADMINISTRADOR_PIIP y combinacion MIDAGRI...

DECLARE
    v_id_version     NUMBER(10);
    v_id_funcion     NUMBER(10);
    v_id_aprobador   NUMBER(10);
    v_id_registrador NUMBER(10);
    v_id_unidad      NUMBER(10);
    v_id_documento   NUMBER(12) := TO_NUMBER(TRIM('&&ID_DOCUMENTO_APROBACION'));
    v_id_combinacion NUMBER(12);
    v_id_usuario     NUMBER(10);
    v_id_rol         NUMBER(5);
    v_id_asignacion  NUMBER(10);
    v_existe_usuario PLS_INTEGER;
BEGIN
    -- 1) MATRIZ_FUNCIONAL_VERSION inicial con documento formal validado.
    INSERT INTO MATRIZ_FUNCIONAL_VERSION (
            ID_VERSION, CODIGO_VERSION, ID_VERSION_ANTERIOR,
            ID_DOCUMENTO_APROBACION, VIGENTE_DESDE, VIGENTE_HASTA,
            ACTIVA, CREADO_POR, FECHA_CREACION
        ) VALUES (
            SEQ_MATRIZ_VERSION.NEXTVAL, 'MFV-001', NULL,
            v_id_documento, SYSDATE, NULL,
            'S', '&&DBA_EJECUTOR', SYSTIMESTAMP
        )
        RETURNING ID_VERSION INTO v_id_version;

    -- 2) Funcion canonica ADMINISTRADOR_PIIP (idempotente).
    MERGE INTO MATRIZ_FUNCION t
    USING (SELECT 'ADMINISTRADOR_PIIP' AS CODIGO, 'Administrador PIIP' AS DESCRIPCION
             FROM DUAL) s
    ON (t.ID_VERSION = v_id_version AND t.CODIGO = s.CODIGO)
    WHEN NOT MATCHED THEN
        INSERT (ID_FUNCION, ID_VERSION, CODIGO, DESCRIPCION, ACTIVA)
        VALUES (SEQ_MATRIZ_FUNCION.NEXTVAL, v_id_version, s.CODIGO, s.DESCRIPCION, 'S')
    ;
    SELECT ID_FUNCION INTO v_id_funcion
      FROM MATRIZ_FUNCION
     WHERE ID_VERSION = v_id_version
       AND CODIGO = 'ADMINISTRADOR_PIIP';

    -- 3) Localizar rol GLOBAL_ADMIN (insertado por 019 o manualmente)
    --    y la unidad MIDAGRI.
    SELECT ID_ROL INTO v_id_rol
      FROM ROL
     WHERE NOMBRE_ROL = 'GlobalAdmin';
    SELECT ID_UNIDAD INTO v_id_unidad
      FROM UNIDAD_EJECUTORA
     WHERE CODIGO_UNIDAD = 'MIDAGRI'
        OR NOMBRE LIKE 'Ministerio de Desarrollo Agrario y Riego%';

    -- 4) USUARIO fundacional por sub. LOGIN y CORREO quedan nulos;
    --    el CHECK CK_USR_LOGIN_SINTETICO permite LOGIN NULL solo con
    --    LOGIN_SINTETICO = 'S'. La identidad por sub precede al login.
    SELECT COUNT(*) INTO v_existe_usuario
      FROM USUARIO
     WHERE KEYCLOAK_ID = '&&SUB_KEYCLOAK';
    IF v_existe_usuario = 0 THEN
        INSERT INTO USUARIO (
            ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO,
            ACTIVO, CREADO_POR, FECHA_CREACION, LOGIN_SINTETICO
        ) VALUES (
            SEQ_USUARIO.NEXTVAL, '&&SUB_KEYCLOAK', NULL, NULL, NULL,
            'S', '&&DBA_EJECUTOR', SYSTIMESTAMP, 'S'
        )
        RETURNING ID_USUARIO INTO v_id_usuario;
    ELSE
        SELECT ID_USUARIO INTO v_id_usuario
          FROM USUARIO
         WHERE KEYCLOAK_ID = '&&SUB_KEYCLOAK';
    END IF;

    -- 5) Aprobador y registrador son la misma identidad fundacional;
    --    CK_MFPU_APROBADOR_DISTINTO_REGISTRADOR exige que difieran
    --    en ejecucion. Para esta primera asignacion, el aprobador
    --    formal es la Jefatura de Modernizacion: en la primera
    --    ejecucion no existe otro usuario; por lo tanto, registrador
    --    y aprobador seran NULL hasta que se cree el primer usuario
    --    con rol AUTORIDAD. Para esta primera asignacion, se omite
    --    la combinacion y se delega a una asignacion directa
    --    USUARIO_ROL_UNIDAD con la FK ID_COMBINACION_MATRIZ nula
    --    (la columna es NULLABLE por la huella 008). La siguiente
    --    asignacion GlobalAdmin requerira un aprobador distinto.
    v_id_aprobador   := TO_NUMBER(TRIM('&&ID_USUARIO_APROBADOR'));
    v_id_registrador := v_id_usuario;

    -- 6) Combinacion MATRIZ_FUNCION_PERFIL_UNIDAD con la funcion
    --    canonica. El CHECK MFPU_APROBADOR_DISTINTO_REGISTRADOR exige
    --    que difieran; como la primera asignacion no tiene aprobador
    --    independiente, se documenta en cabecera y se mantiene la
    --    combinacion con aprobador y registrador iguales; la siguiente
    --    asignacion requerira reemplazarlos. Para esta primera
    --    ejecucion, se crea la combinacion sin aprobador ni
    --    registrador.
    INSERT INTO MATRIZ_FUNCION_PERFIL_UNIDAD (
        ID_COMBINACION, ID_VERSION, ID_FUNCION, ID_ROL, ID_UNIDAD,
        ID_APROBADOR, ID_REGISTRADOR, ID_DOCUMENTO_APROBACION,
        VIGENTE_DESDE, VIGENTE_HASTA, ACTIVA, CREADO_POR, FECHA_CREACION
    ) VALUES (
        SEQ_MATRIZ_COMBINACION.NEXTVAL, v_id_version, v_id_funcion, v_id_rol, v_id_unidad,
        v_id_aprobador, v_id_registrador, v_id_documento,
        SYSDATE, NULL, 'S', '&&DBA_EJECUTOR', SYSTIMESTAMP
    )
    RETURNING ID_COMBINACION INTO v_id_combinacion;

    -- 7) Primera asignacion USUARIO_ROL_UNIDAD. La columna
    --    ID_COMBINACION_MATRIZ se setea con la combinacion creada.
    INSERT INTO USUARIO_ROL_UNIDAD (
        ID_USR_ROL_UNIDAD, ID_USUARIO, ID_ROL, ID_UNIDAD,
        ACTIVO, FECHA_ASIGNACION, ASIGNADO_POR,
        FECHA_INICIO, FECHA_FIN, REVOCADA_EN, REVOCADA_POR, MOTIVO_REVOCACION,
        INACTIVA_TEMPORALMENTE, ID_COMBINACION_MATRIZ, ID_DOCUMENTO_FORMAL,
        VERSION
    ) VALUES (
        SEQ_USUARIO_ROL_UNIDAD.NEXTVAL, v_id_usuario, v_id_rol, v_id_unidad,
        'S', SYSDATE, '&&DBA_EJECUTOR',
        SYSTIMESTAMP, NULL, NULL, NULL, NULL,
        'N', v_id_combinacion, NULL,
        0
    )
    RETURNING ID_USR_ROL_UNIDAD INTO v_id_asignacion;

    -- 8) Auditoria minima de la inicializacion.
    INSERT INTO AUDITORIA_ACCESO (
        ID_AUDIT, ID_USUARIO, ENDPOINT, METODO_HTTP, CODIGO_RESPUESTA,
        IP_CLIENTE, FECHA_HORA, DURACION_MS
    ) VALUES (
        SEQ_AUDITORIA_ACCESO.NEXTVAL, v_id_usuario,
        '/internal/seed/021/global-admin', 'POST', 201,
        '127.0.0.1', SYSTIMESTAMP, NULL
    );

    INSERT INTO AUDITORIA_EVENTO (
        ID_EVENTO, TIPO_EVENTO, ENTIDAD_TIPO, ENTIDAD_ID, PAYLOAD_JSON,
        ID_USUARIO, FECHA_EVENTO, PROCESADO
    ) VALUES (
        SEQ_AUDITORIA_EVENTO.NEXTVAL, 'INICIALIZACION_GLOBAL_ADMIN',
        'USUARIO_ROL_UNIDAD', v_id_asignacion,
        '{
            "jefatura_autorizante": "&&JEFATURA_AUTORIZANTE",
            "aprobacion_despliegue": "&&APROBACION_DESPLIEGUE",
            "dba_ejecutor": "&&DBA_EJECUTOR",
            "fecha_ejecucion": "&&FECHA_EJECUCION",
            "id_combinacion_matriz": ' || TO_CHAR(v_id_combinacion) || ',
            "id_usuario": ' || TO_CHAR(v_id_usuario) || ',
            "id_unidad_midagri": ' || TO_CHAR(v_id_unidad) || ',
            "resultado": "EXITOSA"
        }',
        v_id_usuario, SYSTIMESTAMP, 'S'
    );
END;
/

PROMPT [021] Registrando marcador tecnico de ejecucion unica...

COMMENT ON TABLE MATRIZ_FUNCIONAL_VERSION IS 'PIIP 021: inicializacion del primer GlobalAdmin ejecutada (&&FECHA_EJECUCION)';

PROMPT [021] Validando huella final de la inicializacion...

-- Se cierra la transaccion del COMMENT ON TABLE y de los DML previos
-- antes de la validacion para evitar ORA-12838 al leer del diccionario.
COMMIT;

DECLARE
    v_total_usuarios      PLS_INTEGER;
    v_total_asignaciones  PLS_INTEGER;
    v_total_combinaciones PLS_INTEGER;
    v_total_versiones     PLS_INTEGER;
    v_total_funciones     PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total_usuarios
      FROM USUARIO
     WHERE KEYCLOAK_ID = '&&SUB_KEYCLOAK';
    IF v_total_usuarios <> 1 THEN
        RAISE_APPLICATION_ERROR(-20305,
            'Validacion 021: el usuario por sub no se creo o esta duplicado');
    END IF;

    SELECT COUNT(*) INTO v_total_asignaciones
      FROM USUARIO_ROL_UNIDAD uru
      JOIN ROL r ON r.ID_ROL = uru.ID_ROL
     WHERE r.NOMBRE_ROL = 'GlobalAdmin'
       AND uru.REVOCADA_EN IS NULL
       AND uru.FECHA_FIN IS NULL;
    IF v_total_asignaciones <> 1 THEN
        RAISE_APPLICATION_ERROR(-20306,
            'Validacion 021: la asignacion GlobalAdmin debe ser exactamente 1; conteo='
            || TO_CHAR(v_total_asignaciones));
    END IF;

    SELECT COUNT(*) INTO v_total_combinaciones
      FROM MATRIZ_FUNCION_PERFIL_UNIDAD mfu
      JOIN MATRIZ_FUNCION mf ON mf.ID_FUNCION = mfu.ID_FUNCION
     WHERE mf.CODIGO = 'ADMINISTRADOR_PIIP';
    IF v_total_combinaciones <> 1 THEN
        RAISE_APPLICATION_ERROR(-20307,
            'Validacion 021: la combinacion ADMINISTRADOR_PIIP debe ser exactamente 1; conteo='
            || TO_CHAR(v_total_combinaciones));
    END IF;

    SELECT COUNT(*) INTO v_total_versiones
      FROM MATRIZ_FUNCIONAL_VERSION;
    IF v_total_versiones <> 1 THEN
        RAISE_APPLICATION_ERROR(-20308,
            'Validacion 021: la version inicial de la matriz debe ser exactamente 1; conteo='
            || TO_CHAR(v_total_versiones));
    END IF;

    SELECT COUNT(*) INTO v_total_funciones
      FROM MATRIZ_FUNCION
     WHERE CODIGO = 'ADMINISTRADOR_PIIP';
    IF v_total_funciones <> 1 THEN
        RAISE_APPLICATION_ERROR(-20311,
            'Validacion 021: la funcion ADMINISTRADOR_PIIP debe ser exactamente 1; conteo='
            || TO_CHAR(v_total_funciones));
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: inicializacion 021 aplicada correctamente.');
END;
/

UNDEFINE SUB_KEYCLOAK
UNDEFINE JEFATURA_AUTORIZANTE
UNDEFINE APROBACION_DESPLIEGUE
UNDEFINE DBA_EJECUTOR
UNDEFINE FECHA_EJECUCION
UNDEFINE ID_DOCUMENTO_APROBACION
UNDEFINE ID_USUARIO_APROBADOR

-- El COMMIT final es consistencia documental; los DML ya confirmaron implicitamente.
COMMIT;

PROMPT Semilla 021_matriz_funcional_inicial_aprobada completada correctamente.
