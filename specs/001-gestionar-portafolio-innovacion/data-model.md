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
| `seguridad` | `UsuarioPiip`, `MatrizFuncionalVersion` | `MatrizFuncion`, `MatrizFuncionPerfilUnidad`, `AsignacionFuncional`, `EventoAsignacion`, `SuplenciaFuncional`, `OperacionAprovisionamiento` |
| `portafolio` | `RegistroPortafolio` (`PROYECTO`) | `RelacionIniciativaProyecto`, `UnidadResponsable`, `TitularidadResponsable`, `ParticipantePersona`, `ParticipacionPersona`, `ParticipacionUnidad`, `EvaluacionIniciativa`, `Subsanacion`, `Aplicabilidad`, `TransicionEstado`, `PlanificacionProyecto`, `CicloProyecto`, `ProductoParcial`, `PresentacionProductoFinal`, `CierreProyecto`, `IncorporacionRegistro`, `ClasificacionCampo` |
| `documentos` | `ExpedienteInstitucional`, `SerieDocumental` | `DocumentoVersion`, `HistorialClasificacionDocumento`, `PublicacionDocumento` |
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

`ObjetivoPeiVersion` y `ActividadPoiVersion` son cabeceras independientes. Cada una conserva código
de versión UK, versión anterior, documento de aprobación en expediente institucional, oficina de
planeamiento aprobadora, vigencias, `GlobalAdmin` registrador y fecha. Una versión confirmada es
inmutable y no activa, modifica o retira versiones del otro catálogo.

Cada ítem `ObjetivoPei` o `ActividadPoi` pertenece a su cabecera, tiene código, descripción, vigencias
y estado. La UK es versión/código. Cada registro de portafolio referencia exactamente un ítem de cada
catálogo y conserva su versión de origen. Una referencia retirada permanece en históricos y no aparece
en nuevas selecciones. Las semillas iniciales deben coincidir con aprobaciones formales independientes;
no se inventan valores.

## Seguridad

### UsuarioPiip

Amplía conceptualmente `USUARIO` sin credenciales locales.

| Atributo | Regla |
|---|---|
| `id` | PK. |
| `keycloakId` | UK UUID, autoridad de identidad. |
| `login`, `correoInstitucional` | Datos informativos y UK cuando existen; pueden ser nulos para la identidad fundacional creada solo por `sub`. |
| `nombreCompleto` | Dato institucional no público; puede ser nulo para la identidad fundacional. |
| `activo` | Debe coincidir operativamente con el bloqueo local y Keycloak. |

### AsignacionFuncional

Evolución de `USUARIO_ROL_UNIDAD`.

| Atributo | Regla |
|---|---|
| `id` | PK; una persona puede tener múltiples filas históricas para el mismo perfil/unidad. |
| `usuarioId`, `combinacionMatrizId` | FK obligatorias; la combinación deriva función, perfil y unidad concreta. |
| `fechaInicio`, `fechaFin` | Inicio obligatorio; fin opcional y no anterior al inicio. |
| `revocadaEn`, `revocadaPor`, `motivoRevocacion` | La revocación confirmada invalida inmediatamente la asignación. |
| `inactivaTemporalmente` | Solo por suplencia vigente equivalente. |
| `documentoFormalVersionId` | Versión exacta obligatoria para perfiles que requieren designación o autorización formal. |
| `version` | `@Version`; bloqueo pesimista en revocación, suplencia y último `GlobalAdmin`. |

Una asignación es efectiva cuando el usuario y la asignación están activos, la fecha actual pertenece
a su vigencia, no está revocada ni temporalmente inactiva, y la unidad coincide exactamente con el
recurso. No se combinan dos asignaciones.

### Matriz funcional

- `MatrizFuncionalVersion`: código UK, versión anterior, vigencias, documento de aprobación en
  expediente institucional, registrador y fecha; confirmada es inmutable.
- `MatrizFuncion`: función incluida en una versión, código y descripción; UK versión/código.
- `MatrizFuncionPerfilUnidad`: exactamente una función, un rol canónico y una unidad concreta,
  aprobación formal, vigencia y actividad; UK versión/función/perfil/unidad.
- Una modificación o inactivación crea una nueva versión. Las asignaciones históricas mantienen la
  combinación original; las nuevas rechazan combinaciones futuras, vencidas o inactivas.
- El cliente no envía perfil, función o unidad separados al crear la asignación.

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
error recuperable y resultado Oracle. Si Oracle falla después de crear la identidad, esta permanece
deshabilitada y la operación se reanuda sin duplicar. No almacena contraseña, token ni secreto.

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

El prefijo procede de un valor formalmente aprobado para la unidad principal. Si no existe, el
servicio rechaza la presentación o creación; no usa nombre, abreviatura o jerarquía como fallback.
`ADMINISTRACION` permanece como columna legacy nullable, sin autoridad ni obligatoriedad para nuevos
casos de uso. La separación de `DESCRIPCION` legacy en problema y solución requiere un mapeo aprobado;
sin él, cualquier migración futura permanece diferida.

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
| Tipo documental | Los trece tipos y condiciones de la Constitución/especificación | `documentos`; datos controlados, no regla duplicada. |
| Estado de incorporación | `PENDIENTE`, `VALIDADO`, `RECHAZADO` | `portafolio`, separado del estado de negocio. |
| Estado de prototipo | Diferido junto con US9; valores preservados solo para trazabilidad futura. | No activo en la Fase 1 actual. |
| Objetivo PEI/Actividad POI | Versiones independientes con semillas aprobadas | `organizacion`; planeamiento aprueba y `GlobalAdmin` registra. |

Los catálogos retirados se inactivan y se conservan para históricos. Cambiar un catálogo controlado
genera auditoría. La obligatoriedad por transición o etapa no se lee de una semilla Oracle como
segunda máquina de negocio.

La semilla 021 prevalida y reutiliza una sola vez la unidad `MIDAGRI`, crea la función
`ADMINISTRADOR_PIIP`, la combinación con `GlobalAdmin`, el usuario por `sub` y la primera asignación. Registra Jefatura autorizante,
aprobación de despliegue, DBA, fecha, operación y resultado, y aborta ante cualquier antecedente.

## Documentos

### TipoDocumento

Evoluciona `TIPO_DOCUMENTO` con `contexto` igual a `PORTAFOLIO` o `INSTITUCIONAL`.
`estadoAsociado` permanece obligatorio para tipos de portafolio y es nulo para tipos institucionales,
mediante CHECK coherente con el contexto. Así no se asigna un estado de negocio ficticio a
aprobaciones de PEI, POI, matriz o designaciones. Los valores institucionales son datos controlados
formalmente aprobados; el DDL no inventa tipos documentales.

### ExpedienteInstitucional

Propietario de documentos formales sin iniciativa o proyecto. Conserva `id`, código UK e inmutable,
asunto, módulo de origen, referencia del caso de uso, clasificación, creador, fecha y `@Version`. No
se agregan estados o transiciones no aprobados y nunca participa en la consulta pública.

### SerieDocumental y DocumentoVersion

`DOCUMENTO_SERIE` es la raíz lógica nueva y cada fila de la tabla baseline `DOCUMENTO` representa una
versión. No se crea `DOCUMENTO_VERSION`. La migración conserva los identificadores existentes y
asigna una serie solo cuando su propietario y cadena de versiones se validan sin ambigüedad.

| Atributo | Regla |
|---|---|
| `serieId`, `tipoDocumentoId` | Una serie agrupa correcciones del mismo documento lógico. |
| `registroId`, `expedienteInstitucionalId` | Exactamente uno es no nulo; CHECK XOR. La pertenencia es inmutable. |
| `numeroVersion`, `versionAnteriorId` | UK serie/número; cadena inmutable. |
| `titulo`, `nombreOriginal`, `mimeType`, `formato` | Título público solo si no contiene datos personales y existe publicación aprobada. |
| `tamanoBytes` | `1..104857600` inclusive. |
| `hashSha256` | 64 caracteres hexadecimales calculados por servidor. |
| `contenido` | BLOB Oracle; nunca se expone en DTO público ni consulta pública. |
| `autorId`, `fechaCarga` | Obligatorios. |
| `clasificacionPropuesta`, `clasificacionValidada` | Solo `PUBLICO`, `INTERNO`, `RESTRINGIDO`; propuesta no autoriza uso. |
| `formalizado` | Impide actualización o eliminación. |
| `version` | Optimista mientras aún sea mutable. |

`HistorialClasificacionDocumento` conserva clasificación anterior/nueva, Autoridad decisora,
Evaluador registrador, documento formal, motivo, fecha y resultado. Una clasificación más restrictiva
se aplica en la siguiente autorización y no altera auditorías previas.

`PublicacionDocumento` es append-only y tiene FK/UK a una versión, título público validado,
Evaluador y asignación efectiva confirmadores y `fechaPublicacion` del servidor. Solo se crea para una
versión con clasificación `PUBLICO` validada y título sin datos personales. La confirmación
es idempotente. Una reclasificación restrictiva excluye la versión de futuras proyecciones públicas
sin borrar la publicación o auditoría. No existe relación o endpoint público hacia contenido.

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

`SnapshotReporte` conserva un CLOB JSON canónico, versión de esquema, hash SHA-256, corte, parámetros
y clasificación. El proceso serializa de forma determinista claves, números, fechas y colecciones
antes de calcular el hash. PDF y XLSX referencian el mismo snapshot y nunca reconstruyen el corte
desde datos operativos posteriores.

## Prototipos y medición (diferido)

US9, sus objetos físicos y el incremento 018 están diferidos a una fase posterior por la enmienda
constitucional 5.0.0. La siguiente descripción se conserva como trazabilidad y no define entidades,
tablas ni contratos activos de la Fase 1 actual.

- `PrototipoPiip`: recorrido limitado a `REGISTRO`, `EVALUACION`, `DECISION`, `SEGUIMIENTO`,
  `APROBACION_PRODUCTO`, `CIERRE`, `CONSULTA_INSTITUCIONAL` o `CONSULTA_PUBLICA`; además código,
  versión, versión anterior, fecha, cambios y uno de
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
- Cada medición referencia una versión de dataset sintético formalmente aprobado. Sin esa referencia
  no puede confirmarse ni habilitar una aprobación.
- La preparación para liberación referencia la versión funcional y de accesibilidad candidata y una
  medición aprobada de esa misma versión. Todo cambio invalida el gate anterior y exige otra medición.

## Auditoría e idempotencia

### EventoAuditoria y AuditoriaAcceso

Append-only con actor o identidad anónima mínima, asignación, perfil, unidad, correlación, operación,
recurso, instante, resultado, motivo y cambios necesarios. No almacena contraseñas, tokens, contenido
documental ni datos personales innecesarios. Los accesos públicos no recolectan identidad personal.

### SolicitudIdempotente

UK por consumidor/operación/clave; conserva hash de payload, recurso creado, respuesta estable,
estado técnico y expiración operativa. La misma clave con payload distinto se rechaza. La política de
retención técnica no puede eliminar evidencia de auditoría ni expedientes funcionales.

La ventana inicial de reutilización es de siete días y se configura externamente. Una operación
funcional aún recuperable no expira por el solo vencimiento de esa ventana.

## Objetos Oracle propuestos

Los nombres se fijan para planificar scripts incrementales y pueden ajustarse solo mediante revisión
del modelo físico, sin cambiar semántica o cardinalidades.

Antes de escribir DDL, `database-specialist` completa por objeto columnas, tipos Oracle, longitudes,
nulabilidad, defaults, secuencias, PK/FK/UK/CHECK e índices auxiliares. Una revisión humana DB aprueba
ese diccionario. Los scripts son de ejecución única, prevalidan esquema y versión y fallan antes del
primer DDL ante cualquier incompatibilidad.

El diccionario físico revisable reside en `database/database-physical-design.md` y su evidencia en
`database/physical-design-approval.md`. La versión precedente se valida mediante una huella explícita
de objetos esperados en el catálogo `USER_*`, no mediante una tabla de versión ni leyendo el
`CHANGELOG` desde Oracle. La huella incluye tablas, columnas, constraints y sus columnas, índices,
columnas o expresiones indexadas y secuencias.

| Módulo | Tablas nuevas |
|---|---|
| `auditoria` | `SOLICITUD_IDEMPOTENTE` |
| `organizacion` | `CAT_OBJETIVO_PEI_VERSION`, `CAT_OBJETIVO_PEI`, `CAT_ACTIVIDAD_POI_VERSION`, `CAT_ACTIVIDAD_POI` |
| `seguridad` | `MATRIZ_FUNCIONAL_VERSION`, `MATRIZ_FUNCION`, `MATRIZ_FUNCION_PERFIL_UNIDAD`, `USUARIO_ROL_UNIDAD_EVENTO`, `SUPLENCIA_FUNCIONAL`, `OPERACION_APROVISIONAMIENTO` |
| `portafolio` - estructura | `INICIATIVA_PROYECTO`, `PROYECTO_RESPONSABLE`, `PARTICIPANTE_PERSONA`, `PROYECTO_PARTICIPANTE_PERSONA`, `PROYECTO_PARTICIPANTE_UNIDAD` |
| `portafolio` - evaluación y privacidad | `PROYECTO_CAMPO_CLASIFICACION`, `PROYECTO_CAMPO_CLASIF_HIST`, `EVALUACION_INICIATIVA`, `SUBSANACION_INICIATIVA`, `APLICABILIDAD_INICIATIVA`, `APLICABILIDAD_CRITERIO` |
| `portafolio` - ejecución | `PLANIFICACION_PROYECTO`, `CICLO_PROYECTO`, `CICLO_EVIDENCIA`, `PRODUCTO_PARCIAL`, `PRESENTACION_PRODUCTO_FINAL`, `VALIDACION_RESULTADO`, `CIERRE_PROYECTO` |
| `portafolio` - incorporación | `INCORPORACION_REGISTRO`, `INCORPORACION_CAMBIO`, `INCORPORACION_CONFLICTO` |
| `documentos` | `EXPEDIENTE_INSTITUCIONAL`, `DOCUMENTO_SERIE`, `DOCUMENTO_CLASIFICACION_HIST`, `DOCUMENTO_PUBLICACION` |
| `reportes` | `REPORTE_INSTITUCIONAL`, `REPORTE_SNAPSHOT`, `REPORTE_ARCHIVO`, `REPORTE_APROBACION`, `REPORTE_DESTINATARIO`, `REPORTE_REMISION` |
| `portafolio` - prototipos | Diferido a una fase posterior; no se crean objetos 018 en la Fase 1 actual. |

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
- No existen relación derivada, unidad principal, titular histórico, participantes, versiones
  independientes PEI/POI, matriz funcional, expediente institucional, evaluación, ciclos,
  incorporación, reportes o prototipos.
- PEI/POI y unidades orgánicas son texto legacy; se preservan y no se migran en la Fase 1 actual.
- Asignaciones no soportan vigencia completa, revocación, suplencia ni historial repetido.
- `TIPO_DOCUMENTO.ESTADO_ASOCIADO` es obligatorio y solo admite estados del portafolio; debe
  distinguir contexto institucional antes de cargar documentos aprobatorios sin proyecto.
- Auditoría de acceso carece de perfil/unidad efectivos.
- La semilla de `UnidadAdmin` afirma alcance descendiente, contrario a la especificación.

### Migración y compensación

- El baseline no se renombra, reemplaza ni reejecuta.
- Cada incremento prevalida datos y conserva columnas legacy hasta confirmar el corte.
- Textos PEI/POI no mapeables se conservan sin migrar; una fase futura no podrá asignar valores por
  inferencia y exigirá mapeos hacia ítems de versiones independientes aprobadas.
- Las cadenas documentales legacy deben migrarse a series de un único propietario. Cadenas rotas,
  ciclos o versiones con proyectos incompatibles bloquean el corte.
- Las asignaciones legacy requieren una combinación aprobada de función, perfil y unidad concreta;
  `ID_ROL` e `ID_UNIDAD` deben coincidir con ella antes del constraint final.
- Documentos legacy quedan no publicables y no aptos como evidencia hasta validar clasificación e
  integridad; nunca se convierten automáticamente en `PUBLICO`.
- `DOCUMENTO.SCAN_ANTIVIRUS` y `DOCUMENTO.NOMBRE_STORAGE` se conservan nullable como legado sin
  default, constraint, mapeo ni consumidor; sus valores históricos no autorizan ni bloquean
  operaciones PIIP.
- Una relación histórica ambigua bloquea el corte; no se fusionan ni eliminan registros.
- Debido al commit implícito de DDL Oracle, la compensación es forward-only: detener la capacidad,
  inactivar valores o consumidores y conservar filas, documentos e historia.

El orden concreto de scripts 002-024 y sus compensaciones está en [plan.md](./plan.md). Solo tras la
ejecución humana confirmada podrá actualizarse `database/database-schema.md`.
