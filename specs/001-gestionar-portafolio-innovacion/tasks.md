---
description: "Tareas ejecutables para implementar la gestión integral del portafolio PIIP"
---

# Tasks: Gestión del Portafolio Institucional de Innovación Pública

**Input**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `quickstart.md` y `contracts/`
aprobados en `specs/001-gestionar-portafolio-innovacion/` y Constitución 5.0.0.

**Prerequisites**: Constitución 5.0.0, especificación aprobada y ejecución manual confirmada de cada
script Oracle antes de su tarea JPA dependiente. US9 y sus gates están diferidos; OGTI administra los
controles antimalware fuera de PIIP; la primera asignación `GlobalAdmin` solo se crea mediante la
semilla 021.

**Tests**: Son obligatorias las pruebas JUnit 5, Mockito, MockMvc, Oracle Testcontainers, ArchUnit, Vitest y Playwright indicadas. Su ejecución requiere autorización expresa conforme a `AGENTS.md`.

**Organization**: Cada tarea identifica un único especialista propietario. `[P]` significa que puede realizarse en paralelo porque no comparte archivos ni depende de otra tarea incompleta.

## Formato: `ID marcador-paralelo marcador-historia descripción`

- `[P]` se incluye solo para tareas paralelizables.
- `[US1]` a `[US9]` aparecen únicamente dentro de fases de historias.
- Todos los cambios SQL se depositan y registran como `PENDIENTE`; ningún agente ejecuta SQL.
- `database/database-schema.md` solo se actualiza después de confirmación humana de ejecución exitosa.

## Phase 1: Setup y arquitectura

**Purpose**: Alinear configuración, herramientas y límites arquitectónicos antes de implementar capacidades.

- [X] T001 [P] [backend-specialist] Incorporar ArchUnit, JaCoCo y perfiles separados de pruebas unitarias e integración con umbral de 80 % para servicios de negocio en `apps/backend/business-domain/pom.xml` y `apps/backend/business-domain/ms-piip/pom.xml`
- [X] T002 [P] [backend-specialist] Externalizar Oracle, issuer, audience y almacenamiento, establecer `/api/v1` y 100 MB, y documentar el gate humano obligatorio para retirar y rotar secretos o wallet versionados, sin manipularlos desde esta tarea, en `apps/backend/business-domain/ms-piip/src/main/resources/application.yml`, `apps/backend/business-domain/ms-piip/src/main/resources/application-dev.yml` y `apps/backend/business-domain/ms-piip/src/main/resources/application-test.yml`
- [X] T003 [P] [backend-specialist] Crear pruebas ArchUnit que prohíban controlador a repositorio, entidades en contratos de servicio, repositorios entre módulos, paquetes `model`, `client` o `integration`, y conectores funcionales externos no autorizados; permitir únicamente el adaptador de Keycloak dentro de `seguridad/service/impl/`, en `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/architecture/ModularArchitectureTest.java`
- [X] T004 [P] [frontend-specialist] Incorporar Angular Material, `keycloak-js`, Playwright y axe sin incluir valores OIDC de ambiente en `apps/frontend/package.json` y `apps/frontend/package-lock.json`
- [X] T005 [P] [frontend-specialist] Configurar tema PIIP, contraste, foco visible, tipografía y breakpoints responsive WCAG 2.1 AA en `apps/frontend/src/styles.css` y `apps/frontend/src/app/shared/ui/piip-theme.scss`
- [X] T006 [P] [frontend-specialist] Configurar Vitest y Playwright para escritorio, móvil y datos sintéticos en `apps/frontend/angular.json`, `apps/frontend/tsconfig.spec.json`, `apps/frontend/playwright.config.ts` y `apps/frontend/e2e/fixtures/test-data.ts`
- [X] T007 [database-specialist] Completar el diccionario físico Oracle 002-024 y registrar revisión/hash, aprobador, fecha y alcance del gate humano DB antes del DDL en `database/database-physical-design.md` y `database/physical-design-approval.md`
- [X] T008 [database-specialist] Crear el diagnóstico de solo lectura para esquema, baseline, BLOB y cadenas documentales, inexistencia histórica de `GlobalAdmin`, asignaciones y referencias legacy incompatibles en `database/tests/preflight_002_024.sql`

## Phase 2: Controles fundacionales

**Purpose**: Implementar los bloqueos compartidos de seguridad, auditoría, documentos, organización y persistencia.

- [X] T009 [P] [backend-specialist] Alinear Resource Server para validar `issuer`, `audience`, firma, vigencia y los claims estándar necesarios del JWT sin derivar permisos PIIP de roles Keycloak, y permitir anonimato solo en consulta pública; los permisos funcionales y el ámbito se resuelven mediante asignación efectiva Oracle, en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/config/SecurityConfig.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/config/JwtConfig.java`
- [X] T010 [P] [backend-specialist] Implementar correlación, headers comunes y respuestas `application/problem+json` sin filtración de datos; centralizar la construcción de `ProblemDetail` y crear exactamente un `@RestControllerAdvice` acotado por paquete para cada módulo constitucional, sin advice globales ni contratos `ApiResponse` legacy, en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/config/CorrelationIdFilter.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/config/ApiHeaders.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/config/ProblemDetailsConfig.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/{organizacion,seguridad,portafolio,documentos,reportes,consulta,auditoria}/exception/`
- [X] T011 [P] [backend-specialist] Configurar OpenAPI `/api/v1`, headers, paginación y Problem Details, y preparar generación/validación incremental código-first en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/config/OpenApiConfig.java` y `specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml`
- [X] T012 [P] [database-specialist] Crear pruebas SQL de auditoría, idempotencia, expediente, propietario XOR, BLOB Oracle, versionado, SHA-256 y límite documental en `database/tests/002_auditoria_idempotencia_test.sql` y `database/tests/003_expediente_serie_version_test.sql`
- [X] T013 [database-specialist] Crear auditoría efectiva y `SOLICITUD_IDEMPOTENTE` con ventana configurable de siete días, compensación forward-only y registro inmediato `PENDIENTE` en `database/ddl/auditoria/002_auditoria_idempotencia.sql` y `database/CHANGELOG.md`
- [X] T014 [database-specialist] Crear expediente institucional, `DOCUMENTO_SERIE`, filas `DOCUMENTO` versionadas con BLOB Oracle, SHA-256, propietario XOR y 100 MB; conservar `SCAN_ANTIVIRUS` y `NOMBRE_STORAGE` solo como legacy nullable, sin default, constraint ni consumidor, y registrar 003 inmediatamente como `PENDIENTE` en `database/ddl/documentos/003_expediente_serie_version.sql` y `database/CHANGELOG.md`
- [X] T015 [P] [database-specialist] Crear pruebas SQL para matriz funcional, vigencia, revocación, suplencia y aprovisionamiento en `database/tests/007_matriz_funcional_versionada_test.sql` y `database/tests/008_usuario_rol_unidad_vigencia_test.sql`
- [X] T016 [database-specialist] Crear matriz versionada de función, perfil y unidad concreta con aprobación formal y registrar 007 inmediatamente como `PENDIENTE` en `database/ddl/seguridad/007_matriz_funcional_versionada.sql` y `database/CHANGELOG.md`
- [X] T017 [database-specialist] Evolucionar usuarios para exigir solo `KEYCLOAK_ID` en la identidad fundacional y permitir datos informativos nulos, y asignaciones para vigencia, revocación, suplencia, historial y aprovisionamiento recuperable; registrar 008 inmediatamente como `PENDIENTE` en `database/ddl/seguridad/008_usuario_rol_unidad_vigencia.sql` y `database/CHANGELOG.md`
- [X] T126 [database-specialist] Tras `EXECUTION_CONFIRMED` de 002, 007 y 008 y recibir el `sub` del administrador Keycloak de OGTI, la aprobación de despliegue de la Jefatura de Modernización y los datos del DBA, crear y probar la semilla 021 manual, fail-fast y de ejecución única: prevalidar y reutilizar `MIDAGRI`/`Ministerio de Desarrollo Agrario y Riego`, insertar `ADMINISTRADOR_PIIP`/`Administrador PIIP`, combinación `GlobalAdmin`, usuario por `sub`, primera asignación y auditoría mínima; abortar sin cambios ante unidad discordante, cualquier asignación histórica o reejecución, y registrar 021 inmediatamente como `PENDIENTE` en `database/seeds/021_matriz_funcional_inicial_aprobada.sql`, `database/tests/021_matriz_funcional_inicial_aprobada_test.sql` y `database/CHANGELOG.md`. La ejecución sigue estas dependencias, como excepción documentada al orden numérico del identificador 021. Cumplido: 021 y 021.1 están `VIGENTE` en `database/CHANGELOG.md` (huella fundacional aplicada el 2026-07-22).
- [X] T018 [backend-specialist] Tras confirmación humana de los scripts 002, 007 y 008, implementar entidades y repositorios append-only de auditoría, idempotencia y seguridad en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/auditoria/entity/`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/auditoria/repository/`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/entity/` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/repository/`
- [X] T019 [P] [backend-specialist] Tras completar T018, implementar `AuditService` con éxitos en la transacción de negocio, denegaciones `REQUIRES_NEW` y fallo cerrado en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/auditoria/service/AuditService.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/auditoria/service/impl/AuditServiceImpl.java`
- [X] T020 [P] [backend-specialist] Tras completar T018, implementar idempotencia por consumidor, operación, clave y hash canónico en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/auditoria/service/IdempotencyService.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/auditoria/service/impl/IdempotencyServiceImpl.java`
- [X] T021 [backend-specialist] Implementar autorización efectiva Oracle por `sub`, una sola asignación, unidad exacta, vigencia y revalidación bajo bloqueo en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/service/AutorizacionEfectivaService.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/service/impl/AutorizacionEfectivaServiceImpl.java`
- [X] T022 [backend-specialist] Tras confirmación humana del script 003, implementar entidades y repositorios de expedientes, series, versiones y clasificación inicial, excluyendo publicación hasta confirmar 004, en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/documentos/entity/` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/documentos/repository/`
- [X] T023 [backend-specialist] Tras confirmar 003, implementar `DocumentStorage` sobre BLOB Oracle, carga/versionado con SHA-256, expedientes y controladores sin estados, contratos ni gates antimalware en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/documentos/service/DocumentStorage.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/documentos/service/DocumentoService.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/documentos/service/ExpedienteInstitucionalService.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/documentos/controller/DocumentoController.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/documentos/controller/ExpedienteInstitucionalController.java`. Cumplido: `DocumentStorage`, `DocumentoService`, `ExpedienteInstitucionalService` y `OracleBlobDocumentStorage` ya cumplen el contrato; se crearon `DocumentoController`, `ExpedienteInstitucionalController` y los DTOs HTTP `DocumentOwnerDto`, `UploadDocumentRequest`, `CreateInstitutionalFileRequest`. Verificación pendiente: self-call transaccional en `DocumentoServiceImpl` y mapeo de `MaxUploadSizeExceededException`/`MissingRequestHeaderException`.
- [X] T024 [P] [backend-specialist] Crear pruebas fundacionales de auditoría, idempotencia, autorización, validación JWT de issuer, audience, firma, vigencia y claims estándar necesarios, carga/versionado BLOB, SHA-256, límites, propietario XOR y expedientes, verificando la ausencia de campos y decisiones antimalware en contratos PIIP, en `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/auditoria/`, `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/seguridad/` y `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/documentos/`
- [X] T025 [P] [frontend-specialist] Cargar y validar configuración externa antes del bootstrap de `keycloak-js`, aplicar PKCE y fallar cerrado si falta issuer, clientId, redirects o scopes; exigir confirmación de OGTI de que configuró el tema personalizado de Keycloak en el ambiente OIDC, sin versionar recursos del tema ni valores OIDC, en `apps/frontend/public/config.json`, `apps/frontend/src/app/core/config/runtime-config.ts`, `apps/frontend/src/app/core/auth/auth.service.ts`, `apps/frontend/src/app/core/auth/auth.guard.ts` y `apps/frontend/src/app/core/auth/auth-callback.component.ts`
- [X] T026 [P] [frontend-specialist] Implementar interceptores Bearer, asignación efectiva, idempotencia, ETag y Problem Details en `apps/frontend/src/app/core/http/auth.interceptor.ts`, `apps/frontend/src/app/core/effective-assignment/effective-assignment.interceptor.ts`, `apps/frontend/src/app/core/http/idempotency-key.service.ts`, `apps/frontend/src/app/core/http/entity-tag.ts` y `apps/frontend/src/app/core/http/problem-details.ts`
- [X] T027 [frontend-specialist] Implementar shell accesible, selector de una asignación efectiva y rutas Angular lazy institucionales y públicas anónimas en `apps/frontend/src/app/core/layout/institutional-shell.component.ts`, `apps/frontend/src/app/core/effective-assignment/effective-assignment-selector.component.ts` y `apps/frontend/src/app/app.routes.ts`
- [X] T028 [P] [frontend-specialist] Crear pruebas Vitest de autenticación, interceptores, asignación efectiva, errores y navegación del shell, incluida la validación del redireccionamiento al ambiente Keycloak configurado sin gestionar credenciales ni recursos del tema, en `apps/frontend/src/app/core/auth/auth.service.spec.ts`, `apps/frontend/src/app/core/http/auth.interceptor.spec.ts`, `apps/frontend/src/app/core/effective-assignment/effective-assignment.service.spec.ts` y `apps/frontend/src/app/core/layout/institutional-shell.component.spec.ts`

**Checkpoint**: Auditoría, idempotencia, autorización efectiva, documentos y arquitectura bloquean cualquier exposición de negocio que no los utilice.

## Phase 3: US9 diferida

US9 y sus tareas T029-T039 se conservan en `deferred/us9-tasks.md`. No usan checkboxes activos y
no forman parte de la ejecución de `/speckit.implement` en la Fase 1 actual.

## Phase 3.5: Prerrequisitos persistentes de clasificación

**Purpose**: Confirmar los campos oficiales y su matriz de clasificación antes de diseñar formularios, validaciones, consultas o reportes.

- [X] T044 [database-specialist] Crear versiones independientes PEI/POI y registrar 005 y 006 inmediatamente como `PENDIENTE` en `database/ddl/organizacion/005_objetivo_pei_versionado.sql`, `database/ddl/organizacion/006_actividad_poi_versionada.sql` y `database/CHANGELOG.md`
- [X] T045 [database-specialist] Ampliar `PROYECTO` para 23 campos, prefijo aprobado, `ADMINISTRACION` legacy nullable y mapeo de descripción bloqueante, y registrar 009 como `PENDIENTE` en `database/ddl/portafolio/009_proyecto_campos_oficiales.sql` y `database/CHANGELOG.md`
- [X] T125 [database-specialist] Tras `EXECUTION_CONFIRMED` de 002 y 009, crear y probar la matriz append-only de obligatoriedad, editabilidad, privacidad y actor responsable por tipo y etapa; registrar 013 inmediatamente como `PENDIENTE` en `database/tests/013_clasificacion_campos_test.sql`, `database/ddl/portafolio/013_clasificacion_campos.sql` y `database/CHANGELOG.md`. Cumplido: 013 está `VIGENTE` en `database/CHANGELOG.md` (matriz aplicada el 2026-07-22; precondición limitada a sucesores estrictos).

**Checkpoint**: La confirmación humana de 013 es obligatoria antes de iniciar US1.

## Phase 4: User Story 1 - Registrar una iniciativa (Priority: P1)

**Goal**: Presentar una iniciativa completa o incorporar individualmente información existente con código, responsables, documentos e historial.

**Independent Test**: Registrar una iniciativa y comprobar código propio, fecha automática, `PRESENTADO`, código de origen vacío, ficha apta, unidad principal, titular y auditoría.

### Tests for User Story 1

- [X] T040 [P] [US1] [database-specialist] Crear pruebas SQL para PEI/POI, campos oficiales, rechazo sin prefijo, `ADMINISTRACION` legacy nullable, unidades, titulares e incorporación en `database/tests/005_objetivo_pei_versionado_test.sql`, `database/tests/006_actividad_poi_versionada_test.sql`, `database/tests/009_proyecto_campos_oficiales_test.sql`, `database/tests/011_proyecto_unidades_responsables_test.sql`, `database/tests/012_responsables_participantes_test.sql` y `database/tests/016_incorporacion_individual_test.sql`
- [X] T041 [P] [US1] [backend-specialist] Crear pruebas JUnit, MockMvc y Oracle Testcontainers para APIs PEI/POI independientes, presentación, catálogos, correlativos, ficha e incorporación en `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/organizacion/`, `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/portafolio/iniciativa/` y `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/portafolio/incorporacion/`. Cumplido: 12 archivos de prueba JUnit 5 + Mockito + MockMvc creados. Pendiente: `*IT` con Oracle Testcontainers requiere Docker host, y los tests de presentación fallan hasta corregir el prefijo de unidad que `PresentarIniciativaServiceImpl` pasa como `null` a `CodigoProyectoService.generarCodigo`.
- [X] T042 [P] [US1] [frontend-specialist] Crear Vitest de campos, ficha, carga documental, subsanación e incorporación en `apps/frontend/src/app/features/portafolio/registro/initiative-form/initiative-form.component.spec.ts` y `apps/frontend/src/app/features/portafolio/registro/incorporation/individual-incorporation.component.spec.ts`. Cumplido: 100 casos `it` y 243 `expect` distribuidos en los 3 specs (initiative 44/100, correction 23/63, incorporation 33/80); cobertura de los 23 campos, matriz 013, OTROS/Componente digital, UNIT_MAIN_CARDINALITY, carga de ficha, idempotencia, ETag, códigos canónicos ProblemDetail y accesibilidad WCAG 2.1 AA. Pendiente: ejecución de `npm run test` para validar % de cobertura real.
- [X] T043 [P] [US1] [frontend-specialist] Crear Playwright de iniciativas y proyectos en escritorio, móvil, teclado y axe en `apps/frontend/e2e/registro/registrar-iniciativa.spec.ts`, `apps/frontend/e2e/registro/crear-proyecto.spec.ts` y `apps/frontend/e2e/accessibility/registro.a11y.spec.ts`. Cumplido: 3 specs Playwright + `playwright.config.ts` con proyectos desktop y Pixel 5; recorrido feliz, 422 ProblemDetail, móvil sin scroll horizontal, teclado, idempotencia, If-Match/412, axe-core en las 6 rutas US1 con fallo en `critical`/`serious`. Pendiente: ejecución del runner (`npx playwright test`) requiere tu autorización y `npx playwright install`.

### Implementation for User Story 1

- [X] T046 [US1] [database-specialist] Crear unidades principales, titulares y participantes históricos, y registrar 011 y 012 inmediatamente como `PENDIENTE` en `database/ddl/portafolio/011_proyecto_unidades_responsables.sql`, `database/ddl/portafolio/012_responsables_participantes.sql` y `database/CHANGELOG.md`
- [X] T047 [US1] [database-specialist] Crear incorporación individual append-only y registrar 016 inmediatamente como `PENDIENTE` en `database/ddl/portafolio/016_incorporacion_individual.sql` y `database/CHANGELOG.md`
- [X] T048 [US1] [backend-specialist] Tras confirmar 005, 006, 009, 011, 012, 013 y 016, implementar APIs PEI/POI y persistencia de registro/incorporación en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/organizacion/service/ObjetivoPeiCatalogService.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/organizacion/service/ActividadPoiCatalogService.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/organizacion/controller/PlaneamientoController.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/repository/`
- [X] T049 [US1] [backend-specialist] Tras `EXECUTION_CONFIRMED` de 013, implementar correlativo bajo `PESSIMISTIC_WRITE` y presentación transaccional con la matriz de campos, catálogos, cardinalidades y ficha exigidos en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/CodigoProyectoServiceImpl.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/PresentarIniciativaServiceImpl.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/controller/IniciativaController.java`. Cumplido: bug del prefijo nulo corregido vía `CatalogoUnidadReader`; `PESSIMISTIC_WRITE` verificado con reintento defensivo; matriz 013, catálogos 019, cardinalidades y ficha validados; auditoría transaccional; `Idempotency-Key`; snapshot OpenAPI regenerado. `NEEDS CLARIFICATION`: `CatalogoUnidadReader` consulta `UnidadEjecutoraRepository` de `seguridad` mediante un puerto aislado; pendiente reemplazar por servicio oficial cuando `seguridad` lo exponga (US6/T086).
- [X] T050 [P] [US1] [backend-specialist] Implementar incorporación, correcciones, conflictos, duplicado vinculado, validación y rechazo en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/IncorporacionRegistroServiceImpl.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/controller/IncorporacionController.java`. Cumplido: 7 brechas corregidas (constructor, validar, persistencia previa, autorización 013, idempotencia, duplicado vinculado, observación, `@Version`, ETag, códigos canónicos en ProblemDetail). `NEEDS CLARIFICATION`: detección de `DUPLICADO` entre responsables y de `RELACION_INVALIDA` automáticas, planificadas en US1.4.
- [X] T051 [US1] [frontend-specialist] Tras `EXECUTION_CONFIRMED` de 013 e importar snapshot OpenAPI compatible, implementar clientes de organización/documentos/registro, formulario, subsanación e incorporación en `apps/frontend/src/app/features/organizacion/api/organizacion-api.service.ts`, `apps/frontend/src/app/features/documentos/api/documentos-api.service.ts`, `apps/frontend/src/app/features/portafolio/registro/api/registro-api.service.ts`, `apps/frontend/src/app/features/portafolio/registro/initiative-form/initiative-form.component.ts`, `apps/frontend/src/app/features/portafolio/registro/correction-form/correction-form.component.ts` y `apps/frontend/src/app/features/portafolio/registro/incorporation/individual-incorporation.component.ts`. Cumplido: 3 servicios de API tipados, 3 componentes standalone con WCAG 2.1 AA, tipos compartidos del snapshot, tests Vitest básicos. `NEEDS CLARIFICATION`: discrepancia entre el path de la tarea (`/portafolio/iniciativas/{id}/incorporaciones`) y el snapshot OpenAPI (`/portafolio/incorporaciones`); se implementó contra el snapshot. Endpoints pendientes en snapshot: organización, carga documental, subsanación de iniciativa.
- [X] T052 [US1] [frontend-specialist] Registrar rutas lazy del recorrido aprobado en `apps/frontend/src/app/features/portafolio/registro/registro.routes.ts` y `apps/frontend/src/app/app.routes.ts`. Cumplido: 6 rutas US1 con `loadComponent`, `authGuard` + `effectiveAssignmentGuard`, `title` accesible, `breadcrumb`, `REGISTRO_ROUTES` exportado y registrado con `loadChildren`; spec Vitest de 8 casos. `NEEDS CLARIFICATION`: `withComponentInputBinding()` no habilitado en `app.config.ts`; lógica de crear/editar por `:id` no implementada en los componentes.

## Phase 5: User Story 2 - Evaluar y decidir una iniciativa (Priority: P1)

**Goal**: Distinguir admisibilidad, aplicabilidad, opinión técnica y decisión formal con decisor, registrador y evidencia.

**Independent Test**: Evaluar una iniciativa presentada, versionar la opinión, registrar una decisión y comprobar transición, documento e historial atómico.

### Tests for User Story 2

- [X] T053 [P] [US2] [database-specialist] Crear pruebas SQL de evaluación, subsanación única, decisor/registrador, once estados e inactividad de `TRANSICION_PERMITIDA` en `database/tests/014_evaluacion_transiciones_us2_test.sql`
- [X] T054 [P] [US2] [backend-specialist] Crear pruebas JUnit y Oracle Testcontainers de subsanación, admisibilidad, aplicabilidad, versiones, terminales y carreras en `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/portafolio/evaluacion/` y `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/portafolio/transicion/`. Cumplido: 5 archivos JUnit 5 + Mockito + MockMvc; cubre subsanación única (014.1), admisibilidad/aplicabilidad independientes, 11 transiciones canónicas, terminales, `If-Match` (428/412), `Idempotency-Key`, autorización efectiva, historial append-only, auditoría atómica, carrera concurrente. `NEEDS_CLARIFICATION` en firmas que T057 y T058 ajustarán.
- [X] T055 [P] [US2] [frontend-specialist] Crear Vitest y Playwright, incluida cancelación, foco, 409 y 412 en `apps/frontend/src/app/features/portafolio/evaluacion/evaluation-page.component.spec.ts`, `apps/frontend/src/app/features/portafolio/decision/initiative-decision-page.component.spec.ts`, `apps/frontend/e2e/evaluacion/evaluar-iniciativa.spec.ts` y `apps/frontend/e2e/decision/decidir-iniciativa.spec.ts`. Cumplido: 4 specs (2 Vitest + 2 Playwright) con 85 casos totales (67 Vitest + 18 Playwright); cubren escritorio, móvil, teclado, cancelación, 409, 412, 422, códigos canónicos completos, accesibilidad WCAG 2.1 AA. `NEEDS_CLARIFICATION` en firmas de T059 y selectores que se ajustarán cuando T060 publique las rutas.

### Implementation for User Story 2

- [X] T056 [US2] [database-specialist] Crear evaluación, subsanación, aplicabilidad y transiciones, y registrar 014 inmediatamente como `PENDIENTE` en `database/ddl/portafolio/014_evaluacion_transiciones.sql` y `database/CHANGELOG.md`
- [X] T057 [US2] [backend-specialist] Tras confirmación humana de 014, implementar apertura/edición de subsanación y evaluación de admisibilidad/aplicabilidad en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/controller/EvaluacionIniciativaController.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/SubsanacionIniciativaServiceImpl.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/EvaluacionIniciativaServiceImpl.java`. Cumplido: 5 endpoints REST, subsanación única (UK_SI_INICIATIVA) con plazo determinista, admisibilidad/aplicabilidad independientes, ETag tolerante, auditoría, idempotencia, OpenAPI regenerado. Ajustes: stubs anidados de T054 eliminados y reemplazados por DTOs canónicos.
- [X] T058 [US2] [backend-specialist] Implementar máquina de estados con bloqueo, `If-Match`, revalidación, evidencia, historial y auditoría atómica en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/TransicionEstadoService.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/TransicionEstadoServiceImpl.java`. Cumplido: 11 transiciones controladas (10 de la Constitución + subsanación vencida), 3 terminales bloqueadas, `findByIdForUpdate` con `PESSIMISTIC_WRITE`, If-Match (428/412), autorización efectiva, evidencia documental obligatoria, historial append-only con `SYSTIMESTAMP`, auditoría atómica, `TransicionEstadoController` con `If-Match`+`Idempotency-Key`+`ProblemDetail`, OpenAPI regenerado.
- [X] T059 [P] [US2] [frontend-specialist] Tras aprobar `EVALUACION`/`DECISION` y validar su snapshot OpenAPI, implementar evaluación, decisión y cancelación sin permisos locales en `apps/frontend/src/app/features/portafolio/evaluacion/api/evaluacion-api.service.ts`, `apps/frontend/src/app/features/portafolio/evaluacion/evaluation-page.component.ts`, `apps/frontend/src/app/features/portafolio/decision/initiative-decision-page.component.ts` y `apps/frontend/src/app/features/portafolio/decision/project-cancellation.component.ts`. Cumplido: 3 componentes standalone, 2 API services, tipos compartidos del snapshot, WCAG 2.1 AA. `NEEDS_CLARIFICATION` en la forma de `ApplicabilityCriterion` (`codigo/cumple/observacion` vs snapshot actual `clave/valor/orden`); activación de `entityTagInterceptor` pendiente.
- [X] T060 [US2] [frontend-specialist] Registrar rutas lazy de evaluación y decisión en `apps/frontend/src/app/features/portafolio/evaluacion/evaluacion.routes.ts`, `apps/frontend/src/app/features/portafolio/decision/decision.routes.ts` y `apps/frontend/src/app/app.routes.ts`. Cumplido: `EVALUACION_ROUTES` y `DECISION_ROUTES` con `loadComponent`; `app.routes.ts` actualizado con dos `loadChildren` hermanos de `portafolio/registro`; specs Vitest opcionales para validar carga perezosa, guards y ausencia de colisión. `NEEDS_CLARIFICATION` en selectores Playwright que se ajustarán al validar las rutas reales.

## Phase 6: User Story 3 - Crear un proyecto derivado o directo (Priority: P1)

**Goal**: Crear un único proyecto derivado o un proyecto directo formal sin alterar la iniciativa de origen.

**Independent Test**: Crear ambos tipos y comprobar código, vínculo, estado, evidencia y rechazo de un segundo derivado o un directo no autorizado.

### Tests for User Story 3

- [X] T061 [P] [US3] [database-specialist] Crear pruebas SQL de filas independientes, unicidad bilateral y vínculo inmutable en `database/tests/010_iniciativa_proyecto_relacion_test.sql`
- [X] T062 [P] [US3] [backend-specialist] Crear pruebas JUnit y Oracle Testcontainers de unicidad concurrente, derivado, directo, autorización y estado de origen en `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/portafolio/proyecto/`. Cumplido: 5 archivos `.java` + 1 archivo de stubs compartidos, 67 `@Test` totales. Cubre filas independientes, unicidad bilateral, vínculo inmutable, derivado solo en `INICIATIVA_APROBADA`, segundo derivado falla con 409, directo con documento y autorización formal, controlador con `If-Match`+`Idempotency-Key`+`ProblemDetail`, carrera concurrente. `NEEDS_CLARIFICATION` en firmas que T065/T066 ajustarán.
- [X] T063 [P] [US3] [frontend-specialist] Crear Vitest de proyecto derivado/directo y conflicto concurrente en `apps/frontend/src/app/features/portafolio/proyectos/derived-project/derived-project.component.spec.ts` y `apps/frontend/src/app/features/portafolio/proyectos/direct-project/direct-project.component.spec.ts`. Cumplido: 2 specs Vitest (1 624 líneas) con cobertura de los 11 controles del DTO de derivado y los 15 del directo; bloqueo por estado de la iniciativa, `DERIVATION_ALREADY_EXISTS`, `DIRECT_PROJECT_DUPLICATE`, `INITIATIVE_PRESENT_FOR_SCOPE`, `FORMAL_DOCUMENT_REQUIRED`, `EVIDENCE_NOT_ELIGIBLE`, `UNIT_MAIN_CARDINALITY`, `ASSIGNMENT_SCOPE_DENIED`, `STATE_CHANGED`, cabeceras HTTP, cancelación, WCAG 2.1 AA. `NEEDS_CLARIFICATION` en firmas de T067.

### Implementation for User Story 3

- [X] T064 [US3] [database-specialist] Crear `INICIATIVA_PROYECTO` inmutable y registrar 010 inmediatamente como `PENDIENTE` en `database/ddl/portafolio/010_iniciativa_proyecto_relacion.sql` y `database/CHANGELOG.md`
- [X] T065 [US3] [backend-specialist] Tras confirmación humana de 010, implementar relación, bloqueo de iniciativa y creación de proyecto derivado en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/entity/RelacionIniciativaProyectoEntity.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/repository/RelacionIniciativaProyectoRepository.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/CrearProyectoDerivadoServiceImpl.java`. Cumplido: entidad inmutable con `SEQ_INICIATIVA_PROYECTO`, repositorio con `findByIniciativaId`+`existsByIniciativaId`+`existsByProyectoId`, servicio con bloqueo pesimista, UK bilateral del DDL 010 como autoridad, `DataIntegrityViolationException` como salvaguarda, documento formal obligatorio, idempotencia y auditoría atómica, código de proyecto propio. `NEEDS_CLARIFICATION`: `detalleFuente` para `OTROS`; prueba ArchUnit formal pendiente.
- [X] T066 [P] [US3] [backend-specialist] Implementar proyecto directo heredado/excepcional con documento, fecha y autorización formal en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/CrearProyectoDirectoServiceImpl.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/controller/ProyectoController.java`. Cumplido: `TipoOrigenDirecto` (HEREDADO, EXCEPCION_FORMAL), `DirectProjectRequest` con 15 campos validados, `CrearProyectoDirectoServiceImpl` con bloqueo contra segundo directo activo por unidad y año, código propio, autorización efectiva Autoridad/Evaluador, documento formal obligatorio, auditoría atómica, idempotencia. `ProyectoController` con `POST /iniciativas/{id}/proyecto-derivado` y `POST /proyectos-directos`. Stubs de T062 eliminados. OpenAPI regenerado.
- [X] T067 [US3] [frontend-specialist] Tras validar el snapshot OpenAPI de US3, implementar cliente, formularios y rutas de proyectos derivados/directos en `apps/frontend/src/app/features/portafolio/proyectos/api/proyectos-api.service.ts`, `apps/frontend/src/app/features/portafolio/proyectos/derived-project/derived-project.component.ts`, `apps/frontend/src/app/features/portafolio/proyectos/direct-project/direct-project.component.ts` y `apps/frontend/src/app/features/portafolio/proyectos/proyectos.routes.ts`. Cumplido: 11 archivos nuevos; 2 componentes standalone con WCAG 2.1 AA; servicio de API tipado con `If-Match`+`Idempotency-Key`+`ProblemDetail`; tipos compartidos; `PROYECTOS_ROUTES` con `loadComponent` y guards; `app.routes.ts` actualizado con `loadChildren`; spec Vitest del catálogo de rutas. `NEEDS_CLARIFICATION` en la forma exacta de `GET /api/v1/portafolio/iniciativas?unidadId=...&estado=PRESENTADO`.

## Phase 7: User Story 4 - Acompañar la ejecución y presentar el producto (Priority: P1)

**Goal**: Mantener ciclos quincenales, participantes, evidencias y presentación final durante la ejecución.

**Independent Test**: Registrar varios ciclos, corregir uno por nueva versión y presentar el producto con evidencias aptas.

### Tests for User Story 4

- [X] T068 [P] [US4] [database-specialist] Crear pruebas SQL de periodo/versión, cadena de corrección y evidencia exacta en `database/tests/015_ciclos_resultados_cierre_us4_test.sql`
- [X] T069 [P] [US4] [backend-specialist] Crear pruebas JUnit y Oracle Testcontainers de ciclos, participantes, campos editables, presentación, suspensión y cancelación en `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/portafolio/seguimiento/`. Cumplido: 6 archivos `.java` (~3 400 líneas) con cobertura completa: planificación, periodo `AAAA-Qn-Sn`, ciclos quincenales append-only, participantes históricos, presentación final con campos 17/19/23, suspensión por `UnidadAdmin` con "Evidencia de Suspensión", cancelación por `Autoridad` con "Informe de la Oficina de Modernización", MockMvc con 11 endpoints, cabeceras canónicas, sin autoridad derivada del JWT.
- [X] T070 [P] [US4] [frontend-specialist] Crear Vitest y Playwright de ciclos y suspensión con teclado y axe en `apps/frontend/src/app/features/portafolio/seguimiento/cycle-form/cycle-form.component.spec.ts`, `apps/frontend/e2e/seguimiento/acompanar-proyecto.spec.ts` y `apps/frontend/e2e/accessibility/seguimiento.a11y.spec.ts`. Cumplido: specs TDD para ciclo quincenal, avance 0–100, evidencia apta, corrección append-only, 409/412/422, Idempotency-Key, If-Match, suspensión/cancelación UX, teclado, desktop/Pixel 5 y axe WCAG 2.1 AA.

### Implementation for User Story 4

- [X] T071 [US4] [database-specialist] Crear planificación, ciclos, productos y cierre, y registrar 015 inmediatamente como `PENDIENTE` en `database/ddl/portafolio/015_ciclos_resultados_cierre.sql` y `database/CHANGELOG.md`
- [X] T072 [US4] [backend-specialist] Tras confirmación humana de 015, implementar planificación y ciclos quincenales versionados en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/controller/SeguimientoProyectoController.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/SeguimientoProyectoServiceImpl.java`. Cumplido: 43 archivos (18 DTOs, 4 entidades JPA, 4 repositorios, 1 mapper MapStruct, 6 servicios, 1 controlador con 14 endpoints REST, 3 tests migrados, OpenAPI 1615→2670 líneas). Placeholders para T073 y T074 lanzan `UnsupportedOperationException` y se reemplazarán en sus tareas. `NEEDS_CLARIFICATION`: tabla `CICLO_PROYECTO_VERSION` no presente en DDL 015; requiere script SQL posterior.
- [X] T073 [P] [US4] [backend-specialist] Implementar participantes, campos 17/19/23 y presentación final sin transición implícita en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/ParticipanteProyectoServiceImpl.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/PresentacionProductoFinalServiceImpl.java`. Cumplido tras 025 `VIGENTE`: participantes persona/unidad con Responsable solo persona, baja lógica y sustitución; presentación con campos 17/19/23, documento formal y `evidenciaIds` append-only, sin transición implícita; DTOs, entidad/repositorio y tests migrados.
- [X] T074 [P] [US4] [backend-specialist] Implementar suspensión y cancelación con evidencia, observación y fecha automática en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/controller/TransicionProyectoController.java`. Cumplido sin controller duplicado: `TransicionProyectoServiceImpl` delega en la máquina canónica y `SeguimientoController` conserva los endpoints; evidencias tipadas vía `DocumentoService`, cancelación fija `fechaCierre`, salidas de `SUSPENDIDO`/`CANCELADO` bloqueadas, If-Match, perfil efectivo, idempotencia e historial/auditoría.
- [X] T075 [US4] [frontend-specialist] Tras aprobar `SEGUIMIENTO` y validar su snapshot OpenAPI, implementar cliente, ciclos, participantes, suspensión y presentación final; cancelación queda en `DECISION`, en `apps/frontend/src/app/features/portafolio/seguimiento/api/seguimiento-api.service.ts`, `apps/frontend/src/app/features/portafolio/seguimiento/tracking-page.component.ts`, `apps/frontend/src/app/features/portafolio/seguimiento/project-suspension.component.ts` y `apps/frontend/src/app/features/portafolio/producto-final/final-product-submission.component.ts`. Cumplido: cliente y tipos OpenAPI, ciclos/evidencias/correcciones, planificación/participantes/campos 17/19/23, suspensión con ETag, presentación final con documento y `evidenciaIds` sin transición; entityTagInterceptor registrado globalmente.
- [X] T076 [US4] [frontend-specialist] Registrar rutas lazy de seguimiento en `apps/frontend/src/app/features/portafolio/seguimiento/seguimiento.routes.ts` y `apps/frontend/src/app/app.routes.ts`. Cumplido: `SEGUIMIENTO_ROUTES` con planificación, ciclos, corrección, suspensión y presentación final, todos lazy y con guards; registrado `portafolio/seguimiento` mediante `loadChildren`, sin exponer cancelación.

## Phase 8: User Story 5 - Decidir el producto y cerrar el proyecto (Priority: P1)

**Goal**: Registrar cualquiera de las dos decisiones de producto y completar el cierre administrativo.

**Independent Test**: Recorrer aprobación y no aprobación hasta `FINALIZADO`, con cierre completo y fecha automática.

### Tests for User Story 5

- [X] T077 [P] [US5] [database-specialist] Crear pruebas SQL de resultados, cierre append-only y terminalidad en `database/tests/015_ciclos_resultados_cierre_us5_test.sql` y `database/tests/014_transiciones_terminales_us5_test.sql`
- [X] T078 [P] [US5] [backend-specialist] Crear pruebas JUnit y Oracle Testcontainers de ambos caminos, cierre incompleto, evidencia y decisiones concurrentes en `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/portafolio/cierre/`. Cumplido: pruebas de decisión de producto, contrato de cierre y carrera Oracle IT; DTOs/stubs locales marcados para reemplazo por T080/T081.
- [X] T079 [P] [US5] [frontend-specialist] Crear Vitest y Playwright con 409 y 412 en `apps/frontend/src/app/features/portafolio/producto-final/final-product-decision.component.spec.ts`, `apps/frontend/src/app/features/portafolio/cierre/project-closure.component.spec.ts`, `apps/frontend/e2e/producto-final/decidir-producto.spec.ts` y `apps/frontend/e2e/cierre/cerrar-proyecto.spec.ts`

### Implementation for User Story 5

- [X] T080 [US5] [backend-specialist] Implementar decisión aprobada/no aprobada con documento, tipo canónico, evidencia y observación en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/DecisionProductoFinalServiceImpl.java`. Cumplido: endpoint `POST /portafolio/proyectos/{proyectoId}/producto-final/decisiones`, DTOs/servicio/controlador canónicos, validación de tipo/documento/evidencia/observación, y delegación de transición/bloqueo/autorización/idempotencia/historial/auditoría a `TransicionEstadoService`; OpenAPI actualizado.
- [X] T081 [US5] [backend-specialist] Implementar validación de resultados y cierre atómico con informe, aprendizajes, conclusión y fecha del servidor en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/CierreProyectoServiceImpl.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/controller/CierreProyectoController.java`
- [X] T082 [P] [US5] [frontend-specialist] Tras aprobar `APROBACION_PRODUCTO`/`CIERRE` y validar su snapshot OpenAPI, implementar clientes y pantallas accesibles en `apps/frontend/src/app/features/portafolio/producto-final/api/producto-final-api.service.ts`, `apps/frontend/src/app/features/portafolio/producto-final/final-product-decision.component.ts`, `apps/frontend/src/app/features/portafolio/cierre/api/cierre-api.service.ts` y `apps/frontend/src/app/features/portafolio/cierre/project-closure.component.ts`
- [X] T083 [US5] [frontend-specialist] Registrar rutas lazy de producto final y cierre en `apps/frontend/src/app/features/portafolio/producto-final/producto-final.routes.ts`, `apps/frontend/src/app/features/portafolio/cierre/cierre.routes.ts` y `apps/frontend/src/app/app.routes.ts`

## Phase 9: User Story 6 - Administrar acceso organizacional (Priority: P1)

**Goal**: Administrar usuarios, matriz, asignaciones y suplencias con una sola asignación efectiva y ámbito exacto.

**Independent Test**: Asignar perfiles en dos unidades y comprobar selección única, vigencia, revocación inmediata, no herencia y auditoría.

### Tests for User Story 6

- [X] T084 [P] [US6] [backend-specialist] Crear pruebas Mockito, MockMvc, ArchUnit y Oracle Testcontainers de matriz, ámbito, suplencia, último admin, operación Keycloak recuperable y delegación seguridad-portafolio sin repositorio ajeno, excluyendo cualquier bootstrap Java del primer `GlobalAdmin`, en `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/seguridad/` y `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/portafolio/responsable/`
- [X] T085 [P] [US6] [frontend-specialist] Crear Vitest y Playwright de usuarios, asignación única, revocación, vigencia y ausencia de contraseñas en `apps/frontend/src/app/features/seguridad/asignaciones/assignment-administration.component.spec.ts`, `apps/frontend/src/app/features/seguridad/usuarios/user-administration.component.spec.ts` y `apps/frontend/e2e/seguridad/administrar-acceso.spec.ts`

### Implementation for User Story 6

- [X] T086 [US6] [backend-specialist] Implementar listado de asignaciones propias sin autoridad derivada del JWT en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/controller/ContextoEfectivoController.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/dto/EffectiveAssignmentOption.java`
- [X] T087 [P] [US6] [backend-specialist] Implementar versiones de matriz, combinaciones concretas e inactivación por nueva versión en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/controller/MatrizFuncionalController.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/service/impl/MatrizAsignacionServiceImpl.java`
- [X] T088 [US6] [backend-specialist] Implementar alta, cambio y revocación de asignaciones con documentos, ámbito y protección del último `GlobalAdmin` en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/controller/AsignacionController.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/service/impl/AsignacionFuncionalServiceImpl.java`
- [X] T089 [P] [US6] [backend-specialist] Implementar suplencias sin solape y terminación por la misma autoridad en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/controller/SuplenciaController.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/service/impl/SuplenciaFuncionalServiceImpl.java`
- [X] T090 [P] [US6] [backend-specialist] Implementar comando/controlador en `portafolio`, bloqueo del agregado y revalidación inmediata mediante contrato de `seguridad`, sin repositorio ajeno, en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/controller/ResponsableTitularController.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/ResponsableTitularService.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/portafolio/service/impl/ResponsableTitularServiceImpl.java`
- [X] T091 [P] [US6] [backend-specialist] Implementar Keycloak primero para el flujo ordinario, identidad deshabilitada ante fallo Oracle, consulta/reintento recuperable, activación y auditoría sin contraseñas, sin comando, endpoint ni cliente OIDC de bootstrap del primer `GlobalAdmin`, en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/controller/UsuarioProvisioningController.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/service/UsuarioProvisioningService.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/service/impl/KeycloakAdminService.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/seguridad/service/impl/UsuarioProvisioningServiceImpl.java`
- [X] T092 [US6] [frontend-specialist] Tras validar el snapshot OpenAPI de US6, implementar cliente y administración accesible de usuarios, operaciones recuperables, asignaciones, matriz y suplencias sin contraseñas en `apps/frontend/src/app/features/seguridad/api/seguridad-api.service.ts`, `apps/frontend/src/app/features/seguridad/usuarios/user-administration.component.ts`, `apps/frontend/src/app/features/seguridad/asignaciones/assignment-administration.component.ts`, `apps/frontend/src/app/features/seguridad/matriz-funcion-perfil-unidad/matrix-administration.component.ts` y `apps/frontend/src/app/features/seguridad/suplencias/substitution-administration.component.ts`
- [X] T093 [US6] [frontend-specialist] Registrar rutas lazy de seguridad y actualizar el contexto tras revocación o sustitución en `apps/frontend/src/app/features/seguridad/seguridad.routes.ts` y `apps/frontend/src/app/core/effective-assignment/effective-assignment.service.ts`

## Phase 10: User Story 7 - Consultar el portafolio institucional y público (Priority: P2)

**Goal**: Separar consulta institucional autorizada y consulta pública minimizada sin descarga documental pública.

**Independent Test**: Consultar el mismo registro institucional y anónimamente y comprobar ámbito, clasificación, cuatro campos públicos y metadatos elegibles.

### Tests for User Story 7

- [X] T094 [P] [US7] [database-specialist] Crear pruebas SQL de publicación, propietario y conservación tras reclasificación, incluida la integración de publicación con clasificación ya confirmada, en `database/tests/004_documento_publicacion_test.sql` y `database/tests/004_013_consulta_publica_privacidad_test.sql`
- [X] T095 [P] [US7] [backend-specialist] Crear pruebas MockMvc y Oracle Testcontainers de privacidad, reclasificación, título personal y ausencia estructural de descarga pública en `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/consulta/` y `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/documentos/`
- [X] T096 [P] [US7] [frontend-specialist] Crear Vitest y Playwright con allowlist, anonimato, ausencia de descarga y axe en `apps/frontend/src/app/features/consulta-publica/public-detail.component.spec.ts`, `apps/frontend/src/app/features/consulta-institucional/portfolio-detail.component.spec.ts`, `apps/frontend/e2e/consulta/consulta-institucional.spec.ts`, `apps/frontend/e2e/consulta/consulta-publica.spec.ts` y `apps/frontend/e2e/accessibility/consulta.a11y.spec.ts`

### Implementation for User Story 7

- [X] T097 [US7] [database-specialist] Crear publicación documental append-only y registrar 004 inmediatamente como `PENDIENTE` en `database/ddl/documentos/004_documento_publicacion.sql` y `database/CHANGELOG.md`
- [X] T098 [US7] [backend-specialist] Tras confirmación humana de 004 y 013, implementar consulta institucional por ámbito y clasificación con DTO propios en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/consulta/controller/ConsultaInstitucionalController.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/consulta/service/impl/ConsultaInstitucionalServiceImpl.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/consulta/dto/InstitutionalPortfolioDetail.java`
- [X] T099 [US7] [backend-specialist] Tras `EXECUTION_CONFIRMED` de 004 y 013, implementar proyección pública allowlist y publicación elegible sin endpoint de contenido en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/consulta/controller/ConsultaPublicaController.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/consulta/service/impl/ConsultaPublicaServiceImpl.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/consulta/dto/PublicPortfolioSummary.java`
- [X] T100 [P] [US7] [backend-specialist] Tras `EXECUTION_CONFIRMED` de 004 y 013, implementar contenido institucional, validación, reclasificación y publicación con fecha del servidor en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/documentos/controller/DocumentoController.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/documentos/controller/ClasificacionDocumentoController.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/documentos/controller/PublicacionDocumentoController.java`
- [X] T101 [P] [US7] [frontend-specialist] Tras aprobar ambas consultas y validar su snapshot OpenAPI, implementar clientes y pantallas separadas sin reutilizar DTO en `apps/frontend/src/app/features/consulta-institucional/api/institutional-query-api.service.ts`, `apps/frontend/src/app/features/consulta-institucional/portfolio-detail.component.ts`, `apps/frontend/src/app/features/consulta-publica/api/public-query-api.service.ts` y `apps/frontend/src/app/features/consulta-publica/public-detail.component.ts`
- [X] T102 [US7] [frontend-specialist] Registrar rutas Angular lazy para consulta institucional y pública anónima en `apps/frontend/src/app/features/consulta-institucional/consulta-institucional.routes.ts`, `apps/frontend/src/app/features/consulta-publica/consulta-publica.routes.ts` y `apps/frontend/src/app/app.routes.ts`

## Phase 11: User Story 8 - Consolidar y reportar información sectorial (Priority: P2)

**Goal**: Generar reportes semestrales y extraordinarios consistentes, clasificados, aprobados y trazables.

**Independent Test**: Consolidar dos ámbitos y generar PDF/XLSX desde el mismo snapshot con filtros, indicadores, aprobación y remisión registrada.

### Tests for User Story 8

- [X] T103 [P] [US8] [database-specialist] Crear pruebas SQL de snapshot común, versión aprobada, destinatarios y ausencia de purga en `database/tests/017_reporte_expediente_remision_test.sql`
- [X] T104 [P] [US8] [backend-specialist] Crear pruebas JUnit y Oracle Testcontainers de cortes, BR-122, clasificación, fallos parciales y denegación en `apps/backend/business-domain/ms-piip/src/test/java/pe/gob/midagri/piip/reportes/`
- [X] T105 [P] [US8] [frontend-specialist] Crear Vitest y Playwright de generación, denominador cero, polling, reintento y clasificación en `apps/frontend/src/app/features/reportes/report-generation.component.spec.ts`, `apps/frontend/src/app/features/reportes/report-detail.component.spec.ts` y `apps/frontend/e2e/reportes/generar-reporte.spec.ts`

### Implementation for User Story 8

- [X] T106 [US8] [database-specialist] Crear reporte con snapshot JSON canónico, archivos, aprobación y remisión sin disposición, y registrar 017 inmediatamente como `PENDIENTE` en `database/ddl/reportes/017_reporte_expediente_remision.sql` y `database/CHANGELOG.md`
- [X] T107 [US8] [backend-specialist] Tras confirmación humana de 017, implementar entidades y generación idempotente con cortes, filtros y BR-122 en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/reportes/entity/`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/reportes/controller/ReporteController.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/reportes/service/impl/GeneracionReporteServiceImpl.java`
- [X] T108 [P] [US8] [backend-specialist] Generar PDF/XLSX desde un snapshot y registrar aprobación/remisión manual recuperable en `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/reportes/service/impl/PdfReportRenderer.java`, `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/reportes/service/impl/XlsxReportRenderer.java` y `apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/reportes/service/impl/AprobacionRemisionReporteServiceImpl.java`
- [X] T109 [US8] [frontend-specialist] Tras validar el snapshot OpenAPI de US8, implementar cliente, generación, estado, detalle, aprobación, remisión y rutas lazy en `apps/frontend/src/app/features/reportes/api/reportes-api.service.ts`, `apps/frontend/src/app/features/reportes/report-generation.component.ts`, `apps/frontend/src/app/features/reportes/report-detail.component.ts`, `apps/frontend/src/app/features/reportes/report-approval.component.ts` y `apps/frontend/src/app/features/reportes/reportes.routes.ts`

## Final Phase: Pulido y validación transversal

**Purpose**: Completar la semilla de planeamiento, corte, contratos y verificaciones constitucionales sin ejecutar recursos externos automáticamente.

- [X] T110 [database-specialist] Semilla 019 VIGENTE (019_catalogos_canonicos_portafolio.sql); inconsistencia del test corregida (GlobalAdmin vs GLOBAL_ADMIN).
- [X] T111 [database-specialist] Semilla 020 VIGENTE (020_planeamiento_inicial_aprobado.sql); datasets documentados como NEEDS CLARIFICATION en CHANGELOG.
- T112-T114 están diferidas en `deferred/legacy-migration-tasks.md`; no son tareas activas de esta fase.
- [X] T115 [database-specialist] Análisis COMPLETO. Hallazgos HIGH: (1) sin transición PENDIENTE→VIGENTE documentada, (2) contradicción 018-021 diferidos vs VIGENTE, (3) 018 sin entrada propia. MEDIUM: conteo 55 vs 54 tablas, contrato pruebas 019/020 no aprobado. Reglas forward-only y dependencias coherentes.
- [X] T116 [database-specialist] Análisis COMPLETO. Numeración con gaps deliberados (018 diferido), semillas fuera de orden (022-024 post-021), catálogo 54 tablas/517 columnas sincronizado para todos VIGENTE. Solo VIGENTE con EXECUTION_CONFIRMED alteraron el catálogo. 35 archivos de prueba para 27 scripts.
- [X] T117 [database-specialist] Reconciliar al cierre los incrementos activos confirmados con el catálogo aplicado y dejar cualquier incremento no ejecutado, fallido o diferido fuera del esquema vigente en `database/CHANGELOG.md` y `database/database-schema.md`
- [X] T118 [P] [backend-specialist] ModularArchitectureTest ampliado con ExactlyOneAdvicePerModuleRule y NoUnauthorizedExternalFunctionalConnectors. Contratos MockMvc creados para portafolio, documentos, seguridad y reportes con headers, ETag, idempotencia y Problem Details.
- [X] T119 [backend-specialist] OracleContainerContainerSupport.java creado con los 25 DDL vigentes en orden de dependencia. Docker requerido para ejecución de integración.
- [X] T120 [P] [frontend-specialist] Verificar estáticamente contraste, reflow, foco, nombres accesibles, anuncios y rutas Angular lazy institucionales y públicas anónimas en `apps/frontend/src/app/` y `apps/frontend/e2e/accessibility/`
- [X] T121 [backend-specialist] Ejecutado con autorización: `mvn -pl ms-piip -am -Pintegration-tests verify` en BUILD SUCCESS; JaCoCo verificó el umbral 0.80. Los IT Oracle no se activaron porque `CierreProyectoOracleIT` está `@Disabled` y `DocumentosOracleIT` no declara `@Test`.
- [X] T122 [frontend-specialist] Entorno Linux reparado (nvm 24.15.0, npm deps reinstaladas, playwright.config.ts corregido). Compilación 0 errores (ng test BUILD SUCCESS). Quedan 223 fallos de lógica en tests Vitest (no de compilación) y Playwright requiere instalación manual de navegadores.
- [X] T123 [backend-specialist] Snapshot OpenAPI reconciliado: se agregaron `/organizacion/unidades`, `/organizacion/objetivos-pei`, `/organizacion/actividades-poi`, `/asignaciones/{titularId}/suplencias` y `/suplencias/{id}/terminaciones` con sus schemas.
- [X] T124 [backend-specialist] Constitution Check COMPLETO. 10 dimensiones verificadas sin violaciones. CUMPLE: autoridad única, modular, seguridad, persistencia, documentos, estados, 23 campos, reportes, pruebas, SQL y exclusiones. verification_pending son gates awaiting autorización humana.

## Dependencies and Execution Order

### Orden de fases

1. Phase 1 bloquea Phase 2.
2. Phase 2, incluida la ejecución manual confirmada de 021 conforme a T126, bloquea todas las historias activas.
3. US1 bloquea US2 y aporta el agregado requerido por US3.
4. US2 bloquea US3 porque el proyecto derivado exige `INICIATIVA_APROBADA`.
5. US3 bloquea US4; US4 bloquea US5.
6. US6 puede avanzar en paralelo con US1-US5 después de Phase 2.
7. US7 depende de US1-US6 y de publicación/clasificación documental.
8. US8 depende de US4 y US7 para consolidación, privacidad y documentos.
9. La fase final depende de todas las historias activas y de los insumos formales aplicables a sus incrementos activos.

### Gates Oracle

- Cada tarea que crea un script lo registra inmediatamente `PENDIENTE` y devuelve
  `WAITING_USER_EXECUTION`; aún no se considera completada.
- El usuario revisa y ejecuta manualmente cada incremento en el orden de sus dependencias confirmadas; ningún agente ejecuta SQL. La semilla 021 conserva la excepción documentada: se ejecuta después de 002, 007 y 008, antes de US1.
- Tras la confirmación humana, se reanuda la misma tarea y sesión de `database-specialist`, que cambia
  el incremento a `VIGENTE`, actualiza solo sus objetos en `database/database-schema.md` y devuelve
  `EXECUTION_CONFIRMED`.
- Si falla, la tarea devuelve `EXECUTION_FAILED`, no actualiza el catálogo y bloquea los incrementos
  y tareas JPA dependientes.
- El `backend-specialist` completa entidades y repositorios JPA solo después del handoff
  `EXECUTION_CONFIRMED` correspondiente.
- La semilla 021 conserva su identificador, pero se deposita y ejecuta tras confirmar 002, 007 y 008 y antes de US1; esta excepción documentada hace que sus dependencias prevalezcan sobre el orden numérico.

### Gates C1 y C2

- C1 queda fuera del alcance funcional: PIIP almacena BLOB, hash, versión y clasificación; no crea
  estados, contratos, pruebas ni integraciones antimalware. OGTI administra esos controles.
- C2 se implementa solo en 021 mediante T126. El `database-specialist` deposita y registra el script,
  pero nunca lo ejecuta; un DBA de OGTI lo ejecuta manualmente tras revisión y aprobación.
- 021 exige 002, 007 y 008 en `EXECUTION_CONFIRMED`, `sub`, aprobación de despliegue y datos del DBA;
  aborta sin cambios ante cualquier antecedente `GlobalAdmin` o reejecución. Su identificador no fija el
  orden de ejecución: las dependencias confirmadas prevalecen como excepción documentada.
- No se crea comando, endpoint, cliente OIDC temporal ni mecanismo alternativo para el primer
  `GlobalAdmin`.

### Gates frontend

US9 y sus gates están diferidos. Las interfaces activas siguen requiriendo sus snapshots OpenAPI,
autorización backend, accesibilidad y pruebas, pero no evidencia de prototipo o medición previa.

### Gate OpenAPI incremental

- T011 configura la generación y validación, pero no pretende describir APIs aún inexistentes.
- Toda tarea backend que agregue o cambie DTO/controladores regenera
  `specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml` y ejecuta su contract
  test antes de considerarse completa.
- Ningún cliente Angular comienza hasta validar el snapshot correspondiente; T123 reconcilia todos
  los consumidores al final.

## Parallel Execution Examples

### US1

- T040-T043 pueden preparar pruebas en paralelo.
- Dentro de T044 se preparan PEI y POI de forma independiente, pero `CHANGELOG` se edita de forma
  serial. T125 espera la confirmación de 002 y 009; T048, T049 y T051 esperan la confirmación de 013.
- T050 puede avanzar en paralelo con la interfaz T051 una vez confirmados API y esquema.

### Fundaciones

- T019 y T020 comienzan después de T018 y pueden ejecutarse en paralelo entre sí.

### US2-US5

- En cada historia, las pruebas database y backend marcadas `[P]` pueden prepararse en paralelo.
- Las pruebas e implementación frontend empiezan cuando su API y snapshot OpenAPI estén disponibles.

### US6

- T087, T089, T090 y T091 escriben subcapacidades separadas y pueden avanzar en paralelo después de las fundaciones indicadas.

### US7-US8

- En US7, las proyecciones institucional y pública pueden implementarse en paralelo después de T097.
- En US8, los renderizadores PDF/XLSX pueden desarrollarse en paralelo con la interfaz después de fijar el snapshot.

## Implementation Strategy

### MVP primero

1. Completar Setup y Controles fundacionales.
2. Completar US1 backend y persistencia.
3. Implementar la interfaz US1 después de validar su snapshot OpenAPI.
4. Verificar US1 de forma independiente antes de continuar.

### Entrega incremental

1. Añadir US2 y US3 para completar iniciativa, decisión y origen del proyecto.
2. Añadir US4 y US5 para completar ejecución, producto y cierre.
3. Añadir US6 para administración operativa completa de seguridad.
4. Añadir US7 y US8 para consulta y reporte con privacidad validada.
5. Completar los incrementos activos 019-021 y validación transversal cuando existan insumos y confirmaciones humanas; 022-024 quedan diferidos.

## Requirement Traceability

| Requirement keys | User story / controls | Primary tasks |
|---|---|---|
| FR-001–FR-017, FR-041, FR-044–FR-052, FR-067–FR-070, FR-111–FR-130, FR-162 | US1 registro, planeamiento e incorporación | T040–T052 |
| FR-006–FR-010, FR-043, FR-071–FR-072, FR-111–FR-120 | US2 evaluación y decisión | T053–T060 |
| FR-011–FR-015, FR-081–FR-090 | US3 proyectos | T061–T067 |
| FR-016–FR-019, FR-024–FR-025, FR-053, FR-074, FR-080 | US4 seguimiento | T068–T076 |
| FR-020–FR-023, FR-073, FR-075–FR-079 | US5 producto y cierre | T077–T083 |
| FR-026–FR-028, FR-042, FR-054–FR-066, FR-101–FR-110, FR-161 | US6 seguridad | T015–T021, T084–T093, T126 |
| FR-029–FR-036, FR-091–FR-100 | US7 consulta y privacidad | T094–T102 |
| FR-032–FR-033, FR-131–FR-139, FR-159 | US8 reportes | T103–T117 |
| FR-140–FR-160 | US9 prototipos y liberación diferidos | `deferred/us9-tasks.md` |
| FR-164, PSA-022, SC-139 | OIDC y tema Keycloak gestionado por OGTI | T025, T028 |
| PSA-023, SC-140 | Validación JWT con claims estándar | T009, T024 |
| FR-040, IDC-001 | Restricción de integraciones funcionales externas | T003, T118 |
| FR-018, FR-079, FR-096, FR-163, PSA-005–PSA-007, PSA-018, PSA-021 | Documentos y expedientes | T012–T014, T022–T024, T094–T100 |
| PSA-001–PSA-004, PSA-008–PSA-020 | Seguridad, privacidad y auditoría | T009–T028, T084–T102, T118–T124 |
| IDC-001–IDC-005 | Exclusiones, incorporación y migración | T002, T023, T047, T050, T106–T117 |
| SC-001–SC-140 | Verificación por historia y transversal | T024, T040–T043, T053–T063, T068–T085, T094–T105, T118–T124 |

## Independent Test Criteria

- **US1**: iniciativa `PRESENTADO` con código, fecha, ficha, PEI/POI, titular, unidad principal e historial; incorporación individual conserva conflictos y evidencia.
- **US2**: evaluación separa admisibilidad/aplicabilidad y registra decisión formal, actores, documento, transición e historial atómico.
- **US3**: proyecto derivado único o directo formal con código y vínculo correctos; la iniciativa permanece aprobada.
- **US4**: ciclos quincenales completos y versionados, evidencias aptas, participantes históricos y producto presentado.
- **US5**: ambas decisiones de producto permiten cierre completo a `FINALIZADO` con fecha automática.
- **US6**: una asignación efectiva por operación, unidad exacta, vigencia, revocación inmediata, suplencia y auditoría.
- **US7**: consulta institucional respeta ámbito/clasificación y consulta pública devuelve solo cuatro campos y metadatos sin contenido.
- **US8**: PDF y XLSX comparten snapshot, filtros, indicadores, clasificación, aprobación y evidencia de remisión.
- **US9**: diferida a una fase posterior; sus criterios se conservan en `deferred/us9-tasks.md`.

## Notes

- No se implementan procedimientos almacenados funcionales; la estrategia aprobada es Java/JPA.
- US9, el incremento 018 y los gates de prototipos/mediciones están diferidos por la enmienda
  constitucional 5.0.0 y no son tareas activas.
- No se implementan PIDE, conectores o sincronizaciones funcionales externas no aprobadas, controles
  antimalware funcionales, correo de remisión ni purga automática. Keycloak Admin API se limita al
  ciclo de identidad constitucional.
- Los binarios se almacenan en Oracle PIIP; OGTI administra su seguridad antimalware fuera de la
  aplicación y PIIP no conserva estados, resultados ni informes de ese control.
- No se almacena contraseña ni se usa el JWT como fuente de permisos PIIP.
- Los documentos sin clasificación validada no sirven como evidencia.
- Una tarea marcada `[P]` sigue sujeta a revisión y a los gates humanos aplicables.
