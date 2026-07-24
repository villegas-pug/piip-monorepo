// Página de administración de la matriz función-perfil-unidad (US6 - T092).
//
// Recorrido del administrador de seguridad: lista el historial paginado
// de versiones inmutables de matriz, registra nuevas versiones (aprobadas
// formalmente) y consulta combinaciones e inactiva una combinación
// registrando una nueva versión que la omite. La página NO decide
// funciones, NO crea combinaciones sin documento de aprobación formal y
// NO inactiva por su cuenta: el backend es la autoridad efectiva y exige
// documento de aprobación, aprobador y código de versión únicos.
//
// Accesibilidad WCAG 2.1 AA:
//   * Foco visible y navegación por teclado en cada control.
//   * Labels asociados (`for`/`id`) y mensajes de error con
//     `aria-describedby`.
//   * Regiones `aria-live="polite"` para resultados y
//     `aria-live="assertive"` para errores de servidor.
//   * Tablas con `<caption>`, cabeceras `<th scope="col">` y resumen
//     accesible.
//   * Contraste mínimo 4.5:1 delegado al tema PIIP (`piip-theme.scss`).
//   * Atajo `Escape` revierte los cambios pendientes sin enviar al
//     backend.

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal } from '@angular/core';
import {
  AbstractControl,
  FormArray,
  FormBuilder,
  FormControl,
  FormGroup,
  ReactiveFormsModule,
  ValidationErrors,
  Validators
} from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTableModule } from '@angular/material/table';
import { firstValueFrom } from 'rxjs';

import { parseProblemDetails, ProblemDetails, ProblemViolation } from '../../../core/http/problem-details';
import { EffectiveAssignmentSelectorComponent } from '../../../core/effective-assignment/effective-assignment-selector.component';
import { SeguridadApiService } from '../api/seguridad-api.service';
import {
  MatrixCombination,
  MatrixCombinationRequest,
  MatrixDeactivationRequest,
  MatrixFunctionRequest,
  MatrixVersionDetail,
  MatrixVersionRequest
} from '../api/types';

interface FuncionFormGroup {
  codigo: FormControl<string>;
  descripcion: FormControl<string>;
}

interface CombinacionFormGroup {
  funcionCodigo: FormControl<string>;
  perfil: FormControl<string>;
  unidadId: FormControl<number>;
  vigenteDesde: FormControl<string>;
  vigenteHasta: FormControl<string>;
  documentoAprobacionVersionId: FormControl<number | null>;
  aprobadorUsuarioId: FormControl<number | null>;
}

interface VersionFormGroup {
  codigoVersion: FormControl<string>;
  versionAnteriorId: FormControl<number | null>;
  vigenteDesde: FormControl<string>;
  vigenteHasta: FormControl<string>;
  documentoAprobacionVersionId: FormControl<number | null>;
  funciones: FormArray<FormGroup<FuncionFormGroup>>;
  combinaciones: FormArray<FormGroup<CombinacionFormGroup>>;
}

interface InactivacionFormGroup {
  combinacionId: FormControl<number | null>;
  codigoNuevaVersion: FormControl<string>;
  documentoAprobacionVersionId: FormControl<number | null>;
  aprobadorUsuarioId: FormControl<number | null>;
  motivo: FormControl<string>;
}

const MAX_CODIGO_VERSION = 30;
const MAX_CODIGO_FUNCION = 30;
const MAX_DESCRIPCION_FUNCION = 500;
const MAX_PERFIL = 50;
const MAX_MOTIVO = 2000;
const ID_FORM_VERSION = 'matrix-version-form';
const ID_FORM_INACTIVACION = 'matrix-deactivation-form';
const ID_VERSION_CODIGO = 'matrix-version-codigo';
const ID_VERSION_DOCUMENTO = 'matrix-version-documento';
const ID_VERSION_FECHA_INICIO = 'matrix-version-vigente-desde';
const ID_VERSION_FECHA_FIN = 'matrix-version-vigente-hasta';
const ID_VERSION_ANTERIOR = 'matrix-version-anterior';
const ID_INACT_COMBINACION = 'matrix-deactivation-combinacion';
const ID_INACT_VERSION = 'matrix-deactivation-version';
const ID_INACT_DOCUMENTO = 'matrix-deactivation-documento';
const ID_INACT_APROBADOR = 'matrix-deactivation-aprobador';
const ID_INACT_MOTIVO = 'matrix-deactivation-motivo';

@Component({
  selector: 'app-matrix-administration',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTableModule,
    EffectiveAssignmentSelectorComponent
  ],
  templateUrl: './matrix-administration.component.html',
  styleUrl: './matrix-administration.component.scss'
})
export class MatrixAdministrationComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(SeguridadApiService);

  protected readonly formVersionId = ID_FORM_VERSION;
  protected readonly formInactivacionId = ID_FORM_INACTIVACION;
  protected readonly idVersionCodigo = ID_VERSION_CODIGO;
  protected readonly idVersionDocumento = ID_VERSION_DOCUMENTO;
  protected readonly idVersionFechaInicio = ID_VERSION_FECHA_INICIO;
  protected readonly idVersionFechaFin = ID_VERSION_FECHA_FIN;
  protected readonly idVersionAnterior = ID_VERSION_ANTERIOR;
  protected readonly idInactCombinacion = ID_INACT_COMBINACION;
  protected readonly idInactVersion = ID_INACT_VERSION;
  protected readonly idInactDocumento = ID_INACT_DOCUMENTO;
  protected readonly idInactAprobador = ID_INACT_APROBADOR;
  protected readonly idInactMotivo = ID_INACT_MOTIVO;

  protected readonly columnasVersiones: readonly string[] = ['codigo', 'vigencia', 'funciones', 'combinaciones', 'acciones'];

  protected readonly formVersion: FormGroup<VersionFormGroup> = this.buildVersion();
  protected readonly formInactivacion: FormGroup<InactivacionFormGroup> = this.buildInactivacion();

  protected readonly submittedVersion = signal(false);
  protected readonly submittedInactivacion = signal(false);
  protected readonly submittingVersion = signal(false);
  protected readonly submittingInactivacion = signal(false);
  protected readonly submittingListado = signal(false);

  protected readonly problema = signal<ProblemDetails | undefined>(undefined);
  protected readonly versiones = signal<readonly MatrixVersionDetail[]>([]);
  protected readonly combinacionesSeleccionadas = signal<readonly MatrixCombination[]>([]);
  protected readonly funcionesCatalogo = signal<readonly { readonly codigo: string; readonly descripcion: string }[]>([]);
  protected readonly totalVersiones = signal<number>(0);
  protected readonly paginaActual = signal<number>(0);
  protected readonly tamanioPagina = signal<number>(20);

  protected readonly funciones = this.formVersion.controls.funciones;
  protected readonly combinaciones = this.formVersion.controls.combinaciones;
  protected readonly hayCombinaciones = computed(() => this.combinacionesSeleccionadas().length > 0);

  ngOnInit(): void {
    this.asegurarFilaInicial();
    void this.cargarVersiones();
    void this.cargarFunciones();
  }

  // ---------------------------------------------------------------------------
  // Listado y consultas
  // ---------------------------------------------------------------------------

  protected async cargarVersiones(): Promise<void> {
    this.submittingListado.set(true);
    this.problema.set(undefined);
    try {
      const pagina = await firstValueFrom(this.api.listarVersionesMatriz(this.paginaActual(), this.tamanioPagina()));
      this.versiones.set(pagina.content);
      this.totalVersiones.set(pagina.totalElements);
    } catch (error: unknown) {
      this.manejarError(error);
    } finally {
      this.submittingListado.set(false);
    }
  }

  protected async cargarFunciones(): Promise<void> {
    try {
      const funciones = await firstValueFrom(this.api.listarFunciones());
      this.funcionesCatalogo.set(funciones.map((funcion) => Object.freeze({ codigo: funcion.codigo, descripcion: funcion.descripcion })));
    } catch (error: unknown) {
      this.manejarError(error);
    }
  }

  protected async verCombinaciones(version: MatrixVersionDetail): Promise<void> {
    if (!version.id) {
      this.combinacionesSeleccionadas.set(version.combinaciones);
      return;
    }
    try {
      const combinaciones = await firstValueFrom(this.api.listarCombinacionesMatriz(version.id));
      this.combinacionesSeleccionadas.set(combinaciones);
    } catch (error: unknown) {
      this.manejarError(error);
    }
  }

  protected paginaSiguiente(): void {
    const totalPaginas = Math.max(1, Math.ceil(this.totalVersiones() / this.tamanioPagina()));
    if (this.paginaActual() + 1 < totalPaginas) {
      this.paginaActual.set(this.paginaActual() + 1);
      void this.cargarVersiones();
    }
  }

  protected paginaAnterior(): void {
    if (this.paginaActual() > 0) {
      this.paginaActual.set(this.paginaActual() - 1);
      void this.cargarVersiones();
    }
  }

  // ---------------------------------------------------------------------------
  // Alta de versión
  // ---------------------------------------------------------------------------

  protected agregarFuncion(): void {
    this.funciones.push(
      this.fb.group<FuncionFormGroup>({
        codigo: this.fb.nonNullable.control('', { validators: [Validators.required, Validators.maxLength(MAX_CODIGO_FUNCION)] }),
        descripcion: this.fb.nonNullable.control('', { validators: [Validators.required, Validators.maxLength(MAX_DESCRIPCION_FUNCION)] })
      })
    );
  }

  protected removerFuncion(indice: number): void {
    this.funciones.removeAt(indice);
  }

  protected agregarCombinacion(): void {
    this.combinaciones.push(
      this.fb.group<CombinacionFormGroup>({
        funcionCodigo: this.fb.nonNullable.control('', { validators: [Validators.required, Validators.maxLength(MAX_CODIGO_FUNCION)] }),
        perfil: this.fb.nonNullable.control('', { validators: [Validators.required, Validators.maxLength(MAX_PERFIL)] }),
        unidadId: this.fb.nonNullable.control<number>(0, { validators: [Validators.required, Validators.min(1)] }),
        vigenteDesde: this.fb.nonNullable.control('', { validators: [Validators.required] }),
        vigenteHasta: this.fb.nonNullable.control(''),
        documentoAprobacionVersionId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
        aprobadorUsuarioId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] })
      })
    );
  }

  protected removerCombinacion(indice: number): void {
    this.combinaciones.removeAt(indice);
  }

  protected async crearVersion(): Promise<void> {
    this.submittedVersion.set(true);
    this.problema.set(undefined);
    if (this.formVersion.invalid || this.funciones.length === 0 || this.combinaciones.length === 0) {
      this.funciones.markAllAsTouched();
      this.combinaciones.markAllAsTouched();
      this.formVersion.markAllAsTouched();
      return;
    }
    const raw = this.formVersion.getRawValue();
    if (raw.documentoAprobacionVersionId === null) {
      this.formVersion.controls.documentoAprobacionVersionId.markAsTouched();
      return;
    }
    const payload: MatrixVersionRequest = {
      codigoVersion: raw.codigoVersion,
      versionAnteriorId: typeof raw.versionAnteriorId === 'number' ? raw.versionAnteriorId : undefined,
      vigenteDesde: raw.vigenteDesde,
      vigenteHasta: this.normalizeOptionalDate(raw.vigenteHasta),
      documentoAprobacionVersionId: raw.documentoAprobacionVersionId,
      funciones: raw.funciones.map<MatrixFunctionRequest>((funcion) => ({
        codigo: funcion.codigo,
        descripcion: funcion.descripcion
      })),
      combinaciones: raw.combinaciones.map<MatrixCombinationRequest>((combinacion) => {
        if (combinacion.unidadId === 0 || combinacion.documentoAprobacionVersionId === null || combinacion.aprobadorUsuarioId === null) {
          throw new Error('La combinación requiere unidad, documento y aprobador.');
        }
        return {
          funcionCodigo: combinacion.funcionCodigo,
          perfil: combinacion.perfil,
          unidadId: combinacion.unidadId,
          vigenteDesde: combinacion.vigenteDesde,
          vigenteHasta: this.normalizeOptionalDate(combinacion.vigenteHasta),
          documentoAprobacionVersionId: combinacion.documentoAprobacionVersionId,
          aprobadorUsuarioId: combinacion.aprobadorUsuarioId
        };
      })
    };
    this.submittingVersion.set(true);
    try {
      const version = await firstValueFrom(this.api.crearVersionMatriz(payload));
      this.problema.set(undefined);
      this.submittedVersion.set(false);
      this.formVersion.reset({ codigoVersion: '', versionAnteriorId: null, vigenteDesde: '', vigenteHasta: '', documentoAprobacionVersionId: null });
      this.funciones.clear();
      this.combinaciones.clear();
      this.asegurarFilaInicial();
      this.versiones.set([version, ...this.versiones()]);
      this.totalVersiones.set(this.totalVersiones() + 1);
    } catch (error: unknown) {
      this.manejarError(error);
    } finally {
      this.submittingVersion.set(false);
    }
  }

  protected cancelarVersion(): void {
    this.formVersion.reset({ codigoVersion: '', versionAnteriorId: null, vigenteDesde: '', vigenteHasta: '', documentoAprobacionVersionId: null });
    this.funciones.clear();
    this.combinaciones.clear();
    this.asegurarFilaInicial();
    this.problema.set(undefined);
    this.submittedVersion.set(false);
  }

  // ---------------------------------------------------------------------------
  // Inactivación
  // ---------------------------------------------------------------------------

  protected async inactivarCombinacion(): Promise<void> {
    this.submittedInactivacion.set(true);
    this.problema.set(undefined);
    if (this.formInactivacion.invalid) {
      this.formInactivacion.markAllAsTouched();
      return;
    }
    const raw = this.formInactivacion.getRawValue();
    if (
      raw.combinacionId === null ||
      raw.documentoAprobacionVersionId === null ||
      raw.aprobadorUsuarioId === null
    ) {
      this.formInactivacion.markAllAsTouched();
      return;
    }
    const payload: MatrixDeactivationRequest = {
      codigoNuevaVersion: raw.codigoNuevaVersion,
      documentoAprobacionVersionId: raw.documentoAprobacionVersionId,
      aprobadorUsuarioId: raw.aprobadorUsuarioId,
      motivo: raw.motivo.trim()
    };
    this.submittingInactivacion.set(true);
    try {
      const version = await firstValueFrom(this.api.inactivarCombinacionMatriz(raw.combinacionId, payload));
      this.problema.set(undefined);
      this.submittedInactivacion.set(false);
      this.formInactivacion.reset({ combinacionId: null, codigoNuevaVersion: '', documentoAprobacionVersionId: null, aprobadorUsuarioId: null, motivo: '' });
      this.versiones.set([version, ...this.versiones()]);
      this.totalVersiones.set(this.totalVersiones() + 1);
    } catch (error: unknown) {
      this.manejarError(error);
    } finally {
      this.submittingInactivacion.set(false);
    }
  }

  protected cancelarInactivacion(): void {
    this.formInactivacion.reset({ combinacionId: null, codigoNuevaVersion: '', documentoAprobacionVersionId: null, aprobadorUsuarioId: null, motivo: '' });
    this.problema.set(undefined);
    this.submittedInactivacion.set(false);
  }

  // ---------------------------------------------------------------------------
  // Helpers accesibles
  // ---------------------------------------------------------------------------

  protected mensajeError(
    control: AbstractControl<unknown, unknown> | null,
    submitted: boolean
  ): string | null {
    if (!control || !(control.touched || submitted)) {
      return null;
    }
    const errors: ValidationErrors | null = control.errors;
    if (!errors) {
      return null;
    }
    if (errors['required']) {
      return 'Este campo es obligatorio.';
    }
    if (errors['maxlength']) {
      const requerido = errors['maxlength'] as { requiredLength: number };
      return `Máximo ${requerido.requiredLength} caracteres.`;
    }
    if (errors['min']) {
      const requerido = errors['min'] as { min: number };
      return `Debe ser mayor o igual a ${requerido.min}.`;
    }
    return null;
  }

  protected violacionesPara(campo: string): readonly ProblemViolation[] {
    const problema = this.problema();
    if (!problema) {
      return [];
    }
    return problema.violations.filter((violation: ProblemViolation) => violation.field === campo);
  }

  // ---------------------------------------------------------------------------
  // Internos
  // ---------------------------------------------------------------------------

  private buildVersion(): FormGroup<VersionFormGroup> {
    return this.fb.group<VersionFormGroup>({
      codigoVersion: this.fb.nonNullable.control('', {
        validators: [Validators.required, Validators.maxLength(MAX_CODIGO_VERSION)]
      }),
      versionAnteriorId: this.fb.control<number | null>(null, { validators: [Validators.min(1)] }),
      vigenteDesde: this.fb.nonNullable.control('', { validators: [Validators.required] }),
      vigenteHasta: this.fb.nonNullable.control(''),
      documentoAprobacionVersionId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      funciones: this.fb.array<FormGroup<FuncionFormGroup>>([]),
      combinaciones: this.fb.array<FormGroup<CombinacionFormGroup>>([])
    });
  }

  private buildInactivacion(): FormGroup<InactivacionFormGroup> {
    return this.fb.group<InactivacionFormGroup>({
      combinacionId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      codigoNuevaVersion: this.fb.nonNullable.control('', {
        validators: [Validators.required, Validators.maxLength(MAX_CODIGO_VERSION)]
      }),
      documentoAprobacionVersionId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      aprobadorUsuarioId: this.fb.control<number | null>(null, { validators: [Validators.required, Validators.min(1)] }),
      motivo: this.fb.nonNullable.control('', { validators: [Validators.required, Validators.maxLength(MAX_MOTIVO)] })
    });
  }

  private asegurarFilaInicial(): void {
    if (this.funciones.length === 0) {
      this.agregarFuncion();
    }
    if (this.combinaciones.length === 0) {
      this.agregarCombinacion();
    }
  }

  private normalizeOptionalDate(value: string): string | undefined {
    const trimmed = value?.trim();
    return trimmed ? trimmed : undefined;
  }

  private manejarError(error: unknown): void {
    this.problema.set(parseProblemDetails(error));
    this.submittingVersion.set(false);
    this.submittingInactivacion.set(false);
  }
}
