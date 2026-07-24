// Servicio de carga y consulta de documentos y evidencias.
// Referencia contractual: specs/001-gestionar-portafolio-innovacion/contracts/documentos.md
// El cliente NO calcula hash, NO decide clasificación validada, NO expone storageKey ni
// ofrece descarga pública. La seguridad antimalware es responsabilidad exclusiva de OGTI
// y permanece fuera del alcance de PIIP.
//
// El servicio expone carga documental para que el formulario de iniciativa pueda subir
// la ficha (campo 22 / `fichaDocumentoVersionId` en `CreateInitiativeRequest`).
// El backend calcula SHA-256, valida tamaño/MIME real y devuelve `DocumentVersionDetail`.

import { HttpClient, HttpContext, HttpEvent } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { withEntityTag } from '../../../core/http/entity-tag';
import { REQUIRES_IDEMPOTENCY_KEY } from '../../../core/http/idempotency-key.service';
import {
  DocumentVersionDetail,
  UploadDocumentRequest,
  UploadDocumentResponse
} from '../../portafolio/registro/api/types/documentos.types';

interface RawDocumentVersionDetail {
  readonly documentoId: number;
  readonly serieId: number;
  readonly version: number;
  readonly titulo: string;
  readonly formato: string;
  readonly tamanoBytes: number;
  readonly hashSha256: string;
  readonly clasificacionPropuesta: 'PUBLICO' | 'INTERNO' | 'RESTRINGIDO';
  readonly clasificacionValidada?: 'PUBLICO' | 'INTERNO' | 'RESTRINGIDO';
  readonly aptaComoEvidencia: boolean;
  readonly etag: string;
}

interface RawPageResponse<T> {
  readonly items?: readonly T[];
  readonly page?: number;
  readonly size?: number;
  readonly totalElements?: number;
  readonly totalPages?: number;
}

export interface DocumentVersionPage {
  readonly items: readonly DocumentVersionDetail[];
  readonly page: number;
  readonly size: number;
  readonly totalElements: number;
  readonly totalPages: number;
}

@Injectable({ providedIn: 'root' })
export class DocumentosApiService {
  private readonly http = inject(HttpClient);
  private static readonly BASE = '/api/v1/documentos';

  /**
   * Carga un documento y devuelve la versión persistida. El backend exige `Idempotency-Key`
   * (gestionado por `idempotencyKeyInterceptor`) y calcula SHA-256; el cliente no envía hash.
   */
  cargar(file: File, metadata: UploadDocumentRequest): Observable<UploadDocumentResponse> {
    const form = buildMultipart(file, metadata);
    const context = new HttpContext().set(REQUIRES_IDEMPOTENCY_KEY, true);
    return this.http.post<UploadDocumentResponse>(
      DocumentosApiService.BASE,
      form,
      { context }
    );
  }

  /**
   * Sube una nueva versión de una serie documental. Exige `If-Match` de la última versión.
   */
  crearVersion(
    serieId: number,
    file: File,
    metadata: UploadDocumentRequest,
    motivo: string,
    etag: string
  ): Observable<HttpEvent<UploadDocumentResponse>> {
    const form = buildMultipart(file, metadata);
    form.append('motivo', new Blob([motivo], { type: 'text/plain' }), 'motivo.txt');
    const context = withEntityTag(etag).set(REQUIRES_IDEMPOTENCY_KEY, true);

    return this.http.post<UploadDocumentResponse>(
      `${DocumentosApiService.BASE}/${serieId}/versiones`,
      form,
      { context, observe: 'events' }
    );
  }

  /** Recupera los metadatos de una versión autorizada. */
  obtener(documentoId: number): Observable<DocumentVersionDetail> {
    return this.http
      .get<RawDocumentVersionDetail>(`${DocumentosApiService.BASE}/${documentoId}`)
      .pipe(map((raw) => toDocumentVersionDetail(raw)));
  }

  /**
   * Lista el historial de versiones de una serie, paginado y autorizado.
   * El cliente no ofrece descarga pública de contenido; este endpoint es solo de metadatos.
   */
  listarVersiones(serieId: number, page = 0, size = 20): Observable<DocumentVersionPage> {
    return this.http
      .get<RawPageResponse<RawDocumentVersionDetail>>(
        `${DocumentosApiService.BASE}/series/${serieId}/versiones`,
        { params: { page: String(page), size: String(size) } }
      )
      .pipe(map((response) => normalizePage(response, toDocumentVersionDetail)));
  }
}

function buildMultipart(file: File, metadata: UploadDocumentRequest): FormData {
  const form = new FormData();
  form.append('file', file, file.name);
  form.append('metadata', new Blob([JSON.stringify(metadata)], { type: 'application/json' }), 'metadata.json');
  return form;
}

function toDocumentVersionDetail(raw: RawDocumentVersionDetail): DocumentVersionDetail {
  return Object.freeze({
    documentoId: raw.documentoId,
    serieId: raw.serieId,
    version: raw.version,
    titulo: raw.titulo,
    formato: raw.formato,
    tamanoBytes: raw.tamanoBytes,
    hashSha256: raw.hashSha256,
    clasificacionPropuesta: raw.clasificacionPropuesta,
    clasificacionValidada: raw.clasificacionValidada,
    aptaComoEvidencia: raw.aptaComoEvidencia,
    etag: raw.etag
  });
}

function normalizePage<T, R>(response: RawPageResponse<T>, mapper: (item: T) => R): {
  items: readonly R[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
} {
  const items = (response.items ?? []).map(mapper);
  return Object.freeze({
    items,
    page: response.page ?? 0,
    size: response.size ?? items.length,
    totalElements: response.totalElements ?? items.length,
    totalPages: response.totalPages ?? (items.length ? 1 : 0)
  });
}
