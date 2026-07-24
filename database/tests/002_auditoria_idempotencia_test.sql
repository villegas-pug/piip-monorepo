-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 002: auditoria e idempotencia
-- Dependencia: 001 y 002 VIGENTES.
-- Proposito : verifica auditoria efectiva, unicidad idempotente y CHECKs.
-- Limpieza  : todas las filas de prueba se revierten con ROLLBACK TO; los
--             valores consumidos de secuencia no se revierten en Oracle.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra un
--             ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T002_AUDITORIA_IDEMPOTENCIA;

DECLARE
    v_id_solicitud NUMBER(15);
    v_id_audit     NUMBER(15);
    v_duplicado    BOOLEAN := FALSE;
    v_hash_invalido BOOLEAN := FALSE;
    v_metodo_invalido BOOLEAN := FALSE;
BEGIN
    v_id_solicitud := SEQ_SOLICITUD_IDEMPOTENTE.NEXTVAL;
    INSERT INTO SOLICITUD_IDEMPOTENTE (
        ID_SOLICITUD, CONSUMIDOR, OPERACION, CLAVE, HASH_PAYLOAD,
        ESTADO_TECNICO, FECHA_EXPIRACION, CREADO_POR
    ) VALUES (
        v_id_solicitud, 'TEST_002', 'CREAR_RECURSO', 'TEST-002-CLAVE',
        RPAD('A', 64, 'A'), 'INICIADA', SYSTIMESTAMP + INTERVAL '7' DAY,
        'TEST_002'
    );

    BEGIN
        INSERT INTO SOLICITUD_IDEMPOTENTE (
            ID_SOLICITUD, CONSUMIDOR, OPERACION, CLAVE, HASH_PAYLOAD,
            ESTADO_TECNICO, FECHA_EXPIRACION, CREADO_POR
        ) VALUES (
            SEQ_SOLICITUD_IDEMPOTENTE.NEXTVAL, 'TEST_002', 'CREAR_RECURSO',
            'TEST-002-CLAVE', RPAD('A', 64, 'A'), 'INICIADA',
            SYSTIMESTAMP + INTERVAL '7' DAY, 'TEST_002'
        );
    EXCEPTION WHEN DUP_VAL_ON_INDEX THEN v_duplicado := TRUE;
    END;

    BEGIN
        INSERT INTO SOLICITUD_IDEMPOTENTE (
            ID_SOLICITUD, CONSUMIDOR, OPERACION, CLAVE, HASH_PAYLOAD,
            ESTADO_TECNICO, FECHA_EXPIRACION, CREADO_POR
        ) VALUES (
            SEQ_SOLICITUD_IDEMPOTENTE.NEXTVAL, 'TEST_002', 'HASH_INVALIDO',
            'TEST-002-HASH', 'NO_HEX', 'INICIADA',
            SYSTIMESTAMP + INTERVAL '7' DAY, 'TEST_002'
        );
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_hash_invalido := TRUE; ELSE RAISE; END IF;
    END;

    v_id_audit := SEQ_AUDITORIA_ACCESO.NEXTVAL;
    INSERT INTO AUDITORIA_ACCESO (
        ID_AUDIT, ENDPOINT, METODO_HTTP, CODIGO_RESPUESTA, IP_CLIENTE,
        ID_ROL_EFECTIVO, ID_UNIDAD_EFECTIVA
    ) VALUES (
        v_id_audit, '/test/002', 'POST', 201, '127.0.0.1', 1, 1
    );

    BEGIN
        INSERT INTO AUDITORIA_ACCESO (
            ID_AUDIT, ENDPOINT, METODO_HTTP, CODIGO_RESPUESTA, IP_CLIENTE
        ) VALUES (
            SEQ_AUDITORIA_ACCESO.NEXTVAL, '/test/002', 'TRACE', 200, '127.0.0.1'
        );
    EXCEPTION WHEN OTHERS THEN
        IF SQLCODE = -2290 THEN v_metodo_invalido := TRUE; ELSE RAISE; END IF;
    END;

    IF NOT v_duplicado OR NOT v_hash_invalido OR NOT v_metodo_invalido THEN
        RAISE_APPLICATION_ERROR(-20900,
            'T002 fallo: no se activaron todas las protecciones esperadas');
    END IF;
    DBMS_OUTPUT.PUT_LINE('T002 OK: idempotencia, SHA-256 y auditoria efectiva validadas.');
END;
/

ROLLBACK TO T002_AUDITORIA_IDEMPOTENCIA;
PROMPT T002 finalizada; filas de prueba revertidas.
