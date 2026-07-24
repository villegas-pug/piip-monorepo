-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 002 - Auditoria e idempotencia
-- Archivo   : 002_auditoria_idempotencia.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : auditoria
-- Dependencia: 001 (database/ddl/init/001_baseline_piip.sql)
-- Alcance   : Crea SOLICITUD_IDEMPOTENTE y SEQ_SOLICITUD_IDEMPOTENTE;
--             amplia AUDITORIA_ACCESO con las columnas de contexto efectivo
--             (ID_ROL_EFECTIVO, ID_UNIDAD_EFECTIVA, ID_ASIGNACION_EFECTIVA)
--             y los indices auxiliares. Las FKs hacia USUARIO_ROL_UNIDAD y
--             hacia ROL/UNIDAD_EJECUTORA se difieren al script 008, que
--             introduce la combinacion de matriz y la vigencia de la
--             asignacion. Las FKs hacia ROL y UNIDAD_EJECUTORA tambien
--             se difieren a 008 para mantener un orden de ejecucion sin
--             dependencias hacia objetos no creados todavia; los indices
--             sobre las columnas efectivas se crean en este script.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: detener escritura de los nuevos campos y
--            dejar de consultar SOLICITUD_IDEMPOTENTE; nunca eliminar
--            auditoria ni claves ya consumidas.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [002] Validando precondiciones del baseline 001 y de objetos futuros...

-- ----------------------------------------------------------------------------
-- Huella de precondiciones del baseline 001 (USER_TABLES).
-- Se exige la presencia exacta de las 13 tablas del baseline.
-- ----------------------------------------------------------------------------
DECLARE
    v_total_tablas_baseline PLS_INTEGER;
    v_objetos_futuros_inesperados PLS_INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO v_total_tablas_baseline
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'UNIDAD_EJECUTORA','USUARIO','ROL','USUARIO_ROL_UNIDAD',
            'PROYECTO','PROYECTO_UNIDAD_ORGANICA','TRANSICION_PERMITIDA',
            'TIPO_DOCUMENTO','DOCUMENTO','TRANSICION_ESTADO',
            'SECUENCIA_CODIGO','AUDITORIA_ACCESO','AUDITORIA_EVENTO'
           );

    IF v_total_tablas_baseline <> 13 THEN
        RAISE_APPLICATION_ERROR(
            -20010,
            'Precondicion 002: se esperaban 13 tablas del baseline 001 y se encontraron '
            || TO_CHAR(v_total_tablas_baseline)
        );
    END IF;

    -- Verifica que ningun objeto futuro 002-024 exista ya. Si existe, aborta
    -- porque podria tratarse de una ejecucion parcial previa o de un deposito
    -- no documentado.
    SELECT COUNT(*)
      INTO v_objetos_futuros_inesperados
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'SOLICITUD_IDEMPOTENTE',
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

    IF v_objetos_futuros_inesperados <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Precondicion 002: existen objetos futuros 003-024 ya creados; revierta antes de continuar'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- Validacion explicita de columnas de AUDITORIA_ACCESO segun baseline 001.
-- Si cambian las columnas base, el script debe actualizarse y volver a
-- aprobarse.
-- ----------------------------------------------------------------------------
DECLARE
    v_columnas_esperadas CONSTANT VARCHAR2(4000) :=
        'ID_AUDIT,ID_USUARIO,ENDPOINT,METODO_HTTP,CODIGO_RESPUESTA,IP_CLIENTE,FECHA_HORA,DURACION_MS';
    v_columnas_encontradas VARCHAR2(4000);
BEGIN
    SELECT LISTAGG(COLUMN_NAME, ',') WITHIN GROUP (ORDER BY COLUMN_ID)
      INTO v_columnas_encontradas
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'AUDITORIA_ACCESO';

    IF v_columnas_encontradas IS NULL OR v_columnas_encontradas <> v_columnas_esperadas THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 002: AUDITORIA_ACCESO no coincide con la huella del baseline 001'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- Validacion de constraints de AUDITORIA_ACCESO. El script exige el
-- conjunto minimo declarado en el baseline (PK, FK a USUARIO, CHECK de
-- metodo, codigo y duracion).
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'AUDITORIA_ACCESO'
       AND CONSTRAINT_NAME IN (
            'PK_AUDITORIA_ACCESO','FK_AA_USUARIO',
            'CK_AA_METODO','CK_AA_RESPUESTA','CK_AA_DURACION'
           );

    IF v_total <> 5 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 002: AUDITORIA_ACCESO no expone las 5 constraints del baseline 001'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- Validacion de indices baseline relevantes para auditoria.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME IN ('IDX_AA_USUARIO','IDX_AA_FECHA')
       AND STATUS = 'VALID';

    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 002: indices baseline de AUDITORIA_ACCESO ausentes o invalidos'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- Validacion de las 10 secuencias del baseline.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_UNIDAD_EJECUTORA','SEQ_USUARIO','SEQ_USUARIO_ROL_UNIDAD',
            'SEQ_PROYECTO','SEQ_PROYECTO_UO','SEQ_DOCUMENTO',
            'SEQ_TRANSICION_ESTADO','SEQ_SECUENCIA_CODIGO',
            'SEQ_AUDITORIA_ACCESO','SEQ_AUDITORIA_EVENTO'
           );

    IF v_total <> 10 THEN
        RAISE_APPLICATION_ERROR(
            -20015,
            'Precondicion 002: se esperaban 10 secuencias del baseline 001'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- Validacion de que no exista la secuencia objetivo de este script.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*)
      INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME = 'SEQ_SOLICITUD_IDEMPOTENTE';

    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20016,
            'Precondicion 002: la secuencia SEQ_SOLICITUD_IDEMPOTENTE ya existe'
        );
    END IF;
END;
/

PROMPT [002] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 002
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencia para SOLICITUD_IDEMPOTENTE. Conlleva commit implicito.
-- ----------------------------------------------------------------------------
PROMPT [002.1] Creando secuencia SEQ_SOLICITUD_IDEMPOTENTE
CREATE SEQUENCE SEQ_SOLICITUD_IDEMPOTENTE START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Tabla SOLICITUD_IDEMPOTENTE. La ventana de retencion operativa se
--    calcula en la aplicacion; este script no la enuncia como default.
-- ----------------------------------------------------------------------------
PROMPT [002.2] Creando tabla SOLICITUD_IDEMPOTENTE
CREATE TABLE SOLICITUD_IDEMPOTENTE (
    ID_SOLICITUD       NUMBER(15)                      NOT NULL,
    CONSUMIDOR         VARCHAR2(100 CHAR)              NOT NULL,
    OPERACION          VARCHAR2(100 CHAR)              NOT NULL,
    CLAVE              VARCHAR2(100 CHAR)              NOT NULL,
    HASH_PAYLOAD       VARCHAR2(64 CHAR)               NOT NULL,
    RECURSO_TIPO       VARCHAR2(50 CHAR),
    RECURSO_ID         NUMBER(15),
    RESPUESTA_JSON     CLOB,
    ESTADO_TECNICO     VARCHAR2(20 CHAR)               NOT NULL,
    FECHA_EXPEDICION   TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    FECHA_EXPIRACION   TIMESTAMP(6)                    NOT NULL,
    CREADO_POR         VARCHAR2(100 CHAR)              NOT NULL
);

-- ----------------------------------------------------------------------------
-- 3) Primary key, unique key y checks. Cada CREATE genera un commit
--    implicito.
-- ----------------------------------------------------------------------------
PROMPT [002.3] Creando PK, UK y CHECKs de SOLICITUD_IDEMPOTENTE
ALTER TABLE SOLICITUD_IDEMPOTENTE
    ADD CONSTRAINT PK_SOLICITUD_IDEMPOTENTE PRIMARY KEY (ID_SOLICITUD);

ALTER TABLE SOLICITUD_IDEMPOTENTE
    ADD CONSTRAINT UK_SI_CONSUMIDOR_OPERACION_CLAVE
    UNIQUE (CONSUMIDOR, OPERACION, CLAVE);

ALTER TABLE SOLICITUD_IDEMPOTENTE
    ADD CONSTRAINT CK_SI_ESTADO_TECNICO
    CHECK (ESTADO_TECNICO IN ('INICIADA','COMPLETADA','EXPIRADA','FALLIDA'));

ALTER TABLE SOLICITUD_IDEMPOTENTE
    ADD CONSTRAINT CK_SI_HASH
    CHECK (REGEXP_LIKE(HASH_PAYLOAD, '^[0-9A-Fa-f]{64}$'));

-- ----------------------------------------------------------------------------
-- 4) Indices auxiliares sobre la tabla nueva.
-- ----------------------------------------------------------------------------
PROMPT [002.4] Creando indices auxiliares IDX_SI_EXPEDICION e IDX_SI_EXPIRACION
CREATE INDEX IDX_SI_EXPEDICION ON SOLICITUD_IDEMPOTENTE (FECHA_EXPEDICION);
CREATE INDEX IDX_SI_EXPIRACION ON SOLICITUD_IDEMPOTENTE (FECHA_EXPIRACION);

-- ----------------------------------------------------------------------------
-- 5) Ampliacion de AUDITORIA_ACCESO con las columnas de contexto efectivo.
--    Cada ALTER TABLE add column realiza un commit implicito. Las FKs
--    hacia ROL, UNIDAD_EJECUTORA y USUARIO_ROL_UNIDAD se crean en 008,
--    cuando existan las tablas de combinacion y vigencia de la asignacion.
-- ----------------------------------------------------------------------------
PROMPT [002.5] Anadiendo columnas de contexto efectivo a AUDITORIA_ACCESO
ALTER TABLE AUDITORIA_ACCESO ADD (ID_ROL_EFECTIVO        NUMBER(5));
ALTER TABLE AUDITORIA_ACCESO ADD (ID_UNIDAD_EFECTIVA     NUMBER(10));
ALTER TABLE AUDITORIA_ACCESO ADD (ID_ASIGNACION_EFECTIVA NUMBER(10));

-- ----------------------------------------------------------------------------
-- 6) Indices auxiliares para localizar accesos por rol y unidad efectivos.
--    La columna ID_ASIGNACION_EFECTIVA se documenta sin indice dedicado
--    en este script; se aniadira en 008 cuando exista la combinacion.
-- ----------------------------------------------------------------------------
PROMPT [002.6] Creando indices IDX_AA_ROL_EFECTIVO e IDX_AA_UNIDAD_EFECTIVA
CREATE INDEX IDX_AA_ROL_EFECTIVO    ON AUDITORIA_ACCESO (ID_ROL_EFECTIVO);
CREATE INDEX IDX_AA_UNIDAD_EFECTIVA ON AUDITORIA_ACCESO (ID_UNIDAD_EFECTIVA);

-- ============================================================================
-- Validacion final del script 002
-- ----------------------------------------------------------------------------
-- Verifica que la tabla nueva, su PK, su UK, sus CHECKs, sus indices y
-- las columnas anadidas a AUDITORIA_ACCESO existen con la definicion
-- declarada en el diccionario fisico.
-- ============================================================================
PROMPT [002.7] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    -- Tabla
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME = 'SOLICITUD_IDEMPOTENTE';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20030,
            'Validacion 002: SOLICITUD_IDEMPOTENTE no existe');
    END IF;

    -- PK
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'SOLICITUD_IDEMPOTENTE'
       AND CONSTRAINT_NAME = 'PK_SOLICITUD_IDEMPOTENTE'
       AND CONSTRAINT_TYPE = 'P';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 002: PK_SOLICITUD_IDEMPOTENTE ausente');
    END IF;

    -- UK
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'SOLICITUD_IDEMPOTENTE'
       AND CONSTRAINT_NAME = 'UK_SI_CONSUMIDOR_OPERACION_CLAVE'
       AND CONSTRAINT_TYPE = 'U';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20032,
            'Validacion 002: UK_SI_CONSUMIDOR_OPERACION_CLAVE ausente');
    END IF;

    -- CHECKs
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'SOLICITUD_IDEMPOTENTE'
       AND CONSTRAINT_NAME IN ('CK_SI_ESTADO_TECNICO','CK_SI_HASH')
       AND CONSTRAINT_TYPE = 'C';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20033,
            'Validacion 002: CHECKs de SOLICITUD_IDEMPOTENTE ausentes');
    END IF;

    -- Columnas de la tabla
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'SOLICITUD_IDEMPOTENTE'
       AND COLUMN_NAME IN (
            'ID_SOLICITUD','CONSUMIDOR','OPERACION','CLAVE','HASH_PAYLOAD',
            'RECURSO_TIPO','RECURSO_ID','RESPUESTA_JSON','ESTADO_TECNICO',
            'FECHA_EXPEDICION','FECHA_EXPIRACION','CREADO_POR'
           );
    IF v_total <> 12 THEN
        RAISE_APPLICATION_ERROR(-20034,
            'Validacion 002: columnas de SOLICITUD_IDEMPOTENTE incompletas');
    END IF;

    -- Columnas de AUDITORIA_ACCESO
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'AUDITORIA_ACCESO'
       AND COLUMN_NAME IN (
            'ID_ROL_EFECTIVO','ID_UNIDAD_EFECTIVA','ID_ASIGNACION_EFECTIVA'
           );
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20035,
            'Validacion 002: columnas de contexto efectivo en AUDITORIA_ACCESO incompletas');
    END IF;

    -- Secuencia
    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME = 'SEQ_SOLICITUD_IDEMPOTENTE';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20036,
            'Validacion 002: SEQ_SOLICITUD_IDEMPOTENTE ausente');
    END IF;

    -- Indices auxiliares
    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME IN (
            'IDX_SI_EXPEDICION','IDX_SI_EXPIRACION',
            'IDX_AA_ROL_EFECTIVO','IDX_AA_UNIDAD_EFECTIVA'
           )
       AND STATUS = 'VALID';
    IF v_total <> 4 THEN
        RAISE_APPLICATION_ERROR(-20037,
            'Validacion 002: indices auxiliares del incremento 002 ausentes o invalidos');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 002 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 002_auditoria_idempotencia completada correctamente.
