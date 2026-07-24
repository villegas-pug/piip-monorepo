-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 025 - Historial de versiones de ciclos
-- y evidencias de presentaciones de producto final (gate T072/T073)
-- Archivo       : 025_ciclo_presentacion_evidencia_version.sql
-- Esquema       : KALLPA_PIIP
-- Modulo        : portafolio
-- Proposito     : crea el historial append-only CICLO_PROYECTO_VERSION y la
--                 relacion append-only PRESENTACION_PRODUCTO_FINAL_EVIDENCIA.
-- Dependencias  : 003 VIGENTE (DOCUMENTO y PK_DOCUMENTO) y 015 VIGENTE
--                 (CICLO_PROYECTO, PRESENTACION_PRODUCTO_FINAL y sus PK).
-- Precondiciones: se validan antes del primer DDL. La huella fisica valida la
--                 vigencia de las dependencias; el DBA debe confirmar el orden
--                 del CHANGELOG antes de ejecutar.
-- Ejecucion     : SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--                 KALLPA_PIIP. EJECUCION UNICA y FAIL-FAST.
-- Transaccion   : Oracle confirma implicitamente la transaccion actual antes y
--                 despues de cada CREATE/ALTER/DROP DDL. Por ello, ROLLBACK de
--                 WHENEVER SQLERROR solo revierte DML no confirmado y NO
--                 revierte DDL previo del mismo archivo.
-- Errores       : ORA-20070..ORA-20074 identifican precondiciones o ejecucion
--                 repetida; ORA-20075 identifica validacion final incompleta;
--                 ORA-20076/ORA-20077 bloquean UPDATE/DELETE append-only.
-- Auditoria     : cada fila conserva CREADO_POR y FECHA_CREACION. Los triggers
--                 impiden modificar o borrar historia y evidencias.
-- Orden         : prevalidaciones, secuencias, tablas, restricciones, indices,
--                 triggers append-only y validacion final.
-- Compensacion  : exclusivamente forward-only. Ante fallo posterior a un DDL,
--                 detener altas T072/T073, inventariar la huella con el DBA y
--                 depositar una correccion versionada; no eliminar historia,
--                 evidencias, constraints, secuencias ni triggers aplicados.
-- NEEDS CLARIFICATION: este incremento persiste la cadena estructural de
--                 versiones, pero no define que campos del ciclo se copian en
--                 cada version ni una regla de adyacencia entre numeros; esas
--                 reglas no fueron proporcionadas para este gate.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [025] Validando precondiciones 003 y 015 antes del primer DDL...

DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN ('DOCUMENTO', 'CICLO_PROYECTO',
                          'PRESENTACION_PRODUCTO_FINAL');
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20070,
            'Precondicion 025: huella de tablas 003/015 incompleta');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE (TABLE_NAME = 'DOCUMENTO' AND CONSTRAINT_NAME = 'PK_DOCUMENTO'
            AND CONSTRAINT_TYPE = 'P' AND STATUS = 'ENABLED')
        OR (TABLE_NAME = 'CICLO_PROYECTO' AND CONSTRAINT_NAME = 'PK_CICLO_PROYECTO'
            AND CONSTRAINT_TYPE = 'P' AND STATUS = 'ENABLED')
        OR (TABLE_NAME = 'PRESENTACION_PRODUCTO_FINAL'
            AND CONSTRAINT_NAME = 'PK_PRESENTACION_PRODUCTO_FINAL'
            AND CONSTRAINT_TYPE = 'P' AND STATUS = 'ENABLED');
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20071,
            'Precondicion 025: PK requerida de 003 o 015 ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN ('CICLO_PROYECTO_VERSION',
                          'PRESENTACION_PRODUCTO_FINAL_EVIDENCIA');
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20072,
            'Precondicion 025: tabla propia ya existe; ejecucion repetida o huella parcial');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN ('SEQ_CICLO_PROYECTO_VERSION', 'SEQ_PPF_EVIDENCIA');
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20073,
            'Precondicion 025: secuencia propia ya existe; ejecucion repetida o huella parcial');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TRIGGERS
     WHERE TRIGGER_NAME IN ('TRG_CPV_APPEND_ONLY', 'TRG_PPFE_APPEND_ONLY');
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20074,
            'Precondicion 025: trigger propio ya existe; revise la huella parcial');
    END IF;
END;
/

PROMPT [025] Precondiciones validadas. Iniciando DDL...

CREATE SEQUENCE SEQ_CICLO_PROYECTO_VERSION
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_PPF_EVIDENCIA
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE CICLO_PROYECTO_VERSION (
    ID_CICLO_VERSION      NUMBER(12) NOT NULL,
    ID_CICLO              NUMBER(12) NOT NULL,
    NUMERO_VERSION        NUMBER(3) NOT NULL,
    ID_VERSION_ANTERIOR   NUMBER(12),
    CREADO_POR            VARCHAR2(100 CHAR) NOT NULL,
    FECHA_CREACION        TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

ALTER TABLE CICLO_PROYECTO_VERSION
    ADD CONSTRAINT PK_CICLO_PROYECTO_VERSION PRIMARY KEY (ID_CICLO_VERSION);

ALTER TABLE CICLO_PROYECTO_VERSION
    ADD CONSTRAINT UK_CPV_CICLO_VERSION UNIQUE (ID_CICLO, NUMERO_VERSION);

ALTER TABLE CICLO_PROYECTO_VERSION
    ADD CONSTRAINT FK_CPV_CICLO
    FOREIGN KEY (ID_CICLO) REFERENCES CICLO_PROYECTO (ID_CICLO);

ALTER TABLE CICLO_PROYECTO_VERSION
    ADD CONSTRAINT FK_CPV_VERSION_ANTERIOR
    FOREIGN KEY (ID_VERSION_ANTERIOR)
    REFERENCES CICLO_PROYECTO_VERSION (ID_CICLO_VERSION);

ALTER TABLE CICLO_PROYECTO_VERSION
    ADD CONSTRAINT CK_CPV_VERSION_MIN CHECK (NUMERO_VERSION >= 1);

CREATE INDEX IDX_CPV_VERSION_ANTERIOR
    ON CICLO_PROYECTO_VERSION (ID_VERSION_ANTERIOR);

CREATE TABLE PRESENTACION_PRODUCTO_FINAL_EVIDENCIA (
    ID_EVIDENCIA          NUMBER(12) NOT NULL,
    ID_PRESENTACION       NUMBER(12) NOT NULL,
    ID_DOCUMENTO          NUMBER(12) NOT NULL,
    CREADO_POR            VARCHAR2(100 CHAR) NOT NULL,
    FECHA_CREACION        TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

ALTER TABLE PRESENTACION_PRODUCTO_FINAL_EVIDENCIA
    ADD CONSTRAINT PK_PPF_EVIDENCIA PRIMARY KEY (ID_EVIDENCIA);

ALTER TABLE PRESENTACION_PRODUCTO_FINAL_EVIDENCIA
    ADD CONSTRAINT UK_PPFE_PRESENTACION_DOCUMENTO
    UNIQUE (ID_PRESENTACION, ID_DOCUMENTO);

ALTER TABLE PRESENTACION_PRODUCTO_FINAL_EVIDENCIA
    ADD CONSTRAINT FK_PPFE_PRESENTACION
    FOREIGN KEY (ID_PRESENTACION)
    REFERENCES PRESENTACION_PRODUCTO_FINAL (ID_PRESENTACION);

ALTER TABLE PRESENTACION_PRODUCTO_FINAL_EVIDENCIA
    ADD CONSTRAINT FK_PPFE_DOCUMENTO
    FOREIGN KEY (ID_DOCUMENTO) REFERENCES DOCUMENTO (ID_DOCUMENTO);

CREATE INDEX IDX_PPFE_DOCUMENTO
    ON PRESENTACION_PRODUCTO_FINAL_EVIDENCIA (ID_DOCUMENTO);

CREATE OR REPLACE TRIGGER TRG_CPV_APPEND_ONLY
    BEFORE UPDATE OR DELETE ON CICLO_PROYECTO_VERSION
BEGIN
    RAISE_APPLICATION_ERROR(-20076,
        'CICLO_PROYECTO_VERSION es append-only; cree una nueva version');
END;
/

CREATE OR REPLACE TRIGGER TRG_PPFE_APPEND_ONLY
    BEFORE UPDATE OR DELETE ON PRESENTACION_PRODUCTO_FINAL_EVIDENCIA
BEGIN
    RAISE_APPLICATION_ERROR(-20077,
        'PRESENTACION_PRODUCTO_FINAL_EVIDENCIA es append-only; no se permite modificar ni borrar');
END;
/

PROMPT [025] Validando estado final del incremento...
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN ('CICLO_PROYECTO_VERSION',
                          'PRESENTACION_PRODUCTO_FINAL_EVIDENCIA');
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20075, 'Validacion 025: tablas ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN ('SEQ_CICLO_PROYECTO_VERSION', 'SEQ_PPF_EVIDENCIA')
       AND INCREMENT_BY = 1 AND CACHE_SIZE = 0 AND CYCLE_FLAG = 'N';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20075, 'Validacion 025: secuencias ausentes o incompatibles');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE CONSTRAINT_NAME IN ('PK_CICLO_PROYECTO_VERSION', 'UK_CPV_CICLO_VERSION',
                               'FK_CPV_CICLO', 'FK_CPV_VERSION_ANTERIOR',
                               'PK_PPF_EVIDENCIA', 'UK_PPFE_PRESENTACION_DOCUMENTO',
                               'FK_PPFE_PRESENTACION', 'FK_PPFE_DOCUMENTO')
       AND STATUS = 'ENABLED';
    IF v_total <> 8 THEN
        RAISE_APPLICATION_ERROR(-20075, 'Validacion 025: PK, UK o FK ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME IN ('IDX_CPV_VERSION_ANTERIOR', 'IDX_PPFE_DOCUMENTO')
       AND STATUS = 'VALID';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20075, 'Validacion 025: indice auxiliar invalido');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TRIGGERS
     WHERE TRIGGER_NAME IN ('TRG_CPV_APPEND_ONLY', 'TRG_PPFE_APPEND_ONLY')
       AND STATUS = 'ENABLED';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20075, 'Validacion 025: trigger append-only ausente');
    END IF;

    DBMS_OUTPUT.PUT_LINE('Validacion final satisfactoria: incremento 025 aplicado correctamente.');
END;
/

COMMIT;
PROMPT Migracion 025_ciclo_presentacion_evidencia_version completada correctamente.
