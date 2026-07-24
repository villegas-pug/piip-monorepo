-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 027 - Unidad objetivo de
-- OPERACION_APROVISIONAMIENTO (T091)
-- Archivo       : 027_operacion_aprovisionamiento_unidad_objetivo.sql
-- Esquema       : KALLPA_PIIP
-- Modulo        : seguridad
-- Proposito     : conservar la unidad objetivo solicitada por cada operacion de
--                 aprovisionamiento para revalidar el alcance exacto de
--                 UnidadAdmin en consulta y reintento.
-- Dependencias  : 008.1 VIGENTE (tabla y secuencia de aprovisionamiento) y
--                 026 VIGENTE como incremento estructural predecesor.
-- Precondiciones: valida la huella fisica vigente de la tabla, UNIDAD_EJECUTORA,
--                 sus constraints e indices relevantes. La columna, FK e indice
--                 nuevos no deben existir; una huella parcial o futura aborta.
-- Ejecucion     : SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--                 KALLPA_PIIP. EJECUCION UNICA y FAIL-FAST.
-- Transaccion   : Oracle hace COMMIT implicito antes y despues de cada ALTER y
--                 CREATE INDEX. WHENEVER SQLERROR no revierte DDL ya aplicado.
-- Errores       : ORA-20090..ORA-20092 identifican precondiciones; ORA-20093
--                 identifica validacion final incompleta.
-- Auditoria     : no crea ni modifica eventos; preserva las operaciones y sus
--                 datos historicos. El servicio de seguridad es responsable de
--                 registrar y auditar la unidad en nuevas operaciones ordinarias.
-- Orden         : prevalidacion, columna nullable, FK, indice, validacion final.
-- Compensacion  : exclusivamente forward-only. Ante fallo posterior al DDL,
--                 detener altas/reintentos dependientes, preservar la columna y
--                 la huella aplicada, inventariarla con el DBA y depositar una
--                 correccion versionada. No eliminar objetos ni inventar unidad
--                 para operaciones historicas.
-- NEEDS CLARIFICATION: no existe un discriminador fisico que permita distinguir
--                 una fila historica de una operacion ordinaria nueva. Por
--                 compatibilidad, ID_UNIDAD_OBJETIVO queda nullable; el contrato
--                 backend debe rechazar su omision en nuevas operaciones
--                 ordinarias. Un NOT NULL o trigger futuro requiere regla y
--                 estrategia de migracion aprobadas.
-- Revision/aprobacion fisica: PENDIENTE en
--                 database/physical-design-approval.md antes de ejecutar.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [027] Validando huella vigente antes del primer DDL...

DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total FROM USER_TABLES
     WHERE TABLE_NAME IN ('OPERACION_APROVISIONAMIENTO', 'UNIDAD_EJECUTORA', 'INCORPORACION_REGISTRO');
    IF v_total <> 3 THEN RAISE_APPLICATION_ERROR(-20090, 'Precondicion 027: tablas de 008.1 o 026 incompletas'); END IF;

    SELECT COUNT(*) INTO v_total FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'OPERACION_APROVISIONAMIENTO'
       AND ((COLUMN_NAME = 'ID_OPERACION' AND DATA_TYPE = 'NUMBER' AND DATA_PRECISION = 12 AND DATA_SCALE = 0 AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'CLAVE_IDEMPOTENTE' AND DATA_TYPE = 'VARCHAR2' AND CHAR_LENGTH = 100 AND CHAR_USED = 'C' AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'HASH_PAYLOAD' AND DATA_TYPE = 'VARCHAR2' AND CHAR_LENGTH = 64 AND CHAR_USED = 'C' AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'ID_USUARIO_OBJETIVO' AND DATA_TYPE = 'NUMBER' AND DATA_PRECISION = 10 AND DATA_SCALE = 0 AND NULLABLE = 'Y')
         OR (COLUMN_NAME = 'KEYCLOAK_ID' AND DATA_TYPE = 'VARCHAR2' AND CHAR_LENGTH = 36 AND CHAR_USED = 'C' AND NULLABLE = 'Y')
         OR (COLUMN_NAME = 'ESTADO_TECNICO' AND DATA_TYPE = 'VARCHAR2' AND CHAR_LENGTH = 30 AND CHAR_USED = 'C' AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'INTENTO' AND DATA_TYPE = 'NUMBER' AND DATA_PRECISION = 3 AND DATA_SCALE = 0 AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'ERROR_RECUPERABLE' AND DATA_TYPE = 'CHAR' AND CHAR_LENGTH = 1 AND CHAR_USED = 'C' AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'RESULTADO_ORACLE' AND DATA_TYPE = 'VARCHAR2' AND CHAR_LENGTH = 2000 AND CHAR_USED = 'C' AND NULLABLE = 'Y')
         OR (COLUMN_NAME = 'CREADO_POR' AND DATA_TYPE = 'VARCHAR2' AND CHAR_LENGTH = 100 AND CHAR_USED = 'C' AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'FECHA_CREACION' AND DATA_TYPE = 'TIMESTAMP(6)' AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'FECHA_CIERRE' AND DATA_TYPE = 'TIMESTAMP(6)' AND NULLABLE = 'Y'));
    IF v_total <> 12 THEN RAISE_APPLICATION_ERROR(-20091, 'Precondicion 027: columnas vigentes de OPERACION_APROVISIONAMIENTO incompatibles'); END IF;

    SELECT COUNT(*) INTO v_total FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'OPERACION_APROVISIONAMIENTO';
    IF v_total <> 12 THEN RAISE_APPLICATION_ERROR(-20091, 'Precondicion 027: OPERACION_APROVISIONAMIENTO tiene columnas no esperadas'); END IF;

    SELECT COUNT(*) INTO v_total FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'OPERACION_APROVISIONAMIENTO'
       AND CONSTRAINT_NAME IN ('PK_OPERACION_APROVISIONAMIENTO', 'UK_OA_CLAVE', 'FK_OA_USUARIO_OBJETIVO')
       AND STATUS = 'ENABLED';
    IF v_total <> 3 THEN RAISE_APPLICATION_ERROR(-20091, 'Precondicion 027: constraints vigentes de aprovisionamiento incompletos'); END IF;

    SELECT COUNT(*) INTO v_total FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'UNIDAD_EJECUTORA' AND CONSTRAINT_NAME = 'PK_UNIDAD_EJECUTORA'
       AND CONSTRAINT_TYPE = 'P' AND STATUS = 'ENABLED';
    IF v_total <> 1 THEN RAISE_APPLICATION_ERROR(-20091, 'Precondicion 027: PK_UNIDAD_EJECUTORA ausente o deshabilitada'); END IF;

    SELECT COUNT(*) INTO v_total FROM USER_INDEXES
     WHERE INDEX_NAME IN ('IDX_OA_ESTADO', 'IDX_OA_USUARIO_OBJETIVO')
       AND TABLE_NAME = 'OPERACION_APROVISIONAMIENTO' AND STATUS = 'VALID';
    IF v_total <> 2 THEN RAISE_APPLICATION_ERROR(-20091, 'Precondicion 027: indices vigentes de aprovisionamiento incompletos'); END IF;

    SELECT COUNT(*) INTO v_total FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'OPERACION_APROVISIONAMIENTO' AND COLUMN_NAME = 'ID_UNIDAD_OBJETIVO';
    IF v_total <> 0 THEN RAISE_APPLICATION_ERROR(-20092, 'Precondicion 027: ID_UNIDAD_OBJETIVO ya existe; ejecucion repetida o huella parcial'); END IF;
    SELECT COUNT(*) INTO v_total FROM USER_CONSTRAINTS WHERE CONSTRAINT_NAME = 'FK_OA_UNIDAD_OBJETIVO';
    IF v_total <> 0 THEN RAISE_APPLICATION_ERROR(-20092, 'Precondicion 027: FK_OA_UNIDAD_OBJETIVO ya existe; huella parcial'); END IF;
    SELECT COUNT(*) INTO v_total FROM USER_INDEXES WHERE INDEX_NAME = 'IDX_OA_UNIDAD_OBJETIVO';
    IF v_total <> 0 THEN RAISE_APPLICATION_ERROR(-20092, 'Precondicion 027: IDX_OA_UNIDAD_OBJETIVO ya existe; huella parcial'); END IF;
END;
/

PROMPT [027] Precondiciones validadas. Agregando unidad objetivo compatible con historia...
ALTER TABLE OPERACION_APROVISIONAMIENTO ADD (ID_UNIDAD_OBJETIVO NUMBER(10));
PROMPT [027] Agregando FK de unidad objetivo...
ALTER TABLE OPERACION_APROVISIONAMIENTO ADD CONSTRAINT FK_OA_UNIDAD_OBJETIVO
    FOREIGN KEY (ID_UNIDAD_OBJETIVO) REFERENCES UNIDAD_EJECUTORA (ID_UNIDAD);
PROMPT [027] Creando indice para consulta y reintento por unidad objetivo...
CREATE INDEX IDX_OA_UNIDAD_OBJETIVO ON OPERACION_APROVISIONAMIENTO (ID_UNIDAD_OBJETIVO);

PROMPT [027] Validando estado final del incremento...
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'OPERACION_APROVISIONAMIENTO' AND COLUMN_NAME = 'ID_UNIDAD_OBJETIVO'
       AND DATA_TYPE = 'NUMBER' AND DATA_PRECISION = 10 AND DATA_SCALE = 0 AND NULLABLE = 'Y';
    IF v_total <> 1 THEN RAISE_APPLICATION_ERROR(-20093, 'Validacion 027: ID_UNIDAD_OBJETIVO ausente o incompatible'); END IF;
    SELECT COUNT(*) INTO v_total FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'OPERACION_APROVISIONAMIENTO' AND CONSTRAINT_NAME = 'FK_OA_UNIDAD_OBJETIVO'
       AND CONSTRAINT_TYPE = 'R' AND STATUS = 'ENABLED';
    IF v_total <> 1 THEN RAISE_APPLICATION_ERROR(-20093, 'Validacion 027: FK_OA_UNIDAD_OBJETIVO ausente o deshabilitada'); END IF;
    SELECT COUNT(*) INTO v_total FROM USER_INDEXES
     WHERE INDEX_NAME = 'IDX_OA_UNIDAD_OBJETIVO' AND TABLE_NAME = 'OPERACION_APROVISIONAMIENTO' AND STATUS = 'VALID';
    IF v_total <> 1 THEN RAISE_APPLICATION_ERROR(-20093, 'Validacion 027: IDX_OA_UNIDAD_OBJETIVO ausente o invalido'); END IF;
    SELECT COUNT(*) INTO v_total FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'OPERACION_APROVISIONAMIENTO';
    IF v_total <> 13 THEN RAISE_APPLICATION_ERROR(-20093, 'Validacion 027: conteo final de columnas incompatible'); END IF;
    DBMS_OUTPUT.PUT_LINE('Validacion final satisfactoria: incremento 027 aplicado correctamente.');
END;
/

COMMIT;
PROMPT Migracion 027_operacion_aprovisionamiento_unidad_objetivo completada correctamente.