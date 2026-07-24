-- ============================================================================
-- PIIP MIDAGRI - Prueba manual 027: unidad objetivo de aprovisionamiento
-- Dependencias: 008.1, 026 y 027 VIGENTES.
-- Proposito   : verifica que una operacion de prueba conserva una unidad
--               ejecutora existente y que puede localizarse por su indice.
-- Transaccion : solo DML bajo SAVEPOINT; ROLLBACK TO revierte la fila. La
--               secuencia conserva su salto. No ejecuta DDL ni modifica historia.
-- Ejecucion   : manual por DBA en ambiente autorizado; no usar en compartidos
--               sin autorizacion humana.
-- Compensacion: ante fallo, ejecutar ROLLBACK TO T027_OA_UNIDAD y escalar la
--               huella al DBA para correccion exclusivamente forward-only.
-- NEEDS CLARIFICATION: la obligatoriedad para operaciones ordinarias nuevas se
--               valida en backend; la columna permanece nullable para preservar
--               filas historicas sin unidad inventada.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T027_OA_UNIDAD;

DECLARE
    v_unidad     NUMBER(10);
    v_operacion  NUMBER(12) := SEQ_OPERACION_APROVISIONAMIENTO.NEXTVAL;
    v_encontrada NUMBER(10);
BEGIN
    SELECT MIN(ID_UNIDAD) INTO v_unidad FROM UNIDAD_EJECUTORA;
    IF v_unidad IS NULL THEN RAISE_APPLICATION_ERROR(-20094, 'Prueba 027: se requiere al menos una UNIDAD_EJECUTORA'); END IF;
    INSERT INTO OPERACION_APROVISIONAMIENTO
        (ID_OPERACION, CLAVE_IDEMPOTENTE, HASH_PAYLOAD, ID_UNIDAD_OBJETIVO, ESTADO_TECNICO, CREADO_POR)
    VALUES
        (v_operacion, 'TEST_027_' || TO_CHAR(v_operacion), RPAD('2', 64, '2'), v_unidad, 'INICIADA', 'TEST_027');
    SELECT ID_UNIDAD_OBJETIVO INTO v_encontrada FROM OPERACION_APROVISIONAMIENTO
     WHERE ID_UNIDAD_OBJETIVO = v_unidad AND ID_OPERACION = v_operacion;
    IF v_encontrada <> v_unidad THEN RAISE_APPLICATION_ERROR(-20095, 'Prueba 027: la unidad objetivo no se conserva o no se puede localizar'); END IF;
    DBMS_OUTPUT.PUT_LINE('T027 OK: ID_UNIDAD_OBJETIVO conservada bajo SAVEPOINT.');
END;
/

ROLLBACK TO T027_OA_UNIDAD;
PROMPT T027 finalizada; fila de prueba revertida.
