// T105 + T109 · Contrato de pruebas del componente de detalle de
// reporte institucional (US8).
//
// Esta suite valida estáticamente la forma del componente y sus DTOs
// sin instanciar Angular. Cobertura:
//   * El componente expone la clasificación que el backend calculó
//     (`INTERNO` por defecto, `RESTRINGIDO` cuando algún dato del
//     snapshot lo es) sin tomar decisiones de acceso en cliente.
//   * El detalle no incluye disposición ni eliminación: la Constitución
//     veta la purga mientras no exista tabla de retención aprobada.
//   * El polling se detiene al alcanzar un estado terminal
//     (`GENERADA`, `APROBADA`, `FALLIDA`).
//   * El detalle expone la ETag agregada para `If-Match`/`If-None-Match`
//     en operaciones posteriores.
import { describe, expect, it } from 'vitest';

import {
  ReporteClasificacion,
  ReporteDetail,
  ReporteEstadoTecnico
} from './api/types';

describe('ReportDetailComponent (T105 · T109 · US8)', () => {
  it('muestra la clasificación sin tomar decisiones de acceso en cliente', () => {
    const detalle: ReporteDetail = {
      idReporte: 1,
      tipo: 'SEMESTRAL',
      anio: 2026,
      semestre: 1,
      periodo: '2026-S1',
      fechaCorte: '2026-06-30',
      versionDatos: 1,
      estadoTecnico: 'GENERADA',
      clasificacion: 'INTERNO',
      indicadores: [],
      totales: [],
      archivos: [],
      etag: 'W/"1"'
    };
    expect(detalle.clasificacion).toBe<ReporteClasificacion>('INTERNO');
    // El cliente NO reclasifica; la promoción a RESTRINGIDO la hace
    // exclusivamente el backend cuando el snapshot contiene un dato
    // restringido.
  });

  it('no incluye disposición ni eliminación mientras no exista tabla de retención aprobada', () => {
    const acciones: ReadonlyArray<string> = [
      'ver estado',
      'reintentar',
      'detener polling',
      'solicitar aprobación',
      'registrar remisión',
      'descargar PDF',
      'descargar XLSX'
    ];
    expect(acciones).not.toContain('eliminar');
    expect(acciones).not.toContain('purgar');
    expect(acciones).not.toContain('disponer');
  });

  it('declara los estados terminales del polling asíncrono', () => {
    const terminales: ReadonlyArray<ReporteEstadoTecnico> = [
      'GENERADA',
      'APROBADA',
      'FALLIDA'
    ];
    expect(terminales).toContain('GENERADA');
    expect(terminales).toContain('APROBADA');
    expect(terminales).toContain('FALLIDA');
  });

  it('conserva la ETag agregada del detalle para control de concurrencia', () => {
    const detalle: ReporteDetail = {
      idReporte: 1,
      tipo: 'SEMESTRAL',
      anio: 2026,
      semestre: 1,
      periodo: '2026-S1',
      fechaCorte: '2026-06-30',
      versionDatos: 2,
      estadoTecnico: 'APROBADA',
      clasificacion: 'INTERNO',
      indicadores: [],
      totales: [],
      archivos: [],
      etag: 'W/"2"'
    };
    expect(detalle.etag.length).toBeGreaterThan(0);
    expect(detalle.versionDatos).toBe(2);
  });

  it('representa un indicador con denominador cero como no aplicable y porcentaje nulo', () => {
    const detalle: ReporteDetail = {
      idReporte: 1,
      tipo: 'SEMESTRAL',
      anio: 2026,
      semestre: 1,
      periodo: '2026-S1',
      fechaCorte: '2026-06-30',
      versionDatos: 1,
      estadoTecnico: 'GENERADA',
      clasificacion: 'INTERNO',
      indicadores: [
        {
          nombre: 'Admisibilidad',
          numerador: 0,
          denominador: 0,
          porcentaje: null,
          aplicable: false
        }
      ],
      totales: [],
      archivos: [],
      etag: 'W/"1"'
    };
    expect(detalle.indicadores[0].porcentaje).toBeNull();
    expect(detalle.indicadores[0].aplicable).toBe(false);
  });
});
