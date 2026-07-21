# Plan de implementación: Gestión del Portafolio Institucional de Innovación Pública

**Rama**: `001-gestionar-portafolio-innovacion` | **Fecha**: 2026-07-21 | **Especificación**: [spec.md](./spec.md)

**Entrada**: Especificación funcional activa de la Fase 1, con aclaraciones CL aprobadas.

**Estado del plan**: Diseño técnico generado con gates funcionales pendientes. No habilita BUILD de
las capacidades afectadas por los bloqueos de la sección Constitution Check.

## Resumen

La Fase 1 se implementará como un monolito modular Spring Boot y una aplicación Angular. El agregado
central persistente `PROYECTO` representará iniciativas y proyectos como registros independientes;
un vínculo uno a cero-o-uno conservará de forma inmutable el origen del único proyecto derivado. Los
servicios de aplicación Java/JPA serán la única fuente autoritativa de reglas, transacciones,
autorización y máquina de estados. Oracle aportará persistencia, restricciones, índices y bloqueo de
filas, sin procedimientos funcionales paralelos.

El diseño cubre organización, seguridad, portafolio, documentos, reportes, consulta y auditoría. La
implementación de cada interfaz Angular queda condicionada a su prototipo PIIP aprobado, medición
inicial y matriz de metas. No se incluyen despliegue productivo, infraestructura institucional,
sincronizaciones externas, cargas masivas ni funcionalidades de Fase 2.

## Contexto técnico

**Backend**: Java 21, Spring Boot 3.2.5, Maven reactor, Spring Data JPA, Spring Security OAuth 2.0
Resource Server, MapStruct 1.5.5.Final y Springdoc OpenAPI 2.5.0.

**Frontend**: Angular 22, Angular Material, componentes standalone, TypeScript estricto, diseño
responsive y WCAG 2.1 AA.

**Persistencia**: Oracle 19c+ en `KALLPA_PIIP`; SQL manual incremental bajo `database/`. El baseline
vigente es `database/ddl/init/001_baseline_piip.sql`; la ruta literal
`database/ddl/001_baseline_kallpa_piip.sql` no existe, aunque el encabezado del baseline vigente usa
ese nombre histórico.

**Identidad**: Keycloak 26 compatible, OIDC Authorization Code Flow con PKCE. Keycloak es autoridad
de identidad y credenciales; Oracle PIIP lo es de perfiles, permisos y ámbitos.

**API**: REST OpenAPI 3.0 bajo `/api/v1`, DTO específicos por caso de uso y clasificación,
`application/problem+json`, paginación base cero, idempotencia y control de concurrencia.

**Documentos**: `DocumentStorage` en el módulo `documentos`; adaptador local de filesystem solo para
desarrollo local. SHA-256, versiones, máximo inclusivo de 100 MB, formatos PDF, OOXML, JPEG y PNG, y
estado antimalware `PENDIENTE`, `LIMPIO` o `INFECTADO`. El proveedor institucional y el motor de
análisis no se infieren ni se integran en este plan.

**Pruebas**: JUnit 5, Mockito, Oracle Testcontainers, ArchUnit y JaCoCo en backend; Vitest y
Playwright en frontend. Cobertura mínima de 80 % para código de negocio.

**Tipo de proyecto**: Monolito modular con frontend web Angular.

**Objetivos de rendimiento**: Los objetivos funcionales son exactitud del 100 % para autorización,
transiciones, trazabilidad, privacidad y evidencias. Las metas de experiencia por recorrido se fijan
tras la medición inicial según BR-149; no se inventa un objetivo de latencia o concurrencia no
aprobado. `MV_PORTAFOLIO_RESUMEN` no se crea sin evidencia de necesidad.

**Restricciones**: Sin Flyway/Liquibase; sin ejecución automática de SQL; sin conectores o
sincronización externa; sin descarga pública documental; sin reglas de negocio en controladores,
Angular o PL/SQL; sin `model/`, `client/` o `integration/` genéricos.

**Escala y alcance**: Desarrollo local de las nueve historias de usuario y los ocho recorridos
críticos de la Fase 1 para MIDAGRI y entidades sectoriales dentro de unidades asignadas
explícitamente. La capacidad y dimensionamiento productivos quedan fuera de alcance.

## Constitution Check inicial

*Gate realizado antes del diseño contra la Constitución 3.2.0.*

| Control | Resultado | Evidencia o acción |
|---|---|---|
| Especificación aprobada y desconocidos materiales trazados | **BLOQUEO PARCIAL** | La invocación y el checklist confirman las CL, pero `spec.md` conserva un `Status` desactualizado. Además faltan tres decisiones funcionales listadas abajo. |
| Propósito, actores, reglas, estados, excepciones y aceptación | CUMPLE | `spec.md` define US1-US9, BR-001 a BR-149, FR-001 a FR-160 y criterios medibles. |
| Límites modulares, DTO y transacciones | CUMPLE EN DISEÑO | Se asignan propietarios e interacciones en este plan y [contracts/README.md](./contracts/README.md). |
| Fuente autoritativa única | CUMPLE EN DISEÑO | Servicios Java/JPA; Oracle solo garantiza integridad y concurrencia. |
| Autorización efectiva | CUMPLE EN DISEÑO | Una asignación Oracle seleccionada por operación, revalidada antes del cambio sensible. |
| Privacidad, auditoría y documentos | CUMPLE CON GATE | Matriz consolidada en [data-model.md](./data-model.md); publicación documental permanece bloqueada. |
| Catálogos, estados, transiciones, documentos y códigos | CUMPLE EN DISEÑO | Se preservan valores canónicos y se corrigen únicamente mediante SQL incremental. |
| 23 campos oficiales | CUMPLE EN DISEÑO | Obligatoriedad, edición, privacidad y responsable se consolidan sin alterar BR aprobadas. |
| Iniciativa y proyecto separados | CUMPLE | Relación derivada inmutable y única; proyectos directos exigen autoridad y evidencia. |
| Decisor y registrador | CUMPLE | Se modelan ambos actores y el documento formal en cada transición aplicable. |
| Consulta pública minimizada | CUMPLE CON GATE | Cuatro campos públicos; metadatos documentales solo después de resolver publicación. Nunca contenido o descarga. |
| Prototipos aprobados | **GATE DE BUILD FRONTEND** | No hay evidencia local de prototipos aprobados; ninguna interfaz de recorrido puede implementarse todavía. |
| SQL incremental, orden y compensación | CUMPLE EN DISEÑO | Scripts 002-018 propuestos; baseline intacto; compensación forward-only. |
| Estrategia de pruebas | CUMPLE EN DISEÑO | Matriz de trazabilidad incluida al final de este plan. |
| Exclusiones de Fase 1 | CUMPLE | No se diseñan integraciones externas, carga masiva ni transiciones futuras. |

### Bloqueos funcionales

| Bloqueo | Requisito afectado | Efecto |
|---|---|---|
| No están aprobados los valores iniciales, códigos estables ni autoridad de mantenimiento de Objetivo PEI y Actividad POI. | BR-036, BR-058, FR-048, FR-069 y Constitución 3.2.0, campos 10 y 11. | Se pueden diseñar tablas y contratos de lectura; no se habilita presentación o creación productiva ni mantenimiento del catálogo. |
| No está incluida la matriz concreta cargo o función-perfil-unidad. | Constitución 3.2.0, líneas 126-131; FR-026 a FR-028 y FR-042 a FR-066. | Se puede diseñar la asignación y sus controles; no se habilita administración funcional completa hasta cargar una matriz aprobada. |
| No se define quién confirma la publicación documental ni qué evento fija `fechaPublicacion`. | BR-084, FR-034, FR-095 y PSA-007. | Los documentos permanecen no publicables; la consulta pública expone solo los cuatro campos del portafolio hasta aclaración aprobada. |

El `Status` desactualizado de `spec.md` es una inconsistencia documental adicional. No cambia las CL
confirmadas, pero debe corregirse por el propietario de la especificación antes de declarar el gate
completamente aprobado.

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
│   ├── prototipos.md
│   └── auditoria.md
└── checklists/
```

No se genera `tasks.md` en esta fase.

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
    ├── portafolio/{registro,evaluacion,decision,proyectos,seguimiento,producto-final,cierre}
    ├── documentos/
    ├── seguridad/
    ├── consulta-institucional/
    ├── consulta-publica/
    ├── reportes/
    └── prototipos/

database/
├── ddl/{auditoria,seguridad,portafolio,documentos,reportes}/
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
- Angular es un scaffold sin `core/shared/features`, Material, OIDC/PKCE, rutas lazy o Playwright; el
  BUILD de recorridos además depende del gate de prototipos.
- Las pruebas actuales no cubren autorización Oracle, auditoría, estados, concurrencia, documentos,
  Keycloak, reportes ni límites arquitectónicos.

## Diseño modular

| Módulo | Agregados y responsabilidades | Contratos consumidores |
|---|---|---|
| `organizacion` | Unidad, Objetivo PEI, Actividad POI y vigencias. | Seguridad, portafolio, consulta y reportes. |
| `seguridad` | Usuario, asignación perfil-unidad, revocación, suplencia, ciclo Keycloak y asignación efectiva. | Todos los casos institucionales. |
| `portafolio` | Registro iniciativa/proyecto, relación de origen, titular, participantes, evaluación, incorporación, ciclos, producto, cierre y prototipos. | Documentos, consulta y reportes. |
| `documentos` | Serie, versión, almacenamiento, hash, antimalware, clasificación, validación y reclasificación. | Portafolio, reportes y consulta. |
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
| Evidencia apta | `documentos` | `DocumentoService` | Solo `LIMPIO` y clasificación validada; versión formalizada append-only. |
| Clasificación y reclasificación | `documentos` para documentos; `portafolio` para campos | Servicios propietarios | Decisión, registrador, historial y auditoría en una transacción; acceso posterior revalida. |
| Asignación efectiva | `seguridad` | `AutorizacionEfectivaService` | Una asignación por operación; revalidación bajo bloqueo antes de mutación sensible. |
| Aprovisionamiento Keycloak | `seguridad` | `UsuarioProvisioningService` | Keycloak primero y Oracle después; idempotencia, compensación o fallo recuperable auditado. |
| Suplencia y último `GlobalAdmin` | `seguridad` | Servicios de asignación | Bloqueo pesimista, control de solape/reemplazo y auditoría atómica. |
| Ciclos quincenales | `portafolio` | Servicio de seguimiento | Cierre inmutable; corrección por nueva versión con evidencias. |
| Reporte institucional | `reportes` | Servicio de generación | Snapshot común para PDF/XLSX; reintento idempotente; expediente conserva fallos parciales. |
| Consulta pública | `consulta` | Servicio de proyección pública | DTO allowlist; no consulta contenido documental ni genera URL de descarga. |
| Auditoría | `auditoria` | `AuditService` | Éxitos en transacción de negocio; denegaciones en transacción independiente. |

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
- No se usa borrado físico como recuperación. Las compensaciones inactivan capacidades o crean
  eventos/versiones posteriores sin perder historia.

### Persistencia y cambios SQL

**Objetos existentes**: las 13 tablas y 10 secuencias del baseline permanecen. Se amplían
`PROYECTO`, `PROYECTO_UNIDAD_ORGANICA`, `USUARIO_ROL_UNIDAD`, `SECUENCIA_CODIGO`,
`TRANSICION_ESTADO`, `TIPO_DOCUMENTO`, `DOCUMENTO`, `AUDITORIA_ACCESO` y `AUDITORIA_EVENTO`.
`TRANSICION_PERMITIDA` queda como legado inactivo y no es autoridad. La descripción semilla de
`UnidadAdmin` se corrige porque no existe herencia a descendientes.

**Objetos nuevos**: se detallan en [data-model.md](./data-model.md), incluidos catálogos PEI/POI,
relación iniciativa-proyecto, titular y participantes, evaluación, ciclos, incorporación,
clasificación, reportes, prototipos e idempotencia.

| Orden | Script incremental futuro | Dependencias | Compensación segura |
|---:|---|---|---|
| 002 | `database/ddl/auditoria/002_auditoria_idempotencia.sql` | 001 | Dejar de escribir campos nuevos; nunca eliminar auditoría o claves ya consumidas. |
| 003 | `database/ddl/seguridad/003_usuario_rol_unidad_vigencia.sql` | 002 | Detener nuevas asignaciones/suplencias y conservar historial. |
| 004 | `database/ddl/portafolio/004_catalogos_planeamiento.sql` | 001 | Inactivar valores no referenciados; no borrar históricos. |
| 005 | `database/ddl/portafolio/005_proyecto_campos_oficiales.sql` | 004 | Mantener columnas legacy; no restaurar checks si hay estados nuevos. |
| 006 | `database/ddl/portafolio/006_iniciativa_proyecto_relacion.sql` | 005 | Detener nuevas relaciones; conservar vínculos confirmados. |
| 007 | `database/ddl/portafolio/007_proyecto_unidades_responsables.sql` | 005 | Mantener referencia legacy hasta corte confirmado. |
| 008 | `database/ddl/portafolio/008_proyecto_participantes.sql` | 005, 007 | Deshabilitar altas; conservar periodos e historial. |
| 009 | `database/ddl/portafolio/009_clasificacion_campos.sql` | 002, 005 | Volver datos nuevos no publicables; nunca ampliar acceso. |
| 010 | `database/ddl/documentos/010_documento_version_clasificacion.sql` | 002, 005 | Detener carga/publicación; conservar series y versiones. |
| 011 | `database/ddl/portafolio/011_evaluacion_transiciones.sql` | 003, 005, 010 | Detener nuevos comandos; no revertir estados confirmados. |
| 012 | `database/ddl/portafolio/012_ciclos_resultados_cierre.sql` | 005, 010, 011 | Detener cierres/ciclos; conservar versiones cerradas. |
| 013 | `database/ddl/portafolio/013_incorporacion_individual.sql` | 005, 006, 008, 010 | Mantener expedientes `PENDIENTE`; no borrar evidencia. |
| 014 | `database/ddl/reportes/014_reporte_expediente_remision.sql` | 002, 005, 010, 012 | Detener generación/remisión; conservar expedientes. |
| 015 | `database/ddl/portafolio/015_prototipos_mediciones_metas.sql` | 002, 003, 010 | Detener aprobaciones; conservar versiones y hallazgos. |
| 016 | `database/seeds/016_catalogos_canonicos_portafolio.sql` | 003-015 | Inactivar semillas no referenciadas; nunca borrar referencias. |
| 017 | `database/indexes/017_indices_operacion_portafolio.sql` | 003-015 | Retirar solo índices no esenciales tras revisión. |
| 018 | `database/ddl/portafolio/018_constraints_corte_portafolio.sql` | 003-017 | Deshabilitar solo constraint incompatible; conservar datos. |

Cada script debe prevalidar datos antes del primer DDL, documentar commits implícitos Oracle,
dependencias y compensación, y registrarse `PENDIENTE` en `database/CHANGELOG.md`. Este plan no crea
ni ejecuta esos scripts y no actualiza `database/database-schema.md`.

### Contratos API, Keycloak, privacidad y auditoría

- Los contratos de capacidades están en [contracts/](./contracts/README.md); no exponen entidades
  JPA ni nombres de tablas.
- Las peticiones institucionales presentan el identificador de una asignación efectiva; el backend
  comprueba que pertenece al `sub` autenticado y nunca acepta perfil o unidad del body como autoridad.
- Keycloak Admin API solo se usa para aprovisionar, activar, desactivar y reactivar identidades.
  Ningún DTO contiene contraseña.
- Los DTO públicos son distintos de los institucionales. Ausencia de clasificación nunca significa
  `PUBLICO`.
- Toda operación sensible y denegación registra actor, asignación, perfil, unidad, instante,
  operación, resultado, correlación y cambios mínimos necesarios, sin tokens ni contenido de archivo.

## Estrategia frontend y gate de prototipos

Cada feature Angular será lazy y standalone. `core` concentra OIDC, contexto de asignación,
interceptores y shell; `shared` solo presentación reutilizable. Guards y acciones ocultas mejoran UX,
pero no autorizan ni deciden transiciones.

Antes de implementar cada recorrido de registro, evaluación, decisión, seguimiento, producto final,
cierre, consulta institucional o consulta pública se exige:

1. Prototipo PIIP en `APROBADO`, con código y versión.
2. Validación por cada perfil involucrado y actor sectorial cuando aplique.
3. Aprobador distinto del autor y del único validador.
4. Evidencia de escritorio, móvil, teclado y lector de pantalla aplicable.
5. Medición inicial y matriz de metas aprobadas.
6. Cero errores críticos y cero hallazgos críticos o altos de accesibilidad.

No existe esa evidencia en el proyecto; por tanto, el plan define estructura y contratos, pero el
BUILD de interfaces está bloqueado.

## Trazabilidad de calidad y pruebas

| Riesgo/capacidad | Pruebas obligatorias |
|---|---|
| Límites modulares | ArchUnit: controlador sin repositorio/entidad; servicio sin entidad en contrato; módulo sin repositorio ajeno; ausencia de paquetes prohibidos. |
| Autorización perfil-ámbito | JUnit/MockMvc y Oracle Testcontainers con asignación válida, futura, vencida, revocada, suplida, unidad distinta y no herencia a descendientes. |
| Iniciativa y proyecto | Código concurrente, registros independientes, vínculo inmutable, UK de un derivado, proyecto directo formal y trazabilidad. |
| Máquina de estados | Todas las transiciones listadas, rechazo de inexistentes y terminales, rol decisor/registrador y primera confirmación concurrente. |
| 23 campos | Matriz por tipo/etapa, límites, `TRIM`, condicionales `OTROS`/digital, cardinalidades y edición de subsanación/ejecución. |
| Seguimiento | Ciclo quincenal completo, periodo aplicable, evidencia, cierre inmutable y corrección versionada. |
| Documentos | 100 MB menos un byte, exactamente 100 MB y 100 MB más un byte; MIME, SHA-256, antimalware, clasificación, versión e inmutabilidad. |
| Privacidad | DTO público allowlist, participante restringido, reclasificación inmediata y ausencia de endpoint/enlace de descarga pública. |
| Usuarios | Aprovisionamiento idempotente, activación, desactivación bilateral, reactivación, duplicado, compensación y fallo parcial Keycloak/Oracle. |
| Asignaciones | Administración por perfil, vigencia, revocación inmediata, suplencia no solapada, titular inactivo y protección del último `GlobalAdmin`. |
| Incorporación | `PENDIENTE/VALIDADO/RECHAZADO`, correcciones ilimitadas, conflicto de código/relación, duplicado vinculado y auditoría original. |
| Reportes | Cortes 30/06 y 31/12, BR-122 y denominador cero, filtros, snapshot PDF/XLSX, clasificación, aprobación, remisión y recuperación parcial. |
| Prototipos | Estados, separación autor/validador/aprobador, medición, metas, versionado y bloqueo por hallazgo. |
| Cierre | Ambos caminos `PRODUCTO_APROBADO` y `PRODUCTO_NO_APROBADO` a `FINALIZADO`, requisitos completos y fecha automática. |
| Frontend | Vitest de formularios/errores/asignación; Playwright de ocho recorridos en escritorio/móvil, teclado, lector de pantalla aplicable y consulta pública. |

## Fases locales de implementación posteriores

1. Resolver y aprobar los tres bloqueos funcionales y corregir el estado documental de la spec.
2. Aprobar modelo físico y depositar scripts 002-018 como `PENDIENTE`; ejecución siempre manual.
3. Construir fundaciones de auditoría, organización y seguridad, incluido Keycloak idempotente.
4. Construir portafolio, documentos y máquina de estados mediante JPA.
5. Construir seguimiento, incorporación, reportes y consulta.
6. Registrar y aprobar prototipos, mediciones y metas por etapa.
7. Implementar cada interfaz únicamente después de superar su gate.
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
| Prototipos antes de UI | CUMPLE COMO GATE; evidencia aún ausente |
| Decisiones funcionales completas | **NO CUMPLE** por los tres bloqueos declarados |
| Alcance Fase 1 | CUMPLE |

**Resultado**: El diseño preserva la Constitución, pero el gate global no puede declararse aprobado.
No debe comenzar BUILD de catálogos operativos PEI/POI, administración funcional completa,
publicación documental ni interfaces sin resolver sus prerequisitos. Las demás decisiones del plan no
redefinen reglas funcionales.

## Seguimiento de complejidad

No se solicita ni aprueba ninguna excepción constitucional. La estrategia JPA evita una segunda
implementación funcional en PL/SQL y el adaptador local de almacenamiento queda detrás de
`DocumentStorage`.
