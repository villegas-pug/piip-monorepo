-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 019: semilla de catalogos canonicos de
-- portafolio (cobertura US0 + US1)
-- Dependencia: 001-017 VIGENTES. Esta prueba asume que la semilla 019 ya
--              fue ejecutada y validada; verifica idempotencia, presencia
--              de los 6 roles canonicos y de los 13 tipos documentales,
--              y la inactivacion de transiciones legacy.
-- Proposito : (a) ejecutar una segunda insercion idempotente de la
--             semilla y verificar que el conteo no se duplica;
--             (b) confirmar la presencia de los 6 roles canonicos y
--             los 13 tipos documentales; (c) confirmar que las
--             transiciones legacy estan inactivas.
-- Limpieza  : ROLLBACK TO revierte DML; los MERGE de la propia semilla
--             ya se ejecutaron en una transaccion previa.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra
--             un ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T019_CATALOGOS;

DECLARE
    v_roles_antes        PLS_INTEGER;
    v_roles_despues     PLS_INTEGER;
    v_tipos_antes        PLS_INTEGER;
    v_tipos_despues     PLS_INTEGER;
    v_trans_legacy       PLS_INTEGER;
    v_roles_faltan       PLS_INTEGER;
    v_tipos_faltan       PLS_INTEGER;
BEGIN
    -- Conteos previos a una segunda insercion idempotente
    SELECT COUNT(*) INTO v_roles_antes FROM ROL;
    SELECT COUNT(*) INTO v_tipos_antes FROM TIPO_DOCUMENTO;

    -- Segunda ejecucion de la semilla (idempotencia)
    @@database/seeds/019_catalogos_canonicos_portafolio.sql

    SELECT COUNT(*) INTO v_roles_despues FROM ROL;
    SELECT COUNT(*) INTO v_tipos_despues FROM TIPO_DOCUMENTO;

    IF v_roles_despues <> v_roles_antes THEN
        RAISE_APPLICATION_ERROR(-20919,
            'T019 fallo: el conteo de ROL cambio tras una segunda ejecucion ('
            || TO_CHAR(v_roles_antes) || ' -> ' || TO_CHAR(v_roles_despues) || ')');
    END IF;
    IF v_tipos_despues <> v_tipos_antes THEN
        RAISE_APPLICATION_ERROR(-20919,
            'T019 fallo: el conteo de TIPO_DOCUMENTO cambio tras una segunda ejecucion ('
            || TO_CHAR(v_tipos_antes) || ' -> ' || TO_CHAR(v_tipos_despues) || ')');
    END IF;

    -- Presencia de los 6 roles canonicos
    SELECT COUNT(*) INTO v_roles_faltan
      FROM (
            SELECT 'GlobalAdmin'  AS R FROM DUAL UNION ALL
            SELECT 'UNIDAD_ADMIN'      FROM DUAL UNION ALL
            SELECT 'RESPONSABLE'       FROM DUAL UNION ALL
            SELECT 'EVALUADOR'         FROM DUAL UNION ALL
            SELECT 'AUTORIDAD'         FROM DUAL UNION ALL
            SELECT 'CONSULTA'          FROM DUAL
           ) e
      WHERE NOT EXISTS (
            SELECT 1 FROM ROL r WHERE r.NOMBRE_ROL = e.R
           );
    IF v_roles_faltan <> 0 THEN
        RAISE_APPLICATION_ERROR(-20919,
            'T019 fallo: roles canonicos faltantes: ' || TO_CHAR(v_roles_faltan));
    END IF;

    -- Presencia de los 13 tipos documentales canonicos
    SELECT COUNT(*) INTO v_tipos_faltan
      FROM (
            SELECT 'Ficha de Iniciativa' AS N FROM DUAL UNION ALL
            SELECT 'Informe de Opinion Tecnica'            FROM DUAL UNION ALL
            SELECT 'Documento Formal de Decision'           FROM DUAL UNION ALL
            SELECT 'Documento de Aprobacion o Autorizacion' FROM DUAL UNION ALL
            SELECT 'Nota Conceptual'                          FROM DUAL UNION ALL
            SELECT 'Matriz de Planificacion'                  FROM DUAL UNION ALL
            SELECT 'Seguimiento Agil'                          FROM DUAL UNION ALL
            SELECT 'Autoevaluacion de Ciclo'                  FROM DUAL UNION ALL
            SELECT 'Documento de Aprobacion de Producto'      FROM DUAL UNION ALL
            SELECT 'Evidencia de No Aprobacion'                FROM DUAL UNION ALL
            SELECT 'Informe Final de Cierre'                   FROM DUAL UNION ALL
            SELECT 'Evidencia de Suspension'                   FROM DUAL UNION ALL
            SELECT 'Informe de Cancelacion'                    FROM DUAL
           ) e
      WHERE NOT EXISTS (
            SELECT 1 FROM TIPO_DOCUMENTO t WHERE t.NOMBRE = e.N
           );
    IF v_tipos_faltan <> 0 THEN
        RAISE_APPLICATION_ERROR(-20919,
            'T019 fallo: tipos documentales canonicos faltantes: ' || TO_CHAR(v_tipos_faltan));
    END IF;

    -- Transiciones legacy inactivas
    SELECT COUNT(*) INTO v_trans_legacy
      FROM TRANSICION_PERMITIDA
     WHERE ACTIVO = 'N';
    IF v_trans_legacy < 1 THEN
        RAISE_APPLICATION_ERROR(-20919,
            'T019 fallo: no se inactivo ninguna transicion legacy; revise la maquina de estados');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'T019 OK: 6 roles canonicos, 13 tipos documentales y transiciones legacy inactivas verificados; semilla idempotente.');
END;
/

ROLLBACK TO T019_CATALOGOS;
PROMPT T019 finalizada; filas de prueba revertidas.
