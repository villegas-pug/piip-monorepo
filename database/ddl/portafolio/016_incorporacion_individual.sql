-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 016 - Incorporacion individual de
-- registros con conflictos y trazabilidad append-only
-- Archivo   : 016_incorporacion_individual.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : portafolio
-- Dependencias: 003+003.1+003.2, 010 y 012, ademas de 002, 005, 006, 008+008.1,
--               009 y 001.
-- Alcance   : Crea INCORPORACION_REGISTRO, INCORPORACION_CAMBIO y
--             INCORPORACION_CONFLICTO. La UK por HASH_ORIGINAL + responsable
--             + fuente garantiza idempotencia estructural; INCORPORACION_CAMBIO
--             es append-only y nunca modifica la fila de la incorporacion.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: mantener expedientes PENDIENTE; no borrar
--            evidencia. Los conflictos resueltos no se eliminan.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [016] Validando precondiciones de 001, 002, 003+003.1+003.2, 005, 006, 008+008.1, 009, 010 y 012...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones acumuladas.
-- ----------------------------------------------------------------------------
DECLARE
    v_tablas_precedentes PLS_INTEGER;
    v_secuencias_precedentes PLS_INTEGER;
BEGIN
    -- 23 tablas base + 1 (010) + 3 (012) = 27
    SELECT COUNT(*) INTO v_tablas_precedentes
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'UNIDAD_EJECUTORA','USUARIO','ROL','USUARIO_ROL_UNIDAD',
            'PROYECTO','PROYECTO_UNIDAD_ORGANICA','TRANSICION_PERMITIDA',
            'TIPO_DOCUMENTO','DOCUMENTO','TRANSICION_ESTADO',
            'SECUENCIA_CODIGO','AUDITORIA_ACCESO','AUDITORIA_EVENTO',
            'SOLICITUD_IDEMPOTENTE',
            'EXPEDIENTE_INSTITUCIONAL','DOCUMENTO_SERIE',
            'CAT_OBJETIVO_PEI_VERSION','CAT_OBJETIVO_PEI',
            'CAT_ACTIVIDAD_POI_VERSION','CAT_ACTIVIDAD_POI',
            'USUARIO_ROL_UNIDAD_EVENTO','SUPLENCIA_FUNCIONAL',
            'OPERACION_APROVISIONAMIENTO',
            'INICIATIVA_PROYECTO',
            'PARTICIPANTE_PERSONA','PROYECTO_PARTICIPANTE_PERSONA',
            'PROYECTO_PARTICIPANTE_UNIDAD'
           );
    IF v_tablas_precedentes <> 27 THEN
        RAISE_APPLICATION_ERROR(
            -20010,
            'Precondicion 016: se esperaban 27 tablas previas y se encontraron '
            || TO_CHAR(v_tablas_precedentes)
        );
    END IF;

    -- 20 secuencias base + 1 (010) + 3 (012) = 24
    SELECT COUNT(*) INTO v_secuencias_precedentes
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_UNIDAD_EJECUTORA','SEQ_USUARIO','SEQ_USUARIO_ROL_UNIDAD',
            'SEQ_PROYECTO','SEQ_PROYECTO_UO','SEQ_DOCUMENTO',
            'SEQ_TRANSICION_ESTADO','SEQ_SECUENCIA_CODIGO',
            'SEQ_AUDITORIA_ACCESO','SEQ_AUDITORIA_EVENTO',
            'SEQ_SOLICITUD_IDEMPOTENTE',
            'SEQ_EXPEDIENTE_INSTITUCIONAL','SEQ_DOCUMENTO_SERIE',
            'SEQ_OBJETIVO_PEI_VERSION','SEQ_OBJETIVO_PEI',
            'SEQ_ACTIVIDAD_POI_VERSION','SEQ_ACTIVIDAD_POI',
            'SEQ_URU_EVENTO','SEQ_SUPLENCIA_FUNCIONAL',
            'SEQ_OPERACION_APROVISIONAMIENTO',
            'SEQ_INICIATIVA_PROYECTO',
            'SEQ_PARTICIPANTE_PERSONA','SEQ_PROY_PART_PERSONA',
            'SEQ_PROY_PART_UNIDAD'
           );
    IF v_secuencias_precedentes <> 24 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Precondicion 016: se esperaban 24 secuencias previas y se encontraron '
            || TO_CHAR(v_secuencias_precedentes)
        );
    END IF;

    -- INICIATIVA_PROYECTO con PK vigente (huella 010)
    SELECT COUNT(*) INTO v_tablas_precedentes
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'INICIATIVA_PROYECTO'
       AND CONSTRAINT_NAME = 'PK_INICIATIVA_PROYECTO'
       AND CONSTRAINT_TYPE = 'P' AND STATUS = 'ENABLED';
    IF v_tablas_precedentes <> 1 THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 016: huella 010 (INICIATIVA_PROYECTO) no vigente'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de que las tablas/secuencias del propio 016 NO existen.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'INCORPORACION_REGISTRO',
            'INCORPORACION_CAMBIO',
            'INCORPORACION_CONFLICTO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 016: tablas de incorporacion ya existen; el incremento ya fue aplicado'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_INCORPORACION_REGISTRO',
            'SEQ_INCORPORACION_CAMBIO',
            'SEQ_INCORPORACION_CONFLICTO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 016: secuencias del incremento ya existen; revise el estado'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de objetos futuros estrictamente sucesores de 016.
--    016 es paralelo a 015; su unico sucesor estricto es 017.
--    Tambien se validan los diferidos 018-021.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'REPORTE_INSTITUCIONAL','REPORTE_SNAPSHOT','REPORTE_ARCHIVO','REPORTE_APROBACION',
            'REPORTE_DESTINATARIO','REPORTE_REMISION',
            'PROTOTIPO_PIIP','PROTOTIPO_VALIDACION','PROTOTIPO_HALLAZGO','MEDICION_EXPERIENCIA',
            'MEDICION_MUESTRA','MATRIZ_META_RECORRIDO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20015,
            'Precondicion 016: existen objetos futuros 017 o 018-021 ya creados'
        );
    END IF;
END;
/

PROMPT [016] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 016
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencias del incremento.
-- ----------------------------------------------------------------------------
PROMPT [016.1] Creando secuencias del incremento
CREATE SEQUENCE SEQ_INCORPORACION_REGISTRO
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_INCORPORACION_CAMBIO
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_INCORPORACION_CONFLICTO
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Tabla INCORPORACION_REGISTRO.
-- ----------------------------------------------------------------------------
PROMPT [016.2] Creando tabla INCORPORACION_REGISTRO
CREATE TABLE INCORPORACION_REGISTRO (
    ID_INCORPORACION         NUMBER(12)                      NOT NULL,
    FUENTE                   VARCHAR2(200 CHAR)               NOT NULL,
    FECHA_FUENTE             DATE                            NOT NULL,
    ID_RESPONSABLE           NUMBER(10)                      NOT NULL,
    ID_DOCUMENTO_FUENTE      NUMBER(12)                      NOT NULL,
    HASH_ORIGINAL            VARCHAR2(64 CHAR)               NOT NULL,
    DATOS_ORIGINALES         CLOB,
    ESTADO                   VARCHAR2(20 CHAR) DEFAULT 'PENDIENTE' NOT NULL,
    ID_REGISTRO_VINCULADO    NUMBER(12),
    CREADO_POR               VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION           TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [016.3] PK, UK, FKs y CHECKs de INCORPORACION_REGISTRO
ALTER TABLE INCORPORACION_REGISTRO
    ADD CONSTRAINT PK_INCORPORACION_REGISTRO PRIMARY KEY (ID_INCORPORACION);

ALTER TABLE INCORPORACION_REGISTRO
    ADD CONSTRAINT UK_IR_HASH_FUENTE_RESPONSABLE
    UNIQUE (HASH_ORIGINAL, ID_RESPONSABLE, FUENTE);

ALTER TABLE INCORPORACION_REGISTRO
    ADD CONSTRAINT FK_IR_RESPONSABLE
    FOREIGN KEY (ID_RESPONSABLE) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE INCORPORACION_REGISTRO
    ADD CONSTRAINT FK_IR_DOCUMENTO_FUENTE
    FOREIGN KEY (ID_DOCUMENTO_FUENTE) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE INCORPORACION_REGISTRO
    ADD CONSTRAINT FK_IR_REGISTRO_VINCULADO
    FOREIGN KEY (ID_REGISTRO_VINCULADO) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE INCORPORACION_REGISTRO
    ADD CONSTRAINT CK_IR_ESTADO
    CHECK (ESTADO IN ('PENDIENTE','VALIDADO','RECHAZADO'));

ALTER TABLE INCORPORACION_REGISTRO
    ADD CONSTRAINT CK_IR_HASH
    CHECK (REGEXP_LIKE(HASH_ORIGINAL, '^[0-9A-Fa-f]{64}$'));

-- ----------------------------------------------------------------------------
-- 3) Tabla INCORPORACION_CAMBIO (append-only).
-- ----------------------------------------------------------------------------
PROMPT [016.4] Creando tabla INCORPORACION_CAMBIO
CREATE TABLE INCORPORACION_CAMBIO (
    ID_CAMBIO        NUMBER(12)                      NOT NULL,
    ID_INCORPORACION NUMBER(12)                      NOT NULL,
    DATOS_ANTES      CLOB,
    DATOS_DESPUES    CLOB,
    MOTIVO           VARCHAR2(2000 CHAR),
    ID_ACTOR         NUMBER(10)                      NOT NULL,
    FECHA_CAMBIO     TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [016.5] PK y FKs de INCORPORACION_CAMBIO
ALTER TABLE INCORPORACION_CAMBIO
    ADD CONSTRAINT PK_INCORPORACION_CAMBIO PRIMARY KEY (ID_CAMBIO);

ALTER TABLE INCORPORACION_CAMBIO
    ADD CONSTRAINT FK_IC_INCORPORACION
    FOREIGN KEY (ID_INCORPORACION) REFERENCES INCORPORACION_REGISTRO (ID_INCORPORACION);

ALTER TABLE INCORPORACION_CAMBIO
    ADD CONSTRAINT FK_IC_ACTOR
    FOREIGN KEY (ID_ACTOR) REFERENCES USUARIO (ID_USUARIO);

-- ----------------------------------------------------------------------------
-- 4) Tabla INCORPORACION_CONFLICTO.
-- ----------------------------------------------------------------------------
PROMPT [016.6] Creando tabla INCORPORACION_CONFLICTO
CREATE TABLE INCORPORACION_CONFLICTO (
    ID_CONFLICTO            NUMBER(12)                      NOT NULL,
    ID_INCORPORACION         NUMBER(12)                      NOT NULL,
    TIPO_CONFLICTO           VARCHAR2(30 CHAR)               NOT NULL,
    ID_REGISTRO_CONFLICTIVO  NUMBER(12)                      NOT NULL,
    DESCRIPCION              VARCHAR2(2000 CHAR),
    RESUELTO                 CHAR(1 CHAR) DEFAULT 'N'        NOT NULL,
    ID_RESOLUTOR             NUMBER(10),
    FECHA_RESOLUCION         TIMESTAMP(6),
    ID_DOCUMENTO_RESOLUCION  NUMBER(12)
);

PROMPT [016.7] PK, UK, FKs y CHECKs de INCORPORACION_CONFLICTO
ALTER TABLE INCORPORACION_CONFLICTO
    ADD CONSTRAINT PK_INCORPORACION_CONFLICTO PRIMARY KEY (ID_CONFLICTO);

ALTER TABLE INCORPORACION_CONFLICTO
    ADD CONSTRAINT UK_ICONF_INC_TIPO_REG
    UNIQUE (ID_INCORPORACION, TIPO_CONFLICTO, ID_REGISTRO_CONFLICTIVO);

ALTER TABLE INCORPORACION_CONFLICTO
    ADD CONSTRAINT FK_ICONF_INCORPORACION
    FOREIGN KEY (ID_INCORPORACION) REFERENCES INCORPORACION_REGISTRO (ID_INCORPORACION);

ALTER TABLE INCORPORACION_CONFLICTO
    ADD CONSTRAINT FK_ICONF_REGISTRO_CONFLICTIVO
    FOREIGN KEY (ID_REGISTRO_CONFLICTIVO) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE INCORPORACION_CONFLICTO
    ADD CONSTRAINT FK_ICONF_RESOLUTOR
    FOREIGN KEY (ID_RESOLUTOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE INCORPORACION_CONFLICTO
    ADD CONSTRAINT FK_ICONF_DOCUMENTO_RESOLUCION
    FOREIGN KEY (ID_DOCUMENTO_RESOLUCION) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE INCORPORACION_CONFLICTO
    ADD CONSTRAINT CK_ICONF_TIPO
    CHECK (TIPO_CONFLICTO IN ('CODIGO','DUPLICADO','RELACION','MAPEO'));

ALTER TABLE INCORPORACION_CONFLICTO
    ADD CONSTRAINT CK_ICONF_RESUELTO
    CHECK (RESUELTO IN ('S','N'));

ALTER TABLE INCORPORACION_CONFLICTO
    ADD CONSTRAINT CK_ICONF_RESOLUCION
    CHECK (
        (RESUELTO = 'N' AND FECHA_RESOLUCION IS NULL AND ID_RESOLUTOR IS NULL)
     OR (RESUELTO = 'S' AND FECHA_RESOLUCION IS NOT NULL AND ID_RESOLUTOR IS NOT NULL)
    );

-- ----------------------------------------------------------------------------
-- 5) Indices auxiliares.
-- ----------------------------------------------------------------------------
PROMPT [016.8] Creando indices auxiliares
CREATE INDEX IDX_IC_INCORPORACION ON INCORPORACION_CAMBIO (ID_INCORPORACION);
CREATE INDEX IDX_ICONF_INCORPORACION ON INCORPORACION_CONFLICTO (ID_INCORPORACION);

-- ============================================================================
-- Validacion final del script 016
-- ============================================================================
PROMPT [016.9] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'INCORPORACION_REGISTRO',
            'INCORPORACION_CAMBIO',
            'INCORPORACION_CONFLICTO'
           );
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20030,
            'Validacion 016: tablas de incorporacion ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_INCORPORACION_REGISTRO',
            'SEQ_INCORPORACION_CAMBIO',
            'SEQ_INCORPORACION_CONFLICTO'
           )
       AND INCREMENT_BY = 1 AND CACHE_SIZE = 0 AND CYCLE_FLAG = 'N';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 016: secuencias del incremento ausentes o incompatibles');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'INCORPORACION_REGISTRO'
       AND CONSTRAINT_NAME = 'UK_IR_HASH_FUENTE_RESPONSABLE'
       AND CONSTRAINT_TYPE = 'U';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20032,
            'Validacion 016: UK idempotente ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME IN ('IDX_IC_INCORPORACION','IDX_ICONF_INCORPORACION')
       AND STATUS = 'VALID';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20033,
            'Validacion 016: indices auxiliares invalidos');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 016 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 016_incorporacion_individual completada correctamente.
