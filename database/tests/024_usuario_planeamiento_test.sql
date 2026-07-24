-- ============================================================================
-- PIIP MIDAGRI - Pruebas manuales 024 - Carga de usuario de planeamiento
-- Dependencia: 002+007+008+008.1+021 VIGENTES. El script 024 ya debe
--              estar aplicado; la prueba valida idempotencia, presencia
--              del usuario y, si aplica, de la primera asignacion.
-- Proposito : (a) verificar la presencia del usuario por sub de
--             Keycloak; (b) verificar formato UUID del sub; (c)
--             verificar la asignacion USUARIO_ROL_UNIDAD si se
--             proporciono una combinacion matriz; (d) verificar
--             idempotencia ante una segunda ejecucion.
-- Limpieza  : ROLLBACK TO revierte DML.
-- Ejecucion : manual por DBA en ambiente autorizado. No ejecutar contra
--             un ambiente compartido sin autorizacion humana.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

SAVEPOINT T024_USR;

DECLARE
    v_total_usuarios  PLS_INTEGER;
    v_total_asign     PLS_INTEGER;
    v_id_usuario      NUMBER(10);
    v_uuid_valido     PLS_INTEGER;
BEGIN
    -- 1) Verificar formato UUID del sub de OGTI
    SELECT COUNT(*) INTO v_uuid_valido
      FROM DUAL
     WHERE REGEXP_LIKE('&&KEYCLOAK_ID',
            '^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$');
    IF v_uuid_valido <> 1 THEN
        RAISE_APPLICATION_ERROR(-20924,
            'T024 fallo: el sub no tiene formato UUID valido');
    END IF;

    -- 2) Verificar que existe el usuario
    SELECT COUNT(*) INTO v_total_usuarios
      FROM USUARIO
     WHERE KEYCLOAK_ID = '&&KEYCLOAK_ID';
    IF v_total_usuarios <> 1 THEN
        RAISE_APPLICATION_ERROR(-20924,
            'T024 fallo: el usuario por sub no existe; conteo='
            || TO_CHAR(v_total_usuarios));
    END IF;

    SELECT ID_USUARIO INTO v_id_usuario
      FROM USUARIO
     WHERE KEYCLOAK_ID = '&&KEYCLOAK_ID';

    -- 3) Verificar asignacion si la combinacion fue proporcionada
    IF '&&ID_COMBINACION_PLANEAMIENTO' IS NOT NULL
       AND TO_NUMBER('&&ID_COMBINACION_PLANEAMIENTO') > 0 THEN
        SELECT COUNT(*) INTO v_total_asign
          FROM USUARIO_ROL_UNIDAD
         WHERE ID_USUARIO = v_id_usuario
           AND ID_COMBINACION_MATRIZ = TO_NUMBER('&&ID_COMBINACION_PLANEAMIENTO')
           AND REVOCADA_EN IS NULL
           AND FECHA_FIN IS NULL;
        IF v_total_asign <> 1 THEN
            RAISE_APPLICATION_ERROR(-20924,
                'T024 fallo: la asignacion con ID_COMBINACION debe ser exactamente 1; conteo='
                || TO_CHAR(v_total_asign));
        END IF;
    END IF;

    -- 4) Verificar idempotencia negativa
    BEGIN
        -- @@database/seeds/024_usuario_planeamiento.sql
        RAISE_APPLICATION_ERROR(-20924,
            'T024 fallo: la segunda ejecucion no aborto; la carga manual permitio reejecucion');
    EXCEPTION
        WHEN OTHERS THEN
            IF SQLCODE = -20424 THEN
                DBMS_OUTPUT.PUT_LINE(
                    'T024 OK: la segunda ejecucion aborto con ORA-20424 como se esperaba.');
            ELSE
                RAISE;
            END IF;
    END;

    DBMS_OUTPUT.PUT_LINE('T024 OK: ID_USUARIO=' || TO_CHAR(v_id_usuario) ||
        ' es valido para &&ID_ACTOR_PLANEAMIENTO en 020.');
END;
/

ROLLBACK TO T024_USR;
PROMPT T024 finalizada; filas de prueba revertidas.
