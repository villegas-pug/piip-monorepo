import { isPlatformServer } from '@angular/common';
import { inject, PLATFORM_ID } from '@angular/core';
import { CanActivateFn } from '@angular/router';

import { AuthService } from './auth.service';

/** Guard de experiencia; el backend conserva la autorización efectiva. */
export const authGuard: CanActivateFn = async (): Promise<boolean> => {
  const platformId = inject(PLATFORM_ID);
  const auth = inject(AuthService);

  if (isPlatformServer(platformId)) {
    return true;
  }

  try {
    if (await auth.isAuthenticated()) {
      return true;
    }
    await auth.login();
  } catch {
    // Ante configuración o sesión inválida no se habilita navegación institucional.
  }

  return false;
};
