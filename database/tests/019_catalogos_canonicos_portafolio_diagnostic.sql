-- ============================================================================
-- PIIP MIDAGRI - Diagnostico de solo lectura 019 - Huella 001-017 y estado
-- de filas semilla de portafolio
-- Archivo    : 019_catalogos_canonicos_portafolio_diagnostic.sql
-- Esquema    : KALLPA_PIIP
-- Modulo     : transversal
-- Proposito  : antes de ejecutar manualmente 019, verificar la huella vigente
--              de tablas 001-017 que exige la semilla, listar las tablas
--              realmente presentes en USER_TABLES y comparar contra la lista
--              de 019. Ademas, mostrar el estado real de las filas semilla
--              (TIPO_DOCUMENTO, ROL, TRANSICION_PERMITIDA, MATRIZ_*,
--              USUARIO, USUARIO_ROL_UNIDAD) para diagnosticar si 019 ya fue
--              aplicada o si la huella de 021 esta cargada.
--
-- ADVERTENCIA: este script es exclusivamente de solo lectura. No contiene DDL,
-- DML, COMMIT, ROLLBACK, SAVEPOINT ni cambios persistentes de sesion.
-- No ejecutar contra ambientes compartidos sin autorizacion humana.
-- Ejecucion  : manual por DBA autorizado, con salida DBMS_OUTPUT habilitada.
-- ============================================================================

SET SERVEROUTPUT ON SIZE UNLIMITED
SET FEEDBACK ON
SET VERIFY OFF

PROMPT [019-diagnostic] Listando las tablas esperadas por la precondicion de 019 (55 esperadas)...

DECLARE
    v_esperadas_total PLS_INTEGER := 0;
    v_encontradas_total PLS_INTEGER := 0;
    v_faltantes VARCHAR2(4000);
BEGIN
    FOR r IN (
        WITH esperados (NOMBRE_ESPERADO) AS (
            SELECT 'UNIDAD_EJECUTORA' FROM DUAL UNION ALL
            SELECT 'USUARIO' FROM DUAL UNION ALL
            SELECT 'ROL' FROM DUAL UNION ALL
            SELECT 'USUARIO_ROL_UNIDAD' FROM DUAL UNION ALL
            SELECT 'PROYECTO' FROM DUAL UNION ALL
            SELECT 'PROYECTO_UNIDAD_ORGANICA' FROM DUAL UNION ALL
            SELECT 'TRANSICION_PERMITIDA' FROM DUAL UNION ALL
            SELECT 'TIPO_DOCUMENTO' FROM DUAL UNION ALL
            SELECT 'DOCUMENTO' FROM DUAL UNION ALL
            SELECT 'TRANSICION_ESTADO' FROM DUAL UNION ALL
            SELECT 'SECUENCIA_CODIGO' FROM DUAL UNION ALL
            SELECT 'AUDITORIA_ACCESO' FROM DUAL UNION ALL
            SELECT 'AUDITORIA_EVENTO' FROM DUAL UNION ALL
            SELECT 'SOLICITUD_IDEMPOTENTE' FROM DUAL UNION ALL
            SELECT 'EXPEDIENTE_INSTITUCIONAL' FROM DUAL UNION ALL
            SELECT 'DOCUMENTO_SERIE' FROM DUAL UNION ALL
            SELECT 'CAT_OBJETIVO_PEI_VERSION' FROM DUAL UNION ALL
            SELECT 'CAT_OBJETIVO_PEI' FROM DUAL UNION ALL
            SELECT 'CAT_ACTIVIDAD_POI_VERSION' FROM DUAL UNION ALL
            SELECT 'CAT_ACTIVIDAD_POI' FROM DUAL UNION ALL
            SELECT 'MATRIZ_FUNCIONAL_VERSION' FROM DUAL UNION ALL
            SELECT 'MATRIZ_FUNCION' FROM DUAL UNION ALL
            SELECT 'MATRIZ_FUNCION_PERFIL_UNIDAD' FROM DUAL UNION ALL
            SELECT 'USUARIO_ROL_UNIDAD_EVENTO' FROM DUAL UNION ALL
            SELECT 'SUPLENCIA_FUNCIONAL' FROM DUAL UNION ALL
            SELECT 'OPERACION_APROVISIONAMIENTO' FROM DUAL UNION ALL
            SELECT 'DOCUMENTO_CLASIFICACION_HIST' FROM DUAL UNION ALL
            SELECT 'DOCUMENTO_PUBLICACION' FROM DUAL UNION ALL
            SELECT 'INICIATIVA_PROYECTO' FROM DUAL UNION ALL
            SELECT 'PROYECTO_RESPONSABLE' FROM DUAL UNION ALL
            SELECT 'PARTICIPANTE_PERSONA' FROM DUAL UNION ALL
            SELECT 'PROYECTO_PARTICIPANTE_PERSONA' FROM DUAL UNION ALL
            SELECT 'PROYECTO_PARTICIPANTE_UNIDAD' FROM DUAL UNION ALL
            SELECT 'PROYECTO_CAMPO_CLASIFICACION' FROM DUAL UNION ALL
            SELECT 'PROYECTO_CAMPO_CLASIF_HIST' FROM DUAL UNION ALL
            SELECT 'EVALUACION_INICIATIVA' FROM DUAL UNION ALL
            SELECT 'SUBSANACION_INICIATIVA' FROM DUAL UNION ALL
            SELECT 'APLICABILIDAD_INICIATIVA' FROM DUAL UNION ALL
            SELECT 'APLICABILIDAD_CRITERIO' FROM DUAL UNION ALL
            SELECT 'PLANIFICACION_PROYECTO' FROM DUAL UNION ALL
            SELECT 'CICLO_PROYECTO' FROM DUAL UNION ALL
            SELECT 'CICLO_EVIDENCIA' FROM DUAL UNION ALL
            SELECT 'PRODUCTO_PARCIAL' FROM DUAL UNION ALL
            SELECT 'PRESENTACION_PRODUCTO_FINAL' FROM DUAL UNION ALL
            SELECT 'VALIDACION_RESULTADO' FROM DUAL UNION ALL
            SELECT 'CIERRE_PROYECTO' FROM DUAL UNION ALL
            SELECT 'INCORPORACION_REGISTRO' FROM DUAL UNION ALL
            SELECT 'INCORPORACION_CAMBIO' FROM DUAL UNION ALL
            SELECT 'INCORPORACION_CONFLICTO' FROM DUAL UNION ALL
            SELECT 'REPORTE_INSTITUCIONAL' FROM DUAL UNION ALL
            SELECT 'REPORTE_SNAPSHOT' FROM DUAL UNION ALL
            SELECT 'REPORTE_ARCHIVO' FROM DUAL UNION ALL
            SELECT 'REPORTE_APROBACION' FROM DUAL UNION ALL
            SELECT 'REPORTE_DESTINATARIO' FROM DUAL UNION ALL
            SELECT 'REPORTE_REMISION' FROM DUAL
        )
        SELECT e.NOMBRE_ESPERADO,
               CASE WHEN EXISTS (
                        SELECT 1 FROM USER_TABLES ut
                         WHERE ut.TABLE_NAME = e.NOMBRE_ESPERADO
                   ) THEN 'S' ELSE 'N' END AS VIGENTE
          FROM esperados e
    ) LOOP
        v_esperadas_total := v_esperadas_total + 1;
        IF r.VIGENTE = 'S' THEN
            v_encontradas_total := v_encontradas_total + 1;
        ELSE
            v_faltantes := v_faltantes || r.NOMBRE_ESPERADO || ';';
        END IF;
        DBMS_OUTPUT.PUT_LINE(
            'TABLA_ESPERADA=' || r.NOMBRE_ESPERADO ||
            ';VIGENTE=' || r.VIGENTE
        );
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('TOTAL_ESPERADAS=' || TO_CHAR(v_esperadas_total) || ' (esperado 55)');
    DBMS_OUTPUT.PUT_LINE('TOTAL_ENCONTRADAS=' || TO_CHAR(v_encontradas_total));
    DBMS_OUTPUT.PUT_LINE('FALTANTES_DETALLE=' || NVL(v_faltantes, '<NINGUNA>'));
END;
/

PROMPT [019-diagnostic] Conteo total de USER_TABLES excluyendo objetos Oracle internos...

DECLARE
    v_total        PLS_INTEGER;
    v_en_esperadas PLS_INTEGER;
    v_no_esperadas PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total
      FROM USER_TABLES
     WHERE TABLE_NAME NOT LIKE 'BIN$%'
       AND TABLE_NAME NOT LIKE 'DR$%'
       AND TABLE_NAME NOT LIKE 'SYS_%'
       AND TABLE_NAME NOT LIKE 'X$%'
       AND TABLE_NAME NOT LIKE 'OGG$%'
       AND TABLE_NAME NOT LIKE 'AQ$%'
       AND TABLE_NAME NOT LIKE 'LOGMNR%'
       AND TABLE_NAME NOT LIKE 'REDO_%'
       AND TABLE_NAME NOT LIKE 'SQLPLUS%'
       AND TABLE_NAME NOT LIKE 'APEX%'
       AND TABLE_NAME NOT LIKE 'WRI$_%'
       AND TABLE_NAME NOT LIKE 'F$%'
       AND TABLE_NAME NOT LIKE 'KU$%'
       AND TABLE_NAME NOT LIKE 'CWM$%'
       AND TABLE_NAME NOT LIKE 'CWM2$%'
       AND TABLE_NAME NOT LIKE 'ODM$%';

    SELECT COUNT(*) INTO v_en_esperadas
      FROM USER_TABLES ut
     WHERE ut.TABLE_NAME NOT LIKE 'BIN$%'
       AND ut.TABLE_NAME NOT LIKE 'DR$%'
       AND ut.TABLE_NAME NOT LIKE 'SYS_%'
       AND ut.TABLE_NAME IN (
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
            'PARTICIPANTE_PERSONA','PROYECTO_PARTICIPANTE_PERSONA',
            'PROYECTO_PARTICIPANTE_UNIDAD',
            'PROYECTO_CAMPO_CLASIFICACION','PROYECTO_CAMPO_CLASIF_HIST',
            'EVALUACION_INICIATIVA','SUBSANACION_INICIATIVA',
            'APLICABILIDAD_INICIATIVA','APLICABILIDAD_CRITERIO',
            'PLANIFICACION_PROYECTO','CICLO_PROYECTO','CICLO_EVIDENCIA',
            'PRODUCTO_PARCIAL','PRESENTACION_PRODUCTO_FINAL',
            'VALIDACION_RESULTADO','CIERRE_PROYECTO',
            'INCORPORACION_REGISTRO','INCORPORACION_CAMBIO',
            'INCORPORACION_CONFLICTO',
            'REPORTE_INSTITUCIONAL','REPORTE_SNAPSHOT','REPORTE_ARCHIVO',
            'REPORTE_APROBACION','REPORTE_DESTINATARIO','REPORTE_REMISION'
           );

    v_no_esperadas := v_total - v_en_esperadas;
    DBMS_OUTPUT.PUT_LINE('TOTAL_USER_TABLES_UTILES=' || TO_CHAR(v_total));
    DBMS_OUTPUT.PUT_LINE('TOTAL_EN_ESPERADAS_019=' || TO_CHAR(v_en_esperadas) || ' (esperado 55)');
    DBMS_OUTPUT.PUT_LINE('TOTAL_NO_ESPERADAS=' || TO_CHAR(v_no_esperadas));
END;
/

PROMPT [019-diagnostic] Tablas presentes en USER_TABLES que NO estan en la lista de 019...

DECLARE
    v_total PLS_INTEGER := 0;
BEGIN
    FOR r IN (
        SELECT ut.TABLE_NAME
          FROM USER_TABLES ut
         WHERE ut.TABLE_NAME NOT LIKE 'BIN$%'
           AND ut.TABLE_NAME NOT LIKE 'DR$%'
           AND ut.TABLE_NAME NOT LIKE 'SYS_%'
           AND ut.TABLE_NAME NOT LIKE 'X$%'
           AND ut.TABLE_NAME NOT LIKE 'OGG$%'
           AND ut.TABLE_NAME NOT LIKE 'AQ$%'
           AND ut.TABLE_NAME NOT LIKE 'LOGMNR%'
           AND ut.TABLE_NAME NOT LIKE 'REDO_%'
           AND ut.TABLE_NAME NOT LIKE 'SQLPLUS%'
           AND ut.TABLE_NAME NOT LIKE 'APEX%'
           AND ut.TABLE_NAME NOT LIKE 'WRI$_%'
           AND ut.TABLE_NAME NOT LIKE 'F$%'
           AND ut.TABLE_NAME NOT LIKE 'KU$%'
           AND ut.TABLE_NAME NOT LIKE 'CWM$%'
           AND ut.TABLE_NAME NOT LIKE 'CWM2$%'
           AND ut.TABLE_NAME NOT LIKE 'ODM$%'
           AND ut.TABLE_NAME NOT IN (
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
                'PARTICIPANTE_PERSONA','PROYECTO_PARTICIPANTE_PERSONA',
                'PROYECTO_PARTICIPANTE_UNIDAD',
                'PROYECTO_CAMPO_CLASIFICACION','PROYECTO_CAMPO_CLASIF_HIST',
                'EVALUACION_INICIATIVA','SUBSANACION_INICIATIVA',
                'APLICABILIDAD_INICIATIVA','APLICABILIDAD_CRITERIO',
                'PLANIFICACION_PROYECTO','CICLO_PROYECTO','CICLO_EVIDENCIA',
                'PRODUCTO_PARCIAL','PRESENTACION_PRODUCTO_FINAL',
                'VALIDACION_RESULTADO','CIERRE_PROYECTO',
                'INCORPORACION_REGISTRO','INCORPORACION_CAMBIO',
                'INCORPORACION_CONFLICTO',
                'REPORTE_INSTITUCIONAL','REPORTE_SNAPSHOT','REPORTE_ARCHIVO',
                'REPORTE_APROBACION','REPORTE_DESTINATARIO','REPORTE_REMISION'
               )
         ORDER BY ut.TABLE_NAME
    ) LOOP
        v_total := v_total + 1;
        DBMS_OUTPUT.PUT_LINE('TABLA_NO_ESPERADA=' || r.TABLE_NAME);
    END LOOP;
    IF v_total = 0 THEN
        DBMS_OUTPUT.PUT_LINE('TABLA_NO_ESPERADA=<NINGUNA>');
    END IF;
END;
/

PROMPT [019-diagnostic] Tablas esperadas por 019 que NO estan en USER_TABLES...

DECLARE
    v_total PLS_INTEGER := 0;
BEGIN
    FOR r IN (
        SELECT e.NOMBRE_ESPERADO
          FROM (
                SELECT 'UNIDAD_EJECUTORA' AS NOMBRE_ESPERADO FROM DUAL UNION ALL
                SELECT 'USUARIO' FROM DUAL UNION ALL
                SELECT 'ROL' FROM DUAL UNION ALL
                SELECT 'USUARIO_ROL_UNIDAD' FROM DUAL UNION ALL
                SELECT 'PROYECTO' FROM DUAL UNION ALL
                SELECT 'PROYECTO_UNIDAD_ORGANICA' FROM DUAL UNION ALL
                SELECT 'TRANSICION_PERMITIDA' FROM DUAL UNION ALL
                SELECT 'TIPO_DOCUMENTO' FROM DUAL UNION ALL
                SELECT 'DOCUMENTO' FROM DUAL UNION ALL
                SELECT 'TRANSICION_ESTADO' FROM DUAL UNION ALL
                SELECT 'SECUENCIA_CODIGO' FROM DUAL UNION ALL
                SELECT 'AUDITORIA_ACCESO' FROM DUAL UNION ALL
                SELECT 'AUDITORIA_EVENTO' FROM DUAL UNION ALL
                SELECT 'SOLICITUD_IDEMPOTENTE' FROM DUAL UNION ALL
                SELECT 'EXPEDIENTE_INSTITUCIONAL' FROM DUAL UNION ALL
                SELECT 'DOCUMENTO_SERIE' FROM DUAL UNION ALL
                SELECT 'CAT_OBJETIVO_PEI_VERSION' FROM DUAL UNION ALL
                SELECT 'CAT_OBJETIVO_PEI' FROM DUAL UNION ALL
                SELECT 'CAT_ACTIVIDAD_POI_VERSION' FROM DUAL UNION ALL
                SELECT 'CAT_ACTIVIDAD_POI' FROM DUAL UNION ALL
                SELECT 'MATRIZ_FUNCIONAL_VERSION' FROM DUAL UNION ALL
                SELECT 'MATRIZ_FUNCION' FROM DUAL UNION ALL
                SELECT 'MATRIZ_FUNCION_PERFIL_UNIDAD' FROM DUAL UNION ALL
                SELECT 'USUARIO_ROL_UNIDAD_EVENTO' FROM DUAL UNION ALL
                SELECT 'SUPLENCIA_FUNCIONAL' FROM DUAL UNION ALL
                SELECT 'OPERACION_APROVISIONAMIENTO' FROM DUAL UNION ALL
                SELECT 'DOCUMENTO_CLASIFICACION_HIST' FROM DUAL UNION ALL
                SELECT 'DOCUMENTO_PUBLICACION' FROM DUAL UNION ALL
                SELECT 'INICIATIVA_PROYECTO' FROM DUAL UNION ALL
                SELECT 'PROYECTO_RESPONSABLE' FROM DUAL UNION ALL
                SELECT 'PARTICIPANTE_PERSONA' FROM DUAL UNION ALL
                SELECT 'PROYECTO_PARTICIPANTE_PERSONA' FROM DUAL UNION ALL
                SELECT 'PROYECTO_PARTICIPANTE_UNIDAD' FROM DUAL UNION ALL
                SELECT 'PROYECTO_CAMPO_CLASIFICACION' FROM DUAL UNION ALL
                SELECT 'PROYECTO_CAMPO_CLASIF_HIST' FROM DUAL UNION ALL
                SELECT 'EVALUACION_INICIATIVA' FROM DUAL UNION ALL
                SELECT 'SUBSANACION_INICIATIVA' FROM DUAL UNION ALL
                SELECT 'APLICABILIDAD_INICIATIVA' FROM DUAL UNION ALL
                SELECT 'APLICABILIDAD_CRITERIO' FROM DUAL UNION ALL
                SELECT 'PLANIFICACION_PROYECTO' FROM DUAL UNION ALL
                SELECT 'CICLO_PROYECTO' FROM DUAL UNION ALL
                SELECT 'CICLO_EVIDENCIA' FROM DUAL UNION ALL
                SELECT 'PRODUCTO_PARCIAL' FROM DUAL UNION ALL
                SELECT 'PRESENTACION_PRODUCTO_FINAL' FROM DUAL UNION ALL
                SELECT 'VALIDACION_RESULTADO' FROM DUAL UNION ALL
                SELECT 'CIERRE_PROYECTO' FROM DUAL UNION ALL
                SELECT 'INCORPORACION_REGISTRO' FROM DUAL UNION ALL
                SELECT 'INCORPORACION_CAMBIO' FROM DUAL UNION ALL
                SELECT 'INCORPORACION_CONFLICTO' FROM DUAL UNION ALL
                SELECT 'REPORTE_INSTITUCIONAL' FROM DUAL UNION ALL
                SELECT 'REPORTE_SNAPSHOT' FROM DUAL UNION ALL
                SELECT 'REPORTE_ARCHIVO' FROM DUAL UNION ALL
                SELECT 'REPORTE_APROBACION' FROM DUAL UNION ALL
                SELECT 'REPORTE_DESTINATARIO' FROM DUAL UNION ALL
                SELECT 'REPORTE_REMISION' FROM DUAL
               ) e
         WHERE NOT EXISTS (
                SELECT 1 FROM USER_TABLES ut WHERE ut.TABLE_NAME = e.NOMBRE_ESPERADO
               )
         ORDER BY e.NOMBRE_ESPERADO
    ) LOOP
        v_total := v_total + 1;
        DBMS_OUTPUT.PUT_LINE('TABLA_FALTANTE=' || r.NOMBRE_ESPERADO);
    END LOOP;
    IF v_total = 0 THEN
        DBMS_OUTPUT.PUT_LINE('TABLA_FALTANTE=<NINGUNA>');
    END IF;

    DBMS_OUTPUT.PUT_LINE('Diagnostico 019 completado; no se realizaron cambios.');
END;
/

PROMPT [019-diagnostic] Listado completo de filas de TIPO_DOCUMENTO y conteos canonico/legacy...

DECLARE
    v_total               PLS_INTEGER;
    v_canonicos_total     PLS_INTEGER;
    v_canonicos_presentes PLS_INTEGER;
    v_canonicos_ausentes  PLS_INTEGER;
    v_legacy              PLS_INTEGER;
    v_roles_total         PLS_INTEGER;
    v_roles_canonicos     PLS_INTEGER;
    v_roles_no_canonicos  PLS_INTEGER;
    v_trans_total         PLS_INTEGER;
    v_trans_inactivas     PLS_INTEGER;
    v_trans_activas       PLS_INTEGER;
    v_matriz_funcion      PLS_INTEGER;
    v_matriz_combinacion  PLS_INTEGER;
    v_usuarios_total      PLS_INTEGER;
    v_usuarios_kc         PLS_INTEGER;
    v_uru_total           PLS_INTEGER;
BEGIN
    -- Listado completo de TIPO_DOCUMENTO
    SELECT COUNT(*) INTO v_total FROM TIPO_DOCUMENTO;
    DBMS_OUTPUT.PUT_LINE('TD_TOTAL=' || TO_CHAR(v_total));

    FOR rec_td IN (
        SELECT ID_TIPO_DOC, NOMBRE, CONTEXTO, CLASIFICACION_DEFECTO,
               ESTADO_ASOCIADO, ACTIVO
          FROM TIPO_DOCUMENTO
         ORDER BY ID_TIPO_DOC
    ) LOOP
        DBMS_OUTPUT.PUT_LINE(
            'TD_ID=' || TO_CHAR(rec_td.ID_TIPO_DOC) ||
            ';NOMBRE=' || NVL(rec_td.NOMBRE, '<NULL>') ||
            ';CONTEXTO=' || NVL(rec_td.CONTEXTO, '<NULL>') ||
            ';CLAS_DEF=' || NVL(rec_td.CLASIFICACION_DEFECTO, '<NULL>') ||
            ';ESTADO=' || NVL(rec_td.ESTADO_ASOCIADO, '<NULL>') ||
            ';ACTIVO=' || NVL(rec_td.ACTIVO, '<NULL>')
        );
    END LOOP;

    -- Conteo de filas canonicamente esperadas por 019
    SELECT COUNT(*) INTO v_canonicos_presentes
      FROM TIPO_DOCUMENTO
     WHERE NOMBRE IN (
            'Ficha de Iniciativa','Informe de Opinion Tecnica',
            'Documento Formal de Decision',
            'Documento de Aprobacion o Autorizacion','Nota Conceptual',
            'Matriz de Planificacion','Seguimiento Agil',
            'Autoevaluacion de Ciclo',
            'Documento de Aprobacion de Producto',
            'Evidencia de No Aprobacion',
            'Informe Final de Cierre','Evidencia de Suspension',
            'Informe de Cancelacion'
           );
    SELECT COUNT(*) INTO v_canonicos_total
      FROM (
            SELECT 'Ficha de Iniciativa' FROM DUAL UNION ALL
            SELECT 'Informe de Opinion Tecnica' FROM DUAL UNION ALL
            SELECT 'Documento Formal de Decision' FROM DUAL UNION ALL
            SELECT 'Documento de Aprobacion o Autorizacion' FROM DUAL UNION ALL
            SELECT 'Nota Conceptual' FROM DUAL UNION ALL
            SELECT 'Matriz de Planificacion' FROM DUAL UNION ALL
            SELECT 'Seguimiento Agil' FROM DUAL UNION ALL
            SELECT 'Autoevaluacion de Ciclo' FROM DUAL UNION ALL
            SELECT 'Documento de Aprobacion de Producto' FROM DUAL UNION ALL
            SELECT 'Evidencia de No Aprobacion' FROM DUAL UNION ALL
            SELECT 'Informe Final de Cierre' FROM DUAL UNION ALL
            SELECT 'Evidencia de Suspension' FROM DUAL UNION ALL
            SELECT 'Informe de Cancelacion' FROM DUAL
           );
    v_canonicos_ausentes := v_canonicos_total - v_canonicos_presentes;
    v_legacy := v_total - v_canonicos_presentes;
    DBMS_OUTPUT.PUT_LINE('TD_CANONICOS_PRESENTES=' || TO_CHAR(v_canonicos_presentes) || '/13');
    DBMS_OUTPUT.PUT_LINE('TD_CANONICOS_AUSENTES=' || TO_CHAR(v_canonicos_ausentes));
    DBMS_OUTPUT.PUT_LINE('TD_NO_CANONICOS=' || TO_CHAR(v_legacy));
END;
/

PROMPT [019-diagnostic] Listado de filas de TIPO_DOCUMENTO con NOMBRE canonico y faltantes...

DECLARE
    v_lista VARCHAR2(4000);
BEGIN
    FOR r IN (
        WITH canonicos (NOMBRE) AS (
            SELECT 'Ficha de Iniciativa' FROM DUAL UNION ALL
            SELECT 'Informe de Opinion Tecnica' FROM DUAL UNION ALL
            SELECT 'Documento Formal de Decision' FROM DUAL UNION ALL
            SELECT 'Documento de Aprobacion o Autorizacion' FROM DUAL UNION ALL
            SELECT 'Nota Conceptual' FROM DUAL UNION ALL
            SELECT 'Matriz de Planificacion' FROM DUAL UNION ALL
            SELECT 'Seguimiento Agil' FROM DUAL UNION ALL
            SELECT 'Autoevaluacion de Ciclo' FROM DUAL UNION ALL
            SELECT 'Documento de Aprobacion de Producto' FROM DUAL UNION ALL
            SELECT 'Evidencia de No Aprobacion' FROM DUAL UNION ALL
            SELECT 'Informe Final de Cierre' FROM DUAL UNION ALL
            SELECT 'Evidencia de Suspension' FROM DUAL UNION ALL
            SELECT 'Informe de Cancelacion' FROM DUAL
        )
        SELECT c.NOMBRE
          FROM canonicos c
         WHERE NOT EXISTS (
                SELECT 1 FROM TIPO_DOCUMENTO t WHERE t.NOMBRE = c.NOMBRE
               )
    ) LOOP
        v_lista := v_lista || r.NOMBRE || ';';
        DBMS_OUTPUT.PUT_LINE('TD_CANONICO_FALTANTE=' || r.NOMBRE);
    END LOOP;
    DBMS_OUTPUT.PUT_LINE('TD_FALTANTES_LISTA=' || NVL(v_lista, '<NINGUNO>'));
END;
/

PROMPT [019-diagnostic] Conteo de ROL canonico y no canonico...

DECLARE
    v_total     PLS_INTEGER;
    v_canonicos PLS_INTEGER;
    v_legacy    PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total FROM ROL;
    SELECT COUNT(*) INTO v_canonicos
      FROM ROL
     WHERE NOMBRE_ROL IN (
            'GLOBAL_ADMIN','UNIDAD_ADMIN','RESPONSABLE',
            'EVALUADOR','AUTORIDAD','CONSULTA'
           );
    v_legacy := v_total - v_canonicos;
    DBMS_OUTPUT.PUT_LINE('ROL_TOTAL=' || TO_CHAR(v_total));
    DBMS_OUTPUT.PUT_LINE('ROL_CANONICOS=' || TO_CHAR(v_canonicos) || '/6');
    DBMS_OUTPUT.PUT_LINE('ROL_NO_CANONICOS=' || TO_CHAR(v_legacy));

    FOR r IN (
        SELECT ID_ROL, NOMBRE_ROL, DESCRIPCION, NIVEL_ACCESO
          FROM ROL
         ORDER BY ID_ROL
    ) LOOP
        DBMS_OUTPUT.PUT_LINE(
            'ROL_ID=' || TO_CHAR(r.ID_ROL) ||
            ';NOMBRE=' || NVL(r.NOMBRE_ROL, '<NULL>') ||
            ';NIVEL=' || TO_CHAR(r.NIVEL_ACCESO) ||
            ';DESC=' || NVL(r.DESCRIPCION, '<NULL>')
        );
    END LOOP;
END;
/

PROMPT [019-diagnostic] Conteo de TRANSICION_PERMITIDA legacy e inactivada...

DECLARE
    v_total     PLS_INTEGER;
    v_inactivas PLS_INTEGER;
    v_activas   PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total FROM TRANSICION_PERMITIDA;
    SELECT COUNT(*) INTO v_inactivas FROM TRANSICION_PERMITIDA WHERE ACTIVO = 'N';
    SELECT COUNT(*) INTO v_activas FROM TRANSICION_PERMITIDA WHERE ACTIVO = 'S';
    DBMS_OUTPUT.PUT_LINE('TP_TOTAL=' || TO_CHAR(v_total));
    DBMS_OUTPUT.PUT_LINE('TP_INACTIVAS=' || TO_CHAR(v_inactivas));
    DBMS_OUTPUT.PUT_LINE('TP_ACTIVAS=' || TO_CHAR(v_activas));
END;
/

PROMPT [019-diagnostic] Conteo de MATRIZ_FUNCION y MATRIZ_FUNCION_PERFIL_UNIDAD...

DECLARE
    v_funcion     PLS_INTEGER;
    v_combinacion PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_funcion FROM MATRIZ_FUNCION;
    SELECT COUNT(*) INTO v_combinacion FROM MATRIZ_FUNCION_PERFIL_UNIDAD;
    DBMS_OUTPUT.PUT_LINE('MF_TOTAL=' || TO_CHAR(v_funcion));
    DBMS_OUTPUT.PUT_LINE('MFPU_TOTAL=' || TO_CHAR(v_combinacion));
    FOR rec_mf IN (
        SELECT mf.CODIGO, mf.DESCRIPCION, mf.ACTIVA
          FROM MATRIZ_FUNCION mf
         ORDER BY mf.ID_FUNCION
    ) LOOP
        DBMS_OUTPUT.PUT_LINE(
            'MF_CODIGO=' || NVL(rec_mf.CODIGO, '<NULL>') ||
            ';ACTIVA=' || NVL(rec_mf.ACTIVA, '<NULL>') ||
            ';DESC=' || NVL(rec_mf.DESCRIPCION, '<NULL>')
        );
    END LOOP;
    FOR rec_mfpu IN (
        SELECT mf.CODIGO AS FUNCION, mfu.ACTIVA,
               (SELECT NOMBRE_ROL FROM ROL WHERE ID_ROL = mfu.ID_ROL) AS ROL
          FROM MATRIZ_FUNCION_PERFIL_UNIDAD mfu
          JOIN MATRIZ_FUNCION mf ON mf.ID_FUNCION = mfu.ID_FUNCION
         ORDER BY mfu.ID_COMBINACION
    ) LOOP
        DBMS_OUTPUT.PUT_LINE(
            'MFPU_FUNCION=' || NVL(rec_mfpu.FUNCION, '<NULL>') ||
            ';ROL=' || NVL(rec_mfpu.ROL, '<NULL>') ||
            ';ACTIVA=' || NVL(rec_mfpu.ACTIVA, '<NULL>')
        );
    END LOOP;
END;
/

PROMPT [019-diagnostic] Conteo de USUARIO y USUARIO_ROL_UNIDAD...

DECLARE
    v_total      PLS_INTEGER;
    v_con_kc     PLS_INTEGER;
    v_sin_kc     PLS_INTEGER;
    v_uru        PLS_INTEGER;
    v_uru_abiert PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_total FROM USUARIO;
    SELECT COUNT(*) INTO v_con_kc FROM USUARIO WHERE KEYCLOAK_ID IS NOT NULL;
    SELECT COUNT(*) INTO v_sin_kc FROM USUARIO WHERE KEYCLOAK_ID IS NULL;
    SELECT COUNT(*) INTO v_uru FROM USUARIO_ROL_UNIDAD;
    SELECT COUNT(*) INTO v_uru_abiert
      FROM USUARIO_ROL_UNIDAD
     WHERE FECHA_FIN IS NULL
       AND REVOCADA_EN IS NULL
       AND INACTIVA_TEMPORALMENTE = 'N';
    DBMS_OUTPUT.PUT_LINE('USR_TOTAL=' || TO_CHAR(v_total));
    DBMS_OUTPUT.PUT_LINE('USR_CON_KEYCLOAK=' || TO_CHAR(v_con_kc));
    DBMS_OUTPUT.PUT_LINE('USR_SIN_KEYCLOAK=' || TO_CHAR(v_sin_kc));
    DBMS_OUTPUT.PUT_LINE('URU_TOTAL=' || TO_CHAR(v_uru));
    DBMS_OUTPUT.PUT_LINE('URU_ABIERTAS=' || TO_CHAR(v_uru_abiert));
    FOR r IN (
        SELECT u.KEYCLOAK_ID, u.LOGIN, u.LOGIN_SINTETICO
          FROM USUARIO u
         WHERE u.KEYCLOAK_ID IS NOT NULL
         ORDER BY u.ID_USUARIO
    ) LOOP
        DBMS_OUTPUT.PUT_LINE(
            'USR_KEYCLOAK=' || NVL(r.KEYCLOAK_ID, '<NULL>') ||
            ';LOGIN=' || NVL(r.LOGIN, '<NULL>') ||
            ';SINTETICO=' || NVL(r.LOGIN_SINTETICO, '<NULL>')
        );
    END LOOP;
END;
/

PROMPT [019-diagnostic] Listado del tipo canónico huerfano presente (presencia parcial de 019)...

DECLARE
    v_canonicos_presentes PLS_INTEGER;
    v_canonicos_ausentes  PLS_INTEGER;
    v_huerfanos          VARCHAR2(4000);
BEGIN
    SELECT COUNT(*) INTO v_canonicos_presentes
      FROM TIPO_DOCUMENTO
     WHERE NOMBRE IN (
            'Ficha de Iniciativa','Informe de Opinion Tecnica',
            'Documento Formal de Decision',
            'Documento de Aprobacion o Autorizacion','Nota Conceptual',
            'Matriz de Planificacion','Seguimiento Agil',
            'Autoevaluacion de Ciclo',
            'Documento de Aprobacion de Producto',
            'Evidencia de No Aprobacion',
            'Informe Final de Cierre','Evidencia de Suspension',
            'Informe de Cancelacion'
           );
    v_canonicos_ausentes := 13 - v_canonicos_presentes;

    IF v_canonicos_presentes = 0 THEN
        DBMS_OUTPUT.PUT_LINE(
            'TD_HUERFANO_PRESENTE=NINGUNO (0/13 canonicos presentes; la semilla 019 puede ejecutarse)');
    ELSIF v_canonicos_presentes = 13 THEN
        DBMS_OUTPUT.PUT_LINE(
            'TD_HUERFANO_PRESENTE=TODOS (13/13 canonicos presentes; la semilla 019 ya fue aplicada)');
    ELSE
        DBMS_OUTPUT.PUT_LINE(
            'TD_HUERFANO_PRESENTE=PARCIAL (' || TO_CHAR(v_canonicos_presentes) ||
            '/13 canonicos presentes; ' || TO_CHAR(v_canonicos_ausentes) ||
            ' faltantes; requiere intervencion humana)');
        FOR rec_td_huerfano IN (
            SELECT ID_TIPO_DOC, NOMBRE, CONTEXTO, CLASIFICACION_DEFECTO,
                   ESTADO_ASOCIADO, ACTIVO
              FROM TIPO_DOCUMENTO
             WHERE NOMBRE IN (
                    'Ficha de Iniciativa','Informe de Opinion Tecnica',
                    'Documento Formal de Decision',
                    'Documento de Aprobacion o Autorizacion','Nota Conceptual',
                    'Matriz de Planificacion','Seguimiento Agil',
                    'Autoevaluacion de Ciclo',
                    'Documento de Aprobacion de Producto',
                    'Evidencia de No Aprobacion',
                    'Informe Final de Cierre','Evidencia de Suspension',
                    'Informe de Cancelacion'
                   )
             ORDER BY ID_TIPO_DOC
        ) LOOP
            v_huerfanos := v_huerfanos ||
                'ID=' || TO_CHAR(rec_td_huerfano.ID_TIPO_DOC) ||
                ';NOMBRE=' || NVL(rec_td_huerfano.NOMBRE, '<NULL>') ||
                ';CONTEXTO=' || NVL(rec_td_huerfano.CONTEXTO, '<NULL>') ||
                ';CLAS_DEF=' || NVL(rec_td_huerfano.CLASIFICACION_DEFECTO, '<NULL>') ||
                ';ESTADO=' || NVL(rec_td_huerfano.ESTADO_ASOCIADO, '<NULL>') ||
                ';ACTIVO=' || NVL(rec_td_huerfano.ACTIVO, '<NULL>') || '||';
            DBMS_OUTPUT.PUT_LINE(
                'TD_HUERFANO_ID=' || TO_CHAR(rec_td_huerfano.ID_TIPO_DOC) ||
                ';NOMBRE=' || NVL(rec_td_huerfano.NOMBRE, '<NULL>') ||
                ';CONTEXTO=' || NVL(rec_td_huerfano.CONTEXTO, '<NULL>') ||
                ';CLAS_DEF=' || NVL(rec_td_huerfano.CLASIFICACION_DEFECTO, '<NULL>') ||
                ';ESTADO=' || NVL(rec_td_huerfano.ESTADO_ASOCIADO, '<NULL>') ||
                ';ACTIVO=' || NVL(rec_td_huerfano.ACTIVO, '<NULL>')
            );
        END LOOP;
        DBMS_OUTPUT.PUT_LINE('TD_HUERFANOS_LISTA=' || NVL(v_huerfanos, '<NINGUNO>'));
    END IF;
END;
/

PROMPT [019-diagnostic] Resumen de estado de filas semilla...

DECLARE
    v_td_canonicos PLS_INTEGER;
    v_rol_canonicos PLS_INTEGER;
    v_tp_inactivas PLS_INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_td_canonicos
      FROM TIPO_DOCUMENTO
     WHERE NOMBRE IN (
            'Ficha de Iniciativa','Informe de Opinion Tecnica',
            'Documento Formal de Decision',
            'Documento de Aprobacion o Autorizacion','Nota Conceptual',
            'Matriz de Planificacion','Seguimiento Agil',
            'Autoevaluacion de Ciclo',
            'Documento de Aprobacion de Producto',
            'Evidencia de No Aprobacion',
            'Informe Final de Cierre','Evidencia de Suspension',
            'Informe de Cancelacion'
           );
    SELECT COUNT(*) INTO v_rol_canonicos
      FROM ROL
     WHERE NOMBRE_ROL IN (
            'GLOBAL_ADMIN','UNIDAD_ADMIN','RESPONSABLE',
            'EVALUADOR','AUTORIDAD','CONSULTA'
           );
    SELECT COUNT(*) INTO v_tp_inactivas
      FROM TRANSICION_PERMITIDA WHERE ACTIVO = 'N';

    IF v_td_canonicos = 13 AND v_rol_canonicos = 6 AND v_tp_inactivas >= 1 THEN
        DBMS_OUTPUT.PUT_LINE(
            'SEMILLA_019_ESTADO=APLICADA (13 tipos canonicos, 6 roles canonicos, transiciones legacy inactivas)');
    ELSIF v_td_canonicos = 0 AND v_rol_canonicos = 0 AND v_tp_inactivas = 0 THEN
        DBMS_OUTPUT.PUT_LINE(
            'SEMILLA_019_ESTADO=NO_APLICADA (sin tipos canonicos, sin roles canonicos, sin transiciones inactivas)');
    ELSE
        DBMS_OUTPUT.PUT_LINE(
            'SEMILLA_019_ESTADO=PARCIAL (td_canonicos=' || TO_CHAR(v_td_canonicos) ||
            '/13, rol_canonicos=' || TO_CHAR(v_rol_canonicos) || '/6, tp_inactivas=' ||
            TO_CHAR(v_tp_inactivas) || ')');
    END IF;
    DBMS_OUTPUT.PUT_LINE('Diagnostico 019 extendido completado; no se realizaron cambios.');
END;
/
