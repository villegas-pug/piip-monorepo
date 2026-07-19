---
name: fe-create-api-client
description: Crea clientes HTTP tipados Angular para contratos PIIP; úsala solo desde frontend-specialist cuando exista un contrato API aprobado.
---

# Crear cliente API Angular

Implementa modelos y servicios tipados que reflejen exclusivamente el contrato aprobado bajo
`/api/v1`. Centraliza configuración HTTP en `core`, maneja errores estructurados y no dupliques
validaciones ni autorización backend. No inventes endpoints, campos o mensajes de negocio.

Incluye pruebas de cliente y devuelve un handoff al primario si el contrato presenta ambigüedades.
