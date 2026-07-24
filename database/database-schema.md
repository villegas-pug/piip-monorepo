# Catálogo de tablas Oracle - KALLPA_PIIP

- Fecha generación: 2026-07-23 (incorpora 002, 003, 004, 005+005.1, 006, 007,
  008 parcial, corrección 008.1, 009, 010, 011, 012, 013, 014+014.1, 015, 016, 017, 025, 026 y 027
  confirmados; 018 permanece diferido; 019-024 son semillas/cargas
  VIGENTES sin objetos estructurales adicionales)
- Esquema origen: KALLPA_PIIP
- Fuentes del catálogo vigente: `database/ddl/init/001_baseline_piip.sql`,
  `database/ddl/auditoria/002_auditoria_idempotencia.sql`,
  `database/ddl/documentos/003_expediente_serie_version.sql`,
  `database/ddl/documentos/004_documento_publicacion.sql`,
  `database/ddl/organizacion/005_objetivo_pei_versionado.sql` y
  `database/ddl/organizacion/005.1_objetivo_pei_versionado_indice.sql`,
  `database/ddl/organizacion/006_actividad_poi_versionada.sql`,
  `database/ddl/seguridad/007_matriz_funcional_versionada.sql`,
  `database/ddl/seguridad/008_usuario_rol_unidad_vigencia.sql` (DDL parcial confirmado) y
  `database/ddl/seguridad/008.1_secuencias_vigencia.sql` (corrección forward-only VIGENTE),
  `database/ddl/portafolio/009_proyecto_campos_oficiales.sql`,
   `database/ddl/portafolio/010_iniciativa_proyecto_relacion.sql`,
   `database/ddl/portafolio/011_proyecto_unidades_responsables.sql`,
   `database/ddl/portafolio/012_responsables_participantes.sql` (reconciliación histórica confirmada),
   `database/ddl/portafolio/013_clasificacion_campos.sql`,
  `database/ddl/portafolio/014_evaluacion_transiciones.sql` y
  `database/ddl/portafolio/014.1_subsanacion_iniciativa_plazo.sql`,
  `database/ddl/portafolio/015_ciclos_resultados_cierre.sql`,
  `database/ddl/portafolio/016_incorporacion_individual.sql`,
  `database/ddl/reportes/017_reporte_expediente_remision.sql`,
   `database/ddl/portafolio/025_ciclo_presentacion_evidencia_version.sql`,
   `database/ddl/portafolio/026_incorporacion_registro_observacion_version.sql` y
   `database/ddl/seguridad/027_operacion_aprovisionamiento_unidad_objetivo.sql`.
- Estado de actualización: solo incorpora scripts `VIGENTE` confirmados en
  `database/CHANGELOG.md`; no representa scripts pendientes.

## Resumen

- Total tablas: 54
- Total columnas: 517

### Distribución por tipo de dato

| Tipo | Cantidad |
|---|---:|
| BLOB | 1 |
| CHAR | 50 |
| CLOB | 12 |
| DATE | 43 |
| NUMBER | 174 |
| TIMESTAMP(6) | 46 |
| VARCHAR2 | 190 |

## Detalle por tabla

### AUDITORIA_ACCESO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_AUDIT | NUMBER | 22 | 15 | 0 | N | |
| ID_USUARIO | NUMBER | 22 | 10 | 0 | Y | |
| ENDPOINT | VARCHAR2 | 300 | | | N | |
| METODO_HTTP | VARCHAR2 | 10 | | | N | |
| CODIGO_RESPUESTA | NUMBER | 22 | 3 | 0 | N | |
| IP_CLIENTE | VARCHAR2 | 45 | | | N | |
| FECHA_HORA | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| DURACION_MS | NUMBER | 22 | 8 | 0 | Y | |
| ID_ROL_EFECTIVO | NUMBER | 22 | 5 | 0 | Y | |
| ID_UNIDAD_EFECTIVA | NUMBER | 22 | 10 | 0 | Y | |
| ID_ASIGNACION_EFECTIVA | NUMBER | 22 | 10 | 0 | Y | |

### AUDITORIA_EVENTO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_EVENTO | NUMBER | 22 | 15 | 0 | N | |
| TIPO_EVENTO | VARCHAR2 | 100 | | | N | |
| ENTIDAD_TIPO | VARCHAR2 | 50 | | | N | |
| ENTIDAD_ID | NUMBER | 22 | 15 | 0 | N | |
| PAYLOAD_JSON | CLOB | | | | N | |
| ID_USUARIO | NUMBER | 22 | 10 | 0 | Y | |
| FECHA_EVENTO | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| PROCESADO | CHAR | 1 | | | N | 'N' |

### CAT_ACTIVIDAD_POI_VERSION

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_VERSION | NUMBER | 22 | 10 | 0 | N | |
| CODIGO_VERSION | VARCHAR2 | 30 | | | N | |
| ID_VERSION_ANTERIOR | NUMBER | 22 | 10 | 0 | Y | |
| ID_DOCUMENTO_APROBACION | NUMBER | 22 | 12 | 0 | N | |
| OFICINA_APROBADORA | VARCHAR2 | 200 | | | N | |
| VIGENTE_DESDE | DATE | 7 | | | N | |
| VIGENTE_HASTA | DATE | 7 | | | Y | |
| ACTIVA | CHAR | 1 | | | N | 'S' |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### CAT_ACTIVIDAD_POI

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_ACTIVIDAD | NUMBER | 22 | 10 | 0 | N | |
| ID_VERSION | NUMBER | 22 | 10 | 0 | N | |
| CODIGO | VARCHAR2 | 30 | | | N | |
| DESCRIPCION | VARCHAR2 | 500 | | | N | |
| VIGENTE_DESDE | DATE | 7 | | | N | |
| VIGENTE_HASTA | DATE | 7 | | | Y | |
| ACTIVO | CHAR | 1 | | | N | 'S' |

### CAT_OBJETIVO_PEI_VERSION

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_VERSION | NUMBER | 22 | 10 | 0 | N | |
| CODIGO_VERSION | VARCHAR2 | 30 | | | N | |
| ID_VERSION_ANTERIOR | NUMBER | 22 | 10 | 0 | Y | |
| ID_DOCUMENTO_APROBACION | NUMBER | 22 | 12 | 0 | N | |
| OFICINA_APROBADORA | VARCHAR2 | 200 | | | N | |
| VIGENTE_DESDE | DATE | 7 | | | N | |
| VIGENTE_HASTA | DATE | 7 | | | Y | |
| ACTIVA | CHAR | 1 | | | N | 'S' |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### CAT_OBJETIVO_PEI

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_OBJETIVO | NUMBER | 22 | 10 | 0 | N | |
| ID_VERSION | NUMBER | 22 | 10 | 0 | N | |
| CODIGO | VARCHAR2 | 30 | | | N | |
| DESCRIPCION | VARCHAR2 | 500 | | | N | |
| VIGENTE_DESDE | DATE | 7 | | | N | |
| VIGENTE_HASTA | DATE | 7 | | | Y | |
| ACTIVO | CHAR | 1 | | | N | 'S' |

### DOCUMENTO_SERIE

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_SERIE | NUMBER | 22 | 12 | 0 | N | |
| ID_TIPO_DOC | NUMBER | 22 | 5 | 0 | N | |
| ID_REGISTRO | NUMBER | 22 | 12 | 0 | Y | |
| ID_EXPEDIENTE | NUMBER | 22 | 12 | 0 | Y | |
| TITULO | VARCHAR2 | 500 | | | N | |
| CLASIFICACION_PROPUESTA | VARCHAR2 | 20 | | | N | |
| CLASIFICACION_VALIDADA | VARCHAR2 | 20 | | | Y | |
| ACTIVA | CHAR | 1 | | | N | 'S' |
| VERSION | NUMBER | 22 | 10 | 0 | N | 0 |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### DOCUMENTO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_DOCUMENTO | NUMBER | 22 | 12 | 0 | N | |
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | Y | |
| ID_TIPO_DOC | NUMBER | 22 | 5 | 0 | N | |
| ESTADO_AL_CARGAR | VARCHAR2 | 30 | | | Y | |
| NOMBRE_ORIGINAL | VARCHAR2 | 500 | | | N | |
| NOMBRE_STORAGE | VARCHAR2 | 700 | | | Y | |
| MIME_TYPE | VARCHAR2 | 100 | | | N | |
| TAMANO_BYTES | NUMBER | 22 | 12 | 0 | N | |
| HASH_SHA256 | VARCHAR2 | 64 | | | N | |
| ID_USUARIO_CARGA | NUMBER | 22 | 10 | 0 | N | |
| FECHA_CARGA | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| ACTIVO | CHAR | 1 | | | N | 'S' |
| INMUTABLE | CHAR | 1 | | | N | 'N' |
| SCAN_ANTIVIRUS | VARCHAR2 | 10 | | | Y | |
| NUMERO_VERSION | NUMBER | 22 | 5 | 0 | N | 1 |
| ID_DOCUMENTO_ANTERIOR | NUMBER | 22 | 12 | 0 | Y | |
| CLASIFICACION | VARCHAR2 | 50 | | | Y | |
| CONTENIDO | BLOB | | | | Y | |
| FORMATO | VARCHAR2 | 20 | | | Y | |
| ID_DOCUMENTO_SERIE | NUMBER | 22 | 12 | 0 | Y | |
| CLASIFICACION_VALIDADA | VARCHAR2 | 20 | | | Y | |
| CLASIFICACION_FECHA | TIMESTAMP(6) | 11 | | 6 | Y | |
| ID_USUARIO_VALIDA | NUMBER | 22 | 10 | 0 | Y | |

> **Resolución 003.2**: `ID_PROYECTO` y `ESTADO_AL_CARGAR` son nullable desde
> `003.2_documento_propietario_institucional.sql` (VIGENTE). La corrección habilita versiones de
> series con propietario `EXPEDIENTE_INSTITUCIONAL` sin valores de portafolio ficticios y conserva
> las filas legacy existentes con proyecto y estado válidos.
>
> **Resolución 004**: `CLASIFICACION_VALIDADA`, `CLASIFICACION_FECHA` e `ID_USUARIO_VALIDA`
> se agregaron en `004_documento_publicacion.sql` (VIGENTE) como columnas nullable.
> `CLASIFICACION_VALIDADA` admite los dominios `PUBLICO`, `INTERNO` o `RESTRINGIDO` mediante
> `CK_DOC_CLAS_VALIDADA`; `ID_USUARIO_VALIDA` queda enlazado a `USUARIO` por `FK_DOC_USUARIO_VALIDA`.

### DOCUMENTO_CLASIFICACION_HIST

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_HISTORIAL | NUMBER | 22 | 12 | 0 | N | |
| ID_DOCUMENTO | NUMBER | 22 | 12 | 0 | N | |
| CLASIFICACION_ANTERIOR | VARCHAR2 | 20 | | | Y | |
| CLASIFICACION_NUEVA | VARCHAR2 | 20 | | | N | |
| ID_AUTORIDAD_DECISORA | NUMBER | 22 | 10 | 0 | N | |
| ID_EVALUADOR_REGISTRADOR | NUMBER | 22 | 10 | 0 | N | |
| ID_DOCUMENTO_DECISION | NUMBER | 22 | 12 | 0 | Y | |
| MOTIVO | VARCHAR2 | 2000 | | | Y | |
| FECHA_CAMBIO | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| RESULTADO | VARCHAR2 | 20 | | | N | |

> Historial append-only de reclasificaciones documentales introducido en
> `004_documento_publicacion.sql` (VIGENTE). `CK_DCH_RESTRICTIVA` garantiza que
> la nueva clasificación sea igual o más restrictiva que la anterior, salvo que
> se trate del alta inicial (`CLASIFICACION_ANTERIOR IS NULL`).
> `CK_DCH_AUTORIDAD_DISTINTA_EVALUADOR` exige segregación entre decisor y
> registrador; `CK_DCH_RESULTADO` admite `APLICADA`, `RECHAZADA` o `REVERTIDA`.

### DOCUMENTO_PUBLICACION

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_PUBLICACION | NUMBER | 22 | 12 | 0 | N | |
| ID_DOCUMENTO | NUMBER | 22 | 12 | 0 | N | |
| TITULO_PUBLICO | VARCHAR2 | 500 | | | N | |
| ID_EVALUADOR_CONFIRMADOR | NUMBER | 22 | 10 | 0 | N | |
| ID_ASIGNACION_EFECTIVA | NUMBER | 22 | 10 | 0 | N | |
| FECHA_PUBLICACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

> Tabla append-only de publicaciones documentales introducida en
> `004_documento_publicacion.sql` (VIGENTE). `UK_DP_DOCUMENTO` garantiza una
> única publicación por documento. `CK_DP_FORMATO_TITULO` exige `TITULO_PUBLICO`
> sin `@` ni secuencias de 9 a 12 dígitos consecutivos, para evitar correos y
> teléfonos en el título público.

### EXPEDIENTE_INSTITUCIONAL

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_EXPEDIENTE | NUMBER | 22 | 12 | 0 | N | |
| CODIGO | VARCHAR2 | 30 | | | N | |
| ASUNTO | VARCHAR2 | 500 | | | N | |
| MODULO_ORIGEN | VARCHAR2 | 50 | | | N | |
| REFERENCIA_CASO_USO | VARCHAR2 | 100 | | | N | |
| ID_UNIDAD | NUMBER | 22 | 10 | 0 | Y | |
| CLASIFICACION | VARCHAR2 | 20 | | | N | |
| ACTIVO | CHAR | 1 | | | N | 'S' |
| VERSION | NUMBER | 22 | 10 | 0 | N | 0 |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| MODIFICADO_POR | VARCHAR2 | 100 | | | Y | |
| FECHA_MODIFICACION | TIMESTAMP(6) | 11 | | 6 | Y | |

### MATRIZ_FUNCIONAL_VERSION

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_VERSION | NUMBER | 22 | 10 | 0 | N | |
| CODIGO_VERSION | VARCHAR2 | 30 | | | N | |
| ID_VERSION_ANTERIOR | NUMBER | 22 | 10 | 0 | Y | |
| ID_DOCUMENTO_APROBACION | NUMBER | 22 | 12 | 0 | N | |
| VIGENTE_DESDE | DATE | 7 | | | N | |
| VIGENTE_HASTA | DATE | 7 | | | Y | |
| ACTIVA | CHAR | 1 | | | N | 'S' |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### MATRIZ_FUNCION

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_FUNCION | NUMBER | 22 | 10 | 0 | N | |
| ID_VERSION | NUMBER | 22 | 10 | 0 | N | |
| CODIGO | VARCHAR2 | 30 | | | N | |
| DESCRIPCION | VARCHAR2 | 500 | | | N | |
| ACTIVA | CHAR | 1 | | | N | 'S' |

### MATRIZ_FUNCION_PERFIL_UNIDAD

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_COMBINACION | NUMBER | 22 | 12 | 0 | N | |
| ID_VERSION | NUMBER | 22 | 10 | 0 | N | |
| ID_FUNCION | NUMBER | 22 | 10 | 0 | N | |
| ID_ROL | NUMBER | 22 | 5 | 0 | N | |
| ID_UNIDAD | NUMBER | 22 | 10 | 0 | N | |
| ID_APROBADOR | NUMBER | 22 | 10 | 0 | N | |
| ID_REGISTRADOR | NUMBER | 22 | 10 | 0 | N | |
| ID_DOCUMENTO_APROBACION | NUMBER | 22 | 12 | 0 | N | |
| VIGENTE_DESDE | DATE | 7 | | | N | |
| VIGENTE_HASTA | DATE | 7 | | | Y | |
| ACTIVA | CHAR | 1 | | | N | 'S' |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### OPERACION_APROVISIONAMIENTO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_OPERACION | NUMBER | 22 | 12 | 0 | N | |
| CLAVE_IDEMPOTENTE | VARCHAR2 | 100 | | | N | |
| HASH_PAYLOAD | VARCHAR2 | 64 | | | N | |
| ID_USUARIO_OBJETIVO | NUMBER | 22 | 10 | 0 | Y | |
| KEYCLOAK_ID | VARCHAR2 | 36 | | | Y | |
| ESTADO_TECNICO | VARCHAR2 | 30 | | | N | |
| INTENTO | NUMBER | 22 | 3 | 0 | N | 1 |
| ERROR_RECUPERABLE | CHAR | 1 | | | N | 'N' |
| RESULTADO_ORACLE | VARCHAR2 | 2000 | | | Y | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| FECHA_CIERRE | TIMESTAMP(6) | 11 | | 6 | Y | |
| ID_UNIDAD_OBJETIVO | NUMBER | 22 | 10 | 0 | Y | |

> **Resolución 027**: `ID_UNIDAD_OBJETIVO` (`NUMBER(10)`, nullable, sin default) fue agregada en
> `027_operacion_aprovisionamiento_unidad_objetivo.sql` (VIGENTE). Permite revalidar el alcance
> exacto solicitado en consulta y reintento; se enlaza con `UNIDAD_EJECUTORA` mediante
> `FK_OA_UNIDAD_OBJETIVO` y se localiza con `IDX_OA_UNIDAD_OBJETIVO`. La obligatoriedad para
> nuevas operaciones ordinarias la exige el servicio de seguridad; no se fuerza `NOT NULL` para
> preservar las filas históricas existentes sin unidad inventada.

### PROYECTO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | N | |
| CODIGO | VARCHAR2 | 25 | | | N | |
| CODIGO_ORIGEN | VARCHAR2 | 50 | | | Y | |
| TIPO_REGISTRO | VARCHAR2 | 20 | | | N | |
| NOMBRE | VARCHAR2 | 500 | | | N | |
| TIPO_SOLUCION | VARCHAR2 | 30 | | | N | |
| FUENTE_ORIGEN | VARCHAR2 | 50 | | | N | |
| DESCRIPCION | CLOB | | | | N | |
| OBJETIVO_PEI | VARCHAR2 | 500 | | | N | |
| ACTIVIDAD_POI | VARCHAR2 | 500 | | | N | |
| ADMINISTRACION | VARCHAR2 | 10 | | | Y | |
| ESTADO | VARCHAR2 | 30 | | | N | 'PRESENTADO' |
| TIPO_PRODUCTO_FINAL | VARCHAR2 | 40 | | | Y | |
| RESULTADOS_CLAVE | CLOB | | | | Y | |
| FECHA_INICIO | DATE | 7 | | | N | SYSDATE |
| FECHA_CIERRE | DATE | 7 | | | Y | |
| ID_UNIDAD_EJECUTORA | NUMBER | 22 | 10 | 0 | N | |
| ID_RESPONSABLE | NUMBER | 22 | 10 | 0 | N | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| MODIFICADO_POR | VARCHAR2 | 100 | | | Y | |
| FECHA_MODIFICACION | TIMESTAMP(6) | 11 | | 6 | Y | |
| CODIGO_PREFIJO | VARCHAR2 | 20 | | | Y | |
| DETALLE_FUENTE | VARCHAR2 | 500 | | | Y | |
| PROBLEMA_PUBLICO | VARCHAR2 | 2000 | | | Y | |
| SOLUCION_PROPUESTA | VARCHAR2 | 2000 | | | Y | |
| COMPONENTE_DIGITAL | CHAR | 1 | | | N | 'N' |
| DETALLE_COMPONENTE_DIGITAL | VARCHAR2 | 500 | | | Y | |
| NOTA | VARCHAR2 | 1000 | | | Y | |
| OBJETIVO_PEI_ID | NUMBER | 22 | 10 | 0 | Y | |
| ACTIVIDAD_POI_ID | NUMBER | 22 | 10 | 0 | Y | |
| VERSION | NUMBER | 22 | 10 | 0 | N | 0 |
| SUBSANACION_ACTIVA | CHAR | 1 | | | N | 'N' |

> **Resolución 009**: `ADMINISTRACION` se volvió nullable y se agregaron
> `CODIGO_PREFIJO`, `DETALLE_FUENTE`, `PROBLEMA_PUBLICO`, `SOLUCION_PROPUESTA`,
> `COMPONENTE_DIGITAL`, `DETALLE_COMPONENTE_DIGITAL`, `NOTA`, `OBJETIVO_PEI_ID`,
> `ACTIVIDAD_POI_ID`, `VERSION` y `SUBSANACION_ACTIVA` desde
> `009_proyecto_campos_oficiales.sql` (VIGENTE). `FK_PROY_OBJETIVO_PEI` y
> `FK_PROY_ACTIVIDAD_POI` se crearon `ENABLE NOVALIDATE` hacia
> `CAT_OBJETIVO_PEI`/`CAT_ACTIVIDAD_POI`. `CK_PROY_COMPONENTE_DIGITAL`,
> `CK_PROY_DETALLE_COMPONENTE` y `CK_PROY_SUBSANACION_ACTIVA` se agregaron
> como CHECKs nuevos.

### INICIATIVA_PROYECTO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_RELACION | NUMBER | 22 | 12 | 0 | N | |
| ID_INICIATIVA | NUMBER | 22 | 12 | 0 | N | |
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | N | |
| CREADA_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

> Relación M:N iniciativa–proyecto introducida en
> `010_iniciativa_proyecto_relacion.sql` (VIGENTE). `PK_INICIATIVA_PROYECTO`,
> `UK_IP_INICIATIVA`, `UK_IP_PROYECTO`, `FK_IP_INICIATIVA`, `FK_IP_PROYECTO` y
> `CK_IP_DISTINTOS` (`ID_INICIATIVA <> ID_PROYECTO`).

### PROYECTO_RESPONSABLE

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_TITULARIDAD | NUMBER | 22 | 12 | 0 | N | |
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | N | |
| ID_USUARIO | NUMBER | 22 | 10 | 0 | N | |
| INICIO | DATE | 7 | | | N | |
| FIN | DATE | 7 | | | Y | |
| MOTIVO_SUSTITUCION | VARCHAR2 | 2000 | | | Y | |
| ID_ACTOR_SUSTITUCION | NUMBER | 22 | 10 | 0 | Y | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

> Titularidad vigente de proyecto introducida en
> `011_proyecto_unidades_responsables.sql` (VIGENTE). `PK_PROYECTO_RESPONSABLE`,
> `FK_PR_PROYECTO`, `FK_PR_USUARIO`, `FK_PR_ACTOR` y `CK_PR_VIGENCIA`
> (`FIN IS NULL OR FIN >= INICIO`).

### PARTICIPANTE_PERSONA

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_PARTICIPANTE | NUMBER | 22 | 12 | 0 | N | |
| ID_USUARIO | NUMBER | 22 | 10 | 0 | Y | |
| NOMBRES_COMPLETOS | VARCHAR2 | 300 | | | N | |
| INSTITUCION | VARCHAR2 | 200 | | | Y | |
| FUNCION | VARCHAR2 | 200 | | | Y | |
| CLASIFICACION | VARCHAR2 | 20 | | | N | 'RESTRINGIDO' |

### PROYECTO_PARTICIPANTE_PERSONA

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_PROY_PART_PERSONA | NUMBER | 22 | 12 | 0 | N | |
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | N | |
| ID_PARTICIPANTE | NUMBER | 22 | 12 | 0 | N | |
| INICIO | DATE | 7 | | | N | |
| FIN | DATE | 7 | | | Y | |
| ID_ACTOR | NUMBER | 22 | 10 | 0 | N | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### PROYECTO_PARTICIPANTE_UNIDAD

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_PROY_PART_UNIDAD | NUMBER | 22 | 12 | 0 | N | |
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | N | |
| ID_UNIDAD | NUMBER | 22 | 10 | 0 | N | |
| INICIO | DATE | 7 | | | N | |
| FIN | DATE | 7 | | | Y | |
| ID_ACTOR | NUMBER | 22 | 10 | 0 | N | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

> **Resolución 012**: huella confirmada por reconciliación histórica, sin nueva ejecución del
> DDL. `CK_PP_CLASIFICACION` limita `CLASIFICACION` a `PUBLICO`, `INTERNO` o `RESTRINGIDO`;
> `CK_PP_DATOS_MINIMOS`, `CK_PPP_VIGENCIA` y `CK_PPU_VIGENCIA` permanecen habilitados.

### PROYECTO_CAMPO_CLASIFICACION

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_CLASIFICACION | NUMBER | 22 | 12 | 0 | N | |
| TIPO_REGISTRO | VARCHAR2 | 20 | | | N | |
| ETAPA | VARCHAR2 | 30 | | | N | |
| NRO_CAMPO | NUMBER | 22 | 3 | 0 | N | |
| CLASIFICACION | VARCHAR2 | 20 | | | N | |
| EDITABLE | CHAR | 1 | | | N | 'S' |
| ID_ROL_EDITOR | NUMBER | 22 | 5 | 0 | N | |
| OBLIGATORIO | CHAR | 1 | | | N | 'N' |
| ACTIVA | CHAR | 1 | | | N | 'S' |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### PROYECTO_CAMPO_CLASIF_HIST

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_HISTORIAL | NUMBER | 22 | 12 | 0 | N | |
| ID_CLASIFICACION | NUMBER | 22 | 12 | 0 | N | |
| CLASIFICACION_ANTERIOR | VARCHAR2 | 20 | | | Y | |
| CLASIFICACION_NUEVA | VARCHAR2 | 20 | | | N | |
| EDITABLE_ANTERIOR | CHAR | 1 | | | Y | |
| EDITABLE_NUEVO | CHAR | 1 | | | Y | |
| OBLIGATORIO_ANTERIOR | CHAR | 1 | | | Y | |
| OBLIGATORIO_NUEVO | CHAR | 1 | | | Y | |
| ID_ACTOR | NUMBER | 22 | 10 | 0 | N | |
| FECHA_CAMBIO | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| MOTIVO | VARCHAR2 | 2000 | | | Y | |
| ID_DOCUMENTO_DECISION | NUMBER | 22 | 12 | 0 | Y | |

> **Resolución 013**: matriz append-only de clasificación de campos
> (`PROYECTO_CAMPO_CLASIFICACION` y su historial `PROYECTO_CAMPO_CLASIF_HIST`)
> introducida en `013_clasificacion_campos.sql` (VIGENTE). `UK_PCC_TIPO_ETAPA_CAMPO`
> garantiza unicidad por tipo/etapa/nro_campo. CHECKs validan dominios
> `PUBLICO`/`INTERNO`/`RESTRINGIDO` y flags `S`/`N`.

### EVALUACION_INICIATIVA

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_EVALUACION | NUMBER | 22 | 12 | 0 | N | |
| ID_INICIATIVA | NUMBER | 22 | 12 | 0 | N | |
| ID_EVALUADOR | NUMBER | 22 | 10 | 0 | N | |
| ID_ROL_EFECTIVO | NUMBER | 22 | 5 | 0 | Y | |
| ID_UNIDAD_EFECTIVA | NUMBER | 22 | 10 | 0 | Y | |
| FECHA_EVALUACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| OBSERVACIONES | VARCHAR2 | 2000 | | | Y | |
| ID_DOCUMENTO_OPINION | NUMBER | 22 | 12 | 0 | Y | |

> **Resolución 014 / 014.1**: tabla de evaluación introducida por
> `014_evaluacion_transiciones.sql` (VIGENTE vía 014.1). `PK_EVALUACION_INICIATIVA`,
> `UK_EI_INICIATIVA`, `FK_EI_INICIATIVA`, `FK_EI_EVALUADOR`, `FK_EI_ROL_EFECTIVO`,
> `FK_EI_UNIDAD_EFECTIVA`, `FK_EI_DOCUMENTO_OPINION` y `CK_EI_OBSERVACION_LONGITUD`.

### SUBSANACION_INICIATIVA

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_SUBSANACION | NUMBER | 22 | 12 | 0 | N | |
| ID_INICIATIVA | NUMBER | 22 | 12 | 0 | N | |
| PLAZO | DATE | 7 | | | N | |
| INCUMPLIMIENTOS | CLOB | | | | N | |
| APERTURA_EN | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| ATENCION_EN | TIMESTAMP(6) | 11 | | 6 | Y | |
| ID_ACTOR | NUMBER | 22 | 10 | 0 | N | |

> **Resolución 014 / 014.1**: tabla de subsanación introducida por
> `014_evaluacion_transiciones.sql` (VIGENTE vía 014.1). `PK_SUBSANACION_INICIATIVA`,
> `UK_SI_INICIATIVA`, `FK_SI_INICIATIVA`, `FK_SI_ACTOR` y la invariante
> determinista `CK_SI_PLAZO` (`PLAZO IS NULL OR APERTURA_EN IS NULL OR
> PLAZO > APERTURA_EN`) que reemplaza la versión con `TRUNC(SYSDATE)` rechazada
> por ORA-02436.

### APLICABILIDAD_INICIATIVA

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_APLICABILIDAD | NUMBER | 22 | 12 | 0 | N | |
| ID_INICIATIVA | NUMBER | 22 | 12 | 0 | N | |
| RESULTADO | VARCHAR2 | 20 | | | N | |
| MOTIVO | VARCHAR2 | 2000 | | | Y | |
| ID_EVALUADOR | NUMBER | 22 | 10 | 0 | N | |
| FECHA | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

> **Resolución 014 / 014.1**: aplicabilidad introducida por
> `014_evaluacion_transiciones.sql` (VIGENTE vía 014.1). `PK_APLICABILIDAD_INICIATIVA`,
> `UK_AI_INICIATIVA`, `FK_AI_INICIATIVA`, `FK_AI_EVALUADOR`, `CK_AI_RESULTADO`
> y `CK_AI_MOTIVO` (inviariante determinista entre columnas persistidas).

### APLICABILIDAD_CRITERIO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_CRITERIO | NUMBER | 22 | 12 | 0 | N | |
| ID_APLICABILIDAD | NUMBER | 22 | 12 | 0 | N | |
| CLAVE | VARCHAR2 | 50 | | | N | |
| VALOR | VARCHAR2 | 500 | | | N | |
| ORDEN | NUMBER | 22 | 3 | 0 | N | |

> **Resolución 014 / 014.1**: criterios de aplicabilidad introducidos por
> `014_evaluacion_transiciones.sql` (VIGENTE vía 014.1). `PK_APLICABILIDAD_CRITERIO`,
> `UK_AC_APLICABILIDAD_CLAVE`, `FK_AC_APLICABILIDAD` y `CK_AC_ORDEN`.

### PLANIFICACION_PROYECTO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_PLANIFICACION | NUMBER | 22 | 12 | 0 | N | |
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | N | |
| ALCANCE | VARCHAR2 | 2000 | | | Y | |
| OBJETIVOS | VARCHAR2 | 2000 | | | Y | |
| ENTREGABLES | CLOB | | | | Y | |
| PERIODOS | CLOB | | | | Y | |
| VERSION | NUMBER | 22 | 3 | 0 | N | |
| ID_VERSION_ANTERIOR | NUMBER | 22 | 12 | 0 | Y | |
| CERRADA | CHAR | 1 | | | N | 'N' |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### CICLO_PROYECTO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_CICLO | NUMBER | 22 | 12 | 0 | N | |
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | N | |
| PERIODO | VARCHAR2 | 20 | | | N | |
| NUMERO_VERSION | NUMBER | 22 | 3 | 0 | N | |
| ID_VERSION_ANTERIOR | NUMBER | 22 | 12 | 0 | Y | |
| OBJETIVOS | VARCHAR2 | 2000 | | | Y | |
| ACTIVIDADES | VARCHAR2 | 2000 | | | Y | |
| AVANCE | NUMBER | 22 | 5 | 2 | Y | |
| DIFICULTADES | VARCHAR2 | 2000 | | | Y | |
| PROXIMAS_ACCIONES | VARCHAR2 | 2000 | | | Y | |
| CERRADO | CHAR | 1 | | | N | 'N' |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| FECHA_CIERRE | TIMESTAMP(6) | 11 | | 6 | Y | |

### CICLO_PROYECTO_VERSION

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_CICLO_VERSION | NUMBER | 22 | 12 | 0 | N | |
| ID_CICLO | NUMBER | 22 | 12 | 0 | N | |
| NUMERO_VERSION | NUMBER | 22 | 3 | 0 | N | |
| ID_VERSION_ANTERIOR | NUMBER | 22 | 12 | 0 | Y | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### CICLO_EVIDENCIA

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_EVIDENCIA | NUMBER | 22 | 12 | 0 | N | |
| ID_CICLO | NUMBER | 22 | 12 | 0 | N | |
| ID_DOCUMENTO | NUMBER | 22 | 12 | 0 | N | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### PRODUCTO_PARCIAL

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_PRODUCTO | NUMBER | 22 | 12 | 0 | N | |
| ID_CICLO | NUMBER | 22 | 12 | 0 | N | |
| DESCRIPCION | VARCHAR2 | 2000 | | | N | |
| RESULTADO | CLOB | | | | Y | |
| FECHA | DATE | 7 | | | N | |
| ID_RESPONSABLE | NUMBER | 22 | 10 | 0 | N | |
| VERSION | NUMBER | 22 | 3 | 0 | N | |
| ID_VERSION_ANTERIOR | NUMBER | 22 | 12 | 0 | Y | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### PRESENTACION_PRODUCTO_FINAL

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_PRESENTACION | NUMBER | 22 | 12 | 0 | N | |
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | N | |
| VERSION | NUMBER | 22 | 3 | 0 | N | |
| ID_VERSION_ANTERIOR | NUMBER | 22 | 12 | 0 | Y | |
| DESCRIPCION | VARCHAR2 | 2000 | | | N | |
| ID_RESPONSABLE | NUMBER | 22 | 10 | 0 | N | |
| ID_DOCUMENTO_SUSTENTA | NUMBER | 22 | 12 | 0 | N | |
| FECHA_PRESENTACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### PRESENTACION_PRODUCTO_FINAL_EVIDENCIA

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_EVIDENCIA | NUMBER | 22 | 12 | 0 | N | |
| ID_PRESENTACION | NUMBER | 22 | 12 | 0 | N | |
| ID_DOCUMENTO | NUMBER | 22 | 12 | 0 | N | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### VALIDACION_RESULTADO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_VALIDACION | NUMBER | 22 | 12 | 0 | N | |
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | N | |
| ID_RESPONSABLE | NUMBER | 22 | 10 | 0 | N | |
| ID_EVALUADOR | NUMBER | 22 | 10 | 0 | N | |
| RESULTADOS_CLAVE | CLOB | | | | Y | |
| VALIDADO_EN | TIMESTAMP(6) | 11 | | 6 | Y | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### CIERRE_PROYECTO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_CIERRE | NUMBER | 22 | 12 | 0 | N | |
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | N | |
| INFORME_FINAL | CLOB | | | | Y | |
| RESULTADOS | CLOB | | | | Y | |
| APRENDIZAJES | CLOB | | | | Y | |
| CONCLUSION | VARCHAR2 | 2000 | | | Y | |
| OBSERVACION | VARCHAR2 | 2000 | | | Y | |
| ID_EVALUADOR | NUMBER | 22 | 10 | 0 | N | |
| FECHA_CIERRE | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

> **Resolución 015**: planificación, ciclos, evidencia, productos parciales,
> presentación del producto final, validación de resultados y cierre
> introducidos en `015_ciclos_resultados_cierre.sql` (VIGENTE). UKs por
> `(ID_PROYECTO, VERSION)` en `PLANIFICACION_PROYECTO`, `PRESENTACION_PRODUCTO_FINAL`,
> `(ID_PROYECTO, PERIODO, NUMERO_VERSION)` en `CICLO_PROYECTO`,
> `(ID_CICLO, VERSION)` en `PRODUCTO_PARCIAL`, `(ID_CICLO, ID_DOCUMENTO)` en
> `CICLO_EVIDENCIA` y `(ID_PROYECTO)` en `CIERRE_PROYECTO` aportan sus
> índices únicos canónicos. CHECKs `CK_CP_PERIODO`, `CK_CP_VERSION`,
> `CK_CP_AVANCE`, `CK_VR_ACTORES_DISTINTOS` y demás son invariantes
> deterministas a nivel de fila.

> **Resolución 025**: `CICLO_PROYECTO_VERSION` conserva versiones append-only
> de `CICLO_PROYECTO` mediante `PK_CICLO_PROYECTO_VERSION`,
> `UK_CPV_CICLO_VERSION`, `FK_CPV_CICLO`, `FK_CPV_VERSION_ANTERIOR` y
> `CK_CPV_VERSION_MIN`. `PRESENTACION_PRODUCTO_FINAL_EVIDENCIA` admite varias
> evidencias por presentación mediante `PK_PPF_EVIDENCIA`,
> `UK_PPFE_PRESENTACION_DOCUMENTO`, `FK_PPFE_PRESENTACION` y
> `FK_PPFE_DOCUMENTO`. Los triggers `TRG_CPV_APPEND_ONLY` y
> `TRG_PPFE_APPEND_ONLY` rechazan UPDATE y DELETE; los índices auxiliares son
> `IDX_CPV_VERSION_ANTERIOR` e `IDX_PPFE_DOCUMENTO`.

### INCORPORACION_REGISTRO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_INCORPORACION | NUMBER | 22 | 12 | 0 | N | |
| FUENTE | VARCHAR2 | 200 | | | N | |
| FECHA_FUENTE | DATE | 7 | | | N | |
| ID_RESPONSABLE | NUMBER | 22 | 10 | 0 | N | |
| ID_DOCUMENTO_FUENTE | NUMBER | 22 | 12 | 0 | N | |
| HASH_ORIGINAL | VARCHAR2 | 64 | | | N | |
| DATOS_ORIGINALES | CLOB | | | | Y | |
| ESTADO | VARCHAR2 | 20 | | | N | 'PENDIENTE' |
| ID_REGISTRO_VINCULADO | NUMBER | 22 | 12 | 0 | Y | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| OBSERVACION | VARCHAR2 | 2000 | | | Y | |
| VERSION | NUMBER | 22 | 10 | 0 | N | 0 |

### INCORPORACION_CAMBIO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_CAMBIO | NUMBER | 22 | 12 | 0 | N | |
| ID_INCORPORACION | NUMBER | 22 | 12 | 0 | N | |
| DATOS_ANTES | CLOB | | | | Y | |
| DATOS_DESPUES | CLOB | | | | Y | |
| MOTIVO | VARCHAR2 | 2000 | | | Y | |
| ID_ACTOR | NUMBER | 22 | 10 | 0 | N | |
| FECHA_CAMBIO | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### INCORPORACION_CONFLICTO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_CONFLICTO | NUMBER | 22 | 12 | 0 | N | |
| ID_INCORPORACION | NUMBER | 22 | 12 | 0 | N | |
| TIPO_CONFLICTO | VARCHAR2 | 30 | | | N | |
| ID_REGISTRO_CONFLICTIVO | NUMBER | 22 | 12 | 0 | N | |
| DESCRIPCION | VARCHAR2 | 2000 | | | Y | |
| RESUELTO | CHAR | 1 | | | N | 'N' |
| ID_RESOLUTOR | NUMBER | 22 | 10 | 0 | Y | |
| FECHA_RESOLUCION | TIMESTAMP(6) | 11 | | 6 | Y | |
| ID_DOCUMENTO_RESOLUCION | NUMBER | 22 | 12 | 0 | Y | |

> **Resolución 016**: incorporacion individual introducida en
> `016_incorporacion_individual.sql` (VIGENTE). `UK_IR_HASH_FUENTE_RESPONSABLE`
> garantiza idempotencia por hash; `UK_ICONF_INC_TIPO_REG` evita conflictos
> duplicados. `CK_IR_HASH` y `CK_RA_HASH` validan SHA-256; `CK_ICONF_RESOLUCION`
> es invariante determinista entre columnas persistidas.
>
> **Resolución 026**: `OBSERVACION` (`VARCHAR2(2000 CHAR)`, nullable, sin
> default) y `VERSION` (`NUMBER(10) DEFAULT 0 NOT NULL`) fueron agregadas por
> `026_incorporacion_registro_observacion_version.sql` (VIGENTE). `VERSION`
> soporta concurrencia optimista mediante `@Version`; el incremento no crea
> constraints, índices, secuencias ni triggers adicionales.

### REPORTE_INSTITUCIONAL

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_REPORTE | NUMBER | 22 | 12 | 0 | N | |
| TIPO | VARCHAR2 | 30 | | | N | |
| ANIO | NUMBER | 22 | 4 | 0 | N | |
| SEMESTRE | NUMBER | 22 | 1 | 0 | Y | |
| PERIODO | VARCHAR2 | 30 | | | N | |
| FECHA_CORTE | DATE | 7 | | | N | |
| PARAMETROS | CLOB | | | | Y | |
| ID_SNAPSHOT | NUMBER | 22 | 12 | 0 | Y | |
| VERSION_DATOS | NUMBER | 22 | 5 | 0 | Y | |
| CLASIFICACION | VARCHAR2 | 20 | | | N | |
| ID_GENERADOR | NUMBER | 22 | 10 | 0 | N | |
| FECHA_GENERACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| ESTADO_TECNICO | VARCHAR2 | 20 | | | N | |

### REPORTE_SNAPSHOT

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_SNAPSHOT | NUMBER | 22 | 12 | 0 | N | |
| PAYLOAD_JSON | CLOB | | | | N | |
| VERSION_ESQUEMA | NUMBER | 22 | 5 | 0 | Y | |
| HASH_SHA256 | VARCHAR2 | 64 | | | N | |
| FECHA_CORTE | DATE | 7 | | | N | |
| PARAMETROS | CLOB | | | | Y | |
| CLASIFICACION | VARCHAR2 | 20 | | | Y | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

> `REPORTE_SNAPSHOT.PAYLOAD_JSON` se valida como JSON mediante
> `CK_RS_PAYLOAD_JSON` y su `HASH_SHA256` se valida con `CK_RS_HASH` (regex
> de 64 hex). `UK_RS_HASH` aporta el índice único canónico sobre `HASH_SHA256`.
> `VERSION_ESQUEMA` registra la versión de esquema del payload.

### REPORTE_ARCHIVO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_ARCHIVO | NUMBER | 22 | 12 | 0 | N | |
| ID_REPORTE | NUMBER | 22 | 12 | 0 | N | |
| FORMATO | VARCHAR2 | 10 | | | N | |
| VERSION | NUMBER | 22 | 5 | 0 | N | |
| HASH_SHA256 | VARCHAR2 | 64 | | | N | |
| ID_DOCUMENTO_VERSION | NUMBER | 22 | 12 | 0 | N | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### REPORTE_APROBACION

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_APROBACION | NUMBER | 22 | 12 | 0 | N | |
| ID_REPORTE | NUMBER | 22 | 12 | 0 | N | |
| ID_VERSION | NUMBER | 22 | 5 | 0 | N | |
| ID_OFICINA | NUMBER | 22 | 10 | 0 | N | |
| ID_APROBADOR | NUMBER | 22 | 10 | 0 | N | |
| ID_DOCUMENTO_APROBACION | NUMBER | 22 | 12 | 0 | N | |
| FECHA_APROBACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### REPORTE_DESTINATARIO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_DESTINATARIO | NUMBER | 22 | 12 | 0 | N | |
| ID_APROBACION | NUMBER | 22 | 12 | 0 | N | |
| TIPO_DESTINATARIO | VARCHAR2 | 30 | | | N | |
| ID_ENTIDAD | NUMBER | 22 | 10 | 0 | N | |
| NOMBRE | VARCHAR2 | 200 | | | N | |

### REPORTE_REMISION

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_REMISION | NUMBER | 22 | 12 | 0 | N | |
| ID_REPORTE | NUMBER | 22 | 12 | 0 | N | |
| ID_DESTINATARIO | NUMBER | 22 | 12 | 0 | N | |
| FECHA_REMISION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| RESULTADO | VARCHAR2 | 20 | | | N | |
| MOTIVO | VARCHAR2 | 2000 | | | Y | |

> **Resolución 017**: ciclo de reportes institucionales introducido en
> `017_reporte_expediente_remision.sql` (VIGENTE). `FK_RE_SNAPSHOT` enlaza
> `REPORTE_INSTITUCIONAL` con `REPORTE_SNAPSHOT` (creada tras la tabla
> hija para evitar commit implícito cruzado). `CK_RS_PAYLOAD_JSON`,
> `CK_RS_HASH`, `CK_RE_CORTE` y demás son invariantes deterministas a
> nivel de fila, sin funciones no deterministas.

### PROYECTO_UNIDAD_ORGANICA

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_PROY_UO | NUMBER | 22 | 12 | 0 | N | |
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | N | |
| NRO_ORDEN | NUMBER | 22 | 3 | 0 | N | |
| DESCRIPCION | VARCHAR2 | 300 | | | N | |
| ABREVIATURA | VARCHAR2 | 20 | | | N | |

### ROL

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_ROL | NUMBER | 22 | 5 | 0 | N | |
| NOMBRE_ROL | VARCHAR2 | 50 | | | N | |
| DESCRIPCION | VARCHAR2 | 500 | | | Y | |
| NIVEL_ACCESO | NUMBER | 22 | 2 | 0 | N | |

### SECUENCIA_CODIGO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_SECUENCIA | NUMBER | 22 | 10 | 0 | N | |
| ANIO | NUMBER | 22 | 4 | 0 | N | |
| ID_UNIDAD | NUMBER | 22 | 10 | 0 | N | |
| ULTIMO_NUMERO | NUMBER | 22 | 5 | 0 | N | 0 |

### SOLICITUD_IDEMPOTENTE

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_SOLICITUD | NUMBER | 22 | 15 | 0 | N | |
| CONSUMIDOR | VARCHAR2 | 100 | | | N | |
| OPERACION | VARCHAR2 | 100 | | | N | |
| CLAVE | VARCHAR2 | 100 | | | N | |
| HASH_PAYLOAD | VARCHAR2 | 64 | | | N | |
| RECURSO_TIPO | VARCHAR2 | 50 | | | Y | |
| RECURSO_ID | NUMBER | 22 | 15 | 0 | Y | |
| RESPUESTA_JSON | CLOB | | | | Y | |
| ESTADO_TECNICO | VARCHAR2 | 20 | | | N | |
| FECHA_EXPEDICION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| FECHA_EXPIRACION | TIMESTAMP(6) | 11 | | 6 | N | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |

### SUPLENCIA_FUNCIONAL

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_SUPLENCIA | NUMBER | 22 | 12 | 0 | N | |
| ID_ASIGNACION_TITULAR | NUMBER | 22 | 10 | 0 | N | |
| ID_ASIGNACION_SUPLENTE | NUMBER | 22 | 10 | 0 | N | |
| INICIO | DATE | 7 | | | N | |
| FIN | DATE | 7 | | | N | |
| TERMINADA_EN | TIMESTAMP(6) | 11 | | 6 | Y | |
| ID_AUTORIDAD | NUMBER | 22 | 10 | 0 | N | |
| ID_DOCUMENTO_FORMAL | NUMBER | 22 | 12 | 0 | N | |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

### TIPO_DOCUMENTO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_TIPO_DOC | NUMBER | 22 | 5 | 0 | N | |
| NOMBRE | VARCHAR2 | 200 | | | N | |
| ESTADO_ASOCIADO | VARCHAR2 | 30 | | | Y | |
| OBLIGATORIO | CHAR | 1 | | | N | 'N' |
| DESCRIPCION | VARCHAR2 | 500 | | | Y | |
| ANEXO_NT | VARCHAR2 | 20 | | | Y | |
| ACTIVO | CHAR | 1 | | | N | 'S' |
| CONTEXTO | VARCHAR2 | 20 | | | N | |
| CLASIFICACION_DEFECTO | VARCHAR2 | 20 | | | Y | |

> **Resolución 003.1**: `ESTADO_ASOCIADO` es nullable desde
> `003.1_tipo_documento_contexto_nullable.sql` (VIGENTE). Junto con
> `CK_TD_ESTADO_CONTEXTO`, el esquema permite `ESTADO_ASOCIADO` obligatorio para
> `CONTEXTO='PORTAFOLIO'` y nulo para `CONTEXTO='INSTITUCIONAL'`.

### TRANSICION_ESTADO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_TRANSICION | NUMBER | 22 | 12 | 0 | N | |
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | N | |
| ESTADO_ANTERIOR | VARCHAR2 | 30 | | | N | |
| ESTADO_NUEVO | VARCHAR2 | 30 | | | N | |
| ID_USUARIO | NUMBER | 22 | 10 | 0 | N | |
| ID_ROL_EFECTIVO | NUMBER | 22 | 5 | 0 | N | |
| ID_UNIDAD_EFECTIVA | NUMBER | 22 | 10 | 0 | N | |
| FECHA_TRANSICION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| OBSERVACIONES | VARCHAR2 | 2000 | | | Y | |
| ID_DOCUMENTO_REF | NUMBER | 22 | 12 | 0 | Y | |

### TRANSICION_PERMITIDA

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_TRANS_PERM | NUMBER | 22 | 5 | 0 | N | |
| ESTADO_ORIGEN | VARCHAR2 | 30 | | | N | |
| ESTADO_DESTINO | VARCHAR2 | 30 | | | N | |
| ID_ROL_REQUERIDO | NUMBER | 22 | 5 | 0 | N | |
| DOC_OBLIGATORIO | CHAR | 1 | | | N | 'N' |
| OBS_OBLIGATORIO | CHAR | 1 | | | N | 'N' |
| ACTIVO | CHAR | 1 | | | N | 'S' |

### UNIDAD_EJECUTORA

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_UNIDAD | NUMBER | 22 | 10 | 0 | N | |
| CODIGO_UNIDAD | VARCHAR2 | 20 | | | N | |
| NOMBRE | VARCHAR2 | 200 | | | N | |
| DESCRIPCION | VARCHAR2 | 500 | | | Y | |
| NIVEL_JERARQUICO | NUMBER | 22 | 2 | 0 | N | |
| ID_UNIDAD_PADRE | NUMBER | 22 | 10 | 0 | Y | |
| ACTIVO | CHAR | 1 | | | N | 'S' |
| FECHA_ACTIVACION | DATE | 7 | | | N | SYSDATE |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| MODIFICADO_POR | VARCHAR2 | 100 | | | Y | |
| FECHA_MODIFICACION | TIMESTAMP(6) | 11 | | 6 | Y | |

### USUARIO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_USUARIO | NUMBER | 22 | 10 | 0 | N | |
| KEYCLOAK_ID | VARCHAR2 | 36 | | | N | |
| LOGIN | VARCHAR2 | 100 | | | Y | |
| NOMBRE_COMPLETO | VARCHAR2 | 300 | | | Y | |
| CORREO | VARCHAR2 | 200 | | | Y | |
| ACTIVO | CHAR | 1 | | | N | 'S' |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| LOGIN_SINTETICO | CHAR | 1 | | | N | 'N' |

### USUARIO_ROL_UNIDAD

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_USR_ROL_UNIDAD | NUMBER | 22 | 10 | 0 | N | |
| ID_USUARIO | NUMBER | 22 | 10 | 0 | N | |
| ID_ROL | NUMBER | 22 | 5 | 0 | N | |
| ID_UNIDAD | NUMBER | 22 | 10 | 0 | N | |
| ACTIVO | CHAR | 1 | | | N | 'S' |
| FECHA_ASIGNACION | DATE | 7 | | | N | SYSDATE |
| ASIGNADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_INICIO | DATE | 7 | | | N | SYSDATE |
| FECHA_FIN | DATE | 7 | | | Y | |
| REVOCADA_EN | TIMESTAMP(6) | 11 | | 6 | Y | |
| REVOCADA_POR | VARCHAR2 | 100 | | | Y | |
| MOTIVO_REVOCACION | VARCHAR2 | 2000 | | | Y | |
| INACTIVA_TEMPORALMENTE | CHAR | 1 | | | N | 'N' |
| ID_COMBINACION_MATRIZ | NUMBER | 22 | 12 | 0 | Y | |
| ID_DOCUMENTO_FORMAL | NUMBER | 22 | 12 | 0 | Y | |
| VERSION | NUMBER | 22 | 10 | 0 | N | 0 |

### USUARIO_ROL_UNIDAD_EVENTO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_EVENTO | NUMBER | 22 | 12 | 0 | N | |
| ID_ASIGNACION | NUMBER | 22 | 10 | 0 | N | |
| TIPO_EVENTO | VARCHAR2 | 30 | | | N | |
| ID_USUARIO_ACTOR | NUMBER | 22 | 10 | 0 | Y | |
| ID_ROL_ACTOR | NUMBER | 22 | 5 | 0 | Y | |
| ID_UNIDAD_ACTOR | NUMBER | 22 | 10 | 0 | Y | |
| FECHA_EVENTO | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| MOTIVO | VARCHAR2 | 2000 | | | Y | |
| ID_ASIGNACION_EFECTIVA | NUMBER | 22 | 10 | 0 | Y | |

## Objetos estructurales vigentes

Las definiciones detalladas, dependencias y validaciones de estos objetos están en los scripts
vigentes 001-008.1, 009-012, 013, 014+014.1, 015, 016, 017, 025, 026 y 027. El incremento 005 fue finalizado
por 005.1; el incremento 008 permanece `FALLIDO` como hecho histórico: sus DDL parciales
(tablas, columnas, constraints e índices) fueron completados exclusivamente por las tres
secuencias creadas en la corrección forward-only 008.1. El incremento 014 fue finalizado por
la corrección 014.1 con invariante determinista en `CK_SI_PLAZO`. Los cambios posteriores
deben quedar registrados en `database/CHANGELOG.md` antes de actualizar este catálogo.

### Secuencias

| Secuencia | Tabla asociada | Inicio | Cache |
|---|---|---:|---|
| SEQ_UNIDAD_EJECUTORA | UNIDAD_EJECUTORA | 2 | NOCACHE |
| SEQ_USUARIO | USUARIO | 1 | NOCACHE |
| SEQ_USUARIO_ROL_UNIDAD | USUARIO_ROL_UNIDAD | 1 | NOCACHE |
| SEQ_PROYECTO | PROYECTO | 1 | NOCACHE |
| SEQ_PROYECTO_UO | PROYECTO_UNIDAD_ORGANICA | 1 | NOCACHE |
| SEQ_TRANSICION_ESTADO | TRANSICION_ESTADO | 1 | NOCACHE |
| SEQ_DOCUMENTO | DOCUMENTO | 1 | NOCACHE |
| SEQ_SECUENCIA_CODIGO | SECUENCIA_CODIGO | 1 | NOCACHE |
| SEQ_AUDITORIA_ACCESO | AUDITORIA_ACCESO | 1 | CACHE 50 |
| SEQ_AUDITORIA_EVENTO | AUDITORIA_EVENTO | 1 | CACHE 50 |
| SEQ_SOLICITUD_IDEMPOTENTE | SOLICITUD_IDEMPOTENTE | 1 | NOCACHE |
| SEQ_EXPEDIENTE_INSTITUCIONAL | EXPEDIENTE_INSTITUCIONAL | 1 | NOCACHE |
| SEQ_DOCUMENTO_SERIE | DOCUMENTO_SERIE | 1 | NOCACHE |
| SEQ_DOCUMENTO_CLASIF_HIST | DOCUMENTO_CLASIFICACION_HIST | 1 | NOCACHE |
| SEQ_DOCUMENTO_PUBLICACION | DOCUMENTO_PUBLICACION | 1 | NOCACHE |
| SEQ_OBJETIVO_PEI_VERSION | CAT_OBJETIVO_PEI_VERSION | 1 | NOCACHE |
| SEQ_OBJETIVO_PEI | CAT_OBJETIVO_PEI | 1 | NOCACHE |
| SEQ_ACTIVIDAD_POI_VERSION | CAT_ACTIVIDAD_POI_VERSION | 1 | NOCACHE |
| SEQ_ACTIVIDAD_POI | CAT_ACTIVIDAD_POI | 1 | NOCACHE |
| SEQ_MATRIZ_VERSION | MATRIZ_FUNCIONAL_VERSION | 1 | NOCACHE |
| SEQ_MATRIZ_FUNCION | MATRIZ_FUNCION | 1 | NOCACHE |
| SEQ_MATRIZ_COMBINACION | MATRIZ_FUNCION_PERFIL_UNIDAD | 1 | NOCACHE |
| SEQ_URU_EVENTO | USUARIO_ROL_UNIDAD_EVENTO | 1 | NOCACHE |
| SEQ_SUPLENCIA_FUNCIONAL | SUPLENCIA_FUNCIONAL | 1 | NOCACHE |
| SEQ_OPERACION_APROVISIONAMIENTO | OPERACION_APROVISIONAMIENTO | 1 | NOCACHE |
| SEQ_INICIATIVA_PROYECTO | INICIATIVA_PROYECTO | 1 | NOCACHE |
| SEQ_PROYECTO_RESPONSABLE | PROYECTO_RESPONSABLE | 1 | NOCACHE |
| SEQ_PARTICIPANTE_PERSONA | PARTICIPANTE_PERSONA | 1 | NOCACHE |
| SEQ_PROY_PART_PERSONA | PROYECTO_PARTICIPANTE_PERSONA | 1 | NOCACHE |
| SEQ_PROY_PART_UNIDAD | PROYECTO_PARTICIPANTE_UNIDAD | 1 | NOCACHE |
| SEQ_PROY_CAMPO_CLASIF | PROYECTO_CAMPO_CLASIFICACION | 1 | NOCACHE |
| SEQ_PROY_CAMPO_CLASIF_HIST | PROYECTO_CAMPO_CLASIF_HIST | 1 | NOCACHE |
| SEQ_EVALUACION_INICIATIVA | EVALUACION_INICIATIVA | 1 | NOCACHE |
| SEQ_SUBSANACION_INICIATIVA | SUBSANACION_INICIATIVA | 1 | NOCACHE |
| SEQ_APLICABILIDAD_INICIATIVA | APLICABILIDAD_INICIATIVA | 1 | NOCACHE |
| SEQ_APLICABILIDAD_CRITERIO | APLICABILIDAD_CRITERIO | 1 | NOCACHE |
| SEQ_PLANIFICACION_PROYECTO | PLANIFICACION_PROYECTO | 1 | NOCACHE |
| SEQ_CICLO_PROYECTO | CICLO_PROYECTO | 1 | NOCACHE |
| SEQ_CICLO_PROYECTO_VERSION | CICLO_PROYECTO_VERSION | 1 | NOCACHE |
| SEQ_CICLO_EVIDENCIA | CICLO_EVIDENCIA | 1 | NOCACHE |
| SEQ_PRODUCTO_PARCIAL | PRODUCTO_PARCIAL | 1 | NOCACHE |
| SEQ_PRESENTACION_PRODUCTO_FINAL | PRESENTACION_PRODUCTO_FINAL | 1 | NOCACHE |
| SEQ_PPF_EVIDENCIA | PRESENTACION_PRODUCTO_FINAL_EVIDENCIA | 1 | NOCACHE |
| SEQ_VALIDACION_RESULTADO | VALIDACION_RESULTADO | 1 | NOCACHE |
| SEQ_CIERRE_PROYECTO | CIERRE_PROYECTO | 1 | NOCACHE |
| SEQ_INCORPORACION_REGISTRO | INCORPORACION_REGISTRO | 1 | NOCACHE |
| SEQ_INCORPORACION_CAMBIO | INCORPORACION_CAMBIO | 1 | NOCACHE |
| SEQ_INCORPORACION_CONFLICTO | INCORPORACION_CONFLICTO | 1 | NOCACHE |
| SEQ_REPORTE_INSTITUCIONAL | REPORTE_INSTITUCIONAL | 1 | NOCACHE |
| SEQ_REPORTE_SNAPSHOT | REPORTE_SNAPSHOT | 1 | NOCACHE |
| SEQ_REPORTE_ARCHIVO | REPORTE_ARCHIVO | 1 | NOCACHE |
| SEQ_REPORTE_APROBACION | REPORTE_APROBACION | 1 | NOCACHE |
| SEQ_REPORTE_DESTINATARIO | REPORTE_DESTINATARIO | 1 | NOCACHE |
| SEQ_REPORTE_REMISION | REPORTE_REMISION | 1 | NOCACHE |

### Restricciones

| Tipo | Convención | Estado |
|---|---|---|
| Primary key | `PK_<OBJETO>` | Vigente en las 54 tablas |
| Unique | `UK_<OBJETO>` | Vigente para identificadores de negocio, combinaciones, series, publicaciones, catálogos PEI/POI, matriz, iniciativa–proyecto, clasificación de campos, evaluación/subsanación/aplicabilidad, planificación/ciclos/cierre, incorporación y reportes institucionales |
| Foreign key | `FK_<OBJETO>` | Vigente según 001-017 y 025; `FK_URU_COMBINACION` sigue `ENABLE NOVALIDATE` y `FK_PROY_OBJETIVO_PEI`/`FK_PROY_ACTIVIDAD_POI` también siguen `ENABLE NOVALIDATE` hasta una futura migración/corte expresamente aprobada |
| Check | `CK_<OBJETO>` | Vigente para dominios, formatos, vigencias, flags, propiedad XOR, transiciones restrictivas, formato de títulos públicos, coherencia de componente digital, invariantes deterministas de subsanación/resultados/cierre, versión de ciclo y estados técnicos de 001-017 y 025 |

### Restricciones incorporadas por incrementos confirmados

| Incremento | Tabla | PK / UK / FK / CHECK incorporados o modificados |
|---:|---|---|
| 003 | EXPEDIENTE_INSTITUCIONAL | `PK_EXPEDIENTE_INSTITUCIONAL`, `UK_EI_CODIGO`, `FK_EI_UNIDAD`, `CK_EI_ACTIVO`, `CK_EI_CLASIFICACION`, `CK_EI_MODULO` |
| 003 | DOCUMENTO_SERIE | `PK_DOCUMENTO_SERIE`, `UK_DS_TITULO_TIPO`, `FK_DS_TIPO_DOC`, `FK_DS_REGISTRO`, `FK_DS_EXPEDIENTE`, `CK_DS_XOR_DUENIO`, `CK_DS_CLAS_PROPUESTA`, `CK_DS_CLAS_VALIDADA`, `CK_DS_ACTIVA` |
| 003 | TIPO_DOCUMENTO / DOCUMENTO | `CK_TD_CONTEXTO`, `CK_TD_CLAS_DEFECTO`, `CK_TD_ESTADO_CONTEXTO`, `FK_DOC_SERIE`, `CK_DOC_TAMANO` (100 MB); `CK_DOC_SCAN` eliminado |
| 004 | DOCUMENTO_CLASIFICACION_HIST | `PK_DOCUMENTO_CLASIFICACION_HIST`, `FK_DCH_DOCUMENTO`, `FK_DCH_AUTORIDAD`, `FK_DCH_EVALUADOR`, `FK_DCH_DOCUMENTO_DECISION`, `CK_DCH_CLAS_NUEVA`, `CK_DCH_CLAS_ANT`, `CK_DCH_RESTRICTIVA`, `CK_DCH_RESULTADO`, `CK_DCH_AUTORIDAD_DISTINTA_EVALUADOR` |
| 004 | DOCUMENTO_PUBLICACION | `PK_DOCUMENTO_PUBLICACION`, `UK_DP_DOCUMENTO`, `FK_DP_DOCUMENTO`, `FK_DP_EVALUADOR`, `CK_DP_FORMATO_TITULO` |
| 004 | DOCUMENTO | `FK_DOC_USUARIO_VALIDA`, `CK_DOC_CLAS_VALIDADA` (sobre `CLASIFICACION_VALIDADA`) |
| 005 / 005.1 | CAT_OBJETIVO_PEI_VERSION | `PK_CAT_OBJETIVO_PEI_VERSION`, `UK_OPV_CODIGO`, `FK_OPV_VERSION_ANTERIOR`, `FK_OPV_DOCUMENTO`, `CK_OPV_VIGENCIA`, `CK_OPV_ACTIVA` |
| 005 / 005.1 | CAT_OBJETIVO_PEI | `PK_CAT_OBJETIVO_PEI`, `UK_OP_VERSION_CODIGO`, `FK_OP_VERSION`, `CK_OP_VIGENCIA`, `CK_OP_ACTIVO`; 005.1 confirmó el índice único canónico de respaldo de la UK |
| 006 | CAT_ACTIVIDAD_POI_VERSION | `PK_CAT_ACTIVIDAD_POI_VERSION`, `UK_APV_CODIGO`, `FK_APV_VERSION_ANTERIOR`, `FK_APV_DOCUMENTO`, `CK_APV_VIGENCIA`, `CK_APV_ACTIVA` |
| 006 | CAT_ACTIVIDAD_POI | `PK_CAT_ACTIVIDAD_POI`, `UK_AP_VERSION_CODIGO`, `FK_AP_VERSION`, `CK_AP_VIGENCIA`, `CK_AP_ACTIVO`; la UK respalda el índice único canónico |
| 007 | MATRIZ_FUNCIONAL_VERSION | `PK_MATRIZ_FUNCIONAL_VERSION`, `UK_MFV_CODIGO`, `FK_MFV_VERSION_ANTERIOR`, `FK_MFV_DOCUMENTO`, `CK_MFV_VIGENCIA`, `CK_MFV_ACTIVA` |
| 007 | MATRIZ_FUNCION | `PK_MATRIZ_FUNCION`, `UK_MF_VERSION_CODIGO`, `FK_MF_VERSION`, `CK_MF_ACTIVA` |
| 007 | MATRIZ_FUNCION_PERFIL_UNIDAD | `PK_MATRIZ_COMBINACION`, `UK_MFPU_VERSION_FUNCION_PERFIL_UNIDAD`, `FK_MFPU_VERSION`, `FK_MFPU_FUNCION`, `FK_MFPU_ROL`, `FK_MFPU_UNIDAD`, `FK_MFPU_APROBADOR`, `FK_MFPU_REGISTRADOR`, `FK_MFPU_DOCUMENTO`, `CK_MFPU_APROBADOR_DISTINTO_REGISTRADOR`, `CK_MFPU_VIGENCIA`, `CK_MFPU_ACTIVA` |
| 008 | USUARIO / AUDITORIA_ACCESO / USUARIO_ROL_UNIDAD | `CK_USR_LOGIN_SINTETICO`, `FK_AA_ROL_EFECTIVO`, `FK_AA_UNIDAD_EFECTIVA`, `CK_URU_VIGENCIA`, `CK_URU_REVOCADA`, `CK_URU_INACTIVA_TEMP`, `FK_URU_COMBINACION` (`ENABLE NOVALIDATE`), `FK_URU_DOCUMENTO_FORMAL` |
| 008 | USUARIO_ROL_UNIDAD_EVENTO | `PK_USUARIO_ROL_UNIDAD_EVENTO`, `FK_URUE_ASIGNACION`, `FK_URUE_USUARIO_ACTOR`, `CK_URUE_TIPO_EVENTO` |
| 008 | SUPLENCIA_FUNCIONAL | `PK_SUPLENCIA_FUNCIONAL`, `UK_SF_TITULAR_INICIO`, `FK_SF_TITULAR`, `FK_SF_SUPLENTE`, `FK_SF_AUTORIDAD`, `FK_SF_DOCUMENTO`, `CK_SF_DISTINTAS`, `CK_SF_VIGENCIA` |
| 008 | OPERACION_APROVISIONAMIENTO | `PK_OPERACION_APROVISIONAMIENTO`, `UK_OA_CLAVE`, `FK_OA_USUARIO_OBJETIVO`, `CK_OA_ESTADO`, `CK_OA_ERROR_RECUPERABLE`, `CK_OA_HASH` |
| 027 | OPERACION_APROVISIONAMIENTO | `FK_OA_UNIDAD_OBJETIVO` hacia `UNIDAD_EJECUTORA(ID_UNIDAD)`, índice `IDX_OA_UNIDAD_OBJETIVO` y columna `ID_UNIDAD_OBJETIVO NUMBER(10)` nullable para revalidación de alcance en consulta y reintento |
| 009 | PROYECTO | `FK_PROY_OBJETIVO_PEI`, `FK_PROY_ACTIVIDAD_POI` (ambas `ENABLE NOVALIDATE`), `CK_PROY_COMPONENTE_DIGITAL`, `CK_PROY_DETALLE_COMPONENTE`, `CK_PROY_SUBSANACION_ACTIVA` |
| 010 | INICIATIVA_PROYECTO | `PK_INICIATIVA_PROYECTO`, `UK_IP_INICIATIVA`, `UK_IP_PROYECTO`, `FK_IP_INICIATIVA`, `FK_IP_PROYECTO`, `CK_IP_DISTINTOS`; las UK aportan los índices únicos canónicos y no se crean `IDX_IP_INICIATIVA`/`IDX_IP_PROYECTO` redundantes |
| 011 | PROYECTO_RESPONSABLE | `PK_PROYECTO_RESPONSABLE`, `FK_PR_PROYECTO`, `FK_PR_USUARIO`, `FK_PR_ACTOR`, `CK_PR_VIGENCIA` |
| 012 | PARTICIPANTE_PERSONA | `PK_PARTICIPANTE_PERSONA`, `FK_PP_USUARIO`, `CK_PP_CLASIFICACION`, `CK_PP_DATOS_MINIMOS` |
| 012 | PROYECTO_PARTICIPANTE_PERSONA | `PK_PROYECTO_PARTICIPANTE_PERSONA`, `UK_PPP_PROY_PART`, `FK_PPP_PROYECTO`, `FK_PPP_PARTICIPANTE`, `FK_PPP_ACTOR`, `CK_PPP_VIGENCIA` |
| 012 | PROYECTO_PARTICIPANTE_UNIDAD | `PK_PROYECTO_PARTICIPANTE_UNIDAD`, `UK_PPU_PROY_UNI`, `FK_PPU_PROYECTO`, `FK_PPU_UNIDAD`, `FK_PPU_ACTOR`, `CK_PPU_VIGENCIA` |
| 013 | PROYECTO_CAMPO_CLASIFICACION | `PK_PROYECTO_CAMPO_CLASIFICACION`, `UK_PCC_TIPO_ETAPA_CAMPO`, `FK_PCC_ROL_EDITOR`, `CK_PCC_TIPO_REGISTRO`, `CK_PCC_CLASIFICACION`, `CK_PCC_EDITABLE`, `CK_PCC_OBLIGATORIO`, `CK_PCC_ACTIVA`, `CK_PCC_NRO_CAMPO` |
| 013 | PROYECTO_CAMPO_CLASIF_HIST | `PK_PROYECTO_CAMPO_CLASIF_HIST`, `FK_PCCH_CLASIFICACION`, `FK_PCCH_ACTOR`, `FK_PCCH_DOCUMENTO_DECISION`, `CK_PCCH_CLAS_NUEVA`, `CK_PCCH_CLAS_ANT`, `CK_PCCH_EDITABLE_NUEVO`, `CK_PCCH_EDITABLE_ANT`, `CK_PCCH_OBLIGATORIO_NUEVO`, `CK_PCCH_OBLIGATORIO_ANT` |
| 014 / 014.1 | EVALUACION_INICIATIVA | `PK_EVALUACION_INICIATIVA`, `UK_EI_INICIATIVA`, `FK_EI_INICIATIVA`, `FK_EI_EVALUADOR`, `FK_EI_ROL_EFECTIVO`, `FK_EI_UNIDAD_EFECTIVA`, `FK_EI_DOCUMENTO_OPINION`, `CK_EI_OBSERVACION_LONGITUD` |
| 014 / 014.1 | SUBSANACION_INICIATIVA | `PK_SUBSANACION_INICIATIVA`, `UK_SI_INICIATIVA`, `FK_SI_INICIATIVA`, `FK_SI_ACTOR`, `CK_SI_PLAZO` (inviariante determinista `PLAZO IS NULL OR APERTURA_EN IS NULL OR PLAZO > APERTURA_EN` introducida por 014.1) |
| 014 / 014.1 | APLICABILIDAD_INICIATIVA | `PK_APLICABILIDAD_INICIATIVA`, `UK_AI_INICIATIVA`, `FK_AI_INICIATIVA`, `FK_AI_EVALUADOR`, `CK_AI_RESULTADO`, `CK_AI_MOTIVO` |
| 014 / 014.1 | APLICABILIDAD_CRITERIO | `PK_APLICABILIDAD_CRITERIO`, `UK_AC_APLICABILIDAD_CLAVE`, `FK_AC_APLICABILIDAD`, `CK_AC_ORDEN` |
| 015 | PLANIFICACION_PROYECTO | `PK_PLANIFICACION_PROYECTO`, `UK_PP_PROY_VERSION`, `FK_PP_PROYECTO`, `FK_PP_VERSION_ANTERIOR`, `CK_PP_VERSION_MIN`, `CK_PP_CERRADA` |
| 015 | CICLO_PROYECTO | `PK_CICLO_PROYECTO`, `UK_CP_PROY_PERIODO_VERSION`, `FK_CP_PROYECTO`, `FK_CP_VERSION_ANTERIOR`, `CK_CP_PERIODO`, `CK_CP_VERSION`, `CK_CP_AVANCE`, `CK_CP_CERRADO` |
| 015 | CICLO_EVIDENCIA | `PK_CICLO_EVIDENCIA`, `UK_CE_CICLO_DOC`, `FK_CE_CICLO`, `FK_CE_DOCUMENTO` |
| 015 | PRODUCTO_PARCIAL | `PK_PRODUCTO_PARCIAL`, `UK_PROD_CICLO_VERSION`, `FK_PROD_CICLO`, `FK_PROD_RESPONSABLE`, `FK_PROD_VERSION_ANTERIOR`, `CK_PROD_VERSION_MIN` |
| 015 | PRESENTACION_PRODUCTO_FINAL | `PK_PRESENTACION_PRODUCTO_FINAL`, `UK_PPF_PROY_VERSION`, `FK_PPF_PROYECTO`, `FK_PPF_RESPONSABLE`, `FK_PPF_DOCUMENTO_SUSTENTA`, `FK_PPF_VERSION_ANTERIOR` |
| 015 | VALIDACION_RESULTADO | `PK_VALIDACION_RESULTADO`, `UK_VR_PROYECTO`, `FK_VR_PROYECTO`, `FK_VR_RESPONSABLE`, `FK_VR_EVALUADOR`, `CK_VR_ACTORES_DISTINTOS` |
| 015 | CIERRE_PROYECTO | `PK_CIERRE_PROYECTO`, `UK_CIERRE_PROY`, `FK_CIERRE_PROYECTO`, `FK_CIERRE_EVALUADOR` |
| 016 | INCORPORACION_REGISTRO | `PK_INCORPORACION_REGISTRO`, `UK_IR_HASH_FUENTE_RESPONSABLE`, `FK_IR_RESPONSABLE`, `FK_IR_DOCUMENTO_FUENTE`, `FK_IR_REGISTRO_VINCULADO`, `CK_IR_ESTADO`, `CK_IR_HASH` |
| 016 | INCORPORACION_CAMBIO | `PK_INCORPORACION_CAMBIO`, `FK_IC_INCORPORACION`, `FK_IC_ACTOR` |
| 016 | INCORPORACION_CONFLICTO | `PK_INCORPORACION_CONFLICTO`, `UK_ICONF_INC_TIPO_REG`, `FK_ICONF_INCORPORACION`, `FK_ICONF_REGISTRO_CONFLICTIVO`, `FK_ICONF_RESOLUTOR`, `FK_ICONF_DOCUMENTO_RESOLUCION`, `CK_ICONF_TIPO`, `CK_ICONF_RESUELTO`, `CK_ICONF_RESOLUCION` |
| 017 | REPORTE_INSTITUCIONAL | `PK_REPORTE_INSTITUCIONAL`, `FK_RE_GENERADOR`, `FK_RE_SNAPSHOT` (creada tras `REPORTE_SNAPSHOT` para evitar commit implícito cruzado), `CK_RE_TIPO`, `CK_RE_CLASIFICACION`, `CK_RE_ESTADO_TECNICO`, `CK_RE_SEMESTRE`, `CK_RE_CORTE` (invariante determinista entre `ANIO`/`SEMESTRE`/`FECHA_CORTE`) |
| 017 | REPORTE_SNAPSHOT | `PK_REPORTE_SNAPSHOT`, `UK_RS_HASH`, `CK_RS_PAYLOAD_JSON`, `CK_RS_HASH`, `CK_RS_CLASIFICACION` |
| 017 | REPORTE_ARCHIVO | `PK_REPORTE_ARCHIVO`, `UK_RA_REPORTE_FORMATO_VERSION`, `FK_RA_REPORTE`, `FK_RA_DOCUMENTO_VERSION`, `CK_RA_FORMATO`, `CK_RA_HASH` |
| 017 | REPORTE_APROBACION | `PK_REPORTE_APROBACION`, `UK_RAP_REPORTE_VERSION`, `FK_RAP_REPORTE`, `FK_RAP_OFICINA`, `FK_RAP_APROBADOR`, `FK_RAP_DOCUMENTO` |
| 017 | REPORTE_DESTINATARIO | `PK_REPORTE_DESTINATARIO`, `UK_RD_APROBACION_TIPO_ENTIDAD`, `FK_RD_APROBACION`, `CK_RD_TIPO_DESTINATARIO` |
| 017 | REPORTE_REMISION | `PK_REPORTE_REMISION`, `UK_RREM_REPORTE_DESTINATARIO_FECHA`, `FK_RREM_REPORTE`, `FK_RREM_DESTINATARIO`, `CK_RREM_RESULTADO` |
| 025 | CICLO_PROYECTO_VERSION | `PK_CICLO_PROYECTO_VERSION`, `UK_CPV_CICLO_VERSION`, `FK_CPV_CICLO`, `FK_CPV_VERSION_ANTERIOR`, `CK_CPV_VERSION_MIN`; `TRG_CPV_APPEND_ONLY` impide UPDATE/DELETE |
| 025 | PRESENTACION_PRODUCTO_FINAL_EVIDENCIA | `PK_PPF_EVIDENCIA`, `UK_PPFE_PRESENTACION_DOCUMENTO`, `FK_PPFE_PRESENTACION`, `FK_PPFE_DOCUMENTO`; `TRG_PPFE_APPEND_ONLY` impide UPDATE/DELETE |
| 026 | INCORPORACION_REGISTRO | Sin constraints nuevas; agrega `OBSERVACION` nullable y `VERSION NUMBER(10) DEFAULT 0 NOT NULL` para concurrencia optimista JPA |

### Índices

| Índice | Tabla | Columnas o expresión |
|---|---|---|
| UX_UE_RAIZ | UNIDAD_EJECUTORA | `CASE WHEN ID_UNIDAD_PADRE IS NULL THEN 1 END` |
| IDX_UE_PADRE | UNIDAD_EJECUTORA | ID_UNIDAD_PADRE |
| IDX_URU_USUARIO_ACT | USUARIO_ROL_UNIDAD | ID_USUARIO, ACTIVO |
| IDX_URU_UNIDAD_ACT | USUARIO_ROL_UNIDAD | ID_UNIDAD, ACTIVO |
| IDX_URU_ROL | USUARIO_ROL_UNIDAD | ID_ROL |
| IDX_PROY_UNIDAD_EST | PROYECTO | ID_UNIDAD_EJECUTORA, ESTADO |
| IDX_PROY_ESTADO | PROYECTO | ESTADO |
| IDX_PROY_RESPONSABLE | PROYECTO | ID_RESPONSABLE |
| IDX_PROY_FECHA_INICIO | PROYECTO | FECHA_INICIO |
| IDX_PROY_TIPO_REG | PROYECTO | TIPO_REGISTRO |
| IDX_TP_ROL | TRANSICION_PERMITIDA | ID_ROL_REQUERIDO |
| IDX_DOC_PROY_EST_ACT | DOCUMENTO | ID_PROYECTO, ESTADO_AL_CARGAR, ACTIVO |
| IDX_DOC_TIPO | DOCUMENTO | ID_TIPO_DOC |
| IDX_DOC_USUARIO | DOCUMENTO | ID_USUARIO_CARGA |
| IDX_DOC_ANTERIOR | DOCUMENTO | ID_DOCUMENTO_ANTERIOR |
| IDX_CPV_VERSION_ANTERIOR | CICLO_PROYECTO_VERSION | ID_VERSION_ANTERIOR |
| IDX_PPFE_DOCUMENTO | PRESENTACION_PRODUCTO_FINAL_EVIDENCIA | ID_DOCUMENTO |
| IDX_TE_PROY_FECHA | TRANSICION_ESTADO | ID_PROYECTO, FECHA_TRANSICION |
| IDX_TE_USUARIO | TRANSICION_ESTADO | ID_USUARIO |
| IDX_TE_ROL | TRANSICION_ESTADO | ID_ROL_EFECTIVO |
| IDX_TE_UNIDAD | TRANSICION_ESTADO | ID_UNIDAD_EFECTIVA |
| IDX_TE_DOCUMENTO | TRANSICION_ESTADO | ID_DOCUMENTO_REF |
| IDX_SC_UNIDAD | SECUENCIA_CODIGO | ID_UNIDAD |
| IDX_AA_USUARIO | AUDITORIA_ACCESO | ID_USUARIO |
| IDX_AA_FECHA | AUDITORIA_ACCESO | FECHA_HORA |
| IDX_AA_ROL_EFECTIVO | AUDITORIA_ACCESO | ID_ROL_EFECTIVO |
| IDX_AA_UNIDAD_EFECTIVA | AUDITORIA_ACCESO | ID_UNIDAD_EFECTIVA |
| IDX_AE_USUARIO | AUDITORIA_EVENTO | ID_USUARIO |
| IDX_AE_PROC_FECHA | AUDITORIA_EVENTO | PROCESADO, FECHA_EVENTO |
| IDX_SI_EXPEDICION | SOLICITUD_IDEMPOTENTE | FECHA_EXPEDICION |
| IDX_SI_EXPIRACION | SOLICITUD_IDEMPOTENTE | FECHA_EXPIRACION |
| IDX_EI_UNIDAD | EXPEDIENTE_INSTITUCIONAL | ID_UNIDAD |
| IDX_DS_REGISTRO | DOCUMENTO_SERIE | ID_REGISTRO |
| IDX_DS_EXPEDIENTE | DOCUMENTO_SERIE | ID_EXPEDIENTE |
| IDX_DS_TIPO | DOCUMENTO_SERIE | ID_TIPO_DOC |
| IDX_DOC_SERIE | DOCUMENTO | ID_DOCUMENTO_SERIE |
| IDX_DCH_DOCUMENTO | DOCUMENTO_CLASIFICACION_HIST | ID_DOCUMENTO |
| IDX_DP_PUBLICACION_FECHA | DOCUMENTO_PUBLICACION | FECHA_PUBLICACION |
| Índice único de respaldo de `UK_OP_VERSION_CODIGO` | CAT_OBJETIVO_PEI | ID_VERSION, CODIGO |
| Índice único de respaldo de `UK_AP_VERSION_CODIGO` | CAT_ACTIVIDAD_POI | ID_VERSION, CODIGO |
| IDX_MF_VERSION | MATRIZ_FUNCION | ID_VERSION |
| IDX_MFPU_FUNCION | MATRIZ_FUNCION_PERFIL_UNIDAD | ID_FUNCION |
| IDX_MFPU_UNIDAD | MATRIZ_FUNCION_PERFIL_UNIDAD | ID_UNIDAD |
| IDX_MFPU_DOCUMENTO | MATRIZ_FUNCION_PERFIL_UNIDAD | ID_DOCUMENTO_APROBACION |
| UX_URU_ABIERTAS | USUARIO_ROL_UNIDAD | `CASE WHEN FECHA_FIN IS NULL AND REVOCADA_EN IS NULL AND INACTIVA_TEMPORALMENTE = 'N' THEN ID_USUARIO || ':' || ID_ROL || ':' || ID_UNIDAD END` |
| IDX_URU_COMBINACION_MATRIZ | USUARIO_ROL_UNIDAD | ID_COMBINACION_MATRIZ |
| IDX_URUE_ASIGNACION | USUARIO_ROL_UNIDAD_EVENTO | ID_ASIGNACION |
| IDX_URUE_FECHA | USUARIO_ROL_UNIDAD_EVENTO | FECHA_EVENTO |
| IDX_SF_TITULAR | SUPLENCIA_FUNCIONAL | ID_ASIGNACION_TITULAR |
| IDX_SF_SUPLENTE | SUPLENCIA_FUNCIONAL | ID_ASIGNACION_SUPLENTE |
| IDX_OA_ESTADO | OPERACION_APROVISIONAMIENTO | ESTADO_TECNICO |
| IDX_OA_USUARIO_OBJETIVO | OPERACION_APROVISIONAMIENTO | ID_USUARIO_OBJETIVO |
| IDX_OA_UNIDAD_OBJETIVO | OPERACION_APROVISIONAMIENTO | ID_UNIDAD_OBJETIVO |
| IDX_PROY_OBJETIVO_PEI | PROYECTO | OBJETIVO_PEI_ID |
| IDX_PROY_ACTIVIDAD_POI | PROYECTO | ACTIVIDAD_POI_ID |
| IDX_PROY_COMPONENTE_DIGITAL | PROYECTO | COMPONENTE_DIGITAL |
| Índice único de respaldo de `UK_IP_INICIATIVA` | INICIATIVA_PROYECTO | ID_INICIATIVA |
| Índice único de respaldo de `UK_IP_PROYECTO` | INICIATIVA_PROYECTO | ID_PROYECTO |
| UX_PR_TITULAR_ACTIVO | PROYECTO_RESPONSABLE | `CASE WHEN FIN IS NULL THEN ID_PROYECTO END` |
| IDX_PR_PROYECTO | PROYECTO_RESPONSABLE | ID_PROYECTO |
| IDX_PR_USUARIO | PROYECTO_RESPONSABLE | ID_USUARIO |
| IDX_PP_USUARIO | PARTICIPANTE_PERSONA | ID_USUARIO |
| Índice único de respaldo de `UK_PPP_PROY_PART` | PROYECTO_PARTICIPANTE_PERSONA | ID_PROYECTO, ID_PARTICIPANTE |
| IDX_PPP_PROYECTO | PROYECTO_PARTICIPANTE_PERSONA | ID_PROYECTO |
| Índice único de respaldo de `UK_PPU_PROY_UNI` | PROYECTO_PARTICIPANTE_UNIDAD | ID_PROYECTO, ID_UNIDAD |
| IDX_PPU_PROYECTO | PROYECTO_PARTICIPANTE_UNIDAD | ID_PROYECTO |
| Índice único de respaldo de `UK_PCC_TIPO_ETAPA_CAMPO` | PROYECTO_CAMPO_CLASIFICACION | TIPO_REGISTRO, ETAPA, NRO_CAMPO |
| IDX_PCC_TIPO_ETAPA | PROYECTO_CAMPO_CLASIFICACION | TIPO_REGISTRO, ETAPA |
| IDX_PCCH_CLASIFICACION | PROYECTO_CAMPO_CLASIF_HIST | ID_CLASIFICACION |
| Índice único de respaldo de `UK_PP_PROY_VERSION` | PLANIFICACION_PROYECTO | ID_PROYECTO, VERSION |
| Índice único de respaldo de `UK_CP_PROY_PERIODO_VERSION` | CICLO_PROYECTO | ID_PROYECTO, PERIODO, NUMERO_VERSION |
| Índice único de respaldo de `UK_CE_CICLO_DOC` | CICLO_EVIDENCIA | ID_CICLO, ID_DOCUMENTO |
| Índice único de respaldo de `UK_PROD_CICLO_VERSION` | PRODUCTO_PARCIAL | ID_CICLO, VERSION |
| Índice único de respaldo de `UK_PPF_PROY_VERSION` | PRESENTACION_PRODUCTO_FINAL | ID_PROYECTO, VERSION |
| Índice único de respaldo de `UK_VR_PROYECTO` | VALIDACION_RESULTADO | ID_PROYECTO |
| Índice único de respaldo de `UK_CIERRE_PROY` | CIERRE_PROYECTO | ID_PROYECTO |
| IDX_IC_INCORPORACION | INCORPORACION_CAMBIO | ID_INCORPORACION |
| Índice único de respaldo de `UK_ICONF_INC_TIPO_REG` | INCORPORACION_CONFLICTO | ID_INCORPORACION, TIPO_CONFLICTO, ID_REGISTRO_CONFLICTIVO |
| IDX_ICONF_INCORPORACION | INCORPORACION_CONFLICTO | ID_INCORPORACION |
| Índice único de respaldo de `UK_RS_HASH` | REPORTE_SNAPSHOT | HASH_SHA256 |
| Índice único de respaldo de `UK_RA_REPORTE_FORMATO_VERSION` | REPORTE_ARCHIVO | ID_REPORTE, FORMATO, VERSION |
| Índice único de respaldo de `UK_RAP_REPORTE_VERSION` | REPORTE_APROBACION | ID_REPORTE, ID_VERSION |
| Índice único de respaldo de `UK_RD_APROBACION_TIPO_ENTIDAD` | REPORTE_DESTINATARIO | ID_APROBACION, TIPO_DESTINATARIO, ID_ENTIDAD |
| Índice único de respaldo de `UK_RREM_REPORTE_DESTINATARIO_FECHA` | REPORTE_REMISION | ID_REPORTE, ID_DESTINATARIO, FECHA_REMISION |

### Objetos almacenados, vistas y packages

| Tipo | Objetos vigentes |
|---|---|
| Procedimientos | Ninguno registrado |
| Funciones | Ninguna registrada |
| Packages | Ninguno registrado |
| Vistas | Ninguna registrada |
