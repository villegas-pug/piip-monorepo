-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 010 - Vinculo inmutable iniciativa
-- proyecto derivado unico
-- Archivo   : 010_iniciativa_proyecto_relacion.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : portafolio
-- Dependencias: 009 (database/ddl/portafolio/009_proyecto_campos_oficiales.sql),
--               005, 006, 002, 003+003.1+003.2, 008+008.1 y 001.
-- Alcance   : Crea INICIATIVA_PROYECTO y su secuencia. Cada iniciativa
--             aprobada puede vincularse a lo sumo con un proyecto derivado
--             y cada proyecto puede provenir de a lo sumo una iniciativa.
--             El identificador ID_INICIATIVA reusa PROYECTO.ID_PROYECTO de
--             la iniciativa origen; la fila de relacion es append-only y
--             sus UK aportan los indices unicos canonicos y evitan la
--             carrera del segundo derivado (BR-005); no se crean indices
--             auxiliares redundantes sobre las mismas columnas.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: detener nuevas relaciones; conservar vinculos
--            confirmados. Ninguna relacion creada se elimina; el eventual
--            corte se gestionara con una migracion aprobada.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [010] Validando precondiciones de 001, 002, 003+003.1+003.2, 005, 006, 008+008.1 y 009...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones acumuladas.
-- ----------------------------------------------------------------------------
DECLARE
    v_tablas_001_002_003_005_006_008 PLS_INTEGER;
    v_tablas_009 PLS_INTEGER;
    v_secuencias_acum PLS_INTEGER;
    v_columnas_proyecto PLS_INTEGER;
BEGIN
    -- 16 (001+002+003) + 2 de 005 + 2 de 006 + 3 de 008 = 23
    SELECT COUNT(*) INTO v_tablas_001_002_003_005_006_008
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
    IF v_tablas_001_002_003_005_006_008 <> 23 THEN
        RAISE_APPLICATION_ERROR(
            -20010,
            'Precondicion 010: se esperaban 23 tablas previas y se encontraron '
            || TO_CHAR(v_tablas_001_002_003_005_006_008)
        );
    END IF;

    -- 009 no introduce tablas, solo columnas en PROYECTO
    SELECT COUNT(*) INTO v_tablas_009
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'PROYECTO'
       AND COLUMN_NAME IN ('OBJETIVO_PEI_ID','ACTIVIDAD_POI_ID','VERSION');
    IF v_tablas_009 <> 3 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Precondicion 010: columnas oficiales 009 no vigentes en PROYECTO'
        );
    END IF;

    -- Secuencias acumuladas: 13 (001+002+003) + 2 (005) + 2 (006) + 3 (008.1) = 20
    SELECT COUNT(*) INTO v_secuencias_acum
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
    IF v_secuencias_acum <> 20 THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 010: se esperaban 20 secuencias acumuladas y se encontraron '
            || TO_CHAR(v_secuencias_acum)
        );
    END IF;

    -- Columnas baseline de PROYECTO
    SELECT COUNT(*) INTO v_columnas_proyecto
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'PROYECTO'
       AND COLUMN_NAME IN ('ID_PROYECTO','CODIGO','TIPO_REGISTRO');
    IF v_columnas_proyecto <> 3 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 010: PROYECTO no contiene las columnas baseline'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de que la tabla/secuencia del propio 010 NO existen.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES WHERE TABLE_NAME = 'INICIATIVA_PROYECTO';
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 010: INICIATIVA_PROYECTO ya existe; el incremento ya fue aplicado'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES WHERE SEQUENCE_NAME = 'SEQ_INICIATIVA_PROYECTO';
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20015,
            'Precondicion 010: SEQ_INICIATIVA_PROYECTO ya existe'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de objetos futuros estrictamente sucesores de 010.
--    010 es paralelo a 011 y 013; sus sucesores estrictos son 012,
--    014, 015, 016 y 017. Tambien se validan los diferidos 018-021.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'EVALUACION_INICIATIVA','SUBSANACION_INICIATIVA','APLICABILIDAD_INICIATIVA',
            'APLICABILIDAD_CRITERIO',
            'PARTICIPANTE_PERSONA','PROYECTO_PARTICIPANTE_PERSONA','PROYECTO_PARTICIPANTE_UNIDAD',
            'PROYECTO_CAMPO_CLASIFICACION','PROYECTO_CAMPO_CLASIF_HIST',
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
            'Precondicion 010: existen objetos futuros 012, 014-017 o 018-021 ya creados'
        );
    END IF;
END;
/

PROMPT [010] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 010
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencia.
-- ----------------------------------------------------------------------------
PROMPT [010.1] Creando secuencia del incremento
CREATE SEQUENCE SEQ_INICIATIVA_PROYECTO
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Tabla INICIATIVA_PROYECTO.
-- ----------------------------------------------------------------------------
PROMPT [010.2] Creando tabla INICIATIVA_PROYECTO
CREATE TABLE INICIATIVA_PROYECTO (
    ID_RELACION     NUMBER(12)                      NOT NULL,
    ID_INICIATIVA   NUMBER(12)                      NOT NULL,
    ID_PROYECTO     NUMBER(12)                      NOT NULL,
    CREADA_POR      VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION  TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [010.3] PK, UKs, FKs y CHECK de INICIATIVA_PROYECTO
ALTER TABLE INICIATIVA_PROYECTO
    ADD CONSTRAINT PK_INICIATIVA_PROYECTO PRIMARY KEY (ID_RELACION);

ALTER TABLE INICIATIVA_PROYECTO
    ADD CONSTRAINT UK_IP_INICIATIVA UNIQUE (ID_INICIATIVA);

ALTER TABLE INICIATIVA_PROYECTO
    ADD CONSTRAINT UK_IP_PROYECTO UNIQUE (ID_PROYECTO);

ALTER TABLE INICIATIVA_PROYECTO
    ADD CONSTRAINT FK_IP_INICIATIVA
    FOREIGN KEY (ID_INICIATIVA) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE INICIATIVA_PROYECTO
    ADD CONSTRAINT FK_IP_PROYECTO
    FOREIGN KEY (ID_PROYECTO) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE INICIATIVA_PROYECTO
    ADD CONSTRAINT CK_IP_DISTINTOS
    CHECK (ID_INICIATIVA <> ID_PROYECTO);

-- ----------------------------------------------------------------------------
-- 3) Indices unicos canonicos de UK_IP_INICIATIVA y UK_IP_PROYECTO.
-- Oracle crea y mantiene ambos indices de respaldo; crear indices adicionales
-- sobre las mismas columnas provocaria ORA-01408.
-- ----------------------------------------------------------------------------
PROMPT [010.4] Las UK aportan los indices unicos canonicos; no se crean indices redundantes

-- ============================================================================
-- Validacion final del script 010
-- ============================================================================
PROMPT [010.5] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES WHERE TABLE_NAME = 'INICIATIVA_PROYECTO';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20030, 'Validacion 010: INICIATIVA_PROYECTO ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME = 'SEQ_INICIATIVA_PROYECTO'
       AND INCREMENT_BY = 1 AND CACHE_SIZE = 0 AND CYCLE_FLAG = 'N';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 010: SEQ_INICIATIVA_PROYECTO ausente o incompatible');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'INICIATIVA_PROYECTO'
       AND CONSTRAINT_NAME = 'PK_INICIATIVA_PROYECTO'
       AND CONSTRAINT_TYPE = 'P' AND STATUS = 'ENABLED';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20032, 'Validacion 010: PK ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'INICIATIVA_PROYECTO'
       AND CONSTRAINT_NAME IN ('UK_IP_INICIATIVA','UK_IP_PROYECTO')
       AND CONSTRAINT_TYPE = 'U';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20033, 'Validacion 010: UKs ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'INICIATIVA_PROYECTO'
       AND CONSTRAINT_NAME IN ('FK_IP_INICIATIVA','FK_IP_PROYECTO','CK_IP_DISTINTOS')
       AND STATUS = 'ENABLED';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20034, 'Validacion 010: FKs o CHECK de distintos ausente');
    END IF;

    -- Indices unicos de respaldo de ambas UK. Sus nombres fisicos se derivan
    -- de USER_CONSTRAINTS.INDEX_NAME y no se presuponen.
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS c
      JOIN USER_INDEXES i
        ON i.INDEX_NAME = c.INDEX_NAME
     WHERE c.TABLE_NAME = 'INICIATIVA_PROYECTO'
       AND c.CONSTRAINT_NAME IN ('UK_IP_INICIATIVA','UK_IP_PROYECTO')
       AND c.CONSTRAINT_TYPE = 'U'
       AND c.STATUS = 'ENABLED'
       AND i.STATUS = 'VALID'
       AND i.UNIQUENESS = 'UNIQUE';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20035,
            'Validacion 010: indices de respaldo de las UK de INICIATIVA_PROYECTO ausentes, invalidos o no unicos');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS c
      JOIN USER_IND_COLUMNS ic
        ON ic.INDEX_NAME = c.INDEX_NAME
     WHERE c.TABLE_NAME = 'INICIATIVA_PROYECTO'
       AND c.CONSTRAINT_TYPE = 'U'
       AND ((c.CONSTRAINT_NAME = 'UK_IP_INICIATIVA'
             AND ic.COLUMN_NAME = 'ID_INICIATIVA'
             AND ic.COLUMN_POSITION = 1)
         OR (c.CONSTRAINT_NAME = 'UK_IP_PROYECTO'
             AND ic.COLUMN_NAME = 'ID_PROYECTO'
             AND ic.COLUMN_POSITION = 1));
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20035,
            'Validacion 010: indices de respaldo de las UK no conservan las columnas canonicas');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 010 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 010_iniciativa_proyecto_relacion completada correctamente.
