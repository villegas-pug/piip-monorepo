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

## Almacenamiento documental local

**Decisión**: Definir `DocumentStorage` en `documentos/service/` y un adaptador de filesystem local en
`documentos/service/impl/` para desarrollo. Las referencias de almacenamiento son opacas para DTO,
entidades y controladores.

**Justificación**: Permite verificar archivos de hasta 100 MB, versiones, hash y recuperación local
sin seleccionar un proveedor institucional no aprobado. Conserva el puerto constitucional para
cambiar de adaptador en otra fase.

**Alternativas consideradas**:

- Guardar BLOB en Oracle: rechazado por no estar establecido y acoplar contenido con metadatos.
- Elegir almacenamiento cloud/institucional: rechazado porque sería infraestructura o integración
  no aprobada.

## Análisis antimalware

**Decisión**: Modelar el estado y un puerto interno de recepción de resultados. En desarrollo y
pruebas se usa un test double controlado; no se implementa un conector a un motor externo hasta que
exista una decisión aprobada.

**Justificación**: La regla funcional exige `PENDIENTE`, `LIMPIO` e `INFECTADO`, pero las fuentes no
seleccionan producto, protocolo o infraestructura. Un test double solo valida el caso de uso local y
no simula una integración de producción.

**Alternativas consideradas**:

- Seleccionar ClamAV u otro motor: rechazado por introducir una integración no aprobada.
- Marcar archivos automáticamente `LIMPIO`: rechazado porque vulnera la regla de evidencia formal.

## Reportes consistentes

**Decisión**: Persistir un snapshot lógico y parámetros de cada generación; producir PDF y XLSX desde
el mismo snapshot mediante una operación idempotente potencialmente asíncrona.

**Justificación**: Garantiza el mismo corte, versión de datos, hash y recuperación ante fallo parcial.
Evita que dos consultas separadas produzcan resultados divergentes.

**Alternativas consideradas**:

- Generación síncrona sin snapshot: rechazada por inconsistencia y recuperación deficiente.
- Vista materializada `MV_PORTAFOLIO_RESUMEN`: diferida hasta demostrar necesidad de rendimiento.

## Renderizado Angular

**Decisión**: Mantener consulta pública compatible con SSR y ejecutar recorridos institucionales con
OIDC en cliente después de hidratación. Las rutas se separan por feature lazy.

**Justificación**: La consulta pública es anónima y puede beneficiarse de SSR; el flujo Authorization
Code con PKCE requiere contexto de navegador y no debe exponer tokens en el servidor de renderizado.

**Alternativas consideradas**:

- Prerenderizar todas las rutas: rechazado para datos dinámicos y áreas protegidas.
- Manejar tokens institucionales en SSR: rechazado por ampliar superficie de seguridad sin requisito.

## Decisiones no resueltas por investigación

Las siguientes son funcionales y permanecen bloqueadas; no se resuelven técnicamente:

- valores y autoridad de mantenimiento de Objetivo PEI y Actividad POI;
- matriz cargo o función-perfil-unidad;
- actor y evento de publicación documental y generación de `fechaPublicacion`.
