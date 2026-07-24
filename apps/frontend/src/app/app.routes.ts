import { Routes } from '@angular/router';

import { authGuard } from './core/auth/auth.guard';

/** Las rutas institucionales son una ayuda de navegación; el backend conserva la autorización. */
export const routes: Routes = [
  {
    path: 'institucional',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./core/layout/institutional-shell.component').then(({ InstitutionalShellComponent }) => InstitutionalShellComponent)
  },
  {
    path: 'auth/callback',
    loadComponent: () => import('./core/auth/auth-callback.component').then(({ AuthCallbackComponent }) => AuthCallbackComponent)
  },
  {
    // US7: consulta pública anónima del portafolio. Segmento independiente
    // y lazy; convive con `portafolio/*`, `seguridad` y
    // `consulta-institucional` sin compartir prefijos. No expone
    // `authGuard` ni `effectiveAssignmentGuard` porque el visitante es
    // anónimo y la autorización efectiva la aplica el backend.
    path: 'consulta-publica',
    loadChildren: () => import('./features/consulta-publica/consulta-publica.routes').then(({ PUBLIC_QUERY_ROUTES }) => PUBLIC_QUERY_ROUTES)
  },
  {
    // US7: consulta institucional del portafolio. Segmento independiente
    // y lazy; convive con `consulta-publica` y los `portafolio/*`. La
    // autorización efectiva la conserva el backend; este segmento se
    // limita a registrar la ruta protegida por `authGuard` y
    // `effectiveAssignmentGuard` para evitar navegación a formularios
    // institucionales sin sesión ni asignación efectiva.
    path: 'consulta-institucional',
    canActivate: [authGuard],
    loadChildren: () => import('./features/consulta-institucional/consulta-institucional.routes').then(({ CONSULTA_INSTITUCIONAL_ROUTES }) => CONSULTA_INSTITUCIONAL_ROUTES)
  },
  {
    // US1: recorrido institucional de registro. La carga perezosa mantiene el
    // bundle inicial libre de los componentes de iniciativa, subsanación e
    // incorporación. Las rutas hermanas `portafolio/evaluacion`,
    // `portafolio/decision`, `portafolio/proyectos`, `portafolio/seguimiento`,
    // `portafolio/producto-final` y `portafolio/cierre` (US5) se registran
    // como segmentos independientes; este recorrido no las invade.
    path: 'portafolio/registro',
    loadChildren: () => import('./features/portafolio/registro/registro.routes').then(({ REGISTRO_ROUTES }) => REGISTRO_ROUTES)
  },
  {
    // US2 (Evaluador): recorrido de evaluación de iniciativa con apertura
    // opcional de subsanación. Segmento independiente del recorrido de
    // registro y del de decisión; ambos comparten el path `iniciativas/:id`
    // pero el segmento del padre los separa y no colisionan.
    path: 'portafolio/evaluacion',
    loadChildren: () => import('./features/portafolio/evaluacion/evaluacion.routes').then(({ EVALUACION_ROUTES }) => EVALUACION_ROUTES)
  },
  {
    // US2 (Autoridad): recorrido de decisión formal de iniciativa y
    // cancelación de proyecto. Segmento independiente; comparte el path
    // `iniciativas/:id` con evaluación, pero pertenece a otra etapa del
    // portafolio y no colisiona con `portafolio/registro` ni con
    // `portafolio/evaluacion`.
    path: 'portafolio/decision',
    loadChildren: () => import('./features/portafolio/decision/decision.routes').then(({ DECISION_ROUTES }) => DECISION_ROUTES)
  },
  {
    // US3: recorrido de creacion de proyectos derivados y directos. Segmento
    // independiente; reusa el path `iniciativas/:id` como prefijo de la
    // ruta de derivado, pero pertenece a otra etapa del portafolio y no
    // colisiona con `portafolio/registro`, `portafolio/evaluacion` ni
    // `portafolio/decision` porque el segmento del padre los separa.
    path: 'portafolio/proyectos',
    loadChildren: () => import('./features/portafolio/proyectos/proyectos.routes').then(({ PROYECTOS_ROUTES }) => PROYECTOS_ROUTES)
  },
  {
    // US4: seguimiento de ejecución, ciclos, suspensión y presentación final.
    // La cancelación pertenece al segmento independiente `portafolio/decision`.
    path: 'portafolio/seguimiento',
    loadChildren: () => import('./features/portafolio/seguimiento/seguimiento.routes').then(({ SEGUIMIENTO_ROUTES }) => SEGUIMIENTO_ROUTES)
  },
  {
    // US5: decisión formal del producto final. La presentación permanece en
    // `portafolio/seguimiento`; este segmento solo registra la decisión.
    path: 'portafolio/producto-final',
    loadChildren: () => import('./features/portafolio/producto-final/producto-final.routes').then(({ PRODUCTO_FINAL_ROUTES }) => PRODUCTO_FINAL_ROUTES)
  },
  {
    // US5: cierre administrativo posterior a cualquiera de las decisiones de
    // producto. Es un segmento independiente y lazy.
    path: 'portafolio/cierre',
    loadChildren: () => import('./features/portafolio/cierre/cierre.routes').then(({ CIERRE_ROUTES }) => CIERRE_ROUTES)
  },
  {
    // US6: administración de seguridad institucional (usuarios, matriz
    // función-perfil-unidad, asignaciones y suplencias). Segmento
    // independiente y lazy; convive con `portafolio/*` y `consulta-*`
    // sin compartir prefijos. El contexto efectivo se invalida tras
    // revocación o sustitución mediante `EffectiveAssignmentService`
    // (T093); la autorización efectiva sigue siendo del backend.
    path: 'seguridad',
    loadChildren: () => import('./features/seguridad/seguridad.routes').then(({ SEGURIDAD_ROUTES }) => SEGURIDAD_ROUTES)
  },
  {
    // US8: recorrido institucional de reportes (generación, detalle y
    // estado con polling, aprobación y remisión). Segmento independiente
    // y lazy; convive con `portafolio/*`, `consulta-*` y `seguridad`
    // sin compartir prefijos. La autorización efectiva la conserva el
    // backend: la UI solo refleja `estadoTecnico`, clasificación y
    // resultados. La disposición y la eliminación están vetadas
    // constitucionalmente.
    path: 'reportes',
    loadChildren: () => import('./features/reportes/reportes.routes').then(({ REPORTES_ROUTES }) => REPORTES_ROUTES)
  },
  { path: '', pathMatch: 'full', redirectTo: 'consulta-publica' },
  { path: '**', redirectTo: 'consulta-publica' }
];
