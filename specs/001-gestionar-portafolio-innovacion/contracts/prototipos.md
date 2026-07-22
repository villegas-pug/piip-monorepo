# Contrato: Prototipos, mediciones y metas

Base: `/api/v1/portafolio/prototipos`.

Esta subcapacidad registra evidencia del gate; no implementa por sí misma las interfaces evaluadas.

## Versiones de prototipo

| Operación | Entrada/salida |
|---|---|
| `POST /` | Recorrido canónico, código, fecha, cambios, `expedienteInstitucionalId` y `artefactoDocumentoVersionId`; crea versión `BORRADOR`. |
| `POST /{id}/versiones` | Cambio funcional/accesibilidad y versión anterior; crea nueva versión. |
| `GET /` | Filtros por recorrido, estado, versión y paginación. |
| `GET /{id}` | Detalle, validaciones, hallazgos, medición y meta autorizados. |

Estados únicos: `BORRADOR`, `EN_VALIDACION`, `OBSERVADO`, `VALIDADO`, `APROBADO`, `RECHAZADO`.
Recorridos únicos: `REGISTRO`, `EVALUACION`, `DECISION`, `SEGUIMIENTO`, `APROBACION_PRODUCTO`,
`CIERRE`, `CONSULTA_INSTITUCIONAL`, `CONSULTA_PUBLICA`.

`REGISTRO` cubre iniciativa y creación de proyectos derivados/directos; `SEGUIMIENTO` cubre
suspensión y `DECISION` cubre cancelación. Seguridad, reportes y administración de esta subcapacidad
no agregan recorridos.
El artefacto debe pertenecer al expediente institucional del prototipo y referenciar una versión
documental exacta; cualquier discordancia se rechaza.

## Validaciones y hallazgos

| Operación | Regla |
|---|---|
| `POST /{id}/validaciones` | Evaluador registra usuario, perfil, escenario, resultado, observación, aceptación, escritorio/móvil y asistencia aplicable. |
| `POST /{id}/hallazgos` | Severidad, categoría, descripción y evidencia. |
| `POST /{id}/hallazgos/{hallazgoId}/resoluciones` | Nueva evidencia; no borra hallazgo. |

Se exige al menos un usuario por perfil involucrado y actor sectorial cuando aplique. Un hallazgo
crítico o alto mantiene `OBSERVADO`.

## Medición de experiencia

`POST /{id}/mediciones`

Entrada: etapa, muestra por perfil, escenarios moderados, datos sintéticos, éxito de tarea, tiempos,
errores críticos, satisfacción, accesibilidad, cálculos, hallazgos y hash. Identifica equipo ejecutor,
Evaluador coordinador y otro Evaluador aprobador.

Muestra objetivo: cinco por perfil; una menor exige justificación y nunca omite un perfil. No se
aceptan datos personales reales.

La entrada incluye `datasetSinteticoVersionId`, que debe identificar un dataset formalmente aprobado.
Mientras ese insumo no exista, la medición no puede confirmarse.

## Matriz de metas

`POST /{id}/metas`

Registra versión aprobada por recorrido: éxito >= 90 %, cero errores críticos, satisfacción >= 4/5,
sin hallazgos críticos/altos y meta de tiempo según exista línea base comparable. Una excepción solo
puede afectar otras métricas y exige justificación/aprobación.

## Aprobar prototipo

`POST /{id}/aprobaciones`

Solo Evaluador de la Oficina. Rechaza cuando:

- aprobador es autor o único validador;
- falta un perfil/actor sectorial aplicable;
- falta medición inicial aprobada o matriz de metas;
- existe error crítico o hallazgo crítico/alto;
- falta cobertura de escritorio/móvil o accesibilidad aplicable.

Errores: `PROTOTYPE_VALIDATION_INCOMPLETE`, `PROTOTYPE_SEGREGATION_VIOLATION`,
`MEASUREMENT_REQUIRED`, `TARGET_MATRIX_REQUIRED`, `CRITICAL_FINDING_OPEN`.

Toda versión, validación, medición, meta, aprobación, rechazo y denegación es inmutable y auditada.

## Preparación para liberación

`POST /{id}/preparaciones-liberacion`

Exige una medición aprobada correspondiente a la versión funcional y de accesibilidad candidata. Un
cambio posterior invalida la preparación y obliga a registrar otra medición. Responde
`RELEASE_MEASUREMENT_REQUIRED` cuando la medición falta, no está aprobada o pertenece a otra versión.
