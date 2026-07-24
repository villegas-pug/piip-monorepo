// T070 · Axe-core para seguimiento US4, con mocks locales y sin backend real.
// T075 materializa el ancla accesible; T076 registra la ruta.

import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page, type Route } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const PROJECT_ID = 101;
const ROUTE = `/portafolio/seguimiento/proyectos/${PROJECT_ID}`;
const TAGS = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] as const;

async function mockSeguimiento(page: Page): Promise<void> {
  await page.route(`**/api/v1/portafolio/proyectos/${PROJECT_ID}`, async (route: Route) => route.fulfill({
    status: 200, contentType: 'application/json', headers: { etag: '"project-101-v1"' },
    body: JSON.stringify({ id: PROJECT_ID, codigo: '2026-MIDAGRI-00101', tipoRegistro: 'PROYECTO', estado: 'PROYECTO_EJECUCION', version: 1, etag: '"project-101-v1"', fechaInicio: '2026-07-01' })
  }));
  await page.route(`**/api/v1/portafolio/proyectos/${PROJECT_ID}/ciclos`, async (route: Route) => route.fulfill({ status: 200, contentType: 'application/json', body: '[]' }));
}

test.describe('US4 · accesibilidad WCAG 2.1 AA de seguimiento', () => {
  for (const viewport of [{ nombre: 'escritorio', use: { viewport: { width: 1280, height: 720 } } }, { nombre: 'Pixel 5', use: { viewport: { width: 393, height: 851 }, isMobile: true, hasTouch: true } }]) {
    test.describe(viewport.nombre, () => {
      test.use(viewport.use);
      test('no tiene violaciones severe/critical en tags WCAG 2.1 AA', async ({ page }) => {
        await mockSeguimiento(page);
        await page.goto(`${BASE_URL}${ROUTE}`);
        await expect(page.locator('h1#tracking-page-title, h2#cycle-form-title')).toBeVisible({ timeout: 10000 });

        const result = await new AxeBuilder({ page }).withTags([...TAGS]).analyze();
        const blockers = result.violations.filter((violation) => violation.impact === 'serious' || violation.impact === 'critical');
        expect(blockers, blockers.map((item) => `${item.id} (${item.impact})`).join(', ')).toEqual([]);
      });
    });
  }
});
