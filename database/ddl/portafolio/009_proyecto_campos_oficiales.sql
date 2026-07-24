-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 009 - Campos oficiales, version,
-- subsanacion y FKs catalogo de PROYECTO
-- Archivo   : 009_proyecto_campos_oficiales.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : portafolio
-- Dependencias: 005 (database/ddl/organizacion/005_objetivo_pei_versionado.sql),
--               006 (database/ddl/organizacion/006_actividad_poi_versionada.sql),
--               002, 003, 003.1, 003.2, 008+008.1 y 001.
-- Alcance   : Anade a PROYECTO los 23 campos oficiales aprobados, la columna
--             VERSION (@Version), el flag de subsanacion activa y las FKs
--             OBJETIVO_PEI_ID y ACTIVIDAD_POI_ID hacia los catalogos 005 y
--             006. ADMINISTRACION queda legacy nullable sin reglas nuevas.
--             DESCRIPCION permanece legacy; no se aplica split sin mapeo
--             aprobado. Las FKs hacia catalogos se crean ENABLE NOVALIDATE
--             para preservar la carga inicial.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: mantener columnas legacy; no restaurar checks
--            si hay estados nuevos. La inyeccion del prefijo de unidad
--            aprobado en CODIGO se delega a CodigoProyectoService Java.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [009] Validando precondiciones de 001, 002, 003+003.1+003.2, 005, 006 y 008+008.1...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones: 18 tablas (001+002+003) + 2 de 005 + 2 de 006 + 3 de 008 = 21.
-- ----------------------------------------------------------------------------
DECLARE
    v_tablas_001_002_003 PLS_INTEGER;
    v_tablas_005 PLS_INTEGER;
    v_tablas_006 PLS_INTEGER;
    v_tablas_008 PLS_INTEGER;
    v_secuencias_005_006_008 PLS_INTEGER;
    v_columnas_baseline PLS_INTEGER;
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
            'Precondicion 009: se esperaban 16 tablas (001+002+003) y se encontraron '
            || TO_CHAR(v_tablas_001_002_003)
        );
    END IF;

    SELECT COUNT(*) INTO v_tablas_005
      FROM USER_TABLES
     WHERE TABLE_NAME IN ('CAT_OBJETIVO_PEI_VERSION','CAT_OBJETIVO_PEI');
    IF v_tablas_005 <> 2 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Precondicion 009: catalogos PEI (005) no vigentes'
        );
    END IF;

    SELECT COUNT(*) INTO v_tablas_006
      FROM USER_TABLES
     WHERE TABLE_NAME IN ('CAT_ACTIVIDAD_POI_VERSION','CAT_ACTIVIDAD_POI');
    IF v_tablas_006 <> 2 THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 009: catalogos POI (006) no vigentes'
        );
    END IF;

    SELECT COUNT(*) INTO v_tablas_008
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'USUARIO_ROL_UNIDAD_EVENTO',
            'SUPLENCIA_FUNCIONAL',
            'OPERACION_APROVISIONAMIENTO'
           );
    IF v_tablas_008 <> 3 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 009: la huella parcial 008 no esta vigente; ejecute antes 008+008.1'
        );
    END IF;

    -- Secuencias requeridas: 13 (001+002+003) + 2 de 005 + 2 de 006 + 3 de 008.1 = 20
    SELECT COUNT(*) INTO v_secuencias_005_006_008
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_OBJETIVO_PEI_VERSION','SEQ_OBJETIVO_PEI',
            'SEQ_ACTIVIDAD_POI_VERSION','SEQ_ACTIVIDAD_POI',
            'SEQ_URU_EVENTO','SEQ_SUPLENCIA_FUNCIONAL','SEQ_OPERACION_APROVISIONAMIENTO'
           );
    IF v_secuencias_005_006_008 <> 7 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 009: secuencias de 005, 006 u 008.1 no vigentes'
        );
    END IF;

    -- Columnas baseline de PROYECTO deben existir
    SELECT COUNT(*) INTO v_columnas_baseline
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'PROYECTO'
       AND COLUMN_NAME IN (
            'ID_PROYECTO','CODIGO','TIPO_REGISTRO','NOMBRE','ESTADO',
            'ID_UNIDAD_EJECUTORA','ID_RESPONSABLE'
           );
    IF v_columnas_baseline <> 7 THEN
        RAISE_APPLICATION_ERROR(
            -20015,
            'Precondicion 009: PROYECTO no contiene las columnas baseline esperadas'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de que las columnas del propio 009 NO existen.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'PROYECTO'
       AND COLUMN_NAME IN (
            'CODIGO_PREFIJO','DETALLE_FUENTE','PROBLEMA_PUBLICO',
            'SOLUCION_PROPUESTA','COMPONENTE_DIGITAL',
            'DETALLE_COMPONENTE_DIGITAL','NOTA',
            'OBJETIVO_PEI_ID','ACTIVIDAD_POI_ID',
            'VERSION','SUBSANACION_ACTIVA'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20016,
            'Precondicion 009: PROYECTO ya tiene columnas oficiales; el incremento ya fue aplicado'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de objetos futuros 010-024 que NO deben existir.
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
            'Precondicion 009: existen objetos futuros 010-024 ya creados'
        );
    END IF;
END;
/

PROMPT [009] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 009
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) ADMINISTRACION se vuelve nullable (columna legacy).
-- ----------------------------------------------------------------------------
PROMPT [009.1] Permitir nulo en PROYECTO.ADMINISTRACION (legacy)
ALTER TABLE PROYECTO MODIFY (ADMINISTRACION NULL);

-- ----------------------------------------------------------------------------
-- 2) Nuevas columnas oficiales (cada ALTER agrega un commit implicito).
-- ----------------------------------------------------------------------------
PROMPT [009.2] Anadiendo CODIGO_PREFIJO
ALTER TABLE PROYECTO ADD (CODIGO_PREFIJO VARCHAR2(20 CHAR));

PROMPT [009.3] Anadiendo DETALLE_FUENTE
ALTER TABLE PROYECTO ADD (DETALLE_FUENTE VARCHAR2(500 CHAR));

PROMPT [009.4] Anadiendo PROBLEMA_PUBLICO
ALTER TABLE PROYECTO ADD (PROBLEMA_PUBLICO VARCHAR2(2000 CHAR));

PROMPT [009.5] Anadiendo SOLUCION_PROPUESTA
ALTER TABLE PROYECTO ADD (SOLUCION_PROPUESTA VARCHAR2(2000 CHAR));

PROMPT [009.6] Anadiendo COMPONENTE_DIGITAL
ALTER TABLE PROYECTO ADD (COMPONENTE_DIGITAL CHAR(1 CHAR) DEFAULT 'N' NOT NULL);

PROMPT [009.7] Anadiendo DETALLE_COMPONENTE_DIGITAL
ALTER TABLE PROYECTO ADD (DETALLE_COMPONENTE_DIGITAL VARCHAR2(500 CHAR));

PROMPT [009.8] Anadiendo NOTA
ALTER TABLE PROYECTO ADD (NOTA VARCHAR2(1000 CHAR));

PROMPT [009.9] Anadiendo OBJETIVO_PEI_ID
ALTER TABLE PROYECTO ADD (OBJETIVO_PEI_ID NUMBER(10));

PROMPT [009.10] Anadiendo ACTIVIDAD_POI_ID
ALTER TABLE PROYECTO ADD (ACTIVIDAD_POI_ID NUMBER(10));

PROMPT [009.11] Anadiendo VERSION (@Version)
ALTER TABLE PROYECTO ADD (VERSION NUMBER(10) DEFAULT 0 NOT NULL);

PROMPT [009.12] Anadiendo SUBSANACION_ACTIVA
ALTER TABLE PROYECTO ADD (SUBSANACION_ACTIVA CHAR(1 CHAR) DEFAULT 'N' NOT NULL);

-- ----------------------------------------------------------------------------
-- 3) CHECKs.
-- ----------------------------------------------------------------------------
PROMPT [009.13] CHECK de COMPONENTE_DIGITAL
ALTER TABLE PROYECTO
    ADD CONSTRAINT CK_PROY_COMPONENTE_DIGITAL
    CHECK (COMPONENTE_DIGITAL IN ('S','N'));

PROMPT [009.14] CHECK de coherencia entre COMPONENTE_DIGITAL y DETALLE
ALTER TABLE PROYECTO
    ADD CONSTRAINT CK_PROY_DETALLE_COMPONENTE
    CHECK (
        (COMPONENTE_DIGITAL = 'N' AND DETALLE_COMPONENTE_DIGITAL IS NULL)
     OR (COMPONENTE_DIGITAL = 'S'
         AND DETALLE_COMPONENTE_DIGITAL IS NOT NULL
         AND LENGTH(TRIM(DETALLE_COMPONENTE_DIGITAL)) >= 1)
    );

PROMPT [009.15] CHECK de SUBSANACION_ACTIVA
ALTER TABLE PROYECTO
    ADD CONSTRAINT CK_PROY_SUBSANACION_ACTIVA
    CHECK (SUBSANACION_ACTIVA IN ('S','N'));

-- ----------------------------------------------------------------------------
-- 4) FKs hacia catalogos 005 y 006. ENABLE NOVALIDATE para preservar carga
--    inicial hasta una migracion/corte expresamente aprobada.
-- ----------------------------------------------------------------------------
PROMPT [009.16] FK de OBJETIVO_PEI_ID
ALTER TABLE PROYECTO
    ADD CONSTRAINT FK_PROY_OBJETIVO_PEI
    FOREIGN KEY (OBJETIVO_PEI_ID) REFERENCES CAT_OBJETIVO_PEI (ID_OBJETIVO)
    ENABLE NOVALIDATE;

PROMPT [009.17] FK de ACTIVIDAD_POI_ID
ALTER TABLE PROYECTO
    ADD CONSTRAINT FK_PROY_ACTIVIDAD_POI
    FOREIGN KEY (ACTIVIDAD_POI_ID) REFERENCES CAT_ACTIVIDAD_POI (ID_ACTIVIDAD)
    ENABLE NOVALIDATE;

-- ----------------------------------------------------------------------------
-- 5) Indices auxiliares.
-- ----------------------------------------------------------------------------
PROMPT [009.18] Creando indices auxiliares
CREATE INDEX IDX_PROY_OBJETIVO_PEI
    ON PROYECTO (OBJETIVO_PEI_ID);
CREATE INDEX IDX_PROY_ACTIVIDAD_POI
    ON PROYECTO (ACTIVIDAD_POI_ID);
CREATE INDEX IDX_PROY_COMPONENTE_DIGITAL
    ON PROYECTO (COMPONENTE_DIGITAL);

-- ============================================================================
-- Validacion final del script 009
-- ============================================================================
PROMPT [009.19] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    -- Columnas agregadas a PROYECTO
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'PROYECTO'
       AND COLUMN_NAME IN (
            'CODIGO_PREFIJO','DETALLE_FUENTE','PROBLEMA_PUBLICO',
            'SOLUCION_PROPUESTA','COMPONENTE_DIGITAL',
            'DETALLE_COMPONENTE_DIGITAL','NOTA',
            'OBJETIVO_PEI_ID','ACTIVIDAD_POI_ID',
            'VERSION','SUBSANACION_ACTIVA'
           );
    IF v_total <> 11 THEN
        RAISE_APPLICATION_ERROR(-20030,
            'Validacion 009: columnas oficiales incompletas en PROYECTO');
    END IF;

    -- CHECKs nuevos
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'PROYECTO'
       AND CONSTRAINT_NAME IN (
            'CK_PROY_COMPONENTE_DIGITAL',
            'CK_PROY_DETALLE_COMPONENTE',
            'CK_PROY_SUBSANACION_ACTIVA'
           )
       AND CONSTRAINT_TYPE = 'C'
       AND STATUS = 'ENABLED';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 009: CHECKs de campos oficiales ausentes');
    END IF;

    -- FKs nuevas (estado: ENABLE NOVALIDATE -> STATUS=ENABLED, VALIDATED=NOT VALIDATED)
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'PROYECTO'
       AND CONSTRAINT_NAME IN ('FK_PROY_OBJETIVO_PEI','FK_PROY_ACTIVIDAD_POI')
       AND CONSTRAINT_TYPE = 'R'
       AND STATUS = 'ENABLED';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20032,
            'Validacion 009: FKs hacia catalogos PEI/POI ausentes');
    END IF;

    -- ADMINISTRACION nullable
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'PROYECTO'
       AND COLUMN_NAME = 'ADMINISTRACION'
       AND NULLABLE = 'Y';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20033,
            'Validacion 009: PROYECTO.ADMINISTRACION no admite nulo');
    END IF;

    -- Indices auxiliares
    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME IN (
            'IDX_PROY_OBJETIVO_PEI',
            'IDX_PROY_ACTIVIDAD_POI',
            'IDX_PROY_COMPONENTE_DIGITAL'
           )
       AND STATUS = 'VALID';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20034,
            'Validacion 009: indices auxiliares ausentes o invalidos');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 009 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 009_proyecto_campos_oficiales completada correctamente.
