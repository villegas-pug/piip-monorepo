-- ============================================================================
-- PIIP MIDAGRI - Correccion forward-only 003.1 - Contexto de tipo documental
-- Archivo    : 003.1_tipo_documento_contexto_nullable.sql
-- Esquema    : KALLPA_PIIP
-- Modulo     : documentos
-- Dependencia: 003_expediente_serie_version.sql VIGENTE.
--
-- Causa: 003 creo CK_TD_ESTADO_CONTEXTO para exigir ESTADO_ASOCIADO no nulo
--        en PORTAFOLIO y nulo en INSTITUCIONAL, pero conservo la nulabilidad
--        NOT NULL heredada de 001. Esto impide crear tipos institucionales.
--
-- Alcance: correccion forward-only limitada a permitir NULL en
--          TIPO_DOCUMENTO.ESTADO_ASOCIADO. No modifica filas, constraints ni
--          catalogos de tipos. El script 003 NO debe reejecutarse.
--
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: no revertir nulabilidad mientras existan tipos
--            institucionales; detener altas de tipos institucionales y
--            conservar catalogos.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [003.1] Validando huella parcial de TIPO_DOCUMENTO...

-- Todas las validaciones ocurren antes del unico DDL de esta correccion.
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20060,
            'Precondicion 003.1: TIPO_DOCUMENTO no existe');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND COLUMN_NAME = 'CONTEXTO'
       AND DATA_TYPE = 'VARCHAR2'
       AND CHAR_LENGTH = 20
       AND NULLABLE = 'N';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20061,
            'Precondicion 003.1: CONTEXTO no coincide con la huella vigente de 003');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND COLUMN_NAME = 'ESTADO_ASOCIADO'
       AND NULLABLE = 'N';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20062,
            'Precondicion 003.1: ESTADO_ASOCIADO no esta NOT NULL; no aplicar esta correccion');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND CONSTRAINT_NAME = 'CK_TD_ESTADO_CONTEXTO'
       AND CONSTRAINT_TYPE = 'C'
       AND STATUS = 'ENABLED';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20063,
            'Precondicion 003.1: CK_TD_ESTADO_CONTEXTO ausente o deshabilitado');
    END IF;

    -- 007 y 008/008.1 son predecesores vigentes permitidos; no se acepta
    -- ningun objeto de incrementos posteriores incompatible con esta revision.
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'DOCUMENTO_CLASIFICACION_HIST','DOCUMENTO_PUBLICACION',
            'CAT_OBJETIVO_PEI_VERSION','CAT_OBJETIVO_PEI',
            'CAT_ACTIVIDAD_POI_VERSION','CAT_ACTIVIDAD_POI',
            'INICIATIVA_PROYECTO','PROYECTO_RESPONSABLE',
            'PARTICIPANTE_PERSONA','PROYECTO_PARTICIPANTE_PERSONA',
            'PROYECTO_PARTICIPANTE_UNIDAD',
            'PROYECTO_CAMPO_CLASIFICACION','PROYECTO_CAMPO_CLASIF_HIST',
            'EVALUACION_INICIATIVA','SUBSANACION_INICIATIVA',
            'APLICABILIDAD_INICIATIVA','APLICABILIDAD_CRITERIO',
            'PLANIFICACION_PROYECTO','CICLO_PROYECTO','CICLO_EVIDENCIA',
            'PRODUCTO_PARCIAL','PRESENTACION_PRODUCTO_FINAL',
            'VALIDACION_RESULTADO','CIERRE_PROYECTO',
            'INCORPORACION_REGISTRO','INCORPORACION_CAMBIO',
            'INCORPORACION_CONFLICTO',
            'REPORTE_INSTITUCIONAL','REPORTE_SNAPSHOT','REPORTE_ARCHIVO',
            'REPORTE_APROBACION','REPORTE_DESTINATARIO','REPORTE_REMISION',
            'PROTOTIPO_PIIP','PROTOTIPO_VALIDACION','PROTOTIPO_HALLAZGO',
            'MEDICION_EXPERIENCIA','MEDICION_MUESTRA','MATRIZ_META_RECORRIDO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20064,
            'Precondicion 003.1: existen objetos de incrementos posteriores incompatibles');
    END IF;
END;
/

PROMPT [003.1] Huella validada. Corrigiendo nulabilidad de ESTADO_ASOCIADO...

ALTER TABLE TIPO_DOCUMENTO MODIFY (ESTADO_ASOCIADO NULL);

PROMPT [003.1] Validando correccion aplicada...
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND COLUMN_NAME = 'ESTADO_ASOCIADO'
       AND NULLABLE = 'Y';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20065,
            'Validacion 003.1: ESTADO_ASOCIADO no quedo nullable');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND CONSTRAINT_NAME = 'CK_TD_ESTADO_CONTEXTO'
       AND CONSTRAINT_TYPE = 'C'
       AND STATUS = 'ENABLED';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20066,
            'Validacion 003.1: CK_TD_ESTADO_CONTEXTO no permanece habilitado');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: correccion forward-only 003.1 aplicada correctamente.');
END;
/

-- El COMMIT final es solo documental; ALTER TABLE confirma implicitamente.
COMMIT;

PROMPT Correccion 003.1_tipo_documento_contexto_nullable completada correctamente.
