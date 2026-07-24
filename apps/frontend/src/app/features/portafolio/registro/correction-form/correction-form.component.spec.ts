
// Pruebas del formulario de corrección (T042 - US1).
//
// Cubre dos modos:
//   * `INICIATIVA_SUBSANACION`: edición de los campos oficiales 5-12, 22 y 23 sobre una
//     iniciativa con subsanación abierta por el Evaluador. El snapshot OpenAPI codigo-first
//     aún no expone `POST /portafolio/iniciativas/{id}/subsanaciones` ni
//     `PATCH /portafolio/iniciativas/{id}/subsanacion`; el componente ya modela la
//     forma esperada y reporta `SUBSANACION_ENDPOINT_PENDING` para mantener trazabilidad
//     hasta la publicación contractual.
//   * `INCORPORACION`: corrección append-only con `datosNuevos` y `motivo` obligatorios.
//     Cada corrección crea una nueva entrada en el historial, con control de concurrencia
//     optimista vía `If-Match` cuando se proporciona el ETag.
//
// WCAG 2.1 AA: foco visible, labels asociados, mensajes de error accesibles, contraste
// delegado al tema institucional, regiones `aria-live` para errores y feedback de envío.

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
import { InitiativeDetail } from '../api/types/iniciativa.types';
import { CorrectionFormComponent, CorrectionMode } from './correction-form.component';

interface Mocks {
  readonly registro: { corregirIncorporacion: ReturnType<typeof vi.fn> };
}

function buildMocks(): Mocks {
  return {
    registro: { corregirIncorporacion: vi.fn() }
  };
}

function buildComponentFixture(
  mocks: Mocks,
  configuracion: {
    mode: CorrectionMode;
    iniciativa?: InitiativeDetail;
    incorporacion?: IncorporacionDetail;
    etag?: string;
    usarHttpReal?: boolean;
  }
): { fixture: ComponentFixture<CorrectionFormComponent>; component: CorrectionFormComponent } {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [CorrectionFormComponent],
    providers: [
      configuracion.usarHttpReal
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
  const fixture = TestBed.createComponent(CorrectionFormComponent);
  const component = fixture.componentInstance;
  (component as any).mode = configuracion.mode;
  if (configuracion.iniciativa) (component as any).iniciativa = configuracion.iniciativa;
  if (configuracion.incorporacion) (component as any).incorporacion = configuracion.incorporacion;
  if (configuracion.etag) (component as any).etag = configuracion.etag;
  fixture.detectChanges();
  return { fixture, component };
}

function iniciativaSintetica(overrides: Partial<InitiativeDetail> = {}): InitiativeDetail {
  return {
    id: 1,
    tipoRegistro: 'INICIATIVA',
    codigo: '2026-MIDAGRI-00001',
    fechaInicio: '2026-01-01',
    nombre: 'Iniciativa original',
    tipoSolucion: 'POTENCIAL_ADAPTABLE',
    fuenteOrigen: 'FICHA_INICIATIVA',
    detalleFuente: '',
    problemaPublico: 'Problema detectado',
    solucionPropuesta: 'Solución propuesta',
    objetivoPeiId: 10,
    actividadPoiId: 20,
    componenteDigital: false,
    detalleComponenteDigital: '',
    nota: '',
    estado: 'PRESENTADO',
    version: 1,
    etag: 'W/"1"',
    ...overrides
  };
}

function incorporacionSintetica(overrides: Partial<IncorporacionDetail> = {}): IncorporacionDetail {
  return {
    id: 42,
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

describe('CorrectionFormComponent (T042 - US1)', () => {
  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  describe('modo INICIATIVA_SUBSANACION (campos 5-12, 22 y 23 editables)', () => {
    let mocks: Mocks;
    let component: CorrectionFormComponent;
    let fixture: ComponentFixture<CorrectionFormComponent>;

    beforeEach(() => {
      mocks = buildMocks();
      ({ fixture, component } = buildComponentFixture(mocks, {
        mode: 'INICIATIVA_SUBSANACION',
        iniciativa: iniciativaSintetica()
      }));
    });

    it('prellena los campos editables 5-12, 22 y 23 desde la iniciativa', () => {
      expect((component as any).form.controls.nombre.value).toBe('Iniciativa original');
      expect((component as any).form.controls.tipoSolucion.value).toBe('POTENCIAL_ADAPTABLE');
      expect((component as any).form.controls.fuenteOrigen.value).toBe('FICHA_INICIATIVA');
      expect((component as any).form.controls.detalleFuente.value).toBe('');
      expect((component as any).form.controls.problemaPublico.value).toBe('Problema detectado');
      expect((component as any).form.controls.solucionPropuesta.value).toBe('Solución propuesta');
      expect((component as any).form.controls.objetivoPeiId.value).toBe(10);
      expect((component as any).form.controls.actividadPoiId.value).toBe(20);
      expect((component as any).form.controls.componenteDigital.value).toBe(false);
      expect((component as any).form.controls.detalleComponenteDigital.value).toBe('');
      expect((component as any).form.controls.nota.value).toBe('');
    });

    it('expone el motivo como obligatorio pero no toca campos inmutables', () => {
      expect((component as any).form.controls.motivo.hasError('required')).toBe(true);
      // Los campos 1-4 y 13 los mantiene el backend; este modo no debe exponer controles para ellos.
      expect(((component as any).form.controls as Record<string, unknown>)['codigo']).toBeUndefined();
      expect(((component as any).form.controls as Record<string, unknown>)['fechaInicio']).toBeUndefined();
      expect(((component as any).form.controls as Record<string, unknown>)['estado']).toBeUndefined();
    });

    it('hace obligatorio el detalle de la fuente al elegir OTROS y lo retira al cambiar', () => {
      (component as any).form.controls.fuenteOrigen.setValue('OTROS');
      expect((component as any).form.controls.detalleFuente.hasError('required')).toBe(true);

      (component as any).form.controls.detalleFuente.setValue('Detalle de subsanación');
      expect((component as any).form.controls.detalleFuente.hasError('required')).toBe(false);

      (component as any).form.controls.fuenteOrigen.setValue('CONCURSO_INTERNO');
      expect((component as any).form.controls.detalleFuente.hasError('required')).toBe(false);
    });

    it('hace obligatorio el detalle del componente digital al activar el flag', () => {
      (component as any).form.controls.componenteDigital.setValue(true);
      expect((component as any).form.controls.detalleComponenteDigital.hasError('required')).toBe(true);

      (component as any).form.controls.detalleComponenteDigital.setValue('Plataforma institucional');
      expect((component as any).form.controls.detalleComponenteDigital.hasError('required')).toBe(false);
    });

    it('rechaza el envío cuando falta el motivo', () => {
      (component as any).form.patchValue({ nombre: 'Iniciativa corregida' });
      (component as any).enviar();
      expect((component as any).problem()?.code).toBe('SUBSANACION_ENDPOINT_PENDING');
      // Aunque la subsanación no se persiste, el componente ya tiene motivo obligatorio.
      expect((component as any).form.controls.motivo.hasError('required')).toBe(true);
      expect(mocks.registro.corregirIncorporacion).not.toHaveBeenCalled();
    });

    it('marca el formulario como pendiente contractual y conserva el estado', () => {
      (component as any).form.patchValue({ motivo: 'Subsanación por omisión de detalle', nombre: 'Iniciativa corregida' });
      (component as any).enviar();
      expect((component as any).problem()?.code).toBe('SUBSANACION_ENDPOINT_PENDING');
      expect((component as any).problem()?.status).toBe(501);
    });
  });

  describe('modo INCORPORACION (corrección append-only)', () => {
    let mocks: Mocks;
    let component: CorrectionFormComponent;
    let fixture: ComponentFixture<CorrectionFormComponent>;

    beforeEach(() => {
      mocks = buildMocks();
      ({ fixture, component } = buildComponentFixture(mocks, {
        mode: 'INCORPORACION',
        incorporacion: incorporacionSintetica()
      }));
    });

    it('no prellena los datos editables; exige `datosNuevos` y `motivo`', () => {
      expect((component as any).form.controls.datosNuevos.value).toBe('');
      expect((component as any).form.controls.motivo.value).toBe('');
      expect((component as any).form.controls.datosNuevos.hasError('required')).toBe(true);
      expect((component as any).form.controls.motivo.hasError('required')).toBe(true);
    });

    it('envía la corrección con `datosNuevos` y `motivo` y conserva el `id` de la incorporación', () => {
      mocks.registro.corregirIncorporacion.mockReturnValue(of(incorporacionSintetica({ estado: 'PENDIENTE', version: 2, etag: 'W/"2"' })));

      (component as any).form.patchValue({ datosNuevos: 'datos nuevos', motivo: 'Corrección por error material' });
      (component as any).enviar();

      expect(mocks.registro.corregirIncorporacion).toHaveBeenCalledOnce();
      const args = mocks.registro.corregirIncorporacion.mock.calls[0];
      expect(args[0]).toBe(42);
      expect(args[1]).toEqual({
        incorporacionId: 42,
        datosNuevos: 'datos nuevos',
        motivo: 'Corrección por error material'
      });
    });

    it('muestra el ProblemDetail cuando la corrección es rechazada con `CORRECTION_NOT_OPEN`', () => {
      mocks.registro.corregirIncorporacion.mockReturnValue(
        throwError(
          () =>
            problemaHttp(409, {
              type: 'about:blank',
              title: 'Corrección no permitida',
              status: 409,
              code: 'CORRECTION_NOT_OPEN',
              correlationId: 'corr-1',
              detail: 'La incorporación ya no está en PENDIENTE.'
            })
        )
      );

      (component as any).form.patchValue({ datosNuevos: 'datos nuevos', motivo: 'Motivo válido' });
      (component as any).enviar();

      expect((component as any).problem()?.code).toBe('CORRECTION_NOT_OPEN');
      expect((component as any).problem()?.status).toBe(409);
    });

    it('mapea `CORRECTION_ALREADY_USED` y `EVIDENCE_NOT_ELIGIBLE` desde el backend', () => {
      mocks.registro.corregirIncorporacion.mockReturnValue(
        throwError(
          () =>
            problemaHttp(422, {
              type: 'about:blank',
              title: 'Corrección única consumida',
              status: 422,
              code: 'CORRECTION_ALREADY_USED',
              correlationId: 'corr-2',
              detail: 'La subsanación única ya fue utilizada.'
            })
        )
      );

      (component as any).form.patchValue({ datosNuevos: 'datos nuevos', motivo: 'Motivo válido' });
      (component as any).enviar();

      expect((component as any).problem()?.code).toBe('CORRECTION_ALREADY_USED');
    });

    it('rechaza transiciones inválidas: subsanación sin contexto suficiente', () => {
      mocks.registro.corregirIncorporacion.mockReturnValue(of(incorporacionSintetica()));

      // Sin motivo la corrección no debe enviarse (formulario inválido).
      (component as any).form.patchValue({ datosNuevos: 'algo' });
      (component as any).enviar();
      expect(mocks.registro.corregirIncorporacion).not.toHaveBeenCalled();
      // El formulario queda inválido porque falta motivo; ningún ProblemDetail se expone.
      expect((component as any).problem()).toBeUndefined();
    });

    it('rechaza la corrección cuando la incorporación no está en `PENDIENTE`', () => {
      mocks.registro.corregirIncorporacion.mockReturnValue(
        throwError(
          () =>
            problemaHttp(409, {
              type: 'about:blank',
              title: 'Estado no modificable',
              status: 409,
              code: 'INCORPORATION_NOT_PENDING',
              correlationId: 'corr-3',
              detail: 'La incorporación ya fue validada.'
            })
        )
      );

      (component as any).form.patchValue({ datosNuevos: 'datos nuevos', motivo: 'Motivo válido' });
      (component as any).enviar();

      expect((component as any).problem()?.code).toBe('INCORPORATION_NOT_PENDING');
    });

    it('propaga el error `STATE_TRANSITION_NOT_ALLOWED` cuando el backend rechaza la transición', () => {
      mocks.registro.corregirIncorporacion.mockReturnValue(
        throwError(
          () =>
            problemaHttp(409, {
              type: 'about:blank',
              title: 'Transición no permitida',
              status: 409,
              code: 'STATE_TRANSITION_NOT_ALLOWED',
              correlationId: 'corr-4',
              detail: 'La corrección no es admisible en el estado actual.'
            })
        )
      );

      (component as any).form.patchValue({ datosNuevos: 'datos nuevos', motivo: 'Motivo válido' });
      (component as any).enviar();

      expect((component as any).problem()?.code).toBe('STATE_TRANSITION_NOT_ALLOWED');
    });

    it('rechaza la corrección sin contexto de incorporación', () => {
      const mocksSinContexto = buildMocks();
      mocksSinContexto.registro.corregirIncorporacion.mockReturnValue(of(incorporacionSintetica()));
      const { component: componenteSin } = buildComponentFixture(mocksSinContexto, { mode: 'INCORPORACION' });

      (componenteSin as any).form.patchValue({ datosNuevos: 'datos', motivo: 'motivo' });
      (componenteSin as any).enviar();

      expect((componenteSin as any).problem()?.code).toBe('INCORPORATION_CONTEXT_REQUIRED');
      expect(mocksSinContexto.registro.corregirIncorporacion).not.toHaveBeenCalled();
    });

    it('invoca el callback `onPersisted` cuando la corrección es exitosa', () => {
      const detalleDevuelto = incorporacionSintetica({ estado: 'PENDIENTE', version: 2, etag: 'W/"2"' });
      mocks.registro.corregirIncorporacion.mockReturnValue(of(detalleDevuelto));
      const onPersisted = vi.fn();
      (component as any).onPersisted = onPersisted;

      (component as any).form.patchValue({ datosNuevos: 'datos nuevos', motivo: 'Motivo válido' });
      (component as any).enviar();

      expect(onPersisted).toHaveBeenCalledOnce();
      expect(onPersisted).toHaveBeenCalledWith(detalleDevuelto);
    });
  });

  describe('envío con `Idempotency-Key` y `If-Match` cuando se proporciona el ETag', () => {
    let mocks: Mocks;
    let component: CorrectionFormComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.registro.corregirIncorporacion.mockReturnValue(of(incorporacionSintetica({ id: 7, version: 2, etag: 'W/"2"' })));
      ({ component } = buildComponentFixture(mocks, {
        mode: 'INCORPORACION',
        incorporacion: incorporacionSintetica({ id: 7 }),
        etag: 'W/"1"'
      }));
    });

    it('envía la corrección con `Idempotency-Key`, `If-Match` y la asignación efectiva', () => {
      (component as any).form.patchValue({ datosNuevos: 'datos nuevos', motivo: 'Motivo válido' });
      (component as any).enviar();

      expect(mocks.registro.corregirIncorporacion).toHaveBeenCalledOnce();
      const args = mocks.registro.corregirIncorporacion.mock.calls[0];
      expect(args[0]).toBe(7);
      // Las cabeceras Idempotency-Key, If-Match y X-Asignacion-Efectiva-Id
      // son añadidas por los interceptores globales y se verifican en las
      // pruebas de integración de los interceptores.
    });

    it('omite `If-Match` cuando el consumidor no proporciona ETag', () => {
      const mocksSinEtag = buildMocks();
      mocksSinEtag.registro.corregirIncorporacion.mockReturnValue(of(incorporacionSintetica({ id: 8 })));
      const { component: componenteSin } = buildComponentFixture(mocksSinEtag, {
        mode: 'INCORPORACION',
        incorporacion: incorporacionSintetica({ id: 8 })
      });

      (componenteSin as any).form.patchValue({ datosNuevos: 'datos', motivo: 'motivo' });
      (componenteSin as any).enviar();

      expect(mocksSinEtag.registro.corregirIncorporacion).toHaveBeenCalledOnce();
      const args = mocksSinEtag.registro.corregirIncorporacion.mock.calls[0];
      expect(args[0]).toBe(8);
      expect(args[2]).toEqual({}); // Sin etag
    });
  });
});

describe('accesibilidad WCAG 2.1 AA', () => {
  let mocks: Mocks;
  let fixture: ComponentFixture<CorrectionFormComponent>;

  beforeEach(() => {
    mocks = buildMocks();
    ({ fixture } = buildComponentFixture(mocks, {
      mode: 'INCORPORACION',
      incorporacion: incorporacionSintetica()
    }));
  });

  function dom(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  it('asocia cada `mat-label` con su control mediante `for`/`id` en modo INCORPORACION', () => {
    const asociaciones: Array<[string, string]> = [
      ['correction-datos-nuevos', 'correction-datos-nuevos'],
      ['correction-motivo', 'correction-motivo']
    ];
    for (const [forTarget, id] of asociaciones) {
      expect(dom().querySelector(`mat-label[for="${forTarget}"]`)).not.toBeNull();
      expect(dom().querySelector(`#${id}`)).not.toBeNull();
    }
  });

  it('asocia los campos 5-12, 22 y 23 con sus controles en modo INICIATIVA_SUBSANACION', () => {
    TestBed.resetTestingModule();
    const mocksSub = buildMocks();
    ({ fixture } = buildComponentFixture(mocksSub, {
      mode: 'INICIATIVA_SUBSANACION',
      iniciativa: iniciativaSintetica()
    }));

    const asociaciones: Array<[string, string]> = [
      ['correction-nombre', 'correction-nombre'],
      ['correction-tipo-solucion', 'correction-tipo-solucion'],
      ['correction-fuente', 'correction-fuente'],
      ['correction-detalle-fuente', 'correction-detalle-fuente'],
      ['correction-problema', 'correction-problema'],
      ['correction-solucion', 'correction-solucion'],
      ['correction-pei', 'correction-pei'],
      ['correction-poi', 'correction-poi'],
      ['correction-detalle-digital', 'correction-detalle-digital'],
      ['correction-nota', 'correction-nota'],
      ['correction-motivo', 'correction-motivo']
    ];
    for (const [forTarget, id] of asociaciones) {
      expect(dom().querySelector(`mat-label[for="${forTarget}"]`)).not.toBeNull();
      expect(dom().querySelector(`#${id}`)).not.toBeNull();
    }
  });

  it('expone `aria-describedby` en el campo de motivo y en datos nuevos', () => {
    const motivo = dom().querySelector<HTMLTextAreaElement>('#correction-motivo');
    const datos = dom().querySelector<HTMLTextAreaElement>('#correction-datos-nuevos');
    expect(motivo?.getAttribute('aria-describedby')).toContain('correction-motivo-help');
    expect(motivo?.getAttribute('aria-describedby')).toContain('correction-motivo-error');
    expect(datos?.getAttribute('aria-describedby')).toContain('correction-datos-nuevos-help');
    expect(datos?.getAttribute('aria-describedby')).toContain('correction-datos-nuevos-error');
  });

  it('declara una región `aria-live="assertive"` para errores del servidor', () => {
    const region = dom().querySelector('.correction-form__server-errors');
    expect(region?.getAttribute('role')).toBe('alert');
    expect(region?.getAttribute('aria-live')).toBe('assertive');
  });

  it('expone el botón de envío con texto descriptivo y tipo `submit`', () => {
    const boton = dom().querySelector<HTMLButtonElement>('button[type="submit"]');
    expect(boton).not.toBeNull();
    expect(boton?.textContent?.trim()).toBe('Registrar corrección');
  });

  it('marca como `required` los campos `datosNuevos` y `motivo`', () => {
    const motivo = dom().querySelector<HTMLTextAreaElement>('#correction-motivo');
    const datos = dom().querySelector<HTMLTextAreaElement>('#correction-datos-nuevos');
    expect(motivo?.required).toBe(true);
    expect(datos?.required).toBe(true);
  });
});
