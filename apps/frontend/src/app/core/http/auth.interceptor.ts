import { inject } from '@angular/core';
import { HttpInterceptorFn } from '@angular/common/http';
import { from, switchMap } from 'rxjs';

import { AuthService } from '../auth/auth.service';

/** Añade el token solo a rutas institucionales del API PIIP. */
export const authInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isInstitutionalRequest(request.url) || request.headers.has('Authorization')) {
    return next(request);
  }

  const auth = inject(AuthService);
  return from(auth.getValidAccessToken()).pipe(
    switchMap((token) => next(token ? request.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : request))
  );
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
