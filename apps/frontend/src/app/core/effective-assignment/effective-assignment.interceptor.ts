import { Injectable, inject, signal } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';

/** Conserva exclusivamente el identificador elegido; no interpreta roles ni ámbitos. */
@Injectable({ providedIn: 'root' })
export class EffectiveAssignmentContext {
  private readonly selectedAssignmentId = signal<string | undefined>(undefined);
  readonly assignmentId = this.selectedAssignmentId.asReadonly();

  select(assignmentId: string | number | undefined): void {
    const id = typeof assignmentId === 'string' ? assignmentId.trim() : assignmentId;
    this.selectedAssignmentId.set(id ? String(id) : undefined);
  }

  clear(): void {
    this.selectedAssignmentId.set(undefined);
  }
}

/** Presenta la asignación seleccionada; el backend sigue siendo la autoridad efectiva. */
export const effectiveAssignmentInterceptor: HttpInterceptorFn = (request, next) => {
  const assignmentId = inject(EffectiveAssignmentContext).assignmentId();
  if (!assignmentId || !isInstitutionalRequest(request.url) || request.headers.has('X-Asignacion-Efectiva-Id')) {
    return next(request);
  }

  return next(request.clone({ setHeaders: { 'X-Asignacion-Efectiva-Id': assignmentId } }));
};

function isInstitutionalRequest(url: string): boolean {
  const parsed = new URL(url, 'http://piip.local');
  if (
    !parsed.pathname.startsWith('/api/v1/') ||
    parsed.pathname.startsWith('/api/v1/consulta/publica') ||
    parsed.pathname === '/api/v1/seguridad/me/asignaciones'
  ) {
    return false;
  }

  return !isAbsoluteUrl(url) || typeof location === 'undefined' || parsed.origin === location.origin;
}

function isAbsoluteUrl(url: string): boolean {
  return /^[a-z][a-z\d+.-]*:/i.test(url);
}
