-- ============================================================================
-- PIIP MIDAGRI - Prueba manual 026: OBSERVACION y VERSION de incorporacion
-- Dependencias: 016, 025 y 026 VIGENTES.
-- Proposito   : verifica que OBSERVACION recibe texto de hasta 2000 caracteres
--               y que VERSION se materializa con default 0 al omitirla en un
--               INSERT, base requerida por la concurrencia optimista JPA.
-- Transaccion : solo DML bajo SAVEPOINT; ROLLBACK TO revierte la fila de
--               prueba. Las secuencias conservan sus saltos. No ejecuta DDL.
-- Ejecucion   : manual por DBA en ambiente autorizado; no usar en compartidos
--               sin autorizacion humana.
-- Compensacion: si falla, ejecutar ROLLBACK TO T026_INCORPORACION y no alterar
--               la estructura; escalar la huella al DBA para correccion
--               forward-only.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T026_INCORPORACION;

DECLARE
    v_responsable NUMBER(10);
    v_documento   NUMBER(12);
    v_incorporacion NUMBER(12) := SEQ_INCORPORACION_REGISTRO.NEXTVAL;
    v_version     NUMBER(10);
    v_observacion VARCHAR2(2000 CHAR);
BEGIN
    SELECT MIN(ID_USUARIO) INTO v_responsable FROM USUARIO;
    SELECT MIN(ID_DOCUMENTO) INTO v_documento FROM DOCUMENTO;

    IF v_responsable IS NULL OR v_documento IS NULL THEN
        RAISE_APPLICATION_ERROR(-20084,
            'Prueba 026: se requiere al menos un USUARIO y un DOCUMENTO vigentes');
    END IF;

    INSERT INTO INCORPORACION_REGISTRO
        (ID_INCORPORACION, FUENTE, FECHA_FUENTE, ID_RESPONSABLE,
         ID_DOCUMENTO_FUENTE, HASH_ORIGINAL, OBSERVACION, CREADO_POR)
    VALUES
        (v_incorporacion, 'TEST_026_' || TO_CHAR(v_incorporacion), TRUNC(SYSDATE),
         v_responsable, v_documento, RPAD('2', 64, '2'),
         RPAD('O', 2000, 'O'), 'TEST_026');

    SELECT VERSION, OBSERVACION
      INTO v_version, v_observacion
      FROM INCORPORACION_REGISTRO
     WHERE ID_INCORPORACION = v_incorporacion;

    IF v_version <> 0 OR LENGTH(v_observacion) <> 2000 THEN
        RAISE_APPLICATION_ERROR(-20085,
            'Prueba 026: default VERSION u OBSERVACION no coincide con la huella esperada');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'T026 OK: OBSERVACION y VERSION=0 verificadas bajo SAVEPOINT.');
END;
/

ROLLBACK TO T026_INCORPORACION;
PROMPT T026 finalizada; fila de prueba revertida.