-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 016: incorporacion individual con
-- conflictos y trazabilidad append-only
-- Dependencia: 001, 002, 003, 003.1, 003.2, 005, 006, 008+008.1, 009, 010,
--              012 y 016 VIGENTES.
-- Proposito : verifica UK idempotente (HASH_ORIGINAL + ID_RESPONSABLE +
--             FUENTE), CHECKs de ESTADO y SHA-256, CHECK de transicion
--             RESUELTO coherente, UK de conflictos y FKs a PROYECTO.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T016_INCORPORACION;

DECLARE
    v_unidad  NUMBER(10) := 1;
    v_usuario NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_doc     NUMBER(12) := SEQ_DOCUMENTO.NEXTVAL;
    v_proy1   NUMBER(12) := SEQ_PROYECTO.NEXTVAL;
    v_proy2   NUMBER(12) := SEQ_PROYECTO.NEXTVAL;
    v_incorp  NUMBER(12) := SEQ_INCORPORACION_REGISTRO.NEXTVAL;
    v_cambio  NUMBER(12) := SEQ_INCORPORACION_CAMBIO.NEXTVAL;
    v_conflicto NUMBER(12) := SEQ_INCORPORACION_CONFLICTO.NEXTVAL;
    v_uk_inc  BOOLEAN := FALSE;
    v_hash    BOOLEAN := FALSE;
    v_estado  BOOLEAN := FALSE;
    v_resuelto BOOLEAN := FALSE;
    v_uk_conf BOOLEAN := FALSE;
BEGIN
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_usuario, '00000000-0000-0000-0000-000000000161',
            'test016_' || v_usuario, 'Responsable 016',
            'test016_' || v_usuario || '@example.test', 'TEST_016');

    INSERT INTO DOCUMENTO
        (ID_DOCUMENTO, ID_TIPO_DOC, NOMBRE_ORIGINAL, MIME_TYPE, TAMANO_BYTES,
         HASH_SHA256, ID_USUARIO_CARGA, ID_DOCUMENTO_SERIE, NUMERO_VERSION)
    VALUES (v_doc, 1, 'fuente016.pdf', 'application/pdf', 1,
            RPAD('6', 64, '6'), v_usuario, 1, 1);

    INSERT INTO PROYECTO (ID_PROYECTO, CODIGO, TIPO_REGISTRO, NOMBRE,
                          TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
                          VERSION, SUBSANACION_ACTIVA)
    VALUES (v_proy1, 'PRY-016-1-' || v_proy1, 'PROYECTO', 'Proyecto 016-1',
            'NUEVO_SERVICIO', 'REGISTRADO', v_unidad, v_usuario,
            0, 'N');
    INSERT INTO PROYECTO (ID_PROYECTO, CODIGO, TIPO_REGISTRO, NOMBRE,
                          TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
                          VERSION, SUBSANACION_ACTIVA)
    VALUES (v_proy2, 'PRY-016-2-' || v_proy2, 'PROYECTO', 'Proyecto 016-2',
            'NUEVO_SERVICIO', 'REGISTRADO', v_unidad, v_usuario,
            0, 'N');

    INSERT INTO INCORPORACION_REGISTRO
        (ID_INCORPORACION, FUENTE, FECHA_FUENTE, ID_RESPONSABLE,
         ID_DOCUMENTO_FUENTE, HASH_ORIGINAL, DATOS_ORIGINALES, CREADO_POR)
    VALUES (v_incorp, 'FUENTE_016', TRUNC(SYSDATE), v_usuario,
            v_doc, RPAD('A', 64, 'A'), '{"k":"v"}', 'TEST_016');

    INSERT INTO INCORPORACION_CAMBIO
        (ID_CAMBIO, ID_INCORPORACION, DATOS_ANTES, DATOS_DESPUES,
         MOTIVO, ID_ACTOR)
    VALUES (v_cambio, v_incorp, '{"k":"v"}', '{"k":"v2"}',
            'Correccion menor', v_usuario);

    INSERT INTO INCORPORACION_CONFLICTO
        (ID_CONFLICTO, ID_INCORPORACION, TIPO_CONFLICTO,
         ID_REGISTRO_CONFLICTIVO, DESCRIPCION)
    VALUES (v_conflicto, v_incorp, 'CODIGO', v_proy1,
            'Codigo duplicado');

    BEGIN
        INSERT INTO INCORPORACION_REGISTRO
            (ID_INCORPORACION, FUENTE, FECHA_FUENTE, ID_RESPONSABLE,
             ID_DOCUMENTO_FUENTE, HASH_ORIGINAL, CREADO_POR)
        VALUES (SEQ_INCORPORACION_REGISTRO.NEXTVAL, 'FUENTE_016',
                TRUNC(SYSDATE), v_usuario, v_doc,
                RPAD('A', 64, 'A'), 'TEST_016');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_inc := TRUE;
    END;

    BEGIN
        INSERT INTO INCORPORACION_REGISTRO
            (ID_INCORPORACION, FUENTE, FECHA_FUENTE, ID_RESPONSABLE,
             ID_DOCUMENTO_FUENTE, HASH_ORIGINAL, CREADO_POR)
        VALUES (SEQ_INCORPORACION_REGISTRO.NEXTVAL, 'FUENTE_016',
                TRUNC(SYSDATE), v_usuario, v_doc,
                'HASH_INVALIDO', 'TEST_016');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_hash := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        INSERT INTO INCORPORACION_REGISTRO
            (ID_INCORPORACION, FUENTE, FECHA_FUENTE, ID_RESPONSABLE,
             ID_DOCUMENTO_FUENTE, HASH_ORIGINAL, ESTADO, CREADO_POR)
        VALUES (SEQ_INCORPORACION_REGISTRO.NEXTVAL, 'OTRA_FUENTE_016',
                TRUNC(SYSDATE), v_usuario, v_doc,
                RPAD('B', 64, 'B'), 'OTRO_ESTADO', 'TEST_016');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_estado := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        UPDATE INCORPORACION_CONFLICTO
           SET RESUELTO = 'S', FECHA_RESOLUCION = NULL, ID_RESOLUTOR = NULL
         WHERE ID_CONFLICTO = v_conflicto;
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_resuelto := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        INSERT INTO INCORPORACION_CONFLICTO
            (ID_CONFLICTO, ID_INCORPORACION, TIPO_CONFLICTO,
             ID_REGISTRO_CONFLICTIVO)
        VALUES (SEQ_INCORPORACION_CONFLICTO.NEXTVAL, v_incorp,
                'CODIGO', v_proy2);
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_conf := TRUE;
    END;

    IF NOT v_uk_inc OR NOT v_hash OR NOT v_estado
       OR NOT v_resuelto OR NOT v_uk_conf THEN
        RAISE_APPLICATION_ERROR(-20A10,
            'T016 fallo: UK idempotente, SHA-256, estado, resolucion o UK conflicto');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T016 OK: incorporacion idempotente, conflictos y trazabilidad validados.');
END;
/

ROLLBACK TO T016_INCORPORACION;
PROMPT T016 finalizada; filas de prueba revertidas.
