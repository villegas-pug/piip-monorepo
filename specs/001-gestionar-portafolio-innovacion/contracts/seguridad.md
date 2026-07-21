# Contrato: Seguridad y administración funcional

Base: `/api/v1/seguridad`.

## Contexto efectivo

### Listar asignaciones propias

`GET /me/asignaciones`

Devuelve `EffectiveAssignmentOption { id, perfil, unidad, cargoFuncion, inicio, fin, estadoEfectivo }`
para asignaciones utilizables o informativas del `sub`. El cliente selecciona una; el servidor
vuelve a validarla en cada petición.

## Usuarios

| Operación | Entrada | Salida | Autorización |
|---|---|---|---|
| `POST /usuarios` | `CreateUserRequest { correoInstitucional, nombreCompleto, unidadId }` | `202 ProvisioningResult { operacionId, usuarioId?, estado, recuperable }` | GlobalAdmin institucional o UnidadAdmin en unidad explícita. |
| `POST /usuarios/{id}/desactivaciones` | `UserStatusRequest { motivo }` | `UserStatusResult` | Misma regla; efecto inmediato en Keycloak y PIIP. |
| `POST /usuarios/{id}/reactivaciones` | `UserStatusRequest` | `UserStatusResult` | Autoridad que puede desactivar; no restaura asignaciones inválidas. |
| `GET /usuarios` | filtros `q`, unidad, activo y paginación | Página de `UserSummary` | Administrador dentro de ámbito. |

Creación, desactivación y reactivación exigen `Idempotency-Key`. Keycloak se aprovisiona primero; un
fallo Oracle deja operación recuperable o ejecuta compensación auditada. Nunca se acepta contraseña.

## Asignaciones

| Operación | Entrada | Salida |
|---|---|---|
| `POST /asignaciones` | `AssignmentRequest { usuarioId, perfil, unidadId, cargoFuncion, fechaInicio, fechaFin?, documentoFormalId? }` | `AssignmentDetail` |
| `PATCH /asignaciones/{id}` | Fechas/cargo permitidos y `If-Match` | `AssignmentDetail` |
| `POST /asignaciones/{id}/revocaciones` | `RevocationRequest { motivo, documentoFormalId? }` | `AssignmentDetail` |
| `GET /asignaciones` | usuario, perfil, unidad, vigencia, paginación | Página de `AssignmentSummary` |

Autorización:

- Solo GlobalAdmin administra `UnidadAdmin`.
- GlobalAdmin administra `Responsable` y `Consulta` institucionalmente; UnidadAdmin en su ámbito.
- `Evaluador` requiere autorización formal de la Oficina de Modernización y registro GlobalAdmin.
- `Autoridad` requiere designación formal y registro GlobalAdmin.
- `GlobalAdmin` nuevo requiere decisión de Autoridad y registro por GlobalAdmin, salvo bootstrap
  inicial formal cuando no exista ninguno.
- Modificar/revocar GlobalAdmin requiere otro GlobalAdmin y decisión formal; no se revoca el último
  activo sin reemplazo.

El contenido concreto de la matriz cargo/función-perfil-unidad es un gate pendiente. Hasta aprobarlo,
`POST/PATCH /asignaciones` no se habilita para operación ordinaria.

## Suplencias

| Operación | Entrada | Salida |
|---|---|---|
| `POST /asignaciones/{titularId}/suplencias` | `SubstitutionRequest { suplenteUsuarioId, inicio, fin, documentoFormalId }` | `SubstitutionDetail` |
| `POST /suplencias/{id}/terminaciones` | `EarlyTerminationRequest { motivo, documentoFormalId? }` | `SubstitutionDetail` |

La misma autoridad de la asignación permanente autoriza la suplencia. Inicio y fin son obligatorios;
se rechaza solape y se inactiva al titular equivalente durante el periodo. La terminación anticipada
solo la confirma la autoridad que autorizó.

## Sustitución de Responsable titular

`POST /registros/{registroId}/sustituciones-responsable`

Entrada `ResponsibleReplacementRequest { nuevoResponsableId, motivo }`; salida con titular anterior,
nuevo y vigencia. Solo UnidadAdmin del ámbito; transacción conserva exactamente uno vigente.

## Errores de negocio

- `403 ASSIGNMENT_SCOPE_DENIED`, `ASSIGNMENT_ADMIN_DENIED`.
- `409 IDENTITY_DUPLICATE`, `LAST_GLOBAL_ADMIN`, `SUBSTITUTION_OVERLAP`,
  `RESPONSIBLE_ALREADY_REPLACED`.
- `422 FORMAL_DOCUMENT_REQUIRED`, `ASSIGNMENT_MATRIX_NOT_APPROVED`, `INVALID_VALIDITY_PERIOD`.
- `503 KEYCLOAK_OPERATION_RECOVERABLE` con `operacionId`, sin detalles sensibles.

Todas las altas, cambios, revocaciones, suplencias, denegaciones y operaciones Keycloak generan
auditoría inmutable.
