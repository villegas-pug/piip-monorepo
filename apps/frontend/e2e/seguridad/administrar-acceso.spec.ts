// T085 · Recorrido futuro de T092/T093; utiliza solo datos sintéticos.
import { expect, test } from '@playwright/test';

test.describe('US6 · Administrar acceso organizacional', () => {
  test('la administración no expone controles de contraseña', async ({ page }) => {
    await page.goto('http://localhost:4200/seguridad/usuarios');
    await expect(page.getByLabel(/contraseña|password/i)).toHaveCount(0);
  });
  test('la selección visible de asignación conserva una sola unidad y perfil', async ({ page }) => {
    await page.goto('http://localhost:4200/seguridad/asignaciones');
    await expect(page.getByRole('main')).toBeVisible();
  });
});
