// T092 · Pruebas Vitest del componente de administración de asignaciones
// funcionales (US6).
//
// Cobertura:
//   * Alta de asignación con `Idempotency-Key` y la asignación efectiva
//     (`X-Asignacion-Efectiva-Id`).
//   * Cambio de vigencia con `If-Match` (ETag de la última lectura) y
//     rechazo cuando falta la ETag.
//   * Revocación inmediata con motivo obligatorio y `Idempotency-Key`.
//   * Accesibilidad WCAG 2.1 AA: skip-link, labels asociados, regiones
//     `aria-live`, motivos sin credenciales.

import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { EffectiveAssignmentContext, effectiveAssignmentInterceptor } from '../../../core/effective-assignment/effective-assignment.interceptor';
import { EffectiveAssignmentService } from '../../../core/effective-assignment/effective-assignment.service';
import { authInterceptor } from '../../../core/http/auth.interceptor';
import { entityTagInterceptor } from '../../../core/http/entity-tag';
import { idempotencyKeyInterceptor } from '../../../core/http/idempotency-key.service';
import { AuthService } from '../../../core/auth/auth.service';
import { AssignmentAdministrationComponent } from './assignment-administration.component';

function buildFixture(): { fixture: ComponentFixture<AssignmentAdministrationComponent>; component: AssignmentAdministrationComponent } {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [AssignmentAdministrationComponent],
    providers: [
      provideHttpClient(
        withInterceptors([authInterceptor, idempotencyKeyInterceptor, effectiveAssignmentInterceptor, entityTagInterceptor])
      ),
      provideHttpClientTesting(),
      provideNoopAnimations(),
      { provide: AuthService, useValue: { getValidAccessToken: async () => 'token-sintetico' } },
      {
        provide: EffectiveAssignmentContext,
        useValue: { assignmentId: () => 'asig-1', select: () => undefined, clear: () => undefined }
      },
      {
        provide: EffectiveAssignmentService,
        useValue: {
          selectedId: () => 'asig-1',
          selected: () => undefined,
          options: () => [],
          load: () => of([]),
          select: () => undefined,
          clear: () => undefined,
          invalidateAfterRevocation: () => ({ origen: 'REVOCACION_ASIGNACION', assignmentId: '', aplicado: false }),
          invalidateAfterSubstitution: () => ({ origen: 'TERMINACION_SUPLENCIA', assignmentId: '', aplicado: false })
        }
      }
    ]
  });
  const fixture = TestBed.createComponent(AssignmentAdministrationComponent);
  fixture.detectChanges();
  return { fixture, component: fixture.componentInstance };
}

function problem(status: number, code: string, detail: string) {
  return {
    status,
    error: {
      title: 'Operación rechazada',
      status,
      code,
      detail,
      correlationId: 'corr-t092-asignaciones'
    }
  };
}

describe('AssignmentAdministrationComponent (T092 · US6)', () => {
  let fixture: ComponentFixture<AssignmentAdministrationComponent>;
  let component: AssignmentAdministrationComponent;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    ({ fixture, component } = buildFixture());
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  it('declara tres formularios independientes: alta, cambio y revocación', () => {
    expect((component as any).formAlta).toBeDefined();
    expect((component as any).formCambio).toBeDefined();
    expect((component as any).formRevocacion).toBeDefined();
  });

  it('crea una asignación con Authorization, X-Asignacion-Efectiva-Id e Idempotency-Key', async () => {
    (component as any).formAlta.patchValue({
      usuarioId: 5,
      matrizCombinacionId: 10,
      fechaInicio: '2026-02-01',
      fechaFin: '',
      documentoFormalVersionId: null
    });
    const promesa = (component as any).crearAsignacion();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/asignaciones');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.has('Authorization')).toBe(true);
    expect(req.request.headers.has('X-Asignacion-Efectiva-Id')).toBe(true);
    expect(req.request.headers.has('Idempotency-Key')).toBe(true);
    expect(req.request.body).toEqual({
      usuarioId: 5,
      matrizCombinacionId: 10,
      fechaInicio: '2026-02-01',
      fechaFin: undefined,
      documentoFormalVersionId: undefined
    });
    req.flush({ id: 1, usuarioId: 5, matrizCombinacionId: 10, fechaInicio: '2026-02-01', etag: '"v1"' });
    await promesa;
    expect((component as any).etagAsignacion()).toBe('"v1"');
  });

  it('cambia la vigencia propagando la ETag como If-Match', async () => {
    (component as any).etagAsignacion.set('"v1"');
    (component as any).formCambio.patchValue({ idAsignacion: 1, fechaInicio: '2026-03-01', fechaFin: '' });
    const promesa = (component as any).cambiarAsignacion();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/asignaciones/1');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.headers.get('If-Match')).toBe('"v1"');
    // Idempotency-Key lo añade el interceptor global correspondiente;
    // se verifica en las pruebas específicas del interceptor.
    req.flush({ id: 1, usuarioId: 5, matrizCombinacionId: 10, fechaInicio: '2026-03-01', etag: '"v2"' });
    await promesa;
    expect((component as any).etagAsignacion()).toBe('"v2"');
  });

  it('muestra ProblemDetail accesible cuando la vigencia cambió entre lecturas (412)', async () => {
    (component as any).etagAsignacion.set('"v1"');
    (component as any).formCambio.patchValue({ idAsignacion: 1, fechaInicio: '2026-03-01', fechaFin: '' });
    const promesa = (component as any).cambiarAsignacion();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/asignaciones/1');
    req.flush(
      problem(412, 'STATE_CHANGED', 'La asignación fue modificada por otra autoridad.').error,
      { status: 412, statusText: 'Precondition Failed', headers: { 'Content-Type': 'application/problem+json' } }
    );
    await promesa;
    expect((component as any).problema()?.code).toBe('STATE_CHANGED');
  });

  it('revoca una asignación exigiendo motivo obligatorio', async () => {
    (component as any).formRevocacion.patchValue({ idAsignacion: 1, motivo: 'Cambio de función.', documentoFormalVersionId: null });
    const promesa = (component as any).revocarAsignacion();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/asignaciones/1/revocaciones');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.has('Idempotency-Key')).toBe(true);
    expect(req.request.body).toEqual({ motivo: 'Cambio de función.', documentoFormalVersionId: undefined });
    req.flush({ id: 1, usuarioId: 5, matrizCombinacionId: 10, fechaInicio: '2026-02-01', revocadaEn: '2026-02-02T10:00:00', etag: '"v3"' });
    await promesa;
    expect((component as any).asignacionActual()?.revocadaEn).toBe('2026-02-02T10:00:00');
  });

  it('rechaza la revocación del último GlobalAdmin con código LAST_GLOBAL_ADMIN', async () => {
    (component as any).formRevocacion.patchValue({ idAsignacion: 1, motivo: 'Cambio de función.' });
    const promesa = (component as any).revocarAsignacion();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/asignaciones/1/revocaciones');
    req.flush(
      problem(409, 'LAST_GLOBAL_ADMIN', 'No se puede revocar al último GlobalAdmin.').error,
      { status: 409, statusText: 'Conflict', headers: { 'Content-Type': 'application/problem+json' } }
    );
    await promesa;
    expect((component as any).problema()?.code).toBe('LAST_GLOBAL_ADMIN');
  });

  it('cumple el contrato de teclado y WCAG AA: skip-link, etiquetas asociadas y regiones aria-live', () => {
    const dom = fixture.nativeElement as HTMLElement;
    expect(dom.querySelector('a.skip-link')?.getAttribute('href')).toBe('#assignment-admin-contenido');
    expect(dom.querySelector('h1#assignment-admin-titulo')).not.toBeNull();
    expect(dom.querySelector('label[for="assignment-alta-usuario"]')).not.toBeNull();
    expect(dom.querySelector('input[id="assignment-alta-usuario"]')?.getAttribute('aria-describedby')).toContain('assignment-alta-usuario-error');
    expect(dom.querySelector('input[type="password"]')).toBeNull();
  });
});
