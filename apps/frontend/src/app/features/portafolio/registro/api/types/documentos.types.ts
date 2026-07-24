// Tipos de Documentos y evidencias basados en el contrato `documentos.md` aprobado.
// El snapshot OpenAPI no expone aún los endpoints de carga documental (placeholders hasta T023),
// por lo que estos tipos modelan exclusivamente la forma del contrato aprobado.
// Referencia contractual: specs/001-gestionar-portafolio-innovacion/contracts/documentos.md
// El cliente no calcula hash, no decide clasificación validada, no expone storageKey ni
// ofrece descarga pública. La seguridad antimalware es responsabilidad exclusiva de OGTI.

import { ClasificacionDocumento, DocumentOwnerType } from './common.types';

/** Propietario de un documento. Excluyente: XOR entre los dos identificadores. */
export interface DocumentOwner {
  readonly tipo: DocumentOwnerType;
  readonly registroPortafolioId?: number;
  readonly expedienteInstitucionalId?: number;
}

/** Uso previsto del documento. No condiciona clasificación validada. */
export type DocumentUsage =
  | 'FICHA_INICIATIVA'
  | 'EVIDENCIA'
  | 'DOCUMENTO_FORMAL'
  | 'OPINION_TECNICA'
  | 'DECISION'
  | 'INFORME_FINAL'
  | 'OTROS';

/** Metadatos de carga documental (multipart, no JSON). */
export interface UploadDocumentRequest {
  readonly owner: DocumentOwner;
  readonly tipoDocumentoId: number;
  readonly titulo: string;
  readonly clasificacionPropuesta: ClasificacionDocumento;
  readonly uso: DocumentUsage;
}

/** Detalle de una versión documental devuelto por el backend. */
export interface DocumentVersionDetail {
  readonly documentoId: number;
  readonly serieId: number;
  readonly version: number;
  readonly titulo: string;
  readonly formato: string;
  readonly tamanoBytes: number;
  readonly hashSha256: string;
  readonly clasificacionPropuesta: ClasificacionDocumento;
  readonly clasificacionValidada?: ClasificacionDocumento;
  readonly aptaComoEvidencia: boolean;
  readonly etag: string;
}

/** Respuesta de carga documental. */
export interface UploadDocumentResponse {
  readonly detail: DocumentVersionDetail;
  readonly correlationId?: string;
}
