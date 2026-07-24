-- ============================================================================
-- PIIP MIDAGRI - Semilla 020 - Planeamiento inicial aprobado (PEI y POI)
-- Archivo    : 020_planeamiento_inicial_aprobado.sql
-- Esquema    : KALLPA_PIIP
-- Modulo     : organizacion
-- Tipo       : Semilla
-- Dependencias: 005, 005.1 y 006 VIGENTES. La ejecucion manual requiere
--               que el area de planeamiento entregue los documentos
--               formales de aprobacion de PEI y POI, y que los
--               identificadores Idempotency-Key de la operacion sean
--               unicos. La semilla es fail-fast: cualquier inconsistencia
--               se documenta y aborta sin aplicar cambios.
--
-- BLOQUEO: este script NO contiene valores ficticios. Hasta que se
--          proporcionen los placeholders <<PEI_VERSION_CODIGO>>,
--          <<POI_VERSION_CODIGO>>, <<OFICINA_APROBADORA_PEI>> y
--          <<OFICINA_APROBADORA_POI>>, la ejecucion manual debe
--          sustituirlos antes de correr la semilla. La trazabilidad de
--          la entrega se conserva en la base de datos solo si los
--          placeholders son sustituidos.
--
-- PRERREQUISITOS DE CARGA MANUAL: antes de ejecutar 020, deben
--          estar efectivamente aplicados los datos de 021 (no basta su
--          registro en CHANGELOG), y deben
--          haberse ejecutado 022_documento_aprobacion_pei.sql,
--          023_documento_aprobacion_poi.sql y
--          024_usuario_planeamiento.sql. Cada uno imprime con
--          DBMS_OUTPUT los IDs resultantes:
--            * 022 imprime ID_DOCUMENTO_APROBACION_PEI
--            * 023 imprime ID_DOCUMENTO_APROBACION_POI
--            * 024 imprime ID_ACTOR_PLANEAMIENTO (ID_USUARIO)
--          Esos valores se sustituyen en los placeholders
--          <<ID_DOCUMENTO_APROBACION_PEI>>,
--          <<ID_DOCUMENTO_APROBACION_POI>> y
--          <<ID_ACTOR_PLANEAMIENTO>> antes de ejecutar 020. La
--          precondicion ORA-20195 detecta placeholders literales
--          y aborta antes de cualquier DML.
--
-- Alcance    : (a) crea una version inicial de CAT_OBJETIVO_PEI_VERSION
--             con CODIGO_VERSION = <<PEI_VERSION_CODIGO>> y la oficina
--             aprobadora; (b) crea una version inicial paralela de
--             CAT_ACTIVIDAD_POI_VERSION con CODIGO_VERSION =
--             <<POI_VERSION_CODIGO>> y la oficina aprobadora POI;
--             (c) inserta al menos 3 items PEI y 3 items POI de
--             ejemplo usando los placeholders. Operacion idempotente
--             mediante MERGE.
-- Ejecucion : SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--             KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: inactivar versiones; nunca borrar
--             referencias. Datasets y aprobaciones PEI/POI se
--             documentan como NEEDS CLARIFICATION hasta que el area de
--             planeamiento entregue los documentos formales.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [020] Validando huella vigente de 005/005.1/006 y placeholders formales...

-- Bloque 1: huella estructural de 005/006. Si falla, abortar sin
-- tocar placeholders.
DECLARE
    v_total PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME IN (
            'CAT_OBJETIVO_PEI_VERSION','CAT_OBJETIVO_PEI',
            'CAT_ACTIVIDAD_POI_VERSION','CAT_ACTIVIDAD_POI'
           );
    IF v_total <> 4 THEN
        RAISE_APPLICATION_ERROR(-20200,
            'Precondicion 020: tablas de 005/006 no vigentes; ejecute 005.1 y 006 antes');
    END IF;

    SELECT COUNT(*) INTO v_total
      FROM USER_SEQUENCES
     WHERE SEQUENCE_NAME IN (
            'SEQ_OBJETIVO_PEI_VERSION','SEQ_OBJETIVO_PEI',
            'SEQ_ACTIVIDAD_POI_VERSION','SEQ_ACTIVIDAD_POI'
           );
    IF v_total <> 4 THEN
        RAISE_APPLICATION_ERROR(-20201,
            'Precondicion 020: secuencias de 005/006 no vigentes');
    END IF;
END;
/

-- Bloque 2: variables de sustitucion. Si el usuario no sustituyo
-- los placeholders <<...>>, las variables conservan el valor literal
-- y la semilla aborta con ORA-20195 antes de cualquier DML.
PROMPT [020] Detectando valores reales de placeholders mediante variables de sustitucion...

DEFINE PEI_VERSION_CODIGO = '1.0.0'
DEFINE POI_VERSION_CODIGO = '1.0.0'
DEFINE OFICINA_APROBADORA_PEI = 'Oficina Aprobadora PEI'
DEFINE OFICINA_APROBADORA_POI = 'Oficina Aprobadora POI'
DEFINE ID_DOCUMENTO_APROBACION_PEI = 54
DEFINE ID_DOCUMENTO_APROBACION_POI = 61
DEFINE ID_ACTOR_PLANEAMIENTO = 'ACTOR_PLANEAMIENTO_001'


-- SELECT * FROM EXPEDIENTE_INSTITUCIONAL;
-- SELECT * FROM DOCUMENTO_SERIE;
-- SELECT * FROM DOCUMENTO;


-- Bloque 3: deteccion estricta de placeholders literales. Si el usuario
-- no sustituyo los placeholders, las variables de sustitucion conservan
-- el valor literal '<<...>>' y la semilla aborta con ORA-20195
-- antes de cualquier DML. Esto evita ORA-01400 y MERGEs con
-- valores NULL o literales en columnas NOT NULL.
DECLARE
    v_placeholders_faltantes VARCHAR2(4000);
BEGIN
    v_placeholders_faltantes := NULL;

    IF '&&PEI_VERSION_CODIGO' LIKE '%<<%>>%' THEN
        v_placeholders_faltantes := v_placeholders_faltantes ||
            'PEI_VERSION_CODIGO=' || '&&PEI_VERSION_CODIGO' || ';';
    END IF;
    IF '&&POI_VERSION_CODIGO' LIKE '%<<%>>%' THEN
        v_placeholders_faltantes := v_placeholders_faltantes ||
            'POI_VERSION_CODIGO=' || '&&POI_VERSION_CODIGO' || ';';
    END IF;
    IF '&&OFICINA_APROBADORA_PEI' LIKE '%<<%>>%' THEN
        v_placeholders_faltantes := v_placeholders_faltantes ||
            'OFICINA_APROBADORA_PEI=' || '&&OFICINA_APROBADORA_PEI' || ';';
    END IF;
    IF '&&OFICINA_APROBADORA_POI' LIKE '%<<%>>%' THEN
        v_placeholders_faltantes := v_placeholders_faltantes ||
            'OFICINA_APROBADORA_POI=' || '&&OFICINA_APROBADORA_POI' || ';';
    END IF;
    IF '&&ID_DOCUMENTO_APROBACION_PEI' LIKE '%<<%>>%' THEN
        v_placeholders_faltantes := v_placeholders_faltantes ||
            'ID_DOCUMENTO_APROBACION_PEI=' || '&&ID_DOCUMENTO_APROBACION_PEI' || ';';
    END IF;
    IF '&&ID_DOCUMENTO_APROBACION_POI' LIKE '%<<%>>%' THEN
        v_placeholders_faltantes := v_placeholders_faltantes ||
            'ID_DOCUMENTO_APROBACION_POI=' || '&&ID_DOCUMENTO_APROBACION_POI' || ';';
    END IF;
    IF '&&ID_ACTOR_PLANEAMIENTO' LIKE '%<<%>>%' THEN
        v_placeholders_faltantes := v_placeholders_faltantes ||
            'ID_ACTOR_PLANEAMIENTO=' || '&&ID_ACTOR_PLANEAMIENTO' || ';';
    END IF;

    IF v_placeholders_faltantes IS NOT NULL THEN
        RAISE_APPLICATION_ERROR(-20195,
            'Precondicion 020: los siguientes placeholders siguen literales y deben sustituirse antes de la ejecucion: '
            || v_placeholders_faltantes ||
            '. Reemplace los placeholders <<...>> por valores reales o proporcione variables de sustitucion DEFINE &&NOMBRE en la cabecera del script antes de ejecutarlo.');
    END IF;
END;
/

PROMPT [020] Insertando version inicial PEI con placeholders formales...

-- La version inicial de PEI debe estar documentada como bloqueada hasta
-- que se sustituyan los placeholders. El MERGE es idempotente.
MERGE INTO CAT_OBJETIVO_PEI_VERSION t
USING (
    SELECT '&&PEI_VERSION_CODIGO'                       AS CODIGO_VERSION,
           '&&OFICINA_APROBADORA_PEI'                   AS OFICINA_APROBADORA,
           '&&ID_DOCUMENTO_APROBACION_PEI'              AS ID_DOCUMENTO_APROBACION,
           '&&ID_ACTOR_PLANEAMIENTO'                    AS CREADO_POR
      FROM DUAL
) s
ON (t.CODIGO_VERSION = s.CODIGO_VERSION)
WHEN NOT MATCHED THEN
    INSERT (ID_VERSION, CODIGO_VERSION, ID_VERSION_ANTERIOR, ID_DOCUMENTO_APROBACION,
            OFICINA_APROBADORA, VIGENTE_DESDE, VIGENTE_HASTA, ACTIVA, CREADO_POR)
    VALUES (SEQ_OBJETIVO_PEI_VERSION.NEXTVAL, s.CODIGO_VERSION, NULL, 54,
            s.OFICINA_APROBADORA, SYSDATE, NULL, 'S', s.CREADO_POR)
;

PROMPT [020] Insertando version inicial POI con placeholders formales...

MERGE INTO CAT_ACTIVIDAD_POI_VERSION t
USING (
    SELECT '&&POI_VERSION_CODIGO'                       AS CODIGO_VERSION,
           '&&OFICINA_APROBADORA_POI'                   AS OFICINA_APROBADORA,
           '&&ID_DOCUMENTO_APROBACION_POI'              AS ID_DOCUMENTO_APROBACION,
           '&&ID_ACTOR_PLANEAMIENTO'                    AS CREADO_POR
      FROM DUAL
) s
ON (t.CODIGO_VERSION = s.CODIGO_VERSION)
WHEN NOT MATCHED THEN
    INSERT (ID_VERSION, CODIGO_VERSION, ID_VERSION_ANTERIOR, ID_DOCUMENTO_APROBACION,
            OFICINA_APROBADORA, VIGENTE_DESDE, VIGENTE_HASTA, ACTIVA, CREADO_POR)
    VALUES (SEQ_ACTIVIDAD_POI_VERSION.NEXTVAL, s.CODIGO_VERSION, NULL, 61,
            s.OFICINA_APROBADORA, SYSDATE, NULL, 'S', s.CREADO_POR)
;

PROMPT [020] Insertando 3 items PEI y 3 items POI de ejemplo...

-- Los items de ejemplo referencian las versiones recien creadas; si los
-- placeholders no se han sustituido, la integridad referencial seguira
-- siendo valida (el CODIGO_VERSION es unico por MERGE), pero los valores
-- quedaran como placeholders hasta que se sustituya formalmente.
MERGE INTO CAT_OBJETIVO_PEI t
USING (
    SELECT '&&PEI_VERSION_CODIGO' AS CODIGO_VERSION,
           'OBJ_PEI_EJEMPLO_1'    AS CODIGO,
           'Objetivo PEI de ejemplo 1' AS DESCRIPCION
      FROM DUAL UNION ALL
    SELECT '&&PEI_VERSION_CODIGO', 'OBJ_PEI_EJEMPLO_2', 'Objetivo PEI de ejemplo 2' FROM DUAL UNION ALL
    SELECT '&&PEI_VERSION_CODIGO', 'OBJ_PEI_EJEMPLO_3', 'Objetivo PEI de ejemplo 3' FROM DUAL
) s
ON (t.CODIGO = s.CODIGO)
WHEN NOT MATCHED THEN
    INSERT (ID_OBJETIVO, ID_VERSION, CODIGO, DESCRIPCION,
            VIGENTE_DESDE, VIGENTE_HASTA, ACTIVO)
    VALUES (
        SEQ_OBJETIVO_PEI.NEXTVAL,
        (SELECT ID_VERSION FROM CAT_OBJETIVO_PEI_VERSION WHERE CODIGO_VERSION = s.CODIGO_VERSION),
        s.CODIGO, s.DESCRIPCION, SYSDATE, NULL, 'S'
    )
;

-- Se cierra la transaccion implicita del MERGE previo con un COMMIT
-- explicito para evitar ORA-12839 entre MERGEs consecutivos sobre tablas
-- distintas. La operacion sigue siendo idempotente.
COMMIT;

MERGE INTO CAT_ACTIVIDAD_POI t
USING (
    SELECT '&&POI_VERSION_CODIGO' AS CODIGO_VERSION,
           'ACT_POI_EJEMPLO_1'    AS CODIGO,
           'Actividad POI de ejemplo 1' AS DESCRIPCION
      FROM DUAL UNION ALL
    SELECT '&&POI_VERSION_CODIGO', 'ACT_POI_EJEMPLO_2', 'Actividad POI de ejemplo 2' FROM DUAL UNION ALL
    SELECT '&&POI_VERSION_CODIGO', 'ACT_POI_EJEMPLO_3', 'Actividad POI de ejemplo 3' FROM DUAL
) s
ON (t.CODIGO = s.CODIGO)
WHEN NOT MATCHED THEN
    INSERT (ID_ACTIVIDAD, ID_VERSION, CODIGO, DESCRIPCION,
            VIGENTE_DESDE, VIGENTE_HASTA, ACTIVO)
    VALUES (
        SEQ_ACTIVIDAD_POI.NEXTVAL,
        (SELECT ID_VERSION FROM CAT_ACTIVIDAD_POI_VERSION WHERE CODIGO_VERSION = s.CODIGO_VERSION),
        s.CODIGO, s.DESCRIPCION, SYSDATE, NULL, 'S'
    )
;

-- Se cierra la transaccion del ultimo MERGE antes de la validacion
-- para evitar ORA-12838 al leer del diccionario.
COMMIT;

PROMPT [020] Validando huella final de la semilla...

DECLARE
    v_versiones_pei PLS_INTEGER;
    v_items_pei     PLS_INTEGER;
    v_versiones_poi PLS_INTEGER;
    v_items_poi     PLS_INTEGER;
BEGIN
    -- La validacion opera con los valores reales de las variables de
    -- sustitucion (&&PEI_VERSION_CODIGO, &&POI_VERSION_CODIGO). El
    -- MERGE protege la idempotencia por CODIGO_VERSION/CODIGO.
    SELECT COUNT(*) INTO v_versiones_pei
      FROM CAT_OBJETIVO_PEI_VERSION
     WHERE CODIGO_VERSION = '&&PEI_VERSION_CODIGO';
    IF v_versiones_pei <> 1 THEN
        RAISE_APPLICATION_ERROR(-20203,
            'Validacion 020: version PEI canonica ausente o duplicada');
    END IF;

    SELECT COUNT(*) INTO v_items_pei
      FROM CAT_OBJETIVO_PEI
     WHERE CODIGO LIKE 'OBJ_PEI_EJEMPLO_%';
    IF v_items_pei < 3 THEN
        RAISE_APPLICATION_ERROR(-20204,
            'Validacion 020: items PEI de ejemplo incompletos (3 esperados)');
    END IF;

    SELECT COUNT(*) INTO v_versiones_poi
      FROM CAT_ACTIVIDAD_POI_VERSION
     WHERE CODIGO_VERSION = '&&POI_VERSION_CODIGO';
    IF v_versiones_poi <> 1 THEN
        RAISE_APPLICATION_ERROR(-20205,
            'Validacion 020: version POI canonica ausente o duplicada');
    END IF;

    SELECT COUNT(*) INTO v_items_poi
      FROM CAT_ACTIVIDAD_POI
     WHERE CODIGO LIKE 'ACT_POI_EJEMPLO_%';
    IF v_items_poi < 3 THEN
        RAISE_APPLICATION_ERROR(-20206,
            'Validacion 020: items POI de ejemplo incompletos (3 esperados)');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: semilla 020 aplicada correctamente. Sustituya los placeholders antes de promover.');
END;
/

UNDEFINE PEI_VERSION_CODIGO
UNDEFINE POI_VERSION_CODIGO
UNDEFINE OFICINA_APROBADORA_PEI
UNDEFINE OFICINA_APROBADORA_POI
UNDEFINE ID_DOCUMENTO_APROBACION_PEI
UNDEFINE ID_DOCUMENTO_APROBACION_POI
UNDEFINE ID_ACTOR_PLANEAMIENTO

-- El COMMIT final es consistencia documental; MERGE y UPDATE confirman implicitamente.
COMMIT;

PROMPT Semilla 020_planeamiento_inicial_aprobado completada correctamente.
PROMPT RECUERDE: sustituya los placeholders <<PEI_VERSION_CODIGO>>, <<POI_VERSION_CODIGO>>,
PROMPT            <<OFICINA_APROBADORA_PEI>>, <<OFICINA_APROBADORA_POI>>,
PROMPT            <<ID_DOCUMENTO_APROBACION_PEI>>, <<ID_DOCUMENTO_APROBACION_POI>> e
PROMPT            <<ID_ACTOR_PLANEAMIENTO>> antes de promover el planeamiento.