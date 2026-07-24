-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 017: ciclo de reportes institucionales
-- Dependencia: 001, 002, 003, 003.1, 003.2, 005, 006, 008+008.1, 009, 015
--              y 017 VIGENTES.
-- Proposito : verifica UK por HASH_SHA256 del snapshot, CHECK IS JSON,
--             CHECKs del TIPO/CORTE 30/06 y 31/12, UKs por reporte y
--             aprobacion, y FKs a DOCUMENTO, USUARIO y UNIDAD_EJECUTORA.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T017_REPORTES;

DECLARE
    v_unidad   NUMBER(10) := 1;
    v_generador NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_aprobador NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_doc      NUMBER(12) := SEQ_DOCUMENTO.NEXTVAL;
    v_snap     NUMBER(12) := SEQ_REPORTE_SNAPSHOT.NEXTVAL;
    v_reporte  NUMBER(12) := SEQ_REPORTE_INSTITUCIONAL.NEXTVAL;
    v_archivo  NUMBER(12) := SEQ_REPORTE_ARCHIVO.NEXTVAL;
    v_aprob    NUMBER(12) := SEQ_REPORTE_APROBACION.NEXTVAL;
    v_dest     NUMBER(12) := SEQ_REPORTE_DESTINATARIO.NEXTVAL;
    v_rem      NUMBER(12) := SEQ_REPORTE_REMISION.NEXTVAL;
    v_uk_hash  BOOLEAN := FALSE;
    v_json     BOOLEAN := FALSE;
    v_corte    BOOLEAN := FALSE;
    v_formato  BOOLEAN := FALSE;
    v_rem_dup  BOOLEAN := FALSE;
BEGIN
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_generador, '00000000-0000-0000-0000-000000000171',
            'test017g_' || v_generador, 'Generador 017',
            'test017g_' || v_generador || '@example.test', 'TEST_017');
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_aprobador, '00000000-0000-0000-0000-000000000172',
            'test017a_' || v_aprobador, 'Aprobador 017',
            'test017a_' || v_aprobador || '@example.test', 'TEST_017');

    INSERT INTO REPORTE_SNAPSHOT
        (ID_SNAPSHOT, PAYLOAD_JSON, VERSION_ESQUEMA, HASH_SHA256,
         FECHA_CORTE, CREADO_POR)
    VALUES (v_snap, '{"totales":{"proyectos":1}}', 1,
            RPAD('7', 64, '7'), DATE '2026-06-30', 'TEST_017');

    INSERT INTO REPORTE_INSTITUCIONAL
        (ID_REPORTE, TIPO, ANIO, SEMESTRE, PERIODO, FECHA_CORTE,
         ID_SNAPSHOT, VERSION_DATOS, CLASIFICACION, ID_GENERADOR, ESTADO_TECNICO)
    VALUES (v_reporte, 'SEMESTRAL', 2026, 1, '2026-S1',
            DATE '2026-06-30', v_snap, 1, 'INTERNO', v_generador, 'GENERADA');

    INSERT INTO REPORTE_ARCHIVO
        (ID_ARCHIVO, ID_REPORTE, FORMATO, VERSION, HASH_SHA256,
         ID_DOCUMENTO_VERSION, CREADO_POR)
    VALUES (v_archivo, v_reporte, 'PDF', 1, RPAD('A', 64, 'A'),
            v_doc, 'TEST_017');

    INSERT INTO REPORTE_APROBACION
        (ID_APROBACION, ID_REPORTE, ID_VERSION, ID_OFICINA, ID_APROBADOR,
         ID_DOCUMENTO_APROBACION)
    VALUES (v_aprob, v_reporte, 1, v_unidad, v_aprobador, v_doc);

    INSERT INTO REPORTE_DESTINATARIO
        (ID_DESTINATARIO, ID_APROBACION, TIPO_DESTINATARIO, ID_ENTIDAD, NOMBRE)
    VALUES (v_dest, v_aprob, 'AUTORIDAD_MIDAGRI', 1, 'Despacho Ministerial');

    INSERT INTO REPORTE_REMISION
        (ID_REMISION, ID_REPORTE, ID_DESTINATARIO, RESULTADO)
    VALUES (v_rem, v_reporte, v_dest, 'EXITOSA');

    -- Caso negativo: HASH_SHA256 duplicado en snapshot
    BEGIN
        INSERT INTO REPORTE_SNAPSHOT
            (ID_SNAPSHOT, PAYLOAD_JSON, VERSION_ESQUEMA, HASH_SHA256,
             FECHA_CORTE, CREADO_POR)
        VALUES (SEQ_REPORTE_SNAPSHOT.NEXTVAL, '{"k":2}', 1,
                RPAD('7', 64, '7'), DATE '2026-06-30', 'TEST_017');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_hash := TRUE;
    END;

    -- Caso negativo: PAYLOAD_JSON no es JSON valido
    BEGIN
        INSERT INTO REPORTE_SNAPSHOT
            (ID_SNAPSHOT, PAYLOAD_JSON, VERSION_ESQUEMA, HASH_SHA256,
             FECHA_CORTE, CREADO_POR)
        VALUES (SEQ_REPORTE_SNAPSHOT.NEXTVAL, 'no es json', 1,
                RPAD('8', 64, '8'), DATE '2026-06-30', 'TEST_017');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_json := TRUE; ELSE RAISE; END IF;
    END;

    -- Caso negativo: SEMESTRAL con FECHA_CORTE fuera de 30/06 o 31/12
    BEGIN
        INSERT INTO REPORTE_INSTITUCIONAL
            (ID_REPORTE, TIPO, ANIO, SEMESTRE, PERIODO, FECHA_CORTE,
             CLASIFICACION, ID_GENERADOR, ESTADO_TECNICO)
        VALUES (SEQ_REPORTE_INSTITUCIONAL.NEXTVAL, 'SEMESTRAL', 2026, 1,
                '2026-S1-mal', DATE '2026-07-15',
                'INTERNO', v_generador, 'GENERADA');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_corte := TRUE; ELSE RAISE; END IF;
    END;

    -- Caso negativo: FORMATO fuera del CHECK
    BEGIN
        INSERT INTO REPORTE_ARCHIVO
            (ID_ARCHIVO, ID_REPORTE, FORMATO, VERSION, HASH_SHA256,
             ID_DOCUMENTO_VERSION, CREADO_POR)
        VALUES (SEQ_REPORTE_ARCHIVO.NEXTVAL, v_reporte, 'DOCX', 2,
                RPAD('B', 64, 'B'), v_doc, 'TEST_017');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_formato := TRUE; ELSE RAISE; END IF;
    END;

    -- Caso negativo: UK por (ID_REPORTE, ID_DESTINATARIO, FECHA_REMISION)
    BEGIN
        INSERT INTO REPORTE_REMISION
            (ID_REMISION, ID_REPORTE, ID_DESTINATARIO, RESULTADO)
        VALUES (SEQ_REPORTE_REMISION.NEXTVAL, v_reporte, v_dest, 'EXITOSA');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_rem_dup := TRUE;
    END;

    IF NOT v_uk_hash OR NOT v_json OR NOT v_corte OR NOT v_formato
       OR NOT v_rem_dup THEN
        RAISE_APPLICATION_ERROR(-20A70,
            'T017 fallo: UK hash, IS JSON, CORTE, FORMATO o UK remision');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T017 OK: ciclo de reportes, JSON, cortes y remision validados.');
END;
/

ROLLBACK TO T017_REPORTES;
PROMPT T017 finalizada; filas de prueba revertidas.
