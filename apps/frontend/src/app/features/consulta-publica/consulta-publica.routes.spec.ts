// T101/T102 · Contrato de rutas perezosas de la consulta pública (US7).
//
// La consulta pública es ANÓNIMA: la suite verifica que ninguna ruta
// expone `authGuard` ni `effectiveAssignmentGuard`. La autorización
// efectiva la aplica el backend, y el cliente público nunca debe
// pedir credenciales ni asignación efectiva.
import { Routes } from '@angular/router';
import { describe, expect, it } from 'vitest';

import { authGuard } from '../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../core/effective-assignment/effective-assignment.guard';

import { PUBLIC_QUERY_ROUTES } from './consulta-publica.routes';

describe('PUBLIC_QUERY_ROUTES (T101/T102 · US7)', () => {
  it('expone dos rutas: raíz y detalle con `:id`', () => {
    expect(PUBLIC_QUERY_ROUTES).toHaveLength(2);
    const paths = PUBLIC_QUERY_ROUTES.map((r) => (r as { path?: string }).path ?? '');
    expect(paths).toEqual(['', ':id']);
  });

  it('usa carga perezosa con `loadComponent` en cada ruta y nunca un `component` sincrónico', () => {
    for (const ruta of PUBLIC_QUERY_ROUTES) {
      const referencia = ruta as { loadComponent?: unknown; component?: unknown; path?: string };
      expect(typeof referencia.loadComponent, `Ruta ${referencia.path} sin loadComponent`).toBe('function');
      expect(referencia.component, `Ruta ${referencia.path} con component eager`).toBeUndefined();
    }
  });

  it('mantiene la consulta pública anónima: sin `authGuard` ni `effectiveAssignmentGuard`', () => {
    for (const ruta of PUBLIC_QUERY_ROUTES) {
      const guards = (ruta.canActivate ?? []) as ReadonlyArray<unknown>;
      expect(guards, `Ruta ${ruta.path} expone authGuard`).not.toContain(authGuard);
      expect(guards, `Ruta ${ruta.path} expone effectiveAssignmentGuard`).not.toContain(effectiveAssignmentGuard);
    }
  });

  it('define un `title` accesible (WCAG 2.1 AA) en cada ruta', () => {
    for (const ruta of PUBLIC_QUERY_ROUTES) {
      const title = (ruta as { title?: string }).title;
      expect(typeof title, `Ruta ${ruta.path} sin title`).toBe('string');
      expect((title ?? '').length, `Ruta ${ruta.path} con title vacío`).toBeGreaterThan(0);
    }
  });

  it('expone `breadcrumb` en `data` para todas las rutas', () => {
    for (const ruta of PUBLIC_QUERY_ROUTES) {
      const data = ((ruta as { data?: { breadcrumb?: string } }).data) ?? {};
      expect(typeof data.breadcrumb, `Ruta ${ruta.path} sin breadcrumb`).toBe('string');
      expect((data.breadcrumb ?? '').length, `Ruta ${ruta.path} con breadcrumb vacío`).toBeGreaterThan(0);
    }
  });

  it('carga perezosamente el `PublicDetailComponent` standalone en ambas rutas', async () => {
    for (const ruta of PUBLIC_QUERY_ROUTES) {
      const carga = ruta.loadComponent as unknown as () => Promise<unknown>;
      const promesa = carga();
      expect(promesa).toBeInstanceOf(Promise);
      const ComponentClass = await promesa;
      expect(ComponentClass, `Componente ausente en la carga de ${ruta.path}`).toBeDefined();
    }
  });

  it('mantiene la colección exportada como constante compatible con `Routes`', () => {
    const aceptadaPorAngular: Routes = PUBLIC_QUERY_ROUTES;
    expect(aceptadaPorAngular).toBe(PUBLIC_QUERY_ROUTES);
  });

  it('no expone rutas de portafolio, seguridad, consulta institucional, organización, documentos, reportes ni auditoría', () => {
    const paths = PUBLIC_QUERY_ROUTES.map((r) => (r as { path?: string }).path ?? '');
    const placeholders = [
      'portafolio',
      'seguridad',
      'consulta-institucional',
      'organizacion',
      'documentos',
      'reportes',
      'auditoria'
    ];
    for (const placeholder of placeholders) {
      const colision = paths.some((p) => p === placeholder || p.startsWith(`${placeholder}/`));
      expect(colision, `Colisión con placeholder ${placeholder}`).toBe(false);
    }
  });
});
