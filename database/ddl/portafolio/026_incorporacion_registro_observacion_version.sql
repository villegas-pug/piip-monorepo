-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 026 - Correccion T048 de
-- INCORPORACION_REGISTRO: observacion y concurrencia optimista
-- Archivo       : 026_incorporacion_registro_observacion_version.sql
-- Esquema       : KALLPA_PIIP
-- Modulo        : portafolio
-- Proposito     : agrega de forma aditiva OBSERVACION y VERSION a
--                 INCORPORACION_REGISTRO para alinear su estructura con el
--                 mapeo JPA vigente. VERSION habilita @Version con valor
--                 inicial 0 para filas existentes y nuevas.
-- Dependencias  : 016 VIGENTE (INCORPORACION_REGISTRO y su huella) y 025
--                 VIGENTE como ultimo incremento estructural precedente.
-- Precondiciones: se valida antes del primer DDL la tabla 016, sus once
--                 columnas fisicas vigentes y la huella de 025. OBSERVACION
--                 y VERSION no deben existir; cualquier huella parcial o
--                 futura aborta la ejecucion. No se opera sobre
--                 USER_TAB_COLUMNS.DATA_DEFAULT porque Oracle lo expone como
--                 LONG y TRIM/comparaciones producen ORA-00932.
-- Ejecucion     : SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--                 KALLPA_PIIP. EJECUCION UNICA y FAIL-FAST.
-- Transaccion   : Oracle confirma implicitamente antes y despues de ALTER
--                 TABLE. WHENEVER SQLERROR solo revierte DML no confirmado;
--                 no revierte DDL previo de este archivo.
-- Errores       : ORA-20080..ORA-20082 identifican precondiciones; ORA-20083
--                 identifica una validacion final incompleta.
-- Auditoria     : no crea evidencia de auditoria ni modifica filas de negocio.
--                 La concurrencia se materializa en VERSION para que JPA
--                 controle sus actualizaciones gestionadas.
-- Orden         : prevalidacion, ALTER TABLE aditivo, validacion final.
-- Compensacion  : exclusivamente forward-only. Ante fallo posterior al DDL,
--                 detener altas y actualizaciones de incorporacion, preservar
--                 filas y VERSION ya materializados, inventariar la huella con
--                 el DBA y depositar una correccion versionada. No eliminar
--                 columnas ni ejecutar rollback fisico.
-- NEEDS CLARIFICATION: el mapeo JPA vigente declara OBSERVACION con longitud
--                 2000, pero sin nullable=false; por fidelidad al mapeo se
--                 crea nullable y sin default. VERSION si esta declarado
--                 nullable=false y se crea NUMBER(10) DEFAULT 0 NOT NULL.
-- Revision DBA pendiente: tras una ejecucion satisfactoria, el DBA debe
--                 inspeccionar el DDL efectivo con DBMS_METADATA.GET_DDL o la
--                 herramienta Oracle equivalente y confirmar que VERSION
--                 conserva DEFAULT 0. Esta revision no bloquea el DDL porque
--                 DATA_DEFAULT es LONG y no admite manipulacion SQL segura.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [026] Validando huella 016 y predecesor 025 antes del primer DDL...

DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN ('INCORPORACION_REGISTRO', 'CICLO_PROYECTO_VERSION',
                          'PRESENTACION_PRODUCTO_FINAL_EVIDENCIA');
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20080,
            'Precondicion 026: huella de tablas 016/025 incompleta');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE (TABLE_NAME = 'INCORPORACION_REGISTRO'
            AND CONSTRAINT_NAME = 'PK_INCORPORACION_REGISTRO'
            AND CONSTRAINT_TYPE = 'P' AND STATUS = 'ENABLED')
        OR (TABLE_NAME = 'CICLO_PROYECTO_VERSION'
            AND CONSTRAINT_NAME = 'PK_CICLO_PROYECTO_VERSION'
            AND CONSTRAINT_TYPE = 'P' AND STATUS = 'ENABLED')
        OR (TABLE_NAME = 'PRESENTACION_PRODUCTO_FINAL_EVIDENCIA'
            AND CONSTRAINT_NAME = 'PK_PPF_EVIDENCIA'
            AND CONSTRAINT_TYPE = 'P' AND STATUS = 'ENABLED');
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20081,
            'Precondicion 026: PK requerida de 016 o 025 ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'INCORPORACION_REGISTRO'
       AND ((COLUMN_NAME = 'ID_INCORPORACION' AND DATA_TYPE = 'NUMBER'
             AND DATA_PRECISION = 12 AND DATA_SCALE = 0 AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'FUENTE' AND DATA_TYPE = 'VARCHAR2'
             AND CHAR_LENGTH = 200 AND CHAR_USED = 'C' AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'FECHA_FUENTE' AND DATA_TYPE = 'DATE' AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'ID_RESPONSABLE' AND DATA_TYPE = 'NUMBER'
             AND DATA_PRECISION = 10 AND DATA_SCALE = 0 AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'ID_DOCUMENTO_FUENTE' AND DATA_TYPE = 'NUMBER'
             AND DATA_PRECISION = 12 AND DATA_SCALE = 0 AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'HASH_ORIGINAL' AND DATA_TYPE = 'VARCHAR2'
             AND CHAR_LENGTH = 64 AND CHAR_USED = 'C' AND NULLABLE = 'N')
          OR (COLUMN_NAME = 'DATOS_ORIGINALES' AND DATA_TYPE = 'CLOB' AND NULLABLE = 'Y')
          OR (COLUMN_NAME = 'ESTADO' AND DATA_TYPE = 'VARCHAR2'
              AND CHAR_LENGTH = 20 AND CHAR_USED = 'C' AND NULLABLE = 'N')
         OR (COLUMN_NAME = 'ID_REGISTRO_VINCULADO' AND DATA_TYPE = 'NUMBER'
             AND DATA_PRECISION = 12 AND DATA_SCALE = 0 AND NULLABLE = 'Y')
          OR (COLUMN_NAME = 'CREADO_POR' AND DATA_TYPE = 'VARCHAR2'
              AND CHAR_LENGTH = 100 AND CHAR_USED = 'C' AND NULLABLE = 'N')
          OR (COLUMN_NAME = 'FECHA_CREACION' AND DATA_TYPE = 'TIMESTAMP(6)'
              AND NULLABLE = 'N'));
    IF v_total <> 11 THEN
        RAISE_APPLICATION_ERROR(-20082,
            'Precondicion 026: columnas vigentes de INCORPORACION_REGISTRO incompatibles');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'INCORPORACION_REGISTRO';
    IF v_total <> 11 THEN
        RAISE_APPLICATION_ERROR(-20082,
            'Precondicion 026: INCORPORACION_REGISTRO tiene columnas no esperadas');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'INCORPORACION_REGISTRO'
       AND COLUMN_NAME IN ('OBSERVACION', 'VERSION');
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20082,
            'Precondicion 026: OBSERVACION o VERSION ya existe; ejecucion repetida o huella parcial');
    END IF;
END;
/

PROMPT [026] Precondiciones validadas. Agregando columnas aditivas...

ALTER TABLE INCORPORACION_REGISTRO ADD (
    OBSERVACION VARCHAR2(2000 CHAR),
    VERSION      NUMBER(10) DEFAULT 0 NOT NULL
);

PROMPT [026] Validando estado final del incremento...

DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'INCORPORACION_REGISTRO'
       AND ((COLUMN_NAME = 'OBSERVACION' AND DATA_TYPE = 'VARCHAR2'
             AND CHAR_LENGTH = 2000 AND CHAR_USED = 'C' AND NULLABLE = 'Y')
         OR (COLUMN_NAME = 'VERSION' AND DATA_TYPE = 'NUMBER'
             AND DATA_PRECISION = 10 AND DATA_SCALE = 0 AND NULLABLE = 'N'));
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20083,
            'Validacion 026: OBSERVACION o VERSION ausente o incompatible');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'INCORPORACION_REGISTRO';
    IF v_total <> 13 THEN
        RAISE_APPLICATION_ERROR(-20083,
            'Validacion 026: conteo final de columnas incompatible');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 026 aplicado correctamente.');
    DBMS_OUTPUT.PUT_LINE(
        'Revision DBA pendiente: confirmar VERSION DEFAULT 0 mediante DBMS_METADATA.GET_DDL.');
END;
/

COMMIT;
PROMPT Migracion 026_incorporacion_registro_observacion_version completada correctamente.