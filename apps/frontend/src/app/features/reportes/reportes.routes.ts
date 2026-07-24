// Recorrido del módulo de reportes institucionales (US8 - T109).
//
// El recorrido se carga perezosamente desde `app.routes.ts` mediante
//   `loadChildren: () =>
//      import('./reportes.routes').then(m => m.REPORTES_ROUTES)`
// Ninguno de los componentes ni servicios del recorrido debe incluirse
// en el bundle inicial: por eso todas las rutas usan `loadComponent`.
//
// Decisiones de diseño:
//   * Componentes standalone (sin NgModule), uno por sub-ruta, alineados
//     con el contrato `contracts/reportes.md`:
//       - `''`           → `ReportGenerationComponent` (generación
//         semestral y extraordinaria con `Idempotency-Key`).
//       - `':id'`        → `ReportDetailComponent` (detalle, estado
//         técnico, polling asíncrono, descarga PDF/XLSX y ETag).
//       - `':id/aprobacion'` → `ReportApprovalComponent` (aprobación
//         formal con `If-Match` y remisión manual contra destinatarios
//         aprobados).
//   * `authGuard` protege cada ruta: la sesión institucional la gestiona
//     el backend, pero la UX exige evitar la navegación a formularios
//     institucionales sin login.
//   * `effectiveAssignmentGuard` mejora la experiencia mostrando el
//     selector de asignación efectiva cuando el usuario no ha elegido
//     uno. NO es autoridad local: la correspondencia entre unidad,
//     perfil y recurso la confirma el backend en cada llamada y la
//     transporta `effectiveAssignmentInterceptor` mediante el encabezado
//     `X-Asignacion-Efectiva-Id`.
//   * `title` por ruta para WCAG 2.1 AA (lectores de pantalla y
//     orientación). `data.breadcrumb` queda preparado para integrar un
//     sistema de migas institucional sin acoplar el routing a un
//     componente específico.
//   * Sin permisos locales: este módulo no consulta roles, no decide
//     clasificación ni filtra operaciones; el backend es la autoridad
//     efectiva. La clasificación (`INTERNO` por defecto, `RESTRINGIDO`
//     cuando algún dato del snapshot lo es) la calcula el servidor.
//   * El segmento vive como hermano de `portafolio/*`, `consulta-*` y
//     `seguridad`. Las rutas internas no comparten prefijos con esos
//     segmentos; cada sub-pantalla es independiente y no colisiona.
//   * Sin disposición ni eliminación: la Constitución veta la purga
//     automática mientras no exista tabla de retención aprobada. Esta
//     pantalla nunca expone acciones de borrado.

import { Routes } from '@angular/router';

import { authGuard } from '../../core/auth/auth.guard';
import { effectiveAssignmentGuard } from '../../core/effective-assignment/effective-assignment.guard';

const COMMON_GUARDS = [authGuard, effectiveAssignmentGuard] as const;

/**
 * Catálogo de la US8 para el recorrido de reportes institucionales.
 * Tres pantallas, una por sub-capacidad:
 *   * `''`                 → generación (semestral y extraordinaria).
 *   * `':id'`              → detalle y estado con polling.
 *   * `':id/aprobacion'`   → aprobación formal y remisión manual.
 */
export const REPORTES_ROUTES: Routes = [
  {
    path: '',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Reportes institucionales',
    data: { breadcrumb: 'Reportes institucionales' },
    loadComponent: () =>
      import('./report-generation.component').then(
        ({ ReportGenerationComponent }) => ReportGenerationComponent
      )
  },
  {
    path: ':id',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Detalle de reporte',
    data: { breadcrumb: 'Detalle de reporte' },
    loadComponent: () =>
      import('./report-detail.component').then(
        ({ ReportDetailComponent }) => ReportDetailComponent
      )
  },
  {
    path: ':id/aprobacion',
    canActivate: [...COMMON_GUARDS],
    title: 'PIIP · Aprobación y remisión del reporte',
    data: { breadcrumb: 'Aprobación y remisión' },
    loadComponent: () =>
      import('./report-approval.component').then(
        ({ ReportApprovalComponent }) => ReportApprovalComponent
      )
  }
];
