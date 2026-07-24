// T096/T120 · Comprobaciones de semántica antes de integrar el recorrido de consulta.
import { expect, test } from '@playwright/test';

test('consulta pública conserva h1, main y foco visible para navegación por teclado', async ({ page }) => {
  await page.goto('http://localhost:4200/consulta-publica');
  await expect(page.getByRole('main')).toBeVisible();
  await expect(page.getByRole('heading', { level: 1 })).toBeVisible();
  await page.keyboard.press('Tab');
  expect(await page.locator(':focus').count()).toBe(1);
});
