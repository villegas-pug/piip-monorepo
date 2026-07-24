-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 025 - gate T072/T073
-- Dependencias: 003, 015 y 025 VIGENTES.
-- Proposito   : verifica PK/FK/UK, cadena de versiones, append-only y multiples
--               evidencias para una presentacion de producto final.
-- Transaccion : solo DML bajo SAVEPOINT; ROLLBACK revierte las filas de prueba.
--               Las secuencias conservan saltos. No ejecuta DDL.
-- Ejecucion   : manual por DBA en ambiente autorizado; no usar en compartidos
--               sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T025_CICLO_PRESENTACION;

DECLARE
    v_unidad             NUMBER(10) := 1;
    v_usuario            NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_proyecto           NUMBER(12) := SEQ_PROYECTO.NEXTVAL;
    v_ciclo              NUMBER(12) := SEQ_CICLO_PROYECTO.NEXTVAL;
    v_presentacion       NUMBER(12) := SEQ_PRESENTACION_PRODUCTO_FINAL.NEXTVAL;
    v_documento_1        NUMBER(12) := SEQ_DOCUMENTO.NEXTVAL;
    v_documento_2        NUMBER(12) := SEQ_DOCUMENTO.NEXTVAL;
    v_tipo_documento     NUMBER(5);
    v_version_1          NUMBER(12) := SEQ_CICLO_PROYECTO_VERSION.NEXTVAL;
    v_version_2          NUMBER(12) := SEQ_CICLO_PROYECTO_VERSION.NEXTVAL;
    v_evidencia_1        NUMBER(12) := SEQ_PPF_EVIDENCIA.NEXTVAL;
    v_evidencia_2        NUMBER(12) := SEQ_PPF_EVIDENCIA.NEXTVAL;
    v_pk_cpv             BOOLEAN := FALSE;
    v_pk_ppfe            BOOLEAN := FALSE;
    v_fk_ciclo           BOOLEAN := FALSE;
    v_fk_cadena          BOOLEAN := FALSE;
    v_fk_presentacion    BOOLEAN := FALSE;
    v_fk_documento       BOOLEAN := FALSE;
    v_uk_cpv             BOOLEAN := FALSE;
    v_uk_ppfe            BOOLEAN := FALSE;
    v_append_version     BOOLEAN := FALSE;
    v_append_evidencia   BOOLEAN := FALSE;
    v_cadena_valida      BOOLEAN := FALSE;
    v_evidencias_multiples BOOLEAN := FALSE;
    v_total_dummy        PLS_INTEGER;
BEGIN
    SELECT MIN(ID_TIPO_DOC) INTO v_tipo_documento FROM TIPO_DOCUMENTO;
    IF v_tipo_documento IS NULL THEN
        RAISE_APPLICATION_ERROR(-20100,
            'Prueba 025: no existe TIPO_DOCUMENTO para crear evidencias de prueba');
    END IF;

    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_usuario, '00000000-0000-0000-0000-' || LPAD(TO_CHAR(v_usuario), 12, '0'),
            'test025u_' || v_usuario, 'Usuario prueba 025',
            'test025u_' || v_usuario || '@example.test', 'TEST_025');

    INSERT INTO PROYECTO
        (ID_PROYECTO, CODIGO, TIPO_REGISTRO, NOMBRE, TIPO_SOLUCION, FUENTE_ORIGEN,
         DESCRIPCION, OBJETIVO_PEI, ACTIVIDAD_POI, ESTADO, ID_UNIDAD_EJECUTORA,
         ID_RESPONSABLE, CREADO_POR, VERSION, SUBSANACION_ACTIVA)
    VALUES
        (v_proyecto, '2026-TEST-' || LPAD(TO_CHAR(v_proyecto), 5, '0'), 'PROYECTO',
         'Proyecto prueba 025', 'POR_DEFINIR', 'OTROS', 'Prueba T072/T073',
         'Objetivo prueba', 'Actividad prueba', 'PROYECTO_EJECUCION', v_unidad,
         v_usuario, 'TEST_025', 0, 'N');

    INSERT INTO CICLO_PROYECTO
        (ID_CICLO, ID_PROYECTO, PERIODO, NUMERO_VERSION, OBJETIVOS, CREADO_POR)
    VALUES (v_ciclo, v_proyecto, '2026-Q1-S1', 1, 'Ciclo de prueba', 'TEST_025');

    INSERT INTO DOCUMENTO
        (ID_DOCUMENTO, ID_PROYECTO, ID_TIPO_DOC, ESTADO_AL_CARGAR, NOMBRE_ORIGINAL,
         NOMBRE_STORAGE, MIME_TYPE, TAMANO_BYTES, HASH_SHA256, ID_USUARIO_CARGA,
         NUMERO_VERSION)
    VALUES
        (v_documento_1, v_proyecto, v_tipo_documento, 'PROYECTO_EJECUCION',
         'evidencia-1.pdf', 'test025-' || v_documento_1 || '.pdf', 'application/pdf', 1,
         RPAD('A', 64, 'A'), v_usuario, 1);

    INSERT INTO DOCUMENTO
        (ID_DOCUMENTO, ID_PROYECTO, ID_TIPO_DOC, ESTADO_AL_CARGAR, NOMBRE_ORIGINAL,
         NOMBRE_STORAGE, MIME_TYPE, TAMANO_BYTES, HASH_SHA256, ID_USUARIO_CARGA,
         NUMERO_VERSION)
    VALUES
        (v_documento_2, v_proyecto, v_tipo_documento, 'PROYECTO_EJECUCION',
         'evidencia-2.pdf', 'test025-' || v_documento_2 || '.pdf', 'application/pdf', 1,
         RPAD('B', 64, 'B'), v_usuario, 2);

    INSERT INTO PRESENTACION_PRODUCTO_FINAL
        (ID_PRESENTACION, ID_PROYECTO, VERSION, DESCRIPCION, ID_RESPONSABLE,
         ID_DOCUMENTO_SUSTENTA)
    VALUES (v_presentacion, v_proyecto, 1, 'Presentacion prueba 025', v_usuario,
            v_documento_1);

    INSERT INTO CICLO_PROYECTO_VERSION
        (ID_CICLO_VERSION, ID_CICLO, NUMERO_VERSION, CREADO_POR)
    VALUES (v_version_1, v_ciclo, 1, 'TEST_025');

    INSERT INTO CICLO_PROYECTO_VERSION
        (ID_CICLO_VERSION, ID_CICLO, NUMERO_VERSION, ID_VERSION_ANTERIOR, CREADO_POR)
    VALUES (v_version_2, v_ciclo, 2, v_version_1, 'TEST_025');

    INSERT INTO PRESENTACION_PRODUCTO_FINAL_EVIDENCIA
        (ID_EVIDENCIA, ID_PRESENTACION, ID_DOCUMENTO, CREADO_POR)
    VALUES (v_evidencia_1, v_presentacion, v_documento_1, 'TEST_025');

    INSERT INTO PRESENTACION_PRODUCTO_FINAL_EVIDENCIA
        (ID_EVIDENCIA, ID_PRESENTACION, ID_DOCUMENTO, CREADO_POR)
    VALUES (v_evidencia_2, v_presentacion, v_documento_2, 'TEST_025');

    BEGIN
        INSERT INTO CICLO_PROYECTO_VERSION
            (ID_CICLO_VERSION, ID_CICLO, NUMERO_VERSION, CREADO_POR)
        VALUES (v_version_1, v_ciclo, 3, 'TEST_025');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_pk_cpv := TRUE;
    END;

    BEGIN
        INSERT INTO CICLO_PROYECTO_VERSION
            (ID_CICLO_VERSION, ID_CICLO, NUMERO_VERSION, CREADO_POR)
        VALUES (SEQ_CICLO_PROYECTO_VERSION.NEXTVAL, v_ciclo, 2, 'TEST_025');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_cpv := TRUE;
    END;

    BEGIN
        INSERT INTO PRESENTACION_PRODUCTO_FINAL_EVIDENCIA
            (ID_EVIDENCIA, ID_PRESENTACION, ID_DOCUMENTO, CREADO_POR)
        VALUES (SEQ_PPF_EVIDENCIA.NEXTVAL, v_presentacion, v_documento_1, 'TEST_025');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_uk_ppfe := TRUE;
    END;

    BEGIN
        INSERT INTO PRESENTACION_PRODUCTO_FINAL_EVIDENCIA
            (ID_EVIDENCIA, ID_PRESENTACION, ID_DOCUMENTO, CREADO_POR)
        VALUES (v_evidencia_1, v_presentacion, v_documento_2, 'TEST_025');
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_pk_ppfe := TRUE;
    END;

    BEGIN
        INSERT INTO CICLO_PROYECTO_VERSION
            (ID_CICLO_VERSION, ID_CICLO, NUMERO_VERSION, CREADO_POR)
        VALUES (SEQ_CICLO_PROYECTO_VERSION.NEXTVAL, -1, 3, 'TEST_025');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2291 THEN v_fk_ciclo := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        INSERT INTO CICLO_PROYECTO_VERSION
            (ID_CICLO_VERSION, ID_CICLO, NUMERO_VERSION, ID_VERSION_ANTERIOR, CREADO_POR)
        VALUES (SEQ_CICLO_PROYECTO_VERSION.NEXTVAL, v_ciclo, 3, -1, 'TEST_025');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2291 THEN v_fk_cadena := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        INSERT INTO PRESENTACION_PRODUCTO_FINAL_EVIDENCIA
            (ID_EVIDENCIA, ID_PRESENTACION, ID_DOCUMENTO, CREADO_POR)
        VALUES (SEQ_PPF_EVIDENCIA.NEXTVAL, -1, v_documento_1, 'TEST_025');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2291 THEN v_fk_presentacion := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        INSERT INTO PRESENTACION_PRODUCTO_FINAL_EVIDENCIA
            (ID_EVIDENCIA, ID_PRESENTACION, ID_DOCUMENTO, CREADO_POR)
        VALUES (SEQ_PPF_EVIDENCIA.NEXTVAL, v_presentacion, -1, 'TEST_025');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2291 THEN v_fk_documento := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        UPDATE CICLO_PROYECTO_VERSION SET CREADO_POR = 'NO_PERMITIDO'
         WHERE ID_CICLO_VERSION = v_version_1;
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -20076 THEN v_append_version := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        DELETE FROM PRESENTACION_PRODUCTO_FINAL_EVIDENCIA
         WHERE ID_EVIDENCIA = v_evidencia_1;
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -20077 THEN v_append_evidencia := TRUE; ELSE RAISE; END IF;
    END;

    SELECT CASE WHEN COUNT(*) = 2
                      AND MIN(NUMERO_VERSION) = 1 AND MAX(NUMERO_VERSION) = 2
                      AND MAX(ID_VERSION_ANTERIOR) = v_version_1
                THEN 1 ELSE 0 END
      INTO v_total_dummy
      FROM CICLO_PROYECTO_VERSION
     WHERE ID_CICLO IN (v_ciclo)
       AND ID_CICLO_VERSION IN (v_version_1, v_version_2);

    v_cadena_valida := (v_total_dummy = 1);

    SELECT CASE WHEN COUNT(*) = 2 THEN 1 ELSE 0 END INTO v_total_dummy
      FROM PRESENTACION_PRODUCTO_FINAL_EVIDENCIA
     WHERE ID_PRESENTACION = v_presentacion;
    v_evidencias_multiples := (v_total_dummy = 1);

    IF NOT v_pk_cpv OR NOT v_pk_ppfe OR NOT v_fk_ciclo OR NOT v_fk_cadena
       OR NOT v_fk_presentacion OR NOT v_fk_documento OR NOT v_uk_cpv OR NOT v_uk_ppfe
       OR NOT v_append_version
       OR NOT v_append_evidencia OR NOT v_cadena_valida OR NOT v_evidencias_multiples THEN
        RAISE_APPLICATION_ERROR(-20101,
            'Prueba 025 fallo: PK/FK/UK, append-only, cadena o evidencias multiples');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T025 OK: PK/FK/UK, append-only, cadena y multiples evidencias validadas.');
END;
/

ROLLBACK TO T025_CICLO_PRESENTACION;
PROMPT T025 finalizada; filas de prueba revertidas.
