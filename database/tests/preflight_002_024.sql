-- ============================================================================
-- PIIP MIDAGRI - Diagnostico preflight del alcance activo 002-017 y 019-021
-- Archivo   : preflight_002_024.sql
-- Esquema   : KALLPA_PIIP
-- Modo      : SOLO LECTURA. Este script NO modifica el esquema ni los datos.
--
-- ============================================================================
-- PROPOSITO
-- ----------------------------------------------------------------------------
-- Brindar al equipo de base de datos un diagnostico estatico, ejecutable de
-- forma manual por un DBA autorizado, que determine si la base de datos
-- vigente del esquema KALLPA_PIIP cumple las precondiciones minimas exigidas
-- por el diccionario fisico activo antes de autorizar la ejecucion de
-- cualquier script incremental activo. Las consultas verifican la huella
-- del baseline 001, la presencia o ausencia de objetos futuros, la
-- integridad de los binarios y cadenas documentales, la inexistencia
-- historica de asignaciones GlobalAdmin, la compatibilidad de
-- USUARIO_ROL_UNIDAD con la futura matriz funcional y un inventario
-- informativo de referencias legacy para una futura migracion.
--
-- ============================================================================
-- GARANTIA DE SOLO LECTURA
-- ----------------------------------------------------------------------------
-- Este archivo se compone exclusivamente de sentencias SELECT y de
-- comentarios. No contiene DDL (CREATE, ALTER, DROP, TRUNCATE, RENAME,
-- COMMENT, FLASHBACK, PURGE), ni DML (INSERT, UPDATE, DELETE, MERGE), ni
-- sentencias de control de privilegios (GRANT, REVOKE), ni sentencias
-- transaccionales (COMMIT, ROLLBACK, SAVEPOINT). Las unicas acciones
-- permitidas son SELECT sobre vistas del catalogo del esquema
-- (USER_TABLES, USER_TAB_COLUMNS, USER_CONSTRAINTS, USER_CONS_COLUMNS,
-- USER_INDEXES, USER_IND_COLUMNS, USER_IND_EXPRESSIONS, USER_SEQUENCES,
-- USER_OBJECTS) y SELECT sobre las tablas vigentes de la aplicacion
-- (UNIDAD_EJECUTORA, USUARIO, ROL, USUARIO_ROL_UNIDAD, PROYECTO,
-- PROYECTO_UNIDAD_ORGANICA, TRANSICION_PERMITIDA, TIPO_DOCUMENTO,
-- DOCUMENTO, TRANSICION_ESTADO, SECUENCIA_CODIGO, AUDITORIA_ACCESO,
-- AUDITORIA_EVENTO). El archivo se entrega con SET HEADING ON y SET
-- VERIFY OFF para que la salida sea legible y reproducible.
--
-- El DBA puede copiar el contenido y ejecutarlo en SQL*Plus, SQLcl o
-- SQL Developer (Run Script / F5) sin riesgo de mutacion. El script
-- produce unicamente un informe de salida. Si se detecta un valor
-- distinto del esperado, el script NO aborta; documenta el problema y
-- permite al equipo decidir el camino de remediacion.
--
-- ============================================================================
-- MODO DE EJECUCION MANUAL POR DBA
-- ----------------------------------------------------------------------------
-- 1. Conectarse al esquema KALLPA_PIIP en el ambiente objetivo.
-- 2. Cargar este archivo en SQL*Plus, SQLcl o SQL Developer (no copiar
--    caracteres invisibles; abrir como texto plano UTF-8).
-- 3. Ejecutar como Run Script (F5) en SQL Developer, @script.sql en
--    SQLcl/SQL*Plus. El modo de ejecucion NO debe ser statement-by-
--    statement con commit implicito, ya que anula la garantia de solo
--    lectura observacional; un DBA no debe confirmar el archivo
--    modificado.
-- 4. Capturar la salida estandar (spool recomendado) para adjuntarla al
--    expediente de aprobacion del diseno fisico vigente.
-- 5. Si alguna consulta reporta un conteo o un identificador problematico,
--    NO continuar con la ejecucion del incremento activo afectado. Remitir la salida
--    al aprobador DB junto con la accion correctiva propuesta.
--
-- Este script no exige parametros de sustitucion. Si el equipo requiere
-- ejecutarlo con un sinonimo publico del esquema, ajustar la clausula
-- FROM para prefijar KALLPA_PIIP.; ninguna otra modificacion es
-- necesaria.
-- ============================================================================

SET DEFINE OFF
SET VERIFY OFF
SET FEEDBACK ON
SET HEADING ON
SET LINESIZE 200
SET PAGESIZE 200
SET SQLBLANKLINES ON

PROMPT
PROMPT ============================================================
PROMPT PIIP - Diagnostico preflight del alcance activo 002-017 y 019-021
PROMPT Esquema: KALLPA_PIIP
PROMPT Modo   : SOLO LECTURA (sin DDL/DML/GRANT)
PROMPT ============================================================
PROMPT

-- ============================================================================
-- SECCION 1 - Huella del esquema y baseline 001
-- ----------------------------------------------------------------------------
-- Verifica que el esquema actual contiene exactamente las 13 tablas y 10
-- secuencias del baseline 001. Tambien cuenta constraints, indices y filas
-- semilla canonicas. Si el conteo es diferente, el incremento activo afectado no debe
-- ejecutarse: existe un objeto no documentado o una version divergente.
-- ============================================================================

PROMPT
PROMPT --- [1.1] Tablas del baseline 001 esperadas
SELECT TABLE_NAME,
       CASE
           WHEN TABLE_NAME IN ('UNIDAD_EJECUTORA','USUARIO','ROL','USUARIO_ROL_UNIDAD',
                               'PROYECTO','PROYECTO_UNIDAD_ORGANICA','TRANSICION_PERMITIDA',
                               'TIPO_DOCUMENTO','DOCUMENTO','TRANSICION_ESTADO',
                               'SECUENCIA_CODIGO','AUDITORIA_ACCESO','AUDITORIA_EVENTO')
           THEN 'BASELINE'
           ELSE 'NO_BASELINE'
       END AS CLASIFICACION
  FROM USER_TABLES
 ORDER BY TABLE_NAME;

PROMPT
PROMPT --- [1.2] Conteo de tablas baseline (esperado: 13)
SELECT COUNT(*) AS TOTAL_TABLAS_BASELINE
  FROM USER_TABLES
 WHERE TABLE_NAME IN ('UNIDAD_EJECUTORA','USUARIO','ROL','USUARIO_ROL_UNIDAD',
                      'PROYECTO','PROYECTO_UNIDAD_ORGANICA','TRANSICION_PERMITIDA',
                      'TIPO_DOCUMENTO','DOCUMENTO','TRANSICION_ESTADO',
                      'SECUENCIA_CODIGO','AUDITORIA_ACCESO','AUDITORIA_EVENTO');

PROMPT
PROMPT --- [1.3] Tablas presentes que NO son baseline (anomalias)
SELECT TABLE_NAME
  FROM USER_TABLES
 WHERE TABLE_NAME NOT IN ('UNIDAD_EJECUTORA','USUARIO','ROL','USUARIO_ROL_UNIDAD',
                          'PROYECTO','PROYECTO_UNIDAD_ORGANICA','TRANSICION_PERMITIDA',
                          'TIPO_DOCUMENTO','DOCUMENTO','TRANSICION_ESTADO',
                          'SECUENCIA_CODIGO','AUDITORIA_ACCESO','AUDITORIA_EVENTO')
 ORDER BY TABLE_NAME;

PROMPT
PROMPT --- [1.4] Secuencias del baseline esperadas (esperado: 10)
SELECT SEQUENCE_NAME,
       LAST_NUMBER,
       INCREMENT_BY,
       CACHE_SIZE,
       CYCLE_FLAG
  FROM USER_SEQUENCES
 WHERE SEQUENCE_NAME IN ('SEQ_UNIDAD_EJECUTORA','SEQ_USUARIO','SEQ_USUARIO_ROL_UNIDAD',
                         'SEQ_PROYECTO','SEQ_PROYECTO_UO','SEQ_DOCUMENTO',
                         'SEQ_TRANSICION_ESTADO','SEQ_SECUENCIA_CODIGO',
                         'SEQ_AUDITORIA_ACCESO','SEQ_AUDITORIA_EVENTO')
 ORDER BY SEQUENCE_NAME;

PROMPT
PROMPT --- [1.5] Conteo de secuencias baseline
SELECT COUNT(*) AS TOTAL_SECUENCIAS_BASELINE
  FROM USER_SEQUENCES
 WHERE SEQUENCE_NAME IN ('SEQ_UNIDAD_EJECUTORA','SEQ_USUARIO','SEQ_USUARIO_ROL_UNIDAD',
                         'SEQ_PROYECTO','SEQ_PROYECTO_UO','SEQ_DOCUMENTO',
                         'SEQ_TRANSICION_ESTADO','SEQ_SECUENCIA_CODIGO',
                         'SEQ_AUDITORIA_ACCESO','SEQ_AUDITORIA_EVENTO');

PROMPT
PROMPT --- [1.6] Secuencias presentes que NO son baseline
SELECT SEQUENCE_NAME
  FROM USER_SEQUENCES
 WHERE SEQUENCE_NAME NOT IN ('SEQ_UNIDAD_EJECUTORA','SEQ_USUARIO','SEQ_USUARIO_ROL_UNIDAD',
                             'SEQ_PROYECTO','SEQ_PROYECTO_UO','SEQ_DOCUMENTO',
                             'SEQ_TRANSICION_ESTADO','SEQ_SECUENCIA_CODIGO',
                             'SEQ_AUDITORIA_ACCESO','SEQ_AUDITORIA_EVENTO')
 ORDER BY SEQUENCE_NAME;

PROMPT
PROMPT --- [1.7] Conteo de constraints por tabla baseline (incluye PK, UK, FK, CHECK)
SELECT TABLE_NAME, CONSTRAINT_TYPE, COUNT(*) AS TOTAL
  FROM USER_CONSTRAINTS
 WHERE TABLE_NAME IN ('UNIDAD_EJECUTORA','USUARIO','ROL','USUARIO_ROL_UNIDAD',
                      'PROYECTO','PROYECTO_UNIDAD_ORGANICA','TRANSICION_PERMITIDA',
                      'TIPO_DOCUMENTO','DOCUMENTO','TRANSICION_ESTADO',
                      'SECUENCIA_CODIGO','AUDITORIA_ACCESO','AUDITORIA_EVENTO')
 GROUP BY TABLE_NAME, CONSTRAINT_TYPE
 ORDER BY TABLE_NAME, CONSTRAINT_TYPE;

PROMPT
PROMPT --- [1.8] Constraints deshabilitados (esperado: 0)
SELECT TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE, STATUS, VALIDATED
  FROM USER_CONSTRAINTS
 WHERE STATUS <> 'ENABLED'
   AND TABLE_NAME IN ('UNIDAD_EJECUTORA','USUARIO','ROL','USUARIO_ROL_UNIDAD',
                      'PROYECTO','PROYECTO_UNIDAD_ORGANICA','TRANSICION_PERMITIDA',
                      'TIPO_DOCUMENTO','DOCUMENTO','TRANSICION_ESTADO',
                      'SECUENCIA_CODIGO','AUDITORIA_ACCESO','AUDITORIA_EVENTO')
 ORDER BY TABLE_NAME, CONSTRAINT_NAME;

PROMPT
PROMPT --- [1.9] Conteo de indices VALID del baseline (esperado: 25)
SELECT COUNT(*) AS TOTAL_INDICES_BASELINE_VALIDOS
  FROM USER_INDEXES
 WHERE INDEX_NAME IN ('UX_UE_RAIZ','IDX_UE_PADRE','IDX_URU_USUARIO_ACT',
                      'IDX_URU_UNIDAD_ACT','IDX_URU_ROL','IDX_PROY_UNIDAD_EST',
                      'IDX_PROY_ESTADO','IDX_PROY_RESPONSABLE','IDX_PROY_FECHA_INICIO',
                      'IDX_PROY_TIPO_REG','IDX_TP_ROL','IDX_DOC_PROY_EST_ACT',
                      'IDX_DOC_TIPO','IDX_DOC_USUARIO','IDX_DOC_ANTERIOR',
                      'IDX_TE_PROY_FECHA','IDX_TE_USUARIO','IDX_TE_ROL',
                      'IDX_TE_UNIDAD','IDX_TE_DOCUMENTO','IDX_SC_UNIDAD',
                      'IDX_AA_USUARIO','IDX_AA_FECHA','IDX_AE_USUARIO','IDX_AE_PROC_FECHA')
   AND STATUS = 'VALID';

PROMPT
PROMPT --- [1.10] Semilla canonica: unidad raiz MIDAGRI (esperado: 1 fila)
SELECT COUNT(*) AS TOTAL_MIDAGRI
  FROM UNIDAD_EJECUTORA
 WHERE ID_UNIDAD = 1
   AND CODIGO_UNIDAD = 'MIDAGRI'
   AND NOMBRE = 'Ministerio de Desarrollo Agrario y Riego'
   AND NIVEL_JERARQUICO = 1
   AND ID_UNIDAD_PADRE IS NULL
   AND ACTIVO = 'S';

PROMPT
PROMPT --- [1.11] Semilla canonica: roles (esperado: 6 filas)
SELECT COUNT(*) AS TOTAL_ROLES_CANONICOS
  FROM ROL
 WHERE (ID_ROL = 1 AND NOMBRE_ROL = 'GlobalAdmin')
    OR (ID_ROL = 2 AND NOMBRE_ROL = 'UnidadAdmin')
    OR (ID_ROL = 3 AND NOMBRE_ROL = 'Responsable')
    OR (ID_ROL = 4 AND NOMBRE_ROL = 'Evaluador')
    OR (ID_ROL = 5 AND NOMBRE_ROL = 'Autoridad')
    OR (ID_ROL = 6 AND NOMBRE_ROL = 'Consulta');

PROMPT
PROMPT --- [1.12] Semilla canonica: transiciones permitidas (esperado: 8 filas activas)
SELECT COUNT(*) AS TOTAL_TRANSICIONES_CANONICAS
  FROM TRANSICION_PERMITIDA
 WHERE ID_TRANS_PERM BETWEEN 1 AND 8
   AND ACTIVO = 'S';

PROMPT
PROMPT --- [1.13] Semilla canonica: tipos documentales (esperado: 10 filas activas)
SELECT COUNT(*) AS TOTAL_TIPOS_DOC_CANONICOS
  FROM TIPO_DOCUMENTO
 WHERE ID_TIPO_DOC BETWEEN 1 AND 10
   AND ACTIVO = 'S';

-- ============================================================================
-- SECCION 2 - Objetos incrementales y futuros ya presentes
-- ----------------------------------------------------------------------------
-- Recorre USER_TABLES, USER_SEQUENCES, USER_INDEXES y USER_TAB_COLUMNS en
-- busca de objetos previstos por incrementos activos o diferidos. Si alguno
-- ya existe, el script correspondiente ya se ejecuto (estado VIGENTE) o
-- alguien deposito un objeto no documentado. Cualquier hallazgo aqui debe
-- bloquear la ejecucion del incremento afectado.
-- ============================================================================

PROMPT
PROMPT ============================================================
PROMPT --- [2.1] Tablas incrementales/futuras ya presentes (inventario)
SELECT TABLE_NAME
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
       )
 ORDER BY TABLE_NAME;

PROMPT
PROMPT --- [2.2] Secuencias incrementales/futuras ya presentes (inventario)
SELECT SEQUENCE_NAME
  FROM USER_SEQUENCES
 WHERE SEQUENCE_NAME IN (
        'SEQ_SOLICITUD_IDEMPOTENTE',
        'SEQ_EXPEDIENTE_INSTITUCIONAL','SEQ_DOCUMENTO_SERIE',
        'SEQ_DOCUMENTO_CLASIF_HIST','SEQ_DOCUMENTO_PUBLICACION',
        'SEQ_OBJETIVO_PEI_VERSION','SEQ_OBJETIVO_PEI',
        'SEQ_ACTIVIDAD_POI_VERSION','SEQ_ACTIVIDAD_POI',
        'SEQ_MATRIZ_VERSION','SEQ_MATRIZ_FUNCION','SEQ_MATRIZ_COMBINACION',
        'SEQ_URU_EVENTO','SEQ_SUPLENCIA_FUNCIONAL','SEQ_OPERACION_APROVISIONAMIENTO',
        'SEQ_INICIATIVA_PROYECTO','SEQ_PROYECTO_RESPONSABLE',
        'SEQ_PARTICIPANTE_PERSONA','SEQ_PROY_PART_PERSONA','SEQ_PROY_PART_UNIDAD',
        'SEQ_PROY_CAMPO_CLASIF','SEQ_PROY_CAMPO_CLASIF_HIST',
        'SEQ_EVALUACION_INICIATIVA','SEQ_SUBSANACION_INICIATIVA',
        'SEQ_APLICABILIDAD_INICIATIVA','SEQ_APLICABILIDAD_CRITERIO',
        'SEQ_PLANIFICACION_PROYECTO','SEQ_CICLO_PROYECTO','SEQ_CICLO_EVIDENCIA',
        'SEQ_PRODUCTO_PARCIAL','SEQ_PRESENTACION_PRODUCTO_FINAL','SEQ_VALIDACION_RESULTADO',
        'SEQ_CIERRE_PROYECTO',
        'SEQ_INCORPORACION_REGISTRO','SEQ_INCORPORACION_CAMBIO','SEQ_INCORPORACION_CONFLICTO',
        'SEQ_REPORTE_INSTITUCIONAL','SEQ_REPORTE_SNAPSHOT','SEQ_REPORTE_ARCHIVO',
        'SEQ_REPORTE_APROBACION','SEQ_REPORTE_DESTINATARIO','SEQ_REPORTE_REMISION',
        'SEQ_PROTOTIPO_PIIP','SEQ_PROTOTIPO_VALIDACION','SEQ_PROTOTIPO_HALLAZGO',
        'SEQ_MEDICION_EXPERIENCIA','SEQ_MEDICION_MUESTRA','SEQ_MATRIZ_META_RECORRIDO'
       )
 ORDER BY SEQUENCE_NAME;

PROMPT
PROMPT --- [2.3] Columnas incrementales/futuras ya presentes en tablas baseline
SELECT TABLE_NAME, COLUMN_NAME
  FROM USER_TAB_COLUMNS
 WHERE (TABLE_NAME = 'AUDITORIA_ACCESO' AND COLUMN_NAME IN
        ('ID_ROL_EFECTIVO','ID_UNIDAD_EFECTIVA','ID_ASIGNACION_EFECTIVA'))
    OR (TABLE_NAME = 'DOCUMENTO' AND COLUMN_NAME IN
        ('ID_DOCUMENTO_SERIE','CONTENIDO','FORMATO','CLASIFICACION_VALIDADA',
         'CLASIFICACION_FECHA','ID_USUARIO_VALIDA'))
    OR (TABLE_NAME = 'TIPO_DOCUMENTO' AND COLUMN_NAME IN
        ('CONTEXTO','CLASIFICACION_DEFECTO'))
    OR (TABLE_NAME = 'USUARIO' AND COLUMN_NAME IN ('LOGIN_SINTETICO'))
    OR (TABLE_NAME = 'USUARIO_ROL_UNIDAD' AND COLUMN_NAME IN
        ('FECHA_INICIO','FECHA_FIN','REVOCADA_EN','REVOCADA_POR','MOTIVO_REVOCACION',
         'INACTIVA_TEMPORALMENTE','ID_COMBINACION_MATRIZ','ID_DOCUMENTO_FORMAL','VERSION'))
    OR (TABLE_NAME = 'PROYECTO' AND COLUMN_NAME IN
        ('CODIGO_PREFIJO','DETALLE_FUENTE','PROBLEMA_PUBLICO','SOLUCION_PROPUESTA',
         'COMPONENTE_DIGITAL','DETALLE_COMPONENTE_DIGITAL','NOTA',
         'OBJETIVO_PEI_ID','ACTIVIDAD_POI_ID','VERSION','SUBSANACION_ACTIVA'))
 ORDER BY TABLE_NAME, COLUMN_NAME;

PROMPT
PROMPT --- [2.4] Indices incrementales/futuros ya presentes (inventario)
SELECT INDEX_NAME, TABLE_NAME
  FROM USER_INDEXES
 WHERE INDEX_NAME IN (
        'IDX_SI_EXPEDICION','IDX_SI_EXPIRACION',
        'IDX_AA_ROL_EFECTIVO','IDX_AA_UNIDAD_EFECTIVA',
        'IDX_DS_REGISTRO','IDX_DS_EXPEDIENTE','IDX_DS_TIPO','IDX_DOC_SERIE',
        'IDX_DCH_DOCUMENTO',
        'IDX_OP_VERSION_CODIGO','IDX_AP_VERSION_CODIGO',
        'UX_URU_ABIERTAS',
        'UX_PR_TITULAR_ACTIVO',
        'IDX_PROY_OBJETIVO_PEI','IDX_PROY_ACTIVIDAD_POI','IDX_PROY_COMPONENTE_DIGITAL',
        'IDX_OP_PROY_BUSQUEDA','IDX_OP_DOC_CLAS','IDX_OP_EI_UNIDAD_CLAS',
        'IDX_OP_CIC_PROY','IDX_OP_URU_COMB','IDX_OP_PRT_REC_EST'
       )
 ORDER BY INDEX_NAME;

-- ============================================================================
-- SECCION 3 - DOCUMENTO BLOB, tamanos y cadenas rotas
-- ----------------------------------------------------------------------------
-- Inspecciona la tabla DOCUMENTO: presencia de columna BLOB (aun no creada
-- en 001; debe ser inexistente), tamanos fisicos fuera del limite 25 MB
-- actual y problemas de cadena VERSION (ID_DOCUMENTO_ANTERIOR apunta a un
-- documento inexistente, a si mismo, o forma un ciclo). Tambien verifica
-- que el HASH_SHA256 cumple la longitud canonica y que los MIME
-- declarados pertenecen al dominio permitido.
-- ============================================================================

PROMPT
PROMPT ============================================================
PROMPT --- [3.1] Columna BLOB en DOCUMENTO (esperado: 0 columnas BLOB hoy)
SELECT COUNT(*) AS COLUMNAS_BLOB_EN_DOCUMENTO
  FROM USER_TAB_COLUMNS
 WHERE TABLE_NAME = 'DOCUMENTO'
   AND DATA_TYPE = 'BLOB';

PROMPT
PROMPT --- [3.2] Documentos fuera del limite actual 25 MB (necesitan revision)
SELECT ID_DOCUMENTO, ID_PROYECTO, ID_TIPO_DOC, NOMBRE_ORIGINAL, TAMANO_BYTES
  FROM DOCUMENTO
 WHERE TAMANO_BYTES > 26214400
 ORDER BY TAMANO_BYTES DESC;

PROMPT
PROMPT --- [3.3] Documentos con tamano cero o negativo
SELECT ID_DOCUMENTO, ID_PROYECTO, ID_TIPO_DOC, NOMBRE_ORIGINAL, TAMANO_BYTES
  FROM DOCUMENTO
 WHERE TAMANO_BYTES <= 0;

PROMPT
PROMPT --- [3.4] HASH_SHA256 con longitud distinta de 64 caracteres
SELECT ID_DOCUMENTO, HASH_SHA256, LENGTH(HASH_SHA256) AS LONGITUD
  FROM DOCUMENTO
 WHERE HASH_SHA256 IS NULL
    OR LENGTH(HASH_SHA256) <> 64;

PROMPT
PROMPT --- [3.5] HASH_SHA256 con caracteres no hexadecimales
SELECT ID_DOCUMENTO, HASH_SHA256
  FROM DOCUMENTO
 WHERE HASH_SHA256 IS NOT NULL
   AND NOT REGEXP_LIKE(HASH_SHA256, '^[0-9A-Fa-f]{64}$');

PROMPT
PROMPT --- [3.6] MIME_TYPES distintos del dominio canonico
SELECT ID_DOCUMENTO, MIME_TYPE
  FROM DOCUMENTO
 WHERE MIME_TYPE <> 'application/pdf'
   AND MIME_TYPE NOT LIKE 'application/vnd.openxmlformats-officedocument.%'
   AND MIME_TYPE NOT IN ('image/jpeg','image/png')
 ORDER BY MIME_TYPE;

PROMPT
PROMPT --- [3.7] Documentos cuyo ID_DOCUMENTO_ANTERIOR no existe
SELECT D.ID_DOCUMENTO,
       D.ID_PROYECTO,
       D.ID_TIPO_DOC,
       D.NUMERO_VERSION,
       D.ID_DOCUMENTO_ANTERIOR
  FROM DOCUMENTO D
 WHERE D.ID_DOCUMENTO_ANTERIOR IS NOT NULL
   AND NOT EXISTS (
         SELECT 1
           FROM DOCUMENTO A
          WHERE A.ID_DOCUMENTO = D.ID_DOCUMENTO_ANTERIOR
       )
 ORDER BY D.ID_DOCUMENTO;

PROMPT
PROMPT --- [3.8] Documentos que se apuntan a si mismos
SELECT ID_DOCUMENTO, ID_DOCUMENTO_ANTERIOR
  FROM DOCUMENTO
 WHERE ID_DOCUMENTO_ANTERIOR IS NOT NULL
   AND ID_DOCUMENTO_ANTERIOR = ID_DOCUMENTO;

PROMPT
PROMPT --- [3.9] Cadenas de version con profundidad sospechosa (>= 20 saltos)
WITH CADENA (ID_RAIZ, ID_ACTUAL, PROFUNDIDAD) AS (
    SELECT ID_DOCUMENTO, ID_DOCUMENTO, 1
      FROM DOCUMENTO
     WHERE ID_DOCUMENTO_ANTERIOR IS NOT NULL
    UNION ALL
    SELECT C.ID_RAIZ, D.ID_DOCUMENTO_ANTERIOR, C.PROFUNDIDAD + 1
      FROM CADENA C
      JOIN DOCUMENTO D ON D.ID_DOCUMENTO = C.ID_ACTUAL
     WHERE C.PROFUNDIDAD < 50
       AND D.ID_DOCUMENTO_ANTERIOR IS NOT NULL
)
SELECT ID_RAIZ, MAX(PROFUNDIDAD) AS PROFUNDIDAD_MAX
  FROM CADENA
 GROUP BY ID_RAIZ
HAVING MAX(PROFUNDIDAD) >= 20
 ORDER BY PROFUNDIDAD_MAX DESC, ID_RAIZ;

PROMPT
PROMPT --- [3.10] Documentos con NUMERO_VERSION inconsistente respecto a la cadena
SELECT D.ID_PROYECTO,
       D.ID_TIPO_DOC,
       D.ID_DOCUMENTO,
       D.NUMERO_VERSION,
       A.NUMERO_VERSION AS VERSION_ANTERIOR
  FROM DOCUMENTO D
  JOIN DOCUMENTO A ON A.ID_DOCUMENTO = D.ID_DOCUMENTO_ANTERIOR
 WHERE D.NUMERO_VERSION <= A.NUMERO_VERSION
 ORDER BY D.ID_PROYECTO, D.ID_TIPO_DOC, D.NUMERO_VERSION;

PROMPT
PROMPT --- [3.11] Documentos sin serie asignada (relevante para 003/004)
SELECT COUNT(*) AS DOCUMENTOS_SIN_SERIE
  FROM USER_TAB_COLUMNS UTC
  JOIN USER_TABLES UT ON UT.TABLE_NAME = UTC.TABLE_NAME
 WHERE UTC.TABLE_NAME = 'DOCUMENTO'
   AND UTC.COLUMN_NAME = 'ID_DOCUMENTO_SERIE';

PROMPT
PROMPT --- [3.12] Versionado sin correspondencia (mismo proyecto+tipo+version duplicado)
SELECT ID_PROYECTO, ID_TIPO_DOC, NUMERO_VERSION, COUNT(*) AS REPETICIONES
  FROM DOCUMENTO
 WHERE ACTIVO = 'S'
 GROUP BY ID_PROYECTO, ID_TIPO_DOC, NUMERO_VERSION
HAVING COUNT(*) > 1
 ORDER BY REPETICIONES DESC, ID_PROYECTO, ID_TIPO_DOC;

-- ============================================================================
-- SECCION 4 - Inexistencia historica de asignaciones GlobalAdmin
-- ----------------------------------------------------------------------------
-- La primera asignacion GlobalAdmin solo puede crearse mediante la
-- semilla 021, despues de confirmar las dependencias 002, 007 y 008. Esta
-- seccion verifica que no exista ninguna asignacion vigente, revocada o
-- historica que vincule un USUARIO con el rol GlobalAdmin. Cualquier fila
-- hallada bloquea la ejecucion de 021 y exige remediacion manual.
-- ============================================================================

PROMPT
PROMPT ============================================================
PROMPT --- [4.1] Asignaciones vigentes GlobalAdmin (esperado: 0)
SELECT URU.ID_USR_ROL_UNIDAD,
       URU.ID_USUARIO,
       URU.ID_ROL,
       URU.ID_UNIDAD,
       URU.ACTIVO,
       URU.FECHA_ASIGNACION,
       URU.ASIGNADO_POR
  FROM USUARIO_ROL_UNIDAD URU
  JOIN ROL ON ROL.ID_ROL = URU.ID_ROL
 WHERE ROL.NOMBRE_ROL = 'GlobalAdmin'
 ORDER BY URU.ID_USR_ROL_UNIDAD;

PROMPT
PROMPT --- [4.2] Cualquier asignacion historica GlobalAdmin (esperado: 0)
SELECT URU.ID_USR_ROL_UNIDAD,
       URU.ID_USUARIO,
       URU.ID_ROL,
       URU.ID_UNIDAD,
       URU.ACTIVO,
       URU.FECHA_ASIGNACION
  FROM USUARIO_ROL_UNIDAD URU
  JOIN ROL ON ROL.ID_ROL = URU.ID_ROL
 WHERE ROL.NOMBRE_ROL = 'GlobalAdmin'
 ORDER BY URU.ID_USR_ROL_UNIDAD;

PROMPT
PROMPT --- [4.3] Usuarios con rol GlobalAdmin asignado alguna vez (esperado: 0)
SELECT USUARIO.ID_USUARIO, USUARIO.KEYCLOAK_ID, USUARIO.LOGIN, USUARIO.CORREO
  FROM USUARIO
 WHERE EXISTS (
       SELECT 1
         FROM USUARIO_ROL_UNIDAD URU
         JOIN ROL ON ROL.ID_ROL = URU.ID_ROL
        WHERE URU.ID_USUARIO = USUARIO.ID_USUARIO
          AND ROL.NOMBRE_ROL = 'GlobalAdmin'
       )
 ORDER BY USUARIO.ID_USUARIO;

PROMPT
PROMPT --- [4.4] Eventos de auditoria que mencionan GlobalAdmin (referencia; esperado: 0)
SELECT TIPO_EVENTO, ENTIDAD_TIPO, ENTIDAD_ID, ID_USUARIO, FECHA_EVENTO
  FROM AUDITORIA_EVENTO
 WHERE LOWER(PAYLOAD_JSON) LIKE '%globaladmin%'
 ORDER BY FECHA_EVENTO DESC;

PROMPT
PROMPT --- [4.5] Tablas futuras 007/008/021 que ya deberian estar en cero
SELECT 'MATRIZ_FUNCIONAL_VERSION' AS OBJETO, COUNT(*) AS FILAS FROM USER_TABLES WHERE TABLE_NAME='MATRIZ_FUNCIONAL_VERSION'
UNION ALL
SELECT 'MATRIZ_FUNCION', COUNT(*) FROM USER_TABLES WHERE TABLE_NAME='MATRIZ_FUNCION'
UNION ALL
SELECT 'MATRIZ_FUNCION_PERFIL_UNIDAD', COUNT(*) FROM USER_TABLES WHERE TABLE_NAME='MATRIZ_FUNCION_PERFIL_UNIDAD'
UNION ALL
SELECT 'USUARIO_ROL_UNIDAD_EVENTO', COUNT(*) FROM USER_TABLES WHERE TABLE_NAME='USUARIO_ROL_UNIDAD_EVENTO'
UNION ALL
SELECT 'SUPLENCIA_FUNCIONAL', COUNT(*) FROM USER_TABLES WHERE TABLE_NAME='SUPLENCIA_FUNCIONAL'
UNION ALL
SELECT 'OPERACION_APROVISIONAMIENTO', COUNT(*) FROM USER_TABLES WHERE TABLE_NAME='OPERACION_APROVISIONAMIENTO';

-- ============================================================================
-- SECCION 5 - USUARIO_ROL_UNIDAD incompatibles con la futura matriz
-- ----------------------------------------------------------------------------
-- Cuando se cree la matriz funcional versionada (007), cada asignacion
-- debera coincidir con una combinacion funcion-perfil-unidad concreta.
-- Esta seccion lista las asignaciones actuales y anticipa cuales no
-- encajaran con la matriz 021. Se identifican los perfiles canonicos, las
-- unidades sin combinacion declarada, y los pares (ID_ROL, ID_UNIDAD) que
-- deberan mapearse a una combinacion de matriz.
-- ============================================================================

PROMPT
PROMPT ============================================================
PROMPT --- [5.1] Asignaciones activas por rol (perfil canonico)
SELECT ROL.NOMBRE_ROL, COUNT(*) AS ASIGNACIONES_ACTIVAS
  FROM USUARIO_ROL_UNIDAD URU
  JOIN ROL ON ROL.ID_ROL = URU.ID_ROL
 WHERE URU.ACTIVO = 'S'
 GROUP BY ROL.NOMBRE_ROL
 ORDER BY ROL.NOMBRE_ROL;

PROMPT
PROMPT --- [5.2] Asignaciones por unidad (para validar combinaciones futuras)
SELECT UE.CODIGO_UNIDAD, UE.NOMBRE, COUNT(*) AS ASIGNACIONES
  FROM USUARIO_ROL_UNIDAD URU
  JOIN UNIDAD_EJECUTORA UE ON UE.ID_UNIDAD = URU.ID_UNIDAD
 WHERE URU.ACTIVO = 'S'
 GROUP BY UE.CODIGO_UNIDAD, UE.NOMBRE
 ORDER BY ASIGNACIONES DESC, UE.CODIGO_UNIDAD;

PROMPT
PROMPT --- [5.3] Pares (ROL, UNIDAD) activos que deberan tener combinacion
SELECT ROL.NOMBRE_ROL,
       UE.CODIGO_UNIDAD,
       COUNT(*) AS ASIGNACIONES,
       LISTAGG(URU.ID_USR_ROL_UNIDAD, ',') WITHIN GROUP (ORDER BY URU.ID_USR_ROL_UNIDAD)
         AS IDS_ASIGNACIONES
  FROM USUARIO_ROL_UNIDAD URU
  JOIN ROL ON ROL.ID_ROL = URU.ID_ROL
  JOIN UNIDAD_EJECUTORA UE ON UE.ID_UNIDAD = URU.ID_UNIDAD
 WHERE URU.ACTIVO = 'S'
 GROUP BY ROL.NOMBRE_ROL, UE.CODIGO_UNIDAD
 ORDER BY ROL.NOMBRE_ROL, UE.CODIGO_UNIDAD;

PROMPT
PROMPT --- [5.4] Asignaciones con unidad inactiva (no aptas para combinacion)
SELECT URU.ID_USR_ROL_UNIDAD,
       URU.ID_USUARIO,
       URU.ID_ROL,
       URU.ID_UNIDAD,
       UE.ACTIVO AS UNIDAD_ACTIVA
  FROM USUARIO_ROL_UNIDAD URU
  JOIN UNIDAD_EJECUTORA UE ON UE.ID_UNIDAD = URU.ID_UNIDAD
 WHERE URU.ACTIVO = 'S'
   AND UE.ACTIVO = 'N';

PROMPT
PROMPT --- [5.5] Asignaciones duplicadas por usuario-rol-unidad (mas de una activa)
SELECT URU.ID_USUARIO, URU.ID_ROL, URU.ID_UNIDAD, COUNT(*) AS REPETICIONES
  FROM USUARIO_ROL_UNIDAD URU
 WHERE URU.ACTIVO = 'S'
 GROUP BY URU.ID_USUARIO, URU.ID_ROL, URU.ID_UNIDAD
HAVING COUNT(*) > 1
 ORDER BY REPETICIONES DESC;

PROMPT
PROMPT --- [5.6] Asignaciones con usuario inactivo (bloquean combinacion)
SELECT URU.ID_USR_ROL_UNIDAD,
       URU.ID_USUARIO,
       URU.ID_ROL,
       URU.ID_UNIDAD,
       USUARIO.ACTIVO
  FROM USUARIO_ROL_UNIDAD URU
  JOIN USUARIO ON USUARIO.ID_USUARIO = URU.ID_USUARIO
 WHERE URU.ACTIVO = 'S'
   AND USUARIO.ACTIVO = 'N';

-- ============================================================================
-- SECCION 6 - Inventario informativo de referencias legacy
-- ----------------------------------------------------------------------------
-- Inventaria referencias legacy para una futura migracion/corte expresamente
-- aprobada; no es condicion de ejecucion actual. Incluye: PROYECTO.OBJETIVO_PEI
-- y ACTIVIDAD_POI como texto no mapeable, ADMINISTRACION heredada,
-- unidades sin padre valido, proyectos sin unidad ejecutora, unidades
-- inactivas con asignaciones, y filas en PROYECTO_UNIDAD_ORGANICA que
-- deberan migrar a PROYECTO_RESPONSABLE. Tambien identifica documentos
-- con SCAN_ANTIVIRUS o NOMBRE_STORAGE no nulos que deberan quedar como
-- legacy nullable.
-- ============================================================================

PROMPT
PROMPT ============================================================
PROMPT --- [6.1] Proyectos con OBJETIVO_PEI textual (inventario futuro)
SELECT ID_PROYECTO, CODIGO, OBJETIVO_PEI
  FROM PROYECTO
 WHERE OBJETIVO_PEI IS NOT NULL
   AND TRIM(OBJETIVO_PEI) <> ''
 ORDER BY ID_PROYECTO;

PROMPT
PROMPT --- [6.2] Proyectos con ACTIVIDAD_POI textual (inventario futuro)
SELECT ID_PROYECTO, CODIGO, ACTIVIDAD_POI
  FROM PROYECTO
 WHERE ACTIVIDAD_POI IS NOT NULL
   AND TRIM(ACTIVIDAD_POI) <> ''
 ORDER BY ID_PROYECTO;

PROMPT
PROMPT --- [6.3] Proyectos con ADMINISTRACION fuera del dominio canonico
SELECT ID_PROYECTO, CODIGO, ADMINISTRACION
  FROM PROYECTO
 WHERE ADMINISTRACION IS NOT NULL
   AND ADMINISTRACION NOT IN ('OM','OGTI','OM-OGTI')
 ORDER BY ID_PROYECTO;

PROMPT
PROMPT --- [6.4] Proyectos con ADMINISTRACION NULL (inventario legacy)
SELECT COUNT(*) AS PROYECTOS_ADMINISTRACION_NULL
  FROM PROYECTO
 WHERE ADMINISTRACION IS NULL;

PROMPT
PROMPT --- [6.5] Unidades sin padre valido (nivel > 1 sin ID_UNIDAD_PADRE)
SELECT ID_UNIDAD,
       CODIGO_UNIDAD,
       NOMBRE,
       NIVEL_JERARQUICO,
       ID_UNIDAD_PADRE,
       ACTIVO
  FROM UNIDAD_EJECUTORA
 WHERE NIVEL_JERARQUICO > 1
   AND ID_UNIDAD_PADRE IS NULL
 ORDER BY ID_UNIDAD;

PROMPT
PROMPT --- [6.6] Unidades con padre inexistente (ID_UNIDAD_PADRE no existe)
SELECT UE.ID_UNIDAD,
       UE.CODIGO_UNIDAD,
       UE.NOMBRE,
       UE.NIVEL_JERARQUICO,
       UE.ID_UNIDAD_PADRE
  FROM UNIDAD_EJECUTORA UE
 WHERE UE.ID_UNIDAD_PADRE IS NOT NULL
   AND NOT EXISTS (
         SELECT 1
           FROM UNIDAD_EJECUTORA PADRE
          WHERE PADRE.ID_UNIDAD = UE.ID_UNIDAD_PADRE
       )
 ORDER BY UE.ID_UNIDAD;

PROMPT
PROMPT --- [6.7] Unidades raiz adicionales (esperado: 1 - MIDAGRI)
SELECT ID_UNIDAD, CODIGO_UNIDAD, NOMBRE, NIVEL_JERARQUICO, ID_UNIDAD_PADRE, ACTIVO
  FROM UNIDAD_EJECUTORA
 WHERE ID_UNIDAD_PADRE IS NULL
 ORDER BY ID_UNIDAD;

PROMPT
PROMPT --- [6.8] Proyectos sin unidad ejecutora valida
SELECT P.ID_PROYECTO,
       P.CODIGO,
       P.ID_UNIDAD_EJECUTORA,
       P.ESTADO
  FROM PROYECTO P
 WHERE P.ID_UNIDAD_EJECUTORA IS NULL
    OR NOT EXISTS (
        SELECT 1
          FROM UNIDAD_EJECUTORA UE
         WHERE UE.ID_UNIDAD = P.ID_UNIDAD_EJECUTORA
      )
 ORDER BY P.ID_PROYECTO;

PROMPT
PROMPT --- [6.9] Proyectos sin responsable valido
SELECT P.ID_PROYECTO,
       P.CODIGO,
       P.ID_RESPONSABLE,
       P.ESTADO
  FROM PROYECTO P
 WHERE P.ID_RESPONSABLE IS NULL
    OR NOT EXISTS (
        SELECT 1
          FROM USUARIO U
         WHERE U.ID_USUARIO = P.ID_RESPONSABLE
      )
 ORDER BY P.ID_PROYECTO;

PROMPT
PROMPT --- [6.10] Filas en PROYECTO_UNIDAD_ORGANICA (a migrar a 011/012)
SELECT COUNT(*) AS FILAS_PUO
  FROM PROYECTO_UNIDAD_ORGANICA;

PROMPT
PROMPT --- [6.11] Filas en PROYECTO_UNIDAD_ORGANICA sin proyecto valido
SELECT PUO.ID_PROY_UO,
       PUO.ID_PROYECTO,
       PUO.NRO_ORDEN,
       PUO.DESCRIPCION,
       PUO.ABREVIATURA
  FROM PROYECTO_UNIDAD_ORGANICA PUO
 WHERE NOT EXISTS (
       SELECT 1
         FROM PROYECTO P
        WHERE P.ID_PROYECTO = PUO.ID_PROYECTO
       )
 ORDER BY PUO.ID_PROY_UO;

PROMPT
PROMPT --- [6.12] Documentos con SCAN_ANTIVIRUS no nulo (inventario legacy)
SELECT COUNT(*) AS DOCUMENTOS_CON_SCAN_ANTIVIRUS
  FROM DOCUMENTO
 WHERE SCAN_ANTIVIRUS IS NOT NULL;

PROMPT
PROMPT --- [6.13] Documentos con NOMBRE_STORAGE no nulo (inventario legacy)
SELECT COUNT(*) AS DOCUMENTOS_CON_NOMBRE_STORAGE
  FROM DOCUMENTO
 WHERE NOMBRE_STORAGE IS NOT NULL;

PROMPT
PROMPT --- [6.14] Transiciones permitidas activas que rompen la matriz canonica
SELECT ID_TRANS_PERM, ESTADO_ORIGEN, ESTADO_DESTINO, ID_ROL_REQUERIDO, ACTIVO
  FROM TRANSICION_PERMITIDA
 WHERE ACTIVO = 'S'
   AND (
        (ESTADO_ORIGEN = 'INICIATIVA_ARCHIVADA' AND ESTADO_DESTINO = 'PRESENTADO')
     OR (ESTADO_DESTINO NOT IN ('PRESENTADO','INICIATIVA_APROBADA','INICIATIVA_ARCHIVADA',
                                'PROYECTO_EJECUCION','PRODUCTO_APROBADO','PRODUCTO_NO_APROBADO',
                                'SUSPENDIDO','CANCELADO'))
     OR (ESTADO_ORIGEN NOT IN ('PRESENTADO','INICIATIVA_APROBADA','INICIATIVA_ARCHIVADA',
                                'PROYECTO_EJECUCION','PRODUCTO_APROBADO','PRODUCTO_NO_APROBADO',
                                'SUSPENDIDO','CANCELADO'))
   )
 ORDER BY ID_TRANS_PERM;

PROMPT
PROMPT --- [6.15] Filas de SECUENCIA_CODIGO con anio fuera del rango canonico
SELECT ID_SECUENCIA, ANIO, ID_UNIDAD, ULTIMO_NUMERO
  FROM SECUENCIA_CODIGO
 WHERE ANIO < 2024;

PROMPT
PROMPT --- [6.16] Documentos con version igual a 1 y SIN referencia anterior (cadena incompleta)
SELECT ID_DOCUMENTO,
       ID_PROYECTO,
       ID_TIPO_DOC,
       NUMERO_VERSION,
       ID_DOCUMENTO_ANTERIOR
  FROM DOCUMENTO
 WHERE NUMERO_VERSION > 1
   AND ID_DOCUMENTO_ANTERIOR IS NULL
 ORDER BY ID_DOCUMENTO;

PROMPT
PROMPT --- [6.17] Conteo de proyectos por estado (referencia futura)
SELECT ESTADO, COUNT(*) AS TOTAL
  FROM PROYECTO
 GROUP BY ESTADO
 ORDER BY ESTADO;

PROMPT
PROMPT --- [6.18] Conteo de filas por tabla baseline (huella general)
SELECT 'UNIDAD_EJECUTORA' AS TABLA, COUNT(*) AS FILAS FROM UNIDAD_EJECUTORA
UNION ALL SELECT 'USUARIO', COUNT(*) FROM USUARIO
UNION ALL SELECT 'ROL', COUNT(*) FROM ROL
UNION ALL SELECT 'USUARIO_ROL_UNIDAD', COUNT(*) FROM USUARIO_ROL_UNIDAD
UNION ALL SELECT 'PROYECTO', COUNT(*) FROM PROYECTO
UNION ALL SELECT 'PROYECTO_UNIDAD_ORGANICA', COUNT(*) FROM PROYECTO_UNIDAD_ORGANICA
UNION ALL SELECT 'TRANSICION_PERMITIDA', COUNT(*) FROM TRANSICION_PERMITIDA
UNION ALL SELECT 'TIPO_DOCUMENTO', COUNT(*) FROM TIPO_DOCUMENTO
UNION ALL SELECT 'DOCUMENTO', COUNT(*) FROM DOCUMENTO
UNION ALL SELECT 'TRANSICION_ESTADO', COUNT(*) FROM TRANSICION_ESTADO
UNION ALL SELECT 'SECUENCIA_CODIGO', COUNT(*) FROM SECUENCIA_CODIGO
UNION ALL SELECT 'AUDITORIA_ACCESO', COUNT(*) FROM AUDITORIA_ACCESO
UNION ALL SELECT 'AUDITORIA_EVENTO', COUNT(*) FROM AUDITORIA_EVENTO
ORDER BY TABLA;

PROMPT
PROMPT ============================================================
PROMPT PIIP - Fin del diagnostico preflight del alcance activo
PROMPT Recuerde: este script NO modifica el esquema.
PROMPT Adjunte la salida al expediente de aprobacion del diseno fisico.
PROMPT ============================================================
PROMPT