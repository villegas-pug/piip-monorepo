package pe.gob.midagri.piip.portafolio.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Proyección completa, canónica y neutra que el módulo
 * {@code portafolio} entrega a {@code consulta} para construir
 * {@code InstitutionalPortfolioDetail}. La forma limita la
 * superficie a los campos oficiales del registro (campos 1 a 23)
 * y conserva referencias mínimas a las entidades satélite
 * (titularidades, unidades, transiciones, participantes) que la
 * consulta resuelve de forma controlada.
 *
 * <p>Para mantener la independencia entre módulos y evitar
 * exponer entidades JPA, los campos categóricos se entregan como
 * cadena canónica (nombre del enum). El módulo {@code consulta}
 * los traduce a sus enums públicos mediante su propio
 * conversor.
 *
 * <p>El detalle de las personas y unidades participantes se carga
 * mediante colecciones auxiliares con DTOs del propio módulo
 * para evitar exponer entidades JPA.
 */
public record InstitutionalPortfolioProjection(
        Long id,
        String tipoRegistro,
        String codigo,
        String codigoOrigen,
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        String nombre,
        String tipoSolucion,
        String fuenteOrigen,
        String detalleFuente,
        Long responsableId,
        String problemaPublico,
        String solucionPropuesta,
        Long objetivoPeiId,
        Long actividadPoiId,
        Long unidadEjecutoraId,
        String estado,
        String componenteDigital,
        String detalleComponenteDigital,
        String nota,
        String resultadosClave,
        Long version,
        LocalDateTime fechaCreacion,
        Long iniciativaOrigenId,
        Long proyectoDerivadoId,
        List<InstitutionalUnidadProjection> unidades,
        List<InstitutionalTitularidadProjection> titularidades,
        List<InstitutionalTransitionProjection> transiciones,
        List<InstitutionalParticipanteProjection> participantesPersona) {

    /**
     * Unidad responsable del registro con su descripción canónica
     * y un indicador de principal. Evita exponer la entidad JPA
     * {@code UnidadResponsableEntity}.
     */
    public record InstitutionalUnidadProjection(
            Long id,
            Long unidadId,
            String descripcion,
            String abreviatura,
            Integer nroOrden) { }

    /**
     * Titularidad histórica (vigente y finalizadas) del registro.
     * Una sola fila tendrá {@code fin == null} cuando el
     * Responsable esté activo.
     */
    public record InstitutionalTitularidadProjection(
            Long id,
            Long usuarioId,
            LocalDate inicio,
            LocalDate fin,
            String motivoSustitucion,
            Long actorSustitucionId) { }

    /**
     * Entrada del historial de transiciones de estado, sin
     * exponer la entidad JPA {@code TransicionEstadoEntity}. Los
     * estados se entregan como cadena canónica.
     */
    public record InstitutionalTransitionProjection(
            Long id,
            String estadoAnterior,
            String estadoNuevo,
            Long usuarioId,
            Integer rolEfectivoId,
            Long unidadEfectivaId,
            LocalDateTime fechaTransicion,
            String observaciones,
            Long documentoRefId) { }

    /**
     * Persona participante vigente (FIN IS NULL) asociada al
     * proyecto, con clasificación aplicable al ámbito
     * institucional. Se omite cualquier dato personal cuando la
     * clasificación del registro no lo permite.
     */
    public record InstitutionalParticipanteProjection(
            Long idParticipacion,
            Long participanteId,
            Long usuarioId,
            String nombresCompletos,
            String institucion,
            String funcion,
            String clasificacion,
            LocalDate inicio,
            LocalDate fin) { }
}
