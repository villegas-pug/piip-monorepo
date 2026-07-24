import { Routes } from '@angular/router';

import { authGuard } from '../../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../../core/effective-assignment/effective-assignment.guard';

const COMMON_GUARDS = [authGuard, effectiveAssignmentGuard] as const;

/** Ruta lazy de la decisión de producto; los guards solo orientan la UX y el backend autoriza. */
export const PRODUCTO_FINAL_ROUTES: Routes = [
  {
    path: 'proyectos/:id/decision',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Decidir producto final',
    data: { breadcrumb: 'Decidir producto final' },
    loadComponent: () =>
      import('./final-product-decision.component').then(
        ({ FinalProductDecisionComponent }) => FinalProductDecisionComponent
      )
  }
];
