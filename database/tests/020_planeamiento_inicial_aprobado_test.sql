-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 020: semilla de planeamiento inicial
-- aprobado (PEI y POI)
-- Dependencia: 001, 002, 003, 003.1, 003.2, 004, 005, 005.1, 006, 007,
--              008+008.1, 009, 010, 011, 013, 014+014.1, 015, 016, 017
--              VIGENTES. La semilla 020 ya debe estar aplicada con
--              placeholders o con valores sustituidos.
-- Proposito : (a) verificar la presencia de la version inicial PEI y
--             POI; (b) verificar la presencia de los 3 items de ejemplo
--             por catalogo; (c) verificar idempotencia ante una segunda
--             ejecucion; (d) recordar que los placeholders deben ser
--             sustituidos antes de promover el planeamiento.
-- Limpieza  : ROLLBACK TO revierte DML; los MERGE ya confirmados
--             permanecen.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra
--             un ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T020_PLANEAMIENTO;

DECLARE
    v_versiones_pei_antes   PLS_INTEGER;
    v_versiones_poi_antes   PLS_INTEGER;
    v_items_pei_antes       PLS_INTEGER;
    v_items_poi_antes       PLS_INTEGER;
    v_versiones_pei_despues PLS_INTEGER;
    v_versiones_poi_despues PLS_INTEGER;
    v_items_pei_despues     PLS_INTEGER;
    v_items_poi_despues     PLS_INTEGER;
    v_placeholders_pendientes PLS_INTEGER;
BEGIN
    -- Conteos previos a la segunda ejecucion (idempotencia)
    SELECT COUNT(*) INTO v_versiones_pei_antes
      FROM CAT_OBJETIVO_PEI_VERSION
     WHERE CODIGO_VERSION = '<<PEI_VERSION_CODIGO>>';
    SELECT COUNT(*) INTO v_versiones_poi_antes
      FROM CAT_ACTIVIDAD_POI_VERSION
     WHERE CODIGO_VERSION = '<<POI_VERSION_CODIGO>>';
    SELECT COUNT(*) INTO v_items_pei_antes
      FROM CAT_OBJETIVO_PEI
     WHERE CODIGO LIKE 'OBJ_PEI_EJEMPLO_%';
    SELECT COUNT(*) INTO v_items_poi_antes
      FROM CAT_ACTIVIDAD_POI
     WHERE CODIGO LIKE 'ACT_POI_EJEMPLO_%';

    -- Segunda ejecucion: debe ser idempotente
    @@database/seeds/020_planeamiento_inicial_aprobado.sql

    SELECT COUNT(*) INTO v_versiones_pei_despues
      FROM CAT_OBJETIVO_PEI_VERSION
     WHERE CODIGO_VERSION = '<<PEI_VERSION_CODIGO>>';
    SELECT COUNT(*) INTO v_versiones_poi_despues
      FROM CAT_ACTIVIDAD_POI_VERSION
     WHERE CODIGO_VERSION = '<<POI_VERSION_CODIGO>>';
    SELECT COUNT(*) INTO v_items_pei_despues
      FROM CAT_OBJETIVO_PEI
     WHERE CODIGO LIKE 'OBJ_PEI_EJEMPLO_%';
    SELECT COUNT(*) INTO v_items_poi_despues
      FROM CAT_ACTIVIDAD_POI
     WHERE CODIGO LIKE 'ACT_POI_EJEMPLO_%';

    IF v_versiones_pei_despues <> v_versiones_pei_antes
       OR v_versiones_poi_despues <> v_versiones_poi_antes
       OR v_items_pei_despues     <> v_items_pei_antes
       OR v_items_poi_despues     <> v_items_poi_antes THEN
        RAISE_APPLICATION_ERROR(-20920,
            'T020 fallo: la segunda ejecucion no fue idempotente (pei/poi)');
    END IF;

    IF v_items_pei_antes < 3 OR v_items_poi_antes < 3 THEN
        RAISE_APPLICATION_ERROR(-20920,
            'T020 fallo: los items de ejemplo no estan presentes (3 por catalogo)');
    END IF;

    -- Detectar placeholders sin sustituir
    SELECT COUNT(*) INTO v_placeholders_pendientes
      FROM (
            SELECT 1 FROM CAT_OBJETIVO_PEI_VERSION
             WHERE CODIGO_VERSION LIKE '%<<%>>%'
            UNION ALL
            SELECT 1 FROM CAT_ACTIVIDAD_POI_VERSION
             WHERE CODIGO_VERSION LIKE '%<<%>>%'
            UNION ALL
            SELECT 1 FROM CAT_OBJETIVO_PEI
             WHERE CODIGO LIKE '%<<%>>%'
            UNION ALL
            SELECT 1 FROM CAT_ACTIVIDAD_POI
             WHERE CODIGO LIKE '%<<%>>%'
           );
    IF v_placeholders_pendientes > 0 THEN
        DBMS_OUTPUT.PUT_LINE(
            'T020 AVISO: quedan ' || TO_CHAR(v_placeholders_pendientes) ||
            ' placeholders por sustituir antes de promover el planeamiento.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('T020 OK: los placeholders ya fueron sustituidos.');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'T020 OK: versiones e items de ejemplo PEI/POI validados; semilla idempotente.');
END;
/

ROLLBACK TO T020_PLANEAMIENTO;
PROMPT T020 finalizada; filas de prueba revertidas.
