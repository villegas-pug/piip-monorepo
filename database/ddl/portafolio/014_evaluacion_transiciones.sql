-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 014 - Evaluacion, subsanacion y
-- aplicabilidad de iniciativas (cubre US2 y US5)
-- Archivo   : 014_evaluacion_transiciones.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : portafolio
-- Dependencias: 003+003.1+003.2, 008+008.1 y 009, ademas de 002, 005, 006 y 001.
-- Alcance   : Crea EVALUACION_INICIATIVA, SUBSANACION_INICIATIVA,
--             APLICABILIDAD_INICIATIVA y APLICABILIDAD_CRITERIO. La UK
--             de SUBSANACION_INICIATIVA garantiza una sola oportunidad por
--             iniciativa; APLICABILIDAD_CRITERIO se ancla a la
--             aplicabilidad padre para conservar la lista estructurada.
--             La maquina de estados reside en TransicionEstadoService Java;
--             TRANSICION_PERMITIDA permanece como legado inactivo.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: detener comandos; no revertir estados
--            confirmados. Las evaluaciones y subsanaciones no se eliminan.
-- Reglas de negocio declaradas como invariantes deterministas:
--   CK_SI_PLAZO exige que el plazo sea posterior a la apertura de la
--   subsanacion (PLAZO IS NULL OR PLAZO > APERTURA_EN); la subsanacion solo
--   puede ocurrir despues de abierta. La comparacion entre columnas es
--   determinista y no depende del reloj del servidor, por lo que es valida
--   como CHECK constraint.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [014] Validando precondiciones de 001, 002, 003+003.1+003.2, 005, 006, 008+008.1 y 009...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones acumuladas.
-- ----------------------------------------------------------------------------
DECLARE
    v_tablas_precedentes PLS_INTEGER;
    v_secuencias_precedentes PLS_INTEGER;
BEGIN
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
            'Precondicion 014: se esperaban 23 tablas previas y se encontraron '
            || TO_CHAR(v_tablas_precedentes)
        );
    END IF;

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
            'Precondicion 014: se esperaban 20 secuencias previas y se encontraron '
            || TO_CHAR(v_secuencias_precedentes)
        );
    END IF;

    -- Huella 008: USUARIO_ROL_UNIDAD.VERSION
    SELECT COUNT(*) INTO v_tablas_precedentes
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'USUARIO_ROL_UNIDAD' AND COLUMN_NAME = 'VERSION';
    IF v_tablas_precedentes <> 1 THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 014: USUARIO_ROL_UNIDAD.VERSION ausente (huella 008)'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de que las tablas/secuencias del propio 014 NO existen.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'EVALUACION_INICIATIVA',
            'SUBSANACION_INICIATIVA',
            'APLICABILIDAD_INICIATIVA',
            'APLICABILIDAD_CRITERIO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 014: tablas de evaluacion/subsanacion/aplicabilidad ya existen; el incremento ya fue aplicado'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_EVALUACION_INICIATIVA',
            'SEQ_SUBSANACION_INICIATIVA',
            'SEQ_APLICABILIDAD_INICIATIVA',
            'SEQ_APLICABILIDAD_CRITERIO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 014: secuencias del incremento ya existen; revise el estado'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de objetos futuros estrictamente sucesores de 014.
--    014 es paralelo a 013; sus sucesores estrictos son 015, 016 y 017.
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
            -20015,
            'Precondicion 014: existen objetos futuros 015-017 o 018-021 ya creados'
        );
    END IF;
END;
/

PROMPT [014] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 014
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencias del incremento.
-- ----------------------------------------------------------------------------
PROMPT [014.1] Creando secuencias del incremento
CREATE SEQUENCE SEQ_EVALUACION_INICIATIVA
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_SUBSANACION_INICIATIVA
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_APLICABILIDAD_INICIATIVA
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_APLICABILIDAD_CRITERIO
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Tabla EVALUACION_INICIATIVA.
-- ----------------------------------------------------------------------------
PROMPT [014.2] Creando tabla EVALUACION_INICIATIVA
CREATE TABLE EVALUACION_INICIATIVA (
    ID_EVALUACION          NUMBER(12)                      NOT NULL,
    ID_INICIATIVA          NUMBER(12)                      NOT NULL,
    ID_EVALUADOR           NUMBER(10)                      NOT NULL,
    ID_ROL_EFECTIVO        NUMBER(5),
    ID_UNIDAD_EFECTIVA     NUMBER(10),
    FECHA_EVALUACION       TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    OBSERVACIONES          VARCHAR2(2000 CHAR),
    ID_DOCUMENTO_OPINION   NUMBER(12)
);

PROMPT [014.3] PK, UK, FKs y CHECK de EVALUACION_INICIATIVA
ALTER TABLE EVALUACION_INICIATIVA
    ADD CONSTRAINT PK_EVALUACION_INICIATIVA PRIMARY KEY (ID_EVALUACION);

ALTER TABLE EVALUACION_INICIATIVA
    ADD CONSTRAINT UK_EI_INICIATIVA UNIQUE (ID_INICIATIVA);

ALTER TABLE EVALUACION_INICIATIVA
    ADD CONSTRAINT FK_EI_INICIATIVA
    FOREIGN KEY (ID_INICIATIVA) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE EVALUACION_INICIATIVA
    ADD CONSTRAINT FK_EI_EVALUADOR
    FOREIGN KEY (ID_EVALUADOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE EVALUACION_INICIATIVA
    ADD CONSTRAINT FK_EI_ROL_EFECTIVO
    FOREIGN KEY (ID_ROL_EFECTIVO) REFERENCES ROL (ID_ROL);

ALTER TABLE EVALUACION_INICIATIVA
    ADD CONSTRAINT FK_EI_UNIDAD_EFECTIVA
    FOREIGN KEY (ID_UNIDAD_EFECTIVA) REFERENCES UNIDAD_EJECUTORA (ID_UNIDAD);

ALTER TABLE EVALUACION_INICIATIVA
    ADD CONSTRAINT FK_EI_DOCUMENTO_OPINION
    FOREIGN KEY (ID_DOCUMENTO_OPINION) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE EVALUACION_INICIATIVA
    ADD CONSTRAINT CK_EI_OBSERVACION_LONGITUD
    CHECK (OBSERVACIONES IS NULL OR LENGTH(TRIM(OBSERVACIONES)) <= 2000);

-- ----------------------------------------------------------------------------
-- 3) Tabla SUBSANACION_INICIATIVA.
-- ----------------------------------------------------------------------------
PROMPT [014.4] Creando tabla SUBSANACION_INICIATIVA
CREATE TABLE SUBSANACION_INICIATIVA (
    ID_SUBSANACION     NUMBER(12)                      NOT NULL,
    ID_INICIATIVA      NUMBER(12)                      NOT NULL,
    PLAZO              DATE                            NOT NULL,
    INCUMPLIMIENTOS    CLOB                            NOT NULL,
    APERTURA_EN        TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    ATENCION_EN        TIMESTAMP(6),
    ID_ACTOR           NUMBER(10)                      NOT NULL
);

PROMPT [014.5] PK, UK, FKs y CHECK de SUBSANACION_INICIATIVA
ALTER TABLE SUBSANACION_INICIATIVA
    ADD CONSTRAINT PK_SUBSANACION_INICIATIVA PRIMARY KEY (ID_SUBSANACION);

ALTER TABLE SUBSANACION_INICIATIVA
    ADD CONSTRAINT UK_SI_INICIATIVA UNIQUE (ID_INICIATIVA);

ALTER TABLE SUBSANACION_INICIATIVA
    ADD CONSTRAINT FK_SI_INICIATIVA
    FOREIGN KEY (ID_INICIATIVA) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE SUBSANACION_INICIATIVA
    ADD CONSTRAINT FK_SI_ACTOR
    FOREIGN KEY (ID_ACTOR) REFERENCES USUARIO (ID_USUARIO);

-- CK_SI_PLAZO: invariante determinista a nivel de fila. La subsanacion
-- solo puede ocurrir despues de la apertura, por lo que el plazo debe ser
-- estrictamente posterior a APERTURA_EN cuando ambos son no nulos.
ALTER TABLE SUBSANACION_INICIATIVA
    ADD CONSTRAINT CK_SI_PLAZO
    CHECK (PLAZO IS NULL OR APERTURA_EN IS NULL OR PLAZO > APERTURA_EN);

-- ----------------------------------------------------------------------------
-- 4) Tabla APLICABILIDAD_INICIATIVA.
-- ----------------------------------------------------------------------------
PROMPT [014.6] Creando tabla APLICABILIDAD_INICIATIVA
CREATE TABLE APLICABILIDAD_INICIATIVA (
    ID_APLICABILIDAD   NUMBER(12)                      NOT NULL,
    ID_INICIATIVA      NUMBER(12)                      NOT NULL,
    RESULTADO          VARCHAR2(20 CHAR)               NOT NULL,
    MOTIVO             VARCHAR2(2000 CHAR),
    ID_EVALUADOR       NUMBER(10)                      NOT NULL,
    FECHA              TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [014.7] PK, UK, FKs y CHECKs de APLICABILIDAD_INICIATIVA
ALTER TABLE APLICABILIDAD_INICIATIVA
    ADD CONSTRAINT PK_APLICABILIDAD_INICIATIVA PRIMARY KEY (ID_APLICABILIDAD);

ALTER TABLE APLICABILIDAD_INICIATIVA
    ADD CONSTRAINT UK_AI_INICIATIVA UNIQUE (ID_INICIATIVA);

ALTER TABLE APLICABILIDAD_INICIATIVA
    ADD CONSTRAINT FK_AI_INICIATIVA
    FOREIGN KEY (ID_INICIATIVA) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE APLICABILIDAD_INICIATIVA
    ADD CONSTRAINT FK_AI_EVALUADOR
    FOREIGN KEY (ID_EVALUADOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE APLICABILIDAD_INICIATIVA
    ADD CONSTRAINT CK_AI_RESULTADO
    CHECK (RESULTADO IN ('APLICABLE','NO_APLICABLE'));

ALTER TABLE APLICABILIDAD_INICIATIVA
    ADD CONSTRAINT CK_AI_MOTIVO
    CHECK (
        (RESULTADO = 'APLICABLE')
     OR (RESULTADO = 'NO_APLICABLE' AND MOTIVO IS NOT NULL)
    );

-- ----------------------------------------------------------------------------
-- 5) Tabla APLICABILIDAD_CRITERIO.
-- ----------------------------------------------------------------------------
PROMPT [014.8] Creando tabla APLICABILIDAD_CRITERIO
CREATE TABLE APLICABILIDAD_CRITERIO (
    ID_CRITERIO        NUMBER(12)                      NOT NULL,
    ID_APLICABILIDAD   NUMBER(12)                      NOT NULL,
    CLAVE              VARCHAR2(50 CHAR)               NOT NULL,
    VALOR              VARCHAR2(500 CHAR)               NOT NULL,
    ORDEN              NUMBER(3)                       NOT NULL
);

PROMPT [014.9] PK, UK, FK y CHECKs de APLICABILIDAD_CRITERIO
ALTER TABLE APLICABILIDAD_CRITERIO
    ADD CONSTRAINT PK_APLICABILIDAD_CRITERIO PRIMARY KEY (ID_CRITERIO);

ALTER TABLE APLICABILIDAD_CRITERIO
    ADD CONSTRAINT UK_AC_APLICABILIDAD_CLAVE
    UNIQUE (ID_APLICABILIDAD, CLAVE);

ALTER TABLE APLICABILIDAD_CRITERIO
    ADD CONSTRAINT FK_AC_APLICABILIDAD
    FOREIGN KEY (ID_APLICABILIDAD) REFERENCES APLICABILIDAD_INICIATIVA (ID_APLICABILIDAD);

ALTER TABLE APLICABILIDAD_CRITERIO
    ADD CONSTRAINT CK_AC_ORDEN
    CHECK (ORDEN BETWEEN 1 AND 999);

-- ============================================================================
-- Validacion final del script 014
-- ============================================================================
PROMPT [014.10] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'EVALUACION_INICIATIVA',
            'SUBSANACION_INICIATIVA',
            'APLICABILIDAD_INICIATIVA',
            'APLICABILIDAD_CRITERIO'
           );
    IF v_total <> 4 THEN
        RAISE_APPLICATION_ERROR(-20030,
            'Validacion 014: tablas de evaluacion/subsanacion/aplicabilidad ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_EVALUACION_INICIATIVA',
            'SEQ_SUBSANACION_INICIATIVA',
            'SEQ_APLICABILIDAD_INICIATIVA',
            'SEQ_APLICABILIDAD_CRITERIO'
           )
       AND INCREMENT_BY = 1 AND CACHE_SIZE = 0 AND CYCLE_FLAG = 'N';
    IF v_total <> 4 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 014: secuencias del incremento ausentes o incompatibles');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME IN (
            'EVALUACION_INICIATIVA','SUBSANACION_INICIATIVA',
            'APLICABILIDAD_INICIATIVA'
           )
       AND CONSTRAINT_NAME IN (
            'UK_EI_INICIATIVA','UK_SI_INICIATIVA','UK_AI_INICIATIVA'
           )
       AND CONSTRAINT_TYPE = 'U';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20032,
            'Validacion 014: UKs por iniciativa ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'APLICABILIDAD_INICIATIVA'
       AND CONSTRAINT_NAME IN ('CK_AI_RESULTADO','CK_AI_MOTIVO')
       AND CONSTRAINT_TYPE = 'C' AND STATUS = 'ENABLED';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20033,
            'Validacion 014: CHECKs de aplicabilidad ausentes');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 014 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 014_evaluacion_transiciones completada correctamente.
