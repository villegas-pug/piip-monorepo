import { inject } from '@angular/core';
import { CanActivateFn } from '@angular/router';

import { AuthService } from './auth.service';

/** Guard de experiencia; el backend conserva la autorización efectiva. */
export const authGuard: CanActivateFn = async (): Promise<boolean> => {
  const auth = inject(AuthService);

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
