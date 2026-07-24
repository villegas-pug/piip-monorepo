import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { EffectiveAssignmentContext } from './effective-assignment.interceptor';

export interface EffectiveAssignmentOption {
  readonly id: string;
  readonly matrizCombinacionId: string;
  readonly funcion: string;
  readonly perfil: string;
  readonly unidad: string;
  readonly inicio: string;
  readonly fin?: string;
  readonly estadoEfectivo: string;
}

/** Identifica el camino de invalidación para mantenerlo en pista de auditoría local. */
export type OrigenInvalidacionContexto =
  | 'REVOCACION_ASIGNACION'
  | 'TERMINACION_SUPLENCIA'
  | 'EXPLICITO';

/** Resultado observable para que la UI muestre anuncios accesibles sin acoplarse al servicio. */
export interface InvalidacionContexto {
  readonly origen: OrigenInvalidacionContexto;
  readonly assignmentId: string;
  readonly aplicado: boolean;
}

/** Obtiene opciones desde el servidor; no infiere permisos, perfiles ni ámbitos. */
@Injectable({ providedIn: 'root' })
export class EffectiveAssignmentService {
  private readonly http = inject(HttpClient);
  private readonly context = inject(EffectiveAssignmentContext);
  private readonly assignmentOptions = signal<readonly EffectiveAssignmentOption[]>([]);

  readonly options = this.assignmentOptions.asReadonly();
  readonly selectedId = this.context.assignmentId;
  readonly selected = computed(() => this.options().find(({ id }) => id === this.selectedId()));

  load(): Observable<readonly EffectiveAssignmentOption[]> {
    return this.http
      .get<readonly EffectiveAssignmentOption[]>('/api/v1/seguridad/me/asignaciones')
      .pipe(tap((options) => this.assignmentOptions.set(Object.freeze([...options]))));
  }

  select(assignmentId: string): void {
    if (this.options().some(({ id }) => id === assignmentId)) {
      this.context.select(assignmentId);
    }
  }

  clear(): void {
    this.context.clear();
  }

  /**
   * Invalida el contexto efectivo local cuando la asignación afectada
   * coincide con la seleccionada. Diseñado para invocarse tras una
   * revocación inmediata confirmada por el backend
   * (`POST /api/v1/seguridad/asignaciones/{id}/revocaciones`): si el
   * identificador revocado coincide con la asignación efectiva del
   * usuario, el selector institucional debe volver a pedir una
   * combinación. La autorización efectiva la revalida el backend en
   * cada llamada sensible; este método solo limpia el estado de UX
   * para evitar enviar un encabezado `X-Asignacion-Efectiva-Id` con un
   * identificador revocado.
   */
  invalidateAfterRevocation(revokedAssignmentId: string | number | null | undefined): InvalidacionContexto {
    return this.invalidateIfSelected(revokedAssignmentId, 'REVOCACION_ASIGNACION');
  }

  /**
   * Invalida el contexto efectivo local cuando la asignación titular
   * afectada por una terminación de suplencia coincide con la
   * seleccionada. Diseñado para invocarse tras una terminación
   * anticipada confirmada por el backend
   * (`POST /api/v1/seguridad/suplencias/{id}/terminaciones`): el
   * comportamiento de la suplencia puede mantener, restringir o
   * desactivar la asignación titular; el cliente prefiere limpiar el
   * contexto y dejar que el selector institucional pida una nueva
   * elección en lugar de arriesgarse a enviar un encabezado con un
   * identificador que el backend rechazará. La revalidación efectiva
   * la hace el servidor.
   */
  invalidateAfterSubstitution(
    affectedTitularAssignmentId: string | number | null | undefined
  ): InvalidacionContexto {
    return this.invalidateIfSelected(affectedTitularAssignmentId, 'TERMINACION_SUPLENCIA');
  }

  private invalidateIfSelected(
    candidato: string | number | null | undefined,
    origen: OrigenInvalidacionContexto
  ): InvalidacionContexto {
    const normalizado = normalizarId(candidato);
    if (!normalizado) {
      return { origen, assignmentId: '', aplicado: false };
    }
    const seleccionado = this.context.assignmentId();
    if (!seleccionado || seleccionado !== normalizado) {
      // La revocación o terminación no afecta a la asignación efectiva
      // local. No tocamos el contexto: el usuario puede seguir
      // trabajando con la misma selección.
      return { origen, assignmentId: normalizado, aplicado: false };
    }
    this.context.clear();
    return { origen, assignmentId: normalizado, aplicado: true };
  }
}

function normalizarId(candidato: string | number | null | undefined): string {
  if (candidato === undefined || candidato === null) {
    return '';
  }
  const texto = String(candidato).trim();
  return texto;
}
