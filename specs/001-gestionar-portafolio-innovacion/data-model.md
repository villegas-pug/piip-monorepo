# Modelo de datos: Gestión del Portafolio PIIP

## Principios

- `PROYECTO` es la raíz persistente común de una iniciativa o un proyecto; `TIPO_REGISTRO`
  diferencia ambos sin compartir identidad, código o historial.
- Una relación separada vincula de forma inmutable una iniciativa aprobada con su único proyecto
  derivado. Crear el proyecto no transiciona la iniciativa.
- Las entidades de cada módulo solo se acceden mediante su repositorio propietario. Las referencias
  entre módulos son identificadores, no asociaciones JPA navegables.
- Las reglas funcionales residen en servicios Java/JPA. Oracle expresa integridad estructural y
  detecta carreras mediante PK, FK, UK, CHECK, índices y columnas de versión.
- Documentos formales, transiciones, auditorías, ciclos cerrados, decisiones y versiones son
  append-only. Una corrección inserta una versión o evento.
- No se define borrado lógico por defecto. Desactivación, revocación, baja, estado terminal o nueva
  versión representan el ciclo de vida aprobado.

## Vista de agregados

| Módulo | Agregado raíz | Entidades internas principales |
|---|---|---|
| `organizacion` | `UnidadOrganizacional` | `ObjetivoPei`, `ActividadPoi` |
| `seguridad` | `UsuarioPiip` | `AsignacionFuncional`, `EventoAsignacion`, `SuplenciaFuncional`, `OperacionAprovisionamiento` |
| `portafolio` | `RegistroPortafolio` (`PROYECTO`) | `RelacionIniciativaProyecto`, `UnidadResponsable`, `TitularidadResponsable`, `ParticipantePersona`, `ParticipacionPersona`, `ParticipacionUnidad`, `EvaluacionIniciativa`, `Subsanacion`, `Aplicabilidad`, `TransicionEstado`, `PlanificacionProyecto`, `CicloProyecto`, `ProductoParcial`, `PresentacionProductoFinal`, `CierreProyecto`, `IncorporacionRegistro`, `ClasificacionCampo`, `PrototipoPiip` |
| `documentos` | `SerieDocumental` | `DocumentoVersion`, `HistorialClasificacionDocumento`, `PublicacionDocumento` |
| `reportes` | `ReporteInstitucional` | `SnapshotReporte`, `ArchivoReporte`, `AprobacionReporte`, `DestinatarioReporte`, `RemisionReporte` |
| `auditoria` | `EventoAuditoria` | `AuditoriaAcceso`, `SolicitudIdempotente` |
| `consulta` | Sin persistencia propia | Proyecciones institucionales y públicas construidas desde servicios propietarios |

## Organización

### UnidadOrganizacional

Corresponde a `UNIDAD_EJECUTORA` existente.

| Atributo | Regla |
|---|---|
| `id` | PK numérica. |
| `codigo` | UK, máximo 20; fuente del prefijo del código PIIP cuando exista valor aprobado. |
| `nombre` | Obligatorio, máximo 200. |
| `unidadPadreId` | FK opcional a la misma tabla; no concede alcance descendiente. |
| `nivelJerarquico` | Mayor o igual a 1 y coherente con padre. |
| `activa`, `fechaActivacion` | Solo unidades activas admiten nuevas asignaciones o registros. |

Relaciones: una unidad puede tener muchas asignaciones y registros; cada uso requiere vínculo
explícito. La jerarquía es informativa y no extiende autorización.

### ObjetivoPei y ActividadPoi

| Atributo | Regla |
|---|---|
| `id` | PK sustituta. |
| `codigo` | UK estable; valor pendiente de aprobación funcional. |
| `descripcion` | Obligatoria. |
| `vigenteDesde`, `vigenteHasta`, `activo` | Una referencia retirada se conserva en registros históricos y no aparece en nuevas selecciones. |

Cada registro de portafolio referencia exactamente un Objetivo PEI y una Actividad POI. Los valores
iniciales y la autoridad de mantenimiento permanecen bloqueados; no se inventan semillas.

## Seguridad

### UsuarioPiip

Amplía conceptualmente `USUARIO` sin credenciales locales.

| Atributo | Regla |
|---|---|
| `id` | PK. |
| `keycloakId` | UK UUID, autoridad de identidad. |
| `login`, `correoInstitucional` | UK; coincidencias bloquean duplicados. |
| `nombreCompleto` | Dato institucional, no público. |
| `activo` | Debe coincidir operativamente con el bloqueo local y Keycloak. |

### AsignacionFuncional

Evolución de `USUARIO_ROL_UNIDAD`.

| Atributo | Regla |
|---|---|
| `id` | PK; una persona puede tener múltiples filas históricas para el mismo perfil/unidad. |
| `usuarioId`, `rolId`, `unidadId` | FK obligatorias; exactamente una combinación efectiva por operación. |
| `cargoFuncion` | Obligatorio según matriz aprobada; su catálogo/contenido está bloqueado. |
| `fechaInicio`, `fechaFin` | Inicio obligatorio; fin opcional y no anterior al inicio. |
| `revocadaEn`, `revocadaPor`, `motivoRevocacion` | La revocación confirmada invalida inmediatamente la asignación. |
| `inactivaTemporalmente` | Solo por suplencia vigente equivalente. |
| `documentoFormalId` | Obligatorio para perfiles que requieren designación o autorización formal. |
| `version` | `@Version`; bloqueo pesimista en revocación, suplencia y último `GlobalAdmin`. |

Una asignación es efectiva cuando el usuario y la asignación están activos, la fecha actual pertenece
a su vigencia, no está revocada ni temporalmente inactiva, y la unidad coincide exactamente con el
recurso. No se combinan dos asignaciones.

### EventoAsignacion y SuplenciaFuncional

- `EventoAsignacion` es append-only y registra alta, modificación, revocación, activación temporal y
  resultado con actor, rol y unidad efectivos.
- `SuplenciaFuncional` vincula asignación titular y asignación temporal distinta, suplente, inicio y
  fin obligatorios, autoridad, documento y terminación anticipada.
- No puede existir superposición para la misma asignación titular/perfil/unidad. La validación Java
  ocurre bajo bloqueo de la asignación y un índice por periodo apoya la detección.
- Al terminar, el titular se reactiva solo si su asignación sigue vigente y no fue revocada.

### OperacionAprovisionamiento

Registra clave idempotente, hash de petición, usuario objetivo, `keycloakId`, estado técnico, intento,
error recuperable y resultado Oracle. Permite compensar una identidad creada sin usuario local o
reanudar sin duplicar. No almacena contraseña, token ni secreto.

## Portafolio

### RegistroPortafolio

Mapea `PROYECTO` y representa `INICIATIVA` o `PROYECTO`.

| Grupo | Atributos e invariantes |
|---|---|
| Identidad | `id`, `tipoRegistro`, `codigo`, `codigoOrigen`, `version`. Código UK e inmutable con formato `AAAA-PREFIJO_UNIDAD-NNNNN`. |
| Inicio | `fechaInicio`; presentación automática para iniciativa y fecha de documento formal para proyecto. |
| Definición | `nombre` 500, `tipoSolucion`, `fuenteOrigen`, `detalleFuente` 500, `problemaPublico` 2000, `solucionPropuesta` 2000 opcional. Todo texto usa `TRIM` y rechaza vacío. |
| Planeamiento | FK obligatorias `objetivoPeiId` y `actividadPoiId`. |
| Estado | Estado canónico acorde al tipo; `fechaCierre` solo automática en `FINALIZADO` o `CANCELADO`. |
| Digital | `componenteDigital` booleano y `detalleComponenteDigital` 500 obligatorio cuando es verdadero. |
| Producto/cierre | `tipoProductoFinal`, `resultadosClave`, validación de resultados y referencia a cierre. |
| Nota | `nota` opcional, máximo 1000. |

Cardinalidades:

- Un registro tiene una o más unidades responsables y exactamente una principal.
- Tiene exactamente un titular vigente y cero o más titularidades históricas.
- Tiene cero o más participantes persona y unidad.
- Tiene cero o más series documentales y transiciones.
- Una iniciativa tiene cero o una relación derivada; un proyecto derivado tiene exactamente una.

### RelacionIniciativaProyecto

| Atributo | Regla |
|---|---|
| `iniciativaId` | FK y UK; debe ser `INICIATIVA` en `INICIATIVA_APROBADA`. |
| `proyectoId` | FK y UK; debe ser `PROYECTO` en `PROYECTO_EJECUCION`. |
| `creadaEn`, `creadaPor` | Auditoría de origen. |

La fila no se actualiza ni elimina. El proyecto conserva código, historial y datos propios. Su
`codigoOrigen` se fija al código de iniciativa y su tipo de solución se copia al crear.

### UnidadResponsable y TitularidadResponsable

- `UnidadResponsable`: UK registro/unidad, indicador `principal`, vigencia y auditoría. Un índice
  único condicional evita dos principales; el servicio exige al menos una y exactamente una al
  confirmar.
- `TitularidadResponsable`: usuario, inicio, fin, motivo y actor de sustitución. Un índice único
  condicional evita dos titularidades abiertas. `UnidadAdmin` sustituye bajo bloqueo y en su ámbito.

### Participantes

- `ParticipantePersona`: PK sustituta, cuenta PIIP opcional, nombres completos, institución y
  función. Sin cuenta no se exige documento, correo ni teléfono; su clasificación es `RESTRINGIDO`.
- `ParticipacionPersona`: registro, persona, inicio, fin y actor; no concede permisos.
- `ParticipacionUnidad`: registro, unidad, inicio, fin y actor.
- No existe entidad `Equipo`; se registran sus integrantes.
- Una baja cierra vigencia, nunca elimina la fila.

### EvaluacionIniciativa, Subsanacion y Aplicabilidad

- `EvaluacionIniciativa` agrupa revisión de admisibilidad, observaciones y series de opinión técnica.
- `Subsanacion` tiene UK por iniciativa, plazo, incumplimientos, apertura y atención. Solo existe una
  oportunidad y la iniciativa permanece `PRESENTADO`.
- `Aplicabilidad` conserva respuestas estructuradas de competencia, beneficiarios/resultado público,
  innovación y exclusiones, además del motivo.
- `NO_ADMISIBLE` solo procede después de la subsanación vencida con incumplimientos formales.
- `NO_APLICABLE` solo procede con la lista completa y el motivo.

### Planificación, ciclos, productos y cierre

- `PlanificacionProyecto` conserva alcance, objetivos, entregables, periodos y una versión vigente.
  Una corrección confirmada crea una nueva versión y mantiene la anterior para trazabilidad.
- `CicloProyecto` identifica proyecto, periodo quincenal, número de versión, versión anterior,
  objetivos, actividades, avance, dificultades, próximas acciones, cierre, autor y fecha.
- UK por proyecto/periodo/versión. Una corrección de ciclo cerrado crea otra fila.
- `CicloEvidencia` vincula una versión de ciclo con una versión documental apta.
- `ProductoParcial` registra ciclo, descripción, resultado, fecha, Responsable y evidencias; su
  corrección conserva versiones.
- `PresentacionProductoFinal` conserva versión presentada, documentos de sustento, Responsable y
  fecha, sin cambiar por sí sola el estado del proyecto.
- `ValidacionResultado` registra resultados clave del Responsable y validación del Evaluador.
- `CierreProyecto` conserva informe final, resultados, aprendizajes, conclusión, observación y
  Evaluador. Su confirmación y transición a `FINALIZADO` son atómicas.

### IncorporacionRegistro

| Relación | Cardinalidad/regla |
|---|---|
| Expediente a registro | Cero o uno nuevo; ante duplicado puede vincular uno existente. |
| Expediente a cambios | Uno a muchos, append-only, con antes/después y motivo. |
| Expediente a conflictos | Uno a muchos; código, duplicado o relación inválida bloquean validación. |
| Estado de incorporación | `PENDIENTE`, `VALIDADO` o `RECHAZADO`, independiente del estado del registro. |

Conserva fuente, fecha, Responsable, asistencia de `UnidadAdmin`, Evaluador, archivo o referencia,
hash, datos originales, errores, resoluciones y fechas. Un `PENDIENTE` o `RECHAZADO` no aparece como
registro ordinario en consulta o reporte.

## Máquina de estados

La lista vive en `TransicionEstadoService`; `TRANSICION_PERMITIDA` del baseline no se consulta.

| Origen | Destino | Decide | Registra | Documento/evidencia | Observación |
|---|---|---|---|---|---|
| `PRESENTADO` | `NO_ADMISIBLE` | Evaluador | Evaluador | Lista y subsanación; adicional opcional | Obligatoria |
| `PRESENTADO` | `NO_APLICABLE` | Evaluador | Evaluador | Lista estructurada; adicional opcional | Obligatoria |
| `PRESENTADO` | `INICIATIVA_APROBADA` | Autoridad | Autoridad o Evaluador con decisión | Opinión y decisión formal | Opcional |
| `PRESENTADO` | `INICIATIVA_ARCHIVADA` | Autoridad | Autoridad o Evaluador con decisión | Opinión y decisión formal | Obligatoria |
| `PROYECTO_EJECUCION` | `SUSPENDIDO` | UnidadAdmin | UnidadAdmin | Evidencia | Obligatoria |
| `PROYECTO_EJECUCION` | `CANCELADO` | Autoridad | Autoridad o Evaluador con decisión | Documento formal | Obligatoria |
| `PROYECTO_EJECUCION` | `PRODUCTO_APROBADO` | Autoridad | Autoridad o Evaluador con decisión | Documento formal | Opcional |
| `PROYECTO_EJECUCION` | `PRODUCTO_NO_APROBADO` | Autoridad | Autoridad o Evaluador con decisión | Evidencia | Obligatoria |
| `PRODUCTO_APROBADO` | `FINALIZADO` | Evaluador | Evaluador | Informe final y cierre completo | Obligatoria |
| `PRODUCTO_NO_APROBADO` | `FINALIZADO` | Evaluador | Evaluador | Informe final y cierre completo | Obligatoria |

`NO_ADMISIBLE`, `NO_APLICABLE` e `INICIATIVA_ARCHIVADA` son terminales. No hay salida de
`SUSPENDIDO`, `CANCELADO` o `FINALIZADO`. Toda transición bloquea el registro, revalida asignación,
verifica `If-Match`, actualiza estado y fecha automática, inserta historial y auditoría en una misma
transacción. Una segunda transición concurrente falla por versión.

## Matriz de los 23 campos

`PUB` = `PUBLICO`, `INT` = `INTERNO`. Una regla de documento puede elevar su clasificación a
`RESTRINGIDO`; nunca reducirla por ausencia de valor.

| N.º | Campo | Iniciativa al presentar | Proyecto al crear | Edición posterior y responsable | Privacidad |
|---:|---|---|---|---|---|
| 1 | Tipo de registro | Obligatorio `INICIATIVA`; inmutable | Generado `PROYECTO`; inmutable | Nadie | PUB |
| 2 | Código | Generado al confirmar | Generado al confirmar | Nadie | PUB |
| 3 | Código de origen | No aplica, vacío | Derivado: código de iniciativa; directo: acto/fuente obligatorio | Nadie | INT |
| 4 | Fecha de inicio | Fecha de presentación automática | Fecha del documento formal | Nadie | INT |
| 5 | Nombre | Obligatorio, 500 | Obligatorio, 500 | Responsable solo en subsanación para iniciativa; antes de confirmar proyecto | PUB |
| 6 | Tipo de solución | Obligatorio; `POR_DEFINIR` permitido | Derivado copiado; directo obligatorio | Responsable solo en subsanación de iniciativa | INT |
| 7 | Fuente/origen | Obligatorio; detalle si `OTROS` | Obligatorio; detalle si `OTROS` | Responsable solo en subsanación de iniciativa | INT |
| 8 | Responsable | Exactamente un titular | Exactamente uno; sugerido/definido antes de confirmar | UnidadAdmin sustituye dentro de ámbito | INT |
| 9 | Descripción | Problema obligatorio; solución opcional | Igual | Responsable solo en subsanación de iniciativa | INT |
| 10 | Objetivo PEI | Exactamente uno vigente | Selección propia, exactamente uno | Responsable solo en subsanación de iniciativa | INT |
| 11 | Actividad POI | Exactamente una vigente | Selección propia, exactamente una | Responsable solo en subsanación de iniciativa | INT |
| 12 | Unidades responsables | Una o más, una principal | Una o más, una principal | Responsable solo en subsanación; proyecto ajustable antes de confirmar | INT |
| 13 | Estado | Generado `PRESENTADO` | Generado `PROYECTO_EJECUCION` | Solo transición autorizada | PUB |
| 14 | Opinión técnica | No exigido al presentar | No aplica a creación | Evaluador; obligatorio antes de decisión; corrección versionada | INT |
| 15 | Decisión de iniciativa | No exigido al presentar | Referencia de origen cuando aplique | Autoridad o Evaluador con decisión; inmutable/versionado | INT |
| 16 | Aprobación de producto | No aplica | No exigido al crear | Autoridad o Evaluador con decisión; obligatorio en `PRODUCTO_APROBADO` | INT |
| 17 | Gestión del proyecto | No aplica | No exigido al crear | Responsable en ejecución; colección opcional | INT |
| 18 | Tipo de producto aprobado | No aplica | No exigido al crear | Registrador de decisión; solo `PRODUCTO_APROBADO` | INT |
| 19 | Resultados clave | No aplica | No exigido al crear | Responsable registra; Evaluador valida; obligatorio al cerrar | INT |
| 20 | Fecha de cierre | No aplica | No exigido al crear | Generada en `FINALIZADO` o `CANCELADO` | INT |
| 21 | Informe final | No aplica | No exigido al crear | Evaluador; obligatorio e inmutable al cerrar | INT |
| 22 | Componente digital | Sí/No obligatorio; detalle 500 si Sí | Igual | Responsable en subsanación de iniciativa; no editable en ejecución | INT |
| 23 | Nota | Opcional, 1000 | Opcional, 1000 | Responsable en subsanación y ejecución | INT |

Durante `PROYECTO_EJECUCION`, el Responsable solo modifica 17, 19 y 23. Durante la única
subsanación de una iniciativa, solo modifica 5 a 12, 22 y 23. Todo intento sobre otro campo se
rechaza y audita.

## Catálogos controlados

| Catálogo | Valores/estado de diseño | Autoridad |
|---|---|---|
| Tipo de registro | `INICIATIVA`, `PROYECTO` | Canónico constitucional; Java y CHECK Oracle. |
| Tipo de solución | `POTENCIAL_ADAPTABLE`, `POR_DEFINIR` | Canónico constitucional. |
| Fuente | `FICHA_INICIATIVA`, `CONCURSO_INTERNO`, `INNOVACION_ABIERTA`, `PROPUESTA_JEFATURA`, `OTROS` | Canónico constitucional. |
| Administración | `OM`, `OGTI`, `OM-OGTI` | Canónico constitucional cuando aplique al registro. |
| Producto final | `PROTOTIPO_CONCEPTUALIZADO`, `SOLUCION_FUNCIONAL` | Canónico constitucional. |
| Estado de negocio | Los once estados de la máquina definida arriba | `TransicionEstadoService`; CHECK Oracle solo restringe dominio. |
| Roles | `GlobalAdmin`, `UnidadAdmin`, `Responsable`, `Evaluador`, `Autoridad`, `Consulta` | `seguridad`; semilla controlada. |
| Clasificación | `PUBLICO`, `INTERNO`, `RESTRINGIDO` | Servicios propietarios y CHECK Oracle. |
| Antimalware | `PENDIENTE`, `LIMPIO`, `INFECTADO` | `documentos`. |
| Tipo documental | Los trece tipos y condiciones de la Constitución/especificación | `documentos`; datos controlados, no regla duplicada. |
| Estado de incorporación | `PENDIENTE`, `VALIDADO`, `RECHAZADO` | `portafolio`, separado del estado de negocio. |
| Estado de prototipo | `BORRADOR`, `EN_VALIDACION`, `OBSERVADO`, `VALIDADO`, `APROBADO`, `RECHAZADO` | `portafolio`. |
| Objetivo PEI/Actividad POI | Valores pendientes de aprobación | `organizacion`; gate funcional. |

Los catálogos retirados se inactivan y se conservan para históricos. Cambiar un catálogo controlado
genera auditoría. La obligatoriedad por transición o etapa no se lee de una semilla Oracle como
segunda máquina de negocio.

## Documentos

### SerieDocumental y DocumentoVersion

| Atributo | Regla |
|---|---|
| `serieId`, `tipoDocumentoId`, `registroId` | Una serie agrupa correcciones del mismo documento lógico. |
| `numeroVersion`, `versionAnteriorId` | UK serie/número; cadena inmutable. |
| `titulo`, `nombreOriginal`, `mimeType`, `formato` | Título público solo si no contiene datos personales y existe publicación aprobada. |
| `tamanoBytes` | `1..104857600` inclusive. |
| `hashSha256` | 64 caracteres hexadecimales calculados por servidor. |
| `storageKey` | Opaco y UK; nunca se expone en DTO público. |
| `autorId`, `fechaCarga` | Obligatorios. |
| `estadoAntimalware` | `PENDIENTE`, `LIMPIO`, `INFECTADO`. |
| `clasificacionPropuesta`, `clasificacionValidada` | Solo `PUBLICO`, `INTERNO`, `RESTRINGIDO`; propuesta no autoriza uso. |
| `formalizado` | Impide actualización o eliminación. |
| `version` | Optimista mientras aún sea mutable. |

`HistorialClasificacionDocumento` conserva clasificación anterior/nueva, Autoridad decisora,
Evaluador registrador, documento formal, motivo, fecha y resultado. Una clasificación más restrictiva
se aplica en la siguiente autorización y no altera auditorías previas.

`PublicacionDocumento` se diseña con versión, título público y fecha, pero no puede crearse ni
exponerse hasta aprobar quién confirma la publicación y qué evento fija la fecha. No existe relación
o endpoint público hacia contenido.

## Reportes

### ReporteInstitucional

| Atributo | Regla |
|---|---|
| `tipo` | Semestral o extraordinario según caso de uso, sin frecuencia mensual/trimestral obligatoria. |
| `periodo`, `fechaCorte` | Semestre 1: 30/06; semestre 2: 31/12; extraordinario según solicitud aprobada. |
| `parametros` | Filtros autorizados y ámbito del Evaluador. |
| `snapshotId`, `versionDatos` | Fuente única para PDF y XLSX. |
| `clasificacion` | `INTERNO` por defecto; `RESTRINGIDO` si contiene cualquier dato restringido. |
| `generadoPor`, `generadoEn` | Perfil efectivo Evaluador. |

- `SnapshotReporte` conserva agregados y detalle autorizados del mismo corte.
- `ArchivoReporte` conserva formato PDF/XLSX, versión, hash y versión documental.
- `AprobacionReporte` identifica Oficina de Modernización, versión exacta y destinatarios.
- `DestinatarioReporte` solo admite autoridades MIDAGRI, Oficina de Modernización y PCM-SGP con
  autorización.
- `RemisionReporte` conserva destinatario, fecha y resultado; la remisión es registrada, no una
  integración automática.
- Sin plazo de retención confirmado no existe eliminación automática ni selectiva.

## Prototipos y medición

- `PrototipoPiip`: recorrido, código, versión, versión anterior, fecha, cambios y uno de
  `BORRADOR`, `EN_VALIDACION`, `OBSERVADO`, `VALIDADO`, `APROBADO`, `RECHAZADO`.
- `PrototipoValidacion`: usuario, perfil, escenario, dispositivo, tecnología de asistencia,
  resultado, observaciones y aceptación.
- `PrototipoHallazgo`: severidad, categoría, estado y resolución; un crítico/alto mantiene
  `OBSERVADO` y bloquea liberación.
- `MedicionExperiencia`: etapa, versión, muestra, escenarios, éxito, tiempo mediano, errores,
  satisfacción, accesibilidad, cálculos, hash, coordinador y aprobador.
- `MatrizMetaRecorrido`: versión y umbrales de BR-149; debe aprobarse antes de implementar.
- El aprobador no es autor ni único validador; el Evaluador que aprueba medición es distinto del
  coordinador.

## Auditoría e idempotencia

### EventoAuditoria y AuditoriaAcceso

Append-only con actor o identidad anónima mínima, asignación, perfil, unidad, correlación, operación,
recurso, instante, resultado, motivo y cambios necesarios. No almacena contraseñas, tokens, contenido
documental ni datos personales innecesarios. Los accesos públicos no recolectan identidad personal.

### SolicitudIdempotente

UK por consumidor/operación/clave; conserva hash de payload, recurso creado, respuesta estable,
estado técnico y expiración operativa. La misma clave con payload distinto se rechaza. La política de
retención técnica no puede eliminar evidencia de auditoría ni expedientes funcionales.

## Objetos Oracle propuestos

Los nombres se fijan para planificar scripts incrementales y pueden ajustarse solo mediante revisión
del modelo físico, sin cambiar semántica o cardinalidades.

| Módulo | Tablas nuevas |
|---|---|
| `auditoria` | `SOLICITUD_IDEMPOTENTE` |
| `seguridad` | `USUARIO_ROL_UNIDAD_EVENTO`, `SUPLENCIA_FUNCIONAL`, `OPERACION_APROVISIONAMIENTO` |
| `portafolio` - estructura | `CAT_OBJETIVO_PEI`, `CAT_ACTIVIDAD_POI`, `INICIATIVA_PROYECTO`, `PROYECTO_RESPONSABLE`, `PARTICIPANTE_PERSONA`, `PROYECTO_PARTICIPANTE_PERSONA`, `PROYECTO_PARTICIPANTE_UNIDAD` |
| `portafolio` - evaluación y privacidad | `PROYECTO_CAMPO_CLASIFICACION`, `PROYECTO_CAMPO_CLASIF_HIST`, `EVALUACION_INICIATIVA`, `SUBSANACION_INICIATIVA`, `APLICABILIDAD_INICIATIVA`, `APLICABILIDAD_CRITERIO` |
| `portafolio` - ejecución | `PLANIFICACION_PROYECTO`, `CICLO_PROYECTO`, `CICLO_EVIDENCIA`, `PRODUCTO_PARCIAL`, `PRESENTACION_PRODUCTO_FINAL`, `VALIDACION_RESULTADO`, `CIERRE_PROYECTO` |
| `portafolio` - incorporación | `INCORPORACION_REGISTRO`, `INCORPORACION_CAMBIO`, `INCORPORACION_CONFLICTO` |
| `documentos` | `DOCUMENTO_SERIE`, `DOCUMENTO_CLASIFICACION_HIST`, `DOCUMENTO_PUBLICACION` |
| `reportes` | `REPORTE_INSTITUCIONAL`, `REPORTE_SNAPSHOT`, `REPORTE_ARCHIVO`, `REPORTE_APROBACION`, `REPORTE_DESTINATARIO`, `REPORTE_REMISION` |
| `portafolio` - prototipos | `PROTOTIPO_PIIP`, `PROTOTIPO_VALIDACION`, `PROTOTIPO_HALLAZGO`, `MEDICION_EXPERIENCIA`, `MEDICION_MUESTRA`, `MATRIZ_META_RECORRIDO` |

Cada PK numérica nueva usa una secuencia Oracle versionada siguiendo el baseline. Los scripts crean
primero tablas maestras, luego dependientes, después backfill, índices y constraints finales. No se
usan triggers para reglas funcionales ni procedimientos almacenados.

## Diferencias respecto del baseline

### Objetos existentes sin reemplazo

`UNIDAD_EJECUTORA`, `USUARIO`, `ROL`, `USUARIO_ROL_UNIDAD`, `PROYECTO`,
`PROYECTO_UNIDAD_ORGANICA`, `TRANSICION_PERMITIDA`, `TIPO_DOCUMENTO`, `DOCUMENTO`,
`TRANSICION_ESTADO`, `SECUENCIA_CODIGO`, `AUDITORIA_ACCESO`, `AUDITORIA_EVENTO` y sus secuencias.

### Brechas a modificar

- Checks de estado omiten `NO_ADMISIBLE`, `NO_APLICABLE` y `FINALIZADO`.
- Existe la transición prohibida `INICIATIVA_ARCHIVADA -> PRESENTADO`.
- El baseline transiciona la iniciativa aprobada al proyecto, en lugar de crear otro registro.
- No separa decisor y registrador ni exige correctamente documentos/evidencias.
- `DOCUMENTO` limita a 25 MB, no 100 MB.
- No existen relación derivada, unidad principal, titular histórico, participantes, catálogos PEI/POI,
  evaluación, ciclos, incorporación, reportes o prototipos.
- PEI/POI y unidades orgánicas son texto legacy; deben preservarse hasta un backfill aprobado.
- Asignaciones no soportan vigencia completa, revocación, suplencia ni historial repetido.
- Auditoría de acceso carece de perfil/unidad efectivos.
- La semilla de `UnidadAdmin` afirma alcance descendiente, contrario a la especificación.

### Migración y compensación

- El baseline no se renombra, reemplaza ni reejecuta.
- Cada incremento prevalida datos y conserva columnas legacy hasta confirmar el corte.
- Textos PEI/POI no mapeables bloquean el backfill; no se asigna un valor por inferencia.
- Documentos legacy quedan no publicables y no aptos como evidencia hasta clasificación y seguridad
  validadas; nunca se convierten automáticamente en `PUBLICO` o `LIMPIO`.
- Una relación histórica ambigua bloquea el corte; no se fusionan ni eliminan registros.
- Debido al commit implícito de DDL Oracle, la compensación es forward-only: detener la capacidad,
  inactivar valores o consumidores y conservar filas, documentos e historia.

El orden concreto de scripts 002-018 y sus compensaciones está en [plan.md](./plan.md). Solo tras la
ejecución humana confirmada podrá actualizarse `database/database-schema.md`.
