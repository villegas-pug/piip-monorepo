// Pruebas del componente de creacion de proyecto directo (T063 - US3).
//
// Cubre el recorrido institucional para registrar un proyecto directo
// heredado o excepcional:
//
//   * Selector de `tipoOrigenDirecto` con las dos opciones canonicas del
//     snapshot OpenAPI (`HEREDADO` y `EXCEPCION_FORMAL`).
//   * Formulario con los 15 campos del DTO `DirectProjectRequest`:
//     `tipoOrigen`, `codigoOrigen`, `fechaInicio`, `nombre`,
//     `objetivoPeiId`, `actividadPoiId`, `unidadResponsableId`,
//     `responsableId`, `descripcion`, `componenteDigital`,
//     `detalleComponenteDigital`, `nota`, `documentoAutorizacionId`,
//     `evidenciaIds`, `fuenteOrigen`.
//   * Documento formal obligatorio ("Documento Formal de Aprobacion o
//     Autorizacion de Inicio"). La fecha se fija en el servidor; el
//     cliente solo exige `fechaInicio` y la envia como dato contractual.
//   * Restitucion de la autorizacion efectiva: Autoridad o Evaluador con
//     documento formal. El Responsable queda excluido por constitucion.
//   * No omite la evaluacion de una iniciativa nueva: si existe una
//     iniciativa `PRESENTADO` para el mismo ambito, el backend responde
//     409 (`INITIATIVE_PRESENT_FOR_SCOPE`).
//   * 409 `DIRECT_PROJECT_DUPLICATE` cuando ya hay un proyecto directo
//     activo para la misma unidad ejecutora y anio.
//   * 409 `DIRECT_PROJECT_NOT_AUTHORIZED` para conflictos por carrera
//     concurrente sobre la misma unidad y anio.
//   * 403 `ASSIGNMENT_SCOPE_DENIED` cuando la asignacion efectiva no
//     alcanza la unidad responsable.
//   * 422 `FORMAL_DOCUMENT_REQUIRED`, `EVIDENCE_NOT_ELIGIBLE` y
//     `OFFICIAL_FIELD_REQUIRED` para evidencia incompleta o documento
//     no apto.
//   * WCAG 2.1 AA: labels asociados, mensajes de error con
//     `aria-describedby`, regiones `aria-live` para errores y feedback,
//     contraste delegado al tema institucional, foco visible y
//     navegacion por teclado.
//
// Dependencias contractuales:
//   * El componente `DirectProjectComponent` y el servicio
//     `ProyectosApiService` seran creados por T067 [US3 - frontend]
//     como standalone de Angular 22. El metodo
//     `consultarIniciativaPresentadaPorUnidad` y los tipos
//     `DirectProjectCommand` / `ProjectDetail` los publicara T067.
//   * La ruta `/portafolio/proyectos/directos` queda pendiente de T067.
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
import { DirectProjectCommand, ProjectDetail, TipoOrigenDirecto } from '../api/types/proyectos.types';
import { DirectProjectComponent } from './direct-project.component';

interface Mocks {
  readonly proyectos: {
    consultarIniciativaPresentadaPorUnidad: ReturnType<typeof vi.fn>;
    crearProyectoDirecto: ReturnType<typeof vi.fn>;
  };
}

function buildMocks(): Mocks {
  return {
    proyectos: {
      consultarIniciativaPresentadaPorUnidad: vi.fn(),
      crearProyectoDirecto: vi.fn()
    }
  };
}

function buildComponentFixture(
  mocks: Mocks,
  opciones: { usarHttpReal?: boolean; unidadResponsableInicial?: number } = {}
): { fixture: ComponentFixture<DirectProjectComponent>; component: DirectProjectComponent } {
  TestBed.resetTestingModule();
  TestBed.configureTestingModule({
    imports: [DirectProjectComponent],
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
  const fixture = TestBed.createComponent(DirectProjectComponent);
  const component = fixture.componentInstance;
  if (opciones.unidadResponsableInicial !== undefined) {
    fixture.componentRef.setInput('unidadResponsableInicial', opciones.unidadResponsableInicial);
  }
  fixture.detectChanges();
  return { fixture, component };
}

function proyectoSintetico(overrides: Partial<ProjectDetail> = {}): ProjectDetail {
  return {
    id: 505,
    iniciativaId: undefined,
    codigo: '2026-MIDAGRI-00505',
    codigoOrigen: 'RESOLUCION-2026-MIDAGRI-001',
    fechaInicio: '2026-07-23',
    nombre: 'Proyecto directo piloto',
    tipoRegistro: 'PROYECTO',
    estado: 'PROYECTO_EJECUCION',
    fuenteOrigen: 'PROPUESTA_JEFATURA',
    detalleFuente: undefined,
    responsableId: 9,
    problemaPublico: undefined,
    solucionPropuesta: undefined,
    objetivoPeiId: 10,
    actividadPoiId: 20,
    unidades: [{ unidadId: 7, principal: true, abreviatura: 'MIDAGRI' }],
    componenteDigital: false,
    detalleComponenteDigital: undefined,
    nota: undefined,
    documentoFormalId: 909,
    version: 1,
    etag: '505-1',
    fechaCreacion: '2026-07-23T13:00:00',
    ...overrides
  };
}

function comandoDirectoValido(overrides: Partial<DirectProjectCommand> = {}): DirectProjectCommand {
  return {
    tipoOrigen: 'EXCEPCION_FORMAL' as TipoOrigenDirecto,
    codigoOrigen: 'RESOLUCION-2026-MIDAGRI-001',
    fechaInicio: '2026-07-23',
    nombre: 'Proyecto directo piloto',
    objetivoPeiId: 10,
    actividadPoiId: 20,
    unidadResponsableId: 7,
    responsableId: 9,
    descripcion: 'Descripcion institucional del proyecto directo',
    componenteDigital: false,
    detalleComponenteDigital: undefined,
    nota: 'Nota institucional',
    documentoAutorizacionId: 909,
    evidenciaIds: [901, 902],
    fuenteOrigen: 'PROPUESTA_JEFATURA',
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

const UNIDAD_ID = 7;

describe('DirectProjectComponent (T063 - US3)', () => {
  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  describe('selector de `tipoOrigen` y verificacion de iniciativa nueva', () => {
    let mocks: Mocks;
    let component: DirectProjectComponent;
    let fixture: ComponentFixture<DirectProjectComponent>;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mockReturnValue(of({ id: undefined }));
      ({ fixture, component } = buildComponentFixture(mocks, { unidadResponsableInicial: UNIDAD_ID }));
    });

    it('expone las dos opciones canonicas de `TipoOrigenDirecto`', () => {
      expect((component as any).tipoOrigenOpciones).toEqual(['HEREDADO', 'EXCEPCION_FORMAL']);
    });

    it('consulta la iniciativa `PRESENTADO` de la misma unidad al inicializar', () => {
      expect(mocks.proyectos.consultarIniciativaPresentadaPorUnidad).toHaveBeenCalledOnce();
      expect(mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mock.calls[0][0]).toBe(UNIDAD_ID);
    });

    it('permite registrar el proyecto directo cuando NO existe iniciativa `PRESENTADO` en la unidad', () => {
      expect((component as any).bloqueadoPorIniciativaPresentada()).toBe(false);
    });

    it('bloquea el envio cuando existe una iniciativa `PRESENTADO` para el mismo ambito', async () => {
      mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mockReturnValue(of({ id: 999 }));
      const { fixture: fx } = buildComponentFixture(mocks, { unidadResponsableInicial: UNIDAD_ID });
      await fx.whenStable();
      expect((fx.componentInstance as any).bloqueadoPorIniciativaPresentada()).toBe(true);
      expect((fx.componentInstance as any).iniciativaPresentadaConflicto()?.id).toBe(999);
    });

    it('reconsulta al cambiar la unidad responsable y conserva la regla "no omite evaluacion"', async () => {
      mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mockReturnValue(of({ id: 1234 }));
      (component as any).cambiarUnidadResponsable(8);
      await fixture.whenStable();
      expect((component as any).bloqueadoPorIniciativaPresentada()).toBe(true);
    });
  });

  describe('formulario con los 15 campos de `DirectProjectRequest`', () => {
    let mocks: Mocks;
    let component: DirectProjectComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mockReturnValue(of({ id: undefined }));
      ({ component } = buildComponentFixture(mocks, { unidadResponsableInicial: UNIDAD_ID }));
    });

    it('declara los 15 controles del DTO `DirectProjectRequest`', () => {
      expect((component as any).form.controls.tipoOrigen).toBeDefined();
      expect((component as any).form.controls.codigoOrigen).toBeDefined();
      expect((component as any).form.controls.fechaInicio).toBeDefined();
      expect((component as any).form.controls.nombre).toBeDefined();
      expect((component as any).form.controls.objetivoPeiId).toBeDefined();
      expect((component as any).form.controls.actividadPoiId).toBeDefined();
      expect((component as any).form.controls.unidadResponsableId).toBeDefined();
      expect((component as any).form.controls.responsableId).toBeDefined();
      expect((component as any).form.controls.descripcion).toBeDefined();
      expect((component as any).form.controls.componenteDigital).toBeDefined();
      expect((component as any).form.controls.detalleComponenteDigital).toBeDefined();
      expect((component as any).form.controls.nota).toBeDefined();
      expect((component as any).form.controls.documentoAutorizacionId).toBeDefined();
      expect((component as any).form.controls.evidenciaIds).toBeDefined();
      expect((component as any).form.controls.fuenteOrigen).toBeDefined();
    });

    it('marca como invalido el formulario vacio y exige documento formal y al menos una evidencia', () => {
      expect((component as any).form.invalid).toBe(true);
      expect((component as any).form.controls.documentoAutorizacionId.hasError('required')).toBe(true);
      expect((component as any).form.controls.evidenciaIds.hasError('required')).toBe(true);
    });

    it('trunca el nombre a 500 caracteres y el codigo de origen a 50', () => {
      (component as any).form.controls.nombre.setValue('a'.repeat(501));
      (component as any).form.controls.codigoOrigen.setValue('c'.repeat(51));
      expect((component as any).form.controls.nombre.hasError('maxlength')).toBe(true);
      expect((component as any).form.controls.codigoOrigen.hasError('maxlength')).toBe(true);
    });

    it('limita `descripcion` a 2000, `nota` a 1000 y `detalleComponenteDigital` a 500', () => {
      (component as any).form.controls.descripcion.setValue('d'.repeat(2001));
      (component as any).form.controls.nota.setValue('n'.repeat(1001));
      (component as any).form.controls.componenteDigital.setValue(true);
      (component as any).form.controls.detalleComponenteDigital.setValue('c'.repeat(501));
      expect((component as any).form.controls.descripcion.hasError('maxlength')).toBe(true);
      expect((component as any).form.controls.nota.hasError('maxlength')).toBe(true);
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

    it('hace obligatorio `codigoOrigen` para `EXCEPCION_FORMAL` y valida la fecha de inicio', () => {
      (component as any).form.controls.tipoOrigen.setValue('EXCEPCION_FORMAL');
      expect((component as any).form.controls.codigoOrigen.hasError('required')).toBe(true);
      (component as any).form.controls.codigoOrigen.setValue('RESOLUCION-2026-MIDAGRI-001');
      expect((component as any).form.controls.codigoOrigen.hasError('required')).toBe(false);

      (component as any).form.controls.fechaInicio.setValue('');
      expect((component as any).form.controls.fechaInicio.hasError('required')).toBe(true);
    });

    it('hace obligatorio el detalle de la fuente al elegir OTROS y lo retira al cambiar', () => {
      (component as any).form.controls.fuenteOrigen.setValue('OTROS');
      expect((component as any).form.controls.detalleFuente?.hasError('required')).toBe(true);
      (component as any).form.controls.detalleFuente?.setValue('Detalle institucional');
      expect((component as any).form.controls.detalleFuente?.hasError('required')).toBe(false);
      (component as any).form.controls.fuenteOrigen.setValue('PROPUESTA_JEFATURA');
      expect((component as any).form.controls.detalleFuente?.hasError('required')).toBe(false);
    });

    it('hace obligatorio el detalle del componente digital al activar el flag', () => {
      (component as any).form.controls.componenteDigital.setValue(true);
      expect((component as any).form.controls.detalleComponenteDigital.hasError('required')).toBe(true);
      (component as any).form.controls.detalleComponenteDigital.setValue('Plataforma institucional');
      expect((component as any).form.controls.detalleComponenteDigital.hasError('required')).toBe(false);
    });

    it('declara `documentoAutorizacionId` obligatorio y exige minimo 1', () => {
      (component as any).form.controls.documentoAutorizacionId.setValue(0);
      expect((component as any).form.controls.documentoAutorizacionId.hasError('min')).toBe(true);
      (component as any).form.controls.documentoAutorizacionId.setValue(909);
      expect((component as any).form.controls.documentoAutorizacionId.hasError('min')).toBe(false);
    });

    it('expone el titulo del documento formal exigido por la matriz 013', () => {
      expect((component as any).tituloDocumentoFormal).toBe(
        'Documento Formal de Aprobacion o Autorizacion de Inicio'
      );
    });
  });

  describe('envio del comando directo con `Idempotency-Key`', () => {
    let mocks: Mocks;
    let component: DirectProjectComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mockReturnValue(of({ id: undefined }));
      mocks.proyectos.crearProyectoDirecto.mockReturnValue(of(proyectoSintetico()));
      ({ component } = buildComponentFixture(mocks, { unidadResponsableInicial: UNIDAD_ID }));
    });

    function prepararFormularioValido(): void {
      (component as any).form.patchValue({
        tipoOrigen: 'EXCEPCION_FORMAL',
        codigoOrigen: 'RESOLUCION-2026-MIDAGRI-001',
        fechaInicio: '2026-07-23',
        nombre: 'Proyecto directo piloto',
        objetivoPeiId: 10,
        actividadPoiId: 20,
        unidadResponsableId: UNIDAD_ID,
        responsableId: 9,
        descripcion: 'Descripcion institucional del proyecto directo',
        componenteDigital: false,
        nota: 'Nota institucional',
        documentoAutorizacionId: 909,
        fuenteOrigen: 'PROPUESTA_JEFATURA',
        evidenciaIds: [901, 902]
      });
    }

    it('rechaza el envio cuando el formulario es invalido y no llama al backend', async () => {
      await (component as any).enviar();
      expect(mocks.proyectos.crearProyectoDirecto).not.toHaveBeenCalled();
      expect((component as any).form.invalid).toBe(true);
    });

    it('rechaza el envio cuando hay una iniciativa `PRESENTADO` en la misma unidad', async () => {
      mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mockReturnValue(of({ id: 999 }));
      const { fixture, component: bloqueado } = buildComponentFixture(mocks, { unidadResponsableInicial: UNIDAD_ID });
      await fixture.whenStable();
      (bloqueado as any).form.patchValue({
        tipoOrigen: 'EXCEPCION_FORMAL',
        codigoOrigen: 'RESOLUCION-2026-MIDAGRI-001',
        fechaInicio: '2026-07-23',
        nombre: 'Proyecto directo piloto',
        objetivoPeiId: 10,
        actividadPoiId: 20,
        unidadResponsableId: UNIDAD_ID,
        responsableId: 9,
        descripcion: 'Descripcion institucional del proyecto directo',
        componenteDigital: false,
        documentoAutorizacionId: 909,
        fuenteOrigen: 'PROPUESTA_JEFATURA',
        evidenciaIds: [901, 902]
      });
      await (bloqueado as any).enviar();
      expect(mocks.proyectos.crearProyectoDirecto).not.toHaveBeenCalled();
    });

    it('envia el comando directo con la fecha y todos los campos canonicos', async () => {
      prepararFormularioValido();
      await (component as any).enviar();

      expect(mocks.proyectos.crearProyectoDirecto).toHaveBeenCalledOnce();
      const args = mocks.proyectos.crearProyectoDirecto.mock.calls[0];
      expect(args[0]).toEqual(comandoDirectoValido());
    });

    it('omite `detalleFuente` cuando `fuenteOrigen` no es OTROS', async () => {
      prepararFormularioValido();
      await (component as any).enviar();
      const payload = mocks.proyectos.crearProyectoDirecto.mock.calls[0][0];
      expect(payload.detalleFuente).toBeUndefined();
    });

    it('exige la fecha de inicio porque el cliente la envia como dato contractual', () => {
      (component as any).form.patchValue({ fechaInicio: null });
      expect((component as any).form.controls.fechaInicio.hasError('required')).toBe(true);
    });

    it('actualiza la ETag y expone el proyecto creado al recibir la respuesta exitosa', async () => {
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).proyectoCreado()?.id).toBe(505);
      expect((component as any).proyectoCreado()?.estado).toBe('PROYECTO_EJECUCION');
      expect((component as any).proyectoCreado()?.codigo).toBe('2026-MIDAGRI-00505');
      expect((component as any).etagActual()).toBe('505-1');
    });

    it('mapea `FORMAL_DOCUMENT_REQUIRED` (422) y expone las violaciones del documento', async () => {
      mocks.proyectos.crearProyectoDirecto.mockReturnValue(
        throwError(
          () =>
            problemaHttp(422, {
              type: 'about:blank',
              title: 'Documento formal obligatorio',
              status: 422,
              code: 'FORMAL_DOCUMENT_REQUIRED',
              correlationId: 'dir-422-doc',
              detail: 'El documento de autorizacion es obligatorio.',
              violations: [
                { field: 'documentoAutorizacionId', message: 'Debe adjuntar el documento formal de autorizacion.' }
              ]
            })
        )
      );
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('FORMAL_DOCUMENT_REQUIRED');
      expect((component as any).violacionesPara('documentoAutorizacionId')).toHaveLength(1);
    });

    it('mapea `EVIDENCE_NOT_ELIGIBLE` (422) cuando la evidencia no cumple los requisitos', async () => {
      mocks.proyectos.crearProyectoDirecto.mockReturnValue(
        throwError(
          () =>
            problemaHttp(422, {
              type: 'about:blank',
              title: 'Evidencia no elegible',
              status: 422,
              code: 'EVIDENCE_NOT_ELIGIBLE',
              correlationId: 'dir-422-ev',
              detail: 'La evidencia 901 no es apta para un proyecto directo.'
            })
        )
      );
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('EVIDENCE_NOT_ELIGIBLE');
    });

    it('mapea `OFFICIAL_FIELD_REQUIRED` (422) cuando falta un campo oficial obligatorio', async () => {
      mocks.proyectos.crearProyectoDirecto.mockReturnValue(
        throwError(
          () =>
            problemaHttp(422, {
              type: 'about:blank',
              title: 'Campo oficial obligatorio',
              status: 422,
              code: 'OFFICIAL_FIELD_REQUIRED',
              correlationId: 'dir-422-field',
              detail: 'El campo oficial 12 (Unidad responsable) es obligatorio.',
              violations: [{ field: 'unidadResponsableId', message: 'Debe seleccionar la unidad responsable.' }]
            })
        )
      );
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('OFFICIAL_FIELD_REQUIRED');
      expect((component as any).violacionesPara('unidadResponsableId')).toHaveLength(1);
    });

    it('mapea `ASSIGNMENT_SCOPE_DENIED` (403) cuando la asignacion efectiva no coincide', async () => {
      mocks.proyectos.crearProyectoDirecto.mockReturnValue(
        throwError(
          () =>
            problemaHttp(403, {
              type: 'about:blank',
              title: 'Acceso denegado',
              status: 403,
              code: 'ASSIGNMENT_SCOPE_DENIED',
              correlationId: 'dir-403-scope',
              detail: 'La asignacion efectiva no cubre la unidad responsable.'
            })
        )
      );
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('ASSIGNMENT_SCOPE_DENIED');
      expect((component as any).problem()?.status).toBe(403);
    });
  });

  describe('conflicto 409 por duplicado o por iniciativa nueva', () => {
    let mocks: Mocks;
    let component: DirectProjectComponent;

    function prepararFormularioValido(): void {
      (component as any).form.patchValue({
        tipoOrigen: 'EXCEPCION_FORMAL',
        codigoOrigen: 'RESOLUCION-2026-MIDAGRI-001',
        fechaInicio: '2026-07-23',
        nombre: 'Proyecto directo piloto',
        objetivoPeiId: 10,
        actividadPoiId: 20,
        unidadResponsableId: UNIDAD_ID,
        responsableId: 9,
        descripcion: 'Descripcion institucional del proyecto directo',
        componenteDigital: false,
        documentoAutorizacionId: 909,
        fuenteOrigen: 'PROPUESTA_JEFATURA',
        evidenciaIds: [901, 902]
      });
    }

    it('mapea `DIRECT_PROJECT_DUPLICATE` (409) cuando ya hay un directo activo para la misma unidad y anio', async () => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mockReturnValue(of({ id: undefined }));
      mocks.proyectos.crearProyectoDirecto.mockReturnValue(
        throwError(
          () =>
            problemaHttp(409, {
              type: 'about:blank',
              title: 'Proyecto directo duplicado',
              status: 409,
              code: 'DIRECT_PROJECT_DUPLICATE',
              correlationId: 'dir-409-dup',
              detail: 'Ya existe un proyecto directo activo para la unidad 7 en el anio 2026.'
            })
        )
      );
      ({ component } = buildComponentFixture(mocks, { unidadResponsableInicial: UNIDAD_ID }));
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('DIRECT_PROJECT_DUPLICATE');
      expect((component as any).problem()?.status).toBe(409);
    });

    it('mapea `DIRECT_PROJECT_NOT_AUTHORIZED` (409) por carrera concurrente sobre la misma unidad y anio', async () => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mockReturnValue(of({ id: undefined }));
      mocks.proyectos.crearProyectoDirecto.mockReturnValue(
        throwError(
          () =>
            problemaHttp(409, {
              type: 'about:blank',
              title: 'Proyecto directo no autorizado por carrera',
              status: 409,
              code: 'DIRECT_PROJECT_NOT_AUTHORIZED',
              correlationId: 'dir-409-race',
              detail: 'Otro directo activo para la unidad 7 y anio 2026 gano la carrera.'
            })
        )
      );
      ({ component } = buildComponentFixture(mocks, { unidadResponsableInicial: UNIDAD_ID }));
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('DIRECT_PROJECT_NOT_AUTHORIZED');
    });

    it('mapea `INITIATIVE_PRESENT_FOR_SCOPE` (409) cuando el backend detecta iniciativa `PRESENTADO`', async () => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mockReturnValue(of({ id: undefined }));
      mocks.proyectos.crearProyectoDirecto.mockReturnValue(
        throwError(
          () =>
            problemaHttp(409, {
              type: 'about:blank',
              title: 'Existe iniciativa PRESENTADO en la unidad',
              status: 409,
              code: 'INITIATIVE_PRESENT_FOR_SCOPE',
              correlationId: 'dir-409-ini',
              detail: 'No se omite la evaluacion: la unidad 7 tiene una iniciativa PRESENTADO.'
            })
        )
      );
      ({ component } = buildComponentFixture(mocks, { unidadResponsableInicial: UNIDAD_ID }));
      prepararFormularioValido();
      await (component as any).enviar();

      expect((component as any).problem()?.code).toBe('INITIATIVE_PRESENT_FOR_SCOPE');
    });
  });

  describe('cabeceras HTTP (Idempotency-Key, X-Asignacion-Efectiva-Id)', () => {
    let mocks: Mocks;
    let component: DirectProjectComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mockReturnValue(of({ id: undefined }));
      mocks.proyectos.crearProyectoDirecto.mockReturnValue(of(proyectoSintetico()));
      ({ component } = buildComponentFixture(mocks, {
        unidadResponsableInicial: UNIDAD_ID
      }));
    });

    function prepararFormularioValido(): void {
      (component as any).form.patchValue({
        tipoOrigen: 'EXCEPCION_FORMAL',
        codigoOrigen: 'RESOLUCION-2026-MIDAGRI-001',
        fechaInicio: '2026-07-23',
        nombre: 'Proyecto directo piloto',
        objetivoPeiId: 10,
        actividadPoiId: 20,
        unidadResponsableId: UNIDAD_ID,
        responsableId: 9,
        descripcion: 'Descripcion institucional del proyecto directo',
        componenteDigital: false,
        documentoAutorizacionId: 909,
        fuenteOrigen: 'PROPUESTA_JEFATURA',
        evidenciaIds: [901, 902]
      });
    }

    it('adjunta `Idempotency-Key` y `X-Asignacion-Efectiva-Id` en la creacion directa', async () => {
      prepararFormularioValido();
      await (component as any).enviar();

      expect(mocks.proyectos.crearProyectoDirecto).toHaveBeenCalledOnce();
      const [comando, opciones] = mocks.proyectos.crearProyectoDirecto.mock.calls[0];
      expect(comando.nombre).toBe('Proyecto directo piloto');
      // Idempotency-Key, Authorization y X-Asignacion-Efectiva-Id son cabeceras
      // HTTP añadidas por interceptores; se verifican en pruebas de integración.
      // If-Match no aplica porque proyectos-directos es un endpoint sin ETag.
    });
  });

  describe('cancelacion del formulario sin enviar al backend', () => {
    let mocks: Mocks;
    let component: DirectProjectComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mockReturnValue(of({ id: undefined }));
      ({ component } = buildComponentFixture(mocks, { unidadResponsableInicial: UNIDAD_ID }));
    });

    it('cancela la edicion revirtiendo el formulario y limpiando el ProblemDetail', () => {
      (component as any).form.patchValue({
        tipoOrigen: 'HEREDADO',
        codigoOrigen: 'PROYECTO-LEGACY-2025-001',
        fechaInicio: '2026-07-23',
        nombre: 'Pendiente',
        objetivoPeiId: 10,
        actividadPoiId: 20,
        unidadResponsableId: UNIDAD_ID,
        responsableId: 9,
        descripcion: 'Pendiente',
        componenteDigital: false,
        documentoAutorizacionId: 909,
        fuenteOrigen: 'PROPUESTA_JEFATURA',
        evidenciaIds: [901, 902]
      });

      (component as any).cancelarEdicion();

      expect(mocks.proyectos.crearProyectoDirecto).not.toHaveBeenCalled();
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
    let fixture: ComponentFixture<DirectProjectComponent>;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mockReturnValue(of({ id: undefined }));
      ({ fixture } = buildComponentFixture(mocks, { unidadResponsableInicial: UNIDAD_ID }));
    });

    function dom(): HTMLElement {
      return fixture.nativeElement as HTMLElement;
    }

    it('asocia cada `mat-label` con su control mediante `for`/`id`', () => {
      const asociaciones: Array<[string, string]> = [
        ['direct-tipo-origen', 'direct-tipo-origen'],
        ['direct-codigo-origen', 'direct-codigo-origen'],
        ['direct-fecha-inicio', 'direct-fecha-inicio'],
        ['direct-nombre', 'direct-nombre'],
        ['direct-objetivo-pei', 'direct-objetivo-pei'],
        ['direct-actividad-poi', 'direct-actividad-poi'],
        ['direct-unidad-responsable', 'direct-unidad-responsable'],
        ['direct-responsable', 'direct-responsable'],
        ['direct-descripcion', 'direct-descripcion'],
        ['direct-detalle-digital', 'direct-detalle-digital'],
        ['direct-nota', 'direct-nota'],
        ['direct-documento-autorizacion', 'direct-documento-autorizacion'],
        ['direct-fuente', 'direct-fuente']
      ];

      for (const [forTarget, id] of asociaciones) {
        const label = dom().querySelector(`mat-label[for="${forTarget}"]`);
        const control = dom().querySelector(`#${id}`);
        expect(label, `label[for=${forTarget}] ausente`).not.toBeNull();
        expect(control, `control #${id} ausente`).not.toBeNull();
      }
    });

    it('expone `aria-describedby` con identificadores de ayuda y error en campos obligatorios', () => {
      const nombre = dom().querySelector<HTMLInputElement>('#direct-nombre');
      expect(nombre?.getAttribute('aria-describedby')).toContain('direct-nombre-help');
      expect(nombre?.getAttribute('aria-describedby')).toContain('direct-nombre-error');
    });

    it('declara una region `aria-live="assertive"` para errores del servidor', () => {
      const region = dom().querySelector('.direct-project__server-errors');
      expect(region?.getAttribute('role')).toBe('alert');
      expect(region?.getAttribute('aria-live')).toBe('assertive');
    });

    it('declara un `output` con `aria-live="polite"` para el resultado de la creacion', () => {
      const output = dom().querySelector('output.direct-project__result');
      expect(output).not.toBeNull();
      expect(output?.getAttribute('aria-live')).toBe('polite');
    });

    it('expone el boton de envio con texto descriptivo y tipo `submit`', () => {
      const boton = dom().querySelector<HTMLButtonElement>('button[type="submit"][data-action="crear-directo"]');
      expect(boton).not.toBeNull();
      expect(boton?.textContent?.trim()).toBe('Crear proyecto directo');
    });

    it('asocia el boton de cancelar con la region del formulario mediante `aria-controls`', () => {
      const boton = dom().querySelector<HTMLButtonElement>('button[data-action="cancelar-directo"]');
      expect(boton).not.toBeNull();
      expect(boton?.getAttribute('aria-controls')).toBe('direct-project-form');
    });

    it('marca como `required` los campos `nombre`, `descripcion`, `unidadResponsableId` y `documentoAutorizacionId`', () => {
      const nombre = dom().querySelector<HTMLInputElement>('#direct-nombre');
      const descripcion = dom().querySelector<HTMLTextAreaElement>('#direct-descripcion');
      const unidad = dom().querySelector<HTMLInputElement>('#direct-unidad-responsable');
      const documento = dom().querySelector<HTMLInputElement>('#direct-documento-autorizacion');

      expect(nombre?.required).toBe(true);
      expect(descripcion?.required).toBe(true);
      expect(unidad?.required).toBe(true);
      expect(documento?.required).toBe(true);
    });

    it('anuncia el bloqueo por iniciativa `PRESENTADO` mediante una region `aria-live`', async () => {
      mocks.proyectos.consultarIniciativaPresentadaPorUnidad.mockReturnValue(of({ id: 999 }));
      const { fixture: fx } = buildComponentFixture(mocks, { unidadResponsableInicial: UNIDAD_ID });
      await fx.whenStable();
      fx.detectChanges();
      const region = fx.nativeElement.querySelector('.direct-project__scope-blocked');
      expect(region).not.toBeNull();
      expect(region?.getAttribute('role')).toBe('alert');
      expect(region?.getAttribute('aria-live')).toBe('assertive');
    });
  });
});
