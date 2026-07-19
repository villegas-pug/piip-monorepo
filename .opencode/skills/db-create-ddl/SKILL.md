---
name: db-create-ddl
description: Crea o modifica estructuras Oracle versionadas de PIIP; úsala solo desde database-specialist con especificación y contrato aprobados.
---

# Crear DDL Oracle PIIP

Lee la constitución, especificación, contrato solicitante, catálogo vigente y CHANGELOG. No
infieras estructuras ni realices cambios destructivos sin autorización humana explícita,
compensación y consumidores identificados.

## Rutas y numeración

Para tablas, columnas, constraints y secuencias usa:

```text
database/ddl/<modulo>/<index>_<table_name>_<action>.sql
```

La primera estructura de un módulo recibe `1`; otra tabla recibe el siguiente entero. Un cambio
posterior de la misma tabla usa `<principal>.<revision>`, por ejemplo
`1.1_proyecto_alter.sql`. Una tabla existente solo en el baseline inicia con su índice principal,
por ejemplo `1_proyecto_alter.sql`. Ordena numéricamente, no lexicográficamente.

Enruta índices independientes, vistas, funciones y packages a `database/indexes`,
`database/views`, `database/functions` y `database/packages`, respectivamente, manteniendo el
mismo módulo y convención de versión. Las semillas se ubican en `database/seeds/<modulo>/`.

Documenta objetivo, dependencias, precondiciones, orden, compensación, impacto y verificación en
la cabecera. Registra el script como `PENDIENTE` en `database/CHANGELOG.md`.

No ejecutes SQL. Devuelve `WAITING_USER_EXECUTION`. Solo tras confirmación expresa del usuario
actualiza `database/database-schema.md` y marca el CHANGELOG como `VIGENTE`.
