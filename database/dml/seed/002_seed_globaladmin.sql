-- ============================================================================
-- PIIP MIDAGRI - Actualizacion 002 - Vincular usuario GlobalAdmin a Keycloak
-- Archivo   : 002_seed_globaladmin.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : seguridad
-- Dependencias:
--   021.1 (database/seeds/021.1_bootstrap_matriz_fundacional.sql) - VIGENTE
-- Compensacion:
--   Solo UPDATE: no crea registros nuevos. Si falla, ROLLBACK.
--   Re-ejecutable si falla: los UPDATE son idempotentes.
-- Alcance:
--   Actualiza el usuario bootstrap GlobalAdmin creado por 021.1
--   con el Keycloak `sub` real del usuario. NO crea usuarios,
--   asignaciones ni matrices nuevas.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como KALLPA_PIIP.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- ============================================================================
-- 1) VALIDACIONES PREVIAS
-- ============================================================================
PROMPT [002] Validando precondiciones...

DECLARE
    v_count       PLS_INTEGER;
    v_id_usuario  NUMBER(10);
    v_keycloak_id VARCHAR2(36);

    PROCEDURE exigir(
        p_condicion IN BOOLEAN,
        p_codigo    IN PLS_INTEGER,
        p_mensaje   IN VARCHAR2
    ) IS
    BEGIN
        IF NOT p_condicion THEN
            RAISE_APPLICATION_ERROR(p_codigo, p_mensaje);
        END IF;
    END exigir;
BEGIN
    -- 1a) Debe existir el bootstrap creado por 021.1
    SELECT COUNT(*)
      INTO v_count
      FROM MATRIZ_FUNCION_PERFIL_UNIDAD
     WHERE ES_BOOTSTRAP = 'S' AND ACTIVA = 'S';

    exigir(v_count = 1, -20201,
           'No se encontro la combinacion bootstrap de 021.1. ' ||
           'Ejecute primero 021.1_bootstrap_matriz_fundacional.sql.');

    -- 1b) Debe existir un usuario GlobalAdmin con KEYCLOAK_ID placeholder
    SELECT COUNT(*)
      INTO v_count
      FROM USUARIO u
      JOIN USUARIO_ROL_UNIDAD uru ON uru.ID_USUARIO = u.ID_USUARIO
      JOIN ROL r ON r.ID_ROL = uru.ID_ROL
     WHERE r.NOMBRE_ROL = 'GlobalAdmin'
       AND uru.ACTIVO = 'S'
       AND u.ACTIVO = 'S';

    exigir(v_count = 1, -20202,
           'No se encontro un usuario GlobalAdmin activo. ' ||
           'Ejecute primero 021.1_bootstrap_matriz_fundacional.sql.');

    -- 1c) Verificar que el sub no este ya asignado a otro usuario
    SELECT COUNT(*)
      INTO v_count
      FROM USUARIO
     WHERE KEYCLOAK_ID = 'ed3742bc-f2c2-4884-ae09-07e3f9ab98fc'
       AND ACTIVO = 'S';

    exigir(v_count = 0, -20203,
           'El Keycloak sub ed3742bc-f2c2-4884-ae09-07e3f9ab98fc ya esta asignado ' ||
           'a otro usuario activo. Abortando.');

    DBMS_OUTPUT.PUT_LINE('[002] Precondiciones validadas.');
END;
/

-- ============================================================================
-- 2) UPDATE: vincular usuario bootstrap a Keycloak real
-- ============================================================================
PROMPT [002.1] Actualizando usuario GlobalAdmin con Keycloak sub real...

UPDATE USUARIO
   SET KEYCLOAK_ID     = 'ed3742bc-f2c2-4884-ae09-07e3f9ab98fc',
       LOGIN           = 'rovidev',
       NOMBRE_COMPLETO = 'Rovi Dev',
       CORREO          = 'rovidev@midagri.gob.pe',
       LOGIN_SINTETICO = 'N'
 WHERE ID_USUARIO = (
     SELECT u.ID_USUARIO
       FROM USUARIO u
       JOIN USUARIO_ROL_UNIDAD uru ON uru.ID_USUARIO = u.ID_USUARIO
       JOIN ROL r ON r.ID_ROL = uru.ID_ROL
      WHERE r.NOMBRE_ROL = 'GlobalAdmin'
        AND uru.ACTIVO = 'S'
        AND u.ACTIVO = 'S'
        AND ROWNUM = 1
 );

PROMPT [002.2] Filas actualizadas: ' || SQL%ROWCOUNT;

-- ============================================================================
-- 3) VALIDACION FINAL
-- ============================================================================
PROMPT [002.3] Validando...

DECLARE
    v_count PLS_INTEGER;
    v_keycloak_id VARCHAR2(36);

    PROCEDURE exigir(
        p_condicion IN BOOLEAN,
        p_codigo    IN PLS_INTEGER,
        p_mensaje   IN VARCHAR2
    ) IS
    BEGIN
        IF NOT p_condicion THEN
            RAISE_APPLICATION_ERROR(p_codigo, p_mensaje);
        END IF;
    END exigir;
BEGIN
    SELECT COUNT(*)
      INTO v_count
      FROM USUARIO
     WHERE KEYCLOAK_ID = 'ed3742bc-f2c2-4884-ae09-07e3f9ab98fc'
       AND LOGIN = 'rovidev'
       AND NOMBRE_COMPLETO = 'Rovi Dev'
       AND CORREO = 'rovidev@midagri.gob.pe'
       AND ACTIVO = 'S'
       AND LOGIN_SINTETICO = 'N';

    exigir(v_count = 1, -20211,
           'Fallo: No se encontro el usuario GlobalAdmin actualizado.');

    DBMS_OUTPUT.PUT_LINE('============================================================');
    DBMS_OUTPUT.PUT_LINE('  ACTUALIZACION 002 COMPLETADA EXITOSAMENTE');
    DBMS_OUTPUT.PUT_LINE('  Usuario: Rovi Dev (rovidev@midagri.gob.pe)');
    DBMS_OUTPUT.PUT_LINE('  Keycloak sub: ed3742bc-f2c2-4884-ae09-07e3f9ab98fc');
    DBMS_OUTPUT.PUT_LINE('  Rol: GlobalAdmin');
    DBMS_OUTPUT.PUT_LINE('============================================================');
END;
/

COMMIT;

PROMPT 002_seed_globaladmin.sql completada correctamente.
