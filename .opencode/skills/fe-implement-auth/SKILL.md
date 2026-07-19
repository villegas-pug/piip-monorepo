---
name: fe-implement-auth
description: Implementa autenticación Angular con Keycloak y PKCE para PIIP; úsala solo desde frontend-specialist para flujos aprobados de identidad.
---

# Implementar autenticación frontend

Usa OpenID Connect Authorization Code Flow con PKCE y redirección a Keycloak. No recopiles,
proceses ni almacenes contraseñas. Implementa inicialización, renovación o expiración de sesión,
interceptor de token y guards como experiencia de navegación, sin tratarlos como autorización
efectiva. No expongas secretos de cliente ni material de wallet.

Solicita aclaración sobre realm, client, redirect URIs y claims cuando no estén aprobados.
