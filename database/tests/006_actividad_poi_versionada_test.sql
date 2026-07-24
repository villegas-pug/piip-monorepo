-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 006: catalogos versionados de Actividad POI
-- Dependencia: 001, 002, 003, 003.1, 003.2 y 006 VIGENTES.
-- Proposito : verifica version POI, UK por CODIGO_VERSION, UK funcional y
--             vigencia coherente; el ciclo POI es independiente del PEI.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T006_ACTIVIDAD_POI;

DECLARE
    v_responsable NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_exp         NUMBER(12) := SEQ_EXPEDIENTE_INSTITUCIONAL.NEXTVAL;
    v_serie       NUMBER(12) := SEQ_DOCUMENTO_SERIE.NEXTVAL;
    v_doc         NUMBER(12) := SEQ_DOCUMENTO.NEXTVAL;
    v_version     NUMBER(10) := SEQ_ACTIVIDAD_POI_VERSION.NEXTVAL;
    v_actividad   NUMBER(10) := SEQ_ACTIVIDAD_POI.NEXTVAL;
    v_uk_codigo   BOOLEAN := FALSE;
    v_uk_version  BOOLEAN := FALSE;
    v_vigencia    BOOLEAN := FALSE;
BEGIN
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_responsable, '00000000-0000-0000-0000-000000000061',
            'test006_' || v_responsable, 'Planeamiento prueba 006',
            'test006_' || v_responsable || '@example.test', 'TEST_006');
    INSERT INTO EXPEDIENTE_INSTITUCIONAL
        (ID_EXPEDIENTE, CODIGO, ASUNTO, MODULO_ORIGEN, REFERENCIA_CASO_USO,
         CLASIFICACION, CREADO_POR)
    VALUES (v_exp, 'TEST-006-' || v_exp, 'Aprobacion POI', 'ORGANIZACION', 'TEST_006',
            'INTERNO', 'TEST_006');
    INSERT INTO DOCUMENTO_SERIE
        (ID_SERIE, ID_TIPO_DOC, ID_EXPEDIENTE, TITULO,
         CLASIFICACION_PROPUESTA, CREADO_POR)
    VALUES (v_serie, 1, v_exp, 'Aprobacion POI ' || v_serie, 'INTERNO', 'TEST_006');
    INSERT INTO DOCUMENTO
        (ID_DOCUMENTO, ID_TIPO_DOC, NOMBRE_ORIGINAL, MIME_TYPE, TAMANO_BYTES,
         HASH_SHA256, ID_USUARIO_CARGA, ID_DOCUMENTO_SERIE, NUMERO_VERSION)
    VALUES (v_doc, 1, 'poi.pdf', 'application/pdf', 1, RPAD('6',64,'6'),
            v_responsable, v_serie, 1);
    INSERT INTO CAT_ACTIVIDAD_POI_VERSION
        (ID_VERSION, CODIGO_VERSION, ID_DOCUMENTO_APROBACION,
         OFICINA_APROBADORA, VIGENTE_DESDE, CREADO_POR)
    VALUES (v_version, 'TEST-006-' || v_version, v_doc,
            'OFICINA PLANEAMIENTO PRUEBA', TRUNC(SYSDATE), 'TEST_006');
    INSERT INTO CAT_ACTIVIDAD_POI
        (ID_ACTIVIDAD, ID_VERSION, CODIGO, DESCRIPCION, VIGENTE_DESDE)
    VALUES (v_actividad, v_version, 'ACT_TEST_' || v_actividad,
            'Actividad de prueba', TRUNC(SYSDATE));

    BEGIN
        INSERT INTO CAT_ACTIVIDAD_POI_VERSION
            (ID_VERSION, CODIGO_VERSION, ID_DOCUMENTO_APROBACION,
             OFICINA_APROBADORA, VIGENTE_DESDE, CREADO_POR)
        VALUES (SEQ_ACTIVIDAD_POI_VERSION.NEXTVAL, 'TEST-006-' || v_version,
                v_doc, 'OFICINA PLANEAMIENTO PRUEBA', TRUNC(SYSDATE), 'TEST_006');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_codigo := TRUE;
    END;

    BEGIN
        INSERT INTO CAT_ACTIVIDAD_POI
            (ID_ACTIVIDAD, ID_VERSION, CODIGO, DESCRIPCION, VIGENTE_DESDE)
        VALUES (SEQ_ACTIVIDAD_POI.NEXTVAL, v_version,
                'ACT_TEST_' || v_actividad, 'Duplicado', TRUNC(SYSDATE));
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_version := TRUE;
    END;

    BEGIN
        INSERT INTO CAT_ACTIVIDAD_POI_VERSION
            (ID_VERSION, CODIGO_VERSION, ID_DOCUMENTO_APROBACION,
             OFICINA_APROBADORA, VIGENTE_DESDE, VIGENTE_HASTA, CREADO_POR)
        VALUES (SEQ_ACTIVIDAD_POI_VERSION.NEXTVAL, 'TEST-006-VIG-' || v_version,
                v_doc, 'OFICINA PRUEBA',
                TRUNC(SYSDATE), TRUNC(SYSDATE) - 1, 'TEST_006');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_vigencia := TRUE; ELSE RAISE; END IF;
    END;

    IF NOT v_uk_codigo OR NOT v_uk_version OR NOT v_vigencia THEN
        RAISE_APPLICATION_ERROR(-20960,
            'T006 fallo: UK CODIGO_VERSION, UK (VERSION,CODIGO) o vigencia no rechazo');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T006 OK: version POI, UKs y vigencia validadas.');
END;
/

ROLLBACK TO T006_ACTIVIDAD_POI;
PROMPT T006 finalizada; filas de prueba revertidas.
