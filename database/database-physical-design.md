# Diseño físico Oracle incremental 002-024

## Estado

**Estado de aprobación**: PENDIENTE

Este documento es propiedad de `database-specialist`. Debe completarse y recibir aprobación humana
de base de datos antes de crear cualquier script 002-024. La aprobación se registra en
`database/physical-design-approval.md` para una revisión identificable de este archivo.

## Decisiones aprobadas

- Oracle 19c o superior y esquema `KALLPA_PIIP`.
- `DOCUMENTO_SERIE` es la raíz lógica y cada fila de `DOCUMENTO` representa una versión.
- `PROYECTO.ADMINISTRACION` se conserva como columna legacy nullable sin reglas nuevas.
- El backfill de `PROYECTO.DESCRIPCION` se bloquea hasta recibir un mapeo aprobado.
- `REPORTE_SNAPSHOT` conserva JSON canónico en CLOB, versión de esquema y SHA-256.
- `SOLICITUD_IDEMPOTENTE` usa una ventana inicial configurable de siete días sin eliminar auditoría.
- Los scripts son de ejecución única, fail-fast y con compensación forward-only.

## Prevalidación de versión

No se crea una tabla de versión ni se consulta `database/CHANGELOG.md` desde Oracle. Cada incremento
declara una huella de precondiciones sobre `USER_TABLES`, `USER_TAB_COLUMNS`, `USER_CONSTRAINTS`,
`USER_CONS_COLUMNS`, `USER_INDEXES`, `USER_IND_COLUMNS`, `USER_IND_EXPRESSIONS` y `USER_SEQUENCES`
correspondiente al baseline y a todos sus predecesores. El script aborta antes del primer DDL cuando
falta un objeto esperado, existe uno futuro o una definición no coincide en nombre, columnas, orden,
tipo, longitud, nulabilidad, expresión o unicidad.

## Diccionario requerido

Para cada objeto 002-024 se documentará antes del DDL:

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
- 022: mapeos legacy aprobados, no disponibles.
- 023-024: dependen de 020-022 ejecutados y confirmados.

Ningún valor faltante se infiere. `database/database-schema.md` permanece como catálogo aplicado y no
se actualiza mientras esta aprobación o la ejecución humana del incremento estén pendientes.
