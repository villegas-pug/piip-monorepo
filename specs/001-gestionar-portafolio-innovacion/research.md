# Investigación técnica: Gestión del Portafolio PIIP

Este artefacto contiene solo decisiones técnicas realmente pendientes. No reabre decisiones
funcionales de la Constitución o la especificación.

## Autoridad de reglas y persistencia

**Decisión**: Usar servicios de aplicación Java con Spring Data JPA como única autoridad funcional.
Oracle se limita a persistencia, PK/FK/UK/CHECK, índices, `@Version` y bloqueo de filas. No se crean
procedimientos funcionales para la Fase 1.

**Justificación**: El backend existente ya usa JPA; la orquestación, autorización, auditoría y límites
transaccionales pertenecen constitucionalmente a servicios de aplicación. `PESSIMISTIC_WRITE`,
`@Version` y constraints resuelven correlativos, primera transición confirmada, único proyecto
derivado y suplencias sin duplicar reglas en PL/SQL.

**Alternativas consideradas**:

- Procedimientos Oracle para correlativos, transiciones y asignaciones: rechazados porque crearían
  dos modelos de implementación y contratos adicionales sin necesidad demostrada.
- Máquina de estados en `TRANSICION_PERMITIDA`: rechazada; el baseline contiene filas contrarias a
  la Constitución y la tabla quedará como legado inactivo.

## Almacenamiento documental Oracle

**Decisión**: Definir `DocumentStorage` en `documentos/service/` y una implementación BLOB Oracle en
`documentos/service/impl/`. La persistencia del binario es opaca para DTO, entidades y controladores.

**Justificación**: La Constitución 4.0.0 fija Oracle PIIP como almacenamiento de binarios y conserva el
puerto para aislar persistencia, límites, versiones e integridad SHA-256.

**Alternativas consideradas**:

- Filesystem local: rechazado porque contradice la persistencia Oracle aprobada.
- Elegir almacenamiento cloud/institucional: rechazado porque sería infraestructura o integración
  no aprobada.

## Responsabilidad antimalware

**Decisión**: No modelar estados, resultados, informes, puertos ni gates antimalware en PIIP. OGTI
administra análisis, bloqueo, cuarentena y respuesta sobre la infraestructura Oracle.

**Justificación**: La Constitución 4.0.0 separa la seguridad técnica de plataforma de las reglas
funcionales. PIIP conserva hash, clasificación, versión y auditoría documental, pero no consume datos
antimalware ni condiciona con ellos la evidencia formal.

**Alternativas consideradas**:

- Integrar un motor o estado en PIIP: rechazado por duplicar una responsabilidad exclusiva de OGTI.
- Conservar un test double antimalware: rechazado porque validaría comportamiento fuera del alcance.

## Reportes consistentes

**Decisión**: Persistir un snapshot lógico y parámetros de cada generación; producir PDF y XLSX desde
el mismo snapshot mediante una operación idempotente potencialmente asíncrona.

**Justificación**: Garantiza el mismo corte, versión de datos, hash y recuperación ante fallo parcial.
Evita que dos consultas separadas produzcan resultados divergentes.

**Alternativas consideradas**:

- Generación síncrona sin snapshot: rechazada por inconsistencia y recuperación deficiente.
- Vista materializada `MV_PORTAFOLIO_RESUMEN`: diferida hasta demostrar necesidad de rendimiento.

## Renderizado Angular

**Decisión**: Entregar consultas públicas e institucionales como rutas Angular lazy del lado del
cliente. OIDC opera únicamente en los recorridos institucionales.

**Justificación**: La especificación no demuestra una necesidad de SEO ni renderizado en servidor. El
flujo Authorization Code con PKCE requiere contexto de navegador y la ruta pública anónima mantiene su
allowlist sin incorporar un patrón adicional de renderizado.

**Alternativas consideradas**:

- Prerenderizar todas las rutas: rechazado para datos dinámicos y áreas protegidas.
- Incorporar SSR para la consulta pública: rechazado por añadir complejidad sin una necesidad aprobada.

## Propiedad documental institucional

**Decisión**: Usar `ExpedienteInstitucional` como propietario de documentos formales sin iniciativa o
proyecto. Cada serie documental pertenece de forma excluyente a un registro de portafolio o a un
expediente, y todas sus versiones heredan esa pertenencia.

**Justificación**: Mantiene FK reales y evita asociaciones polimórficas sin integridad o tablas
documentales duplicadas por módulo. También impide que documentos administrativos aparezcan en la
consulta pública del portafolio.

**Alternativas consideradas**:

- Referencia externa y hash sin archivo: rechazada porque reduce la evidencia gestionada por PIIP.
- Tabla documental por módulo: rechazada por duplicar versionado, clasificación e integridad.

## Versionado documental sobre el baseline

**Decisión**: Conservar `DOCUMENTO` como tabla de cada versión documental y agregar
`DOCUMENTO_SERIE` como raíz del documento lógico. Todas las filas `DOCUMENTO` de una serie heredan su
propietario excluyente e inmutable.

**Justificación**: Reduce la migración destructiva del baseline, conserva identificadores e historial
y coincide con el modelo lógico que no propone una tabla adicional `DOCUMENTO_VERSION`.

## Snapshot de reportes

**Decisión**: Persistir el contenido inmutable del snapshot como JSON canónico en CLOB, acompañado de
versión de esquema, hash SHA-256, corte, parámetros y clasificación. PDF y XLSX se renderizan desde el
mismo payload.

**Justificación**: El snapshot es evidencia de un corte, no una proyección mutable de consulta. La
forma canónica permite verificar integridad y evita duplicar un modelo relacional de solo lectura.

## OIDC en Angular

**Decisión**: Usar `keycloak-js` detrás de servicios Angular propios. `issuer`, `clientId`, redirect
URI, post-logout URI y scopes se cargan mediante configuración runtime externa. El frontend usa
`sub` para identidad y trata `email` y datos de `profile` como informativos.

**Justificación**: Usa el adaptador oficial sin convertir claims Keycloak en permisos PIIP ni exponer
tokens fuera del contexto de navegador.

## Fallo parcial Keycloak y Oracle

**Decisión**: Si Keycloak crea la identidad y Oracle falla, conservar la identidad deshabilitada y
una operación recuperable, idempotente y auditada. Un reintento completa Oracle sin crear otra
identidad.

**Justificación**: Evita una eliminación remota irreversible y permite recuperación controlada sin
considerar activa una identidad incompleta.

## Gobierno técnico Oracle

**Decisión**: Los scripts 002-024 son incrementos de ejecución única y fail-fast. El diccionario
físico debe recibir revisión humana DB antes del DDL, y cada script se registra `PENDIENTE` en
`database/CHANGELOG.md` en el mismo cambio que lo crea. Las respuestas idempotentes tienen una
ventana inicial configurable de siete días.

**Justificación**: Oracle realiza commits implícitos de DDL; la prevalidación y el registro inmediato
son más seguros que intentar reejecuciones parciales. La expiración idempotente no afecta auditoría o
evidencia funcional.

## Decisiones funcionales incorporadas

- El Evaluador confirma la publicación de una versión `PUBLICO`; el servidor fija la fecha.
- La primera asignación `GlobalAdmin` se crea solo mediante la semilla SQL 021 manual y auditada.
- La matriz versionada combina función, perfil y unidad concreta; la asignación deriva esos valores.
- PEI y POI tienen versiones independientes, aprobadas por planeamiento y registradas por
  `GlobalAdmin`.

Quedan pendientes insumos, no decisiones de diseño: valores OIDC por ambiente, dataset sintético
aprobado, datasets PEI/POI, matriz funcional, mapeos legacy, revisión humana del diccionario físico y
rotación externa de secretos.
