// Pruebas E2E (Playwright + axe-core) — accesibilidad WCAG 2.1 AA del recorrido US1.
//
// Cubre las seis rutas institucionales del recorrido de registro:
//   1. /portafolio/registro/iniciativas/nueva
//   2. /portafolio/registro/iniciativas/:id
//   3. /portafolio/registro/iniciativas/:id/subsanacion
//   4. /portafolio/registro/incorporaciones/nueva
//   5. /portafolio/registro/incorporaciones/:id
//   6. /portafolio/registro/incorporaciones/:id/correccion
//
// Reglas:
//   * Tags WCAG 2.1 AA: wcag2a, wcag2aa, wcag21a, wcag21aa.
//   * La prueba FALLA si axe-core detecta violaciones `critical` o `serious`.
//   * Las violaciones `moderate` y `minor` se documentan mediante JSDoc y
//     NO bloquean el recorrido; su seguimiento se gestiona en issues
//     específicos fuera del alcance de esta tarea.
//
// Restricciones:
//   * NO se levanta el backend real: las llamadas a `/api/v1/**` se mockean
//     para que la SPA renderice la página esperada y axe-core pueda
//     inspeccionarla.
//   * NO se descarga axe-core desde internet: el paquete `@axe-core/playwright`
//     ya está declarado en `devDependencies` y la versión se mantiene
//     alineada con `axe-core` para evitar inconsistencias de catálogo.
//   * NO se ejecuta `npx playwright test` desde esta tarea: las pruebas se
//     depositan y se ejecutarán únicamente con autorización humana.

import AxeBuilder from '@axe-core/playwright';
import { expect, test, type Page, type Route } from '@playwright/test';

const BASE_URL = 'http://localhost:4200';

/** Etiquetas WCAG 2.1 AA que axe-core debe evaluar en cada ruta. */
const TAGS_WCAG_21_AA = ['wcag2a', 'wcag2aa', 'wcag21a', 'wcag21aa'] as const;

/** IDs sintéticos usados por las pruebas para enrutar mocks. */
const INITIATIVE_ID = 2026;
const INCORPORATION_ID = 42;

/** Detalle sintético de iniciativa para mantener la página renderizada. */
const DETALLE_INICIATIVA = {
  id: INITIATIVE_ID,
  tipoRegistro: 'INICIATIVA',
  codigo: '2026-MIDAGRI-00001',
  fechaInicio: '2026-07-23',
  estado: 'PRESENTADO',
  version: 1,
  etag: 'W/"1"',
  nombre: 'Iniciativa de prueba E2E',
  tipoSolucion: 'POTENCIAL_ADAPTABLE',
  fuenteOrigen: 'FICHA_INICIATIVA',
  problemaPublico: 'Problema detectado',
  responsableId: 5,
  objetivoPeiId: 10,
  actividadPoiId: 20,
  unidades: [{ id: 1, unidadId: 7, principal: true }],
  componenteDigital: false
};

/** Detalle sintético de incorporación para mantener la página renderizada. */
const DETALLE_INCORPORACION = {
  id: INCORPORATION_ID,
  fuente: 'Fuente externa',
  hashOriginal: 'a'.repeat(64),
  estado: 'PENDIENTE',
  version: 1,
  etag: 'W/"1"'
};

/**
 * Mockea los endpoints institucionales mínimos para que la SPA pueda
 * renderizar la página objetivo sin necesidad del backend real. Las
 * respuestas vacías son válidas: las páginas deben manejar catálogos
 * vacíos sin romper la composición accesible.
 */
async function mockInstitucionalMinimo(page: Page): Promise<void> {
  await page.route('**/api/v1/organizacion/**', async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 })
    });
  });
  await page.route(`**/api/v1/portafolio/iniciativas/${INITIATIVE_ID}`, async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(DETALLE_INICIATIVA)
    });
  });
  await page.route(`**/api/v1/portafolio/incorporaciones/${INCORPORATION_ID}`, async (route: Route) => {
    await route.fulfill({
      status: 200,
      contentType: 'application/json',
      body: JSON.stringify(DETALLE_INCORPORACION)
    });
  });
}

/**
 * Construye un informe legible de las violaciones detectadas por axe-core.
 * Separa `critical` y `serious` (bloqueantes) de `moderate` y `minor`
 * (documentadas).
 */
function resumirViolaciones(violaciones: Array<{ id: string; impact?: string | null; description: string; helpUrl: string }>): {
  bloqueantes: typeof violaciones;
  documentadas: typeof violaciones;
} {
  const bloqueantes: typeof violaciones = [];
  const documentadas: typeof violaciones = [];
  for (const violacion of violaciones) {
    if (violacion.impact === 'critical' || violacion.impact === 'serious') {
      bloqueantes.push(violacion);
    } else {
      documentadas.push(violacion);
    }
  }
  return { bloqueantes, documentadas };
}

/**
 * Catálogo de las seis rutas US1 que se verifican con axe-core. Cada
 * entrada incluye la ruta y un identificador legible para los reportes.
 */
const RUTAS_US1 = [
  {
    id: 'iniciativas-nueva',
    ruta: '/portafolio/registro/iniciativas/nueva',
    descripcion: 'Presentación de iniciativa'
  },
  {
    id: 'iniciativas-detalle',
    ruta: `/portafolio/registro/iniciativas/${INITIATIVE_ID}`,
    descripcion: 'Edición de iniciativa'
  },
  {
    id: 'iniciativas-subsanacion',
    ruta: `/portafolio/registro/iniciativas/${INITIATIVE_ID}/subsanacion`,
    descripcion: 'Subsanación de iniciativa'
  },
  {
    id: 'incorporaciones-nueva',
    ruta: '/portafolio/registro/incorporaciones/nueva',
    descripcion: 'Nueva incorporación individual'
  },
  {
    id: 'incorporaciones-detalle',
    ruta: `/portafolio/registro/incorporaciones/${INCORPORATION_ID}`,
    descripcion: 'Edición de incorporación individual'
  },
  {
    id: 'incorporaciones-correccion',
    ruta: `/portafolio/registro/incorporaciones/${INCORPORATION_ID}/correccion`,
    descripcion: 'Corrección de incorporación individual'
  }
] as const;

test.describe('US1 · accesibilidad WCAG 2.1 AA (axe-core)', () => {
  for (const objetivo of RUTAS_US1) {
    test(`ruta "${objetivo.id}" cumple WCAG 2.1 AA`, async ({ page }) => {
      await mockInstitucionalMinimo(page);

      await page.goto(`${BASE_URL}${objetivo.ruta}`);

      // Espera a que Angular hidrate la página objetivo. Cada componente
      // expone un encabezado accesible propio que sirve de ancla.
      await expect(
        page.locator('h2#initiative-form-title, h2#incorporation-title, h2#correction-form-title')
      ).toBeVisible({ timeout: 10000 });

      // Construye el analizador axe-core con las etiquetas WCAG 2.1 AA.
      const accesibilidad = await new AxeBuilder({ page })
        .withTags([...TAGS_WCAG_21_AA])
        // Excluye la región que el SDK de Angular Material puede renderizar
        // con clases `cdk-*` que axe-core no clasifica como críticas.
        .disableRules([])
        .analyze();

      const { bloqueantes, documentadas } = resumirViolaciones(
        accesibilidad.violations as Array<{ id: string; impact?: string | null; description: string; helpUrl: string }>
      );

      // Las violaciones `moderate` y `minor` se documentan en consola y
      // mediante JSDoc; NO bloquean la prueba. Su seguimiento se gestiona
      // en issues específicos fuera del alcance de T043.
      // JSDoc: las violaciones `moderate` y `minor` no rompen el recorrido
      // crítico pero se registran para remediación priorizada.
      if (documentadas.length > 0) {
        // eslint-disable-next-line no-console
        console.info(
          `[a11y] ${objetivo.id} documenta ${documentadas.length} violaciones moderate/minor:`,
          documentadas.map((v) => v.id)
        );
      }

      // Falla si hay violaciones `critical` o `serious`.
      expect(
        bloqueantes,
        `La ruta ${objetivo.ruta} (${objetivo.descripcion}) presenta ${bloqueantes.length} violaciones WCAG 2.1 AA bloqueantes: ` +
          bloqueantes.map((v) => `${v.id} (${v.impact})`).join(', ')
      ).toEqual([]);
    });
  }
});
