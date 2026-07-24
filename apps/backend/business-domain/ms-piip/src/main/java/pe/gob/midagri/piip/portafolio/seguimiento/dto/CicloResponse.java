package pe.gob.midagri.piip.portafolio.seguimiento.dto;

import java.time.LocalDateTime;

/**
 * Detalle canonico de un ciclo quincenal versionado (US4,
 * Constitucion 5.0.0 y DDL
 * {@code 015_ciclos_resultados_cierre.sql}).
 *
 * <p>Es la representacion HTTP que retorna el servicio de
 * seguimiento (alta, correccion y consulta). Su contrato no expone
 * entidades JPA. El campo {@code cerrado} refleja el estado
 * canonico {@code S}/{@code N} de la fila
 * {@code CICLO_PROYECTO.CERRADO} y la cabecera {@code etag} se
 * deriva de la version optimista y el id para soportar If-Match
 * opcional en operaciones posteriores.
 */
public record CicloResponse(
        long idCiclo,
        long idProyecto,
        String periodo,
        int numeroVersion,
        long idVersionAnterior,
        String objetivos,
        String actividades,
        Integer avance,
        String dificultades,
        String proximasAcciones,
        String cerrado,
        LocalDateTime fechaCierre,
        String etag) {
}
