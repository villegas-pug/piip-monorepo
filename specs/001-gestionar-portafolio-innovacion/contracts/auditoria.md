# Contrato interno: Auditoría

El módulo `auditoria` no expone una API pública de exploración porque la especificación no aprueba
un actor para consultar el log transversal. Los historiales funcionales autorizados se exponen por
sus módulos propietarios.

## AuditService

Entrada conceptual `AuditCommand`:

```text
correlationId
actorId o identidadAnónimaMínima
asignacionEfectivaId, perfilEfectivo, unidadEfectiva
operacion, modulo, recursoTipo, recursoId
instante, resultado, codigoResultado
cambiosAntesDespues mínimos o referencia de evento
clasificacion
```

No admite contraseña, token, secreto, contenido documental, ruta física o datos personales no
necesarios.

## Semántica transaccional

- Cambio exitoso: evento insertado en la misma transacción Oracle que el cambio y su historial.
- Denegación o fallo previo a transacción de negocio: evento en transacción independiente para que
  sobreviva al rollback.
- Fallo de auditoría en una operación sensible impide confirmar el cambio; no se permite éxito sin
  evidencia.
- Los eventos son append-only desde el repositorio del módulo; no existe update/delete de negocio.

## Eventos mínimos

- acceso sensible permitido o denegado;
- creación, modificación y transición de iniciativa/proyecto;
- decisión y registro operativo, con ambos actores;
- carga, consulta de contenido, versión, clasificación y reclasificación;
- alta/baja de participante y sustitución de titular;
- creación, modificación, revocación y suplencia de asignación;
- aprovisionamiento, activación, desactivación, reactivación y fallo parcial Keycloak;
- incorporación, corrección, conflicto, resolución, validación o rechazo;
- generación, descarga, aprobación y remisión de reporte;
- validación, medición, aprobación o rechazo de prototipo;
- consulta/exportación institucional sensible.
- creación/versionado/inactivación de función y combinación de matriz por unidad concreta;
- registro independiente de versiones PEI y POI, incluida denegación por aprobación discordante;
- creación de expediente institucional, vinculación de serie y violación de propietario XOR;
- confirmación o denegación de publicación y exclusión pública posterior por reclasificación.
- ejecución o rechazo de la semilla inicial `GlobalAdmin`, con `sub`, función, unidad, Jefatura,
  aprobación de despliegue, DBA, fecha y resultado.

## Privacidad e integridad

El payload contiene identificadores y diferencias mínimas, se clasifica según el evento y se protege
del acceso ordinario. Una reclasificación no altera eventos anteriores. La inmutabilidad se verifica
mediante permisos del repositorio, ausencia de métodos de mutación y pruebas Oracle; no se inventan
triggers o grants institucionales en este plan.
