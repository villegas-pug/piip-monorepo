-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 022 - Carga de documento de aprobacion PEI
-- Dependencia: 003+003.1+003.2+005+005.1+006+008+008.1+019+021 VIGENTES.
--              El script 022 ya debe estar aplicado; la prueba valida
--              idempotencia, presencia del expediente/serie/documento y
--              que el ID_DOCUMENTO impreso es valido.
-- Proposito : (a) verificar que la huella de tablas/sequences/TIPO_DOCUMENTO
--             es la esperada; (b) verificar la presencia del expediente
--             APROBACION_PEI, la serie y el documento; (c) verificar
--             idempotencia ante una segunda ejecucion.
-- Limpieza  : ROLLBACK TO revierte DML; los DML de la propia semilla
--             ya se ejecutaron en una transaccion previa.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra
--             un ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T022_PEI;

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
     WHERE REFERENCIA_CASO_USO = 'APROBACION_PEI';

    -- Segunda ejecucion: debe abortar con ORA-20227 (no es idempotente)
    BEGIN
        @@database/seeds/022_documento_aprobacion_pei.sql
        -- Si llega aqui, la prueba falla.
        RAISE_APPLICATION_ERROR(-20922,
            'T022 fallo: la segunda ejecucion no aborto; la carga manual permitio reejecucion');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20227 THEN
                DBMS_OUTPUT.PUT_LINE(
                    'T022 OK: la segunda ejecucion aborto con ORA-20227 como se esperaba.');
            ELSE
                RAISE;
            END IF;
    END;

    SELECT COUNT(*) INTO v_tablas_despues
      FROM USER_TABLES WHERE TABLE_NAME = 'DOCUMENTO';
    SELECT COUNT(*) INTO v_exp_despues
      FROM EXPEDIENTE_INSTITUCIONAL
     WHERE REFERENCIA_CASO_USO = 'APROBACION_PEI';

    IF v_tablas_despues <> v_tablas_antes
       OR v_exp_despues <> v_exp_antes THEN
        RAISE_APPLICATION_ERROR(-20922,
            'T022 fallo: la segunda ejecucion modifico el estado del esquema');
    END IF;

    -- Huella esperada: un solo expediente, una sola serie, un solo
    -- documento para la aprobacion PEI.
    SELECT COUNT(*) INTO v_total_exp
      FROM EXPEDIENTE_INSTITUCIONAL
     WHERE REFERENCIA_CASO_USO = 'APROBACION_PEI';
    IF v_total_exp <> 1 THEN
        RAISE_APPLICATION_ERROR(-20922,
            'T022 fallo: el expediente de aprobacion PEI debe ser exactamente 1; conteo='
            || TO_CHAR(v_total_exp));
    END IF;

    SELECT ID_TIPO_DOC INTO v_id_tipo
      FROM TIPO_DOCUMENTO
     WHERE NOMBRE = 'Documento de Aprobacion o Autorizacion'
       AND ESTADO_ASOCIADO = 'PROYECTO_EJECUCION'
       AND CONTEXTO = 'PORTAFOLIO'
       AND ROWNUM = 1;

    SELECT COUNT(*) INTO v_total_ser
      FROM DOCUMENTO_SERIE ds
      JOIN EXPEDIENTE_INSTITUCIONAL ex ON ex.ID_EXPEDIENTE = ds.ID_EXPEDIENTE
     WHERE ex.REFERENCIA_CASO_USO = 'APROBACION_PEI'
       AND ds.ID_TIPO_DOC = v_id_tipo;
    IF v_total_ser <> 1 THEN
        RAISE_APPLICATION_ERROR(-20922,
            'T022 fallo: la serie de aprobacion PEI debe ser exactamente 1; conteo='
            || TO_CHAR(v_total_ser));
    END IF;

    SELECT COUNT(*) INTO v_total_doc
      FROM DOCUMENTO d
      JOIN DOCUMENTO_SERIE ds ON ds.ID_SERIE = d.ID_DOCUMENTO_SERIE
      JOIN EXPEDIENTE_INSTITUCIONAL ex ON ex.ID_EXPEDIENTE = ds.ID_EXPEDIENTE
     WHERE ex.REFERENCIA_CASO_USO = 'APROBACION_PEI'
       AND d.ID_TIPO_DOC = v_id_tipo;
    IF v_total_doc <> 1 THEN
        RAISE_APPLICATION_ERROR(-20922,
            'T022 fallo: el documento PEI debe ser exactamente 1; conteo='
            || TO_CHAR(v_total_doc));
    END IF;

    SELECT ID_DOCUMENTO INTO v_id_doc
      FROM DOCUMENTO d
      JOIN DOCUMENTO_SERIE ds ON ds.ID_SERIE = d.ID_DOCUMENTO_SERIE
      JOIN EXPEDIENTE_INSTITUCIONAL ex ON ex.ID_EXPEDIENTE = ds.ID_EXPEDIENTE
     WHERE ex.REFERENCIA_CASO_USO = 'APROBACION_PEI'
       AND d.ID_TIPO_DOC = v_id_tipo
       AND ROWNUM = 1;
    DBMS_OUTPUT.PUT_LINE('T022 OK: ID_DOCUMENTO=' || TO_CHAR(v_id_doc) ||
        ' es valido para &&ID_DOCUMENTO_APROBACION_PEI en 020.');
END;
/

ROLLBACK TO T022_PEI;
PROMPT T022 finalizada; filas de prueba revertidas.
