# Contrato: Organización

Base: `/api/v1/organizacion`.

## Consultar unidades

`GET /unidades`

**Autorización**: cualquier asignación institucional efectiva con permiso de consulta. No amplía el
ámbito del usuario; para selectores de registro solo devuelve unidades en las que puede operar.

**Filtros**: `q`, `activa`, `page`, `size`, `sort=codigo|nombre`.

**Salida**: página de `UnidadOption { id, codigo, nombre, activa }`. La relación jerárquica puede
mostrarse como contexto, pero no implica autorización descendiente.

## Consultar Objetivos PEI

`GET /objetivos-pei`

**Filtros**: `q`, `vigenteEn`, `page`, `size`, `sort=codigo|descripcion`.

**Salida**: `PlaneamientoOption { id, codigo, descripcion, vigenteDesde, vigenteHasta, activo }`.

## Consultar Actividades POI

`GET /actividades-poi`

Mismo contrato de filtros y salida que Objetivos PEI.

## Reglas y errores

- Nuevas selecciones solo admiten referencias vigentes; históricos siguen devolviendo su etiqueta
  aunque estén retirados.
- `422 PLANNING_REFERENCE_NOT_ACTIVE` cuando una confirmación usa una referencia retirada.
- Los valores iniciales y operaciones de mantenimiento no se contratan todavía: dependen de una
  decisión funcional aprobada. No se expone CRUD de catálogos por inferencia.
- Toda consulta sensible por unidad se audita con asignación y ámbito efectivos.
