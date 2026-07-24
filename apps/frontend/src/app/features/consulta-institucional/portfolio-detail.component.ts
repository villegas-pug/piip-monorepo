// Página de detalle institucional del portafolio (US7 - T101).
//
// Esta pantalla muestra el detalle de un registro del portafolio
// dentro del ámbito del actor institucional. La autorización efectiva
// la conserva el backend: el cliente se limita a presentar la
// información que el servidor ya aplicó mediante la matriz de
// privacidad y la asignación efectiva.
//
// Reglas aplicadas:
//   * Los DTOs son los del cliente `InstitutionalQueryApiService`; no
//     se reutilizan con la consulta pública.
//   * La página NO descarga contenido documental: solo se exponen los
//     metadatos. El contenido se solicita por
//     `GET /api/v1/documentos/{id}/contenido` con la revalidación
//     obligatoria de ámbito y clasificación; la habilitación depende
//     de `puedeConsultarContenido` declarado por el backend.
//   * `participantes` solo se renderiza cuando el backend lo incluye;
//     el cliente NO fuerza su visibilidad.
//   * Los errores se traducen desde `application/problem+json`
//     mediante `parseProblemDetails`; el cliente NO decide códigos
//     canónicos.
//
// Accesibilidad WCAG 2.1 AA:
//   * Skip link para saltar al contenido principal.
//   * `<main tabindex="-1">` con foco programático tras la navegación.
//   * `aria-live="polite"` para resultados y `aria-live="assertive"`
//     para errores.
//   * Encabezados jerárquicos y secciones con `aria-labelledby`.
//   * Tablas con `<caption>`, `scope="col"` y resumen.
//   * Estados con `aria-busy` y mensajes de error anunciados.
//   * Navegación completa por teclado y foco visible (tema PIIP).
//   * Sin dependencia del color para distinguir estados.

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { firstValueFrom } from 'rxjs';

import { parseProblemDetails, ProblemDetails } from '../../core/http/problem-details';
import { EffectiveAssignmentSelectorComponent } from '../../core/effective-assignment/effective-assignment-selector.component';
import { InstitutionalQueryApiService } from './api/institutional-query-api.service';
import {
  InstitutionalClasificacionDocumento,
  InstitutionalEstadoIniciativa,
  InstitutionalFuenteOrigen,
  InstitutionalPortfolioDetail,
  InstitutionalPortfolioDocument,
  InstitutionalPortfolioHistoryEntry,
  InstitutionalPortfolioPersonaParticipante,
  InstitutionalPortfolioUnidad,
  InstitutionalTipoRegistro,
  InstitutionalTipoSolucion
} from './api/types';

const ID_MAIN = 'institutional-detail-main';
const ID_RESULTS_REGION = 'institutional-detail-results';
const ID_ERROR_REGION = 'institutional-detail-errors';
const ID_CABECERA = 'institutional-detail-cabecera';
const ID_IDENTIFICACION = 'institutional-detail-identificacion';
const ID_DESCRIPCION = 'institutional-detail-descripcion';
const ID_UNIDADES = 'institutional-detail-unidades';
const ID_PARTICIPANTES = 'institutional-detail-participantes';
const ID_DOCUMENTOS = 'institutional-detail-documentos';
const ID_HISTORIAL = 'institutional-detail-historial';
const ID_RELACION = 'institutional-detail-relacion';

const EMPTY_DETAIL: InstitutionalPortfolioDetail = Object.freeze({
  id: 0,
  tipoRegistro: 'INICIATIVA',
  codigo: '',
  fechaInicio: '',
  nombre: '',
  estado: 'PRESENTADO',
  unidades: Object.freeze([]),
  participantes: Object.freeze([]),
  documentos: Object.freeze([]),
  historial: Object.freeze([]),
  actorEsResponsable: false,
  actorEsEvaluador: false,
  actorEsAdministrador: false,
  version: 0,
  etag: ''
});

@Component({
  selector: 'app-institutional-portfolio-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    CommonModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    EffectiveAssignmentSelectorComponent
  ],
  templateUrl: './portfolio-detail.component.html',
  styleUrl: './portfolio-detail.component.scss'
})
export class PortfolioDetailComponent implements OnInit {
  private readonly api = inject(InstitutionalQueryApiService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  protected readonly idMain = ID_MAIN;
  protected readonly idResultsRegion = ID_RESULTS_REGION;
  protected readonly idErrorRegion = ID_ERROR_REGION;
  protected readonly idCabecera = ID_CABECERA;
  protected readonly idIdentificacion = ID_IDENTIFICACION;
  protected readonly idDescripcion = ID_DESCRIPCION;
  protected readonly idUnidades = ID_UNIDADES;
  protected readonly idParticipantes = ID_PARTICIPANTES;
  protected readonly idDocumentos = ID_DOCUMENTOS;
  protected readonly idHistorial = ID_HISTORIAL;
  protected readonly idRelacion = ID_RELACION;

  @Input() set idRegistro(value: string | number | null | undefined) {
    if (value === null || value === undefined || value === '') {
      this.registroIdSignal.set(undefined);
      return;
    }
    const texto = String(value).trim();
    if (texto === '') {
      this.registroIdSignal.set(undefined);
      return;
    }
    this.registroIdSignal.set(texto);
  }

  protected readonly registroIdSignal = signal<string | undefined>(undefined);
  protected readonly detalle = signal<InstitutionalPortfolioDetail>(EMPTY_DETAIL);
  protected readonly etagActual = signal<string | undefined>(undefined);
  protected readonly cargando = signal(false);
  protected readonly problema = signal<ProblemDetails | undefined>(undefined);
  protected readonly correlationId = signal<string | undefined>(undefined);
  protected readonly notModified = signal(false);

  protected readonly participantesVisibles = computed(() => this.detalle().participantes.length > 0);
  protected readonly unidadesVisibles = computed(() => this.detalle().unidades.length > 0);
  protected readonly documentosVisibles = computed(() => this.detalle().documentos.length > 0);
  protected readonly historialVisible = computed(() => this.detalle().historial.length > 0);
  protected readonly relacionVisible = computed(() => this.detalle().relacion !== undefined);

  async ngOnInit(): Promise<void> {
    const desdeRuta = this.route.snapshot.paramMap.get('id');
    if (desdeRuta && !this.registroIdSignal()) {
      this.registroIdSignal.set(desdeRuta);
    }
    await this.refrescar();
  }

  protected async refrescar(): Promise<void> {
    const idTexto = this.registroIdSignal();
    if (!idTexto) {
      this.problema.set({
        type: 'about:blank',
        title: 'Identificador ausente',
        status: 400,
        detail: 'No se proporcionó un identificador de registro.',
        violations: []
      });
      return;
    }
    const idNumerico = Number(idTexto);
    if (!Number.isInteger(idNumerico) || idNumerico <= 0) {
      this.problema.set({
        type: 'about:blank',
        title: 'Identificador inválido',
        status: 400,
        detail: 'El identificador de registro debe ser un entero positivo.',
        violations: []
      });
      return;
    }
    this.cargando.set(true);
    this.problema.set(undefined);
    this.notModified.set(false);
    try {
      const respuesta = await firstValueFrom(
        this.api.detalle(idNumerico, { ifNoneMatch: this.etagActual() })
      );
      if (respuesta.notModified) {
        this.notModified.set(true);
        this.correlationId.set(undefined);
        return;
      }
      this.detalle.set(respuesta.body);
      this.etagActual.set(respuesta.etag ?? respuesta.body.etag);
      this.correlationId.set(respuesta.correlationId);
    } catch (error: unknown) {
      this.problema.set(parseProblemDetails(error) ?? fallbackProblem('No se pudo cargar el detalle institucional.'));
      this.correlationId.set(undefined);
    } finally {
      this.cargando.set(false);
    }
  }

  protected async reintentar(): Promise<void> {
    await this.refrescar();
  }

  protected cancelar(): void {
    void this.router.navigate(['/institucional']);
  }

  // ---------------------------------------------------------------------------
  // Proyecciones accesibles (WCAG 2.1 AA)
  // ---------------------------------------------------------------------------

  protected descripcionEstado(estado: InstitutionalEstadoIniciativa): string {
    return estado.replace(/_/g, ' ').toLowerCase();
  }

  protected descripcionTipo(tipo: InstitutionalTipoRegistro): string {
    return tipo === 'INICIATIVA' ? 'Iniciativa' : 'Proyecto';
  }

  protected descripcionSolucion(tipo?: InstitutionalTipoSolucion): string {
    if (!tipo) {
      return 'No especificado';
    }
    return tipo === 'POTENCIAL_ADAPTABLE' ? 'Potencial adaptable' : 'Por definir';
  }

  protected descripcionFuente(fuente?: InstitutionalFuenteOrigen, detalle?: string): string {
    if (!fuente) {
      return 'No especificada';
    }
    if (fuente === 'OTROS' && detalle) {
      return `${fuente} · ${detalle}`;
    }
    return fuente.replace(/_/g, ' ').toLowerCase();
  }

  protected descripcionClasificacion(clasificacion?: InstitutionalClasificacionDocumento): string {
    if (!clasificacion) {
      return 'Sin clasificar';
    }
    return clasificacion.toLowerCase();
  }

  protected trackUnidad(_: number, unidad: InstitutionalPortfolioUnidad): number {
    return unidad.id ?? unidad.unidadId;
  }

  protected trackPersona(_: number, persona: InstitutionalPortfolioPersonaParticipante): number {
    return persona.idParticipacion ?? persona.participanteId ?? persona.usuarioId ?? 0;
  }

  protected trackDocumento(_: number, documento: InstitutionalPortfolioDocument): number {
    return documento.documentoId;
  }

  protected trackHistorial(index: number, entrada: InstitutionalPortfolioHistoryEntry): number {
    return entrada.transicionId ?? index;
  }

  protected puedeVerResponsable(): boolean {
    // El detalle institucional solo incluye `responsableId` cuando el
    // actor tiene autorización efectiva; la omisión por parte del
    // backend es la decisión autoritativa, no una comprobación local.
    return this.detalle().responsableId !== undefined;
  }

  protected mensajeError(): string | null {
    const problema = this.problema();
    if (!problema) {
      return null;
    }
    if (problema.status === 404) {
      return 'El registro no pertenece a su ámbito o no está visible. No se confirma su existencia.';
    }
    if (problema.status === 403) {
      return 'No tiene permiso para consultar este registro dentro de su ámbito efectivo.';
    }
    if (problema.status === 412) {
      return 'Otro usuario modificó este registro. Actualice la información y reintente.';
    }
    if (problema.status === 401) {
      return 'Su sesión institucional expiró o no seleccionó una asignación efectiva.';
    }
    if (problema.status === 304) {
      return 'El detalle no cambió desde su última lectura.';
    }
    return problema.detail ?? problema.title;
  }
}

function fallbackProblem(detail: string): ProblemDetails {
  return Object.freeze({
    type: 'about:blank',
    title: 'Detalle no disponible',
    status: 0,
    detail,
    violations: Object.freeze([])
  });
}
