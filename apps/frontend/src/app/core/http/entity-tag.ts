import { HttpContext, HttpContextToken, HttpInterceptorFn, HttpResponse } from '@angular/common/http';

export const ENTITY_TAG = new HttpContextToken<string | undefined>(() => undefined);

/** Conserva el ETag recibido para enviarlo explícitamente en el siguiente comando mutable. */
export function entityTagFrom(response: HttpResponse<unknown>): string | undefined {
  return response.headers.get('ETag') ?? undefined;
}

export function withEntityTag(entityTag: string): HttpContext {
  const normalized = entityTag.trim();
  if (!normalized) {
    throw new Error('If-Match requiere un ETag no vacío.');
  }
  return new HttpContext().set(ENTITY_TAG, normalized);
}

/** Aplica If-Match solo cuando el consumidor proporciona el ETag retornado por el servidor. */
export function ifMatchHeader(entityTag: string | undefined): Record<string, string> {
  return entityTag ? { 'If-Match': entityTag } : {};
}

/** Inserta If-Match cuando el consumidor adjunta el ETag de una lectura previa al comando. */
export const entityTagInterceptor: HttpInterceptorFn = (request, next) => {
  const entityTag = request.context.get(ENTITY_TAG);
  if (!entityTag || request.headers.has('If-Match')) {
    return next(request);
  }
  return next(request.clone({ setHeaders: { 'If-Match': entityTag } }));
};
