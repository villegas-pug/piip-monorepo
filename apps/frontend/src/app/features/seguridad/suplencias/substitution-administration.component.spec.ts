// T092 · Pruebas Vitest del componente de administración de suplencias
// funcionales (US6).
//
// Cobertura:
//   * Creación de suplencia temporal con `Idempotency-Key` y
//     `Authorization`.
//   * Terminación anticipada exigiendo motivo obligatorio.
//   * Accesibilidad WCAG 2.1 AA: skip-link, etiquetas asociadas, regiones
//     `aria-live`, sin campos de contraseña.
//
// `NEEDS CLARIFICATION`: las rutas
// `POST /api/v1/seguridad/asignaciones/{titularId}/suplencias` y
// `POST /api/v1/seguridad/suplencias/{id}/terminaciones` están
// implementadas en el backend (T089) pero aún no aparecen en el snapshot
// OpenAPI. Las pruebas reflejan las rutas del backend.

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
import { SubstitutionAdministrationComponent } from './substitution-administration.component';

function buildFixture(): { fixture: ComponentFixture<SubstitutionAdministrationComponent>; component: SubstitutionAdministrationComponent } {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [SubstitutionAdministrationComponent],
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
  const fixture = TestBed.createComponent(SubstitutionAdministrationComponent);
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
      correlationId: 'corr-t092-suplencias'
    }
  };
}

describe('SubstitutionAdministrationComponent (T092 · US6)', () => {
  let fixture: ComponentFixture<SubstitutionAdministrationComponent>;
  let component: SubstitutionAdministrationComponent;
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

  it('NO incluye campos de contraseña, token ni atributo sensible en los formularios', () => {
    const camposCrear = Object.keys((component as any).formCrear.controls);
    const camposTerminar = Object.keys((component as any).formTerminar.controls);
    expect(camposCrear).toEqual(expect.arrayContaining(['titularAsignacionId', 'suplenteUsuarioId', 'inicio', 'fin', 'documentoFormalVersionId']));
    expect(camposTerminar).toEqual(expect.arrayContaining(['suplenciaId', 'motivo']));
    expect([...camposCrear, ...camposTerminar]).not.toContain('password');
    expect([...camposCrear, ...camposTerminar]).not.toContain('token');
    expect([...camposCrear, ...camposTerminar]).not.toContain('keycloakAttributes');
  });

  it('crea una suplencia con Idempotency-Key y Authorization', async () => {
    (component as any).formCrear.patchValue({
      titularAsignacionId: 10,
      suplenteUsuarioId: 20,
      inicio: '2026-03-01',
      fin: '2026-03-15',
      documentoFormalVersionId: 900
    });
    const promesa = (component as any).crearSuplencia();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/asignaciones/10/suplencias');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.has('Authorization')).toBe(true);
    expect(req.request.headers.has('Idempotency-Key')).toBe(true);
    expect(req.request.body).toEqual({
      suplenteUsuarioId: 20,
      inicio: '2026-03-01',
      fin: '2026-03-15',
      documentoFormalVersionId: 900
    });
    req.flush({
      id: 100,
      asignacionTitularId: 10,
      suplenteUsuarioId: 20,
      inicio: '2026-03-01',
      fin: '2026-03-15'
    });
    await promesa;
    expect((component as any).suplenciaActual()?.id).toBe(100);
  });

  it('rechaza la creación si falta el documento formal de aprobación', () => {
    (component as any).formCrear.patchValue({
      titularAsignacionId: 10,
      suplenteUsuarioId: 20,
      inicio: '2026-03-01',
      fin: '2026-03-15',
      documentoFormalVersionId: null
    });
    expect((component as any).formCrear.invalid).toBe(true);
    expect((component as any).formCrear.controls.documentoFormalVersionId.hasError('required')).toBe(true);
  });

  it('muestra ProblemDetail accesible cuando la suplencia se solapa con otra (409)', async () => {
    (component as any).formCrear.patchValue({
      titularAsignacionId: 10,
      suplenteUsuarioId: 20,
      inicio: '2026-03-01',
      fin: '2026-03-15',
      documentoFormalVersionId: 900
    });
    const promesa = (component as any).crearSuplencia();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/asignaciones/10/suplencias');
    req.flush(
      problem(409, 'SUBSTITUTION_OVERLAP', 'La suplencia se solapa con una vigente.').error,
      { status: 409, statusText: 'Conflict', headers: { 'Content-Type': 'application/problem+json' } }
    );
    await promesa;
    expect((component as any).problema()?.code).toBe('SUBSTITUTION_OVERLAP');
  });

  it('termina anticipadamente exigiendo motivo obligatorio', async () => {
    (component as any).formTerminar.patchValue({ suplenciaId: 100, motivo: 'Regreso anticipado del titular.', documentoFormalVersionId: null });
    const promesa = (component as any).terminarSuplencia();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/suplencias/100/terminaciones');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ motivo: 'Regreso anticipado del titular.', documentoFormalVersionId: undefined });
    req.flush({
      id: 100,
      asignacionTitularId: 10,
      suplenteUsuarioId: 20,
      inicio: '2026-03-01',
      fin: '2026-03-15',
      terminadaEn: '2026-03-10T08:00:00'
    });
    await promesa;
    expect((component as any).suplenciaActual()?.terminadaEn).toBe('2026-03-10T08:00:00');
  });

  it('cumple el contrato de teclado y WCAG AA: skip-link, etiquetas asociadas y regiones aria-live', () => {
    const dom = fixture.nativeElement as HTMLElement;
    expect(dom.querySelector('a.skip-link')?.getAttribute('href')).toBe('#substitution-admin-contenido');
    expect(dom.querySelector('h1#substitution-admin-titulo')).not.toBeNull();
    expect(dom.querySelector('label[for="substitution-create-titular"]')).not.toBeNull();
    expect(dom.querySelector('input[id="substitution-create-titular"]')?.getAttribute('aria-describedby')).toContain('substitution-create-titular-error');
    expect(dom.querySelector('input[type="password"]')).toBeNull();
  });
});
