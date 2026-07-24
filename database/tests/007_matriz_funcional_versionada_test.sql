-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 007: matriz funcional versionada
-- Dependencia: 001, 002, 003, 003.1, 003.2 y 007 VIGENTES.
-- Proposito : verifica version, funcion, combinacion concreta, vigencia y
--             segregacion aprobador/registrador.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias no se revierten.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T007_MATRIZ;

DECLARE
    v_aprobador NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_registrador NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_exp NUMBER(12) := SEQ_EXPEDIENTE_INSTITUCIONAL.NEXTVAL;
    v_serie NUMBER(12) := SEQ_DOCUMENTO_SERIE.NEXTVAL;
    v_doc NUMBER(12) := SEQ_DOCUMENTO.NEXTVAL;
    v_version NUMBER(10) := SEQ_MATRIZ_VERSION.NEXTVAL;
    v_funcion NUMBER(10) := SEQ_MATRIZ_FUNCION.NEXTVAL;
    v_combo NUMBER(12) := SEQ_MATRIZ_COMBINACION.NEXTVAL;
    v_segregacion BOOLEAN := FALSE;
    v_vigencia BOOLEAN := FALSE;
BEGIN
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_aprobador, '00000000-0000-0000-0000-000000000071',
            'test007a_' || v_aprobador, 'Aprobador prueba 007',
            'test007a_' || v_aprobador || '@example.test', 'TEST_007');
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_registrador, '00000000-0000-0000-0000-000000000072',
            'test007r_' || v_registrador, 'Registrador prueba 007',
            'test007r_' || v_registrador || '@example.test', 'TEST_007');
    INSERT INTO EXPEDIENTE_INSTITUCIONAL
        (ID_EXPEDIENTE, CODIGO, ASUNTO, MODULO_ORIGEN, REFERENCIA_CASO_USO, CLASIFICACION, CREADO_POR)
    VALUES (v_exp, 'TEST-007-' || v_exp, 'Aprobacion matriz', 'SEGURIDAD', 'TEST_007', 'INTERNO', 'TEST_007');
    INSERT INTO DOCUMENTO_SERIE
        (ID_SERIE, ID_TIPO_DOC, ID_EXPEDIENTE, TITULO, CLASIFICACION_PROPUESTA, CREADO_POR)
    VALUES (v_serie, 1, v_exp, 'Aprobacion matriz ' || v_serie, 'INTERNO', 'TEST_007');
    INSERT INTO DOCUMENTO
        (ID_DOCUMENTO, ID_TIPO_DOC, NOMBRE_ORIGINAL, MIME_TYPE, TAMANO_BYTES, HASH_SHA256,
         ID_USUARIO_CARGA, ID_DOCUMENTO_SERIE, NUMERO_VERSION)
    VALUES (v_doc, 1, 'matriz.pdf', 'application/pdf', 1, RPAD('7',64,'7'),
            v_registrador, v_serie, 1);
    INSERT INTO MATRIZ_FUNCIONAL_VERSION
        (ID_VERSION, CODIGO_VERSION, ID_DOCUMENTO_APROBACION, VIGENTE_DESDE, CREADO_POR)
    VALUES (v_version, 'TEST-007-' || v_version, v_doc, TRUNC(SYSDATE), 'TEST_007');
    INSERT INTO MATRIZ_FUNCION (ID_FUNCION, ID_VERSION, CODIGO, DESCRIPCION)
    VALUES (v_funcion, v_version, 'FUNC_TEST_' || v_funcion, 'Funcion de prueba');
    INSERT INTO MATRIZ_FUNCION_PERFIL_UNIDAD
        (ID_COMBINACION, ID_VERSION, ID_FUNCION, ID_ROL, ID_UNIDAD, ID_APROBADOR,
         ID_REGISTRADOR, ID_DOCUMENTO_APROBACION, VIGENTE_DESDE, CREADO_POR)
    VALUES (v_combo, v_version, v_funcion, 1, 1, v_aprobador,
            v_registrador, v_doc, TRUNC(SYSDATE), 'TEST_007');

    BEGIN
        INSERT INTO MATRIZ_FUNCION_PERFIL_UNIDAD
            (ID_COMBINACION, ID_VERSION, ID_FUNCION, ID_ROL, ID_UNIDAD, ID_APROBADOR,
             ID_REGISTRADOR, ID_DOCUMENTO_APROBACION, VIGENTE_DESDE, CREADO_POR)
        VALUES (SEQ_MATRIZ_COMBINACION.NEXTVAL, v_version, v_funcion, 2, 1,
                v_aprobador, v_aprobador, v_doc, TRUNC(SYSDATE), 'TEST_007');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_segregacion := TRUE; ELSE RAISE; END IF;
    END;
    BEGIN
        INSERT INTO MATRIZ_FUNCIONAL_VERSION
            (ID_VERSION, CODIGO_VERSION, ID_DOCUMENTO_APROBACION, VIGENTE_DESDE, VIGENTE_HASTA, CREADO_POR)
        VALUES (SEQ_MATRIZ_VERSION.NEXTVAL, 'TEST-007-VIG-' || v_version, v_doc,
                TRUNC(SYSDATE), TRUNC(SYSDATE) - 1, 'TEST_007');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_vigencia := TRUE; ELSE RAISE; END IF;
    END;
    IF NOT v_segregacion OR NOT v_vigencia THEN
        RAISE_APPLICATION_ERROR(-20920, 'T007 fallo: segregacion o vigencia no fue rechazada');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T007 OK: matriz versionada, combinacion y controles validados.');
END;
/

ROLLBACK TO T007_MATRIZ;
PROMPT T007 finalizada; filas de prueba revertidas.
