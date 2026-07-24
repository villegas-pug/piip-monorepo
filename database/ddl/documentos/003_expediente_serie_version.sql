-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 003 - Expediente institucional,
-- serie documental, versionado sobre DOCUMENTO y contexto de TIPO_DOCUMENTO
-- Archivo   : 003_expediente_serie_version.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : documentos
-- Dependencia: 002 (database/ddl/auditoria/002_auditoria_idempotencia.sql) y
--              001 (database/ddl/init/001_baseline_piip.sql)
-- Alcance   : Crea EXPEDIENTE_INSTITUCIONAL, DOCUMENTO_SERIE, sus secuencias,
--             indices y constraints. Modifica TIPO_DOCUMENTO para distinguir
--             contexto PORTAFOLIO/INSTITUCIONAL y clasificacion por defecto.
--             Modifica DOCUMENTO para anadir la columna BLOB CONTENIDO, la
--             columna FORMATO y la FK a DOCUMENTO_SERIE. Neutraliza las
--             columnas legacy SCAN_ANTIVIRUS y NOMBRE_STORAGE (sin default,
--             sin CHECK, sin NOT NULL, sin consumidor) y eleva el limite de
--             TAMANO_BYTES a 104857600 bytes.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: detener nuevas cargas; conservar expedientes,
--            series y versiones. Las columnas legacy SCAN_ANTIVIRUS y
--            NOMBRE_STORAGE quedan nullable con su dato historico, sin
--            default ni constraint, y sin consumidor.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [003] Validando precondiciones del baseline 001 y del incremento 002...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones del baseline 001
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'UNIDAD_EJECUTORA','USUARIO','ROL','USUARIO_ROL_UNIDAD',
            'PROYECTO','PROYECTO_UNIDAD_ORGANICA','TRANSICION_PERMITIDA',
            'TIPO_DOCUMENTO','DOCUMENTO','TRANSICION_ESTADO',
            'SECUENCIA_CODIGO','AUDITORIA_ACCESO','AUDITORIA_EVENTO',
            'SOLICITUD_IDEMPOTENTE'
           );
    IF v_total <> 14 THEN
        RAISE_APPLICATION_ERROR(
            -20010,
            'Precondicion 003: se esperaban 14 tablas (13 baseline + 1 del incremento 002) y se encontraron '
            || TO_CHAR(v_total)
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_UNIDAD_EJECUTORA','SEQ_USUARIO','SEQ_USUARIO_ROL_UNIDAD',
            'SEQ_PROYECTO','SEQ_PROYECTO_UO','SEQ_DOCUMENTO',
            'SEQ_TRANSICION_ESTADO','SEQ_SECUENCIA_CODIGO',
            'SEQ_AUDITORIA_ACCESO','SEQ_AUDITORIA_EVENTO',
            'SEQ_SOLICITUD_IDEMPOTENTE'
           );
    IF v_total <> 11 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Precondicion 003: se esperaban 11 secuencias y se encontraron '
            || TO_CHAR(v_total)
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de las columnas vigentes en TIPO_DOCUMENTO y DOCUMENTO
-- ----------------------------------------------------------------------------
DECLARE
    v_columnas_tipo_doc VARCHAR2(4000);
    v_columnas_documento VARCHAR2(4000);
BEGIN
    SELECT LISTAGG(COLUMN_NAME, ',') WITHIN GROUP (ORDER BY COLUMN_ID)
      INTO v_columnas_tipo_doc
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO';
    IF v_columnas_tipo_doc IS NULL OR
       v_columnas_tipo_doc <>
       'ID_TIPO_DOC,NOMBRE,ESTADO_ASOCIADO,OBLIGATORIO,DESCRIPCION,ANEXO_NT,ACTIVO' THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 003: TIPO_DOCUMENTO no coincide con la huella combinada del baseline 001 e incremento 002'
        );
    END IF;

    SELECT LISTAGG(COLUMN_NAME, ',') WITHIN GROUP (ORDER BY COLUMN_ID)
      INTO v_columnas_documento
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'DOCUMENTO';
    IF v_columnas_documento IS NULL OR
       v_columnas_documento <>
       'ID_DOCUMENTO,ID_PROYECTO,ID_TIPO_DOC,ESTADO_AL_CARGAR,NOMBRE_ORIGINAL,NOMBRE_STORAGE,MIME_TYPE,TAMANO_BYTES,HASH_SHA256,ID_USUARIO_CARGA,FECHA_CARGA,ACTIVO,INMUTABLE,SCAN_ANTIVIRUS,NUMERO_VERSION,ID_DOCUMENTO_ANTERIOR,CLASIFICACION' THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 003: DOCUMENTO no coincide con la huella combinada del baseline 001 e incremento 002'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de constraints baseline en DOCUMENTO y TIPO_DOCUMENTO
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND CONSTRAINT_NAME IN (
            'PK_DOCUMENTO','UK_DOC_STORAGE','UK_DOC_PROY_TIPO_VER',
            'FK_DOC_PROYECTO','FK_DOC_TIPO','FK_DOC_USUARIO','FK_DOC_ANTERIOR',
            'CK_DOC_TAMANO','CK_DOC_ACTIVO','CK_DOC_INMUTABLE','CK_DOC_SCAN',
            'CK_DOC_VERSION','CK_DOC_ANTERIOR_DIST','CK_DOC_ESTADO',
            'CK_DOC_HASH','CK_DOC_MIME'
           );
    IF v_total <> 16 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 003: 16 constraints baseline/002 esperadas en DOCUMENTO, halladas '
            || TO_CHAR(v_total)
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND CONSTRAINT_NAME IN (
            'PK_TIPO_DOCUMENTO','UK_TIPO_DOC_NOMBRE',
            'CK_TD_ESTADO','CK_TD_OBLIGATORIO','CK_TD_ACTIVO'
           );
    IF v_total <> 5 THEN
        RAISE_APPLICATION_ERROR(
            -20015,
            'Precondicion 003: 5 constraints baseline esperadas en TIPO_DOCUMENTO, halladas '
            || TO_CHAR(v_total)
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 4) Validacion de inexistencia de objetos futuros 003-024
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'EXPEDIENTE_INSTITUCIONAL','DOCUMENTO_SERIE',
            'DOCUMENTO_CLASIFICACION_HIST','DOCUMENTO_PUBLICACION',
            'CAT_OBJETIVO_PEI_VERSION','CAT_OBJETIVO_PEI',
            'CAT_ACTIVIDAD_POI_VERSION','CAT_ACTIVIDAD_POI',
            'MATRIZ_FUNCIONAL_VERSION','MATRIZ_FUNCION','MATRIZ_FUNCION_PERFIL_UNIDAD',
            'USUARIO_ROL_UNIDAD_EVENTO','SUPLENCIA_FUNCIONAL','OPERACION_APROVISIONAMIENTO',
            'INICIATIVA_PROYECTO','PROYECTO_RESPONSABLE',
            'PARTICIPANTE_PERSONA','PROYECTO_PARTICIPANTE_PERSONA','PROYECTO_PARTICIPANTE_UNIDAD',
            'PROYECTO_CAMPO_CLASIFICACION','PROYECTO_CAMPO_CLASIF_HIST',
            'EVALUACION_INICIATIVA','SUBSANACION_INICIATIVA','APLICABILIDAD_INICIATIVA',
            'APLICABILIDAD_CRITERIO',
            'PLANIFICACION_PROYECTO','CICLO_PROYECTO','CICLO_EVIDENCIA','PRODUCTO_PARCIAL',
            'PRESENTACION_PRODUCTO_FINAL','VALIDACION_RESULTADO','CIERRE_PROYECTO',
            'INCORPORACION_REGISTRO','INCORPORACION_CAMBIO','INCORPORACION_CONFLICTO',
            'REPORTE_INSTITUCIONAL','REPORTE_SNAPSHOT','REPORTE_ARCHIVO','REPORTE_APROBACION',
            'REPORTE_DESTINATARIO','REPORTE_REMISION',
            'PROTOTIPO_PIIP','PROTOTIPO_VALIDACION','PROTOTIPO_HALLAZGO','MEDICION_EXPERIENCIA',
            'MEDICION_MUESTRA','MATRIZ_META_RECORRIDO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20016,
            'Precondicion 003: existen objetos futuros 004-024 ya creados'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_EXPEDIENTE_INSTITUCIONAL','SEQ_DOCUMENTO_SERIE'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20017,
            'Precondicion 003: las secuencias del incremento 003 ya existen'
        );
    END IF;
END;
/

PROMPT [003] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 003
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencias para las nuevas tablas EXPEDIENTE_INSTITUCIONAL y
--    DOCUMENTO_SERIE. Cada CREATE SEQUENCE ejecuta un commit implicito.
-- ----------------------------------------------------------------------------
PROMPT [003.1] Creando secuencias SEQ_EXPEDIENTE_INSTITUCIONAL y SEQ_DOCUMENTO_SERIE
CREATE SEQUENCE SEQ_EXPEDIENTE_INSTITUCIONAL START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE SEQ_DOCUMENTO_SERIE         START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Tabla EXPEDIENTE_INSTITUCIONAL. La columna VERSIONNUMBER(10) actua
--    como @Version para evitar updates concurrentes desde Java.
-- ----------------------------------------------------------------------------
PROMPT [003.2] Creando tabla EXPEDIENTE_INSTITUCIONAL
CREATE TABLE EXPEDIENTE_INSTITUCIONAL (
    ID_EXPEDIENTE       NUMBER(12)                      NOT NULL,
    CODIGO              VARCHAR2(30 CHAR)               NOT NULL,
    ASUNTO              VARCHAR2(500 CHAR)              NOT NULL,
    MODULO_ORIGEN       VARCHAR2(50 CHAR)               NOT NULL,
    REFERENCIA_CASO_USO VARCHAR2(100 CHAR)              NOT NULL,
    ID_UNIDAD           NUMBER(10),
    CLASIFICACION       VARCHAR2(20 CHAR)               NOT NULL,
    ACTIVO              CHAR(1 CHAR) DEFAULT 'S'        NOT NULL,
    VERSION             NUMBER(10) DEFAULT 0            NOT NULL,
    CREADO_POR          VARCHAR2(100 CHAR)              NOT NULL,
    FECHA_CREACION      TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    MODIFICADO_POR      VARCHAR2(100 CHAR),
    FECHA_MODIFICACION  TIMESTAMP(6)
);

PROMPT [003.3] PK, UK, FKs y CHECKs de EXPEDIENTE_INSTITUCIONAL
ALTER TABLE EXPEDIENTE_INSTITUCIONAL
    ADD CONSTRAINT PK_EXPEDIENTE_INSTITUCIONAL PRIMARY KEY (ID_EXPEDIENTE);

ALTER TABLE EXPEDIENTE_INSTITUCIONAL
    ADD CONSTRAINT UK_EI_CODIGO UNIQUE (CODIGO);

ALTER TABLE EXPEDIENTE_INSTITUCIONAL
    ADD CONSTRAINT FK_EI_UNIDAD
    FOREIGN KEY (ID_UNIDAD) REFERENCES UNIDAD_EJECUTORA (ID_UNIDAD);

ALTER TABLE EXPEDIENTE_INSTITUCIONAL
    ADD CONSTRAINT CK_EI_ACTIVO
    CHECK (ACTIVO IN ('S','N'));

ALTER TABLE EXPEDIENTE_INSTITUCIONAL
    ADD CONSTRAINT CK_EI_CLASIFICACION
    CHECK (CLASIFICACION IN ('PUBLICO','INTERNO','RESTRINGIDO'));

ALTER TABLE EXPEDIENTE_INSTITUCIONAL
    ADD CONSTRAINT CK_EI_MODULO
    CHECK (MODULO_ORIGEN IN ('ORGANIZACION','SEGURIDAD','PORTAFOLIO',
                              'DOCUMENTOS','REPORTES','CONSULTA','AUDITORIA'));

PROMPT [003.4] Creando indice IDX_EI_UNIDAD
CREATE INDEX IDX_EI_UNIDAD ON EXPEDIENTE_INSTITUCIONAL (ID_UNIDAD);

-- ----------------------------------------------------------------------------
-- 3) Tabla DOCUMENTO_SERIE con CHECK XOR para garantizar pertenencia
--    exclusiva a un registro de portafolio o a un expediente institucional.
-- ----------------------------------------------------------------------------
PROMPT [003.5] Creando tabla DOCUMENTO_SERIE
CREATE TABLE DOCUMENTO_SERIE (
    ID_SERIE                NUMBER(12)                      NOT NULL,
    ID_TIPO_DOC             NUMBER(5)                       NOT NULL,
    ID_REGISTRO             NUMBER(12),
    ID_EXPEDIENTE           NUMBER(12),
    TITULO                  VARCHAR2(500 CHAR)              NOT NULL,
    CLASIFICACION_PROPUESTA VARCHAR2(20 CHAR)               NOT NULL,
    CLASIFICACION_VALIDADA  VARCHAR2(20 CHAR),
    ACTIVA                  CHAR(1 CHAR) DEFAULT 'S'        NOT NULL,
    VERSION                 NUMBER(10) DEFAULT 0            NOT NULL,
    CREADO_POR              VARCHAR2(100 CHAR)              NOT NULL,
    FECHA_CREACION          TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [003.6] PK, UK, FKs y CHECKs de DOCUMENTO_SERIE
ALTER TABLE DOCUMENTO_SERIE
    ADD CONSTRAINT PK_DOCUMENTO_SERIE PRIMARY KEY (ID_SERIE);

ALTER TABLE DOCUMENTO_SERIE
    ADD CONSTRAINT UK_DS_TITULO_TIPO UNIQUE (ID_TIPO_DOC, TITULO);

ALTER TABLE DOCUMENTO_SERIE
    ADD CONSTRAINT FK_DS_TIPO_DOC
    FOREIGN KEY (ID_TIPO_DOC) REFERENCES TIPO_DOCUMENTO (ID_TIPO_DOC);

ALTER TABLE DOCUMENTO_SERIE
    ADD CONSTRAINT FK_DS_REGISTRO
    FOREIGN KEY (ID_REGISTRO) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE DOCUMENTO_SERIE
    ADD CONSTRAINT FK_DS_EXPEDIENTE
    FOREIGN KEY (ID_EXPEDIENTE) REFERENCES EXPEDIENTE_INSTITUCIONAL (ID_EXPEDIENTE);

ALTER TABLE DOCUMENTO_SERIE
    ADD CONSTRAINT CK_DS_XOR_DUENIO
    CHECK ((ID_REGISTRO IS NOT NULL AND ID_EXPEDIENTE IS NULL)
        OR (ID_REGISTRO IS NULL AND ID_EXPEDIENTE IS NOT NULL));

ALTER TABLE DOCUMENTO_SERIE
    ADD CONSTRAINT CK_DS_CLAS_PROPUESTA
    CHECK (CLASIFICACION_PROPUESTA IN ('PUBLICO','INTERNO','RESTRINGIDO'));

ALTER TABLE DOCUMENTO_SERIE
    ADD CONSTRAINT CK_DS_CLAS_VALIDADA
    CHECK (CLASIFICACION_VALIDADA IS NULL
        OR CLASIFICACION_VALIDADA IN ('PUBLICO','INTERNO','RESTRINGIDO'));

ALTER TABLE DOCUMENTO_SERIE
    ADD CONSTRAINT CK_DS_ACTIVA
    CHECK (ACTIVA IN ('S','N'));

PROMPT [003.7] Creando indices IDX_DS_REGISTRO, IDX_DS_EXPEDIENTE, IDX_DS_TIPO
CREATE INDEX IDX_DS_REGISTRO   ON DOCUMENTO_SERIE (ID_REGISTRO);
CREATE INDEX IDX_DS_EXPEDIENTE ON DOCUMENTO_SERIE (ID_EXPEDIENTE);
CREATE INDEX IDX_DS_TIPO       ON DOCUMENTO_SERIE (ID_TIPO_DOC);

-- ----------------------------------------------------------------------------
-- 4) Modificacion de TIPO_DOCUMENTO: anadir CONTEXTO y
--    CLASIFICACION_DEFECTO. Backfill para filas existentes antes de aplicar
--    NOT NULL.
-- ----------------------------------------------------------------------------
PROMPT [003.8] Anadiendo columnas CONTEXTO y CLASIFICACION_DEFECTO a TIPO_DOCUMENTO
ALTER TABLE TIPO_DOCUMENTO ADD (CONTEXTO             VARCHAR2(20 CHAR));
ALTER TABLE TIPO_DOCUMENTO ADD (CLASIFICACION_DEFECTO VARCHAR2(20 CHAR));

PROMPT [003.9] Backfill de CONTEXTO y CLASIFICACION_DEFECTO en filas existentes
UPDATE TIPO_DOCUMENTO
   SET CONTEXTO             = 'PORTAFOLIO',
       CLASIFICACION_DEFECTO = 'INTERNO'
 WHERE CONTEXTO IS NULL;

PROMPT [003.10] Aplicando NOT NULL a CONTEXTO y CHECKs de TIPO_DOCUMENTO
ALTER TABLE TIPO_DOCUMENTO MODIFY (CONTEXTO NOT NULL);

ALTER TABLE TIPO_DOCUMENTO
    ADD CONSTRAINT CK_TD_CONTEXTO
    CHECK (CONTEXTO IN ('PORTAFOLIO','INSTITUCIONAL'));

ALTER TABLE TIPO_DOCUMENTO
    ADD CONSTRAINT CK_TD_CLAS_DEFECTO
    CHECK (CLASIFICACION_DEFECTO IS NULL
        OR CLASIFICACION_DEFECTO IN ('PUBLICO','INTERNO','RESTRINGIDO'));

ALTER TABLE TIPO_DOCUMENTO
    ADD CONSTRAINT CK_TD_ESTADO_CONTEXTO
    CHECK ((CONTEXTO = 'PORTAFOLIO' AND ESTADO_ASOCIADO IS NOT NULL)
        OR (CONTEXTO = 'INSTITUCIONAL' AND ESTADO_ASOCIADO IS NULL));

-- ----------------------------------------------------------------------------
-- 5) Modificacion de DOCUMENTO: agregar CONTENIDO BLOB, FORMATO y
--    ID_DOCUMENTO_SERIE. La columna BLOB se crea como LOB en linea; el
--    tamano se controla en aplicacion (maximo 100 MB).
-- ----------------------------------------------------------------------------
PROMPT [003.11] Anadiendo columnas CONTENIDO, FORMATO e ID_DOCUMENTO_SERIE a DOCUMENTO
ALTER TABLE DOCUMENTO ADD (CONTENIDO          BLOB);
ALTER TABLE DOCUMENTO ADD (FORMATO            VARCHAR2(20 CHAR));
ALTER TABLE DOCUMENTO ADD (ID_DOCUMENTO_SERIE NUMBER(12));

PROMPT [003.12] FK a DOCUMENTO_SERIE e indice
ALTER TABLE DOCUMENTO
    ADD CONSTRAINT FK_DOC_SERIE
    FOREIGN KEY (ID_DOCUMENTO_SERIE) REFERENCES DOCUMENTO_SERIE (ID_SERIE);

CREATE INDEX IDX_DOC_SERIE ON DOCUMENTO (ID_DOCUMENTO_SERIE);

-- ----------------------------------------------------------------------------
-- 6) Elevar el limite de TAMANO_BYTES a 100 MB y recrear el CHECK.
-- ----------------------------------------------------------------------------
PROMPT [003.13] Recreando CK_DOC_TAMANO con limite 100 MB
ALTER TABLE DOCUMENTO DROP CONSTRAINT CK_DOC_TAMANO;
ALTER TABLE DOCUMENTO
    ADD CONSTRAINT CK_DOC_TAMANO
    CHECK (TAMANO_BYTES > 0 AND TAMANO_BYTES <= 104857600);

-- ----------------------------------------------------------------------------
-- 7) Neutralizar las columnas legacy SCAN_ANTIVIRUS y NOMBRE_STORAGE.
--    Se vuelven nullable, sin default, sin CHECK, sin NOT NULL.
-- ----------------------------------------------------------------------------
PROMPT [003.14] Neutralizando columnas legacy SCAN_ANTIVIRUS y NOMBRE_STORAGE
ALTER TABLE DOCUMENTO DROP CONSTRAINT CK_DOC_SCAN;
ALTER TABLE DOCUMENTO MODIFY (NOMBRE_STORAGE NULL);
ALTER TABLE DOCUMENTO MODIFY (SCAN_ANTIVIRUS NULL);
ALTER TABLE DOCUMENTO MODIFY (SCAN_ANTIVIRUS DEFAULT NULL);

-- ============================================================================
-- Validacion final del script 003
-- ============================================================================
PROMPT [003.15] Validando estado final del incremento
DECLARE
    v_total              PLS_INTEGER;
    v_search_tamano      USER_CONSTRAINTS.SEARCH_CONDITION_VC%TYPE;
BEGIN
    -- Tablas nuevas
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN ('EXPEDIENTE_INSTITUCIONAL','DOCUMENTO_SERIE');
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20030,
            'Validacion 003: tablas nuevas ausentes');
    END IF;

    -- Secuencias nuevas
    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN ('SEQ_EXPEDIENTE_INSTITUCIONAL','SEQ_DOCUMENTO_SERIE');
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 003: secuencias nuevas ausentes');
    END IF;

    -- PKs
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'EXPEDIENTE_INSTITUCIONAL'
       AND CONSTRAINT_NAME = 'PK_EXPEDIENTE_INSTITUCIONAL'
       AND CONSTRAINT_TYPE = 'P';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20032,
            'Validacion 003: PK_EXPEDIENTE_INSTITUCIONAL ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'DOCUMENTO_SERIE'
       AND CONSTRAINT_NAME = 'PK_DOCUMENTO_SERIE'
       AND CONSTRAINT_TYPE = 'P';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20033,
            'Validacion 003: PK_DOCUMENTO_SERIE ausente');
    END IF;

    -- UKs
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME IN ('EXPEDIENTE_INSTITUCIONAL','DOCUMENTO_SERIE')
       AND ((TABLE_NAME = 'EXPEDIENTE_INSTITUCIONAL'
             AND CONSTRAINT_NAME = 'UK_EI_CODIGO')
         OR (TABLE_NAME = 'DOCUMENTO_SERIE'
             AND CONSTRAINT_NAME = 'UK_DS_TITULO_TIPO'));
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20034,
            'Validacion 003: UKs nuevas ausentes');
    END IF;

    -- CHECKs
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'EXPEDIENTE_INSTITUCIONAL'
       AND CONSTRAINT_NAME IN ('CK_EI_ACTIVO','CK_EI_CLASIFICACION','CK_EI_MODULO')
       AND CONSTRAINT_TYPE = 'C';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20035,
            'Validacion 003: CHECKs de EXPEDIENTE_INSTITUCIONAL ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'DOCUMENTO_SERIE'
       AND CONSTRAINT_NAME IN ('CK_DS_XOR_DUENIO','CK_DS_CLAS_PROPUESTA',
                               'CK_DS_CLAS_VALIDADA','CK_DS_ACTIVA')
       AND CONSTRAINT_TYPE = 'C';
    IF v_total <> 4 THEN
        RAISE_APPLICATION_ERROR(-20036,
            'Validacion 003: CHECKs de DOCUMENTO_SERIE ausentes');
    END IF;

    -- TIPO_DOCUMENTO: nuevas columnas y CHECKs
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND COLUMN_NAME IN ('CONTEXTO','CLASIFICACION_DEFECTO');
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20037,
            'Validacion 003: CONTEXTO/CLASIFICACION_DEFECTO ausentes en TIPO_DOCUMENTO');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND CONSTRAINT_NAME IN ('CK_TD_CONTEXTO','CK_TD_CLAS_DEFECTO','CK_TD_ESTADO_CONTEXTO')
       AND CONSTRAINT_TYPE = 'C';
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20038,
            'Validacion 003: CHECKs nuevos de TIPO_DOCUMENTO ausentes');
    END IF;

    -- DOCUMENTO: nuevas columnas y FK
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND COLUMN_NAME IN ('CONTENIDO','FORMATO','ID_DOCUMENTO_SERIE');
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20039,
            'Validacion 003: columnas nuevas de DOCUMENTO ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND CONSTRAINT_NAME = 'FK_DOC_SERIE'
       AND CONSTRAINT_TYPE = 'R';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20040,
            'Validacion 003: FK_DOC_SERIE ausente');
    END IF;

    -- DOCUMENTO: CK_DOC_SCAN debe estar eliminado
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND CONSTRAINT_NAME = 'CK_DOC_SCAN';
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20041,
            'Validacion 003: CK_DOC_SCAN aun existe; debe estar neutralizado');
    END IF;

    -- DOCUMENTO: SCAN_ANTIVIRUS y NOMBRE_STORAGE deben ser NULL
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND COLUMN_NAME IN ('SCAN_ANTIVIRUS','NOMBRE_STORAGE')
       AND NULLABLE = 'N';
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20042,
            'Validacion 003: SCAN_ANTIVIRUS o NOMBRE_STORAGE aun son NOT NULL');
    END IF;

    -- DOCUMENTO: CK_DOC_TAMANO elevado a 100 MB
    SELECT SEARCH_CONDITION_VC INTO v_search_tamano
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND CONSTRAINT_NAME = 'CK_DOC_TAMANO';
    IF INSTR(UPPER(v_search_tamano), '104857600') = 0 THEN
        RAISE_APPLICATION_ERROR(-20043,
            'Validacion 003: CK_DOC_TAMANO no refleja el limite 100 MB');
    END IF;

    -- Indices auxiliares
    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME IN (
            'IDX_EI_UNIDAD',
            'IDX_DS_REGISTRO','IDX_DS_EXPEDIENTE','IDX_DS_TIPO',
            'IDX_DOC_SERIE'
           )
       AND STATUS = 'VALID';
    IF v_total <> 5 THEN
        RAISE_APPLICATION_ERROR(-20044,
            'Validacion 003: indices auxiliares del incremento ausentes o invalidos');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 003 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 003_expediente_serie_version completada correctamente.
