package pe.gob.midagri.piip.portafolio.seguimiento.dto;

/**
 * Detalle canonico de una version anexada a un ciclo del proyecto
 * (US4, Constitucion 5.0.0 y DDL
 * {@code 015_ciclos_resultados_cierre.sql}).
 *
 * <p>Representa una fila append-only de la cadena de versiones del
 * ciclo; nunca sustituye a la fila original. La cabecera
 * {@code etag} se deriva de la version y la id para soportar
 * If-Match opcional en operaciones posteriores.
 */
public record CicloVersionResponse(
        long idVersion,
        long idCiclo,
        int numeroVersion,
        String motivo,
        String objetivos,
        String actividades,
        Integer avance,
        String dificultades,
        String proximasAcciones,
        String etag) {
}
