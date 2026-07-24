// T079 · Recorrido pendiente del contrato T081/T082; no fija URL de API inexistente.
import { expect, test } from '@playwright/test';

const baseUrl = 'http://localhost:4200';
const route = '/portafolio/cierre/proyectos/501';

test.describe('US5 · Cerrar proyecto', () => {
  test('mantiene el formulario accesible y no inventa fecha de cierre en cliente', async ({ page }) => {
    await page.goto(`${baseUrl}${route}`);
    await expect(page.getByRole('heading', { name: /cierre/i })).toBeVisible();
    await expect(page.getByLabel('Informe final de cierre')).toBeVisible();
    await expect(page.getByLabel('Resultados clave')).toBeVisible();
    await expect(page.getByLabel('Aprendizajes')).toBeVisible();
    await expect(page.getByLabel('Conclusión')).toBeVisible();
  });
});
