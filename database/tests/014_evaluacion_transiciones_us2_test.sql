-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 014: evaluacion, subsanacion y
-- aplicabilidad de iniciativas (cobertura US2 + US5)
-- Dependencia: 001, 002, 003, 003.1, 003.2, 005, 005.1, 006, 007, 008+008.1,
--              009, 010, 011, 012, 013 y 014 VIGENTES (014 incluye 014.1 si
--              la ejecucion original fallo con ORA-02436).
-- Proposito : verifica UK unica por iniciativa en EVALUACION_INICIATIVA,
--             SUBSANACION_INICIATIVA y APLICABILIDAD_INICIATIVA, la
--             invariante determinista CK_SI_PLAZO (PLAZO > APERTURA_EN)
--             y los CHECKs de dominio y obligatoriedad de motivo.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T014_EVALUACION;

DECLARE
    v_unidad  NUMBER(10) := 1;
    v_evaluador NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_ini       NUMBER(12) := SEQ_PROYECTO.NEXTVAL;
    v_ini2      NUMBER(12) := SEQ_PROYECTO.NEXTVAL;
    v_evaluacion NUMBER(12) := SEQ_EVALUACION_INICIATIVA.NEXTVAL;
    v_subs       NUMBER(12) := SEQ_SUBSANACION_INICIATIVA.NEXTVAL;
    v_aplic      NUMBER(12) := SEQ_APLICABILIDAD_INICIATIVA.NEXTVAL;
    v_uk_ei     BOOLEAN := FALSE;
    v_uk_si     BOOLEAN := FALSE;
    v_uk_ai     BOOLEAN := FALSE;
    v_motivo    BOOLEAN := FALSE;
    v_plazo_ok  BOOLEAN := FALSE;
    v_plazo_fail BOOLEAN := FALSE;
    v_apertura  TIMESTAMP(6);
    v_plazo_posterior DATE;
    v_plazo_anterior  DATE;
BEGIN
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_evaluador, '00000000-0000-0000-0000-000000000141',
            'test014_' || v_evaluador, 'Evaluador prueba 014',
            'test014_' || v_evaluador || '@example.test', 'TEST_014');

    INSERT INTO PROYECTO (ID_PROYECTO, CODIGO, TIPO_REGISTRO, NOMBRE,
                          TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
                          VERSION, SUBSANACION_ACTIVA)
    VALUES (v_ini, 'INI-014-' || v_ini, 'INICIATIVA', 'Iniciativa 014',
            'NUEVO_SERVICIO', 'PRESENTADO', v_unidad, v_evaluador,
            0, 'N');
    INSERT INTO PROYECTO (ID_PROYECTO, CODIGO, TIPO_REGISTRO, NOMBRE,
                          TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
                          VERSION, SUBSANACION_ACTIVA)
    VALUES (v_ini2, 'INI-014B-' || v_ini2, 'INICIATIVA', 'Iniciativa 014-B',
            'NUEVO_SERVICIO', 'PRESENTADO', v_unidad, v_evaluador,
            0, 'N');

    INSERT INTO EVALUACION_INICIATIVA
        (ID_EVALUACION, ID_INICIATIVA, ID_EVALUADOR, ID_ROL_EFECTIVO, ID_UNIDAD_EFECTIVA)
    VALUES (v_evaluacion, v_ini, v_evaluador, 1, v_unidad);

    -- Apertura explicita para poder probar la invariante determinista
    -- PLAZO > APERTURA_EN sin depender del reloj del servidor.
    v_apertura := SYSTIMESTAMP;
    v_plazo_posterior := TRUNC(SYSDATE) + 7;
    v_plazo_anterior  := TRUNC(SYSDATE) - 1;

    INSERT INTO SUBSANACION_INICIATIVA
        (ID_SUBSANACION, ID_INICIATIVA, PLAZO, INCUMPLIMIENTOS, APERTURA_EN, ID_ACTOR)
    VALUES (v_subs, v_ini, v_plazo_posterior, 'Campo 5 incompleto', v_apertura, v_evaluador);
    v_plazo_ok := TRUE;

    BEGIN
        INSERT INTO SUBSANACION_INICIATIVA
            (ID_SUBSANACION, ID_INICIATIVA, PLAZO, INCUMPLIMIENTOS, APERTURA_EN, ID_ACTOR)
        VALUES (SEQ_SUBSANACION_INICIATIVA.NEXTVAL, v_ini,
                v_plazo_anterior, 'Plazo anterior a apertura', v_apertura, v_evaluador);
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -2290 THEN
                v_plazo_fail := TRUE;
            ELSE
                RAISE;
            END IF;
    END;

    INSERT INTO APLICABILIDAD_INICIATIVA
        (ID_APLICABILIDAD, ID_INICIATIVA, RESULTADO, MOTIVO, ID_EVALUADOR)
    VALUES (v_aplic, v_ini, 'NO_APLICABLE', 'No aplica al sector', v_evaluador);

    BEGIN
        INSERT INTO EVALUACION_INICIATIVA
            (ID_EVALUACION, ID_INICIATIVA, ID_EVALUADOR)
        VALUES (SEQ_EVALUACION_INICIATIVA.NEXTVAL, v_ini, v_evaluador);
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_ei := TRUE;
    END;

    BEGIN
        INSERT INTO SUBSANACION_INICIATIVA
            (ID_SUBSANACION, ID_INICIATIVA, PLAZO, INCUMPLIMIENTOS, APERTURA_EN, ID_ACTOR)
        VALUES (SEQ_SUBSANACION_INICIATIVA.NEXTVAL, v_ini,
                v_plazo_posterior, 'Otro incumplimiento', v_apertura, v_evaluador);
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_si := TRUE;
    END;

    BEGIN
        INSERT INTO APLICABILIDAD_INICIATIVA
            (ID_APLICABILIDAD, ID_INICIATIVA, RESULTADO, ID_EVALUADOR)
        VALUES (SEQ_APLICABILIDAD_INICIATIVA.NEXTVAL, v_ini2,
                'NO_APLICABLE', v_evaluador);
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_motivo := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        INSERT INTO APLICABILIDAD_INICIATIVA
            (ID_APLICABILIDAD, ID_INICIATIVA, RESULTADO, MOTIVO, ID_EVALUADOR)
        VALUES (SEQ_APLICABILIDAD_INICIATIVA.NEXTVAL, v_ini2,
                'APLICABLE', 'No deberia ser motivo obligatorio', v_evaluador);
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_ai := TRUE;
    END;

    IF NOT v_uk_ei OR NOT v_uk_si OR NOT v_uk_ai
       OR NOT v_motivo OR NOT v_plazo_ok OR NOT v_plazo_fail THEN
        RAISE_APPLICATION_ERROR(-20900,
            'T014 fallo: UK evaluacion/subsanacion/aplicabilidad, motivo obligatorio o CK_SI_PLAZO');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T014 OK: US2 evaluacion + US5 aplicabilidad/subsanacion validadas, CK_SI_PLAZO determinista OK.');
END;
/

ROLLBACK TO T014_EVALUACION;
PROMPT T014 finalizada; filas de prueba revertidas.
