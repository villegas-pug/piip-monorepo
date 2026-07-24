-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 007 - Matriz funcional versionada
-- Archivo   : 007_matriz_funcional_versionada.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : seguridad
-- Dependencias: 003 (database/ddl/documentos/003_expediente_serie_version.sql),
--               002 (database/ddl/auditoria/002_auditoria_idempotencia.sql) y
--               001 (database/ddl/init/001_baseline_piip.sql)
-- Alcance   : Crea MATRIZ_FUNCIONAL_VERSION, MATRIZ_FUNCION y
--             MATRIZ_FUNCION_PERFIL_UNIDAD, con sus secuencias, PK/UK/FK/CHECK
--             e indices auxiliares. La cabecera de la matriz y cada funcion se
--             aprueban mediante un documento de aprobacion formal que reside
--             en DOCUMENTO (FK_DOCUMENTO_APROBACION). Las combinaciones
--             funcion-perfil-unidad concreta se aprueban y registran
--             independientemente, conservando el aprobador y el registrador
--             como columnas auditables.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: detener nuevas versiones; conservar combinaciones
--            historicas. Las modificaciones crean una nueva version; nunca
--            se sobrescribe una version confirmada.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [007] Validando precondiciones del baseline 001, 002 y 003...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones: 14 tablas y 13 secuencias (12 baseline + SEQ_SOLICITUD
--    _IDEMPOTENTE + SEQ_EXPEDIENTE_INSTITUCIONAL + SEQ_DOCUMENTO_SERIE).
-- ----------------------------------------------------------------------------
DECLARE
    v_total_tablas PLS_INTEGER;
    v_total_secuencias PLS_INTEGER;
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
            'Precondicion 007: se esperaban 16 tablas (001+002+003) y se encontraron '
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
            'Precondicion 007: se esperaban 13 secuencias (001+002+003) y se encontraron '
            || TO_CHAR(v_total_secuencias)
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de tablas nuevas que el incremento 007 introduce.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'MATRIZ_FUNCIONAL_VERSION','MATRIZ_FUNCION',
            'MATRIZ_FUNCION_PERFIL_UNIDAD'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 007: las tablas de matriz ya existen; el incremento ya fue aplicado'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_MATRIZ_VERSION','SEQ_MATRIZ_FUNCION','SEQ_MATRIZ_COMBINACION'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 007: las secuencias de matriz ya existen; el incremento ya fue aplicado'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de objetos futuros 008-024 que NO deben existir.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'USUARIO_ROL_UNIDAD_EVENTO','SUPLENCIA_FUNCIONAL','OPERACION_APROVISIONAMIENTO',
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
            -20014,
            'Precondicion 007: existen objetos futuros 004-024 ya creados'
        );
    END IF;
END;
/

PROMPT [007] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 007
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencias para las tres tablas nuevas.
-- ----------------------------------------------------------------------------
PROMPT [007.1] Creando secuencias SEQ_MATRIZ_VERSION, SEQ_MATRIZ_FUNCION y SEQ_MATRIZ_COMBINACION
CREATE SEQUENCE SEQ_MATRIZ_VERSION     START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_MATRIZ_FUNCION     START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_MATRIZ_COMBINACION START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Cabecera MATRIZ_FUNCIONAL_VERSION.
-- ----------------------------------------------------------------------------
PROMPT [007.2] Creando tabla MATRIZ_FUNCIONAL_VERSION
CREATE TABLE MATRIZ_FUNCIONAL_VERSION (
    ID_VERSION              NUMBER(10)                       NOT NULL,
    CODIGO_VERSION          VARCHAR2(30 CHAR)                NOT NULL,
    ID_VERSION_ANTERIOR     NUMBER(10),
    ID_DOCUMENTO_APROBACION NUMBER(12)                       NOT NULL,
    VIGENTE_DESDE           DATE                             NOT NULL,
    VIGENTE_HASTA           DATE,
    ACTIVA                  CHAR(1 CHAR) DEFAULT 'S'         NOT NULL,
    CREADO_POR              VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION          TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [007.3] PK, UK, FKs y CHECK de MATRIZ_FUNCIONAL_VERSION
ALTER TABLE MATRIZ_FUNCIONAL_VERSION
    ADD CONSTRAINT PK_MATRIZ_FUNCIONAL_VERSION PRIMARY KEY (ID_VERSION);

ALTER TABLE MATRIZ_FUNCIONAL_VERSION
    ADD CONSTRAINT UK_MFV_CODIGO UNIQUE (CODIGO_VERSION);

ALTER TABLE MATRIZ_FUNCIONAL_VERSION
    ADD CONSTRAINT FK_MFV_VERSION_ANTERIOR
    FOREIGN KEY (ID_VERSION_ANTERIOR) REFERENCES MATRIZ_FUNCIONAL_VERSION (ID_VERSION);

ALTER TABLE MATRIZ_FUNCIONAL_VERSION
    ADD CONSTRAINT FK_MFV_DOCUMENTO
    FOREIGN KEY (ID_DOCUMENTO_APROBACION) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE MATRIZ_FUNCIONAL_VERSION
    ADD CONSTRAINT CK_MFV_VIGENCIA
    CHECK (VIGENTE_HASTA IS NULL OR VIGENTE_HASTA >= VIGENTE_DESDE);

ALTER TABLE MATRIZ_FUNCIONAL_VERSION
    ADD CONSTRAINT CK_MFV_ACTIVA
    CHECK (ACTIVA IN ('S','N'));

-- ----------------------------------------------------------------------------
-- 3) Tabla MATRIZ_FUNCION. Pertenece a una version y aporta un codigo y una
--    descripcion. UK por version+codigo.
-- ----------------------------------------------------------------------------
PROMPT [007.4] Creando tabla MATRIZ_FUNCION
CREATE TABLE MATRIZ_FUNCION (
    ID_FUNCION   NUMBER(10)                       NOT NULL,
    ID_VERSION   NUMBER(10)                       NOT NULL,
    CODIGO       VARCHAR2(30 CHAR)                NOT NULL,
    DESCRIPCION  VARCHAR2(500 CHAR)               NOT NULL,
    ACTIVA       CHAR(1 CHAR) DEFAULT 'S'         NOT NULL
);

PROMPT [007.5] PK, UK, FK y CHECK de MATRIZ_FUNCION
ALTER TABLE MATRIZ_FUNCION
    ADD CONSTRAINT PK_MATRIZ_FUNCION PRIMARY KEY (ID_FUNCION);

ALTER TABLE MATRIZ_FUNCION
    ADD CONSTRAINT UK_MF_VERSION_CODIGO UNIQUE (ID_VERSION, CODIGO);

ALTER TABLE MATRIZ_FUNCION
    ADD CONSTRAINT FK_MF_VERSION
    FOREIGN KEY (ID_VERSION) REFERENCES MATRIZ_FUNCIONAL_VERSION (ID_VERSION);

ALTER TABLE MATRIZ_FUNCION
    ADD CONSTRAINT CK_MF_ACTIVA
    CHECK (ACTIVA IN ('S','N'));

-- ----------------------------------------------------------------------------
-- 4) Tabla MATRIZ_FUNCION_PERFIL_UNIDAD. La combinacion incluye el
--    aprobador y el registrador; el CHECK impide que coincidan.
-- ----------------------------------------------------------------------------
PROMPT [007.6] Creando tabla MATRIZ_FUNCION_PERFIL_UNIDAD
CREATE TABLE MATRIZ_FUNCION_PERFIL_UNIDAD (
    ID_COMBINACION           NUMBER(12)                      NOT NULL,
    ID_VERSION               NUMBER(10)                      NOT NULL,
    ID_FUNCION               NUMBER(10)                      NOT NULL,
    ID_ROL                   NUMBER(5)                       NOT NULL,
    ID_UNIDAD                NUMBER(10)                      NOT NULL,
    ID_APROBADOR             NUMBER(10)                      NOT NULL,
    ID_REGISTRADOR           NUMBER(10)                      NOT NULL,
    ID_DOCUMENTO_APROBACION  NUMBER(12)                      NOT NULL,
    VIGENTE_DESDE            DATE                            NOT NULL,
    VIGENTE_HASTA            DATE,
    ACTIVA                   CHAR(1 CHAR) DEFAULT 'S'        NOT NULL,
    CREADO_POR               VARCHAR2(100 CHAR)              NOT NULL,
    FECHA_CREACION           TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [007.7] PK, UK, FKs y CHECK de MATRIZ_FUNCION_PERFIL_UNIDAD
ALTER TABLE MATRIZ_FUNCION_PERFIL_UNIDAD
    ADD CONSTRAINT PK_MATRIZ_COMBINACION PRIMARY KEY (ID_COMBINACION);

ALTER TABLE MATRIZ_FUNCION_PERFIL_UNIDAD
    ADD CONSTRAINT UK_MFPU_VERSION_FUNCION_PERFIL_UNIDAD
    UNIQUE (ID_VERSION, ID_FUNCION, ID_ROL, ID_UNIDAD);

ALTER TABLE MATRIZ_FUNCION_PERFIL_UNIDAD
    ADD CONSTRAINT FK_MFPU_VERSION
    FOREIGN KEY (ID_VERSION) REFERENCES MATRIZ_FUNCIONAL_VERSION (ID_VERSION);

ALTER TABLE MATRIZ_FUNCION_PERFIL_UNIDAD
    ADD CONSTRAINT FK_MFPU_FUNCION
    FOREIGN KEY (ID_FUNCION) REFERENCES MATRIZ_FUNCION (ID_FUNCION);

ALTER TABLE MATRIZ_FUNCION_PERFIL_UNIDAD
    ADD CONSTRAINT FK_MFPU_ROL
    FOREIGN KEY (ID_ROL) REFERENCES ROL (ID_ROL);

ALTER TABLE MATRIZ_FUNCION_PERFIL_UNIDAD
    ADD CONSTRAINT FK_MFPU_UNIDAD
    FOREIGN KEY (ID_UNIDAD) REFERENCES UNIDAD_EJECUTORA (ID_UNIDAD);

ALTER TABLE MATRIZ_FUNCION_PERFIL_UNIDAD
    ADD CONSTRAINT FK_MFPU_APROBADOR
    FOREIGN KEY (ID_APROBADOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE MATRIZ_FUNCION_PERFIL_UNIDAD
    ADD CONSTRAINT FK_MFPU_REGISTRADOR
    FOREIGN KEY (ID_REGISTRADOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE MATRIZ_FUNCION_PERFIL_UNIDAD
    ADD CONSTRAINT FK_MFPU_DOCUMENTO
    FOREIGN KEY (ID_DOCUMENTO_APROBACION) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE MATRIZ_FUNCION_PERFIL_UNIDAD
    ADD CONSTRAINT CK_MFPU_APROBADOR_DISTINTO_REGISTRADOR
    CHECK (ID_APROBADOR <> ID_REGISTRADOR);

ALTER TABLE MATRIZ_FUNCION_PERFIL_UNIDAD
    ADD CONSTRAINT CK_MFPU_VIGENCIA
    CHECK (VIGENTE_HASTA IS NULL OR VIGENTE_HASTA >= VIGENTE_DESDE);

ALTER TABLE MATRIZ_FUNCION_PERFIL_UNIDAD
    ADD CONSTRAINT CK_MFPU_ACTIVA
    CHECK (ACTIVA IN ('S','N'));

PROMPT [007.8] Creando indices auxiliares IDX_MF_VERSION e IDX_MFPU_COMBINACION
CREATE INDEX IDX_MF_VERSION      ON MATRIZ_FUNCION (ID_VERSION);
CREATE INDEX IDX_MFPU_FUNCION    ON MATRIZ_FUNCION_PERFIL_UNIDAD (ID_FUNCION);
CREATE INDEX IDX_MFPU_UNIDAD     ON MATRIZ_FUNCION_PERFIL_UNIDAD (ID_UNIDAD);
CREATE INDEX IDX_MFPU_DOCUMENTO  ON MATRIZ_FUNCION_PERFIL_UNIDAD (ID_DOCUMENTO_APROBACION);

-- ============================================================================
-- Validacion final del script 007
-- ============================================================================
PROMPT [007.9] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    -- Tablas nuevas
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'MATRIZ_FUNCIONAL_VERSION','MATRIZ_FUNCION',
            'MATRIZ_FUNCION_PERFIL_UNIDAD'
           );
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20030,
            'Validacion 007: tablas de matriz ausentes');
    END IF;

    -- Secuencias nuevas
    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_MATRIZ_VERSION','SEQ_MATRIZ_FUNCION','SEQ_MATRIZ_COMBINACION'
           );
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 007: secuencias de matriz ausentes');
    END IF;

    -- PKs
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME IN (
            'MATRIZ_FUNCIONAL_VERSION','MATRIZ_FUNCION',
            'MATRIZ_FUNCION_PERFIL_UNIDAD'
           )
       AND CONSTRAINT_NAME IN (
            'PK_MATRIZ_FUNCIONAL_VERSION',
            'PK_MATRIZ_FUNCION',
            'PK_MATRIZ_COMBINACION'
           )
       AND CONSTRAINT_TYPE = 'P';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20032,
            'Validacion 007: PKs de matriz ausentes');
    END IF;

    -- UKs
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME IN (
            'MATRIZ_FUNCIONAL_VERSION','MATRIZ_FUNCION',
            'MATRIZ_FUNCION_PERFIL_UNIDAD'
           )
       AND CONSTRAINT_NAME IN (
            'UK_MFV_CODIGO','UK_MF_VERSION_CODIGO',
            'UK_MFPU_VERSION_FUNCION_PERFIL_UNIDAD'
           )
       AND CONSTRAINT_TYPE = 'U';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20033,
            'Validacion 007: UKs de matriz ausentes');
    END IF;

    -- FKs
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'MATRIZ_FUNCIONAL_VERSION'
       AND CONSTRAINT_NAME IN ('FK_MFV_VERSION_ANTERIOR','FK_MFV_DOCUMENTO')
       AND CONSTRAINT_TYPE = 'R';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20034,
            'Validacion 007: FKs de MATRIZ_FUNCIONAL_VERSION ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'MATRIZ_FUNCION'
       AND CONSTRAINT_NAME = 'FK_MF_VERSION'
       AND CONSTRAINT_TYPE = 'R';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20035,
            'Validacion 007: FK_MF_VERSION ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'MATRIZ_FUNCION_PERFIL_UNIDAD'
       AND CONSTRAINT_NAME IN (
            'FK_MFPU_VERSION','FK_MFPU_FUNCION','FK_MFPU_ROL','FK_MFPU_UNIDAD',
            'FK_MFPU_APROBADOR','FK_MFPU_REGISTRADOR','FK_MFPU_DOCUMENTO'
           )
       AND CONSTRAINT_TYPE = 'R';
    IF v_total <> 7 THEN
        RAISE_APPLICATION_ERROR(-20036,
            'Validacion 007: FKs de MATRIZ_FUNCION_PERFIL_UNIDAD incompletas ('
            || TO_CHAR(v_total) || '/7)');
    END IF;

    -- CHECKs
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'MATRIZ_FUNCIONAL_VERSION'
       AND CONSTRAINT_NAME IN ('CK_MFV_VIGENCIA','CK_MFV_ACTIVA')
       AND CONSTRAINT_TYPE = 'C';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20037,
            'Validacion 007: CHECKs de MATRIZ_FUNCIONAL_VERSION ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'MATRIZ_FUNCION'
       AND CONSTRAINT_NAME = 'CK_MF_ACTIVA'
       AND CONSTRAINT_TYPE = 'C';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20038,
            'Validacion 007: CK_MF_ACTIVA ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'MATRIZ_FUNCION_PERFIL_UNIDAD'
       AND CONSTRAINT_NAME IN (
            'CK_MFPU_APROBADOR_DISTINTO_REGISTRADOR',
            'CK_MFPU_VIGENCIA','CK_MFPU_ACTIVA'
           )
       AND CONSTRAINT_TYPE = 'C';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20039,
            'Validacion 007: CHECKs de MATRIZ_FUNCION_PERFIL_UNIDAD ausentes');
    END IF;

    -- Indices auxiliares
    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME IN (
            'IDX_MF_VERSION','IDX_MFPU_FUNCION','IDX_MFPU_UNIDAD','IDX_MFPU_DOCUMENTO'
           )
       AND STATUS = 'VALID';
    IF v_total <> 4 THEN
        RAISE_APPLICATION_ERROR(-20040,
            'Validacion 007: indices auxiliares del incremento ausentes o invalidos');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 007 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 007_matriz_funcional_versionada completada correctamente.
