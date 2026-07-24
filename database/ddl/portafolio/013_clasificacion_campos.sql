-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 013 - Matriz append-only de
-- obligatoriedad, editabilidad, privacidad y actor responsable por
-- TIPO_REGISTRO/ETAPA/NRO_CAMPO
-- Archivo   : 013_clasificacion_campos.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : portafolio
-- Dependencias: 002, 009, 005, 006, 003+003.1+003.2, 008+008.1 y 001.
-- Alcance   : Crea PROYECTO_CAMPO_CLASIFICACION y su historial append-only
--             PROYECTO_CAMPO_CLASIF_HIST. La UK (TIPO_REGISTRO, ETAPA,
--             NRO_CAMPO) garantiza ausencia de matriz duplicada para la
--             misma combinacion. Ningun formulario, validacion, consulta o
--             reporte dependiente se habilita hasta la confirmacion humana
--             posterior.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: volver datos nuevos no publicables; nunca
--            ampliar acceso. Cambios posteriores generan una fila en el
--            historial y nunca eliminan la fila vigente.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [013] Validando precondiciones de 001, 002, 003+003.1+003.2, 005, 006, 008+008.1 y 009...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones acumuladas.
-- ----------------------------------------------------------------------------
DECLARE
    v_tablas_precedentes PLS_INTEGER;
    v_secuencias_precedentes PLS_INTEGER;
BEGIN
    -- 23 tablas (001+002+003+005+006+008)
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
            'OPERACION_APROVISIONAMIENTO'
           );
    IF v_tablas_precedentes <> 23 THEN
        RAISE_APPLICATION_ERROR(
            -20010,
            'Precondicion 013: se esperaban 23 tablas previas y se encontraron '
            || TO_CHAR(v_tablas_precedentes)
        );
    END IF;

    -- 20 secuencias previas
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
            'SEQ_OPERACION_APROVISIONAMIENTO'
           );
    IF v_secuencias_precedentes <> 20 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Precondicion 013: se esperaban 20 secuencias previas y se encontraron '
            || TO_CHAR(v_secuencias_precedentes)
        );
    END IF;

    -- PROYECTO.VERSION debe existir (huella 009)
    SELECT COUNT(*) INTO v_tablas_precedentes
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'PROYECTO' AND COLUMN_NAME = 'VERSION';
    IF v_tablas_precedentes <> 1 THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 013: PROYECTO.VERSION ausente (huella 009)'
        );
    END IF;

    -- ROL debe existir para la FK de ID_ROL_EDITOR
    SELECT COUNT(*) INTO v_tablas_precedentes
      FROM USER_TABLES WHERE TABLE_NAME = 'ROL';
    IF v_tablas_precedentes <> 1 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 013: tabla ROL ausente (huella 001)'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de que las tablas/secuencias del propio 013 NO existen.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'PROYECTO_CAMPO_CLASIFICACION',
            'PROYECTO_CAMPO_CLASIF_HIST'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 013: las tablas de matriz de campos ya existen; el incremento ya fue aplicado'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_PROY_CAMPO_CLASIF',
            'SEQ_PROY_CAMPO_CLASIF_HIST'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20015,
            'Precondicion 013: las secuencias del incremento ya existen'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de objetos futuros estrictamente sucesores de 013.
--    013 es paralelo a 014; sus sucesores estrictos son 015, 016 y 017.
--    Tambien se validan los diferidos 018-021.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
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
            -20016,
            'Precondicion 013: existen objetos futuros 015-017 o 018-021 ya creados'
        );
    END IF;
END;
/

PROMPT [013] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 013
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencias del incremento.
-- ----------------------------------------------------------------------------
PROMPT [013.1] Creando secuencias del incremento
CREATE SEQUENCE SEQ_PROY_CAMPO_CLASIF
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_PROY_CAMPO_CLASIF_HIST
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Tabla PROYECTO_CAMPO_CLASIFICACION.
-- ----------------------------------------------------------------------------
PROMPT [013.2] Creando tabla PROYECTO_CAMPO_CLASIFICACION
CREATE TABLE PROYECTO_CAMPO_CLASIFICACION (
    ID_CLASIFICACION  NUMBER(12)                      NOT NULL,
    TIPO_REGISTRO     VARCHAR2(20 CHAR)               NOT NULL,
    ETAPA             VARCHAR2(30 CHAR)               NOT NULL,
    NRO_CAMPO         NUMBER(3)                       NOT NULL,
    CLASIFICACION     VARCHAR2(20 CHAR)               NOT NULL,
    EDITABLE          CHAR(1 CHAR) DEFAULT 'S'        NOT NULL,
    ID_ROL_EDITOR     NUMBER(5)                       NOT NULL,
    OBLIGATORIO       CHAR(1 CHAR) DEFAULT 'N'        NOT NULL,
    ACTIVA            CHAR(1 CHAR) DEFAULT 'S'        NOT NULL,
    CREADO_POR        VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION    TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [013.3] PK, UK, FK y CHECKs de PROYECTO_CAMPO_CLASIFICACION
ALTER TABLE PROYECTO_CAMPO_CLASIFICACION
    ADD CONSTRAINT PK_PROYECTO_CAMPO_CLASIFICACION PRIMARY KEY (ID_CLASIFICACION);

ALTER TABLE PROYECTO_CAMPO_CLASIFICACION
    ADD CONSTRAINT UK_PCC_TIPO_ETAPA_CAMPO UNIQUE (TIPO_REGISTRO, ETAPA, NRO_CAMPO);

ALTER TABLE PROYECTO_CAMPO_CLASIFICACION
    ADD CONSTRAINT FK_PCC_ROL_EDITOR
    FOREIGN KEY (ID_ROL_EDITOR) REFERENCES ROL (ID_ROL);

ALTER TABLE PROYECTO_CAMPO_CLASIFICACION
    ADD CONSTRAINT CK_PCC_TIPO_REGISTRO
    CHECK (TIPO_REGISTRO IN ('INICIATIVA','PROYECTO'));

ALTER TABLE PROYECTO_CAMPO_CLASIFICACION
    ADD CONSTRAINT CK_PCC_CLASIFICACION
    CHECK (CLASIFICACION IN ('PUBLICO','INTERNO','RESTRINGIDO'));

ALTER TABLE PROYECTO_CAMPO_CLASIFICACION
    ADD CONSTRAINT CK_PCC_EDITABLE
    CHECK (EDITABLE IN ('S','N'));

ALTER TABLE PROYECTO_CAMPO_CLASIFICACION
    ADD CONSTRAINT CK_PCC_OBLIGATORIO
    CHECK (OBLIGATORIO IN ('S','N'));

ALTER TABLE PROYECTO_CAMPO_CLASIFICACION
    ADD CONSTRAINT CK_PCC_ACTIVA
    CHECK (ACTIVA IN ('S','N'));

ALTER TABLE PROYECTO_CAMPO_CLASIFICACION
    ADD CONSTRAINT CK_PCC_NRO_CAMPO
    CHECK (NRO_CAMPO BETWEEN 1 AND 23);

-- ----------------------------------------------------------------------------
-- 3) Tabla PROYECTO_CAMPO_CLASIF_HIST (append-only).
-- ----------------------------------------------------------------------------
PROMPT [013.4] Creando tabla PROYECTO_CAMPO_CLASIF_HIST
CREATE TABLE PROYECTO_CAMPO_CLASIF_HIST (
    ID_HISTORIAL             NUMBER(12)                      NOT NULL,
    ID_CLASIFICACION         NUMBER(12)                      NOT NULL,
    CLASIFICACION_ANTERIOR   VARCHAR2(20 CHAR),
    CLASIFICACION_NUEVA      VARCHAR2(20 CHAR)               NOT NULL,
    EDITABLE_ANTERIOR        CHAR(1 CHAR),
    EDITABLE_NUEVO           CHAR(1 CHAR),
    OBLIGATORIO_ANTERIOR     CHAR(1 CHAR),
    OBLIGATORIO_NUEVO        CHAR(1 CHAR),
    ID_ACTOR                 NUMBER(10)                      NOT NULL,
    FECHA_CAMBIO             TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    MOTIVO                   VARCHAR2(2000 CHAR),
    ID_DOCUMENTO_DECISION    NUMBER(12)
);

PROMPT [013.5] PK, FKs y CHECKs de PROYECTO_CAMPO_CLASIF_HIST
ALTER TABLE PROYECTO_CAMPO_CLASIF_HIST
    ADD CONSTRAINT PK_PROYECTO_CAMPO_CLASIF_HIST PRIMARY KEY (ID_HISTORIAL);

ALTER TABLE PROYECTO_CAMPO_CLASIF_HIST
    ADD CONSTRAINT FK_PCCH_CLASIFICACION
    FOREIGN KEY (ID_CLASIFICACION) REFERENCES PROYECTO_CAMPO_CLASIFICACION (ID_CLASIFICACION);

ALTER TABLE PROYECTO_CAMPO_CLASIF_HIST
    ADD CONSTRAINT FK_PCCH_ACTOR
    FOREIGN KEY (ID_ACTOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE PROYECTO_CAMPO_CLASIF_HIST
    ADD CONSTRAINT FK_PCCH_DOCUMENTO_DECISION
    FOREIGN KEY (ID_DOCUMENTO_DECISION) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE PROYECTO_CAMPO_CLASIF_HIST
    ADD CONSTRAINT CK_PCCH_CLAS_NUEVA
    CHECK (CLASIFICACION_NUEVA IN ('PUBLICO','INTERNO','RESTRINGIDO'));

ALTER TABLE PROYECTO_CAMPO_CLASIF_HIST
    ADD CONSTRAINT CK_PCCH_CLAS_ANT
    CHECK (CLASIFICACION_ANTERIOR IS NULL
           OR CLASIFICACION_ANTERIOR IN ('PUBLICO','INTERNO','RESTRINGIDO'));

ALTER TABLE PROYECTO_CAMPO_CLASIF_HIST
    ADD CONSTRAINT CK_PCCH_EDITABLE_NUEVO
    CHECK (EDITABLE_NUEVO IS NULL OR EDITABLE_NUEVO IN ('S','N'));

ALTER TABLE PROYECTO_CAMPO_CLASIF_HIST
    ADD CONSTRAINT CK_PCCH_EDITABLE_ANT
    CHECK (EDITABLE_ANTERIOR IS NULL OR EDITABLE_ANTERIOR IN ('S','N'));

ALTER TABLE PROYECTO_CAMPO_CLASIF_HIST
    ADD CONSTRAINT CK_PCCH_OBLIGATORIO_NUEVO
    CHECK (OBLIGATORIO_NUEVO IS NULL OR OBLIGATORIO_NUEVO IN ('S','N'));

ALTER TABLE PROYECTO_CAMPO_CLASIF_HIST
    ADD CONSTRAINT CK_PCCH_OBLIGATORIO_ANT
    CHECK (OBLIGATORIO_ANTERIOR IS NULL OR OBLIGATORIO_ANTERIOR IN ('S','N'));

-- ----------------------------------------------------------------------------
-- 4) Indices auxiliares.
-- ----------------------------------------------------------------------------
PROMPT [013.6] Creando indices auxiliares
CREATE INDEX IDX_PCC_TIPO_ETAPA
    ON PROYECTO_CAMPO_CLASIFICACION (TIPO_REGISTRO, ETAPA);
CREATE INDEX IDX_PCCH_CLASIFICACION
    ON PROYECTO_CAMPO_CLASIF_HIST (ID_CLASIFICACION);

-- ============================================================================
-- Validacion final del script 013
-- ============================================================================
PROMPT [013.7] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'PROYECTO_CAMPO_CLASIFICACION',
            'PROYECTO_CAMPO_CLASIF_HIST'
           );
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20030,
            'Validacion 013: tablas de la matriz de campos ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_PROY_CAMPO_CLASIF',
            'SEQ_PROY_CAMPO_CLASIF_HIST'
           )
       AND INCREMENT_BY = 1 AND CACHE_SIZE = 0 AND CYCLE_FLAG = 'N';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 013: secuencias del incremento ausentes o incompatibles');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'PROYECTO_CAMPO_CLASIFICACION'
       AND CONSTRAINT_NAME = 'UK_PCC_TIPO_ETAPA_CAMPO'
       AND CONSTRAINT_TYPE = 'U';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20032,
            'Validacion 013: UK_PCC_TIPO_ETAPA_CAMPO ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME IN ('IDX_PCC_TIPO_ETAPA','IDX_PCCH_CLASIFICACION')
       AND STATUS = 'VALID';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20033,
            'Validacion 013: indices auxiliares invalidos');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 013 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 013_clasificacion_campos completada correctamente.
