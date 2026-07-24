import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';
import { parseProblemDetails, ProblemDetails } from '../../../core/http/problem-details';
import { SeguimientoApiService } from '../seguimiento/api/seguimiento-api.service';
import { PresentacionProductoFinalResponse, TipoProductoFinal } from '../seguimiento/api/types';

@Component({ selector: 'app-final-product-submission', standalone: true, changeDetection: ChangeDetectionStrategy.OnPush, imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatInputModule, MatSelectModule], templateUrl: './final-product-submission.component.html', styleUrl: './final-product-submission.component.scss' })
export class FinalProductSubmissionComponent {
  private readonly fb = inject(FormBuilder); private readonly api = inject(SeguimientoApiService);
  @Input() projectId: number | null = null;
  readonly saving = signal(false); readonly problem = signal<ProblemDetails | undefined>(undefined); readonly presentation = signal<PresentacionProductoFinalResponse | undefined>(undefined);
  readonly productTypes: readonly TipoProductoFinal[] = ['PROTOTIPO_CONCEPTUALIZADO', 'SOLUCION_FUNCIONAL'];
  readonly form = this.fb.nonNullable.group({ tipoProductoFinal: ['PROTOTIPO_CONCEPTUALIZADO' as TipoProductoFinal, Validators.required], idDocumentoSustenta: [0, [Validators.required, Validators.min(1)]], evidenciaIds: ['', Validators.required], documentacionGestion: ['', Validators.maxLength(2000)], resultadosClave: ['', Validators.maxLength(2000)], nota: ['', Validators.maxLength(1000)] });
  async submit(): Promise<void> { if (!this.projectId || this.form.invalid) { this.form.markAllAsTouched(); return; } const raw = this.form.getRawValue(); const evidenceIds = raw.evidenciaIds.split(',').map((value) => Number(value.trim())).filter((value) => Number.isInteger(value) && value > 0); if (!evidenceIds.length) { this.problem.set({ type: 'about:blank', title: 'Evidencias requeridas', status: 422, detail: 'Ingrese al menos un identificador de evidencia.', violations: [] }); return; } this.saving.set(true); this.problem.set(undefined); try { this.presentation.set(await firstValueFrom(this.api.presentarProductoFinal(this.projectId, { tipoProductoFinal: raw.tipoProductoFinal, idDocumentoSustenta: raw.idDocumentoSustenta,       evidenciaIds: evidenceIds, documentacionGestion: raw.documentacionGestion || undefined, resultadosClave: raw.resultadosClave || undefined, nota: raw.nota || undefined }))); } catch (error: unknown) { this.problem.set(parseProblemDetails(error) ?? { type: 'about:blank', title: 'No fue posible presentar el producto', status: 0, violations: [] }); } finally { this.saving.set(false); } }
}
