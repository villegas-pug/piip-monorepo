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

## Versionar Objetivos PEI

`POST /objetivos-pei/versiones`

Entrada `ObjetivoPeiVersionRequest { codigoVersion, documentoAprobacionVersionId, vigenteDesde,
vigenteHasta?, objetivos[{ codigo, descripcion, vigenteDesde, vigenteHasta? }] }`.

Solo `GlobalAdmin`; `Idempotency-Key` obligatorio. El documento pertenece a un expediente
institucional y acredita la aprobación de planeamiento. No existen `PATCH` o `DELETE`: una corrección
o retiro crea otra versión. `GET /objetivos-pei/versiones` y
`GET /objetivos-pei/versiones/{id}` exponen historial autorizado.

## Versionar Actividades POI

`POST /actividades-poi/versiones`

Entrada `ActividadPoiVersionRequest { codigoVersion, documentoAprobacionVersionId, vigenteDesde,
vigenteHasta?, actividades[{ codigo, descripcion, vigenteDesde, vigenteHasta? }] }`. Aplica las
mismas reglas, pero su ciclo es independiente: una versión POI no altera PEI y viceversa.

Errores: `PEI_APPROVAL_REQUIRED`, `POI_APPROVAL_REQUIRED`, `PEI_APPROVAL_MISMATCH`,
`POI_APPROVAL_MISMATCH`, `PEI_VERSION_DUPLICATE`, `POI_VERSION_DUPLICATE`.

## Reglas y errores

- Nuevas selecciones solo admiten referencias vigentes; históricos siguen devolviendo su etiqueta
  aunque estén retirados.
- `422 PLANNING_REFERENCE_NOT_ACTIVE` cuando una confirmación usa una referencia retirada.
- Las semillas iniciales deben coincidir con versiones formalmente aprobadas; no se infieren valores
  ni se realiza sincronización externa.
- Toda consulta sensible por unidad se audita con asignación y ámbito efectivos.
