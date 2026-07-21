# Contrato: Documentos y evidencias

Base: `/api/v1/documentos`.

## Cargar documento

`POST /documentos`

Multipart:

- `file`: PDF, Office Open XML, JPEG o PNG; tamaño `1..104857600` bytes.
- `metadata`: `UploadDocumentRequest { registroId, tipoDocumentoId, titulo,
  clasificacionPropuesta, uso }`.

Autorización: actor habilitado en el registro. `Idempotency-Key` obligatorio. El servidor valida
tamaño/MIME real, calcula SHA-256 y crea versión en `PENDIENTE`; no confía en hash del cliente.

Salida `201 DocumentVersionDetail { documentoId, serieId, version, titulo, formato, tamanoBytes,
hashSha256, estadoAntimalware, clasificacionPropuesta, clasificacionValidada, aptaComoEvidencia,
etag }`.

## Crear una versión

`POST /documentos/{id}/versiones`

Mismo multipart; exige motivo y referencia la versión anterior. Nunca sobrescribe o elimina. Un
documento formalizado solo admite otra versión trazable.

## Estado antimalware

El resultado ingresa por un contrato interno autenticado del módulo, no por una ruta pública. Solo
`LIMPIO` puede ser evidencia. `PENDIENTE` o `INFECTADO` devuelve `422 EVIDENCE_NOT_ELIGIBLE` en
comandos de negocio.

## Validar clasificación inicial

`POST /documentos/{id}/validaciones-clasificacion`

Entrada `ClassificationValidationRequest { clasificacion, metadatosPublicables[], observacion? }`.
Solo Evaluador. Hasta confirmar, únicamente cargador y Evaluador pueden consultar y el documento no
sirve como evidencia.

## Reclasificar

`POST /documentos/{id}/reclasificaciones`

Entrada `ReclassificationRequest { nuevaClasificacion, documentoDecisionId, motivo }`. Autoridad
decide y Evaluador registra. Se conserva clasificación anterior, decisor, registrador, documento,
momento y resultado. Efecto inmediato sobre accesos posteriores.

## Consultar metadatos y contenido

| Ruta | Salida/regla |
|---|---|
| `GET /documentos/{id}` | Metadatos permitidos por ámbito y clasificación. |
| `GET /documentos/{id}/versiones` | Historial paginado autorizado. |
| `GET /documentos/{id}/contenido` | Stream institucional solo si `LIMPIO`, clasificación validada y ámbito permitido. |

No existe ruta pública de contenido, descarga, URL firmada o `storageKey`.

## Publicación

El modelo admite metadatos publicados, pero no se contrata un comando de publicación hasta aprobar
actor y evento que fijan `fechaPublicacion`. En tanto el gate siga pendiente, ningún metadato
documental se devuelve por consulta pública.

## Fallos y compensación

- `413 DOCUMENT_TOO_LARGE` para 104857601 bytes o más.
- `415 DOCUMENT_TYPE_NOT_ALLOWED` para formato real no permitido.
- `422 CLASSIFICATION_NOT_VALIDATED`, `MALWARE_SCAN_PENDING`, `DOCUMENT_INFECTED`.
- `403 DOCUMENT_CLASSIFICATION_DENIED` sin revelar contenido.
- Fallo al persistir elimina solo temporal no referenciado; fallo después de confirmar se registra y
  recupera, nunca elimina una versión formal.

Carga, versión, consulta de contenido, validación, reclasificación y denegación se auditan.
