-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 012: participantes persona y unidad
-- Dependencia: 001, 002, 003, 003.1, 003.2, 005, 006, 008+008.1, 009, 011
--              y 012 VIGENTES.
-- Proposito : verifica CHECK de datos minimos en PARTICIPANTE_PERSONA, UK
--             (proyecto,participante) y FKs a PROYECTO, USUARIO y
--             UNIDAD_EJECUTORA.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T012_PARTICIPANTES;

DECLARE
    v_unidad  NUMBER(10) := 1;
    v_actor   NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_u1      NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_proy    NUMBER(12) := SEQ_PROYECTO.NEXTVAL;
    v_part    NUMBER(12) := SEQ_PARTICIPANTE_PERSONA.NEXTVAL;
    v_part2   NUMBER(12) := SEQ_PARTICIPANTE_PERSONA.NEXTVAL;
    v_relp    NUMBER(12) := SEQ_PROY_PART_PERSONA.NEXTVAL;
    v_relu    NUMBER(12) := SEQ_PROY_PART_UNIDAD.NEXTVAL;
    v_uk_pers BOOLEAN := FALSE;
    v_uk_uni  BOOLEAN := FALSE;
    v_minimos BOOLEAN := FALSE;
BEGIN
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_actor, '00000000-0000-0000-0000-000000000121',
            'test012a_' || v_actor, 'Responsable prueba 012',
            'test012a_' || v_actor || '@example.test', 'TEST_012');
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_u1, '00000000-0000-0000-0000-000000000122',
            'test012b_' || v_u1, 'Persona externa 012',
            'test012b_' || v_u1 || '@example.test', 'TEST_012');
    INSERT INTO PROYECTO (ID_PROYECTO, CODIGO, TIPO_REGISTRO, NOMBRE,
                          TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
                          VERSION, SUBSANACION_ACTIVA)
    VALUES (v_proy, 'PRY-012-' || v_proy, 'PROYECTO', 'Proyecto prueba 012',
            'NUEVO_SERVICIO', 'REGISTRADO', v_unidad, v_actor,
            0, 'N');

    -- Participante con USUARIO
    INSERT INTO PARTICIPANTE_PERSONA
        (ID_PARTICIPANTE, ID_USUARIO, NOMBRES_COMPLETOS, INSTITUCION, FUNCION)
    VALUES (v_part, v_u1, 'Persona externa 012', 'ORG EXTERNA', 'CONSULTOR');

    -- Participante sin USUARIO con NOMBRES_COMPLETOS (datos minimos)
    INSERT INTO PARTICIPANTE_PERSONA
        (ID_PARTICIPANTE, ID_USUARIO, NOMBRES_COMPLETOS, INSTITUCION, FUNCION)
    VALUES (v_part2, NULL, 'Invitado sin usuario 012', 'OTRA', 'ASESOR');

    INSERT INTO PROYECTO_PARTICIPANTE_PERSONA
        (ID_PROY_PART_PERSONA, ID_PROYECTO, ID_PARTICIPANTE, INICIO, ID_ACTOR, CREADO_POR)
    VALUES (v_relp, v_proy, v_part, TRUNC(SYSDATE), v_actor, 'TEST_012');

    INSERT INTO PROYECTO_PARTICIPANTE_UNIDAD
        (ID_PROY_PART_UNIDAD, ID_PROYECTO, ID_UNIDAD, INICIO, ID_ACTOR, CREADO_POR)
    VALUES (v_relu, v_proy, v_unidad, TRUNC(SYSDATE), v_actor, 'TEST_012');

    BEGIN
        INSERT INTO PROYECTO_PARTICIPANTE_PERSONA
            (ID_PROY_PART_PERSONA, ID_PROYECTO, ID_PARTICIPANTE, INICIO, ID_ACTOR, CREADO_POR)
        VALUES (SEQ_PROY_PART_PERSONA.NEXTVAL, v_proy, v_part,
                TRUNC(SYSDATE), v_actor, 'TEST_012');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_pers := TRUE;
    END;

    BEGIN
        INSERT INTO PROYECTO_PARTICIPANTE_UNIDAD
            (ID_PROY_PART_UNIDAD, ID_PROYECTO, ID_UNIDAD, INICIO, ID_ACTOR, CREADO_POR)
        VALUES (SEQ_PROY_PART_UNIDAD.NEXTVAL, v_proy, v_unidad,
                TRUNC(SYSDATE), v_actor, 'TEST_012');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_uni := TRUE;
    END;

    BEGIN
        INSERT INTO PARTICIPANTE_PERSONA
            (ID_PARTICIPANTE, ID_USUARIO, NOMBRES_COMPLETOS)
        VALUES (SEQ_PARTICIPANTE_PERSONA.NEXTVAL, NULL, NULL);
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_minimos := TRUE; ELSE RAISE; END IF;
    END;

    IF NOT v_uk_pers OR NOT v_uk_uni OR NOT v_minimos THEN
        RAISE_APPLICATION_ERROR(-2090, 'T012 fallo: UK persona, UK unidad o CHECK datos minimos no rechazo');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T012 OK: participantes, datos minimos y vigencias validados.');
END;
/

ROLLBACK TO T012_PARTICIPANTES;
PROMPT T012 finalizada; filas de prueba revertidas.
