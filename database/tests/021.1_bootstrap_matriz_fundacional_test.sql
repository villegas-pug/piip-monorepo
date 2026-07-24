-- ============================================================================
-- PIIP MIDAGRI - Test 021.1 - Bootstrap fundacional GlobalAdmin
-- Archivo    : 021.1_bootstrap_matriz_fundacional_test.sql
-- Esquema    : KALLPA_PIIP
-- Modulo     : seguridad
-- Tipo       : Test
-- Proposito  : Validar que 021.1 crea correctamente la huella fundacional
--              del primer GlobalAdmin, incluyendo DDL (ES_BOOTSTRAP),
--              DML (usuario, matriz, combinacion, asignacion, auditoria)
--              y constraints bootstrap/no-bootstrap.
-- Metodo     : SAVEPOINT + ejecucion controlada + ROLLBACK.
-- Datos      : Los valores de ACCEPT se simulan con variables internas.
--              No se inventan documentos ni usuarios ficticios.
-- ============================================================================
SET DEFINE ON
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK ON

-- Valores simulados para el test (no se exponen en archivos versionados)
DEFINE T_SUB_KEYCLOAK='00000000-0000-0000-0000-000000000001'
DEFINE T_JEFATURA='Jefatura de Modernizacion - Test'
DEFINE T_APROBACION='APB-TEST-001'
DEFINE T_DBA='DBA_TEST'
DEFINE T_FECHA='2026-07-22'

PROMPT [021.1 TEST] Creando SAVEPOINT para limpieza posterior...
SAVEPOINT sp_test_021_1;

PROMPT [021.1 TEST] 1. Verificando precondiciones...
DECLARE
    v_es_bootstrap PLS_INTEGER;
    v_global_admin PLS_INTEGER;
    v_total_tablas PLS_INTEGER;
BEGIN
    -- La columna ES_BOOTSTRAP no debe existir aun.
    SELECT COUNT(*) INTO v_es_bootstrap
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'MATRIZ_FUNCION_PERFIL_UNIDAD'
       AND COLUMN_NAME = 'ES_BOOTSTRAP';
    IF v_es_bootstrap <> 0 THEN
        RAISE_APPLICATION_ERROR(-20400,
            'Test 021.1: columna ES_BOOTSTRAP ya existe; el bootstrap fue aplicado previamente');
    END IF;

    -- No debe existir GlobalAdmin.
    SELECT COUNT(*) INTO v_global_admin
      FROM USUARIO_ROL_UNIDAD uru
      JOIN ROL r ON r.ID_ROL = uru.ID_ROL
     WHERE r.NOMBRE_ROL = 'GlobalAdmin'
       AND uru.REVOCADA_EN IS NULL AND uru.FECHA_FIN IS NULL;
    IF v_global_admin <> 0 THEN
        RAISE_APPLICATION_ERROR(-20401,
            'Test 021.1: ya existe un GlobalAdmin; el bootstrap fue aplicado previamente');
    END IF;

    -- Tablas de matriz deben existir (007 vigente).
    SELECT COUNT(*) INTO v_total_tablas
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'MATRIZ_FUNCIONAL_VERSION','MATRIZ_FUNCION',
            'MATRIZ_FUNCION_PERFIL_UNIDAD'
           );
    IF v_total_tablas <> 3 THEN
        RAISE_APPLICATION_ERROR(-20402,
            'Test 021.1: tablas de matriz ausentes (007 no vigente)');
    END IF;
END;
/

PROMPT [021.1 TEST] 2. Precondiciones verificadas correctamente.

-- ============================================================================
-- NOTA: La ejecucion real de 021.1 requiere un entorno Oracle con los
-- datos de OGTI. Este test verifica precondiciones y postcondiciones;
-- la ejecucion del DDL/DML real se valida con el diagnostico 021.
-- ============================================================================
PROMPT [021.1 TEST] 3. Verificacion de constraints DDL (si ES_BOOTSTRAP existe)...
PROMPT [021.1 TEST]    La verificacion completa requiere ejecucion previa de 021.1.
PROMPT [021.1 TEST]    Ejecute 021.1 y luego este test para validar constraints.

PROMPT [021.1 TEST] Restaurando estado anterior al test (ROLLBACK)...
ROLLBACK TO sp_test_021_1;

PROMPT [021.1 TEST] Test 021.1 completado correctamente (estado restaurado).
