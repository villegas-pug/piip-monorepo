import { Routes } from '@angular/router';
import { describe, expect, it } from 'vitest';

import { authGuard } from '../../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../../core/effective-assignment/effective-assignment.guard';

import { DECISION_ROUTES } from './decision.routes';

describe('DECISION_ROUTES (US2)', () => {
  it('expone dos entradas: decisión de iniciativa y cancelación de proyecto', () => {
    expect(DECISION_ROUTES).toHaveLength(2);

    const rutaDecision = DECISION_ROUTES[0] as { path?: string; loadComponent?: unknown };
    const rutaCancelacion = DECISION_ROUTES[1] as { path?: string; loadComponent?: unknown };

    expect(rutaDecision.path).toBe('iniciativas/:id');
    expect(rutaCancelacion.path).toBe('proyectos/:id/cancelacion');
  });

  it('usa carga perezosa con `loadComponent` en ambas rutas', () => {
    for (const ruta of DECISION_ROUTES) {
      const r = ruta as { path?: string; loadComponent?: unknown; component?: unknown };
      expect(typeof r.loadComponent, `Ruta ${r.path} sin loadComponent`).toBe('function');
      // Sin componente sincrónico: si existiera sería eager.
      expect(r.component, `Ruta ${r.path} con component sincrónico`).toBeUndefined();
    }
  });

  it('protege cada ruta con `authGuard` y `effectiveAssignmentGuard`', () => {
    for (const ruta of DECISION_ROUTES) {
      const r = ruta as { path?: string; canActivate?: ReadonlyArray<unknown> };
      const guards = r.canActivate ?? [];
      expect(guards, `Falta authGuard en ${r.path}`).toContain(authGuard);
      expect(guards, `Falta effectiveAssignmentGuard en ${r.path}`).toContain(effectiveAssignmentGuard);
    }
  });

  it('define un `title` accesible (WCAG 2.1 AA) en cada ruta', () => {
    for (const ruta of DECISION_ROUTES) {
      const r = ruta as { path?: string; title?: string };
      expect(typeof r.title, `Ruta ${r.path} sin title`).toBe('string');
      expect((r.title ?? '').length).toBeGreaterThan(0);
    }
  });

  it('expone `breadcrumb` en `data` para cada ruta', () => {
    for (const ruta of DECISION_ROUTES) {
      const r = ruta as { path?: string; data?: { breadcrumb?: string } };
      const data = r.data ?? {};
      expect(typeof data.breadcrumb, `Ruta ${r.path} sin breadcrumb`).toBe('string');
      expect((data.breadcrumb ?? '').length).toBeGreaterThan(0);
    }
  });

  it('carga perezosamente `InitiativeDecisionPageComponent` y `ProjectCancellationComponent`', async () => {
    for (const ruta of DECISION_ROUTES) {
      const carga = ruta.loadComponent as unknown as () => Promise<unknown>;
      const promesa = carga();
      expect(promesa, `loadComponent de ${(ruta as { path?: string }).path} no devuelve Promise`).toBeInstanceOf(Promise);
      const ComponentClass = await promesa;
      expect(ComponentClass, `Componente ausente en la carga de ${(ruta as { path?: string }).path}`).toBeDefined();
    }
  }, 15000);

  it('declara los paths exactos del recorrido aprobado en US2 decisión', () => {
    const paths = DECISION_ROUTES.map((r) => (r as { path?: string }).path ?? '');
    expect(paths).toEqual(['iniciativas/:id', 'proyectos/:id/cancelacion']);
  });

  it('mantiene la colección exportada como constante compatible con `Routes`', () => {
    const aceptadaPorAngular: Routes = DECISION_ROUTES;
    expect(aceptadaPorAngular).toBe(DECISION_ROUTES);
  });

  it('no expone rutas de evaluación, proyectos (no cancelación), seguimiento, producto-final ni cierre', () => {
    // Las hermanas US3-US8 viven como segmentos hermanos en `app.routes.ts`.
    // La US2 de decisión no debe invadir esos placeholders, salvo la ruta
    // dedicada `proyectos/:id/cancelacion` que sí pertenece a la US2.
    const paths = DECISION_ROUTES.map((r) => (r as { path?: string }).path ?? '');
    const placeholdersProhibidos = ['evaluacion', 'seguimiento', 'producto-final', 'cierre'];
    for (const placeholder of placeholdersProhibidos) {
      const colision = paths.some((p) => p === placeholder || p.startsWith(`${placeholder}/`));
      expect(colision, `Colisión con placeholder ${placeholder}`).toBe(false);
    }

    // Verifica además que no existe una ruta de proyecto sin sufijo
    // `cancelacion`: la Autoridad solo cancela proyectos, no los edita.
    const colisionProyectoPlano = paths.some((p) => p === 'proyectos/:id');
    expect(colisionProyectoPlano, 'No debe existir ruta `proyectos/:id` sin cancelación').toBe(false);
  });
});