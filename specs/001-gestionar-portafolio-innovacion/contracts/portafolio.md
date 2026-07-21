# Contrato: Portafolio

Base: `/api/v1/portafolio`.

## Iniciativas

### Presentar iniciativa

`POST /iniciativas`

Entrada `CreateInitiativeRequest`:

- `nombre`, `tipoSolucion`, `fuenteOrigen`, `detalleFuente?`;
- `problemaPublico`, `solucionPropuesta?`;
- `responsableId`, `objetivoPeiId`, `actividadPoiId`;
- `unidades[{ unidadId, principal }]`, `participantesPersona[]`, `participantesUnidad[]`;
- `componenteDigital`, `detalleComponenteDigital?`, `nota?`;
- `fichaDocumentoVersionId`.

No acepta código, código de origen, fecha de inicio ni estado. Salida `201 InitiativeDetail` con
código generado, fecha de presentación, `PRESENTADO`, versión, ETag y enlaces de trazabilidad.

Validaciones: campos 1, 5-13 y 22; nota opcional; límites y trim; exactamente un titular, PEI, POI y
unidad principal; ficha `LIMPIO` con clasificación validada; `OTROS` y componente digital; ámbito.

### Subsanación y evaluación

| Operación | Entrada | Autorización/resultado |
|---|---|---|
| `POST /iniciativas/{id}/subsanaciones` | `OpenCorrectionRequest { venceEn, incumplimientos[] }` | Evaluador; una sola; iniciativa sigue `PRESENTADO`. |
| `PATCH /iniciativas/{id}/subsanacion` | Solo campos 5-12, 22 y 23; `If-Match` | Responsable titular dentro de plazo abierto. |
| `POST /iniciativas/{id}/opiniones-tecnicas` | `TechnicalOpinionRequest { documentoVersionId }` | Evaluador; cada corrección crea versión. |
| `POST /iniciativas/{id}/admisibilidad` | `AdmissibilityRequest { resultado, observacion }` | Evaluador; verifica campos, ficha, asignación, cardinalidades y duplicados. |
| `POST /iniciativas/{id}/aplicabilidad` | Lista estructurada y motivo | Evaluador; no confunde incumplimiento formal con no aplicabilidad. |
| `POST /iniciativas/{id}/decisiones` | `InitiativeDecisionRequest { destino, documentoVersionId, observacion? }` | Autoridad decide/registra o Evaluador registra con decisión formal. |

`NO_ADMISIBLE` requiere subsanación vencida y observación. `NO_APLICABLE` exige lista completa y
motivo. Aprobación/archivo exigen opinión y decisión formal.

## Proyectos

### Proyecto derivado

`POST /iniciativas/{id}/proyecto-derivado`

Entrada: nombre propio, PEI, POI, unidades, titular, fuente, descripción, componente digital, nota
opcional y documento formal de inicio. El servidor copia `codigoOrigen`, `tipoSolucion`, propone datos
solo antes de confirmar y crea un registro en `PROYECTO_EJECUCION`.

Solo Responsable autorizado; iniciativa `INICIATIVA_APROBADA`; máximo un derivado. Salida `201
ProjectDetail` con vínculo inmutable. Carrera de segundo derivado: `409 DERIVED_PROJECT_EXISTS`.

### Proyecto directo

`POST /proyectos-directos`

Entrada `DirectProjectRequest { origenTipo, codigoOrigen, fechaInicio, camposOficiales,
documentoAutorizacionId, evidencias[] }`. `origenTipo` distingue heredado o excepción formal. Un
heredado acredita inicio previo a PIIP, acto formal y ejecución. Solo Autoridad o Evaluador con
documento formal. Responsable no puede usar esta ruta.

## Participantes y campos de ejecución

| Operación | Regla |
|---|---|
| `POST /proyectos/{id}/participantes/personas` | Responsable titular en ejecución; alta auditada. |
| `POST /proyectos/{id}/participantes/unidades` | Igual para unidad. |
| `POST /proyectos/{id}/participaciones/{participacionId}/bajas` | Cierra vigencia; no elimina. |
| `PATCH /proyectos/{id}` | En ejecución solo permite campos 17, 19 y 23; exige `If-Match`. |

## Ciclos y producto

| Operación | Entrada/salida |
|---|---|
| `POST /proyectos/{id}/planificaciones` | Alcance, objetivos, entregables y periodos; una corrección crea versión. |
| `POST /proyectos/{id}/ciclos` | Periodo quincenal, objetivos, actividades, avance, dificultades, próximas acciones y evidencias. |
| `POST /proyectos/{id}/ciclos/{cicloId}/versiones` | Corrección con motivo; crea versión y conserva cerrada. |
| `POST /proyectos/{id}/productos-parciales` | Ciclo, descripción, resultado y evidencias aptas; versionado. |
| `POST /proyectos/{id}/producto-final/presentaciones` | Responsable; datos y documentos de sustento, sin cambiar estado. |
| `POST /proyectos/{id}/producto-final/decisiones` | Autoridad decide o Evaluador registra; destino aprobado/no aprobado. |

Producto aprobado exige documento formal y tipo canónico. No aprobado exige evidencia y observación.

## Suspensión, cancelación y cierre

| Ruta | Regla |
|---|---|
| `POST /proyectos/{id}/suspensiones` | UnidadAdmin; evidencia y observación; destino `SUSPENDIDO`. |
| `POST /proyectos/{id}/cancelaciones` | Autoridad decide o Evaluador registra; documento y observación; fecha de cierre automática. |
| `POST /proyectos/{id}/cierres` | Evaluador desde ambos estados de producto; informe final, resultados validados, aprendizajes, conclusión y observación. |

No existe comando de salida desde `SUSPENDIDO`, `CANCELADO`, `FINALIZADO` ni estados terminales de
iniciativa.

## Incorporación individual

| Operación | Entrada/salida |
|---|---|
| `POST /incorporaciones` | Fuente, fecha, Responsable, archivo/referencia, hash, código heredado y datos originales; devuelve `PENDIENTE`. |
| `POST /incorporaciones/{id}/correcciones` | Datos nuevos y motivo; ilimitadas mientras esté pendiente. |
| `POST /incorporaciones/{id}/conflictos/{conflictoId}/resoluciones` | Evaluador; resolución documentada. |
| `POST /incorporaciones/{id}/validaciones` | Evaluador selecciona estado canónico con evidencia; valida, rechaza o vincula duplicado existente. |

Un posible duplicado, código conflictivo o relación inválida bloquea validación. UnidadAdmin asiste
sin sustituir al Responsable o Evaluador.

## Errores principales

`OFFICIAL_FIELD_REQUIRED`, `FIELD_NOT_EDITABLE`, `CATALOG_NOT_ACTIVE`, `UNIT_MAIN_CARDINALITY`,
`RESPONSIBLE_CARDINALITY`, `CORRECTION_ALREADY_USED`, `CORRECTION_NOT_OPEN`,
`ADMISSIBILITY_INCOMPLETE`, `APPLICABILITY_INCOMPLETE`, `FORMAL_DECISION_REQUIRED`,
`EVIDENCE_NOT_ELIGIBLE`, `STATE_TRANSITION_NOT_ALLOWED`, `STATE_CHANGED`,
`DERIVED_PROJECT_EXISTS`, `DIRECT_PROJECT_NOT_AUTHORIZED`, `CYCLE_INCOMPLETE`,
`CLOSURE_INCOMPLETE`, `INCORPORATION_CONFLICT_UNRESOLVED`.

Cada comando es idempotente, transaccional, autorizado por perfil/ámbito y auditado.
