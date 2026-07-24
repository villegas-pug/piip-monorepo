import { Routes } from '@angular/router';
import { describe, expect, it } from 'vitest';

import { authGuard } from '../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../core/effective-assignment/effective-assignment.guard';

import { SEGURIDAD_ROUTES } from './seguridad.routes';

describe('SEGURIDAD_ROUTES (US6 · T093)', () => {
  it('expone cuatro rutas: usuarios, matriz, asignaciones y suplencias', () => {
    expect(SEGURIDAD_ROUTES).toHaveLength(4);

    const paths = SEGURIDAD_ROUTES.map((r) => (r as { path?: string }).path ?? '');
    expect(paths).toEqual(['usuarios', 'matriz', 'asignaciones', 'suplencias']);
  });

  it('usa carga perezosa con `loadComponent` en cada ruta y nunca un `component` sincrónico', () => {
    for (const ruta of SEGURIDAD_ROUTES) {
      expect(typeof ruta.loadComponent, `Ruta ${ruta.path} sin loadComponent`).toBe('function');
      expect((ruta as { component?: unknown }).component, `Ruta ${ruta.path} con component eager`).toBeUndefined();
    }
  });

  it('protege cada ruta con `authGuard` y `effectiveAssignmentGuard`', () => {
    for (const ruta of SEGURIDAD_ROUTES) {
      const guards = (ruta.canActivate ?? []) as ReadonlyArray<unknown>;
      expect(guards, `Ruta ${ruta.path} sin authGuard`).toContain(authGuard);
      expect(guards, `Ruta ${ruta.path} sin effectiveAssignmentGuard`).toContain(effectiveAssignmentGuard);
    }
  });

  it('define un `title` accesible (WCAG 2.1 AA) en cada ruta', () => {
    for (const ruta of SEGURIDAD_ROUTES) {
      const title = (ruta as { title?: string }).title;
      expect(typeof title, `Ruta ${ruta.path} sin title`).toBe('string');
      expect((title ?? '').length, `Ruta ${ruta.path} con title vacío`).toBeGreaterThan(0);
    }
  });

  it('expone `breadcrumb` en `data` para todas las rutas', () => {
    for (const ruta of SEGURIDAD_ROUTES) {
      const data = ((ruta as { data?: { breadcrumb?: string } }).data) ?? {};
      expect(typeof data.breadcrumb, `Ruta ${ruta.path} sin breadcrumb`).toBe('string');
      expect((data.breadcrumb ?? '').length, `Ruta ${ruta.path} con breadcrumb vacío`).toBeGreaterThan(0);
    }
  });

  it('carga perezosamente cada componente standalone sin instanciarlo en el spec', async () => {
    // Verificamos que cada `loadComponent` devuelva una promesa que
    // resuelve al componente standalone, no a un objeto con claves.
    const esperados: Record<string, string> = {
      usuarios: 'UserAdministrationComponent',
      matriz: 'MatrixAdministrationComponent',
      asignaciones: 'AssignmentAdministrationComponent',
      suplencias: 'SubstitutionAdministrationComponent'
    };
    for (const ruta of SEGURIDAD_ROUTES) {
      const carga = (ruta.loadComponent as () => Promise<unknown>)();
      expect(carga).toBeInstanceOf(Promise);
      const ComponentClass = await carga;
      const nombre = esperados[(ruta as { path: string }).path];
      expect(nombre, `Mapeo ausente para ${ruta.path}`).toBeDefined();
      expect(ComponentClass, `Componente ${nombre} ausente en la carga de ${ruta.path}`).toBeDefined();
    }
  }, 15000);

  it('mantiene la colección exportada como constante compatible con `Routes`', () => {
    // Esta prueba documenta la API pública: la constante es la única
    // fuente de verdad para `loadChildren` en `app.routes.ts`.
    const aceptadaPorAngular: Routes = SEGURIDAD_ROUTES;
    expect(aceptadaPorAngular).toBe(SEGURIDAD_ROUTES);
  });

  it('no expone rutas de portafolio, consulta, organización, documentos, reportes ni auditoría', () => {
    // El segmento `seguridad` solo administra sus cuatro sub-pantallas.
    // Cualquier placeholder de otro módulo constitucional se mantiene
    // como ruta hermana en `app.routes.ts`.
    const paths = SEGURIDAD_ROUTES.map((r) => (r as { path?: string }).path ?? '');
    const placeholders = [
      'portafolio',
      'consulta-publica',
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
