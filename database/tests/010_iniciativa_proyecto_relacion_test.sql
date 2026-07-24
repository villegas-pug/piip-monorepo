-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 010: vinculo inmutable iniciativa-proyecto
-- Dependencia: 001, 002, 003, 003.1, 003.2, 005, 006, 008+008.1, 009 y 010
--              VIGENTES.
-- Proposito : verifica UK por iniciativa, UK por proyecto, CHECK de
--             IDs distintos y FKs a PROYECTO.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T010_INICIATIVA_PROYECTO;

DECLARE
    v_unidad  NUMBER(10) := 1;
    v_usuario NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_ini     NUMBER(12) := SEQ_PROYECTO.NEXTVAL;
    v_proy1   NUMBER(12) := SEQ_PROYECTO.NEXTVAL;
    v_proy2   NUMBER(12) := SEQ_PROYECTO.NEXTVAL;
    v_rel1    NUMBER(12) := SEQ_INICIATIVA_PROYECTO.NEXTVAL;
    v_uk_ini  BOOLEAN := FALSE;
    v_uk_proy BOOLEAN := FALSE;
    v_dist    BOOLEAN := FALSE;
BEGIN
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_usuario, '00000000-0000-0000-0000-000000000101',
            'test010_' || v_usuario, 'Responsable prueba 010',
            'test010_' || v_usuario || '@example.test', 'TEST_010');

    INSERT INTO PROYECTO (ID_PROYECTO, CODIGO, TIPO_REGISTRO, NOMBRE,
                          TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
                          VERSION, SUBSANACION_ACTIVA)
    VALUES (v_ini, 'INI-TEST-' || v_ini, 'INICIATIVA', 'Iniciativa prueba 010',
            'NUEVO_SERVICIO', 'INICIATIVA_APROBADA', v_unidad, v_usuario,
            0, 'N');
    INSERT INTO PROYECTO (ID_PROYECTO, CODIGO, TIPO_REGISTRO, NOMBRE,
                          TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
                          VERSION, SUBSANACION_ACTIVA)
    VALUES (v_proy1, 'PRY-TEST-1-' || v_proy1, 'PROYECTO', 'Proyecto prueba 010-1',
            'NUEVO_SERVICIO', 'REGISTRADO', v_unidad, v_usuario,
            0, 'N');
    INSERT INTO PROYECTO (ID_PROYECTO, CODIGO, TIPO_REGISTRO, NOMBRE,
                          TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
                          VERSION, SUBSANACION_ACTIVA)
    VALUES (v_proy2, 'PRY-TEST-2-' || v_proy2, 'PROYECTO', 'Proyecto prueba 010-2',
            'NUEVO_SERVICIO', 'REGISTRADO', v_unidad, v_usuario,
            0, 'N');

    INSERT INTO INICIATIVA_PROYECTO
        (ID_RELACION, ID_INICIATIVA, ID_PROYECTO, CREADA_POR)
    VALUES (v_rel1, v_ini, v_proy1, 'TEST_010');

    BEGIN
        INSERT INTO INICIATIVA_PROYECTO
            (ID_RELACION, ID_INICIATIVA, ID_PROYECTO, CREADA_POR)
        VALUES (SEQ_INICIATIVA_PROYECTO.NEXTVAL, v_ini, v_proy2, 'TEST_010');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_ini := TRUE;
    END;

    BEGIN
        INSERT INTO INICIATIVA_PROYECTO
            (ID_RELACION, ID_INICIATIVA, ID_PROYECTO, CREADA_POR)
        VALUES (SEQ_INICIATIVA_PROYECTO.NEXTVAL, v_proy1, v_ini, 'TEST_010');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_proy := TRUE;
    END;

    BEGIN
        INSERT INTO INICIATIVA_PROYECTO
            (ID_RELACION, ID_INICIATIVA, ID_PROYECTO, CREADA_POR)
        VALUES (SEQ_INICIATIVA_PROYECTO.NEXTVAL, v_ini, v_ini, 'TEST_010');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_dist := TRUE; ELSE RAISE; END IF;
    END;

    IF NOT v_uk_ini OR NOT v_uk_proy OR NOT v_dist THEN
        RAISE_APPLICATION_ERROR(-209A0,
            'T010 fallo: UK iniciativa, UK proyecto o CHECK distintos no rechazo');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T010 OK: vinculo iniciativa-proyecto inmutable validado.');
END;
/

ROLLBACK TO T010_INICIATIVA_PROYECTO;
PROMPT T010 finalizada; filas de prueba revertidas.
