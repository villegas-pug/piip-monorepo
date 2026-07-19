# Catálogo de tablas Oracle - KALLPA_PIIP

- Fecha generación: 2026-07-18 21:12:06
- Esquema origen: KALLPA_PIIP

## Resumen

- Total tablas: 13
- Total columnas: 119

### Distribución por tipo de dato

| Tipo | Cantidad |
|---|---:|
| CHAR | 11 |
| CLOB | 3 |
| DATE | 4 |
| NUMBER | 43 |
| TIMESTAMP(6) | 9 |
| VARCHAR2 | 49 |

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

### DOCUMENTO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_DOCUMENTO | NUMBER | 22 | 12 | 0 | N | |
| ID_PROYECTO | NUMBER | 22 | 12 | 0 | N | |
| ID_TIPO_DOC | NUMBER | 22 | 5 | 0 | N | |
| ESTADO_AL_CARGAR | VARCHAR2 | 30 | | | N | |
| NOMBRE_ORIGINAL | VARCHAR2 | 500 | | | N | |
| NOMBRE_STORAGE | VARCHAR2 | 700 | | | N | |
| MIME_TYPE | VARCHAR2 | 100 | | | N | |
| TAMANO_BYTES | NUMBER | 22 | 12 | 0 | N | |
| HASH_SHA256 | VARCHAR2 | 64 | | | N | |
| ID_USUARIO_CARGA | NUMBER | 22 | 10 | 0 | N | |
| FECHA_CARGA | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |
| ACTIVO | CHAR | 1 | | | N | 'S' |
| INMUTABLE | CHAR | 1 | | | N | 'N' |
| SCAN_ANTIVIRUS | VARCHAR2 | 10 | | | N | 'PENDIENTE' |
| NUMERO_VERSION | NUMBER | 22 | 5 | 0 | N | 1 |
| ID_DOCUMENTO_ANTERIOR | NUMBER | 22 | 12 | 0 | Y | |
| CLASIFICACION | VARCHAR2 | 50 | | | Y | |

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
| ADMINISTRACION | VARCHAR2 | 10 | | | N | |
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

### TIPO_DOCUMENTO

| Columna | Tipo | Longitud | Precision | Scale | Nullable | Default |
|---|---|---:|---:|---:|---|---|
| ID_TIPO_DOC | NUMBER | 22 | 5 | 0 | N | |
| NOMBRE | VARCHAR2 | 200 | | | N | |
| ESTADO_ASOCIADO | VARCHAR2 | 30 | | | N | |
| OBLIGATORIO | CHAR | 1 | | | N | 'N' |
| DESCRIPCION | VARCHAR2 | 500 | | | Y | |
| ANEXO_NT | VARCHAR2 | 20 | | | Y | |
| ACTIVO | CHAR | 1 | | | N | 'S' |

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
| LOGIN | VARCHAR2 | 100 | | | N | |
| NOMBRE_COMPLETO | VARCHAR2 | 300 | | | N | |
| CORREO | VARCHAR2 | 200 | | | N | |
| ACTIVO | CHAR | 1 | | | N | 'S' |
| CREADO_POR | VARCHAR2 | 100 | | | N | |
| FECHA_CREACION | TIMESTAMP(6) | 11 | | 6 | N | SYSTIMESTAMP |

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
