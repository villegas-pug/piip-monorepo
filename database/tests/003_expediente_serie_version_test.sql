-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 003/003.1/003.2: expediente y versiones
-- Dependencia: 001, 002, 003, 003.1 y 003.2 VIGENTES.
-- Proposito : verifica expediente, BLOB, SHA-256, propietario XOR, limite
--             100 MB y cadena de versionado. No modela ni prueba antimalware.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T003_EXPEDIENTE_SERIE;

DECLARE
    v_usuario NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_exp     NUMBER(12) := SEQ_EXPEDIENTE_INSTITUCIONAL.NEXTVAL;
    v_serie   NUMBER(12) := SEQ_DOCUMENTO_SERIE.NEXTVAL;
    v_doc_1   NUMBER(12) := SEQ_DOCUMENTO.NEXTVAL;
    v_doc_2   NUMBER(12) := SEQ_DOCUMENTO.NEXTVAL;
    v_xor     BOOLEAN := FALSE;
    v_hash    BOOLEAN := FALSE;
    v_tamano  BOOLEAN := FALSE;
    v_blob_len PLS_INTEGER;
BEGIN
    INSERT INTO USUARIO (
        ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR
    ) VALUES (
        v_usuario, '00000000-0000-0000-0000-000000000003',
        'test003_' || TO_CHAR(v_usuario), 'Usuario prueba 003',
        'test003_' || TO_CHAR(v_usuario) || '@example.test', 'TEST_003'
    );

    INSERT INTO EXPEDIENTE_INSTITUCIONAL (
        ID_EXPEDIENTE, CODIGO, ASUNTO, MODULO_ORIGEN, REFERENCIA_CASO_USO,
        CLASIFICACION, CREADO_POR
    ) VALUES (
        v_exp, 'TEST-003-' || TO_CHAR(v_exp), 'Expediente de prueba 003',
        'DOCUMENTOS', 'TEST_003', 'INTERNO', 'TEST_003'
    );

    INSERT INTO DOCUMENTO_SERIE (
        ID_SERIE, ID_TIPO_DOC, ID_EXPEDIENTE, TITULO,
        CLASIFICACION_PROPUESTA, CREADO_POR
    ) VALUES (
        v_serie, 1, v_exp, 'Serie institucional de prueba ' || TO_CHAR(v_serie),
        'INTERNO', 'TEST_003'
    );

    INSERT INTO DOCUMENTO (
        ID_DOCUMENTO, ID_TIPO_DOC, NOMBRE_ORIGINAL, MIME_TYPE, TAMANO_BYTES,
        HASH_SHA256, ID_USUARIO_CARGA, ID_DOCUMENTO_SERIE, CONTENIDO, FORMATO,
        NUMERO_VERSION
    ) VALUES (
        v_doc_1, 1, 'prueba-003-v1.pdf', 'application/pdf', 4,
        RPAD('B', 64, 'B'), v_usuario, v_serie, TO_BLOB(HEXTORAW('50494950')), 'PDF', 1
    );

    INSERT INTO DOCUMENTO (
        ID_DOCUMENTO, ID_TIPO_DOC, NOMBRE_ORIGINAL, MIME_TYPE, TAMANO_BYTES,
        HASH_SHA256, ID_USUARIO_CARGA, ID_DOCUMENTO_SERIE, CONTENIDO, FORMATO,
        NUMERO_VERSION, ID_DOCUMENTO_ANTERIOR
    ) VALUES (
        v_doc_2, 1, 'prueba-003-v2.pdf', 'application/pdf', 4,
        RPAD('C', 64, 'C'), v_usuario, v_serie, TO_BLOB(HEXTORAW('50494950')), 'PDF', 2, v_doc_1
    );

    SELECT DBMS_LOB.GETLENGTH(CONTENIDO) INTO v_blob_len
      FROM DOCUMENTO WHERE ID_DOCUMENTO = v_doc_1;
    IF v_blob_len <> 4 THEN
        RAISE_APPLICATION_ERROR(-20910, 'T003 fallo: BLOB no conserva cuatro bytes');
    END IF;

    BEGIN
        INSERT INTO DOCUMENTO_SERIE (
            ID_SERIE, ID_TIPO_DOC, ID_REGISTRO, ID_EXPEDIENTE, TITULO,
            CLASIFICACION_PROPUESTA, CREADO_POR
        ) VALUES (
            SEQ_DOCUMENTO_SERIE.NEXTVAL, 1, 1, v_exp, 'XOR invalido ' || TO_CHAR(v_exp),
            'INTERNO', 'TEST_003'
        );
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_xor := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        INSERT INTO DOCUMENTO (
            ID_DOCUMENTO, ID_TIPO_DOC, NOMBRE_ORIGINAL, MIME_TYPE, TAMANO_BYTES,
            HASH_SHA256, ID_USUARIO_CARGA, ID_DOCUMENTO_SERIE, NUMERO_VERSION
        ) VALUES (
            SEQ_DOCUMENTO.NEXTVAL, 1, 'hash-invalido.pdf', 'application/pdf', 1,
            'HASH_INVALIDO', v_usuario, v_serie, 3
        );
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_hash := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        INSERT INTO DOCUMENTO (
            ID_DOCUMENTO, ID_TIPO_DOC, NOMBRE_ORIGINAL, MIME_TYPE, TAMANO_BYTES,
            HASH_SHA256, ID_USUARIO_CARGA, ID_DOCUMENTO_SERIE, NUMERO_VERSION
        ) VALUES (
            SEQ_DOCUMENTO.NEXTVAL, 1, 'tamano-invalido.pdf', 'application/pdf', 104857601,
            RPAD('D', 64, 'D'), v_usuario, v_serie, 3
        );
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_tamano := TRUE; ELSE RAISE; END IF;
    END;

    IF NOT v_xor OR NOT v_hash OR NOT v_tamano THEN
        RAISE_APPLICATION_ERROR(-20911, 'T003 fallo: XOR, SHA-256 o limite 100 MB no rechazo');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T003 OK: expediente, BLOB, XOR, SHA-256, tamano y versionado validados.');
END;
/

ROLLBACK TO T003_EXPEDIENTE_SERIE;
PROMPT T003 finalizada; filas de prueba revertidas y sin pruebas antimalware.
