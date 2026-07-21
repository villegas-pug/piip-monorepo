# Contrato: Consulta institucional y pública

Base: `/api/v1/consulta`.

## Consulta institucional

### Buscar portafolio

`GET /institucional/portafolio`

Autorización: `Consulta` u otro perfil con permiso, siempre con una asignación efectiva. Filtros:
`tipo`, `codigo`, `nombre`, `estado`, `unidadId`, `responsableId`, `fechaDesde`, `fechaHasta`,
`page`, `size` y `sort=codigo|nombre|estado|fechaInicio`.

Salida paginada `InstitutionalPortfolioSummary` con campos clasificados permitidos por ámbito. El
servidor no devuelve filas de unidades no asignadas explícitamente.

### Ver detalle

`GET /institucional/portafolio/{id}`

Salida `InstitutionalPortfolioDetail` con campos permitidos, relación iniciativa-proyecto, unidades,
estado, resultados, documentos autorizados e historial. Personas participantes solo se incluyen si
el actor es Responsable del registro, Evaluador o administrador autorizado.

### Contenido documental

Se consulta exclusivamente por `GET /api/v1/documentos/{id}/contenido`, que revalida ámbito,
clasificación y estado `LIMPIO`. La respuesta de detalle no contiene `storageKey` o URL directa.

## Consulta pública

### Buscar

`GET /publica/portafolio`

Anónimo. Filtros allowlist: `tipo`, `codigo`, `nombre`, `estado`, `page`, `size` y
`sort=codigo|nombre|estado`. Salida `PublicPortfolioSummary { tipoRegistro, codigo, nombre, estado }`.

### Ver detalle público

`GET /publica/portafolio/{codigo}`

Devuelve los mismos cuatro campos. No devuelve Responsable, unidad, descripción, resultados,
participantes, historial interno o documentos.

Cuando se apruebe el actor/evento de publicación, podrá añadirse
`documentos: PublicDocumentMetadata[] { tipo, titulo, version, formato, fechaPublicacion }` solo para
versiones publicadas cuyo título no contenga datos personales. Hasta entonces `documentos` se omite o
permanece vacío. Esta ampliación requiere actualizar formalmente el contrato.

## Prohibiciones y auditoría

- No existe `/publica/documentos/{id}/contenido`, descarga, URL firmada ni redirección a storage.
- Una reclasificación más restrictiva afecta la siguiente petición o descarga institucional.
- Recurso fuera de ámbito se responde como no visible sin confirmar su existencia.
- Consultas/exportaciones institucionales sensibles se auditan. La consulta pública registra solo
  métricas técnicas mínimas, sin datos personales innecesarios.
