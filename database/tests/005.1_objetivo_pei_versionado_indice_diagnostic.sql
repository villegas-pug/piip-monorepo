-- ============================================================================
-- PIIP MIDAGRI - Diagnostico de solo lectura 005.1 - Constraints PEI
-- Archivo    : 005.1_objetivo_pei_versionado_indice_diagnostic.sql
-- Esquema    : KALLPA_PIIP
-- Modulo     : organizacion
-- Proposito  : identificar la huella real de constraints de 005 antes de
--              reintentar la correccion forward-only 005.1.
--
-- ADVERTENCIA: este script es exclusivamente de solo lectura. No ejecuta
-- sentencias de definicion, manipulacion ni control transaccional, y no realiza
-- cambios persistentes de sesion.
-- No ejecutar contra ambientes compartidos sin autorizacion humana.
-- Ejecucion  : manual por DBA autorizado, con salida DBMS_OUTPUT habilitada.
-- ============================================================================

SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK ON
SET VERIFY OFF

PROMPT [005.1-diagnostic] Listando los 11 constraints esperados de la huella parcial 005...

DECLARE
    v_encontrados PLS_INTEGER := 0;
BEGIN
    FOR r IN (
        WITH esperados (NOMBRE_ESPERADO, TABLA_ESPERADA, TIPO_ESPERADO) AS (
            SELECT 'PK_CAT_OBJETIVO_PEI_VERSION', 'CAT_OBJETIVO_PEI_VERSION', 'P' FROM DUAL UNION ALL
            SELECT 'UK_OPV_CODIGO',                 'CAT_OBJETIVO_PEI_VERSION', 'U' FROM DUAL UNION ALL
            SELECT 'FK_OPV_VERSION_ANTERIOR',       'CAT_OBJETIVO_PEI_VERSION', 'R' FROM DUAL UNION ALL
            SELECT 'FK_OPV_DOCUMENTO',              'CAT_OBJETIVO_PEI_VERSION', 'R' FROM DUAL UNION ALL
            SELECT 'CK_OPV_VIGENCIA',               'CAT_OBJETIVO_PEI_VERSION', 'C' FROM DUAL UNION ALL
            SELECT 'CK_OPV_ACTIVA',                 'CAT_OBJETIVO_PEI_VERSION', 'C' FROM DUAL UNION ALL
            SELECT 'PK_CAT_OBJETIVO_PEI',           'CAT_OBJETIVO_PEI',         'P' FROM DUAL UNION ALL
            SELECT 'UK_OP_VERSION_CODIGO',          'CAT_OBJETIVO_PEI',         'U' FROM DUAL UNION ALL
            SELECT 'FK_OP_VERSION',                 'CAT_OBJETIVO_PEI',         'R' FROM DUAL UNION ALL
            SELECT 'CK_OP_VIGENCIA',                'CAT_OBJETIVO_PEI',         'C' FROM DUAL UNION ALL
            SELECT 'CK_OP_ACTIVO',                  'CAT_OBJETIVO_PEI',         'C' FROM DUAL
        )
        SELECT e.NOMBRE_ESPERADO,
               e.TABLA_ESPERADA,
               e.TIPO_ESPERADO,
               c.CONSTRAINT_NAME AS NOMBRE_REAL,
               c.CONSTRAINT_TYPE,
               c.STATUS,
               c.VALIDATED,
               c.INDEX_NAME
          FROM esperados e
          LEFT JOIN USER_CONSTRAINTS c
            ON c.TABLE_NAME = e.TABLA_ESPERADA
           AND c.CONSTRAINT_NAME = e.NOMBRE_ESPERADO
         ORDER BY e.TABLA_ESPERADA, e.NOMBRE_ESPERADO
    ) LOOP
        IF r.NOMBRE_REAL IS NOT NULL THEN
            v_encontrados := v_encontrados + 1;
        END IF;
        DBMS_OUTPUT.PUT_LINE(
            'ESPERADO=' || r.NOMBRE_ESPERADO ||
            ';TABLA=' || r.TABLA_ESPERADA ||
            ';TIPO_ESPERADO=' || r.TIPO_ESPERADO ||
            ';ENCONTRADO=' || CASE WHEN r.NOMBRE_REAL IS NULL THEN 'N' ELSE 'S' END ||
            ';NOMBRE_REAL=' || NVL(r.NOMBRE_REAL, '<NULL>') ||
            ';TIPO=' || NVL(r.CONSTRAINT_TYPE, '<NULL>') ||
            ';ESTADO=' || NVL(r.STATUS, '<NULL>') ||
            ';VALIDADO=' || NVL(r.VALIDATED, '<NULL>') ||
            ';INDEX_NAME=' || NVL(r.INDEX_NAME, '<NULL>')
        );
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('TOTAL_ESPERADOS_ENCONTRADOS=' || TO_CHAR(v_encontrados) || '/11');
END;
/

PROMPT [005.1-diagnostic] Listando todos los constraints reales de las tablas PEI...

BEGIN
    FOR r IN (
        SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE, STATUS, VALIDATED,
               INDEX_NAME, R_CONSTRAINT_NAME
          FROM USER_CONSTRAINTS
         WHERE TABLE_NAME IN ('CAT_OBJETIVO_PEI_VERSION', 'CAT_OBJETIVO_PEI')
         ORDER BY TABLE_NAME, CONSTRAINT_NAME
    ) LOOP
        DBMS_OUTPUT.PUT_LINE(
            'TABLA=' || r.TABLE_NAME ||
            ';CONSTRAINT_NAME=' || r.CONSTRAINT_NAME ||
            ';CONSTRAINT_TYPE=' || r.CONSTRAINT_TYPE ||
            ';STATUS=' || r.STATUS ||
            ';VALIDATED=' || r.VALIDATED ||
            ';INDEX_NAME=' || NVL(r.INDEX_NAME, '<NULL>') ||
            ';R_CONSTRAINT_NAME=' || NVL(r.R_CONSTRAINT_NAME, '<NULL>')
        );
    END LOOP;
END;
/

PROMPT [005.1-diagnostic] Listando columnas reales de las tablas PEI...

BEGIN
    FOR r IN (
        SELECT TABLE_NAME, COLUMN_ID, COLUMN_NAME, DATA_TYPE, DATA_LENGTH,
               CHAR_LENGTH, NULLABLE
          FROM USER_TAB_COLUMNS
         WHERE TABLE_NAME IN ('CAT_OBJETIVO_PEI_VERSION', 'CAT_OBJETIVO_PEI')
         ORDER BY TABLE_NAME, COLUMN_ID
    ) LOOP
        DBMS_OUTPUT.PUT_LINE(
            'TABLA=' || r.TABLE_NAME ||
            ';COLUMN_ID=' || TO_CHAR(r.COLUMN_ID) ||
            ';COLUMNA=' || r.COLUMN_NAME ||
            ';TIPO=' || r.DATA_TYPE ||
            ';DATA_LENGTH=' || TO_CHAR(r.DATA_LENGTH) ||
            ';CHAR_LENGTH=' || NVL(TO_CHAR(r.CHAR_LENGTH), '<NULL>') ||
            ';NULLABLE=' || r.NULLABLE
        );
    END LOOP;
END;
/

PROMPT [005.1-diagnostic] Listando indice de respaldo de UK_OP_VERSION_CODIGO...

DECLARE
    v_indice_encontrado BOOLEAN := FALSE;
    v_columnas_encontradas PLS_INTEGER := 0;
BEGIN
    FOR r IN (
        SELECT c.CONSTRAINT_NAME, c.INDEX_NAME, i.STATUS AS INDEX_STATUS,
               i.UNIQUENESS, i.INDEX_TYPE
          FROM USER_CONSTRAINTS c
          LEFT JOIN USER_INDEXES i
            ON i.INDEX_NAME = c.INDEX_NAME
         WHERE c.TABLE_NAME = 'CAT_OBJETIVO_PEI'
           AND c.CONSTRAINT_NAME = 'UK_OP_VERSION_CODIGO'
    ) LOOP
        v_indice_encontrado := TRUE;
        DBMS_OUTPUT.PUT_LINE(
            'UK=' || r.CONSTRAINT_NAME ||
            ';INDEX_NAME=' || NVL(r.INDEX_NAME, '<NULL>') ||
            ';INDEX_STATUS=' || NVL(r.INDEX_STATUS, '<NULL>') ||
            ';UNIQUENESS=' || NVL(r.UNIQUENESS, '<NULL>') ||
            ';INDEX_TYPE=' || NVL(r.INDEX_TYPE, '<NULL>')
        );
    END LOOP;
    IF NOT v_indice_encontrado THEN
        DBMS_OUTPUT.PUT_LINE('UK_OP_VERSION_CODIGO no fue encontrada en USER_CONSTRAINTS.');
    END IF;

    FOR r IN (
        SELECT c.INDEX_NAME, ic.COLUMN_POSITION, ic.COLUMN_NAME, ic.DESCEND
          FROM USER_CONSTRAINTS c
          JOIN USER_IND_COLUMNS ic
            ON ic.INDEX_NAME = c.INDEX_NAME
         WHERE c.TABLE_NAME = 'CAT_OBJETIVO_PEI'
           AND c.CONSTRAINT_NAME = 'UK_OP_VERSION_CODIGO'
         ORDER BY ic.COLUMN_POSITION
    ) LOOP
        v_columnas_encontradas := v_columnas_encontradas + 1;
        DBMS_OUTPUT.PUT_LINE(
            'INDEX_NAME=' || r.INDEX_NAME ||
            ';COLUMN_POSITION=' || TO_CHAR(r.COLUMN_POSITION) ||
            ';COLUMN_NAME=' || r.COLUMN_NAME ||
            ';DESCEND=' || r.DESCEND
        );
    END LOOP;
    IF v_columnas_encontradas = 0 THEN
        DBMS_OUTPUT.PUT_LINE('No se encontraron columnas de indice para UK_OP_VERSION_CODIGO.');
    END IF;
END;
/

PROMPT [005.1-diagnostic] Listando USER_ERRORS relevante, si existe...

DECLARE
    v_errores PLS_INTEGER := 0;
BEGIN
    FOR r IN (
        SELECT NAME, TYPE, SEQUENCE, LINE, POSITION, ATTRIBUTE, TEXT
          FROM USER_ERRORS
         WHERE NAME IN ('CAT_OBJETIVO_PEI_VERSION', 'CAT_OBJETIVO_PEI')
         ORDER BY NAME, SEQUENCE
    ) LOOP
        v_errores := v_errores + 1;
        DBMS_OUTPUT.PUT_LINE(
            'NAME=' || r.NAME ||
            ';TYPE=' || r.TYPE ||
            ';SEQUENCE=' || TO_CHAR(r.SEQUENCE) ||
            ';LINE=' || TO_CHAR(r.LINE) ||
            ';POSITION=' || TO_CHAR(r.POSITION) ||
            ';ATTRIBUTE=' || r.ATTRIBUTE ||
            ';TEXT=' || r.TEXT
        );
    END LOOP;
    IF v_errores = 0 THEN
        DBMS_OUTPUT.PUT_LINE('USER_ERRORS: sin filas para las tablas PEI.');
    END IF;
    DBMS_OUTPUT.PUT_LINE('Diagnóstico 005.1 completado; no se realizaron cambios.');
END;
/
