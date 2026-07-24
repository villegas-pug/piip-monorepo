// Recorrido de la US1 (presentación, subsanación, incorporación y corrección).
//
// El recorrido se carga perezosamente desde `app.routes.ts` mediante
// `loadChildren: () => import('./registro.routes').then(m => m.REGISTRO_ROUTES)`.
// Ninguno de los componentes ni servicios del recorrido debe incluirse en el
// bundle inicial: por eso todas las rutas usan `loadComponent`.
//
// Decisiones de diseño:
//   - Componentes standalone (sin NgModule), uno por ruta.
//   - `authGuard` protege cada ruta: la sesión institucional la gestiona el
//     backend, pero la UX exige evitar navegación a formularios sin login.
//   - `effectiveAssignmentGuard` mejora la experiencia mostrando el selector de
//     asignación efectiva cuando el usuario no ha elegido uno. NO es autoridad
//     local: la correspondencia entre unidad, perfil y recurso la confirma el
//     backend en cada llamada y la transporta `effectiveAssignmentInterceptor`
//     mediante el encabezado `X-Asignacion-Efectiva-Id`.
//   - `title` por ruta para WCAG 2.1 AA (lectores de pantalla y orientación).
//   - `data.breadcrumb` queda preparado para integrar un sistema de migas
//     institucional sin acoplar el routing a un componente específico.
//   - Las rutas de edición y corrección reusan los componentes de creación. La
//     distinción entre crear y editar la gestiona el componente leyendo el
//     `route.paramMap` (`:id`) y/o `route.data` cuando `withComponentInputBinding`
//     esté habilitado en `provideRouter`.

import { Routes } from '@angular/router';

import { authGuard } from '../../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../../core/effective-assignment/effective-assignment.guard';

const COMMON_GUARDS = [authGuard, effectiveAssignmentGuard] as const;

/**
 * Catálogo canónico de modos consumidos por los componentes del recorrido.
 * `INICIATIVA_SUBSANACION` e `INCORPORACION` coinciden con el discriminador
 * `CorrectionMode` del `CorrectionFormComponent`.
 */
type RouteMode = 'INITIATIVE_CREATE' | 'INITIATIVE_EDIT' | 'INCORPORATION_CREATE' | 'INCORPORATION_EDIT';

type RouteCorrectionMode = 'INICIATIVA_SUBSANACION' | 'INCORPORACION';

export const REGISTRO_ROUTES: Routes = [
  {
    path: 'iniciativas/nueva',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Presentar iniciativa',
    data: { breadcrumb: 'Presentar iniciativa', mode: 'INITIATIVE_CREATE' satisfies RouteMode },
    loadComponent: () =>
      import('./initiative-form/initiative-form.component').then(
        ({ InitiativeFormComponent }) => InitiativeFormComponent
      )
  },
  {
    path: 'iniciativas/:id',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Editar iniciativa',
    data: { breadcrumb: 'Editar iniciativa', mode: 'INITIATIVE_EDIT' satisfies RouteMode },
    loadComponent: () =>
      import('./initiative-form/initiative-form.component').then(
        ({ InitiativeFormComponent }) => InitiativeFormComponent
      )
  },
  {
    path: 'iniciativas/:id/subsanacion',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Subsanación de iniciativa',
    data: {
      breadcrumb: 'Subsanación',
      correctionMode: 'INICIATIVA_SUBSANACION' satisfies RouteCorrectionMode
    },
    loadComponent: () =>
      import('./correction-form/correction-form.component').then(
        ({ CorrectionFormComponent }) => CorrectionFormComponent
      )
  },
  {
    path: 'incorporaciones/nueva',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Registrar incorporación individual',
    data: { breadcrumb: 'Nueva incorporación', mode: 'INCORPORATION_CREATE' satisfies RouteMode },
    loadComponent: () =>
      import('./incorporation/individual-incorporation.component').then(
        ({ IndividualIncorporationComponent }) => IndividualIncorporationComponent
      )
  },
  {
    path: 'incorporaciones/:id',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Editar incorporación individual',
    data: { breadcrumb: 'Editar incorporación', mode: 'INCORPORATION_EDIT' satisfies RouteMode },
    loadComponent: () =>
      import('./incorporation/individual-incorporation.component').then(
        ({ IndividualIncorporationComponent }) => IndividualIncorporationComponent
      )
  },
  {
    path: 'incorporaciones/:id/correccion',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Corrección de incorporación individual',
    data: {
      breadcrumb: 'Corrección de incorporación',
      correctionMode: 'INCORPORACION' satisfies RouteCorrectionMode
    },
    loadComponent: () =>
      import('./correction-form/correction-form.component').then(
        ({ CorrectionFormComponent }) => CorrectionFormComponent
      )
  }
];