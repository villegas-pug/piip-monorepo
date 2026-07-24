// T096 · Ruta anónima ya reservada por app.routes; detalle se completa en T101/T102.
import { expect, test } from '@playwright/test';

test('US7 pública: no redirige a autenticación ni muestra acciones de descarga', async ({ page }) => {
  await page.goto('http://localhost:4200/consulta-publica');
  await expect(page).not.toHaveURL(/auth\/callback/);
  await expect(page.getByRole('link', { name: /descargar/i })).toHaveCount(0);
});
