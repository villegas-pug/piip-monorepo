import { Routes } from '@angular/router';

import { authGuard } from '../../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../../core/effective-assignment/effective-assignment.guard';

const COMMON_GUARDS = [authGuard, effectiveAssignmentGuard] as const;

/** Ruta lazy del cierre administrativo; los guards solo orientan la UX y el backend autoriza. */
export const CIERRE_ROUTES: Routes = [
  {
    path: 'proyectos/:id',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Cerrar proyecto',
    data: { breadcrumb: 'Cerrar proyecto' },
    loadComponent: () =>
      import('./project-closure.component').then(({ ProjectClosureComponent }) => ProjectClosureComponent)
  }
];
