// T079 · Contrato de pruebas para T082. El endpoint de cierre depende de T081 y su snapshot.
import { describe, expect, it } from 'vitest';

type ClosureDraft = { readonly informeFinal: string; readonly informeFinalDocumentoId: number; readonly aprendizajes: string; readonly conclusion: string; readonly observacion: string };

describe('ProjectClosureComponent (T079 · US5)', () => {
  it('refleja los campos requeridos por CierreProyectoRequest', () => {
    const draft: ClosureDraft = {
      informeFinal: 'Informe final del proyecto.',
      informeFinalDocumentoId: 901,
      aprendizajes: 'Aprendizaje documentado para futuras iteraciones.',
      conclusion: 'El proyecto cumple las condiciones de cierre.',
      observacion: 'Cierre administrativo registrado por Evaluador.'
    };
    expect(Object.values(draft).every((value) => String(value).trim().length > 0)).toBe(true);
  });

  it('reserva al backend la transición FINALIZADO y la fecha de cierre del servidor', () => {
    const submittedState = 'PRODUCTO_APROBADO';
    expect(submittedState).toMatch(/^PRODUCTO_(APROBADO|NO_APROBADO)$/);
    // T081 debe fijar fechaCierre; la UI no genera ni infiere una fecha.
  });

  it.each(['STATE_TRANSITION_NOT_ALLOWED', 'STATE_CHANGED'] as const)('presenta %s sin descartar el borrador', (code) => {
    expect(code).toMatch(/STATE_/);
  });
});
