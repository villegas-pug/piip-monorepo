-- ============================================================================
-- PIIP MIDAGRI - Semilla 019 - Catalogos canonicos de portafolio
-- Archivo    : 019_catalogos_canonicos_portafolio.sql
-- Esquema    : KALLPA_PIIP
-- Modulo     : transversal
-- Tipo       : Semilla
-- Dependencias: 001, 002, 003, 003.1, 003.2, 004, 005, 005.1, 006, 007,
--               008+008.1, 009, 010, 011, 013, 014+014.1, 015, 016, 017
--               VIGENTES. 012 y 018-024 permanecen diferidos; la semilla
--               no requiere sus objetos.
-- Alcance    : (a) asegura los 6 roles canonicos (GlobalAdmin,
--               UnidadAdmin, Responsable, Evaluador, Autoridad, Consulta)
--               con la grafia CamelCase vigente en la base de datos.
--               en ROL; (b) ampla TIPO_DOCUMENTO con 13 tipos documentales
--               aprobados para el portafolio; (c) inactiva (no borra) las
--               filas legacy de TRANSICION_PERMITIDA que ya no son
--               autoridad canonica; (d) corrige la descripcion de
--               UnidadAdmin sin duplicar filas. Toda la operacion es
--               idempotente mediante MERGE y no usa DROP/TRUNCATE.
-- Ejecucion : SQL Developer (Run Script/F5), SQLcl o SQL*Plus como
--             KALLPA_PIIP. EJECUCION UNICA, FAIL-FAST.
-- Compensacion forward-only: inactivar semillas no referenciadas; nunca
--             borrar referencias existentes. Los tipos documentales y
--             roles son append-only logico.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET SERVEROUTPUT ON SIZE UNLIMITED
SET SQLBLANKLINES ON
WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK

PROMPT [019] Validando huella vigente 001-017 y ausencia de duplicados previos...

DECLARE
    v_total_esperadas   PLS_INTEGER;
    v_total_encontradas PLS_INTEGER;
    v_total_user_tables PLS_INTEGER;
    v_incidencias       VARCHAR2(4000);
BEGIN
    -- Huella vigente 001-017: 13 de 001 + 1 de 002 + 2 de 003 + 2 de 004
    -- + 2 de 005 + 2 de 006 + 3 de 007 + 3 de 008.1 + 1 de 010 + 1 de 011
    -- + 3 de 012 + 2 de 013 + 4 de 014/014.1 + 7 de 015 + 3 de 016 + 6 de 017
    -- = 55. La lista debe coincidir exactamente con la huella vigente;
    -- cualquier tabla faltante o adicional aborta la precondicion con
    -- un detalle que identifica la diferencia.
    SELECT COUNT(*) INTO v_total_encontradas
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

    -- Total de tablas de la lista esperada (55). Se evalua como check
    -- secundario: si las validaciones individuales pasan, este contador
    -- debe coincidir; si no, aborta con ORA-20198.
    SELECT COUNT(*) INTO v_total_esperadas
      FROM (
            SELECT 'UNIDAD_EJECUTORA' AS NOMBRE FROM DUAL UNION ALL
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
           );

    -- Validacion detallada: tablas de la lista que faltan.
    FOR r IN (
        WITH esperados (NOMBRE) AS (
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
        SELECT e.NOMBRE
          FROM esperados e
         WHERE NOT EXISTS (
                SELECT 1 FROM USER_TABLES ut WHERE ut.TABLE_NAME = e.NOMBRE
               )
    ) LOOP
        v_incidencias := v_incidencias || r.NOMBRE || '[FALTANTE];';
    END LOOP;

    -- Validacion detallada: tablas presentes en USER_TABLES que no
    -- estan en la lista esperada. Se excluyen objetos Oracle internos.
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
    ) LOOP
        v_incidencias := v_incidencias || r.TABLE_NAME || '[NO_ESPERADA];';
    END LOOP;

    IF v_incidencias IS NOT NULL THEN
        RAISE_APPLICATION_ERROR(-20190,
            'Precondicion 019: huella vigente 001-017 invalida. Detalle: ' || v_incidencias);
    END IF;

    IF v_total_encontradas <> v_total_esperadas THEN
        RAISE_APPLICATION_ERROR(-20198,
            'Precondicion 019: conteo de tablas vigentes '
            || TO_CHAR(v_total_encontradas) || ' no coincide con la lista esperada '
            || TO_CHAR(v_total_esperadas));
    END IF;

    -- La semilla no debe haberse ejecutado antes. El nombre de los tipos
    -- documentales de portafolio es canonico: debe haber cero filas
    -- semilla insertadas; la presencia de cualquiera de los nombres
    -- indica una ejecucion previa.
    SELECT COUNT(*) INTO v_total_user_tables
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
    IF v_total_user_tables <> 0 THEN
        -- Distinguir tres casos para abortar con un mensaje claro:
        --  * ninguno presente: la semilla puede ejecutarse.
        --  * todos los 13 canonicos presentes: la semilla ya fue
        --    aplicada y no es re-ejecutable.
        --  * presencia parcial: la huella esta corrupta o se aplico
        --    parcialmente. Listar cuantos faltan.
        DECLARE
            v_presentes PLS_INTEGER := 0;
            v_faltantes  VARCHAR2(4000);
        BEGIN
            SELECT COUNT(*) INTO v_presentes
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
            IF v_presentes = 13 THEN
                RAISE_APPLICATION_ERROR(-20192,
                    'Precondicion 019: la semilla ya fue aplicada (13 tipos canonicos presentes); no es re-ejecutable');
            ELSIF v_presentes > 0 THEN
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
                    v_faltantes := v_faltantes || r.NOMBRE || ';';
                END LOOP;
                DECLARE
                    v_huerfanos VARCHAR2(4000) := '';
                BEGIN
                    FOR rec_td_huerfano IN (
                        SELECT ID_TIPO_DOC, NOMBRE, CONTEXTO, CLASIFICACION_DEFECTO,
                               ESTADO_ASOCIADO, ACTIVO
                          FROM TIPO_DOCUMENTO
                         WHERE NOMBRE IN (
                                'Ficha de Iniciativa','Informe de Opinion Tecnica',
                                'Documento Formal de Decision',
                                'Documento de Aprobacion o Autorizacion',
                                'Nota Conceptual','Matriz de Planificacion',
                                'Seguimiento Agil','Autoevaluacion de Ciclo',
                                'Documento de Aprobacion de Producto',
                                'Evidencia de No Aprobacion',
                                'Informe Final de Cierre',
                                'Evidencia de Suspension',
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
                    END LOOP;
                    RAISE_APPLICATION_ERROR(-20193,
                        'Precondicion 019: presencia parcial de tipos canonicos ('
                        || TO_CHAR(v_presentes) || '/13). Huerfanos presentes: ' ||
                        NVL(v_huerfanos, '<NINGUNO>') ||
                        '. Faltantes: ' || v_faltantes);
                END;
            END IF;
        END;
    END IF;
END;
/

PROMPT [019] Asegurando los 6 roles canonicos de portafolio (grafia CamelCase vigente)...

-- El baseline 001 inserta los 6 roles con ID_ROL literal (1-6); no
-- existe SEQ_ROL. El MERGE usa ID_ROL del origen para evitar ORA-02289.
MERGE INTO ROL t
USING (
    SELECT 1 AS ID_ROL, 'GlobalAdmin' AS NOMBRE_ROL, 5 AS NIVEL_ACCESO,
           'Administrador global PIIP' AS DESCRIPCION FROM DUAL UNION ALL
    SELECT 2, 'UnidadAdmin', 4, 'Administrador de unidad ejecutora' FROM DUAL UNION ALL
    SELECT 3, 'Responsable', 3, 'Responsable de proyecto o iniciativa' FROM DUAL UNION ALL
    SELECT 4, 'Evaluador',   3, 'Evaluador de iniciativa' FROM DUAL UNION ALL
    SELECT 5, 'Autoridad',   4, 'Autoridad aprobadora' FROM DUAL UNION ALL
    SELECT 6, 'Consulta',    1, 'Consulta de solo lectura' FROM DUAL
) s
ON (t.NOMBRE_ROL = s.NOMBRE_ROL)
WHEN MATCHED THEN
    UPDATE SET t.DESCRIPCION   = s.DESCRIPCION,
               t.NIVEL_ACCESO = s.NIVEL_ACCESO
             WHERE t.DESCRIPCION   <> s.DESCRIPCION
                OR t.NIVEL_ACCESO <> s.NIVEL_ACCESO
WHEN NOT MATCHED THEN
    INSERT (ID_ROL, NOMBRE_ROL, DESCRIPCION, NIVEL_ACCESO)
    VALUES (s.ID_ROL, s.NOMBRE_ROL, s.DESCRIPCION, s.NIVEL_ACCESO)
;

PROMPT [019] Ampliando TIPO_DOCUMENTO con 13 tipos documentales canonicos de portafolio...

MERGE INTO TIPO_DOCUMENTO t
USING (
    SELECT 'Ficha de Iniciativa'                       AS NOMBRE, 'PRESENTADO'          AS ESTADO_ASOCIADO, 'S' AS OBLIGATORIO, NULL AS DESCRIPCION, 'ANEXO_NT_FICHA'      AS ANEXO_NT, 'PORTAFOLIO' AS CONTEXTO, 'INTERNO'     AS CLASIFICACION_DEFECTO FROM DUAL UNION ALL
    SELECT 'Informe de Opinion Tecnica',                        'PRESENTADO',          'N', NULL, 'ANEXO_NT_OPINION',     'PORTAFOLIO',        'INTERNO'              FROM DUAL UNION ALL
    SELECT 'Documento Formal de Decision',                     'INICIATIVA_APROBADA', 'S', NULL, 'ANEXO_NT_DECISION',    'PORTAFOLIO',        'RESTRINGIDO'          FROM DUAL UNION ALL
    SELECT 'Documento de Aprobacion o Autorizacion',            'PROYECTO_EJECUCION',  'S', NULL, 'ANEXO_NT_APROB',       'PORTAFOLIO',        'RESTRINGIDO'          FROM DUAL UNION ALL
    SELECT 'Nota Conceptual',                                   'PROYECTO_EJECUCION',  'N', NULL, 'ANEXO_NT_CONCEPTUAL',  'PORTAFOLIO',        'INTERNO'              FROM DUAL UNION ALL
    SELECT 'Matriz de Planificacion',                           'PROYECTO_EJECUCION',  'N', NULL, 'ANEXO_NT_PLAN',        'PORTAFOLIO',        'INTERNO'              FROM DUAL UNION ALL
    SELECT 'Seguimiento Agil',                                  'PROYECTO_EJECUCION',  'N', NULL, 'ANEXO_NT_SEG',         'PORTAFOLIO',        'INTERNO'              FROM DUAL UNION ALL
    SELECT 'Autoevaluacion de Ciclo',                           'PROYECTO_EJECUCION',  'N', NULL, 'ANEXO_NT_AUTOEV',      'PORTAFOLIO',        'INTERNO'              FROM DUAL UNION ALL
    SELECT 'Documento de Aprobacion de Producto',               'PRODUCTO_APROBADO',   'S', NULL, 'ANEXO_NT_APROBPROD',   'PORTAFOLIO',        'RESTRINGIDO'          FROM DUAL UNION ALL
    SELECT 'Evidencia de No Aprobacion',                         'PRODUCTO_NO_APROBADO','N', NULL, 'ANEXO_NT_NOAPROB',    'PORTAFOLIO',        'INTERNO'              FROM DUAL UNION ALL
    SELECT 'Informe Final de Cierre',                            'PRODUCTO_APROBADO',   'N', NULL, 'ANEXO_NT_CIERRE',      'PORTAFOLIO',        'INTERNO'              FROM DUAL UNION ALL
    SELECT 'Evidencia de Suspension',                            'SUSPENDIDO',          'N', NULL, 'ANEXO_NT_SUSP',        'PORTAFOLIO',        'INTERNO'              FROM DUAL UNION ALL
    SELECT 'Informe de Cancelacion',                             'CANCELADO',           'N', NULL, 'ANEXO_NT_CANCEL',      'PORTAFOLIO',        'INTERNO'              FROM DUAL
) s
ON (t.NOMBRE = s.NOMBRE)
WHEN NOT MATCHED THEN
    INSERT (ID_TIPO_DOC, NOMBRE, ESTADO_ASOCIADO, OBLIGATORIO, DESCRIPCION, ANEXO_NT, ACTIVO, CONTEXTO, CLASIFICACION_DEFECTO)
    VALUES (SEQ_DOCUMENTO.NEXTVAL, s.NOMBRE, s.ESTADO_ASOCIADO, s.OBLIGATORIO, s.DESCRIPCION, s.ANEXO_NT, 'S', s.CONTEXTO, s.CLASIFICACION_DEFECTO)
;

PROMPT [019] Inactivando transiciones legacy que ya no son autoridad canonica (sin borrado fisico)...

-- Se inactivan todas las transiciones heredadas del baseline (001) que no
-- forman parte del catalogo canonico vigente. La maquina de estados reside
-- en TransicionEstadoService Java; TRANSICION_PERMITIDA permanece como
-- legado inactivo. La operacion es idempotente: si ya estan inactivas
-- ('N'), el UPDATE no modifica.
-- Se cierra la transaccion implicita del MERGE previo con un COMMIT
-- explicito para evitar ORA-12839 entre el MERGE y el UPDATE. El hint
-- NO_PARALLEL fuerza la ejecucion serial del UPDATE.
COMMIT;

UPDATE /*+ NO_PARALLEL */ TRANSICION_PERMITIDA
   SET ACTIVO = 'N'
 WHERE ACTIVO = 'S'
   AND (ESTADO_DESTINO, ESTADO_ORIGEN) NOT IN (
        ('PRESENTADO',     'PRESENTADO'),
        ('EN_EVALUACION',  'PRESENTADO'),
        ('EN_SUBSANACION', 'EN_EVALUACION'),
        ('EN_PLANIFICACION','EN_SUBSANACION'),
        ('EN_SEGUIMIENTO', 'EN_PLANIFICACION'),
        ('EN_CIERRE',      'EN_SEGUIMIENTO'),
        ('APROBADO',       'EN_CIERRE'),
        ('NO_APROBADO',    'EN_CIERRE'),
        ('SUSPENDIDO',     'EN_SEGUIMIENTO'),
        ('CANCELADO',      'PRESENTADO')
       )
;

PROMPT [019] Normalizando descripcion de UnidadAdmin sin duplicar filas...

MERGE INTO ROL t
USING (SELECT 'UnidadAdmin' AS NOMBRE_ROL,
              'Administrador de unidad ejecutora' AS DESCRIPCION
         FROM DUAL) s
ON (t.NOMBRE_ROL = s.NOMBRE_ROL)
WHEN MATCHED THEN
    UPDATE SET t.DESCRIPCION = s.DESCRIPCION
             WHERE t.DESCRIPCION IS NULL
                OR t.DESCRIPCION <> s.DESCRIPCION
;

PROMPT [019] Validando huella final de la semilla...

-- Se cierra la transaccion implicita del MERGE final de normalizacion
-- de UnidadAdmin antes de que el bloque de validacion lea del
-- diccionario, para evitar ORA-12838 (lectura/modificacion tras DML
-- en paralelo).
COMMIT;

DECLARE
    v_total PLS_INTEGER;
BEGIN
    -- 6 roles canonicos
    SELECT COUNT(*) INTO v_total
      FROM ROL
     WHERE NOMBRE_ROL IN (
            'GlobalAdmin','UnidadAdmin','Responsable',
            'Evaluador','Autoridad','Consulta'
           );
    IF v_total <> 6 THEN
        RAISE_APPLICATION_ERROR(-20192,
            'Validacion 019: roles canonicos incompletos (6 esperados)');
    END IF;

    -- 13 tipos documentales canonicos
    SELECT COUNT(*) INTO v_total
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
    IF v_total <> 13 THEN
        RAISE_APPLICATION_ERROR(-20193,
            'Validacion 019: tipos documentales canonicos incompletos (13 esperados)');
    END IF;

    -- Al menos una transicion legacy debe haber sido inactivada, salvo que
    -- el legado ya estuviera limpio. Esto verifica la semantica forward-only.
    SELECT COUNT(*) INTO v_total
      FROM TRANSICION_PERMITIDA
     WHERE ACTIVO = 'N';
    IF v_total < 1 THEN
        RAISE_APPLICATION_ERROR(-20194,
            'Validacion 019: ninguna transicion legacy inactivada; revise la maquina de estados');
    END IF;

    DBMS_OUTPUT.PUT_LINE(
        'Validacion final satisfactoria: semilla 019 aplicada correctamente.');
END;
/

-- El COMMIT final es consistencia documental; MERGE y UPDATE confirman implicitamente.
COMMIT;

PROMPT Semilla 019_catalogos_canonicos_portafolio completada correctamente.


SELECT * FROM USUARIO;
/

UPDATE USUARIO
    SET KEYCLOAK_ID = 'd0e320e9-3e47-44e1-b457-00963a1a0702'
WHERE ID_USUARIO = 1
/

-- ! COMMIT;

/*
{
  "issuer": "https://rcgv-services-dev.duckdns.org/keycloak/realms/piip",
  "clientId": "piip-token-test",
  "redirectUri": "http://localhost:4200/auth/callback",
  "postLogoutRedirectUri": "http://localhost:4200/consulta-publica",
  "scopes": ["GlobalAdmin", "Responsable", "Autoridad", "Evaluador", "Consulta", "UnidadAdmin"],
  "ogtiThemeConfirmationReference": "cualquier-texto"
}

*/

SELECT * FROM ROL;