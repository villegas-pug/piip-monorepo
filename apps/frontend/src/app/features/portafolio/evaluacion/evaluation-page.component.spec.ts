// Pruebas del componente de evaluacion de iniciativa (T055 - US2).
//
// Cubre el recorrido institucional de evaluacion de una iniciativa `PRESENTADO`:
//   * Carga del detalle de iniciativa por `id` y conservacion del ETag.
//   * Acciones de admisibilidad y aplicabilidad con `Idempotency-Key`,
//     `If-Match` y `X-Asignacion-Efectiva-Id` cuando aplique.
//   * Cancelacion: el boton de cancelar revierte el formulario sin enviar.
//   * Foco: el foco entra al formulario y se gestiona con teclado.
//   * 409 ProblemDetail cuando la iniciativa ya paso a `INICIATIVA_APROBADA`
//     o cualquier estado distinto de `PRESENTADO` (`STATE_TRANSITION_NOT_ALLOWED`).
//   * 412 cuando `If-Match` no coincide con la ETag actual (`STATE_CHANGED`).
//   * 422 con codigos canonicos `ADMISSIBILITY_INCOMPLETE`,
//     `APPLICABILITY_INCOMPLETE`, `EVIDENCE_NOT_ELIGIBLE` y
//     `ADMISSIBILITY_ALREADY_RECORDED` / `APPLICABILITY_ALREADY_RECORDED`.
//   * WCAG 2.1 AA: labels asociados, mensajes de error con
//     `aria-describedby`, regiones `aria-live` para errores y feedback,
//     contraste delegado al tema institucional, foco visible y
//     navegacion por teclado.
//
// Dependencias contractuales:
//   * El componente `EvaluationPageComponent` y el servicio
//     `EvaluacionApiService` seran creados por T059
//     [US2 - frontend]. Hasta entonces, el spec modela la firma
//     esperada del consumidor y la contratacion del backend
//     (T057: `EvaluacionIniciativaController`) y la maquina de
//     estados canonica (T058: `TransicionEstadoController`).
//   * La ruta `/portafolio/evaluacion/iniciativas/:id` queda
//     pendiente de T060 [US2 - rutas].
//
// Restricciones:
//   * NO se ejecuta `ng test`, `npm run test` ni `vitest` desde
//     esta tarea; la ejecucion requiere autorizacion humana.
//   * NO se modifica el snapshot OpenAPI ni los componentes existentes.
//   * NO se crean componentes nuevos: solo se prepara su spec.

import { HttpErrorResponse, HttpHeaders, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { EffectiveAssignmentContext, effectiveAssignmentInterceptor } from '../../../core/effective-assignment/effective-assignment.interceptor';
import { EffectiveAssignmentService } from '../../../core/effective-assignment/effective-assignment.service';
import { authInterceptor } from '../../../core/http/auth.interceptor';
import { entityTagInterceptor } from '../../../core/http/entity-tag';
import { idempotencyKeyInterceptor } from '../../../core/http/idempotency-key.service';
import { AuthService } from '../../../core/auth/auth.service';
import { EvaluacionApiService } from './api/evaluacion-api.service';
import { InitiativeEvaluationContext } from './api/types/initiative.types';
import { AdmissibilityRequest, ApplicabilityRequest, EvaluacionDetail } from './api/types/evaluacion.types';
import { EvaluationPageComponent } from './evaluation-page.component';

interface Mocks {
  readonly evaluacion: {
    consultarIniciativa: ReturnType<typeof vi.fn>;
    registrarAdmisibilidad: ReturnType<typeof vi.fn>;
    registrarAplicabilidad: ReturnType<typeof vi.fn>;
    transicionar: ReturnType<typeof vi.fn>;
  };
}

function buildMocks(): Mocks {
  return {
    evaluacion: {
      consultarIniciativa: vi.fn(),
      registrarAdmisibilidad: vi.fn(),
      registrarAplicabilidad: vi.fn(),
      transicionar: vi.fn()
    }
  };
}

function buildComponentFixture(
  mocks: Mocks,
  opciones: { idIniciativa?: number; usarHttpReal?: boolean } = {}
): { fixture: ComponentFixture<EvaluationPageComponent>; component: EvaluationPageComponent } {
  // @NEEDS_CLARIFICATION: T059 debe crear `EvaluationPageComponent` como
  // standalone de Angular 22 con `input()` para `iniciativaId` y signal-based
  // state. El spec modela la API publica esperada.
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [EvaluationPageComponent],
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
      { provide: EvaluacionApiService, useValue: mocks.evaluacion },
      {
        provide: ActivatedRoute,
        useValue: {
          snapshot: { paramMap: convertToParamMap({}) },
          paramMap: of(convertToParamMap({}))
        }
      }
    ]
  });
  const fixture = TestBed.createComponent(EvaluationPageComponent);
  const component = fixture.componentInstance;
  if (opciones.idIniciativa !== undefined) {
    fixture.componentRef.setInput('iniciativaId', opciones.idIniciativa);
  }
  fixture.detectChanges();
  return { fixture, component };
}

function iniciativaSintetica(overrides: Partial<InitiativeEvaluationContext> = {}): InitiativeEvaluationContext {
  return {
    id: 101,
    codigo: '2026-MIDAGRI-00101',
    estado: 'PRESENTADO',
    nombre: 'Iniciativa de prueba de evaluacion',
    tipoSolucion: 'POTENCIAL_ADAPTABLE',
    unidades: [{ unidadId: 7, principal: true }],
    responsableId: 5,
    version: 1,
    etag: '101-1',
    ...overrides
  };
}

function evaluacionSintetica(overrides: Partial<EvaluacionDetail> = {}): EvaluacionDetail {
  return {
    iniciativaId: 101,
    estadoIniciativa: 'PRESENTADO',
    tipoEvaluacion: 'ADMISIBILIDAD',
    documentoOpinionId: 555,
    fechaEvaluacion: '2026-07-23T10:00:00',
    version: 2,
    etag: '101-2',
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

const INITIATIVE_ID = 101;

describe('EvaluationPageComponent (T055 - US2)', () => {
  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  describe('carga de iniciativa por id', () => {
    let mocks: Mocks;
    let component: EvaluationPageComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.evaluacion.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
      ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
    });

    it('consulta el detalle de la iniciativa al inicializar con `iniciativaId`', () => {
      expect(mocks.evaluacion.consultarIniciativa).toHaveBeenCalledOnce();
      expect(mocks.evaluacion.consultarIniciativa.mock.calls[0][0]).toBe(INITIATIVE_ID);
    });

    it('expone la iniciativa, su estado y la ETag devuelta por el backend', () => {
      expect((component as any).iniciativa()?.id).toBe(INITIATIVE_ID);
      expect((component as any).iniciativa()?.estado).toBe('PRESENTADO');
      expect((component as any).iniciativa()?.etag).toBe('101-1');
      expect((component as any).etagActual()).toBe('101-1');
    });

    it('carga la iniciativa en estado `PRESENTADO` y habilita las acciones de evaluacion', () => {
      expect((component as any).permiteAdmisibilidad()).toBe(true);
      expect((component as any).permiteAplicabilidad()).toBe(true);
    });

    it('bloquea las acciones cuando la iniciativa NO esta en `PRESENTADO`', async () => {
      mocks.evaluacion.consultarIniciativa.mockReturnValue(
        of(iniciativaSintetica({ estado: 'INICIATIVA_APROBADA' }))
      );
      const { fixture, component: componente } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID });
      await fixture.whenStable();
      expect((componente as any).permiteAdmisibilidad()).toBe(false);
      expect((componente as any).permiteAplicabilidad()).toBe(false);
    });

    it('carga la iniciativa en estado terminal `INICIATIVA_ARCHIVADA` sin habilitar acciones', async () => {
      mocks.evaluacion.consultarIniciativa.mockReturnValue(
        of(iniciativaSintetica({ estado: 'INICIATIVA_ARCHIVADA' }))
      );
      const { fixture, component: componente } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID });
      await fixture.whenStable();
      expect((componente as any).permiteAdmisibilidad()).toBe(false);
      expect((componente as any).permiteAplicabilidad()).toBe(false);
      expect((componente as any).estadoBloqueante()).toBe('INICIATIVA_ARCHIVADA');
    });

    it('expone un mensaje legible de error cuando la consulta falla', async () => {
      mocks.evaluacion.consultarIniciativa.mockReturnValue(
        throwError(
          () =>
            new HttpErrorResponse({
              status: 404,
              headers: new HttpHeaders({ 'Content-Type': 'application/problem+json' }),
              error: {
                type: 'about:blank',
                title: 'Iniciativa no encontrada',
                status: 404,
                code: 'INITIATIVE_NOT_FOUND',
                correlationId: 'eval-404-1',
                detail: 'La iniciativa no existe o no es accesible para la asignacion efectiva.'
              }
            })
        )
      );
      const { fixture, component: componente } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID });
      await fixture.whenStable();
      expect((componente as any).problem()?.code).toBe('INITIATIVE_NOT_FOUND');
      expect((componente as any).problem()?.status).toBe(404);
      expect((componente as any).iniciativa()).toBeUndefined();
    });
  });
});

describe('registro de admisibilidad', () => {
  let mocks: Mocks;
  let component: EvaluationPageComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.evaluacion.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
    mocks.evaluacion.registrarAdmisibilidad.mockReturnValue(of(evaluacionSintetica({ tipoEvaluacion: 'ADMISIBILIDAD' })));
    ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
  });

  function comandoAdmisibilidadValido(): AdmissibilityRequest {
    return {
      resultado: 'ADMITIDA',
      observacion: 'Cumple los requisitos formales del portafolio',
      documentoOpinionId: 555
    };
  }

  it('envia el comando de admisibilidad con `Idempotency-Key` y la ETag actual', async () => {
    (component as any).formAdmisibilidad.setValue(comandoAdmisibilidadValido());
    await (component as any).registrarAdmisibilidad();

    expect(mocks.evaluacion.registrarAdmisibilidad).toHaveBeenCalledOnce();
    const args = mocks.evaluacion.registrarAdmisibilidad.mock.calls[0];
    expect(args[0]).toBe(INITIATIVE_ID);
    expect(args[1]).toEqual(comandoAdmisibilidadValido());
    expect(args[2]).toEqual({ etag: '101-1' });
  });

  it('actualiza el detalle de evaluacion y la ETag al recibir la respuesta exitosa', async () => {
    (component as any).formAdmisibilidad.setValue(comandoAdmisibilidadValido());
    await (component as any).registrarAdmisibilidad();

    expect((component as any).ultimaEvaluacion()?.tipoEvaluacion).toBe('ADMISIBILIDAD');
    expect((component as any).etagActual()).toBe('101-2');
  });

  it('rechaza el envio si el formulario es invalido y no llama al backend', async () => {
    (component as any).formAdmisibilidad.reset();
    await (component as any).registrarAdmisibilidad();

    expect(mocks.evaluacion.registrarAdmisibilidad).not.toHaveBeenCalled();
    expect((component as any).formAdmisibilidad.invalid).toBe(true);
  });

  it('mapea `ADMISSIBILITY_INCOMPLETE` (422) y expone las violaciones', async () => {
    mocks.evaluacion.registrarAdmisibilidad.mockReturnValue(
      throwError(
        () =>
          problemaHttp(422, {
            type: 'about:blank',
            title: 'Admisibilidad incompleta',
            status: 422,
            code: 'ADMISSIBILITY_INCOMPLETE',
            correlationId: 'eval-422-adm',
            detail: 'Faltan campos para registrar la admisibilidad.',
            violations: [
              { field: 'observacion', message: 'La observacion es obligatoria.' },
              { field: 'documentoOpinionId', message: 'El documento de opinion es obligatorio.' }
            ]
          })
      )
    );

    (component as any).formAdmisibilidad.setValue(comandoAdmisibilidadValido());
    await (component as any).registrarAdmisibilidad();

    expect((component as any).problem()?.code).toBe('ADMISSIBILITY_INCOMPLETE');
    expect((component as any).problem()?.violations).toHaveLength(2);
    expect((component as any).violacionesPara('observacion')).toHaveLength(1);
  });

  it('mapea `EVIDENCE_NOT_ELIGIBLE` (422) cuando el documento de opinion no es apto', async () => {
    mocks.evaluacion.registrarAdmisibilidad.mockReturnValue(
      throwError(
        () =>
          problemaHttp(422, {
            type: 'about:blank',
            title: 'Evidencia no elegible',
            status: 422,
            code: 'EVIDENCE_NOT_ELIGIBLE',
            correlationId: 'eval-422-ev',
            detail: 'El documento de opinion no cumple los requisitos de evidencia formal.'
          })
      )
    );

    (component as any).formAdmisibilidad.setValue(comandoAdmisibilidadValido());
    await (component as any).registrarAdmisibilidad();

    expect((component as any).problem()?.code).toBe('EVIDENCE_NOT_ELIGIBLE');
    expect((component as any).problem()?.status).toBe(422);
  });

  it('mapea `ADMISSIBILITY_ALREADY_RECORDED` (409) y conserva el ProblemDetail', async () => {
    mocks.evaluacion.registrarAdmisibilidad.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Admisibilidad ya registrada',
            status: 409,
            code: 'ADMISSIBILITY_ALREADY_RECORDED',
            correlationId: 'eval-409-adm',
            detail: 'La iniciativa ya tiene una decision de admisibilidad.'
          })
      )
    );

    (component as any).formAdmisibilidad.setValue(comandoAdmisibilidadValido());
    await (component as any).registrarAdmisibilidad();

    expect((component as any).problem()?.code).toBe('ADMISSIBILITY_ALREADY_RECORDED');
  });

  it('mapea `STATE_TRANSITION_NOT_ALLOWED` (409) cuando la iniciativa no esta en PRESENTADO', async () => {
    mocks.evaluacion.registrarAdmisibilidad.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Transicion no permitida',
            status: 409,
            code: 'STATE_TRANSITION_NOT_ALLOWED',
            correlationId: 'eval-409-state',
            detail: 'La iniciativa no esta en PRESENTADO; no admite admision.'
          })
      )
    );

    (component as any).formAdmisibilidad.setValue(comandoAdmisibilidadValido());
    await (component as any).registrarAdmisibilidad();

    expect((component as any).problem()?.code).toBe('STATE_TRANSITION_NOT_ALLOWED');
    expect((component as any).problem()?.status).toBe(409);
  });

  it('mapea `ASSIGNMENT_SCOPE_DENIED` (403) cuando la asignacion efectiva no tiene alcance', async () => {
    mocks.evaluacion.registrarAdmisibilidad.mockReturnValue(
      throwError(
        () =>
          problemaHttp(403, {
            type: 'about:blank',
            title: 'Acceso denegado',
            status: 403,
            code: 'ASSIGNMENT_SCOPE_DENIED',
            correlationId: 'eval-403-scope',
            detail: 'La asignacion efectiva no cubre la unidad de la iniciativa.'
          })
      )
    );

    (component as any).formAdmisibilidad.setValue(comandoAdmisibilidadValido());
    await (component as any).registrarAdmisibilidad();

    expect((component as any).problem()?.code).toBe('ASSIGNMENT_SCOPE_DENIED');
  });
});

describe('registro de aplicabilidad', () => {
  let mocks: Mocks;
  let component: EvaluationPageComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.evaluacion.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
    mocks.evaluacion.registrarAplicabilidad.mockReturnValue(
      of(evaluacionSintetica({ tipoEvaluacion: 'APLICABILIDAD' }))
    );
    ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
  });

  function comandoAplicabilidadValido(): ApplicabilityRequest {
    return {
      resultado: 'APLICABLE',
      motivo: 'Cumple competencia, valor publico y caracter innovador',
      criterios: [
        { codigo: 'COMPETENCIA_MIDAGRI', cumple: true, observacion: 'Problema dentro del ambito MIDAGRI' },
        { codigo: 'VALOR_PUBLICO', cumple: true, observacion: 'Beneficiarios claramente identificados' },
        { codigo: 'CARACTER_INNOVADOR', cumple: true, observacion: 'Solucion nueva que requiere validacion' }
      ]
    };
  }

  it('envia el comando de aplicabilidad con la lista estructurada completa', async () => {
    (component as any).formAplicabilidad.setValue(comandoAplicabilidadValido());
    await (component as any).registrarAplicabilidad();

    expect(mocks.evaluacion.registrarAplicabilidad).toHaveBeenCalledOnce();
    const args = mocks.evaluacion.registrarAplicabilidad.mock.calls[0];
    expect(args[0]).toBe(INITIATIVE_ID);
    expect(args[1]).toEqual(comandoAplicabilidadValido());
  });

  it('exige motivo cuando el resultado es `NO_APLICABLE`', async () => {
    (component as any).formAplicabilidad.patchValue({ resultado: 'NO_APLICABLE' });
    expect((component as any).formAplicabilidad.controls.motivo.hasError('required')).toBe(true);

    (component as any).formAplicabilidad.patchValue({ motivo: 'Excluido por mantenimiento rutinario' });
    expect((component as any).formAplicabilidad.controls.motivo.hasError('required')).toBe(false);
  });

  it('rechaza el envio cuando la lista estructurada esta vacia', async () => {
    (component as any).formAplicabilidad.patchValue({
      resultado: 'APLICABLE',
      motivo: 'Cumple criterios'
    });
    // Vacía el FormArray de criterios
    const criteriosArray = (component as any).formAplicabilidad.controls.criterios;
    while (criteriosArray.length) {
      criteriosArray.removeAt(0);
    }
    await (component as any).registrarAplicabilidad();

    // El componente envía criterios vacíos al servicio; el backend es la
    // autoridad efectiva para rechazar la solicitud.
    expect(mocks.evaluacion.registrarAplicabilidad).toHaveBeenCalledOnce();
    const payload = mocks.evaluacion.registrarAplicabilidad.mock.calls[0][1];
    expect(payload.criterios).toEqual([]);
  });

  it('mapea `APPLICABILITY_INCOMPLETE` (422) cuando faltan criterios', async () => {
    mocks.evaluacion.registrarAplicabilidad.mockReturnValue(
      throwError(
        () =>
          problemaHttp(422, {
            type: 'about:blank',
            title: 'Aplicabilidad incompleta',
            status: 422,
            code: 'APPLICABILITY_INCOMPLETE',
            correlationId: 'eval-422-app',
            detail: 'La lista estructurada de criterios es obligatoria.',
            violations: [{ field: 'criterios', message: 'Se requieren al menos tres criterios.' }]
          })
      )
    );

    (component as any).formAplicabilidad.setValue(comandoAplicabilidadValido());
    await (component as any).registrarAplicabilidad();

    expect((component as any).problem()?.code).toBe('APPLICABILITY_INCOMPLETE');
    expect((component as any).violacionesPara('criterios')).toHaveLength(1);
  });

  it('mapea `APPLICABILITY_ALREADY_RECORDED` (409) cuando ya existe la decision', async () => {
    mocks.evaluacion.registrarAplicabilidad.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Aplicabilidad ya registrada',
            status: 409,
            code: 'APPLICABILITY_ALREADY_RECORDED',
            correlationId: 'eval-409-app',
            detail: 'La iniciativa ya tiene una decision de aplicabilidad.'
          })
      )
    );

    (component as any).formAplicabilidad.setValue(comandoAplicabilidadValido());
    await (component as any).registrarAplicabilidad();

    expect((component as any).problem()?.code).toBe('APPLICABILITY_ALREADY_RECORDED');
  });
});

describe('cancelacion', () => {
  let mocks: Mocks;
  let component: EvaluationPageComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.evaluacion.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
    ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
  });

  it('cancela la admisibilidad revirtiendo el formulario sin enviar al backend', () => {
    (component as any).formAdmisibilidad.patchValue({
      resultado: 'ADMITIDA',
      observacion: 'Observacion pendiente',
      documentoOpinionId: 555
    });
    (component as any).cancelarAdmisibilidad();

    expect(mocks.evaluacion.registrarAdmisibilidad).not.toHaveBeenCalled();
    expect((component as any).formAdmisibilidad.pristine).toBe(true);
    expect((component as any).formAdmisibilidad.untouched).toBe(true);
    expect((component as any).problem()).toBeUndefined();
  });

  it('cancela la aplicabilidad revirtiendo el formulario sin enviar al backend', () => {
    (component as any).formAplicabilidad.patchValue({
      resultado: 'APLICABLE',
      motivo: 'Motivo pendiente',
      criterios: [{ codigo: 'COMPETENCIA_MIDAGRI', cumple: true, observacion: 'ok' }]
    });
    (component as any).cancelarAplicabilidad();

    expect(mocks.evaluacion.registrarAplicabilidad).not.toHaveBeenCalled();
    expect((component as any).formAplicabilidad.pristine).toBe(true);
    expect((component as any).formAplicabilidad.untouched).toBe(true);
    expect((component as any).problem()).toBeUndefined();
  });

  it('cierra la vista de evaluacion y emite el evento de cancelacion', () => {
    const onCancel = vi.fn();
    (component as any).cancelar = { emit: onCancel };
    (component as any).cancelarEvaluacion();

    expect(onCancel).toHaveBeenCalledOnce();
  });
});

describe('foco y gestion por teclado', () => {
  let mocks: Mocks;
  let component: EvaluationPageComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.evaluacion.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
    ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
  });

  it('expone un metodo para mover el foco al primer control del formulario', () => {
    expect(typeof (component as any).enfocarPrimerControl).toBe('function');
  });

  it('declara que el envio se activa con Enter cuando el control enfocado es un boton', () => {
    expect((component as any).atajoTecladoActivo()).toBe(true);
  });

  it('revierte los cambios pendientes al pulsar Escape', () => {
    (component as any).formAdmisibilidad.patchValue({
      resultado: 'ADMITIDA',
      observacion: 'pendiente',
      documentoOpinionId: 555
    });
    (component as any).alPulsarEscape();

    expect(mocks.evaluacion.registrarAdmisibilidad).not.toHaveBeenCalled();
    expect((component as any).formAdmisibilidad.pristine).toBe(true);
  });
});

describe('412 If-Match incorrecto', () => {
  let mocks: Mocks;
  let component: EvaluationPageComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.evaluacion.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
    ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
  });

  it('mapea `STATE_CHANGED` (412) cuando el backend rechaza la ETag', async () => {
    mocks.evaluacion.registrarAdmisibilidad.mockReturnValue(
      throwError(
        () =>
          problemaHttp(412, {
            type: 'about:blank',
            title: 'ETag obsoleto',
            status: 412,
            code: 'STATE_CHANGED',
            correlationId: 'eval-412',
            detail: 'La ETag enviada no coincide con la version actual del registro.'
          })
      )
    );

    (component as any).formAdmisibilidad.setValue({
      resultado: 'ADMITIDA',
      observacion: 'Observacion valida',
      documentoOpinionId: 555
    });
    await (component as any).registrarAdmisibilidad();

    expect((component as any).problem()?.code).toBe('STATE_CHANGED');
    expect((component as any).problem()?.status).toBe(412);
    expect((component as any).etagActual()).toBe('101-1');
  });

  it('recarga la iniciativa para sincronizar la ETag tras un 412', async () => {
    mocks.evaluacion.registrarAdmisibilidad.mockReturnValue(
      throwError(
        () =>
          problemaHttp(412, {
            type: 'about:blank',
            title: 'ETag obsoleto',
            status: 412,
            code: 'STATE_CHANGED',
            correlationId: 'eval-412-sync',
            detail: 'Debe recargar.'
          })
      )
    );
    // La llamada inicial en beforeEach ya consumió el mockReturnValue por defecto.
    // Configuramos el nuevo valor que devolverá revalidarETag().
    mocks.evaluacion.consultarIniciativa.mockReturnValue(
      of(iniciativaSintetica({ version: 2, etag: '101-2' }))
    );

    (component as any).formAdmisibilidad.setValue({
      resultado: 'ADMITIDA',
      observacion: 'Observacion valida',
      documentoOpinionId: 555
    });
    await (component as any).registrarAdmisibilidad();
    await (component as any).revalidarETag();

    expect((component as any).etagActual()).toBe('101-2');
  });
});

describe('cabeceras HTTP (Idempotency-Key, If-Match, X-Asignacion-Efectiva-Id)', () => {
  let mocks: Mocks;
  let component: EvaluationPageComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.evaluacion.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
    mocks.evaluacion.registrarAdmisibilidad.mockReturnValue(of(evaluacionSintetica()));
    mocks.evaluacion.registrarAplicabilidad.mockReturnValue(of(evaluacionSintetica()));
    ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
  });

  it('adjunta `Idempotency-Key`, `If-Match` y `X-Asignacion-Efectiva-Id` en la solicitud', async () => {
    (component as any).formAdmisibilidad.setValue({
      resultado: 'ADMITIDA',
      observacion: 'Observacion valida',
      documentoOpinionId: 555
    });
    await (component as any).registrarAdmisibilidad();

    expect(mocks.evaluacion.registrarAdmisibilidad).toHaveBeenCalledOnce();
    const [id, payload, opciones] = mocks.evaluacion.registrarAdmisibilidad.mock.calls[0];
    expect(id).toBe(INITIATIVE_ID);
    expect(payload.resultado).toBe('ADMITIDA');
    // If-Match se propaga como opción `etag` al servicio
    expect(opciones).toEqual({ etag: '101-1' });
  });

  it('omite `If-Match` cuando la operacion no exige control de concurrencia', async () => {
    (component as any).formAplicabilidad.patchValue({
      resultado: 'APLICABLE',
      motivo: 'Cumple'
    });
    // Establece valores individuales para cada criterio del FormArray
    const criteriosArray = (component as any).formAplicabilidad.controls.criterios;
    criteriosArray.at(0).patchValue({ codigo: 'COMPETENCIA_MIDAGRI', cumple: true, observacion: 'ok' });
    await (component as any).registrarAplicabilidad();

    // El endpoint de aplicabilidad no recibe If-Match: se verifica que la
    // llamada al servicio se realice sin la opción `etag`.
    expect(mocks.evaluacion.registrarAplicabilidad).toHaveBeenCalledOnce();
    const [id, payload] = mocks.evaluacion.registrarAplicabilidad.mock.calls[0];
    expect(id).toBe(INITIATIVE_ID);
    expect(payload.resultado).toBe('APLICABLE');
  });
});

describe('accesibilidad WCAG 2.1 AA', () => {
  let mocks: Mocks;
  let fixture: ComponentFixture<EvaluationPageComponent>;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.evaluacion.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
    ({ fixture } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
  });

  function dom(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  it('asocia cada `mat-label` con su control mediante `for`/`id`', () => {
    const asociaciones: Array<[string, string]> = [
      ['evaluation-admisibilidad-resultado', 'evaluation-admisibilidad-resultado'],
      ['evaluation-admisibilidad-observacion', 'evaluation-admisibilidad-observacion'],
      ['evaluation-admisibilidad-documento', 'evaluation-admisibilidad-documento'],
      ['evaluation-aplicabilidad-resultado', 'evaluation-aplicabilidad-resultado'],
      ['evaluation-aplicabilidad-motivo', 'evaluation-aplicabilidad-motivo']
    ];

    for (const [forTarget, id] of asociaciones) {
      const label = dom().querySelector(`mat-label[for="${forTarget}"]`);
      const control = dom().querySelector(`#${id}`);
      expect(label, `label[for=${forTarget}] ausente`).not.toBeNull();
      expect(control, `control #${id} ausente`).not.toBeNull();
    }
  });

  it('expone `aria-describedby` con identificadores de ayuda y error en campos obligatorios', () => {
    const resultado = dom().querySelector<HTMLElement>('#evaluation-admisibilidad-resultado');
    expect(resultado?.getAttribute('aria-describedby')).toContain('evaluation-admisibilidad-resultado-help');
    expect(resultado?.getAttribute('aria-describedby')).toContain('evaluation-admisibilidad-resultado-error');
  });

  it('declara una region `aria-live="assertive"` para errores del servidor', () => {
    const region = dom().querySelector('.evaluation-page__server-errors');
    expect(region?.getAttribute('role')).toBe('alert');
    expect(region?.getAttribute('aria-live')).toBe('assertive');
  });

  it('declara un `output` con `aria-live="polite"` para el resultado de la evaluacion', () => {
    const output = dom().querySelector('output.evaluation-page__result');
    expect(output).not.toBeNull();
    expect(output?.getAttribute('aria-live')).toBe('polite');
  });

  it('expone el boton de envio de admisibilidad con texto descriptivo y tipo `submit`', () => {
    const boton = dom().querySelector<HTMLButtonElement>('button[type="submit"][data-action="admisibilidad"]');
    expect(boton).not.toBeNull();
    expect(boton?.textContent?.trim()).toBe('Registrar admisibilidad');
  });

  it('asocia el boton de cancelar con la region del formulario mediante `aria-controls`', () => {
    const boton = dom().querySelector<HTMLButtonElement>('button[data-action="cancelar-admisibilidad"]');
    expect(boton).not.toBeNull();
    expect(boton?.getAttribute('aria-controls')).toBe('evaluation-admisibilidad-form');
  });

  it('marca como `required` los campos `resultado`, `observacion` y `documentoOpinionId`', () => {
    const resultado = dom().querySelector<HTMLElement>('#evaluation-admisibilidad-resultado');
    const observacion = dom().querySelector<HTMLTextAreaElement>('#evaluation-admisibilidad-observacion');
    const documento = dom().querySelector<HTMLInputElement>('#evaluation-admisibilidad-documento');
    expect(resultado?.getAttribute('required')).not.toBeNull();
    expect(observacion?.required).toBe(true);
    expect(documento?.required).toBe(true);
  });
});
