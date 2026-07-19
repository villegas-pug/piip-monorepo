---
name: fe-create-feature
description: Crea una feature Angular 22 de PIIP con carga diferida y accesibilidad; úsala solo desde frontend-specialist para funcionalidad aprobada.
---

# Crear feature Angular PIIP

Lee la especificación y contrato API aprobados. Crea la feature bajo `apps/frontend/src/app/features`
con rutas lazy cuando corresponda, componentes standalone, Angular Material, formularios tipados,
diseño responsive y WCAG 2.1 AA. Usa `core` solo para asuntos singleton y `shared` solo para
presentación reutilizable sin reglas de negocio.

No decidas permisos, transiciones ni reglas en el cliente. Maneja los estados y errores expuestos
por el backend. Solicita un handoff si falta contrato API o definición de identidad. Crea pruebas
requeridas, pero no las ejecutes sin autorización.
