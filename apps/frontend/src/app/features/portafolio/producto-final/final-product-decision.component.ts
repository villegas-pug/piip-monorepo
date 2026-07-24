import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { firstValueFrom } from 'rxjs';

import { parseProblemDetails, ProblemDetails } from '../../../core/http/problem-details';
import {
  DecisionProductoFinal,
  DecisionProductoFinalResponse,
  ProductoFinalApiService,
  TipoProductoFinal
} from './api/producto-final-api.service';

@Component({
  selector: 'app-final-product-decision',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, ReactiveFormsModule, MatButtonModule, MatFormFieldModule, MatInputModule, MatSelectModule],
  templateUrl: './final-product-decision.component.html',
  styleUrl: './final-product-decision.component.scss'
})
export class FinalProductDecisionComponent {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(ProductoFinalApiService);

  @Input() projectId: number | null = null;
  @Input() etag: string | undefined;

  readonly saving = signal(false);
  readonly problem = signal<ProblemDetails | undefined>(undefined);
  readonly decision = signal<DecisionProductoFinalResponse | undefined>(undefined);
  readonly decisionOptions: readonly DecisionProductoFinal[] = ['APROBAR', 'NO_APROBAR'];
  readonly productTypes: readonly TipoProductoFinal[] = ['PROTOTIPO_CONCEPTUALIZADO', 'SOLUCION_FUNCIONAL'];
  readonly form = this.fb.nonNullable.group({
    decision: ['APROBAR' as DecisionProductoFinal, Validators.required],
    tipoProductoFinal: ['PROTOTIPO_CONCEPTUALIZADO' as TipoProductoFinal, Validators.required],
    documentoId: [null as number | null, Validators.min(1)],
    evidenciaId: [null as number | null, Validators.min(1)],
    observacion: ['', Validators.maxLength(2000)]
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
      this.decision.set(await firstValueFrom(this.api.decidir(this.projectId, {
        decision: raw.decision,
        tipoProductoFinal: raw.tipoProductoFinal,
        documentoId: raw.documentoId ?? undefined,
        evidenciaId: raw.evidenciaId ?? undefined,
        observacion: raw.observacion.trim() || undefined
      }, { etag: this.etag })));
    } catch (error: unknown) {
      this.problem.set(parseProblemDetails(error) ?? fallbackProblem('No fue posible registrar la decisión.'));
    } finally {
      this.saving.set(false);
    }
  }
}

function fallbackProblem(detail: string): ProblemDetails {
  return { type: 'about:blank', title: 'Operación no completada', status: 0, detail, violations: [] };
}
