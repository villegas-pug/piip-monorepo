-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 012 - Participantes persona y unidad
-- por proyecto con vigencia
-- Archivo   : 012_responsables_participantes.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : portafolio
-- Dependencias: 008+008.1, 009 y 011, ademas de 002, 003+003.1+003.2 y 001.
-- Alcance   : Crea PARTICIPANTE_PERSONA (con o sin ID_USUARIO) y las
--             relaciones PROYECTO_PARTICIPANTE_PERSONA y
--             PROYECTO_PARTICIPANTE_UNIDAD con vigencia, actor y CHECKs.
--             Conserva la cardinalidad titular de 011 y la regla de
--             clasificacion para participantes restringidos.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: deshabilitar altas; conservar titularidades y
--            participaciones. Las participaciones cerradas no se eliminan.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [012] Validando precondiciones de 001, 002, 003+003.1+003.2, 005, 006, 008+008.1, 009 y 011...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones acumuladas (incluye 011 y la huella 008+008.1).
-- ----------------------------------------------------------------------------
DECLARE
    v_tablas_precedentes PLS_INTEGER;
    v_secuencias_precedentes PLS_INTEGER;
BEGIN
    -- 23 tablas previas (001+002+003+005+006+008) + 1 de 011 = 24
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
            'PROYECTO_RESPONSABLE'
           );
    IF v_tablas_precedentes <> 24 THEN
        RAISE_APPLICATION_ERROR(
            -20010,
            'Precondicion 012: se esperaban 24 tablas previas y se encontraron '
            || TO_CHAR(v_tablas_precedentes)
        );
    END IF;

    -- 20 secuencias previas (001+002+003+005+006+008.1) + 1 de 011 = 21
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
            'SEQ_PROYECTO_RESPONSABLE'
           );
    IF v_secuencias_precedentes <> 21 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Precondicion 012: se esperaban 21 secuencias previas y se encontraron '
            || TO_CHAR(v_secuencias_precedentes)
        );
    END IF;

    -- PROYECTO_RESPONSABLE debe tener PK vigente (huella 011)
    SELECT COUNT(*) INTO v_tablas_precedentes
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'PROYECTO_RESPONSABLE'
       AND CONSTRAINT_NAME = 'PK_PROYECTO_RESPONSABLE'
       AND CONSTRAINT_TYPE = 'P' AND STATUS = 'ENABLED';
    IF v_tablas_precedentes <> 1 THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 012: huella 011 (PROYECTO_RESPONSABLE) no vigente'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de que las tablas/secuencias del propio 012 NO existen.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'PARTICIPANTE_PERSONA',
            'PROYECTO_PARTICIPANTE_PERSONA',
            'PROYECTO_PARTICIPANTE_UNIDAD'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 012: las tablas de participantes ya existen; el incremento ya fue aplicado'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_PARTICIPANTE_PERSONA',
            'SEQ_PROY_PART_PERSONA',
            'SEQ_PROY_PART_UNIDAD'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 012: las secuencias del incremento ya existen; revise el estado'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de objetos futuros estrictamente sucesores de 012.
--    012 es paralelo a 014 y 015; sus sucesores estrictos son 016 y 017.
--    Tambien se validan los diferidos 018-021.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
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
            -20015,
            'Precondicion 012: existen objetos futuros 014-017 o 018-021 ya creados'
        );
    END IF;
END;
/

PROMPT [012] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 012
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencias del incremento.
-- ----------------------------------------------------------------------------
PROMPT [012.1] Creando secuencias del incremento
CREATE SEQUENCE SEQ_PARTICIPANTE_PERSONA
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_PROY_PART_PERSONA
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_PROY_PART_UNIDAD
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Tabla PARTICIPANTE_PERSONA.
-- ----------------------------------------------------------------------------
PROMPT [012.2] Creando tabla PARTICIPANTE_PERSONA
CREATE TABLE PARTICIPANTE_PERSONA (
    ID_PARTICIPANTE     NUMBER(12)                      NOT NULL,
    ID_USUARIO          NUMBER(10),
    NOMBRES_COMPLETOS   VARCHAR2(300 CHAR)               NOT NULL,
    INSTITUCION         VARCHAR2(200 CHAR),
    FUNCION             VARCHAR2(200 CHAR),
    CLASIFICACION       VARCHAR2(20 CHAR) DEFAULT 'RESTRINGIDO' NOT NULL
);

PROMPT [012.3] PK, FK y CHECKs de PARTICIPANTE_PERSONA
ALTER TABLE PARTICIPANTE_PERSONA
    ADD CONSTRAINT PK_PARTICIPANTE_PERSONA PRIMARY KEY (ID_PARTICIPANTE);

ALTER TABLE PARTICIPANTE_PERSONA
    ADD CONSTRAINT FK_PP_USUARIO
    FOREIGN KEY (ID_USUARIO) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE PARTICIPANTE_PERSONA
    ADD CONSTRAINT CK_PP_CLASIFICACION
    CHECK (CLASIFICACION IN ('PUBLICO','INTERNO','RESTRINGIDO'));

ALTER TABLE PARTICIPANTE_PERSONA
    ADD CONSTRAINT CK_PP_DATOS_MINIMOS
    CHECK (
        (ID_USUARIO IS NULL AND NOMBRES_COMPLETOS IS NOT NULL)
     OR (ID_USUARIO IS NOT NULL)
    );

PROMPT [012.4] Indice auxiliar IDX_PP_USUARIO
CREATE INDEX IDX_PP_USUARIO ON PARTICIPANTE_PERSONA (ID_USUARIO);

-- ----------------------------------------------------------------------------
-- 3) Tabla PROYECTO_PARTICIPANTE_PERSONA.
-- ----------------------------------------------------------------------------
PROMPT [012.5] Creando tabla PROYECTO_PARTICIPANTE_PERSONA
CREATE TABLE PROYECTO_PARTICIPANTE_PERSONA (
    ID_PROY_PART_PERSONA NUMBER(12)                      NOT NULL,
    ID_PROYECTO          NUMBER(12)                      NOT NULL,
    ID_PARTICIPANTE      NUMBER(12)                      NOT NULL,
    INICIO               DATE                            NOT NULL,
    FIN                  DATE,
    ID_ACTOR             NUMBER(10)                      NOT NULL,
    CREADO_POR           VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION       TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [012.6] PK, UK, FKs y CHECK de PROYECTO_PARTICIPANTE_PERSONA
ALTER TABLE PROYECTO_PARTICIPANTE_PERSONA
    ADD CONSTRAINT PK_PROYECTO_PARTICIPANTE_PERSONA PRIMARY KEY (ID_PROY_PART_PERSONA);

ALTER TABLE PROYECTO_PARTICIPANTE_PERSONA
    ADD CONSTRAINT UK_PPP_PROY_PART UNIQUE (ID_PROYECTO, ID_PARTICIPANTE);

ALTER TABLE PROYECTO_PARTICIPANTE_PERSONA
    ADD CONSTRAINT FK_PPP_PROYECTO
    FOREIGN KEY (ID_PROYECTO) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE PROYECTO_PARTICIPANTE_PERSONA
    ADD CONSTRAINT FK_PPP_PARTICIPANTE
    FOREIGN KEY (ID_PARTICIPANTE) REFERENCES PARTICIPANTE_PERSONA (ID_PARTICIPANTE);

ALTER TABLE PROYECTO_PARTICIPANTE_PERSONA
    ADD CONSTRAINT FK_PPP_ACTOR
    FOREIGN KEY (ID_ACTOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE PROYECTO_PARTICIPANTE_PERSONA
    ADD CONSTRAINT CK_PPP_VIGENCIA
    CHECK (FIN IS NULL OR FIN >= INICIO);

-- ----------------------------------------------------------------------------
-- 4) Tabla PROYECTO_PARTICIPANTE_UNIDAD.
-- ----------------------------------------------------------------------------
PROMPT [012.7] Creando tabla PROYECTO_PARTICIPANTE_UNIDAD
CREATE TABLE PROYECTO_PARTICIPANTE_UNIDAD (
    ID_PROY_PART_UNIDAD NUMBER(12)                      NOT NULL,
    ID_PROYECTO          NUMBER(12)                      NOT NULL,
    ID_UNIDAD            NUMBER(10)                      NOT NULL,
    INICIO               DATE                            NOT NULL,
    FIN                  DATE,
    ID_ACTOR             NUMBER(10)                      NOT NULL,
    CREADO_POR           VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION       TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [012.8] PK, UK y FKs de PROYECTO_PARTICIPANTE_UNIDAD
ALTER TABLE PROYECTO_PARTICIPANTE_UNIDAD
    ADD CONSTRAINT PK_PROYECTO_PARTICIPANTE_UNIDAD PRIMARY KEY (ID_PROY_PART_UNIDAD);

ALTER TABLE PROYECTO_PARTICIPANTE_UNIDAD
    ADD CONSTRAINT UK_PPU_PROY_UNI UNIQUE (ID_PROYECTO, ID_UNIDAD);

ALTER TABLE PROYECTO_PARTICIPANTE_UNIDAD
    ADD CONSTRAINT FK_PPU_PROYECTO
    FOREIGN KEY (ID_PROYECTO) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE PROYECTO_PARTICIPANTE_UNIDAD
    ADD CONSTRAINT FK_PPU_UNIDAD
    FOREIGN KEY (ID_UNIDAD) REFERENCES UNIDAD_EJECUTORA (ID_UNIDAD);

ALTER TABLE PROYECTO_PARTICIPANTE_UNIDAD
    ADD CONSTRAINT FK_PPU_ACTOR
    FOREIGN KEY (ID_ACTOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE PROYECTO_PARTICIPANTE_UNIDAD
    ADD CONSTRAINT CK_PPU_VIGENCIA
    CHECK (FIN IS NULL OR FIN >= INICIO);

-- ----------------------------------------------------------------------------
-- 5) Indices auxiliares.
-- ----------------------------------------------------------------------------
PROMPT [012.9] Creando indices auxiliares
CREATE INDEX IDX_PPP_PROYECTO ON PROYECTO_PARTICIPANTE_PERSONA (ID_PROYECTO);
CREATE INDEX IDX_PPU_PROYECTO ON PROYECTO_PARTICIPANTE_UNIDAD  (ID_PROYECTO);

-- ============================================================================
-- Validacion final del script 012
-- ============================================================================
PROMPT [012.10] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'PARTICIPANTE_PERSONA',
            'PROYECTO_PARTICIPANTE_PERSONA',
            'PROYECTO_PARTICIPANTE_UNIDAD'
           );
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20030, 'Validacion 012: tablas de participantes ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_PARTICIPANTE_PERSONA',
            'SEQ_PROY_PART_PERSONA',
            'SEQ_PROY_PART_UNIDAD'
           )
       AND INCREMENT_BY = 1 AND CACHE_SIZE = 0 AND CYCLE_FLAG = 'N';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 012: secuencias del incremento ausentes o incompatibles');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME IN (
            'PARTICIPANTE_PERSONA',
            'PROYECTO_PARTICIPANTE_PERSONA',
            'PROYECTO_PARTICIPANTE_UNIDAD'
           )
       AND CONSTRAINT_NAME IN (
            'PK_PARTICIPANTE_PERSONA',
            'PK_PROYECTO_PARTICIPANTE_PERSONA',
            'PK_PROYECTO_PARTICIPANTE_UNIDAD'
           )
       AND CONSTRAINT_TYPE = 'P' AND STATUS = 'ENABLED';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20032, 'Validacion 012: PKs de participantes ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME IN (
            'IDX_PP_USUARIO','IDX_PPP_PROYECTO','IDX_PPU_PROYECTO'
           )
       AND STATUS = 'VALID';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20033, 'Validacion 012: indices auxiliares invalidos');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 012 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 012_responsables_participantes completada correctamente.
