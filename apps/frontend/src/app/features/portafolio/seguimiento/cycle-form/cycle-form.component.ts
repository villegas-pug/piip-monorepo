import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { firstValueFrom } from 'rxjs';
import { parseProblemDetails, ProblemDetails } from '../../../../core/http/problem-details';
import { SeguimientoApiService } from '../api/seguimiento-api.service';
import { CicloResponse, EvidenciaSeleccionable } from '../api/types';

const PERIOD = /^[0-9]{4}-Q[1-4]-S[1-2]$/;

@Component({ selector: 'app-cycle-form', standalone: true, changeDetection: ChangeDetectionStrategy.OnPush, imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule], templateUrl: './cycle-form.component.html', styleUrl: './cycle-form.component.scss' })
export class CycleFormComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(SeguimientoApiService);
  @Input() projectId: number | null = null;
  readonly form = this.fb.nonNullable.group({ periodo: ['', [Validators.required, Validators.pattern(PERIOD)]], objetivos: ['', [Validators.required, Validators.maxLength(2000)]], actividades: ['', [Validators.required, Validators.maxLength(2000)]], avance: [0, [Validators.required, Validators.min(0), Validators.max(100)]], dificultades: ['', Validators.maxLength(2000)], proximasAcciones: ['', Validators.maxLength(2000)] });
  readonly correctionForm = this.fb.nonNullable.group({ motivo: ['', [Validators.required, Validators.maxLength(2000)]], objetivos: ['', [Validators.required, Validators.maxLength(2000)]], actividades: ['', [Validators.required, Validators.maxLength(2000)]], avance: [0, [Validators.required, Validators.min(0), Validators.max(100)]], dificultades: ['', Validators.maxLength(2000)], proximasAcciones: ['', Validators.maxLength(2000)] });
  readonly savedCycle = signal<CicloResponse | undefined>(undefined);
  readonly correctingCycle = signal<CicloResponse | undefined>(undefined);
  readonly selectedEvidence = signal<EvidenciaSeleccionable | undefined>(undefined);
  readonly evidenceError = signal<string | undefined>(undefined);
  readonly problem = signal<ProblemDetails | undefined>(undefined);
  readonly saving = signal(false);

  async submit(): Promise<void> {
    if (!this.valid(this.form) || !this.projectId) return;
    this.saving.set(true); this.problem.set(undefined);
    try { const saved = await firstValueFrom(this.api.registrarCiclo(this.projectId, this.form.getRawValue())); this.savedCycle.set(saved); await this.attachEvidence(saved); this.form.reset({ periodo: '', objetivos: '', actividades: '', avance: 0, dificultades: '', proximasAcciones: '' }); }
    catch (error: unknown) { this.problem.set(parseProblemDetails(error) ?? fallbackProblem(error)); }
    finally { this.saving.set(false); }
  }
  selectEvidence(evidence: EvidenciaSeleccionable): void { if (!evidence.aptaComoEvidencia) { this.selectedEvidence.set(undefined); this.evidenceError.set('El documento seleccionado no es apta como evidencia.'); return; } this.selectedEvidence.set(evidence); this.evidenceError.set(undefined); }
  startCorrection(cycle: CicloResponse): void { this.correctingCycle.set(cycle); this.correctionForm.reset({ motivo: '', objetivos: cycle.objetivos ?? '', actividades: cycle.actividades ?? '', avance: cycle.avance ?? 0, dificultades: cycle.dificultades ?? '', proximasAcciones: cycle.proximasAcciones ?? '' }); }
  cancelCorrection(): void { this.correctingCycle.set(undefined); this.correctionForm.reset(); }
  async submitCorrection(): Promise<void> { const cycle = this.correctingCycle(); if (!cycle || !this.projectId || !this.valid(this.correctionForm)) return; this.saving.set(true); this.problem.set(undefined); try { this.savedCycle.set(await firstValueFrom(this.api.corregirCiclo(this.projectId, cycle.idCiclo, this.correctionForm.getRawValue()))); this.cancelCorrection(); } catch (error: unknown) { this.problem.set(parseProblemDetails(error) ?? fallbackProblem(error)); } finally { this.saving.set(false); } }
  private valid(form: { readonly invalid: boolean; markAllAsTouched(): void }): boolean { if (form.invalid) { form.markAllAsTouched(); return false; } return true; }
  private async attachEvidence(cycle: CicloResponse): Promise<void> { const evidence = this.selectedEvidence(); if (evidence && this.projectId) await firstValueFrom(this.api.adjuntarEvidenciaCiclo(this.projectId, cycle.idCiclo, { idDocumento: evidence.idDocumento, tipoDocumental: evidence.tipoDocumental })); }
}
function fallbackProblem(error: unknown): ProblemDetails { const source = error as { status?: unknown; error?: { title?: unknown; code?: unknown; detail?: unknown } }; return { type: 'about:blank', title: typeof source.error?.title === 'string' ? source.error.title : 'No fue posible registrar el ciclo', status: typeof source.status === 'number' ? source.status : 0, code: typeof source.error?.code === 'string' ? source.error.code : undefined, detail: typeof source.error?.detail === 'string' ? source.error.detail : error instanceof Error ? error.message : undefined, violations: [] }; }
