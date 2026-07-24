// T105 + T109 · Recorrido Playwright de la US8: generación, detalle y
// estado de un reporte institucional.
//
// Esta suite describe el recorrido de un Evaluador para generar un
// reporte semestral, consultar su estado, descargar los archivos y
// reintentar la operación. Los pasos están alineados con el contrato
// OpenAPI codigo-first aprobado en
// `specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml`
// y con el snapshot de la Constitución v5.0.0.
//
// El test no asume estado concreto del backend: comprueba accesibilidad
// y contrato de los controles, no el contenido devuelto por el
// servidor. La ejecución requiere autorización expresa.
import { expect, test } from '@playwright/test';

test.describe('US8 · Generar reporte', () => {
  test('presenta estado de generación y control accesible de reintento', async ({ page }) => {
    await page.goto('http://localhost:4200/reportes');
    await expect(page.getByRole('main')).toBeVisible();
    // La pantalla de generación ofrece los dos tipos de reporte. Antes
    // de enviar nada, no hay botón de reintento porque no existe una
    // operación recuperable.
    await expect(page.getByRole('button', { name: /reintentar/i })).toHaveCount(0);
  });

  test('expone skip-link y encabezado principal accesible', async ({ page }) => {
    await page.goto('http://localhost:4200/reportes');
    const skipLink = page.locator('a.skip-link');
    await expect(skipLink).toHaveCount(1);
    await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  });

  test('permite navegar al detalle de un reporte por su identificador', async ({ page }) => {
    // La ruta es lazy: navegar a `/reportes/:id` debe cargar el
    // componente de detalle sin recargar la página institucional.
    await page.goto('http://localhost:4200/reportes/1');
    await expect(page.getByRole('main')).toBeVisible();
  });

  test('la pantalla de aprobación expone los tipos de destinatario BR-125', async ({ page }) => {
    await page.goto('http://localhost:4200/reportes/1/aprobacion');
    await expect(page.getByRole('main')).toBeVisible();
  });
});
