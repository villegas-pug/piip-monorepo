---
name: db-validate-catalog
description: Valida estáticamente scripts SQL, CHANGELOG y catálogo Oracle PIIP; úsala solo desde database-specialist sin conectarse a una base de datos.
---

# Validar catálogo Oracle

Compara nombres, tipos, dependencias, versiones y estados entre scripts versionados,
`database/CHANGELOG.md` y `database/database-schema.md`. Verifica rutas, numeración, cabeceras,
compensación y que el catálogo solo incluya objetos `VIGENTE` confirmados por el usuario.

No uses conexión Oracle ni ejecutes scripts. Reporta inconsistencias con archivo y línea, y no
modifiques el catálogo para ocultar una discrepancia de ejecución.
