# Diseño físico Oracle incremental activo 002-017, 019-027

## Estado

**Estado de aprobación**: VIGENTE — 027 ejecutado y confirmado manualmente

Este documento es propiedad de `database-specialist`. Debe completarse y recibir aprobación humana
de base de datos antes de crear cualquier script activo. La aprobación se registra en
`database/physical-design-approval.md` para una revisión identificable de este archivo.

## Decisiones aprobadas

- Oracle 19c o superior y esquema `KALLPA_PIIP`.
- `DOCUMENTO_SERIE` es la raíz lógica y cada fila de `DOCUMENTO` representa una versión.
- `PROYECTO.ADMINISTRACION` se conserva como columna legacy nullable sin reglas nuevas.
- El backfill de `PROYECTO.DESCRIPCION` se bloquea hasta recibir un mapeo aprobado.
- `REPORTE_SNAPSHOT` conserva JSON canónico en CLOB, versión de esquema y SHA-256.
- `SOLICITUD_IDEMPOTENTE` usa una ventana inicial configurable de siete días sin eliminar auditoría.
- Los scripts son de ejecución única, fail-fast y con compensación forward-only.
- Las nuevas PK numéricas usan secuencias dedicadas con prefijo `SEQ_` siguiendo la convención del
  baseline; cada secuencia es `NOCACHE NOCYCLE` salvo las de auditoría, que conservan `CACHE 50`.
- Los nombres de constraints son `PK_`, `UK_`, `FK_` y `CK_` concatenados al nombre del objeto
  siguiendo la convención del baseline. Los índices siguen `IDX_` o `UX_` cuando son únicos.
- `TIPO_DOCUMENTO.ESTADO_ASOCIADO` admite nulo para `CONTEXTO='INSTITUCIONAL'`; en `CONTEXTO='PORTAFOLIO'`
  es obligatorio y pertenece al dominio canónico de estados de negocio.
- `TRANSICION_PERMITIDA` queda como legado inactivo y no se amplía. La máquina de estados reside en
  `TransicionEstadoService` Java.
- `DOCUMENTO.SCAN_ANTIVIRUS` y `DOCUMENTO.NOMBRE_STORAGE` permanecen como columnas legacy nullable
  sin default, constraint, mapeo JPA ni consumidor; su nulabilidad es la nueva condición operativa.

## Prevalidación de versión

No se crea una tabla de versión ni se consulta `database/CHANGELOG.md` desde Oracle. Cada incremento
declara una huella de precondiciones sobre `USER_TABLES`, `USER_TAB_COLUMNS`, `USER_CONSTRAINTS`,
`USER_CONS_COLUMNS`, `USER_INDEXES`, `USER_IND_COLUMNS`, `USER_IND_EXPRESSIONS` y `USER_SEQUENCES`
correspondiente al baseline y a todos sus predecesores. El script aborta antes del primer DDL cuando
falta un objeto esperado, existe uno futuro o una definición no coincide en nombre, columnas, orden,
tipo, longitud, nulabilidad, expresión o unicidad.

## Diccionario requerido

Para cada objeto del alcance físico activo se documenta antes del DDL:

- propósito, dependencia y compensación forward-only;
- tabla, columna, tipo Oracle, longitud o precisión, nulabilidad y default;
- secuencia, PK, FK, UK y CHECK con nombre explícito;
- índices auxiliares y condición de unicidad;
- huella de precondiciones y consultas de incompatibilidad de datos;
- orden de creación considerando commits implícitos Oracle;
- prueba SQL positiva y negativa asociada.

## Gates de insumos

- 020: datasets y aprobaciones independientes PEI y POI, no disponibles.
- 021: matriz función-perfil-unidad aprobada, no disponible.
- 022-024: diferidos a fase posterior; no tienen dependencias ni ejecución activas. Su reactivación
  requiere mapeos legacy aprobados y nueva revisión humana.

Ningún valor faltante se infiere. `database/database-schema.md` permanece como catálogo aplicado y no
se actualiza mientras esta aprobación o la ejecución humana del incremento estén pendientes.

## Convenciones del diccionario

- Tipos `VARCHAR2 N CHAR` se documentan como `VARCHAR2(n)` y los `BYTE` no se usan para contenido
  institucional; las unidades de medida se omiten cuando son irrelevantes.
- `NUMBER(n)` indica precisión sin escala; `NUMBER(n,m)` indica precisión y escala.
- `TIMESTAMP(6)` se usa para auditoría y fechas operativas; `DATE` se reserva para cortes y vigencias.
- `CHAR(1 CHAR)` con valores `'S'` o `'N'` representa flags activos; `CHECK` lo enuncia.
- Las secuencias son `START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE`, salvo las de auditoría.
- Las columnas `CREADO_POR`, `MODIFICADO_POR`, `CREADO_EN`, `MODIFICADO_EN` se documentan en la
  sección "Auditoría" de cada tabla cuando corresponda.
- `BLOB` se usa para `DOCUMENTO.CONTENIDO`; `CLOB` para textos largos (`REPORTE_SNAPSHOT.PAYLOAD_JSON`).
- Las columnas `CLOB` no admiten `IS JSON` implícito en PIIP salvo que la regla funcional lo exija.

## Resumen de objetos por incremento

| Incremento | Módulo | Objetos nuevos principales | Modificaciones a baseline |
|---:|---|---|---|
| 002 | auditoria | `SOLICITUD_IDEMPOTENTE` (tabla, `SEQ_SOLICITUD_IDEMPOTENTE`) | `AUDITORIA_ACCESO` (columnas `ID_ROL_EFECTIVO`, `ID_UNIDAD_EFECTIVA`, `ID_ASIGNACION_EFECTIVA`) |
| 003 | documentos | `EXPEDIENTE_INSTITUCIONAL`, `DOCUMENTO_SERIE`; `SEQ_EXPEDIENTE_INSTITUCIONAL`, `SEQ_DOCUMENTO_SERIE` | `TIPO_DOCUMENTO` (columnas `CONTEXTO`, `CLASIFICACION_DEFECTO`); `DOCUMENTO` (columna `ID_DOCUMENTO_SERIE` y `CONTENIDO BLOB`; `NOMBRE_STORAGE` y `SCAN_ANTIVIRUS` quedan legacy nullable) |
| 004 | documentos | `DOCUMENTO_CLASIFICACION_HIST`, `DOCUMENTO_PUBLICACION`; `SEQ_DOCUMENTO_CLASIF_HIST`, `SEQ_DOCUMENTO_PUBLICACION` | `DOCUMENTO` (columna `CLASIFICACION_VALIDADA`, `CLASIFICACION_FECHA`, `ID_USUARIO_VALIDA`) |
| 005 | organizacion | `CAT_OBJETIVO_PEI_VERSION`, `CAT_OBJETIVO_PEI`; `SEQ_OBJETIVO_PEI_VERSION`, `SEQ_OBJETIVO_PEI` | Ninguna sobre baseline. Bloquea la separación de `PROYECTO.OBJETIVO_PEI` textual. |
| 006 | organizacion | `CAT_ACTIVIDAD_POI_VERSION`, `CAT_ACTIVIDAD_POI`; `SEQ_ACTIVIDAD_POI_VERSION`, `SEQ_ACTIVIDAD_POI` | Ninguna sobre baseline. Bloquea la separación de `PROYECTO.ACTIVIDAD_POI` textual. |
| 007 | seguridad | `MATRIZ_FUNCIONAL_VERSION`, `MATRIZ_FUNCION`, `MATRIZ_FUNCION_PERFIL_UNIDAD`; `SEQ_MATRIZ_VERSION`, `SEQ_MATRIZ_FUNCION`, `SEQ_MATRIZ_COMBINACION` | Ninguna sobre baseline. |
| 008 | seguridad | `USUARIO_ROL_UNIDAD_EVENTO`, `SUPLENCIA_FUNCIONAL`, `OPERACION_APROVISIONAMIENTO`; `SEQ_URU_EVENTO`, `SEQ_SUPLENCIA_FUNCIONAL`, `SEQ_OPERACION_APROVISIONAMIENTO` | `USUARIO_ROL_UNIDAD` (columnas `FECHA_INICIO`, `FECHA_FIN`, `REVOCADA_EN`, `REVOCADA_POR`, `MOTIVO_REVOCACION`, `INACTIVA_TEMPORALMENTE`, `ID_COMBINACION_MATRIZ`, `ID_DOCUMENTO_FORMAL`, `VERSION`); `USUARIO` (columnas `CORREO_INSTITUCIONAL`, `NOMBRE_COMPLETO` quedan nulas permitidas) |
| 009 | portafolio | Ninguna tabla nueva | `PROYECTO` (columnas de los 23 campos canónicos, `@Version`, código inmutable, etc.) |
| 010 | portafolio | `INICIATIVA_PROYECTO`; `SEQ_INICIATIVA_PROYECTO` | Ninguna |
| 011 | portafolio | `PROYECTO_RESPONSABLE` (titularidades); `SEQ_PROYECTO_RESPONSABLE` | Ninguna |
| 012 | portafolio | `PARTICIPANTE_PERSONA`, `PROYECTO_PARTICIPANTE_PERSONA`, `PROYECTO_PARTICIPANTE_UNIDAD`; `SEQ_PARTICIPANTE_PERSONA`, `SEQ_PROY_PART_PERSONA`, `SEQ_PROY_PART_UNIDAD` | Ninguna |
| 013 | portafolio | `PROYECTO_CAMPO_CLASIFICACION`, `PROYECTO_CAMPO_CLASIF_HIST`; `SEQ_PROY_CAMPO_CLASIF`, `SEQ_PROY_CAMPO_CLASIF_HIST` | Ninguna |
| 014 | portafolio | `EVALUACION_INICIATIVA`, `SUBSANACION_INICIATIVA`, `APLICABILIDAD_INICIATIVA`, `APLICABILIDAD_CRITERIO`; `SEQ_EVALUACION_INICIATIVA`, `SEQ_SUBSANACION_INICIATIVA`, `SEQ_APLICABILIDAD_INICIATIVA`, `SEQ_APLICABILIDAD_CRITERIO` | Ninguna |
| 015 | portafolio | `PLANIFICACION_PROYECTO`, `CICLO_PROYECTO`, `CICLO_EVIDENCIA`, `PRODUCTO_PARCIAL`, `PRESENTACION_PRODUCTO_FINAL`, `VALIDACION_RESULTADO`, `CIERRE_PROYECTO`; `SEQ_PLANIFICACION_PROYECTO`, `SEQ_CICLO_PROYECTO`, `SEQ_CICLO_EVIDENCIA`, `SEQ_PRODUCTO_PARCIAL`, `SEQ_PRESENTACION_PRODUCTO_FINAL`, `SEQ_VALIDACION_RESULTADO`, `SEQ_CIERRE_PROYECTO` | Ninguna |
| 016 | portafolio | `INCORPORACION_REGISTRO`, `INCORPORACION_CAMBIO`, `INCORPORACION_CONFLICTO`; `SEQ_INCORPORACION_REGISTRO`, `SEQ_INCORPORACION_CAMBIO`, `SEQ_INCORPORACION_CONFLICTO` | Ninguna |
| 017 | reportes | `REPORTE_INSTITUCIONAL`, `REPORTE_SNAPSHOT`, `REPORTE_ARCHIVO`, `REPORTE_APROBACION`, `REPORTE_DESTINATARIO`, `REPORTE_REMISION`; `SEQ_REPORTE_INSTITUCIONAL`, `SEQ_REPORTE_SNAPSHOT`, `SEQ_REPORTE_ARCHIVO`, `SEQ_REPORTE_APROBACION`, `SEQ_REPORTE_DESTINATARIO`, `SEQ_REPORTE_REMISION` | Ninguna |
| 025 | portafolio | `CICLO_PROYECTO_VERSION`, `PRESENTACION_PRODUCTO_FINAL_EVIDENCIA`; `SEQ_CICLO_PROYECTO_VERSION`, `SEQ_PPF_EVIDENCIA` | Ninguna |
| 026 | portafolio | Ninguna tabla nueva | `INCORPORACION_REGISTRO` (`OBSERVACION`, `VERSION`) — VIGENTE |
| 027 | seguridad | Ninguna tabla nueva | `OPERACION_APROVISIONAMIENTO` (`ID_UNIDAD_OBJETIVO`, `FK_OA_UNIDAD_OBJETIVO`, `IDX_OA_UNIDAD_OBJETIVO`) — VIGENTE |
| 018 | portafolio | Diferido a fase posterior; no forma parte del alcance físico activo | Ninguna |
| 019 | transversal (seeds) | Solo datos semilla: roles canónicos, tipos documentales ampliados y transiciones | Solo si una fila canónica se inactiva |
| 020 | transversal (seeds) | Solo datos semilla: versiones PEI/POI e ítems aprobados | Ninguna |
| 021 | transversal (seeds) | Solo datos: `MATRIZ_FUNCIONAL_VERSION` inicial, función `ADMINISTRADOR_PIIP`, combinación, primer `GlobalAdmin` y su asignación | Inserta `USUARIO` con `KEYCLOAK_ID` proporcionado; modifica `USUARIO_ROL_UNIDAD` y `MATRIZ_FUNCION_PERFIL_UNIDAD` con su primera fila |
| 022 | transversal | Diferido a fase posterior; sin DDL, dependencias ni objetos activos | Conserva trazabilidad histórica; reactivación con mapeos aprobados y nueva revisión humana |
| 023 | transversal (índices) | Diferido a fase posterior; sin DDL, dependencias ni índices activos | Conserva trazabilidad histórica; reactivación con mapeos aprobados y nueva revisión humana |
| 024 | transversal (corte) | Diferido a fase posterior; sin DDL, dependencias ni objetos activos | Conserva trazabilidad histórica; reactivación con mapeos aprobados y nueva revisión humana |

## Diccionario por incremento

### 002 — `database/ddl/auditoria/002_auditoria_idempotencia.sql`

- **Propósito**: introducir `SOLICITUD_IDEMPOTENTE` para que cada combinación consumidor+operación+clave
  almacene hash de payload, recurso creado, respuesta estable, estado técnico y expiración operativa, y
  ampliar `AUDITORIA_ACCESO` con `ID_ROL_EFECTIVO`, `ID_UNIDAD_EFECTIVA` y `ID_ASIGNACION_EFECTIVA`
  opcionales para evidenciar el contexto de cada acceso sensible.
- **Dependencia**: 001.
- **Compensación forward-only**: detener escritura de los nuevos campos y dejar de consultar
  `SOLICITUD_IDEMPOTENTE`; nunca eliminar auditoría ni claves ya consumidas.

#### Tabla `SOLICITUD_IDEMPOTENTE` (nueva)

| Columna | Tipo | Longitud | Nulable | Default |
|---|---|---|---:|---|
| `ID_SOLICITUD` | NUMBER | 15 | N | (de `SEQ_SOLICITUD_IDEMPOTENTE.NEXTVAL`) |
| `CONSUMIDOR` | VARCHAR2 | 100 | N |  |
| `OPERACION` | VARCHAR2 | 100 | N |  |
| `CLAVE` | VARCHAR2 | 100 | N |  |
| `HASH_PAYLOAD` | VARCHAR2 | 64 | N |  |
| `RECURSO_TIPO` | VARCHAR2 | 50 | Y |  |
| `RECURSO_ID` | NUMBER | 15 | Y |  |
| `RESPUESTA_JSON` | CLOB |  | Y |  |
| `ESTADO_TECNICO` | VARCHAR2 | 20 | N |  |
| `FECHA_EXPEDICION` | TIMESTAMP(6) |  | N | `SYSTIMESTAMP` |
| `FECHA_EXPIRACION` | TIMESTAMP(6) |  | N |  |
| `CREADO_POR` | VARCHAR2 | 100 | N |  |

- Secuencia: `SEQ_SOLICITUD_IDEMPOTENTE` (`NOCACHE NOCYCLE`).
- PK: `PK_SOLICITUD_IDEMPOTENTE (ID_SOLICITUD)`.
- UK: `UK_SI_CONSUMIDOR_OPERACION_CLAVE (CONSUMIDOR, OPERACION, CLAVE)`.
- CHECKs: `CK_SI_ESTADO_TECNICO` ∈ {`INICIADA`,`COMPLETADA`,`EXPIRADA`,`FALLIDA`};
  `CK_SI_HASH` `REGEXP_LIKE(HASH_PAYLOAD, '^[0-9A-Fa-f]{64}$')`.
- Índices auxiliares: `IDX_SI_EXPEDICION` sobre `FECHA_EXPEDICION`;
  `IDX_SI_EXPIRACION` sobre `FECHA_EXPIRACION` para barridos de expiración.

#### Modificaciones a `AUDITORIA_ACCESO`

- Nuevas columnas nullable: `ID_ROL_EFECTIVO NUMBER(5)`, `ID_UNIDAD_EFECTIVA NUMBER(10)`,
  `ID_ASIGNACION_EFECTIVA NUMBER(10)`.
- FKs nuevas: `FK_AA_ROL_EFECTIVO`, `FK_AA_UNIDAD_EFECTIVA`,
  `FK_AA_ASIGNACION_EFECTIVA` (esta última se crea en 008 cuando exista la combinación).
- Índice: `IDX_AA_ROL_EFECTIVO`, `IDX_AA_UNIDAD_EFECTIVA`.

#### Huella de precondiciones

```sql
SELECT TABLE_NAME FROM USER_TABLES WHERE TABLE_NAME IN ('AUDITORIA_ACCESO','AUDITORIA_EVENTO');
SELECT COLUMN_NAME FROM USER_TAB_COLUMNS WHERE TABLE_NAME='AUDITORIA_ACCESO'
  AND COLUMN_NAME IN ('ID_AUDIT','ID_USUARIO','ENDPOINT','METODO_HTTP','CODIGO_RESPUESTA',
                      'IP_CLIENTE','FECHA_HORA','DURACION_MS');
SELECT CONSTRAINT_NAME FROM USER_CONSTRAINTS
  WHERE TABLE_NAME='AUDITORIA_ACCESO' AND CONSTRAINT_NAME IN
    ('PK_AUDITORIA_ACCESO','FK_AA_USUARIO','CK_AA_METODO','CK_AA_RESPUESTA','CK_AA_DURACION');
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME='SOLICITUD_IDEMPOTENTE';
SELECT COUNT(*) FROM USER_SEQUENCES WHERE SEQUENCE_NAME='SEQ_SOLICITUD_IDEMPOTENTE';
SELECT COUNT(*) FROM USER_TAB_COLUMNS
  WHERE TABLE_NAME='AUDITORIA_ACCESO' AND COLUMN_NAME IN
    ('ID_ROL_EFECTIVO','ID_UNIDAD_EFECTIVA','ID_ASIGNACION_EFECTIVA');
```

#### Orden de creación

1. Crear `SEQ_SOLICITUD_IDEMPOTENTE` (commit implícito).
2. Crear tabla `SOLICITUD_IDEMPOTENTE` con PK y CHECKs (commit implícito).
3. Crear UK e índices (commit implícito por `CREATE INDEX`).
4. `ALTER TABLE AUDITORIA_ACCESO ADD COLUMN` para las tres columnas nuevas (cada `ALTER` agrega un
   commit implícito). El script aborta si `AUDITORIA_ACCESO` está bloqueada por sesión activa.
5. Crear `IDX_AA_ROL_EFECTIVO` e `IDX_AA_UNIDAD_EFECTIVA`. Las FKs se difieren a 008.
6. Validar con `SELECT` y emitir un único `COMMIT` final del bloque PL/SQL.

#### Pruebas SQL

- Positiva: insertar dos veces el mismo `CONSUMIDOR/OPERACION/CLAVE` con el mismo `HASH_PAYLOAD`
  debe arrojar `ORA-00001` por la UK, demostrando idempotencia estructural.
- Negativa: insertar el mismo trío con `HASH_PAYLOAD` distinto debe rechazarse por
  `CK_SI_HASH` o por validación Java posterior (`409`).

---

### 003 — `database/ddl/documentos/003_expediente_serie_version.sql`

- **Propósito**: introducir `EXPEDIENTE_INSTITUCIONAL` y `DOCUMENTO_SERIE` como raíces lógicas de
  expediente y documento, añadir a `DOCUMENTO` la columna `ID_DOCUMENTO_SERIE`, la columna
  `CONTENIDO BLOB` y, sobre `TIPO_DOCUMENTO`, los discriminantes `CONTEXTO` y `CLASIFICACION_DEFECTO`,
  para soportar propiedad XOR y 100 MB.
- **Dependencia**: 002.
- **Compensación forward-only**: detener nuevas cargas; conservar expedientes, series y versiones
  creadas. Las columnas legacy `SCAN_ANTIVIRUS` y `NOMBRE_STORAGE` se vuelven nullable sin default
  ni constraint; los registros históricos no migran a 100 MB.

#### Tabla `EXPEDIENTE_INSTITUCIONAL` (nueva)

| Columna | Tipo | Longitud | Nulable | Default |
|---|---|---|---:|---|
| `ID_EXPEDIENTE` | NUMBER | 12 | N | (de `SEQ_EXPEDIENTE_INSTITUCIONAL`) |
| `CODIGO` | VARCHAR2 | 30 | N |  |
| `ASUNTO` | VARCHAR2 | 500 | N |  |
| `MODULO_ORIGEN` | VARCHAR2 | 50 | N |  |
| `REFERENCIA_CASO_USO` | VARCHAR2 | 100 | N |  |
| `ID_UNIDAD` | NUMBER | 10 | Y |  |
| `CLASIFICACION` | VARCHAR2 | 20 | N |  |
| `ACTIVO` | CHAR | 1 | N | `'S'` |
| `VERSION` | NUMBER | 10 | N | `0` |
| `CREADO_POR` | VARCHAR2 | 100 | N |  |
| `FECHA_CREACION` | TIMESTAMP(6) |  | N | `SYSTIMESTAMP` |
| `MODIFICADO_POR` | VARCHAR2 | 100 | Y |  |
| `FECHA_MODIFICACION` | TIMESTAMP(6) |  | Y |  |

- Secuencia: `SEQ_EXPEDIENTE_INSTITUCIONAL`.
- PK: `PK_EXPEDIENTE_INSTITUCIONAL (ID_EXPEDIENTE)`.
- UK: `UK_EI_CODIGO (CODIGO)`.
- FKs: `FK_EI_UNIDAD` → `UNIDAD_EJECUTORA (ID_UNIDAD)`.
- CHECKs: `CK_EI_ACTIVO` ∈ {`S`,`N`}; `CK_EI_CLASIFICACION` ∈ {`PUBLICO`,`INTERNO`,`RESTRINGIDO`};
  `CK_EI_MODULO` ∈ {`ORGANIZACION`,`SEGURIDAD`,`PORTAFOLIO`,`DOCUMENTOS`,`REPORTES`,`CONSULTA`,`AUDITORIA`}.
- Índice: `IDX_EI_UNIDAD` sobre `ID_UNIDAD`.

#### Tabla `DOCUMENTO_SERIE` (nueva)

| Columna | Tipo | Longitud | Nulable | Default |
|---|---|---|---:|---|
| `ID_SERIE` | NUMBER | 12 | N | (de `SEQ_DOCUMENTO_SERIE`) |
| `ID_TIPO_DOC` | NUMBER | 5 | N |  |
| `ID_REGISTRO` | NUMBER | 12 | Y |  |
| `ID_EXPEDIENTE` | NUMBER | 12 | Y |  |
| `TITULO` | VARCHAR2 | 500 | N |  |
| `CLASIFICACION_PROPUESTA` | VARCHAR2 | 20 | N |  |
| `CLASIFICACION_VALIDADA` | VARCHAR2 | 20 | Y |  |
| `ACTIVA` | CHAR | 1 | N | `'S'` |
| `VERSION` | NUMBER | 10 | N | `0` |
| `CREADO_POR` | VARCHAR2 | 100 | N |  |
| `FECHA_CREACION` | TIMESTAMP(6) |  | N | `SYSTIMESTAMP` |

- Secuencia: `SEQ_DOCUMENTO_SERIE`.
- PK: `PK_DOCUMENTO_SERIE (ID_SERIE)`.
- UK: `UK_DS_TITULO_TIPO (ID_TIPO_DOC, TITULO)`.
- FKs: `FK_DS_TIPO_DOC` → `TIPO_DOCUMENTO (ID_TIPO_DOC)`; `FK_DS_REGISTRO` → `PROYECTO (ID_PROYECTO)`;
  `FK_DS_EXPEDIENTE` → `EXPEDIENTE_INSTITUCIONAL (ID_EXPEDIENTE)`.
- CHECKs: `CK_DS_XOR_DUENIO` `((ID_REGISTRO IS NOT NULL AND ID_EXPEDIENTE IS NULL) OR
  (ID_REGISTRO IS NULL AND ID_EXPEDIENTE IS NOT NULL))`; `CK_DS_CLAS_PROPUESTA` ∈
  {`PUBLICO`,`INTERNO`,`RESTRINGIDO`}; `CK_DS_CLAS_VALIDADA` ∈ {`PUBLICO`,`INTERNO`,`RESTRINGIDO`} o
  nulo; `CK_DS_ACTIVA` ∈ {`S`,`N`}.
- Índice: `IDX_DS_REGISTRO`, `IDX_DS_EXPEDIENTE`, `IDX_DS_TIPO`.

#### Modificaciones a `DOCUMENTO`

- Nuevas columnas: `ID_DOCUMENTO_SERIE NUMBER(12)` (nullable; cualquier cambio de nulabilidad exige
  una futura migración/corte expresamente aprobada),
  `CONTENIDO BLOB` (nullable para permitir formalización previa a escritura binaria), `FORMATO
  VARCHAR2(20 CHAR)` (canónico PDF, OOXML, JPEG, PNG).
- Modificación: `NOMBRE_STORAGE` y `SCAN_ANTIVIRUS` se vuelven nullable sin default, sin `CHECK`, sin
  `NOT NULL` y sin constraint. Se conserva el dato histórico.
- `TAMANO_BYTES` admite `NUMBER(12)` con `CHECK (TAMANO_BYTES > 0 AND TAMANO_BYTES <= 104857600)`.
- FKs: `FK_DOC_SERIE` → `DOCUMENTO_SERIE (ID_SERIE)`.
- Índice: `IDX_DOC_SERIE`.

#### Modificaciones a `TIPO_DOCUMENTO`

- Nuevas columnas: `CONTEXTO VARCHAR2(20 CHAR) NOT NULL` con CHECK ∈ {`PORTAFOLIO`,`INSTITUCIONAL`};
  `CLASIFICACION_DEFECTO VARCHAR2(20 CHAR)` con CHECK ∈ {`PUBLICO`,`INTERNO`,`RESTRINGIDO`}.
- `ESTADO_ASOCIADO` se mantiene; CHECK `CK_TD_ESTADO_CONTEXTO` exige `ESTADO_ASOCIADO` no nulo
  cuando `CONTEXTO='PORTAFOLIO'` y nulo cuando `CONTEXTO='INSTITUCIONAL'`.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('DOCUMENTO','TIPO_DOCUMENTO',
  'UNIDAD_EJECUTORA','PROYECTO');
SELECT COUNT(*) FROM USER_SEQUENCES WHERE SEQUENCE_NAME IN ('SEQ_DOCUMENTO','SEQ_TIPO_DOCUMENTO',
  'SEQ_UNIDAD_EJECUTORA','SEQ_PROYECTO');
SELECT COLUMN_NAME FROM USER_TAB_COLUMNS WHERE TABLE_NAME='DOCUMENTO' AND COLUMN_NAME IN
  ('ID_DOCUMENTO','ID_PROYECTO','ID_TIPO_DOC','TAMANO_BYTES','HASH_SHA256','MIME_TYPE');
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('EXPEDIENTE_INSTITUCIONAL','DOCUMENTO_SERIE');
SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='DOCUMENTO' AND COLUMN_NAME='ID_DOCUMENTO_SERIE';
SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='TIPO_DOCUMENTO' AND COLUMN_NAME='CONTEXTO';
```

#### Orden de creación

1. `SEQ_EXPEDIENTE_INSTITUCIONAL` y `SEQ_DOCUMENTO_SERIE` (commits implícitos).
2. `CREATE TABLE EXPEDIENTE_INSTITUCIONAL` con PK, UK, CHECKs y FKs declaradas.
3. `CREATE TABLE DOCUMENTO_SERIE` con PK, UK, CHECKs y FKs declaradas.
4. `ALTER TABLE TIPO_DOCUMENTO ADD (CONTEXTO, CLASIFICACION_DEFECTO)` con default backfill
   `UPDATE TIPO_DOCUMENTO SET CONTEXTO='PORTAFOLIO' WHERE CONTEXTO IS NULL`. El default nuevo se aplica
   solo a filas insertadas tras el `ALTER`.
5. `ALTER TABLE DOCUMENTO ADD (ID_DOCUMENTO_SERIE, CONTENIDO, FORMATO)`. Migrar nullable de
   `SCAN_ANTIVIRUS` y `NOMBRE_STORAGE` requiere `ALTER TABLE ... MODIFY (...)` con `NULL`.
6. Crear índices asociados.

#### Pruebas SQL

- Positiva: insertar `EXPEDIENTE_INSTITUCIONAL` y `DOCUMENTO_SERIE` con XOR válido debe persistir.
- Negativa: insertar `DOCUMENTO_SERIE` con `ID_REGISTRO` y `ID_EXPEDIENTE` ambos nulos o ambos
  distintos de nulo debe ser rechazado por `CK_DS_XOR_DUENIO`.

---

### 004 — `database/ddl/documentos/004_documento_publicacion.sql`

- **Propósito**: introducir `DOCUMENTO_CLASIFICACION_HIST` y `DOCUMENTO_PUBLICACION` para registrar
  reclasificaciones y publicaciones append-only, y ampliar `DOCUMENTO` con clasificación validada.
- **Dependencia**: 003.
- **Compensación forward-only**: detener nuevas publicaciones; conservar confirmaciones y auditoría.

#### Tabla `DOCUMENTO_CLASIFICACION_HIST` (nueva)

| Columna | Tipo | Longitud | Nulable | Default |
|---|---|---|---:|---|
| `ID_HISTORIAL` | NUMBER | 12 | N | (de `SEQ_DOCUMENTO_CLASIF_HIST`) |
| `ID_DOCUMENTO` | NUMBER | 12 | N |  |
| `CLASIFICACION_ANTERIOR` | VARCHAR2 | 20 | Y |  |
| `CLASIFICACION_NUEVA` | VARCHAR2 | 20 | N |  |
| `ID_AUTORIDAD_DECISORA` | NUMBER | 10 | N |  |
| `ID_EVALUADOR_REGISTRADOR` | NUMBER | 10 | N |  |
| `ID_DOCUMENTO_DECISION` | NUMBER | 12 | Y |  |
| `MOTIVO` | VARCHAR2 | 2000 | Y |  |
| `FECHA_CAMBIO` | TIMESTAMP(6) |  | N | `SYSTIMESTAMP` |
| `RESULTADO` | VARCHAR2 | 20 | N |  |

- Secuencia: `SEQ_DOCUMENTO_CLASIF_HIST`.
- PK: `PK_DOCUMENTO_CLASIFICACION_HIST (ID_HISTORIAL)`.
- FKs: `FK_DCH_DOCUMENTO`, `FK_DCH_AUTORIDAD`, `FK_DCH_EVALUADOR`, `FK_DCH_DOCUMENTO_DECISION`.
- CHECKs: `CK_DCH_CLAS_NUEVA`, `CK_DCH_CLAS_ANT` ∈ {`PUBLICO`,`INTERNO`,`RESTRINGIDO`} o nulo;
  `CK_DCH_RESTRICTIVA` exige que la nueva clasificación sea igual o más restrictiva.
- Índice: `IDX_DCH_DOCUMENTO` sobre `ID_DOCUMENTO`.

#### Tabla `DOCUMENTO_PUBLICACION` (nueva)

| Columna | Tipo | Longitud | Nulable | Default |
|---|---|---|---:|---|
| `ID_PUBLICACION` | NUMBER | 12 | N | (de `SEQ_DOCUMENTO_PUBLICACION`) |
| `ID_DOCUMENTO` | NUMBER | 12 | N |  |
| `TITULO_PUBLICO` | VARCHAR2 | 500 | N |  |
| `ID_EVALUADOR_CONFIRMADOR` | NUMBER | 10 | N |  |
| `ID_ASIGNACION_EFECTIVA` | NUMBER | 10 | N |  |
| `FECHA_PUBLICACION` | TIMESTAMP(6) |  | N | `SYSTIMESTAMP` |

- Secuencia: `SEQ_DOCUMENTO_PUBLICACION`.
- PK: `PK_DOCUMENTO_PUBLICACION (ID_PUBLICACION)`.
- UK: `UK_DP_DOCUMENTO (ID_DOCUMENTO)`.
- FKs: `FK_DP_DOCUMENTO`, `FK_DP_EVALUADOR`, `FK_DP_ASIGNACION`.
- CHECKs: `CK_DP_FORMATO_TITULO` exige `TITULO_PUBLICO` sin `@` ni dígitos de 9 a 12 dígitos
  consecutivos (expresión regular para evitar correos y teléfonos).

#### Modificaciones a `DOCUMENTO`

- Nuevas columnas nullable: `CLASIFICACION_VALIDADA VARCHAR2(20)`, `CLASIFICACION_FECHA
  TIMESTAMP(6)`, `ID_USUARIO_VALIDA NUMBER(10)`. Se omite FK porque `USUARIO_ROL_UNIDAD` evoluciona
  en 008.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('DOCUMENTO','TIPO_DOCUMENTO',
  'DOCUMENTO_SERIE','EXPEDIENTE_INSTITUCIONAL','PROYECTO');
SELECT CONSTRAINT_NAME FROM USER_CONSTRAINTS WHERE TABLE_NAME='DOCUMENTO_SERIE' AND CONSTRAINT_NAME
  IN ('PK_DOCUMENTO_SERIE','CK_DS_XOR_DUENIO');
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('DOCUMENTO_CLASIFICACION_HIST',
  'DOCUMENTO_PUBLICACION');
SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='DOCUMENTO' AND COLUMN_NAME=
  'CLASIFICACION_VALIDADA';
```

#### Orden de creación

1. Secuencias y tablas nuevas con PK y CHECKs.
2. `ALTER TABLE DOCUMENTO ADD` de las tres columnas.
3. UK de `DOCUMENTO_PUBLICACION` y FKs.

#### Pruebas SQL

- Positiva: dos publicaciones con el mismo `ID_DOCUMENTO` deben chocar con la UK.
- Negativa: una reclasificación de `PUBLICO` a `INTERNO` en `DOCUMENTO_CLASIFICACION_HIST` debe ser
  rechazada por `CK_DCH_RESTRICTIVA`; la operación inversa debe aceptarse.

---

### 005 — `database/ddl/organizacion/005_objetivo_pei_versionado.sql`

- **Propósito**: introducir versiones independientes de Objetivo PEI como cabeceras y como ítems.
- **Dependencia**: 003.
- **Compensación forward-only**: inactivar versiones no usadas; preservar referencias históricas.

#### Tabla `CAT_OBJETIVO_PEI_VERSION` (nueva)

| Columna | Tipo | Longitud | Nulable | Default |
|---|---|---|---:|---|
| `ID_VERSION` | NUMBER | 10 | N | (de `SEQ_OBJETIVO_PEI_VERSION`) |
| `CODIGO_VERSION` | VARCHAR2 | 30 | N |  |
| `ID_VERSION_ANTERIOR` | NUMBER | 10 | Y |  |
| `ID_DOCUMENTO_APROBACION` | NUMBER | 12 | N |  |
| `OFICINA_APROBADORA` | VARCHAR2 | 200 | N |  |
| `VIGENTE_DESDE` | DATE |  | N |  |
| `VIGENTE_HASTA` | DATE |  | Y |  |
| `ACTIVA` | CHAR | 1 | N | `'S'` |
| `CREADO_POR` | VARCHAR2 | 100 | N |  |
| `FECHA_CREACION` | TIMESTAMP(6) |  | N | `SYSTIMESTAMP` |

- Secuencia: `SEQ_OBJETIVO_PEI_VERSION`.
- PK: `PK_CAT_OBJETIVO_PEI_VERSION (ID_VERSION)`.
- UK: `UK_OPV_CODIGO (CODIGO_VERSION)`.
- FKs: `FK_OPV_VERSION_ANTERIOR`, `FK_OPV_DOCUMENTO` → `DOCUMENTO (ID_DOCUMENTO)`.
- CHECKs: `CK_OPV_VIGENCIA` `VIGENTE_HASTA IS NULL OR VIGENTE_HASTA >= VIGENTE_DESDE`;
  `CK_OPV_ACTIVA` ∈ {`S`,`N`}.

#### Tabla `CAT_OBJETIVO_PEI` (nueva)

| Columna | Tipo | Longitud | Nulable | Default |
|---|---|---|---:|---|
| `ID_OBJETIVO` | NUMBER | 10 | N | (de `SEQ_OBJETIVO_PEI`) |
| `ID_VERSION` | NUMBER | 10 | N |  |
| `CODIGO` | VARCHAR2 | 30 | N |  |
| `DESCRIPCION` | VARCHAR2 | 500 | N |  |
| `VIGENTE_DESDE` | DATE |  | N |  |
| `VIGENTE_HASTA` | DATE |  | Y |  |
| `ACTIVO` | CHAR | 1 | N | `'S'` |

- Secuencia: `SEQ_OBJETIVO_PEI`.
- PK: `PK_CAT_OBJETIVO_PEI (ID_OBJETIVO)`.
- UK: `UK_OP_VERSION_CODIGO (ID_VERSION, CODIGO)`. Oracle aporta mediante esta UK el índice único
  canónico de respaldo, con orden `(ID_VERSION, CODIGO)`; no se crea un índice auxiliar redundante.
- FK: `FK_OP_VERSION` → `CAT_OBJETIVO_PEI_VERSION (ID_VERSION)`.
- CHECKs: análogos a los de la versión.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('DOCUMENTO','DOCUMENTO_SERIE');
SELECT COUNT(*) FROM USER_SEQUENCES WHERE SEQUENCE_NAME='SEQ_DOCUMENTO';
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('CAT_OBJETIVO_PEI_VERSION','CAT_OBJETIVO_PEI');
SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='PROYECTO' AND COLUMN_NAME='OBJETIVO_PEI_ID';
```

#### Orden de creación

1. Secuencias y tabla de cabeceras con PK, UK y CHECKs.
2. Tabla de ítems con FK a cabecera.
3. La UK `UK_OP_VERSION_CODIGO` aporta el índice único canónico por `(ID_VERSION, CODIGO)`;
   no se crea `IDX_OP_VERSION_CODIGO` ni otro índice con la misma lista de columnas.
4. Corrección forward-only 005.1, solo ante la huella parcial confirmada por ORA-01408: valida
   el índice canónico de la UK y registra un marcador técnico no funcional mediante comentario de
   tabla. No reejecuta 005 ni crea un índice adicional.

#### Pruebas SQL

- Positiva: insertar dos `CAT_OBJETIVO_PEI` con el mismo `ID_VERSION` y `CODIGO` debe chocar con la
  UK; el mismo `CODIGO` en versiones distintas debe aceptarse.
- Negativa: una versión con `VIGENTE_HASTA < VIGENTE_DESDE` debe rechazarse por `CK_OPV_VIGENCIA`.

---

### 006 — `database/ddl/organizacion/006_actividad_poi_versionada.sql`

- **Propósito**: introducir versiones independientes de Actividad POI paralelas a las de PEI.
- **Dependencia**: 003.
- **Compensación forward-only**: análoga a 005, con ciclo POI independiente.

#### Tabla `CAT_ACTIVIDAD_POI_VERSION` (nueva)

Misma estructura que `CAT_OBJETIVO_PEI_VERSION`, con prefijo `APV` y secuencia
`SEQ_ACTIVIDAD_POI_VERSION`. UK `UK_APV_CODIGO`. FKs análogas.

#### Tabla `CAT_ACTIVIDAD_POI` (nueva)

Misma estructura que `CAT_OBJETIVO_PEI`, con prefijo `AP`. UK `UK_AP_VERSION_CODIGO`, cuyo índice
único de respaldo canónico conserva el orden `(ID_VERSION, CODIGO)` sin un índice auxiliar
redundante. Secuencia `SEQ_ACTIVIDAD_POI`. FK a la versión POI.

#### Huella de precondiciones

Análoga a 005 sobre `DOCUMENTO` y `DOCUMENTO_SERIE`.

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('CAT_ACTIVIDAD_POI_VERSION','CAT_ACTIVIDAD_POI');
SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='PROYECTO' AND COLUMN_NAME='ACTIVIDAD_POI_ID';
```

#### Orden de creación

Idéntico a 005: `UK_AP_VERSION_CODIGO` aporta el índice único canónico por `(ID_VERSION, CODIGO)`;
no se crea `IDX_AP_VERSION_CODIGO` ni otro índice con la misma lista de columnas.

#### Pruebas SQL

- Positiva: insertar dos versiones POI independientes debe aceptarse; la UK por `CODIGO_VERSION` debe
  impedir duplicados.
- Negativa: una versión POI con `ID_DOCUMENTO_APROBACION` inexistente debe rechazarse por la FK.

---

### 007 — `database/ddl/seguridad/007_matriz_funcional_versionada.sql`

- **Propósito**: introducir matriz funcional versionada con funciones, combinaciones función-perfil-
  unidad concreta, aprobador formal y documento de aprobación.
- **Dependencia**: 003.
- **Compensación forward-only**: detener nuevas versiones; conservar combinaciones históricas.

#### Tabla `MATRIZ_FUNCIONAL_VERSION` (nueva)

| Columna | Tipo | Longitud | Nulable | Default |
|---|---|---|---:|---|
| `ID_VERSION` | NUMBER | 10 | N | (de `SEQ_MATRIZ_VERSION`) |
| `CODIGO_VERSION` | VARCHAR2 | 30 | N |  |
| `ID_VERSION_ANTERIOR` | NUMBER | 10 | Y |  |
| `ID_DOCUMENTO_APROBACION` | NUMBER | 12 | N |  |
| `VIGENTE_DESDE` | DATE |  | N |  |
| `VIGENTE_HASTA` | DATE |  | Y |  |
| `ACTIVA` | CHAR | 1 | N | `'S'` |
| `CREADO_POR` | VARCHAR2 | 100 | N |  |
| `FECHA_CREACION` | TIMESTAMP(6) |  | N | `SYSTIMESTAMP` |

- Secuencia: `SEQ_MATRIZ_VERSION`.
- PK: `PK_MATRIZ_FUNCIONAL_VERSION (ID_VERSION)`.
- UK: `UK_MFV_CODIGO (CODIGO_VERSION)`.
- FKs: `FK_MFV_VERSION_ANTERIOR`, `FK_MFV_DOCUMENTO`.

#### Tabla `MATRIZ_FUNCION` (nueva)

| Columna | Tipo | Longitud | Nulable | Default |
|---|---|---|---:|---|
| `ID_FUNCION` | NUMBER | 10 | N | (de `SEQ_MATRIZ_FUNCION`) |
| `ID_VERSION` | NUMBER | 10 | N |  |
| `CODIGO` | VARCHAR2 | 30 | N |  |
| `DESCRIPCION` | VARCHAR2 | 500 | N |  |
| `ACTIVA` | CHAR | 1 | N | `'S'` |

- Secuencia: `SEQ_MATRIZ_FUNCION`.
- PK: `PK_MATRIZ_FUNCION (ID_FUNCION)`.
- UK: `UK_MF_VERSION_CODIGO (ID_VERSION, CODIGO)`.
- FK: `FK_MF_VERSION` → `MATRIZ_FUNCIONAL_VERSION`.

#### Tabla `MATRIZ_FUNCION_PERFIL_UNIDAD` (nueva)

| Columna | Tipo | Longitud | Nulable | Default |
|---|---|---|---:|---|
| `ID_COMBINACION` | NUMBER | 12 | N | (de `SEQ_MATRIZ_COMBINACION`) |
| `ID_VERSION` | NUMBER | 10 | N |  |
| `ID_FUNCION` | NUMBER | 10 | N |  |
| `ID_ROL` | NUMBER | 5 | N |  |
| `ID_UNIDAD` | NUMBER | 10 | N |  |
| `ID_APROBADOR` | NUMBER | 10 | N |  |
| `ID_REGISTRADOR` | NUMBER | 10 | N |  |
| `ID_DOCUMENTO_APROBACION` | NUMBER | 12 | N |  |
| `VIGENTE_DESDE` | DATE |  | N |  |
| `VIGENTE_HASTA` | DATE |  | Y |  |
| `ACTIVA` | CHAR | 1 | N | `'S'` |
| `CREADO_POR` | VARCHAR2 | 100 | N |  |
| `FECHA_CREACION` | TIMESTAMP(6) |  | N | `SYSTIMESTAMP` |

- Secuencia: `SEQ_MATRIZ_COMBINACION`.
- PK: `PK_MATRIZ_COMBINACION (ID_COMBINACION)`.
- UK: `UK_MFPU_VERSION_FUNCION_PERFIL_UNIDAD (ID_VERSION, ID_FUNCION, ID_ROL, ID_UNIDAD)`.
- FKs: `FK_MFPU_VERSION`, `FK_MFPU_FUNCION`, `FK_MFPU_ROL`, `FK_MFPU_UNIDAD`,
  `FK_MFPU_APROBADOR`, `FK_MFPU_REGISTRADOR`, `FK_MFPU_DOCUMENTO`.
- CHECKs: `CK_MFPU_APROBADOR_DISTINTO_REGISTRADOR` exige `ID_APROBADOR <> ID_REGISTRADOR`.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('DOCUMENTO','DOCUMENTO_SERIE',
  'EXPEDIENTE_INSTITUCIONAL','USUARIO','UNIDAD_EJECUTORA','ROL');
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('MATRIZ_FUNCIONAL_VERSION',
  'MATRIZ_FUNCION','MATRIZ_FUNCION_PERFIL_UNIDAD');
SELECT COUNT(*) FROM USER_SEQUENCES WHERE SEQUENCE_NAME IN ('SEQ_MATRIZ_VERSION',
  'SEQ_MATRIZ_FUNCION','SEQ_MATRIZ_COMBINACION');
```

#### Orden de creación

1. `SEQ_MATRIZ_VERSION` y `MATRIZ_FUNCIONAL_VERSION` con PK/UK/CHECKs.
2. `SEQ_MATRIZ_FUNCION` y `MATRIZ_FUNCION` con FK a versión.
3. `SEQ_MATRIZ_COMBINACION` y `MATRIZ_FUNCION_PERFIL_UNIDAD` con todas las FKs.

#### Pruebas SQL

- Positiva: una combinación con `ID_APROBADOR = ID_REGISTRADOR` debe rechazarse por el CHECK.
- Negativa: dos combinaciones idénticas en la misma versión deben chocar con la UK; en versiones
  distintas deben coexistir.

---

### 008 — `database/ddl/seguridad/008_usuario_rol_unidad_vigencia.sql`

- **Propósito**: evolucionar `USUARIO_ROL_UNIDAD` con vigencia completa, revocación, suplencia,
  historial repetido y combinarlo con la matriz funcional. Añadir `USUARIO_ROL_UNIDAD_EVENTO`,
  `SUPLENCIA_FUNCIONAL` y `OPERACION_APROVISIONAMIENTO`. Permitir `CORREO` y `NOMBRE_COMPLETO`
  nulos en `USUARIO` para la identidad fundacional.
- **Dependencia**: 002, 003, 007.
- **Compensación forward-only**: detener nuevas asignaciones/suplencias; conservar historial.

#### Modificaciones a `USUARIO`

- `CORREO` y `NOMBRE_COMPLETO` se vuelven nullable; `LOGIN` permanece obligatorio (la única fila
  con `LOGIN` nulo es la identidad fundacional creada en 021 con un `LOGIN` sintético basado en el
  `sub`; el `CHECK` se ajusta en consecuencia).
- Se añade `LOGIN_SINTETICO CHAR(1 CHAR) DEFAULT 'N' NOT NULL` con `CK_USR_LOGIN_SINTETICO`.

#### Modificaciones a `USUARIO_ROL_UNIDAD`

- Nuevas columnas: `FECHA_INICIO DATE DEFAULT SYSDATE NOT NULL`, `FECHA_FIN DATE`,
  `REVOCADA_EN TIMESTAMP(6)`, `REVOCADA_POR VARCHAR2(100)`, `MOTIVO_REVOCACION VARCHAR2(2000)`,
  `INACTIVA_TEMPORALMENTE CHAR(1 CHAR) DEFAULT 'N' NOT NULL`,
  `ID_COMBINACION_MATRIZ NUMBER(12)`, `ID_DOCUMENTO_FORMAL NUMBER(12)`, `VERSION NUMBER(10) DEFAULT 0
  NOT NULL`.
- La UK `UK_URU_USR_ROL_UNI` se conserva y pasa a permitir múltiples filas con misma terna
  usuario/rol/unidad pero con `FECHA_INICIO` distinto (semánticamente histórica). Se crea una UK
  adicional `UK_URU_ABIERTAS` por `(ID_USUARIO, ID_ROL, ID_UNIDAD)` con condición
  `FECHA_FIN IS NULL AND REVOCADA_EN IS NULL AND INACTIVA_TEMPORALMENTE='N'`, mediante índice
  único funcional `UX_URU_ABIERTAS ((CASE WHEN FECHA_FIN IS NULL AND REVOCADA_EN IS NULL AND
  INACTIVA_TEMPORALMENTE='N' THEN ID_USUARIO||':'||ID_ROL||':'||ID_UNIDAD END))`.
- CHECKs: `CK_URU_VIGENCIA` (`FECHA_FIN IS NULL OR FECHA_FIN >= FECHA_INICIO`),
  `CK_URU_REVOCADA` (`(REVOCADA_EN IS NULL AND REVOCADA_POR IS NULL) OR
  (REVOCADA_EN IS NOT NULL AND REVOCADA_POR IS NOT NULL)`),
  `CK_URU_INACTIVA_TEMP` ∈ {`S`,`N`}.
- FKs: `FK_URU_COMBINACION` → `MATRIZ_FUNCION_PERFIL_UNIDAD (ID_COMBINACION)` (creada en 008; el
  constraint sigue `ENABLE NOVALIDATE` hasta una futura migración/corte expresamente aprobada);
  `FK_URU_DOCUMENTO_FORMAL` → `DOCUMENTO (ID_DOCUMENTO)`.

#### Tabla `USUARIO_ROL_UNIDAD_EVENTO` (nueva)

Append-only: `ID_EVENTO NUMBER(12)` (PK; `SEQ_URU_EVENTO`), `ID_ASIGNACION NUMBER(10)` (FK a
`USUARIO_ROL_UNIDAD`), `TIPO_EVENTO VARCHAR2(30)` ∈ {`ALTA`,`MODIFICACION`,`REVOCACION`,
`ACTIVACION_TEMPORAL`,`SUPLENCIA`}, `ID_USUARIO_ACTOR NUMBER(10)`, `ID_ROL_ACTOR NUMBER(5)`,
`ID_UNIDAD_ACTOR NUMBER(10)`, `FECHA_EVENTO TIMESTAMP(6) DEFAULT SYSTIMESTAMP NOT NULL`,
`MOTIVO VARCHAR2(2000)`, `ID_ASIGNACION_EFECTIVA NUMBER(10)`. Sin UK; índices por `ID_ASIGNACION` y
`FECHA_EVENTO`.

#### Tabla `SUPLENCIA_FUNCIONAL` (nueva)

`ID_SUPLENCIA NUMBER(12)` (PK; `SEQ_SUPLENCIA_FUNCIONAL`), `ID_ASIGNACION_TITULAR NUMBER(10)` (FK),
`ID_ASIGNACION_SUPLENTE NUMBER(10)` (FK, distinta), `INICIO DATE NOT NULL`, `FIN DATE NOT NULL`,
`TERMINADA_EN TIMESTAMP(6)`, `ID_AUTORIDAD NUMBER(10)`, `ID_DOCUMENTO_FORMAL NUMBER(12)` (FK),
`CREADO_POR VARCHAR2(100)`, `FECHA_CREACION TIMESTAMP(6) DEFAULT SYSTIMESTAMP`. UK por
`(ID_ASIGNACION_TITULAR, INICIO)`. CHECK `CK_SF_DISTINTAS` (`ID_ASIGNACION_TITULAR <>
ID_ASIGNACION_SUPLENTE`) y `CK_SF_VIGENCIA` (`FIN >= INICIO`).

#### Tabla `OPERACION_APROVISIONAMIENTO` (nueva)

`ID_OPERACION NUMBER(12)` (PK; `SEQ_OPERACION_APROVISIONAMIENTO`), `CLAVE_IDEMPOTENTE
VARCHAR2(100)`, `HASH_PAYLOAD VARCHAR2(64)`, `ID_USUARIO_OBJETIVO NUMBER(10)`, `KEYCLOAK_ID
VARCHAR2(36)`, `ESTADO_TECNICO VARCHAR2(30)`, `INTENTO NUMBER(3)`, `ERROR_RECUPERABLE CHAR(1 CHAR)`,
`RESULTADO_ORACLE VARCHAR2(2000)`, `CREADO_POR VARCHAR2(100)`, `FECHA_CREACION TIMESTAMP(6)
DEFAULT SYSTIMESTAMP`, `FECHA_CIERRE TIMESTAMP(6)`. UK `UK_OA_CLAVE (CLAVE_IDEMPOTENTE)`. CHECK
`CK_OA_ESTADO` ∈ {`INICIADA`,`KEYCLOAK_CREADO_DESHABILITADO`,`ORACLE_PENDIENTE`,`COMPLETADA`,
`FALLIDA_NO_RECUPERABLE`}.

##### Corrección 027 — unidad objetivo (VIGENTE)

- **Propósito**: incorporar `ID_UNIDAD_OBJETIVO NUMBER(10)` nullable, sin default, para conservar el alcance solicitado en la operación y permitir revalidar el ámbito en consulta o reintento. Las filas históricas permanecen nulas: no se realiza backfill ni se infiere unidad.
- **Dependencias e integridad**: 008.1 y 026 vigentes; `FK_OA_UNIDAD_OBJETIVO` referencia `UNIDAD_EJECUTORA(ID_UNIDAD)` habilitada y validada. `IDX_OA_UNIDAD_OBJETIVO (ID_UNIDAD_OBJETIVO)` soporta consulta/reintento por unidad.
- **Regla autoritativa**: el servicio de seguridad debe rechazar operaciones ordinarias nuevas sin unidad objetivo. No se agrega `NOT NULL`, trigger ni CHECK porque no hay discriminador físico entre operaciones históricas y nuevas; hacerlo exigiría inventar o modificar datos históricos.
- **Prevalidación, prueba y compensación**: verificar huella 008.1/026 y ausencia de objetos nuevos; agregar columna nullable, FK e índice y validar los 13 campos. `database/tests/027_operacion_aprovisionamiento_unidad_objetivo_test.sql` inserta bajo `SAVEPOINT` y hace `ROLLBACK TO`. Ante error, detener consumidores, conservar huella y emitir corrección forward-only; no eliminar objetos ni backfill.
- **NEEDS CLARIFICATION**: si la obligatoriedad debe imponerse también en Oracle, se requiere una regla aprobada que distinga filas históricas de nuevas y su estrategia de migración.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('USUARIO','USUARIO_ROL_UNIDAD',
  'MATRIZ_FUNCION_PERFIL_UNIDAD','MATRIZ_FUNCIONAL_VERSION','MATRIZ_FUNCION','ROL',
  'UNIDAD_EJECUTORA','DOCUMENTO');
SELECT COUNT(*) FROM USER_CONSTRAINTS WHERE TABLE_NAME='USUARIO_ROL_UNIDAD' AND CONSTRAINT_NAME IN
  ('PK_USR_ROL_UNIDAD','UK_URU_USR_ROL_UNI','FK_URU_USUARIO','FK_URU_ROL','FK_URU_UNIDAD');
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('USUARIO_ROL_UNIDAD_EVENTO',
  'SUPLENCIA_FUNCIONAL','OPERACION_APROVISIONAMIENTO');
SELECT COLUMN_NAME FROM USER_TAB_COLUMNS WHERE TABLE_NAME='USUARIO_ROL_UNIDAD' AND COLUMN_NAME IN
  ('FECHA_INICIO','FECHA_FIN','REVOCADA_EN','ID_COMBINACION_MATRIZ','VERSION');
SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='USUARIO' AND NULLABLE='Y' AND COLUMN_NAME IN
  ('CORREO','NOMBRE_COMPLETO');
```

#### Orden de creación

1. `ALTER TABLE USUARIO MODIFY (CORREO NULL, NOMBRE_COMPLETO NULL, LOGIN NULL)`. `LOGIN NULL`
   se permite solo cuando `LOGIN_SINTETICO='S'`. Insertar el `CHECK` con `ENABLE NOVALIDATE`.
2. `ALTER TABLE USUARIO ADD (LOGIN_SINTETICO ... DEFAULT 'N' NOT NULL)` y
   `ALTER TABLE USUARIO_ROL_UNIDAD ADD (...)` para cada columna.
3. `CREATE SEQUENCE` para las tres tablas nuevas.
4. `CREATE TABLE` con PK/UK/CHECKs/FKs; las FKs hacia `MATRIZ_FUNCION_PERFIL_UNIDAD` se crean
   inicialmente `ENABLE NOVALIDATE` para no bloquear 021.
5. Crear índice funcional `UX_URU_ABIERTAS`.

#### Pruebas SQL

- Positiva: dos `USUARIO_ROL_UNIDAD` con la misma terna usuario/rol/unidad pero distinta
  `FECHA_INICIO` deben coexistir; el índice funcional `UX_URU_ABIERTAS` impide que ambas estén
  abiertas.
- Negativa: una suplencia con `FIN < INICIO` debe rechazarse por `CK_SF_VIGENCIA`; una operación de
  aprovisionamiento con `ESTADO_TECNICO` fuera del CHECK debe rechazarse.

---

### 009 — `database/ddl/portafolio/009_proyecto_campos_oficiales.sql`

- **Propósito**: ampliar `PROYECTO` para soportar los 23 campos oficiales: prefijo de unidad
  aprobado, código inmutable UK, `DETALLE_FUENTE`, `PROBLEMA_PUBLICO`, `SOLUCION_PROPUESTA`,
  `COMPONENTE_DIGITAL`, `DETALLE_COMPONENTE_DIGITAL`, `NOTA`, `OBJETIVO_PEI_ID` y `ACTIVIDAD_POI_ID`
  como FK nullable hasta una futura migración/corte expresamente aprobada, `@Version` y campos de
  subsanación. `ADMINISTRACION` se vuelve
  nullable.
- **Dependencia**: 005, 006.
- **Compensación forward-only**: mantener columnas legacy; no restaurar checks si hay estados nuevos.

#### Modificaciones a `PROYECTO`

- Nuevas columnas nullable: `CODIGO_PREFIJO VARCHAR2(20)`, `DETALLE_FUENTE VARCHAR2(500)`,
  `PROBLEMA_PUBLICO VARCHAR2(2000)`, `SOLUCION_PROPUESTA VARCHAR2(2000)`,
  `COMPONENTE_DIGITAL CHAR(1) DEFAULT 'N' NOT NULL`, `DETALLE_COMPONENTE_DIGITAL VARCHAR2(500)`,
  `NOTA VARCHAR2(1000)`, `OBJETIVO_PEI_ID NUMBER(10)`, `ACTIVIDAD_POI_ID NUMBER(10)`,
  `VERSION NUMBER(10) DEFAULT 0 NOT NULL`, `SUBSANACION_ACTIVA CHAR(1) DEFAULT 'N' NOT NULL`.
- `DESCRIPCION` se mantiene como legacy; no se hace `SPLIT` sin mapeo aprobado.
- `ADMINISTRACION` se vuelve nullable; se conserva el `CHECK` con el dominio canónico.
- UK existente `UK_PROYECTO_CODIGO` se conserva.
- CHECKs nuevos: `CK_PROY_COMPONENTE_DIGITAL` ∈ {`S`,`N`}; `CK_PROY_DETALLE_COMPONENTE`
  (`(COMPONENTE_DIGITAL='N' AND DETALLE_COMPONENTE_DIGITAL IS NULL) OR
  (COMPONENTE_DIGITAL='S' AND DETALLE_COMPONENTE_DIGITAL IS NOT NULL AND
  LENGTH(TRIM(DETALLE_COMPONENTE_DIGITAL)) >= 1)`).
- FKs: `FK_PROY_OBJETIVO_PEI` → `CAT_OBJETIVO_PEI (ID_OBJETIVO)` y
  `FK_PROY_ACTIVIDAD_POI` → `CAT_ACTIVIDAD_POI (ID_ACTIVIDAD)`, ambas `ENABLE NOVALIDATE` hasta una
  futura migración/corte expresamente aprobada.
- Índices: `IDX_PROY_OBJETIVO_PEI`, `IDX_PROY_ACTIVIDAD_POI`, `IDX_PROY_COMPONENTE_DIGITAL`.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('CAT_OBJETIVO_PEI','CAT_ACTIVIDAD_POI',
  'PROYECTO','UNIDAD_EJECUTORA','USUARIO');
SELECT COLUMN_NAME FROM USER_TAB_COLUMNS WHERE TABLE_NAME='PROYECTO' AND COLUMN_NAME IN
  ('ID_PROYECTO','CODIGO','TIPO_REGISTRO','NOMBRE','ESTADO','ID_UNIDAD_EJECUTORA','ID_RESPONSABLE');
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='PROYECTO' AND COLUMN_NAME IN
  ('COMPONENTE_DIGITAL','VERSION','OBJETIVO_PEI_ID','ACTIVIDAD_POI_ID');
```

#### Orden de creación

1. `ALTER TABLE PROYECTO MODIFY (ADMINISTRACION NULL)`.
2. `ALTER TABLE PROYECTO ADD (...)` por cada columna.
3. Crear FKs `ENABLE NOVALIDATE` y los índices.
4. `BACKFILL` opcional: `UPDATE PROYECTO SET COMPONENTE_DIGITAL='N' WHERE COMPONENTE_DIGITAL IS NULL`
   no es necesario porque el default es `'N'`.

#### Pruebas SQL

- Positiva: insertar un `PROYECTO` con `COMPONENTE_DIGITAL='S'` y `DETALLE_COMPONENTE_DIGITAL`
  presente debe aceptarse.
- Negativa: insertar un `PROYECTO` con `COMPONENTE_DIGITAL='S'` y `DETALLE_COMPONENTE_DIGITAL`
  nulo debe rechazarse por `CK_PROY_DETALLE_COMPONENTE`.

---

### 010 — `database/ddl/portafolio/010_iniciativa_proyecto_relacion.sql`

- **Propósito**: introducir la tabla que vincula de forma inmutable una iniciativa aprobada con su
  único proyecto derivado.
- **Dependencia**: 009.
- **Compensación forward-only**: detener nuevas relaciones; conservar vínculos confirmados.

#### Tabla `INICIATIVA_PROYECTO` (nueva)

| Columna | Tipo | Longitud | Nulable | Default |
|---|---|---|---:|---|
| `ID_RELACION` | NUMBER | 12 | N | (de `SEQ_INICIATIVA_PROYECTO`) |
| `ID_INICIATIVA` | NUMBER | 12 | N |  |
| `ID_PROYECTO` | NUMBER | 12 | N |  |
| `CREADA_POR` | VARCHAR2 | 100 | N |  |
| `FECHA_CREACION` | TIMESTAMP(6) |  | N | `SYSTIMESTAMP` |

- Secuencia: `SEQ_INICIATIVA_PROYECTO`.
- PK: `PK_INICIATIVA_PROYECTO (ID_RELACION)`.
- UKs: `UK_IP_INICIATIVA (ID_INICIATIVA)`, `UK_IP_PROYECTO (ID_PROYECTO)`.
- FKs: `FK_IP_INICIATIVA`, `FK_IP_PROYECTO` → `PROYECTO (ID_PROYECTO)`.
- CHECK: `CK_IP_DISTINTOS` (`ID_INICIATIVA <> ID_PROYECTO`).
- Índices: las UK `UK_IP_INICIATIVA` y `UK_IP_PROYECTO` aportan los índices únicos canónicos sobre
  sus respectivas columnas; no se crean `IDX_IP_INICIATIVA` ni `IDX_IP_PROYECTO` redundantes.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME='PROYECTO';
SELECT COUNT(*) FROM USER_TAB_COLUMNS WHERE TABLE_NAME='PROYECTO' AND COLUMN_NAME='VERSION';
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME='INICIATIVA_PROYECTO';
SELECT COUNT(*) FROM USER_SEQUENCES WHERE SEQUENCE_NAME='SEQ_INICIATIVA_PROYECTO';
```

#### Orden de creación

1. `SEQ_INICIATIVA_PROYECTO`.
2. Tabla con PK, UKs, FKs y CHECK.

#### Pruebas SQL

- Positiva: dos relaciones distintas para iniciativas y proyectos diferentes deben aceptarse.
- Negativa: una segunda relación con el mismo `ID_INICIATIVA` debe chocar con `UK_IP_INICIATIVA`.

---

### 011 — `database/ddl/portafolio/011_proyecto_unidades_responsables.sql`

- **Propósito**: introducir la entidad `PROYECTO_RESPONSABLE` para modelar titularidades del registro
  con vigencia, motivo y actor; la UK principal reside en un índice condicional.
- **Dependencia**: 009.
- **Compensación forward-only**: mantener referencia legacy `PROYECTO_UNIDAD_ORGANICA` hasta una
  futura migración/corte expresamente aprobada.

#### Tabla `PROYECTO_RESPONSABLE` (nueva)

| Columna | Tipo | Longitud | Nulable | Default |
|---|---|---|---:|---|
| `ID_TITULARIDAD` | NUMBER | 12 | N | (de `SEQ_PROYECTO_RESPONSABLE`) |
| `ID_PROYECTO` | NUMBER | 12 | N |  |
| `ID_USUARIO` | NUMBER | 10 | N |  |
| `INICIO` | DATE |  | N |  |
| `FIN` | DATE |  | Y |  |
| `MOTIVO_SUSTITUCION` | VARCHAR2 | 2000 | Y |  |
| `ID_ACTOR_SUSTITUCION` | NUMBER | 10 | Y |  |
| `CREADO_POR` | VARCHAR2 | 100 | N |  |
| `FECHA_CREACION` | TIMESTAMP(6) |  | N | `SYSTIMESTAMP` |

- Secuencia: `SEQ_PROYECTO_RESPONSABLE`.
- PK: `PK_PROYECTO_RESPONSABLE (ID_TITULARIDAD)`.
- FKs: `FK_PR_PROYECTO`, `FK_PR_USUARIO`, `FK_PR_ACTOR`.
- CHECKs: `CK_PR_VIGENCIA` (`FIN IS NULL OR FIN >= INICIO`).
- Índice único funcional: `UX_PR_TITULAR_ACTIVO ((CASE WHEN FIN IS NULL THEN ID_PROYECTO END))`
  que garantiza a lo sumo un titular activo por proyecto.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('PROYECTO','USUARIO',
  'PROYECTO_UNIDAD_ORGANICA');
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME='PROYECTO_RESPONSABLE';
SELECT COUNT(*) FROM USER_INDEXES WHERE INDEX_NAME='UX_PR_TITULAR_ACTIVO';
```

#### Orden de creación

1. `SEQ_PROYECTO_RESPONSABLE`.
2. Tabla con PK, FKs, CHECKs.
3. Índice único funcional.

#### Pruebas SQL

- Positiva: dos titularidades con `FIN` no nulo para el mismo `ID_PROYECTO` deben coexistir; una con
  `FIN` nulo debe aceptarse.
- Negativa: dos titularidades con `FIN` nulo para el mismo `ID_PROYECTO` deben chocar con
  `UX_PR_TITULAR_ACTIVO`.

---

### 012 — `database/ddl/portafolio/012_responsables_participantes.sql`

- **Estado de reconciliación**: **VIGENTE por reconciliación histórica**. El usuario confirmó la
  ejecución manual exitosa de `database/tests/012_responsables_participantes_reconciliation_test.sql`:
  tablas, secuencias, constraints e índices coinciden con la huella 012. Los defaults fueron
  revisados separadamente por el DBA conforme a la nota `NO_VERIFICADO_LONG`; el dominio
  `CLASIFICACION` es {`PUBLICO`,`INTERNO`,`RESTRINGIDO`}. Esta confirmación no afirma una nueva
  ejecución del DDL 012. No reejecutar 012 ni usar el incremento 026 para alterar su huella.
- **Propósito**: introducir `PARTICIPANTE_PERSONA`, `PROYECTO_PARTICIPANTE_PERSONA` y
  `PROYECTO_PARTICIPANTE_UNIDAD` para los participantes del registro.
- **Dependencia**: 008, 009, 011.
- **Compensación forward-only**: deshabilitar altas; conservar titularidades y participaciones.

#### Tabla `PARTICIPANTE_PERSONA` (nueva)

| Columna | Tipo | Longitud | Nulable | Default |
|---|---|---|---:|---|
| `ID_PARTICIPANTE` | NUMBER | 12 | N | (de `SEQ_PARTICIPANTE_PERSONA`) |
| `ID_USUARIO` | NUMBER | 10 | Y |  |
| `NOMBRES_COMPLETOS` | VARCHAR2 | 300 | N |  |
| `INSTITUCION` | VARCHAR2 | 200 | Y |  |
| `FUNCION` | VARCHAR2 | 200 | Y |  |
| `CLASIFICACION` | VARCHAR2 | 20 | N | `'RESTRINGIDO'` |

- Secuencia: `SEQ_PARTICIPANTE_PERSONA`.
- PK: `PK_PARTICIPANTE_PERSONA (ID_PARTICIPANTE)`.
- FK: `FK_PP_USUARIO` → `USUARIO (ID_USUARIO)`.
- CHECK: `CK_PP_CLASIFICACION` ∈ {`PUBLICO`,`INTERNO`,`RESTRINGIDO`};
  `CK_PP_DATOS_MINIMOS` (`(ID_USUARIO IS NULL AND NOMBRES_COMPLETOS IS NOT NULL) OR
  (ID_USUARIO IS NOT NULL)`).
- Índice: `IDX_PP_USUARIO`.

#### Tabla `PROYECTO_PARTICIPANTE_PERSONA` (nueva)

`ID_PROY_PART_PERSONA NUMBER(12)` (PK; `SEQ_PROY_PART_PERSONA`), `ID_PROYECTO NUMBER(12)` (FK),
`ID_PARTICIPANTE NUMBER(12)` (FK), `INICIO DATE NOT NULL`, `FIN DATE`, `ID_ACTOR NUMBER(10)` (FK a
`USUARIO`), `CREADO_POR`, `FECHA_CREACION`. UK `UK_PPP_PROY_PART (ID_PROYECTO, ID_PARTICIPANTE)`.
CHECK `CK_PPP_VIGENCIA`.

#### Tabla `PROYECTO_PARTICIPANTE_UNIDAD` (nueva)

`ID_PROY_PART_UNIDAD NUMBER(12)` (PK; `SEQ_PROY_PART_UNIDAD`), `ID_PROYECTO NUMBER(12)` (FK),
`ID_UNIDAD NUMBER(10)` (FK), `INICIO DATE NOT NULL`, `FIN DATE`, `ID_ACTOR NUMBER(10)`, `CREADO_POR`,
`FECHA_CREACION`. UK `UK_PPU_PROY_UNI (ID_PROYECTO, ID_UNIDAD)`.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('PROYECTO','USUARIO','UNIDAD_EJECUTORA',
  'PROYECTO_RESPONSABLE');
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('PARTICIPANTE_PERSONA',
  'PROYECTO_PARTICIPANTE_PERSONA','PROYECTO_PARTICIPANTE_UNIDAD');
```

#### Orden de creación

1. Secuencias en orden de dependencias: `SEQ_PARTICIPANTE_PERSONA`, `SEQ_PROY_PART_PERSONA`,
   `SEQ_PROY_PART_UNIDAD`.
2. `PARTICIPANTE_PERSONA` con FK a `USUARIO`.
3. `PROYECTO_PARTICIPANTE_PERSONA` con FKs a `PROYECTO` y `PARTICIPANTE_PERSONA`.
4. `PROYECTO_PARTICIPANTE_UNIDAD` con FKs a `PROYECTO` y `UNIDAD_EJECUTORA`.

#### Pruebas SQL

- Positiva: insertar un participante sin `ID_USUARIO` y con `NOMBRES_COMPLETOS` debe aceptarse.
- Negativa: insertar un `PROYECTO_PARTICIPANTE_PERSONA` duplicado para el mismo par
  `ID_PROYECTO/ID_PARTICIPANTE` debe chocar con la UK.

---

### 013 — `database/ddl/portafolio/013_clasificacion_campos.sql`

- **Propósito**: introducir la matriz append-only de obligatoriedad, editabilidad, privacidad y
  actor responsable por tipo de registro y etapa. Confirmación humana obligatoria antes de
  formularios, validaciones, consultas o reportes dependientes.
- **Dependencia**: 002, 009.
- **Compensación forward-only**: volver datos nuevos no publicables; nunca ampliar acceso.

#### Tabla `PROYECTO_CAMPO_CLASIFICACION` (nueva)

`ID_CLASIFICACION NUMBER(12)` (PK; `SEQ_PROY_CAMPO_CLASIF`), `TIPO_REGISTRO VARCHAR2(20)` (FK a
dominio canónico), `ETAPA VARCHAR2(30)`, `NRO_CAMPO NUMBER(3)`, `CLASIFICACION VARCHAR2(20)`, `EDITABLE
CHAR(1)`, `ID_ROL_EDITOR NUMBER(5)`, `OBLIGATORIO CHAR(1)`, `ACTIVA CHAR(1) DEFAULT 'S'`, `CREADO_POR`,
`FECHA_CREACION`. UK `UK_PCC_TIPO_ETAPA_CAMPO (TIPO_REGISTRO, ETAPA, NRO_CAMPO)`. CHECKs de
clasificación, editable y obligatorio ∈ {`S`,`N`}.

#### Tabla `PROYECTO_CAMPO_CLASIF_HIST` (nueva)

Append-only: `ID_HISTORIAL NUMBER(12)` (PK; `SEQ_PROY_CAMPO_CLASIF_HIST`), `ID_CLASIFICACION
NUMBER(12)` (FK), `CLASIFICACION_ANTERIOR`, `CLASIFICACION_NUEVA`, `EDITABLE_ANTERIOR`,
`EDITABLE_NUEVO`, `ID_ACTOR`, `FECHA_CAMBIO`, `MOTIVO`, `ID_DOCUMENTO_DECISION`. CHECKs análogos.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('PROYECTO','ROL');
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('PROYECTO_CAMPO_CLASIFICACION',
  'PROYECTO_CAMPO_CLASIF_HIST');
```

#### Orden de creación

1. Secuencias.
2. `PROYECTO_CAMPO_CLASIFICACION` con UK.
3. `PROYECTO_CAMPO_CLASIF_HIST` con FK a la primera.

#### Pruebas SQL

- Positiva: insertar dos clasificaciones para el mismo `TIPO_REGISTRO/ETAPA/NRO_CAMPO` debe chocar
  con la UK; cambiar la clasificación debe generar una fila en el historial.
- Negativa: una reclasificación a `PUBLICO` desde `RESTRINGIDO` debe aceptarse; el camino inverso
  debe aceptarse también porque la matriz es append-only, pero la aplicación debe bloquearlo en
  Java.

---

### 014 — `database/ddl/portafolio/014_evaluacion_transiciones.sql`

- **Propósito**: introducir las entidades de evaluación, subsanación y aplicabilidad de iniciativas.
- **Dependencia**: 003, 008, 009.
- **Compensación forward-only**: detener comandos; no revertir estados confirmados.
- **Corrección 014.1 (cuarta corrección forward-only)**: la precondición de "objetos previos" del script `014.1_subsanacion_iniciativa_plazo.sql` exige exactamente 27 secuencias vigentes (10 de 001 + 1 de 002 + 2 de 003 + 2 de 004 + 2 de 005 + 2 de 006 + 3 de 007 + 3 de 008.1 + 2 de 014 parcial). No incluye las secuencias de 010, 011, 012, 013 ni de 015, 016, 017. La huella previa a 014.1 fue recalculada tras el fallo ORA-20101 que detectó el bug de conteo original (32 esperado).

#### Tabla `EVALUACION_INICIATIVA` (nueva)

`ID_EVALUACION NUMBER(12)` (PK; `SEQ_EVALUACION_INICIATIVA`), `ID_INICIATIVA NUMBER(12)` (FK),
`ID_EVALUADOR NUMBER(10)`, `ID_ROL_EFECTIVO NUMBER(5)`, `ID_UNIDAD_EFECTIVA NUMBER(10)`,
`FECHA_EVALUACION TIMESTAMP(6) DEFAULT SYSTIMESTAMP`, `OBSERVACIONES VARCHAR2(2000)`,
`ID_DOCUMENTO_OPINION NUMBER(12)` (FK a `DOCUMENTO`). UK por iniciativa: `UK_EI_INICIATIVA
(ID_INICIATIVA)`. CHECK `CK_EI_OBSERVACION` exige `LENGTH(TRIM(OBSERVACIONES)) >= 20` si el destino
final es `NO_ADMISIBLE` o `NO_APLICABLE` (validado en Java).

#### Tabla `SUBSANACION_INICIATIVA` (nueva)

`ID_SUBSANACION NUMBER(12)` (PK; `SEQ_SUBSANACION_INICIATIVA`), `ID_INICIATIVA NUMBER(12)` (FK),
`PLAZO DATE NOT NULL`, `INCUMPLIMIENTOS CLOB NOT NULL`, `APERTURA_EN TIMESTAMP(6)`, `ATENCION_EN
TIMESTAMP(6)`, `ID_ACTOR NUMBER(10)`. UK `UK_SI_INICIATIVA (ID_INICIATIVA)` para garantizar una
sola oportunidad. CHECK `CK_SI_PLAZO` (`PLAZO IS NULL OR APERTURA_EN IS NULL OR PLAZO > APERTURA_EN`)
— invariante determinista que modela la regla de negocio "la subsanación solo puede ocurrir
después de abierta" comparando dos columnas persistidas. La forma original
`PLAZO >= TRUNC(SYSDATE)` se rechazó con ORA-02436 porque Oracle prohíbe
`SYSDATE`/`TRUNC(SYSDATE)` dentro de un CHECK.

#### Tabla `APLICABILIDAD_INICIATIVA` (nueva)

`ID_APLICABILIDAD NUMBER(12)` (PK; `SEQ_APLICABILIDAD_INICIATIVA`), `ID_INICIATIVA NUMBER(12)` (FK),
`RESULTADO VARCHAR2(20)` ∈ {`APLICABLE`,`NO_APLICABLE`}, `MOTIVO VARCHAR2(2000)`, `ID_EVALUADOR
NUMBER(10)`, `FECHA TIMESTAMP(6)`. UK `UK_AI_INICIATIVA (ID_INICIATIVA)`. CHECK `CK_AI_MOTIVO`
exige motivo no nulo cuando `RESULTADO='NO_APLICABLE'`.

#### Tabla `APLICABILIDAD_CRITERIO` (nueva)

`ID_CRITERIO NUMBER(12)` (PK; `SEQ_APLICABILIDAD_CRITERIO`), `ID_APLICABILIDAD NUMBER(12)` (FK),
`CLAVE VARCHAR2(50)`, `VALOR VARCHAR2(500)`, `ORDEN NUMBER(3)`. UK por `ID_APLICABILIDAD,CLAVE`.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('PROYECTO','DOCUMENTO',
  'USUARIO_ROL_UNIDAD','ROL','UNIDAD_EJECUTORA');
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('EVALUACION_INICIATIVA',
  'SUBSANACION_INICIATIVA','APLICABILIDAD_INICIATIVA','APLICABILIDAD_CRITERIO');
```

#### Orden de creación

1. Secuencias en orden de dependencias: `SEQ_EVALUACION_INICIATIVA`, `SEQ_SUBSANACION_INICIATIVA`,
   `SEQ_APLICABILIDAD_INICIATIVA`, `SEQ_APLICABILIDAD_CRITERIO`.
2. `EVALUACION_INICIATIVA` con FKs.
3. `SUBSANACION_INICIATIVA` con FKs.
4. `APLICABILIDAD_INICIATIVA` con FKs.
5. `APLICABILIDAD_CRITERIO` con FK a la anterior.

#### Pruebas SQL

- Positiva: una sola subsanación por iniciativa debe ser posible; el segundo intento debe chocar
  con la UK.
- Negativa: `APLICABILIDAD_INICIATIVA` con `RESULTADO='NO_APLICABLE'` y `MOTIVO` nulo debe
  rechazarse.

---

### 015 — `database/ddl/portafolio/015_ciclos_resultados_cierre.sql`

- **Propósito**: introducir planificación, ciclos, productos parciales, presentación del producto
  final, validación de resultados y cierre del proyecto.
- **Dependencia**: 003, 009, 014.
- **Compensación forward-only**: detener cierres/ciclos; conservar versiones cerradas.

#### Tabla `PLANIFICACION_PROYECTO` (nueva)

`ID_PLANIFICACION NUMBER(12)` (PK; `SEQ_PLANIFICACION_PROYECTO`), `ID_PROYECTO NUMBER(12)` (FK),
`ALCANCE VARCHAR2(2000)`, `OBJETIVOS VARCHAR2(2000)`, `ENTREGABLES CLOB`, `PERIODOS CLOB`,
`VERSION NUMBER(3)`, `ID_VERSION_ANTERIOR NUMBER(12)`, `CERRADA CHAR(1) DEFAULT 'N'`, `CREADO_POR`,
`FECHA_CREACION`. UK `UK_PP_PROY_VERSION (ID_PROYECTO, VERSION)`. CHECK `CK_PP_VERSION_MIN`
(`VERSION >= 1`).

#### Tabla `CICLO_PROYECTO` (nueva)

`ID_CICLO NUMBER(12)` (PK; `SEQ_CICLO_PROYECTO`), `ID_PROYECTO NUMBER(12)` (FK), `PERIODO
VARCHAR2(20)` (formato `YYYY-Qn-Sn` quincenal), `NUMERO_VERSION NUMBER(3)`, `ID_VERSION_ANTERIOR
NUMBER(12)`, `OBJETIVOS VARCHAR2(2000)`, `ACTIVIDADES VARCHAR2(2000)`, `AVANCE NUMBER(5,2)`,
`DIFICULTADES VARCHAR2(2000)`, `PROXIMAS_ACCIONES VARCHAR2(2000)`, `CERRADO CHAR(1) DEFAULT 'N'`,
`CREADO_POR`, `FECHA_CREACION`, `FECHA_CIERRE TIMESTAMP(6)`. UK `UK_CP_PROY_PERIODO_VERSION
(ID_PROYECTO, PERIODO, NUMERO_VERSION)`. CHECK `CK_CP_AVANCE` (`AVANCE BETWEEN 0 AND 100`).

#### Tabla `CICLO_EVIDENCIA` (nueva)

`ID_EVIDENCIA NUMBER(12)` (PK; `SEQ_CICLO_EVIDENCIA`), `ID_CICLO NUMBER(12)` (FK), `ID_DOCUMENTO
NUMBER(12)` (FK a `DOCUMENTO`), `CREADO_POR`, `FECHA_CREACION`. UK `UK_CE_CICLO_DOC
(ID_CICLO, ID_DOCUMENTO)`.

#### Tabla `PRODUCTO_PARCIAL` (nueva)

`ID_PRODUCTO NUMBER(12)` (PK; `SEQ_PRODUCTO_PARCIAL`), `ID_CICLO NUMBER(12)` (FK), `DESCRIPCION
VARCHAR2(2000)`, `RESULTADO CLOB`, `FECHA DATE`, `ID_RESPONSABLE NUMBER(10)`, `VERSION NUMBER(3)`,
`ID_VERSION_ANTERIOR NUMBER(12)`, `CREADO_POR`, `FECHA_CREACION`. UK por
`(ID_CICLO, VERSION)`.

#### Tabla `PRESENTACION_PRODUCTO_FINAL` (nueva)

`ID_PRESENTACION NUMBER(12)` (PK; `SEQ_PRESENTACION_PRODUCTO_FINAL`), `ID_PROYECTO NUMBER(12)` (FK),
`VERSION NUMBER(3)`, `ID_VERSION_ANTERIOR NUMBER(12)`, `DESCRIPCION VARCHAR2(2000)`, `ID_RESPONSABLE
NUMBER(10)`, `ID_DOCUMENTO_SUSTENTA NUMBER(12)` (FK), `FECHA_PRESENTACION TIMESTAMP(6)`. UK por
`(ID_PROYECTO, VERSION)`.

#### Tabla `VALIDACION_RESULTADO` (nueva)

`ID_VALIDACION NUMBER(12)` (PK; `SEQ_VALIDACION_RESULTADO`), `ID_PROYECTO NUMBER(12)` (FK),
`ID_RESPONSABLE NUMBER(10)`, `ID_EVALUADOR NUMBER(10)`, `RESULTADOS_CLAVE CLOB`, `VALIDADO_EN
TIMESTAMP(6)`, `CREADO_POR`, `FECHA_CREACION`. UK por proyecto: `UK_VR_PROYECTO (ID_PROYECTO)`.

#### Tabla `CIERRE_PROYECTO` (nueva)

`ID_CIERRE NUMBER(12)` (PK; `SEQ_CIERRE_PROYECTO`), `ID_PROYECTO NUMBER(12)` (FK), `INFORME_FINAL
CLOB`, `RESULTADOS CLOB`, `APRENDIZAJES CLOB`, `CONCLUSION VARCHAR2(2000)`, `OBSERVACION
VARCHAR2(2000)`, `ID_EVALUADOR NUMBER(10)`, `FECHA_CIERRE TIMESTAMP(6) DEFAULT SYSTIMESTAMP`. UK
`UK_CIERRE_PROY (ID_PROYECTO)`. CHECK `CK_CP_FECHA` (`FECHA_CIERRE IS NOT NULL`).

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('PROYECTO','DOCUMENTO','USUARIO',
  'EVALUACION_INICIATIVA');
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('PLANIFICACION_PROYECTO','CICLO_PROYECTO',
  'CICLO_EVIDENCIA','PRODUCTO_PARCIAL','PRESENTACION_PRODUCTO_FINAL','VALIDACION_RESULTADO',
  'CIERRE_PROYECTO');
```

#### Orden de creación

1. Secuencias.
2. `PLANIFICACION_PROYECTO` con FK a `PROYECTO`.
3. `CICLO_PROYECTO` con FK a `PROYECTO`.
4. `CICLO_EVIDENCIA` con FK a `CICLO_PROYECTO` y `DOCUMENTO`.
5. `PRODUCTO_PARCIAL` con FK a `CICLO_PROYECTO`.
6. `PRESENTACION_PRODUCTO_FINAL` con FK a `PROYECTO` y `DOCUMENTO`.
7. `VALIDACION_RESULTADO` con FK a `PROYECTO` y `USUARIO`.
8. `CIERRE_PROYECTO` con FK a `PROYECTO`.

#### Pruebas SQL

- Positiva: dos versiones de planificación para el mismo proyecto deben aceptarse; la UK por
  `(ID_PROYECTO, VERSION)` impide colisiones.
- Negativa: una planificación con `AVANCE > 100` debe rechazarse por `CK_CP_AVANCE` (en
  `CICLO_PROYECTO`).

---

### 016 — `database/ddl/portafolio/016_incorporacion_individual.sql`

- **Propósito**: introducir las entidades de incorporación individual de registros.
- **Dependencia**: 003, 010, 012.
- **Compensación forward-only**: mantener expedientes `PENDIENTE`; no borrar evidencia.

#### Tabla `INCORPORACION_REGISTRO` (nueva)

`ID_INCORPORACION NUMBER(12)` (PK; `SEQ_INCORPORACION_REGISTRO`), `FUENTE VARCHAR2(200)`,
`FECHA_FUENTE DATE`, `ID_RESPONSABLE NUMBER(10)`, `ID_DOCUMENTO_FUENTE NUMBER(12)` (FK a
`DOCUMENTO`), `HASH_ORIGINAL VARCHAR2(64)`, `DATOS_ORIGINALES CLOB`, `ESTADO VARCHAR2(20) DEFAULT
'PENDIENTE'`, `ID_REGISTRO_VINCULADO NUMBER(12)` (FK opcional a `PROYECTO`), `CREADO_POR`,
`FECHA_CREACION`. UK por hash: `UK_IR_HASH_FUENTE_RESPONSABLE (HASH_ORIGINAL, ID_RESPONSABLE,
FUENTE)`. CHECK `CK_IR_ESTADO` ∈ {`PENDIENTE`,`VALIDADO`,`RECHAZADO`}.

#### Tabla `INCORPORACION_CAMBIO` (nueva)

Append-only: `ID_CAMBIO NUMBER(12)` (PK; `SEQ_INCORPORACION_CAMBIO`), `ID_INCORPORACION NUMBER(12)`
(FK), `DATOS_ANTES CLOB`, `DATOS_DESPUES CLOB`, `MOTIVO VARCHAR2(2000)`, `ID_ACTOR NUMBER(10)`,
`FECHA_CAMBIO TIMESTAMP(6)`. Sin UK; índice por `ID_INCORPORACION`.

#### Tabla `INCORPORACION_CONFLICTO` (nueva)

`ID_CONFLICTO NUMBER(12)` (PK; `SEQ_INCORPORACION_CONFLICTO`), `ID_INCORPORACION NUMBER(12)` (FK),
`TIPO_CONFLICTO VARCHAR2(30)` ∈ {`CODIGO`,`DUPLICADO`,`RELACION`,`MAPEO`}, `ID_REGISTRO_CONFLICTIVO
NUMBER(12)`, `DESCRIPCION VARCHAR2(2000)`, `RESUELTO CHAR(1) DEFAULT 'N'`, `ID_RESOLUTOR
NUMBER(10)`, `FECHA_RESOLUCION TIMESTAMP(6)`, `ID_DOCUMENTO_RESOLUCION NUMBER(12)` (FK a
`DOCUMENTO`). UK por `(ID_INCORPORACION, TIPO_CONFLICTO, ID_REGISTRO_CONFLICTIVO)`.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('PROYECTO','DOCUMENTO','USUARIO',
  'INICIATIVA_PROYECTO');
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('INCORPORACION_REGISTRO',
  'INCORPORACION_CAMBIO','INCORPORACION_CONFLICTO');
```

#### Orden de creación

1. Secuencias.
2. `INCORPORACION_REGISTRO` con FKs y CHECK.
3. `INCORPORACION_CAMBIO` con FK.
4. `INCORPORACION_CONFLICTO` con FKs y UK.

#### Pruebas SQL

- Positiva: dos incorporaciones con distinto `HASH_ORIGINAL` deben coexistir; un cambio append-only
  no debe alterar `INCORPORACION_REGISTRO`.
- Negativa: un conflicto con `TIPO_CONFLICTO` fuera del CHECK debe rechazarse.

---

### 026 — `database/ddl/portafolio/026_incorporacion_registro_observacion_version.sql`

- **Estado**: VIGENTE. El usuario confirmó el 2026-07-23 la ejecución manual exitosa del DDL y de
  `database/tests/026_incorporacion_registro_observacion_version_test.sql`; la prueba informó
  `T026 OK: OBSERVACION y VERSION=0 verificadas bajo SAVEPOINT`, completó su bloque PL/SQL y
  revirtió su DML de prueba al savepoint.
- **Propósito**: corregir T048 mediante dos columnas aditivas en `INCORPORACION_REGISTRO`: el texto
  de observación y el contador requerido por `@Version` para concurrencia optimista JPA.
- **Dependencias**: 016 VIGENTE (`INCORPORACION_REGISTRO` con sus once columnas originales) y 025
  VIGENTE como último incremento estructural precedente.
- **Compensación forward-only**: detener altas y actualizaciones de incorporación, preservar filas y
  versiones materializadas, inventariar la huella con el DBA y depositar una corrección versionada.
  No eliminar columnas ni hacer rollback físico.

#### Modificaciones a `INCORPORACION_REGISTRO`

| Columna | Tipo | Longitud | Nulable | Default | Propósito |
|---|---|---:|---:|---|---|
| `OBSERVACION` | VARCHAR2 | 2000 CHAR | Y |  | Observación de la incorporación. |
| `VERSION` | NUMBER | 10 | N | `0` | Contador de concurrencia optimista para `@Version`. |

- No se crean índices, constraints, secuencias, triggers ni reglas de negocio nuevas.
- La semántica de concurrencia permanece en JPA; Oracle solo persiste el contador inicial `0`.
- **NEEDS CLARIFICATION**: `IncorporacionRegistroEntity` declara `OBSERVACION` con `length=2000`,
  pero sin `nullable=false`. Por alineación literal del mapeo vigente se documenta nullable y sin
  default; la afirmación de obligatoriedad para esa columna debe resolverse antes de una futura
  restricción `NOT NULL`.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('INCORPORACION_REGISTRO',
  'CICLO_PROYECTO_VERSION', 'PRESENTACION_PRODUCTO_FINAL_EVIDENCIA');
SELECT COUNT(*) FROM USER_CONSTRAINTS WHERE CONSTRAINT_NAME IN
  ('PK_INCORPORACION_REGISTRO', 'PK_CICLO_PROYECTO_VERSION', 'PK_PPF_EVIDENCIA');
SELECT COLUMN_NAME, DATA_TYPE, CHAR_LENGTH, DATA_PRECISION, DATA_SCALE, NULLABLE
  FROM USER_TAB_COLUMNS WHERE TABLE_NAME = 'INCORPORACION_REGISTRO';
```

`USER_TAB_COLUMNS.DATA_DEFAULT` es `LONG`; el DDL 026 no lo manipula para evitar ORA-00932. La
prueba manual confirmada verificó funcionalmente `VERSION=0` al omitir la columna en el `INSERT`.

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TAB_COLUMNS
 WHERE TABLE_NAME = 'INCORPORACION_REGISTRO'
   AND COLUMN_NAME IN ('OBSERVACION', 'VERSION');
```

#### Orden de creación

1. Validar las huellas 016 y 025, las once columnas originales y la ausencia de ambas columnas.
2. Ejecutar un único `ALTER TABLE INCORPORACION_REGISTRO ADD (OBSERVACION, VERSION)`; Oracle aplica
   commits implícitos de DDL.
3. Validar tipo, longitud y nulabilidad de las dos columnas, además del total de trece columnas.
4. Ejecutar la prueba DML bajo savepoint. Ejecución confirmada el 2026-07-23; el catálogo físico
   vigente fue sincronizado con esta evidencia.

#### Prueba SQL

- Positiva: insertar una incorporación con `OBSERVACION` de 2000 caracteres y omitir `VERSION` debe
  persistir `VERSION=0`.
- Limpieza: `ROLLBACK TO T026_INCORPORACION` revierte exclusivamente la fila DML de prueba; las
  secuencias conservan su salto y el DDL no se revierte.

---

### 017 — `database/ddl/reportes/017_reporte_expediente_remision.sql`

- **Propósito**: introducir el ciclo de reportes institucionales con snapshot, archivos, aprobación,
  destinatarios y remisión.
- **Dependencia**: 002, 003, 009, 015.
- **Compensación forward-only**: detener generación/remisión; conservar expedientes.

#### Tabla `REPORTE_INSTITUCIONAL` (nueva)

`ID_REPORTE NUMBER(12)` (PK; `SEQ_REPORTE_INSTITUCIONAL`), `TIPO VARCHAR2(30)` ∈ {`SEMESTRAL`,
`EXTRAORDINARIO`}, `ANIO NUMBER(4)`, `SEMESTRE NUMBER(1)`, `PERIODO VARCHAR2(30)`,
`FECHA_CORTE DATE`, `PARAMETROS CLOB`, `ID_SNAPSHOT NUMBER(12)`, `VERSION_DATOS NUMBER(5)`,
`CLASIFICACION VARCHAR2(20)`, `ID_GENERADOR NUMBER(10)`, `FECHA_GENERACION TIMESTAMP(6) DEFAULT
SYSTIMESTAMP`, `ESTADO_TECNICO VARCHAR2(20)`. CHECKs `CK_RE_TIPO`, `CK_RE_CLASIFICACION` ∈
{`INTERNO`,`RESTRINGIDO`}, `CK_RE_CORTE` para cortes 30/06 y 31/12 cuando `TIPO='SEMESTRAL'`.

#### Tabla `REPORTE_SNAPSHOT` (nueva)

`ID_SNAPSHOT NUMBER(12)` (PK; `SEQ_REPORTE_SNAPSHOT`), `PAYLOAD_JSON CLOB NOT NULL`,
`VERSION_ESQUEMA NUMBER(5)`, `HASH_SHA256 VARCHAR2(64)`, `FECHA_CORTE DATE NOT NULL`, `PARAMETROS
CLOB`, `CLASIFICACION VARCHAR2(20)`, `CREADO_POR`, `FECHA_CREACION`. UK `UK_RS_HASH (HASH_SHA256)`.
CHECK `CK_RS_PAYLOAD_JSON` `PAYLOAD_JSON IS JSON`. CHECK `CK_RS_HASH` regex de 64 hex.

#### Tabla `REPORTE_ARCHIVO` (nueva)

`ID_ARCHIVO NUMBER(12)` (PK; `SEQ_REPORTE_ARCHIVO`), `ID_REPORTE NUMBER(12)` (FK),
`FORMATO VARCHAR2(10)` ∈ {`PDF`,`XLSX`}, `VERSION NUMBER(5)`, `HASH_SHA256 VARCHAR2(64)`,
`ID_DOCUMENTO_VERSION NUMBER(12)` (FK a `DOCUMENTO`), `CREADO_POR`, `FECHA_CREACION`. UK
`UK_RA_REPORTE_FORMATO_VERSION (ID_REPORTE, FORMATO, VERSION)`.

#### Tabla `REPORTE_APROBACION` (nueva)

`ID_APROBACION NUMBER(12)` (PK; `SEQ_REPORTE_APROBACION`), `ID_REPORTE NUMBER(12)` (FK), `ID_VERSION
NUMBER(5)`, `ID_OFICINA NUMBER(10)`, `ID_APROBADOR NUMBER(10)`, `ID_DOCUMENTO_APROBACION
NUMBER(12)` (FK), `FECHA_APROBACION TIMESTAMP(6)`. UK `UK_RAP_REPORTE_VERSION (ID_REPORTE,
ID_VERSION)`.

#### Tabla `REPORTE_DESTINATARIO` (nueva)

`ID_DESTINATARIO NUMBER(12)` (PK; `SEQ_REPORTE_DESTINATARIO`), `ID_APROBACION NUMBER(12)` (FK),
`TIPO_DESTINATARIO VARCHAR2(30)` ∈ {`AUTORIDAD_MIDAGRI`,`OFICINA_MODERNIZACION`,`PCM_SGP`},
`ID_ENTIDAD NUMBER(10)`, `NOMBRE VARCHAR2(200)`. UK `UK_RD_APROBACION_TIPO_ENTIDAD
(ID_APROBACION, TIPO_DESTINATARIO, ID_ENTIDAD)`.

#### Tabla `REPORTE_REMISION` (nueva)

`ID_REMISION NUMBER(12)` (PK; `SEQ_REPORTE_REMISION`), `ID_REPORTE NUMBER(12)` (FK),
`ID_DESTINATARIO NUMBER(12)` (FK), `FECHA_REMISION TIMESTAMP(6)`, `RESULTADO VARCHAR2(20)`, `MOTIVO
VARCHAR2(2000)`. UK por `(ID_REPORTE, ID_DESTINATARIO, FECHA_REMISION)`.

#### Huella de precondiciones

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('PROYECTO','DOCUMENTO','USUARIO',
  'CICLO_PROYECTO','PRESENTACION_PRODUCTO_FINAL');
```

#### Consultas de incompatibilidad

```sql
SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('REPORTE_INSTITUCIONAL','REPORTE_SNAPSHOT',
  'REPORTE_ARCHIVO','REPORTE_APROBACION','REPORTE_DESTINATARIO','REPORTE_REMISION');
```

#### Orden de creación

1. Secuencias en orden de dependencias.
2. `REPORTE_INSTITUCIONAL` (con FK a snapshot diferida).
3. `REPORTE_SNAPSHOT`.
4. `ALTER TABLE REPORTE_INSTITUCIONAL ADD CONSTRAINT FK_RE_SNAPSHOT ...`.
5. `REPORTE_ARCHIVO` con FKs.
6. `REPORTE_APROBACION` con FKs.
7. `REPORTE_DESTINATARIO` con FK.
8. `REPORTE_REMISION` con FKs.

#### Pruebas SQL

- Positiva: dos snapshots con el mismo `HASH_SHA256` deben chocar con la UK; un `PAYLOAD_JSON` que
  no sea JSON válido debe rechazarse por `CK_RS_PAYLOAD_JSON`.
- Negativa: una aprobación con `ID_VERSION` no presente en el `REPORTE` debe aceptarse (es una
  versión del reporte), pero la consistencia cruzada se valida en Java.

---

### 019 — `database/seeds/019_catalogos_canonicos_portafolio.sql`

- **Propósito**: asegurar los 6 roles canónicos del portafolio, ampliar
  `TIPO_DOCUMENTO` con 13 tipos documentales aprobados e inactivar las
  transiciones legacy que ya no son autoridad. Operación idempotente.
- **Dependencias**: 001-017 VIGENTES.
- **Compensación forward-only**: inactivar semillas no referenciadas;
  nunca borrar referencias.

#### Huella esperada de la semilla

- 6 filas en `ROL` con `NOMBRE_ROL IN (GLOBAL_ADMIN, UNIDAD_ADMIN, RESPONSABLE,
  EVALUADOR, AUTORIDAD, CONSULTA)`. La descripción de `UNIDAD_ADMIN` se
  normaliza a `Administrador de unidad ejecutora` sin duplicar filas.
- 13 filas en `TIPO_DOCUMENTO` con los nombres canónicos del portafolio
  (Ficha de Iniciativa, Informe de Opinión Técnica, Documento Formal de
  Decisión, Documento de Aprobación o Autorización, Nota Conceptual,
  Matriz de Planificación, Seguimiento Ágil, Autoevaluación de Ciclo,
  Documento de Aprobación de Producto, Evidencia de No Aprobación,
  Informe Final de Cierre, Evidencia de Suspensión, Informe de
  Cancelación), con `CONTEXTO='PORTAFOLIO'` y `ESTADO_ASOCIADO` ajustado
  a la máquina de estados canónica.
- `TRANSICION_PERMITIDA`: filas legacy no canónicas marcadas con
  `ACTIVO='N'`. No se borran filas; la máquina de estados reside en
  `TransicionEstadoService` Java.

---

### 020 — `database/seeds/020_planeamiento_inicial_aprobado.sql`

- **Propósito**: crear la versión inicial de planeamiento PEI/POI con
  sus ítems de ejemplo, preservando la separación constitucional entre
  ambos ciclos.
- **Dependencias**: 005, 005.1 y 006 VIGENTES.
- **Placeholders obligatorios** (sustituir antes de la ejecución manual):
  `<<PEI_VERSION_CODIGO>>`, `<<POI_VERSION_CODIGO>>`,
  `<<OFICINA_APROBADORA_PEI>>`, `<<OFICINA_APROBADORA_POI>>`,
  `<<ID_DOCUMENTO_APROBACION_PEI>>`, `<<ID_DOCUMENTO_APROBACION_POI>>` e
  `<<ID_ACTOR_PLANEAMIENTO>>`. **No se infieren valores**.
- **Compensación forward-only**: inactivar versiones; nunca borrar
  referencias. Datasets y aprobaciones se documentan como
  `NEEDS CLARIFICATION` hasta que el área de planeamiento entregue los
  documentos formales.

#### Huella esperada de la semilla

- 1 fila en `CAT_OBJETIVO_PEI_VERSION` con
  `CODIGO_VERSION = <<PEI_VERSION_CODIGO>>`.
- 1 fila en `CAT_ACTIVIDAD_POI_VERSION` con
  `CODIGO_VERSION = <<POI_VERSION_CODIGO>>`.
- 3 ítems de ejemplo en `CAT_OBJETIVO_PEI` con códigos
  `OBJ_PEI_EJEMPLO_1/2/3`.
- 3 ítems de ejemplo en `CAT_ACTIVIDAD_POI` con códigos
  `ACT_POI_EJEMPLO_1/2/3`.

---

### 021 — `database/seeds/021_matriz_funcional_inicial_aprobada.sql`

- **Propósito**: inicialización constitucional del primer `GlobalAdmin`.
  Esta es la única vía de bootstrap para el primer administrador
  global; **no existe comando, endpoint ni cliente OIDC alternativo**.
- **Estado actual**: FALLIDO (NO_APLICADA). Diagnóstico humano confirmó
  que no existen datos de bootstrap. La semilla original requería
  `ID_DOCUMENTO_APROBACION` e `ID_USUARIO_APROBADOR` previos, lo que
  creaba una dependencia circular (no existen usuarios/documentos porque
  la matriz no fue inicializada). Corregida por 021.1.
- **Excepción documentada al orden numérico**: 021 se ejecuta después
  de 002, 007 y 008+008.1 y antes de US1. Su identificador numérico se
  conserva por trazabilidad.
- **Dependencias**: 002, 007 y 008+008.1 VIGENTES. El resto del catálogo
  (001-017) se asume vigente al momento de la ejecución.
- **Insumos formales obligatorios (variables de sustitución)**:
  - `&&SUB_KEYCLOAK` (formato UUID; entregado por OGTI).
  - `&&JEFATURA_AUTORIZANTE` (quien autoriza la asignación).
  - `&&APROBACION_DESPLIEGUE` (identificador del documento de
    aprobación de despliegue de la Jefatura de Modernización).
  - `&&DBA_EJECUTOR` (login o identificador del DBA ejecutor).
  - `&&FECHA_EJECUCION` (formato ISO 8601 `YYYY-MM-DD`).
- **Marcador de ejecución única**: `COMMENT ON TABLE
  MATRIZ_FUNCIONAL_VERSION IS 'PIIP 021: inicializacion del primer
  GlobalAdmin ejecutada (&&FECHA_EJECUCION)'`.
- **Compensación forward-only**: si falla, aborta sin aplicar cambios.
  La semilla no es re-ejecutable; tras el éxito, queda inutilizable.
- **Nota**: esta semilla se conserva como fallback documental. Para la
  ejecución real, usar 021.1 que implementa la excepción constitucional
  al bootstrap sin circularidad.

#### Huella esperada de la semilla (cuando se ejecute exitosamente)

- 1 fila en `MATRIZ_FUNCIONAL_VERSION` con `CODIGO_VERSION='MFV-001'`.
- 1 fila en `MATRIZ_FUNCION` con `CODIGO='ADMINISTRADOR_PIIP'`.
- 1 fila en `MATRIZ_FUNCION_PERFIL_UNIDAD` que vincula la función con
  el rol `GLOBAL_ADMIN` y la unidad `MIDAGRI` /
  `Ministerio de Desarrollo Agrario y Riego`.
- 1 fila en `USUARIO` con `KEYCLOAK_ID='&&SUB_KEYCLOAK'`, `LOGIN_SINTETICO='S'`,
  `LOGIN` y `CORREO` nulos por la huella fundacional.
- 1 fila en `USUARIO_ROL_UNIDAD` con `FECHA_INICIO=SYSTIMESTAMP`,
  `INACTIVA_TEMPORALMENTE='N'`, `REVOCADA_EN=NULL` y
  `ID_COMBINACION_MATRIZ` apuntando a la combinación creada.
- 1 fila en `AUDITORIA_ACCESO` con `ENDPOINT='/internal/seed/021/global-admin'`.
- 1 fila en `AUDITORIA_EVENTO` con `TIPO_EVENTO='INICIALIZACION_GLOBAL_ADMIN'`,
  `ENTIDAD_TIPO='USUARIO_ROL_UNIDAD'`, `ENTIDAD_ID=ID_USR_ROL_UNIDAD` y
  `PAYLOAD_JSON` con los insumos formales (`JEFATURA_AUTORIZANTE`,
  `APROBACION_DESPLIEGUE`, `DBA_EJECUTOR`, `FECHA_EJECUCION`, `RESULTADO='EXITOSA'`).
- 0 filas de `GLOBAL_ADMIN` previas a la ejecución. La validación final
  exige exactamente 1 asignación GlobalAdmin vigente.

---

### 021.1 — `database/seeds/021.1_bootstrap_matriz_fundacional.sql`

- **Propósito**: corrección forward-only que resuelve la dependencia
  circular de 021. Implementa la excepción constitucional al bootstrap
  fundacional del primer `GlobalAdmin` sin requerir documentos ni
  usuarios preexistentes.
- **Causa**: 021 requería `ID_DOCUMENTO_APROBACION` e
  `ID_USUARIO_APROBADOR` previos (NOT NULL / FK en
  `MATRIZ_FUNCION_PERFIL_UNIDAD`), pero en el bootstrap no existen
  usuarios ni documentos porque la matriz no fue inicializada.
- **Dependencias**: 002, 007, 008+008.1, 019 VIGENTES. Unidad MIDAGRI
  (`ID_UNIDAD=1`) debe existir.
- **Insumos formales obligatorios**:
  - `&&SUB_KEYCLOAK` (UUID; entregado por OGTI).
  - `&&JEFATURA_AUTORIZANTE`, `&&APROBACION_DESPLIEGUE`,
    `&&DBA_EJECUTOR`, `&&FECHA_EJECUCION`.
- **Compensación forward-only**: abortar si `ES_BOOTSTRAP` ya existe o
  si ya existe un GlobalAdmin. Los DDL se autocommitan en Oracle; si el
  DML falla, los cambios estructurales persisten.

#### Excepción constitucional: columna ES_BOOTSTRAP

El esquema original de `MATRIZ_FUNCION_PERFIL_UNIDAD` (007) declara
`ID_APROBADOR`, `ID_REGISTRADOR` e `ID_DOCUMENTO_APROBACION` como
`NOT NULL` con FKs a `USUARIO` y `DOCUMENTO`. Esta corrección:

1. Añade `ES_BOOTSTRAP CHAR(1) DEFAULT 'N' NOT NULL`.
2. Elimina los FK `FK_MFPU_APROBADOR`, `FK_MFPU_REGISTRADOR`,
   `FK_MFPU_DOCUMENTO` y el CHECK `CK_MFPU_APROBADOR_DISTINTO_REGISTRADOR`.
3. Permite `NULL` en `ID_APROBADOR`, `ID_REGISTRADOR` e
   `ID_DOCUMENTO_APROBACION`.
4. Crea `CK_MFPU_REGLA_FUNDACION`: `ES_BOOTSTRAP='S'` exige los tres
   campos `NULL`; `ES_BOOTSTRAP='N'` exige los tres campos `NOT NULL`.
5. Crea `CK_MFPU_NO_BOOTSTRAP_REGISTRO`: cuando `ES_BOOTSTRAP='N'`,
   `ID_APROBADOR <> ID_REGISTRADOR`.
6. Crea índice funcional único `UIX_MFPU_ES_BOOTSTRAP` que permite una
   sola fila con `ES_BOOTSTRAP='S'`.

Para `MATRIZ_FUNCIONAL_VERSION`: elimina `FK_MFV_DOCUMENTO` y permite
`NULL` en `ID_DOCUMENTO_APROBACION` (el bootstrap no tiene documento
formal; la aprobación de despliegue se registra en auditoría).

#### DML bootstrap

- `MATRIZ_FUNCIONAL_VERSION`: 1 fila con `CODIGO_VERSION='MFV-001'`,
  `ID_DOCUMENTO_APROBACION=NULL`.
- `MATRIZ_FUNCION`: 1 fila `ADMINISTRADOR_PIIP` (MERGE idempotente).
- `MATRIZ_FUNCION_PERFIL_UNIDAD`: 1 fila con `ES_BOOTSTRAP='S'`,
  `ID_ROL=GlobalAdmin`, `ID_UNIDAD=1`, `ID_APROBADOR=NULL`,
  `ID_REGISTRADOR=NULL`, `ID_DOCUMENTO_APROBACION=NULL`.
- `USUARIO`: 1 fila con `KEYCLOAK_ID='&&SUB_KEYCLOAK'`,
  `LOGIN_SINTETICO='S'`, `LOGIN=NULL`, `CORREO=NULL`.
- `USUARIO_ROL_UNIDAD`: 1 asignación `GlobalAdmin` vigente.
- `AUDITORIA_EVENTO`: 1 fila `INICIALIZACION_GLOBAL_ADMIN` con
  payload completo (jefatura, aprobación, DBA, fecha, sub, resultado).
- Marcador: `COMMENT ON TABLE MATRIZ_FUNCIONAL_VERSION`.

#### Huella esperada

Igual a 021, más la columna `ES_BOOTSTRAP='S'` en la combinación
inicial y `ES_BOOTSTRAP='N'` (default) para combinaciones futuras.

---

### 022 — `database/seeds/022_documento_aprobacion_pei.sql`

- **Propósito**: carga manual del documento de aprobación del PEI para
  que la semilla 020 pueda referenciarlo. Crea el expediente, la
  serie y el documento de aprobación con su huella inicial.
- **Dependencias**: 003+003.1+003.2+005+005.1+006+008+008.1+019+021
  VIGENTES.
- **Compensación forward-only**: detener cargas manuales; conservar
  huellas; nunca eliminar documentos ni usuarios cargados.

#### Huella esperada de la carga manual

- 1 fila en `EXPEDIENTE_INSTITUCIONAL` con
  `MODULO_ORIGEN='ORGANIZACION'` y `REFERENCIA_CASO_USO='APROBACION_PEI'`.
- 1 fila en `DOCUMENTO_SERIE` con `ID_TIPO_DOC=&&ID_TIPO_DOC_PEI`,
  `ID_EXPEDIENTE` apuntando al expediente anterior,
  `TITULO='Aprobacion PEI'`, `CLASIFICACION_PROPUESTA='INTERNO'` y
  `ACTIVA='S'`.
- 1 fila en `DOCUMENTO` con
  `ID_TIPO_DOC=&&ID_TIPO_DOC_PEI`, `ID_DOCUMENTO_SERIE` apuntando a la
  serie anterior, `ID_USUARIO_CARGA=&&ID_USUARIO_PLANEAMIENTO`,
  `ESTADO_AL_CARGAR='PROYECTO_EJECUCION'`, `INMUTABLE='S'`,
  `NUMERO_VERSION=1` y los demás parámetros del documento.
- 1 fila en `AUDITORIA_EVENTO` con
  `TIPO_EVENTO='CARGA_DOCUMENTO_PEI'` y el `ID_DOCUMENTO` resultante.
- 0 filas previas: la carga manual no es re-ejecutable.

#### Variables de sustitución obligatorias

- `&&ID_TIPO_DOC_PEI`, `&&ID_UNIDAD_PLANEAMIENTO`,
  `&&ID_USUARIO_PLANEAMIENTO`, `&&NOMBRE_DOCUMENTO`,
  `&&NOMBRE_ORIGINAL`, `&&MIME_TYPE`, `&&TAMANO_BYTES`,
  `&&HASH_SHA256` (formato SHA-256 64 hex), `&&NOMBRE_STORAGE`.

#### Resultado impreso

`DBMS_OUTPUT.PUT_LINE` imprime `ID_EXPEDIENTE`, `ID_SERIE` e
`ID_DOCUMENTO`. El DBA usa `ID_DOCUMENTO` como
`<<ID_DOCUMENTO_APROBACION_PEI>>` en la semilla 020.

---

### 023 — `database/seeds/023_documento_aprobacion_poi.sql`

- **Propósito**: carga manual del documento de aprobación del POI.
  Estructura análoga a 022.
- **Dependencias**: 003+003.1+003.2+005+005.1+006+008+008.1+019+021
  VIGENTES.
- **Compensación forward-only**: detener cargas manuales; conservar
  huellas; nunca eliminar documentos ni usuarios cargados.

#### Huella esperada de la carga manual

- 1 fila en `EXPEDIENTE_INSTITUCIONAL` con
  `REFERENCIA_CASO_USO='APROBACION_POI'`.
- 1 fila en `DOCUMENTO_SERIE` con `ID_TIPO_DOC=&&ID_TIPO_DOC_POI`.
- 1 fila en `DOCUMENTO` con `ID_USUARIO_CARGA=&&ID_USUARIO_PLANEAMIENTO`.
- 1 fila en `AUDITORIA_EVENTO` con `TIPO_EVENTO='CARGA_DOCUMENTO_POI'`.
- 0 filas previas: la carga manual no es re-ejecutable.

#### Resultado impreso

`DBMS_OUTPUT.PUT_LINE` imprime `ID_DOCUMENTO`. El DBA usa ese
`ID_DOCUMENTO` como `<<ID_DOCUMENTO_APROBACION_POI>>` en la semilla
020.

---

### 024 — `database/seeds/024_usuario_planeamiento.sql`

- **Propósito**: carga manual del usuario de planeamiento y, opcionalmente,
  su primera asignación en `USUARIO_ROL_UNIDAD` con la combinación
  matriz de planeamiento.
- **Dependencias**: 002+007+008+008.1+021 VIGENTES. La unidad
  `MIDAGRI` debe existir y no estar duplicada.
- **Compensación forward-only**: detener cargas manuales; conservar
  huellas; nunca eliminar usuarios ni asignaciones cargadas.

#### Huella esperada de la carga manual

- 1 fila en `USUARIO` con `KEYCLOAK_ID=&&KEYCLOAK_ID` (formato UUID),
  `LOGIN`, `CORREO_INSTITUCIONAL`, `NOMBRE_COMPLETO` y
  `LOGIN_SINTETICO='S'` si `LOGIN` es NULL.
- Opcionalmente 1 fila en `USUARIO_ROL_UNIDAD` con
  `ID_COMBINACION_MATRIZ=&&ID_COMBINACION_PLANEAMIENTO`,
  `FECHA_INICIO=SYSTIMESTAMP`, `INACTIVA_TEMPORALMENTE='N'` y
  `REVOCADA_EN=NULL`. El rol por defecto es `RESPONSABLE` y la
  unidad es `&&ID_UNIDAD_PLANEAMIENTO`.
- 1 fila en `AUDITORIA_EVENTO` con
  `TIPO_EVENTO='CARGA_USUARIO_PLANEAMIENTO'`.
- 0 filas previas: la carga manual no es re-ejecutable.

#### Variables de sustitución obligatorias

- `&&KEYCLOAK_ID` (formato UUID), `&&LOGIN`, `&&CORREO_INSTITUCIONAL`,
  `&&NOMBRE_COMPLETO`, `&&ID_UNIDAD_PLANEAMIENTO`,
  `&&ID_COMBINACION_PLANEAMIENTO` (opcional; 0 o NULL para no crear
  asignación), `&&DBA_EJECUTOR`.

#### Resultado impreso

`DBMS_OUTPUT.PUT_LINE` imprime `ID_USUARIO` y, si aplica, el
`ID_USR_ROL_UNIDAD`. El DBA usa `ID_USUARIO` como
`<<ID_ACTOR_PLANEAMIENTO>>` en la semilla 020.

---

### 018 — Diferido a fase posterior

US9 (prototipos, mediciones y matrices de metas) queda fuera del alcance físico activo. No se
depositó ni se aplicó un incremento 018; por ello no existen objetos físicos, precondiciones ni
dependencias activas asociadas a este incremento. Su diseño deberá someterse a una nueva
especificación, revisión humana y aprobación antes de reactivarse.

---

### 019 — `database/seeds/019_catalogos_canonicos_portafolio.sql`

- **Alcance**: sembrar catálogos canónicos de portafolio y de documentos en sus versiones activas,
  inactivando duplicados previos. Inserta filas en `TIPO_DOCUMENTO` (nuevos tipos documentales),
  en `TRANSICION_PERMITIDA` (entrada legacy opcional), en `MATRIZ_FUNCION` (funciones aprobadas en
  007) y, si aplica, en catálogos auxiliares.
- **Huella de precondiciones**: existencia de `TIPO_DOCUMENTO` y `MATRIZ_FUNCION`; ningún catálogo
  controlado se reasigna.
- **Compensación forward-only**: inactivar semillas no referenciadas; nunca borrar referencias.
- **Gates**: este script se ejecuta solo tras `EXECUTION_CONFIRMED` de 003-017.

---

### 020 — `database/seeds/020_planeamiento_inicial_aprobado.sql`

- **Alcance**: sembrar versiones independientes PEI y POI (`CAT_OBJETIVO_PEI_VERSION`,
  `CAT_OBJETIVO_PEI`, `CAT_ACTIVIDAD_POI_VERSION`, `CAT_ACTIVIDAD_POI`) con los datasets y
  aprobaciones formales independientes.
- **Huella de precondiciones**: existencia de las tablas 005 y 006 y de los documentos de
  aprobación; el `Idempotency-Key` de la operación debe ser único.
- **Compensación forward-only**: inactivar versiones; nunca borrar referencias.
- **Gates**: datasets y aprobaciones PEI y POI; se documentan como `NEEDS CLARIFICATION` hasta que
  el área de planeamiento entregue los documentos formales. Sin insumo no se ejecuta.

---

### 021 — `database/seeds/021_matriz_funcional_inicial_aprobada.sql`

- **Alcance**: crear `MATRIZ_FUNCIONAL_VERSION` inicial, función `ADMINISTRADOR_PIIP`, la
  combinación con `GlobalAdmin`, el usuario fundacional por `sub`, su primera asignación y la
  auditoría mínima. Prevalida la inexistencia histórica de `GlobalAdmin` y aborta sin cambios ante
  cualquier antecedente o reejecución.
- **Huella de precondiciones**: existencia de `MATRIZ_FUNCION_PERFIL_UNIDAD`, `USUARIO`,
  `USUARIO_ROL_UNIDAD`, `ROL`, `UNIDAD_EJECUTORA`, `AUDITORIA_EVENTO`; existencia de la unidad
  `MIDAGRI` (semilla 001) y del documento de aprobación de despliegue (entregado por la Jefatura de
  Modernización).
- **Compensación forward-only**: la semilla es fail-fast y no implementa reversión; aborta sin
  aplicar cambios ante cualquier incompatibilidad. Tras éxito, queda inutilizable.
- **Gates**: dependencias confirmadas de 002, 007 y 008; `sub` del administrador Keycloak entregado
  por OGTI; aprobación de despliegue y datos del DBA registrados.

---

### 022 — Diferido a fase posterior

El backfill legacy previsto históricamente para 022 no es DDL activo y no tiene objetos,
dependencias ni gates de ejecución activos. Su trazabilidad se conserva para una futura migración;
no se depositará ni ejecutará script alguno hasta contar con mapeos legacy aprobados y una nueva
revisión humana.

---

### 023 — Diferido a fase posterior

Los índices operativos previstos históricamente para 023 no son índices activos ni forman parte del
alcance físico actual. Su trazabilidad se conserva para una futura migración; requiere mapeos
aprobados, revisión de carga y nueva revisión humana antes de definir o depositar DDL.

---

### 024 — Diferido a fase posterior

El corte legacy previsto históricamente para 024 no es DDL activo y no tiene objetos, dependencias
ni gates de ejecución activos. Las columnas y referencias legacy permanecen con su estado vigente,
incluido `FK_URU_COMBINACION` en `ENABLE NOVALIDATE`. Su reactivación exige mapeos aprobados y una
nueva revisión humana antes de proponer una migración/corte expresamente aprobada.

## Tabla de secuencias

| Secuencia | Incremento | Módulo | Asociada a |
|---|---|---|---|
| `SEQ_SOLICITUD_IDEMPOTENTE` | 002 | auditoria | `SOLICITUD_IDEMPOTENTE.ID_SOLICITUD` |
| `SEQ_EXPEDIENTE_INSTITUCIONAL` | 003 | documentos | `EXPEDIENTE_INSTITUCIONAL.ID_EXPEDIENTE` |
| `SEQ_DOCUMENTO_SERIE` | 003 | documentos | `DOCUMENTO_SERIE.ID_SERIE` |
| `SEQ_DOCUMENTO_CLASIF_HIST` | 004 | documentos | `DOCUMENTO_CLASIFICACION_HIST.ID_HISTORIAL` |
| `SEQ_DOCUMENTO_PUBLICACION` | 004 | documentos | `DOCUMENTO_PUBLICACION.ID_PUBLICACION` |
| `SEQ_OBJETIVO_PEI_VERSION` | 005 | organizacion | `CAT_OBJETIVO_PEI_VERSION.ID_VERSION` |
| `SEQ_OBJETIVO_PEI` | 005 | organizacion | `CAT_OBJETIVO_PEI.ID_OBJETIVO` |
| `SEQ_ACTIVIDAD_POI_VERSION` | 006 | organizacion | `CAT_ACTIVIDAD_POI_VERSION.ID_VERSION` |
| `SEQ_ACTIVIDAD_POI` | 006 | organizacion | `CAT_ACTIVIDAD_POI.ID_ACTIVIDAD` |
| `SEQ_MATRIZ_VERSION` | 007 | seguridad | `MATRIZ_FUNCIONAL_VERSION.ID_VERSION` |
| `SEQ_MATRIZ_FUNCION` | 007 | seguridad | `MATRIZ_FUNCION.ID_FUNCION` |
| `SEQ_MATRIZ_COMBINACION` | 007 | seguridad | `MATRIZ_FUNCION_PERFIL_UNIDAD.ID_COMBINACION` |
| `SEQ_URU_EVENTO` | 008 | seguridad | `USUARIO_ROL_UNIDAD_EVENTO.ID_EVENTO` |
| `SEQ_SUPLENCIA_FUNCIONAL` | 008 | seguridad | `SUPLENCIA_FUNCIONAL.ID_SUPLENCIA` |
| `SEQ_OPERACION_APROVISIONAMIENTO` | 008 | seguridad | `OPERACION_APROVISIONAMIENTO.ID_OPERACION` |
| `SEQ_INICIATIVA_PROYECTO` | 010 | portafolio | `INICIATIVA_PROYECTO.ID_RELACION` |
| `SEQ_PROYECTO_RESPONSABLE` | 011 | portafolio | `PROYECTO_RESPONSABLE.ID_TITULARIDAD` |
| `SEQ_PARTICIPANTE_PERSONA` | 012 | portafolio | `PARTICIPANTE_PERSONA.ID_PARTICIPANTE` |
| `SEQ_PROY_PART_PERSONA` | 012 | portafolio | `PROYECTO_PARTICIPANTE_PERSONA.ID_PROY_PART_PERSONA` |
| `SEQ_PROY_PART_UNIDAD` | 012 | portafolio | `PROYECTO_PARTICIPANTE_UNIDAD.ID_PROY_PART_UNIDAD` |
| `SEQ_PROY_CAMPO_CLASIF` | 013 | portafolio | `PROYECTO_CAMPO_CLASIFICACION.ID_CLASIFICACION` |
| `SEQ_PROY_CAMPO_CLASIF_HIST` | 013 | portafolio | `PROYECTO_CAMPO_CLASIF_HIST.ID_HISTORIAL` |
| `SEQ_EVALUACION_INICIATIVA` | 014 | portafolio | `EVALUACION_INICIATIVA.ID_EVALUACION` |
| `SEQ_SUBSANACION_INICIATIVA` | 014 | portafolio | `SUBSANACION_INICIATIVA.ID_SUBSANACION` |
| `SEQ_APLICABILIDAD_INICIATIVA` | 014 | portafolio | `APLICABILIDAD_INICIATIVA.ID_APLICABILIDAD` |
| `SEQ_APLICABILIDAD_CRITERIO` | 014 | portafolio | `APLICABILIDAD_CRITERIO.ID_CRITERIO` |
| `SEQ_PLANIFICACION_PROYECTO` | 015 | portafolio | `PLANIFICACION_PROYECTO.ID_PLANIFICACION` |
| `SEQ_CICLO_PROYECTO` | 015 | portafolio | `CICLO_PROYECTO.ID_CICLO` |
| `SEQ_CICLO_EVIDENCIA` | 015 | portafolio | `CICLO_EVIDENCIA.ID_EVIDENCIA` |
| `SEQ_PRODUCTO_PARCIAL` | 015 | portafolio | `PRODUCTO_PARCIAL.ID_PRODUCTO` |
| `SEQ_PRESENTACION_PRODUCTO_FINAL` | 015 | portafolio | `PRESENTACION_PRODUCTO_FINAL.ID_PRESENTACION` |
| `SEQ_VALIDACION_RESULTADO` | 015 | portafolio | `VALIDACION_RESULTADO.ID_VALIDACION` |
| `SEQ_CIERRE_PROYECTO` | 015 | portafolio | `CIERRE_PROYECTO.ID_CIERRE` |
| `SEQ_INCORPORACION_REGISTRO` | 016 | portafolio | `INCORPORACION_REGISTRO.ID_INCORPORACION` |
| `SEQ_INCORPORACION_CAMBIO` | 016 | portafolio | `INCORPORACION_CAMBIO.ID_CAMBIO` |
| `SEQ_INCORPORACION_CONFLICTO` | 016 | portafolio | `INCORPORACION_CONFLICTO.ID_CONFLICTO` |
| `SEQ_REPORTE_INSTITUCIONAL` | 017 | reportes | `REPORTE_INSTITUCIONAL.ID_REPORTE` |
| `SEQ_REPORTE_SNAPSHOT` | 017 | reportes | `REPORTE_SNAPSHOT.ID_SNAPSHOT` |
| `SEQ_REPORTE_ARCHIVO` | 017 | reportes | `REPORTE_ARCHIVO.ID_ARCHIVO` |
| `SEQ_REPORTE_APROBACION` | 017 | reportes | `REPORTE_APROBACION.ID_APROBACION` |
| `SEQ_REPORTE_DESTINATARIO` | 017 | reportes | `REPORTE_DESTINATARIO.ID_DESTINATARIO` |
| `SEQ_REPORTE_REMISION` | 017 | reportes | `REPORTE_REMISION.ID_REMISION` |

Las secuencias son `NOCACHE NOCYCLE` salvo las del baseline dedicadas a auditoría, que conservan
`CACHE 50`.

## Tabla de índices auxiliares relevantes

| Índice | Tabla | Columnas/expresión | Justificación |
|---|---|---|---|
| `IDX_SI_EXPEDICION` | `SOLICITUD_IDEMPOTENTE` | `FECHA_EXPEDICION` | Barridos por fecha |
| `IDX_SI_EXPIRACION` | `SOLICITUD_IDEMPOTENTE` | `FECHA_EXPIRACION` | Expiración técnica |
| `IDX_AA_ROL_EFECTIVO` | `AUDITORIA_ACCESO` | `ID_ROL_EFECTIVO` | Filtro de auditoría efectiva |
| `IDX_AA_UNIDAD_EFECTIVA` | `AUDITORIA_ACCESO` | `ID_UNIDAD_EFECTIVA` | Filtro de auditoría efectiva |
| `IDX_DS_REGISTRO` | `DOCUMENTO_SERIE` | `ID_REGISTRO` | Series por portafolio |
| `IDX_DS_EXPEDIENTE` | `DOCUMENTO_SERIE` | `ID_EXPEDIENTE` | Series por expediente |
| `IDX_DOC_SERIE` | `DOCUMENTO` | `ID_DOCUMENTO_SERIE` | Versión por serie |
| `IDX_URU_ABIERTAS_FUNC` | `USUARIO_ROL_UNIDAD` | expresión condicional sobre terna | Una sola asignación abierta |
| `UX_PR_TITULAR_ACTIVO` | `PROYECTO_RESPONSABLE` | expresión condicional | Un titular activo por proyecto |
| `IDX_PROY_OBJETIVO_PEI` | `PROYECTO` | `OBJETIVO_PEI_ID` | Localización por FK |
| `IDX_PROY_ACTIVIDAD_POI` | `PROYECTO` | `ACTIVIDAD_POI_ID` | Localización por FK |
| `IDX_PROY_COMPONENTE_DIGITAL` | `PROYECTO` | `COMPONENTE_DIGITAL` | Filtro por componente |
| `IDX_OA_UNIDAD_OBJETIVO` | `OPERACION_APROVISIONAMIENTO` | `ID_UNIDAD_OBJETIVO` | Consulta y reintento con revalidación de alcance por unidad |

## Consideraciones sobre commits implícitos

Oracle ejecuta un `COMMIT` antes y después de cada sentencia DDL (`CREATE`, `ALTER`, `DROP`,
`RENAME`, `TRUNCATE`). Por tanto, cada script del alcance físico activo debe:

- Encerrar sus DDL en bloques PL/SQL con `EXECUTE IMMEDIATE` cuando necesiten control transaccional
  o en script SQL directo cuando la operación sea atómica.
- Documentar explícitamente el orden de DDL, identificando dónde ocurre un `COMMIT` implícito y
  qué efecto tiene sobre las FKs diferidas o `ENABLE NOVALIDATE`.
- Evitar la creación de DDL dentro de transacciones de negocio: los scripts DDL no se invocan
  desde la aplicación; la ejecución es siempre manual.
- Garantizar que la última sentencia del script sea `COMMIT` para confirmar la
  sincronización final de secuencias y, cuando aplique, la actualización de metadatos.

## Política de compensación forward-only

Cada script del alcance físico activo documenta su compensación forward-only. Las reglas son:

- **No se renombran, reemplazan ni reejecutan** el baseline o incrementos anteriores.
- **No se eliminan** filas, columnas, constraints, índices, secuencias, ni datos legados.
- **Se inactivan** capacidades, valores y consumidores cuando el modelo así lo exige; las
  modificaciones quedan documentadas en `database/CHANGELOG.md`.
- **Las columnas legacy** (`DOCUMENTO.SCAN_ANTIVIRUS`, `DOCUMENTO.NOMBRE_STORAGE`,
  `PROYECTO.OBJETIVO_PEI`, `PROYECTO.ACTIVIDAD_POI`, `PROYECTO.ADMINISTRACION`,
  `PROYECTO_UNIDAD_ORGANICA`) permanecen como legado con su dato histórico, hasta un corte
  explícito aprobado.

## Anexos

- **A. Reglas de auditoría mínima**: cada tabla del alcance físico activo incluye `CREADO_POR` y `FECHA_CREACION`;
  cuando aplique, `MODIFICADO_POR`, `FECHA_MODIFICACION` y `@Version` (`VERSION NUMBER(10)`).
- **B. Reglas de FK `ENABLE NOVALIDATE`**: las FKs hacia tablas creadas en el mismo script se
  declaran `ENABLE VALIDATE`; las que dependen de scripts posteriores o de semillas pendientes se
  crean `ENABLE NOVALIDATE` para no bloquear la carga inicial.
- **C. Reglas de CHECK y dominios canónicos**: los `CHECK` de clasificación usan los valores
  `PUBLICO`, `INTERNO` y `RESTRINGIDO`; los `CHECK` de estado de prototipo, transición e
  incorporación usan los dominios definidos en `data-model.md` y la especificación.
- **D. Reglas de auditoría inmutable**: los historiales (`USUARIO_ROL_UNIDAD_EVENTO`,
  `INCORPORACION_CAMBIO`, `DOCUMENTO_CLASIFICACION_HIST`, `PROYECTO_CAMPO_CLASIF_HIST`,
  `AUDITORIA_EVENTO`, `AUDITORIA_ACCESO`) no exponen rutas de actualización ni eliminación.
