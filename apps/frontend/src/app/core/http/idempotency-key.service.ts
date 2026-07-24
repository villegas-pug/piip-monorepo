import { Injectable, inject } from '@angular/core';
import { HttpContextToken, HttpInterceptorFn } from '@angular/common/http';

/** Permite excluir explícitamente un POST que el contrato no considere un comando idempotente. */
export const REQUIRES_IDEMPOTENCY_KEY = new HttpContextToken<boolean>(() => true);

@Injectable({ providedIn: 'root' })
export class IdempotencyKeyService {
  create(): string {
    if (typeof crypto === 'undefined' || typeof crypto.randomUUID !== 'function') {
      throw new Error('El navegador no ofrece un generador criptográfico para Idempotency-Key.');
    }
    return crypto.randomUUID();
  }
}

/** Asigna una clave por solicitud a comandos POST institucionales, sin persistirla. */
export const idempotencyKeyInterceptor: HttpInterceptorFn = (request, next) => {
  if (
    request.method !== 'POST' ||
    !request.context.get(REQUIRES_IDEMPOTENCY_KEY) ||
    request.headers.has('Idempotency-Key') ||
    !isInstitutionalRequest(request.url)
  ) {
    return next(request);
  }

  const key = inject(IdempotencyKeyService).create();
  return next(request.clone({ setHeaders: { 'Idempotency-Key': key } }));
};

function isInstitutionalRequest(url: string): boolean {
  const parsed = new URL(url, 'http://piip.local');
  if (!parsed.pathname.startsWith('/api/v1/') || parsed.pathname.startsWith('/api/v1/consulta/publica')) {
    return false;
  }

  return !isAbsoluteUrl(url) || typeof location === 'undefined' || parsed.origin === location.origin;
}

function isAbsoluteUrl(url: string): boolean {
  return /^[a-z][a-z\d+.-]*:/i.test(url);
}
