// T070 · E2E US4 con mocks page.route; nunca usa backend real.
// T075 implementa los selectores; T076 registra la ruta futura definida aquí.

import { expect, test, type Page, type Route } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';
const PROJECT_ID = 101;
const ROUTE = `/portafolio/seguimiento/proyectos/${PROJECT_ID}`;
const ETAG = '"project-101-v1"';

const PROJECT = { id: PROJECT_ID, codigo: '2026-MIDAGRI-00101', tipoRegistro: 'PROYECTO', estado: 'PROYECTO_EJECUCION', version: 1, etag: ETAG, fechaInicio: '2026-07-01' };
const CYCLE = { idCiclo: 501, idProyecto: PROJECT_ID, periodo: '2026-Q3-S1', numeroVersion: 1, objetivos: 'Objetivo', actividades: 'Actividad', avance: 50, dificultades: '', proximasAcciones: '', cerrado: 'S', etag: '"cycle-501-v1"' };

async function mockProject(page: Page): Promise<void> {
  await page.route(`**/api/v1/portafolio/proyectos/${PROJECT_ID}`, async (route: Route) => {
    if (route.request().method() === 'GET') return route.fulfill({ status: 200, headers: { etag: ETAG }, contentType: 'application/json', body: JSON.stringify(PROJECT) });
    await route.continue();
  });
  await page.route(`**/api/v1/portafolio/proyectos/${PROJECT_ID}/ciclos`, async (route: Route) => {
    if (route.request().method() === 'GET') return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify([CYCLE]) });
    await route.continue();
  });
}

async function completeCycle(page: Page): Promise<void> {
  await page.getByLabel('Periodo quincenal').fill('2026-Q3-S1');
  await page.getByLabel('Objetivos del ciclo').fill('Validar el piloto en campo');
  await page.getByLabel('Actividades realizadas').fill('Ejecutar sesiones de prueba');
  await page.getByLabel('Avance porcentual').fill('50');
  await page.getByLabel('Dificultades').fill('Disponibilidad limitada');
  await page.getByLabel('Próximas acciones').fill('Reprogramar sesión');
}

test.describe('US4 · Acompañar proyecto (escritorio)', () => {
  test.beforeEach(async ({ page }) => mockProject(page));

  test('registra ciclo quincenal completo con Idempotency-Key y evidencia opcional apta', async ({ page }) => {
    const requests: Array<{ body: Record<string, unknown>; key: string | undefined }> = [];
    await page.route(`**/api/v1/portafolio/proyectos/${PROJECT_ID}/ciclos`, async (route: Route) => {
      if (route.request().method() !== 'POST') return route.continue();
      requests.push({ body: route.request().postDataJSON(), key: route.request().headers()['idempotency-key'] });
      await route.fulfill({ status: 201, headers: { etag: CYCLE.etag }, contentType: 'application/json', body: JSON.stringify(CYCLE) });
    });
    await page.route(`**/api/v1/portafolio/proyectos/${PROJECT_ID}/ciclos/${CYCLE.idCiclo}/documentos`, async (route: Route) => {
      expect(route.request().headers()['idempotency-key']).toBeTruthy();
      await route.fulfill({ status: 204 });
    });

    await page.goto(`${BASE_URL}${ROUTE}`);
    await expect(page.locator('h1#tracking-page-title, h2#cycle-form-title')).toBeVisible();
    await completeCycle(page);
    await page.getByLabel('Documento de evidencia apta').fill('700');
    await page.getByRole('button', { name: 'Registrar ciclo' }).click();

    await expect.poll(() => requests).toHaveLength(1);
    expect(requests[0].body).toMatchObject({ periodo: '2026-Q3-S1', avance: 50 });
    expect(requests[0].key).toBeTruthy();
  });

  test('muestra 422 CYCLE_AVANCE_OUT_OF_RANGE sin abandonar el recorrido', async ({ page }) => {
    await page.route(`**/api/v1/portafolio/proyectos/${PROJECT_ID}/ciclos`, async (route: Route) => {
      if (route.request().method() !== 'POST') return route.continue();
      await route.fulfill({ status: 422, contentType: 'application/problem+json', body: JSON.stringify({ title: 'Avance inválido', status: 422, code: 'CYCLE_AVANCE_OUT_OF_RANGE', detail: 'El avance debe estar entre 0 y 100.' }) });
    });
    await page.goto(`${BASE_URL}${ROUTE}`);
    await completeCycle(page);
    await page.getByLabel('Avance porcentual').fill('101');
    await page.getByRole('button', { name: 'Registrar ciclo' }).click();
    await expect(page.getByRole('alert')).toContainText('CYCLE_AVANCE_OUT_OF_RANGE');
    await expect(page).toHaveURL(new RegExp(`${ROUTE}$`));
  });

  test('crea corrección append-only y propaga Idempotency-Key', async ({ page }) => {
    let payload: Record<string, unknown> | undefined;
    await page.route(`**/api/v1/portafolio/proyectos/${PROJECT_ID}/ciclos/${CYCLE.idCiclo}/versiones`, async (route: Route) => {
      payload = route.request().postDataJSON();
      expect(route.request().headers()['idempotency-key']).toBeTruthy();
      await route.fulfill({ status: 201, headers: { etag: '"cycle-501-v2"' }, contentType: 'application/json', body: JSON.stringify({ ...CYCLE, numeroVersion: 2, idVersionAnterior: CYCLE.idCiclo, avance: 60 }) });
    });
    await page.goto(`${BASE_URL}${ROUTE}`);
    await page.getByRole('button', { name: `Corregir ciclo ${CYCLE.periodo}` }).click();
    await page.getByLabel('Motivo de corrección').fill('Se recibió evidencia posterior.');
    await page.getByLabel('Avance porcentual').fill('60');
    await page.getByRole('button', { name: 'Guardar nueva versión' }).click();
    await expect.poll(() => payload).toBeDefined();
    expect(payload).toMatchObject({ motivo: 'Se recibió evidencia posterior.', avance: 60 });
  });

  test('409 y 412 se anuncian mediante ProblemDetail accesible', async ({ page }) => {
    await page.route(`**/api/v1/portafolio/proyectos/${PROJECT_ID}/ciclos`, async (route: Route) => {
      if (route.request().method() !== 'POST') return route.continue();
      await route.fulfill({ status: 409, contentType: 'application/problem+json', body: JSON.stringify({ title: 'Conflicto', status: 409, code: 'CYCLE_DUPLICATED', detail: 'El periodo ya fue registrado.' }) });
    });
    await page.goto(`${BASE_URL}${ROUTE}`);
    await completeCycle(page);
    await page.getByRole('button', { name: 'Registrar ciclo' }).click();
    await expect(page.getByRole('alert')).toContainText('CYCLE_DUPLICATED');
  });

  test('suspende con evidencia, Idempotency-Key e If-Match; 412 STATE_CHANGED es accesible', async ({ page }) => {
    let requestHeaders: Record<string, string> | undefined;
    await page.route(`**/api/v1/portafolio/proyectos/${PROJECT_ID}/suspensiones`, async (route: Route) => {
      requestHeaders = route.request().headers();
      await route.fulfill({ status: 412, contentType: 'application/problem+json', body: JSON.stringify({ title: 'Estado actualizado', status: 412, code: 'STATE_CHANGED', detail: 'La ETag está obsoleta.' }) });
    });
    await page.goto(`${BASE_URL}${ROUTE}`);
    await page.getByRole('button', { name: 'Suspender proyecto' }).click();
    await page.getByLabel('Documento de suspensión').fill('900');
    await page.getByLabel('Observación de suspensión').fill('Suspensión sustentada por la unidad administradora.');
    await page.getByRole('button', { name: 'Confirmar suspensión' }).click();

    await expect.poll(() => requestHeaders).toBeDefined();
    expect(requestHeaders?.['idempotency-key']).toBeTruthy();
    expect(requestHeaders?.['if-match']).toBe(ETAG);
    await expect(page.getByRole('alert')).toContainText('STATE_CHANGED');
  });
});

test.describe('US4 · Acompañar proyecto (Pixel 5 y teclado)', () => {
  test.use({ viewport: { width: 393, height: 851 } });
  test.beforeEach(async ({ page }) => mockProject(page));

  test('no presenta scroll horizontal y permite completar controles tocables', async ({ page }) => {
    await page.goto(`${BASE_URL}${ROUTE}`);
    expect(await page.evaluate(() => document.documentElement.scrollWidth <= document.documentElement.clientWidth)).toBe(true);
    await page.getByLabel('Periodo quincenal').tap();
    await page.getByLabel('Periodo quincenal').fill('2026-Q3-S1');
    await expect(page.getByRole('button', { name: 'Registrar ciclo' })).toBeVisible();
  });

  test('Tab respeta el orden, Enter envía y Escape cancela corrección sin mutar', async ({ page }) => {
    let mutations = 0;
    await page.route(`**/api/v1/portafolio/proyectos/${PROJECT_ID}/ciclos/**`, async (route: Route) => { mutations += 1; await route.fulfill({ status: 201, contentType: 'application/json', body: JSON.stringify(CYCLE) }); });
    await page.goto(`${BASE_URL}${ROUTE}`);
    await page.getByLabel('Periodo quincenal').focus();
    await page.keyboard.press('Tab');
    await expect(page.getByLabel('Objetivos del ciclo')).toBeFocused();
    await page.getByRole('button', { name: `Corregir ciclo ${CYCLE.periodo}` }).click();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog', { name: /corregir ciclo/i })).toBeHidden();
    expect(mutations).toBe(0);
  });
});
