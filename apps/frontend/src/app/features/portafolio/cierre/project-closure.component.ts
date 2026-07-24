import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';

import { parseProblemDetails, ProblemDetails } from '../../../core/http/problem-details';
import { CierreApiService, CierreProyectoResponse } from './api/cierre-api.service';

@Component({
  selector: 'app-project-closure',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  templateUrl: './project-closure.component.html',
  styleUrl: './project-closure.component.scss'
})
export class ProjectClosureComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(CierreApiService);

  @Input() projectId: number | null = null;
  @Input() etag: string | undefined;

  readonly saving = signal(false);
  readonly problem = signal<ProblemDetails | undefined>(undefined);
  readonly closure = signal<CierreProyectoResponse | undefined>(undefined);
  readonly form = this.fb.nonNullable.group({
    informeFinal: ['', Validators.required],
    informeFinalDocumentoId: [0, [Validators.required, Validators.min(1)]],
    aprendizajes: ['', Validators.required],
    conclusion: ['', [Validators.required, Validators.maxLength(2000)]],
    observacion: ['', [Validators.required, Validators.maxLength(2000)]]
  });

  async submit(): Promise<void> {
    if (!this.projectId || !this.etag || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    this.saving.set(true);
    this.problem.set(undefined);
    try {
      this.closure.set(await firstValueFrom(this.api.cerrar(this.projectId, {
        informeFinal: raw.informeFinal.trim(),
        informeFinalDocumentoId: raw.informeFinalDocumentoId,
        aprendizajes: raw.aprendizajes.trim(),
        conclusion: raw.conclusion.trim(),
        observacion: raw.observacion.trim()
      }, { etag: this.etag })));
    } catch (error: unknown) {
      this.problem.set(parseProblemDetails(error) ?? fallbackProblem('No fue posible completar el cierre.'));
    } finally {
      this.saving.set(false);
    }
  }
}

function fallbackProblem(detail: string): ProblemDetails {
  return { type: 'about:blank', title: 'Operación no completada', status: 0, detail, violations: [] };
}
