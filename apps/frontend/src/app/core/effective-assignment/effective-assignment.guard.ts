// Guard institucional de experiencia para el recorrido de portafolio.
//
// Este guard NO es autoridad local: solo verifica que el usuario ha elegido una
// asignación efectiva vigente en el selector. La correspondencia exacta entre la
// unidad/perfil y el recurso (iniciativa, incorporación) la confirma el backend en
// cada operación sensible. Por eso este guard se complementa con el
// `effectiveAssignmentInterceptor`, que añade el encabezado `X-Asignacion-Efectiva-Id`
// a las llamadas API institucionales.
//
// Decisión constitucional:
//   - Sin autoridad local sobre reglas de negocio.
//   - La verificación de la "correspondencia con el recurso" queda en el backend.
//   - El guard evita navegación a formularios sin contexto seleccionado y mejora la UX.

import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { EffectiveAssignmentService } from './effective-assignment.service';

/** Ruta institucional donde el selector de asignación efectiva está disponible. */
const FALLBACK_INSTITUTIONAL_HOME = '/institucional';

/**
 * Impide la navegación a rutas de portafolio cuando el usuario no ha elegido una
 * asignación efectiva. Redirige al shell institucional para que pueda elegirla.
 */
export const effectiveAssignmentGuard: CanActivateFn = () => {
  const assignments = inject(EffectiveAssignmentService);
  const router = inject(Router);

  if (assignments.selectedId()) {
    return true;
  }

  // Si las opciones aún no se cargaron, las solicitamos para que el selector
  // del shell institucional muestre alternativas al usuario tras la redirección.
  if (assignments.options().length === 0) {
    assignments.load().subscribe({
      error: () => {
        // La falla de carga no debe bloquear la redirección al shell; el
        // selector volverá a intentarlo y el backend conservará la autoridad.
      }
    });
  }

  return router.parseUrl(FALLBACK_INSTITUTIONAL_HOME);
};