import { Routes } from '@angular/router';

import { authGuard } from '../../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../../core/effective-assignment/effective-assignment.guard';

const COMMON_GUARDS = [authGuard, effectiveAssignmentGuard] as const;

/** Rutas lazy de la US4; los guards orientan la UX y el backend autoriza cada operación. */
export const SEGUIMIENTO_ROUTES: Routes = [
  {
    path: 'proyectos/:id',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Seguimiento del proyecto',
    data: { breadcrumb: 'Seguimiento del proyecto' },
    loadComponent: () =>
      import('./tracking-page.component').then(({ TrackingPageComponent }) => TrackingPageComponent)
  },
  {
    path: 'proyectos/:id/ciclos/nuevo',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Registrar ciclo',
    data: { breadcrumb: 'Registrar ciclo' },
    loadComponent: () =>
      import('./cycle-form/cycle-form.component').then(({ CycleFormComponent }) => CycleFormComponent)
  },
  {
    path: 'proyectos/:id/ciclos/:cicloId/correccion',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Corregir ciclo',
    data: { breadcrumb: 'Corregir ciclo' },
    loadComponent: () =>
      import('./cycle-form/cycle-form.component').then(({ CycleFormComponent }) => CycleFormComponent)
  },
  {
    path: 'proyectos/:id/suspension',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Suspender proyecto',
    data: { breadcrumb: 'Suspender proyecto' },
    loadComponent: () =>
      import('./project-suspension.component').then(({ ProjectSuspensionComponent }) => ProjectSuspensionComponent)
  },
  {
    path: 'proyectos/:id/producto-final/presentacion',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Presentar producto final',
    data: { breadcrumb: 'Presentar producto final' },
    loadComponent: () =>
      import('../producto-final/final-product-submission.component').then(
        ({ FinalProductSubmissionComponent }) => FinalProductSubmissionComponent
      )
  }
];
