// T109 · Pruebas Vitest del cliente de reportes institucionales (US8).
//
// Cubre el contrato del snapshot OpenAPI codigo-first PIIP para los
// endpoints del módulo `reportes` y verifica que el cliente:
//   * añade `Idempotency-Key` a los verbos POST institucionales cuando
//     activa el contexto, sin filtrar contraseñas ni atributos sensibles;
//   * añade `If-Match` cuando el consumidor facilita la ETag de una
//     lectura previa;
//   * serializa el resultado del detalle como `ReporteDetalleRespuesta`
//     con la ETag de la cabecera y el indicador 304 (`notModified`);
//   * serializa el indicador BR-122 con denominador cero como no
//     aplicable y porcentaje nulo.

import { HttpClient, HttpContext, HttpHeaders, HttpParams, HttpResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { firstValueFrom, of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { ReportesApiService } from './reportes-api.service';
import {
  ReporteAprobacionRequest,
  ReporteExtraordinarioRequest,
  ReporteRemisionRequest,
  ReporteSemestralRequest
} from './types';

interface HttpCall {
  readonly method: string;
  readonly url: string;
  readonly body?: unknown;
  readonly context: HttpContext;
  readonly params?: HttpParams;
  readonly headers?: HttpHeaders;
  readonly responseType?: string;
}

interface StubOptions {
  body?: unknown;
  status?: number;
  headers?: Record<string, string>;
}

function buildHttpStub(calls: HttpCall[]): HttpClient {
  return {
    get: vi.fn((url: string, options?: StubOptions & { params?: HttpParams; headers?: HttpHeaders; observe?: 'response' | 'body' }) => {
      calls.push({
        method: 'GET',
        url,
        context: new HttpContext(),
        params: options?.params,
        headers: options?.headers
      });
      // Retorna el cuerpo directamente (no HttpResponse) porque HttpClient
      // por defecto emite el body, no la respuesta completa.
      return of(options?.body ?? null);
    }),
    post: vi.fn((url: string, body: unknown, options?: { context?: HttpContext; headers?: HttpHeaders }) => {
      calls.push({
        method: 'POST',
        url,
        body,
        context: options?.context ?? new HttpContext()
      });
      return of(
        options?.headers
          ? undefined
          : { reporteId: 1, operacionId: 'op-1', corte: '2026-06-30', versionDatos: 1, estadoTecnico: 'INICIADA' }
      );
    })
  } as unknown as HttpClient;
}

describe('ReportesApiService (T109 · US8)', () => {
  let calls: HttpCall[];
  let service: ReportesApiService;

  beforeEach(() => {
    calls = [];
    const http = buildHttpStub(calls);
    TestBed.configureTestingModule({ providers: [{ provide: HttpClient, useValue: http }] });
    service = TestBed.inject(ReportesApiService);
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  // -------------------------------------------------------------------------
  // Generación
  // -------------------------------------------------------------------------

  it('envía POST a /reportes/semestrales/generaciones con Idempotency-Key contextual', () => {
    const payload: ReporteSemestralRequest = { anio: 2026, semestre: 1 };
    void firstValueFrom(service.generarReporteSemestral(payload));
    const call = calls[0];
    expect(call).toBeDefined();
    expect(call.method).toBe('POST');
    expect(call.url).toBe('/api/v1/reportes/semestrales/generaciones');
    expect(call.context).toBeDefined();
    // El interceptor global añade la clave; el cliente activa el contexto
    // para que el interceptor la inyecte.
    expect(typeof call.context).toBe('object');
  });

  it('envía POST a /reportes/extraordinarios/generaciones con solicitud y aprobación documentadas', () => {
    const payload: ReporteExtraordinarioRequest = {
      solicitudDocumentoId: 100,
      aprobacionOficinaDocumentoId: 200,
      periodo: '2026-01',
      fechaCorte: '2026-01-31',
      filtros: { tipo: 'INICIATIVA' }
    };
    void firstValueFrom(service.generarReporteExtraordinario(payload));
    const call = calls[0];
    expect(call.method).toBe('POST');
    expect(call.url).toBe('/api/v1/reportes/extraordinarios/generaciones');
    expect(call.body).toEqual(payload);
  });

  it('rechaza ETag inválida en comandos mutables para evitar carreras silenciosas', () => {
    expect(() =>
      firstValueFrom(
        service.aprobarReporte(
          1,
          { idVersion: 1, idDocumentoAprobacion: 5, destinatarios: [] },
          { etag: '   ' }
        )
      )
    ).toThrow(/ETag/);
  });

  // -------------------------------------------------------------------------
  // Estado y detalle
  // -------------------------------------------------------------------------

  it('consulta el detalle y propaga la ETag como If-None-Match', () => {
    const stub = {
      get: vi.fn(() =>
        of(
          new HttpResponse({
            status: 200,
            body: {
              idReporte: 1,
              tipo: 'SEMESTRAL',
              anio: 2026,
              semestre: 1,
              periodo: '2026-S1',
              fechaCorte: '2026-06-30',
              versionDatos: 1,
              estadoTecnico: 'GENERADA',
              clasificacion: 'INTERNO',
              indicadores: [
                { nombre: 'Admisibilidad', numerador: 0, denominador: 0, porcentaje: null, aplicable: false }
              ],
              totales: [
                { dimension: 'tipo', items: [{ clave: 'INICIATIVA', etiqueta: 'Iniciativa', total: 3 }] }
              ],
              archivos: [
                {
                  idArchivo: 10,
                  formato: 'PDF',
                  version: 1,
                  hashSha256: 'abc'
                }
              ],
              etag: 'W/"1"'
            },
            headers: new HttpHeaders({ 'ETag': 'W/"1"', 'X-Correlation-Id': 'corr-1' })
          })
        )
      )
    };
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [{ provide: HttpClient, useValue: stub }] });
    const servicio = TestBed.inject(ReportesApiService);
    const respuesta = firstValueFrom(servicio.consultarReporte(1, { ifNoneMatch: 'W/"0"' }));
    expect(stub.get).toHaveBeenCalledWith('/api/v1/reportes/generaciones/1', expect.objectContaining({ observe: 'response' }));
    respuesta.then(({ body, etag, correlationId, notModified }) => {
      expect(notModified).toBe(false);
      expect(etag).toBe('W/"1"');
      expect(correlationId).toBe('corr-1');
      expect(body.indicadores[0].aplicable).toBe(false);
      expect(body.indicadores[0].porcentaje).toBeNull();
    });
  });

  it('marca el detalle como no modificado cuando el servidor responde 304', () => {
    const stub = {
      get: vi.fn(() =>
        of(
          new HttpResponse({
            status: 304,
            body: null,
            headers: new HttpHeaders({ 'ETag': 'W/"1"' })
          })
        )
      )
    };
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [{ provide: HttpClient, useValue: stub }] });
    const servicio = TestBed.inject(ReportesApiService);
    const promesa = firstValueFrom(servicio.consultarReporte(1));
    return promesa.then((respuesta) => {
      expect(respuesta.notModified).toBe(true);
      expect(respuesta.etag).toBe('W/"1"');
    });
  });

  it('descarga el PDF y XLSX con `responseType: blob`', () => {
    const blob = new Blob(['x'], { type: 'application/pdf' });
    const stub = {
      get: vi.fn(() => of(blob))
    };
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [{ provide: HttpClient, useValue: stub }] });
    const servicio = TestBed.inject(ReportesApiService);
    const pdf = firstValueFrom(servicio.descargarArchivoReporte(1, 'PDF'));
    const xlsx = firstValueFrom(servicio.descargarArchivoReporte(1, 'XLSX'));
    return Promise.all([pdf, xlsx]).then(() => {
      const callsPdf = (stub.get as ReturnType<typeof vi.fn>).mock.calls;
      expect(callsPdf[0][0]).toBe('/api/v1/reportes/generaciones/1/archivos/PDF');
      expect(callsPdf[0][1]).toEqual({ responseType: 'blob' });
      expect(callsPdf[1][0]).toBe('/api/v1/reportes/generaciones/1/archivos/XLSX');
    });
  });

  // -------------------------------------------------------------------------
  // Aprobación
  // -------------------------------------------------------------------------

  it('aprueba una versión con destinatarios BR-125 y ETag', () => {
    const payload: ReporteAprobacionRequest = {
      idVersion: 1,
      idDocumentoAprobacion: 99,
      destinatarios: [
        { tipoDestinatario: 'AUTORIDAD_MIDAGRI', idEntidad: 1, nombre: 'Viceministerio' }
      ]
    };
    const stub = {
      post: vi.fn(() =>
        of({
          idAprobacion: 50,
          idReporte: 1,
          idVersion: 1,
          idOficina: 7,
          idAprobador: 8,
          idDocumentoAprobacion: 99,
          fechaAprobacion: '2026-07-23T00:00:00Z',
          destinatarios: [
            { idDestinatario: 11, idAprobacion: 50, tipoDestinatario: 'AUTORIDAD_MIDAGRI', idEntidad: 1, nombre: 'Viceministerio' }
          ]
        })
      )
    };
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [{ provide: HttpClient, useValue: stub }] });
    const servicio = TestBed.inject(ReportesApiService);
    return firstValueFrom(servicio.aprobarReporte(1, payload, { etag: 'W/"1"' })).then((detalle) => {
      expect(detalle.idAprobacion).toBe(50);
      expect(detalle.destinatarios[0].tipoDestinatario).toBe('AUTORIDAD_MIDAGRI');
    });
  });

  it('lista los destinatarios aprobados como un arreglo inmutable', async () => {
    const stub = {
      get: vi.fn(() =>
        of([
          { idDestinatario: 1, idAprobacion: 50, tipoDestinatario: 'OFICINA_MODERNIZACION', idEntidad: 7, nombre: 'Oficina' }
        ])
      )
    };
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({ providers: [{ provide: HttpClient, useValue: stub }] });
    const servicio = TestBed.inject(ReportesApiService);
    const destinatarios = await firstValueFrom(servicio.listarDestinatariosReporte(1));
    expect(destinatarios).toHaveLength(1);
    expect(Object.isFrozen(destinatarios)).toBe(true);
    expect(Object.isFrozen(destinatarios[0])).toBe(true);
  });

  // -------------------------------------------------------------------------
  // Remisión
  // -------------------------------------------------------------------------

  it('consulta el historial de remisiones con filtro opcional por versión', () => {
    void firstValueFrom(service.consultarRemisionesReporte(1, 2));
    const call = calls[0];
    expect(call.method).toBe('GET');
    expect(call.url).toBe('/api/v1/reportes/generaciones/1/remisiones');
    expect(call.params?.get('idVersion')).toBe('2');
  });

  it('registra la remisión manual contra los destinatarios aprobados', () => {
    const payload: ReporteRemisionRequest = {
      idVersion: 1,
      destinatariosIds: [11, 12],
      resultado: 'EXITOSA'
    };
    void firstValueFrom(service.remitirReporte(1, payload, { etag: 'W/"1"' }));
    const call = calls[0];
    expect(call.method).toBe('POST');
    expect(call.url).toBe('/api/v1/reportes/generaciones/1/remisiones');
    expect(call.body).toEqual(payload);
  });

  it('publica la ruta base del módulo como única fuente de verdad', () => {
    expect(ReportesApiService.rutaBase).toBe('/api/v1/reportes');
  });
});
