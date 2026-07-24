-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 011: titularidad vigente por proyecto
-- Dependencia: 001, 002, 003, 003.1, 003.2, 005, 006, 008+008.1, 009 y 011
--              VIGENTES.
-- Proposito : verifica FKs a PROYECTO y USUARIO, indice unico funcional
--             del titular activo y CHECK de vigencia.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T011_PROYECTO_RESPONSABLE;

DECLARE
    v_unidad  NUMBER(10) := 1;
    v_u1      NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_u2      NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_proy    NUMBER(12) := SEQ_PROYECTO.NEXTVAL;
    v_t1      NUMBER(12) := SEQ_PROYECTO_RESPONSABLE.NEXTVAL;
    v_t2      NUMBER(12) := SEQ_PROYECTO_RESPONSABLE.NEXTVAL;
    v_activo  BOOLEAN := FALSE;
    v_vigencia BOOLEAN := FALSE;
BEGIN
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_u1, '00000000-0000-0000-0000-000000000111',
            'test011a_' || v_u1, 'Titular prueba 011',
            'test011a_' || v_u1 || '@example.test', 'TEST_011');
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_u2, '00000000-0000-0000-0000-000000000112',
            'test011b_' || v_u2, 'Suplente prueba 011',
            'test011b_' || v_u2 || '@example.test', 'TEST_011');
    INSERT INTO PROYECTO (ID_PROYECTO, CODIGO, TIPO_REGISTRO, NOMBRE,
                          TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
                          VERSION, SUBSANACION_ACTIVA)
    VALUES (v_proy, 'PRY-011-' || v_proy, 'PROYECTO', 'Proyecto prueba 011',
            'NUEVO_SERVICIO', 'REGISTRADO', v_unidad, v_u1,
            0, 'N');

    INSERT INTO PROYECTO_RESPONSABLE
        (ID_TITULARIDAD, ID_PROYECTO, ID_USUARIO, INICIO, CREADO_POR)
    VALUES (v_t1, v_proy, v_u1, TRUNC(SYSDATE), 'TEST_011');

    -- Caso positivo: titular cerrado (FIN no nulo) para el mismo proyecto
    INSERT INTO PROYECTO_RESPONSABLE
        (ID_TITULARIDAD, ID_PROYECTO, ID_USUARIO, INICIO, FIN, CREADO_POR)
    VALUES (v_t2, v_proy, v_u2,
            TRUNC(SYSDATE) - 30, TRUNC(SYSDATE) - 1, 'TEST_011');

    BEGIN
        INSERT INTO PROYECTO_RESPONSABLE
            (ID_TITULARIDAD, ID_PROYECTO, ID_USUARIO, INICIO, CREADO_POR)
        VALUES (SEQ_PROYECTO_RESPONSABLE.NEXTVAL, v_proy, v_u2,
                TRUNC(SYSDATE) + 1, 'TEST_011');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_activo := TRUE;
    END;

    BEGIN
        INSERT INTO PROYECTO_RESPONSABLE
            (ID_TITULARIDAD, ID_PROYECTO, ID_USUARIO, INICIO, FIN, CREADO_POR)
        VALUES (SEQ_PROYECTO_RESPONSABLE.NEXTVAL, v_proy, v_u1,
                TRUNC(SYSDATE), TRUNC(SYSDATE) - 1, 'TEST_011');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_vigencia := TRUE; ELSE RAISE; END IF;
    END;

    IF NOT v_activo OR NOT v_vigencia THEN
        RAISE_APPLICATION_ERROR(-209B0,
            'T011 fallo: titular activo unico o vigencia no rechazo');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T011 OK: titularidad vigente, UX titular activo y CHECK de vigencia validados.');
END;
/

ROLLBACK TO T011_PROYECTO_RESPONSABLE;
PROMPT T011 finalizada; filas de prueba revertidas.
