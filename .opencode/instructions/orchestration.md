# Orquestación de especialistas PIIP

## Principio de mediación

Los agentes primarios `PLAN` y `BUILD` son los únicos orquestadores. Los especialistas no se
comunican directamente entre sí ni crean subagentes. El primario conserva el `task_id` de cada
especialista, transmite los resultados y reanuda la misma sesión cuando sea necesario.

`PLAN` solicita solo análisis y debe instruir explícitamente que no se edite. `BUILD` puede
solicitar implementación, pero cada edición requiere aprobación del usuario mediante `edit: ask`.

## Selección del especialista

| Cambio principal | Especialista |
|---|---|
| Angular, accesibilidad, autenticación de cliente o UI | `frontend-specialist` |
| Java, Spring, API, JPA, seguridad backend o pruebas backend | `backend-specialist` |
| SQL, DDL, procedimientos, catálogo de esquema o CHANGELOG | `database-specialist` |

No delegues tareas que escriban el mismo archivo en paralelo. Si una tarea cruza dominios,
separa los ownerships por ruta y coordina sus dependencias mediante el contrato siguiente.

## Contrato de handoff

Todo resultado de especialista que requiera continuación debe incluir este bloque YAML:

```yaml
request_id: <identificador legible>
mode: PLAN | BUILD
status: ANALYSIS_COMPLETE | BLOCKED_DATABASE | ARTIFACT_READY | WAITING_USER_EXECUTION | EXECUTION_CONFIRMED | EXECUTION_FAILED | IMPLEMENTATION_COMPLETE
source_spec: <ruta a la especificación aprobada>
module: <módulo constitucional o subcapacidad aprobada>
specialist: <nombre del agente>
skill: <nombre de la SKILL>
persistence_strategy: JPA | SP | NOT_APPLICABLE
operations: [CREATE, READ, UPDATE, DELETE]
contract: <ruta o resumen del contrato afectado>
files: []
dependencies: []
blockers: []
verification_pending: []
```

No uses archivos temporales como buzón. El resultado vuelve al primario mediante `task`; el
primario debe incluir el bloque relevante al reanudar otro `task_id`.

## Gate para procedimientos y DDL

1. `backend-specialist` detecta estrategia `SP` o necesidad de estructura nueva y devuelve
   `BLOCKED_DATABASE` con el contrato completo.
2. El primario delega en `database-specialist`.
3. `database-specialist` crea únicamente el script versionado y el registro pendiente en
   `database/CHANGELOG.md`; devuelve `WAITING_USER_EXECUTION`.
4. El primario solicita al usuario que ejecute manualmente el script. Ningún agente ejecuta SQL.
5. Tras confirmación expresa del usuario, el primario reanuda el mismo `task_id` de database.
6. `database-specialist` actualiza `database/database-schema.md` y devuelve
   `EXECUTION_CONFIRMED`. Si la ejecución falló, devuelve `EXECUTION_FAILED` y no actualiza el
   catálogo.
7. El primario reanuda el `task_id` de backend para completar el acceso Java.

Las reglas funcionales pertenecen solo a Java o al procedimiento, según la especificación
aprobada. No se duplican.
