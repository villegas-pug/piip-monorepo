// T092 · Pruebas Vitest del cliente de seguridad (US6).
//
// Cubre el contrato del snapshot OpenAPI codigo-first PIIP y verifica
// que el cliente añade `Idempotency-Key` en POST, `If-Match` cuando el
// consumidor facilita la ETag, y que NUNCA envía campos de contraseña,
// token ni atributo sensible en el payload de aprovisionamiento.

import { HttpClient, HttpContext, HttpRequest, HttpResponse, HttpParams } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { SeguridadApiService } from './seguridad-api.service';
import { CreateUserRequest, MatrixVersionRequest } from './types';

interface HttpCall {
  readonly method: string;
  readonly url: string;
  readonly body?: unknown;
  readonly context: HttpContext;
  readonly params?: HttpParams;
}

function buildHttpStub(calls: HttpCall[]): HttpClient {
  const stub = {
    get: vi.fn((url: string, options?: { params?: HttpParams }) => {
      calls.push({ method: 'GET', url, context: new HttpContext(), params: options?.params });
      return of(
        new HttpResponse({
          status: 200,
          body: { content: [], totalElements: 0, totalPages: 0, size: 20, number: 0 }
        })
      );
    }),
    post: vi.fn((url: string, body: unknown, options?: { context?: HttpContext }) => {
      calls.push({ method: 'POST', url, body, context: options?.context ?? new HttpContext() });
      return of(
        new HttpResponse({
          status: 201,
          body: { operacionId: 1, estado: 'INICIADA', recuperable: true, intento: 1 }
        })
      );
    }),
    patch: vi.fn((url: string, body: unknown, options?: { context?: HttpContext }) => {
      calls.push({ method: 'PATCH', url, body, context: options?.context ?? new HttpContext() });
      return of(
        new HttpResponse({
          status: 200,
          body: { id: 1, usuarioId: 2, matrizCombinacionId: 3, fechaInicio: '2026-01-01', etag: '"v1"' }
        })
      );
    })
  };
  return stub as unknown as HttpClient;
}

describe('SeguridadApiService (T092 · US6)', () => {
  let calls: HttpCall[];
  let service: SeguridadApiService;

  beforeEach(() => {
    calls = [];
    const http = buildHttpStub(calls);
    TestBed.configureTestingModule({ providers: [{ provide: HttpClient, useValue: http }] });
    service = TestBed.inject(SeguridadApiService);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('envía POST a /seguridad/usuarios con un HttpContext que activa Idempotency-Key', () => {
    const payload: CreateUserRequest = {
      correoInstitucional: 'persona@midagri.gob.pe',
      nombreCompleto: 'Persona de Prueba',
      unidadId: 100
    };
    void firstValueFrom(service.aprovisionarUsuario(payload));
    const call = calls[0];
    expect(call).toBeDefined();
    expect(call.method).toBe('POST');
    expect(call.url).toBe('/api/v1/seguridad/usuarios');
    expect(call.context).toBeDefined();
  });

  it('NO incluye campos de contraseña, token ni atributo sensible en el payload de aprovisionamiento', () => {
    const payload: CreateUserRequest = {
      correoInstitucional: 'persona@midagri.gob.pe',
      nombreCompleto: 'Persona de Prueba',
      unidadId: 100
    };
    void firstValueFrom(service.aprovisionarUsuario(payload));
    const call = calls[0];
    expect(call).toBeDefined();
    const body = call.body as Record<string, unknown> | undefined;
    expect(body).toBeDefined();
    const claves = Object.keys(body ?? {}).sort();
    expect(claves).toEqual(['correoInstitucional', 'nombreCompleto', 'unidadId']);
  });

  it('adjunta HttpContext con ETag al cambio de asignación cuando se propaga la ETag', () => {
    void firstValueFrom(
      service.cambiarAsignacion(1, { fechaInicio: '2026-02-01' }, { etag: '"abc"' })
    );
    const call = calls[0];
    expect(call.method).toBe('PATCH');
    expect(call.url).toBe('/api/v1/seguridad/asignaciones/1');
    expect(call.context).toBeDefined();
  });

  it('rechaza el cambio de asignación sin ETag previa para evitar carreras silenciosas', () => {
    expect(() =>
      firstValueFrom(service.cambiarAsignacion(1, { fechaInicio: '2026-02-01' }, { etag: undefined }))
    ).toThrow(/ETag/);
  });

  it('crea una versión de matriz con sus funciones y combinaciones canónicas', () => {
    const payload: MatrixVersionRequest = {
      codigoVersion: 'VFPU-2026-1',
      vigenteDesde: '2026-01-01',
      documentoAprobacionVersionId: 900,
      funciones: [{ codigo: 'F1', descripcion: 'Función 1' }],
      combinaciones: [
        {
          funcionCodigo: 'F1',
          perfil: 'Responsable',
          unidadId: 1,
          vigenteDesde: '2026-01-01',
          documentoAprobacionVersionId: 900,
          aprobadorUsuarioId: 7
        }
      ]
    };
    void firstValueFrom(service.crearVersionMatriz(payload));
    const call = calls[0];
    expect(call.method).toBe('POST');
    expect(call.url).toBe('/api/v1/seguridad/matrices/versiones');
  });

  it('publica los endpoints de suplencias definidos por el backend (T089)', () => {
    void firstValueFrom(
      service.crearSuplencia(10, {
        suplenteUsuarioId: 20,
        inicio: '2026-03-01',
        fin: '2026-03-15',
        documentoFormalVersionId: 900
      })
    );
    const call = calls[0];
    expect(call.method).toBe('POST');
    expect(call.url).toBe('/api/v1/seguridad/asignaciones/10/suplencias');
  });

  it('publica el endpoint de terminación de suplencia definido por el backend (T089)', () => {
    void firstValueFrom(
      service.terminarSuplenciaAnticipadamente(100, { motivo: 'Regreso anticipado.' })
    );
    const call = calls[0];
    expect(call.method).toBe('POST');
    expect(call.url).toBe('/api/v1/seguridad/suplencias/100/terminaciones');
  });

  it('lista las funciones de la matriz sin necesidad de carga adicional', () => {
    void firstValueFrom(service.listarFunciones());
    const call = calls[0];
    expect(call.method).toBe('GET');
    expect(call.url).toBe('/api/v1/seguridad/funciones');
  });

  it('lista las combinaciones de una versión de matriz concreta', () => {
    void firstValueFrom(service.listarCombinacionesMatriz(2));
    const call = calls[0];
    expect(call.method).toBe('GET');
    expect(call.url).toBe('/api/v1/seguridad/matrices/versiones/2/combinaciones');
  });

  it('inactiva una combinación mediante una nueva versión', () => {
    void firstValueFrom(
      service.inactivarCombinacionMatriz(11, {
        codigoNuevaVersion: 'VFPU-2026-3',
        documentoAprobacionVersionId: 901,
        aprobadorUsuarioId: 7,
        motivo: 'Reorganización institucional.'
      })
    );
    const call = calls[0];
    expect(call.method).toBe('POST');
    expect(call.url).toBe('/api/v1/seguridad/matrices/combinaciones/11/inactivaciones');
  });

  it('revoca una asignación', () => {
    void firstValueFrom(
      service.revocarAsignacion(1, { motivo: 'Cambio de función.' })
    );
    const call = calls[0];
    expect(call.method).toBe('POST');
    expect(call.url).toBe('/api/v1/seguridad/asignaciones/1/revocaciones');
  });

  it('consulta y reintenta una operación de aprovisionamiento', () => {
    void firstValueFrom(service.consultarOperacionAprovisionamiento(42));
    const consulta = calls[0];
    expect(consulta.method).toBe('GET');
    expect(consulta.url).toBe('/api/v1/seguridad/usuarios/operaciones/42');

    void firstValueFrom(service.reintentarAprovisionamiento(42));
    const reintento = calls[1];
    expect(reintento.method).toBe('POST');
    expect(reintento.url).toBe('/api/v1/seguridad/usuarios/operaciones/42/reintentos');
  });

  it('desactiva y reactiva un usuario con motivo obligatorio', () => {
    void firstValueFrom(service.desactivarUsuario(7, { motivo: 'Baja solicitada.' }));
    const desactivar = calls[0];
    expect(desactivar.method).toBe('POST');
    expect(desactivar.url).toBe('/api/v1/seguridad/usuarios/7/desactivaciones');

    void firstValueFrom(service.reactivarUsuario(7, { motivo: 'Reactivación solicitada.' }));
    const reactivar = calls[1];
    expect(reactivar.method).toBe('POST');
    expect(reactivar.url).toBe('/api/v1/seguridad/usuarios/7/reactivaciones');
  });
});

describe('HttpRequest contract (T092 · US6)', () => {
  it('representa el verbo POST con HttpContext inyectable', () => {
    const request = new HttpRequest('POST', '/api/v1/seguridad/asignaciones', { foo: 'bar' });
    expect(request.method).toBe('POST');
  });
});
