# Quickstart local verificable: Fase 1 PIIP

Esta guía describe la validación local esperada después de implementar el plan. No implica que el
código, los scripts o las pruebas existan todavía. Ningún comando fue ejecutado al generar este
artefacto.

## Gates previos

No iniciar BUILD o corte de la capacidad afectada sin:

1. primera versión PEI y primera versión POI con semillas y aprobaciones formales independientes;
2. matriz inicial con combinaciones función-perfil-unidad concreta aprobadas;
3. documentos aprobatorios en expedientes institucionales.

US9 y sus gates de prototipos, mediciones y metas están diferidos a una fase posterior por la
enmienda constitucional 5.0.0; no bloquean los recorridos de la Fase 1 actual.

Antes de usar Oracle, una persona debe revisar y ejecutar manualmente los incrementos activos del
[plan](./plan.md); 018 está diferido. `database/database-schema.md` solo se actualiza después de una
confirmación humana de éxito.

## Prerrequisitos locales

- Java 21 y Maven.
- Node.js compatible con Angular 22 y npm 11.
- Oracle 19c+ local o instancia autorizada para desarrollo, nunca una base compartida sin permiso.
- Keycloak 26 compatible con cliente público OIDC y PKCE aprobado.
- Directorio local privado para documentos, fuera de rutas públicas del frontend.
- Variables de entorno, sin guardar secretos en archivos versionados:

```bash
export PROFILE=dev
export ORACLE_DEV_URL='jdbc:oracle:thin:@...'
export ORACLE_DEV_USERNAME='...'
export ORACLE_DEV_PASSWORD='...'
export ORACLE_DEV_TNS_ADMIN='/ruta/local/wallet'
export KEYCLOAK_ISSUER_URI='https://.../realms/piip'
export PIIP_JWT_AUDIENCE='ms-piip'
export PIIP_DOCUMENT_STORAGE_ROOT='/ruta/local/piip-documents'
```

La configuración actual debe migrarse a estas variables antes de ejecutar: hoy contiene destino y
credenciales Oracle, issuer y audience fijos, contexto `/api/ms-piip/v1` y límite multipart de 10 MB.
El objetivo es `/api/v1` y 100 MB inclusivos.

## Inicio local futuro

Backend, desde `apps/backend/business-domain/`:

```bash
mvn -pl ms-piip -am spring-boot:run
```

Frontend, desde `apps/frontend/`:

```bash
npm start
```

Resultados esperados:

- Backend en `http://localhost:4001/api/v1`.
- OpenAPI en la ruta Springdoc configurada bajo el contexto `/api/v1`.
- Consulta pública accesible sin token.
- Rutas institucionales redirigen a Keycloak y usan Authorization Code con PKCE.
- PIIP nunca presenta ni registra una contraseña.

## Datos de validación

Usar solo datos sintéticos:

- unidades A y B sin herencia de permisos;
- usuarios para cada perfil canónico;
- una asignación vigente, una vencida, una revocada y una suplencia;
- referencias PEI/POI aprobadas para pruebas;
- archivos PDF/OOXML/JPEG/PNG de menos de 100 MB, exactamente 104857600 bytes y 104857601 bytes;
- decisiones y evidencias sintéticas sin firmas, correos o documentos personales reales.

No se configura test double ni estado antimalware: OGTI administra ese control fuera de PIIP.

## Escenarios verificables

### 1. Autorización efectiva

1. Autenticar una persona con dos asignaciones en unidades distintas.
2. Seleccionar solo una mediante `X-Asignacion-Efectiva-Id`.
3. Crear/consultar dentro de esa unidad y comprobar éxito.
4. Intentar usar el mismo contexto en otra unidad y comprobar `403` auditado.
5. Revocar la asignación entre validación y commit; comprobar rechazo, rollback y auditoría.

### 2. Iniciativa y proyecto derivado

1. Presentar una iniciativa completa; comprobar código, fecha automática, `PRESENTADO` y código de
   origen vacío.
2. Aprobarla con opinión y decisión formal, distinguiendo decisor y registrador.
3. Crear un proyecto derivado; comprobar código propio, `PROYECTO_EJECUCION`, código de origen y
   vínculo inmutable.
4. Repetir con otra `Idempotency-Key`; comprobar `409 DERIVED_PROJECT_EXISTS`.
5. Verificar que la iniciativa continúa `INICIATIVA_APROBADA`.

### 3. Máquina de estados

Probar cada transición de [data-model.md](./data-model.md) con rol, unidad, documento y observación
correctos. Intentar una transición no listada, una salida desde `SUSPENDIDO` y dos decisiones
concurrentes; deben rechazarse sin sobrescribir historial.

### 4. Campos oficiales

1. Omitir cada campo obligatorio y verificar error preciso.
2. Probar máximos 500/2000/1000, trim y contenido solo espacios.
3. Probar `OTROS` sin detalle y componente digital Sí sin descripción.
4. Abrir la única subsanación y modificar solo campos 5-12, 22 y 23.
5. En ejecución, modificar solo 17, 19 y 23.

### 5. Ciclos, producto y cierre

1. Crear ciclos quincenales completos con evidencias.
2. Intentar cerrar uno incompleto; comprobar `422 CYCLE_INCOMPLETE`.
3. Corregir un ciclo cerrado y comprobar nueva versión.
4. Recorrer aprobación y no aprobación de producto.
5. Desde ambos estados, cerrar con resultados validados, informe, aprendizajes, conclusión y
   observación; comprobar `FINALIZADO` y fecha automática.

### 6. Documentos de 100 MB

| Tamaño | Esperado |
|---:|---|
| 104857599 bytes | Aceptado si MIME válido; conserva BLOB y SHA-256. |
| 104857600 bytes | Aceptado; el límite es inclusivo. |
| 104857601 bytes | `413 DOCUMENT_TOO_LARGE`; sin metadato formalizado. |

Probar además MIME no permitido, hash calculado, clasificación pendiente, validación, reclasificación
restrictiva y nueva versión. Solo una clasificación validada permite usarlo como evidencia.

### 7. Consulta pública

1. Consultar anónimamente un registro con información interna/restringida.
2. Comprobar que solo se reciben tipo, código, nombre y estado.
3. Como Evaluador, confirmar una versión `PUBLICO` y validada; comprobar fecha del servidor.
4. Verificar que se muestran solo tipo, título público, versión, formato y fecha de publicación.
5. Comprobar exclusión de versiones no públicas, no confirmadas o con título personal.
6. Comprobar que no existe URL, endpoint o acción de descarga pública.
7. Reclasificar de forma restrictiva y verificar que desaparece de consultas futuras sin perder la
   publicación ni su auditoría.
8. Verificar que la consulta institucional sí revalida ámbito y clasificación.

### 8. Usuarios, suplencia y recuperación

1. Aprovisionar identidad y usuario con la misma clave dos veces; debe existir uno.
2. Simular Keycloak exitoso y Oracle fallido; comprobar identidad deshabilitada y operación
   recuperable auditada sin duplicados.
3. Desactivar en ambos sistemas y comprobar bloqueo inmediato.
4. Reactivar y comprobar que asignaciones vencidas/revocadas no vuelven.
5. Crear una suplencia y rechazar otra superpuesta; durante su vigencia el titular equivalente no
   opera.
6. Intentar revocar el último GlobalAdmin sin reemplazo; comprobar `409 LAST_GLOBAL_ADMIN`.
7. Verificar por prueba SQL que 021 crea una sola vez `MIDAGRI`, `ADMINISTRADOR_PIIP`, combinación,
   usuario por `sub`, asignación y auditoría, y aborta ante reejecución o antecedente `GlobalAdmin`.

### 9. Matriz función-perfil-unidad

1. Registrar una versión con funciones y combinaciones de unidad concreta respaldadas por aprobación.
2. Rechazar autoridad o administrador registrador incorrectos.
3. Crear una asignación enviando únicamente `matrizCombinacionId` y fechas.
4. Comprobar que función, perfil y unidad derivan de la combinación y que una combinación de unidad A
   no autoriza en unidad B.
5. Inactivar mediante una nueva versión; rechazar nuevas asignaciones y conservar históricos.

### 10. Catálogos PEI y POI

1. Registrar por `GlobalAdmin` una versión PEI con aprobación formal de planeamiento.
2. Registrar de forma independiente una versión POI y comprobar que no altera la versión PEI.
3. Publicar otra versión PEI y comprobar que no cambia POI.
4. Retirar referencias mediante nuevas versiones; excluirlas de selectores y conservarlas en
   detalles históricos.
5. Rechazar ítems que no coinciden con el documento y semilla aprobados.

### 11. Expedientes institucionales

1. Crear un expediente para la aprobación de una versión PEI o matriz.
2. Cargar una serie con ese expediente como único propietario.
3. Rechazar un tipo documental de portafolio usado en expediente y viceversa.
4. Rechazar propietario ausente, simultáneo o cambio posterior de propietario.
5. Verificar que el documento valida el caso de uso y nunca aparece en consulta pública.

### 12. Incorporación individual

Registrar un expediente `PENDIENTE`, corregirlo varias veces, bloquear duplicado/código/relación,
resolver como Evaluador y validar o rechazar. Un duplicado confirmado debe vincular evidencia al
registro existente y no crear otro.

### 13. Reportes

1. Generar semestre 1 y 2 con cortes 30/06 y 31/12.
2. Verificar BR-122, incluido denominador cero.
3. Comprobar que PDF y XLSX comparten snapshot, parámetros y versión.
4. Simular fallo de un formato y reintentar sin duplicar expediente.
5. Aprobar versión/destinatarios y registrar remisión manual.
6. Comprobar clasificación `RESTRINGIDO` si el snapshot contiene ese nivel.

### 14. Prototipos (diferido)

US9 se conserva para una fase posterior y no forma parte de la validación local actual.

## Verificación automatizada futura

Con autorización expresa para ejecutar pruebas:

Backend, desde `apps/backend/business-domain/`:

```bash
mvn -pl ms-piip -am test
```

Frontend unitario, desde `apps/frontend/`:

```bash
npm test -- --watch=false
```

Playwright, después de incorporarlo al proyecto:

```bash
npx playwright test
```

La suite debe cubrir la matriz de pruebas de [plan.md](./plan.md), usar Oracle Testcontainers y
alcanzar al menos 80 % de cobertura en código de negocio. No usar una omisión silenciosa de Docker
como evidencia de aprobación.

## Criterio de salida local

- Constitution Check final sin bloqueos de la capacidad implementada.
- Scripts ejecutados manualmente y catálogo confirmado.
- Contratos OpenAPI concordantes con estos documentos.
- Versiones iniciales PEI/POI, matriz y expedientes respaldados por aprobaciones formales.
- Auditoría presente para éxito, denegación y fallo parcial.
- US9 y sus gates de prototipos están diferidos; no condicionan las interfaces de la Fase 1 actual.
- Todas las pruebas autorizadas aprobadas, sin exposición o descarga documental pública.
