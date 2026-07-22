# Contrato: Documentos y evidencias

Base común: `/api/v1`.

## Cargar documento

`POST /documentos`

Multipart:

- `file`: PDF, Office Open XML, JPEG o PNG; tamaño `1..104857600` bytes.
- `metadata`: `UploadDocumentRequest { owner, tipoDocumentoId, titulo, clasificacionPropuesta, uso }`.

`owner` es `DocumentOwner { tipo: PORTAFOLIO | EXPEDIENTE_INSTITUCIONAL,
registroPortafolioId?, expedienteInstitucionalId? }`. Exactamente un identificador corresponde al
tipo. `tipoDocumentoId` debe tener el mismo contexto que el propietario. Una serie y todas sus
versiones conservan ese propietario de forma inmutable.

Autorización: actor habilitado por el caso de uso propietario, sea registro de portafolio o expediente
institucional. `Idempotency-Key` obligatorio. El servidor valida permiso sobre el propietario,
tamaño/MIME real, calcula SHA-256 y almacena el BLOB Oracle; no confía en hash del cliente.

Salida `201 DocumentVersionDetail { documentoId, serieId, version, titulo, formato, tamanoBytes,
hashSha256, clasificacionPropuesta, clasificacionValidada, aptaComoEvidencia, etag }`.

Persistencia: `DOCUMENTO_SERIE` representa el documento lógico y cada fila de la tabla `DOCUMENTO`
representa una versión con BLOB Oracle. No se crea una tabla paralela `DOCUMENTO_VERSION`.

## Crear una versión

`POST /documentos/{serieId}/versiones`

Mismo multipart; `{serieId}` identifica `DOCUMENTO_SERIE`, exige motivo, `If-Match` de la última
versión y registra `versionAnteriorId` con el identificador exacto de esa fila `DOCUMENTO`. Nunca
sobrescribe o elimina. Un documento formalizado solo admite otra versión trazable.

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
| `GET /documentos/{id}` | Metadatos de una versión permitidos por ámbito y clasificación. |
| `GET /documentos/series/{serieId}/versiones` | Historial paginado autorizado de la serie. |
| `GET /documentos/{id}/contenido` | Stream del BLOB de una versión institucional solo si la clasificación está validada y el ámbito permitido. |

No existe ruta pública de contenido, descarga, URL firmada o `storageKey`.

## Publicación

`POST /documentos/{versionId}/publicaciones`

Solo Evaluador; exige `Idempotency-Key` e `If-Match`. Entrada
`PublishDocumentMetadataRequest { tituloPublico }`, sin fecha proporcionada por el cliente. La versión
debe pertenecer a un propietario `PORTAFOLIO`, tener clasificación `PUBLICO` validada y título sin
datos personales. Un propietario `EXPEDIENTE_INSTITUCIONAL` se rechaza. Salida
`PublicDocumentMetadata { tipo, titulo, version, formato, fechaPublicacion }`, donde la fecha proviene
del servidor. La confirmación es append-only y no crea contenido, URL o descarga pública.

Errores: `PUBLIC_CLASSIFICATION_REQUIRED`, `CLASSIFICATION_NOT_VALIDATED`,
`PUBLIC_METADATA_CONTAINS_PERSONAL_DATA`.

## Expedientes institucionales

| Operación | Regla |
|---|---|
| `POST /expedientes-institucionales` | Crea expediente para documentos formales sin portafolio. |
| `GET /expedientes-institucionales/{id}` | Detalle autorizado. |
| `GET /expedientes-institucionales/{id}/documentos` | Series y versiones autorizadas. |

`CreateInstitutionalFileRequest { asunto, moduloOrigen, referenciaCasoUso, unidadId?, clasificacion }`.
El backend autoriza creación y consulta mediante el permiso y ámbito del módulo/caso de uso indicado;
el cliente no obtiene permisos por declarar `moduloOrigen`. Crear el expediente no concede por sí
mismo permiso documental. Errores:
`DOCUMENT_OWNER_REQUIRED`, `DOCUMENT_OWNER_XOR_VIOLATION`, `DOCUMENT_OWNER_IMMUTABLE`,
`INSTITUTIONAL_FILE_SCOPE_DENIED`, `FORMAL_DOCUMENT_FILE_MISMATCH`.

Los casos de uso se exponen mediante `ExpedienteInstitucionalService` y `DocumentoService`; los
controladores solo validan y delegan. Ambos contratos usan DTO y nunca exponen entidades o claves de
almacenamiento.

## Fallos y compensación

- `413 DOCUMENT_TOO_LARGE` para 104857601 bytes o más.
- `415 DOCUMENT_TYPE_NOT_ALLOWED` para formato real no permitido.
- `422 CLASSIFICATION_NOT_VALIDATED`.
- `403 DOCUMENT_CLASSIFICATION_DENIED` sin revelar contenido.
- Fallo al persistir elimina solo temporal no referenciado; fallo después de confirmar se registra y
  recupera, nunca elimina una versión formal.

Carga, versión, consulta de contenido, validación, reclasificación y denegación se auditan.

OGTI administra fuera de PIIP el análisis, bloqueo, cuarentena y respuesta antimalware de los BLOB
Oracle. Este contrato no expone estados, resultados, informes ni operaciones antimalware.
