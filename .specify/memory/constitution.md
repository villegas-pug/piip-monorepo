<!--
Informe de impacto de sincronización
- Cambio de versión: 3.0.0 -> 3.1.0.
- Principios modificados: marcadores de plantilla -> I. Entrega guiada por especificaciones;
  II. Simplicidad modular; III. Seguridad, privacidad y auditabilidad desde el diseño;
  IV. Fuente autoritativa única para reglas de negocio.
- Secciones agregadas: Restricciones técnicas y arquitectónicas; Flujo de entrega, calidad y
  especificaciones; catálogos canónicos e invariantes funcionales; versiones de referencia del
  template backend.
- Secciones eliminadas: ninguna.
- Plantillas que requieren actualización: actualizada .specify/templates/plan-template.md;
  actualizada .specify/templates/spec-template.md;
  actualizada .specify/templates/tasks-template.md.
- Comandos que requieren actualización: actualizado .opencode/commands/speckit.specify.md;
  actualizado .opencode/commands/speckit.plan.md;
  actualizado .opencode/commands/speckit.tasks.md;
  actualizado .opencode/commands/speckit.implement.md.
- Elementos diferidos: ninguno. La fecha de ratificación se infiere del registro de cambios 2.0.0.
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

### IV. Fuente autoritativa única para reglas de negocio

Los cambios de estado, documentos obligatorios, correlativos, permisos y alcance organizacional
DEBEN residir en un servicio de aplicación o procedimiento almacenado Oracle, según determine la
especificación aprobada de la funcionalidad. Los controladores y componentes visuales NO DEBEN
poseer reglas de negocio. Cada regla tiene exactamente una implementación autoritativa y NO DEBE
duplicarse entre Java y PL/SQL. Los servicios poseen la orquestación del caso de uso y los límites
transaccionales; los procedimientos almacenados que implementan comportamiento funcional son
código de aplicación y requieren versionado, auditoría y pruebas automatizadas.

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

Keycloak es la fuente autoritativa de identidad y credenciales. Oracle PIIP es la fuente
autoritativa de roles, permisos y alcance organizacional. La implementación del servicio
`seguridad` DEBE usar Keycloak Admin API para aprovisionar primero la identidad y después crear
el usuario Oracle y las asignaciones usuario-rol-unidad con el identificador retornado. Este flujo
DEBE ser idempotente y compensar una creación en Keycloak o dejar un fallo parcial recuperable y
auditado. No se crea un perfil local en el primer inicio de sesión. La activación inicial DEBE
usar acciones de correo de Keycloak; la desactivación DEBE bloquear el acceso en Keycloak y PIIP,
conservando los registros y asignaciones locales para auditoría.

### Persistencia y documentos

El agregado central es `PROYECTO`; representa tanto iniciativas como proyectos. El modelo DEBE
cubrir unidades, usuarios, roles, asignaciones rol-unidad, proyectos, relaciones
proyecto-unidad-orgánica, transiciones de estado, tipos documentales y documentos, secuencias de
código, auditorías de acceso y eventos, y `MV_PORTAFOLIO_RESUMEN` solo cuando se demuestre una
necesidad de rendimiento.

Los scripts de base de datos usan `database/ddl`, `database/procedures`, `database/functions`,
`database/packages`, `database/indexes`, `database/views` y `database/seeds`, con la secuencia
aprobada en `database/CHANGELOG.md`. Cada objeto almacenado DEBE documentar propósito,
parámetros, salidas, errores, dependencias, comportamiento transaccional, orden de ejecución y
reversión o compensación. Las restricciones y claves únicas expresan invariantes de negocio. El
historial de transiciones y los documentos formalizados son inmutables; las correcciones crean
nuevos eventos o versiones.

Los documentos están limitados a 25 MB y aceptan inicialmente PDF, Office Open XML, JPEG y PNG.
DEBEN conservar metadatos, versión, autor, fecha, clasificación, hash SHA-256 y estado de análisis
antimalware `PENDIENTE`, `LIMPIO` o `INFECTADO`. Los archivos pendientes o infectados NO DEBEN
publicarse ni utilizarse como evidencia formal. El módulo `documentos` DEBE exponer
`DocumentStorage` en `service/` e implementarlo en `service/impl/`, sin filtrar un proveedor de
almacenamiento hacia los DTO, entidades o controladores.

### Catálogos e invariantes canónicos del portafolio

Los siguientes valores son referencias obligatorias de diseño provenientes de MCVS-204:

| Catálogo | Valores canónicos |
|---|---|
| Tipo de registro | `INICIATIVA`, `PROYECTO` |
| Tipo de solución | `POTENCIAL_ADAPTABLE`, `POR_DEFINIR` |
| Fuente | `FICHA_INICIATIVA`, `CONCURSO_INTERNO`, `INNOVACION_ABIERTA`, `PROPUESTA_JEFATURA`, `OTROS` |
| Administración | `OM`, `OGTI`, `OM-OGTI` |
| Tipo de producto final | `PROTOTIPO_CONCEPTUALIZADO`, `SOLUCION_FUNCIONAL` |
| Estados | `PRESENTADO`, `INICIATIVA_APROBADA`, `INICIATIVA_ARCHIVADA`, `PROYECTO_EJECUCION`, `PRODUCTO_APROBADO`, `PRODUCTO_NO_APROBADO`, `SUSPENDIDO`, `CANCELADO` |

`FINALIZADO`, `NO_APLICABLE` y `NO_ADMISIBLE` no son estados canónicos. Los datos legados que
los usen requieren una regla de migración aprobada. Las transiciones controladas iniciales son:

| Origen | Destino | Rol autorizado | Documento | Observación |
|---|---|---|---|---|
| `PRESENTADO` | `INICIATIVA_APROBADA` | `Evaluador` | Obligatorio | Opcional |
| `PRESENTADO` | `INICIATIVA_ARCHIVADA` | `Evaluador` | Opcional | Obligatoria |
| `INICIATIVA_APROBADA` | `PROYECTO_EJECUCION` | `UnidadAdmin` | Obligatorio | Opcional |
| `PROYECTO_EJECUCION` | `PRODUCTO_APROBADO` | `Autoridad` | Obligatorio | Opcional |
| `PROYECTO_EJECUCION` | `PRODUCTO_NO_APROBADO` | `Autoridad` | Opcional | Obligatoria |
| `PROYECTO_EJECUCION` | `SUSPENDIDO` | `UnidadAdmin` | Opcional | Obligatoria |
| `PROYECTO_EJECUCION` | `CANCELADO` | `Autoridad` | Obligatorio | Obligatoria |
| `INICIATIVA_ARCHIVADA` | `PRESENTADO` | `Responsable` | Opcional | Opcional |

Una iniciativa que evoluciona a proyecto conserva su registro `PROYECTO`. Un código usa el
formato inmutable `AAAA-PREFIJO_UNIDAD-NNNNN`; su correlativo es único por año y unidad y nunca
se reutiliza. Una nueva iniciativa inicia en `PRESENTADO`. La creación directa de un proyecto
puede iniciar en `PROYECTO_EJECUCION` solo bajo una especificación aprobada con evidencia formal.
Cada transición DEBE ser transaccional y registrar estado anterior y nuevo, actor, rol efectivo,
unidad, fecha, observación y documento asociado cuando corresponda. La máquina de estados DEBE
rechazar transiciones no listadas, roles no autorizados y evidencia incompleta.

Los tipos documentales iniciales y sus condiciones son datos controlados:

| Tipo documental | Estado relacionado | Condición |
|---|---|---|
| Ficha de Iniciativa de Innovación Pública | `PRESENTADO` | Obligatorio |
| Informe de Opinión Técnica de Evaluación | `INICIATIVA_APROBADA` | Obligatorio |
| Documento Formal de Aprobación de Inicio | `PROYECTO_EJECUCION` | Obligatorio |
| Nota Conceptual del Proyecto | `PROYECTO_EJECUCION` | Opcional |
| Matriz de Planificación de Ciclos | `PROYECTO_EJECUCION` | Opcional |
| Seguimiento Ágil, Tablero Kanban | `PROYECTO_EJECUCION` | Opcional |
| Autoevaluación de Ciclo de Trabajo | `PROYECTO_EJECUCION` | Opcional |
| Documento Formal de Aprobación de Producto Final | `PRODUCTO_APROBADO` | Obligatorio |
| Informe Final de Cierre | `PRODUCTO_APROBADO` | Obligatorio |
| Informe de la Unidad de Modernización, Cancelación | `CANCELADO` | Obligatorio |

Los documentos formalizados y el historial de transiciones son inmutables. Una corrección o
sustitución DEBE producir una nueva versión o evento trazable. El borrado lógico PUEDE usarse
solo cuando un requisito funcional o de auditoría explícito lo justifique; no es el valor
predeterminado para las entidades de PIIP.

### Alcance de integraciones

La Fase 1 proporciona únicamente el registro centralizado y la operación interna de PIIP. La
sincronización con PIDE, servicios de otras entidades y motores de integración externa están
fuera de alcance. Los contratos de API y puertos futuros pueden ser claros, pero NO DEBEN
construirse conectores, adaptadores simulados ni procesos de sincronización sin una
especificación aprobada para la Fase 2.

## Flujo de entrega, calidad y especificaciones

Toda especificación DEBE incluir una verificación de conformidad constitucional antes de la
planificación y antes de declarar completa la implementación. DEBE indicar propósito de negocio,
actores, reglas, estados afectados, excepciones, requisitos documentales, autorización y alcance
organizacional, eventos de auditoría, clasificación de privacidad, integraciones y exclusiones
explícitas. Las decisiones materiales no resueltas permanecen como `NEEDS CLARIFICATION` y no
como supuestos.

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
| 3.1.0 | 2026-07-18 | Fija las versiones de referencia del template backend PIIP y sus dependencias de seguridad y OpenAPI. |
| 3.0.0 | 2026-07-18 | Sustituye la plantilla sin completar de Spec Kit por la constitución de monolito modular PIIP, catálogos canónicos, modelo de seguridad, puertas de calidad y gobierno. |
| 2.0.0 | 2026-07-18 | Registro histórico del proyecto: sustituyó una plantilla ajena por la constitución PIIP. |

**Versión**: 3.1.0 | **Ratificada**: 2026-07-18 | **Última enmienda**: 2026-07-18
