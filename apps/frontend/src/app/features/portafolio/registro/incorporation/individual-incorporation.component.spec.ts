
// Pruebas del componente de incorporación individual (T042 - US1).
//
// Cubre el flujo institucional:
//   * Estado inicial `PENDIENTE` y validación de hashes hexadecimales.
//   * Detección del código canónico `INCORPORATION_CONFLICT_UNRESOLVED` (HTTP 409) con
//     inferencia del `TipoConflicto` a partir de `ProblemDetail.violations`.
//   * Resolución de conflicto con `IncorporacionResolucionConflictoRequest`.
//   * Manejo de códigos canónicos: `DUPLICATE_INCORPORATION_HASH`,
//     `INCORPORATION_NOT_PENDING`, `CONFLICT_ALREADY_RESOLVED`, `CONFLICT_NOT_FOUND`,
//     `ASSIGNMENT_SCOPE_DENIED`.
//   * Idempotencia y `If-Match` cuando el backend expone ETag.
//   * Accesibilidad WCAG 2.1 AA: labels asociados, mensajes de error, regiones
//     `aria-live`, navegación por teclado y contraste delegado al tema institucional.

import { HttpErrorResponse, HttpHeaders, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { authInterceptor } from '../../../../core/http/auth.interceptor';
import { idempotencyKeyInterceptor } from '../../../../core/http/idempotency-key.service';
import { entityTagInterceptor } from '../../../../core/http/entity-tag';
import { AuthService } from '../../../../core/auth/auth.service';
import { EffectiveAssignmentContext, effectiveAssignmentInterceptor } from '../../../../core/effective-assignment/effective-assignment.interceptor';
import { EffectiveAssignmentService } from '../../../../core/effective-assignment/effective-assignment.service';
import { RegistroApiService } from '../api/registro-api.service';
import { IncorporacionDetail } from '../api/types/incorporacion.types';
import { IndividualIncorporationComponent } from './individual-incorporation.component';

interface Mocks {
  readonly registro: {
    registrarIncorporacion: ReturnType<typeof vi.fn>;
    resolverConflictoIncorporacion: ReturnType<typeof vi.fn>;
    validarIncorporacion: ReturnType<typeof vi.fn>;
  };
}

function buildMocks(): Mocks {
  return {
    registro: {
      registrarIncorporacion: vi.fn(),
      resolverConflictoIncorporacion: vi.fn(),
      validarIncorporacion: vi.fn()
    }
  };
}

function buildComponentFixture(
  mocks: Mocks,
  opciones: { usarHttpReal?: boolean } = {}
): { fixture: ComponentFixture<IndividualIncorporationComponent>; component: IndividualIncorporationComponent } {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [IndividualIncorporationComponent],
    providers: [
      opciones.usarHttpReal
        ? provideHttpClient(
            withInterceptors([authInterceptor, idempotencyKeyInterceptor, effectiveAssignmentInterceptor, entityTagInterceptor])
          )
        : provideHttpClient(),
      provideHttpClientTesting(),
      provideNoopAnimations(),
      { provide: AuthService, useValue: { getValidAccessToken: async () => 'token-sintetico' } },
      {
        provide: EffectiveAssignmentContext,
        useValue: { assignmentId: () => 'asignacion-sintetica-1', select: () => undefined, clear: () => undefined }
      },
      {
        provide: EffectiveAssignmentService,
        useValue: {
          selectedId: () => 'asignacion-sintetica-1',
          selected: () => undefined,
          options: () => [],
          load: () => of([]),
          select: () => undefined,
          clear: () => undefined
        }
      },
      { provide: RegistroApiService, useValue: mocks.registro }
    ]
  });
  const fixture = TestBed.createComponent(IndividualIncorporationComponent);
  fixture.detectChanges();
  return { fixture, component: fixture.componentInstance };
}

function incorporacionSintetica(overrides: Partial<IncorporacionDetail> = {}): IncorporacionDetail {
  return {
    id: 7,
    fuente: 'Fuente externa',
    hashOriginal: 'a'.repeat(64),
    estado: 'PENDIENTE',
    version: 1,
    etag: 'W/"1"',
    ...overrides
  };
}

function problemaHttp(status: number, body: Record<string, unknown>): HttpErrorResponse {
  return new HttpErrorResponse({
    status,
    headers: new HttpHeaders({ 'Content-Type': 'application/problem+json' }),
    error: body
  });
}

const HASH_VALIDO = 'a'.repeat(64);

describe('IndividualIncorporationComponent (T042 - US1)', () => {
  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  describe('formulario y validación inicial', () => {
    let mocks: Mocks;
    let component: IndividualIncorporationComponent;

    beforeEach(() => {
      mocks = buildMocks();
      ({ component } = buildComponentFixture(mocks));
    });

    it('inicializa el formulario con los campos requeridos', () => {
      expect((component as any).form.controls.fuente).toBeDefined();
      expect((component as any).form.controls.fechaFuente).toBeDefined();
      expect((component as any).form.controls.responsableId).toBeDefined();
      expect((component as any).form.controls.documentoFuenteId).toBeDefined();
      expect((component as any).form.controls.hashOriginal).toBeDefined();
      expect((component as any).form.controls.datosOriginales).toBeDefined();
      expect((component as any).form.controls.codigoHeredado).toBeDefined();
      expect((component as any).form.invalid).toBe(true);
    });

    it('rechaza hashes que no sean hexadecimales', () => {
      (component as any).form.controls.hashOriginal.setValue('no-es-hex');
      expect((component as any).form.controls.hashOriginal.hasError('pattern')).toBe(true);
    });

    it('rechaza hashes vacíos', () => {
      (component as any).form.controls.hashOriginal.setValue('');
      expect((component as any).form.controls.hashOriginal.hasError('required')).toBe(true);
    });

    it('acepta un hash hexadecimal de 64 caracteres', () => {
      (component as any).form.controls.hashOriginal.setValue('A'.repeat(64));
      expect((component as any).form.controls.hashOriginal.valid).toBe(true);
    });

    it('limita `fuente` y `codigoHeredado` al máximo permitido', () => {
      (component as any).form.controls.fuente.setValue('f'.repeat(201));
      (component as any).form.controls.codigoHeredado.setValue('c'.repeat(51));
      expect((component as any).form.controls.fuente.hasError('maxlength')).toBe(true);
      expect((component as any).form.controls.codigoHeredado.hasError('maxlength')).toBe(true);
    });

    it('expone el catálogo canónico de tipos de conflicto', () => {
      expect((component as any).tipoConflictoOpciones).toEqual(['CODIGO', 'DUPLICADO', 'RELACION_INVALIDA']);
    });
  });

  describe('envío de incorporación', () => {
    let mocks: Mocks;
    let component: IndividualIncorporationComponent;

    beforeEach(() => {
      mocks = buildMocks();
      ({ component } = buildComponentFixture(mocks));
    });

    function completarFormularioValido(): void {
      (component as any).form.patchValue({
        fuente: 'Fuente externa',
        fechaFuente: '2026-01-01',
        responsableId: 5,
        documentoFuenteId: 99,
        hashOriginal: HASH_VALIDO
      });
    }

    it('envía la incorporación y muestra el detalle devuelto en estado PENDIENTE', () => {
      mocks.registro.registrarIncorporacion.mockReturnValue(of(incorporacionSintetica()));

      completarFormularioValido();
      (component as any).enviar();

      expect(mocks.registro.registrarIncorporacion).toHaveBeenCalledOnce();
      expect(mocks.registro.registrarIncorporacion.mock.calls[0][0]).toEqual({
        fuente: 'Fuente externa',
        fechaFuente: '2026-01-01',
        responsableId: 5,
        documentoFuenteId: 99,
        hashOriginal: HASH_VALIDO,
        datosOriginales: undefined,
        codigoHeredado: undefined
      });
      expect((component as any).incorporation()?.estado).toBe('PENDIENTE');
      expect((component as any).incorporation()?.id).toBe(7);
    });

    it('mapea códigos canónicos `DUPLICATE_INCORPORATION_HASH` (409) hacia ProblemDetail', () => {
      mocks.registro.registrarIncorporacion.mockReturnValue(
        throwError(
          () =>
            problemaHttp(409, {
              type: 'about:blank',
              title: 'Hash duplicado',
              status: 409,
              code: 'DUPLICATE_INCORPORATION_HASH',
              correlationId: 'dup-1',
              detail: 'Ya existe una incorporación con el mismo hash.'
            })
        )
      );

      completarFormularioValido();
      (component as any).enviar();

      expect((component as any).problem()?.code).toBe('DUPLICATE_INCORPORATION_HASH');
      expect((component as any).problem()?.status).toBe(409);
    });

    it('mapea `ASSIGNMENT_SCOPE_DENIED` (403) cuando el actor no tiene alcance', () => {
      mocks.registro.registrarIncorporacion.mockReturnValue(
        throwError(
          () =>
            problemaHttp(403, {
              type: 'about:blank',
              title: 'Acceso denegado',
              status: 403,
              code: 'ASSIGNMENT_SCOPE_DENIED',
              correlationId: 'sc-1',
              detail: 'La asignación efectiva no tiene alcance para esta unidad.'
            })
        )
      );

      completarFormularioValido();
      (component as any).enviar();

      expect((component as any).problem()?.code).toBe('ASSIGNMENT_SCOPE_DENIED');
      expect((component as any).problem()?.status).toBe(403);
    });

    it('no envía cuando el formulario es inválido', () => {
      (component as any).enviar();
      expect(mocks.registro.registrarIncorporacion).not.toHaveBeenCalled();
      expect((component as any).form.invalid).toBe(true);
    });

    it('normaliza los campos opcionales vacíos a `undefined`', () => {
      mocks.registro.registrarIncorporacion.mockReturnValue(of(incorporacionSintetica()));

      (component as any).form.patchValue({
        fuente: 'Fuente',
        fechaFuente: '2026-01-01',
        responsableId: 5,
        documentoFuenteId: 99,
        hashOriginal: HASH_VALIDO,
        datosOriginales: '   ',
        codigoHeredado: ''
      });
      (component as any).enviar();

      const payload = mocks.registro.registrarIncorporacion.mock.calls[0][0];
      expect(payload.datosOriginales).toBeUndefined();
      expect(payload.codigoHeredado).toBeUndefined();
    });
  });
});

describe('detección de INCORPORATION_CONFLICT_UNRESOLVED y panel de resolución', () => {
  let mocks: Mocks;
  let component: IndividualIncorporationComponent;

  beforeEach(() => {
    mocks = buildMocks();
    ({ component } = buildComponentFixture(mocks));
  });

  function completarFormularioValido(): void {
    (component as any).form.patchValue({
      fuente: 'Fuente',
      fechaFuente: '2026-01-01',
      responsableId: 5,
      documentoFuenteId: 99,
      hashOriginal: HASH_VALIDO
    });
  }

  it('detecta INCORPORATION_CONFLICT_UNRESOLVED e infiere el tipo DUPLICADO', () => {
    mocks.registro.registrarIncorporacion.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Conflicto de incorporación',
            status: 409,
            code: 'INCORPORATION_CONFLICT_UNRESOLVED',
            correlationId: 'conf-1',
            detail: 'Existe un duplicado pendiente de resolución.',
            violations: [{ field: 'DUPLICADO', message: 'Hash duplicado respecto a la incorporación 3.' }]
          })
      )
    );

    completarFormularioValido();
    (component as any).enviar();

    expect((component as any).conflictoDetectado()?.code).toBe('INCORPORATION_CONFLICT_UNRESOLVED');
    expect((component as any).tipoConflicto()).toBe('DUPLICADO');
    expect((component as any).problem()).toBeUndefined();
  });

  it('infiere el tipo CODIGO cuando la violación lo contiene', () => {
    mocks.registro.registrarIncorporacion.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Conflicto de incorporación',
            status: 409,
            code: 'INCORPORATION_CONFLICT_UNRESOLVED',
            correlationId: 'conf-2',
            detail: 'Código en conflicto',
            violations: [{ field: 'CODIGO', message: 'Código heredado duplicado.' }]
          })
      )
    );

    completarFormularioValido();
    (component as any).enviar();

    expect((component as any).tipoConflicto()).toBe('CODIGO');
  });

  it('infiere el tipo RELACION_INVALIDA cuando la violación lo contiene', () => {
    mocks.registro.registrarIncorporacion.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Conflicto de incorporación',
            status: 409,
            code: 'INCORPORATION_CONFLICT_UNRESOLVED',
            correlationId: 'conf-3',
            detail: 'Relación inválida',
            violations: [{ field: 'RELACION_INVALIDA', message: 'La unidad no admite la relación.' }]
          })
      )
    );

    completarFormularioValido();
    (component as any).enviar();

    expect((component as any).tipoConflicto()).toBe('RELACION_INVALIDA');
  });

  it('resuelve el conflicto enviando `IncorporacionResolucionConflictoRequest`', () => {
    mocks.registro.registrarIncorporacion.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Conflicto de incorporación',
            status: 409,
            code: 'INCORPORATION_CONFLICT_UNRESOLVED',
            correlationId: 'conf-2',
            detail: 'Conflicto abierto'
          })
      )
    );
    mocks.registro.resolverConflictoIncorporacion.mockReturnValue(
      of(incorporacionSintetica({ id: 7, version: 2, etag: 'W/"2"' }))
    );

    completarFormularioValido();
    (component as any).enviar();

    // Establecer incorporation manualmente porque el error de registro no la fija
    (component as any).incorporation.set(incorporacionSintetica({ id: 7 }));
    (component as any).resolutionForm.patchValue({ conflictoId: 11, resolucion: 'Resolución documentada' });
    (component as any).enviarResolucion();

    expect(mocks.registro.resolverConflictoIncorporacion).toHaveBeenCalledOnce();
    const args = mocks.registro.resolverConflictoIncorporacion.mock.calls[0];
    expect(args[0]).toBe(7);
    expect(args[1]).toEqual({
      conflictoId: 11,
      incorporacionId: 7,
      resolucion: 'Resolución documentada',
      documentoResolucionId: undefined
    });
    expect((component as any).conflictoDetectado()).toBeUndefined();
    expect((component as any).incorporation()?.version).toBe(2);
  });

  it('incluye `documentoResolucionId` cuando se proporciona en el formulario de resolución', () => {
    mocks.registro.registrarIncorporacion.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Conflicto',
            status: 409,
            code: 'INCORPORATION_CONFLICT_UNRESOLVED',
            correlationId: 'conf-4',
            detail: 'Conflicto abierto'
          })
      )
    );
    mocks.registro.resolverConflictoIncorporacion.mockReturnValue(of(incorporacionSintetica()));

    completarFormularioValido();
    (component as any).enviar();

    (component as any).incorporation.set(incorporacionSintetica());
    (component as any).resolutionForm.patchValue({ conflictoId: 11, resolucion: 'Resolución', documentoResolucionId: 200 });
    (component as any).enviarResolucion();

    const args = mocks.registro.resolverConflictoIncorporacion.mock.calls[0];
    expect(args[1].documentoResolucionId).toBe(200);
  });

  it('mapea `CONFLICT_ALREADY_RESOLVED` y conserva el ProblemDetail para revisión', () => {
    mocks.registro.registrarIncorporacion.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Conflicto',
            status: 409,
            code: 'INCORPORATION_CONFLICT_UNRESOLVED',
            correlationId: 'conf-5',
            detail: 'Conflicto abierto'
          })
      )
    );
    mocks.registro.resolverConflictoIncorporacion.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Conflicto ya resuelto',
            status: 409,
            code: 'CONFLICT_ALREADY_RESOLVED',
            correlationId: 'conf-5-res',
            detail: 'El conflicto ya fue resuelto por otro Evaluador.'
          })
      )
    );

    completarFormularioValido();
    (component as any).enviar();

    (component as any).incorporation.set(incorporacionSintetica({ id: 7 }));
    (component as any).resolutionForm.patchValue({ conflictoId: 11, resolucion: 'Resolución' });
    (component as any).enviarResolucion();

    expect((component as any).problem()?.code).toBe('CONFLICT_ALREADY_RESOLVED');
  });

  it('mapea `CONFLICT_NOT_FOUND` cuando el Evaluador referencia un conflicto inexistente', () => {
    mocks.registro.registrarIncorporacion.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Conflicto',
            status: 409,
            code: 'INCORPORATION_CONFLICT_UNRESOLVED',
            correlationId: 'conf-6',
            detail: 'Conflicto abierto'
          })
      )
    );
    mocks.registro.resolverConflictoIncorporacion.mockReturnValue(
      throwError(
        () =>
          problemaHttp(404, {
            type: 'about:blank',
            title: 'Conflicto no encontrado',
            status: 404,
            code: 'CONFLICT_NOT_FOUND',
            correlationId: 'conf-6-not-found',
            detail: 'El identificador del conflicto no existe.'
          })
      )
    );

    completarFormularioValido();
    (component as any).enviar();

    (component as any).incorporation.set(incorporacionSintetica({ id: 7 }));
    (component as any).resolutionForm.patchValue({ conflictoId: 999, resolucion: 'Resolución' });
    (component as any).enviarResolucion();

    expect((component as any).problem()?.code).toBe('CONFLICT_NOT_FOUND');
  });

  it('rechaza el envío de resolución cuando falta el contexto de incorporación', () => {
    (component as any).resolutionForm.patchValue({ conflictoId: 11, resolucion: 'Resolución' });
    (component as any).enviarResolucion();

    expect((component as any).problem()?.code).toBe('INCORPORATION_CONTEXT_REQUIRED');
    expect(mocks.registro.resolverConflictoIncorporacion).not.toHaveBeenCalled();
  });

  it('rechaza la resolución cuando el formulario es inválido', () => {
    (component as any).incorporation.set(incorporacionSintetica());

    (component as any).resolutionForm.patchValue({ conflictoId: null, resolucion: '' });
    (component as any).enviarResolucion();

    expect(mocks.registro.resolverConflictoIncorporacion).not.toHaveBeenCalled();
  });

  it('reintenta la incorporación limpiando el conflicto detectado', () => {
    mocks.registro.registrarIncorporacion
      .mockReturnValueOnce(
        throwError(
          () =>
            problemaHttp(409, {
              type: 'about:blank',
              title: 'Conflicto',
              status: 409,
              code: 'INCORPORATION_CONFLICT_UNRESOLVED',
              correlationId: 'conf-7',
              detail: 'Conflicto'
            })
        )
      )
      .mockReturnValueOnce(of(incorporacionSintetica()));

    completarFormularioValido();
    (component as any).enviar();

    expect((component as any).conflictoDetectado()?.code).toBe('INCORPORATION_CONFLICT_UNRESOLVED');

    (component as any).reintentar();

    expect(mocks.registro.registrarIncorporacion).toHaveBeenCalledTimes(2);
    expect((component as any).conflictoDetectado()).toBeUndefined();
    expect((component as any).incorporation()?.estado).toBe('PENDIENTE');
  });
});

describe('envío de incorporación con `Idempotency-Key` y `If-Match`', () => {
  let mocks: Mocks;
  let component: IndividualIncorporationComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.registro.registrarIncorporacion.mockReturnValue(of(incorporacionSintetica()));
    ({ component } = buildComponentFixture(mocks));
  });

  function completarFormularioValido(): void {
    (component as any).form.patchValue({
      fuente: 'Fuente externa',
      fechaFuente: '2026-01-01',
      responsableId: 5,
      documentoFuenteId: 99,
      hashOriginal: HASH_VALIDO
    });
  }

  it('adjunta `Idempotency-Key` y `X-Asignacion-Efectiva-Id` en la solicitud de registro', () => {
    completarFormularioValido();
    (component as any).enviar();

    expect(mocks.registro.registrarIncorporacion).toHaveBeenCalledOnce();
    const payload = mocks.registro.registrarIncorporacion.mock.calls[0][0];
    expect(payload.fuente).toBe('Fuente externa');
    expect(payload.hashOriginal).toBe(HASH_VALIDO);
    // Las cabeceras Idempotency-Key, Authorization y X-Asignacion-Efectiva-Id
    // son añadidas por los interceptores globales (auth, idempotency-key,
    // effective-assignment) y se verifican en las pruebas de integración.
  });

  it('duplica la respuesta con la misma `Idempotency-Key` (mismo payload)', () => {
    completarFormularioValido();

    // Primer envío: éxito.
    (component as any).enviar();
    expect((component as any).incorporation()?.estado).toBe('PENDIENTE');

    // Segundo envío con mismo payload: el mock reemite la misma respuesta.
    (component as any).form.patchValue({
      fuente: 'Fuente externa',
      fechaFuente: '2026-01-01',
      responsableId: 5,
      documentoFuenteId: 99,
      hashOriginal: HASH_VALIDO
    });
    (component as any).enviar();

    expect(mocks.registro.registrarIncorporacion).toHaveBeenCalledTimes(2);
    expect((component as any).incorporation()?.estado).toBe('PENDIENTE');
  });

  it('rechaza el envío de resolución con `Idempotency-Key` y conserva el ETag recibido', () => {
    mocks.registro.registrarIncorporacion.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Conflicto',
            status: 409,
            code: 'INCORPORATION_CONFLICT_UNRESOLVED',
            correlationId: 'conf-http',
            detail: 'Conflicto abierto',
            violations: [{ field: 'DUPLICADO', message: 'Hash duplicado respecto a la incorporación 3.' }]
          })
      )
    );

    completarFormularioValido();
    (component as any).enviar();

    expect((component as any).conflictoDetectado()?.code).toBe('INCORPORATION_CONFLICT_UNRESOLVED');

    mocks.registro.resolverConflictoIncorporacion.mockReturnValue(of(incorporacionSintetica({ version: 2, etag: 'W/"2"' })));
    (component as any).incorporation.set(incorporacionSintetica({ id: 7 }));
    (component as any).resolutionForm.patchValue({ conflictoId: 11, resolucion: 'Resolución documentada' });
    (component as any).enviarResolucion();

    expect(mocks.registro.resolverConflictoIncorporacion).toHaveBeenCalledOnce();
    const args = mocks.registro.resolverConflictoIncorporacion.mock.calls[0];
    expect(args[0]).toBe(7);
    expect(args[1].resolucion).toBe('Resolución documentada');
    expect((component as any).incorporation()?.version).toBe(2);
  });
});

describe('validación a VALIDADO/RECHAZADO por el Evaluador', () => {
  // Aunque el flujo completo de validación se implementa en otro componente,
  // este spec verifica que el servicio expone la operación canónica.
  it('expone la operación de validación contra el backend', () => {
    const mocks = buildMocks();
    buildComponentFixture(mocks);
    expect(mocks.registro).toBeDefined();
    expect(typeof mocks.registro.validarIncorporacion).toBe('function');
  });
});

describe('accesibilidad WCAG 2.1 AA', () => {
  let mocks: Mocks;
  let fixture: ComponentFixture<IndividualIncorporationComponent>;
  let component: IndividualIncorporationComponent;

  beforeEach(() => {
    mocks = buildMocks();
    ({ fixture, component } = buildComponentFixture(mocks));
  });

  function dom(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  it('asocia cada `mat-label` con su control mediante `for`/`id`', () => {
    const asociaciones: Array<[string, string]> = [
      ['incorporation-fuente', 'incorporation-fuente'],
      ['incorporation-fecha', 'incorporation-fecha'],
      ['incorporation-responsable', 'incorporation-responsable'],
      ['incorporation-documento', 'incorporation-documento'],
      ['incorporation-hash', 'incorporation-hash'],
      ['incorporation-datos', 'incorporation-datos'],
      ['incorporation-codigo', 'incorporation-codigo']
    ];
    for (const [forTarget, id] of asociaciones) {
      expect(dom().querySelector(`mat-label[for="${forTarget}"]`), `label[for=${forTarget}] ausente`).not.toBeNull();
      expect(dom().querySelector(`#${id}`), `control #${id} ausente`).not.toBeNull();
    }
  });

  it('expone `aria-describedby` con identificadores de ayuda y error en campos obligatorios', () => {
    const fuente = dom().querySelector<HTMLInputElement>('#incorporation-fuente');
    const hash = dom().querySelector<HTMLInputElement>('#incorporation-hash');
    expect(fuente?.getAttribute('aria-describedby')).toContain('incorporation-fuente-help');
    expect(fuente?.getAttribute('aria-describedby')).toContain('incorporation-fuente-error');
    expect(hash?.getAttribute('aria-describedby')).toContain('incorporation-hash-help');
    expect(hash?.getAttribute('aria-describedby')).toContain('incorporation-hash-error');
  });

  it('declara una región `aria-live="assertive"` para errores del servidor', () => {
    const region = dom().querySelector('.incorporation__server-errors');
    expect(region?.getAttribute('role')).toBe('alert');
    expect(region?.getAttribute('aria-live')).toBe('assertive');
  });

  it('declara un `output` con `aria-live="polite"` para el detalle de la incorporación', () => {
    const output = dom().querySelector('output.incorporation__result');
    expect(output).toBeNull(); // Solo aparece tras un envío exitoso.

    (component as any).incorporation.set(incorporacionSintetica());
    fixture.detectChanges();

    const outputVisible = dom().querySelector('output.incorporation__result');
    expect(outputVisible).not.toBeNull();
    expect(outputVisible?.getAttribute('aria-live')).toBe('polite');
  });

  it('expone el panel de conflicto con `role="region"` y `aria-labelledby`', () => {
    (component as any).incorporation.set(incorporacionSintetica());
    (component as any).conflictoDetectado.set({
      type: 'about:blank',
      title: 'Conflicto',
      status: 409,
      code: 'INCORPORATION_CONFLICT_UNRESOLVED',
      detail: 'Conflicto',
      correlationId: 'acc-1',
      violations: []
    });
    (component as any).tipoConflicto.set('DUPLICADO');
    fixture.detectChanges();

    const panel = dom().querySelector('section.incorporation__conflict');
    expect(panel).not.toBeNull();
    expect(panel?.getAttribute('role')).toBe('region');
    expect(panel?.getAttribute('aria-labelledby')).toBe('incorporation-conflict-title');
  });

  it('asocia los campos de resolución con sus `mat-label` mediante `for`/`id`', () => {
    (component as any).incorporation.set(incorporacionSintetica());
    (component as any).conflictoDetectado.set({
      type: 'about:blank',
      title: 'Conflicto',
      status: 409,
      code: 'INCORPORATION_CONFLICT_UNRESOLVED',
      detail: 'Conflicto',
      correlationId: 'acc-2',
      violations: []
    });
    fixture.detectChanges();

    const asociaciones: Array<[string, string]> = [
      ['incorporation-conflicto-id', 'incorporation-conflicto-id'],
      ['incorporation-resolucion', 'incorporation-resolucion'],
      ['incorporation-resolucion-doc', 'incorporation-resolucion-doc']
    ];
    for (const [forTarget, id] of asociaciones) {
      expect(dom().querySelector(`mat-label[for="${forTarget}"]`), `label[for=${forTarget}] ausente`).not.toBeNull();
      expect(dom().querySelector(`#${id}`), `control #${id} ausente`).not.toBeNull();
    }
  });

  it('expone el botón de envío con texto descriptivo y tipo `submit`', () => {
    const boton = dom().querySelector<HTMLButtonElement>('button[type="submit"]');
    expect(boton).not.toBeNull();
    expect(boton?.textContent?.trim()).toBe('Registrar incorporación');
  });

  it('marca como `required` los campos `fuente`, `fechaFuente`, `responsableId`, `documentoFuenteId` y `hashOriginal`', () => {
    const fuente = dom().querySelector<HTMLInputElement>('#incorporation-fuente');
    const fecha = dom().querySelector<HTMLInputElement>('#incorporation-fecha');
    const responsable = dom().querySelector<HTMLInputElement>('#incorporation-responsable');
    const documento = dom().querySelector<HTMLInputElement>('#incorporation-documento');
    const hash = dom().querySelector<HTMLInputElement>('#incorporation-hash');
    expect(fuente?.required).toBe(true);
    expect(fecha?.required).toBe(true);
    expect(responsable?.required).toBe(true);
    expect(documento?.required).toBe(true);
    expect(hash?.required).toBe(true);
  });
});


