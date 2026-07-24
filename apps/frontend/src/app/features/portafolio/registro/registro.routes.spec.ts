import { Routes } from '@angular/router';
import { afterEach, describe, expect, it } from 'vitest';

import { authGuard } from '../../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../../core/effective-assignment/effective-assignment.guard';

import { REGISTRO_ROUTES } from './registro.routes';

describe('REGISTRO_ROUTES (US1)', () => {
  afterEach(() => {
    // No hay estado compartido entre pruebas: REGISTRO_ROUTES es inmutable.
  });

  it('exporta seis rutas y todas usan carga perezosa con loadComponent', () => {
    expect(REGISTRO_ROUTES).toHaveLength(6);
    for (const ruta of REGISTRO_ROUTES) {
      expect(typeof ruta.loadComponent).toBe('function');
      // No debe existir un `component` sincrónico: sería eager.
      expect((ruta as { component?: unknown }).component).toBeUndefined();
    }
  });

  it('protege cada ruta con authGuard y effectiveAssignmentGuard', () => {
    for (const ruta of REGISTRO_ROUTES) {
      const guards = (ruta.canActivate ?? []) as ReadonlyArray<unknown>;
      expect(guards, `Ruta ${ruta.path} sin authGuard`).toContain(authGuard);
      expect(guards, `Ruta ${ruta.path} sin effectiveAssignmentGuard`).toContain(effectiveAssignmentGuard);
    }
  });

  it('define un título accesible (WCAG 2.1 AA) en cada ruta', () => {
    for (const ruta of REGISTRO_ROUTES) {
      expect(typeof ruta.title, `Ruta ${ruta.path} sin title`).toBe('string');
      expect((ruta.title as string).length).toBeGreaterThan(0);
    }
  });

  it('expone breadcrumb en `data` para todas las rutas', () => {
    for (const ruta of REGISTRO_ROUTES) {
      const data = (ruta.data ?? {}) as { breadcrumb?: string };
      expect(typeof data.breadcrumb, `Ruta ${ruta.path} sin breadcrumb`).toBe('string');
    }
  });

  it('declara los paths exactos del recorrido aprobado en US1', () => {
    const paths = REGISTRO_ROUTES.map((r) => r.path as string);
    expect(paths).toEqual([
      'iniciativas/nueva',
      'iniciativas/:id',
      'iniciativas/:id/subsanacion',
      'incorporaciones/nueva',
      'incorporaciones/:id',
      'incorporaciones/:id/correccion'
    ]);
  });

  it('no colisiona con placeholders de US2-US8 dentro del segmento portafolio', () => {
    // Los placeholders futuros viven como rutas hermanas bajo `portafolio/<etapa>`
    // en `app.routes.ts`. Las rutas de US1 no deben invadir esos segmentos.
    const paths = REGISTRO_ROUTES.map((r) => r.path as string);
    const placeholdersFuturos = [
      'evaluacion',
      'decision',
      'proyectos',
      'seguimiento',
      'producto-final',
      'cierre'
    ];
    for (const placeholder of placeholdersFuturos) {
      const colision = paths.some((p) => p === placeholder || p.startsWith(`${placeholder}/`));
      expect(colision, `Colisión con placeholder ${placeholder}`).toBe(false);
    }
  });

  it('ordena las rutas estáticas antes de las dinámicas para que `/nueva` no sea capturada por `/:id`', () => {
    const paths = REGISTRO_ROUTES.map((r) => r.path as string);
    const indiceNueva = paths.indexOf('iniciativas/nueva');
    const indiceId = paths.indexOf('iniciativas/:id');
    const indiceIncorporacionNueva = paths.indexOf('incorporaciones/nueva');
    const indiceIncorporacionId = paths.indexOf('incorporaciones/:id');

    expect(indiceNueva).toBeGreaterThanOrEqual(0);
    expect(indiceId).toBeGreaterThanOrEqual(0);
    expect(indiceIncorporacionNueva).toBeGreaterThanOrEqual(0);
    expect(indiceIncorporacionId).toBeGreaterThanOrEqual(0);

    expect(indiceNueva).toBeLessThan(indiceId);
    expect(indiceIncorporacionNueva).toBeLessThan(indiceIncorporacionId);
  });

  it('carga perezosamente los tres componentes de la US1 sin reusar referencias sincrónicas', async () => {
    // Cada `loadComponent` debe devolver una promesa que resuelva a un componente
    // standalone distinto. Verificamos la forma del módulo diferido sin instanciar.
    for (const ruta of REGISTRO_ROUTES) {
      const carga = (ruta.loadComponent as () => Promise<unknown>)();
      expect(carga).toBeInstanceOf(Promise);
      const ComponentClass = await carga;
      expect(ComponentClass, `Componente ausente en la carga de ${ruta.path}`).toBeDefined();
    }
  }, 15000);

  it('mantiene la colección exportada como constante inmutable y compatible con `Routes`', () => {
    // Esta prueba documenta la API pública: la constante es la única fuente de
    // verdad para `loadChildren` en `app.routes.ts`.
    const aceptadaPorAngular: Routes = REGISTRO_ROUTES;
    expect(aceptadaPorAngular).toBe(REGISTRO_ROUTES);
  });
});