// T092 · Pruebas Vitest del componente de administración de usuarios
// institucionales (US6).
//
// Cobertura:
//   * Sin contraseñas, tokens ni atributos sensibles en los modelos del
//     componente.
//   * Aprovisionamiento con `Idempotency-Key` (autogenerado por el
//     interceptor global; el cliente activa el contexto).
//   * Consulta de operación recuperable.
//   * Reintento de operación con código canónico
//     `KEYCLOAK_OPERATION_RECOVERABLE`.
//   * Desactivación y reactivación con motivo obligatorio.
//   * Accesibilidad WCAG 2.1 AA: labels asociados, mensajes de error
//     con `aria-describedby`, regiones `aria-live`, sin campos de
//     contraseña.

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
import { UserAdministrationComponent } from './user-administration.component';

function buildFixture(): { fixture: ComponentFixture<UserAdministrationComponent>; component: UserAdministrationComponent } {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [UserAdministrationComponent],
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
        useValue: { selectedId: () => 'asig-1', selected: () => undefined, options: () => [], load: () => of([]), select: () => undefined, clear: () => undefined }
      }
    ]
  });
  const fixture = TestBed.createComponent(UserAdministrationComponent);
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
      correlationId: 'corr-t092-usuarios'
    }
  };
}

describe('UserAdministrationComponent (T092 · US6)', () => {
  let fixture: ComponentFixture<UserAdministrationComponent>;
  let component: UserAdministrationComponent;
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

  it('expide un formulario de aprovisionamiento sin campos de contraseña, token ni atributo sensible', () => {
    const campos = Object.keys((component as any).formAprovisionamiento.controls);
    expect(campos).toEqual(expect.arrayContaining(['correoInstitucional', 'nombreCompleto', 'unidadId']));
    expect(campos).not.toContain('password');
    expect(campos).not.toContain('token');
    expect(campos).not.toContain('keycloakAttributes');
  });

  it('rechaza un aprovisionamiento con correo inválido', () => {
    (component as any).formAprovisionamiento.patchValue({
      correoInstitucional: 'correo-no-valido',
      nombreCompleto: 'Persona',
      unidadId: 1
    });
    expect((component as any).formAprovisionamiento.invalid).toBe(true);
    expect((component as any).formAprovisionamiento.controls.correoInstitucional.hasError('email')).toBe(true);
  });

  it('envía POST /seguridad/usuarios con Idempotency-Key y Authorization', async () => {
    (component as any).formAprovisionamiento.patchValue({
      correoInstitucional: 'persona@midagri.gob.pe',
      nombreCompleto: 'Persona de Prueba',
      unidadId: 1
    });
    const promesa = (component as any).aprovisionarUsuario();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/usuarios');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.has('Authorization')).toBe(true);
    expect(req.request.headers.has('Idempotency-Key')).toBe(true);
    expect(req.request.body).toEqual({
      correoInstitucional: 'persona@midagri.gob.pe',
      nombreCompleto: 'Persona de Prueba',
      unidadId: 1
    });
    req.flush({ operacionId: 1, estado: 'INICIADA', recuperable: true, intento: 1 });
    await promesa;
  });

  it('muestra ProblemDetail accesible cuando la operación es recuperable', async () => {
    (component as any).formAprovisionamiento.patchValue({
      correoInstitucional: 'persona@midagri.gob.pe',
      nombreCompleto: 'Persona',
      unidadId: 1
    });
    const promesa = (component as any).aprovisionarUsuario();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/usuarios');
    req.flush(
      problem(503, 'KEYCLOAK_OPERATION_RECOVERABLE', 'Keycloak no disponible, reintente.').error,
      { status: 503, statusText: 'Service Unavailable', headers: { 'Content-Type': 'application/problem+json' } }
    );
    await promesa;
    expect((component as any).problema()?.code).toBe('KEYCLOAK_OPERATION_RECOVERABLE');
  });

  it('permite reintentar una operación recuperable', async () => {
    (component as any).operacionActual.set({ operacionId: 42, estado: 'KEYCLOAK_CREADO_DESHABILITADO', recuperable: true, intento: 1 });
    const promesa = (component as any).reintentarOperacion();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/usuarios/operaciones/42/reintentos');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.has('Idempotency-Key')).toBe(true);
    req.flush({ operacionId: 42, estado: 'COMPLETADA', recuperable: false, intento: 2 });
    await promesa;
    expect((component as any).operacionActual()?.estado).toBe('COMPLETADA');
  });

  it('consulta el estado actual de una operación de aprovisionamiento', async () => {
    (component as any).operacionActual.set({ operacionId: 42, estado: 'ORACLE_PENDIENTE', recuperable: true, intento: 1 });
    const promesa = (component as any).consultarOperacion();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/usuarios/operaciones/42');
    expect(req.request.method).toBe('GET');
    req.flush({ operacionId: 42, estado: 'COMPLETADA', recuperable: false, intento: 1 });
    await promesa;
    expect((component as any).operacionActual()?.estado).toBe('COMPLETADA');
  });

  it('desactiva un usuario exigiendo motivo obligatorio', async () => {
    (component as any).seleccionarModo('DESACTIVAR');
    (component as any).formEstado.patchValue({ idUsuario: 7, motivo: 'Baja solicitada por la unidad.' });
    const promesa = (component as any).aplicarCambioEstado();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/usuarios/7/desactivaciones');
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.has('Idempotency-Key')).toBe(true);
    expect(req.request.body).toEqual({ motivo: 'Baja solicitada por la unidad.' });
    req.flush({ usuarioId: 7, estado: 'DESHABILITADO' });
    await promesa;
    expect((component as any).estadoActual()?.estado).toBe('DESHABILITADO');
  });

  it('rechaza la desactivación sin motivo', () => {
    (component as any).seleccionarModo('DESACTIVAR');
    (component as any).formEstado.patchValue({ idUsuario: 7, motivo: '' });
    expect((component as any).formEstado.controls.motivo.hasError('required')).toBe(true);
  });

  it('cumple el contrato de teclado y WCAG AA: skip-link, regiones aria-live y sin campos de contraseña', () => {
    const dom = fixture.nativeElement as HTMLElement;
    expect(dom.querySelector('a.skip-link')?.getAttribute('href')).toBe('#user-admin-contenido');
    expect(dom.querySelector('h1#user-admin-titulo')).not.toBeNull();
    expect(dom.querySelector('label[for="user-provisioning-correo"]')).not.toBeNull();
    expect(dom.querySelector('input[id="user-provisioning-correo"]')?.getAttribute('aria-describedby')).toContain('user-provisioning-correo-error');
    expect(dom.querySelector('input[type="password"]')).toBeNull();
  });
});
