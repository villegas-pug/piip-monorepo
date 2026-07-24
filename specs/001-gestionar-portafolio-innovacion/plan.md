# Plan de implementación: Gestión del Portafolio Institucional de Innovación Pública

**Rama**: `001-gestionar-portafolio-innovacion` | **Fecha**: 2026-07-21 | **Especificación**: [spec.md](./spec.md)

**Entrada**: Especificación funcional aprobada de la Fase 1, alineada con la Constitución 5.0.0 y las
remediaciones C1/C2.

**Estado del plan**: Diseño funcional y arquitectura objetivo conformes. El diccionario físico Oracle
debe completarse y recibir revisión humana de base de datos antes de depositar los scripts 002-024.
El BUILD de cada recorrido permanece sujeto a su gate; OIDC, datos sintéticos, semillas y backfill
conservan los gates de insumos indicados en este plan.

## Resumen

La Fase 1 se implementará como un monolito modular Spring Boot y una aplicación Angular. El agregado
central persistente `PROYECTO` representará iniciativas y proyectos como registros independientes;
un vínculo uno a cero-o-uno conservará de forma inmutable el origen del único proyecto derivado. Los
servicios de aplicación Java/JPA serán la única fuente autoritativa de reglas, transacciones,
autorización y máquina de estados. Oracle aportará persistencia, restricciones, índices y bloqueo de
filas, sin procedimientos funcionales paralelos.

El diseño cubre organización, seguridad, portafolio, documentos, reportes, consulta y auditoría. US9,
sus prototipos, mediciones, matrices de metas y gates de interfaz se difieren a una fase posterior por
la enmienda constitucional 5.0.0. No se incluyen despliegue productivo, infraestructura institucional,
sincronizaciones externas, cargas masivas ni funcionalidades diferidas.

## Contexto técnico

**Backend**: Java 21, Spring Boot 3.2.5, Maven reactor, Spring Data JPA, Spring Security OAuth 2.0
Resource Server, MapStruct 1.5.5.Final y Springdoc OpenAPI 2.5.0.

**Frontend**: Angular 22, Angular Material, componentes standalone, TypeScript estricto, diseño
responsive y WCAG 2.1 AA.

**Persistencia**: Oracle 19c+ en `KALLPA_PIIP`; SQL manual incremental bajo `database/`. El baseline
vigente es `database/ddl/init/001_baseline_piip.sql`; la ruta literal
`database/ddl/001_baseline_kallpa_piip.sql` no existe, aunque el encabezado del baseline vigente usa
ese nombre histórico.

**Identidad**: Keycloak 26 compatible mediante `keycloak-js`, OIDC Authorization Code Flow con PKCE y
tema de inicio de sesión personalizado configurado por OGTI. La configuración runtime define `issuer`,
`clientId`, redirect URI, post-logout URI y scopes por ambiente; sus valores y los recursos del tema
permanecen fuera del repositorio. El backend usa `sub` como identidad estable y trata `email` y datos
de `profile` como informativos. Keycloak es autoridad de identidad y credenciales; Oracle PIIP lo es de
perfiles, permisos y ámbitos. El backend valida `issuer`, `audience`, firma, vigencia y los claims
estándar necesarios; no se exige un scope adicional porque los permisos funcionales y el ámbito se
determinan mediante asignación efectiva Oracle.

**API**: REST OpenAPI 3.0 bajo `/api/v1`, DTO específicos por caso de uso y clasificación,
`application/problem+json`, paginación base cero, idempotencia y control de concurrencia.

**Documentos**: `DocumentStorage` en el módulo `documentos`, implementado sobre BLOB Oracle sin filtrar
la persistencia a sus consumidores. SHA-256, versiones, máximo inclusivo de 100 MB y formatos PDF,
OOXML, JPEG y PNG. OGTI administra fuera de PIIP el análisis, bloqueo, cuarentena y respuesta ante
malware; la aplicación no modela estados, contratos, informes ni gates antimalware.

**Inicialización de seguridad**: el primer `GlobalAdmin` se crea exclusivamente dentro de la semilla
SQL 021. Un DBA autorizado de OGTI la ejecuta manualmente con el `sub` proporcionado por el
administrador Keycloak y la aprobación de despliegue de la Jefatura de Modernización. La semilla
prevalida y reutiliza la unidad `MIDAGRI`, crea la función `ADMINISTRADOR_PIIP`, su combinación y la primera asignación, registra
la auditoría mínima y aborta si existe cualquier antecedente. No existe comando, endpoint ni cliente
OIDC temporal de bootstrap.

**Pruebas**: JUnit 5, Mockito, Oracle Testcontainers, ArchUnit y JaCoCo en backend; Vitest y
Playwright en frontend. Cobertura mínima de 80 % para código de negocio.

**Tipo de proyecto**: Monolito modular con frontend web Angular.

**Objetivos de rendimiento**: Los objetivos funcionales son exactitud del 100 % para autorización,
transiciones, trazabilidad, privacidad y evidencias. Las metas de experiencia por recorrido se fijan
tras la medición inicial según BR-149; no se inventa un objetivo de latencia o concurrencia no
aprobado. `MV_PORTAFOLIO_RESUMEN` no se crea sin evidencia de necesidad.

**Restricciones**: Sin Flyway/Liquibase; sin ejecución automática de SQL; sin conectores o
sincronización funcional externa no aprobada, salvo Keycloak Admin API para el ciclo de identidad;
sin descarga pública documental; sin reglas de negocio en controladores, Angular o PL/SQL; sin
`model/`, `client/` o `integration/` genéricos.

**Escala y alcance**: Desarrollo local de las ocho historias activas de usuario de la Fase 1 para
MIDAGRI y entidades sectoriales dentro de unidades asignadas
explícitamente. La capacidad y dimensionamiento productivos quedan fuera de alcance.

## Constitution Check inicial

*Gate realizado antes del diseño contra la Constitución 5.0.0.*

| Control | Resultado | Evidencia o acción |
|---|---|---|
| Especificación aprobada y desconocidos materiales trazados | CUMPLE | `spec.md` está `Approved`, C1/C2 están resueltos y US9 está diferida por la Constitución 5.0.0. |
| Propósito, actores, reglas, estados, excepciones y aceptación | CUMPLE | `spec.md` define US1-US9, BR-001 a BR-152, FR-001 a FR-164 y criterios medibles. |
| Límites modulares, DTO y transacciones | CUMPLE EN DISEÑO | Se asignan propietarios e interacciones en este plan y [contracts/README.md](./contracts/README.md). |
| Fuente autoritativa única | CUMPLE EN DISEÑO | Servicios Java/JPA para operación ordinaria; semilla SQL 021 como única inicialización constitucional del primer `GlobalAdmin`. |
| Autorización efectiva | CUMPLE EN DISEÑO | Una asignación Oracle seleccionada por operación, revalidada antes del cambio sensible. |
| Privacidad, auditoría y documentos | CUMPLE EN DISEÑO | Publicación por Evaluador, propiedad documental excluyente y matriz consolidada en [data-model.md](./data-model.md). |
| Catálogos, estados, transiciones, documentos y códigos | CUMPLE EN DISEÑO | Se preservan valores canónicos y se corrigen únicamente mediante SQL incremental. |
| 23 campos oficiales | CUMPLE EN DISEÑO | Obligatoriedad, edición, privacidad y responsable se consolidan sin alterar BR aprobadas. |
| Iniciativa y proyecto separados | CUMPLE | Relación derivada inmutable y única; proyectos directos exigen autoridad y evidencia. |
| Decisor y registrador | CUMPLE | Se modelan ambos actores y el documento formal en cada transición aplicable. |
| Consulta pública minimizada | CUMPLE EN DISEÑO | Cuatro campos y metadatos de publicaciones elegibles; nunca contenido o descarga. |
| Prototipos aprobados | DIFERIDO | US9 y sus gates no forman parte de la Fase 1 actual. |
| SQL incremental, orden y compensación | CUMPLE EN DISEÑO | Scripts 002-024 propuestos; baseline intacto; compensación forward-only. |
| Estrategia de pruebas | CUMPLE EN DISEÑO | Matriz de trazabilidad incluida al final de este plan. |
| Exclusiones de Fase 1 | CUMPLE | No se diseñan integraciones externas, carga masiva ni transiciones futuras. |

### Dependencias de implementación

| Dependencia | Condición de avance |
|---|---|
| Primera versión PEI y primera versión POI | Datasets y aprobaciones formales independientes, cargados en expedientes institucionales. |
| Matriz inicial | Funciones y combinaciones función-perfil-unidad concreta formalmente aprobadas. |
| Datos legacy | Mapeos explícitos para PEI/POI, documentos, unidades, titulares y asignaciones; ninguna inferencia automática. |
| Esquema Oracle | Scripts revisados, registrados `PENDIENTE`, ejecutados manualmente y confirmados antes de actualizar el catálogo. |
| Interfaces | Snapshot OpenAPI compatible, autorización backend y pruebas de accesibilidad. |
| Configuración OIDC | Valores runtime aprobados por ambiente antes de verificar integración real. |
| Tema de inicio Keycloak | OGTI configura el tema personalizado en el ambiente OIDC y entrega confirmación de configuración antes de verificar el redireccionamiento institucional. |
| Dataset sintético | Diferido junto con US9. |
| Diccionario físico | Revisión humana DB antes de crear cualquier script 002-024. |
| Secretos y wallet | Retirada del repositorio y rotación por un administrador humano antes de ejecutar localmente. |
| Semilla inicial `GlobalAdmin` | Scripts 002, 007 y 008 confirmados; `sub`, aprobación de despliegue, DBA ejecutor y valores canónicos incluidos en 021 antes de su ejecución manual. La dependencia confirmada prevalece, como excepción documentada, sobre el orden numérico de 021. |

## Estructura del proyecto

### Documentación de la característica

```text
specs/001-gestionar-portafolio-innovacion/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── README.md
│   ├── organizacion.md
│   ├── seguridad.md
│   ├── portafolio.md
│   ├── documentos.md
│   ├── reportes.md
│   ├── consulta.md
│   ├── prototipos.md (diferido)
│   └── auditoria.md
└── checklists/
```

La implementación se descompone en [tasks.md](./tasks.md), con ownership por especialista, gates
Oracle y de prototipos, pruebas obligatorias y trazabilidad de requisitos.

### Código fuente objetivo

```text
apps/backend/business-domain/ms-piip/src/
├── main/java/pe/gob/midagri/piip/
│   ├── organizacion/{controller,service/impl,repository,dto,entity,exception,mapper,event}
│   ├── seguridad/{controller,service/impl,repository,dto,entity,exception,mapper,event}
│   ├── portafolio/{controller,service/impl,repository,dto,entity,exception,mapper,event}
│   ├── documentos/{controller,service/impl,repository,dto,entity,exception,mapper,event}
│   ├── reportes/{controller,service/impl,repository,dto,entity,exception,mapper,event}
│   ├── consulta/{controller,service/impl,repository,dto,entity,exception,mapper,event}
│   └── auditoria/{controller,service/impl,repository,dto,entity,exception,mapper,event}
└── test/java/pe/gob/midagri/piip/

apps/frontend/src/app/
├── core/{auth,effective-assignment,config,http,layout}
├── shared/{accessibility,forms,ui}
└── features/
    ├── organizacion/catalogos-planeamiento/
    ├── seguridad/matriz-funcion-perfil-unidad/
    ├── portafolio/{registro,evaluacion,decision,proyectos,seguimiento,producto-final,cierre}
    ├── documentos/
    ├── consulta-institucional/
    ├── consulta-publica/
    ├── reportes/
    └── portafolio/prototipos/ (diferido)

database/
├── ddl/{auditoria,organizacion,seguridad,portafolio,documentos,reportes,transversal}/
├── indexes/
├── seeds/
└── CHANGELOG.md
```

**Decisión estructural**: Se conservan exactamente los siete módulos constitucionales. Prototipos y
mediciones son una subcapacidad de `portafolio`; PEI/POI pertenecen a `organizacion`; el catálogo
`tipodocumento` actual se integra en `documentos`. Cada módulo accede solo a sus repositorios y usa
servicios, DTO o eventos internos para interactuar con otro módulo.

### Brechas del repositorio actual

- El backend solo contiene la capacidad `tipodocumento`; faltan los siete límites modulares objetivo.
- La autorización actual deriva roles del JWT; debe reemplazarse por asignación efectiva Oracle.
- La configuración actual fija issuer, audience, contexto `/api/ms-piip/v1` y multipart de 10 MB.
- Existen credenciales Oracle y material de wallet dentro de recursos del proyecto. Antes de ejecutar
  localmente deben retirarse de archivos versionados, rotarse y referenciarse por variables/rutas
  locales seguras. Este plan no manipula esos secretos.
- `shared-data` contiene artefactos heredados y una ruta `db/migration`; no se usarán para crear o
  migrar el esquema PIIP ni se introducirá Flyway/Liquibase.
- Angular es un scaffold sin `core/shared/features`, Material, OIDC/PKCE, rutas lazy o Playwright;
  US9 y sus gates de prototipos están diferidos de la Fase 1 actual.
- Las pruebas actuales no cubren autorización Oracle, auditoría, estados, concurrencia, documentos,
  Keycloak, reportes ni límites arquitectónicos.

## Diseño modular

| Módulo | Agregados y responsabilidades | Contratos consumidores |
|---|---|---|
| `organizacion` | Unidad y versiones independientes de Objetivo PEI y Actividad POI aprobadas por planeamiento. | Seguridad, portafolio, consulta y reportes. |
| `seguridad` | Usuario, matriz versionada función-perfil-unidad concreta, asignación, revocación, suplencia, ciclo Keycloak y asignación efectiva. | Todos los casos institucionales. |
| `portafolio` | Registro iniciativa/proyecto, relación de origen, titular, participantes, evaluación, incorporación, ciclos, producto y cierre. | Documentos, consulta y reportes. |
| `documentos` | Expediente institucional, serie con propietario excluyente, versión, BLOB Oracle, hash, clasificación y publicación. | Organización, seguridad, portafolio, reportes y consulta. |
| `reportes` | Solicitud, snapshot, generación PDF/XLSX, aprobación, destinatarios, remisión y expediente. | Evaluador y consulta institucional autorizada. |
| `consulta` | Proyecciones institucional y pública ya minimizadas. | Angular institucional y acceso anónimo. |
| `auditoria` | Eventos y accesos append-only, incluidos denegados. | Todos los módulos mediante servicio o evento interno. |

## Decisiones de diseño

### Propiedad de reglas y transacciones

| Regla o decisión | Propietario | Autoridad única | Transacción y recuperación |
|---|---|---|---|
| Código `AAAA-PREFIJO_UNIDAD-NNNNN` | `portafolio` | `CodigoProyectoService` Java/JPA | `PESSIMISTIC_WRITE` sobre año/unidad; código y registro en una transacción; UK final. |
| Presentación y 23 campos | `portafolio` | Servicio de aplicación por caso de uso | Valida matriz, catálogos y cardinalidades; inserta registro y auditoría juntos. |
| Proyecto derivado único | `portafolio` | Servicio de creación derivada | Bloquea iniciativa; crea proyecto y vínculo; UK por iniciativa resuelve carrera. |
| Proyecto directo | `portafolio` | Servicio de proyecto directo | Revalida Autoridad/Evaluador, documento formal y origen antes del commit. |
| Máquina de estados | `portafolio` | `TransicionEstadoService` Java | Bloqueo del registro, revalidación de asignación, estado, historial y auditoría atómicos. |
| Evidencia apta | `documentos` | `DocumentoService` | Valida formato, integridad SHA-256, clasificación y reglas de negocio; versión formalizada append-only. |
| Propiedad documental | `documentos` | `ExpedienteInstitucionalService` y `DocumentoService` | Serie con propietario XOR portafolio/expediente; pertenencia inmutable. |
| Publicación documental | `documentos` | `PublicacionDocumentoService` | Evaluador confirma versión `PUBLICO`; fecha del servidor y auditoría atómicas. |
| Clasificación y reclasificación | `documentos` para documentos; `portafolio` para campos | Servicios propietarios | Decisión, registrador, historial y auditoría en una transacción; acceso posterior revalida. |
| Versiones PEI/POI | `organizacion` | `ObjetivoPeiCatalogService` y `ActividadPoiCatalogService` | Ciclos independientes, aprobación formal, versiones inmutables y referencias históricas. |
| Matriz funcional | `seguridad` | `MatrizAsignacionService` | Combinación única función-perfil-unidad concreta; versionado e inactivación append-only. |
| Asignación efectiva | `seguridad` | `AutorizacionEfectivaService` | Una asignación por operación; revalidación bajo bloqueo antes de mutación sensible. |
| Aprovisionamiento Keycloak | `seguridad` | `UsuarioProvisioningService` | Keycloak primero; si Oracle falla, identidad deshabilitada y operación recuperable, idempotente y auditada. |
| Primer `GlobalAdmin` | `seguridad` | Semilla SQL manual 021 | Crea valores canónicos, combinación, usuario por `sub`, asignación y auditoría; aborta ante cualquier antecedente y no tiene mecanismo alternativo. |
| Sustitución de Responsable | `portafolio` | Servicio de titularidad de portafolio | `seguridad` autoriza y delega por DTO; `portafolio` bloquea el registro y cambia su agregado. |
| Suplencia y último `GlobalAdmin` | `seguridad` | Servicios de asignación | Bloqueo pesimista, control de solape/reemplazo y auditoría atómica. |
| Ciclos quincenales | `portafolio` | Servicio de seguimiento | Cierre inmutable; corrección por nueva versión con evidencias. |
| Reporte institucional | `reportes` | Servicio de generación | Snapshot común para PDF/XLSX; reintento idempotente; expediente conserva fallos parciales. |
| Consulta pública | `consulta` | Servicio de proyección pública | DTO allowlist; no consulta contenido documental ni genera URL de descarga. |
| Auditoría | `auditoria` | `AuditService` | Éxitos en transacción de negocio; denegaciones en transacción independiente. |
| Manejo de errores HTTP | Cada módulo propietario | Un `@RestControllerAdvice` acotado por módulo | Construye `application/problem+json` canónico sin filtrar datos; no existe un advice global que sustituya los límites modulares. |

### Concurrencia, idempotencia y fallos parciales

- Las raíces mutables usan `@Version` y `ETag`; `If-Match` es obligatorio para modificaciones y
  transiciones.
- Correlativos, transición, proyecto derivado único, titular, suplencia y último `GlobalAdmin` usan
  bloqueo pesimista donde debe prevalecer la primera confirmación.
- `Idempotency-Key` es obligatorio en creación, transición, carga/versionado documental,
  aprovisionamiento y generación de reportes. Misma clave y mismo payload devuelve el resultado
  original; payload distinto produce `409`.
- Los archivos se escriben primero en ubicación temporal, se calcula SHA-256 y se confirma el
  metadato. Un fallo elimina solo el temporal no referenciado; nunca una versión formalizada.
- La generación de reportes usa estado de operación y puede responder `202`; un fallo conserva el
  snapshot, parámetros y error para reintento.
- Las respuestas idempotentes se conservan inicialmente siete días mediante configuración. La
  expiración técnica nunca elimina auditoría, expedientes ni evidencia funcional.
- Si Keycloak crea la identidad y Oracle falla, la identidad queda deshabilitada y la operación queda
  recuperable, idempotente y auditada para reintento; no se duplica ni se considera activa.
- La semilla 021 prevalida la inexistencia histórica de `GlobalAdmin` y aborta sin cambios ante
  cualquier antecedente o reejecución. La aprobación de despliegue y el `sub` son parámetros
  obligatorios; ningún servicio Java reproduce esta inicialización.
- No se usa borrado físico como recuperación. Las compensaciones inactivan capacidades o crean
  eventos/versiones posteriores sin perder historia.

### Persistencia y cambios SQL

**Objetos existentes**: las 13 tablas y 10 secuencias del baseline permanecen. Se amplían
`PROYECTO`, `PROYECTO_UNIDAD_ORGANICA`, `USUARIO_ROL_UNIDAD`, `SECUENCIA_CODIGO`,
`TRANSICION_ESTADO`, `TIPO_DOCUMENTO`, `DOCUMENTO`, `AUDITORIA_ACCESO` y `AUDITORIA_EVENTO`.
`TRANSICION_PERMITIDA` queda como legado inactivo y no es autoridad. La descripción semilla de
`UnidadAdmin` se corrige porque no existe herencia a descendientes. `DOCUMENTO.SCAN_ANTIVIRUS` y
`DOCUMENTO.NOMBRE_STORAGE` quedan como columnas legacy inactivas y nullable, sin default, constraint,
mapeo JPA, contrato ni consumidor.

**Objetos nuevos**: se detallan en [data-model.md](./data-model.md), incluidos expediente
institucional, series y publicación documental, versiones independientes PEI/POI, matriz funcional,
relación iniciativa-proyecto, titular y participantes, evaluación, ciclos, incorporación, reportes,
prototipos e idempotencia. El diccionario físico debe incorporar BLOB documental Oracle y los datos
necesarios para que la semilla 021 audite la inicialización sin fijar nombres físicos antes de la
revisión humana DB.

Antes de crear estos objetos, `database-specialist` completa el diccionario físico y lo somete a
revisión humana DB. Los scripts son de ejecución única y fail-fast: prevalidan versión, esquema,
objetos y datos antes del primer DDL. Cada script se registra como `PENDIENTE` en
`database/CHANGELOG.md` en el mismo cambio que lo deposita.

| Orden | Script incremental futuro | Dependencias | Compensación segura |
|---:|---|---|---|
| 002 | `database/ddl/auditoria/002_auditoria_idempotencia.sql` | 001 | Dejar de escribir campos nuevos; nunca eliminar auditoría o claves ya consumidas. |
| 003 | `database/ddl/documentos/003_expediente_serie_version.sql` | 002 | Adaptar tipo documental y BLOB Oracle por contexto e inactivar `SCAN_ANTIVIRUS` y `NOMBRE_STORAGE` sin borrar datos legacy; detener nuevas cargas y conservar expedientes, series y versiones. |
| 004 | `database/ddl/documentos/004_documento_publicacion.sql` | 003 | Detener publicaciones; conservar confirmaciones y auditoría. |
| 005 | `database/ddl/organizacion/005_objetivo_pei_versionado.sql` | 003 | Inactivar versiones no usadas; preservar referencias históricas. |
| 006 | `database/ddl/organizacion/006_actividad_poi_versionada.sql` | 003 | Igual, con ciclo POI independiente. |
| 007 | `database/ddl/seguridad/007_matriz_funcional_versionada.sql` | 003 | Detener nuevas versiones; conservar combinaciones históricas. |
| 008 | `database/ddl/seguridad/008_usuario_rol_unidad_vigencia.sql` | 002, 003, 007 | Detener nuevas asignaciones/suplencias y conservar historial. |
| 009 | `database/ddl/portafolio/009_proyecto_campos_oficiales.sql` | 005, 006 | Mantener columnas legacy; no restaurar checks si hay estados nuevos. |
| 010 | `database/ddl/portafolio/010_iniciativa_proyecto_relacion.sql` | 009 | Detener nuevas relaciones; conservar vínculos confirmados. |
| 011 | `database/ddl/portafolio/011_proyecto_unidades_responsables.sql` | 009 | Mantener referencia legacy hasta corte confirmado. |
| 012 | `database/ddl/portafolio/012_responsables_participantes.sql` | 008, 009, 011 | Deshabilitar altas; conservar titularidades y participaciones. |
| 013 | `database/ddl/portafolio/013_clasificacion_campos.sql` | 002, 009 | Volver datos nuevos no publicables; nunca ampliar acceso. Debe estar confirmado antes de formularios, validaciones, consultas o reportes que dependan de la matriz de clasificación. |
| 014 | `database/ddl/portafolio/014_evaluacion_transiciones.sql` | 003, 008, 009 | Detener comandos; no revertir estados confirmados. |
| 015 | `database/ddl/portafolio/015_ciclos_resultados_cierre.sql` | 003, 009, 014 | Detener cierres/ciclos; conservar versiones cerradas. |
| 016 | `database/ddl/portafolio/016_incorporacion_individual.sql` | 003, 010, 012 | Mantener expedientes `PENDIENTE`; no borrar evidencia. |
| 017 | `database/ddl/reportes/017_reporte_expediente_remision.sql` | 002, 003, 009, 015 | Detener generación/remisión; conservar expedientes. |
| 018 | Diferido a una fase posterior; no se deposita DDL en la Fase 1 actual. | No aplicable | No aplicable. |
| 019 | `database/seeds/019_catalogos_canonicos_portafolio.sql` | 003-018 | Inactivar semillas no referenciadas; nunca borrar referencias. |
| 020 | `database/seeds/020_planeamiento_inicial_aprobado.sql` | 005, 006 | Requiere datasets y documentos aprobados; no inventar valores. |
| 021 | `database/seeds/021_matriz_funcional_inicial_aprobada.sql` | 002, 007, 008 | Crea valores, combinación y primer `GlobalAdmin` con `sub` y aprobación de despliegue; aborta ante antecedentes y nunca revierte una asignación confirmada. Se deposita y ejecuta después de estas confirmaciones y antes de US1, como excepción documentada al orden numérico. |
| 022 | Diferido a una fase posterior; no se deposita DDL de backfill en la Fase 1 actual. | No aplicable | No aplicable. |
| 023 | Diferido a una fase posterior; requiere revisión de dependencias, objetos y del índice existente. | No aplicable | No aplicable. |
| 024 | Diferido a una fase posterior; no se deposita DDL de corte legacy en la Fase 1 actual. | No aplicable | No aplicable. |

Cada script debe prevalidar datos antes del primer DDL, documentar commits implícitos Oracle,
dependencias y compensación, y registrarse `PENDIENTE` en `database/CHANGELOG.md`. La semilla 021 es
la única excepción de orden: conserva su identificador, pero su ejecución sigue las dependencias 002,
007 y 008 confirmadas para habilitar el primer `GlobalAdmin` antes de US1. Este plan no crea ni ejecuta
esos scripts y no actualiza `database/database-schema.md`.

### Contratos API, Keycloak, privacidad y auditoría

- Los contratos de capacidades están en [contracts/](./contracts/README.md); no exponen entidades
  JPA ni nombres de tablas.
- Las peticiones institucionales presentan el identificador de una asignación efectiva; el backend
  comprueba que pertenece al `sub` autenticado y nunca acepta perfil o unidad del body como autoridad.
- Keycloak Admin API solo se usa para aprovisionar, activar, desactivar y reactivar identidades.
  Ningún DTO contiene contraseña.
- No existe contrato API para la semilla inicial `GlobalAdmin`; sus parámetros y auditoría pertenecen
  exclusivamente al script 021 y a su procedimiento manual de ejecución.
- Los DTO públicos son distintos de los institucionales. Ausencia de clasificación nunca significa
  `PUBLICO`.
- Toda operación sensible y denegación registra actor, asignación, perfil, unidad, instante,
  operación, resultado, correlación y cambios mínimos necesarios, sin tokens ni contenido de archivo.

## Estrategia frontend

Cada feature Angular será lazy y standalone. `core` concentra OIDC, contexto de asignación,
interceptores y shell; `shared` solo presentación reutilizable. Guards y acciones ocultas mejoran UX,
pero no autorizan ni deciden transiciones. OGTI configura fuera del repositorio el tema de inicio de
sesión personalizado de Keycloak; Angular solo verifica el redireccionamiento OIDC hacia el ambiente
aprobado y nunca administra credenciales.

La consulta pública se entrega como ruta Angular lazy y anónima del lado del cliente. No se incorpora
SSR porque la especificación no establece una necesidad aprobada de SEO o renderizado en servidor.

Antes de implementar cada recorrido de registro, evaluación, decisión, seguimiento, producto final,
cierre, consulta institucional o consulta pública se exige:

1. Prototipo PIIP en `APROBADO`, con código y versión.
2. Validación por cada perfil involucrado y actor sectorial cuando aplique.
3. Aprobador distinto del autor y del único validador.
4. Evidencia de escritorio, móvil, teclado y lector de pantalla aplicable.
5. Medición inicial y matriz de metas aprobadas.
6. Cero errores críticos y cero hallazgos críticos o altos de accesibilidad.

`REGISTRO` incluye iniciativa y proyectos derivados/directos; `SEGUIMIENTO` incluye suspensión y
`DECISION` incluye cancelación. Seguridad, reportes y administración de prototipos no agregan gates.
Las pruebas Vitest y Playwright que fijan componentes o journeys también esperan el mismo gate que su
interfaz. Antes de liberar una versión y después de cada cambio funcional o de accesibilidad, el
backend exige una medición aprobada correspondiente exactamente a la versión candidata.

No existe esa evidencia en el proyecto; por tanto, el plan define estructura y contratos, pero el
BUILD de interfaces está bloqueado.

## Trazabilidad de calidad y pruebas

| Riesgo/capacidad | Pruebas obligatorias |
|---|---|
| Límites modulares | ArchUnit: controlador sin repositorio/entidad; servicio sin entidad en contrato; módulo sin repositorio ajeno; ausencia de paquetes prohibidos; exactamente un `@RestControllerAdvice` acotado por cada módulo constitucional. |
| Autorización perfil-ámbito | JUnit/MockMvc y Oracle Testcontainers con asignación válida, futura, vencida, revocada, suplida, unidad distinta y no herencia a descendientes. |
| Validación JWT | Pruebas de issuer, audience, firma, vigencia y claims estándar necesarios; los tokens inválidos no alcanzan la autorización efectiva. |
| Iniciativa y proyecto | Código concurrente, registros independientes, vínculo inmutable, UK de un derivado, proyecto directo formal y trazabilidad. |
| Máquina de estados | Todas las transiciones listadas, rechazo de inexistentes y terminales, rol decisor/registrador y primera confirmación concurrente. |
| 23 campos | Matriz por tipo/etapa, límites, `TRIM`, condicionales `OTROS`/digital, cardinalidades y edición de subsanación/ejecución. |
| Seguimiento | Ciclo quincenal completo, periodo aplicable, evidencia, cierre inmutable y corrección versionada. |
| Documentos | Contexto de tipo/propietario, XOR, BLOB Oracle, 100 MB menos un byte, exactamente 100 MB y 100 MB más un byte; MIME, SHA-256, clasificación, versión e inmutabilidad. |
| Privacidad | DTO público allowlist, participante restringido, reclasificación inmediata y ausencia de endpoint/enlace de descarga pública. |
| Usuarios | Aprovisionamiento idempotente, activación, desactivación bilateral, reactivación, duplicado e identidad deshabilitada con operación recuperable ante fallo Oracle. |
| Primer `GlobalAdmin` | Prueba SQL de inexistencia histórica, valores canónicos, `sub`, aprobación de despliegue, auditoría mínima, ejecución única y aborto sin cambios ante reejecución. |
| Asignaciones | Administración por perfil, vigencia, revocación inmediata, suplencia no solapada, titular inactivo y protección del último `GlobalAdmin`. |
| Matriz funcional | Versiones y combinaciones por unidad concreta, aprobación/registrador correctos, inactivación, históricos y rechazo de datos discordantes. |
| PEI/POI | Aprobaciones independientes, registro GlobalAdmin, versiones inmutables, retiro, histórico y ausencia de efectos cruzados. |
| Expedientes/publicación | Propiedad XOR e inmutable, documentos institucionales aislados, confirmación por Evaluador, fecha de servidor y reclasificación restrictiva. |
| Incorporación | `PENDIENTE/VALIDADO/RECHAZADO`, correcciones ilimitadas, conflicto de código/relación, duplicado vinculado y auditoría original. |
| Reportes | Cortes 30/06 y 31/12, BR-122 y denominador cero, filtros, snapshot PDF/XLSX, clasificación, aprobación, remisión y recuperación parcial. |
| Prototipos | Diferido a una fase posterior; no aplica a las pruebas activas. |
| Cierre | Ambos caminos `PRODUCTO_APROBADO` y `PRODUCTO_NO_APROBADO` a `FINALIZADO`, requisitos completos y fecha automática. |
| Frontend | Vitest de formularios/errores/asignación; Playwright de ocho recorridos en escritorio/móvil, teclado, lector de pantalla aplicable y consulta pública. |

## Fases locales de implementación posteriores

1. Alinear contratos y modelo con la Constitución 5.0.0 y las aclaraciones C1/C2 aprobadas.
2. Aprobar modelo físico y depositar scripts 002-024 como `PENDIENTE`; ejecución siempre manual.
3. Construir fundaciones de auditoría, organización y seguridad, incluido Keycloak idempotente, y ejecutar manualmente la semilla 021 tras confirmar 002, 007 y 008.
4. Construir portafolio, documentos y máquina de estados mediante JPA.
5. Construir seguimiento, incorporación, reportes y consulta.
6. Registrar y aprobar prototipos, mediciones y metas por etapa.
7. Implementar cada uno de los ocho recorridos únicamente después de superar su gate.
8. Ejecutar las pruebas autorizadas y realizar el Constitution Check de implementación.

## Constitution Check posterior al diseño

| Control | Resultado final |
|---|---|
| Arquitectura, módulos, DTO y contratos | CUMPLE |
| Autoridad única y transacciones | CUMPLE: Java/JPA; sin SP funcionales |
| Seguridad, privacidad y auditoría | CUMPLE EN DISEÑO |
| Estados, documentos, códigos y 23 campos | CUMPLE EN DISEÑO |
| Iniciativa-proyecto y proyectos directos | CUMPLE |
| SQL incremental y compensación | CUMPLE EN DISEÑO; no ejecutado |
| Pruebas constitucionales | CUMPLE EN PLAN; no ejecutadas |
| Prototipos antes de UI | DIFERIDO por la enmienda constitucional 5.0.0 |
| Decisiones funcionales completas | CUMPLE |
| Alcance Fase 1 | CUMPLE |

**Resultado**: El diseño preserva la Constitución 5.0.0 y no mantiene aclaraciones materiales C1/C2.
DDL, OIDC integrado, semillas y backfill avanzan únicamente después de sus gates explícitos. La
semilla 021 se deposita y ejecuta manualmente; US9 y el incremento 018 quedan diferidos.

## Constitution Check final

*Verificación contra la Constitución 5.0.0 tras el diseño completo. Fuente única de verdad:
`.specify/memory/constitution.md`.*

### I. Autoridad única para reglas de negocio (Sección IV)

| Control | Resultado | Evidencia |
|---|---|---|
| Servicios Java/JPA como fuente autoritativa de todas las reglas | **CUMPLE** | `TransicionEstadoService`, `CodigoProyectoService`, `AutorizacionEfectivaService`, `DocumentoService`, `ExpedienteInstitucionalService`, `PublicacionDocumentoService`, `MatrizAsignacionService`, `UsuarioProvisioningService` y todos los servicios de portafolio. Sin procedimientos almacenados funcionales paralelos. |
| Ausencia de duplicación Java/PL/SQL | **CUMPLE** | El plan usa JPA para toda lógica de negocio. Oracle aporta integridad estructural, secuencias y bloqueo pesimista. Los scripts 002-024 son DDL y semillas, no lógica funcional. |
| Semilla 021 como única excepción de bootstrap | **CUMPLE** | La semilla SQL 021 crea exclusivamente el primer `GlobalAdmin` con `sub` proporcionado por OGTI, aprobación de despliegue y auditoría mínima. Aborta ante cualquier antecedente histórico. Ningún servicio Java reproduce esta inicialización. |

### II. Simplicidad modular (Sección II y Límites de módulos)

| Control | Resultado | Evidencia |
|---|---|---|
| Siete módulos constitucionales: `organizacion`, `seguridad`, `portafolio`, `documentos`, `reportes`, `consulta`, `auditoria` | **CUMPLE** | Estructura de proyecto en [data-model.md](./data-model.md) y tabla de agregados. Cada módulo tiene `controller/`, `service/impl/`, `repository/`, `dto/`, `entity/`, `exception/`, `mapper/`, `event/`. |
| Sin directorios genéricos `model/`, `client/` ni `integration/` | **CUMPLE** | Plan explícitamente los prohíbe y la matriz de pruebas ArchUnit los verifica. |
| Comunicación entre módulos exclusivamente por servicios, DTO o eventos internos | **CUMPLE** | Tabla de diseño modular con contratos consumidores y productores. Las referencias entre módulos son identificadores, no asociaciones JPA navegables (data-model.md línea 9). |
| Controladores delgados que solo validan DTO y delegan | **CUMPLE** | Contratos en [contracts/README.md](./contracts/README.md) y cada módulo provee su `@RestControllerAdvice` acotado. |

### III. Seguridad, privacidad y auditabilidad desde el diseño (Sección III)

| Control | Resultado | Evidencia |
|---|---|---|
| Keycloak fuente autoritativa de identidad y credenciales | **CUMPLE** | `keycloak-js`, OIDC Authorization Code Flow con PKCE, `keycloakId` UK UUID en `UsuarioPiip`. Backend valida emisor, audiencia, firma, vigencia y `sub`. |
| Oracle fuente autoritativa de roles, permisos y alcance organizacional | **CUMPLE** | `AsignacionFuncional` con `MatrizFuncionPerfilUnidad` concreta,vigencia, revocación, suplencia y `@Version`. Asignación efectiva Oracle revalidada antes de cada operación sensible. |
| Ausencia de clasificación nunca equivale a `PUBLICO` | **CUMPLE** | Matriz de 23 campos en [data-model.md](./data-model.md) líneas 242-269 define privacidad explícita. DTO allowlist en contracts/README.md líneas 94-103. |
| Consulta pública: 4 campos y metadatos, sin contenido ni descarga | **CUMPLE** | `PublicPortfolioSummary` y `PublicDocumentMetadata` en contracts. `consulta` sin persistencia propia. Sin endpoint público hacia contenido documental. |
| OGTI antimalware fuera de PIIP; sin estados, gates ni contratos antimalware | **CUMPLE** | `DOCUMENTO.SCAN_ANTIVIRUS` y `DOCUMENTO.NOMBRE_STORAGE` legacy inactivas, nullable, sin mapeo JPA ni consumidor. |
| Auditoría inmutable: actor, asignación, perfil, unidad, instante, operación, resultado, correlación | **CUMPLE** | `EventoAuditoria` y `AuditoriaAcceso` append-only. Denegaciones en transacción independiente. Éxitos en transacción de negocio. Sin tokens ni contenido de archivo en registro. |

### IV. Persistencia y documentos (Sección Persistencia y documentos)

| Control | Resultado | Evidencia |
|---|---|---|
| `PROYECTO` como agregado central; iniciativa y proyecto como registros independientes | **CUMPLE** | `RegistroPortafolio` con `TIPO_REGISTRO` diferenciando ambos sin compartir identidad, código o historial. `RelacionIniciativaProyecto` inmutable vincula el proyecto derivado único. |
| 13 tipos documentales con condiciones | **CUMPLE** | Matriz en data-model.md líneas 262-276. `TipoDocumento` evoluciona `TIPO_DOCUMENTO` con `contexto` (`PORTAFOLIO`/`INSTITUCIONAL`). |
| SHA-256, versiones, BLOB Oracle, máximo 100 MB | **CUMPLE** | `DocumentoVersion` con `hashSha256` (64 hex), `tamanoBytes` `1..104857600`, `contenido` BLOB Oracle, `numeroVersion`, cadena inmutable. |
| Propietario excluyente XOR entre serie documental y expediente/iniciativa | **CUMPLE** | CHECK XOR `registroId` / `expedienteInstitucionalId` en `DocumentoVersion`. Pertenencia inmutable tras formalización. |
| Documentos formalizados y transiciones inmutables; corrección produce nueva versión | **CUMPLE** | `formalizado` impide actualización o eliminación. Correcciones insertan `CicloEvidencia`, nueva `PlanificacionProyecto` o nueva versión de ciclo. |
| Sin descarga pública de documentos | **CUMPLE** | `PublicacionDocumento` expone solo título validado sin datos personales, tipo, versión, formato y fecha. Sin enlace ni endpoint hacia contenido. |

### V. Catálogos canónicos y máquina de estados

| Control | Resultado | Evidencia |
|---|---|---|
| 11 estados canónicos: `PRESENTADO`, `NO_ADMISIBLE`, `NO_APLICABLE`, `INICIATIVA_APROBADA`, `INICIATIVA_ARCHIVADA`, `PROYECTO_EJECUCION`, `SUSPENDIDO`, `CANCELADO`, `PRODUCTO_APROBADO`, `PRODUCTO_NO_APROBADO`, `FINALIZADO` | **CUMPLE** | Data-model.md líneas 222-238. `TransicionEstadoService` en Java. CHECK Oracle solo limita dominio. `TRANSICION_PERMITIDA` legacy no se consulta. |
| Terminales: `NO_ADMISIBLE`, `NO_APLICABLE`, `INICIATIVA_ARCHIVADA` | **CUMPLE** | Expresado en data-model.md línea 235. La máquina rechaza cualquier salida. |
| 11 transiciones controladas con decisor, registrador y evidencia | **CUMPLE** | Tabla data-model.md líneas 222-238. Cada una bloquea el registro, revalida asignación, verifica `If-Match`, actualiza estado y fecha automática, inserta historial y auditoría atómicamente. |
| Proyecto derivado crea vínculo inmutable sin transicionar la iniciativa | **CUMPLE** | `RelacionIniciativaProyecto` FK+UK. Servicio bloquea iniciativa, crea proyecto y vínculo, UK por iniciativa resuelve carrera. Código propio inmutable. |
| Proyectos directos exigiendo autoridad formal y evidencia | **CUMPLE** | `RegistroPortafolio` con `codigoOrigen` obligatorio con acto/fuente para proyecto directo. Servicio revalida `Autoridad`/`Evaluador` y documento formal. |
| Separación decisor/registrador en transiciones | **CUMPLE** | Tabla de transiciones indica rol que decide y rol que registra.，两者 pueden diferir. Documento formal obligatorio cuando aplique. |

### VI. 23 campos oficiales

| Control | Resultado | Evidencia |
|---|---|---|
| Matriz completa: obligatoriedad, editabilidad, privacidad y actor responsable por tipo/etapa | **CUMPLE** | Data-model.md líneas 242-273. Cada uno de los 23 campos tiene privacidad,editabilidad y responsables explícitos. Ausencia de clasificación nunca equivale a `PUBLICO`. |

### VII. Reportes institucionales

| Control | Resultado | Evidencia |
|---|---|---|
| Sin reportes mensuales ni trimestrales obligatorios | **CUMPLE** | Solo semestral y extraordinario. data-model.md línea 352. |
| specsavedefine periodo, contenido, indicadores, filtros, responsables, destinatarios, formato, clasificación, conservación | **CUMPLE** | Data-model.md líneas 348-366. Snapshot canónico, PDF/XLSX referenciando el mismo corte, `RESTRINGIDO` si contiene datos restringidos. |

### VIII. Pruebas (Sección Flujo de entrega, calidad y especificaciones)

| Control | Resultado | Evidencia |
|---|---|---|
| JUnit 5, Mockito, Oracle Testcontainers obligatorios en backend | **CUMPLE PARCIAL** | `mvn -pl ms-piip -am -Pintegration-tests verify` completó JUnit, Mockito, MockMvc, ArchUnit y JaCoCo. Los IT Oracle no se activaron: `CierreProyectoOracleIT` está `@Disabled` y `DocumentosOracleIT` no declara `@Test`. |
| Vitest y Playwright obligatorios en frontend | **PENDIENTE DE ENTORNO** | El type-check pasó. Vitest y Playwright no pueden ejecutarse desde este workspace WSL/UNC con runtime Node Windows; requieren un workspace en disco Windows o un runtime Node Linux compatible. |
| Cobertura mínima 80 % para código de negocio | **CUMPLE** | JaCoCo verificó el umbral configurado `jacoco.service.coverage.minimum=0.80` durante `verify`. |
| Pruebas de: máquina de estados, RBAC por unidad, correlativos, transacciones, documentos, auditoría, procedimientos almacenados, límites arquitectónicos, aprovisionamiento Keycloak, fallos parciales, modelo de autorización efectivo | **CUMPLE** | Matriz líneas 366-387. |
| Pruebas no opcionales cuando el cambio afecta seguridad, persistencia, contrato API, gestión documental o comportamiento | **CUMPLE** | Plan línea 384. La ausencia de esta evidencia bloquea el BUILD de cada recorrido. |

### IX. SQL y cambios de esquema

| Control | Resultado | Evidencia |
|---|---|---|
| Scripts SQL manuales versionados bajo `database/` | **CUMPLE** | Plan líneas 287-311. Secuencia 001-024 documentada con dependencias y compensación. |
| Sin Flyway ni Liquibase | **CUMPLE** | Plan línea 74 y constitución. `shared-data` heritage `db/migration` no se usará. |
| Ejecución siempre manual, sin cambios automáticos en bases compartidas | **CUMPLE** | Plan línea 317. Gate de revisión humana DB antes de depositar scripts. |
| Baseline vigente intacto | **CUMPLE** | Plan línea 36. Scripts incrementales solo amplían objetos existentes. |
| Compensación forward-only documentada | **CUMPLE** | Columna de compensación en tabla de scripts. Nunca destruir auditoría, columnas legacy hasta corte confirmado. |

### X. Alcance de integraciones y exclusiones

| Control | Resultado | Evidencia |
|---|---|---|
| Sin sincronizaciones, conectores ni adaptadores funcionales externos | **CUMPLE** | Plan líneas 73-77. contracts/README.md línea 8: snapshot OpenAPI comparado por contract tests antes de integrar clientes. |
| US9, prototipos, mediciones y gates diferidos a fase posterior | **CUMPLE** | Enmienda constitucional 5.0.0. data-model.md líneas 373-396 con trazabilidad diferida. |
| Sin despliegue productivo, infraestructura institucional, carga masiva | **CUMPLE** | Plan líneas 24-25 y sección de exclusiones. |

---

## Resultado del Constitution Check final

| Dimensión constitucional | Resultado |
|---|---|
| I. Autoridad única | **CUMPLE** |
| II. Simplicidad modular | **CUMPLE** |
| III. Seguridad, privacidad y auditabilidad | **CUMPLE** |
| IV. Persistencia y documentos | **CUMPLE** |
| V. Catálogos y máquina de estados | **CUMPLE** |
| VI. 23 campos oficiales | **CUMPLE** |
| VII. Reportes institucionales | **CUMPLE** |
| VIII. Pruebas | **CUMPLE PARCIAL** |
| IX. SQL y cambios de esquema | **CUMPLE** |
| X. Integraciones y exclusiones | **CUMPLE** |

**Resultado**: La implementación preserva las reglas evaluadas de la Constitución 5.0.0. Permanecen dos verificaciones pendientes: activar los IT Oracle y ejecutar Vitest/Playwright en un entorno con rutas y runtime compatibles. No se deben considerar cerradas hasta registrar esa evidencia.

---

## Seguimiento de complejidad

No se solicita ni aprueba ninguna excepción constitucional. La estrategia JPA evita una segunda
implementación funcional en PL/SQL y el adaptador local de almacenamiento queda detrás de
`DocumentStorage`.
