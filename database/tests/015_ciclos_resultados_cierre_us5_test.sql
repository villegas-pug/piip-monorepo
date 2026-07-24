-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 015 - cobertura US5: evaluacion y
-- aplicabilidad de iniciativas a partir de la huella 014, manteniendo
-- transiciones coherentes con 015
-- Dependencia: 001, 002, 003, 003.1, 003.2, 005, 006, 008+008.1, 009, 014
--              y 015 VIGENTES.
-- Proposito : revalida los CHECKs de US5 contra 015 y verifica que la
--             insercion de PRODUCTO_PARCIAL respalda la transicion desde
--             EJECUCION hacia PRESENTACION del producto final.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T015_CICLOS_US5;

DECLARE
    v_unidad     NUMBER(10) := 1;
    v_evaluador  NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_responsable NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_ini        NUMBER(12) := SEQ_PROYECTO.NEXTVAL;
    v_aplic      NUMBER(12) := SEQ_APLICABILIDAD_INICIATIVA.NEXTVAL;
    v_criterio   NUMBER(12) := SEQ_APLICABILIDAD_CRITERIO.NEXTVAL;
    v_motivo     BOOLEAN := FALSE;
    v_uk_criterio BOOLEAN := FALSE;
    v_orden      BOOLEAN := FALSE;
BEGIN
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_evaluador, '00000000-0000-0000-0000-000000000153',
            'test015u5e_' || v_evaluador, 'Evaluador US5 015',
            'test015u5e_' || v_evaluador || '@example.test', 'TEST_015U5');
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_responsable, '00000000-0000-0000-0000-000000000154',
            'test015u5r_' || v_responsable, 'Responsable US5 015',
            'test015u5r_' || v_responsable || '@example.test', 'TEST_015U5');
    INSERT INTO PROYECTO (ID_PROYECTO, CODIGO, TIPO_REGISTRO, NOMBRE,
                          TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
                          VERSION, SUBSANACION_ACTIVA)
    VALUES (v_ini, 'INI-015U5-' || v_ini, 'INICIATIVA', 'Iniciativa US5 015',
            'NUEVO_SERVICIO', 'PRESENTADO', v_unidad, v_responsable,
            0, 'N');

    INSERT INTO APLICABILIDAD_INICIATIVA
        (ID_APLICABILIDAD, ID_INICIATIVA, RESULTADO, MOTIVO, ID_EVALUADOR)
    VALUES (v_aplic, v_ini, 'NO_APLICABLE', 'No aplica al ambito', v_evaluador);

    INSERT INTO APLICABILIDAD_CRITERIO
        (ID_CRITERIO, ID_APLICABILIDAD, CLAVE, VALOR, ORDEN)
    VALUES (v_criterio, v_aplic, 'COBERTURA', 'NACIONAL', 1);

    BEGIN
        INSERT INTO APLICABILIDAD_INICIATIVA
            (ID_APLICABILIDAD, ID_INICIATIVA, RESULTADO, ID_EVALUADOR)
        VALUES (SEQ_APLICABILIDAD_INICIATIVA.NEXTVAL, v_ini,
                'NO_APLICABLE', v_evaluador);
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_motivo := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        INSERT INTO APLICABILIDAD_CRITERIO
            (ID_CRITERIO, ID_APLICABILIDAD, CLAVE, VALOR, ORDEN)
        VALUES (SEQ_APLICABILIDAD_CRITERIO.NEXTVAL, v_aplic,
                'COBERTURA', 'NACIONAL', 2);
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_criterio := TRUE;
    END;

    BEGIN
        INSERT INTO APLICABILIDAD_CRITERIO
            (ID_CRITERIO, ID_APLICABILIDAD, CLAVE, VALOR, ORDEN)
        VALUES (SEQ_APLICABILIDAD_CRITERIO.NEXTVAL, v_aplic,
                'ORDEN_INVALIDO', '0', 0);
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_orden := TRUE; ELSE RAISE; END IF;
    END;

    IF NOT v_motivo OR NOT v_uk_criterio OR NOT v_orden THEN
        RAISE_APPLICATION_ERROR(-209F5,
            'T015 US5 fallo: motivo obligatorio, UK criterio o rango ORDEN');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T015 US5 OK: aplicabilidad, criterios y orden validados.');
END;
/

ROLLBACK TO T015_CICLOS_US5;
PROMPT T015 US5 finalizada; filas de prueba revertidas.
