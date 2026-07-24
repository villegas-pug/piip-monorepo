import { ApplicationConfig, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideClientHydration } from '@angular/platform-browser';
import { authInterceptor } from './core/http/auth.interceptor';
import { effectiveAssignmentInterceptor } from './core/effective-assignment/effective-assignment.interceptor';
import { idempotencyKeyInterceptor } from './core/http/idempotency-key.service';
import { entityTagInterceptor } from './core/http/entity-tag';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    provideClientHydration(),
    provideHttpClient(
      withInterceptors([authInterceptor, effectiveAssignmentInterceptor, entityTagInterceptor, idempotencyKeyInterceptor])
    )
  ]
};
