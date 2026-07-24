import { defineConfig, devices } from '@playwright/test';

/**
 * Configuración E2E de PIIP (Playwright).
 *
 * Alcance:
 * - `testDir` apunta a `e2e/`; los recorridos críticos de US1 (registro) y US3
 *   (creación de proyecto, diferida) se cubren mediante archivos `*.spec.ts`
 *   separados por subcapacidad dentro de `e2e/registro/` y `e2e/accessibility/`.
 * - Proyectos de escritorio (Chromium 1280x720) y móvil (Pixel 5, viewport
 *   institucional 393x851), alineados con los breakpoints del tema PIIP.
 * - `baseURL` local configurable por la variable de entorno `PIIP_E2E_BASE_URL`;
 *   por defecto apunta al servidor de desarrollo Angular (`http://localhost:4200`).
 * - `webServer` reusa el dev server ya levantado; si no existe, lo arranca con
 *   un wrapper local que invoca Angular CLI directamente desde `node.exe`
 *   (sin pasar por `npm run start`, que falla en este entorno WSL/Windows).
 *   `timeout: 60000` y `reuseExistingServer: true` evitan reinicios innecesarios
 *   y bloqueos cuando el runner no tiene autorización para arrancar procesos.
 * - Traza solo en el primer reintento (`on-first-retry`) y captura de pantalla
 *   y video solo cuando una prueba falla.
 *
 * Restricciones:
 * - NO se descarga ni se invoca `npx playwright install` desde esta
 *   configuración: la instalación de binarios de navegador requiere
 *   autorización humana explícita.
 * - NO se ejecuta `ng serve` ni `npm run start` desde el runner sin que el
 *   usuario haya autorizado el arranque del dev server. Si la variable
 *   `PIIP_E2E_NO_WEBSERVER` está definida, se omite `webServer` por completo
 *   y se asume un servidor ya disponible externamente.
 */
const baseURL = process.env['PIIP_E2E_BASE_URL'] ?? 'http://localhost:4200';

export default defineConfig({
  testDir: 'e2e',
  fullyParallel: true,
  forbidOnly: !!process.env['CI'],
  retries: process.env['CI'] ? 1 : 0,
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure'
  },
webServer: process.env['PIIP_E2E_NO_WEBSERVER']
    ? undefined
    : {
        command: 'npx ng serve --port 4200',
        url: baseURL,
        reuseExistingServer: true,
        timeout: 60000
      },
  projects: [
    {
      name: 'desktop-chromium',
      use: { ...devices['Desktop Chrome'], viewport: { width: 1280, height: 720 } }
    },
    {
      name: 'mobile-chromium',
      use: { ...devices['Pixel 5'] }
    }
  ]
});
