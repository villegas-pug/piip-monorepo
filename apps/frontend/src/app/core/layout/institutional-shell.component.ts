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
      <nav aria-label="Módulos institucionales">
        <section>
          <h2>Portafolio</h2>
          <a routerLink="/portafolio/registro/iniciativas/nueva">Registrar iniciativa</a>
          <a routerLink="/portafolio/proyectos/proyectos-directos/nuevo">Proyecto directo</a>
          <a routerLink="/portafolio/decision/iniciativas/1">Decidir iniciativa</a>
          <a routerLink="/portafolio/seguimiento/proyectos/1">Seguimiento</a>
          <a routerLink="/portafolio/producto-final/proyectos/1/decision">Producto final</a>
          <a routerLink="/portafolio/cierre/proyectos/1">Cierre</a>
        </section>
        <section>
          <h2>Seguridad</h2>
          <a routerLink="/seguridad/usuarios">Usuarios</a>
          <a routerLink="/seguridad/matriz">Matriz funcional</a>
          <a routerLink="/seguridad/asignaciones">Asignaciones</a>
          <a routerLink="/seguridad/suplencias">Suplencias</a>
        </section>
        <section>
          <h2>Reportes</h2>
          <a routerLink="/reportes">Generar reporte</a>
        </section>
      </nav>
      <router-outlet />
    </main>
  `,
  styles: `
    .toolbar-spacer { flex: 1 1 auto; }
    a { color: inherit; text-decoration: none; }
    a:hover { text-decoration: underline; }
    main { padding: 1rem; }
    nav { display: flex; gap: 2rem; margin-top: 1.5rem; flex-wrap: wrap; }
    nav section { display: flex; flex-direction: column; gap: 0.5rem; }
    nav h2 { margin: 0 0 0.25rem; font-size: 0.875rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.05em; color: var(--piip-color-primary, var(--mat-sys-primary)); }
  `
})
export class InstitutionalShellComponent {}
