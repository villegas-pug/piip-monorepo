# Contrato: Seguridad y administración funcional

Base: `/api/v1/seguridad`.

## Contexto efectivo

### Listar asignaciones propias

`GET /me/asignaciones`

Devuelve `EffectiveAssignmentOption { id, matrizCombinacionId, funcion, perfil, unidad, inicio, fin,
estadoEfectivo }`
para asignaciones utilizables o informativas del `sub`. El cliente selecciona una; el servidor
vuelve a validarla en cada petición.

## Usuarios

| Operación | Entrada | Salida | Autorización |
|---|---|---|---|
| `POST /usuarios` | `CreateUserRequest { correoInstitucional, nombreCompleto, unidadId }` | `202 ProvisioningResult { operacionId, usuarioId?, estado, recuperable }` | GlobalAdmin institucional o UnidadAdmin en unidad explícita. |
| `POST /usuarios/{id}/desactivaciones` | `UserStatusRequest { motivo }` | `UserStatusResult` | Misma regla; efecto inmediato en Keycloak y PIIP. |
| `POST /usuarios/{id}/reactivaciones` | `UserStatusRequest` | `UserStatusResult` | Autoridad que puede desactivar; no restaura asignaciones inválidas. |
| `GET /usuarios` | filtros `q`, unidad, activo y paginación | Página de `UserSummary` | Administrador dentro de ámbito. |
| `GET /usuarios/operaciones/{operacionId}` | Sin body | `ProvisioningResult` actualizado | Administrador que inició la operación o GlobalAdmin. |
| `POST /usuarios/operaciones/{operacionId}/reintentos` | Sin body; misma `Idempotency-Key` | `ProvisioningResult` | Misma autorización de creación; solo estado recuperable. |

Creación, desactivación y reactivación exigen `Idempotency-Key`. Keycloak se aprovisiona primero; un
fallo Oracle deja la identidad deshabilitada y una operación recuperable auditada para reintento sin
duplicados. Nunca se acepta contraseña.

Estados técnicos: `INICIADA`, `KEYCLOAK_CREADO_DESHABILITADO`, `ORACLE_PENDIENTE`, `COMPLETADA` y
`FALLIDA_NO_RECUPERABLE`. El reintento solo acepta `KEYCLOAK_CREADO_DESHABILITADO` u
`ORACLE_PENDIENTE`, revalida autorización y no crea otra identidad. Cada consulta, reintento y
transición queda auditado sin token o credenciales.

## Asignaciones

| Operación | Entrada | Salida |
|---|---|---|
| `POST /asignaciones` | `AssignmentRequest { usuarioId, matrizCombinacionId, fechaInicio, fechaFin?, documentoFormalVersionId? }` | `AssignmentDetail` |
| `PATCH /asignaciones/{id}` | Fechas permitidas y `If-Match`; función/perfil/unidad no se editan | `AssignmentDetail` |
| `POST /asignaciones/{id}/revocaciones` | `RevocationRequest { motivo, documentoFormalVersionId? }` | `AssignmentDetail` |
| `GET /asignaciones` | usuario, perfil, unidad, vigencia, paginación | Página de `AssignmentSummary` |

Autorización:

- Solo GlobalAdmin administra `UnidadAdmin`.
- GlobalAdmin administra `Responsable` y `Consulta` institucionalmente; UnidadAdmin en su ámbito.
- `Evaluador` requiere autorización formal de la Oficina de Modernización y registro GlobalAdmin.
- `Autoridad` requiere designación formal y registro GlobalAdmin.
- `GlobalAdmin` nuevo requiere decisión de Autoridad y registro por GlobalAdmin. La única excepción es
  la primera asignación creada fuera de la API por la semilla SQL manual 021.
- Modificar/revocar GlobalAdmin requiere otro GlobalAdmin y decisión formal; no se revoca el último
  activo sin reemplazo.

La función, el perfil y la unidad concreta se derivan de `matrizCombinacionId`; el comando no los
acepta por separado. La combinación debe estar vigente y su autoridad aprobadora, registrador y
documento formal deben corresponder al perfil.

La semilla 021 no tiene contrato HTTP: usa el `sub` proporcionado por OGTI, valores `MIDAGRI` y
`ADMINISTRADOR_PIIP`, aprobación de despliegue y auditoría mínima; aborta ante cualquier antecedente
`GlobalAdmin` o reejecución.

## Matriz función-perfil-unidad

| Operación | Regla |
|---|---|
| `GET /funciones` | Lista funciones vigentes e históricas autorizadas. |
| `GET /matrices/versiones` | Historial paginado de matrices. |
| `POST /matrices/versiones` | Crea versión, funciones y combinaciones función-perfil-unidad concreta. |
| `GET /matrices/versiones/{id}/combinaciones` | Devuelve combinaciones y vigencias. |
| `POST /matrices/combinaciones/{id}/inactivaciones` | Genera nueva versión; no sobrescribe históricos. |

`MatrixCombination { id, matrizVersionId, funcion, perfil, unidad, vigenteDesde, vigenteHasta?,
activa, documentoAprobacionVersionId }`.

Entrada `MatrixVersionRequest { codigoVersion, versionAnteriorId?, vigenteDesde, vigenteHasta?,
documentoAprobacionVersionId, funciones[{ codigo, descripcion }], combinaciones[{
funcionCodigo, perfil, unidadId, vigenteDesde, vigenteHasta?, documentoAprobacionVersionId }] }`.
Salida `MatrixVersionDetail` con funciones y combinaciones persistidas. La creación es idempotente.

La inactivación recibe `MatrixDeactivationRequest { codigoNuevaVersion,
documentoAprobacionVersionId, motivo }` y devuelve la nueva `MatrixVersionDetail`; nunca modifica la
versión anterior.

## Suplencias

| Operación | Entrada | Salida |
|---|---|---|
| `POST /asignaciones/{titularId}/suplencias` | `SubstitutionRequest { suplenteUsuarioId, inicio, fin, documentoFormalVersionId }` | `SubstitutionDetail` |
| `POST /suplencias/{id}/terminaciones` | `EarlyTerminationRequest { motivo, documentoFormalVersionId? }` | `SubstitutionDetail` |

La misma autoridad de la asignación permanente autoriza la suplencia. Inicio y fin son obligatorios;
se rechaza solape y se inactiva al titular equivalente durante el periodo. La terminación anticipada
solo la confirma la autoridad que autorizó.

## Autorización de sustitución de Responsable

La ruta pública pertenece a `portafolio`. Su servicio bloquea el agregado y, dentro de la misma
orquestación, invoca `AutorizacionEfectivaService` para revalidar que el actor sea `UnidadAdmin` del
ámbito inmediatamente antes de mutar. `seguridad` no accede a entidades o repositorios de portafolio.

## Errores de negocio

- `403 ASSIGNMENT_SCOPE_DENIED`, `ASSIGNMENT_ADMIN_DENIED`.
- `409 IDENTITY_DUPLICATE`, `LAST_GLOBAL_ADMIN`, `SUBSTITUTION_OVERLAP`,
  `RESPONSIBLE_ALREADY_REPLACED`.
- `422 FORMAL_DOCUMENT_REQUIRED`, `ASSIGNMENT_MATRIX_NOT_APPROVED`, `INVALID_VALIDITY_PERIOD`.
- `422 MATRIX_COMBINATION_NOT_ACTIVE`, `MATRIX_CONCRETE_UNIT_MISMATCH`,
  `MATRIX_APPROVAL_REQUIRED`, `MATRIX_APPROVER_INVALID`, `MATRIX_REGISTRAR_DENIED`.
- `503 KEYCLOAK_OPERATION_RECOVERABLE` con `operacionId`, sin detalles sensibles.

Todas las altas, cambios, revocaciones, suplencias, denegaciones y operaciones Keycloak generan
auditoría inmutable.
