import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { Router } from '@angular/router';

import { AuthService } from './auth.service';

/** Punto de retorno OIDC; Keycloak procesa el código únicamente en el navegador. */
@Component({
  selector: 'app-auth-callback',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <main aria-live="polite">
      <p>{{ message() }}</p>
    </main>
  `
})
export class AuthCallbackComponent implements OnInit {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  protected readonly message = signal('Completando la autenticación institucional…');

  async ngOnInit(): Promise<void> {
    try {
      if (await this.auth.isAuthenticated()) {
        await this.router.navigateByUrl('/institucional');
        return;
      }
      await this.auth.login();
    } catch {
      this.message.set('No fue posible completar la autenticación institucional.');
    }
  }
}
