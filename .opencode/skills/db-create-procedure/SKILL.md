---
name: db-create-procedure
description: Crea procedimientos almacenados Oracle versionados para PIIP; úsala solo desde database-specialist a partir de un contrato SP aprobado.
---

# Crear procedimiento Oracle PIIP

## Precondiciones

Lee la constitución, especificación aprobada, handoff backend, `database/database-schema.md` y
`database/CHANGELOG.md`. El catálogo es la única fuente de nombres, tipos, longitudes, precisión,
escala, nulabilidad y defaults del esquema físico vigente. No inventes metadatos.

Rechaza contratos sin parámetros, salidas, errores, tablas, autoridad de reglas, auditoría,
transacción, dependencias y prueba definida. Las reglas viven en Java o PL/SQL, nunca en ambos.

## Archivo y contenido

Deposita el script en:

```text
database/procedures/<modulo>/sp_<modulo>_<accion>.sql
```

El nombre Oracle usa mayúsculas: `SP_<MODULO>_<ACCION>`. La acción debe expresar la
responsabilidad del CRUD, por ejemplo `SP_PORTAFOLIO_CREAR_PROYECTO`.

La cabecera del script documenta propósito, parámetros, salidas/cursor, errores, dependencias,
autoridad de reglas, auditoría, comportamiento transaccional, orden de ejecución y compensación.
No emitas `COMMIT` o `ROLLBACK` autónomos salvo una decisión aprobada. Agrega el script a
`database/CHANGELOG.md` con estado `PENDIENTE`.

## Gate de ejecución

No ejecutes SQL. Devuelve `WAITING_USER_EXECUTION` y el `task_id` al primario. Tras una
confirmación expresa de ejecución correcta, actualiza `database/database-schema.md` y el
CHANGELOG a `VIGENTE`; ante fallo conserva el catálogo sin cambio y devuelve
`EXECUTION_FAILED`.
