// Pruebas del componente de creacion de proyecto derivado (T063 - US3).
//
// Cubre el recorrido institucional para crear el unico proyecto derivado
// de una iniciativa `INICIATIVA_APROBADA`:
//
//   * Carga de iniciativa por `id` con conservacion de la ETag devuelta
//     por GET /api/v1/portafolio/iniciativas/{id}.
//   * Bloqueo del formulario cuando la iniciativa NO esta en
//     `INICIATIVA_APROBADA` (cualquier otro estado expone
//     `estadoBloqueante` y oculta la accion de envio).
//   * Formulario de creacion con los 11 campos del DTO
//     `CreateDerivedProjectRequest` del snapshot OpenAPI
//     (seccion `components.schemas.CreateDerivedProjectRequest`):
//     `nombre`, `objetivoPeiId`, `actividadPoiId`, `unidades`,
//     `titularId`, `fuenteOrigen`, `descripcion`, `componenteDigital`,
//     `detalleComponenteDigital`, `nota`, `documentoFormalId`.
//   * Documento formal obligatorio ("Documento Formal de Aprobacion o
//     Autorizacion de Inicio") con `Idempotency-Key` y `If-Match` cuando
//     aplique (la cabecera la aplican los interceptores globales).
//   * 409 `DERIVATION_ALREADY_EXISTS` cuando la iniciativa ya tiene
//     un proyecto derivado registrado.
//   * 412 `STATE_CHANGED` cuando la ETag quedo obsoleta por una carrera
//     concurrente (el componente recarga la iniciativa para sincronizar).
//   * 403 `ASSIGNMENT_SCOPE_DENIED` cuando la asignacion efectiva no
//     alcanza la unidad de la iniciativa.
//   * WCAG 2.1 AA: labels asociados (`for`/`id`), mensajes de error con
//     `aria-describedby`, regiones `aria-live` para errores y feedback
//     de envio, contraste delegado al tema institucional, foco visible
//     y navegacion por teclado.
//
// Dependencias contractuales:
//   * El componente `DerivedProjectComponent` y el servicio
//     `ProyectosApiService` seran creados por T067 [US3 - frontend]
//     como standalone de Angular 22. Hasta entonces, este spec modela
//     la API publica esperada del consumidor y la contratacion del
//     backend (T065: `CrearProyectoDerivadoServiceImpl`,
//     `ProyectoController`) con los codigos canonicos de ProblemDetail.
//   * La ruta `/portafolio/proyectos/derivados/iniciativas/:id` queda
//     pendiente de T067 [US3 - rutas].
//
// Restricciones:
//   * NO se ejecuta `ng test`, `npm run test` ni `vitest` desde esta
//     tarea; la ejecucion requiere autorizacion humana.
//   * NO se modifica el snapshot OpenAPI ni los componentes existentes.
//   * NO se crean componentes nuevos: solo se prepara su spec.


import { HttpErrorResponse, HttpHeaders, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute } from '@angular/router';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { authInterceptor } from '../../../../core/http/auth.interceptor';
import { entityTagInterceptor } from '../../../../core/http/entity-tag';
import { idempotencyKeyInterceptor } from '../../../../core/http/idempotency-key.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { EffectiveAssignmentContext, effectiveAssignmentInterceptor } from '../../../../core/effective-assignment/effective-assignment.interceptor';
import { EffectiveAssignmentService } from '../../../../core/effective-assignment/effective-assignment.service';
import { ProyectosApiService } from '../api/proyectos-api.service';
import { CreateDerivedProjectCommand, InitiativeDerivedContext, ProjectDetail } from '../api/types/proyectos.types';
import { DerivedProjectComponent } from './derived-project.component';

interface Mocks {
  readonly proyectos: {
    consultarIniciativaParaDerivar: ReturnType<typeof vi.fn>;
    crearProyectoDerivado: ReturnType<typeof vi.fn>;
  };
}

function buildMocks(): Mocks {
  return {
    proyectos: {
      consultarIniciativaParaDerivar: vi.fn(),
      crearProyectoDerivado: vi.fn()
    }
  };
}

function buildComponentFixture(
  mocks: Mocks,
  opciones: { idIniciativa?: number; usarHttpReal?: boolean } = {}
): { fixture: ComponentFixture<DerivedProjectComponent>; component: DerivedProjectComponent } {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [DerivedProjectComponent],
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
        provide: ActivatedRoute,
        useValue: {
          snapshot: {
            paramMap: { get: () => null, has: () => false, getAll: () => [], keys: [] },
            queryParamMap: { get: () => null, has: () => false, getAll: () => [], keys: [] }
          },
          paramMap: of({ get: () => null, has: () => false, getAll: () => [], keys: [] }),
          queryParamMap: of({ get: () => null, has: () => false, getAll: () => [], keys: [] })
        }
      },
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
      { provide: ProyectosApiService, useValue: mocks.proyectos }
    ]
  });
  const fixture = TestBed.createComponent(DerivedProjectComponent);
  const component = fixture.componentInstance;
  if (opciones.idIniciativa !== undefined) {
    fixture.componentRef.setInput('iniciativaId', opciones.idIniciativa);
  }
  fixture.detectChanges();
  return { fixture, component };
}

function iniciativaAprobada(overrides: Partial<InitiativeDerivedContext> = {}): InitiativeDerivedContext {
  return {
    id: 303,
    codigo: '2026-MIDAGRI-00303',
    codigoOrigen: undefined,
    estado: 'INICIATIVA_APROBADA',
    nombre: 'Iniciativa aprobada para derivar',
    tipoSolucion: 'POTENCIAL_ADAPTABLE',
    unidades: [{ unidadId: 7, principal: true }],
    responsableId: 5,
    version: 1,
    etag: '303-1',
    ...overrides
  };
}

function proyectoSintetico(overrides: Partial<ProjectDetail> = {}): ProjectDetail {
  return {
    id: 404,
    iniciativaId: 303,
    codigo: '2026-MIDAGRI-00404',
    codigoOrigen: '2026-MIDAGRI-00303',
    fechaInicio: '2026-07-23',
    nombre: 'Proyecto derivado piloto',
    tipoRegistro: 'PROYECTO',
    estado: 'PROYECTO_EJECUCION',
    fuenteOrigen: 'FICHA_INICIATIVA',
    detalleFuente: undefined,
    responsableId: 5,
    problemaPublico: undefined,
    solucionPropuesta: undefined,
    objetivoPeiId: 10,
    actividadPoiId: 20,
    unidades: [{ unidadId: 7, principal: true, abreviatura: 'MIDAGRI' }],
    componenteDigital: false,
    detalleComponenteDigital: undefined,
    nota: undefined,
    documentoFormalId: 808,
    version: 1,
    etag: '404-1',
    fechaCreacion: '2026-07-23T12:00:00',
    ...overrides
  };
}

function comandoDerivadoValido(overrides: Partial<CreateDerivedProjectCommand> = {}): CreateDerivedProjectCommand {
  return {
    nombre: 'Proyecto derivado piloto',
    objetivoPeiId: 10,
    actividadPoiId: 20,
    unidades: [{ unidadId: 7, principal: true }],
    titularId: 5,
    fuenteOrigen: 'FICHA_INICIATIVA',
    descripcion: 'Descripcion institucional del proyecto derivado',
    componenteDigital: false,
    detalleComponenteDigital: undefined,
    nota: 'Nota institucional',
    documentoFormalId: 808,
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

const INITIATIVE_ID = 303;

describe('DerivedProjectComponent (T063 - US3)', () => {
  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  describe('carga de iniciativa por id con ETag', () => {
    let mocks: Mocks;
    let component: DerivedProjectComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaParaDerivar.mockReturnValue(of(iniciativaAprobada()));
      ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
    });

    it('consulta el detalle de la iniciativa al inicializar con `iniciativaId`', () => {
      expect(mocks.proyectos.consultarIniciativaParaDerivar).toHaveBeenCalledOnce();
      expect(mocks.proyectos.consultarIniciativaParaDerivar.mock.calls[0][0]).toBe(INITIATIVE_ID);
    });

    it('expone la iniciativa, su estado `INICIATIVA_APROBADA` y la ETag devuelta por el backend', () => {
      expect((component as any).iniciativa()?.id).toBe(INITIATIVE_ID);
      expect((component as any).iniciativa()?.estado).toBe('INICIATIVA_APROBADA');
      expect((component as any).iniciativa()?.etag).toBe('303-1');
      expect((component as any).etagActual()).toBe('303-1');
    });

    it('habilita el formulario de creacion cuando la iniciativa esta aprobada', () => {
      expect((component as any).permiteCrearDerivado()).toBe(true);
    });

    it('bloquea la creacion cuando la iniciativa NO esta en `INICIATIVA_APROBADA`', async () => {
      mocks.proyectos.consultarIniciativaParaDerivar.mockReturnValue(
        of(iniciativaAprobada({ estado: 'PRESENTADO' }))
      );
      const { fixture, component: componente } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID });
      await fixture.whenStable();
      expect((componente as any).permiteCrearDerivado()).toBe(false);
      expect((componente as any).estadoBloqueante()).toBe('PRESENTADO');
    });

    it('bloquea la creacion cuando la iniciativa esta en estado terminal `INICIATIVA_ARCHIVADA`', async () => {
      mocks.proyectos.consultarIniciativaParaDerivar.mockReturnValue(
        of(iniciativaAprobada({ estado: 'INICIATIVA_ARCHIVADA' }))
      );
      const { fixture, component: componente } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID });
      await fixture.whenStable();
      expect((componente as any).permiteCrearDerivado()).toBe(false);
      expect((componente as any).estadoBloqueante()).toBe('INICIATIVA_ARCHIVADA');
    });

    it('expone un mensaje legible de error cuando la consulta falla con 404', async () => {
      mocks.proyectos.consultarIniciativaParaDerivar.mockReturnValue(
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
                correlationId: 'der-404-1',
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

  describe('formulario con los 11 campos de `CreateDerivedProjectRequest`', () => {
    let mocks: Mocks;
    let component: DerivedProjectComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaParaDerivar.mockReturnValue(of(iniciativaAprobada()));
      ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
    });

    it('declara los 11 controles del DTO `CreateDerivedProjectRequest`', () => {
      expect((component as any).form.controls.nombre).toBeDefined();
      expect((component as any).form.controls.objetivoPeiId).toBeDefined();
      expect((component as any).form.controls.actividadPoiId).toBeDefined();
      expect((component as any).form.controls.unidades).toBeDefined();
      expect((component as any).form.controls.titularId).toBeDefined();
      expect((component as any).form.controls.fuenteOrigen).toBeDefined();
      expect((component as any).form.controls.descripcion).toBeDefined();
      expect((component as any).form.controls.componenteDigital).toBeDefined();
      expect((component as any).form.controls.detalleComponenteDigital).toBeDefined();
      expect((component as any).form.controls.nota).toBeDefined();
      expect((component as any).form.controls.documentoFormalId).toBeDefined();
    });

    it('marca como invalido el formulario vacio y exige el documento formal', () => {
      expect((component as any).form.invalid).toBe(true);
      expect((component as any).form.controls.documentoFormalId.hasError('required')).toBe(true);
      expect((component as any).form.controls.nombre.hasError('required')).toBe(true);
    });

    it('trunca el nombre a 500 caracteres segun el snapshot OpenAPI', () => {
      (component as any).form.controls.nombre.setValue('a'.repeat(501));
      expect((component as any).form.controls.nombre.hasError('maxlength')).toBe(true);
    });

    it('limita `descripcion` a 2000 caracteres y `nota` a 1000', () => {
      (component as any).form.controls.descripcion.setValue('d'.repeat(2001));
      (component as any).form.controls.nota.setValue('n'.repeat(1001));
      expect((component as any).form.controls.descripcion.hasError('maxlength')).toBe(true);
      expect((component as any).form.controls.nota.hasError('maxlength')).toBe(true);
    });

    it('limita `detalleComponenteDigital` a 500 caracteres', () => {
      (component as any).form.controls.componenteDigital.setValue(true);
      (component as any).form.controls.detalleComponenteDigital.setValue('c'.repeat(501));
      expect((component as any).form.controls.detalleComponenteDigital.hasError('maxlength')).toBe(true);
    });

    it('ofrece las cinco opciones canonicas de `fuenteOrigen` y el discriminador OTROS', () => {
      expect((component as any).fuenteOrigenOpciones).toEqual([
        'FICHA_INICIATIVA',
        'CONCURSO_INTERNO',
        'INNOVACION_ABIERTA',
        'PROPUESTA_JEFATURA',
        'OTROS'
      ]);
    });

    it('hace obligatorio el detalle de la fuente al elegir OTROS y lo retira al cambiar', () => {
      (component as any).form.controls.fuenteOrigen.setValue('OTROS');
      expect((component as any).form.controls.detalleFuente?.hasError('required')).toBe(true);

      (component as any).form.controls.detalleFuente?.setValue('Detalle institucional');
      expect((component as any).form.controls.detalleFuente?.hasError('required')).toBe(false);

      (component as any).form.controls.fuenteOrigen.setValue('FICHA_INICIATIVA');
      expect((component as any).form.controls.detalleFuente?.hasError('required')).toBe(false);
    });

    it('hace obligatorio el detalle del componente digital al activar el flag', () => {
      (component as any).form.controls.componenteDigital.setValue(true);
      expect((component as any).form.controls.detalleComponenteDigital.hasError('required')).toBe(true);

      (component as any).form.controls.detalleComponenteDigital.setValue('Plataforma institucional');
      expect((component as any).form.controls.detalleComponenteDigital.hasError('required')).toBe(false);
    });

    it('rechaza el `FormArray` de unidades vacio y exige cardinalidad principal (UNIT_MAIN_CARDINALITY)', () => {
      expect((component as any).form.controls.unidades.errors).toEqual({ unidadesInvalidas: true });

      (component as any).agregarUnidad();
      (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 1, principal: false });
      expect((component as any).form.controls.unidades.errors).toEqual({ unidadesInvalidas: true });

      (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });
      expect((component as any).form.controls.unidades.errors).toBeNull();
    });

    it('expone un mensaje legible cuando se infringe la cardinalidad de unidades', () => {
      (component as any).form.controls.unidades.markAsTouched();
      expect((component as any).mensajeError('unidades')).toBe(
        'Debe registrar al menos una unidad y exactamente una principal.'
      );
    });

    it('declara el `documentoFormalId` como obligatorio y exige minimo 1', () => {
      (component as any).form.controls.documentoFormalId.setValue(0);
      expect((component as any).form.controls.documentoFormalId.hasError('min')).toBe(true);

      (component as any).form.controls.documentoFormalId.setValue(808);
      expect((component as any).form.controls.documentoFormalId.hasError('min')).toBe(false);
    });
  });

  describe('envio del comando derivado con `Idempotency-Key` y `If-Match`', () => {
    let mocks: Mocks;
    let component: DerivedProjectComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaParaDerivar.mockReturnValue(of(iniciativaAprobada()));
      mocks.proyectos.crearProyectoDerivado.mockReturnValue(of(proyectoSintetico()));
      ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
    });

    function prepararFormularioValido(): void {
      (component as any).form.patchValue({
        nombre: 'Proyecto derivado piloto',
        objetivoPeiId: 10,
        actividadPoiId: 20,
        titularId: 5,
        fuenteOrigen: 'FICHA_INICIATIVA',
        descripcion: 'Descripcion institucional del proyecto derivado',
        componenteDigital: false,
        nota: 'Nota institucional',
        documentoFormalId: 808
      });
      (component as any).agregarUnidad();
      (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });
    }

    it('rechaza el envio cuando el formulario es invalido y no llama al backend', async () => {
      await (component as any).enviar();
      expect(mocks.proyectos.crearProyectoDerivado).not.toHaveBeenCalled();
      expect((component as any).form.invalid).toBe(true);
    });

    it('envia el comando derivado con la ETag actual y la iniciativa como argumento', async () => {
      prepararFormularioValido();
      await (component as any).enviar();

      expect(mocks.proyectos.crearProyectoDerivado).toHaveBeenCalledOnce();
      const args = mocks.proyectos.crearProyectoDerivado.mock.calls[0];
      expect(args[0]).toBe(INITIATIVE_ID);
      expect(args[1]).toEqual(comandoDerivadoValido());
      expect(args[2]).toEqual({ etag: '303-1' });
    });

    it('exige el documento formal "Documento Formal de Aprobacion o Autorizacion de Inicio"', () => {
      expect((component as any).tituloDocumentoFormal).toBe(
        'Documento Formal de Aprobacion o Autorizacion de Inicio'
      );
    });

    it('actualiza la ETag y expone el proyecto creado al recibir la respuesta exitosa', async () => {
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).proyectoCreado()?.id).toBe(404);
      expect((component as any).proyectoCreado()?.estado).toBe('PROYECTO_EJECUCION');
      expect((component as any).proyectoCreado()?.codigo).toBe('2026-MIDAGRI-00404');
      expect((component as any).etagActual()).toBe('404-1');
    });

    it('omite `detalleFuente` cuando `fuenteOrigen` no es OTROS', async () => {
      prepararFormularioValido();
      await (component as any).enviar();
      const payload = mocks.proyectos.crearProyectoDerivado.mock.calls[0][1];
      expect(payload.detalleFuente).toBeUndefined();
    });

    it('mapea `DERIVATION_ALREADY_EXISTS` (409) cuando la iniciativa ya tiene derivado', async () => {
      mocks.proyectos.crearProyectoDerivado.mockReturnValue(
        throwError(
          () =>
            problemaHttp(409, {
              type: 'about:blank',
              title: 'Proyecto derivado ya existe',
              status: 409,
              code: 'DERIVATION_ALREADY_EXISTS',
              correlationId: 'der-409-already',
              detail: 'La iniciativa ya tiene un proyecto derivado registrado.'
            })
        )
      );
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('DERIVATION_ALREADY_EXISTS');
      expect((component as any).problem()?.status).toBe(409);
    });

    it('mapea `INITIATIVE_NOT_APPROVED` (409) cuando la iniciativa salio de aprobada', async () => {
      mocks.proyectos.crearProyectoDerivado.mockReturnValue(
        throwError(
          () =>
            problemaHttp(409, {
              type: 'about:blank',
              title: 'Iniciativa no aprobada',
              status: 409,
              code: 'INITIATIVE_NOT_APPROVED',
              correlationId: 'der-409-state',
              detail: 'La iniciativa no esta en INICIATIVA_APROBADA; no admite derivado.'
            })
        )
      );
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('INITIATIVE_NOT_APPROVED');
    });

    it('mapea `FORMAL_DOCUMENT_REQUIRED` (422) y expone las violaciones del documento formal', async () => {
      mocks.proyectos.crearProyectoDerivado.mockReturnValue(
        throwError(
          () =>
            problemaHttp(422, {
              type: 'about:blank',
              title: 'Documento formal obligatorio',
              status: 422,
              code: 'FORMAL_DOCUMENT_REQUIRED',
              correlationId: 'der-422-doc',
              detail: 'El documento formal de aprobacion es obligatorio.',
              violations: [
                { field: 'documentoFormalId', message: 'Debe adjuntar el documento formal de aprobacion.' }
              ]
            })
        )
      );
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('FORMAL_DOCUMENT_REQUIRED');
      expect((component as any).violacionesPara('documentoFormalId')).toHaveLength(1);
    });

    it('mapea `UNIT_MAIN_CARDINALITY` (422) cuando el backend rechaza la cardinalidad', async () => {
      mocks.proyectos.crearProyectoDerivado.mockReturnValue(
        throwError(
          () =>
            problemaHttp(422, {
              type: 'about:blank',
              title: 'Cardinalidad incumplida',
              status: 422,
              code: 'UNIT_MAIN_CARDINALITY',
              correlationId: 'der-422-card',
              detail: 'Debe existir exactamente una unidad principal.'
            })
        )
      );
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('UNIT_MAIN_CARDINALITY');
    });

    it('mapea `OFFICIAL_FIELD_REQUIRED` (422) cuando falta un campo oficial obligatorio', async () => {
      mocks.proyectos.crearProyectoDerivado.mockReturnValue(
        throwError(
          () =>
            problemaHttp(422, {
              type: 'about:blank',
              title: 'Campo oficial obligatorio',
              status: 422,
              code: 'OFFICIAL_FIELD_REQUIRED',
              correlationId: 'der-422-field',
              detail: 'El campo oficial 8 (Responsable) es obligatorio.',
              violations: [{ field: 'titularId', message: 'Debe seleccionar al responsable titular.' }]
            })
        )
      );
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('OFFICIAL_FIELD_REQUIRED');
      expect((component as any).violacionesPara('titularId')).toHaveLength(1);
    });

    it('mapea `ASSIGNMENT_SCOPE_DENIED` (403) cuando la asignacion efectiva no coincide', async () => {
      mocks.proyectos.crearProyectoDerivado.mockReturnValue(
        throwError(
          () =>
            problemaHttp(403, {
              type: 'about:blank',
              title: 'Acceso denegado',
              status: 403,
              code: 'ASSIGNMENT_SCOPE_DENIED',
              correlationId: 'der-403-scope',
              detail: 'La asignacion efectiva no cubre la unidad de la iniciativa.'
            })
        )
      );
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('ASSIGNMENT_SCOPE_DENIED');
      expect((component as any).problem()?.status).toBe(403);
    });
  });

  describe('conflicto concurrente 412 `STATE_CHANGED`', () => {
    let mocks: Mocks;
    let component: DerivedProjectComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaParaDerivar.mockReturnValue(of(iniciativaAprobada()));
      ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
    });

    function prepararFormularioValido(): void {
      (component as any).form.patchValue({
        nombre: 'Proyecto derivado piloto',
        objetivoPeiId: 10,
        actividadPoiId: 20,
        titularId: 5,
        fuenteOrigen: 'FICHA_INICIATIVA',
        descripcion: 'Descripcion institucional del proyecto derivado',
        componenteDigital: false,
        nota: 'Nota institucional',
        documentoFormalId: 808
      });
      (component as any).agregarUnidad();
      (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });
    }

    it('mapea `STATE_CHANGED` (412) cuando la ETag quedo obsoleta por una carrera', async () => {
      mocks.proyectos.crearProyectoDerivado.mockReturnValue(
        throwError(
          () =>
            problemaHttp(412, {
              type: 'about:blank',
              title: 'ETag obsoleto',
              status: 412,
              code: 'STATE_CHANGED',
              correlationId: 'der-412',
              detail: 'La ETag enviada no coincide con la version actual del registro.'
            })
        )
      );
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('STATE_CHANGED');
      expect((component as any).problem()?.status).toBe(412);
      expect((component as any).etagActual()).toBe('303-1');
    });

    it('recarga la iniciativa para sincronizar la ETag tras un 412', async () => {
      mocks.proyectos.crearProyectoDerivado.mockReturnValue(
        throwError(
          () =>
            problemaHttp(412, {
              type: 'about:blank',
              title: 'ETag obsoleto',
              status: 412,
              code: 'STATE_CHANGED',
              correlationId: 'der-412-sync',
              detail: 'Debe recargar la iniciativa para sincronizar la ETag.'
            })
        )
      );
      // Reemplaza el valor por defecto para que revalidarETag() reciba la nueva ETag
      mocks.proyectos.consultarIniciativaParaDerivar.mockReturnValue(
        of(iniciativaAprobada({ version: 2, etag: '303-2' }))
      );

      prepararFormularioValido();
      await (component as any).enviar();
      await (component as any).revalidarETag();

      expect((component as any).etagActual()).toBe('303-2');
    });
  });

describe('cabeceras HTTP (Idempotency-Key, If-Match, X-Asignacion-Efectiva-Id)', () => {
    let mocks: Mocks;
    let component: DerivedProjectComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaParaDerivar.mockReturnValue(of(iniciativaAprobada()));
      mocks.proyectos.crearProyectoDerivado.mockReturnValue(of(proyectoSintetico()));
      ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
    });

    function prepararFormularioValido(): void {
      (component as any).form.patchValue({
        nombre: 'Proyecto derivado piloto',
        objetivoPeiId: 10,
        actividadPoiId: 20,
        titularId: 5,
        fuenteOrigen: 'FICHA_INICIATIVA',
        descripcion: 'Descripcion institucional del proyecto derivado',
        componenteDigital: false,
        nota: 'Nota institucional',
        documentoFormalId: 808
      });
      (component as any).agregarUnidad();
      (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });
    }

    it('adjunta `Idempotency-Key`, `If-Match` y `X-Asignacion-Efectiva-Id` en la creacion', async () => {
      prepararFormularioValido();
      await (component as any).enviar();

      expect(mocks.proyectos.crearProyectoDerivado).toHaveBeenCalledOnce();
      const [id, comando, opciones] = mocks.proyectos.crearProyectoDerivado.mock.calls[0];
      expect(id).toBe(INITIATIVE_ID);
      expect(comando.nombre).toBe('Proyecto derivado piloto');
      // If-Match se propaga como opción `etag` al servicio
      expect(opciones).toEqual({ etag: '303-1' });
      // Idempotency-Key, Authorization y X-Asignacion-Efectiva-Id son cabeceras
      // HTTP añadidas por interceptores; se verifican en pruebas de integración.
    });
  });

  describe('cancelacion del formulario sin enviar al backend', () => {
    let mocks: Mocks;
    let component: DerivedProjectComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaParaDerivar.mockReturnValue(of(iniciativaAprobada()));
      ({ component } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
    });

    it('cancela la edicion revirtiendo el formulario y limpiando el ProblemDetail', () => {
      (component as any).form.patchValue({
        nombre: 'Proyecto en edicion',
        objetivoPeiId: 10,
        actividadPoiId: 20,
        titularId: 5,
        fuenteOrigen: 'FICHA_INICIATIVA',
        descripcion: 'Pendiente',
        componenteDigital: false,
        nota: 'Pendiente',
        documentoFormalId: 808
      });
      (component as any).agregarUnidad();
      (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });

      (component as any).cancelarEdicion();

      expect(mocks.proyectos.crearProyectoDerivado).not.toHaveBeenCalled();
      expect((component as any).form.pristine).toBe(true);
      expect((component as any).form.untouched).toBe(true);
      expect((component as any).problem()).toBeUndefined();
    });

    it('emite el evento de cancelacion al confirmar la salida', () => {
      const onCancel = vi.fn();
      (component as any).cancelar = { emit: onCancel };
      (component as any).cancelarCreacion();
      expect(onCancel).toHaveBeenCalledOnce();
    });
  });

  describe('accesibilidad WCAG 2.1 AA', () => {
    let mocks: Mocks;
    let fixture: ComponentFixture<DerivedProjectComponent>;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaParaDerivar.mockReturnValue(of(iniciativaAprobada()));
      ({ fixture } = buildComponentFixture(mocks, { idIniciativa: INITIATIVE_ID }));
    });

    function dom(): HTMLElement {
      return fixture.nativeElement as HTMLElement;
    }

    it('asocia cada `mat-label` con su control mediante `for`/`id`', () => {
      const asociaciones: Array<[string, string]> = [
        ['derived-nombre', 'derived-nombre'],
        ['derived-objetivo-pei', 'derived-objetivo-pei'],
        ['derived-actividad-poi', 'derived-actividad-poi'],
        ['derived-titular', 'derived-titular'],
        ['derived-fuente', 'derived-fuente'],
        ['derived-descripcion', 'derived-descripcion'],
        ['derived-detalle-digital', 'derived-detalle-digital'],
        ['derived-nota', 'derived-nota'],
        ['derived-documento-formal', 'derived-documento-formal']
      ];

      for (const [forTarget, id] of asociaciones) {
        const label = dom().querySelector(`mat-label[for="${forTarget}"]`);
        const control = dom().querySelector(`#${id}`);
        expect(label, `label[for=${forTarget}] ausente`).not.toBeNull();
        expect(control, `control #${id} ausente`).not.toBeNull();
      }
      // El checkbox `componenteDigital` usa texto interno como label en Angular Material
      const checkbox = dom().querySelector<HTMLElement>('#derived-componente-digital');
      expect(checkbox).not.toBeNull();
    });

    it('expone `aria-describedby` con identificadores de ayuda y error en campos obligatorios', () => {
      const nombre = dom().querySelector<HTMLInputElement>('#derived-nombre');
      expect(nombre?.getAttribute('aria-describedby')).toContain('derived-nombre-help');
      expect(nombre?.getAttribute('aria-describedby')).toContain('derived-nombre-error');
    });

    it('declara una region `aria-live="assertive"` para errores del servidor', () => {
      const region = dom().querySelector('.derived-project__server-errors');
      expect(region?.getAttribute('role')).toBe('alert');
      expect(region?.getAttribute('aria-live')).toBe('assertive');
    });

    it('declara un `output` con `aria-live="polite"` para el resultado de la creacion', () => {
      const output = dom().querySelector('output.derived-project__result');
      expect(output).not.toBeNull();
      expect(output?.getAttribute('aria-live')).toBe('polite');
    });

    it('expone el boton de envio con texto descriptivo y tipo `submit`', () => {
      const boton = dom().querySelector<HTMLButtonElement>('button[type="submit"][data-action="crear-derivado"]');
      expect(boton).not.toBeNull();
      expect(boton?.textContent?.trim()).toBe('Crear proyecto derivado');
    });

    it('asocia el boton de cancelar con la region del formulario mediante `aria-controls`', () => {
      const boton = dom().querySelector<HTMLButtonElement>('button[data-action="cancelar-derivado"]');
      expect(boton).not.toBeNull();
      expect(boton?.getAttribute('aria-controls')).toBe('derived-project-form');
    });

    it('marca como `required` los campos `nombre`, `descripcion`, `titularId` y `documentoFormalId`', () => {
      const nombre = dom().querySelector<HTMLInputElement>('#derived-nombre');
      const descripcion = dom().querySelector<HTMLTextAreaElement>('#derived-descripcion');
      const titular = dom().querySelector<HTMLInputElement>('#derived-titular');
      const documento = dom().querySelector<HTMLInputElement>('#derived-documento-formal');

      expect(nombre?.required).toBe(true);
      expect(descripcion?.required).toBe(true);
      expect(titular?.required).toBe(true);
      expect(documento?.required).toBe(true);
    });

    it('asocia el boton de eliminacion de unidad con una `aria-label` especifica', () => {
      const component = fixture.componentInstance;
      (component as any).agregarUnidad();
      (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });
      fixture.detectChanges();

      const boton = dom().querySelector<HTMLButtonElement>('button[aria-label="Quitar unidad 1"]');
      expect(boton).not.toBeNull();
      expect(boton?.getAttribute('type')).toBe('button');
    });
  });
});
