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
| 002 | `database/ddl/auditoria/002_auditoria_idempotencia.sql` | Tablas/índices | auditoria | VIGENTE | 2026-07-22 — ejecución manual confirmada | Dejar de escribir campos nuevos; nunca eliminar auditoría o claves ya consumidas |
| 003 | `database/ddl/documentos/003_expediente_serie_version.sql` | Tablas/índices | documentos | VIGENTE | 2026-07-22 — ejecución manual confirmada | Adaptar tipo documental y BLOB Oracle por contexto e inactivar `SCAN_ANTIVIRUS` y `NOMBRE_STORAGE` sin borrar datos legacy; detener nuevas cargas y conservar expedientes, series y versiones |
| 003.1 | `database/ddl/documentos/003.1_tipo_documento_contexto_nullable.sql` | Corrección forward-only | documentos | VIGENTE | 2026-07-22 — ejecución manual confirmada | No revertir nulabilidad mientras existan tipos institucionales; detener altas de tipos institucionales y conservar catálogos |
| 003.2 | `database/ddl/documentos/003.2_documento_propietario_institucional.sql` | Corrección forward-only | documentos | VIGENTE | 2026-07-22 — ejecución manual confirmada | No volver obligatorios los campos mientras existan series institucionales; detener altas institucionales y conservar documentos legacy |
| 004 | `database/ddl/documentos/004_documento_publicacion.sql` | Tablas/índices | documentos | VIGENTE | 2026-07-22 — ejecución manual confirmada | Detener publicaciones y reclasificaciones; conservar confirmaciones y auditoría. |
| 005 | `database/ddl/organizacion/005_objetivo_pei_versionado.sql` | Tablas/índices + corrección forward-only 005.1 | organizacion | VIGENTE | 2026-07-22 — huella parcial validada y finalizada mediante ejecución manual confirmada de 005.1 | Inactivar versiones no usadas; preservar referencias históricas. |
| 005.1 | `database/ddl/organizacion/005.1_objetivo_pei_versionado_indice.sql` | Corrección forward-only | organizacion | VIGENTE | 2026-07-22 — ejecución manual confirmada | No reejecutar 005; detener altas PEI y conservar la huella parcial hasta revisión DBA si falla; no hay rollback físico automático del marcador. |
| 006 | `database/ddl/organizacion/006_actividad_poi_versionada.sql` | Tablas/índices | organizacion | VIGENTE | 2026-07-22 — ejecución manual confirmada | Inactivar versiones no usadas; preservar referencias históricas; ciclo POI independiente del PEI. |
| 007 | `database/ddl/seguridad/007_matriz_funcional_versionada.sql` | Tablas/índices | seguridad | VIGENTE | 2026-07-22 — ejecución manual confirmada | Detener nuevas versiones; conservar combinaciones históricas |
| 008.1 | `database/ddl/seguridad/008.1_secuencias_vigencia.sql` | Corrección forward-only | seguridad | VIGENTE | 2026-07-22 — ejecución manual confirmada; completa la corrección forward-only del incremento 008 | No eliminar secuencias; detener nuevas operaciones de asignación/suplencia y conservar historial |
| 009 | `database/ddl/portafolio/009_proyecto_campos_oficiales.sql` | Modificaciones a `PROYECTO` | portafolio | VIGENTE | 2026-07-22 — ejecución manual confirmada | Mantener columnas legacy (`ADMINISTRACION`, `OBJETIVO_PEI`, `ACTIVIDAD_POI`); no restaurar `CHECK` si hay estados nuevos. |
| 010 | `database/ddl/portafolio/010_iniciativa_proyecto_relacion.sql` | Tablas/índices | portafolio | VIGENTE | 2026-07-22 — ejecución manual confirmada; se eliminaron `IDX_IP_INICIATIVA` e `IDX_IP_PROYECTO` por redundantes y sus UK aportan los índices únicos canónicos | Detener nuevas relaciones; conservar vínculos confirmados. |
| 011 | `database/ddl/portafolio/011_proyecto_unidades_responsables.sql` | Tablas/índices | portafolio | VIGENTE | 2026-07-22 — ejecución manual confirmada; precondición de objetos futuros limitada a sucesores estrictos | Mantener referencia legacy `PROYECTO_UNIDAD_ORGANICA` hasta una migración/corte expresamente aprobada. |
| 012 | `database/ddl/portafolio/012_responsables_participantes.sql` | Tablas, secuencias, constraints e índices | portafolio | VIGENTE | 2026-07-23 — reconciliación histórica confirmada por el usuario tras ejecución manual exitosa del verificador; no afirma una nueva ejecución del DDL 012 | Forward-only: detener altas si se requiere corrección; conservar participantes y participaciones; no reejecutar 012 ni usar el incremento 026 para alterar su huella. |
| 013 | `database/ddl/portafolio/013_clasificacion_campos.sql` | Tablas/índices | portafolio | VIGENTE | 2026-07-22 — ejecución manual confirmada; precondición de objetos futuros limitada a sucesores estrictos | Volver datos nuevos no publicables; nunca ampliar acceso. Debe confirmarse antes de formularios, validaciones, consultas o reportes dependientes. |
| 014 | `database/ddl/portafolio/014_evaluacion_transiciones.sql` | Tablas/índices + corrección forward-only 014.1 | portafolio | VIGENTE (vía 014.1) | 2026-07-22 — ejecución manual confirmada de 014.1 que completa la huella de 014 | Detener comandos; no revertir estados confirmados; la regla determinista de subsanación queda fijada por 014.1. |
| 014.1 | `database/ddl/portafolio/014.1_subsanacion_iniciativa_plazo.sql` | Corrección forward-only | portafolio | VIGENTE | 2026-07-22 — ejecución manual confirmada (sexta corrección forward-only de la familia 014/014.1) | No reejecutar 014; detener altas de subsanación/evaluación/aplicabilidad y conservar la huella; si la corrección ya fue aplicada, abortar con ORA-20199. |
| 015 | `database/ddl/portafolio/015_ciclos_resultados_cierre.sql` | Tablas/índices | portafolio | VIGENTE | 2026-07-22 — ejecución manual confirmada; revisión preventiva de CHECKs no deterministas documentada en la nota del incremento | Detener cierres/ciclos; conservar versiones cerradas. |
| 016 | `database/ddl/portafolio/016_incorporacion_individual.sql` | Tablas/índices | portafolio | VIGENTE | 2026-07-22 — ejecución manual confirmada; revisión preventiva de CHECKs no deterministas documentada en la nota del incremento | Mantener expedientes `PENDIENTE`; no borrar evidencia. |
| 017 | `database/ddl/reportes/017_reporte_expediente_remision.sql` | Tablas/índices | reportes | VIGENTE | 2026-07-22 — ejecución manual confirmada; revisión preventiva de CHECKs no deterministas documentada en la nota del incremento | Detener generación/remisión; conservar expedientes. |
| 019 | `database/seeds/019_catalogos_canonicos_portafolio.sql` | Semilla | transversal | VIGENTE | 2026-07-22 — ejecución manual confirmada (aplicación efectiva de roles, tipos documentales y transiciones legacy). Tras varios intentos, se aplicaron las correcciones: conteo de tablas (49→52→55 con 012), validación individual detallada, precondición de duplicados con tres casos (no aplicada, parcial, ya aplicada), mensaje ORA-20193 con nombre y contexto del huérfano, corrección del error de sintaxis PLS-00364 en el diagnóstico, unificación de los nombres de roles canónicos a la grafía CamelCase vigente, reemplazo de `CODIGO` por `NOMBRE` en las referencias a `TIPO_DOCUMENTO`, renombre del cursor `h` a `rec_td_huerfano`, reemplazo de `SEQ_ROL.NEXTVAL` por `ID_ROL` literal del origen, corrección del MERGE de TIPO_DOCUMENTO (subquery sin paréntesis extra, valores `ESTADO_ASOCIADO` dentro del dominio canónico del baseline `CK_TD_ESTADO`, e inclusión de `DESCRIPCION` en el `INSERT` con el orden real de columnas), corrección de ORA-12839 con `COMMIT` intermedio y hint `/*+ NO_PARALLEL */` en el `UPDATE` de `TRANSICION_PERMITIDA`, y corrección de ORA-12838 con `COMMIT` intermedio entre el último `MERGE` de `UnidadAdmin` y el bloque PL/SQL de validación final (misma corrección aplicada a 020 entre su último `MERGE` y su validación, y a 021 antes de su validación) | Inactivar semillas no referenciadas; nunca borrar referencias. La semilla 019 no es re-ejecutable; aborta con ORA-20192 si se intenta de nuevo. |
| 021 | `database/seeds/021_matriz_funcional_inicial_aprobada.sql` | Semilla | seguridad | VIGENTE (vía 021.1) | 2026-07-22 — ejecución manual confirmada de 021.1 que completa la huella de 021. La semilla original 021 tenía dependencia circular (requiere documento/aprobador previos); la corrección 021.1 implementa la excepción constitucional `ES_BOOTSTRAP` | Conservar como fallback documental. La huella fundacional fue creada por 021.1. |
| 021.1 | `database/seeds/021.1_bootstrap_matriz_fundacional.sql` | Corrección forward-only | seguridad | VIGENTE | 2026-07-22 — ejecución manual confirmada. Añade columna `ES_BOOTSTRAP` a `MATRIZ_FUNCION_PERFIL_UNIDAD`, permite NULL en aprobador/registrador/documento con constraint exclusivo, crea la huella fundacional (versión matriz, función ADMINISTRADOR_PIIP, combinación bootstrap, usuario GlobalAdmin por sub OGTI, asignación y auditoría) | No borrar datos. Si el DML falla, los cambios DDL persisten (autocommit); verificar estado antes de reintentar. |
| 020 | `database/seeds/020_planeamiento_inicial_aprobado.sql` | Semilla | organizacion | VIGENTE | 2026-07-22 — ejecución manual confirmada. Crea versiones iniciales PEI y POI con sus items de ejemplo, usando placeholders sustituidos por el operador | Inactivar versiones; nunca borrar referencias. Datasets y aprobaciones PEI/POI se documentan como `NEEDS CLARIFICATION` hasta que el área de planeamiento entregue los documentos formales. |
| 022 | `database/seeds/022_documento_aprobacion_pei.sql` | Carga manual | documentos | VIGENTE | 2026-07-22 — ejecución manual confirmada. Crea expediente, serie y documento de aprobación PEI con placeholders literales directos | Detener cargas manuales; conservar huellas; nunca eliminar documentos ni usuarios cargados. |
| 023 | `database/seeds/023_documento_aprobacion_poi.sql` | Carga manual | documentos | VIGENTE | 2026-07-22 — ejecución manual confirmada. Crea expediente, serie y documento de aprobación POI con placeholders literales directos | Detener cargas manuales; conservar huellas; nunca eliminar documentos ni usuarios cargados. |
| 024 | `database/seeds/024_usuario_planeamiento.sql` | Carga manual | seguridad | VIGENTE | 2026-07-22 — ejecución manual confirmada. Crea usuario de planeamiento con placeholder literal directo | Detener cargas manuales; conservar huellas; nunca eliminar documentos ni usuarios cargados. |
| 025 | `database/ddl/portafolio/025_ciclo_presentacion_evidencia_version.sql` | Tablas, secuencias, constraints, índices y triggers append-only | portafolio | VIGENTE | 2026-07-23 — ejecución manual del DDL y de `database/tests/025_ciclo_presentacion_evidencia_version_test.sql` confirmada por el usuario | Forward-only: detener altas T072/T073, preservar historia/evidencias y depositar corrección versionada; no eliminar objetos ni datos aplicados. |
| 026 | `database/ddl/portafolio/026_incorporacion_registro_observacion_version.sql` | Corrección forward-only: columnas `OBSERVACION` y `VERSION` en `INCORPORACION_REGISTRO` | portafolio | VIGENTE | 2026-07-23 — ejecución manual del DDL y de `database/tests/026_incorporacion_registro_observacion_version_test.sql` confirmada por el usuario; salida: `T026 OK: OBSERVACION y VERSION=0 verificadas bajo SAVEPOINT` | Detener altas y actualizaciones de incorporación, conservar filas y versiones materializadas, inventariar la huella con el DBA y depositar corrección versionada; no eliminar columnas ni ejecutar rollback físico. |
| 027 | `database/ddl/seguridad/027_operacion_aprovisionamiento_unidad_objetivo.sql` | Corrección forward-only: columna `ID_UNIDAD_OBJETIVO`, FK e índice en `OPERACION_APROVISIONAMIENTO` | seguridad | VIGENTE | 2026-07-23 — ejecución manual del DDL y de `database/tests/027_operacion_aprovisionamiento_unidad_objetivo_test.sql` confirmada por el usuario; salida: `T027 OK: ID_UNIDAD_OBJETIVO conservada bajo SAVEPOINT` | Detener altas/reintentos dependientes, conservar la columna/FK/índice y filas históricas sin unidad inventada; inventariar con DBA y depositar corrección versionada. |

## Objetos pendientes

| Orden | Script | Tipo | Módulo | Estado | Confirmación | Compensación |
|---:|---|---|---|---|---|---|
| 028 | `database/dml/seed/002_seed_globaladmin.sql` | Actualización DML: vincular usuario GlobalAdmin bootstrap a Keycloak real | seguridad | VIGENTE | 2026-07-24 — ejecución manual confirmada. Salida: "002 completada exitosamente. Usuario: Rovi Dev, Keycloak sub: ed3742bc-f2c2-4884-ae09-07e3f9ab98fc, Rol: GlobalAdmin, Unidad: MIDAGRI, Matriz: MFV-001, Funcion: ADMIN" | Solo UPDATE idempotente sobre usuario bootstrap de 021.1. No crea registros nuevos. Re-ejecutable si falla.

## Objetos diferidos por la enmienda 5.0.0

- `018-021` (US9, semillas iniciales, corte legacy): no son DDL activo, no
  tienen objetos físicos, dependencias ni gates de ejecución; quedan fuera
  del alcance físico de la Fase 1 por la enmienda 5.0.0. 021 fue completado
  por 021.1 (ya VIGENTE).

## Objetos fallidos

| Orden | Script | Tipo | Módulo | Estado | Confirmación | Compensación |
|---:|---|---|---|---|---|---|
| 008 | `database/ddl/seguridad/008_usuario_rol_unidad_vigencia.sql` | Tablas/índices | seguridad | FALLIDO | 2026-07-22 — ejecución manual fallida antes de la validación final: ORA-20040 (secuencias de vigencia ausentes; DDL parcial confirmado) | Detener nuevas asignaciones/suplencias y conservar historial |

## NEEDS CLARIFICATION — reconciliación estática T110, T111, T115 y T116

- **019 vs. catálogo vigente:** `database/seeds/019_catalogos_canonicos_portafolio.sql`
  declara 55 tablas e incluye las tres tablas del incremento 012; el catálogo vigente declara 51
  tablas y `012` diferido. Aunque 019 figura `VIGENTE`, se requiere evidencia DBA de la huella
  aplicada o una decisión aprobada sobre la autoridad del conteo antes de modificar scripts o
  catálogo. No reejecutar 019.
- **020 sin datasets formales:** el artefacto versionado contiene valores concretos, IDs y
  elementos de ejemplo, en contradicción con T111. Se requieren los datasets PEI/POI y las
  aprobaciones formales independientes, además de autorización para una corrección forward-only
  si corresponde. No reejecutar 020 ni alterar datos aplicados.
- **Pruebas 019/020:** ambos archivos de prueba invocan `@@database/seeds/...` dentro de bloques
  PL/SQL; 019, además, rechaza expresamente una segunda ejecución. Se requiere aprobar el
  contrato de pruebas post-ejecución antes de sustituirlos por verificaciones de solo lectura.
- **Historial de estados:** este CHANGELOG conserva el estado final y la confirmación, pero no
  una transición auditable `PENDIENTE → estado final` para cada incremento vigente. Se requiere
  definir la fuente de evidencia histórica y el formato de trazabilidad sin inferir ejecuciones.

## Incrementos bloqueados por dependencias pendientes

No hay scripts activos del alcance físico bloqueados por dependencias
pendientes dentro de la Fase 1; los incrementos diferidos 012 y 018 no
forman parte del alcance físico actual.

## Protocolo de actualización

1. `database-specialist` agrega el script con estado `PENDIENTE` y su compensación.
2. El primario solicita la ejecución manual al usuario.
3. Tras la confirmación expresa de éxito, el mismo especialista actualiza este registro a
   `VIGENTE` y sincroniza `database/database-schema.md`.
4. Ante error, conserva `PENDIENTE` o registra `FALLIDO`; nunca actualiza el catálogo vigente.
