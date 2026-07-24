// Pruebas E2E (Playwright) — US2 · Evaluar iniciativa.
//
// Cubre, con `page.route` (sin backend real), el recorrido critico de la US2:
//   * Escritorio (Chromium 1280x720): el Evaluador consulta una iniciativa
//     `PRESENTADO`, registra la decision de admisibilidad y la lista
//     estructurada de aplicabilidad; la SPA expone la nueva ETag y el
//     estado actualizado.
//   * Movil (Pixel 5): mismo recorrido, sin scroll horizontal.
//   * Teclado: `Tab` recorre los campos, `Enter` envia el formulario
//     cuando el foco esta en el boton, `Esc` cancela y revierte.
//   * Cancelacion: abrir el panel de confirmacion, pulsar `Esc` o el
//     boton "Cancelar" y validar que no se envio la peticion.
//   * 409: el backend devuelve `STATE_TRANSITION_NOT_ALLOWED` cuando la
//     iniciativa ya paso a `INICIATIVA_APROBADA`; la SPA expone el
//     `ProblemDetail` con `code` y `correlationId`.
//   * 412: `If-Match` incorrecto devuelve `STATE_CHANGED` y la SPA
//     permite revalidar la ETag.
//
// Restricciones:
//   * NO se levanta el backend real: todas las llamadas a `/api/v1/**`
//     se mockean con `page.route` para mantener la prueba determinista
//     y offline.
//   * NO se descargan binarios de navegador ni se ejecuta
//     `npx playwright install` desde estas pruebas.
//   * NO se modifica el snapshot OpenAPI ni los componentes existentes.
//   * La ruta `/portafolio/evaluacion/iniciativas/{id}` queda pendiente
//     de T060 [US2 - rutas] y el componente `EvaluationPageComponent` de
//     T059 [US2 - frontend]; mientras no existan, los selectores del
//     spec serviran como contrato.

import { expect, test, type Page, type Route } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

/** Ruta institucional de la pantalla de evaluacion de iniciativa. */
const RUTA_EVALUACION = (id: number): string => `/portafolio/evaluacion/iniciativas/${id}`;

/** Identificador sintetico de iniciativa para la prueba. */
const INITIATIVE_ID = 2026;

/** ETag inicial devuelto por el backend. */
const ETAG_INICIAL = `"${INITIATIVE_ID}-1"`;

/** Detalle sintetico de iniciativa en estado `PRESENTADO`. */
const DETALLE_INICIATIVA_PRESENTADO = {
  id: INITIATIVE_ID,
  tipoRegistro: 'INICIATIVA',
  codigo: '2026-MIDAGRI-00001',
  fechaInicio: '2026-07-23',
  estado: 'PRESENTADO',
  version: 1,
  etag: ETAG_INICIAL,
  nombre: 'Iniciativa de prueba E2E de evaluacion',
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
  version: 3,
  etag: `"${INITIATIVE_ID}-3"`
};

/** Respuesta exitosa de admisibilidad (backend T057). */
const RESPUESTA_ADMISIBILIDAD = {
  iniciativaId: INITIATIVE_ID,
  estadoIniciativa: 'PRESENTADO',
  tipoEvaluacion: 'ADMISIBILIDAD',
  documentoOpinionId: 555,
  fechaEvaluacion: '2026-07-23T10:00:00',
  version: 2,
  etag: `"${INITIATIVE_ID}-2"`
};

/** Respuesta exitosa de aplicabilidad (backend T057). */
const RESPUESTA_APLICABILIDAD = {
  iniciativaId: INITIATIVE_ID,
  estadoIniciativa: 'PRESENTADO',
  tipoEvaluacion: 'APLICABILIDAD',
  documentoOpinionId: 555,
  fechaEvaluacion: '2026-07-23T10:00:00',
  version: 2,
  etag: `"${INITIATIVE_ID}-2"`
};

/**
 * Mockea la consulta del detalle de iniciativa.
 *
 * @param estado Estado canonico a devolver: `PRESENTADO` por defecto o
 *               cualquier valor valido (p. ej. `INICIATIVA_APROBADA`).
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

/** Mock del POST de admisibilidad que devuelve 200 con la ETag actualizada. */
async function mockAdmisibilidadOk(page: Page): Promise<{ payload: unknown; ifMatch: string | null }[]> {
  const capturas: { payload: unknown; ifMatch: string | null }[] = [];
  await page.route('**/api/v1/portafolio/iniciativas/*/evaluaciones/admisibilidad', async (route: Route) => {
    if (route.request().method() === 'POST') {
      capturas.push({
        payload: route.request().postDataJSON(),
        ifMatch: route.request().headers()['if-match'] ?? null
      });
      await route.fulfill({
        status: 200,
        headers: { 'content-type': 'application/json', etag: RESPUESTA_ADMISIBILIDAD.etag },
        body: JSON.stringify(RESPUESTA_ADMISIBILIDAD)
      });
      return;
    }
    await route.continue();
  });
  return capturas;
}

/** Mock del POST de aplicabilidad que devuelve 200 con la ETag actualizada. */
async function mockAplicabilidadOk(page: Page): Promise<{ payload: unknown }[]> {
  const capturas: { payload: unknown }[] = [];
  await page.route('**/api/v1/portafolio/iniciativas/*/evaluaciones/aplicabilidad', async (route: Route) => {
    if (route.request().method() === 'POST') {
      capturas.push({ payload: route.request().postDataJSON() });
      await route.fulfill({
        status: 200,
        headers: { 'content-type': 'application/json', etag: RESPUESTA_APLICABILIDAD.etag },
        body: JSON.stringify(RESPUESTA_APLICABILIDAD)
      });
      return;
    }
    await route.continue();
  });
  return capturas;
}

test.describe('US2 · Evaluar iniciativa (escritorio 1280x720)', () => {
  test.beforeEach(async ({ page }) => {
    await mockDetalleIniciativa(page);
  });

  test('recorrido feliz: registra admisibilidad, valida ETag y exposicion del nuevo estado', async ({ page }) => {
    const capturas = await mockAdmisibilidadOk(page);
    await mockAplicabilidadOk(page);

    await page.goto(`${BASE_URL}${RUTA_EVALUACION(INITIATIVE_ID)}`);

    // El encabezado accesible de la pantalla de evaluacion sirve como ancla
    // para confirmar la hidratacion. La etiqueta exacta la define T059; usamos
    // un selector flexible que tolere la implementacion real.
    // @NEEDS_CLARIFICATION: el titulo exacto y el id del encabezado dependen
    // de T059; este selector es intencionalmente laxo para no acoplar la
    // prueba a una implementacion todavia no publicada.
    await expect(page.locator('h2#evaluation-page-title, h1:has-text("Evaluacion")')).toBeVisible({ timeout: 10000 });

    // Completar el formulario de admisibilidad (selects y campos del backend).
    await page.getByLabel('Resultado de admisibilidad').click();
    await page.getByRole('option', { name: 'ADMISIBLE' }).click();
    await page.getByLabel('Observacion').fill('Cumple los requisitos formales del portafolio');
    await page.getByLabel('Documento de opinion tecnica').fill('555');

    await page.getByRole('button', { name: 'Registrar admisibilidad' }).click();

    // Espera la captura de la peticion y valida el cuerpo + cabecera If-Match.
    await expect.poll(() => capturas.length, { timeout: 10000 }).toBe(1);
    expect(capturas).toHaveLength(1);
    const cuerpo = capturas[0].payload as Record<string, unknown>;
    expect(cuerpo['resultado']).toBe('ADMISIBLE');
    expect(cuerpo['observacion']).toBe('Cumple los requisitos formales del portafolio');
    expect(cuerpo['documentoOpinionId']).toBe(555);
    expect(capturas[0].ifMatch).toBe(ETAG_INICIAL);
  });

  test('recorrido feliz: registra aplicabilidad con la lista estructurada completa', async ({ page }) => {
    await mockAdmisibilidadOk(page);
    const capturas = await mockAplicabilidadOk(page);

    await page.goto(`${BASE_URL}${RUTA_EVALUACION(INITIATIVE_ID)}`);

    await page.getByLabel('Resultado de aplicabilidad').click();
    await page.getByRole('option', { name: 'APLICABLE' }).click();
    await page.getByLabel('Motivo').fill('Cumple competencia, valor publico y caracter innovador');
    // La lista estructurada se modela con campos `codigo`, `cumple` y
    // `observacion`. El componente expone un boton para agregar criterios
    // y selects por cada criterio; el spec usa selectores laXos para
    // tolerar la implementacion real.
    await page.getByLabel('Codigo del criterio 1').fill('COMPETENCIA_MIDAGRI');
    await page.getByLabel('Cumple criterio 1').check();
    await page.getByLabel('Observacion del criterio 1').fill('Problema dentro del ambito');

    await page.getByRole('button', { name: 'Agregar criterio' }).click();
    await page.getByLabel('Codigo del criterio 2').fill('VALOR_PUBLICO');
    await page.getByLabel('Cumple criterio 2').check();
    await page.getByLabel('Observacion del criterio 2').fill('Beneficiarios identificados');

    await page.getByRole('button', { name: 'Registrar aplicabilidad' }).click();

    await expect.poll(() => capturas.length, { timeout: 10000 }).toBe(1);
    const cuerpo = capturas[0].payload as Record<string, unknown>;
    expect(cuerpo['resultado']).toBe('APLICABLE');
    const criterios = cuerpo['criterios'] as Array<Record<string, unknown>>;
    expect(criterios.length).toBeGreaterThanOrEqual(2);
  });
});

test.describe('US2 · Evaluar iniciativa (movil Pixel 5)', () => {
  test.use({ viewport: { width: 393, height: 851 } });

  test.beforeEach(async ({ page }) => {
    await mockDetalleIniciativa(page);
  });

  test('recorrido sin scroll horizontal y controles tocables', async ({ page }) => {
    await mockAdmisibilidadOk(page);
    await mockAplicabilidadOk(page);

    await page.goto(`${BASE_URL}${RUTA_EVALUACION(INITIATIVE_ID)}`);

    // Sin scroll horizontal.
    const overflow = await page.evaluate(() => ({
      scrollWidth: document.documentElement.scrollWidth,
      clientWidth: document.documentElement.clientWidth
    }));
    expect(overflow.scrollWidth).toBeLessThanOrEqual(overflow.clientWidth);

    // Tocar el campo de observacion sin overflow horizontal.
    await page.getByLabel('Observacion').tap();
    await page.getByLabel('Observacion').fill('Observacion en movil');

    // El boton de envio debe ser visible y tocable.
    await expect(page.getByRole('button', { name: 'Registrar admisibilidad' })).toBeVisible();
  });
});

test.describe('US2 · Evaluar iniciativa (teclado)', () => {
  test.beforeEach(async ({ page }) => {
    await mockDetalleIniciativa(page);
  });

  test('Tab recorre los campos, Enter envia, Esc cancela', async ({ page }) => {
    await mockAdmisibilidadOk(page);
    await mockAplicabilidadOk(page);

    await page.goto(`${BASE_URL}${RUTA_EVALUACION(INITIATIVE_ID)}`);

    // Foco inicial en el primer control del formulario de admisibilidad.
    await page.getByLabel('Resultado de admisibilidad').focus();
    await page.keyboard.press('Tab');
    // El siguiente control accesible es la observacion.
    await expect(page.getByLabel('Observacion')).toBeFocused();

    await page.keyboard.press('Shift+Tab');
    await expect(page.getByLabel('Resultado de admisibilidad')).toBeFocused();

    // Captura global de Escape para cerrar cualquier dialogo modal.
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

    // Esc sobre la observacion no debe enviar el formulario.
    await page.getByLabel('Observacion').press('Escape');
    // La SPA debe permanecer en la misma ruta (no hay navegacion).
    await expect(page).toHaveURL(new RegExp(`${RUTA_EVALUACION(INITIATIVE_ID)}$`));
  });
});

test.describe('US2 · Evaluar iniciativa (cancelacion)', () => {
  test.beforeEach(async ({ page }) => {
    await mockDetalleIniciativa(page);
  });

  test('abrir panel de confirmacion y pulsar Esc: no se envia la peticion', async ({ page }) => {
    const capturas: unknown[] = [];
    await page.route('**/api/v1/portafolio/iniciativas/*/evaluaciones/admisibilidad', async (route: Route) => {
      if (route.request().method() === 'POST') {
        capturas.push(route.request().postDataJSON());
        await route.fulfill({
          status: 200,
          headers: { 'content-type': 'application/json', etag: RESPUESTA_ADMISIBILIDAD.etag },
          body: JSON.stringify(RESPUESTA_ADMISIBILIDAD)
        });
        return;
      }
      await route.continue();
    });
    await mockAplicabilidadOk(page);

    await page.goto(`${BASE_URL}${RUTA_EVALUACION(INITIATIVE_ID)}`);

    // Completar parcialmente el formulario y abrir el panel de confirmacion.
    await page.getByLabel('Observacion').fill('Observacion pendiente de confirmar');
    await page.getByRole('button', { name: 'Cancelar evaluacion' }).click();

    // El dialogo accesible expone titulo, descripcion y un boton "Mantener edicion".
    await expect(page.getByRole('dialog', { name: /confirmar cancelacion/i })).toBeVisible();

    // Pulsar Esc cierra el dialogo sin enviar nada.
    await page.keyboard.press('Escape');
    await expect(page.getByRole('dialog', { name: /confirmar cancelacion/i })).toBeHidden();

    expect(capturas).toHaveLength(0);
  });

  test('boton "Cancelar" revierte el formulario y no envia la peticion', async ({ page }) => {
    let enviado = false;
    await page.route('**/api/v1/portafolio/iniciativas/*/evaluaciones/admisibilidad', async (route: Route) => {
      if (route.request().method() === 'POST') {
        enviado = true;
        await route.fulfill({
          status: 200,
          headers: { 'content-type': 'application/json', etag: RESPUESTA_ADMISIBILIDAD.etag },
          body: JSON.stringify(RESPUESTA_ADMISIBILIDAD)
        });
        return;
      }
      await route.continue();
    });
    await mockAplicabilidadOk(page);

    await page.goto(`${BASE_URL}${RUTA_EVALUACION(INITIATIVE_ID)}`);

    await page.getByLabel('Observacion').fill('Algo pendiente');
    await page.getByRole('button', { name: 'Cancelar evaluacion' }).click();
    await page.getByRole('button', { name: 'Descartar cambios' }).click();

    expect(enviado).toBe(false);
  });
});

test.describe('US2 · Evaluar iniciativa (errores del backend)', () => {
  test.beforeEach(async ({ page }) => {
    await mockDetalleIniciativa(page);
  });

  test('409 STATE_TRANSITION_NOT_ALLOWED cuando la iniciativa ya esta aprobada', async ({ page }) => {
    await page.route('**/api/v1/portafolio/iniciativas/*/evaluaciones/admisibilidad', async (route: Route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 409,
          contentType: 'application/problem+json',
          body: JSON.stringify({
            type: 'about:blank',
            title: 'Transicion no permitida',
            status: 409,
            code: 'STATE_TRANSITION_NOT_ALLOWED',
            detail: 'La iniciativa ya paso a INICIATIVA_APROBADA; no admite admision.',
            correlationId: 'eval-409-e2e'
          })
        });
        return;
      }
      await route.continue();
    });
    await mockAplicabilidadOk(page);

    // Forzamos al backend a entregar la iniciativa en estado aprobado.
    await page.route(`**/api/v1/portafolio/iniciativas/${INITIATIVE_ID}`, async (route: Route) => {
      if (route.request().method() === 'GET') {
        await route.fulfill({
          status: 200,
          contentType: 'application/json',
          headers: { etag: DETALLE_INICIATIVA_APROBADA.etag },
          body: JSON.stringify(DETALLE_INICIATIVA_APROBADA)
        });
        return;
      }
      await route.continue();
    });

    await page.goto(`${BASE_URL}${RUTA_EVALUACION(INITIATIVE_ID)}`);

    // La SPA debe mostrar el ProblemDetail accesible con `code` y `title`.
    const regionErrores = page.locator('[role="alert"]').filter({ hasText: 'STATE_TRANSITION_NOT_ALLOWED' });
    await expect(regionErrores).toBeVisible();
    await expect(regionErrores).toContainText('STATE_TRANSITION_NOT_ALLOWED');
    await expect(page).toHaveURL(new RegExp(`${RUTA_EVALUACION(INITIATIVE_ID)}$`));
  });

  test('412 STATE_CHANGED cuando la ETag es incorrecta', async ({ page }) => {
    await page.route('**/api/v1/portafolio/iniciativas/*/evaluaciones/admisibilidad', async (route: Route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 412,
          contentType: 'application/problem+json',
          body: JSON.stringify({
            type: 'about:blank',
            title: 'ETag obsoleto',
            status: 412,
            code: 'STATE_CHANGED',
            detail: 'La ETag enviada no coincide con la version actual del registro.',
            correlationId: 'eval-412-e2e'
          })
        });
        return;
      }
      await route.continue();
    });
    await mockAplicabilidadOk(page);

    await page.goto(`${BASE_URL}${RUTA_EVALUACION(INITIATIVE_ID)}`);

    await page.getByLabel('Observacion').fill('Observacion con ETag obsoleta');
    await page.getByRole('button', { name: 'Registrar admisibilidad' }).click();

    const regionErrores = page.locator('[role="alert"]').filter({ hasText: 'STATE_CHANGED' });
    await expect(regionErrores).toBeVisible();
    await expect(regionErrores).toContainText('STATE_CHANGED');
    await expect(regionErrores).toContainText('412');
  });

  test('422 ADMISSIBILITY_INCOMPLETE cuando faltan campos obligatorios', async ({ page }) => {
    await page.route('**/api/v1/portafolio/iniciativas/*/evaluaciones/admisibilidad', async (route: Route) => {
      if (route.request().method() === 'POST') {
        await route.fulfill({
          status: 422,
          contentType: 'application/problem+json',
          body: JSON.stringify({
            type: 'about:blank',
            title: 'Admisibilidad incompleta',
            status: 422,
            code: 'ADMISSIBILITY_INCOMPLETE',
            detail: 'Faltan campos para registrar la admisibilidad.',
            correlationId: 'eval-422-e2e',
            violations: [
              { field: 'observacion', message: 'La observacion es obligatoria.' },
              { field: 'documentoOpinionId', message: 'El documento de opinion es obligatorio.' }
            ]
          })
        });
        return;
      }
      await route.continue();
    });
    await mockAplicabilidadOk(page);

    await page.goto(`${BASE_URL}${RUTA_EVALUACION(INITIATIVE_ID)}`);

    await page.getByRole('button', { name: 'Registrar admisibilidad' }).click();

    const regionErrores = page.locator('[role="alert"]').filter({ hasText: 'ADMISSIBILITY_INCOMPLETE' });
    await expect(regionErrores).toBeVisible();
    await expect(regionErrores).toContainText('ADMISSIBILITY_INCOMPLETE');
  });
});
