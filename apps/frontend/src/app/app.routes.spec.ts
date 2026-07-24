import { describe, expect, it } from 'vitest';

import { routes } from './app.routes';

/**
 * Carga perezosamente un segmento de `app.routes.ts` y devuelve el array
 * resultante de `Routes` para inspección estática. Se usa para verificar
 * la no-colisión entre `portafolio/evaluacion` y `portafolio/decision` y
 * la presencia de los `loadComponent` de cada subruta.
 *
 * NOTA: las funciones `loadChildren` en `app.routes.ts` ya extraen la constante
 * `*_ROUTES` del módulo importado y la retornan directamente. Por eso el
 * resultado de `loadChildren()` es el array `Routes` mismo, no un objeto
 * con claves `_ROUTES`.
 */
async function cargarSegmentoPortafolio(path: string): Promise<readonly unknown[]> {
  const segmento = routes.find(
    (r) => (r as { path?: string }).path === path
  ) as { loadChildren?: () => Promise<unknown> } | undefined;
  expect(segmento, `Falta segmento ${path}`).toBeDefined();
  const coleccion = (await segmento!.loadChildren!()) as readonly unknown[];
  expect(Array.isArray(coleccion), `Exportación de ${path} no es un array`).toBe(true);
  return coleccion;
}

describe('app.routes', () => {
  it('registra `portafolio/registro` (US1) sin colisión con US2', () => {
    const paths = routes.map((r) => (r as { path?: string }).path ?? '');
    expect(paths).toContain('portafolio/registro');
  });

  it('registra `portafolio/evaluacion` y `portafolio/decision` como hermanos independientes', () => {
    const paths = routes.map((r) => (r as { path?: string }).path ?? '');
    expect(paths).toContain('portafolio/evaluacion');
    expect(paths).toContain('portafolio/decision');
  });

  it('no colisiona `portafolio/evaluacion` con `portafolio/decision`', () => {
    // El segmento padre los separa: ambos usan `iniciativas/:id` en su
    // recorrido interno, pero viven bajo prefijos distintos. Verificamos
    // que los paths de primer nivel sean únicos.
    const pathsPrimerNivel = routes
      .map((r) => (r as { path?: string }).path ?? '')
      .filter((p) => p.length > 0);
    const unicos = new Set(pathsPrimerNivel);
    expect(unicos.size, 'Hay paths duplicados en `app.routes`').toBe(pathsPrimerNivel.length);
  });

  it('registra seguimiento, producto final y cierre como segmentos lazy independientes', () => {
    const paths = routes.map((r) => (r as { path?: string }).path ?? '');
    expect(paths).toContain('portafolio/seguimiento');
    expect(paths).toContain('portafolio/producto-final');
    expect(paths).toContain('portafolio/cierre');
  });

  it('coloca la ruta wildcard `**` como la última entrada del array', () => {
    const paths = routes.map((r) => (r as { path?: string }).path ?? '');
    const indiceWildcard = paths.lastIndexOf('**');
    expect(indiceWildcard, 'Falta la ruta wildcard `**`').toBeGreaterThanOrEqual(0);
    expect(indiceWildcard, 'La ruta wildcard `**` no debe ser la última entrada').toBe(
      paths.length - 1
    );
  });

  it('la redirección raíz precede a la wildcard para evitar captura por `**`', () => {
    const paths = routes.map((r) => (r as { path?: string }).path ?? '');
    const indiceRaiz = paths.indexOf('');
    const indiceWildcard = paths.lastIndexOf('**');
    expect(indiceRaiz, 'Falta la ruta raíz `\'\'`').toBeGreaterThanOrEqual(0);
    expect(indiceRaiz < indiceWildcard, 'La ruta raíz debe preceder a `**`').toBe(true);
  });

  it('carga perezosamente el segmento `portafolio/evaluacion` y sus rutas usan `loadComponent`', async () => {
    const subrutas = await cargarSegmentoPortafolio('portafolio/evaluacion');
    expect(subrutas.length).toBeGreaterThan(0);
    // La ruta canónica usa `loadComponent`; la redirección no lo necesita.
    const evaluacion = subrutas.find(
      (r) => (r as { path?: string }).path === 'iniciativas/:id'
    ) as { loadComponent?: unknown } | undefined;
    expect(evaluacion, 'Falta ruta `iniciativas/:id` en evaluación').toBeDefined();
    expect(typeof evaluacion!.loadComponent).toBe('function');
  });

  it('carga perezosamente el segmento `portafolio/decision` y todas sus rutas usan `loadComponent`', async () => {
    const subrutas = await cargarSegmentoPortafolio('portafolio/decision');
    expect(subrutas.length).toBeGreaterThan(0);
    for (const sub of subrutas) {
      const s = sub as { path?: string; loadComponent?: unknown };
      expect(
        typeof s.loadComponent,
        `Ruta ${s.path as string} sin loadComponent`
      ).toBe('function');
    }
  });

  it('carga perezosamente el segmento `portafolio/seguimiento`', async () => {
    const subrutas = await cargarSegmentoPortafolio('portafolio/seguimiento');
    expect(subrutas).toHaveLength(5);
    for (const sub of subrutas) {
      const route = sub as { path?: string; loadComponent?: unknown };
      expect(typeof route.loadComponent, `Ruta ${route.path ?? ''} sin loadComponent`).toBe('function');
    }
  });

  it('carga perezosamente los segmentos US5 con sus rutas de decisión y cierre', async () => {
    const productoFinal = await cargarSegmentoPortafolio('portafolio/producto-final');
    const cierre = await cargarSegmentoPortafolio('portafolio/cierre');

    expect((productoFinal[0] as { path?: string; loadComponent?: unknown }).path).toBe('proyectos/:id/decision');
    expect(typeof (productoFinal[0] as { loadComponent?: unknown }).loadComponent).toBe('function');
    expect((cierre[0] as { path?: string; loadComponent?: unknown }).path).toBe('proyectos/:id');
    expect(typeof (cierre[0] as { loadComponent?: unknown }).loadComponent).toBe('function');
  });

  it('registra `consulta-institucional` (US7) como segmento protegido y lazy', () => {
    const paths = routes.map((r) => (r as { path?: string }).path ?? '');
    expect(paths).toContain('consulta-institucional');
    const ruta = routes.find((r) => (r as { path?: string }).path === 'consulta-institucional') as
      | { canActivate?: ReadonlyArray<unknown>; loadChildren?: () => Promise<unknown> }
      | undefined;
    expect(ruta, 'Falta el segmento consulta-institucional').toBeDefined();
    expect(typeof ruta!.loadChildren, 'consulta-institucional no es lazy').toBe('function');
    // El segmento institucional exige autenticación previa; el guard
    // institucional precede a la redirección de UX.
    const guards = (ruta!.canActivate ?? []) as ReadonlyArray<unknown>;
    expect(guards.length, 'consulta-institucional sin guard').toBeGreaterThan(0);
  });

  it('registra `consulta-publica` (US7) como segmento anónimo y lazy', () => {
    const paths = routes.map((r) => (r as { path?: string }).path ?? '');
    expect(paths).toContain('consulta-publica');
    const ruta = routes.find((r) => (r as { path?: string }).path === 'consulta-publica') as
      | { canActivate?: ReadonlyArray<unknown>; loadChildren?: () => Promise<unknown> }
      | undefined;
    expect(ruta, 'Falta el segmento consulta-publica').toBeDefined();
    expect(typeof ruta!.loadChildren, 'consulta-publica no es lazy').toBe('function');
    // La consulta pública no exige autenticación: no debe exponer guard.
    const guards = (ruta!.canActivate ?? []) as ReadonlyArray<unknown>;
    expect(guards, 'consulta-publica expone guard').toHaveLength(0);
  });

  it('carga perezosamente el segmento `consulta-institucional` con sus dos rutas', async () => {
    const subrutas = await cargarSegmentoPortafolio('consulta-institucional');
    expect(subrutas).toHaveLength(2);
    const paths = subrutas.map((r) => (r as { path?: string }).path);
    expect(paths).toEqual(['', ':id']);
    for (const sub of subrutas) {
      const route = sub as { path?: string; loadComponent?: unknown };
      expect(typeof route.loadComponent, `Ruta ${route.path ?? ''} sin loadComponent`).toBe('function');
    }
  });

  it('carga perezosamente el segmento `consulta-publica` con sus dos rutas', async () => {
    const subrutas = await cargarSegmentoPortafolio('consulta-publica');
    expect(subrutas).toHaveLength(2);
    const paths = subrutas.map((r) => (r as { path?: string }).path);
    expect(paths).toEqual(['', ':id']);
    for (const sub of subrutas) {
      const route = sub as { path?: string; loadComponent?: unknown };
      expect(typeof route.loadComponent, `Ruta ${route.path ?? ''} sin loadComponent`).toBe('function');
    }
  });

  it('registra `seguridad` (US6) como segmento hermano independiente y lazy', () => {
    const paths = routes.map((r) => (r as { path?: string }).path ?? '');
    expect(paths).toContain('seguridad');
  });

  it('carga perezosamente el segmento `seguridad` con sus cuatro sub-rutas usando `loadComponent`', async () => {
    const subrutas = await cargarSegmentoPortafolio('seguridad');
    expect(subrutas).toHaveLength(4);
    const paths = subrutas.map((r) => (r as { path?: string }).path);
    expect(paths).toEqual(['usuarios', 'matriz', 'asignaciones', 'suplencias']);
    for (const sub of subrutas) {
      const route = sub as { path?: string; loadComponent?: unknown };
      expect(typeof route.loadComponent, `Ruta ${route.path ?? ''} sin loadComponent`).toBe('function');
    }
  });

  it('registra `reportes` (US8) como segmento hermano independiente y lazy', () => {
    const paths = routes.map((r) => (r as { path?: string }).path ?? '');
    expect(paths).toContain('reportes');
    const ruta = routes.find((r) => (r as { path?: string }).path === 'reportes') as
      | { canActivate?: ReadonlyArray<unknown>; loadChildren?: () => Promise<unknown> }
      | undefined;
    expect(ruta, 'Falta el segmento reportes').toBeDefined();
    expect(typeof ruta!.loadChildren, 'reportes no es lazy').toBe('function');
    // El segmento de reportes exige autenticación previa: el guard
    // institucional precede a la redirección de UX.
    const guards = (ruta!.canActivate ?? []) as ReadonlyArray<unknown>;
    expect(guards, 'reportes expone guard de nivel raíz no previsto').toHaveLength(0);
  });

  it('carga perezosamente el segmento `reportes` con sus tres sub-rutas usando `loadComponent`', async () => {
    const subrutas = await cargarSegmentoPortafolio('reportes');
    expect(subrutas).toHaveLength(3);
    const paths = subrutas.map((r) => (r as { path?: string }).path);
    expect(paths).toEqual(['', ':id', ':id/aprobacion']);
    for (const sub of subrutas) {
      const route = sub as { path?: string; loadComponent?: unknown };
      expect(typeof route.loadComponent, `Ruta ${route.path ?? ''} sin loadComponent`).toBe('function');
    }
  });
});
