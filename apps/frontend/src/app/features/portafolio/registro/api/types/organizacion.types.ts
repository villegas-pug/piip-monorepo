// Tipos de Organización basados en el contrato `organizacion.md` aprobado.
// El snapshot OpenAPI no expone aún los endpoints de organización (placeholders hasta T048),
// por lo que estos tipos modelan exclusivamente la forma del contrato aprobado.
// Referencia contractual: specs/001-gestionar-portafolio-innovacion/contracts/organizacion.md
// El cliente nunca interpreta permisos, ámbito ni vigencia: el backend autoriza cada operación.

/** Opción de unidad para selectores. El cliente solo muestra; el backend decide el alcance efectivo. */
export interface UnidadOption {
  readonly id: number;
  readonly codigo: string;
  readonly nombre: string;
  readonly activa: boolean;
}

/** Opción de objetivo PEI o actividad POI (catálogo versionado independiente). */
export interface PlaneamientoOption {
  readonly id: number;
  readonly codigo: string;
  readonly descripcion: string;
  readonly vigenteDesde: string;
  readonly vigenteHasta?: string;
  readonly activo: boolean;
}

/** Filtros de consulta para `/api/v1/organizacion/unidades`. */
export interface UnidadQuery {
  readonly q?: string;
  readonly activa?: boolean;
  readonly page?: number;
  readonly size?: number;
  readonly sort?: 'codigo' | 'nombre';
}

/** Filtros de consulta para PEI y POI. */
export interface PlaneamientoQuery {
  readonly q?: string;
  readonly vigenteEn?: string;
  readonly page?: number;
  readonly size?: number;
  readonly sort?: 'codigo' | 'descripcion';
}
