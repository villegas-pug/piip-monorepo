-- ============================================================================
-- PIIP MIDAGRI - Correccion forward-only 014.1 - Invariante determinista de
-- SUBSANACION_INICIATIVA.PLAZO y DDL faltante de 014
-- Archivo    : 014.1_subsanacion_iniciativa_plazo.sql
-- Esquema    : KALLPA_PIIP
-- Modulo     : portafolio
-- Dependencias: 001, 002, 003, 003.1, 003.2, 004, 005, 005.1, 006, 007,
--               008+008.1, 009, 010, 011, 012, 013 VIGENTES, y huella
--               parcial confirmada de 014.
--
-- Causa: la ejecucion manual de 014 alcanzo la creacion de las cuatro
--        secuencias, las tablas EVALUACION_INICIATIVA y SUBSANACION_INICIATIVA
--        y sus PK, UK y FKs. Falla al crear CK_SI_PLAZO con
--        ORA-02436: la regla original PLazo >= TRUNC(SYSDATE) usaba
--        SYSDATE dentro de un CHECK, lo cual Oracle prohibe por no ser
--        determinista.
--
-- Alcance: (a) crea CK_SI_PLAZO con una invariante determinista a nivel de
--          fila (PLAZO IS NULL OR APERTURA_EN IS NULL OR PLAZO > APERTURA_EN);
--          (b) crea APLICABILIDAD_INICIATIVA, APLICABILIDAD_CRITERIO y
--          todos sus PK/UK/FK/CHECKs/indices que quedaron pendientes por
--          la falla. No recrea objetos previos. 014 NO debe re-ejecutarse.
--
-- Nota sobre determinismo: la nueva regla compara dos columnas
-- persistidas (PLAZO y APERTURA_EN) y no depende del reloj del servidor,
-- por lo que es valida como CHECK constraint. La regla de negocio es
-- "la subsanacion solo puede ocurrir despues de abierta".
--
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: detener altas de subsanacion/evaluacion/
--            aplicabilidad mientras no se confirme; nunca eliminar las
--            tablas ni constraints previos. Tras el exito, el incremento
--            014 se considerara vigente y la nota ORA-02436 quedara
--            registrada en CHANGELOG.md.
-- SHA-256: este cambio altera la invariante documentada de
--          database-physical-design.md; el SHA-256 del diseno fisico
--          cambiara cuando el especialista re-apruebe el catalogo.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [014.1] Validando huella parcial confirmada de 014 y predecesores vigentes...

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
            'SOLICITUD_IDEMPOTENTE',
            'EXPEDIENTE_INSTITUCIONAL','DOCUMENTO_SERIE',
            'CAT_OBJETIVO_PEI_VERSION','CAT_OBJETIVO_PEI',
            'CAT_ACTIVIDAD_POI_VERSION','CAT_ACTIVIDAD_POI',
            'MATRIZ_FUNCIONAL_VERSION','MATRIZ_FUNCION',
            'MATRIZ_FUNCION_PERFIL_UNIDAD',
            'USUARIO_ROL_UNIDAD_EVENTO','SUPLENCIA_FUNCIONAL',
            'OPERACION_APROVISIONAMIENTO',
            'DOCUMENTO_CLASIFICACION_HIST','DOCUMENTO_PUBLICACION',
            'INICIATIVA_PROYECTO','PROYECTO_RESPONSABLE',
            'PROYECTO_CAMPO_CLASIFICACION','PROYECTO_CAMPO_CLASIF_HIST',
            'EVALUACION_INICIATIVA','SUBSANACION_INICIATIVA'
           );
    IF v_total <> 34 THEN
        RAISE_APPLICATION_ERROR(-20100,
            'Precondicion 014.1: huella de tablas vigentes incompleta (34 esperadas)');
    END IF;

    -- Huella vigente antes de 014.1: 10 de 001 + 1 de 002 + 2 de 003 +
    -- 2 de 004 + 2 de 005 + 2 de 006 + 3 de 007 + 3 de 008.1 + 4 de 014
    -- parcial = 29. 010, 011, 012 y 013 permanecen PENDIENTES y todavia
    -- no forman parte del esquema fisico; sus secuencias no deben estar
    -- en la base. La correccion 014.1 NO crea nuevas secuencias: las
    -- cuatro de 014 (incluidas SEQ_APLICABILIDAD_*) ya quedaron vigentes
    -- antes de la falla ORA-02436; lo unico que 014.1 crea son el
    -- CHECK determinista CK_SI_PLAZO, las tablas APLICABILIDAD_*
    -- y los constraints/indices de estas.
    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_UNIDAD_EJECUTORA','SEQ_USUARIO','SEQ_USUARIO_ROL_UNIDAD',
            'SEQ_PROYECTO','SEQ_PROYECTO_UO','SEQ_DOCUMENTO',
            'SEQ_TRANSICION_ESTADO','SEQ_SECUENCIA_CODIGO',
            'SEQ_AUDITORIA_ACCESO','SEQ_AUDITORIA_EVENTO',
            'SEQ_SOLICITUD_IDEMPOTENTE',
            'SEQ_EXPEDIENTE_INSTITUCIONAL','SEQ_DOCUMENTO_SERIE',
            'SEQ_DOCUMENTO_CLASIF_HIST','SEQ_DOCUMENTO_PUBLICACION',
            'SEQ_OBJETIVO_PEI_VERSION','SEQ_OBJETIVO_PEI',
            'SEQ_ACTIVIDAD_POI_VERSION','SEQ_ACTIVIDAD_POI',
            'SEQ_MATRIZ_VERSION','SEQ_MATRIZ_FUNCION','SEQ_MATRIZ_COMBINACION',
            'SEQ_URU_EVENTO','SEQ_SUPLENCIA_FUNCIONAL',
            'SEQ_OPERACION_APROVISIONAMIENTO',
            'SEQ_EVALUACION_INICIATIVA','SEQ_SUBSANACION_INICIATIVA',
            'SEQ_APLICABILIDAD_INICIATIVA','SEQ_APLICABILIDAD_CRITERIO'
           );
    IF v_total <> 29 THEN
        RAISE_APPLICATION_ERROR(-20101,
            'Precondicion 014.1: huella de secuencias vigentes incompleta (29 esperadas)');
    END IF;

    -- Objetos futuros esperados: SOLO sucesores estrictos de 014.
    -- 015, 016 y 017 son los unicos sucesores de 014 en el alcance fisico
    -- activo; los incrementos 001-013 ya estan vigentes y no deben
    -- incluirse aqui. Los diferidos 018-021 permanecen fuera del
    -- alcance fisico y no se validan.
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'PLANIFICACION_PROYECTO','CICLO_PROYECTO','CICLO_EVIDENCIA',
            'PRODUCTO_PARCIAL','PRESENTACION_PRODUCTO_FINAL',
            'VALIDACION_RESULTADO','CIERRE_PROYECTO',
            'INCORPORACION_REGISTRO','INCORPORACION_CAMBIO',
            'INCORPORACION_CONFLICTO',
            'REPORTE_INSTITUCIONAL','REPORTE_SNAPSHOT','REPORTE_ARCHIVO',
            'REPORTE_APROBACION','REPORTE_DESTINATARIO','REPORTE_REMISION'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20103,
            'Precondicion 014.1: existen objetos futuros 015-017 ya creados; requiere revision humana');
    END IF;
END;
/

PROMPT [014.1] Verificando que CK_SI_PLAZO, APLICABILIDAD_INICIATIVA y APLICABILIDAD_CRITERIO no existen aun...

DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE TABLE_NAME = 'SUBSANACION_INICIATIVA'
       AND CONSTRAINT_NAME = 'CK_SI_PLAZO'
       AND CONSTRAINT_TYPE = 'C';
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20199,
            'Precondicion 014.1: CK_SI_PLAZO ya existe; la correccion 014.1 ya fue aplicada o 014 re-ejecuto');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN ('APLICABILIDAD_INICIATIVA','APLICABILIDAD_CRITERIO');
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20199,
            'Precondicion 014.1: las tablas de aplicabilidad ya existen; la correccion 014.1 ya fue aplicada');
    END IF;
END;
/

PROMPT [014.1] Verificando PK/UK/FK previos de EVALUACION_INICIATIVA y SUBSANACION_INICIATIVA...

DECLARE
    v_total PLS_INTEGER;
    v_incidencias VARCHAR2(4000);

    PROCEDURE validar_constraint(
        p_nombre IN VARCHAR2,
        p_tabla IN VARCHAR2,
        p_tipo IN VARCHAR2
    ) IS
        v_cantidad PLS_INTEGER;
        v_tipo_real USER_CONSTRAINTS.CONSTRAINT_TYPE%TYPE;
        v_estado_real USER_CONSTRAINTS.STATUS%TYPE;
    BEGIN
        SELECT COUNT(*), MIN(CONSTRAINT_TYPE), MIN(STATUS)
          INTO v_cantidad, v_tipo_real, v_estado_real
          FROM USER_CONSTRAINTS
         WHERE TABLE_NAME = p_tabla
           AND CONSTRAINT_NAME = p_nombre;

        IF v_cantidad = 0 THEN
            v_incidencias := v_incidencias || p_nombre || '[MISSING];';
        ELSIF v_cantidad <> 1 THEN
            v_incidencias := v_incidencias || p_nombre || '[COUNT_' || TO_CHAR(v_cantidad) || '];';
        ELSIF v_tipo_real <> p_tipo THEN
            v_incidencias := v_incidencias || p_nombre || '[TYPE_' || NVL(v_tipo_real, 'NULL') || '];';
        ELSIF v_estado_real <> 'ENABLED' THEN
            v_incidencias := v_incidencias || p_nombre || '[STATUS_' || NVL(v_estado_real, 'NULL') || '];';
        END IF;
    END validar_constraint;
BEGIN
    validar_constraint('PK_EVALUACION_INICIATIVA', 'EVALUACION_INICIATIVA', 'P');
    validar_constraint('UK_EI_INICIATIVA',         'EVALUACION_INICIATIVA', 'U');
    validar_constraint('FK_EI_INICIATIVA',         'EVALUACION_INICIATIVA', 'R');
    validar_constraint('FK_EI_EVALUADOR',          'EVALUACION_INICIATIVA', 'R');
    validar_constraint('FK_EI_ROL_EFECTIVO',       'EVALUACION_INICIATIVA', 'R');
    validar_constraint('FK_EI_UNIDAD_EFECTIVA',    'EVALUACION_INICIATIVA', 'R');
    validar_constraint('FK_EI_DOCUMENTO_OPINION',  'EVALUACION_INICIATIVA', 'R');
    validar_constraint('CK_EI_OBSERVACION_LONGITUD','EVALUACION_INICIATIVA', 'C');
    validar_constraint('PK_SUBSANACION_INICIATIVA','SUBSANACION_INICIATIVA', 'P');
    validar_constraint('UK_SI_INICIATIVA',         'SUBSANACION_INICIATIVA', 'U');
    validar_constraint('FK_SI_INICIATIVA',         'SUBSANACION_INICIATIVA', 'R');
    validar_constraint('FK_SI_ACTOR',              'SUBSANACION_INICIATIVA', 'R');

    IF v_incidencias IS NOT NULL THEN
        RAISE_APPLICATION_ERROR(-20104,
            'Precondicion 014.1: huella parcial 014 invalida; revisar constraints previos: ' || v_incidencias);
    END IF;
END;
/

PROMPT [014.1] Aplicando CK_SI_PLAZO determinista y DDL pendiente de 014...

-- ----------------------------------------------------------------------------
-- 1) Invariante determinista de SUBSANACION_INICIATIVA.PLAZO. Compara dos
-- columnas persistidas (PLAZO y APERTURA_EN); no depende del reloj del
-- servidor. La nulabilidad se acepta: la condicion es verdadera si
-- PLAZO es nulo, si APERTURA_EN es nulo, o si PLAZO es estrictamente
-- posterior a APERTURA_EN.
-- ----------------------------------------------------------------------------
ALTER TABLE SUBSANACION_INICIATIVA
    ADD CONSTRAINT CK_SI_PLAZO
    CHECK (PLAZO IS NULL OR APERTURA_EN IS NULL OR PLAZO > APERTURA_EN);

PROMPT [014.1] Creando tabla APLICABILIDAD_INICIATIVA

CREATE TABLE APLICABILIDAD_INICIATIVA (
    ID_APLICABILIDAD   NUMBER(12)                      NOT NULL,
    ID_INICIATIVA      NUMBER(12)                      NOT NULL,
    RESULTADO          VARCHAR2(20 CHAR)               NOT NULL,
    MOTIVO             VARCHAR2(2000 CHAR),
    ID_EVALUADOR       NUMBER(10)                      NOT NULL,
    FECHA              TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL
);

-- SEQ_APLICABILIDAD_INICIATIVA ya fue creada por la huella parcial 014;
-- no se recrea para evitar ORA-00955.

ALTER TABLE APLICABILIDAD_INICIATIVA
    ADD CONSTRAINT PK_APLICABILIDAD_INICIATIVA PRIMARY KEY (ID_APLICABILIDAD);

ALTER TABLE APLICABILIDAD_INICIATIVA
    ADD CONSTRAINT UK_AI_INICIATIVA UNIQUE (ID_INICIATIVA);

ALTER TABLE APLICABILIDAD_INICIATIVA
    ADD CONSTRAINT FK_AI_INICIATIVA
    FOREIGN KEY (ID_INICIATIVA) REFERENCES PROYECTO (ID_PROYECTO);

ALTER TABLE APLICABILIDAD_INICIATIVA
    ADD CONSTRAINT FK_AI_EVALUADOR
    FOREIGN KEY (ID_EVALUADOR) REFERENCES USUARIO (ID_USUARIO);

ALTER TABLE APLICABILIDAD_INICIATIVA
    ADD CONSTRAINT CK_AI_RESULTADO
    CHECK (RESULTADO IN ('APLICABLE','NO_APLICABLE'));

-- CK_AI_MOTIVO es una invariante determinista a nivel de fila: si el
-- resultado es NO_APLICABLE, MOTIVO debe ser no nulo. No usa SYSDATE
-- ni funciones no deterministas.
ALTER TABLE APLICABILIDAD_INICIATIVA
    ADD CONSTRAINT CK_AI_MOTIVO
    CHECK (
        (RESULTADO = 'APLICABLE')
     OR (RESULTADO = 'NO_APLICABLE' AND MOTIVO IS NOT NULL)
    );

PROMPT [014.1] Creando tabla APLICABILIDAD_CRITERIO

CREATE TABLE APLICABILIDAD_CRITERIO (
    ID_CRITERIO        NUMBER(12)                      NOT NULL,
    ID_APLICABILIDAD   NUMBER(12)                      NOT NULL,
    CLAVE              VARCHAR2(50 CHAR)               NOT NULL,
    VALOR              VARCHAR2(500 CHAR)               NOT NULL,
    ORDEN              NUMBER(3)                       NOT NULL
);

-- SEQ_APLICABILIDAD_CRITERIO ya fue creada por la huella parcial 014;
-- no se recrea para evitar ORA-00955.

ALTER TABLE APLICABILIDAD_CRITERIO
    ADD CONSTRAINT PK_APLICABILIDAD_CRITERIO PRIMARY KEY (ID_CRITERIO);

ALTER TABLE APLICABILIDAD_CRITERIO
    ADD CONSTRAINT UK_AC_APLICABILIDAD_CLAVE
    UNIQUE (ID_APLICABILIDAD, CLAVE);

ALTER TABLE APLICABILIDAD_CRITERIO
    ADD CONSTRAINT FK_AC_APLICABILIDAD
    FOREIGN KEY (ID_APLICABILIDAD) REFERENCES APLICABILIDAD_INICIATIVA (ID_APLICABILIDAD);

ALTER TABLE APLICABILIDAD_CRITERIO
    ADD CONSTRAINT CK_AC_ORDEN
    CHECK (ORDEN BETWEEN 1 AND 999);

PROMPT [014.1] Validando huella total de 014 tras la correccion...

DECLARE
    v_total PLS_INTEGER;
    v_incidencias VARCHAR2(4000);

    PROCEDURE validar_constraint(
        p_nombre IN VARCHAR2,
        p_tabla IN VARCHAR2,
        p_tipo IN VARCHAR2
    ) IS
        v_cantidad PLS_INTEGER;
        v_tipo_real USER_CONSTRAINTS.CONSTRAINT_TYPE%TYPE;
        v_estado_real USER_CONSTRAINTS.STATUS%TYPE;
    BEGIN
        SELECT COUNT(*), MIN(CONSTRAINT_TYPE), MIN(STATUS)
          INTO v_cantidad, v_tipo_real, v_estado_real
          FROM USER_CONSTRAINTS
         WHERE TABLE_NAME = p_tabla
           AND CONSTRAINT_NAME = p_nombre;

        IF v_cantidad = 0 THEN
            v_incidencias := v_incidencias || p_nombre || '[MISSING];';
        ELSIF v_cantidad <> 1 THEN
            v_incidencias := v_incidencias || p_nombre || '[COUNT_' || TO_CHAR(v_cantidad) || '];';
        ELSIF v_tipo_real <> p_tipo THEN
            v_incidencias := v_incidencias || p_nombre || '[TYPE_' || NVL(v_tipo_real, 'NULL') || '];';
        ELSIF v_estado_real <> 'ENABLED' THEN
            v_incidencias := v_incidencias || p_nombre || '[STATUS_' || NVL(v_estado_real, 'NULL') || '];';
        END IF;
    END validar_constraint;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'EVALUACION_INICIATIVA','SUBSANACION_INICIATIVA',
            'APLICABILIDAD_INICIATIVA','APLICABILIDAD_CRITERIO'
           );
    IF v_total <> 4 THEN
        RAISE_APPLICATION_ERROR(-20105,
            'Validacion 014.1: tablas de 014 ausentes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_EVALUACION_INICIATIVA','SEQ_SUBSANACION_INICIATIVA',
            'SEQ_APLICABILIDAD_INICIATIVA','SEQ_APLICABILIDAD_CRITERIO'
           )
       AND INCREMENT_BY = 1 AND CACHE_SIZE = 0 AND CYCLE_FLAG = 'N';
    IF v_total <> 4 THEN
        RAISE_APPLICATION_ERROR(-20106,
            'Validacion 014.1: secuencias de 014 ausentes o incompatibles');
    END IF;

    validar_constraint('PK_EVALUACION_INICIATIVA',         'EVALUACION_INICIATIVA', 'P');
    validar_constraint('UK_EI_INICIATIVA',                 'EVALUACION_INICIATIVA', 'U');
    validar_constraint('FK_EI_INICIATIVA',                 'EVALUACION_INICIATIVA', 'R');
    validar_constraint('FK_EI_EVALUADOR',                  'EVALUACION_INICIATIVA', 'R');
    validar_constraint('FK_EI_ROL_EFECTIVO',               'EVALUACION_INICIATIVA', 'R');
    validar_constraint('FK_EI_UNIDAD_EFECTIVA',            'EVALUACION_INICIATIVA', 'R');
    validar_constraint('FK_EI_DOCUMENTO_OPINION',          'EVALUACION_INICIATIVA', 'R');
    validar_constraint('CK_EI_OBSERVACION_LONGITUD',       'EVALUACION_INICIATIVA', 'C');
    validar_constraint('PK_SUBSANACION_INICIATIVA',        'SUBSANACION_INICIATIVA', 'P');
    validar_constraint('UK_SI_INICIATIVA',                 'SUBSANACION_INICIATIVA', 'U');
    validar_constraint('FK_SI_INICIATIVA',                 'SUBSANACION_INICIATIVA', 'R');
    validar_constraint('FK_SI_ACTOR',                      'SUBSANACION_INICIATIVA', 'R');
    validar_constraint('CK_SI_PLAZO',                      'SUBSANACION_INICIATIVA', 'C');
    validar_constraint('PK_APLICABILIDAD_INICIATIVA',      'APLICABILIDAD_INICIATIVA', 'P');
    validar_constraint('UK_AI_INICIATIVA',                 'APLICABILIDAD_INICIATIVA', 'U');
    validar_constraint('FK_AI_INICIATIVA',                 'APLICABILIDAD_INICIATIVA', 'R');
    validar_constraint('FK_AI_EVALUADOR',                  'APLICABILIDAD_INICIATIVA', 'R');
    validar_constraint('CK_AI_RESULTADO',                  'APLICABILIDAD_INICIATIVA', 'C');
    validar_constraint('CK_AI_MOTIVO',                     'APLICABILIDAD_INICIATIVA', 'C');
    validar_constraint('PK_APLICABILIDAD_CRITERIO',        'APLICABILIDAD_CRITERIO', 'P');
    validar_constraint('UK_AC_APLICABILIDAD_CLAVE',        'APLICABILIDAD_CRITERIO', 'U');
    validar_constraint('FK_AC_APLICABILIDAD',              'APLICABILIDAD_CRITERIO', 'R');
    validar_constraint('CK_AC_ORDEN',                      'APLICABILIDAD_CRITERIO', 'C');

    IF v_incidencias IS NOT NULL THEN
        RAISE_APPLICATION_ERROR(-20107,
            'Validacion 014.1: constraints de 014 invalidos: ' || v_incidencias);
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: correccion 014.1 aplicada.');
END;
/

-- El COMMIT final es documental; los CREATE/ALTER confirman implicitamente.
COMMIT;

PROMPT Correccion 014.1_subsanacion_iniciativa_plazo completada correctamente.
