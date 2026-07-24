<!--
Informe de impacto de sincronización
- Cambio de versión: 3.2.0 -> 4.0.0.
- Fuente del cambio: aclaración aprobada de responsabilidades entre PIIP y OGTI para seguridad de
  binarios y aprovisionamiento fundacional del primer `GlobalAdmin`, autorizada el 2026-07-21.
- Principios modificados: III. Seguridad, privacidad y auditabilidad desde el diseño; IV. Fuente
  autoritativa única para reglas de negocio.
- Secciones modificadas: Identidad y autorización; Persistencia y documentos; Alcance de
  integraciones; Flujo de entrega, calidad y especificaciones; Gobierno.
- Secciones agregadas: inicialización funcional mediante semilla controlada.
- Secciones eliminadas: estados y gate antimalware funcionales de PIIP.
- Plantillas que requieren actualización: ninguna; las plantillas no fijan estados antimalware ni un
  mecanismo de bootstrap.
- Comandos que requieren actualización: ninguno.
- Artefactos con actualización posterior requerida: especificación, plan, tareas, investigación,
  modelo de datos, contratos y quickstart de gestión del portafolio. Los scripts Oracle solo se
  depositarán tras diseño y revisión; esta enmienda NO ejecuta SQL ni modifica el baseline vigente.
- Impacto en código, datos y pruebas: PIIP almacenará binarios en Oracle con hash, versión y
  clasificación sin modelar resultados antimalware; OGTI administrará los controles técnicos. La
  primera asignación `GlobalAdmin` será una semilla SQL manual, fail-fast y auditada.
- Elementos diferidos: proveedor y controles antimalware de OGTI permanecen fuera del alcance
  funcional de PIIP; no se infieren ni se integran en la aplicación.
-->

# Constitución de PIIP MIDAGRI

## Principios rectores

### I. Entrega guiada por especificaciones

Toda funcionalidad DEBE comenzar con una especificación aprobada que indique su propósito,
actores, reglas de negocio, criterios de aceptación, estados afectados, casos excepcionales e
impacto en privacidad o seguridad. Los artefactos de GitHub Spec Kit DEBEN preservar la
trazabilidad desde la intención hasta el diseño, las tareas, la implementación y las pruebas.
Una ambigüedad que cambie materialmente el comportamiento DEBE registrarse como
`NEEDS CLARIFICATION`; los agentes NO DEBEN inferir un requisito, actor, estado, permiso,
transición, integración ni valor de catálogo. Esto evita que decisiones funcionales no
aprobadas ingresen a PIIP.

### II. Simplicidad modular

La Fase 1 DEBE ser un monolito modular. Los módulos tienen responsabilidades explícitas, alta
cohesión, bajo acoplamiento y ninguna dependencia circular. Se aplican SOLID, DRY, KISS y
YAGNI; una nueva abstracción, patrón o servicio desplegable por separado requiere una necesidad
demostrada de escalabilidad, autonomía o integración. Un módulo DEBE comunicarse con otro solo
mediante una interfaz de servicio explícita, DTO o evento interno, nunca mediante las tablas o
repositorios del otro módulo.

### III. Seguridad, privacidad y auditabilidad desde el diseño

La autenticación, autorización, protección de datos personales y auditoría son requisitos
transversales obligatorios. El backend DEBE validar la identidad y autorizar cada operación
sensible; el estado del frontend o los datos proporcionados por el cliente nunca pueden ser la
autoridad efectiva. Los accesos sensibles, accesos denegados, cambios de negocio, transiciones,
operaciones documentales, asignaciones de roles y cambios de catálogos controlados DEBEN generar
evidencia de auditoría inmutable que identifique al actor, momento, alcance y datos modificados.
La consulta pública DEBE exponer únicamente información clasificada expresamente como pública.
La seguridad técnica antimalware de los binarios almacenados por PIIP es responsabilidad exclusiva
de OGTI y NO forma parte de las reglas funcionales, estados, contratos ni auditorías de PIIP. Esta
separación no reduce las obligaciones de PIIP sobre autenticación, autorización, privacidad,
integridad SHA-256, clasificación, versionado, inmutabilidad y auditoría de operaciones documentales.

### IV. Fuente autoritativa única para reglas de negocio

Los cambios de estado, documentos obligatorios, correlativos, permisos y alcance organizacional
DEBEN residir en un servicio de aplicación o procedimiento almacenado Oracle, según determine la
especificación aprobada de la funcionalidad. Los controladores y componentes visuales NO DEBEN
poseer reglas de negocio. Cada regla tiene exactamente una implementación autoritativa y NO DEBE
duplicarse entre Java y PL/SQL. Los servicios poseen la orquestación del caso de uso y los límites
transaccionales; los procedimientos almacenados que implementan comportamiento funcional son
código de aplicación y requieren versionado, auditoría y pruebas automatizadas.
La única excepción de inicialización es la semilla SQL manual del primer `GlobalAdmin`, definida en
esta Constitución; no constituye una segunda implementación del flujo administrativo ordinario y
DEBE quedar inutilizable después de su ejecución exitosa.

## Restricciones técnicas y arquitectónicas

### Tecnología obligatoria

- Backend: Java 21, Spring Boot 3.2.5, Maven, Spring Data JPA, procedimientos almacenados Oracle,
  Spring Security OAuth 2.0/OIDC Resource Server y OpenAPI 3.0 bajo `/api/v1`. El template usa
  Spring Cloud 2023.0.0, Oracle JDBC 21.9.0.0, Lombok 1.18.30, MapStruct 1.5.5.Final y Springdoc
  OpenAPI 2.5.0; Spring Security y OAuth2 Resource Server se gestionan mediante el BOM de Spring
  Boot 3.2.5.
- Paquete raíz del backend: `pe.gob.midagri.piip`.
- Frontend: Angular 22, Angular Material, TypeScript estricto, diseño responsive y cumplimiento
  de WCAG 2.1 AA.
- Persistencia: Oracle Database 19c o superior.
- Identidad: Keycloak 26 o versión compatible aprobada, OpenID Connect Authorization Code Flow
  con PKCE y tema de inicio de sesión Keycloak personalizado.
- Los cambios de base de datos DEBEN ser scripts SQL manuales, revisables y versionados bajo
  `database/`. NO DEBEN usarse Flyway ni Liquibase. Los scripts NO DEBEN ejecutarse contra bases
  de datos compartidas ni modificar datos destructivamente sin autorización humana explícita.

### Límites de módulos backend

Los módulos de la Fase 1 son `organizacion`, `seguridad`, `portafolio`, `documentos`,
`reportes`, `consulta` y `auditoria`. Cada módulo backend DEBE usar esta estructura interna:

```text
<modulo>/
├── controller/
├── service/
│   └── impl/
├── repository/
├── dto/
├── entity/
├── exception/
├── mapper/
└── event/
```

Los controladores validan DTO, delegan en servicios y construyen respuestas HTTP. Los contratos
de servicio usan DTO o tipos simples y nunca exponen entidades JPA. Los repositorios encapsulan
JPA y el acceso a procedimientos almacenados. Los mapeadores contienen solo conversiones. Cada
módulo proporciona un manejador de excepciones `@RestControllerAdvice` acotado a su módulo. NO
DEBEN agregarse directorios genéricos `model/`, `client/` ni `integration/`. Las integraciones
autorizadas pertenecen a `service/impl/`, detrás de contratos de servicio cohesivos.

Angular DEBE organizar el código en `core` para asuntos singleton y autenticación, `shared` para
utilidades de presentación reutilizables sin reglas de negocio y `features` cargadas de forma
diferida por módulo funcional cuando corresponda. Los componentes pueden ocultar acciones no
disponibles, pero NO DEBEN decidir permisos ni transiciones de estado.

### Identidad y autorización

Angular DEBE redirigir a Keycloak y NO DEBE recopilar, procesar ni almacenar contraseñas. El
backend DEBE validar emisor, audiencia, firma, vigencia y scopes o claims del token. Los roles
canónicos son `GlobalAdmin`, `UnidadAdmin`, `Responsable`, `Evaluador`, `Autoridad` y `Consulta`.
La autorización efectiva combina el permiso funcional con el alcance organizacional mediante la
relación usuario-rol-unidad.

Las asignaciones funcionales DEBEN basarse en una matriz aprobada que relacione cargo o función,
perfil canónico y unidad organizacional. Una persona PUEDE tener múltiples perfiles y alcances
simultáneos según las funciones que desempeñe. Cada operación DEBE evaluar el perfil y la unidad
efectivos; ninguna asignación concede acceso fuera de su ámbito. La especificación de seguridad
DEBE definir el alcance sobre unidades descendientes, la combinación de perfiles y las reglas de
vigencia, suplencia y revocación antes de implementar la administración funcional.

Keycloak es la fuente autoritativa de identidad y credenciales. Oracle PIIP es la fuente
autoritativa de roles, permisos y alcance organizacional. La implementación del servicio
`seguridad` DEBE usar Keycloak Admin API para aprovisionar primero la identidad y después crear
el usuario Oracle y las asignaciones usuario-rol-unidad con el identificador retornado. Este flujo
DEBE ser idempotente y compensar una creación en Keycloak o dejar un fallo parcial recuperable y
auditado. No se crea un perfil local en el primer inicio de sesión. La activación inicial DEBE
usar acciones de correo de Keycloak; la desactivación DEBE bloquear el acceso en Keycloak y PIIP,
conservando los registros y asignaciones locales para auditoría.

El flujo anterior rige la administración ordinaria. La semilla fundacional descrita a continuación
usa exclusivamente un `sub` existente proporcionado por el administrador Keycloak de OGTI y NO crea,
activa ni modifica identidades en Keycloak.

La primera asignación `GlobalAdmin` DEBE crearse exclusivamente mediante una semilla SQL manual,
revisable, de ejecución única y fail-fast, ejecutada por un DBA autorizado de OGTI. El administrador
de Keycloak de OGTI proporciona el `sub` beneficiario, que se considera la identidad activa aprobada
para esta inicialización. La semilla DEBE abortar sin cambios si existe cualquier asignación histórica
`GlobalAdmin` o si ya fue ejecutada. La Jefatura de la Oficina de Modernización autoriza la asignación
mediante una aprobación de despliegue identificable. Los valores iniciales son
`codigoUnidad=MIDAGRI`, `nombreUnidad=Ministerio de Desarrollo Agrario y Riego`,
`codigoFuncion=ADMINISTRADOR_PIIP` y `nombreFuncion=Administrador PIIP`; la unidad raíz existente
DEBE prevalidarse y reutilizarse, nunca duplicarse. La auditoría DEBE conservar
como mínimo `sub`, perfil, función, unidad, Jefatura autorizante, aprobación de despliegue, DBA
ejecutor, fecha, operación y resultado. Después se usa exclusivamente el flujo administrativo
ordinario de PIIP; NO DEBE existir un comando, endpoint o segundo bootstrap alternativo.

### Persistencia y documentos

El agregado central es `PROYECTO`; representa tanto iniciativas como proyectos mediante registros
independientes. Cuando una iniciativa aprobada origina un proyecto, ambos registros DEBEN conservar
su identidad, tipo, código e historial propios, y el proyecto DEBE mantener una relación inmutable
con la iniciativa de origen. El modelo DEBE cubrir unidades, usuarios, roles, asignaciones
rol-unidad, proyectos, relaciones entre iniciativa y proyecto, relaciones proyecto-unidad-orgánica,
transiciones de estado, tipos documentales y documentos, secuencias de código, auditorías de acceso
y eventos, y `MV_PORTAFOLIO_RESUMEN` solo cuando se demuestre una necesidad de rendimiento.

Los scripts de base de datos usan `database/ddl`, `database/procedures`, `database/functions`,
`database/packages`, `database/indexes`, `database/views` y `database/seeds`, con la secuencia
aprobada en `database/CHANGELOG.md`. Cada objeto almacenado DEBE documentar propósito,
parámetros, salidas, errores, dependencias, comportamiento transaccional, orden de ejecución y
reversión o compensación. Las restricciones y claves únicas expresan invariantes de negocio. El
historial de transiciones y los documentos formalizados son inmutables; las correcciones crean
nuevos eventos o versiones.

Cada documento o evidencia está limitado a 100 MB y acepta inicialmente PDF, Office Open XML,
JPEG y PNG. Los documentos y evidencias DEBEN conservar metadatos, versión, autor, fecha,
clasificación y hash SHA-256. Los binarios DEBEN almacenarse en Oracle PIIP; OGTI administra fuera de
la aplicación su análisis, detección, bloqueo, cuarentena y respuesta ante malware. PIIP NO DEBE
modelar estados, resultados, informes, integraciones ni gates antimalware, ni condicionar el uso
formal de un documento a datos antimalware. El módulo `documentos` DEBE exponer `DocumentStorage` en
`service/` e implementarlo en `service/impl/`, sin filtrar la persistencia Oracle hacia los DTO,
entidades o controladores.

La clasificación DEBE aplicarse a campos y documentos mediante una matriz aprobada. La ausencia de
clasificación nunca equivale a autorización pública. Durante la Fase 1, la consulta pública DEBE
permitir que ciudadanos y representantes de otras entidades busquen y consulten solo campos
expresamente públicos y metadatos documentales descriptivos autorizados; NO DEBE mostrar el
contenido de los documentos ni permitir su descarga.

### Catálogos e invariantes canónicos del portafolio

Los siguientes valores son referencias obligatorias de diseño del portafolio institucional:

| Catálogo | Valores canónicos |
|---|---|
| Tipo de registro | `INICIATIVA`, `PROYECTO` |
| Tipo de solución | `POTENCIAL_ADAPTABLE`, `POR_DEFINIR` |
| Fuente | `FICHA_INICIATIVA`, `CONCURSO_INTERNO`, `INNOVACION_ABIERTA`, `PROPUESTA_JEFATURA`, `OTROS` |
| Administración | `OM`, `OGTI`, `OM-OGTI` |
| Tipo de producto final | `PROTOTIPO_CONCEPTUALIZADO`, `SOLUCION_FUNCIONAL` |
| Estados | `PRESENTADO`, `NO_ADMISIBLE`, `NO_APLICABLE`, `INICIATIVA_APROBADA`, `INICIATIVA_ARCHIVADA`, `PROYECTO_EJECUCION`, `SUSPENDIDO`, `CANCELADO`, `PRODUCTO_APROBADO`, `PRODUCTO_NO_APROBADO`, `FINALIZADO` |

Las transiciones controladas iniciales distinguen la decisión de negocio de su registro operativo:

| Origen | Destino | Rol que decide | Rol que registra | Documento o evidencia | Observación |
|---|---|---|---|---|---|
| `PRESENTADO` | `NO_ADMISIBLE` | `Evaluador` | `Evaluador` | Según especificación aprobada | Obligatoria |
| `PRESENTADO` | `NO_APLICABLE` | `Evaluador` | `Evaluador` | Según especificación aprobada | Obligatoria |
| `PRESENTADO` | `INICIATIVA_APROBADA` | `Autoridad` | `Autoridad` o `Evaluador` con decisión formal | Obligatorio | Opcional |
| `PRESENTADO` | `INICIATIVA_ARCHIVADA` | `Autoridad` | `Autoridad` o `Evaluador` con decisión formal | Obligatorio | Obligatoria |
| `PROYECTO_EJECUCION` | `SUSPENDIDO` | `UnidadAdmin` | `UnidadAdmin` | Obligatorio | Obligatoria |
| `PROYECTO_EJECUCION` | `CANCELADO` | `Autoridad` | `Autoridad` o `Evaluador` con decisión formal | Obligatorio | Obligatoria |
| `PROYECTO_EJECUCION` | `PRODUCTO_APROBADO` | `Autoridad` | `Autoridad` o `Evaluador` con decisión formal | Obligatorio | Opcional |
| `PROYECTO_EJECUCION` | `PRODUCTO_NO_APROBADO` | `Autoridad` | `Autoridad` o `Evaluador` con decisión formal | Obligatorio | Obligatoria |
| `PRODUCTO_APROBADO` | `FINALIZADO` | `Evaluador` | `Evaluador` | Obligatorio | Obligatoria |
| `PRODUCTO_NO_APROBADO` | `FINALIZADO` | `Evaluador` | `Evaluador` | Obligatorio | Obligatoria |

`NO_ADMISIBLE`, `NO_APLICABLE` e `INICIATIVA_ARCHIVADA` son estados terminales. La creación del
proyecto no modifica el estado de la iniciativa aprobada: crea un nuevo registro de tipo
`PROYECTO`, vinculado con la iniciativa, que inicia en `PROYECTO_EJECUCION`. La reanudación o
cualquier otra salida desde `SUSPENDIDO`, así como una salida desde `CANCELADO`, requiere una
transición expresamente aprobada en una futura enmienda; mientras no exista, DEBE rechazarse.

Cada iniciativa y proyecto usa un código propio con el formato inmutable
`AAAA-PREFIJO_UNIDAD-NNNNN`; su correlativo es único por año y unidad y nunca se reutiliza. Una
nueva iniciativa inicia en `PRESENTADO`. Un `Responsable` autorizado y dentro de su alcance PUEDE
crear el proyecto derivado de una iniciativa aprobada cuando exista la decisión formal requerida.
La creación directa de un proyecto en `PROYECTO_EJECUCION` solo procede para un proyecto heredado
o una excepción formalmente autorizada y DEBE registrar como mínimo el documento de aprobación o
autorización, origen, unidad responsable, responsable, fecha de inicio, estado actual y evidencias
disponibles. La especificación funcional DEBE identificar quién autoriza la excepción y cómo se
acredita; este mecanismo NO DEBE utilizarse para omitir la evaluación de una iniciativa nueva.

Durante `PROYECTO_EJECUCION`, el `Responsable` del proyecto DEBE poder mantener la planificación,
ciclos de trabajo, avances, dificultades, productos parciales y evidencias dentro de su alcance.
También DEBE poder presentar el producto final con sus documentos de sustento. La `Autoridad`
decide su aprobación o no aprobación mediante decisión formal. Después de cualquiera de esos
resultados, el `Evaluador` de la Oficina de Modernización PUEDE completar el cierre administrativo
solo cuando estén registrados el informe final, los resultados, los aprendizajes y la conclusión;
el cierre cambia el proyecto a `FINALIZADO`.

Los cambios del modelo de registro único al vínculo iniciativa-proyecto y la incorporación de
nuevos estados DEBEN contar con scripts versionados y una regla de migración o compensación
aprobada antes de aplicarse a datos existentes. Ninguna migración PUEDE destruir el historial,
reutilizar códigos ni perder la relación de origen.

Cada transición DEBE ser transaccional y registrar estado anterior y nuevo, actor, rol efectivo,
unidad, fecha, observación y documento asociado cuando corresponda. La máquina de estados DEBE
rechazar transiciones no listadas, roles no autorizados y evidencia incompleta.

Los tipos documentales iniciales y sus condiciones son datos controlados:

| Tipo documental | Etapa o estado relacionado | Condición |
|---|---|---|
| Ficha de Iniciativa de Innovación Pública | `PRESENTADO` | Obligatorio |
| Informe de Opinión Técnica de Evaluación | Decisión sobre iniciativa | Obligatorio antes de la decisión de la autoridad |
| Documento Formal de Decisión sobre la Iniciativa | `INICIATIVA_APROBADA`, `INICIATIVA_ARCHIVADA` | Obligatorio |
| Documento Formal de Aprobación o Autorización de Inicio | `PROYECTO_EJECUCION` | Obligatorio para proyecto derivado o directo |
| Nota Conceptual del Proyecto | `PROYECTO_EJECUCION` | Opcional |
| Matriz de Planificación de Ciclos | `PROYECTO_EJECUCION` | Opcional |
| Seguimiento Ágil, Tablero Kanban | `PROYECTO_EJECUCION` | Opcional |
| Autoevaluación de Ciclo de Trabajo | `PROYECTO_EJECUCION` | Opcional |
| Documento Formal de Aprobación de Producto Final | `PRODUCTO_APROBADO` | Obligatorio |
| Evidencia de No Aprobación del Producto Final | `PRODUCTO_NO_APROBADO` | Obligatorio junto con la observación |
| Informe Final de Cierre | `FINALIZADO` | Obligatorio |
| Evidencia de Suspensión | `SUSPENDIDO` | Obligatorio |
| Informe de la Oficina de Modernización, Cancelación | `CANCELADO` | Obligatorio |

Los documentos formalizados y el historial de transiciones son inmutables. Una corrección o
sustitución DEBE producir una nueva versión o evento trazable. El borrado lógico PUEDE usarse
solo cuando un requisito funcional o de auditoría explícito lo justifique; no es el valor
predeterminado para las entidades de PIIP.

### Campos oficiales del portafolio

El registro institucional contempla los siguientes 23 campos oficiales:

| N.º | Campo |
|---:|---|
| 1 | Tipo de registro |
| 2 | Código |
| 3 | Código de origen |
| 4 | Fecha de inicio |
| 5 | Nombre de iniciativa o proyecto |
| 6 | Tipo de solución |
| 7 | Fuente u origen |
| 8 | Responsable |
| 9 | Descripción |
| 10 | Objetivo PEI |
| 11 | Actividad POI |
| 12 | Unidades de organización responsables |
| 13 | Estado |
| 14 | Informe de opinión técnica de evaluación de iniciativa |
| 15 | Documento formal de decisión de aprobación |
| 16 | Documento formal de aprobación del producto final |
| 17 | Documentación de la gestión del proyecto |
| 18 | Tipo de producto final aprobado |
| 19 | Resultados clave |
| 20 | Fecha de cierre |
| 21 | Informe final de cierre |
| 22 | Componente digital |
| 23 | Nota |

Una matriz funcional aprobada DEBE definir para cada campo su obligatoriedad, editabilidad,
clasificación de privacidad y actor responsable según el tipo de registro y la etapa del ciclo de
vida. La ausencia de esa matriz bloquea la planificación de los formularios, validaciones,
consultas y reportes; la nulabilidad técnica del esquema no sustituye esta decisión funcional.

Los usuarios institucionales autorizados DEBEN poder buscar, filtrar y consultar iniciativas y
proyectos, incluidos estados, responsables, resultados, documentos autorizados e historial,
únicamente dentro de su alcance organizacional. Toda consulta o exportación DEBE aplicar la matriz
de privacidad y generar la evidencia de auditoría exigida por la sensibilidad de la operación.

### Alcance de integraciones

La Fase 1 proporciona el registro centralizado y la operación interna y sectorial de PIIP. Los
participantes autorizados de Programas, Proyectos Especiales y Organismos Públicos Adscritos
PUEDEN registrar y mantener iniciativas y proyectos exclusivamente dentro de su ámbito. Esta
facultad no elimina la evaluación ordinaria de iniciativas ni habilita proyectos directos fuera
de las excepciones formalizadas.

La incorporación inicial de información existente PUEDE realizarse mediante una carga manual
controlada si una especificación aprobada define responsables, validaciones, tratamiento de
errores, evidencias y auditoría. La sincronización con PIDE, servicios de otras entidades y
motores de integración externa está fuera de alcance. Los contratos de API y puertos futuros
pueden ser claros, pero NO DEBEN construirse conectores, adaptadores simulados ni procesos de
sincronización sin una especificación aprobada para la Fase 2.
Los controles antimalware administrados por OGTI son controles de plataforma sobre Oracle y NO una
integración funcional de PIIP; la aplicación no consume ni expone contratos para ellos.

### Reportes institucionales y prototipos

La Oficina de Modernización DEBE poder generar el reporte institucional del portafolio con
periodicidad semestral y también cuando sea requerido. No existen reportes mensuales ni
trimestrales obligatorios durante la Fase 1. Una especificación aprobada DEBE definir el periodo y
fecha de corte, contenido, indicadores, filtros, responsables, destinatarios, formato,
clasificación, alcance organizacional, conservación y eventos de auditoría antes de implementar
el reporte.

Los flujos de registro, evaluación, decisión, seguimiento, aprobación del producto, cierre,
consulta institucional y consulta pública PUEDEN incorporar prototipos específicos de PIIP cuando
la especificación de la fase los incluya. US9, sus prototipos, mediciones, matrices de metas y
preparación de liberación quedan diferidos de la Fase 1 actual mediante la enmienda 5.0.0. Una fase
que los reactive DEBE definir aprobador, criterios de aceptación, evidencia y persistencia antes de
convertirlos en gate de interfaz.

## Flujo de entrega, calidad y especificaciones

Toda especificación DEBE incluir una verificación de conformidad constitucional antes de la
planificación y antes de declarar completa la implementación. DEBE indicar propósito de negocio,
actores, reglas, estados afectados, excepciones, requisitos documentales, autorización y alcance
organizacional, eventos de auditoría, clasificación de privacidad, integraciones y exclusiones
explícitas. Las decisiones materiales no resueltas permanecen como `NEEDS CLARIFICATION` y no
como supuestos.

Las especificaciones del portafolio DEBEN identificar además los campos oficiales afectados y su
matriz de obligatoriedad, la separación o relación entre iniciativa y proyecto, el rol que decide
y el rol que registra cada transición, los documentos habilitantes, el tratamiento de proyectos
directos y el alcance de consulta. La evidencia de aprobación de prototipos se exige solo cuando
esa subcapacidad forme parte del alcance aprobado de la fase.

Los planes DEBEN documentar propiedad de reglas autoritativas, límites de módulos, transacciones,
scripts o procedimientos Oracle, contratos API, efectos en Keycloak e impacto en privacidad. Un
plan DEBE identificar consumidores de contratos antes de cambiar esquemas, API o catálogos
canónicos. El trabajo SQL DEBE incluir su orden de ejecución y mecanismo de compensación o
reversión, sin ejecutar automáticamente cambios en bases de datos compartidas.

JUnit 5, Mockito cuando el aislamiento sea apropiado y Oracle Testcontainers son obligatorios
para la calidad backend. Vitest y Playwright son obligatorios para el frontend y recorridos
críticos. El código de negocio DEBE mantener al menos 80 por ciento de cobertura. Las pruebas
DEBEN cubrir máquinas de estado, RBAC por unidad, correlativos, transacciones, documentos,
auditoría, procedimientos almacenados, límites arquitectónicos, aprovisionamiento administrativo
con Keycloak y fallos parciales, y el modelo de autorización efectiva derivado de Oracle. Las
pruebas no son opcionales cuando un cambio afecta una regla constitucional, seguridad,
persistencia, contrato API, gestión documental o comportamiento de negocio.

Los planes de tareas DEBEN incluir verificaciones de arquitectura que impidan que los
controladores accedan a repositorios, que los servicios expongan entidades JPA y que se agreguen
directorios prohibidos. DEBEN incluir tareas apropiadas de seguridad, privacidad, auditoría,
accesibilidad, backend, frontend y pruebas SQL. Los agentes PUEDEN proponer cambios SQL en el
repositorio, pero NO DEBEN realizar operaciones destructivas de datos, ejecutar contra recursos
compartidos ni realizar acciones externas sin autorización humana explícita.

## Gobierno

Esta constitución tiene precedencia sobre las especificaciones funcionales y todas las fuentes de
menor nivel. Las fuentes aplicables siguen este orden: especificación funcional aprobada,
MCVS-204, MCVS-202, MCVS-103 y evidencia heredada de Excel. Una fuente de menor precedencia puede
proporcionar un valor no definido de otro modo solo cuando no contradiga esta constitución ni los
catálogos canónicos y la especificación deje trazada la decisión. Los conflictos DEBEN registrarse
como `NEEDS CLARIFICATION` y NO DEBEN resolverse por inferencia.

Una enmienda DEBE explicar su motivo y requisitos afectados, evaluar el impacto en
especificaciones, código, datos y pruebas existentes, actualizar el versionado semántico y añadir
una entrada fechada al historial de cambios. Los cambios mayores eliminan o redefinen de forma
incompatible un principio rector. Los cambios menores agregan un principio o amplían
materialmente la orientación obligatoria. Los cambios de parche aclaran la redacción sin cambiar
la semántica de gobierno. Cada revisión de especificación DEBE verificar conformidad antes de la
planificación y antes de completar la implementación.

| Versión | Fecha | Cambio |
|---|---|---|
| 5.0.0 | 2026-07-22 | Difiere US9, prototipos, mediciones, matrices de metas y sus gates de interfaz a una fase posterior; la Fase 1 actual mantiene accesibilidad, seguridad y pruebas sin ese gate. |
| 4.0.0 | 2026-07-21 | Traslada a OGTI la responsabilidad exclusiva sobre seguridad antimalware de binarios, elimina sus estados y gates funcionales de PIIP y establece la semilla SQL auditada del primer `GlobalAdmin`. |
| 3.2.0 | 2026-07-21 | Alinea el ciclo de vida, registros vinculados, roles de decisión y registro, documentos de hasta 100 MB, campos oficiales, cierre, alcance sectorial, reportes y prototipos con la gestión institucional del portafolio. |
| 3.1.0 | 2026-07-18 | Fija las versiones de referencia del template backend PIIP y sus dependencias de seguridad y OpenAPI. |
| 3.0.0 | 2026-07-18 | Sustituye la plantilla sin completar de Spec Kit por la constitución de monolito modular PIIP, catálogos canónicos, modelo de seguridad, puertas de calidad y gobierno. |
| 2.0.0 | 2026-07-18 | Registro histórico del proyecto: sustituyó una plantilla ajena por la constitución PIIP. |

**Versión**: 5.0.0 | **Ratificada**: 2026-07-18 | **Última enmienda**: 2026-07-22
