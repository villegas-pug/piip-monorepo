// Recorrido de la US7 (consulta institucional del portafolio).
//
// El recorrido se carga perezosamente desde `app.routes.ts` mediante
// `loadChildren: () => import('./consulta-institucional.routes').then(m => m.CONSULTA_INSTITUCIONAL_ROUTES)`.
// Ninguno de los componentes ni servicios del recorrido debe incluirse
// en el bundle inicial: por eso todas las rutas usan `loadComponent`.
//
// Decisiones de diseño:
//   * Componentes standalone (sin NgModule), uno por ruta.
//   * `authGuard` protege cada ruta: la sesión institucional la
//     gestiona el backend, pero la UX exige evitar navegación a
//     formularios institucionales sin login.
//   * `effectiveAssignmentGuard` mejora la experiencia mostrando el
//     selector de asignación efectiva cuando el usuario no ha
//     elegido uno. NO es autoridad local: la correspondencia entre
//     unidad, perfil y recurso la confirma el backend en cada llamada
//     y la transporta `effectiveAssignmentInterceptor` mediante el
//     encabezado `X-Asignacion-Efectiva-Id`.
//   * `title` por ruta para WCAG 2.1 AA (lectores de pantalla y
//     orientación). `data.breadcrumb` queda preparado para integrar un
//     sistema de migas institucional sin acoplar el routing a un
//     componente específico.
//   * Sin permisos locales: este módulo no consulta roles, no decide
//     autoridad ni filtra operaciones; el backend es la autoridad
//     efectiva.
//   * El segmento vive como hermano de `consulta-publica` y de los
//     `portafolio/*`; las rutas internas no comparten prefijos.
//   * El segmento NO expone DTOs públicos y NO comparte tipos con
//     `consulta-publica`: la separación entre cliente institucional y
//     cliente público es una decisión constitucional de privacidad.

import { Routes } from '@angular/router';

import { authGuard } from '../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../core/effective-assignment/effective-assignment.guard';

const COMMON_GUARDS = [authGuard, effectiveAssignmentGuard] as const;

/**
 * Catálogo de la US7 para la consulta institucional. La pantalla
 * canónica es la ruta `:id` y carga perezosamente
 * `PortfolioDetailComponent` standalone. La entrada raíz lista
 * los filtros como ayuda de navegación y delega en la ruta con
 * `id` para ver el detalle seleccionado.
 */
export const CONSULTA_INSTITUCIONAL_ROUTES: Routes = [
  {
    path: '',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Consulta institucional',
    data: { breadcrumb: 'Consulta institucional' },
    loadComponent: () =>
      import('./portfolio-detail.component').then(
        ({ PortfolioDetailComponent }) => PortfolioDetailComponent
      )
  },
  {
    path: ':id',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Detalle institucional',
    data: { breadcrumb: 'Detalle institucional' },
    loadComponent: () =>
      import('./portfolio-detail.component').then(
        ({ PortfolioDetailComponent }) => PortfolioDetailComponent
      )
  }
];
