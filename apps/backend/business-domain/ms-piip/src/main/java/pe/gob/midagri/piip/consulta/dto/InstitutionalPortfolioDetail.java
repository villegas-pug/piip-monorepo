package pe.gob.midagri.piip.consulta.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalle institucional de un registro del portafolio para la
 * consulta autorizada. DTO propio del módulo {@code consulta} que
 * restringe la proyección a los campos permitidos por la matriz
 * de privacidad y por la clasificación efectiva del documento.
 *
 * <p>El detalle nunca expone:
 * <ul>
 *   <li>El BLOB, clave física o URL directa del documento (solo
 *       metadatos).</li>
 *   <li>Los expedientes institucionales ni sus documentos.</li>
 *   <li>El contenido de un documento reclasificado a una
 *       categoría más restrictiva.</li>
 *   <li>Datos personales de los participantes cuando el actor
 *       no es Responsable, Evaluador o administrador
 *       autorizado.</li>
 * </ul>
 *
 * <p>La cabecera ETag del detalle permite la
 * concurrencia optimista y se calcula a partir de la versión
 * del registro y de sus documentos vigentes.
 */
public record InstitutionalPortfolioDetail(
        Long id,
        TipoRegistroConsulta tipoRegistro,
        String codigo,
        String codigoOrigen,
        LocalDate fechaInicio,
        LocalDate fechaCierre,
        String nombre,
        TipoSolucionConsulta tipoSolucion,
        FuenteOrigenConsulta fuenteOrigen,
        String detalleFuente,
        Long responsableId,
        String problemaPublico,
        String solucionPropuesta,
        Long objetivoPeiId,
        Long actividadPoiId,
        Long unidadEjecutoraId,
        String unidadEjecutoraDescripcion,
        String unidadEjecutoraAbreviatura,
        EstadoIniciativaConsulta estado,
        Boolean componenteDigital,
        String detalleComponenteDigital,
        String nota,
        String resultadosClave,
        List<InstitutionalPortfolioUnidad> unidades,
        List<InstitutionalPortfolioPersonaParticipante> participantes,
        List<InstitutionalPortfolioDocument> documentos,
        List<InstitutionalPortfolioHistoryEntry> historial,
        IniciativaProyectoRelacion relacion,
        boolean actorEsResponsable,
        boolean actorEsEvaluador,
        boolean actorEsAdministrador,
        LocalDateTime fechaCreacion,
        Long version,
        String etag) {

    /**
     * Relación iniciativa-proyecto cuando aplica; vacío en
     * cualquier otro caso.
     */
    public record IniciativaProyectoRelacion(
            Long iniciativaId,
            Long proyectoId,
            boolean iniciativaActual,
            boolean proyectoActual) { }
}
