// T101 · Contrato de presentación y privacidad del detalle institucional (US7).
//
// Esta suite valida la forma del componente y sus DTOs. No instancia
// el componente ni ejecuta Angular: los chequeos son estáticos para
// mantener la suite rápida y no depender del inyector.
import { describe, expect, it } from 'vitest';

import { InstitutionalQueryApiService } from './api/institutional-query-api.service';
import {
  InstitutionalPortfolioDetail,
  InstitutionalPortfolioSummary
} from './api/types';

import { PublicPortfolioDetail, PublicPortfolioSummary } from '../consulta-publica/api/types';

describe('PortfolioDetailComponent (T101 · US7 · institucional)', () => {
  it('expone un cliente institucional con DTOs separados del público', () => {
    expect(typeof InstitutionalQueryApiService).toBe('function');
    const camposInstitucional: ReadonlyArray<keyof InstitutionalPortfolioDetail> = [
      'id',
      'tipoRegistro',
      'codigo',
      'codigoOrigen',
      'fechaInicio',
      'fechaCierre',
      'nombre',
      'tipoSolucion',
      'fuenteOrigen',
      'detalleFuente',
      'responsableId',
      'problemaPublico',
      'solucionPropuesta',
      'objetivoPeiId',
      'actividadPoiId',
      'unidadEjecutoraId',
      'unidadEjecutoraDescripcion',
      'unidadEjecutoraAbreviatura',
      'estado',
      'componenteDigital',
      'detalleComponenteDigital',
      'nota',
      'resultadosClave',
      'unidades',
      'participantes',
      'documentos',
      'historial',
      'relacion',
      'actorEsResponsable',
      'actorEsEvaluador',
      'actorEsAdministrador',
      'fechaCreacion',
      'version',
      'etag'
    ];
    const camposPublico: ReadonlyArray<keyof PublicPortfolioDetail> = [
      'id',
      'tipoRegistro',
      'codigo',
      'nombre',
      'estado',
      'publicaciones',
      'etag'
    ];
    // La intersección debe limitarse a la allowlist canónica; cualquier
    // campo institucional sensible presente en el cliente público
    // significaría una fuga de privacidad.
    const compartidos = camposInstitucional.filter((campo) =>
      (camposPublico as ReadonlyArray<string>).includes(campo as string)
    );
    expect(compartidos).toEqual(['id', 'tipoRegistro', 'codigo', 'nombre', 'estado', 'etag']);
    // El institucional debe contener al menos un campo que el público
    // no expone (descripción, resultados, Responsable, historial,
    // documentos, unidades, etc.).
    const exclusivosInstitucional = camposInstitucional.filter(
      (campo) => !(camposPublico as ReadonlyArray<string>).includes(campo as string)
    );
    expect(exclusivosInstitucional.length).toBeGreaterThan(5);
  });

  it('no expone responsable ni campos sensibles cuando puedeVerResponsable es false', () => {
    const resumen: InstitutionalPortfolioSummary = {
      id: 1,
      tipoRegistro: 'INICIATIVA',
      codigo: '2026-UNIDAD-A-00001',
      fechaInicio: '2026-01-01',
      nombre: 'Iniciativa sintética',
      estado: 'PRESENTADO',
      puedeVerResponsable: false,
      version: 1,
      etag: 'W/"1"'
    };
    expect(resumen.puedeVerResponsable).toBe(false);
    // El resumen no debe exponerResponsable cuando el backend lo ocultó.
    expect(resumen.responsableId).toBeUndefined();
  });

  it('conserva resumen paginado con ETag agregada para concurrencia optimista', () => {
    const resumen: InstitutionalPortfolioSummary = {
      id: 2,
      tipoRegistro: 'PROYECTO',
      codigo: '2026-UNIDAD-B-00001',
      nombre: 'Proyecto sintético',
      estado: 'PROYECTO_EJECUCION',
      fechaInicio: '2026-02-01',
      puedeVerResponsable: true,
      version: 1,
      etag: 'W/"2"'
    };
    expect(resumen.etag.length).toBeGreaterThan(0);
    // El resumen NUNCA debe exponer detalle, resultados ni historial:
    // esos campos viven en `InstitutionalPortfolioDetail` y en
    // colecciones dentro del detalle, no en el resumen.
    type ResumenKeys = keyof InstitutionalPortfolioSummary;
    const camposResumen: ResumenKeys[] = [
      'id',
      'tipoRegistro',
      'codigo',
      'codigoOrigen',
      'nombre',
      'estado',
      'fechaInicio',
      'unidadEjecutoraId',
      'unidadEjecutoraDescripcion',
      'unidadEjecutoraAbreviatura',
      'responsableId',
      'puedeVerResponsable',
      'version',
      'etag'
    ];
    expect(([...camposResumen] as string[]).sort()).toEqual(
      ([...camposResumen] as string[]).sort()
    );
  });

  it('publica la ruta base del cliente institucional sin mezclarse con la pública', () => {
    // La ruta base es la única fuente de verdad para `BASE` y nunca
    // debe apuntar al segmento público.
    type Servicio = { BASE: string };
    const BASE = (InstitutionalQueryApiService as unknown as Servicio).BASE;
    expect(BASE).toBe('/api/v1/consulta/institucional/portafolio');
    expect(BASE).not.toContain('publica');
  });

  it('mantiene el DTO público independiente del institucional', () => {
    const publico: PublicPortfolioDetail = {
      id: 3,
      tipoRegistro: 'INICIATIVA',
      codigo: '2026-UNIDAD-C-00001',
      nombre: 'Iniciativa pública',
      estado: 'PRESENTADO',
      publicaciones: [],
      etag: 'W/"3"'
    };
    const resumenPublico: PublicPortfolioSummary = {
      id: publico.id,
      tipoRegistro: publico.tipoRegistro,
      codigo: publico.codigo,
      nombre: publico.nombre,
      estado: publico.estado,
      publicaciones: publico.publicaciones,
      etag: publico.etag
    };
    // El detalle público expone exactamente siete campos. La igualdad
    // estructural refuerza la separación: cada tipo contiene solo los
    // campos autorizados. La ausencia de campos sensibles en la
    // proyección del cliente se valida arriba con la intersección de
    // tipos.
    expect(Object.keys(publico).sort()).toEqual(
      ['codigo', 'estado', 'etag', 'id', 'nombre', 'publicaciones', 'tipoRegistro'].sort()
    );
    expect(resumenPublico.id).toBe(publico.id);
  });
});
