-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 004: clasificacion validada, historial de
-- reclasificacion y publicacion documental
-- Dependencia: 001, 002, 003, 003.1, 003.2, 004, 008 parcial y 008.1 VIGENTES.
-- Proposito : verifica UK unica por documento, CHECK restrictivo del
--             historial, formato de titulo publico y validacion de SHA-256.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T004_DOCUMENTO_PUBLICACION;

DECLARE
    v_usuario        NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_serie          NUMBER(12) := SEQ_DOCUMENTO_SERIE.NEXTVAL;
    v_doc            NUMBER(12) := SEQ_DOCUMENTO.NEXTVAL;
    v_hist           NUMBER(12) := SEQ_DOCUMENTO_CLASIF_HIST.NEXTVAL;
    v_pub            NUMBER(12) := SEQ_DOCUMENTO_PUBLICACION.NEXTVAL;
    v_xor             BOOLEAN := FALSE;
    v_restrictiva     BOOLEAN := FALSE;
    v_titulo_correo   BOOLEAN := FALSE;
    v_hash_invalido   BOOLEAN := FALSE;
BEGIN
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_usuario, '00000000-0000-0000-0000-000000000041',
            'test004_' || v_usuario, 'Evaluador prueba 004',
            'test004_' || v_usuario || '@example.test', 'TEST_004');

    INSERT INTO EXPEDIENTE_INSTITUCIONAL
        (ID_EXPEDIENTE, CODIGO, ASUNTO, MODULO_ORIGEN, REFERENCIA_CASO_USO,
         CLASIFICACION, CREADO_POR)
    VALUES (SEQ_EXPEDIENTE_INSTITUCIONAL.NEXTVAL,
            'TEST-004-' || TO_CHAR(SYSDATE, 'HH24MISS'),
            'Publicacion prueba 004', 'DOCUMENTOS', 'TEST_004', 'INTERNO', 'TEST_004');

    INSERT INTO DOCUMENTO_SERIE
        (ID_SERIE, ID_TIPO_DOC, ID_EXPEDIENTE, TITULO,
         CLASIFICACION_PROPUESTA, CREADO_POR)
    VALUES (v_serie, 1, SEQ_EXPEDIENTE_INSTITUCIONAL.CURRVAL,
            'Serie publicacion 004 ' || v_serie, 'INTERNO', 'TEST_004');

    INSERT INTO DOCUMENTO
        (ID_DOCUMENTO, ID_TIPO_DOC, NOMBRE_ORIGINAL, MIME_TYPE, TAMANO_BYTES,
         HASH_SHA256, ID_USUARIO_CARGA, ID_DOCUMENTO_SERIE, NUMERO_VERSION,
         CLASIFICACION_VALIDADA, CLASIFICACION_FECHA, ID_USUARIO_VALIDA)
    VALUES
        (v_doc, 1, 'publicacion.pdf', 'application/pdf', 4,
         RPAD('4', 64, '4'), v_usuario, v_serie, 1,
         'PUBLICO', SYSTIMESTAMP, v_usuario);

    INSERT INTO DOCUMENTO_CLASIFICACION_HIST
        (ID_HISTORIAL, ID_DOCUMENTO, CLASIFICACION_ANTERIOR, CLASIFICACION_NUEVA,
         ID_AUTORIDAD_DECISORA, ID_EVALUADOR_REGISTRADOR, RESULTADO)
    VALUES
        (v_hist, v_doc, 'INTERNO', 'PUBLICO', v_usuario, v_usuario, 'APLICADA');

    INSERT INTO DOCUMENTO_PUBLICACION
        (ID_PUBLICACION, ID_DOCUMENTO, TITULO_PUBLICO,
         ID_EVALUADOR_CONFIRMADOR, ID_ASIGNACION_EFECTIVA)
    VALUES
        (v_pub, v_doc, 'Documento institucional de prueba 004',
         v_usuario, 1);

    BEGIN
        INSERT INTO DOCUMENTO_PUBLICACION
            (ID_PUBLICACION, ID_DOCUMENTO, TITULO_PUBLICO,
             ID_EVALUADOR_CONFIRMADOR, ID_ASIGNACION_EFECTIVA)
        VALUES
            (SEQ_DOCUMENTO_PUBLICACION.NEXTVAL, v_doc,
             'Segunda publicacion del mismo documento',
             v_usuario, 1);
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_xor := TRUE;
    END;

    BEGIN
        INSERT INTO DOCUMENTO_CLASIFICACION_HIST
            (ID_HISTORIAL, ID_DOCUMENTO, CLASIFICACION_ANTERIOR,
             CLASIFICACION_NUEVA, ID_AUTORIDAD_DECISORA,
             ID_EVALUADOR_REGISTRADOR, RESULTADO)
        VALUES
            (SEQ_DOCUMENTO_CLASIF_HIST.NEXTVAL, v_doc,
             'PUBLICO', 'INTERNO', v_usuario, v_usuario, 'APLICADA');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_restrictiva := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        INSERT INTO DOCUMENTO_PUBLICACION
            (ID_PUBLICACION, ID_DOCUMENTO, TITULO_PUBLICO,
             ID_EVALUADOR_CONFIRMADOR, ID_ASIGNACION_EFECTIVA)
        VALUES
            (SEQ_DOCUMENTO_PUBLICACION.NEXTVAL, SEQ_DOCUMENTO.NEXTVAL,
             'publicar@midagri', v_usuario, 1);
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_titulo_correo := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        INSERT INTO DOCUMENTO
            (ID_DOCUMENTO, ID_TIPO_DOC, NOMBRE_ORIGINAL, MIME_TYPE, TAMANO_BYTES,
             HASH_SHA256, ID_USUARIO_CARGA, ID_DOCUMENTO_SERIE, NUMERO_VERSION,
             CLASIFICACION_VALIDADA)
        VALUES
            (SEQ_DOCUMENTO.NEXTVAL, 1, 'invalido.pdf', 'application/pdf', 1,
             'HASH_INVALIDO', v_usuario, v_serie, 2, 'PUBLICO');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_hash_invalido := TRUE; ELSE RAISE; END IF;
    END;

    IF NOT v_xor OR NOT v_restrictiva OR NOT v_titulo_correo
       OR NOT v_hash_invalido THEN
        RAISE_APPLICATION_ERROR(-20940,
            'T004 fallo: UK publicacion, restrictiva, formato titulo o SHA-256');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T004 OK: reclasificacion restrictiva, UK, formato titulo y SHA-256 validados.');
END;
/

ROLLBACK TO T004_DOCUMENTO_PUBLICACION;
PROMPT T004 finalizada; filas de prueba revertidas.
