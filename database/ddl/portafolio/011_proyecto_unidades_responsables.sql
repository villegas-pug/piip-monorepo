-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 011 - Titularidad vigente de
-- Responsable por proyecto (PROYECTO_RESPONSABLE)
-- Archivo   : 011_proyecto_unidades_responsables.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : portafolio
-- Dependencias: 009 (database/ddl/portafolio/009_proyecto_campos_oficiales.sql),
--               005, 006, 002, 003+003.1+003.2, 008+008.1 y 001.
-- Alcance   : Crea PROYECTO_RESPONSABLE para modelar titularidades del
--             registro con vigencia, motivo y actor de sustitucion. La
--             PROYECTO_UNIDAD_ORGANICA legacy se conserva hasta una
--             migracion/corte expresamente aprobada. La unicidad del
--             titular activo se garantiza mediante un indice unico
--             funcional (UX_PR_TITULAR_ACTIVO) que solo aplica cuando
--             FIN IS NULL.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: mantener referencia legacy
--            PROYECTO_UNIDAD_ORGANICA hasta corte confirmado. Las
--            titularidades finalizadas nunca se eliminan.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [011] Validando precondiciones de 001, 002, 003+003.1+003.2, 005, 006, 008+008.1 y 009...

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
            'Precondicion 011: se esperaban 23 tablas previas y se encontraron '
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
            'Precondicion 011: se esperaban 20 secuencias previas y se encontraron '
            || TO_CHAR(v_secuencias_precedentes)
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de que la tabla/secuencia del propio 011 NO existen.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES WHERE TABLE_NAME = 'PROYECTO_RESPONSABLE';
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 011: PROYECTO_RESPONSABLE ya existe; el incremento ya fue aplicado'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES WHERE SEQUENCE_NAME = 'SEQ_PROYECTO_RESPONSABLE';
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 011: SEQ_PROYECTO_RESPONSABLE ya existe'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de objetos futuros estrictamente sucesores de 011.
--    011 es paralelo a 010, 013 y 014; sus sucesores estrictos son 012,
--    015, 016 y 017. Tambien se validan los diferidos 018-021.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'PARTICIPANTE_PERSONA','PROYECTO_PARTICIPANTE_PERSONA','PROYECTO_PARTICIPANTE_UNIDAD',
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
            -20014,
            'Precondicion 011: existen objetos futuros 012, 015-017 o 018-021 ya creados'
        );
    END IF;
END;
/

PROMPT [011] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 011
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencia.
-- ----------------------------------------------------------------------------
PROMPT [011.1] Creando secuencia del incremento
CREATE SEQUENCE SEQ_PROYECTO_RESPONSABLE
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Tabla PROYECTO_RESPONSABLE.
-- ----------------------------------------------------------------------------
PROMPT [011.2] Creando tabla PROYECTO_RESPONSABLE
CREATE TABLE PROYECTO_RESPONSABLE (
    ID_TITULARIDAD        NUMBER(12)                      NOT NULL,
    ID_PROYECTO           NUMBER(12)                      NOT NULL,
    ID_USUARIO            NUMBER(10)                      NOT NULL,
    INICIO                DATE                            NOT NULL,
    FIN                   DATE,
    MOTIVO_SUSTITUCION    VARCHAR2(2000 CHAR),
    ID_ACTOR_SUSTITUCION  NUMBER(10),
    CREADO_POR            VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION        TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [011.3] PK, FKs y CHECKs de PROYECTO_RESPONSABLE
ALTER TABLE PROYECTO_RESPONSABLE
    ADD CONSTRAINT PK_PROYECTO_RESPONSABLE PRIMARY KEY (ID_TITULARIDAD);

ALTER TABLE PROYECTO_RESPONSABLE
    ADD CONSTRAINT FK_PR_PROYECTO
    FOREIGN KEY (ID_PROYECTO) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE PROYECTO_RESPONSABLE
    ADD CONSTRAINT FK_PR_USUARIO
    FOREIGN KEY (ID_USUARIO) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE PROYECTO_RESPONSABLE
    ADD CONSTRAINT FK_PR_ACTOR
    FOREIGN KEY (ID_ACTOR_SUSTITUCION) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE PROYECTO_RESPONSABLE
    ADD CONSTRAINT CK_PR_VIGENCIA
    CHECK (FIN IS NULL OR FIN >= INICIO);

-- ----------------------------------------------------------------------------
-- 3) Indice unico funcional del titular activo.
-- ----------------------------------------------------------------------------
PROMPT [011.4] Creando indice unico funcional UX_PR_TITULAR_ACTIVO
CREATE UNIQUE INDEX UX_PR_TITULAR_ACTIVO
    ON PROYECTO_RESPONSABLE (
        CASE WHEN FIN IS NULL THEN ID_PROYECTO END
    );

PROMPT [011.5] Indice auxiliar IDX_PR_PROYECTO
CREATE INDEX IDX_PR_PROYECTO ON PROYECTO_RESPONSABLE (ID_PROYECTO);
CREATE INDEX IDX_PR_USUARIO ON PROYECTO_RESPONSABLE (ID_USUARIO);

-- ============================================================================
-- Validacion final del script 011
-- ============================================================================
PROMPT [011.6] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES WHERE TABLE_NAME = 'PROYECTO_RESPONSABLE';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20030, 'Validacion 011: PROYECTO_RESPONSABLE ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME = 'SEQ_PROYECTO_RESPONSABLE'
       AND INCREMENT_BY = 1 AND CACHE_SIZE = 0 AND CYCLE_FLAG = 'N';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 011: SEQ_PROYECTO_RESPONSABLE ausente o incompatible');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'PROYECTO_RESPONSABLE'
       AND CONSTRAINT_NAME = 'PK_PROYECTO_RESPONSABLE'
       AND CONSTRAINT_TYPE = 'P' AND STATUS = 'ENABLED';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20032, 'Validacion 011: PK ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'PROYECTO_RESPONSABLE'
       AND CONSTRAINT_NAME IN ('FK_PR_PROYECTO','FK_PR_USUARIO','FK_PR_ACTOR','CK_PR_VIGENCIA')
       AND STATUS = 'ENABLED';
    IF v_total <> 4 THEN
        RAISE_APPLICATION_ERROR(-20033, 'Validacion 011: FKs o CHECK ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME IN ('UX_PR_TITULAR_ACTIVO','IDX_PR_PROYECTO','IDX_PR_USUARIO')
       AND STATUS = 'VALID';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20034, 'Validacion 011: indices ausentes o invalidos');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 011 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 011_proyecto_unidades_responsables completada correctamente.
