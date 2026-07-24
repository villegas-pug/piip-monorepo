// Recorrido de la US7 (consulta pública del portafolio).
//
// El recorrido se carga perezosamente desde `app.routes.ts` mediante
// `loadChildren: () => import('./consulta-publica.routes').then(m => m.PUBLIC_QUERY_ROUTES)`.
// Ninguno de los componentes ni servicios del recorrido debe incluirse
// en el bundle inicial: por eso todas las rutas usan `loadComponent`.
//
// Decisiones de diseño:
//   * Componentes standalone (sin NgModule), uno por ruta.
//   * La consulta pública es ANÓNIMA: ninguna ruta expone `authGuard`
//     ni `effectiveAssignmentGuard`. La sesión institucional y la
//     asignación efectiva solo existen en el espacio institucional;
//     un visitante anónimo no debe autenticarse para consultar la
//     allowlist pública.
//   * El segmento es hermano de `consulta-institucional`, `portafolio/*`
//     y `seguridad`; las rutas internas no comparten prefijos.
//   * El cliente público NO reutiliza DTOs del cliente institucional.
//     Los tipos viven en `consulta-publica/api/types.ts` y nunca
//     importan desde `consulta-institucional/api/types.ts`.
//   * La página de detalle NO expone contenido ni habilita descarga.
//     El cliente `PublicQueryApiService` jamás invoca
//     `/api/v1/documentos/{id}/contenido`; el componente
//     `PublicDetailComponent` no renderiza enlaces de descarga.

import { Routes } from '@angular/router';

/**
 * Catálogo de la US7 para la consulta pública. La pantalla
 * canónica es la entrada raíz y la ruta `:id` carga perezosamente
 * `PublicDetailComponent` standalone. La entrada raíz y la ruta
 * con `id` no comparten estado: cada navegación instancia un
 * componente independiente.
 */
export const PUBLIC_QUERY_ROUTES: Routes = [
  {
    path: '',
    title: 'PIIP · Consulta pública',
    data: { breadcrumb: 'Consulta pública' },
    loadComponent: () =>
      import('./public-detail.component').then(
        ({ PublicDetailComponent }) => PublicDetailComponent
      )
  },
  {
    path: ':id',
    title: 'PIIP · Detalle público',
    data: { breadcrumb: 'Detalle público' },
    loadComponent: () =>
      import('./public-detail.component').then(
        ({ PublicDetailComponent }) => PublicDetailComponent
      )
  }
];
