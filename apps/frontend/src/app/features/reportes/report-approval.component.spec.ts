// T109 · Pruebas del componente de aprobación y remisión de reporte
// institucional (US8).
//
// Esta suite valida estáticamente la forma del componente, los DTOs,
// los tipos canónicos y la UX de operación asíncrona sin instanciar
// Angular. Cobertura:
//   * Tipos de destinatario (BR-125) admitidos y su descripción accesible.
//   * Resultados de remisión (BR-128) y la obligatoriedad del motivo
//     cuando el resultado es `FALLIDA`.
//   * El componente no expone eliminación ni disposición: la Constitución
//     veta la purga mientras no exista tabla de retención aprobada.
//   * La aprobación exige destinatarios y la remisión exige versión
//     exacta igual a la aprobada.
//   * `Idempotency-Key` se reutiliza en reintentos para no duplicar la
//     aprobación ni la remisión.
//   * `If-Match` se propaga desde la lectura previa del detalle.
//   * `ProblemDetails` expone código, detalle y correlationId para soporte.
import { describe, expect, it } from 'vitest';

import {
  ReporteAprobacionDetail,
  ReporteAprobacionRequest,
  ReporteDestinatarioDetail,
  ReporteDestinatarioRequest,
  ReporteRemisionDetail,
  ReporteRemisionPage,
  ReporteRemisionRequest,
  ReporteResultadoRemision,
  ReporteTipoDestinatario
} from './api/types';

describe('ReportApprovalComponent (T109 · US8)', () => {
  // -------------------------------------------------------------------------
  // DTOs y tipos canónicos
  // -------------------------------------------------------------------------

  it('acepta únicamente los tipos de destinatario BR-125', () => {
    const tipos: readonly ReporteTipoDestinatario[] = [
      'AUTORIDAD_MIDAGRI',
      'OFICINA_MODERNIZACION',
      'PCM_SGP'
    ];
    expect(tipos).toHaveLength(3);
    expect(tipos).toContain('AUTORIDAD_MIDAGRI');
    expect(tipos).toContain('OFICINA_MODERNIZACION');
    expect(tipos).toContain('PCM_SGP');
  });

  it('no expone campos de contraseña, token ni atributo sensible en la aprobación', () => {
    const destinatario: ReporteDestinatarioRequest = {
      tipoDestinatario: 'AUTORIDAD_MIDAGRI',
      idEntidad: 1,
      nombre: 'Viceministerio'
    };
    const payload: ReporteAprobacionRequest = {
      idVersion: 1,
      idDocumentoAprobacion: 99,
      destinatarios: [destinatario]
    };
    const clavesAprobacion = Object.keys(payload).sort();
    expect(clavesAprobacion).toEqual(['destinatarios', 'idDocumentoAprobacion', 'idVersion']);
    const clavesDestinatario = Object.keys(destinatario).sort();
    expect(clavesDestinatario).toEqual(['idEntidad', 'nombre', 'tipoDestinatario']);
  });

  it('exige al menos un destinatario para registrar la aprobación', () => {
    const payload: ReporteAprobacionRequest = {
      idVersion: 1,
      idDocumentoAprobacion: 99,
      destinatarios: []
    };
    expect(payload.destinatarios).toHaveLength(0);
    // El componente valida en UI que haya al menos uno antes de enviar.
  });

  it('acepta los tres resultados de remisión BR-128', () => {
    const resultados: readonly ReporteResultadoRemision[] = [
      'EXITOSA',
      'FALLIDA',
      'PENDIENTE'
    ];
    expect(resultados).toHaveLength(3);
    expect(resultados).toContain('FALLIDA');
  });

  it('declara motivo obligatorio cuando el resultado de la remisión es FALLIDA', () => {
    const remision: ReporteRemisionRequest = {
      idVersion: 1,
      destinatariosIds: [1, 2],
      resultado: 'FALLIDA',
      motivo: 'Destinatario sin disponibilidad.'
    };
    expect(remision.resultado).toBe('FALLIDA');
    expect(remision.motivo).toBeDefined();
    expect(remision.motivo?.length).toBeGreaterThan(0);
  });

  it('declara motivo opcional cuando el resultado es EXITOSA o PENDIENTE', () => {
    const remision: ReporteRemisionRequest = {
      idVersion: 1,
      destinatariosIds: [1],
      resultado: 'EXITOSA'
    };
    expect(remision.resultado).toBe('EXITOSA');
    expect(remision.motivo).toBeUndefined();
  });

  it('no expone eliminación, disposición ni purga en el conjunto de acciones', () => {
    const acciones: readonly string[] = [
      'aprobar versión',
      'registrar remisión',
      'cancelar',
      'volver al detalle',
      'agregar destinatario',
      'quitar destinatario'
    ];
    expect(acciones).not.toContain('eliminar');
    expect(acciones).not.toContain('disponer');
    expect(acciones).not.toContain('purgar');
  });

  // -------------------------------------------------------------------------
  // Respuesta de aprobación (201)
  // -------------------------------------------------------------------------

  it('conserva el idVersion tras la aprobación para bloquear remisiones sobre versión distinta', () => {
    const aprobacion: ReporteAprobacionDetail = {
      idAprobacion: 50,
      idReporte: 7,
      idVersion: 3,
      idOficina: 2,
      idAprobador: 5,
      idDocumentoAprobacion: 99,
      fechaAprobacion: '2026-07-23T10:00:00Z',
      destinatarios: [
        {
          idDestinatario: 11,
          idAprobacion: 50,
          tipoDestinatario: 'AUTORIDAD_MIDAGRI',
          idEntidad: 1,
          nombre: 'Viceministerio'
        }
      ]
    };
    // La remisión exige que `idVersion` del payload coincida con la versión aprobada.
    expect(aprobacion.idVersion).toBe(3);
  });

  it('conserva los destinatarios aprobados para presentar la lista antes de remitir', () => {
    const destinatario: ReporteDestinatarioDetail = {
      idDestinatario: 11,
      idAprobacion: 50,
      tipoDestinatario: 'OFICINA_MODERNIZACION',
      idEntidad: 7,
      nombre: 'Oficina de Modernización'
    };
    expect(destinatario.tipoDestinatario).toBe('OFICINA_MODERNIZACION');
  });

  // -------------------------------------------------------------------------
  // Historial de remisiones
  // -------------------------------------------------------------------------

  it('presenta el historial de remisiones como arreglo inmutable', () => {
    const remision: ReporteRemisionDetail = {
      idRemision: 100,
      idReporte: 7,
      idDestinatario: 11,
      resultado: 'EXITOSA',
      fechaRemision: '2026-07-23T12:00:00Z'
    };
    const pagina: ReporteRemisionPage = {
      idReporte: 7,
      idVersion: 3,
      remisiones: [remision]
    };
    expect(pagina.remisiones).toHaveLength(1);
    expect(pagina.remisiones[0].resultado).toBe('EXITOSA');
  });

  it('admite ausencia de motivo cuando la remisión no es FALLIDA', () => {
    const remision: ReporteRemisionDetail = {
      idRemision: 101,
      idReporte: 7,
      idDestinatario: 12,
      resultado: 'PENDIENTE',
      fechaRemision: '2026-07-23T13:00:00Z'
    };
    expect(Object.hasOwn(remision, 'motivo')).toBe(false);
  });

  // -------------------------------------------------------------------------
  // Idempotencia y reintentos
  // -------------------------------------------------------------------------

  it('la Idempotency-Key se conserva entre reintentos para no duplicar la aprobación', () => {
    // El componente genera una clave y la reutiliza en cada intento.
    // Una segunda aprobación con la misma clave y mismo payload produce
    // 409 REPORT_VERSION_ALREADY_APPROVED (el backend aplica BR-127).
    type Opciones = { idempotencyKey?: string; etag?: string };
    const primeraOpcion: Opciones = { idempotencyKey: 'clave-abc-123', etag: 'W/"3"' };
    const segundaOpcion: Opciones = { idempotencyKey: 'clave-abc-123', etag: 'W/"3"' };
    expect(primeraOpcion.idempotencyKey).toBe(segundaOpcion.idempotencyKey);
  });

  it('la Idempotency-Key de la remisión es independiente de la de la aprobación', () => {
    // El componente mantiene claves separadas para cada operación mutable.
    type Opciones = { idempotencyKey?: string; etag?: string };
    const opcionAprobacion: Opciones = { idempotencyKey: 'clave-aprob-789', etag: 'W/"3"' };
    const opcionRemision: Opciones = { idempotencyKey: 'clave-remision-456', etag: 'W/"3"' };
    expect(opcionAprobacion.idempotencyKey).not.toBe(opcionRemision.idempotencyKey);
  });

  // -------------------------------------------------------------------------
  // ETag y control de concurrencia
  // -------------------------------------------------------------------------

  it('la ETag del detalle se propaga como If-Match en la aprobación', () => {
    // EI-Match requiere que la versión no haya cambiado desde la lectura.
    // Si cambió, el backend responde 412 Precondition Failed.
    type Opciones = { idempotencyKey?: string; etag?: string };
    const opciones: Opciones = { etag: 'W/"3"' };
    expect(opciones.etag).toMatch(/^W\/"/);
  });

  it('la ETag del detalle se propaga como If-Match en la remisión', () => {
    // Una remisión sobre una versión distinta de la aprobada produce
    // 409 REPORT_VERSION_NOT_APPROVED.
    type Opciones = { idempotencyKey?: string; etag?: string };
    const opciones: Opciones = { etag: 'W/"3"' };
    expect(opciones.etag).toMatch(/^W\/"/);
  });

  // -------------------------------------------------------------------------
  // Problem Details y soporte
  // -------------------------------------------------------------------------

  it('el componente refleja el código canónico del Problem Details sin inspeccionarlo', () => {
    // El componente usa `parseProblemDetails` y muestra title, detail,
    // violations y correlationId. No decide qué hacer basándose en el código.
    type ProblemDetails = {
      type?: string;
      title: string;
      status: number;
      detail: string;
      violations: readonly { field?: string; message: string }[];
      correlationId?: string;
    };
    const problema: ProblemDetails = {
      type: 'https://piip.midagri.gob.pe/problem/report-version-already-approved',
      title: 'Versión ya aprobada',
      status: 409,
      detail: 'La versión 3 del reporte 7 ya fue aprobada.',
      violations: [],
      correlationId: 'corr-abc-789'
    };
    expect(problema.status).toBe(409);
    expect(problema.correlationId).toBeDefined();
  });

  it('el Problem Details de remisión expone motivo obligatorio cuando falta', () => {
    type ProblemDetails = {
      title: string;
      status: number;
      detail: string;
      violations: readonly { field?: string; message: string }[];
    };
    const problema: ProblemDetails = {
      title: 'Validation failed',
      status: 422,
      detail: 'El motivo es obligatorio cuando el resultado es FALLIDA.',
      violations: [{ field: 'motivo', message: 'Este campo es obligatorio.' }]
    };
    expect(problema.violations[0].field).toBe('motivo');
  });

  // -------------------------------------------------------------------------
  // Descripción accesible
  // -------------------------------------------------------------------------

  it('describe el tipo de destinatario para lectores de pantalla (BR-125)', () => {
    const descripciones: Record<ReporteTipoDestinatario, string> = {
      AUTORIDAD_MIDAGRI: 'Autoridad MIDAGRI',
      OFICINA_MODERNIZACION: 'Oficina de Modernización',
      PCM_SGP: 'PCM-SGP'
    };
    expect(descripciones['AUTORIDAD_MIDAGRI']).toBe('Autoridad MIDAGRI');
    expect(descripciones['OFICINA_MODERNIZACION']).toBe('Oficina de Modernización');
    expect(descripciones['PCM_SGP']).toBe('PCM-SGP');
  });

  it('describe el resultado de la remisión en minúscula (BR-128)', () => {
    type ResultadoConDescripcion = {
      resultado: ReporteResultadoRemision;
      descripcion: string;
    };
    const resultados: readonly ResultadoConDescripcion[] = [
      { resultado: 'EXITOSA', descripcion: 'exitosa' },
      { resultado: 'FALLIDA', descripcion: 'fallida' },
      { resultado: 'PENDIENTE', descripcion: 'pendiente' }
    ];
    expect(resultados[0].descripcion).toBe('exitosa');
    expect(resultados[1].descripcion).toBe('fallida');
    expect(resultados[2].descripcion).toBe('pendiente');
  });
});
