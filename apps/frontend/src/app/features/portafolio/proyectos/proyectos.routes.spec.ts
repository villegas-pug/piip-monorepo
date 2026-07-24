import { Routes } from '@angular/router';
import { describe, expect, it } from 'vitest';

import { authGuard } from '../../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../../core/effective-assignment/effective-assignment.guard';

import { PROYECTOS_ROUTES } from './proyectos.routes';

describe('PROYECTOS_ROUTES (US3)', () => {
  it('expone dos entradas: proyecto derivado y proyecto directo', () => {
    expect(PROYECTOS_ROUTES).toHaveLength(2);

    const rutaDerivado = PROYECTOS_ROUTES[0] as { path?: string; loadComponent?: unknown };
    const rutaDirecto = PROYECTOS_ROUTES[1] as { path?: string; loadComponent?: unknown };

    expect(rutaDerivado.path).toBe('iniciativas/:id/proyecto-derivado');
    expect(rutaDirecto.path).toBe('proyectos-directos/nuevo');
  });

  it('usa carga perezosa con `loadComponent` en ambas rutas', () => {
    for (const ruta of PROYECTOS_ROUTES) {
      const r = ruta as { path?: string; loadComponent?: unknown; component?: unknown };
      expect(typeof r.loadComponent, `Ruta ${r.path as string} sin loadComponent`).toBe('function');
      // Sin componente sincronico: si existiera seria eager.
      expect(r.component, `Ruta ${r.path as string} con component sincronico`).toBeUndefined();
    }
  });

  it('protege cada ruta con `authGuard` y `effectiveAssignmentGuard`', () => {
    for (const ruta of PROYECTOS_ROUTES) {
      const r = ruta as { path?: string; canActivate?: ReadonlyArray<unknown> };
      const guards = r.canActivate ?? [];
      expect(guards, `Falta authGuard en ${r.path as string}`).toContain(authGuard);
      expect(guards, `Falta effectiveAssignmentGuard en ${r.path as string}`).toContain(
        effectiveAssignmentGuard
      );
    }
  });

  it('define un `title` accesible (WCAG 2.1 AA) en cada ruta', () => {
    for (const ruta of PROYECTOS_ROUTES) {
      const r = ruta as { path?: string; title?: string };
      expect(typeof r.title, `Ruta ${r.path as string} sin title`).toBe('string');
      expect((r.title ?? '').length).toBeGreaterThan(0);
    }
  });

  it('expone `breadcrumb` en `data` para cada ruta', () => {
    for (const ruta of PROYECTOS_ROUTES) {
      const r = ruta as { path?: string; data?: { breadcrumb?: string } };
      const data = r.data ?? {};
      expect(typeof data.breadcrumb, `Ruta ${r.path as string} sin breadcrumb`).toBe('string');
      expect((data.breadcrumb ?? '').length).toBeGreaterThan(0);
    }
  });

  it('carga perezosamente `DerivedProjectComponent` y `DirectProjectComponent`', async () => {
    for (const ruta of PROYECTOS_ROUTES) {
      const r = ruta as unknown as { path?: string; loadComponent: () => Promise<unknown> };
      const carga = r.loadComponent();
      expect(carga, `loadComponent de ${r.path as string} no devuelve Promise`).toBeInstanceOf(
        Promise
      );
      const ComponentClass = await carga;
      expect(ComponentClass, `Componente ausente en la carga de ${r.path as string}`).toBeDefined();
    }
  });

  it('declara los paths exactos del recorrido aprobado en US3', () => {
    const paths = PROYECTOS_ROUTES.map((r) => (r as { path?: string }).path ?? '');
    expect(paths).toEqual(['iniciativas/:id/proyecto-derivado', 'proyectos-directos/nuevo']);
  });

  it('mantiene la coleccion exportada como constante compatible con `Routes`', () => {
    const aceptadaPorAngular: Routes = PROYECTOS_ROUTES;
    expect(aceptadaPorAngular).toBe(PROYECTOS_ROUTES);
  });

  it('no expone rutas de registro, evaluacion, decision, seguimiento, producto-final ni cierre', () => {
    // Las hermanas US1, US2 y US4-US8 viven como segmentos hermanos en
    // `app.routes.ts`. La US3 de proyectos no debe invadir esos placeholders.
    const paths = PROYECTOS_ROUTES.map((r) => (r as { path?: string }).path ?? '');
    const placeholdersProhibidos = [
      'registro',
      'evaluacion',
      'decision',
      'seguimiento',
      'producto-final',
      'cierre'
    ];
    for (const placeholder of placeholdersProhibidos) {
      const colision = paths.some((p) => p === placeholder || p.startsWith(`${placeholder}/`));
      expect(colision, `Colision con placeholder ${placeholder}`).toBe(false);
    }
  });
});