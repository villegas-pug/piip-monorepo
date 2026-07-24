-- ============================================================================
-- PIIP MIDAGRI - Correccion forward-only 005.1 - Huella parcial de catalogos
-- Objetivo PEI y validacion del indice canonico de la UK
-- Archivo    : 005.1_objetivo_pei_versionado_indice.sql
-- Esquema    : KALLPA_PIIP
-- Modulo     : organizacion
-- Dependencias: 001 (database/ddl/init/001_baseline_piip.sql),
--               002 (database/ddl/auditoria/002_auditoria_idempotencia.sql),
--               003, 003.1 y 003.2 (documentos),
--               004 (database/ddl/documentos/004_documento_publicacion.sql),
--               007 (database/ddl/seguridad/007_matriz_funcional_versionada.sql)
--               y 008+008.1 (huella parcial y secuencias de vigencia).
--               Requiere la huella parcial confirmada de
--               005_objetivo_pei_versionado.sql.
--
-- Causa: la ejecucion manual de 005 alcanzo la creacion de las dos
--        secuencias, las tablas CAT_OBJETIVO_PEI_VERSION y CAT_OBJETIVO_PEI,
--        sus PK, UK, FK y CHECKs. Fallo en CREATE INDEX
--        IDX_OP_VERSION_CODIGO por ORA-01408: UK_OP_VERSION_CODIGO ya provee
--        el indice unico de respaldo sobre (ID_VERSION, CODIGO).
--
-- Alcance: valida la huella parcial completa de 005 y confirma que el indice
--          canonico de UK_OP_VERSION_CODIGO es valido, unico y conserva el
--          orden de columnas. No crea un segundo indice redundante. Registra
--          una marca tecnica no funcional mediante COMMENT ON TABLE para
--          impedir la reejecucion de esta correccion. El script 005 NO debe
--          reejecutarse.
--
-- Ejecucion: SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--            KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: detener altas PEI; conservar catalogos y
--            referencias historicas. Nunca eliminar tablas, secuencias, UK ni
--            el indice canonico respaldado por la UK.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [005.1] Validando huella parcial confirmada de 005 y predecesores vigentes...

-- ----------------------------------------------------------------------------
-- 1) Huella de tablas y secuencias: 001+002+003+004+007+008/008.1 y 005
--    parcial. Los objetos de 004, 007 y 008 se permiten por ser paralelo o
--    predecesor vigente de esta correccion.
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
            'SOLICITUD_IDEMPOTENTE',
            'EXPEDIENTE_INSTITUCIONAL','DOCUMENTO_SERIE',
            'DOCUMENTO_CLASIFICACION_HIST','DOCUMENTO_PUBLICACION',
            'MATRIZ_FUNCIONAL_VERSION','MATRIZ_FUNCION','MATRIZ_FUNCION_PERFIL_UNIDAD',
            'USUARIO_ROL_UNIDAD_EVENTO','SUPLENCIA_FUNCIONAL','OPERACION_APROVISIONAMIENTO',
            'CAT_OBJETIVO_PEI_VERSION','CAT_OBJETIVO_PEI'
           );
    IF v_total <> 26 THEN
        RAISE_APPLICATION_ERROR(-20070,
            'Precondicion 005.1: huella de tablas previa y parcial de 005 incompleta');
    END IF;

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
            'SEQ_MATRIZ_VERSION','SEQ_MATRIZ_FUNCION','SEQ_MATRIZ_COMBINACION',
            'SEQ_URU_EVENTO','SEQ_SUPLENCIA_FUNCIONAL','SEQ_OPERACION_APROVISIONAMIENTO',
            'SEQ_OBJETIVO_PEI_VERSION','SEQ_OBJETIVO_PEI'
           );
    IF v_total <> 23 THEN
        RAISE_APPLICATION_ERROR(-20071,
            'Precondicion 005.1: huella de secuencias previa y parcial de 005 incompleta');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE (TABLE_NAME = 'CAT_OBJETIVO_PEI_VERSION' AND COLUMN_NAME IN (
                'ID_VERSION','CODIGO_VERSION','ID_VERSION_ANTERIOR',
                'ID_DOCUMENTO_APROBACION','OFICINA_APROBADORA','VIGENTE_DESDE',
                'VIGENTE_HASTA','ACTIVA','CREADO_POR','FECHA_CREACION'))
        OR (TABLE_NAME = 'CAT_OBJETIVO_PEI' AND COLUMN_NAME IN (
                'ID_OBJETIVO','ID_VERSION','CODIGO','DESCRIPCION',
                'VIGENTE_DESDE','VIGENTE_HASTA','ACTIVO'));
    IF v_total <> 17 THEN
        RAISE_APPLICATION_ERROR(-20072,
            'Precondicion 005.1: columnas de catalogos PEI parciales incompletas');
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2) Huella 003.1/003.2 y constraints creados por 005 antes de ORA-01408.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'TIPO_DOCUMENTO'
       AND COLUMN_NAME = 'ESTADO_ASOCIADO'
       AND NULLABLE = 'Y';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20073,
            'Precondicion 005.1: ESTADO_ASOCIADO no refleja la correccion 003.1');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS
     WHERE (TABLE_NAME = 'TIPO_DOCUMENTO'
            AND CONSTRAINT_NAME = 'CK_TD_ESTADO_CONTEXTO'
            AND CONSTRAINT_TYPE = 'C'
            AND STATUS = 'ENABLED')
        OR (TABLE_NAME = 'DOCUMENTO'
            AND CONSTRAINT_NAME = 'FK_DOC_TIPO'
            AND CONSTRAINT_TYPE = 'R'
            AND STATUS = 'ENABLED');
    IF v_total <> 2 THEN
        RAISE_APPLICATION_ERROR(-20074,
            'Precondicion 005.1: huella 003.1 o 003.2 incompleta');
    END IF;

    DECLARE
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
        validar_constraint('PK_CAT_OBJETIVO_PEI_VERSION', 'CAT_OBJETIVO_PEI_VERSION', 'P');
        validar_constraint('UK_OPV_CODIGO', 'CAT_OBJETIVO_PEI_VERSION', 'U');
        validar_constraint('FK_OPV_VERSION_ANTERIOR', 'CAT_OBJETIVO_PEI_VERSION', 'R');
        validar_constraint('FK_OPV_DOCUMENTO', 'CAT_OBJETIVO_PEI_VERSION', 'R');
        validar_constraint('CK_OPV_VIGENCIA', 'CAT_OBJETIVO_PEI_VERSION', 'C');
        validar_constraint('CK_OPV_ACTIVA', 'CAT_OBJETIVO_PEI_VERSION', 'C');
        validar_constraint('PK_CAT_OBJETIVO_PEI', 'CAT_OBJETIVO_PEI', 'P');
        validar_constraint('UK_OP_VERSION_CODIGO', 'CAT_OBJETIVO_PEI', 'U');
        validar_constraint('FK_OP_VERSION', 'CAT_OBJETIVO_PEI', 'R');
        validar_constraint('CK_OP_VIGENCIA', 'CAT_OBJETIVO_PEI', 'C');
        validar_constraint('CK_OP_ACTIVO', 'CAT_OBJETIVO_PEI', 'C');

        IF v_incidencias IS NOT NULL THEN
            RAISE_APPLICATION_ERROR(-20075,
                'Precondicion 005.1: constraints requeridos invalidos: ' || v_incidencias);
        END IF;
    END;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS c
      JOIN USER_INDEXES i
        ON i.INDEX_NAME = c.INDEX_NAME
     WHERE c.TABLE_NAME = 'CAT_OBJETIVO_PEI'
       AND c.CONSTRAINT_NAME = 'UK_OP_VERSION_CODIGO'
       AND c.CONSTRAINT_TYPE = 'U'
       AND c.STATUS = 'ENABLED'
       AND i.STATUS = 'VALID'
       AND i.UNIQUENESS = 'UNIQUE';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20076,
            'Precondicion 005.1: indice de respaldo de UK_OP_VERSION_CODIGO ausente, invalido o no unico');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM (
            SELECT c.INDEX_NAME
              FROM USER_CONSTRAINTS c
              JOIN USER_IND_COLUMNS ic
                ON ic.INDEX_NAME = c.INDEX_NAME
             WHERE c.TABLE_NAME = 'CAT_OBJETIVO_PEI'
               AND c.CONSTRAINT_NAME = 'UK_OP_VERSION_CODIGO'
               AND c.CONSTRAINT_TYPE = 'U'
             GROUP BY c.INDEX_NAME
            HAVING COUNT(*) = 2
               AND SUM(CASE WHEN ic.COLUMN_NAME = 'ID_VERSION'
                             AND ic.COLUMN_POSITION = 1 THEN 1 ELSE 0 END) = 1
               AND SUM(CASE WHEN ic.COLUMN_NAME = 'CODIGO'
                             AND ic.COLUMN_POSITION = 2 THEN 1 ELSE 0 END) = 1
           );
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20077,
            'Precondicion 005.1: indice de respaldo de UK_OP_VERSION_CODIGO no conserva ID_VERSION, CODIGO en ese orden');
    END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3) Incompatibilidades: el indice redundante no debe existir; 006 y 009-017
--    no pueden haberse creado antes de confirmar esta correccion.
-- ----------------------------------------------------------------------------
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_INDEXES
     WHERE INDEX_NAME = 'IDX_OP_VERSION_CODIGO';
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20078,
            'Precondicion 005.1: existe IDX_OP_VERSION_CODIGO redundante; requiere revision humana');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
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
            'REPORTE_DESTINATARIO','REPORTE_REMISION'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20079,
            'Precondicion 005.1: existen objetos de 006 o 009-017; requiere revision humana');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_ACTIVIDAD_POI_VERSION','SEQ_ACTIVIDAD_POI',
            'SEQ_INICIATIVA_PROYECTO','SEQ_PROYECTO_RESPONSABLE',
            'SEQ_PARTICIPANTE_PERSONA','SEQ_PROY_PART_PERSONA','SEQ_PROY_PART_UNIDAD',
            'SEQ_PROY_CAMPO_CLASIF','SEQ_PROY_CAMPO_CLASIF_HIST',
            'SEQ_EVALUACION_INICIATIVA','SEQ_SUBSANACION_INICIATIVA',
            'SEQ_APLICABILIDAD_INICIATIVA','SEQ_APLICABILIDAD_CRITERIO',
            'SEQ_PLANIFICACION_PROYECTO','SEQ_CICLO_PROYECTO','SEQ_CICLO_EVIDENCIA',
            'SEQ_PRODUCTO_PARCIAL','SEQ_PRESENTACION_PRODUCTO_FINAL',
            'SEQ_VALIDACION_RESULTADO','SEQ_CIERRE_PROYECTO',
            'SEQ_INCORPORACION_REGISTRO','SEQ_INCORPORACION_CAMBIO',
            'SEQ_INCORPORACION_CONFLICTO',
            'SEQ_REPORTE_INSTITUCIONAL','SEQ_REPORTE_SNAPSHOT','SEQ_REPORTE_ARCHIVO',
            'SEQ_REPORTE_APROBACION','SEQ_REPORTE_DESTINATARIO','SEQ_REPORTE_REMISION'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20079,
            'Precondicion 005.1: existen secuencias de 006 o 010-017; requiere revision humana');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COLUMNS
     WHERE TABLE_NAME = 'PROYECTO'
       AND COLUMN_NAME IN (
            'CODIGO_PREFIJO','DETALLE_FUENTE','PROBLEMA_PUBLICO',
            'SOLUCION_PROPUESTA','COMPONENTE_DIGITAL',
            'DETALLE_COMPONENTE_DIGITAL','NOTA',
            'OBJETIVO_PEI_ID','ACTIVIDAD_POI_ID','VERSION','SUBSANACION_ACTIVA'
           );
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20080,
            'Precondicion 005.1: existen columnas de 009 en PROYECTO; requiere revision humana');
    END IF;

    -- La marca se escribe solo una vez. Un comentario previo, incluido el de
    -- esta correccion, impide sobrescribir metadatos no documentados.
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COMMENTS
     WHERE TABLE_NAME = 'CAT_OBJETIVO_PEI'
       AND COMMENTS IS NOT NULL;
    IF v_total <> 0 THEN
        RAISE_APPLICATION_ERROR(-20081,
            'Precondicion 005.1: CAT_OBJETIVO_PEI ya tiene comentario; 005.1 fue aplicada o requiere revision humana');
    END IF;
END;
/

PROMPT [005.1] Huella parcial validada. Registrando marcador tecnico de ejecucion unica...

-- El comentario es metadato tecnico no funcional y actua como marcador
-- estructural minimo de ejecucion. Oracle confirma implicitamente este DDL.
COMMENT ON TABLE CAT_OBJETIVO_PEI IS 'PIIP 005.1: correccion ORA-01408 confirmada';

PROMPT [005.1] Validando marcador tecnico y huella corregida...
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TAB_COMMENTS
     WHERE TABLE_NAME = 'CAT_OBJETIVO_PEI'
       AND COMMENTS = 'PIIP 005.1: correccion ORA-01408 confirmada';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20082,
            'Validacion 005.1: marcador tecnico de ejecucion unica ausente');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_CONSTRAINTS c
      JOIN USER_INDEXES i
        ON i.INDEX_NAME = c.INDEX_NAME
     WHERE c.TABLE_NAME = 'CAT_OBJETIVO_PEI'
       AND c.CONSTRAINT_NAME = 'UK_OP_VERSION_CODIGO'
       AND c.CONSTRAINT_TYPE = 'U'
       AND c.STATUS = 'ENABLED'
       AND i.STATUS = 'VALID'
       AND i.UNIQUENESS = 'UNIQUE';
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20083,
            'Validacion 005.1: indice canonico de UK_OP_VERSION_CODIGO no permanece valido y unico');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM (
            SELECT c.INDEX_NAME
              FROM USER_CONSTRAINTS c
              JOIN USER_IND_COLUMNS ic
                ON ic.INDEX_NAME = c.INDEX_NAME
             WHERE c.TABLE_NAME = 'CAT_OBJETIVO_PEI'
               AND c.CONSTRAINT_NAME = 'UK_OP_VERSION_CODIGO'
               AND c.CONSTRAINT_TYPE = 'U'
             GROUP BY c.INDEX_NAME
            HAVING COUNT(*) = 2
               AND SUM(CASE WHEN ic.COLUMN_NAME = 'ID_VERSION'
                             AND ic.COLUMN_POSITION = 1 THEN 1 ELSE 0 END) = 1
               AND SUM(CASE WHEN ic.COLUMN_NAME = 'CODIGO'
                             AND ic.COLUMN_POSITION = 2 THEN 1 ELSE 0 END) = 1
           );
    IF v_total <> 1 THEN
        RAISE_APPLICATION_ERROR(-20084,
            'Validacion 005.1: indice canonico de UK_OP_VERSION_CODIGO cambio su orden de columnas');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: correccion forward-only 005.1 aplicada correctamente.');
END;
/

-- El COMMIT final es documental; COMMENT ON TABLE confirma implicitamente.
COMMIT;

PROMPT Correccion 005.1_objetivo_pei_versionado_indice completada correctamente.
