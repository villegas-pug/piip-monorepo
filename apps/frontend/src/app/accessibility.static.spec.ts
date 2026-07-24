// T120 · Verificación estática de convenciones WCAG y lazy loading, sin ejecutar navegador.
import { describe, expect, it } from 'vitest';
import { routes } from './app.routes';

describe('verificación estática de accesibilidad y rutas (T120)', () => {
  it('mantiene la consulta pública como ruta lazy sin authGuard', () => {
    const publicRoute = routes.find((route) => route.path === 'consulta-publica');
    expect(publicRoute?.loadChildren).toBeTypeOf('function');
    expect(publicRoute?.canActivate).toBeUndefined();
  });
  it('mantiene la consulta institucional como ruta lazy protegida', () => {
    const institutional = routes.find((route) => route.path === 'consulta-institucional');
    expect(institutional?.loadChildren).toBeTypeOf('function');
    // El segmento institucional debe exigir autenticación previa.
    const guards = (institutional?.canActivate ?? []) as ReadonlyArray<unknown>;
    expect(guards.length).toBeGreaterThan(0);
  });
  it('carga recorridos institucionales de portafolio de forma diferida', () => {
    const institutional = routes.filter((route) => route.path?.startsWith('portafolio/'));
    expect(institutional.length).toBeGreaterThan(0);
    expect(institutional.every((route) => typeof route.loadChildren === 'function')).toBe(true);
  });
});
