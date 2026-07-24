-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 008 - Vigencia de usuarios, rol,
-- unidad, asignaciones, eventos, suplencia y aprovisionamiento
-- Archivo   : 008_usuario_rol_unidad_vigencia.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : seguridad
-- Dependencias: 002 (database/ddl/auditoria/002_auditoria_idempotencia.sql),
--               003 (database/ddl/documentos/003_expediente_serie_version.sql),
--               007 (database/ddl/seguridad/007_matriz_funcional_versionada.sql)
--               y 001 (database/ddl/init/001_baseline_piip.sql)
-- Alcance   : Evoluciona USUARIO para permitir identidad fundacional con
--             LOGIN nulo y LOGIN_SINTETICO='S'. Evoluciona USUARIO_ROL_UNIDAD
--             con vigencia completa (FECHA_INICIO, FECHA_FIN), revocacion,
--             inactivacion temporal, FK a combinacion de matriz y columna
--             VERSION. Crea USUARIO_ROL_UNIDAD_EVENTO append-only,
--             SUPLENCIA_FUNCIONAL y OPERACION_APROVISIONAMIENTO. La FK hacia
--             MATRIZ_FUNCION_PERFIL_UNIDAD se crea ENABLE NOVALIDATE hasta
--             el corte 024; un indice funcional unico evita multiples
--             asignaciones abiertas por terna usuario-rol-unidad.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: detener nuevas asignaciones/suplencias y
--            conservar historial. Los eventos y la trazabilidad de
--            aprovisionamiento permanecen append-only.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [008] Validando precondiciones del baseline 001, 002, 003 y 007...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones: 16 tablas y 13 secuencias tras 001+002+003; ademas las
--    3 tablas y 3 secuencias de la matriz (007) deben estar presentes.
-- ----------------------------------------------------------------------------
DECLARE
    v_total_tablas PLS_INTEGER;
    v_total_secuencias PLS_INTEGER;
    v_total_matriz_tablas PLS_INTEGER;
    v_total_matriz_sec PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total_tablas
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'UNIDAD_EJECUTORA','USUARIO','ROL','USUARIO_ROL_UNIDAD',
            'PROYECTO','PROYECTO_UNIDAD_ORGANICA','TRANSICION_PERMITIDA',
            'TIPO_DOCUMENTO','DOCUMENTO','TRANSICION_ESTADO',
            'SECUENCIA_CODIGO','AUDITORIA_ACCESO','AUDITORIA_EVENTO',
            'SOLICITUD_IDEMPOTENTE','EXPEDIENTE_INSTITUCIONAL','DOCUMENTO_SERIE'
           );
    IF v_total_tablas <> 16 THEN
        RAISE_APPLICATION_ERROR(
            -20010,
            'Precondicion 008: se esperaban 16 tablas (001+002+003) y se encontraron '
            || TO_CHAR(v_total_tablas)
        );
    END IF;

    SELECT COUNT(*) INTO v_total_secuencias
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_UNIDAD_EJECUTORA','SEQ_USUARIO','SEQ_USUARIO_ROL_UNIDAD',
            'SEQ_PROYECTO','SEQ_PROYECTO_UO','SEQ_DOCUMENTO',
            'SEQ_TRANSICION_ESTADO','SEQ_SECUENCIA_CODIGO',
            'SEQ_AUDITORIA_ACCESO','SEQ_AUDITORIA_EVENTO',
            'SEQ_SOLICITUD_IDEMPOTENTE',
            'SEQ_EXPEDIENTE_INSTITUCIONAL','SEQ_DOCUMENTO_SERIE'
           );
    IF v_total_secuencias <> 13 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Precondicion 008: se esperaban 13 secuencias (001+002+003) y se encontraron '
            || TO_CHAR(v_total_secuencias)
        );
    END IF;

    SELECT COUNT(*) INTO v_total_matriz_tablas
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'MATRIZ_FUNCIONAL_VERSION','MATRIZ_FUNCION',
            'MATRIZ_FUNCION_PERFIL_UNIDAD'
           );
    IF v_total_matriz_tablas <> 3 THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 008: la matriz funcional (007) no esta vigente; ejecute antes 007'
        );
    END IF;

    SELECT COUNT(*) INTO v_total_matriz_sec
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_MATRIZ_VERSION','SEQ_MATRIZ_FUNCION','SEQ_MATRIZ_COMBINACION'
           );
    IF v_total_matriz_sec <> 3 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 008: las secuencias de la matriz (007) no estan vigentes'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de que las nuevas tablas/columnas de 008 NO existen.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'USUARIO_ROL_UNIDAD_EVENTO','SUPLENCIA_FUNCIONAL',
            'OPERACION_APROVISIONAMIENTO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 008: las tablas de vigencia ya existen; el incremento ya fue aplicado'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'USUARIO_ROL_UNIDAD'
       AND COLUMN_NAME IN (
            'FECHA_INICIO','FECHA_FIN','REVOCADA_EN','REVOCADA_POR',
            'MOTIVO_REVOCACION','INACTIVA_TEMPORALMENTE','ID_COMBINACION_MATRIZ',
            'ID_DOCUMENTO_FORMAL','VERSION'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20015,
            'Precondicion 008: USUARIO_ROL_UNIDAD ya tiene columnas de vigencia; el incremento ya fue aplicado'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de objetos futuros 009-024 que NO deben existir.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'INICIATIVA_PROYECTO','PROYECTO_RESPONSABLE',
            'PARTICIPANTE_PERSONA','PROYECTO_PARTICIPANTE_PERSONA','PROYECTO_PARTICIPANTE_UNIDAD',
            'PROYECTO_CAMPO_CLASIFICACION','PROYECTO_CAMPO_CLASIF_HIST',
            'EVALUACION_INICIATIVA','SUBSANACION_INICIATIVA','APLICABILIDAD_INICIATIVA',
            'APLICABILIDAD_CRITERIO',
            'PLANIFICACION_PROYECTO','CICLO_PROYECTO','CICLO_EVIDENCIA','PRODUCTO_PARCIAL',
            'PRESENTACION_PRODUCTO_FINAL','VALIDACION_RESULTADO','CIERRE_PROYECTO',
            'INCORPORACION_REGISTRO','INCORPORACION_CAMBIO','INCORPORACION_CONFLICTO',
            'REPORTE_INSTITUCIONAL','REPORTE_SNAPSHOT','REPORTE_ARCHIVO','REPORTE_APROBACION',
            'REPORTE_DESTINATARIO','REPORTE_REMISION',
            'PROTOTIPO_PIIP','PROTOTIPO_VALIDACION','PROTOTIPO_HALLAZGO','MEDICION_EXPERIENCIA',
            'MEDICION_MUESTRA','MATRIZ_META_RECORRIDO',
            'CAT_OBJETIVO_PEI_VERSION','CAT_OBJETIVO_PEI',
            'CAT_ACTIVIDAD_POI_VERSION','CAT_ACTIVIDAD_POI',
            'DOCUMENTO_CLASIFICACION_HIST','DOCUMENTO_PUBLICACION'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20016,
            'Precondicion 008: existen objetos futuros 009-024 ya creados'
        );
    END IF;
END;
/

PROMPT [008] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 008
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Evolucion de USUARIO. Se anade LOGIN_SINTETICO y se permite
--    CORREO/NOMBRE_COMPLETO/LOGIN nulos solo bajo LOGIN_SINTETICO='S' para
--    la identidad fundacional creada en 021.
-- ----------------------------------------------------------------------------
PROMPT [008.1] Anadiendo LOGIN_SINTETICO a USUARIO
ALTER TABLE USUARIO ADD (LOGIN_SINTETICO CHAR(1 CHAR) DEFAULT 'N' NOT NULL);

PROMPT [008.2] Backfill de LOGIN_SINTETICO en filas existentes
UPDATE USUARIO SET LOGIN_SINTETICO = 'N' WHERE LOGIN_SINTETICO IS NULL;

PROMPT [008.3] Permitir LOGIN/CORREO/NOMBRE_COMPLETO nulos
ALTER TABLE USUARIO MODIFY (LOGIN            NULL);
ALTER TABLE USUARIO MODIFY (CORREO           NULL);
ALTER TABLE USUARIO MODIFY (NOMBRE_COMPLETO  NULL);

PROMPT [008.4] CHECK de coherencia LOGIN_SINTETICO/LOGIN en USUARIO
ALTER TABLE USUARIO
    ADD CONSTRAINT CK_USR_LOGIN_SINTETICO
    CHECK (
        (LOGIN_SINTETICO = 'S' AND LOGIN IS NULL)
     OR (LOGIN_SINTETICO = 'N' AND LOGIN IS NOT NULL)
    );

PROMPT [008.5] FKs efectivas en AUDITORIA_ACCESO (002 se difirio a 008)
ALTER TABLE AUDITORIA_ACCESO
    ADD CONSTRAINT FK_AA_ROL_EFECTIVO
    FOREIGN KEY (ID_ROL_EFECTIVO) REFERENCES ROL (ID_ROL);

ALTER TABLE AUDITORIA_ACCESO
    ADD CONSTRAINT FK_AA_UNIDAD_EFECTIVA
    FOREIGN KEY (ID_UNIDAD_EFECTIVA) REFERENCES UNIDAD_EJECUTORA (ID_UNIDAD);

-- ----------------------------------------------------------------------------
-- 2) Evolucion de USUARIO_ROL_UNIDAD. Nuevas columnas de vigencia, version
--    y FKs a la combinacion de matriz y al documento formal.
-- ----------------------------------------------------------------------------
PROMPT [008.6] Anadiendo columnas de vigencia, revocacion e inactivacion temporal
ALTER TABLE USUARIO_ROL_UNIDAD ADD (FECHA_INICIO            DATE DEFAULT SYSDATE NOT NULL);
ALTER TABLE USUARIO_ROL_UNIDAD ADD (FECHA_FIN               DATE);
ALTER TABLE USUARIO_ROL_UNIDAD ADD (REVOCADA_EN             TIMESTAMP(6));
ALTER TABLE USUARIO_ROL_UNIDAD ADD (REVOCADA_POR            VARCHAR2(100 CHAR));
ALTER TABLE USUARIO_ROL_UNIDAD ADD (MOTIVO_REVOCACION       VARCHAR2(2000 CHAR));
ALTER TABLE USUARIO_ROL_UNIDAD ADD (INACTIVA_TEMPORALMENTE  CHAR(1 CHAR) DEFAULT 'N' NOT NULL);
ALTER TABLE USUARIO_ROL_UNIDAD ADD (ID_COMBINACION_MATRIZ   NUMBER(12));
ALTER TABLE USUARIO_ROL_UNIDAD ADD (ID_DOCUMENTO_FORMAL     NUMBER(12));
ALTER TABLE USUARIO_ROL_UNIDAD ADD (VERSION                 NUMBER(10) DEFAULT 0 NOT NULL);

PROMPT [008.7] CHECKs de vigencia y revocacion en USUARIO_ROL_UNIDAD
ALTER TABLE USUARIO_ROL_UNIDAD
    ADD CONSTRAINT CK_URU_VIGENCIA
    CHECK (FECHA_FIN IS NULL OR FECHA_FIN >= FECHA_INICIO);

ALTER TABLE USUARIO_ROL_UNIDAD
    ADD CONSTRAINT CK_URU_REVOCADA
    CHECK (
        (REVOCADA_EN IS NULL AND REVOCADA_POR IS NULL)
     OR (REVOCADA_EN IS NOT NULL AND REVOCADA_POR IS NOT NULL)
    );

ALTER TABLE USUARIO_ROL_UNIDAD
    ADD CONSTRAINT CK_URU_INACTIVA_TEMP
    CHECK (INACTIVA_TEMPORALMENTE IN ('S','N'));

PROMPT [008.8] FKs de USUARIO_ROL_UNIDAD (combinacion ENABLE NOVALIDATE)
ALTER TABLE USUARIO_ROL_UNIDAD
    ADD CONSTRAINT FK_URU_COMBINACION
    FOREIGN KEY (ID_COMBINACION_MATRIZ) REFERENCES MATRIZ_FUNCION_PERFIL_UNIDAD (ID_COMBINACION)
    ENABLE NOVALIDATE;

ALTER TABLE USUARIO_ROL_UNIDAD
    ADD CONSTRAINT FK_URU_DOCUMENTO_FORMAL
    FOREIGN KEY (ID_DOCUMENTO_FORMAL) REFERENCES DOCUMENTO (ID_DOCUMENTO);

PROMPT [008.9] Indice funcional unico UX_URU_ABIERTAS
CREATE UNIQUE INDEX UX_URU_ABIERTAS
    ON USUARIO_ROL_UNIDAD (
        CASE WHEN FECHA_FIN IS NULL
               AND REVOCADA_EN IS NULL
               AND INACTIVA_TEMPORALMENTE = 'N'
             THEN ID_USUARIO || ':' || ID_ROL || ':' || ID_UNIDAD
        END
    );

PROMPT [008.10] Indice auxiliar IDX_URU_COMBINACION_MATRIZ
CREATE INDEX IDX_URU_COMBINACION_MATRIZ
    ON USUARIO_ROL_UNIDAD (ID_COMBINACION_MATRIZ);

-- ----------------------------------------------------------------------------
-- 3) Tabla USUARIO_ROL_UNIDAD_EVENTO (append-only).
-- ----------------------------------------------------------------------------
PROMPT [008.11] Creando tabla USUARIO_ROL_UNIDAD_EVENTO
CREATE TABLE USUARIO_ROL_UNIDAD_EVENTO (
    ID_EVENTO              NUMBER(12)                      NOT NULL,
    ID_ASIGNACION          NUMBER(10)                      NOT NULL,
    TIPO_EVENTO            VARCHAR2(30 CHAR)               NOT NULL,
    ID_USUARIO_ACTOR       NUMBER(10),
    ID_ROL_ACTOR           NUMBER(5),
    ID_UNIDAD_ACTOR        NUMBER(10),
    FECHA_EVENTO           TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    MOTIVO                 VARCHAR2(2000 CHAR),
    ID_ASIGNACION_EFECTIVA NUMBER(10)
);

PROMPT [008.12] PK, FKs y CHECKs de USUARIO_ROL_UNIDAD_EVENTO
ALTER TABLE USUARIO_ROL_UNIDAD_EVENTO
    ADD CONSTRAINT PK_USUARIO_ROL_UNIDAD_EVENTO PRIMARY KEY (ID_EVENTO);

ALTER TABLE USUARIO_ROL_UNIDAD_EVENTO
    ADD CONSTRAINT FK_URUE_ASIGNACION
    FOREIGN KEY (ID_ASIGNACION) REFERENCES USUARIO_ROL_UNIDAD (ID_USR_ROL_UNIDAD);

ALTER TABLE USUARIO_ROL_UNIDAD_EVENTO
    ADD CONSTRAINT FK_URUE_USUARIO_ACTOR
    FOREIGN KEY (ID_USUARIO_ACTOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE USUARIO_ROL_UNIDAD_EVENTO
    ADD CONSTRAINT CK_URUE_TIPO_EVENTO
    CHECK (TIPO_EVENTO IN ('ALTA','MODIFICACION','REVOCACION',
                            'ACTIVACION_TEMPORAL','SUPLENCIA'));

PROMPT [008.13] Creando indices IDX_URUE_ASIGNACION e IDX_URUE_FECHA
CREATE INDEX IDX_URUE_ASIGNACION ON USUARIO_ROL_UNIDAD_EVENTO (ID_ASIGNACION);
CREATE INDEX IDX_URUE_FECHA      ON USUARIO_ROL_UNIDAD_EVENTO (FECHA_EVENTO);

-- ----------------------------------------------------------------------------
-- 4) Tabla SUPLENCIA_FUNCIONAL.
-- ----------------------------------------------------------------------------
PROMPT [008.14] Creando tabla SUPLENCIA_FUNCIONAL
CREATE TABLE SUPLENCIA_FUNCIONAL (
    ID_SUPLENCIA              NUMBER(12)                      NOT NULL,
    ID_ASIGNACION_TITULAR     NUMBER(10)                      NOT NULL,
    ID_ASIGNACION_SUPLENTE    NUMBER(10)                      NOT NULL,
    INICIO                    DATE                            NOT NULL,
    FIN                       DATE                            NOT NULL,
    TERMINADA_EN              TIMESTAMP(6),
    ID_AUTORIDAD              NUMBER(10)                      NOT NULL,
    ID_DOCUMENTO_FORMAL       NUMBER(12)                      NOT NULL,
    CREADO_POR                VARCHAR2(100 CHAR)              NOT NULL,
    FECHA_CREACION            TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [008.15] PK, UK, FKs y CHECKs de SUPLENCIA_FUNCIONAL
ALTER TABLE SUPLENCIA_FUNCIONAL
    ADD CONSTRAINT PK_SUPLENCIA_FUNCIONAL PRIMARY KEY (ID_SUPLENCIA);

ALTER TABLE SUPLENCIA_FUNCIONAL
    ADD CONSTRAINT UK_SF_TITULAR_INICIO UNIQUE (ID_ASIGNACION_TITULAR, INICIO);

ALTER TABLE SUPLENCIA_FUNCIONAL
    ADD CONSTRAINT FK_SF_TITULAR
    FOREIGN KEY (ID_ASIGNACION_TITULAR) REFERENCES USUARIO_ROL_UNIDAD (ID_USR_ROL_UNIDAD);

ALTER TABLE SUPLENCIA_FUNCIONAL
    ADD CONSTRAINT FK_SF_SUPLENTE
    FOREIGN KEY (ID_ASIGNACION_SUPLENTE) REFERENCES USUARIO_ROL_UNIDAD (ID_USR_ROL_UNIDAD);

ALTER TABLE SUPLENCIA_FUNCIONAL
    ADD CONSTRAINT FK_SF_AUTORIDAD
    FOREIGN KEY (ID_AUTORIDAD) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE SUPLENCIA_FUNCIONAL
    ADD CONSTRAINT FK_SF_DOCUMENTO
    FOREIGN KEY (ID_DOCUMENTO_FORMAL) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE SUPLENCIA_FUNCIONAL
    ADD CONSTRAINT CK_SF_DISTINTAS
    CHECK (ID_ASIGNACION_TITULAR <> ID_ASIGNACION_SUPLENTE);

ALTER TABLE SUPLENCIA_FUNCIONAL
    ADD CONSTRAINT CK_SF_VIGENCIA
    CHECK (FIN >= INICIO);

PROMPT [008.16] Creando indices IDX_SF_TITULAR e IDX_SF_SUPLENTE
CREATE INDEX IDX_SF_TITULAR  ON SUPLENCIA_FUNCIONAL (ID_ASIGNACION_TITULAR);
CREATE INDEX IDX_SF_SUPLENTE ON SUPLENCIA_FUNCIONAL (ID_ASIGNACION_SUPLENTE);

-- ----------------------------------------------------------------------------
-- 5) Tabla OPERACION_APROVISIONAMIENTO. Conserva la huella idempotente y
--    el estado tecnico de la operacion Keycloak-Oracle.
-- ----------------------------------------------------------------------------
PROMPT [008.17] Creando tabla OPERACION_APROVISIONAMIENTO
CREATE TABLE OPERACION_APROVISIONAMIENTO (
    ID_OPERACION        NUMBER(12)                      NOT NULL,
    CLAVE_IDEMPOTENTE   VARCHAR2(100 CHAR)              NOT NULL,
    HASH_PAYLOAD        VARCHAR2(64 CHAR)               NOT NULL,
    ID_USUARIO_OBJETIVO NUMBER(10),
    KEYCLOAK_ID         VARCHAR2(36 CHAR),
    ESTADO_TECNICO      VARCHAR2(30 CHAR)               NOT NULL,
    INTENTO             NUMBER(3) DEFAULT 1             NOT NULL,
    ERROR_RECUPERABLE   CHAR(1 CHAR) DEFAULT 'N'        NOT NULL,
    RESULTADO_ORACLE    VARCHAR2(2000 CHAR),
    CREADO_POR          VARCHAR2(100 CHAR)              NOT NULL,
    FECHA_CREACION      TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    FECHA_CIERRE        TIMESTAMP(6)
);

PROMPT [008.18] PK, UK, FKs y CHECKs de OPERACION_APROVISIONAMIENTO
ALTER TABLE OPERACION_APROVISIONAMIENTO
    ADD CONSTRAINT PK_OPERACION_APROVISIONAMIENTO PRIMARY KEY (ID_OPERACION);

ALTER TABLE OPERACION_APROVISIONAMIENTO
    ADD CONSTRAINT UK_OA_CLAVE UNIQUE (CLAVE_IDEMPOTENTE);

ALTER TABLE OPERACION_APROVISIONAMIENTO
    ADD CONSTRAINT FK_OA_USUARIO_OBJETIVO
    FOREIGN KEY (ID_USUARIO_OBJETIVO) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE OPERACION_APROVISIONAMIENTO
    ADD CONSTRAINT CK_OA_ESTADO
    CHECK (ESTADO_TECNICO IN ('INICIADA','KEYCLOAK_CREADO_DESHABILITADO',
                                'ORACLE_PENDIENTE','COMPLETADA',
                                'FALLIDA_NO_RECUPERABLE'));

ALTER TABLE OPERACION_APROVISIONAMIENTO
    ADD CONSTRAINT CK_OA_ERROR_RECUPERABLE
    CHECK (ERROR_RECUPERABLE IN ('S','N'));

ALTER TABLE OPERACION_APROVISIONAMIENTO
    ADD CONSTRAINT CK_OA_HASH
    CHECK (REGEXP_LIKE(HASH_PAYLOAD, '^[0-9A-Fa-f]{64}$'));

PROMPT [008.19] Creando indices IDX_OA_ESTADO e IDX_OA_USUARIO_OBJETIVO
CREATE INDEX IDX_OA_ESTADO          ON OPERACION_APROVISIONAMIENTO (ESTADO_TECNICO);
CREATE INDEX IDX_OA_USUARIO_OBJETIVO ON OPERACION_APROVISIONAMIENTO (ID_USUARIO_OBJETIVO);

-- ============================================================================
-- Validacion final del script 008
-- ============================================================================
PROMPT [008.20] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    -- USUARIO: LOGIN_SINTETICO y CHECK
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'USUARIO'
       AND COLUMN_NAME = 'LOGIN_SINTETICO';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20030,
            'Validacion 008: LOGIN_SINTETICO ausente en USUARIO');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'USUARIO'
       AND CONSTRAINT_NAME = 'CK_USR_LOGIN_SINTETICO'
       AND CONSTRAINT_TYPE = 'C';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 008: CK_USR_LOGIN_SINTETICO ausente');
    END IF;

    -- AUDITORIA_ACCESO: FKs efectivas
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'AUDITORIA_ACCESO'
       AND CONSTRAINT_NAME IN ('FK_AA_ROL_EFECTIVO','FK_AA_UNIDAD_EFECTIVA')
       AND CONSTRAINT_TYPE = 'R';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20032,
            'Validacion 008: FKs efectivas de AUDITORIA_ACCESO ausentes');
    END IF;

    -- USUARIO_ROL_UNIDAD: columnas de vigencia
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'USUARIO_ROL_UNIDAD'
       AND COLUMN_NAME IN (
            'FECHA_INICIO','FECHA_FIN','REVOCADA_EN','REVOCADA_POR',
            'MOTIVO_REVOCACION','INACTIVA_TEMPORALMENTE','ID_COMBINACION_MATRIZ',
            'ID_DOCUMENTO_FORMAL','VERSION'
           );
    IF v_total <> 9 THEN
        RAISE_APPLICATION_ERROR(-20033,
            'Validacion 008: columnas de vigencia en USUARIO_ROL_UNIDAD incompletas');
    END IF;

    -- USUARIO_ROL_UNIDAD: CHECKs y FKs
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'USUARIO_ROL_UNIDAD'
       AND CONSTRAINT_NAME IN ('CK_URU_VIGENCIA','CK_URU_REVOCADA','CK_URU_INACTIVA_TEMP')
       AND CONSTRAINT_TYPE = 'C';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20034,
            'Validacion 008: CHECKs de USUARIO_ROL_UNIDAD ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'USUARIO_ROL_UNIDAD'
       AND CONSTRAINT_NAME IN ('FK_URU_COMBINACION','FK_URU_DOCUMENTO_FORMAL')
       AND CONSTRAINT_TYPE = 'R';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20035,
            'Validacion 008: FKs de USUARIO_ROL_UNIDAD ausentes');
    END IF;

    -- UX_URU_ABIERTAS debe existir
    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME = 'UX_URU_ABIERTAS';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20036,
            'Validacion 008: UX_URU_ABIERTAS ausente');
    END IF;

    -- USUARIO_ROL_UNIDAD_EVENTO
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES WHERE TABLE_NAME = 'USUARIO_ROL_UNIDAD_EVENTO';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20037,
            'Validacion 008: USUARIO_ROL_UNIDAD_EVENTO ausente');
    END IF;

    -- SUPLENCIA_FUNCIONAL
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES WHERE TABLE_NAME = 'SUPLENCIA_FUNCIONAL';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20038,
            'Validacion 008: SUPLENCIA_FUNCIONAL ausente');
    END IF;

    -- OPERACION_APROVISIONAMIENTO
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES WHERE TABLE_NAME = 'OPERACION_APROVISIONAMIENTO';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20039,
            'Validacion 008: OPERACION_APROVISIONAMIENTO ausente');
    END IF;

    -- Secuencias nuevas
    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_URU_EVENTO','SEQ_SUPLENCIA_FUNCIONAL',
            'SEQ_OPERACION_APROVISIONAMIENTO'
           );
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20040,
            'Validacion 008: secuencias de vigencia ausentes');
    END IF;

    -- Indices auxiliares del incremento
    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME IN (
            'UX_URU_ABIERTAS',
            'IDX_URU_COMBINACION_MATRIZ',
            'IDX_URUE_ASIGNACION','IDX_URUE_FECHA',
            'IDX_SF_TITULAR','IDX_SF_SUPLENTE',
            'IDX_OA_ESTADO','IDX_OA_USUARIO_OBJETIVO'
           )
       AND STATUS = 'VALID';
    IF v_total <> 8 THEN
        RAISE_APPLICATION_ERROR(-20041,
            'Validacion 008: indices auxiliares del incremento ausentes o invalidos');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 008 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 008_usuario_rol_unidad_vigencia completada correctamente.
