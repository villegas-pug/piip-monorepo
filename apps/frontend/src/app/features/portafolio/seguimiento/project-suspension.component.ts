import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, inject, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';
import { parseProblemDetails, ProblemDetails } from '../../../core/http/problem-details';
import { SeguimientoApiService } from './api/seguimiento-api.service';
import { TransicionResponse } from './api/types';

@Component({ selector: 'app-project-suspension', standalone: true, changeDetection: ChangeDetectionStrategy.OnPush, imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatInputModule], templateUrl: './project-suspension.component.html', styleUrl: './project-suspension.component.scss' })
export class ProjectSuspensionComponent {
  private readonly fb = inject(FormBuilder); private readonly api = inject(SeguimientoApiService);
  @Input() projectId: number | null = null; @Input() etag: string | undefined;
  readonly suspended = output<TransicionResponse>(); readonly open = signal(false); readonly saving = signal(false); readonly problem = signal<ProblemDetails | undefined>(undefined);
  readonly form = this.fb.nonNullable.group({ idDocumento: [0, [Validators.required, Validators.min(1)]], observacion: ['', [Validators.required, Validators.maxLength(2000)]] });
  abrir(): void { this.open.set(true); }
  cerrar(): void { this.open.set(false); this.form.reset({ idDocumento: 0, observacion: '' }); }
  async submit(): Promise<void> { if (!this.projectId || this.form.invalid) { this.form.markAllAsTouched(); return; } this.saving.set(true); this.problem.set(undefined); try { const result = await firstValueFrom(this.api.suspenderProyecto(this.projectId, this.form.getRawValue(), { etag: this.etag })); this.suspended.emit(result); this.cerrar(); } catch (error: unknown) { this.problem.set(parseProblemDetails(error) ?? { type: 'about:blank', title: 'No fue posible suspender', status: 0, violations: [] }); } finally { this.saving.set(false); } }
}
