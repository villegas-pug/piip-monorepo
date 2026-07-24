package pe.gob.midagri.piip.consulta.mapper;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import pe.gob.midagri.piip.consulta.dto.ClasificacionDocumentoConsulta;
import pe.gob.midagri.piip.consulta.dto.EstadoIniciativaConsulta;
import pe.gob.midagri.piip.consulta.dto.FuenteOrigenConsulta;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioDetail;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioDetail.IniciativaProyectoRelacion;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioDocument;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioHistoryEntry;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioPage;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioPersonaParticipante;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioSummary;
import pe.gob.midagri.piip.consulta.dto.InstitutionalPortfolioUnidad;
import pe.gob.midagri.piip.consulta.dto.TipoRegistroConsulta;
import pe.gob.midagri.piip.consulta.dto.TipoSolucionConsulta;
import pe.gob.midagri.piip.documentos.dto.DocumentoInstitucionalMetadata;
import pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioProjection;
import pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioProjection.InstitutionalParticipanteProjection;
import pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioProjection.InstitutionalTransitionProjection;
import pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioProjection.InstitutionalUnidadProjection;

/**
 * Conversor entre las proyecciones internas (módulos
 * {@code portafolio} y {@code documentos}) y los DTOs públicos
 * del módulo {@code consulta}. La clase encapsula las decisiones
 * de privacidad: omite campos sensibles para actores que no
 * sean Responsable, Evaluador o administrador autorizado y
 * filtra los documentos por clasificación validada.
 */
public final class InstitutionalPortfolioMapper {

    private InstitutionalPortfolioMapper() {
    }

    /**
     * Convierte la página entregada por {@code portafolio} en
     * la página canónica del módulo {@code consulta}, aplicando
     * la privacidad por ámbito.
     */
    public static InstitutionalPortfolioPage aPagina(
            pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioPage origen,
            boolean actorEsResponsable, boolean actorEsEvaluador, boolean actorEsAdministrador) {
        List<InstitutionalPortfolioSummary> items = origen.items().stream()
                .map(resumen -> aResumen(resumen, actorEsResponsable, actorEsEvaluador, actorEsAdministrador))
                .toList();
        String etag = calcularEtagPagina(items);
        return new InstitutionalPortfolioPage(items, origen.pagina(), origen.tamanio(),
                origen.totalElementos(), origen.totalPaginas(), etag);
    }

    /**
     * Convierte una proyección completa del portafolio y la lista
     * de metadatos documentales en el detalle institucional
     * visible para el actor.
     */
    public static InstitutionalPortfolioDetail aDetalle(
            InstitutionalPortfolioProjection origen,
            List<DocumentoInstitucionalMetadata> documentos,
            boolean actorEsResponsable, boolean actorEsEvaluador, boolean actorEsAdministrador,
            String unidadEjecutoraDescripcion, String unidadEjecutoraAbreviatura) {
        List<InstitutionalPortfolioUnidad> unidades = origen.unidades().stream()
                .map(InstitutionalPortfolioMapper::aUnidad)
                .toList();
        boolean incluirPersonas = actorEsResponsable || actorEsEvaluador || actorEsAdministrador;
        List<InstitutionalPortfolioPersonaParticipante> participantes = incluirPersonas
                ? origen.participantesPersona().stream()
                        .map(InstitutionalPortfolioMapper::aParticipante)
                        .toList()
                : List.of();
        List<InstitutionalPortfolioDocument> documentosVisibles = documentos.stream()
                .map(InstitutionalPortfolioMapper::aDocumento)
                .toList();
        List<InstitutionalPortfolioHistoryEntry> historial = origen.transiciones().stream()
                .map(InstitutionalPortfolioMapper::aHistory)
                .toList();
        IniciativaProyectoRelacion relacion = origen.iniciativaOrigenId() == null
                && origen.proyectoDerivadoId() == null
                ? null
                : new IniciativaProyectoRelacion(
                        origen.iniciativaOrigenId(),
                        origen.proyectoDerivadoId(),
                        origen.iniciativaOrigenId() != null
                                && origen.iniciativaOrigenId().equals(origen.id()),
                        origen.proyectoDerivadoId() != null
                                && origen.proyectoDerivadoId().equals(origen.id()));
        String etag = "\"" + origen.id() + "-" + origen.version() + "\"";
        return new InstitutionalPortfolioDetail(
                origen.id(),
                aTipoRegistro(origen.tipoRegistro()),
                origen.codigo(),
                origen.codigoOrigen(),
                origen.fechaInicio(),
                origen.fechaCierre(),
                origen.nombre(),
                aTipoSolucion(origen.tipoSolucion()),
                aFuente(origen.fuenteOrigen()),
                origen.detalleFuente(),
                origen.responsableId(),
                origen.problemaPublico(),
                origen.solucionPropuesta(),
                origen.objetivoPeiId(),
                origen.actividadPoiId(),
                origen.unidadEjecutoraId(),
                unidadEjecutoraDescripcion,
                unidadEjecutoraAbreviatura,
                aEstado(origen.estado()),
                origen.componenteDigital() == null ? null : "S".equals(origen.componenteDigital()),
                origen.detalleComponenteDigital(),
                origen.nota(),
                origen.resultadosClave(),
                unidades,
                participantes,
                documentosVisibles,
                historial,
                relacion,
                actorEsResponsable,
                actorEsEvaluador,
                actorEsAdministrador,
                origen.fechaCreacion(),
                origen.version(),
                etag);
    }

    private static InstitutionalPortfolioSummary aResumen(pe.gob.midagri.piip.portafolio.dto.InstitutionalPortfolioPage.InstitutionalPortfolioSummary origen,
            boolean actorEsResponsable, boolean actorEsEvaluador, boolean actorEsAdministrador) {
        boolean puedeVerResponsable = actorEsResponsable || actorEsEvaluador || actorEsAdministrador;
        Long responsable = puedeVerResponsable ? origen.responsableId() : null;
        return new InstitutionalPortfolioSummary(
                origen.id(),
                aTipoRegistro(origen.tipoRegistro()),
                origen.codigo(),
                origen.codigoOrigen(),
                origen.nombre(),
                origen.estado(),
                origen.fechaInicio(),
                origen.unidadEjecutoraId(),
                origen.unidadEjecutoraDescripcion(),
                origen.unidadEjecutoraAbreviatura(),
                responsable,
                puedeVerResponsable,
                origen.version(),
                origen.etag());
    }

    private static InstitutionalPortfolioUnidad aUnidad(InstitutionalUnidadProjection origen) {
        boolean principal = origen.nroOrden() != null && origen.nroOrden() == 1;
        return new InstitutionalPortfolioUnidad(
                origen.id(),
                origen.unidadId(),
                origen.descripcion(),
                origen.abreviatura(),
                origen.nroOrden(),
                principal);
    }

    private static InstitutionalPortfolioPersonaParticipante aParticipante(
            InstitutionalParticipanteProjection origen) {
        boolean vigente = origen.fin() == null;
        return new InstitutionalPortfolioPersonaParticipante(
                origen.idParticipacion(),
                origen.participanteId(),
                origen.usuarioId(),
                origen.nombresCompletos(),
                origen.institucion(),
                origen.funcion(),
                origen.clasificacion(),
                origen.inicio(),
                origen.fin(),
                vigente);
    }

    private static InstitutionalPortfolioDocument aDocumento(DocumentoInstitucionalMetadata origen) {
        boolean puedeConsultarContenido = origen.publicado()
                || origen.clasificacionValidada() == null
                || !"RESTRINGIDO".equals(origen.clasificacionValidada().name());
        return new InstitutionalPortfolioDocument(
                origen.documentoId(),
                origen.serieId(),
                origen.numeroVersion(),
                origen.titulo(),
                origen.formato(),
                origen.mimeType(),
                origen.tamanoBytes(),
                origen.hashSha256(),
                aClasificacion(origen.clasificacionPropuesta()),
                aClasificacion(origen.clasificacionValidada()),
                origen.tipoDocumental(),
                origen.contextoDocumental(),
                origen.publicado(),
                origen.fechaCarga(),
                origen.usuarioCargaId(),
                puedeConsultarContenido,
                origen.etag());
    }

    private static InstitutionalPortfolioHistoryEntry aHistory(InstitutionalTransitionProjection origen) {
        return new InstitutionalPortfolioHistoryEntry(
                origen.id(),
                aEstado(origen.estadoAnterior()),
                aEstado(origen.estadoNuevo()),
                origen.usuarioId(),
                origen.rolEfectivoId(),
                origen.unidadEfectivaId(),
                origen.fechaTransicion(),
                origen.observaciones(),
                origen.documentoRefId());
    }

    private static TipoRegistroConsulta aTipoRegistro(TipoRegistroConsulta origen) {
        return origen;
    }

    private static TipoRegistroConsulta aTipoRegistro(String origen) {
        if (origen == null || origen.isBlank()) {
            return null;
        }
        return TipoRegistroConsulta.valueOf(origen);
    }

    private static TipoRegistroConsulta aTipoRegistro(pe.gob.midagri.piip.portafolio.entity.TipoRegistro origen) {
        if (origen == null) {
            return null;
        }
        return TipoRegistroConsulta.valueOf(origen.name());
    }

    private static TipoSolucionConsulta aTipoSolucion(String origen) {
        if (origen == null || origen.isBlank()) {
            return null;
        }
        return TipoSolucionConsulta.valueOf(origen);
    }

    private static FuenteOrigenConsulta aFuente(String origen) {
        if (origen == null || origen.isBlank()) {
            return null;
        }
        return FuenteOrigenConsulta.valueOf(origen);
    }

    private static EstadoIniciativaConsulta aEstado(String origen) {
        if (origen == null || origen.isBlank()) {
            return null;
        }
        return EstadoIniciativaConsulta.valueOf(origen);
    }

    private static ClasificacionDocumentoConsulta aClasificacion(
            pe.gob.midagri.piip.documentos.entity.ClasificacionDocumento origen) {
        if (origen == null) {
            return null;
        }
        return ClasificacionDocumentoConsulta.valueOf(origen.name());
    }

    private static String calcularEtagPagina(List<InstitutionalPortfolioSummary> items) {
        if (items == null || items.isEmpty()) {
            return "\"0\"";
        }
        StringBuilder builder = new StringBuilder();
        items.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(InstitutionalPortfolioSummary::id,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(item -> builder.append(item.id()).append(':').append(item.etag()).append(';'));
        return "\"" + Integer.toHexString(builder.toString().hashCode()) + "\"";
    }
}
