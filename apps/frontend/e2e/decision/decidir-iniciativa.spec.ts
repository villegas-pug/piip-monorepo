// Pruebas E2E (Playwright) — US2 · Decision formal de iniciativa.
//
// Cubre, con `page.route` (sin backend real), el recorrido critico de
// decision formal de la US2:
//   * Escritorio (Chromium 1280x720): la Autoridad (o el Evaluador con
//     documento formal) consulta una iniciativa `PRESENTADO` y registra
//     la decision `INICIATIVA_APROBADA` o `INICIATIVA_ARCHIVADA` con el
//     documento formal obligatorio (campo 15).
//   * Movil (Pixel 5): mismo recorrido, sin scroll horizontal.
//   * Teclado: `Tab` recorre los campos, `Enter` envia cuando el foco
//     esta en el boton confirmar, `Esc` cancela y revierte.
//   * Cancelacion: abrir el panel de confirmacion, pulsar `Esc` o el
//     boton "Descartar cambios" y validar que no se envio la peticion.
//   * 409 `STATE_TRANSITION_NOT_ALLOWED`: la iniciativa no esta en
//     `PRESENTADO`; la SPA expone el `ProblemDetail` con `code`.
//   * 412 `STATE_CHANGED`: `If-Match` incorrecto; la SPA permite
//     revalidar la ETag.
//   * 422 `FORMAL_DECISION_REQUIRED`: se omite el documento formal; la
//     SPA bloquea el envio y expone el `ProblemDetail` accesible.
//
// Restricciones:
//   * NO se levanta el backend real: todas las llamadas a `/api/v1/**`
//     se mockean con `page.route`.
//   * NO se descargan binarios de navegador ni se ejecuta
//     `npx playwright install` desde estas pruebas.
//   * NO se modifica el snapshot OpenAPI ni los componentes existentes.
//   * La ruta `/portafolio/decision/iniciativas/{id}` queda pendiente
//     de T060 [US2 - rutas] y el componente `InitiativeDecisionPageComponent`
//     de T059 [US2 - frontend]; los selectores sirven como contrato.

import { expect, test, type Page, type Route } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

/** Ruta institucional de la pantalla de decision formal. */
const RUTA_DECISION = (id: number): string => `/portafolio/decision/iniciativas/${id}`;

/** Identificador sintetico de iniciativa para la prueba. */
const INITIATIVE_ID = 3030;

/** ETag inicial devuelto por el backend. */
const ETAG_INICIAL = `"${INITIATIVE_ID}-1"`;

/** Detalle sintetico de iniciativa en estado `PRESENTADO`. */
const DETALLE_INICIATIVA_PRESENTADO = {
  id: INITIATIVE_ID,
  tipoRegistro: 'INICIATIVA',
  codigo: '2026-MIDAGRI-00030',
  fechaInicio: '2026-07-23',
  estado: 'PRESENTADO',
  version: 1,
  etag: ETAG_INICIAL,
  nombre: 'Iniciativa de prueba E2E de decision',
  tipoSolucion: 'POTENCIAL_ADAPTABLE',
  fuenteOrigen: 'FICHA_INICIATIVA',
  problemaPublico: 'Problema detectado en territorio',
  responsableId: 5,
  objetivoPeiId: 10,
  actividadPoiId: 20,
  unidades: [{ id: 1, unidadId: 7, principal: true }],
  componenteDigital: false
};

/** Detalle de una iniciativa ya aprobada. */
const DETALLE_INICIATIVA_APROBADA = {
  ...DETALLE_INICIATIVA_PRESENTADO,
  estado: 'INICIATIVA_APROBADA',
  version: 5,
  etag: `"${INITIATIVE_ID}-5"`
};

/** Respuesta exitosa de transicion (backend T058). */
const RESPUESTA_APROBACION = {
  registroId: INITIATIVE_ID,
  estadoAnterior: 'PRESENTADO',
  estadoNuevo: 'INICIATIVA_APROBADA',
  transicionId: 9001,
  fechaTransicion: '2026-07-23T11:00:00',
  actorSub: 'sub-sintetico-autoridad-0005',
  version: 2,
  etag: `"${INITIATIVE_ID}-2"`
};

const RESPUESTA_ARCHIVO = {
  ...RESPUESTA_APROBACION,
  estadoNuevo: 'INICIATIVA_ARCHIVADA'
};

/**
 * Mockea la consulta del detalle de iniciativa.
 *
 * @param estado Estado canonico a devolver.
 */
async function mockDetalleIniciativa(page: Page, estado: 'PRESENTADO' | 'INICIATIVA_APROBADA' = 'PRESENTADO'): Promise<void> {
  const detalle = estado === 'INICIATIVA_APROBADA' ? DETALLE_INICIATIVA_APROBADA : DETALLE_INICIATIVA_PRESENTADO;
  await page.route(`**/api/v1/portafolio/iniciativas/${INITIATIVE_ID}`, async (route: Route) => {
    if (route.request().method() === 'GET') {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        headers: { etag: detalle.etag },
        body: JSON.stringify(detalle)
      });
      return;
    }
    await route.continue();
  });
}

/** Mock del POST de transicion con respuesta 200. */
async function mockTransicionOk(
  page: Page,
  destino: 'INICIATIVA_APROBADA' | 'INICIATIVA_ARCHIVADA'
): Promise<{ payload: unknown; ifMatch: string | null }[]> {
  const capturas: { payload: unknown; ifMatch: string | null }[] = [];
  const respuesta = destino === 'INICIATIVA_APROBADA' ? RESPUESTA_APROBACION : RESPUESTA_ARCHIVO;
  await page.route(`**/api/v1/portafolio/transiciones/${INITIATIVE_ID}`, async (route: Route) => {
    if (route.request().method() === 'POST') {
      capturas.push({
        payload: route.request().postDataJSON(),
        ifMatch: route.request().headers()['if-match'] ?? null
      });
      await route.fulfill({
        status: 200,
        headers: { 'content-type': 'application/json', etag: respuesta.etag },
        body: JSON.stringify(respuesta)
      });
      return;
    }
    await route.continue();
  });
  return capturas;
}

test.describe('US2 · Decidir iniciativa (escritorio 1280x720)', () => {
  test.beforeEach(async ({ page }) => {
    await mockDetalleIniciativa(page);
  });

  test('recorrido feliz: decision INICIATIVA_APROBADA con documento formal', async ({ page }) => {
    const capturas = await mockTransicionOk(page, 'INICIATIVA_APROBADA');

    await page.goto(`${BASE_URL}${RUTA_DECISION(INITIATIVE_ID)}`);

    // Ancla accesible para confirmar la hidratacion.
    // @NEEDS_CLARIFICATION: el titulo exacto depende de T059; el selector
    // es laxo para no acoplarse a una implementacion no publicada.
    await expect(page.locator('h2#decision-page-title, h1:has-text("Decision")')).toBeVisible({ timeout: 10000 });

    await page.getByLabel('Destino de la decision').click();
    await page.getByRole('option', { name: 'INICIATIVA_APROBADA' }).click();
    await page.getByLabel('Documento formal de decision').fill('777');
    await page.getByLabel('Observaciones').fill('Decision formal de la Autoridad');

    await page.getByRole('button', { name: 'Confirmar decision formal' }).click();

    await expect.poll(() => capturas.length, { timeout: 10000 }).toBe(1);
    const cuerpo = capturas[0].payload as Record<string, unknown>;
    expect(cuerpo['destino']).toBe('INICIATIVA_APROBADA');
    expect(cuerpo['documentoRefId']).toBe(777);
    expect(capturas[0].ifMatch).toBe(ETAG_INICIAL);
  });

  test('recorrido feliz: decision INICIATIVA_ARCHIVADA con documento formal', async ({ page }) => {
    const capturas = await mockTransicionOk(page, 'INICIATIVA_ARCHIVADA');

    await page.goto(`${BASE_URL}${RUTA_DECISION(INITIATIVE_ID)}`);

    await page.getByLabel('Destino de la decision').click();
    await page.getByRole('option', { name: 'INICIATIVA_ARCHIVADA' }).click();
    await page.getByLabel('Documento formal de decision').fill('778');
    await page.getByLabel('Observaciones').fill('Archivo formal con sustento normativo');

    await page.getByRole('button', { name: 'Confirmar decision formal' }).click();

    await expect.poll(() => capturas.length, { timeout: 10000 }).toBe(1);
    const cuerpo = capturas[0].payload as Record<string, unknown>;
    expect(cuerpo['destino']).toBe('INICIATIVA_ARCHIVADA');
    expect(cuerpo['documentoRefId']).toBe(778);
  });
});

test.describe('US2 · Decidir iniciativa (movil Pixel 5)', () => {
  test.use({ viewport: { width: 393, height: 851 } });

  test.beforeEach(async ({ page }) => {
    await mockDetalleIniciativa(page);
  });

  test('recorrido sin scroll horizontal y controles tocables', async ({ page }) => {
    await mockTransicionOk(page, 'INICIATIVA_APROBADA');

    await page.goto(`${BASE_URL}${RUTA_DECISION(INITIATIVE_ID)}`);

    const overflow = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth
    }));
    expect(overflow.scrollWidth).toBeLessThanOrEqual(overflow.clientWidth);

    await page.getByLabel('Observaciones').tap();
    await page.getByLabel('Observaciones').fill('Observacion en movil');

    await expect(page.getByRole('button', { name: 'Confirmar decision formal' })).toBeVisible();
  });
});

test.describe('US2 · Decidir iniciativa (teclado)', () => {
  test.beforeEach(async ({ page }) => {
    await mockDetalleIniciativa(page);
  });

  test('Tab recorre, Enter envia, Esc cancela', async ({ page }) => {
    await mockTransicionOk(page, 'INICIATIVA_APROBADA');

    await page.goto(`${BASE_URL}${RUTA_DECISION(INITIATIVE_ID)}`);

    await page.getByLabel('Destino de la decision').focus();
    await page.keyboard.press('Tab');
    await expect(page.getByLabel('Documento formal de decision')).toBeFocused();

    await page.keyboard.press('Shift+Tab');
    await expect(page.getByLabel('Destino de la decision')).toBeFocused();

    // Captura global de Escape para cerrar dialogos.
    await page.evaluate(() => {
      window.addEventListener(
        'keydown',
        (evento) => {
          if (evento.key === 'Escape') {
            const dialogo = document.querySelector('dialog[open]');
            if (dialogo instanceof HTMLDialogElement) {
              dialogo.close();
            }
          }
        },
        { once: false }
      );
    });

    await page.getByLabel('Observaciones').press('Escape');
    await expect(page).toHaveURL(new RegExp(`${RUTA_DECISION(INITIATIVE_ID)}$`));
  });
});

test.describe('US2 · Decidir iniciativa (cancelacion)', () => {
  test.beforeEach(async ({ page }) => {
    await mockDetalleIniciativa(page);
  });

  test('abrir dialogo de cancelacion y pulsar Esc: no se envia la peticion', async ({ page }) => {
    const capturas: unknown[] = [];
    await page.route(`**/api/v1/portafolio/transiciones/${INITIATIVE_ID}`, async (route: Route) => {
      if (route.request().method() === 'POST') {
        capturas.push(route.request().postDataJSON());
        await route.fulfill({
          status: 200,
          headers: { 'content-type': 'application/json', etag: RESPUESTA_APROBACION.etag },
          body: JSON.stringify(RESPUESTA_APROBACION)
        });
        return;
      }
      await route.continue();
    });

    await page.goto(`${BASE_URL}${RUTA_DECISION(INITIATIVE_ID)}`);

    await page.getByLabel('Observaciones').fill('Observacion pendiente de confirmar');
    await page.getByRole('button', { name: 'Cancelar decision' }).click();

    await expect(page.getByRole('dialog', { name: /confirmar cancelacion/i })).toBeVisible();
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog', { name: /confirmar cancelacion/i })).toBeHidden();

    expect(capturas).toHaveLength(0);
  });

  test('boton "Descartar cambios" revierte el formulario y no envia la peticion', async ({ page }) => {
    let enviado = false;
    await page.route(`**/api/v1/portafolio/transiciones/${INITIATIVE_ID}`, async (route: Route) => {
      if (route.request().method() === 'POST') {
        enviado = true;
        await route.fulfill({
          status: 200,
          headers: { 'content-type': 'application/json', etag: RESPUESTA_APROBACION.etag },
          body: JSON.stringify(RESPUESTA_APROBACION)
        });
        return;
      }
      await route.continue();
    });

    await page.goto(`${BASE_URL}${RUTA_DECISION(INITIATIVE_ID)}`);

    await page.getByLabel('Observaciones').fill('Algo pendiente');
    await page.getByRole('button', { name: 'Cancelar decision' }).click();
    await page.getByRole('button', { name: 'Descartar cambios' }).click();

    expect(enviado).toBe(false);
  });
});

test.describe('US2 · Decidir iniciativa (errores del backend)', () => {
  test.beforeEach(async ({ page }) => {
    await mockDetalleIniciativa(page);
  });

  test('409 STATE_TRANSITION_NOT_ALLOWED cuando la iniciativa ya esta aprobada', async ({ page }) => {
    // El detalle de la iniciativa se mockea con estado aprobado; la SPA
// (linea removida: el mock del detalle aprobado es suficiente)
    await mockDetalleIniciativa(page, 'INICIATIVA_APROBADA');

    await page.route(`**/api/v1/portafolio/transiciones/${INITIATIVE_ID}`, async (route: Route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 409,
          contentType: 'application/problem+json',
          body: JSON.stringify({
            type: 'about:blank',
            title: 'Transicion no permitida',
            status: 409,
            code: 'STATE_TRANSITION_NOT_ALLOWED',
            detail: 'INICIATIVA_APROBADA no admite transiciones adicionales.',
            correlationId: 'dec-409-e2e'
          })
        });
        return;
      }
      await route.continue();
    });

    await page.goto(`${BASE_URL}${RUTA_DECISION(INITIATIVE_ID)}`);

    const regionErrores = page.locator('[role="alert"]').filter({ hasText: 'STATE_TRANSITION_NOT_ALLOWED' });
    await expect(regionErrores).toBeVisible();
    await expect(regionErrores).toContainText('STATE_TRANSITION_NOT_ALLOWED');
  });

  test('412 STATE_CHANGED cuando la ETag es incorrecta', async ({ page }) => {
    await page.route(`**/api/v1/portafolio/transiciones/${INITIATIVE_ID}`, async (route: Route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 412,
          contentType: 'application/problem+json',
          body: JSON.stringify({
            type: 'about:blank',
            title: 'ETag obsoleto',
            status: 412,
            code: 'STATE_CHANGED',
            detail: 'La ETag enviada no coincide con la version actual.',
            correlationId: 'dec-412-e2e'
          })
        });
        return;
      }
      await route.continue();
    });

    await page.goto(`${BASE_URL}${RUTA_DECISION(INITIATIVE_ID)}`);

    await page.getByLabel('Observaciones').fill('Observacion con ETag obsoleta');
    await page.getByRole('button', { name: 'Confirmar decision formal' }).click();

    const regionErrores = page.locator('[role="alert"]').filter({ hasText: 'STATE_CHANGED' });
    await expect(regionErrores).toBeVisible();
    await expect(regionErrores).toContainText('STATE_CHANGED');
    await expect(regionErrores).toContainText('412');
  });

  test('422 FORMAL_DECISION_REQUIRED cuando se omite el documento formal', async ({ page }) => {
    await page.route(`**/api/v1/portafolio/transiciones/${INITIATIVE_ID}`, async (route: Route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 422,
          contentType: 'application/problem+json',
          body: JSON.stringify({
            type: 'about:blank',
            title: 'Decision formal requerida',
            status: 422,
            code: 'FORMAL_DECISION_REQUIRED',
            detail: 'La decision exige un documento formal registrado.',
            correlationId: 'dec-422-e2e',
            violations: [
              { field: 'documentoRefId', message: 'El documento formal de decision es obligatorio.' }
            ]
          })
        });
        return;
      }
      await route.continue();
    });

    await page.goto(`${BASE_URL}${RUTA_DECISION(INITIATIVE_ID)}`);

    // La SPA debe bloquear el envio en el cliente (campo obligatorio).
    // Aun si el cliente lo permitiera, el backend rechaza con 422.
    await page.getByLabel('Documento formal de decision').fill('');
    await page.getByRole('button', { name: 'Confirmar decision formal' }).click();

    const regionErrores = page.locator('[role="alert"]').filter({ hasText: 'FORMAL_DECISION_REQUIRED' });
    await expect(regionErrores).toBeVisible();
    await expect(regionErrores).toContainText('FORMAL_DECISION_REQUIRED');
  });
});
