import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';

import { EffectiveAssignmentSelectorComponent } from '../effective-assignment/effective-assignment-selector.component';

@Component({
  selector: 'app-institutional-shell',
  imports: [RouterLink, RouterOutlet, MatToolbarModule, EffectiveAssignmentSelectorComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a class="skip-link" href="#institutional-content">Saltar al contenido principal</a>
    <mat-toolbar role="banner" color="primary">
      <a routerLink="/institucional" aria-label="PIIP, inicio institucional">PIIP</a>
      <span class="toolbar-spacer" aria-hidden="true"></span>
      <a routerLink="/consulta-publica">Consulta pública</a>
    </mat-toolbar>
    <main id="institutional-content" tabindex="-1">
      <h1>Plataforma de Innovación Pública</h1>
      <app-effective-assignment-selector />
      <router-outlet />
    </main>
  `,
  styles: `
    .toolbar-spacer { flex: 1 1 auto; }
    a { color: inherit; }
    main { padding: 1rem; }
  `
})
export class InstitutionalShellComponent {}
