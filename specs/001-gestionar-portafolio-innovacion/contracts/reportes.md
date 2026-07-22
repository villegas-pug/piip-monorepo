# Contrato: Reportes institucionales

Base: `/api/v1/reportes`.

Todas las operaciones exigen perfil `Evaluador`, ámbito institucional autorizado y
`Idempotency-Key` en generaciones/remisiones.

## Reporte semestral

`POST /semestrales/generaciones`

Entrada `SemesterReportRequest { anio, semestre }`. `semestre=1` deriva periodo enero-junio y corte
30/06; `semestre=2`, julio-diciembre y corte 31/12. No se acepta una fecha de corte distinta.

Salida `202 ReportOperation { reporteId, operacionId, corte, estadoTecnico }`.

Contenido: totales por tipo, estado, unidad, fuente, tipo de solución, producto y cierre; indicadores
de admisibilidad, aprobación, cierre y cumplimiento de ciclos con numerador, denominador, porcentaje
o `noAplicable` cuando denominador es cero.

## Reporte extraordinario

`POST /extraordinarios/generaciones`

Entrada `OnDemandReportRequest { solicitudDocumentoId, aprobacionOficinaDocumentoId, periodo,
filtros }`. Filtros permitidos: tipo, estado, unidad, Responsable, fuente, tipo de solución y
producto. Los filtros no amplían el ámbito del generador.

Sin solicitud y aprobación documentadas: `422 REPORT_REQUEST_APPROVAL_REQUIRED`.

## Estado y archivos

| Ruta | Salida |
|---|---|
| `GET /generaciones/{id}` | Corte, parámetros, versión de datos, estado técnico, clasificación, hashes y errores recuperables. |
| `GET /generaciones/{id}/archivos/PDF` | PDF oficial autorizado. |
| `GET /generaciones/{id}/archivos/XLSX` | Detalle del mismo snapshot. |

PDF y XLSX provienen del mismo `snapshotId`, parámetros y versión. Clasificación `INTERNO` por
defecto y `RESTRINGIDO` si cualquier dato incluido lo es.

## Aprobación y remisión

| Operación | Entrada/regla |
|---|---|
| `POST /{id}/aprobaciones-remision` | Versión exacta, documento de aprobación y destinatarios. Oficina de Modernización. |
| `POST /{id}/remisiones` | Destinatarios previamente aprobados y resultado de remisión manual. |

Destinatarios: autoridades MIDAGRI, Oficina de Modernización y PCM-SGP cuando esté autorizado. El
contrato registra la remisión; no implementa correo, PIDE ni sincronización externa.

## Fallos y conservación

- Un fallo parcial conserva solicitud, snapshot, parámetros, intento y resultado; el reintento con la
  misma clave no duplica reporte.
- No se remite una versión distinta de la aprobada: `409 REPORT_VERSION_NOT_APPROVED`.
- `403 REPORT_SCOPE_DENIED` y `REPORT_CLASSIFICATION_DENIED` protegen ámbito y clasificación.
- Sin plazo de retención confirmado no existe endpoint de eliminación ni purga automática.
- La Fase 1 tampoco expone disposición manual; cualquier eliminación futura requiere otra
  especificación aprobada y la tabla de retención vigente.
- Generación, descarga institucional, aprobación, remisión, fallo y denegación se auditan.
