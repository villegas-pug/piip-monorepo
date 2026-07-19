---
name: be-enhance-base-oracle-repository
description: Mejora BaseOracleRepository de shared-data para contratos Oracle reutilizables; úsala solo desde backend-specialist cuando un SP aprobado exceda sus capacidades.
---

# Mejorar BaseOracleRepository

Lee el contrato SP aprobado y la implementación actual. La mejora debe vivir en
`shared-data`, conservar bajo acoplamiento y no conocer módulos, tablas, procedimientos ni reglas
de negocio concretas.

Implementa solo capacidades genéricas justificadas por el contrato, por ejemplo: schema/package
configurable, parámetros declarados, tipos IN/OUT Oracle, múltiples OUT, cursores mediante
`RowMapper`, resultados escalares, mapeo seguro, y traducción uniforme de errores. Evita casts
silenciosos, descubrimiento de metadatos no controlado y fallbacks de tipos incorrectos.

No introduzcas commits autónomos ni límites transaccionales: pertenecen al servicio de aplicación.
Incluye pruebas unitarias y de integración requeridas, pero no las ejecutes sin autorización.
Devuelve los cambios de contrato que el módulo consumidor debe aplicar y cualquier limitación que
permanezca como `NEEDS CLARIFICATION`.
