// Recorrido de la US6 (administración de seguridad institucional).
//
// El recorrido se carga perezosamente desde `app.routes.ts` mediante
// `loadChildren: () => import('./seguridad.routes').then(m => m.SEGURIDAD_ROUTES)`.
// Ninguno de los componentes ni servicios del recorrido debe incluirse en
// el bundle inicial: por eso todas las rutas usan `loadComponent`.
//
// Decisiones de diseño:
//   - Componentes standalone (sin NgModule), uno por sub-ruta, alineados
//     con los contratos de `contracts/seguridad.md`: usuarios, matriz
//     función-perfil-unidad, asignaciones y suplencias.
//   - `authGuard` protege cada ruta: la sesión institucional la gestiona
//     el backend, pero la UX exige evitar navegación a formularios sin
//     login. Sin `authGuard` no se exponen formularios institucionales.
//   - `effectiveAssignmentGuard` mejora la experiencia mostrando el
//     selector de asignación efectiva cuando el usuario no ha elegido
//     uno. NO es autoridad local: la correspondencia entre unidad,
//     perfil y recurso la confirma el backend en cada llamada y la
//     transporta `effectiveAssignmentInterceptor` mediante el encabezado
//     `X-Asignacion-Efectiva-Id`. La invalidación tras revocación o
//     sustitución la gestiona `EffectiveAssignmentService` (T093) y se
//     documenta por ruta.
//   - `title` por ruta para WCAG 2.1 AA (lectores de pantalla y
//     orientación). `data.breadcrumb` queda preparado para integrar un
//     sistema de migas institucional sin acoplar el routing a un
//     componente específico.
//   - Sin permisos locales: este módulo no consulta roles, no decide
//     autoridad ni filtra operaciones; la matriz y el catálogo de
//     funciones los carga el servicio desde el backend.
//   - El segmento vive como hermano de `portafolio/*` y `consulta-*`.
//     Las rutas internas no comparten prefijos: cada sub-pantalla es
//     un segmento independiente y no colisiona con los demás.
//
// Discrepancia documentada (`NEEDS CLARIFICATION`): el snapshot
// OpenAPI codigo-first de los endpoints de suplencias aún no incluye
// `POST /api/v1/seguridad/asignaciones/{titularId}/suplencias` ni
// `POST /api/v1/seguridad/suplencias/{id}/terminaciones`. La ruta
// de `substitution-administration` se conserva de todos modos porque
// el backend `SuplenciaController` ya está implementado; la
// regeneración del snapshot OpenAPI corresponde al backend y se
// registrará en el handoff.

import { Routes } from '@angular/router';

import { authGuard } from '../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../core/effective-assignment/effective-assignment.guard';

const COMMON_GUARDS = [authGuard, effectiveAssignmentGuard] as const;

/**
 * Catálogo canónico de la US6 para la administración de seguridad
 * institucional. Cuatro pantallas, una por sub-capacidad:
 *   * `usuarios`: aprovisionamiento, consulta, reintento, desactivación
 *     y reactivación. Las revocaciones de usuarios NO invalidan el
 *     contexto de asignación efectiva porque no tocan la combinación
 *     función-perfil-unidad.
 *   * `matriz`: versiones inmutables de función-perfil-unidad y
 *     combinaciones vigentes. La inactivación por nueva versión NO
 *     invalida el contexto local: la verificación de la combinación
 *     efectiva concreta la hace el backend en cada llamada sensible.
 *   * `asignaciones`: alta, cambio de vigencia y revocación inmediata.
 *     La revocación SÍ puede afectar a la asignación efectiva del
 *     usuario actual; `EffectiveAssignmentService` la invoca para
 *     limpiar el contexto si coincide.
 *   * `suplencias`: creación temporal sin solape y terminación
 *     anticipada. La terminación SÍ puede afectar a la asignación
 *     titular del usuario actual; `EffectiveAssignmentService` la
 *     invoca para limpiar el contexto si coincide.
 */
export const SEGURIDAD_ROUTES: Routes = [
  {
    path: 'usuarios',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Administración de usuarios',
    data: { breadcrumb: 'Usuarios' },
    loadComponent: () =>
      import('./usuarios/user-administration.component').then(
        ({ UserAdministrationComponent }) => UserAdministrationComponent
      )
  },
  {
    path: 'matriz',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Matriz función-perfil-unidad',
    data: { breadcrumb: 'Matriz función-perfil-unidad' },
    loadComponent: () =>
      import('./matriz-funcion-perfil-unidad/matrix-administration.component').then(
        ({ MatrixAdministrationComponent }) => MatrixAdministrationComponent
      )
  },
  {
    path: 'asignaciones',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Administración de asignaciones',
    data: { breadcrumb: 'Asignaciones' },
    loadComponent: () =>
      import('./asignaciones/assignment-administration.component').then(
        ({ AssignmentAdministrationComponent }) => AssignmentAdministrationComponent
      )
  },
  {
    path: 'suplencias',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Administración de suplencias',
    data: { breadcrumb: 'Suplencias' },
    loadComponent: () =>
      import('./suplencias/substitution-administration.component').then(
        ({ SubstitutionAdministrationComponent }) => SubstitutionAdministrationComponent
      )
  }
];
