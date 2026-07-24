// Recorrido de la US2 (evaluación y subsanación de iniciativa).
//
// El recorrido se carga perezosamente desde `app.routes.ts` mediante
// `loadChildren: () => import('./evaluacion.routes').then(m => m.EVALUACION_ROUTES)`.
// Ninguno de los componentes ni servicios del recorrido debe incluirse en el
// bundle inicial: por eso todas las rutas usan `loadComponent`.
//
// Decisiones de diseño:
//   - Componentes standalone (sin NgModule), uno por ruta.
//   - `authGuard` protege cada ruta: la sesión institucional la gestiona el
//     backend, pero la UX exige evitar navegación a formularios sin login.
//   - `effectiveAssignmentGuard` mejora la experiencia mostrando el selector
//     de asignación efectiva cuando el usuario no ha elegido uno. NO es
//     autoridad local: la correspondencia entre unidad, perfil y recurso la
//     confirma el backend en cada llamada y la transporta
//     `effectiveAssignmentInterceptor` mediante el encabezado
//     `X-Asignacion-Efectiva-Id`.
//   - `title` por ruta para WCAG 2.1 AA (lectores de pantalla y orientación).
//   - `data.breadcrumb` queda preparado para integrar un sistema de migas
//     institucional sin acoplar el routing a un componente específico.
//   - La subsanación se invoca desde la página de evaluación mediante el
//     query param `subsanacion=1`. La ruta `iniciativas/:id/subsanacion`
//     redirige para mantener una única página canónica y concentrar la
//     responsabilidad en `EvaluationPageComponent`. La redirección NO carga
//     la página (no instancia el componente); `pathMatch: 'full'` garantiza
//     que solo coincide con la URL exacta.
//   - El segmento vive como hermano de `portafolio/registro` y
//     `portafolio/decision`; ambos comparten el path `iniciativas/:id`,
//     pero pertenecen a etapas distintas del portafolio y no colisionan.

import { Routes } from '@angular/router';

import { authGuard } from '../../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../../core/effective-assignment/effective-assignment.guard';

const COMMON_GUARDS = [authGuard, effectiveAssignmentGuard] as const;

/**
 * Catálogo canónico de la US2 para el Evaluador: una sola ruta cargada
 * perezosamente y una redirección hacia ella con query param que activa
 * la apertura de subsanación.
 */
export const EVALUACION_ROUTES: Routes = [
  {
    path: 'iniciativas/:id',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Evaluar iniciativa',
    data: { breadcrumb: 'Evaluar iniciativa' },
    loadComponent: () =>
      import('./evaluation-page.component').then(
        ({ EvaluationPageComponent }) => EvaluationPageComponent
      )
  },
  {
    path: 'iniciativas/:id/subsanacion',
    redirectTo: 'iniciativas/:id?subsanacion=1',
    pathMatch: 'full'
  }
];