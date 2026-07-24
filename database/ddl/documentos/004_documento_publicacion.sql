-- ============================================================================
-- PIIP MIDAGRI - Migracion incremental 004 - Clasificacion validada,
-- historial de reclasificacion y publicacion documental
-- Archivo   : 004_documento_publicacion.sql
-- Esquema   : KALLPA_PIIP
-- Modulo    : documentos
-- Dependencias: 003 (database/ddl/documentos/003_expediente_serie_version.sql),
--               003.1 (003.1_tipo_documento_contexto_nullable.sql),
--               003.2 (003.2_documento_propietario_institucional.sql),
--               002 (database/ddl/auditoria/002_auditoria_idempotencia.sql),
--               008 (008_usuario_rol_unidad_vigencia.sql, con huella parcial
--                    confirmada) y 008.1 (008.1_secuencias_vigencia.sql) y
--               001 (database/ddl/init/001_baseline_piip.sql).
-- Alcance   : Crea DOCUMENTO_CLASIFICACION_HIST (append-only) y
--             DOCUMENTO_PUBLICACION con sus secuencias, PK, UK, FK y CHECKs.
--             Amplia DOCUMENTO con CLASIFICACION_VALIDADA,
--             CLASIFICACION_FECHA e ID_USUARIO_VALIDA, todos nullable.
--             No introduce logica de negocio: las reglas de transicion de
--             clasificacion y publicacion residen en PublicacionDocumentoService
--             Java. No modela ni prueba antimalware. La validacion de la
--             transicion restrictiva se aplica a nivel de CHECK mediante la
--             funcion CASE y, sobre la capa de aplicacion, mediante Java.
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: detener nuevas publicaciones y reclasificaciones;
--            conservar confirmaciones y auditoria. Las columnas agregadas a
--            DOCUMENTO quedan nullable para no afectar documentos legacy.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [004] Validando precondiciones de 001, 002, 003+003.1+003.2 y 008+008.1...

-- ----------------------------------------------------------------------------
-- 1) Precondiciones: 16 tablas (001+002+003) y 19 tablas tras 003.1, 003.2 y la
--    huella parcial 008 confirmada con 008.1 vigente.
-- ----------------------------------------------------------------------------
DECLARE
    v_tablas_baseline PLS_INTEGER;
    v_tablas_002_003 PLS_INTEGER;
    v_tablas_008 PLS_INTEGER;
    v_tablas_008_1 PLS_INTEGER;
    v_total_tablas_precedentes PLS_INTEGER;
    v_total_secuencias PLS_INTEGER;
BEGIN
    -- 13 tablas del baseline 001
    SELECT COUNT(*) INTO v_tablas_baseline
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'UNIDAD_EJECUTORA','USUARIO','ROL','USUARIO_ROL_UNIDAD',
            'PROYECTO','PROYECTO_UNIDAD_ORGANICA','TRANSICION_PERMITIDA',
            'TIPO_DOCUMENTO','DOCUMENTO','TRANSICION_ESTADO',
            'SECUENCIA_CODIGO','AUDITORIA_ACCESO','AUDITORIA_EVENTO'
           );
    IF v_tablas_baseline <> 13 THEN
        RAISE_APPLICATION_ERROR(
            -20010,
            'Precondicion 004: se esperaban 13 tablas del baseline 001 y se encontraron '
            || TO_CHAR(v_tablas_baseline)
        );
    END IF;

    -- Tablas adicionales de 002 y 003
    SELECT COUNT(*) INTO v_tablas_002_003
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'SOLICITUD_IDEMPOTENTE',
            'EXPEDIENTE_INSTITUCIONAL','DOCUMENTO_SERIE'
           );
    IF v_tablas_002_003 <> 3 THEN
        RAISE_APPLICATION_ERROR(
            -20011,
            'Precondicion 004: se esperaban 3 tablas adicionales de 002+003 y se encontraron '
            || TO_CHAR(v_tablas_002_003)
        );
    END IF;

    -- Huella parcial confirmada de 008 (3 tablas)
    SELECT COUNT(*) INTO v_tablas_008
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'USUARIO_ROL_UNIDAD_EVENTO',
            'SUPLENCIA_FUNCIONAL',
            'OPERACION_APROVISIONAMIENTO'
           );
    IF v_tablas_008 <> 3 THEN
        RAISE_APPLICATION_ERROR(
            -20012,
            'Precondicion 004: la huella parcial de 008 (3 tablas) no esta vigente; ejecute antes 008+008.1'
        );
    END IF;

    -- Secuencias de 008.1 vigentes
    SELECT COUNT(*) INTO v_tablas_008_1
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_URU_EVENTO',
            'SEQ_SUPLENCIA_FUNCIONAL',
            'SEQ_OPERACION_APROVISIONAMIENTO'
           );
    IF v_tablas_008_1 <> 3 THEN
        RAISE_APPLICATION_ERROR(
            -20013,
            'Precondicion 004: las 3 secuencias de 008.1 no estan vigentes'
        );
    END IF;

    -- Total acumulado de tablas precedentes esperadas: 19
    v_total_tablas_precedentes := v_tablas_baseline + v_tablas_002_003 + v_tablas_008;
    IF v_total_tablas_precedentes <> 19 THEN
        RAISE_APPLICATION_ERROR(
            -20014,
            'Precondicion 004: total de tablas precedentes inconsistente: '
            || TO_CHAR(v_total_tablas_precedentes)
        );
    END IF;

    -- Secuencias de 001+002+003+008.1 = 16 (13 + 3 de 002/003 + 3 de 008.1)
    -- NOCOUNT: 002 aporta 1, 003 aporta 2, 008.1 aporta 3 -> 13 + 1 + 2 + 3 = 19
    SELECT COUNT(*) INTO v_total_secuencias
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_UNIDAD_EJECUTORA','SEQ_USUARIO','SEQ_USUARIO_ROL_UNIDAD',
            'SEQ_PROYECTO','SEQ_PROYECTO_UO','SEQ_DOCUMENTO',
            'SEQ_TRANSICION_ESTADO','SEQ_SECUENCIA_CODIGO',
            'SEQ_AUDITORIA_ACCESO','SEQ_AUDITORIA_EVENTO',
            'SEQ_SOLICITUD_IDEMPOTENTE',
            'SEQ_EXPEDIENTE_INSTITUCIONAL','SEQ_DOCUMENTO_SERIE',
            'SEQ_URU_EVENTO','SEQ_SUPLENCIA_FUNCIONAL',
            'SEQ_OPERACION_APROVISIONAMIENTO'
           );
    IF v_total_secuencias <> 16 THEN
        RAISE_APPLICATION_ERROR(
            -20015,
            'Precondicion 004: se esperaban 16 secuencias (13 baseline + 002 + 003 + 008.1) y se encontraron '
            || TO_CHAR(v_total_secuencias)
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Validacion de columnas, constraints e indices generados por 003 y 008.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    -- DOCUMENTO_SERIE debe existir con su PK y CHECK XOR vigentes
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'DOCUMENTO_SERIE'
       AND CONSTRAINT_NAME IN ('PK_DOCUMENTO_SERIE','CK_DS_XOR_DUENIO')
       AND CONSTRAINT_TYPE IN ('P','C')
       AND STATUS = 'ENABLED';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(
            -20016,
            'Precondicion 004: PK o CHECK XOR de DOCUMENTO_SERIE no vigentes'
        );
    END IF;

    -- TIPO_DOCUMENTO.ESTADO_ASOCIADO debe admitir nulo (huella 003.1).
    -- CONTEXTO permanece NOT NULL por 003: el discriminante CONTEXTO es
    -- obligatorio y CK_TD_ESTADO_CONTEXTO exige ESTADO_ASOCIADO no nulo
    -- cuando CONTEXTO='PORTAFOLIO' y nulo cuando CONTEXTO='INSTITUCIONAL'.
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND COLUMN_NAME = 'ESTADO_ASOCIADO'
       AND NULLABLE = 'Y';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(
            -20017,
            'Precondicion 004: TIPO_DOCUMENTO.ESTADO_ASOCIADO no acepta nulo (huella 003.1)'
        );
    END IF;

    -- USUARIO_ROL_UNIDAD.VERSION debe existir (huella 008)
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'USUARIO_ROL_UNIDAD'
       AND COLUMN_NAME = 'VERSION'
       AND DATA_TYPE = 'NUMBER';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(
            -20018,
            'Precondicion 004: USUARIO_ROL_UNIDAD.VERSION ausente (huella 008)'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Validacion de que las tablas y columnas del propio 004 NO existen.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'DOCUMENTO_CLASIFICACION_HIST',
            'DOCUMENTO_PUBLICACION'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20019,
            'Precondicion 004: las tablas del incremento ya existen; el incremento ya fue aplicado'
        );
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND COLUMN_NAME IN (
            'CLASIFICACION_VALIDADA',
            'CLASIFICACION_FECHA',
            'ID_USUARIO_VALIDA'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20020,
            'Precondicion 004: DOCUMENTO ya tiene columnas de clasificacion validada; el incremento ya fue aplicado'
        );
    END IF;

    -- Las secuencias del propio incremento no deben existir
    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_DOCUMENTO_CLASIF_HIST',
            'SEQ_DOCUMENTO_PUBLICACION'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(
            -20021,
            'Precondicion 004: las secuencias del incremento ya existen; revise el estado'
        );
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 4) Validacion de objetos futuros 005-024 que NO deben existir.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'CAT_OBJETIVO_PEI_VERSION','CAT_OBJETIVO_PEI',
            'CAT_ACTIVIDAD_POI_VERSION','CAT_ACTIVIDAD_POI',
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
            -20022,
            'Precondicion 004: existen objetos futuros 005-024 ya creados'
        );
    END IF;
END;
/

PROMPT [004] Precondiciones validadas. Iniciando DDL del incremento...

-- ============================================================================
-- DDL del incremento 004
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1) Secuencias del incremento (commits implicitos por cada CREATE).
-- ----------------------------------------------------------------------------
PROMPT [004.1] Creando secuencias del incremento
CREATE SEQUENCE SEQ_DOCUMENTO_CLASIF_HIST
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE SEQUENCE SEQ_DOCUMENTO_PUBLICACION
    START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

-- ----------------------------------------------------------------------------
-- 2) Tabla DOCUMENTO_CLASIFICACION_HIST (append-only).
-- ----------------------------------------------------------------------------
PROMPT [004.2] Creando tabla DOCUMENTO_CLASIFICACION_HIST
CREATE TABLE DOCUMENTO_CLASIFICACION_HIST (
    ID_HISTORIAL            NUMBER(12)                      NOT NULL,
    ID_DOCUMENTO             NUMBER(12)                      NOT NULL,
    CLASIFICACION_ANTERIOR   VARCHAR2(20 CHAR),
    CLASIFICACION_NUEVA      VARCHAR2(20 CHAR)               NOT NULL,
    ID_AUTORIDAD_DECISORA    NUMBER(10)                      NOT NULL,
    ID_EVALUADOR_REGISTRADOR NUMBER(10)                      NOT NULL,
    ID_DOCUMENTO_DECISION    NUMBER(12),
    MOTIVO                   VARCHAR2(2000 CHAR),
    FECHA_CAMBIO             TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL,
    RESULTADO                VARCHAR2(20 CHAR)               NOT NULL
);

PROMPT [004.3] PK, FKs y CHECKs de DOCUMENTO_CLASIFICACION_HIST
ALTER TABLE DOCUMENTO_CLASIFICACION_HIST
    ADD CONSTRAINT PK_DOCUMENTO_CLASIFICACION_HIST PRIMARY KEY (ID_HISTORIAL);

ALTER TABLE DOCUMENTO_CLASIFICACION_HIST
    ADD CONSTRAINT FK_DCH_DOCUMENTO
    FOREIGN KEY (ID_DOCUMENTO) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE DOCUMENTO_CLASIFICACION_HIST
    ADD CONSTRAINT FK_DCH_AUTORIDAD
    FOREIGN KEY (ID_AUTORIDAD_DECISORA) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE DOCUMENTO_CLASIFICACION_HIST
    ADD CONSTRAINT FK_DCH_EVALUADOR
    FOREIGN KEY (ID_EVALUADOR_REGISTRADOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE DOCUMENTO_CLASIFICACION_HIST
    ADD CONSTRAINT FK_DCH_DOCUMENTO_DECISION
    FOREIGN KEY (ID_DOCUMENTO_DECISION) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE DOCUMENTO_CLASIFICACION_HIST
    ADD CONSTRAINT CK_DCH_CLAS_NUEVA
    CHECK (CLASIFICACION_NUEVA IN ('PUBLICO','INTERNO','RESTRINGIDO'));

ALTER TABLE DOCUMENTO_CLASIFICACION_HIST
    ADD CONSTRAINT CK_DCH_CLAS_ANT
    CHECK (CLASIFICACION_ANTERIOR IS NULL
           OR CLASIFICACION_ANTERIOR IN ('PUBLICO','INTERNO','RESTRINGIDO'));

ALTER TABLE DOCUMENTO_CLASIFICACION_HIST
    ADD CONSTRAINT CK_DCH_RESTRICTIVA
    CHECK (
        (CLASIFICACION_ANTERIOR IS NULL)
     OR (CLASIFICACION_ANTERIOR = 'PUBLICO'
         AND CLASIFICACION_NUEVA IN ('PUBLICO','INTERNO','RESTRINGIDO'))
     OR (CLASIFICACION_ANTERIOR = 'INTERNO'
         AND CLASIFICACION_NUEVA IN ('INTERNO','RESTRINGIDO'))
     OR (CLASIFICACION_ANTERIOR = 'RESTRINGIDO'
         AND CLASIFICACION_NUEVA = 'RESTRINGIDO')
    );

ALTER TABLE DOCUMENTO_CLASIFICACION_HIST
    ADD CONSTRAINT CK_DCH_RESULTADO
    CHECK (RESULTADO IN ('APLICADA','RECHAZADA','REVERTIDA'));

ALTER TABLE DOCUMENTO_CLASIFICACION_HIST
    ADD CONSTRAINT CK_DCH_AUTORIDAD_DISTINTA_EVALUADOR
    CHECK (ID_AUTORIDAD_DECISORA <> ID_EVALUADOR_REGISTRADOR);

PROMPT [004.4] Indice auxiliar IDX_DCH_DOCUMENTO
CREATE INDEX IDX_DCH_DOCUMENTO
    ON DOCUMENTO_CLASIFICACION_HIST (ID_DOCUMENTO);

-- ----------------------------------------------------------------------------
-- 3) Tabla DOCUMENTO_PUBLICACION.
-- ----------------------------------------------------------------------------
PROMPT [004.5] Creando tabla DOCUMENTO_PUBLICACION
CREATE TABLE DOCUMENTO_PUBLICACION (
    ID_PUBLICACION             NUMBER(12)                      NOT NULL,
    ID_DOCUMENTO                NUMBER(12)                      NOT NULL,
    TITULO_PUBLICO              VARCHAR2(500 CHAR)               NOT NULL,
    ID_EVALUADOR_CONFIRMADOR    NUMBER(10)                      NOT NULL,
    ID_ASIGNACION_EFECTIVA      NUMBER(10)                      NOT NULL,
    FECHA_PUBLICACION           TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

PROMPT [004.6] PK, UK, FKs y CHECKs de DOCUMENTO_PUBLICACION
ALTER TABLE DOCUMENTO_PUBLICACION
    ADD CONSTRAINT PK_DOCUMENTO_PUBLICACION PRIMARY KEY (ID_PUBLICACION);

ALTER TABLE DOCUMENTO_PUBLICACION
    ADD CONSTRAINT UK_DP_DOCUMENTO UNIQUE (ID_DOCUMENTO);

ALTER TABLE DOCUMENTO_PUBLICACION
    ADD CONSTRAINT FK_DP_DOCUMENTO
    FOREIGN KEY (ID_DOCUMENTO) REFERENCES DOCUMENTO (ID_DOCUMENTO);

ALTER TABLE DOCUMENTO_PUBLICACION
    ADD CONSTRAINT FK_DP_EVALUADOR
    FOREIGN KEY (ID_EVALUADOR_CONFIRMADOR) REFERENCES USUARIO (ID_USUARIO);

-- No se crea FK hacia USUARIO_ROL_UNIDAD en este script: la relacion se
-- materializa mediante el servicio Java para preservar el acoplamiento
-- controlado con la huella 008 (vigencia y asignacion efectiva).

ALTER TABLE DOCUMENTO_PUBLICACION
    ADD CONSTRAINT CK_DP_FORMATO_TITULO
    CHECK (
        TITULO_PUBLICO IS NOT NULL
        AND LENGTH(TRIM(TITULO_PUBLICO)) >= 5
        AND NOT REGEXP_LIKE(TITULO_PUBLICO, '@')
        AND NOT REGEXP_LIKE(TITULO_PUBLICO, '([0-9]){9,12}')
    );

PROMPT [004.7] Indice auxiliar IDX_DP_PUBLICACION_FECHA
CREATE INDEX IDX_DP_PUBLICACION_FECHA
    ON DOCUMENTO_PUBLICACION (FECHA_PUBLICACION);

-- ----------------------------------------------------------------------------
-- 4) Modificacion a DOCUMENTO: columnas de clasificacion validada.
-- ----------------------------------------------------------------------------
PROMPT [004.8] Anadiendo CLASIFICACION_VALIDADA, CLASIFICACION_FECHA e ID_USUARIO_VALIDA
ALTER TABLE DOCUMENTO ADD (CLASIFICACION_VALIDADA VARCHAR2(20 CHAR));
ALTER TABLE DOCUMENTO ADD (CLASIFICACION_FECHA     TIMESTAMP(6));
ALTER TABLE DOCUMENTO ADD (ID_USUARIO_VALIDA      NUMBER(10));

PROMPT [004.9] CHECK de dominio para CLASIFICACION_VALIDADA
ALTER TABLE DOCUMENTO
    ADD CONSTRAINT CK_DOC_CLAS_VALIDADA
    CHECK (CLASIFICACION_VALIDADA IS NULL
           OR CLASIFICACION_VALIDADA IN ('PUBLICO','INTERNO','RESTRINGIDO'));

PROMPT [004.10] FK opcional ID_USUARIO_VALIDA
ALTER TABLE DOCUMENTO
    ADD CONSTRAINT FK_DOC_USUARIO_VALIDA
    FOREIGN KEY (ID_USUARIO_VALIDA) REFERENCES USUARIO (ID_USUARIO);

-- ============================================================================
-- Validacion final del script 004
-- ============================================================================
PROMPT [004.11] Validando estado final del incremento
DECLARE
    v_total PLS_INTEGER;
BEGIN
    -- Tablas nuevas
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'DOCUMENTO_CLASIFICACION_HIST',
            'DOCUMENTO_PUBLICACION'
           );
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20030,
            'Validacion 004: tablas nuevas ausentes');
    END IF;

    -- Secuencias nuevas
    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_DOCUMENTO_CLASIF_HIST',
            'SEQ_DOCUMENTO_PUBLICACION'
           )
       AND INCREMENT_BY = 1
       AND CACHE_SIZE = 0
       AND CYCLE_FLAG = 'N';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20031,
            'Validacion 004: secuencias nuevas ausentes o con atributos incompatibles');
    END IF;

    -- PKs nuevas
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME IN (
            'DOCUMENTO_CLASIFICACION_HIST',
            'DOCUMENTO_PUBLICACION'
           )
       AND CONSTRAINT_NAME IN (
            'PK_DOCUMENTO_CLASIFICACION_HIST',
            'PK_DOCUMENTO_PUBLICACION'
           )
       AND CONSTRAINT_TYPE = 'P'
       AND STATUS = 'ENABLED';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20032,
            'Validacion 004: PKs de tablas nuevas ausentes');
    END IF;

    -- UK sobre DOCUMENTO_PUBLICACION(ID_DOCUMENTO)
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'DOCUMENTO_PUBLICACION'
       AND CONSTRAINT_NAME = 'UK_DP_DOCUMENTO'
       AND CONSTRAINT_TYPE = 'U';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20033,
            'Validacion 004: UK_DP_DOCUMENTO ausente');
    END IF;

    -- Columnas agregadas a DOCUMENTO
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'DOCUMENTO'
       AND COLUMN_NAME IN (
            'CLASIFICACION_VALIDADA',
            'CLASIFICACION_FECHA',
            'ID_USUARIO_VALIDA'
           );
    IF v_total <> 3 THEN
        RAISE_APPLICATION_ERROR(-20034,
            'Validacion 004: columnas de clasificacion validada incompletas en DOCUMENTO');
    END IF;

    -- Indices auxiliares
    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME IN ('IDX_DCH_DOCUMENTO','IDX_DP_PUBLICACION_FECHA')
       AND STATUS = 'VALID';
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20035,
            'Validacion 004: indices auxiliares ausentes o invalidos');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: incremento 004 aplicado correctamente.');
END;
/

COMMIT;

PROMPT Migracion 004_documento_publicacion completada correctamente.
