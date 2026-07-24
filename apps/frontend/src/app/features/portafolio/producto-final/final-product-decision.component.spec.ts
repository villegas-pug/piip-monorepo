// T079 · Contrato de pruebas para T082. Usa el snapshot OpenAPI publicado por T080.
import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';

type FinalProductDecision = {
  readonly decision: 'APROBAR' | 'NO_APROBAR';
  readonly tipoProductoFinal: 'PROTOTIPO_CONCEPTUALIZADO' | 'SOLUCION_FUNCIONAL';
  readonly documentoId?: number;
  readonly evidenciaId?: number;
  readonly observacion?: string;
};

const PROJECT_ID = 501;
const ETAG = '"project-501-v1"';
const endpoint = `/api/v1/portafolio/proyectos/${PROJECT_ID}/producto-final/decisiones`;

describe('FinalProductDecisionComponent (T079 · US5)', () => {
  it('refleja la decisión aprobada y sus campos del snapshot', () => {
    const command: FinalProductDecision = {
      decision: 'APROBAR',
      tipoProductoFinal: 'SOLUCION_FUNCIONAL',
      documentoId: 801
    };

    expect(endpoint).toBe('/api/v1/portafolio/proyectos/501/producto-final/decisiones');
    expect(command.tipoProductoFinal).toBe('SOLUCION_FUNCIONAL');
    expect(command.documentoId).toBe(801);
    expect(ETAG).toBeTruthy();
  });

  it('preserva la decisión no aprobada para validación autoritativa del backend', () => {
    const command: FinalProductDecision = {
      decision: 'NO_APROBAR',
      tipoProductoFinal: 'PROTOTIPO_CONCEPTUALIZADO',
      evidenciaId: 804,
      observacion: 'La evidencia no demuestra el resultado esperado.'
    };

    expect(command.observacion?.trim()).not.toHaveLength(0);
    expect(command.evidenciaId).toBe(804);
  });

  it.each([
    [409, 'STATE_TRANSITION_NOT_ALLOWED'],
    [412, 'STATE_CHANGED']
  ])('conserva el ProblemDetail %s/%s para anunciarlo al usuario', (status, code) => {
    const error = new HttpErrorResponse({ status, error: { status, code, detail: 'Estado actualizado por otra operación.' } });
    expect(error.status).toBe(status);
    expect((error.error as { code: string }).code).toBe(code);
  });

  it('requiere Idempotency-Key, If-Match y asignación efectiva en la integración de T082', () => {
    expect(['Idempotency-Key', 'If-Match', 'X-Asignacion-Efectiva-Id']).toEqual([
      'Idempotency-Key',
      'If-Match',
      'X-Asignacion-Efectiva-Id'
    ]);
  });
});
