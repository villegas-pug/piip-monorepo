---
name: be-create-module
description: Crea un módulo o subcapacidad backend Java/Spring Boot de PIIP con CRUD completo; úsala solo desde backend-specialist cuando exista una especificación aprobada.
---

# Crear módulo backend PIIP

## Precondiciones obligatorias

1. Lee la constitución, `AGENTS.md`, especificación aprobada, plan y contratos.
2. Verifica que la capacidad pertenezca a un módulo constitucional o que su condición de
   subcapacidad esté aprobada. Una tabla o entidad no constituye por sí sola un módulo.
3. Detén el trabajo ante cualquier `NEEDS CLARIFICATION` material: actores, autorización y
   alcance por unidad, privacidad, auditoría, transición, documentos, eliminación, contrato API,
   persistencia o autoridad de reglas.
4. Pregunta expresamente si el CRUD completo usa `JPA` o `SP`. No mezcles estrategias en las
   operaciones de un mismo CRUD sin una nueva decisión aprobada.
5. Confirma qué representa `DELETE`. El borrado físico o lógico no es predeterminado en PIIP.

## Ubicación y capas

Crea código bajo:

```text
apps/backend/business-domain/ms-piip/src/main/java/pe/gob/midagri/piip/<modulo>/
```

Usa solo las capas necesarias de la estructura constitucional: `controller`, `service/impl`,
`repository`, `dto`, `entity`, `exception`, `mapper` y `event`. No crees directorios vacíos ni
`model`, `client` o `integration`. Los contratos de servicio exponen DTOs o tipos simples, nunca
entidades JPA. Otros módulos se comunican por servicios, DTOs o eventos; nunca por repositorios,
entidades o tablas ajenas.

## Implementación CRUD

Implementa únicamente operaciones aprobadas. Para cada una incluye, según corresponda:

- DTO request/response con validación Bean Validation basada en el contrato.
- Entidad JPA y MapStruct solo si la estrategia es `JPA`.
- Repositorio de dominio encapsulando JPA o el acceso Oracle.
- Servicio con transacción, autorización efectiva Oracle, auditoría y reglas en una única fuente.
- Controlador delgado con respuestas HTTP y OpenAPI.
- Excepciones del módulo y `@RestControllerAdvice` limitado a su paquete.
- Pruebas JUnit 5/Mockito, arquitectura y Oracle Testcontainers cuando afecte persistencia o SP.

No copies patrones heredados sin verificar que cumplen la constitución. Añade solo eventos internos
que la especificación requiera. No ejecutes Maven, pruebas ni servicios sin autorización.

## Estrategia JPA

Confirma que cada estructura requerida existe y coincide con `database/database-schema.md`. Si
falta una estructura, devuelve `BLOCKED_DATABASE` al primario con tabla, columnas, constraints,
motivo y consumidores. No crees SQL desde esta SKILL.

## Estrategia SP

Devuelve `BLOCKED_DATABASE` al primario con el contrato del procedimiento: nombre propuesto,
parámetros, cursores/salidas, errores, tablas, reglas autoritativas, auditoría, transacción y
pruebas. Espera `EXECUTION_CONFIRMED` de `database-specialist` antes de crear el acceso Java.

El repositorio que invoque SP extiende `BaseOracleRepository`. Si el contrato excede sus
capacidades, usa `be-enhance-base-oracle-repository` antes de implementar el módulo. La mejora
debe ser genérica y sin nombres ni reglas del módulo consumidor.

## Cierre

Devuelve el contrato de handoff de orquestación con archivos, decisiones, bloqueos y verificaciones
pendientes. No declares completa una funcionalidad si faltan pruebas requeridas o confirmación de
ejecución de scripts.
