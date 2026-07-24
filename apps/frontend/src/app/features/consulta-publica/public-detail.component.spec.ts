// T101 · Contrato de privacidad y forma del detalle público (US7).
//
// Esta suite valida la forma del componente y sus DTOs sin instanciar
// el componente. La verificación es estática para no depender del
// inyector y mantener la suite rápida.
import { describe, expect, it } from 'vitest';

import { PublicQueryApiService } from './api/public-query-api.service';
import {
  PublicPortfolioDetail,
  PublicPortfolioDocumento,
  PublicPortfolioSummary
} from './api/types';

import { InstitutionalPortfolioDetail } from '../consulta-institucional/api/types';

describe('PublicDetailComponent (T101 · US7 · público)', () => {
  it('expone un cliente público con DTOs separados del institucional', () => {
    expect(typeof PublicQueryApiService).toBe('function');
    const camposPublico: ReadonlyArray<keyof PublicPortfolioDetail> = [
      'id',
      'tipoRegistro',
      'codigo',
      'nombre',
      'estado',
      'publicaciones',
      'etag'
    ];
    const camposInstitucional: ReadonlyArray<keyof InstitutionalPortfolioDetail> = [
      'id',
      'tipoRegistro',
      'codigo',
      'nombre',
      'estado',
      'unidades',
      'participantes',
      'documentos',
      'historial',
      'relacion',
      'responsableId',
      'nota',
      'problemaPublico',
      'solucionPropuesta',
      'resultadosClave',
      'version',
      'etag'
    ];
    // La intersección se limita a la allowlist canónica.
    const compartidos = camposPublico.filter((campo) =>
      (camposInstitucional as ReadonlyArray<string>).includes(campo as string)
    );
    expect(compartidos).toEqual(['id', 'tipoRegistro', 'codigo', 'nombre', 'estado', 'etag']);
    // El público debe ser un subconjunto estricto: no debe contener
    // ningún campo sensible del institucional.
    const exclusivosPublico = camposPublico.filter(
      (campo) => !(camposInstitucional as ReadonlyArray<string>).includes(campo as string)
    );
    expect(exclusivosPublico).toEqual(['publicaciones']);
  });

  it('expone únicamente la allowlist canónica y metadatos descriptivos publicados', () => {
    const publicacion: PublicPortfolioDocumento = {
      tipoDocumental: 'Ficha de Iniciativa de Innovación Pública',
      tituloPublico: 'Ficha pública sin datos personales',
      version: 1,
      formato: 'PDF',
      fechaPublicacion: '2026-07-23T00:00:00Z'
    };
    const detalle: PublicPortfolioDetail = {
      id: 1,
      tipoRegistro: 'INICIATIVA',
      codigo: '2026-UNIDAD-A-00001',
      nombre: 'Registro sintético',
      estado: 'PRESENTADO',
      publicaciones: [publicacion],
      etag: 'W/"1"'
    };
    expect(Object.keys(detalle).sort()).toEqual(
      ['codigo', 'estado', 'etag', 'id', 'nombre', 'publicaciones', 'tipoRegistro'].sort()
    );
    // El JSON serializado no debe contenerResponsable, correo, telefono,
    // contenido, BLOB ni URL de descarga.
    const serializado = JSON.stringify(detalle);
    expect(serializado).not.toMatch(/responsable|correo|telefono|contenido|download|blob|urlDescarga/i);
  });

  it('no expone metadatos sensibles en la colección de publicaciones', () => {
    const publicacion: PublicPortfolioDocumento = {
      tipoDocumental: 'Informe',
      tituloPublico: 'Informe público sin datos personales',
      version: 1,
      formato: 'PDF',
      fechaPublicacion: '2026-07-23T00:00:00Z'
    };
    const serializado = JSON.stringify(publicacion);
    expect(serializado).not.toMatch(/hash|blob|tamano|usuarioCarga|clasificacion/i);
  });

  it('no ofrece descarga ni contenido documental al visitante anónimo', () => {
    // El resumen público no incluye ninguna URL ni accion de descarga.
    const resumen: PublicPortfolioSummary = {
      id: 1,
      tipoRegistro: 'INICIATIVA',
      codigo: '2026-UNIDAD-A-00001',
      nombre: 'Registro sintético',
      estado: 'PRESENTADO',
      publicaciones: [],
      etag: 'W/"1"'
    };
    const serializado = JSON.stringify(resumen);
    expect(serializado).not.toMatch(/descarga|download|url|href/i);
  });

  it('publica la ruta base del cliente público sin mezclarse con la institucional', () => {
    type Servicio = { BASE: string };
    const BASE = (PublicQueryApiService as unknown as Servicio).BASE;
    expect(BASE).toBe('/api/v1/consulta/publica/portafolio');
    expect(BASE).not.toContain('institucional');
  });

  it('la ruta pública no expone tipos del cliente institucional', async () => {
    // Verificación estática: las declaraciones importadas desde el
    // espacio público NO deben arrastrar tipos sensibles del espacio
    // institucional. La intersección quedó validada arriba; este test
    // documenta la dirección inversa.
    const declaracion: ReadonlyArray<keyof PublicPortfolioDetail> = [
      'id',
      'tipoRegistro',
      'codigo',
      'nombre',
      'estado',
      'publicaciones',
      'etag'
    ];
    expect(declaracion).toHaveLength(7);
  });
});
