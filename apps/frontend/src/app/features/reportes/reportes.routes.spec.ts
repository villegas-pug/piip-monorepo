// T109 · Contrato de rutas perezosas del módulo de reportes
// institucionales (US8).
//
// La suite valida estáticamente la configuración de rutas, los guards
// y la exposición de los componentes lazy. No se instancia ningún
// componente ni se ejercita el inyector.
import { Routes } from '@angular/router';
import { describe, expect, it } from 'vitest';

import { authGuard } from '../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../core/effective-assignment/effective-assignment.guard';

import { REPORTES_ROUTES } from './reportes.routes';

describe('REPORTES_ROUTES (T109 · US8)', () => {
  it('expone tres rutas: raíz, detalle con `:id` y aprobación con `:id/aprobacion`', () => {
    expect(REPORTES_ROUTES).toHaveLength(3);
    const paths = REPORTES_ROUTES.map((r) => (r as { path?: string }).path ?? '');
    expect(paths).toEqual(['', ':id', ':id/aprobacion']);
  });

  it('usa carga perezosa con `loadComponent` en cada ruta y nunca un `component` sincrónico', () => {
    for (const ruta of REPORTES_ROUTES) {
      const referencia = ruta as { loadComponent?: unknown; component?: unknown; path?: string };
      expect(typeof referencia.loadComponent, `Ruta ${referencia.path} sin loadComponent`).toBe('function');
      expect(referencia.component, `Ruta ${referencia.path} con component eager`).toBeUndefined();
    }
  });

  it('protege cada ruta con `authGuard` y `effectiveAssignmentGuard`', () => {
    for (const ruta of REPORTES_ROUTES) {
      const guards = (ruta.canActivate ?? []) as ReadonlyArray<unknown>;
      expect(guards, `Ruta ${ruta.path} sin authGuard`).toContain(authGuard);
      expect(guards, `Ruta ${ruta.path} sin effectiveAssignmentGuard`).toContain(effectiveAssignmentGuard);
    }
  });

  it('define un `title` accesible (WCAG 2.1 AA) en cada ruta', () => {
    for (const ruta of REPORTES_ROUTES) {
      const title = (ruta as { title?: string }).title;
      expect(typeof title, `Ruta ${ruta.path} sin title`).toBe('string');
      expect((title ?? '').length, `Ruta ${ruta.path} con title vacío`).toBeGreaterThan(0);
    }
  });

  it('expone `breadcrumb` en `data` para todas las rutas', () => {
    for (const ruta of REPORTES_ROUTES) {
      const data = ((ruta as { data?: { breadcrumb?: string } }).data) ?? {};
      expect(typeof data.breadcrumb, `Ruta ${ruta.path} sin breadcrumb`).toBe('string');
      expect((data.breadcrumb ?? '').length, `Ruta ${ruta.path} con breadcrumb vacío`).toBeGreaterThan(0);
    }
  });

  it('carga perezosamente cada componente standalone de US8', async () => {
    const esperados: Record<string, string> = {
      '': 'ReportGenerationComponent',
      ':id': 'ReportDetailComponent',
      ':id/aprobacion': 'ReportApprovalComponent'
    };
    for (const ruta of REPORTES_ROUTES) {
      const carga = (ruta.loadComponent as () => Promise<unknown>)();
      expect(carga).toBeInstanceOf(Promise);
      const ComponentClass = await carga;
      const nombre = esperados[(ruta as { path: string }).path];
      expect(nombre, `Mapeo ausente para ${ruta.path}`).toBeDefined();
      expect(ComponentClass, `Componente ${nombre} ausente en la carga de ${ruta.path}`).toBeDefined();
    }
  });

  it('mantiene la colección exportada como constante compatible con `Routes`', () => {
    const aceptadaPorAngular: Routes = REPORTES_ROUTES;
    expect(aceptadaPorAngular).toBe(REPORTES_ROUTES);
  });

  it('no expone rutas de portafolio, seguridad, consulta, organización, documentos ni auditoría', () => {
    const paths = REPORTES_ROUTES.map((r) => (r as { path?: string }).path ?? '');
    const placeholders = [
      'portafolio',
      'seguridad',
      'consulta-publica',
      'consulta-institucional',
      'organizacion',
      'documentos',
      'auditoria'
    ];
    for (const placeholder of placeholders) {
      const colision = paths.some((p) => p === placeholder || p.startsWith(`${placeholder}/`));
      expect(colision, `Colisión con placeholder ${placeholder}`).toBe(false);
    }
  });
});
