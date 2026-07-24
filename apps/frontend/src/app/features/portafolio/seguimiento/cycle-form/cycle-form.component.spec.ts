// T070 · Pruebas TDD de seguimiento (US4).
// T075 publica CycleFormComponent y SeguimientoApiService con las firmas usadas aquí.
// T076 debe exponer la ruta lazy que aloja el componente. Estos tests no implementan reglas de negocio:
// el backend conserva la autoridad para periodo, evidencias, concurrencia y estado.

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { SeguimientoApiService } from '../api/seguimiento-api.service';
import { CycleFormComponent } from './cycle-form.component';

interface SeguimientoApiMock {
  registrarCiclo: ReturnType<typeof vi.fn>;
  corregirCiclo: ReturnType<typeof vi.fn>;
  adjuntarEvidenciaCiclo: ReturnType<typeof vi.fn>;
}

const PROYECTO_ID = 101;
const CICLO_ID = 501;
const CICLO_CERRADO = {
  idCiclo: CICLO_ID,
  idProyecto: PROYECTO_ID,
  periodo: '2026-Q3-S1',
  numeroVersion: 1,
  objetivos: 'Validar el piloto.',
  actividades: 'Realizar sesiones de campo.',
  avance: 50,
  dificultades: 'Disponibilidad de participantes.',
  proximasAcciones: 'Reprogramar sesiones.',
  cerrado: 'S' as const,
  etag: '"ciclo-501-v1"'
};

function problem(status: number, code: string, detail: string) {
  return { status, error: { title: 'Operación rechazada', status, code, detail, correlationId: 'corr-t070' } };
}

function buildApiMock(): SeguimientoApiMock {
  return {
    registrarCiclo: vi.fn(),
    corregirCiclo: vi.fn(),
    adjuntarEvidenciaCiclo: vi.fn()
  };
}

describe('CycleFormComponent (T070 · US4)', () => {
  let fixture: ComponentFixture<CycleFormComponent>;
  let component: CycleFormComponent;
  let api: SeguimientoApiMock;

  beforeEach(() => {
    api = buildApiMock();
    TestBed.configureTestingModule({
      imports: [CycleFormComponent],
      providers: [provideNoopAnimations(), { provide: SeguimientoApiService, useValue: api }]
    });
    fixture = TestBed.createComponent(CycleFormComponent);
    component = fixture.componentInstance;
    // T075 recibe el proyecto mediante input; T076 aportará su valor desde la ruta.
    (component as any).projectId = PROYECTO_ID;
    fixture.detectChanges();
  });

  afterEach(() => {
    TestBed.resetTestingModule();
    vi.restoreAllMocks();
  });

  it('declara periodo quincenal, objetivos, actividades, avance, dificultades y próximas acciones', () => {
    expect((component as any).form.controls.periodo).toBeDefined();
    expect((component as any).form.controls.objetivos).toBeDefined();
    expect((component as any).form.controls.actividades).toBeDefined();
    expect((component as any).form.controls.avance).toBeDefined();
    expect((component as any).form.controls.dificultades).toBeDefined();
    expect((component as any).form.controls.proximasAcciones).toBeDefined();
  });

  it.each(['2026-Q1-S1', '2026-Q1-S2', '2026-Q4-S2'])('acepta el periodo quincenal canónico %s', (periodo) => {
    (component as any).form.controls.periodo.setValue(periodo);
    expect((component as any).form.controls.periodo.valid).toBe(true);
  });

  it.each(['2026-Q0-S1', '2026-Q5-S1', '2026-Q1-S3', '2026-01-15'])('rechaza periodo no quincenal %s', (periodo) => {
    (component as any).form.controls.periodo.setValue(periodo);
    expect((component as any).form.controls.periodo.hasError('pattern')).toBe(true);
  });

  it.each([-1, 101])('rechaza avance fuera del intervalo 0..100: %i', (avance) => {
    (component as any).form.controls.avance.setValue(avance);
    expect((component as any).form.controls.avance.invalid).toBe(true);
  });

  it.each([0, 100])('acepta los límites inclusivos de avance: %i', (avance) => {
    (component as any).form.controls.avance.setValue(avance);
    expect((component as any).form.controls.avance.valid).toBe(true);
  });

  it('permite guardar un ciclo completo sin evidencia, porque es opcional', async () => {
    api.registrarCiclo.mockReturnValue(of(CICLO_CERRADO));
    (component as any).form.setValue({
      periodo: '2026-Q3-S1', objetivos: 'Validar el piloto.', actividades: 'Realizar sesiones de campo.',
      avance: 50, dificultades: 'Disponibilidad de participantes.', proximasAcciones: 'Reprogramar sesiones.'
    });

    await (component as any).submit();

    expect(api.registrarCiclo).toHaveBeenCalledWith(PROYECTO_ID, expect.objectContaining({
      periodo: '2026-Q3-S1', avance: 50
    }));
    expect((component as any).savedCycle()).toEqual(CICLO_CERRADO);
  });

  it('anexa evidencia solo cuando el documento ya fue declarado apto por backend', async () => {
    api.registrarCiclo.mockReturnValue(of(CICLO_CERRADO));
    api.adjuntarEvidenciaCiclo.mockReturnValue(of(void 0));
    (component as any).form.setValue({
      periodo: '2026-Q3-S1', objetivos: 'Objetivo', actividades: 'Actividad', avance: 0,
      dificultades: '', proximasAcciones: ''
    });
    (component as any).selectEvidence({ idDocumento: 700, tipoDocumental: 'Seguimiento Ágil, Tablero Kanban', aptaComoEvidencia: true });

    await (component as any).submit();

    expect(api.adjuntarEvidenciaCiclo).toHaveBeenCalledWith(PROYECTO_ID, CICLO_ID, {
      idDocumento: 700, tipoDocumental: 'Seguimiento Ágil, Tablero Kanban'
    });
  });

  it('bloquea en UX una evidencia marcada como no apta, sin decidir su elegibilidad', () => {
    (component as any).selectEvidence({ idDocumento: 701, tipoDocumental: 'Otro', aptaComoEvidencia: false });
    expect((component as any).evidenceError()).toContain('no es apta');
    expect((component as any).selectedEvidence()).toBeUndefined();
  });

  it('corrige un ciclo cerrado mediante una nueva versión con motivo, sin sobrescribir la versión previa', async () => {
    api.corregirCiclo.mockReturnValue(of({ ...CICLO_CERRADO, numeroVersion: 2, idVersionAnterior: CICLO_ID, avance: 60 }));
    (component as any).startCorrection(CICLO_CERRADO);
    (component as any).correctionForm.setValue({
      motivo: 'Se corrigió el avance con evidencia posterior.', objetivos: 'Validar el piloto.',
      actividades: 'Realizar sesiones de campo.', avance: 60,
      dificultades: 'Disponibilidad de participantes.', proximasAcciones: 'Reprogramar sesiones.'
    });

    await (component as any).submitCorrection();

    expect(api.corregirCiclo).toHaveBeenCalledWith(PROYECTO_ID, CICLO_ID, expect.objectContaining({ motivo: expect.any(String), avance: 60 }));
    expect((component as any).savedCycle()?.numeroVersion).toBe(2);
    expect(CICLO_CERRADO.numeroVersion).toBe(1);
  });

  it('exige motivo antes de crear una corrección append-only', () => {
    (component as any).startCorrection(CICLO_CERRADO);
    (component as any).correctionForm.patchValue({ objetivos: 'Objetivo', actividades: 'Actividad', avance: 20 });
    expect((component as any).correctionForm.controls.motivo.hasError('required')).toBe(true);
  });

  it.each([
    [409, 'CYCLE_DUPLICATED'], [409, 'CYCLE_VERSION_CONFLICT'], [412, 'STATE_CHANGED'], [422, 'CYCLE_INCOMPLETE'], [422, 'CYCLE_AVANCE_OUT_OF_RANGE']
  ])('muestra ProblemDetail accesible para %i %s', async (status, code) => {
    api.registrarCiclo.mockReturnValue(throwError(() => problem(status, code, 'El ciclo no puede registrarse.')));
    (component as any).form.setValue({ periodo: '2026-Q3-S1', objetivos: 'Objetivo', actividades: 'Actividad', avance: 30, dificultades: '', proximasAcciones: '' });

    await (component as any).submit();

    expect((component as any).problem()?.status).toBe(status);
    expect((component as any).problem()?.code).toBe(code);
  });

  it('cancela la corrección sin enviar una mutación', () => {
    (component as any).startCorrection(CICLO_CERRADO);
    (component as any).cancelCorrection();
    expect(api.corregirCiclo).not.toHaveBeenCalled();
    expect((component as any).correctingCycle()).toBeUndefined();
  });

  it('cumple el contrato de teclado y WCAG AA: labels, errores y región de estado', () => {
    const dom = fixture.nativeElement as HTMLElement;
    expect(dom.querySelector('h2#cycle-form-title')).not.toBeNull();
    expect(dom.querySelector('label[for="cycle-periodo"]')).not.toBeNull();
    expect(dom.querySelector('#cycle-periodo')?.getAttribute('aria-describedby')).toContain('cycle-periodo-error');
    const alertRegion = dom.querySelector('[role="alert"]');
    if (alertRegion) {
      expect(alertRegion.getAttribute('aria-live')).toBe('assertive');
    } else {
      // Fallback: buscar una región aria-live="assertive"
      expect(dom.querySelector('[aria-live="assertive"]')).not.toBeNull();
    }
    expect(dom.querySelector('output[aria-live="polite"]')).not.toBeNull();
    expect(dom.querySelector<HTMLButtonElement>('button[type="submit"]')?.textContent).toContain('Registrar ciclo');
  });
});
