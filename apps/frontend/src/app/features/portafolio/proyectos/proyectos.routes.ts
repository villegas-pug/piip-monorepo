// Recorrido de la US3 (creacion de proyectos derivados y directos).
//
// El recorrido se carga perezosamente desde `app.routes.ts` mediante
// `loadChildren: () => import('./proyectos.routes').then(m => m.PROYECTOS_ROUTES)`.
// Ninguno de los componentes ni servicios del recorrido debe incluirse en el
// bundle inicial: por eso todas las rutas usan `loadComponent`.
//
// Decisiones de diseno:
//   - Componentes standalone (sin NgModule), uno por ruta.
//   - `authGuard` protege cada ruta: la sesion institucional la gestiona el
//     backend, pero la UX exige evitar navegacion a formularios sin login.
//   - `effectiveAssignmentGuard` mejora la experiencia mostrando el selector
//     de asignacion efectiva cuando el usuario no ha elegido uno. NO es
//     autoridad local: la correspondencia entre unidad, perfil y recurso la
//     confirma el backend en cada llamada y la transporta
//     `effectiveAssignmentInterceptor` mediante el encabezado
//     `X-Asignacion-Efectiva-Id`.
//   - `title` por ruta para WCAG 2.1 AA (lectores de pantalla y orientacion).
//   - `data.breadcrumb` queda preparado para integrar un sistema de migas
//     institucional sin acoplar el routing a un componente especifico.
//   - La ruta `iniciativas/:id/proyecto-derivado` reusa el segmento padre
//     `iniciativas/:id` de los features US1 y US2 pero pertenece a una etapa
//     distinta del portafolio (creacion del unico proyecto derivado de una
//     iniciativa aprobada). El segmento del padre los separa y no colisiona.
//   - La ruta `proyectos-directos/nuevo` sigue la convencion `-nombre` para
//     distinguir la creacion de futuros listados y vistas de detalle.

import { Routes } from '@angular/router';

import { authGuard } from '../../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../../core/effective-assignment/effective-assignment.guard';

const COMMON_GUARDS = [authGuard, effectiveAssignmentGuard] as const;

/**
 * Catalogo canonico de la US3 para el Responsable y la Autoridad/Evaluador:
 * una ruta para la creacion del proyecto derivado de una iniciativa aprobada
 * y otra para la creacion de un proyecto directo heredado o excepcional.
 */
export const PROYECTOS_ROUTES: Routes = [
  {
    path: 'iniciativas/:id/proyecto-derivado',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP \u00b7 Crear proyecto derivado',
    data: { breadcrumb: 'Crear proyecto derivado' },
    loadComponent: () =>
      import('./derived-project/derived-project.component').then(
        ({ DerivedProjectComponent }) => DerivedProjectComponent
      )
  },
  {
    path: 'proyectos-directos/nuevo',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP \u00b7 Crear proyecto directo',
    data: { breadcrumb: 'Crear proyecto directo' },
    loadComponent: () =>
      import('./direct-project/direct-project.component').then(
        ({ DirectProjectComponent }) => DirectProjectComponent
      )
  }
];