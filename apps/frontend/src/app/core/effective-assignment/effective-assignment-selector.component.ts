import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';

import { EffectiveAssignmentService } from './effective-assignment.service';

/** Selector de contexto para UX; cada solicitud sensible sigue siendo validada por el backend. */
@Component({
  selector: 'app-effective-assignment-selector',
  imports: [MatFormFieldModule, MatSelectModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <mat-form-field appearance="outline">
      <mat-label>Asignación efectiva para esta operación</mat-label>
      <mat-select
        [value]="assignments.selectedId()"
        (selectionChange)="assignments.select($event.value)"
        aria-describedby="effective-assignment-help">
        @for (assignment of assignments.options(); track assignment.id) {
          <mat-option [value]="assignment.id">
            {{ assignment.funcion }} · {{ assignment.perfil }} · {{ assignment.unidad }}
          </mat-option>
        }
      </mat-select>
      <mat-hint id="effective-assignment-help">
        Seleccione una sola asignación. El servidor confirma su vigencia y ámbito en cada operación.
      </mat-hint>
    </mat-form-field>
  `
})
export class EffectiveAssignmentSelectorComponent implements OnInit {
  protected readonly assignments = inject(EffectiveAssignmentService);

  ngOnInit(): void {
    this.assignments.load().subscribe();
  }
}
