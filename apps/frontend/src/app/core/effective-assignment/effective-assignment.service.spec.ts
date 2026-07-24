import { HttpRequest, HttpResponse, provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { afterEach, describe, expect, it } from 'vitest';

import { EffectiveAssignmentContext, effectiveAssignmentInterceptor } from './effective-assignment.interceptor';
import { EffectiveAssignmentService } from './effective-assignment.service';

describe('EffectiveAssignmentService', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('carga opciones del servidor y conserva exactamente la opción seleccionada', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(EffectiveAssignmentService);
    const http = TestBed.inject(HttpTestingController);
    const context = TestBed.inject(EffectiveAssignmentContext);

    service.load().subscribe();
    http.expectOne('/api/v1/seguridad/me/asignaciones').flush([{
      id: 'asignacion-sintetica-1', matrizCombinacionId: 'matriz-1', funcion: 'FUNCION_SINTETICA',
      perfil: 'Consulta', unidad: 'Unidad sintética', inicio: '2026-01-01', estadoEfectivo: 'VIGENTE'
    }]);
    service.select('asignacion-sintetica-1');

    expect(context.assignmentId()).toBe('asignacion-sintetica-1');
    service.select('asignacion-no-listada');
    expect(context.assignmentId()).toBe('asignacion-sintetica-1');
    http.verify();
  });
});

describe('EffectiveAssignmentService · invalidación tras revocación o sustitución (T093)', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('limpia el contexto local cuando la asignación revocada coincide con la seleccionada', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(EffectiveAssignmentService);
    const context = TestBed.inject(EffectiveAssignmentContext);

    context.select('asignacion-sintetica-1');
    expect(context.assignmentId()).toBe('asignacion-sintetica-1');

    const resultado = service.invalidateAfterRevocation('asignacion-sintetica-1');
    expect(resultado).toEqual({
      origen: 'REVOCACION_ASIGNACION',
      assignmentId: 'asignacion-sintetica-1',
      aplicado: true
    });
    expect(context.assignmentId()).toBeUndefined();
  });

  it('acepta identificadores numéricos en la invalidación tras revocación y los normaliza', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(EffectiveAssignmentService);
    const context = TestBed.inject(EffectiveAssignmentContext);

    context.select('42');
    const resultado = service.invalidateAfterRevocation(42);
    expect(resultado.aplicado).toBe(true);
    expect(resultado.assignmentId).toBe('42');
    expect(context.assignmentId()).toBeUndefined();
  });

  it('preserva el contexto local cuando la asignación revocada NO coincide con la seleccionada', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(EffectiveAssignmentService);
    const context = TestBed.inject(EffectiveAssignmentContext);

    context.select('asignacion-sintetica-1');
    const resultado = service.invalidateAfterRevocation('asignacion-de-otra-persona');
    expect(resultado.aplicado).toBe(false);
    expect(resultado.assignmentId).toBe('asignacion-de-otra-persona');
    expect(context.assignmentId()).toBe('asignacion-sintetica-1');
  });

  it('no aplica la revocación cuando no hay asignación efectiva seleccionada', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(EffectiveAssignmentService);
    const context = TestBed.inject(EffectiveAssignmentContext);

    const resultado = service.invalidateAfterRevocation('asignacion-sintetica-1');
    expect(resultado.aplicado).toBe(false);
    expect(context.assignmentId()).toBeUndefined();
  });

  it('descarta silenciosamente identificadores vacíos o nulos en la revocación', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(EffectiveAssignmentService);
    const context = TestBed.inject(EffectiveAssignmentContext);

    context.select('asignacion-sintetica-1');
    expect(service.invalidateAfterRevocation(undefined).aplicado).toBe(false);
    expect(service.invalidateAfterRevocation(null).aplicado).toBe(false);
    expect(service.invalidateAfterRevocation('').aplicado).toBe(false);
    expect(service.invalidateAfterRevocation('   ').aplicado).toBe(false);
    expect(context.assignmentId()).toBe('asignacion-sintetica-1');
  });

  it('limpia el contexto local cuando la asignación titular afectada por la suplencia coincide', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(EffectiveAssignmentService);
    const context = TestBed.inject(EffectiveAssignmentContext);

    context.select('asignacion-titular-1');
    const resultado = service.invalidateAfterSubstitution('asignacion-titular-1');
    expect(resultado).toEqual({
      origen: 'TERMINACION_SUPLENCIA',
      assignmentId: 'asignacion-titular-1',
      aplicado: true
    });
    expect(context.assignmentId()).toBeUndefined();
  });

  it('acepta identificadores numéricos en la invalidación tras terminación de suplencia', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(EffectiveAssignmentService);
    const context = TestBed.inject(EffectiveAssignmentContext);

    context.select('7');
    const resultado = service.invalidateAfterSubstitution(7);
    expect(resultado.aplicado).toBe(true);
    expect(resultado.assignmentId).toBe('7');
    expect(context.assignmentId()).toBeUndefined();
  });

  it('preserva el contexto local cuando la suplencia afecta a una asignación distinta a la seleccionada', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(EffectiveAssignmentService);
    const context = TestBed.inject(EffectiveAssignmentContext);

    context.select('asignacion-titular-1');
    const resultado = service.invalidateAfterSubstitution('asignacion-titular-2');
    expect(resultado.aplicado).toBe(false);
    expect(resultado.origen).toBe('TERMINACION_SUPLENCIA');
    expect(context.assignmentId()).toBe('asignacion-titular-1');
  });

  it('no aplica la terminación de suplencia cuando no hay asignación efectiva seleccionada', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(EffectiveAssignmentService);
    const context = TestBed.inject(EffectiveAssignmentContext);

    const resultado = service.invalidateAfterSubstitution('asignacion-titular-1');
    expect(resultado.aplicado).toBe(false);
    expect(context.assignmentId()).toBeUndefined();
  });

  it('mantiene `clear()` como mecanismo explícito, independiente de revocación o terminación', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    const service = TestBed.inject(EffectiveAssignmentService);
    const context = TestBed.inject(EffectiveAssignmentContext);

    context.select('asignacion-sintetica-1');
    service.clear();
    expect(context.assignmentId()).toBeUndefined();

    // Tras un `clear` explícito, una revocación posterior no debe reaplicar.
    const resultado = service.invalidateAfterRevocation('asignacion-sintetica-1');
    expect(resultado.aplicado).toBe(false);
  });
});

describe('effectiveAssignmentInterceptor', () => {
  afterEach(() => TestBed.resetTestingModule());

  it('presenta solo la asignación elegida y omite el listado de contexto', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient()] });
    const context = TestBed.inject(EffectiveAssignmentContext);
    context.select('asignacion-sintetica-1');
    let requestWithAssignment!: HttpRequest<unknown>;

    await firstValueFrom(TestBed.runInInjectionContext(() => effectiveAssignmentInterceptor(
      new HttpRequest('POST', '/api/v1/portafolio/operaciones', null),
      (request) => {
        requestWithAssignment = request;
        return of(new HttpResponse({ status: 200 }));
      }
    )));
    expect(requestWithAssignment.headers.get('X-Asignacion-Efectiva-Id')).toBe('asignacion-sintetica-1');

    let contextRequest!: HttpRequest<unknown>;
    await firstValueFrom(TestBed.runInInjectionContext(() => effectiveAssignmentInterceptor(
      new HttpRequest('GET', '/api/v1/seguridad/me/asignaciones'),
      (request) => {
        contextRequest = request;
        return of(new HttpResponse({ status: 200 }));
      }
    )));
    expect(contextRequest.headers.has('X-Asignacion-Efectiva-Id')).toBe(false);
  });

  it('omite el encabezado de asignación efectiva cuando el contexto se invalida tras revocación', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient()] });
    const service = TestBed.inject(EffectiveAssignmentService);
    const context = TestBed.inject(EffectiveAssignmentContext);

    context.select('asignacion-sintetica-1');
    service.invalidateAfterRevocation('asignacion-sintetica-1');
    expect(context.assignmentId()).toBeUndefined();

    let requestSinAsignacion!: HttpRequest<unknown>;
    await firstValueFrom(TestBed.runInInjectionContext(() => effectiveAssignmentInterceptor(
      new HttpRequest('POST', '/api/v1/portafolio/operaciones', null),
      (request) => {
        requestSinAsignacion = request;
        return of(new HttpResponse({ status: 200 }));
      }
    )));
    expect(requestSinAsignacion.headers.has('X-Asignacion-Efectiva-Id')).toBe(false);
  });
});
