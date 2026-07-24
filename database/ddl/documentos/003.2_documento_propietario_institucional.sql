-- ============================================================================
-- PIIP MIDAGRI - Correccion forward-only 003.2 - Propietario institucional
-- Archivo    : 003.2_documento_propietario_institucional.sql
-- Esquema    : KALLPA_PIIP
-- Modulo     : documentos
-- Dependencias: 003 y 003.1 VIGENTES.
--
-- Causa: DOCUMENTO_SERIE ya protege el propietario exclusivo mediante
--        CK_DS_XOR_DUENIO, pero DOCUMENTO conserva ID_PROYECTO y
--        ESTADO_AL_CARGAR como NOT NULL desde el baseline. Esto impide una
--        version documental cuya serie pertenezca exclusivamente a un
--        EXPEDIENTE_INSTITUCIONAL, pues exigiria valores de portafolio
--        ficticios.
--
-- Alcance: permite NULL exclusivamente en DOCUMENTO.ID_PROYECTO y
--          DOCUMENTO.ESTADO_AL_CARGAR. Conserva FK_DOC_PROYECTO,
--          CK_DOC_ESTADO, UK_DOC_PROY_TIPO_VER y todos los documentos legacy.
--          CK_DS_XOR_DUENIO ya es la integridad estructural aprobada del
--          propietario de DOCUMENTO_SERIE; no se agrega una asociacion
--          polimorfica ni se duplican reglas Java. No reejecutar 003.
--
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: no volver obligatorios los campos mientras
--            existan series institucionales; detener altas institucionales y
--            conservar documentos legacy.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [003.2] Validando huella aplicada de 003 y 003.1...

DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN ('EXPEDIENTE_INSTITUCIONAL','DOCUMENTO_SERIE');
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20070,
            'Precondicion 003.2: faltan tablas EXPEDIENTE_INSTITUCIONAL o DOCUMENTO_SERIE');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND COLUMN_NAME IN ('ID_DOCUMENTO_SERIE','CONTENIDO','FORMATO');
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20071,
            'Precondicion 003.2: faltan columnas de versionado documental de 003');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND COLUMN_NAME = 'CONTEXTO'
       AND NULLABLE = 'N';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20072,
            'Precondicion 003.2: CONTEXTO no coincide con la huella de 003');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND COLUMN_NAME = 'ESTADO_ASOCIADO'
       AND NULLABLE = 'Y';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20073,
            'Precondicion 003.2: ESTADO_ASOCIADO no esta nullable; falta 003.1');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND COLUMN_NAME IN ('ID_PROYECTO','ESTADO_AL_CARGAR')
       AND NULLABLE = 'N';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20074,
            'Precondicion 003.2: ID_PROYECTO o ESTADO_AL_CARGAR no esta NOT NULL; no aplicar');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND CONSTRAINT_NAME IN ('FK_DOC_PROYECTO','CK_DOC_ESTADO')
       AND STATUS = 'ENABLED';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20075,
            'Precondicion 003.2: FK_DOC_PROYECTO o CK_DOC_ESTADO ausente/deshabilitado');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'DOCUMENTO_SERIE'
       AND CONSTRAINT_NAME = 'CK_DS_XOR_DUENIO'
       AND CONSTRAINT_TYPE = 'C'
       AND STATUS = 'ENABLED';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20076,
            'Precondicion 003.2: CK_DS_XOR_DUENIO ausente/deshabilitado');
    END IF;

    -- La correccion preserva filas legacy: antes del DDL no debe haber
    -- documentos legacy sin proyecto o estado de portafolio valido.
    SELECT COUNT(*) INTO v_total
      FROM DOCUMENTO
     WHERE ID_PROYECTO IS NULL
        OR ESTADO_AL_CARGAR IS NULL;
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20077,
            'Precondicion 003.2: existen documentos legacy sin proyecto o estado');
    END IF;
END;
/

PROMPT [003.2] Huella validada. Permitiendo propietario institucional sin valores ficticios...

ALTER TABLE DOCUMENTO MODIFY (ID_PROYECTO NULL);
ALTER TABLE DOCUMENTO MODIFY (ESTADO_AL_CARGAR NULL);

PROMPT [003.2] Validando correccion aplicada...
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND COLUMN_NAME IN ('ID_PROYECTO','ESTADO_AL_CARGAR')
       AND NULLABLE = 'Y';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20078,
            'Validacion 003.2: ID_PROYECTO o ESTADO_AL_CARGAR no quedo nullable');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND CONSTRAINT_NAME IN ('FK_DOC_PROYECTO','CK_DOC_ESTADO')
       AND STATUS = 'ENABLED';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20079,
            'Validacion 003.2: FK_DOC_PROYECTO o CK_DOC_ESTADO no permanece habilitado');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM DOCUMENTO
     WHERE ID_PROYECTO IS NULL
        OR ESTADO_AL_CARGAR IS NULL;
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20080,
            'Validacion 003.2: documentos legacy no conservaron proyecto o estado');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: correccion forward-only 003.2 aplicada correctamente.');
END;
/

-- El COMMIT final es solo documental; ALTER TABLE confirma implicitamente.
COMMIT;

PROMPT Correccion 003.2_documento_propietario_institucional completada correctamente.
