// Recorrido de la US2 (decisión formal de iniciativa y cancelación de proyecto).
//
// El recorrido se carga perezosamente desde `app.routes.ts` mediante
// `loadChildren: () => import('./decision.routes').then(m => m.DECISION_ROUTES)`.
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
//   - La cancelación de proyecto vive bajo `proyectos/:id/cancelacion`
//     para no reutilizar la página de decisión de iniciativa. Aunque ambos
//     recursos comparten identificador numérico, pertenecen a etapas
//     distintas del portafolio y a componentes distintos. La Autoridad
//     llega al formulario de cancelación solo desde el contexto del
//     proyecto.
//   - El segmento vive como hermano de `portafolio/registro` y
//     `portafolio/evaluacion`; comparte el path `iniciativas/:id` con la
//     evaluación, pero pertenece a otra etapa del portafolio y no colisiona
//     porque el segmento del padre los separa.

import { Routes } from '@angular/router';

import { authGuard } from '../../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../../core/effective-assignment/effective-assignment.guard';

const COMMON_GUARDS = [authGuard, effectiveAssignmentGuard] as const;

/**
 * Catálogo canónico de la US2 para la Autoridad: una ruta de decisión de
 * iniciativa y una de cancelación de proyecto, ambas con carga perezosa
 * y protegidas por los mismos guards de experiencia.
 */
export const DECISION_ROUTES: Routes = [
  {
    path: 'iniciativas/:id',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Decidir iniciativa',
    data: { breadcrumb: 'Decidir iniciativa' },
    loadComponent: () =>
      import('./initiative-decision-page.component').then(
        ({ InitiativeDecisionPageComponent }) => InitiativeDecisionPageComponent
      )
  },
  {
    path: 'proyectos/:id/cancelacion',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Cancelar proyecto',
    data: { breadcrumb: 'Cancelar proyecto' },
    loadComponent: () =>
      import('./project-cancellation.component').then(
        ({ ProjectCancellationComponent }) => ProjectCancellationComponent
      )
  }
];