package pe.gob.midagri.piip.portafolio.seguimiento.service;

/**
 * Servicio de aptitud documental para los flujos del modulo
 * seguimiento (US4, Constitucion 5.0.0). Determina si un documento
 * del portafolio es apto como evidencia segun el tipo documental
 * canonico solicitado.
 *
 * <p>La autoridad definitiva es el modulo {@code documentos}; la
 * implementacion concreta de este servicio consulta la
 * {@code TIPO_DOCUMENTO} y los estados del documento
 * (clasificacion validada, integridad, hash, etc.). En T072 la
 * firma queda estabilizada como contrato del modulo portafolio;
 * la implementacion se conecta al servicio transversal cuando
 * esta disponible (inyeccion opcional con
 * {@code @Autowired(required = false)}).
 */
public interface AptitudDocumentalService {

    /** Tipos documentales canónicos exigidos por las transiciones de US4. */
    enum TipoEvidenciaTransicion {
        SUSPENSION("Evidencia de Suspensión"),
        CANCELACION("Informe de la Oficina de Modernización, Cancelación"),
        APROBACION_PRODUCTO_FINAL("Documento de Aprobacion de Producto"),
        NO_APROBACION_PRODUCTO_FINAL("Evidencia de No Aprobacion"),
        INFORME_FINAL_CIERRE("Informe Final de Cierre");

        private final String tipoDocumental;

        TipoEvidenciaTransicion(String tipoDocumental) {
            this.tipoDocumental = tipoDocumental;
        }

        public String tipoDocumental() {
            return tipoDocumental;
        }
    }

    /**
     * Indica si el documento es apto como evidencia del tipo
     * documental indicado. Devuelve {@code false} si el documento
     * no existe, si su tipo no coincide, si la clasificacion no
     * esta validada o si su integridad (hash SHA-256) esta rota.
     *
     * @param documentoId identificador del documento en
     *     {@code DOCUMENTO}
     * @param tipoDocumental tipo documental canonico
     *     ({@code AutoevaluacionCicloTrabajo},
     *     {@code SeguimientoAgilTableroKanban} o
     *     {@code MatrizPlanificacionCiclos})
     * @return {@code true} si el documento cumple los requisitos
     *     para ser evidencia; {@code false} en caso contrario
     */
    boolean esApto(long documentoId, String tipoDocumental);

    /** Valida una evidencia de transición mediante el contrato de documentos. */
    boolean esAptoParaTransicion(long documentoId, TipoEvidenciaTransicion tipo);
}
