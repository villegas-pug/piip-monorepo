# Tareas diferidas: migración y corte legacy

**Estado**: Diferidas a una fase posterior por decisión aprobada el 2026-07-22.

Estas tareas se conservan sin checkboxes activos. No forman parte de `/speckit.implement` en la
Fase 1 actual y no crean scripts ni cambios pendientes en `database/CHANGELOG.md`.

| ID | Especialista | Alcance diferido |
|---|---|---|
| T112 | database-specialist | Backfill forward-only de referencias legacy e incremento 022. |
| T113 | database-specialist | Revisión y creación de índices operativos mediante incremento 023. |
| T114 | database-specialist | Constraints finales de corte legacy mediante incremento 024. |

La reactivación exige mapeos legacy aprobados, revisión del índice ya existente sobre
`USUARIO_ROL_UNIDAD.ID_COMBINACION_MATRIZ`, diseño físico actualizado y nueva revisión humana DB.
