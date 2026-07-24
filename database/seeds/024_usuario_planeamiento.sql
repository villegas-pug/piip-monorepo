-- ============================================================================
-- PIIP MIDAGRI - Carga manual: usuario de planeamiento
-- Archivo: 024_usuario_planeamiento.sql
-- Esquema: KALLPA_PIIP
-- Modulo: seguridad
-- Dependencias: 008+008.1, 021 VIGENTES
-- Compensacion: conservar el usuario cargado; no eliminar.
-- ============================================================================
SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

-- Usuario de planeamiento
INSERT INTO USUARIO (ID_USUARIO, KEYCLOAK_ID, LOGIN, NOMBRE_COMPLETO, CORREO, ACTIVO, CREADO_POR, FECHA_CREACION, LOGIN_SINTETICO)
VALUES (SEQ_USUARIO.NEXTVAL, 'f58e6729-03e1-4340-b75c-69fe3fe3429d', NULL, 'Cristopher Guevara Villegas', 'test@midra.gob.pe', 'S', 'CARGA_MANUAL_024', SYSTIMESTAMP, 'S');

COMMIT;
PROMPT Carga manual 024 completada correctamente.