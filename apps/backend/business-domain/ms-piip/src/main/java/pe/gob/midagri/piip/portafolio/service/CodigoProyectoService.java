package pe.gob.midagri.piip.portafolio.service;

/**
 * Genera el código PIIP secuencial por unidad ejecutora bajo PESSIMISTIC_WRITE.
 * Formato: AAAA-PREFIJO_UNIDAD-NNNNN
 */
public interface CodigoProyectoService {

    /**
     * Genera un nuevo código PIIP para la unidad ejecutora en el año actual.
     *
     * @param unidadId ID de la unidad ejecutora principal
     * @return código generado con formato AAAA-PREFIJO-NNNNN
     * @throws pe.gob.midagri.piip.portafolio.exception.PortafolioValidationException
     *         si la unidad no tiene prefijo aprobado
     */
    String generarCodigo(Integer anio, Long unidadId, String prefijo);
}
