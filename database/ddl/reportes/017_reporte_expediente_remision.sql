-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 017 - Ciclo de reportes
-- institucionales (snapshot, archivos, aprobacion, destinatarios y remision)
-- Archivo   : 017_reporte_expediente_remision.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : reportes
-- Dependencias: 002, 003+003.1+003.2, 009 y 015, ademas de 005, 006, 008+008.1,
--               010, 011, 012, 013, 014, 016 y 001.
-- Alcance   : Crea REPORTE_INSTITUCIONAL, REPORTE_SNAPSHOT, REPORTE_ARCHIVO,
--             REPORTE_APROBACION, REPORTE_DESTINATARIO y REPORTE_REMISION.
--             REPORTE_SNAPSHOT.PAYLOAD_JSON es CLOB con version de esquema
--             y SHA-256. La FK de REPORTE_INSTITUCIONAL.ID_SNAPSHOT se
--             crea tras REPORTE_SNAPSHOT para evitar el commit implicito
--             cruzado. MV_PORTAFOLIO_RESUMEN no se crea.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: detener generacion/remision; conservar
--            expedientes. Los reportes generados no se eliminan.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [017] Validando precondiciones de 001, 002, 003+003.1+003.2, 005, 006, 008+008.1, 009 y 015...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones acumuladas.
-- ----------------------------------------------------------------------------
DECLARE
    v_tablas_precedentes PLS_INTEGER;
    v_secuencias_precedentes PLS_INTEGER;
BEGIN
    -- 23 base + 7 (015) = 30
    SELECT COUNT(*) INTO v_tablas_precedentes
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'UNIDAD_EJECUTORA','USUARIO','ROL','USUARIO_ROL_UNIDAD',
            'PROYECTO','PROYECTO_UNIDAD_ORGANICA','TRANSICION_PERMITIDA',
            'TIPO_DOCUMENTO','DOCUMENTO','TRANSICION_ESTADO',
            'SECUENCIA_CODIGO','AUDITORIA_ACCESO','AUDITORIA_EVENTO',
            'SOLICITUD_IDEMPOTENTE',
            'EXPEDIENTE_INSTITUCIONAL','DOCUMENTO_SERIE',
            'CAT_OBJETIVO_PEI_VERSION','CAT_OBJETIVO_PEI',
            'CAT_ACTIVIDAD_POI_VERSION','CAT_ACTIVIDAD_POI',
            'USUARIO_ROL_UNIDAD_EVENTO','SUPLENCIA_FUNCIONAL',
            'OPERACION_APROVISIONAMIENTO',
            'PLANIFICACION_PROYECTO','CICLO_PROYECTO','CICLO_EVIDENCIA',
            'PRODUCTO_PARCIAL','PRESENTACION_PRODUCTO_FINAL',
            'VALIDACION_RESULTADO','CIERRE_PROYECTO'
           );
    IF v_tablas_precedentes <> 30 THEN
        RAISE_APPLICATION_ERROR(
            -20010,
            'Precondicion 017: se esperaban 30 tablas previas y se encontraron '
            || TO_CHAR(v_tablas_precedentes)
        );
    END IF;

    -- 20 base + 7 (015) = 27
    SELECT COUNT(*) INTO v_secuencias_precedentes
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_UNIDAD_EJECUTORA','SEQ_USUARIO','SEQ_USUARIO_ROL_UNIDAD',
            'SEQ_PROYECTO','SEQ_PROYECTO_UO','SEQ_DOCUMENTO',
            'SEQ_TRANSICION_ESTADO','SEQ_SECUENCIA_CODIGO',
            'SEQ_AUDITORIA_ACCESO','SEQ_AUDITORIA_EVENTO',
            'SEQ_SOLICITUD_IDEMPOTENTE',
            'SEQ_EXPEDIENTE_INSTITUCIONAL','SEQ_DOCUMENTO_SERIE',
            'SEQ_OBJETIVO_PEI_VERSION','SEQ_OBJETIVO_PEI',
            'SEQ_ACTIVIDAD_POI_VERSION','SEQ_ACTIVIDAD_POI',
            'SEQ_URU_EVENTO','SEQ_SUPLENCIA_FUNCIONAL',
            'SEQ_OPERACION_APROVISIONAMIENTO',
            'SEQ_PLANIFICACION_PROYECTO','SEQ_CICLO_PROYECTO',
            'SEQ_CICLO_EVIDENCIA','SEQ_PRODUCTO_PARCIAL',
            'SEQ_PRESENTACION_PRODUCTO_FINAL','SEQ_VALIDACION_RESULTADO',
            'SEQ_CIERRE_PROYECTO'
           );
    IF v_secuencias_precedentes <> 27 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Precondicion 017: se esperaban 27 secuencias previas y se encontraron '
            || TO_CHAR(v_secuencias_precedentes)
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de que las tablas/secuencias del propio 017 NO existen.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'REPORTE_INSTITUCIONAL','REPORTE_SNAPSHOT','REPORTE_ARCHIVO',
            'REPORTE_APROBACION','REPORTE_DESTINATARIO','REPORTE_REMISION'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 017: tablas de reportes ya existen; el incremento ya fue aplicado'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_REPORTE_INSTITUCIONAL','SEQ_REPORTE_SNAPSHOT',
            'SEQ_REPORTE_ARCHIVO','SEQ_REPORTE_APROBACION',
            'SEQ_REPORTE_DESTINATARIO','SEQ_REPORTE_REMISION'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 017: secuencias del incremento ya existen; revise el estado'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de objetos futuros de 017.
--    017 es el ultimo incremento activo de la Fase 1; sus unicos
--    hipoteticos sucesores son los diferidos 018-021 (US9, semillas y
--    corte), que la enmienda 5.0.0 deja fuera del alcance fisico. Por
--    consistencia con el resto del diseno, se conservan en la lista
--    unicamente los objetos de esos diferidos por si llegasen a crearse
--    fuera de orden.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'PROTOTIPO_PIIP','PROTOTIPO_VALIDACION','PROTOTIPO_HALLAZGO','MEDICION_EXPERIENCIA',
            'MEDICION_MUESTRA','MATRIZ_META_RECORRIDO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 017: no hay objetos futuros que validar (017 es el ultimo incremento activo)'
        );
    END IF;
END;
/

PROMPT [017] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 017
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencias del incremento.
-- ----------------------------------------------------------------------------
PROMPT [017.1] Creando secuencias del incremento
CREATE SEQUENCE SEQ_REPORTE_INSTITUCIONAL
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_REPORTE_SNAPSHOT
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_REPORTE_ARCHIVO
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_REPORTE_APROBACION
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_REPORTE_DESTINATARIO
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_REPORTE_REMISION
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Tabla REPORTE_INSTITUCIONAL (sin FK a snapshot por ahora).
-- ----------------------------------------------------------------------------
PROMPT [017.2] Creando tabla REPORTE_INSTITUCIONAL
CREATE TABLE REPORTE_INSTITUCIONAL (
    ID_REPORTE           NUMBER(12)                      NOT NULL,
    TIPO                 VARCHAR2(30 CHAR)               NOT NULL,
    ANIO                 NUMBER(4)                       NOT NULL,
    SEMESTRE             NUMBER(1),
    PERIODO              VARCHAR2(30 CHAR)               NOT NULL,
    FECHA_CORTE          DATE                            NOT NULL,
    PARAMETROS           CLOB,
    ID_SNAPSHOT          NUMBER(12),
    VERSION_DATOS        NUMBER(5),
    CLASIFICACION        VARCHAR2(20 CHAR)               NOT NULL,
    ID_GENERADOR         NUMBER(10)                      NOT NULL,
    FECHA_GENERACION     TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    ESTADO_TECNICO       VARCHAR2(20 CHAR)               NOT NULL
);

PROMPT [017.3] PK y CHECKs de REPORTE_INSTITUCIONAL
ALTER TABLE REPORTE_INSTITUCIONAL
    ADD CONSTRAINT PK_REPORTE_INSTITUCIONAL PRIMARY KEY (ID_REPORTE);

ALTER TABLE REPORTE_INSTITUCIONAL
    ADD CONSTRAINT FK_RE_GENERADOR
    FOREIGN KEY (ID_GENERADOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE REPORTE_INSTITUCIONAL
    ADD CONSTRAINT CK_RE_TIPO
    CHECK (TIPO IN ('SEMESTRAL','EXTRAORDINARIO'));

ALTER TABLE REPORTE_INSTITUCIONAL
    ADD CONSTRAINT CK_RE_CLASIFICACION
    CHECK (CLASIFICACION IN ('INTERNO','RESTRINGIDO'));

ALTER TABLE REPORTE_INSTITUCIONAL
    ADD CONSTRAINT CK_RE_ESTADO_TECNICO
    CHECK (ESTADO_TECNICO IN ('INICIADA','GENERADA','APROBADA','FALLIDA'));

ALTER TABLE REPORTE_INSTITUCIONAL
    ADD CONSTRAINT CK_RE_SEMESTRE
    CHECK ((TIPO = 'EXTRAORDINARIO' AND SEMESTRE IS NULL)
        OR (TIPO = 'SEMESTRAL' AND SEMESTRE IN (1,2)));

ALTER TABLE REPORTE_INSTITUCIONAL
    ADD CONSTRAINT CK_RE_CORTE
    CHECK (
        (TIPO = 'EXTRAORDINARIO')
     OR (TIPO = 'SEMESTRAL' AND SEMESTRE = 1
         AND FECHA_CORTE = TO_DATE(TO_CHAR(ANIO) || '-06-30','YYYY-MM-DD'))
     OR (TIPO = 'SEMESTRAL' AND SEMESTRE = 2
         AND FECHA_CORTE = TO_DATE(TO_CHAR(ANIO) || '-12-31','YYYY-MM-DD'))
    );

-- ----------------------------------------------------------------------------
-- 3) Tabla REPORTE_SNAPSHOT.
-- ----------------------------------------------------------------------------
PROMPT [017.4] Creando tabla REPORTE_SNAPSHOT
CREATE TABLE REPORTE_SNAPSHOT (
    ID_SNAPSHOT       NUMBER(12)                      NOT NULL,
    PAYLOAD_JSON      CLOB                            NOT NULL,
    VERSION_ESQUEMA   NUMBER(5),
    HASH_SHA256       VARCHAR2(64 CHAR)               NOT NULL,
    FECHA_CORTE       DATE                            NOT NULL,
    PARAMETROS        CLOB,
    CLASIFICACION     VARCHAR2(20 CHAR),
    CREADO_POR        VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION    TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [017.5] PK, UK y CHECKs de REPORTE_SNAPSHOT
ALTER TABLE REPORTE_SNAPSHOT
    ADD CONSTRAINT PK_REPORTE_SNAPSHOT PRIMARY KEY (ID_SNAPSHOT);

ALTER TABLE REPORTE_SNAPSHOT
    ADD CONSTRAINT UK_RS_HASH UNIQUE (HASH_SHA256);

ALTER TABLE REPORTE_SNAPSHOT
    ADD CONSTRAINT CK_RS_PAYLOAD_JSON
    CHECK (PAYLOAD_JSON IS JSON);

ALTER TABLE REPORTE_SNAPSHOT
    ADD CONSTRAINT CK_RS_HASH
    CHECK (REGEXP_LIKE(HASH_SHA256, '^[0-9A-Fa-f]{64}$'));

ALTER TABLE REPORTE_SNAPSHOT
    ADD CONSTRAINT CK_RS_CLASIFICACION
    CHECK (CLASIFICACION IS NULL
           OR CLASIFICACION IN ('INTERNO','RESTRINGIDO'));

-- ----------------------------------------------------------------------------
-- 4) FK REPORTE_INSTITUCIONAL.ID_SNAPSHOT -> REPORTE_SNAPSHOT, creada
--    despues de REPORTE_SNAPSHOT para evitar commit implicito cruzado.
-- ----------------------------------------------------------------------------
PROMPT [017.6] FK de REPORTE_INSTITUCIONAL.ID_SNAPSHOT
ALTER TABLE REPORTE_INSTITUCIONAL
    ADD CONSTRAINT FK_RE_SNAPSHOT
    FOREIGN KEY (ID_SNAPSHOT) REFERENCES REPORTE_SNAPSHOT (ID_SNAPSHOT);

-- ----------------------------------------------------------------------------
-- 5) Tabla REPORTE_ARCHIVO.
-- ----------------------------------------------------------------------------
PROMPT [017.7] Creando tabla REPORTE_ARCHIVO
CREATE TABLE REPORTE_ARCHIVO (
    ID_ARCHIVO              NUMBER(12)                      NOT NULL,
    ID_REPORTE              NUMBER(12)                      NOT NULL,
    FORMATO                 VARCHAR2(10 CHAR)               NOT NULL,
    VERSION                 NUMBER(5)                       NOT NULL,
    HASH_SHA256             VARCHAR2(64 CHAR)               NOT NULL,
    ID_DOCUMENTO_VERSION    NUMBER(12)                      NOT NULL,
    CREADO_POR              VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION          TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [017.8] PK, UK, FKs y CHECKs de REPORTE_ARCHIVO
ALTER TABLE REPORTE_ARCHIVO
    ADD CONSTRAINT PK_REPORTE_ARCHIVO PRIMARY KEY (ID_ARCHIVO);

ALTER TABLE REPORTE_ARCHIVO
    ADD CONSTRAINT UK_RA_REPORTE_FORMATO_VERSION
    UNIQUE (ID_REPORTE, FORMATO, VERSION);

ALTER TABLE REPORTE_ARCHIVO
    ADD CONSTRAINT FK_RA_REPORTE
    FOREIGN KEY (ID_REPORTE) REFERENCES REPORTE_INSTITUCIONAL (ID_REPORTE);

ALTER TABLE REPORTE_ARCHIVO
    ADD CONSTRAINT FK_RA_DOCUMENTO_VERSION
    FOREIGN KEY (ID_DOCUMENTO_VERSION) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE REPORTE_ARCHIVO
    ADD CONSTRAINT CK_RA_FORMATO
    CHECK (FORMATO IN ('PDF','XLSX'));

ALTER TABLE REPORTE_ARCHIVO
    ADD CONSTRAINT CK_RA_HASH
    CHECK (REGEXP_LIKE(HASH_SHA256, '^[0-9A-Fa-f]{64}$'));

-- ----------------------------------------------------------------------------
-- 6) Tabla REPORTE_APROBACION.
-- ----------------------------------------------------------------------------
PROMPT [017.9] Creando tabla REPORTE_APROBACION
CREATE TABLE REPORTE_APROBACION (
    ID_APROBACION            NUMBER(12)                      NOT NULL,
    ID_REPORTE               NUMBER(12)                      NOT NULL,
    ID_VERSION               NUMBER(5)                       NOT NULL,
    ID_OFICINA               NUMBER(10)                      NOT NULL,
    ID_APROBADOR             NUMBER(10)                      NOT NULL,
    ID_DOCUMENTO_APROBACION  NUMBER(12)                      NOT NULL,
    FECHA_APROBACION         TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [017.10] PK, UK y FKs de REPORTE_APROBACION
ALTER TABLE REPORTE_APROBACION
    ADD CONSTRAINT PK_REPORTE_APROBACION PRIMARY KEY (ID_APROBACION);

ALTER TABLE REPORTE_APROBACION
    ADD CONSTRAINT UK_RAP_REPORTE_VERSION
    UNIQUE (ID_REPORTE, ID_VERSION);

ALTER TABLE REPORTE_APROBACION
    ADD CONSTRAINT FK_RAP_REPORTE
    FOREIGN KEY (ID_REPORTE) REFERENCES REPORTE_INSTITUCIONAL (ID_REPORTE);

ALTER TABLE REPORTE_APROBACION
    ADD CONSTRAINT FK_RAP_OFICINA
    FOREIGN KEY (ID_OFICINA) REFERENCES UNIDAD_EJECUTORA (ID_UNIDAD);

ALTER TABLE REPORTE_APROBACION
    ADD CONSTRAINT FK_RAP_APROBADOR
    FOREIGN KEY (ID_APROBADOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE REPORTE_APROBACION
    ADD CONSTRAINT FK_RAP_DOCUMENTO
    FOREIGN KEY (ID_DOCUMENTO_APROBACION) REFERENCES DOCUMENTO (ID_DOCUMENTO);

-- ----------------------------------------------------------------------------
-- 7) Tabla REPORTE_DESTINATARIO.
-- ----------------------------------------------------------------------------
PROMPT [017.11] Creando tabla REPORTE_DESTINATARIO
CREATE TABLE REPORTE_DESTINATARIO (
    ID_DESTINATARIO     NUMBER(12)                      NOT NULL,
    ID_APROBACION       NUMBER(12)                      NOT NULL,
    TIPO_DESTINATARIO   VARCHAR2(30 CHAR)               NOT NULL,
    ID_ENTIDAD          NUMBER(10)                      NOT NULL,
    NOMBRE              VARCHAR2(200 CHAR)               NOT NULL
);

PROMPT [017.12] PK, UK y FKs de REPORTE_DESTINATARIO
ALTER TABLE REPORTE_DESTINATARIO
    ADD CONSTRAINT PK_REPORTE_DESTINATARIO PRIMARY KEY (ID_DESTINATARIO);

ALTER TABLE REPORTE_DESTINATARIO
    ADD CONSTRAINT UK_RD_APROBACION_TIPO_ENTIDAD
    UNIQUE (ID_APROBACION, TIPO_DESTINATARIO, ID_ENTIDAD);

ALTER TABLE REPORTE_DESTINATARIO
    ADD CONSTRAINT FK_RD_APROBACION
    FOREIGN KEY (ID_APROBACION) REFERENCES REPORTE_APROBACION (ID_APROBACION);

ALTER TABLE REPORTE_DESTINATARIO
    ADD CONSTRAINT CK_RD_TIPO_DESTINATARIO
    CHECK (TIPO_DESTINATARIO IN ('AUTORIDAD_MIDAGRI','OFICINA_MODERNIZACION','PCM_SGP'));

-- ----------------------------------------------------------------------------
-- 8) Tabla REPORTE_REMISION.
-- ----------------------------------------------------------------------------
PROMPT [017.13] Creando tabla REPORTE_REMISION
CREATE TABLE REPORTE_REMISION (
    ID_REMISION      NUMBER(12)                      NOT NULL,
    ID_REPORTE       NUMBER(12)                      NOT NULL,
    ID_DESTINATARIO  NUMBER(12)                      NOT NULL,
    FECHA_REMISION   TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    RESULTADO        VARCHAR2(20 CHAR)               NOT NULL,
    MOTIVO           VARCHAR2(2000 CHAR)
);

PROMPT [017.14] PK, UK, FKs y CHECKs de REPORTE_REMISION
ALTER TABLE REPORTE_REMISION
    ADD CONSTRAINT PK_REPORTE_REMISION PRIMARY KEY (ID_REMISION);

ALTER TABLE REPORTE_REMISION
    ADD CONSTRAINT UK_RREM_REPORTE_DESTINATARIO_FECHA
    UNIQUE (ID_REPORTE, ID_DESTINATARIO, FECHA_REMISION);

ALTER TABLE REPORTE_REMISION
    ADD CONSTRAINT FK_RREM_REPORTE
    FOREIGN KEY (ID_REPORTE) REFERENCES REPORTE_INSTITUCIONAL (ID_REPORTE);

ALTER TABLE REPORTE_REMISION
    ADD CONSTRAINT FK_RREM_DESTINATARIO
    FOREIGN KEY (ID_DESTINATARIO) REFERENCES REPORTE_DESTINATARIO (ID_DESTINATARIO);

ALTER TABLE REPORTE_REMISION
    ADD CONSTRAINT CK_RREM_RESULTADO
    CHECK (RESULTADO IN ('EXITOSA','FALLIDA','PENDIENTE'));

-- ============================================================================
-- Validacion final del script 017
-- ============================================================================
PROMPT [017.15] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'REPORTE_INSTITUCIONAL','REPORTE_SNAPSHOT','REPORTE_ARCHIVO',
            'REPORTE_APROBACION','REPORTE_DESTINATARIO','REPORTE_REMISION'
           );
    IF v_total <> 6 THEN
        RAISE_APPLICATION_ERROR(-20030,
            'Validacion 017: tablas de reportes ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_REPORTE_INSTITUCIONAL','SEQ_REPORTE_SNAPSHOT',
            'SEQ_REPORTE_ARCHIVO','SEQ_REPORTE_APROBACION',
            'SEQ_REPORTE_DESTINATARIO','SEQ_REPORTE_REMISION'
           )
       AND INCREMENT_BY = 1 AND CACHE_SIZE = 0 AND CYCLE_FLAG = 'N';
    IF v_total <> 6 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 017: secuencias del incremento ausentes o incompatibles');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'REPORTE_INSTITUCIONAL'
       AND CONSTRAINT_NAME = 'FK_RE_SNAPSHOT'
       AND CONSTRAINT_TYPE = 'R' AND STATUS = 'ENABLED';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20032,
            'Validacion 017: FK_RE_SNAPSHOT ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'REPORTE_SNAPSHOT'
       AND CONSTRAINT_NAME IN ('CK_RS_PAYLOAD_JSON','CK_RS_HASH')
       AND CONSTRAINT_TYPE = 'C' AND STATUS = 'ENABLED';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20033,
            'Validacion 017: CHECKs JSON/SHA de snapshot ausentes');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 017 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 017_reporte_expediente_remision completada correctamente.
