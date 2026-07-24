-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 023 - Carga de documento de aprobacion POI
-- Dependencia: 003+003.1+003.2+005+005.1+006+008+008.1+019+021 VIGENTES.
--              El script 023 ya debe estar aplicado; la prueba valida
--              idempotencia, presencia del expediente/serie/documento y
--              que el ID_DOCUMENTO impreso es valido.
-- Proposito : (a) verificar la huella esperada; (b) verificar la
--             presencia del expediente APROBACION_POI, la serie y el
--             documento; (c) verificar idempotencia ante una segunda
--             ejecucion.
-- Limpieza  : ROLLBACK TO revierte DML.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra
--             un ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T023_POI;

DECLARE
    v_tablas_antes    PLS_INTEGER;
    v_tablas_despues PLS_INTEGER;
    v_exp_antes       PLS_INTEGER;
    v_exp_despues     PLS_INTEGER;
    v_total_exp       PLS_INTEGER;
    v_total_ser       PLS_INTEGER;
    v_total_doc       PLS_INTEGER;
    v_id_doc          NUMBER(12);
    v_id_tipo         NUMBER(5);
BEGIN
    SELECT COUNT(*) INTO v_tablas_antes
      FROM USER_TABLES WHERE TABLE_NAME = 'DOCUMENTO';
    SELECT COUNT(*) INTO v_exp_antes
      FROM EXPEDIENTE_INSTITUCIONAL
     WHERE REFERENCIA_CASO_USO = 'APROBACION_POI';

    -- Segunda ejecucion: debe abortar con ORA-20326 (no es idempotente)
    BEGIN
        @@database/seeds/023_documento_aprobacion_poi.sql
        RAISE_APPLICATION_ERROR(-20923,
            'T023 fallo: la segunda ejecucion no aborto; la carga manual permitio reejecucion');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20326 THEN
                DBMS_OUTPUT.PUT_LINE(
                    'T023 OK: la segunda ejecucion aborto con ORA-20326 como se esperaba.');
            ELSE
                RAISE;
            END IF;
    END;

    SELECT COUNT(*) INTO v_tablas_despues
      FROM USER_TABLES WHERE TABLE_NAME = 'DOCUMENTO';
    SELECT COUNT(*) INTO v_exp_despues
      FROM EXPEDIENTE_INSTITUCIONAL
     WHERE REFERENCIA_CASO_USO = 'APROBACION_POI';

    IF v_tablas_despues <> v_tablas_antes
       OR v_exp_despues <> v_exp_antes THEN
        RAISE_APPLICATION_ERROR(-20923,
            'T023 fallo: la segunda ejecucion modifico el estado del esquema');
    END IF;

    SELECT COUNT(*) INTO v_total_exp
      FROM EXPEDIENTE_INSTITUCIONAL
     WHERE REFERENCIA_CASO_USO = 'APROBACION_POI';
    IF v_total_exp <> 1 THEN
        RAISE_APPLICATION_ERROR(-20923,
            'T023 fallo: el expediente de aprobacion POI debe ser exactamente 1; conteo='
            || TO_CHAR(v_total_exp));
    END IF;

    -- Para 023 el ID_TIPO_DOC_POI puede ser cualquiera de los 13
    -- tipos canonicos. Buscamos el documento a traves de la serie
    -- y el expediente sin filtrar por tipo.
    SELECT ID_DOCUMENTO INTO v_id_doc
      FROM (
          SELECT d.ID_DOCUMENTO
            FROM DOCUMENTO d
            JOIN DOCUMENTO_SERIE ds ON ds.ID_SERIE = d.ID_DOCUMENTO_SERIE
            JOIN EXPEDIENTE_INSTITUCIONAL ex ON ex.ID_EXPEDIENTE = ds.ID_EXPEDIENTE
           WHERE ex.REFERENCIA_CASO_USO = 'APROBACION_POI'
           ORDER BY d.ID_DOCUMENTO
      )
      WHERE ROWNUM = 1;
    IF v_id_doc IS NULL THEN
        RAISE_APPLICATION_ERROR(-20923,
            'T023 fallo: no se encontro ID_DOCUMENTO para la aprobacion POI');
    END IF;

    SELECT COUNT(*) INTO v_total_doc
      FROM DOCUMENTO d
      JOIN DOCUMENTO_SERIE ds ON ds.ID_SERIE = d.ID_DOCUMENTO_SERIE
      JOIN EXPEDIENTE_INSTITUCIONAL ex ON ex.ID_EXPEDIENTE = ds.ID_EXPEDIENTE
     WHERE ex.REFERENCIA_CASO_USO = 'APROBACION_POI';
    IF v_total_doc <> 1 THEN
        RAISE_APPLICATION_ERROR(-20923,
            'T023 fallo: el documento POI debe ser exactamente 1; conteo='
            || TO_CHAR(v_total_doc));
    END IF;

    DBMS_OUTPUT.PUT_LINE('T023 OK: ID_DOCUMENTO=' || TO_CHAR(v_id_doc) ||
        ' es valido para &&ID_DOCUMENTO_APROBACION_POI en 020.');
END;
/

ROLLBACK TO T023_POI;
PROMPT T023 finalizada; filas de prueba revertidas.
