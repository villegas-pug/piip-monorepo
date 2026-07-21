# Contratos de capacidades PIIP

Estos contratos son el insumo de OpenAPI 3.0 para `/api/v1`. Definen límites funcionales y no
exponen entidades JPA, tablas, claves de almacenamiento ni detalles de Keycloak.

## Convenciones comunes

### Autenticación y asignación efectiva

- Rutas institucionales: `Authorization: Bearer <token>` y
  `X-Asignacion-Efectiva-Id: <uuid|id-opaco>`.
- El backend valida emisor, audiencia, firma, vigencia y `sub`, carga la asignación desde Oracle y
  comprueba usuario, perfil, unidad, vigencia, revocación e inactividad.
- Una petición usa una sola asignación. Perfil y unidad incluidos en un body son datos del caso de
  uso, nunca autoridad del solicitante.
- El backend revalida la asignación inmediatamente antes de aplicar una operación sensible.
- Rutas públicas bajo `/consulta/publica` son anónimas y no aceptan contexto institucional.

### Idempotencia y concurrencia

- `Idempotency-Key` obligatorio en `POST` que crea recursos, transiciones, versiones documentales,
  aprovisionamientos o reportes. Misma clave y payload devuelve el resultado original; misma clave
  y payload distinto devuelve `409 IDEMPOTENCY_KEY_REUSED`.
- Recursos mutables devuelven `ETag`; `PATCH` y comandos que dependen de estado exigen `If-Match`.
- Versión desactualizada devuelve `412 STALE_VERSION`. Una transición concurrente ya confirmada
  devuelve `409 STATE_CHANGED` y no modifica historial.

### Paginación y filtros

- `page`: base cero, por defecto 0.
- `size`: por defecto 20, máximo 100.
- `sort`: solo campos allowlist del contrato, con desempate estable por identificador.
- Respuesta: `items`, `page`, `size`, `totalElements`, `totalPages`.
- Filtros fuera del ámbito nunca amplían resultados; el servidor aplica primero autorización y
  clasificación.

### Errores

Content-Type `application/problem+json`:

```json
{
  "type": "https://piip.midagri.gob.pe/problems/STATE_TRANSITION_NOT_ALLOWED",
  "title": "Transición no permitida",
  "status": 409,
  "code": "STATE_TRANSITION_NOT_ALLOWED",
  "detail": "La operación no puede completarse en el estado actual.",
  "instance": "/api/v1/portafolio/proyectos/123/transiciones",
  "correlationId": "01J...",
  "violations": []
}
```

| HTTP | Uso |
|---:|---|
| 400 | JSON, multipart, filtro, formato o Bean Validation inválido. |
| 401 | Token ausente o inválido en ruta institucional. |
| 403 | Perfil, ámbito, clasificación o asignación no efectiva. |
| 404 | Recurso no visible o inexistente. |
| 409 | Estado, duplicado, cardinalidad, idempotencia o carrera de unicidad. |
| 412 | `If-Match` desactualizado. |
| 413 | Archivo mayor de 104857600 bytes. |
| 415 | MIME no permitido. |
| 422 | Precondición funcional o evidencia incompleta. |
| 503 | Dependencia local/Keycloak/almacenamiento no disponible; operación recuperable. |

No se revela en errores la existencia de un recurso fuera del ámbito, datos personales, contenido
documental, tokens, credenciales o rutas físicas.

## Contratos

| Archivo | Capacidad |
|---|---|
| [organizacion.md](./organizacion.md) | Unidades y referencias PEI/POI vigentes. |
| [seguridad.md](./seguridad.md) | Usuarios, asignaciones, revocaciones y suplencias. |
| [portafolio.md](./portafolio.md) | Iniciativas, evaluación, proyectos, seguimiento y cierre. |
| [documentos.md](./documentos.md) | Carga, versiones, seguridad y clasificación documental. |
| [reportes.md](./reportes.md) | Generación, aprobación y remisión. |
| [consulta.md](./consulta.md) | Consulta institucional y pública. |
| [prototipos.md](./prototipos.md) | Prototipos, validaciones, mediciones y metas. |
| [auditoria.md](./auditoria.md) | Contrato interno de evidencia inmutable. |

## Privacidad de DTO

- `PublicPortfolioSummary`: solo tipo, código, nombre y estado.
- `PublicDocumentMetadata`: tipo, título sin datos personales, versión, formato y fecha de
  publicación, únicamente después de resolver el gate de publicación.
- `InstitutionalPortfolioDetail`: campos permitidos por ámbito y clasificación.
- `RestrictedParticipant`: solo Responsable del registro, Evaluador o administrador autorizado.
- Nunca se reutiliza un DTO institucional en la consulta pública mediante ocultamiento tardío.

## Transacciones y auditoría

Cada comando sensible confirma en una transacción el cambio, historial y evento de auditoría. Las
denegaciones se auditan en una transacción independiente. Las operaciones con Keycloak o filesystem
usan idempotencia y compensación porque no comparten la transacción Oracle.
