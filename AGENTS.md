# Guía de PIIP

## Idioma y ejecución
- Responde, documenta, planifica, crea comentarios de código y escribe mensajes de commit en español; conserva en inglés técnico los identificadores, rutas y código.
- No ejecutes compilaciones, empaquetados, servidores, pruebas ni operaciones de base de datos sin autorización explícita del usuario. Linters, formateadores y comprobaciones de tipos sí están permitidos.
- Si el usuario pide verificar que algo funciona, solicita autorización antes de compilar o ejecutar pruebas.

## Estructura actual
- El backend está en `apps/backend/business-domain/`: es un reactor Maven con Java 21 y Spring Boot 3.2.5. Sus módulos son `shared-data` (utilidades compartidas) y `ms-piip` (aplicación Spring Boot que depende de `shared-data`).
- El punto de entrada es `pe.gob.midagri.piip.PiipApplication`. El paquete raíz es `pe.gob.midagri.piip` y el código compartido usa `pe.gob.midagri.piip.shareddata`.
- Con autorización, ejecuta objetivos Maven desde `apps/backend/business-domain/` mediante el reactor, por ejemplo `mvn -pl ms-piip -am <goal>`; el agregador no incluye Maven Wrapper.
- `ms-piip` usa Oracle, valida el esquema con `spring.jpa.hibernate.ddl-auto=validate`, escucha en el puerto `4001` y sirve bajo `/api/v1`. El perfil activo por defecto es `dev`; define `PROFILE` y las variables `ORACLE_<PERFIL>_*` antes de cualquier ejecución autorizada.
- La validación de JWT requiere `KEYCLOAK_ISSUER_URI` y `PIIP_JWT_AUDIENCE`. No sustituyas las variables de entorno por credenciales, destinos Oracle o material de wallet en archivos versionados.
- `apps/frontend/` contiene el scaffold Angular 22. `database/ddl/init/001_baseline_piip.sql` es el DDL inicial de Oracle y declara que se ejecuta una sola vez; nunca lo ejecutes ni lo apliques a una base compartida sin autorización.

## Reglas del producto
- `.specify/memory/constitution.md` tiene precedencia sobre las fuentes de menor nivel. Para funcionalidad nueva o cambios de negocio, parte de una especificación aprobada; registra como `NEEDS CLARIFICATION` cualquier requisito material no resuelto en vez de inferirlo.
- La arquitectura objetivo es un monolito modular PIIP. Los módulos se comunican solo por servicios, DTO o eventos internos, nunca mediante tablas o repositorios de otro módulo. No crees directorios genéricos `model/`, `client/` ni `integration/`.
- Mantén las reglas de negocio, transacciones y transiciones en servicios de aplicación o procedimientos Oracle según la especificación. Los controladores solo validan y delegan; los contratos de servicio no exponen entidades JPA.
- Keycloak es la fuente de identidad y credenciales; Oracle lo es de roles, permisos y alcance organizacional. Toda operación sensible requiere autorización efectiva en backend y evidencia de auditoría.
- Los cambios de esquema son scripts SQL manuales, revisables y versionados bajo `database/`; no introduzcas Flyway ni Liquibase. Evita cambios destructivos de datos y cualquier acción sobre recursos compartidos sin autorización humana.
- Las reglas, catálogos y estados canónicos de portafolio, documentos y auditoría están definidos en la constitución; no los inventes ni los dupliques entre Java y PL/SQL.

## Calidad y especificaciones
- Los cambios que afecten reglas de negocio, seguridad, persistencia, contratos API, documentos o comportamiento requieren pruebas conforme a la constitución. El estándar previsto es JUnit 5/Mockito/Oracle Testcontainers en backend y Vitest/Playwright en frontend.
- Para artefactos de Spec Kit y documentación descriptiva, usa español. Las descripciones de frontmatter también van en español; pueden conservarse en inglés los nombres de archivos, claves de frontmatter y encabezados técnicos convencionales.

## Orquestación OpenCode
- `PLAN` y `BUILD` median todo trabajo entre `frontend-specialist`, `backend-specialist` y `database-specialist`; los especialistas no se comunican ni delegan entre sí.
- Las SKILLs se invocan solo desde su especialista propietario: `fe-*`, `be-*` y `db-*`, respectivamente. Los agentes primarios no invocan SKILLs.
- Toda edición de un especialista solicita aprobación. Para SQL, deposita scripts y actualiza `database/CHANGELOG.md`, pero solo actualiza `database/database-schema.md` después de la confirmación humana de ejecución exitosa.
- El protocolo de handoff y los estados de coordinación están en `.opencode/instructions/orchestration.md`.
