# CHANGELOG de base de datos PIIP

Este registro ordena los scripts SQL de PIIP. Un script `PENDIENTE` está depositado en el
repositorio, pero no forma parte del esquema físico vigente hasta que una persona confirme su
ejecución correcta. Los agentes nunca ejecutan estos scripts.

## Convenciones

- DDL de tablas, columnas, constraints y secuencias: `database/ddl/<modulo>/`.
- Procedimientos: `database/procedures/<modulo>/sp_<modulo>_<accion>.sql`.
- Índices, vistas, funciones, packages y semillas: sus directorios constitucionales por módulo.
- Primera estructura de un módulo: `<n>_<tabla>_<accion>.sql`.
- Revisión de una tabla: `<n>.<revision>_<tabla>_<accion>.sql`.
- El orden es numérico por índice principal y revisión, no lexicográfico.

## Objetos vigentes

| Orden | Script | Tipo | Módulo | Estado | Confirmación | Compensación |
|---:|---|---|---|---|---|---|
| 001 | `database/ddl/init/001_baseline_piip.sql` | Baseline: tablas, secuencias, constraints, índices y semillas | transversal | VIGENTE | Baseline documentado en el repositorio | No aplicable; script declarado de ejecución única |

## Objetos pendientes

No hay scripts pendientes.

## Protocolo de actualización

1. `database-specialist` agrega el script con estado `PENDIENTE` y su compensación.
2. El primario solicita la ejecución manual al usuario.
3. Tras la confirmación expresa de éxito, el mismo especialista actualiza este registro a
   `VIGENTE` y sincroniza `database/database-schema.md`.
4. Ante error, conserva `PENDIENTE` o registra `FALLIDO`; nunca actualiza el catálogo vigente.
