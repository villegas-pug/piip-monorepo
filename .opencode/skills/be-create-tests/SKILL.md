---
name: be-create-tests
description: Crea pruebas backend JUnit, Mockito y Oracle Testcontainers de PIIP; úsala solo desde backend-specialist cuando cambie comportamiento, seguridad, persistencia o contratos.
---

# Crear pruebas backend

Deriva escenarios de la especificación, contratos y constitución. Cubre reglas de negocio,
transacciones, autorización por unidad, auditoría, errores HTTP, mapeos, procedimientos, límites
arquitectónicos y contratos afectados. Usa Mockito cuando aporte aislamiento y Oracle
Testcontainers para persistencia real. No sustituyas Oracle por un motor incompatible.

No ejecutes pruebas, Maven, Docker ni Oracle sin autorización explícita. Informa los comandos de
verificación pendientes y la cobertura que requiere revisión.
