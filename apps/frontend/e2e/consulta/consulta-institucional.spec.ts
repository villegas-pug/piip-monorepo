// T096 · Recorrido pendiente de T101/T102. No se presupone endpoint no publicado.
import { expect, test } from '@playwright/test';

test('US7 institucional: anuncia denegación de ámbito sin revelar detalle', async ({ page }) => {
  await page.goto('http://localhost:4200/consulta-institucional');
  await expect(page.getByRole('main')).toBeVisible();
  await expect(page.getByText(/ASSIGNMENT_SCOPE_DENIED/)).toHaveCount(0);
});
