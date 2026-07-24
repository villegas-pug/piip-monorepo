import { Routes } from '@angular/router';
import { describe, expect, it } from 'vitest';

import { authGuard } from '../../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../../core/effective-assignment/effective-assignment.guard';

import { SEGUIMIENTO_ROUTES } from './seguimiento.routes';

describe('SEGUIMIENTO_ROUTES (US4)', () => {
  it('declara los paths aprobados sin exponer cancelación', () => {
    const paths = SEGUIMIENTO_ROUTES.map((route) => route.path);

    expect(paths).toEqual([
      'proyectos/:id',
      'proyectos/:id/ciclos/nuevo',
      'proyectos/:id/ciclos/:cicloId/correccion',
      'proyectos/:id/suspension',
      'proyectos/:id/producto-final/presentacion'
    ]);
    expect(paths.some((path) => path?.includes('cancelacion'))).toBe(false);
  });

  it('carga cada página de forma perezosa y protege la navegación institucional', () => {
    for (const route of SEGUIMIENTO_ROUTES) {
      expect(typeof route.loadComponent, `Ruta ${route.path ?? ''} sin loadComponent`).toBe('function');
      expect(route.component, `Ruta ${route.path ?? ''} con component eager`).toBeUndefined();
      expect(route.canActivate, `Falta authGuard en ${route.path ?? ''}`).toContain(authGuard);
      expect(route.canActivate, `Falta effectiveAssignmentGuard en ${route.path ?? ''}`).toContain(effectiveAssignmentGuard);
    }
  });

  it('define título y breadcrumb accesibles en cada ruta', () => {
    for (const route of SEGUIMIENTO_ROUTES) {
      expect(route.title, `Ruta ${route.path ?? ''} sin title`).toEqual(expect.any(String));
      expect(route.data?.['breadcrumb'], `Ruta ${route.path ?? ''} sin breadcrumb`).toEqual(expect.any(String));
    }
  });

  it('es compatible con el tipo Routes de Angular', () => {
    const acceptedRoutes: Routes = SEGUIMIENTO_ROUTES;
    expect(acceptedRoutes).toBe(SEGUIMIENTO_ROUTES);
  });
});
