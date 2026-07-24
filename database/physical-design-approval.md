# Aprobación histórica del diseño físico 002-027

**Estado**: VIGENTE — 027 ejecutado manualmente y confirmado por el usuario.

La revisión humana DB debe registrar en este archivo, antes de habilitar el DDL:

- identificador de revisión o commit de `database/database-physical-design.md`;
- nombre o identificador institucional del aprobador;
- fecha y hora de aprobación;
- alcance exacto de incrementos aprobados;
- observaciones o restricciones;
- resultado `APROBADO` o `RECHAZADO`.

Mientras el estado sea `PENDIENTE` o `RECHAZADO`, los scripts 002-024 permanecen bloqueados.

## Identificador de revisión vigente

- **Archivo revisado**: `database/database-physical-design.md`
- **SHA-256 (identificador de revisión)**: `1ecf268110acd115fa900a782f30257bfe2df9bdefb95031193a897d27001890`
- **Tamaño**: actualizado tras correcciones forward-only 005.1, 014.1, 019, 021.1, semillas 019-024 y la confirmación de reconciliación histórica 012
- **Alcance del documento**: diccionario físico de los incrementos 002-024. Detalle completo para
   002-018 (propósito, dependencia, compensación, columnas con tipo/longitud/nulabilidad/default,
  secuencias, PK/UK/FK/CHECK, índices auxiliares, huella de precondiciones, consultas de
  incompatibilidad, orden de creación con commits implícitos, pruebas SQL positiva y negativa).
  Para 019-024 se documenta alcance, huella de precondiciones, compensación forward-only y gates de
  insumos.

### Nota de revisión

- Hash registrado al depósito inicial: `2de4e68d75e0520f29ff5da84d5daf314b482b6a7056f4d6ebbae1249f8171fa`
- Hash actual verificado al momento de la re-aprobación:
  `c24941bdc2ff4c310f02286bf9d0066918c6105d86845cc98f60aa373ed9ebf9`
- Resultado: el archivo `database-physical-design.md` fue modificado por las correcciones
  forward-only 005.1, 014.1, 019, 021.1 y por la incorporación de semillas 019-024. El
  identificador de revisión se actualiza a la nueva huella.

## Datos del aprobador

- **Aprobador**: `revisor humano DB`
- **Fecha y hora de aprobación**: `2026-07-22 00:00 UTC-5`
- **Alcance aprobado**: `incrementos 002-024`
- **Observaciones o restricciones**: `Sin observaciones adicionales`
- **Resultado**: `APROBADO`

## Enmienda de re-aprobación por correcciones forward-only

- **Fecha**: `2026-07-22`
- **Motivo**: Las correcciones forward-only 005.1, 014.1, 019 y 021.1 modificaron
  `database/database-physical-design.md` para registrar la huella de cada corrección, el
  CHECK determinista de `CK_SI_PLAZO`, la excepción bootstrap del primer `GlobalAdmin`
  (columna `ES_BOOTSTRAP`), y las semillas 019-024.
- **Hash anterior**: `2de4e68d75e0520f29ff5da84d5daf314b482b6a7056f4d6ebbae1249f8171fa`
- **Hash nuevo**: `c24941bdc2ff4c310f02286bf9d0066918c6105d86845cc98f60aa373ed9ebf9`
- **Alcance físico actual**: `incrementos 002-017, 019-021, 021.1, 022-024`; 018 diferido.
- **Estado de objetos vigentes**: sin cambios aprobatorios adicionales; los incrementos
  002-017, 019, 021.1, 022-024 permanecen vigentes.

## Enmienda de alcance posterior

- **Origen**: aprobación expresa del usuario en modo BUILD.
- **Enmienda**: US9 (prototipos, mediciones y matrices de metas), previsto como incremento 018, y
  los incrementos 022 (backfill legacy), 023 (índices operativos) y 024 (corte legacy) quedan
  diferidos a una fase posterior por confirmación expresa del usuario en modo BUILD.
- **Alcance físico actual**: `incrementos 002-017 y 019-021`; 018 y 022-024 están diferidos.
- **Trazabilidad histórica**: el hash `2de4e68d75e0520f29ff5da84d5daf314b482b6a7056f4d6ebbae1249f8171fa`
  y la aprobación humana previa se conservan como registro de la revisión original; no constituyen
  una nueva aprobación del diseño enmendado.
- **Estado de objetos vigentes**: sin cambios. Esta enmienda no modifica las aprobaciones ni la
  ejecución de los incrementos vigentes.
- **Estado de 018, 022, 023 y 024**: no se depositan ni aplican como alcance activo. Su
  reactivación requiere mapeos aprobados y nueva revisión humana.

## Reconciliación documental 012 confirmada

- **Origen**: confirmaciones expresas del usuario en modo BUILD.
- **Alcance**: se reactiva la capacidad 012 solo para reconciliar la huella ya existente de
  `PARTICIPANTE_PERSONA`, `PROYECTO_PARTICIPANTE_PERSONA`,
  `PROYECTO_PARTICIPANTE_UNIDAD` y sus tres secuencias. El dominio confirmado de
  `CLASIFICACION` es `PUBLICO`, `INTERNO`, `RESTRINGIDO`.
- **Evidencia confirmada**: el usuario confirmó que la reejecución manual del verificador de
  solo lectura `database/tests/012_responsables_participantes_reconciliation_test.sql` terminó
  correctamente; tablas, secuencias, constraints e índices coinciden con la huella 012. Los
  defaults fueron revisados por DBA conforme a la nota `NO_VERIFICADO_LONG`.
- **Estado**: CONFIRMADA. 012 queda `VIGENTE` por reconciliación histórica, sin afirmar una nueva
  ejecución del DDL. No autoriza reejecutar 012 ni usar el incremento 026 para alterar su huella.
- **Identificador de revisión confirmado**: `1ecf268110acd115fa900a782f30257bfe2df9bdefb95031193a897d27001890`.

## Trazabilidad

Cualquier modificación posterior que incorpore o altere diseño físico activo en
`database/database-physical-design.md` debe:

1. Recalcular el SHA-256 del archivo y actualizar el identificador de revisión.
2. Mantener el estado en `PENDIENTE` mientras no exista confirmación humana explícita.
3. Documentar la versión, el motivo del cambio y, en su caso, revalidar la aprobación.

La enmienda de alcance documentada arriba conserva el hash como evidencia histórica de la revisión
original y no habilita los diseños diferidos de 018 y 022-024.

## Revisión pendiente — incremento 027 (T091)

- **Fecha de depósito**: 2026-07-23.
- **Archivo a revisar**: `database/database-physical-design.md`.
- **SHA-256 (identificador de revisión pendiente)**:
  `3ef66bd43533a8758676ce884fcd184564ec1f5b0bd3694a7608625b78102dfe`.
- **Alcance propuesto**: `database/ddl/seguridad/027_operacion_aprovisionamiento_unidad_objetivo.sql` y `database/tests/027_operacion_aprovisionamiento_unidad_objetivo_test.sql`; agrega `ID_UNIDAD_OBJETIVO NUMBER(10)` nullable, `FK_OA_UNIDAD_OBJETIVO` e `IDX_OA_UNIDAD_OBJETIVO` a `OPERACION_APROVISIONAMIENTO`.
- **Compatibilidad**: no se realiza backfill; operaciones históricas permanecen sin unidad objetivo. La obligatoriedad para altas ordinarias corresponde al contrato backend, pues no hay discriminador físico aprobado para distinguirlas de filas históricas.

## Ejecución confirmada — incremento 027 (T091)

- **Fecha de confirmación**: 2026-07-23.
- **Archivo revisado**: `database/database-physical-design.md`.
- **SHA-256 (identificador de revisión al depósito)**:
  `c30a947b7dbf5457cf428e94ee6a34d7833697ca5deb31bbd2871daf80f889a3`.
- **Alcance confirmado**: `database/ddl/seguridad/027_operacion_aprovisionamiento_unidad_objetivo.sql`,
  que agrega `ID_UNIDAD_OBJETIVO NUMBER(10)` nullable, `FK_OA_UNIDAD_OBJETIVO` hacia
  `UNIDAD_EJECUTORA(ID_UNIDAD)` e `IDX_OA_UNIDAD_OBJETIVO` a `OPERACION_APROVISIONAMIENTO`; la
  prueba `database/tests/027_operacion_aprovisionamiento_unidad_objetivo_test.sql` se ejecutó
  bajo `SAVEPOINT` con `ROLLBACK TO`.
- **Motivo**: T091 — conservar la unidad objetivo de cada operación de aprovisionamiento para
  revalidar el alcance exacto de `UnidadAdmin` en consulta y reintento sin afectar filas
  históricas.
- **Compatibilidad**: las filas existentes permanecen con `ID_UNIDAD_OBJETIVO IS NULL`; no se
  inventa unidad para historia y la regla de obligatoriedad para operaciones ordinarias nuevas
  reside en el servicio de seguridad.
- **Ejecución y prueba**: el usuario confirmó la ejecución manual exitosa del DDL y de la prueba
  asociada. La salida confirmada es `T027 OK: ID_UNIDAD_OBJETIVO conservada bajo SAVEPOINT`; el
  bloque PL/SQL y el rollback al savepoint completaron correctamente.
- **Catálogo y CHANGELOG**: sincronizados como `VIGENTE` tras la confirmación humana; la columna
  se documenta con nota 027 en `database/database-schema.md` y se conserva la `NEEDS CLARIFICATION`
  sobre `NOT NULL` o trigger futuros.
- **Aprobador / fecha y hora / resultado**: usuario — 2026-07-23 — APROBADO y EJECUCIÓN CONFIRMADA.

## Ejecución confirmada — incremento 026 (T048)

- **Fecha de depósito**: 2026-07-23.
- **Archivo revisado**: `database/database-physical-design.md`.
- **SHA-256 (identificador de revisión)**:
  `9fa0456b24de81ac5225737804be718b9d1b079d7bea9aaba6515ca2d778db5c`.
- **Alcance confirmado**: `database/ddl/portafolio/026_incorporacion_registro_observacion_version.sql`,
  que agrega de forma aditiva `OBSERVACION VARCHAR2(2000 CHAR)` nullable y `VERSION NUMBER(10)
  DEFAULT 0 NOT NULL` a `INCORPORACION_REGISTRO`; incluye la prueba manual
  `database/tests/026_incorporacion_registro_observacion_version_test.sql` bajo savepoint.
- **Motivo**: corrección forward-only T048 para alinear el tipo, longitud, default y contador de
  concurrencia con el mapeo JPA vigente.
- **Restricción de diseño**: el mapeo actual no declara `nullable=false` en `OBSERVACION`; se conserva
  nullable y sin default. La supuesta obligatoriedad funcional de esa columna queda registrada como
  `NEEDS CLARIFICATION`; esta revisión no autoriza agregar un `NOT NULL` futuro sin decisión aprobada.
- **Ejecución y prueba**: el usuario confirmó la ejecución manual exitosa del DDL y de
  `database/tests/026_incorporacion_registro_observacion_version_test.sql`. La salida confirmada es
  `T026 OK: OBSERVACION y VERSION=0 verificadas bajo SAVEPOINT`; el bloque PL/SQL y el rollback al
  savepoint completaron correctamente.
- **Catálogo y CHANGELOG**: sincronizados como `VIGENTE` tras la confirmación humana.
- **Aprobador / fecha y hora / resultado**: usuario — 2026-07-23 — APROBADO y EJECUCIÓN CONFIRMADA.
