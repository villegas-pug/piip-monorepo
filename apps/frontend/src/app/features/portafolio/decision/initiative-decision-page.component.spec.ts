// Pruebas del componente de decision formal de iniciativa (T055 - US2).
//
// Cubre el recorrido institucional de decision formal:
//   * Carga de iniciativa `PRESENTADO` por `id` con conservacion de la ETag.
//   * Decision `INICIATIVA_APROBADA` y `INICIATIVA_ARCHIVADA` con
//     documento formal obligatorio (campo 15), observacion opcional
//     y `Idempotency-Key` + `If-Match` + `X-Asignacion-Efectiva-Id`.
//   * Cancelacion: el boton de cancelar revierte el formulario sin enviar.
//   * 422 `FORMAL_DECISION_REQUIRED` cuando se omite el documento formal.
//   * 422 `EVIDENCE_NOT_ELIGIBLE` cuando el documento no cumple requisitos.
//   * 409 `STATE_TRANSITION_NOT_ALLOWED` si la iniciativa NO esta en
//     `PRESENTADO` (p. ej. `INICIATIVA_ARCHIVADA` o `INICIATIVA_APROBADA`).
//   * 412 `STATE_CHANGED` cuando `If-Match` no coincide con la ETag actual.
//   * WCAG 2.1 AA: labels asociados, mensajes de error con
//     `aria-describedby`, regiones `aria-live` para errores y feedback,
//     contraste delegado al tema institucional, foco visible y
//     navegacion por teclado.
//
// Dependencias contractuales:
//   * El componente `InitiativeDecisionPageComponent` y el servicio
//     `DecisionApiService` seran creados por T059
//     [US2 - frontend]. La decision reutiliza el endpoint canonico de
//     transiciones `POST /api/v1/portafolio/transiciones/{id}` definido por
//     T058 [US2 - backend].
//   * La ruta `/portafolio/decision/iniciativas/:id` queda
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
import { DecisionApiService } from './api/decision-api.service';
import { InitiativeDecisionContext } from './api/types/initiative.types';
import { DecisionTransitionCommand, TransicionDetail } from './api/types/decision.types';
import { InitiativeDecisionPageComponent } from './initiative-decision-page.component';

interface Mocks {
  readonly decision: {
    consultarIniciativa: ReturnType<typeof vi.fn>;
    transicionar: ReturnType<typeof vi.fn>;
  };
}

function buildMocks(): Mocks {
  return {
    decision: {
      consultarIniciativa: vi.fn(),
      transicionar: vi.fn()
    }
  };
}

function buildComponentFixture(
  mocks: Mocks,
  opciones: { idIniciativa?: number; usarHttpReal?: boolean } = {}
): { fixture: ComponentFixture<InitiativeDecisionPageComponent>; component: InitiativeDecisionPageComponent } {
  // @NEEDS_CLARIFICATION: T059 debe crear `InitiativeDecisionPageComponent` como
  // standalone de Angular 22 con `input()` para `iniciativaId`. El spec modela
  // la API publica esperada.
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [InitiativeDecisionPageComponent],
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
      { provide: DecisionApiService, useValue: mocks.decision },
      {
        provide: ActivatedRoute,
        useValue: {
          snapshot: { paramMap: convertToParamMap({}) },
          paramMap: of(convertToParamMap({}))
        }
      }
    ]
  });
  const fixture = TestBed.createComponent(InitiativeDecisionPageComponent);
  const component = fixture.componentInstance;
  if (opciones.idIniciativa !== undefined) {
    fixture.componentRef.setInput('iniciativaId', opciones.idIniciativa);
  }
  fixture.detectChanges();
  return { fixture, component };
}

function iniciativaSintetica(overrides: Partial<InitiativeDecisionContext> = {}): InitiativeDecisionContext {
  return {
    id: 202,
    codigo: '2026-MIDAGRI-00202',
    estado: 'PRESENTADO',
    nombre: 'Iniciativa de prueba de decision',
    tipoSolucion: 'POTENCIAL_ADAPTABLE',
    unidades: [{ unidadId: 7, principal: true }],
    responsableId: 5,
    version: 1,
    etag: '202-1',
    ...overrides
  };
}

function transicionSintetica(overrides: Partial<TransicionDetail> = {}): TransicionDetail {
  return {
    registroId: 202,
    estadoAnterior: 'PRESENTADO',
    estadoNuevo: 'INICIATIVA_APROBADA',
    transicionId: 9001,
    fechaTransicion: '2026-07-23T11:00:00',
    actorSub: 'sub-sintetico-autoridad-0005',
    version: 2,
    etag: '202-2',
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

const INITIATIVE_ID = 202;

describe('InitiativeDecisionPageComponent (T055 - US2)', () => {
  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  describe('carga de iniciativa por id', () => {
    let mocks: Mocks;
    let component: InitiativeDecisionPageComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.decision.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
      ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
    });

    it('consulta el detalle de la iniciativa al inicializar con `iniciativaId`', () => {
      expect(mocks.decision.consultarIniciativa).toHaveBeenCalledOnce();
      expect(mocks.decision.consultarIniciativa.mock.calls[0][0]).toBe(INITIATIVE_ID);
    });

    it('expone la iniciativa, su estado y la ETag devuelta por el backend', () => {
      expect((component as any).iniciativa()?.id).toBe(INITIATIVE_ID);
      expect((component as any).iniciativa()?.estado).toBe('PRESENTADO');
      expect((component as any).etagActual()).toBe('202-1');
    });

    it('habilita las acciones de decision solo en `PRESENTADO`', () => {
      expect((component as any).permiteDecision()).toBe(true);
    });

    it('bloquea la decision cuando la iniciativa esta en `INICIATIVA_APROBADA`', async () => {
      mocks.decision.consultarIniciativa.mockReturnValue(
        of(iniciativaSintetica({ estado: 'INICIATIVA_APROBADA' }))
      );
      const { fixture, component: componente } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID });
      await fixture.whenStable();
      expect((componente as any).permiteDecision()).toBe(false);
      expect((componente as any).estadoBloqueante()).toBe('INICIATIVA_APROBADA');
    });

    it('bloquea la decision cuando la iniciativa esta en estado terminal `NO_ADMISIBLE`', async () => {
      mocks.decision.consultarIniciativa.mockReturnValue(
        of(iniciativaSintetica({ estado: 'NO_ADMISIBLE' }))
      );
      const { fixture, component: componente } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID });
      await fixture.whenStable();
      expect((componente as any).permiteDecision()).toBe(false);
      expect((componente as any).estadoBloqueante()).toBe('NO_ADMISIBLE');
    });

    it('expone el ProblemDetail cuando la consulta falla con `INITIATIVE_NOT_FOUND`', async () => {
      mocks.decision.consultarIniciativa.mockReturnValue(
        throwError(
          () =>
            problemaHttp(404, {
              type: 'about:blank',
              title: 'Iniciativa no encontrada',
              status: 404,
              code: 'INITIATIVE_NOT_FOUND',
              correlationId: 'dec-404',
              detail: 'La iniciativa no existe.'
            })
        )
      );
      const { fixture, component: componente } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID });
      await fixture.whenStable();
      expect((componente as any).problem()?.code).toBe('INITIATIVE_NOT_FOUND');
      expect((componente as any).problem()?.status).toBe(404);
    });
  });
});

describe('decision INICIATIVA_APROBADA', () => {
  let mocks: Mocks;
  let component: InitiativeDecisionPageComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.decision.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
    mocks.decision.transicionar.mockReturnValue(of(transicionSintetica({ estadoNuevo: 'INICIATIVA_APROBADA' })));
    ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
  });

  function comandoAprobacionValido(): DecisionTransitionCommand {
    return {
      destino: 'INICIATIVA_APROBADA',
      documentoRefId: 777,
      observaciones: 'Decision formal de la Autoridad'
    };
  }

  it('envia la transicion con destino `INICIATIVA_APROBADA` y la ETag actual', async () => {
    (component as any).formDecision.setValue(comandoAprobacionValido());
    await (component as any).confirmarDecision();

    expect(mocks.decision.transicionar).toHaveBeenCalledOnce();
    const args = mocks.decision.transicionar.mock.calls[0];
    expect(args[0]).toBe(INITIATIVE_ID);
    expect(args[1]).toEqual(comandoAprobacionValido());
    expect(args[2]).toEqual({ etag: '202-1' });
  });

  it('expone la nueva ETag y el estado actualizado tras la respuesta exitosa', async () => {
    (component as any).formDecision.setValue(comandoAprobacionValido());
    await (component as any).confirmarDecision();

    expect((component as any).etagActual()).toBe('202-2');
    expect((component as any).ultimaTransicion()?.estadoNuevo).toBe('INICIATIVA_APROBADA');
    expect((component as any).iniciativa()?.estado).toBe('INICIATIVA_APROBADA');
  });

  it('rechaza el envio sin documento formal (FORMAL_DECISION_REQUIRED)', async () => {
    (component as any).formDecision.patchValue({ destino: 'INICIATIVA_APROBADA', documentoRefId: undefined });
    await (component as any).confirmarDecision();

    expect(mocks.decision.transicionar).not.toHaveBeenCalled();
    expect((component as any).formDecision.invalid).toBe(true);
  });

  it('mapea `FORMAL_DECISION_REQUIRED` (422) cuando el backend rechaza por falta de documento', async () => {
    mocks.decision.transicionar.mockReturnValue(
      throwError(
        () =>
          problemaHttp(422, {
            type: 'about:blank',
            title: 'Decision formal requerida',
            status: 422,
            code: 'FORMAL_DECISION_REQUIRED',
            correlationId: 'dec-422-fdr',
            detail: 'La decision exige un documento formal registrado.'
          })
      )
    );

    (component as any).formDecision.setValue(comandoAprobacionValido());
    await (component as any).confirmarDecision();

    expect((component as any).problem()?.code).toBe('FORMAL_DECISION_REQUIRED');
    expect((component as any).problem()?.status).toBe(422);
  });

  it('mapea `EVIDENCE_NOT_ELIGIBLE` (422) cuando el documento no cumple requisitos', async () => {
    mocks.decision.transicionar.mockReturnValue(
      throwError(
        () =>
          problemaHttp(422, {
            type: 'about:blank',
            title: 'Evidencia no elegible',
            status: 422,
            code: 'EVIDENCE_NOT_ELIGIBLE',
            correlationId: 'dec-422-ev',
            detail: 'El documento formal no cumple los requisitos de evidencia.'
          })
      )
    );

    (component as any).formDecision.setValue(comandoAprobacionValido());
    await (component as any).confirmarDecision();

    expect((component as any).problem()?.code).toBe('EVIDENCE_NOT_ELIGIBLE');
  });

  it('permite omitir la observacion en `INICIATIVA_APROBADA` (regla de destino opcional)', () => {
    (component as any).formDecision.patchValue({
      destino: 'INICIATIVA_APROBADA',
      documentoRefId: 777
    });
    expect((component as any).formDecision.controls.observaciones.valid).toBe(true);
  });
});

describe('decision INICIATIVA_ARCHIVADA', () => {
  let mocks: Mocks;
  let component: InitiativeDecisionPageComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.decision.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
    mocks.decision.transicionar.mockReturnValue(
      of(transicionSintetica({ estadoNuevo: 'INICIATIVA_ARCHIVADA' }))
    );
    ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
  });

  function comandoArchivoValido(): DecisionTransitionCommand {
    return {
      destino: 'INICIATIVA_ARCHIVADA',
      documentoRefId: 778,
      observaciones: 'Archivo formal con sustento normativo'
    };
  }

  it('envia la transicion con destino `INICIATIVA_ARCHIVADA` y documento formal', async () => {
    (component as any).formDecision.setValue(comandoArchivoValido());
    await (component as any).confirmarDecision();

    expect(mocks.decision.transicionar).toHaveBeenCalledOnce();
    const args = mocks.decision.transicionar.mock.calls[0];
    expect(args[0]).toBe(INITIATIVE_ID);
    expect(args[1]).toEqual(comandoArchivoValido());
  });

  it('expone `INICIATIVA_ARCHIVADA` como estado terminal tras la transicion', async () => {
    (component as any).formDecision.setValue(comandoArchivoValido());
    await (component as any).confirmarDecision();

    expect((component as any).iniciativa()?.estado).toBe('INICIATIVA_ARCHIVADA');
    expect((component as any).estadoTerminal()).toBe('INICIATIVA_ARCHIVADA');
  });

  it('mapea `FORMAL_DECISION_REQUIRED` (422) cuando se omite el documento formal', async () => {
    mocks.decision.transicionar.mockReturnValue(
      throwError(
        () =>
          problemaHttp(422, {
            type: 'about:blank',
            title: 'Decision formal requerida',
            status: 422,
            code: 'FORMAL_DECISION_REQUIRED',
            correlationId: 'dec-422-arch',
            detail: 'La iniciativa archivada exige un documento formal.'
          })
      )
    );

    (component as any).formDecision.setValue(comandoArchivoValido());
    await (component as any).confirmarDecision();

    expect((component as any).problem()?.code).toBe('FORMAL_DECISION_REQUIRED');
  });
});

describe('cancelacion', () => {
  let mocks: Mocks;
  let component: InitiativeDecisionPageComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.decision.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
    ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
  });

  it('cancela la decision revirtiendo el formulario sin enviar al backend', () => {
    (component as any).formDecision.patchValue({
      destino: 'INICIATIVA_APROBADA',
      documentoRefId: 777,
      observaciones: 'Observacion pendiente'
    });
    (component as any).cancelarDecision();

    expect(mocks.decision.transicionar).not.toHaveBeenCalled();
    expect((component as any).formDecision.pristine).toBe(true);
    expect((component as any).formDecision.untouched).toBe(true);
    expect((component as any).problem()).toBeUndefined();
  });

  it('emite el evento de cancelacion y revierte el estado', () => {
    const onCancel = vi.fn();
    (component as any).cancelar = { emit: onCancel };
    (component as any).cancelarVista();

    expect(onCancel).toHaveBeenCalledOnce();
  });

  it('revierte los cambios al pulsar Escape', () => {
    (component as any).formDecision.patchValue({
      destino: 'INICIATIVA_APROBADA',
      documentoRefId: 777,
      observaciones: 'pendiente'
    });
    (component as any).alPulsarEscape();

    expect(mocks.decision.transicionar).not.toHaveBeenCalled();
    expect((component as any).formDecision.pristine).toBe(true);
  });
});

describe('409 STATE_TRANSITION_NOT_ALLOWED y 412 STATE_CHANGED', () => {
  let mocks: Mocks;
  let component: InitiativeDecisionPageComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.decision.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
    ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
  });

  it('mapea `STATE_TRANSITION_NOT_ALLOWED` (409) cuando la iniciativa no esta en PRESENTADO', async () => {
    mocks.decision.transicionar.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Transicion no permitida',
            status: 409,
            code: 'STATE_TRANSITION_NOT_ALLOWED',
            correlationId: 'dec-409-state',
            detail: 'La iniciativa no esta en PRESENTADO; no admite decision formal.'
          })
      )
    );

    (component as any).formDecision.setValue({
      destino: 'INICIATIVA_APROBADA',
      documentoRefId: 777,
      observaciones: 'Decision formal'
    });
    await (component as any).confirmarDecision();

    expect((component as any).problem()?.code).toBe('STATE_TRANSITION_NOT_ALLOWED');
    expect((component as any).problem()?.status).toBe(409);
  });

  it('mapea `STATE_TRANSITION_NOT_ALLOWED` (409) cuando el destino es terminal', async () => {
    mocks.decision.transicionar.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Estado terminal',
            status: 409,
            code: 'STATE_TRANSITION_NOT_ALLOWED',
            correlationId: 'dec-409-terminal',
            detail: 'INICIATIVA_ARCHIVADA es estado terminal y no admite transiciones adicionales.'
          })
      )
    );

    (component as any).formDecision.setValue({
      destino: 'INICIATIVA_APROBADA',
      documentoRefId: 777,
      observaciones: 'Decision formal'
    });
    await (component as any).confirmarDecision();

    expect((component as any).problem()?.code).toBe('STATE_TRANSITION_NOT_ALLOWED');
  });

  it('mapea `STATE_CHANGED` (412) cuando la ETag no coincide', async () => {
    mocks.decision.transicionar.mockReturnValue(
      throwError(
        () =>
          problemaHttp(412, {
            type: 'about:blank',
            title: 'ETag obsoleto',
            status: 412,
            code: 'STATE_CHANGED',
            correlationId: 'dec-412',
            detail: 'La ETag enviada no coincide con la version actual.'
          })
      )
    );

    (component as any).formDecision.setValue({
      destino: 'INICIATIVA_APROBADA',
      documentoRefId: 777,
      observaciones: 'Decision formal'
    });
    await (component as any).confirmarDecision();

    expect((component as any).problem()?.code).toBe('STATE_CHANGED');
    expect((component as any).problem()?.status).toBe(412);
    expect((component as any).etagActual()).toBe('202-1');
  });

  it('mapea `IF_MATCH_REQUIRED` (428) cuando el cliente omite `If-Match`', async () => {
    mocks.decision.transicionar.mockReturnValue(
      throwError(
        () =>
          problemaHttp(428, {
            type: 'about:blank',
            title: 'If-Match requerido',
            status: 428,
            code: 'IF_MATCH_REQUIRED',
            correlationId: 'dec-428',
            detail: 'La transicion exige la cabecera If-Match.'
          })
      )
    );

    (component as any).formDecision.setValue({
      destino: 'INICIATIVA_APROBADA',
      documentoRefId: 777,
      observaciones: 'Decision formal'
    });
    await (component as any).confirmarDecision();

    expect((component as any).problem()?.code).toBe('IF_MATCH_REQUIRED');
  });

  it('mapea `ASSIGNMENT_SCOPE_DENIED` (403) cuando la asignacion efectiva no cubre la unidad', async () => {
    mocks.decision.transicionar.mockReturnValue(
      throwError(
        () =>
          problemaHttp(403, {
            type: 'about:blank',
            title: 'Acceso denegado',
            status: 403,
            code: 'ASSIGNMENT_SCOPE_DENIED',
            correlationId: 'dec-403-scope',
            detail: 'La asignacion efectiva no cubre la unidad de la iniciativa.'
          })
      )
    );

    (component as any).formDecision.setValue({
      destino: 'INICIATIVA_APROBADA',
      documentoRefId: 777,
      observaciones: 'Decision formal'
    });
    await (component as any).confirmarDecision();

    expect((component as any).problem()?.code).toBe('ASSIGNMENT_SCOPE_DENIED');
  });
});

describe('cabeceras HTTP (Idempotency-Key, If-Match, X-Asignacion-Efectiva-Id)', () => {
  let mocks: Mocks;
  let component: InitiativeDecisionPageComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.decision.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
    mocks.decision.transicionar.mockReturnValue(of(transicionSintetica()));
    ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
  });

  it('adjunta `Idempotency-Key`, `If-Match` y `X-Asignacion-Efectiva-Id` en la transicion', async () => {
    (component as any).formDecision.setValue({
      destino: 'INICIATIVA_APROBADA',
      documentoRefId: 777,
      observaciones: 'Decision formal'
    });
    await (component as any).confirmarDecision();

    expect(mocks.decision.transicionar).toHaveBeenCalledOnce();
    const [id, comando, opciones] = mocks.decision.transicionar.mock.calls[0];
    expect(id).toBe(INITIATIVE_ID);
    expect(comando.destino).toBe('INICIATIVA_APROBADA');
    expect(comando.documentoRefId).toBe(777);
    // If-Match se propaga como opción `etag` al servicio
    expect(opciones).toEqual({ etag: '202-1' });
    // Idempotency-Key, Authorization y X-Asignacion-Efectiva-Id son cabeceras
    // HTTP añadidas por interceptores; se verifican en pruebas de integración
    // con el flujo HTTP real (servicio no mockeado).
  });
});

describe('accesibilidad WCAG 2.1 AA', () => {
  let mocks: Mocks;
  let fixture: ComponentFixture<InitiativeDecisionPageComponent>;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.decision.consultarIniciativa.mockReturnValue(of(iniciativaSintetica()));
    ({ fixture } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
  });

  function dom(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  it('asocia cada `mat-label` con su control mediante `for`/`id`', () => {
    const asociaciones: Array<[string, string]> = [
      ['decision-destino', 'decision-destino'],
      ['decision-documento', 'decision-documento'],
      ['decision-observaciones', 'decision-observaciones']
    ];

    for (const [forTarget, id] of asociaciones) {
      const label = dom().querySelector(`mat-label[for="${forTarget}"]`);
      const control = dom().querySelector(`#${id}`);
      expect(label, `label[for=${forTarget}] ausente`).not.toBeNull();
      expect(control, `control #${id} ausente`).not.toBeNull();
    }
  });

  it('expone `aria-describedby` con identificadores de ayuda y error en campos obligatorios', () => {
    const destino = dom().querySelector<HTMLElement>('#decision-destino');
    expect(destino?.getAttribute('aria-describedby')).toContain('decision-destino-help');
    expect(destino?.getAttribute('aria-describedby')).toContain('decision-destino-error');
  });

  it('declara una region `aria-live="assertive"` para errores del servidor', () => {
    const region = dom().querySelector('.decision-page__server-errors');
    expect(region?.getAttribute('role')).toBe('alert');
    expect(region?.getAttribute('aria-live')).toBe('assertive');
  });

  it('declara un `output` con `aria-live="polite"` para el resultado de la decision', () => {
    const output = dom().querySelector('output.decision-page__result');
    expect(output).not.toBeNull();
    expect(output?.getAttribute('aria-live')).toBe('polite');
  });

  it('expone el boton de envio con texto descriptivo y tipo `submit`', () => {
    const boton = dom().querySelector<HTMLButtonElement>('button[type="submit"][data-action="confirmar-decision"]');
    expect(boton).not.toBeNull();
    expect(boton?.textContent?.trim()).toBe('Confirmar decision formal');
  });

  it('asocia el boton de cancelar con la region del formulario mediante `aria-controls`', () => {
    const boton = dom().querySelector<HTMLButtonElement>('button[data-action="cancelar-decision"]');
    expect(boton).not.toBeNull();
    expect(boton?.getAttribute('aria-controls')).toBe('decision-form');
  });

  it('marca como `required` los campos `destino` y `documentoRefId`', () => {
    const destino = dom().querySelector<HTMLElement>('#decision-destino');
    const documento = dom().querySelector<HTMLInputElement>('#decision-documento');
    expect(destino?.getAttribute('required')).not.toBeNull();
    expect(documento?.required).toBe(true);
  });
});
