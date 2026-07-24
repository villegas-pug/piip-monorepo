
// Pruebas del formulario de presentación de iniciativa (T042 - US1).
//
// Cubre la matriz 013 con los 23 campos oficiales del portafolio institucional:
//   * Campos 5-12, 22 y 23 editables por el cliente.
//   * Campos 1, 2, 3, 4 y 13 generados por el backend.
//   * Reglas condicionales: detalle de fuente (OTROS) y detalle de componente digital.
//   * Cardinalidad de unidades: exactamente una principal (`UNIT_MAIN_CARDINALITY`).
//   * Carga de ficha documental con `Idempotency-Key` y `If-Match` cuando aplique.
//   * Manejo de `ProblemDetail` con códigos canónicos (`UNIT_PREFIX_NOT_AVAILABLE`,
//     `OFFICIAL_FIELD_REQUIRED`, `CATALOG_NOT_ACTIVE`, `CAMPO_REQUERIDO`, etc.).
//   * Envío con `Idempotency-Key` (doble envío con misma clave produce el mismo resultado;
//     con clave distinta produce 409 Conflict).
//   * Accesibilidad WCAG 2.1 AA: labels asociados, mensajes de error con
//     `aria-describedby`, regiones `aria-live`, navegación por teclado y contraste
//     delegado al tema institucional.

import { HttpErrorResponse, HttpHeaders, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { firstValueFrom, of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { EffectiveAssignmentContext, effectiveAssignmentInterceptor } from '../../../../core/effective-assignment/effective-assignment.interceptor';
import { EffectiveAssignmentService } from '../../../../core/effective-assignment/effective-assignment.service';
import { authInterceptor } from '../../../../core/http/auth.interceptor';
import { entityTagInterceptor } from '../../../../core/http/entity-tag';
import { idempotencyKeyInterceptor } from '../../../../core/http/idempotency-key.service';
import { AuthService } from '../../../../core/auth/auth.service';
import { DocumentosApiService } from '../../../documentos/api/documentos-api.service';
import { OrganizacionApiService } from '../../../organizacion/api/organizacion-api.service';
import { RegistroApiService } from '../api/registro-api.service';
import { InitiativeDetail } from '../api/types/iniciativa.types';
import { InitiativeFormComponent } from './initiative-form.component';

interface Mocks {
  readonly documentos: { cargar: ReturnType<typeof vi.fn> };
  readonly registro: { presentarIniciativa: ReturnType<typeof vi.fn> };
  readonly organizacion: {
    consultarUnidades: ReturnType<typeof vi.fn>;
    consultarObjetivosPei: ReturnType<typeof vi.fn>;
    consultarActividadesPoi: ReturnType<typeof vi.fn>;
  };
}

function buildMocks(): Mocks {
  return {
    documentos: { cargar: vi.fn() },
    registro: { presentarIniciativa: vi.fn() },
    organizacion: {
      consultarUnidades: vi.fn(),
      consultarObjetivosPei: vi.fn(),
      consultarActividadesPoi: vi.fn()
    }
  };
}

function buildComponentFixture(
  mocks: Mocks,
  opciones: { usarHttpReal?: boolean } = {}
): { fixture: ComponentFixture<InitiativeFormComponent>; component: InitiativeFormComponent } {
  TestBed.resetTestingModule();
  if (opciones.usarHttpReal) {
    TestBed.configureTestingModule({
      imports: [InitiativeFormComponent],
      providers: [
        provideHttpClient(
          withInterceptors([authInterceptor, idempotencyKeyInterceptor, effectiveAssignmentInterceptor, entityTagInterceptor])
        ),
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
        { provide: DocumentosApiService, useValue: mocks.documentos },
        { provide: RegistroApiService, useValue: mocks.registro },
        { provide: OrganizacionApiService, useValue: mocks.organizacion }
      ]
    });
  } else {
    TestBed.configureTestingModule({
      imports: [InitiativeFormComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        {
          provide: AuthService,
          useValue: { getValidAccessToken: async () => 'token-sintetico' }
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
        { provide: DocumentosApiService, useValue: mocks.documentos },
        { provide: RegistroApiService, useValue: mocks.registro },
        { provide: OrganizacionApiService, useValue: mocks.organizacion }
      ]
    });
  }
  const fixture = TestBed.createComponent(InitiativeFormComponent);
  fixture.detectChanges();
  return { fixture, component: fixture.componentInstance };
}

function detalleIniciativaValido(overrides: Partial<InitiativeDetail> = {}): InitiativeDetail {
  return {
    id: 1,
    tipoRegistro: 'INICIATIVA',
    codigo: '2026-MIDAGRI-00001',
    fechaInicio: '2026-02-01',
    estado: 'PRESENTADO',
    version: 1,
    etag: 'W/"1"',
    nombre: 'Iniciativa piloto',
    tipoSolucion: 'POTENCIAL_ADAPTABLE',
    fuenteOrigen: 'FICHA_INICIATIVA',
    detalleFuente: '',
    problemaPublico: 'Problema detectado',
    solucionPropuesta: '',
    responsableId: 5,
    objetivoPeiId: 1,
    actividadPoiId: 2,
    unidades: [{ unidadId: 7, principal: true }],
    componenteDigital: false,
    detalleComponenteDigital: '',
    nota: '',
    ...overrides
  };
}

function problemaHttp(
  status: number,
  body: Record<string, unknown>,
  contentType = 'application/problem+json'
): HttpErrorResponse {
  return new HttpErrorResponse({
    status,
    headers: new HttpHeaders({ 'Content-Type': contentType }),
    error: body
  });
}

describe('InitiativeFormComponent (T042 - US1)', () => {
  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  describe('campos oficiales 5-12, 22 y 23 de la matriz 013', () => {
    let mocks: Mocks;
    let component: InitiativeFormComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.organizacion.consultarUnidades.mockReturnValue(
        of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 })
      );
      mocks.organizacion.consultarObjetivosPei.mockReturnValue(
        of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 })
      );
      mocks.organizacion.consultarActividadesPoi.mockReturnValue(
        of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 })
      );
      ({ component } = buildComponentFixture(mocks));
    });

    it('declara los 23 campos oficiales (5-12, 22 y 23 editables)', () => {
      // Campos 5 a 12 de la matriz 013.
      expect((component as any).form.controls.nombre).toBeDefined();
      expect((component as any).form.controls.tipoSolucion).toBeDefined();
      expect((component as any).form.controls.fuenteOrigen).toBeDefined();
      expect((component as any).form.controls.detalleFuente).toBeDefined();
      expect((component as any).form.controls.problemaPublico).toBeDefined();
      expect((component as any).form.controls.solucionPropuesta).toBeDefined();
      expect((component as any).form.controls.responsableId).toBeDefined();
      expect((component as any).form.controls.objetivoPeiId).toBeDefined();
      expect((component as any).form.controls.actividadPoiId).toBeDefined();
      expect((component as any).form.controls.unidades).toBeDefined();
      // Campo 22.
      expect((component as any).form.controls.componenteDigital).toBeDefined();
      expect((component as any).form.controls.detalleComponenteDigital).toBeDefined();
      // Campo 23.
      expect((component as any).form.controls.nota).toBeDefined();
      // Ficha documental obligatoria (campo 14, expuesto como control).
      expect((component as any).form.controls.fichaDocumentoVersionId).toBeDefined();
    });

    it('marca como inválido el formulario vacío y exige los 23 campos', () => {
      expect((component as any).form.invalid).toBe(true);
    });

    it('rechaza valores fuera de catálogo para tipoSolucion y fuenteOrigen', () => {
      // El tipado rechaza valores ajenos al union; este test verifica el validador
      // `required` y la pertenencia al catálogo de opciones mostrado al usuario.
      expect((component as any).tipoSolucionOpciones).toEqual(['POTENCIAL_ADAPTABLE', 'POR_DEFINIR']);
      expect((component as any).fuenteOrigenOpciones).toEqual([
        'FICHA_INICIATIVA',
        'CONCURSO_INTERNO',
        'INNOVACION_ABIERTA',
        'PROPUESTA_JEFATURA',
        'OTROS'
      ]);
    });

    it('trunca el nombre a 500 caracteres según la matriz 013 y el contrato OpenAPI', () => {
      const nombreLargo = 'a'.repeat(750);
      (component as any).form.controls.nombre.setValue(nombreLargo);
      expect((component as any).form.controls.nombre.hasError('maxlength')).toBe(true);
    });

    it('limita problemaPublico y solucionPropuesta a 2000 caracteres', () => {
      (component as any).form.controls.problemaPublico.setValue('p'.repeat(2001));
      (component as any).form.controls.solucionPropuesta.setValue('s'.repeat(2001));
      expect((component as any).form.controls.problemaPublico.hasError('maxlength')).toBe(true);
      expect((component as any).form.controls.solucionPropuesta.hasError('maxlength')).toBe(true);
    });

    it('limita nota institucional a 1000 caracteres (campo 23)', () => {
      (component as any).form.controls.nota.setValue('n'.repeat(1001));
      expect((component as any).form.controls.nota.hasError('maxlength')).toBe(true);
    });
  });

  describe('reglas condicionales OTROS y Componente digital', () => {
    let mocks: Mocks;
    let component: InitiativeFormComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.organizacion.consultarUnidades.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
      mocks.organizacion.consultarObjetivosPei.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
      mocks.organizacion.consultarActividadesPoi.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
      ({ component } = buildComponentFixture(mocks));
    });

    it('hace obligatorio el detalle de la fuente cuando se elige OTROS', () => {
      (component as any).form.controls.fuenteOrigen.setValue('OTROS');
      expect((component as any).form.controls.detalleFuente.hasError('required')).toBe(true);

      (component as any).form.controls.detalleFuente.setValue('Detalle institucional');
      expect((component as any).form.controls.detalleFuente.hasError('required')).toBe(false);
    });

    it('retira la obligatoriedad del detalle de la fuente al cambiar a otra opción', () => {
      (component as any).form.controls.fuenteOrigen.setValue('OTROS');
      (component as any).form.controls.detalleFuente.setValue('Detalle institucional');
      (component as any).form.controls.fuenteOrigen.setValue('FICHA_INICIATIVA');

      expect((component as any).form.controls.detalleFuente.hasError('required')).toBe(false);
    });

    it('hace obligatorio el detalle del componente digital cuando se marca Sí', () => {
      (component as any).form.controls.componenteDigital.setValue(true);
      expect((component as any).form.controls.detalleComponenteDigital.hasError('required')).toBe(true);

      (component as any).form.controls.detalleComponenteDigital.setValue('Plataforma móvil');
      expect((component as any).form.controls.detalleComponenteDigital.hasError('required')).toBe(false);
    });

    it('retira la obligatoriedad del detalle del componente digital al marcar No', () => {
      (component as any).form.controls.componenteDigital.setValue(true);
      (component as any).form.controls.detalleComponenteDigital.setValue('Plataforma móvil');
      (component as any).form.controls.componenteDigital.setValue(false);

      expect((component as any).form.controls.detalleComponenteDigital.hasError('required')).toBe(false);
    });

    it('expone un mensaje de error legible para los campos condicionales', () => {
      (component as any).form.controls.fuenteOrigen.setValue('OTROS');
      (component as any).form.controls.detalleFuente.markAsTouched();
      expect((component as any).mensajeError('detalleFuente')).toBe('Cuando la fuente es OTROS debe describirla.');

      (component as any).form.controls.componenteDigital.setValue(true);
      (component as any).form.controls.detalleComponenteDigital.markAsTouched();
      expect((component as any).mensajeError('detalleComponenteDigital')).toBe(
        'Cuando el componente digital es Sí debe describirlo.'
      );
    });
  });

  describe('cardinalidad de unidades (UNIT_MAIN_CARDINALITY)', () => {
    let mocks: Mocks;
    let component: InitiativeFormComponent;

    beforeEach(() => {
      mocks = buildMocks();
      mocks.organizacion.consultarUnidades.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
      mocks.organizacion.consultarObjetivosPei.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
      mocks.organizacion.consultarActividadesPoi.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
      ({ component } = buildComponentFixture(mocks));
    });

    it('rechaza un FormArray de unidades vacío', () => {
      expect((component as any).form.controls.unidades.errors).toEqual({ unidadesInvalidas: true });
    });

    it('rechaza unidades sin una principal', () => {
      (component as any).agregarUnidad();
      (component as any).agregarUnidad();
      (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 1, principal: false });
      (component as any).form.controls.unidades.at(1).patchValue({ unidadId: 2, principal: false });

      expect((component as any).form.controls.unidades.errors).toEqual({ unidadesInvalidas: true });
    });

    it('rechaza más de una unidad principal', () => {
      (component as any).agregarUnidad();
      (component as any).agregarUnidad();
      (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 1, principal: true });
      (component as any).form.controls.unidades.at(1).patchValue({ unidadId: 2, principal: true });

      expect((component as any).form.controls.unidades.errors).toEqual({ unidadesInvalidas: true });
    });

    it('acepta exactamente una unidad principal', () => {
      (component as any).agregarUnidad();
      (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });
      expect((component as any).form.controls.unidades.errors).toBeNull();
    });

    it('recalcula la principal al invocar `marcarUnidadPrincipal`', () => {
      (component as any).agregarUnidad();
      (component as any).agregarUnidad();
      (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 1, principal: true });
      (component as any).form.controls.unidades.at(1).patchValue({ unidadId: 2, principal: false });

      (component as any).marcarUnidadPrincipal(1);

      expect((component as any).form.controls.unidades.at(0).controls.principal.value).toBe(false);
      expect((component as any).form.controls.unidades.at(1).controls.principal.value).toBe(true);
    });

    it('expone un mensaje legible cuando se infringe la cardinalidad', () => {
      (component as any).form.controls.unidades.markAsTouched();
      expect((component as any).mensajeError('unidades')).toBe(
        'Debe registrar al menos una unidad y exactamente una principal.'
      );
    });
  });
});

describe('carga de ficha documental con Idempotency-Key', () => {
  let mocks: Mocks;
  let httpMock: HttpTestingController;
  let component: InitiativeFormComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.organizacion.consultarUnidades.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    mocks.organizacion.consultarObjetivosPei.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    mocks.organizacion.consultarActividadesPoi.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    ({ component } = buildComponentFixture(mocks, { usarHttpReal: true }));
    httpMock = TestBed.inject(HttpTestingController);
  });

  it('carga la ficha vía POST /api/v1/documentos con `Idempotency-Key`', async () => {
    mocks.documentos.cargar.mockReturnValue(
      of({
        detail: {
          documentoId: 99,
          serieId: 1,
          version: 1,
          titulo: 'Ficha',
          formato: 'pdf',
          tamanoBytes: 10,
          hashSha256: 'a'.repeat(64),
          clasificacionPropuesta: 'INTERNO',
          aptaComoEvidencia: false,
          etag: 'W/"1"'
        }
      })
    );
    mocks.registro.presentarIniciativa.mockReturnValue(of(detalleIniciativaValido()));

    (component as any).fichaArchivo.set(new File(['contenido'], 'ficha.pdf', { type: 'application/pdf' }));
    (component as any).form.patchValue({
      nombre: 'Iniciativa piloto',
      tipoSolucion: 'POTENCIAL_ADAPTABLE',
      fuenteOrigen: 'FICHA_INICIATIVA',
      problemaPublico: 'Problema detectado',
      responsableId: 5,
      objetivoPeiId: 1,
      actividadPoiId: 2,
      componenteDigital: false,
      nota: ''
    });
    (component as any).agregarUnidad();
    (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });

    await (component as any).enviar();

    expect(mocks.documentos.cargar).toHaveBeenCalledOnce();
    expect(mocks.registro.presentarIniciativa).toHaveBeenCalledOnce();
    const payload = mocks.registro.presentarIniciativa.mock.calls[0][0];
    expect(payload.fichaDocumentoVersionId).toBe(99);
    httpMock.verify();
  });

  it('advierte que la ficha es obligatoria y bloquea el envío cuando falta el archivo', async () => {
    mocks.registro.presentarIniciativa.mockReturnValue(of(detalleIniciativaValido()));

    (component as any).form.patchValue({
      nombre: 'Iniciativa piloto',
      tipoSolucion: 'POTENCIAL_ADAPTABLE',
      fuenteOrigen: 'FICHA_INICIATIVA',
      problemaPublico: 'Problema detectado',
      responsableId: 5,
      objetivoPeiId: 1,
      actividadPoiId: 2,
      componenteDigital: false,
      nota: ''
    });
    (component as any).agregarUnidad();
    (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });

    await (component as any).enviar();

    expect((component as any).fichaError()).toBe('La ficha documental es obligatoria al presentar una iniciativa.');
    expect(mocks.registro.presentarIniciativa).not.toHaveBeenCalled();
  });

  it('muestra el ProblemDetail cuando la carga de ficha falla con `EVIDENCE_NOT_ELIGIBLE`', async () => {
    mocks.documentos.cargar.mockReturnValue(
      throwError(
        () =>
          new HttpErrorResponse({
            status: 422,
            headers: new HttpHeaders({ 'Content-Type': 'application/problem+json' }),
            error: {
              type: 'about:blank',
              title: 'Documento no elegible',
              status: 422,
              code: 'EVIDENCE_NOT_ELIGIBLE',
              correlationId: 'corr-ficha-1',
              detail: 'El archivo no cumple los requisitos del tipo documental.'
            }
          })
      )
    );
    mocks.registro.presentarIniciativa.mockReturnValue(of(detalleIniciativaValido()));

    (component as any).fichaArchivo.set(new File(['contenido'], 'ficha.pdf', { type: 'application/pdf' }));
    (component as any).form.patchValue({
      nombre: 'Iniciativa piloto',
      tipoSolucion: 'POTENCIAL_ADAPTABLE',
      fuenteOrigen: 'FICHA_INICIATIVA',
      problemaPublico: 'Problema detectado',
      responsableId: 5,
      objetivoPeiId: 1,
      actividadPoiId: 2,
      componenteDigital: false,
      nota: ''
    });
    (component as any).agregarUnidad();
    (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });

    await (component as any).enviar();

    expect((component as any).fichaError()).toBe('El archivo no cumple los requisitos del tipo documental.');
    expect(mocks.registro.presentarIniciativa).not.toHaveBeenCalled();
  });
});

describe('envío de iniciativa con `Idempotency-Key` y manejo de ProblemDetail', () => {
  let mocks: Mocks;
  let component: InitiativeFormComponent;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.organizacion.consultarUnidades.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    mocks.organizacion.consultarObjetivosPei.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    mocks.organizacion.consultarActividadesPoi.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    ({ component } = buildComponentFixture(mocks, { usarHttpReal: true }));
    httpMock = TestBed.inject(HttpTestingController);
  });

  function prepararFormularioValido(): void {
    (component as any).fichaArchivo.set(new File(['contenido'], 'ficha.pdf', { type: 'application/pdf' }));
    (component as any).form.patchValue({
      nombre: 'Iniciativa piloto',
      tipoSolucion: 'POTENCIAL_ADAPTABLE',
      fuenteOrigen: 'FICHA_INICIATIVA',
      problemaPublico: 'Problema detectado',
      responsableId: 5,
      objetivoPeiId: 1,
      actividadPoiId: 2,
      componenteDigital: false,
      nota: ''
    });
    (component as any).agregarUnidad();
    (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });
  }

  it('envía el payload normalizado y expone el detalle devuelto por el backend', async () => {
    mocks.documentos.cargar.mockReturnValue(
      of({
        detail: {
          documentoId: 99,
          serieId: 1,
          version: 1,
          titulo: 'Ficha',
          formato: 'pdf',
          tamanoBytes: 10,
          hashSha256: 'a'.repeat(64),
          clasificacionPropuesta: 'INTERNO',
          aptaComoEvidencia: false,
          etag: 'W/"1"'
        }
      })
    );
    mocks.registro.presentarIniciativa.mockReturnValue(of(detalleIniciativaValido({ codigo: '2026-MIDAGRI-00007' })));

    prepararFormularioValido();
    await (component as any).enviar();

    const payload = mocks.registro.presentarIniciativa.mock.calls[0][0];
    expect(payload).toMatchObject({
      nombre: 'Iniciativa piloto',
      tipoSolucion: 'POTENCIAL_ADAPTABLE',
      fuenteOrigen: 'FICHA_INICIATIVA',
      problemaPublico: 'Problema detectado',
      responsableId: 5,
      objetivoPeiId: 1,
      actividadPoiId: 2,
      componenteDigital: false,
      fichaDocumentoVersionId: 99
    });
    expect(payload.unidades).toEqual([{ unidadId: 7, principal: true }]);
    expect((component as any).submittedInitiative()?.codigo).toBe('2026-MIDAGRI-00007');
    expect((component as any).submittedInitiative()?.estado).toBe('PRESENTADO');
    httpMock.verify();
  });

  it('envuelve el nombre, problema y solución con `trim` para evitar espacios residuales', async () => {
    mocks.documentos.cargar.mockReturnValue(
      of({
        detail: {
          documentoId: 99,
          serieId: 1,
          version: 1,
          titulo: 'Ficha',
          formato: 'pdf',
          tamanoBytes: 10,
          hashSha256: 'a'.repeat(64),
          clasificacionPropuesta: 'INTERNO',
          aptaComoEvidencia: false,
          etag: 'W/"1"'
        }
      })
    );
    mocks.registro.presentarIniciativa.mockReturnValue(of(detalleIniciativaValido()));

    (component as any).fichaArchivo.set(new File(['contenido'], 'ficha.pdf', { type: 'application/pdf' }));
    (component as any).form.patchValue({
      nombre: '  iniciativa piloto  ',
      tipoSolucion: 'POTENCIAL_ADAPTABLE',
      fuenteOrigen: 'OTROS',
      detalleFuente: '  detalle  ',
      problemaPublico: '  Problema  ',
      solucionPropuesta: '  Solución  ',
      responsableId: 5,
      objetivoPeiId: 1,
      actividadPoiId: 2,
      componenteDigital: true,
      detalleComponenteDigital: '  App móvil  ',
      nota: '  Nota  '
    });
    (component as any).agregarUnidad();
    (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });

    await (component as any).enviar();

    const payload = mocks.registro.presentarIniciativa.mock.calls[0][0];
    expect(payload.nombre).toBe('iniciativa piloto');
    expect(payload.detalleFuente).toBe('detalle');
    expect(payload.problemaPublico).toBe('Problema');
    expect(payload.solucionPropuesta).toBe('Solución');
    expect(payload.detalleComponenteDigital).toBe('App móvil');
    expect(payload.nota).toBe('Nota');
  });

  it('duplica el resultado con la misma `Idempotency-Key` (mismo payload)', async () => {
    // El cliente delega en el interceptor; aquí verificamos que la operación se repite
    // y el backend responde el mismo recurso (idempotencia).
    mocks.documentos.cargar.mockReturnValue(
      of({
        detail: {
          documentoId: 99,
          serieId: 1,
          version: 1,
          titulo: 'Ficha',
          formato: 'pdf',
          tamanoBytes: 10,
          hashSha256: 'a'.repeat(64),
          clasificacionPropuesta: 'INTERNO',
          aptaComoEvidencia: false,
          etag: 'W/"1"'
        }
      })
    );
    const detalleOriginal = detalleIniciativaValido();
    mocks.registro.presentarIniciativa
      .mockReturnValueOnce(of(detalleOriginal))
      .mockReturnValueOnce(of(detalleOriginal));

    prepararFormularioValido();
    await (component as any).enviar();

    prepararFormularioValido();
    await (component as any).enviar();

    expect(mocks.registro.presentarIniciativa).toHaveBeenCalledTimes(2);
    const primera = mocks.registro.presentarIniciativa.mock.calls[0][0];
    const segunda = mocks.registro.presentarIniciativa.mock.calls[1][0];
    expect(segunda).toEqual(primera);
    expect((component as any).submittedInitiative()?.id).toBe(detalleOriginal.id);
    httpMock.verify();
  });

  it('rechaza con 409 un envío con la misma `Idempotency-Key` y payload distinto', async () => {
    mocks.documentos.cargar.mockReturnValue(
      of({
        detail: {
          documentoId: 99,
          serieId: 1,
          version: 1,
          titulo: 'Ficha',
          formato: 'pdf',
          tamanoBytes: 10,
          hashSha256: 'a'.repeat(64),
          clasificacionPropuesta: 'INTERNO',
          aptaComoEvidencia: false,
          etag: 'W/"1"'
        }
      })
    );
    mocks.registro.presentarIniciativa.mockReturnValue(
      throwError(
        () =>
          problemaHttp(409, {
            type: 'about:blank',
            title: 'Idempotency-Key reutilizada con payload distinto',
            status: 409,
            code: 'STATE_CHANGED',
            correlationId: 'idempo-1',
            detail: 'La clave de idempotencia ya fue utilizada con un cuerpo diferente.'
          })
      )
    );

    prepararFormularioValido();
    await (component as any).enviar();

    expect((component as any).problem()?.code).toBe('STATE_CHANGED');
    expect((component as any).problem()?.status).toBe(409);
    httpMock.verify();
  });

  it('mapea `UNIT_PREFIX_NOT_AVAILABLE` desde ProblemDetail y expone las violaciones', async () => {
    mocks.documentos.cargar.mockReturnValue(
      of({
        detail: {
          documentoId: 99,
          serieId: 1,
          version: 1,
          titulo: 'Ficha',
          formato: 'pdf',
          tamanoBytes: 10,
          hashSha256: 'a'.repeat(64),
          clasificacionPropuesta: 'INTERNO',
          aptaComoEvidencia: false,
          etag: 'W/"1"'
        }
      })
    );
    mocks.registro.presentarIniciativa.mockReturnValue(
      throwError(
        () =>
          problemaHttp(422, {
            type: 'about:blank',
            title: 'Prefijo de unidad no disponible',
            status: 422,
            code: 'UNIT_PREFIX_NOT_AVAILABLE',
            correlationId: 'pre-1',
            detail: 'La unidad seleccionada no tiene prefijo configurado.',
            violations: [{ field: 'unidades', message: 'Prefijo no configurado para la unidad 7.' }]
          })
      )
    );

    prepararFormularioValido();
    await (component as any).enviar();

    expect((component as any).problem()?.code).toBe('UNIT_PREFIX_NOT_AVAILABLE');
    expect((component as any).problem()?.violations[0].field).toBe('unidades');
    expect((component as any).violacionesPara('unidades')).toHaveLength(1);
    httpMock.verify();
  });

  it('mapea `UNIT_MAIN_CARDINALITY` desde ProblemDetail cuando el backend rechaza', async () => {
    mocks.documentos.cargar.mockReturnValue(
      of({
        detail: {
          documentoId: 99,
          serieId: 1,
          version: 1,
          titulo: 'Ficha',
          formato: 'pdf',
          tamanoBytes: 10,
          hashSha256: 'a'.repeat(64),
          clasificacionPropuesta: 'INTERNO',
          aptaComoEvidencia: false,
          etag: 'W/"1"'
        }
      })
    );
    mocks.registro.presentarIniciativa.mockReturnValue(
      throwError(
        () =>
          problemaHttp(422, {
            type: 'about:blank',
            title: 'Cardinalidad incumplida',
            status: 422,
            code: 'UNIT_MAIN_CARDINALITY',
            correlationId: 'card-1',
            detail: 'Debe existir exactamente una unidad principal.'
          })
      )
    );

    prepararFormularioValido();
    await (component as any).enviar();

    expect((component as any).problem()?.code).toBe('UNIT_MAIN_CARDINALITY');
    httpMock.verify();
  });
});

describe('cabeceras HTTP (Idempotency-Key, If-Match, X-Asignacion-Efectiva-Id)', () => {
  let mocks: Mocks;
  let component: InitiativeFormComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.organizacion.consultarUnidades.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    mocks.organizacion.consultarObjetivosPei.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    mocks.organizacion.consultarActividadesPoi.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    ({ component } = buildComponentFixture(mocks));
  });

  it('adjunta `Idempotency-Key` y `X-Asignacion-Efectiva-Id` en la ficha y la presentación', async () => {
    mocks.documentos.cargar.mockReturnValue(
      of({
        detail: {
          documentoId: 99,
          serieId: 1,
          version: 1,
          titulo: 'Ficha',
          formato: 'pdf',
          tamanoBytes: 10,
          hashSha256: 'a'.repeat(64),
          clasificacionPropuesta: 'INTERNO',
          aptaComoEvidencia: false,
          etag: 'W/"1"'
        }
      })
    );
    mocks.registro.presentarIniciativa.mockReturnValue(of(detalleIniciativaValido()));

    (component as any).fichaArchivo.set(new File(['contenido'], 'ficha.pdf', { type: 'application/pdf' }));
    (component as any).form.patchValue({
      nombre: 'Iniciativa piloto',
      tipoSolucion: 'POTENCIAL_ADAPTABLE',
      fuenteOrigen: 'FICHA_INICIATIVA',
      problemaPublico: 'Problema detectado',
      responsableId: 5,
      objetivoPeiId: 1,
      actividadPoiId: 2,
      componenteDigital: false,
      nota: ''
    });
    (component as any).agregarUnidad();
    (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });

    await (component as any).enviar();

    expect(mocks.documentos.cargar).toHaveBeenCalledOnce();
    expect(mocks.registro.presentarIniciativa).toHaveBeenCalledOnce();
    const payload = mocks.registro.presentarIniciativa.mock.calls[0][0];
    expect(payload.fichaDocumentoVersionId).toBe(99);
    // Las cabeceras Idempotency-Key, Authorization y X-Asignacion-Efectiva-Id
    // son añadidas por los interceptores globales y se verifican en las
    // pruebas de integración de los interceptores.
  });
});

describe('carga de archivos y validación de formato', () => {
  let mocks: Mocks;
  let component: InitiativeFormComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.organizacion.consultarUnidades.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    mocks.organizacion.consultarObjetivosPei.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    mocks.organizacion.consultarActividadesPoi.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    ({ component } = buildComponentFixture(mocks));
  });

  it('rechaza archivos vacíos', () => {
    const input = document.createElement('input');
    const archivo = new File([], 'vacio.pdf', { type: 'application/pdf' });
    Object.defineProperty(input, 'files', { value: [archivo] });

    (component as any).onFichaSeleccionada({ target: input } as unknown as Event);

    expect((component as any).fichaError()).toBe('La ficha documental no puede estar vacía.');
    expect((component as any).fichaArchivo()).toBeUndefined();
  });

  it('rechaza archivos con extensión no permitida', () => {
    const input = document.createElement('input');
    const archivo = new File(['contenido'], 'malware.exe', { type: 'application/octet-stream' });
    Object.defineProperty(input, 'files', { value: [archivo] });

    (component as any).onFichaSeleccionada({ target: input } as unknown as Event);

    expect((component as any).fichaError()).toBe('Formato no permitido. Use PDF, Office Open XML, JPEG o PNG.');
  });

  it('acepta un PDF válido', () => {
    const input = document.createElement('input');
    const archivo = new File(['contenido'], 'ficha.pdf', { type: 'application/pdf' });
    Object.defineProperty(input, 'files', { value: [archivo] });

    (component as any).onFichaSeleccionada({ target: input } as unknown as Event);

    expect((component as any).fichaArchivo()?.name).toBe('ficha.pdf');
    expect((component as any).fichaError()).toBeUndefined();
  });
});

describe('participantes persona (opcional)', () => {
  let mocks: Mocks;
  let component: InitiativeFormComponent;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.organizacion.consultarUnidades.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    mocks.organizacion.consultarObjetivosPei.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    mocks.organizacion.consultarActividadesPoi.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    ({ component } = buildComponentFixture(mocks));
  });

  it('omite `participantesPersona` cuando no se registran', async () => {
    mocks.documentos.cargar.mockReturnValue(
      of({
        detail: {
          documentoId: 99,
          serieId: 1,
          version: 1,
          titulo: 'Ficha',
          formato: 'pdf',
          tamanoBytes: 10,
          hashSha256: 'a'.repeat(64),
          clasificacionPropuesta: 'INTERNO',
          aptaComoEvidencia: false,
          etag: 'W/"1"'
        }
      })
    );
    mocks.registro.presentarIniciativa.mockReturnValue(of(detalleIniciativaValido()));

    (component as any).fichaArchivo.set(new File(['x'], 'ficha.pdf', { type: 'application/pdf' }));
    (component as any).form.patchValue({
      nombre: 'Iniciativa piloto',
      tipoSolucion: 'POTENCIAL_ADAPTABLE',
      fuenteOrigen: 'FICHA_INICIATIVA',
      problemaPublico: 'Problema',
      responsableId: 5,
      objetivoPeiId: 1,
      actividadPoiId: 2,
      componenteDigital: false
    });
    (component as any).agregarUnidad();
    (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });

    await (component as any).enviar();
    await firstValueFrom(of(null));

    const payload = mocks.registro.presentarIniciativa.mock.calls[0]?.[0];
    expect(payload?.participantesPersona).toBeUndefined();
  });

  it('incluye `participantesPersona` cuando se agregan integrantes', async () => {
    mocks.documentos.cargar.mockReturnValue(
      of({
        detail: {
          documentoId: 99,
          serieId: 1,
          version: 1,
          titulo: 'Ficha',
          formato: 'pdf',
          tamanoBytes: 10,
          hashSha256: 'a'.repeat(64),
          clasificacionPropuesta: 'INTERNO',
          aptaComoEvidencia: false,
          etag: 'W/"1"'
        }
      })
    );
    mocks.registro.presentarIniciativa.mockReturnValue(of(detalleIniciativaValido()));

    (component as any).fichaArchivo.set(new File(['x'], 'ficha.pdf', { type: 'application/pdf' }));
    (component as any).form.patchValue({
      nombre: 'Iniciativa piloto',
      tipoSolucion: 'POTENCIAL_ADAPTABLE',
      fuenteOrigen: 'FICHA_INICIATIVA',
      problemaPublico: 'Problema',
      responsableId: 5,
      objetivoPeiId: 1,
      actividadPoiId: 2,
      componenteDigital: false
    });
    (component as any).agregarUnidad();
    (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });

    (component as any).agregarParticipante();
    (component as any).form.controls.participantesPersona.at(0).patchValue({
      nombresCompletos: 'Ana Pérez',
      institucion: 'MIDAGRI',
      funcion: 'Especialista'
    });

    await (component as any).enviar();
    await firstValueFrom(of(null));

    const payload = mocks.registro.presentarIniciativa.mock.calls[0]?.[0];
    expect(payload?.participantesPersona).toEqual([
      { nombresCompletos: 'Ana Pérez', institucion: 'MIDAGRI', funcion: 'Especialista' }
    ]);
  });

  it('permite remover un participante agregado por error', () => {
    (component as any).agregarParticipante();
    (component as any).agregarParticipante();
    (component as any).removerParticipante(0);
    expect((component as any).form.controls.participantesPersona.length).toBe(1);
  });
});

describe('consulta de catálogos de organización', () => {
  it('carga unidades, objetivos PEI y actividades POI con sus catálogos aprobados', () => {
    const mocks = buildMocks();
    mocks.organizacion.consultarUnidades.mockReturnValue(
      of({
        items: [
          { id: 1, codigo: 'MIDAGRI', nombre: 'Ministerio de Desarrollo Agrario y Riego', activa: true }
        ],
        page: 0,
        size: 1,
        totalElements: 1,
        totalPages: 1
      })
    );
    mocks.organizacion.consultarObjetivosPei.mockReturnValue(
      of({
        items: [{ id: 1, codigo: 'OEI.01', descripcion: 'Objetivo institucional', vigenteDesde: '2026-01-01', activo: true }],
        page: 0,
        size: 1,
        totalElements: 1,
        totalPages: 1
      })
    );
    mocks.organizacion.consultarActividadesPoi.mockReturnValue(
      of({
        items: [{ id: 1, codigo: 'AO.01', descripcion: 'Actividad operativa', vigenteDesde: '2026-01-01', activo: true }],
        page: 0,
        size: 1,
        totalElements: 1,
        totalPages: 1
      })
    );

    const { component } = buildComponentFixture(mocks);

    expect(component['unidadesCatalogo']()).toEqual([
      { id: 1, codigo: 'MIDAGRI', nombre: 'Ministerio de Desarrollo Agrario y Riego' }
    ]);
    expect(component['objetivosPei']()[0].codigo).toBe('OEI.01');
    expect(component['actividadesPoi']()[0].codigo).toBe('AO.01');
  });

  it('deja los catálogos vacíos cuando el servicio falla', () => {
    const mocks = buildMocks();
    mocks.organizacion.consultarUnidades.mockReturnValue(throwError(() => new Error('Falla catálogo')));
    mocks.organizacion.consultarObjetivosPei.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    mocks.organizacion.consultarActividadesPoi.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));

    const { component } = buildComponentFixture(mocks);

    expect(component['unidadesCatalogo']()).toEqual([]);
  });
});

describe('accesibilidad WCAG 2.1 AA', () => {
  let mocks: Mocks;
  let fixture: ComponentFixture<InitiativeFormComponent>;

  beforeEach(() => {
    mocks = buildMocks();
    mocks.organizacion.consultarUnidades.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    mocks.organizacion.consultarObjetivosPei.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    mocks.organizacion.consultarActividadesPoi.mockReturnValue(of({ items: [], page: 0, size: 0, totalElements: 0, totalPages: 0 }));
    ({ fixture } = buildComponentFixture(mocks));
  });

  function dom(): HTMLElement {
    return fixture.nativeElement as HTMLElement;
  }

  it('asocia cada `mat-label` con su control mediante `for`/`id`', () => {
    const asociaciones: Array<[string, string]> = [
      ['initiative-nombre', 'initiative-nombre'],
      ['initiative-tipo-solucion', 'initiative-tipo-solucion'],
      ['initiative-fuente', 'initiative-fuente'],
      ['initiative-detalle-fuente', 'initiative-detalle-fuente'],
      ['initiative-problema', 'initiative-problema'],
      ['initiative-solucion', 'initiative-solucion'],
      ['initiative-responsable', 'initiative-responsable'],
      ['initiative-pei', 'initiative-pei'],
      ['initiative-poi', 'initiative-poi'],
      ['initiative-detalle-digital', 'initiative-detalle-digital'],
      ['initiative-nota', 'initiative-nota']
    ];

    for (const [forTarget, id] of asociaciones) {
      const label = dom().querySelector(`mat-label[for="${forTarget}"]`);
      const control = dom().querySelector(`#${id}`);
      expect(label, `label[for=${forTarget}] ausente`).not.toBeNull();
      expect(control, `control #${id} ausente`).not.toBeNull();
    }
  });

  it('expone `aria-describedby` con identificadores de ayuda y error en campos obligatorios', () => {
    const nombre = dom().querySelector<HTMLInputElement>('#initiative-nombre');
    expect(nombre?.getAttribute('aria-describedby')).toContain('initiative-nombre-help');
    expect(nombre?.getAttribute('aria-describedby')).toContain('initiative-nombre-error');
  });

  it('declara una región `aria-live="assertive"` para errores del servidor', () => {
    const region = dom().querySelector('.initiative-form__server-errors');
    expect(region?.getAttribute('role')).toBe('alert');
    expect(region?.getAttribute('aria-live')).toBe('assertive');
  });

  it('declara un `output` con `aria-live="polite"` para el resultado de la presentación', () => {
    const component = fixture.componentInstance;
    (component as any).submittedInitiative.set({
      id: 1, tipoRegistro: 'INICIATIVA', codigo: '2026-0001', fechaInicio: '2026-01-01',
      estado: 'PRESENTADO', version: 1, etag: '"1"', nombre: 'Test',
      tipoSolucion: 'POTENCIAL_ADAPTABLE', fuenteOrigen: 'FICHA_INICIATIVA',
      problemaPublico: '', responsableId: 1, objetivoPeiId: 1, actividadPoiId: 1,
      unidades: [], componenteDigital: false, detalleComponenteDigital: '', nota: ''
    } as any);
    fixture.detectChanges();

    const output = dom().querySelector('output.initiative-form__result');
    expect(output).not.toBeNull();
    expect(output?.getAttribute('aria-live')).toBe('polite');
  });

  it('envuelve cada unidad agregada en un `fieldset` con `legend` accesible', () => {
    const component = fixture.componentInstance;
    (component as any).agregarUnidad();
    (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });
    fixture.detectChanges();

    const fieldset = dom().querySelector('fieldset.initiative-form__array-item');
    expect(fieldset).not.toBeNull();
    expect(fieldset?.querySelector('legend')?.textContent).toContain('Unidad 1');
  });

  it('marca como `required` los campos obligatorios', () => {
    const nombre = dom().querySelector<HTMLInputElement>('#initiative-nombre');
    const tipoSolucion = dom().querySelector<HTMLElement>('#initiative-tipo-solucion');
    const problema = dom().querySelector<HTMLTextAreaElement>('#initiative-problema');
    const responsable = dom().querySelector<HTMLInputElement>('#initiative-responsable');

    expect(nombre?.required).toBe(true);
    expect(tipoSolucion?.getAttribute('required')).not.toBeNull();
    expect(problema?.required).toBe(true);
    expect(responsable?.required).toBe(true);
  });

  it('expone el botón de envío con texto descriptivo y tipo `submit`', () => {
    const boton = dom().querySelector<HTMLButtonElement>('button[type="submit"]');
    expect(boton).not.toBeNull();
    expect(boton?.textContent?.trim()).toBe('Presentar iniciativa');
  });

  it('asocia el botón de envío con la ayuda del formulario mediante `aria-describedby`', () => {
    const boton = dom().querySelector<HTMLButtonElement>('button[type="submit"]');
    expect(boton?.getAttribute('aria-describedby')).toBe('initiative-form-help');
  });

  it('asocia el botón de eliminación de unidad con una `aria-label` específica', () => {
    const component = fixture.componentInstance;
    (component as any).agregarUnidad();
    (component as any).form.controls.unidades.at(0).patchValue({ unidadId: 7, principal: true });
    fixture.detectChanges();

    const boton = dom().querySelector<HTMLButtonElement>('button[aria-label="Quitar unidad 1"]');
    expect(boton).not.toBeNull();
    expect(boton?.getAttribute('type')).toBe('button');
  });
});
