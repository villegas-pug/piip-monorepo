import { Routes } from '@angular/router';
import { describe, expect, it } from 'vitest';

import { authGuard } from '../../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../../core/effective-assignment/effective-assignment.guard';

import { EVALUACION_ROUTES } from './evaluacion.routes';

describe('EVALUACION_ROUTES (US2)', () => {
  it('expone dos entradas: una ruta canónica y una redirección a subsanación', () => {
    expect(EVALUACION_ROUTES).toHaveLength(2);

    const rutaEvaluacion = EVALUACION_ROUTES[0] as { path?: string; redirectTo?: string };
    const rutaSubsanacion = EVALUACION_ROUTES[1] as { path?: string; redirectTo?: string; pathMatch?: string };

    expect(rutaEvaluacion.path).toBe('iniciativas/:id');
    expect(rutaEvaluacion.redirectTo).toBeUndefined();

    expect(rutaSubsanacion.path).toBe('iniciativas/:id/subsanacion');
    expect(rutaSubsanacion.redirectTo).toBe('iniciativas/:id?subsanacion=1');
    // `pathMatch: 'full'` garantiza que la redirección solo aplique a la URL exacta.
    expect(rutaSubsanacion.pathMatch).toBe('full');
  });

  it('usa carga perezosa con `loadComponent` en la ruta canónica', () => {
    const ruta = EVALUACION_ROUTES[0] as { loadComponent?: unknown; component?: unknown };
    expect(typeof ruta.loadComponent).toBe('function');
    // Sin componente sincrónico: si existiera sería eager.
    expect(ruta.component).toBeUndefined();
  });

  it('protege la ruta canónica con `authGuard` y `effectiveAssignmentGuard`', () => {
    const ruta = EVALUACION_ROUTES[0] as { canActivate?: ReadonlyArray<unknown> };
    const guards = ruta.canActivate ?? [];
    expect(guards, 'Falta authGuard').toContain(authGuard);
    expect(guards, 'Falta effectiveAssignmentGuard').toContain(effectiveAssignmentGuard);
  });

  it('define un `title` accesible (WCAG 2.1 AA) en la ruta canónica', () => {
    const ruta = EVALUACION_ROUTES[0] as { title?: string };
    expect(typeof ruta.title).toBe('string');
    expect((ruta.title ?? '').length).toBeGreaterThan(0);
  });

  it('expone `breadcrumb` en `data` para la ruta canónica', () => {
    const ruta = EVALUACION_ROUTES[0] as { data?: { breadcrumb?: string } };
    const data = ruta.data ?? {};
    expect(typeof data.breadcrumb).toBe('string');
    expect((data.breadcrumb ?? '').length).toBeGreaterThan(0);
  });

  it('carga perezosamente el `EvaluationPageComponent` standalone', async () => {
    const ruta = EVALUACION_ROUTES[0] as unknown as {
      loadComponent: () => Promise<unknown>;
    };
    const carga = ruta.loadComponent();
    expect(carga).toBeInstanceOf(Promise);
    const ComponentClass = await carga;
    expect(ComponentClass, 'EvaluationPageComponent ausente en la carga diferida').toBeDefined();
  });

  it('mantiene la colección exportada como constante compatible con `Routes`', () => {
    const aceptadaPorAngular: Routes = EVALUACION_ROUTES;
    expect(aceptadaPorAngular).toBe(EVALUACION_ROUTES);
  });

  it('no expone rutas de decisión, proyectos, seguimiento, producto-final ni cierre', () => {
    // Las hermanas US3-US8 viven como segmentos hermanos en `app.routes.ts`.
    // La US2 de evaluación no debe invadir esos placeholders.
    const paths = EVALUACION_ROUTES.map((r) => (r as { path?: string }).path ?? '');
    const placeholders = ['decision', 'proyectos', 'seguimiento', 'producto-final', 'cierre'];
    for (const placeholder of placeholders) {
      const colision = paths.some((p) => p === placeholder || p.startsWith(`${placeholder}/`));
      expect(colision, `Colisión con placeholder ${placeholder}`).toBe(false);
    }
  });
});