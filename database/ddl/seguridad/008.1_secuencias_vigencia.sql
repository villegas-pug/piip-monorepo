-- ============================================================================
-- PIIP MIDAGRI - Correccion forward-only 008.1 - Secuencias de vigencia
-- Archivo    : 008.1_secuencias_vigencia.sql
-- Esquema    : KALLPA_PIIP
-- Modulo     : seguridad
-- Dependencia: huella parcial confirmada de
--              008_usuario_rol_unidad_vigencia.sql.
--
-- Causa: la ejecucion manual de 008 alcanzo el ultimo DDL confirmado
--        (IDX_OA_USUARIO_OBJETIVO) y fallo en su validacion final porque
--        omitio CREATE SEQUENCE para SEQ_URU_EVENTO,
--        SEQ_SUPLENCIA_FUNCIONAL y SEQ_OPERACION_APROVISIONAMIENTO.
--
-- Alcance: correccion forward-only limitada exclusivamente a las tres
--          secuencias omitidas. No recrea, modifica ni deshace tablas,
--          columnas, constraints o indices ya confirmados parcialmente.
--          El script 008 NO debe reejecutarse.
--
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: no eliminar secuencias; detener nuevas
--            operaciones de asignacion/suplencia y conservar historial.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [008.1] Validando huella parcial confirmada del incremento 008...

-- ----------------------------------------------------------------------------
-- Todas las validaciones preceden al primer DDL. La huella debe coincidir
-- exactamente con el estado parcial producido por 008 antes de su bloque de
-- validacion final.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    -- Las tres tablas creadas por 008 deben existir, sin sustituciones.
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'USUARIO_ROL_UNIDAD_EVENTO',
            'SUPLENCIA_FUNCIONAL',
            'OPERACION_APROVISIONAMIENTO'
           );
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20050,
            'Precondicion 008.1: faltan o sobran tablas de la huella parcial 008');
    END IF;

    -- PKs requeridas de las tres tablas nuevas.
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE CONSTRAINT_NAME IN (
            'PK_USUARIO_ROL_UNIDAD_EVENTO',
            'PK_SUPLENCIA_FUNCIONAL',
            'PK_OPERACION_APROVISIONAMIENTO'
           )
       AND CONSTRAINT_TYPE = 'P'
       AND STATUS = 'ENABLED'
       AND VALIDATED = 'VALIDATED';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20051,
            'Precondicion 008.1: PKs de la huella parcial 008 incompletas');
    END IF;

    -- Las nueve columnas de vigencia agregadas a USUARIO_ROL_UNIDAD deben
    -- existir antes de crear las secuencias omitidas.
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'USUARIO_ROL_UNIDAD'
       AND COLUMN_NAME IN (
            'FECHA_INICIO','FECHA_FIN','REVOCADA_EN','REVOCADA_POR',
            'MOTIVO_REVOCACION','INACTIVA_TEMPORALMENTE',
            'ID_COMBINACION_MATRIZ','ID_DOCUMENTO_FORMAL','VERSION'
           );
    IF v_total <> 9 THEN
        RAISE_APPLICATION_ERROR(-20052,
            'Precondicion 008.1: columnas de vigencia en USUARIO_ROL_UNIDAD incompletas');
    END IF;

    -- FKs creadas por 008: contexto efectivo, asignacion versionada,
    -- eventos, suplencias y aprovisionamiento.
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE CONSTRAINT_NAME IN (
            'FK_AA_ROL_EFECTIVO','FK_AA_UNIDAD_EFECTIVA',
            'FK_URU_COMBINACION','FK_URU_DOCUMENTO_FORMAL',
            'FK_URUE_ASIGNACION','FK_URUE_USUARIO_ACTOR',
            'FK_SF_TITULAR','FK_SF_SUPLENTE','FK_SF_AUTORIDAD','FK_SF_DOCUMENTO',
            'FK_OA_USUARIO_OBJETIVO'
           )
       AND CONSTRAINT_TYPE = 'R'
       AND STATUS = 'ENABLED';
    IF v_total <> 11 THEN
        RAISE_APPLICATION_ERROR(-20053,
            'Precondicion 008.1: FKs de la huella parcial 008 incompletas');
    END IF;

    -- Indices finales relevantes. Se exige de manera expresa el ultimo DDL
    -- confirmado por el DBA: IDX_OA_USUARIO_OBJETIVO.
    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME IN (
            'UX_URU_ABIERTAS','IDX_URU_COMBINACION_MATRIZ',
            'IDX_URUE_ASIGNACION','IDX_URUE_FECHA',
            'IDX_SF_TITULAR','IDX_SF_SUPLENTE',
            'IDX_OA_ESTADO','IDX_OA_USUARIO_OBJETIVO'
           )
       AND STATUS = 'VALID';
    IF v_total <> 8 THEN
        RAISE_APPLICATION_ERROR(-20054,
            'Precondicion 008.1: indices de la huella parcial 008 incompletos');
    END IF;

    -- Las tres secuencias omitidas deben estar ausentes. Su presencia indica
    -- una correccion previa, parcial o externa y requiere revision humana.
    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_URU_EVENTO',
            'SEQ_SUPLENCIA_FUNCIONAL',
            'SEQ_OPERACION_APROVISIONAMIENTO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20055,
            'Precondicion 008.1: una o mas secuencias de vigencia ya existen');
    END IF;
END;
/

PROMPT [008.1] Huella parcial validada. Creando exclusivamente las secuencias omitidas...

-- Cada CREATE SEQUENCE confirma implicitamente en Oracle.
CREATE SEQUENCE SEQ_URU_EVENTO
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_SUPLENCIA_FUNCIONAL
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_OPERACION_APROVISIONAMIENTO
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

PROMPT [008.1] Validando secuencias creadas...
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_URU_EVENTO',
            'SEQ_SUPLENCIA_FUNCIONAL',
            'SEQ_OPERACION_APROVISIONAMIENTO'
           )
       AND INCREMENT_BY = 1
       AND CACHE_SIZE = 0
       AND CYCLE_FLAG = 'N';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20056,
            'Validacion 008.1: secuencias de vigencia ausentes o con atributos incompatibles');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: correccion forward-only 008.1 aplicada correctamente.');
END;
/

-- El COMMIT final es solo consistencia documental; los DDL Oracle ya
-- confirmaron implicitamente cada secuencia creada.
COMMIT;

PROMPT Correccion 008.1_secuencias_vigencia completada correctamente.
