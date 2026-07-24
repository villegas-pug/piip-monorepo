# Feature Specification: Gestión del Portafolio Institucional de Innovación Pública

**Feature Branch**: `main`
**Created**: 2026-07-21
**Status**: Approved
**Input**: Gestión integral de iniciativas y proyectos del PIIP conforme a la Norma Técnica
N.º 003-2025-PCM-SGP, desde el registro hasta el reporte y la consulta pública.

## Purpose and Scope *(mandatory)*

**Purpose**: Permitir que el MIDAGRI mantenga un portafolio institucional y sectorial actualizado,
trazable y respaldado por responsables, decisiones formales y evidencias, reduciendo la dispersión
de información y el riesgo de reportes incompletos o desactualizados.

## Diferimiento aprobado de US9

La subcapacidad US9, sus prototipos, mediciones, matrices de metas, preparación de liberación y los
gates asociados a interfaces quedan diferidos a una fase posterior por la enmienda constitucional
5.0.0 del 2026-07-22. FR-140 a FR-160 y sus escenarios se conservan como trazabilidad futura, pero
no son requisitos ejecutables de la Fase 1 actual.

**In Scope**:

- Registrar y mantener iniciativas con sus datos oficiales, participantes y documentos.
- Evaluar iniciativas, emitir opinión técnica y registrar decisiones formales.
- Crear un único proyecto vinculado con cada iniciativa aprobada y proyectos directos
  excepcionales.
- Registrar planificación, ciclos, avances, dificultades, productos parciales y evidencias.
- Presentar y decidir sobre el producto final, y cerrar administrativamente el proyecto.
- Gestionar participantes, perfiles y ámbitos organizacionales.
- Consultar el portafolio institucional dentro del ámbito autorizado.
- Generar el reporte institucional semestral y reportes cuando sean requeridos.
- Permitir que participantes de Programas, Proyectos Especiales y Organismos Públicos Adscritos
  mantengan iniciativas y proyectos de su ámbito.
- Buscar y consultar públicamente `Tipo de registro`, `Código`, `Nombre de iniciativa o proyecto`
  y `Estado`, además de tipo documental, título sin datos personales, versión, formato y fecha de
  publicación.

**Out of Scope**:

- Definir arquitectura, infraestructura, persistencia o diseño técnico.
- Conectores, sincronizaciones o intercambios automáticos con sistemas funcionales externos no
  aprobados en la Fase 1. Esta exclusión no alcanza el uso obligatorio de Keycloak Admin API para el
  ciclo de identidad definido por la Constitución.
- Reportes mensuales o trimestrales obligatorios durante la Fase 1.
- Mostrar contenido documental o permitir descargas en la consulta pública durante la Fase 1.
- Diseñar, validar o persistir prototipos PIIP, mediciones, datasets sintéticos, matrices de metas o
  preparación de liberación; quedan diferidos a una fase posterior.
- Administrar análisis, detección, bloqueo, cuarentena o respuesta antimalware; corresponden
  exclusivamente a OGTI fuera de PIIP.
- Considerar aprobados los prototipos existentes o genéricos.
- Realizar cargas manuales conjuntas o masivas de información existente; la incorporación inicial
  se hará mediante registro individual del Responsable, asistencia de `UnidadAdmin` y validación
  del Evaluador, conforme a los controles de validación definidos en esta especificación.
- Reanudar proyectos suspendidos o ejecutar transiciones salientes de `SUSPENDIDO` o `CANCELADO`
  mientras no exista una enmienda constitucional que las autorice.

## Clarifications

### Session 2026-07-21

- Q: ¿Cuántos proyectos derivados puede originar una iniciativa aprobada? → A: Exactamente un
  proyecto derivado.
- Q: ¿Cómo se aplica el alcance de una asignación sobre unidades descendientes? → A: Solo unidades
  asignadas explícitamente.
- Q: ¿Qué campos del portafolio serán públicos en la Fase 1? → A: Tipo de registro, código, nombre
  y estado.
- Q: ¿Con qué frecuencia debe registrarse cada ciclo obligatorio durante la ejecución? → A:
  Quincenal.
- Q: ¿Quién registra, asiste y valida la incorporación individual de información existente? → A:
  El Responsable registra, `UnidadAdmin` asiste y el Evaluador valida.
- Q: ¿Qué criterio distingue `NO_ADMISIBLE` de `NO_APLICABLE`? → A: `NO_ADMISIBLE` corresponde al
  incumplimiento de requisitos formales; `NO_APLICABLE`, a casos que no corresponden a innovación
  pública.
- Q: ¿Qué caracteriza a un proyecto heredado? → A: Haber iniciado antes de PIIP y contar con acto
  formal y evidencia de ejecución.
- Q: ¿Cómo se aplican múltiples perfiles de una persona en una operación? → A: Se selecciona una
  asignación efectiva y no se combinan permisos.
- Q: ¿Qué periodo y fecha de corte usa el reporte semestral oficial? → A: Enero-junio con corte al
  30/06 y julio-diciembre con corte al 31/12.
- Q: ¿Qué perfil usa un participante sectorial para mantener registros de su entidad? → A:
  `Responsable` con unidades explícitamente asignadas.
- Q: ¿Cuándo debe hacerse efectiva la revocación de una asignación funcional? → A:
  Inmediatamente después de confirmarla.
- Q: ¿Qué metadatos documentales serán públicos en la Fase 1? → A: Todo metadato documental
  descriptivo que no contenga datos personales.
- Q: ¿Cuántas unidades responsables puede tener una iniciativa o proyecto? → A: Varias, con una
  unidad principal.
- Q: ¿Cómo se corrige un ciclo quincenal ya cerrado? → A: Mediante una nueva versión trazable.
- Q: ¿Qué perfil genera el reporte institucional? → A: `Evaluador`.
- Q: ¿Qué datos deben ser obligatorios al presentar una iniciativa nueva? → A: Los campos oficiales
  1 al 13, 22 y 23; se excluyen los campos de evaluación, ejecución, producto y cierre.
- Q: ¿Qué regla de vigencia deben usar las asignaciones funcionales? → A: Fecha de inicio
  obligatoria y fecha de fin opcional.
- Q: ¿Quién autoriza una reclasificación de privacidad? → A: La Autoridad decide y el Evaluador
  registra.
- Q: ¿Qué oportunidad de subsanación existe antes de declarar `NO_ADMISIBLE`? → A: Una subsanación
  con plazo registrado por el Evaluador.
- Q: ¿Qué contenido mínimo debe registrar cada ciclo quincenal? → A: Objetivos, actividades, avance,
  dificultades, próximas acciones y evidencias.
- Q: Después de presentar una iniciativa en estado `PRESENTADO`, ¿cuándo puede el `Responsable`
  editar los campos oficiales 1 al 13, 22 y 23? → A: Solo los campos 5 al 12, 22 y 23 durante la
  única subsanación abierta por el Evaluador.
- Q: ¿Quién puede confirmar la revocación de una asignación funcional? → A: `GlobalAdmin` en el
  ámbito institucional y `UnidadAdmin` dentro de su ámbito autorizado.
- Q: ¿Quién establece la clasificación inicial de un documento y sus metadatos? → A: El
  `Responsable` propone y el `Evaluador` valida.
- Q: Mientras la matriz no establezca una excepción más restrictiva, ¿qué acceso tendrán los campos
  no públicos? → A: Restringidos a usuarios autorizados dentro de su ámbito organizacional.
- Q: Durante la incorporación individual de información existente, ¿qué ocurre si se detecta un
  posible duplicado? → A: Se bloquea la validación hasta que el Evaluador resuelva el conflicto.
- Q: Para una iniciativa nueva, ¿cómo se trata el campo oficial 3, `Código de origen`? → A: No aplica
  y queda vacío.
- Q: ¿Cuándo se genera el campo oficial 2, `Código`, para una iniciativa nueva? → A: Automáticamente
  al confirmar la presentación.
- Q: Para presentar una iniciativa nueva, ¿qué obligatoriedad tiene el campo oficial 23, `Nota`? →
  A: Opcional.
- Q: ¿Cómo debe registrarse el campo oficial 22, `Componente digital`, al presentar una iniciativa?
  → A: `Sí/No`; si es `Sí`, descripción obligatoria.
- Q: Cuando el campo oficial 7, `Fuente u origen`, tenga el valor `OTROS`, ¿qué debe exigirse? → A:
  Una descripción obligatoria de la fuente.
- Q: Si `Tipo de solución` se registra como `POR_DEFINIR`, ¿cuándo debe resolverse? → A: Puede
  permanecer `POR_DEFINIR` definitivamente.
- Q: Para una iniciativa nueva, ¿qué representa el campo oficial 4, `Fecha de inicio`? → A: Fecha de
  presentación, generada automáticamente.
- Q: ¿Cuántos responsables puede tener una iniciativa o proyecto en el campo oficial 8,
  `Responsable`? → A: Exactamente un Responsable titular.
- Q: ¿Qué cardinalidad tienen `Objetivo PEI` y `Actividad POI` para cada iniciativa o proyecto? → A:
  Exactamente uno de cada uno.
- Q: ¿Cómo debe representar el campo oficial 9, `Descripción`, el problema público y la solución
  propuesta? → A: Problema obligatorio y solución opcional.
- Q: Además del Responsable titular, ¿qué cardinalidad tienen los participantes de una iniciativa al
  presentarla? → A: Cero o más participantes.
- Q: ¿Qué tipos de participante pueden asociarse a una iniciativa o proyecto? → A: Personas y
  unidades; los equipos se representan mediante sus integrantes.
- Q: ¿Una persona participante debe tener obligatoriamente una cuenta PIIP? → A: No; puede
  registrarse sin acceso y vincularse si tiene cuenta.
- Q: ¿Qué datos mínimos se registran para una persona participante sin cuenta PIIP? → A: Nombres
  completos, institución y función.
- Q: Durante `PROYECTO_EJECUCION`, ¿puede el Responsable modificar los participantes del proyecto? →
  A: Sí; altas y bajas auditadas.
- Q: ¿Quién puede sustituir al Responsable titular de una iniciativa o proyecto? → A: `UnidadAdmin`
  dentro de su ámbito.
- Q: ¿Cuándo se hace efectiva la sustitución del Responsable titular? → A: Inmediatamente al
  confirmarla `UnidadAdmin`.
- Q: ¿Quién puede asignar, modificar o revocar el perfil `UnidadAdmin`? → A: Únicamente
  `GlobalAdmin`.
- Q: ¿Cómo se autoriza una nueva asignación `GlobalAdmin`? → A: La `Autoridad` decide y un
  `GlobalAdmin` la registra con documento formal.
- Q: ¿Cómo se crea el primer `GlobalAdmin` cuando aún no existe otro que registre la decisión? → A:
  Mediante la semilla SQL manual, fail-fast y auditada aprobada para la inicialización.
- Q: ¿Cómo debe manejarse una suplencia funcional en PIIP? → A: Asignación temporal con inicio y fin
  obligatorios.
- Q: ¿Quién autoriza una suplencia funcional? → A: La misma autoridad que autoriza la asignación
  permanente del perfil.
- Q: Durante la vigencia de una suplencia, ¿qué ocurre con la asignación equivalente del titular? →
  A: Queda temporalmente inactiva para ese perfil y unidad.
- Q: ¿Puede existir más de una suplencia solapada para la misma asignación de perfil y unidad? → A:
  No; toda superposición se rechaza.
- Q: ¿Quién puede terminar anticipadamente una suplencia? → A: La misma autoridad que la autorizó.
- Q: Si una asignación se revoca, vence o queda inactiva mientras una operación sensible está en
  curso, ¿qué ocurre? → A: Se revalida antes de aplicar y se rechaza si ya no está vigente.
- Q: ¿Quién puede asignar, modificar o revocar los perfiles `Responsable` y `Consulta`? → A:
  `GlobalAdmin` institucionalmente y `UnidadAdmin` dentro de su ámbito.
- Q: ¿Quién autoriza y registra las asignaciones del perfil `Evaluador`? → A: La Oficina de
  Modernización autoriza y `GlobalAdmin` registra con documento formal.
- Q: ¿Cómo se asigna, modifica o revoca el perfil `Autoridad`? → A: `GlobalAdmin` registra según
  designación formal institucional vigente.
- Q: ¿Cuál es la longitud máxima de `Nombre de iniciativa o proyecto`? → A: 500 caracteres.
- Q: ¿Cuál es la longitud máxima de la descripción del problema público? → A: 2000 caracteres.
- Q: ¿Cuál es la longitud máxima de la solución propuesta opcional? → A: 2000 caracteres.
- Q: ¿Cuál es la longitud máxima de las descripciones de `OTROS` y `Componente digital`? → A: 500
  caracteres.
- Q: ¿Cuál es la longitud máxima de `Nota`? → A: 1000 caracteres.
- Q: ¿Cómo se seleccionan `Objetivo PEI` y `Actividad POI`? → A: Desde catálogos controlados
  vigentes.
- Q: ¿Qué campos puede editar el Responsable durante una subsanación? → A: Los campos 5 al 12, 22 y
  23.
- Q: ¿Puede cambiar `Tipo de registro` después de presentar? → A: No; queda inmutable.
- Q: ¿Cómo se tratan referencias PEI o POI eliminadas del catálogo? → A: Se conservan en registros
  históricos y no se permiten en nuevas selecciones.
- Q: ¿Cómo se normalizan espacios y contenido vacío? → A: Se recortan espacios extremos y se rechaza
  el contenido compuesto solo por espacios.
- Q: ¿Cómo se gestiona el campo 14, `Informe de opinión técnica de evaluación de iniciativa`? → A:
  El Evaluador lo registra, es obligatorio antes de la decisión y sus correcciones crean versiones.
- Q: ¿Cómo se gestiona el campo 15, `Documento formal de decisión sobre la iniciativa`? → A: La
  Autoridad o el Evaluador con documento formal lo registra; es obligatorio e inmutable.
- Q: ¿Cómo se gestiona el campo 16, `Documento formal de aprobación del producto final`? → A: La
  Autoridad o el Evaluador con documento formal lo registra; es obligatorio e inmutable.
- Q: ¿Qué obligatoriedad tiene el campo 17, `Documentación de la gestión del proyecto`? → A: Es una
  colección opcional que no sustituye ciclos ni evidencias obligatorias.
- Q: ¿Cuándo es obligatorio el campo 18, `Tipo de producto final aprobado`? → A: Solo en
  `PRODUCTO_APROBADO`, usando el catálogo canónico.
- Q: ¿Quién registra y valida el campo 19, `Resultados clave`? → A: El Responsable registra y el
  Evaluador valida; es obligatorio para cerrar.
- Q: ¿Cuándo se genera el campo 20, `Fecha de cierre`? → A: Automáticamente al pasar a `FINALIZADO` o
  `CANCELADO`.
- Q: ¿Cómo se gestiona el campo 21, `Informe final de cierre`? → A: El Evaluador lo registra; es
  obligatorio e inmutable al cerrar.
- Q: ¿Cómo se corrigen los documentos formales? → A: Se crea una nueva versión y nunca se
  sobrescribe el archivo anterior.
- Q: ¿Qué campos puede editar el Responsable durante `PROYECTO_EJECUCION`? → A: Solo los campos 17,
  19 y 23, con auditoría.
- Q: ¿Qué contiene `Código de origen` en un proyecto derivado? → A: El código de la iniciativa,
  asignado automáticamente.
- Q: ¿Qué contiene `Código de origen` en un proyecto directo? → A: El identificador obligatorio del
  acto formal o de la fuente heredada.
- Q: ¿Qué representa `Fecha de inicio` de un proyecto? → A: La fecha indicada en el documento formal
  de inicio.
- Q: ¿Qué campos se exigen al crear un proyecto? → A: Los campos 1 al 13 y 22, respetando los campos
  generados; el campo 23 es opcional.
- Q: ¿Cómo se define el nombre de un proyecto derivado? → A: Tiene nombre propio obligatorio,
  sugerido inicialmente desde la iniciativa y editable antes de confirmar.
- Q: ¿Cómo se define el tipo de solución de un proyecto derivado? → A: Se copia desde la iniciativa
  al crear el proyecto.
- Q: ¿Cómo se definen PEI y POI del proyecto? → A: Se seleccionan de forma propia al crearlo.
- Q: ¿Cómo se definen las unidades responsables del proyecto derivado? → A: Se copian inicialmente
  desde la iniciativa y pueden ajustarse antes de confirmar.
- Q: ¿Cómo se define el Responsable titular del proyecto derivado? → A: Se sugiere el de la
  iniciativa y puede cambiarse antes de confirmar.
- Q: ¿Quién registra un proyecto directo? → A: La Autoridad o el Evaluador con el documento formal.
- Q: ¿Qué niveles de clasificación usa PIIP? → A: `PUBLICO`, `INTERNO` y `RESTRINGIDO`.
- Q: ¿Qué clasificación predeterminada tienen los campos no públicos sin regla especial? → A:
  `INTERNO`, visibles dentro del ámbito autorizado.
- Q: ¿Qué clasificación tiene el campo `Responsable`? → A: `INTERNO`, visible en consultas
  institucionales del ámbito.
- Q: ¿Quién puede consultar personas participantes? → A: El Responsable, el Evaluador y los
  administradores autorizados, bajo clasificación `RESTRINGIDO`.
- Q: ¿Qué metadatos documentales son públicos? → A: Tipo, título sin datos personales, versión,
  formato y fecha de publicación.
- Q: ¿Quién puede consultar contenido documental? → A: Usuarios institucionales según ámbito y
  clasificación; nunca la consulta pública en Fase 1.
- Q: ¿Qué se considera dato personal detectable? → A: Nombres, documento, correo, teléfono,
  dirección, firma e identificadores equivalentes.
- Q: ¿Quién puede consultar un documento con clasificación pendiente o no validada? → A: Solo el
  Responsable cargador y el Evaluador; no sirve como evidencia formal.
- Q: ¿Cuándo surte efecto una reclasificación? → A: Inmediatamente al registrarla el Evaluador con la
  decisión formal.
- Q: ¿Qué ocurre cuando una reclasificación es más restrictiva? → A: Bloquea accesos futuros y
  conserva las auditorías anteriores.
- Q: ¿Quién puede crear usuarios? → A: `GlobalAdmin` institucionalmente y `UnidadAdmin` dentro de su
  ámbito.
- Q: ¿A qué unidad puede pertenecer una cuenta creada por `UnidadAdmin`? → A: A una de sus unidades
  autorizadas.
- Q: ¿Cómo se realiza la activación inicial? → A: Mediante correo de activación de Keycloak; PIIP no
  gestiona la contraseña.
- Q: ¿Quién puede desactivar usuarios? → A: `GlobalAdmin` institucionalmente y `UnidadAdmin` dentro de
  su ámbito.
- Q: ¿Qué efecto tiene la desactivación? → A: Es inmediata en Keycloak y PIIP y conserva usuario,
  asignaciones e historial.
- Q: ¿Quién puede reactivar un usuario y qué asignaciones recupera? → A: La misma autoridad que puede
  desactivarlo; no restaura asignaciones vencidas o revocadas.
- Q: ¿Cómo se modifica o revoca una asignación `GlobalAdmin`? → A: Con decisión formal de la
  Autoridad y registro por otro `GlobalAdmin`.
- Q: ¿Puede revocarse el último `GlobalAdmin` activo? → A: No, hasta designar un reemplazo.
- Q: ¿Cómo se trata un cambio de unidad de una persona? → A: Se cierran las asignaciones anteriores y
  se crean nuevas asignaciones explícitas.
- Q: ¿Cómo se trata una identidad duplicada? → A: Se bloquea y se resuelve usando el identificador de
  Keycloak y el correo institucional.
- Q: ¿Los campos y catálogos completos son requisito formal de admisibilidad? → A: Sí.
- Q: ¿Qué condición debe cumplir la ficha de iniciativa para la admisibilidad? → A: Debe estar
  adjunta, conservar integridad SHA-256 y tener clasificación validada.
- Q: ¿Qué condición debe cumplir el Responsable para la admisibilidad? → A: Tener asignación vigente
  y actuar dentro de su unidad.
- Q: ¿Qué cardinalidad exige la admisibilidad? → A: Exactamente un Responsable titular y una unidad
  principal.
- Q: ¿Un posible duplicado bloquea la admisibilidad? → A: Sí, hasta que el Evaluador resuelva el
  conflicto.
- Q: ¿Qué competencia institucional exige la aplicabilidad? → A: El problema debe corresponder al
  MIDAGRI o a su ámbito sectorial.
- Q: ¿Qué valor público exige la aplicabilidad? → A: Identificar beneficiarios y un resultado público
  esperado.
- Q: ¿Qué carácter innovador exige la aplicabilidad? → A: Una solución nueva o sustancialmente
  mejorada que requiera validación.
- Q: ¿Qué casos se excluyen de aplicabilidad? → A: Solo compra, mantenimiento, digitalización sin
  rediseño o cumplimiento rutinario.
- Q: ¿Cómo decide el Evaluador la aplicabilidad? → A: Con una lista estructurada que exige cumplir los
  criterios y registrar el motivo.
- Q: ¿Qué evidencia de origen exige la incorporación individual? → A: Fuente, fecha, Responsable,
  archivo o referencia y hash.
- Q: ¿Cómo se conserva un código heredado? → A: Como `Código de origen`, generando un código PIIP.
- Q: ¿Cómo se asigna el estado de negocio heredado? → A: El Evaluador selecciona un estado canónico
  con evidencia.
- Q: ¿Cómo se trata un registro existente con datos incompletos? → A: Permanece `PENDIENTE` y no se
  considera validado.
- Q: ¿Qué ocurre con un duplicado confirmado? → A: No se crea otro registro y su evidencia se vincula
  al existente.
- Q: ¿Qué ocurre con un código reutilizado o conflictivo? → A: Se bloquea hasta que el Evaluador
  registre una resolución documentada.
- Q: ¿Qué ocurre con una relación iniciativa-proyecto inválida? → A: Se bloquea la validación hasta
  corregirla.
- Q: ¿Cuántas correcciones se permiten antes de validar? → A: Ilimitadas y auditadas.
- Q: ¿Qué estados usa la incorporación? → A: `PENDIENTE`, `VALIDADO` y `RECHAZADO`, separados del
  estado de negocio.
- Q: ¿Qué auditoría exige la incorporación? → A: Fuente, actores, datos originales, cambios, errores,
  resolución, fechas y hash.
- Q: ¿Cómo se autoriza un reporte extraordinario? → A: Mediante solicitud documentada y aprobación
  de la Oficina de Modernización; lo genera un usuario con perfil `Evaluador`.
- Q: ¿Qué contenido incluye el reporte semestral? → A: Totales por tipo, estado, unidad, fuente, tipo
  de solución, producto y cierre.
- Q: ¿Qué indicadores incluye el reporte institucional? → A: Admisibilidad, aprobación, cierre y
  cumplimiento de ciclos, con numerador y denominador.
- Q: ¿Qué filtros admite un reporte configurable? → A: Periodo, tipo, estado, unidad, Responsable,
  fuente, tipo de solución y producto.
- Q: ¿En qué formatos se genera? → A: PDF oficial y XLSX de detalle.
- Q: ¿Quiénes pueden recibirlo? → A: Autoridades MIDAGRI, Oficina de Modernización y PCM-SGP cuando
  su remisión esté autorizada.
- Q: ¿Qué clasificación tiene? → A: `INTERNO` por defecto y `RESTRINGIDO` si incluye información con
  esa clasificación.
- Q: ¿Qué aprobación requiere antes de remitirse? → A: La Oficina de Modernización aprueba la versión
  y los destinatarios.
- Q: ¿Qué evidencia conserva la generación y remisión? → A: Parámetros, corte, generador, versión,
  hash, aprobación, destinatario, fecha y resultado.
- Q: ¿Cuánto tiempo se conservan los reportes y sus evidencias? → A: Durante el plazo de la tabla de
  retención documental vigente del MIDAGRI; mientras no esté confirmado, no hay eliminación
  automática, y toda disposición posterior es autorizada, auditada y aplicada al expediente completo.
- Q: ¿Qué perfil registra la validación y aprobación de prototipos? → A: `Evaluador`.
- Q: ¿Quiénes validan cada prototipo? → A: Al menos un usuario por perfil funcional involucrado y un
  actor sectorial cuando el recorrido aplique a su ámbito.
- Q: ¿Qué criterios se validan? → A: Recorrido completo, reglas, mensajes, accesibilidad y privacidad.
- Q: ¿Cuántas iteraciones se permiten? → A: Sin límite hasta resolver todos los hallazgos críticos y
  altos.
- Q: ¿Cómo se versiona un prototipo? → A: Con código, fecha, recorrido, cambios, estado y versión
  anterior.
- Q: ¿Qué evidencia exige cada validación? → A: Usuario, perfil, escenario, resultado, observaciones y
  aceptación.
- Q: ¿Qué estados usa un prototipo? → A: `BORRADOR`, `EN_VALIDACION`, `OBSERVADO`, `VALIDADO`,
  `APROBADO` y `RECHAZADO`.
- Q: ¿Qué separación de funciones exige la aprobación? → A: El aprobador no puede ser el autor ni el
  único validador.
- Q: ¿Qué ocurre con un cambio funcional o de accesibilidad posterior? → A: Crea una versión nueva y
  exige revalidación.
- Q: ¿Qué cobertura de dispositivos y accesibilidad exige cada recorrido? → A: Escritorio y móvil;
  teclado y lector de pantalla según los componentes e interacciones presentes.
- Q: ¿Qué alcance tiene la medición inicial? → A: Los ocho recorridos, agrupados por etapa de
  implementación.
- Q: ¿Qué métricas registra? → A: Éxito de tarea, tiempo mediano, errores críticos, satisfacción y
  accesibilidad.
- Q: ¿Qué método utiliza? → A: Pruebas moderadas con escenarios y prototipos versionados.
- Q: ¿Qué muestra utiliza? → A: Cinco usuarios por perfil como referencia; toda desviación se
  justifica y no puede omitir un perfil involucrado.
- Q: ¿Qué datos utiliza? → A: Datos sintéticos representativos sin información personal real.
- Q: ¿Quién ejecuta, coordina y aprueba la medición? → A: El equipo de experiencia de usuario ejecuta
  y calcula, un Evaluador coordina y otro Evaluador aprueba por la Oficina de Modernización.
- Q: ¿Cuándo se realiza la medición inicial? → A: Durante la validación y antes de aprobar el
  prototipo de cada recorrido.
- Q: ¿Qué evidencia conserva? → A: Versión, muestra, escenarios, resultados, cálculos, hallazgos,
  metas aplicables, aprobación y hash.
- Q: ¿Cuándo se repite? → A: Antes de liberar cada recorrido y después de todo cambio funcional o de
  accesibilidad que lo afecte.
- Q: ¿Qué metas se aprueban después de la medición inicial? → A: Por recorrido, éxito de tarea igual o
  mayor al 90 %, cero errores críticos, satisfacción igual o mayor a 4/5, ausencia de hallazgos
  críticos o altos de accesibilidad y, con línea base comparable, mejora mínima del 20 % en tiempo
  mediano; sin línea base comparable, la meta de tiempo parte de la medición inicial.
- Q: ¿Quién confirma la publicación documental y qué evento fija `fechaPublicacion`? → A: El
  Evaluador confirma la publicación de una versión con clasificación `PUBLICO` validada, y la fecha
  del servidor al confirmar fija `fechaPublicacion`.
- Q: ¿Cómo se define y gobierna la matriz cargo o función-perfil-unidad? → A: Mediante un catálogo
  configurable y versionado de funciones con relaciones múltiples a perfiles y unidades; cada
  combinación es aprobada por la autoridad que autoriza el perfil correspondiente y registrada por
  el administrador habilitado según las reglas vigentes de ese perfil.
- Q: ¿Cómo se cargan y gobiernan los catálogos Objetivo PEI y Actividad POI? → A: La oficina
  responsable de planeamiento aprueba el catálogo versionado y `GlobalAdmin` registra códigos,
  descripciones y vigencias; la carga inicial usa una semilla formalmente aprobada, sin sincronización
  externa.
- Q: ¿La matriz función-perfil-unidad relaciona tipos de unidad o unidades concretas? → A: Cada
  combinación relaciona exactamente una función, un perfil y una unidad concreta.
- Q: ¿Objetivo PEI y Actividad POI comparten una versión? → A: Se versionan de forma independiente,
  con aprobaciones, vigencias y semillas propias.
- Q: ¿Cómo se conservan los documentos formales que no pertenecen a una iniciativa o proyecto? → A:
  En un expediente institucional; cada serie documental pertenece de forma excluyente e inmutable a
  un registro de portafolio o a un expediente institucional.

### Session 2026-07-21 - Remediación previa a implementación

- Q: ¿Qué recorrido habilita las interfaces para crear proyectos derivados y directos? → A: El gate
  `REGISTRO` cubre el registro de iniciativas y la creación de ambos tipos de proyecto.
- Q: ¿Qué recorridos cubren suspensión y cancelación? → A: La suspensión pertenece a `SEGUIMIENTO` y
  la cancelación a `DECISION`.
- Q: ¿Las interfaces administrativas de seguridad, reportes y gestión de prototipos agregan nuevos
  gates? → A: No. Los ocho gates permanecen limitados a `REGISTRO`, `EVALUACION`, `DECISION`,
  `SEGUIMIENTO`, `APROBACION_PRODUCTO`, `CIERRE`, `CONSULTA_INSTITUCIONAL` y `CONSULTA_PUBLICA`.
- Q: ¿Qué ocurre si la unidad principal no tiene un prefijo de código formalmente aprobado? → A: La
  presentación o creación se rechaza; PIIP no infiere ni fabrica un prefijo.
- Q: ¿Cómo se controla el uso de datos sintéticos en mediciones? → A: Cada medición referencia una
  versión de un dataset sintético formalmente aprobado. Mientras no exista ese dataset, la medición y
  la aprobación del prototipo permanecen bloqueadas.
- Q: ¿Se implementa disposición o eliminación de reportes mientras no exista una tabla de retención
  aprobada? → A: No. La Fase 1 no expone endpoint, proceso ni acción de disposición; una capacidad
  posterior requiere especificación y aprobación independientes.

### Session 2026-07-21 - Remediación constitucional C1 y C2

- Q: ¿Quién administra la seguridad antimalware de los binarios? → A: OGTI, de forma exclusiva y
  fuera del alcance funcional de PIIP.
- Q: ¿Qué conserva PIIP sobre los documentos? → A: El binario en Oracle PIIP, metadatos, autor,
  fecha, versión, clasificación y hash SHA-256; no conserva estados, resultados, informes ni
  auditorías antimalware.
- Q: ¿El uso formal de un documento depende de un resultado antimalware en PIIP? → A: No. PIIP aplica
  únicamente sus reglas de negocio, clasificación, integridad, versionado y autorización documental.
- Q: ¿Cómo se crea el primer `GlobalAdmin`? → A: Exclusivamente mediante una semilla SQL manual,
  revisable, de ejecución única y fail-fast, ejecutada por un DBA autorizado de OGTI.
- Q: ¿Quién proporciona la identidad y autoriza la asignación inicial? → A: El administrador Keycloak
  de OGTI proporciona el `sub`; la Jefatura de la Oficina de Modernización autoriza mediante una
  aprobación de despliegue identificable.
- Q: ¿Qué valores usa la asignación inicial? → A: `codigoUnidad=MIDAGRI`,
  `nombreUnidad=Ministerio de Desarrollo Agrario y Riego`,
  `codigoFuncion=ADMINISTRADOR_PIIP` y `nombreFuncion=Administrador PIIP`; la semilla prevalida y
  reutiliza la unidad raíz existente.
- Q: ¿Qué auditoría exige la semilla? → A: `sub`, perfil, función, unidad, Jefatura autorizante,
  aprobación de despliegue, DBA ejecutor, fecha, operación y resultado.
- Q: ¿Qué ocurre si ya existió un `GlobalAdmin`? → A: La semilla aborta sin cambios; no existe
  comando, endpoint, cliente OIDC temporal ni mecanismo alternativo de bootstrap.

### Session 2026-07-21 - Remediación de autenticación

- Q: ¿Qué scope o claim del JWT debe exigir el backend además de `issuer`, `audience`, firma y
  vigencia? → A: No se exige un scope adicional. El backend valida los claims estándar necesarios para
  comprobar `issuer`, `audience` y vigencia, además de la firma del token.

## Actors and Authorization *(mandatory)*

| Actor or role | Action | Functional permission | Organizational scope | Decision or registration responsibility |
|---|---|---|---|---|
| Responsable | Registrar y mantener iniciativas y proyectos derivados, incluida la incorporación individual de información existente; proponer la clasificación inicial de documentos y metadatos; presentar el producto final | `Responsable` | Unidades asignadas explícitamente | Crea proyectos derivados y registra información; después de presentar una iniciativa solo edita sus campos editables durante la única subsanación abierta; no registra proyectos directos ni reemplaza decisiones formales |
| Evaluador de la Oficina de Modernización | Evaluar iniciativas, validar la incorporación individual de información existente y la clasificación inicial de documentos y metadatos, registrar proyectos directos y decisiones formalizadas, y cerrar proyectos | `Evaluador` | Ámbito institucional autorizado | La Oficina de Modernización autoriza su asignación y `GlobalAdmin` la registra con documento formal; decide admisibilidad y aplicabilidad; valida incorporaciones y clasificaciones iniciales; registra proyectos directos, decisiones y reclasificaciones autorizadas por la Autoridad solo con documento formal; decide y registra el cierre |
| Máxima autoridad administrativa | Aprobar o archivar iniciativas; autorizar y registrar proyectos directos, reclasificaciones de privacidad y nuevas asignaciones `GlobalAdmin`; aprobar o no aprobar productos; decidir cancelaciones | `Autoridad` | Ámbito de autoridad formal | Su perfil refleja una designación formal institucional vigente registrada por `GlobalAdmin`; decide y puede registrar proyectos directos y sus decisiones, o permitir que el Evaluador los registre con el documento formal; toda nueva asignación `GlobalAdmin` requiere su decisión formal |
| Administrador funcional | Gestionar participantes, identidades funcionales, perfiles y ámbitos | `GlobalAdmin` en toda la institución y `UnidadAdmin` en sus unidades autorizadas | Institucional para `GlobalAdmin`; unidades autorizadas para `UnidadAdmin` | `GlobalAdmin` registra `Autoridad` según designación formal institucional vigente y `Evaluador` con autorización formal de la Oficina de Modernización; administra `Responsable` y `Consulta` institucionalmente y `UnidadAdmin` dentro de su ámbito; solo `GlobalAdmin` administra asignaciones `UnidadAdmin`; registra una nueva asignación `GlobalAdmin` únicamente con decisión formal de la Autoridad; confirma revocaciones en su ámbito; `UnidadAdmin` sustituye al Responsable titular dentro de su ámbito; gestiona identidades según la matriz aprobada; no decide otras transiciones sin permiso canónico independiente |
| Unidad administradora | Asistir la incorporación individual de información existente y registrar una suspensión sustentada | `UnidadAdmin` | Unidades asignadas explícitamente | Asiste sin sustituir al Responsable ni al Evaluador; decide y registra la suspensión |
| Usuario institucional | Buscar, filtrar y consultar información autorizada | `Consulta` u otro perfil con permiso de consulta | Unidades asignadas explícitamente | No decide ni modifica información |
| Participante sectorial autorizado | Mantener iniciativas y proyectos de su entidad | `Responsable` | Unidades de su entidad asignadas explícitamente | Registra información dentro de su asignación efectiva; no decide transiciones reservadas a otros perfiles |
| Ciudadano o representante externo | Consultar información pública anónimamente | Acceso público sin perfil institucional | `Tipo de registro`, `Código`, `Nombre de iniciativa o proyecto`, `Estado`, tipo documental, título sin datos personales, versión, formato y fecha de publicación | No decide ni modifica información |
| Responsable de reportes de la Oficina de Modernización | Generar reportes semestrales y requeridos | `Evaluador` | Ámbito institucional autorizado | Genera y deja evidencia; no altera decisiones del portafolio |
| Oficina responsable de planeamiento | Aprobar los catálogos versionados de Objetivo PEI y Actividad POI | Autoridad funcional de planeamiento; no requiere un perfil PIIP adicional para emitir la aprobación formal | Ámbito institucional | Aprueba códigos, descripciones y vigencias; `GlobalAdmin` registra la versión aprobada en PIIP |
| Usuario validador de negocio | Validar prototipos PIIP | Perfil funcional correspondiente al recorrido | Recorridos correspondientes a su función | Ejecuta escenarios y comunica resultados; un `Evaluador` registra la evidencia |
| Aprobador de prototipos | Aprobar formalmente prototipos PIIP | `Evaluador` de la Oficina de Modernización | Todos los recorridos PIIP | Registra la aprobación; debe ser distinto del autor y no puede ser el único validador |
| Equipo de experiencia de usuario | Ejecutar pruebas moderadas y calcular métricas | No requiere un perfil PIIP adicional; usa prototipos y datos sintéticos autorizados | Recorridos de la etapa de implementación evaluada | Aplica escenarios, calcula resultados y entrega evidencia al Evaluador coordinador; no aprueba la medición |
| OGTI | Administrar Keycloak, Oracle y los controles técnicos de seguridad de binarios | Responsabilidad técnica externa a los perfiles PIIP | Infraestructura institucional | Administra identidad técnica, credenciales, plataforma y seguridad antimalware; no decide permisos funcionales ordinarios |
| DBA autorizado de OGTI | Ejecutar una sola vez la semilla inicial `GlobalAdmin` | Procedimiento SQL fundacional aprobado | Unidad raíz `MIDAGRI` | Usa el `sub` proporcionado por el administrador Keycloak, registra la auditoría mínima y no administra asignaciones ordinarias |
| PCM-SGP | Recibir la información institucional que corresponda | Receptor del reporte autorizado | Información aprobada para remisión | No opera el portafolio PIIP |

La autorización efectiva combina el permiso funcional con el ámbito organizacional. Una persona
puede tener varios perfiles y unidades, pero cada operación debe evaluarse contra el perfil y el
ámbito efectivos seleccionados para esa actuación. Una asignación no concede acceso implícito a
unidades descendientes; cada unidad debe estar asignada expresamente. Cada operación utiliza una
sola asignación efectiva y no combina permisos de perfiles o unidades diferentes.
La revocación de una asignación funcional se hace efectiva inmediatamente después de confirmarse;
desde ese momento, ninguna operación puede autorizarse con esa asignación.
`GlobalAdmin` puede confirmar una revocación en el ámbito institucional y `UnidadAdmin` únicamente
dentro de su ámbito autorizado.
Cada asignación funcional tiene una fecha de inicio obligatoria y puede tener una fecha de fin; solo
autoriza operaciones durante su periodo de vigencia y mientras no haya sido revocada.
Toda operación sensible debe revalidar la asignación efectiva inmediatamente antes de aplicar el
cambio; si fue revocada, venció o quedó inactiva, la operación se rechaza y audita aunque hubiera
comenzado autorizada.
`GlobalAdmin` puede crear, desactivar y reactivar usuarios institucionalmente; `UnidadAdmin` solo
dentro de sus unidades autorizadas. La activación usa correo de Keycloak y PIIP nunca gestiona la
contraseña. La desactivación es inmediata en Keycloak y PIIP, pero conserva usuario, asignaciones e
historial; una reactivación no restaura asignaciones vencidas o revocadas.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar una iniciativa (Priority: P1)

Un Responsable registra y mantiene una iniciativa con sus datos oficiales, problema público,
solución propuesta cuando esté disponible, participantes, unidades responsables y documentos de
sustento.

**Why this priority**: Es el punto de entrada ordinario del portafolio y reemplaza el registro
transitorio disperso.
**Independent Test**: Registrar una iniciativa completa y comprobar que obtiene código propio,
estado `PRESENTADO`, responsables, documentos e historial identificables.

**Acceptance Scenarios**:

1. **Given** un Responsable autorizado para una unidad, **When** registra una iniciativa con los
   campos ingresables exigidos para la presentación completos, **Then** la iniciativa queda en
   `PRESENTADO`, recibe automáticamente su código propio y fecha de presentación, y conserva vacío
   `Código de origen`.
2. **Given** un registro con un campo o documento obligatorio ausente, **When** intenta presentarlo,
   **Then** no se completa la presentación y se identifican los requisitos pendientes.
3. **Given** un actor fuera del ámbito de la unidad, **When** intenta modificar la iniciativa,
   **Then** la operación se deniega y queda auditada.
4. **Given** información existente que debe incorporarse individualmente, **When** el Responsable
   registra la información con asistencia de `UnidadAdmin`, **Then** el Evaluador debe validarla
   antes de que se considere un registro ordinario validado.
5. **Given** una iniciativa con varias unidades responsables, **When** se presenta, **Then**
   exactamente una de ellas está identificada como unidad principal.
6. **Given** un registro de información existente con un posible duplicado, **When** se intenta
   validar, **Then** la validación se bloquea hasta que el Evaluador resuelva el conflicto y quede
   evidencia de la resolución.
7. **Given** que una iniciativa declara `Sí` en `Componente digital`, **When** se intenta presentar
   sin describir ese componente, **Then** la presentación se rechaza y se identifica el dato
   pendiente.
8. **Given** que una iniciativa selecciona `OTROS` en `Fuente u origen`, **When** se intenta
   presentar sin describir la fuente, **Then** la presentación se rechaza y se identifica el dato
   pendiente.
9. **Given** una iniciativa con participantes adicionales, **When** se presenta, **Then** identifica
   exactamente un Responsable titular y los demás participantes no comparten esa titularidad.
10. **Given** una iniciativa que se presenta, **When** se validan sus referencias de planeamiento,
    **Then** contiene exactamente un `Objetivo PEI` y una `Actividad POI`.
11. **Given** una iniciativa con el problema público descrito y sin solución propuesta, **When** se
    presenta, **Then** la ausencia de solución no bloquea la presentación.
12. **Given** una iniciativa con Responsable titular y sin participantes adicionales, **When** se
    presenta, **Then** la ausencia de participantes no bloquea la presentación.
13. **Given** un nombre, problema, solución, detalle de `OTROS`, detalle de componente digital o nota,
    **When** supera su longitud máxima, **Then** la presentación se rechaza e identifica el límite.
14. **Given** contenido textual con espacios en los extremos, **When** se registra, **Then** se
    conserva recortado; si contiene únicamente espacios, se rechaza como vacío.
15. **Given** una referencia PEI o POI retirada del catálogo, **When** ya está asociada a un registro,
    **Then** se conserva históricamente, pero no aparece en nuevas selecciones.
16. **Given** una subsanación abierta, **When** el Responsable corrige la iniciativa, **Then** solo
    puede editar los campos 5 al 12, 22 y 23; `Tipo de registro` permanece inmutable.
17. **Given** información existente, **When** el Responsable inicia su incorporación individual,
    **Then** registra fuente, fecha, archivo o referencia, hash y conserva el código heredado como
    `Código de origen`, mientras PIIP genera su código propio.
18. **Given** un registro incompleto, con código conflictivo o relación inválida, **When** se intenta
    validar, **Then** permanece `PENDIENTE` y no se considera un registro ordinario validado.
19. **Given** un posible duplicado, **When** el Evaluador confirma que corresponde a un registro
    existente, **Then** no se crea otro registro y la evidencia se vincula al existente.
20. **Given** un registro pendiente, **When** se corrige antes de validar, **Then** admite las
    correcciones necesarias y cada una queda auditada.
21. **Given** información existente completa y consistente, **When** el Evaluador selecciona con
    evidencia su estado canónico y valida la incorporación, **Then** pasa a `VALIDADO` sin confundir
    ese estado con el estado de negocio.
22. **Given** una incorporación que no puede validarse, **When** el Evaluador registra el motivo,
    **Then** pasa a `RECHAZADO` y conserva fuente, datos originales, cambios, errores y resolución.

---

### User Story 2 - Evaluar y decidir una iniciativa (Priority: P1)

El Evaluador revisa requisitos y documentos, registra observaciones y emite una opinión técnica.
La Autoridad adopta la decisión formal de aprobar o archivar; el Evaluador puede registrar el
resultado únicamente cuando dispone del documento formal.

**Why this priority**: Garantiza que la recomendación y la decisión tengan responsables y sustento
diferenciados.
**Independent Test**: Evaluar una iniciativa presentada, emitir opinión técnica, registrar una
decisión formal y comprobar el estado y la auditoría resultantes.

**Acceptance Scenarios**:

1. **Given** una iniciativa `PRESENTADO` que mantiene incumplimientos formales después de agotar su
   única subsanación, **When** vence el plazo registrado por el Evaluador, **Then** pasa a
   `NO_ADMISIBLE` con observación obligatoria.
2. **Given** una iniciativa `PRESENTADO` que no cumple todos los criterios de competencia, valor
   público y carácter innovador, o incurre en una exclusión, **When** el Evaluador completa la lista
   estructurada, **Then** pasa a `NO_APLICABLE` con el motivo obligatorio.
3. **Given** una opinión técnica emitida y una decisión formal de la Autoridad, **When** la
   Autoridad o el Evaluador autorizado registra el resultado, **Then** la iniciativa pasa a
   `INICIATIVA_APROBADA` o `INICIATIVA_ARCHIVADA` según el documento.
4. **Given** que falta el documento formal, **When** el Evaluador intenta registrar aprobación o
   archivo, **Then** la transición es rechazada.
5. **Given** una iniciativa `PRESENTADO` con incumplimientos formales, **When** el Evaluador abre la
   subsanación, **Then** registra un único plazo y la iniciativa permanece en `PRESENTADO` hasta su
   atención o vencimiento.
6. **Given** una iniciativa `PRESENTADO`, **When** el Responsable intenta editar los campos oficiales
   sin una subsanación abierta, **Then** la operación se rechaza y los datos presentados permanecen
   sin cambios.
7. **Given** una iniciativa cuyo `Tipo de solución` es `POR_DEFINIR`, **When** el Evaluador emite la
   opinión técnica o la Autoridad adopta una decisión, **Then** ese valor no bloquea la actuación ni
   exige un plazo de resolución.
8. **Given** una opinión técnica registrada, **When** el Evaluador corrige su contenido, **Then** se
   crea una nueva versión y se conservan las anteriores.
9. **Given** una iniciativa con campos o catálogos incompletos, ficha no apta, Responsable sin
   asignación vigente, cardinalidad inválida o posible duplicado sin resolver, **When** el Evaluador
   revisa la admisibilidad, **Then** abre la única subsanación y registra los incumplimientos.
10. **Given** una iniciativa con todos los requisitos formales atendidos, **When** el Evaluador
    confirma la admisibilidad, **Then** continúa a la evaluación de aplicabilidad.
11. **Given** un problema dentro del ámbito MIDAGRI o sectorial, con beneficiarios, resultado público
    esperado y una solución nueva o sustancialmente mejorada que requiere validación, **When** no
    incurre en exclusiones, **Then** la lista de aplicabilidad resulta favorable.

---

### User Story 3 - Crear un proyecto derivado o directo (Priority: P1)

Un Responsable autorizado crea un único proyecto nuevo y vinculado a una iniciativa aprobada. La
Autoridad o el Evaluador con el documento formal registra un proyecto directo heredado o excepcional.

**Why this priority**: Preserva la trazabilidad sin permitir que el ingreso directo omita el
proceso ordinario.
**Independent Test**: Crear ambos tipos de proyecto y comprobar códigos, origen, vínculo, estado,
evidencia habilitante y rechazo de ingresos directos no autorizados.

**Acceptance Scenarios**:

1. **Given** una iniciativa `INICIATIVA_APROBADA`, **When** un Responsable autorizado crea el
   proyecto y aún no existe otro derivado, **Then** nace un registro distinto en
   `PROYECTO_EJECUCION`, con código propio y vínculo inmutable con la iniciativa.
2. **Given** un proyecto iniciado antes de PIIP con acto formal y evidencia de ejecución, **When** la
   Autoridad o el Evaluador con el documento formal registra los campos exigidos, **Then** puede
   incorporarse como proyecto heredado directamente en `PROYECTO_EJECUCION`.
3. **Given** un proyecto excepcional cuya incorporación fue autorizada formalmente por la Autoridad,
   **When** la Autoridad o el Evaluador con el documento registra los campos exigidos, **Then** puede
   iniciar directamente en `PROYECTO_EJECUCION`.
4. **Given** una nueva propuesta sin iniciativa aprobada ni excepción formal, **When** se intenta
   registrar como proyecto directo, **Then** la operación es rechazada y auditada.
5. **Given** una iniciativa aprobada que ya originó su proyecto derivado, **When** se intenta crear
   otro proyecto derivado, **Then** la operación es rechazada sin alterar el vínculo existente.
6. **Given** una iniciativa aprobada, **When** el Responsable prepara su proyecto derivado, **Then**
   el código de origen y tipo de solución se copian automáticamente, mientras nombre, PEI, POI,
   unidades y Responsable titular pueden definirse o ajustarse antes de confirmar.
7. **Given** un proyecto derivado o directo listo para confirmar, **When** se valida su creación,
   **Then** contiene los campos 1 al 13 y 22, `Nota` es opcional y `Fecha de inicio` coincide con el
   documento formal.
8. **Given** un actor distinto de la Autoridad o del Evaluador con documento formal, **When** intenta
   registrar un proyecto directo, **Then** la operación se rechaza y queda auditada.

---

### User Story 4 - Acompañar la ejecución y presentar el producto (Priority: P1)

El Responsable del proyecto registra planificación, ciclos, avances, dificultades, productos
parciales y evidencias, y presenta el producto final con sus documentos de sustento.

**Why this priority**: Permite acompañamiento continuo y evita reconstruir la ejecución al final.
**Independent Test**: Mantener un proyecto durante varios ciclos, adjuntar evidencias y presentar
un producto final completo para decisión.

**Acceptance Scenarios**:

1. **Given** un proyecto `PROYECTO_EJECUCION`, **When** su Responsable registra el ciclo quincenal
   obligatorio, **Then** incluye objetivos, actividades, avance, dificultades, próximas acciones y
   evidencias relacionadas, fechadas y trazables.
2. **Given** un archivo mayor de 100 MB, con formato no permitido o no apto para evidencia formal,
   **When** se intenta adjuntar, **Then** se rechaza su uso como evidencia.
3. **Given** la información y documentos de sustento requeridos, **When** el Responsable presenta
   el producto final, **Then** queda disponible para decisión de la Autoridad sin alterar aún el
   estado del proyecto.
4. **Given** un proyecto `PROYECTO_EJECUCION`, **When** `UnidadAdmin` registra una suspensión con
   evidencia y observación, **Then** pasa a `SUSPENDIDO` y no admite una transición saliente.
5. **Given** un proyecto `PROYECTO_EJECUCION`, **When** la Autoridad decide cancelarlo y la decisión
   se registra con documento y observación, **Then** pasa a `CANCELADO` sin transición saliente.
6. **Given** un ciclo quincenal cerrado, **When** el Responsable registra una corrección, **Then** se
   crea una nueva versión trazable y la versión cerrada permanece sin cambios.
7. **Given** un proyecto en `PROYECTO_EJECUCION`, **When** el Responsable agrega o retira un
   participante, **Then** el cambio se aplica y queda auditado sin borrar el historial anterior.
8. **Given** un proyecto sin documentación de gestión opcional, **When** registra sus ciclos y
   evidencias obligatorias, **Then** la ausencia de esa colección no bloquea la ejecución.
9. **Given** una cancelación confirmada, **When** el proyecto pasa a `CANCELADO`, **Then** su fecha de
   cierre se genera automáticamente.
10. **Given** un proyecto en `PROYECTO_EJECUCION`, **When** el Responsable modifica información,
     **Then** solo puede editar los campos 17, 19 y 23 y cada cambio queda auditado.

---

### User Story 5 - Decidir el producto y cerrar el proyecto (Priority: P1)

La Autoridad aprueba o no aprueba el producto final mediante decisión formal. Posteriormente, el
Evaluador registra el informe final, resultados, aprendizajes, conclusión y observación para cerrar
el proyecto.

**Why this priority**: Completa el ciclo de vida con un resultado formal y una conclusión trazable.
**Independent Test**: Ejecutar las dos alternativas de decisión y comprobar que ambas permiten el
cierre administrativo y terminan en `FINALIZADO`.

**Acceptance Scenarios**:

1. **Given** un producto final presentado, **When** la Autoridad lo aprueba con documento formal,
   **Then** el proyecto pasa a `PRODUCTO_APROBADO`.
2. **Given** un producto final presentado, **When** la Autoridad no lo aprueba, **Then** el proyecto
   pasa a `PRODUCTO_NO_APROBADO` únicamente con observación y evidencia obligatorias.
3. **Given** un proyecto en cualquiera de los dos estados de producto y un cierre completo,
   **When** el Evaluador registra la observación de cierre y lo completa, **Then** el proyecto pasa
   a `FINALIZADO`.
4. **Given** que falta el informe final, resultados, aprendizajes o conclusión, **When** se intenta
   cerrar el proyecto, **Then** la transición es rechazada.
5. **Given** una decisión formal de producto o cancelación, **When** el Evaluador registra el
   resultado, **Then** solo se acepta si el documento corresponde a la decisión de la Autoridad.
6. **Given** dos decisiones incompatibles registradas simultáneamente, **When** una transición se
   confirma primero, **Then** esa transición prevalece y la otra se rechaza sin sobrescribir el
   historial.
7. **Given** un producto aprobado, **When** se registra la decisión, **Then** contiene el documento
   formal inmutable y un tipo de producto del catálogo canónico.
8. **Given** resultados clave registrados por el Responsable, **When** el Evaluador los valida y
   registra el informe final completo, **Then** el proyecto puede pasar a `FINALIZADO` y recibe
   automáticamente su fecha de cierre.

---

### User Story 6 - Administrar acceso organizacional (Priority: P1)

El Administrador funcional mantiene participantes y asignaciones de perfil por cargo o función y
unidad organizacional, incluidas múltiples asignaciones para una misma persona.

**Why this priority**: El aislamiento por ámbito protege la información y las decisiones.
**Independent Test**: Asignar a una persona perfiles distintos en dos unidades y comprobar que sus
acciones y consultas cambian según el perfil y ámbito efectivos.

**Acceptance Scenarios**:

1. **Given** una matriz aprobada, **When** se asigna un perfil y unidad a una persona, **Then** solo
   obtiene las acciones e información autorizadas por esa combinación.
2. **Given** una persona con varias asignaciones, **When** opera en un ámbito específico, **Then**
   selecciona una sola asignación efectiva y no hereda ni combina permisos de otro perfil, ámbito o
   unidad descendiente no asignada explícitamente.
3. **Given** un cambio de asignación, **When** se activa, modifica, revoca o deniega, **Then** queda
   evidencia inmutable del actor, momento, alcance y cambio.
4. **Given** una asignación funcional vigente, **When** se confirma su revocación, **Then** deja de
   autorizar operaciones inmediatamente, incluso si el actor ya había iniciado una sesión.
5. **Given** una asignación con fecha de inicio futura o fecha de fin vencida, **When** el actor
   intenta utilizarla, **Then** la operación se deniega y queda auditada.
6. **Given** una iniciativa o proyecto dentro del ámbito de `UnidadAdmin`, **When** sustituye al
   Responsable titular y confirma la operación, **Then** el nuevo titular asume inmediatamente, el
   anterior deja de serlo, queda exactamente un titular y se audita el cambio.
7. **Given** un actor distinto de `GlobalAdmin`, **When** intenta asignar, modificar o revocar un
   perfil `UnidadAdmin`, **Then** la operación se deniega y queda auditada.
8. **Given** una decisión formal de la Autoridad para una nueva asignación `GlobalAdmin`, **When** un
   `GlobalAdmin` registra la asignación con el documento correspondiente, **Then** queda vigente y
   auditada; sin ese documento, la operación se rechaza.
9. **Given** que nunca existió un `GlobalAdmin`, **When** el DBA autorizado ejecuta la semilla con el
   `sub` proporcionado por OGTI y la aprobación de despliegue de la Jefatura de Modernización,
   **Then** reutiliza la unidad raíz, crea función, combinación y asignación iniciales y registra la
   auditoría mínima.
10. **Given** una suplencia funcional autorizada, **When** se registra, **Then** crea una asignación
    temporal distinta con fecha de inicio y fin obligatorias, sin transferir las credenciales del
    titular.
11. **Given** una solicitud de suplencia, **When** la autoriza un actor distinto de quien autoriza la
    asignación permanente del perfil, **Then** la operación se rechaza y queda auditada.
12. **Given** una suplencia dentro de su periodo, **When** el titular intenta usar la asignación del
    mismo perfil y unidad, **Then** la operación se deniega; al finalizar la suplencia, la asignación
    del titular se reactiva si continúa vigente y no fue revocada.
13. **Given** una suplencia registrada para un perfil y unidad, **When** se intenta registrar otra
    cuyo periodo se superpone para la misma asignación, **Then** la nueva suplencia se rechaza y queda
    auditada.
14. **Given** una suplencia vigente, **When** la misma autoridad que la autorizó confirma su
    terminación anticipada, **Then** la suplencia cesa inmediatamente y la asignación del titular se
    reactiva solo si continúa vigente y no fue revocada.
15. **Given** una operación sensible iniciada con una asignación vigente, **When** la asignación se
    revoca, vence o queda inactiva antes de aplicar el cambio, **Then** la operación se rechaza y queda
    auditada.
16. **Given** una asignación `Responsable` o `Consulta`, **When** `GlobalAdmin` la administra en el
    ámbito institucional o `UnidadAdmin` dentro de su ámbito, **Then** la operación se permite y
    audita; fuera de esos límites, se rechaza.
17. **Given** una autorización formal de la Oficina de Modernización, **When** `GlobalAdmin` registra
    una asignación `Evaluador` con el documento correspondiente, **Then** la asignación queda vigente
    y auditada; sin autorización o documento, se rechaza.
18. **Given** una designación formal institucional vigente, **When** `GlobalAdmin` registra, modifica
    o revoca el perfil `Autoridad` conforme al documento, **Then** la asignación refleja su vigencia y
    queda auditada; sin designación, se rechaza.
19. **Given** una persona de una unidad autorizada, **When** `UnidadAdmin` crea su usuario, **Then** la
    identidad se aprovisiona y recibe la activación por correo de Keycloak sin que PIIP gestione su
    contraseña.
20. **Given** una persona fuera del ámbito de `UnidadAdmin`, **When** intenta crear, desactivar o
    reactivar su usuario, **Then** la operación se rechaza y queda auditada.
21. **Given** un usuario activo, **When** la autoridad administradora confirma su desactivación,
    **Then** se bloquea inmediatamente en Keycloak y PIIP y se conservan usuario, asignaciones e
    historial.
22. **Given** un usuario desactivado con asignaciones vencidas o revocadas, **When** la misma autoridad
    lo reactiva, **Then** recupera acceso únicamente mediante asignaciones que continúan vigentes.
23. **Given** una decisión formal de la Autoridad, **When** otro `GlobalAdmin` modifica o revoca una
    asignación `GlobalAdmin`, **Then** el cambio se aplica y audita; sin decisión o autor distinto, se
    rechaza.
24. **Given** que solo queda un `GlobalAdmin` activo, **When** se intenta revocarlo sin reemplazo
    designado, **Then** la operación se rechaza.
25. **Given** un cambio de unidad de una persona, **When** se confirma, **Then** se cierran las
    asignaciones anteriores y se crean explícitamente las nuevas sin trasladar permisos.
26. **Given** que el identificador de Keycloak o correo institucional ya pertenece a una identidad,
     **When** se intenta crear otra, **Then** la creación se bloquea hasta resolver el conflicto.
27. **Given** que ya existe o existió una asignación `GlobalAdmin`, **When** se intenta ejecutar la
    semilla inicial, **Then** aborta sin cambios y conserva el mecanismo administrativo ordinario como
    única vía para asignaciones posteriores.

---

### User Story 7 - Consultar el portafolio institucional y público (Priority: P2)

Los usuarios institucionales consultan el portafolio dentro de su ámbito; la ciudadanía consulta
anónimamente y exclusivamente `Tipo de registro`, `Código`, `Nombre de iniciativa o proyecto`,
`Estado`, tipo documental, título sin datos personales, versión, formato y fecha de publicación.

**Why this priority**: Proporciona seguimiento institucional y transparencia sin exponer datos
restringidos.
**Independent Test**: Buscar el mismo registro con un usuario institucional y desde la consulta
pública, y comprobar las diferencias de ámbito, campos y documentos visibles.

**Acceptance Scenarios**:

1. **Given** un usuario institucional autorizado, **When** filtra por estado, responsable o unidad,
   **Then** obtiene únicamente registros y documentos autorizados de su ámbito.
2. **Given** un registro con campos públicos y restringidos, **When** se consulta públicamente,
   **Then** solo se muestran `Tipo de registro`, `Código`, `Nombre de iniciativa o proyecto`,
   `Estado`, tipo documental, título sin datos personales, versión, formato y fecha de publicación.
3. **Given** una consulta pública, **When** se intenta abrir o descargar un documento, **Then** el
   contenido no se muestra ni se descarga.
4. **Given** un metadato documental que contiene datos personales, **When** se consulta
   públicamente, **Then** ese metadato no se muestra.
5. **Given** una decisión formal de reclasificación emitida por la Autoridad, **When** el Evaluador
   registra el cambio, **Then** la nueva clasificación se aplica y queda auditada sin alterar el
   historial anterior.
6. **Given** un documento, **When** un usuario institucional autorizado lo consulta, **Then** el
   contenido se muestra solo si su ámbito y clasificación lo permiten; nunca se expone en la consulta
   pública.
7. **Given** un documento con clasificación pendiente o no validada, **When** se intenta consultar,
   **Then** solo el Responsable cargador y el Evaluador pueden acceder y no puede usarse como
   evidencia formal.
8. **Given** una reclasificación más restrictiva registrada por el Evaluador, **When** se confirma,
   **Then** bloquea inmediatamente los accesos futuros incompatibles y conserva las auditorías de
   accesos anteriores.

---

### User Story 8 - Consolidar y reportar información sectorial (Priority: P2)

La Oficina de Modernización consolida el portafolio interno y sectorial, mantiene una configuración
oficial del reporte semestral y genera reportes configurables cuando son requeridos. Los
participantes sectoriales mantienen su propio ámbito.

**Why this priority**: Permite seguimiento institucional y atención de requerimientos de PCM-SGP.
**Independent Test**: Registrar información desde dos ámbitos sectoriales y generar un reporte que
respete filtros, clasificación y alcance autorizados.

**Acceptance Scenarios**:

1. **Given** participantes sectoriales con perfil `Responsable` y unidades explícitamente asignadas,
   **When** mantienen sus registros, **Then** cada uno opera solo en las unidades de su entidad que
   tiene asignadas y la Oficina obtiene la visión consolidada autorizada.
2. **Given** el semestre enero-junio o julio-diciembre, **When** la Oficina genera el reporte oficial
   mediante un usuario con perfil `Evaluador` y corte al 30/06 o 31/12 respectivamente, **Then**
   incluye totales e indicadores con sus numeradores y denominadores, y deja evidencia de generación.
3. **Given** una solicitud documentada y aprobada por la Oficina de Modernización, **When** el
   Evaluador configura periodo, tipo, estado, unidad, Responsable, fuente, tipo de solución o
   producto, **Then** genera el reporte extraordinario en PDF oficial y XLSX de detalle con las mismas
   reglas de alcance y privacidad.
4. **Given** un usuario sin perfil `Evaluador`, **When** intenta generar un reporte institucional,
   **Then** la operación se deniega y queda auditada.
5. **Given** un reporte listo para remisión, **When** la Oficina de Modernización aprueba su versión y
   destinatarios, **Then** puede remitirse a autoridades MIDAGRI, a la propia Oficina o a PCM-SGP si
   está autorizado, conservando la evidencia completa y el resultado de la remisión.
6. **Given** un reporte que contiene información `RESTRINGIDO`, **When** se genera o remite, **Then**
   el reporte adopta esa clasificación y solo queda disponible para destinatarios autorizados.
7. **Given** que el plazo de retención documental no está confirmado, **When** un reporte o cualquiera
   de sus evidencias alcanza una antigüedad determinada, **Then** no se elimina automáticamente; una
   disposición posterior exige autorización, auditoría y tratamiento del expediente completo.

---

### User Story 9 - Validar prototipos PIIP (Priority: P1)

Los usuarios representativos de negocio validan prototipos específicos para cada recorrido crítico
y la Oficina de Modernización los aprueba antes de que sus interfaces se consideren listas para
implementación.

**Why this priority**: Evita implementar recorridos no validados para los roles y estados reales.
**Independent Test**: Comprobar que cada recorrido crítico cuenta con prototipo PIIP, resultados de
validación, aprobación y versión identificable.

**Acceptance Scenarios**:

1. **Given** un prototipo nuevo de un recorrido crítico, **When** usuarios representativos lo
   validan y la Oficina de Modernización acepta los criterios, **Then** un Evaluador distinto del
   autor y que no sea el único validador registra la evidencia de la versión aprobada.
2. **Given** un prototipo existente o genérico, **When** no tiene aprobación específica de PIIP,
   **Then** no puede utilizarse como evidencia de aprobación.
3. **Given** un prototipo con hallazgos críticos o altos, **When** concluye una iteración, **Then**
   queda `OBSERVADO` y se realizan tantas iteraciones como sean necesarias hasta resolverlos.
4. **Given** una validación, **When** un usuario representativo ejecuta su escenario, **Then** queda
   evidencia de usuario, perfil, escenario, resultado, observaciones y aceptación.
5. **Given** un prototipo `APROBADO`, **When** cambia una función o una condición de accesibilidad,
   **Then** se crea otra versión, la anterior queda como historial y la nueva requiere validación y
   aprobación.
6. **Given** cualquiera de los ocho recorridos funcionales, **When** se valida su prototipo, **Then**
   se cubren escritorio y móvil y se comprueba teclado y lector de pantalla en todos los componentes e
   interacciones donde resulten aplicables.
7. **Given** los recorridos de una etapa de implementación, **When** se realiza la medición inicial
   antes de aprobar sus prototipos, **Then** el equipo de experiencia de usuario aplica pruebas
   moderadas con datos sintéticos y registra las métricas aprobadas.
8. **Given** una muestra objetivo de cinco usuarios por perfil, **When** no puede alcanzarse, **Then**
   la desviación queda justificada sin omitir ninguno de los perfiles involucrados.
9. **Given** resultados calculados por el equipo de experiencia de usuario, **When** el Evaluador
   coordinador los presenta, **Then** otro Evaluador los aprueba por la Oficina de Modernización y se
   conserva la evidencia completa.
10. **Given** un recorrido próximo a liberarse o afectado por un cambio funcional o de accesibilidad,
    **When** se evalúa su preparación, **Then** se repite la medición antes de liberarlo.
11. **Given** una medición inicial aprobada, **When** la Oficina de Modernización define las metas del
    recorrido, **Then** aprueba y versiona la matriz antes de iniciar su implementación.
12. **Given** una línea base comparable de tiempo mediano, **When** se aprueba la meta, **Then** exige
    una mejora mínima del 20 %; si no existe, la meta se fija a partir de la medición inicial.
13. **Given** un error crítico o un hallazgo crítico o alto de accesibilidad, **When** se evalúa una
    liberación, **Then** queda bloqueada; una excepción solo puede afectar las demás métricas y exige
    justificación y aprobación documentadas.

## Business Rules and State Impact *(mandatory)*

- **BR-001**: Toda iniciativa y proyecto debe contemplar los 23 campos oficiales; su obligatoriedad,
  editabilidad, privacidad y responsable dependen del tipo y etapa según una matriz aprobada.
- **BR-002**: Una iniciativa nueva inicia exclusivamente en `PRESENTADO`.
- **BR-003**: `NO_ADMISIBLE`, `NO_APLICABLE` e `INICIATIVA_ARCHIVADA` son terminales.
- **BR-004**: Una iniciativa aprobada conserva su registro y puede originar un único registro de
  proyecto, con códigos e historiales propios y un vínculo inmutable.
- **BR-005**: Un proyecto directo requiere origen heredado o excepción formal, evidencia habilitante
  y los campos 1 al 13 y 22 completos; `Nota` es opcional. Un proyecto heredado debe haber iniciado
  antes de PIIP y contar con acto formal y evidencia de ejecución. La Autoridad autoriza formalmente
  toda incorporación directa, la Autoridad o el Evaluador con el documento la registra y el mecanismo
  nunca sustituye el proceso ordinario de una iniciativa.
- **BR-006**: Solo la Autoridad adopta decisiones formales de aprobación o archivo de iniciativas,
  aprobación o no aprobación de productos y cancelación; el Evaluador solo puede registrar su
  resultado cuando dispone del documento formal.
- **BR-007**: Un producto no aprobado requiere observación y evidencia; no retorna a ejecución y
  continúa al cierre administrativo.
- **BR-008**: El cierre desde cualquier resultado de producto requiere informe final, resultados,
  aprendizajes, conclusión y observación, y cambia el proyecto a `FINALIZADO`.
- **BR-009**: Los documentos y evidencias no pueden superar 100 MB y solo sirven como evidencia
  formal cuando cumplen formato, clasificación, integridad SHA-256 y las reglas de negocio
  aplicables. Los binarios se almacenan en Oracle PIIP; OGTI administra fuera de la aplicación los
  controles antimalware y PIIP no modela ni aplica estados o gates antimalware.
- **BR-010**: En la Fase 1, solo `Tipo de registro`, `Código`, `Nombre de iniciativa o proyecto` y
  `Estado` son campos públicos. También son públicos tipo documental, título sin datos personales,
  versión, formato y fecha de publicación; los documentos no muestran contenido ni permiten
  descarga pública.
- **BR-011**: Toda transición, decisión, documento, asignación y acceso sensible debe quedar
  auditado de forma inmutable con actor, momento, rol, unidad y cambio.
- **BR-012**: Los proyectos suspendidos o cancelados no tienen transición saliente autorizada en
  esta especificación.
- **BR-013**: El reporte institucional es generado por un usuario con perfil `Evaluador`; es
  semestral y también puede generarse cuando sea requerido. La versión semestral usa los periodos
  enero-junio con corte al 30/06 y julio-diciembre con corte al 31/12, mientras los reportes
  extraordinarios aplican BR-120 a BR-128. No son obligatorios reportes mensuales o trimestrales en
  Fase 1.
- **BR-014**: Ningún prototipo previo se considera aprobado sin validación de usuarios
  representativos y aprobación específica de la Oficina de Modernización.
- **BR-015**: Los ciclos de trabajo son quincenales y obligatorios durante la ejecución. Un ciclo
  debe registrar como mínimo objetivos, actividades, avance, dificultades, próximas acciones y
  evidencias. Un ciclo cerrado no se modifica: toda corrección crea una nueva versión trazable.
- **BR-016**: La incorporación inicial no incluye una carga conjunta o masiva; el Responsable
  incorpora cada registro individualmente, `UnidadAdmin` asiste y el Evaluador valida conforme a los
  controles definidos en BR-110 a BR-119.
- **BR-017**: Ante transiciones concurrentes incompatibles, prevalece la primera confirmada; las
  posteriores se rechazan y no sobrescriben el historial.
- **BR-018**: `NO_ADMISIBLE` se aplica únicamente al incumplimiento de requisitos formales;
  `NO_APLICABLE` se aplica únicamente cuando el caso no corresponde al proceso de innovación
  pública.
- **BR-019**: Cada operación se autoriza con una sola asignación efectiva de perfil y unidad; los
  permisos de asignaciones distintas no se combinan. Una asignación revocada deja de autorizar
  operaciones inmediatamente después de confirmarse su revocación.
- **BR-020**: Un participante sectorial mantiene iniciativas y proyectos con el perfil
  `Responsable` y únicamente en las unidades de su entidad asignadas explícitamente.
- **BR-021**: Cada iniciativa o proyecto puede tener una o varias unidades responsables, pero debe
  identificar exactamente una como unidad principal.
- **BR-022**: Cada asignación funcional tiene una fecha de inicio obligatoria y una fecha de fin
  opcional; solo está vigente dentro de ese periodo y mientras no haya sido revocada.
- **BR-023**: Toda reclasificación de privacidad requiere una decisión formal de la Autoridad; el
  Evaluador puede registrarla únicamente con el documento correspondiente.
- **BR-024**: Antes de declarar una iniciativa `NO_ADMISIBLE`, el Evaluador debe conceder una única
  oportunidad de subsanación con plazo registrado; si el plazo vence con incumplimientos formales,
  puede registrar la transición con observación obligatoria.
- **BR-025**: La presentación de una iniciativa exige el ingreso de los campos oficiales 1, 5 al 13
  y 22. Los campos 2, `Código`, y 4, `Fecha de inicio`, se generan automáticamente al confirmar la
  presentación; el campo 3, `Código de origen`, no aplica a una iniciativa nueva y queda vacío; el
  campo 23, `Nota`, es opcional. Los campos 14 al 21 se completan únicamente en sus etapas
  posteriores y no bloquean la presentación inicial.
- **BR-026**: Después de presentar una iniciativa, el Responsable solo puede editar los campos
  5 al 12, 22 y 23 durante la única subsanación abierta por el Evaluador. `Tipo de registro`,
  `Código`, `Código de origen`, `Fecha de inicio` y `Estado` no son editables.
- **BR-027**: `GlobalAdmin` puede confirmar la revocación de una asignación en el ámbito
  institucional y `UnidadAdmin` únicamente dentro de su ámbito autorizado.
- **BR-028**: El Responsable propone la clasificación inicial de cada documento y sus metadatos, y
  el Evaluador debe validarla.
- **BR-029**: Todo campo no público queda restringido a usuarios autorizados dentro de su ámbito
  organizacional, salvo que la matriz aprobada establezca una restricción mayor.
- **BR-030**: La detección de un posible duplicado durante la incorporación individual de
  información existente bloquea su validación hasta que el Evaluador resuelva el conflicto.
- **BR-031**: `Componente digital` se registra obligatoriamente como `Sí` o `No`; cuando su valor es
  `Sí`, debe incluir una descripción.
- **BR-032**: Cuando `Fuente u origen` tiene el valor `OTROS`, debe incluir una descripción de la
  fuente.
- **BR-033**: `POR_DEFINIR` es un valor válido de `Tipo de solución`, puede conservarse sin plazo
  obligatorio de resolución y no bloquea la evaluación ni la decisión de la Autoridad.
- **BR-034**: Para una iniciativa nueva, `Fecha de inicio` corresponde a la fecha de presentación y
  se genera automáticamente al confirmarla.
- **BR-035**: Cada iniciativa o proyecto debe tener exactamente un Responsable titular; los demás
  participantes no comparten esa titularidad.
- **BR-036**: Cada iniciativa o proyecto debe relacionarse con exactamente un `Objetivo PEI` y una
  `Actividad POI`.
- **BR-037**: El campo `Descripción` debe contener el problema público; la solución propuesta es una
  sección opcional y su ausencia no bloquea la presentación.
- **BR-038**: Una iniciativa puede presentarse con cero o más participantes adicionales; su ausencia
  no bloquea la presentación ni sustituye la obligación de un Responsable titular.
- **BR-039**: Los participantes se registran como personas o unidades organizacionales; un equipo se
  representa mediante sus personas integrantes y no como un participante independiente.
- **BR-040**: Una persona participante no requiere una cuenta PIIP y su registro no le concede
  acceso; cuando ya tenga una cuenta, debe vincularse con ella sin duplicar su identidad.
- **BR-041**: Una persona participante sin cuenta PIIP requiere únicamente nombres completos,
  institución y función; no se exige documento de identidad ni dato de contacto.
- **BR-042**: Durante `PROYECTO_EJECUCION`, el Responsable puede agregar o retirar participantes;
  cada alta o baja debe quedar auditada y conservar el historial anterior.
- **BR-043**: `UnidadAdmin` puede sustituir al Responsable titular de una iniciativa o proyecto
  únicamente dentro de su ámbito. La sustitución se hace efectiva inmediatamente al confirmarla:
  asume el nuevo titular, cesa el anterior, se conserva un solo titular y queda el historial del
  cambio.
- **BR-044**: Únicamente `GlobalAdmin` puede asignar, modificar o revocar el perfil `UnidadAdmin`;
  ningún `UnidadAdmin` puede administrar su propia asignación ni la de otro `UnidadAdmin`.
- **BR-045**: Toda nueva asignación `GlobalAdmin` requiere una decisión formal de la Autoridad y debe
  ser registrada por un `GlobalAdmin` con el documento correspondiente.
- **BR-046**: Cuando no exista ningún `GlobalAdmin`, el primero debe crearse mediante designación
  exclusivamente mediante una semilla SQL manual, revisable, de ejecución única y fail-fast. Un DBA
  autorizado de OGTI la ejecuta con el `sub` proporcionado por el administrador Keycloak y la
  aprobación de despliegue de la Jefatura de Modernización. La semilla usa los valores iniciales
  canónicos aprobados, registra la auditoría mínima y aborta sin cambios si existe cualquier
  asignación histórica `GlobalAdmin`. Después se aplica exclusivamente el flujo ordinario de PIIP.
- **BR-047**: Una suplencia funcional debe registrarse como una asignación temporal distinta, con
  fecha de inicio y fin obligatorias; no transfiere ni comparte las credenciales del titular.
- **BR-048**: Cada suplencia debe ser autorizada por la misma autoridad que autoriza la asignación
  permanente del perfil objeto de suplencia.
- **BR-049**: Durante la suplencia, la asignación equivalente del titular queda temporalmente
  inactiva para el mismo perfil y unidad. Al finalizar, se reactiva automáticamente solo si continúa
  dentro de su vigencia y no fue revocada.
- **BR-050**: No puede existir más de una suplencia con periodos superpuestos para la misma
  asignación de perfil y unidad; toda superposición debe rechazarse.
- **BR-051**: Una suplencia solo puede terminar anticipadamente por decisión de la misma autoridad
  que la autorizó. La terminación se hace efectiva al confirmarse y reactiva la asignación del
  titular únicamente si continúa vigente y no fue revocada.
- **BR-052**: Toda operación sensible debe revalidar la asignación efectiva inmediatamente antes de
  aplicar el cambio y rechazarse si fue revocada, venció o quedó inactiva, aunque hubiera comenzado
  autorizada.
- **BR-053**: `GlobalAdmin` puede asignar, modificar o revocar `Responsable` y `Consulta` en el ámbito
  institucional; `UnidadAdmin` puede hacerlo únicamente dentro de su ámbito autorizado.
- **BR-054**: Toda asignación, modificación o revocación de `Evaluador` requiere autorización formal
  de la Oficina de Modernización y registro por `GlobalAdmin` con el documento correspondiente.
- **BR-055**: Toda asignación, modificación o revocación de `Autoridad` debe ser registrada por
  `GlobalAdmin` conforme a una designación formal institucional vigente.
- **BR-056**: Los límites máximos son 500 caracteres para `Nombre de iniciativa o proyecto`, 2000
  para el problema público, 2000 para la solución propuesta, 500 para las descripciones de `OTROS` y
  `Componente digital`, y 1000 para `Nota`.
- **BR-057**: Todo contenido textual debe almacenarse sin espacios extremos y debe rechazarse cuando,
  después de recortarlos, quede vacío.
- **BR-058**: `Objetivo PEI` y `Actividad POI` se seleccionan de catálogos controlados vigentes. Una
  referencia retirada se conserva en registros históricos, pero no puede utilizarse en nuevas
  selecciones.
- **BR-059**: `Tipo de registro` queda inmutable después de presentar la iniciativa.
- **BR-060**: El Evaluador registra el informe de opinión técnica antes de la decisión sobre la
  iniciativa; toda corrección crea una nueva versión y conserva las anteriores.
- **BR-061**: La Autoridad o el Evaluador con la decisión formal correspondiente puede registrar el
  documento de decisión sobre la iniciativa; es obligatorio para aprobar o archivar e inmutable una
  vez registrada la transición.
- **BR-062**: La Autoridad o el Evaluador con la decisión formal correspondiente puede registrar el
  documento de aprobación del producto final; es obligatorio para `PRODUCTO_APROBADO` e inmutable.
- **BR-063**: La documentación de gestión del proyecto es una colección opcional y no sustituye los
  ciclos quincenales ni las evidencias obligatorias.
- **BR-064**: `Tipo de producto final aprobado` es obligatorio únicamente en `PRODUCTO_APROBADO` y
  utiliza los valores del catálogo canónico.
- **BR-065**: El Responsable registra los resultados clave y el Evaluador los valida; su validación
  es obligatoria para cerrar un proyecto.
- **BR-066**: `Fecha de cierre` se genera automáticamente cuando el proyecto pasa a `FINALIZADO` o
  `CANCELADO`.
- **BR-067**: El Evaluador registra el informe final de cierre; es obligatorio e inmutable al pasar a
  `FINALIZADO`.
- **BR-068**: Toda corrección de un documento formal debe crear una nueva versión trazable y conservar
  sin cambios el archivo anterior.
- **BR-069**: Durante `PROYECTO_EJECUCION`, el Responsable solo puede editar los campos 17,
  `Documentación de la gestión del proyecto`; 19, `Resultados clave`; y 23, `Nota`. Todo cambio debe
  quedar auditado.
- **BR-070**: En un proyecto derivado, `Código de origen` corresponde al código de la iniciativa y se
  asigna automáticamente; en un proyecto directo, corresponde al identificador obligatorio del acto
  formal o de la fuente heredada.
- **BR-071**: `Fecha de inicio` de todo proyecto corresponde a la fecha indicada en el documento
  formal de inicio y no a la fecha de registro.
- **BR-072**: La creación de un proyecto exige los campos 1 al 13 y 22, respetando los valores
  generados o copiados; el campo 23, `Nota`, es opcional y los campos 14 al 21 se completan en sus
  etapas posteriores.
- **BR-073**: Un proyecto derivado tiene nombre propio obligatorio; inicialmente se sugiere el nombre
  de la iniciativa y puede cambiarse antes de confirmar la creación.
- **BR-074**: Al crear un proyecto derivado, `Tipo de solución` se copia de la iniciativa de origen.
- **BR-075**: Cada proyecto selecciona su propio `Objetivo PEI` y `Actividad POI` al crearse, sin
  heredarlos automáticamente de la iniciativa.
- **BR-076**: Las unidades responsables de un proyecto derivado se copian inicialmente desde la
  iniciativa y pueden ajustarse antes de confirmar, manteniendo una o varias y exactamente una
  principal.
- **BR-077**: El Responsable titular del proyecto derivado se sugiere desde la iniciativa y puede
  cambiarse antes de confirmar, manteniendo exactamente un titular.
- **BR-078**: Solo la Autoridad o el Evaluador con el documento formal puede registrar un proyecto
  directo; el Responsable no puede registrarlo.
- **BR-079**: Todo proyecto nuevo inicia con `Tipo de registro = PROYECTO`, código propio generado y
  estado `PROYECTO_EJECUCION`.
- **BR-080**: Los niveles canónicos de clasificación son `PUBLICO`, `INTERNO` y `RESTRINGIDO`.
- **BR-081**: Todo campo no público sin regla especial tiene clasificación `INTERNO` y solo es
  visible para usuarios autorizados dentro de su ámbito organizacional.
- **BR-082**: El campo `Responsable` tiene clasificación `INTERNO` y es visible en consultas
  institucionales dentro del ámbito autorizado.
- **BR-083**: Los datos de personas participantes tienen clasificación `RESTRINGIDO` y solo pueden
  consultarlos el Responsable del registro, el Evaluador y los administradores autorizados dentro de
  su ámbito.
- **BR-084**: Los únicos metadatos documentales públicos son tipo documental, título sin datos
  personales, versión, formato y fecha de publicación. Solo se publican cuando el Evaluador confirma
  una versión con clasificación `PUBLICO` validada; la fecha del servidor al confirmar fija
  `fechaPublicacion`. Si la clasificación pública resulta de una reclasificación, se exige además la
  decisión formal de la Autoridad conforme a BR-023.
- **BR-085**: El contenido de un documento puede consultarse institucionalmente solo cuando el ámbito
  y la clasificación del usuario lo permiten; nunca es público en la Fase 1.
- **BR-086**: Se consideran datos personales detectables los nombres, documento de identidad, correo,
  teléfono, dirección, firma e identificadores equivalentes.
- **BR-087**: Un documento con clasificación pendiente o no validada solo es visible para el
  Responsable que lo cargó y el Evaluador, y no puede utilizarse como evidencia formal.
- **BR-088**: Una reclasificación surte efecto inmediatamente cuando el Evaluador la registra con la
  decisión formal de la Autoridad.
- **BR-089**: Una reclasificación más restrictiva bloquea los accesos futuros incompatibles y conserva
  inmutables las auditorías de accesos anteriores.
- **BR-090**: `GlobalAdmin` puede crear usuarios institucionalmente y `UnidadAdmin` únicamente para
  personas de sus unidades autorizadas.
- **BR-091**: La activación inicial debe realizarse mediante correo de Keycloak; PIIP no recopila,
  procesa ni almacena la contraseña.
- **BR-092**: `GlobalAdmin` puede desactivar usuarios institucionalmente y `UnidadAdmin` únicamente
  dentro de su ámbito autorizado.
- **BR-093**: La desactivación se hace efectiva inmediatamente en Keycloak y PIIP y conserva el
  usuario, sus asignaciones y todo el historial.
- **BR-094**: La reactivación corresponde a la misma autoridad que puede desactivar al usuario y no
  restaura asignaciones vencidas o revocadas.
- **BR-095**: La modificación o revocación de una asignación `GlobalAdmin` requiere decisión formal de
  la Autoridad y registro por otro `GlobalAdmin`.
- **BR-096**: El último `GlobalAdmin` activo no puede revocarse hasta que exista un reemplazo
  formalmente designado y activo.
- **BR-097**: Un cambio de unidad cierra las asignaciones anteriores y exige crear nuevas asignaciones
  explícitas; ningún permiso se traslada automáticamente.
- **BR-098**: La creación de una identidad duplicada debe bloquearse y resolverse usando el
  identificador de Keycloak y el correo institucional.
- **BR-099**: Toda creación, activación, desactivación, reactivación, cambio de unidad o resolución de
  identidad duplicada debe quedar auditada con actor, momento, ámbito, persona, operación y resultado.
- **BR-100**: La admisibilidad exige que todos los campos obligatorios estén completos y que los
  valores seleccionados pertenezcan a catálogos vigentes.
- **BR-101**: La Ficha de Iniciativa de Innovación Pública debe estar adjunta, conservar integridad
  SHA-256 y tener clasificación validada para superar la admisibilidad.
- **BR-102**: El Responsable debe tener una asignación vigente y actuar dentro de la unidad efectiva
  de la iniciativa para superar la admisibilidad.
- **BR-103**: La admisibilidad exige exactamente un Responsable titular y exactamente una unidad
  principal.
- **BR-104**: Un posible duplicado bloquea la admisibilidad hasta que el Evaluador resuelva y documente
  el conflicto.
- **BR-105**: Una iniciativa es aplicable únicamente si el problema corresponde a las competencias del
  MIDAGRI o de su ámbito sectorial.
- **BR-106**: Una iniciativa es aplicable únicamente si identifica beneficiarios y un resultado
  público esperado.
- **BR-107**: Una iniciativa es aplicable únicamente si propone una solución nueva o sustancialmente
  mejorada que requiere validación.
- **BR-108**: Una iniciativa es `NO_APLICABLE` cuando consiste únicamente en compra, mantenimiento,
  digitalización sin rediseño o cumplimiento rutinario.
- **BR-109**: El Evaluador debe decidir la aplicabilidad mediante una lista estructurada que exige
  cumplir BR-105 a BR-107 y no incurrir en BR-108, registrando el resultado y motivo. Los
  incumplimientos de BR-100 a BR-104 se tratan mediante la única subsanación y pueden concluir en
  `NO_ADMISIBLE` si persisten al vencer el plazo.
- **BR-110**: Toda incorporación individual exige fuente, fecha, Responsable, archivo o referencia y
  hash de la evidencia de origen.
- **BR-111**: El código heredado se conserva como `Código de origen` y PIIP genera un código propio
  conforme a sus reglas.
- **BR-112**: El Evaluador asigna el estado canónico de negocio del registro heredado y debe sustentar
  esa selección con evidencia.
- **BR-113**: Un registro con datos incompletos permanece en estado de incorporación `PENDIENTE` y no
  se considera un registro ordinario validado.
- **BR-114**: Si el Evaluador confirma un duplicado, no se crea otro registro y la evidencia de origen
  se vincula al registro existente con trazabilidad.
- **BR-115**: Un código reutilizado o conflictivo bloquea la validación hasta que el Evaluador registre
  una resolución documentada; no se renumera automáticamente.
- **BR-116**: Una relación iniciativa-proyecto inválida bloquea la validación hasta ser corregida.
- **BR-117**: Antes de la validación se permiten correcciones ilimitadas, cada una con auditoría de
  datos anteriores, nuevos, actor, momento y motivo.
- **BR-118**: Los estados de incorporación son `PENDIENTE`, `VALIDADO` y `RECHAZADO` y son
  independientes del estado canónico de negocio.
- **BR-119**: La auditoría de incorporación conserva fuente, actores, datos originales, cambios,
  errores, resolución, fechas y hash; un rechazo nunca elimina esa evidencia.
- **BR-120**: Un reporte extraordinario requiere una solicitud documentada, aprobación de la Oficina
  de Modernización y generación por un usuario con perfil `Evaluador`.
- **BR-121**: El reporte semestral incluye totales por tipo, estado, unidad, fuente, tipo de solución,
  producto y cierre.
- **BR-122**: Los indicadores institucionales son admisibilidad, aprobación, cierre y cumplimiento de
  ciclos. Se calculan, respectivamente, como decisiones admisibles entre decisiones de admisibilidad
  completadas, iniciativas aprobadas entre decisiones de Autoridad sobre iniciativas, proyectos
  `FINALIZADO` entre proyectos `FINALIZADO` o `CANCELADO`, y ciclos completos entre ciclos aplicables;
  cada cociente se multiplica por 100, muestra numerador y denominador y se presenta como no aplicable
  cuando el denominador es cero.
- **BR-123**: Un reporte configurable admite filtros por periodo, tipo, estado, unidad, Responsable,
  fuente, tipo de solución y producto, sin exceder el ámbito autorizado del generador.
- **BR-124**: Cada reporte se genera como PDF oficial y XLSX de detalle a partir del mismo corte,
  parámetros y versión de datos.
- **BR-125**: Los destinatarios permitidos son autoridades MIDAGRI, la Oficina de Modernización y
  PCM-SGP cuando su remisión esté autorizada.
- **BR-126**: Todo reporte se clasifica `INTERNO` por defecto y `RESTRINGIDO` cuando contenga cualquier
  información con esa clasificación.
- **BR-127**: Antes de toda remisión, la Oficina de Modernización aprueba la versión exacta y sus
  destinatarios.
- **BR-128**: La generación y remisión conserva parámetros, corte, generador, versión, hash,
  aprobación, destinatario, fecha y resultado.
- **BR-129**: Un usuario con perfil `Evaluador` registra las validaciones y la aprobación de
  prototipos en representación de la Oficina de Modernización.
- **BR-130**: Cada prototipo es validado por al menos un usuario de cada perfil funcional involucrado
  y por al menos un actor sectorial cuando el recorrido aplica a ese ámbito.
- **BR-131**: La validación evalúa el recorrido completo, sus reglas de negocio, mensajes de
  información y error, accesibilidad y privacidad.
- **BR-132**: No existe un límite de iteraciones; un prototipo conserva estado `OBSERVADO` mientras
  tenga algún hallazgo crítico o alto sin resolver.
- **BR-133**: Cada versión identifica código, fecha, recorrido, cambios, estado y versión anterior.
- **BR-134**: Cada validación conserva usuario, perfil funcional, escenario, resultado, observaciones
  y aceptación.
- **BR-135**: Los estados de prototipo son `BORRADOR`, `EN_VALIDACION`, `OBSERVADO`, `VALIDADO`,
  `APROBADO` y `RECHAZADO`.
- **BR-136**: El Evaluador que aprueba un prototipo debe ser distinto de su autor y no puede ser su
  único validador.
- **BR-137**: Todo cambio funcional o de accesibilidad sobre un prototipo `APROBADO` crea una nueva
  versión que requiere validación y aprobación; la versión anterior se conserva como historial, pero
  no habilita la implementación del cambio.
- **BR-138**: Los ocho recorridos funcionales se validan en escritorio y móvil; la validación por
  teclado y lector de pantalla se exige en cada componente e interacción donde resulte aplicable.
- **BR-139**: La medición inicial cubre los ocho recorridos funcionales agrupados según su etapa de
  implementación.
- **BR-140**: Cada medición registra éxito de tarea, tiempo mediano, errores críticos, satisfacción y
  accesibilidad.
- **BR-141**: La medición se realiza mediante pruebas moderadas con escenarios definidos y prototipos
  versionados.
- **BR-142**: La muestra objetivo es de cinco usuarios por cada perfil funcional involucrado. Se
  admite una cantidad menor únicamente con justificación documentada y sin omitir ningún perfil.
- **BR-143**: Las pruebas usan datos sintéticos representativos y no utilizan información personal
  real.
- **BR-144**: El equipo de experiencia de usuario ejecuta las pruebas y calcula las métricas, un
  Evaluador coordina y otro Evaluador aprueba los resultados por la Oficina de Modernización.
- **BR-145**: La medición inicial de cada recorrido ocurre durante la validación y antes de aprobar su
  prototipo.
- **BR-146**: La evidencia de medición conserva versión, muestra, escenarios, resultados, cálculos,
  hallazgos, metas aplicables, aprobación y hash.
- **BR-147**: La medición se repite antes de liberar cada recorrido y después de todo cambio funcional
  o de accesibilidad que lo afecte.
- **BR-148**: Los reportes institucionales y el expediente completo de formatos, versiones,
  aprobaciones, hashes y evidencias de remisión se conservan durante el plazo establecido por la tabla
  de retención documental vigente del MIDAGRI. Mientras ese plazo no esté confirmado, PIIP no realiza
  eliminaciones automáticas. Toda disposición posterior debe ser autorizada, auditada y aplicada al
  expediente completo, sin eliminar evidencias selectivamente.
- **BR-149**: Después de la medición inicial, la Oficina de Modernización aprueba y versiona por
  recorrido una matriz de metas antes de iniciar la implementación. Exige éxito de tarea mayor o igual
  al 90 %, cero errores críticos, satisfacción mayor o igual a 4/5 y ausencia de hallazgos críticos o
  altos de accesibilidad según el estándar institucional aprobado. Con línea base comparable, el
  tiempo mediano mejora al menos 20 %; sin ella, su meta se fija desde la medición inicial. Los errores
  críticos y los hallazgos críticos o altos de accesibilidad bloquean la liberación; una excepción a
  las demás métricas requiere justificación y aprobación documentadas.
- **BR-150**: La matriz cargo o función-perfil-unidad es un catálogo configurable y versionado. Una
  función puede relacionarse con múltiples perfiles y unidades concretas, pero cada combinación
  identifica exactamente una función, un perfil y una unidad concreta. Cada asignación funcional
  selecciona una sola combinación vigente y deriva de ella su función, perfil y unidad, sin aceptar
  valores discordantes ni combinar permisos. Crear,
  modificar o inactivar una combinación exige la aprobación de la misma autoridad que autoriza la
  asignación del perfil afectado y el registro por `GlobalAdmin` o `UnidadAdmin` únicamente cuando las
  reglas de ese perfil le permitan administrarlo. Las combinaciones inactivas se conservan en las
  asignaciones históricas y no se permiten en nuevas asignaciones.
- **BR-151**: Objetivo PEI y Actividad POI son catálogos controlados y versionados. La oficina
  responsable de planeamiento aprueba códigos, descripciones y vigencias, y `GlobalAdmin` registra en
  PIIP la versión formalmente aprobada. La carga inicial se realiza mediante una semilla aprobada y no
  mediante sincronización externa. Una referencia inactiva se conserva en históricos y no se permite
  en nuevas selecciones. Ambos catálogos se versionan de forma independiente: una versión PEI no
  crea, activa, modifica ni retira una versión POI, y viceversa.
- **BR-152**: Todo documento pertenece mediante su serie documental a exactamente un propietario:
  un registro de portafolio o un expediente institucional. Los documentos formales de catálogos,
  matrices, designaciones u otras decisiones sin iniciativa o proyecto pertenecen a un expediente
  institucional. La pertenencia es excluyente e inmutable y todas las versiones de una serie la
  heredan; ningún expediente institucional se expone por la consulta pública del portafolio.

Los 23 campos oficiales afectados por esta especificación son:

| N.º | Campo oficial | Presentación de iniciativa |
|---:|---|---|
| 1 | Tipo de registro | Obligatorio; inmutable después de presentar |
| 2 | Código | Generado automáticamente al confirmar la presentación |
| 3 | Código de origen | No aplica; permanece vacío |
| 4 | Fecha de inicio | Generada automáticamente al confirmar la presentación |
| 5 | Nombre de iniciativa o proyecto | Obligatorio; máximo 500 caracteres |
| 6 | Tipo de solución | Obligatorio; `POR_DEFINIR` puede conservarse sin plazo de resolución |
| 7 | Fuente u origen | Obligatorio; descripción de máximo 500 caracteres cuando es `OTROS` |
| 8 | Responsable | Obligatorio; exactamente un titular |
| 9 | Descripción | Problema público obligatorio y solución propuesta opcional; máximo 2000 caracteres cada sección |
| 10 | Objetivo PEI | Obligatorio; exactamente uno de catálogo vigente |
| 11 | Actividad POI | Obligatorio; exactamente una de catálogo vigente |
| 12 | Unidades de organización responsables | Obligatorio |
| 13 | Estado | Obligatorio |
| 14 | Informe de opinión técnica de evaluación de iniciativa | Evaluador; obligatorio antes de la decisión; correcciones versionadas |
| 15 | Documento formal de decisión de aprobación | Autoridad o Evaluador con decisión formal; obligatorio para aprobar o archivar; inmutable |
| 16 | Documento formal de aprobación del producto final | Autoridad o Evaluador con decisión formal; obligatorio en `PRODUCTO_APROBADO`; inmutable |
| 17 | Documentación de la gestión del proyecto | Colección opcional; no sustituye ciclos ni evidencias obligatorias |
| 18 | Tipo de producto final aprobado | Obligatorio solo en `PRODUCTO_APROBADO`; catálogo canónico |
| 19 | Resultados clave | Responsable registra; Evaluador valida; obligatorio para cerrar |
| 20 | Fecha de cierre | Generada automáticamente en `FINALIZADO` o `CANCELADO` |
| 21 | Informe final de cierre | Evaluador registra; obligatorio e inmutable en `FINALIZADO` |
| 22 | Componente digital | Obligatorio: `Sí/No`; descripción obligatoria de máximo 500 caracteres cuando es `Sí` |
| 23 | Nota | Opcional; máximo 1000 caracteres |

La creación de proyectos aplica esta matriz:

| N.º | Campo oficial | Proyecto derivado | Proyecto directo |
|---:|---|---|---|
| 1 | Tipo de registro | `PROYECTO`, generado | `PROYECTO`, generado |
| 2 | Código | Generado automáticamente | Generado automáticamente |
| 3 | Código de origen | Código de iniciativa, asignado automáticamente | Identificador obligatorio del acto formal o fuente heredada |
| 4 | Fecha de inicio | Fecha del documento formal | Fecha del documento formal |
| 5 | Nombre | Propio y obligatorio; sugerido desde la iniciativa y ajustable antes de confirmar | Propio y obligatorio |
| 6 | Tipo de solución | Copiado desde la iniciativa | Obligatorio; catálogo canónico |
| 7 | Fuente u origen | Obligatorio | Obligatorio |
| 8 | Responsable | Exactamente uno; sugerido desde la iniciativa y ajustable antes de confirmar | Exactamente uno |
| 9 | Descripción | Problema obligatorio; solución opcional | Problema obligatorio; solución opcional |
| 10 | Objetivo PEI | Selección propia de catálogo vigente | Selección propia de catálogo vigente |
| 11 | Actividad POI | Selección propia de catálogo vigente | Selección propia de catálogo vigente |
| 12 | Unidades responsables | Copiadas inicialmente y ajustables antes de confirmar; exactamente una principal | Una o varias; exactamente una principal |
| 13 | Estado | `PROYECTO_EJECUCION`, generado | `PROYECTO_EJECUCION`, generado |
| 14-21 | Campos de evaluación, ejecución, producto y cierre | Se completan en la etapa correspondiente | Se completan en la etapa correspondiente |
| 22 | Componente digital | Obligatorio según regla `Sí/No` | Obligatorio según regla `Sí/No` |
| 23 | Nota | Opcional | Opcional |

Los tipos documentales y condiciones mínimas aplicables son:

| Tipo documental | Etapa o estado | Condición |
|---|---|---|
| Ficha de Iniciativa de Innovación Pública | `PRESENTADO` | Obligatorio |
| Informe de Opinión Técnica de Evaluación | Decisión sobre iniciativa | Obligatorio antes de la decisión |
| Documento Formal de Decisión sobre la Iniciativa | `INICIATIVA_APROBADA`, `INICIATIVA_ARCHIVADA` | Obligatorio |
| Documento Formal de Aprobación o Autorización de Inicio | `PROYECTO_EJECUCION` | Obligatorio para proyecto derivado o directo |
| Nota Conceptual del Proyecto | `PROYECTO_EJECUCION` | Opcional |
| Matriz de Planificación de Ciclos | `PROYECTO_EJECUCION` | Opcional |
| Seguimiento Ágil, Tablero Kanban | `PROYECTO_EJECUCION` | Opcional |
| Autoevaluación de Ciclo de Trabajo | `PROYECTO_EJECUCION` | Opcional |
| Documento Formal de Aprobación de Producto Final | `PRODUCTO_APROBADO` | Obligatorio |
| Evidencia de No Aprobación del Producto Final | `PRODUCTO_NO_APROBADO` | Obligatorio con observación |
| Informe Final de Cierre | `FINALIZADO` | Obligatorio con observación de cierre |
| Evidencia de Suspensión | `SUSPENDIDO` | Obligatorio con observación |
| Informe de la Oficina de Modernización, Cancelación | `CANCELADO` | Obligatorio con observación |

Cada archivo admite como máximo 100 MB y los formatos iniciales PDF, Office Open XML, JPEG y PNG.
Debe conservar el binario en Oracle PIIP, versión, autor, fecha, clasificación e integridad SHA-256.
OGTI administra los controles antimalware fuera del alcance funcional de PIIP.

| Affected entity or state | Current condition | Change | Evidence or document | Exception |
|---|---|---|---|---|
| Iniciativa | Registro nuevo | `PRESENTADO` | Ficha y documentos exigidos por matriz | Ninguna |
| `PRESENTADO` | Mantiene incumplimientos de campos, catálogos, ficha, asignación, cardinalidad o duplicados después de agotar la única subsanación | `NO_ADMISIBLE` | Lista de admisibilidad, plazo y observación obligatoria; documento adicional opcional | Terminal |
| `PRESENTADO` | Incumple competencia, valor público o carácter innovador, o incurre en una exclusión | `NO_APLICABLE` | Lista estructurada y motivo obligatorio; documento adicional opcional | Terminal |
| `PRESENTADO` | Opinión y decisión formal favorables | `INICIATIVA_APROBADA` | Opinión técnica y decisión formal | Origina proyecto separado |
| `PRESENTADO` | Opinión y decisión formal de archivo | `INICIATIVA_ARCHIVADA` | Opinión, decisión formal y observación | Terminal |
| Iniciativa aprobada | Responsable autorizado crea proyecto | Proyecto nuevo en `PROYECTO_EJECUCION` | Decisión formal de aprobación o inicio | No cambia el estado de la iniciativa |
| Proyecto directo | Heredado o excepción autorizada | `PROYECTO_EJECUCION` | Autorización formal de la Autoridad y evidencias mínimas | Solo casos formalizados |
| `PROYECTO_EJECUCION` | Decisión sustentada de UnidadAdmin | `SUSPENDIDO` | Evidencia y observación obligatorias | Sin salida autorizada |
| `PROYECTO_EJECUCION` | Decisión formal de Autoridad | `CANCELADO` | Documento y observación obligatorios | Sin salida autorizada |
| `PROYECTO_EJECUCION` | Producto aprobado por Autoridad | `PRODUCTO_APROBADO` | Documento formal | Continúa al cierre |
| `PROYECTO_EJECUCION` | Producto no aprobado por Autoridad | `PRODUCTO_NO_APROBADO` | Evidencia y observación obligatorias | Continúa al cierre |
| `PRODUCTO_APROBADO` | Cierre completo por Evaluador | `FINALIZADO` | Informe final, resultados, aprendizajes, conclusión y observación | Ninguna |
| `PRODUCTO_NO_APROBADO` | Cierre completo por Evaluador | `FINALIZADO` | Informe final, resultados, aprendizajes, conclusión y observación | Ninguna |

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema debe permitir al Responsable registrar una iniciativa dentro de su ámbito
  organizacional y aplicar después de la presentación las restricciones de edición de FR-070.
- **FR-002**: El registro debe contemplar exactamente los 23 campos oficiales identificados en la
  Constitución PIIP.
- **FR-003**: El sistema debe aplicar la matriz de obligatoriedad, editabilidad, privacidad y actor
  responsable por tipo de registro y etapa definida en esta especificación.
- **FR-004**: El registro debe exigir la descripción del problema público y permitir registrar una
  solución propuesta opcional, además de participantes y documentos conforme a la representación y
  obligatoriedad definidas en esta especificación. Cuando `Fuente u origen` sea `OTROS`, debe exigir una
  descripción de la fuente.
- **FR-005**: Cada iniciativa y proyecto debe recibir un código propio, inmutable, único por año y
  unidad principal, con formato `AAAA-PREFIJO_UNIDAD-NNNNN`, que nunca se reutiliza. Para una
  iniciativa nueva, el sistema debe generarlo automáticamente al confirmar la presentación.
- **FR-006**: Una iniciativa nueva debe iniciar en `PRESENTADO` y solo puede seguir las transiciones
  autorizadas en esta especificación.
- **FR-007**: El Evaluador debe poder revisar requisitos y documentos, registrar observaciones y
  emitir una opinión técnica previa a la decisión de la Autoridad.
- **FR-008**: El Evaluador debe poder determinar `NO_ADMISIBLE` o `NO_APLICABLE` con observación,
  aplicando respectivamente los requisitos formales y la lista estructurada de aplicabilidad
  definidos en esta especificación; `NO_ADMISIBLE` solo procede después de una única subsanación con
  plazo registrado, y no se exige documento adicional.
- **FR-009**: Solo la Autoridad debe poder decidir la aprobación o archivo de una iniciativa.
- **FR-010**: La Autoridad puede registrar su decisión; el Evaluador puede registrar el resultado
  únicamente con el documento formal correspondiente.
- **FR-011**: Una iniciativa aprobada debe poder originar un único proyecto nuevo en
  `PROYECTO_EJECUCION`, con identidad, código e historial propios y vínculo inmutable con la
  iniciativa; el Responsable autorizado debe crearlo y todo intento de crear un segundo proyecto
  derivado debe rechazarse.
- **FR-012**: La consulta debe mostrar la trazabilidad completa entre iniciativa, decisión formal,
  proyecto derivado, documentos e historiales.
- **FR-013**: El sistema debe permitir crear un proyecto directo únicamente cuando sea heredado o
  exista una excepción; en ambos casos, la Autoridad debe autorizar formalmente su incorporación y
  solo la Autoridad o el Evaluador con el documento formal puede registrarlo.
- **FR-014**: La creación directa como proyecto heredado debe exigir que haya iniciado antes de PIIP
  y que presente el acto formal y la evidencia de ejecución correspondientes.
- **FR-015**: Un proyecto directo debe completar los campos oficiales 1 al 13 y 22, tratar el campo
  23 como opcional y registrar la autorización y evidencias disponibles.
- **FR-016**: El Responsable debe poder registrar planificación, ciclos, avances, dificultades,
  productos parciales y evidencias durante `PROYECTO_EJECUCION`.
- **FR-017**: Cada proyecto en ejecución debe registrar un ciclo obligatorio quincenal con la
  estructura mínima de objetivos, actividades, avance, dificultades, próximas acciones y
  evidencias. La corrección de un ciclo cerrado debe crear una nueva versión trazable sin modificar
  la versión cerrada.
- **FR-018**: Cada documento o evidencia debe admitir como máximo 100 MB, aplicar los formatos
  constitucionales, almacenar el binario en Oracle PIIP y cumplir íntegramente los controles
  documentales de PSA-005 y PSA-006. PIIP no debe modelar estados, resultados, informes ni gates
  antimalware; esos controles técnicos pertenecen exclusivamente a OGTI.
- **FR-019**: El Responsable debe poder presentar el producto final con sus documentos de sustento.
- **FR-020**: Solo la Autoridad debe decidir formalmente la aprobación o no aprobación del producto.
- **FR-021**: `PRODUCTO_NO_APROBADO` debe exigir observación y evidencia antes de registrar el
  resultado.
- **FR-022**: El Evaluador debe poder cerrar un proyecto desde `PRODUCTO_APROBADO` o
  `PRODUCTO_NO_APROBADO` solo con informe final, resultados, aprendizajes, conclusión y observación
  completos.
- **FR-023**: El cierre administrativo completo debe cambiar el proyecto a `FINALIZADO`.
- **FR-024**: La suspensión debe corresponder a `UnidadAdmin` y exigir decisión sustentada,
  evidencia y observación; la cancelación debe corresponder a `Autoridad` y exigir documento y
  observación.
- **FR-025**: El sistema debe rechazar toda transición no listada, incluido cualquier intento de
  salida desde `SUSPENDIDO`, `CANCELADO` o los estados terminales de iniciativa.
- **FR-026**: El Administrador funcional debe poder gestionar participantes, perfiles y ámbitos
  como `GlobalAdmin` en toda la institución y como `UnidadAdmin` dentro de sus unidades autorizadas;
  `GlobalAdmin` debe poder confirmar revocaciones institucionales y `UnidadAdmin` solo las de su
  ámbito autorizado.
- **FR-027**: La autorización debe aplicar la matriz de operaciones por perfil y unidad que se
  define en esta especificación, limitar cada asignación a las unidades explícitamente incluidas y
  evaluar una sola asignación efectiva por operación. La revocación debe invalidar la asignación
  inmediatamente después de confirmarse.
- **FR-028**: Una persona debe poder mantener varias asignaciones de perfil y unidad sin extender
  automáticamente permisos entre perfiles, ámbitos o unidades descendientes; ninguna operación
  debe combinar permisos de asignaciones distintas.
- **FR-029**: Los usuarios institucionales deben poder buscar, filtrar y consultar estados,
  responsables, resultados, documentos autorizados e historial dentro de su ámbito.
- **FR-030**: Los participantes sectoriales autorizados deben poder mantener registros únicamente
  con el perfil `Responsable` y dentro de las unidades explícitamente asignadas de su Programa,
  Proyecto Especial u Organismo Público Adscrito.
- **FR-031**: La Fase 1 no debe incluir carga manual conjunta o masiva; la información existente se
  incorpora individualmente por el Responsable, con asistencia de `UnidadAdmin` y validación del
  Evaluador. Si se detecta un posible duplicado, la validación debe bloquearse hasta que el Evaluador
  resuelva el conflicto y quede evidencia de la resolución; los demás controles deben aplicar
  FR-121 a FR-130.
- **FR-032**: La Oficina de Modernización debe poder generar, mediante un usuario con perfil
  `Evaluador`, el reporte institucional semestral y reportes adicionales cuando sean requeridos.
- **FR-033**: El reporte semestral debe cubrir enero-junio con corte al 30/06 y julio-diciembre con
  corte al 31/12; los reportes extraordinarios deben aplicar FR-131 a FR-139.
- **FR-034**: La consulta pública debe permitir la búsqueda y consulta minimizada definida en
  PSA-007 y PSA-008.
- **FR-035**: La consulta pública debe aplicar la prohibición de contenido y descarga documental de
  PSA-007 durante la Fase 1.
- **FR-036**: La consulta pública y la consulta institucional deben aplicar la clasificación,
  exclusiones y restricciones de PSA-001, PSA-007, PSA-008 y PSA-009 sin ampliar el ámbito.
- **FR-037**: Deben existir prototipos PIIP validados y aprobados para registro, evaluación,
  decisión, seguimiento, producto final, cierre, consulta institucional y consulta pública.
- **FR-038**: Usuarios representativos deben validar cada prototipo y la Oficina de Modernización
  debe aprobarlo aplicando FR-140 a FR-148.
- **FR-039**: Los prototipos existentes no deben considerarse aprobados sin la validación específica
  de PIIP.
- **FR-040**: El sistema no debe realizar conectores, intercambios o sincronizaciones automáticas con
  sistemas funcionales externos no aprobados durante la Fase 1. Esta restricción no prohíbe Keycloak
  Admin API para el aprovisionamiento, activación, desactivación y reactivación de identidades exigidos
  por la Constitución.
- **FR-041**: Cada iniciativa o proyecto debe admitir una o varias unidades responsables y exigir
  que exactamente una de ellas sea la unidad principal.
- **FR-042**: Cada asignación funcional debe registrar una fecha de inicio y puede registrar una
  fecha de fin; el sistema debe rechazar su uso fuera de ese periodo o después de su revocación.
- **FR-043**: El sistema debe permitir que la Autoridad autorice una reclasificación de privacidad y
  que el Evaluador la registre únicamente con la decisión formal correspondiente.
- **FR-044**: El sistema debe permitir al Evaluador abrir una única subsanación por iniciativa
  `PRESENTADO`, registrar su plazo y rechazar una segunda oportunidad; mientras el plazo no venza o
  la subsanación no sea atendida, la iniciativa debe permanecer en `PRESENTADO`.
- **FR-045**: El sistema debe exigir el ingreso de los campos oficiales 1, 5 al 13 y 22 para
  presentar una iniciativa nueva; al confirmar la presentación debe generar automáticamente el
  campo 2 y el campo 4 con la fecha de presentación, mantener vacío el campo 3 porque no aplica y
  tratar el campo 23 como opcional. El campo 22 debe aceptar `Sí` o `No` y exigir una descripción
  cuando su valor sea `Sí`. No debe exigir los campos 14 al 21 hasta la etapa posterior que les
  corresponde.
- **FR-046**: El sistema debe permitir que `Tipo de solución` conserve el valor `POR_DEFINIR` sin
  plazo obligatorio de resolución y no debe rechazar por ese único motivo una evaluación o decisión
  de la Autoridad.
- **FR-047**: Cada iniciativa o proyecto debe identificar exactamente un Responsable titular y debe
  rechazar la ausencia o asignación simultánea de más de un titular, sin impedir registrar otros
  participantes.
- **FR-048**: Cada iniciativa o proyecto debe relacionarse con exactamente un `Objetivo PEI` y una
  `Actividad POI` de sus catálogos controlados vigentes, y debe rechazar la ausencia o selección
  múltiple de cualquiera de ellos.
- **FR-049**: El sistema debe permitir presentar una iniciativa con cero o más participantes
  adicionales, sin confundirlos con el Responsable titular ni exigir al menos uno.
- **FR-050**: El sistema debe permitir asociar personas y unidades organizacionales como
  participantes y debe representar un equipo mediante sus personas integrantes, sin crear un tipo
  de participante independiente para el equipo.
- **FR-051**: El sistema debe permitir registrar una persona participante sin crearle acceso y debe
  vincularla con su cuenta PIIP existente cuando corresponda, sin duplicar su identidad ni conceder
  permisos por el solo hecho de participar.
- **FR-052**: Para una persona participante sin cuenta PIIP, el sistema debe exigir nombres
  completos, institución y función, sin exigir documento de identidad ni datos de contacto.
- **FR-053**: Durante `PROYECTO_EJECUCION`, el sistema debe permitir al Responsable agregar o retirar
  participantes dentro de su ámbito y debe auditar cada cambio sin eliminar el historial anterior.
- **FR-054**: El sistema debe permitir a `UnidadAdmin` sustituir al Responsable titular únicamente
  dentro de su ámbito. Al confirmar la operación, debe hacer efectivo inmediatamente al nuevo
  titular, cesar al anterior, mantener exactamente un titular y conservar el anterior, el nuevo, el
  actor y el momento en un historial inmutable.
- **FR-055**: El sistema debe permitir únicamente a `GlobalAdmin` asignar, modificar o revocar el
  perfil `UnidadAdmin`, y debe denegar y auditar cualquier intento realizado por otro perfil.
- **FR-056**: El sistema debe permitir registrar una nueva asignación `GlobalAdmin` únicamente a un
  `GlobalAdmin` y solo cuando exista la decisión formal de la Autoridad y el documento
  correspondiente; cualquier intento incompleto debe rechazarse y auditarse.
- **FR-057**: El sistema debe admitir el aprovisionamiento inicial del primer `GlobalAdmin` solo con
  la semilla SQL manual aprobada. La semilla debe aceptar el `sub` proporcionado por el administrador
  Keycloak de OGTI, prevalidar y reutilizar `MIDAGRI`, crear `ADMINISTRADOR_PIIP`, su combinación y la asignación,
  exigir la aprobación de despliegue de la Jefatura de Modernización, registrar la auditoría definida
  en PSA-014 y abortar sin cambios si existe cualquier asignación histórica `GlobalAdmin` o si ya fue
  ejecutada. No debe existir comando, endpoint, cliente OIDC temporal ni bootstrap alternativo.
- **FR-058**: El sistema debe registrar cada suplencia como una asignación temporal distinta, exigir
  fechas de inicio y fin y aplicar sus permisos únicamente dentro de ese periodo, sin transferir ni
  compartir credenciales del titular.
- **FR-059**: El sistema debe exigir que una suplencia sea autorizada por la misma autoridad que
  autoriza la asignación permanente del perfil y debe rechazar y auditar cualquier autorización
  realizada por otro actor.
- **FR-060**: Durante la vigencia de una suplencia, el sistema debe impedir que el titular utilice la
  asignación del mismo perfil y unidad y, al finalizar, debe reactivarla automáticamente solo si
  continúa vigente y no fue revocada.
- **FR-061**: El sistema debe rechazar y auditar toda suplencia cuyo periodo se superponga con otra
  suplencia de la misma asignación de perfil y unidad.
- **FR-062**: El sistema debe permitir terminar anticipadamente una suplencia únicamente a la misma
  autoridad que la autorizó; al confirmar la terminación debe cesar inmediatamente al suplente y
  reactivar al titular solo si su asignación continúa vigente y no fue revocada.
- **FR-063**: El sistema debe revalidar la asignación efectiva inmediatamente antes de aplicar toda
  operación sensible y debe rechazar y auditar la operación si la asignación fue revocada, venció o
  quedó inactiva durante su ejecución.
- **FR-064**: El sistema debe permitir a `GlobalAdmin` asignar, modificar o revocar `Responsable` y
  `Consulta` institucionalmente y a `UnidadAdmin` únicamente dentro de su ámbito, rechazando y
  auditando toda operación fuera de esos límites.
- **FR-065**: El sistema debe permitir a `GlobalAdmin` asignar, modificar o revocar `Evaluador`
  únicamente con autorización formal de la Oficina de Modernización y el documento correspondiente;
  todo intento incompleto debe rechazarse y auditarse.
- **FR-066**: El sistema debe permitir a `GlobalAdmin` asignar, modificar o revocar `Autoridad`
  únicamente conforme a una designación formal institucional vigente y debe rechazar y auditar toda
  operación sin el documento correspondiente.
- **FR-067**: El sistema debe limitar `Nombre de iniciativa o proyecto` a 500 caracteres, cada sección
  de `Descripción` a 2000, las descripciones de `OTROS` y `Componente digital` a 500 y `Nota` a 1000,
  rechazando cualquier valor que exceda su límite.
- **FR-068**: El sistema debe recortar los espacios extremos de todo contenido textual y rechazarlo
  como vacío cuando, después de recortarlo, contenga únicamente espacios.
- **FR-069**: El sistema debe conservar en registros históricos las referencias PEI y POI retiradas
  de sus catálogos y debe excluirlas de toda nueva selección.
- **FR-070**: Después de presentar una iniciativa, el sistema debe mantener inmutables `Tipo de
  registro`, `Código`, `Código de origen`, `Fecha de inicio` y `Estado`; durante una subsanación solo
  debe permitir al Responsable editar los campos 5 al 12, 22 y 23.
- **FR-071**: El sistema debe permitir al Evaluador registrar el informe de opinión técnica antes de
  la decisión sobre la iniciativa y debe conservar una nueva versión por cada corrección.
- **FR-072**: El sistema debe permitir a la Autoridad o al Evaluador con la decisión formal registrar
  el documento de decisión sobre la iniciativa, exigirlo para aprobar o archivar y mantenerlo
  inmutable después de la transición.
- **FR-073**: El sistema debe permitir a la Autoridad o al Evaluador con la decisión formal registrar
  el documento de aprobación del producto final, exigirlo para `PRODUCTO_APROBADO` y mantenerlo
  inmutable.
- **FR-074**: El sistema debe permitir registrar una colección opcional de documentación de gestión
  del proyecto y no debe tratarla como sustituto de ciclos quincenales ni evidencias obligatorias.
- **FR-075**: El sistema debe exigir `Tipo de producto final aprobado` únicamente para
  `PRODUCTO_APROBADO` y debe limitarlo a los valores del catálogo canónico.
- **FR-076**: El sistema debe permitir al Responsable registrar resultados clave y debe exigir su
  validación por el Evaluador antes de cerrar el proyecto.
- **FR-077**: El sistema debe generar automáticamente `Fecha de cierre` al confirmar una transición
  a `FINALIZADO` o `CANCELADO` y no debe permitir su ingreso manual.
- **FR-078**: El sistema debe permitir al Evaluador registrar el informe final de cierre, exigirlo
  para pasar a `FINALIZADO` y mantenerlo inmutable después de la transición.
- **FR-079**: El sistema debe crear una nueva versión trazable para toda corrección de un documento
  formal y debe impedir sobrescribir o eliminar la versión anterior.
- **FR-080**: Durante `PROYECTO_EJECUCION`, el sistema debe permitir al Responsable editar únicamente
  los campos 17, 19 y 23 dentro de su ámbito y debe auditar cada cambio; cualquier intento de editar
  otro campo debe rechazarse y auditarse.
- **FR-081**: Al crear un proyecto derivado, el sistema debe asignar automáticamente como `Código de
  origen` el código de la iniciativa vinculada.
- **FR-082**: Al crear un proyecto directo, el sistema debe exigir como `Código de origen` el
  identificador del acto formal o de la fuente heredada.
- **FR-083**: El sistema debe exigir que `Fecha de inicio` de todo proyecto coincida con la fecha
  indicada en el documento formal y debe rechazar el uso de la fecha de registro como sustituto.
- **FR-084**: Para crear un proyecto, el sistema debe exigir los campos 1 al 13 y 22 conforme a la
  matriz de proyecto, tratar el campo 23 como opcional y generar `Tipo de registro`, código y estado.
- **FR-085**: Para un proyecto derivado, el sistema debe sugerir el nombre de la iniciativa como
  nombre inicial y permitir definir un nombre propio antes de confirmar la creación.
- **FR-086**: Para un proyecto derivado, el sistema debe copiar `Tipo de solución` desde la iniciativa
  de origen.
- **FR-087**: Cada proyecto debe seleccionar su propio `Objetivo PEI` y `Actividad POI` desde los
  catálogos vigentes al crearse.
- **FR-088**: Para un proyecto derivado, el sistema debe copiar inicialmente las unidades de la
  iniciativa y permitir ajustarlas antes de confirmar, exigiendo una o varias y exactamente una
  principal.
- **FR-089**: Para un proyecto derivado, el sistema debe sugerir como Responsable titular al de la
  iniciativa y permitir cambiarlo antes de confirmar, exigiendo exactamente uno.
- **FR-090**: El sistema debe permitir registrar proyectos directos únicamente a la Autoridad o al
  Evaluador que disponga del documento formal y debe rechazar y auditar a cualquier otro actor.
- **FR-091**: El sistema debe clasificar la información únicamente como `PUBLICO`, `INTERNO` o
  `RESTRINGIDO` y debe rechazar valores fuera de ese catálogo.
- **FR-092**: El sistema debe aplicar `INTERNO` a todo campo no público sin regla especial y limitar
  su consulta a usuarios autorizados dentro de su ámbito.
- **FR-093**: El sistema debe clasificar `Responsable` como `INTERNO` y permitir verlo en consultas
  institucionales autorizadas del ámbito.
- **FR-094**: El sistema debe clasificar los datos de personas participantes como `RESTRINGIDO` y
  permitir su consulta únicamente al Responsable del registro, al Evaluador y a administradores
  autorizados dentro de su ámbito.
- **FR-095**: La consulta pública documental debe limitarse a tipo documental, título sin datos
  personales, versión, formato y fecha de publicación. Debe incluir únicamente versiones cuya
  clasificación `PUBLICO` haya sido validada y cuya publicación haya sido confirmada por el Evaluador;
  debe generar `fechaPublicacion` con la fecha del servidor al confirmar.
- **FR-096**: El sistema debe permitir consultar institucionalmente contenido documental solo cuando
  el ámbito y la clasificación lo permitan y debe impedir toda exposición pública del contenido en
  la Fase 1.
- **FR-097**: El sistema debe identificar como datos personales nombres, documento de identidad,
  correo, teléfono, dirección, firma e identificadores equivalentes para aplicar las restricciones
  de clasificación.
- **FR-098**: El sistema debe limitar un documento con clasificación pendiente o no validada al
  Responsable que lo cargó y al Evaluador, e impedir su uso como evidencia formal.
- **FR-099**: El sistema debe aplicar una reclasificación inmediatamente cuando el Evaluador la
  registre con la decisión formal de la Autoridad.
- **FR-100**: Ante una reclasificación más restrictiva, el sistema debe bloquear accesos futuros
  incompatibles y conservar sin alteración las auditorías de acceso anteriores.
- **FR-101**: El sistema debe permitir a `GlobalAdmin` crear usuarios institucionalmente y a
  `UnidadAdmin` únicamente para personas pertenecientes a sus unidades autorizadas.
- **FR-102**: La activación inicial debe enviarse mediante las acciones de correo de Keycloak y PIIP
  no debe recopilar, procesar ni almacenar contraseñas.
- **FR-103**: El sistema debe permitir a `GlobalAdmin` desactivar usuarios institucionalmente y a
  `UnidadAdmin` únicamente dentro de su ámbito.
- **FR-104**: Al confirmar una desactivación, el sistema debe bloquear inmediatamente el acceso en
  Keycloak y PIIP y conservar usuario, asignaciones e historial.
- **FR-105**: El sistema debe permitir reactivar al usuario únicamente a la misma autoridad que puede
  desactivarlo y no debe restaurar asignaciones vencidas o revocadas.
- **FR-106**: El sistema debe permitir modificar o revocar una asignación `GlobalAdmin` únicamente a
  otro `GlobalAdmin` que disponga de la decisión formal de la Autoridad.
- **FR-107**: El sistema debe rechazar la revocación del último `GlobalAdmin` activo mientras no exista
  un reemplazo formalmente designado y activo.
- **FR-108**: Ante un cambio de unidad, el sistema debe cerrar las asignaciones anteriores y exigir la
  creación explícita de las nuevas sin trasladar permisos automáticamente.
- **FR-109**: El sistema debe bloquear una identidad duplicada cuando coincida el identificador de
  Keycloak o el correo institucional y exigir la resolución del conflicto antes de continuar.
- **FR-110**: El sistema debe auditar toda creación, activación, desactivación, reactivación, cambio de
  unidad y resolución de duplicados con actor, momento, ámbito, persona, operación y resultado.
- **FR-111**: El sistema debe impedir confirmar la admisibilidad mientras falte un campo obligatorio o
  exista un valor que no pertenezca a un catálogo vigente.
- **FR-112**: El sistema debe exigir para la admisibilidad una Ficha de Iniciativa adjunta, con hash
  SHA-256 y clasificación validada.
- **FR-113**: El sistema debe exigir que el Responsable tenga asignación vigente y corresponda a la
  unidad efectiva de la iniciativa.
- **FR-114**: El sistema debe exigir exactamente un Responsable titular y una unidad principal para
  confirmar la admisibilidad.
- **FR-115**: El sistema debe bloquear la admisibilidad ante un posible duplicado hasta que el
  Evaluador registre la resolución del conflicto.
- **FR-116**: La lista de aplicabilidad debe exigir que el problema corresponda a las competencias del
  MIDAGRI o de su ámbito sectorial.
- **FR-117**: La lista de aplicabilidad debe exigir beneficiarios identificados y un resultado público
  esperado.
- **FR-118**: La lista de aplicabilidad debe exigir una solución nueva o sustancialmente mejorada que
  requiera validación.
- **FR-119**: La lista de aplicabilidad debe marcar `NO_APLICABLE` cuando el caso sea únicamente una
  compra, mantenimiento, digitalización sin rediseño o cumplimiento rutinario.
- **FR-120**: El sistema debe exigir al Evaluador completar la lista estructurada, registrar el motivo
  y verificar todos los criterios aplicables antes de confirmar `NO_APLICABLE` o continuar la
  evaluación.
- **FR-121**: El sistema debe exigir fuente, fecha, Responsable, archivo o referencia y hash para
  iniciar una incorporación individual.
- **FR-122**: El sistema debe conservar el código heredado como `Código de origen` y generar un código
  PIIP propio.
- **FR-123**: El sistema debe exigir al Evaluador seleccionar un estado canónico de negocio y adjuntar
  la evidencia que sustenta su correspondencia.
- **FR-124**: El sistema debe mantener en `PENDIENTE` todo registro incompleto y debe impedir tratarlo,
  consultarlo o reportarlo como registro ordinario validado.
- **FR-125**: Ante un duplicado confirmado, el sistema debe impedir crear otro registro y permitir
  vincular de forma auditada la evidencia de origen con el registro existente.
- **FR-126**: El sistema debe bloquear un código reutilizado o conflictivo hasta que el Evaluador
  registre su resolución documentada y no debe renumerarlo automáticamente.
- **FR-127**: El sistema debe bloquear la validación de una relación iniciativa-proyecto inválida
  hasta que sea corregida.
- **FR-128**: El sistema debe permitir correcciones ilimitadas mientras la incorporación permanezca
  `PENDIENTE` y auditar los datos anteriores y nuevos, actor, momento y motivo.
- **FR-129**: El sistema debe administrar `PENDIENTE`, `VALIDADO` y `RECHAZADO` como estados de
  incorporación independientes del estado de negocio.
- **FR-130**: El sistema debe auditar fuente, actores, datos originales, cambios, errores, resolución,
  fechas y hash, conservando la evidencia incluso cuando la incorporación sea `RECHAZADO`.
- **FR-131**: El sistema debe exigir una solicitud documentada y aprobación de la Oficina de
  Modernización antes de permitir que un Evaluador genere un reporte extraordinario.
- **FR-132**: El sistema debe incluir en el reporte semestral totales por tipo, estado, unidad, fuente,
  tipo de solución, producto y cierre.
- **FR-133**: El sistema debe calcular y mostrar indicadores de admisibilidad, aprobación, cierre y
  cumplimiento de ciclos conforme a BR-122, con numerador y denominador, y mostrar no aplicable si
  este último es cero.
- **FR-134**: El sistema debe permitir configurar reportes por periodo, tipo, estado, unidad,
  Responsable, fuente, tipo de solución y producto dentro del ámbito autorizado.
- **FR-135**: El sistema debe generar el PDF oficial y el XLSX de detalle desde el mismo corte,
  parámetros y versión de datos.
- **FR-136**: El sistema debe limitar los destinatarios a autoridades MIDAGRI, Oficina de
  Modernización y PCM-SGP con remisión autorizada.
- **FR-137**: El sistema debe asignar `INTERNO` por defecto al reporte y elevarlo a `RESTRINGIDO` si
  contiene información con esa clasificación.
- **FR-138**: El sistema debe impedir la remisión hasta que la Oficina de Modernización apruebe la
  versión exacta y todos los destinatarios.
- **FR-139**: El sistema debe conservar para cada generación y remisión los parámetros, corte,
  generador, versión, hash, aprobación, destinatario, fecha y resultado.
- **FR-140**: El sistema debe permitir que un usuario `Evaluador` registre las validaciones y la
  aprobación de prototipos en representación de la Oficina de Modernización.
- **FR-141**: El sistema debe exigir la validación de al menos un usuario por cada perfil funcional
  involucrado y de al menos un actor sectorial cuando el recorrido aplique a su ámbito.
- **FR-142**: El sistema debe registrar para cada prototipo la evaluación del recorrido completo,
  reglas de negocio, mensajes de información y error, accesibilidad y privacidad.
- **FR-143**: El sistema debe permitir iteraciones sin límite y mantener el prototipo `OBSERVADO`
  mientras tenga hallazgos críticos o altos sin resolver.
- **FR-144**: El sistema debe identificar cada versión por código, fecha, recorrido, cambios, estado y
  versión anterior.
- **FR-145**: El sistema debe conservar en cada validación el usuario, perfil funcional, escenario,
  resultado, observaciones y aceptación.
- **FR-146**: El sistema debe administrar los estados `BORRADOR`, `EN_VALIDACION`, `OBSERVADO`,
  `VALIDADO`, `APROBADO` y `RECHAZADO` para los prototipos.
- **FR-147**: El sistema debe impedir la aprobación cuando el Evaluador aprobador sea el autor o el
  único validador del prototipo.
- **FR-148**: El sistema debe crear una nueva versión y exigir otra validación y aprobación ante todo
  cambio funcional o de accesibilidad de un prototipo aprobado, conservando la versión anterior como
  historial sin habilitar con ella el cambio.
- **FR-149**: La evidencia de los ocho recorridos debe cubrir escritorio y móvil e identificar los
  resultados de teclado y lector de pantalla para cada componente e interacción aplicable.
- **FR-150**: El sistema debe organizar la medición inicial de los ocho recorridos por etapa de
  implementación.
- **FR-151**: El registro de cada medición debe incluir éxito de tarea, tiempo mediano, errores
  críticos, satisfacción y accesibilidad.
- **FR-152**: El sistema debe vincular cada medición con sus escenarios moderados y la versión exacta
  del prototipo evaluado.
- **FR-153**: El sistema debe registrar una muestra objetivo de cinco usuarios por perfil involucrado
  y exigir una justificación cuando sea menor, sin permitir la omisión de un perfil.
- **FR-154**: El sistema debe exigir datos sintéticos representativos e impedir registrar información
  personal real como datos de prueba.
- **FR-155**: El sistema debe distinguir al equipo de experiencia de usuario que ejecuta y calcula, al
  Evaluador coordinador y a otro Evaluador que aprueba por la Oficina de Modernización.
- **FR-156**: El sistema debe impedir aprobar el prototipo de un recorrido mientras no tenga su
  medición inicial completa y aprobada.
- **FR-157**: El sistema debe conservar versión, muestra, escenarios, resultados, cálculos, hallazgos,
  metas aplicables, aprobación y hash de cada medición.
- **FR-158**: El sistema debe exigir otra medición antes de liberar cada recorrido y después de todo
  cambio funcional o de accesibilidad que lo afecte. La preparación para liberación debe permanecer
  bloqueada mientras la última medición aprobada no corresponda a la versión funcional y de
  accesibilidad candidata.
- **FR-159**: El sistema debe conservar cada reporte y su expediente completo de formatos, versiones,
  aprobaciones, hashes y evidencias de remisión durante el plazo de la tabla de retención documental
  vigente del MIDAGRI. Mientras el plazo no esté confirmado, debe impedir eliminaciones automáticas;
  toda disposición posterior debe exigir autorización, auditoría y tratamiento del expediente
  completo sin eliminación selectiva.
- **FR-160**: El sistema debe exigir una matriz de metas versionada y aprobada por la Oficina de
  Modernización para cada recorrido antes de iniciar la implementación de su interfaz Angular. Debe
  aplicar BR-149, bloquear
  la liberación ante errores críticos o hallazgos críticos o altos de accesibilidad y exigir
  justificación y aprobación documentadas para cualquier excepción permitida en las demás métricas.
- **FR-161**: El sistema debe administrar la matriz cargo o función-perfil-unidad como un catálogo
  configurable y versionado, permitir relaciones múltiples entre funciones, perfiles y unidades
  concretas, y exigir que cada combinación identifique exactamente una función, un perfil y una
  unidad. Cada asignación debe seleccionar una sola combinación vigente y derivar de ella esos tres
  valores, rechazando cualquier dato discordante proporcionado por el cliente.
  Debe aplicar para cada cambio la autoridad de aprobación y el administrador registrador definidos
  para el perfil, conservar las combinaciones históricas inactivas y excluirlas de nuevas asignaciones.
- **FR-162**: El sistema debe administrar Objetivo PEI y Actividad POI como catálogos versionados,
  permitir a `GlobalAdmin` registrar únicamente códigos, descripciones y vigencias contenidos en una
  aprobación formal de la oficina responsable de planeamiento, cargar sus valores iniciales desde una
  semilla aprobada y conservar referencias inactivas solo para históricos, sin sincronización externa.
  Debe versionar PEI y POI de forma independiente y evitar que una operación sobre uno altere la
  versión o vigencia del otro.
- **FR-163**: El sistema debe permitir crear expedientes institucionales para documentos formales que
  no pertenecen al portafolio y exigir que cada serie documental tenga exactamente un propietario,
  sea un registro de portafolio o un expediente institucional. Debe mantener esa pertenencia
  inmutable para todas las versiones, rechazar propietarios simultáneos o ausentes e impedir que los
  expedientes institucionales aparezcan en la consulta pública del portafolio.
- **FR-164**: La autenticación mediante OIDC debe redirigir al tema de inicio de sesión personalizado
  configurado por OGTI en el ambiente OIDC. PIIP no debe versionar sus recursos ni administrar
  credenciales, pero debe verificar el redireccionamiento al ambiente configurado antes de habilitar
  interfaces institucionales.

### Privacy, Security, and Audit Requirements

- **PSA-001**: Los niveles son `PUBLICO`, `INTERNO` y `RESTRINGIDO`. `Tipo de registro`, `Código`,
  `Nombre de iniciativa o proyecto`, `Estado`, tipo documental, título sin datos personales, versión,
  formato y fecha de publicación son `PUBLICO`; toda otra información sin regla especial es
  `INTERNO` dentro del ámbito autorizado, salvo clasificación `RESTRINGIDO`.
- **PSA-002**: Las acciones institucionales deben exigir identidad vigente, permiso funcional y
  ámbito organizacional efectivo asignado explícitamente; una asignación revocada debe dejar de
  autorizar acciones inmediatamente después de confirmarse su revocación.
- **PSA-003**: El sistema debe auditar accesos sensibles y denegados, cambios de negocio,
  transiciones, decisiones, documentos, asignaciones de perfiles y cambios de clasificación.
- **PSA-004**: Cada auditoría debe identificar actor, momento, perfil efectivo, unidad, operación,
  resultado y datos modificados, y no debe poder alterarse.
- **PSA-005**: Los documentos deben conservar autor, fecha, versión, clasificación, integridad y
  binario Oracle; el Responsable propone la clasificación inicial del documento y sus metadatos, y el
  Evaluador la valida. Mientras la clasificación esté pendiente o no validada, solo el Responsable
  cargador y el Evaluador pueden consultarla y no puede utilizarse como evidencia formal ni publicarse.
  OGTI administra fuera de PIIP todos los controles antimalware.
- **PSA-006**: Los documentos formalizados y el historial de transiciones deben ser inmutables; las
  correcciones deben crear una nueva versión o evento trazable.
- **PSA-007**: La consulta pública debe aplicar minimización: solo `Tipo de registro`, `Código`,
  `Nombre de iniciativa o proyecto`, `Estado`, tipo documental, título sin datos personales, versión,
  formato y fecha de publicación, sin contenido ni descarga documental. Los metadatos documentales
  solo pueden exponerse después de que el Evaluador confirme la publicación de una versión con
  clasificación `PUBLICO` validada.
- **PSA-008**: La consulta pública debe ser anónima; cualquier métrica o auditoría debe evitar la
  recopilación de datos personales no necesarios. Se consideran personales nombres, documento de
  identidad, correo, teléfono, dirección, firma e identificadores equivalentes.
- **PSA-009**: Toda reclasificación debe conservar la clasificación anterior, la decisión formal,
  el actor decisor, el actor registrador, el momento, el motivo y el resultado aplicado; surte efecto
  inmediatamente y, si es más restrictiva, bloquea accesos futuros sin eliminar auditorías previas.
- **PSA-010**: Los nombres completos, institución y función de una persona participante sin cuenta
  tienen clasificación `RESTRINGIDO` y solo son visibles para el Responsable del registro, el
  Evaluador y administradores autorizados dentro de su ámbito; no debe recopilarse obligatoriamente
  su documento de identidad ni datos de contacto.
- **PSA-011**: Cada alta o baja de participantes debe auditar actor, momento, proyecto, participante,
  operación y resultado, conservando el historial anterior.
- **PSA-012**: Cada sustitución de Responsable titular debe auditar actor, momento, ámbito, titular
  anterior, titular nuevo y resultado.
- **PSA-013**: Cada intento de asignar `GlobalAdmin` debe auditar actor registrador, Autoridad
  decisora, documento formal, persona asignada, momento y resultado.
- **PSA-014**: La semilla del primer `GlobalAdmin` debe auditar el `sub` beneficiario, perfil, función,
  unidad, Jefatura de Modernización autorizante, aprobación de despliegue, DBA ejecutor, fecha,
  operación y resultado.
- **PSA-015**: Cada suplencia debe auditar titular, suplente, perfil, unidad, fechas, actor que la
  registra, resultado y cualquier terminación anticipada, incluida la autoridad que la decidió.
- **PSA-016**: Cada asignación, modificación o revocación de `Evaluador` debe auditar la Oficina de
  Modernización autorizante, el documento formal, `GlobalAdmin` registrador, persona, ámbito, momento
  y resultado.
- **PSA-017**: Cada asignación, modificación o revocación de `Autoridad` debe auditar la designación
  formal, su vigencia, `GlobalAdmin` registrador, persona, ámbito, momento y resultado.
- **PSA-018**: El contenido documental solo puede mostrarse a usuarios institucionales cuyo ámbito y
  clasificación lo permitan, y nunca puede exponerse públicamente en la Fase 1.
- **PSA-019**: PIIP no debe recopilar, procesar, almacenar, registrar ni auditar contraseñas; la
  activación y las credenciales pertenecen exclusivamente a Keycloak.
- **PSA-020**: Las operaciones del ciclo de identidad deben auditar identificador de Keycloak, correo
  institucional, actor, perfil efectivo, unidad, momento, operación y resultado, sin incluir
  credenciales.
- **PSA-021**: La creación de un expediente institucional, la vinculación de una serie documental y
  todo intento de asignar propietario ausente, múltiple o distinto deben auditar expediente, registro
  de portafolio cuando corresponda, actor, perfil y unidad efectivos, momento, resultado y versión
  documental, sin exponer expedientes institucionales en proyecciones públicas.
- **PSA-022**: El tema de inicio de sesión personalizado de Keycloak es configurado y administrado por
  OGTI fuera del repositorio PIIP. El flujo OIDC debe redirigir al ambiente configurado sin recopilar,
  procesar o almacenar contraseñas en PIIP.
- **PSA-023**: El backend debe validar `issuer`, `audience`, firma y vigencia de cada JWT, y aplicar
  los claims estándar necesarios para comprobarlos antes de autorizar endpoints institucionales. No se
  exige un scope adicional para PIIP; los permisos funcionales y el ámbito se determinan exclusivamente
  mediante la asignación efectiva en Oracle.

### Integration and Data Change Requirements

- **IDC-001**: La Fase 1 no incluye conectores, sincronizaciones ni intercambios automáticos con
  sistemas funcionales externos no aprobados. Keycloak Admin API queda permitido exclusivamente para
  el ciclo de identidad constitucional; no habilita integraciones funcionales adicionales.
- **IDC-002**: La remisión a PCM-SGP exige autorización, aprobación previa de la versión y el
  destinatario, y evidencia del resultado; no implica una integración automática.
- **IDC-003**: La incorporación individual asistida de registros existentes debe preservar origen,
  códigos, relaciones, historial y evidencia de validación sin presentar datos incompletos como
  registros ordinarios validados. El Responsable registra, `UnidadAdmin` asiste y el Evaluador
  valida. Un posible duplicado bloquea la validación hasta que el Evaluador resuelva el conflicto y
  quede evidencia. El código heredado se conserva como origen, los conflictos e inconsistencias
  bloquean la validación y los estados `PENDIENTE`, `VALIDADO` y `RECHAZADO` permanecen separados del
  estado de negocio.
- **IDC-004**: Si existen registros PIIP previos bajo un modelo de registro único, su adecuación a
  iniciativa y proyecto separados es independiente de la carga manual de fuentes externas y debe
  preservar códigos, relación de origen e historial disponible.
- **IDC-005**: Los consumidores institucionales de reportes, consultas y documentos deben mantener
  las mismas restricciones de ámbito y clasificación aplicadas al portafolio.

## Edge Cases *(mandatory)*

- Una persona tiene varios perfiles o unidades y trata de combinar permisos de asignaciones
  distintas o acceder a una unidad descendiente que no le fue asignada explícitamente.
- Una persona intenta continuar operando con una asignación cuya revocación ya fue confirmada.
- Una persona intenta operar antes de la fecha de inicio o después de la fecha de fin de su
  asignación funcional.
- Dos actores intentan registrar simultáneamente decisiones incompatibles; prevalece la primera
  transición confirmada y la otra se rechaza sin sobrescribir el historial.
- El Evaluador intenta registrar una decisión de Autoridad sin documento formal o con documento no
  apto como evidencia.
- El Evaluador intenta usar `NO_APLICABLE` para un incumplimiento formal o `NO_ADMISIBLE` para un
  caso que no corresponde a innovación pública.
- Se intenta confirmar admisibilidad con campos incompletos, catálogos no vigentes, ficha ausente o
  no apta, Responsable sin asignación vigente, cardinalidad inválida o duplicado sin resolver.
- El Evaluador intenta declarar `NO_APLICABLE` sin completar la lista estructurada, sin registrar el
  motivo o pese a cumplir competencia, valor público y carácter innovador sin incurrir en exclusiones.
- Se intenta bloquear una evaluación o decisión únicamente porque `Tipo de solución` conserva el
  valor `POR_DEFINIR`.
- El Evaluador intenta declarar `NO_ADMISIBLE` sin haber concedido la subsanación, sin plazo
  registrado o antes de su vencimiento, o intenta abrir una segunda oportunidad.
- El Responsable intenta editar los campos oficiales de una iniciativa presentada sin una
  subsanación abierta por el Evaluador.
- Durante una subsanación, el Responsable intenta editar un campo distinto del 5 al 12, 22 o 23.
- `UnidadAdmin` intenta confirmar la revocación de una asignación fuera de su ámbito autorizado.
- Se intenta crear un segundo proyecto derivado para una iniciativa que ya tiene uno; la operación
  se rechaza sin alterar el vínculo existente.
- Una iniciativa o proyecto tiene una o varias unidades responsables, pero ninguna o más de una
  está identificada como principal.
- Una iniciativa o proyecto carece de Responsable titular o identifica simultáneamente a más de uno.
- Una iniciativa sin participantes adicionales es rechazada pese a tener un Responsable titular.
- Se intenta registrar un equipo como participante independiente en lugar de asociar a sus personas
  integrantes.
- Se intenta conceder acceso por el solo registro de una persona como participante o duplicar su
  identidad pese a que ya tiene una cuenta PIIP vinculable.
- Se intenta exigir documento de identidad o datos de contacto a una persona participante sin
  cuenta PIIP.
- Se intenta agregar o retirar un participante fuera de `PROYECTO_EJECUCION`, fuera del ámbito del
  Responsable o eliminando el historial de participación.
- `UnidadAdmin` intenta sustituir al Responsable titular fuera de su ámbito o dejando cero o varios
  titulares simultáneos.
- Tras confirmarse una sustitución, el titular anterior intenta continuar actuando con esa
  titularidad.
- Un `UnidadAdmin` intenta modificar su propia asignación o administrar la asignación de otro
  `UnidadAdmin`.
- Se intenta asignar `GlobalAdmin` sin decisión formal de la Autoridad, sin documento o mediante un
  actor distinto de `GlobalAdmin`.
- Se intenta ejecutar la semilla inicial cuando existe cualquier asignación histórica `GlobalAdmin`,
  sin `sub` proporcionado por OGTI o sin aprobación de despliegue de la Jefatura de Modernización.
- Se intenta crear una suplencia sin fecha de inicio o fin, utilizarla fuera de su periodo o
  transferir las credenciales del titular al suplente.
- Una suplencia es autorizada por un actor que no puede autorizar la asignación permanente del mismo
  perfil.
- El titular intenta utilizar la asignación equivalente durante una suplencia, o esta finaliza
  cuando la asignación del titular ya venció o fue revocada.
- Se intenta registrar una segunda suplencia cuyo periodo se superpone con otra para la misma
  asignación de perfil y unidad.
- El titular, el suplente u otro actor intenta terminar anticipadamente una suplencia sin ser la
  autoridad que la autorizó.
- Una operación sensible comienza autorizada, pero la asignación efectiva se revoca, vence o queda
  inactiva antes de aplicar el cambio.
- `UnidadAdmin` intenta asignar, modificar o revocar `Responsable` o `Consulta` fuera de su ámbito.
- Se intenta asignar, modificar o revocar `Evaluador` sin autorización formal de la Oficina de
  Modernización, sin documento o mediante un actor distinto de `GlobalAdmin`.
- Se intenta asignar, modificar o revocar `Autoridad` sin designación formal institucional vigente,
  sin documento o mediante un actor distinto de `GlobalAdmin`.
- Se intenta crear o modificar una combinación función-perfil-unidad sin aprobación de la autoridad
  que autoriza ese perfil, registrarla mediante un administrador no habilitado, usar una combinación
  inactiva en una asignación nueva, asociarla a una unidad distinta de la combinación o combinar dos
  combinaciones en una sola operación.
- `UnidadAdmin` intenta crear, desactivar o reactivar un usuario de una unidad fuera de su ámbito.
- Se intenta activar una cuenta mediante una contraseña gestionada por PIIP en lugar del correo de
  Keycloak.
- Se desactiva un usuario en PIIP pero continúa activo en Keycloak, o viceversa.
- Se reactiva un usuario y se intenta restaurar una asignación vencida o revocada.
- Un `GlobalAdmin` intenta modificar o revocar a otro sin decisión formal de la Autoridad, o intenta
  revocarse a sí mismo como último `GlobalAdmin` activo sin reemplazo.
- Una persona cambia de unidad y se intentan trasladar automáticamente sus permisos anteriores.
- El identificador de Keycloak o correo institucional coincide con una identidad existente y se
  intenta crear un duplicado.
- Una iniciativa o proyecto carece de `Objetivo PEI` o `Actividad POI`, o relaciona más de uno de
  cualquiera de ellos.
- Se intenta presentar una iniciativa sin completar alguno de los campos oficiales 1, 5 al 13 o 22,
  se proporciona manualmente `Fecha de inicio` o un `Código de origen` que no aplica, se exige una
  `Nota` o se intenta exigir durante la presentación un campo reservado a una etapa posterior.
- Se declara `Sí` en `Componente digital` sin proporcionar su descripción.
- Se selecciona `OTROS` en `Fuente u origen` sin describir la fuente.
- Se intenta presentar una iniciativa sin describir el problema público o se intenta exigir una
  solución propuesta.
- Un valor textual supera su longitud máxima, contiene solo espacios o conserva espacios extremos.
- Se intenta cambiar `Tipo de registro` después de presentar una iniciativa.
- Se intenta seleccionar una referencia PEI o POI retirada del catálogo; los registros históricos
  que ya la utilizan deben conservarla.
- `GlobalAdmin` intenta registrar o modificar una referencia PEI o POI sin aprobación formal de la
  oficina responsable de planeamiento, alterar una versión aprobada en lugar de crear otra o cargar
  valores mediante una sincronización externa.
- Se intenta activar, modificar o retirar una versión PEI mediante una operación POI, o viceversa.
- Se intenta adoptar una decisión sobre una iniciativa sin opinión técnica o sobrescribir una
  versión anterior de la opinión.
- Se intenta aprobar o archivar una iniciativa, o aprobar un producto, sin el documento formal
  inmutable correspondiente.
- Se intenta exigir documentación de gestión opcional como sustituto o requisito adicional de los
  ciclos y evidencias obligatorias.
- Se intenta registrar `Tipo de producto final aprobado` para un producto no aprobado o con un valor
  ajeno al catálogo canónico.
- Se intenta cerrar un proyecto con resultados clave no validados por el Evaluador o sin informe
  final inmutable.
- Se intenta ingresar manualmente `Fecha de cierre` o un estado `FINALIZADO` o `CANCELADO` no la
  genera automáticamente.
- Se intenta corregir un documento formal sobrescribiendo o eliminando la versión anterior.
- Durante `PROYECTO_EJECUCION`, el Responsable intenta editar un campo distinto de 17, 19 o 23.
- Un proyecto se declara heredado sin haber iniciado antes de PIIP o sin acto formal, evidencia de
  ejecución, autorización, unidad o responsable.
- Un proyecto derivado no copia el código o tipo de solución de la iniciativa, o se intenta crear sin
  nombre propio, PEI, POI, unidades o Responsable titular confirmados.
- Un proyecto directo carece del identificador del acto formal o fuente heredada, o intenta
  registrarlo un actor distinto de la Autoridad o del Evaluador con documento formal.
- La fecha de inicio del proyecto no coincide con la indicada en el documento formal.
- Se intenta confirmar un proyecto sin completar los campos 1 al 13 y 22, se exige `Nota` o no queda
  exactamente una unidad principal y un Responsable titular.
- Un documento supera 100 MB, tiene formato no permitido, carece de hash SHA-256 o no cumple las
  reglas de clasificación para servir como evidencia formal.
- Un documento o sus metadatos conservan una clasificación inicial propuesta por el Responsable,
  pero todavía no validada por el Evaluador.
- Se intenta crear una serie documental sin propietario, con un registro de portafolio y un expediente
  institucional simultáneamente, o moverla de propietario después de registrar una versión.
- Se intenta publicar metadatos documentales sin confirmación del Evaluador o sin clasificación
  `PUBLICO` validada; la publicación se bloquea y no se genera `fechaPublicacion`.
- Un usuario distinto del Responsable cargador o del Evaluador intenta consultar un documento con
  clasificación pendiente o no validada.
- Un usuario institucional intenta consultar contenido documental fuera de su ámbito o sin
  autorización para su clasificación.
- Un producto no aprobado carece de observación o evidencia.
- Se intenta modificar directamente un ciclo quincenal cerrado en lugar de registrar una nueva
  versión trazable.
- Se intenta cerrar un ciclo quincenal sin objetivos, actividades, avance, dificultades, próximas
  acciones o evidencias.
- Se intenta cerrar un proyecto sin informe final, resultados, aprendizajes, conclusión u
  observación.
- Se intenta reanudar un proyecto suspendido o modificar un estado terminal.
- Una reclasificación haría público otro campo o un metadato con datos personales; la publicación
  del campo o metadato se bloquea.
- Una reclasificación más restrictiva se registra mientras otro usuario conserva una consulta
  abierta; todo acceso posterior debe aplicar inmediatamente la nueva clasificación.
- Un actor distinto de la Autoridad intenta autorizar una reclasificación o el Evaluador intenta
  registrarla sin la decisión formal correspondiente.
- Un registro público se relaciona con documentos o participantes restringidos.
- Un usuario institucional intenta consultar un campo no público fuera de su ámbito organizacional
  o sujeto a una restricción mayor definida por la matriz.
- Durante un registro individual asistido se detectan duplicados, códigos reutilizados o relaciones
  inválidas; la validación se bloquea hasta que el Evaluador resuelva el conflicto y deje evidencia.
- Se intenta considerar ordinario, consultar o reportar un registro cuya incorporación permanece
  `PENDIENTE` o fue `RECHAZADO`.
- Un duplicado confirmado intenta crear un segundo registro en lugar de vincular la evidencia al
  existente.
- Se intenta renumerar automáticamente un código heredado conflictivo o inferir automáticamente el
  estado de negocio sin evidencia del Evaluador.
- Un reporte cruza ámbitos organizacionales, incluye campos no autorizados para su destinatario o
  usa un periodo o fecha de corte distintos de los definidos para el reporte semestral oficial.
- Un usuario sin perfil `Evaluador` intenta generar un reporte institucional.
- La tabla de retención documental no está disponible o su plazo no ha sido confirmado y se intenta
  programar una eliminación automática de reportes.
- Una disposición autorizada intenta eliminar un formato, hash, aprobación o evidencia de remisión
  sin tratar el expediente completo.
- Un prototipo cambia después de ser aprobado y pierde correspondencia con la versión validada.
- No existe una línea base comparable para el tiempo mediano y se intenta exigir la mejora del 20 % en
  lugar de fijar la meta desde la medición inicial.
- Un recorrido presenta un error crítico o un hallazgo crítico o alto de accesibilidad y se intenta
  aprobar una excepción para liberarlo.

## Key Entities *(include when the feature involves data)*

- **Iniciativa**: Propuesta de innovación que inicia en `PRESENTADO`, conserva código e historial
  propios y puede concluir o dar origen a un único proyecto vinculado.
- **Proyecto**: Registro de ejecución derivado o directo, con código, responsable, unidades, estado,
  seguimiento, producto final y cierre propios. Al crearse completa los campos 1 al 13 y 22, admite
  `Nota` opcional, toma la fecha de inicio del documento formal y comienza en `PROYECTO_EJECUCION`.
- **Relación iniciativa-proyecto**: Vínculo inmutable y único por iniciativa que permite consultar
  el origen y la trazabilidad entre registros independientes. En el proyecto derivado, el código de
  origen es el código de la iniciativa y el tipo de solución se copia al confirmar la creación.
- **Participante**: Persona o unidad organizacional relacionada con la presentación, evaluación,
  ejecución, decisión, administración o consulta; una iniciativa puede tener cero o más y su
  participación no le concede la titularidad del Responsable. Un equipo se representa mediante sus
  personas integrantes, no como participante independiente. Una persona participante puede carecer
  de cuenta y acceso PIIP; si ya tiene cuenta, se vincula sin duplicar su identidad. Sin cuenta, sus
  datos mínimos son nombres completos, institución y función. Durante `PROYECTO_EJECUCION`, sus altas
  y bajas son auditadas y no eliminan el historial anterior.
- **Usuario PIIP**: Identidad institucional vinculada al identificador de Keycloak y correo
  institucional, sin credenciales locales. Puede desactivarse conservando asignaciones e historial;
  su reactivación no restaura asignaciones vencidas o revocadas.
- **Incorporación individual**: Proceso auditado para información existente, con estado
  `PENDIENTE`, `VALIDADO` o `RECHAZADO`, fuente, código heredado, evidencia, hash, errores,
  correcciones y resolución independientes del estado de negocio.
- **Responsable titular**: Única persona identificada como Responsable de una iniciativa o proyecto,
  sin perjuicio de sus participantes y unidades responsables. `UnidadAdmin` puede sustituirla dentro
  de su ámbito; la sustitución se hace efectiva inmediatamente al confirmarla, cesa al titular
  anterior y conserva el historial del anterior y del nuevo.
- **Asignación funcional**: Relación vigente entre persona, cargo o función, perfil y unidad que
  determina el permiso y ámbito efectivos y que se evalúa de forma individual en cada operación;
  tiene fecha de inicio obligatoria, fecha de fin opcional y deja de ser efectiva inmediatamente
  después de confirmarse su revocación. `GlobalAdmin` confirma revocaciones institucionales y
  `UnidadAdmin` solo dentro de su ámbito autorizado. Una suplencia es una asignación temporal
  distinta con fechas de inicio y fin obligatorias y nunca transfiere credenciales del titular;
  mientras está vigente, inactiva la asignación del mismo perfil y unidad del titular y no admite
  otra suplencia superpuesta para esa asignación. Cada asignación referencia una única combinación
  vigente de la matriz configurable y versionada de función-perfil-unidad; las combinaciones
  inactivas se conservan solo para trazabilidad histórica.
- **Unidad organizacional**: Ámbito institucional o sectorial sobre el que se registran y consultan
  iniciativas, proyectos y asignaciones. Cada iniciativa o proyecto se relaciona con una o varias
  unidades responsables y distingue exactamente una unidad principal.
- **Evaluación técnica**: Revisión, observaciones y opinión técnica previas a una decisión formal;
  ante incumplimientos formales concede una única subsanación con plazo registrado por el Evaluador.
  La admisibilidad verifica campos, catálogos, ficha, asignación, cardinalidad y duplicados. La
  aplicabilidad usa una lista estructurada de competencia, valor público, carácter innovador y
  exclusiones. La opinión es obligatoria antes de la decisión y toda corrección genera una nueva
  versión.
- **Decisión formal**: Determinación exclusiva de la Autoridad con documento y resultado trazable e
  inmutable; la Autoridad o el Evaluador con el documento puede registrar el resultado.
- **Seguimiento de proyecto**: Planificación, ciclos quincenales, avances, dificultades, productos
  parciales y evidencias registrados durante la ejecución. Cada ciclo contiene objetivos,
  actividades, avance, dificultades, próximas acciones y evidencias; la corrección de un ciclo
  cerrado crea una nueva versión trazable.
- **Producto final**: Resultado presentado para aprobación o no aprobación formal. Si se aprueba,
  exige documento formal inmutable y un tipo del catálogo canónico.
- **Cierre administrativo**: Informe final, resultados, aprendizajes, conclusión y observación que
  permiten alcanzar `FINALIZADO`; el Responsable registra resultados clave, el Evaluador los valida
  y registra el informe final inmutable, y la fecha de cierre se genera automáticamente.
- **Documento o evidencia**: Binario Oracle con tipo, autor, fecha, versión, clasificación, integridad
  SHA-256 y una serie con propietario único. La serie pertenece de forma excluyente e inmutable a un
  registro de portafolio o a un expediente institucional. El Responsable propone su
  clasificación inicial y la de sus metadatos, y el Evaluador la valida. La corrección de un
  documento formal crea una nueva versión y conserva sin cambios las anteriores. El Evaluador solo
  puede confirmar la publicación de una versión con clasificación `PUBLICO` validada; la
  fecha del servidor de esa confirmación fija `fechaPublicacion`.
- **Expediente institucional**: Propietario documental para aprobaciones de catálogos, matrices,
  designaciones y otras decisiones formales sin iniciativa o proyecto. Conserva código, asunto,
  módulo de origen, documentos y auditoría, y nunca se incluye en la consulta pública del portafolio.
- **Transición de estado**: Evento inmutable con estado anterior y nuevo, actor, perfil, unidad,
  fecha, observación y evidencia aplicable.
- **Evento de auditoría**: Evidencia inmutable de acceso, denegación, cambio, transición, documento,
  asignación o clasificación, con actor, momento, ámbito, resultado y datos afectados.
- **Reporte institucional**: Consolidado oficial de enero-junio con corte al 30/06 o de
  julio-diciembre con corte al 31/12, o reporte extraordinario configurable con solicitud documentada
  y aprobación de la Oficina de Modernización. Contiene totales e indicadores o filtros autorizados,
  se genera en PDF oficial y XLSX de detalle por un usuario `Evaluador`, y conserva clasificación,
  parámetros, corte, versión, hash, aprobación, destinatarios y resultado de remisión. Su expediente
  completo se conserva según la tabla de retención documental vigente; sin plazo confirmado no admite
  eliminación automática ni disposición selectiva de evidencias.
- **Clasificación de información**: Regla que clasifica como públicos `Tipo de registro`, `Código`,
  `Nombre de iniciativa o proyecto`, `Estado`, tipo documental, título sin datos personales, versión,
  formato y fecha de publicación. Usa los niveles `PUBLICO`, `INTERNO` y `RESTRINGIDO`; todo campo no
  público sin regla especial es `INTERNO`. Toda reclasificación requiere decisión formal de la
  Autoridad, registro por el Evaluador y aplicación inmediata.
- **Referencia de planeamiento**: Valor controlado de los catálogos `Objetivo PEI` o `Actividad POI`;
  al retirarse deja de estar disponible para nuevas selecciones, pero se conserva en los registros
  históricos que ya lo utilizan. La oficina responsable de planeamiento aprueba códigos,
  descripciones y vigencias, y `GlobalAdmin` registra cada versión aprobada. La carga inicial procede
  de una semilla formalmente aprobada y no de sincronización externa. PEI y POI conservan ciclos de
  versión independientes.
- **Prototipo PIIP**: Versión identificable de un recorrido funcional sometida a validación y
  aprobación de negocio. Registra código, fecha, recorrido, cambios, estado, versión anterior,
  hallazgos y evidencias de validación. Usa `BORRADOR`, `EN_VALIDACION`, `OBSERVADO`, `VALIDADO`,
  `APROBADO` y `RECHAZADO`; los cambios funcionales o de accesibilidad generan una nueva versión. Su
  cobertura identifica escritorio, móvil y las comprobaciones aplicables de teclado y lector de
  pantalla.
- **Medición de experiencia**: Evaluación versionada de un recorrido mediante pruebas moderadas y
  datos sintéticos, con etapa de implementación, muestra por perfil, escenarios, éxito de tarea,
  tiempo mediano, errores críticos, satisfacción, accesibilidad, cálculos, hallazgos, metas aplicables,
  aprobación y hash. Su matriz de metas se aprueba y versiona por recorrido antes de implementar la
  interfaz Angular correspondiente y
  conserva umbrales, línea base comparable, excepciones justificadas y aprobación.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las iniciativas presentadas queda con código propio, estado
  `PRESENTADO`, campos oficiales 1, 5 al 13 y 22 completos, código y fecha de presentación generados
  automáticamente, `Código de origen` vacío e historial identificable; `Componente digital` contiene
  `Sí` o `No` y, cuando contiene `Sí`, incluye una descripción; `Fuente u origen = OTROS` incluye la
  descripción de la fuente. No se exige `Nota` ni campos reservados a evaluación, ejecución,
  producto o cierre.
- **SC-002**: El 100 % de las transiciones evaluadas rechaza actores, estados o evidencias que no
  cumplen las reglas aprobadas.
- **SC-003**: El 100 % de los proyectos derivados permite recorrer desde el proyecto hasta la
  iniciativa, decisión formal, códigos, documentos e historiales relacionados; copia código de origen
  y tipo de solución, y ninguna iniciativa origina más de un proyecto derivado.
- **SC-004**: El 100 % de los proyectos directos aceptados contiene los campos 1 al 13 y 22,
  autorización y evidencias; identifica el acto formal o fuente heredada como código de origen y es
  registrado por la Autoridad o el Evaluador con documento. Todo proyecto heredado acredita inicio
  anterior a PIIP, acto formal y evidencia de ejecución.
- **SC-005**: El 100 % de los resultados `PRODUCTO_NO_APROBADO` contiene observación y evidencia, y
  ambos resultados de producto pueden llegar a `FINALIZADO` solo con cierre completo.
- **SC-006**: En pruebas de privacidad, ninguna consulta institucional muestra registros fuera del
  ámbito autorizado y ninguna consulta pública expone campos distintos de `Tipo de registro`,
  `Código`, `Nombre de iniciativa o proyecto` y `Estado`, salvo tipo documental, título sin datos
  personales, versión, formato y fecha de publicación; tampoco expone contenido ni descargas.
- **SC-007**: Se genera un reporte oficial para enero-junio con corte al 30/06 y otro para
  julio-diciembre con corte al 31/12 por cada año aplicable, además de cada requerimiento autorizado,
  con trazabilidad de periodo, responsable, alcance y fecha de generación.
- **SC-008**: Los ocho recorridos funcionales exigidos por la Constitución cuentan con un prototipo
  PIIP validado y evidencia de aprobación antes de implementar sus interfaces.
- **SC-009**: Los objetivos operativos deben fijarse después de completar la medición inicial conforme
  a BR-149.
- **SC-010**: El 100 % de los recorridos tiene antes de implementar su interfaz Angular una matriz de metas aprobada
  y versionada con éxito de tarea mayor o igual al 90 %, cero errores críticos, satisfacción mayor o
  igual a 4/5, ausencia de hallazgos críticos o altos de accesibilidad y la meta de tiempo definida
  según BR-149.
- **SC-011**: El 100 % de los proyectos en `PROYECTO_EJECUCION` registra el ciclo obligatorio de
  cada periodo quincenal aplicable con objetivos, actividades, avance, dificultades, próximas
  acciones y evidencias completas.
- **SC-012**: El 100 % de los registros de información existente identifica al Responsable que
  registra, a `UnidadAdmin` que asiste y al Evaluador que valida antes de considerarse un registro
  ordinario validado; ningún posible duplicado se valida antes de que el Evaluador resuelva el
  conflicto y quede evidencia de la resolución.
- **SC-013**: El 100 % de las operaciones institucionales se autoriza con una sola asignación
  efectiva y rechaza cualquier intento de combinar permisos de asignaciones distintas.
- **SC-014**: El 100 % de las operaciones de mantenimiento realizadas por participantes sectoriales
  exige el perfil `Responsable` y rechaza registros fuera de las unidades explícitamente asignadas
  de su entidad.
- **SC-015**: El 100 % de las asignaciones funcionales revocadas deja de autorizar operaciones
  inmediatamente después de confirmarse su revocación.
- **SC-016**: El 100 % de las iniciativas y proyectos identifica exactamente una unidad principal,
  incluso cuando tiene varias unidades responsables.
- **SC-017**: El 100 % de las correcciones de ciclos quincenales cerrados crea una nueva versión
  trazable y conserva sin cambios la versión cerrada objeto de corrección.
- **SC-018**: El 100 % de las generaciones de reportes institucionales exige el perfil `Evaluador` y
  deja evidencia de la operación o de su denegación.
- **SC-019**: El 100 % de las operaciones con asignaciones aún no iniciadas, vencidas o revocadas se
  rechaza y queda auditado.
- **SC-020**: El 100 % de las reclasificaciones aplicadas cuenta con decisión formal de la Autoridad,
  registro del Evaluador e historial inmutable; cualquier intento incompleto se rechaza.
- **SC-021**: El 100 % de las transiciones a `NO_ADMISIBLE` acredita una única subsanación, su plazo
  registrado y el vencimiento con incumplimientos formales; toda transición anticipada se rechaza.
- **SC-022**: El 100 % de las evaluaciones y decisiones sobre iniciativas con `Tipo de solución =
  POR_DEFINIR` puede continuar sin exigir un plazo de resolución de ese valor.
- **SC-023**: El 100 % de las iniciativas y proyectos identifica exactamente un Responsable titular
  y rechaza la ausencia o coexistencia de varios titulares.
- **SC-024**: El 100 % de las iniciativas y proyectos identifica exactamente un `Objetivo PEI` y una
  `Actividad POI` y rechaza cardinalidades distintas.
- **SC-025**: El 100 % de las iniciativas presentadas describe el problema público y ninguna se
  rechaza únicamente por no incluir una solución propuesta.
- **SC-026**: El 100 % de las iniciativas con Responsable titular puede presentarse con cero o más
  participantes adicionales, sin que su ausencia provoque rechazo.
- **SC-027**: El 100 % de los participantes corresponde a una persona o unidad organizacional y
  ningún equipo se registra como participante independiente.
- **SC-028**: El 100 % de las personas participantes sin cuenta permanece sin acceso, y toda persona
  participante que ya tenga cuenta PIIP se vincula sin crear una identidad duplicada.
- **SC-029**: El 100 % de las personas participantes sin cuenta PIIP puede registrarse con nombres
  completos, institución y función, sin que se exija documento de identidad ni datos de contacto.
- **SC-030**: El 100 % de las altas y bajas de participantes durante `PROYECTO_EJECUCION` conserva el
  historial anterior y genera evidencia de auditoría.
- **SC-031**: El 100 % de las sustituciones de Responsable titular es realizado por `UnidadAdmin`
  dentro de su ámbito, se hace efectivo inmediatamente al confirmarse, conserva exactamente un
  titular y deja historial inmutable del cambio.
- **SC-032**: El 100 % de las asignaciones, modificaciones y revocaciones del perfil `UnidadAdmin`
  exige `GlobalAdmin`; todo intento de otro perfil se rechaza y queda auditado.
- **SC-033**: El 100 % de las nuevas asignaciones `GlobalAdmin` cuenta con decisión formal de la
  Autoridad, documento correspondiente y registro por un `GlobalAdmin`; todo intento incompleto se
  rechaza y queda auditado.
- **SC-034**: El primer `GlobalAdmin` solo puede aprovisionarse cuando nunca existió otro, cuenta con
  el `sub` proporcionado por el administrador Keycloak de OGTI y la aprobación de despliegue de la
  Jefatura de Modernización. La semilla manual reutiliza `MIDAGRI`, crea `ADMINISTRADOR_PIIP`, su combinación y
  la asignación con auditoría mínima, y toda reejecución o existencia histórica aborta sin cambios.
- **SC-035**: El 100 % de las suplencias tiene fecha de inicio y fin, solo autoriza dentro de ese
  periodo y utiliza las credenciales propias del suplente.
- **SC-036**: El 100 % de las suplencias es autorizado por la misma autoridad que la asignación
  permanente del perfil; toda autorización por otro actor se rechaza y queda auditada.
- **SC-037**: El 100 % de las suplencias vigentes impide usar la asignación equivalente del titular;
  al finalizar, solo se reactiva una asignación que continúa vigente y no fue revocada.
- **SC-038**: El 100 % de las suplencias superpuestas para la misma asignación de perfil y unidad se
  rechaza y queda auditado.
- **SC-039**: El 100 % de las terminaciones anticipadas de suplencias es confirmado por la misma
  autoridad que las autorizó, cesa inmediatamente al suplente y solo reactiva una asignación vigente
  y no revocada del titular.
- **SC-040**: El 100 % de las operaciones sensibles revalida la asignación efectiva antes de aplicar
  el cambio y rechaza de forma auditada cualquier asignación revocada, vencida o inactiva.
- **SC-041**: El 100 % de las asignaciones, modificaciones y revocaciones de `Responsable` y
  `Consulta` es realizado por `GlobalAdmin` institucionalmente o `UnidadAdmin` dentro de su ámbito;
  toda operación fuera de esos límites se rechaza y audita.
- **SC-042**: El 100 % de las asignaciones, modificaciones y revocaciones de `Evaluador` cuenta con
  autorización formal de la Oficina de Modernización, documento y registro por `GlobalAdmin`; todo
  intento incompleto se rechaza y audita.
- **SC-043**: El 100 % de las asignaciones, modificaciones y revocaciones de `Autoridad` es registrado
  por `GlobalAdmin` conforme a una designación formal institucional vigente; todo intento incompleto
  se rechaza y audita.
- **SC-044**: El 100 % de los campos textuales aplica los límites de 500 caracteres para nombre, 2000
  para cada sección de descripción, 500 para detalles de `OTROS` y componente digital, y 1000 para
  nota; todo exceso se rechaza.
- **SC-045**: El 100 % del contenido textual se conserva sin espacios extremos y se rechaza cuando,
  después de recortarlo, queda vacío.
- **SC-046**: El 100 % de las referencias PEI y POI nuevas pertenece a catálogos vigentes; toda
  referencia retirada se conserva en registros históricos y se excluye de nuevas selecciones.
- **SC-047**: El 100 % de las subsanaciones permite al Responsable editar únicamente los campos 5 al
  12, 22 y 23 y mantiene inmutable `Tipo de registro`.
- **SC-048**: El 100 % de las decisiones sobre iniciativas cuenta con opinión técnica previa; toda
  corrección de la opinión crea una nueva versión y conserva las anteriores.
- **SC-049**: El 100 % de las aprobaciones o archivos de iniciativas cuenta con documento formal
  inmutable registrado por la Autoridad o el Evaluador autorizado.
- **SC-050**: El 100 % de los estados `PRODUCTO_APROBADO` cuenta con documento formal inmutable y un
  tipo de producto del catálogo canónico.
- **SC-051**: El 100 % de los proyectos puede operar sin documentación de gestión opcional cuando
  cumple sus ciclos y evidencias obligatorias.
- **SC-052**: El 100 % de los cierres cuenta con resultados clave registrados por el Responsable y
  validados por el Evaluador.
- **SC-053**: El 100 % de las transiciones a `FINALIZADO` o `CANCELADO` genera automáticamente la
  fecha de cierre y rechaza su ingreso manual.
- **SC-054**: El 100 % de las transiciones a `FINALIZADO` cuenta con informe final registrado por el
  Evaluador y conservado de forma inmutable.
- **SC-055**: El 100 % de las correcciones de documentos formales crea una nueva versión trazable y
  conserva sin cambios todas las versiones anteriores.
- **SC-056**: El 100 % de las modificaciones realizadas por el Responsable durante
  `PROYECTO_EJECUCION` afecta únicamente los campos 17, 19 o 23 y queda auditado; todo intento sobre
  otro campo se rechaza y audita.
- **SC-057**: El 100 % de los proyectos derivados asigna automáticamente el código de la iniciativa
  como código de origen y copia su tipo de solución.
- **SC-058**: El 100 % de los proyectos directos exige como código de origen el identificador del
  acto formal o fuente heredada.
- **SC-059**: El 100 % de los proyectos usa como fecha de inicio la indicada en el documento formal y
  rechaza una fecha de registro usada como sustituto.
- **SC-060**: El 100 % de los proyectos creados completa los campos 1 al 13 y 22, trata `Nota` como
  opcional, recibe código propio y comienza en `PROYECTO_EJECUCION`.
- **SC-061**: El 100 % de los proyectos derivados tiene nombre propio confirmado y permite ajustar
  la sugerencia inicial antes de su creación.
- **SC-062**: El 100 % de los proyectos selecciona su propio `Objetivo PEI` y `Actividad POI` desde
  catálogos vigentes.
- **SC-063**: El 100 % de los proyectos derivados permite ajustar antes de confirmar las unidades
  copiadas y conserva una o varias con exactamente una principal.
- **SC-064**: El 100 % de los proyectos derivados permite confirmar o cambiar antes de crear el
  Responsable sugerido y conserva exactamente un titular.
- **SC-065**: El 100 % de los proyectos directos es registrado por la Autoridad o el Evaluador con el
  documento formal; todo intento de otro actor se rechaza y audita.
- **SC-066**: El 100 % de los campos usa únicamente `PUBLICO`, `INTERNO` o `RESTRINGIDO`; todo campo no
  público sin regla especial recibe `INTERNO`.
- **SC-067**: El 100 % de las consultas institucionales de `Responsable` respeta el ámbito y su
  clasificación `INTERNO`.
- **SC-068**: El 100 % de los accesos a datos de personas participantes exige ser Responsable del
  registro, Evaluador o administrador autorizado dentro del ámbito; cualquier otro acceso se
  rechaza y audita.
- **SC-069**: El 100 % de las consultas públicas documentales se limita a tipo, título sin datos
  personales, versión, formato y fecha de publicación.
- **SC-070**: El 100 % de las consultas de contenido documental exige ámbito y clasificación
  autorizados; ninguna consulta pública expone contenido en la Fase 1.
- **SC-071**: El 100 % de los controles de datos personales reconoce nombres, documento de identidad,
  correo, teléfono, dirección, firma e identificadores equivalentes.
- **SC-072**: El 100 % de los documentos con clasificación pendiente o no validada solo es accesible
  al Responsable cargador y al Evaluador y no puede utilizarse como evidencia formal.
- **SC-073**: El 100 % de las reclasificaciones se aplica inmediatamente al registrarse con decisión
  formal.
- **SC-074**: El 100 % de las reclasificaciones más restrictivas bloquea accesos futuros
  incompatibles y conserva las auditorías anteriores.
- **SC-075**: El 100 % de las creaciones de usuario es realizado por `GlobalAdmin` institucionalmente
  o por `UnidadAdmin` para una persona de sus unidades autorizadas.
- **SC-076**: El 100 % de las activaciones iniciales utiliza correo de Keycloak y ninguna operación de
  PIIP recopila, procesa o almacena contraseñas.
- **SC-077**: El 100 % de las desactivaciones bloquea inmediatamente al usuario en Keycloak y PIIP y
  conserva su identidad, asignaciones e historial.
- **SC-078**: El 100 % de las reactivaciones es realizado por una autoridad habilitada y no restaura
  asignaciones vencidas o revocadas.
- **SC-079**: El 100 % de las modificaciones o revocaciones de `GlobalAdmin` cuenta con decisión
  formal de la Autoridad y registro por otro `GlobalAdmin`.
- **SC-080**: El 100 % de los intentos de revocar al último `GlobalAdmin` activo sin reemplazo se
  rechaza y audita.
- **SC-081**: El 100 % de los cambios de unidad cierra asignaciones anteriores y exige nuevas
  asignaciones explícitas sin trasladar permisos.
- **SC-082**: El 100 % de las coincidencias por identificador de Keycloak o correo institucional
  bloquea la creación de una identidad duplicada hasta resolver el conflicto.
- **SC-083**: El 100 % de las operaciones del ciclo de identidad deja auditoría de actor, momento,
  ámbito, persona, operación y resultado sin registrar credenciales.
- **SC-084**: El 100 % de las iniciativas admitidas tiene campos obligatorios completos y valores de
  catálogos vigentes.
- **SC-085**: El 100 % de las iniciativas admitidas cuenta con Ficha de Iniciativa adjunta, integridad
  SHA-256 y clasificación validada.
- **SC-086**: El 100 % de las iniciativas admitidas tiene Responsable con asignación vigente dentro
  de la unidad efectiva.
- **SC-087**: El 100 % de las iniciativas admitidas tiene exactamente un Responsable titular y una
  unidad principal.
- **SC-088**: El 100 % de los posibles duplicados bloquea la admisibilidad hasta que el Evaluador
  registra la resolución.
- **SC-089**: El 100 % de las iniciativas aplicables corresponde a competencias del MIDAGRI o de su
  ámbito sectorial.
- **SC-090**: El 100 % de las iniciativas aplicables identifica beneficiarios y un resultado público
  esperado.
- **SC-091**: El 100 % de las iniciativas aplicables propone una solución nueva o sustancialmente
  mejorada que requiere validación.
- **SC-092**: El 100 % de los casos que consisten únicamente en compra, mantenimiento, digitalización
  sin rediseño o cumplimiento rutinario se clasifica `NO_APLICABLE`.
- **SC-093**: El 100 % de las decisiones de aplicabilidad completa la lista estructurada y registra el
  motivo; toda decisión incompleta se rechaza.
- **SC-094**: El 100 % de las incorporaciones identifica fuente, fecha, Responsable, archivo o
  referencia y hash.
- **SC-095**: El 100 % de los registros heredados conserva su código anterior como `Código de origen`
  y recibe un código PIIP propio.
- **SC-096**: El 100 % de los estados de negocio heredados es seleccionado por el Evaluador y cuenta
  con evidencia de correspondencia.
- **SC-097**: El 100 % de los registros incompletos permanece `PENDIENTE` y no aparece como registro
  ordinario validado en consultas o reportes.
- **SC-098**: El 100 % de los duplicados confirmados evita crear otro registro y vincula la evidencia
  al registro existente.
- **SC-099**: El 100 % de los códigos reutilizados o conflictivos bloquea la validación hasta una
  resolución documentada y ninguno se renumera automáticamente.
- **SC-100**: El 100 % de las relaciones iniciativa-proyecto inválidas bloquea la validación hasta su
  corrección.
- **SC-101**: El 100 % de las correcciones previas a la validación conserva datos anteriores y nuevos,
  actor, momento y motivo, sin límite de intentos mientras permanezca `PENDIENTE`.
- **SC-102**: El 100 % de las incorporaciones usa `PENDIENTE`, `VALIDADO` o `RECHAZADO` sin alterar el
  catálogo de estados de negocio.
- **SC-103**: El 100 % de las incorporaciones conserva fuente, actores, datos originales, cambios,
  errores, resolución, fechas y hash incluso cuando resulta `RECHAZADO`.
- **SC-104**: El 100 % de los reportes extraordinarios cuenta con solicitud documentada, aprobación de
  la Oficina de Modernización y generación por un usuario `Evaluador`.
- **SC-105**: El 100 % de los reportes semestrales contiene totales por tipo, estado, unidad, fuente,
  tipo de solución, producto y cierre.
- **SC-106**: El 100 % de los indicadores de admisibilidad, aprobación, cierre y cumplimiento de ciclos
  aplica BR-122, muestra numerador y denominador, y usa no aplicable cuando el denominador es cero.
- **SC-107**: El 100 % de los filtros de reportes configurables se limita a periodo, tipo, estado,
  unidad, Responsable, fuente, tipo de solución y producto dentro del ámbito autorizado.
- **SC-108**: El 100 % de los reportes genera un PDF oficial y un XLSX de detalle con el mismo corte,
  parámetros y versión de datos.
- **SC-109**: El 100 % de las remisiones se limita a autoridades MIDAGRI, Oficina de Modernización o
  PCM-SGP con autorización vigente.
- **SC-110**: El 100 % de los reportes se clasifica `INTERNO` o `RESTRINGIDO` según BR-126 y rechaza
  accesos incompatibles.
- **SC-111**: El 100 % de las remisiones cuenta previamente con aprobación de la Oficina de
  Modernización para la versión exacta y todos sus destinatarios.
- **SC-112**: El 100 % de las generaciones y remisiones conserva parámetros, corte, generador,
  versión, hash, aprobación, destinatario, fecha y resultado.
- **SC-113**: El 100 % de las validaciones y aprobaciones de prototipos queda registrado por un
  usuario con perfil `Evaluador`.
- **SC-114**: El 100 % de los prototipos cuenta con al menos un validador por perfil funcional
  involucrado y un actor sectorial cuando el recorrido aplica a ese ámbito.
- **SC-115**: El 100 % de las validaciones cubre recorrido completo, reglas, mensajes, accesibilidad y
  privacidad.
- **SC-116**: El 100 % de los prototipos con hallazgos críticos o altos permanece `OBSERVADO` y no
  limita la cantidad de iteraciones necesarias para resolverlos.
- **SC-117**: El 100 % de las versiones identifica código, fecha, recorrido, cambios, estado y versión
  anterior.
- **SC-118**: El 100 % de las validaciones conserva usuario, perfil, escenario, resultado,
  observaciones y aceptación.
- **SC-119**: El 100 % de los prototipos utiliza únicamente `BORRADOR`, `EN_VALIDACION`, `OBSERVADO`,
  `VALIDADO`, `APROBADO` o `RECHAZADO`.
- **SC-120**: El 100 % de las aprobaciones rechaza que el aprobador sea el autor o el único validador.
- **SC-121**: El 100 % de los cambios funcionales o de accesibilidad sobre versiones aprobadas crea
  una nueva versión, exige revalidación y conserva la anterior solo como historial.
- **SC-122**: El 100 % de los ocho recorridos cuenta con evidencia en escritorio y móvil y con
  resultados de teclado y lector de pantalla para cada componente e interacción aplicable.
- **SC-123**: El 100 % de los ocho recorridos forma parte de una medición inicial agrupada por su etapa
  de implementación.
- **SC-124**: El 100 % de las mediciones registra éxito de tarea, tiempo mediano, errores críticos,
  satisfacción y accesibilidad.
- **SC-125**: El 100 % de las mediciones identifica sus escenarios moderados y la versión exacta del
  prototipo.
- **SC-126**: El 100 % de las muestras busca cinco usuarios por perfil; toda cantidad menor contiene
  una justificación y ninguna medición omite un perfil involucrado.
- **SC-127**: El 100 % de las pruebas usa datos sintéticos representativos y no contiene información
  personal real.
- **SC-128**: El 100 % de las mediciones identifica al equipo ejecutor, al Evaluador coordinador y a
  otro Evaluador aprobador.
- **SC-129**: El 100 % de los prototipos aprobados cuenta previamente con una medición inicial completa
  y aprobada.
- **SC-130**: El 100 % de las mediciones conserva versión, muestra, escenarios, resultados, cálculos,
  hallazgos, metas aplicables, aprobación y hash.
- **SC-131**: El 100 % de los recorridos se mide antes de liberarse y vuelve a medirse después de cada
  cambio funcional o de accesibilidad que lo afecte.
- **SC-132**: El 100 % de los reportes conserva junto con su expediente los formatos, versiones,
  aprobaciones, hashes y evidencias de remisión durante el plazo de la tabla de retención documental
  vigente; cuando el plazo no está confirmado, ninguna eliminación automática se ejecuta.
- **SC-133**: El 100 % de las disposiciones de reportes está autorizado y auditado, afecta el
  expediente completo y rechaza la eliminación selectiva de evidencias.
- **SC-134**: El 100 % de las líneas base comparables fija una mejora mínima del 20 % en tiempo
  mediano; cuando no existe comparación válida, la meta queda fijada desde la medición inicial.
- **SC-135**: El 100 % de los errores críticos y hallazgos críticos o altos de accesibilidad bloquea la
  liberación; toda excepción sobre las demás métricas contiene justificación y aprobación documentadas.
- **SC-136**: El 100 % de las asignaciones nuevas referencia una sola combinación vigente de la matriz
  función-perfil-unidad y una unidad explícita; todo cambio de la matriz conserva versión, autoridad
  aprobadora, administrador registrador e historial, y rechaza combinaciones inactivas o no aprobadas.
- **SC-137**: El 100 % de las altas, cambios e inactivaciones de Objetivo PEI y Actividad POI cuenta
  con aprobación formal de la oficina responsable de planeamiento y registro por `GlobalAdmin`; las
  nuevas selecciones rechazan referencias inactivas, los históricos las conservan sin alteración y
  ninguna versión PEI modifica una versión POI ni viceversa.
- **SC-138**: El 100 % de las series documentales tiene exactamente un propietario inmutable,
  portafolio o expediente institucional; todo propietario ausente o simultáneo se rechaza y ningún
  documento de expediente institucional aparece en la consulta pública del portafolio.
- **SC-139**: El 100 % de los inicios de sesión institucionales redirige al ambiente Keycloak configurado
  con su tema personalizado, y ninguna interfaz PIIP recopila, procesa o almacena contraseñas.
- **SC-140**: El 100 % de los JWT usados en endpoints institucionales valida `issuer`, `audience`,
  firma, vigencia y los claims estándar necesarios; los tokens inválidos se rechazan antes de evaluar la
  asignación efectiva.

## Assumptions and Clarifications

- La Fase 1 centraliza la operación interna y sectorial sin sincronizaciones automáticas con sistemas
  funcionales externos; Keycloak Admin API se limita al ciclo de identidad constitucional.
- Se aplican los catálogos, estados, documentos y reglas constitucionales vigentes en la versión
  4.0.0.
- Los detalles visuales que no afecten accesibilidad, autorización o comportamiento se resolverán
  durante la validación de prototipos.
- La incorporación inicial será individual: el Responsable registra, `UnidadAdmin` asiste y el
  Evaluador valida; no habrá carga conjunta o masiva en Fase 1.
- La incorporación exige fuente, fecha, Responsable, archivo o referencia y hash; conserva el código
  heredado como origen y genera un código PIIP.
- El Evaluador selecciona con evidencia el estado canónico de negocio; `PENDIENTE`, `VALIDADO` y
  `RECHAZADO` son estados independientes de la incorporación.
- Datos incompletos, códigos conflictivos y relaciones inválidas bloquean la validación. Un duplicado
  confirmado no crea otro registro y vincula su evidencia al existente.
- Mientras permanezca `PENDIENTE`, admite correcciones ilimitadas y auditadas. La auditoría conserva
  fuente, actores, datos originales, cambios, errores, resolución, fechas y hash.
- `GlobalAdmin` administra el ámbito institucional y `UnidadAdmin` sus unidades autorizadas.
- Ninguna asignación se extiende implícitamente a unidades descendientes; cada unidad requiere una
  asignación explícita.
- Cada operación usa una sola asignación efectiva y no combina permisos entre perfiles o unidades.
- La revocación de una asignación funcional surte efecto inmediatamente después de confirmarse.
- Cada asignación funcional tiene fecha de inicio obligatoria y fecha de fin opcional.
- La matriz cargo o función-perfil-unidad es un catálogo configurable y versionado con relaciones
  múltiples. Cada combinación relaciona exactamente una función, un perfil y una unidad concreta, y
  cada asignación selecciona una sola combinación vigente y deriva de ella esos valores; cada
  cambio de combinación es aprobado por la autoridad que autoriza el perfil y registrado por el
  administrador habilitado para ese perfil, conservando las versiones inactivas en históricos.
- `GlobalAdmin` confirma revocaciones institucionales y `UnidadAdmin` únicamente dentro de su
  ámbito autorizado.
- Los participantes sectoriales mantienen registros con `Responsable` y solo en unidades de su
  entidad asignadas explícitamente.
- La consulta pública será anónima y quedará limitada a `Tipo de registro`, `Código`, `Nombre de
  iniciativa o proyecto`, `Estado`, tipo documental, título sin datos personales, versión, formato y
  fecha de publicación.
- Los niveles son `PUBLICO`, `INTERNO` y `RESTRINGIDO`; todo campo no público sin regla especial es
  `INTERNO` y queda limitado al ámbito autorizado.
- `Responsable` es `INTERNO`; las personas participantes son `RESTRINGIDO` y solo las consultan el
  Responsable del registro, el Evaluador y administradores autorizados dentro de su ámbito.
- La Autoridad decide toda reclasificación de privacidad y el Evaluador la registra con la decisión
  formal correspondiente; surte efecto inmediatamente y, si es más restrictiva, bloquea accesos
  futuros sin eliminar auditorías anteriores.
- El Responsable propone la clasificación inicial de cada documento y sus metadatos, y el Evaluador
  la valida.
- El Evaluador confirma la publicación documental únicamente para una versión con clasificación
  `PUBLICO` validada; `fechaPublicacion` corresponde a la fecha del servidor al
  confirmar. Una reclasificación a `PUBLICO` conserva el requisito de decisión formal de la
  Autoridad.
- Un documento con clasificación pendiente o no validada solo es visible para el Responsable
  cargador y el Evaluador y no sirve como evidencia formal.
- El contenido documental solo es visible institucionalmente según ámbito y clasificación y nunca se
  expone públicamente en la Fase 1.
- Son datos personales detectables nombres, documento de identidad, correo, teléfono, dirección,
  firma e identificadores equivalentes.
- `NO_ADMISIBLE` y `NO_APLICABLE` requieren observación; un documento adicional es opcional.
- Antes de declarar `NO_ADMISIBLE`, el Evaluador concede una única subsanación y registra su plazo.
- Después de presentar una iniciativa, el Responsable solo edita los campos editables dentro del
  conjunto 5 al 12, 22 y 23 durante esa única subsanación abierta; `Tipo de registro`, código, código
  de origen, fecha de inicio y estado permanecen inmutables.
- `NO_ADMISIBLE` identifica incumplimientos de requisitos formales y `NO_APLICABLE` identifica casos
  que no corresponden al proceso de innovación pública.
- La admisibilidad exige campos y catálogos válidos, ficha adjunta con integridad SHA-256 y
  clasificación validada, Responsable vigente dentro de su unidad, exactamente un titular y una
  unidad principal, y ausencia de duplicados sin resolver.
- La aplicabilidad exige competencia MIDAGRI o sectorial, beneficiarios y resultado público
  esperado, y una solución nueva o sustancialmente mejorada que requiera validación.
- Son `NO_APLICABLE` los casos que consisten únicamente en compra, mantenimiento, digitalización sin
  rediseño o cumplimiento rutinario; el Evaluador completa una lista estructurada y registra el
  motivo.
- La Autoridad autoriza formalmente la incorporación de todo proyecto directo; la Autoridad o el
  Evaluador con el documento formal lo registra.
- Un proyecto heredado debe haber iniciado antes de PIIP y acreditar acto formal y evidencia de
  ejecución.
- Cada iniciativa aprobada puede originar exactamente un proyecto derivado.
- Los ciclos de trabajo serán quincenales y obligatorios.
- Cada ciclo quincenal registra objetivos, actividades, avance, dificultades, próximas acciones y
  evidencias.
- Cada iniciativa o proyecto puede tener una o varias unidades responsables, con exactamente una
  unidad principal.
- Cada iniciativa o proyecto tiene exactamente un Responsable titular; los demás participantes no
  comparten esa titularidad.
- Cada iniciativa o proyecto se relaciona con exactamente un `Objetivo PEI` y una `Actividad POI`.
- `Descripción` exige el problema público y admite una solución propuesta opcional.
- Una iniciativa puede presentarse con cero o más participantes adicionales al Responsable titular.
- Los participantes son personas o unidades organizacionales; los equipos se representan mediante
  sus integrantes.
- Una persona participante no requiere cuenta ni recibe acceso por su participación; si ya tiene
  cuenta PIIP, se vincula sin duplicar su identidad.
- Una persona participante sin cuenta PIIP registra como mínimo nombres completos, institución y
  función; no requiere documento de identidad ni datos de contacto.
- Durante `PROYECTO_EJECUCION`, el Responsable puede realizar altas y bajas auditadas de
  participantes sin eliminar el historial anterior.
- `UnidadAdmin` puede sustituir al Responsable titular únicamente dentro de su ámbito, conservando
  exactamente un titular y el historial del cambio; la sustitución se hace efectiva inmediatamente
  al confirmarla y cesa al titular anterior.
- Únicamente `GlobalAdmin` puede asignar, modificar o revocar el perfil `UnidadAdmin`.
- Toda nueva asignación `GlobalAdmin` requiere decisión formal de la Autoridad y registro por un
  `GlobalAdmin` con el documento correspondiente.
- El primer `GlobalAdmin`, cuando nunca existió una asignación de ese perfil, se crea mediante semilla
  SQL manual con `sub` proporcionado por OGTI, aprobación de despliegue de la Jefatura de
  Modernización, valores iniciales canónicos y auditoría mínima.
- Una suplencia es una asignación temporal distinta con fecha de inicio y fin obligatorias y no
  transfiere ni comparte credenciales del titular.
- La suplencia es autorizada por la misma autoridad que autoriza la asignación permanente del perfil.
- Durante la suplencia, la asignación del mismo perfil y unidad del titular queda temporalmente
  inactiva y se reactiva al finalizar solo si continúa vigente y no fue revocada.
- No se admiten suplencias con periodos superpuestos para la misma asignación de perfil y unidad.
- La misma autoridad que autorizó una suplencia puede terminarla anticipadamente; el cese es
  inmediato y la asignación del titular solo se reactiva si continúa vigente y no fue revocada.
- Toda operación sensible revalida la asignación efectiva antes de aplicar el cambio y se rechaza si
  fue revocada, venció o quedó inactiva, aunque hubiera comenzado autorizada.
- `GlobalAdmin` administra `Responsable` y `Consulta` institucionalmente; `UnidadAdmin` solo dentro de
  su ámbito autorizado.
- La Oficina de Modernización autoriza las asignaciones, modificaciones y revocaciones de
  `Evaluador`; `GlobalAdmin` las registra con documento formal.
- `GlobalAdmin` asigna, modifica o revoca `Autoridad` conforme a una designación formal institucional
  vigente.
- `GlobalAdmin` crea, desactiva y reactiva usuarios institucionalmente; `UnidadAdmin` solo para
  personas de sus unidades autorizadas.
- La activación inicial usa correo de Keycloak y PIIP no gestiona contraseñas.
- OGTI configura y administra fuera del repositorio el tema de inicio de sesión personalizado de
  Keycloak; PIIP solo verifica el redireccionamiento OIDC hacia el ambiente aprobado.
- La desactivación es inmediata en Keycloak y PIIP y conserva usuario, asignaciones e historial; la
  reactivación no restaura asignaciones vencidas o revocadas.
- Modificar o revocar `GlobalAdmin` requiere decisión formal de la Autoridad y registro por otro
  `GlobalAdmin`; el último activo no puede revocarse sin reemplazo designado y activo.
- Un cambio de unidad cierra asignaciones anteriores y exige crear las nuevas explícitamente, sin
  trasladar permisos.
- Una coincidencia por identificador de Keycloak o correo institucional bloquea la identidad
  duplicada hasta resolver el conflicto.
- La presentación de una iniciativa exige ingresar los campos oficiales 1, 5 al 13 y 22; el código y
  la fecha de presentación se generan automáticamente al confirmarla, `Código de origen` no aplica,
  `Nota` es opcional y los campos 14 al 21 se completan en evaluación, ejecución, producto o cierre.
- `Componente digital` se registra como `Sí` o `No` y requiere descripción cuando su valor es `Sí`.
- `Fuente u origen = OTROS` requiere una descripción de la fuente.
- `Tipo de solución = POR_DEFINIR` puede conservarse sin plazo obligatorio de resolución y no
  bloquea la evaluación ni la decisión de la Autoridad.
- `Nombre de iniciativa o proyecto` admite hasta 500 caracteres; cada sección de `Descripción`,
  hasta 2000; los detalles de `OTROS` y componente digital, hasta 500; y `Nota`, hasta 1000.
- Todo texto se recorta en sus extremos y se rechaza si, después de recortarlo, contiene solo
  espacios.
- `Objetivo PEI` y `Actividad POI` proceden de catálogos controlados vigentes; las referencias
  retiradas se conservan históricamente y se excluyen de nuevas selecciones.
- La oficina responsable de planeamiento aprueba los códigos, descripciones y vigencias de Objetivo
  PEI y Actividad POI; `GlobalAdmin` registra cada versión. La carga inicial utiliza una semilla
  formalmente aprobada, PEI y POI se versionan de forma independiente y no existe sincronización
  externa en la Fase 1.
- Los documentos formales sin iniciativa o proyecto pertenecen a un expediente institucional. Cada
  serie documental conserva exactamente un propietario inmutable, portafolio o expediente, y los
  expedientes institucionales no se exponen en la consulta pública.
- El Evaluador registra y versiona la opinión técnica; la Autoridad o el Evaluador con decisión
  formal registra los documentos inmutables de decisión de iniciativa y aprobación de producto.
- La documentación de gestión del proyecto es opcional y no sustituye ciclos ni evidencias
  obligatorias.
- `Tipo de producto final aprobado` se exige solo en `PRODUCTO_APROBADO` y usa el catálogo canónico.
- El Responsable registra resultados clave y el Evaluador los valida antes del cierre; el Evaluador
  registra el informe final inmutable.
- `Fecha de cierre` se genera automáticamente al pasar a `FINALIZADO` o `CANCELADO`.
- Toda corrección de un documento formal crea una nueva versión y conserva el archivo anterior.
- Durante `PROYECTO_EJECUCION`, el Responsable solo puede editar los campos 17, 19 y 23, con
  auditoría.
- Todo proyecto completa los campos 1 al 13 y 22 al crearse, admite `Nota` opcional, usa la fecha del
  documento formal como inicio y comienza en `PROYECTO_EJECUCION` con código propio.
- Un proyecto derivado usa como código de origen el código de la iniciativa, copia su tipo de
  solución, propone su nombre y Responsable, y copia sus unidades; antes de confirmar se define un
  nombre propio, se seleccionan PEI y POI propios y pueden ajustarse Responsable y unidades.
- Un proyecto directo usa como código de origen el identificador del acto formal o fuente heredada y
  solo puede registrarlo la Autoridad o el Evaluador con el documento formal.
- La corrección de un ciclo quincenal cerrado crea una nueva versión trazable.
- El reporte semestral oficial cubrirá enero-junio con corte al 30/06 y julio-diciembre con corte al
  31/12. Los extraordinarios requieren solicitud documentada y aprobación de la Oficina de
  Modernización; ambos son generados con perfil `Evaluador` en PDF oficial y XLSX de detalle.
- El reporte semestral presenta totales por tipo, estado, unidad, fuente, tipo de solución, producto y
  cierre, e indicadores de admisibilidad, aprobación, cierre y cumplimiento de ciclos según BR-122.
- Los reportes admiten los filtros aprobados, son `INTERNO` por defecto y pasan a `RESTRINGIDO` si
  contienen información así clasificada.
- Antes de remitir un reporte, la Oficina de Modernización aprueba la versión y los destinatarios; la
  evidencia conserva parámetros, corte, generador, versión, hash, aprobación, destinatario, fecha y
  resultado.
- Al menos un usuario de cada perfil funcional involucrado valida el prototipo; se incluye un actor
  sectorial cuando el recorrido aplica a ese ámbito.
- Un `Evaluador` registra las validaciones y otro que no sea autor ni único validador registra la
  aprobación de la Oficina de Modernización.
- La validación cubre recorrido completo, reglas, mensajes, accesibilidad y privacidad y conserva
  usuario, perfil, escenario, resultado, observaciones y aceptación.
- Las iteraciones no tienen límite mientras existan hallazgos críticos o altos. Cada versión conserva
  código, fecha, recorrido, cambios, estado y versión anterior.
- Los estados son `BORRADOR`, `EN_VALIDACION`, `OBSERVADO`, `VALIDADO`, `APROBADO` y `RECHAZADO`; un
  cambio funcional o de accesibilidad crea otra versión y exige revalidación.
- Cada uno de los ocho recorridos se valida en escritorio y móvil; teclado y lector de pantalla se
  comprueban según los componentes e interacciones presentes.
- La medición inicial cubre los ocho recorridos por etapa de implementación, durante la validación y
  antes de aprobar cada prototipo.
- El equipo de experiencia de usuario ejecuta pruebas moderadas con escenarios versionados y datos
  sintéticos, calcula éxito, tiempo mediano, errores críticos, satisfacción y accesibilidad; un
  Evaluador coordina y otro aprueba.
- La muestra objetivo es cinco usuarios por perfil; una cantidad menor requiere justificación y no
  puede omitir perfiles involucrados.
- La evidencia conserva versión, muestra, escenarios, resultados, cálculos, hallazgos, metas
  aplicables, aprobación y hash.
- Cada recorrido se mide nuevamente antes de liberarse y después de cambios funcionales o de
  accesibilidad que lo afecten.
- Los reportes y todo su expediente se conservan según la tabla de retención documental vigente del
  MIDAGRI. Sin plazo confirmado no se eliminan automáticamente; una disposición posterior requiere
  autorización, auditoría y tratamiento completo, sin eliminar evidencias selectivamente.
- Después de la medición inicial, la Oficina de Modernización aprueba y versiona por recorrido la
  matriz de metas antes de implementar la interfaz Angular correspondiente: éxito mínimo de 90 %, cero errores críticos, satisfacción
  mínima de 4/5 y ausencia de hallazgos críticos o altos de accesibilidad.
- Con línea base comparable, el tiempo mediano debe mejorar al menos 20 %; sin ella, su meta se fija
  desde la medición inicial. Los errores críticos y hallazgos críticos o altos de accesibilidad son
  bloqueantes; las excepciones a otras métricas requieren justificación y aprobación documentadas.
- Ante concurrencia incompatible, prevalece la primera transición confirmada y las demás se
  rechazan sin alterar el historial.

## Constitution Conformance *(mandatory)*

- La especificación conserva los actores, estados, transiciones, documentos, códigos y límites de
  Fase 1 de la Constitución PIIP 4.0.0; toda decisión faltante se marca como aclaración.
- La propiedad funcional queda separada: el Responsable mantiene información, el Evaluador evalúa
  y cierra, la Autoridad decide y el Administrador funcional gestiona asignaciones.
- La autorización exige perfil y ámbito efectivos, y las operaciones sensibles generan auditoría
  inmutable.
- Los participantes sectoriales mantienen registros con `Responsable` y unidades de su entidad
  asignadas explícitamente.
- La iniciativa y el proyecto son registros separados, con códigos propios y vínculo inmutable;
  los proyectos directos requieren origen heredado o excepcional y autorización formal de la
  Autoridad.
- Los 23 campos oficiales cuentan con matriz funcional de obligatoriedad, edición, actores,
  formatos, cardinalidad y privacidad para iniciativas y proyectos.
- La consulta pública limita los campos a `Tipo de registro`, `Código`, `Nombre de iniciativa o
  proyecto` y `Estado`, publica tipo documental, título sin datos personales, versión, formato y
  fecha de publicación, y no muestra ni descarga contenido documental.
- La consulta pública es anónima; la incorporación inicial la registra el Responsable con asistencia
  de `UnidadAdmin` y validación del Evaluador; y los reportes requeridos son configurables dentro
  del ámbito y clasificación autorizados.
- Los prototipos PIIP aprobados son una puerta previa para las interfaces de los recorridos
  críticos.
- Las obligaciones de pruebas deben cubrir estados, RBAC por unidad, documentos de 100 MB,
  privacidad, auditoría, trazabilidad, reportes, accesibilidad y recorridos críticos.
- Las decisiones sobre límites técnicos, transacciones y autoridad de implementación corresponden
  a la planificación posterior y no alteran las reglas funcionales aquí definidas.
