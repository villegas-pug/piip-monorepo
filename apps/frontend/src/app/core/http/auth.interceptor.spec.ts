import { HttpErrorResponse, HttpHeaders, HttpRequest, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { afterEach, describe, expect, it } from 'vitest';

import { AuthService } from '../auth/auth.service';
import { authInterceptor } from './auth.interceptor';
import { parseProblemDetails } from './problem-details';

describe('authInterceptor', () => {
  beforeEach(() => TestBed.resetTestingModule());
  afterEach(() => TestBed.resetTestingModule());

  it('adjunta Bearer solo en solicitudes institucionales', async () => {
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: { getValidAccessToken: async () => 'token-sintetico' } }]
    });
    let received!: HttpRequest<unknown>;

    const interceptor$ = TestBed.runInInjectionContext(() => authInterceptor(
      new HttpRequest('GET', '/api/v1/seguridad/me/asignaciones'),
      (request) => {
        received = request;
        return of(new HttpResponse({ status: 200 }));
      }
    ));
    await firstValueFrom(interceptor$);

    expect(received.headers.get('Authorization')).toBe('Bearer token-sintetico');
  });

  it('no adjunta token a la consulta pública', async () => {
    TestBed.configureTestingModule({ providers: [{ provide: AuthService, useValue: {} }] });
    let received!: HttpRequest<unknown>;

    const interceptor$ = TestBed.runInInjectionContext(() => authInterceptor(
      new HttpRequest('GET', '/api/v1/consulta/publica/portafolio'),
      (request) => {
        received = request;
        return of(new HttpResponse({ status: 200 }));
      }
    ));
    await firstValueFrom(interceptor$);

    expect(received.headers.has('Authorization')).toBe(false);
  });
});

describe('parseProblemDetails', () => {
  it('acepta únicamente Problem Details estructurado', () => {
    const problem = parseProblemDetails(new HttpErrorResponse({
      status: 403,
      headers: new HttpHeaders({ 'Content-Type': 'application/problem+json; charset=utf-8' }),
      error: { title: 'Acceso denegado', status: 403, code: 'ASSIGNMENT_SCOPE_DENIED' }
    }));

    expect(problem).toMatchObject({ status: 403, code: 'ASSIGNMENT_SCOPE_DENIED' });
  });
});
