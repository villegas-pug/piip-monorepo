---
name: be-implement-authorization-audit
description: Implementa autorización efectiva Oracle y auditoría backend PIIP; úsala solo desde backend-specialist para operaciones sensibles aprobadas.
---

# Autorización y auditoría backend

Verifica la especificación: permiso funcional, rol, unidad y descendencia, datos privados, evento
de auditoría, actor, alcance, resultado y retención. Keycloak solo autentica identidad; Oracle es
la autoridad para roles, permisos y alcance organizacional.

Implementa la comprobación en servicio o procedimiento según la autoridad aprobada. Registra
accesos sensibles, denegaciones, cambios, transiciones, documentos y catálogos controlados de
forma inmutable. No otorgues acceso usando solo roles del token ni dejes la decisión al
controlador o frontend. Incluye pruebas de autorización y auditoría, sin ejecutarlas.
