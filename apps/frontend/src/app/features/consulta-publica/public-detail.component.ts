// Página de detalle público del portafolio (US7 - T101).
//
// La consulta pública es ANÓNIMA: el cliente NO envía credenciales, no
// conoce ni la identidad ni la asignación efectiva del visitante, y
// los DTOs del cliente `PublicQueryApiService` están limitados a la
// allowlist canónica (`tipoRegistro`, `codigo`, `nombre`, `estado` y
// metadatos descriptivos de publicaciones elegibles).
//
// Reglas aplicadas:
//   * Esta página NO expone contenido ni habilita descarga. La
//     Constitución y la especificación lo prohíben: la consulta
//     pública es un instrumento de transparencia sin acceso a BLOB ni
//     a URL firmada.
//   * Si el registro no es elegible, el backend responde 404 sin
//     confirmar su existencia. La UI muestra un anuncio genérico.
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
//   * Ausencia deliberada de elementos `download`, enlaces a
//     `/api/v1/documentos/{id}/contenido` o URL firmadas; el componente
//     no puede invocar ni generar contenido descargable.

import { CommonModule } from '@angular/common';
import { ChangeDetectionStrategy, Component, Input, OnInit, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { firstValueFrom } from 'rxjs';

import { parseProblemDetails, ProblemDetails } from '../../core/http/problem-details';
import { PublicQueryApiService } from './api/public-query-api.service';
import {
  PublicPortfolioDetail,
  PublicPortfolioDocumento,
  PublicTipoRegistro
} from './api/types';

const ID_MAIN = 'public-detail-main';
const ID_RESULTS_REGION = 'public-detail-results';
const ID_ERROR_REGION = 'public-detail-errors';
const ID_PUBLICACIONES = 'public-detail-publicaciones';

const EMPTY_DETAIL: PublicPortfolioDetail = Object.freeze({
  id: 0,
  tipoRegistro: 'INICIATIVA',
  codigo: '',
  nombre: '',
  estado: '',
  publicaciones: Object.freeze([]),
  etag: ''
});

@Component({
  selector: 'app-public-portfolio-detail',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './public-detail.component.html',
  styleUrl: './public-detail.component.scss'
})
export class PublicDetailComponent implements OnInit {
  private readonly api = inject(PublicQueryApiService);
  private readonly route = inject(ActivatedRoute);

  protected readonly idMain = ID_MAIN;
  protected readonly idResultsRegion = ID_RESULTS_REGION;
  protected readonly idErrorRegion = ID_ERROR_REGION;
  protected readonly idPublicaciones = ID_PUBLICACIONES;

  @Input() set idRegistro(value: string | number | null | undefined) {
    if (value === null || value === undefined || value === '') {
      this.registroIdSignal.set(undefined);
      return;
    }
    const texto = String(value).trim();
    this.registroIdSignal.set(texto === '' ? undefined : texto);
  }

  protected readonly registroIdSignal = signal<string | undefined>(undefined);
  protected readonly detalle = signal<PublicPortfolioDetail>(EMPTY_DETAIL);
  protected readonly etagActual = signal<string | undefined>(undefined);
  protected readonly cargando = signal(false);
  protected readonly problema = signal<ProblemDetails | undefined>(undefined);
  protected readonly notModified = signal(false);

  protected readonly publicacionesVisibles = computed(() => this.detalle().publicaciones.length > 0);

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
        detail: 'No se proporcionó un identificador de registro público.',
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
        detail: 'El identificador público debe ser un entero positivo.',
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
        return;
      }
      this.detalle.set(respuesta.body);
      this.etagActual.set(respuesta.etag ?? respuesta.body.etag);
    } catch (error: unknown) {
      this.problema.set(parseProblemDetails(error) ?? fallbackProblem('No se pudo cargar el detalle público.'));
    } finally {
      this.cargando.set(false);
    }
  }

  protected async reintentar(): Promise<void> {
    await this.refrescar();
  }

  // ---------------------------------------------------------------------------
  // Proyecciones accesibles (WCAG 2.1 AA)
  // ---------------------------------------------------------------------------

  protected descripcionTipo(tipo: PublicTipoRegistro): string {
    return tipo === 'INICIATIVA' ? 'Iniciativa' : 'Proyecto';
  }

  protected trackPublicacion(_: number, publicacion: PublicPortfolioDocumento): string {
    return `${publicacion.tipoDocumental}-${publicacion.version}-${publicacion.fechaPublicacion}`;
  }

  protected mensajeError(): string | null {
    const problema = this.problema();
    if (!problema) {
      return null;
    }
    if (problema.status === 404) {
      return 'El registro no es elegible para consulta pública o no está visible.';
    }
    if (problema.status === 400) {
      return problema.detail ?? 'La solicitud es inválida.';
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
