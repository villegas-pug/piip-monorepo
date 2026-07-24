// Tipos DTO del cliente público de consulta del portafolio (US7 - T101).
//
// Estos tipos son la proyección cliente de la allowlist canónica declarada
// en `specs/001-gestionar-portafolio-innovacion/contracts/openapi/piip-api.yaml`
// para los endpoints:
//   * `GET /api/v1/consulta/publica/portafolio`
//   * `GET /api/v1/consulta/publica/portafolio/{id}`
//
// Son DTOs PROPIOS del módulo `consulta-publica` y NO se reutilizan con
// el cliente institucional (`consulta-institucional/api/types.ts`). La
// allowlist pública está fijada por la Constitución y la especificación:
//   * `tipoRegistro` (`INICIATIVA` | `PROYECTO`).
//   * `codigo`.
//   * `nombre`.
//   * `estado`.
//   * `publicaciones`: metadatos descriptivos sin datos personales
//     (`tipoDocumental`, `tituloPublico`, `version`, `formato`,
//     `fechaPublicacion`).
//
// NUNCA se incluyen en estos tipos: `responsableId`, `descripcion`,
// `resultadosClave`, `participantes`, `historial`, `unidadEjecutora`,
// `BLOB`, `URL de descarga`, `hash` ni `clave física`. Si el equipo
// detecta una necesidad funcional, debe registrarla como
// `NEEDS CLARIFICATION` y esperar una decisión constitucional.
//
// Reglas de privacidad reforzadas en el cliente:
//   * El cliente público NO envía `Authorization`: el interceptor
//     `authInterceptor` omite la cabecera para `/api/v1/consulta/publica`.
//   * El cliente público NO envía `X-Asignacion-Efectiva-Id`: la consulta
//     pública es anónima y el interceptor omite la cabecera para esa
//     ruta.
//   * El cliente público NO expone `InstitutionalPortfolioDocument` ni
//     ningún tipo con `BLOB`, `tamanoBytes`, `hashSha256` o
//     `puedeConsultarContenido`: solo se exponen los metadatos públicos
//     de las versiones con publicación confirmada y clasificación
//     `PUBLICO` validada.

export type PublicTipoRegistro = 'INICIATIVA' | 'PROYECTO';

export type PublicEstado = string;

/**
 * Metadatos públicos de un documento elegible para la consulta pública.
 *
 * Solo se incluyen las versiones con publicación confirmada y
 * clasificación `PUBLICO` validada; el título público no debe contener
 * datos personales. NUNCA incluye BLOB, URL de descarga, hash ni clave
 * física.
 */
export interface PublicPortfolioDocumento {
  readonly tipoDocumental: string;
  readonly tituloPublico: string;
  readonly version: number;
  readonly formato: string;
  readonly fechaPublicacion: string;
}

/**
 * Resumen público de un registro del portafolio. Contiene únicamente
 * la allowlist de cuatro campos públicos más los metadatos de las
 * publicaciones elegibles.
 */
export interface PublicPortfolioSummary {
  readonly id: number;
  readonly tipoRegistro: PublicTipoRegistro;
  readonly codigo: string;
  readonly nombre: string;
  readonly estado: PublicEstado;
  readonly fechaInicio?: string;
  readonly publicaciones: readonly PublicPortfolioDocumento[];
  readonly etag: string;
}

/**
 * Detalle público de un registro del portafolio. Replica la allowlist
 * del resumen y añade los metadatos de las publicaciones elegibles.
 * NUNCA expone contenido ni una URL de descarga.
 */
export interface PublicPortfolioDetail {
  readonly id: number;
  readonly tipoRegistro: PublicTipoRegistro;
  readonly codigo: string;
  readonly nombre: string;
  readonly estado: PublicEstado;
  readonly publicaciones: readonly PublicPortfolioDocumento[];
  readonly etag: string;
}

/** Página pública devuelta por `GET /consulta/publica/portafolio`. */
export interface PublicPortfolioPage {
  readonly items: readonly PublicPortfolioSummary[];
  readonly pagina: number;
  readonly tamanio: number;
  readonly totalElementos: number;
  readonly totalPaginas: number;
  readonly etag: string;
}

/** Filtros admitidos por la búsqueda pública. */
export interface PublicPortfolioFilters {
  readonly tipo?: PublicTipoRegistro;
  readonly codigo?: string;
  readonly nombre?: string;
  readonly page?: number;
  readonly size?: number;
  readonly sort?: 'codigo' | 'nombre' | 'estado';
}

/** Opciones de la consulta pública de detalle. */
export interface PublicPortfolioDetailOptions {
  /**
   * ETag de la última lectura. Si se envía, el backend responde 304 sin
   * cuerpo cuando el detalle público no ha cambiado.
   */
  readonly ifNoneMatch?: string;
}

/**
 * Resultado crudo de la respuesta HTTP de detalle público, con la ETag
 * recibida en la cabecera. Permite al componente conservar la ETag para
 * `If-None-Match` en lecturas posteriores sin exponer más campos.
 */
export interface PublicPortfolioDetailResponse {
  readonly body: PublicPortfolioDetail;
  readonly etag?: string;
  readonly notModified: boolean;
}
