// T079 · Recorrido futuro de T082; mocks locales, sin backend real.
import { expect, test, type Route } from '@playwright/test';

const baseUrl = 'http://localhost:4200';
const projectId = 501;
const route = `/portafolio/producto-final/proyectos/${projectId}/decision`;
const endpoint = `**/api/v1/portafolio/proyectos/${projectId}/producto-final/decisiones`;

test.describe('US5 · Decidir producto final', () => {
  test('envía decisión aprobada con concurrencia e idempotencia', async ({ page }) => {
    await page.route(endpoint, async (request: Route) => {
      expect(request.request().headers()['idempotency-key']).toBeTruthy();
      expect(request.request().headers()['if-match']).toBeTruthy();
      await request.fulfill({ status: 200, headers: { etag: '"project-501-v2"' }, contentType: 'application/json', body: JSON.stringify({ estadoNuevo: 'PRODUCTO_APROBADO' }) });
    });
    await page.goto(`${baseUrl}${route}`);
    await page.getByLabel('Documento formal de aprobación').fill('801');
    await page.getByLabel('Tipo de producto final aprobado').selectOption('SOLUCION_FUNCIONAL');
    await page.getByRole('button', { name: 'Confirmar decisión de producto' }).click();
  });

  test.each([409, 412])('anuncia ProblemDetail %s sin navegar fuera del recorrido', async ({ page }, status) => {
    await page.route(endpoint, async (request: Route) => request.fulfill({ status, contentType: 'application/problem+json', body: JSON.stringify({ status, code: status === 409 ? 'STATE_TRANSITION_NOT_ALLOWED' : 'STATE_CHANGED', detail: 'Conflicto de estado.' }) }));
    await page.goto(`${baseUrl}${route}`);
    await page.getByRole('button', { name: 'Confirmar decisión de producto' }).click();
    await expect(page.getByRole('alert')).toBeVisible();
    await expect(page).toHaveURL(new RegExp(`${route}$`));
  });
});
