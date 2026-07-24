-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 006 - Catalogos versionados de
-- Actividad POI con cabeceras e items (ciclo independiente del PEI)
-- Archivo   : 006_actividad_poi_versionada.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : organizacion
-- Dependencias: 003 (database/ddl/documentos/003_expediente_serie_version.sql),
--               003.1, 003.2, 002 (002_auditoria_idempotencia.sql) y 001
--               (001_baseline_piip.sql).
-- Alcance   : Crea CAT_ACTIVIDAD_POI_VERSION como cabecera de version y
--             CAT_ACTIVIDAD_POI como item de la version, con sus secuencias,
--             PK/UK/FK/CHECK. La UK `UK_AP_VERSION_CODIGO` aporta el indice
--             unico canonico para `(ID_VERSION, CODIGO)`; no se crea un
--             indice auxiliar redundante. Paralelo a 005 con
--             ciclo independiente; los codigos de version POI no se cruzan
--             con PEI. La columna textual PROYECTO.ACTIVIDAD_POI queda
--             bloqueada: ACTIVIDAD_POI_ID solo se rellenara con el id
--             canonico cuando 009 integre la migracion.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: inactivar versiones no usadas; preservar
--            referencias historicas. Ninguna actividad creada se elimina.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [006] Validando precondiciones de 001, 002 y 003+003.1+003.2...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones del baseline 001 y de los incrementos 002, 003.
-- ----------------------------------------------------------------------------
DECLARE
    v_tablas_001_002_003 PLS_INTEGER;
    v_tablas_003_1_003_2 PLS_INTEGER;
    v_secuencias_001_002_003 PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_tablas_001_002_003
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'UNIDAD_EJECUTORA','USUARIO','ROL','USUARIO_ROL_UNIDAD',
            'PROYECTO','PROYECTO_UNIDAD_ORGANICA','TRANSICION_PERMITIDA',
            'TIPO_DOCUMENTO','DOCUMENTO','TRANSICION_ESTADO',
            'SECUENCIA_CODIGO','AUDITORIA_ACCESO','AUDITORIA_EVENTO',
            'SOLICITUD_IDEMPOTENTE',
            'EXPEDIENTE_INSTITUCIONAL','DOCUMENTO_SERIE'
           );
    IF v_tablas_001_002_003 <> 16 THEN
        RAISE_APPLICATION_ERROR(
            -20010,
            'Precondicion 006: se esperaban 16 tablas (001+002+003) y se encontraron '
            || TO_CHAR(v_tablas_001_002_003)
        );
    END IF;

    SELECT COUNT(*) INTO v_tablas_003_1_003_2
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND CONSTRAINT_NAME = 'CK_TD_ESTADO_CONTEXTO'
       AND CONSTRAINT_TYPE = 'C'
       AND STATUS = 'ENABLED';
    IF v_tablas_003_1_003_2 <> 1 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Precondicion 006: la huella 003.1 (CK_TD_ESTADO_CONTEXTO) no esta vigente'
        );
    END IF;

    SELECT COUNT(*) INTO v_tablas_003_1_003_2
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND CONSTRAINT_NAME = 'FK_DOC_TIPO'
       AND CONSTRAINT_TYPE = 'R'
       AND STATUS = 'ENABLED';
    IF v_tablas_003_1_003_2 <> 1 THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 006: la huella 003.2 (FK_DOC_TIPO) no esta vigente'
        );
    END IF;

    SELECT COUNT(*) INTO v_secuencias_001_002_003
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_UNIDAD_EJECUTORA','SEQ_USUARIO','SEQ_USUARIO_ROL_UNIDAD',
            'SEQ_PROYECTO','SEQ_PROYECTO_UO','SEQ_DOCUMENTO',
            'SEQ_TRANSICION_ESTADO','SEQ_SECUENCIA_CODIGO',
            'SEQ_AUDITORIA_ACCESO','SEQ_AUDITORIA_EVENTO',
            'SEQ_SOLICITUD_IDEMPOTENTE',
            'SEQ_EXPEDIENTE_INSTITUCIONAL','SEQ_DOCUMENTO_SERIE'
           );
    IF v_secuencias_001_002_003 <> 13 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 006: se esperaban 13 secuencias (001+002+003) y se encontraron '
            || TO_CHAR(v_secuencias_001_002_003)
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de que las tablas/secuencias del propio 006 NO existen.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'CAT_ACTIVIDAD_POI_VERSION',
            'CAT_ACTIVIDAD_POI'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 006: las tablas de catalogo POI ya existen; el incremento ya fue aplicado'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_ACTIVIDAD_POI_VERSION',
            'SEQ_ACTIVIDAD_POI'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20015,
            'Precondicion 006: las secuencias del incremento ya existen; revise el estado'
        );
    END IF;

    -- PROYECTO.ACTIVIDAD_POI_ID no debe existir (reservado para 009)
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'PROYECTO'
       AND COLUMN_NAME = 'ACTIVIDAD_POI_ID';
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20016,
            'Precondicion 006: PROYECTO.ACTIVIDAD_POI_ID ya existe; el incremento 009 debe coordinarse'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de objetos futuros estrictamente sucesores de 006.
--    006 es paralelo a 004, 005 y 007 y predecesor de 009; solo se rechazan
--    objetos de sucesores estrictos (010-017) y diferidos (018-021).
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
            'MEDICION_MUESTRA','MATRIZ_META_RECORRIDO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20017,
            'Precondicion 006: existen objetos futuros 009-017 o 018-021 ya creados'
        );
    END IF;
END;
/

PROMPT [006] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 006
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencias del incremento.
-- ----------------------------------------------------------------------------
PROMPT [006.1] Creando secuencias del incremento
CREATE SEQUENCE SEQ_ACTIVIDAD_POI_VERSION
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_ACTIVIDAD_POI
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Tabla CAT_ACTIVIDAD_POI_VERSION (cabecera de version).
-- ----------------------------------------------------------------------------
PROMPT [006.2] Creando tabla CAT_ACTIVIDAD_POI_VERSION
CREATE TABLE CAT_ACTIVIDAD_POI_VERSION (
    ID_VERSION              NUMBER(10)                      NOT NULL,
    CODIGO_VERSION          VARCHAR2(30 CHAR)               NOT NULL,
    ID_VERSION_ANTERIOR     NUMBER(10),
    ID_DOCUMENTO_APROBACION NUMBER(12)                      NOT NULL,
    OFICINA_APROBADORA      VARCHAR2(200 CHAR)               NOT NULL,
    VIGENTE_DESDE           DATE                            NOT NULL,
    VIGENTE_HASTA           DATE,
    ACTIVA                  CHAR(1 CHAR) DEFAULT 'S'        NOT NULL,
    CREADO_POR              VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION          TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [006.3] PK, UK, FKs y CHECKs de CAT_ACTIVIDAD_POI_VERSION
ALTER TABLE CAT_ACTIVIDAD_POI_VERSION
    ADD CONSTRAINT PK_CAT_ACTIVIDAD_POI_VERSION PRIMARY KEY (ID_VERSION);

ALTER TABLE CAT_ACTIVIDAD_POI_VERSION
    ADD CONSTRAINT UK_APV_CODIGO UNIQUE (CODIGO_VERSION);

ALTER TABLE CAT_ACTIVIDAD_POI_VERSION
    ADD CONSTRAINT FK_APV_VERSION_ANTERIOR
    FOREIGN KEY (ID_VERSION_ANTERIOR) REFERENCES CAT_ACTIVIDAD_POI_VERSION (ID_VERSION);

ALTER TABLE CAT_ACTIVIDAD_POI_VERSION
    ADD CONSTRAINT FK_APV_DOCUMENTO
    FOREIGN KEY (ID_DOCUMENTO_APROBACION) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE CAT_ACTIVIDAD_POI_VERSION
    ADD CONSTRAINT CK_APV_VIGENCIA
    CHECK (VIGENTE_HASTA IS NULL OR VIGENTE_HASTA >= VIGENTE_DESDE);

ALTER TABLE CAT_ACTIVIDAD_POI_VERSION
    ADD CONSTRAINT CK_APV_ACTIVA
    CHECK (ACTIVA IN ('S','N'));

-- ----------------------------------------------------------------------------
-- 3) Tabla CAT_ACTIVIDAD_POI (item por version).
-- ----------------------------------------------------------------------------
PROMPT [006.4] Creando tabla CAT_ACTIVIDAD_POI
CREATE TABLE CAT_ACTIVIDAD_POI (
    ID_ACTIVIDAD    NUMBER(10)                      NOT NULL,
    ID_VERSION      NUMBER(10)                      NOT NULL,
    CODIGO          VARCHAR2(30 CHAR)               NOT NULL,
    DESCRIPCION     VARCHAR2(500 CHAR)               NOT NULL,
    VIGENTE_DESDE   DATE                            NOT NULL,
    VIGENTE_HASTA   DATE,
    ACTIVO          CHAR(1 CHAR) DEFAULT 'S'        NOT NULL
);

PROMPT [006.5] PK, UK, FK y CHECKs de CAT_ACTIVIDAD_POI
ALTER TABLE CAT_ACTIVIDAD_POI
    ADD CONSTRAINT PK_CAT_ACTIVIDAD_POI PRIMARY KEY (ID_ACTIVIDAD);

ALTER TABLE CAT_ACTIVIDAD_POI
    ADD CONSTRAINT UK_AP_VERSION_CODIGO UNIQUE (ID_VERSION, CODIGO);

ALTER TABLE CAT_ACTIVIDAD_POI
    ADD CONSTRAINT FK_AP_VERSION
    FOREIGN KEY (ID_VERSION) REFERENCES CAT_ACTIVIDAD_POI_VERSION (ID_VERSION);

ALTER TABLE CAT_ACTIVIDAD_POI
    ADD CONSTRAINT CK_AP_VIGENCIA
    CHECK (VIGENTE_HASTA IS NULL OR VIGENTE_HASTA >= VIGENTE_DESDE);

ALTER TABLE CAT_ACTIVIDAD_POI
    ADD CONSTRAINT CK_AP_ACTIVO
    CHECK (ACTIVO IN ('S','N'));

-- ----------------------------------------------------------------------------
-- 4) Indice unico canonico de UK_AP_VERSION_CODIGO.
-- Oracle crea y mantiene el indice de respaldo de la UK; crear otro indice
-- con la misma lista `(ID_VERSION, CODIGO)` provocaria ORA-01408.
-- ----------------------------------------------------------------------------
PROMPT [006.6] UK_AP_VERSION_CODIGO aporta el indice unico canonico; no se crea indice redundante

-- ============================================================================
-- Validacion final del script 006
-- ============================================================================
PROMPT [006.7] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'CAT_ACTIVIDAD_POI_VERSION',
            'CAT_ACTIVIDAD_POI'
           );
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20030,
            'Validacion 006: tablas de catalogo POI ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_ACTIVIDAD_POI_VERSION',
            'SEQ_ACTIVIDAD_POI'
           )
       AND INCREMENT_BY = 1
       AND CACHE_SIZE = 0
       AND CYCLE_FLAG = 'N';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 006: secuencias del incremento ausentes o incompatibles');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME IN ('CAT_ACTIVIDAD_POI_VERSION','CAT_ACTIVIDAD_POI')
       AND CONSTRAINT_NAME IN ('PK_CAT_ACTIVIDAD_POI_VERSION','PK_CAT_ACTIVIDAD_POI')
       AND CONSTRAINT_TYPE = 'P'
       AND STATUS = 'ENABLED';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20032,
            'Validacion 006: PKs de catalogos POI ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE CONSTRAINT_NAME IN ('UK_APV_CODIGO','UK_AP_VERSION_CODIGO')
       AND CONSTRAINT_TYPE = 'U';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20033,
            'Validacion 006: UKs de catalogos POI ausentes');
    END IF;

    -- Indice unico de respaldo de UK_AP_VERSION_CODIGO. El nombre fisico se
    -- deriva de USER_CONSTRAINTS.INDEX_NAME y no se presupone.
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS c
      JOIN USER_INDEXES i
        ON i.INDEX_NAME = c.INDEX_NAME
     WHERE c.TABLE_NAME = 'CAT_ACTIVIDAD_POI'
       AND c.CONSTRAINT_NAME = 'UK_AP_VERSION_CODIGO'
       AND c.CONSTRAINT_TYPE = 'U'
       AND c.STATUS = 'ENABLED'
       AND i.STATUS = 'VALID'
       AND i.UNIQUENESS = 'UNIQUE';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20034,
            'Validacion 006: indice de respaldo de UK_AP_VERSION_CODIGO ausente, invalido o no unico');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS c
      JOIN USER_IND_COLUMNS ic
        ON ic.INDEX_NAME = c.INDEX_NAME
     WHERE c.TABLE_NAME = 'CAT_ACTIVIDAD_POI'
       AND c.CONSTRAINT_NAME = 'UK_AP_VERSION_CODIGO'
       AND c.CONSTRAINT_TYPE = 'U'
       AND ((ic.COLUMN_NAME = 'ID_VERSION' AND ic.COLUMN_POSITION = 1)
         OR (ic.COLUMN_NAME = 'CODIGO' AND ic.COLUMN_POSITION = 2));
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20034,
            'Validacion 006: indice de respaldo de UK_AP_VERSION_CODIGO no conserva ID_VERSION, CODIGO en ese orden');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 006 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 006_actividad_poi_versionada completada correctamente.
