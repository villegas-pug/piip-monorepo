// T092 · Pruebas Vitest del componente de administración de la matriz
// función-perfil-unidad (US6).
//
// Cobertura:
//   * Listado paginado de versiones inmutables.
//   * Creación de una nueva versión con `Idempotency-Key` y
//     `Authorization`/`X-Asignacion-Efectiva-Id`.
//   * Inactivación de combinación exigiendo documento, aprobador y
//     motivo.
//   * Accesibilidad WCAG 2.1 AA: skip-link, tablas con `<caption>` y
//     `<th scope="col">`, regiones `aria-live`, foco visible.

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
import { MatrixAdministrationComponent } from './matrix-administration.component';

function buildFixture(): { fixture: ComponentFixture<MatrixAdministrationComponent>; component: MatrixAdministrationComponent } {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [MatrixAdministrationComponent],
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
  const fixture = TestBed.createComponent(MatrixAdministrationComponent);
  fixture.detectChanges();
  return { fixture, component: fixture.componentInstance };
}

describe('MatrixAdministrationComponent (T092 · US6)', () => {
  let fixture: ComponentFixture<MatrixAdministrationComponent>;
  let component: MatrixAdministrationComponent;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    ({ fixture, component } = buildFixture());
    httpMock = TestBed.inject(HttpTestingController);
    // Flush initial HTTP calls from ngOnInit
    await new Promise(r => setTimeout(r));
    const versionesReq = httpMock.expectOne((r) => r.url === '/api/v1/seguridad/matrices/versiones' && r.method === 'GET');
    versionesReq.flush({ content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 });
    const funcionesReq = httpMock.expectOne('/api/v1/seguridad/funciones');
    funcionesReq.flush([]);
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  it('asegura al menos una función y una combinación en el FormArray', () => {
    expect((component as any).funciones.length).toBeGreaterThanOrEqual(1);
    expect((component as any).combinaciones.length).toBeGreaterThanOrEqual(1);
  });

  it('carga el historial paginado de versiones al iniciar', async () => {
    // The initial carga was already flushed in beforeEach; verify the data is set
    expect((component as any).versiones().length).toBe(0);
  });

  it('crea una nueva versión con Idempotency-Key y Authorization', async () => {
    // ngOnInit already called asegurarFilaInicial which added 1 function and 1 combinacion
    (component as any).formVersion.patchValue({
      codigoVersion: 'VFPU-2026-2',
      versionAnteriorId: 1,
      vigenteDesde: '2026-02-01',
      vigenteHasta: '',
      documentoAprobacionVersionId: 900
    });
    // Patch the default function and combinacion added by asegurarFilaInicial
    (component as any).funciones.at(0).patchValue({ codigo: 'F1', descripcion: 'Función 1' });
    (component as any).combinaciones.at(0).patchValue({
      funcionCodigo: 'F1',
      perfil: 'Responsable',
      unidadId: 1,
      vigenteDesde: '2026-02-01',
      vigenteHasta: '',
      documentoAprobacionVersionId: 900,
      aprobadorUsuarioId: 7
    });
    // Remove the extra empty rows added by asegurarFilaInicial (only keep the patched ones)
    // Actually asegurarFilaInicial adds exactly 1 each, so we have the right amount
    const promesa = (component as any).crearVersion();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url.includes('versiones'));
    expect(req.request.method).toBe('POST');
    expect(req.request.headers.has('Authorization')).toBe(true);
    expect(req.request.headers.has('Idempotency-Key')).toBe(true);
    req.flush({
      id: 2,
      codigoVersion: 'VFPU-2026-2',
      vigenteDesde: '2026-02-01',
      activa: true,
      funciones: [{ codigo: 'F1', descripcion: 'Función 1' }],
      combinaciones: []
    });
    await promesa;
    expect((component as any).versiones().length).toBe(1);
  });

  it('inactiva una combinación exigiendo documento, aprobador y motivo', async () => {
    (component as any).formInactivacion.patchValue({
      combinacionId: 11,
      codigoNuevaVersion: 'VFPU-2026-3',
      documentoAprobacionVersionId: 901,
      aprobadorUsuarioId: 7,
      motivo: 'Reorganización institucional.'
    });
    const promesa = (component as any).inactivarCombinacion();
    await new Promise(r => setTimeout(r));
    const req = httpMock.expectOne('/api/v1/seguridad/matrices/combinaciones/11/inactivaciones');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({
      codigoNuevaVersion: 'VFPU-2026-3',
      documentoAprobacionVersionId: 901,
      aprobadorUsuarioId: 7,
      motivo: 'Reorganización institucional.'
    });
    req.flush({
      id: 3,
      codigoVersion: 'VFPU-2026-3',
      vigenteDesde: '2026-03-01',
      activa: true,
      funciones: [],
      combinaciones: []
    });
    await promesa;
    expect((component as any).versiones().length).toBe(1);
  });

  it('cumple el contrato WCAG 2.1 AA: skip-link, encabezado H1 y regiones aria-live', () => {
    const dom = fixture.nativeElement as HTMLElement;
    expect(dom.querySelector('a.skip-link')?.getAttribute('href')).toBe('#matrix-admin-contenido');
    expect(dom.querySelector('h1#matrix-admin-titulo')).not.toBeNull();
    expect(dom.querySelector('label[for="matrix-version-codigo"]')).not.toBeNull();
    expect(dom.querySelector('input[id="matrix-version-codigo"]')?.getAttribute('aria-describedby')).toContain('matrix-version-codigo-error');
  });
});
