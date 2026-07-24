import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnChanges, SimpleChanges, inject, signal, viewChild } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { firstValueFrom } from 'rxjs';
import { parseProblemDetails, ProblemDetails } from '../../../core/http/problem-details';
import { CycleFormComponent } from './cycle-form/cycle-form.component';
import { SeguimientoApiService } from './api/seguimiento-api.service';
import { CicloResponse, ParticipanteResponse, PlanificacionResponse, ProyectoSeguimiento, TransicionResponse } from './api/types';
import { ProjectSuspensionComponent } from './project-suspension.component';

@Component({ selector: 'app-tracking-page', standalone: true, changeDetection: ChangeDetectionStrategy.OnPush, imports: [CommonModule, ReactiveFormsModule, MatButtonModule, CycleFormComponent, ProjectSuspensionComponent], templateUrl: './tracking-page.component.html', styleUrl: './tracking-page.component.scss' })
export class TrackingPageComponent implements OnChanges {
  private readonly api = inject(SeguimientoApiService); private readonly fb = inject(FormBuilder);
  @Input() projectId: number | null = null;
  private readonly cycleForm = viewChild(CycleFormComponent);
  readonly project = signal<ProyectoSeguimiento | undefined>(undefined); readonly plans = signal<readonly PlanificacionResponse[]>([]); readonly cycles = signal<readonly CicloResponse[]>([]); readonly participants = signal<readonly ParticipanteResponse[]>([]); readonly problem = signal<ProblemDetails | undefined>(undefined);
  readonly planningForm = this.fb.nonNullable.group({ alcance: ['', [Validators.required, Validators.maxLength(2000)]], objetivos: ['', [Validators.required, Validators.maxLength(2000)]], entregables: ['', Validators.maxLength(100000)], periodos: ['', Validators.maxLength(100000)] });
  readonly fieldsForm = this.fb.nonNullable.group({ documentacionGestion: ['', Validators.maxLength(2000)], resultadosClave: ['', Validators.maxLength(2000)], nota: ['', Validators.maxLength(1000)] });
  async ngOnChanges(changes: SimpleChanges): Promise<void> { if (changes['projectId'] && this.projectId) await this.load(); }
  async savePlanning(): Promise<void> { if (!this.projectId || this.planningForm.invalid) { this.planningForm.markAllAsTouched(); return; } await this.run(async () => { await firstValueFrom(this.api.registrarPlanificacion(this.projectId!, this.planningForm.getRawValue())); await this.load(); }); }
  async saveFields(): Promise<void> { const project = this.project(); if (!project || this.fieldsForm.invalid) return; await this.run(async () => { await firstValueFrom(this.api.editarCamposEditables(project.id, this.fieldsForm.getRawValue(), { etag: project.etag })); await this.load(); }); }
  onSuspended(event: TransicionResponse): void { const project = this.project(); if (project) this.project.set({ ...project, estado: event.estadoNuevo, etag: event.etag }); }
  startCorrection(cycle: CicloResponse): void { this.cycleForm()?.startCorrection(cycle); }
  private async load(): Promise<void> { if (!this.projectId) return; await this.run(async () => { const [project, plans, cycles, participants] = await Promise.all([firstValueFrom(this.api.consultarProyecto(this.projectId!)), firstValueFrom(this.api.listarPlanificaciones(this.projectId!)), firstValueFrom(this.api.listarCiclos(this.projectId!)), firstValueFrom(this.api.listarParticipantes(this.projectId!))]); this.project.set(project); this.plans.set(plans); this.cycles.set(cycles); this.participants.set(participants); this.fieldsForm.patchValue({ documentacionGestion: project.documentacionGestion ?? '', resultadosClave: project.resultadosClave ?? '', nota: project.nota ?? '' }); }); }
  private async run(work: () => Promise<void>): Promise<void> { try { this.problem.set(undefined); await work(); } catch (error: unknown) { this.problem.set(parseProblemDetails(error) ?? { type: 'about:blank', title: 'No fue posible cargar el seguimiento', status: 0, violations: [] }); } }
}
