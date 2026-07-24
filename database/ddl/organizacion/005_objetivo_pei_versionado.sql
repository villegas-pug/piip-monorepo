-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 005 - Catalogos versionados de
-- Objetivo PEI con cabeceras e items
-- Archivo   : 005_objetivo_pei_versionado.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : organizacion
-- Dependencias: 003 (database/ddl/documentos/003_expediente_serie_version.sql),
--               003.1, 003.2, 002 (002_auditoria_idempotencia.sql) y 001
--               (001_baseline_piip.sql).
-- Alcance   : Crea CAT_OBJETIVO_PEI_VERSION como cabecera de version y
--             CAT_OBJETIVO_PEI como item de la version, con sus secuencias,
--             PK/UK/FK/CHECK. La UK `UK_OP_VERSION_CODIGO` aporta el indice
--             unico canonico para `(ID_VERSION, CODIGO)`; no se crea un
--             indice auxiliar redundante. La columna textual
--             PROYECTO.OBJETIVO_PEI queda bloqueada: hasta que se apruebe
--             formalmente el corte, se conserva su valor legado y
--             OBJETIVO_PEI_ID solo se rellenara con el id canonico cuando
--             009 integre la migracion.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: inactivar versiones no usadas; preservar
--            referencias historicas. Ningun objetivo creado se elimina.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [005] Validando precondiciones de 001, 002 y 003+003.1+003.2...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones del baseline 001 y de los incrementos 002, 003.
-- ----------------------------------------------------------------------------
DECLARE
    v_tablas_001_002_003 PLS_INTEGER;
    v_tablas_003_1_003_2 PLS_INTEGER;
    v_secuencias_001_002_003 PLS_INTEGER;
    v_documento_columnas PLS_INTEGER;
BEGIN
    -- 13 baseline + 1 (002) + 2 (003) = 16 tablas
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
            'Precondicion 005: se esperaban 16 tablas (001+002+003) y se encontraron '
            || TO_CHAR(v_tablas_001_002_003)
        );
    END IF;

    -- 003.1 y 003.2 son correcciones forward-only; exigimos sus huellas en
    -- TIPO_DOCUMENTO (ESTADO_ASOCIADO nullable por 003.1; CONTEXTO sigue
    -- NOT NULL como discriminante) y DOCUMENTO (FK a TIPO_DOCUMENTO).
    SELECT COUNT(*) INTO v_tablas_003_1_003_2
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND CONSTRAINT_NAME = 'CK_TD_ESTADO_CONTEXTO'
       AND CONSTRAINT_TYPE = 'C'
       AND STATUS = 'ENABLED';
    IF v_tablas_003_1_003_2 <> 1 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Precondicion 005: la huella 003.1 (CK_TD_ESTADO_CONTEXTO) no esta vigente'
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
            'Precondicion 005: la huella 003.2 (FK_DOC_TIPO) no esta vigente'
        );
    END IF;

    -- 11 secuencias: 10 baseline + SEQ_SOLICITUD_IDEMPOTENTE
    --                + SEQ_EXPEDIENTE_INSTITUCIONAL + SEQ_DOCUMENTO_SERIE = 13
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
            'Precondicion 005: se esperaban 13 secuencias (001+002+003) y se encontraron '
            || TO_CHAR(v_secuencias_001_002_003)
        );
    END IF;

    -- DOCUMENTO.ID_TIPO_DOC debe existir (huella 003)
    SELECT COUNT(*) INTO v_documento_columnas
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND COLUMN_NAME = 'ID_TIPO_DOC'
       AND DATA_TYPE = 'NUMBER';
    IF v_documento_columnas <> 1 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 005: DOCUMENTO.ID_TIPO_DOC ausente (huella 003)'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de que las tablas/secuencias del propio 005 NO existen.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'CAT_OBJETIVO_PEI_VERSION',
            'CAT_OBJETIVO_PEI'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20015,
            'Precondicion 005: las tablas de catalogo PEI ya existen; el incremento ya fue aplicado'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_OBJETIVO_PEI_VERSION',
            'SEQ_OBJETIVO_PEI'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20016,
            'Precondicion 005: las secuencias del incremento ya existen; revise el estado'
        );
    END IF;

    -- PROYECTO.OBJETIVO_PEI_ID no debe existir (se reservara para 009)
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'PROYECTO'
       AND COLUMN_NAME = 'OBJETIVO_PEI_ID';
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20017,
            'Precondicion 005: PROYECTO.OBJETIVO_PEI_ID ya existe; el incremento 009 debe coordinarse'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de objetos futuros estrictamente sucesores de 005.
--    005 es paralelo a 004, 006 y 007 y predecesor de 009; solo se rechazan
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
            -20018,
            'Precondicion 005: existen objetos futuros 009-017 o 018-021 ya creados'
        );
    END IF;
END;
/

PROMPT [005] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 005
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencias del incremento.
-- ----------------------------------------------------------------------------
PROMPT [005.1] Creando secuencias del incremento
CREATE SEQUENCE SEQ_OBJETIVO_PEI_VERSION
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_OBJETIVO_PEI
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Tabla CAT_OBJETIVO_PEI_VERSION (cabecera de version).
-- ----------------------------------------------------------------------------
PROMPT [005.2] Creando tabla CAT_OBJETIVO_PEI_VERSION
CREATE TABLE CAT_OBJETIVO_PEI_VERSION (
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

PROMPT [005.3] PK, UK, FKs y CHECKs de CAT_OBJETIVO_PEI_VERSION
ALTER TABLE CAT_OBJETIVO_PEI_VERSION
    ADD CONSTRAINT PK_CAT_OBJETIVO_PEI_VERSION PRIMARY KEY (ID_VERSION);

ALTER TABLE CAT_OBJETIVO_PEI_VERSION
    ADD CONSTRAINT UK_OPV_CODIGO UNIQUE (CODIGO_VERSION);

ALTER TABLE CAT_OBJETIVO_PEI_VERSION
    ADD CONSTRAINT FK_OPV_VERSION_ANTERIOR
    FOREIGN KEY (ID_VERSION_ANTERIOR) REFERENCES CAT_OBJETIVO_PEI_VERSION (ID_VERSION);

ALTER TABLE CAT_OBJETIVO_PEI_VERSION
    ADD CONSTRAINT FK_OPV_DOCUMENTO
    FOREIGN KEY (ID_DOCUMENTO_APROBACION) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE CAT_OBJETIVO_PEI_VERSION
    ADD CONSTRAINT CK_OPV_VIGENCIA
    CHECK (VIGENTE_HASTA IS NULL OR VIGENTE_HASTA >= VIGENTE_DESDE);

ALTER TABLE CAT_OBJETIVO_PEI_VERSION
    ADD CONSTRAINT CK_OPV_ACTIVA
    CHECK (ACTIVA IN ('S','N'));

-- ----------------------------------------------------------------------------
-- 3) Tabla CAT_OBJETIVO_PEI (item por version).
-- ----------------------------------------------------------------------------
PROMPT [005.4] Creando tabla CAT_OBJETIVO_PEI
CREATE TABLE CAT_OBJETIVO_PEI (
    ID_OBJETIVO     NUMBER(10)                      NOT NULL,
    ID_VERSION      NUMBER(10)                      NOT NULL,
    CODIGO          VARCHAR2(30 CHAR)               NOT NULL,
    DESCRIPCION     VARCHAR2(500 CHAR)               NOT NULL,
    VIGENTE_DESDE   DATE                            NOT NULL,
    VIGENTE_HASTA   DATE,
    ACTIVO          CHAR(1 CHAR) DEFAULT 'S'        NOT NULL
);

PROMPT [005.5] PK, UK, FK y CHECKs de CAT_OBJETIVO_PEI
ALTER TABLE CAT_OBJETIVO_PEI
    ADD CONSTRAINT PK_CAT_OBJETIVO_PEI PRIMARY KEY (ID_OBJETIVO);

ALTER TABLE CAT_OBJETIVO_PEI
    ADD CONSTRAINT UK_OP_VERSION_CODIGO UNIQUE (ID_VERSION, CODIGO);

ALTER TABLE CAT_OBJETIVO_PEI
    ADD CONSTRAINT FK_OP_VERSION
    FOREIGN KEY (ID_VERSION) REFERENCES CAT_OBJETIVO_PEI_VERSION (ID_VERSION);

ALTER TABLE CAT_OBJETIVO_PEI
    ADD CONSTRAINT CK_OP_VIGENCIA
    CHECK (VIGENTE_HASTA IS NULL OR VIGENTE_HASTA >= VIGENTE_DESDE);

ALTER TABLE CAT_OBJETIVO_PEI
    ADD CONSTRAINT CK_OP_ACTIVO
    CHECK (ACTIVO IN ('S','N'));

-- ----------------------------------------------------------------------------
-- 4) Indice unico canonico de UK_OP_VERSION_CODIGO.
-- Oracle crea y mantiene el indice de respaldo de la UK; crear otro indice
-- con la misma lista `(ID_VERSION, CODIGO)` provocaria ORA-01408.
-- ----------------------------------------------------------------------------
PROMPT [005.6] UK_OP_VERSION_CODIGO aporta el indice unico canonico; no se crea indice redundante

-- ============================================================================
-- Validacion final del script 005
-- ============================================================================
PROMPT [005.7] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    -- Tablas nuevas
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'CAT_OBJETIVO_PEI_VERSION',
            'CAT_OBJETIVO_PEI'
           );
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20030,
            'Validacion 005: tablas de catalogo PEI ausentes');
    END IF;

    -- Secuencias nuevas
    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_OBJETIVO_PEI_VERSION',
            'SEQ_OBJETIVO_PEI'
           )
       AND INCREMENT_BY = 1
       AND CACHE_SIZE = 0
       AND CYCLE_FLAG = 'N';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 005: secuencias del incremento ausentes o incompatibles');
    END IF;

    -- PKs
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME IN ('CAT_OBJETIVO_PEI_VERSION','CAT_OBJETIVO_PEI')
       AND CONSTRAINT_NAME IN ('PK_CAT_OBJETIVO_PEI_VERSION','PK_CAT_OBJETIVO_PEI')
       AND CONSTRAINT_TYPE = 'P'
       AND STATUS = 'ENABLED';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20032,
            'Validacion 005: PKs de catalogos PEI ausentes');
    END IF;

    -- UKs
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE CONSTRAINT_NAME IN ('UK_OPV_CODIGO','UK_OP_VERSION_CODIGO')
       AND CONSTRAINT_TYPE = 'U';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20033,
            'Validacion 005: UKs de catalogos PEI ausentes');
    END IF;

    -- Indice unico de respaldo de UK_OP_VERSION_CODIGO. El nombre fisico se
    -- deriva de USER_CONSTRAINTS.INDEX_NAME y no se presupone.
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS c
      JOIN USER_INDEXES i
        ON i.INDEX_NAME = c.INDEX_NAME
     WHERE c.TABLE_NAME = 'CAT_OBJETIVO_PEI'
       AND c.CONSTRAINT_NAME = 'UK_OP_VERSION_CODIGO'
       AND c.CONSTRAINT_TYPE = 'U'
       AND c.STATUS = 'ENABLED'
       AND i.STATUS = 'VALID'
       AND i.UNIQUENESS = 'UNIQUE';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20034,
            'Validacion 005: indice de respaldo de UK_OP_VERSION_CODIGO ausente, invalido o no unico');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS c
      JOIN USER_IND_COLUMNS ic
        ON ic.INDEX_NAME = c.INDEX_NAME
     WHERE c.TABLE_NAME = 'CAT_OBJETIVO_PEI'
       AND c.CONSTRAINT_NAME = 'UK_OP_VERSION_CODIGO'
       AND c.CONSTRAINT_TYPE = 'U'
       AND ((ic.COLUMN_NAME = 'ID_VERSION' AND ic.COLUMN_POSITION = 1)
         OR (ic.COLUMN_NAME = 'CODIGO' AND ic.COLUMN_POSITION = 2));
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20034,
            'Validacion 005: indice de respaldo de UK_OP_VERSION_CODIGO no conserva ID_VERSION, CODIGO en ese orden');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 005 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 005_objetivo_pei_versionado completada correctamente.
