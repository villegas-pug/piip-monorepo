-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 015 - Planificacion, ciclos
-- quincenales, productos parciales, presentacion del producto final,
-- validacion de resultados y cierre del proyecto (cubre US4 y US5)
-- Archivo   : 015_ciclos_resultados_cierre.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : portafolio
-- Dependencias: 003+003.1+003.2, 009 y 014, ademas de 002, 005, 006, 008+008.1
--               y 001.
-- Alcance   : Crea PLANIFICACION_PROYECTO, CICLO_PROYECTO, CICLO_EVIDENCIA,
--             PRODUCTO_PARCIAL, PRESENTACION_PRODUCTO_FINAL,
--             VALIDACION_RESULTADO y CIERRE_PROYECTO. Los ciclos son
--             quincenales (periodo YYYY-Qn-Sn) y las correcciones crean
--             nueva version con la fila anterior conservada.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: detener cierres/ciclos; conservar versiones
--            cerradas. Las planificaciones y ciclos finalizados no se
--            eliminan.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [015] Validando precondiciones de 001, 002, 003+003.1+003.2, 005, 006, 008+008.1, 009 y 014...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones acumuladas.
-- ----------------------------------------------------------------------------
DECLARE
    v_tablas_precedentes PLS_INTEGER;
    v_secuencias_precedentes PLS_INTEGER;
BEGIN
    -- 23 tablas (001+002+003+005+006+008) + 4 de 014 = 27
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
            'EVALUACION_INICIATIVA','SUBSANACION_INICIATIVA',
            'APLICABILIDAD_INICIATIVA','APLICABILIDAD_CRITERIO'
           );
    IF v_tablas_precedentes <> 27 THEN
        RAISE_APPLICATION_ERROR(
            -20010,
            'Precondicion 015: se esperaban 27 tablas previas y se encontraron '
            || TO_CHAR(v_tablas_precedentes)
        );
    END IF;

    -- 20 secuencias previas + 4 de 014 = 24
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
            'SEQ_EVALUACION_INICIATIVA','SEQ_SUBSANACION_INICIATIVA',
            'SEQ_APLICABILIDAD_INICIATIVA','SEQ_APLICABILIDAD_CRITERIO'
           );
    IF v_secuencias_precedentes <> 24 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Precondicion 015: se esperaban 24 secuencias previas y se encontraron '
            || TO_CHAR(v_secuencias_precedentes)
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de que las tablas/secuencias del propio 015 NO existen.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'PLANIFICACION_PROYECTO','CICLO_PROYECTO','CICLO_EVIDENCIA',
            'PRODUCTO_PARCIAL','PRESENTACION_PRODUCTO_FINAL',
            'VALIDACION_RESULTADO','CIERRE_PROYECTO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 015: tablas de planificacion/ciclos/cierre ya existen; el incremento ya fue aplicado'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_PLANIFICACION_PROYECTO','SEQ_CICLO_PROYECTO','SEQ_CICLO_EVIDENCIA',
            'SEQ_PRODUCTO_PARCIAL','SEQ_PRESENTACION_PRODUCTO_FINAL',
            'SEQ_VALIDACION_RESULTADO','SEQ_CIERRE_PROYECTO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 015: secuencias del incremento ya existen; revise el estado'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de objetos futuros estrictamente sucesores de 015.
--    015 es paralelo a 016; sus sucesores estrictos son 016 y 017.
--    Tambien se validan los diferidos 018-021.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'INCORPORACION_REGISTRO','INCORPORACION_CAMBIO','INCORPORACION_CONFLICTO',
            'REPORTE_INSTITUCIONAL','REPORTE_SNAPSHOT','REPORTE_ARCHIVO','REPORTE_APROBACION',
            'REPORTE_DESTINATARIO','REPORTE_REMISION',
            'PROTOTIPO_PIIP','PROTOTIPO_VALIDACION','PROTOTIPO_HALLAZGO','MEDICION_EXPERIENCIA',
            'MEDICION_MUESTRA','MATRIZ_META_RECORRIDO'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 015: existen objetos futuros 016-017 o 018-021 ya creados'
        );
    END IF;
END;
/

PROMPT [015] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 015
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencias del incremento.
-- ----------------------------------------------------------------------------
PROMPT [015.1] Creando secuencias del incremento
CREATE SEQUENCE SEQ_PLANIFICACION_PROYECTO
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_CICLO_PROYECTO
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_CICLO_EVIDENCIA
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_PRODUCTO_PARCIAL
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_PRESENTACION_PRODUCTO_FINAL
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_VALIDACION_RESULTADO
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_CIERRE_PROYECTO
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Tabla PLANIFICACION_PROYECTO.
-- ----------------------------------------------------------------------------
PROMPT [015.2] Creando tabla PLANIFICACION_PROYECTO
CREATE TABLE PLANIFICACION_PROYECTO (
    ID_PLANIFICACION     NUMBER(12)                      NOT NULL,
    ID_PROYECTO          NUMBER(12)                      NOT NULL,
    ALCANCE              VARCHAR2(2000 CHAR),
    OBJETIVOS            VARCHAR2(2000 CHAR),
    ENTREGABLES          CLOB,
    PERIODOS             CLOB,
    VERSION              NUMBER(3)                       NOT NULL,
    ID_VERSION_ANTERIOR  NUMBER(12),
    CERRADA              CHAR(1 CHAR) DEFAULT 'N'        NOT NULL,
    CREADO_POR           VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION       TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [015.3] PK, UK, FKs y CHECKs de PLANIFICACION_PROYECTO
ALTER TABLE PLANIFICACION_PROYECTO
    ADD CONSTRAINT PK_PLANIFICACION_PROYECTO PRIMARY KEY (ID_PLANIFICACION);

ALTER TABLE PLANIFICACION_PROYECTO
    ADD CONSTRAINT UK_PP_PROY_VERSION UNIQUE (ID_PROYECTO, VERSION);

ALTER TABLE PLANIFICACION_PROYECTO
    ADD CONSTRAINT FK_PP_PROYECTO
    FOREIGN KEY (ID_PROYECTO) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE PLANIFICACION_PROYECTO
    ADD CONSTRAINT FK_PP_VERSION_ANTERIOR
    FOREIGN KEY (ID_VERSION_ANTERIOR) REFERENCES PLANIFICACION_PROYECTO (ID_PLANIFICACION);

ALTER TABLE PLANIFICACION_PROYECTO
    ADD CONSTRAINT CK_PP_VERSION_MIN
    CHECK (VERSION >= 1);

ALTER TABLE PLANIFICACION_PROYECTO
    ADD CONSTRAINT CK_PP_CERRADA
    CHECK (CERRADA IN ('S','N'));

-- ----------------------------------------------------------------------------
-- 3) Tabla CICLO_PROYECTO.
-- ----------------------------------------------------------------------------
PROMPT [015.4] Creando tabla CICLO_PROYECTO
CREATE TABLE CICLO_PROYECTO (
    ID_CICLO             NUMBER(12)                      NOT NULL,
    ID_PROYECTO          NUMBER(12)                      NOT NULL,
    PERIODO              VARCHAR2(20 CHAR)               NOT NULL,
    NUMERO_VERSION       NUMBER(3)                       NOT NULL,
    ID_VERSION_ANTERIOR  NUMBER(12),
    OBJETIVOS            VARCHAR2(2000 CHAR),
    ACTIVIDADES          VARCHAR2(2000 CHAR),
    AVANCE               NUMBER(5,2),
    DIFICULTADES         VARCHAR2(2000 CHAR),
    PROXIMAS_ACCIONES    VARCHAR2(2000 CHAR),
    CERRADO              CHAR(1 CHAR) DEFAULT 'N'        NOT NULL,
    CREADO_POR           VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION       TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    FECHA_CIERRE         TIMESTAMP(6)
);

PROMPT [015.5] PK, UK, FKs y CHECKs de CICLO_PROYECTO
ALTER TABLE CICLO_PROYECTO
    ADD CONSTRAINT PK_CICLO_PROYECTO PRIMARY KEY (ID_CICLO);

ALTER TABLE CICLO_PROYECTO
    ADD CONSTRAINT UK_CP_PROY_PERIODO_VERSION
    UNIQUE (ID_PROYECTO, PERIODO, NUMERO_VERSION);

ALTER TABLE CICLO_PROYECTO
    ADD CONSTRAINT FK_CP_PROYECTO
    FOREIGN KEY (ID_PROYECTO) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE CICLO_PROYECTO
    ADD CONSTRAINT FK_CP_VERSION_ANTERIOR
    FOREIGN KEY (ID_VERSION_ANTERIOR) REFERENCES CICLO_PROYECTO (ID_CICLO);

ALTER TABLE CICLO_PROYECTO
    ADD CONSTRAINT CK_CP_PERIODO
    CHECK (REGEXP_LIKE(PERIODO, '^[0-9]{4}-Q[1-4]-S[1-2]$'));

ALTER TABLE CICLO_PROYECTO
    ADD CONSTRAINT CK_CP_VERSION
    CHECK (NUMERO_VERSION >= 1);

ALTER TABLE CICLO_PROYECTO
    ADD CONSTRAINT CK_CP_AVANCE
    CHECK (AVANCE IS NULL OR (AVANCE BETWEEN 0 AND 100));

ALTER TABLE CICLO_PROYECTO
    ADD CONSTRAINT CK_CP_CERRADO
    CHECK (CERRADO IN ('S','N'));

-- ----------------------------------------------------------------------------
-- 4) Tabla CICLO_EVIDENCIA.
-- ----------------------------------------------------------------------------
PROMPT [015.6] Creando tabla CICLO_EVIDENCIA
CREATE TABLE CICLO_EVIDENCIA (
    ID_EVIDENCIA  NUMBER(12)                      NOT NULL,
    ID_CICLO      NUMBER(12)                      NOT NULL,
    ID_DOCUMENTO  NUMBER(12)                      NOT NULL,
    CREADO_POR    VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [015.7] PK, UK y FKs de CICLO_EVIDENCIA
ALTER TABLE CICLO_EVIDENCIA
    ADD CONSTRAINT PK_CICLO_EVIDENCIA PRIMARY KEY (ID_EVIDENCIA);

ALTER TABLE CICLO_EVIDENCIA
    ADD CONSTRAINT UK_CE_CICLO_DOC UNIQUE (ID_CICLO, ID_DOCUMENTO);

ALTER TABLE CICLO_EVIDENCIA
    ADD CONSTRAINT FK_CE_CICLO
    FOREIGN KEY (ID_CICLO) REFERENCES CICLO_PROYECTO (ID_CICLO);

ALTER TABLE CICLO_EVIDENCIA
    ADD CONSTRAINT FK_CE_DOCUMENTO
    FOREIGN KEY (ID_DOCUMENTO) REFERENCES DOCUMENTO (ID_DOCUMENTO);

-- ----------------------------------------------------------------------------
-- 5) Tabla PRODUCTO_PARCIAL.
-- ----------------------------------------------------------------------------
PROMPT [015.8] Creando tabla PRODUCTO_PARCIAL
CREATE TABLE PRODUCTO_PARCIAL (
    ID_PRODUCTO          NUMBER(12)                      NOT NULL,
    ID_CICLO             NUMBER(12)                      NOT NULL,
    DESCRIPCION          VARCHAR2(2000 CHAR)              NOT NULL,
    RESULTADO            CLOB,
    FECHA                DATE                            NOT NULL,
    ID_RESPONSABLE       NUMBER(10)                      NOT NULL,
    VERSION              NUMBER(3)                       NOT NULL,
    ID_VERSION_ANTERIOR  NUMBER(12),
    CREADO_POR           VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION       TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [015.9] PK, UK y FKs de PRODUCTO_PARCIAL
ALTER TABLE PRODUCTO_PARCIAL
    ADD CONSTRAINT PK_PRODUCTO_PARCIAL PRIMARY KEY (ID_PRODUCTO);

ALTER TABLE PRODUCTO_PARCIAL
    ADD CONSTRAINT UK_PROD_CICLO_VERSION UNIQUE (ID_CICLO, VERSION);

ALTER TABLE PRODUCTO_PARCIAL
    ADD CONSTRAINT FK_PROD_CICLO
    FOREIGN KEY (ID_CICLO) REFERENCES CICLO_PROYECTO (ID_CICLO);

ALTER TABLE PRODUCTO_PARCIAL
    ADD CONSTRAINT FK_PROD_RESPONSABLE
    FOREIGN KEY (ID_RESPONSABLE) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE PRODUCTO_PARCIAL
    ADD CONSTRAINT FK_PROD_VERSION_ANTERIOR
    FOREIGN KEY (ID_VERSION_ANTERIOR) REFERENCES PRODUCTO_PARCIAL (ID_PRODUCTO);

ALTER TABLE PRODUCTO_PARCIAL
    ADD CONSTRAINT CK_PROD_VERSION_MIN
    CHECK (VERSION >= 1);

-- ----------------------------------------------------------------------------
-- 6) Tabla PRESENTACION_PRODUCTO_FINAL.
-- ----------------------------------------------------------------------------
PROMPT [015.10] Creando tabla PRESENTACION_PRODUCTO_FINAL
CREATE TABLE PRESENTACION_PRODUCTO_FINAL (
    ID_PRESENTACION      NUMBER(12)                      NOT NULL,
    ID_PROYECTO          NUMBER(12)                      NOT NULL,
    VERSION              NUMBER(3)                       NOT NULL,
    ID_VERSION_ANTERIOR  NUMBER(12),
    DESCRIPCION          VARCHAR2(2000 CHAR)              NOT NULL,
    ID_RESPONSABLE       NUMBER(10)                      NOT NULL,
    ID_DOCUMENTO_SUSTENTA NUMBER(12)                     NOT NULL,
    FECHA_PRESENTACION   TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [015.11] PK, UK y FKs de PRESENTACION_PRODUCTO_FINAL
ALTER TABLE PRESENTACION_PRODUCTO_FINAL
    ADD CONSTRAINT PK_PRESENTACION_PRODUCTO_FINAL PRIMARY KEY (ID_PRESENTACION);

ALTER TABLE PRESENTACION_PRODUCTO_FINAL
    ADD CONSTRAINT UK_PPF_PROY_VERSION UNIQUE (ID_PROYECTO, VERSION);

ALTER TABLE PRESENTACION_PRODUCTO_FINAL
    ADD CONSTRAINT FK_PPF_PROYECTO
    FOREIGN KEY (ID_PROYECTO) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE PRESENTACION_PRODUCTO_FINAL
    ADD CONSTRAINT FK_PPF_RESPONSABLE
    FOREIGN KEY (ID_RESPONSABLE) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE PRESENTACION_PRODUCTO_FINAL
    ADD CONSTRAINT FK_PPF_DOCUMENTO_SUSTENTA
    FOREIGN KEY (ID_DOCUMENTO_SUSTENTA) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE PRESENTACION_PRODUCTO_FINAL
    ADD CONSTRAINT FK_PPF_VERSION_ANTERIOR
    FOREIGN KEY (ID_VERSION_ANTERIOR) REFERENCES PRESENTACION_PRODUCTO_FINAL (ID_PRESENTACION);

ALTER TABLE PRESENTACION_PRODUCTO_FINAL
    ADD CONSTRAINT CK_PPF_VERSION_MIN
    CHECK (VERSION >= 1);

-- ----------------------------------------------------------------------------
-- 7) Tabla VALIDACION_RESULTADO.
-- ----------------------------------------------------------------------------
PROMPT [015.12] Creando tabla VALIDACION_RESULTADO
CREATE TABLE VALIDACION_RESULTADO (
    ID_VALIDACION        NUMBER(12)                      NOT NULL,
    ID_PROYECTO          NUMBER(12)                      NOT NULL,
    ID_RESPONSABLE       NUMBER(10)                      NOT NULL,
    ID_EVALUADOR         NUMBER(10)                      NOT NULL,
    RESULTADOS_CLAVE     CLOB,
    VALIDADO_EN          TIMESTAMP(6),
    CREADO_POR           VARCHAR2(100 CHAR)               NOT NULL,
    FECHA_CREACION       TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [015.13] PK, UK, FKs y CHECKs de VALIDACION_RESULTADO
ALTER TABLE VALIDACION_RESULTADO
    ADD CONSTRAINT PK_VALIDACION_RESULTADO PRIMARY KEY (ID_VALIDACION);

ALTER TABLE VALIDACION_RESULTADO
    ADD CONSTRAINT UK_VR_PROYECTO UNIQUE (ID_PROYECTO);

ALTER TABLE VALIDACION_RESULTADO
    ADD CONSTRAINT FK_VR_PROYECTO
    FOREIGN KEY (ID_PROYECTO) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE VALIDACION_RESULTADO
    ADD CONSTRAINT FK_VR_RESPONSABLE
    FOREIGN KEY (ID_RESPONSABLE) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE VALIDACION_RESULTADO
    ADD CONSTRAINT FK_VR_EVALUADOR
    FOREIGN KEY (ID_EVALUADOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE VALIDACION_RESULTADO
    ADD CONSTRAINT CK_VR_ACTORES_DISTINTOS
    CHECK (ID_RESPONSABLE <> ID_EVALUADOR);

-- ----------------------------------------------------------------------------
-- 8) Tabla CIERRE_PROYECTO.
-- ----------------------------------------------------------------------------
PROMPT [015.14] Creando tabla CIERRE_PROYECTO
CREATE TABLE CIERRE_PROYECTO (
    ID_CIERRE            NUMBER(12)                      NOT NULL,
    ID_PROYECTO          NUMBER(12)                      NOT NULL,
    INFORME_FINAL        CLOB,
    RESULTADOS           CLOB,
    APRENDIZAJES         CLOB,
    CONCLUSION           VARCHAR2(2000 CHAR),
    OBSERVACION          VARCHAR2(2000 CHAR),
    ID_EVALUADOR         NUMBER(10)                      NOT NULL,
    FECHA_CIERRE         TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [015.15] PK, UK, FKs y CHECKs de CIERRE_PROYECTO
ALTER TABLE CIERRE_PROYECTO
    ADD CONSTRAINT PK_CIERRE_PROYECTO PRIMARY KEY (ID_CIERRE);

ALTER TABLE CIERRE_PROYECTO
    ADD CONSTRAINT UK_CIERRE_PROY UNIQUE (ID_PROYECTO);

ALTER TABLE CIERRE_PROYECTO
    ADD CONSTRAINT FK_CIERRE_PROYECTO
    FOREIGN KEY (ID_PROYECTO) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE CIERRE_PROYECTO
    ADD CONSTRAINT FK_CIERRE_EVALUADOR
    FOREIGN KEY (ID_EVALUADOR) REFERENCES USUARIO (ID_USUARIO);

-- ============================================================================
-- Validacion final del script 015
-- ============================================================================
PROMPT [015.16] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'PLANIFICACION_PROYECTO','CICLO_PROYECTO','CICLO_EVIDENCIA',
            'PRODUCTO_PARCIAL','PRESENTACION_PRODUCTO_FINAL',
            'VALIDACION_RESULTADO','CIERRE_PROYECTO'
           );
    IF v_total <> 7 THEN
        RAISE_APPLICATION_ERROR(-20030,
            'Validacion 015: tablas de planificacion/ciclos/cierre ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_PLANIFICACION_PROYECTO','SEQ_CICLO_PROYECTO','SEQ_CICLO_EVIDENCIA',
            'SEQ_PRODUCTO_PARCIAL','SEQ_PRESENTACION_PRODUCTO_FINAL',
            'SEQ_VALIDACION_RESULTADO','SEQ_CIERRE_PROYECTO'
           )
       AND INCREMENT_BY = 1 AND CACHE_SIZE = 0 AND CYCLE_FLAG = 'N';
    IF v_total <> 7 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 015: secuencias del incremento ausentes o incompatibles');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'CICLO_PROYECTO'
       AND CONSTRAINT_NAME = 'CK_CP_AVANCE'
       AND CONSTRAINT_TYPE = 'C' AND STATUS = 'ENABLED';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20032,
            'Validacion 015: CHECK CK_CP_AVANCE ausente');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 015 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 015_ciclos_resultados_cierre completada correctamente.
