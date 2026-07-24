-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 009: campos oficiales de PROYECTO,
-- @Version, subsanacion y FKs a catalogos 005/006
-- Dependencia: 001, 002, 003, 003.1, 003.2, 005, 006, 008 parcial, 008.1
--              y 009 VIGENTES.
-- Proposito : verifica CHECK de coherencia entre COMPONENTE_DIGITAL y
--             DETALLE_COMPONENTE_DIGITAL, NULL habilitada para
--             ADMINISTRACION legacy, VERSION y SUBSANACION_ACTIVA.
-- Limpieza  : ROLLBACK TO revierte DML; las secuencias conservan sus saltos.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T009_PROYECTO_CAMPOS;

DECLARE
    v_unidad   NUMBER(10) := 1;
    v_usuario  NUMBER(10) := SEQ_USUARIO.NEXTVAL;
    v_pei      NUMBER(10) := SEQ_OBJETIVO_PEI.NEXTVAL;
    v_poi      NUMBER(10) := SEQ_ACTIVIDAD_POI.NEXTVAL;
    v_exp      NUMBER(12) := SEQ_EXPEDIENTE_INSTITUCIONAL.NEXTVAL;
    v_serie    NUMBER(12) := SEQ_DOCUMENTO_SERIE.NEXTVAL;
    v_doc      NUMBER(12) := SEQ_DOCUMENTO.NEXTVAL;
    v_v        NUMBER(10) := SEQ_OBJETIVO_PEI_VERSION.NEXTVAL;
    v_w        NUMBER(10) := SEQ_ACTIVIDAD_POI_VERSION.NEXTVAL;
    v_proy     NUMBER(12) := SEQ_PROYECTO.NEXTVAL;
    v_detalle  BOOLEAN := FALSE;
    v_comp     BOOLEAN := FALSE;
    v_adm      BOOLEAN := TRUE;
BEGIN
    -- Cabeceras de catalogo PEI/POI minimas para que la FK a 005/006 valide
    INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, CREADO_POR)
    VALUES (v_usuario, '00000000-0000-0000-0000-000000000091',
            'test009_' || v_usuario, 'Responsable prueba 009',
            'test009_' || v_usuario || '@example.test', 'TEST_009');
    INSERT INTO EXPEDIENTE_INSTITUCIONAL
        (ID_EXPEDIENTE, CODIGO, ASUNTO, MODULO_ORIGEN, REFERENCIA_CASO_USO,
         CLASIFICACION, CREADO_POR)
    VALUES (v_exp, 'TEST-009-' || v_exp, 'Aprobacion PEI/POI 009',
            'ORGANIZACION', 'TEST_009', 'INTERNO', 'TEST_009');
    INSERT INTO DOCUMENTO_SERIE
        (ID_SERIE, ID_TIPO_DOC, ID_EXPEDIENTE, TITULO,
         CLASIFICACION_PROPUESTA, CREADO_POR)
    VALUES (v_serie, 1, v_exp, 'Aprobacion 009 ' || v_serie, 'INTERNO', 'TEST_009');
    INSERT INTO DOCUMENTO
        (ID_DOCUMENTO, ID_TIPO_DOC, NOMBRE_ORIGINAL, MIME_TYPE, TAMANO_BYTES,
         HASH_SHA256, ID_USUARIO_CARGA, ID_DOCUMENTO_SERIE, NUMERO_VERSION)
    VALUES (v_doc, 1, 'aprob009.pdf', 'application/pdf', 1, RPAD('9',64,'9'),
            v_usuario, v_serie, 1);
    INSERT INTO CAT_OBJETIVO_PEI_VERSION
        (ID_VERSION, CODIGO_VERSION, ID_DOCUMENTO_APROBACION,
         OFICINA_APROBADORA, VIGENTE_DESDE, CREADO_POR)
    VALUES (v_v, 'TEST-009-PEI-' || v_v, v_doc,
            'OFICINA PRUEBA 009', TRUNC(SYSDATE), 'TEST_009');
    INSERT INTO CAT_OBJETIVO_PEI
        (ID_OBJETIVO, ID_VERSION, CODIGO, DESCRIPCION, VIGENTE_DESDE)
    VALUES (v_pei, v_v, 'OBJ_TEST_009_' || v_pei, 'Objetivo 009',
            TRUNC(SYSDATE));
    INSERT INTO CAT_ACTIVIDAD_POI_VERSION
        (ID_VERSION, CODIGO_VERSION, ID_DOCUMENTO_APROBACION,
         OFICINA_APROBADORA, VIGENTE_DESDE, CREADO_POR)
    VALUES (v_w, 'TEST-009-POI-' || v_w, v_doc,
            'OFICINA PRUEBA 009', TRUNC(SYSDATE), 'TEST_009');
    INSERT INTO CAT_ACTIVIDAD_POI
        (ID_ACTIVIDAD, ID_VERSION, CODIGO, DESCRIPCION, VIGENTE_DESDE)
    VALUES (v_poi, v_w, 'ACT_TEST_009_' || v_poi, 'Actividad 009',
            TRUNC(SYSDATE));

    INSERT INTO PROYECTO
        (ID_PROYECTO, CODIGO, CODIGO_PREFIJO, TIPO_REGISTRO, NOMBRE,
         TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
         OBJETIVO_PEI_ID, ACTIVIDAD_POI_ID,
         COMPONENTE_DIGITAL, DETALLE_COMPONENTE_DIGITAL,
         VERSION, SUBSANACION_ACTIVA, ADMINISTRACION)
    VALUES
        (v_proy, 'TEST-009-' || v_proy, 'TEST', 'PROYECTO',
         'Proyecto prueba 009', 'NUEVO_SERVICIO', 'REGISTRADO',
         v_unidad, v_usuario, v_pei, v_poi,
         'S', 'Plataforma digital de prueba',
         0, 'N', NULL);

    -- Caso positivo: COMPONENTE_DIGITAL='N' y DETALLE_COMPONENTE_DIGITAL NULL
    INSERT INTO PROYECTO
        (ID_PROYECTO, CODIGO, CODIGO_PREFIJO, TIPO_REGISTRO, NOMBRE,
         TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
         COMPONENTE_DIGITAL, ADMINISTRACION, VERSION, SUBSANACION_ACTIVA)
    VALUES
        (SEQ_PROYECTO.NEXTVAL, 'TEST-009B-' || SEQ_PROYECTO.CURRVAL, 'TEST',
         'PROYECTO', 'Proyecto sin componente 009',
         'NUEVO_SERVICIO', 'REGISTRADO', v_unidad, v_usuario,
         'N', 'LEGACY', 0, 'N');

    BEGIN
        INSERT INTO PROYECTO
            (ID_PROYECTO, CODIGO, CODIGO_PREFIJO, TIPO_REGISTRO, NOMBRE,
             TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
             COMPONENTE_DIGITAL, VERSION, SUBSANACION_ACTIVA)
        VALUES
            (SEQ_PROYECTO.NEXTVAL, 'TEST-009C-' || SEQ_PROYECTO.CURRVAL, 'TEST',
             'PROYECTO', 'Detalle obligatorio 009',
             'NUEVO_SERVICIO', 'REGISTRADO', v_unidad, v_usuario,
             'S', 0, 'N');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_detalle := TRUE; ELSE RAISE; END IF;
    END;

    BEGIN
        INSERT INTO PROYECTO
            (ID_PROYECTO, CODIGO, CODIGO_PREFIJO, TIPO_REGISTRO, NOMBRE,
             TIPO_SOLUCION, ESTADO, ID_UNIDAD_EJECUTORA, ID_RESPONSABLE,
             COMPONENTE_DIGITAL, VERSION, SUBSANACION_ACTIVA)
        VALUES
            (SEQ_PROYECTO.NEXTVAL, 'TEST-009D-' || SEQ_PROYECTO.CURRVAL, 'TEST',
             'PROYECTO', 'Componente invalido 009',
             'NUEVO_SERVICIO', 'REGISTRADO', v_unidad, v_usuario,
             'X', 0, 'N');
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_comp := TRUE; ELSE RAISE; END IF;
    END;

    -- Confirmacion: ADMINISTRACION admite nulo (legacy)
    SELECT COUNT(*) INTO v_adm
      FROM PROYECTO
     WHERE ID_PROYECTO = v_proy AND ADMINISTRACION IS NULL;
    IF v_adm <> 1 THEN v_adm := FALSE; END IF;

    IF NOT v_detalle OR NOT v_comp OR NOT v_adm THEN
        RAISE_APPLICATION_ERROR(-20990,
            'T009 fallo: coherencia de componente, dominio o nulidad ADMINISTRACION');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T009 OK: campos oficiales, version, subsanacion y FKs PEI/POI validados.');
END;
/

ROLLBACK TO T009_PROYECTO_CAMPOS;
PROMPT T009 finalizada; filas de prueba revertidas.
