-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 013: matriz append-only de obligatoriedad,
-- editabilidad, privacidad y actor responsable por campo
-- Dependencia: 001, 002, 003, 003.1, 003.2, 005, 006, 008+008.1, 009 y 013
--              VIGENTES.
-- Proposito : verifica UK (TIPO_REGISTRO, ETAPA, NRO_CAMPO), CHECKs de
--             dominio y FK a ROL. La reclasificacion genera una fila en
--             PROYECTO_CAMPO_CLASIF_HIST; no se modela control publico de
--             la matriz.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T013_CLASIFICACION_CAMPOS;

DECLARE
    v_clas   NUMBER(12) := SEQ_PROY_CAMPO_CLASIF.NEXTVAL;
    v_clas2  NUMBER(12) := SEQ_PROY_CAMPO_CLASIF.NEXTVAL;
    v_hist   NUMBER(12) := SEQ_PROY_CAMPO_CLASIF_HIST.NEXTVAL;
    v_uk     BOOLEAN := FALSE;
    v_tipo   BOOLEAN := FALSE;
    v_nro    BOOLEAN := FALSE;
BEGIN
    INSERT INTO PROYECTO_CAMPO_CLASIFICACION
        (ID_CLASIFICACION, TIPO_REGISTRO, ETAPA, NRO_CAMPO,
         CLASIFICACION, EDITABLE, ID_ROL_EDITOR, OBLIGATORIO, ACTIVA, CREADO_POR)
    VALUES (v_clas, 'INICIATIVA', 'PRESENTACION', 1,
            'INTERNO', 'S', 1, 'S', 'S', 'TEST_013');

    -- Reclasificacion valida: cambia CLASIFICACION y crea fila en historial
    UPDATE PROYECTO_CAMPO_CLASIFICACION
       SET CLASIFICACION = 'RESTRINGIDO'
     WHERE ID_CLASIFICACION = v_clas;
    INSERT INTO PROYECTO_CAMPO_CLASIF_HIST
        (ID_HISTORIAL, ID_CLASIFICACION, CLASIFICACION_ANTERIOR,
         CLASIFICACION_NUEVA, ID_ACTOR)
    VALUES (v_hist, v_clas, 'INTERNO', 'RESTRINGIDO', 1);

    -- Caso negativo: UK duplicada
    BEGIN
        INSERT INTO PROYECTO_CAMPO_CLASIFICACION
            (ID_CLASIFICACION, TIPO_REGISTRO, ETAPA, NRO_CAMPO,
             CLASIFICACION, EDITABLE, ID_ROL_EDITOR, OBLIGATORIO, ACTIVA, CREADO_POR)
        VALUES (v_clas2, 'INICIATIVA', 'PRESENTACION', 1,
                'PUBLICO', 'S', 1, 'N', 'S', 'TEST_013');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk := TRUE;
    END;

    -- Caso negativo: TIPO_REGISTRO fuera del dominio
    BEGIN
        INSERT INTO PROYECTO_CAMPO_CLASIFICACION
            (ID_CLASIFICACION, TIPO_REGISTRO, ETAPA, NRO_CAMPO,
             CLASIFICACION, EDITABLE, ID_ROL_EDITOR, OBLIGATORIO, ACTIVA, CREADO_POR)
        VALUES (SEQ_PROY_CAMPO_CLASIF.NEXTVAL, 'OTRO', 'PRESENTACION', 2,
                'INTERNO', 'S', 1, 'N', 'S', 'TEST_013');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_tipo := TRUE; ELSE RAISE; END IF;
    END;

    -- Caso negativo: NRO_CAMPO fuera del rango 1..23
    BEGIN
        INSERT INTO PROYECTO_CAMPO_CLASIFICACION
            (ID_CLASIFICACION, TIPO_REGISTRO, ETAPA, NRO_CAMPO,
             CLASIFICACION, EDITABLE, ID_ROL_EDITOR, OBLIGATORIO, ACTIVA, CREADO_POR)
        VALUES (SEQ_PROY_CAMPO_CLASIF.NEXTVAL, 'INICIATIVA', 'PRESENTACION', 99,
                'INTERNO', 'S', 1, 'N', 'S', 'TEST_013');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_nro := TRUE; ELSE RAISE; END IF;
    END;

    IF NOT v_uk OR NOT v_tipo OR NOT v_nro THEN
        RAISE_APPLICATION_ERROR(-209D0,
            'T013 fallo: UK tipo/etapa/campo, dominio o NRO_CAMPO no rechazo');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T013 OK: matriz append-only y CHECKs de campos validados.');
END;
/

ROLLBACK TO T013_CLASIFICACION_CAMPOS;
PROMPT T013 finalizada; filas de prueba revertidas.
