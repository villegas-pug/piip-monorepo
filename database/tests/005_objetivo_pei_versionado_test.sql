-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 005: catalogos versionados de Objetivo PEI
-- Dependencia: 001, 002, 003, 003.1, 003.2, 004, 007, 008+008.1 y 005.1
--              VIGENTES; 005 conserva la huella parcial confirmada por
--              ORA-01408 y no debe reejecutarse.
-- Proposito : verifica la version de Objetivo PEI, su UK por CODIGO_VERSION,
--             la UK funcional (ID_VERSION, CODIGO) y la vigencia coherente.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T005_OBJETIVO_PEI;

DECLARE
    v_responsable NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_exp         NUMBER(12) := SEQ_EXPEDIENTE_INSTITUCIONAL.NEXTVAL;
    v_serie       NUMBER(12) := SEQ_DOCUMENTO_SERIE.NEXTVAL;
    v_doc         NUMBER(12) := SEQ_DOCUMENTO.NEXTVAL;
    v_version     NUMBER(10) := SEQ_OBJETIVO_PEI_VERSION.NEXTVAL;
    v_objetivo    NUMBER(10) := SEQ_OBJETIVO_PEI.NEXTVAL;
    v_uk_codigo   BOOLEAN := FALSE;
    v_uk_version  BOOLEAN := FALSE;
    v_vigencia    BOOLEAN := FALSE;
BEGIN
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_responsable, '00000000-0000-0000-0000-000000000051',
            'test005_' || v_responsable, 'Planeamiento prueba 005',
            'test005_' || v_responsable || '@example.test', 'TEST_005');
    INSERT INTO EXPEDIENTE_INSTITUCIONAL
        (ID_EXPEDIENTE, CODIGO, ASUNTO, MODULO_ORIGEN, REFERENCIA_CASO_USO,
         CLASIFICACION, CREADO_POR)
    VALUES (v_exp, 'TEST-005-' || v_exp, 'Aprobacion PEI', 'ORGANIZACION', 'TEST_005',
            'INTERNO', 'TEST_005');
    INSERT INTO DOCUMENTO_SERIE
        (ID_SERIE, ID_TIPO_DOC, ID_EXPEDIENTE, TITULO,
         CLASIFICACION_PROPUESTA, CREADO_POR)
    VALUES (v_serie, 1, v_exp, 'Aprobacion PEI ' || v_serie, 'INTERNO', 'TEST_005');
    INSERT INTO DOCUMENTO
        (ID_DOCUMENTO, ID_TIPO_DOC, NOMBRE_ORIGINAL, MIME_TYPE, TAMANO_BYTES,
         HASH_SHA256, ID_USUARIO_CARGA, ID_DOCUMENTO_SERIE, NUMERO_VERSION)
    VALUES (v_doc, 1, 'pei.pdf', 'application/pdf', 1, RPAD('5',64,'5'),
            v_responsable, v_serie, 1);
    INSERT INTO CAT_OBJETIVO_PEI_VERSION
        (ID_VERSION, CODIGO_VERSION, ID_DOCUMENTO_APROBACION,
         OFICINA_APROBADORA, VIGENTE_DESDE, CREADO_POR)
    VALUES (v_version, 'TEST-005-' || v_version, v_doc,
            'OFICINA PLANEAMIENTO PRUEBA', TRUNC(SYSDATE), 'TEST_005');
    INSERT INTO CAT_OBJETIVO_PEI
        (ID_OBJETIVO, ID_VERSION, CODIGO, DESCRIPCION, VIGENTE_DESDE)
    VALUES (v_objetivo, v_version, 'OBJ_TEST_' || v_objetivo,
            'Objetivo de prueba', TRUNC(SYSDATE));

    BEGIN
        INSERT INTO CAT_OBJETIVO_PEI_VERSION
            (ID_VERSION, CODIGO_VERSION, ID_DOCUMENTO_APROBACION,
             OFICINA_APROBADORA, VIGENTE_DESDE, CREADO_POR)
        VALUES (SEQ_OBJETIVO_PEI_VERSION.NEXTVAL, 'TEST-005-' || v_version,
                v_doc, 'OFICINA PLANEAMIENTO PRUEBA', TRUNC(SYSDATE), 'TEST_005');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_codigo := TRUE;
    END;

    BEGIN
        INSERT INTO CAT_OBJETIVO_PEI
            (ID_OBJETIVO, ID_VERSION, CODIGO, DESCRIPCION, VIGENTE_DESDE)
        VALUES (SEQ_OBJETIVO_PEI.NEXTVAL, v_version,
                'OBJ_TEST_' || v_objetivo, 'Duplicado', TRUNC(SYSDATE));
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_version := TRUE;
    END;

    BEGIN
        INSERT INTO CAT_OBJETIVO_PEI_VERSION
            (ID_VERSION, CODIGO_VERSION, ID_DOCUMENTO_APROBACION,
             OFICINA_APROBADORA, VIGENTE_DESDE, VIGENTE_HASTA, CREADO_POR)
        VALUES (SEQ_OBJETIVO_PEI_VERSION.NEXTVAL, 'TEST-005-VIG-' || v_version,
                v_doc, 'OFICINA PRUEBA',
                TRUNC(SYSDATE), TRUNC(SYSDATE) - 1, 'TEST_005');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_vigencia := TRUE; ELSE RAISE; END IF;
    END;

    IF NOT v_uk_codigo OR NOT v_uk_version OR NOT v_vigencia THEN
        RAISE_APPLICATION_ERROR(-20950,
            'T005 fallo: UK CODIGO_VERSION, UK (VERSION,CODIGO) o vigencia no rechazo');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T005 OK: version PEI, UKs y vigencia validadas.');
END;
/

ROLLBACK TO T005_OBJETIVO_PEI;
PROMPT T005 finalizada; filas de prueba revertidas.
