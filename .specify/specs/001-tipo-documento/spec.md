# Especificación: catálogo de tipos documentales

**Estado**: aprobada

## Propósito

Proveer un módulo de referencia funcional que permita consultar el catálogo canónico
`TIPO_DOCUMENTO` de PIIP sin modificar sus valores controlados.

## Alcance

- `GET /api/v1/tipo-documentos` devuelve el catálogo completo.
- `GET /api/v1/tipo-documentos/{id}` devuelve un tipo documental por identificador.
- Las respuestas exponen identificador, nombre, estado asociado, obligatoriedad, descripción,
  anexo normativo y condición de activo mediante DTOs.
- La consulta requiere un JWT de Keycloak con uno de los roles canónicos de PIIP.

## Fuera de alcance

- Crear, modificar o eliminar tipos documentales.
- Cambiar el DDL, los valores canónicos o los datos de `TIPO_DOCUMENTO`.
- Exponer entidades JPA, credenciales Oracle o configuración de Keycloak.

## Criterios de aceptación

1. El catálogo se consulta bajo `/api/v1` y no existen rutas heredadas de `taller`.
2. Una consulta por identificador inexistente devuelve `404` con una respuesta API estructurada.
3. Las consultas no autenticadas devuelven `401` y los JWT sin rol PIIP autorizado devuelven `403`.
4. El módulo no escribe en Oracle y no usa repositorios ni tablas de otros módulos.
