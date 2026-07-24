package pe.gob.midagri.piip.portafolio.transicion;

import java.time.LocalDateTime;

import pe.gob.midagri.piip.portafolio.entity.EstadoIniciativa;

/**
 * DTO HTTP de respuesta para una transicion de estado confirmada.
 *
 * <p>Se expone como contrato del modulo portafolio; nunca contiene
 * entidades JPA. La cabecera {@code ETag} que se devuelve al cliente
 * reproduce el formato canonico del repositorio
 * ({@code "<id>-<version>"}). La fecha es la del servidor, fijada
 * por la columna {@code SYSTIMESTAMP} de {@code TRANSICION_ESTADO}
 * (DDL 014 vigente); el servicio no la sobreescribe.
 */
public record TransicionDetail(
        Long registroId,
        EstadoIniciativa estadoAnterior,
        EstadoIniciativa estadoNuevo,
        Long transicionId,
        LocalDateTime fechaTransicion,
        String actorSub,
        Long version,
        String etag) {
}
