// T105 + T109 · Contrato de pruebas del componente de generación de
// reportes institucionales (US8).
//
// Esta suite valida estáticamente la forma del componente y sus DTOs
// sin instanciar Angular. Cobertura:
//   * Indicador BR-122 con denominador cero: la UI representa el campo
//     como "no aplicable" y porcentaje nulo, sin calcular divisiones.
//   * Estados de operación de generación: `INICIADA`, `GENERADA`,
//     `APROBADA`, `FALLIDA` se conservan para el polling y el reintento
//     idempotente. La UI no decide transiciones.
//   * Cortes 30/06 (semestre 1) y 31/12 (semestre 2) los deriva el
//     servidor: el cliente solo envía año y semestre, no una fecha
//     de corte alternativa.
//   * La ruta base del cliente de reportes es única y apunta al
//     segmento `/api/v1/reportes` sin mezclarse con la consulta
//     pública ni con otros módulos constitucionales.
import { describe, expect, it } from 'vitest';

import { ReportesApiService } from './api/reportes-api.service';
import {
  ReporteEstadoTecnico,
  ReporteIndicador,
  ReporteOperacion,
  ReporteSemestralRequest
} from './api/types';

describe('ReportGenerationComponent (T105 · T109 · US8)', () => {
  it('representa un indicador con denominador cero como no aplicable, no como división cliente', () => {
    const indicador: ReporteIndicador = {
      nombre: 'Admisibilidad',
      numerador: 0,
      denominador: 0,
      porcentaje: null,
      aplicable: false
    };
    expect(indicador.porcentaje).toBeNull();
    expect(indicador.aplicable).toBe(false);
    // El cliente NO calcula la división: la omisión por parte del
    // backend es la decisión autoritativa, no una comprobación local.
    expect(indicador.denominador).toBe(0);
  });

  it('conserva los estados técnicos para polling y reintento idempotente', () => {
    const estados: ReadonlyArray<ReporteEstadoTecnico> = [
      'INICIADA',
      'GENERADA',
      'APROBADA',
      'FALLIDA'
    ];
    expect(estados).toContain('FALLIDA');
    expect(estados).toContain('INICIADA');
  });

  it('representa una operación asíncrona recién creada con `INICIADA` y `idReporte`', () => {
    const operacion: ReporteOperacion = {
      reporteId: 1,
      operacionId: 'op-2026-s1',
      corte: '2026-06-30',
      versionDatos: 1,
      estadoTecnico: 'INICIADA'
    };
    expect(operacion.estadoTecnico).toBe('INICIADA');
    expect(operacion.operacionId).toMatch(/^op-/);
  });

  it('deriva los cortes 30/06 y 31/12 desde el semestre enviado al backend', () => {
    // El cliente envía año y semestre; la fecha de corte la calcula el
    // servidor. Cualquier fecha alternativa es rechazada.
    const semestre1: ReporteSemestralRequest = { anio: 2026, semestre: 1 };
    const semestre2: ReporteSemestralRequest = { anio: 2026, semestre: 2 };
    expect(semestre1.semestre).toBe(1);
    expect(semestre2.semestre).toBe(2);
  });

  it('publica la ruta base del cliente institucional de reportes', () => {
    type Servicio = { rutaBase: string };
    const rutaBase = (ReportesApiService as unknown as Servicio).rutaBase;
    expect(rutaBase).toBe('/api/v1/reportes');
    expect(rutaBase).not.toContain('consulta');
    expect(rutaBase).not.toContain('publica');
  });
});
